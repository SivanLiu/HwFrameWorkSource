package tmsdkobf;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;

public final class dx extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF;
    static byte[] il;
    public int ii = 0;
    public byte[] ij = null;
    public int ik = 0;

    static {
        boolean z = false;
        if (!dx.class.desiredAssertionStatus()) {
            z = true;
        }
        bF = z;
    }

    public dx() {
        c(this.ii);
        a(this.ij);
        d(this.ik);
    }

    public void a(byte[] bArr) {
        this.ij = bArr;
    }

    public void c(int i) {
        this.ii = i;
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

    public void d(int i) {
        this.ik = i;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null) {
            return false;
        }
        dx -l_2_R = (dx) obj;
        if (d.equals(this.ii, -l_2_R.ii) && d.equals(this.ij, -l_2_R.ij) && d.equals(this.ik, -l_2_R.ik)) {
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
        c(jceInputStream.read(this.ii, 0, true));
        if (il == null) {
            il = new byte[1];
            il[0] = (byte) 0;
        }
        a(jceInputStream.read(il, 1, true));
        d(jceInputStream.read(this.ik, 2, true));
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.ii, 0);
        jceOutputStream.write(this.ij, 1);
        jceOutputStream.write(this.ik, 2);
    }
}
