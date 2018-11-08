package tmsdk.fg.module.urlcheck;

import android.content.Context;
import com.qq.taf.jce.JceStruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import tmsdk.common.module.urlcheck.UrlCheckResult;
import tmsdk.common.utils.f;
import tmsdk.common.utils.s;
import tmsdk.fg.creator.BaseManagerF;
import tmsdkobf.ey;
import tmsdkobf.ez;
import tmsdkobf.im;
import tmsdkobf.jy;
import tmsdkobf.kr;

final class a extends BaseManagerF {
    private ConcurrentHashMap<Long, ez> Rf;
    private LinkedHashMap<Long, ez> Rg;
    private long Rh;
    private UrlCheckResult Ri = null;
    private final Object lock = new Object();

    a() {
    }

    private void kB() throws IOException {
        this.Rf = new ConcurrentHashMap();
        this.Rg = new LinkedHashMap();
    }

    private void kC() {
        long -l_1_J = System.currentTimeMillis();
        if (this.Rh != 0) {
            if ((-l_1_J - this.Rh >= 21600000 ? 1 : null) != null) {
                Object<Long> -l_3_R = new ArrayList();
                synchronized (this.Rg) {
                    Object -l_5_R = this.Rg.keySet().iterator();
                    while (-l_5_R.hasNext()) {
                        long -l_6_J = ((Long) -l_5_R.next()).longValue();
                        this.Rh = -l_6_J;
                        if ((-l_1_J - -l_6_J < 21600000 ? 1 : null) != null) {
                            break;
                        }
                        -l_5_R.remove();
                        -l_3_R.add(Long.valueOf(-l_6_J));
                    }
                }
                for (Long -l_5_R2 : -l_3_R) {
                    this.Rf.remove(-l_5_R2);
                }
                if (this.Rf.size() == 0) {
                    this.Rh = 0;
                }
            }
        }
    }

    public int a(String str, final ICheckUrlCallback iCheckUrlCallback) {
        if (str == null || str.length() == 0 || iCheckUrlCallback == null) {
            return -1006;
        }
        s.bW(64);
        kC();
        f.f("jiejie-url", "mCheckedUrlsCache size is " + this.Rf.values().size());
        for (ez -l_4_R : this.Rf.values()) {
            if (str.equalsIgnoreCase(-l_4_R.getUrl())) {
                iCheckUrlCallback.onCheckUrlCallback(new UrlCheckResult(-l_4_R));
                return 0;
            }
        }
        Object -l_3_R = im.bK();
        final Object -l_4_R2 = new ey();
        -l_4_R2.setUrl(str);
        Object -l_5_R = new ez();
        -l_5_R.setUrl(str);
        f.f("UrlCheckManager", "[GUID] " + -l_3_R.b());
        -l_3_R.a(1040, -l_4_R2, -l_5_R, 0, new jy(this) {
            final /* synthetic */ a Rl;

            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                f.f("UrlCheckManager", "onFinish() seqNo: " + i + " cmdId: " + i2 + " retCode: " + i3 + " dataRetCode: " + i4);
                ez -l_6_R = (ez) jceStruct;
                if (-l_6_R != null) {
                    long -l_7_J = System.currentTimeMillis();
                    -l_6_R.setUrl(-l_4_R2.getUrl());
                    this.Rl.Rf.put(Long.valueOf(-l_7_J), -l_6_R);
                    f.f("UrlCheckManager", "加入缓存, url is " + -l_6_R.getUrl());
                    synchronized (this.Rl.Rg) {
                        this.Rl.Rg.put(Long.valueOf(-l_7_J), -l_6_R);
                    }
                    if (this.Rl.Rh == 0) {
                        this.Rl.Rh = -l_7_J;
                    }
                    iCheckUrlCallback.onCheckUrlCallback(new UrlCheckResult(-l_6_R));
                    return;
                }
                f.f("UrlCheckManager", "response is null");
                iCheckUrlCallback.onCheckUrlCallback(null);
            }
        });
        kr.dz();
        return 0;
    }

    public UrlCheckResult dJ(String str) {
        if (str == null || str.length() == 0) {
            return null;
        }
        s.bW(64);
        kC();
        f.f("UrlCheckManager", "Sync--mCheckedUrlsCache size is " + this.Rf.values().size());
        for (ez -l_3_R : this.Rf.values()) {
            if (str.equalsIgnoreCase(-l_3_R.getUrl())) {
                return new UrlCheckResult(-l_3_R);
            }
        }
        Object -l_2_R = im.bK();
        final Object -l_3_R2 = new ey();
        -l_3_R2.setUrl(str);
        Object -l_4_R = new ez();
        -l_4_R.setUrl(str);
        this.Ri = null;
        f.f("UrlCheckManager", "[GUID] " + -l_2_R.b());
        -l_2_R.a(1040, -l_3_R2, -l_4_R, 0, new jy(this) {
            final /* synthetic */ a Rl;

            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                f.f("UrlCheckManager", "Sync--onFinish() seqNo: " + i + " cmdId: " + i2 + " retCode: " + i3 + " dataRetCode: " + i4);
                ez -l_6_R = (ez) jceStruct;
                if (-l_6_R != null) {
                    long -l_7_J = System.currentTimeMillis();
                    -l_6_R.setUrl(-l_3_R2.getUrl());
                    this.Rl.Rf.put(Long.valueOf(-l_7_J), -l_6_R);
                    f.f("UrlCheckManager", "sync--加入缓存, url is " + -l_6_R.getUrl());
                    synchronized (this.Rl.Rg) {
                        this.Rl.Rg.put(Long.valueOf(-l_7_J), -l_6_R);
                    }
                    if (this.Rl.Rh == 0) {
                        this.Rl.Rh = -l_7_J;
                    }
                    this.Rl.Ri = new UrlCheckResult(-l_6_R);
                }
                synchronized (this.Rl.lock) {
                    f.f("UrlCheckManager", "sync--notify");
                    this.Rl.lock.notify();
                }
            }
        });
        synchronized (this.lock) {
            try {
                f.f("UrlCheckManager", "sync--wait");
                this.lock.wait();
            } catch (Exception e) {
            }
        }
        kr.dz();
        return this.Ri;
    }

    public int getSingletonType() {
        return 0;
    }

    public void onCreate(Context context) {
        try {
            kB();
        } catch (Object -l_2_R) {
            -l_2_R.printStackTrace();
        }
    }
}
