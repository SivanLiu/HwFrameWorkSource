package android.service.autofill;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.metrics.LogMaker;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.PrintWriter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class AutofillServiceInfo {
    private static final String TAG = "AutofillServiceInfo";
    private static final String TAG_AUTOFILL_SERVICE = "autofill-service";
    private static final String TAG_COMPATIBILITY_PACKAGE = "compatibility-package";
    private final ArrayMap<String, Long> mCompatibilityPackages;
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

    public AutofillServiceInfo(Context context, ComponentName comp, int userHandle) throws NameNotFoundException {
        this(context, getServiceInfoOrThrow(comp, userHandle));
    }

    public AutofillServiceInfo(Context context, ServiceInfo si) {
        if (!"android.permission.BIND_AUTOFILL_SERVICE".equals(si.permission)) {
            if ("android.permission.BIND_AUTOFILL".equals(si.permission)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AutofillService from '");
                stringBuilder.append(si.packageName);
                stringBuilder.append("' uses unsupported permission ");
                stringBuilder.append("android.permission.BIND_AUTOFILL");
                stringBuilder.append(". It works for now, but might not be supported on future releases");
                Log.w(str, stringBuilder.toString());
                new MetricsLogger().write(new LogMaker(1289).setPackageName(si.packageName));
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("AutofillService from '");
                stringBuilder2.append(si.packageName);
                stringBuilder2.append("' does not require permission ");
                stringBuilder2.append("android.permission.BIND_AUTOFILL_SERVICE");
                Log.w(TAG, stringBuilder2.toString());
                throw new SecurityException("Service does not require permission android.permission.BIND_AUTOFILL_SERVICE");
            }
        }
        this.mServiceInfo = si;
        XmlResourceParser parser = si.loadXmlMetaData(context.getPackageManager(), AutofillService.SERVICE_META_DATA);
        TypedArray afsAttributes = null;
        if (parser == null) {
            this.mSettingsActivity = null;
            this.mCompatibilityPackages = null;
            return;
        }
        String settingsActivity = null;
        ArrayMap<String, Long> compatibilityPackages = null;
        try {
            Resources resources = context.getPackageManager().getResourcesForApplication(si.applicationInfo);
            int type = 0;
            while (type != 1 && type != 2) {
                type = parser.next();
            }
            if (TAG_AUTOFILL_SERVICE.equals(parser.getName())) {
                afsAttributes = resources.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.AutofillService);
                settingsActivity = afsAttributes.getString(0);
                if (afsAttributes != null) {
                    afsAttributes.recycle();
                }
                compatibilityPackages = parseCompatibilityPackages(parser, resources);
            } else {
                Log.e(TAG, "Meta-data does not start with autofill-service tag");
            }
        } catch (NameNotFoundException | IOException | XmlPullParserException e) {
            Log.e(TAG, "Error parsing auto fill service meta-data", e);
        } catch (Throwable th) {
            if (afsAttributes != null) {
                afsAttributes.recycle();
            }
        }
        this.mSettingsActivity = settingsActivity;
        this.mCompatibilityPackages = compatibilityPackages;
    }

    /* JADX WARNING: Missing block: B:19:0x0058, code skipped:
            if (r2 != null) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:20:0x005a, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:31:0x0092, code skipped:
            if (r2 != null) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:36:0x00af, code skipped:
            if (r2 == null) goto L_0x00da;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ArrayMap<String, Long> parseCompatibilityPackages(XmlPullParser parser, Resources resources) throws IOException, XmlPullParserException {
        ArrayMap<String, Long> compatibilityPackages = null;
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type != 3) {
                if (type != 4) {
                    if (TAG_COMPATIBILITY_PACKAGE.equals(parser.getName())) {
                        TypedArray cpAttributes = null;
                        String str;
                        String str2;
                        StringBuilder stringBuilder;
                        try {
                            cpAttributes = resources.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.AutofillService_CompatibilityPackage);
                            String name = cpAttributes.getString(null);
                            if (TextUtils.isEmpty(name)) {
                                str = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Invalid compatibility package:");
                                stringBuilder2.append(name);
                                Log.e(str, stringBuilder2.toString());
                                XmlUtils.skipCurrentTag(parser);
                            } else {
                                Long maxVersionCode;
                                str = cpAttributes.getString(1);
                                if (str != null) {
                                    maxVersionCode = Long.valueOf(Long.parseLong(str));
                                    if (maxVersionCode.longValue() < 0) {
                                        str2 = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Invalid compatibility max version code:");
                                        stringBuilder.append(maxVersionCode);
                                        Log.e(str2, stringBuilder.toString());
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                } else {
                                    maxVersionCode = Long.valueOf(-1);
                                }
                                if (compatibilityPackages == null) {
                                    compatibilityPackages = new ArrayMap();
                                }
                                compatibilityPackages.put(name, maxVersionCode);
                                XmlUtils.skipCurrentTag(parser);
                                if (cpAttributes != null) {
                                    cpAttributes.recycle();
                                }
                            }
                        } catch (NumberFormatException e) {
                            str2 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Invalid compatibility max version code:");
                            stringBuilder.append(str);
                            Log.e(str2, stringBuilder.toString());
                            XmlUtils.skipCurrentTag(parser);
                        } catch (Throwable th) {
                            XmlUtils.skipCurrentTag(parser);
                            if (cpAttributes != null) {
                                cpAttributes.recycle();
                            }
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        return compatibilityPackages;
    }

    public ServiceInfo getServiceInfo() {
        return this.mServiceInfo;
    }

    public String getSettingsActivity() {
        return this.mSettingsActivity;
    }

    public ArrayMap<String, Long> getCompatibilityPackages() {
        return this.mCompatibilityPackages;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append("[");
        builder.append(this.mServiceInfo);
        builder.append(", settings:");
        builder.append(this.mSettingsActivity);
        builder.append(", hasCompatPckgs:");
        boolean z = (this.mCompatibilityPackages == null || this.mCompatibilityPackages.isEmpty()) ? false : true;
        builder.append(z);
        builder.append("]");
        return builder.toString();
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("Component: ");
        pw.println(getServiceInfo().getComponentName());
        pw.print(prefix);
        pw.print("Settings: ");
        pw.println(this.mSettingsActivity);
        pw.print(prefix);
        pw.print("Compat packages: ");
        pw.println(this.mCompatibilityPackages);
    }
}
