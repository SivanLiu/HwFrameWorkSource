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
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.a.e;
import com.huawei.android.pushagent.utils.d.b;
import com.huawei.android.pushagent.utils.d.d;
import com.huawei.android.pushagent.utils.f.a;
import com.huawei.android.pushagent.utils.f.c;
import java.io.FileDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class g {
    public static byte[] gw(int i) {
        return new byte[]{(byte) ((i >> 24) & 255), (byte) ((i >> 16) & 255), (byte) ((i >> 8) & 255), (byte) (i & 255)};
    }

    public static int gx(byte[] bArr) {
        return ((bArr[0] & 255) << 8) | (bArr[1] & 255);
    }

    public static int gy(byte b) {
        return b & 255;
    }

    public static byte[] gd(int i) {
        return new byte[]{(byte) ((i >> 8) & 255), (byte) (i & 255)};
    }

    public static String gi(Context context) {
        String gz = gz(context);
        if (!TextUtils.isEmpty(gz)) {
            return gz;
        }
        b cn = d.cn(context, com.huawei.android.pushagent.model.prefs.b.oq(context).or());
        gz = cn.cl();
        int deviceIdType = cn.getDeviceIdType();
        com.huawei.android.pushagent.model.prefs.b.oq(context).oy(gz);
        com.huawei.android.pushagent.model.prefs.b.oq(context).setDeviceIdType(deviceIdType);
        return gz;
    }

    public static int gp(Context context) {
        if (-1 == com.huawei.android.pushagent.model.prefs.b.oq(context).getDeviceIdType()) {
            gi(context);
        }
        return com.huawei.android.pushagent.model.prefs.b.oq(context).getDeviceIdType();
    }

    public static String gk(Context context) {
        return String.valueOf(3413);
    }

    public static String fx(Context context) {
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

    public static boolean gu(Context context, String str) {
        List gs = gs(context.getPackageManager(), new Intent("com.huawei.android.push.intent.RECEIVE").setPackage(str), 787072, a.fb());
        if (gs == null || gs.size() == 0) {
            c.er("PushLog3413", "isClientSupportMsgResponse: false");
            return false;
        }
        String str2;
        if (((ResolveInfo) gs.get(0)).serviceInfo != null) {
            str2 = ((ResolveInfo) gs.get(0)).serviceInfo.packageName;
        } else {
            str2 = ((ResolveInfo) gs.get(0)).activityInfo.packageName;
        }
        if (str2 != null && str2.equals(str)) {
            Object gt = gt((ResolveInfo) gs.get(0), "CS_cloud_ablitity");
            if (TextUtils.isEmpty(gt)) {
                return false;
            }
            try {
                boolean contains = Arrays.asList(gt.split("\\|")).contains("successRateAnalytics");
                c.er("PushLog3413", "isClientSupportMsgResponse:" + contains);
                return contains;
            } catch (Exception e) {
                c.eq("PushLog3413", e.toString());
            }
        }
        return false;
    }

    public static String gt(ResolveInfo resolveInfo, String str) {
        Bundle bundle = resolveInfo.serviceInfo != null ? resolveInfo.serviceInfo.metaData : resolveInfo.activityInfo.metaData;
        if (bundle == null) {
            return null;
        }
        return bundle.getString(str);
    }

    public static String fy(Context context) {
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

    public static int fw(Context context) {
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

    public static boolean fq(Context context) {
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

    public static void gb() {
        c.er("PushLog3413", "enter powerLow");
        try {
            Class.forName("com.huawei.pgmng.log.LogPower").getMethod("push", new Class[]{Integer.TYPE}).invoke(null, new Object[]{Integer.valueOf(119)});
        } catch (ClassNotFoundException e) {
            c.er("PushLog3413", "ClassNotFoundException, not support LogPower");
        } catch (NoSuchMethodException e2) {
            c.er("PushLog3413", "NoSuchMethodException, not support LogPower");
        } catch (IllegalArgumentException e3) {
            c.er("PushLog3413", "IllegalArgumentException, not support LogPower");
        } catch (IllegalAccessException e4) {
            c.er("PushLog3413", "IllegalAccessException, not support LogPower");
        } catch (InvocationTargetException e5) {
            c.er("PushLog3413", "InvocationTargetException, not support LogPower");
        }
    }

    public static int ge(int i, int i2) {
        c.ep("PushLog3413", "enter ctrlSockets(cmd:" + i + " param:" + i2 + ")");
        try {
            return ((Integer) Class.forName("dalvik.system.Zygote").getMethod("ctrlSockets", new Class[]{Integer.TYPE, Integer.TYPE}).invoke(null, new Object[]{Integer.valueOf(i), Integer.valueOf(i2)})).intValue();
        } catch (ClassNotFoundException e) {
            c.er("PushLog3413", "There is no method of ctrlSockets.");
            return -2;
        } catch (NoSuchMethodException e2) {
            c.er("PushLog3413", "There is no method of ctrlSockets.");
            return -2;
        } catch (IllegalArgumentException e3) {
            c.er("PushLog3413", "There is no method of ctrlSockets.");
            return -2;
        } catch (IllegalAccessException e4) {
            c.er("PushLog3413", "There is no method of ctrlSockets.");
            return -2;
        } catch (InvocationTargetException e5) {
            c.er("PushLog3413", "There is no method of ctrlSockets.");
            return -2;
        }
    }

    public static int gh(Socket socket) {
        int i = 0;
        int intValue;
        try {
            intValue = ((Integer) FileDescriptor.class.getMethod("getInt$", new Class[0]).invoke((FileDescriptor) Socket.class.getMethod("getFileDescriptor$", new Class[0]).invoke(socket, new Object[0]), new Object[0])).intValue();
            try {
                c.er("PushLog3413", "socket fd is " + intValue);
                return intValue;
            } catch (NoSuchMethodException e) {
                i = intValue;
                c.er("PushLog3413", "There is no method of ctrlSockets.");
                return i;
            } catch (IllegalArgumentException e2) {
                i = intValue;
                c.er("PushLog3413", "There is no method of ctrlSockets.");
                return i;
            } catch (IllegalAccessException e3) {
                i = intValue;
                c.er("PushLog3413", "There is no method of ctrlSockets.");
                return i;
            } catch (InvocationTargetException e4) {
                c.er("PushLog3413", "There is no method of ctrlSockets.");
                return intValue;
            }
        } catch (NoSuchMethodException e5) {
        } catch (IllegalArgumentException e6) {
            c.er("PushLog3413", "There is no method of ctrlSockets.");
            return i;
        } catch (IllegalAccessException e7) {
            c.er("PushLog3413", "There is no method of ctrlSockets.");
            return i;
        } catch (InvocationTargetException e8) {
            intValue = 0;
            c.er("PushLog3413", "There is no method of ctrlSockets.");
            return intValue;
        }
    }

    public static void fr(Context context, long j) {
        c.er("PushLog3413", "enter wakeSystem");
        ((PowerManager) context.getSystemService("power")).newWakeLock(1, "dispatcher").acquire(j);
    }

    /* JADX WARNING: Removed duplicated region for block: B:9:0x0020  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static String fz(Context context) {
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

    public static String gq() {
        String str = "";
        Class[] clsArr = new Class[]{String.class};
        Object[] objArr = new Object[]{"ro.build.version.emui"};
        try {
            Class cls = Class.forName("android.os.SystemProperties");
            String str2 = (String) cls.getDeclaredMethod("get", clsArr).invoke(cls, objArr);
            c.er("PushLog3413", "get EMUI version is:" + str2);
            if (TextUtils.isEmpty(str2)) {
                return str;
            }
            return str2;
        } catch (ClassNotFoundException e) {
            c.eq("PushLog3413", " getEmuiVersion wrong, ClassNotFoundException");
        } catch (LinkageError e2) {
            c.eq("PushLog3413", " getEmuiVersion wrong, LinkageError");
        } catch (NoSuchMethodException e3) {
            c.eq("PushLog3413", " getEmuiVersion wrong, NoSuchMethodException");
        } catch (Exception e4) {
            c.eq("PushLog3413", " getEmuiVersion wrong");
        }
    }

    public static boolean gr() {
        Class[] clsArr = new Class[]{String.class, Boolean.TYPE};
        Object[] objArr = new Object[]{"ro.config.bg_data_switch", Boolean.valueOf(false)};
        try {
            Class cls = Class.forName("android.os.SystemProperties");
            boolean booleanValue = ((Boolean) cls.getDeclaredMethod("getBoolean", clsArr).invoke(cls, objArr)).booleanValue();
            c.ep("PushLog3413", "is support HW network policy:" + booleanValue);
            return booleanValue;
        } catch (ClassNotFoundException e) {
            c.eq("PushLog3413", " get SupportHwNetworkPolicy wrong, ClassNotFoundException");
            return false;
        } catch (LinkageError e2) {
            c.eq("PushLog3413", " get SupportHwNetworkPolicy wrong, LinkageError");
            return false;
        } catch (NoSuchMethodException e3) {
            c.eq("PushLog3413", " get SupportHwNetworkPolicy wrong, NoSuchMethodException");
            return false;
        } catch (Exception e4) {
            c.eq("PushLog3413", " get SupportHwNetworkPolicy wrong");
            return false;
        }
    }

    public static boolean gj(Context context, String str) {
        boolean equals = (hc(context) || hb(context)) ? true : "com.huawei.hidisk".equals(str);
        if (!equals) {
            c.ep("PushLog3413", "OOBE is not complete and never Agreement so return");
        }
        return equals;
    }

    public static boolean hc(Context context) {
        boolean z = true;
        if (!(Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 1 || Global.getInt(context.getContentResolver(), "user_setup_complete", 0) == 1)) {
            z = false;
        }
        c.er("PushLog3413", "isOOBE:" + z);
        return z;
    }

    public static boolean hb(Context context) {
        boolean z = true;
        if (Global.getInt(context.getContentResolver(), "oobe_statement_agreed", 0) != 1) {
            z = false;
        }
        c.er("PushLog3413", "isAgreement:" + z);
        return z;
    }

    public static boolean gc(Context context, String str, int i) {
        if (context == null || str == null || "".equals(str)) {
            return false;
        }
        try {
            PackageInfo packageInfoAsUser = context.getPackageManager().getPackageInfoAsUser(str, 0, i);
            if (packageInfoAsUser == null) {
                c.eq("PushLog3413", "query package isInstalled failed! packageInfo is null");
                return false;
            }
            if (str.equals(packageInfoAsUser.packageName)) {
                c.er("PushLog3413", str + " is installed in user " + i);
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            c.eq("PushLog3413", "query package isInstalled failed, NameNotFoundException!");
        } catch (Exception e2) {
            c.eq("PushLog3413", "query package isInstalled failed!");
        }
    }

    private static String gz(Context context) {
        c.er("PushLog3413", "enter getCachedDeviceId()");
        return com.huawei.android.pushagent.model.prefs.b.oq(context).getDeviceId();
    }

    public static String fs(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return str + "/" + a.fe(str2);
    }

    public static String fu(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return str.split("/")[0];
    }

    public static int fv(String str) {
        int i = 0;
        if (TextUtils.isEmpty(str)) {
            return i;
        }
        String[] split = str.split("/");
        if (split.length > 1) {
            try {
                return Integer.parseInt(split[1]);
            } catch (Exception e) {
                c.eq("PushLog3413", "parse int error:" + split[1]);
            }
        }
        return i;
    }

    public static String gv(String str) {
        if (str.indexOf("/") <= 0) {
            return str + "/" + "00";
        }
        return str;
    }

    public static boolean gn(Context context, String str, int i) {
        if (context == null) {
            c.ep("PushLog3413", "enter getNotificationsBanned, context is null");
            return false;
        } else if (TextUtils.isEmpty(str)) {
            c.ep("PushLog3413", "enter getNotificationsBanned, pkg is empty");
            return false;
        } else {
            try {
                ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str, 0, i);
                if (applicationInfo == null) {
                    c.ep("PushLog3413", "enter getNotificationsBanned, ApplicationInfo is empty");
                    return false;
                }
                boolean areNotificationsEnabledForPackage = Stub.asInterface(ServiceManager.getService("notification")).areNotificationsEnabledForPackage(str, applicationInfo.uid);
                c.ep("PushLog3413", "areNotificationsEnabledForPackage:" + areNotificationsEnabledForPackage);
                return areNotificationsEnabledForPackage;
            } catch (Throwable e) {
                c.ev("PushLog3413", "Error calling NoMan", e);
                return false;
            }
        }
    }

    public static boolean gf(Context context, String str, int i) {
        if (context == null || str == null) {
            c.ep("PushLog3413", "areNotificationEnableForChannel context or pkg is null");
            return true;
        }
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
            int intValue = ((Integer) notificationManager.getClass().getDeclaredField("IMPORTANCE_NONE").get(notificationManager)).intValue();
            ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str, 0, i);
            if (applicationInfo == null) {
                c.ep("PushLog3413", "ApplicationInfo is empty, maybe app is not installed ");
                return true;
            }
            c.ep("PushLog3413", str + "  in user " + i + " ,uid is " + applicationInfo.uid);
            Class cls = Class.forName("android.app.NotificationManager");
            Object invoke = cls.getDeclaredMethod("getService", new Class[0]).invoke(cls, new Object[0]);
            Object invoke2 = Class.forName("android.app.INotificationManager").getDeclaredMethod("getNotificationChannelForPackage", new Class[]{String.class, Integer.TYPE, String.class, Boolean.TYPE}).invoke(invoke, new Object[]{str, Integer.valueOf(applicationInfo.uid), "com.huawei.android.pushagent", Boolean.valueOf(false)});
            if (invoke2 == null) {
                c.ep("PushLog3413", "notificationChannel is null, maybe not set");
                return true;
            }
            int intValue2 = ((Integer) Class.forName("android.app.NotificationChannel").getDeclaredMethod("getImportance", new Class[0]).invoke(invoke2, new Object[0])).intValue();
            c.ep("PushLog3413", "importance:" + intValue2);
            if (intValue == intValue2) {
                c.ep("PushLog3413", "areNotificationEnableForChannel:false");
                return false;
            }
            return true;
        } catch (ClassNotFoundException e) {
            c.eq("PushLog3413", e.toString());
        } catch (NoSuchMethodException e2) {
            c.eq("PushLog3413", e2.toString());
        } catch (IllegalAccessException e3) {
            c.eq("PushLog3413", e3.toString());
        } catch (IllegalArgumentException e4) {
            c.eq("PushLog3413", e4.toString());
        } catch (InvocationTargetException e5) {
            c.eq("PushLog3413", e5.toString());
        } catch (NoSuchFieldException e6) {
            c.eq("PushLog3413", e6.toString());
        } catch (RemoteException e7) {
            c.eq("PushLog3413", e7.toString());
        } catch (Exception e8) {
            c.eq("PushLog3413", e8.toString());
        }
    }

    public static final String go() {
        return e.r(String.valueOf("com.huawei.android.pushagent".hashCode()));
    }

    public static String ha(Context context, String str, String str2, String str3) {
        if (context == null || TextUtils.isEmpty(str) || TextUtils.isEmpty(str3)) {
            return "";
        }
        String fs = fs(str, str2);
        Object j = com.huawei.android.pushagent.utils.a.c.j(new a(context, "push_notify_key").ec(fs));
        if (!TextUtils.isEmpty(j)) {
            return j;
        }
        String r = e.r(String.valueOf((str3 + System.currentTimeMillis()).hashCode()));
        new a(context, "push_notify_key").ee(fs, com.huawei.android.pushagent.utils.a.c.k(r));
        return r;
    }

    public static void ft(Context context, String str, String str2, String str3) {
        if (str != null && str3 != null) {
            try {
                Intent flags = new Intent("com.huawei.android.push.intent.REGISTRATION").setPackage(str).putExtra("device_token", str3.getBytes("UTF-8")).putExtra("belongId", k.rh(context).getBelongId()).setFlags(32);
                Object ha = ha(context, str, str2, str3);
                if (!TextUtils.isEmpty(ha)) {
                    flags.putExtra("extra_encrypt_key", ha);
                }
                c.ep("PushLog3413", "send registerToken to:" + str);
                gm(context, flags, Integer.parseInt(str2));
            } catch (Throwable e) {
                c.es("PushLog3413", e.toString(), e);
            }
        }
    }

    public static void gm(Context context, Intent intent, int i) {
        boolean supportsMultipleUsers = UserManager.supportsMultipleUsers();
        c.ep("PushLog3413", "isSupportsMultiUsers: " + supportsMultipleUsers);
        if (supportsMultipleUsers) {
            context.sendBroadcastAsUser(intent, new UserHandle(i));
        } else {
            context.sendBroadcast(intent);
        }
    }

    public static List<ResolveInfo> gs(PackageManager packageManager, Intent intent, int i, int i2) {
        c.er("PushLog3413", "enter queryBroadcastReceiversAsUser");
        if (packageManager == null || intent == null) {
            c.eq("PushLog3413", "packageManager is null");
            return null;
        }
        try {
            return (List) Class.forName("android.content.pm.PackageManager").getMethod("queryBroadcastReceiversAsUser", new Class[]{Intent.class, Integer.TYPE, Integer.TYPE}).invoke(packageManager, new Object[]{intent, Integer.valueOf(i), Integer.valueOf(i2)});
        } catch (ClassNotFoundException e) {
            c.er("PushLog3413", "queryBroadcastReceiversAsUser ClassNotFoundException");
            return null;
        } catch (NoSuchMethodException e2) {
            c.er("PushLog3413", "queryBroadcastReceiversAsUser NoSuchMethodException");
            return null;
        } catch (IllegalArgumentException e3) {
            c.er("PushLog3413", "queryBroadcastReceiversAsUser IllegalArgumentException");
            return null;
        } catch (IllegalAccessException e4) {
            c.er("PushLog3413", "queryBroadcastReceiversAsUser IllegalAccessException");
            return null;
        } catch (InvocationTargetException e5) {
            c.er("PushLog3413", "queryBroadcastReceiversAsUser InvocationTargetException");
            return null;
        }
    }

    public static String gg(Throwable th) {
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

    public static boolean gl() {
        try {
            String format = new SimpleDateFormat("HH:mm").format(new Date());
            if (format.compareTo("00:00") > 0 && format.compareTo("06:00") < 0) {
                return true;
            }
        } catch (RuntimeException e) {
            c.er("PushLog3413", "format dawn perild time RuntimeException.");
        } catch (Exception e2) {
            c.er("PushLog3413", "format dawn perild time exception.");
        }
        return false;
    }

    public static boolean ga() {
        BatteryManager batteryManager = new BatteryManager();
        c.ep("PushLog3413", " get charing ");
        boolean isCharging = batteryManager.isCharging();
        c.ep("PushLog3413", " get charing " + isCharging);
        return isCharging;
    }
}
