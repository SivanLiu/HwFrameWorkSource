package com.huawei.internal.telephony;

import com.android.internal.telephony.CallForwardInfo;
import java.util.ArrayList;

public class CallForwardInfoEx {
    private CallForwardInfo mCallForwardInfo;

    private CallForwardInfoEx(CallForwardInfo callForwardInfo) {
        this.mCallForwardInfo = callForwardInfo;
    }

    public static CallForwardInfoEx from(Object result) {
        if (result == null || !(result instanceof CallForwardInfo)) {
            return null;
        }
        return new CallForwardInfoEx((CallForwardInfo) result);
    }

    public static CallForwardInfoEx[] fromArray(Object result) {
        int i = 0;
        if (result == null || !(result instanceof CallForwardInfo[])) {
            return new CallForwardInfoEx[0];
        }
        CallForwardInfo[] infos = (CallForwardInfo[]) result;
        int len = infos.length;
        CallForwardInfoEx[] infoExs = new CallForwardInfoEx[len];
        while (i < len) {
            infoExs[i] = new CallForwardInfoEx(infos[i]);
            i++;
        }
        return infoExs;
    }

    public static CallForwardInfoEx[] fromArrayList(ArrayList<CallForwardInfo> result) {
        int i = 0;
        if (result == null) {
            return new CallForwardInfoEx[0];
        }
        ArrayList<CallForwardInfo> infos = result;
        int len = infos.size();
        CallForwardInfoEx[] infoExs = new CallForwardInfoEx[len];
        while (i < len) {
            infoExs[i] = new CallForwardInfoEx((CallForwardInfo) infos.get(i));
            i++;
        }
        return infoExs;
    }

    public String getNumber() {
        return this.mCallForwardInfo.number;
    }

    public int getStatus() {
        return this.mCallForwardInfo.status;
    }

    public int getServiceClass() {
        return this.mCallForwardInfo.serviceClass;
    }
}
