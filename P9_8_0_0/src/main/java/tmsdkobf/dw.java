package tmsdkobf;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;

public final class dw extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF;
    public long ig = 0;
    public String ih = "";
    public int state = 0;
    public float weight = 0.0f;

    static {
        boolean z = false;
        if (!dw.class.desiredAssertionStatus()) {
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

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null) {
            return false;
        }
        dw -l_2_R = (dw) obj;
        if (d.a(this.ig, -l_2_R.ig) && d.equals(this.weight, -l_2_R.weight) && d.equals(this.ih, -l_2_R.ih) && d.equals(this.state, -l_2_R.state)) {
            z = true;
        }
        return z;
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
        this.ig = jceInputStream.read(this.ig, 0, true);
        this.weight = jceInputStream.read(this.weight, 1, true);
        this.ih = jceInputStream.readString(2, true);
        this.state = jceInputStream.read(this.state, 3, false);
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.ig, 0);
        jceOutputStream.write(this.weight, 1);
        jceOutputStream.write(this.ih, 2);
        jceOutputStream.write(this.state, 3);
    }
}
