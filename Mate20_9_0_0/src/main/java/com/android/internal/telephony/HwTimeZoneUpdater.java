package com.android.internal.telephony;

import android.app.AlarmManager;
import android.content.Context;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.vsim.HwVSimUtils;
import com.android.internal.telephony.vsim.VSimUtilsInner;
import java.util.List;
import java.util.TimeZone;
import libcore.util.TimeZoneFinder;

public class HwTimeZoneUpdater {
    private static final int EVENT_AUTO_TIME_ZONE_CHANGED = 1;
    private static final int EVENT_MCC_CHANGED = 2;
    private static final int MCC_LEN = 3;
    private static final int PHONE_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static final int SINGLE_TIME_ZONE = 1;
    private static final String TAG = "HwTimeZoneUpdater";
    private static final boolean USE_LOCATION_TIME_ZONE = SystemProperties.getBoolean("ro.config.location_time_zone", true);
    private Context mContext;
    private Handler mHandler;
    private HwLocationBasedTimeZoneUpdater mHwLocTzUpdater;
    private NitzStateMachine mNitzState;
    private SettingsObserver mSettingsObserver;

    private class MyHandler extends Handler {
        public MyHandler(Looper l) {
            super(l);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    HwTimeZoneUpdater.this.log("EVENT_AUTO_TIME_ZONE_CHANGED");
                    if (HwTimeZoneUpdater.this.isAutomaticTimeZoneRequested()) {
                        HwTimeZoneUpdater.this.checkAutoTimeZoneForRplmn();
                        return;
                    }
                    return;
                case 2:
                    HwTimeZoneUpdater.this.log("EVENT_MCC_CHANGED");
                    HwTimeZoneUpdater.this.onMccChanged((AsyncResult) msg.obj);
                    return;
                default:
                    HwTimeZoneUpdater.this.log("unknown message, ignore");
                    return;
            }
        }
    }

    private static class SettingsObserver extends ContentObserver {
        private Handler mHandler;
        private int mMsg;

        SettingsObserver(Handler handler, int msg) {
            super(handler);
            this.mHandler = handler;
            this.mMsg = msg;
        }

        void observe(Context context) {
            context.getContentResolver().registerContentObserver(Global.getUriFor("auto_time_zone"), false, this);
        }

        public void onChange(boolean selfChange) {
            this.mHandler.obtainMessage(this.mMsg).sendToTarget();
        }
    }

    public HwTimeZoneUpdater(Context context) {
        this.mContext = context;
        init();
        if (USE_LOCATION_TIME_ZONE) {
            this.mHwLocTzUpdater = HwLocationBasedTimeZoneUpdater.init(context);
            this.mHwLocTzUpdater.start();
        }
        HwDualCardsLocationTimeZoneUpdate.init(context);
    }

    private void init() {
        log("init...");
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new MyHandler(thread.getLooper());
        this.mSettingsObserver = new SettingsObserver(this.mHandler, 1);
        this.mSettingsObserver.observe(this.mContext);
        for (int i = 0; i < PHONE_NUM; i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone != null) {
                phone.registerForMccChanged(this.mHandler, 2, Integer.valueOf(i));
            }
        }
        Phone phone2 = PhoneFactory.getDefaultPhone();
        ServiceStateTracker serviceStateTracker = phone2 != null ? phone2.getServiceStateTracker() : null;
        if (serviceStateTracker != null) {
            this.mNitzState = serviceStateTracker.getNitzState();
        }
    }

    private void setTimeZoneByRplmn(String zoneId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setTimeZoneByRplmn: zoneId=");
        stringBuilder.append(zoneId);
        log(stringBuilder.toString());
        ((AlarmManager) this.mContext.getSystemService("alarm")).setTimeZone(zoneId);
        HwTelephonyFactory.getHwReportManager().reportSetTimeZoneByIso(PhoneFactory.getDefaultPhone(), zoneId, false, "RplmnIso");
    }

    private boolean isAutomaticTimeZoneRequested() {
        return Global.getInt(this.mContext.getContentResolver(), "auto_time_zone", 0) != 0;
    }

    private void onMccChanged(AsyncResult ar) {
        Integer slotId = ar.userObj;
        String mcc = ar.result;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[SLOT");
        stringBuilder.append(slotId);
        stringBuilder.append("] rplmn mcc changed, mcc");
        stringBuilder.append(mcc);
        log(stringBuilder.toString());
        if (!checkAllCardsNotRegInService()) {
            log("onMccChanged, any card is in service, return.");
        } else if (isAutomaticTimeZoneRequested()) {
            String currentZoneId = TimeZone.getDefault().getID();
            String zoneId = getTimeZoneId(mcc);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onMccChanged, zoneId:");
            stringBuilder2.append(zoneId);
            stringBuilder2.append(", currentZoneId:");
            stringBuilder2.append(currentZoneId);
            log(stringBuilder2.toString());
            if (!(TextUtils.isEmpty(zoneId) || zoneId.equals(currentZoneId))) {
                setTimeZoneByRplmn(zoneId);
            }
        } else {
            log("onMccChanged, auto timezone disabled.");
        }
    }

    private void checkAutoTimeZoneForRplmn() {
        if (checkAllCardsNotRegInService()) {
            String mcc = "";
            String rplmn = "";
            for (int i = 0; i < PHONE_NUM; i++) {
                Phone phone = PhoneFactory.getPhone(i);
                ServiceStateTracker sst = phone != null ? phone.getServiceStateTracker() : null;
                rplmn = sst != null ? sst.getRplmn() : "";
                if (!TextUtils.isEmpty(rplmn)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("checkAutoTimeZoneForRplmn, got valid rplmn: ");
                    stringBuilder.append(rplmn);
                    log(stringBuilder.toString());
                    break;
                }
            }
            if (!TextUtils.isEmpty(rplmn) && rplmn.length() > 3) {
                mcc = rplmn.substring(0, 3);
            }
            String currentZoneId = TimeZone.getDefault().getID();
            String zoneId = getTimeZoneId(mcc);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("checkAutoTimeZoneForRplmn, zoneId:");
            stringBuilder2.append(zoneId);
            stringBuilder2.append(", currentZoneId:");
            stringBuilder2.append(currentZoneId);
            log(stringBuilder2.toString());
            if (!(TextUtils.isEmpty(zoneId) || zoneId.equals(currentZoneId))) {
                setTimeZoneByRplmn(zoneId);
            }
            return;
        }
        log("checkAutoTimeZoneForRplmn, any card is in service, return.");
    }

    private boolean checkAllCardsNotRegInService() {
        if (HwVSimUtils.isVSimEnabled() && isRegInService(VSimUtilsInner.getVSimPhone())) {
            log("checkAllCardsNotRegInService, vsim is in service");
            return false;
        }
        for (int i = 0; i < PHONE_NUM; i++) {
            if (isRegInService(PhoneFactory.getPhone(i))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkAllCardsNotRegInService, [SLOT");
                stringBuilder.append(i);
                stringBuilder.append("] is in service");
                log(stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    private boolean isRegInService(Phone phone) {
        ServiceState serviceState = phone != null ? phone.getServiceState() : null;
        boolean z = false;
        if (serviceState == null) {
            return false;
        }
        if (serviceState.getVoiceRegState() == 0 || serviceState.getDataRegState() == 0) {
            z = true;
        }
        return z;
    }

    private String getTimeZoneId(String mcc) {
        if (TextUtils.isEmpty(mcc)) {
            return "";
        }
        String zoneId = this.mNitzState != null ? this.mNitzState.getTimeZoneFromMcc(mcc) : null;
        if (zoneId != null) {
            return zoneId;
        }
        String countryIso = "";
        try {
            countryIso = MccTable.countryCodeForMcc(Integer.parseInt(mcc));
        } catch (NumberFormatException e) {
            loge("countryCodeForMcc NumberFormatException");
        }
        if (!TextUtils.isEmpty(countryIso)) {
            List<android.icu.util.TimeZone> timeZones = TimeZoneFinder.getInstance().lookupTimeZonesByCountry(countryIso);
            if (timeZones != null && timeZones.size() == 1) {
                return ((android.icu.util.TimeZone) timeZones.get(0)).getID();
            }
            log("timeZones more than one.");
        }
        return "";
    }

    protected void log(String s) {
        Rlog.d(TAG, s);
    }

    protected void loge(String s) {
        Rlog.e(TAG, s);
    }
}
