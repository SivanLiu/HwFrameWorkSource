package tmsdkobf;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.qq.taf.jce.d;

public final class fg extends JceStruct implements Cloneable {
    static final /* synthetic */ boolean bF;
    public int lF = 2;
    public String mb = "";
    public boolean mc = true;
    public String md = "";
    public String me = "";
    public int timestamp = 0;
    public String url = "";
    public int version = 0;

    static {
        boolean z = false;
        if (!fg.class.desiredAssertionStatus()) {
            z = true;
        }
        bF = z;
    }

    public fg() {
        x(this.mb);
        b(this.mc);
        setVersion(this.version);
        A(this.timestamp);
        setUrl(this.url);
        y(this.md);
        z(this.me);
        B(this.lF);
    }

    public void A(int i) {
        this.timestamp = i;
    }

    public void B(int i) {
        this.lF = i;
    }

    public void b(boolean z) {
        this.mc = z;
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
        fg -l_2_R = (fg) obj;
        if (d.equals(this.mb, -l_2_R.mb) && d.a(this.mc, -l_2_R.mc) && d.equals(this.version, -l_2_R.version) && d.equals(this.timestamp, -l_2_R.timestamp) && d.equals(this.url, -l_2_R.url) && d.equals(this.md, -l_2_R.md) && d.equals(this.me, -l_2_R.me) && d.equals(this.lF, -l_2_R.lF)) {
            z = true;
        }
        return z;
    }

    public boolean f() {
        return this.mc;
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
        x(jceInputStream.readString(0, true));
        b(jceInputStream.read(this.mc, 1, true));
        setVersion(jceInputStream.read(this.version, 2, true));
        A(jceInputStream.read(this.timestamp, 3, true));
        setUrl(jceInputStream.readString(4, true));
        y(jceInputStream.readString(5, true));
        z(jceInputStream.readString(6, true));
        B(jceInputStream.read(this.lF, 7, false));
    }

    public void setUrl(String str) {
        this.url = str;
    }

    public void setVersion(int i) {
        this.version = i;
    }

    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.mb, 0);
        jceOutputStream.write(this.mc, 1);
        jceOutputStream.write(this.version, 2);
        jceOutputStream.write(this.timestamp, 3);
        jceOutputStream.write(this.url, 4);
        jceOutputStream.write(this.md, 5);
        jceOutputStream.write(this.me, 6);
        jceOutputStream.write(this.lF, 7);
    }

    public void x(String str) {
        this.mb = str;
    }

    public void y(String str) {
        this.md = str;
    }

    public void z(String str) {
        this.me = str;
    }
}
