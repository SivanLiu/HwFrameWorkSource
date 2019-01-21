package android.net;

import android.content.Context;
import android.net.ConnectivityManager.PacketKeepalive;
import android.net.ConnectivityManager.PacketKeepaliveCallback;
import android.net.IIpSecService.Stub;
import android.net.IpSecManager.ResourceUnavailableException;
import android.net.IpSecManager.SecurityParameterIndex;
import android.net.IpSecManager.SpiUnavailableException;
import android.net.IpSecManager.UdpEncapsulationSocket;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import dalvik.system.CloseGuard;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;

public final class IpSecTransform implements AutoCloseable {
    public static final int ENCAP_ESPINUDP = 2;
    public static final int ENCAP_ESPINUDP_NON_IKE = 1;
    public static final int ENCAP_NONE = 0;
    public static final int MODE_TRANSPORT = 0;
    public static final int MODE_TUNNEL = 1;
    private static final String TAG = "IpSecTransform";
    private Handler mCallbackHandler;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final IpSecConfig mConfig;
    private final Context mContext;
    private PacketKeepalive mKeepalive;
    private final PacketKeepaliveCallback mKeepaliveCallback = new PacketKeepaliveCallback() {
        public void onStarted() {
            synchronized (this) {
                IpSecTransform.this.mCallbackHandler.post(new -$$Lambda$IpSecTransform$1$zl9bpxiE2uj_QuCOkuJ091wPuwo(this));
            }
        }

        public void onStopped() {
            synchronized (this) {
                IpSecTransform.this.mKeepalive = null;
                IpSecTransform.this.mCallbackHandler.post(new -$$Lambda$IpSecTransform$1$Rc3lbWP51o1kJRHwkpVUEV1G_d8(this));
            }
        }

        public void onError(int error) {
            synchronized (this) {
                IpSecTransform.this.mKeepalive = null;
                IpSecTransform.this.mCallbackHandler.post(new -$$Lambda$IpSecTransform$1$_ae2VrMToKvertNlEIezU0bdvXE(this, error));
            }
        }
    };
    private int mResourceId;
    private NattKeepaliveCallback mUserKeepaliveCallback;

    public static class Builder {
        private IpSecConfig mConfig = new IpSecConfig();
        private Context mContext;

        public Builder setEncryption(IpSecAlgorithm algo) {
            Preconditions.checkNotNull(algo);
            this.mConfig.setEncryption(algo);
            return this;
        }

        public Builder setAuthentication(IpSecAlgorithm algo) {
            Preconditions.checkNotNull(algo);
            this.mConfig.setAuthentication(algo);
            return this;
        }

        public Builder setAuthenticatedEncryption(IpSecAlgorithm algo) {
            Preconditions.checkNotNull(algo);
            this.mConfig.setAuthenticatedEncryption(algo);
            return this;
        }

        public Builder setIpv4Encapsulation(UdpEncapsulationSocket localSocket, int remotePort) {
            Preconditions.checkNotNull(localSocket);
            this.mConfig.setEncapType(2);
            if (localSocket.getResourceId() != -1) {
                this.mConfig.setEncapSocketResourceId(localSocket.getResourceId());
                this.mConfig.setEncapRemotePort(remotePort);
                return this;
            }
            throw new IllegalArgumentException("Invalid UdpEncapsulationSocket");
        }

        public IpSecTransform buildTransportModeTransform(InetAddress sourceAddress, SecurityParameterIndex spi) throws ResourceUnavailableException, SpiUnavailableException, IOException {
            Preconditions.checkNotNull(sourceAddress);
            Preconditions.checkNotNull(spi);
            if (spi.getResourceId() != -1) {
                this.mConfig.setMode(0);
                this.mConfig.setSourceAddress(sourceAddress.getHostAddress());
                this.mConfig.setSpiResourceId(spi.getResourceId());
                return new IpSecTransform(this.mContext, this.mConfig).activate();
            }
            throw new IllegalArgumentException("Invalid SecurityParameterIndex");
        }

        public IpSecTransform buildTunnelModeTransform(InetAddress sourceAddress, SecurityParameterIndex spi) throws ResourceUnavailableException, SpiUnavailableException, IOException {
            Preconditions.checkNotNull(sourceAddress);
            Preconditions.checkNotNull(spi);
            if (spi.getResourceId() != -1) {
                this.mConfig.setMode(1);
                this.mConfig.setSourceAddress(sourceAddress.getHostAddress());
                this.mConfig.setSpiResourceId(spi.getResourceId());
                return new IpSecTransform(this.mContext, this.mConfig).activate();
            }
            throw new IllegalArgumentException("Invalid SecurityParameterIndex");
        }

        public Builder(Context context) {
            Preconditions.checkNotNull(context);
            this.mContext = context;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface EncapType {
    }

    public static class NattKeepaliveCallback {
        public static final int ERROR_HARDWARE_ERROR = 3;
        public static final int ERROR_HARDWARE_UNSUPPORTED = 2;
        public static final int ERROR_INVALID_NETWORK = 1;

        public void onStarted() {
        }

        public void onStopped() {
        }

        public void onError(int error) {
        }
    }

    @VisibleForTesting
    public IpSecTransform(Context context, IpSecConfig config) {
        this.mContext = context;
        this.mConfig = new IpSecConfig(config);
        this.mResourceId = -1;
    }

    private IIpSecService getIpSecService() {
        IBinder b = ServiceManager.getService("ipsec");
        if (b != null) {
            return Stub.asInterface(b);
        }
        throw new RemoteException("Failed to connect to IpSecService").rethrowAsRuntimeException();
    }

    private void checkResultStatus(int status) throws IOException, ResourceUnavailableException, SpiUnavailableException {
        switch (status) {
            case 0:
                return;
            case 1:
                throw new ResourceUnavailableException("Failed to allocate a new IpSecTransform");
            case 2:
                Log.wtf(TAG, "Attempting to use an SPI that was somehow not reserved");
                break;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to Create a Transform with status code ");
        stringBuilder.append(status);
        throw new IllegalStateException(stringBuilder.toString());
    }

    private IpSecTransform activate() throws IOException, ResourceUnavailableException, SpiUnavailableException {
        synchronized (this) {
            try {
                IpSecTransformResponse result = getIpSecService().createTransform(this.mConfig, new Binder(), this.mContext.getOpPackageName());
                checkResultStatus(result.status);
                this.mResourceId = result.resourceId;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Added Transform with Id ");
                stringBuilder.append(this.mResourceId);
                Log.d(str, stringBuilder.toString());
                this.mCloseGuard.open("build");
            } catch (ServiceSpecificException e) {
                throw IpSecManager.rethrowUncheckedExceptionFromServiceSpecificException(e);
            } catch (RemoteException e2) {
                throw e2.rethrowAsRuntimeException();
            } catch (Throwable th) {
            }
        }
        return this;
    }

    @VisibleForTesting
    public static boolean equals(IpSecTransform lhs, IpSecTransform rhs) {
        boolean z = false;
        if (lhs == null || rhs == null) {
            if (lhs == rhs) {
                z = true;
            }
            return z;
        }
        if (IpSecConfig.equals(lhs.getConfig(), rhs.getConfig()) && lhs.mResourceId == rhs.mResourceId) {
            z = true;
        }
        return z;
    }

    public void close() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Removing Transform with Id ");
        stringBuilder.append(this.mResourceId);
        Log.d(str, stringBuilder.toString());
        if (this.mResourceId == -1) {
            this.mCloseGuard.close();
            return;
        }
        try {
            getIpSecService().deleteTransform(this.mResourceId);
            stopNattKeepalive();
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        } catch (Exception e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to close ");
            stringBuilder2.append(this);
            stringBuilder2.append(", Exception=");
            stringBuilder2.append(e2);
            Log.e(str2, stringBuilder2.toString());
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

    IpSecConfig getConfig() {
        return this.mConfig;
    }

    @VisibleForTesting
    public int getResourceId() {
        return this.mResourceId;
    }

    public void startNattKeepalive(NattKeepaliveCallback userCallback, int intervalSeconds, Handler handler) throws IOException {
        Preconditions.checkNotNull(userCallback);
        if (intervalSeconds < 20 || intervalSeconds > 3600) {
            throw new IllegalArgumentException("Invalid NAT-T keepalive interval");
        }
        Preconditions.checkNotNull(handler);
        if (this.mResourceId != -1) {
            synchronized (this.mKeepaliveCallback) {
                if (this.mKeepaliveCallback == null) {
                    this.mUserKeepaliveCallback = userCallback;
                    this.mKeepalive = ((ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).startNattKeepalive(this.mConfig.getNetwork(), intervalSeconds, this.mKeepaliveCallback, NetworkUtils.numericToInetAddress(this.mConfig.getSourceAddress()), PacketKeepalive.NATT_PORT, NetworkUtils.numericToInetAddress(this.mConfig.getDestinationAddress()));
                    this.mCallbackHandler = handler;
                } else {
                    throw new IllegalStateException("Keepalive already active");
                }
            }
            return;
        }
        throw new IllegalStateException("Packet keepalive cannot be started for an inactive transform");
    }

    public void stopNattKeepalive() {
        synchronized (this.mKeepaliveCallback) {
            if (this.mKeepalive == null) {
                Log.e(TAG, "No active keepalive to stop");
                return;
            }
            this.mKeepalive.stop();
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IpSecTransform{resourceId=");
        stringBuilder.append(this.mResourceId);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
