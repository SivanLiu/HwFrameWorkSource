package android.vrsystem;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import java.util.List;

public interface IVRListener extends IInterface {

    public static abstract class Stub extends Binder implements IVRListener {
        private static final String DESCRIPTOR = "android.vrsystem.IVRListener";
        static final int TRANSACTION_onBatteryChanged = 5;
        static final int TRANSACTION_onCallback = 9;
        static final int TRANSACTION_onHeartBeatChanged = 8;
        static final int TRANSACTION_onHelmetChanged = 7;
        static final int TRANSACTION_onModeChanged = 1;
        static final int TRANSACTION_onNetworkStateChanged = 6;
        static final int TRANSACTION_onNewNotification = 4;
        static final int TRANSACTION_onNewSMS = 3;
        static final int TRANSACTION_onPhoneStateChanged = 2;

        private static class Proxy implements IVRListener {
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

            public void onModeChanged(boolean isVR) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(isVR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onPhoneStateChanged(int subId, int state, String incomingNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(state);
                    _data.writeString(incomingNumber);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNewSMS(Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNewNotification(StatusBarNotification sbn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sbn != null) {
                        _data.writeInt(1);
                        sbn.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onBatteryChanged(Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onNetworkStateChanged(Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onHelmetChanged(boolean up) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(up);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onHeartBeatChanged(boolean connected) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(connected);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onCallback(String callbackType, List params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callbackType);
                    _data.writeList(params);
                    this.mRemote.transact(9, _data, _reply, 0);
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

        public static IVRListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IVRListener)) {
                return new Proxy(obj);
            }
            return (IVRListener) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                boolean _arg0 = false;
                Intent _arg02 = null;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        onModeChanged(_arg0);
                        reply.writeNoException();
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        onPhoneStateChanged(data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (Intent) Intent.CREATOR.createFromParcel(data);
                        }
                        onNewSMS(_arg02);
                        reply.writeNoException();
                        return true;
                    case 4:
                        StatusBarNotification _arg03;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (StatusBarNotification) StatusBarNotification.CREATOR.createFromParcel(data);
                        }
                        onNewNotification(_arg03);
                        reply.writeNoException();
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (Intent) Intent.CREATOR.createFromParcel(data);
                        }
                        onBatteryChanged(_arg02);
                        reply.writeNoException();
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (Intent) Intent.CREATOR.createFromParcel(data);
                        }
                        onNetworkStateChanged(_arg02);
                        reply.writeNoException();
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        onHelmetChanged(_arg0);
                        reply.writeNoException();
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        onHeartBeatChanged(_arg0);
                        reply.writeNoException();
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        onCallback(data.readString(), data.readArrayList(getClass().getClassLoader()));
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

    void onBatteryChanged(Intent intent) throws RemoteException;

    void onCallback(String str, List list) throws RemoteException;

    void onHeartBeatChanged(boolean z) throws RemoteException;

    void onHelmetChanged(boolean z) throws RemoteException;

    void onModeChanged(boolean z) throws RemoteException;

    void onNetworkStateChanged(Intent intent) throws RemoteException;

    void onNewNotification(StatusBarNotification statusBarNotification) throws RemoteException;

    void onNewSMS(Intent intent) throws RemoteException;

    void onPhoneStateChanged(int i, int i2, String str) throws RemoteException;
}
