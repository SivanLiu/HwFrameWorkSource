package com.android.internal.telephony;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.ContactsContract;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Intents;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SmsMessage.SubmitPdu;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.SmsMessageBase.SubmitPduBase;
import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.cdma.HwCdmaSMSDispatcher;
import com.android.internal.telephony.cdma.HwSmsMessage;
import com.android.internal.telephony.cdma.sms.HwBearerData;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.android.internal.telephony.gsm.GsmSMSDispatcher;
import com.android.internal.telephony.gsm.HwGsmSMSDispatcher;
import com.android.internal.telephony.gsm.SmsMessage;
import com.android.internal.telephony.gsm.SmsMessageUtils;
import com.huawei.utils.reflect.HwReflectUtils;
import huawei.android.app.admin.HwDevicePolicyManagerEx;
import huawei.android.telephony.CallerInfoHW;
import huawei.android.telephony.wrapper.WrapperFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.PatternSyntaxException;

public class HwInnerSmsManagerImpl implements HwInnerSmsManager {
    private static final String BLOCK_TYPE = "BLOCKED_TYPE";
    private static final String BLUETOOTH_PACKAGE_NAME = "com.android.bluetooth";
    private static final Class<?> CLASS_ContentType = HwReflectUtils.getClass("com.google.android.mms.ContentType");
    private static final Class<?> CLASS_EncodedStringValue = HwReflectUtils.getClass("com.google.android.mms.pdu.EncodedStringValue");
    private static final Class<?> CLASS_GenericPdu = HwReflectUtils.getClass("com.google.android.mms.pdu.GenericPdu");
    private static final Class<?> CLASS_PduHeaders = HwReflectUtils.getClass("com.google.android.mms.pdu.PduHeaders");
    private static final Class<?> CLASS_PduParser = HwReflectUtils.getClass("com.google.android.mms.pdu.PduParser");
    private static final String COLUMN_IS_PRIVATE = "is_private";
    private static final Constructor<?> CONSTRUCTOR_PduParser = HwReflectUtils.getConstructor(CLASS_PduParser, new Class[]{byte[].class});
    private static final String CONTACT_PACKAGE_NAME = "com.android.contacts";
    private static final String DAY_MODE = "day_mode";
    private static final String DAY_MODE_TIME = "day_mode_time";
    private static final int DECEMBER = 11;
    private static final Field FIELD_MESSAGE_TYPE_NOTIFICATION_IND = HwReflectUtils.getField(CLASS_PduHeaders, "MESSAGE_TYPE_NOTIFICATION_IND");
    private static final Field FIELD_MMS_MESSAGE = HwReflectUtils.getField(CLASS_ContentType, "MMS_MESSAGE");
    private static final String HANDLE_KEY_SMSINTENT = "HANDLE_SMS_INTENT";
    private static final String HANDLE_KEY_WAP_PUSH_INTENT = "HANDLE_WAP_PUSH_INTENT";
    private static final String INCOMING_DAY_LIMIT = "incoming_day_limit";
    private static final String INCOMING_MONTH_LIMIT = "incoming_month_limit";
    private static final String INCOMING_SMS_LIMIT = "incoming_limit";
    private static final String INCOMING_WEEK_LIMIT = "incoming_week_limit";
    private static final String INIT_VALUE = "0";
    private static final boolean IS_CHINATELECOM;
    private static final int LIMIT_INIT_NUM = 0;
    private static final String LIMIT_OF_DAY = "limit_number_day";
    private static final String LIMIT_OF_MONTH = "limit_number_month";
    private static final String LIMIT_OF_WEEK = "limit_number_week";
    private static final String LOG_TAG = "HwInnerSmsManagerImpl";
    private static final int MESSAGE_TYPE_NOTIFICATION_IND = HwReflectUtils.objectToInt(HwReflectUtils.getFieldValue(null, FIELD_MESSAGE_TYPE_NOTIFICATION_IND));
    private static final Method METHOD_getFrom = HwReflectUtils.getMethod(CLASS_GenericPdu, "getFrom", new Class[0]);
    private static final Method METHOD_getMessageType = HwReflectUtils.getMethod(CLASS_GenericPdu, "getMessageType", new Class[0]);
    private static final Method METHOD_getString = HwReflectUtils.getMethod(CLASS_EncodedStringValue, "getString", new Class[0]);
    private static final Method METHOD_parse = HwReflectUtils.getMethod(CLASS_PduParser, "parse", new Class[0]);
    private static final String MMS_ACTIVITY_NAME = "com.android.mms.ui.ComposeMessageActivity";
    private static final String MMS_MESSAGE = HwReflectUtils.objectToString(HwReflectUtils.getFieldValue(null, FIELD_MMS_MESSAGE));
    private static final String MMS_PACKAGE_NAME = "com.android.mms";
    private static final String MODE_INIT_VALUE = "false";
    private static final String MONTH_MODE = "month_mode";
    private static final String MONTH_MODE_TIME = "month_mode_time";
    static int Message_Reference_Num = 0;
    private static final String OUTGOING_DAY_LIMIT = "outgoing_day_limit";
    private static final String OUTGOING_MONTH_LIMIT = "outgoing_month_limit";
    private static final String OUTGOING_SMS_LIMIT = "outgoing_limit";
    private static final String OUTGOING_WEEK_LIMIT = "outgoing_week_limit";
    private static final String POLICY_KEY = "value";
    private static int SUBSCRIPTION_FOR_NONE_VARIABLE_METHODS = -1;
    private static final Uri URI_PRIVATE_NUMBER = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "private_contacts");
    private static final String USED_OF_DAY = "used_number_day";
    private static final String USED_OF_MONTH = "used_number_month";
    private static final String USED_OF_WEEK = "used_number_week";
    private static final String WEEK_MODE = "week_mode";
    private static final String WEEK_MODE_TIME = "week_mode_time";
    private static SmsMessageUtils gsmSmsMessageUtils = new SmsMessageUtils();
    private static HwInnerSmsManager mInstance = new HwInnerSmsManagerImpl();
    private static final Uri sRawUri = Uri.withAppendedPath(Sms.CONTENT_URI, "raw");
    private HwDevicePolicyManagerEx mDpm = new HwDevicePolicyManagerEx();
    private KeyguardManager mKeyguardManager;
    private SmsInterceptionService mSmsInterceptionService = null;

    static {
        boolean z = true;
        if (!(SystemProperties.get("ro.config.hw_opta", INIT_VALUE).equals("92") && SystemProperties.get("ro.config.hw_optb", INIT_VALUE).equals("156"))) {
            z = false;
        }
        IS_CHINATELECOM = z;
    }

    public static HwInnerSmsManager getDefault() {
        return mInstance;
    }

    private static void setMessageReferenceNum(int value) {
        Message_Reference_Num = value;
    }

    public int getMessageRefrenceNumber() {
        if (HwSubscriptionManager.SUB_INIT_STATE <= Message_Reference_Num) {
            setMessageReferenceNum(0);
        } else {
            setMessageReferenceNum(Message_Reference_Num + 1);
        }
        return Message_Reference_Num;
    }

    public byte[] getUserDataHeaderForGsm(int seqNum, int maxNum, int MessageReferenceNum) {
        byte[] UDH = new byte[5];
        if (seqNum > maxNum) {
            return null;
        }
        UDH[0] = (byte) 0;
        UDH[1] = (byte) 3;
        UDH[2] = (byte) MessageReferenceNum;
        UDH[3] = (byte) maxNum;
        UDH[4] = (byte) seqNum;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("maxNum:");
        stringBuilder.append(maxNum);
        stringBuilder.append(";seqNum:");
        stringBuilder.append(seqNum);
        stringBuilder.append(";MR:");
        stringBuilder.append(UDH[2]);
        Rlog.d("android/SmsMessage", stringBuilder.toString());
        return UDH;
    }

    public SubmitPdu getSubmitPdu(String scAddress, String timeStamps, String destinationAddress, String message, byte[] UDH) {
        return getSubmitPdu(scAddress, timeStamps, destinationAddress, message, UDH, SUBSCRIPTION_FOR_NONE_VARIABLE_METHODS);
    }

    public SubmitPdu getSubmitPdu(String scAddress, String timeStamps, String destinationAddress, String message, byte[] UDH, int subscription) {
        int activePhone;
        SubmitPduBase spb;
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            activePhone = TelephonyManager.getDefault().getCurrentPhoneType(subscription);
        } else {
            activePhone = TelephonyManager.getDefault().getPhoneType();
        }
        if (2 == activePhone) {
            SmsHeader header = null;
            if (UDH != null) {
                header = SmsHeader.fromByteArray(UDH);
            }
            spb = HwSmsMessage.getSubmitDeliverPdu(true, null, destinationAddress, message, header);
        } else {
            spb = SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, false, UDH);
        }
        if (spb == null) {
            return null;
        }
        return new SubmitPdu(spb);
    }

    public SubmitPdu getDeliverPdu(String scAddress, String scTimeStamp, String origAddress, String message, byte[] UDH) {
        return getDeliverPdu(scAddress, scTimeStamp, origAddress, message, UDH, SUBSCRIPTION_FOR_NONE_VARIABLE_METHODS);
    }

    public SubmitPdu getDeliverPdu(String scAddress, String scTimeStamp, String origAddress, String message, byte[] UDH, int subscription) {
        int activePhone;
        SubmitPduBase spb;
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            activePhone = TelephonyManager.getDefault().getCurrentPhoneType(subscription);
        } else {
            activePhone = TelephonyManager.getDefault().getPhoneType();
        }
        if (2 == activePhone) {
            spb = null;
            if (UDH != null) {
                spb = SmsHeader.fromByteArray(UDH);
            }
            spb = HwSmsMessage.getSubmitDeliverPdu(false, scTimeStamp, origAddress, message, spb);
        } else {
            spb = com.android.internal.telephony.gsm.HwSmsMessage.getDeliverPdu(scAddress, scTimeStamp, origAddress, message, UDH);
        }
        if (spb == null) {
            return null;
        }
        return new SubmitPdu(spb);
    }

    public android.telephony.SmsMessage createFromEfRecord(int index, byte[] data, int subscription) {
        SmsMessageBase wrappedMessage;
        if (WrapperFactory.getMSimTelephonyManagerWrapper().getCurrentPhoneType(subscription) == 2) {
            wrappedMessage = com.android.internal.telephony.cdma.SmsMessage.createFromEfRecord(index, data);
        } else {
            wrappedMessage = SmsMessage.createFromEfRecord(index, data);
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createFromEfRecord(): wrappedMessage=");
        stringBuilder.append(wrappedMessage);
        Rlog.e(str, stringBuilder.toString());
        return wrappedMessage != null ? new android.telephony.SmsMessage(wrappedMessage) : null;
    }

    public WspTypeDecoder createHwWspTypeDecoder(byte[] pdu) {
        return new HwWspTypeDecoder(pdu);
    }

    public boolean handleWapPushExtraMimeType(String mimeType) {
        return HwWspTypeDecoder.CONTENT_TYPE_B_CONNECT_WBXML.endsWith(mimeType);
    }

    public WapPushOverSms createHwWapPushOverSms(Phone phone, SMSDispatcher smsDispatcher) {
        return new HwWapPushOverSms(phone, smsDispatcher);
    }

    public WapPushOverSms createHwWapPushOverSms(Context context) {
        return new HwWapPushOverSms(context);
    }

    public IccSmsInterfaceManager createHwIccSmsInterfaceManager(Phone phone) {
        return new HwIccSmsInterfaceManager(phone);
    }

    public GsmSMSDispatcher createHwGsmSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController, GsmInboundSmsHandler gsmInboundSmsHandler) {
        return new HwGsmSMSDispatcher(phone, smsDispatchersController, gsmInboundSmsHandler);
    }

    public CdmaSMSDispatcher createHwCdmaSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        return new HwCdmaSMSDispatcher(phone, smsDispatchersController);
    }

    public TextEncodingDetails calcTextEncodingDetailsEx(CharSequence msg, boolean force7BitEncoding) {
        return HwBearerData.calcTextEncodingDetailsEx(msg, force7BitEncoding);
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:0x00d8  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean shouldSendReceivedActionInPrivacyMode(InboundSmsHandler handler, Context context, Intent intent, String deleteWhere, String[] deleteWhereArgs) {
        Exception ex;
        String str;
        StringBuilder stringBuilder;
        InboundSmsHandler inboundSmsHandler;
        String[] strArr;
        Context context2 = context;
        this.mKeyguardManager = (KeyguardManager) context2.getSystemService("keyguard");
        String action = intent.getAction();
        String number = null;
        boolean z = SystemProperties.getBoolean("ro.config.hw_privacymode", false) && 1 == Secure.getInt(context.getContentResolver(), "privacy_mode_on", 0) && (1 == Secure.getInt(context.getContentResolver(), "privacy_mode_state", 0) || (this.mKeyguardManager != null && this.mKeyguardManager.isKeyguardLocked()));
        Intent intent2;
        if (z) {
            if ("android.provider.Telephony.SMS_DELIVER".equals(action)) {
                number = Intents.getMessagesFromIntent(intent)[0].getOriginatingAddress();
                intent2 = intent;
            } else if ("android.provider.Telephony.WAP_PUSH_DELIVER".equals(action) && MMS_MESSAGE.equals(intent.getType())) {
                try {
                    try {
                        byte[] pushData = intent.getByteArrayExtra("data");
                        Object pdu = HwReflectUtils.invoke(HwReflectUtils.newInstance(CONSTRUCTOR_PduParser, new Object[]{pushData}), METHOD_parse, new Object[0]);
                        if (pdu != null && MESSAGE_TYPE_NOTIFICATION_IND == ((Integer) HwReflectUtils.invoke(pdu, METHOD_getMessageType, new Object[0])).intValue()) {
                            Object fromValue = HwReflectUtils.invoke(pdu, METHOD_getFrom, new Object[0]);
                            if (fromValue != null) {
                                number = (String) HwReflectUtils.invoke(fromValue, METHOD_getString, new Object[0]);
                            }
                        }
                    } catch (Exception e) {
                        ex = e;
                        str = LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("get mms original number cause exception!");
                        stringBuilder.append(ex.toString());
                        Rlog.e(str, stringBuilder.toString());
                        if (number == null) {
                        }
                        inboundSmsHandler = handler;
                        str = deleteWhere;
                        strArr = deleteWhereArgs;
                        return true;
                    }
                } catch (Exception e2) {
                    ex = e2;
                    intent2 = intent;
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("get mms original number cause exception!");
                    stringBuilder.append(ex.toString());
                    Rlog.e(str, stringBuilder.toString());
                    if (number == null) {
                    }
                    inboundSmsHandler = handler;
                    str = deleteWhere;
                    strArr = deleteWhereArgs;
                    return true;
                }
            } else {
                inboundSmsHandler = handler;
                intent2 = intent;
                str = deleteWhere;
                strArr = deleteWhereArgs;
                Rlog.d(LOG_TAG, "ignore action, do nothing");
                return true;
            }
            if (number == null && isPrivacyNumber(context2, number)) {
                Rlog.d(LOG_TAG, "sms originating address: xxxxxx is private, do not send received action.");
                deleteFromRawTable(context2, deleteWhere, deleteWhereArgs);
                handler.sendMessage(3);
                return false;
            }
            inboundSmsHandler = handler;
            str = deleteWhere;
            strArr = deleteWhereArgs;
            return true;
        }
        inboundSmsHandler = handler;
        intent2 = intent;
        str = deleteWhere;
        strArr = deleteWhereArgs;
        return true;
    }

    /* JADX WARNING: Missing block: B:11:0x0036, code skipped:
            if (r7 != null) goto L_0x0038;
     */
    /* JADX WARNING: Missing block: B:12:0x0038, code skipped:
            r7.close();
     */
    /* JADX WARNING: Missing block: B:17:0x0059, code skipped:
            if (r7 == null) goto L_0x005c;
     */
    /* JADX WARNING: Missing block: B:18:0x005c, code skipped:
            r1 = LOG_TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("The number is private or not: ");
     */
    /* JADX WARNING: Missing block: B:19:0x0068, code skipped:
            if (r9 == false) goto L_0x006d;
     */
    /* JADX WARNING: Missing block: B:20:0x006a, code skipped:
            r3 = "Yes";
     */
    /* JADX WARNING: Missing block: B:21:0x006d, code skipped:
            r3 = "No";
     */
    /* JADX WARNING: Missing block: B:22:0x006f, code skipped:
            r2.append(r3);
            android.telephony.Rlog.d(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:23:0x0079, code skipped:
            return r9;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isPrivacyNumber(Context context, String number) {
        Uri queryUri = URI_PRIVATE_NUMBER.buildUpon().appendEncodedPath(number).build();
        Cursor c = null;
        boolean isPrivate = false;
        try {
            c = context.getContentResolver().query(queryUri, new String[]{COLUMN_IS_PRIVATE}, null, null, null);
            if (c != null && c.moveToFirst()) {
                boolean z = true;
                if (c.getInt(0) != 1) {
                    z = false;
                }
                isPrivate = z;
            }
        } catch (Exception ex) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isPrivacyNumber cause exception!");
            stringBuilder.append(ex.toString());
            Rlog.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }

    private void deleteFromRawTable(Context context, String deleteWhere, String[] deleteWhereArgs) {
        int rows = context.getContentResolver().delete(sRawUri, deleteWhere, deleteWhereArgs);
        if (rows == 0) {
            Rlog.e(LOG_TAG, "No rows were deleted from raw table!");
            return;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Deleted ");
        stringBuilder.append(rows);
        stringBuilder.append(" rows from raw table.");
        Rlog.d(str, stringBuilder.toString());
    }

    public boolean useCdmaFormatForMoSms() {
        return android.telephony.SmsMessageUtils.useCdmaFormatForMoSms(SUBSCRIPTION_FOR_NONE_VARIABLE_METHODS);
    }

    public ArrayList<String> fragmentText(String text) {
        return android.telephony.SmsMessage.fragmentText(text, SUBSCRIPTION_FOR_NONE_VARIABLE_METHODS);
    }

    public boolean isCdmaVoice() {
        return android.telephony.SmsMessageUtils.isCdmaVoice(SUBSCRIPTION_FOR_NONE_VARIABLE_METHODS);
    }

    public void createSmsInterceptionService(Context context) {
        Rlog.d(LOG_TAG, "createSmsInterceptionService ...");
        this.mSmsInterceptionService = SmsInterceptionService.getDefault(context);
    }

    public boolean newSmsShouldBeIntercepted(Context context, Intent intent, InboundSmsHandler handler, String deleteWhere, String[] deleteWhereArgs, boolean isWapPush) {
        try {
            Bundle smsInfo = new Bundle();
            if (isWapPush) {
                smsInfo.putParcelable(HANDLE_KEY_WAP_PUSH_INTENT, intent);
            } else {
                smsInfo.putParcelable(HANDLE_KEY_SMSINTENT, intent);
            }
            if (this.mSmsInterceptionService.dispatchNewSmsToInterceptionProcess(smsInfo, isWapPush)) {
                Rlog.d(LOG_TAG, "sms is intercepted ...");
                deleteFromRawTable(context, deleteWhere, deleteWhereArgs);
                handler.sendMessage(3);
                return true;
            }
        } catch (Exception e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Get exception while communicate with sms interception service: ");
            stringBuilder.append(e.getMessage());
            Rlog.e(str, stringBuilder.toString());
        }
        return false;
    }

    public boolean isMatchSMSPattern(String address, String policyTag, int phoneId) {
        String str;
        StringBuilder stringBuilder;
        if (address == null || TextUtils.isEmpty(policyTag)) {
            Rlog.d(LOG_TAG, "address or policyTag is null!");
            return false;
        }
        Bundle bundle = this.mDpm.getPolicy(null, policyTag);
        if (bundle == null) {
            Rlog.d(LOG_TAG, "bundle is null!");
            return false;
        }
        String pattern = bundle.getString(POLICY_KEY);
        if (TextUtils.isEmpty(pattern)) {
            Rlog.d(LOG_TAG, "pattern is null!");
            return false;
        }
        boolean isMatchPattern = false;
        String countryIso = null;
        String formatNum = null;
        if (TelephonyManager.getDefault() != null) {
            countryIso = TelephonyManager.getDefault().getNetworkCountryIso(phoneId);
        }
        String str2 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("countryIso : ");
        stringBuilder2.append(countryIso);
        Rlog.d(str2, stringBuilder2.toString());
        if (countryIso != null) {
            formatNum = PhoneNumberUtils.formatNumberToE164(address, countryIso.toUpperCase(Locale.getDefault()));
            int len = CallerInfoHW.getInstance().getIntlPrefixAndCCLen(formatNum);
            if (len <= 0 || formatNum == null) {
                Rlog.d(LOG_TAG, "can not format address");
                formatNum = address;
            } else {
                formatNum = formatNum.substring(len);
            }
        }
        if (!TextUtils.isEmpty(formatNum)) {
            try {
                isMatchPattern = formatNum.matches(pattern);
            } catch (PatternSyntaxException e) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("pattern has exception : ");
                stringBuilder.append(e);
                Rlog.d(str, stringBuilder.toString());
                return false;
            }
        }
        if (!isMatchPattern) {
            try {
                isMatchPattern = address.matches(pattern);
            } catch (PatternSyntaxException e2) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("pattern has exception match address: ");
                stringBuilder.append(e2);
                Rlog.d(str, stringBuilder.toString());
                return false;
            }
        }
        String str3 = LOG_TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("isMatchPattern : ");
        stringBuilder3.append(isMatchPattern);
        Rlog.d(str3, stringBuilder3.toString());
        return isMatchPattern;
    }

    public void sendGoogleSmsBlockedRecord(Intent intent) {
        try {
            Bundle smsInfo = new Bundle();
            if (intent.getBooleanExtra("isWapPush", false)) {
                smsInfo.putParcelable(HANDLE_KEY_WAP_PUSH_INTENT, intent);
                smsInfo.putInt(BLOCK_TYPE, 3);
            } else {
                smsInfo.putParcelable(HANDLE_KEY_SMSINTENT, intent);
                smsInfo.putInt(BLOCK_TYPE, 2);
            }
            if (this.mSmsInterceptionService != null) {
                this.mSmsInterceptionService.sendNumberBlockedRecord(smsInfo);
            }
        } catch (Exception e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Get exception while communicate with sms interception service: ");
            stringBuilder.append(e.getMessage());
            Rlog.e(str, stringBuilder.toString());
        }
    }

    private String getAppNameByPid(int pid, Context context) {
        String processName = "";
        for (RunningAppProcessInfo appInfo : ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses()) {
            if (pid == appInfo.pid) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pid: ");
                stringBuilder.append(appInfo.pid);
                stringBuilder.append(" processName: ");
                stringBuilder.append(appInfo.processName);
                Rlog.d(str, stringBuilder.toString());
                return appInfo.processName;
            }
        }
        return processName;
    }

    public PackageInfo getPackageInfoByPid(PackageInfo appInfo, PackageManager pm, String[] packageNames, Context context) {
        if (packageNames == null || packageNames.length <= 1) {
            return appInfo;
        }
        try {
            String packageNameByPid = getAppNameByPid(Binder.getCallingPid(), context);
            if ("".equals(packageNameByPid)) {
                return appInfo;
            }
            return pm.getPackageInfo(packageNameByPid, 64);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return appInfo;
        }
    }

    public boolean checkShouldWriteSmsPackage(String packageName, Context context) {
        boolean ret = false;
        int i = 0;
        if (packageName == null || "".equals(packageName)) {
            Rlog.e(LOG_TAG, "checkShouldWriteSmsPackage packageName not exist");
            return false;
        }
        String defaultSmsPackage = null;
        ComponentName component = SmsApplication.getDefaultSmsApplication(context, false);
        if (component != null) {
            defaultSmsPackage = component.getPackageName();
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkShouldWriteSmsPackage defaultSmsPackage: ");
        stringBuilder.append(defaultSmsPackage);
        stringBuilder.append(", current package: ");
        stringBuilder.append(packageName);
        Rlog.d(str, stringBuilder.toString());
        if ((defaultSmsPackage != null && defaultSmsPackage.equals(packageName)) || packageName.equals(BLUETOOTH_PACKAGE_NAME)) {
            return false;
        }
        str = null;
        try {
            str = System.getString(context.getContentResolver(), "should_write_sms_package");
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("checkShouldWriteSmsPackage cust: ");
            stringBuilder2.append(str);
            Rlog.d(str2, stringBuilder2.toString());
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Exception when got name value", e);
        }
        if (str == null || "".equals(str)) {
            str = "com.huawei.vassistant,com.hellotext.hello,com.android.emergency";
        }
        String[] shouldWriteSmsPackageArray = str.split(",");
        int length = shouldWriteSmsPackageArray.length;
        while (i < length) {
            if (packageName.equalsIgnoreCase(shouldWriteSmsPackageArray[i])) {
                ret = true;
                break;
            }
            i++;
        }
        return ret;
    }

    public boolean currentSubIsChinaTelecomSim(int phoneId) {
        return IS_CHINATELECOM || HwTelephonyManagerInner.getDefault().isCTSimCard(phoneId);
    }

    public boolean isLimitNumOfSmsEnabled(boolean isOutgoing) {
        return isDayMode(isOutgoing) || isWeekMode(isOutgoing) || isMonthMode(isOutgoing);
    }

    public void updateSmsUsedNum(Context context, boolean isOutgoing) {
        Context context2 = context;
        boolean z = isOutgoing;
        if (context2 == null) {
            Rlog.e(LOG_TAG, "context is null");
            return;
        }
        String policyName;
        if (z) {
            policyName = OUTGOING_SMS_LIMIT;
        } else {
            policyName = INCOMING_SMS_LIMIT;
        }
        SharedPreferences sharedPreferences = context2.getSharedPreferences(policyName, 0);
        if (sharedPreferences == null) {
            Rlog.e(LOG_TAG, "get sharedPreferences failed");
            return;
        }
        Editor editor = sharedPreferences.edit();
        long nowTime = System.currentTimeMillis();
        long shareTimeDay = sharedPreferences.getLong(DAY_MODE_TIME, getStartTime(z, DAY_MODE));
        if (isSameDay(nowTime, shareTimeDay)) {
            editor.putInt(USED_OF_DAY, sharedPreferences.getInt(USED_OF_DAY, 0) + 1);
            editor.putLong(DAY_MODE_TIME, shareTimeDay);
        } else {
            editor.putInt(USED_OF_DAY, 1);
            editor.putLong(DAY_MODE_TIME, nowTime);
        }
        long shareTimeWeek = sharedPreferences.getLong(WEEK_MODE_TIME, getStartTime(z, WEEK_MODE));
        if (isSameWeek(nowTime, shareTimeWeek)) {
            editor.putInt(USED_OF_WEEK, sharedPreferences.getInt(USED_OF_WEEK, 0) + 1);
            editor.putLong(WEEK_MODE_TIME, shareTimeWeek);
        } else {
            editor.putInt(USED_OF_WEEK, 1);
            editor.putLong(WEEK_MODE_TIME, nowTime);
        }
        shareTimeDay = sharedPreferences.getLong(MONTH_MODE_TIME, getStartTime(z, MONTH_MODE));
        if (isSameMonth(nowTime, shareTimeDay)) {
            editor.putInt(USED_OF_MONTH, sharedPreferences.getInt(USED_OF_MONTH, 0) + 1);
            editor.putLong(MONTH_MODE_TIME, shareTimeDay);
        } else {
            editor.putInt(USED_OF_MONTH, 1);
            editor.putLong(MONTH_MODE_TIME, nowTime);
        }
        editor.apply();
    }

    public boolean isExceedSMSLimit(Context context, boolean isOutgoing) {
        Context context2 = context;
        boolean z = isOutgoing;
        SharedPreferences sharedPreferences = null;
        String policyName = z ? OUTGOING_SMS_LIMIT : INCOMING_SMS_LIMIT;
        if (context2 != null) {
            sharedPreferences = context2.getSharedPreferences(policyName, 0);
        }
        if (sharedPreferences != null) {
            int usedNumOfDay = sharedPreferences.getInt(USED_OF_DAY, 0);
            int usedNumOfWeek = sharedPreferences.getInt(USED_OF_WEEK, 0);
            int usedNumOfMonth = sharedPreferences.getInt(USED_OF_MONTH, 0);
            int limitNumOfDay = getLimitNum(z, DAY_MODE);
            int limitNumOfWeek = getLimitNum(z, WEEK_MODE);
            int limitNumOfMonth = getLimitNum(z, MONTH_MODE);
            long nowTime = System.currentTimeMillis();
            Editor editor = sharedPreferences.edit();
            if (editor == null) {
                Rlog.e(LOG_TAG, "editor is null");
                return false;
            }
            int usedNumOfDay2;
            int usedNumOfWeek2;
            if (isMonthMode(z)) {
                usedNumOfDay2 = usedNumOfDay;
                usedNumOfWeek2 = usedNumOfWeek;
                if (!isSameMonth(nowTime, sharedPreferences.getLong(MONTH_MODE_TIME, getStartTime(z, MONTH_MODE)))) {
                    Rlog.d(LOG_TAG, "it is next month, so reset data");
                    editor.putInt(USED_OF_MONTH, 0);
                    editor.putLong(MONTH_MODE_TIME, nowTime);
                } else if (usedNumOfMonth >= limitNumOfMonth) {
                    return true;
                }
            }
            usedNumOfDay2 = usedNumOfDay;
            usedNumOfWeek2 = usedNumOfWeek;
            if (!isWeekMode(z)) {
            } else if (!isSameWeek(nowTime, sharedPreferences.getLong(WEEK_MODE_TIME, getStartTime(z, WEEK_MODE)))) {
                Rlog.d(LOG_TAG, "it is next week, so reset data");
                editor.putInt(USED_OF_WEEK, 0);
                editor.putLong(WEEK_MODE_TIME, nowTime);
                int i = usedNumOfWeek2;
            } else if (usedNumOfWeek2 >= limitNumOfWeek) {
                return true;
            }
            if (isDayMode(z)) {
                if (!isSameDay(nowTime, sharedPreferences.getLong(DAY_MODE_TIME, getStartTime(z, DAY_MODE)))) {
                    Rlog.d(LOG_TAG, "it is next day, so reset data");
                    editor.putInt(USED_OF_DAY, 0);
                    editor.putLong(DAY_MODE_TIME, nowTime);
                } else if (usedNumOfDay2 >= limitNumOfDay) {
                    return true;
                }
            }
        }
        return false;
    }

    private long getStartTime(boolean isOutgoing, String timeMode) {
        if (TextUtils.isEmpty(timeMode)) {
            return 0;
        }
        Object obj = -1;
        int hashCode = timeMode.hashCode();
        if (hashCode != -2105529586) {
            if (hashCode != -1628741630) {
                if (hashCode == 1931104358 && timeMode.equals(DAY_MODE)) {
                    obj = null;
                }
            } else if (timeMode.equals(MONTH_MODE)) {
                obj = 2;
            }
        } else if (timeMode.equals(WEEK_MODE)) {
            obj = 1;
        }
        String policyName;
        switch (obj) {
            case null:
                if (isOutgoing) {
                    policyName = OUTGOING_DAY_LIMIT;
                } else {
                    policyName = INCOMING_DAY_LIMIT;
                }
                return Long.parseLong(this.mDpm.getPolicy(null, policyName).getString(DAY_MODE_TIME, INIT_VALUE));
            case 1:
                if (isOutgoing) {
                    policyName = OUTGOING_WEEK_LIMIT;
                } else {
                    policyName = INCOMING_WEEK_LIMIT;
                }
                return Long.parseLong(this.mDpm.getPolicy(null, policyName).getString(WEEK_MODE_TIME, INIT_VALUE));
            case 2:
                if (isOutgoing) {
                    policyName = OUTGOING_MONTH_LIMIT;
                } else {
                    policyName = INCOMING_MONTH_LIMIT;
                }
                return Long.parseLong(this.mDpm.getPolicy(null, policyName).getString(MONTH_MODE_TIME, INIT_VALUE));
            default:
                return 0;
        }
    }

    private int getLimitNum(boolean isOutgoing, String timeMode) {
        if (TextUtils.isEmpty(timeMode)) {
            return 0;
        }
        Object obj = -1;
        int hashCode = timeMode.hashCode();
        if (hashCode != -2105529586) {
            if (hashCode != -1628741630) {
                if (hashCode == 1931104358 && timeMode.equals(DAY_MODE)) {
                    obj = null;
                }
            } else if (timeMode.equals(MONTH_MODE)) {
                obj = 2;
            }
        } else if (timeMode.equals(WEEK_MODE)) {
            obj = 1;
        }
        String policyName;
        Bundle bundleDay;
        switch (obj) {
            case null:
                if (isOutgoing) {
                    policyName = OUTGOING_DAY_LIMIT;
                } else {
                    policyName = INCOMING_DAY_LIMIT;
                }
                bundleDay = this.mDpm.getPolicy(null, policyName);
                if (bundleDay != null) {
                    return Integer.parseInt(bundleDay.getString(LIMIT_OF_DAY, INIT_VALUE));
                }
                return 0;
            case 1:
                if (isOutgoing) {
                    policyName = OUTGOING_WEEK_LIMIT;
                } else {
                    policyName = INCOMING_WEEK_LIMIT;
                }
                bundleDay = this.mDpm.getPolicy(null, policyName);
                if (bundleDay != null) {
                    return Integer.parseInt(bundleDay.getString(LIMIT_OF_WEEK, INIT_VALUE));
                }
                return 0;
            case 2:
                if (isOutgoing) {
                    policyName = OUTGOING_MONTH_LIMIT;
                } else {
                    policyName = INCOMING_MONTH_LIMIT;
                }
                bundleDay = this.mDpm.getPolicy(null, policyName);
                if (bundleDay != null) {
                    return Integer.parseInt(bundleDay.getString(LIMIT_OF_MONTH, INIT_VALUE));
                }
                return 0;
            default:
                return 0;
        }
    }

    private boolean isDayMode(boolean isOutgoing) {
        Bundle bundle;
        if (isOutgoing) {
            bundle = this.mDpm.getPolicy(null, OUTGOING_DAY_LIMIT);
        } else {
            bundle = this.mDpm.getPolicy(null, INCOMING_DAY_LIMIT);
        }
        if (bundle != null) {
            return Boolean.parseBoolean(bundle.getString(DAY_MODE, MODE_INIT_VALUE));
        }
        return false;
    }

    private boolean isWeekMode(boolean isOutgoing) {
        Bundle bundle;
        if (isOutgoing) {
            bundle = this.mDpm.getPolicy(null, OUTGOING_WEEK_LIMIT);
        } else {
            bundle = this.mDpm.getPolicy(null, INCOMING_WEEK_LIMIT);
        }
        if (bundle != null) {
            return Boolean.parseBoolean(bundle.getString(WEEK_MODE, MODE_INIT_VALUE));
        }
        return false;
    }

    private boolean isMonthMode(boolean isOutgoing) {
        Bundle bundle;
        if (isOutgoing) {
            bundle = this.mDpm.getPolicy(null, OUTGOING_MONTH_LIMIT);
        } else {
            bundle = this.mDpm.getPolicy(null, INCOMING_MONTH_LIMIT);
        }
        if (bundle != null) {
            return Boolean.parseBoolean(bundle.getString(MONTH_MODE, MODE_INIT_VALUE));
        }
        return false;
    }

    private boolean isSameDay(long nowTime, long sharedTime) {
        if (TextUtils.isEmpty(String.valueOf(nowTime)) || TextUtils.isEmpty(String.valueOf(sharedTime))) {
            Rlog.e(LOG_TAG, "get time failed");
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowTime);
        int nowDay = calendar.get(6);
        int nowYear = calendar.get(1);
        calendar.setTimeInMillis(sharedTime);
        int sharedDay = calendar.get(6);
        if (nowYear == calendar.get(1) && sharedDay == nowDay) {
            return true;
        }
        return false;
    }

    private boolean isSameWeek(long nowTime, long sharedTime) {
        if (TextUtils.isEmpty(String.valueOf(nowTime)) || TextUtils.isEmpty(String.valueOf(sharedTime))) {
            Rlog.e(LOG_TAG, "get time failed");
            return false;
        }
        boolean isSameWeek = false;
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(2);
        calendar.setTimeInMillis(nowTime);
        int nowWeek = calendar.get(3);
        int nowYear = calendar.get(1);
        calendar.setTimeInMillis(sharedTime);
        int sharedWeek = calendar.get(3);
        int subYear = nowYear - calendar.get(1);
        if (subYear == 0 && nowWeek == sharedWeek) {
            isSameWeek = true;
        } else if (subYear == 1 && calendar.get(2) == 11 && sharedWeek == nowWeek) {
            isSameWeek = true;
        }
        return isSameWeek;
    }

    private boolean isSameMonth(long nowTime, long sharedTime) {
        if (TextUtils.isEmpty(String.valueOf(nowTime)) || TextUtils.isEmpty(String.valueOf(sharedTime))) {
            Rlog.e(LOG_TAG, "get time failed");
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowTime);
        int nowMonth = calendar.get(2);
        int nowYear = calendar.get(1);
        calendar.setTimeInMillis(sharedTime);
        int sharedMonth = calendar.get(2);
        if (nowYear == calendar.get(1) && nowMonth == sharedMonth) {
            return true;
        }
        return false;
    }

    public SmsUsageMonitor createHwSmsUsageMonitor(Context context, Phone phone) {
        return new HwSmsUsageMonitor(context, phone);
    }
}
