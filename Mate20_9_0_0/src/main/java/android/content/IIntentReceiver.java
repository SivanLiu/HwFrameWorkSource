package android.content;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IIntentReceiver extends IInterface {

    public static abstract class Stub extends Binder implements IIntentReceiver {
        private static final String DESCRIPTOR = "android.content.IIntentReceiver";
        static final int TRANSACTION_performReceive = 1;

        private static class Proxy implements IIntentReceiver {
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

            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(resultCode);
                    _data.writeString(data);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(ordered);
                    _data.writeInt(sticky);
                    _data.writeInt(sendingUser);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IIntentReceiver asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IIntentReceiver)) {
                return new Proxy(obj);
            }
            return (IIntentReceiver) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            String descriptor = DESCRIPTOR;
            if (i == 1) {
                Intent _arg0;
                Parcel parcel2 = reply;
                parcel.enforceInterface(descriptor);
                Bundle bundle = null;
                if (data.readInt() != 0) {
                    _arg0 = (Intent) Intent.CREATOR.createFromParcel(parcel);
                } else {
                    _arg0 = null;
                }
                int _arg1 = data.readInt();
                String _arg2 = data.readString();
                if (data.readInt() != 0) {
                    bundle = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                }
                performReceive(_arg0, _arg1, _arg2, bundle, data.readInt() != 0, data.readInt() != 0, data.readInt());
                return true;
            } else if (i != 1598968902) {
                return super.onTransact(code, data, reply, flags);
            } else {
                reply.writeString(descriptor);
                return true;
            }
        }
    }

    void performReceive(Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) throws RemoteException;
}
