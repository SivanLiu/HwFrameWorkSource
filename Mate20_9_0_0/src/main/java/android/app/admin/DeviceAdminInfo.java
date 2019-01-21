package android.app.admin;

import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Printer;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class DeviceAdminInfo implements Parcelable {
    public static final Creator<DeviceAdminInfo> CREATOR = new Creator<DeviceAdminInfo>() {
        public DeviceAdminInfo createFromParcel(Parcel source) {
            return new DeviceAdminInfo(source);
        }

        public DeviceAdminInfo[] newArray(int size) {
            return new DeviceAdminInfo[size];
        }
    };
    static final String TAG = "DeviceAdminInfo";
    public static final int USES_ENCRYPTED_STORAGE = 7;
    public static final int USES_POLICY_DEVICE_OWNER = -2;
    public static final int USES_POLICY_DISABLE_CAMERA = 8;
    public static final int USES_POLICY_DISABLE_KEYGUARD_FEATURES = 9;
    public static final int USES_POLICY_EXPIRE_PASSWORD = 6;
    public static final int USES_POLICY_FORCE_LOCK = 3;
    public static final int USES_POLICY_LIMIT_PASSWORD = 0;
    public static final int USES_POLICY_PROFILE_OWNER = -1;
    public static final int USES_POLICY_RESET_PASSWORD = 2;
    public static final int USES_POLICY_SETS_GLOBAL_PROXY = 5;
    public static final int USES_POLICY_WATCH_LOGIN = 1;
    public static final int USES_POLICY_WIPE_DATA = 4;
    static HashMap<String, Integer> sKnownPolicies = new HashMap();
    static ArrayList<PolicyInfo> sPoliciesDisplayOrder = new ArrayList();
    static SparseArray<PolicyInfo> sRevKnownPolicies = new SparseArray();
    final ActivityInfo mActivityInfo;
    private IHwDeviceAdminInfo mHwDeviceAdminInfo;
    boolean mSupportsTransferOwnership;
    int mUsesPolicies;
    boolean mVisible;

    public static class PolicyInfo {
        public final int description;
        public final int descriptionForSecondaryUsers;
        public final int ident;
        public final int label;
        public final int labelForSecondaryUsers;
        public final String tag;

        public PolicyInfo(int ident, String tag, int label, int description) {
            this(ident, tag, label, description, label, description);
        }

        public PolicyInfo(int ident, String tag, int label, int description, int labelForSecondaryUsers, int descriptionForSecondaryUsers) {
            this.ident = ident;
            this.tag = tag;
            this.label = label;
            this.description = description;
            this.labelForSecondaryUsers = labelForSecondaryUsers;
            this.descriptionForSecondaryUsers = descriptionForSecondaryUsers;
        }
    }

    static {
        sPoliciesDisplayOrder.add(new PolicyInfo(4, "wipe-data", R.string.policylab_wipeData, R.string.policydesc_wipeData, R.string.policylab_wipeData_secondaryUser, R.string.policydesc_wipeData_secondaryUser));
        sPoliciesDisplayOrder.add(new PolicyInfo(2, "reset-password", R.string.policylab_resetPassword, R.string.policydesc_resetPassword));
        int i = 0;
        sPoliciesDisplayOrder.add(new PolicyInfo(0, "limit-password", R.string.policylab_limitPassword, R.string.policydesc_limitPassword));
        sPoliciesDisplayOrder.add(new PolicyInfo(1, "watch-login", R.string.policylab_watchLogin, R.string.policydesc_watchLogin, R.string.policylab_watchLogin, R.string.policydesc_watchLogin_secondaryUser));
        sPoliciesDisplayOrder.add(new PolicyInfo(3, "force-lock", R.string.policylab_forceLock, R.string.policydesc_forceLock));
        sPoliciesDisplayOrder.add(new PolicyInfo(5, "set-global-proxy", R.string.policylab_setGlobalProxy, R.string.policydesc_setGlobalProxy));
        sPoliciesDisplayOrder.add(new PolicyInfo(6, "expire-password", R.string.policylab_expirePassword, R.string.policydesc_expirePassword));
        sPoliciesDisplayOrder.add(new PolicyInfo(7, "encrypted-storage", R.string.policylab_encryptedStorage, R.string.policydesc_encryptedStorage));
        sPoliciesDisplayOrder.add(new PolicyInfo(8, "disable-camera", R.string.policylab_disableCamera, R.string.policydesc_disableCamera));
        sPoliciesDisplayOrder.add(new PolicyInfo(9, "disable-keyguard-features", R.string.policylab_disableKeyguardFeatures, R.string.policydesc_disableKeyguardFeatures));
        while (true) {
            int i2 = i;
            if (i2 < sPoliciesDisplayOrder.size()) {
                PolicyInfo pi = (PolicyInfo) sPoliciesDisplayOrder.get(i2);
                sRevKnownPolicies.put(pi.ident, pi);
                sKnownPolicies.put(pi.tag, Integer.valueOf(pi.ident));
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    public DeviceAdminInfo(Context context, ResolveInfo resolveInfo) throws XmlPullParserException, IOException {
        this(context, resolveInfo.activityInfo);
    }

    /* JADX WARNING: Removed duplicated region for block: B:76:0x0164  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public DeviceAdminInfo(Context context, ActivityInfo activityInfo) throws XmlPullParserException, IOException {
        StringBuilder stringBuilder;
        Throwable th;
        Context context2;
        this.mActivityInfo = activityInfo;
        PackageManager pm = context.getPackageManager();
        XmlResourceParser parser = null;
        try {
            try {
                this.mHwDeviceAdminInfo = HwFrameworkFactory.getHwDeviceAdminInfo(context, this.mActivityInfo);
                parser = this.mActivityInfo.loadXmlMetaData(pm, DeviceAdminReceiver.DEVICE_ADMIN_META_DATA);
                if (parser != null) {
                    boolean z;
                    Resources res = pm.getResourcesForApplication(this.mActivityInfo.applicationInfo);
                    AttributeSet attrs = Xml.asAttributeSet(parser);
                    while (true) {
                        int next = parser.next();
                        int type = next;
                        z = true;
                        if (next == 1 || type == 2) {
                        }
                    }
                    Resources resources;
                    if ("device-admin".equals(parser.getName())) {
                        TypedArray sa = res.obtainAttributes(attrs, R.styleable.DeviceAdmin);
                        this.mVisible = sa.getBoolean(0, true);
                        sa.recycle();
                        int outerDepth = parser.getDepth();
                        while (true) {
                            boolean next2 = parser.next();
                            boolean type2 = next2;
                            if (next2 == z) {
                                break;
                            }
                            next2 = true;
                            if (type2 && parser.getDepth() <= outerDepth) {
                                break;
                            }
                            ActivityInfo activityInfo2;
                            if (type2) {
                                resources = res;
                                res = z;
                            } else if (type2) {
                                resources = res;
                                res = z;
                            } else {
                                String tagName = parser.getName();
                                if (tagName.equals("uses-policies")) {
                                    int innerDepth = parser.getDepth();
                                    while (true) {
                                        boolean next3 = parser.next();
                                        type2 = next3;
                                        if (next3 == z) {
                                            resources = res;
                                            break;
                                        }
                                        if (type2 == next2) {
                                            if (parser.getDepth() <= innerDepth) {
                                                resources = res;
                                                break;
                                            }
                                        }
                                        if (type2 == next2) {
                                            resources = res;
                                        } else if (type2) {
                                            resources = res;
                                        } else {
                                            String policyName = parser.getName();
                                            Integer val = (Integer) sKnownPolicies.get(policyName);
                                            if (val != null) {
                                                this.mUsesPolicies |= 1 << val.intValue();
                                                resources = res;
                                            } else {
                                                String str = TAG;
                                                resources = res;
                                                StringBuilder stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Unknown tag under uses-policies of ");
                                                stringBuilder2.append(getComponent());
                                                stringBuilder2.append(": ");
                                                stringBuilder2.append(policyName);
                                                Log.w(str, stringBuilder2.toString());
                                            }
                                        }
                                        res = resources;
                                        activityInfo2 = activityInfo;
                                        z = true;
                                        next2 = true;
                                    }
                                } else {
                                    resources = res;
                                    if (tagName.equals("support-transfer-ownership")) {
                                        if (parser.next() == 3) {
                                            res = true;
                                            this.mSupportsTransferOwnership = true;
                                        } else {
                                            throw new XmlPullParserException("support-transfer-ownership tag must be empty.");
                                        }
                                    }
                                }
                                res = true;
                            }
                            z = res;
                            res = resources;
                            activityInfo2 = activityInfo;
                        }
                        if (parser != null) {
                            parser.close();
                            return;
                        }
                        return;
                    }
                    resources = res;
                    throw new XmlPullParserException("Meta-data does not start with device-admin tag");
                }
                throw new XmlPullParserException("No android.app.device_admin meta-data");
            } catch (NameNotFoundException e) {
                try {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to create context for: ");
                    stringBuilder.append(this.mActivityInfo.packageName);
                    throw new XmlPullParserException(stringBuilder.toString());
                } catch (Throwable th2) {
                    th = th2;
                    if (parser != null) {
                    }
                    throw th;
                }
            }
        } catch (NameNotFoundException e2) {
            context2 = context;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to create context for: ");
            stringBuilder.append(this.mActivityInfo.packageName);
            throw new XmlPullParserException(stringBuilder.toString());
        } catch (Throwable th3) {
            th = th3;
            context2 = context;
            if (parser != null) {
                parser.close();
            }
            throw th;
        }
    }

    DeviceAdminInfo(Parcel source) {
        this.mActivityInfo = (ActivityInfo) ActivityInfo.CREATOR.createFromParcel(source);
        this.mUsesPolicies = source.readInt();
        this.mSupportsTransferOwnership = source.readBoolean();
    }

    public String getPackageName() {
        return this.mActivityInfo.packageName;
    }

    public String getReceiverName() {
        return this.mActivityInfo.name;
    }

    public ActivityInfo getActivityInfo() {
        return this.mActivityInfo;
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mActivityInfo.packageName, this.mActivityInfo.name);
    }

    public CharSequence loadLabel(PackageManager pm) {
        return this.mActivityInfo.loadLabel(pm);
    }

    public CharSequence loadDescription(PackageManager pm) throws NotFoundException {
        if (this.mActivityInfo.descriptionRes != 0) {
            return pm.getText(this.mActivityInfo.packageName, this.mActivityInfo.descriptionRes, this.mActivityInfo.applicationInfo);
        }
        throw new NotFoundException();
    }

    public Drawable loadIcon(PackageManager pm) {
        return this.mActivityInfo.loadIcon(pm);
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    public boolean usesPolicy(int policyIdent) {
        return (this.mUsesPolicies & (1 << policyIdent)) != 0;
    }

    public String getTagForPolicy(int policyIdent) {
        return ((PolicyInfo) sRevKnownPolicies.get(policyIdent)).tag;
    }

    public boolean supportsTransferOwnership() {
        return this.mSupportsTransferOwnership;
    }

    public ArrayList<PolicyInfo> getUsedPolicies() {
        ArrayList<PolicyInfo> res = new ArrayList();
        int i = 0;
        for (int i2 = 0; i2 < sPoliciesDisplayOrder.size(); i2++) {
            PolicyInfo pi = (PolicyInfo) sPoliciesDisplayOrder.get(i2);
            if (usesPolicy(pi.ident)) {
                res.add(pi);
            }
        }
        if (this.mHwDeviceAdminInfo != null) {
            ArrayList<PolicyInfo> mUsePolicies = this.mHwDeviceAdminInfo.getHwUsedPoliciesList();
            while (i < mUsePolicies.size()) {
                res.add((PolicyInfo) mUsePolicies.get(i));
                i++;
            }
        }
        return res;
    }

    public void writePoliciesToXml(XmlSerializer out) throws IllegalArgumentException, IllegalStateException, IOException {
        out.attribute(null, "flags", Integer.toString(this.mUsesPolicies));
    }

    public void readPoliciesFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        this.mUsesPolicies = Integer.parseInt(parser.getAttributeValue(null, "flags"));
    }

    public void dump(Printer pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("Receiver:");
        pw.println(stringBuilder.toString());
        ActivityInfo activityInfo = this.mActivityInfo;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append("  ");
        activityInfo.dump(pw, stringBuilder2.toString());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DeviceAdminInfo{");
        stringBuilder.append(this.mActivityInfo.name);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        this.mActivityInfo.writeToParcel(dest, flags);
        dest.writeInt(this.mUsesPolicies);
        dest.writeBoolean(this.mSupportsTransferOwnership);
    }

    public int describeContents() {
        return 0;
    }
}
