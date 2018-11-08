package tmsdkobf;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;

public final class ew extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF;
    public String T = "";
    public int U = 0;
    public int kV = 0;
    public int kW = 0;
    public String title = "";
    public int type = 0;

    static {
        boolean z = false;
        if (!ew.class.desiredAssertionStatus()) {
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
        ew -l_2_R = (ew) obj;
        if (d.equals(this.title, -l_2_R.title) && d.equals(this.T, -l_2_R.T) && d.equals(this.type, -l_2_R.type) && d.equals(this.U, -l_2_R.U) && d.equals(this.kV, -l_2_R.kV) && d.equals(this.kW, -l_2_R.kW)) {
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
        this.title = jceInputStream.readString(0, true);
        this.T = jceInputStream.readString(1, true);
        this.type = jceInputStream.read(this.type, 2, true);
        this.U = jceInputStream.read(this.U, 3, true);
        this.kV = jceInputStream.read(this.kV, 4, false);
        this.kW = jceInputStream.read(this.kW, 5, false);
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.title, 0);
        jceOutputStream.write(this.T, 1);
        jceOutputStream.write(this.type, 2);
        jceOutputStream.write(this.U, 3);
        jceOutputStream.write(this.kV, 4);
        jceOutputStream.write(this.kW, 5);
    }
}
