package huawei.android.security;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IHwEidPlugin extends IInterface {

    public static abstract class Stub extends Binder implements IHwEidPlugin {
        private static final String DESCRIPTOR = "huawei.android.security.IHwEidPlugin";
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
            int _result;
            int _arg0;
            int _arg1;
            byte[] _arg2;
            int _arg3;
            int _arg4_length;
            byte[] bArr;
            int _arg5_length;
            int[] iArr;
            int _arg6_length;
            byte[] bArr2;
            int _arg7_length;
            int[] iArr2;
            byte[] _arg02;
            int _arg0_length;
            int _arg1_length;
            int[] iArr3;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    _result = eid_init(data.createByteArray(), data.readInt(), data.createByteArray(), data.readInt(), data.createByteArray(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    _result = eid_finish();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readInt();
                    _arg1 = data.readInt();
                    _arg2 = data.createByteArray();
                    _arg3 = data.readInt();
                    _arg4_length = data.readInt();
                    if (_arg4_length < 0) {
                        bArr = null;
                    } else {
                        bArr = new byte[_arg4_length];
                    }
                    _arg5_length = data.readInt();
                    if (_arg5_length < 0) {
                        iArr = null;
                    } else {
                        iArr = new int[_arg5_length];
                    }
                    _arg6_length = data.readInt();
                    if (_arg6_length < 0) {
                        bArr2 = null;
                    } else {
                        bArr2 = new byte[_arg6_length];
                    }
                    _arg7_length = data.readInt();
                    if (_arg7_length < 0) {
                        iArr2 = null;
                    } else {
                        iArr2 = new int[_arg7_length];
                    }
                    _result = eid_get_image(_arg0, _arg1, _arg2, _arg3, bArr, iArr, bArr2, iArr2);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    reply.writeByteArray(bArr);
                    reply.writeIntArray(iArr);
                    reply.writeByteArray(bArr2);
                    reply.writeIntArray(iArr2);
                    return true;
                case 4:
                    byte[] bArr3;
                    int[] iArr4;
                    data.enforceInterface(DESCRIPTOR);
                    _arg02 = data.createByteArray();
                    _arg1 = data.readInt();
                    int _arg22 = data.readInt();
                    _arg3 = data.readInt();
                    bArr = data.createByteArray();
                    int _arg5 = data.readInt();
                    _arg6_length = data.readInt();
                    if (_arg6_length < 0) {
                        bArr2 = null;
                    } else {
                        bArr2 = new byte[_arg6_length];
                    }
                    _arg7_length = data.readInt();
                    if (_arg7_length < 0) {
                        iArr2 = null;
                    } else {
                        iArr2 = new int[_arg7_length];
                    }
                    int _arg8_length = data.readInt();
                    if (_arg8_length < 0) {
                        bArr3 = null;
                    } else {
                        bArr3 = new byte[_arg8_length];
                    }
                    int _arg9_length = data.readInt();
                    if (_arg9_length < 0) {
                        iArr4 = null;
                    } else {
                        iArr4 = new int[_arg9_length];
                    }
                    _result = eid_get_unsec_image(_arg02, _arg1, _arg22, _arg3, bArr, _arg5, bArr2, iArr2, bArr3, iArr4);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    reply.writeByteArray(bArr2);
                    reply.writeIntArray(iArr2);
                    reply.writeByteArray(bArr3);
                    reply.writeIntArray(iArr4);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0_length = data.readInt();
                    if (_arg0_length < 0) {
                        _arg02 = null;
                    } else {
                        _arg02 = new byte[_arg0_length];
                    }
                    _arg1_length = data.readInt();
                    if (_arg1_length < 0) {
                        iArr3 = null;
                    } else {
                        iArr3 = new int[_arg1_length];
                    }
                    _result = eid_get_certificate_request_message(_arg02, iArr3);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    reply.writeByteArray(_arg02);
                    reply.writeIntArray(iArr3);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readInt();
                    _arg1 = data.readInt();
                    _arg2 = data.createByteArray();
                    _arg3 = data.readInt();
                    _arg4_length = data.readInt();
                    if (_arg4_length < 0) {
                        bArr = null;
                    } else {
                        bArr = new byte[_arg4_length];
                    }
                    _arg5_length = data.readInt();
                    if (_arg5_length < 0) {
                        iArr = null;
                    } else {
                        iArr = new int[_arg5_length];
                    }
                    _result = eid_sign_info(_arg0, _arg1, _arg2, _arg3, bArr, iArr);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    reply.writeByteArray(bArr);
                    reply.writeIntArray(iArr);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0_length = data.readInt();
                    if (_arg0_length < 0) {
                        _arg02 = null;
                    } else {
                        _arg02 = new byte[_arg0_length];
                    }
                    _arg1_length = data.readInt();
                    if (_arg1_length < 0) {
                        iArr3 = null;
                    } else {
                        iArr3 = new int[_arg1_length];
                    }
                    _result = eid_get_identity_information(_arg02, iArr3);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    reply.writeByteArray(_arg02);
                    reply.writeIntArray(iArr3);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    _result = eid_get_face_is_changed(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _result2 = eid_get_version();
                    reply.writeNoException();
                    reply.writeString(_result2);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

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
