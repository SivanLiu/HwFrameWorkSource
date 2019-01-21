package com.android.server.am;

import android.app.SynchronousUserSwitchObserver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.contentsensor.IActivityObserver;
import android.contentsensor.IActivityObserver.Stub;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Slog;
import com.huawei.android.app.IGameObserver;
import java.util.ArrayList;
import java.util.List;

public final class HwGameAssistantController {
    public static final boolean DEBUG = false;
    public static final int EVENT_MARK_AS_GAME = 4;
    public static final int EVENT_MOVE_BACKGROUND = 2;
    public static final int EVENT_MOVE_FRONT = 1;
    public static final int EVENT_REPLACE_FRONT = 3;
    public static final String GAME_GESTURE_MODE = "game_gesture_disabled_mode";
    public static final int GAME_GESTURE_MODE_CLOSE = 2;
    public static final int GAME_GESTURE_MODE_DEFAULT = 2;
    public static final int GAME_GESTURE_MODE_OPEN = 1;
    public static final String GAME_KEY_CONTROL_MODE = "game_key_control_mode";
    public static final int GAME_KEY_CONTROL_MODE_CLOSE = 2;
    public static final int GAME_KEY_CONTROL_MODE_DEFAULT = 2;
    public static final int GAME_KEY_CONTROL_MODE_OPEN = 1;
    private static final int MSG_BROADCAST_USER_PRESENT = 106;
    private static final int MSG_FOREGROUND_ACTIVITY_CHANGED = 100;
    private static final int MSG_GAME_LIST_CHANGED = 102;
    private static final int MSG_GAME_STATUS_CHANGED = 101;
    private static final int MSG_MARK_AS_GAME = 103;
    private static final int MSG_PACKAGE_REMOVED_OR_DATA_CLEARED = 104;
    private static final int MSG_USER_SWITCHING = 105;
    public static final String PERMISSION_READ_GAME_LIST = "com.huawei.gameassistant.permission.READ_GAME_LIST";
    public static final String PERMISSION_WRITE_GAME_LIST = "com.huawei.gameassistant.permission.WIRTE_GAME_LIST";
    static final String TAG = "HwGameAssistantController";
    private boolean isSwitchingUser = false;
    private IActivityObserver mActivityObserver = new Stub() {
        public void activityResumed(int pid, int uid, ComponentName componentName) throws RemoteException {
            String str = HwGameAssistantController.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pid=");
            stringBuilder.append(pid);
            stringBuilder.append(", uid=");
            stringBuilder.append(uid);
            stringBuilder.append(", component=");
            stringBuilder.append(componentName);
            Slog.d(str, stringBuilder.toString());
            Bundle bundle = new Bundle();
            bundle.putInt("pid", pid);
            bundle.putInt("uid", uid);
            bundle.putString("packageName", componentName != null ? componentName.getPackageName() : "");
            HwGameAssistantController.this.mHandler.sendMessage(HwGameAssistantController.this.mHandler.obtainMessage(100, bundle));
        }

        public void activityPaused(int pid, int uid, ComponentName componentName) throws RemoteException {
        }
    };
    private final Context mContext;
    private int mCurFgPid;
    private String mCurFgPkg;
    private boolean mCurIsGame;
    final RemoteCallbackList<IGameObserver> mGameObservers = new RemoteCallbackList();
    private ArrayList<String> mGameSpacePackageList = new ArrayList();
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private String mLastForegroundPkg = "";
    private boolean mLastIsGame = false;
    private final HwActivityManagerService mService;
    private SynchronousUserSwitchObserver mUserSwitchObserver = new SynchronousUserSwitchObserver() {
        public void onUserSwitching(int newUserId) throws RemoteException {
            String str = HwGameAssistantController.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onUserSwitching newUserId = ");
            stringBuilder.append(newUserId);
            Slog.w(str, stringBuilder.toString());
            HwGameAssistantController.this.isSwitchingUser = true;
            HwGameAssistantController.this.mHandler.sendEmptyMessage(105);
        }
    };

    public HwGameAssistantController(HwActivityManagerService service) {
        this.mService = service;
        this.mContext = service.mContext;
        initHandlerThread();
        this.mService.registerActivityObserver(this.mActivityObserver);
        this.mService.registerUserSwitchObserver(this.mUserSwitchObserver, TAG);
    }

    private void initHandlerThread() {
        this.mHandlerThread = new HandlerThread(TAG);
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                Bundle bundle;
                switch (msg.what) {
                    case 100:
                        bundle = (Bundle) msg.obj;
                        HwGameAssistantController.this.updateForegroundActivityState(bundle.getInt("pid"), bundle.getInt("uid"), bundle.getString("packageName"));
                        if (!HwGameAssistantController.this.isSwitchingUser) {
                            HwGameAssistantController.this.handleForegroundActivityChanged();
                            break;
                        } else {
                            Slog.d(HwGameAssistantController.TAG, "user is not present");
                            return;
                        }
                    case 101:
                        bundle = (Bundle) msg.obj;
                        HwGameAssistantController.this.handleGameStatusChanged(bundle.getString("packageName"), bundle.getInt("event"));
                        break;
                    case 102:
                        HwGameAssistantController.this.handleGameListChanged();
                        break;
                    case 104:
                        bundle = msg.obj;
                        HwGameAssistantController.this.handlePackageRemovedOrDataCleared(bundle.getInt("userId"), bundle.getString("packageName"));
                        break;
                    case 105:
                        HwGameAssistantController.this.updateForegroundActivityState(0, 0, "");
                        HwGameAssistantController.this.handleForegroundActivityChanged();
                        break;
                    case 106:
                        HwGameAssistantController.this.isSwitchingUser = false;
                        HwGameAssistantController.this.handleForegroundActivityChanged();
                        break;
                }
            }
        };
    }

    private boolean isPrimaryUser(int userId) {
        UserInfo userInfo = null;
        long identity = Binder.clearCallingIdentity();
        try {
            userInfo = UserManager.get(this.mContext).getUserInfo(userId);
            if (userInfo == null || !userInfo.isPrimary()) {
                return false;
            }
            return true;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private UserInfo getCurrentUserInfo() {
        UserInfo userInfo = null;
        long identity = Binder.clearCallingIdentity();
        try {
            userInfo = this.mService.getCurrentUser();
            return userInfo;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private int getCurrentUserId() {
        UserInfo userInfo = getCurrentUserInfo();
        return userInfo != null ? userInfo.id : 0;
    }

    private boolean isGameModeDisabled() {
        UserInfo userInfo = getCurrentUserInfo();
        if (userInfo != null && userInfo.isPrimary()) {
            return false;
        }
        Slog.e(TAG, "Game Mode is disabled for non-primary user!");
        return true;
    }

    private void updateForegroundActivityState(int pid, int uid, String packageName) {
        this.mCurFgPkg = "";
        this.mCurFgPid = 0;
        if (isPrimaryUser(UserHandle.getUserId(uid))) {
            this.mCurFgPkg = packageName;
            this.mCurFgPid = pid;
        }
        this.mCurIsGame = isGame(this.mCurFgPkg);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UPDATE: mCurFgPkg=");
        stringBuilder.append(this.mCurFgPkg);
        stringBuilder.append(", mCurFgPid=");
        stringBuilder.append(this.mCurFgPid);
        stringBuilder.append(", mCurIsGame=");
        stringBuilder.append(this.mCurIsGame);
        Slog.d(str, stringBuilder.toString());
    }

    private String getForegroundPackage() {
        return this.mLastForegroundPkg;
    }

    private boolean isGameForeground() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isGameForeground? ");
        stringBuilder.append(this.mLastIsGame);
        Slog.d(str, stringBuilder.toString());
        return this.mLastIsGame;
    }

    private boolean hasPermission(String permission, String func) {
        if (this.mService.checkCallingPermission(permission) == 0) {
            return true;
        }
        String msg = new StringBuilder();
        msg.append("Permission Denial: ");
        msg.append(func);
        msg.append(" from pid=");
        msg.append(Binder.getCallingPid());
        msg.append(", uid=");
        msg.append(Binder.getCallingUid());
        msg.append(" requires ");
        msg.append(permission);
        Slog.w(TAG, msg.toString());
        return false;
    }

    public boolean addGameSpacePackageList(List<String> packageList) {
        if (!hasPermission(PERMISSION_WRITE_GAME_LIST, "addGameSpacePackageList") || isGameModeDisabled()) {
            return false;
        }
        if (packageList == null || packageList.isEmpty()) {
            Slog.e(TAG, "addGameSpacePackageList error: empty list");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addGameSpacePackageList, pid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", list=");
        stringBuilder.append(packageList.toString());
        Slog.d(str, stringBuilder.toString());
        boolean changed = false;
        synchronized (this.mGameSpacePackageList) {
            String foregroundPkg = getForegroundPackage();
            int j = packageList.size();
            for (int i = 0; i < j; i++) {
                String pkgName = (String) packageList.get(i);
                if (!(TextUtils.isEmpty(pkgName) || this.mGameSpacePackageList.contains(pkgName))) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("add to Game Space:");
                    stringBuilder2.append(pkgName);
                    Slog.d(str2, stringBuilder2.toString());
                    this.mGameSpacePackageList.add(pkgName);
                    changed = true;
                    if (foregroundPkg != null && foregroundPkg.equals(pkgName)) {
                        this.mLastIsGame = true;
                        Bundle bundle = new Bundle();
                        bundle.putString("packageName", pkgName);
                        bundle.putInt("event", 4);
                        this.mHandler.sendMessage(this.mHandler.obtainMessage(101, bundle));
                    }
                }
            }
        }
        if (changed) {
            this.mHandler.sendEmptyMessage(102);
        }
        return changed;
    }

    public boolean delGameSpacePackageList(List<String> packageList) {
        if (!hasPermission(PERMISSION_WRITE_GAME_LIST, "delGameSpacePackageList") || isGameModeDisabled()) {
            return false;
        }
        if (packageList == null || packageList.isEmpty()) {
            Slog.e(TAG, "delGameSpacePackageList error: empty list");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("delGameSpacePackageList, pid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", list=");
        stringBuilder.append(packageList.toString());
        Slog.d(str, stringBuilder.toString());
        boolean changed = false;
        synchronized (this.mGameSpacePackageList) {
            int j = packageList.size();
            for (int i = 0; i < j; i++) {
                String pkgName = (String) packageList.get(i);
                if (this.mGameSpacePackageList.contains(pkgName)) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("del from Game Space:");
                    stringBuilder2.append(pkgName);
                    Slog.d(str2, stringBuilder2.toString());
                    this.mGameSpacePackageList.remove(pkgName);
                    changed = true;
                }
            }
        }
        if (changed) {
            this.mHandler.sendEmptyMessage(102);
        }
        return changed;
    }

    private boolean isGame(String packageName) {
        boolean contains;
        synchronized (this.mGameSpacePackageList) {
            contains = this.mGameSpacePackageList.contains(packageName);
        }
        return contains;
    }

    public boolean isInGameSpace(String packageName) {
        if (isGameModeDisabled()) {
            return false;
        }
        return isGame(packageName);
    }

    public List<String> getGameList() {
        if (!hasPermission(PERMISSION_READ_GAME_LIST, "getGameList") || isGameModeDisabled()) {
            return null;
        }
        List<String> list = new ArrayList();
        synchronized (this.mGameSpacePackageList) {
            list.addAll(this.mGameSpacePackageList);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getGameList: ");
        stringBuilder.append(list.toString());
        Slog.d(str, stringBuilder.toString());
        return list;
    }

    public void registerGameObserver(IGameObserver observer) {
        if (observer != null) {
            boolean result = this.mGameObservers.register(observer);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("registerGameObserver:");
            stringBuilder.append(observer);
            stringBuilder.append(", result=");
            stringBuilder.append(result);
            Slog.d(str, stringBuilder.toString());
        }
    }

    public void unregisterGameObserver(IGameObserver observer) {
        if (observer != null) {
            boolean result = this.mGameObservers.unregister(observer);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unregisterGameObserver:");
            stringBuilder.append(observer);
            stringBuilder.append(", result=");
            stringBuilder.append(result);
            Slog.d(str, stringBuilder.toString());
        }
    }

    public boolean isGameDndOn() {
        return isGameForeground();
    }

    public boolean isGameKeyControlOn() {
        int userId = getCurrentUserId();
        int mode = Secure.getIntForUser(this.mContext.getContentResolver(), GAME_KEY_CONTROL_MODE, 2, userId);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Keycontrol mode is ");
        stringBuilder.append(mode);
        stringBuilder.append(" for user ");
        stringBuilder.append(userId);
        Slog.d(str, stringBuilder.toString());
        return mode == 1 && isGameForeground();
    }

    public boolean isGameGestureDisabled() {
        int userId = getCurrentUserId();
        int mode = Secure.getIntForUser(this.mContext.getContentResolver(), GAME_GESTURE_MODE, 2, userId);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Gesture disable mode is ");
        stringBuilder.append(mode);
        stringBuilder.append(" for user ");
        stringBuilder.append(userId);
        Slog.d(str, stringBuilder.toString());
        return mode == 1 && isGameForeground();
    }

    private void handleGameStatusChanged(String packageName, int event) {
        dispatchGameStatusChanged(packageName, event);
    }

    private void handleForegroundActivityChanged() {
        int event = 0;
        if (!(this.mCurFgPkg == null || this.mCurFgPkg.equals(this.mLastForegroundPkg))) {
            if (this.mLastIsGame) {
                event = this.mCurIsGame ? 3 : 2;
            } else if (this.mCurIsGame) {
                event = 1;
            }
        }
        this.mLastForegroundPkg = this.mCurFgPkg;
        this.mLastIsGame = this.mCurIsGame;
        if (event != 0) {
            dispatchGameStatusChanged(this.mCurFgPkg, event);
        }
    }

    private void dispatchGameStatusChanged(String packageName, int event) {
        int i = this.mGameObservers.beginBroadcast();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dispatchGameStatusChanged, packageName=");
        stringBuilder.append(packageName);
        stringBuilder.append(", event=");
        stringBuilder.append(event);
        stringBuilder.append(", i=");
        stringBuilder.append(i);
        Slog.d(str, stringBuilder.toString());
        while (i > 0) {
            i--;
            IGameObserver observer = (IGameObserver) this.mGameObservers.getBroadcastItem(i);
            if (observer != null) {
                try {
                    observer.onGameStatusChanged(packageName, event);
                } catch (RemoteException e) {
                    Slog.e(TAG, "dispatchGameStatusChanged error because RemoteException!");
                }
            }
        }
        this.mGameObservers.finishBroadcast();
    }

    private void handleGameListChanged() {
        dispatchGameListChanged();
    }

    private void dispatchGameListChanged() {
        int i = this.mGameObservers.beginBroadcast();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dispatchGameListChanged, i=");
        stringBuilder.append(i);
        Slog.d(str, stringBuilder.toString());
        while (i > 0) {
            i--;
            IGameObserver observer = (IGameObserver) this.mGameObservers.getBroadcastItem(i);
            if (observer != null) {
                try {
                    observer.onGameListChanged();
                } catch (RemoteException e) {
                    Slog.e(TAG, "dispatchGameListChanged error because RemoteException!");
                }
            }
        }
        this.mGameObservers.finishBroadcast();
    }

    /* JADX WARNING: Missing block: B:14:0x0029, code skipped:
            r3.mHandler.sendEmptyMessage(102);
     */
    /* JADX WARNING: Missing block: B:19:0x0034, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handlePackageRemovedOrDataCleared(int userId, String packageName) {
        if ("com.huawei.gameassistant".equals(packageName) && isPrimaryUser(userId)) {
            synchronized (this.mGameSpacePackageList) {
                if (this.mGameSpacePackageList.isEmpty()) {
                } else {
                    Slog.d(TAG, "Clear gamespace because GameAssistant is removed or data cleared!");
                    this.mGameSpacePackageList.clear();
                }
            }
        }
    }

    void onPackageRemovedOrDataCleared(Intent intent) {
        if (intent != null && intent.getData() != null) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            String packageName = intent.getData().getSchemeSpecificPart();
            Bundle bundle = new Bundle();
            bundle.putInt("userId", userId);
            bundle.putString("packageName", packageName);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(104, bundle));
        }
    }

    void handleInterestedBroadcast(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            boolean z = true;
            int hashCode = action.hashCode();
            if (hashCode != 267468725) {
                if (hashCode != 525384130) {
                    if (hashCode == 823795052 && action.equals("android.intent.action.USER_PRESENT")) {
                        z = true;
                    }
                } else if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                    z = false;
                }
            } else if (action.equals("android.intent.action.PACKAGE_DATA_CLEARED")) {
                z = true;
            }
            switch (z) {
                case false:
                    if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                        Slog.d(TAG, "ACTION_PACKAGE_REMOVED received, but has EXTRA_REPLACING true");
                        break;
                    }
                case true:
                    onPackageRemovedOrDataCleared(intent);
                    break;
                case true:
                    this.mHandler.sendEmptyMessage(106);
                    break;
            }
        }
    }
}
