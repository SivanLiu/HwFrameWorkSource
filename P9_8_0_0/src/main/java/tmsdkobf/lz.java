package tmsdkobf;

import android.os.IBinder;
import android.os.Parcel;

final class lz implements ly {
    private int TRANSACTION_call;
    private int TRANSACTION_cancelMissedCallsNotification;
    private int TRANSACTION_endCall;
    private String mName;
    private String zH;

    public lz(String str) {
        Object -l_2_R;
        this.mName = str;
        try {
            -l_2_R = eN();
            if (-l_2_R != null) {
                this.zH = -l_2_R.getInterfaceDescriptor();
                mh.bY(this.zH + "$Stub");
                this.TRANSACTION_call = mh.e("TRANSACTION_call", 2);
                this.TRANSACTION_endCall = mh.e("TRANSACTION_endCall", 5);
                this.TRANSACTION_cancelMissedCallsNotification = mh.e("TRANSACTION_cancelMissedCallsNotification", 13);
            }
        } catch (Object -l_2_R2) {
            -l_2_R2.printStackTrace();
            this.TRANSACTION_call = 2;
            this.TRANSACTION_endCall = 5;
            this.TRANSACTION_cancelMissedCallsNotification = 13;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean aP(int i) {
        Object -l_2_R = Parcel.obtain();
        Object -l_3_R = Parcel.obtain();
        int -l_4_I = 0;
        try {
            -l_2_R.writeInterfaceToken(this.zH);
            -l_2_R.writeInt(i);
            eN().transact(this.TRANSACTION_endCall, -l_2_R, -l_3_R, 0);
            -l_3_R.readException();
            -l_4_I = -l_3_R.readInt() == 0 ? 0 : 1;
            -l_3_R.recycle();
            -l_2_R.recycle();
        } catch (Object -l_5_R) {
            -l_5_R.printStackTrace();
        } catch (Throwable th) {
            -l_3_R.recycle();
            -l_2_R.recycle();
        }
        return -l_4_I;
    }

    public IBinder eN() {
        return mi.getService(this.mName);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean endCall() {
        Object -l_1_R = Parcel.obtain();
        Object -l_2_R = Parcel.obtain();
        int -l_3_I = 0;
        try {
            -l_1_R.writeInterfaceToken(this.zH);
            eN().transact(this.TRANSACTION_endCall, -l_1_R, -l_2_R, 0);
            -l_2_R.readException();
            -l_3_I = -l_2_R.readInt() == 0 ? 0 : 1;
            -l_2_R.recycle();
            -l_1_R.recycle();
        } catch (Object -l_4_R) {
            -l_4_R.printStackTrace();
        } catch (Object -l_4_R2) {
            -l_4_R2.printStackTrace();
        } catch (Throwable th) {
            -l_2_R.recycle();
            -l_1_R.recycle();
        }
        return -l_3_I;
    }
}
