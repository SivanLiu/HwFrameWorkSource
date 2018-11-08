package tmsdkobf;

import tmsdk.common.module.aresengine.FilterConfig;
import tmsdk.common.module.aresengine.FilterResult;
import tmsdk.common.module.aresengine.TelephonyEntity;

public final class hm {
    private Object mLock = new Object();
    private int[] pU;
    private a[] pV;

    static abstract class a {
        private TelephonyEntity mData;
        private Object[] mParams;
        private int mState;
        private FilterResult pW;
        private int pX;
        private Object pY;

        a() {
        }

        public void a(Object obj) {
            this.pY = obj;
        }

        public void a(FilterResult filterResult) {
            this.pW = filterResult;
        }

        public TelephonyEntity bm() {
            return this.mData;
        }

        public int bn() {
            return this.mState;
        }

        public Object[] bo() {
            return this.mParams;
        }

        public int bp() {
            return this.pX;
        }

        public Object bq() {
            return this.pY;
        }

        abstract boolean br();

        abstract void bs();
    }

    private FilterResult a(int i, int i2, TelephonyEntity telephonyEntity, FilterConfig filterConfig, Object... objArr) {
        Object -l_6_R = null;
        a -l_7_R = this.pV[i];
        if (-l_7_R != null) {
            synchronized (this.pV) {
                -l_7_R.mData = telephonyEntity;
                -l_7_R.mState = i2;
                -l_7_R.mParams = objArr;
                -l_7_R.pX = this.pU[i];
                if (-l_7_R.br()) {
                    -l_7_R.bs();
                }
                -l_6_R = -l_7_R.pW;
                -l_7_R.pY = null;
                -l_7_R.mData = null;
                -l_7_R.a(null);
                -l_7_R.mParams = null;
            }
        }
        return -l_6_R;
    }

    private int ab(int i) {
        for (int -l_3_I = 0; -l_3_I < this.pU.length; -l_3_I++) {
            if (this.pU[-l_3_I] == i) {
                return -l_3_I;
            }
        }
        return -1;
    }

    public FilterResult a(TelephonyEntity telephonyEntity, FilterConfig filterConfig, Object... objArr) {
        Object -l_4_R = null;
        if (!(this.pU == null || this.pV == null || filterConfig == null)) {
            synchronized (this.mLock) {
                for (int -l_6_I = 0; -l_6_I < this.pU.length; -l_6_I++) {
                    int -l_8_I = filterConfig.get(this.pU[-l_6_I]);
                    if (!(-l_8_I == 4 || -l_8_I == 3)) {
                        -l_4_R = a(-l_6_I, -l_8_I, telephonyEntity, filterConfig, objArr);
                    }
                    if (-l_4_R != null) {
                        break;
                    }
                }
            }
        }
        return -l_4_R;
    }

    public void a(int i, a aVar) {
        int -l_3_I = ab(i);
        if (-l_3_I < 0) {
            throw new IndexOutOfBoundsException("the filed " + i + "is not define from setOrderedFileds method.");
        }
        this.pV[-l_3_I] = aVar;
    }

    public void a(int... iArr) {
        this.pU = iArr;
        this.pV = new a[this.pU.length];
    }
}
