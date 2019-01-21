package android.view.inputmethod;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
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
import android.util.Printer;
import android.util.Xml;
import android.view.inputmethod.InputMethodSubtype.InputMethodSubtypeBuilder;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public final class InputMethodInfo implements Parcelable {
    public static final Creator<InputMethodInfo> CREATOR = new Creator<InputMethodInfo>() {
        public InputMethodInfo createFromParcel(Parcel source) {
            return new InputMethodInfo(source);
        }

        public InputMethodInfo[] newArray(int size) {
            return new InputMethodInfo[size];
        }
    };
    static final String TAG = "InputMethodInfo";
    private final boolean mForceDefault;
    final String mId;
    private final boolean mIsAuxIme;
    final int mIsDefaultResId;
    final boolean mIsVrOnly;
    final ResolveInfo mService;
    final String mSettingsActivityName;
    private final InputMethodSubtypeArray mSubtypes;
    private final boolean mSupportsSwitchingToNextInputMethod;

    public static String computeId(ResolveInfo service) {
        ServiceInfo si = service.serviceInfo;
        return new ComponentName(si.packageName, si.name).flattenToShortString();
    }

    public InputMethodInfo(Context context, ResolveInfo service) throws XmlPullParserException, IOException {
        this(context, service, null);
    }

    /* JADX WARNING: Removed duplicated region for block: B:105:0x0239  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0239  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0239  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0239  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0239  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0239  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0239  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public InputMethodInfo(Context context, ResolveInfo service, List<InputMethodSubtype> additionalSubtypes) throws XmlPullParserException, IOException {
        boolean isAuxIme;
        StringBuilder stringBuilder;
        Throwable th;
        ResolveInfo resolveInfo = service;
        List<InputMethodSubtype> list = additionalSubtypes;
        this.mService = resolveInfo;
        ServiceInfo si = resolveInfo.serviceInfo;
        this.mId = computeId(service);
        boolean isAuxIme2 = true;
        this.mForceDefault = false;
        PackageManager pm = context.getPackageManager();
        XmlResourceParser parser = null;
        List subtypes = new ArrayList();
        boolean isAuxIme3;
        PackageManager packageManager;
        try {
            parser = si.loadXmlMetaData(pm, InputMethod.SERVICE_META_DATA);
            if (parser != null) {
                Resources res = pm.getResourcesForApplication(si.applicationInfo);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                while (true) {
                    int next = parser.next();
                    int type = next;
                    if (next == 1 || type == 2) {
                    }
                }
                Resources res2;
                if ("input-method".equals(parser.getName())) {
                    TypedArray sa = res.obtainAttributes(attrs, R.styleable.InputMethod);
                    String settingsActivityComponent = sa.getString(1);
                    isAuxIme3 = true;
                    try {
                        boolean next2;
                        boolean isVrOnly = sa.getBoolean(3, false);
                        int isDefaultResId = sa.getResourceId(0, 0);
                        boolean supportsSwitchingToNextInputMethod = sa.getBoolean(2, false);
                        sa.recycle();
                        int depth = parser.getDepth();
                        isAuxIme2 = isAuxIme3;
                        while (true) {
                            TypedArray sa2 = sa;
                            try {
                                next2 = parser.next();
                                boolean type2 = next2;
                                isAuxIme = isAuxIme2;
                                if (next2) {
                                    try {
                                        if (parser.getDepth() <= depth) {
                                            packageManager = pm;
                                            pm = null;
                                            break;
                                        }
                                    } catch (NameNotFoundException | IndexOutOfBoundsException | NumberFormatException e) {
                                        isAuxIme2 = isAuxIme;
                                        try {
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Unable to create context for: ");
                                            stringBuilder.append(si.packageName);
                                            throw new XmlPullParserException(stringBuilder.toString());
                                        } catch (Throwable th2) {
                                            th = th2;
                                            isAuxIme = isAuxIme2;
                                            if (parser != null) {
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        packageManager = pm;
                                        if (parser != null) {
                                        }
                                        throw th;
                                    }
                                }
                                int depth2;
                                if (type2) {
                                    pm = null;
                                    break;
                                } else if (type2) {
                                    try {
                                        if ("subtype".equals(parser.getName())) {
                                            sa = res.obtainAttributes(attrs, R.styleable.InputMethod_Subtype);
                                            depth2 = depth;
                                            packageManager = pm;
                                            try {
                                                res2 = res;
                                                depth = new InputMethodSubtypeBuilder().setSubtypeNameResId(sa.getResourceId(0, 0)).setSubtypeIconResId(sa.getResourceId(1, 0)).setLanguageTag(sa.getString(9)).setSubtypeLocale(sa.getString(2)).setSubtypeMode(sa.getString(3)).setSubtypeExtraValue(sa.getString(4)).setIsAuxiliary(sa.getBoolean(5, false)).setOverridesImplicitlyEnabledSubtype(sa.getBoolean(6, false)).setSubtypeId(sa.getInt(7, 0)).setIsAsciiCapable(sa.getBoolean(8, false)).build();
                                                if (depth.isAuxiliary()) {
                                                    isAuxIme2 = isAuxIme;
                                                } else {
                                                    isAuxIme2 = false;
                                                }
                                            } catch (NameNotFoundException | IndexOutOfBoundsException | NumberFormatException e2) {
                                                isAuxIme2 = isAuxIme;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Unable to create context for: ");
                                                stringBuilder.append(si.packageName);
                                                throw new XmlPullParserException(stringBuilder.toString());
                                            } catch (Throwable th4) {
                                                th = th4;
                                                if (parser != null) {
                                                }
                                                throw th;
                                            }
                                            try {
                                                subtypes.add(depth);
                                                sa = sa2;
                                                depth = depth2;
                                                pm = packageManager;
                                                res = res2;
                                            } catch (NameNotFoundException | IndexOutOfBoundsException | NumberFormatException e3) {
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Unable to create context for: ");
                                                stringBuilder.append(si.packageName);
                                                throw new XmlPullParserException(stringBuilder.toString());
                                            }
                                        }
                                        depth2 = depth;
                                        packageManager = pm;
                                        res2 = res;
                                        throw new XmlPullParserException("Meta-data in input-method does not start with subtype tag");
                                    } catch (NameNotFoundException | IndexOutOfBoundsException | NumberFormatException e4) {
                                        packageManager = pm;
                                        isAuxIme2 = isAuxIme;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Unable to create context for: ");
                                        stringBuilder.append(si.packageName);
                                        throw new XmlPullParserException(stringBuilder.toString());
                                    } catch (Throwable th5) {
                                        th = th5;
                                        packageManager = pm;
                                        if (parser != null) {
                                        }
                                        throw th;
                                    }
                                } else {
                                    depth2 = depth;
                                    packageManager = pm;
                                    sa = sa2;
                                    isAuxIme2 = isAuxIme;
                                }
                            } catch (NameNotFoundException | IndexOutOfBoundsException | NumberFormatException e5) {
                                isAuxIme = isAuxIme2;
                                packageManager = pm;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unable to create context for: ");
                                stringBuilder.append(si.packageName);
                                throw new XmlPullParserException(stringBuilder.toString());
                            } catch (Throwable th6) {
                                th = th6;
                                isAuxIme = isAuxIme2;
                                packageManager = pm;
                                if (parser != null) {
                                }
                                throw th;
                            }
                        }
                        if (parser != null) {
                            parser.close();
                        }
                        next2 = isVrOnly;
                        if (subtypes.size() == 0) {
                            isAuxIme2 = false;
                        } else {
                            isAuxIme2 = isAuxIme;
                        }
                        if (list != null) {
                            depth = additionalSubtypes.size();
                            for (pm = 
/*
Method generation error in method: android.view.inputmethod.InputMethodInfo.<init>(android.content.Context, android.content.pm.ResolveInfo, java.util.List):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r7_9 'pm' android.content.pm.PackageManager) = (r7_2 'pm' android.content.pm.PackageManager), (r7_8 'pm' android.content.pm.PackageManager) binds: {(r7_2 'pm' android.content.pm.PackageManager)=B:25:0x0090, (r7_8 'pm' android.content.pm.PackageManager)=B:60:0x016c} in method: android.view.inputmethod.InputMethodInfo.<init>(android.content.Context, android.content.pm.ResolveInfo, java.util.List):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:185)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:280)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:65)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:280)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:65)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: PHI can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 44 more

*/

    InputMethodInfo(Parcel source) {
        this.mId = source.readString();
        this.mSettingsActivityName = source.readString();
        this.mIsDefaultResId = source.readInt();
        boolean z = true;
        this.mIsAuxIme = source.readInt() == 1;
        if (source.readInt() != 1) {
            z = false;
        }
        this.mSupportsSwitchingToNextInputMethod = z;
        this.mIsVrOnly = source.readBoolean();
        this.mService = (ResolveInfo) ResolveInfo.CREATOR.createFromParcel(source);
        this.mSubtypes = new InputMethodSubtypeArray(source);
        this.mForceDefault = false;
    }

    public InputMethodInfo(String packageName, String className, CharSequence label, String settingsActivity) {
        this(buildDummyResolveInfo(packageName, className, label), false, settingsActivity, null, 0, false, true, false);
    }

    public InputMethodInfo(ResolveInfo ri, boolean isAuxIme, String settingsActivity, List<InputMethodSubtype> subtypes, int isDefaultResId, boolean forceDefault) {
        this(ri, isAuxIme, settingsActivity, subtypes, isDefaultResId, forceDefault, true, false);
    }

    public InputMethodInfo(ResolveInfo ri, boolean isAuxIme, String settingsActivity, List<InputMethodSubtype> subtypes, int isDefaultResId, boolean forceDefault, boolean supportsSwitchingToNextInputMethod, boolean isVrOnly) {
        ServiceInfo si = ri.serviceInfo;
        this.mService = ri;
        this.mId = new ComponentName(si.packageName, si.name).flattenToShortString();
        this.mSettingsActivityName = settingsActivity;
        this.mIsDefaultResId = isDefaultResId;
        this.mIsAuxIme = isAuxIme;
        this.mSubtypes = new InputMethodSubtypeArray((List) subtypes);
        this.mForceDefault = forceDefault;
        this.mSupportsSwitchingToNextInputMethod = supportsSwitchingToNextInputMethod;
        this.mIsVrOnly = isVrOnly;
    }

    private static ResolveInfo buildDummyResolveInfo(String packageName, String className, CharSequence label) {
        ResolveInfo ri = new ResolveInfo();
        ServiceInfo si = new ServiceInfo();
        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = packageName;
        ai.enabled = true;
        si.applicationInfo = ai;
        si.enabled = true;
        si.packageName = packageName;
        si.name = className;
        si.exported = true;
        si.nonLocalizedLabel = label;
        ri.serviceInfo = si;
        return ri;
    }

    public String getId() {
        return this.mId;
    }

    public String getPackageName() {
        return this.mService.serviceInfo.packageName;
    }

    public String getServiceName() {
        return this.mService.serviceInfo.name;
    }

    public ServiceInfo getServiceInfo() {
        return this.mService.serviceInfo;
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mService.serviceInfo.packageName, this.mService.serviceInfo.name);
    }

    public CharSequence loadLabel(PackageManager pm) {
        return this.mService.loadLabel(pm);
    }

    public Drawable loadIcon(PackageManager pm) {
        return this.mService.loadIcon(pm);
    }

    public String getSettingsActivity() {
        return this.mSettingsActivityName;
    }

    public boolean isVrOnly() {
        return this.mIsVrOnly;
    }

    public int getSubtypeCount() {
        return this.mSubtypes.getCount();
    }

    public InputMethodSubtype getSubtypeAt(int index) {
        return this.mSubtypes.get(index);
    }

    public int getIsDefaultResourceId() {
        return this.mIsDefaultResId;
    }

    public boolean isDefault(Context context) {
        if (this.mForceDefault) {
            return true;
        }
        try {
            if (getIsDefaultResourceId() == 0) {
                return false;
            }
            return context.createPackageContext(getPackageName(), 0).getResources().getBoolean(getIsDefaultResourceId());
        } catch (NameNotFoundException | NotFoundException e) {
            return false;
        }
    }

    public void dump(Printer pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mId=");
        stringBuilder.append(this.mId);
        stringBuilder.append(" mSettingsActivityName=");
        stringBuilder.append(this.mSettingsActivityName);
        stringBuilder.append(" mIsVrOnly=");
        stringBuilder.append(this.mIsVrOnly);
        stringBuilder.append(" mSupportsSwitchingToNextInputMethod=");
        stringBuilder.append(this.mSupportsSwitchingToNextInputMethod);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mIsDefaultResId=0x");
        stringBuilder.append(Integer.toHexString(this.mIsDefaultResId));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("Service:");
        pw.println(stringBuilder.toString());
        ResolveInfo resolveInfo = this.mService;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append("  ");
        resolveInfo.dump(pw, stringBuilder2.toString());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("InputMethodInfo{");
        stringBuilder.append(this.mId);
        stringBuilder.append(", settings: ");
        stringBuilder.append(this.mSettingsActivityName);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !(o instanceof InputMethodInfo)) {
            return false;
        }
        return this.mId.equals(((InputMethodInfo) o).mId);
    }

    public int hashCode() {
        return this.mId.hashCode();
    }

    public boolean isAuxiliaryIme() {
        return this.mIsAuxIme;
    }

    public boolean supportsSwitchingToNextInputMethod() {
        return this.mSupportsSwitchingToNextInputMethod;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mId);
        dest.writeString(this.mSettingsActivityName);
        dest.writeInt(this.mIsDefaultResId);
        dest.writeInt(this.mIsAuxIme);
        dest.writeInt(this.mSupportsSwitchingToNextInputMethod);
        dest.writeBoolean(this.mIsVrOnly);
        this.mService.writeToParcel(dest, flags);
        this.mSubtypes.writeToParcel(dest);
    }

    public int describeContents() {
        return 0;
    }
}
