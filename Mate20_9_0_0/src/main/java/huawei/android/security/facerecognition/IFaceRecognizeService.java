package huawei.android.security.facerecognition;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IFaceRecognizeService extends IInterface {

    public static abstract class Stub extends Binder implements IFaceRecognizeService {
        private static final String DESCRIPTOR = "huawei.android.security.facerecognition.IFaceRecognizeService";
        static final int TRANSACTION_authenticate = 1;
        static final int TRANSACTION_cancelAuthentication = 2;
        static final int TRANSACTION_cancelEnrollment = 4;
        static final int TRANSACTION_enroll = 3;
        static final int TRANSACTION_getAngleDim = 18;
        static final int TRANSACTION_getEnrolledFaceRecognizes = 7;
        static final int TRANSACTION_getHardwareSupportType = 8;
        static final int TRANSACTION_getPayResult = 20;
        static final int TRANSACTION_getRemainingNum = 12;
        static final int TRANSACTION_getRemainingTime = 14;
        static final int TRANSACTION_getTotalAuthFailedTimes = 13;
        static final int TRANSACTION_hasAlternateAppearance = 21;
        static final int TRANSACTION_init = 15;
        static final int TRANSACTION_postEnroll = 10;
        static final int TRANSACTION_preEnroll = 9;
        static final int TRANSACTION_preparePayInfo = 19;
        static final int TRANSACTION_release = 16;
        static final int TRANSACTION_remove = 5;
        static final int TRANSACTION_rename = 6;
        static final int TRANSACTION_resetTimeout = 11;
        static final int TRANSACTION_setSecureFaceMode = 17;

        private static class Proxy implements IFaceRecognizeService {
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

            public void authenticate(IBinder token, long sessionId, int flags, int userId, IFaceRecognizeServiceReceiver receiver, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeLong(sessionId);
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

            public void cancelAuthentication(IBinder token, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enroll(IBinder token, byte[] authToken, int flags, int userId, IFaceRecognizeServiceReceiver receiver, String opPackageName) throws RemoteException {
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
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void cancelEnrollment(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void remove(IBinder token, int faceId, int userId, IFaceRecognizeServiceReceiver receiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(faceId);
                    _data.writeInt(userId);
                    _data.writeStrongBinder(receiver != null ? receiver.asBinder() : null);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void rename(int faceId, int userId, String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(faceId);
                    _data.writeInt(userId);
                    _data.writeString(name);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<FaceRecognition> getEnrolledFaceRecognizes(int userId, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    List<FaceRecognition> _result = _reply.createTypedArrayList(FaceRecognition.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getHardwareSupportType() throws RemoteException {
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

            public long preEnroll(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int postEnroll(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void resetTimeout(byte[] token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(token);
                    this.mRemote.transact(11, _data, _reply, 0);
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
                    this.mRemote.transact(12, _data, _reply, 0);
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
                    this.mRemote.transact(13, _data, _reply, 0);
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
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int init(IBinder token, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(packageName);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int release(IBinder token, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(packageName);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setSecureFaceMode(IBinder token, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(mode);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getAngleDim(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int preparePayInfo(IBinder token, byte[] aaid, byte[] nonce, byte[] reserve) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeByteArray(aaid);
                    _data.writeByteArray(nonce);
                    _data.writeByteArray(reserve);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readByteArray(reserve);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPayResult(IBinder token, int[] faceId, byte[] tokenResult, int[] tokenResultLen, byte[] reserve) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (faceId == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(faceId.length);
                    }
                    if (tokenResult == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(tokenResult.length);
                    }
                    if (tokenResultLen == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(tokenResultLen.length);
                    }
                    _data.writeByteArray(reserve);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readIntArray(faceId);
                    _reply.readByteArray(tokenResult);
                    _reply.readIntArray(tokenResultLen);
                    _reply.readByteArray(reserve);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int hasAlternateAppearance(IBinder token, int faceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(faceId);
                    this.mRemote.transact(21, _data, _reply, 0);
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

        public static IFaceRecognizeService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IFaceRecognizeService)) {
                return new Proxy(obj);
            }
            return (IFaceRecognizeService) iin;
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
                boolean z;
                int _result;
                int _result2;
                int _result3;
                int _result4;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        authenticate(data.readStrongBinder(), data.readLong(), data.readInt(), data.readInt(), huawei.android.security.facerecognition.IFaceRecognizeServiceReceiver.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        cancelAuthentication(data.readStrongBinder(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        enroll(data.readStrongBinder(), data.createByteArray(), data.readInt(), data.readInt(), huawei.android.security.facerecognition.IFaceRecognizeServiceReceiver.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 4:
                        boolean z2 = true;
                        parcel.enforceInterface(descriptor);
                        cancelEnrollment(data.readStrongBinder());
                        reply.writeNoException();
                        return z2;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        remove(data.readStrongBinder(), data.readInt(), data.readInt(), huawei.android.security.facerecognition.IFaceRecognizeServiceReceiver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        rename(data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        List<FaceRecognition> _result5 = getEnrolledFaceRecognizes(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeTypedList(_result5);
                        return true;
                    case 8:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = getHardwareSupportType();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        long _result6 = preEnroll(data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeLong(_result6);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        _result2 = postEnroll(data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 11:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        resetTimeout(data.createByteArray());
                        reply.writeNoException();
                        return z;
                    case 12:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = getRemainingNum();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 13:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = getTotalAuthFailedTimes();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 14:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        long _result7 = getRemainingTime();
                        reply.writeNoException();
                        parcel2.writeLong(_result7);
                        return z;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        _result3 = init(data.readStrongBinder(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        _result3 = release(data.readStrongBinder(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _result3 = setSecureFaceMode(data.readStrongBinder(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        _result2 = getAngleDim(data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        IBinder _arg0 = data.readStrongBinder();
                        byte[] _arg1 = data.createByteArray();
                        byte[] _arg2 = data.createByteArray();
                        byte[] _arg3 = data.createByteArray();
                        _result4 = preparePayInfo(_arg0, _arg1, _arg2, _arg3);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        parcel2.writeByteArray(_arg3);
                        return true;
                    case 20:
                        int[] _arg12;
                        byte[] _arg22;
                        parcel.enforceInterface(descriptor);
                        IBinder _arg02 = data.readStrongBinder();
                        int _arg1_length = data.readInt();
                        if (_arg1_length < 0) {
                            _arg12 = null;
                        } else {
                            _arg12 = new int[_arg1_length];
                        }
                        int[] _arg13 = _arg12;
                        int _arg2_length = data.readInt();
                        if (_arg2_length < 0) {
                            _arg22 = null;
                        } else {
                            _arg22 = new byte[_arg2_length];
                        }
                        byte[] _arg23 = _arg22;
                        _result4 = data.readInt();
                        if (_result4 < 0) {
                            _arg12 = null;
                        } else {
                            _arg12 = new int[_result4];
                        }
                        int[] _arg32 = _arg12;
                        byte[] _arg4 = data.createByteArray();
                        int[] _arg33 = _arg32;
                        byte[] _arg24 = _arg23;
                        _result = getPayResult(_arg02, _arg13, _arg23, _arg33, _arg4);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        parcel2.writeIntArray(_arg13);
                        parcel2.writeByteArray(_arg24);
                        parcel2.writeIntArray(_arg33);
                        parcel2.writeByteArray(_arg4);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        _result3 = hasAlternateAppearance(data.readStrongBinder(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            boolean z3 = true;
            parcel2.writeString(descriptor);
            return z3;
        }
    }

    void authenticate(IBinder iBinder, long j, int i, int i2, IFaceRecognizeServiceReceiver iFaceRecognizeServiceReceiver, String str) throws RemoteException;

    void cancelAuthentication(IBinder iBinder, String str) throws RemoteException;

    void cancelEnrollment(IBinder iBinder) throws RemoteException;

    void enroll(IBinder iBinder, byte[] bArr, int i, int i2, IFaceRecognizeServiceReceiver iFaceRecognizeServiceReceiver, String str) throws RemoteException;

    int getAngleDim(IBinder iBinder) throws RemoteException;

    List<FaceRecognition> getEnrolledFaceRecognizes(int i, String str) throws RemoteException;

    int getHardwareSupportType() throws RemoteException;

    int getPayResult(IBinder iBinder, int[] iArr, byte[] bArr, int[] iArr2, byte[] bArr2) throws RemoteException;

    int getRemainingNum() throws RemoteException;

    long getRemainingTime() throws RemoteException;

    int getTotalAuthFailedTimes() throws RemoteException;

    int hasAlternateAppearance(IBinder iBinder, int i) throws RemoteException;

    int init(IBinder iBinder, String str) throws RemoteException;

    int postEnroll(IBinder iBinder) throws RemoteException;

    long preEnroll(IBinder iBinder) throws RemoteException;

    int preparePayInfo(IBinder iBinder, byte[] bArr, byte[] bArr2, byte[] bArr3) throws RemoteException;

    int release(IBinder iBinder, String str) throws RemoteException;

    void remove(IBinder iBinder, int i, int i2, IFaceRecognizeServiceReceiver iFaceRecognizeServiceReceiver) throws RemoteException;

    void rename(int i, int i2, String str) throws RemoteException;

    void resetTimeout(byte[] bArr) throws RemoteException;

    int setSecureFaceMode(IBinder iBinder, int i) throws RemoteException;
}
