package com.huawei.android.pushselfshow.utils;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.TextView;
import com.huawei.android.pushagent.PushReceiver.KEY_TYPE;
import com.huawei.android.pushagent.a.a.a.d;
import com.huawei.android.pushagent.a.a.a.e;
import com.huawei.android.pushagent.a.a.c;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;

public class a {
    private static final char[] a = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static Typeface b = null;

    public static int a(int i, int i2) {
        c.a("PushSelfShowLog", "enter ctrlSockets(cmd:" + i + " param:" + i2 + ")");
        int -l_2_I = -2;
        try {
            -l_2_I = ((Integer) Class.forName("dalvik.system.Zygote").getMethod("ctrlSockets", new Class[]{Integer.TYPE, Integer.TYPE}).invoke(null, new Object[]{Integer.valueOf(i), Integer.valueOf(i2)})).intValue();
        } catch (Object -l_3_R) {
            c.d("PushSelfShowLog", "NoSuchMethodException:" + -l_3_R);
        } catch (Object -l_3_R2) {
            c.d("PushSelfShowLog", "ClassNotFoundException:" + -l_3_R2);
        } catch (Object -l_3_R22) {
            c.d("PushSelfShowLog", "IllegalAccessException:" + -l_3_R22);
        } catch (Object -l_3_R222) {
            c.d("PushSelfShowLog", "InvocationTargetException:" + -l_3_R222);
        } catch (Object -l_3_R2222) {
            c.d("PushSelfShowLog", "RuntimeException:" + -l_3_R2222);
        } catch (Object -l_3_R22222) {
            c.d("PushSelfShowLog", "Exception:" + -l_3_R22222);
        }
        return -l_2_I;
    }

    public static int a(Context context, float f) {
        return (int) ((f * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public static long a() {
        return System.currentTimeMillis();
    }

    public static long a(Context context) {
        c.a("PushSelfShowLog", "enter getVersion()");
        long -l_1_J = -1000;
        Object -l_3_R;
        try {
            -l_3_R = context.getPackageManager().queryBroadcastReceivers(new Intent("com.huawei.android.push.intent.REGISTER").setPackage(context.getPackageName()), 640);
            if (-l_3_R == null || -l_3_R.size() == 0) {
                return -1000;
            }
            -l_1_J = a((ResolveInfo) -l_3_R.get(0), "CS_cloud_version");
            c.a("PushSelfShowLog", "get the version is :" + -l_1_J);
            return -l_1_J;
        } catch (Object -l_3_R2) {
            c.d("PushSelfShowLog", -l_3_R2.toString(), -l_3_R2);
        }
    }

    public static long a(ResolveInfo resolveInfo, String str) {
        long -l_2_J = -1;
        if (resolveInfo == null) {
            return -1;
        }
        try {
            Object -l_4_R = b(resolveInfo, str);
            if (-l_4_R == null || -l_4_R.length() == 0) {
                return -1;
            }
            -l_2_J = Long.parseLong(-l_4_R);
            return -l_2_J;
        } catch (NumberFormatException e) {
            c.b("PushSelfShowLog", str + " is not set in " + a(resolveInfo));
        }
    }

    public static Boolean a(Context context, String str, Intent intent) {
        Object -l_3_R;
        try {
            -l_3_R = context.getPackageManager().queryIntentActivities(intent, 0);
            if (-l_3_R != null) {
                if (-l_3_R.size() > 0) {
                    int -l_4_I = -l_3_R.size();
                    int -l_5_I = 0;
                    while (-l_5_I < -l_4_I) {
                        if (((ResolveInfo) -l_3_R.get(-l_5_I)).activityInfo != null && str.equals(((ResolveInfo) -l_3_R.get(-l_5_I)).activityInfo.applicationInfo.packageName)) {
                            return Boolean.valueOf(true);
                        }
                        -l_5_I++;
                    }
                }
            }
        } catch (Object -l_3_R2) {
            c.d("PushSelfShowLog", -l_3_R2.toString(), -l_3_R2);
        }
        return Boolean.valueOf(false);
    }

    public static String a(Context context, String str) {
        try {
            Object -l_2_R = context.getPackageManager();
            return -l_2_R.getApplicationLabel(-l_2_R.getApplicationInfo(str, 128)).toString();
        } catch (NameNotFoundException e) {
            c.b("PushSelfShowLog", "get the app name of package:" + str + " failed.");
            return null;
        }
    }

    public static String a(ResolveInfo resolveInfo) {
        return resolveInfo.serviceInfo == null ? resolveInfo.activityInfo.packageName : resolveInfo.serviceInfo.packageName;
    }

    public static String a(String str) {
        Object -l_1_R = "";
        Object -l_2_R = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDf5raDExuuXbsVNCWl48yuB89W\rfNOuuhPuS2Mptii/0UorpzypBkNTTGt11E7aorCc1lFwlB+4KDMIpFyQsdChSk+A\rt9UfhFKa95uiDpMe5rMfU+DAhoXGER6WQ2qGtrHmBWVv33i3lc76u9IgEfYuLwC6\r1mhQDHzAKPiViY6oeQIDAQAB\r";
        try {
            -l_1_R = a(e.a(str.getBytes("UTF-8"), "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDf5raDExuuXbsVNCWl48yuB89W\rfNOuuhPuS2Mptii/0UorpzypBkNTTGt11E7aorCc1lFwlB+4KDMIpFyQsdChSk+A\rt9UfhFKa95uiDpMe5rMfU+DAhoXGER6WQ2qGtrHmBWVv33i3lc76u9IgEfYuLwC6\r1mhQDHzAKPiViY6oeQIDAQAB\r"));
        } catch (Object -l_3_R) {
            c.e("PushSelfShowLog", "encrypter error ", -l_3_R);
        }
        return -l_1_R;
    }

    public static String a(byte[] bArr) {
        Object -l_1_R = new StringBuilder(bArr.length);
        for (int -l_2_I = 0; -l_2_I < bArr.length; -l_2_I++) {
            -l_1_R.append(a[(bArr[-l_2_I] >>> 4) & 15]);
            -l_1_R.append(a[bArr[-l_2_I] & 15]);
        }
        return -l_1_R.toString();
    }

    public static void a(Context context, int i) {
        if (context != null) {
            try {
                NotificationManager -l_2_R = (NotificationManager) context.getSystemService("notification");
                if (-l_2_R != null) {
                    -l_2_R.cancel(i);
                }
            } catch (Object -l_2_R2) {
                c.d("PushSelfShowLog", "removeNotifiCationById err:" + -l_2_R2.toString());
            }
            return;
        }
        c.d("PushSelfShowLog", "context is null");
    }

    public static void a(Context context, Intent intent, long j) {
        try {
            c.a("PushSelfShowLog", "enter setAPDelayAlarm(intent:" + intent.toURI() + " interval:" + j + "ms, context:" + context);
            ((AlarmManager) context.getSystemService("alarm")).set(0, System.currentTimeMillis() + j, PendingIntent.getBroadcast(context, new SecureRandom().nextInt(), intent, 0));
        } catch (Throwable -l_4_R) {
            c.a("PushSelfShowLog", "set DelayAlarm error", -l_4_R);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static synchronized void a(Context context, TextView textView) {
        synchronized (a.class) {
            if (context == null || textView == null) {
                c.b("PushSelfShowLog", "context is null or textView is null");
            } else if (com.huawei.android.pushagent.a.a.a.a() >= 10) {
                if (g()) {
                    Object -l_2_R = "chnfzxh";
                    if (com.huawei.android.pushagent.a.a.a.a() >= 11) {
                        -l_2_R = "HwChinese-medium";
                    }
                    if (b == null) {
                        try {
                            b = Typeface.create(-l_2_R, 0);
                        } catch (Object -l_3_R) {
                            c.d("PushSelfShowLog", -l_3_R.toString());
                        }
                    }
                    if (b != null) {
                        c.a("PushSelfShowLog", "setTypeFaceEx success");
                        textView.setTypeface(b);
                    }
                }
            }
        }
    }

    public static void a(Context context, String str, com.huawei.android.pushselfshow.c.a aVar, int i) {
        if (context == null || aVar == null) {
            c.b("PushSelfShowLog", "context or msg is null");
            return;
        }
        if ("com.huawei.android.pushagent".equals(context.getPackageName())) {
            b(context, str, aVar.a(), aVar.m(), aVar.k(), i);
        } else {
            a(context, str, aVar.a(), aVar.m(), aVar.k(), i);
        }
    }

    public static void a(Context context, String str, String str2) {
        Object -l_5_R;
        Object -l_7_R;
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            if (!new File(str2).exists()) {
                inputStream = context.getAssets().open(str);
                FileOutputStream -l_4_R = new FileOutputStream(str2);
                try {
                    -l_5_R = new byte[IncomingSmsFilterConsts.PAY_SMS];
                    while (true) {
                        int -l_6_I = inputStream.read(-l_5_R);
                        if (-l_6_I <= 0) {
                            break;
                        }
                        -l_4_R.write(-l_5_R, 0, -l_6_I);
                    }
                    fileOutputStream = -l_4_R;
                } catch (IOException e) {
                    -l_5_R = e;
                    fileOutputStream = -l_4_R;
                    try {
                        c.e("PushSelfShowLog", "copyAsset ", -l_5_R);
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (Object -l_5_R2) {
                                c.e("PushSelfShowLog", "fos.close() ", -l_5_R2);
                            }
                        }
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Object -l_5_R22) {
                                c.e("PushSelfShowLog", "is.close() ", -l_5_R22);
                                return;
                            }
                        }
                    } catch (Throwable th) {
                        -l_7_R = th;
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (Object -l_8_R) {
                                c.e("PushSelfShowLog", "fos.close() ", -l_8_R);
                            }
                        }
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Object -l_8_R2) {
                                c.e("PushSelfShowLog", "is.close() ", -l_8_R2);
                            }
                        }
                        throw -l_7_R;
                    }
                } catch (Throwable th2) {
                    -l_7_R = th2;
                    fileOutputStream = -l_4_R;
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    throw -l_7_R;
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Object -l_5_R222) {
                    c.e("PushSelfShowLog", "fos.close() ", -l_5_R222);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Object -l_5_R2222) {
                    c.e("PushSelfShowLog", "is.close() ", -l_5_R2222);
                }
            }
        } catch (IOException e2) {
            -l_5_R2222 = e2;
            c.e("PushSelfShowLog", "copyAsset ", -l_5_R2222);
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public static void a(Context context, String str, String str2, String str3, String str4, int i) {
        if (com.huawei.android.pushagent.a.a.a.c() && com.huawei.android.pushagent.a.a.a.d()) {
            c.b("PushSelfShowLog", "enter sendHiAnalytics, eventId is " + str + ",msgid is " + str2 + ",cmd is " + str3);
            new Thread(new b(context, str2, str, str3, str4, i)).start();
            return;
        }
        c.a("PushSelfShowLog", "not EMUI system or not in China, no need report analytics.");
    }

    public static void a(File file) {
        if (file != null) {
            c.a("PushSelfShowLog", "delete file " + file.getAbsolutePath());
            Object -l_1_R = new File(file.getAbsolutePath() + System.currentTimeMillis());
            if (file.renameTo(-l_1_R)) {
                if (!(-l_1_R.isFile() && -l_1_R.delete()) && -l_1_R.isDirectory()) {
                    Object -l_2_R = -l_1_R.listFiles();
                    if (-l_2_R == null || -l_2_R.length == 0) {
                        if (!-l_1_R.delete()) {
                            c.a("PushSelfShowLog", "delete file failed");
                        }
                        return;
                    }
                    for (File a : -l_2_R) {
                        a(a);
                    }
                    if (!-l_1_R.delete()) {
                        c.a("PushSelfShowLog", "delete file unsuccess");
                    }
                }
            }
        }
    }

    public static void a(File file, File file2) {
        Object -l_8_R;
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        Object -l_6_R;
        try {
            FileInputStream -l_2_R = new FileInputStream(file);
            try {
                try {
                    BufferedInputStream -l_3_R = new BufferedInputStream(-l_2_R);
                    try {
                        FileOutputStream -l_4_R = new FileOutputStream(file2);
                        try {
                            try {
                                BufferedOutputStream -l_5_R = new BufferedOutputStream(-l_4_R);
                                try {
                                    -l_6_R = new byte[5120];
                                    while (true) {
                                        int -l_7_I = -l_3_R.read(-l_6_R);
                                        if (-l_7_I == -1) {
                                            break;
                                        }
                                        -l_5_R.write(-l_6_R, 0, -l_7_I);
                                    }
                                    if (-l_3_R != null) {
                                        try {
                                            -l_3_R.close();
                                        } catch (Object -l_6_R2) {
                                            c.e("PushSelfShowLog", "inBuff.close() ", -l_6_R2);
                                        }
                                    }
                                    if (-l_5_R != null) {
                                        try {
                                            -l_5_R.flush();
                                        } catch (Object -l_6_R22) {
                                            c.d("PushSelfShowLog", -l_6_R22.toString(), -l_6_R22);
                                        }
                                        try {
                                            -l_5_R.close();
                                        } catch (Object -l_6_R222) {
                                            c.d("PushSelfShowLog", -l_6_R222.toString(), -l_6_R222);
                                        }
                                    }
                                    if (-l_4_R != null) {
                                        try {
                                            -l_4_R.close();
                                        } catch (Object -l_6_R2222) {
                                            c.e("PushSelfShowLog", "output.close() ", -l_6_R2222);
                                        }
                                    }
                                    if (-l_2_R != null) {
                                        try {
                                            -l_2_R.close();
                                        } catch (Object -l_6_R22222) {
                                            c.e("PushSelfShowLog", "input.close() ", -l_6_R22222);
                                        }
                                    }
                                    bufferedOutputStream = -l_5_R;
                                    fileOutputStream = -l_4_R;
                                    bufferedInputStream = -l_3_R;
                                    fileInputStream = -l_2_R;
                                } catch (IOException e) {
                                    -l_6_R22222 = e;
                                    bufferedOutputStream = -l_5_R;
                                    fileOutputStream = -l_4_R;
                                    bufferedInputStream = -l_3_R;
                                    fileInputStream = -l_2_R;
                                    try {
                                        c.e("PushSelfShowLog", "copyFile ", -l_6_R22222);
                                        if (bufferedInputStream != null) {
                                            try {
                                                bufferedInputStream.close();
                                            } catch (Object -l_6_R222222) {
                                                c.e("PushSelfShowLog", "inBuff.close() ", -l_6_R222222);
                                            }
                                        }
                                        if (bufferedOutputStream != null) {
                                            try {
                                                bufferedOutputStream.flush();
                                            } catch (Object -l_6_R2222222) {
                                                c.d("PushSelfShowLog", -l_6_R2222222.toString(), -l_6_R2222222);
                                            }
                                            try {
                                                bufferedOutputStream.close();
                                            } catch (Object -l_6_R22222222) {
                                                c.d("PushSelfShowLog", -l_6_R22222222.toString(), -l_6_R22222222);
                                            }
                                        }
                                        if (fileOutputStream != null) {
                                            try {
                                                fileOutputStream.close();
                                            } catch (Object -l_6_R222222222) {
                                                c.e("PushSelfShowLog", "output.close() ", -l_6_R222222222);
                                            }
                                        }
                                        if (fileInputStream == null) {
                                            try {
                                                fileInputStream.close();
                                            } catch (Object -l_6_R2222222222) {
                                                c.e("PushSelfShowLog", "input.close() ", -l_6_R2222222222);
                                            }
                                        }
                                    } catch (Throwable th) {
                                        -l_8_R = th;
                                        if (bufferedInputStream != null) {
                                            try {
                                                bufferedInputStream.close();
                                            } catch (Object -l_9_R) {
                                                c.e("PushSelfShowLog", "inBuff.close() ", -l_9_R);
                                            }
                                        }
                                        if (bufferedOutputStream != null) {
                                            try {
                                                bufferedOutputStream.flush();
                                            } catch (Object -l_9_R2) {
                                                c.d("PushSelfShowLog", -l_9_R2.toString(), -l_9_R2);
                                            }
                                            try {
                                                bufferedOutputStream.close();
                                            } catch (Object -l_9_R22) {
                                                c.d("PushSelfShowLog", -l_9_R22.toString(), -l_9_R22);
                                            }
                                        }
                                        if (fileOutputStream != null) {
                                            try {
                                                fileOutputStream.close();
                                            } catch (Object -l_9_R222) {
                                                c.e("PushSelfShowLog", "output.close() ", -l_9_R222);
                                            }
                                        }
                                        if (fileInputStream != null) {
                                            try {
                                                fileInputStream.close();
                                            } catch (Object -l_9_R2222) {
                                                c.e("PushSelfShowLog", "input.close() ", -l_9_R2222);
                                            }
                                        }
                                        throw -l_8_R;
                                    }
                                } catch (Throwable th2) {
                                    -l_8_R = th2;
                                    bufferedOutputStream = -l_5_R;
                                    fileOutputStream = -l_4_R;
                                    bufferedInputStream = -l_3_R;
                                    fileInputStream = -l_2_R;
                                    if (bufferedInputStream != null) {
                                        bufferedInputStream.close();
                                    }
                                    if (bufferedOutputStream != null) {
                                        bufferedOutputStream.flush();
                                        bufferedOutputStream.close();
                                    }
                                    if (fileOutputStream != null) {
                                        fileOutputStream.close();
                                    }
                                    if (fileInputStream != null) {
                                        fileInputStream.close();
                                    }
                                    throw -l_8_R;
                                }
                            } catch (IOException e2) {
                                -l_6_R2222222222 = e2;
                                fileOutputStream = -l_4_R;
                                bufferedInputStream = -l_3_R;
                                fileInputStream = -l_2_R;
                                c.e("PushSelfShowLog", "copyFile ", -l_6_R2222222222);
                                if (bufferedInputStream != null) {
                                    bufferedInputStream.close();
                                }
                                if (bufferedOutputStream != null) {
                                    bufferedOutputStream.flush();
                                    bufferedOutputStream.close();
                                }
                                if (fileOutputStream != null) {
                                    fileOutputStream.close();
                                }
                                if (fileInputStream == null) {
                                    fileInputStream.close();
                                }
                            } catch (Throwable th3) {
                                -l_8_R = th3;
                                fileOutputStream = -l_4_R;
                                bufferedInputStream = -l_3_R;
                                fileInputStream = -l_2_R;
                                if (bufferedInputStream != null) {
                                    bufferedInputStream.close();
                                }
                                if (bufferedOutputStream != null) {
                                    bufferedOutputStream.flush();
                                    bufferedOutputStream.close();
                                }
                                if (fileOutputStream != null) {
                                    fileOutputStream.close();
                                }
                                if (fileInputStream != null) {
                                    fileInputStream.close();
                                }
                                throw -l_8_R;
                            }
                        } catch (IOException e3) {
                            -l_6_R2222222222 = e3;
                            fileOutputStream = -l_4_R;
                            bufferedInputStream = -l_3_R;
                            fileInputStream = -l_2_R;
                            c.e("PushSelfShowLog", "copyFile ", -l_6_R2222222222);
                            if (bufferedInputStream != null) {
                                bufferedInputStream.close();
                            }
                            if (bufferedOutputStream != null) {
                                bufferedOutputStream.flush();
                                bufferedOutputStream.close();
                            }
                            if (fileOutputStream != null) {
                                fileOutputStream.close();
                            }
                            if (fileInputStream == null) {
                                fileInputStream.close();
                            }
                        } catch (Throwable th4) {
                            -l_8_R = th4;
                            fileOutputStream = -l_4_R;
                            bufferedInputStream = -l_3_R;
                            fileInputStream = -l_2_R;
                            if (bufferedInputStream != null) {
                                bufferedInputStream.close();
                            }
                            if (bufferedOutputStream != null) {
                                bufferedOutputStream.flush();
                                bufferedOutputStream.close();
                            }
                            if (fileOutputStream != null) {
                                fileOutputStream.close();
                            }
                            if (fileInputStream != null) {
                                fileInputStream.close();
                            }
                            throw -l_8_R;
                        }
                    } catch (IOException e4) {
                        -l_6_R2222222222 = e4;
                        bufferedInputStream = -l_3_R;
                        fileInputStream = -l_2_R;
                        c.e("PushSelfShowLog", "copyFile ", -l_6_R2222222222);
                        if (bufferedInputStream != null) {
                            bufferedInputStream.close();
                        }
                        if (bufferedOutputStream != null) {
                            bufferedOutputStream.flush();
                            bufferedOutputStream.close();
                        }
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                        if (fileInputStream == null) {
                            fileInputStream.close();
                        }
                    } catch (Throwable th5) {
                        -l_8_R = th5;
                        bufferedInputStream = -l_3_R;
                        fileInputStream = -l_2_R;
                        if (bufferedInputStream != null) {
                            bufferedInputStream.close();
                        }
                        if (bufferedOutputStream != null) {
                            bufferedOutputStream.flush();
                            bufferedOutputStream.close();
                        }
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        throw -l_8_R;
                    }
                } catch (IOException e5) {
                    -l_6_R2222222222 = e5;
                    fileInputStream = -l_2_R;
                    c.e("PushSelfShowLog", "copyFile ", -l_6_R2222222222);
                    if (bufferedInputStream != null) {
                        bufferedInputStream.close();
                    }
                    if (bufferedOutputStream != null) {
                        bufferedOutputStream.flush();
                        bufferedOutputStream.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    if (fileInputStream == null) {
                        fileInputStream.close();
                    }
                } catch (Throwable th6) {
                    -l_8_R = th6;
                    fileInputStream = -l_2_R;
                    if (bufferedInputStream != null) {
                        bufferedInputStream.close();
                    }
                    if (bufferedOutputStream != null) {
                        bufferedOutputStream.flush();
                        bufferedOutputStream.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    throw -l_8_R;
                }
            } catch (IOException e6) {
                -l_6_R2222222222 = e6;
                fileInputStream = -l_2_R;
                c.e("PushSelfShowLog", "copyFile ", -l_6_R2222222222);
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                if (fileInputStream == null) {
                    fileInputStream.close();
                }
            } catch (Throwable th7) {
                -l_8_R = th7;
                fileInputStream = -l_2_R;
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw -l_8_R;
            }
        } catch (IOException e7) {
            -l_6_R2222222222 = e7;
            c.e("PushSelfShowLog", "copyFile ", -l_6_R2222222222);
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (bufferedOutputStream != null) {
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            if (fileInputStream == null) {
                fileInputStream.close();
            }
        }
    }

    public static boolean a(Context context, Intent intent) {
        if (context == null) {
            c.b("PushSelfShowLog", "context is null");
            return false;
        } else if (intent != null) {
            Object -l_3_R = context.getPackageManager().queryIntentActivities(intent, 640);
            if (-l_3_R == null || -l_3_R.size() == 0) {
                c.d("PushSelfShowLog", "no activity exist, may be system Err!! pkgName:");
                return false;
            }
            int -l_4_I = ((ResolveInfo) -l_3_R.get(0)).activityInfo.exported;
            c.b("PushSelfShowLog", "exportedFlag:" + -l_4_I);
            Object -l_5_R = ((ResolveInfo) -l_3_R.get(0)).activityInfo.permission;
            c.b("PushSelfShowLog", "need permission:" + -l_5_R);
            if (-l_4_I != 0) {
                return TextUtils.isEmpty(-l_5_R) || "com.huawei.pushagent.permission.LAUNCH_ACTIVITY".equals(-l_5_R);
            } else {
                return false;
            }
        } else {
            c.b("PushSelfShowLog", "intent is null");
            return false;
        }
    }

    public static boolean a(String str, String str2) {
        try {
            int -l_2_I = new File(str2).mkdirs();
            c.a("PushSelfShowLog", "urlSrc is %s ,urlDest is %s,urlDest is already exist?%s ", str, str2, Boolean.valueOf(-l_2_I));
            Object -l_3_R = new File(str).listFiles();
            if (-l_3_R != null) {
                for (int -l_4_I = 0; -l_4_I < -l_3_R.length; -l_4_I++) {
                    if (-l_3_R[-l_4_I].isFile()) {
                        a(-l_3_R[-l_4_I], new File(str2 + File.separator + -l_3_R[-l_4_I].getName()));
                    }
                    if (-l_3_R[-l_4_I].isDirectory()) {
                        b(str + File.separator + -l_3_R[-l_4_I].getName(), str2 + File.separator + -l_3_R[-l_4_I].getName());
                    }
                }
            }
            return true;
        } catch (Object -l_2_R) {
            c.e("PushSelfShowLog", "fileCopy error ", -l_2_R);
            return false;
        }
    }

    public static long b(String str) {
        if (str == null) {
            str = "";
        }
        Object -l_4_R;
        try {
            -l_4_R = new Date();
            int -l_3_I = (-l_4_R.getHours() * 2) + (-l_4_R.getMinutes() / 30);
            String concat = str.concat(str);
            c.a("PushSelfShowLog", "startIndex is %s ,and ap is %s ,length is %s", Integer.valueOf(-l_3_I), concat, Integer.valueOf(concat.length()));
            int -l_5_I = -l_3_I;
            while (-l_5_I < concat.length()) {
                if (concat.charAt(-l_5_I) == '0') {
                    -l_5_I++;
                } else {
                    long -l_1_J = ((long) (((-l_5_I - -l_3_I) * 30) - (-l_4_R.getMinutes() % 30))) * 60000;
                    c.a("PushSelfShowLog", "startIndex is %s i is %s delay %s", Integer.valueOf(-l_3_I), Integer.valueOf(-l_5_I), Long.valueOf(-l_1_J));
                    if ((-l_1_J < 0 ? 1 : null) != null) {
                        -l_1_J = 0;
                    }
                    return -l_1_J;
                }
            }
        } catch (Object -l_4_R2) {
            c.d("PushSelfShowLog", "error ", -l_4_R2);
        }
        return 0;
    }

    public static Intent b(Context context, String str) {
        Object -l_3_R = null;
        try {
            -l_3_R = context.getPackageManager().getLaunchIntentForPackage(str);
        } catch (Throwable -l_4_R) {
            c.b("PushSelfShowLog", -l_4_R.toString(), -l_4_R);
        }
        return -l_3_R;
    }

    public static String b(Context context) {
        Object -l_1_R = "";
        try {
            TelephonyManager -l_2_R = (TelephonyManager) context.getSystemService("phone");
            if (-l_2_R != null) {
                -l_1_R = -l_2_R.getDeviceId();
            }
        } catch (Object -l_2_R2) {
            c.d("PushSelfShowLog", -l_2_R2.toString());
        }
        return -l_1_R;
    }

    private static String b(ResolveInfo resolveInfo, String str) {
        Object -l_2_R = resolveInfo.serviceInfo == null ? resolveInfo.activityInfo.metaData : resolveInfo.serviceInfo.metaData;
        return -l_2_R != null ? -l_2_R.getString(str) : null;
    }

    public static void b(Context context, Intent intent) {
        try {
            AlarmManager -l_2_R = (AlarmManager) context.getSystemService("alarm");
            int -l_3_I = 0;
            if (intent.hasExtra("selfshow_notify_id")) {
                -l_3_I = intent.getIntExtra("selfshow_notify_id", 0) + 3;
            }
            c.a("PushSelfShowLog", "setDelayAlarm(cancel) alarmNotityId " + -l_3_I + " and intent is " + intent.toURI());
            Object -l_4_R = new Intent("com.huawei.intent.action.PUSH");
            -l_4_R.setPackage(context.getPackageName()).setFlags(32);
            Object -l_5_R = PendingIntent.getBroadcast(context, -l_3_I, -l_4_R, 536870912);
            if (-l_5_R == null) {
                c.a("PushSelfShowLog", "alarm not exist");
                return;
            }
            c.a("PushSelfShowLog", "  alarm cancel");
            -l_2_R.cancel(-l_5_R);
        } catch (Object -l_2_R2) {
            c.d("PushSelfShowLog", "cancelAlarm err:" + -l_2_R2.toString());
        }
    }

    public static void b(Context context, String str, String str2, String str3, String str4, int i) {
        Object -l_7_R;
        c.b("PushSelfShowLog", "enter bdReport, cmd =" + str3 + ", msgid = " + str2 + ", eventId = " + str + ",notifyId= " + i);
        if (context == null) {
            c.d("PushSelfShowLog", "context is null");
        } else if (c(str)) {
            try {
                if ("-1".equals(str)) {
                    str = "101";
                } else if ("0".equals(str)) {
                    str = "100";
                }
                int -l_6_I = Integer.parseInt(str);
                -l_7_R = new JSONObject();
                -l_7_R.put("msgId", str2);
                -l_7_R.put("version", "2907");
                -l_7_R.put("cmd", str3);
                -l_7_R.put("pkg", str4);
                -l_7_R.put(KEY_TYPE.PUSH_KEY_NOTIFY_ID, i);
                Object -l_8_R = Class.forName("com.huawei.bd.Reporter");
                -l_8_R.getMethod("j", new Class[]{Context.class, Integer.TYPE, JSONObject.class}).invoke(-l_8_R, new Object[]{context, Integer.valueOf(-l_6_I), -l_7_R});
                c.b("PushSelfShowLog", "bd success");
            } catch (Object -l_7_R2) {
                c.d("PushSelfShowLog", -l_7_R2.toString(), -l_7_R2);
            } catch (Object -l_7_R22) {
                c.d("PushSelfShowLog", -l_7_R22.toString());
            } catch (Object -l_7_R222) {
                c.d("PushSelfShowLog", -l_7_R222.toString(), -l_7_R222);
            } catch (Object -l_7_R2222) {
                c.d("PushSelfShowLog", -l_7_R2222.toString(), -l_7_R2222);
            } catch (Object -l_7_R22222) {
                c.d("PushSelfShowLog", -l_7_R22222.toString(), -l_7_R22222);
            } catch (Object -l_7_R222222) {
                c.d("PushSelfShowLog", -l_7_R222222.toString(), -l_7_R222222);
            } catch (Object -l_7_R2222222) {
                c.d("PushSelfShowLog", -l_7_R2222222.toString(), -l_7_R2222222);
            } catch (Object -l_7_R22222222) {
                c.d("PushSelfShowLog", -l_7_R22222222.toString(), -l_7_R22222222);
            }
        } else {
            c.b("PushSelfShowLog", str + " need not bdreport");
        }
    }

    public static void b(File file) {
        c.a("PushSelfShowLog", "delete file before ");
        if (file != null && file.exists()) {
            Object -l_1_R = file.listFiles();
            if (-l_1_R != null && -l_1_R.length != 0) {
                long -l_2_J = System.currentTimeMillis();
                for (File -l_5_R : -l_1_R) {
                    try {
                        if ((-l_2_J - -l_5_R.lastModified() <= 86400000 ? 1 : null) == null) {
                            c.e("PushSelfShowLog", "delete file before " + -l_5_R.getAbsolutePath());
                            a(-l_5_R);
                        }
                    } catch (Object -l_5_R2) {
                        c.e("PushSelfShowLog", -l_5_R2.toString());
                    }
                }
            }
        }
    }

    private static void b(String str, String str2) throws IOException {
        if (new File(str2).mkdirs()) {
            c.e("PushSelfShowLog", "mkdir");
        }
        Object -l_2_R = new File(str).listFiles();
        if (-l_2_R != null) {
            for (int -l_3_I = 0; -l_3_I < -l_2_R.length; -l_3_I++) {
                if (-l_2_R[-l_3_I].isFile()) {
                    a(-l_2_R[-l_3_I], new File(new File(str2).getAbsolutePath() + File.separator + -l_2_R[-l_3_I].getName()));
                }
                if (-l_2_R[-l_3_I].isDirectory()) {
                    b(str + "/" + -l_2_R[-l_3_I].getName(), str2 + "/" + -l_2_R[-l_3_I].getName());
                }
            }
        }
    }

    public static boolean b() {
        return VERSION.SDK_INT >= 11;
    }

    public static ArrayList c(Context context) {
        Object -l_1_R = new ArrayList();
        Object -l_2_R = new Intent("android.intent.action.VIEW");
        -l_2_R.setData(Uri.parse("market://details?id="));
        Object -l_3_R = context.getPackageManager().queryIntentActivities(-l_2_R, 0);
        if (!(-l_3_R == null || -l_3_R.size() == 0)) {
            int -l_4_I = -l_3_R.size();
            for (int -l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
                if (((ResolveInfo) -l_3_R.get(-l_5_I)).activityInfo != null) {
                    -l_1_R.add(((ResolveInfo) -l_3_R.get(-l_5_I)).activityInfo.applicationInfo.packageName);
                }
            }
        }
        return -l_1_R;
    }

    public static boolean c() {
        return VERSION.SDK_INT >= 16;
    }

    public static boolean c(Context context, String str) {
        if (context == null || str == null || "".equals(str)) {
            return false;
        }
        try {
            if (context.getPackageManager().getApplicationInfo(str, 8192) == null) {
                return false;
            }
            c.a("PushSelfShowLog", str + " is installed");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean c(String str) {
        if (TextUtils.isEmpty(str)) {
            c.b("PushSelfShowLog", "eventId is empty");
            return false;
        }
        Object -l_1_R = new ArrayList();
        -l_1_R.add("-1");
        -l_1_R.add("0");
        -l_1_R.add("1");
        -l_1_R.add("2");
        -l_1_R.add("14");
        -l_1_R.add("17");
        return -l_1_R.contains(str);
    }

    public static String d(Context context, String str) {
        Object -l_3_R;
        Object -l_2_R = "";
        try {
            -l_3_R = "";
            -l_2_R = ((Environment.getExternalStorageState().equals("mounted") != 0 ? Environment.getExternalStorageDirectory().getPath() : context.getFilesDir().getPath()) + File.separator + "PushService") + File.separator + str;
            c.a("PushSelfShowLog", "dbPath is " + -l_2_R);
            return -l_2_R;
        } catch (Object -l_3_R2) {
            c.e("PushSelfShowLog", "getDbPath error", -l_3_R2);
            return -l_2_R;
        }
    }

    public static boolean d() {
        return com.huawei.android.pushagent.a.a.a.a() >= 9;
    }

    public static boolean d(Context context) {
        Object -l_1_R = new Intent("android.intent.action.SENDTO");
        -l_1_R.setPackage("com.android.email");
        -l_1_R.setData(Uri.fromParts("mailto", "xxxx@xxxx.com", null));
        Object -l_2_R = context.getPackageManager().queryIntentActivities(-l_1_R, 0);
        return (-l_2_R == null || -l_2_R.size() == 0) ? false : true;
    }

    public static final String e() {
        return com.huawei.android.pushagent.a.a.a.c.a(String.valueOf("com.huawei.android.pushagent".hashCode()));
    }

    public static void e(Context context, String str) {
        if (TextUtils.isEmpty(str)) {
            c.b("PushSelfShowLog", "url is null.");
            return;
        }
        Object -l_2_R;
        try {
            Object -l_6_R;
            -l_2_R = new Intent("android.intent.action.VIEW");
            -l_2_R.setData(Uri.parse(str));
            -l_2_R.setFlags(402653184);
            Object<ResolveInfo> -l_4_R = context.getPackageManager().queryIntentActivities(-l_2_R, 0);
            Object -l_5_R = null;
            for (ResolveInfo -l_7_R : -l_4_R) {
                Object -l_8_R = -l_7_R.activityInfo.packageName;
                if (f(context, -l_8_R) != 0) {
                    -l_5_R = -l_8_R;
                    break;
                }
            }
            if (-l_5_R == null) {
                -l_6_R = "com.android.browser";
                for (ResolveInfo -l_8_R2 : -l_4_R) {
                    Object -l_9_R = -l_8_R2.activityInfo.packageName;
                    if (-l_6_R.equalsIgnoreCase(-l_9_R)) {
                        -l_5_R = -l_9_R;
                        break;
                    }
                }
            }
            if (-l_5_R != null) {
                -l_2_R.setPackage(-l_5_R);
            }
            context.startActivity(-l_2_R);
        } catch (Object -l_2_R2) {
            c.d("PushSelfShowLog", "start browser activity failed, exception:" + -l_2_R2.getMessage());
        }
    }

    public static boolean e(Context context) {
        return "com.huawei.android.pushagent".equals(context.getPackageName());
    }

    public static final String f() {
        return com.huawei.android.pushagent.a.a.a.c.a(String.valueOf("com.huawei.hwid".hashCode()));
    }

    public static boolean f(Context context) {
        return "com.huawei.hwid".equals(context.getPackageName());
    }

    public static boolean f(Context context, String str) {
        Object -l_2_R = new ArrayList();
        Object -l_3_R = new ArrayList();
        context.getPackageManager().getPreferredActivities(-l_2_R, -l_3_R, str);
        return -l_3_R != null && -l_3_R.size() > 0;
    }

    private static boolean g() {
        return "zh".equals(Locale.getDefault().getLanguage());
    }

    public static boolean g(Context context) {
        boolean z = false;
        try {
            if (context.getPackageManager().getApplicationInfo("com.huawei.android.pushagent", 128) != null) {
                z = true;
            }
            return z;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public static boolean h(Context context) {
        int -l_4_I = 0;
        Cursor cursor = null;
        try {
            Object -l_2_R = context.getContentResolver().query(com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider.a.a, null, null, null, null);
            if (-l_2_R != null && -l_2_R.moveToFirst()) {
                int -l_3_I = -l_2_R.getInt(-l_2_R.getColumnIndex("isSupport"));
                c.a("PushSelfShowLog", "isExistProvider:" + -l_3_I);
                if (1 == -l_3_I) {
                    -l_4_I = 1;
                }
                if (-l_2_R != null) {
                    -l_2_R.close();
                }
                return -l_4_I;
            }
            if (-l_2_R != null) {
                -l_2_R.close();
            }
            return false;
        } catch (Throwable -l_3_R) {
            c.a("PushSelfShowLog", -l_3_R.toString(), -l_3_R);
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static int i(Context context) {
        if (context == null) {
            return 3;
        }
        return (VERSION.SDK_INT >= 16 && context.getResources().getIdentifier("androidhwext:style/Theme.Emui", null, null) != 0) ? 0 : 3;
    }

    public static int j(Context context) {
        try {
            Object -l_1_R = Class.forName("com.huawei.android.immersion.ImmersionStyle");
            int -l_3_I = ((Integer) -l_1_R.getDeclaredMethod("getPrimaryColor", new Class[]{Context.class}).invoke(-l_1_R, new Object[]{context})).intValue();
            c.b("PushSelfShowLog", "colorPrimary:" + -l_3_I);
            return -l_3_I;
        } catch (ClassNotFoundException e) {
            c.d("PushSelfShowLog", "ImmersionStyle ClassNotFoundException");
            return 0;
        } catch (Object -l_2_R) {
            c.d("PushSelfShowLog", -l_2_R.toString(), -l_2_R);
            return 0;
        } catch (Object -l_2_R2) {
            c.d("PushSelfShowLog", -l_2_R2.toString(), -l_2_R2);
            return 0;
        } catch (Object -l_2_R22) {
            c.d("PushSelfShowLog", -l_2_R22.toString(), -l_2_R22);
            return 0;
        } catch (Object -l_2_R222) {
            c.d("PushSelfShowLog", -l_2_R222.toString(), -l_2_R222);
            return 0;
        } catch (Object -l_2_R2222) {
            c.d("PushSelfShowLog", -l_2_R2222.toString(), -l_2_R2222);
            return 0;
        }
    }

    public static int k(Context context) {
        int -l_1_I = -1;
        try {
            Object -l_2_R = Class.forName("com.huawei.android.immersion.ImmersionStyle");
            int -l_4_I = ((Integer) -l_2_R.getDeclaredMethod("getPrimaryColor", new Class[]{Context.class}).invoke(-l_2_R, new Object[]{context})).intValue();
            -l_1_I = ((Integer) -l_2_R.getDeclaredMethod("getSuggestionForgroundColorStyle", new Class[]{Integer.TYPE}).invoke(-l_2_R, new Object[]{Integer.valueOf(-l_4_I)})).intValue();
            c.b("PushSelfShowLog", "getSuggestionForgroundColorStyle:" + -l_1_I);
            return -l_1_I;
        } catch (ClassNotFoundException e) {
            c.d("PushSelfShowLog", "ImmersionStyle ClassNotFoundException");
            return -l_1_I;
        } catch (Object -l_3_R) {
            c.d("PushSelfShowLog", -l_3_R.toString(), -l_3_R);
            return -l_1_I;
        } catch (Object -l_3_R2) {
            c.d("PushSelfShowLog", -l_3_R2.toString(), -l_3_R2);
            return -l_1_I;
        } catch (Object -l_3_R22) {
            c.d("PushSelfShowLog", -l_3_R22.toString(), -l_3_R22);
            return -l_1_I;
        } catch (Object -l_3_R222) {
            c.d("PushSelfShowLog", -l_3_R222.toString(), -l_3_R222);
            return -l_1_I;
        } catch (Object -l_3_R2222) {
            c.d("PushSelfShowLog", -l_3_R2222.toString(), -l_3_R2222);
            return -l_1_I;
        }
    }

    public static String l(Context context) {
        Object -l_1_R = context.getExternalCacheDir();
        if (-l_1_R != null) {
            return -l_1_R.getPath();
        }
        return Environment.getExternalStorageDirectory().getPath() + ("/Android/data/" + context.getPackageName() + "/cache");
    }

    public static String m(Context context) {
        Object -l_1_R = "";
        Object -l_2_R = "";
        Object -l_3_R = context.getPackageName();
        -l_2_R = !"com.huawei.android.pushagent".equals(-l_3_R) ? !"com.huawei.hwid".equals(-l_3_R) ? d.b(context, new com.huawei.android.pushagent.a.a.e(context, "push_client_self_info").b("push_notify_key")) : f() : e();
        try {
            -l_1_R = d.a(context, -l_3_R, -l_2_R.getBytes("UTF-8"));
        } catch (Object -l_4_R) {
            c.d("PushSelfShowLog", -l_4_R.toString());
        }
        return -l_1_R;
    }

    private static boolean o(Context context) {
        boolean z = false;
        if (context == null) {
            return false;
        }
        int i = -1;
        try {
            i = Secure.getInt(context.getContentResolver(), "user_experience_involved", -1);
            c.a("PushSelfShowLog", "settingMainSwitch:" + i);
        } catch (Object -l_2_R) {
            c.d("PushSelfShowLog", -l_2_R.toString(), -l_2_R);
        }
        if (i == 1) {
            z = true;
        }
        return z;
    }
}
