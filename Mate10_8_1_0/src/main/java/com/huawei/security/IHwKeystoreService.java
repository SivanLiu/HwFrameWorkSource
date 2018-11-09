package com.huawei.security;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.huawei.security.keymaster.HwExportResult;
import com.huawei.security.keymaster.HwKeyCharacteristics;
import com.huawei.security.keymaster.HwKeymasterArguments;
import com.huawei.security.keymaster.HwKeymasterBlob;
import com.huawei.security.keymaster.HwKeymasterCertificateChain;
import com.huawei.security.keymaster.HwOperationResult;

public interface IHwKeystoreService extends IInterface {

    public static abstract class Stub extends Binder implements IHwKeystoreService {
        private static final String DESCRIPTOR = "com.huawei.security.IHwKeystoreService";
        static final int TRANSACTION_abort = 8;
        static final int TRANSACTION_attestDeviceIds = 13;
        static final int TRANSACTION_attestKey = 9;
        static final int TRANSACTION_begin = 5;
        static final int TRANSACTION_del = 1;
        static final int TRANSACTION_exportKey = 4;
        static final int TRANSACTION_exportTrustCert = 14;
        static final int TRANSACTION_finish = 7;
        static final int TRANSACTION_generateKey = 2;
        static final int TRANSACTION_get = 10;
        static final int TRANSACTION_getHuksServiceVersion = 12;
        static final int TRANSACTION_getKeyCharacteristics = 3;
        static final int TRANSACTION_set = 11;
        static final int TRANSACTION_update = 6;

        private static class Proxy implements IHwKeystoreService {
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

            public int del(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int generateKey(String alias, HwKeymasterArguments arguments, byte[] entropy, int uid, int flags, HwKeyCharacteristics characteristics) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    if (arguments != null) {
                        _data.writeInt(1);
                        arguments.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(entropy);
                    _data.writeInt(uid);
                    _data.writeInt(flags);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        characteristics.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getKeyCharacteristics(String alias, HwKeymasterBlob clientId, HwKeymasterBlob appId, int uid, HwKeyCharacteristics characteristics) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    if (clientId != null) {
                        _data.writeInt(1);
                        clientId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (appId != null) {
                        _data.writeInt(1);
                        appId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        characteristics.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public HwExportResult exportKey(String alias, int format, HwKeymasterBlob clientId, HwKeymasterBlob appId, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwExportResult hwExportResult;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(format);
                    if (clientId != null) {
                        _data.writeInt(1);
                        clientId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (appId != null) {
                        _data.writeInt(1);
                        appId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        hwExportResult = (HwExportResult) HwExportResult.CREATOR.createFromParcel(_reply);
                    } else {
                        hwExportResult = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return hwExportResult;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public HwOperationResult begin(IBinder appToken, String alias, int purpose, boolean pruneable, HwKeymasterArguments params, byte[] entropy, int uid) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwOperationResult hwOperationResult;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(appToken);
                    _data.writeString(alias);
                    _data.writeInt(purpose);
                    if (!pruneable) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(entropy);
                    _data.writeInt(uid);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        hwOperationResult = (HwOperationResult) HwOperationResult.CREATOR.createFromParcel(_reply);
                    } else {
                        hwOperationResult = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return hwOperationResult;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public HwOperationResult update(IBinder token, HwKeymasterArguments params, byte[] input) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwOperationResult hwOperationResult;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(input);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        hwOperationResult = (HwOperationResult) HwOperationResult.CREATOR.createFromParcel(_reply);
                    } else {
                        hwOperationResult = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return hwOperationResult;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public HwOperationResult finish(IBinder token, HwKeymasterArguments params, byte[] signature, byte[] entropy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwOperationResult hwOperationResult;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(signature);
                    _data.writeByteArray(entropy);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        hwOperationResult = (HwOperationResult) HwOperationResult.CREATOR.createFromParcel(_reply);
                    } else {
                        hwOperationResult = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return hwOperationResult;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int abort(IBinder handle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(handle);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int attestKey(String alias, int uid, HwKeymasterArguments params, HwKeymasterCertificateChain chain) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(uid);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        chain.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public HwExportResult get(String alias, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwExportResult hwExportResult;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(uid);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        hwExportResult = (HwExportResult) HwExportResult.CREATOR.createFromParcel(_reply);
                    } else {
                        hwExportResult = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return hwExportResult;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int set(String alias, HwKeymasterBlob blob, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    if (blob != null) {
                        _data.writeInt(1);
                        blob.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    this.mRemote.transact(Stub.TRANSACTION_set, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getHuksServiceVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getHuksServiceVersion, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int attestDeviceIds(HwKeymasterArguments params, HwKeymasterCertificateChain outChain) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_attestDeviceIds, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        outChain.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int exportTrustCert(HwKeymasterCertificateChain chain) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_exportTrustCert, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        chain.readFromParcel(_reply);
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

        public static IHwKeystoreService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IHwKeystoreService)) {
                return new Proxy(obj);
            }
            return (IHwKeystoreService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int _result;
            String _arg0;
            HwKeymasterArguments hwKeymasterArguments;
            int _arg3;
            HwKeymasterBlob hwKeymasterBlob;
            HwKeymasterBlob hwKeymasterBlob2;
            int _arg1;
            HwExportResult _result2;
            IBinder _arg02;
            HwOperationResult _result3;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    _result = del(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readString();
                    if (data.readInt() != 0) {
                        hwKeymasterArguments = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        hwKeymasterArguments = null;
                    }
                    byte[] _arg2 = data.createByteArray();
                    _arg3 = data.readInt();
                    int _arg4 = data.readInt();
                    HwKeyCharacteristics _arg5 = new HwKeyCharacteristics();
                    _result = generateKey(_arg0, hwKeymasterArguments, _arg2, _arg3, _arg4, _arg5);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    if (_arg5 != null) {
                        reply.writeInt(1);
                        _arg5.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readString();
                    if (data.readInt() != 0) {
                        hwKeymasterBlob = (HwKeymasterBlob) HwKeymasterBlob.CREATOR.createFromParcel(data);
                    } else {
                        hwKeymasterBlob = null;
                    }
                    if (data.readInt() != 0) {
                        hwKeymasterBlob2 = (HwKeymasterBlob) HwKeymasterBlob.CREATOR.createFromParcel(data);
                    } else {
                        hwKeymasterBlob2 = null;
                    }
                    _arg3 = data.readInt();
                    HwKeyCharacteristics _arg42 = new HwKeyCharacteristics();
                    _result = getKeyCharacteristics(_arg0, hwKeymasterBlob, hwKeymasterBlob2, _arg3, _arg42);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    if (_arg42 != null) {
                        reply.writeInt(1);
                        _arg42.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 4:
                    HwKeymasterBlob hwKeymasterBlob3;
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readString();
                    _arg1 = data.readInt();
                    if (data.readInt() != 0) {
                        hwKeymasterBlob2 = (HwKeymasterBlob) HwKeymasterBlob.CREATOR.createFromParcel(data);
                    } else {
                        hwKeymasterBlob2 = null;
                    }
                    if (data.readInt() != 0) {
                        hwKeymasterBlob3 = (HwKeymasterBlob) HwKeymasterBlob.CREATOR.createFromParcel(data);
                    } else {
                        hwKeymasterBlob3 = null;
                    }
                    _result2 = exportKey(_arg0, _arg1, hwKeymasterBlob2, hwKeymasterBlob3, data.readInt());
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 5:
                    HwKeymasterArguments hwKeymasterArguments2;
                    data.enforceInterface(DESCRIPTOR);
                    _arg02 = data.readStrongBinder();
                    String _arg12 = data.readString();
                    int _arg22 = data.readInt();
                    boolean _arg32 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        hwKeymasterArguments2 = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        hwKeymasterArguments2 = null;
                    }
                    _result3 = begin(_arg02, _arg12, _arg22, _arg32, hwKeymasterArguments2, data.createByteArray(), data.readInt());
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    _arg02 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        hwKeymasterArguments = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        hwKeymasterArguments = null;
                    }
                    _result3 = update(_arg02, hwKeymasterArguments, data.createByteArray());
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    _arg02 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        hwKeymasterArguments = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        hwKeymasterArguments = null;
                    }
                    _result3 = finish(_arg02, hwKeymasterArguments, data.createByteArray(), data.createByteArray());
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    _result = abort(data.readStrongBinder());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 9:
                    HwKeymasterArguments hwKeymasterArguments3;
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readString();
                    _arg1 = data.readInt();
                    if (data.readInt() != 0) {
                        hwKeymasterArguments3 = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        hwKeymasterArguments3 = null;
                    }
                    HwKeymasterCertificateChain _arg33 = new HwKeymasterCertificateChain();
                    _result = attestKey(_arg0, _arg1, hwKeymasterArguments3, _arg33);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    if (_arg33 != null) {
                        reply.writeInt(1);
                        _arg33.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = get(data.readString(), data.readInt());
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case TRANSACTION_set /*11*/:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readString();
                    if (data.readInt() != 0) {
                        hwKeymasterBlob = (HwKeymasterBlob) HwKeymasterBlob.CREATOR.createFromParcel(data);
                    } else {
                        hwKeymasterBlob = null;
                    }
                    _result = set(_arg0, hwKeymasterBlob, data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case TRANSACTION_getHuksServiceVersion /*12*/:
                    data.enforceInterface(DESCRIPTOR);
                    String _result4 = getHuksServiceVersion();
                    reply.writeNoException();
                    reply.writeString(_result4);
                    return true;
                case TRANSACTION_attestDeviceIds /*13*/:
                    HwKeymasterArguments hwKeymasterArguments4;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        hwKeymasterArguments4 = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        hwKeymasterArguments4 = null;
                    }
                    HwKeymasterCertificateChain _arg13 = new HwKeymasterCertificateChain();
                    _result = attestDeviceIds(hwKeymasterArguments4, _arg13);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    if (_arg13 != null) {
                        reply.writeInt(1);
                        _arg13.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case TRANSACTION_exportTrustCert /*14*/:
                    data.enforceInterface(DESCRIPTOR);
                    HwKeymasterCertificateChain _arg03 = new HwKeymasterCertificateChain();
                    _result = exportTrustCert(_arg03);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    if (_arg03 != null) {
                        reply.writeInt(1);
                        _arg03.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    int abort(IBinder iBinder) throws RemoteException;

    int attestDeviceIds(HwKeymasterArguments hwKeymasterArguments, HwKeymasterCertificateChain hwKeymasterCertificateChain) throws RemoteException;

    int attestKey(String str, int i, HwKeymasterArguments hwKeymasterArguments, HwKeymasterCertificateChain hwKeymasterCertificateChain) throws RemoteException;

    HwOperationResult begin(IBinder iBinder, String str, int i, boolean z, HwKeymasterArguments hwKeymasterArguments, byte[] bArr, int i2) throws RemoteException;

    int del(String str, int i) throws RemoteException;

    HwExportResult exportKey(String str, int i, HwKeymasterBlob hwKeymasterBlob, HwKeymasterBlob hwKeymasterBlob2, int i2) throws RemoteException;

    int exportTrustCert(HwKeymasterCertificateChain hwKeymasterCertificateChain) throws RemoteException;

    HwOperationResult finish(IBinder iBinder, HwKeymasterArguments hwKeymasterArguments, byte[] bArr, byte[] bArr2) throws RemoteException;

    int generateKey(String str, HwKeymasterArguments hwKeymasterArguments, byte[] bArr, int i, int i2, HwKeyCharacteristics hwKeyCharacteristics) throws RemoteException;

    HwExportResult get(String str, int i) throws RemoteException;

    String getHuksServiceVersion() throws RemoteException;

    int getKeyCharacteristics(String str, HwKeymasterBlob hwKeymasterBlob, HwKeymasterBlob hwKeymasterBlob2, int i, HwKeyCharacteristics hwKeyCharacteristics) throws RemoteException;

    int set(String str, HwKeymasterBlob hwKeymasterBlob, int i) throws RemoteException;

    HwOperationResult update(IBinder iBinder, HwKeymasterArguments hwKeymasterArguments, byte[] bArr) throws RemoteException;
}
