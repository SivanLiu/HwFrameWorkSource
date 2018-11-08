package tmsdkobf;

import android.content.Context;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Properties;
import tmsdk.common.utils.f;

final class iq {
    private static final HashMap<String, String> rN = new HashMap();
    private static final long rO = (new GregorianCalendar(2040, 0, 1).getTimeInMillis() / 1000);
    private Context mContext;
    private Properties rP;
    boolean rQ = false;

    static {
        rN.put("AresEngineManager", "aresengine");
        rN.put("QScannerManager", "qscanner");
        rN.put("LocationManager", "phoneservice");
        rN.put("IpDialManager", "phoneservice");
        rN.put("UsefulNumberManager", "phoneservice");
        rN.put("NetworkManager", "network");
        rN.put("TrafficCorrectionManager", "network");
        rN.put("FirewallManager", "network");
        rN.put("NetSettingManager", "netsetting");
        rN.put("OptimizeManager", "optimize");
        rN.put("UpdateManager", "update");
        rN.put("UrlCheckManager", "urlcheck");
        rN.put("PermissionManager", "permission");
        rN.put("SoftwareManager", "software");
        rN.put("AntitheftManager", "antitheft");
        rN.put("PowerSavingManager", "powersaving");
    }

    iq(Properties properties, Context context) {
        this.rP = properties;
        this.mContext = context;
    }

    private String aH(String str) {
        Object -l_3_R = null;
        try {
            -l_3_R = this.mContext.getPackageManager().getPackageInfo(str, 64);
        } catch (Object -l_4_R) {
            -l_4_R.printStackTrace();
        }
        Object -l_4_R2 = null;
        if (-l_3_R != null) {
            Object -l_6_R = new ByteArrayInputStream(-l_3_R.signatures[0].toByteArray());
            try {
                -l_4_R2 = mc.n(((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(-l_6_R)).getEncoded());
                -l_6_R.close();
            } catch (Object -l_7_R) {
                -l_7_R.printStackTrace();
            } catch (Object -l_7_R2) {
                -l_7_R2.printStackTrace();
            }
        }
        return -l_4_R2;
    }

    public String bQ() {
        return this.rP.getProperty("lc_sdk_channel");
    }

    public boolean bS() {
        if (this.rQ) {
            return true;
        }
        Object -l_2_R = aH(this.mContext.getPackageName());
        if (-l_2_R == null) {
            return true;
        }
        Object -l_3_R = this.rP.getProperty("signature").toUpperCase().trim();
        this.rQ = -l_2_R.equals(-l_3_R);
        if (!this.rQ) {
            f.f("DEBUG", "your    signature is " + -l_2_R + " len:" + -l_2_R.length());
            f.f("DEBUG", "licence signature is " + -l_3_R + " len:" + -l_3_R.length());
        }
        return this.rQ;
    }

    public long bT() {
        return Long.parseLong(this.rP.getProperty("expiry.seconds", Long.toString(rO)));
    }
}
