package android.content.pm;

import android.common.HwFrameworkFactory;
import android.common.HwPackageManager;
import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.hwtheme.HwThemeManager;
import android.os.Parcel;
import android.util.Printer;

public class ComponentInfo extends PackageItemInfo {
    public ApplicationInfo applicationInfo;
    public int descriptionRes;
    public boolean directBootAware = false;
    public boolean enabled = true;
    @Deprecated
    public boolean encryptionAware = false;
    public boolean exported = false;
    public String processName;
    public String splitName;

    public ComponentInfo(ComponentInfo orig) {
        super((PackageItemInfo) orig);
        this.applicationInfo = orig.applicationInfo;
        this.processName = orig.processName;
        this.splitName = orig.splitName;
        this.descriptionRes = orig.descriptionRes;
        this.enabled = orig.enabled;
        this.exported = orig.exported;
        boolean z = orig.directBootAware;
        this.directBootAware = z;
        this.encryptionAware = z;
    }

    public CharSequence loadUnsafeLabel(PackageManager pm) {
        if (this.nonLocalizedLabel != null) {
            return this.nonLocalizedLabel;
        }
        ApplicationInfo ai = this.applicationInfo;
        CharSequence label = null;
        if (this.labelRes != 0) {
            label = pm.getText(this.packageName, this.labelRes, ai);
            if (label != null) {
                return label;
            }
        }
        if (ai.nonLocalizedLabel != null) {
            return ai.nonLocalizedLabel;
        }
        if (ai.labelRes != 0) {
            HwPackageManager hpm = HwFrameworkFactory.getHwPackageManager();
            if (!(ai.hwLabelRes == 0 || hpm == null)) {
                label = hpm.getAppLabelText(pm, this.packageName, ai.hwLabelRes, ai);
            }
            if (label == null) {
                label = pm.getText(this.packageName, ai.labelRes, ai);
            }
            if (label != null) {
                return label;
            }
        }
        return this.name;
    }

    public boolean isEnabled() {
        return this.enabled && this.applicationInfo.enabled;
    }

    public final int getIconResource() {
        HwThemeManager.updateIconCache(this, this.name, this.packageName, this.icon, this.applicationInfo.icon);
        return this.icon != 0 ? this.icon : this.applicationInfo.icon;
    }

    public final int getLogoResource() {
        return this.logo != 0 ? this.logo : this.applicationInfo.logo;
    }

    public final int getBannerResource() {
        return this.banner != 0 ? this.banner : this.applicationInfo.banner;
    }

    public ComponentName getComponentName() {
        return new ComponentName(this.packageName, this.name);
    }

    protected void dumpFront(Printer pw, String prefix) {
        StringBuilder stringBuilder;
        super.dumpFront(pw, prefix);
        if (!(this.processName == null || this.packageName.equals(this.processName))) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("processName=");
            stringBuilder.append(this.processName);
            pw.println(stringBuilder.toString());
        }
        if (this.splitName != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("splitName=");
            stringBuilder.append(this.splitName);
            pw.println(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("enabled=");
        stringBuilder.append(this.enabled);
        stringBuilder.append(" exported=");
        stringBuilder.append(this.exported);
        stringBuilder.append(" directBootAware=");
        stringBuilder.append(this.directBootAware);
        pw.println(stringBuilder.toString());
        if (this.descriptionRes != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("description=");
            stringBuilder.append(this.descriptionRes);
            pw.println(stringBuilder.toString());
        }
    }

    protected void dumpBack(Printer pw, String prefix) {
        dumpBack(pw, prefix, 3);
    }

    void dumpBack(Printer pw, String prefix, int dumpFlags) {
        if ((dumpFlags & 2) != 0) {
            StringBuilder stringBuilder;
            if (this.applicationInfo != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("ApplicationInfo:");
                pw.println(stringBuilder.toString());
                ApplicationInfo applicationInfo = this.applicationInfo;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append("  ");
                applicationInfo.dump(pw, stringBuilder2.toString(), dumpFlags);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("ApplicationInfo: null");
                pw.println(stringBuilder.toString());
            }
        }
        super.dumpBack(pw, prefix);
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        if ((parcelableFlags & 2) != 0) {
            dest.writeInt(0);
        } else {
            dest.writeInt(1);
            this.applicationInfo.writeToParcel(dest, parcelableFlags);
        }
        dest.writeString(this.processName);
        dest.writeString(this.splitName);
        dest.writeInt(this.descriptionRes);
        dest.writeInt(this.enabled);
        dest.writeInt(this.exported);
        dest.writeInt(this.directBootAware);
    }

    protected ComponentInfo(Parcel source) {
        super(source);
        boolean z = true;
        if (source.readInt() != 0) {
            this.applicationInfo = (ApplicationInfo) ApplicationInfo.CREATOR.createFromParcel(source);
        }
        this.processName = source.readString();
        this.splitName = source.readString();
        this.descriptionRes = source.readInt();
        this.enabled = source.readInt() != 0;
        this.exported = source.readInt() != 0;
        if (source.readInt() == 0) {
            z = false;
        }
        this.directBootAware = z;
        this.encryptionAware = z;
    }

    public Drawable loadDefaultIcon(PackageManager pm) {
        return this.applicationInfo.loadIcon(pm);
    }

    protected Drawable loadDefaultBanner(PackageManager pm) {
        return this.applicationInfo.loadBanner(pm);
    }

    protected Drawable loadDefaultLogo(PackageManager pm) {
        return this.applicationInfo.loadLogo(pm);
    }

    protected ApplicationInfo getApplicationInfo() {
        return this.applicationInfo;
    }
}
