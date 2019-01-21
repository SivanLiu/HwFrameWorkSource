package huawei.android.app;

import android.app.ApplicationPackageManager;
import android.app.ApplicationPackageManager.ResourceName;
import android.common.HwPackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.util.Log;

public class HwApplicationPackageManager implements HwPackageManager {
    private static HwPackageManager sInstance = null;

    public static HwPackageManager getDefault() {
        if (sInstance == null) {
            sInstance = new HwApplicationPackageManager();
        }
        return sInstance;
    }

    public CharSequence getAppLabelText(PackageManager pm, String packageName, int resid, ApplicationInfo appInfo) {
        StringBuilder stringBuilder;
        if (pm instanceof ApplicationPackageManager) {
            ApplicationPackageManager apm = (ApplicationPackageManager) pm;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("label_");
            stringBuilder2.append(packageName);
            ResourceName name = new ResourceName(stringBuilder2.toString(), resid);
            CharSequence text = apm.getCachedString(name);
            if (text != null) {
                return text;
            }
            if (appInfo == null) {
                try {
                    appInfo = apm.getApplicationInfo(packageName, 0);
                } catch (NameNotFoundException e) {
                    return null;
                }
            }
            try {
                text = apm.getResourcesForApplication(appInfo).getText(resid);
                if (text != null) {
                    String[] labels = text.toString().split(",");
                    int brandId = getBrand();
                    if (labels != null && labels.length > 0) {
                        String label = labels[0];
                        if (labels.length > 1 && brandId < labels.length) {
                            label = labels[brandId];
                        }
                        apm.putCachedString(name, labels[brandId]);
                        return labels[brandId];
                    }
                }
            } catch (NameNotFoundException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failure retrieving resources for");
                stringBuilder.append(appInfo.packageName);
                Log.w("PackageManager", stringBuilder.toString());
            } catch (RuntimeException e3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failure retrieving text 0x");
                stringBuilder.append(Integer.toHexString(resid));
                stringBuilder.append(" in package ");
                stringBuilder.append(packageName);
                Log.w("PackageManager", stringBuilder.toString(), e3);
            }
        }
        return null;
    }

    public static final int getBrand() {
        return getDeliverInfo(0);
    }

    public static final boolean isIOTVersion() {
        return 1 == getDeliverInfo(4);
    }

    private static final int getDeliverInfo(int index) {
        String[] infos = SystemProperties.get("ro.config.hw_channel_info", "0,0,460,1,0").split(",");
        if (infos.length >= index + 1 && infos[index] != null) {
            try {
                return Integer.parseInt(infos[index]);
            } catch (NumberFormatException e) {
            }
        }
        return 0;
    }

    public Drawable getBadgedIconForTrustSpace(PackageManager pm) {
        if (pm == null) {
            return null;
        }
        return pm.getDrawable("androidhwext", 33751239, null);
    }
}
