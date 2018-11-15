package com.android.systemui.shared.recents.hwutil;

import android.app.ActivityManager.RecentTaskInfo;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.hsm.MediaTransactWrapper;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import com.android.systemui.shared.recents.model.Task;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HwRecentsTaskUtils {
    public static final String ACTION_REPLY_REMOVE_TASK_FINISH = "com.huawei.systemmanager.action.REPLY_TRIM_ALL";
    public static final String ACTION_REQUEST_REMOVE_ALL_TASK = "com.huawei.systemmanager.action.REQUEST_TRIM_ALL";
    public static final String ACTION_REQUEST_REMOVE_SINGAL_TASK = "huawei.intent.action.hsm_remove_pkg";
    private static final String AUTHORITY = "com.android.systemui.recent.HwRecentsLockProdiver";
    public static final Uri AUTHORITY_URI = Uri.parse("content://com.android.systemui.recent.HwRecentsLockProdiver");
    public static final String DATABASE_RECENT_LOCK_STATE = "recent_lock_state";
    public static final String DATABASE_RECENT_PKG_NAME = "recent_lock_pkgname";
    public static final String EXTRA_REQUEST_ID = "request_id";
    private static final String EXTRA_START_TRIM_TIME = "start_time";
    public static final long MAX_REMOVE_TASK_TIME = 20000;
    public static final String PERMISSION_REPLY_REMOVE_TASK = "com.android.systemui.permission.removeTask";
    public static final String PERMISSION_REQUEST_REMOVE_TASK = "com.huawei.android.launcher.permission.ONEKEYCLEAN";
    public static final String PKG_SYS_MANAGER = "com.huawei.systemmanager";
    private static final String TAG = "HwRecentsTaskUtils";
    private static boolean isRemovingTask = false;
    private static Map<String, Boolean> lockStateMap = null;
    private static long mRequestRemoveTaskClockTime = 0;
    private static long mRequestRemoveTaskSystemTime = 0;
    private static Set<Integer> musiclist = null;

    public static synchronized void setRemoveTaskSystemTime(long removeTaskSystemTime) {
        synchronized (HwRecentsTaskUtils.class) {
            mRequestRemoveTaskSystemTime = removeTaskSystemTime;
        }
    }

    public static synchronized void setRemoveTaskClockTime(long requestRemoveTaskClockTime) {
        synchronized (HwRecentsTaskUtils.class) {
            mRequestRemoveTaskClockTime = requestRemoveTaskClockTime;
        }
    }

    public static synchronized void setInRemoveTask(boolean isRemovingTask) {
        synchronized (HwRecentsTaskUtils.class) {
            isRemovingTask = isRemovingTask;
        }
    }

    public static synchronized long getRequestRemoveTaskSystemTime() {
        long j;
        synchronized (HwRecentsTaskUtils.class) {
            j = mRequestRemoveTaskSystemTime;
        }
        return j;
    }

    public static synchronized long getRequestRemoveTaskClockTime() {
        long j;
        synchronized (HwRecentsTaskUtils.class) {
            j = mRequestRemoveTaskClockTime;
        }
        return j;
    }

    public static synchronized boolean isInRemoveTask() {
        boolean z;
        synchronized (HwRecentsTaskUtils.class) {
            z = isRemovingTask && SystemClock.elapsedRealtime() - getRequestRemoveTaskClockTime() < MAX_REMOVE_TASK_TIME;
        }
        return z;
    }

    public static synchronized boolean willRemovedTask(RecentTaskInfo task) {
        synchronized (HwRecentsTaskUtils.class) {
            boolean isInRemove = isRemovingTask;
            long removeTime = mRequestRemoveTaskSystemTime;
            if (isInRemoveTask()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("in willRemovedTask:");
                stringBuilder.append(isInRemove);
                stringBuilder.append(", task:");
                stringBuilder.append(task.id);
                stringBuilder.append(",activeTime:");
                stringBuilder.append(task.lastActiveTime);
                stringBuilder.append(",requestTime:");
                stringBuilder.append(removeTime);
                stringBuilder.append(", less:");
                stringBuilder.append(removeTime - task.lastActiveTime);
                stringBuilder.append(", absTime:");
                stringBuilder.append(Math.abs(task.lastActiveTime - removeTime));
                Log.d(str, stringBuilder.toString());
                if (task.lastActiveTime <= removeTime) {
                    return true;
                }
            }
            return false;
        }
    }

    public static synchronized Map<String, Boolean> searchFromCache() {
        synchronized (HwRecentsTaskUtils.class) {
            if (lockStateMap == null) {
                Log.e(TAG, "when call searchFromCache, lockStateMap is null!!");
                Map hashMap = new HashMap();
                return hashMap;
            }
            Map<String, Boolean> map = lockStateMap;
            return map;
        }
    }

    private static synchronized void setLockStateMap(Map<String, Boolean> map) {
        synchronized (HwRecentsTaskUtils.class) {
            lockStateMap = map;
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0055, code:
            if (r2 == null) goto L_0x006b;
     */
    /* JADX WARNING: Missing block: B:12:0x0057, code:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:17:0x0068, code:
            if (r2 == null) goto L_0x006b;
     */
    /* JADX WARNING: Missing block: B:18:0x006b, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Map<String, Boolean> searchFromDate(Context context) {
        Log.d(TAG, "searchFromDate");
        Map<String, Boolean> lockmap = new HashMap();
        Cursor tmpCursor = null;
        try {
            tmpCursor = new CursorLoader(context, AUTHORITY_URI, new String[]{DATABASE_RECENT_PKG_NAME, DATABASE_RECENT_LOCK_STATE}, null, null, null).loadInBackground();
            if (tmpCursor != null && tmpCursor.moveToFirst()) {
                do {
                    String key = tmpCursor.getString(tmpCursor.getColumnIndex(DATABASE_RECENT_PKG_NAME));
                    boolean z = true;
                    if (tmpCursor.getInt(tmpCursor.getColumnIndex(DATABASE_RECENT_LOCK_STATE)) != 1) {
                        z = false;
                    }
                    lockmap.put(key, Boolean.valueOf(z));
                } while (tmpCursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        } catch (Throwable th) {
            if (tmpCursor != null) {
                tmpCursor.close();
            }
        }
    }

    public static synchronized Map<String, Boolean> refreshToCache(Context context) {
        Map<String, Boolean> results;
        synchronized (HwRecentsTaskUtils.class) {
            Log.d(TAG, "refreshToCache");
            results = new HashMap();
            synchronized (HwRecentsTaskUtils.class) {
                setLockStateMap(searchFromDate(context));
                results = lockStateMap;
            }
        }
        return results;
    }

    public static boolean isHwTaskLocked(String pkgName, boolean def) {
        Map<String, Boolean> map = searchFromCache();
        Boolean locked = Boolean.valueOf(def);
        if (map.get(pkgName) != null) {
            locked = (Boolean) map.get(pkgName);
        }
        return locked.booleanValue();
    }

    public static void refreshPlayingMusicUidSet() {
        musiclist = MediaTransactWrapper.playingMusicUidSet();
    }

    /* JADX WARNING: Missing block: B:20:0x0055, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean getPlayingMusicUid(Context context, Task task) {
        if (context == null || task == null || musiclist == null || musiclist.isEmpty()) {
            return false;
        }
        try {
            if (!musiclist.contains(Integer.valueOf(context.getPackageManager().getPackageUid(task.packageName, task.key.userId)))) {
                return false;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PlayingMusic is ");
            stringBuilder.append(task.packageName);
            Log.d(str, stringBuilder.toString());
            return true;
        } catch (NameNotFoundException e) {
            Log.d(TAG, "Can not get packageUid return.");
            return false;
        }
    }

    public static void sendRemoveTaskToSystemManager(Context context, Task task) {
        if (context == null || task == null || task.key == null) {
            Log.i(TAG, "(sendRemoveTaskToSystemManager context == null || task == null || task.key == null), return");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("remove task send broadcast packageName=");
        stringBuilder.append(task.packageName);
        stringBuilder.append(", userId=");
        stringBuilder.append(task.key.userId);
        stringBuilder.append(",taskId=");
        stringBuilder.append(task.key.id);
        Log.i(str, stringBuilder.toString());
        Intent intent = new Intent(ACTION_REQUEST_REMOVE_SINGAL_TASK);
        intent.putExtra("pkg_name", task.packageName);
        intent.putExtra("userid", task.key.userId);
        intent.putExtra("taskid", task.key.id);
        intent.setPackage(PKG_SYS_MANAGER);
        context.sendBroadcast(intent);
    }

    private static Intent getRemoveTaskRequestIntent() {
        long currentRequestId = System.currentTimeMillis();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("gener requestId:");
        stringBuilder.append(currentRequestId);
        stringBuilder.append(" for remove all task");
        Log.d(str, stringBuilder.toString());
        return new Intent(ACTION_REQUEST_REMOVE_ALL_TASK).putExtra(EXTRA_REQUEST_ID, currentRequestId).putExtra(EXTRA_START_TRIM_TIME, getRequestRemoveTaskSystemTime());
    }

    public static void sendRemoveAllTask(Context context) {
        if (context == null) {
            Log.i(TAG, "(sendRemoveAllTask context == null return");
        } else {
            context.sendBroadcast(getRemoveTaskRequestIntent(), PERMISSION_REQUEST_REMOVE_TASK);
        }
    }
}
