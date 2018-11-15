package android.nfc.cardemulation;

import android.accounts.GrantCredentialsPermissionActivity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.midi.MidiDeviceInfo;
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
import java.util.List;
import java.util.Map.Entry;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class ApduServiceInfo implements Parcelable {
    public static final Creator<ApduServiceInfo> CREATOR = new Creator<ApduServiceInfo>() {
        public ApduServiceInfo createFromParcel(Parcel source) {
            ResolveInfo info = (ResolveInfo) ResolveInfo.CREATOR.createFromParcel(source);
            String description = source.readString();
            boolean onHost = source.readInt() != 0;
            ArrayList<AidGroup> staticAidGroups = new ArrayList();
            if (source.readInt() > 0) {
                source.readTypedList(staticAidGroups, AidGroup.CREATOR);
            }
            ArrayList<AidGroup> dynamicAidGroups = new ArrayList();
            if (source.readInt() > 0) {
                source.readTypedList(dynamicAidGroups, AidGroup.CREATOR);
            }
            boolean requiresUnlock = source.readInt() != 0;
            int bannerResource = source.readInt();
            int uid = source.readInt();
            String settingsActivityName = source.readString();
            ESeInfo seExtension = (ESeInfo) ESeInfo.CREATOR.createFromParcel(source);
            ArrayList<Nfcid2Group> nfcid2Groups = new ArrayList();
            if (source.readInt() > 0) {
                source.readTypedList(nfcid2Groups, Nfcid2Group.CREATOR);
            }
            Drawable drawable = null;
            if (getClass().getClassLoader() != null) {
                Bitmap bitmap = (Bitmap) source.readParcelable(getClass().getClassLoader());
                if (bitmap != null) {
                    drawable = new BitmapDrawable(bitmap);
                    bannerResource = -1;
                }
            }
            ApduServiceInfo service = new ApduServiceInfo(info, onHost, description, staticAidGroups, dynamicAidGroups, requiresUnlock, bannerResource, uid, settingsActivityName, seExtension, nfcid2Groups, drawable, source.readInt() != 0);
            service.setServiceState(CardEmulation.CATEGORY_OTHER, source.readInt());
            return service;
        }

        public ApduServiceInfo[] newArray(int size) {
            return new ApduServiceInfo[size];
        }
    };
    static final String NXP_NFC_EXT_META_DATA = "com.nxp.nfc.extensions";
    static final int POWER_STATE_BATTERY_OFF = 4;
    static final int POWER_STATE_SWITCH_OFF = 2;
    static final int POWER_STATE_SWITCH_ON = 1;
    static final String SECURE_ELEMENT_ESE = "eSE";
    public static final int SECURE_ELEMENT_ROUTE_ESE = 1;
    public static final int SECURE_ELEMENT_ROUTE_UICC = 2;
    public static final int SECURE_ELEMENT_ROUTE_UICC2 = 4;
    static final String SECURE_ELEMENT_UICC = "UICC";
    static final String SECURE_ELEMENT_UICC2 = "UICC2";
    static final String TAG = "ApduServiceInfo";
    public final Drawable mBanner;
    final int mBannerResourceId;
    final String mDescription;
    final HashMap<String, AidGroup> mDynamicAidGroups;
    final FelicaInfo mFelicaExtension;
    final boolean mModifiable;
    final HashMap<String, Nfcid2Group> mNfcid2CategoryToGroup;
    final ArrayList<Nfcid2Group> mNfcid2Groups;
    final ArrayList<String> mNfcid2s;
    final boolean mOnHost;
    final boolean mRequiresDeviceUnlock;
    final ESeInfo mSeExtension;
    final ResolveInfo mService;
    int mServiceState;
    final String mSettingsActivityName;
    final HashMap<String, AidGroup> mStaticAidGroups;
    final int mUid;

    public static class ESeInfo implements Parcelable {
        public static final Creator<ESeInfo> CREATOR = new Creator<ESeInfo>() {
            public ESeInfo createFromParcel(Parcel source) {
                return new ESeInfo(source.readInt(), source.readInt());
            }

            public ESeInfo[] newArray(int size) {
                return new ESeInfo[size];
            }
        };
        final int powerState;
        final int seId;

        public ESeInfo(int seId, int powerState) {
            this.seId = seId;
            this.powerState = powerState;
        }

        public int getSeId() {
            return this.seId;
        }

        public int getPowerState() {
            return this.powerState;
        }

        public String toString() {
            boolean z;
            boolean z2 = true;
            StringBuilder append = new StringBuilder().append("seId: ").append(this.seId).append(",Power state: [switchOn: ");
            if ((this.powerState & 1) != 0) {
                z = true;
            } else {
                z = false;
            }
            append = append.append(z).append(",switchOff: ");
            if ((this.powerState & 2) != 0) {
                z = true;
            } else {
                z = false;
            }
            StringBuilder append2 = append.append(z).append(",batteryOff: ");
            if ((this.powerState & 4) == 0) {
                z2 = false;
            }
            return new StringBuilder(append2.append(z2).append("]").toString()).toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.seId);
            dest.writeInt(this.powerState);
        }
    }

    public static class FelicaInfo implements Parcelable {
        public static final Creator<FelicaInfo> CREATOR = new Creator<FelicaInfo>() {
            public FelicaInfo createFromParcel(Parcel source) {
                return new FelicaInfo(source.readString(), source.readString());
            }

            public FelicaInfo[] newArray(int size) {
                return new FelicaInfo[size];
            }
        };
        final String felicaId;
        final String optParams;

        public FelicaInfo(String felica_id, String opt_params) {
            this.felicaId = felica_id;
            this.optParams = opt_params;
        }

        public String getFelicaId() {
            return this.felicaId;
        }

        public String getOptParams() {
            return this.optParams;
        }

        public String toString() {
            return new StringBuilder("felica id: " + this.felicaId + ",optional params: " + this.optParams).toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.felicaId);
            dest.writeString(this.optParams);
        }
    }

    public static class Nfcid2Group implements Parcelable {
        public static final Creator<Nfcid2Group> CREATOR = new Creator<Nfcid2Group>() {
            public Nfcid2Group createFromParcel(Parcel source) {
                String category = source.readString();
                String description = source.readString();
                int syscodelistSize = source.readInt();
                ArrayList<String> syscodeList = new ArrayList();
                if (syscodelistSize > 0) {
                    source.readStringList(syscodeList);
                }
                int optparamlistSize = source.readInt();
                ArrayList<String> optparamList = new ArrayList();
                if (optparamlistSize > 0) {
                    source.readStringList(optparamList);
                }
                int nfcid2listSize = source.readInt();
                ArrayList<String> nfcid2List = new ArrayList();
                if (nfcid2listSize > 0) {
                    source.readStringList(nfcid2List);
                }
                return new Nfcid2Group(nfcid2List, syscodeList, optparamList, category, description);
            }

            public Nfcid2Group[] newArray(int size) {
                return new Nfcid2Group[size];
            }
        };
        final String category;
        final String description;
        final ArrayList<String> nfcid2s;
        final ArrayList<String> optparam;
        final ArrayList<String> syscode;

        Nfcid2Group(ArrayList<String> nfcid2s, ArrayList<String> syscode, ArrayList<String> optparam, String category, String description) {
            this.nfcid2s = nfcid2s;
            this.category = category;
            this.description = description;
            this.syscode = syscode;
            this.optparam = optparam;
        }

        Nfcid2Group(String category, String description) {
            this.nfcid2s = new ArrayList();
            this.syscode = new ArrayList();
            this.optparam = new ArrayList();
            this.category = category;
            this.description = description;
        }

        public String getCategory() {
            return this.category;
        }

        public ArrayList<String> getNfcid2s() {
            return this.nfcid2s;
        }

        public String getSyscodeForNfcid2(String nfcid2) {
            int idx = this.nfcid2s.indexOf(nfcid2);
            if (idx != -1) {
                return (String) this.syscode.get(idx);
            }
            return "";
        }

        public String getOptparamForNfcid2(String nfcid2) {
            int idx = this.nfcid2s.indexOf(nfcid2);
            if (idx != -1) {
                return (String) this.optparam.get(idx);
            }
            return "";
        }

        public String toString() {
            StringBuilder out = new StringBuilder("Category: " + this.category + ", description: " + this.description + ", AIDs:");
            for (String nfcid2 : this.nfcid2s) {
                out.append(nfcid2);
                out.append(", ");
            }
            return out.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.category);
            dest.writeString(this.description);
            dest.writeInt(this.syscode.size());
            if (this.syscode.size() > 0) {
                dest.writeStringList(this.syscode);
            }
            dest.writeInt(this.optparam.size());
            if (this.optparam.size() > 0) {
                dest.writeStringList(this.optparam);
            }
            dest.writeInt(this.nfcid2s.size());
            if (this.nfcid2s.size() > 0) {
                dest.writeStringList(this.nfcid2s);
            }
        }
    }

    public ApduServiceInfo(ResolveInfo info, boolean onHost, String description, ArrayList<AidGroup> staticAidGroups, ArrayList<AidGroup> dynamicAidGroups, boolean requiresUnlock, int bannerResource, int uid, String settingsActivityName, ESeInfo seExtension, ArrayList<Nfcid2Group> nfcid2Groups, Drawable banner, boolean modifiable) {
        if (banner != null) {
            this.mBanner = banner;
        } else {
            this.mBanner = null;
        }
        this.mModifiable = modifiable;
        this.mService = info;
        this.mDescription = description;
        this.mNfcid2Groups = new ArrayList();
        this.mNfcid2s = new ArrayList();
        this.mStaticAidGroups = new HashMap();
        this.mDynamicAidGroups = new HashMap();
        this.mNfcid2CategoryToGroup = new HashMap();
        this.mOnHost = onHost;
        this.mRequiresDeviceUnlock = requiresUnlock;
        this.mServiceState = 2;
        if (staticAidGroups != null) {
            for (AidGroup aidGroup : staticAidGroups) {
                this.mStaticAidGroups.put(aidGroup.category, aidGroup);
            }
        }
        for (AidGroup aidGroup2 : dynamicAidGroups) {
            this.mDynamicAidGroups.put(aidGroup2.category, aidGroup2);
        }
        if (nfcid2Groups != null) {
            for (Nfcid2Group nfcid2Group : nfcid2Groups) {
                this.mNfcid2Groups.add(nfcid2Group);
                this.mNfcid2CategoryToGroup.put(nfcid2Group.category, nfcid2Group);
                this.mNfcid2s.addAll(nfcid2Group.nfcid2s);
            }
        }
        this.mBannerResourceId = bannerResource;
        this.mUid = uid;
        this.mSettingsActivityName = settingsActivityName;
        this.mSeExtension = seExtension;
        this.mFelicaExtension = null;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ApduServiceInfo(PackageManager pm, ResolveInfo info, boolean onHost) throws XmlPullParserException, IOException {
        this.mBanner = null;
        this.mModifiable = false;
        this.mServiceState = 2;
        ServiceInfo si = info.serviceInfo;
        XmlResourceParser parser = null;
        XmlResourceParser xmlResourceParser = null;
        if (onHost) {
            try {
                parser = si.loadXmlMetaData(pm, HostApduService.SERVICE_META_DATA);
                if (parser == null) {
                    throw new XmlPullParserException("No android.nfc.cardemulation.host_apdu_service meta-data");
                }
            } catch (NameNotFoundException e) {
                throw new XmlPullParserException("Unable to create context for: " + si.packageName);
            } catch (Throwable th) {
                if (parser != null) {
                    parser.close();
                }
            }
        } else {
            parser = si.loadXmlMetaData(pm, OffHostApduService.SERVICE_META_DATA);
            if (parser == null) {
                throw new XmlPullParserException("No android.nfc.cardemulation.off_host_apdu_service meta-data");
            }
        }
        int eventType = parser.getEventType();
        while (eventType != 2 && eventType != 1) {
            eventType = parser.next();
        }
        String tagName = parser.getName();
        if (onHost && ("host-apdu-service".equals(tagName) ^ 1) != 0) {
            throw new XmlPullParserException("Meta-data does not start with <host-apdu-service> tag");
        } else if (onHost || ("offhost-apdu-service".equals(tagName) ^ 1) == 0) {
            Resources res = pm.getResourcesForApplication(si.applicationInfo);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            TypedArray sa;
            if (onHost) {
                sa = res.obtainAttributes(attrs, R.styleable.HostApduService);
                this.mService = info;
                this.mDescription = sa.getString(0);
                this.mRequiresDeviceUnlock = sa.getBoolean(2, false);
                this.mBannerResourceId = sa.getResourceId(3, -1);
                this.mSettingsActivityName = sa.getString(1);
                sa.recycle();
            } else {
                sa = res.obtainAttributes(attrs, R.styleable.OffHostApduService);
                this.mService = info;
                this.mDescription = sa.getString(0);
                this.mRequiresDeviceUnlock = false;
                this.mBannerResourceId = sa.getResourceId(2, -1);
                this.mSettingsActivityName = sa.getString(1);
                sa.recycle();
            }
            this.mNfcid2Groups = new ArrayList();
            this.mStaticAidGroups = new HashMap();
            this.mDynamicAidGroups = new HashMap();
            this.mNfcid2CategoryToGroup = new HashMap();
            this.mNfcid2s = new ArrayList();
            this.mOnHost = onHost;
            int depth = parser.getDepth();
            AidGroup currentGroup = null;
            Nfcid2Group nfcid2Group = null;
            while (true) {
                eventType = parser.next();
                if ((eventType != 3 || parser.getDepth() > depth) && eventType != 1) {
                    tagName = parser.getName();
                    TypedArray groupAttrs;
                    String groupCategory;
                    String groupDescription;
                    if (eventType == 2 && "aid-group".equals(tagName) && currentGroup == null) {
                        groupAttrs = res.obtainAttributes(attrs, R.styleable.AidGroup);
                        groupCategory = groupAttrs.getString(1);
                        groupDescription = groupAttrs.getString(0);
                        if (!CardEmulation.CATEGORY_PAYMENT.equals(groupCategory)) {
                            groupCategory = CardEmulation.CATEGORY_OTHER;
                        }
                        currentGroup = (AidGroup) this.mStaticAidGroups.get(groupCategory);
                        if (currentGroup == null) {
                            currentGroup = new AidGroup(groupCategory, groupDescription);
                        } else if (!CardEmulation.CATEGORY_OTHER.equals(groupCategory)) {
                            Log.e(TAG, "Not allowing multiple aid-groups in the " + groupCategory + " category");
                            currentGroup = null;
                        }
                        groupAttrs.recycle();
                    } else if (eventType == 3 && "aid-group".equals(tagName) && currentGroup != null) {
                        if (currentGroup.aids.size() <= 0) {
                            Log.e(TAG, "Not adding <aid-group> with empty or invalid AIDs");
                        } else if (!this.mStaticAidGroups.containsKey(currentGroup.category)) {
                            this.mStaticAidGroups.put(currentGroup.category, currentGroup);
                        }
                        currentGroup = null;
                    } else if (eventType == 2 && "aid-filter".equals(tagName) && currentGroup != null) {
                        a = res.obtainAttributes(attrs, R.styleable.AidFilter);
                        aid = a.getString(0).toUpperCase();
                        if (!CardEmulation.isValidAid(aid) || (currentGroup.aids.contains(aid) ^ 1) == 0) {
                            Log.e(TAG, "Ignoring invalid or duplicate aid: " + aid);
                        } else {
                            currentGroup.aids.add(aid);
                        }
                        a.recycle();
                    } else if (eventType == 2 && "aid-prefix-filter".equals(tagName) && currentGroup != null) {
                        a = res.obtainAttributes(attrs, R.styleable.AidFilter);
                        aid = a.getString(0).toUpperCase().concat("*");
                        if (!CardEmulation.isValidAid(aid) || (currentGroup.aids.contains(aid) ^ 1) == 0) {
                            Log.e(TAG, "Ignoring invalid or duplicate aid: " + aid);
                        } else {
                            currentGroup.aids.add(aid);
                        }
                        a.recycle();
                    } else if (eventType == 2 && tagName.equals("aid-suffix-filter") && currentGroup != null) {
                        a = res.obtainAttributes(attrs, R.styleable.AidFilter);
                        aid = a.getString(0).toUpperCase().concat("#");
                        if (!CardEmulation.isValidAid(aid) || (currentGroup.aids.contains(aid) ^ 1) == 0) {
                            Log.e(TAG, "Ignoring invalid or duplicate aid: " + aid);
                        } else {
                            currentGroup.aids.add(aid);
                        }
                        a.recycle();
                    } else if (eventType == 2 && "nfcid2-group".equals(tagName) && nfcid2Group == null) {
                        groupAttrs = res.obtainAttributes(attrs, R.styleable.AidGroup);
                        groupDescription = groupAttrs.getString(0);
                        groupCategory = groupAttrs.getString(1);
                        if (!CardEmulation.CATEGORY_PAYMENT.equals(groupCategory)) {
                            groupCategory = CardEmulation.CATEGORY_OTHER;
                        }
                        nfcid2Group = (Nfcid2Group) this.mNfcid2CategoryToGroup.get(groupCategory);
                        if (nfcid2Group == null) {
                            nfcid2Group = new Nfcid2Group(groupCategory, groupDescription);
                        } else if (!CardEmulation.CATEGORY_OTHER.equals(groupCategory)) {
                            Log.e(TAG, "Not allowing multiple nfcid2-groups in the " + groupCategory + " category");
                            nfcid2Group = null;
                        }
                        groupAttrs.recycle();
                    } else if (eventType == 3 && "nfcid2-group".equals(tagName) && nfcid2Group != null) {
                        if (nfcid2Group.nfcid2s.size() <= 0) {
                            Log.e(TAG, "Not adding <nfcid2-group> with empty or invalid NFCID2s");
                        } else if (!this.mNfcid2CategoryToGroup.containsKey(nfcid2Group.category)) {
                            this.mNfcid2Groups.add(nfcid2Group);
                            this.mNfcid2CategoryToGroup.put(nfcid2Group.category, nfcid2Group);
                        }
                        nfcid2Group = null;
                    } else if (eventType == 2 && "nfcid2-filter".equals(tagName) && nfcid2Group != null) {
                        String nfcid2 = parser.getAttributeValue(null, MidiDeviceInfo.PROPERTY_NAME).toUpperCase();
                        String syscode = parser.getAttributeValue(null, "syscode").toUpperCase();
                        String optparam = parser.getAttributeValue(null, "optparam").toUpperCase();
                        if (isValidNfcid2(nfcid2) && nfcid2Group.nfcid2s.size() == 0) {
                            nfcid2Group.nfcid2s.add(nfcid2);
                            nfcid2Group.syscode.add(syscode);
                            nfcid2Group.optparam.add(optparam);
                            this.mNfcid2s.add(nfcid2);
                        } else {
                            Log.e(TAG, "Ignoring invalid or duplicate aid: " + nfcid2);
                        }
                    }
                }
            }
            if (parser != null) {
                parser.close();
            }
            this.mUid = si.applicationInfo.uid;
            if (!onHost) {
                try {
                    xmlResourceParser = si.loadXmlMetaData(pm, NXP_NFC_EXT_META_DATA);
                } catch (Exception e2) {
                    throw new XmlPullParserException("Unable to create context for: " + si.packageName);
                } catch (Throwable th2) {
                    if (xmlResourceParser != null) {
                        xmlResourceParser.close();
                    }
                }
            }
            if (xmlResourceParser != null) {
                eventType = xmlResourceParser.getEventType();
                depth = xmlResourceParser.getDepth();
                String str = null;
                int powerState = 0;
                String str2 = null;
                String str3 = null;
                while (eventType != 2 && eventType != 1) {
                    eventType = xmlResourceParser.next();
                }
                tagName = xmlResourceParser.getName();
                if ("extensions".equals(tagName)) {
                    while (true) {
                        eventType = xmlResourceParser.next();
                        if ((eventType != 3 || xmlResourceParser.getDepth() > depth) && eventType != 1) {
                            tagName = xmlResourceParser.getName();
                            if (eventType == 2 && "se-id".equals(tagName)) {
                                str = xmlResourceParser.getAttributeValue(null, MidiDeviceInfo.PROPERTY_NAME);
                                if (str == null || !(str.equalsIgnoreCase(SECURE_ELEMENT_ESE) || (str.equalsIgnoreCase(SECURE_ELEMENT_UICC) ^ 1) == 0 || (str.equalsIgnoreCase(SECURE_ELEMENT_UICC2) ^ 1) == 0)) {
                                }
                            } else {
                                if (eventType == 2) {
                                    if ("se-power-state".equals(tagName)) {
                                        String powerName = xmlResourceParser.getAttributeValue(null, MidiDeviceInfo.PROPERTY_NAME);
                                        boolean powerValue = xmlResourceParser.getAttributeValue(null, "value").equals("true");
                                        if (powerName.equalsIgnoreCase("SwitchOn") && powerValue) {
                                            powerState |= 1;
                                        } else if (powerName.equalsIgnoreCase("SwitchOff") && powerValue) {
                                            powerState |= 2;
                                        } else if (powerName.equalsIgnoreCase("BatteryOff") && powerValue) {
                                            powerState |= 4;
                                        }
                                    }
                                }
                                if (eventType == 2 && "felica-id".equals(tagName)) {
                                    str2 = xmlResourceParser.getAttributeValue(null, MidiDeviceInfo.PROPERTY_NAME);
                                    if (str2 != null && str2.length() <= 10) {
                                        str3 = xmlResourceParser.getAttributeValue(null, "opt-params");
                                        if (str3.length() > 8) {
                                            throw new XmlPullParserException("Unsupported opt-params: " + str3);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    throw new XmlPullParserException("Unsupported se name: " + str);
                }
                throw new XmlPullParserException("Meta-data does not start with <extensions> tag " + tagName);
            }
            Log.d(TAG, "No com.nxp.nfc.extensions meta-data");
            if (onHost) {
                this.mSeExtension = new ESeInfo(-1, 0);
            } else {
                Log.e(TAG, "SE extension not present, Setting default offhost seID");
                this.mSeExtension = new ESeInfo(2, 0);
            }
            this.mFelicaExtension = new FelicaInfo(null, null);
            if (xmlResourceParser != null) {
                xmlResourceParser.close();
            }
        } else {
            throw new XmlPullParserException("Meta-data does not start with <offhost-apdu-service> tag");
        }
    }

    public void writeToXml(XmlSerializer out) throws IOException {
        out.attribute(null, "description", this.mDescription);
        String modifiable = "";
        if (this.mModifiable) {
            modifiable = "true";
        } else {
            modifiable = "false";
        }
        out.attribute(null, "modifiable", modifiable);
        out.attribute(null, GrantCredentialsPermissionActivity.EXTRAS_REQUESTING_UID, Integer.toString(this.mUid));
        out.attribute(null, "seId", Integer.toString(this.mSeExtension.seId));
        out.attribute(null, "bannerId", Integer.toString(this.mBannerResourceId));
        for (AidGroup group : this.mDynamicAidGroups.values()) {
            group.writeAsXml(out);
        }
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mService.serviceInfo.packageName, this.mService.serviceInfo.name);
    }

    public ResolveInfo getResolveInfo() {
        return this.mService;
    }

    public List<String> getAids() {
        ArrayList<String> aids = new ArrayList();
        for (AidGroup group : getAidGroups()) {
            aids.addAll(group.aids);
        }
        return aids;
    }

    public int getAidCacheSize(String category) {
        if (CardEmulation.CATEGORY_OTHER.equals(category) && (hasCategory(CardEmulation.CATEGORY_OTHER) ^ 1) == 0) {
            return getAidCacheSizeForCategory(CardEmulation.CATEGORY_OTHER);
        }
        return 0;
    }

    public int getAidCacheSizeForCategory(String category) {
        ArrayList<AidGroup> aidGroups = new ArrayList();
        int aidCacheSize = 0;
        aidGroups.addAll(getStaticAidGroups());
        aidGroups.addAll(getDynamicAidGroups());
        if (aidGroups == null || aidGroups.size() == 0) {
            return 0;
        }
        for (AidGroup aidCache : aidGroups) {
            if (aidCache.getCategory().equals(category)) {
                List<String> aids = aidCache.getAids();
                if (!(aids == null || aids.size() == 0)) {
                    for (String aid : aids) {
                        int aidLen = aid.length();
                        if (aid.endsWith("*")) {
                            aidLen--;
                        }
                        aidCacheSize += aidLen >> 1;
                    }
                }
            }
        }
        return aidCacheSize;
    }

    public int geTotalAidNum(String category) {
        if (CardEmulation.CATEGORY_OTHER.equals(category) && (hasCategory(CardEmulation.CATEGORY_OTHER) ^ 1) == 0) {
            return getTotalAidNumCategory(CardEmulation.CATEGORY_OTHER);
        }
        return 0;
    }

    private int getTotalAidNumCategory(String category) {
        ArrayList<AidGroup> aidGroups = new ArrayList();
        int aidTotalNum = 0;
        aidGroups.addAll(getStaticAidGroups());
        aidGroups.addAll(getDynamicAidGroups());
        if (aidGroups == null || aidGroups.size() == 0) {
            return 0;
        }
        for (AidGroup aidCache : aidGroups) {
            if (aidCache.getCategory().equals(category)) {
                List<String> aids = aidCache.getAids();
                if (!(aids == null || aids.size() == 0)) {
                    for (String aid : aids) {
                        if (aid != null && aid.length() > 0) {
                            aidTotalNum++;
                        }
                    }
                }
            }
        }
        return aidTotalNum;
    }

    public List<String> getPrefixAids() {
        ArrayList<String> prefixAids = new ArrayList();
        for (AidGroup group : getAidGroups()) {
            for (String aid : group.aids) {
                if (aid.endsWith("*")) {
                    prefixAids.add(aid);
                }
            }
        }
        return prefixAids;
    }

    public List<String> getSubsetAids() {
        ArrayList<String> subsetAids = new ArrayList();
        for (AidGroup group : getAidGroups()) {
            for (String aid : group.aids) {
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

    public ArrayList<String> getNfcid2s() {
        return this.mNfcid2s;
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
        for (AidGroup group : getAidGroups()) {
            if (group.aids.contains(aid.toUpperCase())) {
                return group.category;
            }
        }
        return null;
    }

    public ArrayList<AidGroup> getStaticAidGroups() {
        ArrayList<AidGroup> groups = new ArrayList();
        for (Entry<String, AidGroup> entry : this.mStaticAidGroups.entrySet()) {
            groups.add((AidGroup) entry.getValue());
        }
        return groups;
    }

    public ArrayList<AidGroup> getDynamicAidGroups() {
        ArrayList<AidGroup> groups = new ArrayList();
        for (Entry<String, AidGroup> entry : this.mDynamicAidGroups.entrySet()) {
            groups.add((AidGroup) entry.getValue());
        }
        return groups;
    }

    public ArrayList<Nfcid2Group> getNfcid2Groups() {
        return this.mNfcid2Groups;
    }

    public ESeInfo getSEInfo() {
        return this.mSeExtension;
    }

    public boolean hasCategory(String category) {
        return !this.mStaticAidGroups.containsKey(category) ? this.mDynamicAidGroups.containsKey(category) : true;
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

    public boolean getModifiable() {
        return this.mModifiable;
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
            Drawable banner;
            if (this.mBannerResourceId == -1) {
                banner = this.mBanner;
            } else {
                banner = pm.getResourcesForApplication(this.mService.serviceInfo.packageName).getDrawable(this.mBannerResourceId);
            }
            return banner;
        } catch (NotFoundException e) {
            Log.e(TAG, "Could not load banner.");
            return null;
        } catch (NameNotFoundException e2) {
            Log.e(TAG, "Could not load banner.");
            return null;
        }
    }

    public int getBannerId() {
        return this.mBannerResourceId;
    }

    public String getSettingsActivityName() {
        return this.mSettingsActivityName;
    }

    static boolean isValidNfcid2(String nfcid2) {
        if (nfcid2 == null) {
            return false;
        }
        int nfcid2Length = nfcid2.length();
        if (nfcid2Length == 0 || nfcid2Length % 2 != 0) {
            Log.e(TAG, "AID " + nfcid2 + " is not correctly formatted.");
            return false;
        } else if (nfcid2Length == 16) {
            return true;
        } else {
            Log.e(TAG, "NFCID2 " + nfcid2 + " is not 8 bytes.");
            return false;
        }
    }

    public String toString() {
        StringBuilder out = new StringBuilder("ApduService: ");
        out.append(getComponent());
        out.append(", description: ").append(this.mDescription);
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
        int i;
        int i2 = 1;
        this.mService.writeToParcel(dest, flags);
        dest.writeString(this.mDescription);
        dest.writeInt(this.mOnHost ? 1 : 0);
        dest.writeInt(this.mStaticAidGroups.size());
        if (this.mStaticAidGroups.size() > 0) {
            dest.writeTypedList(new ArrayList(this.mStaticAidGroups.values()));
        }
        dest.writeInt(this.mDynamicAidGroups.size());
        if (this.mDynamicAidGroups.size() > 0) {
            dest.writeTypedList(new ArrayList(this.mDynamicAidGroups.values()));
        }
        if (this.mRequiresDeviceUnlock) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeInt(i);
        dest.writeInt(this.mBannerResourceId);
        dest.writeInt(this.mUid);
        dest.writeString(this.mSettingsActivityName);
        this.mSeExtension.writeToParcel(dest, flags);
        dest.writeInt(this.mNfcid2Groups.size());
        if (this.mNfcid2Groups.size() > 0) {
            dest.writeTypedList(this.mNfcid2Groups);
        }
        if (this.mBanner != null) {
            dest.writeParcelable(((BitmapDrawable) this.mBanner).getBitmap(), flags);
        } else {
            dest.writeParcelable(null, flags);
        }
        if (!this.mModifiable) {
            i2 = 0;
        }
        dest.writeInt(i2);
        dest.writeInt(this.mServiceState);
    }

    public boolean isServiceEnabled(String category) {
        if (!CardEmulation.CATEGORY_OTHER.equals(category) || this.mServiceState == 1 || this.mServiceState == 3) {
            return true;
        }
        return false;
    }

    public void enableService(String category, boolean flagEnable) {
        if (CardEmulation.CATEGORY_OTHER.equals(category)) {
            Log.d(TAG, "setServiceState:Description:" + this.mDescription + ":InternalState:" + this.mServiceState + ":flagEnable:" + flagEnable);
            if (!(this.mServiceState == 1 && flagEnable) && ((this.mServiceState != 0 || flagEnable) && ((this.mServiceState != 3 || flagEnable) && !(this.mServiceState == 2 && flagEnable)))) {
                if (this.mServiceState == 1 && !flagEnable) {
                    this.mServiceState = 3;
                } else if (this.mServiceState == 0 && flagEnable) {
                    this.mServiceState = 2;
                } else if (this.mServiceState == 3 && flagEnable) {
                    this.mServiceState = 1;
                } else if (this.mServiceState == 2 && !flagEnable) {
                    this.mServiceState = 0;
                }
            }
        }
    }

    public int getServiceState(String category) {
        if (CardEmulation.CATEGORY_OTHER.equals(category)) {
            return this.mServiceState;
        }
        return 1;
    }

    public int setServiceState(String category, int state) {
        if (!CardEmulation.CATEGORY_OTHER.equals(category)) {
            return 1;
        }
        this.mServiceState = state;
        return this.mServiceState;
    }

    public void updateServiceCommitStatus(String category, boolean commitStatus) {
        if (CardEmulation.CATEGORY_OTHER.equals(category)) {
            Log.d(TAG, "updateServiceCommitStatus:Description:" + this.mDescription + ":InternalState:" + this.mServiceState + ":commitStatus:" + commitStatus);
            if (commitStatus) {
                if (this.mServiceState == 3) {
                    this.mServiceState = 0;
                } else if (this.mServiceState == 2) {
                    this.mServiceState = 1;
                }
            } else if (this.mServiceState == 3) {
                this.mServiceState = 1;
            } else if (this.mServiceState == 2) {
                this.mServiceState = 0;
            }
        }
    }

    static String serviceStateToString(int state) {
        switch (state) {
            case 0:
                return "DISABLED";
            case 1:
                return "ENABLED";
            case 2:
                return "ENABLING";
            case 3:
                return "DISABLING";
            default:
                return "UNKNOWN";
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("    " + getComponent() + " (Description: " + getDescription() + ")");
        pw.println("    Static AID groups:");
        for (AidGroup group : this.mStaticAidGroups.values()) {
            pw.println("        Category: " + group.category);
            for (String aid : group.aids) {
                pw.println("            AID: " + aid);
            }
        }
        pw.println("    Dynamic AID groups:");
        for (AidGroup group2 : this.mDynamicAidGroups.values()) {
            pw.println("        Category: " + group2.category);
            for (String aid2 : group2.aids) {
                pw.println("            AID: " + aid2);
            }
        }
        pw.println("    Settings Activity: " + this.mSettingsActivityName);
        pw.println("    Routing Destination: " + (this.mOnHost ? "host" : "secure element"));
        if (hasCategory(CardEmulation.CATEGORY_OTHER)) {
            pw.println("    Service State: " + serviceStateToString(this.mServiceState));
        }
    }
}
