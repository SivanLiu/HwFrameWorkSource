package tmsdkobf;

import java.util.ArrayList;
import java.util.HashMap;

public class oe {
    private static pe<Integer, oe> HP = new pe(50);
    public String HB = "";
    public String HC = "";
    public int HD = 0;
    public String HE = "";
    private String HF = "";
    public long HG = -1;
    public String HH = "";
    public String HI = "";
    public String HJ = "";
    public String HK = "";
    public boolean HL = false;
    public boolean HM = false;
    private long HN = 0;
    private long HO = 0;
    public int errorCode = 0;

    public static void a(oe oeVar, int i) {
        if (oeVar != null) {
            oeVar.HN = System.currentTimeMillis();
            HP.put(Integer.valueOf(i), oeVar);
        }
    }

    public static oe bC(int i) {
        oe -l_1_R = (oe) HP.get(Integer.valueOf(i));
        if (-l_1_R != null) {
            -l_1_R.HO = System.currentTimeMillis();
        }
        HP.f(Integer.valueOf(i));
        return -l_1_R;
    }

    private HashMap<String, String> gR() {
        Object -l_1_R = new HashMap();
        -l_1_R.put("B4", this.HE);
        -l_1_R.put("B20", this.HK);
        -l_1_R.put("B7", String.valueOf(this.errorCode));
        -l_1_R.put("B8", this.HH);
        -l_1_R.put("B10", this.HJ);
        -l_1_R.put("B9", this.HI);
        -l_1_R.put("B6", String.valueOf(this.HG));
        -l_1_R.put("B5", this.HF);
        -l_1_R.put("B3", this.HB);
        -l_1_R.put("B11", this.HC);
        -l_1_R.put("B12", String.valueOf(this.HD));
        -l_1_R.put("B21", String.valueOf(this.HL));
        -l_1_R.put("B22", String.valueOf(this.HM));
        return -l_1_R;
    }

    public void bB(int i) {
        this.HK += String.valueOf(i) + ";";
    }

    public void d(nl nlVar) {
        if (nlVar != null) {
            this.HF = "1";
            mb.n("TcpInfoUpload", toString());
            nlVar.a(gR());
        }
    }

    public void e(nl nlVar) {
    }

    public void f(nl nlVar) {
    }

    public void g(nl nlVar) {
    }

    public String toString() {
        Object -l_1_R = new StringBuilder();
        -l_1_R.append("|ip|" + this.HB);
        -l_1_R.append("|port|" + this.HC);
        -l_1_R.append("|tryTimes|" + this.HD);
        -l_1_R.append("|apn|" + this.HE);
        -l_1_R.append("|requestType|" + this.HF);
        -l_1_R.append("|requestTime|" + this.HG);
        -l_1_R.append("|errorCode|" + this.errorCode);
        -l_1_R.append("|cmdids|" + this.HK);
        -l_1_R.append("|iplist|" + this.HJ);
        -l_1_R.append("|lastRequest|" + this.HI);
        -l_1_R.append("|errorDetail|" + this.HH);
        -l_1_R.append("|isDetect|" + this.HL);
        -l_1_R.append("|isConnect|" + this.HM);
        return -l_1_R.toString();
    }

    public void u(ArrayList<String> arrayList) {
        if (arrayList != null && arrayList.size() > 0) {
            Object -l_2_R = new StringBuilder();
            Object -l_3_R = arrayList.iterator();
            while (-l_3_R.hasNext()) {
                String -l_4_R = (String) -l_3_R.next();
                if (-l_4_R != null) {
                    -l_2_R.append(-l_4_R);
                    -l_2_R.append(";");
                }
            }
        }
    }
}
