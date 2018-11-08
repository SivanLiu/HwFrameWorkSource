package com.huawei.android.pushselfshow.richpush.tools;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.Toast;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.utils.d;
import java.io.File;

public class a {
    public Resources a;
    public Activity b;
    private com.huawei.android.pushselfshow.c.a c = null;

    public a(Activity activity) {
        this.b = activity;
        this.a = activity.getResources();
    }

    public void a() {
        Object -l_1_R;
        try {
            c.a("PushSelfShowLog", "creat shortcut");
            -l_1_R = new Intent();
            -l_1_R.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            Object -l_2_R = BitmapFactory.decodeResource(this.b.getResources(), d.g(this.b, "hwpush_main_icon"));
            -l_1_R.putExtra("android.intent.extra.shortcut.NAME", this.b.getResources().getString(d.a(this.b, "hwpush_msg_collect")));
            -l_1_R.putExtra("android.intent.extra.shortcut.ICON", -l_2_R);
            -l_1_R.putExtra("duplicate", false);
            Object -l_4_R = new Intent("com.huawei.android.push.intent.RICHPUSH");
            -l_4_R.putExtra("type", "favorite");
            -l_4_R.addFlags(1476395008);
            Object -l_5_R = "com.huawei.android.pushagent";
            if (com.huawei.android.pushselfshow.utils.a.c(this.b, -l_5_R) == 0) {
                -l_4_R.setPackage(this.b.getPackageName());
            } else {
                -l_4_R.setPackage(-l_5_R);
            }
            -l_1_R.putExtra("android.intent.extra.shortcut.INTENT", -l_4_R);
            this.b.sendBroadcast(-l_1_R);
        } catch (Object -l_1_R2) {
            c.e("PushSelfShowLog", "creat shortcut error", -l_1_R2);
        }
    }

    public void a(com.huawei.android.pushselfshow.c.a aVar) {
        this.c = aVar;
    }

    public void b() {
        Object -l_1_R;
        try {
            if (this.c == null || this.c.x() == null) {
                Toast.makeText(this.b, this.b.getResources().getString(d.a(this.b, "hwpush_save_failed")), 0).show();
            }
            c.e("PushSelfShowLog", "the rpl is " + this.c.x());
            -l_1_R = "";
            -l_1_R = !this.c.x().startsWith("file://") ? this.c.x() : this.c.x().substring(7);
            c.e("PushSelfShowLog", "filePath is " + -l_1_R);
            if ("text/html_local".equals(this.c.z())) {
                Object -l_5_R = new File(-l_1_R).getParentFile();
                if (-l_5_R != null && -l_5_R.isDirectory() && this.c.x().contains("richpush")) {
                    String -l_2_R = -l_5_R.getAbsolutePath();
                    String -l_3_R = -l_2_R.replace("richpush", "shotcut");
                    c.b("PushSelfShowLog", "srcDir is %s ,destDir is %s", -l_2_R, -l_3_R);
                    if (com.huawei.android.pushselfshow.utils.a.a(-l_2_R, -l_3_R)) {
                        this.c.d(Uri.fromFile(new File(-l_3_R + File.separator + "index.html")).toString());
                    } else {
                        c.b("PushSelfShowLog", "rich push save failed");
                        return;
                    }
                }
            }
            c.a("PushSelfShowLog", "insert data into db");
            a();
            int -l_2_I = com.huawei.android.pushselfshow.utils.a.d.a(this.b, this.c.o(), this.c);
            c.e("PushSelfShowLog", "insert result is " + -l_2_I);
            if (-l_2_I == 0) {
                c.d("PushSelfShowLog", "save icon fail");
            } else {
                com.huawei.android.pushselfshow.utils.a.a(this.b, "14", this.c, -1);
            }
        } catch (Object -l_1_R2) {
            c.d("PushSelfShowLog", "SaveBtnClickListener error ", -l_1_R2);
        }
    }
}
