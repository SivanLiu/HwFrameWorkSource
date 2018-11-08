package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import com.android.internal.telephony.vsim.HwVSimUtils;
import java.util.TimeZone;

public class HwReportManagerImpl implements HwReportManager {
    private static final int AUTO_TIME_ZONE_OFF = 0;
    private static final int INVAILUE_TZ_OFFSET = -1;
    private static final String LOG_TAG = "HwReportManagerImpl";
    private static final int MAX_REPORT_TIME = 5;
    private static final String PROPERTY_GLOBAL_OPERATOR_NUMERIC = "ril.operator.numeric";
    private static final long RESET_REPORT_TIMES_INTERVAL = 86400000;
    private static long mInitTime;
    private static HwReportManager mInstance = new HwReportManagerImpl();
    private static int mReportTimes;
    private String mMccMnc = "";
    private String mMccMncOtherSlot = "";
    private String mPreForidden = "";
    private long mPreOffset;
    private String mPreZoneId = "";
    private String mSimOperator = "";
    private String mSimOperatorOtherSlot = "";

    private HwReportManagerImpl() {
        mInitTime = SystemClock.elapsedRealtime();
    }

    public static HwReportManager getDefault() {
        if (SystemClock.elapsedRealtime() - mInitTime >= RESET_REPORT_TIMES_INTERVAL) {
            mReportTimes = 0;
        }
        return mInstance;
    }

    private void getRegisteredInfo() {
        this.mMccMnc = TelephonyManager.getDefault().getNetworkOperator(0);
        this.mSimOperator = TelephonyManager.getDefault().getSimOperator(0);
        this.mMccMncOtherSlot = TelephonyManager.getDefault().getNetworkOperator(1);
        this.mSimOperatorOtherSlot = TelephonyManager.getDefault().getSimOperator(1);
    }

    private void setRegisteredInfoParam(EventStream eventStream) {
        synchronized (eventStream) {
            eventStream.setParam((short) 0, this.mMccMnc);
            eventStream.setParam((short) 1, this.mSimOperator);
            eventStream.setParam((short) 2, this.mMccMncOtherSlot);
            eventStream.setParam((short) 3, this.mSimOperatorOtherSlot);
        }
    }

    public void reportSetTimeZoneByNitz(Phone phone, String zoneId, int tzOffset, String source) {
        log("reportSetTimeZoneByNitz  tzOffset:" + tzOffset);
        int subId = phone.getPhoneId();
        getRegisteredInfo();
        if (tzOffset != -1) {
            if (((long) tzOffset) == this.mPreOffset || mReportTimes > 5) {
                log("tzOffset is not chaneged or report time is " + mReportTimes);
                return;
            }
            this.mPreOffset = (long) tzOffset;
        } else if (zoneId.equals(this.mPreZoneId) || mReportTimes > 5) {
            log("zoneId is not chaneged.");
            return;
        } else {
            this.mPreZoneId = zoneId;
        }
        EventStream regInfoStream = IMonitor.openEventStream(907047001);
        if (regInfoStream != null) {
            setRegisteredInfoParam(regInfoStream);
        }
        EventStream eventStream = IMonitor.openEventStream(907047002);
        if (eventStream != null) {
            eventStream.fillArrayParam((short) 0, regInfoStream);
            eventStream.setParam((short) 1, zoneId);
            eventStream.setParam((short) 2, getAutoTimeZone());
            eventStream.setParam((short) 3, subId);
            eventStream.setParam((short) 4, source);
            IMonitor.sendEvent(eventStream);
            mReportTimes++;
            log("Send infomation this report time is :" + mReportTimes);
        }
        IMonitor.closeEventStream(regInfoStream);
        IMonitor.closeEventStream(eventStream);
    }

    public void reportSetTimeZoneByIso(Phone phone, String zoneId, boolean mNitzUpdatedTime, String source) {
        log("reportSetTimeZoneByIso  nitzUpdatedTime:" + mNitzUpdatedTime + " source: " + source);
        getRegisteredInfo();
        if (!isAllowedReport(zoneId) || mReportTimes > 5) {
            log("zoneId is not chaneged or report time is " + mReportTimes);
            return;
        }
        EventStream regInfoStream = IMonitor.openEventStream(907047001);
        if (regInfoStream != null) {
            setRegisteredInfoParam(regInfoStream);
        }
        EventStream eventStream = IMonitor.openEventStream(907047003);
        if (eventStream != null) {
            eventStream.fillArrayParam((short) 0, regInfoStream);
            eventStream.setParam((short) 1, Boolean.valueOf(mNitzUpdatedTime));
            eventStream.setParam((short) 2, getAutoTimeZone());
            eventStream.setParam((short) 3, SystemProperties.get(PROPERTY_GLOBAL_OPERATOR_NUMERIC, ""));
            eventStream.setParam((short) 4, source);
            IMonitor.sendEvent(eventStream);
            mReportTimes++;
            log("Send infomation this report time is :" + mReportTimes);
        }
        IMonitor.closeEventStream(regInfoStream);
        IMonitor.closeEventStream(eventStream);
    }

    public void reportNitzIgnore(int phoneId, String forbidden) {
        log("reportNitzIgnore  phoneId : " + phoneId + "forbidden:" + forbidden);
        getRegisteredInfo();
        if (this.mPreForidden.equals(forbidden) || mReportTimes > 5) {
            log("forbidden condition is not chaneged or report time is " + mReportTimes);
            return;
        }
        this.mPreForidden = forbidden;
        EventStream regInfoStream = IMonitor.openEventStream(907047001);
        if (regInfoStream != null) {
            setRegisteredInfoParam(regInfoStream);
        }
        EventStream eventStream = IMonitor.openEventStream(907047004);
        if (eventStream != null) {
            eventStream.fillArrayParam((short) 0, regInfoStream);
            eventStream.setParam((short) 1, phoneId);
            eventStream.setParam((short) 2, Boolean.valueOf(HwVSimUtils.isVSimEnabled()));
            eventStream.setParam((short) 4, forbidden);
            IMonitor.sendEvent(eventStream);
            mReportTimes++;
            log("Send infomation this report time is :" + mReportTimes);
        }
        IMonitor.closeEventStream(regInfoStream);
        IMonitor.closeEventStream(eventStream);
    }

    public void reportMultiTZRegistered() {
        boolean z = false;
        log("reportMultiTZRegistered");
        getRegisteredInfo();
        EventStream regInfoStream = IMonitor.openEventStream(907047001);
        if (regInfoStream != null) {
            setRegisteredInfoParam(regInfoStream);
        }
        EventStream eventStream = IMonitor.openEventStream(907047005);
        if (eventStream != null) {
            eventStream.fillArrayParam((short) 0, regInfoStream);
            if (getLocationMode() != 0) {
                z = true;
            }
            eventStream.setParam((short) 2, Boolean.valueOf(z));
            eventStream.setParam((short) 3, getLocationMode());
            eventStream.setParam((short) 3, getAutoTimeZone());
            eventStream.setParam((short) 4, Boolean.valueOf(isNetworkAvailable()));
            IMonitor.sendEvent(eventStream);
        }
        IMonitor.closeEventStream(regInfoStream);
        IMonitor.closeEventStream(eventStream);
    }

    public void reportMultiTZNoNitz() {
        boolean z = true;
        log("reportMultiTZNoNitz");
        getRegisteredInfo();
        EventStream regInfoStream = IMonitor.openEventStream(907047001);
        if (regInfoStream != null) {
            setRegisteredInfoParam(regInfoStream);
        }
        EventStream eventStream = IMonitor.openEventStream(907047006);
        if (eventStream != null) {
            eventStream.fillArrayParam((short) 0, regInfoStream);
            eventStream.setParam((short) 1, TimeZone.getDefault().getID());
            if (getLocationMode() == 0) {
                z = false;
            }
            eventStream.setParam((short) 2, Boolean.valueOf(z));
            eventStream.setParam((short) 3, getLocationMode());
            eventStream.setParam((short) 4, getAutoTimeZone());
            eventStream.setParam((short) 5, Boolean.valueOf(isNetworkAvailable()));
            IMonitor.sendEvent(eventStream);
        }
        IMonitor.closeEventStream(regInfoStream);
        IMonitor.closeEventStream(eventStream);
    }

    public void reportSetTimeZoneByLocation(String zoneId) {
        boolean z = true;
        log("reportSetTimeZoneByLocation");
        getRegisteredInfo();
        if (!isAllowedReport(zoneId) || mReportTimes > 5) {
            log("zoneId is not chaneged or report time is " + mReportTimes);
            return;
        }
        EventStream regInfoStream = IMonitor.openEventStream(907047001);
        if (regInfoStream != null) {
            setRegisteredInfoParam(regInfoStream);
        }
        EventStream eventStream = IMonitor.openEventStream(907047007);
        if (eventStream != null) {
            eventStream.fillArrayParam((short) 0, regInfoStream);
            eventStream.setParam((short) 1, zoneId);
            if (getLocationMode() == 0) {
                z = false;
            }
            eventStream.setParam((short) 2, Boolean.valueOf(z));
            eventStream.setParam((short) 3, getLocationMode());
            eventStream.setParam((short) 4, getAutoTimeZone());
            eventStream.setParam((short) 5, Boolean.valueOf(isNetworkAvailable()));
            IMonitor.sendEvent(eventStream);
            mReportTimes++;
            log("Send infomation this report time is :" + mReportTimes);
        }
        IMonitor.closeEventStream(regInfoStream);
        IMonitor.closeEventStream(eventStream);
    }

    private int getAutoTimeZone() {
        Context context = null;
        if (PhoneFactory.getDefaultPhone() != null) {
            context = PhoneFactory.getDefaultPhone().getContext();
        }
        if (context != null) {
            ContentResolver cr = context.getContentResolver();
            if (cr != null) {
                return Global.getInt(cr, "auto_time_zone", 0);
            }
            log(" cr is null.");
            return 0;
        }
        log(" context is null, can not get cr.");
        return 0;
    }

    private boolean isAllowedReport(String zoneId) {
        String str = TimeZone.getDefault().getID();
        return str != null ? str.equalsIgnoreCase(zoneId) ^ 1 : true;
    }

    private int getLocationMode() {
        Context context = null;
        if (PhoneFactory.getDefaultPhone() != null) {
            context = PhoneFactory.getDefaultPhone().getContext();
        }
        if (context != null) {
            ContentResolver cr = context.getContentResolver();
            if (cr != null) {
                return Secure.getInt(cr, "location_mode", 0);
            }
            log(" cr is null.");
            return 0;
        }
        log(" context is null, can not get cr.");
        return 0;
    }

    private boolean isNetworkAvailable() {
        boolean z = false;
        Context context = null;
        if (PhoneFactory.getDefaultPhone() != null) {
            context = PhoneFactory.getDefaultPhone().getContext();
        }
        if (context == null) {
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        NetworkInfo activeNetworkInfo = cm == null ? null : cm.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            z = activeNetworkInfo.isConnected();
        }
        return z;
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    protected void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }
}
