package huawei.com.android.server.policy.fingersense;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings.System;
import android.util.DisplayMetrics;
import android.util.Flog;
import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import android.util.Log;
import android.view.MotionEvent;
import com.android.server.gesture.GestureNavConst;
import com.huawei.itouch.HwITouchJniAdapter;

public class KnockGestureDetector {
    private static final boolean DEBUG = false;
    private static final float DOUBLE_KNOCK_RADIUS_IN = 0.4f;
    private static final float DOUBLE_KNOCK_RADIUS_LONG_IN = 0.6f;
    private static final long DOUBLE_KNOCK_TIMEOUT_MS = 250;
    private static final long DOUBLE_KNOCK_TIMEOUT_SLOW_MS = 400;
    private static final int DOUBLE_KNOCK_TOGGLE_THRESHOLD = 5;
    private static final long KNUCKLE_POINTER_COUNT_TIMEOUT_MS = 250;
    private static final long MS_DELAY_TO_DISABLE_DOUBLE_KNOCK = 250;
    private static final long MS_DELAY_TO_ENABLE_DOUBLE_KNOCK = 550;
    private static final long MS_FILTER_TO_PRINT_DOUBLE_KNOCK_INTERVAL = 1000;
    private static final String TAG = "KnockGestureDetector";
    private int consecutiveTouchCount = 1;
    private float doubleKnockDistanceInterval;
    private float doubleKnockDistanceMm;
    private int doubleKnockDistanceTrigger;
    private float doubleKnockRadiusLongPx;
    private float doubleKnockRadiusPx;
    private long doubleKnockTimeDiffMs;
    private long doubleKnockTimeInterval;
    private int doubleKnockTimeTrigger;
    private final MotionEventRunnable doubleKnuckleDoubleKnockRunnable = new MotionEventRunnable() {
        public void run() {
            if (KnockGestureDetector.this.consecutiveTouchCount < 3) {
                KnockGestureDetector.this.notifyDoubleKnuckleDoubleKnock("fingersense_knuckle_gesture_double_knock", this.event);
            } else {
                KnockGestureDetector.this.dumpDebugVariables();
            }
        }
    };
    private KnuckGestureSetting knuckGestureSetting = KnuckGestureSetting.getInstance();
    private int knucklePointerCount;
    private int knucklePointerId = -1;
    private float lastKnuckleDist;
    private long lastKnuckleEventTimeDiffMs;
    private long lastKnuckleEventTimeMs;
    private float lastKnuckleMoveDist;
    private float lastKnuckleMoveX;
    private float lastKnuckleMoveY;
    private float lastKnuckleX;
    private float lastKnuckleY;
    private long lastMotionEventTimeDiffMs;
    private long lastMotionEventTimeMs;
    private Context mContext;
    private final Handler mHandler;
    private final OnKnockGestureListener mOnKnockGestureListener;
    private final MotionEventRunnable singleKnuckleDoubleKnockRunnable = new MotionEventRunnable() {
        public void run() {
            if (KnockGestureDetector.this.consecutiveTouchCount < 3) {
                KnockGestureDetector.this.notifySingleKnuckleDoubleKnock("fingersense_knuckle_gesture_knock", this.event);
            } else {
                KnockGestureDetector.this.dumpDebugVariables();
            }
        }
    };

    private static abstract class MotionEventRunnable implements Runnable {
        MotionEvent event;

        private MotionEventRunnable() {
            this.event = null;
        }

        /* synthetic */ MotionEventRunnable(AnonymousClass1 x0) {
            this();
        }
    }

    public interface OnKnockGestureListener {
        void onDoubleKnocksNotYetConfirmed(String str, MotionEvent motionEvent);

        void onDoubleKnuckleDoubleKnock(String str, MotionEvent motionEvent);

        void onSingleKnuckleDoubleKnock(String str, MotionEvent motionEvent);
    }

    public KnockGestureDetector(Context context, OnKnockGestureListener listener) {
        this.mOnKnockGestureListener = listener;
        this.mHandler = new Handler();
        this.mContext = context;
        this.doubleKnockTimeDiffMs = System.getLong(this.mContext.getContentResolver(), "double_knock_timeout", 250);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        this.doubleKnockRadiusPx = (0.4f * metrics.density) * 160.0f;
        this.doubleKnockRadiusLongPx = (0.6f * metrics.density) * 160.0f;
        this.doubleKnockDistanceMm = System.getFloat(this.mContext.getContentResolver(), "double_knock_distance", this.doubleKnockRadiusPx);
        this.doubleKnockTimeTrigger = 0;
        this.doubleKnockDistanceTrigger = 0;
        HwITouchJniAdapter.getInstance().registerJniListener();
    }

    public int getKnucklePointerCount() {
        return this.knucklePointerCount;
    }

    public long getDoubleKnockTimeInterval() {
        return this.doubleKnockTimeInterval;
    }

    public float getDoubleKnockDisInterval() {
        return this.doubleKnockDistanceInterval;
    }

    public boolean onKnuckleTouchEvent(MotionEvent motionEvent) {
        long thisKnuckleEventTimeMs = motionEvent.getEventTime();
        this.lastKnuckleEventTimeDiffMs = thisKnuckleEventTimeMs - this.lastKnuckleEventTimeMs;
        this.lastKnuckleEventTimeMs = thisKnuckleEventTimeMs;
        boolean ret = false;
        switch (motionEvent.getAction()) {
            case 0:
                ret = touchDown(motionEvent);
                break;
            case 1:
                ret = touchUp(motionEvent);
                break;
            case 2:
                ret = touchMove(motionEvent);
                break;
            case 3:
                ret = touchCancel(motionEvent);
                break;
        }
        int count = motionEvent.getPointerCount();
        if (count > this.knucklePointerCount) {
            this.knucklePointerCount = count;
        } else if (this.lastKnuckleEventTimeDiffMs < 0 || this.lastKnuckleEventTimeDiffMs > 250) {
            this.knucklePointerCount = count;
        }
        if (motionEvent.getAction() == 0) {
            checkForDoubleKnocks(motionEvent);
        }
        return ret;
    }

    private boolean touchDown(MotionEvent motionEvent) {
        float thisKnuckleX = motionEvent.getX();
        float thisKnuckleY = motionEvent.getY();
        this.lastKnuckleDist = (float) Math.sqrt(Math.pow((double) (thisKnuckleX - this.lastKnuckleX), 2.0d) + Math.pow((double) (thisKnuckleY - this.lastKnuckleY), 2.0d));
        this.lastKnuckleMoveX = thisKnuckleX;
        this.lastKnuckleX = thisKnuckleX;
        this.lastKnuckleMoveY = thisKnuckleY;
        this.lastKnuckleY = thisKnuckleY;
        if (this.lastKnuckleEventTimeDiffMs < 0 || this.lastKnuckleEventTimeDiffMs > this.doubleKnockTimeDiffMs) {
            this.lastKnuckleMoveDist = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.knucklePointerId = motionEvent.getPointerId(0);
        }
        return true;
    }

    private boolean touchMove(MotionEvent motionEvent) {
        int pointerIndex = motionEvent.findPointerIndex(this.knucklePointerId);
        if (pointerIndex < 0) {
            return true;
        }
        float thisKnuckleX = motionEvent.getX(pointerIndex);
        float thisKnuckleY = motionEvent.getY(pointerIndex);
        this.lastKnuckleMoveDist += (float) Math.sqrt(Math.pow((double) (thisKnuckleX - this.lastKnuckleMoveX), 2.0d) + Math.pow((double) (thisKnuckleY - this.lastKnuckleMoveY), 2.0d));
        this.lastKnuckleMoveX = thisKnuckleX;
        this.lastKnuckleMoveY = thisKnuckleY;
        return true;
    }

    private boolean touchUp(MotionEvent motionEvent) {
        return true;
    }

    private boolean touchCancel(MotionEvent motionEvent) {
        return true;
    }

    private void toggleDoubleKnockSpeed() {
        long j = this.doubleKnockTimeDiffMs;
        long j2 = DOUBLE_KNOCK_TIMEOUT_SLOW_MS;
        if (j == DOUBLE_KNOCK_TIMEOUT_SLOW_MS) {
            j2 = 250;
        }
        j = j2;
        if (System.putLong(this.mContext.getContentResolver(), "double_knock_timeout", j)) {
            this.doubleKnockTimeDiffMs = j;
        } else {
            Log.d(TAG, "Write doubleKnockTimeDiffMs back to Settings failed!!");
        }
    }

    private void checkDoubleKnockSpeedToggle(long doubleKnockTime) {
        long timeInterval = doubleKnockTime;
        if (this.doubleKnockTimeDiffMs == 250) {
            if (timeInterval < 250 || timeInterval > DOUBLE_KNOCK_TIMEOUT_SLOW_MS) {
                return;
            }
        } else if (this.doubleKnockTimeDiffMs == DOUBLE_KNOCK_TIMEOUT_SLOW_MS && timeInterval > 250) {
            return;
        }
        this.doubleKnockTimeTrigger++;
        if (this.doubleKnockTimeTrigger >= 5) {
            toggleDoubleKnockSpeed();
            this.doubleKnockTimeTrigger = 0;
        }
    }

    private void toggleDoubleKnockDistance() {
        float newDistance = this.doubleKnockDistanceMm == this.doubleKnockRadiusLongPx ? this.doubleKnockRadiusPx : this.doubleKnockRadiusLongPx;
        if (System.putFloat(this.mContext.getContentResolver(), "double_knock_distance", newDistance)) {
            this.doubleKnockDistanceMm = newDistance;
        } else {
            Log.d(TAG, "Write doubleKnockDistance back to Settings failed!!");
        }
    }

    private void checkDoubleKnockDistanceToggle(float doubleKnockDistance) {
        float distanceInterval = doubleKnockDistance;
        if (this.doubleKnockDistanceMm == this.doubleKnockRadiusPx) {
            if (distanceInterval < this.doubleKnockRadiusPx || distanceInterval > this.doubleKnockRadiusLongPx) {
                return;
            }
        } else if (this.doubleKnockDistanceMm == this.doubleKnockRadiusLongPx && distanceInterval > this.doubleKnockRadiusPx) {
            return;
        }
        this.doubleKnockDistanceTrigger++;
        if (this.doubleKnockDistanceTrigger >= 5) {
            toggleDoubleKnockDistance();
            this.doubleKnockDistanceTrigger = 0;
        }
    }

    public boolean onAnyTouchEvent(MotionEvent motionEvent) {
        MotionEvent motionEvent2;
        if (motionEvent.getAction() == 0) {
            long thisMotionEventTimeMs = motionEvent.getEventTime();
            long timeDiffToLastKnuckleMs = thisMotionEventTimeMs - this.lastKnuckleEventTimeMs;
            this.lastMotionEventTimeDiffMs = thisMotionEventTimeMs - this.lastMotionEventTimeMs;
            this.lastMotionEventTimeMs = thisMotionEventTimeMs;
            if (this.lastMotionEventTimeDiffMs <= 0 || this.lastMotionEventTimeDiffMs >= MS_DELAY_TO_ENABLE_DOUBLE_KNOCK) {
                motionEvent2 = motionEvent;
                this.consecutiveTouchCount = 1;
            } else {
                int pointCount;
                EventStream eStream;
                this.consecutiveTouchCount++;
                int i = 1503;
                if (this.consecutiveTouchCount == 2 && timeDiffToLastKnuckleMs < 250) {
                    pointCount = motionEvent.getPointerCount();
                    int i2 = 0;
                    while (true) {
                        int i3 = i2;
                        if (i3 >= pointCount) {
                            break;
                        } else if (motionEvent.getToolType(i3) != 7) {
                            Flog.i(i, "The second tap is finger, aborting double knuckle detecting!!");
                            if (this.lastMotionEventTimeMs - this.knuckGestureSetting.getLastReportFSTime() > 1000) {
                                eStream = IMonitor.openEventStream(KnuckGestureSetting.KNOCK_GESTURE_DATA_RECORD);
                                if (eStream != null) {
                                    eStream.setParam((short) 13, 1);
                                    eStream.setParam((short) 22, 1);
                                    eStream.setParam((short) 23, this.knuckGestureSetting.getTpVendorName());
                                    eStream.setParam((short) 24, this.knuckGestureSetting.getAccVendorName());
                                    eStream.setParam((short) 25, this.knuckGestureSetting.getLcdInfo());
                                    eStream.setParam((short) 26, this.knuckGestureSetting.getOrientation());
                                    IMonitor.sendEvent(eStream);
                                    this.knuckGestureSetting.setLastReportFSTime(SystemClock.uptimeMillis());
                                    IMonitor.closeEventStream(eStream);
                                }
                            }
                        } else {
                            i2 = i3 + 1;
                            i = 1503;
                        }
                    }
                }
                motionEvent2 = motionEvent;
                pointCount = HwITouchJniAdapter.getInstance().getAppType();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onAnyTouchEvent current appFilter=");
                stringBuilder.append(pointCount);
                Log.d(str, stringBuilder.toString());
                if (pointCount == 1 && this.consecutiveTouchCount > 2) {
                    if (timeDiffToLastKnuckleMs < MS_DELAY_TO_ENABLE_DOUBLE_KNOCK) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Multiple rapid taps detecting, aborting Knuckle Gestures:");
                        stringBuilder2.append(this.consecutiveTouchCount);
                        Flog.i(1503, stringBuilder2.toString());
                        if (this.lastMotionEventTimeMs - this.knuckGestureSetting.getLastReportFSTime() > 1000) {
                            eStream = IMonitor.openEventStream(KnuckGestureSetting.KNOCK_GESTURE_DATA_RECORD);
                            if (eStream != null) {
                                eStream.setParam((short) 13, 1);
                                eStream.setParam((short) 21, 1);
                                eStream.setParam((short) 23, this.knuckGestureSetting.getTpVendorName());
                                eStream.setParam((short) 24, this.knuckGestureSetting.getAccVendorName());
                                eStream.setParam((short) 25, this.knuckGestureSetting.getLcdInfo());
                                eStream.setParam((short) 26, this.knuckGestureSetting.getOrientation());
                                IMonitor.sendEvent(eStream);
                                this.knuckGestureSetting.setLastReportFSTime(SystemClock.uptimeMillis());
                                IMonitor.closeEventStream(eStream);
                            }
                        }
                    }
                    this.mHandler.removeCallbacks(this.singleKnuckleDoubleKnockRunnable);
                }
            }
        } else {
            motionEvent2 = motionEvent;
        }
        return true;
    }

    public boolean checkForDoubleKnocks(MotionEvent e) {
        this.doubleKnockTimeInterval = this.lastKnuckleEventTimeDiffMs;
        this.doubleKnockDistanceInterval = this.lastKnuckleDist;
        StringBuilder stringBuilder;
        if (this.lastKnuckleEventTimeDiffMs < this.doubleKnockTimeDiffMs) {
            if (this.lastKnuckleMoveDist < 2.0f * this.doubleKnockDistanceMm) {
                final MotionEvent motionEvent = e;
                int appFilter = HwITouchJniAdapter.getInstance().getAppType();
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("checkForDoubleKnocks current appFilter=");
                stringBuilder2.append(appFilter);
                Log.d(str, stringBuilder2.toString());
                if (appFilter == 1) {
                    this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            KnockGestureDetector.this.checkForDoubleKnocksInternal(motionEvent);
                        }
                    }, 25);
                } else {
                    checkForDoubleKnocksInternal(motionEvent);
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("lastKnuckleMoveDist too far, not triggering knock: ");
                stringBuilder.append(this.lastKnuckleMoveDist);
                stringBuilder.append(" > ");
                stringBuilder.append(this.doubleKnockRadiusPx);
                Flog.i(1503, stringBuilder.toString());
            }
        } else if (this.lastKnuckleEventTimeDiffMs < 1000) {
            if ((this.knucklePointerCount == 1 && this.lastKnuckleDist < this.doubleKnockDistanceMm) || (this.knucklePointerCount == 2 && this.lastKnuckleMoveDist < 2.0f * this.doubleKnockDistanceMm)) {
                checkDoubleKnockSpeedToggle(this.doubleKnockTimeInterval);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("lastKnuckleEventTimeDiffMs too large, not triggering knock: ");
            stringBuilder.append(this.lastKnuckleEventTimeDiffMs);
            stringBuilder.append(" > ");
            stringBuilder.append(this.doubleKnockTimeDiffMs);
            Flog.i(1503, stringBuilder.toString());
            if (e.getEventTime() - this.knuckGestureSetting.getLastReportFSTime() > 1000) {
                EventStream eStream = IMonitor.openEventStream(KnuckGestureSetting.KNOCK_GESTURE_DATA_RECORD);
                if (eStream != null) {
                    eStream.setParam((short) 19, 1);
                    if (this.knucklePointerCount == 1) {
                        eStream.setParam((short) 16, this.lastKnuckleEventTimeDiffMs);
                        eStream.setParam((short) 13, 1);
                    } else if (this.knucklePointerCount == 2) {
                        eStream.setParam((short) 17, this.lastKnuckleEventTimeDiffMs);
                        eStream.setParam((short) 14, 1);
                    }
                    eStream.setParam((short) 23, this.knuckGestureSetting.getTpVendorName());
                    eStream.setParam((short) 24, this.knuckGestureSetting.getAccVendorName());
                    eStream.setParam((short) 25, this.knuckGestureSetting.getLcdInfo());
                    eStream.setParam((short) 26, this.knuckGestureSetting.getOrientation());
                    IMonitor.sendEvent(eStream);
                    this.knuckGestureSetting.setLastReportFSTime(SystemClock.uptimeMillis());
                    IMonitor.closeEventStream(eStream);
                }
            }
        }
        return false;
    }

    private void checkForDoubleKnocksInternal(MotionEvent e) {
        if (this.knucklePointerCount == 1) {
            if (this.lastKnuckleDist < this.doubleKnockDistanceMm) {
                onDoubleKnocks(e, "fingersense_knuckle_gesture_knock");
                checkDoubleKnockSpeedToggle(this.doubleKnockTimeInterval);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Double knock too far: ");
                stringBuilder.append(this.lastKnuckleDist);
                stringBuilder.append(" > ");
                stringBuilder.append(this.doubleKnockDistanceMm);
                stringBuilder.append(", no screenshot");
                Flog.i(1503, stringBuilder.toString());
                if (e.getEventTime() - this.knuckGestureSetting.getLastReportFSTime() > 1000) {
                    EventStream eStream = IMonitor.openEventStream(KnuckGestureSetting.KNOCK_GESTURE_DATA_RECORD);
                    if (eStream != null) {
                        eStream.setParam((short) 13, 1);
                        eStream.setParam((short) 18, this.lastKnuckleDist);
                        eStream.setParam((short) 20, 1);
                        eStream.setParam((short) 23, this.knuckGestureSetting.getTpVendorName());
                        eStream.setParam((short) 24, this.knuckGestureSetting.getAccVendorName());
                        eStream.setParam((short) 25, this.knuckGestureSetting.getLcdInfo());
                        eStream.setParam((short) 26, this.knuckGestureSetting.getOrientation());
                        IMonitor.sendEvent(eStream);
                        this.knuckGestureSetting.setLastReportFSTime(SystemClock.uptimeMillis());
                        IMonitor.closeEventStream(eStream);
                    }
                }
            }
            checkDoubleKnockDistanceToggle(this.doubleKnockDistanceInterval);
            return;
        }
        onDoubleKnocks(e, "fingersense_knuckle_gesture_double_knock");
        checkDoubleKnockSpeedToggle(this.doubleKnockTimeInterval);
    }

    public void onDoubleKnocks(MotionEvent event, String knuckleGesture) {
        notifyOnDoubleKnocksNotYetConfirmed(knuckleGesture, event);
        int appFilter = HwITouchJniAdapter.getInstance().getAppType();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onDoubleKnocks current appFilter=");
        stringBuilder.append(appFilter);
        Log.d(str, stringBuilder.toString());
        if (knuckleGesture != null && knuckleGesture.equals("fingersense_knuckle_gesture_knock")) {
            Flog.i(1503, "Single Knuckle Double Knock captured");
            if (appFilter == 1) {
                this.singleKnuckleDoubleKnockRunnable.event = event;
                this.mHandler.postDelayed(this.singleKnuckleDoubleKnockRunnable, 250);
            } else if (this.consecutiveTouchCount < 3) {
                notifySingleKnuckleDoubleKnock("fingersense_knuckle_gesture_knock", event);
            } else {
                dumpDebugVariables();
            }
        } else if (knuckleGesture == null || !knuckleGesture.equals("fingersense_knuckle_gesture_double_knock")) {
            Flog.w(1503, "FingerSense Gesture Unrecognized Knock Pattern.");
        } else {
            Flog.i(1503, "Double Knuckle Double Knock captured");
            if (appFilter == 1) {
                this.doubleKnuckleDoubleKnockRunnable.event = event;
                this.mHandler.postDelayed(this.doubleKnuckleDoubleKnockRunnable, 250);
            } else if (this.consecutiveTouchCount < 3) {
                notifyDoubleKnuckleDoubleKnock("fingersense_knuckle_gesture_double_knock", event);
            } else {
                dumpDebugVariables();
            }
        }
    }

    private void notifyDoubleKnuckleDoubleKnock(String gestureName, MotionEvent event) {
        Log.w(TAG, "Notifying Double Knuckle Double Knock");
        if (this.mOnKnockGestureListener != null) {
            this.mOnKnockGestureListener.onDoubleKnuckleDoubleKnock(gestureName, event);
        }
    }

    private void notifySingleKnuckleDoubleKnock(String gestureName, MotionEvent event) {
        Log.w(TAG, "Notifying Single Knuckle Double Knock");
        if (this.mOnKnockGestureListener != null) {
            this.mOnKnockGestureListener.onSingleKnuckleDoubleKnock(gestureName, event);
        }
    }

    private void notifyOnDoubleKnocksNotYetConfirmed(String gestureName, MotionEvent event) {
        if (this.mOnKnockGestureListener != null) {
            this.mOnKnockGestureListener.onDoubleKnocksNotYetConfirmed(gestureName, event);
        }
    }

    private void dumpDebugVariables() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("lastMotionEventTimeDiffMs: ");
        stringBuilder.append(this.lastMotionEventTimeDiffMs);
        stringBuilder.append(" lastKnuckleEventTimeDiffMs: ");
        stringBuilder.append(this.lastKnuckleEventTimeDiffMs);
        stringBuilder.append(" lastKnuckleDist: ");
        stringBuilder.append(this.lastKnuckleDist);
        stringBuilder.append(" lastKnuckleMoveDist: ");
        stringBuilder.append(this.lastKnuckleMoveDist);
        stringBuilder.append(" knucklePointerCount: ");
        stringBuilder.append(this.knucklePointerCount);
        stringBuilder.append(" consecutiveTouchCount: ");
        stringBuilder.append(this.consecutiveTouchCount);
        stringBuilder.append(" doubleKnockTimeDiffMs: ");
        stringBuilder.append(this.doubleKnockTimeDiffMs);
        stringBuilder.append(" doubleKnockDistanceMm: ");
        stringBuilder.append(this.doubleKnockDistanceMm);
        Flog.i(1503, stringBuilder.toString());
    }
}
