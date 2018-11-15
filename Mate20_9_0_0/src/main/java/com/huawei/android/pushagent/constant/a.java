package com.huawei.android.pushagent.constant;

public class a {
    private static final String[] iy = new String[]{"android.intent.action.TIME_SET", "android.intent.action.TIMEZONE_CHANGED", "com.huawei.android.push.intent.GET_PUSH_STATE", "com.huawei.android.push.intent.DEREGISTER", "com.huawei.intent.action.SELF_SHOW_FLAG", "com.huawei.android.push.intent.MSG_RESPONSE", "android.ctrlsocket.all.allowed", "android.scroff.ctrlsocket.status"};
    private static final String[] iz = new String[]{"com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT", "com.huawei.intent.action.PUSH_OFF", "com.huawei.action.CONNECT_PUSHSRV", "com.huawei.action.CONNECT_PUSHSRV_NON_WAKEUP", "com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE", "com.huawei.intent.action.PUSH", "com.huawei.push.alarm.HEARTBEAT", "com.huawei.android.push.intent.MSG_RSP_TIMEOUT", "com.huawei.android.push.intent.RESET_BASTET", "com.huawei.android.push.intent.RESPONSE_FAIL"};
    private static final String[] ja = new String[]{"com.huawei.android.push.intent.REGISTER"};

    public static String[] abm() {
        Object obj = new String[iz.length];
        System.arraycopy(iz, 0, obj, 0, iz.length);
        return obj;
    }

    public static String[] abl() {
        Object obj = new String[iy.length];
        System.arraycopy(iy, 0, obj, 0, iy.length);
        return obj;
    }

    public static String[] abn() {
        Object obj = new String[ja.length];
        System.arraycopy(ja, 0, obj, 0, ja.length);
        return obj;
    }
}
