package huawei.android.security.voicerecognition;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IVoiceRecognizeService extends IInterface {

    public static abstract class Stub extends Binder implements IVoiceRecognizeService {
        private static final String DESCRIPTOR = "huawei.android.security.voicerecognition.IVoiceRecognizeService";
        static final int TRANSACTION_cancelEnroll = 2;
        static final int TRANSACTION_continueEnroll = 16;
        static final int TRANSACTION_enroll = 1;
        static final int TRANSACTION_getEnrolledVoiceIdList = 7;
        static final int TRANSACTION_getHeadsetStatus = 14;
        static final int TRANSACTION_getHeadsetStatusByMac = 18;
        static final int TRANSACTION_getRemainingNum = 11;
        static final int TRANSACTION_getRemainingTime = 13;
        static final int TRANSACTION_getTotalAuthFailedTimes = 12;
        static final int TRANSACTION_getVoiceCommandList = 19;
        static final int TRANSACTION_getVoiceEnrollStringList = 15;
        static final int TRANSACTION_postEnroll = 9;
        static final int TRANSACTION_preEnroll = 8;
        static final int TRANSACTION_remove = 3;
        static final int TRANSACTION_removeAll = 4;
        static final int TRANSACTION_resetTimeout = 10;
        static final int TRANSACTION_setAuthCallback = 5;
        static final int TRANSACTION_setHeadsetStatusCallback = 6;
        static final int TRANSACTION_startVoiceActivity = 17;

        private static class Proxy implements IVoiceRecognizeService {
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

            public void enroll(IBinder token, byte[] authToken, int flags, int userId, IVoiceRecognizeServiceReceiver receiver, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeByteArray(authToken);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    _data.writeStrongBinder(receiver != null ? receiver.asBinder() : null);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void cancelEnroll(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void remove(IBinder token, int voiceId, int userId, IVoiceRecognizeServiceReceiver receiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(voiceId);
                    _data.writeInt(userId);
                    _data.writeStrongBinder(receiver != null ? receiver.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeAll(IBinder token, int userId, IVoiceRecognizeServiceReceiver receiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(userId);
                    _data.writeStrongBinder(receiver != null ? receiver.asBinder() : null);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAuthCallback(IVoiceAuthCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setHeadsetStatusCallback(IHeadsetStatusCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int[] getEnrolledVoiceIdList(int userId, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long preEnroll() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int postEnroll() throws RemoteException {
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

            public void resetTimeout() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getRemainingNum() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getTotalAuthFailedTimes() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getRemainingTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getHeadsetStatus() throws RemoteException {
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

            public List<String> getVoiceEnrollStringList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void continueEnroll() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startVoiceActivity(int voiceType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(voiceType);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getHeadsetStatusByMac(String mac) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(mac);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public VoiceCommandList getVoiceCommandList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    VoiceCommandList _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (VoiceCommandList) VoiceCommandList.CREATOR.createFromParcel(_reply);
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
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVoiceRecognizeService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IVoiceRecognizeService)) {
                return new Proxy(obj);
            }
            return (IVoiceRecognizeService) iin;
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
                long _result;
                int _result2;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        enroll(data.readStrongBinder(), data.createByteArray(), data.readInt(), data.readInt(), huawei.android.security.voicerecognition.IVoiceRecognizeServiceReceiver.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        cancelEnroll(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        remove(data.readStrongBinder(), data.readInt(), data.readInt(), huawei.android.security.voicerecognition.IVoiceRecognizeServiceReceiver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        removeAll(data.readStrongBinder(), data.readInt(), huawei.android.security.voicerecognition.IVoiceRecognizeServiceReceiver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        setAuthCallback(huawei.android.security.voicerecognition.IVoiceAuthCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        setHeadsetStatusCallback(huawei.android.security.voicerecognition.IHeadsetStatusCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        int[] _result3 = getEnrolledVoiceIdList(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeIntArray(_result3);
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        _result = preEnroll();
                        reply.writeNoException();
                        parcel2.writeLong(_result);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _result2 = postEnroll();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        resetTimeout();
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _result2 = getRemainingNum();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        _result2 = getTotalAuthFailedTimes();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        _result = getRemainingTime();
                        reply.writeNoException();
                        parcel2.writeLong(_result);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _result2 = getHeadsetStatus();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        List<String> _result4 = getVoiceEnrollStringList();
                        reply.writeNoException();
                        parcel2.writeStringList(_result4);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        continueEnroll();
                        reply.writeNoException();
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        startVoiceActivity(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        int _result5 = getHeadsetStatusByMac(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        VoiceCommandList _result6 = getVoiceCommandList();
                        reply.writeNoException();
                        if (_result6 != null) {
                            parcel2.writeInt(1);
                            _result6.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void cancelEnroll(IBinder iBinder) throws RemoteException;

    void continueEnroll() throws RemoteException;

    void enroll(IBinder iBinder, byte[] bArr, int i, int i2, IVoiceRecognizeServiceReceiver iVoiceRecognizeServiceReceiver, String str) throws RemoteException;

    int[] getEnrolledVoiceIdList(int i, String str) throws RemoteException;

    int getHeadsetStatus() throws RemoteException;

    int getHeadsetStatusByMac(String str) throws RemoteException;

    int getRemainingNum() throws RemoteException;

    long getRemainingTime() throws RemoteException;

    int getTotalAuthFailedTimes() throws RemoteException;

    VoiceCommandList getVoiceCommandList() throws RemoteException;

    List<String> getVoiceEnrollStringList() throws RemoteException;

    int postEnroll() throws RemoteException;

    long preEnroll() throws RemoteException;

    void remove(IBinder iBinder, int i, int i2, IVoiceRecognizeServiceReceiver iVoiceRecognizeServiceReceiver) throws RemoteException;

    void removeAll(IBinder iBinder, int i, IVoiceRecognizeServiceReceiver iVoiceRecognizeServiceReceiver) throws RemoteException;

    void resetTimeout() throws RemoteException;

    void setAuthCallback(IVoiceAuthCallback iVoiceAuthCallback) throws RemoteException;

    void setHeadsetStatusCallback(IHeadsetStatusCallback iHeadsetStatusCallback) throws RemoteException;

    void startVoiceActivity(int i) throws RemoteException;
}
