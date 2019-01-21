package android.view.textservice;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.provider.SettingsStringUtil;
import android.util.AttributeSet;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParserException;

public final class SpellCheckerInfo implements Parcelable {
    public static final Creator<SpellCheckerInfo> CREATOR = new Creator<SpellCheckerInfo>() {
        public SpellCheckerInfo createFromParcel(Parcel source) {
            return new SpellCheckerInfo(source);
        }

        public SpellCheckerInfo[] newArray(int size) {
            return new SpellCheckerInfo[size];
        }
    };
    private static final String TAG = SpellCheckerInfo.class.getSimpleName();
    private final String mId;
    private final int mLabel;
    private final ResolveInfo mService;
    private final String mSettingsActivityName;
    private final ArrayList<SpellCheckerSubtype> mSubtypes = new ArrayList();

    public SpellCheckerInfo(Context context, ResolveInfo service) throws XmlPullParserException, IOException {
        ResolveInfo resolveInfo = service;
        this.mService = resolveInfo;
        ServiceInfo si = resolveInfo.serviceInfo;
        this.mId = new ComponentName(si.packageName, si.name).flattenToShortString();
        PackageManager pm = context.getPackageManager();
        XmlResourceParser parser = null;
        try {
            parser = si.loadXmlMetaData(pm, SpellCheckerSession.SERVICE_META_DATA);
            if (parser != null) {
                int type;
                int i;
                Resources res = pm.getResourcesForApplication(si.applicationInfo);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                while (true) {
                    int next = parser.next();
                    type = next;
                    i = 1;
                    if (next == 1 || type == 2) {
                    }
                }
                Resources res2;
                if ("spell-checker".equals(parser.getName())) {
                    TypedArray sa = res.obtainAttributes(attrs, R.styleable.SpellChecker);
                    int label = sa.getResourceId(0, 0);
                    String settingsActivityComponent = sa.getString(1);
                    sa.recycle();
                    int depth = parser.getDepth();
                    while (true) {
                        int next2 = parser.next();
                        type = next2;
                        if ((next2 != 3 || parser.getDepth() > depth) && type != i) {
                            if (type == 2) {
                                if ("subtype".equals(parser.getName())) {
                                    TypedArray a = res.obtainAttributes(attrs, R.styleable.SpellChecker_Subtype);
                                    res2 = res;
                                    this.mSubtypes.add(new SpellCheckerSubtype(a.getResourceId(0, 0), a.getString(1), a.getString(4), a.getString(2), a.getInt(3, 0)));
                                } else {
                                    throw new XmlPullParserException("Meta-data in spell-checker does not start with subtype tag");
                                }
                            }
                            res2 = res;
                            res = res2;
                            i = 1;
                        }
                    }
                    if (parser != null) {
                        parser.close();
                    }
                    this.mLabel = label;
                    this.mSettingsActivityName = settingsActivityComponent;
                    return;
                }
                res2 = res;
                throw new XmlPullParserException("Meta-data does not start with spell-checker tag");
            }
            throw new XmlPullParserException("No android.view.textservice.scs meta-data");
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Caught exception: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
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

    public SpellCheckerInfo(Parcel source) {
        this.mLabel = source.readInt();
        this.mId = source.readString();
        this.mSettingsActivityName = source.readString();
        this.mService = (ResolveInfo) ResolveInfo.CREATOR.createFromParcel(source);
        source.readTypedList(this.mSubtypes, SpellCheckerSubtype.CREATOR);
    }

    public String getId() {
        return this.mId;
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mService.serviceInfo.packageName, this.mService.serviceInfo.name);
    }

    public String getPackageName() {
        return this.mService.serviceInfo.packageName;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mLabel);
        dest.writeString(this.mId);
        dest.writeString(this.mSettingsActivityName);
        this.mService.writeToParcel(dest, flags);
        dest.writeTypedList(this.mSubtypes);
    }

    public CharSequence loadLabel(PackageManager pm) {
        if (this.mLabel == 0 || pm == null) {
            return "";
        }
        return pm.getText(getPackageName(), this.mLabel, this.mService.serviceInfo.applicationInfo);
    }

    public Drawable loadIcon(PackageManager pm) {
        return this.mService.loadIcon(pm);
    }

    public ServiceInfo getServiceInfo() {
        return this.mService.serviceInfo;
    }

    public String getSettingsActivity() {
        return this.mSettingsActivityName;
    }

    public int getSubtypeCount() {
        return this.mSubtypes.size();
    }

    public SpellCheckerSubtype getSubtypeAt(int index) {
        return (SpellCheckerSubtype) this.mSubtypes.get(index);
    }

    public int describeContents() {
        return 0;
    }

    public void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mId=");
        stringBuilder.append(this.mId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mSettingsActivityName=");
        stringBuilder.append(this.mSettingsActivityName);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("Service:");
        pw.println(stringBuilder.toString());
        ResolveInfo resolveInfo = this.mService;
        PrintWriterPrinter printWriterPrinter = new PrintWriterPrinter(pw);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append("  ");
        resolveInfo.dump(printWriterPrinter, stringBuilder2.toString());
        int N = getSubtypeCount();
        for (int i = 0; i < N; i++) {
            SpellCheckerSubtype st = getSubtypeAt(i);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(prefix);
            stringBuilder3.append("  Subtype #");
            stringBuilder3.append(i);
            stringBuilder3.append(SettingsStringUtil.DELIMITER);
            pw.println(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(prefix);
            stringBuilder3.append("    locale=");
            stringBuilder3.append(st.getLocale());
            stringBuilder3.append(" languageTag=");
            stringBuilder3.append(st.getLanguageTag());
            pw.println(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(prefix);
            stringBuilder3.append("    extraValue=");
            stringBuilder3.append(st.getExtraValue());
            pw.println(stringBuilder3.toString());
        }
    }
}
