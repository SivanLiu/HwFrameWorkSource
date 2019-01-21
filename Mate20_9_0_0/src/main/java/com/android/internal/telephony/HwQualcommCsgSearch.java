package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException.Error;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class HwQualcommCsgSearch extends CsgSearch {
    private static final String LOG_TAG = "HwQualcommCsgSearch";

    private class CSGNetworkList {
        private static final byte CSG_INFO_TAG = (byte) 20;
        public static final int CSG_LIST_CAT_ALLOWED = 1;
        public static final int CSG_LIST_CAT_OPERATOR = 2;
        public static final int CSG_LIST_CAT_UNKNOWN = 0;
        private static final byte CSG_SCAN_RESULT_TAG = (byte) 19;
        private static final byte CSG_SIG_INFO_TAG = (byte) 21;
        public static final byte GSM_ONLY = (byte) 1;
        public static final byte LTE_ONLY = (byte) 4;
        public static final byte MNC_DIGIT_IS_THREE = (byte) 1;
        public static final byte MNC_DIGIT_IS_TWO = (byte) 0;
        public static final int NAS_SCAN_AS_ABORT = 1;
        public static final int NAS_SCAN_REJ_IN_RLF = 2;
        public static final int NAS_SCAN_SUCCESS = 0;
        public static final int RADIO_IF_GSM = 4;
        public static final int RADIO_IF_LTE = 8;
        public static final int RADIO_IF_TDSCDMA = 9;
        public static final int RADIO_IF_UMTS = 5;
        public static final int SCAN_RESULT_LEN_FAIL = 0;
        public static final int SCAN_RESULT_LEN_SUCC = 4;
        public static final byte TDSCDMA_ONLY = (byte) 8;
        public static final byte UMTS_ONLY = (byte) 2;
        public ArrayList<HwQualcommCsgNetworkInfo> mCSGNetworks;
        private HwQualcommCsgNetworkInfo mCurSelectingCsgNetwork;

        private CSGNetworkList() {
            this.mCSGNetworks = new ArrayList();
            this.mCurSelectingCsgNetwork = null;
        }

        public HwQualcommCsgNetworkInfo getCurrentSelectingCsgNetwork() {
            return this.mCurSelectingCsgNetwork;
        }

        public boolean parseCsgResponseData(byte[] data) {
            Exception e;
            boolean isParseSucc;
            String str;
            StringBuilder stringBuilder;
            boolean isParseSucc2 = false;
            if (data == null) {
                Rlog.e(HwQualcommCsgSearch.LOG_TAG, "=csg= response data is null");
                return false;
            }
            try {
                ByteBuffer resultBuffer = ByteBuffer.wrap(data);
                resultBuffer.order(ByteOrder.nativeOrder());
                byte byteVar = resultBuffer.get();
                if (CSG_SCAN_RESULT_TAG != byteVar) {
                    try {
                        String str2 = HwQualcommCsgSearch.LOG_TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("=csg= scanResult  tag is an unexpected value: ");
                        stringBuilder2.append(byteVar);
                        Rlog.e(str2, stringBuilder2.toString());
                    } catch (Exception e2) {
                        e = e2;
                        isParseSucc = false;
                    }
                } else {
                    short scanResultLen = resultBuffer.getShort();
                    if (scanResultLen == (short) 0) {
                        Rlog.e(HwQualcommCsgSearch.LOG_TAG, "=csg= scanResultLen is 0x00, scan failed");
                    } else {
                        if ((short) 4 == scanResultLen) {
                            int intVar = resultBuffer.getInt();
                            String str3;
                            StringBuilder stringBuilder3;
                            if (intVar != 0) {
                                str3 = HwQualcommCsgSearch.LOG_TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("=csg= scanResult is not success with the value: ");
                                stringBuilder3.append(intVar);
                                stringBuilder3.append(", break");
                                Rlog.e(str3, stringBuilder3.toString());
                            } else {
                                Rlog.d(HwQualcommCsgSearch.LOG_TAG, "=csg= scanResult is success, go on with the parsing");
                                byteVar = resultBuffer.get();
                                if (CSG_INFO_TAG != byteVar) {
                                    str3 = HwQualcommCsgSearch.LOG_TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("=csg= CSG_INFO_TAG is not corrcet with the value: ");
                                    stringBuilder3.append(byteVar);
                                    stringBuilder3.append(", break");
                                    Rlog.e(str3, stringBuilder3.toString());
                                } else if (Short.valueOf(resultBuffer.getShort()).shortValue() == (short) 0) {
                                    Rlog.e(HwQualcommCsgSearch.LOG_TAG, "csg_info_total_len is 0x00, break");
                                } else {
                                    byte numOfCsgInfoEntries = resultBuffer.get();
                                    String str4 = HwQualcommCsgSearch.LOG_TAG;
                                    StringBuilder stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("=csg= numOfEntries for CSG info = ");
                                    stringBuilder4.append(numOfCsgInfoEntries);
                                    Rlog.d(str4, stringBuilder4.toString());
                                    if (numOfCsgInfoEntries > (byte) 0) {
                                        byte i;
                                        byte i2 = (byte) 0;
                                        HwQualcommCsgNetworkInfo csgNetworkInfo = null;
                                        for (i = (byte) 0; i < numOfCsgInfoEntries; i++) {
                                            csgNetworkInfo = new HwQualcommCsgNetworkInfo();
                                            csgNetworkInfo.mcc = resultBuffer.getShort();
                                            csgNetworkInfo.mnc = resultBuffer.getShort();
                                            csgNetworkInfo.bIncludePcsDigit = resultBuffer.get();
                                            csgNetworkInfo.iCSGListCat = resultBuffer.getInt();
                                            csgNetworkInfo.iCSGId = resultBuffer.getInt();
                                            byte[] nameBuffer = new byte[(resultBuffer.get() * 2)];
                                            resultBuffer.get(nameBuffer);
                                            csgNetworkInfo.sCSGName = new String(nameBuffer, "UTF-16");
                                            this.mCSGNetworks.add(csgNetworkInfo);
                                        }
                                        byteVar = resultBuffer.get();
                                        if (CSG_SIG_INFO_TAG != byteVar) {
                                            str4 = HwQualcommCsgSearch.LOG_TAG;
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("=csg= CSG_SIG_INFO_TAG is not corrcet with the value: ");
                                            stringBuilder4.append(byteVar);
                                            stringBuilder4.append(", break");
                                            Rlog.e(str4, stringBuilder4.toString());
                                        } else if (Short.valueOf(resultBuffer.getShort()).shortValue() == (short) 0) {
                                            Rlog.e(HwQualcommCsgSearch.LOG_TAG, "=csg= csg_sig_info_total_len is 0x00, break");
                                        } else {
                                            i = resultBuffer.get();
                                            String str5 = HwQualcommCsgSearch.LOG_TAG;
                                            StringBuilder stringBuilder5 = new StringBuilder();
                                            stringBuilder5.append("=csg= numOfCsgSigInfoEntries for CSG sig info = ");
                                            stringBuilder5.append(i);
                                            Rlog.d(str5, stringBuilder5.toString());
                                            ByteBuffer resultBuffer2;
                                            if (i > (byte) 0) {
                                                while (i2 < i) {
                                                    short mcc = resultBuffer.getShort();
                                                    short mnc = resultBuffer.getShort();
                                                    byte bIncludePcsDigit = resultBuffer.get();
                                                    int iCSGId = resultBuffer.getInt();
                                                    int iCSGSignalStrength = resultBuffer.getInt();
                                                    resultBuffer2 = resultBuffer;
                                                    resultBuffer = this.mCSGNetworks.size();
                                                    isParseSucc = isParseSucc2;
                                                    int j = 0;
                                                    while (j < resultBuffer) {
                                                        int s = resultBuffer;
                                                        try {
                                                            if (mcc == ((HwQualcommCsgNetworkInfo) this.mCSGNetworks.get(j)).mcc && mnc == ((HwQualcommCsgNetworkInfo) this.mCSGNetworks.get(j)).mnc && bIncludePcsDigit == ((HwQualcommCsgNetworkInfo) this.mCSGNetworks.get(j)).bIncludePcsDigit && iCSGId == ((HwQualcommCsgNetworkInfo) this.mCSGNetworks.get(j)).iCSGId) {
                                                                ((HwQualcommCsgNetworkInfo) this.mCSGNetworks.get(j)).iSignalStrength = iCSGSignalStrength;
                                                                break;
                                                            }
                                                            j++;
                                                            resultBuffer = s;
                                                        } catch (Exception e3) {
                                                            e = e3;
                                                            str = HwQualcommCsgSearch.LOG_TAG;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("=csg= exception occurrs: ");
                                                            stringBuilder.append(e);
                                                            Rlog.e(str, stringBuilder.toString());
                                                            return isParseSucc;
                                                        }
                                                    }
                                                    i2++;
                                                    resultBuffer = resultBuffer2;
                                                    isParseSucc2 = isParseSucc;
                                                }
                                                isParseSucc = isParseSucc2;
                                                Rlog.i(HwQualcommCsgSearch.LOG_TAG, "=csg= parse csg response data successfull");
                                                isParseSucc2 = true;
                                            } else {
                                                resultBuffer2 = resultBuffer;
                                                isParseSucc = false;
                                                Rlog.e(HwQualcommCsgSearch.LOG_TAG, "=csg= num Of Csg Sig Info Entries is not corrcet break");
                                            }
                                        }
                                    } else {
                                        isParseSucc = false;
                                        Rlog.e(HwQualcommCsgSearch.LOG_TAG, "=csg= numOfCsgInfoEntries is not correct break");
                                    }
                                }
                            }
                        } else {
                            isParseSucc = false;
                            Rlog.e(HwQualcommCsgSearch.LOG_TAG, "=csg= scanResultLen is invalid, scan failed");
                        }
                        return isParseSucc;
                    }
                }
                isParseSucc = isParseSucc2;
            } catch (Exception e4) {
                e = e4;
                isParseSucc = false;
                str = HwQualcommCsgSearch.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("=csg= exception occurrs: ");
                stringBuilder.append(e);
                Rlog.e(str, stringBuilder.toString());
                return isParseSucc;
            }
            return isParseSucc;
        }

        public HwQualcommCsgNetworkInfo getToBeRegsiteredCSGNetwork() {
            this.mCurSelectingCsgNetwork = null;
            if (this.mCSGNetworks == null) {
                Rlog.e(HwQualcommCsgSearch.LOG_TAG, "=csg= input param is null, not should be here!");
                return this.mCurSelectingCsgNetwork;
            }
            String str;
            StringBuilder stringBuilder;
            try {
                boolean uiccIsCsgAware = HwQualcommCsgSearch.this.isCsgAwareUicc();
                str = HwQualcommCsgSearch.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("=csg= only search ");
                stringBuilder.append(uiccIsCsgAware ? "EF-Operator" : "UE Allowed or unknown");
                stringBuilder.append(" CSG lists");
                Rlog.d(str, stringBuilder.toString());
                int list_size = this.mCSGNetworks.size();
                for (int i = 0; i < list_size; i++) {
                    HwQualcommCsgNetworkInfo csgInfo = (HwQualcommCsgNetworkInfo) this.mCSGNetworks.get(i);
                    if (csgInfo.isSelectedFail) {
                        Rlog.d(HwQualcommCsgSearch.LOG_TAG, "=csg=  had selected and failed, so not reselect again!");
                    } else if (uiccIsCsgAware) {
                        if (2 == csgInfo.iCSGListCat && (this.mCurSelectingCsgNetwork == null || csgInfo.iSignalStrength < this.mCurSelectingCsgNetwork.iSignalStrength)) {
                            this.mCurSelectingCsgNetwork = csgInfo;
                        }
                    } else if ((1 == csgInfo.iCSGListCat || csgInfo.iCSGListCat == 0) && (this.mCurSelectingCsgNetwork == null || csgInfo.iSignalStrength < this.mCurSelectingCsgNetwork.iSignalStrength)) {
                        this.mCurSelectingCsgNetwork = csgInfo;
                    }
                }
                str = HwQualcommCsgSearch.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("=csg=  get the strongest CSG network: ");
                stringBuilder.append(this.mCurSelectingCsgNetwork);
                Rlog.i(str, stringBuilder.toString());
            } catch (Exception e) {
                str = HwQualcommCsgSearch.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("=csg=  exception occurrs: ");
                stringBuilder.append(e);
                Rlog.e(str, stringBuilder.toString());
            }
            return this.mCurSelectingCsgNetwork;
        }

        public boolean isToBeSearchedCsgListsEmpty() {
            boolean isEmpty = true;
            boolean uiccIsCsgAware = HwQualcommCsgSearch.this.isCsgAwareUicc();
            String str = HwQualcommCsgSearch.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("=csg= only search ");
            stringBuilder.append(uiccIsCsgAware ? "EF-Operator" : "UE Allowed or unknown");
            stringBuilder.append(" CSG lists");
            Rlog.d(str, stringBuilder.toString());
            if (this.mCSGNetworks == null) {
                Rlog.e(HwQualcommCsgSearch.LOG_TAG, "=csg= input param is null, not should be here!");
                return true;
            }
            int list_size = this.mCSGNetworks.size();
            for (int i = 0; i < list_size; i++) {
                HwQualcommCsgNetworkInfo csgInfo = (HwQualcommCsgNetworkInfo) this.mCSGNetworks.get(i);
                String str2;
                StringBuilder stringBuilder2;
                if (uiccIsCsgAware) {
                    if (2 == csgInfo.iCSGListCat) {
                        str2 = HwQualcommCsgSearch.LOG_TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("=csg=  have one valid CSG item ");
                        stringBuilder2.append(csgInfo);
                        Rlog.d(str2, stringBuilder2.toString());
                        isEmpty = false;
                        break;
                    }
                } else if (1 == csgInfo.iCSGListCat || csgInfo.iCSGListCat == 0) {
                    str2 = HwQualcommCsgSearch.LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("=csg=  have one valid CSG item ");
                    stringBuilder2.append(csgInfo);
                    Rlog.d(str2, stringBuilder2.toString());
                    isEmpty = false;
                    break;
                }
            }
            return isEmpty;
        }
    }

    public HwQualcommCsgSearch(GsmCdmaPhone phone) {
        super(phone);
    }

    public void handleMessage(Message msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("msg id is ");
        stringBuilder.append(msg.what);
        Rlog.d(str, stringBuilder.toString());
        int i = msg.what;
        AsyncResult ar;
        String str2;
        if (i == 1) {
            Rlog.d(LOG_TAG, "=csg=  Receved EVENT_SELECT_CSG_NETWORK_DONE.");
            ar = (AsyncResult) msg.obj;
            if (ar == null) {
                Rlog.e(LOG_TAG, "=csg=  ar is null, the code should never come here!!");
                return;
            }
            Message onComplete = ar.userObj;
            if (onComplete != null) {
                CSGNetworkList csgNetworklist = onComplete.obj.result;
                if (ar.exception != null) {
                    String str3 = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("=csg= select CSG failed! ");
                    stringBuilder2.append(ar.exception);
                    Rlog.e(str3, stringBuilder2.toString());
                    HwQualcommCsgNetworkInfo curSelectingCsgNetwork = csgNetworklist.getCurrentSelectingCsgNetwork();
                    if (curSelectingCsgNetwork == null) {
                        Rlog.i(LOG_TAG, "=csg= current select CSG is null->maybe loop end. response result.");
                        AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                        onComplete.sendToTarget();
                        return;
                    }
                    curSelectingCsgNetwork.isSelectedFail = true;
                    str2 = LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("=csg= mark  current CSG-ID item Failed!");
                    stringBuilder2.append(csgNetworklist.mCurSelectingCsgNetwork);
                    Rlog.e(str2, stringBuilder2.toString());
                    Rlog.i(LOG_TAG, "=csg= select next strongest CSG-ID->start select");
                    selectCSGNetwork(onComplete);
                    return;
                }
                AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                onComplete.sendToTarget();
                return;
            }
            Rlog.e(LOG_TAG, "=csg=  ar.userObj is null, the code should never come here!!");
        } else if (i != 5) {
            super.handleMessage(msg);
        } else {
            Rlog.i(LOG_TAG, "=csg= Receved EVENT_CSG_PERIODIC_SCAN_DONE.");
            ar = msg.obj;
            if (ar == null) {
                Rlog.e(LOG_TAG, "=csg= ar is null, the code should never come here!!");
            } else if (ar.exception != null || ar.result == null) {
                str2 = LOG_TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("=csg= Periodic Search: get avaiable CSG list failed! ");
                stringBuilder3.append(ar.exception);
                Rlog.e(str2, stringBuilder3.toString());
            } else {
                CSGNetworkList csgNetworklist2 = ar.result;
                Rlog.d(LOG_TAG, "=csg= Periodic Search: get avaiable CSG list success -> select Csg! ");
                if (csgNetworklist2.isToBeSearchedCsgListsEmpty()) {
                    Rlog.i(LOG_TAG, "=csg= Periodic Search: no avaiable CSG-ID -> cancel periodic search! ");
                    cancelCsgPeriodicSearchTimer();
                    return;
                }
                selectCSGNetwork(obtainMessage(6, ar));
            }
        }
    }

    void getAvailableCSGNetworks(Message response) {
        byte[] requestData = new byte[7];
        try {
            ByteBuffer buf = ByteBuffer.wrap(requestData);
            buf.order(ByteOrder.nativeOrder());
            buf.put((byte) 16);
            buf.putShort((short) 1);
            buf.put((byte) 6);
            buf.put((byte) 17);
            buf.putShort((short) 0);
        } catch (Exception e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception occurrs: ");
            stringBuilder.append(e);
            Rlog.e(str, stringBuilder.toString());
        }
        this.mPhone.mCi.getAvailableCSGNetworks(requestData, obtainMessage(null, response));
    }

    void selectCSGNetwork(Message response) {
        AsyncResult ar = response.obj;
        if (ar == null || ar.result == null) {
            Rlog.e(LOG_TAG, "=csg= parsed CSG list is null, return exception");
            AsyncResult.forMessage(response, null, new CommandException(Error.GENERIC_FAILURE));
            response.sendToTarget();
            return;
        }
        CSGNetworkList csgNetworklist = ar.result;
        if (csgNetworklist.mCSGNetworks.size() > 0) {
            HwQualcommCsgNetworkInfo curSelCsgNetwork = csgNetworklist.getToBeRegsiteredCSGNetwork();
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("to be registered CSG info is ");
            stringBuilder.append(curSelCsgNetwork);
            Rlog.d(str, stringBuilder.toString());
            if (curSelCsgNetwork != null && !curSelCsgNetwork.isEmpty()) {
                byte[] requestData = new byte[13];
                try {
                    ByteBuffer buf = ByteBuffer.wrap(requestData);
                    buf.order(ByteOrder.nativeOrder());
                    buf.put((byte) 32);
                    buf.putShort((short) 10);
                    buf.putShort(curSelCsgNetwork.mcc);
                    buf.putShort(curSelCsgNetwork.mnc);
                    buf.put(curSelCsgNetwork.bIncludePcsDigit);
                    buf.putInt(curSelCsgNetwork.iCSGId);
                    buf.put((byte) 5);
                    this.mPhone.mCi.setCSGNetworkSelectionModeManual(requestData, obtainMessage(1, response));
                    return;
                } catch (Exception e) {
                    String str2 = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("=csg= exception occurrs: ");
                    stringBuilder2.append(e);
                    Rlog.e(str2, stringBuilder2.toString());
                    AsyncResult.forMessage(response, null, new CommandException(Error.GENERIC_FAILURE));
                    response.sendToTarget();
                    return;
                }
            } else if (curSelCsgNetwork == null || !curSelCsgNetwork.isEmpty()) {
                Rlog.e(LOG_TAG, "=csg= not find suitable CSG-ID, Select CSG fail!");
                AsyncResult.forMessage(response, null, new CommandException(Error.GENERIC_FAILURE));
                response.sendToTarget();
                return;
            } else {
                Rlog.e(LOG_TAG, "=csg= not find suitable CSG-ID, so finish search! ");
                AsyncResult.forMessage(response, null, null);
                response.sendToTarget();
                return;
            }
        }
        Rlog.e(LOG_TAG, "=csg= mCSGNetworks is not initailized, return with exception");
        AsyncResult.forMessage(response, null, new CommandException(Error.GENERIC_FAILURE));
        response.sendToTarget();
    }

    void handleCsgNetworkQueryResult(AsyncResult ar) {
        if (ar == null || ar.userObj == null) {
            Rlog.e(LOG_TAG, "=csg=  ar or userObj is null, the code should never come here!!");
        } else if (ar.exception != null) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("=csg=  exception happen: ");
            stringBuilder.append(ar.exception);
            Rlog.e(str, stringBuilder.toString());
            AsyncResult.forMessage((Message) ar.userObj, null, ar.exception);
            ((Message) ar.userObj).sendToTarget();
        } else {
            CSGNetworkList csgNetworklist = new CSGNetworkList();
            if (csgNetworklist.parseCsgResponseData((byte[]) ar.result)) {
                AsyncResult.forMessage((Message) ar.userObj, csgNetworklist, null);
                ((Message) ar.userObj).sendToTarget();
            } else {
                AsyncResult.forMessage((Message) ar.userObj, null, new CommandException(Error.GENERIC_FAILURE));
                ((Message) ar.userObj).sendToTarget();
            }
        }
    }
}
