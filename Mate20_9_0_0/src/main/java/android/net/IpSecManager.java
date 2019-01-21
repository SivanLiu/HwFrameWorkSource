package android.net;

import android.content.Context;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.AndroidException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public final class IpSecManager {
    public static final int DIRECTION_IN = 0;
    public static final int DIRECTION_OUT = 1;
    public static final int INVALID_RESOURCE_ID = -1;
    public static final int INVALID_SECURITY_PARAMETER_INDEX = 0;
    private static final String TAG = "IpSecManager";
    private final Context mContext;
    private final IIpSecService mService;

    public static final class IpSecTunnelInterface implements AutoCloseable {
        private final CloseGuard mCloseGuard;
        private String mInterfaceName;
        private final InetAddress mLocalAddress;
        private final String mOpPackageName;
        private final InetAddress mRemoteAddress;
        private int mResourceId;
        private final IIpSecService mService;
        private final Network mUnderlyingNetwork;

        public String getInterfaceName() {
            return this.mInterfaceName;
        }

        public void addAddress(InetAddress address, int prefixLen) throws IOException {
            try {
                this.mService.addAddressToTunnelInterface(this.mResourceId, new LinkAddress(address, prefixLen), this.mOpPackageName);
            } catch (ServiceSpecificException e) {
                throw IpSecManager.rethrowCheckedExceptionFromServiceSpecificException(e);
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }

        public void removeAddress(InetAddress address, int prefixLen) throws IOException {
            try {
                this.mService.removeAddressFromTunnelInterface(this.mResourceId, new LinkAddress(address, prefixLen), this.mOpPackageName);
            } catch (ServiceSpecificException e) {
                throw IpSecManager.rethrowCheckedExceptionFromServiceSpecificException(e);
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }

        private IpSecTunnelInterface(Context ctx, IIpSecService service, InetAddress localAddress, InetAddress remoteAddress, Network underlyingNetwork) throws ResourceUnavailableException, IOException {
            this.mCloseGuard = CloseGuard.get();
            this.mResourceId = -1;
            this.mOpPackageName = ctx.getOpPackageName();
            this.mService = service;
            this.mLocalAddress = localAddress;
            this.mRemoteAddress = remoteAddress;
            this.mUnderlyingNetwork = underlyingNetwork;
            try {
                IpSecTunnelInterfaceResponse result = this.mService.createTunnelInterface(localAddress.getHostAddress(), remoteAddress.getHostAddress(), underlyingNetwork, new Binder(), this.mOpPackageName);
                switch (result.status) {
                    case 0:
                        this.mResourceId = result.resourceId;
                        this.mInterfaceName = result.interfaceName;
                        this.mCloseGuard.open("constructor");
                        return;
                    case 1:
                        throw new ResourceUnavailableException("No more tunnel interfaces may be allocated by this requester.");
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown status returned by IpSecService: ");
                        stringBuilder.append(result.status);
                        throw new RuntimeException(stringBuilder.toString());
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
            throw e.rethrowFromSystemServer();
        }

        public void close() {
            try {
                this.mService.deleteTunnelInterface(this.mResourceId, this.mOpPackageName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (Exception e2) {
                String str = IpSecManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to close ");
                stringBuilder.append(this);
                stringBuilder.append(", Exception=");
                stringBuilder.append(e2);
                Log.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                this.mResourceId = -1;
                this.mCloseGuard.close();
            }
            this.mResourceId = -1;
            this.mCloseGuard.close();
        }

        protected void finalize() throws Throwable {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            close();
        }

        @VisibleForTesting
        public int getResourceId() {
            return this.mResourceId;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IpSecTunnelInterface{ifname=");
            stringBuilder.append(this.mInterfaceName);
            stringBuilder.append(",resourceId=");
            stringBuilder.append(this.mResourceId);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PolicyDirection {
    }

    public static final class SecurityParameterIndex implements AutoCloseable {
        private final CloseGuard mCloseGuard;
        private final InetAddress mDestinationAddress;
        private int mResourceId;
        private final IIpSecService mService;
        private int mSpi;

        public int getSpi() {
            return this.mSpi;
        }

        public void close() {
            try {
                this.mService.releaseSecurityParameterIndex(this.mResourceId);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (Exception e2) {
                String str = IpSecManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to close ");
                stringBuilder.append(this);
                stringBuilder.append(", Exception=");
                stringBuilder.append(e2);
                Log.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                this.mResourceId = -1;
                this.mCloseGuard.close();
            }
            this.mResourceId = -1;
            this.mCloseGuard.close();
        }

        protected void finalize() throws Throwable {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            close();
        }

        private SecurityParameterIndex(IIpSecService service, InetAddress destinationAddress, int spi) throws ResourceUnavailableException, SpiUnavailableException {
            this.mCloseGuard = CloseGuard.get();
            this.mSpi = 0;
            this.mResourceId = -1;
            this.mService = service;
            this.mDestinationAddress = destinationAddress;
            try {
                IpSecSpiResponse result = this.mService.allocateSecurityParameterIndex(destinationAddress.getHostAddress(), spi, new Binder());
                if (result != null) {
                    int status = result.status;
                    StringBuilder stringBuilder;
                    switch (status) {
                        case 0:
                            this.mSpi = result.spi;
                            this.mResourceId = result.resourceId;
                            if (this.mSpi == 0) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Invalid SPI returned by IpSecService: ");
                                stringBuilder.append(status);
                                throw new RuntimeException(stringBuilder.toString());
                            } else if (this.mResourceId != -1) {
                                this.mCloseGuard.open("open");
                                return;
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Invalid Resource ID returned by IpSecService: ");
                                stringBuilder.append(status);
                                throw new RuntimeException(stringBuilder.toString());
                            }
                        case 1:
                            throw new ResourceUnavailableException("No more SPIs may be allocated by this requester.");
                        case 2:
                            throw new SpiUnavailableException("Requested SPI is unavailable", spi);
                        default:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown status returned by IpSecService: ");
                            stringBuilder.append(status);
                            throw new RuntimeException(stringBuilder.toString());
                    }
                    throw e.rethrowFromSystemServer();
                }
                throw new NullPointerException("Received null response from IpSecService");
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @VisibleForTesting
        public int getResourceId() {
            return this.mResourceId;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SecurityParameterIndex{spi=");
            stringBuilder.append(this.mSpi);
            stringBuilder.append(",resourceId=");
            stringBuilder.append(this.mResourceId);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    public interface Status {
        public static final int OK = 0;
        public static final int RESOURCE_UNAVAILABLE = 1;
        public static final int SPI_UNAVAILABLE = 2;
    }

    public static final class UdpEncapsulationSocket implements AutoCloseable {
        private final CloseGuard mCloseGuard;
        private final ParcelFileDescriptor mPfd;
        private final int mPort;
        private int mResourceId;
        private final IIpSecService mService;

        private UdpEncapsulationSocket(IIpSecService service, int port) throws ResourceUnavailableException, IOException {
            this.mResourceId = -1;
            this.mCloseGuard = CloseGuard.get();
            this.mService = service;
            try {
                IpSecUdpEncapResponse result = this.mService.openUdpEncapsulationSocket(port, new Binder());
                switch (result.status) {
                    case 0:
                        this.mResourceId = result.resourceId;
                        this.mPort = result.port;
                        this.mPfd = result.fileDescriptor;
                        this.mCloseGuard.open("constructor");
                        return;
                    case 1:
                        throw new ResourceUnavailableException("No more Sockets may be allocated by this requester.");
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown status returned by IpSecService: ");
                        stringBuilder.append(result.status);
                        throw new RuntimeException(stringBuilder.toString());
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
            throw e.rethrowFromSystemServer();
        }

        public FileDescriptor getFileDescriptor() {
            if (this.mPfd == null) {
                return null;
            }
            return this.mPfd.getFileDescriptor();
        }

        public int getPort() {
            return this.mPort;
        }

        public void close() throws IOException {
            try {
                this.mService.closeUdpEncapsulationSocket(this.mResourceId);
                this.mResourceId = -1;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (Exception e2) {
                String str = IpSecManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to close ");
                stringBuilder.append(this);
                stringBuilder.append(", Exception=");
                stringBuilder.append(e2);
                Log.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                this.mResourceId = -1;
                this.mCloseGuard.close();
            }
            this.mResourceId = -1;
            this.mCloseGuard.close();
            try {
                this.mPfd.close();
            } catch (IOException e3) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to close UDP Encapsulation Socket with Port= ");
                stringBuilder2.append(this.mPort);
                Log.e(IpSecManager.TAG, stringBuilder2.toString());
                throw e3;
            }
        }

        protected void finalize() throws Throwable {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            close();
        }

        @VisibleForTesting
        public int getResourceId() {
            return this.mResourceId;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UdpEncapsulationSocket{port=");
            stringBuilder.append(this.mPort);
            stringBuilder.append(",resourceId=");
            stringBuilder.append(this.mResourceId);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    public static final class ResourceUnavailableException extends AndroidException {
        ResourceUnavailableException(String msg) {
            super(msg);
        }
    }

    public static final class SpiUnavailableException extends AndroidException {
        private final int mSpi;

        SpiUnavailableException(String msg, int spi) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append(" (spi: ");
            stringBuilder.append(spi);
            stringBuilder.append(")");
            super(stringBuilder.toString());
            this.mSpi = spi;
        }

        public int getSpi() {
            return this.mSpi;
        }
    }

    public SecurityParameterIndex allocateSecurityParameterIndex(InetAddress destinationAddress) throws ResourceUnavailableException {
        try {
            return new SecurityParameterIndex(this.mService, destinationAddress, 0);
        } catch (ServiceSpecificException e) {
            throw rethrowUncheckedExceptionFromServiceSpecificException(e);
        } catch (SpiUnavailableException e2) {
            throw new ResourceUnavailableException("No SPIs available");
        }
    }

    public SecurityParameterIndex allocateSecurityParameterIndex(InetAddress destinationAddress, int requestedSpi) throws SpiUnavailableException, ResourceUnavailableException {
        if (requestedSpi != 0) {
            try {
                return new SecurityParameterIndex(this.mService, destinationAddress, requestedSpi);
            } catch (ServiceSpecificException e) {
                throw rethrowUncheckedExceptionFromServiceSpecificException(e);
            }
        }
        throw new IllegalArgumentException("Requested SPI must be a valid (non-zero) SPI");
    }

    public void applyTransportModeTransform(Socket socket, int direction, IpSecTransform transform) throws IOException {
        socket.getSoLinger();
        applyTransportModeTransform(socket.getFileDescriptor$(), direction, transform);
    }

    public void applyTransportModeTransform(DatagramSocket socket, int direction, IpSecTransform transform) throws IOException {
        applyTransportModeTransform(socket.getFileDescriptor$(), direction, transform);
    }

    public void applyTransportModeTransform(FileDescriptor socket, int direction, IpSecTransform transform) throws IOException {
        ParcelFileDescriptor pfd;
        try {
            pfd = ParcelFileDescriptor.dup(socket);
            this.mService.applyTransportModeTransform(pfd, direction, transform.getResourceId());
            if (pfd != null) {
                $closeResource(null, pfd);
            }
        } catch (ServiceSpecificException e) {
            throw rethrowCheckedExceptionFromServiceSpecificException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        } catch (Throwable th) {
            if (pfd != null) {
                $closeResource(r1, pfd);
            }
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    public void removeTransportModeTransforms(Socket socket) throws IOException {
        socket.getSoLinger();
        removeTransportModeTransforms(socket.getFileDescriptor$());
    }

    public void removeTransportModeTransforms(DatagramSocket socket) throws IOException {
        removeTransportModeTransforms(socket.getFileDescriptor$());
    }

    public void removeTransportModeTransforms(FileDescriptor socket) throws IOException {
        ParcelFileDescriptor pfd;
        try {
            pfd = ParcelFileDescriptor.dup(socket);
            this.mService.removeTransportModeTransforms(pfd);
            if (pfd != null) {
                $closeResource(null, pfd);
            }
        } catch (ServiceSpecificException e) {
            throw rethrowCheckedExceptionFromServiceSpecificException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        } catch (Throwable th) {
            if (pfd != null) {
                $closeResource(r1, pfd);
            }
        }
    }

    public void removeTunnelModeTransform(Network net, IpSecTransform transform) {
    }

    public UdpEncapsulationSocket openUdpEncapsulationSocket(int port) throws IOException, ResourceUnavailableException {
        if (port != 0) {
            try {
                return new UdpEncapsulationSocket(this.mService, port);
            } catch (ServiceSpecificException e) {
                throw rethrowCheckedExceptionFromServiceSpecificException(e);
            }
        }
        throw new IllegalArgumentException("Specified port must be a valid port number!");
    }

    public UdpEncapsulationSocket openUdpEncapsulationSocket() throws IOException, ResourceUnavailableException {
        try {
            return new UdpEncapsulationSocket(this.mService, 0);
        } catch (ServiceSpecificException e) {
            throw rethrowCheckedExceptionFromServiceSpecificException(e);
        }
    }

    public IpSecTunnelInterface createIpSecTunnelInterface(InetAddress localAddress, InetAddress remoteAddress, Network underlyingNetwork) throws ResourceUnavailableException, IOException {
        try {
            return new IpSecTunnelInterface(this.mContext, this.mService, localAddress, remoteAddress, underlyingNetwork);
        } catch (ServiceSpecificException e) {
            throw rethrowCheckedExceptionFromServiceSpecificException(e);
        }
    }

    public void applyTunnelModeTransform(IpSecTunnelInterface tunnel, int direction, IpSecTransform transform) throws IOException {
        try {
            this.mService.applyTunnelModeTransform(tunnel.getResourceId(), direction, transform.getResourceId(), this.mContext.getOpPackageName());
        } catch (ServiceSpecificException e) {
            throw rethrowCheckedExceptionFromServiceSpecificException(e);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public IpSecManager(Context ctx, IIpSecService service) {
        this.mContext = ctx;
        this.mService = (IIpSecService) Preconditions.checkNotNull(service, "missing service");
    }

    private static void maybeHandleServiceSpecificException(ServiceSpecificException sse) {
        if (sse.errorCode == OsConstants.EINVAL) {
            throw new IllegalArgumentException(sse);
        } else if (sse.errorCode == OsConstants.EAGAIN) {
            throw new IllegalStateException(sse);
        } else if (sse.errorCode == OsConstants.EOPNOTSUPP) {
            throw new UnsupportedOperationException(sse);
        }
    }

    static RuntimeException rethrowUncheckedExceptionFromServiceSpecificException(ServiceSpecificException sse) {
        maybeHandleServiceSpecificException(sse);
        throw new RuntimeException(sse);
    }

    static IOException rethrowCheckedExceptionFromServiceSpecificException(ServiceSpecificException sse) throws IOException {
        maybeHandleServiceSpecificException(sse);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IpSec encountered errno=");
        stringBuilder.append(sse.errorCode);
        throw new ErrnoException(stringBuilder.toString(), sse.errorCode).rethrowAsIOException();
    }
}
