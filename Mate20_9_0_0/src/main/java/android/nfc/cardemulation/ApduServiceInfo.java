package android.nfc.cardemulation;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
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
import android.util.Xml;
import com.android.internal.R;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.xmlpull.v1.XmlPullParserException;

public class ApduServiceInfo implements Parcelable {
    public static final Creator<ApduServiceInfo> CREATOR = new Creator<ApduServiceInfo>() {
        public ApduServiceInfo createFromParcel(Parcel source) {
            Parcel parcel = source;
            ResolveInfo info = (ResolveInfo) ResolveInfo.CREATOR.createFromParcel(parcel);
            String description = source.readString();
            boolean z = false;
            boolean onHost = source.readInt() != 0;
            ArrayList<AidGroup> staticAidGroups = new ArrayList();
            if (source.readInt() > 0) {
                parcel.readTypedList(staticAidGroups, AidGroup.CREATOR);
            }
            ArrayList<AidGroup> dynamicAidGroups = new ArrayList();
            if (source.readInt() > 0) {
                parcel.readTypedList(dynamicAidGroups, AidGroup.CREATOR);
            }
            boolean requiresUnlock = source.readInt() != 0;
            int bannerResource = source.readInt();
            int uid = source.readInt();
            String settingsActivityName = source.readString();
            if (source.readInt() != 0) {
                z = true;
            }
            boolean isOtherCategoryServiceEnabled = z;
            ApduServiceInfo apduServiceInfo = new ApduServiceInfo(info, onHost, description, staticAidGroups, dynamicAidGroups, requiresUnlock, bannerResource, uid, settingsActivityName);
            apduServiceInfo.setOtherCategoryServiceState(isOtherCategoryServiceEnabled);
            return apduServiceInfo;
        }

        public ApduServiceInfo[] newArray(int size) {
            return new ApduServiceInfo[size];
        }
    };
    static final String TAG = "ApduServiceInfo";
    protected int mBannerResourceId;
    protected String mDescription;
    protected HashMap<String, AidGroup> mDynamicAidGroups;
    private boolean mIsOtherCategoryServiceEnabled = false;
    protected boolean mOnHost;
    protected boolean mRequiresDeviceUnlock;
    protected ResolveInfo mService;
    protected String mSettingsActivityName;
    protected HashMap<String, AidGroup> mStaticAidGroups;
    protected int mUid;

    public ApduServiceInfo(ResolveInfo info, boolean onHost, String description, ArrayList<AidGroup> staticAidGroups, ArrayList<AidGroup> dynamicAidGroups, boolean requiresUnlock, int bannerResource, int uid, String settingsActivityName) {
        AidGroup aidGroup;
        this.mService = info;
        this.mDescription = description;
        this.mStaticAidGroups = new HashMap();
        this.mDynamicAidGroups = new HashMap();
        this.mOnHost = onHost;
        this.mRequiresDeviceUnlock = requiresUnlock;
        Iterator it = staticAidGroups.iterator();
        while (it.hasNext()) {
            aidGroup = (AidGroup) it.next();
            this.mStaticAidGroups.put(aidGroup.category, aidGroup);
        }
        it = dynamicAidGroups.iterator();
        while (it.hasNext()) {
            aidGroup = (AidGroup) it.next();
            this.mDynamicAidGroups.put(aidGroup.category, aidGroup);
        }
        this.mBannerResourceId = bannerResource;
        this.mUid = uid;
        this.mSettingsActivityName = settingsActivityName;
    }

    public ApduServiceInfo(PackageManager pm, ResolveInfo info, boolean onHost) throws XmlPullParserException, IOException {
        int i;
        String tagName;
        PackageManager packageManager = pm;
        ResolveInfo resolveInfo = info;
        boolean z = onHost;
        int i2 = 0;
        ServiceInfo si = resolveInfo.serviceInfo;
        XmlResourceParser parser = null;
        if (z) {
            try {
                parser = si.loadXmlMetaData(packageManager, HostApduService.SERVICE_META_DATA);
                if (parser == null) {
                    throw new XmlPullParserException("No android.nfc.cardemulation.host_apdu_service meta-data");
                }
            } catch (NameNotFoundException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to create context for: ");
                stringBuilder.append(si.packageName);
                throw new XmlPullParserException(stringBuilder.toString());
            } catch (Throwable th) {
                if (parser != null) {
                    parser.close();
                }
            }
        } else {
            parser = si.loadXmlMetaData(packageManager, OffHostApduService.SERVICE_META_DATA);
            if (parser == null) {
                throw new XmlPullParserException("No android.nfc.cardemulation.off_host_apdu_service meta-data");
            }
        }
        int eventType = parser.getEventType();
        while (true) {
            i = 2;
            if (eventType == 2 || eventType == 1) {
                tagName = parser.getName();
            } else {
                eventType = parser.next();
            }
        }
        tagName = parser.getName();
        if (z) {
            if (!"host-apdu-service".equals(tagName)) {
                throw new XmlPullParserException("Meta-data does not start with <host-apdu-service> tag");
            }
        }
        if (!z) {
            if (!"offhost-apdu-service".equals(tagName)) {
                throw new XmlPullParserException("Meta-data does not start with <offhost-apdu-service> tag");
            }
        }
        Resources res = packageManager.getResourcesForApplication(si.applicationInfo);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        int i3 = 3;
        TypedArray sa;
        if (z) {
            sa = res.obtainAttributes(attrs, R.styleable.HostApduService);
            this.mService = resolveInfo;
            this.mDescription = sa.getString(0);
            this.mRequiresDeviceUnlock = sa.getBoolean(2, false);
            this.mBannerResourceId = sa.getResourceId(3, -1);
            this.mSettingsActivityName = sa.getString(1);
            sa.recycle();
        } else {
            sa = res.obtainAttributes(attrs, R.styleable.OffHostApduService);
            this.mService = resolveInfo;
            this.mDescription = sa.getString(0);
            this.mRequiresDeviceUnlock = false;
            this.mBannerResourceId = sa.getResourceId(2, -1);
            this.mSettingsActivityName = sa.getString(1);
            sa.recycle();
        }
        this.mStaticAidGroups = new HashMap();
        this.mDynamicAidGroups = new HashMap();
        this.mOnHost = z;
        int depth = parser.getDepth();
        AidGroup currentGroup = null;
        while (true) {
            AidGroup currentGroup2 = currentGroup;
            int next = parser.next();
            eventType = next;
            if ((next != i3 || parser.getDepth() > depth) && eventType != 1) {
                tagName = parser.getName();
                String str;
                StringBuilder stringBuilder2;
                if (eventType == i && "aid-group".equals(tagName) && currentGroup2 == null) {
                    TypedArray groupAttrs = res.obtainAttributes(attrs, R.styleable.AidGroup);
                    String groupCategory = groupAttrs.getString(1);
                    String groupDescription = groupAttrs.getString(i2);
                    String groupCategory2 = groupCategory;
                    if (!CardEmulation.CATEGORY_PAYMENT.equals(groupCategory2)) {
                        groupCategory2 = "other";
                    }
                    currentGroup2 = (AidGroup) this.mStaticAidGroups.get(groupCategory2);
                    if (currentGroup2 != null) {
                        if ("other".equals(groupCategory2)) {
                            currentGroup = currentGroup2;
                        } else {
                            str = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Not allowing multiple aid-groups in the ");
                            stringBuilder2.append(groupCategory2);
                            stringBuilder2.append(" category");
                            Log.e(str, stringBuilder2.toString());
                            currentGroup = null;
                        }
                    } else {
                        currentGroup = new AidGroup(groupCategory2, groupDescription);
                    }
                    groupAttrs.recycle();
                } else if (eventType == 3 && "aid-group".equals(tagName) && currentGroup2 != null) {
                    if (currentGroup2.aids.size() <= 0) {
                        Log.e(TAG, "Not adding <aid-group> with empty or invalid AIDs");
                    } else if (!this.mStaticAidGroups.containsKey(currentGroup2.category)) {
                        this.mStaticAidGroups.put(currentGroup2.category, currentGroup2);
                    }
                    currentGroup = null;
                    i3 = 3;
                    i2 = 0;
                    packageManager = pm;
                    i = 2;
                } else {
                    TypedArray a;
                    String str2;
                    if (eventType == 2 && "aid-filter".equals(tagName) && currentGroup2 != null) {
                        a = res.obtainAttributes(attrs, R.styleable.AidFilter);
                        str = a.getString(0).toUpperCase();
                        if (!CardEmulation.isValidAid(str) || currentGroup2.aids.contains(str)) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Ignoring invalid or duplicate aid: ");
                            stringBuilder2.append(str);
                            Log.e(str2, stringBuilder2.toString());
                        } else {
                            currentGroup2.aids.add(str);
                        }
                        a.recycle();
                    } else if (eventType == 2 && "aid-prefix-filter".equals(tagName) && currentGroup2 != null) {
                        TypedArray a2 = res.obtainAttributes(attrs, R.styleable.AidFilter);
                        String aid = a2.getString(0).toUpperCase().concat("*");
                        if (!CardEmulation.isValidAid(aid) || currentGroup2.aids.contains(aid)) {
                            str = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Ignoring invalid or duplicate aid: ");
                            stringBuilder3.append(aid);
                            Log.e(str, stringBuilder3.toString());
                        } else {
                            currentGroup2.aids.add(aid);
                        }
                        a2.recycle();
                    } else if (eventType == 2 && "aid-suffix-filter".equals(tagName) && currentGroup2 != null) {
                        a = res.obtainAttributes(attrs, R.styleable.AidFilter);
                        str2 = a.getString(0).toUpperCase().concat("#");
                        if (!CardEmulation.isValidAid(str2) || currentGroup2.aids.contains(str2)) {
                            String str3 = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Ignoring invalid or duplicate aid: ");
                            stringBuilder4.append(str2);
                            Log.e(str3, stringBuilder4.toString());
                        } else {
                            currentGroup2.aids.add(str2);
                        }
                        a.recycle();
                    }
                    currentGroup = currentGroup2;
                }
                i2 = 0;
                packageManager = pm;
                i = 2;
                i3 = 3;
            }
        }
        if (parser != null) {
            parser.close();
        }
        this.mUid = si.applicationInfo.uid;
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mService.serviceInfo.packageName, this.mService.serviceInfo.name);
    }

    public List<String> getAids() {
        ArrayList<String> aids = new ArrayList();
        Iterator it = getAidGroups().iterator();
        while (it.hasNext()) {
            aids.addAll(((AidGroup) it.next()).aids);
        }
        return aids;
    }

    public int getAidCacheSize(String category) {
        return 0;
    }

    public List<String> getPrefixAids() {
        ArrayList<String> prefixAids = new ArrayList();
        Iterator it = getAidGroups().iterator();
        while (it.hasNext()) {
            for (String aid : ((AidGroup) it.next()).aids) {
                if (aid.endsWith("*")) {
                    prefixAids.add(aid);
                }
            }
        }
        return prefixAids;
    }

    public List<String> getSubsetAids() {
        ArrayList<String> subsetAids = new ArrayList();
        Iterator it = getAidGroups().iterator();
        while (it.hasNext()) {
            for (String aid : ((AidGroup) it.next()).aids) {
                if (aid.endsWith("#")) {
                    subsetAids.add(aid);
                }
            }
        }
        return subsetAids;
    }

    public AidGroup getDynamicAidGroupForCategory(String category) {
        return (AidGroup) this.mDynamicAidGroups.get(category);
    }

    public boolean removeDynamicAidGroupForCategory(String category) {
        return this.mDynamicAidGroups.remove(category) != null;
    }

    public ArrayList<AidGroup> getAidGroups() {
        ArrayList<AidGroup> groups = new ArrayList();
        for (Entry<String, AidGroup> entry : this.mDynamicAidGroups.entrySet()) {
            groups.add((AidGroup) entry.getValue());
        }
        for (Entry<String, AidGroup> entry2 : this.mStaticAidGroups.entrySet()) {
            if (!this.mDynamicAidGroups.containsKey(entry2.getKey())) {
                groups.add((AidGroup) entry2.getValue());
            }
        }
        return groups;
    }

    public String getCategoryForAid(String aid) {
        Iterator it = getAidGroups().iterator();
        while (it.hasNext()) {
            AidGroup group = (AidGroup) it.next();
            if (group.aids.contains(aid.toUpperCase())) {
                return group.category;
            }
        }
        return null;
    }

    public boolean hasCategory(String category) {
        return this.mStaticAidGroups.containsKey(category) || this.mDynamicAidGroups.containsKey(category);
    }

    public boolean isOnHost() {
        return this.mOnHost;
    }

    public boolean requiresUnlock() {
        return this.mRequiresDeviceUnlock;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public int getUid() {
        return this.mUid;
    }

    public void setOrReplaceDynamicAidGroup(AidGroup aidGroup) {
        this.mDynamicAidGroups.put(aidGroup.getCategory(), aidGroup);
    }

    public CharSequence loadLabel(PackageManager pm) {
        return this.mService.loadLabel(pm);
    }

    public CharSequence loadAppLabel(PackageManager pm) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(this.mService.resolvePackageName, 128));
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public Drawable loadIcon(PackageManager pm) {
        return this.mService.loadIcon(pm);
    }

    public Drawable loadBanner(PackageManager pm) {
        try {
            return pm.getResourcesForApplication(this.mService.serviceInfo.packageName).getDrawable(this.mBannerResourceId);
        } catch (NotFoundException e) {
            Log.e(TAG, "Could not load banner.");
            return null;
        } catch (NameNotFoundException e2) {
            Log.e(TAG, "Could not load banner.");
            return null;
        }
    }

    public String getSettingsActivityName() {
        return this.mSettingsActivityName;
    }

    public String toString() {
        StringBuilder out = new StringBuilder("ApduService: ");
        out.append(getComponent());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(", description: ");
        stringBuilder.append(this.mDescription);
        out.append(stringBuilder.toString());
        out.append(", Static AID Groups: ");
        for (AidGroup aidGroup : this.mStaticAidGroups.values()) {
            out.append(aidGroup.toString());
        }
        out.append(", Dynamic AID Groups: ");
        for (AidGroup aidGroup2 : this.mDynamicAidGroups.values()) {
            out.append(aidGroup2.toString());
        }
        return out.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ApduServiceInfo) {
            return ((ApduServiceInfo) o).getComponent().equals(getComponent());
        }
        return false;
    }

    public int hashCode() {
        return getComponent().hashCode();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        this.mService.writeToParcel(dest, flags);
        dest.writeString(this.mDescription);
        dest.writeInt(this.mOnHost);
        dest.writeInt(this.mStaticAidGroups.size());
        if (this.mStaticAidGroups.size() > 0) {
            dest.writeTypedList(new ArrayList(this.mStaticAidGroups.values()));
        }
        dest.writeInt(this.mDynamicAidGroups.size());
        if (this.mDynamicAidGroups.size() > 0) {
            dest.writeTypedList(new ArrayList(this.mDynamicAidGroups.values()));
        }
        dest.writeInt(this.mRequiresDeviceUnlock);
        dest.writeInt(this.mBannerResourceId);
        dest.writeInt(this.mUid);
        dest.writeString(this.mSettingsActivityName);
        dest.writeInt(this.mIsOtherCategoryServiceEnabled);
    }

    public boolean isServiceEnabled(String category) {
        if ("other".equals(category)) {
            return this.mIsOtherCategoryServiceEnabled;
        }
        return true;
    }

    public void setOtherCategoryServiceState(boolean isOtherCategoryServiceEnabled) {
        this.mIsOtherCategoryServiceEnabled = isOtherCategoryServiceEnabled;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("    ");
        stringBuilder3.append(getComponent());
        stringBuilder3.append(" (Description: ");
        stringBuilder3.append(getDescription());
        stringBuilder3.append(")");
        pw.println(stringBuilder3.toString());
        pw.println("    Static AID groups:");
        for (AidGroup group : this.mStaticAidGroups.values()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("        Category: ");
            stringBuilder.append(group.category);
            pw.println(stringBuilder.toString());
            for (String aid : group.aids) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("            AID: ");
                stringBuilder2.append(aid);
                pw.println(stringBuilder2.toString());
            }
        }
        pw.println("    Dynamic AID groups:");
        for (AidGroup group2 : this.mDynamicAidGroups.values()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("        Category: ");
            stringBuilder.append(group2.category);
            pw.println(stringBuilder.toString());
            for (String aid2 : group2.aids) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("            AID: ");
                stringBuilder2.append(aid2);
                pw.println(stringBuilder2.toString());
            }
        }
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("    Settings Activity: ");
        stringBuilder3.append(this.mSettingsActivityName);
        pw.println(stringBuilder3.toString());
    }
}
