package com.android.internal.telephony.uicc;

import android.os.Environment;
import android.telephony.Rlog;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class CarrierTestOverride {
    static final String CARRIER_TEST_XML_HEADER = "carrierTestOverrides";
    static final String CARRIER_TEST_XML_ITEM_KEY = "key";
    static final String CARRIER_TEST_XML_ITEM_KEY_STRING_GID1 = "gid1";
    static final String CARRIER_TEST_XML_ITEM_KEY_STRING_GID2 = "gid2";
    static final String CARRIER_TEST_XML_ITEM_KEY_STRING_IMSI = "imsi";
    static final String CARRIER_TEST_XML_ITEM_KEY_STRING_ISINTESTMODE = "isInTestMode";
    static final String CARRIER_TEST_XML_ITEM_KEY_STRING_SPN = "spn";
    static final String CARRIER_TEST_XML_ITEM_VALUE = "value";
    static final String CARRIER_TEST_XML_SUBHEADER = "carrierTestOverride";
    static final String DATA_CARRIER_TEST_OVERRIDE_PATH = "/user_de/0/com.android.phone/files/carrier_test_conf.xml";
    static final String LOG_TAG = "CarrierTestOverride";
    private HashMap<String, String> mCarrierTestParamMap = new HashMap();

    CarrierTestOverride() {
        loadCarrierTestOverrides();
    }

    boolean isInTestMode() {
        if (this.mCarrierTestParamMap.containsKey(CARRIER_TEST_XML_ITEM_KEY_STRING_ISINTESTMODE)) {
            return ((String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_ISINTESTMODE)).equals("true");
        }
        return false;
    }

    String getFakeSpn() {
        try {
            String spn = (String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_SPN);
            Rlog.d(LOG_TAG, "reading spn from CarrierTestConfig file: " + spn);
            return spn;
        } catch (NullPointerException e) {
            Rlog.w(LOG_TAG, "No spn in CarrierTestConfig file ");
            return null;
        }
    }

    String getFakeIMSI() {
        try {
            String imsi = (String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_IMSI);
            Rlog.d(LOG_TAG, "reading imsi from CarrierTestConfig file: " + imsi);
            return imsi;
        } catch (NullPointerException e) {
            Rlog.w(LOG_TAG, "No imsi in CarrierTestConfig file ");
            return null;
        }
    }

    String getFakeGid1() {
        try {
            String gid1 = (String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_GID1);
            Rlog.d(LOG_TAG, "reading gid1 from CarrierTestConfig file: " + gid1);
            return gid1;
        } catch (NullPointerException e) {
            Rlog.w(LOG_TAG, "No gid1 in CarrierTestConfig file ");
            return null;
        }
    }

    String getFakeGid2() {
        try {
            String gid2 = (String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_GID2);
            Rlog.d(LOG_TAG, "reading gid2 from CarrierTestConfig file: " + gid2);
            return gid2;
        } catch (NullPointerException e) {
            Rlog.w(LOG_TAG, "No gid2 in CarrierTestConfig file ");
            return null;
        }
    }

    private void loadCarrierTestOverrides() {
        File carrierTestConfigFile = new File(Environment.getDataDirectory(), DATA_CARRIER_TEST_OVERRIDE_PATH);
        try {
            FileReader carrierTestConfigReader = new FileReader(carrierTestConfigFile);
            Rlog.d(LOG_TAG, "CarrierTestConfig file Modified Timestamp: " + carrierTestConfigFile.lastModified());
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(carrierTestConfigReader);
                XmlUtils.beginDocument(parser, CARRIER_TEST_XML_HEADER);
                while (true) {
                    XmlUtils.nextElement(parser);
                    if (!CARRIER_TEST_XML_SUBHEADER.equals(parser.getName())) {
                        break;
                    }
                    String key = parser.getAttributeValue(null, CARRIER_TEST_XML_ITEM_KEY);
                    String value = parser.getAttributeValue(null, CARRIER_TEST_XML_ITEM_VALUE);
                    Rlog.d(LOG_TAG, "extracting key-values from CarrierTestConfig file: " + key + "|" + value);
                    this.mCarrierTestParamMap.put(key, value);
                }
                carrierTestConfigReader.close();
            } catch (XmlPullParserException e) {
                Rlog.w(LOG_TAG, "Exception in carrier_test_conf parser " + e);
            } catch (IOException e2) {
                Rlog.w(LOG_TAG, "Exception in carrier_test_conf parser " + e2);
            }
        } catch (FileNotFoundException e3) {
            Rlog.w(LOG_TAG, "Can not open " + carrierTestConfigFile.getAbsolutePath());
        }
    }
}
