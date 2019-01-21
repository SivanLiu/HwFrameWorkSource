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

    private class InnerHandler extends Handler {
        public InnerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String str = HwAddViewManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage msg=");
            stringBuilder.append(msg.what);
            Slog.d(str, stringBuilder.toString());
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    HwAddViewManager.this.addListInfo(msg.getData());
                    return;
                case 1:
                    HwAddViewManager.this.deleteListInfo(msg.getData());
                    return;
                default:
                    return;
            }
        }
    }

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
        switch (operation) {
            case 0:
                msg.what = 0;
                break;
            case 1:
                msg.what = 1;
                break;
            default:
                return 1;
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
                int userid = uid / 100000;
                if (this.addviewMapWithUser.size() != 0) {
                    if (this.addviewMapWithUser.get(Integer.valueOf(userid)) != null) {
                        if (((AppOpsManager) this.mContext.getSystemService("appops")).checkOpNoThrow("android:system_alert_window", uid, packageName) == 0) {
                            Slog.d(TAG, "permission allow");
                            return true;
                        }
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("addViewPermissionCheck: ");
                        stringBuilder.append(packageName);
                        stringBuilder.append(" type: ");
                        stringBuilder.append(type);
                        Slog.d(str, stringBuilder.toString());
                        Map<String, Integer> currentUserPermissionMap = (Map) this.addviewMapWithUser.get(Integer.valueOf(userid));
                        if (currentUserPermissionMap == null || !currentUserPermissionMap.containsKey(packageName)) {
                            Slog.d(TAG, "not in blacklist, return default result-success");
                            return true;
                        }
                        boolean permissionResult = getPermissionResult(type, ((Integer) currentUserPermissionMap.get(packageName)).intValue());
                        return permissionResult;
                    }
                }
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("list not ready, return allow, uid:");
                stringBuilder2.append(uid);
                Slog.i(str2, stringBuilder2.toString());
                return true;
            }
        }
    }

    private boolean getPermissionResult(int compareValue, int sourceValue) {
        if (sourceValue == 0 || sourceValue == 1 || (compareValue & sourceValue) == 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("permission: ");
            stringBuilder.append(compareValue);
            stringBuilder.append(" allow");
            Slog.d(str, stringBuilder.toString());
            return true;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("permission: ");
        stringBuilder2.append(compareValue);
        stringBuilder2.append(" denied");
        Slog.d(str2, stringBuilder2.toString());
        return false;
    }

    private void addListInfo(Bundle data) {
        if (data == null) {
            Slog.e(TAG, "error of null bundle");
            return;
        }
        int userid = data.getInt(USERID, -1);
        Map<String, Integer> blackListMap = arrayConvertMap(data.getStringArrayList(PACKAGENAME), data.getIntegerArrayList("value"));
        if (!(blackListMap == null || userid == -1)) {
            synchronized (serviceLock) {
                Map<String, Integer> currentUserPermissionMap = (Map) this.addviewMapWithUser.get(Integer.valueOf(userid));
                if (currentUserPermissionMap != null) {
                    currentUserPermissionMap.putAll(blackListMap);
                } else {
                    currentUserPermissionMap = blackListMap;
                }
                this.addviewMapWithUser.put(Integer.valueOf(userid), currentUserPermissionMap);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("update data ok, user=");
                stringBuilder.append(userid);
                Slog.i(str, stringBuilder.toString());
            }
        }
    }

    private void deleteListInfo(Bundle data) {
        if (data != null) {
            int userid = data.getInt(USERID, -1);
            ArrayList<String> PackageNameList = data.getStringArrayList(PACKAGENAME);
            if (PackageNameList != null && userid != -1) {
                int packagenameSize = PackageNameList.size();
                synchronized (serviceLock) {
                    Map<String, Integer> currentUserPermissionMap = (Map) this.addviewMapWithUser.get(Integer.valueOf(userid));
                    if (currentUserPermissionMap != null) {
                        for (int i = 0; i < packagenameSize; i++) {
                            currentUserPermissionMap.remove(PackageNameList.get(i));
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("index:");
                            stringBuilder.append(i);
                            stringBuilder.append(" remove package:");
                            stringBuilder.append((String) PackageNameList.get(i));
                            Slog.d(str, stringBuilder.toString());
                        }
                        this.addviewMapWithUser.put(Integer.valueOf(userid), currentUserPermissionMap);
                    }
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("remove done, size ");
                    stringBuilder2.append(packagenameSize);
                    Slog.i(str2, stringBuilder2.toString());
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
        Map<String, Integer> listMap = new HashMap();
        for (int i = 0; i < packagenameSize; i++) {
            listMap.put((String) packageNameList.get(i), (Integer) valueList.get(i));
        }
        Slog.d(TAG, "comvert array over");
        return listMap;
    }
}
