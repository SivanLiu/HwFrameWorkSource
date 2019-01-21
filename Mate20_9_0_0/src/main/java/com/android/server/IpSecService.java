package com.android.server;

import android.app.AppOpsManager;
import android.content.Context;
import android.net.IIpSecService.Stub;
import android.net.INetd;
import android.net.IpSecAlgorithm;
import android.net.IpSecConfig;
import android.net.IpSecSpiResponse;
import android.net.IpSecTransformResponse;
import android.net.IpSecTunnelInterfaceResponse;
import android.net.IpSecUdpEncapResponse;
import android.net.LinkAddress;
import android.net.Network;
import android.net.NetworkUtils;
import android.net.TrafficStats;
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
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;

public class IpSecService extends Stub {
    private static final boolean DBG = Log.isLoggable(TAG, 3);
    private static final int[] DIRECTIONS = new int[]{1, 0};
    static final int FREE_PORT_MIN = 1024;
    private static final InetAddress INADDR_ANY;
    private static final int MAX_PORT_BIND_ATTEMPTS = 10;
    private static final int NETD_FETCH_TIMEOUT_MS = 5000;
    private static final String NETD_SERVICE_NAME = "netd";
    static final int PORT_MAX = 65535;
    private static final String TAG = "IpSecService";
    @VisibleForTesting
    static final int TUN_INTF_NETID_RANGE = 1024;
    @VisibleForTesting
    static final int TUN_INTF_NETID_START = 64512;
    private static final String[] WILDCARD_ADDRESSES = new String[]{"0.0.0.0", "::"};
    private final Context mContext;
    @GuardedBy("IpSecService.this")
    private int mNextResourceId;
    private int mNextTunnelNetIdIndex;
    private final IpSecServiceConfiguration mSrvConfig;
    private final SparseBooleanArray mTunnelNetIds;
    final UidFdTagger mUidFdTagger;
    @VisibleForTesting
    final UserResourceTracker mUserResourceTracker;

    @VisibleForTesting
    public interface IResource {
        void freeUnderlyingResources() throws RemoteException;

        void invalidate() throws RemoteException;
    }

    interface IpSecServiceConfiguration {
        public static final IpSecServiceConfiguration GETSRVINSTANCE = new IpSecServiceConfiguration() {
            public INetd getNetdInstance() throws RemoteException {
                INetd netd = NetdService.getInstance();
                if (netd != null) {
                    return netd;
                }
                throw new RemoteException("Failed to Get Netd Instance");
            }
        };

        INetd getNetdInstance() throws RemoteException;
    }

    @VisibleForTesting
    public class RefcountedResource<T extends IResource> implements DeathRecipient {
        IBinder mBinder;
        private final List<RefcountedResource> mChildren;
        int mRefCount = 1;
        private final T mResource;

        RefcountedResource(T resource, IBinder binder, RefcountedResource... children) {
            synchronized (IpSecService.this) {
                this.mResource = resource;
                this.mChildren = new ArrayList(children.length);
                this.mBinder = binder;
                for (RefcountedResource child : children) {
                    this.mChildren.add(child);
                    child.mRefCount++;
                }
                try {
                    this.mBinder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    binderDied();
                }
            }
        }

        public void binderDied() {
            synchronized (IpSecService.this) {
                try {
                    userRelease();
                } catch (Exception e) {
                    String str = IpSecService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to release resource: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                }
            }
        }

        public T getResource() {
            return this.mResource;
        }

        @GuardedBy("IpSecService.this")
        public void userRelease() throws RemoteException {
            if (this.mBinder != null) {
                this.mBinder.unlinkToDeath(this, 0);
                this.mBinder = null;
                this.mResource.invalidate();
                releaseReference();
            }
        }

        @GuardedBy("IpSecService.this")
        @VisibleForTesting
        public void releaseReference() throws RemoteException {
            this.mRefCount--;
            if (this.mRefCount <= 0) {
                if (this.mRefCount >= 0) {
                    this.mResource.freeUnderlyingResources();
                    for (RefcountedResource<? extends IResource> child : this.mChildren) {
                        child.releaseReference();
                    }
                    this.mRefCount--;
                    return;
                }
                throw new IllegalStateException("Invalid operation - resource has already been released.");
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{mResource=");
            stringBuilder.append(this.mResource);
            stringBuilder.append(", mRefCount=");
            stringBuilder.append(this.mRefCount);
            stringBuilder.append(", mChildren=");
            stringBuilder.append(this.mChildren);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    static class RefcountedResourceArray<T extends IResource> {
        SparseArray<RefcountedResource<T>> mArray = new SparseArray();
        private final String mTypeName;

        public RefcountedResourceArray(String typeName) {
            this.mTypeName = typeName;
        }

        T getResourceOrThrow(int key) {
            return getRefcountedResourceOrThrow(key).getResource();
        }

        RefcountedResource<T> getRefcountedResourceOrThrow(int key) {
            RefcountedResource<T> resource = (RefcountedResource) this.mArray.get(key);
            if (resource != null) {
                return resource;
            }
            throw new IllegalArgumentException(String.format("No such %s found for given id: %d", new Object[]{this.mTypeName, Integer.valueOf(key)}));
        }

        void put(int key, RefcountedResource<T> obj) {
            Preconditions.checkNotNull(obj, "Null resources cannot be added");
            this.mArray.put(key, obj);
        }

        void remove(int key) {
            this.mArray.remove(key);
        }

        public String toString() {
            return this.mArray.toString();
        }
    }

    @VisibleForTesting
    static class ResourceTracker {
        int mCurrent = 0;
        private final int mMax;

        ResourceTracker(int max) {
            this.mMax = max;
        }

        boolean isAvailable() {
            return this.mCurrent < this.mMax;
        }

        void take() {
            if (!isAvailable()) {
                Log.wtf(IpSecService.TAG, "Too many resources allocated!");
            }
            this.mCurrent++;
        }

        void give() {
            if (this.mCurrent <= 0) {
                Log.wtf(IpSecService.TAG, "We've released this resource too many times");
            }
            this.mCurrent--;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{mCurrent=");
            stringBuilder.append(this.mCurrent);
            stringBuilder.append(", mMax=");
            stringBuilder.append(this.mMax);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    @VisibleForTesting
    public interface UidFdTagger {
        void tag(FileDescriptor fileDescriptor, int i) throws IOException;
    }

    @VisibleForTesting
    static final class UserRecord {
        public static final int MAX_NUM_ENCAP_SOCKETS = 2;
        public static final int MAX_NUM_SPIS = 8;
        public static final int MAX_NUM_TRANSFORMS = 4;
        public static final int MAX_NUM_TUNNEL_INTERFACES = 2;
        final RefcountedResourceArray<EncapSocketRecord> mEncapSocketRecords = new RefcountedResourceArray(EncapSocketRecord.class.getSimpleName());
        final ResourceTracker mSocketQuotaTracker = new ResourceTracker(2);
        final ResourceTracker mSpiQuotaTracker = new ResourceTracker(8);
        final RefcountedResourceArray<SpiRecord> mSpiRecords = new RefcountedResourceArray(SpiRecord.class.getSimpleName());
        final ResourceTracker mTransformQuotaTracker = new ResourceTracker(4);
        final RefcountedResourceArray<TransformRecord> mTransformRecords = new RefcountedResourceArray(TransformRecord.class.getSimpleName());
        final RefcountedResourceArray<TunnelInterfaceRecord> mTunnelInterfaceRecords = new RefcountedResourceArray(TunnelInterfaceRecord.class.getSimpleName());
        final ResourceTracker mTunnelQuotaTracker = new ResourceTracker(2);

        UserRecord() {
        }

        void removeSpiRecord(int resourceId) {
            this.mSpiRecords.remove(resourceId);
        }

        void removeTransformRecord(int resourceId) {
            this.mTransformRecords.remove(resourceId);
        }

        void removeTunnelInterfaceRecord(int resourceId) {
            this.mTunnelInterfaceRecords.remove(resourceId);
        }

        void removeEncapSocketRecord(int resourceId) {
            this.mEncapSocketRecords.remove(resourceId);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{mSpiQuotaTracker=");
            stringBuilder.append(this.mSpiQuotaTracker);
            stringBuilder.append(", mTransformQuotaTracker=");
            stringBuilder.append(this.mTransformQuotaTracker);
            stringBuilder.append(", mSocketQuotaTracker=");
            stringBuilder.append(this.mSocketQuotaTracker);
            stringBuilder.append(", mTunnelQuotaTracker=");
            stringBuilder.append(this.mTunnelQuotaTracker);
            stringBuilder.append(", mSpiRecords=");
            stringBuilder.append(this.mSpiRecords);
            stringBuilder.append(", mTransformRecords=");
            stringBuilder.append(this.mTransformRecords);
            stringBuilder.append(", mEncapSocketRecords=");
            stringBuilder.append(this.mEncapSocketRecords);
            stringBuilder.append(", mTunnelInterfaceRecords=");
            stringBuilder.append(this.mTunnelInterfaceRecords);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    @VisibleForTesting
    static final class UserResourceTracker {
        private final SparseArray<UserRecord> mUserRecords = new SparseArray();

        UserResourceTracker() {
        }

        public UserRecord getUserRecord(int uid) {
            checkCallerUid(uid);
            UserRecord r = (UserRecord) this.mUserRecords.get(uid);
            if (r != null) {
                return r;
            }
            r = new UserRecord();
            this.mUserRecords.put(uid, r);
            return r;
        }

        private void checkCallerUid(int uid) {
            if (uid != Binder.getCallingUid() && 1000 != Binder.getCallingUid()) {
                throw new SecurityException("Attempted access of unowned resources");
            }
        }

        public String toString() {
            return this.mUserRecords.toString();
        }
    }

    private abstract class OwnedResourceRecord implements IResource {
        protected final int mResourceId;
        final int pid;
        final int uid;

        public abstract void freeUnderlyingResources() throws RemoteException;

        protected abstract ResourceTracker getResourceTracker();

        public abstract void invalidate() throws RemoteException;

        OwnedResourceRecord(int resourceId) {
            if (resourceId != -1) {
                this.mResourceId = resourceId;
                this.pid = Binder.getCallingPid();
                this.uid = Binder.getCallingUid();
                getResourceTracker().take();
                return;
            }
            throw new IllegalArgumentException("Resource ID must not be INVALID_RESOURCE_ID");
        }

        protected UserRecord getUserRecord() {
            return IpSecService.this.mUserResourceTracker.getUserRecord(this.uid);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{mResourceId=");
            stringBuilder.append(this.mResourceId);
            stringBuilder.append(", pid=");
            stringBuilder.append(this.pid);
            stringBuilder.append(", uid=");
            stringBuilder.append(this.uid);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private final class EncapSocketRecord extends OwnedResourceRecord {
        private final int mPort;
        private FileDescriptor mSocket;

        EncapSocketRecord(int resourceId, FileDescriptor socket, int port) {
            super(resourceId);
            this.mSocket = socket;
            this.mPort = port;
        }

        public void freeUnderlyingResources() {
            String str = IpSecService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Closing port ");
            stringBuilder.append(this.mPort);
            Log.d(str, stringBuilder.toString());
            IoUtils.closeQuietly(this.mSocket);
            this.mSocket = null;
            getResourceTracker().give();
        }

        public int getPort() {
            return this.mPort;
        }

        public FileDescriptor getFileDescriptor() {
            return this.mSocket;
        }

        protected ResourceTracker getResourceTracker() {
            return getUserRecord().mSocketQuotaTracker;
        }

        public void invalidate() {
            getUserRecord().removeEncapSocketRecord(this.mResourceId);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{super=");
            stringBuilder.append(super.toString());
            stringBuilder.append(", mSocket=");
            stringBuilder.append(this.mSocket);
            stringBuilder.append(", mPort=");
            stringBuilder.append(this.mPort);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private final class SpiRecord extends OwnedResourceRecord {
        private final String mDestinationAddress;
        private boolean mOwnedByTransform = false;
        private final String mSourceAddress;
        private int mSpi;

        SpiRecord(int resourceId, String sourceAddress, String destinationAddress, int spi) {
            super(resourceId);
            this.mSourceAddress = sourceAddress;
            this.mDestinationAddress = destinationAddress;
            this.mSpi = spi;
        }

        public void freeUnderlyingResources() {
            try {
                if (!this.mOwnedByTransform) {
                    IpSecService.this.mSrvConfig.getNetdInstance().ipSecDeleteSecurityAssociation(this.mResourceId, this.mSourceAddress, this.mDestinationAddress, this.mSpi, 0, 0);
                }
            } catch (RemoteException | ServiceSpecificException e) {
                String str = IpSecService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to delete SPI reservation with ID: ");
                stringBuilder.append(this.mResourceId);
                Log.e(str, stringBuilder.toString(), e);
            }
            this.mSpi = 0;
            getResourceTracker().give();
        }

        public int getSpi() {
            return this.mSpi;
        }

        public String getDestinationAddress() {
            return this.mDestinationAddress;
        }

        public void setOwnedByTransform() {
            if (this.mOwnedByTransform) {
                throw new IllegalStateException("Cannot own an SPI twice!");
            }
            this.mOwnedByTransform = true;
        }

        public boolean getOwnedByTransform() {
            return this.mOwnedByTransform;
        }

        public void invalidate() throws RemoteException {
            getUserRecord().removeSpiRecord(this.mResourceId);
        }

        protected ResourceTracker getResourceTracker() {
            return getUserRecord().mSpiQuotaTracker;
        }

        public String toString() {
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("{super=");
            strBuilder.append(super.toString());
            strBuilder.append(", mSpi=");
            strBuilder.append(this.mSpi);
            strBuilder.append(", mSourceAddress=");
            strBuilder.append(this.mSourceAddress);
            strBuilder.append(", mDestinationAddress=");
            strBuilder.append(this.mDestinationAddress);
            strBuilder.append(", mOwnedByTransform=");
            strBuilder.append(this.mOwnedByTransform);
            strBuilder.append("}");
            return strBuilder.toString();
        }
    }

    private final class TransformRecord extends OwnedResourceRecord {
        private final IpSecConfig mConfig;
        private final EncapSocketRecord mSocket;
        private final SpiRecord mSpi;

        TransformRecord(int resourceId, IpSecConfig config, SpiRecord spi, EncapSocketRecord socket) {
            super(resourceId);
            this.mConfig = config;
            this.mSpi = spi;
            this.mSocket = socket;
            spi.setOwnedByTransform();
        }

        public IpSecConfig getConfig() {
            return this.mConfig;
        }

        public SpiRecord getSpiRecord() {
            return this.mSpi;
        }

        public EncapSocketRecord getSocketRecord() {
            return this.mSocket;
        }

        public void freeUnderlyingResources() {
            try {
                IpSecService.this.mSrvConfig.getNetdInstance().ipSecDeleteSecurityAssociation(this.mResourceId, this.mConfig.getSourceAddress(), this.mConfig.getDestinationAddress(), this.mSpi.getSpi(), this.mConfig.getMarkValue(), this.mConfig.getMarkMask());
            } catch (RemoteException | ServiceSpecificException e) {
                String str = IpSecService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to delete SA with ID: ");
                stringBuilder.append(this.mResourceId);
                Log.e(str, stringBuilder.toString(), e);
            }
            getResourceTracker().give();
        }

        public void invalidate() throws RemoteException {
            getUserRecord().removeTransformRecord(this.mResourceId);
        }

        protected ResourceTracker getResourceTracker() {
            return getUserRecord().mTransformQuotaTracker;
        }

        public String toString() {
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("{super=");
            strBuilder.append(super.toString());
            strBuilder.append(", mSocket=");
            strBuilder.append(this.mSocket);
            strBuilder.append(", mSpi.mResourceId=");
            strBuilder.append(this.mSpi.mResourceId);
            strBuilder.append(", mConfig=");
            strBuilder.append(this.mConfig);
            strBuilder.append("}");
            return strBuilder.toString();
        }
    }

    private final class TunnelInterfaceRecord extends OwnedResourceRecord {
        private final int mIkey;
        private final String mInterfaceName;
        private final String mLocalAddress;
        private final int mOkey;
        private final String mRemoteAddress;
        private final Network mUnderlyingNetwork;

        TunnelInterfaceRecord(int resourceId, String interfaceName, Network underlyingNetwork, String localAddr, String remoteAddr, int ikey, int okey) {
            super(resourceId);
            this.mInterfaceName = interfaceName;
            this.mUnderlyingNetwork = underlyingNetwork;
            this.mLocalAddress = localAddr;
            this.mRemoteAddress = remoteAddr;
            this.mIkey = ikey;
            this.mOkey = okey;
        }

        public void freeUnderlyingResources() {
            try {
                IpSecService.this.mSrvConfig.getNetdInstance().removeVirtualTunnelInterface(this.mInterfaceName);
                for (String wildcardAddr : IpSecService.WILDCARD_ADDRESSES) {
                    for (int direction : IpSecService.DIRECTIONS) {
                        IpSecService.this.mSrvConfig.getNetdInstance().ipSecDeleteSecurityPolicy(0, direction, wildcardAddr, wildcardAddr, direction == 0 ? this.mIkey : this.mOkey, -1);
                    }
                }
            } catch (RemoteException | ServiceSpecificException e) {
                String str = IpSecService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to delete VTI with interface name: ");
                stringBuilder.append(this.mInterfaceName);
                stringBuilder.append(" and id: ");
                stringBuilder.append(this.mResourceId);
                Log.e(str, stringBuilder.toString(), e);
            }
            getResourceTracker().give();
            IpSecService.this.releaseNetId(this.mIkey);
            IpSecService.this.releaseNetId(this.mOkey);
        }

        public String getInterfaceName() {
            return this.mInterfaceName;
        }

        public Network getUnderlyingNetwork() {
            return this.mUnderlyingNetwork;
        }

        public String getLocalAddress() {
            return this.mLocalAddress;
        }

        public String getRemoteAddress() {
            return this.mRemoteAddress;
        }

        public int getIkey() {
            return this.mIkey;
        }

        public int getOkey() {
            return this.mOkey;
        }

        protected ResourceTracker getResourceTracker() {
            return getUserRecord().mTunnelQuotaTracker;
        }

        public void invalidate() {
            getUserRecord().removeTunnelInterfaceRecord(this.mResourceId);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{super=");
            stringBuilder.append(super.toString());
            stringBuilder.append(", mInterfaceName=");
            stringBuilder.append(this.mInterfaceName);
            stringBuilder.append(", mUnderlyingNetwork=");
            stringBuilder.append(this.mUnderlyingNetwork);
            stringBuilder.append(", mLocalAddress=");
            stringBuilder.append(this.mLocalAddress);
            stringBuilder.append(", mRemoteAddress=");
            stringBuilder.append(this.mRemoteAddress);
            stringBuilder.append(", mIkey=");
            stringBuilder.append(this.mIkey);
            stringBuilder.append(", mOkey=");
            stringBuilder.append(this.mOkey);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    static {
        try {
            INADDR_ANY = InetAddress.getByAddress(new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0});
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    int reserveNetId() {
        synchronized (this.mTunnelNetIds) {
            int i = 0;
            while (i < 1024) {
                try {
                    int netId = TUN_INTF_NETID_START + this.mNextTunnelNetIdIndex;
                    int i2 = this.mNextTunnelNetIdIndex + 1;
                    this.mNextTunnelNetIdIndex = i2;
                    if (i2 >= 1024) {
                        this.mNextTunnelNetIdIndex = 0;
                    }
                    if (this.mTunnelNetIds.get(netId)) {
                        i++;
                    } else {
                        this.mTunnelNetIds.put(netId, true);
                        return netId;
                    }
                } catch (Throwable th) {
                    while (true) {
                        throw th;
                    }
                }
            }
            throw new IllegalStateException("No free netIds to allocate");
        }
    }

    @VisibleForTesting
    void releaseNetId(int netId) {
        synchronized (this.mTunnelNetIds) {
            this.mTunnelNetIds.delete(netId);
        }
    }

    private IpSecService(Context context) {
        this(context, IpSecServiceConfiguration.GETSRVINSTANCE);
    }

    static IpSecService create(Context context) throws InterruptedException {
        IpSecService service = new IpSecService(context);
        service.connectNativeNetdService();
        return service;
    }

    private AppOpsManager getAppOpsManager() {
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService("appops");
        if (appOps != null) {
            return appOps;
        }
        throw new RuntimeException("System Server couldn't get AppOps");
    }

    @VisibleForTesting
    public IpSecService(Context context, IpSecServiceConfiguration config) {
        this(context, config, -$$Lambda$IpSecService$AnqunmSwm_yQvDDEPg-gokhVs5M.INSTANCE);
    }

    static /* synthetic */ void lambda$new$0(FileDescriptor fd, int uid) throws IOException {
        try {
            TrafficStats.setThreadStatsUid(uid);
            TrafficStats.tagFileDescriptor(fd);
        } finally {
            TrafficStats.clearThreadStatsUid();
        }
    }

    @VisibleForTesting
    public IpSecService(Context context, IpSecServiceConfiguration config, UidFdTagger uidFdTagger) {
        this.mNextResourceId = 1;
        this.mUserResourceTracker = new UserResourceTracker();
        this.mTunnelNetIds = new SparseBooleanArray();
        this.mNextTunnelNetIdIndex = 0;
        this.mContext = context;
        this.mSrvConfig = config;
        this.mUidFdTagger = uidFdTagger;
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

    synchronized boolean isNetdAlive() {
        try {
            INetd netd = this.mSrvConfig.getNetdInstance();
            if (netd == null) {
                return false;
            }
            return netd.isAlive();
        } catch (RemoteException e) {
            return false;
        }
    }

    private static void checkInetAddress(String inetAddress) {
        if (TextUtils.isEmpty(inetAddress)) {
            throw new IllegalArgumentException("Unspecified address");
        } else if (NetworkUtils.numericToInetAddress(inetAddress).isAnyLocalAddress()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Inappropriate wildcard address: ");
            stringBuilder.append(inetAddress);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static void checkDirection(int direction) {
        switch (direction) {
            case 0:
            case 1:
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid Direction: ");
                stringBuilder.append(direction);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:48:0x00b1 A:{SYNTHETIC, Splitter:B:48:0x00b1} */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00a8 A:{Catch:{ ServiceSpecificException -> 0x009e, RemoteException -> 0x0095 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized IpSecSpiResponse allocateSecurityParameterIndex(String destinationAddress, int requestedSpi, IBinder binder) throws RemoteException {
        ServiceSpecificException e;
        RemoteException e2;
        int i = requestedSpi;
        IBinder iBinder = binder;
        synchronized (this) {
            checkInetAddress(destinationAddress);
            if (i > 0) {
                if (i < 256) {
                    throw new IllegalArgumentException("ESP SPI must not be in the range of 0-255.");
                }
            }
            Preconditions.checkNotNull(iBinder, "Null Binder passed to allocateSecurityParameterIndex");
            UserRecord userRecord = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
            int i2 = this.mNextResourceId;
            this.mNextResourceId = i2 + 1;
            int resourceId = i2;
            int spi = 0;
            String str;
            int spi2;
            IpSecSpiResponse ipSecSpiResponse;
            try {
                if (userRecord.mSpiQuotaTracker.isAvailable()) {
                    str = destinationAddress;
                    try {
                        spi2 = this.mSrvConfig.getNetdInstance().ipSecAllocateSpi(resourceId, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, str, i);
                        try {
                            String str2 = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Allocated SPI ");
                            stringBuilder.append(spi2);
                            Log.d(str2, stringBuilder.toString());
                            RefcountedResourceArray refcountedResourceArray = userRecord.mSpiRecords;
                            SpiRecord spiRecord = r1;
                            SpiRecord spiRecord2 = new SpiRecord(resourceId, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, str, spi2);
                            refcountedResourceArray.put(resourceId, new RefcountedResource(spiRecord, iBinder, new RefcountedResource[0]));
                            ipSecSpiResponse = new IpSecSpiResponse(0, resourceId, spi2);
                            return ipSecSpiResponse;
                        } catch (ServiceSpecificException e3) {
                            e = e3;
                        } catch (RemoteException e4) {
                            e2 = e4;
                            throw e2.rethrowFromSystemServer();
                        }
                    } catch (ServiceSpecificException e5) {
                        e = e5;
                        spi2 = spi;
                        if (e.errorCode != OsConstants.ENOENT) {
                        }
                    } catch (RemoteException e6) {
                        e2 = e6;
                        throw e2.rethrowFromSystemServer();
                    }
                }
                IpSecSpiResponse ipSecSpiResponse2 = new IpSecSpiResponse(1, -1, spi);
                return ipSecSpiResponse2;
            } catch (ServiceSpecificException e7) {
                e = e7;
                str = destinationAddress;
                spi2 = spi;
                if (e.errorCode != OsConstants.ENOENT) {
                    ipSecSpiResponse = new IpSecSpiResponse(2, -1, spi2);
                    return ipSecSpiResponse;
                }
                throw e;
            } catch (RemoteException e8) {
                e2 = e8;
                str = destinationAddress;
                throw e2.rethrowFromSystemServer();
            }
        }
    }

    private void releaseResource(RefcountedResourceArray resArray, int resourceId) throws RemoteException {
        resArray.getRefcountedResourceOrThrow(resourceId).userRelease();
    }

    public synchronized void releaseSecurityParameterIndex(int resourceId) throws RemoteException {
        releaseResource(this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mSpiRecords, resourceId);
    }

    private int bindToRandomPort(FileDescriptor sockFd) throws IOException {
        int i = 10;
        while (i > 0) {
            try {
                FileDescriptor probeSocket = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
                Os.bind(probeSocket, INADDR_ANY, 0);
                int port = ((InetSocketAddress) Os.getsockname(probeSocket)).getPort();
                Os.close(probeSocket);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Binding to port ");
                stringBuilder.append(port);
                Log.v(str, stringBuilder.toString());
                Os.bind(sockFd, INADDR_ANY, port);
                return port;
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
        if (port == 0 || (port >= 1024 && port <= 65535)) {
            Preconditions.checkNotNull(binder, "Null Binder passed to openUdpEncapsulationSocket");
            int callingUid = Binder.getCallingUid();
            UserRecord userRecord = this.mUserResourceTracker.getUserRecord(callingUid);
            int resourceId = this.mNextResourceId;
            this.mNextResourceId = resourceId + 1;
            try {
                if (userRecord.mSocketQuotaTracker.isAvailable()) {
                    FileDescriptor sockFd = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
                    this.mUidFdTagger.tag(sockFd, callingUid);
                    Os.setsockoptInt(sockFd, OsConstants.IPPROTO_UDP, OsConstants.UDP_ENCAP, OsConstants.UDP_ENCAP_ESPINUDP);
                    this.mSrvConfig.getNetdInstance().ipSecSetEncapSocketOwner(sockFd, callingUid);
                    if (port != 0) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Binding to port ");
                        stringBuilder.append(port);
                        Log.v(str, stringBuilder.toString());
                        Os.bind(sockFd, INADDR_ANY, port);
                    } else {
                        port = bindToRandomPort(sockFd);
                    }
                    userRecord.mEncapSocketRecords.put(resourceId, new RefcountedResource(new EncapSocketRecord(resourceId, sockFd, port), binder, new RefcountedResource[0]));
                    return new IpSecUdpEncapResponse(0, resourceId, port, sockFd);
                }
                return new IpSecUdpEncapResponse(1);
            } catch (ErrnoException | IOException e) {
                IoUtils.closeQuietly(null);
                return new IpSecUdpEncapResponse(1);
            }
        }
        throw new IllegalArgumentException("Specified port number must be a valid non-reserved UDP port");
    }

    public synchronized void closeUdpEncapsulationSocket(int resourceId) throws RemoteException {
        releaseResource(this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mEncapSocketRecords, resourceId);
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x0113 A:{ExcHandler: Throwable (th java.lang.Throwable), Splitter:B:12:0x005d} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:58:0x0113, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:59:0x0114, code skipped:
            r1 = r6;
            r26 = r8;
            r27 = r14;
            r14 = r9;
     */
    /* JADX WARNING: Missing block: B:63:0x0123, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:64:0x0124, code skipped:
            r1 = r6;
            r2 = r8;
            r27 = r14;
            r14 = r9;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized IpSecTunnelInterfaceResponse createTunnelInterface(String localAddr, String remoteAddr, Network underlyingNetwork, IBinder binder, String callingPackage) {
        RemoteException e;
        int okey;
        UserRecord userRecord;
        String str;
        int userRecord2;
        Throwable t;
        int okey2;
        IBinder iBinder = binder;
        synchronized (this) {
            enforceTunnelPermissions(callingPackage);
            Preconditions.checkNotNull(iBinder, "Null Binder passed to createTunnelInterface");
            Network network = underlyingNetwork;
            Preconditions.checkNotNull(network, "No underlying network was specified");
            checkInetAddress(localAddr);
            checkInetAddress(remoteAddr);
            UserRecord userRecord3 = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
            int i = 1;
            IpSecTunnelInterfaceResponse ipSecTunnelInterfaceResponse;
            if (userRecord3.mTunnelQuotaTracker.isAvailable()) {
                int i2 = this.mNextResourceId;
                this.mNextResourceId = i2 + 1;
                int resourceId = i2;
                int ikey = reserveNetId();
                int okey3 = reserveNetId();
                String intfName = String.format("%s%d", new Object[]{INetd.IPSEC_INTERFACE_PREFIX, Integer.valueOf(resourceId)});
                try {
                    RefcountedResourceArray refcountedResourceArray;
                    TunnelInterfaceRecord tunnelInterfaceRecord;
                    Network network2;
                    TunnelInterfaceRecord tunnelInterfaceRecord2;
                    String intfName2 = intfName;
                    i2 = 0;
                    try {
                        this.mSrvConfig.getNetdInstance().addVirtualTunnelInterface(intfName, localAddr, remoteAddr, ikey, okey3);
                        String[] strArr = WILDCARD_ADDRESSES;
                        int length = strArr.length;
                        int i3 = i2;
                        while (i3 < length) {
                            try {
                                String wildcardAddr = strArr[i3];
                                int[] iArr = DIRECTIONS;
                                int length2 = iArr.length;
                                int i4 = i2;
                                while (i4 < length2) {
                                    i2 = iArr[i4];
                                    this.mSrvConfig.getNetdInstance().ipSecAddSecurityPolicy(0, i2, wildcardAddr, wildcardAddr, 0, i2 == i ? okey3 : ikey, -1);
                                    i4++;
                                    i = 1;
                                }
                                i3++;
                                i2 = 0;
                                i = 1;
                            } catch (RemoteException e2) {
                                e = e2;
                                okey = okey3;
                                userRecord = userRecord3;
                                str = intfName2;
                                userRecord2 = ikey;
                                releaseNetId(userRecord2);
                                releaseNetId(okey);
                                throw e.rethrowFromSystemServer();
                            } catch (Throwable th) {
                                t = th;
                                okey2 = okey3;
                                userRecord = userRecord3;
                                str = intfName2;
                                userRecord3 = ikey;
                                releaseNetId(userRecord3);
                                releaseNetId(okey2);
                                throw t;
                            }
                        }
                        refcountedResourceArray = userRecord3.mTunnelInterfaceRecords;
                        tunnelInterfaceRecord = tunnelInterfaceRecord;
                        network2 = network;
                        tunnelInterfaceRecord2 = tunnelInterfaceRecord;
                        RefcountedResource refcountedResource = refcountedResource;
                        okey2 = okey3;
                        userRecord2 = ikey;
                    } catch (RemoteException e3) {
                        e = e3;
                        userRecord = userRecord3;
                        str = intfName2;
                        userRecord2 = ikey;
                        okey = okey3;
                        releaseNetId(userRecord2);
                        releaseNetId(okey);
                        throw e.rethrowFromSystemServer();
                    } catch (Throwable th2) {
                        t = th2;
                        okey2 = okey3;
                        userRecord = userRecord3;
                        str = intfName2;
                        userRecord3 = ikey;
                        releaseNetId(userRecord3);
                        releaseNetId(okey2);
                        throw t;
                    }
                    try {
                        tunnelInterfaceRecord = new TunnelInterfaceRecord(resourceId, intfName2, network2, localAddr, remoteAddr, ikey, okey2);
                        refcountedResourceArray.put(resourceId, new RefcountedResource(tunnelInterfaceRecord2, iBinder, new RefcountedResource[0]));
                        try {
                            ipSecTunnelInterfaceResponse = new IpSecTunnelInterfaceResponse(0, resourceId, intfName2);
                            return ipSecTunnelInterfaceResponse;
                        } catch (RemoteException e4) {
                            e = e4;
                            okey = okey2;
                            releaseNetId(userRecord2);
                            releaseNetId(okey);
                            throw e.rethrowFromSystemServer();
                        } catch (Throwable th3) {
                            t = th3;
                            releaseNetId(userRecord3);
                            releaseNetId(okey2);
                            throw t;
                        }
                    } catch (RemoteException e5) {
                        e = e5;
                        str = intfName2;
                        okey = okey2;
                        releaseNetId(userRecord2);
                        releaseNetId(okey);
                        throw e.rethrowFromSystemServer();
                    } catch (Throwable th4) {
                        t = th4;
                        str = intfName2;
                        releaseNetId(userRecord3);
                        releaseNetId(okey2);
                        throw t;
                    }
                } catch (RemoteException e6) {
                    e = e6;
                    str = intfName;
                    userRecord = userRecord3;
                    userRecord2 = ikey;
                    okey = okey3;
                    releaseNetId(userRecord2);
                    releaseNetId(okey);
                    throw e.rethrowFromSystemServer();
                } catch (Throwable th5) {
                }
            } else {
                ipSecTunnelInterfaceResponse = new IpSecTunnelInterfaceResponse(1);
                return ipSecTunnelInterfaceResponse;
            }
        }
    }

    public synchronized void addAddressToTunnelInterface(int tunnelResourceId, LinkAddress localAddr, String callingPackage) {
        enforceTunnelPermissions(callingPackage);
        try {
            this.mSrvConfig.getNetdInstance().interfaceAddAddress(((TunnelInterfaceRecord) this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mTunnelInterfaceRecords.getResourceOrThrow(tunnelResourceId)).mInterfaceName, localAddr.getAddress().getHostAddress(), localAddr.getPrefixLength());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public synchronized void removeAddressFromTunnelInterface(int tunnelResourceId, LinkAddress localAddr, String callingPackage) {
        enforceTunnelPermissions(callingPackage);
        try {
            this.mSrvConfig.getNetdInstance().interfaceDelAddress(((TunnelInterfaceRecord) this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mTunnelInterfaceRecords.getResourceOrThrow(tunnelResourceId)).mInterfaceName, localAddr.getAddress().getHostAddress(), localAddr.getPrefixLength());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public synchronized void deleteTunnelInterface(int resourceId, String callingPackage) throws RemoteException {
        enforceTunnelPermissions(callingPackage);
        releaseResource(this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mTunnelInterfaceRecords, resourceId);
    }

    @VisibleForTesting
    void validateAlgorithms(IpSecConfig config) throws IllegalArgumentException {
        IpSecAlgorithm auth = config.getAuthentication();
        IpSecAlgorithm crypt = config.getEncryption();
        IpSecAlgorithm aead = config.getAuthenticatedEncryption();
        boolean z = true;
        boolean z2 = (aead == null && crypt == null && auth == null) ? false : true;
        Preconditions.checkArgument(z2, "No Encryption or Authentication algorithms specified");
        z2 = auth == null || auth.isAuthentication();
        Preconditions.checkArgument(z2, "Unsupported algorithm for Authentication");
        z2 = crypt == null || crypt.isEncryption();
        Preconditions.checkArgument(z2, "Unsupported algorithm for Encryption");
        z2 = aead == null || aead.isAead();
        Preconditions.checkArgument(z2, "Unsupported algorithm for Authenticated Encryption");
        if (!(aead == null || (auth == null && crypt == null))) {
            z = false;
        }
        Preconditions.checkArgument(z, "Authenticated Encryption is mutually exclusive with other Authentication or Encryption algorithms");
    }

    private void checkIpSecConfig(IpSecConfig config) {
        StringBuilder stringBuilder;
        UserRecord userRecord = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
        switch (config.getEncapType()) {
            case 0:
                break;
            case 1:
            case 2:
                userRecord.mEncapSocketRecords.getResourceOrThrow(config.getEncapSocketResourceId());
                int port = config.getEncapRemotePort();
                if (port <= 0 || port > 65535) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid remote UDP port: ");
                    stringBuilder.append(port);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            default:
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid Encap Type: ");
                stringBuilder2.append(config.getEncapType());
                throw new IllegalArgumentException(stringBuilder2.toString());
        }
        validateAlgorithms(config);
        SpiRecord s = (SpiRecord) userRecord.mSpiRecords.getResourceOrThrow(config.getSpiResourceId());
        if (s.getOwnedByTransform()) {
            throw new IllegalStateException("SPI already in use; cannot be used in new Transforms");
        }
        if (TextUtils.isEmpty(config.getDestinationAddress())) {
            config.setDestinationAddress(s.getDestinationAddress());
        }
        if (config.getDestinationAddress().equals(s.getDestinationAddress())) {
            checkInetAddress(config.getDestinationAddress());
            checkInetAddress(config.getSourceAddress());
            switch (config.getMode()) {
                case 0:
                case 1:
                    return;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid IpSecTransform.mode: ");
                    stringBuilder.append(config.getMode());
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        throw new IllegalArgumentException("Mismatched remote addresseses.");
    }

    private void enforceTunnelPermissions(String callingPackage) {
        Preconditions.checkNotNull(callingPackage, "Null calling package cannot create IpSec tunnels");
        int noteOp = getAppOpsManager().noteOp(75, Binder.getCallingUid(), callingPackage);
        if (noteOp == 0) {
            return;
        }
        if (noteOp == 3) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_IPSEC_TUNNELS", TAG);
            return;
        }
        throw new SecurityException("Request to ignore AppOps for non-legacy API");
    }

    private void createOrUpdateTransform(IpSecConfig c, int resourceId, SpiRecord spiRecord, EncapSocketRecord socketRecord) throws RemoteException {
        int i;
        int truncationLengthBits;
        int truncationLengthBits2;
        int encapType = c.getEncapType();
        int encapLocalPort = 0;
        int encapRemotePort = 0;
        if (encapType != 0) {
            encapLocalPort = socketRecord.getPort();
            encapRemotePort = c.getEncapRemotePort();
        }
        int encapLocalPort2 = encapLocalPort;
        int encapRemotePort2 = encapRemotePort;
        IpSecAlgorithm auth = c.getAuthentication();
        IpSecAlgorithm crypt = c.getEncryption();
        IpSecAlgorithm authCrypt = c.getAuthenticatedEncryption();
        String name = crypt == null ? authCrypt == null ? "ecb(cipher_null)" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : crypt.getName();
        String cryptName = name;
        INetd netdInstance = this.mSrvConfig.getNetdInstance();
        int mode = c.getMode();
        String sourceAddress = c.getSourceAddress();
        String destinationAddress = c.getDestinationAddress();
        if (c.getNetwork() != null) {
            i = c.getNetwork().netId;
        } else {
            i = 0;
        }
        int spi = spiRecord.getSpi();
        int markValue = c.getMarkValue();
        int markMask = c.getMarkMask();
        String name2 = auth != null ? auth.getName() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        byte[] key = auth != null ? auth.getKey() : new byte[0];
        if (auth != null) {
            truncationLengthBits = auth.getTruncationLengthBits();
        } else {
            truncationLengthBits = 0;
        }
        byte[] key2 = crypt != null ? crypt.getKey() : new byte[0];
        if (crypt != null) {
            truncationLengthBits2 = crypt.getTruncationLengthBits();
        } else {
            truncationLengthBits2 = 0;
        }
        netdInstance.ipSecAddSecurityAssociation(resourceId, mode, sourceAddress, destinationAddress, i, spi, markValue, markMask, name2, key, truncationLengthBits, cryptName, key2, truncationLengthBits2, authCrypt != null ? authCrypt.getName() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, authCrypt != null ? authCrypt.getKey() : new byte[0], authCrypt != null ? authCrypt.getTruncationLengthBits() : 0, encapType, encapLocalPort2, encapRemotePort2);
    }

    public synchronized IpSecTransformResponse createTransform(IpSecConfig c, IBinder binder, String callingPackage) throws RemoteException {
        IBinder iBinder = binder;
        synchronized (this) {
            Preconditions.checkNotNull(c);
            if (c.getMode() == 1) {
                enforceTunnelPermissions(callingPackage);
            } else {
                String str = callingPackage;
            }
            checkIpSecConfig(c);
            Preconditions.checkNotNull(iBinder, "Null Binder passed to createTransform");
            int i = this.mNextResourceId;
            this.mNextResourceId = i + 1;
            int resourceId = i;
            UserRecord userRecord = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
            ArrayList dependencies = new ArrayList();
            IpSecTransformResponse ipSecTransformResponse;
            if (userRecord.mTransformQuotaTracker.isAvailable()) {
                EncapSocketRecord socketRecord = null;
                if (c.getEncapType() != 0) {
                    RefcountedResource<EncapSocketRecord> refcountedSocketRecord = userRecord.mEncapSocketRecords.getRefcountedResourceOrThrow(c.getEncapSocketResourceId());
                    dependencies.add(refcountedSocketRecord);
                    socketRecord = (EncapSocketRecord) refcountedSocketRecord.getResource();
                }
                EncapSocketRecord socketRecord2 = socketRecord;
                RefcountedResource<SpiRecord> refcountedSpiRecord = userRecord.mSpiRecords.getRefcountedResourceOrThrow(c.getSpiResourceId());
                dependencies.add(refcountedSpiRecord);
                SpiRecord spiRecord = (SpiRecord) refcountedSpiRecord.getResource();
                IpSecConfig ipSecConfig = c;
                createOrUpdateTransform(ipSecConfig, resourceId, spiRecord, socketRecord2);
                TransformRecord transformRecord = r1;
                RefcountedResourceArray refcountedResourceArray = userRecord.mTransformRecords;
                TransformRecord transformRecord2 = new TransformRecord(resourceId, ipSecConfig, spiRecord, socketRecord2);
                refcountedResourceArray.put(resourceId, new RefcountedResource(transformRecord, iBinder, (RefcountedResource[]) dependencies.toArray(new RefcountedResource[dependencies.size()])));
                ipSecTransformResponse = new IpSecTransformResponse(0, resourceId);
                return ipSecTransformResponse;
            }
            ipSecTransformResponse = new IpSecTransformResponse(1);
            return ipSecTransformResponse;
        }
    }

    public synchronized void deleteTransform(int resourceId) throws RemoteException {
        releaseResource(this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mTransformRecords, resourceId);
    }

    public synchronized void applyTransportModeTransform(ParcelFileDescriptor socket, int direction, int resourceId) throws RemoteException {
        UserRecord userRecord = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
        checkDirection(direction);
        TransformRecord info = (TransformRecord) userRecord.mTransformRecords.getResourceOrThrow(resourceId);
        if (info.pid == getCallingPid() && info.uid == getCallingUid()) {
            IpSecConfig c = info.getConfig();
            Preconditions.checkArgument(c.getMode() == 0, "Transform mode was not Transport mode; cannot be applied to a socket");
            this.mSrvConfig.getNetdInstance().ipSecApplyTransportModeTransform(socket.getFileDescriptor(), resourceId, direction, c.getSourceAddress(), c.getDestinationAddress(), info.getSpiRecord().getSpi());
        } else {
            throw new SecurityException("Only the owner of an IpSec Transform may apply it!");
        }
    }

    public synchronized void removeTransportModeTransforms(ParcelFileDescriptor socket) throws RemoteException {
        this.mSrvConfig.getNetdInstance().ipSecRemoveTransportModeTransform(socket.getFileDescriptor());
    }

    /* JADX WARNING: Removed duplicated region for block: B:44:0x0109  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x00ff  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x00ff  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x0109  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void applyTunnelModeTransform(int tunnelResourceId, int direction, int transformResourceId, String callingPackage) throws RemoteException {
        ServiceSpecificException e;
        SpiRecord spiRecord;
        IpSecConfig ipSecConfig;
        int i = direction;
        int i2 = transformResourceId;
        synchronized (this) {
            int ikey;
            enforceTunnelPermissions(callingPackage);
            checkDirection(direction);
            UserRecord userRecord = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
            TransformRecord transformInfo = (TransformRecord) userRecord.mTransformRecords.getResourceOrThrow(i2);
            TunnelInterfaceRecord tunnelInterfaceInfo = (TunnelInterfaceRecord) userRecord.mTunnelInterfaceRecords.getResourceOrThrow(tunnelResourceId);
            IpSecConfig c = transformInfo.getConfig();
            int i3 = 0;
            Preconditions.checkArgument(c.getMode() == 1, "Transform mode was not Tunnel mode; cannot be applied to a tunnel interface");
            EncapSocketRecord socketRecord = null;
            if (c.getEncapType() != 0) {
                socketRecord = (EncapSocketRecord) userRecord.mEncapSocketRecords.getResourceOrThrow(c.getEncapSocketResourceId());
            }
            EncapSocketRecord socketRecord2 = socketRecord;
            SpiRecord spiRecord2 = (SpiRecord) userRecord.mSpiRecords.getResourceOrThrow(c.getSpiResourceId());
            if (i == 0) {
                ikey = tunnelInterfaceInfo.getIkey();
            } else {
                ikey = tunnelInterfaceInfo.getOkey();
            }
            int mark = ikey;
            int mark2;
            TunnelInterfaceRecord tunnelInterfaceInfo2;
            try {
                c.setMarkValue(mark);
                c.setMarkMask(-1);
                if (i == 1) {
                    try {
                        c.setNetwork(tunnelInterfaceInfo.getUnderlyingNetwork());
                        String[] strArr = WILDCARD_ADDRESSES;
                        int length = strArr.length;
                        while (i3 < length) {
                            String wildcardAddr = strArr[i3];
                            String[] strArr2 = strArr;
                            int i4 = i3;
                            ikey = length;
                            mark2 = mark;
                            SpiRecord spiRecord3 = spiRecord2;
                            EncapSocketRecord socketRecord3 = socketRecord2;
                            IpSecConfig c2 = c;
                            tunnelInterfaceInfo2 = tunnelInterfaceInfo;
                            try {
                                this.mSrvConfig.getNetdInstance().ipSecUpdateSecurityPolicy(0, i, wildcardAddr, wildcardAddr, transformInfo.getSpiRecord().getSpi(), mark2, -1);
                                i3 = i4 + 1;
                                length = ikey;
                                strArr = strArr2;
                                mark = mark2;
                                tunnelInterfaceInfo = tunnelInterfaceInfo2;
                                spiRecord2 = spiRecord3;
                                socketRecord2 = socketRecord3;
                                c = c2;
                            } catch (ServiceSpecificException e2) {
                                e = e2;
                                spiRecord = spiRecord3;
                                EncapSocketRecord encapSocketRecord = socketRecord3;
                                ipSecConfig = c2;
                                if (e.errorCode == OsConstants.EINVAL) {
                                    throw new IllegalArgumentException(e.toString());
                                }
                                throw e;
                            }
                        }
                    } catch (ServiceSpecificException e3) {
                        e = e3;
                        mark2 = mark;
                        tunnelInterfaceInfo2 = tunnelInterfaceInfo;
                        spiRecord = spiRecord2;
                        mark = socketRecord2;
                        ipSecConfig = c;
                        if (e.errorCode == OsConstants.EINVAL) {
                        }
                    }
                }
                tunnelInterfaceInfo2 = tunnelInterfaceInfo;
                try {
                    createOrUpdateTransform(c, i2, spiRecord2, socketRecord2);
                } catch (ServiceSpecificException e4) {
                    e = e4;
                }
            } catch (ServiceSpecificException e5) {
                e = e5;
                mark2 = mark;
                spiRecord = spiRecord2;
                ipSecConfig = c;
                tunnelInterfaceInfo2 = tunnelInterfaceInfo;
                if (e.errorCode == OsConstants.EINVAL) {
                }
            }
        }
    }

    protected synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        pw.println("IpSecService dump:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NetdNativeService Connection: ");
        stringBuilder.append(isNetdAlive() ? "alive" : "dead");
        pw.println(stringBuilder.toString());
        pw.println();
        pw.println("mUserResourceTracker:");
        pw.println(this.mUserResourceTracker);
    }
}
