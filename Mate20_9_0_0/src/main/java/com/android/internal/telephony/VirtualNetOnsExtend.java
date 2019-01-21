package com.android.internal.telephony;

import android.database.Cursor;
import android.net.Uri;
import android.provider.HwTelephony.NumMatchs;
import android.provider.HwTelephony.VirtualNets;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import com.android.internal.telephony.uicc.IccRecords;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class VirtualNetOnsExtend extends VirtualNet {
    private static final String LOG_TAG = "GSM";
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);
    private static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";
    private static final int RULE_APN = 5;
    private static final String XML_ELEMENT_ITEM_NAME = "virtualNet";
    private static final String XML_ELEMENT_TAG_NAME = "virtualNets";
    private static final String XML_NAME = "VirtualNet_cust.xml";
    private static final String XML_PATH = "/data/cust/xml";
    private static List<VirtualNetOnsExtend> display_name_list = null;
    private static VirtualNetOnsExtend mVirtualNetOnsExtend = null;
    private String apn;
    private String gid1;
    private String gid_mask;
    private String hplmn;
    private String imsi_start;
    private String match_file;
    private String match_mask;
    private String match_path;
    private String match_value;
    private String ons_name;
    private String rplmn;
    private String spn;
    private String virtual_net_rule;

    public static VirtualNetOnsExtend getCurrentVirtualNet() {
        return mVirtualNetOnsExtend;
    }

    public VirtualNetOnsExtend(String rplmn, String hplmn, String virtual_net_rule, String imsi_start, String gid1, String gid_mask, String spn, String match_path, String match_file, String match_mask, String apn, String match_value, String ons_name) {
        this.rplmn = rplmn;
        this.hplmn = hplmn;
        this.virtual_net_rule = virtual_net_rule;
        this.imsi_start = imsi_start;
        this.gid1 = gid1;
        this.gid_mask = gid_mask;
        this.spn = spn;
        this.match_path = match_path;
        this.match_file = match_file;
        this.match_mask = match_mask;
        this.apn = apn;
        this.match_value = match_value;
        this.ons_name = ons_name;
    }

    public static boolean isVirtualNetOnsExtend() {
        loadVirtualNetCustFiles();
        return display_name_list != null && display_name_list.size() > 0;
    }

    public static void loadVirtualNetCustFiles() {
        IOException iOException;
        Exception e;
        Exception exception;
        File confFile = initFile();
        if (display_name_list == null && confFile.exists()) {
            display_name_list = new ArrayList();
            InputStream operatorFile = null;
            InputStream operatorFile2 = null;
            try {
                operatorFile = new FileInputStream(confFile);
                XmlPullParser operatorFile3 = Xml.newPullParser();
                operatorFile3.setInput(operatorFile, null);
                for (int xmlEventType = operatorFile3.getEventType(); xmlEventType != 1; xmlEventType = operatorFile3.next()) {
                    if (xmlEventType != 2 || !XML_ELEMENT_ITEM_NAME.equals(operatorFile3.getName())) {
                        if (xmlEventType == 3 && XML_ELEMENT_TAG_NAME.equals(operatorFile3.getName())) {
                            break;
                        }
                    } else {
                        display_name_list.add(new VirtualNetOnsExtend(operatorFile3.getAttributeValue(null, "rplmn"), operatorFile3.getAttributeValue(null, "hplmn"), operatorFile3.getAttributeValue(null, VirtualNets.VIRTUAL_NET_RULE), operatorFile3.getAttributeValue(null, VirtualNets.IMSI_START), operatorFile3.getAttributeValue(null, VirtualNets.GID1), operatorFile3.getAttributeValue(null, VirtualNets.GID_MASK), operatorFile3.getAttributeValue(null, "spn"), operatorFile3.getAttributeValue(null, VirtualNets.MATCH_PATH), operatorFile3.getAttributeValue(null, VirtualNets.MATCH_FILE), operatorFile3.getAttributeValue(null, VirtualNets.MATCH_MASK), operatorFile3.getAttributeValue(null, "apn"), operatorFile3.getAttributeValue(null, VirtualNets.MATCH_VALUE), operatorFile3.getAttributeValue(null, VirtualNets.ONS_NAME)));
                    }
                }
                try {
                    operatorFile.close();
                } catch (IOException e2) {
                    iOException = e2;
                    loge("An error occurs attempting to close this stream!");
                }
                if (operatorFile3 != null) {
                    try {
                        operatorFile3.setInput(null);
                    } catch (Exception e3) {
                        e = e3;
                    }
                }
                return;
            } catch (FileNotFoundException e4) {
                loge("FileNotFoundException : could not find xml file.");
                if (operatorFile != null) {
                    try {
                        operatorFile.close();
                    } catch (IOException e22) {
                        iOException = e22;
                        loge("An error occurs attempting to close this stream!");
                    }
                }
                if (operatorFile2 != null) {
                    try {
                        operatorFile2.setInput(null);
                    } catch (Exception e5) {
                        e = e5;
                    }
                } else {
                    return;
                }
            } catch (XmlPullParserException e6) {
                e6.printStackTrace();
                if (operatorFile != null) {
                    try {
                        operatorFile.close();
                    } catch (IOException e222) {
                        iOException = e222;
                        loge("An error occurs attempting to close this stream!");
                    }
                }
                if (operatorFile2 != null) {
                    try {
                        operatorFile2.setInput(null);
                    } catch (Exception e7) {
                        e = e7;
                    }
                } else {
                    return;
                }
            } catch (IOException e2222) {
                e2222.printStackTrace();
                if (operatorFile != null) {
                    try {
                        operatorFile.close();
                    } catch (IOException e22222) {
                        iOException = e22222;
                        loge("An error occurs attempting to close this stream!");
                    }
                }
                if (operatorFile2 != null) {
                    try {
                        operatorFile2.setInput(null);
                    } catch (Exception e8) {
                        e = e8;
                    }
                } else {
                    return;
                }
            } catch (Throwable th) {
                InputStream parser = operatorFile2;
                operatorFile2 = operatorFile;
                Throwable operatorFile4 = th;
                if (operatorFile2 != null) {
                    try {
                        operatorFile2.close();
                    } catch (IOException e222222) {
                        IOException iOException2 = e222222;
                        loge("An error occurs attempting to close this stream!");
                    }
                }
                if (parser != null) {
                    try {
                        parser.setInput(null);
                    } catch (Exception e9) {
                        exception = e9;
                        e9.printStackTrace();
                    }
                }
            }
        } else {
            return;
        }
        exception = e9;
        e9.printStackTrace();
    }

    private static File initFile() {
        File confFile = new File(XML_PATH, XML_NAME);
        File vnSystemFile = new File("/system/etc", XML_NAME);
        try {
            File cfg = HwCfgFilePolicy.getCfgFile("xml/VirtualNet_cust.xml", 0);
            if (cfg != null) {
                confFile = cfg;
                logd("loadVirtualNetCust from hwCfgPolicyPath folder");
                return confFile;
            } else if (confFile.exists()) {
                logd("loadVirtualNetCust from cust folder");
                return confFile;
            } else {
                confFile = vnSystemFile;
                logd("loadVirtualNetCust from etc folder");
                return confFile;
            }
        } catch (NoClassDefFoundError e) {
            Log.w(LOG_TAG, "NoClassDefFoundError : HwCfgFilePolicy ");
            return confFile;
        }
    }

    public static void createVirtualNetByHplmn(String hplmn, IccRecords simRecords) {
        mVirtualNetOnsExtend = null;
        if (display_name_list != null && display_name_list.size() > 0) {
            int displayNameListSize = display_name_list.size();
            for (int i = 0; i < displayNameListSize; i++) {
                String simMccMnc = ((VirtualNetOnsExtend) display_name_list.get(i)).getHplmn();
                String VirtualNetRule = ((VirtualNetOnsExtend) display_name_list.get(i)).getRule();
                Uri preCarrierUri = PREFERAPN_URI;
                if (!TextUtils.isEmpty(simMccMnc) && simMccMnc.equals(hplmn)) {
                    int tmpVirtualNetRule = 0;
                    if (!TextUtils.isEmpty(VirtualNetRule)) {
                        tmpVirtualNetRule = Integer.parseInt(VirtualNetRule);
                    }
                    switch (tmpVirtualNetRule) {
                        case 1:
                            if (!VirtualNet.isImsiVirtualNet(simRecords.getIMSI(), ((VirtualNetOnsExtend) display_name_list.get(i)).getImsistart())) {
                                break;
                            }
                            mVirtualNetOnsExtend = (VirtualNetOnsExtend) display_name_list.get(i);
                            break;
                        case 2:
                            if (!VirtualNet.isGid1VirtualNet(simRecords.getGID1(), ((VirtualNetOnsExtend) display_name_list.get(i)).getGID1(), ((VirtualNetOnsExtend) display_name_list.get(i)).getGidmask())) {
                                break;
                            }
                            mVirtualNetOnsExtend = (VirtualNetOnsExtend) display_name_list.get(i);
                            break;
                        case 3:
                            if (!VirtualNet.isSpnVirtualNet(simRecords.getServiceProviderName(), ((VirtualNetOnsExtend) display_name_list.get(i)).getSpn())) {
                                break;
                            }
                            mVirtualNetOnsExtend = (VirtualNetOnsExtend) display_name_list.get(i);
                            break;
                        case 4:
                            if (!VirtualNet.isSpecialFileVirtualNet(((VirtualNetOnsExtend) display_name_list.get(i)).getPath(), ((VirtualNetOnsExtend) display_name_list.get(i)).getFile(), ((VirtualNetOnsExtend) display_name_list.get(i)).getFilemask(), ((VirtualNetOnsExtend) display_name_list.get(i)).getFilevalue(), simRecords.getSlotId())) {
                                break;
                            }
                            mVirtualNetOnsExtend = (VirtualNetOnsExtend) display_name_list.get(i);
                            break;
                        case 5:
                            if (!isApnVirtualNet(((VirtualNetOnsExtend) display_name_list.get(i)).getApn(), getPreApnName(preCarrierUri))) {
                                break;
                            }
                            mVirtualNetOnsExtend = (VirtualNetOnsExtend) display_name_list.get(i);
                            break;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("unhandled case: ");
                            stringBuilder.append(tmpVirtualNetRule);
                            logd(stringBuilder.toString());
                            break;
                    }
                }
            }
        }
    }

    public String getRplmn() {
        return this.rplmn;
    }

    public String getApn() {
        return this.apn;
    }

    public String getHplmn() {
        return this.hplmn;
    }

    public String getImsistart() {
        return this.imsi_start;
    }

    public String getRule() {
        return this.virtual_net_rule;
    }

    public String getGID1() {
        return this.gid1;
    }

    public String getGidmask() {
        return this.gid_mask;
    }

    public String getSpn() {
        return this.spn;
    }

    public String getPath() {
        return this.match_path;
    }

    public String getFile() {
        return this.match_file;
    }

    public String getFilemask() {
        return this.match_mask;
    }

    public String getOperatorName() {
        return this.ons_name;
    }

    public String getFilevalue() {
        return this.match_value;
    }

    /* JADX WARNING: Missing block: B:8:0x000f, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isApnVirtualNet(String apn, String preApn) {
        if (apn == null || preApn == null || !apn.equals(preApn)) {
            return false;
        }
        return true;
    }

    private static String getPreApnName(Uri uri) {
        Cursor cursor = PhoneFactory.getDefaultPhone().getContext().getContentResolver().query(uri, new String[]{"_id", NumMatchs.NAME, "apn"}, null, null, NumMatchs.DEFAULT_SORT_ORDER);
        String apn = null;
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            apn = cursor.getString(cursor.getColumnIndexOrThrow("apn"));
        }
        if (cursor != null) {
            cursor.close();
        }
        return apn;
    }

    private static void logd(String text) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[VirtualNet] ");
        stringBuilder.append(text);
        Log.d(str, stringBuilder.toString());
    }

    private static void loge(String text) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[VirtualNet] ");
        stringBuilder.append(text);
        Log.e(str, stringBuilder.toString());
    }
}
