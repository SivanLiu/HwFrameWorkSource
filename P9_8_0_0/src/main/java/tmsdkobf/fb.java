package tmsdkobf;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;

public final class fb extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF;
    public int fP = 0;
    public int time = 0;

    static {
        boolean z = false;
        if (!fb.class.desiredAssertionStatus()) {
            z = true;
        }
        bF = z;
    }

    public fb() {
        z(this.fP);
        w(this.time);
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
        fb -l_2_R = (fb) obj;
        if (d.equals(this.fP, -l_2_R.fP) && d.equals(this.time, -l_2_R.time)) {
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
        z(jceInputStream.read(this.fP, 0, true));
        w(jceInputStream.read(this.time, 1, true));
    }

    public void w(int i) {
        this.time = i;
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.fP, 0);
        jceOutputStream.write(this.time, 1);
    }

    public void z(int i) {
        this.fP = i;
    }
}
