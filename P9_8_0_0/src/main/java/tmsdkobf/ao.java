package tmsdkobf;

import com.qq.taf.jce.JceDisplayer;
import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;
import java.util.ArrayList;

public final class ao extends JceStruct implements Cloneable {
    static ArrayList<ap> bE = new ArrayList();
    static final /* synthetic */ boolean bF;
    public int bC = 0;
    public ArrayList<ap> bD = null;

    static {
        boolean z = false;
        if (!ao.class.desiredAssertionStatus()) {
            z = true;
        }
        bF = z;
        bE.add(new ap());
    }

    public ao(int i, ArrayList<ap> arrayList) {
        this.bC = i;
        this.bD = arrayList;
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
        -l_3_R.display(this.bC, "reportID");
        -l_3_R.display(this.bD, "vecReportInfo");
    }

    public void displaySimple(StringBuilder stringBuilder, int i) {
        Object -l_3_R = new JceDisplayer(stringBuilder, i);
        -l_3_R.displaySimple(this.bC, true);
        -l_3_R.displaySimple(this.bD, false);
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null) {
            return false;
        }
        ao -l_2_R = (ao) obj;
        if (d.equals(this.bC, -l_2_R.bC) && d.equals(this.bD, -l_2_R.bD)) {
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
        this.bC = jceInputStream.read(this.bC, 0, true);
        this.bD = (ArrayList) jceInputStream.read(bE, 1, true);
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.bC, 0);
        jceOutputStream.write(this.bD, 1);
    }
}
