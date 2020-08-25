package com.android.server.am;

public final class HwActiveServices extends ActiveServices {
    static final String EXCLUDE_PROCESS = "com.huawei.android.pushagent.PushService";
    static final String TAG = "HwActiveServices";

    public HwActiveServices(ActivityManagerService service) {
        super(service);
    }
}
