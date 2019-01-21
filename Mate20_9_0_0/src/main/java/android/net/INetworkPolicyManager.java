package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.SubscriptionPlan;

public interface INetworkPolicyManager extends IInterface {

    public static abstract class Stub extends Binder implements INetworkPolicyManager {
        private static final String DESCRIPTOR = "android.net.INetworkPolicyManager";
        static final int TRANSACTION_addUidPolicy = 2;
        static final int TRANSACTION_factoryReset = 22;
        static final int TRANSACTION_getNetworkPolicies = 9;
        static final int TRANSACTION_getNetworkQuotaInfo = 17;
        static final int TRANSACTION_getRestrictBackground = 12;
        static final int TRANSACTION_getRestrictBackgroundByCaller = 14;
        static final int TRANSACTION_getSubscriptionPlans = 18;
        static final int TRANSACTION_getSubscriptionPlansOwner = 20;
        static final int TRANSACTION_getUidPolicy = 4;
        static final int TRANSACTION_getUidsWithPolicy = 5;
        static final int TRANSACTION_isUidNetworkingBlocked = 23;
        static final int TRANSACTION_onTetheringChanged = 13;
        static final int TRANSACTION_registerListener = 6;
        static final int TRANSACTION_removeUidPolicy = 3;
        static final int TRANSACTION_setDeviceIdleMode = 15;
        static final int TRANSACTION_setNetworkPolicies = 8;
        static final int TRANSACTION_setRestrictBackground = 11;
        static final int TRANSACTION_setSubscriptionOverride = 21;
        static final int TRANSACTION_setSubscriptionPlans = 19;
        static final int TRANSACTION_setUidPolicy = 1;
        static final int TRANSACTION_setWifiMeteredOverride = 16;
        static final int TRANSACTION_snoozeLimit = 10;
        static final int TRANSACTION_unregisterListener = 7;

        private static class Proxy implements INetworkPolicyManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public void setUidPolicy(int uid, int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(policy);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addUidPolicy(int uid, int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(policy);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeUidPolicy(int uid, int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(policy);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getUidPolicy(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int[] getUidsWithPolicy(int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(policy);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerListener(INetworkPolicyListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterListener(INetworkPolicyListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setNetworkPolicies(NetworkPolicy[] policies) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedArray(policies, 0);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public NetworkPolicy[] getNetworkPolicies(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    NetworkPolicy[] _result = (NetworkPolicy[]) _reply.createTypedArray(NetworkPolicy.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void snoozeLimit(NetworkTemplate template) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (template != null) {
                        _data.writeInt(1);
                        template.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setRestrictBackground(boolean restrictBackground) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(restrictBackground);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getRestrictBackground() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onTetheringChanged(String iface, boolean tethering) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeInt(tethering);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getRestrictBackgroundByCaller() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDeviceIdleMode(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setWifiMeteredOverride(String networkId, int meteredOverride) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(networkId);
                    _data.writeInt(meteredOverride);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public NetworkQuotaInfo getNetworkQuotaInfo(NetworkState state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    NetworkQuotaInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (state != null) {
                        _data.writeInt(1);
                        state.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (NetworkQuotaInfo) NetworkQuotaInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public SubscriptionPlan[] getSubscriptionPlans(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    SubscriptionPlan[] _result = (SubscriptionPlan[]) _reply.createTypedArray(SubscriptionPlan.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setSubscriptionPlans(int subId, SubscriptionPlan[] plans, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeTypedArray(plans, 0);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getSubscriptionPlansOwner(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setSubscriptionOverride(int subId, int overrideMask, int overrideValue, long timeoutMillis, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(overrideMask);
                    _data.writeInt(overrideValue);
                    _data.writeLong(timeoutMillis);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void factoryReset(String subscriber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(subscriber);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isUidNetworkingBlocked(int uid, boolean meteredNetwork) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(meteredNetwork);
                    boolean z = false;
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkPolicyManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INetworkPolicyManager)) {
                return new Proxy(obj);
            }
            return (INetworkPolicyManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                NetworkTemplate _arg0 = null;
                boolean _arg1 = false;
                int _result;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        setUidPolicy(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        addUidPolicy(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        removeUidPolicy(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        int _result2 = getUidPolicy(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        int[] _result3 = getUidsWithPolicy(data.readInt());
                        reply.writeNoException();
                        parcel2.writeIntArray(_result3);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        registerListener(android.net.INetworkPolicyListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        unregisterListener(android.net.INetworkPolicyListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        setNetworkPolicies((NetworkPolicy[]) parcel.createTypedArray(NetworkPolicy.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        NetworkPolicy[] _result4 = getNetworkPolicies(data.readString());
                        reply.writeNoException();
                        parcel2.writeTypedArray(_result4, 1);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (NetworkTemplate) NetworkTemplate.CREATOR.createFromParcel(parcel);
                        }
                        snoozeLimit(_arg0);
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setRestrictBackground(_arg1);
                        reply.writeNoException();
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        boolean _result5 = getRestrictBackground();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        String _arg02 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        onTetheringChanged(_arg02, _arg1);
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _result = getRestrictBackgroundByCaller();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setDeviceIdleMode(_arg1);
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        setWifiMeteredOverride(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 17:
                        NetworkState _arg03;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (NetworkState) NetworkState.CREATOR.createFromParcel(parcel);
                        }
                        NetworkQuotaInfo _result6 = getNetworkQuotaInfo(_arg03);
                        reply.writeNoException();
                        if (_result6 != null) {
                            parcel2.writeInt(1);
                            _result6.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        SubscriptionPlan[] _result7 = getSubscriptionPlans(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeTypedArray(_result7, 1);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        setSubscriptionPlans(data.readInt(), (SubscriptionPlan[]) parcel.createTypedArray(SubscriptionPlan.CREATOR), data.readString());
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        String _result8 = getSubscriptionPlansOwner(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result8);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        setSubscriptionOverride(data.readInt(), data.readInt(), data.readInt(), data.readLong(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        factoryReset(data.readString());
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        _result = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        boolean _result9 = isUidNetworkingBlocked(_result, _arg1);
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void addUidPolicy(int i, int i2) throws RemoteException;

    void factoryReset(String str) throws RemoteException;

    NetworkPolicy[] getNetworkPolicies(String str) throws RemoteException;

    NetworkQuotaInfo getNetworkQuotaInfo(NetworkState networkState) throws RemoteException;

    boolean getRestrictBackground() throws RemoteException;

    int getRestrictBackgroundByCaller() throws RemoteException;

    SubscriptionPlan[] getSubscriptionPlans(int i, String str) throws RemoteException;

    String getSubscriptionPlansOwner(int i) throws RemoteException;

    int getUidPolicy(int i) throws RemoteException;

    int[] getUidsWithPolicy(int i) throws RemoteException;

    boolean isUidNetworkingBlocked(int i, boolean z) throws RemoteException;

    void onTetheringChanged(String str, boolean z) throws RemoteException;

    void registerListener(INetworkPolicyListener iNetworkPolicyListener) throws RemoteException;

    void removeUidPolicy(int i, int i2) throws RemoteException;

    void setDeviceIdleMode(boolean z) throws RemoteException;

    void setNetworkPolicies(NetworkPolicy[] networkPolicyArr) throws RemoteException;

    void setRestrictBackground(boolean z) throws RemoteException;

    void setSubscriptionOverride(int i, int i2, int i3, long j, String str) throws RemoteException;

    void setSubscriptionPlans(int i, SubscriptionPlan[] subscriptionPlanArr, String str) throws RemoteException;

    void setUidPolicy(int i, int i2) throws RemoteException;

    void setWifiMeteredOverride(String str, int i) throws RemoteException;

    void snoozeLimit(NetworkTemplate networkTemplate) throws RemoteException;

    void unregisterListener(INetworkPolicyListener iNetworkPolicyListener) throws RemoteException;
}
