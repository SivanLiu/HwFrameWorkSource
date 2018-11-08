package tmsdkobf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;

public class it {
    private static final String[] sc = new String[]{"android.intent.action.PHONE_STATE", "android.intent.action.PHONE_STATE_2", "android.intent.action.PHONE_STATE2", "android.intent.action.PHONE_STATE_EXT"};

    public static int a(Context context, Intent intent) {
        Object -l_2_R = intent.getStringExtra("state");
        if (-l_2_R != null) {
            if (-l_2_R.equals("IDLE")) {
                return 0;
            }
            if (-l_2_R.equals("RINGING")) {
                return 1;
            }
            if (-l_2_R.equals("OFFHOOK")) {
                return 2;
            }
        }
        TelephonyManager -l_3_R = (TelephonyManager) context.getSystemService("phone");
        return -l_3_R == null ? 0 : -l_3_R.getCallState();
    }

    public static void a(Context context, BroadcastReceiver broadcastReceiver) {
        Object -l_4_R;
        Object -l_3_R;
        Object -l_2_R = im.rE;
        if (-l_2_R != null) {
            -l_4_R = -l_2_R.io();
            -l_3_R = (-l_4_R == null || -l_4_R.equalsIgnoreCase("android.intent.action.PHONE_STATE")) ? sc : new String[]{"android.intent.action.PHONE_STATE", -l_4_R};
        } else {
            -l_3_R = sc;
        }
        -l_4_R = new IntentFilter();
        -l_4_R.setPriority(Integer.MAX_VALUE);
        -l_4_R.addCategory("android.intent.category.DEFAULT");
        Object -l_5_R = -l_3_R;
        int -l_6_I = -l_3_R.length;
        for (int -l_7_I = 0; -l_7_I < -l_6_I; -l_7_I++) {
            -l_4_R.addAction(-l_5_R[-l_7_I]);
        }
        context.registerReceiver(broadcastReceiver, -l_4_R);
    }
}
