package huawei.android.security;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IHwEidPlugin extends IInterface {

    public static abstract class Stub extends Binder implements IHwEidPlugin {
        private static final String DESCRIPTOR = "huawei.android.security.IHwEidPlugin";
        static final int TRANSACTION_ctid_get_sec_image = 11;
        static final int TRANSACTION_ctid_get_service_verion_info = 12;
        static final int TRANSACTION_ctid_set_sec_mode = 10;
        static final int TRANSACTION_eidGetSecImageZip = 13;
        static final int TRANSACTION_eidGetUnsecImageZip = 14;
        static final int TRANSACTION_eid_finish = 2;
        static final int TRANSACTION_eid_get_certificate_request_message = 5;
        static final int TRANSACTION_eid_get_face_is_changed = 8;
        static final int TRANSACTION_eid_get_identity_information = 7;
        static final int TRANSACTION_eid_get_image = 3;
        static final int TRANSACTION_eid_get_unsec_image = 4;
        static final int TRANSACTION_eid_get_version = 9;
        static final int TRANSACTION_eid_init = 1;
        static final int TRANSACTION_eid_sign_info = 6;

        private static class Proxy implements IHwEidPlugin {
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

            public int eid_init(byte[] hw_aid, int hw_aid_len, byte[] eid_aid, int eid_aid_len, byte[] logo, int logo_size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(hw_aid);
                    _data.writeInt(hw_aid_len);
                    _data.writeByteArray(eid_aid);
                    _data.writeInt(eid_aid_len);
                    _data.writeByteArray(logo);
                    _data.writeInt(logo_size);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int eid_finish() throws RemoteException {
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

            public int eid_get_image(int transpotCounter, int encryption_method, byte[] certificate, int certificate_len, byte[] image, int[] image_len, byte[] de_skey, int[] de_skey_len) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transpotCounter);
                    _data.writeInt(encryption_method);
                    _data.writeByteArray(certificate);
                    _data.writeInt(certificate_len);
                    if (image == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(image.length);
                    }
                    if (image_len == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(image_len.length);
                    }
                    if (de_skey == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(de_skey.length);
                    }
                    if (de_skey_len == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(de_skey_len.length);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readByteArray(image);
                    _reply.readIntArray(image_len);
                    _reply.readByteArray(de_skey);
                    _reply.readIntArray(de_skey_len);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int eid_get_unsec_image(byte[] src_image, int src_image_len, int transpotCounter, int encryption_method, byte[] certificate, int certificate_len, byte[] image, int[] image_len, byte[] de_skey, int[] de_skey_len) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(src_image);
                    _data.writeInt(src_image_len);
                    _data.writeInt(transpotCounter);
                    _data.writeInt(encryption_method);
                    _data.writeByteArray(certificate);
                    _data.writeInt(certificate_len);
                    if (image == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(image.length);
                    }
                    if (image_len == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(image_len.length);
                    }
                    if (de_skey == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(de_skey.length);
                    }
                    if (de_skey_len == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(de_skey_len.length);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readByteArray(image);
                    _reply.readIntArray(image_len);
                    _reply.readByteArray(de_skey);
                    _reply.readIntArray(de_skey_len);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int eid_get_certificate_request_message(byte[] request_message, int[] message_len) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request_message == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(request_message.length);
                    }
                    if (message_len == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(message_len.length);
                    }
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readByteArray(request_message);
                    _reply.readIntArray(message_len);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int eid_sign_info(int transpotCounter, int encryption_method, byte[] info, int info_len, byte[] sign, int[] sign_len) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transpotCounter);
                    _data.writeInt(encryption_method);
                    _data.writeByteArray(info);
                    _data.writeInt(info_len);
                    if (sign == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(sign.length);
                    }
                    if (sign_len == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(sign_len.length);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readByteArray(sign);
                    _reply.readIntArray(sign_len);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int eid_get_identity_information(byte[] identity_info, int[] identity_info_len) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (identity_info == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(identity_info.length);
                    }
                    if (identity_info_len == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(identity_info_len.length);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readByteArray(identity_info);
                    _reply.readIntArray(identity_info_len);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int eid_get_face_is_changed(int cmd_id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cmd_id);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String eid_get_version() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int ctid_set_sec_mode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int ctid_get_sec_image() throws RemoteException {
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

            public int ctid_get_service_verion_info(byte[] uuid, int uuid_len, String ta_path, int[] cmd_list, int cmd_count) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(uuid);
                    _data.writeInt(uuid_len);
                    _data.writeString(ta_path);
                    _data.writeIntArray(cmd_list);
                    _data.writeInt(cmd_count);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int eidGetSecImageZip(int hash_len, byte[] hash, int image_zip_len, byte[] image_zip, int up, int down, int left, int right, int encryption_method, int certificate_len, byte[] certificate, int[] sec_image_len, byte[] sec_image, int[] de_skey_len, byte[] de_skey) throws RemoteException {
                Throwable th;
                int i;
                int i2;
                int i3;
                int i4;
                int i5;
                byte[] bArr;
                int[] iArr = sec_image_len;
                byte[] bArr2 = sec_image;
                int[] iArr2 = de_skey_len;
                byte[] bArr3 = de_skey;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(hash_len);
                    _data.writeByteArray(hash);
                    try {
                        _data.writeInt(image_zip_len);
                        try {
                            _data.writeByteArray(image_zip);
                            try {
                                _data.writeInt(up);
                            } catch (Throwable th2) {
                                th = th2;
                                i = down;
                                i2 = left;
                                i3 = right;
                                i4 = encryption_method;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            i5 = up;
                            i = down;
                            i2 = left;
                            i3 = right;
                            i4 = encryption_method;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        bArr = image_zip;
                        i5 = up;
                        i = down;
                        i2 = left;
                        i3 = right;
                        i4 = encryption_method;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(down);
                        try {
                            _data.writeInt(left);
                            try {
                                _data.writeInt(right);
                            } catch (Throwable th5) {
                                th = th5;
                                i4 = encryption_method;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            i3 = right;
                            i4 = encryption_method;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        i2 = left;
                        i3 = right;
                        i4 = encryption_method;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(encryption_method);
                        _data.writeInt(certificate_len);
                        _data.writeByteArray(certificate);
                        if (iArr == null) {
                            _data.writeInt(-1);
                        } else {
                            _data.writeInt(iArr.length);
                        }
                        if (bArr2 == null) {
                            _data.writeInt(-1);
                        } else {
                            _data.writeInt(bArr2.length);
                        }
                        if (iArr2 == null) {
                            _data.writeInt(-1);
                        } else {
                            _data.writeInt(iArr2.length);
                        }
                        if (bArr3 == null) {
                            _data.writeInt(-1);
                        } else {
                            _data.writeInt(bArr3.length);
                        }
                        this.mRemote.transact(13, _data, _reply, 0);
                        _reply.readException();
                        int _result = _reply.readInt();
                        _reply.readIntArray(iArr);
                        _reply.readByteArray(bArr2);
                        _reply.readIntArray(iArr2);
                        _reply.readByteArray(bArr3);
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
                    int i6 = image_zip_len;
                    bArr = image_zip;
                    i5 = up;
                    i = down;
                    i2 = left;
                    i3 = right;
                    i4 = encryption_method;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public int eidGetUnsecImageZip(int hash_len, byte[] hash, int image_zip_len, byte[] image_zip, int encryption_method, int certificate_len, byte[] certificate, int[] sec_image_len, byte[] sec_image, int[] de_skey_len, byte[] de_skey) throws RemoteException {
                Throwable th;
                int i;
                byte[] bArr;
                int i2;
                int i3;
                byte[] bArr2;
                int[] iArr = sec_image_len;
                byte[] bArr3 = sec_image;
                int[] iArr2 = de_skey_len;
                byte[] bArr4 = de_skey;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(hash_len);
                    try {
                        _data.writeByteArray(hash);
                    } catch (Throwable th2) {
                        th = th2;
                        i = image_zip_len;
                        bArr = image_zip;
                        i2 = encryption_method;
                        i3 = certificate_len;
                        bArr2 = certificate;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(image_zip_len);
                        try {
                            _data.writeByteArray(image_zip);
                            try {
                                _data.writeInt(encryption_method);
                            } catch (Throwable th3) {
                                th = th3;
                                i3 = certificate_len;
                                bArr2 = certificate;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            i2 = encryption_method;
                            i3 = certificate_len;
                            bArr2 = certificate;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        bArr = image_zip;
                        i2 = encryption_method;
                        i3 = certificate_len;
                        bArr2 = certificate;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(certificate_len);
                        try {
                            _data.writeByteArray(certificate);
                            if (iArr == null) {
                                _data.writeInt(-1);
                            } else {
                                _data.writeInt(iArr.length);
                            }
                            if (bArr3 == null) {
                                _data.writeInt(-1);
                            } else {
                                _data.writeInt(bArr3.length);
                            }
                            if (iArr2 == null) {
                                _data.writeInt(-1);
                            } else {
                                _data.writeInt(iArr2.length);
                            }
                            if (bArr4 == null) {
                                _data.writeInt(-1);
                            } else {
                                _data.writeInt(bArr4.length);
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            this.mRemote.transact(14, _data, _reply, 0);
                            _reply.readException();
                            int _result = _reply.readInt();
                            _reply.readIntArray(iArr);
                            _reply.readByteArray(bArr3);
                            _reply.readIntArray(iArr2);
                            _reply.readByteArray(bArr4);
                            _reply.recycle();
                            _data.recycle();
                            return _result;
                        } catch (Throwable th7) {
                            th = th7;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        bArr2 = certificate;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th9) {
                    th = th9;
                    byte[] bArr5 = hash;
                    i = image_zip_len;
                    bArr = image_zip;
                    i2 = encryption_method;
                    i3 = certificate_len;
                    bArr2 = certificate;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IHwEidPlugin asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IHwEidPlugin)) {
                return new Proxy(obj);
            }
            return (IHwEidPlugin) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            Parcel parcel3;
            String descriptor2;
            Parcel parcel4;
            if (i != 1598968902) {
                int _result;
                int _arg1;
                byte[] _arg2;
                int _arg3;
                int _arg4_length;
                byte[] _arg4;
                byte[] _arg42;
                int _arg5_length;
                int[] _arg5;
                int[] _arg52;
                int _arg6_length;
                int _arg7_length;
                byte[] _arg43;
                int _arg32;
                byte[] _arg44;
                int _arg53;
                int[] _arg7;
                byte[] _arg0;
                int[] _arg12;
                byte[] _arg13;
                int _arg54;
                switch (i) {
                    case 1:
                        parcel3 = parcel;
                        descriptor2 = descriptor;
                        parcel4 = parcel2;
                        parcel3.enforceInterface(descriptor2);
                        _result = eid_init(data.createByteArray(), data.readInt(), data.createByteArray(), data.readInt(), data.createByteArray(), data.readInt());
                        reply.writeNoException();
                        parcel4.writeInt(_result);
                        return true;
                    case 2:
                        parcel3 = parcel;
                        descriptor2 = descriptor;
                        parcel4 = parcel2;
                        parcel3.enforceInterface(descriptor2);
                        _result = eid_finish();
                        reply.writeNoException();
                        parcel4.writeInt(_result);
                        return true;
                    case 3:
                        parcel3 = parcel;
                        descriptor2 = descriptor;
                        parcel4 = parcel2;
                        parcel3.enforceInterface(descriptor2);
                        int _arg02 = data.readInt();
                        _arg1 = data.readInt();
                        _arg2 = data.createByteArray();
                        _arg3 = data.readInt();
                        _arg4_length = data.readInt();
                        if (_arg4_length < 0) {
                            _arg4 = null;
                        } else {
                            _arg4 = new byte[_arg4_length];
                        }
                        _arg42 = _arg4;
                        _arg5_length = data.readInt();
                        if (_arg5_length < 0) {
                            _arg5 = null;
                        } else {
                            _arg5 = new int[_arg5_length];
                        }
                        _arg52 = _arg5;
                        _arg6_length = data.readInt();
                        if (_arg6_length < 0) {
                            _arg4 = null;
                        } else {
                            _arg4 = new byte[_arg6_length];
                        }
                        byte[] _arg6 = _arg4;
                        _arg7_length = data.readInt();
                        if (_arg7_length < 0) {
                            _arg5 = null;
                        } else {
                            _arg5 = new int[_arg7_length];
                        }
                        int[] _arg72 = _arg5;
                        byte[] _arg62 = _arg6;
                        int[] _arg55 = _arg52;
                        _arg43 = _arg42;
                        _result = eid_get_image(_arg02, _arg1, _arg2, _arg3, _arg42, _arg55, _arg62, _arg72);
                        reply.writeNoException();
                        parcel4.writeInt(_result);
                        parcel4.writeByteArray(_arg43);
                        parcel4.writeIntArray(_arg55);
                        parcel4.writeByteArray(_arg62);
                        parcel4.writeIntArray(_arg72);
                        return true;
                    case 4:
                        parcel3 = parcel;
                        descriptor2 = descriptor;
                        parcel4 = parcel2;
                        parcel3.enforceInterface(descriptor2);
                        _arg2 = data.createByteArray();
                        _arg3 = data.readInt();
                        int _arg22 = data.readInt();
                        _arg32 = data.readInt();
                        _arg44 = data.createByteArray();
                        _arg53 = data.readInt();
                        _arg1 = data.readInt();
                        if (_arg1 < 0) {
                            _arg4 = null;
                        } else {
                            _arg4 = new byte[_arg1];
                        }
                        _arg43 = _arg4;
                        _arg4_length = data.readInt();
                        if (_arg4_length < 0) {
                            _arg5 = null;
                        } else {
                            _arg5 = new int[_arg4_length];
                        }
                        _arg7 = _arg5;
                        _arg5_length = data.readInt();
                        if (_arg5_length < 0) {
                            _arg4 = null;
                        } else {
                            _arg4 = new byte[_arg5_length];
                        }
                        byte[] _arg8 = _arg4;
                        _arg6_length = data.readInt();
                        if (_arg6_length < 0) {
                            _arg5 = null;
                        } else {
                            _arg5 = new int[_arg6_length];
                        }
                        int[] _arg9 = _arg5;
                        byte[] _arg82 = _arg8;
                        int[] _arg73 = _arg7;
                        _arg2 = _arg43;
                        _result = eid_get_unsec_image(_arg2, _arg3, _arg22, _arg32, _arg44, _arg53, _arg43, _arg73, _arg82, _arg9);
                        reply.writeNoException();
                        parcel4.writeInt(_result);
                        parcel4.writeByteArray(_arg2);
                        parcel4.writeIntArray(_arg73);
                        parcel4.writeByteArray(_arg82);
                        parcel4.writeIntArray(_arg9);
                        return true;
                    case 5:
                        parcel3 = parcel;
                        descriptor2 = descriptor;
                        parcel4 = parcel2;
                        parcel3.enforceInterface(descriptor2);
                        _result = data.readInt();
                        if (_result < 0) {
                            _arg0 = null;
                        } else {
                            _arg0 = new byte[_result];
                        }
                        _arg7_length = data.readInt();
                        if (_arg7_length < 0) {
                            _arg12 = null;
                        } else {
                            _arg12 = new int[_arg7_length];
                        }
                        _arg6_length = eid_get_certificate_request_message(_arg0, _arg12);
                        reply.writeNoException();
                        parcel4.writeInt(_arg6_length);
                        parcel4.writeByteArray(_arg0);
                        parcel4.writeIntArray(_arg12);
                        return true;
                    case 6:
                        Object _arg56;
                        parcel3 = parcel;
                        descriptor2 = descriptor;
                        parcel4 = parcel2;
                        parcel3.enforceInterface(descriptor2);
                        int _arg03 = data.readInt();
                        _arg4_length = data.readInt();
                        _arg43 = data.createByteArray();
                        _arg1 = data.readInt();
                        int _arg4_length2 = data.readInt();
                        if (_arg4_length2 < 0) {
                            _arg4 = null;
                        } else {
                            _arg4 = new byte[_arg4_length2];
                        }
                        byte[] _arg45 = _arg4;
                        int _arg5_length2 = data.readInt();
                        if (_arg5_length2 < 0) {
                            _arg56 = null;
                        } else {
                            _arg56 = new int[_arg5_length2];
                        }
                        Object _arg57 = _arg56;
                        _arg42 = _arg45;
                        _result = eid_sign_info(_arg03, _arg4_length, _arg43, _arg1, _arg45, _arg57);
                        reply.writeNoException();
                        parcel4.writeInt(_result);
                        parcel4.writeByteArray(_arg42);
                        parcel4.writeIntArray(_arg57);
                        return true;
                    case 7:
                        parcel3 = parcel;
                        descriptor2 = descriptor;
                        parcel4 = parcel2;
                        parcel3.enforceInterface(descriptor2);
                        _result = data.readInt();
                        if (_result < 0) {
                            _arg0 = null;
                        } else {
                            _arg0 = new byte[_result];
                        }
                        _arg7_length = data.readInt();
                        if (_arg7_length < 0) {
                            _arg12 = null;
                        } else {
                            _arg12 = new int[_arg7_length];
                        }
                        _arg6_length = eid_get_identity_information(_arg0, _arg12);
                        reply.writeNoException();
                        parcel4.writeInt(_arg6_length);
                        parcel4.writeByteArray(_arg0);
                        parcel4.writeIntArray(_arg12);
                        return true;
                    case 8:
                        parcel3 = parcel;
                        descriptor2 = descriptor;
                        parcel4 = parcel2;
                        parcel3.enforceInterface(descriptor2);
                        int _result2 = eid_get_face_is_changed(data.readInt());
                        reply.writeNoException();
                        parcel4.writeInt(_result2);
                        return true;
                    case 9:
                        parcel3 = parcel;
                        descriptor2 = descriptor;
                        parcel4 = parcel2;
                        parcel3.enforceInterface(descriptor2);
                        String _result3 = eid_get_version();
                        reply.writeNoException();
                        parcel4.writeString(_result3);
                        return true;
                    case 10:
                        parcel3 = parcel;
                        descriptor2 = descriptor;
                        parcel4 = parcel2;
                        parcel3.enforceInterface(descriptor2);
                        _result = ctid_set_sec_mode();
                        reply.writeNoException();
                        parcel4.writeInt(_result);
                        return true;
                    case 11:
                        parcel3 = parcel;
                        descriptor2 = descriptor;
                        parcel4 = parcel2;
                        parcel3.enforceInterface(descriptor2);
                        _result = ctid_get_sec_image();
                        reply.writeNoException();
                        parcel4.writeInt(_result);
                        return true;
                    case 12:
                        String descriptor3 = descriptor;
                        parcel4 = parcel2;
                        data.enforceInterface(descriptor3);
                        _result = ctid_get_service_verion_info(data.createByteArray(), data.readInt(), data.readString(), data.createIntArray(), data.readInt());
                        reply.writeNoException();
                        parcel4.writeInt(_result);
                        return true;
                    case 13:
                        String descriptor4 = descriptor;
                        parcel.enforceInterface(descriptor4);
                        _arg3 = data.readInt();
                        _arg13 = data.createByteArray();
                        _arg32 = data.readInt();
                        _arg44 = data.createByteArray();
                        _arg53 = data.readInt();
                        _arg54 = data.readInt();
                        int _arg63 = data.readInt();
                        int _arg74 = data.readInt();
                        int _arg83 = data.readInt();
                        int _arg92 = data.readInt();
                        byte[] _arg10 = data.createByteArray();
                        i = data.readInt();
                        if (i < 0) {
                            _arg5 = null;
                        } else {
                            _arg5 = new int[i];
                        }
                        descriptor = _arg5;
                        _arg1 = data.readInt();
                        if (_arg1 < 0) {
                            _arg4 = null;
                        } else {
                            _arg4 = new byte[_arg1];
                        }
                        _arg43 = _arg4;
                        _arg4_length = data.readInt();
                        if (_arg4_length < 0) {
                            _arg5 = null;
                        } else {
                            _arg5 = new int[_arg4_length];
                        }
                        _arg7 = _arg5;
                        _arg5_length = data.readInt();
                        if (_arg5_length < 0) {
                            _arg4 = null;
                        } else {
                            _arg4 = new byte[_arg5_length];
                        }
                        byte[] _arg14 = _arg4;
                        int[] _arg132 = _arg7;
                        byte[] _arg122 = _arg43;
                        int[] _arg11 = descriptor;
                        _result = eidGetSecImageZip(_arg3, _arg13, _arg32, _arg44, _arg53, _arg54, _arg63, _arg74, _arg83, _arg92, _arg10, _arg11, _arg122, _arg132, _arg14);
                        reply.writeNoException();
                        parcel4 = reply;
                        parcel4.writeInt(_result);
                        parcel4.writeIntArray(_arg11);
                        parcel4.writeByteArray(_arg122);
                        parcel4.writeIntArray(_arg132);
                        parcel4.writeByteArray(_arg14);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _arg3 = data.readInt();
                        _arg13 = data.createByteArray();
                        _arg32 = data.readInt();
                        _arg44 = data.createByteArray();
                        _arg53 = data.readInt();
                        _arg54 = data.readInt();
                        byte[] _arg64 = data.createByteArray();
                        _arg1 = data.readInt();
                        if (_arg1 < 0) {
                            _arg5 = null;
                        } else {
                            _arg5 = new int[_arg1];
                        }
                        int[] _arg75 = _arg5;
                        _arg4_length = data.readInt();
                        if (_arg4_length < 0) {
                            _arg4 = null;
                        } else {
                            _arg4 = new byte[_arg4_length];
                        }
                        _arg42 = _arg4;
                        _arg5_length = data.readInt();
                        if (_arg5_length < 0) {
                            _arg5 = null;
                        } else {
                            _arg5 = new int[_arg5_length];
                        }
                        _arg52 = _arg5;
                        _arg6_length = data.readInt();
                        if (_arg6_length < 0) {
                            _arg4 = null;
                        } else {
                            _arg4 = new byte[_arg6_length];
                        }
                        byte[] _arg102 = _arg4;
                        int[] _arg93 = _arg52;
                        byte[] _arg84 = _arg42;
                        int[] _arg76 = _arg75;
                        _result = eidGetUnsecImageZip(_arg3, _arg13, _arg32, _arg44, _arg53, _arg54, _arg64, _arg75, _arg84, _arg93, _arg102);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        parcel2.writeIntArray(_arg76);
                        parcel2.writeByteArray(_arg84);
                        parcel2.writeIntArray(_arg93);
                        parcel2.writeByteArray(_arg102);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel3 = parcel;
            descriptor2 = descriptor;
            parcel4 = parcel2;
            parcel2 = parcel3;
            parcel4.writeString(descriptor2);
            return true;
        }
    }

    int ctid_get_sec_image() throws RemoteException;

    int ctid_get_service_verion_info(byte[] bArr, int i, String str, int[] iArr, int i2) throws RemoteException;

    int ctid_set_sec_mode() throws RemoteException;

    int eidGetSecImageZip(int i, byte[] bArr, int i2, byte[] bArr2, int i3, int i4, int i5, int i6, int i7, int i8, byte[] bArr3, int[] iArr, byte[] bArr4, int[] iArr2, byte[] bArr5) throws RemoteException;

    int eidGetUnsecImageZip(int i, byte[] bArr, int i2, byte[] bArr2, int i3, int i4, byte[] bArr3, int[] iArr, byte[] bArr4, int[] iArr2, byte[] bArr5) throws RemoteException;

    int eid_finish() throws RemoteException;

    int eid_get_certificate_request_message(byte[] bArr, int[] iArr) throws RemoteException;

    int eid_get_face_is_changed(int i) throws RemoteException;

    int eid_get_identity_information(byte[] bArr, int[] iArr) throws RemoteException;

    int eid_get_image(int i, int i2, byte[] bArr, int i3, byte[] bArr2, int[] iArr, byte[] bArr3, int[] iArr2) throws RemoteException;

    int eid_get_unsec_image(byte[] bArr, int i, int i2, int i3, byte[] bArr2, int i4, byte[] bArr3, int[] iArr, byte[] bArr4, int[] iArr2) throws RemoteException;

    String eid_get_version() throws RemoteException;

    int eid_init(byte[] bArr, int i, byte[] bArr2, int i2, byte[] bArr3, int i3) throws RemoteException;

    int eid_sign_info(int i, int i2, byte[] bArr, int i3, byte[] bArr2, int[] iArr) throws RemoteException;
}
