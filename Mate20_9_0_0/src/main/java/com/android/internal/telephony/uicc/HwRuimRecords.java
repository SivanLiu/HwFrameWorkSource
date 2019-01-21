package com.android.internal.telephony.uicc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HwCarrierConfigCardManager;
import com.android.internal.telephony.HwHotplugController;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwSubscriptionManager;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConfig;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.vsim.HwVSimConstants;
import java.util.Locale;

public class HwRuimRecords extends RuimRecords {
    private static final int CDMA_GSM_IMSI_ARRAY_LENGTH = 2;
    private static final int DELAY_GET_CDMA_GSM_IMSI_TIME = 2000;
    private static final int EVENT_DELAY_GET_CDMA_GSM_IMSI = 40;
    private static final int EVENT_GET_ICCID_DONE = 5;
    private static final int EVENT_GET_PBR_DONE = 33;
    private static final int EVENT_SET_MDN_DONE = 39;
    private static final int GSM_CDMA_IMSI_SPLIT_ARRAY_LEN = 2;
    private static final boolean IS_MODEM_CAPABILITY_GET_ICCID_AT = HwModemCapability.isCapabilitySupport(19);
    private static final int MAX_CG_IMSI_RETRIES = 10;
    private static final int MAX_MDN_BYTES = 11;
    private static final int MAX_MDN_NUMBERS = 15;
    protected boolean bNeedSendRefreshBC = false;
    private int mCGImsiRetryNum = 0;
    HwCarrierConfigCardManager mHwCarrierCardManager;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if ("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE".equals(intent.getAction())) {
                    HwRuimRecords hwRuimRecords = HwRuimRecords.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Receives ACTION_SET_RADIO_CAPABILITY_DONE on slot ");
                    stringBuilder.append(HwRuimRecords.this.getSlotId());
                    hwRuimRecords.log(stringBuilder.toString());
                    boolean bNeedFetchRecords = HwFullNetworkConfig.IS_FAST_SWITCH_SIMSLOT && HwRuimRecords.this.mIsSimPowerDown && HwRuimRecords.this.mParentApp != null && AppState.APPSTATE_READY == HwRuimRecords.this.mParentApp.getState();
                    if (bNeedFetchRecords) {
                        HwRuimRecords.this.log("fetchRuimRecords again.");
                        HwRuimRecords.this.mIsSimPowerDown = false;
                        HwRuimRecords.this.mRecordsRequested = false;
                        HwRuimRecords.this.recordsRequired();
                    }
                }
            }
        }
    };
    private boolean mIsSimPowerDown = false;
    protected String mNewMdnNumber = null;

    public HwRuimRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        super(app, c, ci);
        this.mHwCarrierCardManager = HwCarrierConfigCardManager.getDefault(c);
        this.mHwCarrierCardManager.reportIccRecordInstance(getSlotId(), this);
        if (getIccidSwitch()) {
            if (IS_MODEM_CAPABILITY_GET_ICCID_AT) {
                this.mCi.getICCID(obtainMessage(5));
            } else {
                this.mFh.loadEFTransparent(12258, obtainMessage(5));
            }
            this.mRecordsToLoad++;
        }
        addIntentFilter(c);
    }

    private void addIntentFilter(Context c) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        c.registerReceiver(this.mIntentReceiver, filter);
    }

    public boolean beforeHandleRuimRefresh(IccRefreshResponse refreshResponse) {
        switch (refreshResponse.refreshResult) {
            case 0:
                log("beforeHandleRuimRefresh with SIM_REFRESH_FILE_UPDATED");
                this.mRecordsRequested = false;
                break;
            case 1:
                log("beforeHandleRuimRefresh with SIM_REFRESH_INIT");
                this.mRecordsRequested = false;
                break;
            case 2:
                log("beforeHandleRuimRefresh with SIM_REFRESH_RESET");
                this.mAdnCache.reset();
                break;
            default:
                log("beforeHandleRuimRefresh with unknown operation");
                break;
        }
        return false;
    }

    public boolean afterHandleRuimRefresh(IccRefreshResponse refreshResponse) {
        switch (refreshResponse.refreshResult) {
            case 0:
                log("afterHandleRuimRefresh with SIM_REFRESH_FILE_UPDATED");
                synchronized (this) {
                    this.mIccRefreshRegistrants.notifyRegistrants();
                }
                break;
            case 1:
                log("afterHandleRuimRefresh with SIM_REFRESH_INIT");
                if (HW_SIM_REFRESH) {
                    this.bNeedSendRefreshBC = true;
                    break;
                }
                break;
            case 2:
                log("afterHandleRuimRefresh with SIM_REFRESH_RESET");
                if (HW_SIM_REFRESH) {
                    this.bNeedSendRefreshBC = true;
                    break;
                }
                break;
            default:
                log("afterHandleRuimRefresh with unknown operation");
                break;
        }
        return false;
    }

    public void onReady() {
        super.onReady();
        if (this.bNeedSendRefreshBC && HW_SIM_REFRESH) {
            this.bNeedSendRefreshBC = false;
            synchronized (this) {
                this.mIccRefreshRegistrants.notifyRegistrants();
            }
        }
    }

    protected void resetRecords() {
        super.resetRecords();
        this.mIs3Gphonebook = false;
        this.mIsGetPBRDone = false;
        this.mIsSimPowerDown = false;
    }

    protected void onIccIdLoadedHw() {
        if (getIccidSwitch()) {
            sendIccidDoneBroadcast(this.mIccId);
        }
        if (HwHotplugController.IS_HOTSWAP_SUPPORT) {
            HwHotplugController.getInstance().onHotplugIccIdChanged(this.mIccId, getSlotId());
        }
        updateCarrierFile(getSlotId(), 1, this.mIccId);
    }

    protected void updateCarrierFile(int slotId, int fileType, String fileValue) {
        this.mHwCarrierCardManager.updateCarrierFile(slotId, fileType, fileValue);
    }

    /* JADX WARNING: Missing block: B:48:0x018e, code skipped:
            if (r0 != false) goto L_0x0190;
     */
    /* JADX WARNING: Missing block: B:49:0x0190, code skipped:
            onRecordLoaded();
     */
    /* JADX WARNING: Missing block: B:54:0x019e, code skipped:
            if (null == null) goto L_0x01a1;
     */
    /* JADX WARNING: Missing block: B:55:0x01a1, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleMessage(Message msg) {
        boolean isRecordLoadResponse = false;
        if (this.mDestroyed.get()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Received message ");
            stringBuilder.append(msg);
            stringBuilder.append("[");
            stringBuilder.append(msg.what);
            stringBuilder.append("] while being destroyed. Ignoring.");
            loge(stringBuilder.toString());
            return;
        }
        try {
            int i = msg.what;
            AsyncResult ar;
            StringBuilder stringBuilder2;
            if (i == EVENT_GET_PBR_DONE) {
                isRecordLoadResponse = true;
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    this.mIs3Gphonebook = true;
                } else if ((ar.exception instanceof CommandException) && Error.SIM_ABSENT == ((CommandException) ar.exception).getCommandError()) {
                    this.mIsSimPowerDown = true;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Get PBR Done,mIsSimPowerDown: ");
                    stringBuilder2.append(this.mIsSimPowerDown);
                    log(stringBuilder2.toString());
                }
                this.mIsGetPBRDone = true;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Get PBR Done,mIs3Gphonebook: ");
                stringBuilder2.append(this.mIs3Gphonebook);
                log(stringBuilder2.toString());
            } else if (i != 37) {
                switch (i) {
                    case EVENT_SET_MDN_DONE /*39*/:
                        ar = msg.obj;
                        if (ar.exception == null) {
                            this.mMdn = this.mNewMdnNumber;
                        }
                        if (ar.userObj != null) {
                            AsyncResult.forMessage((Message) ar.userObj).exception = ar.exception;
                            ((Message) ar.userObj).sendToTarget();
                        }
                        log("Success to update EF_MDN");
                        break;
                    case 40:
                        log("EVENT_DELAY_GET_CDMA_GSM_IMSI");
                        this.mCi.getCdmaGsmImsi(obtainMessage(37));
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            } else {
                log("get CDMA_GSM_IMSI");
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Exception querying CDMAGSM IMSI, Exception:");
                    stringBuilder3.append(ar.exception);
                    Rlog.e("RuimRecords", stringBuilder3.toString());
                    if (this.mCGImsiRetryNum >= 10) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("get CDMA_GSM_IMSI mImsiLoad is ");
                        stringBuilder2.append(this.mImsiLoad);
                        log(stringBuilder2.toString());
                        if (this.mImsiLoad) {
                            this.mHwCarrierCardManager.setSingleModeCdmaCard(getSlotId(), true);
                            updateCarrierFile(getSlotId(), 4, this.mImsi);
                            log("getCImsiFromCdmaGsmImsi done");
                        } else {
                            getCdmaGsmImsiDone(null);
                        }
                    }
                } else {
                    this.mCdmaGsmImsi = (String) ar.result;
                    if (isValidCdmaGsmImsi() || this.mCGImsiRetryNum >= 10) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("get CDMA_GSM_IMSI mImsiLoad is ");
                        stringBuilder2.append(this.mImsiLoad);
                        log(stringBuilder2.toString());
                        if (isValidCdmaGsmImsi() || !this.mImsiLoad) {
                            getCdmaGsmImsiDone(this.mCdmaGsmImsi);
                        } else {
                            this.mHwCarrierCardManager.setSingleModeCdmaCard(getSlotId(), true);
                            updateCarrierFile(getSlotId(), 4, this.mImsi);
                            log("getCImsiFromCdmaGsmImsi done");
                        }
                        this.mCGImsiRetryNum = 0;
                    } else {
                        log("CDMA_GSM_IMSI not get, retry");
                        this.mCGImsiRetryNum++;
                        delayGetCdmaGsmImsi();
                    }
                }
            }
        } catch (RuntimeException exc) {
            Rlog.w("RuimRecords", "Exception parsing RUIM record", exc);
        } catch (Throwable th) {
            if (null != null) {
                onRecordLoaded();
            }
        }
    }

    public int getSlotId() {
        if (this.mParentApp != null && this.mParentApp.getUiccCard() != null) {
            return this.mParentApp.getUiccCard().getPhoneId();
        }
        log("error , mParentApp.getUiccCard  is null");
        return 0;
    }

    protected void getPbrRecordSize() {
        this.mFh.loadEFLinearFixedAll(20272, obtainMessage(EVENT_GET_PBR_DONE));
        this.mRecordsToLoad++;
    }

    private int decodeImsiDigits(int digits, int length) {
        int denominator;
        int constant = 0;
        int i = 0;
        while (true) {
            denominator = 1;
            if (i >= length) {
                break;
            }
            constant = (constant * 10) + 1;
            i++;
        }
        digits += constant;
        for (i = 0; i < length; i++) {
            digits = (digits / denominator) % 10 == 0 ? digits - (10 * denominator) : digits;
            denominator *= 10;
        }
        return digits;
    }

    public String decodeCdmaImsi(byte[] data) {
        int mcc = decodeImsiDigits(((data[9] & 3) << 8) | (data[8] & HwSubscriptionManager.SUB_INIT_STATE), 3);
        int digits_11_12 = decodeImsiDigits(data[6] & 127, 2);
        int first3digits = ((data[2] & 3) << 8) + (data[1] & HwSubscriptionManager.SUB_INIT_STATE);
        int second3digits = (((data[5] & HwSubscriptionManager.SUB_INIT_STATE) << 8) | (data[4] & HwSubscriptionManager.SUB_INIT_STATE)) >> 6;
        int digit7 = (data[4] >> 2) & 15;
        if (digit7 > 9) {
            digit7 = 0;
        }
        int last3digits = ((data[4] & 3) << 8) | (data[3] & HwSubscriptionManager.SUB_INIT_STATE);
        int first3digits2 = decodeImsiDigits(first3digits, 3);
        int second3digits2 = decodeImsiDigits(second3digits, 3);
        last3digits = decodeImsiDigits(last3digits, 3);
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.US, "%03d", new Object[]{Integer.valueOf(mcc)}));
        builder.append(String.format(Locale.US, "%02d", new Object[]{Integer.valueOf(digits_11_12)}));
        builder.append(String.format(Locale.US, "%03d", new Object[]{Integer.valueOf(first3digits2)}));
        builder.append(String.format(Locale.US, "%03d", new Object[]{Integer.valueOf(second3digits2)}));
        builder.append(String.format(Locale.US, "%d", new Object[]{Integer.valueOf(digit7)}));
        builder.append(String.format(Locale.US, "%03d", new Object[]{Integer.valueOf(last3digits)}));
        return builder.toString();
    }

    public void setMdnNumber(String alphaTag, String number, Message onComplete) {
        byte[] mMdn = new byte[11];
        int i;
        if (number == null || number.length() == 0) {
            log("setMdnNumber, invalid number");
            this.mNewMdnNumber = null;
            for (i = 0; i < 11; i++) {
                mMdn[i] = (byte) 0;
            }
            this.mFh.updateEFLinearFixed(28484, 1, mMdn, null, obtainMessage(EVENT_SET_MDN_DONE, onComplete));
            return;
        }
        i = number.length();
        if (i > 15) {
            i = 15;
        }
        int length = i;
        log("setMdnNumber, validNumber input ");
        StringBuilder validNumber = new StringBuilder();
        for (i = 0; i < length; i++) {
            char c = number.charAt(i);
            if ((c >= '0' && c <= '9') || c == '*' || c == '#') {
                validNumber.append(c);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setMdnNumber, invalide char ");
                stringBuilder.append(c);
                stringBuilder.append(", at index ");
                stringBuilder.append(i);
                log(stringBuilder.toString());
            }
        }
        byte[] convertByte = HwIccUtils.stringToCdmaDTMF(validNumber.toString());
        if (convertByte.length == 0 || convertByte.length > 11) {
            log("setMdnNumber, invalide convertByte");
            return;
        }
        for (i = 0; i < 11; i++) {
            mMdn[i] = (byte) -1;
        }
        mMdn[0] = (byte) validNumber.length();
        System.arraycopy(convertByte, 0, mMdn, 1, convertByte.length < 11 ? convertByte.length : 10);
        if ("+".equals(number.substring(0, 1))) {
            mMdn[9] = (byte) 9;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("+");
            stringBuilder2.append(validNumber.toString());
            this.mNewMdnNumber = stringBuilder2.toString();
        } else {
            mMdn[9] = (byte) 10;
            this.mNewMdnNumber = validNumber.toString();
        }
        mMdn[10] = (byte) 0;
        this.mFh.updateEFLinearFixed(28484, 1, mMdn, null, obtainMessage(EVENT_SET_MDN_DONE, onComplete));
    }

    private void delayGetCdmaGsmImsi() {
        log("delayGetCdmaGsmImsi");
        sendMessageDelayed(obtainMessage(40), HwVSimConstants.GET_MODEM_SUPPORT_VERSION_INTERVAL);
    }

    private boolean isValidCdmaGsmImsi() {
        int i = 0;
        if (this.mCdmaGsmImsi == null) {
            return false;
        }
        boolean isValid = true;
        String[] imsiArray = this.mCdmaGsmImsi.split(",");
        if (2 == imsiArray.length) {
            while (i < imsiArray.length) {
                if (TextUtils.isEmpty(imsiArray[i].trim())) {
                    isValid = false;
                    break;
                }
                i++;
            }
        } else {
            isValid = false;
        }
        return isValid;
    }

    private void getCdmaGsmImsiDone(String cdmaGsmImsi) {
        String cdmaImsi = cdmaGsmImsi;
        if (cdmaGsmImsi != null) {
            String[] imsiArray = cdmaGsmImsi.split(",");
            if (imsiArray.length >= 2) {
                cdmaImsi = imsiArray[0];
            }
        }
        updateCarrierFile(getSlotId(), 4, cdmaImsi);
        log("getCImsiFromCdmaGsmImsi done");
    }

    public void dispose() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Disposing HwRuimRecords ");
        stringBuilder.append(this);
        log(stringBuilder.toString());
        removeMessages(40);
        this.mHwCarrierCardManager.destory(getSlotId(), this);
        this.mContext.unregisterReceiver(this.mIntentReceiver);
        super.dispose();
    }
}
