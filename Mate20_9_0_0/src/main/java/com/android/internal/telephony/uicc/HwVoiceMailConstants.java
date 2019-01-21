package com.android.internal.telephony.uicc;

import android.os.Environment;
import android.telephony.Rlog;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class HwVoiceMailConstants {
    static final String LOG_TAG = "VoiceMailConstants";
    static final String PARTNER_VOICEMAIL_PATH = "etc/voicemail-conf.xml";

    public static FileReader getVoiceMailFileReader() {
        File confFile = new File("/system/etc", "voicemail-conf.xml");
        File vmFileCust = new File("/data/cust/", "xml/voicemail-conf.xml");
        File vmFile = new File(Environment.getRootDirectory(), PARTNER_VOICEMAIL_PATH);
        String[] fileInfo = null;
        try {
            fileInfo = HwCfgFilePolicy.getDownloadCfgFile("/global", "global/voicemail-conf.xml");
            File cota_cfg = fileInfo == null ? null : new File(fileInfo[0]);
            FileReader vmReader;
            if (cota_cfg == null || !cota_cfg.exists()) {
                File cfg = HwCfgFilePolicy.getCfgFile("voicemail-conf.xml", 0);
                if (cfg != null) {
                    confFile = cfg;
                    Rlog.d(LOG_TAG, "load voicemail-conf.xml from HwCfgFilePolicy folder");
                } else if (vmFileCust.exists()) {
                    confFile = vmFileCust;
                    Rlog.d(LOG_TAG, "load voicemail-conf.xml from cust folder");
                } else {
                    confFile = vmFile;
                    Rlog.d(LOG_TAG, "load voicemail-conf.xml from etc folder");
                }
                vmReader = null;
                try {
                    return new FileReader(confFile);
                } catch (FileNotFoundException e) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't open ");
                    stringBuilder.append(Environment.getRootDirectory());
                    stringBuilder.append("/");
                    stringBuilder.append(PARTNER_VOICEMAIL_PATH);
                    Rlog.w(str, stringBuilder.toString());
                    return null;
                }
            }
            confFile = cota_cfg;
            Rlog.d(LOG_TAG, "load voicemail-conf.xml from HwCfgFilePolicy DownloadFile folder");
            vmReader = null;
            return new FileReader(confFile);
        } catch (NoClassDefFoundError e2) {
            Rlog.w(LOG_TAG, "NoClassDefFoundError : HwCfgFilePolicy ");
        }
    }
}
