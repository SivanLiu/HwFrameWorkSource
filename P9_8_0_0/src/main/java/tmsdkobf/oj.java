package tmsdkobf;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import tmsdk.common.TMSDKContext;

public class oj {
    public static PendingIntent a(Context context, String str, long j) {
        Object -l_5_R;
        mb.n("AlarmerUtil", "添加闹钟 : " + str + " " + (j / 1000) + "s");
        Object -l_4_R = null;
        try {
            -l_5_R = new Intent(str);
            -l_5_R.setPackage(TMSDKContext.getApplicaionContext().getPackageName());
            -l_4_R = PendingIntent.getBroadcast(context, 0, -l_5_R, 0);
            ((AlarmManager) context.getSystemService("alarm")).set(0, System.currentTimeMillis() + j, -l_4_R);
            return -l_4_R;
        } catch (Object -l_5_R2) {
            mb.o("AlarmerUtil", "addAlarm: " + -l_5_R2);
            return -l_4_R;
        }
    }

    public static void h(Context context, String str) {
        mb.n("AlarmerUtil", "删除闹钟 : " + str);
        Object -l_2_R;
        try {
            -l_2_R = new Intent(str);
            -l_2_R.setPackage(TMSDKContext.getApplicaionContext().getPackageName());
            ((AlarmManager) context.getSystemService("alarm")).cancel(PendingIntent.getBroadcast(context, 0, -l_2_R, 0));
        } catch (Object -l_2_R2) {
            mb.o("AlarmerUtil", "delAlarm exception: " + -l_2_R2);
        }
    }
}
