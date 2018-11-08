package tmsdkobf;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;
import java.util.ArrayList;

public final class fw extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF;
    static ArrayList<fu> iD;
    public ArrayList<fu> iC = null;
    public String nx = "";

    static {
        boolean z = false;
        if (!fw.class.desiredAssertionStatus()) {
            z = true;
        }
        bF = z;
    }

    public fw() {
        V(this.nx);
        g(this.iC);
    }

    public ArrayList<fu> I() {
        return this.iC;
    }

    public void V(String str) {
        this.nx = str;
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
        fw -l_2_R = (fw) obj;
        if (d.equals(this.nx, -l_2_R.nx) && d.equals(this.iC, -l_2_R.iC)) {
            z = true;
        }
        return z;
    }

    public void g(ArrayList<fu> arrayList) {
        this.iC = arrayList;
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
        V(jceInputStream.readString(0, true));
        if (iD == null) {
            iD = new ArrayList();
            iD.add(new fu());
        }
        this.iC = (ArrayList) jceInputStream.read(iD, 1, true);
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.nx, 0);
        jceOutputStream.write(this.iC, 1);
    }
}
