package tmsdkobf;

import tmsdk.common.utils.f;

public class rc extends rb {
    public rc(qz qzVar) {
        super(qzVar);
    }

    protected boolean ka() {
        rc -l_1_R = this;
        synchronized (this) {
            f.e("ZhongSi", "doScan start");
            if (this.Ob.jD()) {
                Object obj;
                Object -l_2_R = this.Ob.jV().jE();
                if (-l_2_R != null) {
                    this.Pa.setWhitePaths(-l_2_R);
                    obj = -l_2_R;
                    for (Object -l_6_R : -l_2_R) {
                        f.e("ZhongSi", "doScan white path:" + -l_6_R);
                    }
                }
                obj = this.Ob.jV().jF();
                if (obj == null || obj.length == 0) {
                    this.Ob.onScanError(-2);
                    return false;
                }
                Object -l_5_R;
                int -l_6_I;
                Object -l_7_R;
                this.Pa.setRootPaths(obj);
                Object -l_4_R = this.Ob.jV().jG();
                if (-l_4_R != null) {
                    -l_5_R = new String[-l_4_R.size()];
                    for (-l_6_I = 0; -l_6_I < -l_5_R.length; -l_6_I++) {
                        -l_7_R = new StringBuilder();
                        qt.a(-l_7_R, (qt) -l_4_R.get(-l_6_I));
                        -l_5_R[-l_6_I] = -l_7_R.toString();
                    }
                    this.Pa.setComRubRule(-l_5_R);
                }
                -l_4_R = this.Ob.jV().jH();
                if (-l_4_R != null) {
                    -l_5_R = new String[-l_4_R.size()];
                    for (-l_6_I = 0; -l_6_I < -l_5_R.length; -l_6_I++) {
                        -l_7_R = new StringBuilder();
                        qt.a(-l_7_R, (qt) -l_4_R.get(-l_6_I));
                        -l_5_R[-l_6_I] = -l_7_R.toString();
                    }
                    this.Pa.setOtherFilterRule(-l_5_R);
                }
                Object<String> -l_5_R2 = this.Ob.jZ();
                if (-l_5_R2 != null && 1 <= -l_5_R2.size()) {
                    for (String -l_7_R2 : -l_5_R2) {
                        if (!this.Pb) {
                            this.Pa.scanPath(-l_7_R2, "/");
                        }
                    }
                    return true;
                }
                this.Ob.onScanError(-3);
                return false;
            }
            this.Ob.onScanError(-2);
            return false;
        }
    }

    protected void kb() {
        if (this.Ob.jR()) {
            this.Pc.bX(1);
        } else {
            this.Pc.bX(0);
        }
    }
}
