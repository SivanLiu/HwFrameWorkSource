package tmsdkobf;

import android.content.Context;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import tmsdk.common.creator.BaseManagerC;

final class pr extends BaseManagerC {
    public static String TAG = "WupSessionManagerImpl";
    private po KA;
    private Context mContext;

    pr() {
    }

    public int b(ec ecVar, AtomicReference<eg> atomicReference) {
        pp -l_3_R = pm.bP(9);
        Object -l_4_R = new HashMap(3);
        -l_4_R.put("phonetype", this.KA.ht());
        -l_4_R.put("userinfo", this.KA.hu());
        -l_4_R.put("deviceinfo", ecVar);
        -l_3_R.Kv = -l_4_R;
        int -l_5_I = this.KA.a(-l_3_R, true);
        if (-l_5_I != 0) {
            return -l_5_I;
        }
        Object -l_7_R = this.KA.a(-l_3_R.Kx, "guidinfo", new eg());
        if (-l_7_R != null) {
            atomicReference.set((eg) -l_7_R);
        }
        return 0;
    }

    public int getSingletonType() {
        return 1;
    }

    public pl hV() {
        return this.KA;
    }

    public void onCreate(Context context) {
        this.mContext = context;
        this.KA = new po(this.mContext);
    }

    public int u(List<es> list) {
        Object -l_2_R = pm.bP(12);
        Object -l_3_R = new HashMap(3);
        -l_3_R.put("phonetype", this.KA.hO());
        -l_3_R.put("userinfo", this.KA.hP());
        -l_3_R.put("vecSmsReport", list);
        -l_2_R.Kv = -l_3_R;
        int -l_4_I = this.KA.a(-l_2_R);
        return -l_4_I == 0 ? 0 : -l_4_I;
    }
}
