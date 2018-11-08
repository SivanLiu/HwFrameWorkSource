package com.huawei.android.pushselfshow.permission;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import com.huawei.android.pushagent.a.a.a;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.SelfShowReceiver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequestPermissionsActivity extends Activity {
    private static final String[] a = new String[]{"android.permission.READ_PHONE_STATE"};
    private static List c = new ArrayList();
    private static boolean d = false;
    private String[] b = new String[0];

    public static void a(Context context, Intent intent) {
        c.b("PushSelfShowLog", "enter startPermissionActivity");
        if (context != null) {
            Object -l_2_R = new Intent(context.getApplicationContext(), RequestPermissionsActivity.class);
            -l_2_R.addFlags(276824064);
            if (intent != null) {
                -l_2_R.putExtra("previous_intent", intent);
            }
            try {
                context.startActivity(-l_2_R);
            } catch (Object -l_3_R) {
                c.d("PushSelfShowLog", -l_3_R.toString(), -l_3_R);
            }
        }
    }

    private static void a(boolean z) {
        d = z;
    }

    public static boolean a(Context context) {
        if (VERSION.SDK_INT < 23) {
            return false;
        }
        if (!a.a(context, context.getPackageName()) || !a.e()) {
            return !a(context, a);
        } else {
            c.a("PushSelfShowLog", "It is system app, no need request permission.");
            return false;
        }
    }

    protected static boolean a(Context context, String[] -l_2_R) {
        if (context == null || -l_2_R == null || -l_2_R.length == 0) {
            return false;
        }
        int -l_3_I = -l_2_R.length;
        int -l_4_I = 0;
        while (-l_4_I < -l_3_I) {
            Object -l_5_R = -l_2_R[-l_4_I];
            if (context.checkSelfPermission(-l_5_R) == 0) {
                -l_4_I++;
            } else {
                c.a("PushSelfShowLog", -l_5_R + " need request");
                return false;
            }
        }
        return true;
    }

    private boolean a(String str) {
        return Arrays.asList(a()).contains(str);
    }

    private boolean a(String[] strArr) {
        int -l_2_I = 0;
        while (-l_2_I < strArr.length) {
            if (checkSelfPermission(strArr[-l_2_I]) != 0 && a(strArr[-l_2_I])) {
                c.b("PushSelfShowLog", "request permissions failed:" + strArr[-l_2_I]);
                return false;
            }
            -l_2_I++;
        }
        c.b("PushSelfShowLog", "request all permissions success:");
        return true;
    }

    private boolean a(String[] strArr, int[] iArr) {
        int -l_3_I = 0;
        while (-l_3_I < strArr.length) {
            if (iArr[-l_3_I] != 0 && a(strArr[-l_3_I])) {
                c.a("PushSelfShowLog", "request permissions failed:" + strArr[-l_3_I]);
                return false;
            }
            -l_3_I++;
        }
        c.a("PushSelfShowLog", "request all permissions success:");
        return true;
    }

    private void b(String[] strArr) {
        if (d) {
            c.b("PushSelfShowLog", "has Start PermissionActivity, do nothing");
            finish();
            return;
        }
        a(true);
        try {
            Intent -l_2_R = new Intent("huawei.intent.action.REQUEST_PERMISSIONS");
            -l_2_R.setPackage("com.huawei.systemmanager");
            -l_2_R.putExtra("KEY_HW_PERMISSION_ARRAY", strArr);
            -l_2_R.putExtra("KEY_HW_PERMISSION_PKG", getPackageName());
            if (com.huawei.android.pushselfshow.utils.a.a((Context) this, "com.huawei.systemmanager", -l_2_R).booleanValue()) {
                try {
                    c.b("PushSelfShowLog", "checkAndRequestPermission: systemmanager permission activity is exist");
                    startActivityForResult(-l_2_R, 1357);
                } catch (Object -l_3_R) {
                    c.d("PushSelfShowLog", "checkAndRequestPermission: Exception", -l_3_R);
                    requestPermissions(strArr, 1357);
                }
            } else {
                c.b("PushSelfShowLog", "checkAndRequestPermission: systemmanager permission activity is not exist");
                requestPermissions(strArr, 1357);
            }
        } catch (Object -l_2_R2) {
            c.d("PushSelfShowLog", -l_2_R2.toString(), -l_2_R2);
        }
    }

    private void c() {
        Object -l_1_R = new ArrayList();
        for (Object -l_5_R : b()) {
            if (checkSelfPermission(-l_5_R) != 0) {
                -l_1_R.add(-l_5_R);
            }
        }
        if (-l_1_R.size() == 0) {
            c.a("PushSelfShowLog", "unsatisfiedPermissions size is 0, finish");
            finish();
            return;
        }
        this.b = (String[]) -l_1_R.toArray(new String[-l_1_R.size()]);
        b(this.b);
    }

    protected String[] a() {
        return a;
    }

    protected String[] b() {
        return a;
    }

    protected void onActivityResult(int i, int i2, Intent intent) {
        if (1357 == i) {
            if (i2 == 0) {
                c.b("PushSelfShowLog", "onActivityResult: RESULT_CANCELED");
            } else if (-1 == i2) {
                c.b("PushSelfShowLog", "onActivityResult: RESULT_OK");
                if (!(this.b == null || this.b.length == 0 || !a(this.b))) {
                    c.b("PushSelfShowLog", "onActivityResult: Permission is granted");
                    c.b("PushSelfShowLog", "mCacheIntents size: " + c.size());
                    for (Intent -l_5_R : c) {
                        new SelfShowReceiver().onReceive(this, -l_5_R);
                    }
                }
            }
        }
        try {
            a(true);
            c.clear();
            finish();
        } catch (Object -l_4_R) {
            c.d("PushSelfShowLog", -l_4_R.toString(), -l_4_R);
        }
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        c.a((Context) this);
        c.b("PushSelfShowLog", "enter RequestPermissionsActivity onCreate");
        requestWindowFeature(1);
        Object -l_2_R = getIntent();
        if (-l_2_R == null) {
            c.b("PushSelfShowLog", "enter RequestPermissionsActivity onCreate, intent is null, finish");
            finish();
        } else if (VERSION.SDK_INT >= 23) {
            Object -l_3_R;
            try {
                -l_3_R = -l_2_R.getExtras();
                if (-l_3_R != null) {
                    Intent -l_4_R = (Intent) -l_3_R.get("previous_intent");
                    c.a("PushSelfShowLog", "mCacheIntents size is " + c.size());
                    if (c.size() >= 30) {
                        c.remove(0);
                    }
                    c.add(-l_4_R);
                }
            } catch (Object -l_3_R2) {
                c.d("PushSelfShowLog", -l_3_R2.toString(), -l_3_R2);
            }
            c.a("PushSelfShowLog", "savedInstanceState is " + bundle);
            if (bundle == null) {
                c();
            }
        } else {
            c.b("PushSelfShowLog", "enter RequestPermissionsActivity onCreate, SDK version is less than 23, finish");
            finish();
        }
    }

    protected void onDestroy() {
        c.a("PushSelfShowLog", "enter RequestPermissionsActivity onDestroy");
        super.onDestroy();
    }

    protected void onNewIntent(Intent intent) {
        c.a("PushSelfShowLog", "enter RequestPermissionsActivity onNewIntent");
        super.onNewIntent(intent);
    }

    protected void onPause() {
        c.a("PushSelfShowLog", "RequestPermissionsActivity onPause");
        super.onPause();
    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        c.b("PushSelfShowLog", "RequestPermissionsActivity onRequestPermissionsResult");
        if (1357 == i && strArr != null && strArr.length > 0 && iArr != null && iArr.length > 0 && a(strArr, iArr)) {
            for (Intent -l_5_R : c) {
                new SelfShowReceiver().onReceive(this, -l_5_R);
            }
        }
        a(false);
        c.clear();
        finish();
    }

    protected void onStop() {
        c.a("PushSelfShowLog", "RequestPermissionsActivity onStop");
        super.onStop();
    }
}
