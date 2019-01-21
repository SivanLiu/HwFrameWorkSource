package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.UiccCarrierPrivilegeRules.TLV;
import com.google.android.mms.pdu.PduHeaders;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UiccPkcs15 extends Handler {
    private static final String CARRIER_RULE_AID = "FFFFFFFFFFFF";
    private static final boolean DBG = true;
    private static final int EVENT_CLOSE_LOGICAL_CHANNEL_DONE = 7;
    private static final int EVENT_LOAD_ACCF_DONE = 6;
    private static final int EVENT_LOAD_ACMF_DONE = 4;
    private static final int EVENT_LOAD_ACRF_DONE = 5;
    private static final int EVENT_LOAD_DODF_DONE = 3;
    private static final int EVENT_LOAD_ODF_DONE = 2;
    private static final int EVENT_SELECT_PKCS15_DONE = 1;
    private static final String ID_ACRF = "4300";
    private static final String LOG_TAG = "UiccPkcs15";
    private static final String TAG_ASN_OCTET_STRING = "04";
    private static final String TAG_ASN_SEQUENCE = "30";
    private static final String TAG_TARGET_AID = "A0";
    private int mChannelId = -1;
    private FileHandler mFh;
    private Message mLoadedCallback;
    private Pkcs15Selector mPkcs15Selector;
    private List<String> mRules = new ArrayList();
    private UiccProfile mUiccProfile;

    private class FileHandler extends Handler {
        protected static final int EVENT_READ_BINARY_DONE = 102;
        protected static final int EVENT_SELECT_FILE_DONE = 101;
        private Message mCallback;
        private String mFileId;
        private final String mPkcs15Path;

        public FileHandler(String pkcs15Path) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Creating FileHandler, pkcs15Path: ");
            stringBuilder.append(pkcs15Path);
            UiccPkcs15.log(stringBuilder.toString());
            this.mPkcs15Path = pkcs15Path;
        }

        public boolean loadFile(String fileId, Message callBack) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("loadFile: ");
            stringBuilder.append(fileId);
            UiccPkcs15.log(stringBuilder.toString());
            if (fileId == null || callBack == null) {
                return false;
            }
            this.mFileId = fileId;
            this.mCallback = callBack;
            selectFile();
            return true;
        }

        private void selectFile() {
            if (UiccPkcs15.this.mChannelId > 0) {
                UiccPkcs15.this.mUiccProfile.iccTransmitApduLogicalChannel(UiccPkcs15.this.mChannelId, 0, PduHeaders.MM_FLAGS, 0, 4, 2, this.mFileId, obtainMessage(101));
            } else {
                UiccPkcs15.log("EF based");
            }
        }

        private void readBinary() {
            if (UiccPkcs15.this.mChannelId > 0) {
                UiccPkcs15.this.mUiccProfile.iccTransmitApduLogicalChannel(UiccPkcs15.this.mChannelId, 0, 176, 0, 0, 0, "", obtainMessage(102));
            } else {
                UiccPkcs15.log("EF based");
            }
        }

        public void handleMessage(Message msg) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage: ");
            stringBuilder.append(msg.what);
            UiccPkcs15.log(stringBuilder.toString());
            AsyncResult ar = msg.obj;
            Throwable th = null;
            StringBuilder stringBuilder2;
            if (ar.exception != null || ar.result == null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error: ");
                stringBuilder2.append(ar.exception);
                UiccPkcs15.log(stringBuilder2.toString());
                AsyncResult.forMessage(this.mCallback, null, ar.exception);
                this.mCallback.sendToTarget();
                return;
            }
            switch (msg.what) {
                case 101:
                    readBinary();
                    break;
                case 102:
                    IccIoResult response = ar.result;
                    String result = IccUtils.bytesToHexString(response.payload).toUpperCase(Locale.US);
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("IccIoResult: ");
                    stringBuilder3.append(response);
                    stringBuilder3.append(" payload: ");
                    stringBuilder3.append(result);
                    UiccPkcs15.log(stringBuilder3.toString());
                    Message message = this.mCallback;
                    if (result == null) {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Error: null response for ");
                        stringBuilder4.append(this.mFileId);
                        th = new IccException(stringBuilder4.toString());
                    }
                    AsyncResult.forMessage(message, result, th);
                    this.mCallback.sendToTarget();
                    break;
                default:
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unknown event");
                    stringBuilder2.append(msg.what);
                    UiccPkcs15.log(stringBuilder2.toString());
                    break;
            }
        }
    }

    private class Pkcs15Selector extends Handler {
        private static final int EVENT_OPEN_LOGICAL_CHANNEL_DONE = 201;
        private static final String PKCS15_AID = "A000000063504B43532D3135";
        private Message mCallback;

        public Pkcs15Selector(Message callBack) {
            this.mCallback = callBack;
            UiccPkcs15.this.mUiccProfile.iccOpenLogicalChannel(PKCS15_AID, 4, obtainMessage(EVENT_OPEN_LOGICAL_CHANNEL_DONE));
        }

        public void handleMessage(Message msg) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage: ");
            stringBuilder.append(msg.what);
            UiccPkcs15.log(stringBuilder.toString());
            if (msg.what != EVENT_OPEN_LOGICAL_CHANNEL_DONE) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown event");
                stringBuilder.append(msg.what);
                UiccPkcs15.log(stringBuilder.toString());
                return;
            }
            AsyncResult ar = msg.obj;
            StringBuilder stringBuilder2;
            if (ar.exception == null && ar.result != null && (ar.result instanceof int[])) {
                UiccPkcs15.this.mChannelId = ((int[]) ar.result)[0];
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mChannelId: ");
                stringBuilder2.append(UiccPkcs15.this.mChannelId);
                UiccPkcs15.log(stringBuilder2.toString());
                AsyncResult.forMessage(this.mCallback, null, null);
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("error: ");
                stringBuilder2.append(ar.exception);
                UiccPkcs15.log(stringBuilder2.toString());
                AsyncResult.forMessage(this.mCallback, null, ar.exception);
            }
            this.mCallback.sendToTarget();
        }
    }

    public UiccPkcs15(UiccProfile uiccProfile, Message loadedCallback) {
        log("Creating UiccPkcs15");
        this.mUiccProfile = uiccProfile;
        this.mLoadedCallback = loadedCallback;
        this.mPkcs15Selector = new Pkcs15Selector(obtainMessage(1));
    }

    public void handleMessage(Message msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleMessage: ");
        stringBuilder.append(msg.what);
        log(stringBuilder.toString());
        AsyncResult ar = msg.obj;
        int i = msg.what;
        if (i != 1) {
            switch (i) {
                case 5:
                    if (ar.exception == null && ar.result != null && (ar.result instanceof String)) {
                        if (!this.mFh.loadFile(parseAcrf((String) ar.result), obtainMessage(6))) {
                            cleanUp();
                            return;
                        }
                        return;
                    }
                    cleanUp();
                    return;
                case 6:
                    if (ar.exception == null && ar.result != null && (ar.result instanceof String)) {
                        parseAccf((String) ar.result);
                    }
                    cleanUp();
                    return;
                case 7:
                    return;
                default:
                    String str = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unknown event ");
                    stringBuilder2.append(msg.what);
                    Rlog.e(str, stringBuilder2.toString());
                    return;
            }
        } else if (ar.exception == null) {
            this.mFh = new FileHandler((String) ar.result);
            if (!this.mFh.loadFile(ID_ACRF, obtainMessage(5))) {
                cleanUp();
            }
        } else {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("select pkcs15 failed: ");
            stringBuilder3.append(ar.exception);
            log(stringBuilder3.toString());
            if (this.mLoadedCallback != null && this.mLoadedCallback.getTarget() != null) {
                this.mLoadedCallback.sendToTarget();
            }
        }
    }

    private void cleanUp() {
        log("cleanUp");
        if (this.mChannelId >= 0) {
            this.mUiccProfile.iccCloseLogicalChannel(this.mChannelId, obtainMessage(7));
            this.mChannelId = -1;
        }
        this.mLoadedCallback.sendToTarget();
    }

    private String parseAcrf(String data) {
        String ret = null;
        String acRules = data;
        while (!acRules.isEmpty()) {
            TLV tlvRule = new TLV(TAG_ASN_SEQUENCE);
            try {
                acRules = tlvRule.parse(acRules, false);
                String ruleString = tlvRule.getValue();
                if (ruleString.startsWith(TAG_TARGET_AID)) {
                    TLV tlvTarget = new TLV(TAG_TARGET_AID);
                    TLV tlvAid = new TLV(TAG_ASN_OCTET_STRING);
                    TLV tlvAsnPath = new TLV(TAG_ASN_SEQUENCE);
                    TLV tlvPath = new TLV(TAG_ASN_OCTET_STRING);
                    ruleString = tlvTarget.parse(ruleString, false);
                    tlvAid.parse(tlvTarget.getValue(), true);
                    if (CARRIER_RULE_AID.equals(tlvAid.getValue())) {
                        tlvAsnPath.parse(ruleString, true);
                        tlvPath.parse(tlvAsnPath.getValue(), true);
                        ret = tlvPath.getValue();
                    }
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: ");
                stringBuilder.append(ex);
                log(stringBuilder.toString());
            }
        }
        return ret;
    }

    private void parseAccf(String data) {
        String acCondition = data;
        while (!acCondition.isEmpty()) {
            TLV tlvCondition = new TLV(TAG_ASN_SEQUENCE);
            TLV tlvCert = new TLV(TAG_ASN_OCTET_STRING);
            try {
                acCondition = tlvCondition.parse(acCondition, false);
                tlvCert.parse(tlvCondition.getValue(), true);
                if (!tlvCert.getValue().isEmpty()) {
                    this.mRules.add(tlvCert.getValue());
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: ");
                stringBuilder.append(ex);
                log(stringBuilder.toString());
                return;
            }
        }
    }

    public List<String> getRules() {
        return this.mRules;
    }

    private static void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mRules != null) {
            pw.println(" mRules:");
            for (String cert : this.mRules) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(cert);
                pw.println(stringBuilder.toString());
            }
        }
    }
}
