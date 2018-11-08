package tmsdk.fg.module.urlcheck;

import android.content.Context;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import tmsdk.common.module.urlcheck.UrlCheckResultV3;
import tmsdk.common.utils.f;
import tmsdk.fg.creator.BaseManagerF;
import tmsdkobf.cu;
import tmsdkobf.cv;
import tmsdkobf.cw;
import tmsdkobf.im;
import tmsdkobf.jy;
import tmsdkobf.kr;

final class b extends BaseManagerF {
    private ConcurrentHashMap<Long, UrlCheckResultV3> Rn;
    private LinkedHashMap<Long, UrlCheckResultV3> Ro;
    private long Rp;

    b() {
    }

    private void kD() {
        if (this.Rn == null) {
            this.Rn = new ConcurrentHashMap();
        }
        if (this.Ro == null) {
            this.Ro = new LinkedHashMap();
        }
    }

    private void kE() {
        long -l_1_J = System.currentTimeMillis();
        if (this.Rp != 0) {
            if ((-l_1_J - this.Rp >= 21600000 ? 1 : null) != null) {
                Object<Long> -l_3_R = new ArrayList();
                synchronized (this.Ro) {
                    Object -l_5_R = this.Ro.keySet().iterator();
                    while (-l_5_R.hasNext()) {
                        long -l_6_J = ((Long) -l_5_R.next()).longValue();
                        this.Rp = -l_6_J;
                        if ((-l_1_J - -l_6_J < 21600000 ? 1 : null) != null) {
                            break;
                        }
                        -l_5_R.remove();
                        -l_3_R.add(Long.valueOf(-l_6_J));
                    }
                }
                for (Long -l_5_R2 : -l_3_R) {
                    this.Rn.remove(-l_5_R2);
                }
                if (this.Rn.size() == 0) {
                    this.Rp = 0;
                }
            }
        }
    }

    public boolean a(final String str, int i, final ICheckUrlCallbackV3 iCheckUrlCallbackV3) {
        if (str == null || str.length() == 0 || iCheckUrlCallbackV3 == null) {
            return false;
        }
        kD();
        kE();
        for (UrlCheckResultV3 -l_5_R : this.Rn.values()) {
            if (str.equalsIgnoreCase(-l_5_R.url)) {
                iCheckUrlCallbackV3.onCheckUrlCallback(-l_5_R);
                return true;
            }
        }
        Object -l_4_R = new cu();
        -l_4_R.fX = 1;
        -l_4_R.fY = i;
        -l_4_R.fZ = new cw();
        -l_4_R.fZ.url = str;
        Object -l_5_R2 = new cv();
        Object -l_6_R = im.bK();
        f.f("UrlCheckManagerV2Impl", "sendShark url = " + -l_4_R.fZ.url);
        -l_6_R.a(2002, -l_4_R, -l_5_R2, 0, new jy(this) {
            final /* synthetic */ b Rs;

            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                f.f("UrlCheckManagerV2Impl", "onFinish() seqNo: " + i + " cmdId: " + i2 + " retCode: " + i3 + " dataRetCode: " + i4);
                cv -l_6_R = (cv) jceStruct;
                if (-l_6_R == null || -l_6_R.gb == null) {
                    iCheckUrlCallbackV3.onCheckUrlCallback(null);
                    return;
                }
                Object -l_7_R = new UrlCheckResultV3(str, -l_6_R.gb);
                iCheckUrlCallbackV3.onCheckUrlCallback(-l_7_R);
                long -l_8_J = System.currentTimeMillis();
                this.Rs.Rn.put(Long.valueOf(-l_8_J), -l_7_R);
                synchronized (this.Rs.Ro) {
                    this.Rs.Ro.put(Long.valueOf(-l_8_J), -l_7_R);
                }
                if (this.Rs.Rp == 0) {
                    this.Rs.Rp = -l_8_J;
                }
            }
        });
        kr.dz();
        return true;
    }

    public int getSingletonType() {
        return 0;
    }

    public void onCreate(Context context) {
    }
}
