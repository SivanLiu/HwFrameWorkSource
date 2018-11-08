package tmsdkobf;

import java.util.LinkedList;

public class kx {
    private static kx xX;
    private LinkedList<a> xW = new LinkedList();

    public interface a {
        void dT();
    }

    public static kx dR() {
        if (xX == null) {
            Object -l_0_R = kx.class;
            synchronized (kx.class) {
                if (xX == null) {
                    xX = new kx();
                }
            }
        }
        return xX;
    }

    public void a(a aVar) {
        synchronized (this.xW) {
            this.xW.add(aVar);
        }
    }

    public void b(a aVar) {
        synchronized (this.xW) {
            this.xW.remove(aVar);
        }
    }

    public synchronized void dS() {
        try {
            LinkedList -l_1_R;
            synchronized (this.xW) {
                -l_1_R = (LinkedList) this.xW.clone();
            }
            if (-l_1_R != null) {
                kv.n("ccrManager", "copy.size() : " + -l_1_R.size());
                Object -l_2_R = -l_1_R.iterator();
                while (-l_2_R.hasNext()) {
                    a -l_3_R = (a) -l_2_R.next();
                    if (-l_3_R != null) {
                        -l_3_R.dT();
                    }
                }
            }
        } catch (Object -l_1_R2) {
            Object obj = -l_1_R2;
        }
    }
}
