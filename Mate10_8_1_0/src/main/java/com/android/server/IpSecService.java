package com.android.server;

import android.content.Context;
import android.net.IIpSecService.Stub;
import android.net.INetd;
import android.net.IpSecAlgorithm;
import android.net.IpSecConfig;
import android.net.IpSecSpiResponse;
import android.net.IpSecTransformResponse;
import android.net.IpSecUdpEncapResponse;
import android.net.util.NetdService;
import android.os.Binder;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;

public class IpSecService extends Stub {
    private static final boolean DBG = Log.isLoggable(TAG, 3);
    private static final int[] DIRECTIONS = new int[]{1, 0};
    static final int FREE_PORT_MIN = 1024;
    private static final InetAddress INADDR_ANY;
    private static final int MAX_PORT_BIND_ATTEMPTS = 10;
    private static final int NETD_FETCH_TIMEOUT = 5000;
    private static final String NETD_SERVICE_NAME = "netd";
    static final int PORT_MAX = 65535;
    private static final String TAG = "IpSecService";
    private static AtomicInteger mNextResourceId = new AtomicInteger(16441040);
    private final Context mContext;
    @GuardedBy("this")
    private final ManagedResourceArray<SpiRecord> mSpiRecords = new ManagedResourceArray();
    @GuardedBy("this")
    private final ManagedResourceArray<TransformRecord> mTransformRecords = new ManagedResourceArray();
    @GuardedBy("this")
    private final ManagedResourceArray<UdpSocketRecord> mUdpSocketRecords = new ManagedResourceArray();

    private abstract class ManagedResource implements DeathRecipient {
        private IBinder mBinder;
        private AtomicInteger mReferenceCount = new AtomicInteger(0);
        protected int mResourceId;
        final int pid;
        final int uid;

        protected abstract void releaseResources() throws RemoteException;

        ManagedResource(int resourceId, IBinder binder) {
            this.mBinder = binder;
            this.mResourceId = resourceId;
            this.pid = Binder.getCallingPid();
            this.uid = Binder.getCallingUid();
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        public void addReference() {
            this.mReferenceCount.incrementAndGet();
        }

        public void removeReference() {
            if (this.mReferenceCount.decrementAndGet() < 0) {
                Log.wtf(IpSecService.TAG, "Programming error: negative reference count");
            }
        }

        public boolean isReferenced() {
            return this.mReferenceCount.get() > 0;
        }

        public void checkOwnerOrSystemAndThrow() {
            if (this.uid != Binder.getCallingUid() && 1000 != Binder.getCallingUid()) {
                throw new SecurityException("Only the owner may access managed resources!");
            }
        }

        public final void release() throws RemoteException {
            synchronized (IpSecService.this) {
                if (isReferenced()) {
                    throw new IllegalStateException("Cannot release a resource that has active references!");
                } else if (this.mResourceId == 0) {
                } else {
                    releaseResources();
                    if (this.mBinder != null) {
                        this.mBinder.unlinkToDeath(this, 0);
                    }
                    this.mBinder = null;
                    this.mResourceId = 0;
                }
            }
        }

        public final void binderDied() {
            try {
                release();
            } catch (Exception e) {
                Log.e(IpSecService.TAG, "Failed to release resource: " + e);
            }
        }
    }

    private class ManagedResourceArray<T extends ManagedResource> {
        SparseArray<T> mArray;

        private ManagedResourceArray() {
            this.mArray = new SparseArray();
        }

        T get(int key) {
            ManagedResource val = (ManagedResource) this.mArray.get(key);
            if (val != null) {
                val.checkOwnerOrSystemAndThrow();
            }
            return val;
        }

        void put(int key, T obj) {
            Preconditions.checkNotNull(obj, "Null resources cannot be added");
            this.mArray.put(key, obj);
        }

        void remove(int key) {
            this.mArray.remove(key);
        }
    }

    private final class SpiRecord extends ManagedResource {
        private final int mDirection;
        private final String mLocalAddress;
        private boolean mOwnedByTransform = false;
        private final String mRemoteAddress;
        private int mSpi;

        SpiRecord(int resourceId, IBinder binder, int direction, String localAddress, String remoteAddress, int spi) {
            super(resourceId, binder);
            this.mDirection = direction;
            this.mLocalAddress = localAddress;
            this.mRemoteAddress = remoteAddress;
            this.mSpi = spi;
        }

        protected void releaseResources() {
            if (this.mOwnedByTransform) {
                Log.d(IpSecService.TAG, "Cannot release Spi " + this.mSpi + ": Currently locked by a Transform");
                return;
            }
            try {
                IpSecService.this.getNetdInstance().ipSecDeleteSecurityAssociation(this.mResourceId, this.mDirection, this.mLocalAddress, this.mRemoteAddress, this.mSpi);
            } catch (ServiceSpecificException e) {
            } catch (RemoteException e2) {
                Log.e(IpSecService.TAG, "Failed to delete SPI reservation with ID: " + this.mResourceId);
            }
            this.mSpi = 0;
        }

        public int getSpi() {
            return this.mSpi;
        }

        public void setOwnedByTransform() {
            if (this.mOwnedByTransform) {
                throw new IllegalStateException("Cannot own an SPI twice!");
            }
            this.mOwnedByTransform = true;
        }
    }

    private final class TransformRecord extends ManagedResource {
        private final IpSecConfig mConfig;
        private final UdpSocketRecord mSocket;
        private final SpiRecord[] mSpis;

        TransformRecord(int resourceId, IBinder binder, IpSecConfig config, SpiRecord[] spis, UdpSocketRecord socket) {
            super(resourceId, binder);
            this.mConfig = config;
            this.mSpis = spis;
            this.mSocket = socket;
            for (int direction : IpSecService.DIRECTIONS) {
                this.mSpis[direction].addReference();
                this.mSpis[direction].setOwnedByTransform();
            }
            if (this.mSocket != null) {
                this.mSocket.addReference();
            }
        }

        public IpSecConfig getConfig() {
            return this.mConfig;
        }

        public SpiRecord getSpiRecord(int direction) {
            return this.mSpis[direction];
        }

        protected void releaseResources() {
            for (int direction : IpSecService.DIRECTIONS) {
                int spi = this.mSpis[direction].getSpi();
                try {
                    String hostAddress;
                    String hostAddress2;
                    INetd netdInstance = IpSecService.this.getNetdInstance();
                    int i = this.mResourceId;
                    if (this.mConfig.getLocalAddress() != null) {
                        hostAddress = this.mConfig.getLocalAddress().getHostAddress();
                    } else {
                        hostAddress = "";
                    }
                    if (this.mConfig.getRemoteAddress() != null) {
                        hostAddress2 = this.mConfig.getRemoteAddress().getHostAddress();
                    } else {
                        hostAddress2 = "";
                    }
                    netdInstance.ipSecDeleteSecurityAssociation(i, direction, hostAddress, hostAddress2, spi);
                } catch (ServiceSpecificException e) {
                } catch (RemoteException e2) {
                    Log.e(IpSecService.TAG, "Failed to delete SA with ID: " + this.mResourceId);
                }
            }
            for (int direction2 : IpSecService.DIRECTIONS) {
                this.mSpis[direction2].removeReference();
            }
            if (this.mSocket != null) {
                this.mSocket.removeReference();
            }
        }
    }

    private final class UdpSocketRecord extends ManagedResource {
        private final int mPort;
        private FileDescriptor mSocket;

        UdpSocketRecord(int resourceId, IBinder binder, FileDescriptor socket, int port) {
            super(resourceId, binder);
            this.mSocket = socket;
            this.mPort = port;
        }

        protected void releaseResources() {
            Log.d(IpSecService.TAG, "Closing port " + this.mPort);
            IoUtils.closeQuietly(this.mSocket);
            this.mSocket = null;
        }

        public int getPort() {
            return this.mPort;
        }

        public FileDescriptor getSocket() {
            return this.mSocket;
        }
    }

    static {
        try {
            INADDR_ANY = InetAddress.getByAddress(new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0});
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private IpSecService(Context context) {
        this.mContext = context;
    }

    static IpSecService create(Context context) throws InterruptedException {
        IpSecService service = new IpSecService(context);
        service.connectNativeNetdService();
        return service;
    }

    public void systemReady() {
        if (isNetdAlive()) {
            Slog.d(TAG, "IpSecService is ready");
        } else {
            Slog.wtf(TAG, "IpSecService not ready: failed to connect to NetD Native Service!");
        }
    }

    private void connectNativeNetdService() {
        new Thread() {
            public void run() {
                synchronized (IpSecService.this) {
                    NetdService.get(5000);
                }
            }
        }.start();
    }

    INetd getNetdInstance() throws RemoteException {
        INetd netd = NetdService.getInstance();
        if (netd != null) {
            return netd;
        }
        throw new RemoteException("Failed to Get Netd Instance");
    }

    synchronized boolean isNetdAlive() {
        boolean z = false;
        synchronized (this) {
            try {
                INetd netd = getNetdInstance();
                if (netd == null) {
                    return z;
                }
                z = netd.isAlive();
                return z;
            } catch (RemoteException e) {
                return z;
            }
        }
    }

    public synchronized IpSecSpiResponse reserveSecurityParameterIndex(int direction, String remoteAddress, int requestedSpi, IBinder binder) throws RemoteException {
        int resourceId;
        int spi;
        resourceId = mNextResourceId.getAndIncrement();
        spi = 0;
        String localAddress = "";
        try {
            spi = getNetdInstance().ipSecAllocateSpi(resourceId, direction, localAddress, remoteAddress, requestedSpi);
            Log.d(TAG, "Allocated SPI " + spi);
            this.mSpiRecords.put(resourceId, new SpiRecord(resourceId, binder, direction, localAddress, remoteAddress, spi));
        } catch (ServiceSpecificException e) {
            return new IpSecSpiResponse(2, 0, spi);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
        return new IpSecSpiResponse(0, resourceId, spi);
    }

    private synchronized <T extends ManagedResource> void releaseManagedResource(ManagedResourceArray<T> resArray, int resourceId, String typeName) throws RemoteException {
        T record = resArray.get(resourceId);
        if (record == null) {
            throw new IllegalArgumentException(typeName + " " + resourceId + " is not available to be deleted");
        }
        record.release();
        resArray.remove(resourceId);
    }

    public void releaseSecurityParameterIndex(int resourceId) throws RemoteException {
        releaseManagedResource(this.mSpiRecords, resourceId, "SecurityParameterIndex");
    }

    private void bindToRandomPort(FileDescriptor sockFd) throws IOException {
        int i = 10;
        while (i > 0) {
            try {
                FileDescriptor probeSocket = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
                Os.bind(probeSocket, INADDR_ANY, 0);
                int port = ((InetSocketAddress) Os.getsockname(probeSocket)).getPort();
                Os.close(probeSocket);
                Log.v(TAG, "Binding to port " + port);
                Os.bind(sockFd, INADDR_ANY, port);
                return;
            } catch (ErrnoException e) {
                if (e.errno == OsConstants.EADDRINUSE) {
                    i--;
                } else {
                    throw e.rethrowAsIOException();
                }
            }
        }
        throw new IOException("Failed 10 attempts to bind to a port");
    }

    public synchronized IpSecUdpEncapResponse openUdpEncapsulationSocket(int port, IBinder binder) throws RemoteException {
        int resourceId;
        FileDescriptor fileDescriptor;
        if (port == 0 || (port >= 1024 && port <= 65535)) {
            resourceId = mNextResourceId.getAndIncrement();
            fileDescriptor = null;
            try {
                fileDescriptor = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
                if (port != 0) {
                    Log.v(TAG, "Binding to port " + port);
                    Os.bind(fileDescriptor, INADDR_ANY, port);
                } else {
                    bindToRandomPort(fileDescriptor);
                }
                Os.setsockoptInt(fileDescriptor, OsConstants.IPPROTO_UDP, OsConstants.UDP_ENCAP, OsConstants.UDP_ENCAP_ESPINUDP);
                this.mUdpSocketRecords.put(resourceId, new UdpSocketRecord(resourceId, binder, fileDescriptor, port));
            } catch (IOException e) {
                IoUtils.closeQuietly(fileDescriptor);
                return new IpSecUdpEncapResponse(1);
            }
        }
        throw new IllegalArgumentException("Specified port number must be a valid non-reserved UDP port");
        return new IpSecUdpEncapResponse(0, resourceId, port, fileDescriptor);
    }

    public void closeUdpEncapsulationSocket(int resourceId) throws RemoteException {
        releaseManagedResource(this.mUdpSocketRecords, resourceId, "UdpEncapsulationSocket");
    }

    public synchronized IpSecTransformResponse createTransportModeTransform(IpSecConfig c, IBinder binder) throws RemoteException {
        int resourceId;
        resourceId = mNextResourceId.getAndIncrement();
        SpiRecord[] spis = new SpiRecord[DIRECTIONS.length];
        int encapLocalPort = 0;
        int encapRemotePort = 0;
        UdpSocketRecord udpSocketRecord = null;
        int encapType = c.getEncapType();
        if (encapType != 0) {
            udpSocketRecord = (UdpSocketRecord) this.mUdpSocketRecords.get(c.getEncapLocalResourceId());
            encapLocalPort = udpSocketRecord.getPort();
            encapRemotePort = c.getEncapRemotePort();
        }
        int[] iArr = DIRECTIONS;
        int length = iArr.length;
        int i = 0;
        while (i < length) {
            int direction = iArr[i];
            IpSecAlgorithm auth = c.getAuthentication(direction);
            IpSecAlgorithm crypt = c.getEncryption(direction);
            spis[direction] = (SpiRecord) this.mSpiRecords.get(c.getSpiResourceId(direction));
            int spi = spis[direction].getSpi();
            try {
                String hostAddress;
                String hostAddress2;
                long networkHandle;
                int truncationLengthBits;
                INetd netdInstance = getNetdInstance();
                int mode = c.getMode();
                if (c.getLocalAddress() != null) {
                    hostAddress = c.getLocalAddress().getHostAddress();
                } else {
                    hostAddress = "";
                }
                if (c.getRemoteAddress() != null) {
                    hostAddress2 = c.getRemoteAddress().getHostAddress();
                } else {
                    hostAddress2 = "";
                }
                if (c.getNetwork() != null) {
                    networkHandle = c.getNetwork().getNetworkHandle();
                } else {
                    networkHandle = 0;
                }
                String name = auth != null ? auth.getName() : "";
                byte[] key = auth != null ? auth.getKey() : null;
                int truncationLengthBits2 = auth != null ? auth.getTruncationLengthBits() : 0;
                String name2 = crypt != null ? crypt.getName() : "";
                byte[] key2 = crypt != null ? crypt.getKey() : null;
                if (crypt != null) {
                    truncationLengthBits = crypt.getTruncationLengthBits();
                } else {
                    truncationLengthBits = 0;
                }
                netdInstance.ipSecAddSecurityAssociation(resourceId, mode, direction, hostAddress, hostAddress2, networkHandle, spi, name, key, truncationLengthBits2, name2, key2, truncationLengthBits, encapType, encapLocalPort, encapRemotePort);
                i++;
            } catch (ServiceSpecificException e) {
                return new IpSecTransformResponse(1);
            }
        }
        this.mTransformRecords.put(resourceId, new TransformRecord(resourceId, binder, c, spis, udpSocketRecord));
        return new IpSecTransformResponse(0, resourceId);
    }

    public void deleteTransportModeTransform(int resourceId) throws RemoteException {
        releaseManagedResource(this.mTransformRecords, resourceId, "IpSecTransform");
    }

    public synchronized void applyTransportModeTransform(ParcelFileDescriptor socket, int resourceId) throws RemoteException {
        TransformRecord info = (TransformRecord) this.mTransformRecords.get(resourceId);
        if (info == null) {
            throw new IllegalArgumentException("Transform " + resourceId + " is not active");
        } else if (info.pid == getCallingPid() && info.uid == getCallingUid()) {
            IpSecConfig c = info.getConfig();
            try {
                for (int direction : DIRECTIONS) {
                    String hostAddress;
                    String hostAddress2;
                    INetd netdInstance = getNetdInstance();
                    FileDescriptor fileDescriptor = socket.getFileDescriptor();
                    if (c.getLocalAddress() != null) {
                        hostAddress = c.getLocalAddress().getHostAddress();
                    } else {
                        hostAddress = "";
                    }
                    if (c.getRemoteAddress() != null) {
                        hostAddress2 = c.getRemoteAddress().getHostAddress();
                    } else {
                        hostAddress2 = "";
                    }
                    netdInstance.ipSecApplyTransportModeTransform(fileDescriptor, resourceId, direction, hostAddress, hostAddress2, info.getSpiRecord(direction).getSpi());
                }
            } catch (ServiceSpecificException e) {
            }
        } else {
            throw new SecurityException("Only the owner of an IpSec Transform may apply it!");
        }
    }

    public void removeTransportModeTransform(ParcelFileDescriptor socket, int resourceId) throws RemoteException {
        try {
            getNetdInstance().ipSecRemoveTransportModeTransform(socket.getFileDescriptor());
        } catch (ServiceSpecificException e) {
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        pw.println("IpSecService Log:");
        pw.println("NetdNativeService Connection: " + (isNetdAlive() ? "alive" : "dead"));
        pw.println();
    }
}
