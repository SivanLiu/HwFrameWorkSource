package android.telephony.ims.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IImsRegistration extends IInterface {

    public static abstract class Stub extends Binder implements IImsRegistration {
        private static final String DESCRIPTOR = "android.telephony.ims.aidl.IImsRegistration";
        static final int TRANSACTION_addRegistrationCallback = 2;
        static final int TRANSACTION_getRegistrationTechnology = 1;
        static final int TRANSACTION_removeRegistrationCallback = 3;

        private static class Proxy implements IImsRegistration {
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

            public int getRegistrationTechnology() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addRegistrationCallback(IImsRegistrationCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(c != null ? c.asBinder() : null);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void removeRegistrationCallback(IImsRegistrationCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(c != null ? c.asBinder() : null);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsRegistration asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IImsRegistration)) {
                return new Proxy(obj);
            }
            return (IImsRegistration) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        int _result = getRegistrationTechnology();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        addRegistrationCallback(android.telephony.ims.aidl.IImsRegistrationCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        removeRegistrationCallback(android.telephony.ims.aidl.IImsRegistrationCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void addRegistrationCallback(IImsRegistrationCallback iImsRegistrationCallback) throws RemoteException;

    int getRegistrationTechnology() throws RemoteException;

    void removeRegistrationCallback(IImsRegistrationCallback iImsRegistrationCallback) throws RemoteException;
}
