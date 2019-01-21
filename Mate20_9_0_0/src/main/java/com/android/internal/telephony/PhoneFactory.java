package com.android.internal.telephony;

import android.content.Context;
import android.net.LocalServerSocket;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.MultiSimVariants;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.telephony.dataconnection.KeepaliveStatus;
import com.android.internal.telephony.dataconnection.TelephonyNetworkFactory;
import com.android.internal.telephony.euicc.EuiccCardController;
import com.android.internal.telephony.euicc.EuiccController;
import com.android.internal.telephony.ims.ImsResolver;
import com.android.internal.telephony.imsphone.ImsPhoneFactory;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.sip.SipPhoneFactory;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.telephony.vsim.VSimUtilsInner;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class PhoneFactory {
    static final boolean DBG = false;
    public static final boolean IS_DUAL_VOLTE_SUPPORTED = HwModemCapability.isCapabilitySupport(21);
    private static final boolean IS_FULL_NETWORK_SUPPORTED_IN_HISI = HwTelephonyFactory.getHwUiccManager().isFullNetworkSupported();
    public static final boolean IS_QCOM_DUAL_LTE_STACK = HwModemCapability.isCapabilitySupport(27);
    static final String LOG_TAG = "PhoneFactory";
    public static final int MAX_ACTIVE_PHONES;
    static final int SOCKET_OPEN_MAX_RETRY = 3;
    static final int SOCKET_OPEN_RETRY_MILLIS = 2000;
    private static final boolean mIsAdaptMultiSimConfiguration = SystemProperties.getBoolean("ro.config.multi_sim_cfg_adapt", false);
    private static CommandsInterface sCommandsInterface = null;
    private static CommandsInterface[] sCommandsInterfaces = null;
    private static Context sContext;
    private static EuiccCardController sEuiccCardController;
    private static EuiccController sEuiccController;
    private static ImsResolver sImsResolver;
    private static IntentBroadcaster sIntentBroadcaster;
    private static final HashMap<String, LocalLog> sLocalLogs = new HashMap();
    static final Object sLockProxyPhones = new Object();
    private static boolean sMadeDefaults = false;
    private static NotificationChannelController sNotificationChannelController;
    private static Phone sPhone = null;
    private static PhoneNotifier sPhoneNotifier;
    private static PhoneSwitcher sPhoneSwitcher;
    private static Phone[] sPhones = null;
    private static ProxyController sProxyController;
    private static SubscriptionInfoUpdater sSubInfoRecordUpdater = null;
    private static SubscriptionMonitor sSubscriptionMonitor;
    private static TelephonyNetworkFactory[] sTelephonyNetworkFactories;
    private static UiccController sUiccController;

    static {
        if ((MultiSimVariants.DSDA != TelephonyManager.getDefault().getMultiSimConfiguration() || mIsAdaptMultiSimConfiguration) && !(IS_QCOM_DUAL_LTE_STACK && IS_DUAL_VOLTE_SUPPORTED)) {
            MAX_ACTIVE_PHONES = 1;
        } else {
            MAX_ACTIVE_PHONES = 2;
        }
    }

    public static void makeDefaultPhones(Context context) {
        makeDefaultPhone(context);
    }

    /* JADX WARNING: Missing block: B:14:?, code skipped:
            sPhoneNotifier = new com.android.internal.telephony.DefaultPhoneNotifier();
            r13 = com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager.getDefault(r32);
            r0 = LOG_TAG;
            r1 = new java.lang.StringBuilder();
            r1.append("Cdma Subscription set to ");
            r1.append(r13);
            android.telephony.Rlog.i(r0, r1.toString());
            r0 = android.telephony.TelephonyManager.getDefault().getPhoneCount();
            r15 = sContext.getResources().getBoolean(17956945);
            r14 = sContext.getResources().getString(17039822);
            r1 = LOG_TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("ImsResolver: defaultImsPackage: ");
            r2.append(r14);
            android.telephony.Rlog.i(r1, r2.toString());
            sImsResolver = new com.android.internal.telephony.ims.ImsResolver(sContext, r14, r0, r15);
            sImsResolver.initPopulateCacheAndStartBind();
            r23 = new int[r0];
            sPhones = new com.android.internal.telephony.Phone[r0];
            sCommandsInterfaces = new com.android.internal.telephony.RIL[r0];
            sTelephonyNetworkFactories = new com.android.internal.telephony.dataconnection.TelephonyNetworkFactory[r0];
            r1 = 0;
     */
    /* JADX WARNING: Missing block: B:15:0x009b, code skipped:
            if (r1 >= r0) goto L_0x00f5;
     */
    /* JADX WARNING: Missing block: B:16:0x009d, code skipped:
            r23[r1] = com.android.internal.telephony.RILConstants.PREFERRED_NETWORK_MODE;
            r2 = LOG_TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Network Mode set to ");
            r3.append(java.lang.Integer.toString(r23[r1]));
            android.telephony.Rlog.i(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            sCommandsInterfaces[r1] = (com.android.internal.telephony.CommandsInterface) com.android.internal.telephony.HwTelephonyFactory.getHwPhoneManager().createHwRil(r8, r23[r1], r13, java.lang.Integer.valueOf(r1));
     */
    /* JADX WARNING: Missing block: B:19:0x00d1, code skipped:
            r1 = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:20:0x00d5, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:21:0x00d6, code skipped:
            r2 = r0;
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            sCommandsInterfaces[r1] = new com.android.internal.telephony.RIL(r8, r23[r1], r13, java.lang.Integer.valueOf(r1));
     */
    /* JADX WARNING: Missing block: B:24:0x00e6, code skipped:
            android.telephony.Rlog.e(LOG_TAG, "Unable to construct custom RIL class", r2);
     */
    /* JADX WARNING: Missing block: B:27:?, code skipped:
            java.lang.Thread.sleep(10000);
     */
    /* JADX WARNING: Missing block: B:32:?, code skipped:
            android.telephony.Rlog.i(LOG_TAG, "Creating SubscriptionController");
            com.android.internal.telephony.SubscriptionController.init(r8, sCommandsInterfaces);
            sUiccController = com.android.internal.telephony.uicc.UiccController.make(r8, sCommandsInterfaces);
            com.android.internal.telephony.HwTelephonyFactory.getHwUiccManager().initHwSubscriptionManager(r8, sCommandsInterfaces);
     */
    /* JADX WARNING: Missing block: B:33:0x011c, code skipped:
            if (r32.getPackageManager().hasSystemFeature("android.hardware.telephony.euicc") == false) goto L_0x012a;
     */
    /* JADX WARNING: Missing block: B:34:0x011e, code skipped:
            sEuiccController = com.android.internal.telephony.euicc.EuiccController.init(r32);
            sEuiccCardController = com.android.internal.telephony.euicc.EuiccCardController.init(r32);
     */
    /* JADX WARNING: Missing block: B:35:0x012a, code skipped:
            r1 = 0;
     */
    /* JADX WARNING: Missing block: B:36:0x012b, code skipped:
            r7 = r1;
     */
    /* JADX WARNING: Missing block: B:37:0x012c, code skipped:
            if (r7 >= r0) goto L_0x01e5;
     */
    /* JADX WARNING: Missing block: B:38:0x012e, code skipped:
            r16 = null;
            r1 = android.telephony.TelephonyManager.getPhoneType(r23[r7]);
            r6 = android.telephony.TelephonyManager.getTelephonyProperty(r7, "persist.radio.last_phone_type", "");
     */
    /* JADX WARNING: Missing block: B:39:0x0145, code skipped:
            if ("CDMA".equals(r6) == false) goto L_0x015f;
     */
    /* JADX WARNING: Missing block: B:40:0x0147, code skipped:
            r1 = 2;
            r2 = LOG_TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("phone type set to lastPhoneType = ");
            r3.append(r6);
            android.telephony.Rlog.i(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:42:0x0165, code skipped:
            if ("GSM".equals(r6) == false) goto L_0x017e;
     */
    /* JADX WARNING: Missing block: B:43:0x0167, code skipped:
            r1 = 1;
            r2 = LOG_TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("phone type set to lastPhoneType = ");
            r3.append(r6);
            android.telephony.Rlog.i(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:44:0x017e, code skipped:
            r5 = r1;
     */
    /* JADX WARNING: Missing block: B:45:0x017f, code skipped:
            if (r5 != r11) goto L_0x01a1;
     */
    /* JADX WARNING: Missing block: B:46:0x0181, code skipped:
            r11 = r5;
            r20 = r6;
            r10 = r7;
            r16 = new com.android.internal.telephony.GsmCdmaPhone(r8, sCommandsInterfaces[r7], sPhoneNotifier, r7, 1, com.android.internal.telephony.TelephonyComponentFactory.getInstance());
     */
    /* JADX WARNING: Missing block: B:47:0x01a1, code skipped:
            r11 = r5;
            r20 = r6;
            r10 = r7;
     */
    /* JADX WARNING: Missing block: B:48:0x01a6, code skipped:
            if (r11 != 2) goto L_0x01be;
     */
    /* JADX WARNING: Missing block: B:49:0x01a8, code skipped:
            r16 = new com.android.internal.telephony.GsmCdmaPhone(r8, sCommandsInterfaces[r10], sPhoneNotifier, r10, 6, com.android.internal.telephony.TelephonyComponentFactory.getInstance());
     */
    /* JADX WARNING: Missing block: B:50:0x01be, code skipped:
            r1 = LOG_TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Creating Phone with type = ");
            r2.append(r11);
            r2.append(" sub = ");
            r2.append(r10);
            android.telephony.Rlog.i(r1, r2.toString());
            sPhones[r10] = r16;
            r1 = r10 + 1;
            r11 = 1;
     */
    /* JADX WARNING: Missing block: B:51:0x01e5, code skipped:
            r2 = 0;
            sPhone = sPhones[0];
            sCommandsInterface = sCommandsInterfaces[0];
            r1 = com.android.internal.telephony.SmsApplication.getDefaultSmsApplication(r8, true);
            r3 = "NONE";
     */
    /* JADX WARNING: Missing block: B:52:0x01fb, code skipped:
            if (r1 == false) goto L_0x0202;
     */
    /* JADX WARNING: Missing block: B:53:0x01fd, code skipped:
            r3 = r1.getPackageName();
     */
    /* JADX WARNING: Missing block: B:54:0x0202, code skipped:
            r4 = LOG_TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("defaultSmsApplication: ");
            r5.append(r3);
            android.telephony.Rlog.i(r4, r5.toString());
            com.android.internal.telephony.SmsApplication.initSmsPackageMonitor(r32);
            sMadeDefaults = true;
            android.telephony.Rlog.i(LOG_TAG, "Creating SubInfoRecordUpdater ");
            sSubInfoRecordUpdater = new com.android.internal.telephony.SubscriptionInfoUpdater(com.android.internal.os.BackgroundThread.get().getLooper(), r8, sPhones, sCommandsInterfaces);
            com.android.internal.telephony.SubscriptionController.getInstance().updatePhonesAvailability(sPhones);
            r4 = 0;
     */
    /* JADX WARNING: Missing block: B:55:0x0242, code skipped:
            if (r4 >= r0) goto L_0x024e;
     */
    /* JADX WARNING: Missing block: B:56:0x0244, code skipped:
            sPhones[r4].startMonitoringImsService();
            r4 = r4 + 1;
     */
    /* JADX WARNING: Missing block: B:57:0x024e, code skipped:
            r4 = com.android.internal.telephony.ITelephonyRegistry.Stub.asInterface(android.os.ServiceManager.getService("telephony.registry"));
            r5 = com.android.internal.telephony.SubscriptionController.getInstance();
            sSubscriptionMonitor = new com.android.internal.telephony.SubscriptionMonitor(r4, sContext, r5, r0);
            r10 = r14;
            r11 = r15;
            sPhoneSwitcher = new com.android.internal.telephony.PhoneSwitcher(MAX_ACTIVE_PHONES, r0, sContext, r5, android.os.Looper.myLooper(), r4, sCommandsInterfaces, sPhones);
            sProxyController = com.android.internal.telephony.ProxyController.getInstance(r8, sPhones, sUiccController, sCommandsInterfaces, sPhoneSwitcher);
            sIntentBroadcaster = com.android.internal.telephony.IntentBroadcaster.getInstance(r32);
            sNotificationChannelController = new com.android.internal.telephony.util.NotificationChannelController(r8);
            sTelephonyNetworkFactories = new com.android.internal.telephony.dataconnection.TelephonyNetworkFactory[r0];
     */
    /* JADX WARNING: Missing block: B:58:0x02a2, code skipped:
            if (r2 >= r0) goto L_0x02c8;
     */
    /* JADX WARNING: Missing block: B:59:0x02a4, code skipped:
            sTelephonyNetworkFactories[r2] = new com.android.internal.telephony.dataconnection.TelephonyNetworkFactory(sPhoneSwitcher, r5, sSubscriptionMonitor, android.os.Looper.myLooper(), sContext, r2, sPhones[r2].mDcTracker);
            r2 = r2 + 1;
     */
    /* JADX WARNING: Missing block: B:60:0x02c8, code skipped:
            com.android.internal.telephony.vsim.VSimUtilsInner.makeVSimPhoneFactory(r8, sPhoneNotifier, sPhones, sCommandsInterfaces);
            com.android.internal.telephony.HwTelephonyFactory.getHwPhoneManager().loadHuaweiPhoneService(sPhones, sContext);
            android.telephony.Rlog.i(LOG_TAG, "initHwTimeZoneUpdater");
            com.android.internal.telephony.HwTelephonyFactory.getHwPhoneManager().initHwTimeZoneUpdater(sContext);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void makeDefaultPhone(Context context) {
        Context context2 = context;
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                sContext = context2;
                TelephonyDevController.create();
                int retryCount = 0;
                while (true) {
                    boolean hasException = false;
                    int i = 1;
                    int retryCount2 = retryCount + 1;
                    try {
                        LocalServerSocket localServerSocket = new LocalServerSocket("com.android.internal.telephony");
                    } catch (IOException e) {
                        hasException = true;
                    }
                    if (!hasException) {
                        break;
                    } else if (retryCount2 <= 3) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e2) {
                        }
                        retryCount = retryCount2;
                    } else {
                        throw new RuntimeException("PhoneFactory probably already running");
                    }
                }
            }
        }
    }

    public static Phone getDefaultPhone() {
        Phone phone;
        synchronized (sLockProxyPhones) {
            if (sMadeDefaults) {
                phone = sPhone;
            } else {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
        }
        return phone;
    }

    public static Phone getPhone(int phoneId) {
        Phone phone;
        String dbgInfo = "";
        synchronized (sLockProxyPhones) {
            if (sMadeDefaults) {
                if (VSimUtilsInner.isVSimSub(phoneId)) {
                    dbgInfo = "phoneId == SUB_VSIM return sVSimPhone";
                    phone = VSimUtilsInner.getVSimPhone();
                } else {
                    if (phoneId != KeepaliveStatus.INVALID_HANDLE) {
                        if (phoneId != -1) {
                            phone = (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) ? null : sPhones[phoneId];
                        }
                    }
                    phone = sPhone;
                }
            } else {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
        }
        return phone;
    }

    public static Phone[] getPhones() {
        Phone[] phoneArr;
        synchronized (sLockProxyPhones) {
            if (sMadeDefaults) {
                phoneArr = sPhones;
            } else {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
        }
        return phoneArr;
    }

    public static SubscriptionInfoUpdater getSubscriptionInfoUpdater() {
        return sSubInfoRecordUpdater;
    }

    public static ImsResolver getImsResolver() {
        return sImsResolver;
    }

    public static SipPhone makeSipPhone(String sipUri) {
        return SipPhoneFactory.makePhone(sipUri, sContext, sPhoneNotifier);
    }

    public static int calculatePreferredNetworkType(Context context, int phoneSubId) {
        int networkType = context.getContentResolver();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("preferred_network_mode");
        stringBuilder.append(phoneSubId);
        networkType = Global.getInt(networkType, stringBuilder.toString(), -1);
        String str = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("calculatePreferredNetworkType: phoneSubId = ");
        stringBuilder2.append(phoneSubId);
        stringBuilder2.append(" networkType = ");
        stringBuilder2.append(networkType);
        Rlog.d(str, stringBuilder2.toString());
        if (networkType != -1) {
            return networkType;
        }
        networkType = RILConstants.PREFERRED_NETWORK_MODE;
        try {
            return TelephonyManager.getIntAtIndex(context.getContentResolver(), "preferred_network_mode", SubscriptionController.getInstance().getPhoneId(phoneSubId));
        } catch (SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Value At Index for Settings.Global.PREFERRED_NETWORK_MODE");
            return networkType;
        }
    }

    public static int getDefaultSubscription() {
        return SubscriptionController.getInstance().getDefaultSubId();
    }

    public static boolean isSMSPromptEnabled() {
        boolean z = false;
        int value = 0;
        try {
            value = Global.getInt(sContext.getContentResolver(), "multi_sim_sms_prompt");
        } catch (SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Prompt Values");
        }
        if (value != 0) {
            z = true;
        }
        boolean prompt = z;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SMS Prompt option:");
        stringBuilder.append(prompt);
        Rlog.d(str, stringBuilder.toString());
        return prompt;
    }

    public static Phone makeImsPhone(PhoneNotifier phoneNotifier, Phone defaultPhone) {
        return ImsPhoneFactory.makePhone(sContext, phoneNotifier, defaultPhone);
    }

    public static SubscriptionInfoUpdater getSubInfoRecordUpdater() {
        return sSubInfoRecordUpdater;
    }

    public static void requestEmbeddedSubscriptionInfoListRefresh(Runnable callback) {
        sSubInfoRecordUpdater.requestEmbeddedSubscriptionInfoListRefresh(callback);
    }

    public static void addLocalLog(String key, int size) {
        synchronized (sLocalLogs) {
            if (sLocalLogs.containsKey(key)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("key ");
                stringBuilder.append(key);
                stringBuilder.append(" already present");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            sLocalLogs.put(key, new LocalLog(size));
        }
    }

    public static void localLog(String key, String log) {
        synchronized (sLocalLogs) {
            if (sLocalLogs.containsKey(key)) {
                ((LocalLog) sLocalLogs.get(key)).log(log);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("key ");
                stringBuilder.append(key);
                stringBuilder.append(" not found");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    public static void dump(FileDescriptor fd, PrintWriter printwriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printwriter, "  ");
        pw.println("PhoneFactory:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" sMadeDefaults=");
        stringBuilder.append(sMadeDefaults);
        pw.println(stringBuilder.toString());
        sPhoneSwitcher.dump(fd, pw, args);
        pw.println();
        Phone[] phones = getPhones();
        for (int i = 0; i < phones.length; i++) {
            pw.increaseIndent();
            Phone phone = phones[i];
            try {
                phone.dump(fd, pw, args);
                pw.flush();
                pw.println("++++++++++++++++++++++++++++++++");
                sTelephonyNetworkFactories[i].dump(fd, pw, args);
                pw.flush();
                pw.println("++++++++++++++++++++++++++++++++");
                try {
                    UiccProfile uiccProfile = (UiccProfile) phone.getIccCard();
                    if (uiccProfile != null) {
                        uiccProfile.dump(fd, pw, args);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pw.flush();
                pw.decreaseIndent();
                pw.println("++++++++++++++++++++++++++++++++");
            } catch (Exception e2) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Telephony DebugService: Could not get Phone[");
                stringBuilder2.append(i);
                stringBuilder2.append("] e=");
                stringBuilder2.append(e2);
                pw.println(stringBuilder2.toString());
            }
        }
        pw.println("SubscriptionMonitor:");
        pw.increaseIndent();
        try {
            sSubscriptionMonitor.dump(fd, pw, args);
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");
        pw.println("UiccController:");
        pw.increaseIndent();
        try {
            sUiccController.dump(fd, pw, args);
        } catch (Exception e32) {
            e32.printStackTrace();
        }
        pw.flush();
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");
        if (sEuiccController != null) {
            pw.println("EuiccController:");
            pw.increaseIndent();
            try {
                sEuiccController.dump(fd, pw, args);
                sEuiccCardController.dump(fd, pw, args);
            } catch (Exception e322) {
                e322.printStackTrace();
            }
            pw.flush();
            pw.decreaseIndent();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        pw.println("SubscriptionController:");
        pw.increaseIndent();
        try {
            SubscriptionController.getInstance().dump(fd, pw, args);
        } catch (Exception e3222) {
            e3222.printStackTrace();
        }
        pw.flush();
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");
        pw.println("SubInfoRecordUpdater:");
        pw.increaseIndent();
        try {
            sSubInfoRecordUpdater.dump(fd, pw, args);
        } catch (Exception e32222) {
            e32222.printStackTrace();
        }
        pw.flush();
        pw.decreaseIndent();
        VSimUtilsInner.dumpVSimPhoneFactory(fd, pw, args);
        pw.println("++++++++++++++++++++++++++++++++");
        pw.println("LocalLogs:");
        pw.increaseIndent();
        synchronized (sLocalLogs) {
            for (String key : sLocalLogs.keySet()) {
                pw.println(key);
                pw.increaseIndent();
                ((LocalLog) sLocalLogs.get(key)).dump(fd, pw, args);
                pw.decreaseIndent();
            }
            pw.flush();
        }
        pw.decreaseIndent();
        pw.println("++++++++++++++++++++++++++++++++");
        if (Log.HWINFO) {
            pw.println("SharedPreferences:");
            pw.increaseIndent();
            try {
                if (sContext != null) {
                    Map spValues = PreferenceManager.getDefaultSharedPreferences(sContext).getAll();
                    for (Object key2 : spValues.keySet()) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(key2);
                        stringBuilder3.append(" : ");
                        stringBuilder3.append(spValues.get(key2));
                        pw.println(stringBuilder3.toString());
                    }
                }
            } catch (Exception e322222) {
                e322222.printStackTrace();
            }
            pw.flush();
            pw.decreaseIndent();
        }
    }

    public static int getTopPrioritySubscriptionId() {
        if (sPhoneSwitcher != null) {
            return sPhoneSwitcher.getTopPrioritySubscriptionId();
        }
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    public static int onDataSubChange(int what, Handler handler) {
        if (sPhoneSwitcher != null) {
            return sPhoneSwitcher.onDataSubChange(what, handler);
        }
        return 0;
    }

    public static void resendDataAllowed(int phoneId) {
        if (sPhoneSwitcher != null) {
            sPhoneSwitcher.resendDataAllowed(phoneId);
        }
    }

    public static TelephonyNetworkFactory getTelephonyNetworkFactory(int phoneId) {
        if (sTelephonyNetworkFactories != null) {
            return sTelephonyNetworkFactories[phoneId];
        }
        return null;
    }

    public static boolean getInitState() {
        boolean z;
        synchronized (sLockProxyPhones) {
            z = sMadeDefaults;
        }
        return z;
    }
}
