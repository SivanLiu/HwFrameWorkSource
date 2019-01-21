package huawei.android.security;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ITrustCircleManager extends IInterface {

    public static abstract class Stub extends Binder implements ITrustCircleManager {
        private static final String DESCRIPTOR = "huawei.android.security.ITrustCircleManager";
        static final int TRANSACTION_cancelAuthentication = 17;
        static final int TRANSACTION_cancelRegOrLogin = 8;
        static final int TRANSACTION_finalLogin = 7;
        static final int TRANSACTION_finalRegister = 6;
        static final int TRANSACTION_getCurrentState = 3;
        static final int TRANSACTION_getTcisInfo = 1;
        static final int TRANSACTION_initAuthenticate = 11;
        static final int TRANSACTION_initKeyAgreement = 2;
        static final int TRANSACTION_loginServerRequest = 4;
        static final int TRANSACTION_logout = 9;
        static final int TRANSACTION_receiveAck = 14;
        static final int TRANSACTION_receiveAuthSync = 12;
        static final int TRANSACTION_receiveAuthSyncAck = 13;
        static final int TRANSACTION_receivePK = 16;
        static final int TRANSACTION_requestPK = 15;
        static final int TRANSACTION_unregister = 10;
        static final int TRANSACTION_updateServerRequest = 5;

        private static class Proxy implements ITrustCircleManager {
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

            public Bundle getTcisInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bundle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Bundle) Bundle.CREATOR.createFromParcel(_reply);
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

            public long initKeyAgreement(IKaCallback callBack, int kaVersion, long userId, byte[] aesTmpKey, String kaInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callBack != null ? callBack.asBinder() : null);
                    _data.writeInt(kaVersion);
                    _data.writeLong(userId);
                    _data.writeByteArray(aesTmpKey);
                    _data.writeString(kaInfo);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCurrentState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void loginServerRequest(ILifeCycleCallback callback, long userID, int serverRegisterStatus, String sessionID) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeLong(userID);
                    _data.writeInt(serverRegisterStatus);
                    _data.writeString(sessionID);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateServerRequest(ILifeCycleCallback callback, long userID) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeLong(userID);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void finalRegister(ILifeCycleCallback callback, String authPKData, String authPKDataSign, String updateIndexInfo, String updateIndexSignature) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(authPKData);
                    _data.writeString(authPKDataSign);
                    _data.writeString(updateIndexInfo);
                    _data.writeString(updateIndexSignature);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void finalLogin(ILifeCycleCallback callback, int updateResult, String updateIndexInfo, String updateIndexSignature) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(updateResult);
                    _data.writeString(updateIndexInfo);
                    _data.writeString(updateIndexSignature);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void cancelRegOrLogin(ILifeCycleCallback callback, long userID) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeLong(userID);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void logout(ILifeCycleCallback callback, long userID) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeLong(userID);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregister(ILifeCycleCallback callback, long userID) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeLong(userID);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long initAuthenticate(IAuthCallback callback, int authType, int authVersion, int policy, long userID, byte[] AESTmpKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(authType);
                    _data.writeInt(authVersion);
                    _data.writeInt(policy);
                    _data.writeLong(userID);
                    _data.writeByteArray(AESTmpKey);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long receiveAuthSync(IAuthCallback callback, int authType, int authVersion, int taVersion, int policy, long userID, byte[] AESTmpKey, byte[] tcisId, int pkVersion, long nonce, int authKeyAlgoType, byte[] authKeyInfo, byte[] authKeyInfoSign) throws RemoteException {
                Throwable th;
                byte[] bArr;
                byte[] bArr2;
                int i;
                long j;
                int i2;
                long j2;
                int i3;
                int i4;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(authType);
                    try {
                        _data.writeInt(authVersion);
                        try {
                            _data.writeInt(taVersion);
                            try {
                                _data.writeInt(policy);
                                try {
                                    _data.writeLong(userID);
                                } catch (Throwable th2) {
                                    th = th2;
                                    bArr = AESTmpKey;
                                    bArr2 = tcisId;
                                    i = pkVersion;
                                    j = nonce;
                                    i2 = authKeyAlgoType;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                j2 = userID;
                                bArr = AESTmpKey;
                                bArr2 = tcisId;
                                i = pkVersion;
                                j = nonce;
                                i2 = authKeyAlgoType;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            i3 = policy;
                            j2 = userID;
                            bArr = AESTmpKey;
                            bArr2 = tcisId;
                            i = pkVersion;
                            j = nonce;
                            i2 = authKeyAlgoType;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeByteArray(AESTmpKey);
                            try {
                                _data.writeByteArray(tcisId);
                                try {
                                    _data.writeInt(pkVersion);
                                } catch (Throwable th5) {
                                    th = th5;
                                    j = nonce;
                                    i2 = authKeyAlgoType;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th6) {
                                th = th6;
                                i = pkVersion;
                                j = nonce;
                                i2 = authKeyAlgoType;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            bArr2 = tcisId;
                            i = pkVersion;
                            j = nonce;
                            i2 = authKeyAlgoType;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeLong(nonce);
                            try {
                                _data.writeInt(authKeyAlgoType);
                                _data.writeByteArray(authKeyInfo);
                                _data.writeByteArray(authKeyInfoSign);
                                this.mRemote.transact(12, _data, _reply, 0);
                                _reply.readException();
                                long _result = _reply.readLong();
                                _reply.recycle();
                                _data.recycle();
                                return _result;
                            } catch (Throwable th8) {
                                th = th8;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            i2 = authKeyAlgoType;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        i4 = taVersion;
                        i3 = policy;
                        j2 = userID;
                        bArr = AESTmpKey;
                        bArr2 = tcisId;
                        i = pkVersion;
                        j = nonce;
                        i2 = authKeyAlgoType;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th11) {
                    th = th11;
                    int i5 = authVersion;
                    i4 = taVersion;
                    i3 = policy;
                    j2 = userID;
                    bArr = AESTmpKey;
                    bArr2 = tcisId;
                    i = pkVersion;
                    j = nonce;
                    i2 = authKeyAlgoType;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public boolean receiveAuthSyncAck(long authID, byte[] tcisIdSlave, int pkVersionSlave, long nonceSlave, byte[] mac, int authKeyAlgoTypeSlave, byte[] authKeyInfoSlave, byte[] authKeyInfoSignSlave) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(authID);
                    _data.writeByteArray(tcisIdSlave);
                    _data.writeInt(pkVersionSlave);
                    _data.writeLong(nonceSlave);
                    _data.writeByteArray(mac);
                    _data.writeInt(authKeyAlgoTypeSlave);
                    _data.writeByteArray(authKeyInfoSlave);
                    _data.writeByteArray(authKeyInfoSignSlave);
                    boolean z = false;
                    this.mRemote.transact(13, _data, _reply, 0);
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

            public boolean receiveAck(long authID, byte[] mac) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(authID);
                    _data.writeByteArray(mac);
                    boolean z = false;
                    this.mRemote.transact(14, _data, _reply, 0);
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

            public boolean requestPK(long authID, long userID) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(authID);
                    _data.writeLong(userID);
                    boolean z = false;
                    this.mRemote.transact(15, _data, _reply, 0);
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

            public boolean receivePK(long authID, int authKeyAlgoType, byte[] authKeyData, byte[] authKeyDataSign) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(authID);
                    _data.writeInt(authKeyAlgoType);
                    _data.writeByteArray(authKeyData);
                    _data.writeByteArray(authKeyDataSign);
                    boolean z = false;
                    this.mRemote.transact(16, _data, _reply, 0);
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

            public void cancelAuthentication(long authId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(authId);
                    this.mRemote.transact(17, _data, _reply, 0);
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

        public static ITrustCircleManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ITrustCircleManager)) {
                return new Proxy(obj);
            }
            return (ITrustCircleManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            boolean z;
            if (i != 1598968902) {
                Parcel parcel3;
                long _result;
                boolean z2;
                boolean _result2;
                switch (i) {
                    case 1:
                        int i2 = 1;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        Bundle _result3 = getTcisInfo();
                        reply.writeNoException();
                        if (_result3 != null) {
                            parcel3.writeInt(i2);
                            _result3.writeToParcel(parcel3, i2);
                        } else {
                            parcel3.writeInt(0);
                        }
                        return i2;
                    case 2:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        _result = initKeyAgreement(huawei.android.security.IKaCallback.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readLong(), data.createByteArray(), data.readString());
                        reply.writeNoException();
                        parcel3.writeLong(_result);
                        return z;
                    case 3:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        int _result4 = getCurrentState();
                        reply.writeNoException();
                        parcel3.writeInt(_result4);
                        return z;
                    case 4:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        loginServerRequest(huawei.android.security.ILifeCycleCallback.Stub.asInterface(data.readStrongBinder()), data.readLong(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return z;
                    case 5:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        updateServerRequest(huawei.android.security.ILifeCycleCallback.Stub.asInterface(data.readStrongBinder()), data.readLong());
                        reply.writeNoException();
                        return z;
                    case 6:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        finalRegister(huawei.android.security.ILifeCycleCallback.Stub.asInterface(data.readStrongBinder()), data.readString(), data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return z;
                    case 7:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        finalLogin(huawei.android.security.ILifeCycleCallback.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        return z;
                    case 8:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        cancelRegOrLogin(huawei.android.security.ILifeCycleCallback.Stub.asInterface(data.readStrongBinder()), data.readLong());
                        reply.writeNoException();
                        return z;
                    case 9:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        logout(huawei.android.security.ILifeCycleCallback.Stub.asInterface(data.readStrongBinder()), data.readLong());
                        reply.writeNoException();
                        return z;
                    case 10:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        unregister(huawei.android.security.ILifeCycleCallback.Stub.asInterface(data.readStrongBinder()), data.readLong());
                        reply.writeNoException();
                        return z;
                    case 11:
                        z = true;
                        parcel3 = parcel2;
                        data.enforceInterface(descriptor);
                        _result = initAuthenticate(huawei.android.security.IAuthCallback.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readInt(), data.readInt(), data.readLong(), data.createByteArray());
                        reply.writeNoException();
                        parcel3.writeLong(_result);
                        return z;
                    case 12:
                        z2 = true;
                        parcel.enforceInterface(descriptor);
                        _result = receiveAuthSync(huawei.android.security.IAuthCallback.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readLong(), data.createByteArray(), data.createByteArray(), data.readInt(), data.readLong(), data.readInt(), data.createByteArray(), data.createByteArray());
                        reply.writeNoException();
                        reply.writeLong(_result);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        z2 = true;
                        _result2 = receiveAuthSyncAck(data.readLong(), data.createByteArray(), data.readInt(), data.readLong(), data.createByteArray(), data.readInt(), data.createByteArray(), data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return z2;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        boolean _result5 = receiveAck(data.readLong(), data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        boolean _result6 = requestPK(data.readLong(), data.readLong());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        _result2 = receivePK(data.readLong(), data.readInt(), data.createByteArray(), data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        cancelAuthentication(data.readLong());
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            z = true;
            Parcel parcel4 = parcel;
            parcel2.writeString(descriptor);
            return z;
        }
    }

    void cancelAuthentication(long j) throws RemoteException;

    void cancelRegOrLogin(ILifeCycleCallback iLifeCycleCallback, long j) throws RemoteException;

    void finalLogin(ILifeCycleCallback iLifeCycleCallback, int i, String str, String str2) throws RemoteException;

    void finalRegister(ILifeCycleCallback iLifeCycleCallback, String str, String str2, String str3, String str4) throws RemoteException;

    int getCurrentState() throws RemoteException;

    Bundle getTcisInfo() throws RemoteException;

    long initAuthenticate(IAuthCallback iAuthCallback, int i, int i2, int i3, long j, byte[] bArr) throws RemoteException;

    long initKeyAgreement(IKaCallback iKaCallback, int i, long j, byte[] bArr, String str) throws RemoteException;

    void loginServerRequest(ILifeCycleCallback iLifeCycleCallback, long j, int i, String str) throws RemoteException;

    void logout(ILifeCycleCallback iLifeCycleCallback, long j) throws RemoteException;

    boolean receiveAck(long j, byte[] bArr) throws RemoteException;

    long receiveAuthSync(IAuthCallback iAuthCallback, int i, int i2, int i3, int i4, long j, byte[] bArr, byte[] bArr2, int i5, long j2, int i6, byte[] bArr3, byte[] bArr4) throws RemoteException;

    boolean receiveAuthSyncAck(long j, byte[] bArr, int i, long j2, byte[] bArr2, int i2, byte[] bArr3, byte[] bArr4) throws RemoteException;

    boolean receivePK(long j, int i, byte[] bArr, byte[] bArr2) throws RemoteException;

    boolean requestPK(long j, long j2) throws RemoteException;

    void unregister(ILifeCycleCallback iLifeCycleCallback, long j) throws RemoteException;

    void updateServerRequest(ILifeCycleCallback iLifeCycleCallback, long j) throws RemoteException;
}
