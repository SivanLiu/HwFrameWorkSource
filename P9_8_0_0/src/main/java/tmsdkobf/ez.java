package tmsdkobf;

import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import com.qq.taf.jce.JceDisplayer;
import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;

public final class ez extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF = (!ez.class.desiredAssertionStatus());
    static int ld = 0;
    static int le = 0;
    public String body = "";
    public int kZ = fa.lg.value();
    public String la = "";
    public int lb = 0;
    public int lc = 0;
    public int mainHarmId = fa.lg.value();
    public int seq = 0;
    public String title = "";
    public String url = "";

    public Object clone() {
        Object -l_1_R = null;
        try {
            -l_1_R = super.clone();
        } catch (CloneNotSupportedException e) {
            if (!bF) {
                throw new AssertionError();
            }
        }
        return -l_1_R;
    }

    public int d() {
        return this.lc;
    }

    public void display(StringBuilder stringBuilder, int i) {
        Object -l_3_R = new JceDisplayer(stringBuilder, i);
        -l_3_R.display(this.url, CheckVersionField.CHECK_VERSION_SERVER_URL);
        -l_3_R.display(this.mainHarmId, "mainHarmId");
        -l_3_R.display(this.kZ, "subHarmId");
        -l_3_R.display(this.seq, "seq");
        -l_3_R.display(this.la, "desc");
        -l_3_R.display(this.lb, "UrlType");
        -l_3_R.display(this.title, "title");
        -l_3_R.display(this.body, "body");
        -l_3_R.display(this.lc, "evilclass");
    }

    public void displaySimple(StringBuilder stringBuilder, int i) {
        Object -l_3_R = new JceDisplayer(stringBuilder, i);
        -l_3_R.displaySimple(this.url, true);
        -l_3_R.displaySimple(this.mainHarmId, true);
        -l_3_R.displaySimple(this.kZ, true);
        -l_3_R.displaySimple(this.seq, true);
        -l_3_R.displaySimple(this.la, true);
        -l_3_R.displaySimple(this.lb, true);
        -l_3_R.displaySimple(this.title, true);
        -l_3_R.displaySimple(this.body, true);
        -l_3_R.displaySimple(this.lc, false);
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null) {
            return false;
        }
        ez -l_2_R = (ez) obj;
        if (d.equals(this.url, -l_2_R.url) && d.equals(this.mainHarmId, -l_2_R.mainHarmId) && d.equals(this.kZ, -l_2_R.kZ) && d.equals(this.seq, -l_2_R.seq) && d.equals(this.la, -l_2_R.la) && d.equals(this.lb, -l_2_R.lb) && d.equals(this.title, -l_2_R.title) && d.equals(this.body, -l_2_R.body) && d.equals(this.lc, -l_2_R.lc)) {
            z = true;
        }
        return z;
    }

    public String getUrl() {
        return this.url;
    }

    public int hashCode() {
        try {
            throw new Exception("Need define key first!");
        } catch (Object -l_1_R) {
            -l_1_R.printStackTrace();
            return 0;
        }
    }

    public void readFrom(JceInputStream jceInputStream) {
        this.url = jceInputStream.readString(0, true);
        this.mainHarmId = jceInputStream.read(this.mainHarmId, 1, true);
        this.kZ = jceInputStream.read(this.kZ, 2, false);
        this.seq = jceInputStream.read(this.seq, 3, false);
        this.la = jceInputStream.readString(4, false);
        this.lb = jceInputStream.read(this.lb, 5, false);
        this.title = jceInputStream.readString(6, false);
        this.body = jceInputStream.readString(7, false);
        this.lc = jceInputStream.read(this.lc, 8, false);
    }

    public void setUrl(String str) {
        this.url = str;
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.url, 0);
        jceOutputStream.write(this.mainHarmId, 1);
        jceOutputStream.write(this.kZ, 2);
        jceOutputStream.write(this.seq, 3);
        if (this.la != null) {
            jceOutputStream.write(this.la, 4);
        }
        jceOutputStream.write(this.lb, 5);
        if (this.title != null) {
            jceOutputStream.write(this.title, 6);
        }
        if (this.body != null) {
            jceOutputStream.write(this.body, 7);
        }
        jceOutputStream.write(this.lc, 8);
    }
}
