package com.android.ims.internal;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IImsUt extends IInterface {

    public static abstract class Stub extends Binder implements IImsUt {
        private static final String DESCRIPTOR = "com.android.ims.internal.IImsUt";
        static final int TRANSACTION_close = 1;
        static final int TRANSACTION_getUtIMPUFromNetwork = 22;
        static final int TRANSACTION_isSupportCFT = 23;
        static final int TRANSACTION_processECT = 21;
        static final int TRANSACTION_queryCLIP = 7;
        static final int TRANSACTION_queryCLIR = 6;
        static final int TRANSACTION_queryCOLP = 9;
        static final int TRANSACTION_queryCOLR = 8;
        static final int TRANSACTION_queryCallBarring = 2;
        static final int TRANSACTION_queryCallBarringForServiceClass = 19;
        static final int TRANSACTION_queryCallForward = 3;
        static final int TRANSACTION_queryCallForwardForServiceClass = 4;
        static final int TRANSACTION_queryCallWaiting = 5;
        static final int TRANSACTION_setListener = 18;
        static final int TRANSACTION_transact = 10;
        static final int TRANSACTION_updateCLIP = 15;
        static final int TRANSACTION_updateCLIR = 14;
        static final int TRANSACTION_updateCOLP = 17;
        static final int TRANSACTION_updateCOLR = 16;
        static final int TRANSACTION_updateCallBarring = 11;
        static final int TRANSACTION_updateCallBarringForServiceClass = 20;
        static final int TRANSACTION_updateCallBarringOption = 24;
        static final int TRANSACTION_updateCallForward = 12;
        static final int TRANSACTION_updateCallForwardUncondTimer = 25;
        static final int TRANSACTION_updateCallWaiting = 13;

        private static class Proxy implements IImsUt {
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

            public void close() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int queryCallBarring(int cbType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cbType);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int queryCallForward(int condition, String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(condition);
                    _data.writeString(number);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int queryCallForwardForServiceClass(int condition, String number, int serviceClass) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(condition);
                    _data.writeString(number);
                    _data.writeInt(serviceClass);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int queryCallWaiting() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int queryCLIR() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int queryCLIP() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int queryCOLR() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int queryCOLP() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int transact(Bundle ssInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ssInfo != null) {
                        _data.writeInt(1);
                        ssInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int updateCallBarring(int cbType, int action, String[] barrList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cbType);
                    _data.writeInt(action);
                    _data.writeStringArray(barrList);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int updateCallForward(int action, int condition, String number, int serviceClass, int timeSeconds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(action);
                    _data.writeInt(condition);
                    _data.writeString(number);
                    _data.writeInt(serviceClass);
                    _data.writeInt(timeSeconds);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int updateCallWaiting(boolean enable, int serviceClass) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    _data.writeInt(serviceClass);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int updateCLIR(int clirMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(clirMode);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int updateCLIP(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int updateCOLR(int presentation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(presentation);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int updateCOLP(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setListener(IImsUtListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int queryCallBarringForServiceClass(int cbType, int serviceClass) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cbType);
                    _data.writeInt(serviceClass);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int updateCallBarringForServiceClass(int cbType, int action, String[] barrList, int serviceClass) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cbType);
                    _data.writeInt(action);
                    _data.writeStringArray(barrList);
                    _data.writeInt(serviceClass);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void processECT() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getUtIMPUFromNetwork() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isSupportCFT() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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

            public int updateCallBarringOption(String password, int cbType, boolean action, int serviceClass, String[] barrList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(password);
                    _data.writeInt(cbType);
                    _data.writeInt(action);
                    _data.writeInt(serviceClass);
                    _data.writeStringArray(barrList);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int updateCallForwardUncondTimer(int startHour, int startMinute, int endHour, int endMinute, int action, int condition, String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(startHour);
                    _data.writeInt(startMinute);
                    _data.writeInt(endHour);
                    _data.writeInt(endMinute);
                    _data.writeInt(action);
                    _data.writeInt(condition);
                    _data.writeString(number);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsUt asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IImsUt)) {
                return new Proxy(obj);
            }
            return (IImsUt) iin;
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
                boolean _arg0 = false;
                int _result;
                int _result2;
                int _result3;
                int _result4;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        close();
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        _result = queryCallBarring(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        _result2 = queryCallForward(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _result3 = queryCallForwardForServiceClass(data.readInt(), data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        _result4 = queryCallWaiting();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        _result4 = queryCLIR();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _result4 = queryCLIP();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        _result4 = queryCOLR();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _result4 = queryCOLP();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 10:
                        Bundle _arg02;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg02 = null;
                        }
                        _result = transact(_arg02);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _result3 = updateCallBarring(data.readInt(), data.readInt(), data.createStringArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        _result4 = updateCallForward(data.readInt(), data.readInt(), data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        _result2 = updateCallWaiting(_arg0, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _result = updateCLIR(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        _result = updateCLIP(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        _result = updateCOLR(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        _result = updateCOLP(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        setListener(com.android.ims.internal.IImsUtListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        _result2 = queryCallBarringForServiceClass(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        int _result5 = updateCallBarringForServiceClass(data.readInt(), data.readInt(), data.createStringArray(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        processECT();
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        String _result6 = getUtIMPUFromNetwork();
                        reply.writeNoException();
                        parcel2.writeString(_result6);
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        _arg0 = isSupportCFT();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        _result4 = updateCallBarringOption(data.readString(), data.readInt(), data.readInt() != 0, data.readInt(), data.createStringArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        _result4 = updateCallForwardUncondTimer(data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void close() throws RemoteException;

    String getUtIMPUFromNetwork() throws RemoteException;

    boolean isSupportCFT() throws RemoteException;

    void processECT() throws RemoteException;

    int queryCLIP() throws RemoteException;

    int queryCLIR() throws RemoteException;

    int queryCOLP() throws RemoteException;

    int queryCOLR() throws RemoteException;

    int queryCallBarring(int i) throws RemoteException;

    int queryCallBarringForServiceClass(int i, int i2) throws RemoteException;

    int queryCallForward(int i, String str) throws RemoteException;

    int queryCallForwardForServiceClass(int i, String str, int i2) throws RemoteException;

    int queryCallWaiting() throws RemoteException;

    void setListener(IImsUtListener iImsUtListener) throws RemoteException;

    int transact(Bundle bundle) throws RemoteException;

    int updateCLIP(boolean z) throws RemoteException;

    int updateCLIR(int i) throws RemoteException;

    int updateCOLP(boolean z) throws RemoteException;

    int updateCOLR(int i) throws RemoteException;

    int updateCallBarring(int i, int i2, String[] strArr) throws RemoteException;

    int updateCallBarringForServiceClass(int i, int i2, String[] strArr, int i3) throws RemoteException;

    int updateCallBarringOption(String str, int i, boolean z, int i2, String[] strArr) throws RemoteException;

    int updateCallForward(int i, int i2, String str, int i3, int i4) throws RemoteException;

    int updateCallForwardUncondTimer(int i, int i2, int i3, int i4, int i5, int i6, String str) throws RemoteException;

    int updateCallWaiting(boolean z, int i) throws RemoteException;
}
