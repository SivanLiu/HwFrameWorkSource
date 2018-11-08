package android.service.euicc;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IGetDefaultDownloadableSubscriptionListCallback extends IInterface {

    public static abstract class Stub extends Binder implements IGetDefaultDownloadableSubscriptionListCallback {
        private static final String DESCRIPTOR = "android.service.euicc.IGetDefaultDownloadableSubscriptionListCallback";
        static final int TRANSACTION_onComplete = 1;

        private static class Proxy implements IGetDefaultDownloadableSubscriptionListCallback {
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

            public void onComplete(GetDefaultDownloadableSubscriptionListResult result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGetDefaultDownloadableSubscriptionListCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IGetDefaultDownloadableSubscriptionListCallback)) {
                return new Proxy(obj);
            }
            return (IGetDefaultDownloadableSubscriptionListCallback) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    GetDefaultDownloadableSubscriptionListResult getDefaultDownloadableSubscriptionListResult;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        getDefaultDownloadableSubscriptionListResult = (GetDefaultDownloadableSubscriptionListResult) GetDefaultDownloadableSubscriptionListResult.CREATOR.createFromParcel(data);
                    } else {
                        getDefaultDownloadableSubscriptionListResult = null;
                    }
                    onComplete(getDefaultDownloadableSubscriptionListResult);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    void onComplete(GetDefaultDownloadableSubscriptionListResult getDefaultDownloadableSubscriptionListResult) throws RemoteException;
}
