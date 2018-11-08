package tmsdkobf;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;

public final class fe extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF;
    public short lG = (short) 0;
    public String lH = "";

    static {
        boolean z = false;
        if (!fe.class.desiredAssertionStatus()) {
            z = true;
        }
        bF = z;
    }

    public fe() {
        a(this.lG);
        w(this.lH);
    }

    public void a(short s) {
        this.lG = (short) s;
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
        fe -l_2_R = (fe) obj;
        if (d.a(this.lG, -l_2_R.lG) && d.equals(this.lH, -l_2_R.lH)) {
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
        a(jceInputStream.read(this.lG, 0, true));
        w(jceInputStream.readString(1, true));
    }

    public void w(String str) {
        this.lH = str;
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.lG, 0);
        jceOutputStream.write(this.lH, 1);
    }
}
