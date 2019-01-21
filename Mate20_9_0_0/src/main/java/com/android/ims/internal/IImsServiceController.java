package com.android.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IImsServiceController extends IInterface {

    public static abstract class Stub extends Binder implements IImsServiceController {
        private static final String DESCRIPTOR = "com.android.ims.internal.IImsServiceController";
        static final int TRANSACTION_createEmergencyMMTelFeature = 1;
        static final int TRANSACTION_createMMTelFeature = 2;
        static final int TRANSACTION_createRcsFeature = 3;
        static final int TRANSACTION_removeImsFeature = 4;

        private static class Proxy implements IImsServiceController {
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

            public IImsMMTelFeature createEmergencyMMTelFeature(int slotId, IImsFeatureStatusCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeStrongBinder(c != null ? c.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    IImsMMTelFeature _result = com.android.ims.internal.IImsMMTelFeature.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IImsMMTelFeature createMMTelFeature(int slotId, IImsFeatureStatusCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeStrongBinder(c != null ? c.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    IImsMMTelFeature _result = com.android.ims.internal.IImsMMTelFeature.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IImsRcsFeature createRcsFeature(int slotId, IImsFeatureStatusCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeStrongBinder(c != null ? c.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    IImsRcsFeature _result = com.android.ims.internal.IImsRcsFeature.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeImsFeature(int slotId, int featureType, IImsFeatureStatusCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeInt(featureType);
                    _data.writeStrongBinder(c != null ? c.asBinder() : null);
                    this.mRemote.transact(4, _data, _reply, 0);
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

        public static IImsServiceController asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IImsServiceController)) {
                return new Proxy(obj);
            }
            return (IImsServiceController) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                IBinder iBinder = null;
                IImsMMTelFeature _result;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _result = createEmergencyMMTelFeature(data.readInt(), com.android.ims.internal.IImsFeatureStatusCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        if (_result != null) {
                            iBinder = _result.asBinder();
                        }
                        reply.writeStrongBinder(iBinder);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        _result = createMMTelFeature(data.readInt(), com.android.ims.internal.IImsFeatureStatusCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        if (_result != null) {
                            iBinder = _result.asBinder();
                        }
                        reply.writeStrongBinder(iBinder);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        IImsRcsFeature _result2 = createRcsFeature(data.readInt(), com.android.ims.internal.IImsFeatureStatusCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        if (_result2 != null) {
                            iBinder = _result2.asBinder();
                        }
                        reply.writeStrongBinder(iBinder);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        removeImsFeature(data.readInt(), data.readInt(), com.android.ims.internal.IImsFeatureStatusCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    IImsMMTelFeature createEmergencyMMTelFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException;

    IImsMMTelFeature createMMTelFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException;

    IImsRcsFeature createRcsFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException;

    void removeImsFeature(int i, int i2, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException;
}
