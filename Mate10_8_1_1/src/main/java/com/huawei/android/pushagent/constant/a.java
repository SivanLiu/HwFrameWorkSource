package com.huawei.android.pushagent.constant;

public class a {
    private static final String[] hf = new String[]{"android.intent.action.TIME_SET", "android.intent.action.TIMEZONE_CHANGED", "com.huawei.android.push.intent.GET_PUSH_STATE", "com.huawei.android.push.intent.DEREGISTER", "com.huawei.intent.action.SELF_SHOW_FLAG", "com.huawei.android.push.intent.MSG_RESPONSE", "android.ctrlsocket.all.allowed", "android.scroff.ctrlsocket.status"};
    private static final String[] hg = new String[]{"com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT", "com.huawei.intent.action.PUSH_OFF", "com.huawei.action.CONNECT_PUSHSRV", "com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE", "com.huawei.intent.action.PUSH", "com.huawei.android.push.intent.MSG_RSP_TIMEOUT", "com.huawei.android.push.intent.RESET_BASTET", "com.huawei.android.push.intent.RESPONSE_FAIL"};
    private static final String[] hh = new String[]{"com.huawei.android.push.intent.REGISTER"};

    public static String[] xg() {
        Object obj = new String[hg.length];
        System.arraycopy(hg, 0, obj, 0, hg.length);
        return obj;
    }

    public static String[] xe() {
        Object obj = new String[hf.length];
        System.arraycopy(hf, 0, obj, 0, hf.length);
        return obj;
    }

    public static String[] xf() {
        Object obj = new String[hh.length];
        System.arraycopy(hh, 0, obj, 0, hh.length);
        return obj;
    }
}
