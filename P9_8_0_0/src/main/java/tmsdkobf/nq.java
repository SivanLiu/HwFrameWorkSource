package tmsdkobf;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.qq.taf.jce.JceStruct;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import javax.crypto.Cipher;

public class nq {
    private b DS;
    private nw Dm;
    private Object mLock = new Object();

    public interface a {
        void a(int i, int i2, int i3);
    }

    public static class b {
        public volatile String DW = "";
        public volatile String DX = "";

        public String toString() {
            Object -l_1_R = new StringBuilder();
            -l_1_R.append("mSessionId: ");
            -l_1_R.append(this.DW);
            -l_1_R.append(" mEncodeKey: ");
            -l_1_R.append(this.DX);
            return -l_1_R.toString();
        }
    }

    public nq(Context context, nw nwVar) {
        this.Dm = nwVar;
        this.DS = new b();
        load();
    }

    static void a(Context context, int i, b bVar) {
        try {
            Object -l_4_R = new Intent(String.format("action.rsa.got:%s", new Object[]{context.getPackageName()}));
            -l_4_R.setPackage(context.getPackageName());
            -l_4_R.putExtra("k.rc", i);
            if (i == 0 && bVar != null) {
                -l_4_R.putExtra("k.r.k", bVar.DX);
                -l_4_R.putExtra("k.r.s", bVar.DW);
            }
            context.sendBroadcast(-l_4_R);
        } catch (Object -l_3_R) {
            mb.b("RsaKeyCertifier", "[rsa_key]sendBroadcast(): " + -l_3_R, -l_3_R);
        }
    }

    private String bo(int i) {
        Object -l_2_R = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Object -l_3_R = new SecureRandom();
        Object -l_4_R = new StringBuffer();
        for (int -l_5_I = 0; -l_5_I < i; -l_5_I++) {
            -l_4_R.append(-l_2_R.charAt(-l_3_R.nextInt(62)));
        }
        return -l_4_R.toString();
    }

    private byte[] cg(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        Object -l_2_R = null;
        try {
            Object -l_4_R = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(tmsdk.common.utils.b.decode("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDb49jFnNqMDLdl87UtY5jOMqqdMuvQg65Zuva3Qm1tORQGBuM04u7fqygA64XbOx9e/KPNkDNDmqS8SlsAPL1fV2lqM/phgV0NY62TJqSR+PLngwJd2rhYR8wQ1N0JE+R59a5c08EGsd6axStjHsVu2+evCf/SWU9Y/oQpEtOjGwIDAQAB", 0)));
            Object -l_5_R = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            -l_5_R.init(1, -l_4_R);
            -l_2_R = -l_5_R.doFinal(str.getBytes());
        } catch (Object -l_3_R) {
            -l_3_R.printStackTrace();
        }
        return -l_2_R;
    }

    private void load() {
        Object -l_1_R = this.Dm.gl().aE();
        if (-l_1_R == null) {
            mb.s("RsaKeyCertifier", "[rsa_key]load(), no record!");
            return;
        }
        synchronized (this.mLock) {
            this.DS.DX = -l_1_R.DX;
            this.DS.DW = -l_1_R.DW;
            mb.n("RsaKeyCertifier", "[rsa_key]load(), mEncodeKey: " + this.DS.DX + " mSessionId: " + this.DS.DW);
        }
    }

    private void x(String str, String str2) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            mb.o("RsaKeyCertifier", "[rsa_key] saveRsaKey(), argument is null! encodeKey: " + str + " sessionId: " + str2);
            return;
        }
        synchronized (this.mLock) {
            this.DS.DX = str;
            this.DS.DW = str2;
            this.Dm.gl().b(this.DS);
        }
    }

    static void y(Context context) {
        try {
            mb.n("RsaKeyCertifier", "[rsa_key]requestSendProcessUpdateRsaKey()");
            Object -l_2_R = new Intent(String.format("action.up.rsa:%s", new Object[]{context.getPackageName()}));
            -l_2_R.setPackage(context.getPackageName());
            context.sendBroadcast(-l_2_R);
        } catch (Object -l_1_R) {
            mb.b("RsaKeyCertifier", "[rsa_key]requestSendProcessUpdateRsaKey(): " + -l_1_R, -l_1_R);
        }
    }

    public void a(a aVar) {
        mb.d("RsaKeyCertifier", "[rsa_key]updateRsaKey()");
        final int -l_2_I = ns.fW().fP();
        Object -l_4_R = bo(16);
        Object -l_5_R = cg(-l_4_R);
        if (-l_5_R != null) {
            JceStruct -l_6_R = new aw();
            -l_6_R.cf = -l_5_R;
            mb.n("RsaKeyCertifier", "[rsa_key]updateRsaKey() reqRSA.enc: " + com.tencent.tcuser.util.a.bytesToHexString(-l_6_R.cf));
            ArrayList -l_7_R = new ArrayList();
            final bw -l_8_R = new bw();
            -l_8_R.ey = -l_2_I;
            -l_8_R.bz = 152;
            -l_8_R.eE |= 2;
            -l_8_R.data = nh.a(null, -l_6_R, -l_8_R.bz, -l_8_R);
            -l_7_R.add(-l_8_R);
            nt.ga().a(-l_8_R.ey, -1, null);
            final a aVar2 = aVar;
            this.Dm.a(-l_7_R, new nr(this, -l_4_R) {
                final /* synthetic */ nq DV;

                public void a(boolean z, int i, int i2, ArrayList<ce> arrayList) {
                    mb.n("RsaKeyCertifier", "[rsa_key]updateRsaKey(), isTcpChannel: " + z + ", seqNo " + -l_8_R.ey + ", retCode: " + i);
                    if (i != 0) {
                        mb.o("RsaKeyCertifier", "[rsa_key]updateRsaKey(), retCode: " + i);
                        aVar2.a(-l_2_I, 152, i);
                    } else if (arrayList == null) {
                        mb.o("RsaKeyCertifier", "[rsa_key]updateRsaKey(), null == serverSashimis");
                        aVar2.a(-l_2_I, 152, -21250000);
                    } else if (arrayList.size() > 0) {
                        ce -l_5_R = (ce) arrayList.get(0);
                        if (-l_5_R == null) {
                            mb.o("RsaKeyCertifier", "[rsa_key]updateRsaKey(), serverSashimi is null");
                            aVar2.a(-l_2_I, 152, -21250000);
                        } else if (-l_5_R.eB != 0) {
                            mb.o("RsaKeyCertifier", "[rsa_key]updateRsaKey(), mazu error: " + -l_5_R.eB);
                            aVar2.a(-l_2_I, 152, -l_5_R.eB);
                        } else if (-l_5_R.eC != 0) {
                            mb.o("RsaKeyCertifier", "[rsa_key]updateRsaKey(), rs.dataRetCode: " + -l_5_R.eC);
                            aVar2.a(-l_2_I, 152, -21300000);
                        } else if (-l_5_R.data != null) {
                            try {
                                Object -l_6_R = nh.a(null, this.DY.getBytes(), -l_5_R.data, new ax(), false, -l_5_R.eE);
                                if (-l_6_R != null) {
                                    ax -l_8_R = (ax) -l_6_R;
                                    if (TextUtils.isEmpty(-l_8_R.K) == 0) {
                                        this.DV.x(this.DY, -l_8_R.K);
                                        mb.n("RsaKeyCertifier", "[rsa_key]updateRsaKey(), encodeKey: " + this.DY + " sessionId: " + -l_8_R.K);
                                        aVar2.a(-l_2_I, 152, 0);
                                        return;
                                    }
                                    mb.o("RsaKeyCertifier", "[rsa_key]updateRsaKey(), ret.sessionId is null");
                                    aVar2.a(-l_2_I, 152, -21280000);
                                    return;
                                }
                                mb.o("RsaKeyCertifier", "[rsa_key]updateRsaKey(), decode jce failed: null == js");
                                aVar2.a(-l_2_I, 152, -21000400);
                            } catch (Object -l_8_R2) {
                                mb.o("RsaKeyCertifier", "[rsa_key]updateRsaKey(), decode jce exception: " + -l_8_R2);
                                aVar2.a(-l_2_I, 152, -21000400);
                            }
                        } else {
                            mb.o("RsaKeyCertifier", "[rsa_key]updateRsaKey(), null == rs.data");
                            aVar2.a(-l_2_I, 152, -21000005);
                        }
                    } else {
                        mb.o("RsaKeyCertifier", "[rsa_key]updateRsaKey(), serverSashimis.size() <= 0");
                        aVar2.a(-l_2_I, 152, -21250000);
                    }
                }
            });
            return;
        }
        mb.o("RsaKeyCertifier", "[rsa_key]updateRsaKey(), gen dynamic key failed");
        aVar.a(-l_2_I, 152, -20001000);
    }

    public b ap() {
        Object -l_1_R = new b();
        synchronized (this.mLock) {
            -l_1_R.DW = this.DS.DW;
            -l_1_R.DX = this.DS.DX;
        }
        return -l_1_R;
    }

    public void refresh() {
        mb.n("RsaKeyCertifier", "refresh()");
        load();
    }
}
