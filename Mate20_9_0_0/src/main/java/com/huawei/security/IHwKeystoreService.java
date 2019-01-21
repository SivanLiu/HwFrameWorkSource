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
        static final int TRANSACTION_assetHandleReq = 14;
        static final int TRANSACTION_attestDeviceIds = 13;
        static final int TRANSACTION_attestKey = 9;
        static final int TRANSACTION_begin = 5;
        static final int TRANSACTION_contains = 17;
        static final int TRANSACTION_del = 1;
        static final int TRANSACTION_exportKey = 4;
        static final int TRANSACTION_exportTrustCert = 15;
        static final int TRANSACTION_finish = 7;
        static final int TRANSACTION_generateKey = 2;
        static final int TRANSACTION_get = 10;
        static final int TRANSACTION_getHuksServiceVersion = 12;
        static final int TRANSACTION_getKeyCharacteristics = 3;
        static final int TRANSACTION_set = 11;
        static final int TRANSACTION_setKeyProtection = 16;
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
                    HwExportResult _result;
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
                        _result = (HwExportResult) HwExportResult.CREATOR.createFromParcel(_reply);
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

            public HwOperationResult begin(IBinder appToken, String alias, int purpose, boolean pruneable, HwKeymasterArguments params, byte[] entropy, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwOperationResult _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(appToken);
                    _data.writeString(alias);
                    _data.writeInt(purpose);
                    _data.writeInt(pruneable);
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
                        _result = (HwOperationResult) HwOperationResult.CREATOR.createFromParcel(_reply);
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

            public HwOperationResult update(IBinder token, HwKeymasterArguments params, byte[] input) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwOperationResult _result;
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
                        _result = (HwOperationResult) HwOperationResult.CREATOR.createFromParcel(_reply);
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

            public HwOperationResult finish(IBinder token, HwKeymasterArguments params, byte[] signature, byte[] entropy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwOperationResult _result;
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
                        _result = (HwOperationResult) HwOperationResult.CREATOR.createFromParcel(_reply);
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
                    HwExportResult _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(uid);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (HwExportResult) HwExportResult.CREATOR.createFromParcel(_reply);
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

            public int assetHandleReq(HwKeymasterArguments params, HwKeymasterCertificateChain outResult) throws RemoteException {
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
                    this.mRemote.transact(Stub.TRANSACTION_assetHandleReq, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        outResult.readFromParcel(_reply);
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
                    this.mRemote.transact(15, _data, _reply, 0);
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

            public int setKeyProtection(String alias, HwKeymasterArguments params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int contains(String alias) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    this.mRemote.transact(Stub.TRANSACTION_contains, _data, _reply, 0);
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
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                HwKeymasterArguments _arg1 = null;
                int _result;
                int _result2;
                String _arg0;
                int _arg3;
                IBinder _arg02;
                int _result3;
                String _arg03;
                HwKeymasterCertificateChain _arg12;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        _result = del(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        String _arg04 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        HwKeymasterArguments _arg13 = _arg1;
                        byte[] _arg2 = data.createByteArray();
                        int _arg32 = data.readInt();
                        int _arg4 = data.readInt();
                        HwKeyCharacteristics _arg5 = new HwKeyCharacteristics();
                        HwKeyCharacteristics _arg52 = _arg5;
                        _result2 = generateKey(_arg04, _arg13, _arg2, _arg32, _arg4, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        parcel2.writeInt(1);
                        _arg52.writeToParcel(parcel2, 1);
                        return true;
                    case 3:
                        HwKeymasterBlob _arg14;
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        if (data.readInt() != 0) {
                            _arg14 = (HwKeymasterBlob) HwKeymasterBlob.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg14 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg1 = (HwKeymasterBlob) HwKeymasterBlob.CREATOR.createFromParcel(parcel);
                        }
                        HwKeymasterArguments _arg22 = _arg1;
                        _arg3 = data.readInt();
                        HwKeyCharacteristics _arg42 = new HwKeyCharacteristics();
                        _result2 = getKeyCharacteristics(_arg0, _arg14, _arg22, _arg3, _arg42);
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        parcel2.writeInt(1);
                        _arg42.writeToParcel(parcel2, 1);
                        return true;
                    case 4:
                        HwKeymasterBlob _arg23;
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        _arg3 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg23 = (HwKeymasterBlob) HwKeymasterBlob.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg23 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg1 = (HwKeymasterBlob) HwKeymasterBlob.CREATOR.createFromParcel(parcel);
                        }
                        HwExportResult _result4 = exportKey(_arg0, _arg3, _arg23, _arg1, data.readInt());
                        reply.writeNoException();
                        if (_result4 != null) {
                            parcel2.writeInt(1);
                            _result4.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        IBinder _arg05 = data.readStrongBinder();
                        String _arg15 = data.readString();
                        int _arg24 = data.readInt();
                        boolean _arg33 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg1 = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        HwOperationResult _result5 = begin(_arg05, _arg15, _arg24, _arg33, _arg1, data.createByteArray(), data.readInt());
                        reply.writeNoException();
                        if (_result5 != null) {
                            parcel2.writeInt(1);
                            _result5.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg1 = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        HwOperationResult _result6 = update(_arg02, _arg1, data.createByteArray());
                        reply.writeNoException();
                        if (_result6 != null) {
                            parcel2.writeInt(1);
                            _result6.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg1 = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        HwOperationResult _result7 = finish(_arg02, _arg1, data.createByteArray(), data.createByteArray());
                        reply.writeNoException();
                        if (_result7 != null) {
                            parcel2.writeInt(1);
                            _result7.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        _result3 = abort(data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readString();
                        _result = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        HwKeymasterCertificateChain _arg34 = new HwKeymasterCertificateChain();
                        int _result8 = attestKey(_arg03, _result, _arg1, _arg34);
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        parcel2.writeInt(1);
                        _arg34.writeToParcel(parcel2, 1);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        HwExportResult _result9 = get(data.readString(), data.readInt());
                        reply.writeNoException();
                        if (_result9 != null) {
                            parcel2.writeInt(1);
                            _result9.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case TRANSACTION_set /*11*/:
                        HwKeymasterBlob _arg16;
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readString();
                        if (data.readInt() != 0) {
                            _arg16 = (HwKeymasterBlob) HwKeymasterBlob.CREATOR.createFromParcel(parcel);
                        }
                        int _result10 = set(_arg03, _arg16, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result10);
                        return true;
                    case TRANSACTION_getHuksServiceVersion /*12*/:
                        parcel.enforceInterface(descriptor);
                        String _result11 = getHuksServiceVersion();
                        reply.writeNoException();
                        parcel2.writeString(_result11);
                        return true;
                    case TRANSACTION_attestDeviceIds /*13*/:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        _arg12 = new HwKeymasterCertificateChain();
                        _result = attestDeviceIds(_arg1, _arg12);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        parcel2.writeInt(1);
                        _arg12.writeToParcel(parcel2, 1);
                        return true;
                    case TRANSACTION_assetHandleReq /*14*/:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        _arg12 = new HwKeymasterCertificateChain();
                        _result = assetHandleReq(_arg1, _arg12);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        parcel2.writeInt(1);
                        _arg12.writeToParcel(parcel2, 1);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        HwKeymasterCertificateChain _arg06 = new HwKeymasterCertificateChain();
                        _result3 = exportTrustCert(_arg06);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        parcel2.writeInt(1);
                        _arg06.writeToParcel(parcel2, 1);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = (HwKeymasterArguments) HwKeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        _result = setKeyProtection(_arg03, _arg1);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case TRANSACTION_contains /*17*/:
                        parcel.enforceInterface(descriptor);
                        _result3 = contains(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    int abort(IBinder iBinder) throws RemoteException;

    int assetHandleReq(HwKeymasterArguments hwKeymasterArguments, HwKeymasterCertificateChain hwKeymasterCertificateChain) throws RemoteException;

    int attestDeviceIds(HwKeymasterArguments hwKeymasterArguments, HwKeymasterCertificateChain hwKeymasterCertificateChain) throws RemoteException;

    int attestKey(String str, int i, HwKeymasterArguments hwKeymasterArguments, HwKeymasterCertificateChain hwKeymasterCertificateChain) throws RemoteException;

    HwOperationResult begin(IBinder iBinder, String str, int i, boolean z, HwKeymasterArguments hwKeymasterArguments, byte[] bArr, int i2) throws RemoteException;

    int contains(String str) throws RemoteException;

    int del(String str, int i) throws RemoteException;

    HwExportResult exportKey(String str, int i, HwKeymasterBlob hwKeymasterBlob, HwKeymasterBlob hwKeymasterBlob2, int i2) throws RemoteException;

    int exportTrustCert(HwKeymasterCertificateChain hwKeymasterCertificateChain) throws RemoteException;

    HwOperationResult finish(IBinder iBinder, HwKeymasterArguments hwKeymasterArguments, byte[] bArr, byte[] bArr2) throws RemoteException;

    int generateKey(String str, HwKeymasterArguments hwKeymasterArguments, byte[] bArr, int i, int i2, HwKeyCharacteristics hwKeyCharacteristics) throws RemoteException;

    HwExportResult get(String str, int i) throws RemoteException;

    String getHuksServiceVersion() throws RemoteException;

    int getKeyCharacteristics(String str, HwKeymasterBlob hwKeymasterBlob, HwKeymasterBlob hwKeymasterBlob2, int i, HwKeyCharacteristics hwKeyCharacteristics) throws RemoteException;

    int set(String str, HwKeymasterBlob hwKeymasterBlob, int i) throws RemoteException;

    int setKeyProtection(String str, HwKeymasterArguments hwKeymasterArguments) throws RemoteException;

    HwOperationResult update(IBinder iBinder, HwKeymasterArguments hwKeymasterArguments, byte[] bArr) throws RemoteException;
}
