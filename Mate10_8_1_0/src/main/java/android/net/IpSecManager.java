package android.net;

import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.AndroidException;
import android.util.Log;
import com.android.internal.util.Preconditions;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public final class IpSecManager {
    public static final int INVALID_RESOURCE_ID = 0;
    public static final int INVALID_SECURITY_PARAMETER_INDEX = 0;
    private static final String TAG = "IpSecManager";
    private final IIpSecService mService;

    public static final class ResourceUnavailableException extends AndroidException {
        ResourceUnavailableException(String msg) {
            super(msg);
        }
    }

    public static final class SecurityParameterIndex implements AutoCloseable {
        private final CloseGuard mCloseGuard;
        private final InetAddress mRemoteAddress;
        private int mResourceId;
        private final IIpSecService mService;
        private int mSpi;

        public int getSpi() {
            return this.mSpi;
        }

        public void close() {
            try {
                this.mService.releaseSecurityParameterIndex(this.mResourceId);
                this.mCloseGuard.close();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        protected void finalize() {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            close();
        }

        private SecurityParameterIndex(IIpSecService service, int direction, InetAddress remoteAddress, int spi) throws ResourceUnavailableException, SpiUnavailableException {
            this.mCloseGuard = CloseGuard.get();
            this.mSpi = 0;
            this.mService = service;
            this.mRemoteAddress = remoteAddress;
            try {
                IpSecSpiResponse result = this.mService.reserveSecurityParameterIndex(direction, remoteAddress.getHostAddress(), spi, new Binder());
                if (result == null) {
                    throw new NullPointerException("Received null response from IpSecService");
                }
                int status = result.status;
                switch (status) {
                    case 0:
                        this.mSpi = result.spi;
                        this.mResourceId = result.resourceId;
                        if (this.mSpi == 0) {
                            throw new RuntimeException("Invalid SPI returned by IpSecService: " + status);
                        } else if (this.mResourceId == 0) {
                            throw new RuntimeException("Invalid Resource ID returned by IpSecService: " + status);
                        } else {
                            this.mCloseGuard.open("open");
                            return;
                        }
                    case 1:
                        throw new ResourceUnavailableException("No more SPIs may be allocated by this requester.");
                    case 2:
                        throw new SpiUnavailableException("Requested SPI is unavailable", spi);
                    default:
                        throw new RuntimeException("Unknown status returned by IpSecService: " + status);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        int getResourceId() {
            return this.mResourceId;
        }
    }

    public static final class SpiUnavailableException extends AndroidException {
        private final int mSpi;

        SpiUnavailableException(String msg, int spi) {
            super(msg + "(spi: " + spi + ")");
            this.mSpi = spi;
        }

        public int getSpi() {
            return this.mSpi;
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
        private final int mResourceId;
        private final IIpSecService mService;

        private UdpEncapsulationSocket(IIpSecService service, int port) throws ResourceUnavailableException, IOException {
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
                        throw new RuntimeException("Unknown status returned by IpSecService: " + result.status);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
            throw e.rethrowFromSystemServer();
        }

        public FileDescriptor getSocket() {
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
                try {
                    this.mPfd.close();
                    this.mCloseGuard.close();
                } catch (IOException e) {
                    Log.e(IpSecManager.TAG, "Failed to close UDP Encapsulation Socket with Port= " + this.mPort);
                    throw e;
                }
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }

        protected void finalize() throws Throwable {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            close();
        }

        int getResourceId() {
            return this.mResourceId;
        }
    }

    public SecurityParameterIndex reserveSecurityParameterIndex(int direction, InetAddress remoteAddress) throws ResourceUnavailableException {
        try {
            return new SecurityParameterIndex(this.mService, direction, remoteAddress, 0);
        } catch (SpiUnavailableException e) {
            throw new ResourceUnavailableException("No SPIs available");
        }
    }

    public SecurityParameterIndex reserveSecurityParameterIndex(int direction, InetAddress remoteAddress, int requestedSpi) throws SpiUnavailableException, ResourceUnavailableException {
        if (requestedSpi != 0) {
            return new SecurityParameterIndex(this.mService, direction, remoteAddress, requestedSpi);
        }
        throw new IllegalArgumentException("Requested SPI must be a valid (non-zero) SPI");
    }

    public void applyTransportModeTransform(Socket socket, IpSecTransform transform) throws IOException {
        Throwable th;
        Throwable th2 = null;
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = ParcelFileDescriptor.fromSocket(socket);
            applyTransportModeTransform(parcelFileDescriptor, transform);
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 != null) {
                throw th2;
            }
            return;
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    public void applyTransportModeTransform(DatagramSocket socket, IpSecTransform transform) throws IOException {
        Throwable th;
        Throwable th2 = null;
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = ParcelFileDescriptor.fromDatagramSocket(socket);
            applyTransportModeTransform(parcelFileDescriptor, transform);
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 != null) {
                throw th2;
            }
            return;
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    public void applyTransportModeTransform(FileDescriptor socket, IpSecTransform transform) throws IOException {
        Throwable th;
        Throwable th2 = null;
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = ParcelFileDescriptor.dup(socket);
            applyTransportModeTransform(parcelFileDescriptor, transform);
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 != null) {
                throw th2;
            }
            return;
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    private void applyTransportModeTransform(ParcelFileDescriptor pfd, IpSecTransform transform) {
        try {
            this.mService.applyTransportModeTransform(pfd, transform.getResourceId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void applyTunnelModeTransform(Network net, IpSecTransform transform) {
    }

    public void removeTransportModeTransform(Socket socket, IpSecTransform transform) throws IOException {
        Throwable th;
        Throwable th2 = null;
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = ParcelFileDescriptor.fromSocket(socket);
            removeTransportModeTransform(parcelFileDescriptor, transform);
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 != null) {
                throw th2;
            }
            return;
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    public void removeTransportModeTransform(DatagramSocket socket, IpSecTransform transform) throws IOException {
        Throwable th;
        Throwable th2 = null;
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = ParcelFileDescriptor.fromDatagramSocket(socket);
            removeTransportModeTransform(parcelFileDescriptor, transform);
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 != null) {
                throw th2;
            }
            return;
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    public void removeTransportModeTransform(FileDescriptor socket, IpSecTransform transform) throws IOException {
        Throwable th;
        Throwable th2 = null;
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = ParcelFileDescriptor.dup(socket);
            removeTransportModeTransform(parcelFileDescriptor, transform);
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 != null) {
                throw th2;
            }
            return;
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    private void removeTransportModeTransform(ParcelFileDescriptor pfd, IpSecTransform transform) {
        try {
            this.mService.removeTransportModeTransform(pfd, transform.getResourceId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeTunnelModeTransform(Network net, IpSecTransform transform) {
    }

    public UdpEncapsulationSocket openUdpEncapsulationSocket(int port) throws IOException, ResourceUnavailableException {
        if (port != 0) {
            return new UdpEncapsulationSocket(this.mService, port);
        }
        throw new IllegalArgumentException("Specified port must be a valid port number!");
    }

    public UdpEncapsulationSocket openUdpEncapsulationSocket() throws IOException, ResourceUnavailableException {
        return new UdpEncapsulationSocket(this.mService, 0);
    }

    public IpSecManager(IIpSecService service) {
        this.mService = (IIpSecService) Preconditions.checkNotNull(service, "missing service");
    }
}
