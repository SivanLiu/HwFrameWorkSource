package tmsdkobf;

import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import com.qq.taf.jce.JceDisplayer;
import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;

public final class ey extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF;
    public String I = "";
    public String kX = "";
    public int kY = 28;
    public int seq = 0;
    public String url = "";
    public int version = 0;

    static {
        boolean z = false;
        if (!ey.class.desiredAssertionStatus()) {
            z = true;
        }
        bF = z;
    }

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

    public void display(StringBuilder stringBuilder, int i) {
        Object -l_3_R = new JceDisplayer(stringBuilder, i);
        -l_3_R.display(this.url, CheckVersionField.CHECK_VERSION_SERVER_URL);
        -l_3_R.display(this.kX, "ext");
        -l_3_R.display(this.seq, "seq");
        -l_3_R.display(this.version, "version");
        -l_3_R.display(this.I, "guid");
        -l_3_R.display(this.kY, "appId");
    }

    public void displaySimple(StringBuilder stringBuilder, int i) {
        Object -l_3_R = new JceDisplayer(stringBuilder, i);
        -l_3_R.displaySimple(this.url, true);
        -l_3_R.displaySimple(this.kX, true);
        -l_3_R.displaySimple(this.seq, true);
        -l_3_R.displaySimple(this.version, true);
        -l_3_R.displaySimple(this.I, true);
        -l_3_R.displaySimple(this.kY, false);
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null) {
            return false;
        }
        ey -l_2_R = (ey) obj;
        if (d.equals(this.url, -l_2_R.url) && d.equals(this.kX, -l_2_R.kX) && d.equals(this.seq, -l_2_R.seq) && d.equals(this.version, -l_2_R.version) && d.equals(this.I, -l_2_R.I) && d.equals(this.kY, -l_2_R.kY)) {
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
        this.kX = jceInputStream.readString(1, false);
        this.seq = jceInputStream.read(this.seq, 2, false);
        this.version = jceInputStream.read(this.version, 3, false);
        this.I = jceInputStream.readString(4, false);
        this.kY = jceInputStream.read(this.kY, 5, false);
    }

    public void setUrl(String str) {
        this.url = str;
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.url, 0);
        if (this.kX != null) {
            jceOutputStream.write(this.kX, 1);
        }
        jceOutputStream.write(this.seq, 2);
        jceOutputStream.write(this.version, 3);
        if (this.I != null) {
            jceOutputStream.write(this.I, 4);
        }
        jceOutputStream.write(this.kY, 5);
    }
}
