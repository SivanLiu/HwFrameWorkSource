package android.os;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Parcelable.Creator;
import android.provider.Settings.Secure;
import android.view.accessibility.AccessibilityManager;
import java.util.ArrayList;

public class JankCheckPerfBug implements Parcelable {
    public static final Creator<JankCheckPerfBug> CREATOR = new Creator<JankCheckPerfBug>() {
        public JankCheckPerfBug createFromParcel(Parcel in) {
            return new JankCheckPerfBug(in, null);
        }

        public JankCheckPerfBug[] newArray(int size) {
            return new JankCheckPerfBug[size];
        }
    };
    private static final String[] perfkillerapps = new String[]{"com.qihoo360.mobilesafe", "com.qihoo.vpnmaster", "com.qihoo.antivirus", "com.tencent.qqpimsecure", "com.tencent.powermanager", "com.tencent.token"};
    public boolean hasInstalledPerfKillerApps;
    public boolean isEnableAccessibility;
    public boolean isEnableDaltonizer;
    public boolean isEnableZoomGesture;
    public boolean isHighTextContrastEnabled;
    public boolean isTouchExplorationEnabled;

    /* synthetic */ JankCheckPerfBug(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    private JankCheckPerfBug(Parcel in) {
        if (in.readInt() >= 6) {
            boolean z = false;
            this.hasInstalledPerfKillerApps = in.readInt() != 0;
            this.isEnableAccessibility = in.readInt() != 0;
            this.isTouchExplorationEnabled = in.readInt() != 0;
            this.isHighTextContrastEnabled = in.readInt() != 0;
            this.isEnableZoomGesture = in.readInt() != 0;
            if (in.readInt() != 0) {
                z = true;
            }
            this.isEnableDaltonizer = z;
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(6);
        dest.writeInt(this.hasInstalledPerfKillerApps);
        dest.writeInt(this.isEnableAccessibility);
        dest.writeInt(this.isTouchExplorationEnabled);
        dest.writeInt(this.isHighTextContrastEnabled);
        dest.writeInt(this.isEnableZoomGesture);
        dest.writeInt(this.isEnableDaltonizer);
    }

    public String getAppLabel(String pkgname, PackageManager pm) {
        try {
            return (String) pm.getApplicationLabel(pm.getApplicationInfo(pkgname, null));
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public ArrayList<String> hasInstalledPerfKillerApps(Context context) {
        ArrayList<String> result = new ArrayList();
        PackageManager pm = context.getPackageManager();
        for (int i = 0; i < perfkillerapps.length; i++) {
            if (pm.isPackageAvailable(perfkillerapps[i])) {
                result.add(getAppLabel(perfkillerapps[i], pm));
            }
        }
        return result;
    }

    public void checkPerfBug(Context context) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService("accessibility");
        this.isEnableAccessibility = am.isEnabled();
        this.isTouchExplorationEnabled = am.isTouchExplorationEnabled();
        this.isHighTextContrastEnabled = am.isHighTextContrastEnabled();
        boolean z = false;
        if (hasInstalledPerfKillerApps(context).size() > 0) {
            this.hasInstalledPerfKillerApps = true;
        } else {
            this.hasInstalledPerfKillerApps = false;
        }
        this.isEnableZoomGesture = Secure.getInt(context.getContentResolver(), "accessibility_display_magnification_enabled", 0) == 1;
        if (Secure.getInt(context.getContentResolver(), "accessibility_display_daltonizer_enabled", 0) == 1) {
            z = true;
        }
        this.isEnableDaltonizer = z;
    }
}
