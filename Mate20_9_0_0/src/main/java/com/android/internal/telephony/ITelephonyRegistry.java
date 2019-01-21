package com.android.internal.telephony;

import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.CellInfo;
import android.telephony.PhysicalChannelConfig;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.VoLteServiceState;
import java.util.List;

public interface ITelephonyRegistry extends IInterface {

    public static abstract class Stub extends Binder implements ITelephonyRegistry {
        private static final String DESCRIPTOR = "com.android.internal.telephony.ITelephonyRegistry";
        static final int TRANSACTION_addOnSubscriptionsChangedListener = 1;
        static final int TRANSACTION_listen = 3;
        static final int TRANSACTION_listenForSubscriber = 4;
        static final int TRANSACTION_notifyCallForwardingChanged = 10;
        static final int TRANSACTION_notifyCallForwardingChangedForSubscriber = 11;
        static final int TRANSACTION_notifyCallState = 5;
        static final int TRANSACTION_notifyCallStateForPhoneId = 6;
        static final int TRANSACTION_notifyCarrierNetworkChange = 32;
        static final int TRANSACTION_notifyCellInfo = 21;
        static final int TRANSACTION_notifyCellInfoForSubscriber = 27;
        static final int TRANSACTION_notifyCellLocation = 18;
        static final int TRANSACTION_notifyCellLocationForSubscriber = 19;
        static final int TRANSACTION_notifyDataActivity = 12;
        static final int TRANSACTION_notifyDataActivityForSubscriber = 13;
        static final int TRANSACTION_notifyDataConnection = 14;
        static final int TRANSACTION_notifyDataConnectionFailed = 16;
        static final int TRANSACTION_notifyDataConnectionFailedForSubscriber = 17;
        static final int TRANSACTION_notifyDataConnectionForSubscriber = 15;
        static final int TRANSACTION_notifyDisconnectCause = 25;
        static final int TRANSACTION_notifyMessageWaitingChangedForPhoneId = 9;
        static final int TRANSACTION_notifyOemHookRawEventForSubscriber = 30;
        static final int TRANSACTION_notifyOtaspChanged = 20;
        static final int TRANSACTION_notifyPhysicalChannelConfiguration = 22;
        static final int TRANSACTION_notifyPhysicalChannelConfigurationForSubscriber = 23;
        static final int TRANSACTION_notifyPreciseCallState = 24;
        static final int TRANSACTION_notifyPreciseDataConnectionFailed = 26;
        static final int TRANSACTION_notifyServiceStateForPhoneId = 7;
        static final int TRANSACTION_notifySignalStrengthForPhoneId = 8;
        static final int TRANSACTION_notifySimActivationStateChangedForPhoneId = 29;
        static final int TRANSACTION_notifySubscriptionInfoChanged = 31;
        static final int TRANSACTION_notifyUserMobileDataStateChangedForPhoneId = 33;
        static final int TRANSACTION_notifyVoLteServiceStateChanged = 28;
        static final int TRANSACTION_removeOnSubscriptionsChangedListener = 2;

        private static class Proxy implements ITelephonyRegistry {
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

            public void addOnSubscriptionsChangedListener(String pkg, IOnSubscriptionsChangedListener callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeOnSubscriptionsChangedListener(String pkg, IOnSubscriptionsChangedListener callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void listen(String pkg, IPhoneStateListener callback, int events, boolean notifyNow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(events);
                    _data.writeInt(notifyNow);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void listenForSubscriber(int subId, String pkg, IPhoneStateListener callback, int events, boolean notifyNow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(pkg);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(events);
                    _data.writeInt(notifyNow);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyCallState(int state, String incomingNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeString(incomingNumber);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyCallStateForPhoneId(int phoneId, int subId, int state, String incomingNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeInt(subId);
                    _data.writeInt(state);
                    _data.writeString(incomingNumber);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyServiceStateForPhoneId(int phoneId, int subId, ServiceState state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeInt(subId);
                    if (state != null) {
                        _data.writeInt(1);
                        state.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifySignalStrengthForPhoneId(int phoneId, int subId, SignalStrength signalStrength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeInt(subId);
                    if (signalStrength != null) {
                        _data.writeInt(1);
                        signalStrength.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyMessageWaitingChangedForPhoneId(int phoneId, int subId, boolean mwi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeInt(subId);
                    _data.writeInt(mwi);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyCallForwardingChanged(boolean cfi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cfi);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyCallForwardingChangedForSubscriber(int subId, boolean cfi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(cfi);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyDataActivity(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyDataActivityForSubscriber(int subId, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(state);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyDataConnection(int state, boolean isDataConnectivityPossible, String reason, String apn, String apnType, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int networkType, boolean roaming) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeInt(isDataConnectivityPossible);
                    _data.writeString(reason);
                    _data.writeString(apn);
                    _data.writeString(apnType);
                    if (linkProperties != null) {
                        _data.writeInt(1);
                        linkProperties.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (networkCapabilities != null) {
                        _data.writeInt(1);
                        networkCapabilities.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(networkType);
                    _data.writeInt(roaming);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyDataConnectionForSubscriber(int subId, int state, boolean isDataConnectivityPossible, String reason, String apn, String apnType, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int networkType, boolean roaming) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(state);
                    _data.writeInt(isDataConnectivityPossible);
                    _data.writeString(reason);
                    _data.writeString(apn);
                    _data.writeString(apnType);
                    if (linkProperties != null) {
                        _data.writeInt(1);
                        linkProperties.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (networkCapabilities != null) {
                        _data.writeInt(1);
                        networkCapabilities.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(networkType);
                    _data.writeInt(roaming);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyDataConnectionFailed(String reason, String apnType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(reason);
                    _data.writeString(apnType);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyDataConnectionFailedForSubscriber(int subId, String reason, String apnType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(reason);
                    _data.writeString(apnType);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyCellLocation(Bundle cellLocation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (cellLocation != null) {
                        _data.writeInt(1);
                        cellLocation.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyCellLocationForSubscriber(int subId, Bundle cellLocation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    if (cellLocation != null) {
                        _data.writeInt(1);
                        cellLocation.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyOtaspChanged(int otaspMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(otaspMode);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyCellInfo(List<CellInfo> cellInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(cellInfo);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyPhysicalChannelConfiguration(List<PhysicalChannelConfig> configs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(configs);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyPhysicalChannelConfigurationForSubscriber(int subId, List<PhysicalChannelConfig> configs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeTypedList(configs);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyPreciseCallState(int ringingCallState, int foregroundCallState, int backgroundCallState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ringingCallState);
                    _data.writeInt(foregroundCallState);
                    _data.writeInt(backgroundCallState);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyDisconnectCause(int disconnectCause, int preciseDisconnectCause) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(disconnectCause);
                    _data.writeInt(preciseDisconnectCause);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyPreciseDataConnectionFailed(String reason, String apnType, String apn, String failCause) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(reason);
                    _data.writeString(apnType);
                    _data.writeString(apn);
                    _data.writeString(failCause);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyCellInfoForSubscriber(int subId, List<CellInfo> cellInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeTypedList(cellInfo);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyVoLteServiceStateChanged(VoLteServiceState lteState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (lteState != null) {
                        _data.writeInt(1);
                        lteState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifySimActivationStateChangedForPhoneId(int phoneId, int subId, int activationState, int activationType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeInt(subId);
                    _data.writeInt(activationState);
                    _data.writeInt(activationType);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyOemHookRawEventForSubscriber(int subId, byte[] rawData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeByteArray(rawData);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifySubscriptionInfoChanged() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyCarrierNetworkChange(boolean active) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(active);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyUserMobileDataStateChangedForPhoneId(int phoneId, int subId, boolean state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeInt(subId);
                    _data.writeInt(state);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITelephonyRegistry asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ITelephonyRegistry)) {
                return new Proxy(obj);
            }
            return (ITelephonyRegistry) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                ServiceState _arg2 = null;
                boolean _arg22 = false;
                int _arg0;
                int _arg1;
                int _arg02;
                String _arg4;
                Bundle _arg03;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        addOnSubscriptionsChangedListener(data.readString(), com.android.internal.telephony.IOnSubscriptionsChangedListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        removeOnSubscriptionsChangedListener(data.readString(), com.android.internal.telephony.IOnSubscriptionsChangedListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        String _arg04 = data.readString();
                        IPhoneStateListener _arg12 = com.android.internal.telephony.IPhoneStateListener.Stub.asInterface(data.readStrongBinder());
                        int _arg23 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg22 = true;
                        }
                        listen(_arg04, _arg12, _arg23, _arg22);
                        reply.writeNoException();
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        listenForSubscriber(data.readInt(), data.readString(), com.android.internal.telephony.IPhoneStateListener.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        notifyCallState(data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        notifyCallStateForPhoneId(data.readInt(), data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg2 = (ServiceState) ServiceState.CREATOR.createFromParcel(parcel);
                        }
                        notifyServiceStateForPhoneId(_arg0, _arg1, _arg2);
                        reply.writeNoException();
                        return true;
                    case 8:
                        SignalStrength _arg24;
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg24 = (SignalStrength) SignalStrength.CREATOR.createFromParcel(parcel);
                        }
                        notifySignalStrengthForPhoneId(_arg0, _arg1, _arg24);
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg22 = true;
                        }
                        notifyMessageWaitingChangedForPhoneId(_arg02, _arg1, _arg22);
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg22 = true;
                        }
                        notifyCallForwardingChanged(_arg22);
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg22 = true;
                        }
                        notifyCallForwardingChangedForSubscriber(_arg02, _arg22);
                        reply.writeNoException();
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        notifyDataActivity(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        notifyDataActivityForSubscriber(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 14:
                        LinkProperties _arg5;
                        parcel.enforceInterface(descriptor);
                        int _arg05 = data.readInt();
                        boolean _arg13 = data.readInt() != 0;
                        String _arg25 = data.readString();
                        String _arg3 = data.readString();
                        _arg4 = data.readString();
                        if (data.readInt() != 0) {
                            _arg5 = (LinkProperties) LinkProperties.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg5 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg03 = (NetworkCapabilities) NetworkCapabilities.CREATOR.createFromParcel(parcel);
                        }
                        notifyDataConnection(_arg05, _arg13, _arg25, _arg3, _arg4, _arg5, _arg03, data.readInt(), data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 15:
                        LinkProperties _arg6;
                        parcel.enforceInterface(descriptor);
                        int _arg06 = data.readInt();
                        int _arg14 = data.readInt();
                        boolean _arg26 = data.readInt() != 0;
                        _arg4 = data.readString();
                        String _arg42 = data.readString();
                        String _arg52 = data.readString();
                        if (data.readInt() != 0) {
                            _arg6 = (LinkProperties) LinkProperties.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg6 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg03 = (NetworkCapabilities) NetworkCapabilities.CREATOR.createFromParcel(parcel);
                        }
                        notifyDataConnectionForSubscriber(_arg06, _arg14, _arg26, _arg4, _arg42, _arg52, _arg6, _arg03, data.readInt(), data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        notifyDataConnectionFailed(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        notifyDataConnectionFailedForSubscriber(data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        notifyCellLocation(_arg03);
                        reply.writeNoException();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg03 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        notifyCellLocationForSubscriber(_arg0, _arg03);
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        notifyOtaspChanged(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        notifyCellInfo(parcel.createTypedArrayList(CellInfo.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        notifyPhysicalChannelConfiguration(parcel.createTypedArrayList(PhysicalChannelConfig.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        notifyPhysicalChannelConfigurationForSubscriber(data.readInt(), parcel.createTypedArrayList(PhysicalChannelConfig.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        notifyPreciseCallState(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        notifyDisconnectCause(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        notifyPreciseDataConnectionFailed(data.readString(), data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        notifyCellInfoForSubscriber(data.readInt(), parcel.createTypedArrayList(CellInfo.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 28:
                        VoLteServiceState _arg07;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg07 = (VoLteServiceState) VoLteServiceState.CREATOR.createFromParcel(parcel);
                        }
                        notifyVoLteServiceStateChanged(_arg07);
                        reply.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        notifySimActivationStateChangedForPhoneId(data.readInt(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        notifyOemHookRawEventForSubscriber(data.readInt(), data.createByteArray());
                        reply.writeNoException();
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        notifySubscriptionInfoChanged();
                        reply.writeNoException();
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg22 = true;
                        }
                        notifyCarrierNetworkChange(_arg22);
                        reply.writeNoException();
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg22 = true;
                        }
                        notifyUserMobileDataStateChangedForPhoneId(_arg02, _arg1, _arg22);
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void addOnSubscriptionsChangedListener(String str, IOnSubscriptionsChangedListener iOnSubscriptionsChangedListener) throws RemoteException;

    void listen(String str, IPhoneStateListener iPhoneStateListener, int i, boolean z) throws RemoteException;

    void listenForSubscriber(int i, String str, IPhoneStateListener iPhoneStateListener, int i2, boolean z) throws RemoteException;

    void notifyCallForwardingChanged(boolean z) throws RemoteException;

    void notifyCallForwardingChangedForSubscriber(int i, boolean z) throws RemoteException;

    void notifyCallState(int i, String str) throws RemoteException;

    void notifyCallStateForPhoneId(int i, int i2, int i3, String str) throws RemoteException;

    void notifyCarrierNetworkChange(boolean z) throws RemoteException;

    void notifyCellInfo(List<CellInfo> list) throws RemoteException;

    void notifyCellInfoForSubscriber(int i, List<CellInfo> list) throws RemoteException;

    void notifyCellLocation(Bundle bundle) throws RemoteException;

    void notifyCellLocationForSubscriber(int i, Bundle bundle) throws RemoteException;

    void notifyDataActivity(int i) throws RemoteException;

    void notifyDataActivityForSubscriber(int i, int i2) throws RemoteException;

    void notifyDataConnection(int i, boolean z, String str, String str2, String str3, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int i2, boolean z2) throws RemoteException;

    void notifyDataConnectionFailed(String str, String str2) throws RemoteException;

    void notifyDataConnectionFailedForSubscriber(int i, String str, String str2) throws RemoteException;

    void notifyDataConnectionForSubscriber(int i, int i2, boolean z, String str, String str2, String str3, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int i3, boolean z2) throws RemoteException;

    void notifyDisconnectCause(int i, int i2) throws RemoteException;

    void notifyMessageWaitingChangedForPhoneId(int i, int i2, boolean z) throws RemoteException;

    void notifyOemHookRawEventForSubscriber(int i, byte[] bArr) throws RemoteException;

    void notifyOtaspChanged(int i) throws RemoteException;

    void notifyPhysicalChannelConfiguration(List<PhysicalChannelConfig> list) throws RemoteException;

    void notifyPhysicalChannelConfigurationForSubscriber(int i, List<PhysicalChannelConfig> list) throws RemoteException;

    void notifyPreciseCallState(int i, int i2, int i3) throws RemoteException;

    void notifyPreciseDataConnectionFailed(String str, String str2, String str3, String str4) throws RemoteException;

    void notifyServiceStateForPhoneId(int i, int i2, ServiceState serviceState) throws RemoteException;

    void notifySignalStrengthForPhoneId(int i, int i2, SignalStrength signalStrength) throws RemoteException;

    void notifySimActivationStateChangedForPhoneId(int i, int i2, int i3, int i4) throws RemoteException;

    void notifySubscriptionInfoChanged() throws RemoteException;

    void notifyUserMobileDataStateChangedForPhoneId(int i, int i2, boolean z) throws RemoteException;

    void notifyVoLteServiceStateChanged(VoLteServiceState voLteServiceState) throws RemoteException;

    void removeOnSubscriptionsChangedListener(String str, IOnSubscriptionsChangedListener iOnSubscriptionsChangedListener) throws RemoteException;
}
