package com.android.internal.telephony.uicc;

import android.telephony.Rlog;
import android.util.Xml;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.util.XmlUtils;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class VoiceMailConstants extends AbstractVoiceMailConstants {
    static final String LOG_TAG = "VoiceMailConstants";
    static final int NAME = 0;
    static final int NUMBER = 1;
    static final String PARTNER_VOICEMAIL_PATH = "etc/voicemail-conf.xml";
    static final int SIZE = 3;
    static final int TAG = 2;
    private HashMap<String, String[]> CarrierVmMap = new HashMap();
    private boolean voiceMailLoaded = false;

    VoiceMailConstants() {
    }

    boolean containsCarrier(String carrier) {
        if (loadVoiceMailConfigFromCard("voicemail_carrier", carrier) != null) {
            return true;
        }
        loadVoiceMail();
        return this.CarrierVmMap.containsKey(carrier);
    }

    String getCarrierName(String carrier) {
        String carrierNameFromCard = loadVoiceMailConfigFromCard("voicemail_carrier", carrier);
        if (carrierNameFromCard != null) {
            return carrierNameFromCard;
        }
        loadVoiceMail();
        String[] data = (String[]) this.CarrierVmMap.get(carrier);
        if (data != null) {
            return data[0];
        }
        return null;
    }

    String getVoiceMailNumber(String carrier) {
        String voiceMailNumberFromCard = loadVoiceMailConfigFromCard("voicemail_number", carrier);
        if (voiceMailNumberFromCard != null) {
            return voiceMailNumberFromCard;
        }
        loadVoiceMail();
        String[] data = (String[]) this.CarrierVmMap.get(carrier);
        if (data != null) {
            return data[1];
        }
        return null;
    }

    String getVoiceMailTag(String carrier) {
        String voiceMailTagFromCard = loadVoiceMailConfigFromCard("voicemail_tag", carrier);
        if (voiceMailTagFromCard != null) {
            return voiceMailTagFromCard;
        }
        loadVoiceMail();
        String[] data = (String[]) this.CarrierVmMap.get(carrier);
        if (data != null) {
            return data[2];
        }
        return null;
    }

    private void loadVoiceMail() {
        String str;
        StringBuilder stringBuilder;
        if (!this.voiceMailLoaded) {
            Rlog.w(LOG_TAG, "loadVoiceMail begin!");
            FileReader vmReader = HwTelephonyFactory.getHwUiccManager().getVoiceMailFileReader();
            if (vmReader == null) {
                Rlog.w(LOG_TAG, "loadVoiceMail failed!");
                return;
            }
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(vmReader);
                XmlUtils.beginDocument(parser, "voicemail");
                while (true) {
                    XmlUtils.nextElement(parser);
                    if (!"voicemail".equals(parser.getName())) {
                        break;
                    }
                    data = new String[3];
                    String numeric = parser.getAttributeValue(null, "numeric");
                    data[0] = parser.getAttributeValue(null, "carrier");
                    data[1] = parser.getAttributeValue(null, "vmnumber");
                    data[2] = parser.getAttributeValue(null, "vmtag");
                    this.CarrierVmMap.put(numeric, data);
                }
                if (vmReader != null) {
                    try {
                        vmReader.close();
                    } catch (IOException e) {
                    }
                }
            } catch (XmlPullParserException e2) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception in Voicemail parser ");
                stringBuilder.append(e2);
                Rlog.w(str, stringBuilder.toString());
                if (vmReader != null) {
                    vmReader.close();
                }
            } catch (IOException e3) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception in Voicemail parser ");
                stringBuilder.append(e3);
                Rlog.w(str, stringBuilder.toString());
                if (vmReader != null) {
                    vmReader.close();
                }
            } catch (Throwable th) {
                if (vmReader != null) {
                    try {
                        vmReader.close();
                    } catch (IOException e4) {
                    }
                }
            }
            this.voiceMailLoaded = true;
        }
    }
}
