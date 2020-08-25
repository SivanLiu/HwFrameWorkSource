package com.android.server.security.hsm;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HwAddViewManager {
    private static final int ACTIVITY_BG_DENIED = 4;
    private static final int ACTIVITY_lS_DENIED = 8;
    private static final int HANDLER_ADD = 0;
    private static final int HANDLER_DELETE = 1;
    private static final int HANDLER_UNKNOW = -1;
    private static final int OPERATION_ADD = 0;
    private static final int OPERATION_DELETE = 1;
    private static final int OPS_ACTIVITY_BG_DENIED = 5;
    private static final int OPS_ACTIVITY_LS_DENIED = 9;
    private static final int OPS_ALLOW = 0;
    private static final int OPS_DENIED_ORG = 1;
    private static final int OPS_TOAST_DENIED = 3;
    private static final String PACKAGENAME = "packagename";
    private static final int PER_USER_RANGE = 100000;
    private static final int RET_DIRTY_DATA = 2;
    private static final int RET_FAIL = 1;
    private static final int RET_SUCCESS = 0;
    private static final String TAG = "HwAddViewManager";
    private static final int TOAST_DENIED = 2;
    private static final String USERID = "userid";
    private static final String VALUE = "value";
    private static volatile HwAddViewManager sInstance;
    private static final Object serviceLock = new Object();
    private Map<Integer, Map<String, Integer>> addviewMapWithUser = new HashMap();
    private Context mContext;
    private Handler mHandler = null;
    private HandlerThread mHandlerThread = new HandlerThread("HwAddViewManagerHandleThread");

    private HwAddViewManager(Context context) {
        this.mContext = context;
        this.mHandlerThread.start();
        this.mHandler = new InnerHandler(this.mHandlerThread.getLooper());
    }

    public static HwAddViewManager getInstance(Context context) {
        HwAddViewManager hwAddViewManager;
        synchronized (serviceLock) {
            if (sInstance == null) {
                sInstance = new HwAddViewManager(context);
            }
            hwAddViewManager = sInstance;
        }
        return hwAddViewManager;
    }

    public int updateAddViewData(Bundle data, int operation) {
        Message msg = this.mHandler.obtainMessage();
        if (operation == 0) {
            msg.what = 0;
        } else if (operation != 1) {
            return 1;
        } else {
            msg.what = 1;
        }
        msg.setData(data);
        this.mHandler.sendMessage(msg);
        return 0;
    }

    public boolean addViewPermissionCheck(String packageName, int type, int uid) {
        if (packageName == null) {
            Slog.e(TAG, "param error");
            return false;
        } else if (uid == 0) {
            Slog.i(TAG, "param uid info error, true default");
            return true;
        } else {
            synchronized (serviceLock) {
                int userid = uid / PER_USER_RANGE;
                if (this.addviewMapWithUser.size() != 0) {
                    if (this.addviewMapWithUser.get(Integer.valueOf(userid)) != null) {
                        if (((AppOpsManager) this.mContext.getSystemService("appops")).checkOpNoThrow("android:system_alert_window", uid, packageName) == 0) {
                            Slog.d(TAG, "permission allow");
                            return true;
                        }
                        Map<String, Integer> currentUserPermissionMap = this.addviewMapWithUser.get(Integer.valueOf(userid));
                        if (currentUserPermissionMap == null || !currentUserPermissionMap.containsKey(packageName)) {
                            Slog.d(TAG, "not in blacklist, return default result-success");
                            return true;
                        }
                        Slog.d(TAG, "addViewPermissionCheck: " + packageName + " type: " + type);
                        return getPermissionResult(type, currentUserPermissionMap.get(packageName).intValue());
                    }
                }
                Slog.i(TAG, "list not ready, return allow, uid:" + uid);
                return true;
            }
        }
    }

    private boolean getPermissionResult(int compareValue, int sourceValue) {
        if (sourceValue == 0 || sourceValue == 1 || (compareValue & sourceValue) == 0) {
            Slog.d(TAG, "permission: " + compareValue + " allow");
            return true;
        }
        Slog.d(TAG, "permission: " + compareValue + " denied");
        return false;
    }

    private class InnerHandler extends Handler {
        public InnerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Slog.d(HwAddViewManager.TAG, "handleMessage msg=" + msg.what);
            super.handleMessage(msg);
            int i = msg.what;
            if (i == 0) {
                HwAddViewManager.this.addListInfo(msg.getData());
            } else if (i == 1) {
                HwAddViewManager.this.deleteListInfo(msg.getData());
            }
        }
    }

    /* access modifiers changed from: private */
    public void addListInfo(Bundle data) {
        if (data == null) {
            Slog.e(TAG, "error of null bundle");
            return;
        }
        int userid = data.getInt(USERID, -1);
        Map<String, Integer> blackListMap = arrayConvertMap(data.getStringArrayList(PACKAGENAME), data.getIntegerArrayList("value"));
        if (blackListMap != null && userid != -1) {
            synchronized (serviceLock) {
                Map<String, Integer> currentUserPermissionMap = this.addviewMapWithUser.get(Integer.valueOf(userid));
                if (currentUserPermissionMap != null) {
                    currentUserPermissionMap.putAll(blackListMap);
                } else {
                    currentUserPermissionMap = blackListMap;
                }
                this.addviewMapWithUser.put(Integer.valueOf(userid), currentUserPermissionMap);
                Slog.i(TAG, "update data ok, user=" + userid);
            }
        }
    }

    /* access modifiers changed from: private */
    public void deleteListInfo(Bundle data) {
        if (data != null) {
            int userid = data.getInt(USERID, -1);
            ArrayList<String> PackageNameList = data.getStringArrayList(PACKAGENAME);
            if (PackageNameList != null && userid != -1) {
                int packagenameSize = PackageNameList.size();
                synchronized (serviceLock) {
                    Map<String, Integer> currentUserPermissionMap = this.addviewMapWithUser.get(Integer.valueOf(userid));
                    if (currentUserPermissionMap != null) {
                        for (int i = 0; i < packagenameSize; i++) {
                            currentUserPermissionMap.remove(PackageNameList.get(i));
                            Slog.d(TAG, "index:" + i + " remove package:" + PackageNameList.get(i));
                        }
                        this.addviewMapWithUser.put(Integer.valueOf(userid), currentUserPermissionMap);
                    }
                    Slog.i(TAG, "remove done, size " + packagenameSize);
                }
            }
        }
    }

    private Map<String, Integer> arrayConvertMap(ArrayList<String> packageNameList, ArrayList<Integer> valueList) {
        if (packageNameList == null || valueList == null) {
            Slog.e(TAG, "null list or map");
            return null;
        }
        int packagenameSize = packageNameList.size();
        if (packagenameSize != valueList.size()) {
            Slog.e(TAG, "dirty list");
            return null;
        }
        Map<String, Integer> listMap = new HashMap<>();
        for (int i = 0; i < packagenameSize; i++) {
            listMap.put(packageNameList.get(i), valueList.get(i));
        }
        Slog.d(TAG, "comvert array over");
        return listMap;
    }
}
