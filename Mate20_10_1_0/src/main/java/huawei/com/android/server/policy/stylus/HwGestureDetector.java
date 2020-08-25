package huawei.com.android.server.policy.stylus;

import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GesturePoint;
import android.gesture.GestureStroke;
import android.gesture.GestureUtils;
import android.gesture.OrientedBoundingBox;
import android.gesture.Prediction;
import android.graphics.RectF;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import com.huawei.android.util.HwPCUtilsEx;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HwGestureDetector {
    private static final float BOX_WIDTH_SCALE = 0.9f;
    private static final float DEFAULT_LINE_GESTURE_STROKE_STRAIGTHNESS = 4.0f;
    private static final boolean IS_DEBUG_LETTER = false;
    private static final float LINE_GESTURE_STROKE_LANDSCAPE_ANGLE = 90.0f;
    private static final float LINE_GESTURE_STROKE_PORTRAIT_ANGLE = 0.0f;
    private static final float MAX_LINE_GESTURE_SQUARENESS = 0.06f;
    private static final float MAX_LINE_GESTURE_STROKE_ANGLE_DEVIATION = 10.0f;
    private static final float MAX_LINE_GESTURE_STROKE_SQUARENESS = 0.1f;
    private static final float MIN_GESTURE_STROKE_LENGTH = 200.0f;
    private static final float MIN_LETTER_GESTURE_STROKE_SQUARENESS = 0.15f;
    private static final float MIN_LINE_GESTURE_STROKE_LENGTH = 500.0f;
    private static final int MIN_POINT_LENGTH = 4;
    private static final float MIN_PREDICTION_SCORE = 2.0f;
    private static final float MOVE_TOLERANCE = 3.0f;
    private static final float NON_ENCLOSED_REGION_DIAGONAL_THRESHOLD = 0.6f;
    private static final int POINT_LAST_INDEX1 = 1;
    private static final int POINT_LAST_INDEX2 = 2;
    private static final float REGION_DIAGONAL_THRESHOLD = 0.25f;
    private static final String STYLUS_GESTURE_C_SUFFIX = "c";
    private static final String STYLUS_GESTURE_LINE_SUFFIX = "line";
    private static final String STYLUS_GESTURE_M_SUFFIX = "m";
    private static final String STYLUS_GESTURE_REGION_SUFFIX = "region";
    private static final String STYLUS_GESTURE_REJECT_SUFFIX = "reject";
    private static final String STYLUS_GESTURE_S_SUFFIX = "s";
    private static final String STYLUS_GESTURE_W_SUFFIX = "w";
    private static final String TAG = "HwGestureDetector";
    private static final int TYPE_STYLUS_OPERATION_SUCCESSD = 952;
    /* access modifiers changed from: private */
    public static AtomicInteger sGestureId = new AtomicInteger(0);
    private Context mContext;
    private GestureListener mGestureListener;
    /* access modifiers changed from: private */
    public GesturePerformedRunnable mGestureRecognizer = new GesturePerformedRunnable();
    private GestureState mGestureState;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private boolean mIsAsyncNotification = true;
    private float mLineGestureLandAngle = LINE_GESTURE_STROKE_LANDSCAPE_ANGLE;
    private float mLineGesturePortAngle = 0.0f;
    private float mLineGestureStraightness = DEFAULT_LINE_GESTURE_STROKE_STRAIGTHNESS;
    private float mMaxLineGestureAngleDeviation = MAX_LINE_GESTURE_STROKE_ANGLE_DEVIATION;
    /* access modifiers changed from: private */
    public float mMinGestureStrokeLength = MIN_GESTURE_STROKE_LENGTH;
    /* access modifiers changed from: private */
    public float mMinLetterGestureSquareness = MIN_LETTER_GESTURE_STROKE_SQUARENESS;
    private float mMinLineGestureLength = MIN_LINE_GESTURE_STROKE_LENGTH;
    private float mMinNonEnclosedRegionDiag = 0.6f;
    private float mMinPredictionScore = 2.0f;
    private float mMinRegionDiag = REGION_DIAGONAL_THRESHOLD;
    private GestureLibrary mMultiOrientGestureLib;
    private OrientationFix[] mOrientationFixes = {new OrientationFix("c", "w", null), new OrientationFix("c", "m", null)};
    private GestureLibrary mSigleOrientGestureLib;

    public interface GestureListener {
        void onLetterGesture(String str, Gesture gesture, double d);

        void onLineGesture(String str, Gesture gesture);

        void onRegionGesture(String str, Gesture gesture, double d);
    }

    /* access modifiers changed from: private */
    public class GesturePerformedRunnable implements Runnable {
        private GestureState mGestureState;

        GesturePerformedRunnable() {
        }

        public void run() {
            HwGestureDetector.this.fireOnGesturePerformed(this.mGestureState);
        }

        public void setGestureState(GestureState state) {
            this.mGestureState = state;
        }
    }

    public static class OrientationFix {
        String mMultiOrientGestureName;
        String mReplaceGestureWith;
        String mSingleOrientGestureName;

        public OrientationFix(String gestureName, String singleOrientationGestureName, String replaceGestureWith) {
            this.mMultiOrientGestureName = gestureName;
            this.mSingleOrientGestureName = singleOrientationGestureName;
            this.mReplaceGestureWith = replaceGestureWith;
        }
    }

    /* access modifiers changed from: private */
    public class GestureState {
        private static final int INITIAL_CAPACITY = 100;
        private Gesture mGesture = null;
        private int mGestureId;
        private boolean mIsGesturing = false;
        /* access modifiers changed from: private */
        public boolean mIsLetterGesturing = false;
        private boolean mIsListeningGestures;
        /* access modifiers changed from: private */
        public boolean mIsStylus = false;
        private float mLastX = 0.0f;
        private float mLastY = 0.0f;
        private final ArrayList<GesturePoint> mStrokeBufferList = new ArrayList<>(100);
        private float mTrackLength = 0.0f;

        GestureState() {
            reset();
        }

        /* access modifiers changed from: private */
        public void touchDown(MotionEvent event) {
            reset();
            this.mGestureId = HwGestureDetector.sGestureId.getAndIncrement();
            boolean z = true;
            this.mIsListeningGestures = true;
            float eventX = event.getX();
            float eventY = event.getY();
            this.mLastX = eventX;
            this.mLastY = eventY;
            this.mGesture = new Gesture();
            this.mStrokeBufferList.add(new GesturePoint(eventX, eventY, event.getEventTime()));
            if (event.getToolType(0) != 2) {
                z = false;
            }
            this.mIsStylus = z;
        }

        /* access modifiers changed from: private */
        public void touchMove(MotionEvent event) {
            if (this.mIsListeningGestures) {
                float eventX = event.getX();
                float eventY = event.getY();
                float prevEventX = this.mLastX;
                float prevEventY = this.mLastY;
                float dx = Math.abs(eventX - prevEventX);
                float dy = Math.abs(eventY - prevEventY);
                if (dx >= 3.0f || dy >= 3.0f) {
                    this.mLastX = eventX;
                    this.mLastY = eventY;
                    this.mStrokeBufferList.add(new GesturePoint(eventX, eventY, event.getEventTime()));
                    if (!this.mIsGesturing) {
                        this.mTrackLength += (float) Math.sqrt((double) ((dx * dx) + (dy * dy)));
                        if (this.mTrackLength > HwGestureDetector.this.mMinGestureStrokeLength) {
                            this.mIsGesturing = true;
                        }
                    }
                    if (this.mIsGesturing && !this.mIsLetterGesturing && GestureUtils.computeOrientedBoundingBox(this.mStrokeBufferList).squareness > HwGestureDetector.this.mMinLetterGestureSquareness) {
                        this.mIsLetterGesturing = true;
                    }
                }
            }
        }

        /* access modifiers changed from: private */
        public void touchUp(MotionEvent event, boolean isCancel) {
            this.mIsListeningGestures = false;
            Gesture gesture = this.mGesture;
            if (gesture != null) {
                gesture.addStroke(new GestureStroke(this.mStrokeBufferList));
                HwGestureDetector.this.mGestureRecognizer.setGestureState(this);
                HwGestureDetector.this.mHandler.removeCallbacks(HwGestureDetector.this.mGestureRecognizer);
                if (!isCancel) {
                    HwGestureDetector.this.mHandler.post(HwGestureDetector.this.mGestureRecognizer);
                }
            }
        }

        private void reset() {
            this.mLastX = 0.0f;
            this.mLastY = 0.0f;
            this.mTrackLength = 0.0f;
            this.mIsGesturing = false;
            this.mIsLetterGesturing = false;
            this.mIsStylus = false;
            this.mGesture = null;
        }

        public Gesture getGesture() {
            return this.mGesture;
        }

        public boolean checkLetter() {
            return this.mIsLetterGesturing;
        }

        public boolean isListening() {
            return this.mIsListeningGestures;
        }

        public boolean conflict(GestureState other) {
            return other != null && this.mGestureId == other.mGestureId;
        }
    }

    HwGestureDetector(Context context, String gestureLibraryFilePath) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mMultiOrientGestureLib = GestureLibraries.fromFile(gestureLibraryFilePath);
        this.mMultiOrientGestureLib.setOrientationStyle(2);
        this.mMultiOrientGestureLib.setSequenceType(2);
        this.mMultiOrientGestureLib.load();
        this.mSigleOrientGestureLib = GestureLibraries.fromFile(gestureLibraryFilePath);
        this.mSigleOrientGestureLib.setOrientationStyle(1);
        this.mSigleOrientGestureLib.setSequenceType(1);
        this.mSigleOrientGestureLib.load();
        this.mGestureState = new GestureState();
    }

    public void setGestureListener(GestureListener gestureListener) {
        this.mGestureListener = gestureListener;
    }

    public void setAsyncNotifications(boolean isAsync) {
        this.mIsAsyncNotification = isAsync;
    }

    public void setMinLineGestureStrokeLength(int length) {
        this.mMinLineGestureLength = (float) length;
    }

    public void clear() {
        clear(false);
    }

    private void clear(boolean isFireActionPerformed) {
        if (isFireActionPerformed) {
            this.mGestureRecognizer.setGestureState(this.mGestureState);
            if (this.mIsAsyncNotification) {
                this.mHandler.post(this.mGestureRecognizer);
            } else {
                fireOnGesturePerformed(this.mGestureState);
            }
        } else {
            Log.w(TAG, "Knuckle Gesture performed but not recognized as Region, Letter, or Line Gesture.");
            this.mGestureState = null;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        int action = event.getAction();
        if (action != 0 && !this.mGestureState.isListening()) {
            return false;
        }
        if (action == 0) {
            this.mGestureState = new GestureState();
            this.mGestureState.touchDown(event);
        } else if (action == 1) {
            this.mGestureState.touchUp(event, false);
        } else if (action == 2) {
            this.mGestureState.touchMove(event);
        } else if (action == 3) {
            this.mGestureState.touchUp(event, true);
        }
        return true;
    }

    private float calcMoveDistance(Gesture gesture) {
        float[] points = gesture.getStrokes().get(0).points;
        if (points.length < 4) {
            return 0.0f;
        }
        float firstX = points[0];
        float firstY = points[1];
        float lastX = points[points.length - 2];
        float dx = firstX - lastX;
        float dy = firstY - points[points.length - 1];
        return (float) Math.sqrt((double) ((dx * dx) + (dy * dy)));
    }

    private boolean notifyIfLineGesture(GestureState gestureState) {
        float baseAngle;
        if (!gestureState.conflict(this.mGestureState)) {
            Log.e(TAG, "notifyIfLineGesture confilct!");
            return false;
        }
        Gesture gesture = gestureState.getGesture();
        OrientedBoundingBox box = gesture.getStrokes().get(0).computeOrientedBoundingBox();
        if (box.squareness > MAX_LINE_GESTURE_SQUARENESS) {
            return false;
        }
        if (this.mContext.getResources().getConfiguration().orientation == 2) {
            baseAngle = this.mLineGestureLandAngle;
        } else {
            baseAngle = this.mLineGesturePortAngle;
        }
        if (Math.abs(baseAngle - Math.abs(box.orientation)) > this.mMaxLineGestureAngleDeviation) {
            return false;
        }
        float distance = calcMoveDistance(gesture);
        if (distance < this.mMinLineGestureLength || distance < box.width * BOX_WIDTH_SCALE) {
            return false;
        }
        notifyLineGesture(STYLUS_GESTURE_LINE_SUFFIX, gesture);
        return true;
    }

    private boolean notifyIfRegionGesture(GestureState gestureState, float threshold) {
        if (!gestureState.conflict(this.mGestureState)) {
            Log.e(TAG, "notifyIfRegionGesture confilct!");
            return false;
        }
        Gesture gesture = gestureState.getGesture();
        float distance = calcMoveDistance(gesture);
        RectF box = gesture.getBoundingBox();
        float height = box.height();
        float width = box.width();
        if (distance >= ((float) Math.sqrt((double) ((height * height) + (width * width)))) * threshold) {
            return false;
        }
        notifyRegionGesture("region", (double) this.mMinPredictionScore, gesture);
        return true;
    }

    private boolean notifyIfLetterGesture(GestureState gestureState) {
        if (!gestureState.conflict(this.mGestureState) || !gestureState.mIsLetterGesturing) {
            return false;
        }
        Gesture gesture = gestureState.getGesture();
        ArrayList<Prediction> multiOrientPredictions = this.mMultiOrientGestureLib.recognize(gesture);
        ArrayList<Prediction> singleOrientPredictions = this.mSigleOrientGestureLib.recognize(gesture);
        if (multiOrientPredictions == null || singleOrientPredictions == null) {
            Log.e(TAG, "The gesture library returned a null list of predictions. Ignoring gesture.");
            return false;
        } else if (multiOrientPredictions.isEmpty()) {
            return false;
        } else {
            debugPredictionInfo(multiOrientPredictions, singleOrientPredictions);
            Prediction bestMultiOrientPrediction = multiOrientPredictions.get(0);
            if (bestMultiOrientPrediction.name.equals(STYLUS_GESTURE_REJECT_SUFFIX) || bestMultiOrientPrediction.score < ((double) this.mMinPredictionScore)) {
                return false;
            }
            if (singleOrientPredictions.isEmpty()) {
                notifyLetterGesture(bestMultiOrientPrediction.name, bestMultiOrientPrediction.score, gesture);
                return true;
            }
            Prediction bestSingleOrientPrediction = singleOrientPredictions.get(0);
            if (bestSingleOrientPrediction.name.equals(STYLUS_GESTURE_REJECT_SUFFIX)) {
                return false;
            }
            if (bestMultiOrientPrediction.name.equals(bestSingleOrientPrediction.name)) {
                notifyLetterGesture(bestMultiOrientPrediction.name, bestMultiOrientPrediction.score, gesture);
                return true;
            } else if (this.mOrientationFixes == null) {
                return fixSingleOrientationLibrary(singleOrientPredictions, bestMultiOrientPrediction, gesture);
            } else {
                return fixMultiOrientationLibrary(singleOrientPredictions, bestMultiOrientPrediction, bestSingleOrientPrediction, gesture);
            }
        }
    }

    private boolean fixMultiOrientationLibrary(List<Prediction> singleOrientPredictions, Prediction bestMultiOrientPrediction, Prediction bestSingleOrientPrediction, Gesture gesture) {
        OrientationFix[] orientationFixArr = this.mOrientationFixes;
        int length = orientationFixArr.length;
        int i = 0;
        while (i < length) {
            OrientationFix fix = orientationFixArr[i];
            if (!bestMultiOrientPrediction.name.equals(fix.mMultiOrientGestureName) || !bestSingleOrientPrediction.name.equals(fix.mSingleOrientGestureName)) {
                i++;
            } else if (fix.mReplaceGestureWith == null) {
                return false;
            } else {
                notifyLetterGesture(fix.mReplaceGestureWith, bestMultiOrientPrediction.score, gesture);
                return true;
            }
        }
        return fixSingleOrientationLibrary(singleOrientPredictions, bestMultiOrientPrediction, gesture);
    }

    private boolean fixSingleOrientationLibrary(List<Prediction> singleOrientPredictions, Prediction bestMultiOrientPrediction, Gesture gesture) {
        if (singleOrientPredictions.size() <= 1 || !singleOrientPredictions.get(1).name.equals(bestMultiOrientPrediction.name)) {
            return false;
        }
        notifyLetterGesture(bestMultiOrientPrediction.name, bestMultiOrientPrediction.score, gesture);
        return true;
    }

    private void notifyLetterGesture(String gestureName, double score, Gesture gesture) {
        GestureListener gestureListener = this.mGestureListener;
        if (gestureListener != null) {
            gestureListener.onLetterGesture(gestureName, gesture, score);
        }
    }

    private void notifyRegionGesture(String gestureName, double score, Gesture gesture) {
        GestureListener gestureListener = this.mGestureListener;
        if (gestureListener != null) {
            gestureListener.onRegionGesture(gestureName, gesture, score);
        }
    }

    private void notifyLineGesture(String gestureName, Gesture gesture) {
        GestureListener gestureListener = this.mGestureListener;
        if (gestureListener != null) {
            gestureListener.onLineGesture(gestureName, gesture);
        }
    }

    /* access modifiers changed from: private */
    public void fireOnGesturePerformed(GestureState state) {
        Gesture gesture = state.getGesture();
        if (gesture == null || gesture.getStrokesCount() <= 0) {
            Log.w(TAG, "Ignoring empty gesture.");
        } else if (notifyIfLineGesture(state)) {
            Log.w(TAG, "Recognized Line Gesture.");
        } else if (notifyIfRegionGesture(state, this.mMinRegionDiag)) {
            Log.w(TAG, "Recognized Region Gesture.");
        } else if (notifyIfLetterGesture(state)) {
            Log.w(TAG, "Recognized Letter Gesture.");
        } else if (notifyIfRegionGesture(state, this.mMinNonEnclosedRegionDiag)) {
            Log.w(TAG, "Recognized Region Gesture.");
        } else {
            Log.w(TAG, "stylus: Not a Region gesture or letter gesture");
            bdReportIfNeed(state);
        }
    }

    private boolean bdReportIfNeed(GestureState gestureState) {
        if (!gestureState.mIsStylus) {
            return true;
        }
        HwPCUtilsEx.bdReport(this.mContext, (int) TYPE_STYLUS_OPERATION_SUCCESSD, "false");
        return true;
    }

    private void debugPredictionInfo(List<Prediction> list, List<Prediction> list2) {
    }
}
