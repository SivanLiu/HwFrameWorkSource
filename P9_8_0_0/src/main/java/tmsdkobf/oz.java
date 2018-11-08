package tmsdkobf;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build.VERSION;
import android.text.TextUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.creator.BaseManagerC;
import tmsdk.common.utils.f;

public final class oz extends BaseManagerC implements pa {
    private CertificateFactory Ji = null;
    private Context mContext = null;
    private PackageManager mPackageManager = null;

    private Certificate a(Signature signature) {
        Object -l_2_R = new ByteArrayInputStream(signature.toByteArray());
        Object -l_3_R = null;
        try {
            -l_3_R = (X509Certificate) this.Ji.generateCertificate(-l_2_R);
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

    private void a(PackageInfo packageInfo, ov ovVar) {
        if (packageInfo != null && packageInfo.signatures != null && packageInfo.signatures.length >= 1) {
            X509Certificate -l_3_R = (X509Certificate) a(packageInfo.signatures[0]);
            if (-l_3_R != null) {
                Object -l_4_R = null;
                try {
                    -l_4_R = mc.n(-l_3_R.getEncoded());
                } catch (Object -l_5_R) {
                    -l_5_R.printStackTrace();
                }
                ovVar.put("signatureCermMD5", -l_4_R);
            }
        }
    }

    private void a(PackageInfo packageInfo, ov ovVar, int i) {
        int i2 = -1;
        boolean z = false;
        if (packageInfo != null && ovVar != null) {
            if ((i & 16) != 0) {
                a(packageInfo, ovVar);
            }
            if ((i & 32) != 0) {
                ovVar.put("permissions", packageInfo.requestedPermissions);
            }
            if (packageInfo.applicationInfo != null) {
                if ((i & 1) != 0) {
                    ovVar.put("pkgName", packageInfo.applicationInfo.packageName);
                    ovVar.put("appName", this.mPackageManager.getApplicationLabel(packageInfo.applicationInfo).toString());
                    ovVar.put("isSystem", Boolean.valueOf((packageInfo.applicationInfo.flags & 1) != 0));
                    ovVar.put("uid", Integer.valueOf(packageInfo.applicationInfo == null ? -1 : packageInfo.applicationInfo.uid));
                }
                if ((i & 2) != 0) {
                    ovVar.put("pkgName", packageInfo.applicationInfo.packageName);
                    ovVar.put("isSystem", Boolean.valueOf((packageInfo.applicationInfo.flags & 1) != 0));
                    String str = "uid";
                    if (packageInfo.applicationInfo != null) {
                        i2 = packageInfo.applicationInfo.uid;
                    }
                    ovVar.put(str, Integer.valueOf(i2));
                }
                if ((i & 4) != 0) {
                    ovVar.put("icon", packageInfo.applicationInfo.loadIcon(this.mPackageManager));
                }
                if (!((i & 8) == 0 || TextUtils.isEmpty(packageInfo.applicationInfo.sourceDir))) {
                    ovVar.put("version", packageInfo.versionName);
                    ovVar.put("versionCode", Integer.valueOf(packageInfo.versionCode));
                    Object -l_4_R = new File(packageInfo.applicationInfo.sourceDir);
                    ovVar.put("size", Long.valueOf(-l_4_R.length()));
                    ovVar.put("lastModified", Long.valueOf(-l_4_R.lastModified()));
                }
                if ((i & 64) != 0) {
                    ovVar.put("apkPath", packageInfo.applicationInfo.sourceDir);
                    ovVar.put("isApk", Boolean.valueOf(false));
                }
                if (!((i & 8192) == 0 || VERSION.SDK_INT <= 7 || packageInfo.applicationInfo == null)) {
                    String str2 = "installedOnSdcard";
                    if ((packageInfo.applicationInfo.flags & 262144) != 0) {
                        z = true;
                    }
                    ovVar.put(str2, Boolean.valueOf(z));
                }
            }
        }
    }

    private int bF(int i) {
        int -l_2_I = 0;
        if ((i & 16) != 0) {
            -l_2_I = 64;
        }
        return (i & 32) == 0 ? -l_2_I : -l_2_I | 4096;
    }

    public ov a(String str, int i) {
        ov -l_3_R = new ov();
        -l_3_R.put("pkgName", str);
        return a(-l_3_R, i);
    }

    public ov a(ov ovVar, int i) {
        Object -l_3_R = getPackageInfo((String) ovVar.get("pkgName"), bF(i));
        if (-l_3_R == null) {
            return null;
        }
        a(-l_3_R, ovVar, i);
        return ovVar;
    }

    public boolean ai(String str) {
        return getPackageInfo(str, 0) != null;
    }

    public ArrayList<ov> f(int i, int i2) {
        Object -l_3_R = null;
        try {
            -l_3_R = this.mPackageManager.getInstalledPackages(bF(i));
        } catch (Object -l_4_R) {
            -l_4_R.printStackTrace();
        }
        Object -l_4_R2 = new ArrayList();
        if (-l_3_R != null) {
            for (PackageInfo -l_6_R : -l_3_R) {
                int -l_7_I = (-l_6_R.applicationInfo.flags & 1) == 0 ? 0 : 1;
                if (-l_7_I != 0 || i2 != 1) {
                    if (-l_7_I != 0) {
                        if (i2 != 0) {
                        }
                    }
                    Object -l_8_R = new ov();
                    a(-l_6_R, -l_8_R, i);
                    -l_4_R2.add(-l_8_R);
                }
            }
        }
        return -l_4_R2;
    }

    public NetworkInfo getActiveNetworkInfo() {
        Object -l_1_R = null;
        try {
            -l_1_R = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getActiveNetworkInfo();
        } catch (Object -l_2_R) {
            f.g("getActiveNetworkInfo", " getActiveNetworkInfo NullPointerException--- \n" + -l_2_R.getMessage());
        }
        return -l_1_R;
    }

    public PackageInfo getPackageInfo(String str, int i) {
        try {
            return this.mPackageManager.getPackageInfo(str, i);
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public void onCreate(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        try {
            this.Ji = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
        }
    }

    public List<ResolveInfo> queryIntentServices(Intent intent, int i) {
        return this.mPackageManager.queryIntentServices(intent, i);
    }
}
