package tmsdkobf;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import tmsdk.common.DataEntity;

public interface ih extends IInterface {

    public static abstract class b extends Binder implements ih {
        public b() {
            attachInterface(this, "com.tencent.tmsecure.common.ISDKClient");
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean equals(Object obj) {
            return !(obj instanceof b) ? false : super.equals((b) obj);
        }

        public String getInterfaceDescriptor() {
            return "com.tencent.tmsecure.common.ISDKClient";
        }

        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i != 20100405) {
                return super.onTransact(i, parcel, parcel2, i2);
            }
            parcel.enforceInterface("com.tencent.tmsecure.common.ISDKClient");
            Object -l_6_R = sendMessage((DataEntity) parcel.readParcelable(DataEntity.class.getClassLoader()));
            parcel2.writeNoException();
            parcel2.writeParcelable(-l_6_R, 0);
            return true;
        }
    }

    public static class a implements ih {
        private IBinder mRemote;
        private int rx = Binder.getCallingUid();

        a(IBinder iBinder) {
            this.mRemote = iBinder;
        }

        public static ih a(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            Object -l_1_R = iBinder.queryLocalInterface("com.tencent.tmsecure.common.ISDKClient");
            return (-l_1_R != null && (-l_1_R instanceof ih)) ? (ih) -l_1_R : new a(iBinder);
        }

        public IBinder asBinder() {
            return this.mRemote;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof a)) {
                return false;
            }
            a -l_2_R = (a) obj;
            if (this.mRemote == -l_2_R.mRemote && this.rx == -l_2_R.rx) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return this.rx;
        }

        public DataEntity sendMessage(DataEntity dataEntity) throws RemoteException {
            Object -l_2_R = Parcel.obtain();
            Object -l_3_R = Parcel.obtain();
            -l_2_R.writeInterfaceToken("com.tencent.tmsecure.common.ISDKClient");
            -l_2_R.writeParcelable(dataEntity, 0);
            Object obj = null;
            try {
                this.mRemote.transact(20100405, -l_2_R, -l_3_R, 0);
                -l_3_R.readException();
                obj = (DataEntity) -l_3_R.readParcelable(DataEntity.class.getClassLoader());
                return obj;
            } finally {
                -l_2_R.recycle();
                -l_3_R.recycle();
            }
        }
    }

    DataEntity sendMessage(DataEntity dataEntity) throws RemoteException;
}
