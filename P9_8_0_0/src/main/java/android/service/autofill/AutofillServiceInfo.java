package android.service.autofill;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.RemoteException;
import android.util.Log;

public final class AutofillServiceInfo {
    private static final String TAG = "AutofillServiceInfo";
    private final ServiceInfo mServiceInfo;
    private final String mSettingsActivity;

    private static ServiceInfo getServiceInfoOrThrow(ComponentName comp, int userHandle) throws NameNotFoundException {
        try {
            ServiceInfo si = AppGlobals.getPackageManager().getServiceInfo(comp, 128, userHandle);
            if (si != null) {
                return si;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "catch a RemoteException in function getServiceInfoOrThrow");
        }
        throw new NameNotFoundException(comp.toString());
    }

    public AutofillServiceInfo(PackageManager pm, ComponentName comp, int userHandle) throws NameNotFoundException {
        this(pm, getServiceInfoOrThrow(comp, userHandle));
    }

    public AutofillServiceInfo(PackageManager pm, ServiceInfo si) {
        this.mServiceInfo = si;
        TypedArray metaDataArray = getMetaDataArray(pm, si);
        if (metaDataArray != null) {
            this.mSettingsActivity = metaDataArray.getString(0);
            metaDataArray.recycle();
            return;
        }
        this.mSettingsActivity = null;
    }

    private static TypedArray getMetaDataArray(PackageManager pm, ServiceInfo si) {
        if ("android.permission.BIND_AUTOFILL_SERVICE".equals(si.permission) || ("android.permission.BIND_AUTOFILL".equals(si.permission) ^ 1) == 0) {
            XmlResourceParser parser = si.loadXmlMetaData(pm, AutofillService.SERVICE_META_DATA);
            if (parser == null) {
                return null;
            }
            while (true) {
                try {
                    int type = parser.next();
                    if (type != 1) {
                    }
                    
/*
Method generation error in method: android.service.autofill.AutofillServiceInfo.getMetaDataArray(android.content.pm.PackageManager, android.content.pm.ServiceInfo):android.content.res.TypedArray
jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x005e: IF  (r5_0 'type' int) == (2 int)  -> B:15:0x0060 in method: android.service.autofill.AutofillServiceInfo.getMetaDataArray(android.content.pm.PackageManager, android.content.pm.ServiceInfo):android.content.res.TypedArray
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:226)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:203)
	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:100)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:50)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:277)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:174)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:61)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:118)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:57)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:187)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:328)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:265)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:228)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:118)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:83)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:19)
	at jadx.core.ProcessClass.process(ProcessClass.java:40)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.CodegenException: IF can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:530)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:449)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:220)
	... 37 more

*/

                    public ServiceInfo getServiceInfo() {
                        return this.mServiceInfo;
                    }

                    public String getSettingsActivity() {
                        return this.mSettingsActivity;
                    }
                }
