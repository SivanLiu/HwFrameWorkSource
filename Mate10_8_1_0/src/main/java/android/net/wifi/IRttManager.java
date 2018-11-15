package android.net.wifi;

import android.net.wifi.RttManager.RttCapabilities;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRttManager extends IInterface {

    public static abstract class Stub extends Binder implements IRttManager {
        private static final String DESCRIPTOR = "android.net.wifi.IRttManager";
        static final int TRANSACTION_getMessenger = 1;
        static final int TRANSACTION_getRttCapabilities = 2;

        private static class Proxy implements IRttManager {
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

            public Messenger getMessenger(IBinder binder, int[] key) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Messenger messenger;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(binder);
                    if (key == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(key.length);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        messenger = (Messenger) Messenger.CREATOR.createFromParcel(_reply);
                    } else {
                        messenger = null;
                    }
                    _reply.readIntArray(key);
                    return messenger;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public RttCapabilities getRttCapabilities() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    RttCapabilities rttCapabilities;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        rttCapabilities = (RttCapabilities) RttCapabilities.CREATOR.createFromParcel(_reply);
                    } else {
                        rttCapabilities = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return rttCapabilities;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRttManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IRttManager)) {
                return new Proxy(obj);
            }
            return (IRttManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    int[] iArr;
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg0 = data.readStrongBinder();
                    int _arg1_length = data.readInt();
                    if (_arg1_length < 0) {
                        iArr = null;
                    } else {
                        iArr = new int[_arg1_length];
                    }
                    Messenger _result = getMessenger(_arg0, iArr);
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    reply.writeIntArray(iArr);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    RttCapabilities _result2 = getRttCapabilities();
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case IBinder.INTERFACE_TRANSACTION /*1598968902*/:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    Messenger getMessenger(IBinder iBinder, int[] iArr) throws RemoteException;

    RttCapabilities getRttCapabilities() throws RemoteException;
}
