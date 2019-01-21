package android.location;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.SystemClock;
import android.util.Printer;
import android.util.TimeUtils;
import java.text.DecimalFormat;
import java.util.StringTokenizer;

public class Location implements Parcelable {
    public static final Creator<Location> CREATOR = new Creator<Location>() {
        public Location createFromParcel(Parcel in) {
            Location l = new Location(in.readString());
            l.mTime = in.readLong();
            l.mElapsedRealtimeNanos = in.readLong();
            l.mFieldsMask = in.readByte();
            l.mLatitude = in.readDouble();
            l.mLongitude = in.readDouble();
            l.mAltitude = in.readDouble();
            l.mSpeed = in.readFloat();
            l.mBearing = in.readFloat();
            l.mHorizontalAccuracyMeters = in.readFloat();
            l.mVerticalAccuracyMeters = in.readFloat();
            l.mSpeedAccuracyMetersPerSecond = in.readFloat();
            l.mBearingAccuracyDegrees = in.readFloat();
            l.mExtras = Bundle.setDefusable(in.readBundle(), true);
            return l;
        }

        public Location[] newArray(int size) {
            return new Location[size];
        }
    };
    public static final String EXTRA_COARSE_LOCATION = "coarseLocation";
    public static final String EXTRA_NO_GPS_LOCATION = "noGPSLocation";
    public static final int FORMAT_DEGREES = 0;
    public static final int FORMAT_MINUTES = 1;
    public static final int FORMAT_SECONDS = 2;
    private static final int HAS_ALTITUDE_MASK = 1;
    private static final int HAS_BEARING_ACCURACY_MASK = 128;
    private static final int HAS_BEARING_MASK = 4;
    private static final int HAS_HORIZONTAL_ACCURACY_MASK = 8;
    private static final int HAS_MOCK_PROVIDER_MASK = 16;
    private static final int HAS_SPEED_ACCURACY_MASK = 64;
    private static final int HAS_SPEED_MASK = 2;
    private static final int HAS_VERTICAL_ACCURACY_MASK = 32;
    private static ThreadLocal<BearingDistanceCache> sBearingDistanceCache = new ThreadLocal<BearingDistanceCache>() {
        protected BearingDistanceCache initialValue() {
            return new BearingDistanceCache();
        }
    };
    private double mAltitude = 0.0d;
    private float mBearing = 0.0f;
    private float mBearingAccuracyDegrees = 0.0f;
    private long mElapsedRealtimeNanos = 0;
    private Bundle mExtras = null;
    private byte mFieldsMask = (byte) 0;
    private float mHorizontalAccuracyMeters = 0.0f;
    private double mLatitude = 0.0d;
    private double mLongitude = 0.0d;
    private String mProvider;
    private float mSpeed = 0.0f;
    private float mSpeedAccuracyMetersPerSecond = 0.0f;
    private long mTime = 0;
    private float mVerticalAccuracyMeters = 0.0f;

    private static class BearingDistanceCache {
        private float mDistance;
        private float mFinalBearing;
        private float mInitialBearing;
        private double mLat1;
        private double mLat2;
        private double mLon1;
        private double mLon2;

        private BearingDistanceCache() {
            this.mLat1 = 0.0d;
            this.mLon1 = 0.0d;
            this.mLat2 = 0.0d;
            this.mLon2 = 0.0d;
            this.mDistance = 0.0f;
            this.mInitialBearing = 0.0f;
            this.mFinalBearing = 0.0f;
        }

        /* synthetic */ BearingDistanceCache(AnonymousClass1 x0) {
            this();
        }
    }

    public Location(String provider) {
        this.mProvider = provider;
    }

    public Location(Location l) {
        set(l);
    }

    public void set(Location l) {
        this.mProvider = l.mProvider;
        this.mTime = l.mTime;
        this.mElapsedRealtimeNanos = l.mElapsedRealtimeNanos;
        this.mFieldsMask = l.mFieldsMask;
        this.mLatitude = l.mLatitude;
        this.mLongitude = l.mLongitude;
        this.mAltitude = l.mAltitude;
        this.mSpeed = l.mSpeed;
        this.mBearing = l.mBearing;
        this.mHorizontalAccuracyMeters = l.mHorizontalAccuracyMeters;
        this.mVerticalAccuracyMeters = l.mVerticalAccuracyMeters;
        this.mSpeedAccuracyMetersPerSecond = l.mSpeedAccuracyMetersPerSecond;
        this.mBearingAccuracyDegrees = l.mBearingAccuracyDegrees;
        this.mExtras = l.mExtras == null ? null : new Bundle(l.mExtras);
    }

    public void reset() {
        this.mProvider = null;
        this.mTime = 0;
        this.mElapsedRealtimeNanos = 0;
        this.mFieldsMask = (byte) 0;
        this.mLatitude = 0.0d;
        this.mLongitude = 0.0d;
        this.mAltitude = 0.0d;
        this.mSpeed = 0.0f;
        this.mBearing = 0.0f;
        this.mHorizontalAccuracyMeters = 0.0f;
        this.mVerticalAccuracyMeters = 0.0f;
        this.mSpeedAccuracyMetersPerSecond = 0.0f;
        this.mBearingAccuracyDegrees = 0.0f;
        this.mExtras = null;
    }

    public static String convert(double coordinate, int outputType) {
        StringBuilder stringBuilder;
        if (coordinate < -180.0d || coordinate > 180.0d || Double.isNaN(coordinate)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("coordinate=");
            stringBuilder.append(coordinate);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (outputType == 0 || outputType == 1 || outputType == 2) {
            StringBuilder sb = new StringBuilder();
            if (coordinate < 0.0d) {
                sb.append('-');
                coordinate = -coordinate;
            }
            DecimalFormat df = new DecimalFormat("###.#####");
            if (outputType == 1 || outputType == 2) {
                int degrees = (int) Math.floor(coordinate);
                sb.append(degrees);
                sb.append(':');
                coordinate = (coordinate - ((double) degrees)) * 60.0d;
                if (outputType == 2) {
                    int minutes = (int) Math.floor(coordinate);
                    sb.append(minutes);
                    sb.append(':');
                    coordinate = (coordinate - ((double) minutes)) * 60.0d;
                }
            }
            sb.append(df.format(coordinate));
            return sb.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("outputType=");
            stringBuilder.append(outputType);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static double convert(String coordinate) {
        StringTokenizer stringTokenizer;
        StringBuilder stringBuilder;
        String coordinate2 = coordinate;
        if (coordinate2 != null) {
            String coordinate3 = null;
            if (coordinate2.charAt(0) == '-') {
                coordinate2 = coordinate2.substring(1);
                coordinate3 = true;
            }
            boolean negative = coordinate3;
            coordinate3 = coordinate2;
            StringTokenizer st = new StringTokenizer(coordinate3, ":");
            int tokens = st.countTokens();
            if (tokens >= 1) {
                try {
                    coordinate2 = st.nextToken();
                    if (tokens == 1) {
                        try {
                            double val = Double.parseDouble(coordinate2);
                            return negative ? -val : val;
                        } catch (NumberFormatException e) {
                            stringTokenizer = st;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("coordinate=");
                            stringBuilder.append(coordinate3);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                    }
                    double min;
                    String minutes = st.nextToken();
                    int deg = Integer.parseInt(coordinate2);
                    double sec = 0.0d;
                    boolean secPresent = false;
                    if (st.hasMoreTokens()) {
                        min = (double) Integer.parseInt(minutes);
                        sec = Double.parseDouble(st.nextToken());
                        secPresent = true;
                    } else {
                        min = Double.parseDouble(minutes);
                    }
                    boolean z = negative && deg == 180 && min == 0.0d && sec == 0.0d;
                    boolean isNegative180 = z;
                    StringBuilder stringBuilder2;
                    if (((double) deg) < null || (deg > 179 && !isNegative180)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("coordinate=");
                        stringBuilder2.append(coordinate3);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    } else if (min < 0.0d || min >= 60.0d || (secPresent && min > 59.0d)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("coordinate=");
                        stringBuilder2.append(coordinate3);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    } else if (sec < 0.0d || sec >= 60.0d) {
                        try {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("coordinate=");
                            stringBuilder2.append(coordinate3);
                            throw new IllegalArgumentException(stringBuilder2.toString());
                        } catch (NumberFormatException e2) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("coordinate=");
                            stringBuilder.append(coordinate3);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                    } else {
                        double val2 = (((((double) deg) * 3600.0d) + (60.0d * min)) + sec) / 3600.0d;
                        return negative ? -val2 : val2;
                    }
                } catch (NumberFormatException e3) {
                    stringTokenizer = st;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("coordinate=");
                    stringBuilder.append(coordinate3);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("coordinate=");
            stringBuilder3.append(coordinate3);
            throw new IllegalArgumentException(stringBuilder3.toString());
        }
        throw new NullPointerException("coordinate");
    }

    private static void computeDistanceAndBearing(double lat1, double lon1, double lat2, double lon2, BearingDistanceCache results) {
        double lat22;
        BearingDistanceCache bearingDistanceCache = results;
        double lat12 = lat1 * 0.017453292519943295d;
        double lat23 = lat2 * 0.017453292519943295d;
        double lon12 = lon1 * 0.017453292519943295d;
        double lon22 = 0.017453292519943295d * lon2;
        double f = (6378137.0d - 6356752.3142d) / 6378137.0d;
        double aSqMinusBSqOverBSq = ((6378137.0d * 6378137.0d) - (6356752.3142d * 6356752.3142d)) / (6356752.3142d * 6356752.3142d);
        double L = lon22 - lon12;
        double A = 0.0d;
        double a = 6378137.0d;
        double U1 = Math.atan((1.0d - f) * Math.tan(lat12));
        double lon23 = lon22;
        lon22 = Math.atan((1.0d - f) * Math.tan(lat23));
        double cosU1 = Math.cos(U1);
        double cosU2 = Math.cos(lon22);
        double lon13 = lon12;
        lon12 = Math.sin(U1);
        double sinU2 = Math.sin(lon22);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = lon12 * sinU2;
        double sigma = 0.0d;
        double deltaSigma = 0.0d;
        double cosSigma = 0.0d;
        double sinSigma = 0.0d;
        double cosLambda = 0.0d;
        double sinLambda = 0.0d;
        int iter = 0;
        double lambda = L;
        while (true) {
            double U2 = lon22;
            int iter2 = iter;
            double d;
            double d2;
            double d3;
            if (iter2 >= 20) {
                lat22 = lat23;
                d = U1;
                d2 = lambda;
                d3 = sinSigma;
                break;
            }
            double lambdaOrig = lambda;
            d = U1;
            U1 = lambda;
            cosLambda = Math.cos(U1);
            sinLambda = Math.sin(U1);
            double t1 = cosU2 * sinLambda;
            double t2 = (cosU1 * sinU2) - ((lon12 * cosU2) * cosLambda);
            U1 = (t1 * t1) + (t2 * t2);
            lat22 = lat23;
            lat23 = Math.sqrt(U1);
            U1 = sinU1sinU2 + (cosU1cosU2 * cosLambda);
            sigma = Math.atan2(lat23, U1);
            cosSigma = 0.0d;
            sinSigma = lat23 == 0.0d ? 0.0d : (cosU1cosU2 * sinLambda) / lat23;
            double cosSqAlpha = 1.0d - (sinSigma * sinSigma);
            if (cosSqAlpha != 0.0d) {
                cosSigma = U1 - ((2.0d * sinU1sinU2) / cosSqAlpha);
            }
            double cos2SM = cosSigma;
            cosSigma = cosSqAlpha * aSqMinusBSqOverBSq;
            A = 1.0d + ((cosSigma / 16384.0d) * (4096.0d + ((-768.0d + ((320.0d - (175.0d * cosSigma)) * cosSigma)) * cosSigma)));
            double B = (cosSigma / 1024.0d) * (256.0d + ((-128.0d + ((74.0d - (47.0d * cosSigma)) * cosSigma)) * cosSigma));
            double C = ((f / 16.0d) * cosSqAlpha) * (4.0d + ((4.0d - (3.0d * cosSqAlpha)) * f));
            double cos2SMSq = cos2SM * cos2SM;
            deltaSigma = (B * lat23) * (cos2SM + ((B / 4.0d) * (((-1.0d + (2.0d * cos2SMSq)) * U1) - ((((B / 6.0d) * cos2SM) * (-3.0d + ((4.0d * lat23) * lat23))) * (-3.0d + (4.0d * cos2SMSq))))));
            double lambda2 = L + ((((1.0d - C) * f) * sinSigma) * (sigma + ((C * lat23) * (cos2SM + ((C * U1) * (-1.0d + ((2.0d * cos2SM) * cos2SM)))))));
            d3 = lat23;
            if (Math.abs((lambda2 - lambdaOrig) / lambda2) < 1.0E-12d) {
                d2 = lambda2;
                break;
            }
            iter = iter2 + 1;
            cosSigma = U1;
            lon22 = U2;
            U1 = d;
            lambda = lambda2;
            lat23 = lat22;
            sinSigma = d3;
        }
        float distance = (float) ((6356752.3142d * A) * (sigma - deltaSigma));
        bearingDistanceCache.mDistance = distance;
        int MAXITERS = 20;
        float initialBearing = (float) (((double) ((float) Math.atan2(cosU2 * sinLambda, (cosU1 * sinU2) - ((lon12 * cosU2) * cosLambda)))) * 57.29577951308232d);
        bearingDistanceCache.mInitialBearing = initialBearing;
        distance = (float) (((double) ((float) Math.atan2(cosU1 * sinLambda, ((-lon12) * cosU2) + ((cosU1 * sinU2) * cosLambda)))) * 57.29577951308232d);
        bearingDistanceCache.mFinalBearing = distance;
        bearingDistanceCache.mLat1 = lat12;
        bearingDistanceCache.mLat2 = lat22;
        double lon14 = lon13;
        bearingDistanceCache.mLon1 = lon14;
        bearingDistanceCache.mLon2 = lon23;
    }

    public static void distanceBetween(double startLatitude, double startLongitude, double endLatitude, double endLongitude, float[] results) {
        float[] fArr = results;
        if (fArr == null || fArr.length < 1) {
            throw new IllegalArgumentException("results is null or has length < 1");
        }
        BearingDistanceCache cache = (BearingDistanceCache) sBearingDistanceCache.get();
        computeDistanceAndBearing(startLatitude, startLongitude, endLatitude, endLongitude, cache);
        fArr[0] = cache.mDistance;
        if (fArr.length > 1) {
            fArr[1] = cache.mInitialBearing;
            if (fArr.length > 2) {
                fArr[2] = cache.mFinalBearing;
            }
        }
    }

    public float distanceTo(Location dest) {
        BearingDistanceCache cache = (BearingDistanceCache) sBearingDistanceCache.get();
        if (!(this.mLatitude == cache.mLat1 && this.mLongitude == cache.mLon1 && dest.mLatitude == cache.mLat2 && dest.mLongitude == cache.mLon2)) {
            computeDistanceAndBearing(this.mLatitude, this.mLongitude, dest.mLatitude, dest.mLongitude, cache);
        }
        return cache.mDistance;
    }

    public float bearingTo(Location dest) {
        BearingDistanceCache cache = (BearingDistanceCache) sBearingDistanceCache.get();
        if (!(this.mLatitude == cache.mLat1 && this.mLongitude == cache.mLon1 && dest.mLatitude == cache.mLat2 && dest.mLongitude == cache.mLon2)) {
            computeDistanceAndBearing(this.mLatitude, this.mLongitude, dest.mLatitude, dest.mLongitude, cache);
        }
        return cache.mInitialBearing;
    }

    public String getProvider() {
        return this.mProvider;
    }

    public void setProvider(String provider) {
        this.mProvider = provider;
    }

    public long getTime() {
        return this.mTime;
    }

    public void setTime(long time) {
        this.mTime = time;
    }

    public long getElapsedRealtimeNanos() {
        return this.mElapsedRealtimeNanos;
    }

    public void setElapsedRealtimeNanos(long time) {
        this.mElapsedRealtimeNanos = time;
    }

    public double getLatitude() {
        return this.mLatitude;
    }

    public void setLatitude(double latitude) {
        this.mLatitude = latitude;
    }

    public double getLongitude() {
        return this.mLongitude;
    }

    public void setLongitude(double longitude) {
        this.mLongitude = longitude;
    }

    public boolean hasAltitude() {
        return (this.mFieldsMask & 1) != 0;
    }

    public double getAltitude() {
        return this.mAltitude;
    }

    public void setAltitude(double altitude) {
        this.mAltitude = altitude;
        this.mFieldsMask = (byte) (this.mFieldsMask | 1);
    }

    @Deprecated
    public void removeAltitude() {
        this.mAltitude = 0.0d;
        this.mFieldsMask = (byte) (this.mFieldsMask & -2);
    }

    public boolean hasSpeed() {
        return (this.mFieldsMask & 2) != 0;
    }

    public float getSpeed() {
        return this.mSpeed;
    }

    public void setSpeed(float speed) {
        this.mSpeed = speed;
        this.mFieldsMask = (byte) (this.mFieldsMask | 2);
    }

    @Deprecated
    public void removeSpeed() {
        this.mSpeed = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & -3);
    }

    public boolean hasBearing() {
        return (this.mFieldsMask & 4) != 0;
    }

    public float getBearing() {
        return this.mBearing;
    }

    public void setBearing(float bearing) {
        while (bearing < 0.0f) {
            bearing += 360.0f;
        }
        while (bearing >= 360.0f) {
            bearing -= 360.0f;
        }
        this.mBearing = bearing;
        this.mFieldsMask = (byte) (this.mFieldsMask | 4);
    }

    @Deprecated
    public void removeBearing() {
        this.mBearing = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & -5);
    }

    public boolean hasAccuracy() {
        return (this.mFieldsMask & 8) != 0;
    }

    public float getAccuracy() {
        return this.mHorizontalAccuracyMeters;
    }

    public void setAccuracy(float horizontalAccuracy) {
        this.mHorizontalAccuracyMeters = horizontalAccuracy;
        this.mFieldsMask = (byte) (this.mFieldsMask | 8);
    }

    @Deprecated
    public void removeAccuracy() {
        this.mHorizontalAccuracyMeters = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & -9);
    }

    public boolean hasVerticalAccuracy() {
        return (this.mFieldsMask & 32) != 0;
    }

    public float getVerticalAccuracyMeters() {
        return this.mVerticalAccuracyMeters;
    }

    public void setVerticalAccuracyMeters(float verticalAccuracyMeters) {
        this.mVerticalAccuracyMeters = verticalAccuracyMeters;
        this.mFieldsMask = (byte) (this.mFieldsMask | 32);
    }

    @Deprecated
    public void removeVerticalAccuracy() {
        this.mVerticalAccuracyMeters = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & -33);
    }

    public boolean hasSpeedAccuracy() {
        return (this.mFieldsMask & 64) != 0;
    }

    public float getSpeedAccuracyMetersPerSecond() {
        return this.mSpeedAccuracyMetersPerSecond;
    }

    public void setSpeedAccuracyMetersPerSecond(float speedAccuracyMeterPerSecond) {
        this.mSpeedAccuracyMetersPerSecond = speedAccuracyMeterPerSecond;
        this.mFieldsMask = (byte) (this.mFieldsMask | 64);
    }

    @Deprecated
    public void removeSpeedAccuracy() {
        this.mSpeedAccuracyMetersPerSecond = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & -65);
    }

    public boolean hasBearingAccuracy() {
        return (this.mFieldsMask & 128) != 0;
    }

    public float getBearingAccuracyDegrees() {
        return this.mBearingAccuracyDegrees;
    }

    public void setBearingAccuracyDegrees(float bearingAccuracyDegrees) {
        this.mBearingAccuracyDegrees = bearingAccuracyDegrees;
        this.mFieldsMask = (byte) (this.mFieldsMask | 128);
    }

    @Deprecated
    public void removeBearingAccuracy() {
        this.mBearingAccuracyDegrees = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & -129);
    }

    @SystemApi
    public boolean isComplete() {
        if (this.mProvider == null || !hasAccuracy() || this.mTime == 0 || this.mElapsedRealtimeNanos == 0) {
            return false;
        }
        return true;
    }

    @SystemApi
    public void makeComplete() {
        if (this.mProvider == null) {
            this.mProvider = "?";
        }
        if (!hasAccuracy()) {
            this.mFieldsMask = (byte) (this.mFieldsMask | 8);
            this.mHorizontalAccuracyMeters = 100.0f;
        }
        if (this.mTime == 0) {
            this.mTime = System.currentTimeMillis();
        }
        if (this.mElapsedRealtimeNanos == 0) {
            this.mElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        }
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public void setExtras(Bundle extras) {
        this.mExtras = extras == null ? null : new Bundle(extras);
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("Location[");
        s.append(this.mProvider);
        s.append(String.format(" %.0f******,%.0f******", new Object[]{Double.valueOf(this.mLatitude), Double.valueOf(this.mLongitude)}));
        if (hasAccuracy()) {
            s.append(String.format(" hAcc=%.0f", new Object[]{Float.valueOf(this.mHorizontalAccuracyMeters)}));
        } else {
            s.append(" hAcc=???");
        }
        if (this.mTime == 0) {
            s.append(" t=?!?");
        }
        if (this.mElapsedRealtimeNanos == 0) {
            s.append(" et=?!?");
        } else {
            s.append(" et=");
            TimeUtils.formatDuration(this.mElapsedRealtimeNanos / 1000000, s);
        }
        if (hasAltitude()) {
            s.append(" alt=");
            s.append(this.mAltitude);
        }
        if (hasSpeed()) {
            s.append(" vel=");
            s.append(this.mSpeed);
        }
        if (hasBearing()) {
            s.append(" bear=");
            s.append(this.mBearing);
        }
        if (hasVerticalAccuracy()) {
            s.append(String.format(" vAcc=%.0f", new Object[]{Float.valueOf(this.mVerticalAccuracyMeters)}));
        } else {
            s.append(" vAcc=???");
        }
        if (hasSpeedAccuracy()) {
            s.append(String.format(" sAcc=%.0f", new Object[]{Float.valueOf(this.mSpeedAccuracyMetersPerSecond)}));
        } else {
            s.append(" sAcc=???");
        }
        if (hasBearingAccuracy()) {
            s.append(String.format(" bAcc=%.0f", new Object[]{Float.valueOf(this.mBearingAccuracyDegrees)}));
        } else {
            s.append(" bAcc=???");
        }
        if (isFromMockProvider()) {
            s.append(" mock");
        }
        s.append(']');
        return s.toString();
    }

    public void dump(Printer pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(toString());
        pw.println(stringBuilder.toString());
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.mProvider);
        parcel.writeLong(this.mTime);
        parcel.writeLong(this.mElapsedRealtimeNanos);
        parcel.writeByte(this.mFieldsMask);
        parcel.writeDouble(this.mLatitude);
        parcel.writeDouble(this.mLongitude);
        parcel.writeDouble(this.mAltitude);
        parcel.writeFloat(this.mSpeed);
        parcel.writeFloat(this.mBearing);
        parcel.writeFloat(this.mHorizontalAccuracyMeters);
        parcel.writeFloat(this.mVerticalAccuracyMeters);
        parcel.writeFloat(this.mSpeedAccuracyMetersPerSecond);
        parcel.writeFloat(this.mBearingAccuracyDegrees);
        parcel.writeBundle(this.mExtras);
    }

    public Location getExtraLocation(String key) {
        if (this.mExtras != null) {
            Parcelable value = this.mExtras.getParcelable(key);
            if (value instanceof Location) {
                return (Location) value;
            }
        }
        return null;
    }

    public void setExtraLocation(String key, Location value) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putParcelable(key, value);
    }

    public boolean isFromMockProvider() {
        return (this.mFieldsMask & 16) != 0;
    }

    @SystemApi
    public void setIsFromMockProvider(boolean isFromMockProvider) {
        if (isFromMockProvider) {
            this.mFieldsMask = (byte) (this.mFieldsMask | 16);
        } else {
            this.mFieldsMask = (byte) (this.mFieldsMask & -17);
        }
    }
}
