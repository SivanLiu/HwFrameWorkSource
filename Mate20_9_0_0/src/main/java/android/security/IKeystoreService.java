package android.security;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.security.keymaster.ExportResult;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterBlob;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keymaster.OperationResult;

public interface IKeystoreService extends IInterface {

    public static abstract class Stub extends Binder implements IKeystoreService {
        private static final String DESCRIPTOR = "android.security.IKeystoreService";
        static final int TRANSACTION_abort = 30;
        static final int TRANSACTION_addAuthToken = 32;
        static final int TRANSACTION_addRngEntropy = 22;
        static final int TRANSACTION_attestDeviceIds = 36;
        static final int TRANSACTION_attestKey = 35;
        static final int TRANSACTION_begin = 27;
        static final int TRANSACTION_cancelConfirmationPrompt = 40;
        static final int TRANSACTION_clear_uid = 21;
        static final int TRANSACTION_del = 4;
        static final int TRANSACTION_exist = 5;
        static final int TRANSACTION_exportKey = 26;
        static final int TRANSACTION_finish = 29;
        static final int TRANSACTION_generate = 12;
        static final int TRANSACTION_generateKey = 23;
        static final int TRANSACTION_get = 2;
        static final int TRANSACTION_getKeyCharacteristics = 24;
        static final int TRANSACTION_getState = 1;
        static final int TRANSACTION_get_pubkey = 16;
        static final int TRANSACTION_getmtime = 19;
        static final int TRANSACTION_grant = 17;
        static final int TRANSACTION_importKey = 25;
        static final int TRANSACTION_importWrappedKey = 38;
        static final int TRANSACTION_import_key = 13;
        static final int TRANSACTION_insert = 3;
        static final int TRANSACTION_isConfirmationPromptSupported = 41;
        static final int TRANSACTION_isEmpty = 11;
        static final int TRANSACTION_isOperationAuthorized = 31;
        static final int TRANSACTION_is_hardware_backed = 20;
        static final int TRANSACTION_list = 6;
        static final int TRANSACTION_lock = 9;
        static final int TRANSACTION_onDeviceOffBody = 37;
        static final int TRANSACTION_onKeyguardVisibilityChanged = 42;
        static final int TRANSACTION_onUserAdded = 33;
        static final int TRANSACTION_onUserPasswordChanged = 8;
        static final int TRANSACTION_onUserRemoved = 34;
        static final int TRANSACTION_presentConfirmationPrompt = 39;
        static final int TRANSACTION_reset = 7;
        static final int TRANSACTION_sign = 14;
        static final int TRANSACTION_ungrant = 18;
        static final int TRANSACTION_unlock = 10;
        static final int TRANSACTION_update = 28;
        static final int TRANSACTION_verify = 15;

        private static class Proxy implements IKeystoreService {
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

            public int getState(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] get(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int insert(String name, byte[] item, int uid, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeByteArray(item);
                    _data.writeInt(uid);
                    _data.writeInt(flags);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int del(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
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

            public int exist(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] list(String namePrefix, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(namePrefix);
                    _data.writeInt(uid);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int reset() throws RemoteException {
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

            public int onUserPasswordChanged(int userId, String newPassword) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(newPassword);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int lock(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int unlock(int userId, String userPassword) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(userPassword);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int isEmpty(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int generate(String name, int uid, int keyType, int keySize, int flags, KeystoreArguments args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    _data.writeInt(keyType);
                    _data.writeInt(keySize);
                    _data.writeInt(flags);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int import_key(String name, byte[] data, int uid, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeByteArray(data);
                    _data.writeInt(uid);
                    _data.writeInt(flags);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] sign(String name, byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeByteArray(data);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int verify(String name, byte[] data, byte[] signature) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeByteArray(data);
                    _data.writeByteArray(signature);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] get_pubkey(String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String grant(String name, int granteeUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(granteeUid);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int ungrant(String name, int granteeUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(granteeUid);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getmtime(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int is_hardware_backed(String string) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(string);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int clear_uid(long uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(uid);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int addRngEntropy(byte[] data, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(data);
                    _data.writeInt(flags);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int generateKey(String alias, KeymasterArguments arguments, byte[] entropy, int uid, int flags, KeyCharacteristics characteristics) throws RemoteException {
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
                    this.mRemote.transact(23, _data, _reply, 0);
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

            public int getKeyCharacteristics(String alias, KeymasterBlob clientId, KeymasterBlob appData, int uid, KeyCharacteristics characteristics) throws RemoteException {
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
                    if (appData != null) {
                        _data.writeInt(1);
                        appData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    this.mRemote.transact(24, _data, _reply, 0);
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

            public int importKey(String alias, KeymasterArguments arguments, int format, byte[] keyData, int uid, int flags, KeyCharacteristics characteristics) throws RemoteException {
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
                    _data.writeInt(format);
                    _data.writeByteArray(keyData);
                    _data.writeInt(uid);
                    _data.writeInt(flags);
                    this.mRemote.transact(25, _data, _reply, 0);
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

            public ExportResult exportKey(String alias, int format, KeymasterBlob clientId, KeymasterBlob appData, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ExportResult _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(format);
                    if (clientId != null) {
                        _data.writeInt(1);
                        clientId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (appData != null) {
                        _data.writeInt(1);
                        appData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ExportResult) ExportResult.CREATOR.createFromParcel(_reply);
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

            public OperationResult begin(IBinder appToken, String alias, int purpose, boolean pruneable, KeymasterArguments params, byte[] entropy, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    OperationResult _result;
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
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (OperationResult) OperationResult.CREATOR.createFromParcel(_reply);
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

            public OperationResult update(IBinder token, KeymasterArguments params, byte[] input) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    OperationResult _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(input);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (OperationResult) OperationResult.CREATOR.createFromParcel(_reply);
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

            public OperationResult finish(IBinder token, KeymasterArguments params, byte[] signature, byte[] entropy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    OperationResult _result;
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
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (OperationResult) OperationResult.CREATOR.createFromParcel(_reply);
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
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isOperationAuthorized(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    boolean z = false;
                    this.mRemote.transact(31, _data, _reply, 0);
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

            public int addAuthToken(byte[] authToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(authToken);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int onUserAdded(int userId, int parentId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeInt(parentId);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int onUserRemoved(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int attestKey(String alias, KeymasterArguments params, KeymasterCertificateChain chain) throws RemoteException {
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
                    this.mRemote.transact(35, _data, _reply, 0);
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

            public int attestDeviceIds(KeymasterArguments params, KeymasterCertificateChain chain) throws RemoteException {
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
                    this.mRemote.transact(36, _data, _reply, 0);
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

            public int onDeviceOffBody() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int importWrappedKey(String wrappedKeyAlias, byte[] wrappedKey, String wrappingKeyAlias, byte[] maskingKey, KeymasterArguments arguments, long rootSid, long fingerprintSid, KeyCharacteristics characteristics) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(wrappedKeyAlias);
                    _data.writeByteArray(wrappedKey);
                    _data.writeString(wrappingKeyAlias);
                    _data.writeByteArray(maskingKey);
                    if (arguments != null) {
                        _data.writeInt(1);
                        arguments.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(rootSid);
                    _data.writeLong(fingerprintSid);
                    this.mRemote.transact(38, _data, _reply, 0);
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

            public int presentConfirmationPrompt(IBinder listener, String promptText, byte[] extraData, String locale, int uiOptionsAsFlags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener);
                    _data.writeString(promptText);
                    _data.writeByteArray(extraData);
                    _data.writeString(locale);
                    _data.writeInt(uiOptionsAsFlags);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int cancelConfirmationPrompt(IBinder listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isConfirmationPromptSupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(41, _data, _reply, 0);
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

            public int onKeyguardVisibilityChanged(boolean isShowing, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(isShowing);
                    _data.writeInt(userId);
                    this.mRemote.transact(42, _data, _reply, 0);
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

        public static IKeystoreService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IKeystoreService)) {
                return new Proxy(obj);
            }
            return (IKeystoreService) iin;
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
                boolean z2 = false;
                KeystoreArguments _arg1 = null;
                int _result;
                byte[] _result2;
                int _result3;
                int _result4;
                int _result5;
                String _arg0;
                int _arg2;
                int _arg3;
                int _arg4;
                int _result6;
                KeymasterArguments _arg12;
                KeymasterArguments _arg13;
                String _arg02;
                int _arg32;
                IBinder _arg03;
                switch (i) {
                    case 1:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = getState(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 2:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result2 = get(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeByteArray(_result2);
                        return z;
                    case 3:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result3 = insert(data.readString(), data.createByteArray(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return z;
                    case 4:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result4 = del(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return z;
                    case 5:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result4 = exist(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return z;
                    case 6:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        String[] _result7 = list(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeStringArray(_result7);
                        return z;
                    case 7:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result5 = reset();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return z;
                    case 8:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result4 = onUserPasswordChanged(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return z;
                    case 9:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = lock(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 10:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result4 = unlock(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return z;
                    case 11:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = isEmpty(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 12:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        int _arg14 = data.readInt();
                        _arg2 = data.readInt();
                        _arg3 = data.readInt();
                        _arg4 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (KeystoreArguments) KeystoreArguments.CREATOR.createFromParcel(parcel);
                        }
                        _result5 = generate(_arg0, _arg14, _arg2, _arg3, _arg4, _arg1);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return z;
                    case 13:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result3 = import_key(data.readString(), data.createByteArray(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return z;
                    case 14:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result2 = sign(data.readString(), data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeByteArray(_result2);
                        return z;
                    case 15:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result6 = verify(data.readString(), data.createByteArray(), data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return z;
                    case 16:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        byte[] _result8 = get_pubkey(data.readString());
                        reply.writeNoException();
                        parcel2.writeByteArray(_result8);
                        return z;
                    case 17:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        String _result9 = grant(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result9);
                        return z;
                    case 18:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result4 = ungrant(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return z;
                    case 19:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        long _result10 = getmtime(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeLong(_result10);
                        return z;
                    case 20:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = is_hardware_backed(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 21:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result4 = clear_uid(data.readLong());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return z;
                    case 22:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result4 = addRngEntropy(data.createByteArray(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return z;
                    case 23:
                        i = 1;
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        if (data.readInt() != 0) {
                            _arg12 = (KeymasterArguments) KeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        _arg13 = _arg12;
                        byte[] _arg22 = data.createByteArray();
                        _arg2 = data.readInt();
                        _arg3 = data.readInt();
                        KeyCharacteristics _arg5 = new KeyCharacteristics();
                        KeyCharacteristics _arg52 = _arg5;
                        _result5 = generateKey(_arg0, _arg13, _arg22, _arg2, _arg3, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        parcel2.writeInt(i);
                        _arg52.writeToParcel(parcel2, i);
                        return i;
                    case 24:
                        KeymasterBlob _arg15;
                        i = 1;
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readString();
                        if (data.readInt() != 0) {
                            _arg15 = (KeymasterBlob) KeymasterBlob.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg15 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg12 = (KeymasterBlob) KeymasterBlob.CREATOR.createFromParcel(parcel);
                        }
                        KeymasterArguments _arg23 = _arg12;
                        _arg32 = data.readInt();
                        KeyCharacteristics _arg42 = new KeyCharacteristics();
                        _result5 = getKeyCharacteristics(_arg02, _arg15, _arg23, _arg32, _arg42);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        parcel2.writeInt(i);
                        _arg42.writeToParcel(parcel2, i);
                        return i;
                    case 25:
                        i = 1;
                        parcel.enforceInterface(descriptor);
                        String _arg04 = data.readString();
                        if (data.readInt() != 0) {
                            _arg12 = (KeymasterArguments) KeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        _arg13 = _arg12;
                        _arg2 = data.readInt();
                        byte[] _arg33 = data.createByteArray();
                        _arg4 = data.readInt();
                        int _arg53 = data.readInt();
                        KeyCharacteristics _arg6 = new KeyCharacteristics();
                        KeyCharacteristics _arg62 = _arg6;
                        _result5 = importKey(_arg04, _arg13, _arg2, _arg33, _arg4, _arg53, _arg6);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        parcel2.writeInt(i);
                        _arg62.writeToParcel(parcel2, i);
                        return i;
                    case 26:
                        KeymasterBlob _arg24;
                        i = 1;
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readString();
                        _arg32 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg24 = (KeymasterBlob) KeymasterBlob.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg24 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg12 = (KeymasterBlob) KeymasterBlob.CREATOR.createFromParcel(parcel);
                        }
                        ExportResult _result11 = exportKey(_arg02, _arg32, _arg24, _arg12, data.readInt());
                        reply.writeNoException();
                        if (_result11 != null) {
                            parcel2.writeInt(i);
                            _result11.writeToParcel(parcel2, i);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return i;
                    case 27:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        IBinder _arg05 = data.readStrongBinder();
                        String _arg16 = data.readString();
                        _arg4 = data.readInt();
                        boolean _arg34 = data.readInt() != 0 ? z : false;
                        if (data.readInt() != 0) {
                            _arg12 = (KeymasterArguments) KeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        OperationResult _result12 = begin(_arg05, _arg16, _arg4, _arg34, _arg12, data.createByteArray(), data.readInt());
                        reply.writeNoException();
                        if (_result12 != null) {
                            parcel2.writeInt(z);
                            _result12.writeToParcel(parcel2, z);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return z;
                    case 28:
                        i = 1;
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg12 = (KeymasterArguments) KeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        OperationResult _result13 = update(_arg03, _arg12, data.createByteArray());
                        reply.writeNoException();
                        if (_result13 != null) {
                            parcel2.writeInt(i);
                            _result13.writeToParcel(parcel2, i);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return i;
                    case 29:
                        i = 1;
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg12 = (KeymasterArguments) KeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        OperationResult _result14 = finish(_arg03, _arg12, data.createByteArray(), data.createByteArray());
                        reply.writeNoException();
                        if (_result14 != null) {
                            parcel2.writeInt(i);
                            _result14.writeToParcel(parcel2, i);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return i;
                    case 30:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = abort(data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 31:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        boolean _result15 = isOperationAuthorized(data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeInt(_result15);
                        return z;
                    case 32:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = addAuthToken(data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 33:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result4 = onUserAdded(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return z;
                    case 34:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = onUserRemoved(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 35:
                        i = 1;
                        parcel.enforceInterface(descriptor);
                        String _arg06 = data.readString();
                        if (data.readInt() != 0) {
                            _arg12 = (KeymasterArguments) KeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        KeymasterCertificateChain _arg25 = new KeymasterCertificateChain();
                        _result6 = attestKey(_arg06, _arg12, _arg25);
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        parcel2.writeInt(i);
                        _arg25.writeToParcel(parcel2, i);
                        return i;
                    case 36:
                        i = 1;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg12 = (KeymasterArguments) KeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        KeymasterCertificateChain _arg17 = new KeymasterCertificateChain();
                        _result4 = attestDeviceIds(_arg12, _arg17);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        parcel2.writeInt(i);
                        _arg17.writeToParcel(parcel2, i);
                        return i;
                    case 37:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result5 = onDeviceOffBody();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return z;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        String _arg07 = data.readString();
                        byte[] _arg18 = data.createByteArray();
                        String _arg26 = data.readString();
                        byte[] _arg35 = data.createByteArray();
                        if (data.readInt() != 0) {
                            _arg1 = (KeymasterArguments) KeymasterArguments.CREATOR.createFromParcel(parcel);
                        }
                        KeystoreArguments _arg43 = _arg1;
                        long _arg54 = data.readLong();
                        long _arg63 = data.readLong();
                        KeyCharacteristics _arg7 = new KeyCharacteristics();
                        i = 1;
                        _result5 = importWrappedKey(_arg07, _arg18, _arg26, _arg35, _arg43, _arg54, _arg63, _arg7);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        parcel2.writeInt(i);
                        _arg7.writeToParcel(parcel2, i);
                        return i;
                    case 39:
                        parcel.enforceInterface(descriptor);
                        _result5 = presentConfirmationPrompt(data.readStrongBinder(), data.readString(), data.createByteArray(), data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        _result = cancelConfirmationPrompt(data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 41:
                        parcel.enforceInterface(descriptor);
                        boolean _result16 = isConfirmationPromptSupported();
                        reply.writeNoException();
                        parcel2.writeInt(_result16);
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            z2 = true;
                        }
                        _result4 = onKeyguardVisibilityChanged(z2, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            z = true;
            parcel2.writeString(descriptor);
            return z;
        }
    }

    int abort(IBinder iBinder) throws RemoteException;

    int addAuthToken(byte[] bArr) throws RemoteException;

    int addRngEntropy(byte[] bArr, int i) throws RemoteException;

    int attestDeviceIds(KeymasterArguments keymasterArguments, KeymasterCertificateChain keymasterCertificateChain) throws RemoteException;

    int attestKey(String str, KeymasterArguments keymasterArguments, KeymasterCertificateChain keymasterCertificateChain) throws RemoteException;

    OperationResult begin(IBinder iBinder, String str, int i, boolean z, KeymasterArguments keymasterArguments, byte[] bArr, int i2) throws RemoteException;

    int cancelConfirmationPrompt(IBinder iBinder) throws RemoteException;

    int clear_uid(long j) throws RemoteException;

    int del(String str, int i) throws RemoteException;

    int exist(String str, int i) throws RemoteException;

    ExportResult exportKey(String str, int i, KeymasterBlob keymasterBlob, KeymasterBlob keymasterBlob2, int i2) throws RemoteException;

    OperationResult finish(IBinder iBinder, KeymasterArguments keymasterArguments, byte[] bArr, byte[] bArr2) throws RemoteException;

    int generate(String str, int i, int i2, int i3, int i4, KeystoreArguments keystoreArguments) throws RemoteException;

    int generateKey(String str, KeymasterArguments keymasterArguments, byte[] bArr, int i, int i2, KeyCharacteristics keyCharacteristics) throws RemoteException;

    byte[] get(String str, int i) throws RemoteException;

    int getKeyCharacteristics(String str, KeymasterBlob keymasterBlob, KeymasterBlob keymasterBlob2, int i, KeyCharacteristics keyCharacteristics) throws RemoteException;

    int getState(int i) throws RemoteException;

    byte[] get_pubkey(String str) throws RemoteException;

    long getmtime(String str, int i) throws RemoteException;

    String grant(String str, int i) throws RemoteException;

    int importKey(String str, KeymasterArguments keymasterArguments, int i, byte[] bArr, int i2, int i3, KeyCharacteristics keyCharacteristics) throws RemoteException;

    int importWrappedKey(String str, byte[] bArr, String str2, byte[] bArr2, KeymasterArguments keymasterArguments, long j, long j2, KeyCharacteristics keyCharacteristics) throws RemoteException;

    int import_key(String str, byte[] bArr, int i, int i2) throws RemoteException;

    int insert(String str, byte[] bArr, int i, int i2) throws RemoteException;

    boolean isConfirmationPromptSupported() throws RemoteException;

    int isEmpty(int i) throws RemoteException;

    boolean isOperationAuthorized(IBinder iBinder) throws RemoteException;

    int is_hardware_backed(String str) throws RemoteException;

    String[] list(String str, int i) throws RemoteException;

    int lock(int i) throws RemoteException;

    int onDeviceOffBody() throws RemoteException;

    int onKeyguardVisibilityChanged(boolean z, int i) throws RemoteException;

    int onUserAdded(int i, int i2) throws RemoteException;

    int onUserPasswordChanged(int i, String str) throws RemoteException;

    int onUserRemoved(int i) throws RemoteException;

    int presentConfirmationPrompt(IBinder iBinder, String str, byte[] bArr, String str2, int i) throws RemoteException;

    int reset() throws RemoteException;

    byte[] sign(String str, byte[] bArr) throws RemoteException;

    int ungrant(String str, int i) throws RemoteException;

    int unlock(int i, String str) throws RemoteException;

    OperationResult update(IBinder iBinder, KeymasterArguments keymasterArguments, byte[] bArr) throws RemoteException;

    int verify(String str, byte[] bArr, byte[] bArr2) throws RemoteException;
}
