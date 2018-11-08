package com.hianalytics.android.a.a;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;

public final class a {
    static final char[] a = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static boolean b = true;
    private static Long c = Long.valueOf(30);
    private static Long d = Long.valueOf(86400);
    private static Long e = Long.valueOf(1000);
    private static Long f = Long.valueOf(1800);
    private static int g = Integer.MAX_VALUE;
    private static HandlerThread h;
    private static HandlerThread i;
    private static Handler j;
    private static Handler k;

    static {
        HandlerThread handlerThread = new HandlerThread("HiAnalytics_messageThread");
        h = handlerThread;
        handlerThread.start();
        handlerThread = new HandlerThread("HiAnalytics_sessionThread");
        i = handlerThread;
        handlerThread.start();
    }

    public static long a(String str) {
        long -l_2_J = 0;
        try {
            Date parse = new SimpleDateFormat("yyyyMMddHHmmss").parse(str);
            if (parse != null) {
                -l_2_J = parse.getTime();
            }
        } catch (ParseException e) {
            e.toString();
        }
        return -l_2_J / 1000;
    }

    public static Long a() {
        return c;
    }

    public static String a(Context context) {
        Object -l_1_R = "";
        try {
            Object -l_2_R = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128);
            if (-l_2_R != null) {
                -l_2_R = -l_2_R.metaData.get("APPKEY");
                if (-l_2_R != null) {
                    -l_1_R = -l_2_R.toString();
                }
            }
        } catch (Exception e) {
        }
        return (-l_1_R == null || -l_1_R.trim().length() == 0) ? context.getPackageName() : -l_1_R;
    }

    public static void a(int i) {
        g = i;
    }

    public static void a(Long l) {
        c = l;
    }

    public static void a(boolean z) {
        b = z;
    }

    public static boolean a(Context context, String str) {
        return context.getPackageManager().checkPermission(str, context.getPackageName()) == 0;
    }

    public static byte[] a(byte[] bArr) {
        Exception e;
        Throwable th;
        byte[] bArr2 = null;
        ByteArrayOutputStream -l_1_R = null;
        DeflaterOutputStream deflaterOutputStream = null;
        try {
            ByteArrayOutputStream -l_1_R2 = new ByteArrayOutputStream();
            try {
                try {
                    DeflaterOutputStream -l_2_R = new DeflaterOutputStream(-l_1_R2);
                    try {
                        -l_2_R.write(bArr);
                        -l_2_R.close();
                        bArr2 = -l_1_R2.toByteArray();
                        try {
                            -l_2_R.close();
                            -l_1_R2.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                        return bArr2;
                    } catch (Exception e3) {
                        e = e3;
                        deflaterOutputStream = -l_2_R;
                        -l_1_R = -l_1_R2;
                        try {
                            e.printStackTrace();
                            if (deflaterOutputStream != null) {
                                try {
                                    deflaterOutputStream.close();
                                    -l_1_R.close();
                                } catch (IOException e22) {
                                    e22.printStackTrace();
                                }
                            }
                            return bArr2;
                        } catch (Throwable th2) {
                            th = th2;
                            if (deflaterOutputStream != null) {
                                try {
                                    deflaterOutputStream.close();
                                    -l_1_R.close();
                                } catch (IOException e4) {
                                    e4.printStackTrace();
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        deflaterOutputStream = -l_2_R;
                        -l_1_R = -l_1_R2;
                        if (deflaterOutputStream != null) {
                            deflaterOutputStream.close();
                            -l_1_R.close();
                        }
                        throw th;
                    }
                } catch (Exception e5) {
                    e = e5;
                    -l_1_R = -l_1_R2;
                    e.printStackTrace();
                    if (deflaterOutputStream != null) {
                        deflaterOutputStream.close();
                        -l_1_R.close();
                    }
                    return bArr2;
                } catch (Throwable th4) {
                    th = th4;
                    -l_1_R = -l_1_R2;
                    if (deflaterOutputStream != null) {
                        deflaterOutputStream.close();
                        -l_1_R.close();
                    }
                    throw th;
                }
            } catch (Exception e6) {
                e = e6;
                -l_1_R = -l_1_R2;
                e.printStackTrace();
                if (deflaterOutputStream != null) {
                    deflaterOutputStream.close();
                    -l_1_R.close();
                }
                return bArr2;
            } catch (Throwable th5) {
                th = th5;
                -l_1_R = -l_1_R2;
                if (deflaterOutputStream != null) {
                    deflaterOutputStream.close();
                    -l_1_R.close();
                }
                throw th;
            }
        } catch (Exception e7) {
            e = e7;
            e.printStackTrace();
            if (deflaterOutputStream != null) {
                deflaterOutputStream.close();
                -l_1_R.close();
            }
            return bArr2;
        }
    }

    public static Long b() {
        return d;
    }

    public static String b(Context context) {
        Object -l_1_R = "Unknown";
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128);
            if (!(applicationInfo == null || applicationInfo.metaData == null)) {
                Object obj = applicationInfo.metaData.get("CHANNEL");
                if (obj != null) {
                    -l_1_R = obj.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -l_1_R;
    }

    public static String b(String str) {
        return (str == null || str.equals("")) ? "000000000000000" : str;
    }

    public static String b(byte[] -l_4_R) {
        if (-l_4_R == null) {
            return null;
        }
        Object -l_1_R = new StringBuilder(-l_4_R.length * 2);
        for (int -l_0_I : -l_4_R) {
            -l_1_R.append(a[(-l_0_I & 240) >> 4]).append(a[-l_0_I & 15]);
        }
        return -l_1_R.toString();
    }

    public static void b(Long l) {
        d = l;
    }

    public static Long c() {
        return f;
    }

    public static void c(Long l) {
        e = l;
    }

    public static String[] c(Context context) {
        Object -l_1_R = new String[]{"Unknown", "Unknown"};
        if (context.getPackageManager().checkPermission("android.permission.ACCESS_NETWORK_STATE", context.getPackageName()) == 0) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
            if (connectivityManager == null) {
                -l_1_R[0] = "Unknown";
                return -l_1_R;
            } else if (connectivityManager.getNetworkInfo(1).getState() != State.CONNECTED) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(0);
                if (networkInfo.getState() != State.CONNECTED) {
                    return -l_1_R;
                }
                -l_1_R[0] = "2G/3G";
                -l_1_R[1] = networkInfo.getSubtypeName();
                return -l_1_R;
            } else {
                -l_1_R[0] = "Wi-Fi";
                return -l_1_R;
            }
        }
        -l_1_R[0] = "Unknown";
        return -l_1_R;
    }

    public static int d() {
        return g;
    }

    public static void d(Long l) {
        f = l;
    }

    public static boolean d(Context context) {
        if (!(e.longValue() >= 0)) {
            return false;
        }
        String packageName = context.getPackageName();
        return !((new File(new StringBuilder("/data/data/").append(packageName).append("/shared_prefs/").append(new StringBuilder("hianalytics_state_").append(packageName).append(".xml").toString()).toString()).length() > e.longValue() ? 1 : (new File(new StringBuilder("/data/data/").append(packageName).append("/shared_prefs/").append(new StringBuilder("hianalytics_state_").append(packageName).append(".xml").toString()).toString()).length() == e.longValue() ? 0 : -1)) <= 0);
    }

    public static String e(Context context) {
        try {
            return String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
        } catch (NameNotFoundException e) {
            return "unknown";
        }
    }

    public static boolean e() {
        return b;
    }

    public static Handler f() {
        if (j == null) {
            Object -l_0_R = h.getLooper();
            if (-l_0_R == null) {
                return null;
            }
            j = new Handler(-l_0_R);
        }
        return j;
    }

    public static boolean f(Context context) {
        SharedPreferences a = c.a(context, "flag");
        Object -l_1_R = Build.DISPLAY;
        String string = a.getString("rom_version", "");
        "currentRom=" + -l_1_R + ",lastRom=" + string;
        return "".equals(string) || !string.equals(-l_1_R);
    }

    public static Handler g() {
        if (k == null) {
            Object -l_0_R = i.getLooper();
            if (-l_0_R == null) {
                return null;
            }
            k = new Handler(-l_0_R);
        }
        return k;
    }

    public static void h() {
    }

    public static String i() {
        Object -l_0_R = "http://data.hicloud.com:8089/sdkv1";
        "URL = " + -l_0_R;
        return -l_0_R;
    }
}
