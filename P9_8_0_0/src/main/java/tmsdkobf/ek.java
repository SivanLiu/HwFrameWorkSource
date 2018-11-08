package tmsdkobf;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;

public final class ek extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF;
    public int dt = ee.jE.value();
    public int kn = 0;

    static {
        boolean z = false;
        if (!ek.class.desiredAssertionStatus()) {
            z = true;
        }
        bF = z;
    }

    public ek() {
        f(this.kn);
        g(this.dt);
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
        ek -l_2_R = (ek) obj;
        if (d.equals(this.kn, -l_2_R.kn) && d.equals(this.dt, -l_2_R.dt)) {
            z = true;
        }
        return z;
    }

    public void f(int i) {
        this.kn = i;
    }

    public void g(int i) {
        this.dt = i;
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
        f(jceInputStream.read(this.kn, 0, true));
        g(jceInputStream.read(this.dt, 1, false));
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.kn, 0);
        jceOutputStream.write(this.dt, 1);
    }
}
