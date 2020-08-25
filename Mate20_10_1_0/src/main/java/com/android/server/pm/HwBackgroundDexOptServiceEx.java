package com.android.server.pm;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class HwBackgroundDexOptServiceEx implements IHwBackgroundDexOptServiceEx {
    private static final String ATTR_NAME = "name";
    private static final int BATTERY_FULL_LEVEL = 100;
    private static final int BATTERY_PROPERTY_CAPACITY = 95;
    private static final String CUST_FILE_PATH = "/xml/hw_aot_compile_apps_config.xml";
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final int DEFAULT_EXTER_INT = -1;
    private static final int DELAYOPT_PERIOD = 60000;
    private static final int JOB_POST_BOOT_UPDATE_DELAYOPT = 8001;
    private static final int MSG_PREPARE_DATA = 1;
    static final String TAG = "HwBackgroundDexOptServiceEx";
    private static final String TAG_NAME = "speed";
    private final AtomicBoolean isBroadcastReceiverDexopt = new AtomicBoolean(false);
    private final BroadcastReceiverDexopt mBroadcastReceiverDexopt = new BroadcastReceiverDexopt();
    final Context mContext;
    /* access modifiers changed from: private */
    public DexoptHandler mDexoptHandler = null;
    IHwBackgroundDexOptInner mIbgDexOptInner = null;
    /* access modifiers changed from: private */
    public final SparseArray<JobParameters> mJobParamsMap = new SparseArray<>();
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    final JobService mService;
    private ArraySet<String> mSpeedNodePkgs = null;

    public HwBackgroundDexOptServiceEx(IHwBackgroundDexOptInner bdos, JobService service, Context context) {
        this.mIbgDexOptInner = bdos;
        this.mService = service;
        this.mContext = context;
    }

    public int getReason(int reason, int reasonBackgroudDexopt, int reasonSpeedDexopt, String pkg) {
        if (this.mSpeedNodePkgs == null) {
            this.mSpeedNodePkgs = getAllNeedForSpeedApps();
        }
        ArraySet<String> arraySet = this.mSpeedNodePkgs;
        if (arraySet == null || !arraySet.contains(pkg)) {
            return reasonBackgroudDexopt;
        }
        return reasonSpeedDexopt;
    }

    private ArraySet<String> getAllNeedForSpeedApps() {
        try {
            File file = HwCfgFilePolicy.getCfgFile(CUST_FILE_PATH, 0);
            if (file != null) {
                return readSpeedAppsFromXml(file);
            }
            Log.i(TAG, "hw_aot_compile_apps_config not exist");
            return null;
        } catch (NoClassDefFoundError e) {
            Log.i(TAG, "get speed apps failed");
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0037 A[Catch:{ XmlPullParserException -> 0x00c6, IOException -> 0x00b2, all -> 0x00b0 }] */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0047  */
    private ArraySet<String> readSpeedAppsFromXml(File config) {
        int type;
        FileInputStream stream = null;
        if (!config.exists() || !config.canRead()) {
            return null;
        }
        try {
            FileInputStream stream2 = new FileInputStream(config);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream2, StandardCharsets.UTF_8.name());
            while (true) {
                type = parser.next();
                if (type == 2 || type == 1) {
                    if (type == 2) {
                        Log.w(TAG, "Failed parsing config, can't find start tag");
                        try {
                            stream2.close();
                        } catch (IOException e) {
                            Log.w(TAG, "readSpeedAppsFromXml stream.close catch IOException");
                        }
                        return null;
                    }
                    ArraySet<String> speedApps = new ArraySet<>();
                    int outerDepth = parser.getDepth();
                    while (true) {
                        int type2 = parser.next();
                        if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                            try {
                                stream2.close();
                            } catch (IOException e2) {
                                Log.w(TAG, "readSpeedAppsFromXml stream.close catch IOException");
                            }
                            return speedApps;
                        } else if (!(type2 == 3 || type2 == 4)) {
                            if (TAG_NAME.equals(parser.getName())) {
                                String name = parser.getAttributeValue(null, "name");
                                if (!TextUtils.isEmpty(name)) {
                                    speedApps.add(name.intern());
                                }
                            } else {
                                Log.w(TAG, "Unknown element under <config>: " + parser.getName());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                }
            }
            if (type == 2) {
            }
        } catch (XmlPullParserException e3) {
            Log.w(TAG, "Failed parsing config XmlPullParserException");
            if (0 != 0) {
                try {
                    stream.close();
                } catch (IOException e4) {
                    Log.w(TAG, "readSpeedAppsFromXml stream.close catch IOException");
                }
            }
            return null;
        } catch (IOException e5) {
            Log.w(TAG, "Failed parsing config IOException");
            if (0 != 0) {
                try {
                    stream.close();
                } catch (IOException e6) {
                    Log.w(TAG, "readSpeedAppsFromXml stream.close catch IOException");
                }
            }
            return null;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    stream.close();
                } catch (IOException e7) {
                    Log.w(TAG, "readSpeedAppsFromXml stream.close catch IOException");
                }
            }
            throw th;
        }
    }

    private int getBatteryLevel() {
        if (this.mContext == null) {
            return 0;
        }
        Intent intent = this.mContext.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        int level = intent.getIntExtra(MemoryConstant.MEM_FILECACHE_ITEM_LEVEL, -1);
        int scale = intent.getIntExtra("scale", -1);
        if (!intent.getBooleanExtra("present", true)) {
            return 100;
        }
        if (level < 0 || scale <= 0) {
            return 0;
        }
        return (level * 100) / scale;
    }

    public boolean runBootUpdateDelayOpt(JobParameters params) {
        if (params == null || params.getJobId() != JOB_POST_BOOT_UPDATE_DELAYOPT) {
            return false;
        }
        this.mJobParamsMap.put(JOB_POST_BOOT_UPDATE_DELAYOPT, params);
        this.mDexoptHandler = new DexoptHandler();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF);
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        registerReceiverDexopt(intentFilter);
        synchronized (this.mLock) {
            if (this.mDexoptHandler != null) {
                Message msg = Message.obtain();
                msg.what = 1;
                msg.obj = params;
                this.mDexoptHandler.sendMessageDelayed(msg, AppHibernateCst.DELAY_ONE_MINS);
                if (DEBUG) {
                    Log.i(TAG, "Job DELAYOPT start!");
                }
            }
        }
        return true;
    }

    public boolean stopBootUpdateDelayOpt(JobParameters params) {
        if (params == null || params.getJobId() != JOB_POST_BOOT_UPDATE_DELAYOPT) {
            return false;
        }
        unregisterReceiverDexopt();
        this.mJobParamsMap.remove(JOB_POST_BOOT_UPDATE_DELAYOPT);
        DexoptHandler dexoptHandler = this.mDexoptHandler;
        if (dexoptHandler != null && dexoptHandler.hasMessages(1)) {
            this.mDexoptHandler.removeMessages(1);
        }
        this.mDexoptHandler = null;
        return true;
    }

    /* access modifiers changed from: private */
    public void handlerPrepareData(JobParameters params) {
        if (params.getJobId() == JOB_POST_BOOT_UPDATE_DELAYOPT && this.mIbgDexOptInner != null && this.mContext != null && this.mService != null) {
            int batteryLevel = getBatteryLevel();
            if (!((PowerManager) this.mContext.getSystemService("power")).isScreenOn() && batteryLevel >= 95) {
                PackageManagerService pm = ServiceManager.getService("package");
                if (pm.isStorageLow()) {
                    this.mService.jobFinished(params, false);
                    if (DEBUG) {
                        Log.i(TAG, "Low storage, skipping this run");
                        return;
                    }
                    return;
                }
                ArraySet<String> pkgs = pm.getOptimizablePackages();
                if (pkgs.isEmpty()) {
                    this.mService.jobFinished(params, false);
                    if (DEBUG) {
                        Log.i(TAG, "No packages to optimize");
                        return;
                    }
                    return;
                }
                if (DEBUG) {
                    Log.i(TAG, "Start update!");
                }
                if (!this.mIbgDexOptInner.runPostBootUpdateEx(params, pm, pkgs)) {
                    this.mService.jobFinished(params, false);
                    if (DEBUG) {
                        Log.i(TAG, "Superseded by IDLE and Job DELAYOPT stop!");
                    }
                }
                stopBootUpdateDelayOpt(params);
            } else if (DEBUG) {
                Log.i(TAG, "ScreenOn or BatteryLevel low, and Job DELAYOPT stop! BatteryLevel =" + batteryLevel);
            }
        }
    }

    /* access modifiers changed from: private */
    public class DexoptHandler extends Handler {
        private DexoptHandler() {
        }

        public void handleMessage(Message msg) {
            if (msg == null) {
                Log.w(HwBackgroundDexOptServiceEx.TAG, "msg is null, error!");
            } else if (msg.what != 1) {
                Log.w(HwBackgroundDexOptServiceEx.TAG, "DexoptHandler, default branch, msg.what is " + msg.what);
            } else if (msg.obj instanceof JobParameters) {
                HwBackgroundDexOptServiceEx.this.handlerPrepareData((JobParameters) msg.obj);
            }
        }
    }

    private class BroadcastReceiverDexopt extends BroadcastReceiver {
        private BroadcastReceiverDexopt() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                String action = intent.getAction();
                char c = 65535;
                int hashCode = action.hashCode();
                if (hashCode != -2128145023) {
                    if (hashCode == -1538406691 && action.equals("android.intent.action.BATTERY_CHANGED")) {
                        c = 1;
                    }
                } else if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF)) {
                    c = 0;
                }
                if (c == 0) {
                    synchronized (HwBackgroundDexOptServiceEx.this.mLock) {
                        if (HwBackgroundDexOptServiceEx.this.mDexoptHandler != null && !HwBackgroundDexOptServiceEx.this.mDexoptHandler.hasMessages(1)) {
                            Message msg = Message.obtain();
                            msg.what = 1;
                            msg.obj = HwBackgroundDexOptServiceEx.this.mJobParamsMap.get(HwBackgroundDexOptServiceEx.JOB_POST_BOOT_UPDATE_DELAYOPT);
                            HwBackgroundDexOptServiceEx.this.mDexoptHandler.sendMessageDelayed(msg, AppHibernateCst.DELAY_ONE_MINS);
                            if (HwBackgroundDexOptServiceEx.DEBUG) {
                                Log.i(HwBackgroundDexOptServiceEx.TAG, "Job DELAYOPT start when ACTION_SCREEN_OFF!");
                            }
                        }
                    }
                } else if (c == 1) {
                    synchronized (HwBackgroundDexOptServiceEx.this.mLock) {
                        if (HwBackgroundDexOptServiceEx.this.mDexoptHandler != null && !HwBackgroundDexOptServiceEx.this.mDexoptHandler.hasMessages(1)) {
                            Message msg2 = Message.obtain();
                            msg2.what = 1;
                            msg2.obj = HwBackgroundDexOptServiceEx.this.mJobParamsMap.get(HwBackgroundDexOptServiceEx.JOB_POST_BOOT_UPDATE_DELAYOPT);
                            HwBackgroundDexOptServiceEx.this.mDexoptHandler.sendMessageDelayed(msg2, AppHibernateCst.DELAY_ONE_MINS);
                            if (HwBackgroundDexOptServiceEx.DEBUG) {
                                Log.i(HwBackgroundDexOptServiceEx.TAG, "Job DELAYOPT start when ACTION_BATTERY_CHANGED!");
                            }
                        }
                    }
                }
            }
        }
    }

    private synchronized void registerReceiverDexopt(IntentFilter intentFilter) {
        if (!this.isBroadcastReceiverDexopt.get() && this.mContext != null) {
            this.mContext.registerReceiver(this.mBroadcastReceiverDexopt, intentFilter);
            this.isBroadcastReceiverDexopt.set(true);
        }
    }

    private synchronized void unregisterReceiverDexopt() {
        if (this.isBroadcastReceiverDexopt.get() && this.mContext != null) {
            this.mContext.unregisterReceiver(this.mBroadcastReceiverDexopt);
            this.isBroadcastReceiverDexopt.set(false);
        }
    }
}
