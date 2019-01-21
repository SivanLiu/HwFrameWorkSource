package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.TimeUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.TimeServiceHelper.Listener;
import com.android.internal.telephony.TimeZoneLookupHelper.CountryResult;
import com.android.internal.telephony.TimeZoneLookupHelper.OffsetResult;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.util.TimeStampedValue;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.TimeZone;

public class NitzStateMachine {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "NitzStateMachine";
    private static final int NUMERIC_MIN_LEN = 5;
    private static final int VSIM_SUBID = 2;
    private static final String WAKELOCK_TAG = "NitzStateMachine";
    private final DeviceState mDeviceState;
    private boolean mGotCountryCode;
    private TimeStampedValue<NitzData> mLatestNitzSignal;
    private boolean mNeedCountryCodeForNitz;
    private boolean mNitzTimeZoneDetectionSuccessful;
    private final GsmCdmaPhone mPhone;
    private TimeStampedValue<Long> mSavedNitzTime;
    private String mSavedTimeZoneId;
    private final LocalLog mTimeLog;
    private final TimeServiceHelper mTimeServiceHelper;
    private final LocalLog mTimeZoneLog;
    private final TimeZoneLookupHelper mTimeZoneLookupHelper;
    private final WakeLock mWakeLock;

    public static class DeviceState {
        private static final int NITZ_UPDATE_DIFF_DEFAULT = 2000;
        private static final int NITZ_UPDATE_SPACING_DEFAULT = 600000;
        private final ContentResolver mCr;
        private final int mNitzUpdateDiff = SystemProperties.getInt("ro.nitz_update_diff", 2000);
        private final int mNitzUpdateSpacing = SystemProperties.getInt("ro.nitz_update_spacing", NITZ_UPDATE_SPACING_DEFAULT);
        private final GsmCdmaPhone mPhone;
        private final TelephonyManager mTelephonyManager;

        public DeviceState(GsmCdmaPhone phone) {
            this.mPhone = phone;
            Context context = phone.getContext();
            this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
            this.mCr = context.getContentResolver();
        }

        public int getNitzUpdateSpacingMillis() {
            return Global.getInt(this.mCr, "nitz_update_spacing", this.mNitzUpdateSpacing);
        }

        public int getNitzUpdateDiffMillis() {
            return Global.getInt(this.mCr, "nitz_update_diff", this.mNitzUpdateDiff);
        }

        public boolean getIgnoreNitz() {
            String ignoreNitz = SystemProperties.get("gsm.ignore-nitz");
            return ignoreNitz != null && ignoreNitz.equals("yes");
        }

        public String getNetworkCountryIsoForPhone() {
            if (this.mPhone.getPhoneId() == 2) {
                return SystemProperties.get("gsm.operator.iso-country.vsim", "");
            }
            return this.mTelephonyManager.getNetworkCountryIsoForPhone(this.mPhone.getPhoneId());
        }
    }

    public NitzStateMachine(GsmCdmaPhone phone) {
        this(phone, new TimeServiceHelper(phone.getContext()), new DeviceState(phone), new TimeZoneLookupHelper());
    }

    @VisibleForTesting
    public NitzStateMachine(GsmCdmaPhone phone, TimeServiceHelper timeServiceHelper, DeviceState deviceState, TimeZoneLookupHelper timeZoneLookupHelper) {
        this.mNeedCountryCodeForNitz = false;
        this.mGotCountryCode = false;
        this.mNitzTimeZoneDetectionSuccessful = false;
        this.mTimeLog = new LocalLog(15);
        this.mTimeZoneLog = new LocalLog(15);
        this.mPhone = phone;
        this.mWakeLock = ((PowerManager) phone.getContext().getSystemService("power")).newWakeLock(1, "NitzStateMachine");
        this.mDeviceState = deviceState;
        this.mTimeZoneLookupHelper = timeZoneLookupHelper;
        this.mTimeServiceHelper = timeServiceHelper;
        this.mTimeServiceHelper.setListener(new Listener() {
            public void onTimeDetectionChange(boolean enabled) {
                if (enabled) {
                    NitzStateMachine.this.handleAutoTimeEnabled();
                }
            }

            public void onTimeZoneDetectionChange(boolean enabled) {
                if (enabled) {
                    NitzStateMachine.this.handleAutoTimeZoneEnabled();
                }
            }
        });
    }

    public void handleNetworkCountryCodeSet(boolean countryChanged) {
        Throwable th;
        this.mGotCountryCode = true;
        String isoCountryCode = this.mDeviceState.getNetworkCountryIsoForPhone();
        if (!(TextUtils.isEmpty(isoCountryCode) || this.mNitzTimeZoneDetectionSuccessful || !this.mTimeServiceHelper.isTimeZoneDetectionEnabled())) {
            updateTimeZoneByNetworkCountryCode(isoCountryCode);
        }
        if (countryChanged || this.mNeedCountryCodeForNitz) {
            String zoneId;
            StringBuilder stringBuilder;
            boolean isTimeZoneSettingInitialized = this.mTimeServiceHelper.isTimeZoneSettingInitialized();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleNetworkCountryCodeSet: isTimeZoneSettingInitialized=");
            stringBuilder2.append(isTimeZoneSettingInitialized);
            stringBuilder2.append(" mLatestNitzSignal=");
            stringBuilder2.append(this.mLatestNitzSignal);
            stringBuilder2.append(" isoCountryCode=");
            stringBuilder2.append(isoCountryCode);
            Rlog.d("NitzStateMachine", stringBuilder2.toString());
            boolean fixZoneByNitz = this.mNeedCountryCodeForNitz;
            String str = null;
            StringBuilder stringBuilder3;
            if (TextUtils.isEmpty(isoCountryCode) && this.mNeedCountryCodeForNitz) {
                OffsetResult lookupResult = this.mTimeZoneLookupHelper.lookupByNitz((NitzData) this.mLatestNitzSignal.mValue);
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("handleNetworkCountryCodeSet: guessZoneIdByNitz() returned lookupResult=");
                stringBuilder3.append(lookupResult);
                Rlog.d("NitzStateMachine", stringBuilder3.toString());
                if (lookupResult != null) {
                    str = lookupResult.zoneId;
                }
                zoneId = str;
            } else if (this.mLatestNitzSignal == null) {
                zoneId = null;
                Rlog.d("NitzStateMachine", "handleNetworkCountryCodeSet: No cached NITZ data available, not setting zone");
            } else if (nitzOffsetMightBeBogus((NitzData) this.mLatestNitzSignal.mValue) && isTimeZoneSettingInitialized && !countryUsesUtc(isoCountryCode, this.mLatestNitzSignal)) {
                String zoneId2;
                TimeZone zone = TimeZone.getDefault();
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleNetworkCountryCodeSet: NITZ looks bogus, maybe using current default zone to adjust the system clock, mNeedCountryCodeForNitz=");
                stringBuilder.append(this.mNeedCountryCodeForNitz);
                stringBuilder.append(" mLatestNitzSignal=");
                stringBuilder.append(this.mLatestNitzSignal);
                stringBuilder.append(" zone=");
                stringBuilder.append(zone);
                Rlog.d("NitzStateMachine", stringBuilder.toString());
                String zoneId3 = zone.getID();
                if (this.mNeedCountryCodeForNitz) {
                    NitzData nitzData = (NitzData) this.mLatestNitzSignal.mValue;
                    try {
                        this.mWakeLock.acquire();
                        long delayAdjustedCtm = (this.mTimeServiceHelper.elapsedRealtime() - this.mLatestNitzSignal.mElapsedRealtime) + nitzData.getCurrentTimeInMillis();
                        long tzOffset = (long) zone.getOffset(delayAdjustedCtm);
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("handleNetworkCountryCodeSet: tzOffset=");
                        stringBuilder4.append(tzOffset);
                        stringBuilder4.append(" delayAdjustedCtm=");
                        stringBuilder4.append(TimeUtils.logTimeOfDay(delayAdjustedCtm));
                        Rlog.d("NitzStateMachine", stringBuilder4.toString());
                        if (this.mTimeServiceHelper.isTimeDetectionEnabled()) {
                            zoneId2 = zoneId3;
                            zone = delayAdjustedCtm - tzOffset;
                            try {
                                zoneId = new StringBuilder();
                                zoneId.append("handleNetworkCountryCodeSet: setting time timeZoneAdjustedCtm=");
                                zoneId.append(TimeUtils.logTimeOfDay(zone));
                                setAndBroadcastNetworkSetTime(zoneId.toString(), zone);
                            } catch (Throwable th2) {
                                th = th2;
                                this.mWakeLock.release();
                                throw th;
                            }
                        }
                        zoneId2 = zoneId3;
                        this.mSavedNitzTime = new TimeStampedValue(Long.valueOf(((Long) this.mSavedNitzTime.mValue).longValue() - tzOffset), this.mSavedNitzTime.mElapsedRealtime);
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("handleNetworkCountryCodeSet:adjusting time mSavedNitzTime=");
                        stringBuilder5.append(this.mSavedNitzTime);
                        Rlog.d("NitzStateMachine", stringBuilder5.toString());
                        this.mWakeLock.release();
                    } catch (Throwable th3) {
                        th = th3;
                        TimeZone timeZone = zone;
                        zoneId2 = zoneId3;
                        this.mWakeLock.release();
                        throw th;
                    }
                }
                zoneId2 = zoneId3;
                zoneId = zoneId2;
            } else {
                NitzData nitzData2 = this.mLatestNitzSignal.mValue;
                OffsetResult lookupResult2 = this.mTimeZoneLookupHelper.lookupByNitzCountry(nitzData2, isoCountryCode);
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("handleNetworkCountryCodeSet: using guessZoneIdByNitzCountry(nitzData, isoCountryCode), nitzData=");
                stringBuilder3.append(nitzData2);
                stringBuilder3.append(" isoCountryCode=");
                stringBuilder3.append(isoCountryCode);
                stringBuilder3.append(" lookupResult=");
                stringBuilder3.append(lookupResult2);
                Rlog.d("NitzStateMachine", stringBuilder3.toString());
                if (lookupResult2 != null) {
                    str = lookupResult2.zoneId;
                }
                zoneId = str;
            }
            String tmpLog = new StringBuilder();
            tmpLog.append("handleNetworkCountryCodeSet: isTimeZoneSettingInitialized=");
            tmpLog.append(isTimeZoneSettingInitialized);
            tmpLog.append(" mLatestNitzSignal=");
            tmpLog.append(this.mLatestNitzSignal);
            tmpLog.append(" isoCountryCode=");
            tmpLog.append(isoCountryCode);
            tmpLog.append(" mNeedCountryCodeForNitz=");
            tmpLog.append(this.mNeedCountryCodeForNitz);
            tmpLog.append(" zoneId=");
            tmpLog.append(zoneId);
            this.mTimeZoneLog.log(tmpLog.toString());
            if (zoneId != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleNetworkCountryCodeSet: zoneId != null, zoneId=");
                stringBuilder.append(zoneId);
                Rlog.d("NitzStateMachine", stringBuilder.toString());
                if (this.mTimeServiceHelper.isTimeZoneDetectionEnabled()) {
                    setAndBroadcastNetworkSetTimeZone(zoneId);
                    if (fixZoneByNitz && this.mPhone.getServiceStateTracker() != null) {
                        HwTelephonyFactory.getHwNetworkManager().sendNitzTimeZoneUpdateMessage(this.mPhone.getServiceStateTracker().getCellLocationInfo());
                    }
                    HwTelephonyFactory.getHwReportManager().reportSetTimeZoneByNitz(this.mPhone, zoneId, -1, "NitzFix");
                } else {
                    Rlog.d("NitzStateMachine", "handleNetworkCountryCodeSet: skip changing zone as isTimeZoneDetectionEnabled() is false");
                }
                if (this.mNeedCountryCodeForNitz) {
                    this.mSavedTimeZoneId = zoneId;
                }
            } else {
                Rlog.d("NitzStateMachine", "handleNetworkCountryCodeSet: lookupResult == null, do nothing");
            }
            this.mNeedCountryCodeForNitz = false;
        }
    }

    private boolean countryUsesUtc(String isoCountryCode, TimeStampedValue<NitzData> nitzSignal) {
        return this.mTimeZoneLookupHelper.countryUsesUtc(isoCountryCode, ((NitzData) nitzSignal.mValue).getCurrentTimeInMillis());
    }

    public void handleNetworkAvailable() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleNetworkAvailable: mNitzTimeZoneDetectionSuccessful=");
        stringBuilder.append(this.mNitzTimeZoneDetectionSuccessful);
        stringBuilder.append(", Setting mNitzTimeZoneDetectionSuccessful=false");
        Rlog.d("NitzStateMachine", stringBuilder.toString());
        this.mNitzTimeZoneDetectionSuccessful = false;
    }

    public void handleNetworkUnavailable() {
        Rlog.d("NitzStateMachine", "handleNetworkUnavailable");
        this.mGotCountryCode = false;
        this.mNitzTimeZoneDetectionSuccessful = false;
    }

    private static boolean nitzOffsetMightBeBogus(NitzData nitzData) {
        return nitzData.getLocalOffsetMillis() == 0 && !nitzData.isDst();
    }

    public void handleNitzReceived(TimeStampedValue<NitzData> nitzSignal) {
        handleTimeZoneFromNitz(nitzSignal);
        handleTimeFromNitz(nitzSignal);
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x00bb A:{Catch:{ RuntimeException -> 0x00f3 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleTimeZoneFromNitz(TimeStampedValue<NitzData> nitzSignal) {
        try {
            String zoneId;
            String tmpLog;
            NitzData newNitzData = nitzSignal.mValue;
            String iso = this.mDeviceState.getNetworkCountryIsoForPhone();
            if (newNitzData.getEmulatorHostTimeZone() != null) {
                zoneId = newNitzData.getEmulatorHostTimeZone().getID();
            } else if (this.mGotCountryCode) {
                String str = null;
                OffsetResult lookupResult;
                if (TextUtils.isEmpty(iso)) {
                    lookupResult = this.mTimeZoneLookupHelper.lookupByNitz(newNitzData);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleTimeZoneFromNitz: guessZoneIdByNitz returned lookupResult=");
                    stringBuilder.append(lookupResult);
                    Rlog.d("NitzStateMachine", stringBuilder.toString());
                    if (lookupResult != null) {
                        str = lookupResult.zoneId;
                    }
                    zoneId = str;
                } else {
                    lookupResult = this.mTimeZoneLookupHelper.lookupByNitzCountry(newNitzData, iso);
                    if (lookupResult != null) {
                        str = lookupResult.zoneId;
                    }
                    zoneId = str;
                }
                if (zoneId == null || this.mLatestNitzSignal == null || offsetInfoDiffers(newNitzData, (NitzData) this.mLatestNitzSignal.mValue)) {
                    this.mNeedCountryCodeForNitz = true;
                    this.mLatestNitzSignal = nitzSignal;
                }
                tmpLog = new StringBuilder();
                tmpLog.append("handleTimeZoneFromNitz: nitzSignal=");
                tmpLog.append(nitzSignal);
                tmpLog.append(" zoneId=");
                tmpLog.append(zoneId);
                tmpLog.append(" iso=");
                tmpLog.append(iso);
                tmpLog.append(" mGotCountryCode=");
                tmpLog.append(this.mGotCountryCode);
                tmpLog.append(" mNeedCountryCodeForNitz=");
                tmpLog.append(this.mNeedCountryCodeForNitz);
                tmpLog.append(" isTimeZoneDetectionEnabled()=");
                tmpLog.append(this.mTimeServiceHelper.isTimeZoneDetectionEnabled());
                tmpLog = tmpLog.toString();
                Rlog.d("NitzStateMachine", tmpLog);
                this.mTimeZoneLog.log(tmpLog);
                if (zoneId == null) {
                    if (this.mTimeServiceHelper.isTimeZoneDetectionEnabled()) {
                        setAndBroadcastNetworkSetTimeZone(zoneId);
                        if (this.mPhone.getServiceStateTracker() != null) {
                            HwTelephonyFactory.getHwNetworkManager().sendNitzTimeZoneUpdateMessage(this.mPhone.getServiceStateTracker().getCellLocationInfo());
                        }
                        HwTelephonyFactory.getHwReportManager().reportSetTimeZoneByNitz(this.mPhone, zoneId, newNitzData.getLocalOffsetMillis(), "Nitz");
                    }
                    this.mNitzTimeZoneDetectionSuccessful = true;
                    this.mSavedTimeZoneId = zoneId;
                    return;
                }
                return;
            } else {
                zoneId = null;
            }
            this.mNeedCountryCodeForNitz = true;
            this.mLatestNitzSignal = nitzSignal;
            tmpLog = new StringBuilder();
            tmpLog.append("handleTimeZoneFromNitz: nitzSignal=");
            tmpLog.append(nitzSignal);
            tmpLog.append(" zoneId=");
            tmpLog.append(zoneId);
            tmpLog.append(" iso=");
            tmpLog.append(iso);
            tmpLog.append(" mGotCountryCode=");
            tmpLog.append(this.mGotCountryCode);
            tmpLog.append(" mNeedCountryCodeForNitz=");
            tmpLog.append(this.mNeedCountryCodeForNitz);
            tmpLog.append(" isTimeZoneDetectionEnabled()=");
            tmpLog.append(this.mTimeServiceHelper.isTimeZoneDetectionEnabled());
            tmpLog = tmpLog.toString();
            Rlog.d("NitzStateMachine", tmpLog);
            this.mTimeZoneLog.log(tmpLog);
            if (zoneId == null) {
            }
        } catch (RuntimeException ex) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleTimeZoneFromNitz: Processing NITZ data nitzSignal=");
            stringBuilder2.append(nitzSignal);
            stringBuilder2.append(" ex=");
            stringBuilder2.append(ex);
            Rlog.e("NitzStateMachine", stringBuilder2.toString());
        }
    }

    private static boolean offsetInfoDiffers(NitzData one, NitzData two) {
        return (one.getLocalOffsetMillis() == two.getLocalOffsetMillis() && one.isDst() == two.isDst()) ? false : true;
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:34:0x0123=Splitter:B:34:0x0123, B:27:0x00dd=Splitter:B:27:0x00dd} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleTimeFromNitz(TimeStampedValue<NitzData> nitzSignal) {
        TimeStampedValue<NitzData> timeStampedValue = nitzSignal;
        try {
            if (this.mDeviceState.getIgnoreNitz()) {
                Rlog.d("NitzStateMachine", "handleTimeFromNitz: Not setting clock because gsm.ignore-nitz is set");
                return;
            }
            StringBuilder stringBuilder;
            this.mWakeLock.acquire();
            long elapsedRealtime = this.mTimeServiceHelper.elapsedRealtime();
            long millisSinceNitzReceived = elapsedRealtime - timeStampedValue.mElapsedRealtime;
            long j;
            if (millisSinceNitzReceived < 0) {
                j = millisSinceNitzReceived;
            } else if (millisSinceNitzReceived > 2147483647L) {
                j = millisSinceNitzReceived;
            } else {
                long adjustedCurrentTimeMillis = ((NitzData) timeStampedValue.mValue).getCurrentTimeInMillis() + millisSinceNitzReceived;
                long gained = adjustedCurrentTimeMillis - this.mTimeServiceHelper.currentTimeMillis();
                long j2;
                if (this.mTimeServiceHelper.isTimeDetectionEnabled()) {
                    String logMsg = new StringBuilder();
                    logMsg.append("handleTimeFromNitz: nitzSignal=");
                    logMsg.append(timeStampedValue);
                    logMsg.append(" adjustedCurrentTimeMillis=");
                    logMsg.append(adjustedCurrentTimeMillis);
                    logMsg.append(" millisSinceNitzReceived= ");
                    logMsg.append(millisSinceNitzReceived);
                    logMsg.append(" gained=");
                    logMsg.append(gained);
                    logMsg = logMsg.toString();
                    if (this.mSavedNitzTime == null) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(logMsg);
                        stringBuilder2.append(": First update received.");
                        setAndBroadcastNetworkSetTime(stringBuilder2.toString(), adjustedCurrentTimeMillis);
                        j = millisSinceNitzReceived;
                        j2 = gained;
                    } else {
                        long elapsedRealtimeSinceLastSaved = this.mTimeServiceHelper.elapsedRealtime() - this.mSavedNitzTime.mElapsedRealtime;
                        int nitzUpdateSpacing = this.mDeviceState.getNitzUpdateSpacingMillis();
                        int nitzUpdateDiff = this.mDeviceState.getNitzUpdateDiffMillis();
                        if (elapsedRealtimeSinceLastSaved <= ((long) nitzUpdateSpacing)) {
                            if (Math.abs(gained) <= ((long) nitzUpdateDiff)) {
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(logMsg);
                                stringBuilder3.append(": Update throttled.");
                                Rlog.d("NitzStateMachine", stringBuilder3.toString());
                                this.mWakeLock.release();
                                return;
                            }
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(logMsg);
                        stringBuilder.append(": New update received.");
                        setAndBroadcastNetworkSetTime(stringBuilder.toString(), adjustedCurrentTimeMillis);
                    }
                } else {
                    j2 = gained;
                }
                this.mSavedNitzTime = new TimeStampedValue(Long.valueOf(adjustedCurrentTimeMillis), timeStampedValue.mElapsedRealtime);
                SystemProperties.set("gsm.nitz.time", String.valueOf(adjustedCurrentTimeMillis));
                SystemProperties.set("gsm.nitz.timereference", String.valueOf(SystemClock.elapsedRealtime()));
                this.mWakeLock.release();
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("handleTimeFromNitz: not setting time, unexpected elapsedRealtime=");
            stringBuilder.append(elapsedRealtime);
            stringBuilder.append(" nitzSignal=");
            stringBuilder.append(timeStampedValue);
            Rlog.d("NitzStateMachine", stringBuilder.toString());
            this.mWakeLock.release();
        } catch (RuntimeException ex) {
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("handleTimeFromNitz: Processing NITZ data nitzSignal=");
            stringBuilder4.append(timeStampedValue);
            stringBuilder4.append(" ex=");
            stringBuilder4.append(ex);
            Rlog.e("NitzStateMachine", stringBuilder4.toString());
        } catch (Throwable th) {
            this.mWakeLock.release();
        }
    }

    private void setAndBroadcastNetworkSetTimeZone(String zoneId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAndBroadcastNetworkSetTimeZone: zoneId=");
        stringBuilder.append(zoneId);
        Rlog.d("NitzStateMachine", stringBuilder.toString());
        if (HwTelephonyFactory.getHwNetworkManager().isNeedLocationTimeZoneUpdate(this.mPhone, zoneId)) {
            Rlog.d("NitzStateMachine", "there is no need update time zone.");
            return;
        }
        this.mTimeServiceHelper.setDeviceTimeZone(zoneId);
        stringBuilder = new StringBuilder();
        stringBuilder.append("setAndBroadcastNetworkSetTimeZone: called setDeviceTimeZone() zoneId=");
        stringBuilder.append(zoneId);
        Rlog.d("NitzStateMachine", stringBuilder.toString());
    }

    private void setAndBroadcastNetworkSetTime(String msg, long time) {
        if (!this.mWakeLock.isHeld()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAndBroadcastNetworkSetTime: Wake lock not held while setting device time (msg=");
            stringBuilder.append(msg);
            stringBuilder.append(")");
            Rlog.w("NitzStateMachine", stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setAndBroadcastNetworkSetTime: [Setting time to time=");
        stringBuilder2.append(time);
        stringBuilder2.append("]:");
        stringBuilder2.append(msg);
        msg = stringBuilder2.toString();
        Rlog.d("NitzStateMachine", msg);
        this.mTimeLog.log(msg);
        this.mTimeServiceHelper.setDeviceTime(time);
        TelephonyMetrics.getInstance().writeNITZEvent(this.mPhone.getPhoneId(), time);
    }

    private void handleAutoTimeEnabled() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleAutoTimeEnabled: Reverting to NITZ Time: mSavedNitzTime=");
        stringBuilder.append(this.mSavedNitzTime);
        Rlog.d("NitzStateMachine", stringBuilder.toString());
        if (this.mSavedNitzTime != null) {
            try {
                this.mWakeLock.acquire();
                long elapsedRealtime = this.mTimeServiceHelper.elapsedRealtime();
                String msg = new StringBuilder();
                msg.append("mSavedNitzTime: Reverting to NITZ time elapsedRealtime=");
                msg.append(elapsedRealtime);
                msg.append(" mSavedNitzTime=");
                msg.append(this.mSavedNitzTime);
                setAndBroadcastNetworkSetTime(msg.toString(), ((Long) this.mSavedNitzTime.mValue).longValue() + (elapsedRealtime - this.mSavedNitzTime.mElapsedRealtime));
            } finally {
                this.mWakeLock.release();
            }
        }
    }

    public void handleAutoTimeZoneEnabled() {
        int airplaneMode = Global.getInt(this.mPhone.getContext().getContentResolver(), "airplane_mode_on", 0);
        String tmpLog = new StringBuilder();
        tmpLog.append("handleAutoTimeZoneEnabled: Reverting to NITZ TimeZone: mSavedTimeZoneId=");
        tmpLog.append(this.mSavedTimeZoneId);
        tmpLog.append(" airplaneMode=");
        tmpLog.append(airplaneMode);
        tmpLog = tmpLog.toString();
        Rlog.d("NitzStateMachine", tmpLog);
        this.mTimeZoneLog.log(tmpLog);
        if (this.mSavedTimeZoneId != null || airplaneMode > 0) {
            setAndBroadcastNetworkSetTimeZone(this.mSavedTimeZoneId);
            HwTelephonyFactory.getHwReportManager().reportSetTimeZoneByNitz(this.mPhone, this.mSavedTimeZoneId, -1, "AutoTZ");
            return;
        }
        String iso = this.mDeviceState.getNetworkCountryIsoForPhone();
        if (!TextUtils.isEmpty(iso)) {
            updateTimeZoneByNetworkCountryCode(iso);
        }
    }

    public void dumpState(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" mSavedTime=");
        stringBuilder.append(this.mSavedNitzTime);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mNeedCountryCodeForNitz=");
        stringBuilder.append(this.mNeedCountryCodeForNitz);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mLatestNitzSignal=");
        stringBuilder.append(this.mLatestNitzSignal);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mGotCountryCode=");
        stringBuilder.append(this.mGotCountryCode);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mSavedTimeZoneId=");
        stringBuilder.append(this.mSavedTimeZoneId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mNitzTimeZoneDetectionSuccessful=");
        stringBuilder.append(this.mNitzTimeZoneDetectionSuccessful);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mWakeLock=");
        stringBuilder.append(this.mWakeLock);
        pw.println(stringBuilder.toString());
        pw.flush();
    }

    public void dumpLogs(FileDescriptor fd, IndentingPrintWriter ipw, String[] args) {
        ipw.println(" Time Logs:");
        ipw.increaseIndent();
        this.mTimeLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
        ipw.println(" Time zone Logs:");
        ipw.increaseIndent();
        this.mTimeZoneLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
    }

    private void updateTimeZoneByNetworkCountryCode(String iso) {
        String numeric;
        CountryResult lookupResult = this.mTimeZoneLookupHelper.lookupByCountry(iso, this.mTimeServiceHelper.currentTimeMillis());
        if (this.mPhone.getPhoneId() == 2) {
            numeric = SystemProperties.get("gsm.operator.numeric.vsim", "");
        } else {
            numeric = this.mDeviceState.mTelephonyManager.getNetworkOperatorForPhone(this.mPhone.getPhoneId());
        }
        String mcc = null;
        if (!isInvalidOperatorNumeric(numeric)) {
            mcc = numeric.substring(0, 3);
        }
        boolean isNeedDefaultZone = isNeedDefaultZone(mcc);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateTimeZoneByNetworkCountryCode: mcc: ");
        stringBuilder.append(mcc);
        stringBuilder.append("  isNeedDefaultZone: ");
        stringBuilder.append(isNeedDefaultZone);
        stringBuilder.append(" lookupResult=");
        stringBuilder.append(lookupResult);
        Rlog.d("NitzStateMachine", stringBuilder.toString());
        if (lookupResult == null || !(lookupResult.allZonesHaveSameOffset || isNeedDefaultZone)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateTimeZoneByNetworkCountryCode: no good zone for iso=");
            stringBuilder.append(iso);
            stringBuilder.append(" lookupResult=");
            stringBuilder.append(lookupResult);
            Rlog.d("NitzStateMachine", stringBuilder.toString());
            return;
        }
        String logMsg = new StringBuilder();
        logMsg.append("updateTimeZoneByNetworkCountryCode: set time lookupResult=");
        logMsg.append(lookupResult);
        logMsg.append(" iso=");
        logMsg.append(iso);
        logMsg = logMsg.toString();
        Rlog.d("NitzStateMachine", logMsg);
        this.mTimeZoneLog.log(logMsg);
        String zoneId = !isNeedDefaultZone ? lookupResult.zoneId : getTimeZoneFromMcc(mcc);
        setAndBroadcastNetworkSetTimeZone(zoneId);
        HwReportManager hwReportManager = HwTelephonyFactory.getHwReportManager();
        GsmCdmaPhone gsmCdmaPhone = this.mPhone;
        boolean z = this.mNitzTimeZoneDetectionSuccessful;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(this.mPhone.getPhoneId());
        stringBuilder2.append(" Iso");
        hwReportManager.reportSetTimeZoneByIso(gsmCdmaPhone, zoneId, z, stringBuilder2.toString());
    }

    protected boolean isNeedDefaultZone(String mcc) {
        if ("460".equals(mcc) || "255".equals(mcc) || "214".equals(mcc)) {
            return true;
        }
        return false;
    }

    protected String getTimeZoneFromMcc(String mcc) {
        if ("460".equals(mcc)) {
            return "Asia/Shanghai";
        }
        if ("255".equals(mcc)) {
            return "Europe/Kiev";
        }
        if ("214".equals(mcc)) {
            return "Europe/Madrid";
        }
        return null;
    }

    protected boolean isInvalidOperatorNumeric(String operatorNumeric) {
        return operatorNumeric == null || operatorNumeric.length() < 5;
    }

    public boolean getNitzTimeZoneDetectionSuccessful() {
        return this.mNitzTimeZoneDetectionSuccessful;
    }

    public NitzData getCachedNitzData() {
        return this.mLatestNitzSignal != null ? (NitzData) this.mLatestNitzSignal.mValue : null;
    }

    public String getSavedTimeZoneId() {
        return this.mSavedTimeZoneId;
    }

    public long getNitzSpaceTime() {
        if (this.mSavedNitzTime != null) {
            return this.mTimeServiceHelper.elapsedRealtime() - this.mSavedNitzTime.mElapsedRealtime;
        }
        return this.mTimeServiceHelper.elapsedRealtime();
    }
}
