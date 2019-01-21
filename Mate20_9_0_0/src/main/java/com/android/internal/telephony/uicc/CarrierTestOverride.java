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
    static final String CARRIER_TEST_XML_ITEM_KEY_STRING_ICCID = "iccid";
    static final String CARRIER_TEST_XML_ITEM_KEY_STRING_IMSI = "imsi";
    static final String CARRIER_TEST_XML_ITEM_KEY_STRING_ISINTESTMODE = "isInTestMode";
    static final String CARRIER_TEST_XML_ITEM_KEY_STRING_MCCMNC = "mccmnc";
    static final String CARRIER_TEST_XML_ITEM_KEY_STRING_PNN = "pnn";
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
        return this.mCarrierTestParamMap.containsKey(CARRIER_TEST_XML_ITEM_KEY_STRING_ISINTESTMODE) && ((String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_ISINTESTMODE)).equals("true");
    }

    String getFakeSpn() {
        try {
            String spn = (String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_SPN);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reading spn from CarrierTestConfig file: ");
            stringBuilder.append(spn);
            Rlog.d(str, stringBuilder.toString());
            return spn;
        } catch (NullPointerException e) {
            Rlog.w(LOG_TAG, "No spn in CarrierTestConfig file ");
            return null;
        }
    }

    String getFakeIMSI() {
        try {
            String imsi = (String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_IMSI);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reading imsi from CarrierTestConfig file: ");
            stringBuilder.append(imsi);
            Rlog.d(str, stringBuilder.toString());
            return imsi;
        } catch (NullPointerException e) {
            Rlog.w(LOG_TAG, "No imsi in CarrierTestConfig file ");
            return null;
        }
    }

    String getFakeGid1() {
        try {
            String gid1 = (String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_GID1);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reading gid1 from CarrierTestConfig file: ");
            stringBuilder.append(gid1);
            Rlog.d(str, stringBuilder.toString());
            return gid1;
        } catch (NullPointerException e) {
            Rlog.w(LOG_TAG, "No gid1 in CarrierTestConfig file ");
            return null;
        }
    }

    String getFakeGid2() {
        try {
            String gid2 = (String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_GID2);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reading gid2 from CarrierTestConfig file: ");
            stringBuilder.append(gid2);
            Rlog.d(str, stringBuilder.toString());
            return gid2;
        } catch (NullPointerException e) {
            Rlog.w(LOG_TAG, "No gid2 in CarrierTestConfig file ");
            return null;
        }
    }

    String getFakePnnHomeName() {
        try {
            String pnn = (String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_PNN);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reading pnn from CarrierTestConfig file: ");
            stringBuilder.append(pnn);
            Rlog.d(str, stringBuilder.toString());
            return pnn;
        } catch (NullPointerException e) {
            Rlog.w(LOG_TAG, "No pnn in CarrierTestConfig file ");
            return null;
        }
    }

    String getFakeIccid() {
        try {
            String iccid = (String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_ICCID);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reading iccid from CarrierTestConfig file: ");
            stringBuilder.append(iccid);
            Rlog.d(str, stringBuilder.toString());
            return iccid;
        } catch (NullPointerException e) {
            Rlog.w(LOG_TAG, "No iccid in CarrierTestConfig file ");
            return null;
        }
    }

    String getFakeMccMnc() {
        try {
            String mccmnc = (String) this.mCarrierTestParamMap.get(CARRIER_TEST_XML_ITEM_KEY_STRING_MCCMNC);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reading mccmnc from CarrierTestConfig file: ");
            stringBuilder.append(mccmnc);
            Rlog.d(str, stringBuilder.toString());
            return mccmnc;
        } catch (NullPointerException e) {
            Rlog.w(LOG_TAG, "No mccmnc in CarrierTestConfig file ");
            return null;
        }
    }

    void override(String mccmnc, String imsi, String iccid, String gid1, String gid2, String pnn, String spn) {
        this.mCarrierTestParamMap.put(CARRIER_TEST_XML_ITEM_KEY_STRING_ISINTESTMODE, "true");
        this.mCarrierTestParamMap.put(CARRIER_TEST_XML_ITEM_KEY_STRING_MCCMNC, mccmnc);
        this.mCarrierTestParamMap.put(CARRIER_TEST_XML_ITEM_KEY_STRING_IMSI, imsi);
        this.mCarrierTestParamMap.put(CARRIER_TEST_XML_ITEM_KEY_STRING_ICCID, iccid);
        this.mCarrierTestParamMap.put(CARRIER_TEST_XML_ITEM_KEY_STRING_GID1, gid1);
        this.mCarrierTestParamMap.put(CARRIER_TEST_XML_ITEM_KEY_STRING_GID2, gid2);
        this.mCarrierTestParamMap.put(CARRIER_TEST_XML_ITEM_KEY_STRING_PNN, pnn);
        this.mCarrierTestParamMap.put(CARRIER_TEST_XML_ITEM_KEY_STRING_SPN, spn);
    }

    private void loadCarrierTestOverrides() {
        String str;
        StringBuilder stringBuilder;
        File carrierTestConfigFile = new File(Environment.getDataDirectory(), DATA_CARRIER_TEST_OVERRIDE_PATH);
        String str2;
        StringBuilder stringBuilder2;
        try {
            FileReader carrierTestConfigReader = new FileReader(carrierTestConfigFile);
            str2 = LOG_TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CarrierTestConfig file Modified Timestamp: ");
            stringBuilder2.append(carrierTestConfigFile.lastModified());
            Rlog.d(str2, stringBuilder2.toString());
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
                    String str3 = LOG_TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("extracting key-values from CarrierTestConfig file: ");
                    stringBuilder3.append(key);
                    stringBuilder3.append("|");
                    stringBuilder3.append(value);
                    Rlog.d(str3, stringBuilder3.toString());
                    this.mCarrierTestParamMap.put(key, value);
                }
                carrierTestConfigReader.close();
            } catch (XmlPullParserException e) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception in carrier_test_conf parser ");
                stringBuilder.append(e);
                Rlog.w(str, stringBuilder.toString());
            } catch (IOException e2) {
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception in carrier_test_conf parser ");
                stringBuilder.append(e2);
                Rlog.w(str, stringBuilder.toString());
            }
        } catch (FileNotFoundException e3) {
            str2 = LOG_TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Can not open ");
            stringBuilder2.append(carrierTestConfigFile.getAbsolutePath());
            Rlog.w(str2, stringBuilder2.toString());
        }
    }
}
