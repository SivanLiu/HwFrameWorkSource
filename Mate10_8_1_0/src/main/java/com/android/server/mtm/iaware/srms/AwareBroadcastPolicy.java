package com.android.server.mtm.iaware.srms;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.am.HwBroadcastRecord;
import com.android.server.pfw.autostartup.comm.XmlConst.PreciseIgnore;
import com.android.server.pm.PackageManagerService;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup.IAwareStateCallback;
import com.android.server.rms.iaware.appmng.AwareSceneRecognize;
import com.android.server.rms.iaware.appmng.AwareSceneRecognize.IAwareSceneRecCallback;
import com.android.server.rms.iaware.srms.BroadcastFeature;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class AwareBroadcastPolicy {
    private static final int BROADCAST_PROXY_SPEED_INDEX = 2;
    private static final int BROADCAST_PROXY_SPEED_INTERVAL = 100;
    private static final long BROADCAST_PROXY_SPEED_INTERVAL_LONG = 100;
    private static final int BROADCAST_PROXY_SPEED_NUMBER = 10;
    private static final int BROADCAST_PROXY_SPEED_NUMBER_INDEX_MAX = 9;
    private static final int IAWARE_APP_INSTALL_DELAY_TIME = 5000;
    private static final int IAWARE_DWONLOAD_DELAY_TIME = 3000;
    private static final int IAWARE_START_ACTIVITY_DELAY_TIME = 3000;
    private static final int LOOPER_CHECK_TIME = 1000;
    private static final int LOOPER_CHECK_TIME_RECOUNT = 2000;
    private static final int MSG_INSTALL_APP_TIMEOUT = 208;
    private static final int MSG_POLICY_DL_END = 202;
    private static final int MSG_POLICY_DL_START = 201;
    private static final int MSG_POLICY_END_CHECK = 206;
    private static final int MSG_POLICY_SCENE_ACTIVITY = 205;
    private static final int MSG_POLICY_SCENE_SLIP = 203;
    private static final int MSG_START_ACTIVITY_TIMEOUT = 207;
    private static final String TAG = "AwareBroadcastPolicy";
    private AwareBroadcastConfig mAwareBroadcastConfig;
    private AwareSceneStateCallback mAwareSceneStateCallback;
    private AwareStateCallback mAwareStateCallback;
    private AwareBroadcastProcess mBgIawareBr = null;
    private long mCountCheck = 0;
    private AwareBroadcastProcess mFgIawareBr = null;
    private int mForegroundAppLevel = 2;
    private final IawareBroadcastPolicyHandler mHandler;
    private ArraySet<Integer> mIawareDownloadingUid = new ArraySet();
    private boolean mIawareInstallApp = false;
    private ArraySet<String> mIawareNoProxyActions = new ArraySet();
    private ArraySet<String> mIawareNoProxyPkgs = new ArraySet();
    private boolean mIawareProxyActivitStart = false;
    private boolean mIawareProxySlip = false;
    private boolean mIawareScreenOn = true;
    private ArraySet<String> mIawareTrimActions = new ArraySet();
    private ArrayMap<String, ArraySet<String>> mIawareUnProxySys = new ArrayMap();
    private long mLastParallelBrTime = 0;
    private Object mLockNoProxyActions = new Object();
    private Object mLockNoProxyPkgs = new Object();
    private Object mLockTrimActions = new Object();
    private Object mLockUnProxySys = new Object();
    private int mNoTouchCheckCount = 200;
    private final long[][] mProxyCount = ((long[][]) Array.newInstance(Long.TYPE, new int[]{10, 2}));
    private boolean mSpeedParallelStartProxy = false;
    private long mStartParallelBrTime = 0;
    private int mTouchCheckCount = 60;

    private class AwareSceneStateCallback implements IAwareSceneRecCallback {
        private AwareSceneStateCallback() {
        }

        public void onStateChanged(int sceneType, int eventType, String pkg) {
            if (BroadcastFeature.isFeatureEnabled(10)) {
                Message msg;
                if (sceneType == 2) {
                    msg = AwareBroadcastPolicy.this.mHandler.obtainMessage();
                    msg.what = AwareBroadcastPolicy.MSG_POLICY_SCENE_SLIP;
                    msg.arg1 = eventType;
                    AwareBroadcastPolicy.this.mHandler.sendMessage(msg);
                } else if (sceneType == 4) {
                    msg = AwareBroadcastPolicy.this.mHandler.obtainMessage();
                    msg.what = AwareBroadcastPolicy.MSG_POLICY_SCENE_ACTIVITY;
                    msg.arg1 = eventType;
                    AwareBroadcastPolicy.this.mHandler.sendMessage(msg);
                    if (AwareBroadcastPolicy.this.mHandler.hasMessages(AwareBroadcastPolicy.MSG_START_ACTIVITY_TIMEOUT)) {
                        AwareBroadcastPolicy.this.mHandler.removeMessages(AwareBroadcastPolicy.MSG_START_ACTIVITY_TIMEOUT);
                    }
                    if (eventType == 1) {
                        AwareBroadcastPolicy.this.mHandler.sendEmptyMessageDelayed(AwareBroadcastPolicy.MSG_START_ACTIVITY_TIMEOUT, 3000);
                    }
                } else if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.d(AwareBroadcastPolicy.TAG, "don't process scene type " + sceneType);
                }
            }
        }
    }

    private class AwareStateCallback implements IAwareStateCallback {
        private AwareStateCallback() {
        }

        public void onStateChanged(int stateType, int eventType, int pid, int uid) {
            if (BroadcastFeature.isFeatureEnabled(10) && stateType == 5 && uid >= 0) {
                Message msg;
                if (eventType == 1) {
                    msg = AwareBroadcastPolicy.this.mHandler.obtainMessage();
                    msg.what = AwareBroadcastPolicy.MSG_POLICY_DL_START;
                    msg.arg1 = uid;
                    AwareBroadcastPolicy.this.mHandler.sendMessage(msg);
                } else if (eventType == 2) {
                    msg = AwareBroadcastPolicy.this.mHandler.obtainMessage();
                    msg.what = AwareBroadcastPolicy.MSG_POLICY_DL_END;
                    msg.arg1 = uid;
                    AwareBroadcastPolicy.this.mHandler.sendMessageDelayed(msg, 3000);
                } else if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.d(AwareBroadcastPolicy.TAG, "don't process type " + eventType);
                }
            }
        }
    }

    private final class IawareBroadcastPolicyHandler extends Handler {
        public IawareBroadcastPolicyHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            ArraySet -get1;
            switch (msg.what) {
                case AwareBroadcastPolicy.MSG_POLICY_DL_START /*201*/:
                    -get1 = AwareBroadcastPolicy.this.mIawareDownloadingUid;
                    synchronized (-get1) {
                        AwareBroadcastPolicy.this.mIawareDownloadingUid.add(Integer.valueOf(msg.arg1));
                        break;
                    }
                case AwareBroadcastPolicy.MSG_POLICY_DL_END /*202*/:
                    -get1 = AwareBroadcastPolicy.this.mIawareDownloadingUid;
                    synchronized (-get1) {
                        AwareBroadcastPolicy.this.mIawareDownloadingUid.remove(Integer.valueOf(msg.arg1));
                        break;
                    }
                case AwareBroadcastPolicy.MSG_POLICY_SCENE_SLIP /*203*/:
                    AwareBroadcastPolicy.this.setIawarePolicy(2, msg.arg1);
                    return;
                case AwareBroadcastPolicy.MSG_POLICY_SCENE_ACTIVITY /*205*/:
                    AwareBroadcastPolicy.this.setIawarePolicy(4, msg.arg1);
                    return;
                case AwareBroadcastPolicy.MSG_POLICY_END_CHECK /*206*/:
                    AwareBroadcastPolicy.this.startUnproxyBroadcast();
                    return;
                case AwareBroadcastPolicy.MSG_START_ACTIVITY_TIMEOUT /*207*/:
                    AwareBroadcastPolicy.this.mIawareProxyActivitStart = false;
                    return;
                case AwareBroadcastPolicy.MSG_INSTALL_APP_TIMEOUT /*208*/:
                    AwareBroadcastPolicy.this.mIawareInstallApp = false;
                    return;
                default:
                    return;
            }
        }
    }

    public AwareBroadcastPolicy(Handler handler) {
        this.mBgIawareBr = new AwareBroadcastProcess(this, handler, "iawarebackground");
        this.mFgIawareBr = new AwareBroadcastProcess(this, handler, "iawareforeground");
        this.mHandler = new IawareBroadcastPolicyHandler(handler.getLooper());
        this.mAwareBroadcastConfig = AwareBroadcastConfig.getInstance();
    }

    public void init() {
        this.mAwareStateCallback = new AwareStateCallback();
        AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 5);
        this.mAwareSceneStateCallback = new AwareSceneStateCallback();
        AwareSceneRecognize.getInstance().registerStateCallback(this.mAwareSceneStateCallback, 1);
        this.mAwareBroadcastConfig.doinit();
    }

    public boolean enqueueIawareProxyBroacast(boolean isParallel, HwBroadcastRecord r) {
        if (r == null) {
            return false;
        }
        if (r.isBg()) {
            this.mBgIawareBr.enqueueIawareProxyBroacast(isParallel, r);
        } else {
            this.mFgIawareBr.enqueueIawareProxyBroacast(isParallel, r);
        }
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean shouldIawareProxyBroadcast(String brAction, int callingPid, int receiverUid, int receiverPid, String recevierPkg) {
        synchronized (this.mIawareDownloadingUid) {
            if (this.mIawareDownloadingUid.contains(Integer.valueOf(receiverUid))) {
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.d(TAG, "iaware_br : downloading, don't proxy : " + recevierPkg + ": action : " + brAction);
                }
            }
        }
    }

    private boolean isIawarePrepared() {
        if (this.mFgIawareBr == null || this.mBgIawareBr == null) {
            return false;
        }
        return true;
    }

    private boolean isForbidProxy(String action, String pkg) {
        synchronized (this.mLockNoProxyActions) {
            boolean noProxyAction = this.mIawareNoProxyActions.contains(action);
        }
        if (noProxyAction) {
            return true;
        }
        boolean noProxyPkg;
        synchronized (this.mLockNoProxyPkgs) {
            noProxyPkg = this.mIawareNoProxyPkgs.contains(pkg);
        }
        return noProxyPkg;
    }

    public boolean isProxyedAllowedCondition() {
        return this.mIawareScreenOn ? this.mSpeedParallelStartProxy : false;
    }

    public boolean isNotProxySysPkg(String pkg, String action) {
        synchronized (this.mLockUnProxySys) {
            ArraySet<String> actions = (ArraySet) this.mIawareUnProxySys.get(pkg);
            if (actions != null) {
                boolean contains = actions.contains(action);
                return contains;
            }
            return false;
        }
    }

    public boolean isTrimAction(String action) {
        boolean contains;
        synchronized (this.mLockTrimActions) {
            contains = this.mIawareTrimActions.contains(action);
        }
        return contains;
    }

    public void updateXmlConfig() {
        if (AwareBroadcastDebug.getDebugDetail()) {
            AwareLog.i(TAG, "updateXmlConfig begin");
        }
        if (isIawarePrepared()) {
            synchronized (this.mLockNoProxyActions) {
                this.mIawareNoProxyActions = this.mAwareBroadcastConfig.getUnProxyActionList();
            }
            synchronized (this.mLockNoProxyPkgs) {
                this.mIawareNoProxyPkgs = this.mAwareBroadcastConfig.getUnProxyPkgList();
            }
            synchronized (this.mLockUnProxySys) {
                this.mIawareUnProxySys = this.mAwareBroadcastConfig.getUnProxySysList();
            }
            synchronized (this.mLockTrimActions) {
                this.mIawareTrimActions = this.mAwareBroadcastConfig.getTrimActionList();
            }
            this.mForegroundAppLevel = this.mAwareBroadcastConfig.getFGAppLevel();
            this.mNoTouchCheckCount = this.mAwareBroadcastConfig.getNoTouchCheckCount();
            this.mTouchCheckCount = this.mAwareBroadcastConfig.getTouchCheckCount();
            this.mBgIawareBr.setUnProxyMaxDuration(this.mAwareBroadcastConfig.getUnProxyMaxDuration());
            this.mBgIawareBr.setUnProxyMaxSpeed(this.mAwareBroadcastConfig.getUnProxyMaxSpeed());
            this.mBgIawareBr.setUnProxyMinSpeed(this.mAwareBroadcastConfig.getUnProxyMinSpeed());
            this.mBgIawareBr.setUnProxyMiddleSpeed(this.mAwareBroadcastConfig.getUnProxyMiddleSpeed());
            this.mBgIawareBr.setUnProxyHighSpeed(this.mAwareBroadcastConfig.getUnProxyHighSpeed());
            this.mFgIawareBr.setUnProxyMaxDuration(this.mAwareBroadcastConfig.getUnProxyMaxDuration());
            this.mFgIawareBr.setUnProxyMaxSpeed(this.mAwareBroadcastConfig.getUnProxyMaxSpeed());
            this.mFgIawareBr.setUnProxyMinSpeed(this.mAwareBroadcastConfig.getUnProxyMinSpeed());
            this.mFgIawareBr.setUnProxyMiddleSpeed(this.mAwareBroadcastConfig.getUnProxyMiddleSpeed());
            this.mFgIawareBr.setUnProxyHighSpeed(this.mAwareBroadcastConfig.getUnProxyHighSpeed());
            return;
        }
        AwareLog.e(TAG, "iaware process broacast don't prepared.");
    }

    public void iawareStartCountBroadcastSpeed(boolean isParallel, long dispatchClockTime, int size) {
        if (this.mIawareScreenOn && !this.mSpeedParallelStartProxy && isIawarePrepared() && isParallel) {
            checkParallCount(dispatchClockTime, size);
        }
    }

    public void endCheckCount() {
        if (isIawarePrepared()) {
            Message msg = this.mHandler.obtainMessage();
            msg.what = MSG_POLICY_END_CHECK;
            this.mHandler.sendMessage(msg);
        }
    }

    private void startUnproxyBroadcast() {
        if (isEmptyIawareBrList()) {
            this.mSpeedParallelStartProxy = false;
            return;
        }
        this.mBgIawareBr.starUnproxyBroadcast();
        this.mFgIawareBr.starUnproxyBroadcast();
    }

    private void checkParallCount(long dispatchClockTime, int size) {
        this.mCountCheck = 0;
        int index;
        if (this.mStartParallelBrTime == 0) {
            long[] jArr = this.mProxyCount[0];
            this.mStartParallelBrTime = dispatchClockTime;
            jArr[0] = dispatchClockTime;
            jArr = this.mProxyCount[0];
            this.mCountCheck = (long) size;
            jArr[1] = (long) size;
            for (index = 1; index < 10; index++) {
                this.mProxyCount[index][0] = this.mStartParallelBrTime + (((long) index) * BROADCAST_PROXY_SPEED_INTERVAL_LONG);
                this.mProxyCount[index][1] = 0;
            }
            setProxyCount();
            return;
        }
        this.mLastParallelBrTime = dispatchClockTime;
        long tempPeriod = this.mLastParallelBrTime - this.mStartParallelBrTime;
        if (tempPeriod < 0) {
            if (AwareBroadcastDebug.getDebugDetail()) {
                AwareLog.d(TAG, "iaware_br checkcount <0");
            }
            this.mStartParallelBrTime = 0;
            checkParallCount(dispatchClockTime, size);
        }
        if (tempPeriod >= 2000) {
            if (AwareBroadcastDebug.getDebugDetail()) {
                AwareLog.d(TAG, "iaware_br checkcount >2000");
            }
            this.mStartParallelBrTime = 0;
            checkParallCount(dispatchClockTime, size);
        } else if (tempPeriod >= 0 && tempPeriod < 1000) {
            index = (int) (tempPeriod / BROADCAST_PROXY_SPEED_INTERVAL_LONG);
            this.mProxyCount[index][1] = this.mProxyCount[index][1] + ((long) size);
            for (tempIndex = 0; tempIndex <= index; tempIndex++) {
                this.mCountCheck += this.mProxyCount[tempIndex][1];
            }
            setProxyCount();
        } else if (tempPeriod >= 1000) {
            index = (int) ((tempPeriod - 1000) / BROADCAST_PROXY_SPEED_INTERVAL_LONG);
            if (index == 9) {
                this.mStartParallelBrTime = 0;
                checkParallCount(dispatchClockTime, size);
            } else if (index < 9) {
                this.mStartParallelBrTime = this.mProxyCount[index + 1][0];
                int rIndex = index;
                for (tempIndex = 0; tempIndex < 10; tempIndex++) {
                    rIndex++;
                    if (rIndex < 10) {
                        this.mProxyCount[tempIndex][0] = this.mProxyCount[rIndex][0];
                        this.mProxyCount[tempIndex][1] = this.mProxyCount[rIndex][1];
                    } else if (tempIndex < 9) {
                        this.mProxyCount[tempIndex][0] = this.mProxyCount[tempIndex - 1][0] + BROADCAST_PROXY_SPEED_INTERVAL_LONG;
                        this.mProxyCount[tempIndex][1] = 0;
                    } else {
                        this.mProxyCount[tempIndex][0] = this.mProxyCount[tempIndex - 1][0] + BROADCAST_PROXY_SPEED_INTERVAL_LONG;
                        this.mProxyCount[tempIndex][1] = (long) size;
                    }
                }
                for (int countIndex = 0; countIndex < 10; countIndex++) {
                    this.mCountCheck += this.mProxyCount[countIndex][1];
                }
                setProxyCount();
            }
        }
    }

    private void setProxyCount() {
        if (isStrictCondition()) {
            if (this.mCountCheck > ((long) this.mTouchCheckCount)) {
                if (AwareBroadcastDebug.getDebug()) {
                    AwareLog.i(TAG, "iaware_br checkcount touch and receiver > " + this.mTouchCheckCount);
                }
                this.mSpeedParallelStartProxy = true;
            }
        } else if (this.mCountCheck > ((long) this.mNoTouchCheckCount)) {
            if (AwareBroadcastDebug.getDebug()) {
                AwareLog.i(TAG, "iaware_br checkcount no touch and receiver > " + this.mNoTouchCheckCount);
            }
            this.mSpeedParallelStartProxy = true;
        }
    }

    public void reportSysEvent(int event, int eventType) {
        switch (event) {
            case 15016:
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.i(TAG, "iaware_br install app: " + eventType);
                }
                if (eventType == 0) {
                    this.mIawareInstallApp = true;
                } else if (eventType == 1) {
                    this.mIawareInstallApp = false;
                } else {
                    return;
                }
                if (this.mHandler.hasMessages(MSG_INSTALL_APP_TIMEOUT)) {
                    this.mHandler.removeMessages(MSG_INSTALL_APP_TIMEOUT);
                }
                if (eventType == 0) {
                    this.mHandler.sendEmptyMessageDelayed(MSG_INSTALL_APP_TIMEOUT, 5000);
                    break;
                }
                break;
            case 20011:
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.d(TAG, "iaware_br dev status event screen on");
                }
                this.mIawareScreenOn = true;
                break;
            case 90011:
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.d(TAG, "iaware_br dev status event screen off");
                }
                this.mIawareScreenOn = false;
                resetUnproxySpeedScreenOff();
                break;
        }
    }

    private void resetUnproxySpeedScreenOff() {
        if (isIawarePrepared()) {
            this.mBgIawareBr.setUnProxySpeedScreenOff();
            this.mFgIawareBr.setUnProxySpeedScreenOff();
        }
    }

    private void setIawarePolicy(int type, int event) {
        switch (type) {
            case 2:
                if (event == 1) {
                    this.mIawareProxySlip = true;
                    return;
                } else if (event == 0) {
                    this.mIawareProxySlip = false;
                    return;
                } else if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.d(TAG, "don't process event " + event);
                    return;
                } else {
                    return;
                }
            case 4:
                if (event == 1) {
                    this.mIawareProxyActivitStart = true;
                    return;
                } else if (event == 0) {
                    this.mIawareProxyActivitStart = false;
                    return;
                } else if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.d(TAG, "don't process event " + event);
                    return;
                } else {
                    return;
                }
            default:
                return;
        }
    }

    public boolean isSpeedNoCtrol() {
        return isStrictCondition() ^ 1;
    }

    public boolean isScreenOff() {
        return this.mIawareScreenOn ^ 1;
    }

    private boolean isStrictCondition() {
        if (this.mIawareProxySlip || this.mIawareProxyActivitStart) {
            return true;
        }
        return false;
    }

    public boolean isEmptyIawareBrList() {
        return this.mBgIawareBr.getIawareBrSize() == 0 && this.mFgIawareBr.getIawareBrSize() == 0;
    }

    public void setStartProxy(boolean startProxy) {
        this.mSpeedParallelStartProxy = startProxy;
    }

    public boolean getStartProxy() {
        return this.mSpeedParallelStartProxy;
    }

    public void dumpIawareBr(PrintWriter pw) {
        pw.println("    feature enable :" + BroadcastFeature.isFeatureEnabled(10));
        synchronized (this.mLockNoProxyActions) {
            pw.println("    Default no proxy actions :" + this.mIawareNoProxyActions);
        }
        synchronized (this.mLockNoProxyPkgs) {
            pw.println("    Default no proxy pkgs :" + this.mIawareNoProxyPkgs);
        }
        synchronized (this.mLockUnProxySys) {
            pw.println("    Default unproxy sys :" + this.mIawareUnProxySys);
        }
        synchronized (this.mLockTrimActions) {
            pw.println("    Default trim action :" + this.mIawareTrimActions);
        }
        pw.println("    fg app level :" + this.mForegroundAppLevel);
        pw.println("    The receiver speed :" + this.mCountCheck);
        ArraySet<Integer> iawareDownloadingUid = new ArraySet();
        ArrayList<String> iawareDownloadingPkgs = new ArrayList();
        synchronized (this.mIawareDownloadingUid) {
            iawareDownloadingUid.addAll(this.mIawareDownloadingUid);
        }
        if (iawareDownloadingUid.size() > 0) {
            PackageManagerService pms = (PackageManagerService) ServiceManager.getService("package");
            for (Integer uid : iawareDownloadingUid) {
                String name = pms.getNameForUid(uid.intValue());
                if (name != null) {
                    iawareDownloadingPkgs.add(name);
                }
            }
        }
        pw.println("    App Downloading:" + iawareDownloadingPkgs);
        pw.println("    Screen:" + (this.mIawareScreenOn ? PreciseIgnore.COMP_SCREEN_ON_VALUE_ : "off"));
        pw.println("    Operation: [" + (this.mIawareProxySlip ? "slip" : "") + " " + (this.mIawareProxyActivitStart ? "activityStart" : "") + "]");
        pw.println("    Proxy info:");
        this.mBgIawareBr.dump(pw);
        this.mFgIawareBr.dump(pw);
    }

    public void notifyIawareUnproxyBr(int pid, int uid) {
        if (isIawarePrepared()) {
            this.mBgIawareBr.startUnproxyFgAppBroadcast(pid, uid);
            this.mFgIawareBr.startUnproxyFgAppBroadcast(pid, uid);
        }
    }

    public int getForegroundAppLevel() {
        return this.mForegroundAppLevel;
    }

    public boolean isInstallApp() {
        return this.mIawareInstallApp;
    }
}
