package com.android.server.pm;

import android.annotation.SuppressLint;
import android.content.pm.FeatureInfo;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Slog;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class HwForbidUninstallManager {
    private static final boolean DEBUG = SystemProperties.get("ro.dbg.pms_log", "0").equals("on");
    private static final String NFCTAG_SAVE_PATCH = "/system/app/HwNfcTag.apk";
    private static final String NFC_DEVICE_PATH = SystemProperties.get("nfc.node", "/dev/pn544");
    private static final String NFC_FEATURE = "android.hardware.nfc";
    private static final String NFC_HCEF_FEATURE = "android.hardware.nfc.hcef";
    private static final String NFC_HCE_FEATURE = "android.hardware.nfc.hce";
    private static final String NFC_SAVE_PATCH = "/system/app/NfcNci_45.apk";
    private static final String TAG = "HwForbidUninstallManager";
    private static volatile HwForbidUninstallManager mInstance;
    private IHwPackageManagerServiceExInner mHwPmsExInner;
    private String mUninstallApk = null;

    public static HwForbidUninstallManager getInstance(IHwPackageManagerServiceExInner pmsEx) {
        if (mInstance == null) {
            synchronized (HwForbidUninstallManager.class) {
                if (mInstance == null) {
                    mInstance = new HwForbidUninstallManager(pmsEx);
                }
            }
        }
        return mInstance;
    }

    private HwForbidUninstallManager(IHwPackageManagerServiceExInner pmsEx) {
        this.mHwPmsExInner = pmsEx;
    }

    public boolean isUninstallApk(String filePath) {
        String str = this.mUninstallApk;
        return str != null && str.contains(filePath);
    }

    private void setUninstallApk(String string) {
        if (this.mUninstallApk != null) {
            this.mUninstallApk += ";" + string;
            return;
        }
        this.mUninstallApk = string;
    }

    private void restoreUninstallApk(String restoreApk) {
        if (this.mUninstallApk != null && restoreApk != null) {
            for (String apkPath : Pattern.compile("\\s*|\n|\r|\t").matcher(restoreApk).replaceAll("").split(";")) {
                this.mUninstallApk = this.mUninstallApk.replaceAll(apkPath, "");
            }
        }
    }

    @SuppressLint({"PreferForInArrayList"})
    public void getUninstallApk() {
        ArrayMap<String, FeatureInfo> availableFeatures = this.mHwPmsExInner.getIPmsInner().getAvailableFeaturesInner();
        ArrayList<File> allList = new ArrayList<>();
        try {
            allList = HwCfgFilePolicy.getCfgFileList("xml/unstall_apk.xml", 0);
        } catch (NoClassDefFoundError e) {
            Slog.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
        if (allList.size() > 0) {
            Iterator<File> it = allList.iterator();
            while (it.hasNext()) {
                loadUninstallApps(it.next());
            }
        }
        try {
            if (!new File(NFC_DEVICE_PATH).exists()) {
                if (availableFeatures.containsKey(NFC_FEATURE)) {
                    availableFeatures.remove(NFC_FEATURE);
                }
                if (availableFeatures.containsKey(NFC_HCE_FEATURE)) {
                    availableFeatures.remove(NFC_HCE_FEATURE);
                }
                if (availableFeatures.containsKey(NFC_HCEF_FEATURE)) {
                    availableFeatures.remove(NFC_HCEF_FEATURE);
                }
                if (this.mUninstallApk != null) {
                    if (!"".equals(this.mUninstallApk)) {
                        if (!this.mUninstallApk.contains(NFC_SAVE_PATCH)) {
                            this.mUninstallApk += ";" + NFC_SAVE_PATCH + ";" + NFCTAG_SAVE_PATCH;
                        }
                    }
                }
                this.mUninstallApk = "/system/app/NfcNci_45.apk;/system/app/HwNfcTag.apk";
            }
        } catch (Exception e2) {
        }
        if (DEBUG) {
            Slog.d(TAG, "mUninstallApk: " + this.mUninstallApk);
        }
    }

    private void loadUninstallApps(File list) {
        File file = list;
        if (this.mHwPmsExInner.getCust() != null) {
            file = this.mHwPmsExInner.getCust().customizeUninstallApk(file);
        }
        if (file.exists()) {
            FileInputStream in = null;
            try {
                FileInputStream in2 = new FileInputStream(file);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(in2, null);
                for (int eventType = xpp.getEventType(); eventType != 1; eventType = xpp.next()) {
                    if (eventType == 2) {
                        if ("apk".equals(xpp.getName())) {
                            setUninstallApk(xpp.nextText());
                        } else if ("restoreapk".equals(xpp.getName())) {
                            restoreUninstallApk(xpp.nextText());
                        }
                    }
                }
                try {
                    in2.close();
                } catch (IOException e) {
                }
            } catch (XmlPullParserException e2) {
                if (0 != 0) {
                    in.close();
                }
            } catch (IOException e3) {
                if (0 != 0) {
                    in.close();
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        in.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        }
    }
}
