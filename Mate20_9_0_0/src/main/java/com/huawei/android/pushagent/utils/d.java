package com.huawei.android.pushagent.utils;

import android.app.AppGlobals;
import android.app.INotificationManager.Stub;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.f;
import com.huawei.android.pushagent.utils.a.g;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.b.b;
import java.io.FileDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class d {
    public static byte[] zi(int i) {
        return new byte[]{(byte) ((i >> 24) & 255), (byte) ((i >> 16) & 255), (byte) ((i >> 8) & 255), (byte) (i & 255)};
    }

    public static int yg(byte[] bArr) {
        return ((bArr[0] & 255) << 8) | (bArr[1] & 255);
    }

    public static int zs(byte b) {
        return b & 255;
    }

    public static byte[] zr(int i) {
        return new byte[]{(byte) ((i >> 8) & 255), (byte) (i & 255)};
    }

    public static String yq(Context context) {
        String yz = yz(context);
        if (!TextUtils.isEmpty(yz)) {
            return yz;
        }
        g rz = f.rz(context, com.huawei.android.pushagent.model.prefs.g.kp(context).kq());
        yz = rz.rx();
        int deviceIdType = rz.getDeviceIdType();
        com.huawei.android.pushagent.model.prefs.g.kp(context).kr(yz);
        com.huawei.android.pushagent.model.prefs.g.kp(context).setDeviceIdType(deviceIdType);
        return yz;
    }

    public static int za(Context context) {
        if (-1 == com.huawei.android.pushagent.model.prefs.g.kp(context).getDeviceIdType()) {
            yq(context);
        }
        return com.huawei.android.pushagent.model.prefs.g.kp(context).getDeviceIdType();
    }

    public static String yv(Context context) {
        return String.valueOf(3414);
    }

    public static String yi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
        if (wifiManager == null) {
            return "";
        }
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo.getBSSID() != null) {
            return connectionInfo.getBSSID();
        }
        return "";
    }

    public static boolean zk(Context context, String str) {
        List zp = zp(context.getPackageManager(), new Intent("com.huawei.android.push.intent.RECEIVE").setPackage(str), 787072, a.xy());
        if (zp == null || zp.size() == 0) {
            a.st("PushLog3414", "isClientSupportMsgResponse: false");
            return false;
        }
        String str2;
        if (((ResolveInfo) zp.get(0)).serviceInfo != null) {
            str2 = ((ResolveInfo) zp.get(0)).serviceInfo.packageName;
        } else {
            str2 = ((ResolveInfo) zp.get(0)).activityInfo.packageName;
        }
        if (str2 != null && str2.equals(str)) {
            str2 = zg((ResolveInfo) zp.get(0), "CS_cloud_ablitity");
            if (TextUtils.isEmpty(str2)) {
                return false;
            }
            try {
                boolean contains = Arrays.asList(str2.split("\\|")).contains("successRateAnalytics");
                a.st("PushLog3414", "isClientSupportMsgResponse:" + contains);
                return contains;
            } catch (Exception e) {
                a.su("PushLog3414", e.toString());
            }
        }
        return false;
    }

    public static String zg(ResolveInfo resolveInfo, String str) {
        Bundle bundle = resolveInfo.serviceInfo != null ? resolveInfo.serviceInfo.metaData : resolveInfo.activityInfo.metaData;
        if (bundle == null) {
            return null;
        }
        return bundle.getString(str);
    }

    public static String yj(Context context) {
        String simOperator = ((TelephonyManager) context.getSystemService("phone")).getSimOperator();
        if (simOperator == null) {
            return "";
        }
        char[] toCharArray = simOperator.toCharArray();
        int i = 0;
        while (i < toCharArray.length) {
            if (toCharArray[i] < '0' || toCharArray[i] > '9') {
                return simOperator.substring(0, i);
            }
            i++;
        }
        return simOperator;
    }

    public static int yh(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (connectivityManager == null) {
            return -1;
        }
        NetworkInfo[] allNetworkInfo = connectivityManager.getAllNetworkInfo();
        if (allNetworkInfo == null) {
            return -1;
        }
        for (int i = 0; i < allNetworkInfo.length; i++) {
            if (allNetworkInfo[i].getState() == State.CONNECTED) {
                return allNetworkInfo[i].getType();
            }
        }
        return -1;
    }

    public static boolean yp(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null) {
            return false;
        }
        if (activeNetworkInfo.getState() == State.CONNECTED || activeNetworkInfo.getState() == State.CONNECTING) {
            return true;
        }
        return false;
    }

    public static void zo() {
        a.st("PushLog3414", "enter powerLow");
        try {
            Class.forName("com.huawei.pgmng.log.LogPower").getMethod("push", new Class[]{Integer.TYPE}).invoke(null, new Object[]{Integer.valueOf(119)});
        } catch (ClassNotFoundException e) {
            a.st("PushLog3414", "ClassNotFoundException, not support LogPower");
        } catch (NoSuchMethodException e2) {
            a.st("PushLog3414", "NoSuchMethodException, not support LogPower");
        } catch (IllegalArgumentException e3) {
            a.st("PushLog3414", "IllegalArgumentException, not support LogPower");
        } catch (IllegalAccessException e4) {
            a.st("PushLog3414", "IllegalAccessException, not support LogPower");
        } catch (InvocationTargetException e5) {
            a.st("PushLog3414", "InvocationTargetException, not support LogPower");
        }
    }

    public static int ym(int i, int i2) {
        a.sv("PushLog3414", "enter ctrlSockets(cmd:" + i + " param:" + i2 + ")");
        try {
            return ((Integer) Class.forName("dalvik.system.Zygote").getMethod("ctrlSockets", new Class[]{Integer.TYPE, Integer.TYPE}).invoke(null, new Object[]{Integer.valueOf(i), Integer.valueOf(i2)})).intValue();
        } catch (ClassNotFoundException e) {
            a.st("PushLog3414", "There is no method of ctrlSockets.");
            return -2;
        } catch (NoSuchMethodException e2) {
            a.st("PushLog3414", "There is no method of ctrlSockets.");
            return -2;
        } catch (IllegalArgumentException e3) {
            a.st("PushLog3414", "There is no method of ctrlSockets.");
            return -2;
        } catch (IllegalAccessException e4) {
            a.st("PushLog3414", "There is no method of ctrlSockets.");
            return -2;
        } catch (InvocationTargetException e5) {
            a.st("PushLog3414", "There is no method of ctrlSockets.");
            return -2;
        }
    }

    public static int zf(Socket socket) {
        int i = 0;
        int intValue;
        try {
            intValue = ((Integer) FileDescriptor.class.getMethod("getInt$", new Class[0]).invoke((FileDescriptor) Socket.class.getMethod("getFileDescriptor$", new Class[0]).invoke(socket, new Object[0]), new Object[0])).intValue();
            try {
                a.st("PushLog3414", "socket fd is " + intValue);
                return intValue;
            } catch (NoSuchMethodException e) {
                i = intValue;
                a.st("PushLog3414", "There is no method of ctrlSockets.");
                return i;
            } catch (IllegalArgumentException e2) {
                i = intValue;
                a.st("PushLog3414", "There is no method of ctrlSockets.");
                return i;
            } catch (IllegalAccessException e3) {
                i = intValue;
                a.st("PushLog3414", "There is no method of ctrlSockets.");
                return i;
            } catch (InvocationTargetException e4) {
                a.st("PushLog3414", "There is no method of ctrlSockets.");
                return intValue;
            }
        } catch (NoSuchMethodException e5) {
        } catch (IllegalArgumentException e6) {
            a.st("PushLog3414", "There is no method of ctrlSockets.");
            return i;
        } catch (IllegalAccessException e7) {
            a.st("PushLog3414", "There is no method of ctrlSockets.");
            return i;
        } catch (InvocationTargetException e8) {
            intValue = 0;
            a.st("PushLog3414", "There is no method of ctrlSockets.");
            return intValue;
        }
    }

    public static void yr(Context context, long j) {
        a.st("PushLog3414", "enter wakeSystem");
        ((PowerManager) context.getSystemService("power")).newWakeLock(1, "dispatcher").acquire(j);
    }

    /* JADX WARNING: Removed duplicated region for block: B:9:0x0020  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static String yk(Context context) {
        if (context == null) {
            return "";
        }
        String extraInfo;
        String str = "";
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                extraInfo = activeNetworkInfo.getExtraInfo();
                if (extraInfo == null) {
                    extraInfo = "";
                }
                return extraInfo;
            }
        }
        extraInfo = str;
        if (extraInfo == null) {
        }
        return extraInfo;
    }

    public static String zb() {
        String str = "";
        Class[] clsArr = new Class[]{String.class};
        Object[] objArr = new Object[]{"ro.build.version.emui"};
        try {
            Class cls = Class.forName("android.os.SystemProperties");
            String str2 = (String) cls.getDeclaredMethod("get", clsArr).invoke(cls, objArr);
            a.st("PushLog3414", "get EMUI version is:" + str2);
            if (TextUtils.isEmpty(str2)) {
                return str;
            }
            return str2;
        } catch (ClassNotFoundException e) {
            a.su("PushLog3414", " getEmuiVersion wrong, ClassNotFoundException");
        } catch (LinkageError e2) {
            a.su("PushLog3414", " getEmuiVersion wrong, LinkageError");
        } catch (NoSuchMethodException e3) {
            a.su("PushLog3414", " getEmuiVersion wrong, NoSuchMethodException");
        } catch (Exception e4) {
            a.su("PushLog3414", " getEmuiVersion wrong");
        }
    }

    public static boolean zn() {
        Class[] clsArr = new Class[]{String.class, Boolean.TYPE};
        Object[] objArr = new Object[]{"ro.config.bg_data_switch", Boolean.valueOf(false)};
        try {
            Class cls = Class.forName("android.os.SystemProperties");
            boolean booleanValue = ((Boolean) cls.getDeclaredMethod("getBoolean", clsArr).invoke(cls, objArr)).booleanValue();
            a.sv("PushLog3414", "is support HW network policy:" + booleanValue);
            return booleanValue;
        } catch (ClassNotFoundException e) {
            a.su("PushLog3414", " get SupportHwNetworkPolicy wrong, ClassNotFoundException");
            return false;
        } catch (LinkageError e2) {
            a.su("PushLog3414", " get SupportHwNetworkPolicy wrong, LinkageError");
            return false;
        } catch (NoSuchMethodException e3) {
            a.su("PushLog3414", " get SupportHwNetworkPolicy wrong, NoSuchMethodException");
            return false;
        } catch (Exception e4) {
            a.su("PushLog3414", " get SupportHwNetworkPolicy wrong");
            return false;
        }
    }

    public static boolean yu(Context context, String str) {
        boolean equals = (zm(context) || zj(context)) ? true : "com.huawei.hidisk".equals(str);
        if (!equals) {
            a.sv("PushLog3414", "OOBE is not complete and never Agreement so return");
        }
        return equals;
    }

    public static boolean zm(Context context) {
        boolean z = true;
        if (!(Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 1 || Global.getInt(context.getContentResolver(), "user_setup_complete", 0) == 1)) {
            z = false;
        }
        a.st("PushLog3414", "isOOBE:" + z);
        return z;
    }

    public static boolean zj(Context context) {
        boolean z = true;
        if (Global.getInt(context.getContentResolver(), "oobe_statement_agreed", 0) != 1) {
            z = false;
        }
        a.st("PushLog3414", "isAgreement:" + z);
        return z;
    }

    public static boolean yn(Context context, String str, int i) {
        if (context == null || str == null || "".equals(str)) {
            return false;
        }
        try {
            PackageInfo packageInfoAsUser = context.getPackageManager().getPackageInfoAsUser(str, 0, i);
            if (packageInfoAsUser == null) {
                a.su("PushLog3414", "query package isInstalled failed! packageInfo is null");
                return false;
            }
            if (str.equals(packageInfoAsUser.packageName)) {
                a.st("PushLog3414", str + " is installed in user " + i);
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            a.su("PushLog3414", "query package isInstalled failed, NameNotFoundException!");
        } catch (Exception e2) {
            a.su("PushLog3414", "query package isInstalled failed!");
        }
    }

    private static String yz(Context context) {
        a.st("PushLog3414", "enter getCachedDeviceId()");
        return com.huawei.android.pushagent.model.prefs.g.kp(context).getDeviceId();
    }

    public static String ys(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return str + "/" + a.yb(str2);
    }

    public static String yw(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return str.split("/")[0];
    }

    public static int zh(String str) {
        int i = 0;
        if (TextUtils.isEmpty(str)) {
            return i;
        }
        String[] split = str.split("/");
        if (split.length > 1) {
            try {
                return Integer.parseInt(split[1]);
            } catch (Exception e) {
                a.su("PushLog3414", "parse int error:" + split[1]);
            }
        }
        return i;
    }

    public static String yx(String str) {
        if (str.indexOf("/") <= 0) {
            return str + "/" + "00";
        }
        return str;
    }

    public static boolean yy(Context context, String str, int i) {
        if (context == null) {
            a.sv("PushLog3414", "enter getNotificationsBanned, context is null");
            return false;
        } else if (TextUtils.isEmpty(str)) {
            a.sv("PushLog3414", "enter getNotificationsBanned, pkg is empty");
            return false;
        } else {
            try {
                ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str, 0, i);
                if (applicationInfo == null) {
                    a.sv("PushLog3414", "enter getNotificationsBanned, ApplicationInfo is empty");
                    return false;
                }
                boolean areNotificationsEnabledForPackage = Stub.asInterface(ServiceManager.getService("notification")).areNotificationsEnabledForPackage(str, applicationInfo.uid);
                a.sv("PushLog3414", "areNotificationsEnabledForPackage:" + areNotificationsEnabledForPackage);
                return areNotificationsEnabledForPackage;
            } catch (Exception e) {
                a.td("PushLog3414", "Error calling NoMan", e);
                return false;
            }
        }
    }

    public static boolean yo(Context context, String str, int i) {
        if (context == null || str == null) {
            a.sv("PushLog3414", "areNotificationEnableForChannel context or pkg is null");
            return true;
        }
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
            int intValue = ((Integer) notificationManager.getClass().getDeclaredField("IMPORTANCE_NONE").get(notificationManager)).intValue();
            ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str, 0, i);
            if (applicationInfo == null) {
                a.sv("PushLog3414", "ApplicationInfo is empty, maybe app is not installed ");
                return true;
            }
            a.sv("PushLog3414", str + "  in user " + i + " ,uid is " + applicationInfo.uid);
            Class cls = Class.forName("android.app.NotificationManager");
            Object invoke = cls.getDeclaredMethod("getService", new Class[0]).invoke(cls, new Object[0]);
            Object invoke2 = Class.forName("android.app.INotificationManager").getDeclaredMethod("getNotificationChannelForPackage", new Class[]{String.class, Integer.TYPE, String.class, Boolean.TYPE}).invoke(invoke, new Object[]{str, Integer.valueOf(applicationInfo.uid), "com.huawei.android.pushagent", Boolean.valueOf(false)});
            if (invoke2 == null) {
                a.sv("PushLog3414", "notificationChannel is null, maybe not set");
                return true;
            }
            int intValue2 = ((Integer) Class.forName("android.app.NotificationChannel").getDeclaredMethod("getImportance", new Class[0]).invoke(invoke2, new Object[0])).intValue();
            a.sv("PushLog3414", "importance:" + intValue2);
            if (intValue == intValue2) {
                a.sv("PushLog3414", "areNotificationEnableForChannel:false");
                return false;
            }
            return true;
        } catch (ClassNotFoundException e) {
            a.su("PushLog3414", e.toString());
        } catch (NoSuchMethodException e2) {
            a.su("PushLog3414", e2.toString());
        } catch (IllegalAccessException e3) {
            a.su("PushLog3414", e3.toString());
        } catch (IllegalArgumentException e4) {
            a.su("PushLog3414", e4.toString());
        } catch (InvocationTargetException e5) {
            a.su("PushLog3414", e5.toString());
        } catch (NoSuchFieldException e6) {
            a.su("PushLog3414", e6.toString());
        } catch (RemoteException e7) {
            a.su("PushLog3414", e7.toString());
        } catch (Exception e8) {
            a.su("PushLog3414", e8.toString());
        }
    }

    public static final String zd() {
        return com.huawei.android.pushagent.utils.e.f.wv(String.valueOf("com.huawei.android.pushagent".hashCode()));
    }

    public static String zc(Context context, String str, String str2, String str3) {
        if (context == null || TextUtils.isEmpty(str) || TextUtils.isEmpty(str3)) {
            return "";
        }
        String ys = ys(str, str2);
        String vt = com.huawei.android.pushagent.utils.e.a.vt(new b(context, "push_notify_key").tg(ys));
        if (!TextUtils.isEmpty(vt)) {
            return vt;
        }
        vt = com.huawei.android.pushagent.utils.e.f.wv(String.valueOf((str3 + System.currentTimeMillis()).hashCode()));
        new b(context, "push_notify_key").tm(ys, com.huawei.android.pushagent.utils.e.a.vu(vt));
        return vt;
    }

    public static void yt(Context context, String str, String str2, String str3) {
        if (str != null && str3 != null) {
            try {
                Intent flags = new Intent("com.huawei.android.push.intent.REGISTRATION").setPackage(str).putExtra("device_token", str3.getBytes("UTF-8")).putExtra("belongId", com.huawei.android.pushagent.model.prefs.a.ff(context).getBelongId()).setFlags(32);
                String zc = zc(context, str, str2, str3);
                if (!TextUtils.isEmpty(zc)) {
                    flags.putExtra("extra_encrypt_key", zc);
                }
                a.sv("PushLog3414", "send registerToken to:" + str);
                zq(context, flags, Integer.parseInt(str2));
            } catch (Exception e) {
                a.sw("PushLog3414", e.toString(), e);
            }
        }
    }

    public static void zq(Context context, Intent intent, int i) {
        boolean supportsMultipleUsers = UserManager.supportsMultipleUsers();
        a.sv("PushLog3414", "isSupportsMultiUsers: " + supportsMultipleUsers);
        if (supportsMultipleUsers) {
            context.sendBroadcastAsUser(intent, new UserHandle(i));
        } else {
            context.sendBroadcast(intent);
        }
    }

    public static List<ResolveInfo> zp(PackageManager packageManager, Intent intent, int i, int i2) {
        a.st("PushLog3414", "enter queryBroadcastReceiversAsUser");
        if (packageManager == null || intent == null) {
            a.su("PushLog3414", "packageManager is null");
            return null;
        }
        try {
            return (List) Class.forName("android.content.pm.PackageManager").getMethod("queryBroadcastReceiversAsUser", new Class[]{Intent.class, Integer.TYPE, Integer.TYPE}).invoke(packageManager, new Object[]{intent, Integer.valueOf(i), Integer.valueOf(i2)});
        } catch (ClassNotFoundException e) {
            a.st("PushLog3414", "queryBroadcastReceiversAsUser ClassNotFoundException");
            return null;
        } catch (NoSuchMethodException e2) {
            a.st("PushLog3414", "queryBroadcastReceiversAsUser NoSuchMethodException");
            return null;
        } catch (IllegalArgumentException e3) {
            a.st("PushLog3414", "queryBroadcastReceiversAsUser IllegalArgumentException");
            return null;
        } catch (IllegalAccessException e4) {
            a.st("PushLog3414", "queryBroadcastReceiversAsUser IllegalAccessException");
            return null;
        } catch (InvocationTargetException e5) {
            a.st("PushLog3414", "queryBroadcastReceiversAsUser InvocationTargetException");
            return null;
        }
    }

    public static String ze(Throwable th) {
        if (th == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Exception: ").append(th.getClass().getName()).append(10);
        StackTraceElement[] stackTrace = th.getStackTrace();
        if (stackTrace == null) {
            return "";
        }
        for (StackTraceElement stackTraceElement : stackTrace) {
            stringBuilder.append(stackTraceElement.toString()).append(10);
        }
        return stringBuilder.toString();
    }

    public static boolean zl() {
        try {
            String format = new SimpleDateFormat("HH:mm").format(new Date());
            if (format.compareTo("00:00") > 0 && format.compareTo("06:00") < 0) {
                return true;
            }
        } catch (RuntimeException e) {
            a.st("PushLog3414", "format dawn perild time RuntimeException.");
        } catch (Exception e2) {
            a.st("PushLog3414", "format dawn perild time exception.");
        }
        return false;
    }

    public static boolean yl() {
        BatteryManager batteryManager = new BatteryManager();
        a.sv("PushLog3414", " get charing ");
        boolean isCharging = batteryManager.isCharging();
        a.sv("PushLog3414", " get charing " + isCharging);
        return isCharging;
    }
}
