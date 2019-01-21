package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import com.android.internal.util.HexDump;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.Locale;

public class HwCustInboundSmsHandlerImpl extends HwCustInboundSmsHandler {
    private static final boolean HWDBG = true;
    private static final boolean IS_IQI_Enable = SystemProperties.getBoolean("ro.config.iqi_att_support", false);
    private static final String TAG = "HwCustInboundSmsHandlerImpl";
    private static Class jarClass = null;
    private static Object jarObj = null;
    private Constructor<?> CONSTRUCTOR_IQClient;

    public void log(String message) {
        Rlog.d(TAG, message);
    }

    public boolean isIQIEnable() {
        return IS_IQI_Enable;
    }

    public boolean dispatchMessageByDestPort(int destPort, SmsMessageBase sms, Context mContext) {
        Intent intent1;
        StringBuilder stringBuilder;
        String[] hexStringPduArray1;
        StringBuilder stringBuilder2;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("destination port before switch: ");
        stringBuilder3.append(destPort);
        log(stringBuilder3.toString());
        if (!(destPort == 49175 || destPort == 49198)) {
            switch (destPort) {
                case 49162:
                case 49163:
                    intent1 = new Intent();
                    intent1.putExtra("tid", destPort);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("test_ dispatchNormalMessage 3 tid=");
                    stringBuilder.append(destPort);
                    log(stringBuilder.toString());
                    hexStringPduArray1 = bytesToHexString(sms.getPdu()).toUpperCase(Locale.getDefault()).split("1D");
                    if (hexStringPduArray1.length < 4) {
                        return false;
                    }
                    intent1.putExtra("message", hexStringPduArray1[0]);
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("test_ dispatchNormalMessage message = ");
                    stringBuilder4.append(hexStringPduArray1[0]);
                    log(stringBuilder4.toString());
                    intent1.putExtra("appId", new String(hexStringToBytes(hexStringPduArray1[1]), Charset.defaultCharset()));
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("test_ dispatchNormalMessage appId = ");
                    stringBuilder4.append(new String(hexStringToBytes(hexStringPduArray1[1]), Charset.defaultCharset()));
                    log(stringBuilder4.toString());
                    intent1.putExtra("cmd", Integer.valueOf(new String(hexStringToBytes(hexStringPduArray1[2]), Charset.defaultCharset())));
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("test_ dispatchNormalMessage cmd = ");
                    stringBuilder4.append(Integer.valueOf(new String(hexStringToBytes(hexStringPduArray1[2]), Charset.defaultCharset())));
                    log(stringBuilder4.toString());
                    intent1.putExtra("payload", new String(subBytes(hexStringToBytes(hexStringPduArray1[3]), 0, hexStringToBytes(hexStringPduArray1[3]).length - 1), Charset.defaultCharset()));
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("test_ dispatchNormalMessage payload = ");
                    stringBuilder4.append(new String(subBytes(hexStringToBytes(hexStringPduArray1[3]), 0, hexStringToBytes(hexStringPduArray1[3]).length - 1), Charset.defaultCharset()));
                    log(stringBuilder4.toString());
                    if (Integer.parseInt(new String(hexStringToBytes(hexStringPduArray1[2]), Charset.defaultCharset())) == 0) {
                        intent1.setAction("android.lgt.action.CMD_EQUALS_ZERO");
                        intent1.putExtra("body", HexDump.toHexString(sms.getPdu()));
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("test_ dispatchNormalMessage body = ");
                        stringBuilder2.append(HexDump.toHexString(sms.getPdu()));
                        log(stringBuilder2.toString());
                        mContext.sendBroadcast(intent1);
                        log("test_ dispatchNormalMessage before startService for cmd=0");
                        return false;
                    } else if (Integer.parseInt(new String(hexStringToBytes(hexStringPduArray1[2]), Charset.defaultCharset())) == 1) {
                        intent1.setAction("android.lgt.action.APM_START_APP");
                        mContext.startService(intent1);
                        log("test_ dispatchNormalMessage after startService for cmd=1");
                        return HWDBG;
                    }
                    break;
                default:
                    switch (destPort) {
                        case 49200:
                        case 49201:
                        case 49202:
                        case 49204:
                            break;
                        case 49203:
                            intent1 = new Intent("android.lgt.action.APM_SMS_RECEIVED");
                            intent1.putExtra("tid", destPort);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("test_ dispatchNormalMessage 5 tid=");
                            stringBuilder.append(destPort);
                            log(stringBuilder.toString());
                            hexStringPduArray1 = bytesToHexString(sms.getPdu()).toUpperCase(Locale.getDefault()).split("1D");
                            if (hexStringPduArray1.length < 4) {
                                return false;
                            }
                            intent1.putExtra("message", hexStringPduArray1[0]);
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("test_ dispatchNormalMessage message = ");
                            stringBuilder2.append(hexStringPduArray1[0]);
                            log(stringBuilder2.toString());
                            intent1.putExtra("appId", new String(hexStringToBytes(hexStringPduArray1[1]), Charset.defaultCharset()));
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("test_ dispatchNormalMessage appId = ");
                            stringBuilder2.append(new String(hexStringToBytes(hexStringPduArray1[1]), Charset.defaultCharset()));
                            log(stringBuilder2.toString());
                            intent1.putExtra("cmd", Integer.valueOf(new String(hexStringToBytes(hexStringPduArray1[2]), Charset.defaultCharset())));
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("test_ dispatchNormalMessage cmd = ");
                            stringBuilder2.append(Integer.valueOf(new String(hexStringToBytes(hexStringPduArray1[2]), Charset.defaultCharset())));
                            log(stringBuilder2.toString());
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("0x");
                            stringBuilder5.append(Integer.toHexString(subBytes(hexStringToBytes(hexStringPduArray1[3]), 0, 1)[0] & 255));
                            stringBuilder5.append(new String(subBytes(hexStringToBytes(hexStringPduArray1[3]), 1, hexStringToBytes(hexStringPduArray1[3]).length - 1), Charset.defaultCharset()));
                            intent1.putExtra("payload", stringBuilder5.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("test_ dispatchNormalMessage payload = 0x");
                            stringBuilder2.append(Integer.toHexString(subBytes(hexStringToBytes(hexStringPduArray1[3]), 0, 1)[0] & 255));
                            stringBuilder2.append(new String(subBytes(hexStringToBytes(hexStringPduArray1[3]), 1, hexStringToBytes(hexStringPduArray1[3]).length - 1), Charset.defaultCharset()));
                            log(stringBuilder2.toString());
                            mContext.sendBroadcast(intent1);
                            log("test_ dispatchNormalMessage after sendBroadcast for 49203");
                            return HWDBG;
                        default:
                            return false;
                    }
            }
        }
        intent1 = new Intent("android.lgt.action.APM_SMS_RECEIVED");
        intent1.putExtra("tid", destPort);
        stringBuilder = new StringBuilder();
        stringBuilder.append("test_ dispatchNormalMessage 4 tid=");
        stringBuilder.append(destPort);
        log(stringBuilder.toString());
        hexStringPduArray1 = bytesToHexString(sms.getPdu()).toUpperCase(Locale.getDefault()).split("1D");
        if (hexStringPduArray1.length < 4) {
            return false;
        }
        intent1.putExtra("message", hexStringPduArray1[0]);
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("test_ dispatchNormalMessage message = ");
        stringBuilder2.append(hexStringPduArray1[0]);
        log(stringBuilder2.toString());
        intent1.putExtra("appId", new String(hexStringToBytes(hexStringPduArray1[1]), Charset.defaultCharset()));
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("test_ dispatchNormalMessage appId = ");
        stringBuilder2.append(new String(hexStringToBytes(hexStringPduArray1[1]), Charset.defaultCharset()));
        log(stringBuilder2.toString());
        intent1.putExtra("cmd", Integer.valueOf(new String(hexStringToBytes(hexStringPduArray1[2]), Charset.defaultCharset())));
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("test_ dispatchNormalMessage cmd = ");
        stringBuilder2.append(Integer.valueOf(new String(hexStringToBytes(hexStringPduArray1[2]), Charset.defaultCharset())));
        log(stringBuilder2.toString());
        intent1.putExtra("payload", new String(subBytes(hexStringToBytes(hexStringPduArray1[3]), 0, hexStringToBytes(hexStringPduArray1[3]).length - 1), Charset.defaultCharset()));
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("test_ dispatchNormalMessage payload = ");
        stringBuilder2.append(new String(subBytes(hexStringToBytes(hexStringPduArray1[3]), 0, hexStringToBytes(hexStringPduArray1[3]).length - 1), Charset.defaultCharset()));
        log(stringBuilder2.toString());
        mContext.sendBroadcast(intent1);
        log("test_ dispatchNormalMessage after sendBroadcast");
        return HWDBG;
    }

    public byte[] subBytes(byte[] src, int begin, int end) {
        byte[] bs = new byte[(end - begin)];
        for (int i = begin; i < end; i++) {
            bs[i - begin] = src[i];
        }
        return bs;
    }

    public String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int v : src) {
            String hv = Integer.toHexString(v & 255);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public byte[] hexStringToBytes(String hexString) {
        int i = 0;
        if (hexString == null || hexString.equals("")) {
            return new byte[0];
        }
        hexString = hexString.toUpperCase(Locale.getDefault());
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        while (i < length) {
            int pos = i * 2;
            d[i] = (byte) ((charToByte(hexChars[pos]) << 4) | charToByte(hexChars[pos + 1]));
            i++;
        }
        return d;
    }

    public byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public void createIQClient(Context mContext) {
        try {
            String className = "com.carrieriq.iqagent.client.IQClientUtil";
            setJarClass(Class.forName("com.carrieriq.iqagent.client.IQClientUtil"));
            this.CONSTRUCTOR_IQClient = jarClass.getConstructor(new Class[]{Context.class});
            setJarObj(this.CONSTRUCTOR_IQClient.newInstance(new Object[]{mContext}));
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get client error");
            stringBuilder.append(e);
            log(stringBuilder.toString());
        }
    }

    public boolean isIQISms(SmsMessage sms) {
        boolean isIQIsms = false;
        try {
            return ((Boolean) jarClass.getMethod("checkSMS", new Class[]{String.class}).invoke(jarObj, new Object[]{sms.getMessageBody()})).booleanValue();
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("check SMS error");
            stringBuilder.append(e);
            log(stringBuilder.toString());
            return isIQIsms;
        }
    }

    public boolean isIQIWapPush(ByteArrayOutputStream output) {
        boolean isIQIwap = false;
        try {
            return ((Boolean) jarClass.getMethod("checkWAPPush", new Class[]{byte[].class}).invoke(jarObj, new Object[]{output.toByteArray()})).booleanValue();
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("check WapPush error");
            stringBuilder.append(e);
            log(stringBuilder.toString());
            return isIQIwap;
        }
    }

    public static void setJarClass(Class iqiClass) {
        jarClass = iqiClass;
    }

    public static void setJarObj(Object iqiObj) {
        jarObj = iqiObj;
    }

    public boolean isNotNotifyWappushEnabled(AsyncResult ar) {
        if (SystemProperties.getBoolean("ro.config.hw_nonotify_wap", false) && isWapPushMessage(ar)) {
            return HWDBG;
        }
        return false;
    }

    private boolean isWapPushMessage(AsyncResult ar) {
        try {
            SmsHeader smsHeader = ar.result.mWrappedSmsMessage.getUserDataHeader();
            if (smsHeader == null || smsHeader.portAddrs == null || 2948 != smsHeader.portAddrs.destPort) {
                return false;
            }
            return HWDBG;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parse massage error:");
            stringBuilder.append(e);
            Rlog.e(str, stringBuilder.toString());
            return false;
        }
    }
}
