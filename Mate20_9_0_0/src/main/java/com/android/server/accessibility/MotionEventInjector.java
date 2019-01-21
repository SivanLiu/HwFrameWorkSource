package com.android.server.accessibility;

import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.GestureDescription.GestureStep;
import android.accessibilityservice.GestureDescription.TouchPoint;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import com.android.internal.os.SomeArgs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MotionEventInjector extends BaseEventStreamTransformation implements Callback {
    private static final int EVENT_BUTTON_STATE = 0;
    private static final int EVENT_DEVICE_ID = 0;
    private static final int EVENT_EDGE_FLAGS = 0;
    private static final int EVENT_FLAGS = 0;
    private static final int EVENT_META_STATE = 0;
    private static final int EVENT_SOURCE = 4098;
    private static final float EVENT_X_PRECISION = 1.0f;
    private static final float EVENT_Y_PRECISION = 1.0f;
    private static final String LOG_TAG = "MotionEventInjector";
    private static final int MESSAGE_INJECT_EVENTS = 2;
    private static final int MESSAGE_SEND_MOTION_EVENT = 1;
    private static PointerCoords[] sPointerCoords;
    private static PointerProperties[] sPointerProps;
    private long mDownTime;
    private final Handler mHandler;
    private boolean mIsDestroyed = false;
    private long mLastScheduledEventTime;
    private TouchPoint[] mLastTouchPoints;
    private int mNumLastTouchPoints;
    private final SparseArray<Boolean> mOpenGesturesInProgress = new SparseArray();
    private IntArray mSequencesInProgress = new IntArray(5);
    private IAccessibilityServiceClient mServiceInterfaceForCurrentGesture;
    private SparseIntArray mStrokeIdToPointerId = new SparseIntArray(5);

    public /* bridge */ /* synthetic */ EventStreamTransformation getNext() {
        return super.getNext();
    }

    public /* bridge */ /* synthetic */ void setNext(EventStreamTransformation eventStreamTransformation) {
        super.setNext(eventStreamTransformation);
    }

    public MotionEventInjector(Looper looper) {
        this.mHandler = new Handler(looper, this);
    }

    public MotionEventInjector(Handler handler) {
        this.mHandler = handler;
    }

    public void injectEvents(List<GestureStep> gestureSteps, IAccessibilityServiceClient serviceInterface, int sequence) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = gestureSteps;
        args.arg2 = serviceInterface;
        args.argi1 = sequence;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, args));
    }

    public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelAnyPendingInjectedEvents();
        sendMotionEventToNext(event, rawEvent, policyFlags);
    }

    public void clearEvents(int inputSource) {
        if (!this.mHandler.hasMessages(1)) {
            this.mOpenGesturesInProgress.put(inputSource, Boolean.valueOf(false));
        }
    }

    public void onDestroy() {
        cancelAnyPendingInjectedEvents();
        this.mIsDestroyed = true;
    }

    public boolean handleMessage(Message message) {
        if (message.what == 2) {
            SomeArgs args = message.obj;
            injectEventsMainThread((List) args.arg1, (IAccessibilityServiceClient) args.arg2, args.argi1);
            args.recycle();
            return true;
        } else if (message.what != 1) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown message: ");
            stringBuilder.append(message.what);
            Slog.e(str, stringBuilder.toString());
            return false;
        } else {
            MotionEvent motionEvent = message.obj;
            sendMotionEventToNext(motionEvent, motionEvent, 1073741824);
            if (message.arg1 != 0) {
                notifyService(this.mServiceInterfaceForCurrentGesture, this.mSequencesInProgress.get(0), true);
                this.mSequencesInProgress.remove(0);
            }
            return true;
        }
    }

    private void injectEventsMainThread(List<GestureStep> gestureSteps, IAccessibilityServiceClient serviceInterface, int sequence) {
        MotionEventInjector motionEventInjector = this;
        IAccessibilityServiceClient iAccessibilityServiceClient = serviceInterface;
        int i = sequence;
        if (motionEventInjector.mIsDestroyed) {
            try {
                iAccessibilityServiceClient.onPerformGestureResult(i, false);
            } catch (RemoteException re) {
                RemoteException remoteException = re;
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error sending status with mIsDestroyed to ");
                stringBuilder.append(iAccessibilityServiceClient);
                Slog.e(str, stringBuilder.toString(), re);
            }
        } else if (getNext() == null) {
            motionEventInjector.notifyService(iAccessibilityServiceClient, i, false);
        } else {
            boolean continuingGesture = newGestureTriesToContinueOldOne(gestureSteps);
            if (!continuingGesture || (iAccessibilityServiceClient == motionEventInjector.mServiceInterfaceForCurrentGesture && prepareToContinueOldGesture(gestureSteps))) {
                List<MotionEvent> events;
                if (!continuingGesture) {
                    cancelAnyPendingInjectedEvents();
                    motionEventInjector.cancelAnyGestureInProgress(4098);
                }
                motionEventInjector.mServiceInterfaceForCurrentGesture = iAccessibilityServiceClient;
                long currentTime = SystemClock.uptimeMillis();
                if (motionEventInjector.mSequencesInProgress.size() == 0) {
                    events = currentTime;
                } else {
                    events = motionEventInjector.mLastScheduledEventTime;
                }
                events = motionEventInjector.getMotionEventsFromGestureSteps(gestureSteps, events);
                if (events.isEmpty()) {
                    motionEventInjector.notifyService(iAccessibilityServiceClient, i, false);
                    return;
                }
                motionEventInjector.mSequencesInProgress.add(i);
                int i2 = 0;
                while (i2 < events.size()) {
                    MotionEvent event = (MotionEvent) events.get(i2);
                    Message message = motionEventInjector.mHandler.obtainMessage(1, i2 == events.size() - 1 ? 1 : 0, 0, event);
                    motionEventInjector.mLastScheduledEventTime = event.getEventTime();
                    boolean continuingGesture2 = continuingGesture;
                    motionEventInjector.mHandler.sendMessageDelayed(message, Math.max(0, event.getEventTime() - currentTime));
                    i2++;
                    continuingGesture = continuingGesture2;
                    motionEventInjector = this;
                }
                return;
            }
            cancelAnyPendingInjectedEvents();
            motionEventInjector.notifyService(iAccessibilityServiceClient, i, false);
        }
    }

    private boolean newGestureTriesToContinueOldOne(List<GestureStep> gestureSteps) {
        if (gestureSteps.isEmpty()) {
            return false;
        }
        GestureStep firstStep = (GestureStep) gestureSteps.get(0);
        for (int i = 0; i < firstStep.numTouchPoints; i++) {
            if (!firstStep.touchPoints[i].mIsStartOfPath) {
                return true;
            }
        }
        return false;
    }

    private boolean prepareToContinueOldGesture(List<GestureStep> gestureSteps) {
        boolean z = false;
        if (gestureSteps.isEmpty() || this.mLastTouchPoints == null || this.mNumLastTouchPoints == 0) {
            return false;
        }
        int i;
        GestureStep firstStep = (GestureStep) gestureSteps.get(0);
        int numContinuedStrokes = 0;
        for (i = 0; i < firstStep.numTouchPoints; i++) {
            TouchPoint touchPoint = firstStep.touchPoints[i];
            if (!touchPoint.mIsStartOfPath) {
                int continuedPointerId = this.mStrokeIdToPointerId.get(touchPoint.mContinuedStrokeId, -1);
                if (continuedPointerId == -1) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't continue gesture due to unknown continued stroke id in ");
                    stringBuilder.append(touchPoint);
                    Slog.w(str, stringBuilder.toString());
                    return false;
                }
                this.mStrokeIdToPointerId.put(touchPoint.mStrokeId, continuedPointerId);
                int lastPointIndex = findPointByStrokeId(this.mLastTouchPoints, this.mNumLastTouchPoints, touchPoint.mContinuedStrokeId);
                String str2;
                StringBuilder stringBuilder2;
                if (lastPointIndex < 0) {
                    str2 = LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Can't continue gesture due continued gesture id of ");
                    stringBuilder2.append(touchPoint);
                    stringBuilder2.append(" not matching any previous strokes in ");
                    stringBuilder2.append(Arrays.asList(this.mLastTouchPoints));
                    Slog.w(str2, stringBuilder2.toString());
                    return false;
                } else if (!this.mLastTouchPoints[lastPointIndex].mIsEndOfPath && this.mLastTouchPoints[lastPointIndex].mX == touchPoint.mX && this.mLastTouchPoints[lastPointIndex].mY == touchPoint.mY) {
                    this.mLastTouchPoints[lastPointIndex].mStrokeId = touchPoint.mStrokeId;
                } else {
                    str2 = LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Can't continue gesture due to points mismatch between ");
                    stringBuilder2.append(this.mLastTouchPoints[lastPointIndex]);
                    stringBuilder2.append(" and ");
                    stringBuilder2.append(touchPoint);
                    Slog.w(str2, stringBuilder2.toString());
                    return false;
                }
            }
            numContinuedStrokes++;
        }
        for (i = 0; i < this.mNumLastTouchPoints; i++) {
            if (!this.mLastTouchPoints[i].mIsEndOfPath) {
                numContinuedStrokes--;
            }
        }
        if (numContinuedStrokes == 0) {
            z = true;
        }
        return z;
    }

    private void sendMotionEventToNext(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (getNext() != null) {
            super.onMotionEvent(event, rawEvent, policyFlags);
            if (event.getActionMasked() == 0) {
                this.mOpenGesturesInProgress.put(event.getSource(), Boolean.valueOf(true));
            }
            if (event.getActionMasked() == 1 || event.getActionMasked() == 3) {
                this.mOpenGesturesInProgress.put(event.getSource(), Boolean.valueOf(false));
            }
        }
    }

    private void cancelAnyGestureInProgress(int source) {
        if (getNext() != null && ((Boolean) this.mOpenGesturesInProgress.get(source, Boolean.valueOf(false))).booleanValue()) {
            long now = SystemClock.uptimeMillis();
            MotionEvent cancelEvent = obtainMotionEvent(now, now, 3, getLastTouchPoints(), 1);
            sendMotionEventToNext(cancelEvent, cancelEvent, 1073741824);
            this.mOpenGesturesInProgress.put(source, Boolean.valueOf(false));
        }
    }

    private void cancelAnyPendingInjectedEvents() {
        if (this.mHandler.hasMessages(1)) {
            this.mHandler.removeMessages(1);
            cancelAnyGestureInProgress(4098);
            for (int i = this.mSequencesInProgress.size() - 1; i >= 0; i--) {
                notifyService(this.mServiceInterfaceForCurrentGesture, this.mSequencesInProgress.get(i), false);
                this.mSequencesInProgress.remove(i);
            }
        } else if (this.mNumLastTouchPoints != 0) {
            cancelAnyGestureInProgress(4098);
        }
        this.mNumLastTouchPoints = 0;
        this.mStrokeIdToPointerId.clear();
    }

    private void notifyService(IAccessibilityServiceClient service, int sequence, boolean success) {
        try {
            service.onPerformGestureResult(sequence, success);
        } catch (RemoteException re) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error sending motion event injection status to ");
            stringBuilder.append(this.mServiceInterfaceForCurrentGesture);
            Slog.e(str, stringBuilder.toString(), re);
        }
    }

    private List<MotionEvent> getMotionEventsFromGestureSteps(List<GestureStep> steps, long startTime) {
        List<MotionEvent> motionEvents = new ArrayList();
        TouchPoint[] lastTouchPoints = getLastTouchPoints();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= steps.size()) {
                return motionEvents;
            }
            GestureStep step = (GestureStep) steps.get(i2);
            int currentTouchPointSize = step.numTouchPoints;
            if (currentTouchPointSize > lastTouchPoints.length) {
                this.mNumLastTouchPoints = 0;
                motionEvents.clear();
                return motionEvents;
            }
            int i3 = currentTouchPointSize;
            appendMoveEventIfNeeded(motionEvents, step.touchPoints, i3, startTime + step.timeSinceGestureStart);
            appendUpEvents(motionEvents, step.touchPoints, i3, startTime + step.timeSinceGestureStart);
            appendDownEvents(motionEvents, step.touchPoints, i3, startTime + step.timeSinceGestureStart);
            i = i2 + 1;
        }
    }

    private TouchPoint[] getLastTouchPoints() {
        if (this.mLastTouchPoints == null) {
            int capacity = GestureDescription.getMaxStrokeCount();
            this.mLastTouchPoints = new TouchPoint[capacity];
            for (int i = 0; i < capacity; i++) {
                this.mLastTouchPoints[i] = new TouchPoint();
            }
        }
        return this.mLastTouchPoints;
    }

    private void appendMoveEventIfNeeded(List<MotionEvent> motionEvents, TouchPoint[] currentTouchPoints, int currentTouchPointsSize, long currentTime) {
        TouchPoint[] lastTouchPoints = getLastTouchPoints();
        boolean moveFound = false;
        for (int i = 0; i < currentTouchPointsSize; i++) {
            int lastPointsIndex = findPointByStrokeId(lastTouchPoints, this.mNumLastTouchPoints, currentTouchPoints[i].mStrokeId);
            if (lastPointsIndex >= 0) {
                boolean moveFound2 = (lastTouchPoints[lastPointsIndex].mX == currentTouchPoints[i].mX && lastTouchPoints[lastPointsIndex].mY == currentTouchPoints[i].mY) ? false : true;
                moveFound2 |= moveFound;
                lastTouchPoints[lastPointsIndex].copyFrom(currentTouchPoints[i]);
                moveFound = moveFound2;
            }
        }
        if (moveFound) {
            motionEvents.add(obtainMotionEvent(this.mDownTime, currentTime, 2, lastTouchPoints, this.mNumLastTouchPoints));
        } else {
            List<MotionEvent> list = motionEvents;
        }
    }

    private void appendUpEvents(List<MotionEvent> motionEvents, TouchPoint[] currentTouchPoints, int currentTouchPointsSize, long currentTime) {
        TouchPoint[] lastTouchPoints = getLastTouchPoints();
        int i = 0;
        while (true) {
            int i2 = i;
            List<MotionEvent> list;
            if (i2 < currentTouchPointsSize) {
                if (currentTouchPoints[i2].mIsEndOfPath) {
                    int indexOfUpEvent = findPointByStrokeId(lastTouchPoints, this.mNumLastTouchPoints, currentTouchPoints[i2].mStrokeId);
                    if (indexOfUpEvent >= 0) {
                        motionEvents.add(obtainMotionEvent(this.mDownTime, currentTime, (this.mNumLastTouchPoints == 1 ? 1 : 6) | (indexOfUpEvent << 8), lastTouchPoints, this.mNumLastTouchPoints));
                        for (i = indexOfUpEvent; i < this.mNumLastTouchPoints - 1; i++) {
                            lastTouchPoints[i].copyFrom(this.mLastTouchPoints[i + 1]);
                        }
                        this.mNumLastTouchPoints--;
                        if (this.mNumLastTouchPoints == 0) {
                            this.mStrokeIdToPointerId.clear();
                        }
                        i = i2 + 1;
                    }
                }
                list = motionEvents;
                i = i2 + 1;
            } else {
                list = motionEvents;
                return;
            }
        }
    }

    private void appendDownEvents(List<MotionEvent> motionEvents, TouchPoint[] currentTouchPoints, int currentTouchPointsSize, long currentTime) {
        TouchPoint[] lastTouchPoints = getLastTouchPoints();
        int i = 0;
        while (true) {
            int i2 = i;
            long j;
            List<MotionEvent> list;
            if (i2 < currentTouchPointsSize) {
                if (currentTouchPoints[i2].mIsStartOfPath) {
                    i = this.mNumLastTouchPoints;
                    this.mNumLastTouchPoints = i + 1;
                    lastTouchPoints[i].copyFrom(currentTouchPoints[i2]);
                    i = this.mNumLastTouchPoints == 1 ? 0 : 5;
                    if (i == 0) {
                        j = currentTime;
                        this.mDownTime = j;
                    } else {
                        j = currentTime;
                    }
                    motionEvents.add(obtainMotionEvent(this.mDownTime, j, i | (i2 << 8), lastTouchPoints, this.mNumLastTouchPoints));
                } else {
                    list = motionEvents;
                    j = currentTime;
                }
                i = i2 + 1;
            } else {
                list = motionEvents;
                j = currentTime;
                return;
            }
        }
    }

    private MotionEvent obtainMotionEvent(long downTime, long eventTime, int action, TouchPoint[] touchPoints, int touchPointsSize) {
        int i;
        int i2 = touchPointsSize;
        if (sPointerCoords == null || sPointerCoords.length < i2) {
            sPointerCoords = new PointerCoords[i2];
            for (i = 0; i < i2; i++) {
                sPointerCoords[i] = new PointerCoords();
            }
        }
        if (sPointerProps == null || sPointerProps.length < i2) {
            sPointerProps = new PointerProperties[i2];
            for (i = 0; i < i2; i++) {
                sPointerProps[i] = new PointerProperties();
            }
        }
        for (i = 0; i < i2; i++) {
            int pointerId = this.mStrokeIdToPointerId.get(touchPoints[i].mStrokeId, -1);
            if (pointerId == -1) {
                pointerId = getUnusedPointerId();
                this.mStrokeIdToPointerId.put(touchPoints[i].mStrokeId, pointerId);
            }
            sPointerProps[i].id = pointerId;
            sPointerProps[i].toolType = 0;
            sPointerCoords[i].clear();
            sPointerCoords[i].pressure = 1.0f;
            sPointerCoords[i].size = 1.0f;
            sPointerCoords[i].x = touchPoints[i].mX;
            sPointerCoords[i].y = touchPoints[i].mY;
        }
        return MotionEvent.obtain(downTime, eventTime, action, i2, sPointerProps, sPointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0);
    }

    private static int findPointByStrokeId(TouchPoint[] touchPoints, int touchPointsSize, int strokeId) {
        for (int i = 0; i < touchPointsSize; i++) {
            if (touchPoints[i].mStrokeId == strokeId) {
                return i;
            }
        }
        return -1;
    }

    private int getUnusedPointerId() {
        int pointerId = 0;
        while (this.mStrokeIdToPointerId.indexOfValue(pointerId) >= 0) {
            pointerId++;
            if (pointerId >= 10) {
                return 10;
            }
        }
        return pointerId;
    }
}
