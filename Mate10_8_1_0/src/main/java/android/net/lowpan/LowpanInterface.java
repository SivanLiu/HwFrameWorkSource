package android.net.lowpan;

import android.content.Context;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.lowpan.ILowpanInterfaceListener.Stub;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.Log;
import java.util.HashMap;

public class LowpanInterface {
    public static final String EMPTY_PARTITION_ID = "";
    public static final String NETWORK_TYPE_THREAD_V1 = "org.threadgroup.thread.v1";
    public static final String ROLE_COORDINATOR = "coordinator";
    public static final String ROLE_DETACHED = "detached";
    public static final String ROLE_END_DEVICE = "end-device";
    public static final String ROLE_LEADER = "leader";
    public static final String ROLE_ROUTER = "router";
    public static final String ROLE_SLEEPY_END_DEVICE = "sleepy-end-device";
    public static final String ROLE_SLEEPY_ROUTER = "sleepy-router";
    public static final String STATE_ATTACHED = "attached";
    public static final String STATE_ATTACHING = "attaching";
    public static final String STATE_COMMISSIONING = "commissioning";
    public static final String STATE_FAULT = "fault";
    public static final String STATE_OFFLINE = "offline";
    private static final String TAG = LowpanInterface.class.getSimpleName();
    private final ILowpanInterface mBinder;
    private final HashMap<Integer, ILowpanInterfaceListener> mListenerMap = new HashMap();
    private final Looper mLooper;

    public static abstract class Callback {
        public void onConnectedChanged(boolean value) {
        }

        public void onEnabledChanged(boolean value) {
        }

        public void onUpChanged(boolean value) {
        }

        public void onRoleChanged(String value) {
        }

        public void onStateChanged(String state) {
        }

        public void onLowpanIdentityChanged(LowpanIdentity value) {
        }

        public void onLinkNetworkAdded(IpPrefix prefix) {
        }

        public void onLinkNetworkRemoved(IpPrefix prefix) {
        }

        public void onLinkAddressAdded(LinkAddress address) {
        }

        public void onLinkAddressRemoved(LinkAddress address) {
        }
    }

    public LowpanInterface(Context context, ILowpanInterface service, Looper looper) {
        this.mBinder = service;
        this.mLooper = looper;
    }

    public ILowpanInterface getService() {
        return this.mBinder;
    }

    public void form(LowpanProvision provision) throws LowpanException {
        try {
            this.mBinder.form(provision);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void join(LowpanProvision provision) throws LowpanException {
        try {
            this.mBinder.join(provision);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void attach(LowpanProvision provision) throws LowpanException {
        try {
            this.mBinder.attach(provision);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void leave() throws LowpanException {
        try {
            this.mBinder.leave();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public LowpanCommissioningSession startCommissioningSession(LowpanBeaconInfo beaconInfo) throws LowpanException {
        try {
            this.mBinder.startCommissioningSession(beaconInfo);
            return new LowpanCommissioningSession(this.mBinder, beaconInfo, this.mLooper);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void reset() throws LowpanException {
        try {
            this.mBinder.reset();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public String getName() {
        try {
            return this.mBinder.getName();
        } catch (DeadObjectException e) {
            return "";
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public boolean isEnabled() {
        try {
            return this.mBinder.isEnabled();
        } catch (DeadObjectException e) {
            return false;
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public void setEnabled(boolean enabled) throws LowpanException {
        try {
            this.mBinder.setEnabled(enabled);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public boolean isUp() {
        try {
            return this.mBinder.isUp();
        } catch (DeadObjectException e) {
            return false;
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public boolean isConnected() {
        try {
            return this.mBinder.isConnected();
        } catch (DeadObjectException e) {
            return false;
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public boolean isCommissioned() {
        try {
            return this.mBinder.isCommissioned();
        } catch (DeadObjectException e) {
            return false;
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public String getState() {
        try {
            return this.mBinder.getState();
        } catch (DeadObjectException e) {
            return "fault";
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public String getPartitionId() {
        try {
            return this.mBinder.getPartitionId();
        } catch (DeadObjectException e) {
            return "";
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public LowpanIdentity getLowpanIdentity() {
        try {
            return this.mBinder.getLowpanIdentity();
        } catch (DeadObjectException e) {
            return new LowpanIdentity();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public String getRole() {
        try {
            return this.mBinder.getRole();
        } catch (DeadObjectException e) {
            return "detached";
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public LowpanCredential getLowpanCredential() {
        try {
            return this.mBinder.getLowpanCredential();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public String[] getSupportedNetworkTypes() throws LowpanException {
        try {
            return this.mBinder.getSupportedNetworkTypes();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public LowpanChannelInfo[] getSupportedChannels() throws LowpanException {
        try {
            return this.mBinder.getSupportedChannels();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void registerCallback(final Callback cb, final Handler handler) {
        Stub listenerBinder = new Stub() {
            private Handler mHandler;

            public void onEnabledChanged(boolean value) {
                this.mHandler.post(new -$Lambda$kGwbyTn61Si3sH7muskKIr7PCeU((byte) 1, value, cb));
            }

            public void onConnectedChanged(boolean value) {
                this.mHandler.post(new -$Lambda$kGwbyTn61Si3sH7muskKIr7PCeU((byte) 0, value, cb));
            }

            public void onUpChanged(boolean value) {
                this.mHandler.post(new -$Lambda$kGwbyTn61Si3sH7muskKIr7PCeU((byte) 2, value, cb));
            }

            public void onRoleChanged(String value) {
                this.mHandler.post(new -$Lambda$lq0tFj928XFoCdCDLCq_E-OIg9U((byte) 6, cb, value));
            }

            public void onStateChanged(String value) {
                this.mHandler.post(new -$Lambda$lq0tFj928XFoCdCDLCq_E-OIg9U((byte) 7, cb, value));
            }

            public void onLowpanIdentityChanged(LowpanIdentity value) {
                this.mHandler.post(new -$Lambda$lq0tFj928XFoCdCDLCq_E-OIg9U((byte) 5, cb, value));
            }

            public void onLinkNetworkAdded(IpPrefix value) {
                this.mHandler.post(new -$Lambda$lq0tFj928XFoCdCDLCq_E-OIg9U((byte) 3, cb, value));
            }

            public void onLinkNetworkRemoved(IpPrefix value) {
                this.mHandler.post(new -$Lambda$lq0tFj928XFoCdCDLCq_E-OIg9U((byte) 4, cb, value));
            }

            public void onLinkAddressAdded(String value) {
                try {
                    this.mHandler.post(new -$Lambda$lq0tFj928XFoCdCDLCq_E-OIg9U((byte) 1, cb, new LinkAddress(value)));
                } catch (IllegalArgumentException x) {
                    Log.e(LowpanInterface.TAG, "onLinkAddressAdded: Bad LinkAddress \"" + value + "\", " + x);
                }
            }

            public void onLinkAddressRemoved(String value) {
                try {
                    this.mHandler.post(new -$Lambda$lq0tFj928XFoCdCDLCq_E-OIg9U((byte) 2, cb, new LinkAddress(value)));
                } catch (IllegalArgumentException x) {
                    Log.e(LowpanInterface.TAG, "onLinkAddressRemoved: Bad LinkAddress \"" + value + "\", " + x);
                }
            }

            public void onReceiveFromCommissioner(byte[] packet) {
            }
        };
        try {
            this.mBinder.addListener(listenerBinder);
            synchronized (this.mListenerMap) {
                this.mListenerMap.put(Integer.valueOf(System.identityHashCode(cb)), listenerBinder);
            }
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public void registerCallback(Callback cb) {
        registerCallback(cb, null);
    }

    public void unregisterCallback(Callback cb) {
        int hashCode = System.identityHashCode(cb);
        synchronized (this.mListenerMap) {
            ILowpanInterfaceListener listenerBinder = (ILowpanInterfaceListener) this.mListenerMap.get(Integer.valueOf(hashCode));
            if (listenerBinder != null) {
                this.mListenerMap.remove(Integer.valueOf(hashCode));
                try {
                    this.mBinder.removeListener(listenerBinder);
                } catch (DeadObjectException e) {
                } catch (RemoteException x) {
                    throw x.rethrowAsRuntimeException();
                }
            }
        }
    }

    public LowpanScanner createScanner() {
        return new LowpanScanner(this.mBinder);
    }

    public LinkAddress[] getLinkAddresses() throws LowpanException {
        try {
            String[] linkAddressStrings = this.mBinder.getLinkAddresses();
            LinkAddress[] ret = new LinkAddress[linkAddressStrings.length];
            int i = 0;
            int length = linkAddressStrings.length;
            int i2 = 0;
            while (i < length) {
                int i3 = i2 + 1;
                ret[i2] = new LinkAddress(linkAddressStrings[i]);
                i++;
                i2 = i3;
            }
            return ret;
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public IpPrefix[] getLinkNetworks() throws LowpanException {
        try {
            return this.mBinder.getLinkNetworks();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void addOnMeshPrefix(IpPrefix prefix, int flags) throws LowpanException {
        try {
            this.mBinder.addOnMeshPrefix(prefix, flags);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void removeOnMeshPrefix(IpPrefix prefix) {
        try {
            this.mBinder.removeOnMeshPrefix(prefix);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            Log.e(TAG, x2.toString());
        }
    }

    public void addExternalRoute(IpPrefix prefix, int flags) throws LowpanException {
        try {
            this.mBinder.addExternalRoute(prefix, flags);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void removeExternalRoute(IpPrefix prefix) {
        try {
            this.mBinder.removeExternalRoute(prefix);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            Log.e(TAG, x2.toString());
        }
    }
}
