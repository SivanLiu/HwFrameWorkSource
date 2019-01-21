package android.nfc.cardemulation;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiEnterpriseConfig;
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
import org.xmlpull.v1.XmlPullParserException;

public final class NfcFServiceInfo implements Parcelable {
    public static final Creator<NfcFServiceInfo> CREATOR = new Creator<NfcFServiceInfo>() {
        public NfcFServiceInfo createFromParcel(Parcel source) {
            ResolveInfo info = (ResolveInfo) ResolveInfo.CREATOR.createFromParcel(source);
            String description = source.readString();
            String systemCode = source.readString();
            String dynamicSystemCode = null;
            if (source.readInt() != 0) {
                dynamicSystemCode = source.readString();
            }
            String dynamicSystemCode2 = dynamicSystemCode;
            String nfcid2 = source.readString();
            dynamicSystemCode = null;
            if (source.readInt() != 0) {
                dynamicSystemCode = source.readString();
            }
            return new NfcFServiceInfo(info, description, systemCode, dynamicSystemCode2, nfcid2, dynamicSystemCode, source.readInt(), source.readString());
        }

        public NfcFServiceInfo[] newArray(int size) {
            return new NfcFServiceInfo[size];
        }
    };
    private static final String DEFAULT_T3T_PMM = "FFFFFFFFFFFFFFFF";
    static final String TAG = "NfcFServiceInfo";
    final String mDescription;
    String mDynamicNfcid2;
    String mDynamicSystemCode;
    final String mNfcid2;
    final ResolveInfo mService;
    final String mSystemCode;
    final String mT3tPmm;
    final int mUid;

    public NfcFServiceInfo(ResolveInfo info, String description, String systemCode, String dynamicSystemCode, String nfcid2, String dynamicNfcid2, int uid, String t3tPmm) {
        this.mService = info;
        this.mDescription = description;
        this.mSystemCode = systemCode;
        this.mDynamicSystemCode = dynamicSystemCode;
        this.mNfcid2 = nfcid2;
        this.mDynamicNfcid2 = dynamicNfcid2;
        this.mUid = uid;
        this.mT3tPmm = t3tPmm;
    }

    public NfcFServiceInfo(PackageManager pm, ResolveInfo info) throws XmlPullParserException, IOException {
        PackageManager packageManager = pm;
        ResolveInfo resolveInfo = info;
        ServiceInfo si = resolveInfo.serviceInfo;
        XmlResourceParser parser = null;
        StringBuilder stringBuilder;
        try {
            parser = si.loadXmlMetaData(packageManager, HostNfcFService.SERVICE_META_DATA);
            if (parser != null) {
                int i;
                int eventType = parser.getEventType();
                while (true) {
                    i = 1;
                    if (eventType != 2 && eventType != 1) {
                        eventType = parser.next();
                    }
                }
                if ("host-nfcf-service".equals(parser.getName())) {
                    Resources res = packageManager.getResourcesForApplication(si.applicationInfo);
                    AttributeSet attrs = Xml.asAttributeSet(parser);
                    TypedArray sa = res.obtainAttributes(attrs, R.styleable.HostNfcFService);
                    this.mService = resolveInfo;
                    this.mDescription = sa.getString(0);
                    this.mDynamicSystemCode = null;
                    this.mDynamicNfcid2 = null;
                    sa.recycle();
                    String systemCode = null;
                    String nfcid2 = null;
                    String t3tPmm = null;
                    int depth = parser.getDepth();
                    while (true) {
                        int depth2 = depth;
                        int next = parser.next();
                        eventType = next;
                        if (next == 3) {
                            next = depth2;
                            if (parser.getDepth() <= next) {
                                break;
                            }
                        } else {
                            next = depth2;
                        }
                        if (eventType == i) {
                            break;
                        }
                        String tagName = parser.getName();
                        if (eventType == 2 && "system-code-filter".equals(tagName) && systemCode == null) {
                            TypedArray a = res.obtainAttributes(attrs, R.styleable.SystemCodeFilter);
                            systemCode = a.getString(0).toUpperCase();
                            String str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("systemCode: ");
                            stringBuilder2.append(systemCode);
                            Log.d(str, stringBuilder2.toString());
                            if (!(NfcFCardEmulation.isValidSystemCode(systemCode) || systemCode.equalsIgnoreCase(WifiEnterpriseConfig.EMPTY_VALUE))) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Invalid System Code: ");
                                stringBuilder.append(systemCode);
                                Log.e(str, stringBuilder.toString());
                                systemCode = null;
                            }
                            a.recycle();
                        } else if (eventType == 2 && "nfcid2-filter".equals(tagName) && nfcid2 == null) {
                            TypedArray a2 = res.obtainAttributes(attrs, R.styleable.Nfcid2Filter);
                            String nfcid22 = a2.getString(0).toUpperCase();
                            if (!(nfcid22.equalsIgnoreCase("RANDOM") || nfcid22.equalsIgnoreCase(WifiEnterpriseConfig.EMPTY_VALUE) || NfcFCardEmulation.isValidNfcid2(nfcid22))) {
                                String str2 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Invalid NFCID2: ");
                                stringBuilder3.append(nfcid22);
                                Log.e(str2, stringBuilder3.toString());
                                nfcid22 = null;
                            }
                            nfcid2 = nfcid22;
                            a2.recycle();
                        } else if (eventType == 2 && "t3tPmm-filter".equals(tagName) && t3tPmm == null) {
                            TypedArray a3 = res.obtainAttributes(attrs, R.styleable.T3tPmmFilter);
                            t3tPmm = a3.getString(0).toUpperCase();
                            String str3 = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("T3T PMM ");
                            stringBuilder4.append(t3tPmm);
                            Log.e(str3, stringBuilder4.toString());
                            a3.recycle();
                        }
                        depth = next;
                        packageManager = pm;
                        resolveInfo = info;
                        i = 1;
                    }
                    this.mSystemCode = systemCode == null ? WifiEnterpriseConfig.EMPTY_VALUE : systemCode;
                    this.mNfcid2 = nfcid2 == null ? WifiEnterpriseConfig.EMPTY_VALUE : nfcid2;
                    this.mT3tPmm = t3tPmm == null ? DEFAULT_T3T_PMM : t3tPmm;
                    if (parser != null) {
                        parser.close();
                    }
                    this.mUid = si.applicationInfo.uid;
                    return;
                }
                throw new XmlPullParserException("Meta-data does not start with <host-nfcf-service> tag");
            }
            throw new XmlPullParserException("No android.nfc.cardemulation.host_nfcf_service meta-data");
        } catch (NameNotFoundException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to create context for: ");
            stringBuilder.append(si.packageName);
            throw new XmlPullParserException(stringBuilder.toString());
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
        }
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mService.serviceInfo.packageName, this.mService.serviceInfo.name);
    }

    public String getSystemCode() {
        return this.mDynamicSystemCode == null ? this.mSystemCode : this.mDynamicSystemCode;
    }

    public void setOrReplaceDynamicSystemCode(String systemCode) {
        this.mDynamicSystemCode = systemCode;
    }

    public String getNfcid2() {
        return this.mDynamicNfcid2 == null ? this.mNfcid2 : this.mDynamicNfcid2;
    }

    public void setOrReplaceDynamicNfcid2(String nfcid2) {
        this.mDynamicNfcid2 = nfcid2;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public int getUid() {
        return this.mUid;
    }

    public String getT3tPmm() {
        return this.mT3tPmm;
    }

    public CharSequence loadLabel(PackageManager pm) {
        return this.mService.loadLabel(pm);
    }

    public Drawable loadIcon(PackageManager pm) {
        return this.mService.loadIcon(pm);
    }

    public String toString() {
        StringBuilder out = new StringBuilder("NfcFService: ");
        out.append(getComponent());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(", description: ");
        stringBuilder.append(this.mDescription);
        out.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", System Code: ");
        stringBuilder.append(this.mSystemCode);
        out.append(stringBuilder.toString());
        if (this.mDynamicSystemCode != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(", dynamic System Code: ");
            stringBuilder.append(this.mDynamicSystemCode);
            out.append(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(", NFCID2: ");
        stringBuilder.append(this.mNfcid2);
        out.append(stringBuilder.toString());
        if (this.mDynamicNfcid2 != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(", dynamic NFCID2: ");
            stringBuilder.append(this.mDynamicNfcid2);
            out.append(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(", T3T PMM:");
        stringBuilder.append(this.mT3tPmm);
        out.append(stringBuilder.toString());
        return out.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NfcFServiceInfo)) {
            return false;
        }
        NfcFServiceInfo thatService = (NfcFServiceInfo) o;
        if (thatService.getComponent().equals(getComponent()) && thatService.mSystemCode.equalsIgnoreCase(this.mSystemCode) && thatService.mNfcid2.equalsIgnoreCase(this.mNfcid2) && thatService.mT3tPmm.equalsIgnoreCase(this.mT3tPmm)) {
            return true;
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
        dest.writeString(this.mSystemCode);
        int i = 0;
        dest.writeInt(this.mDynamicSystemCode != null ? 1 : 0);
        if (this.mDynamicSystemCode != null) {
            dest.writeString(this.mDynamicSystemCode);
        }
        dest.writeString(this.mNfcid2);
        if (this.mDynamicNfcid2 != null) {
            i = 1;
        }
        dest.writeInt(i);
        if (this.mDynamicNfcid2 != null) {
            dest.writeString(this.mDynamicNfcid2);
        }
        dest.writeInt(this.mUid);
        dest.writeString(this.mT3tPmm);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("    ");
        stringBuilder.append(getComponent());
        stringBuilder.append(" (Description: ");
        stringBuilder.append(getDescription());
        stringBuilder.append(")");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("    System Code: ");
        stringBuilder.append(getSystemCode());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("    NFCID2: ");
        stringBuilder.append(getNfcid2());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("    T3tPmm: ");
        stringBuilder.append(getT3tPmm());
        pw.println(stringBuilder.toString());
    }
}
