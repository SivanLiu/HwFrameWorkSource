package com.android.internal.telephony;

import android.content.Context;
import android.os.Environment;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import huawei.cust.HwCfgFilePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwSmsUsageMonitor extends SmsUsageMonitor {
    private static final boolean ENABLE_CUSTOM_SHORTCODES = SystemProperties.getBoolean("ro.config.custom_short_codes", false);
    private static String FILE_FROM_CUST_DIR = "/data/cust/xml/xml/smsShortcodesList-conf.xml";
    private static String FILE_FROM_SYSTEM_ETC_DIR = "/system/etc/xml/smsShortcodesList-conf.xml";
    private static final String PARAM_SHORTCODE_PATH = "etc/smsShortcodesList-conf.xml";
    private static final String SHORT_CODE_CONFIG_FILE = "xml/smsShortcodesList-conf.xml";
    private static final String TAG = "HwSmsUsageMonitor";
    private static ArrayList<ShortCodeCfg> mshortCodeList;
    Phone mPhone;

    static class ShortCodeCfg {
        String numeric;
        String shortcodes;

        ShortCodeCfg() {
        }
    }

    public HwSmsUsageMonitor(Context context, Phone phone) {
        super(context);
        loadShortCodeList();
        this.mPhone = phone;
    }

    /* JADX WARNING: Missing block: B:25:0x003e, code skipped:
            return checkDestination(r5, r6);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected int checkDestinationHw(String destAddress, String countryIso, String simMccmnc) {
        synchronized (this.mSettingsObserverHandler) {
            if (PhoneNumberUtils.isEmergencyNumber(destAddress, countryIso)) {
                Rlog.d(TAG, "isEmergencyNumber");
                return 0;
            } else if (!this.mCheckEnabled.get()) {
                Rlog.d(TAG, "check disabled");
                return 0;
            } else if (getShortcodesCust(destAddress)) {
                return 1;
            } else if (ENABLE_CUSTOM_SHORTCODES && isSpecialPattern(destAddress, simMccmnc)) {
                return 1;
            }
        }
    }

    private boolean getShortcodesCust(String destAddress) {
        int slotId = SubscriptionManager.getSlotIndex(this.mPhone.getPhoneId());
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("slotIds:");
        stringBuilder.append(slotId);
        Rlog.d(str, stringBuilder.toString());
        try {
            Boolean shortCodeSwitch = (Boolean) HwCfgFilePolicy.getValue("custom_short_codes_switch", slotId, Boolean.class);
            if (shortCodeSwitch != null && shortCodeSwitch.booleanValue()) {
                String shortCodesList = (String) HwCfgFilePolicy.getValue("sms_short_codes_free", slotId, String.class);
                if (shortCodesList != null) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("shortCodesList:");
                    stringBuilder2.append(shortCodesList);
                    Rlog.d(str2, stringBuilder2.toString());
                    String[] shortcodes = shortCodesList.split(";");
                    int j = 0;
                    int n = shortcodes.length;
                    while (j < n) {
                        if (shortcodes[j] == null || !shortcodes[j].equals(destAddress)) {
                            j++;
                        } else {
                            Rlog.d(TAG, "match the free short code ");
                            return true;
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            Rlog.e(TAG, "Failed to get Shortcodes in carrier", e);
        }
        return false;
    }

    private static BufferedReader getShortCodesFileReader() {
        File confFile = new File(FILE_FROM_SYSTEM_ETC_DIR);
        File sShortCodeCust = new File(FILE_FROM_CUST_DIR);
        File sShortcodeFile = new File(Environment.getRootDirectory(), PARAM_SHORTCODE_PATH);
        try {
            File cfg = HwCfgFilePolicy.getCfgFile(SHORT_CODE_CONFIG_FILE, 0);
            if (cfg != null) {
                confFile = cfg;
                Rlog.d(TAG, "load smsShortcodesList-conf.xml from HwCfgFilePolicy folder");
            } else if (sShortCodeCust.exists()) {
                confFile = sShortCodeCust;
                Rlog.d(TAG, "load smsShortcodesList-conf.xml from cust folder");
            } else {
                confFile = sShortcodeFile;
                Rlog.d(TAG, "load smsShortcodesList-conf.xml from etc folder");
            }
        } catch (NoClassDefFoundError e) {
            Rlog.e(TAG, "NoClassDefFoundError : HwCfgFilePolicy ");
        }
        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(confFile), "UTF-8"));
        } catch (FileNotFoundException e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't open ");
            stringBuilder.append(Environment.getRootDirectory());
            stringBuilder.append("/");
            stringBuilder.append(PARAM_SHORTCODE_PATH);
            Rlog.e(str, stringBuilder.toString());
            return null;
        } catch (UnsupportedEncodingException e3) {
            Rlog.d(TAG, "UnsupportedEncodingException Exception");
            return null;
        }
    }

    private static void loadShortCodeList() {
        String str;
        StringBuilder stringBuilder;
        mshortCodeList = new ArrayList();
        BufferedReader sShortcodeReader = getShortCodesFileReader();
        if (sShortcodeReader == null) {
            Rlog.d(TAG, "loadShortCodeList failed!");
            return;
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(sShortcodeReader);
            XmlUtils.beginDocument(parser, "shortCodesList");
            while (true) {
                XmlUtils.nextElement(parser);
                if (parser.getName() == null) {
                    try {
                        break;
                    } catch (IOException e) {
                        Rlog.e(TAG, "IOException happen.close failed.");
                    }
                } else {
                    ShortCodeCfg shortcodeConfig = new ShortCodeCfg();
                    shortcodeConfig.numeric = parser.getAttributeValue(null, "numeric");
                    shortcodeConfig.shortcodes = parser.getAttributeValue(null, "codes");
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getAttributeValue numeric = ");
                    stringBuilder2.append(shortcodeConfig.numeric);
                    stringBuilder2.append("getAttributeValue codes = ");
                    stringBuilder2.append(shortcodeConfig.shortcodes);
                    Rlog.d(str2, stringBuilder2.toString());
                    mshortCodeList.add(shortcodeConfig);
                }
            }
            sShortcodeReader.close();
        } catch (XmlPullParserException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in smsShortcodesList parser ");
            stringBuilder.append(e2);
            Rlog.d(str, stringBuilder.toString());
            sShortcodeReader.close();
        } catch (IOException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in smsShortcodesList parser ");
            stringBuilder.append(e3);
            Rlog.d(str, stringBuilder.toString());
            sShortcodeReader.close();
        } catch (Throwable th) {
            try {
                sShortcodeReader.close();
            } catch (IOException e4) {
                Rlog.e(TAG, "IOException happen.close failed.");
            }
            throw th;
        }
    }

    private boolean isSpecialPattern(String destAdd, String simMccmnc) {
        if (destAdd == null || simMccmnc == null || mshortCodeList == null) {
            return false;
        }
        int listSize = mshortCodeList.size();
        for (int i = 0; i < listSize; i++) {
            String plmn = ((ShortCodeCfg) mshortCodeList.get(i)).numeric;
            if (plmn != null && plmn.equals(simMccmnc)) {
                String shortcodesStr = ((ShortCodeCfg) mshortCodeList.get(i)).shortcodes;
                if (shortcodesStr != null) {
                    String[] shortcodes = shortcodesStr.split(";");
                    int j = 0;
                    while (j < shortcodes.length) {
                        if (shortcodes[j] == null || !shortcodes[j].equals(destAdd)) {
                            j++;
                        } else {
                            Rlog.d(TAG, "match the free short code ");
                            return true;
                        }
                    }
                    continue;
                } else {
                    continue;
                }
            }
        }
        return false;
    }
}
