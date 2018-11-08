package com.huawei.android.pushagent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.huawei.android.pushagent.a.a.d;
import com.huawei.android.pushagent.a.a.e;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

public abstract class PushReceiver extends BroadcastReceiver {
    private static int a = -1;

    public static class ACTION {
        public static final String ACTION_CLIENT_DEREGISTER = "com.huawei.android.push.intent.DEREGISTER";
        public static final String ACTION_NOTIFICATION_MSG_CLICK = "com.huawei.android.push.intent.CLICK";
        public static final String ACTION_PUSH_MESSAGE = "com.huawei.android.push.intent.RECEIVE";
    }

    public static class BOUND_KEY {
        public static final String deviceTokenKey = "deviceToken";
        public static final String pushMsgKey = "pushMsg";
        public static final String pushNotifyId = "pushNotifyId";
        public static final String pushStateKey = "pushState";
        public static final String receiveTypeKey = "receiveType";
    }

    class EventThread extends Thread {
        Context a;
        Bundle b;
        final /* synthetic */ PushReceiver c;

        public EventThread(PushReceiver pushReceiver, Context context, Bundle bundle) {
            this.c = pushReceiver;
            super("EventRunable");
            this.a = context;
            this.b = bundle;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            try {
                if (this.b != null) {
                    int -l_1_I = this.b.getInt(BOUND_KEY.receiveTypeKey);
                    if (-l_1_I >= 0 && -l_1_I < ReceiveType.values().length) {
                        switch (ReceiveType.values()[-l_1_I]) {
                            case ReceiveType_Token:
                                this.c.onToken(this.a, this.b.getString(BOUND_KEY.deviceTokenKey), this.b);
                                break;
                            case ReceiveType_Msg:
                                this.c.onPushMsg(this.a, this.b.getByteArray(BOUND_KEY.pushMsgKey), this.b.getString(BOUND_KEY.deviceTokenKey));
                                break;
                            case ReceiveType_PushState:
                                this.c.onPushState(this.a, this.b.getBoolean(BOUND_KEY.pushStateKey));
                                break;
                            case ReceiveType_NotifyClick:
                                this.c.onNotifyClickMsg(this.a, this.b.getString(BOUND_KEY.pushMsgKey));
                                break;
                            case ReceiveType_ClickBtn:
                                this.c.onNotifyBtnClick(this.a, this.b.getInt(BOUND_KEY.pushNotifyId), this.b.getString(BOUND_KEY.pushMsgKey), new Bundle());
                                break;
                            case ReceiveType_PluginRsp:
                                this.c.onPluginRsp(this.a, this.b.getInt(KEY_TYPE.PLUGINREPORTTYPE, -1), this.b.getBoolean(KEY_TYPE.PLUGINREPORTRESULT, false), this.b.getBundle(KEY_TYPE.PLUGINREPORTEXTRA));
                                break;
                        }
                        return;
                    }
                    Log.e("PushLogLightSC2907", "invalid receiverType:" + -l_1_I);
                }
            } catch (Object -l_1_R) {
                Log.e("PushLogLightSC2907", "call EventThread(ReceiveType cause:" + -l_1_R.toString(), -l_1_R);
            }
        }
    }

    static class HandlePushTokenThread extends Thread {
        Context a;
        String b;
        String c;

        public HandlePushTokenThread(Context context, String str, String str2) {
            this.a = context;
            this.b = str;
            this.c = str2;
        }

        public void run() {
            Object -l_1_R = new e(this.a, "push_client_self_info");
            -l_1_R.a("hasRequestToken", false);
            -l_1_R.d("token_info");
            d.a(this.a, "push_client_self_info", "token_info", this.b);
            if (!TextUtils.isEmpty(this.c)) {
                String -l_2_R = com.huawei.android.pushagent.a.a.a.d.a(this.a, this.c);
                if (!TextUtils.isEmpty(-l_2_R)) {
                    new e(this.a, "push_client_self_info").a("push_notify_key", -l_2_R);
                }
            }
        }
    }

    public static class KEY_TYPE {
        public static final String PKGNAME = "pkg_name";
        public static final String PLUGINREPORTEXTRA = "reportExtra";
        public static final String PLUGINREPORTRESULT = "isReportSuccess";
        public static final String PLUGINREPORTTYPE = "reportType";
        public static final String PUSHSTATE = "push_state";
        public static final String PUSH_BROADCAST_MESSAGE = "msg_data";
        public static final String PUSH_KEY_CLICK = "click";
        public static final String PUSH_KEY_CLICK_BTN = "clickBtn";
        public static final String PUSH_KEY_DEVICE_TOKEN = "device_token";
        public static final String PUSH_KEY_NOTIFY_ID = "notifyId";
        public static final String USERID = "userid";
    }

    enum ReceiveType {
        ReceiveType_Init,
        ReceiveType_Token,
        ReceiveType_Msg,
        ReceiveType_PushState,
        ReceiveType_NotifyClick,
        ReceiveType_PluginRsp,
        ReceiveType_ClickBtn
    }

    public static class SERVER {
        public static final String DEVICETOKEN = "device_token";
    }

    private static int a() {
        int -l_0_I = -999;
        try {
            Object -l_1_R = Class.forName("android.os.UserHandle");
            -l_0_I = ((Integer) -l_1_R.getDeclaredMethod("myUserId", new Class[0]).invoke(-l_1_R, new Object[0])).intValue();
            Log.d("PushLogLightSC2907", "getUserId:" + -l_0_I);
            return -l_0_I;
        } catch (ClassNotFoundException e) {
            Log.d("PushLogLightSC2907", " getUserId wrong");
            return -l_0_I;
        } catch (NoSuchMethodException e2) {
            Log.d("PushLogLightSC2907", " getUserId wrong");
            return -l_0_I;
        } catch (IllegalArgumentException e3) {
            Log.d("PushLogLightSC2907", " getUserId wrong");
            return -l_0_I;
        } catch (IllegalAccessException e4) {
            Log.d("PushLogLightSC2907", " getUserId wrong");
            return -l_0_I;
        } catch (InvocationTargetException e5) {
            Log.d("PushLogLightSC2907", " getUserId wrong");
            return -l_0_I;
        }
    }

    private void a(Context context, Intent intent) throws UnsupportedEncodingException {
        if (context == null || intent == null) {
            Log.e("PushLogLightSC2907", "context is null");
            return;
        }
        Object -l_3_R = new String(intent.getByteArrayExtra("device_token"), "UTF-8");
        Object -l_4_R = intent.getStringExtra("extra_encrypt_key");
        Log.d("PushLogLightSC2907", "get a deviceToken");
        if (TextUtils.isEmpty(-l_3_R)) {
            Log.w("PushLogLightSC2907", "get a deviceToken, but it is null");
            return;
        }
        int -l_6_I = new e(context, "push_client_self_info").a("hasRequestToken");
        Object -l_7_R = d.a(context, "push_client_self_info", "token_info");
        Object -l_8_R = new e(context, "push_client_self_info").b("push_notify_key");
        int -l_9_I = (TextUtils.isEmpty(-l_8_R) ? !TextUtils.isEmpty(-l_4_R) : !(TextUtils.isEmpty(-l_4_R) || -l_8_R.equals(-l_4_R))) ? 1 : 0;
        if (-l_6_I == 0 && -l_3_R.equals(-l_7_R) && -l_9_I == 0) {
            Log.w("PushLogLightSC2907", "get a deviceToken, but do not requested token, and new token is equals old token");
        } else {
            Log.i("PushLogLightSC2907", "push client begin to receive the token");
            new HandlePushTokenThread(context, -l_3_R, -l_4_R).start();
            Object -l_10_R = new Bundle();
            -l_10_R.putString(BOUND_KEY.deviceTokenKey, -l_3_R);
            -l_10_R.putByteArray(BOUND_KEY.pushMsgKey, null);
            -l_10_R.putInt(BOUND_KEY.receiveTypeKey, ReceiveType.ReceiveType_Token.ordinal());
            if (intent.getExtras() != null) {
                -l_10_R.putAll(intent.getExtras());
            }
            new EventThread(this, context, -l_10_R).start();
        }
    }

    private static boolean a(Context context, boolean z) {
        boolean z2 = false;
        Log.d("PushLogLightSC2907", "existFrameworkPush:" + a + ",realCheck:" + z);
        if (z) {
            try {
                Object -l_4_R = new File("/system/framework/" + "hwpush.jar");
                if (isInCustDir()) {
                    Log.d("PushLogLightSC2907", "push jarFile is exist in cust");
                } else if (-l_4_R.isFile()) {
                    Log.d("PushLogLightSC2907", "push jarFile is exist");
                } else {
                    Log.i("PushLogLightSC2907", "push jarFile is not exist");
                    if (SystemProperties.getBoolean("ro.config.push_enable", "CN".equals(SystemProperties.get("ro.product.locale.region"))) != 0) {
                        Object -l_7_R = SystemProperties.get("ro.build.version.emui", "-1");
                        if (!TextUtils.isEmpty(-l_7_R)) {
                            if (-l_7_R.contains("2.0") || -l_7_R.contains("2.3")) {
                                Log.d("PushLogLightSC2907", "emui is 2.0 or 2.3");
                            }
                        }
                        Log.d("PushLogLightSC2907", "can not use framework push");
                        return false;
                    }
                    Log.d("PushLogLightSC2907", "framework not support push");
                    return false;
                }
                Object -l_5_R = context.getPackageManager().queryIntentServices(new Intent().setClassName("android", "com.huawei.android.pushagentproxy.PushService"), 128);
                if (-l_5_R == null || -l_5_R.size() == 0) {
                    Log.i("PushLogLightSC2907", "framework push not exist, need vote apk or sdk to support pushservice");
                    return false;
                }
                Log.i("PushLogLightSC2907", "framework push exist, use framework push first");
                return true;
            } catch (Object -l_2_R) {
                Log.e("PushLogLightSC2907", "get Apk version faild ,Exception e= " + -l_2_R.toString());
                return false;
            }
        }
        if (1 == a) {
            z2 = true;
        }
        return z2;
    }

    private void b(Context context, Intent intent) throws UnsupportedEncodingException {
        if (context == null || intent == null) {
            Log.e("PushLogLightSC2907", "context is null");
            return;
        }
        g(context, intent);
        int -l_4_I = new e(context, "push_switch").a("normal_msg_enable");
        Log.d("PushLogLightSC2907", "closePush_Normal:" + -l_4_I);
        if (-l_4_I == 0) {
            Object -l_5_R = intent.getByteArrayExtra(KEY_TYPE.PUSH_BROADCAST_MESSAGE);
            Object -l_6_R = new String(intent.getByteArrayExtra("device_token"), "UTF-8");
            Log.d("PushLogLightSC2907", "PushReceiver receive a message success");
            Object -l_7_R = new Bundle();
            -l_7_R.putString(BOUND_KEY.deviceTokenKey, -l_6_R);
            -l_7_R.putByteArray(BOUND_KEY.pushMsgKey, -l_5_R);
            -l_7_R.putInt(BOUND_KEY.receiveTypeKey, ReceiveType.ReceiveType_Msg.ordinal());
            new EventThread(this, context, -l_7_R).start();
        }
    }

    private void c(Context context, Intent intent) {
        if (context == null || intent == null) {
            Log.e("PushLogLightSC2907", "context is null");
            return;
        }
        Object -l_3_R = intent.getStringExtra(KEY_TYPE.PUSH_KEY_CLICK);
        Object -l_4_R = new Bundle();
        -l_4_R.putString(BOUND_KEY.pushMsgKey, -l_3_R);
        -l_4_R.putInt(BOUND_KEY.receiveTypeKey, ReceiveType.ReceiveType_NotifyClick.ordinal());
        new EventThread(this, context, -l_4_R).start();
    }

    private void d(Context context, Intent intent) {
        if (context == null || intent == null) {
            Log.e("PushLogLightSC2907", "context is null");
            return;
        }
        Object -l_3_R = intent.getStringExtra(KEY_TYPE.PUSH_KEY_CLICK_BTN);
        int -l_4_I = intent.getIntExtra(KEY_TYPE.PUSH_KEY_NOTIFY_ID, 0);
        Object -l_5_R = new Bundle();
        -l_5_R.putString(BOUND_KEY.pushMsgKey, -l_3_R);
        -l_5_R.putInt(BOUND_KEY.pushNotifyId, -l_4_I);
        -l_5_R.putInt(BOUND_KEY.receiveTypeKey, ReceiveType.ReceiveType_ClickBtn.ordinal());
        new EventThread(this, context, -l_5_R).start();
    }

    private void e(Context context, Intent intent) {
        if (context == null || intent == null) {
            Log.e("PushLogLightSC2907", "context is null");
            return;
        }
        int -l_3_I = intent.getBooleanExtra(KEY_TYPE.PUSHSTATE, false);
        Object -l_4_R = new Bundle();
        -l_4_R.putBoolean(BOUND_KEY.pushStateKey, -l_3_I);
        -l_4_R.putInt(BOUND_KEY.receiveTypeKey, ReceiveType.ReceiveType_PushState.ordinal());
        new EventThread(this, context, -l_4_R).start();
    }

    public static final void enableReceiveNormalMsg(Context context, boolean z) {
        boolean z2 = false;
        if (context != null) {
            Object -l_2_R = new e(context, "push_switch");
            String str = "normal_msg_enable";
            if (!z) {
                z2 = true;
            }
            -l_2_R.a(str, z2);
            return;
        }
        Log.d("PushLogLightSC2907", "context is null");
    }

    public static final void enableReceiveNotifyMsg(Context context, boolean z) {
        boolean z2 = false;
        if (context != null) {
            Object -l_2_R = new e(context, "push_switch");
            String str = "notify_msg_enable";
            if (!z) {
                z2 = true;
            }
            -l_2_R.a(str, z2);
            return;
        }
        Log.d("PushLogLightSC2907", "context is null");
    }

    private void f(Context context, Intent intent) {
        Object -l_5_R;
        int -l_4_I = new e(context, "push_switch").a("notify_msg_enable");
        Log.d("PushLogLightSC2907", "closePush_Notify:" + -l_4_I);
        if (-l_4_I == 0) {
            try {
                Log.i("PushLogLightSC2907", "run push selfshow");
                -l_5_R = Class.forName("com.huawei.android.pushselfshow.SelfShowReceiver");
                Object -l_7_R = -l_5_R.getConstructor(new Class[0]).newInstance(new Object[0]);
                -l_5_R.getDeclaredMethod("onReceive", new Class[]{Context.class, Intent.class}).invoke(-l_7_R, new Object[]{context, intent});
            } catch (Object -l_5_R2) {
                Log.e("PushLogLightSC2907", "SelfShowReceiver class not found:" + -l_5_R2.getMessage(), -l_5_R2);
            } catch (Object -l_5_R22) {
                Log.e("PushLogLightSC2907", "onReceive method not found:" + -l_5_R22.getMessage(), -l_5_R22);
            } catch (Object -l_5_R222) {
                Log.e("PushLogLightSC2907", "invokeSelfShow error:" + -l_5_R222.getMessage(), -l_5_R222);
            }
        }
    }

    private void g(Context context, Intent intent) {
        if (context != null && intent != null) {
            Object -l_3_R = intent.getStringExtra("msgIdStr");
            if (!TextUtils.isEmpty(-l_3_R) && isFrameworkPushExist(context)) {
                Object -l_4_R = new Intent("com.huawei.android.push.intent.MSG_RESPONSE");
                -l_4_R.putExtra("msgIdStr", -l_3_R);
                -l_4_R.setPackage("android");
                -l_4_R.setFlags(32);
                Log.d("PushLogLightSC2907", "send msg response broadcast to frameworkPush");
                context.sendBroadcast(-l_4_R);
            }
        }
    }

    public static void getPushState(Context context) {
        Log.d("PushLogLightSC2907", "enter PushEntity:getPushState() pkgName" + context.getPackageName());
        Object -l_1_R = new Intent("com.huawei.android.push.intent.GET_PUSH_STATE");
        -l_1_R.putExtra(KEY_TYPE.PKGNAME, context.getPackageName());
        -l_1_R.setFlags(32);
        if (isFrameworkPushExist(context)) {
            -l_1_R.setPackage("android");
            Log.d("PushLogLightSC2907", "send register broadcast to frameworkPush");
        }
        context.sendOrderedBroadcast(-l_1_R, null);
    }

    public static final void getToken(Context context) {
        Log.d("PushLogLightSC2907", "enter PushEntity:getToken() pkgName" + context.getPackageName());
        Object -l_1_R = new Intent("com.huawei.android.push.intent.REGISTER");
        -l_1_R.putExtra(KEY_TYPE.PKGNAME, context.getPackageName());
        int -l_2_I = a();
        if (-999 != -l_2_I) {
            -l_1_R.putExtra(KEY_TYPE.USERID, String.valueOf(-l_2_I));
        }
        -l_1_R.setFlags(32);
        if (isFrameworkPushExist(context)) {
            -l_1_R.setPackage("android");
            Log.d("PushLogLightSC2907", "send register broadcast to frameworkPush");
        }
        context.sendBroadcast(-l_1_R);
        new e(context, "push_client_self_info").a("hasRequestToken", true);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static synchronized boolean isFrameworkPushExist(Context context) {
        boolean z = false;
        synchronized (PushReceiver.class) {
            Log.d("PushLogLightSC2907", "existFrameworkPush:" + a);
            if (-1 == a) {
                if (a(context, true)) {
                    a = 1;
                } else {
                    a = 0;
                }
                if (1 == a) {
                    z = true;
                }
            } else if (1 == a) {
                z = true;
            }
        }
    }

    public static boolean isInCustDir() {
        try {
            Object -l_0_R = Class.forName("huawei.cust.HwCfgFilePolicy");
            int -l_2_I = ((Integer) -l_0_R.getDeclaredField("CUST_TYPE_CONFIG").get(-l_0_R)).intValue();
            File -l_4_R = (File) -l_0_R.getDeclaredMethod("getCfgFile", new Class[]{String.class, Integer.TYPE}).invoke(-l_0_R, new Object[]{"jars/hwpush.jar", Integer.valueOf(-l_2_I)});
            if (-l_4_R != null && -l_4_R.exists()) {
                Log.i("PushLogLightSC2907", "get push cust File path is " + -l_4_R.getAbsolutePath());
                return true;
            }
        } catch (ClassNotFoundException e) {
            Log.e("PushLogLightSC2907", "HwCfgFilePolicy ClassNotFoundException");
        } catch (Object -l_1_R) {
            Log.e("PushLogLightSC2907", -l_1_R.toString(), -l_1_R);
        } catch (Object -l_1_R2) {
            Log.e("PushLogLightSC2907", -l_1_R2.toString(), -l_1_R2);
        } catch (Object -l_1_R22) {
            Log.e("PushLogLightSC2907", -l_1_R22.toString(), -l_1_R22);
        } catch (Object -l_1_R222) {
            Log.e("PushLogLightSC2907", -l_1_R222.toString(), -l_1_R222);
        } catch (Object -l_1_R2222) {
            Log.e("PushLogLightSC2907", -l_1_R2222.toString(), -l_1_R2222);
        } catch (Object -l_1_R22222) {
            Log.e("PushLogLightSC2907", -l_1_R22222.toString(), -l_1_R22222);
        }
        return false;
    }

    public boolean canExit() {
        return true;
    }

    public void onNotifyBtnClick(Context context, int i, String str, Bundle bundle) {
    }

    public void onNotifyClickMsg(Context context, String str) {
    }

    public void onPluginRsp(Context context, int i, boolean z, Bundle bundle) {
    }

    public abstract void onPushMsg(Context context, byte[] bArr, String str);

    public void onPushState(Context context, boolean z) {
    }

    public final void onReceive(Context context, Intent intent) {
        Object -l_3_R;
        try {
            -l_3_R = new Bundle();
            Log.d("PushLogLightSC2907", "enter PushMsgReceiver:onReceive(Intent:" + intent.getAction() + " pkgName:" + context.getPackageName() + ")");
            Object -l_4_R = intent.getAction();
            if ("com.huawei.android.push.intent.REGISTRATION".equals(-l_4_R) && intent.hasExtra("device_token")) {
                a(context, intent);
                return;
            }
            if (ACTION.ACTION_PUSH_MESSAGE.equals(-l_4_R)) {
                if (intent.hasExtra(KEY_TYPE.PUSH_BROADCAST_MESSAGE)) {
                    b(context, intent);
                    return;
                }
            }
            if (ACTION.ACTION_NOTIFICATION_MSG_CLICK.equals(-l_4_R)) {
                if (intent.hasExtra(KEY_TYPE.PUSH_KEY_CLICK)) {
                    c(context, intent);
                    return;
                }
            }
            if (ACTION.ACTION_NOTIFICATION_MSG_CLICK.equals(-l_4_R) && intent.hasExtra(KEY_TYPE.PUSH_KEY_CLICK_BTN)) {
                d(context, intent);
            } else if ("com.huawei.intent.action.PUSH_STATE".equals(-l_4_R)) {
                e(context, intent);
            } else if ("com.huawei.intent.action.PUSH".equals(-l_4_R) && intent.hasExtra("selfshow_info")) {
                f(context, intent);
            } else {
                Log.w("PushLogLightSC2907", "unknowned message");
            }
        } catch (Object -l_3_R2) {
            Log.e("PushLogLightSC2907", "call onReceive(intent:" + intent + ") cause:" + -l_3_R2.toString(), -l_3_R2);
        } catch (Object -l_3_R22) {
            Log.e("PushLogLightSC2907", "call onReceive(intent:" + intent + ") cause:" + -l_3_R22.toString(), -l_3_R22);
        }
    }

    public abstract void onToken(Context context, String str);

    public void onToken(Context context, String str, Bundle bundle) {
        onToken(context, str);
    }
}
