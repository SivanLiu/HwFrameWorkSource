package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LocaleTracker extends Handler {
    private static final long CELL_INFO_MAX_DELAY_MS = 600000;
    private static final long CELL_INFO_MIN_DELAY_MS = 2000;
    private static final long CELL_INFO_PERIODIC_POLLING_DELAY_MS = 600000;
    private static final boolean DBG = true;
    private static final int EVENT_GET_CELL_INFO = 1;
    private static final int EVENT_SERVICE_STATE_CHANGED = 3;
    private static final int EVENT_UPDATE_OPERATOR_NUMERIC = 2;
    private static final int MAX_FAIL_COUNT = 30;
    private static final String TAG = LocaleTracker.class.getSimpleName();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.telephony.action.SIM_CARD_STATE_CHANGED".equals(intent.getAction()) && intent.getIntExtra("phone", 0) == LocaleTracker.this.mPhone.getPhoneId()) {
                LocaleTracker.this.onSimCardStateChanged(intent.getIntExtra("android.telephony.extra.SIM_STATE", 0));
            }
        }
    };
    private List<CellInfo> mCellInfo;
    private String mCurrentCountryIso;
    private int mFailCellInfoCount;
    private int mLastServiceState = -1;
    private final LocalLog mLocalLog = new LocalLog(50);
    private String mOperatorNumeric;
    private final Phone mPhone;
    private int mSimState;

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                synchronized (this) {
                    getCellInfo();
                    updateLocale();
                }
                return;
            case 2:
                updateOperatorNumericSync((String) msg.obj);
                return;
            case 3:
                onServiceStateChanged((ServiceState) msg.obj.result);
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected message arrives. msg = ");
                stringBuilder.append(msg.what);
                throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public LocaleTracker(Phone phone, Looper looper) {
        super(looper);
        this.mPhone = phone;
        this.mSimState = 0;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.telephony.action.SIM_CARD_STATE_CHANGED");
        this.mPhone.getContext().registerReceiver(this.mBroadcastReceiver, filter);
        this.mPhone.registerForServiceStateChanged(this, 3, null);
    }

    public synchronized String getCurrentCountry() {
        return this.mCurrentCountryIso != null ? this.mCurrentCountryIso : "";
    }

    private String getMccFromCellInfo() {
        String selectedMcc = null;
        if (this.mCellInfo != null) {
            Map<String, Integer> countryCodeMap = new HashMap();
            int maxCount = 0;
            for (CellInfo cellInfo : this.mCellInfo) {
                String mcc = null;
                if (cellInfo instanceof CellInfoGsm) {
                    mcc = ((CellInfoGsm) cellInfo).getCellIdentity().getMccString();
                } else if (cellInfo instanceof CellInfoLte) {
                    mcc = ((CellInfoLte) cellInfo).getCellIdentity().getMccString();
                } else if (cellInfo instanceof CellInfoWcdma) {
                    mcc = ((CellInfoWcdma) cellInfo).getCellIdentity().getMccString();
                }
                if (mcc != null) {
                    int count = 1;
                    if (countryCodeMap.containsKey(mcc)) {
                        count = ((Integer) countryCodeMap.get(mcc)).intValue() + 1;
                    }
                    countryCodeMap.put(mcc, Integer.valueOf(count));
                    if (count > maxCount) {
                        maxCount = count;
                        selectedMcc = mcc;
                    }
                }
            }
        }
        return selectedMcc;
    }

    private synchronized void onSimCardStateChanged(int state) {
        if (this.mSimState != state && state == 1) {
            log("Sim absent. Get latest cell info from the modem.");
            getCellInfo();
            updateLocale();
        }
        this.mSimState = state;
    }

    private void onServiceStateChanged(ServiceState serviceState) {
        int state = serviceState.getState();
        if (state != this.mLastServiceState) {
            if (state != 3 && TextUtils.isEmpty(this.mOperatorNumeric)) {
                String msg = new StringBuilder();
                msg.append("Service state ");
                msg.append(ServiceState.rilServiceStateToString(state));
                msg.append(". Get cell info now.");
                msg = msg.toString();
                log(msg);
                this.mLocalLog.log(msg);
                getCellInfo();
            } else if (state == 3) {
                if (this.mCellInfo != null) {
                    this.mCellInfo.clear();
                }
                stopCellInfoRetry();
            }
            updateLocale();
            this.mLastServiceState = state;
        }
    }

    public synchronized void updateOperatorNumericSync(String operatorNumeric) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateOperatorNumericSync. mcc/mnc=");
        stringBuilder.append(operatorNumeric);
        log(stringBuilder.toString());
        if (!Objects.equals(this.mOperatorNumeric, operatorNumeric)) {
            String msg = new StringBuilder();
            msg.append("Operator numeric changes to ");
            msg.append(operatorNumeric);
            msg = msg.toString();
            log(msg);
            this.mLocalLog.log(msg);
            this.mOperatorNumeric = operatorNumeric;
            if (TextUtils.isEmpty(this.mOperatorNumeric)) {
                log("Operator numeric unavailable. Get latest cell info from the modem.");
                getCellInfo();
            } else {
                if (this.mCellInfo != null) {
                    this.mCellInfo.clear();
                }
                stopCellInfoRetry();
            }
            updateLocale();
        }
    }

    public void updateOperatorNumericAsync(String operatorNumeric) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateOperatorNumericAsync. mcc/mnc=");
        stringBuilder.append(operatorNumeric);
        log(stringBuilder.toString());
        sendMessage(obtainMessage(2, operatorNumeric));
    }

    private long getCellInfoDelayTime(int failCount) {
        if (failCount >= 30) {
            failCount = 30;
            this.mFailCellInfoCount = 0;
        }
        long delay = ((long) Math.pow(2.0d, (double) (failCount - 1))) * CELL_INFO_MIN_DELAY_MS;
        if (delay < CELL_INFO_MIN_DELAY_MS) {
            return CELL_INFO_MIN_DELAY_MS;
        }
        if (delay > 600000) {
            return 600000;
        }
        return delay;
    }

    private void stopCellInfoRetry() {
        this.mFailCellInfoCount = 0;
        removeMessages(1);
    }

    private void getCellInfo() {
        String msg;
        if (this.mPhone.mCi.getRadioState() == RadioState.RADIO_OFF) {
            msg = "Radio is off. Stopped cell info retry. Cleared the previous cached cell info.";
            if (this.mCellInfo != null) {
                this.mCellInfo.clear();
            }
            log(msg);
            this.mLocalLog.log(msg);
            stopCellInfoRetry();
            return;
        }
        this.mCellInfo = this.mPhone.getAllCellInfo(null);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCellInfo: cell info=");
        stringBuilder.append(this.mCellInfo);
        msg = stringBuilder.toString();
        log(msg);
        this.mLocalLog.log(msg);
        if (CollectionUtils.isEmpty(this.mCellInfo)) {
            int i = this.mFailCellInfoCount + 1;
            this.mFailCellInfoCount = i;
            long delay = getCellInfoDelayTime(i);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Can't get cell info. Try again in ");
            stringBuilder2.append(delay / 1000);
            stringBuilder2.append(" secs.");
            log(stringBuilder2.toString());
            removeMessages(1);
            sendMessageDelayed(obtainMessage(1), delay);
        } else {
            stopCellInfoRetry();
            sendMessageDelayed(obtainMessage(1), 600000);
        }
    }

    private void updateLocale() {
        StringBuilder stringBuilder;
        String mcc = null;
        String countryIso = "";
        if (!TextUtils.isEmpty(this.mOperatorNumeric)) {
            try {
                mcc = this.mOperatorNumeric.substring(0, 3);
                countryIso = MccTable.countryCodeForMcc(Integer.parseInt(mcc));
            } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateLocale: Can't get country from operator numeric. mcc = ");
                stringBuilder.append(mcc);
                stringBuilder.append(". ex=");
                stringBuilder.append(ex);
                loge(stringBuilder.toString());
            }
        }
        if (TextUtils.isEmpty(countryIso)) {
            mcc = getMccFromCellInfo();
            if (!TextUtils.isEmpty(mcc)) {
                try {
                    countryIso = MccTable.countryCodeForMcc(Integer.parseInt(mcc));
                } catch (NumberFormatException ex2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLocale: Can't get country from cell info. mcc = ");
                    stringBuilder.append(mcc);
                    stringBuilder.append(". ex=");
                    stringBuilder.append(ex2);
                    loge(stringBuilder.toString());
                }
            }
        }
        String msg = new StringBuilder();
        msg.append("updateLocale: mcc = ");
        msg.append(mcc);
        msg.append(", country = ");
        msg.append(countryIso);
        msg = msg.toString();
        log(msg);
        this.mLocalLog.log(msg);
        if (!Objects.equals(countryIso, this.mCurrentCountryIso)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateLocale: Change the current country to ");
            stringBuilder.append(countryIso);
            msg = stringBuilder.toString();
            log(msg);
            this.mLocalLog.log(msg);
            this.mCurrentCountryIso = countryIso;
            TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "gsm.operator.iso-country", this.mCurrentCountryIso);
            ((WifiManager) this.mPhone.getContext().getSystemService("wifi")).setCountryCode(countryIso);
        }
    }

    private void log(String msg) {
        Rlog.d(TAG, msg);
    }

    private void loge(String msg) {
        Rlog.e(TAG, msg);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        pw.println("LocaleTracker:");
        ipw.increaseIndent();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mOperatorNumeric = ");
        stringBuilder.append(this.mOperatorNumeric);
        ipw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mSimState = ");
        stringBuilder.append(this.mSimState);
        ipw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCellInfo = ");
        stringBuilder.append(this.mCellInfo);
        ipw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCurrentCountryIso = ");
        stringBuilder.append(this.mCurrentCountryIso);
        ipw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mFailCellInfoCount = ");
        stringBuilder.append(this.mFailCellInfoCount);
        ipw.println(stringBuilder.toString());
        ipw.println("Local logs:");
        ipw.increaseIndent();
        this.mLocalLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
        ipw.decreaseIndent();
        ipw.flush();
    }
}
