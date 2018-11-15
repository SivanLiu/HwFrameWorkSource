package com.android.server.security.hwkeychain;

import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import com.huawei.common.service.IDecision;
import com.huawei.common.service.IDecision.Stub;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DecisionUtil {
    private static final String CATEGORY_KEY = "category";
    private static final String DECISION_ACTION = "com.huawei.securitymgr.intent.action.KeychainAutoFillEvent";
    private static final String ID_KEY = "id";
    private static final String TAG = DecisionUtil.class.getSimpleName();
    private static ConcurrentHashMap<String, DecisionCallback> mCallbackList = new ConcurrentHashMap();
    private static IDecision mDecisionApi = null;
    private static ServiceConnection mDecisionConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(DecisionUtil.TAG, "service connected.");
            DecisionUtil.mDecisionApi = Stub.asInterface(service);
            if (DecisionUtil.sExtras != null) {
                Log.d(DecisionUtil.TAG, "execute again.");
                if (DecisionUtil.executeEvent(DecisionUtil.DECISION_ACTION, DecisionUtil.sExtras)) {
                    Log.d(DecisionUtil.TAG, "unbind service.");
                    DecisionUtil.unbindService(ActivityThread.currentActivityThread().getSystemUiContext());
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            DecisionUtil.mDecisionApi = null;
            Log.i(DecisionUtil.TAG, "service disconnect.");
        }
    };
    private static Handler mHander = null;
    private static Map<String, Object> sExtras;

    private static class DecisionAyncTask extends AsyncTask<Void, Void, Void> {
        private DecisionAyncTask() {
        }

        /* synthetic */ DecisionAyncTask(AnonymousClass1 x0) {
            this();
        }

        protected Void doInBackground(Void... voids) {
            DecisionUtil.bindService(ActivityThread.currentActivityThread().getSystemUiContext());
            Log.d(DecisionUtil.TAG, "execute event.");
            if (DecisionUtil.executeEvent(DecisionUtil.DECISION_ACTION, DecisionUtil.sExtras)) {
                Log.d(DecisionUtil.TAG, "unbind service.");
                DecisionUtil.unbindService(ActivityThread.currentActivityThread().getSystemUiContext());
            }
            return null;
        }
    }

    public static void bindService(Context context) {
        String ACTION_PACKAGE_NAME = "com.huawei.recsys";
        String ACTION_COMMON_SERVICE_NAME = "com.huawei.recsys.decision.action.BIND_DECISION_SERVICE";
        if (context == null || mDecisionApi != null) {
            Log.i(TAG, "service already binded");
            return;
        }
        if (mHander == null) {
            mHander = new Handler(context.getMainLooper());
        }
        Intent actionService = new Intent("com.huawei.recsys.decision.action.BIND_DECISION_SERVICE");
        actionService.setPackage("com.huawei.recsys");
        try {
            context.bindService(actionService, mDecisionConnection, 1);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bind service error: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    public static void unbindService(Context context) {
        if (context != null) {
            try {
                context.unbindService(mDecisionConnection);
            } catch (Exception e) {
                Log.e(TAG, "unbind service error");
                Log.e(TAG, e.getMessage());
            }
            mDecisionApi = null;
            sExtras = null;
        }
    }

    public static boolean executeEvent(String eventName, Map<String, Object> extras) {
        return executeEvent(eventName, null, extras, null);
    }

    public static boolean executeEvent(String eventName, String dataId, Map<String, Object> extras, DecisionCallback callback) {
        return executeEvent(eventName, dataId, extras, callback, -1);
    }

    public static boolean executeEvent(String eventName, String dataId, Map<String, Object> extras, DecisionCallback callback, long timeout) {
        if (mDecisionApi == null) {
            return false;
        }
        String key;
        ArrayMap<String, Object> extra2 = new ArrayMap();
        if (extras != null) {
            extra2.putAll(extras);
        }
        extra2.put("id", dataId != null ? dataId : "");
        if (!(eventName == null || eventName.equals(dataId))) {
            extra2.put("category", eventName);
        }
        if (callback != null) {
            key = callback.toString();
        } else {
            key = null;
        }
        final DecisionCallback innerCallback = new DecisionCallback() {
            public void onResult(Map result) throws RemoteException {
                if (key != null) {
                    DecisionUtil.mCallbackList.remove(key);
                }
                if (this.mReversed1 != null) {
                    this.mReversed1.onResult(result);
                }
            }
        };
        innerCallback.setReversed1(callback);
        if (callback != null && timeout > 0) {
            mCallbackList.put(key, callback);
            mHander.postDelayed(new Runnable() {
                public void run() {
                    DecisionCallback userCallback = (DecisionCallback) DecisionUtil.mCallbackList.remove(key);
                    if (userCallback != null) {
                        innerCallback.clearReversed1();
                        try {
                            userCallback.onTimeout();
                        } catch (Exception e) {
                        }
                    }
                }
            }, timeout);
        }
        try {
            mDecisionApi.executeEvent(extra2, innerCallback);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void autoExecuteEvent(Map<String, Object> extras) {
        sExtras = extras;
        if (sExtras != null) {
            new DecisionAyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }
}
