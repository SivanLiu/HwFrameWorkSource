package com.android.server.appwidget;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.admin.DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener;
import android.appwidget.AppWidgetManagerInternal;
import android.appwidget.AppWidgetProviderInfo;
import android.appwidget.IHwAWSIDAMonitorCallback;
import android.appwidget.IHwAppWidgetManager;
import android.appwidget.PendingHostUpdate;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.FilterComparison;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.WindowManager;
import android.widget.RemoteViews;
import com.android.internal.app.SuspendedAppActivity;
import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.appwidget.IAppWidgetHost;
import com.android.internal.appwidget.IAppWidgetService.Stub;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.widget.IRemoteViewsFactory;
import com.android.server.AbsLocationManagerService;
import com.android.server.LocalServices;
import com.android.server.WidgetBackupProvider;
import com.android.server.display.DisplayTransformManager;
import com.android.server.policy.IconUtilities;
import com.android.server.utils.PriorityDump;
import com.huawei.pgmng.log.LogPower;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class AppWidgetServiceImpl extends Stub implements WidgetBackupProvider, OnCrossProfileWidgetProvidersChangeListener {
    private static final String COTA_APP_UPDATE_APPWIDGET = "huawei.intent.action.UPDATE_COTA_APP_WIDGET";
    private static final String COTA_APP_UPDATE_APPWIDGET_EXTRA = "huawei.intent.extra.cota_package_list";
    private static final int CURRENT_VERSION = 1;
    private static boolean DEBUG = false;
    private static HashMap<String, String> HIDDEN_WEATHER_WIDGETS = new HashMap();
    private static final boolean HIDE_HUAWEI_WEATHER_WIDGET = SystemProperties.getBoolean("ro.config.hide_weather_widget", false);
    private static final String HUAWEI_LAUNCHER_PACKAGE = "com.huawei.android.launcher";
    private static final int ID_PROVIDER_CHANGED = 1;
    private static final int ID_VIEWS_UPDATE = 0;
    private static final int KEYGUARD_HOST_ID = 1262836039;
    private static final int LOADED_PROFILE_ID = -1;
    private static final int MIN_UPDATE_PERIOD = (DEBUG ? 0 : 1800000);
    private static final String MUSlIM_APP_WIDGET_PACKAGE = "com.android.alarmclock.MuslimAppWidgetProvider";
    private static final String NEW_KEYGUARD_HOST_PACKAGE = "com.android.keyguard";
    private static final String OLD_KEYGUARD_HOST_PACKAGE = "android";
    private static final String STATE_FILENAME = "appwidgets.xml";
    private static final String TAG = "AppWidgetServiceImpl";
    private static final int TAG_UNDEFINED = -1;
    private static final int UNKNOWN_UID = -1;
    private static final int UNKNOWN_USER_ID = -10;
    private static final AtomicLong UPDATE_COUNTER = new AtomicLong();
    HwAWSIDAMonitorProxy mAWSIProxy = new HwAWSIDAMonitorProxy();
    private AlarmManager mAlarmManager;
    private AppOpsManager mAppOpsManager;
    private BackupRestoreController mBackupRestoreController;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (AppWidgetServiceImpl.DEBUG) {
                String str = AppWidgetServiceImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Received broadcast: ");
                stringBuilder.append(action);
                stringBuilder.append(" on user ");
                stringBuilder.append(userId);
                Slog.i(str, stringBuilder.toString());
            }
            boolean z = true;
            switch (action.hashCode()) {
                case -1238404651:
                    if (action.equals("android.intent.action.MANAGED_PROFILE_UNAVAILABLE")) {
                        z = true;
                        break;
                    }
                    break;
                case -1001645458:
                    if (action.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                        z = true;
                        break;
                    }
                    break;
                case -864107122:
                    if (action.equals("android.intent.action.MANAGED_PROFILE_AVAILABLE")) {
                        z = true;
                        break;
                    }
                    break;
                case 158859398:
                    if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                        z = false;
                        break;
                    }
                    break;
                case 1290767157:
                    if (action.equals("android.intent.action.PACKAGES_UNSUSPENDED")) {
                        z = true;
                        break;
                    }
                    break;
            }
            switch (z) {
                case false:
                    AppWidgetServiceImpl.this.onConfigurationChanged();
                    return;
                case true:
                case true:
                    synchronized (AppWidgetServiceImpl.this.mLock) {
                        AppWidgetServiceImpl.this.reloadWidgetsMaskedState(userId);
                    }
                    return;
                case true:
                    AppWidgetServiceImpl.this.onPackageBroadcastReceived(intent, getSendingUserId());
                    AppWidgetServiceImpl.this.updateWidgetPackageSuspensionMaskedState(intent, true, getSendingUserId());
                    return;
                case true:
                    AppWidgetServiceImpl.this.onPackageBroadcastReceived(intent, getSendingUserId());
                    AppWidgetServiceImpl.this.updateWidgetPackageSuspensionMaskedState(intent, false, getSendingUserId());
                    return;
                default:
                    AppWidgetServiceImpl.this.onPackageBroadcastReceived(intent, getSendingUserId());
                    return;
            }
        }
    };
    private Handler mCallbackHandler;
    private final Context mContext;
    private DevicePolicyManagerInternal mDevicePolicyManagerInternal;
    private final ArrayList<Host> mHosts = new ArrayList();
    HwInnerAppWidgetService mHwInnerService = new HwInnerAppWidgetService(this);
    private IconUtilities mIconUtilities;
    private KeyguardManager mKeyguardManager;
    private final SparseIntArray mLoadedUserIds = new SparseIntArray();
    private Locale mLocale;
    private final Object mLock = new Object();
    private int mMaxWidgetBitmapMemory;
    private final SparseIntArray mNextAppWidgetIds = new SparseIntArray();
    private IPackageManager mPackageManager;
    private PackageManagerInternal mPackageManagerInternal;
    private final ArraySet<Pair<Integer, String>> mPackagesWithBindWidgetPermission = new ArraySet();
    private final ArrayList<Provider> mProviders = new ArrayList();
    private final HashMap<Pair<Integer, FilterComparison>, HashSet<Integer>> mRemoteViewsServicesAppWidgets = new HashMap();
    private boolean mSafeMode;
    private Handler mSaveStateHandler;
    private SecurityPolicy mSecurityPolicy;
    private UserManager mUserManager;
    private final SparseArray<ArraySet<String>> mWidgetPackages = new SparseArray();
    protected final ArrayList<Widget> mWidgets = new ArrayList();

    private class AppWidgetManagerLocal extends AppWidgetManagerInternal {
        private AppWidgetManagerLocal() {
        }

        /* synthetic */ AppWidgetManagerLocal(AppWidgetServiceImpl x0, AnonymousClass1 x1) {
            this();
        }

        public ArraySet<String> getHostedWidgetPackages(int uid) {
            ArraySet<String> widgetPackages;
            synchronized (AppWidgetServiceImpl.this.mLock) {
                widgetPackages = null;
                int widgetCount = AppWidgetServiceImpl.this.mWidgets.size();
                for (int i = 0; i < widgetCount; i++) {
                    Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                    if (widget.host.id.uid == uid) {
                        if (widgetPackages == null) {
                            widgetPackages = new ArraySet();
                        }
                        widgetPackages.add(widget.provider.id.componentName.getPackageName());
                    }
                }
            }
            return widgetPackages;
        }
    }

    private final class BackupRestoreController {
        private static final boolean DEBUG = true;
        private static final String TAG = "BackupRestoreController";
        private static final int WIDGET_STATE_VERSION = 2;
        private final HashSet<String> mPrunedApps;
        private final HashMap<Host, ArrayList<RestoreUpdateRecord>> mUpdatesByHost;
        private final HashMap<Provider, ArrayList<RestoreUpdateRecord>> mUpdatesByProvider;

        private class RestoreUpdateRecord {
            public int newId;
            public boolean notified = false;
            public int oldId;

            public RestoreUpdateRecord(int theOldId, int theNewId) {
                this.oldId = theOldId;
                this.newId = theNewId;
            }
        }

        private BackupRestoreController() {
            this.mPrunedApps = new HashSet();
            this.mUpdatesByProvider = new HashMap();
            this.mUpdatesByHost = new HashMap();
        }

        /* synthetic */ BackupRestoreController(AppWidgetServiceImpl x0, AnonymousClass1 x1) {
            this();
        }

        public List<String> getWidgetParticipants(int userId) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Getting widget participants for user: ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
            HashSet<String> packages = new HashSet();
            synchronized (AppWidgetServiceImpl.this.mLock) {
                int N = AppWidgetServiceImpl.this.mWidgets.size();
                for (int i = 0; i < N; i++) {
                    Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                    if (isProviderAndHostInUser(widget, userId)) {
                        packages.add(widget.host.id.packageName);
                        Provider provider = widget.provider;
                        if (provider != null) {
                            packages.add(provider.id.componentName.getPackageName());
                        }
                    }
                }
            }
            return new ArrayList(packages);
        }

        public byte[] getWidgetState(String backedupPackage, int userId) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Getting widget state for user: ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            synchronized (AppWidgetServiceImpl.this.mLock) {
                if (packageNeedsWidgetBackupLocked(backedupPackage, userId)) {
                    try {
                        int i;
                        Provider provider;
                        XmlSerializer out = new FastXmlSerializer();
                        out.setOutput(stream, StandardCharsets.UTF_8.name());
                        out.startDocument(null, Boolean.valueOf(true));
                        out.startTag(null, "ws");
                        out.attribute(null, "version", String.valueOf(2));
                        out.attribute(null, AbsLocationManagerService.DEL_PKG, backedupPackage);
                        int N = AppWidgetServiceImpl.this.mProviders.size();
                        int i2 = 0;
                        int index = 0;
                        for (i = 0; i < N; i++) {
                            provider = (Provider) AppWidgetServiceImpl.this.mProviders.get(i);
                            if (provider.shouldBePersisted() && (provider.isInPackageForUser(backedupPackage, userId) || provider.hostedByPackageForUser(backedupPackage, userId))) {
                                provider.tag = index;
                                AppWidgetServiceImpl.serializeProvider(out, provider);
                                index++;
                            }
                        }
                        i = AppWidgetServiceImpl.this.mHosts.size();
                        index = 0;
                        for (N = 0; N < i; N++) {
                            Host host = (Host) AppWidgetServiceImpl.this.mHosts.get(N);
                            if (!host.widgets.isEmpty() && (host.isInPackageForUser(backedupPackage, userId) || host.hostsPackageForUser(backedupPackage, userId))) {
                                host.tag = index;
                                AppWidgetServiceImpl.serializeHost(out, host);
                                index++;
                            }
                        }
                        i = AppWidgetServiceImpl.this.mWidgets.size();
                        while (true) {
                            N = i2;
                            if (N < i) {
                                Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(N);
                                provider = widget.provider;
                                if (widget.host.isInPackageForUser(backedupPackage, userId) || (provider != null && provider.isInPackageForUser(backedupPackage, userId))) {
                                    AppWidgetServiceImpl.serializeAppWidget(out, widget);
                                }
                                i2 = N + 1;
                            } else {
                                out.endTag(null, "ws");
                                out.endDocument();
                                return stream.toByteArray();
                            }
                        }
                    } catch (IOException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unable to save widget state for ");
                        stringBuilder2.append(backedupPackage);
                        Slog.w(str2, stringBuilder2.toString());
                        return null;
                    }
                }
                return null;
            }
        }

        public void restoreStarting(int userId) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Restore starting for user: ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
            synchronized (AppWidgetServiceImpl.this.mLock) {
                this.mPrunedApps.clear();
                this.mUpdatesByProvider.clear();
                this.mUpdatesByHost.clear();
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:113:0x02f0 A:{Splitter: B:111:0x02ef, ExcHandler: org.xmlpull.v1.XmlPullParserException (e org.xmlpull.v1.XmlPullParserException)} */
        /* JADX WARNING: Removed duplicated region for block: B:117:0x02f8 A:{Splitter: B:1:0x002c, ExcHandler: org.xmlpull.v1.XmlPullParserException (e org.xmlpull.v1.XmlPullParserException)} */
        /* JADX WARNING: Removed duplicated region for block: B:113:0x02f0 A:{Splitter: B:111:0x02ef, ExcHandler: org.xmlpull.v1.XmlPullParserException (e org.xmlpull.v1.XmlPullParserException)} */
        /* JADX WARNING: Missing block: B:118:0x02f9, code:
            r17 = r5;
     */
        /* JADX WARNING: Missing block: B:120:?, code:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("Unable to restore widget state for ");
            r5.append(r2);
            android.util.Slog.w(r4, r5.toString());
     */
        /* JADX WARNING: Missing block: B:123:0x0318, code:
            r0 = th;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void restoreWidgetState(String packageName, byte[] restoredState, int userId) {
            Throwable type;
            String str = packageName;
            int i = userId;
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Restoring widget state for user:");
            stringBuilder.append(i);
            stringBuilder.append(" package: ");
            stringBuilder.append(str);
            Slog.i(str2, stringBuilder.toString());
            ByteArrayInputStream stream = new ByteArrayInputStream(restoredState);
            ByteArrayInputStream stream2;
            try {
                ArrayList<Provider> restoredProviders = new ArrayList();
                ArrayList<Host> restoredHosts = new ArrayList();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, StandardCharsets.UTF_8.name());
                synchronized (AppWidgetServiceImpl.this.mLock) {
                    while (true) {
                        ArrayList<Provider> arrayList;
                        ArrayList<Host> arrayList2;
                        try {
                            int type2 = parser.next();
                            if (type2 == 2) {
                                String tag = parser.getName();
                                String str3;
                                if ("ws".equals(tag)) {
                                    try {
                                        String version = parser.getAttributeValue(null, "version");
                                        if (Integer.parseInt(version) > 2) {
                                            String str4 = TAG;
                                            StringBuilder stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Unable to process state version ");
                                            stringBuilder2.append(version);
                                            Slog.w(str4, stringBuilder2.toString());
                                            AppWidgetServiceImpl.this.saveGroupStateAsync(i);
                                            return;
                                        } else if (str.equals(parser.getAttributeValue(null, AbsLocationManagerService.DEL_PKG))) {
                                            stream2 = stream;
                                        } else {
                                            Slog.w(TAG, "Package mismatch in ws");
                                            AppWidgetServiceImpl.this.saveGroupStateAsync(i);
                                            return;
                                        }
                                    } catch (Throwable th) {
                                        type = th;
                                        stream2 = stream;
                                        try {
                                            throw type;
                                        } catch (XmlPullParserException e) {
                                        }
                                    }
                                } else if ("p".equals(tag)) {
                                    try {
                                        Provider p;
                                        ComponentName componentName = new ComponentName(parser.getAttributeValue(null, AbsLocationManagerService.DEL_PKG), parser.getAttributeValue(null, "cl"));
                                        Provider p2 = findProviderLocked(componentName, i);
                                        if (p2 == null) {
                                            p = new Provider();
                                            stream2 = stream;
                                            try {
                                                p.id = new ProviderId(-1, componentName, null);
                                                p.info = new AppWidgetProviderInfo();
                                                p.info.provider = componentName;
                                                p.zombie = true;
                                                AppWidgetServiceImpl.this.mProviders.add(p);
                                            } catch (Throwable th2) {
                                                type = th2;
                                            }
                                        } else {
                                            stream2 = stream;
                                            p = p2;
                                        }
                                        str3 = TAG;
                                        stream = new StringBuilder();
                                        stream.append("   provider ");
                                        stream.append(p.id);
                                        Slog.i(str3, stream.toString());
                                        restoredProviders.add(p);
                                    } catch (Throwable th3) {
                                        type = th3;
                                        stream2 = stream;
                                        arrayList = restoredProviders;
                                        arrayList2 = restoredHosts;
                                        throw type;
                                    }
                                } else {
                                    stream2 = stream;
                                    try {
                                        String str5;
                                        StringBuilder stringBuilder3;
                                        if ("h".equals(tag)) {
                                            str3 = parser.getAttributeValue(null, AbsLocationManagerService.DEL_PKG);
                                            Host h = AppWidgetServiceImpl.this.lookupOrAddHostLocked(new HostId(AppWidgetServiceImpl.this.getUidForPackage(str3, i), Integer.parseInt(parser.getAttributeValue(null, "id"), 16), str3));
                                            restoredHosts.add(h);
                                            str5 = TAG;
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("   host[");
                                            stringBuilder3.append(restoredHosts.size());
                                            stringBuilder3.append("]: {");
                                            stringBuilder3.append(h.id);
                                            stringBuilder3.append("}");
                                            Slog.i(str5, stringBuilder3.toString());
                                        } else if ("g".equals(tag)) {
                                            int restoredId = Integer.parseInt(parser.getAttributeValue(null, "id"), 16);
                                            Host host = (Host) restoredHosts.get(Integer.parseInt(parser.getAttributeValue(null, "h"), 16));
                                            Provider p3 = null;
                                            str5 = parser.getAttributeValue(null, "p");
                                            if (str5 != null) {
                                                p3 = (Provider) restoredProviders.get(Integer.parseInt(str5, 16));
                                            }
                                            if (AppWidgetServiceImpl.HUAWEI_LAUNCHER_PACKAGE.equals(host.id.packageName) != null) {
                                                try {
                                                    stream = TAG;
                                                    stringBuilder3 = new StringBuilder();
                                                    arrayList = restoredProviders;
                                                    try {
                                                        stringBuilder3.append("Skip restore widget state in huawei launcher host for package: ");
                                                        stringBuilder3.append(str);
                                                        Slog.i(stream, stringBuilder3.toString());
                                                        arrayList2 = restoredHosts;
                                                    } catch (Throwable th4) {
                                                        type = th4;
                                                    }
                                                } catch (Throwable th5) {
                                                    type = th5;
                                                    arrayList = restoredProviders;
                                                    arrayList2 = restoredHosts;
                                                }
                                            } else {
                                                String str6;
                                                StringBuilder stringBuilder4;
                                                arrayList = restoredProviders;
                                                pruneWidgetStateLocked(host.id.packageName, i);
                                                if (p3 != null) {
                                                    pruneWidgetStateLocked(p3.id.componentName.getPackageName(), i);
                                                }
                                                stream = findRestoredWidgetLocked(restoredId, host, p3);
                                                if (stream == null) {
                                                    stream = new Widget();
                                                    stream.appWidgetId = AppWidgetServiceImpl.this.incrementAndGetAppWidgetIdLocked(i);
                                                    stream.restoredId = restoredId;
                                                    stream.options = parseWidgetIdOptions(parser);
                                                    stream.host = host;
                                                    stream.host.widgets.add(stream);
                                                    stream.provider = p3;
                                                    if (stream.provider != null) {
                                                        stream.provider.widgets.add(stream);
                                                    }
                                                    try {
                                                        str6 = TAG;
                                                        stringBuilder3 = new StringBuilder();
                                                        arrayList2 = restoredHosts;
                                                        stringBuilder3.append("New restored id ");
                                                        stringBuilder3.append(restoredId);
                                                        stringBuilder3.append(" now ");
                                                        stringBuilder3.append(stream);
                                                        Slog.i(str6, stringBuilder3.toString());
                                                        AppWidgetServiceImpl.this.addWidgetLocked(stream);
                                                    } catch (Throwable th6) {
                                                        type = th6;
                                                    }
                                                } else {
                                                    arrayList2 = restoredHosts;
                                                }
                                                if (stream.provider == null || stream.provider.info == null) {
                                                    str6 = TAG;
                                                    stringBuilder4 = new StringBuilder();
                                                    stringBuilder4.append("Missing provider for restored widget ");
                                                    stringBuilder4.append(stream);
                                                    Slog.w(str6, stringBuilder4.toString());
                                                } else {
                                                    stashProviderRestoreUpdateLocked(stream.provider, restoredId, stream.appWidgetId);
                                                }
                                                stashHostRestoreUpdateLocked(stream.host, restoredId, stream.appWidgetId);
                                                str6 = TAG;
                                                stringBuilder4 = new StringBuilder();
                                                stringBuilder4.append("   instance: ");
                                                stringBuilder4.append(restoredId);
                                                stringBuilder4.append(" -> ");
                                                stringBuilder4.append(stream.appWidgetId);
                                                stringBuilder4.append(" :: p=");
                                                stringBuilder4.append(stream.provider);
                                                Slog.i(str6, stringBuilder4.toString());
                                            }
                                        } else {
                                            arrayList = restoredProviders;
                                            arrayList2 = restoredHosts;
                                        }
                                    } catch (Throwable th7) {
                                        type = th7;
                                        arrayList = restoredProviders;
                                        arrayList2 = restoredHosts;
                                    }
                                }
                                arrayList = restoredProviders;
                                arrayList2 = restoredHosts;
                            } else {
                                stream2 = stream;
                                arrayList = restoredProviders;
                                arrayList2 = restoredHosts;
                            }
                            if (type2 != 1) {
                                stream = stream2;
                                restoredProviders = arrayList;
                                restoredHosts = arrayList2;
                                byte[] bArr = restoredState;
                            }
                        } catch (Throwable th8) {
                            type = th8;
                            stream2 = stream;
                            arrayList = restoredProviders;
                            arrayList2 = restoredHosts;
                        }
                    }
                    AppWidgetServiceImpl.this.saveGroupStateAsync(i);
                }
            } catch (XmlPullParserException e2) {
            } catch (Throwable th9) {
                type = th9;
                stream2 = stream;
                AppWidgetServiceImpl.this.saveGroupStateAsync(i);
                throw type;
            }
        }

        public void restoreFinished(int userId) {
            int i = userId;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("restoreFinished for ");
            stringBuilder.append(i);
            Slog.i(str, stringBuilder.toString());
            UserHandle userHandle = new UserHandle(i);
            synchronized (AppWidgetServiceImpl.this.mLock) {
                int pending;
                int[] newIds;
                Iterator it = this.mUpdatesByProvider.entrySet().iterator();
                while (true) {
                    boolean z = true;
                    if (!it.hasNext()) {
                        break;
                    }
                    Entry<Provider, ArrayList<RestoreUpdateRecord>> e = (Entry) it.next();
                    Provider provider = (Provider) e.getKey();
                    ArrayList<RestoreUpdateRecord> updates = (ArrayList) e.getValue();
                    pending = countPendingUpdates(updates);
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Provider ");
                    stringBuilder2.append(provider);
                    stringBuilder2.append(" pending: ");
                    stringBuilder2.append(pending);
                    Slog.i(str2, stringBuilder2.toString());
                    if (pending > 0) {
                        int[] oldIds = new int[pending];
                        newIds = new int[pending];
                        int N = updates.size();
                        int nextPending = 0;
                        int i2 = 0;
                        while (true) {
                            int i3 = i2;
                            if (i3 >= N) {
                                break;
                            }
                            RestoreUpdateRecord r = (RestoreUpdateRecord) updates.get(i3);
                            if (!r.notified) {
                                r.notified = z;
                                oldIds[nextPending] = r.oldId;
                                newIds[nextPending] = r.newId;
                                nextPending++;
                                String str3 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("   ");
                                stringBuilder3.append(r.oldId);
                                stringBuilder3.append(" => ");
                                stringBuilder3.append(r.newId);
                                Slog.i(str3, stringBuilder3.toString());
                            }
                            i2 = i3 + 1;
                            z = true;
                        }
                        sendWidgetRestoreBroadcastLocked("android.appwidget.action.APPWIDGET_RESTORED", provider, null, oldIds, newIds, userHandle);
                    }
                }
                for (Entry<Host, ArrayList<RestoreUpdateRecord>> e2 : this.mUpdatesByHost.entrySet()) {
                    Host host = (Host) e2.getKey();
                    if (host.id.uid != -1) {
                        ArrayList<RestoreUpdateRecord> updates2 = (ArrayList) e2.getValue();
                        int pending2 = countPendingUpdates(updates2);
                        String str4 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Host ");
                        stringBuilder4.append(host);
                        stringBuilder4.append(" pending: ");
                        stringBuilder4.append(pending2);
                        Slog.i(str4, stringBuilder4.toString());
                        if (pending2 > 0) {
                            newIds = new int[pending2];
                            int[] newIds2 = new int[pending2];
                            pending = updates2.size();
                            int nextPending2 = 0;
                            for (int i4 = 0; i4 < pending; i4++) {
                                RestoreUpdateRecord r2 = (RestoreUpdateRecord) updates2.get(i4);
                                if (!r2.notified) {
                                    r2.notified = true;
                                    newIds[nextPending2] = r2.oldId;
                                    newIds2[nextPending2] = r2.newId;
                                    nextPending2++;
                                    String str5 = TAG;
                                    StringBuilder stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("   ");
                                    stringBuilder5.append(r2.oldId);
                                    stringBuilder5.append(" => ");
                                    stringBuilder5.append(r2.newId);
                                    Slog.i(str5, stringBuilder5.toString());
                                }
                            }
                            int i5 = pending;
                            sendWidgetRestoreBroadcastLocked("android.appwidget.action.APPWIDGET_HOST_RESTORED", null, host, newIds, newIds2, userHandle);
                            i = userId;
                        }
                    }
                    i = userId;
                }
            }
        }

        private Provider findProviderLocked(ComponentName componentName, int userId) {
            int providerCount = AppWidgetServiceImpl.this.mProviders.size();
            for (int i = 0; i < providerCount; i++) {
                Provider provider = (Provider) AppWidgetServiceImpl.this.mProviders.get(i);
                if (provider.getUserId() == userId && provider.id.componentName.equals(componentName)) {
                    return provider;
                }
            }
            return null;
        }

        private Widget findRestoredWidgetLocked(int restoredId, Host host, Provider p) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Find restored widget: id=");
            stringBuilder.append(restoredId);
            stringBuilder.append(" host=");
            stringBuilder.append(host);
            stringBuilder.append(" provider=");
            stringBuilder.append(p);
            Slog.i(str, stringBuilder.toString());
            if (p == null || host == null) {
                return null;
            }
            int N = AppWidgetServiceImpl.this.mWidgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                if (widget.restoredId == restoredId && widget.host.id.equals(host.id) && widget.provider.id.equals(p.id)) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("   Found at ");
                    stringBuilder2.append(i);
                    stringBuilder2.append(" : ");
                    stringBuilder2.append(widget);
                    Slog.i(str, stringBuilder2.toString());
                    return widget;
                }
            }
            return null;
        }

        private boolean packageNeedsWidgetBackupLocked(String packageName, int userId) {
            int N = AppWidgetServiceImpl.this.mWidgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                if (isProviderAndHostInUser(widget, userId)) {
                    if (widget.host.isInPackageForUser(packageName, userId)) {
                        return true;
                    }
                    Provider provider = widget.provider;
                    if (provider != null && provider.isInPackageForUser(packageName, userId)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void stashProviderRestoreUpdateLocked(Provider provider, int oldId, int newId) {
            ArrayList<RestoreUpdateRecord> r = (ArrayList) this.mUpdatesByProvider.get(provider);
            if (r == null) {
                r = new ArrayList();
                this.mUpdatesByProvider.put(provider, r);
            } else if (alreadyStashed(r, oldId, newId)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ID remap ");
                stringBuilder.append(oldId);
                stringBuilder.append(" -> ");
                stringBuilder.append(newId);
                stringBuilder.append(" already stashed for ");
                stringBuilder.append(provider);
                Slog.i(str, stringBuilder.toString());
                return;
            }
            r.add(new RestoreUpdateRecord(oldId, newId));
        }

        private boolean alreadyStashed(ArrayList<RestoreUpdateRecord> stash, int oldId, int newId) {
            int N = stash.size();
            for (int i = 0; i < N; i++) {
                RestoreUpdateRecord r = (RestoreUpdateRecord) stash.get(i);
                if (r.oldId == oldId && r.newId == newId) {
                    return true;
                }
            }
            return false;
        }

        private void stashHostRestoreUpdateLocked(Host host, int oldId, int newId) {
            ArrayList<RestoreUpdateRecord> r = (ArrayList) this.mUpdatesByHost.get(host);
            if (r == null) {
                r = new ArrayList();
                this.mUpdatesByHost.put(host, r);
            } else if (alreadyStashed(r, oldId, newId)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ID remap ");
                stringBuilder.append(oldId);
                stringBuilder.append(" -> ");
                stringBuilder.append(newId);
                stringBuilder.append(" already stashed for ");
                stringBuilder.append(host);
                Slog.i(str, stringBuilder.toString());
                return;
            }
            r.add(new RestoreUpdateRecord(oldId, newId));
        }

        private void sendWidgetRestoreBroadcastLocked(String action, Provider provider, Host host, int[] oldIds, int[] newIds, UserHandle userHandle) {
            Intent intent = new Intent(action);
            intent.putExtra("appWidgetOldIds", oldIds);
            intent.putExtra("appWidgetIds", newIds);
            if (provider != null) {
                intent.setComponent(provider.info.provider);
                AppWidgetServiceImpl.this.sendBroadcastAsUser(intent, userHandle);
            }
            if (host != null) {
                intent.setComponent(null);
                intent.setPackage(host.id.packageName);
                intent.putExtra("hostId", host.id.hostId);
                AppWidgetServiceImpl.this.sendBroadcastAsUser(intent, userHandle);
            }
        }

        private void pruneWidgetStateLocked(String pkg, int userId) {
            String str;
            StringBuilder stringBuilder;
            if (this.mPrunedApps.contains(pkg)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("already pruned ");
                stringBuilder.append(pkg);
                stringBuilder.append(", continuing normally");
                Slog.i(str, stringBuilder.toString());
                return;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("pruning widget state for restoring package ");
            stringBuilder.append(pkg);
            Slog.i(str, stringBuilder.toString());
            for (int i = AppWidgetServiceImpl.this.mWidgets.size() - 1; i >= 0; i--) {
                Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                Host host = widget.host;
                Provider provider = widget.provider;
                if (host.hostsPackageForUser(pkg, userId) || (provider != null && provider.isInPackageForUser(pkg, userId))) {
                    host.widgets.remove(widget);
                    provider.widgets.remove(widget);
                    AppWidgetServiceImpl.this.decrementAppWidgetServiceRefCount(widget);
                    AppWidgetServiceImpl.this.removeWidgetLocked(widget);
                }
            }
            this.mPrunedApps.add(pkg);
        }

        private boolean isProviderAndHostInUser(Widget widget, int userId) {
            return widget.host.getUserId() == userId && (widget.provider == null || widget.provider.getUserId() == userId);
        }

        private Bundle parseWidgetIdOptions(XmlPullParser parser) {
            Bundle options = new Bundle();
            String minWidthString = parser.getAttributeValue(null, "min_width");
            if (minWidthString != null) {
                options.putInt("appWidgetMinWidth", Integer.parseInt(minWidthString, 16));
            }
            String minHeightString = parser.getAttributeValue(null, "min_height");
            if (minHeightString != null) {
                options.putInt("appWidgetMinHeight", Integer.parseInt(minHeightString, 16));
            }
            String maxWidthString = parser.getAttributeValue(null, "max_width");
            if (maxWidthString != null) {
                options.putInt("appWidgetMaxWidth", Integer.parseInt(maxWidthString, 16));
            }
            String maxHeightString = parser.getAttributeValue(null, "max_height");
            if (maxHeightString != null) {
                options.putInt("appWidgetMaxHeight", Integer.parseInt(maxHeightString, 16));
            }
            String categoryString = parser.getAttributeValue(null, "host_category");
            if (categoryString != null) {
                options.putInt("appWidgetCategory", Integer.parseInt(categoryString, 16));
            }
            return options;
        }

        private int countPendingUpdates(ArrayList<RestoreUpdateRecord> updates) {
            int pending = 0;
            int N = updates.size();
            for (int i = 0; i < N; i++) {
                if (!((RestoreUpdateRecord) updates.get(i)).notified) {
                    pending++;
                }
            }
            return pending;
        }
    }

    private final class CallbackHandler extends Handler {
        public static final int MSG_NOTIFY_PROVIDERS_CHANGED = 3;
        public static final int MSG_NOTIFY_PROVIDER_CHANGED = 2;
        public static final int MSG_NOTIFY_RECYCLE_REMOTE_VIEW = 20;
        public static final int MSG_NOTIFY_UPDATE_APP_WIDGET = 1;
        public static final int MSG_NOTIFY_VIEW_DATA_CHANGED = 4;

        public CallbackHandler(Looper looper) {
            super(looper, null, false);
        }

        public void handleMessage(Message message) {
            int i = message.what;
            SomeArgs args;
            if (i != 20) {
                Host host;
                IAppWidgetHost callbacks;
                long requestId;
                int appWidgetId;
                switch (i) {
                    case 1:
                        args = (SomeArgs) message.obj;
                        host = (Host) args.arg1;
                        callbacks = (IAppWidgetHost) args.arg2;
                        RemoteViews views = args.arg3;
                        requestId = ((Long) args.arg4).longValue();
                        appWidgetId = args.argi1;
                        args.recycle();
                        AppWidgetServiceImpl.this.handleNotifyUpdateAppWidget(host, callbacks, appWidgetId, views, requestId);
                        return;
                    case 2:
                        args = (SomeArgs) message.obj;
                        host = (Host) args.arg1;
                        callbacks = (IAppWidgetHost) args.arg2;
                        AppWidgetProviderInfo info = args.arg3;
                        requestId = ((Long) args.arg4).longValue();
                        appWidgetId = args.argi1;
                        args.recycle();
                        AppWidgetServiceImpl.this.handleNotifyProviderChanged(host, callbacks, appWidgetId, info, requestId);
                        return;
                    case 3:
                        args = (SomeArgs) message.obj;
                        host = (Host) args.arg1;
                        IAppWidgetHost callbacks2 = args.arg2;
                        args.recycle();
                        AppWidgetServiceImpl.this.handleNotifyProvidersChanged(host, callbacks2);
                        return;
                    case 4:
                        args = message.obj;
                        host = args.arg1;
                        callbacks = args.arg2;
                        long requestId2 = ((Long) args.arg3).longValue();
                        int appWidgetId2 = args.argi1;
                        appWidgetId = args.argi2;
                        args.recycle();
                        AppWidgetServiceImpl.this.handleNotifyAppWidgetViewDataChanged(host, callbacks, appWidgetId2, appWidgetId, requestId2);
                        return;
                    default:
                        return;
                }
            }
            args = (SomeArgs) message.obj;
            RemoteViews views2 = args.arg1;
            if (views2 != null) {
                views2.recycle();
            }
            args.recycle();
        }
    }

    private static final class Host {
        IAppWidgetHost callbacks;
        HostId id;
        long lastWidgetUpdateSequenceNo;
        int tag;
        ArrayList<Widget> widgets;
        boolean zombie;

        private Host() {
            this.widgets = new ArrayList();
            this.tag = -1;
        }

        /* synthetic */ Host(AnonymousClass1 x0) {
            this();
        }

        public int getUserId() {
            return UserHandle.getUserId(this.id.uid);
        }

        public boolean isInPackageForUser(String packageName, int userId) {
            return getUserId() == userId && this.id.packageName.equals(packageName);
        }

        private boolean hostsPackageForUser(String pkg, int userId) {
            int N = this.widgets.size();
            for (int i = 0; i < N; i++) {
                Provider provider = ((Widget) this.widgets.get(i)).provider;
                if (provider != null && provider.getUserId() == userId && provider.info != null && pkg.equals(provider.info.provider.getPackageName())) {
                    return true;
                }
            }
            return false;
        }

        public boolean getPendingUpdatesForId(int appWidgetId, LongSparseArray<PendingHostUpdate> outUpdates) {
            long updateSequenceNo = this.lastWidgetUpdateSequenceNo;
            int N = this.widgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = (Widget) this.widgets.get(i);
                if (widget.appWidgetId == appWidgetId) {
                    outUpdates.clear();
                    for (int j = widget.updateSequenceNos.size() - 1; j >= 0; j--) {
                        long requestId = widget.updateSequenceNos.valueAt(j);
                        if (requestId > updateSequenceNo) {
                            PendingHostUpdate update;
                            int id = widget.updateSequenceNos.keyAt(j);
                            switch (id) {
                                case 0:
                                    update = PendingHostUpdate.updateAppWidget(appWidgetId, AppWidgetServiceImpl.cloneIfLocalBinder(widget.getEffectiveViewsLocked()));
                                    break;
                                case 1:
                                    update = PendingHostUpdate.providerChanged(appWidgetId, widget.provider.info);
                                    break;
                                default:
                                    update = PendingHostUpdate.viewDataChanged(appWidgetId, id);
                                    break;
                            }
                            outUpdates.put(requestId, update);
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Host{");
            stringBuilder.append(this.id);
            stringBuilder.append(this.zombie ? " Z" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    private static final class HostId {
        final int hostId;
        final String packageName;
        final int uid;

        public HostId(int uid, int hostId, String packageName) {
            this.uid = uid;
            this.hostId = hostId;
            this.packageName = packageName;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HostId other = (HostId) obj;
            if (this.uid != other.uid || this.hostId != other.hostId) {
                return false;
            }
            if (this.packageName == null) {
                if (other.packageName != null) {
                    return false;
                }
            } else if (!this.packageName.equals(other.packageName)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            return (31 * ((31 * this.uid) + this.hostId)) + (this.packageName != null ? this.packageName.hashCode() : 0);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HostId{user:");
            stringBuilder.append(UserHandle.getUserId(this.uid));
            stringBuilder.append(", app:");
            stringBuilder.append(UserHandle.getAppId(this.uid));
            stringBuilder.append(", hostId:");
            stringBuilder.append(this.hostId);
            stringBuilder.append(", pkg:");
            stringBuilder.append(this.packageName);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    public class HwInnerAppWidgetService extends IHwAppWidgetManager.Stub {
        AppWidgetServiceImpl mAWSI;

        HwInnerAppWidgetService(AppWidgetServiceImpl service) {
            this.mAWSI = service;
        }

        private boolean checkSystemPermission() {
            int uid = UserHandle.getAppId(Binder.getCallingUid());
            if (uid == 1000) {
                return true;
            }
            String str = AppWidgetServiceImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("process permission error! uid:");
            stringBuilder.append(uid);
            Slog.e(str, stringBuilder.toString());
            return false;
        }

        public boolean registerAWSIMonitorCallback(IHwAWSIDAMonitorCallback callback) {
            if (!checkSystemPermission()) {
                return false;
            }
            AppWidgetServiceImpl.this.mAWSIProxy.registerAWSIMonitorCallback(callback);
            return true;
        }
    }

    private class LoadedWidgetState {
        final int hostTag;
        final int providerTag;
        final Widget widget;

        public LoadedWidgetState(Widget widget, int hostTag, int providerTag) {
            this.widget = widget;
            this.hostTag = hostTag;
            this.providerTag = providerTag;
        }
    }

    protected static final class Provider {
        PendingIntent broadcast;
        ProviderId id;
        AppWidgetProviderInfo info;
        String infoTag;
        boolean maskedByLockedProfile;
        boolean maskedByQuietProfile;
        boolean maskedBySuspendedPackage;
        int tag = -1;
        ArrayList<Widget> widgets = new ArrayList();
        boolean zombie;

        protected Provider() {
        }

        public int getUserId() {
            return UserHandle.getUserId(this.id.uid);
        }

        public boolean isInPackageForUser(String packageName, int userId) {
            return getUserId() == userId && this.id.componentName.getPackageName().equals(packageName);
        }

        public boolean hostedByPackageForUser(String packageName, int userId) {
            int N = this.widgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = (Widget) this.widgets.get(i);
                if (packageName.equals(widget.host.id.packageName) && widget.host.getUserId() == userId) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Provider{");
            stringBuilder.append(this.id);
            stringBuilder.append(this.zombie ? " Z" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }

        public boolean setMaskedByQuietProfileLocked(boolean masked) {
            boolean oldState = this.maskedByQuietProfile;
            this.maskedByQuietProfile = masked;
            return masked != oldState;
        }

        public boolean setMaskedByLockedProfileLocked(boolean masked) {
            boolean oldState = this.maskedByLockedProfile;
            this.maskedByLockedProfile = masked;
            return masked != oldState;
        }

        public boolean setMaskedBySuspendedPackageLocked(boolean masked) {
            boolean oldState = this.maskedBySuspendedPackage;
            this.maskedBySuspendedPackage = masked;
            return masked != oldState;
        }

        public boolean isMaskedLocked() {
            return this.maskedByQuietProfile || this.maskedByLockedProfile || this.maskedBySuspendedPackage;
        }

        public boolean shouldBePersisted() {
            return (this.widgets.isEmpty() && TextUtils.isEmpty(this.infoTag)) ? false : true;
        }
    }

    private static final class ProviderId {
        final ComponentName componentName;
        final int uid;

        /* synthetic */ ProviderId(int x0, ComponentName x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private ProviderId(int uid, ComponentName componentName) {
            this.uid = uid;
            this.componentName = componentName;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ProviderId other = (ProviderId) obj;
            if (this.uid != other.uid) {
                return false;
            }
            if (this.componentName == null) {
                if (other.componentName != null) {
                    return false;
                }
            } else if (!this.componentName.equals(other.componentName)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            return (31 * this.uid) + (this.componentName != null ? this.componentName.hashCode() : 0);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ProviderId{user:");
            stringBuilder.append(UserHandle.getUserId(this.uid));
            stringBuilder.append(", app:");
            stringBuilder.append(UserHandle.getAppId(this.uid));
            stringBuilder.append(", cmp:");
            stringBuilder.append(this.componentName);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    private final class SaveStateRunnable implements Runnable {
        final int mUserId;

        public SaveStateRunnable(int userId) {
            this.mUserId = userId;
        }

        public void run() {
            synchronized (AppWidgetServiceImpl.this.mLock) {
                AppWidgetServiceImpl.this.ensureGroupStateLoadedLocked(this.mUserId, false);
                AppWidgetServiceImpl.this.saveStateLocked(this.mUserId);
            }
        }
    }

    private final class SecurityPolicy {
        private SecurityPolicy() {
        }

        /* synthetic */ SecurityPolicy(AppWidgetServiceImpl x0, AnonymousClass1 x1) {
            this();
        }

        public boolean isEnabledGroupProfile(int profileId) {
            return isParentOrProfile(UserHandle.getCallingUserId(), profileId) && isProfileEnabled(profileId);
        }

        public int[] getEnabledGroupProfileIds(int userId) {
            int parentId = getGroupParent(userId);
            long identity = Binder.clearCallingIdentity();
            try {
                int[] enabledProfileIds = AppWidgetServiceImpl.this.mUserManager.getEnabledProfileIds(parentId);
                return enabledProfileIds;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void enforceServiceExistsAndRequiresBindRemoteViewsPermission(ComponentName componentName, int userId) {
            long identity = Binder.clearCallingIdentity();
            try {
                ServiceInfo serviceInfo = AppWidgetServiceImpl.this.mPackageManager.getServiceInfo(componentName, 4096, userId);
                StringBuilder stringBuilder;
                if (serviceInfo != null) {
                    if (!"android.permission.BIND_REMOTEVIEWS".equals(serviceInfo.permission)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Service ");
                        stringBuilder.append(componentName);
                        stringBuilder.append(" in user ");
                        stringBuilder.append(userId);
                        stringBuilder.append("does not require ");
                        stringBuilder.append("android.permission.BIND_REMOTEVIEWS");
                        throw new SecurityException(stringBuilder.toString());
                    }
                    Binder.restoreCallingIdentity(identity);
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Service ");
                stringBuilder.append(componentName);
                stringBuilder.append(" not installed for user ");
                stringBuilder.append(userId);
                throw new SecurityException(stringBuilder.toString());
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void enforceModifyAppWidgetBindPermissions(String packageName) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hasBindAppWidgetPermission packageName=");
            stringBuilder.append(packageName);
            AppWidgetServiceImpl.this.mContext.enforceCallingPermission("android.permission.MODIFY_APPWIDGET_BIND_PERMISSIONS", stringBuilder.toString());
        }

        public boolean isCallerInstantAppLocked() {
            int callingUid = Binder.getCallingUid();
            long identity = Binder.clearCallingIdentity();
            try {
                String[] uidPackages = AppWidgetServiceImpl.this.mPackageManager.getPackagesForUid(callingUid);
                if (!ArrayUtils.isEmpty(uidPackages)) {
                    boolean isInstantApp = AppWidgetServiceImpl.this.mPackageManager.isInstantApp(uidPackages[0], UserHandle.getCallingUserId());
                    Binder.restoreCallingIdentity(identity);
                    return isInstantApp;
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
            Binder.restoreCallingIdentity(identity);
            return false;
        }

        public boolean isInstantAppLocked(String packageName, int userId) {
            long identity = Binder.clearCallingIdentity();
            boolean e;
            try {
                e = AppWidgetServiceImpl.this.mPackageManager.isInstantApp(packageName, userId);
                return e;
            } catch (RemoteException e2) {
                e = e2;
                return false;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void enforceCallFromPackage(String packageName) {
            AppWidgetServiceImpl.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), packageName);
        }

        public boolean hasCallerBindPermissionOrBindWhiteListedLocked(String packageName) {
            try {
                AppWidgetServiceImpl.this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_APPWIDGET", null);
            } catch (SecurityException e) {
                if (!isCallerBindAppWidgetWhiteListedLocked(packageName)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isCallerBindAppWidgetWhiteListedLocked(String packageName) {
            int userId = UserHandle.getCallingUserId();
            if (AppWidgetServiceImpl.this.getUidForPackage(packageName, userId) >= 0) {
                synchronized (AppWidgetServiceImpl.this.mLock) {
                    AppWidgetServiceImpl.this.ensureGroupStateLoadedLocked(userId);
                    if (AppWidgetServiceImpl.this.mPackagesWithBindWidgetPermission.contains(Pair.create(Integer.valueOf(userId), packageName))) {
                        return true;
                    }
                    return false;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No package ");
            stringBuilder.append(packageName);
            stringBuilder.append(" for user ");
            stringBuilder.append(userId);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public boolean canAccessAppWidget(Widget widget, int uid, String packageName) {
            if (isHostInPackageForUid(widget.host, uid, packageName) || isProviderInPackageForUid(widget.provider, uid, packageName) || isHostAccessingProvider(widget.host, widget.provider, uid, packageName)) {
                return true;
            }
            int userId = UserHandle.getUserId(uid);
            if ((widget.host.getUserId() == userId || (widget.provider != null && widget.provider.getUserId() == userId)) && AppWidgetServiceImpl.this.mContext.checkCallingPermission("android.permission.BIND_APPWIDGET") == 0) {
                return true;
            }
            return false;
        }

        private boolean isParentOrProfile(int parentId, int profileId) {
            boolean z = true;
            if (parentId == profileId) {
                return true;
            }
            if (getProfileParent(profileId) != parentId) {
                z = false;
            }
            return z;
        }

        public boolean isProviderInCallerOrInProfileAndWhitelListed(String packageName, int profileId) {
            int callerId = UserHandle.getCallingUserId();
            if (profileId == callerId) {
                return true;
            }
            if (getProfileParent(profileId) != callerId) {
                return false;
            }
            return isProviderWhiteListed(packageName, profileId);
        }

        public boolean isProviderWhiteListed(String packageName, int profileId) {
            if (AppWidgetServiceImpl.this.mDevicePolicyManagerInternal == null) {
                return false;
            }
            return AppWidgetServiceImpl.this.mDevicePolicyManagerInternal.getCrossProfileWidgetProviders(profileId).contains(packageName);
        }

        public int getProfileParent(int profileId) {
            long identity = Binder.clearCallingIdentity();
            try {
                UserInfo parent = AppWidgetServiceImpl.this.mUserManager.getProfileParent(profileId);
                if (parent != null) {
                    int identifier = parent.getUserHandle().getIdentifier();
                    return identifier;
                }
                Binder.restoreCallingIdentity(identity);
                return -10;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public int getGroupParent(int profileId) {
            int parentId = AppWidgetServiceImpl.this.mSecurityPolicy.getProfileParent(profileId);
            return parentId != -10 ? parentId : profileId;
        }

        public boolean isHostInPackageForUid(Host host, int uid, String packageName) {
            return host.id.uid == uid && host.id.packageName.equals(packageName);
        }

        public boolean isProviderInPackageForUid(Provider provider, int uid, String packageName) {
            return provider != null && provider.id.uid == uid && provider.id.componentName.getPackageName().equals(packageName);
        }

        public boolean isHostAccessingProvider(Host host, Provider provider, int uid, String packageName) {
            return host.id.uid == uid && provider != null && provider.id.componentName.getPackageName().equals(packageName);
        }

        private boolean isProfileEnabled(int profileId) {
            long identity = Binder.clearCallingIdentity();
            try {
                UserInfo userInfo = AppWidgetServiceImpl.this.mUserManager.getUserInfo(profileId);
                if (userInfo != null && userInfo.isEnabled()) {
                    return true;
                }
                Binder.restoreCallingIdentity(identity);
                return false;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    protected static final class Widget {
        int appWidgetId;
        Host host;
        RemoteViews maskedViews;
        Bundle options;
        Provider provider;
        int restoredId;
        SparseLongArray updateSequenceNos = new SparseLongArray(2);
        RemoteViews views;

        protected Widget() {
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AppWidgetId{");
            stringBuilder.append(this.appWidgetId);
            stringBuilder.append(':');
            stringBuilder.append(this.host);
            stringBuilder.append(':');
            stringBuilder.append(this.provider);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }

        private boolean replaceWithMaskedViewsLocked(RemoteViews views) {
            this.maskedViews = views;
            return true;
        }

        private boolean clearMaskedViewsLocked() {
            if (this.maskedViews == null) {
                return false;
            }
            this.maskedViews = null;
            return true;
        }

        public RemoteViews getEffectiveViewsLocked() {
            return this.maskedViews != null ? this.maskedViews : this.views;
        }
    }

    static {
        HIDDEN_WEATHER_WIDGETS.put("com.huawei.android.totemweather.widget.mulan.MulanWidgetWeatherProvider", "1");
        HIDDEN_WEATHER_WIDGETS.put("com.huawei.android.totemweather.widget.doublecity.DualWidgetWeatherProvider", "1");
        HIDDEN_WEATHER_WIDGETS.put("com.huawei.android.totemweather.widget.WeatherSmallWidgetProvider", "1");
        HIDDEN_WEATHER_WIDGETS.put("com.huawei.android.totemweather.widget.WeatherSimpleWidgetProvider", "1");
        HIDDEN_WEATHER_WIDGETS.put("com.huawei.android.totemweather.widget.WeatherLimitWidgetProvider", "1");
    }

    AppWidgetServiceImpl(Context context) {
        this.mContext = context;
    }

    public void onStart() {
        this.mPackageManager = AppGlobals.getPackageManager();
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mDevicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mSaveStateHandler = BackgroundThread.getHandler();
        this.mCallbackHandler = new CallbackHandler(this.mContext.getMainLooper());
        this.mBackupRestoreController = new BackupRestoreController(this, null);
        this.mSecurityPolicy = new SecurityPolicy(this, null);
        this.mIconUtilities = new IconUtilities(this.mContext);
        computeMaximumWidgetBitmapMemory();
        this.mLocale = Locale.getDefault();
        registerBroadcastReceiver();
        registerOnCrossProfileProvidersChangedListener();
        LocalServices.addService(AppWidgetManagerInternal.class, new AppWidgetManagerLocal(this, null));
    }

    private void computeMaximumWidgetBitmapMemory() {
        Display display = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        this.mMaxWidgetBitmapMemory = (6 * size.x) * size.y;
    }

    private void registerBroadcastReceiver() {
        IntentFilter configFilter = new IntentFilter();
        configFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, configFilter, null, null);
        IntentFilter cotaAppFilter = new IntentFilter();
        cotaAppFilter.addAction(COTA_APP_UPDATE_APPWIDGET);
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, cotaAppFilter, null, null);
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, packageFilter, null, null);
        IntentFilter sdFilter = new IntentFilter();
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, sdFilter, null, null);
        IntentFilter offModeFilter = new IntentFilter();
        offModeFilter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        offModeFilter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, offModeFilter, null, null);
        IntentFilter suspendPackageFilter = new IntentFilter();
        suspendPackageFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        suspendPackageFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, suspendPackageFilter, null, null);
    }

    private void registerOnCrossProfileProvidersChangedListener() {
        if (this.mDevicePolicyManagerInternal != null) {
            this.mDevicePolicyManagerInternal.addOnCrossProfileWidgetProvidersChangeListener(this);
        }
    }

    public void setSafeMode(boolean safeMode) {
        this.mSafeMode = safeMode;
    }

    private void onConfigurationChanged() {
        if (DEBUG) {
            Slog.i(TAG, "onConfigurationChanged()");
        }
        Locale revised = Locale.getDefault();
        if (revised == null || this.mLocale == null || !revised.equals(this.mLocale)) {
            this.mLocale = revised;
            synchronized (this.mLock) {
                int i;
                SparseIntArray changedGroups = null;
                ArrayList<Provider> installedProviders = new ArrayList(this.mProviders);
                HashSet<ProviderId> removedProviders = new HashSet();
                for (i = installedProviders.size() - 1; i >= 0; i--) {
                    Provider provider = (Provider) installedProviders.get(i);
                    int userId = provider.getUserId();
                    if (this.mUserManager.isUserUnlockingOrUnlocked(userId) && !isProfileWithLockedParent(userId)) {
                        ensureGroupStateLoadedLocked(userId);
                        if (!removedProviders.contains(provider.id) && updateProvidersForPackageLocked(provider.id.componentName.getPackageName(), provider.getUserId(), removedProviders)) {
                            if (changedGroups == null) {
                                changedGroups = new SparseIntArray();
                            }
                            int groupId = this.mSecurityPolicy.getGroupParent(provider.getUserId());
                            changedGroups.put(groupId, groupId);
                        }
                    }
                }
                if (changedGroups != null) {
                    i = changedGroups.size();
                    for (int i2 = 0; i2 < i; i2++) {
                        saveGroupStateAsync(changedGroups.get(i2));
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:73:0x00fb, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onPackageBroadcastReceived(Intent intent, int userId) {
        boolean z;
        String[] pkgList;
        String action = intent.getAction();
        boolean added = false;
        boolean changed = false;
        boolean componentsModified = false;
        boolean cotaFlag = false;
        boolean packageRemovedPermanently = true;
        int i = 0;
        switch (action.hashCode()) {
            case -1403934493:
                if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                    z = true;
                    break;
                }
            case -1338021860:
                if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE")) {
                    z = true;
                    break;
                }
            case -1001645458:
                if (action.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                    z = false;
                    break;
                }
            case -626041473:
                if (action.equals(COTA_APP_UPDATE_APPWIDGET)) {
                    z = true;
                    break;
                }
            case 1290767157:
                if (action.equals("android.intent.action.PACKAGES_UNSUSPENDED")) {
                    z = true;
                    break;
                }
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
            case true:
                pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                changed = true;
                break;
            case true:
                added = true;
                break;
            case true:
                break;
            case true:
                pkgList = intent.getStringArrayExtra(COTA_APP_UPDATE_APPWIDGET_EXTRA);
                added = true;
                cotaFlag = true;
                break;
            default:
                Uri uri = intent.getData();
                if (uri != null && uri.getSchemeSpecificPart() != null) {
                    String[] pkgList2 = new String[]{uri.getSchemeSpecificPart()};
                    added = "android.intent.action.PACKAGE_ADDED".equals(action);
                    changed = "android.intent.action.PACKAGE_CHANGED".equals(action);
                    pkgList = pkgList2;
                    break;
                }
                return;
                break;
        }
        pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
        if (pkgList != null && pkgList.length != 0) {
            synchronized (this.mLock) {
                if ((!this.mUserManager.isUserUnlockingOrUnlocked(userId) || isProfileWithLockedParent(userId)) && !cotaFlag) {
                    return;
                }
                ensureGroupStateLoadedLocked(userId, false);
                Bundle extras = intent.getExtras();
                if (added || changed) {
                    if ((!added || (extras != null && extras.getBoolean("android.intent.extra.REPLACING", false))) && !cotaFlag) {
                        packageRemovedPermanently = false;
                    }
                    int length = pkgList.length;
                    while (i < length) {
                        String pkgName = pkgList[i];
                        componentsModified |= updateProvidersForPackageLocked(pkgName, userId, null);
                        if (packageRemovedPermanently && userId == 0) {
                            int uid = getUidForPackage(pkgName, userId);
                            if (uid >= 0) {
                                resolveHostUidLocked(pkgName, uid);
                            }
                        }
                        i++;
                    }
                } else {
                    if (extras != null && extras.getBoolean("android.intent.extra.REPLACING", false)) {
                        packageRemovedPermanently = false;
                    }
                    if (packageRemovedPermanently) {
                        while (i < pkgList.length) {
                            componentsModified |= removeHostsAndProvidersForPackageLocked(pkgList[i], userId);
                            i++;
                        }
                    }
                }
                if (componentsModified || cotaFlag) {
                    saveGroupStateAsync(userId);
                    scheduleNotifyGroupHostsForProvidersChangedLocked(userId);
                }
            }
        }
    }

    void reloadWidgetsMaskedStateForGroup(int userId) {
        if (this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
            synchronized (this.mLock) {
                reloadWidgetsMaskedState(userId);
                for (int profileId : this.mUserManager.getEnabledProfileIds(userId)) {
                    reloadWidgetsMaskedState(profileId);
                }
            }
        }
    }

    private void reloadWidgetsMaskedState(int userId) {
        long identity = Binder.clearCallingIdentity();
        RemoteException e;
        try {
            boolean lockedProfile = this.mUserManager.isUserUnlockingOrUnlocked(userId) ^ 1;
            boolean quietProfile = this.mUserManager.getUserInfo(userId).isQuietModeEnabled();
            int N = this.mProviders.size();
            for (int i = 0; i < N; i++) {
                Provider provider = (Provider) this.mProviders.get(i);
                if (provider.getUserId() == userId) {
                    boolean changed = provider.setMaskedByLockedProfileLocked(lockedProfile) | provider.setMaskedByQuietProfileLocked(quietProfile);
                    try {
                        e = this.mPackageManager.isPackageSuspendedForUser(provider.info.provider.getPackageName(), provider.getUserId());
                    } catch (IllegalArgumentException e2) {
                        e = null;
                    }
                    changed |= provider.setMaskedBySuspendedPackageLocked(e);
                    if (changed) {
                        if (provider.isMaskedLocked()) {
                            maskWidgetsViewsLocked(provider, null);
                        } else {
                            unmaskWidgetsViewsLocked(provider);
                        }
                    }
                }
            }
            Binder.restoreCallingIdentity(identity);
        } catch (RemoteException e3) {
            Slog.e(TAG, "Failed to query application info", e3);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void updateWidgetPackageSuspensionMaskedState(Intent intent, boolean suspended, int profileId) {
        String[] packagesArray = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
        if (packagesArray != null) {
            Set<String> packages = new ArraySet(Arrays.asList(packagesArray));
            synchronized (this.mLock) {
                int N = this.mProviders.size();
                for (int i = 0; i < N; i++) {
                    Provider provider = (Provider) this.mProviders.get(i);
                    if (provider.getUserId() == profileId && packages.contains(provider.info.provider.getPackageName()) && provider.setMaskedBySuspendedPackageLocked(suspended)) {
                        if (provider.isMaskedLocked()) {
                            maskWidgetsViewsLocked(provider, null);
                        } else {
                            unmaskWidgetsViewsLocked(provider);
                        }
                    }
                }
            }
        }
    }

    private Bitmap createMaskedWidgetBitmap(String providerPackage, int providerUserId) {
        long identity = Binder.clearCallingIdentity();
        try {
            PackageManager pm = this.mContext.createPackageContextAsUser(providerPackage, 0, UserHandle.of(providerUserId)).getPackageManager();
            Drawable icon = pm.getApplicationInfo(providerPackage, 0).loadUnbadgedIcon(pm).mutate();
            icon.setColorFilter(this.mIconUtilities.getDisabledColorFilter());
            Bitmap createIconBitmap = this.mIconUtilities.createIconBitmap(icon);
            Binder.restoreCallingIdentity(identity);
            return createIconBitmap;
        } catch (NameNotFoundException e) {
            Slog.e(TAG, "Fail to get application icon", e);
            Binder.restoreCallingIdentity(identity);
            return null;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    private RemoteViews createMaskedWidgetRemoteViews(Bitmap icon, boolean showBadge, PendingIntent onClickIntent) {
        RemoteViews views = new RemoteViews(this.mContext.getPackageName(), 17367338);
        if (icon != null) {
            views.setImageViewBitmap(16909532, icon);
        }
        if (!showBadge) {
            views.setViewVisibility(16909533, 4);
        }
        if (onClickIntent != null) {
            views.setOnClickPendingIntent(16909534, onClickIntent);
        }
        return views;
    }

    private void maskWidgetsViewsLocked(Provider provider, Widget targetWidget) {
        Provider provider2 = provider;
        Widget widget = targetWidget;
        int widgetCount = provider2.widgets.size();
        if (widgetCount != 0) {
            String providerPackage = provider2.info.provider.getPackageName();
            int providerUserId = provider.getUserId();
            Bitmap iconBitmap = createMaskedWidgetBitmap(providerPackage, providerUserId);
            if (iconBitmap != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    boolean showBadge;
                    Intent onClickIntent;
                    if (provider2.maskedBySuspendedPackage) {
                        Intent onClickIntent2;
                        showBadge = this.mUserManager.getUserInfo(providerUserId).isManagedProfile();
                        String suspendingPackage = this.mPackageManagerInternal.getSuspendingPackage(providerPackage, providerUserId);
                        if ("android".equals(suspendingPackage)) {
                            onClickIntent2 = this.mDevicePolicyManagerInternal.createShowAdminSupportIntent(providerUserId, true);
                        } else {
                            onClickIntent2 = SuspendedAppActivity.createSuspendedAppInterceptIntent(providerPackage, suspendingPackage, this.mPackageManagerInternal.getSuspendedDialogMessage(providerPackage, providerUserId), providerUserId);
                        }
                        onClickIntent = onClickIntent2;
                    } else if (provider2.maskedByQuietProfile) {
                        showBadge = true;
                        onClickIntent = UnlaunchableAppActivity.createInQuietModeDialogIntent(providerUserId);
                    } else {
                        showBadge = true;
                        onClickIntent = this.mKeyguardManager.createConfirmDeviceCredentialIntent(null, null, providerUserId);
                        if (onClickIntent != null) {
                            onClickIntent.setFlags(276824064);
                        }
                    }
                    int j = 0;
                    while (j < widgetCount) {
                        Widget widget2 = (Widget) provider2.widgets.get(j);
                        if (widget == null || widget == widget2) {
                            PendingIntent intent = null;
                            if (onClickIntent != null) {
                                intent = PendingIntent.getActivity(this.mContext, widget2.appWidgetId, onClickIntent, 134217728);
                            }
                            if (widget2.replaceWithMaskedViewsLocked(createMaskedWidgetRemoteViews(iconBitmap, showBadge, intent))) {
                                scheduleNotifyUpdateAppWidgetLocked(widget2, widget2.getEffectiveViewsLocked());
                            }
                        }
                        j++;
                        provider2 = provider;
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    private void unmaskWidgetsViewsLocked(Provider provider) {
        int widgetCount = provider.widgets.size();
        for (int j = 0; j < widgetCount; j++) {
            Widget widget = (Widget) provider.widgets.get(j);
            if (widget.clearMaskedViewsLocked()) {
                scheduleNotifyUpdateAppWidgetLocked(widget, widget.getEffectiveViewsLocked());
            }
        }
    }

    private void resolveHostUidLocked(String pkg, int uid) {
        int N = this.mHosts.size();
        for (int i = 0; i < N; i++) {
            Host host = (Host) this.mHosts.get(i);
            if (host.id.uid == -1 && pkg.equals(host.id.packageName)) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("host ");
                    stringBuilder.append(host.id);
                    stringBuilder.append(" resolved to uid ");
                    stringBuilder.append(uid);
                    Slog.i(str, stringBuilder.toString());
                }
                host.id = new HostId(uid, host.id.hostId, host.id.packageName);
                return;
            }
        }
    }

    private void ensureGroupStateLoadedLocked(int userId) {
        ensureGroupStateLoadedLocked(userId, true);
    }

    private void ensureGroupStateLoadedLocked(int userId, boolean enforceUserUnlockingOrUnlocked) {
        StringBuilder stringBuilder;
        if (enforceUserUnlockingOrUnlocked && !isUserRunningAndUnlocked(userId)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("User ");
            stringBuilder.append(userId);
            stringBuilder.append(" must be unlocked for widgets to be available");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (enforceUserUnlockingOrUnlocked && isProfileWithLockedParent(userId)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Profile ");
            stringBuilder.append(userId);
            stringBuilder.append(" must have unlocked parent");
            throw new IllegalStateException(stringBuilder.toString());
        } else {
            int i;
            int[] profileIds = this.mSecurityPolicy.getEnabledGroupProfileIds(userId);
            int profileIdCount = profileIds.length;
            int i2 = 0;
            int newMemberCount = 0;
            for (i = 0; i < profileIdCount; i++) {
                if (this.mLoadedUserIds.indexOfKey(profileIds[i]) >= 0) {
                    profileIds[i] = -1;
                } else {
                    newMemberCount++;
                }
            }
            if (newMemberCount > 0) {
                i = 0;
                int[] newProfileIds = new int[newMemberCount];
                while (i2 < profileIdCount) {
                    int profileId = profileIds[i2];
                    if (profileId != -1) {
                        this.mLoadedUserIds.put(profileId, profileId);
                        newProfileIds[i] = profileId;
                        i++;
                    }
                    i2++;
                }
                clearProvidersAndHostsTagsLocked();
                loadGroupWidgetProvidersLocked(newProfileIds);
                loadGroupStateLocked(newProfileIds);
            }
        }
    }

    private boolean isUserRunningAndUnlocked(int userId) {
        return this.mUserManager.isUserUnlockingOrUnlocked(userId);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            synchronized (this.mLock) {
                if (args.length <= 0 || !PriorityDump.PROTO_ARG.equals(args[0])) {
                    dumpInternal(pw);
                } else {
                    dumpProto(fd);
                }
            }
        }
    }

    private void dumpProto(FileDescriptor fd) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dump proto for ");
        stringBuilder.append(this.mWidgets.size());
        stringBuilder.append(" widgets");
        Slog.i(str, stringBuilder.toString());
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        int N = this.mWidgets.size();
        for (int i = 0; i < N; i++) {
            dumpProtoWidget(proto, (Widget) this.mWidgets.get(i));
        }
        proto.flush();
    }

    private void dumpProtoWidget(ProtoOutputStream proto, Widget widget) {
        if (widget.host == null || widget.provider == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("skip dumping widget because host or provider is null: widget.host=");
            stringBuilder.append(widget.host);
            stringBuilder.append(" widget.provider=");
            stringBuilder.append(widget.provider);
            Slog.d(str, stringBuilder.toString());
            return;
        }
        long token = proto.start(2246267895809L);
        boolean z = true;
        proto.write(1133871366145L, widget.host.getUserId() != widget.provider.getUserId());
        if (widget.host.callbacks != null) {
            z = false;
        }
        proto.write(1133871366146L, z);
        proto.write(1138166333443L, widget.host.id.packageName);
        proto.write(1138166333444L, widget.provider.id.componentName.getPackageName());
        proto.write(1138166333445L, widget.provider.id.componentName.getClassName());
        if (widget.options != null) {
            proto.write(1120986464262L, widget.options.getInt("appWidgetMinWidth", 0));
            proto.write(1120986464263L, widget.options.getInt("appWidgetMinHeight", 0));
            proto.write(1120986464264L, widget.options.getInt("appWidgetMaxWidth", 0));
            proto.write(1120986464265L, widget.options.getInt("appWidgetMaxHeight", 0));
        }
        proto.end(token);
    }

    private void dumpInternal(PrintWriter pw) {
        int i;
        int N = this.mProviders.size();
        pw.println("Providers:");
        int i2 = 0;
        for (i = 0; i < N; i++) {
            dumpProvider((Provider) this.mProviders.get(i), i, pw);
        }
        N = this.mWidgets.size();
        pw.println(" ");
        pw.println("Widgets:");
        for (i = 0; i < N; i++) {
            dumpWidget((Widget) this.mWidgets.get(i), i, pw);
        }
        N = this.mHosts.size();
        pw.println(" ");
        pw.println("Hosts:");
        for (i = 0; i < N; i++) {
            dumpHost((Host) this.mHosts.get(i), i, pw);
        }
        N = this.mPackagesWithBindWidgetPermission.size();
        pw.println(" ");
        pw.println("Grants:");
        while (i2 < N) {
            dumpGrant((Pair) this.mPackagesWithBindWidgetPermission.valueAt(i2), i2, pw);
            i2++;
        }
    }

    public ParceledListSlice<PendingHostUpdate> startListening(IAppWidgetHost callbacks, String callingPackage, int hostId, int[] appWidgetIds) {
        String str;
        StringBuilder stringBuilder;
        Throwable th;
        IAppWidgetHost iAppWidgetHost;
        String str2 = callingPackage;
        int[] iArr = appWidgetIds;
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startListening() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("startListening:callingpackage");
        stringBuilder.append(str2);
        Log.d(str, stringBuilder.toString());
        this.mSecurityPolicy.enforceCallFromPackage(str2);
        synchronized (this.mLock) {
            try {
                ParceledListSlice<PendingHostUpdate> emptyList;
                if (this.mSecurityPolicy.isInstantAppLocked(str2, userId)) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Instant package ");
                    stringBuilder2.append(str2);
                    stringBuilder2.append(" cannot host app widgets");
                    Slog.w(str, stringBuilder2.toString());
                    emptyList = ParceledListSlice.emptyList();
                    return emptyList;
                }
                ensureGroupStateLoadedLocked(userId);
                try {
                    HostId id = new HostId(Binder.getCallingUid(), hostId, str2);
                    Host host = lookupOrAddHostLocked(id);
                    host.callbacks = callbacks;
                    long updateSequenceNo = UPDATE_COUNTER.incrementAndGet();
                    int N = iArr.length;
                    ArrayList<PendingHostUpdate> outUpdates = new ArrayList(N);
                    LongSparseArray<PendingHostUpdate> updatesMap = new LongSparseArray();
                    int i = 0;
                    while (i < N) {
                        HostId id2;
                        if (host.getPendingUpdatesForId(iArr[i], updatesMap)) {
                            int M = updatesMap.size();
                            int j = 0;
                            while (true) {
                                id2 = id;
                                id = j;
                                if (id >= M) {
                                    break;
                                }
                                outUpdates.add((PendingHostUpdate) updatesMap.valueAt(id));
                                j = id + 1;
                                id = id2;
                            }
                        } else {
                            id2 = id;
                        }
                        i++;
                        id = id2;
                    }
                    host.lastWidgetUpdateSequenceNo = updateSequenceNo;
                    emptyList = new ParceledListSlice(outUpdates);
                    return emptyList;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                iAppWidgetHost = callbacks;
                int i2 = hostId;
                throw th;
            }
        }
    }

    public void stopListening(String callingPackage, int hostId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopListening() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId, false);
            Host host = lookupHostLocked(new HostId(Binder.getCallingUid(), hostId, callingPackage));
            if (host != null) {
                host.callbacks = null;
                pruneHostLocked(host);
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x00a7, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int allocateAppWidgetId(String callingPackage, int hostId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("allocateAppWidgetId() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.isInstantAppLocked(callingPackage, userId)) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Instant package ");
                stringBuilder2.append(callingPackage);
                stringBuilder2.append(" cannot host app widgets");
                Slog.w(str2, stringBuilder2.toString());
                return 0;
            }
            ensureGroupStateLoadedLocked(userId);
            if (this.mNextAppWidgetIds.indexOfKey(userId) < 0) {
                this.mNextAppWidgetIds.put(userId, 1);
            }
            int appWidgetId = incrementAndGetAppWidgetIdLocked(userId);
            Host host = lookupOrAddHostLocked(new HostId(Binder.getCallingUid(), hostId, callingPackage));
            Widget widget = new Widget();
            widget.appWidgetId = appWidgetId;
            widget.host = host;
            host.widgets.add(widget);
            addWidgetLocked(widget);
            saveGroupStateAsync(userId);
            if (DEBUG) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Allocated widget id ");
                stringBuilder3.append(appWidgetId);
                stringBuilder3.append(" for host ");
                stringBuilder3.append(host.id);
                Slog.i(str3, stringBuilder3.toString());
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x0063, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void deleteAppWidgetId(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deleteAppWidgetId() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null) {
                return;
            }
            deleteAppWidgetLocked(widget);
            saveGroupStateAsync(userId);
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Deleted widget id ");
                stringBuilder2.append(appWidgetId);
                stringBuilder2.append(" for host ");
                stringBuilder2.append(widget.host.id);
                Slog.i(str2, stringBuilder2.toString());
            }
        }
    }

    public boolean hasBindAppWidgetPermission(String packageName, int grantId) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hasBindAppWidgetPermission() ");
            stringBuilder.append(UserHandle.getCallingUserId());
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceModifyAppWidgetBindPermissions(packageName);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(grantId);
            if (getUidForPackage(packageName, grantId) < 0) {
                return false;
            }
            boolean contains = this.mPackagesWithBindWidgetPermission.contains(Pair.create(Integer.valueOf(grantId), packageName));
            return contains;
        }
    }

    public void setBindAppWidgetPermission(String packageName, int grantId, boolean grantPermission) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setBindAppWidgetPermission() ");
            stringBuilder.append(UserHandle.getCallingUserId());
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceModifyAppWidgetBindPermissions(packageName);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(grantId);
            if (getUidForPackage(packageName, grantId) < 0) {
                return;
            }
            Pair<Integer, String> packageId = Pair.create(Integer.valueOf(grantId), packageName);
            if (grantPermission) {
                this.mPackagesWithBindWidgetPermission.add(packageId);
            } else {
                this.mPackagesWithBindWidgetPermission.remove(packageId);
            }
            saveGroupStateAsync(grantId);
        }
    }

    public IntentSender createAppWidgetConfigIntentSender(String callingPackage, int appWidgetId) {
        IntentSender intentSender;
        Throwable th;
        String str = callingPackage;
        int i = appWidgetId;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.APPWIGDET_CREATEAPPWIDGETCONFIGINTENTSENDER);
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createAppWidgetConfigIntentSender() ");
            stringBuilder.append(userId);
            Slog.i(str2, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(i, Binder.getCallingUid(), str);
            StringBuilder stringBuilder2;
            if (widget != null) {
                Provider provider = widget.provider;
                if (provider != null) {
                    Intent intent = new Intent("android.appwidget.action.APPWIDGET_CONFIGURE");
                    intent.putExtra("appWidgetId", i);
                    intent.setComponent(provider.info.configure);
                    long identity = Binder.clearCallingIdentity();
                    long identity2;
                    try {
                        Context context = this.mContext;
                        identity2 = identity;
                        try {
                            intentSender = PendingIntent.getActivityAsUser(context, 0, intent, 1409286144, null, new UserHandle(provider.getUserId())).getIntentSender();
                            Binder.restoreCallingIdentity(identity2);
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(identity2);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        identity2 = identity;
                        Binder.restoreCallingIdentity(identity2);
                        throw th;
                    }
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Widget not bound ");
                stringBuilder2.append(i);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Bad widget id ");
            stringBuilder2.append(i);
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
        return intentSender;
    }

    /* JADX WARNING: Missing block: B:56:0x019b, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean bindAppWidgetId(String callingPackage, int appWidgetId, int providerProfileId, ComponentName providerComponent, Bundle options) {
        String str = callingPackage;
        int i = appWidgetId;
        int i2 = providerProfileId;
        ComponentName componentName = providerComponent;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.APPWIGDET_BINDAPPWIDGETID);
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bindAppWidgetId() ");
            stringBuilder.append(userId);
            Slog.i(str2, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        if (!this.mSecurityPolicy.isEnabledGroupProfile(i2) || !this.mSecurityPolicy.isProviderInCallerOrInProfileAndWhitelListed(providerComponent.getPackageName(), i2)) {
            return false;
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            if (this.mSecurityPolicy.hasCallerBindPermissionOrBindWhiteListedLocked(str)) {
                Widget widget = lookupWidgetLocked(i, Binder.getCallingUid(), str);
                String str3;
                StringBuilder stringBuilder2;
                if (widget == null) {
                    str3 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Bad widget id ");
                    stringBuilder2.append(i);
                    Slog.e(str3, stringBuilder2.toString());
                    return false;
                } else if (widget.provider != null) {
                    str3 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Widget id ");
                    stringBuilder2.append(i);
                    stringBuilder2.append(" already bound to: ");
                    stringBuilder2.append(widget.provider.id);
                    Slog.e(str3, stringBuilder2.toString());
                    return false;
                } else {
                    int providerUid = getUidForPackage(providerComponent.getPackageName(), i2);
                    if (providerUid < 0) {
                        String str4 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Package ");
                        stringBuilder3.append(providerComponent.getPackageName());
                        stringBuilder3.append(" not installed  for profile ");
                        stringBuilder3.append(i2);
                        Slog.e(str4, stringBuilder3.toString());
                        return false;
                    }
                    Provider provider = lookupProviderLocked(new ProviderId(providerUid, componentName, null));
                    String str5;
                    StringBuilder stringBuilder4;
                    if (provider == null) {
                        str5 = TAG;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("No widget provider ");
                        stringBuilder4.append(componentName);
                        stringBuilder4.append(" for profile ");
                        stringBuilder4.append(i2);
                        Slog.e(str5, stringBuilder4.toString());
                        return false;
                    } else if (provider.zombie) {
                        str5 = TAG;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Can't bind to a 3rd party provider in safe mode ");
                        stringBuilder4.append(provider);
                        Slog.e(str5, stringBuilder4.toString());
                        return false;
                    } else {
                        widget.provider = provider;
                        widget.options = options != null ? cloneIfLocalBinder(options) : new Bundle();
                        if (!widget.options.containsKey("appWidgetCategory")) {
                            widget.options.putInt("appWidgetCategory", 1);
                        }
                        provider.widgets.add(widget);
                        onWidgetProviderAddedOrChangedLocked(widget);
                        if (provider.widgets.size() == 1) {
                            LogPower.push(168, providerComponent.getPackageName(), String.valueOf(providerUid));
                            sendEnableIntentLocked(provider);
                        }
                        sendUpdateIntentLocked(provider, new int[]{i});
                        registerForBroadcastsLocked(provider, getWidgetIds(provider.widgets));
                        saveGroupStateAsync(userId);
                        if (DEBUG) {
                            String str6 = TAG;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("Bound widget ");
                            stringBuilder5.append(i);
                            stringBuilder5.append(" to provider ");
                            stringBuilder5.append(provider.id);
                            Slog.i(str6, stringBuilder5.toString());
                        }
                    }
                }
            } else {
                return false;
            }
        }
    }

    public int[] getAppWidgetIds(ComponentName componentName) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAppWidgetIds() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(componentName.getPackageName());
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Provider provider = lookupProviderLocked(new ProviderId(Binder.getCallingUid(), componentName, null));
            int[] widgetIds;
            if (provider != null) {
                widgetIds = getWidgetIds(provider.widgets);
                return widgetIds;
            }
            widgetIds = new int[0];
            return widgetIds;
        }
    }

    public int[] getAppWidgetIdsForHost(String callingPackage, int hostId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAppWidgetIdsForHost() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Host host = lookupHostLocked(new HostId(Binder.getCallingUid(), hostId, callingPackage));
            int[] widgetIds;
            if (host != null) {
                widgetIds = getWidgetIds(host.widgets);
                return widgetIds;
            }
            widgetIds = new int[0];
            return widgetIds;
        }
    }

    public boolean bindRemoteViewsService(String callingPackage, int appWidgetId, Intent intent, IApplicationThread caller, IBinder activtiyToken, IServiceConnection connection, int flags) {
        StringBuilder stringBuilder;
        String str;
        String str2;
        ComponentName componentName;
        Throwable th;
        int i = appWidgetId;
        Intent intent2 = intent;
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str3 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("bindRemoteViewsService() ");
            stringBuilder.append(userId);
            Slog.i(str3, stringBuilder.toString());
        }
        Object obj = this.mLock;
        synchronized (obj) {
            int i2;
            Object obj2;
            try {
                ensureGroupStateLoadedLocked(userId);
                Widget widget = lookupWidgetLocked(i, Binder.getCallingUid(), callingPackage);
                if (widget == null) {
                    i2 = userId;
                    obj2 = obj;
                    throw new IllegalArgumentException("Bad widget id");
                } else if (widget.provider != null) {
                    ComponentName componentName2 = intent.getComponent();
                    String providerPackage = widget.provider.id.componentName.getPackageName();
                    String servicePackage = componentName2.getPackageName();
                    Widget widget2;
                    if (servicePackage.equals(providerPackage)) {
                        this.mSecurityPolicy.enforceServiceExistsAndRequiresBindRemoteViewsPermission(componentName2, widget.provider.getUserId());
                        long callingIdentity = Binder.clearCallingIdentity();
                        try {
                            obj2 = obj;
                            userId = callingIdentity;
                            widget2 = widget;
                        } catch (RemoteException e) {
                            str = servicePackage;
                            str2 = providerPackage;
                            componentName = componentName2;
                            widget2 = widget;
                            i2 = userId;
                            obj2 = obj;
                            userId = callingIdentity;
                            Binder.restoreCallingIdentity(userId);
                            return false;
                        } catch (Throwable th2) {
                            th = th2;
                            str = servicePackage;
                            str2 = providerPackage;
                            componentName = componentName2;
                            widget2 = widget;
                            i2 = userId;
                            obj2 = obj;
                            userId = callingIdentity;
                            Binder.restoreCallingIdentity(userId);
                            throw th;
                        }
                        try {
                            if (ActivityManager.getService().bindService(caller, activtiyToken, intent2, intent2.resolveTypeIfNeeded(this.mContext.getContentResolver()), connection, flags, this.mContext.getOpPackageName(), widget.provider.getUserId()) != 0) {
                                incrementAppWidgetServiceRefCount(i, Pair.create(Integer.valueOf(widget2.provider.id.uid), new FilterComparison(intent2)));
                                Binder.restoreCallingIdentity(userId);
                                return true;
                            }
                            Binder.restoreCallingIdentity(userId);
                            return false;
                        } catch (RemoteException e2) {
                            Binder.restoreCallingIdentity(userId);
                            return false;
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                    str = servicePackage;
                    str2 = providerPackage;
                    componentName = componentName2;
                    widget2 = widget;
                    i2 = userId;
                    obj2 = obj;
                    throw new SecurityException("The taget service not in the same package as the widget provider");
                } else {
                    i2 = userId;
                    obj2 = obj;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("No provider for widget ");
                    stringBuilder.append(i);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } catch (Throwable th4) {
                th = th4;
                i2 = userId;
                obj2 = obj;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x005d, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void deleteHost(String callingPackage, int hostId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deleteHost() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Host host = lookupHostLocked(new HostId(Binder.getCallingUid(), hostId, callingPackage));
            if (host == null) {
                return;
            }
            deleteHostLocked(host);
            saveGroupStateAsync(userId);
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Deleted host ");
                stringBuilder2.append(host.id);
                Slog.i(str2, stringBuilder2.toString());
            }
        }
    }

    public void deleteAllHosts() {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deleteAllHosts() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            boolean changed = false;
            for (int i = this.mHosts.size() - 1; i >= 0; i--) {
                Host host = (Host) this.mHosts.get(i);
                if (host.id.uid == Binder.getCallingUid()) {
                    deleteHostLocked(host);
                    changed = true;
                    if (DEBUG) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Deleted host ");
                        stringBuilder2.append(host.id);
                        Slog.i(str2, stringBuilder2.toString());
                    }
                }
            }
            if (changed) {
                saveGroupStateAsync(userId);
            }
        }
    }

    public AppWidgetProviderInfo getAppWidgetInfo(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAppWidgetInfo() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null || widget.provider == null || widget.provider.zombie) {
                return null;
            }
            AppWidgetProviderInfo cloneIfLocalBinder = cloneIfLocalBinder(widget.provider.info);
            return cloneIfLocalBinder;
        }
    }

    public RemoteViews getAppWidgetViews(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAppWidgetViews() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget != null) {
                RemoteViews cloneIfLocalBinder = cloneIfLocalBinder(widget.getEffectiveViewsLocked());
                return cloneIfLocalBinder;
            }
            return null;
        }
    }

    public void updateAppWidgetOptions(String callingPackage, int appWidgetId, Bundle options) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateAppWidgetOptions() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null) {
                return;
            }
            widget.options.putAll(options);
            sendOptionsChangedIntentLocked(widget);
            saveGroupStateAsync(userId);
            updateWidgetOptionsReport(userId, widget);
            LogPower.push(DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE, callingPackage);
        }
    }

    public Bundle getAppWidgetOptions(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAppWidgetOptions() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            Bundle bundle;
            if (widget == null || widget.options == null) {
                bundle = Bundle.EMPTY;
                return bundle;
            }
            bundle = cloneIfLocalBinder(widget.options);
            return bundle;
        }
    }

    public void updateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateAppWidgetIds() ");
            stringBuilder.append(UserHandle.getCallingUserId());
            Slog.i(str, stringBuilder.toString());
        }
        updateAppWidgetIds(callingPackage, appWidgetIds, views, false);
    }

    public void partiallyUpdateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("partiallyUpdateAppWidgetIds() ");
            stringBuilder.append(UserHandle.getCallingUserId());
            Slog.i(str, stringBuilder.toString());
        }
        updateAppWidgetIds(callingPackage, appWidgetIds, views, true);
    }

    public void notifyAppWidgetViewDataChanged(String callingPackage, int[] appWidgetIds, int viewId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyAppWidgetViewDataChanged() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        if (appWidgetIds != null && appWidgetIds.length != 0) {
            synchronized (this.mLock) {
                ensureGroupStateLoadedLocked(userId);
                for (int appWidgetId : appWidgetIds) {
                    Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
                    if (widget != null) {
                        scheduleNotifyAppWidgetViewDataChanged(widget, viewId);
                        LogPower.push(DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE, callingPackage);
                        this.mAWSIProxy.updateWidgetFlushReport(userId, callingPackage);
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0081, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateAppWidgetProvider(ComponentName componentName, RemoteViews views) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateAppWidgetProvider() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(componentName.getPackageName());
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            ProviderId providerId = new ProviderId(Binder.getCallingUid(), componentName, null);
            Provider provider = lookupProviderLocked(providerId);
            if (provider == null) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Provider doesn't exist ");
                stringBuilder2.append(providerId);
                Slog.w(str2, stringBuilder2.toString());
                return;
            }
            ArrayList<Widget> instances = provider.widgets;
            int N = instances.size();
            for (int i = 0; i < N; i++) {
                updateAppWidgetInstanceLocked((Widget) instances.get(i), views, false);
            }
            if (componentName != null) {
                LogPower.push(DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE, componentName.getPackageName());
                this.mAWSIProxy.updateWidgetFlushReport(userId, componentName.getPackageName());
            }
        }
    }

    public void updateAppWidgetProviderInfo(ComponentName componentName, String metadataKey) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateAppWidgetProvider() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        this.mSecurityPolicy.enforceCallFromPackage(componentName.getPackageName());
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            ProviderId providerId = new ProviderId(Binder.getCallingUid(), componentName, null);
            Provider provider = lookupProviderLocked(providerId);
            if (provider == null) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(componentName);
                stringBuilder2.append(" is not a valid AppWidget provider");
                throw new IllegalArgumentException(stringBuilder2.toString());
            } else if (Objects.equals(provider.infoTag, metadataKey)) {
            } else {
                String keyToUse = metadataKey == null ? "android.appwidget.provider" : metadataKey;
                AppWidgetProviderInfo info = parseAppWidgetProviderInfo(providerId, provider.info.providerInfo, keyToUse);
                if (info != null) {
                    provider.info = info;
                    provider.infoTag = metadataKey;
                    int N = provider.widgets.size();
                    for (int i = 0; i < N; i++) {
                        Widget widget = (Widget) provider.widgets.get(i);
                        scheduleNotifyProviderChangedLocked(widget);
                        updateAppWidgetInstanceLocked(widget, widget.views, false);
                    }
                    saveGroupStateAsync(userId);
                    scheduleNotifyGroupHostsForProvidersChangedLocked(userId);
                    return;
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Unable to parse ");
                stringBuilder3.append(keyToUse);
                stringBuilder3.append(" meta-data to a valid AppWidget provider");
                throw new IllegalArgumentException(stringBuilder3.toString());
            }
        }
    }

    public boolean isRequestPinAppWidgetSupported() {
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.isCallerInstantAppLocked()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Instant uid ");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(" query information about app widgets");
                Slog.w(str, stringBuilder.toString());
                return false;
            }
            return ((ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class)).isRequestPinItemSupported(UserHandle.getCallingUserId(), 2);
        }
    }

    public boolean requestPinAppWidget(String callingPackage, ComponentName componentName, Bundle extras, IntentSender resultSender) {
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("requestPinAppWidget() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Provider provider = lookupProviderLocked(new ProviderId(callingUid, componentName, null));
            if (provider == null || provider.zombie) {
                return false;
            }
            AppWidgetProviderInfo info = provider.info;
            if ((info.widgetCategory & 1) == 0) {
                return false;
            }
            return ((ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class)).requestPinAppWidget(callingPackage, info, extras, resultSender, userId);
        }
    }

    public ParceledListSlice<AppWidgetProviderInfo> getInstalledProvidersForProfile(int categoryFilter, int profileId, String packageName) {
        String str;
        int i = profileId;
        String str2 = packageName;
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getInstalledProvidersForProfiles() ");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
        if (!this.mSecurityPolicy.isEnabledGroupProfile(i)) {
            return null;
        }
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.isCallerInstantAppLocked()) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Instant uid ");
                stringBuilder2.append(Binder.getCallingUid());
                stringBuilder2.append(" cannot access widget providers");
                Slog.w(str, stringBuilder2.toString());
                ParceledListSlice<AppWidgetProviderInfo> emptyList = ParceledListSlice.emptyList();
                return emptyList;
            }
            ensureGroupStateLoadedLocked(userId);
            ArrayList<AppWidgetProviderInfo> result = new ArrayList();
            int providerCount = this.mProviders.size();
            for (int i2 = 0; i2 < providerCount; i2++) {
                Provider provider = (Provider) this.mProviders.get(i2);
                AppWidgetProviderInfo info = provider.info;
                boolean inPackage = str2 == null || provider.id.componentName.getPackageName().equals(str2);
                if (!(provider.zombie || (info.widgetCategory & categoryFilter) == 0 || !inPackage)) {
                    ComponentName cn = info.provider;
                    if (!HIDE_HUAWEI_WEATHER_WIDGET || cn == null || !HIDDEN_WEATHER_WIDGETS.containsKey(cn.getClassName()) || !isThirdPartyLauncherActive()) {
                        int providerProfileId = info.getProfile().getIdentifier();
                        if (providerProfileId == i && this.mSecurityPolicy.isProviderInCallerOrInProfileAndWhitelListed(provider.id.componentName.getPackageName(), providerProfileId)) {
                            result.add(cloneIfLocalBinder(info));
                        }
                    }
                }
            }
            ParceledListSlice<AppWidgetProviderInfo> parceledListSlice = new ParceledListSlice(result);
            return parceledListSlice;
        }
    }

    private boolean isThirdPartyLauncherActive() {
        boolean isThridPartylauncherActive = false;
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        launcherIntent.addCategory("android.intent.category.HOME");
        List<ComponentName> prefActList = new ArrayList();
        List<IntentFilter> intentList = new ArrayList();
        PackageManager mPm = this.mContext.getPackageManager();
        if (mPm == null) {
            return false;
        }
        int userId = UserHandle.getCallingUserId();
        long origId = Binder.clearCallingIdentity();
        List<ResolveInfo> list = null;
        try {
            list = mPm.queryIntentActivitiesAsUser(launcherIntent, 0, userId);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isThirdPartyLauncherActive Exception:");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
        }
        Binder.restoreCallingIdentity(origId);
        if (list == null) {
            return false;
        }
        for (ResolveInfo info : list) {
            mPm.getPreferredActivities(intentList, prefActList, info.activityInfo.packageName);
            if (prefActList.size() > 0) {
                if (HUAWEI_LAUNCHER_PACKAGE.equals(info.activityInfo.packageName)) {
                    isThridPartylauncherActive = false;
                } else {
                    isThridPartylauncherActive = true;
                }
                return isThridPartylauncherActive;
            }
        }
        return isThridPartylauncherActive;
    }

    private void updateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views, boolean partially) {
        int userId = UserHandle.getCallingUserId();
        if (appWidgetIds != null && appWidgetIds.length != 0) {
            this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
            synchronized (this.mLock) {
                ensureGroupStateLoadedLocked(userId);
                for (int appWidgetId : appWidgetIds) {
                    Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
                    if (widget != null) {
                        updateAppWidgetInstanceLocked(widget, views, partially);
                        LogPower.push(DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE, callingPackage);
                        this.mAWSIProxy.updateWidgetFlushReport(userId, callingPackage);
                    }
                }
            }
        }
    }

    private int incrementAndGetAppWidgetIdLocked(int userId) {
        int appWidgetId = peekNextAppWidgetIdLocked(userId) + 1;
        this.mNextAppWidgetIds.put(userId, appWidgetId);
        return appWidgetId;
    }

    private void setMinAppWidgetIdLocked(int userId, int minWidgetId) {
        if (peekNextAppWidgetIdLocked(userId) < minWidgetId) {
            this.mNextAppWidgetIds.put(userId, minWidgetId);
        }
    }

    private int peekNextAppWidgetIdLocked(int userId) {
        if (this.mNextAppWidgetIds.indexOfKey(userId) < 0) {
            return 1;
        }
        return this.mNextAppWidgetIds.get(userId);
    }

    private Host lookupOrAddHostLocked(HostId id) {
        Host host = lookupHostLocked(id);
        if (host != null) {
            return host;
        }
        host = new Host();
        host.id = id;
        this.mHosts.add(host);
        return host;
    }

    private void deleteHostLocked(Host host) {
        for (int i = host.widgets.size() - 1; i >= 0; i--) {
            deleteAppWidgetLocked((Widget) host.widgets.remove(i));
        }
        this.mHosts.remove(host);
        host.callbacks = null;
    }

    private void deleteAppWidgetLocked(Widget widget) {
        decrementAppWidgetServiceRefCount(widget);
        Host host = widget.host;
        host.widgets.remove(widget);
        pruneHostLocked(host);
        removeWidgetLocked(widget);
        Provider provider = widget.provider;
        if (provider != null) {
            provider.widgets.remove(widget);
            if (!provider.zombie) {
                sendDeletedIntentLocked(widget);
                if (provider.widgets.isEmpty()) {
                    cancelBroadcasts(provider);
                    sendDisabledIntentLocked(provider);
                }
            }
        }
    }

    private void cancelBroadcasts(Provider provider) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cancelBroadcasts() for ");
            stringBuilder.append(provider);
            Slog.i(str, stringBuilder.toString());
        }
        if (provider.broadcast != null) {
            this.mAlarmManager.cancel(provider.broadcast);
            long token = Binder.clearCallingIdentity();
            try {
                provider.broadcast.cancel();
                provider.broadcast = null;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    private void destroyRemoteViewsService(final Intent intent, Widget widget) {
        ServiceConnection conn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    IRemoteViewsFactory.Stub.asInterface(service).onDestroy(intent);
                } catch (RemoteException re) {
                    Slog.e(AppWidgetServiceImpl.TAG, "Error calling remove view factory", re);
                }
                AppWidgetServiceImpl.this.mContext.unbindService(this);
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.bindServiceAsUser(intent, conn, 33554433, widget.provider.info.getProfile());
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void incrementAppWidgetServiceRefCount(int appWidgetId, Pair<Integer, FilterComparison> serviceId) {
        HashSet<Integer> appWidgetIds;
        if (this.mRemoteViewsServicesAppWidgets.containsKey(serviceId)) {
            appWidgetIds = (HashSet) this.mRemoteViewsServicesAppWidgets.get(serviceId);
        } else {
            appWidgetIds = new HashSet();
            this.mRemoteViewsServicesAppWidgets.put(serviceId, appWidgetIds);
        }
        appWidgetIds.add(Integer.valueOf(appWidgetId));
    }

    private void decrementAppWidgetServiceRefCount(Widget widget) {
        Iterator<Pair<Integer, FilterComparison>> it = this.mRemoteViewsServicesAppWidgets.keySet().iterator();
        while (it.hasNext()) {
            Pair<Integer, FilterComparison> key = (Pair) it.next();
            HashSet<Integer> ids = (HashSet) this.mRemoteViewsServicesAppWidgets.get(key);
            if (ids.remove(Integer.valueOf(widget.appWidgetId)) && ids.isEmpty()) {
                destroyRemoteViewsService(((FilterComparison) key.second).getIntent(), widget);
                it.remove();
            }
        }
    }

    private void saveGroupStateAsync(int groupId) {
        this.mSaveStateHandler.post(new SaveStateRunnable(groupId));
    }

    private void updateAppWidgetInstanceLocked(Widget widget, RemoteViews views, boolean isPartialUpdate) {
        RemoteViews oldRemoteViews = null;
        if (widget != null && widget.provider != null && !widget.provider.zombie && !widget.host.zombie) {
            if (!isPartialUpdate || widget.views == null) {
                if (!(widget.views == null || widget.views == views)) {
                    oldRemoteViews = widget.views;
                }
                widget.views = views;
            } else {
                widget.views.mergeRemoteViews(views);
            }
            if (!(UserHandle.getAppId(Binder.getCallingUid()) == 1000 || widget.views == null)) {
                int estimateMemoryUsage = widget.views.estimateMemoryUsage();
                int memoryUsage = estimateMemoryUsage;
                if (estimateMemoryUsage > this.mMaxWidgetBitmapMemory) {
                    widget.views = null;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("RemoteViews for widget update exceeds maximum bitmap memory usage (used: ");
                    stringBuilder.append(memoryUsage);
                    stringBuilder.append(", max: ");
                    stringBuilder.append(this.mMaxWidgetBitmapMemory);
                    stringBuilder.append(")");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            scheduleNotifyUpdateAppWidgetLocked(widget, widget.getEffectiveViewsLocked());
            if (oldRemoteViews != null) {
                Slog.i(TAG, "recycle oldRemoteView");
                scheduleRecyleRemoteView(oldRemoteViews);
            }
        }
    }

    private void scheduleNotifyAppWidgetViewDataChanged(Widget widget, int viewId) {
        if (viewId != 0 && viewId != 1) {
            long requestId = UPDATE_COUNTER.incrementAndGet();
            if (widget != null) {
                widget.updateSequenceNos.put(viewId, requestId);
            }
            if (widget != null && widget.host != null && !widget.host.zombie && widget.host.callbacks != null && widget.provider != null && !widget.provider.zombie) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = widget.host;
                args.arg2 = widget.host.callbacks;
                args.arg3 = Long.valueOf(requestId);
                args.argi1 = widget.appWidgetId;
                args.argi2 = viewId;
                this.mCallbackHandler.obtainMessage(4, args).sendToTarget();
            }
        }
    }

    private void handleNotifyAppWidgetViewDataChanged(Host host, IAppWidgetHost callbacks, int appWidgetId, int viewId, long requestId) {
        try {
            callbacks.viewDataChanged(appWidgetId, viewId);
            host.lastWidgetUpdateSequenceNo = requestId;
        } catch (RemoteException e) {
            callbacks = null;
        }
        synchronized (this.mLock) {
            if (callbacks == null) {
                host.callbacks = null;
                for (Pair<Integer, FilterComparison> key : this.mRemoteViewsServicesAppWidgets.keySet()) {
                    if (((HashSet) this.mRemoteViewsServicesAppWidgets.get(key)).contains(Integer.valueOf(appWidgetId))) {
                        bindService(((FilterComparison) key.second).getIntent(), new ServiceConnection() {
                            public void onServiceConnected(ComponentName name, IBinder service) {
                                try {
                                    IRemoteViewsFactory.Stub.asInterface(service).onDataSetChangedAsync();
                                } catch (RemoteException e) {
                                    Slog.e(AppWidgetServiceImpl.TAG, "Error calling onDataSetChangedAsync()", e);
                                }
                                AppWidgetServiceImpl.this.mContext.unbindService(this);
                            }

                            public void onServiceDisconnected(ComponentName name) {
                            }
                        }, new UserHandle(UserHandle.getUserId(((Integer) key.first).intValue())));
                    }
                }
            }
        }
    }

    private void scheduleNotifyUpdateAppWidgetLocked(Widget widget, RemoteViews updateViews) {
        long requestId = UPDATE_COUNTER.incrementAndGet();
        if (widget != null) {
            widget.updateSequenceNos.put(0, requestId);
        }
        if (widget == null || widget.provider == null || widget.provider.zombie || widget.host.callbacks == null || widget.host.zombie) {
            Slog.i(TAG, "Widget is null when scheduleNotifyUpdateAppWidgetLocked");
            return;
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = widget.host;
        args.arg2 = widget.host.callbacks;
        args.arg3 = updateViews != null ? updateViews.clone() : null;
        args.arg4 = Long.valueOf(requestId);
        args.argi1 = widget.appWidgetId;
        this.mCallbackHandler.obtainMessage(1, args).sendToTarget();
    }

    private void handleNotifyUpdateAppWidget(Host host, IAppWidgetHost callbacks, int appWidgetId, RemoteViews views, long requestId) {
        try {
            callbacks.updateAppWidget(appWidgetId, views);
            host.lastWidgetUpdateSequenceNo = requestId;
        } catch (RemoteException re) {
            synchronized (this.mLock) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Widget host dead: ");
                stringBuilder.append(host.id);
                Slog.e(str, stringBuilder.toString(), re);
                host.callbacks = null;
            }
        }
    }

    private void scheduleNotifyProviderChangedLocked(Widget widget) {
        long requestId = UPDATE_COUNTER.incrementAndGet();
        if (widget != null) {
            widget.updateSequenceNos.clear();
            widget.updateSequenceNos.append(1, requestId);
        }
        if (widget == null || widget.provider == null || widget.provider.zombie || widget.host.callbacks == null || widget.host.zombie) {
            Slog.i(TAG, "widget may be null when scheduleNotifyProviderChangedLocked");
            return;
        }
        if (!(widget.provider.info == null || widget.provider.info.providerInfo == null || widget.provider.info.providerInfo.applicationInfo == null || !"com.huawei.health".equals(widget.provider.info.providerInfo.packageName))) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("scheduleNotifyProviderChangedLocked, sourceDir=");
            stringBuilder.append(widget.provider.info.providerInfo.applicationInfo.sourceDir);
            Slog.i(str, stringBuilder.toString());
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = widget.host;
        args.arg2 = widget.host.callbacks;
        args.arg3 = widget.provider.info;
        args.arg4 = Long.valueOf(requestId);
        args.argi1 = widget.appWidgetId;
        this.mCallbackHandler.obtainMessage(2, args).sendToTarget();
    }

    private void scheduleRecyleRemoteView(RemoteViews views) {
        if (views != null) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = views;
            this.mCallbackHandler.obtainMessage(20, args).sendToTarget();
        }
    }

    private void handleNotifyProviderChanged(Host host, IAppWidgetHost callbacks, int appWidgetId, AppWidgetProviderInfo info, long requestId) {
        try {
            callbacks.providerChanged(appWidgetId, info);
            host.lastWidgetUpdateSequenceNo = requestId;
        } catch (RemoteException re) {
            synchronized (this.mLock) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Widget host dead: ");
                stringBuilder.append(host.id);
                Slog.e(str, stringBuilder.toString(), re);
                host.callbacks = null;
            }
        }
    }

    private void scheduleNotifyGroupHostsForProvidersChangedLocked(int userId) {
        int[] profileIds = this.mSecurityPolicy.getEnabledGroupProfileIds(userId);
        for (int i = this.mHosts.size() - 1; i >= 0; i--) {
            Host host = (Host) this.mHosts.get(i);
            boolean hostInGroup = false;
            for (int profileId : profileIds) {
                if (host.getUserId() == profileId) {
                    hostInGroup = true;
                    break;
                }
            }
            if (!(!hostInGroup || host == null || host.zombie || host.callbacks == null)) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = host;
                args.arg2 = host.callbacks;
                this.mCallbackHandler.obtainMessage(3, args).sendToTarget();
            }
        }
    }

    private void handleNotifyProvidersChanged(Host host, IAppWidgetHost callbacks) {
        try {
            callbacks.providersChanged();
        } catch (RemoteException re) {
            synchronized (this.mLock) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Widget host dead: ");
                stringBuilder.append(host.id);
                Slog.e(str, stringBuilder.toString(), re);
                host.callbacks = null;
            }
        }
    }

    private static boolean isLocalBinder() {
        return Process.myPid() == Binder.getCallingPid();
    }

    private static RemoteViews cloneIfLocalBinder(RemoteViews rv) {
        if (!isLocalBinder() || rv == null) {
            return rv;
        }
        return rv.clone();
    }

    private static AppWidgetProviderInfo cloneIfLocalBinder(AppWidgetProviderInfo info) {
        if (!isLocalBinder() || info == null) {
            return info;
        }
        return info.clone();
    }

    private static Bundle cloneIfLocalBinder(Bundle bundle) {
        if (!isLocalBinder() || bundle == null) {
            return bundle;
        }
        return (Bundle) bundle.clone();
    }

    private Widget lookupWidgetLocked(int appWidgetId, int uid, String packageName) {
        int N = this.mWidgets.size();
        for (int i = 0; i < N; i++) {
            Widget widget = (Widget) this.mWidgets.get(i);
            if (widget.appWidgetId == appWidgetId && this.mSecurityPolicy.canAccessAppWidget(widget, uid, packageName)) {
                return widget;
            }
        }
        return null;
    }

    private Provider lookupProviderLocked(ProviderId id) {
        int N = this.mProviders.size();
        for (int i = 0; i < N; i++) {
            Provider provider = (Provider) this.mProviders.get(i);
            if (provider.id.equals(id)) {
                return provider;
            }
        }
        return null;
    }

    private Host lookupHostLocked(HostId hostId) {
        int N = this.mHosts.size();
        for (int i = 0; i < N; i++) {
            Host host = (Host) this.mHosts.get(i);
            if (host.id.equals(hostId)) {
                return host;
            }
        }
        return null;
    }

    private void pruneHostLocked(Host host) {
        if (host.widgets.size() == 0 && host.callbacks == null) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Pruning host ");
                stringBuilder.append(host.id);
                Slog.i(str, stringBuilder.toString());
            }
            this.mHosts.remove(host);
        }
    }

    private void loadGroupWidgetProvidersLocked(int[] profileIds) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        int i = 0;
        List<ResolveInfo> allReceivers = null;
        for (int profileId : profileIds) {
            List<ResolveInfo> receivers = queryIntentReceivers(intent, profileId);
            if (!(receivers == null || receivers.isEmpty())) {
                if (allReceivers == null) {
                    allReceivers = new ArrayList();
                }
                allReceivers.addAll(receivers);
            }
        }
        int i2 = allReceivers == null ? 0 : allReceivers.size();
        while (i < i2) {
            addProviderLocked((ResolveInfo) allReceivers.get(i));
            i++;
        }
    }

    private boolean addProviderLocked(ResolveInfo ri) {
        if ((ri.activityInfo.applicationInfo.flags & 262144) != 0) {
            return false;
        }
        if (!ri.activityInfo.isEnabled() && !MUSlIM_APP_WIDGET_PACKAGE.equals(ri.activityInfo.name)) {
            return false;
        }
        ComponentName componentName = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
        ProviderId providerId = new ProviderId(ri.activityInfo.applicationInfo.uid, componentName, null);
        Provider provider = parseProviderInfoXml(providerId, ri, null);
        if (provider == null) {
            return false;
        }
        Provider existing = lookupProviderLocked(providerId);
        if (existing == null) {
            existing = lookupProviderLocked(new ProviderId(-1, componentName, null));
        }
        if (existing == null) {
            this.mProviders.add(provider);
        } else if (existing.zombie && !this.mSafeMode) {
            existing.id = providerId;
            existing.zombie = false;
            existing.info = provider.info;
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Provider placeholder now reified: ");
                stringBuilder.append(existing);
                Slog.i(str, stringBuilder.toString());
            }
        }
        return true;
    }

    private void deleteWidgetsLocked(Provider provider, int userId) {
        for (int i = provider.widgets.size() - 1; i >= 0; i--) {
            Widget widget = (Widget) provider.widgets.get(i);
            if (userId == -1 || userId == widget.host.getUserId()) {
                provider.widgets.remove(i);
                updateAppWidgetInstanceLocked(widget, null, false);
                widget.host.widgets.remove(widget);
                removeWidgetLocked(widget);
                widget.provider = null;
                pruneHostLocked(widget.host);
                widget.host = null;
            }
        }
    }

    private void deleteProviderLocked(Provider provider) {
        deleteWidgetsLocked(provider, -1);
        this.mProviders.remove(provider);
        cancelBroadcasts(provider);
    }

    private void sendEnableIntentLocked(Provider p) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_ENABLED");
        intent.setComponent(p.info.provider);
        sendBroadcastAsUser(intent, p.info.getProfile());
    }

    private void sendUpdateIntentLocked(Provider provider, int[] appWidgetIds) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.putExtra("appWidgetIds", appWidgetIds);
        intent.setComponent(provider.info.provider);
        sendBroadcastAsUser(intent, provider.info.getProfile());
    }

    private void sendDeletedIntentLocked(Widget widget) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_DELETED");
        intent.setComponent(widget.provider.info.provider);
        intent.putExtra("appWidgetId", widget.appWidgetId);
        sendBroadcastAsUser(intent, widget.provider.info.getProfile());
    }

    private void sendDisabledIntentLocked(Provider provider) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_DISABLED");
        intent.setComponent(provider.info.provider);
        sendBroadcastAsUser(intent, provider.info.getProfile());
    }

    public void sendOptionsChangedIntentLocked(Widget widget) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE_OPTIONS");
        intent.setComponent(widget.provider.info.provider);
        intent.putExtra("appWidgetId", widget.appWidgetId);
        intent.putExtra("appWidgetOptions", widget.options);
        sendBroadcastAsUser(intent, widget.provider.info.getProfile());
    }

    private void registerForBroadcastsLocked(Provider provider, int[] appWidgetIds) {
        Throwable th;
        Provider provider2 = provider;
        if (provider2.info.updatePeriodMillis > 0) {
            boolean alreadyRegistered = provider2.broadcast != null;
            Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
            intent.putExtra("appWidgetIds", appWidgetIds);
            intent.setComponent(provider2.info.provider);
            long token = Binder.clearCallingIdentity();
            try {
                provider2.broadcast = PendingIntent.getBroadcastAsUser(this.mContext, 1, intent, 134217728, provider2.info.getProfile());
                if (!alreadyRegistered) {
                    long period = (long) provider2.info.updatePeriodMillis;
                    if (period < ((long) MIN_UPDATE_PERIOD)) {
                        period = (long) MIN_UPDATE_PERIOD;
                    }
                    long oldId = Binder.clearCallingIdentity();
                    long oldId2;
                    try {
                        AlarmManager alarmManager = this.mAlarmManager;
                        oldId2 = oldId;
                        try {
                            alarmManager.setInexactRepeating(2, SystemClock.elapsedRealtime() + period, period, provider2.broadcast);
                            Binder.restoreCallingIdentity(oldId2);
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(oldId2);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        oldId2 = oldId;
                        Binder.restoreCallingIdentity(oldId2);
                        throw th;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            int[] iArr = appWidgetIds;
        }
    }

    private static int[] getWidgetIds(ArrayList<Widget> widgets) {
        int instancesSize = widgets.size();
        int[] appWidgetIds = new int[instancesSize];
        for (int i = 0; i < instancesSize; i++) {
            appWidgetIds[i] = ((Widget) widgets.get(i)).appWidgetId;
        }
        return appWidgetIds;
    }

    private static void dumpProvider(Provider provider, int index, PrintWriter pw) {
        AppWidgetProviderInfo info = provider.info;
        pw.print("  [");
        pw.print(index);
        pw.print("] provider ");
        pw.println(provider.id);
        pw.print("    min=(");
        pw.print(info.minWidth);
        pw.print("x");
        pw.print(info.minHeight);
        pw.print(")   minResize=(");
        pw.print(info.minResizeWidth);
        pw.print("x");
        pw.print(info.minResizeHeight);
        pw.print(") updatePeriodMillis=");
        pw.print(info.updatePeriodMillis);
        pw.print(" resizeMode=");
        pw.print(info.resizeMode);
        pw.print(" widgetCategory=");
        pw.print(info.widgetCategory);
        pw.print(" autoAdvanceViewId=");
        pw.print(info.autoAdvanceViewId);
        pw.print(" initialLayout=#");
        pw.print(Integer.toHexString(info.initialLayout));
        pw.print(" initialKeyguardLayout=#");
        pw.print(Integer.toHexString(info.initialKeyguardLayout));
        pw.print(" zombie=");
        pw.println(provider.zombie);
    }

    private static void dumpHost(Host host, int index, PrintWriter pw) {
        pw.print("  [");
        pw.print(index);
        pw.print("] hostId=");
        pw.println(host.id);
        pw.print("    callbacks=");
        pw.println(host.callbacks);
        pw.print("    widgets.size=");
        pw.print(host.widgets.size());
        pw.print(" zombie=");
        pw.println(host.zombie);
    }

    private static void dumpGrant(Pair<Integer, String> grant, int index, PrintWriter pw) {
        pw.print("  [");
        pw.print(index);
        pw.print(']');
        pw.print(" user=");
        pw.print(grant.first);
        pw.print(" package=");
        pw.println((String) grant.second);
    }

    private static void dumpWidget(Widget widget, int index, PrintWriter pw) {
        pw.print("  [");
        pw.print(index);
        pw.print("] id=");
        pw.println(widget.appWidgetId);
        pw.print("    host=");
        pw.println(widget.host.id);
        if (widget.provider != null) {
            pw.print("    provider=");
            pw.println(widget.provider.id);
        }
        if (widget.host != null) {
            pw.print("    host.callbacks=");
            pw.println(widget.host.callbacks);
        }
        if (widget.views != null) {
            pw.print("    views=");
            pw.println(widget.views);
        }
    }

    private static void serializeProvider(XmlSerializer out, Provider p) throws IOException {
        out.startTag(null, "p");
        out.attribute(null, AbsLocationManagerService.DEL_PKG, p.info.provider.getPackageName());
        out.attribute(null, "cl", p.info.provider.getClassName());
        out.attribute(null, "tag", Integer.toHexString(p.tag));
        if (!TextUtils.isEmpty(p.infoTag)) {
            out.attribute(null, "info_tag", p.infoTag);
        }
        out.endTag(null, "p");
    }

    private static void serializeHost(XmlSerializer out, Host host) throws IOException {
        out.startTag(null, "h");
        out.attribute(null, AbsLocationManagerService.DEL_PKG, host.id.packageName);
        out.attribute(null, "id", Integer.toHexString(host.id.hostId));
        out.attribute(null, "tag", Integer.toHexString(host.tag));
        out.endTag(null, "h");
    }

    private static void serializeAppWidget(XmlSerializer out, Widget widget) throws IOException {
        out.startTag(null, "g");
        out.attribute(null, "id", Integer.toHexString(widget.appWidgetId));
        out.attribute(null, "rid", Integer.toHexString(widget.restoredId));
        out.attribute(null, "h", Integer.toHexString(widget.host.tag));
        if (widget.provider != null) {
            out.attribute(null, "p", Integer.toHexString(widget.provider.tag));
        }
        if (widget.options != null) {
            int minWidth = widget.options.getInt("appWidgetMinWidth");
            int minHeight = widget.options.getInt("appWidgetMinHeight");
            int maxWidth = widget.options.getInt("appWidgetMaxWidth");
            int maxHeight = widget.options.getInt("appWidgetMaxHeight");
            int i = 0;
            out.attribute(null, "min_width", Integer.toHexString(minWidth > 0 ? minWidth : 0));
            out.attribute(null, "min_height", Integer.toHexString(minHeight > 0 ? minHeight : 0));
            out.attribute(null, "max_width", Integer.toHexString(maxWidth > 0 ? maxWidth : 0));
            String str = "max_height";
            if (maxHeight > 0) {
                i = maxHeight;
            }
            out.attribute(null, str, Integer.toHexString(i));
            out.attribute(null, "host_category", Integer.toHexString(widget.options.getInt("appWidgetCategory")));
        }
        out.endTag(null, "g");
    }

    public List<String> getWidgetParticipants(int userId) {
        return this.mBackupRestoreController.getWidgetParticipants(userId);
    }

    public byte[] getWidgetState(String packageName, int userId) {
        return this.mBackupRestoreController.getWidgetState(packageName, userId);
    }

    public void restoreStarting(int userId) {
        this.mBackupRestoreController.restoreStarting(userId);
    }

    public void restoreWidgetState(String packageName, byte[] restoredState, int userId) {
        this.mBackupRestoreController.restoreWidgetState(packageName, restoredState, userId);
    }

    public void restoreFinished(int userId) {
        this.mBackupRestoreController.restoreFinished(userId);
    }

    private Provider parseProviderInfoXml(ProviderId providerId, ResolveInfo ri, Provider oldProvider) {
        AppWidgetProviderInfo info = null;
        if (!(oldProvider == null || TextUtils.isEmpty(oldProvider.infoTag))) {
            info = parseAppWidgetProviderInfo(providerId, ri.activityInfo, oldProvider.infoTag);
        }
        if (info == null) {
            info = parseAppWidgetProviderInfo(providerId, ri.activityInfo, "android.appwidget.provider");
        }
        if (info == null) {
            return null;
        }
        Provider provider = new Provider();
        provider.id = providerId;
        provider.info = info;
        return provider;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Exception block dominator not found, method:com.android.server.appwidget.AppWidgetServiceImpl.parseAppWidgetProviderInfo(com.android.server.appwidget.AppWidgetServiceImpl$ProviderId, android.content.pm.ActivityInfo, java.lang.String):android.appwidget.AppWidgetProviderInfo, dom blocks: [B:5:0x0017, B:14:0x0048, B:30:0x00a0, B:66:0x0180]
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.searchTryCatchDominators(ProcessTryCatchRegions.java:89)
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.process(ProcessTryCatchRegions.java:45)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.postProcessRegions(RegionMakerVisitor.java:63)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:58)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x0187 A:{Splitter: B:1:0x0009, ExcHandler: java.io.IOException (r0_39 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x0187 A:{Splitter: B:1:0x0009, ExcHandler: java.io.IOException (r0_39 'e' java.lang.Exception)} */
    private android.appwidget.AppWidgetProviderInfo parseAppWidgetProviderInfo(com.android.server.appwidget.AppWidgetServiceImpl.ProviderId r19, android.content.pm.ActivityInfo r20, java.lang.String r21) {
        /*
        r18 = this;
        r1 = r18;
        r2 = r19;
        r3 = r20;
        r4 = r21;
        r5 = 0;
        r0 = r1.mContext;	 Catch:{ IOException -> 0x0187, IOException -> 0x0187, IOException -> 0x0187 }
        r0 = r0.getPackageManager();	 Catch:{ IOException -> 0x0187, IOException -> 0x0187, IOException -> 0x0187 }
        r0 = r3.loadXmlMetaData(r0, r4);	 Catch:{ IOException -> 0x0187, IOException -> 0x0187, IOException -> 0x0187 }
        r6 = r0;
        if (r6 != 0) goto L_0x0048;
    L_0x0017:
        r0 = "AppWidgetServiceImpl";	 Catch:{ Throwable -> 0x0044 }
        r7 = new java.lang.StringBuilder;	 Catch:{ Throwable -> 0x0044 }
        r7.<init>();	 Catch:{ Throwable -> 0x0044 }
        r8 = "No ";	 Catch:{ Throwable -> 0x0044 }
        r7.append(r8);	 Catch:{ Throwable -> 0x0044 }
        r7.append(r4);	 Catch:{ Throwable -> 0x0044 }
        r8 = " meta-data for AppWidget provider '";	 Catch:{ Throwable -> 0x0044 }
        r7.append(r8);	 Catch:{ Throwable -> 0x0044 }
        r7.append(r2);	 Catch:{ Throwable -> 0x0044 }
        r8 = 39;	 Catch:{ Throwable -> 0x0044 }
        r7.append(r8);	 Catch:{ Throwable -> 0x0044 }
        r7 = r7.toString();	 Catch:{ Throwable -> 0x0044 }
        android.util.Slog.w(r0, r7);	 Catch:{ Throwable -> 0x0044 }
        if (r6 == 0) goto L_0x0040;
    L_0x003d:
        $closeResource(r5, r6);	 Catch:{ IOException -> 0x0187, IOException -> 0x0187, IOException -> 0x0187 }
    L_0x0040:
        return r5;
    L_0x0041:
        r0 = move-exception;
        goto L_0x0181;
    L_0x0044:
        r0 = move-exception;
        r5 = r0;
        goto L_0x0180;
    L_0x0048:
        r0 = android.util.Xml.asAttributeSet(r6);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x004c:
        r7 = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r6.next();	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r8 = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r9 = 2;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r10 = 1;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        if (r0 == r10) goto L_0x005a;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x0056:
        if (r8 == r9) goto L_0x005a;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x0058:
        r0 = r7;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        goto L_0x004c;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x005a:
        r0 = r6.getName();	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r11 = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = "appwidget-provider";	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r0.equals(r11);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        if (r0 != 0) goto L_0x0090;
    L_0x0067:
        r0 = "AppWidgetServiceImpl";	 Catch:{ Throwable -> 0x0044 }
        r9 = new java.lang.StringBuilder;	 Catch:{ Throwable -> 0x0044 }
        r9.<init>();	 Catch:{ Throwable -> 0x0044 }
        r10 = "Meta-data does not start with appwidget-provider tag for AppWidget provider ";	 Catch:{ Throwable -> 0x0044 }
        r9.append(r10);	 Catch:{ Throwable -> 0x0044 }
        r10 = r2.componentName;	 Catch:{ Throwable -> 0x0044 }
        r9.append(r10);	 Catch:{ Throwable -> 0x0044 }
        r10 = " for user ";	 Catch:{ Throwable -> 0x0044 }
        r9.append(r10);	 Catch:{ Throwable -> 0x0044 }
        r10 = r2.uid;	 Catch:{ Throwable -> 0x0044 }
        r9.append(r10);	 Catch:{ Throwable -> 0x0044 }
        r9 = r9.toString();	 Catch:{ Throwable -> 0x0044 }
        android.util.Slog.w(r0, r9);	 Catch:{ Throwable -> 0x0044 }
        if (r6 == 0) goto L_0x008f;
    L_0x008c:
        $closeResource(r5, r6);	 Catch:{ IOException -> 0x0187, IOException -> 0x0187, IOException -> 0x0187 }
    L_0x008f:
        return r5;
    L_0x0090:
        r0 = new android.appwidget.AppWidgetProviderInfo;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0.<init>();	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12 = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r2.componentName;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.provider = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.providerInfo = r3;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r13 = android.os.Binder.clearCallingIdentity();	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r1.mContext;	 Catch:{ all -> 0x0178 }
        r0 = r0.getPackageManager();	 Catch:{ all -> 0x0178 }
        r15 = r2.uid;	 Catch:{ all -> 0x0178 }
        r15 = android.os.UserHandle.getUserId(r15);	 Catch:{ all -> 0x0178 }
        r5 = r3.packageName;	 Catch:{ all -> 0x0178 }
        r9 = 0;	 Catch:{ all -> 0x0178 }
        r5 = r0.getApplicationInfoAsUser(r5, r9, r15);	 Catch:{ all -> 0x0178 }
        r16 = r0.getResourcesForApplication(r5);	 Catch:{ all -> 0x0178 }
        r0 = r16;
        android.os.Binder.restoreCallingIdentity(r13);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r5 = com.android.internal.R.styleable.AppWidgetProviderInfo;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r5 = r0.obtainAttributes(r7, r5);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15 = r5.peekValue(r9);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        if (r15 == 0) goto L_0x00ce;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00cb:
        r9 = r15.data;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        goto L_0x00cf;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00ce:
        r9 = 0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00cf:
        r12.minWidth = r9;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r9 = r5.peekValue(r10);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        if (r9 == 0) goto L_0x00da;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00d7:
        r15 = r9.data;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        goto L_0x00db;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00da:
        r15 = 0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00db:
        r12.minHeight = r15;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15 = 8;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15 = r5.peekValue(r15);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r9 = r15;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        if (r9 == 0) goto L_0x00e9;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00e6:
        r15 = r9.data;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        goto L_0x00eb;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00e9:
        r15 = r12.minWidth;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00eb:
        r12.minResizeWidth = r15;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15 = 9;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15 = r5.peekValue(r15);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r9 = r15;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        if (r9 == 0) goto L_0x00f9;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00f6:
        r15 = r9.data;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        goto L_0x00fb;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00f9:
        r15 = r12.minHeight;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x00fb:
        r12.minResizeHeight = r15;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r10 = 2;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15 = 0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r10 = r5.getInt(r10, r15);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.updatePeriodMillis = r10;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r10 = 3;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r10 = r5.getResourceId(r10, r15);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.initialLayout = r10;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r10 = 10;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r10 = r5.getResourceId(r10, r15);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.initialKeyguardLayout = r10;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r10 = 4;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r10 = r5.getString(r10);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        if (r10 == 0) goto L_0x012b;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x011b:
        r15 = new android.content.ComponentName;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r17 = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r2.componentName;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r0.getPackageName();	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15.<init>(r0, r10);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.configure = r15;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        goto L_0x012d;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x012b:
        r17 = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x012d:
        r0 = r1.mContext;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r0.getPackageManager();	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r3.loadLabel(r0);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r0.toString();	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.label = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r20.getIconResource();	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.icon = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = 5;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15 = 0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r5.getResourceId(r0, r15);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.previewImage = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = 6;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15 = -1;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r5.getResourceId(r0, r15);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.autoAdvanceViewId = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = 7;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15 = 0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r5.getInt(r0, r15);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.resizeMode = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = 11;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15 = 1;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r5.getInt(r0, r15);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.widgetCategory = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = 12;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r15 = 0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r0 = r5.getInt(r0, r15);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r12.widgetFeatures = r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        r5.recycle();	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        if (r6 == 0) goto L_0x0177;
    L_0x0173:
        r15 = 0;
        $closeResource(r15, r6);	 Catch:{ IOException -> 0x0187, IOException -> 0x0187, IOException -> 0x0187 }
    L_0x0177:
        return r12;
    L_0x0178:
        r0 = move-exception;
        android.os.Binder.restoreCallingIdentity(r13);	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
        throw r0;	 Catch:{ Throwable -> 0x0044, all -> 0x017d }
    L_0x017d:
        r0 = move-exception;
        r5 = 0;
        goto L_0x0181;
    L_0x0180:
        throw r5;	 Catch:{ all -> 0x0041 }
    L_0x0181:
        if (r6 == 0) goto L_0x0186;
    L_0x0183:
        $closeResource(r5, r6);	 Catch:{ IOException -> 0x0187, IOException -> 0x0187, IOException -> 0x0187 }
    L_0x0186:
        throw r0;	 Catch:{ IOException -> 0x0187, IOException -> 0x0187, IOException -> 0x0187 }
    L_0x0187:
        r0 = move-exception;
        r5 = "AppWidgetServiceImpl";
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "XML parsing failed for AppWidget provider ";
        r6.append(r7);
        r7 = r2.componentName;
        r6.append(r7);
        r7 = " for user ";
        r6.append(r7);
        r7 = r2.uid;
        r6.append(r7);
        r6 = r6.toString();
        android.util.Slog.w(r5, r6, r0);
        r5 = 0;
        return r5;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.appwidget.AppWidgetServiceImpl.parseAppWidgetProviderInfo(com.android.server.appwidget.AppWidgetServiceImpl$ProviderId, android.content.pm.ActivityInfo, java.lang.String):android.appwidget.AppWidgetProviderInfo");
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private int getUidForPackage(String packageName, int userId) {
        PackageInfo pkgInfo = null;
        long identity = Binder.clearCallingIdentity();
        try {
            pkgInfo = this.mPackageManager.getPackageInfo(packageName, 0, userId);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
        Binder.restoreCallingIdentity(identity);
        if (pkgInfo == null || pkgInfo.applicationInfo == null) {
            return -1;
        }
        return pkgInfo.applicationInfo.uid;
    }

    private ActivityInfo getProviderInfo(ComponentName componentName, int userId) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.setComponent(componentName);
        List<ResolveInfo> receivers = queryIntentReceivers(intent, userId);
        if (receivers.isEmpty()) {
            return null;
        }
        return ((ResolveInfo) receivers.get(0)).activityInfo;
    }

    private List<ResolveInfo> queryIntentReceivers(Intent intent, int userId) {
        long identity = Binder.clearCallingIdentity();
        List<ResolveInfo> list = 268435456;
        int flags = 128 | 268435456;
        try {
            if (isProfileWithUnlockedParent(userId)) {
                flags |= 786432;
            }
            list = this.mPackageManager.queryIntentReceivers(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), flags | 1024, userId).getList();
            return list;
        } catch (RemoteException e) {
            list = Collections.emptyList();
            return list;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    void onUserUnlocked(int userId) {
        if (!isProfileWithLockedParent(userId)) {
            if (this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
                long time = SystemClock.elapsedRealtime();
                synchronized (this.mLock) {
                    Trace.traceBegin(64, "appwidget ensure");
                    ensureGroupStateLoadedLocked(userId);
                    Trace.traceEnd(64);
                    Trace.traceBegin(64, "appwidget reload");
                    reloadWidgetsMaskedStateForGroup(this.mSecurityPolicy.getGroupParent(userId));
                    Trace.traceEnd(64);
                    int N = this.mProviders.size();
                    for (int i = 0; i < N; i++) {
                        Provider provider = (Provider) this.mProviders.get(i);
                        if (provider.getUserId() == userId && provider.widgets.size() > 0) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("appwidget init ");
                            stringBuilder.append(provider.info.provider.getPackageName());
                            Trace.traceBegin(64, stringBuilder.toString());
                            sendEnableIntentLocked(provider);
                            int[] appWidgetIds = getWidgetIds(provider.widgets);
                            sendUpdateIntentLocked(provider, appWidgetIds);
                            registerForBroadcastsLocked(provider, appWidgetIds);
                            Trace.traceEnd(64);
                        }
                    }
                }
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Async processing of onUserUnlocked u");
                stringBuilder2.append(userId);
                stringBuilder2.append(" took ");
                stringBuilder2.append(SystemClock.elapsedRealtime() - time);
                stringBuilder2.append(" ms");
                Slog.i(str, stringBuilder2.toString());
                return;
            }
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("User ");
            stringBuilder3.append(userId);
            stringBuilder3.append(" is no longer unlocked - exiting");
            Slog.w(str2, stringBuilder3.toString());
        }
    }

    private void loadGroupStateLocked(int[] profileIds) {
        int i;
        List<LoadedWidgetState> loadedWidgets = new ArrayList();
        int i2 = 0;
        int version = 0;
        for (int profileId : profileIds) {
            FileInputStream stream;
            try {
                stream = getSavedStateFile(profileId).openRead();
                version = readProfileStateFromFileLocked(stream, profileId, loadedWidgets);
                if (stream != null) {
                    $closeResource(null, stream);
                }
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to read state: ");
                stringBuilder.append(e);
                Slog.w(str, stringBuilder.toString());
            } catch (Throwable th) {
                if (stream != null) {
                    $closeResource(r8, stream);
                }
            }
        }
        if (version >= 0) {
            bindLoadedWidgetsLocked(loadedWidgets);
            performUpgradeLocked(version);
            return;
        }
        Slog.w(TAG, "Failed to read state, clearing widgets and hosts.");
        clearWidgetsLocked();
        this.mHosts.clear();
        i = this.mProviders.size();
        while (i2 < i) {
            ((Provider) this.mProviders.get(i2)).widgets.clear();
            i2++;
        }
    }

    private void bindLoadedWidgetsLocked(List<LoadedWidgetState> loadedWidgets) {
        for (int i = loadedWidgets.size() - 1; i >= 0; i--) {
            LoadedWidgetState loadedWidget = (LoadedWidgetState) loadedWidgets.remove(i);
            Widget widget = loadedWidget.widget;
            widget.provider = findProviderByTag(loadedWidget.providerTag);
            if (widget.provider != null) {
                widget.host = findHostByTag(loadedWidget.hostTag);
                if (widget.host != null) {
                    widget.provider.widgets.add(widget);
                    widget.host.widgets.add(widget);
                    addWidgetLocked(widget);
                }
            }
        }
    }

    private Provider findProviderByTag(int tag) {
        if (tag < 0) {
            return null;
        }
        int providerCount = this.mProviders.size();
        for (int i = 0; i < providerCount; i++) {
            Provider provider = (Provider) this.mProviders.get(i);
            if (provider.tag == tag) {
                return provider;
            }
        }
        return null;
    }

    private Host findHostByTag(int tag) {
        if (tag < 0) {
            return null;
        }
        int hostCount = this.mHosts.size();
        for (int i = 0; i < hostCount; i++) {
            Host host = (Host) this.mHosts.get(i);
            if (host.tag == tag) {
                return host;
            }
        }
        return null;
    }

    void addWidgetLocked(Widget widget) {
        this.mWidgets.add(widget);
        onWidgetProviderAddedOrChangedLocked(widget);
    }

    void onWidgetProviderAddedOrChangedLocked(Widget widget) {
        if (widget.provider != null) {
            int userId = widget.provider.getUserId();
            ArraySet<String> packages = (ArraySet) this.mWidgetPackages.get(userId);
            if (packages == null) {
                SparseArray sparseArray = this.mWidgetPackages;
                ArraySet<String> arraySet = new ArraySet();
                packages = arraySet;
                sparseArray.put(userId, arraySet);
            }
            packages.add(widget.provider.info.provider.getPackageName());
            if (widget.provider.isMaskedLocked()) {
                maskWidgetsViewsLocked(widget.provider, widget);
            } else {
                widget.clearMaskedViewsLocked();
            }
            addWidgetReport(userId, widget);
        }
    }

    void removeWidgetLocked(Widget widget) {
        this.mWidgets.remove(widget);
        onWidgetRemovedLocked(widget);
    }

    private void onWidgetRemovedLocked(Widget widget) {
        if (widget.provider != null) {
            int userId = widget.provider.getUserId();
            String packageName = widget.provider.info.provider.getPackageName();
            removeWidgetReport(userId, widget);
            ArraySet<String> packages = (ArraySet) this.mWidgetPackages.get(userId);
            if (packages != null) {
                int N = this.mWidgets.size();
                int i = 0;
                while (i < N) {
                    Widget w = (Widget) this.mWidgets.get(i);
                    if (w.provider == null || w.provider.getUserId() != userId || !packageName.equals(w.provider.info.provider.getPackageName())) {
                        i++;
                    } else {
                        return;
                    }
                }
                packages.remove(packageName);
            }
        }
    }

    protected void addWidgetReport(int userId, Widget widget) {
    }

    protected void removeWidgetReport(int userId, Widget widget) {
    }

    protected void updateWidgetOptionsReport(int userId, Widget widget) {
    }

    protected void clearWidgetReport() {
    }

    void clearWidgetsLocked() {
        this.mWidgets.clear();
        onWidgetsClearedLocked();
    }

    private void onWidgetsClearedLocked() {
        this.mWidgetPackages.clear();
        clearWidgetReport();
    }

    public boolean isBoundWidgetPackage(String packageName, int userId) {
        if (Binder.getCallingUid() == 1000) {
            synchronized (this.mLock) {
                ArraySet<String> packages = (ArraySet) this.mWidgetPackages.get(userId);
                if (packages != null) {
                    boolean contains = packages.contains(packageName);
                    return contains;
                }
                return false;
            }
        }
        throw new SecurityException("Only the system process can call this");
    }

    private void saveStateLocked(int userId) {
        tagProvidersAndHosts();
        for (int profileId : this.mSecurityPolicy.getEnabledGroupProfileIds(userId)) {
            AtomicFile file = getSavedStateFile(profileId);
            try {
                FileOutputStream stream = file.startWrite();
                if (writeProfileStateToFileLocked(stream, profileId)) {
                    file.finishWrite(stream);
                } else {
                    file.failWrite(stream);
                    Slog.w(TAG, "Failed to save state, restoring backup.");
                }
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed open state file for write: ");
                stringBuilder.append(e);
                Slog.w(str, stringBuilder.toString());
            }
        }
    }

    private void tagProvidersAndHosts() {
        int i;
        int providerCount = this.mProviders.size();
        int i2 = 0;
        for (i = 0; i < providerCount; i++) {
            ((Provider) this.mProviders.get(i)).tag = i;
        }
        i = this.mHosts.size();
        while (i2 < i) {
            ((Host) this.mHosts.get(i2)).tag = i2;
            i2++;
        }
    }

    private void clearProvidersAndHostsTagsLocked() {
        int i;
        int providerCount = this.mProviders.size();
        int i2 = 0;
        for (i = 0; i < providerCount; i++) {
            ((Provider) this.mProviders.get(i)).tag = -1;
        }
        i = this.mHosts.size();
        while (i2 < i) {
            ((Host) this.mHosts.get(i2)).tag = -1;
            i2++;
        }
    }

    private boolean writeProfileStateToFileLocked(FileOutputStream stream, int userId) {
        try {
            int i;
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(stream, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, "gs");
            out.attribute(null, "version", String.valueOf(1));
            int N = this.mProviders.size();
            for (i = 0; i < N; i++) {
                Provider provider = (Provider) this.mProviders.get(i);
                if (provider.getUserId() == userId && provider.shouldBePersisted()) {
                    serializeProvider(out, provider);
                }
            }
            N = this.mHosts.size();
            for (i = 0; i < N; i++) {
                Host host = (Host) this.mHosts.get(i);
                if (host.getUserId() == userId) {
                    serializeHost(out, host);
                }
            }
            N = this.mWidgets.size();
            for (i = 0; i < N; i++) {
                Widget widget = (Widget) this.mWidgets.get(i);
                if (widget.host.getUserId() == userId) {
                    serializeAppWidget(out, widget);
                }
            }
            Iterator<Pair<Integer, String>> it = this.mPackagesWithBindWidgetPermission.iterator();
            while (it.hasNext()) {
                Pair<Integer, String> binding = (Pair) it.next();
                if (((Integer) binding.first).intValue() == userId) {
                    out.startTag(null, "b");
                    out.attribute(null, "packageName", (String) binding.second);
                    out.endTag(null, "b");
                }
            }
            out.endTag(null, "gs");
            out.endDocument();
            return true;
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to write state: ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
            return false;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:102:0x026d A:{LOOP_END, LOOP:0: B:7:0x001a->B:102:0x026d} */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x026c A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0274 A:{Splitter: B:8:0x001b, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0274 A:{Splitter: B:8:0x001b, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0274 A:{Splitter: B:8:0x001b, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0274 A:{Splitter: B:8:0x001b, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x027a A:{Splitter: B:1:0x0006, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x027a A:{Splitter: B:1:0x0006, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x027a A:{Splitter: B:1:0x0006, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x027a A:{Splitter: B:1:0x0006, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0278 A:{Splitter: B:4:0x0013, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0278 A:{Splitter: B:4:0x0013, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0278 A:{Splitter: B:4:0x0013, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0278 A:{Splitter: B:4:0x0013, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0265 A:{Splitter: B:96:0x0261, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0265 A:{Splitter: B:96:0x0261, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0265 A:{Splitter: B:96:0x0261, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0265 A:{Splitter: B:96:0x0261, ExcHandler: java.lang.NullPointerException (e java.lang.NullPointerException)} */
    /* JADX WARNING: Missing block: B:98:0x0265, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:103:0x0274, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:104:0x0275, code:
            r1 = r30;
     */
    /* JADX WARNING: Missing block: B:105:0x0278, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:106:0x027a, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:107:0x027b, code:
            r6 = r28;
     */
    /* JADX WARNING: Missing block: B:108:0x027d, code:
            r1 = r30;
            r7 = r4;
     */
    /* JADX WARNING: Missing block: B:109:0x0280, code:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("failed parsing ");
            r3.append(r0);
            android.util.Slog.w(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:110:0x0297, code:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int readProfileStateFromFileLocked(FileInputStream stream, int userId, List<LoadedWidgetState> outLoadedWidgets) {
        AppWidgetServiceImpl appWidgetServiceImpl = this;
        int i = userId;
        int i2 = -1;
        try {
            XmlPullParser parser = Xml.newPullParser();
            try {
                parser.setInput(stream, StandardCharsets.UTF_8.name());
                int version = i2;
                i2 = -1;
                int legacyHostIndex = -1;
                while (true) {
                    int legacyHostIndex2 = legacyHostIndex;
                    try {
                        List<LoadedWidgetState> list;
                        int type = parser.next();
                        if (type == 2) {
                            String tag = parser.getName();
                            String pkg;
                            int providerTag;
                            String pkg2;
                            int uid;
                            String tagAttribute;
                            if ("gs".equals(tag)) {
                                try {
                                    legacyHostIndex = Integer.parseInt(parser.getAttributeValue(null, "version"));
                                } catch (NumberFormatException e) {
                                    NumberFormatException numberFormatException = e;
                                    legacyHostIndex = 0;
                                }
                                list = outLoadedWidgets;
                                version = legacyHostIndex;
                            } else if ("p".equals(tag)) {
                                i2++;
                                pkg = parser.getAttributeValue(null, AbsLocationManagerService.DEL_PKG);
                                String cl = parser.getAttributeValue(null, "cl");
                                pkg = appWidgetServiceImpl.getCanonicalPackageName(pkg, cl, i);
                                if (pkg != null) {
                                    int uid2 = appWidgetServiceImpl.getUidForPackage(pkg, i);
                                    if (uid2 >= 0) {
                                        ComponentName componentName = new ComponentName(pkg, cl);
                                        ActivityInfo providerInfo = appWidgetServiceImpl.getProviderInfo(componentName, i);
                                        if (providerInfo != null) {
                                            Provider provider;
                                            ProviderId providerId = new ProviderId(uid2, componentName, null);
                                            Provider provider2 = appWidgetServiceImpl.lookupProviderLocked(providerId);
                                            if (provider2 == null && appWidgetServiceImpl.mSafeMode) {
                                                provider = new Provider();
                                                provider.info = new AppWidgetProviderInfo();
                                                provider.info.provider = providerId.componentName;
                                                provider.info.providerInfo = providerInfo;
                                                provider.zombie = true;
                                                provider.id = providerId;
                                                appWidgetServiceImpl.mProviders.add(provider);
                                            } else {
                                                ComponentName componentName2 = componentName;
                                                provider = provider2;
                                            }
                                            pkg = parser.getAttributeValue(null, "tag");
                                            providerTag = !TextUtils.isEmpty(pkg) ? Integer.parseInt(pkg, 16) : i2;
                                            provider.tag = providerTag;
                                            provider.infoTag = parser.getAttributeValue(0, "info_tag");
                                            if (!(TextUtils.isEmpty(provider.infoTag) || appWidgetServiceImpl.mSafeMode)) {
                                                legacyHostIndex = appWidgetServiceImpl.parseAppWidgetProviderInfo(providerId, providerInfo, provider.infoTag);
                                                if (legacyHostIndex != 0) {
                                                    provider.info = legacyHostIndex;
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if ("h".equals(tag)) {
                                legacyHostIndex2++;
                                Host host = new Host();
                                pkg2 = parser.getAttributeValue(null, AbsLocationManagerService.DEL_PKG);
                                uid = appWidgetServiceImpl.getUidForPackage(pkg2, i);
                                if (uid < 0) {
                                    host.zombie = true;
                                }
                                if (!host.zombie || appWidgetServiceImpl.mSafeMode) {
                                    int hostId = Integer.parseInt(parser.getAttributeValue(null, "id"), 16);
                                    tagAttribute = parser.getAttributeValue(null, "tag");
                                    host.tag = !TextUtils.isEmpty(tagAttribute) ? Integer.parseInt(tagAttribute, 16) : legacyHostIndex2;
                                    host.id = new HostId(uid, hostId, pkg2);
                                    appWidgetServiceImpl.mHosts.add(host);
                                }
                            } else if ("b".equals(tag)) {
                                pkg = parser.getAttributeValue(null, "packageName");
                                if (appWidgetServiceImpl.getUidForPackage(pkg, i) >= 0) {
                                    appWidgetServiceImpl.mPackagesWithBindWidgetPermission.add(Pair.create(Integer.valueOf(userId), pkg));
                                }
                            } else if ("g".equals(tag)) {
                                Widget widget = new Widget();
                                widget.appWidgetId = Integer.parseInt(parser.getAttributeValue(null, "id"), 16);
                                appWidgetServiceImpl.setMinAppWidgetIdLocked(i, widget.appWidgetId + 1);
                                pkg2 = parser.getAttributeValue(null, "rid");
                                if (pkg2 == null) {
                                    uid = 0;
                                } else {
                                    uid = Integer.parseInt(pkg2, 16);
                                }
                                widget.restoredId = uid;
                                Bundle options = new Bundle();
                                String minWidthString = parser.getAttributeValue(null, "min_width");
                                if (minWidthString != null) {
                                    options.putInt("appWidgetMinWidth", Integer.parseInt(minWidthString, 16));
                                }
                                tagAttribute = parser.getAttributeValue(null, "min_height");
                                if (tagAttribute != null) {
                                    options.putInt("appWidgetMinHeight", Integer.parseInt(tagAttribute, 16));
                                }
                                String maxWidthString = parser.getAttributeValue(null, "max_width");
                                if (maxWidthString != null) {
                                    options.putInt("appWidgetMaxWidth", Integer.parseInt(maxWidthString, 16));
                                }
                                pkg2 = parser.getAttributeValue(null, "max_height");
                                if (pkg2 != null) {
                                    options.putInt("appWidgetMaxHeight", Integer.parseInt(pkg2, 16));
                                }
                                maxWidthString = parser.getAttributeValue(null, "host_category");
                                if (maxWidthString != null) {
                                    options.putInt("appWidgetCategory", Integer.parseInt(maxWidthString, 16));
                                }
                                widget.options = options;
                                providerTag = Integer.parseInt(parser.getAttributeValue(null, "h"), 16);
                                if (parser.getAttributeValue(null, "p") != null) {
                                    i = Integer.parseInt(parser.getAttributeValue(null, "p"), 16);
                                } else {
                                    i = -1;
                                }
                                try {
                                    outLoadedWidgets.add(new LoadedWidgetState(widget, providerTag, i));
                                } catch (NullPointerException e2) {
                                }
                            }
                            legacyHostIndex = legacyHostIndex2;
                            if (type != 1) {
                                return version;
                            }
                            appWidgetServiceImpl = this;
                            i = userId;
                        }
                        list = outLoadedWidgets;
                        legacyHostIndex = legacyHostIndex2;
                        if (type != 1) {
                        }
                    } catch (NullPointerException e3) {
                    }
                }
            } catch (NullPointerException e4) {
            }
        } catch (NullPointerException e5) {
        }
    }

    private void performUpgradeLocked(int fromVersion) {
        if (fromVersion < 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Upgrading widget database from ");
            stringBuilder.append(fromVersion);
            stringBuilder.append(" to ");
            stringBuilder.append(1);
            Slog.v(str, stringBuilder.toString());
        }
        int version = fromVersion;
        if (version == 0) {
            Host host = lookupHostLocked(new HostId(Process.myUid(), KEYGUARD_HOST_ID, "android"));
            if (host != null) {
                int uid = getUidForPackage(NEW_KEYGUARD_HOST_PACKAGE, 0);
                if (uid >= 0) {
                    host.id = new HostId(uid, KEYGUARD_HOST_ID, NEW_KEYGUARD_HOST_PACKAGE);
                }
            }
            version = 1;
        }
        if (version != 1) {
            throw new IllegalStateException("Failed to upgrade widget database");
        }
    }

    private static File getStateFile(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), STATE_FILENAME);
    }

    private static AtomicFile getSavedStateFile(int userId) {
        File dir = Environment.getUserSystemDirectory(userId);
        File settingsFile = getStateFile(userId);
        if (!settingsFile.exists() && userId == 0) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            new File("/data/system/appwidgets.xml").renameTo(settingsFile);
        }
        return new AtomicFile(settingsFile);
    }

    void onUserStopped(int userId) {
        synchronized (this.mLock) {
            int i;
            boolean crossProfileWidgetsChanged = false;
            int i2 = this.mWidgets.size() - 1;
            while (true) {
                boolean providerInUser = true;
                if (i2 < 0) {
                    break;
                }
                Widget widget = (Widget) this.mWidgets.get(i2);
                boolean hostInUser = widget.host.getUserId() == userId;
                boolean hasProvider = widget.provider != null;
                if (!(hasProvider && widget.provider.getUserId() == userId)) {
                    providerInUser = false;
                }
                if (hostInUser && (!hasProvider || providerInUser)) {
                    removeWidgetLocked(widget);
                    widget.host.widgets.remove(widget);
                    widget.host = null;
                    if (hasProvider) {
                        widget.provider.widgets.remove(widget);
                        widget.provider = null;
                    }
                }
                i2--;
            }
            for (i = this.mHosts.size() - 1; i >= 0; i--) {
                Host host = (Host) this.mHosts.get(i);
                if (host.getUserId() == userId) {
                    crossProfileWidgetsChanged |= host.widgets.isEmpty() ^ 1;
                    deleteHostLocked(host);
                }
            }
            for (i = this.mPackagesWithBindWidgetPermission.size() - 1; i >= 0; i--) {
                if (((Integer) ((Pair) this.mPackagesWithBindWidgetPermission.valueAt(i)).first).intValue() == userId) {
                    this.mPackagesWithBindWidgetPermission.removeAt(i);
                }
            }
            i = this.mLoadedUserIds.indexOfKey(userId);
            if (i >= 0) {
                this.mLoadedUserIds.removeAt(i);
            }
            int nextIdIndex = this.mNextAppWidgetIds.indexOfKey(userId);
            if (nextIdIndex >= 0) {
                this.mNextAppWidgetIds.removeAt(nextIdIndex);
            }
            if (crossProfileWidgetsChanged) {
                saveGroupStateAsync(userId);
            }
        }
    }

    private boolean updateProvidersForPackageLocked(String packageName, int userId, Set<ProviderId> removedProviders) {
        List<ResolveInfo> broadcastReceivers;
        int N;
        String str = packageName;
        int i = userId;
        Set<ProviderId> set = removedProviders;
        HashSet<ProviderId> keep = new HashSet();
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.setPackage(str);
        List<ResolveInfo> broadcastReceivers2 = queryIntentReceivers(intent, i);
        int N2 = broadcastReceivers2 == null ? 0 : broadcastReceivers2.size();
        boolean providersUpdated = false;
        int i2 = 0;
        while (i2 < N2) {
            Intent intent2;
            ResolveInfo ri = (ResolveInfo) broadcastReceivers2.get(i2);
            ActivityInfo ai = ri.activityInfo;
            if ((ai.applicationInfo.flags & 262144) != 0) {
                intent2 = intent;
            } else {
                if (str.equals(ai.packageName)) {
                    intent2 = intent;
                    ProviderId providerId = new ProviderId(ai.applicationInfo.uid, new ComponentName(ai.packageName, ai.name), null);
                    Provider provider = lookupProviderLocked(providerId);
                    if (provider != null) {
                        ProviderId providerId2;
                        Provider parsed = parseProviderInfoXml(providerId, ri, provider);
                        if (parsed != null) {
                            keep.add(providerId);
                            provider.info = parsed.info;
                            int M = provider.widgets.size();
                            if (M > 0) {
                                intent = getWidgetIds(provider.widgets);
                                cancelBroadcasts(provider);
                                registerForBroadcastsLocked(provider, intent);
                                int j = 0;
                                while (true) {
                                    broadcastReceivers = broadcastReceivers2;
                                    broadcastReceivers2 = j;
                                    if (broadcastReceivers2 >= M) {
                                        break;
                                    }
                                    providerId2 = providerId;
                                    Widget providerId3 = (Widget) provider.widgets.get(broadcastReceivers2);
                                    N = N2;
                                    providerId3.views = 0;
                                    scheduleNotifyProviderChangedLocked(providerId3);
                                    j = broadcastReceivers2 + 1;
                                    broadcastReceivers2 = broadcastReceivers;
                                    providerId = providerId2;
                                    N2 = N;
                                }
                                N = N2;
                                sendUpdateIntentLocked(provider, intent);
                                providersUpdated = true;
                            }
                        }
                        broadcastReceivers = broadcastReceivers2;
                        providerId2 = providerId;
                        N = N2;
                        providersUpdated = true;
                    } else if (addProviderLocked(ri) != null) {
                        keep.add(providerId);
                        providersUpdated = true;
                    }
                } else {
                    intent2 = intent;
                    broadcastReceivers = broadcastReceivers2;
                    N = N2;
                }
                i2++;
                intent = intent2;
                broadcastReceivers2 = broadcastReceivers;
                N2 = N;
            }
            broadcastReceivers = broadcastReceivers2;
            N = N2;
            i2++;
            intent = intent2;
            broadcastReceivers2 = broadcastReceivers;
            N2 = N;
        }
        broadcastReceivers = broadcastReceivers2;
        N = N2;
        for (int i3 = this.mProviders.size() - 1; i3 >= 0; i3--) {
            Provider provider2 = (Provider) this.mProviders.get(i3);
            if (str.equals(provider2.info.provider.getPackageName()) && provider2.getUserId() == i && !keep.contains(provider2.id)) {
                if (set != null) {
                    set.add(provider2.id);
                }
                deleteProviderLocked(provider2);
                providersUpdated = true;
            }
        }
        return providersUpdated;
    }

    private void removeWidgetsForPackageLocked(String pkgName, int userId, int parentUserId) {
        int N = this.mProviders.size();
        for (int i = 0; i < N; i++) {
            Provider provider = (Provider) this.mProviders.get(i);
            if (pkgName.equals(provider.info.provider.getPackageName()) && provider.getUserId() == userId && provider.widgets.size() > 0) {
                deleteWidgetsLocked(provider, parentUserId);
            }
        }
    }

    private boolean removeProvidersForPackageLocked(String pkgName, int userId) {
        boolean removed = false;
        for (int i = this.mProviders.size() - 1; i >= 0; i--) {
            Provider provider = (Provider) this.mProviders.get(i);
            if (pkgName.equals(provider.info.provider.getPackageName()) && provider.getUserId() == userId) {
                deleteProviderLocked(provider);
                removed = true;
            }
        }
        return removed;
    }

    private boolean removeHostsAndProvidersForPackageLocked(String pkgName, int userId) {
        boolean removed = removeProvidersForPackageLocked(pkgName, userId);
        for (int i = this.mHosts.size() - 1; i >= 0; i--) {
            Host host = (Host) this.mHosts.get(i);
            if (pkgName.equals(host.id.packageName) && host.getUserId() == userId) {
                deleteHostLocked(host);
                removed = true;
            }
        }
        return removed;
    }

    private String getCanonicalPackageName(String packageName, String className, int userId) {
        long identity = Binder.clearCallingIdentity();
        String str = 0;
        try {
            AppGlobals.getPackageManager().getReceiverInfo(new ComponentName(packageName, className), 0, userId);
            return packageName;
        } catch (RemoteException e) {
            String[] packageNames = this.mContext.getPackageManager().currentToCanonicalPackageNames(new String[]{packageName});
            if (packageNames == null || packageNames.length <= 0) {
                Binder.restoreCallingIdentity(identity);
                return null;
            }
            str = packageNames[0];
            return str;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void sendBroadcastAsUser(Intent intent, UserHandle userHandle) {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, userHandle);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void bindService(Intent intent, ServiceConnection connection, UserHandle userHandle) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.bindServiceAsUser(intent, connection, 33554433, userHandle);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void unbindService(ServiceConnection connection) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.unbindService(connection);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void onCrossProfileWidgetProvidersChanged(int userId, List<String> packages) {
        int parentId = this.mSecurityPolicy.getProfileParent(userId);
        if (parentId != userId) {
            synchronized (this.mLock) {
                int i;
                int i2;
                ArraySet<String> previousPackages = new ArraySet();
                int providerCount = this.mProviders.size();
                int i3 = 0;
                for (i = 0; i < providerCount; i++) {
                    Provider provider = (Provider) this.mProviders.get(i);
                    if (provider.getUserId() == userId) {
                        previousPackages.add(provider.id.componentName.getPackageName());
                    }
                }
                i = packages.size();
                boolean providersChanged = false;
                for (i2 = 0; i2 < i; i2++) {
                    String packageName = (String) packages.get(i2);
                    previousPackages.remove(packageName);
                    providersChanged |= updateProvidersForPackageLocked(packageName, userId, null);
                }
                i2 = previousPackages.size();
                while (i3 < i2) {
                    removeWidgetsForPackageLocked((String) previousPackages.valueAt(i3), userId, parentId);
                    i3++;
                }
                if (providersChanged || i2 > 0) {
                    saveGroupStateAsync(userId);
                    scheduleNotifyGroupHostsForProvidersChangedLocked(userId);
                }
            }
        }
    }

    private boolean isProfileWithLockedParent(int userId) {
        long token = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = this.mUserManager.getUserInfo(userId);
            if (userInfo != null && userInfo.isManagedProfile()) {
                UserInfo parentInfo = this.mUserManager.getProfileParent(userId);
                if (!(parentInfo == null || isUserRunningAndUnlocked(parentInfo.getUserHandle().getIdentifier()))) {
                    return true;
                }
            }
            Binder.restoreCallingIdentity(token);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isProfileWithUnlockedParent(int userId) {
        UserInfo userInfo = this.mUserManager.getUserInfo(userId);
        if (userInfo != null && userInfo.isManagedProfile()) {
            UserInfo parentInfo = this.mUserManager.getProfileParent(userId);
            if (parentInfo != null && this.mUserManager.isUserUnlockingOrUnlocked(parentInfo.getUserHandle())) {
                return true;
            }
        }
        return false;
    }

    public IBinder getHwInnerService() {
        return this.mHwInnerService;
    }
}
