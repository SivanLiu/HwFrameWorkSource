package com.huawei.servicehost;

import android.hardware.camera2.impl.CameraMetadataNative;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IImageProcessService extends IInterface {

    public static abstract class Stub extends Binder implements IImageProcessService {
        private static final String DESCRIPTOR = "com.huawei.servicehost.IImageProcessService";
        static final int TRANSACTION_createIPSession = 1;
        static final int TRANSACTION_dualCameraMode = 5;
        static final int TRANSACTION_getGlobalSession = 2;
        static final int TRANSACTION_getSupportedMode = 3;
        static final int TRANSACTION_queryCapability = 4;

        private static class Proxy implements IImageProcessService {
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

            public IImageProcessSession createIPSession(String type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(type);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    IImageProcessSession _result = com.huawei.servicehost.IImageProcessSession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IGlobalSession getGlobalSession() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    IGlobalSession _result = com.huawei.servicehost.IGlobalSession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSupportedMode() throws RemoteException {
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

            public void queryCapability(String cameraId, CameraMetadataNative nativeMeta) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(cameraId);
                    if (nativeMeta != null) {
                        _data.writeInt(1);
                        nativeMeta.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        nativeMeta.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int dualCameraMode() throws RemoteException {
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
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImageProcessService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IImageProcessService)) {
                return new Proxy(obj);
            }
            return (IImageProcessService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                IBinder _arg1 = null;
                int _result;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        IImageProcessSession _result2 = createIPSession(data.readString());
                        reply.writeNoException();
                        if (_result2 != null) {
                            _arg1 = _result2.asBinder();
                        }
                        reply.writeStrongBinder(_arg1);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        IGlobalSession _result3 = getGlobalSession();
                        reply.writeNoException();
                        if (_result3 != null) {
                            _arg1 = _result3.asBinder();
                        }
                        reply.writeStrongBinder(_arg1);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        _result = getSupportedMode();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 4:
                        CameraMetadataNative _arg12;
                        data.enforceInterface(descriptor);
                        String _arg0 = data.readString();
                        if (data.readInt() != 0) {
                            _arg12 = (CameraMetadataNative) CameraMetadataNative.CREATOR.createFromParcel(data);
                        }
                        queryCapability(_arg0, _arg12);
                        reply.writeNoException();
                        if (_arg12 != null) {
                            reply.writeInt(1);
                            _arg12.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        _result = dualCameraMode();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    IImageProcessSession createIPSession(String str) throws RemoteException;

    int dualCameraMode() throws RemoteException;

    IGlobalSession getGlobalSession() throws RemoteException;

    int getSupportedMode() throws RemoteException;

    void queryCapability(String str, CameraMetadataNative cameraMetadataNative) throws RemoteException;
}
