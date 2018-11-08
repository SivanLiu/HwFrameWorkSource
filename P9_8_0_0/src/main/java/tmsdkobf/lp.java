package tmsdkobf;

public class lp {
    private md vu = new md("CheckPoint");
    private StringBuffer yS = new StringBuffer();

    public void commit() {
        if (this.yS.length() > 0) {
            Object -l_1_R = this.vu.getString("data", null);
            if (-l_1_R == null) {
                -l_1_R = "";
            }
            this.vu.a("data", -l_1_R + this.yS.toString(), true);
            this.yS = new StringBuffer();
        }
    }

    public void t(int i, int i2) {
        this.yS.append(i + ":" + i2 + ";");
    }
}
