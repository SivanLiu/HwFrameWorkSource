package com.android.server.location;

import android.content.Context;
import android.location.Country;
import android.location.CountryListener;
import android.location.Geocoder;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ComprehensiveCountryDetector extends CountryDetectorBase {
    static final boolean DEBUG = false;
    private static final long LOCATION_REFRESH_INTERVAL = 86400000;
    private static final int MAX_LENGTH_DEBUG_LOGS = 20;
    private static final String TAG = "CountryDetector";
    private int mCountServiceStateChanges;
    private Country mCountry;
    private Country mCountryFromLocation;
    private final ConcurrentLinkedQueue<Country> mDebugLogs = new ConcurrentLinkedQueue();
    private Country mLastCountryAddedToLogs;
    private CountryListener mLocationBasedCountryDetectionListener = new CountryListener() {
        public void onCountryDetected(Country country) {
            ComprehensiveCountryDetector.this.mCountryFromLocation = country;
            ComprehensiveCountryDetector.this.detectCountry(true, false);
            ComprehensiveCountryDetector.this.stopLocationBasedDetector();
        }
    };
    protected CountryDetectorBase mLocationBasedCountryDetector;
    protected Timer mLocationRefreshTimer;
    private final Object mObject = new Object();
    private PhoneStateListener mPhoneStateListener;
    private long mStartTime;
    private long mStopTime;
    private boolean mStopped = false;
    private final TelephonyManager mTelephonyManager;
    private int mTotalCountServiceStateChanges;
    private long mTotalTime;

    public ComprehensiveCountryDetector(Context context) {
        super(context);
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
    }

    public Country detectCountry() {
        return detectCountry(false, this.mStopped ^ 1);
    }

    public void stop() {
        Slog.i(TAG, "Stop the detector.");
        cancelLocationRefresh();
        removePhoneStateListener();
        stopLocationBasedDetector();
        this.mListener = null;
        this.mStopped = true;
    }

    private boolean isValidCountryIso(String countryIso) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isValidCountryIso: ");
        stringBuilder.append(countryIso);
        Slog.d(str, stringBuilder.toString());
        if (!TextUtils.isEmpty(countryIso)) {
            int len = countryIso.length();
            if (2 == len) {
                for (int i = 0; i < len; i++) {
                    char ch = countryIso.charAt(i);
                    if (('a' > ch || ch > 'z') && ('A' > ch || ch > 'Z')) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private Country getCountry() {
        Country result = getNetworkBasedCountry();
        if (result == null) {
            result = getLastKnownLocationBasedCountry();
        }
        if (result == null || ("CN".equalsIgnoreCase(SystemProperties.get("ro.product.locale.region", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)) && !isValidCountryIso(result.mCountryIso))) {
            result = getSimBasedCountry();
        }
        if (result == null) {
            result = getLocaleCountry();
        }
        addToLogs(result);
        return result;
    }

    /* JADX WARNING: Missing block: B:14:0x001f, code skipped:
            if (r2.mDebugLogs.size() < 20) goto L_0x0026;
     */
    /* JADX WARNING: Missing block: B:15:0x0021, code skipped:
            r2.mDebugLogs.poll();
     */
    /* JADX WARNING: Missing block: B:16:0x0026, code skipped:
            r2.mDebugLogs.add(r3);
     */
    /* JADX WARNING: Missing block: B:17:0x002b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void addToLogs(Country country) {
        if (country != null) {
            synchronized (this.mObject) {
                if (this.mLastCountryAddedToLogs == null || !this.mLastCountryAddedToLogs.equals(country)) {
                    this.mLastCountryAddedToLogs = country;
                }
            }
        }
    }

    private boolean isNetworkCountryCodeAvailable() {
        boolean z = true;
        if ("CN".equalsIgnoreCase(SystemProperties.get("ro.product.locale.region", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS))) {
            return true;
        }
        if (this.mTelephonyManager.getPhoneType() != 1) {
            z = false;
        }
        return z;
    }

    protected Country getNetworkBasedCountry() {
        if (isNetworkCountryCodeAvailable()) {
            String countryIso = this.mTelephonyManager.getNetworkCountryIso();
            if (!TextUtils.isEmpty(countryIso)) {
                return new Country(countryIso, 0);
            }
        }
        return null;
    }

    protected Country getLastKnownLocationBasedCountry() {
        return this.mCountryFromLocation;
    }

    protected Country getSimBasedCountry() {
        String countryIso = this.mTelephonyManager.getSimCountryIso();
        if (TextUtils.isEmpty(countryIso)) {
            return null;
        }
        return new Country(countryIso, 2);
    }

    protected Country getLocaleCountry() {
        Locale defaultLocale = Locale.getDefault();
        if (defaultLocale != null) {
            return new Country(defaultLocale.getCountry(), 3);
        }
        return null;
    }

    private Country detectCountry(boolean notifyChange, boolean startLocationBasedDetection) {
        Country country = getCountry();
        runAfterDetectionAsync(this.mCountry != null ? new Country(this.mCountry) : this.mCountry, country, notifyChange, startLocationBasedDetection);
        this.mCountry = country;
        return this.mCountry;
    }

    protected void runAfterDetectionAsync(Country country, Country detectedCountry, boolean notifyChange, boolean startLocationBasedDetection) {
        final Country country2 = country;
        final Country country3 = detectedCountry;
        final boolean z = notifyChange;
        final boolean z2 = startLocationBasedDetection;
        this.mHandler.post(new Runnable() {
            public void run() {
                ComprehensiveCountryDetector.this.runAfterDetection(country2, country3, z, z2);
            }
        });
    }

    public void setCountryListener(CountryListener listener) {
        CountryListener prevListener = this.mListener;
        this.mListener = listener;
        if (this.mListener == null) {
            removePhoneStateListener();
            stopLocationBasedDetector();
            cancelLocationRefresh();
            this.mStopTime = SystemClock.elapsedRealtime();
            this.mTotalTime += this.mStopTime;
        } else if (prevListener == null) {
            addPhoneStateListener();
            detectCountry(false, true);
            this.mStartTime = SystemClock.elapsedRealtime();
            this.mStopTime = 0;
            this.mCountServiceStateChanges = 0;
        }
    }

    void runAfterDetection(Country country, Country detectedCountry, boolean notifyChange, boolean startLocationBasedDetection) {
        if (notifyChange) {
            notifyIfCountryChanged(country, detectedCountry);
        }
        if (startLocationBasedDetection && ((detectedCountry == null || detectedCountry.getSource() > 1) && isAirplaneModeOff() && this.mListener != null && isGeoCoderImplemented())) {
            startLocationBasedDetector(this.mLocationBasedCountryDetectionListener);
        }
        if (detectedCountry == null || detectedCountry.getSource() >= 1) {
            scheduleLocationRefresh();
            return;
        }
        cancelLocationRefresh();
        stopLocationBasedDetector();
    }

    private synchronized void startLocationBasedDetector(CountryListener listener) {
        if (this.mLocationBasedCountryDetector == null) {
            this.mLocationBasedCountryDetector = createLocationBasedCountryDetector();
            this.mLocationBasedCountryDetector.setCountryListener(listener);
            this.mLocationBasedCountryDetector.detectCountry();
        }
    }

    private synchronized void stopLocationBasedDetector() {
        if (this.mLocationBasedCountryDetector != null) {
            this.mLocationBasedCountryDetector.stop();
            this.mLocationBasedCountryDetector = null;
        }
    }

    protected CountryDetectorBase createLocationBasedCountryDetector() {
        return new LocationBasedCountryDetector(this.mContext);
    }

    protected boolean isAirplaneModeOff() {
        return Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 0;
    }

    private void notifyIfCountryChanged(Country country, Country detectedCountry) {
        if (detectedCountry != null && this.mListener != null) {
            if (country == null || !country.equals(detectedCountry)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                stringBuilder.append(country);
                stringBuilder.append(" --> ");
                stringBuilder.append(detectedCountry);
                Slog.d(str, stringBuilder.toString());
                notifyListener(detectedCountry);
            }
        }
    }

    private synchronized void scheduleLocationRefresh() {
        if (this.mLocationRefreshTimer == null) {
            this.mLocationRefreshTimer = new Timer();
            this.mLocationRefreshTimer.schedule(new TimerTask() {
                public void run() {
                    ComprehensiveCountryDetector.this.mLocationRefreshTimer = null;
                    ComprehensiveCountryDetector.this.detectCountry(false, true);
                }
            }, 86400000);
        }
    }

    private synchronized void cancelLocationRefresh() {
        if (this.mLocationRefreshTimer != null) {
            this.mLocationRefreshTimer.cancel();
            this.mLocationRefreshTimer = null;
        }
    }

    protected synchronized void addPhoneStateListener() {
        if (this.mPhoneStateListener == null) {
            this.mPhoneStateListener = new PhoneStateListener() {
                public void onServiceStateChanged(ServiceState serviceState) {
                    ComprehensiveCountryDetector.this.mCountServiceStateChanges = ComprehensiveCountryDetector.this.mCountServiceStateChanges + 1;
                    ComprehensiveCountryDetector.this.mTotalCountServiceStateChanges = ComprehensiveCountryDetector.this.mTotalCountServiceStateChanges + 1;
                    if (ComprehensiveCountryDetector.this.isNetworkCountryCodeAvailable()) {
                        ComprehensiveCountryDetector.this.detectCountry(true, true);
                    }
                }
            };
            this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
        }
    }

    protected synchronized void removePhoneStateListener() {
        if (this.mPhoneStateListener != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
            this.mPhoneStateListener = null;
        }
    }

    protected boolean isGeoCoderImplemented() {
        return Geocoder.isPresent();
    }

    public String toString() {
        StringBuilder stringBuilder;
        long currentTime = SystemClock.elapsedRealtime();
        long currentSessionLength = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("ComprehensiveCountryDetector{");
        if (this.mStopTime == 0) {
            currentSessionLength = currentTime - this.mStartTime;
            stringBuilder = new StringBuilder();
            stringBuilder.append("timeRunning=");
            stringBuilder.append(currentSessionLength);
            stringBuilder.append(", ");
            sb.append(stringBuilder.toString());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("lastRunTimeLength=");
            stringBuilder.append(this.mStopTime - this.mStartTime);
            stringBuilder.append(", ");
            sb.append(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("totalCountServiceStateChanges=");
        stringBuilder.append(this.mTotalCountServiceStateChanges);
        stringBuilder.append(", ");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("currentCountServiceStateChanges=");
        stringBuilder.append(this.mCountServiceStateChanges);
        stringBuilder.append(", ");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("totalTime=");
        stringBuilder.append(this.mTotalTime + currentSessionLength);
        stringBuilder.append(", ");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("currentTime=");
        stringBuilder.append(currentTime);
        stringBuilder.append(", ");
        sb.append(stringBuilder.toString());
        sb.append("countries=");
        Iterator it = this.mDebugLogs.iterator();
        while (it.hasNext()) {
            Country country = (Country) it.next();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("\n   ");
            stringBuilder2.append(country.toString());
            sb.append(stringBuilder2.toString());
        }
        sb.append("}");
        return sb.toString();
    }
}
