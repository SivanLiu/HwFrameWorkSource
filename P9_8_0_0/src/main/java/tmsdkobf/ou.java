package tmsdkobf;

import android.content.Context;
import com.qq.taf.jce.JceStruct;
import java.util.concurrent.atomic.AtomicReference;
import tmsdk.common.creator.BaseManagerC;
import tmsdk.common.utils.f;

final class ou extends BaseManagerC {
    public static String TAG = "SharkSessionManagerImpl";
    private os IX;
    final int IY = 8000;
    private Context mContext;
    ob wS;

    ou() {
    }

    public int a(em emVar) {
        int -l_5_I;
        final Object -l_4_R = new AtomicReference(Integer.valueOf(100));
        JceStruct -l_6_R = new dt();
        -l_6_R.hR = this.IX.ht();
        -l_6_R.hS = this.IX.hu();
        -l_6_R.hX = emVar;
        this.wS.a(554, -l_6_R, null, 0, new jy(this) {
            final /* synthetic */ ou Ja;

            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                f.f("jiejie-modified", "SoftList onFinish " + i3);
                synchronized (-l_4_R) {
                    -l_4_R.set(Integer.valueOf(i3));
                    -l_4_R.notify();
                }
            }
        }, 8000);
        Object -l_7_R = -l_4_R;
        synchronized (-l_4_R) {
            while (((Integer) -l_4_R.get()).intValue() == 100) {
                try {
                    -l_4_R.wait();
                } catch (Object -l_8_R) {
                    -l_8_R.printStackTrace();
                }
            }
            -l_5_I = ((Integer) -l_4_R.get()).intValue();
        }
        return -l_5_I;
    }

    public int hw() {
        int -l_4_I;
        final Object -l_3_R = new AtomicReference(Integer.valueOf(100));
        JceStruct -l_5_R = new ds();
        -l_5_R.hR = this.IX.ht();
        -l_5_R.hS = this.IX.hu();
        -l_5_R.hT = this.IX.hv();
        f.f("jiejie-modified", "ChannelId is " + -l_5_R.hT.id);
        f.f("jiejie-modified", "ChannelInfo is " + -l_5_R.hT.toString());
        this.wS.a(553, -l_5_R, null, 0, new jy(this) {
            final /* synthetic */ ou Ja;

            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                f.f("jiejie-modified", "ChannelInfo onFinish " + i3);
                synchronized (-l_3_R) {
                    -l_3_R.set(Integer.valueOf(i3));
                    -l_3_R.notify();
                }
            }
        }, 8000);
        Object -l_6_R = -l_3_R;
        synchronized (-l_3_R) {
            while (((Integer) -l_3_R.get()).intValue() == 100) {
                try {
                    -l_3_R.wait();
                } catch (Object -l_7_R) {
                    -l_7_R.printStackTrace();
                }
            }
            -l_4_I = ((Integer) -l_3_R.get()).intValue();
        }
        return -l_4_I;
    }

    public void onCreate(Context context) {
        this.mContext = context;
        this.IX = new os(this.mContext);
        this.wS = im.bK();
    }
}
