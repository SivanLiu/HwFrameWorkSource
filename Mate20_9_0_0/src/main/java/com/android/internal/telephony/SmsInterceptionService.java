package com.android.internal.telephony;

import android.content.Context;
import android.os.Bundle;
import android.os.ServiceManager;
import android.telephony.Rlog;
import com.android.internal.telephony.ISmsInterception.Stub;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmsInterceptionService extends Stub {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SmsInterceptionService";
    static final int SMS_HANDLE_RESULT_BLOCK = 1;
    static final int SMS_HANDLE_RESULT_INVALID = -1;
    static final int SMS_HANDLE_RESULT_NOT_BLOCK = 0;
    private static SmsInterceptionService sInstance = null;
    private final Context mContext;
    private Map<Integer, ISmsInterceptionListener> mListener = new HashMap();

    private SmsInterceptionService(Context context) {
        this.mContext = context;
        if (ServiceManager.getService("isms_interception") == null) {
            ServiceManager.addService("isms_interception", this);
        }
    }

    public static SmsInterceptionService getDefault(Context context) {
        if (sInstance == null) {
            sInstance = new SmsInterceptionService(context);
        }
        return sInstance;
    }

    public void registerListener(ISmsInterceptionListener listener, int priority) {
        this.mContext.enforceCallingPermission("huawei.permission.RECEIVE_SMS_INTERCEPTION", "Enabling SMS interception");
        synchronized (this.mListener) {
            this.mListener.put(Integer.valueOf(priority), listener);
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerListener . priority : ");
        stringBuilder.append(priority);
        Rlog.d(str, stringBuilder.toString());
    }

    public void unregisterListener(int priority) {
        this.mContext.enforceCallingPermission("huawei.permission.RECEIVE_SMS_INTERCEPTION", "Disabling SMS interception");
        synchronized (this.mListener) {
            this.mListener.remove(Integer.valueOf(priority));
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unregisterListener . priority : ");
        stringBuilder.append(priority);
        Rlog.d(str, stringBuilder.toString());
    }

    public boolean dispatchNewSmsToInterceptionProcess(Bundle smsInfo, boolean isWapPush) {
        Exception e;
        if (this.mListener == null || this.mListener.size() == 0) {
            Rlog.d(LOG_TAG, "mListener is null or mListener.size is 0 !");
            return false;
        }
        Set<Integer> prioritySet = this.mListener.keySet();
        List<Integer> priorityList = new ArrayList();
        for (Integer keyInteger : prioritySet) {
            priorityList.add(keyInteger);
        }
        Collections.sort(priorityList);
        synchronized (this.mListener) {
            for (int i = priorityList.size() - 1; i >= 0; i--) {
                StringBuilder stringBuilder;
                if (isWapPush) {
                    try {
                        if (1 == ((ISmsInterceptionListener) this.mListener.get(priorityList.get(i))).handleWapPushDeliverActionInner(smsInfo)) {
                            String str = LOG_TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("wap push is intercepted by ...");
                            stringBuilder.append(this.mListener.get(priorityList.get(i)));
                            Rlog.d(str, stringBuilder.toString());
                            return true;
                        }
                    } catch (Exception e2) {
                        String str2 = LOG_TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Get exception while communicate with sms interception service: ");
                        stringBuilder2.append(e2.getMessage());
                        Rlog.e(str2, stringBuilder2.toString());
                    }
                } else if (1 == ((ISmsInterceptionListener) this.mListener.get(priorityList.get(i))).handleSmsDeliverActionInner(smsInfo)) {
                    e2 = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("sms is intercepted by ...");
                    stringBuilder.append(this.mListener.get(priorityList.get(i)));
                    stringBuilder.append(" priority ");
                    stringBuilder.append(priorityList.get(i));
                    Rlog.d(e2, stringBuilder.toString());
                    return true;
                }
            }
            return false;
        }
    }

    public boolean sendNumberBlockedRecord(Bundle smsInfo) {
        if (this.mListener == null || this.mListener.size() == 0) {
            Rlog.d(LOG_TAG, "mListener is null or mListener.size is 0 !");
            return false;
        }
        Set<Integer> prioritySet = this.mListener.keySet();
        List<Integer> priorityList = new ArrayList();
        for (Integer keyInteger : prioritySet) {
            priorityList.add(keyInteger);
        }
        Collections.sort(priorityList);
        synchronized (this.mListener) {
            int i = priorityList.size() - 1;
            while (i >= 0) {
                try {
                    if (((ISmsInterceptionListener) this.mListener.get(priorityList.get(i))).sendNumberBlockedRecordInner(smsInfo)) {
                        String str = LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("block has record by ...");
                        stringBuilder.append(this.mListener.get(priorityList.get(i)));
                        Rlog.d(str, stringBuilder.toString());
                        return true;
                    }
                    i--;
                } catch (Exception e) {
                    String str2 = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Get exception while communicate with sms interception service: ");
                    stringBuilder2.append(e.getMessage());
                    Rlog.e(str2, stringBuilder2.toString());
                }
            }
            return false;
        }
    }
}
