package android.nfc.cardemulation;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.nfc.cardemulation.NxpAidGroup.ApduPatternGroup;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import com.nxp.nfc.NxpConstants;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class NxpApduServiceInfo extends ApduServiceInfo implements Parcelable {
    public static final Creator<NxpApduServiceInfo> CREATOR = new Creator<NxpApduServiceInfo>() {
        public NxpApduServiceInfo createFromParcel(Parcel source) {
            Parcel parcel = source;
            ResolveInfo info = (ResolveInfo) ResolveInfo.CREATOR.createFromParcel(parcel);
            String description = source.readString();
            boolean onHost = source.readInt() != 0;
            ArrayList<NxpAidGroup> staticNxpAidGroups = new ArrayList();
            if (source.readInt() > 0) {
                parcel.readTypedList(staticNxpAidGroups, NxpAidGroup.CREATOR);
            }
            ArrayList<NxpAidGroup> dynamicNxpAidGroups = new ArrayList();
            if (source.readInt() > 0) {
                parcel.readTypedList(dynamicNxpAidGroups, NxpAidGroup.CREATOR);
            }
            boolean requiresUnlock = source.readInt() != 0;
            int bannerResource = source.readInt();
            int uid = source.readInt();
            String settingsActivityName = source.readString();
            ESeInfo seExtension = (ESeInfo) ESeInfo.CREATOR.createFromParcel(parcel);
            ArrayList<Nfcid2Group> nfcid2Groups = new ArrayList();
            if (source.readInt() > 0) {
                parcel.readTypedList(nfcid2Groups, Nfcid2Group.CREATOR);
            }
            new byte[1][0] = (byte) 0;
            NxpApduServiceInfo service = new NxpApduServiceInfo(info, onHost, description, staticNxpAidGroups, dynamicNxpAidGroups, requiresUnlock, bannerResource, uid, settingsActivityName, seExtension, nfcid2Groups, source.createByteArray(), source.readInt() != 0);
            service.setServiceState("other", source.readInt());
            return service;
        }

        public NxpApduServiceInfo[] newArray(int size) {
            return new NxpApduServiceInfo[size];
        }
    };
    static final String GSMA_EXT_META_DATA = "com.gsma.services.nfc.extensions";
    static final String NXP_NFC_EXT_META_DATA = "com.nxp.nfc.extensions";
    static final int POWER_STATE_BATTERY_OFF = 4;
    static final int POWER_STATE_SWITCH_OFF = 2;
    static final int POWER_STATE_SWITCH_ON = 1;
    static final String SECURE_ELEMENT_ESE = "eSE";
    public static final int SECURE_ELEMENT_ROUTE_ESE = 1;
    public static final int SECURE_ELEMENT_ROUTE_UICC = 2;
    public static final int SECURE_ELEMENT_ROUTE_UICC2 = 4;
    static final String SECURE_ELEMENT_SIM = "SIM";
    static final String SECURE_ELEMENT_UICC = "UICC";
    static final String SECURE_ELEMENT_UICC2 = "UICC2";
    static final String TAG = "NxpApduServiceInfo";
    boolean mAidSupport;
    byte[] mByteArrayBanner;
    final HashMap<String, NxpAidGroup> mDynamicNxpAidGroups;
    final FelicaInfo mFelicaExtension;
    final boolean mModifiable;
    final HashMap<String, Nfcid2Group> mNfcid2CategoryToGroup;
    final ArrayList<Nfcid2Group> mNfcid2Groups;
    final ArrayList<String> mNfcid2s;
    final ESeInfo mSeExtension;
    int mServiceState;
    final HashMap<String, NxpAidGroup> mStaticNxpAidGroups;

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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("seId: ");
            stringBuilder.append(this.seId);
            stringBuilder.append(",Power state: [switchOn: ");
            boolean z = true;
            stringBuilder.append((this.powerState & 1) != 0);
            stringBuilder.append(",switchOff: ");
            stringBuilder.append((this.powerState & 2) != 0);
            stringBuilder.append(",batteryOff: ");
            if ((this.powerState & 4) == 0) {
                z = false;
            }
            stringBuilder.append(z);
            stringBuilder.append("]");
            return new StringBuilder(stringBuilder.toString()).toString();
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("felica id: ");
            stringBuilder.append(this.felicaId);
            stringBuilder.append(",optional params: ");
            stringBuilder.append(this.optParams);
            return new StringBuilder(stringBuilder.toString()).toString();
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
                Parcel parcel = source;
                String category = source.readString();
                String description = source.readString();
                int syscodelistSize = source.readInt();
                ArrayList<String> syscodeList = new ArrayList();
                if (syscodelistSize > 0) {
                    parcel.readStringList(syscodeList);
                }
                int optparamlistSize = source.readInt();
                ArrayList<String> optparamList = new ArrayList();
                if (optparamlistSize > 0) {
                    parcel.readStringList(optparamList);
                }
                int nfcid2listSize = source.readInt();
                ArrayList<String> nfcid2List = new ArrayList();
                if (nfcid2listSize > 0) {
                    parcel.readStringList(nfcid2List);
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Category: ");
            stringBuilder.append(this.category);
            stringBuilder.append(", description: ");
            stringBuilder.append(this.description);
            stringBuilder.append(", AIDs:");
            StringBuilder out = new StringBuilder(stringBuilder.toString());
            Iterator it = this.nfcid2s.iterator();
            while (it.hasNext()) {
                out.append((String) it.next());
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

    public NxpApduServiceInfo(ResolveInfo info, boolean onHost, String description, ArrayList<NxpAidGroup> staticNxpAidGroups, ArrayList<NxpAidGroup> dynamicNxpAidGroups, boolean requiresUnlock, int bannerResource, int uid, String settingsActivityName, ESeInfo seExtension, ArrayList<Nfcid2Group> nfcid2Groups, byte[] banner, boolean modifiable) {
        byte[] bArr = banner;
        super(info, onHost, description, nxpAidGroups2AidGroups(staticNxpAidGroups), nxpAidGroups2AidGroups(dynamicNxpAidGroups), requiresUnlock, bannerResource, uid, settingsActivityName);
        this.mByteArrayBanner = null;
        this.mAidSupport = true;
        if (bArr != null) {
            this.mByteArrayBanner = bArr;
        } else {
            this.mByteArrayBanner = null;
        }
        this.mModifiable = modifiable;
        this.mServiceState = 2;
        this.mNfcid2Groups = new ArrayList();
        this.mNfcid2s = new ArrayList();
        this.mNfcid2CategoryToGroup = new HashMap();
        this.mStaticNxpAidGroups = new HashMap();
        this.mDynamicNxpAidGroups = new HashMap();
        if (staticNxpAidGroups != null) {
            Iterator it = staticNxpAidGroups.iterator();
            while (it.hasNext()) {
                NxpAidGroup nxpAidGroup = (NxpAidGroup) it.next();
                this.mStaticNxpAidGroups.put(nxpAidGroup.getCategory(), nxpAidGroup);
            }
        }
        if (dynamicNxpAidGroups != null) {
            Iterator it2 = dynamicNxpAidGroups.iterator();
            while (it2.hasNext()) {
                NxpAidGroup nxpAidGroup2 = (NxpAidGroup) it2.next();
                this.mDynamicNxpAidGroups.put(nxpAidGroup2.getCategory(), nxpAidGroup2);
            }
        }
        if (nfcid2Groups != null) {
            Iterator it3 = nfcid2Groups.iterator();
            while (it3.hasNext()) {
                Nfcid2Group nfcid2Group = (Nfcid2Group) it3.next();
                this.mNfcid2Groups.add(nfcid2Group);
                this.mNfcid2CategoryToGroup.put(nfcid2Group.category, nfcid2Group);
                this.mNfcid2s.addAll(nfcid2Group.nfcid2s);
            }
        }
        this.mSeExtension = seExtension;
        this.mFelicaExtension = null;
    }

    /* JADX WARNING: Missing block: B:184:0x041f, code skipped:
            throw new org.xmlpull.v1.XmlPullParserException(r5.toString());
     */
    /* JADX WARNING: Missing block: B:186:0x0423, code skipped:
            if (r4 == null) goto L_0x044d;
     */
    /* JADX WARNING: Missing block: B:188:0x042d, code skipped:
            if (r4.equals(SECURE_ELEMENT_ESE) == false) goto L_0x0431;
     */
    /* JADX WARNING: Missing block: B:189:0x042f, code skipped:
            r5 = 1;
     */
    /* JADX WARNING: Missing block: B:191:0x0437, code skipped:
            if (r4.equals(SECURE_ELEMENT_UICC) == false) goto L_0x043b;
     */
    /* JADX WARNING: Missing block: B:192:0x0439, code skipped:
            r5 = 2;
     */
    /* JADX WARNING: Missing block: B:193:0x043b, code skipped:
            r5 = 4;
     */
    /* JADX WARNING: Missing block: B:194:0x043c, code skipped:
            r1.mSeExtension = new android.nfc.cardemulation.NxpApduServiceInfo.ESeInfo(r5, r6);
            android.util.Log.d(TAG, r1.mSeExtension.toString());
     */
    /* JADX WARNING: Missing block: B:195:0x044d, code skipped:
            r1.mSeExtension = new android.nfc.cardemulation.NxpApduServiceInfo.ESeInfo(-1, 0);
            android.util.Log.d(TAG, r1.mSeExtension.toString());
     */
    /* JADX WARNING: Missing block: B:196:0x0461, code skipped:
            if (r11 == null) goto L_0x0476;
     */
    /* JADX WARNING: Missing block: B:197:0x0463, code skipped:
            r1.mFelicaExtension = new android.nfc.cardemulation.NxpApduServiceInfo.FelicaInfo(r11, r2);
            android.util.Log.d(TAG, r1.mFelicaExtension.toString());
     */
    /* JADX WARNING: Missing block: B:198:0x0476, code skipped:
            r1.mFelicaExtension = new android.nfc.cardemulation.NxpApduServiceInfo.FelicaInfo(null, null);
     */
    /* JADX WARNING: Missing block: B:199:0x047e, code skipped:
            r9.close();
     */
    /* JADX WARNING: Missing block: B:240:0x0534, code skipped:
            r6 = new java.lang.StringBuilder();
            r6.append("Unsupported se name: ");
            r6.append(r3);
     */
    /* JADX WARNING: Missing block: B:241:0x054a, code skipped:
            throw new org.xmlpull.v1.XmlPullParserException(r6.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public NxpApduServiceInfo(PackageManager pm, ResolveInfo info, boolean onHost) throws XmlPullParserException, IOException {
        int i;
        String groupDescription;
        String groupCategory;
        String optparam;
        StringBuilder stringBuilder;
        int eventType;
        boolean powerValue;
        PackageManager packageManager = pm;
        super(pm, info, onHost);
        this.mByteArrayBanner = null;
        this.mAidSupport = true;
        this.mModifiable = false;
        this.mServiceState = 2;
        ServiceInfo si = info.serviceInfo;
        XmlResourceParser parser = null;
        XmlResourceParser extParser = null;
        XmlResourceParser nfcSeExtParser = null;
        if (onHost) {
            try {
                parser = si.loadXmlMetaData(packageManager, "android.nfc.cardemulation.host_apdu_service");
                if (parser == null) {
                    throw new XmlPullParserException("No android.nfc.cardemulation.host_apdu_service meta-data");
                }
            } catch (NameNotFoundException e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to create context for: ");
                stringBuilder2.append(si.packageName);
                throw new XmlPullParserException(stringBuilder2.toString());
            } catch (Throwable th) {
                if (parser != null) {
                    parser.close();
                }
            }
        } else {
            parser = si.loadXmlMetaData(packageManager, "android.nfc.cardemulation.off_host_apdu_service");
            if (parser != null) {
                extParser = si.loadXmlMetaData(packageManager, NXP_NFC_EXT_META_DATA);
                if (extParser == null) {
                    Log.d(TAG, "No com.nxp.nfc.extensions meta-data");
                }
                nfcSeExtParser = si.loadXmlMetaData(packageManager, GSMA_EXT_META_DATA);
                if (nfcSeExtParser == null) {
                    Log.d(TAG, "No com.gsma.services.nfc.extensions meta-data");
                }
            } else {
                throw new XmlPullParserException("No android.nfc.cardemulation.off_host_apdu_service meta-data");
            }
        }
        int eventType2 = parser.getEventType();
        while (eventType2 != 2 && eventType2 != 1) {
            eventType2 = parser.next();
        }
        String tagName = parser.getName();
        if (onHost) {
            if (!"host-apdu-service".equals(tagName)) {
                throw new XmlPullParserException("Meta-data does not start with <host-apdu-service> tag");
            }
        }
        if (!onHost) {
            if (!"offhost-apdu-service".equals(tagName)) {
                throw new XmlPullParserException("Meta-data does not start with <offhost-apdu-service> tag");
            }
        }
        Resources res = packageManager.getResourcesForApplication(si.applicationInfo);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        this.mStaticNxpAidGroups = new HashMap();
        this.mDynamicNxpAidGroups = new HashMap();
        Iterator it = this.mStaticAidGroups.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, AidGroup> stringaidgroup = (Entry) it.next();
            String category = (String) stringaidgroup.getKey();
            AidGroup aidg = (AidGroup) stringaidgroup.getValue();
            Iterator it2 = it;
            AidGroup aidg2 = aidg;
            AidGroup aidGroup = aidg2;
            this.mStaticNxpAidGroups.put(category, new NxpAidGroup(aidg2));
            it = it2;
        }
        it = this.mDynamicAidGroups.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, AidGroup> stringaidgroup2 = (Entry) it.next();
            Iterator it3 = it;
            this.mDynamicNxpAidGroups.put((String) stringaidgroup2.getKey(), new NxpAidGroup((AidGroup) stringaidgroup2.getValue()));
            it = it3;
            packageManager = pm;
        }
        this.mNfcid2Groups = new ArrayList();
        this.mNfcid2CategoryToGroup = new HashMap();
        this.mNfcid2s = new ArrayList();
        int depth = parser.getDepth();
        ApduPatternGroup currApduPatternGroup = null;
        Nfcid2Group currentNfcid2Group = null;
        while (true) {
            int next = parser.next();
            eventType2 = next;
            i = 3;
            if ((next != 3 || parser.getDepth() > depth) && eventType2 != 1) {
                tagName = parser.getName();
                int depth2;
                if (!onHost && eventType2 == 2 && "apdu-pattern-group".equals(tagName) && currApduPatternGroup == null) {
                    Log.e(TAG, "apdu-pattern-group");
                    TypedArray groupAttrs = res.obtainAttributes(attrs, R.styleable.ApduPatternGroup);
                    depth2 = depth;
                    NxpAidGroup depth3 = (NxpAidGroup) this.mStaticNxpAidGroups.get("other");
                    currApduPatternGroup = new ApduPatternGroup(groupAttrs.getString(0));
                    groupAttrs.recycle();
                    depth = depth2;
                } else {
                    ApduPatternGroup currApduPatternGroup2;
                    Nfcid2Group currentNfcid2Group2;
                    depth2 = depth;
                    if (!onHost && eventType2 == 3 && "apdu-pattern-group".equals(tagName) && currApduPatternGroup != null) {
                        if (currApduPatternGroup.getApduPattern().size() > 0) {
                            ((NxpAidGroup) this.mStaticNxpAidGroups.get("other")).addApduGroup(currApduPatternGroup);
                        }
                        Log.e(TAG, "apdu-pattern-group end");
                    } else if (onHost || eventType2 != 2 || !"apdu-pattern-filter".equals(tagName) || currApduPatternGroup == null) {
                        if (eventType2 == 2 && "nfcid2-group".equals(tagName) && currentNfcid2Group == null) {
                            TypedArray groupAttrs2 = res.obtainAttributes(attrs, R.styleable.AidGroup);
                            groupDescription = groupAttrs2.getString(0);
                            groupCategory = groupAttrs2.getString(1);
                            if (!"payment".equals(groupCategory)) {
                                groupCategory = "other";
                            }
                            currentNfcid2Group = (Nfcid2Group) this.mNfcid2CategoryToGroup.get(groupCategory);
                            if (currentNfcid2Group == null) {
                                currApduPatternGroup2 = currApduPatternGroup;
                                currentNfcid2Group = new Nfcid2Group(groupCategory, groupDescription);
                            } else if ("other".equals(groupCategory)) {
                                currApduPatternGroup2 = currApduPatternGroup;
                            } else {
                                String str = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                currApduPatternGroup2 = currApduPatternGroup;
                                stringBuilder3.append("Not allowing multiple nfcid2-groups in the ");
                                stringBuilder3.append(groupCategory);
                                stringBuilder3.append(" category");
                                Log.e(str, stringBuilder3.toString());
                                currentNfcid2Group = null;
                            }
                            groupAttrs2.recycle();
                        } else {
                            currApduPatternGroup2 = currApduPatternGroup;
                            if (eventType2 == 3 && "nfcid2-group".equals(tagName) && currentNfcid2Group != null) {
                                if (currentNfcid2Group.nfcid2s.size() <= 0) {
                                    Log.e(TAG, "Not adding <nfcid2-group> with empty or invalid NFCID2s");
                                } else if (!this.mNfcid2CategoryToGroup.containsKey(currentNfcid2Group.category)) {
                                    this.mNfcid2Groups.add(currentNfcid2Group);
                                    this.mNfcid2CategoryToGroup.put(currentNfcid2Group.category, currentNfcid2Group);
                                }
                                currentNfcid2Group = null;
                            } else if (eventType2 == 2 && "nfcid2-filter".equals(tagName) && currentNfcid2Group != null) {
                                String nfcid2 = parser.getAttributeValue(null, "name").toUpperCase();
                                groupDescription = parser.getAttributeValue(null, "syscode").toUpperCase();
                                optparam = parser.getAttributeValue(null, "optparam").toUpperCase();
                                if (isValidNfcid2(nfcid2) && currentNfcid2Group.nfcid2s.size() == 0) {
                                    currentNfcid2Group.nfcid2s.add(nfcid2);
                                    currentNfcid2Group.syscode.add(groupDescription);
                                    currentNfcid2Group.optparam.add(optparam);
                                    this.mNfcid2s.add(nfcid2);
                                    currentNfcid2Group2 = currentNfcid2Group;
                                } else {
                                    groupCategory = TAG;
                                    stringBuilder = new StringBuilder();
                                    currentNfcid2Group2 = currentNfcid2Group;
                                    stringBuilder.append("Ignoring invalid or duplicate aid: ");
                                    stringBuilder.append(nfcid2);
                                    Log.e(groupCategory, stringBuilder.toString());
                                }
                                depth = depth2;
                                currApduPatternGroup = currApduPatternGroup2;
                                currentNfcid2Group = currentNfcid2Group2;
                            } else {
                                currentNfcid2Group2 = currentNfcid2Group;
                                depth = depth2;
                                currApduPatternGroup = currApduPatternGroup2;
                                currentNfcid2Group = currentNfcid2Group2;
                            }
                        }
                        depth = depth2;
                        currApduPatternGroup = currApduPatternGroup2;
                    }
                    currentNfcid2Group2 = currentNfcid2Group;
                    currApduPatternGroup2 = currApduPatternGroup;
                    depth = depth2;
                    currApduPatternGroup = currApduPatternGroup2;
                    currentNfcid2Group = currentNfcid2Group2;
                }
                ResolveInfo resolveInfo = info;
            }
        }
        if (parser != null) {
            parser.close();
        }
        if (extParser != null) {
            try {
                eventType = extParser.getEventType();
                int depth4 = extParser.getDepth();
                groupDescription = null;
                int powerState = 0;
                String felicaId = null;
                int eventType3 = eventType;
                String optParam = null;
                while (eventType3 != 2 && eventType3 != 1) {
                    eventType3 = extParser.next();
                }
                String tagName2 = extParser.getName();
                StringBuilder stringBuilder4;
                if ("extensions".equals(tagName2)) {
                    while (true) {
                        int next2 = extParser.next();
                        eventType3 = next2;
                        if ((next2 != i || extParser.getDepth() > depth4) && eventType3 != 1) {
                            tagName2 = extParser.getName();
                            if (eventType3 == 2 && "se-id".equals(tagName2)) {
                                groupDescription = extParser.getAttributeValue(null, "name");
                                if (groupDescription != null) {
                                    if (!(groupDescription.equalsIgnoreCase(SECURE_ELEMENT_ESE) || groupDescription.equalsIgnoreCase(SECURE_ELEMENT_UICC))) {
                                        if (!groupDescription.equalsIgnoreCase(SECURE_ELEMENT_UICC2)) {
                                            break;
                                        }
                                    }
                                } else {
                                    break;
                                }
                            } else if (eventType3 == 2 && "se-power-state".equals(tagName2)) {
                                groupCategory = extParser.getAttributeValue(null, "name");
                                powerValue = extParser.getAttributeValue(null, "value").equals("true");
                                if (groupCategory.equalsIgnoreCase("SwitchOn") && powerValue) {
                                    powerState |= 1;
                                } else if (groupCategory.equalsIgnoreCase("SwitchOff") && powerValue) {
                                    powerState |= 2;
                                } else if (groupCategory.equalsIgnoreCase("BatteryOff") && powerValue) {
                                    powerState |= 4;
                                }
                            } else if (eventType3 == 2 && "felica-id".equals(tagName2)) {
                                felicaId = extParser.getAttributeValue(null, "name");
                                if (felicaId == null || felicaId.length() > 10) {
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Unsupported felicaId: ");
                                    stringBuilder4.append(felicaId);
                                } else {
                                    optParam = extParser.getAttributeValue(null, "opt-params");
                                    if (optParam.length() > 8) {
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("Unsupported opt-params: ");
                                        stringBuilder4.append(optParam);
                                        throw new XmlPullParserException(stringBuilder4.toString());
                                    }
                                }
                            }
                            i = 3;
                        }
                    }
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Unsupported se name: ");
                    stringBuilder4.append(groupDescription);
                    throw new XmlPullParserException(stringBuilder4.toString());
                }
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Meta-data does not start with <extensions> tag ");
                stringBuilder4.append(tagName2);
                throw new XmlPullParserException(stringBuilder4.toString());
            } catch (Throwable th2) {
                extParser.close();
            }
        } else {
            if (onHost) {
                this.mSeExtension = new ESeInfo(-1, 0);
            } else {
                Log.e(TAG, "SE extension not present, Setting default offhost seID");
                this.mSeExtension = new ESeInfo(2, 0);
            }
            this.mFelicaExtension = new FelicaInfo(null, null);
        }
        if (nfcSeExtParser != null) {
            try {
                powerValue = nfcSeExtParser.getEventType();
                eventType = nfcSeExtParser.getDepth();
                boolean z = true;
                this.mAidSupport = true;
                while (!powerValue && powerValue != z) {
                    powerValue = nfcSeExtParser.next();
                    z = true;
                }
                groupDescription = nfcSeExtParser.getName();
                if ("extensions".equals(groupDescription)) {
                    while (true) {
                        i = nfcSeExtParser.next();
                        depth = i;
                        if ((i != 3 || nfcSeExtParser.getDepth() > eventType) && depth != 1) {
                            groupDescription = nfcSeExtParser.getName();
                            if (depth == 2 && "se-id".equals(groupDescription)) {
                                optparam = nfcSeExtParser.getAttributeValue(null, "name");
                                if (optparam != null) {
                                    if (!(optparam.equalsIgnoreCase(SECURE_ELEMENT_ESE) || optparam.equalsIgnoreCase(SECURE_ELEMENT_UICC) || optparam.equalsIgnoreCase(SECURE_ELEMENT_UICC2))) {
                                        if (!optparam.equalsIgnoreCase("SIM")) {
                                            break;
                                        }
                                    }
                                } else {
                                    break;
                                }
                            }
                            if (depth == 2 && "AID-based".equals(groupDescription)) {
                                this.mAidSupport = nfcSeExtParser.getAttributeBooleanValue(0, true);
                            }
                        }
                    }
                    nfcSeExtParser.close();
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Meta-data does not start with <extensions> tag ");
                stringBuilder.append(groupDescription);
                throw new XmlPullParserException(stringBuilder.toString());
            } catch (Throwable th3) {
                nfcSeExtParser.close();
            }
        }
    }

    static ArrayList<AidGroup> nxpAidGroups2AidGroups(ArrayList<NxpAidGroup> nxpAidGroup) {
        ArrayList<AidGroup> aidGroups = new ArrayList();
        if (nxpAidGroup != null) {
            Iterator it = nxpAidGroup.iterator();
            while (it.hasNext()) {
                aidGroups.add(((NxpAidGroup) it.next()).createAidGroup());
            }
        }
        return aidGroups;
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
        out.attribute(null, "uid", Integer.toString(this.mUid));
        out.attribute(null, "seId", Integer.toString(this.mSeExtension.seId));
        out.attribute(null, "bannerId", Integer.toString(this.mBannerResourceId));
        for (AidGroup group : this.mDynamicNxpAidGroups.values()) {
            group.writeAsXml(out);
        }
    }

    public ResolveInfo getResolveInfo() {
        return this.mService;
    }

    public ArrayList<String> getAids() {
        ArrayList<String> aids = new ArrayList();
        Iterator it = getNxpAidGroups().iterator();
        while (it.hasNext()) {
            aids.addAll(((NxpAidGroup) it.next()).getAids());
        }
        return aids;
    }

    public ArrayList<NxpAidGroup> getNxpAidGroups() {
        ArrayList<NxpAidGroup> groups = new ArrayList();
        for (Entry<String, NxpAidGroup> entry : this.mDynamicNxpAidGroups.entrySet()) {
            groups.add((NxpAidGroup) entry.getValue());
        }
        for (Entry<String, NxpAidGroup> entry2 : this.mStaticNxpAidGroups.entrySet()) {
            if (!this.mDynamicNxpAidGroups.containsKey(entry2.getKey())) {
                groups.add((NxpAidGroup) entry2.getValue());
            }
        }
        return groups;
    }

    public ApduServiceInfo createApduServiceInfo() {
        ApduServiceInfo apduServiceInfo = new ApduServiceInfo(getResolveInfo(), isOnHost(), getDescription(), nxpAidGroups2AidGroups(getStaticNxpAidGroups()), nxpAidGroups2AidGroups(getDynamicNxpAidGroups()), requiresUnlock(), getBannerId(), getUid(), getSettingsActivityName());
        apduServiceInfo.setOtherCategoryServiceState(isServiceEnabled("other"));
        return apduServiceInfo;
    }

    public int getAidCacheSize(String category) {
        if ("other".equals(category) && hasCategory("other")) {
            return getAidCacheSizeForCategory("other");
        }
        return 0;
    }

    public int getAidCacheSizeForCategory(String category) {
        ArrayList<NxpAidGroup> nxpAidGroups = new ArrayList();
        int aidCacheSize = 0;
        nxpAidGroups.addAll(getStaticNxpAidGroups());
        nxpAidGroups.addAll(getDynamicNxpAidGroups());
        if (nxpAidGroups.size() == 0) {
            return 0;
        }
        Iterator it = nxpAidGroups.iterator();
        while (it.hasNext()) {
            NxpAidGroup aidCache = (NxpAidGroup) it.next();
            if (aidCache.getCategory().equals(category)) {
                List<String> aids = aidCache.getAids();
                if (aids != null) {
                    if (aids.size() != 0) {
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
        }
        return aidCacheSize;
    }

    public int geTotalAidNum(String category) {
        if ("other".equals(category) && hasCategory("other")) {
            return getTotalAidNumCategory("other");
        }
        return 0;
    }

    public boolean isNonAidBasedRoutingSupported() {
        return this.mAidSupport;
    }

    private int getTotalAidNumCategory(String category) {
        ArrayList<NxpAidGroup> aidGroups = new ArrayList();
        int aidTotalNum = 0;
        aidGroups.addAll(getStaticNxpAidGroups());
        aidGroups.addAll(getDynamicNxpAidGroups());
        if (aidGroups.size() == 0) {
            return 0;
        }
        Iterator it = aidGroups.iterator();
        while (it.hasNext()) {
            NxpAidGroup aidCache = (NxpAidGroup) it.next();
            if (aidCache.getCategory().equals(category)) {
                List<String> aids = aidCache.getAids();
                if (aids != null) {
                    if (aids.size() != 0) {
                        for (String aid : aids) {
                            if (aid != null && aid.length() > 0) {
                                aidTotalNum++;
                            }
                        }
                    }
                }
            }
        }
        return aidTotalNum;
    }

    public ArrayList<NxpAidGroup> getStaticNxpAidGroups() {
        ArrayList<NxpAidGroup> groups = new ArrayList();
        for (Entry<String, NxpAidGroup> entry : this.mStaticNxpAidGroups.entrySet()) {
            groups.add((NxpAidGroup) entry.getValue());
        }
        return groups;
    }

    public ArrayList<NxpAidGroup> getDynamicNxpAidGroups() {
        ArrayList<NxpAidGroup> groups = new ArrayList();
        for (Entry<String, NxpAidGroup> entry : this.mDynamicNxpAidGroups.entrySet()) {
            groups.add((NxpAidGroup) entry.getValue());
        }
        return groups;
    }

    public ArrayList<String> getNfcid2s() {
        return this.mNfcid2s;
    }

    public ArrayList<Nfcid2Group> getNfcid2Groups() {
        return this.mNfcid2Groups;
    }

    public ESeInfo getSEInfo() {
        return this.mSeExtension;
    }

    public boolean getModifiable() {
        return this.mModifiable;
    }

    public Bitmap getBitmapBanner() {
        if (this.mByteArrayBanner == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(this.mByteArrayBanner, 0, this.mByteArrayBanner.length);
    }

    public void setOrReplaceDynamicNxpAidGroup(NxpAidGroup nxpAidGroup) {
        super.setOrReplaceDynamicAidGroup(nxpAidGroup);
        this.mDynamicNxpAidGroups.put(nxpAidGroup.getCategory(), nxpAidGroup);
    }

    public NxpAidGroup getDynamicNxpAidGroupForCategory(String category) {
        return (NxpAidGroup) this.mDynamicNxpAidGroups.get(category);
    }

    public boolean removeDynamicNxpAidGroupForCategory(String category) {
        super.removeDynamicAidGroupForCategory(category);
        return this.mDynamicNxpAidGroups.remove(category) != null;
    }

    public Drawable loadBanner(PackageManager pm) {
        try {
            Drawable banner;
            Resources res = pm.getResourcesForApplication(this.mService.serviceInfo.packageName);
            if (this.mBannerResourceId == -1) {
                banner = new BitmapDrawable(getBitmapBanner());
            } else {
                banner = res.getDrawable(this.mBannerResourceId, null);
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

    static boolean isValidNfcid2(String nfcid2) {
        if (nfcid2 == null) {
            return false;
        }
        int nfcid2Length = nfcid2.length();
        String str;
        StringBuilder stringBuilder;
        if (nfcid2Length == 0 || nfcid2Length % 2 != 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("AID ");
            stringBuilder.append(nfcid2);
            stringBuilder.append(" is not correctly formatted.");
            Log.e(str, stringBuilder.toString());
            return false;
        } else if (nfcid2Length == 16) {
            return true;
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NFCID2 ");
            stringBuilder.append(nfcid2);
            stringBuilder.append(" is not 8 bytes.");
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public String toString() {
        StringBuilder out = new StringBuilder("ApduService: ");
        out.append(getComponent());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(", description: ");
        stringBuilder.append(this.mDescription);
        out.append(stringBuilder.toString());
        out.append(", Static AID Groups: ");
        for (NxpAidGroup nxpAidGroup : this.mStaticNxpAidGroups.values()) {
            out.append(nxpAidGroup.toString());
        }
        out.append(", Dynamic AID Groups: ");
        for (NxpAidGroup nxpAidGroup2 : this.mDynamicNxpAidGroups.values()) {
            out.append(nxpAidGroup2.toString());
        }
        return out.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof NxpApduServiceInfo) {
            return ((NxpApduServiceInfo) o).getComponent().equals(getComponent());
        }
        return false;
    }

    public void writeToParcel(Parcel dest, int flags) {
        this.mService.writeToParcel(dest, flags);
        dest.writeString(this.mDescription);
        dest.writeInt(this.mOnHost);
        dest.writeInt(this.mStaticNxpAidGroups.size());
        if (this.mStaticNxpAidGroups.size() > 0) {
            dest.writeTypedList(new ArrayList(this.mStaticNxpAidGroups.values()));
        }
        dest.writeInt(this.mDynamicNxpAidGroups.size());
        if (this.mDynamicNxpAidGroups.size() > 0) {
            dest.writeTypedList(new ArrayList(this.mDynamicNxpAidGroups.values()));
        }
        dest.writeInt(this.mRequiresDeviceUnlock);
        dest.writeInt(this.mBannerResourceId);
        dest.writeInt(this.mUid);
        dest.writeString(this.mSettingsActivityName);
        this.mSeExtension.writeToParcel(dest, flags);
        dest.writeInt(this.mNfcid2Groups.size());
        if (this.mNfcid2Groups.size() > 0) {
            dest.writeTypedList(this.mNfcid2Groups);
        }
        dest.writeByteArray(this.mByteArrayBanner);
        dest.writeInt(this.mModifiable);
        dest.writeInt(this.mServiceState);
    }

    public boolean isServiceEnabled(String category) {
        if (category != "other" || this.mServiceState == 1 || this.mServiceState == 3) {
            return true;
        }
        return false;
    }

    public void enableService(String category, boolean flagEnable) {
        if (category == "other") {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setServiceState:Description:");
            stringBuilder.append(this.mDescription);
            stringBuilder.append(":InternalState:");
            stringBuilder.append(this.mServiceState);
            stringBuilder.append(":flagEnable:");
            stringBuilder.append(flagEnable);
            Log.d(str, stringBuilder.toString());
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
        if (category != "other") {
            return 1;
        }
        return this.mServiceState;
    }

    public int setServiceState(String category, int state) {
        if (category != "other") {
            return 1;
        }
        this.mServiceState = state;
        return this.mServiceState;
    }

    public void updateServiceCommitStatus(String category, boolean commitStatus) {
        if (category == "other") {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateServiceCommitStatus:Description:");
            stringBuilder.append(this.mDescription);
            stringBuilder.append(":InternalState:");
            stringBuilder.append(this.mServiceState);
            stringBuilder.append(":commitStatus:");
            stringBuilder.append(commitStatus);
            Log.d(str, stringBuilder.toString());
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
            case NxpConstants.SERVICE_STATE_DISABLING /*3*/:
                return "DISABLING";
            default:
                return "UNKNOWN";
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("    Routing Destination: ");
        stringBuilder.append(this.mOnHost ? "host" : "secure element");
        pw.println(stringBuilder.toString());
        if (hasCategory("other")) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("    Service State: ");
            stringBuilder.append(serviceStateToString(this.mServiceState));
            pw.println(stringBuilder.toString());
        }
    }
}
