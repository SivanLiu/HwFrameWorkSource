package tmsdkobf;

public class nv {
    public static final void A(String str, String str2) {
        qg.c(65539, str + "|" + str2);
        mb.o(str, str2);
        y(str, str2);
    }

    public static final void a(String str, String str2, bw bwVar, ce ceVar) {
        mb.d(str, str2);
        qg.d(65539, str + "|" + str2);
    }

    public static final void a(String str, cf cfVar) {
        Object -l_2_R = new StringBuilder();
        -l_2_R.append("ServerShark seqNo|" + cfVar.ey + "|refSeqNo|" + cfVar.ez);
        if (cfVar.eQ != null && cfVar.eQ.size() > 0) {
            Object -l_3_R = cfVar.eQ.iterator();
            while (-l_3_R.hasNext()) {
                ce -l_4_R = (ce) -l_3_R.next();
                if (-l_4_R.ez == 0) {
                    -l_2_R.append(" || push cmd|" + -l_4_R.bz + "|seqNo|" + -l_4_R.ey + "|refSeqNo|" + -l_4_R.ez + "|retCode|" + -l_4_R.eB + "|dataRetCode|" + -l_4_R.eC + "|pushId|" + -l_4_R.eO.ex);
                } else {
                    -l_2_R.append(" || sashimi cmd|" + -l_4_R.bz + "|seqNo|" + -l_4_R.ey + "|refSeqNo|" + -l_4_R.ez + "|retCode|" + -l_4_R.eB + "|dataRetCode|" + -l_4_R.eC);
                }
            }
        }
        r(str, -l_2_R.toString());
    }

    public static final void a(String str, byte[] bArr) {
        try {
            a(str, nn.r(bArr));
        } catch (Object -l_2_R) {
            c(str, mb.getStackTraceString(-l_2_R), null, null);
        }
    }

    public static final void b(String str, String str2, bw bwVar, ce ceVar) {
        mb.n(str, str2);
        qg.b(65539, str + "|" + str2);
    }

    public static final void c(String str, String str2, bw bwVar, ce ceVar) {
        mb.o(str, str2);
        qg.c(65539, str + "|" + str2);
    }

    public static final void r(String str, String str2) {
        mb.r(str, str2);
        qg.a(65539, str + "|" + str2);
    }

    private static final void y(String str, String str2) {
    }

    public static final void z(String str, String str2) {
        qg.d(65539, str + "|" + str2);
        mb.d(str, str2);
        y(str, str2);
    }
}
