package tmsdkobf;

import com.qq.taf.jce.JceDisplayer;
import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;
import java.util.ArrayList;

public final class ei extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF;
    static ArrayList<String> kk;
    public String ki = "";
    public ArrayList<String> kj = null;

    static {
        boolean z = false;
        if (!ei.class.desiredAssertionStatus()) {
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
        -l_3_R.display(this.ki, "typeName");
        -l_3_R.display(this.kj, "keySet");
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null) {
            return false;
        }
        ei -l_2_R = (ei) obj;
        if (d.equals(this.ki, -l_2_R.ki) && d.equals(this.kj, -l_2_R.kj)) {
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
        this.ki = jceInputStream.readString(0, true);
        if (kk == null) {
            kk = new ArrayList();
            kk.add("");
        }
        this.kj = (ArrayList) jceInputStream.read(kk, 1, false);
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.ki, 0);
        if (this.kj != null) {
            jceOutputStream.write(this.kj, 1);
        }
    }
}
