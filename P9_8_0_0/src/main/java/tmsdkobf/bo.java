package tmsdkobf;

import com.qq.taf.jce.JceDisplayer;
import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;

public final class bo extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF = (!bo.class.desiredAssertionStatus());
    static int da = 0;
    public int cZ = 0;

    public int a() {
        return this.cZ;
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
        new JceDisplayer(stringBuilder, i).display(this.cZ, "reqType");
    }

    public void displaySimple(StringBuilder stringBuilder, int i) {
        new JceDisplayer(stringBuilder, i).displaySimple(this.cZ, false);
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return d.equals(this.cZ, ((bo) obj).cZ);
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
        this.cZ = jceInputStream.read(this.cZ, 0, true);
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.cZ, 0);
    }
}
