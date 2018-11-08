package tmsdkobf;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.util.DisplayMetrics;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.BaseManagerC;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.f;
import tmsdk.common.utils.v;

public final class oy extends BaseManagerC {
    private v Jg;
    private ow Jh;
    private CertificateFactory Ji = null;
    private Context mContext = null;
    private PackageManager mPackageManager = null;

    private static Certificate a(CertificateFactory certificateFactory, Signature signature) {
        Object -l_2_R = new ByteArrayInputStream(signature.toByteArray());
        Object -l_3_R = null;
        try {
            -l_3_R = (X509Certificate) certificateFactory.generateCertificate(-l_2_R);
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (Object -l_4_R) {
                    -l_4_R.printStackTrace();
                }
            }
        } catch (Object -l_4_R2) {
            -l_4_R2.printStackTrace();
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (Object -l_4_R22) {
                    -l_4_R22.printStackTrace();
                }
            }
        } catch (Object -l_4_R222) {
            -l_4_R222.printStackTrace();
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (Object -l_4_R2222) {
                    -l_4_R2222.printStackTrace();
                }
            }
        } catch (Throwable th) {
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (Object -l_6_R) {
                    -l_6_R.printStackTrace();
                }
            }
        }
        return -l_3_R;
    }

    private ov a(String str, ov ovVar, int i) {
        Object -l_5_R;
        PackageInfo -l_4_R = null;
        try {
            -l_5_R = oy.class;
            synchronized (oy.class) {
                -l_4_R = this.mPackageManager.getPackageArchiveInfo(str, bF(i));
                if (-l_4_R == null) {
                    return null;
                }
                if ((i & 128) != 0) {
                    ovVar.put("pkgName", -l_4_R.packageName);
                }
                if ((i & 256) != 0) {
                    ovVar.put("version", -l_4_R.versionName);
                }
                if ((i & 512) != 0) {
                    ovVar.put("versionCode", Integer.valueOf(-l_4_R.versionCode));
                }
                if ((i & 32) != 0) {
                    ovVar.put("permissions", -l_4_R.requestedPermissions);
                }
                if ((i & 2048) != 0) {
                    ovVar.setAppName(this.mPackageManager.getApplicationLabel(-l_4_R.applicationInfo).toString());
                }
                if (!((i & IncomingSmsFilterConsts.PAY_SMS) == 0 || -l_4_R.applicationInfo == null)) {
                    ovVar.put("uid", Integer.valueOf(-l_4_R.applicationInfo.uid));
                }
                return ovVar;
            }
        } catch (Object -l_5_R2) {
            -l_5_R2.printStackTrace();
        }
    }

    private static boolean a(Context -l_2_R, Drawable drawable) {
        try {
            Context -l_2_R2 = TMSDKContext.getCurrentContext();
            if (-l_2_R2 != null) {
                -l_2_R = -l_2_R2;
            }
            float -l_3_F = -l_2_R.getResources().getDisplayMetrics().density;
            int -l_4_I = (int) (((float) drawable.getIntrinsicWidth()) / -l_3_F);
            int -l_5_I = (int) (((float) drawable.getIntrinsicHeight()) / -l_3_F);
            if (-l_4_I > SmsCheckResult.ESCT_320 || -l_5_I > SmsCheckResult.ESCT_320) {
                f.f("SoftwareManagerImpl", "too large: (" + -l_4_I + ", " + -l_5_I + ")");
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private ov b(String str, ov ovVar, int i) {
        Object -l_11_R;
        try {
            Object -l_4_R = mf.bW(str);
            Object -l_5_R = new File(str);
            if (!-l_5_R.exists()) {
                return null;
            }
            Object -l_6_R = new DisplayMetrics();
            -l_6_R.setToDefaults();
            Object -l_8_R = oy.class;
            synchronized (oy.class) {
                Object -l_7_R = mf.a(-l_4_R, -l_5_R, str, -l_6_R, 0);
                if (-l_7_R == null || !-l_5_R.exists()) {
                    return null;
                }
                String -l_8_R2;
                if ((i & 128) != 0 && -l_5_R.exists()) {
                    -l_8_R2 = (String) mh.a(-l_7_R, "packageName");
                    if (-l_8_R2 != null) {
                        ovVar.put("pkgName", -l_8_R2);
                    }
                }
                if ((i & 256) != 0 && -l_5_R.exists()) {
                    -l_8_R2 = (String) mh.a(-l_7_R, "mVersionName");
                    if (-l_8_R2 != null) {
                        ovVar.put("version", -l_8_R2);
                    }
                }
                if ((i & 512) != 0 && -l_5_R.exists()) {
                    ovVar.put("versionCode", Integer.valueOf(((Integer) mh.a(-l_7_R, "mVersionCode")).intValue()));
                }
                if ((i & 32) != 0 && -l_5_R.exists()) {
                    ArrayList -l_8_R3 = (ArrayList) mh.a(-l_7_R, "requestedPermissions");
                    if (-l_8_R3 != null) {
                        ovVar.put("permissions", -l_8_R3.toArray());
                    }
                }
                ApplicationInfo -l_8_R4 = null;
                if ((i & IncomingSmsFilterConsts.PAY_SMS) != 0 && -l_5_R.exists()) {
                    -l_8_R4 = (ApplicationInfo) mh.a(-l_7_R, "applicationInfo");
                    if (-l_8_R4 != null) {
                        ovVar.put("uid", Integer.valueOf(-l_8_R4.uid));
                    }
                }
                if ((i & 8192) != 0 && -l_5_R.exists()) {
                    if (VERSION.SDK_INT > 7) {
                        -l_8_R4 = (ApplicationInfo) mh.a(-l_7_R, "applicationInfo");
                        if (-l_8_R4 != null) {
                            ovVar.put("installedOnSdcard", Boolean.valueOf((-l_8_R4.flags & 262144) != 0));
                        }
                    } else if (str.startsWith("/data")) {
                        ovVar.put("installedOnSdcard", Boolean.valueOf(false));
                    } else {
                        ovVar.put("installedOnSdcard", Boolean.valueOf(true));
                    }
                }
                if ((i & 2048) != 0 || (i & 4) != 0) {
                    if (-l_5_R.exists()) {
                        Object -l_10_R;
                        Resources resources = null;
                        if (-l_8_R4 == null) {
                            -l_8_R4 = (ApplicationInfo) mh.a(-l_7_R, "applicationInfo");
                        }
                        if (!((i & 2048) == 0 || -l_8_R4 == null)) {
                            -l_10_R = null;
                            if (-l_8_R4.labelRes != 0) {
                                try {
                                    resources = co(str);
                                    -l_10_R = resources.getText(-l_8_R4.labelRes);
                                } catch (Throwable th) {
                                }
                            }
                            if (-l_10_R != null) {
                                if (-l_10_R.toString().length() > 0) {
                                    ovVar.put("appName", -l_10_R);
                                }
                            }
                            -l_10_R = this.mPackageManager.getApplicationLabel(-l_8_R4);
                            ovVar.put("appName", -l_10_R);
                        }
                        if (!((i & 4) == 0 || -l_8_R4 == null)) {
                            Drawable -l_10_R2 = null;
                            if (-l_8_R4.icon != 0) {
                                if (resources == null) {
                                    resources = co(str);
                                }
                                try {
                                    -l_10_R2 = resources.getDrawable(-l_8_R4.icon);
                                } catch (Object -l_11_R2) {
                                    f.e("SoftwareManagerImpl", "" + str + " | res.getDrawable() error: " + -l_11_R2);
                                }
                            }
                            if (-l_10_R2 == null || a(this.mContext, -l_10_R2)) {
                                -l_10_R = this.mPackageManager.getApplicationIcon(-l_8_R4);
                            }
                            ovVar.put("icon", -l_10_R);
                        }
                    }
                }
                if ((i & 16) != 0 && -l_5_R.exists()) {
                    mh.a(-l_4_R, "collectCertificates", new Object[]{-l_7_R, Integer.valueOf(0)});
                    Object -l_9_R = (Signature[]) mh.a(-l_7_R, "mSignatures");
                    if (-l_9_R != null && -l_9_R.length > 0) {
                        X509Certificate -l_10_R3 = (X509Certificate) a(this.Ji, -l_9_R[0]);
                        if (-l_10_R3 != null) {
                            -l_11_R2 = null;
                            try {
                                -l_11_R2 = mc.n(-l_10_R3.getEncoded());
                            } catch (Object -l_12_R) {
                                -l_12_R.printStackTrace();
                            }
                            ovVar.put("signatureCermMD5", -l_11_R2);
                        }
                    }
                }
                return ovVar;
            }
        } catch (Throwable th2) {
            return null;
        }
    }

    private int bF(int i) {
        int -l_2_I = 0;
        if ((i & 16) != 0) {
            -l_2_I = 64;
        }
        return (i & 32) == 0 ? -l_2_I : -l_2_I | 4096;
    }

    private Resources co(String str) throws Exception {
        Object -l_2_R = TMSDKContext.getCurrentContext();
        if (-l_2_R == null) {
            -l_2_R = this.mContext;
        }
        Object -l_3_R = -l_2_R.getResources();
        mh.a(mh.a("android.content.res.AssetManager", null), "addAssetPath", new Object[]{str});
        return (Resources) mh.a("android.content.res.Resources", new Object[]{-l_4_R, -l_3_R.getDisplayMetrics(), -l_3_R.getConfiguration()});
    }

    public static List<String> h(String str, int i) {
        Object -l_2_R = new ArrayList();
        Object -l_3_R;
        try {
            -l_3_R = TMSDKContext.getApplicaionContext().getPackageManager().getPackageInfo(str, 64);
            if (!(-l_3_R == null || -l_3_R.signatures == null || -l_3_R.signatures.length <= 0)) {
                Object -l_4_R = -l_3_R.signatures;
                int -l_5_I = 0;
                while (-l_5_I < -l_4_R.length && -l_5_I < i) {
                    X509Certificate -l_6_R = (X509Certificate) a(CertificateFactory.getInstance("X.509"), -l_4_R[-l_5_I]);
                    if (-l_6_R != null) {
                        try {
                            -l_2_R.add(mc.n(-l_6_R.getEncoded()));
                        } catch (Object -l_7_R) {
                            f.c("SoftwareManagerImpl", "extractPkgCertMd5s(), CertificateEncodingException: " + -l_7_R, -l_7_R);
                            -l_7_R.printStackTrace();
                        }
                    }
                    -l_5_I++;
                }
            }
        } catch (Object -l_3_R2) {
            f.c("SoftwareManagerImpl", "extractPkgCertMd5s(), Exception: " + -l_3_R2, -l_3_R2);
        }
        return -l_2_R;
    }

    public ov c(ov ovVar, int i) {
        String -l_3_R = (String) ovVar.get("apkPath");
        try {
            if (!this.Jg.cO(-l_3_R)) {
                return null;
            }
            int i2;
            if ((i & 1) == 0) {
                i2 = i;
            } else {
                ovVar.put("isSystem", Boolean.FALSE);
                i2 = ((i | 128) | 2048) | IncomingSmsFilterConsts.PAY_SMS;
            }
            if ((i2 & 2) != 0) {
                ovVar.put("isSystem", Boolean.FALSE);
                i2 = (i2 | 128) | IncomingSmsFilterConsts.PAY_SMS;
            }
            if ((i2 & 8) != 0) {
                Object -l_4_R = new File(-l_3_R);
                ovVar.put("size", Long.valueOf(-l_4_R.length()));
                ovVar.put("lastModified", Long.valueOf(-l_4_R.lastModified()));
                i2 = (i2 | 256) | 512;
            }
            if ((i2 & 64) != 0) {
                ovVar.put("apkPath", -l_3_R);
                ovVar.put("isApk", Boolean.valueOf(true));
            }
            return ((i2 & 2048) == 0 && (i2 & 4) == 0 && (i2 & 16) == 0) ? a(-l_3_R, ovVar, i2) : b(-l_3_R, ovVar, i2);
        } catch (Throwable th) {
            return null;
        }
    }

    public ov g(String str, int i) {
        Object -l_3_R = new ov();
        -l_3_R.put("apkPath", str);
        return c(-l_3_R, i);
    }

    public int getSingletonType() {
        return 2;
    }

    public void onCreate(Context context) {
        this.mContext = context;
        this.Jg = new v();
        this.Jh = new ow();
        this.mPackageManager = context.getPackageManager();
        try {
            this.Ji = CertificateFactory.getInstance("X.509");
        } catch (Object -l_2_R) {
            f.f("SoftwareManagerImpl", -l_2_R.getLocalizedMessage());
        }
    }
}
