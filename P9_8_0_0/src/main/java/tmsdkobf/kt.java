package tmsdkobf;

import android.content.Context;
import java.util.ArrayList;
import java.util.Map.Entry;
import tmsdk.common.TMSDKContext;

public final class kt {
    static kt xL;
    kf nX = ((kf) fj.D(9));

    kt(Context context) {
    }

    public static void aE(int i) {
        dE().r(i, 0);
    }

    public static void aF(int i) {
        dE().dG().remove(aG(i));
    }

    static String aG(int i) {
        return "" + i;
    }

    public static void aH(int i) {
        dE().dF().remove(aG(i));
    }

    public static void aI(int i) {
        dE().dH().remove(aG(i));
    }

    public static kt dE() {
        if (xL == null) {
            Object -l_0_R = kt.class;
            synchronized (kt.class) {
                if (xL == null) {
                    xL = new kt(TMSDKContext.getApplicaionContext());
                }
            }
        }
        return xL;
    }

    public static void e(int i, String str) {
        dE().a(dE().dH(), i, str, false);
    }

    public static void f(int i, String str) {
        dE().a(dE().dH(), i, str, true);
    }

    public static String o(ArrayList<ks> arrayList) {
        Object -l_1_R = new StringBuffer();
        for (int -l_2_I = 0; -l_2_I < arrayList.size(); -l_2_I++) {
            -l_1_R.append(((ks) arrayList.get(-l_2_I)).xI);
            -l_1_R.append("&");
            -l_1_R.append(((ks) arrayList.get(-l_2_I)).xH);
            if (((ks) arrayList.get(-l_2_I)).errorCode != 0) {
                -l_1_R.append("&");
                -l_1_R.append(((ks) arrayList.get(-l_2_I)).errorCode);
            }
            -l_1_R.append(";");
        }
        return -l_1_R.toString();
    }

    public static void saveActionData(int i) {
        dE().q(i, 0);
    }

    public static void saveMultiValueData(int i, int i2) {
        dE().s(i, i2);
    }

    ArrayList<b> a(jx jxVar) {
        Object -l_2_R = new ArrayList();
        Object -l_3_R = jxVar.getAll();
        if (-l_3_R != null) {
            for (Entry -l_5_R : -l_3_R.entrySet()) {
                try {
                    String -l_6_R = (String) -l_5_R.getKey();
                    Object -l_7_R = -l_5_R.getValue();
                    if (-l_7_R != null && (-l_7_R instanceof String)) {
                        String -l_8_R = (String) -l_7_R;
                        if (-l_8_R.indexOf("$") > 0) {
                            int -l_9_I = Integer.valueOf(-l_6_R).intValue();
                            Object -l_10_R = -l_8_R.split("\\$");
                            if (-l_10_R != null && -l_10_R.length > 0) {
                                Object -l_11_R = -l_10_R;
                                for (Object -l_14_R : -l_10_R) {
                                    Object -l_15_R = -l_14_R.split("\\|");
                                    if (-l_15_R != null && -l_15_R.length == 2) {
                                        long -l_16_J = Long.valueOf(-l_15_R[0]).longValue();
                                        Object -l_18_R = -l_15_R[1];
                                        Object -l_19_R = new b();
                                        -l_19_R.c = -l_9_I;
                                        -l_19_R.timestamp = (int) (-l_16_J / 1000);
                                        -l_19_R.e = new ArrayList();
                                        -l_19_R.e.add(-l_18_R);
                                        if (-l_19_R != null) {
                                            -l_2_R.add(-l_19_R);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        return -l_2_R;
    }

    ArrayList<b> a(jx jxVar, String str) {
        if (jxVar == null) {
            return null;
        }
        Object -l_3_R = new ArrayList();
        Object -l_4_R = null;
        try {
            -l_4_R = jxVar.getAll();
        } catch (Exception e) {
        }
        if (-l_4_R != null) {
            for (Entry -l_6_R : -l_4_R.entrySet()) {
                try {
                    String -l_7_R = (String) -l_6_R.getKey();
                    Object -l_8_R = -l_6_R.getValue();
                    if (-l_8_R instanceof String) {
                        -l_3_R.addAll(g(Integer.valueOf(-l_7_R).intValue(), (String) -l_8_R));
                    }
                } catch (Exception e2) {
                }
            }
        }
        return p(-l_3_R);
    }

    void a(final jx jxVar, final int i, final int i2) {
        im.bJ().addTask(new Runnable(this) {
            final /* synthetic */ kt xO;

            public void run() {
                jxVar.putString(kt.aG(i), String.valueOf(i2));
            }
        }, "doxxx");
    }

    void a(jx jxVar, int i, String str, boolean z) {
        final String str2 = str;
        final int i2 = i;
        final jx jxVar2 = jxVar;
        final boolean z2 = z;
        im.bJ().addTask(new Runnable(this) {
            final /* synthetic */ kt xO;

            public void run() {
                if (str2 != null && str2.length() > 0) {
                    Object -l_1_R = kt.aG(i2);
                    Object -l_2_R = jxVar2.getString(-l_1_R, null);
                    if (-l_2_R == null || z2) {
                        -l_2_R = "";
                    }
                    Object -l_3_R = new StringBuilder();
                    -l_3_R.append(-l_2_R);
                    -l_3_R.append(System.currentTimeMillis());
                    -l_3_R.append("|");
                    -l_3_R.append(str2);
                    -l_3_R.append("$");
                    if (-l_3_R.length() <= 16384) {
                        jxVar2.putString(-l_1_R, -l_3_R.toString());
                    }
                }
            }
        }, "doxxx");
    }

    ArrayList<b> b(jx jxVar, String str) {
        if (jxVar == null) {
            return null;
        }
        Object -l_3_R = new ArrayList();
        Object -l_4_R = jxVar.getAll();
        if (-l_4_R != null) {
            for (Entry -l_6_R : -l_4_R.entrySet()) {
                try {
                    String -l_7_R = (String) -l_6_R.getKey();
                    Object -l_8_R = -l_6_R.getValue();
                    if (-l_8_R instanceof String) {
                        int -l_9_I = Integer.valueOf(-l_7_R).intValue();
                        kv.n("ccrService", "id: " + -l_9_I + " | " + ((String) -l_8_R));
                        Object -l_10_R = h(-l_9_I, (String) -l_8_R);
                        if (-l_10_R != null) {
                            -l_3_R.add(-l_10_R);
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        return -l_3_R;
    }

    ArrayList<Integer> bs(String str) {
        if (str == null) {
            return null;
        }
        Object -l_2_R = new ArrayList();
        Object -l_3_R = str.split("\\|");
        Object -l_4_R = -l_3_R;
        try {
            int -l_5_I = -l_3_R.length;
            for (int -l_6_I = 0; -l_6_I < -l_5_I; -l_6_I++) {
                -l_2_R.add(Integer.valueOf(-l_4_R[-l_6_I]));
            }
        } catch (NumberFormatException e) {
        }
        return -l_2_R;
    }

    jx dF() {
        return this.nX.getPreferenceService("actionStats");
    }

    jx dG() {
        return this.nX.getPreferenceService("mulDataStats");
    }

    jx dH() {
        return this.nX.getPreferenceService("stringStats");
    }

    public void dI() {
        dF().clear();
    }

    public void dJ() {
        dG().clear();
    }

    public void dK() {
        dH().clear();
    }

    public ArrayList<b> dL() {
        return a(dF(), "Action");
    }

    public ArrayList<b> dM() {
        return b(dG(), "MultiValue");
    }

    public ArrayList<b> dN() {
        return a(dE().dH());
    }

    ArrayList<ks> g(int i, String str) {
        Object -l_3_R = new ArrayList();
        if (str == null) {
            return -l_3_R;
        }
        while (true) {
            try {
                int -l_4_I = str.indexOf(";");
                if (-l_4_I != -1) {
                    Object -l_5_R = new ks();
                    Object -l_6_R = str.substring(0, -l_4_I);
                    if (-l_6_R.indexOf("&") != -1) {
                        -l_5_R.xG = i;
                        -l_5_R.xI = Long.parseLong(-l_6_R.substring(0, -l_6_R.indexOf("&")));
                        if (-l_5_R.xI == 0) {
                            -l_5_R.xI = System.currentTimeMillis();
                        }
                        -l_6_R = -l_6_R.substring(-l_6_R.indexOf("&") + 1);
                        if (-l_6_R.indexOf("&") == -1) {
                            -l_5_R.xH = Integer.parseInt(-l_6_R);
                        } else {
                            -l_5_R.xH = Integer.parseInt(-l_6_R.substring(0, -l_6_R.indexOf("&")));
                            -l_5_R.errorCode = Integer.parseInt(-l_6_R.substring(-l_6_R.indexOf("&") + 1));
                        }
                        -l_3_R.add(-l_5_R);
                    }
                    if (-l_4_I == str.length()) {
                        break;
                    }
                    str = str.substring(-l_4_I + 1);
                } else {
                    break;
                }
            } catch (Exception e) {
            }
        }
        return -l_3_R;
    }

    b h(int i, String str) {
        Object -l_3_R = new b();
        -l_3_R.c = i;
        -l_3_R.d = bs(str);
        -l_3_R.timestamp = (int) (System.currentTimeMillis() / 1000);
        return -l_3_R;
    }

    ArrayList<b> p(ArrayList<ks> arrayList) {
        if (arrayList == null) {
            return null;
        }
        Object -l_2_R = new ArrayList();
        Object -l_3_R = arrayList.iterator();
        while (-l_3_R.hasNext()) {
            ks -l_4_R = (ks) -l_3_R.next();
            if (-l_4_R.xH > 0) {
                Object -l_5_R = new b();
                -l_5_R.c = -l_4_R.xG;
                -l_5_R.timestamp = (int) (-l_4_R.xI / 1000);
                -l_5_R.count = -l_4_R.xH;
                if (-l_4_R.errorCode != 0) {
                    -l_5_R.d = new ArrayList();
                    -l_5_R.d.add(Integer.valueOf(-l_4_R.errorCode));
                }
                -l_2_R.add(-l_5_R);
            }
        }
        return -l_2_R;
    }

    void q(final int i, final int i2) {
        im.bJ().addTask(new Runnable(this) {
            final /* synthetic */ kt xO;

            public void run() {
                Object -l_1_R = kt.aG(i);
                long -l_2_J = System.currentTimeMillis();
                Object -l_4_R = this.xO.dF().getString(-l_1_R, null);
                if (-l_4_R == null || -l_4_R.length() <= 8192) {
                    Object -l_5_R = new ArrayList();
                    Object -l_6_R = new ks();
                    -l_6_R.xG = i;
                    -l_6_R.xI = -l_2_J;
                    -l_6_R.xH = 1;
                    -l_6_R.errorCode = i2;
                    -l_5_R.add(-l_6_R);
                    Object -l_7_R = kt.o(-l_5_R);
                    if (-l_7_R != null) {
                        Object -l_8_R = new StringBuilder();
                        if (-l_4_R != null) {
                            -l_8_R.append(-l_4_R);
                        }
                        -l_8_R.append(-l_7_R);
                        this.xO.dF().putString(-l_1_R, -l_8_R.toString());
                    }
                }
            }
        }, "doxxx");
    }

    void r(final int i, final int i2) {
        im.bJ().addTask(new Runnable(this) {
            final /* synthetic */ kt xO;

            public void run() {
                Object -l_1_R = kt.aG(i);
                long -l_2_J = System.currentTimeMillis();
                Object -l_4_R = new ArrayList();
                Object -l_5_R = new ks();
                -l_5_R.xG = i;
                -l_5_R.xI = -l_2_J;
                -l_5_R.xH = 1;
                -l_5_R.errorCode = i2;
                -l_4_R.add(-l_5_R);
                Object -l_6_R = kt.o(-l_4_R);
                if (-l_6_R != null) {
                    Object -l_7_R = new StringBuilder();
                    -l_7_R.append(-l_6_R);
                    this.xO.dF().putString(-l_1_R, -l_7_R.toString());
                }
            }
        }, "doxxx");
    }

    void s(int i, int i2) {
        a(dG(), i, i2);
    }
}
