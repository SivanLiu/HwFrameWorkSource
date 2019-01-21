package com.android.internal.telephony.vsim.process;

import android.os.AsyncResult;
import android.os.Message;
import com.android.internal.telephony.HwDsdsController;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.telephony.vsim.HwVSimController;
import com.android.internal.telephony.vsim.HwVSimLog;
import com.android.internal.telephony.vsim.HwVSimModemAdapter;
import com.android.internal.telephony.vsim.HwVSimRequest;
import com.android.internal.telephony.vsim.HwVSimUtilsInner;

public class HwVSimInitialProcessor extends HwVSimProcessor {
    public static final int INVALID = -1;
    public static final String LOG_TAG = "VSimInitialProcessor";
    private static final int MAX_TRY_GET_MODEM_SUPPORT_VSIM_VERSION_TIMES = 3;
    private boolean mDSDSAutoSetModemModeDone = false;
    private boolean mGetModemSupportVSimVersionDone = false;
    private boolean mSlotSwitchInitDone;
    private int mTryGetModemSupportVSimVersionTimes;
    protected HwVSimController mVSimController;

    public HwVSimInitialProcessor(HwVSimController controller, HwVSimModemAdapter modemAdapter, HwVSimRequest request) {
        super(modemAdapter, request);
        this.mVSimController = controller;
    }

    public void onEnter() {
        logd("onEnter");
        this.mTryGetModemSupportVSimVersionTimes = 0;
        this.mVSimController.getHandler().sendEmptyMessageDelayed(75, 120000);
        this.mModemAdapter.getModemSupportVSimVersion(this, this.mVSimController.getRadioOnSubId());
        if (HwVSimUtilsInner.isPlatformRealTripple() && HwVSimUtilsInner.IS_DSDSPOWER_SUPPORT) {
            this.mDSDSAutoSetModemModeDone = HwDsdsController.getInstance().uiccHwdsdsNeedSetActiveMode() ^ 1;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DSDS Auto modem mode done is ");
            stringBuilder.append(this.mDSDSAutoSetModemModeDone);
            logd(stringBuilder.toString());
        }
    }

    public void onExit() {
        logd("onExit");
        this.mVSimController.getHandler().removeMessages(75);
        this.mVSimController.getHandler().removeMessages(78);
    }

    public boolean processMessage(Message msg) {
        int i = msg.what;
        if (i != 5) {
            switch (i) {
                case HwVSimConstants.EVENT_GET_MODEM_SUPPORT_VSIM_VER_DONE /*73*/:
                    checkGetModemSupportVSimVersionDone(msg);
                    return true;
                case HwVSimConstants.EVENT_DSDS_AUTO_SETMODEM_DONE /*74*/:
                    checkDSDSAutoSetModemDone(msg);
                    return true;
                case HwVSimConstants.EVENT_INITIAL_TIMEOUT /*75*/:
                    onInitialTimeout();
                    return true;
                case HwVSimConstants.EVENT_INITIAL_SUBSTATE_DONE /*76*/:
                    checkInitialSubStateDone(msg);
                    return true;
                case HwVSimConstants.EVENT_INITIAL_UPDATE_CARDTYPE /*77*/:
                    checkInitialSimCardTypes(msg);
                    return true;
                case HwVSimConstants.EVENT_INITIAL_GET_MODEM_SUPPORT_VSIM_VER /*78*/:
                    this.mModemAdapter.getModemSupportVSimVersion(this, this.mVSimController.getRadioOnSubId());
                    return true;
                default:
                    return false;
            }
        }
        checkSlotSwitchInitDone();
        return true;
    }

    public Message obtainMessage(int what, Object obj) {
        return this.mVSimController.obtainMessage(what, obj);
    }

    public void transitionToState(int state) {
        this.mVSimController.transitionToState(state);
    }

    public void doProcessException(AsyncResult ar, HwVSimRequest request) {
    }

    protected void logd(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    private void checkSlotSwitchInitDone() {
        this.mSlotSwitchInitDone = true;
        checkInitDone();
    }

    private void checkGetModemSupportVSimVersionDone(Message msg) {
        logd("checkGetModemSupportVSimVersionDone");
        this.mModemAdapter.onGetModemSupportVSimVersionDone(this, (AsyncResult) msg.obj);
        if (this.mVSimController.getPlatformSupportVSimVer(0) == -1) {
            int i = this.mTryGetModemSupportVSimVersionTimes;
            this.mTryGetModemSupportVSimVersionTimes = i + 1;
            if (i < 3) {
                logd("retry to getModemSupportVSimVersion");
                this.mVSimController.getHandler().sendEmptyMessageDelayed(78, HwVSimConstants.GET_MODEM_SUPPORT_VERSION_INTERVAL);
                checkInitDone();
            }
        }
        this.mGetModemSupportVSimVersionDone = true;
        checkInitDone();
    }

    private void checkDSDSAutoSetModemDone(Message msg) {
        logd("checkDSDSAutoSetModemDone");
        this.mDSDSAutoSetModemModeDone = true;
        checkInitDone();
    }

    private void checkInitialSubStateDone(Message msg) {
        logd("checkInitSubStateDone");
        checkInitDone();
    }

    private void checkInitialSimCardTypes(Message msg) {
        logd("checkInitialSimCardTypes");
        int[] simCardTypes = this.mVSimController.getSimCardTypes();
        for (int i = 0; i < simCardTypes.length; i++) {
            if (simCardTypes[i] == 0) {
                this.mVSimController.updateSubState(i, 1);
            }
        }
        checkInitDone();
    }

    private void checkInitDone() {
        if (!this.mSlotSwitchInitDone) {
            logd("slot switch not done!");
        } else if (this.mGetModemSupportVSimVersionDone) {
            if (HwVSimUtilsInner.isPlatformRealTripple() && HwVSimUtilsInner.IS_DSDSPOWER_SUPPORT) {
                if (this.mDSDSAutoSetModemModeDone) {
                    boolean z = true;
                    if ((-1 != this.mVSimController.getSubState(0) || this.mVSimController.needBlockUnReservedForVsim(0)) && (-1 != this.mVSimController.getSubState(1) || this.mVSimController.needBlockUnReservedForVsim(1))) {
                        z = false;
                    }
                    if (z) {
                        logd("sub state init not done!");
                        return;
                    }
                }
                logd("dsds auto modem change not done!");
                return;
            }
            logd("checkInitDone");
            transitionToState(0);
        } else {
            logd("get modem support vsim version not done!");
        }
    }

    private void onInitialTimeout() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Initial time out : mSlotSwitchInitDone : ");
        stringBuilder.append(this.mSlotSwitchInitDone);
        stringBuilder.append(", mGetModemSupportVSimVersionDone ");
        stringBuilder.append(this.mGetModemSupportVSimVersionDone);
        stringBuilder.append(", mDSDSAutoSetModemModeDone ");
        stringBuilder.append(this.mDSDSAutoSetModemModeDone);
        logd(stringBuilder.toString());
        transitionToState(0);
    }
}
