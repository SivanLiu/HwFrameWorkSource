package android.rms.resource;

import android.app.mtm.MultiTaskPolicy;
import android.database.IContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.rms.HwSysSpeedRes;
import android.rms.config.ResourceConfig;
import android.rms.control.ResourceFlowControl;
import android.rms.utils.Utils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public final class AppResource extends HwSysSpeedRes {
    private static final String TAG = "RMS.AppResource";
    private static final int TYPE_APP_PERMIT = 5;
    private static final int TYPE_CLEAR_DATA = 1;
    private static final int TYPE_DISABLE_APP = 2;
    private static final int TYPE_NOTIFY_CRASHINFO = 3;
    private static final int TYPE_NOTIFY_CRASHINFO_SYSAPP = 4;
    private static AppResource mAppResource;
    private HashSet<String> mAppLaunchedInfo = new HashSet();
    private HashMap<String, Integer> mAppResourceDoPolicyConfigs;
    private long mLifeTime;

    private AppResource() {
        super(18, TAG);
        getConfig();
        this.mAppResourceDoPolicyConfigs = new HashMap();
        ArrayList<String> whitelist = getResWhiteList(null);
        if (whitelist != null && whitelist.size() > 0) {
            initAppResourceDoPolicyConfigs(whitelist);
        }
    }

    public static synchronized AppResource getInstance() {
        AppResource appResource;
        synchronized (AppResource.class) {
            if (mAppResource == null) {
                mAppResource = new AppResource();
                if (Utils.DEBUG) {
                    Log.d(TAG, "getInstance create new AppResource");
                }
            }
            appResource = mAppResource;
        }
        return appResource;
    }

    protected void onWhiteListUpdate() {
        ArrayList<String> whitelist = getResWhiteList(null);
        if (whitelist != null && whitelist.size() > 0) {
            this.mAppResourceDoPolicyConfigs.clear();
            initAppResourceDoPolicyConfigs(whitelist);
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0072, code skipped:
            if (android.rms.utils.Utils.DEBUG == false) goto L_0x00b9;
     */
    /* JADX WARNING: Missing block: B:17:0x0074, code skipped:
            r0 = TAG;
            r15 = new java.lang.StringBuilder();
            r15.append("acquire mLifeTime:");
            r17 = r5;
            r18 = r6;
            r15.append(r1.mLifeTime);
            r15.append(" pkg ");
            r15.append(r4);
            r15.append(" blaunched ");
            r15.append(r14);
            r15.append(" launchfromActivity ");
            r15.append(r8);
            r15.append(" inteval ");
            r15.append(r11);
            r15.append(" top ");
            r15.append(r9);
            android.util.Log.d(r0, r15.toString());
     */
    /* JADX WARNING: Missing block: B:18:0x00b9, code skipped:
            r17 = r5;
            r18 = r6;
     */
    /* JADX WARNING: Missing block: B:19:0x00bd, code skipped:
            if (r7 != 0) goto L_0x011a;
     */
    /* JADX WARNING: Missing block: B:20:0x00bf, code skipped:
            if (r8 == false) goto L_0x011a;
     */
    /* JADX WARNING: Missing block: B:21:0x00c1, code skipped:
            r19 = r7;
            r20 = r8;
     */
    /* JADX WARNING: Missing block: B:22:0x00ca, code skipped:
            if (r1.mLifeTime <= ((long) r12)) goto L_0x00dc;
     */
    /* JADX WARNING: Missing block: B:24:0x00d1, code skipped:
            if (r1.mLifeTime >= ((long) r11)) goto L_0x011e;
     */
    /* JADX WARNING: Missing block: B:26:0x00d8, code skipped:
            if (r1.mLifeTime <= ((long) r12)) goto L_0x011e;
     */
    /* JADX WARNING: Missing block: B:27:0x00da, code skipped:
            if (r9 == false) goto L_0x011e;
     */
    /* JADX WARNING: Missing block: B:28:0x00dc, code skipped:
            if (r14 == false) goto L_0x011e;
     */
    /* JADX WARNING: Missing block: B:29:0x00de, code skipped:
            r0 = (java.lang.Integer) r1.mAppResourceDoPolicyConfigs.get(r4);
     */
    /* JADX WARNING: Missing block: B:30:0x00e6, code skipped:
            if (r0 == null) goto L_0x00f0;
     */
    /* JADX WARNING: Missing block: B:32:0x00ed, code skipped:
            if (r0.intValue() != 5) goto L_0x00f0;
     */
    /* JADX WARNING: Missing block: B:33:0x00ef, code skipped:
            return 1;
     */
    /* JADX WARNING: Missing block: B:34:0x00f0, code skipped:
            r13 = java.lang.Integer.valueOf(3);
     */
    /* JADX WARNING: Missing block: B:35:0x00f7, code skipped:
            if (android.rms.utils.Utils.DEBUG == false) goto L_0x0117;
     */
    /* JADX WARNING: Missing block: B:36:0x00f9, code skipped:
            r0 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("third app ");
            r5.append(r4);
            r5.append(", doPolicyType:");
            r5.append(r13);
            android.util.Log.d(r0, r5.toString());
     */
    /* JADX WARNING: Missing block: B:37:0x0117, code skipped:
            r5 = r19;
     */
    /* JADX WARNING: Missing block: B:38:0x011a, code skipped:
            r19 = r7;
            r20 = r8;
     */
    /* JADX WARNING: Missing block: B:39:0x011e, code skipped:
            r5 = r19;
     */
    /* JADX WARNING: Missing block: B:40:0x0121, code skipped:
            if (r5 != 2) goto L_0x0155;
     */
    /* JADX WARNING: Missing block: B:41:0x0123, code skipped:
            r0 = (java.lang.Integer) r1.mAppResourceDoPolicyConfigs.get(r4);
     */
    /* JADX WARNING: Missing block: B:42:0x012b, code skipped:
            if (r0 != null) goto L_0x0132;
     */
    /* JADX WARNING: Missing block: B:43:0x012d, code skipped:
            r0 = java.lang.Integer.valueOf(4);
     */
    /* JADX WARNING: Missing block: B:44:0x0132, code skipped:
            r13 = r0;
     */
    /* JADX WARNING: Missing block: B:45:0x0135, code skipped:
            if (android.rms.utils.Utils.DEBUG == false) goto L_0x0155;
     */
    /* JADX WARNING: Missing block: B:46:0x0137, code skipped:
            r0 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("system app ");
            r6.append(r4);
            r6.append(", doPolicyType:");
            r6.append(r13);
            android.util.Log.d(r0, r6.toString());
     */
    /* JADX WARNING: Missing block: B:47:0x0155, code skipped:
            if (r13 == null) goto L_0x018a;
     */
    /* JADX WARNING: Missing block: B:48:0x0157, code skipped:
            r6 = r17;
     */
    /* JADX WARNING: Missing block: B:49:0x015d, code skipped:
            if (isResourceSpeedOverload(r6, r4, r5) == false) goto L_0x018c;
     */
    /* JADX WARNING: Missing block: B:50:0x015f, code skipped:
            r0 = new android.os.Bundle();
            r0.putInt("callingUid", r6);
            r0.putString("pkg", r4);
            r7 = new android.app.mtm.MultiTaskPolicy(r13.intValue(), r0);
     */
    /* JADX WARNING: Missing block: B:51:0x017c, code skipped:
            if (r13.intValue() == 3) goto L_0x0185;
     */
    /* JADX WARNING: Missing block: B:52:0x017e, code skipped:
            r1.mResourceManger.dispatch(18, r7);
     */
    /* JADX WARNING: Missing block: B:53:0x0185, code skipped:
            r3 = getSpeedOverloadStrategy(r5);
     */
    /* JADX WARNING: Missing block: B:54:0x018a, code skipped:
            r6 = r17;
     */
    /* JADX WARNING: Missing block: B:55:0x018c, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int acquire(Uri uri, IContentObserver observer, Bundle args) {
        Throwable th;
        Long l;
        boolean z;
        Bundle bundle = args;
        int strategy = 1;
        if (this.mResourceConfig == null) {
            return 1;
        }
        String pkg = bundle.getString("pkg");
        int callingUid = bundle.getInt("callingUid");
        Long startTime = Long.valueOf(bundle.getLong("startTime"));
        int typeID = bundle.getInt("processType");
        boolean launchfromActivity = bundle.getBoolean("launchfromActivity");
        boolean isTopProcess = bundle.getBoolean("topProcess");
        int crachTimeInterval = this.mResourceConfig[3].getLoopInterval();
        int shortTime = this.mResourceConfig[3].getResourceStrategy();
        this.mLifeTime = SystemClock.elapsedRealtime() - startTime.longValue();
        Integer doPolicyType = null;
        boolean blaunched = false;
        synchronized (this.mAppLaunchedInfo) {
            if (pkg != null) {
                try {
                    blaunched = this.mAppLaunchedInfo.contains(pkg);
                    if (blaunched && !launchfromActivity) {
                        this.mAppLaunchedInfo.remove(pkg);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    l = startTime;
                    z = launchfromActivity;
                    startTime = callingUid;
                    callingUid = typeID;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                    throw th;
                }
            }
            try {
            } catch (Throwable th4) {
                th = th4;
                l = startTime;
                z = launchfromActivity;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    public int acquire(int callingUid, String pkg, int processType) {
        int strategy = 1;
        if (this.mResourceConfig == null) {
            return 1;
        }
        if (processType == 0) {
            Integer doPolicyType = (Integer) this.mAppResourceDoPolicyConfigs.get(pkg);
            if (doPolicyType != null && doPolicyType.intValue() == 5) {
                return 1;
            }
            doPolicyType = Integer.valueOf(3);
            if (Utils.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("third app ");
                stringBuilder.append(pkg);
                stringBuilder.append(", doPolicyType:");
                stringBuilder.append(doPolicyType);
                Log.d(str, stringBuilder.toString());
            }
            if (isResourceSpeedOverload(callingUid, pkg, processType)) {
                Bundle data = new Bundle();
                data.putInt("callingUid", callingUid);
                data.putString("pkg", pkg);
                this.mResourceManger.dispatch(18, new MultiTaskPolicy(doPolicyType.intValue(), data));
                strategy = getSpeedOverloadStrategy(processType);
                this.mResourceFlowControl.removeResourceSpeedRecord(super.getResourceId(callingUid, pkg, processType));
            }
        }
        return strategy;
    }

    public void clear(int callingUid, String pkg, int processTpye) {
        this.mResourceFlowControl.removeResourceSpeedRecord(super.getResourceId(callingUid, pkg, processTpye));
    }

    protected int getSpeedOverloadStrategy(int typeID) {
        return this.mResourceConfig[typeID].getResourceStrategy();
    }

    protected Bundle createBundleForResource(long id, int typeID, ResourceConfig config, ResourceFlowControl resourceCountControl) {
        if (typeID == 0) {
            this.mOverloadNumber = resourceCountControl.getCountInPeroid(id);
        }
        Bundle bundle = new Bundle();
        bundle.putLong(Utils.BUNDLE_THIRD_PARTY_APP_LIFETIME, this.mLifeTime);
        return bundle;
    }

    private void initAppResourceDoPolicyConfigs(ArrayList<String> whiteList) {
        int whiteListCount = whiteList.size();
        String[] list = null;
        for (int i = 0; i < whiteListCount; i++) {
            list = ((String) whiteList.get(i)).split(":");
            if (list.length == 2) {
                Integer policy = Integer.valueOf(Integer.parseInt(list[1]));
                if (((Integer) this.mAppResourceDoPolicyConfigs.get(list[0])) != null) {
                    if (Utils.DEBUG) {
                        Log.d(TAG, " PolicyConfigs is already!");
                    }
                    this.mAppResourceDoPolicyConfigs.remove(list[0]);
                }
                this.mAppResourceDoPolicyConfigs.put(list[0], policy);
            }
        }
    }

    public int queryPkgPolicy(int type, int value, String key) {
        if (key == null) {
            return 0;
        }
        Integer policy = (Integer) this.mAppResourceDoPolicyConfigs.get(key);
        if (policy == null) {
            return 0;
        }
        return policy.intValue();
    }
}
