package tmsdkobf;

import android.text.TextUtils;
import java.util.List;

public class qu implements Comparable<qu> {
    private static int Os = 0;
    public String MB;
    public String Ne;
    public int Nt;
    public int Nu;
    public List<Integer> Nw;
    public String Ol;
    public String Om;
    public String On;
    public String Oo;
    public List<String> Ot;
    public String Ou;
    public int Ov;
    public String Ow;
    public String Ox;
    public boolean Oy = false;
    public String mDescription;
    public String mFileName;
    public int mID;

    public qu() {
        int i = Os + 1;
        Os = i;
        this.mID = i;
    }

    public static void a(StringBuilder stringBuilder, qu quVar, boolean z, boolean -l_6_I) {
        for (String -l_5_R : quVar.Ot) {
            stringBuilder.append('0');
            stringBuilder.append(quVar.mID);
            stringBuilder.append(':');
            stringBuilder.append('7');
            stringBuilder.append(!-l_6_I ? '0' : '1');
            if (!TextUtils.isEmpty(-l_5_R)) {
                stringBuilder.append(':');
                stringBuilder.append('1');
                stringBuilder.append(-l_5_R);
            }
            if (!TextUtils.isEmpty(quVar.mFileName)) {
                stringBuilder.append(':');
                stringBuilder.append('2');
                stringBuilder.append(quVar.mFileName);
            }
            if (!TextUtils.isEmpty(quVar.Ol)) {
                stringBuilder.append(':');
                stringBuilder.append('3');
                stringBuilder.append(quVar.Ol);
            }
            if (!TextUtils.isEmpty(quVar.Om)) {
                stringBuilder.append(':');
                stringBuilder.append('4');
                stringBuilder.append(quVar.Om);
            }
            if (!TextUtils.isEmpty(quVar.On)) {
                stringBuilder.append(':');
                stringBuilder.append('5');
                stringBuilder.append(quVar.On);
            }
            if (!TextUtils.isEmpty(quVar.Oo)) {
                stringBuilder.append(':');
                stringBuilder.append('6');
                stringBuilder.append(quVar.Oo);
            }
            stringBuilder.append(';');
        }
    }

    public static void jL() {
        Os = 0;
    }

    public int a(qu quVar) {
        int -l_2_I = this.MB.compareTo(quVar.MB);
        if (-l_2_I != 0) {
            return -l_2_I;
        }
        -l_2_I = ((String) this.Ot.get(0)).compareTo((String) quVar.Ot.get(0));
        return -l_2_I == 0 ? this.mDescription.compareTo(quVar.mDescription) : -l_2_I;
    }

    public /* synthetic */ int compareTo(Object obj) {
        return a((qu) obj);
    }
}
