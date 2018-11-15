package com.android.server.policy;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import com.android.server.HwServiceFactory;
import com.android.server.policy.IHWExtMotionRotationProcessor.WindowOrientationListenerProxy;
import java.io.PrintWriter;

public abstract class WindowOrientationListener {
    private static final int DEFAULT_BATCH_LATENCY = 100000;
    private static final boolean LOG = SystemProperties.getBoolean("debug.orientation.log", false);
    private static boolean McuSwitch = "true".equals(SystemProperties.get("ro.config.hw_sensorhub", "false"));
    private static final String TAG = "WindowOrientationListener";
    private static final boolean USE_GRAVITY_SENSOR = false;
    private int mCurrentRotation;
    private boolean mEnabled;
    private IHWExtMotionRotationProcessor mHWEMRProcessor;
    private Handler mHandler;
    private final Object mLock;
    private OrientationJudge mOrientationJudge;
    private int mRate;
    private Sensor mSensor;
    private SensorManager mSensorManager;
    private String mSensorType;
    private WindowOrientationListenerProxy mWOLProxy;

    abstract class OrientationJudge implements SensorEventListener {
        protected static final float MILLIS_PER_NANO = 1.0E-6f;
        protected static final long NANOS_PER_MS = 1000000;
        protected static final long PROPOSAL_MIN_TIME_SINCE_TOUCH_END_NANOS = 500000000;

        public abstract void dumpLocked(PrintWriter printWriter, String str);

        public abstract int getProposedRotationLocked();

        public abstract void onAccuracyChanged(Sensor sensor, int i);

        public abstract void onSensorChanged(SensorEvent sensorEvent);

        public abstract void onTouchEndLocked(long j);

        public abstract void onTouchStartLocked();

        public abstract void resetLocked(boolean z);

        OrientationJudge() {
        }
    }

    final class AccelSensorJudge extends OrientationJudge {
        private static final float ACCELERATION_TOLERANCE = 4.0f;
        private static final int ACCELEROMETER_DATA_X = 0;
        private static final int ACCELEROMETER_DATA_Y = 1;
        private static final int ACCELEROMETER_DATA_Z = 2;
        private static final int ADJACENT_ORIENTATION_ANGLE_GAP = 45;
        private static final float FILTER_TIME_CONSTANT_MS = 200.0f;
        private static final float FLAT_ANGLE = 80.0f;
        private static final long FLAT_TIME_NANOS = 1000000000;
        private static final float MAX_ACCELERATION_MAGNITUDE = 13.80665f;
        private static final long MAX_FILTER_DELTA_TIME_NANOS = 1000000000;
        private static final int MAX_TILT = 80;
        private static final float MIN_ACCELERATION_MAGNITUDE = 5.80665f;
        private static final float NEAR_ZERO_MAGNITUDE = 1.0f;
        private static final long PROPOSAL_MIN_TIME_SINCE_ACCELERATION_ENDED_NANOS = 500000000;
        private static final long PROPOSAL_MIN_TIME_SINCE_FLAT_ENDED_NANOS = 500000000;
        private static final long PROPOSAL_MIN_TIME_SINCE_SWING_ENDED_NANOS = 300000000;
        private static final long PROPOSAL_SETTLE_TIME_NANOS = 40000000;
        private static final float RADIANS_TO_DEGREES = 57.29578f;
        private static final float SWING_AWAY_ANGLE_DELTA = 20.0f;
        private static final long SWING_TIME_NANOS = 300000000;
        private static final int TILT_HISTORY_SIZE = 200;
        private static final int TILT_OVERHEAD_ENTER = -40;
        private static final int TILT_OVERHEAD_EXIT = -15;
        private boolean mAccelerating;
        private long mAccelerationTimestampNanos;
        private boolean mFlat;
        private long mFlatTimestampNanos;
        private long mLastFilteredTimestampNanos;
        private float mLastFilteredX;
        private float mLastFilteredY;
        private float mLastFilteredZ;
        private boolean mOverhead;
        private int mPredictedRotation;
        private long mPredictedRotationTimestampNanos;
        private int mProposedRotation;
        private long mSwingTimestampNanos;
        private boolean mSwinging;
        private float[] mTiltHistory = new float[200];
        private int mTiltHistoryIndex;
        private long[] mTiltHistoryTimestampNanos = new long[200];
        private final int[][] mTiltToleranceConfig = new int[][]{new int[]{-25, 70}, new int[]{-25, 65}, new int[]{-25, 60}, new int[]{-25, 65}};
        private long mTouchEndedTimestampNanos = Long.MIN_VALUE;
        private boolean mTouched;

        public AccelSensorJudge(Context context) {
            super();
            int[] tiltTolerance = context.getResources().getIntArray(17235986);
            if (tiltTolerance.length == 8) {
                for (int i = 0; i < 4; i++) {
                    int min = tiltTolerance[i * 2];
                    int max = tiltTolerance[(i * 2) + 1];
                    if (min < -90 || min > max || max > 90) {
                        String str = WindowOrientationListener.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("config_autoRotationTiltTolerance contains invalid range: min=");
                        stringBuilder.append(min);
                        stringBuilder.append(", max=");
                        stringBuilder.append(max);
                        Slog.wtf(str, stringBuilder.toString());
                    } else {
                        this.mTiltToleranceConfig[i][0] = min;
                        this.mTiltToleranceConfig[i][1] = max;
                    }
                }
                return;
            }
            Slog.wtf(WindowOrientationListener.TAG, "config_autoRotationTiltTolerance should have exactly 8 elements");
        }

        public int getProposedRotationLocked() {
            return this.mProposedRotation;
        }

        public void dumpLocked(PrintWriter pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("AccelSensorJudge");
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            prefix = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mProposedRotation=");
            stringBuilder.append(this.mProposedRotation);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mPredictedRotation=");
            stringBuilder.append(this.mPredictedRotation);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mLastFilteredX=");
            stringBuilder.append(this.mLastFilteredX);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mLastFilteredY=");
            stringBuilder.append(this.mLastFilteredY);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mLastFilteredZ=");
            stringBuilder.append(this.mLastFilteredZ);
            pw.println(stringBuilder.toString());
            long delta = SystemClock.elapsedRealtimeNanos() - this.mLastFilteredTimestampNanos;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("mLastFilteredTimestampNanos=");
            stringBuilder2.append(this.mLastFilteredTimestampNanos);
            stringBuilder2.append(" (");
            stringBuilder2.append(((float) delta) * 1.0E-6f);
            stringBuilder2.append("ms ago)");
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("mTiltHistory={last: ");
            stringBuilder2.append(getLastTiltLocked());
            stringBuilder2.append("}");
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("mFlat=");
            stringBuilder2.append(this.mFlat);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("mSwinging=");
            stringBuilder2.append(this.mSwinging);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("mAccelerating=");
            stringBuilder2.append(this.mAccelerating);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("mOverhead=");
            stringBuilder2.append(this.mOverhead);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("mTouched=");
            stringBuilder2.append(this.mTouched);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("mTiltToleranceConfig=[");
            pw.print(stringBuilder2.toString());
            for (int i = 0; i < 4; i++) {
                if (i != 0) {
                    pw.print(", ");
                }
                pw.print("[");
                pw.print(this.mTiltToleranceConfig[i][0]);
                pw.print(", ");
                pw.print(this.mTiltToleranceConfig[i][1]);
                pw.print("]");
            }
            pw.println("]");
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            int oldProposedRotation;
            int proposedRotation;
            SensorEvent sensorEvent = event;
            synchronized (WindowOrientationListener.this.mLock) {
                boolean skipSample;
                StringBuilder stringBuilder;
                String str;
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                if (WindowOrientationListener.LOG) {
                    String str2 = WindowOrientationListener.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Raw acceleration vector: x=");
                    stringBuilder2.append(x);
                    stringBuilder2.append(", y=");
                    stringBuilder2.append(y);
                    stringBuilder2.append(", z=");
                    stringBuilder2.append(z);
                    stringBuilder2.append(", magnitude=");
                    stringBuilder2.append(Math.sqrt((double) (((x * x) + (y * y)) + (z * z))));
                    Slog.v(str2, stringBuilder2.toString());
                }
                long now = sensorEvent.timestamp;
                long then = this.mLastFilteredTimestampNanos;
                float timeDeltaMS = ((float) (now - then)) * 1.0E-6f;
                if (now < then || now > 1000000000 + then || (x == 0.0f && y == 0.0f && z == 0.0f)) {
                    if (WindowOrientationListener.LOG) {
                        Slog.v(WindowOrientationListener.TAG, "Resetting orientation listener.");
                    }
                    resetLocked(true);
                    skipSample = true;
                } else {
                    float z2;
                    float alpha = timeDeltaMS / (FILTER_TIME_CONSTANT_MS + timeDeltaMS);
                    x = ((x - this.mLastFilteredX) * alpha) + this.mLastFilteredX;
                    y = ((y - this.mLastFilteredY) * alpha) + this.mLastFilteredY;
                    z = ((z - this.mLastFilteredZ) * alpha) + this.mLastFilteredZ;
                    if (WindowOrientationListener.LOG) {
                        String str3 = WindowOrientationListener.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Filtered acceleration vector: x=");
                        stringBuilder.append(x);
                        stringBuilder.append(", y=");
                        stringBuilder.append(y);
                        stringBuilder.append(", z=");
                        stringBuilder.append(z);
                        stringBuilder.append(", magnitude=");
                        z2 = z;
                        stringBuilder.append(Math.sqrt((double) (((x * x) + (y * y)) + (z * z))));
                        Slog.v(str3, stringBuilder.toString());
                    } else {
                        z2 = z;
                    }
                    z = z2;
                    skipSample = false;
                }
                this.mLastFilteredTimestampNanos = now;
                this.mLastFilteredX = x;
                this.mLastFilteredY = y;
                this.mLastFilteredZ = z;
                boolean isAccelerating = false;
                boolean isFlat = false;
                boolean isSwinging = false;
                if (skipSample) {
                    long j = then;
                } else {
                    float magnitude = (float) Math.sqrt((double) (((x * x) + (y * y)) + (z * z)));
                    if (magnitude < 1.0f) {
                        if (WindowOrientationListener.LOG) {
                            Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, magnitude too close to zero.");
                        }
                        clearPredictedRotationLocked();
                        float f = z;
                    } else {
                        boolean isFlat2;
                        boolean isSwinging2;
                        if (isAcceleratingLocked(magnitude)) {
                            isAccelerating = true;
                            this.mAccelerationTimestampNanos = now;
                        }
                        boolean isAccelerating2 = isAccelerating;
                        int tiltAngle = (int) Math.round(Math.asin((double) (z / magnitude)) * 57.295780181884766d);
                        addTiltHistoryEntryLocked(now, (float) tiltAngle);
                        if (isFlatLocked(now) != null) {
                            isFlat = true;
                            this.mFlatTimestampNanos = now;
                        }
                        if (isSwingingLocked(now, (float) tiltAngle) != null) {
                            isSwinging = true;
                            this.mSwingTimestampNanos = now;
                        }
                        if (tiltAngle <= TILT_OVERHEAD_ENTER) {
                            this.mOverhead = true;
                        } else if (tiltAngle >= TILT_OVERHEAD_EXIT) {
                            this.mOverhead = false;
                        }
                        StringBuilder stringBuilder3;
                        if (this.mOverhead != null) {
                            if (WindowOrientationListener.LOG != null) {
                                z = WindowOrientationListener.TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Ignoring sensor data, device is overhead: tiltAngle=");
                                stringBuilder3.append(tiltAngle);
                                Slog.v(z, stringBuilder3.toString());
                            }
                            clearPredictedRotationLocked();
                        } else if (Math.abs(tiltAngle) > 80) {
                            if (WindowOrientationListener.LOG != null) {
                                z = WindowOrientationListener.TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Ignoring sensor data, tilt angle too high: tiltAngle=");
                                stringBuilder3.append(tiltAngle);
                                Slog.v(z, stringBuilder3.toString());
                            }
                            clearPredictedRotationLocked();
                        } else {
                            isFlat2 = isFlat;
                            isSwinging2 = isSwinging;
                            z = (int) Math.round((-Math.atan2((double) (-x), (double) y)) * 57.295780181884766d);
                            if (z < null) {
                                z += 360;
                            }
                            then = (z + 45) / 90;
                            if (then == 4) {
                                then = null;
                            }
                            StringBuilder stringBuilder4;
                            if (isTiltAngleAcceptableLocked(then, tiltAngle) && isOrientationAngleAcceptableLocked(then, z)) {
                                updatePredictedRotationLocked(now, then);
                                if (WindowOrientationListener.LOG) {
                                    str = WindowOrientationListener.TAG;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Predicted: tiltAngle=");
                                    stringBuilder4.append(tiltAngle);
                                    stringBuilder4.append(", orientationAngle=");
                                    stringBuilder4.append(z);
                                    stringBuilder4.append(", predictedRotation=");
                                    stringBuilder4.append(this.mPredictedRotation);
                                    stringBuilder4.append(", predictedRotationAgeMS=");
                                    stringBuilder4.append(((float) (now - this.mPredictedRotationTimestampNanos)) * 1.0E-6f);
                                    Slog.v(str, stringBuilder4.toString());
                                }
                                isAccelerating = isAccelerating2;
                                isFlat = isFlat2;
                                isSwinging = isSwinging2;
                            } else {
                                if (WindowOrientationListener.LOG) {
                                    str = WindowOrientationListener.TAG;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Ignoring sensor data, no predicted rotation: tiltAngle=");
                                    stringBuilder4.append(tiltAngle);
                                    stringBuilder4.append(", orientationAngle=");
                                    stringBuilder4.append(z);
                                    Slog.v(str, stringBuilder4.toString());
                                }
                                clearPredictedRotationLocked();
                                isAccelerating = isAccelerating2;
                                isFlat = isFlat2;
                                isSwinging = isSwinging2;
                            }
                        }
                        isFlat2 = isFlat;
                        isSwinging2 = isSwinging;
                        isAccelerating = isAccelerating2;
                        isFlat = isFlat2;
                        isSwinging = isSwinging2;
                    }
                }
                this.mFlat = isFlat;
                this.mSwinging = isSwinging;
                this.mAccelerating = isAccelerating;
                oldProposedRotation = this.mProposedRotation;
                if (this.mPredictedRotation < 0 || isPredictedRotationAcceptableLocked(now)) {
                    this.mProposedRotation = this.mPredictedRotation;
                }
                proposedRotation = this.mProposedRotation;
                if (WindowOrientationListener.LOG) {
                    str = WindowOrientationListener.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Result: currentRotation=");
                    stringBuilder.append(WindowOrientationListener.this.mCurrentRotation);
                    stringBuilder.append(", proposedRotation=");
                    stringBuilder.append(proposedRotation);
                    stringBuilder.append(", predictedRotation=");
                    stringBuilder.append(this.mPredictedRotation);
                    stringBuilder.append(", timeDeltaMS=");
                    stringBuilder.append(timeDeltaMS);
                    stringBuilder.append(", isAccelerating=");
                    stringBuilder.append(isAccelerating);
                    stringBuilder.append(", isFlat=");
                    stringBuilder.append(isFlat);
                    stringBuilder.append(", isSwinging=");
                    stringBuilder.append(isSwinging);
                    stringBuilder.append(", isOverhead=");
                    stringBuilder.append(this.mOverhead);
                    stringBuilder.append(", isTouched=");
                    stringBuilder.append(this.mTouched);
                    stringBuilder.append(", timeUntilSettledMS=");
                    stringBuilder.append(remainingMS(now, this.mPredictedRotationTimestampNanos + PROPOSAL_SETTLE_TIME_NANOS));
                    stringBuilder.append(", timeUntilAccelerationDelayExpiredMS=");
                    stringBuilder.append(remainingMS(now, this.mAccelerationTimestampNanos + 500000000));
                    stringBuilder.append(", timeUntilFlatDelayExpiredMS=");
                    stringBuilder.append(remainingMS(now, this.mFlatTimestampNanos + 500000000));
                    stringBuilder.append(", timeUntilSwingDelayExpiredMS=");
                    stringBuilder.append(remainingMS(now, this.mSwingTimestampNanos + 300000000));
                    stringBuilder.append(", timeUntilTouchDelayExpiredMS=");
                    stringBuilder.append(remainingMS(now, this.mTouchEndedTimestampNanos + 500000000));
                    Slog.v(str, stringBuilder.toString());
                }
            }
            int proposedRotation2 = proposedRotation;
            int oldProposedRotation2 = oldProposedRotation;
            if (proposedRotation2 != oldProposedRotation2 && proposedRotation2 >= 0) {
                if (WindowOrientationListener.LOG) {
                    String str4 = WindowOrientationListener.TAG;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("Proposed rotation changed!  proposedRotation=");
                    stringBuilder5.append(proposedRotation2);
                    stringBuilder5.append(", oldProposedRotation=");
                    stringBuilder5.append(oldProposedRotation2);
                    Slog.v(str4, stringBuilder5.toString());
                }
                WindowOrientationListener.this.onProposedRotationChanged(proposedRotation2);
            }
        }

        public void onTouchStartLocked() {
            this.mTouched = true;
        }

        public void onTouchEndLocked(long whenElapsedNanos) {
            this.mTouched = false;
            this.mTouchEndedTimestampNanos = whenElapsedNanos;
        }

        public void resetLocked(boolean clearCurrentRotation) {
            this.mLastFilteredTimestampNanos = Long.MIN_VALUE;
            if (clearCurrentRotation) {
                this.mProposedRotation = -1;
            }
            this.mFlatTimestampNanos = Long.MIN_VALUE;
            this.mFlat = false;
            this.mSwingTimestampNanos = Long.MIN_VALUE;
            this.mSwinging = false;
            this.mAccelerationTimestampNanos = Long.MIN_VALUE;
            this.mAccelerating = false;
            this.mOverhead = false;
            clearPredictedRotationLocked();
            clearTiltHistoryLocked();
        }

        private boolean isTiltAngleAcceptableLocked(int rotation, int tiltAngle) {
            return tiltAngle >= this.mTiltToleranceConfig[rotation][0] && tiltAngle <= this.mTiltToleranceConfig[rotation][1];
        }

        private boolean isOrientationAngleAcceptableLocked(int rotation, int orientationAngle) {
            int currentRotation = WindowOrientationListener.this.mCurrentRotation;
            if (currentRotation >= 0) {
                int lowerBound;
                if (rotation == currentRotation || rotation == (currentRotation + 1) % 4) {
                    lowerBound = ((rotation * 90) - 45) + 22;
                    if (rotation == 0) {
                        if (orientationAngle >= 315 && orientationAngle < lowerBound + 360) {
                            return false;
                        }
                    } else if (orientationAngle < lowerBound) {
                        return false;
                    }
                }
                if (rotation == currentRotation || rotation == (currentRotation + 3) % 4) {
                    lowerBound = ((rotation * 90) + 45) - 22;
                    if (rotation == 0) {
                        if (orientationAngle <= 45 && orientationAngle > lowerBound) {
                            return false;
                        }
                    } else if (orientationAngle > lowerBound) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isPredictedRotationAcceptableLocked(long now) {
            if (now >= this.mPredictedRotationTimestampNanos + PROPOSAL_SETTLE_TIME_NANOS && now >= this.mFlatTimestampNanos + 500000000 && now >= this.mSwingTimestampNanos + 300000000 && now >= this.mAccelerationTimestampNanos + 500000000 && !this.mTouched && now >= this.mTouchEndedTimestampNanos + 500000000) {
                return true;
            }
            return false;
        }

        private void clearPredictedRotationLocked() {
            this.mPredictedRotation = -1;
            this.mPredictedRotationTimestampNanos = Long.MIN_VALUE;
        }

        private void updatePredictedRotationLocked(long now, int rotation) {
            if (this.mPredictedRotation != rotation) {
                this.mPredictedRotation = rotation;
                this.mPredictedRotationTimestampNanos = now;
            }
        }

        private boolean isAcceleratingLocked(float magnitude) {
            return magnitude < MIN_ACCELERATION_MAGNITUDE || magnitude > MAX_ACCELERATION_MAGNITUDE;
        }

        private void clearTiltHistoryLocked() {
            this.mTiltHistoryTimestampNanos[0] = Long.MIN_VALUE;
            this.mTiltHistoryIndex = 1;
        }

        private void addTiltHistoryEntryLocked(long now, float tilt) {
            this.mTiltHistory[this.mTiltHistoryIndex] = tilt;
            this.mTiltHistoryTimestampNanos[this.mTiltHistoryIndex] = now;
            this.mTiltHistoryIndex = (this.mTiltHistoryIndex + 1) % 200;
            this.mTiltHistoryTimestampNanos[this.mTiltHistoryIndex] = Long.MIN_VALUE;
        }

        private boolean isFlatLocked(long now) {
            int i = this.mTiltHistoryIndex;
            do {
                int nextTiltHistoryIndexLocked = nextTiltHistoryIndexLocked(i);
                i = nextTiltHistoryIndexLocked;
                if (nextTiltHistoryIndexLocked < 0 || this.mTiltHistory[i] < FLAT_ANGLE) {
                    return false;
                }
            } while (this.mTiltHistoryTimestampNanos[i] + 1000000000 > now);
            return true;
        }

        private boolean isSwingingLocked(long now, float tilt) {
            int i = this.mTiltHistoryIndex;
            do {
                int nextTiltHistoryIndexLocked = nextTiltHistoryIndexLocked(i);
                i = nextTiltHistoryIndexLocked;
                if (nextTiltHistoryIndexLocked < 0 || this.mTiltHistoryTimestampNanos[i] + 300000000 < now) {
                    return false;
                }
            } while (this.mTiltHistory[i] + SWING_AWAY_ANGLE_DELTA > tilt);
            return true;
        }

        private int nextTiltHistoryIndexLocked(int index) {
            int index2 = (index == 0 ? 200 : index) - 1;
            return this.mTiltHistoryTimestampNanos[index2] != Long.MIN_VALUE ? index2 : -1;
        }

        private float getLastTiltLocked() {
            int index = nextTiltHistoryIndexLocked(this.mTiltHistoryIndex);
            return index >= 0 ? this.mTiltHistory[index] : Float.NaN;
        }

        private float remainingMS(long now, long until) {
            return now >= until ? 0.0f : ((float) (until - now)) * 1.0E-6f;
        }
    }

    final class OrientationSensorJudge extends OrientationJudge {
        private int mDesiredRotation = -1;
        private int mProposedRotation = -1;
        private boolean mRotationEvaluationScheduled;
        private Runnable mRotationEvaluator = new Runnable() {
            public void run() {
                int newRotation;
                synchronized (WindowOrientationListener.this.mLock) {
                    OrientationSensorJudge.this.mRotationEvaluationScheduled = false;
                    newRotation = OrientationSensorJudge.this.evaluateRotationChangeLocked();
                }
                if (newRotation >= 0) {
                    WindowOrientationListener.this.onProposedRotationChanged(newRotation);
                }
            }
        };
        private long mTouchEndedTimestampNanos = Long.MIN_VALUE;
        private boolean mTouching;

        OrientationSensorJudge() {
            super();
        }

        public int getProposedRotationLocked() {
            return this.mProposedRotation;
        }

        public void onTouchStartLocked() {
            this.mTouching = true;
        }

        public void onTouchEndLocked(long whenElapsedNanos) {
            this.mTouching = false;
            this.mTouchEndedTimestampNanos = whenElapsedNanos;
            if (this.mDesiredRotation != this.mProposedRotation) {
                scheduleRotationEvaluationIfNecessaryLocked(SystemClock.elapsedRealtimeNanos());
            }
        }

        public void onSensorChanged(SensorEvent event) {
            int newRotation;
            synchronized (WindowOrientationListener.this.mLock) {
                this.mDesiredRotation = (int) event.values[0];
                newRotation = evaluateRotationChangeLocked();
            }
            if (newRotation >= 0) {
                WindowOrientationListener.this.onProposedRotationChanged(newRotation);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void dumpLocked(PrintWriter pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("OrientationSensorJudge");
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            prefix = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mDesiredRotation=");
            stringBuilder.append(Surface.rotationToString(this.mDesiredRotation));
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mProposedRotation=");
            stringBuilder.append(Surface.rotationToString(this.mProposedRotation));
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mTouching=");
            stringBuilder.append(this.mTouching);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mTouchEndedTimestampNanos=");
            stringBuilder.append(this.mTouchEndedTimestampNanos);
            pw.println(stringBuilder.toString());
        }

        public void resetLocked(boolean clearCurrentRotation) {
            if (clearCurrentRotation) {
                this.mProposedRotation = -1;
                this.mDesiredRotation = -1;
            }
            this.mTouching = false;
            this.mTouchEndedTimestampNanos = Long.MIN_VALUE;
            unscheduleRotationEvaluationLocked();
        }

        public int evaluateRotationChangeLocked() {
            unscheduleRotationEvaluationLocked();
            if (this.mDesiredRotation == this.mProposedRotation) {
                return -1;
            }
            long now = SystemClock.elapsedRealtimeNanos();
            if (isDesiredRotationAcceptableLocked(now)) {
                this.mProposedRotation = this.mDesiredRotation;
                return this.mProposedRotation;
            }
            scheduleRotationEvaluationIfNecessaryLocked(now);
            return -1;
        }

        private boolean isDesiredRotationAcceptableLocked(long now) {
            if (!this.mTouching && now >= this.mTouchEndedTimestampNanos + 500000000) {
                return true;
            }
            return false;
        }

        private void scheduleRotationEvaluationIfNecessaryLocked(long now) {
            if (this.mRotationEvaluationScheduled || this.mDesiredRotation == this.mProposedRotation) {
                if (WindowOrientationListener.LOG) {
                    Slog.d(WindowOrientationListener.TAG, "scheduleRotationEvaluationLocked: ignoring, an evaluation is already scheduled or is unnecessary.");
                }
            } else if (this.mTouching) {
                if (WindowOrientationListener.LOG) {
                    Slog.d(WindowOrientationListener.TAG, "scheduleRotationEvaluationLocked: ignoring, user is still touching the screen.");
                }
            } else {
                long timeOfNextPossibleRotationNanos = this.mTouchEndedTimestampNanos + 500000000;
                if (now >= timeOfNextPossibleRotationNanos) {
                    if (WindowOrientationListener.LOG) {
                        Slog.d(WindowOrientationListener.TAG, "scheduleRotationEvaluationLocked: ignoring, already past the next possible time of rotation.");
                    }
                    return;
                }
                WindowOrientationListener.this.mHandler.postDelayed(this.mRotationEvaluator, (long) Math.ceil((double) (((float) (timeOfNextPossibleRotationNanos - now)) * 1.0E-6f)));
                this.mRotationEvaluationScheduled = true;
            }
        }

        private void unscheduleRotationEvaluationLocked() {
            if (this.mRotationEvaluationScheduled) {
                WindowOrientationListener.this.mHandler.removeCallbacks(this.mRotationEvaluator);
                this.mRotationEvaluationScheduled = false;
            }
        }
    }

    public abstract void onProposedRotationChanged(int i);

    public WindowOrientationListener(Context context, Handler handler) {
        this(context, handler, 2);
    }

    private WindowOrientationListener(Context context, Handler handler, int rate) {
        this.mCurrentRotation = -1;
        this.mLock = new Object();
        this.mWOLProxy = new WindowOrientationListenerProxy() {
            public void setCurrentOrientation(int proposedRotation) {
                WindowOrientationListener.this.mCurrentRotation = proposedRotation;
            }

            public void notifyProposedRotation(int proposedRotation) {
                WindowOrientationListener.this.onProposedRotationChanged(proposedRotation);
            }
        };
        if (McuSwitch) {
            this.mHandler = handler;
            this.mHWEMRProcessor = HwServiceFactory.getHWExtMotionRotationProcessor(this.mWOLProxy);
            return;
        }
        this.mHandler = handler;
        this.mSensorManager = (SensorManager) context.getSystemService("sensor");
        this.mRate = rate;
        Sensor nonWakeUpDeviceOrientationSensor = null;
        for (Sensor s : this.mSensorManager.getSensorList(27)) {
            if (s.isWakeUpSensor()) {
            }
            nonWakeUpDeviceOrientationSensor = s;
        }
        if (null != null) {
            this.mSensor = null;
        } else {
            this.mSensor = nonWakeUpDeviceOrientationSensor;
        }
        if (this.mSensor != null) {
            this.mOrientationJudge = new OrientationSensorJudge();
        }
        if (this.mOrientationJudge == null) {
            this.mSensor = this.mSensorManager.getDefaultSensor(1);
            if (this.mSensor != null) {
                this.mOrientationJudge = new AccelSensorJudge(context);
            }
        }
    }

    public void enable() {
        enable(true);
    }

    public void enable(boolean clearCurrentRotation) {
        synchronized (this.mLock) {
            if (this.mEnabled) {
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WindowOrientationListener enabled clearCurrentRotation=");
            stringBuilder.append(clearCurrentRotation);
            Slog.i(str, stringBuilder.toString());
            if (McuSwitch) {
                this.mHWEMRProcessor.enableMotionRotation(this.mHandler);
            } else if (this.mSensor == null) {
                Slog.w(TAG, "Cannot detect sensors. Not enabled");
                return;
            } else {
                this.mOrientationJudge.resetLocked(clearCurrentRotation);
                if (this.mSensor.getType() == 1) {
                    this.mSensorManager.registerListener(this.mOrientationJudge, this.mSensor, this.mRate, DEFAULT_BATCH_LATENCY, this.mHandler);
                } else {
                    this.mSensorManager.registerListener(this.mOrientationJudge, this.mSensor, this.mRate, this.mHandler);
                }
            }
            this.mEnabled = true;
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0031, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void disable() {
        synchronized (this.mLock) {
            if (this.mEnabled) {
                Slog.i(TAG, "WindowOrientationListener disabled");
                if (McuSwitch) {
                    this.mHWEMRProcessor.disableMotionRotation();
                } else if (this.mSensor == null) {
                    Slog.w(TAG, "Cannot detect sensors. Invalid disable");
                    return;
                } else {
                    this.mSensorManager.unregisterListener(this.mOrientationJudge);
                }
                this.mEnabled = false;
            }
        }
    }

    public void onTouchStart() {
        synchronized (this.mLock) {
            if (this.mOrientationJudge != null) {
                this.mOrientationJudge.onTouchStartLocked();
            }
        }
    }

    public void onTouchEnd() {
        long whenElapsedNanos = SystemClock.elapsedRealtimeNanos();
        synchronized (this.mLock) {
            if (this.mOrientationJudge != null) {
                this.mOrientationJudge.onTouchEndLocked(whenElapsedNanos);
            }
        }
    }

    public void setCurrentRotation(int rotation) {
        synchronized (this.mLock) {
            this.mCurrentRotation = rotation;
        }
    }

    public int getProposedRotation() {
        synchronized (this.mLock) {
            int proposedRotation;
            if (!this.mEnabled) {
                return -1;
            } else if (McuSwitch) {
                proposedRotation = this.mHWEMRProcessor.getProposedRotation();
                return proposedRotation;
            } else {
                proposedRotation = this.mOrientationJudge.getProposedRotationLocked();
                return proposedRotation;
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0010, code:
            return r2;
     */
    /* JADX WARNING: Missing block: B:14:0x0018, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean canDetectOrientation() {
        synchronized (this.mLock) {
            boolean z = false;
            if (McuSwitch) {
                if (this.mHWEMRProcessor != null) {
                    z = true;
                }
            } else if (this.mSensor != null) {
                z = true;
            }
        }
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        synchronized (this.mLock) {
            proto.write(1133871366145L, this.mEnabled);
            proto.write(1159641169922L, this.mCurrentRotation);
        }
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix) {
        synchronized (this.mLock) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append(TAG);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            prefix = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mEnabled=");
            stringBuilder.append(this.mEnabled);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mCurrentRotation=");
            stringBuilder.append(Surface.rotationToString(this.mCurrentRotation));
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mSensorType=");
            stringBuilder.append(this.mSensorType);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mSensor=");
            stringBuilder.append(this.mSensor);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mRate=");
            stringBuilder.append(this.mRate);
            pw.println(stringBuilder.toString());
            if (this.mOrientationJudge != null) {
                this.mOrientationJudge.dumpLocked(pw, prefix);
            }
        }
    }
}
