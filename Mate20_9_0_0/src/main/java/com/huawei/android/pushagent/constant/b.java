package com.huawei.android.pushagent.constant;

public class b {
    private static final String[] v = new String[]{"android.intent.action.TIME_SET", "android.intent.action.TIMEZONE_CHANGED", "com.huawei.android.push.intent.GET_PUSH_STATE", "com.huawei.android.push.intent.DEREGISTER", "com.huawei.intent.action.SELF_SHOW_FLAG", "com.huawei.android.push.intent.MSG_RESPONSE", "android.ctrlsocket.all.allowed", "android.scroff.ctrlsocket.status"};
    private static final String[] w = new String[]{"com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT", "com.huawei.intent.action.PUSH_OFF", "com.huawei.action.CONNECT_PUSHSRV", "com.huawei.action.CONNECT_PUSHSRV_NON_WAKEUP", "com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE", "com.huawei.intent.action.PUSH", "com.huawei.push.alarm.HEARTBEAT", "com.huawei.android.push.intent.MSG_RSP_TIMEOUT", "com.huawei.android.push.intent.RESET_BASTET", "com.huawei.android.push.intent.RESPONSE_FAIL"};
    private static final String[] x = new String[]{"com.huawei.android.push.intent.REGISTER"};

    public static String[] be() {
        String[] strArr = new String[w.length];
        System.arraycopy(w, 0, strArr, 0, w.length);
        return strArr;
    }

    public static String[] bc() {
        String[] strArr = new String[v.length];
        System.arraycopy(v, 0, strArr, 0, v.length);
        return strArr;
    }

    public static String[] bd() {
        String[] strArr = new String[x.length];
        System.arraycopy(x, 0, strArr, 0, x.length);
        return strArr;
    }
}
