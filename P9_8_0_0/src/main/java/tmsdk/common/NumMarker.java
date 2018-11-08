package tmsdk.common;

import android.content.Context;
import android.os.Build.VERSION;
import android.text.TextUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.tcc.NumMarkerConsts;
import tmsdk.common.utils.f;
import tmsdk.common.utils.r;
import tmsdkobf.lu;

public class NumMarker implements NumMarkerConsts {
    public static final int KEY_TAG_CALL_TIME_LENGTH = 1;
    public static final String Tag = "NumMarkerTag";
    private static NumMarker xg = null;
    private Context mContext;
    private Object mLock = new Object();
    private String mPath = "";
    private long xh = 0;
    private boolean xi = true;
    private boolean xj = true;
    private List<Integer> xk = new ArrayList();
    private List<String> xl = new ArrayList();
    private List<Integer> xm = new ArrayList();
    private List<Integer> xn = new ArrayList();

    public static class MarkFileInfo {
        public String md5;
        public int timeStampSecondLastDiff;
        public int timeStampSecondWhole;
        public int version;
    }

    public static class NumMark {
        public int count;
        public String num;
        public String tagName;
        public int tagValue;
    }

    static {
        TMSDKContext.registerNatives(4, NumMarker.class);
    }

    private NumMarker(Context context) {
        f.f(Tag, "NumMarker()");
        this.mContext = context;
        this.xh = nNewInstance(VERSION.SDK_INT);
        if (this.xh != 0) {
            Object -l_2_R = "40458.sdb";
            f.f(Tag, "datafile name =" + -l_2_R);
            this.mPath = lu.b(this.mContext, -l_2_R, null);
            f.f(Tag, "NumMarker() mPath: " + this.mPath);
            if (this.mPath != null) {
                if (!"".equals(this.mPath)) {
                    nSetPath(this.xh, this.mPath);
                }
            }
            this.mPath = this.mContext.getFilesDir().toString() + File.separator + -l_2_R;
            nSetPath(this.xh, this.mPath);
        }
    }

    public static synchronized NumMarker getDefault(Context context) {
        NumMarker numMarker;
        synchronized (NumMarker.class) {
            if (xg == null) {
                xg = new NumMarker(context);
            }
            numMarker = xg;
        }
        return numMarker;
    }

    private native void nDestroyInstance(long j);

    private native String nGetDataMd5(long j, String str);

    private native boolean nGetHeaderInfo(long j, AtomicInteger atomicInteger, AtomicInteger atomicInteger2, AtomicInteger atomicInteger3, AtomicReference<String> atomicReference);

    private native boolean nGetMarkInfoByPhoneNumber(long j, String str, AtomicInteger atomicInteger, AtomicInteger atomicInteger2);

    private native boolean nGetTagList(long j, List<Integer> list, List<Integer> list2);

    private native boolean nGetTypeNameMapping(long j, List<Integer> list, List<String> list2);

    private native long nNewInstance(int i);

    private native boolean nRepack(long j);

    private native boolean nSetPath(long j, String str);

    private native int nUpdate(long j, String str, String str2);

    public synchronized void destroy() {
        this.xk.clear();
        this.xl.clear();
        this.xm.clear();
        this.xn.clear();
        if (this.xh != 0) {
            nDestroyInstance(this.xh);
        }
        this.xh = 0;
        xg = null;
    }

    public void getConfigList(List<Integer> list, List<Integer> list2) {
        if (this.xj) {
            this.xm.clear();
            this.xn.clear();
            this.xj = false;
            if (this.xh != 0) {
                nGetTagList(this.xh, this.xm, this.xn);
            }
            if (this.xn.size() >= 1) {
                f.f(Tag, "getConfigList() value[0]: " + this.xn.get(0));
            }
        }
        list.clear();
        list2.clear();
        list.addAll(this.xm);
        list2.addAll(this.xn);
    }

    public String getDataMd5(String str) {
        return this.xh != 0 ? nGetDataMd5(this.xh, str) : null;
    }

    public NumMark getInfoOfNum(String str) {
        Object -l_2_R = new AtomicInteger(0);
        Object -l_3_R = new AtomicInteger(0);
        try {
            if (this.xh != 0 && nGetMarkInfoByPhoneNumber(this.xh, str, -l_2_R, -l_3_R)) {
                Object -l_4_R = new NumMark();
                -l_4_R.num = str;
                -l_4_R.tagValue = -l_2_R.get();
                -l_4_R.count = -l_3_R.get();
                return -l_4_R;
            }
        } catch (Object -l_4_R2) {
            f.e(Tag, -l_4_R2);
        }
        return null;
    }

    public NumMark getInfoOfNumForBigFile(String str) {
        Object -l_4_R;
        Object -l_6_R;
        long -l_2_J = 0;
        try {
            -l_2_J = nNewInstance(VERSION.SDK_INT);
            if (-l_2_J != 0) {
                -l_4_R = r.k(this.mContext, UpdateConfig.getLargeMarkFileName());
                if (TextUtils.isEmpty(-l_4_R)) {
                    if (-l_2_J != 0) {
                        try {
                            nDestroyInstance(-l_2_J);
                        } catch (Object -l_6_R2) {
                            f.e(Tag, -l_6_R2);
                        }
                    }
                    return null;
                }
                nSetPath(-l_2_J, -l_4_R);
                Object -l_5_R = new AtomicInteger(0);
                -l_6_R2 = new AtomicInteger(0);
                if (nGetMarkInfoByPhoneNumber(-l_2_J, str, -l_5_R, -l_6_R2)) {
                    Object -l_7_R = new NumMark();
                    -l_7_R.num = str;
                    -l_7_R.tagValue = -l_5_R.get();
                    -l_7_R.count = -l_6_R2.get();
                    Object -l_8_R = -l_7_R;
                    if (-l_2_J != 0) {
                        try {
                            nDestroyInstance(-l_2_J);
                        } catch (Object -l_9_R) {
                            f.e(Tag, -l_9_R);
                        }
                    }
                    return -l_7_R;
                }
            }
            if (-l_2_J != 0) {
                try {
                    nDestroyInstance(-l_2_J);
                } catch (Object -l_4_R2) {
                    f.e(Tag, -l_4_R2);
                }
            }
        } catch (Object -l_4_R22) {
            f.e(Tag, -l_4_R22);
        }
        return null;
    }

    public MarkFileInfo getMarkFileInfo(int i, String str) {
        Object -l_3_R = new MarkFileInfo();
        Object -l_4_R = new AtomicInteger(0);
        Object -l_5_R = new AtomicInteger(0);
        Object -l_6_R = new AtomicInteger(0);
        Object -l_7_R = new AtomicReference("");
        long -l_8_J = nNewInstance(VERSION.SDK_INT);
        if (-l_8_J != 0) {
            nSetPath(-l_8_J, str);
        }
        if (-l_8_J != 0 && nGetHeaderInfo(-l_8_J, -l_4_R, -l_5_R, -l_6_R, -l_7_R)) {
            -l_3_R.version = -l_4_R.get();
            -l_3_R.timeStampSecondWhole = -l_5_R.get();
            -l_3_R.timeStampSecondLastDiff = -l_6_R.get();
            -l_3_R.md5 = -l_7_R.get() == null ? "" : (String) -l_7_R.get();
        }
        return -l_3_R;
    }

    public void getMarkList(List<Integer> list, List<String> list2) {
        if (this.xi) {
            this.xk.clear();
            this.xl.clear();
            this.xi = false;
            if (this.xh != 0) {
                nGetTypeNameMapping(this.xh, this.xk, this.xl);
            }
        }
        list.clear();
        list2.clear();
        list.addAll(this.xk);
        list2.addAll(this.xl);
    }

    public boolean refreshMarkFile() {
        return this.xh != 0 ? nRepack(this.xh) : false;
    }

    public int updateMarkBigFile(String str, String str2) {
        int -l_3_I = -3;
        long -l_4_J = 0;
        try {
            -l_4_J = nNewInstance(VERSION.SDK_INT);
            if (-l_4_J != 0) {
                this.mPath = r.b(this.mContext, UpdateConfig.getLargeMarkFileId(), ".sdb");
                if (TextUtils.isEmpty(this.mPath)) {
                    this.mPath = this.mContext.getFilesDir().toString() + File.separator + UpdateConfig.getLargeMarkFileName();
                }
                nSetPath(-l_4_J, this.mPath);
                synchronized (this.mLock) {
                    if (-l_4_J != 0) {
                        -l_3_I = nUpdate(-l_4_J, str, str2);
                    }
                }
            }
            if (-l_4_J != 0) {
                try {
                    nDestroyInstance(-l_4_J);
                } catch (Object -l_6_R) {
                    f.e(Tag, -l_6_R);
                }
            }
        } catch (Object -l_6_R2) {
            f.e(Tag, -l_6_R2);
        }
        return -l_3_I;
    }

    public int updateMarkFile(String str, String str2) {
        int -l_3_I = -3;
        synchronized (this.mLock) {
            if (this.xh != 0) {
                -l_3_I = nUpdate(this.xh, str, str2);
            }
        }
        if (-l_3_I == 0) {
            this.xi = true;
            this.xj = true;
        }
        return -l_3_I;
    }
}
