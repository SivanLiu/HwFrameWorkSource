package tmsdkobf;

public class rd extends rb {
    public rd(qz qzVar) {
        super(qzVar);
    }

    protected boolean ka() {
        rd -l_1_R = this;
        synchronized (this) {
            Object -l_2_R = this.Ob.jV().cX(this.Ob.jU());
            if (-l_2_R == null || -l_2_R.size() == 0) {
                this.Ob.onScanError(-2);
                return false;
            }
            Object -l_5_R;
            String[] -l_4_R = (String[]) -l_2_R.keySet().toArray(new String[0]);
            this.Pa.setRootPaths(-l_4_R);
            Object -l_6_R = this.Ob.jV().jG();
            if (-l_6_R != null) {
                -l_5_R = new String[-l_6_R.size()];
                for (int -l_7_I = 0; -l_7_I < -l_5_R.length; -l_7_I++) {
                    StringBuilder -l_8_R = new StringBuilder();
                    qt.a(-l_8_R, (qt) -l_6_R.get(-l_7_I));
                    -l_5_R[-l_7_I] = -l_8_R.toString();
                }
                this.Pa.setComRubRule(-l_5_R);
            }
            Object -l_7_R = this.Ob.jV().jH();
            if (-l_7_R != null) {
                -l_5_R = new String[-l_7_R.size()];
                for (int -l_8_I = 0; -l_8_I < -l_5_R.length; -l_8_I++) {
                    Object -l_9_R = new StringBuilder();
                    qt.a(-l_9_R, (qt) -l_7_R.get(-l_8_I));
                    -l_5_R[-l_8_I] = -l_9_R.toString();
                }
                this.Pa.setOtherFilterRule(-l_5_R);
            }
            Object<String> -l_8_R2 = this.Ob.jZ();
            if (-l_8_R2 != null && 1 <= -l_8_R2.size()) {
                String[] strArr = -l_4_R;
                for (Object -l_12_R : -l_4_R) {
                    for (String -l_14_R : -l_8_R2) {
                        if (!this.Pb) {
                            this.Pa.scanPath(-l_14_R, -l_12_R);
                        }
                    }
                }
                return true;
            }
            this.Ob.onScanError(-3);
            return false;
        }
    }

    protected void kb() {
        this.Pc.bX(2);
    }
}
