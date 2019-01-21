package com.huawei.attestation;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IHwAttestationService extends IInterface {

    public static abstract class Stub extends Binder implements IHwAttestationService {
        private static final String DESCRIPTOR = "com.huawei.attestation.IHwAttestationService";
        static final int TRANSACTION_getAttestationSignature = 1;
        static final int TRANSACTION_getAttestationSignatureWithPkgName = 7;
        static final int TRANSACTION_getDeviceCert = 6;
        static final int TRANSACTION_getDeviceCertType = 5;
        static final int TRANSACTION_getDeviceID = 3;
        static final int TRANSACTION_getLastError = 2;
        static final int TRANSACTION_getPublickKey = 4;

        private static class Proxy implements IHwAttestationService {
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

            public byte[] getAttestationSignature(int keyIndex, int deviceIdType, String signatureType, byte[] challenge) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(keyIndex);
                    _data.writeInt(deviceIdType);
                    _data.writeString(signatureType);
                    _data.writeByteArray(challenge);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getLastError() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] getDeviceID(int deviceIdType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceIdType);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPublickKey(int keyIndex, byte[] keyBuf) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(keyIndex);
                    _data.writeByteArray(keyBuf);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readByteArray(keyBuf);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDeviceCertType(int keyIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(keyIndex);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDeviceCert(int keyIndex, int certType, byte[] certBuf) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(keyIndex);
                    _data.writeInt(certType);
                    _data.writeByteArray(certBuf);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readByteArray(certBuf);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] getAttestationSignatureWithPkgName(int keyIndex, int deviceIdType, String signatureType, byte[] challenge, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(keyIndex);
                    _data.writeInt(deviceIdType);
                    _data.writeString(signatureType);
                    _data.writeByteArray(challenge);
                    _data.writeString(packageName);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
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

        public static IHwAttestationService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IHwAttestationService)) {
                return new Proxy(obj);
            }
            return (IHwAttestationService) iin;
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
                int _result;
                byte[] _result2;
                int _result3;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        byte[] _result4 = getAttestationSignature(data.readInt(), data.readInt(), data.readString(), data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeByteArray(_result4);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        _result = getLastError();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        _result2 = getDeviceID(data.readInt());
                        reply.writeNoException();
                        parcel2.writeByteArray(_result2);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _result = data.readInt();
                        _result2 = data.createByteArray();
                        int _result5 = getPublickKey(_result, _result2);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        parcel2.writeByteArray(_result2);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        _result3 = getDeviceCertType(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        _result = data.readInt();
                        _result3 = data.readInt();
                        byte[] _arg2 = data.createByteArray();
                        int _result6 = getDeviceCert(_result, _result3, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        parcel2.writeByteArray(_arg2);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        byte[] _result7 = getAttestationSignatureWithPkgName(data.readInt(), data.readInt(), data.readString(), data.createByteArray(), data.readString());
                        reply.writeNoException();
                        parcel2.writeByteArray(_result7);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    byte[] getAttestationSignature(int i, int i2, String str, byte[] bArr) throws RemoteException;

    byte[] getAttestationSignatureWithPkgName(int i, int i2, String str, byte[] bArr, String str2) throws RemoteException;

    int getDeviceCert(int i, int i2, byte[] bArr) throws RemoteException;

    int getDeviceCertType(int i) throws RemoteException;

    byte[] getDeviceID(int i) throws RemoteException;

    int getLastError() throws RemoteException;

    int getPublickKey(int i, byte[] bArr) throws RemoteException;
}
