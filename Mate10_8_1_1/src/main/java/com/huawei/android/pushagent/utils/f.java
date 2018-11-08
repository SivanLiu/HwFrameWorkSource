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
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.b.b;
import com.huawei.android.pushagent.utils.b.d;
import com.huawei.android.pushagent.utils.c.c;
import java.io.FileDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class f {
    public static byte[] gv(int i) {
        return new byte[]{(byte) ((i >> 24) & 255), (byte) ((i >> 16) & 255), (byte) ((i >> 8) & 255), (byte) (i & 255)};
    }

    public static int ft(byte[] bArr) {
        return ((bArr[0] & 255) << 8) | (bArr[1] & 255);
    }

    public static int fs(byte b) {
        return b & 255;
    }

    public static byte[] fr(int i) {
        return new byte[]{(byte) ((i >> 8) & 255), (byte) (i & 255)};
    }

    public static String gk(Context context) {
        String gx = gx(context);
        if (!TextUtils.isEmpty(gx)) {
            return gx;
        }
        d av = b.av(context, com.huawei.android.pushagent.model.prefs.b.kp(context).km());
        gx = av.aw();
        int deviceIdType = av.getDeviceIdType();
        com.huawei.android.pushagent.model.prefs.b.kp(context).kt(gx);
        com.huawei.android.pushagent.model.prefs.b.kp(context).setDeviceIdType(deviceIdType);
        return gx;
    }

    public static int gt(Context context) {
        if (-1 == com.huawei.android.pushagent.model.prefs.b.kp(context).getDeviceIdType()) {
            gk(context);
        }
        return com.huawei.android.pushagent.model.prefs.b.kp(context).getDeviceIdType();
    }

    public static String gs(Context context) {
        return String.valueOf(2976);
    }

    public static String gc(Context context) {
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

    public static boolean fq(Context context, String str) {
        List gm = gm(context.getPackageManager(), new Intent("com.huawei.android.push.intent.RECEIVE").setPackage(str), 787072, a.fc());
        if (gm == null || gm.size() == 0) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "isClientSupportMsgResponse: false");
            return false;
        }
        String str2;
        if (((ResolveInfo) gm.get(0)).serviceInfo != null) {
            str2 = ((ResolveInfo) gm.get(0)).serviceInfo.packageName;
        } else {
            str2 = ((ResolveInfo) gm.get(0)).activityInfo.packageName;
        }
        if (str2 != null && str2.equals(str)) {
            Object gn = gn((ResolveInfo) gm.get(0), "CS_cloud_ablitity");
            if (TextUtils.isEmpty(gn)) {
                return false;
            }
            try {
                boolean contains = Arrays.asList(gn.split("\\|")).contains("successRateAnalytics");
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "isClientSupportMsgResponse:" + contains);
                return contains;
            } catch (Exception e) {
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e.toString());
            }
        }
        return false;
    }

    public static String gn(ResolveInfo resolveInfo, String str) {
        Bundle bundle = resolveInfo.serviceInfo != null ? resolveInfo.serviceInfo.metaData : resolveInfo.activityInfo.metaData;
        if (bundle == null) {
            return null;
        }
        return bundle.getString(str);
    }

    public static String ga(Context context) {
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

    public static int fp(Context context) {
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

    public static boolean ge(Context context) {
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

    public static void gj() {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "enter powerLow");
        try {
            Class.forName("com.huawei.pgmng.log.LogPower").getMethod("push", new Class[]{Integer.TYPE}).invoke(null, new Object[]{Integer.valueOf(119)});
        } catch (ClassNotFoundException e) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "ClassNotFoundException, not support LogPower");
        } catch (NoSuchMethodException e2) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "NoSuchMethodException, not support LogPower");
        } catch (IllegalArgumentException e3) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "IllegalArgumentException, not support LogPower");
        } catch (IllegalAccessException e4) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "IllegalAccessException, not support LogPower");
        } catch (InvocationTargetException e5) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "InvocationTargetException, not support LogPower");
        }
    }

    public static int fv(int i, int i2) {
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "enter ctrlSockets(cmd:" + i + " param:" + i2 + ")");
        try {
            return ((Integer) Class.forName("dalvik.system.Zygote").getMethod("ctrlSockets", new Class[]{Integer.TYPE, Integer.TYPE}).invoke(null, new Object[]{Integer.valueOf(i), Integer.valueOf(i2)})).intValue();
        } catch (ClassNotFoundException e) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
            return -2;
        } catch (NoSuchMethodException e2) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
            return -2;
        } catch (IllegalArgumentException e3) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
            return -2;
        } catch (IllegalAccessException e4) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
            return -2;
        } catch (InvocationTargetException e5) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
            return -2;
        }
    }

    public static int gp(Socket socket) {
        int i = 0;
        int intValue;
        try {
            intValue = ((Integer) FileDescriptor.class.getMethod("getInt$", new Class[0]).invoke((FileDescriptor) Socket.class.getMethod("getFileDescriptor$", new Class[0]).invoke(socket, new Object[0]), new Object[0])).intValue();
            try {
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "socket fd is " + intValue);
                return intValue;
            } catch (NoSuchMethodException e) {
                i = intValue;
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
                return i;
            } catch (IllegalArgumentException e2) {
                i = intValue;
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
                return i;
            } catch (IllegalAccessException e3) {
                i = intValue;
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
                return i;
            } catch (InvocationTargetException e4) {
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
                return intValue;
            }
        } catch (NoSuchMethodException e5) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
            return i;
        } catch (IllegalArgumentException e6) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
            return i;
        } catch (IllegalAccessException e7) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
            return i;
        } catch (InvocationTargetException e8) {
            intValue = 0;
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "There is no method of ctrlSockets.");
            return intValue;
        }
    }

    public static void gf(Context context, long j) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "enter wakeSystem");
        ((PowerManager) context.getSystemService("power")).newWakeLock(1, "dispatcher").acquire(j);
    }

    public static String gb(Context context) {
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
            extraInfo = "";
        }
        return extraInfo;
    }

    public static String gu() {
        String str = "";
        Class[] clsArr = new Class[]{String.class};
        Object[] objArr = new Object[]{"ro.build.version.emui"};
        try {
            Class cls = Class.forName("android.os.SystemProperties");
            String str2 = (String) cls.getDeclaredMethod("get", clsArr).invoke(cls, objArr);
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "get EMUI version is:" + str2);
            if (TextUtils.isEmpty(str2)) {
                return str;
            }
            return str2;
        } catch (ClassNotFoundException e) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", " getEmuiVersion wrong, ClassNotFoundException");
        } catch (LinkageError e2) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", " getEmuiVersion wrong, LinkageError");
        } catch (NoSuchMethodException e3) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", " getEmuiVersion wrong, NoSuchMethodException");
        } catch (Exception e4) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", " getEmuiVersion wrong");
        }
    }

    public static boolean gd() {
        Class[] clsArr = new Class[]{String.class, Boolean.TYPE};
        Object[] objArr = new Object[]{"ro.config.bg_data_switch", Boolean.valueOf(false)};
        try {
            Class cls = Class.forName("android.os.SystemProperties");
            boolean booleanValue = ((Boolean) cls.getDeclaredMethod("getBoolean", clsArr).invoke(cls, objArr)).booleanValue();
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "is support HW network policy:" + booleanValue);
            return booleanValue;
        } catch (ClassNotFoundException e) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", " get SupportHwNetworkPolicy wrong, ClassNotFoundException");
            return false;
        } catch (LinkageError e2) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", " get SupportHwNetworkPolicy wrong, LinkageError");
            return false;
        } catch (NoSuchMethodException e3) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", " get SupportHwNetworkPolicy wrong, NoSuchMethodException");
            return false;
        } catch (Exception e4) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", " get SupportHwNetworkPolicy wrong");
            return false;
        }
    }

    public static boolean gr(Context context, String str) {
        boolean equals = (ha(context) || gz(context)) ? true : "com.huawei.hidisk".equals(str);
        if (!equals) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "OOBE is not complete and never Agreement so return");
        }
        return equals;
    }

    public static boolean ha(Context context) {
        boolean z = true;
        if (!(Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 1 || Global.getInt(context.getContentResolver(), "user_setup_complete", 0) == 1)) {
            z = false;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "isOOBE:" + z);
        return z;
    }

    public static boolean gz(Context context) {
        boolean z = true;
        if (Global.getInt(context.getContentResolver(), "oobe_statement_agreed", 0) != 1) {
            z = false;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "isAgreement:" + z);
        return z;
    }

    public static boolean gq(Context context, String str, int i) {
        if (context == null || str == null || "".equals(str)) {
            return false;
        }
        try {
            PackageInfo packageInfoAsUser = context.getPackageManager().getPackageInfoAsUser(str, 0, i);
            if (packageInfoAsUser == null) {
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "query package isInstalled failed! packageInfo is null");
                return false;
            }
            if (str.equals(packageInfoAsUser.packageName)) {
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", str + " is installed in user " + i);
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "query package isInstalled failed, NameNotFoundException!");
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "query package isInstalled failed!");
        }
    }

    private static String gx(Context context) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "enter getCachedDeviceId()");
        return com.huawei.android.pushagent.model.prefs.b.kp(context).getDeviceId();
    }

    public static String fz(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return str + "/" + a.ff(str2);
    }

    public static String gh(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return str.split("/")[0];
    }

    public static int gi(String str) {
        int i = 0;
        if (TextUtils.isEmpty(str)) {
            return i;
        }
        String[] split = str.split("/");
        if (split.length > 1) {
            try {
                return Integer.parseInt(split[1]);
            } catch (Exception e) {
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "parse int error:" + split[1]);
            }
        }
        return i;
    }

    public static String gl(String str) {
        if (str.indexOf("/") <= 0) {
            return str + "/" + "00";
        }
        return str;
    }

    public static boolean fx(Context context, String str, int i) {
        if (context == null) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "enter getNotificationsBanned, context is null");
            return false;
        } else if (TextUtils.isEmpty(str)) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "enter getNotificationsBanned, pkg is empty");
            return false;
        } else {
            try {
                ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str, 0, i);
                if (applicationInfo == null) {
                    com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "enter getNotificationsBanned, ApplicationInfo is empty");
                    return false;
                }
                boolean areNotificationsEnabledForPackage = Stub.asInterface(ServiceManager.getService("notification")).areNotificationsEnabledForPackage(str, applicationInfo.uid);
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "areNotificationsEnabledForPackage:" + areNotificationsEnabledForPackage);
                return areNotificationsEnabledForPackage;
            } catch (Throwable e) {
                com.huawei.android.pushagent.utils.a.b.ac("PushLog2976", "Error calling NoMan", e);
                return false;
            }
        }
    }

    public static boolean gw(Context context, String str, int i) {
        if (context == null || str == null) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "areNotificationEnableForChannel context or pkg is null");
            return true;
        }
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
            int intValue = ((Integer) notificationManager.getClass().getDeclaredField("IMPORTANCE_NONE").get(notificationManager)).intValue();
            ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str, 0, i);
            if (applicationInfo == null) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "ApplicationInfo is empty, maybe app is not installed ");
                return true;
            }
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", str + "  in user " + i + " ,uid is " + applicationInfo.uid);
            Class cls = Class.forName("android.app.NotificationManager");
            Object invoke = cls.getDeclaredMethod("getService", new Class[0]).invoke(cls, new Object[0]);
            Object invoke2 = Class.forName("android.app.INotificationManager").getDeclaredMethod("getNotificationChannelForPackage", new Class[]{String.class, Integer.TYPE, String.class, Boolean.TYPE}).invoke(invoke, new Object[]{str, Integer.valueOf(applicationInfo.uid), "com.huawei.android.pushagent", Boolean.valueOf(false)});
            if (invoke2 == null) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "notificationChannel is null, maybe not set");
                return true;
            }
            int intValue2 = ((Integer) Class.forName("android.app.NotificationChannel").getDeclaredMethod("getImportance", new Class[0]).invoke(invoke2, new Object[0])).intValue();
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "importance:" + intValue2);
            if (intValue == intValue2) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "areNotificationEnableForChannel:false");
                return false;
            }
            return true;
        } catch (ClassNotFoundException e) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e.toString());
        } catch (NoSuchMethodException e2) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e2.toString());
        } catch (IllegalAccessException e3) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e3.toString());
        } catch (IllegalArgumentException e4) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e4.toString());
        } catch (InvocationTargetException e5) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e5.toString());
        } catch (NoSuchFieldException e6) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e6.toString());
        } catch (RemoteException e7) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e7.toString());
        } catch (Exception e8) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e8.toString());
        }
    }

    public static final String fy() {
        return com.huawei.android.pushagent.utils.c.f.bz(String.valueOf("com.huawei.android.pushagent".hashCode()));
    }

    public static String gy(Context context, String str, String str2, String str3) {
        if (context == null || TextUtils.isEmpty(str) || TextUtils.isEmpty(str3)) {
            return "";
        }
        String fz = fz(str, str2);
        Object bb = c.bb(new com.huawei.android.pushagent.utils.a.c(context, "push_notify_key").aj(fz));
        if (!TextUtils.isEmpty(bb)) {
            return bb;
        }
        String bz = com.huawei.android.pushagent.utils.c.f.bz(String.valueOf((str3 + System.currentTimeMillis()).hashCode()));
        new com.huawei.android.pushagent.utils.a.c(context, "push_notify_key").ak(fz, c.bc(bz));
        return bz;
    }

    public static void gg(Context context, String str, String str2, String str3) {
        if (str != null && str3 != null) {
            try {
                Intent flags = new Intent("com.huawei.android.push.intent.REGISTRATION").setPackage(str).putExtra("device_token", str3.getBytes("UTF-8")).putExtra("belongId", i.mj(context).getBelongId()).setFlags(32);
                Object gy = gy(context, str, str2, str3);
                if (!TextUtils.isEmpty(gy)) {
                    flags.putExtra("extra_encrypt_key", gy);
                }
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "send registerToken to:" + str);
                fw(context, flags, Integer.parseInt(str2));
            } catch (Throwable e) {
                com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", e.toString(), e);
            }
        }
    }

    public static void fw(Context context, Intent intent, int i) {
        boolean supportsMultipleUsers = UserManager.supportsMultipleUsers();
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "isSupportsMultiUsers: " + supportsMultipleUsers);
        if (supportsMultipleUsers) {
            context.sendBroadcastAsUser(intent, new UserHandle(i));
        } else {
            context.sendBroadcast(intent);
        }
    }

    public static List<ResolveInfo> gm(PackageManager packageManager, Intent intent, int i, int i2) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "enter queryBroadcastReceiversAsUser");
        if (packageManager == null || intent == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "packageManager is null");
            return null;
        }
        try {
            return (List) Class.forName("android.content.pm.PackageManager").getMethod("queryBroadcastReceiversAsUser", new Class[]{Intent.class, Integer.TYPE, Integer.TYPE}).invoke(packageManager, new Object[]{intent, Integer.valueOf(i), Integer.valueOf(i2)});
        } catch (ClassNotFoundException e) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "queryBroadcastReceiversAsUser ClassNotFoundException");
            return null;
        } catch (NoSuchMethodException e2) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "queryBroadcastReceiversAsUser NoSuchMethodException");
            return null;
        } catch (IllegalArgumentException e3) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "queryBroadcastReceiversAsUser IllegalArgumentException");
            return null;
        } catch (IllegalAccessException e4) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "queryBroadcastReceiversAsUser IllegalAccessException");
            return null;
        } catch (InvocationTargetException e5) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "queryBroadcastReceiversAsUser InvocationTargetException");
            return null;
        }
    }

    public static String go(Throwable th) {
        if (th == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Exception: ").append(th.getClass().getName()).append('\n');
        StackTraceElement[] stackTrace = th.getStackTrace();
        if (stackTrace == null) {
            return "";
        }
        for (StackTraceElement stackTraceElement : stackTrace) {
            stringBuilder.append(stackTraceElement.toString()).append('\n');
        }
        return stringBuilder.toString();
    }

    public static boolean fu() {
        try {
            String format = new SimpleDateFormat("HH:mm").format(new Date());
            if (format.compareTo("00:00") > 0 && format.compareTo("06:00") < 0) {
                return true;
            }
        } catch (RuntimeException e) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "format dawn perild time RuntimeException.");
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "format dawn perild time exception.");
        }
        return false;
    }
}
