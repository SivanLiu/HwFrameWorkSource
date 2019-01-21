package com.huawei.decision;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.huawei.common.service.IDecision;
import com.huawei.common.service.IDecision.Stub;

public final class DecisionHelper {
    private static final String ACTION_COMMON_SERVICE_NAME = "com.huawei.recsys.decision.action.BIND_DECISION_SERVICE";
    private static final String CATEGORY_KEY = "category";
    private static final boolean DEBUG = false;
    private static final String ID_KEY = "id";
    public static final String ROLLBACK_EVENT = "com.huawei.control.intent.action.RollBackEvent";
    public static final String ROLLBACK_USED_EVENT = "com.huawei.control.intent.action.RollBackUsedEvent";
    private static final String SERVICE_PACKAGE_NAME = "com.huawei.recsys";
    private static final String TAG = DecisionHelper.class.getSimpleName();
    private boolean mBound;
    private IDecision mDecisionApi;
    private ServiceConnection mDecisionConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            DecisionHelper.this.mDecisionApi = Stub.asInterface(service);
            DecisionHelper.this.mBound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            DecisionHelper.this.mDecisionApi = null;
            DecisionHelper.this.mBound = DecisionHelper.DEBUG;
        }
    };

    public void bindService(Context context) {
        if (this.mBound) {
            Log.w(TAG, "service already binded");
            return;
        }
        if (context != null && this.mDecisionApi == null) {
            Intent actionService = new Intent(ACTION_COMMON_SERVICE_NAME);
            actionService.setPackage(SERVICE_PACKAGE_NAME);
            try {
                context.bindService(actionService, this.mDecisionConnection, 1);
            } catch (Exception e) {
                Log.e(TAG, "bind service exception");
            }
        }
    }

    public void unbindService(Context context) {
        if (this.mBound) {
            if (!(context == null || this.mDecisionApi == null)) {
                try {
                    context.unbindService(this.mDecisionConnection);
                } catch (Exception e) {
                    Log.e(TAG, "unbindService service exception");
                }
            }
            this.mDecisionApi = null;
            this.mBound = DEBUG;
            return;
        }
        Log.w(TAG, "service already unbindService");
    }

    public void executeEvent(String eventName) {
        if (this.mDecisionApi != null && !TextUtils.isEmpty(eventName)) {
            try {
                ArrayMap<String, String> extra = new ArrayMap();
                extra.put("id", "");
                extra.put(CATEGORY_KEY, eventName);
                this.mDecisionApi.executeEvent(extra, null);
            } catch (Exception e) {
                Log.e(TAG, "executeEvent exception");
            }
        }
    }
}
