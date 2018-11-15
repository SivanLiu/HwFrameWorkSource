package com.android.uiautomator.core;

import android.app.UiAutomation.AccessibilityEventFilter;
import android.graphics.Point;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

class InteractionController {
    private static final boolean DEBUG = Log.isLoggable(LOG_TAG, 3);
    private static final String LOG_TAG = InteractionController.class.getSimpleName();
    private static final int MOTION_EVENT_INJECTION_DELAY_MILLIS = 5;
    private static final long REGULAR_CLICK_LENGTH = 100;
    private long mDownTime;
    private final KeyCharacterMap mKeyCharacterMap = KeyCharacterMap.load(-1);
    private final UiAutomatorBridge mUiAutomatorBridge;

    class EventCollectingPredicate implements AccessibilityEventFilter {
        List<AccessibilityEvent> mEventsList;
        int mMask;

        EventCollectingPredicate(int mask, List<AccessibilityEvent> events) {
            this.mMask = mask;
            this.mEventsList = events;
        }

        public boolean accept(AccessibilityEvent t) {
            if ((t.getEventType() & this.mMask) != 0) {
                this.mEventsList.add(AccessibilityEvent.obtain(t));
            }
            return InteractionController.DEBUG;
        }
    }

    class WaitForAllEventPredicate implements AccessibilityEventFilter {
        int mMask;

        WaitForAllEventPredicate(int mask) {
            this.mMask = mask;
        }

        public boolean accept(AccessibilityEvent t) {
            if ((t.getEventType() & this.mMask) == 0) {
                return InteractionController.DEBUG;
            }
            this.mMask &= ~t.getEventType();
            if (this.mMask != 0) {
                return InteractionController.DEBUG;
            }
            return true;
        }
    }

    class WaitForAnyEventPredicate implements AccessibilityEventFilter {
        int mMask;

        WaitForAnyEventPredicate(int mask) {
            this.mMask = mask;
        }

        public boolean accept(AccessibilityEvent t) {
            if ((t.getEventType() & this.mMask) != 0) {
                return true;
            }
            return InteractionController.DEBUG;
        }
    }

    public InteractionController(UiAutomatorBridge bridge) {
        this.mUiAutomatorBridge = bridge;
    }

    private AccessibilityEvent runAndWaitForEvents(Runnable command, AccessibilityEventFilter filter, long timeout) {
        try {
            return this.mUiAutomatorBridge.executeCommandAndWaitForAccessibilityEvent(command, filter, timeout);
        } catch (TimeoutException e) {
            Log.w(LOG_TAG, "runAndwaitForEvent timedout waiting for events");
            return null;
        } catch (Exception e2) {
            Log.e(LOG_TAG, "exception from executeCommandAndWaitForAccessibilityEvent", e2);
            return null;
        }
    }

    public boolean sendKeyAndWaitForEvent(final int keyCode, final int metaState, int eventType, long timeout) {
        return runAndWaitForEvents(new Runnable() {
            public void run() {
                long eventTime = SystemClock.uptimeMillis();
                KeyEvent downEvent = new KeyEvent(eventTime, eventTime, 0, keyCode, 0, metaState, -1, 0, 0, 257);
                if (InteractionController.this.injectEventSync(downEvent)) {
                    InteractionController.this.injectEventSync(new KeyEvent(eventTime, eventTime, 1, keyCode, 0, metaState, -1, 0, 0, 257));
                    return;
                }
            }
        }, new WaitForAnyEventPredicate(eventType), timeout) != null ? true : DEBUG;
    }

    public boolean clickNoSync(int x, int y) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("clickNoSync (");
        stringBuilder.append(x);
        stringBuilder.append(", ");
        stringBuilder.append(y);
        stringBuilder.append(")");
        Log.d(str, stringBuilder.toString());
        if (touchDown(x, y)) {
            SystemClock.sleep(REGULAR_CLICK_LENGTH);
            if (touchUp(x, y)) {
                return true;
            }
        }
        return DEBUG;
    }

    public boolean clickAndSync(int x, int y, long timeout) {
        Log.d(LOG_TAG, String.format("clickAndSync(%d, %d)", new Object[]{Integer.valueOf(x), Integer.valueOf(y)}));
        if (runAndWaitForEvents(clickRunnable(x, y), new WaitForAnyEventPredicate(2052), timeout) != null) {
            return true;
        }
        return DEBUG;
    }

    public boolean clickAndWaitForNewWindow(int x, int y, long timeout) {
        Log.d(LOG_TAG, String.format("clickAndWaitForNewWindow(%d, %d)", new Object[]{Integer.valueOf(x), Integer.valueOf(y)}));
        if (runAndWaitForEvents(clickRunnable(x, y), new WaitForAllEventPredicate(2080), timeout) != null) {
            return true;
        }
        return DEBUG;
    }

    private Runnable clickRunnable(final int x, final int y) {
        return new Runnable() {
            public void run() {
                if (InteractionController.this.touchDown(x, y)) {
                    SystemClock.sleep(InteractionController.REGULAR_CLICK_LENGTH);
                    InteractionController.this.touchUp(x, y);
                }
            }
        };
    }

    public boolean longTapNoSync(int x, int y) {
        if (DEBUG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("longTapNoSync (");
            stringBuilder.append(x);
            stringBuilder.append(", ");
            stringBuilder.append(y);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        if (touchDown(x, y)) {
            SystemClock.sleep(this.mUiAutomatorBridge.getSystemLongPressTime());
            if (touchUp(x, y)) {
                return true;
            }
        }
        return DEBUG;
    }

    private boolean touchDown(int x, int y) {
        if (DEBUG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("touchDown (");
            stringBuilder.append(x);
            stringBuilder.append(", ");
            stringBuilder.append(y);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        this.mDownTime = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(this.mDownTime, this.mDownTime, 0, (float) x, (float) y, 1);
        event.setSource(4098);
        return injectEventSync(event);
    }

    private boolean touchUp(int x, int y) {
        if (DEBUG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("touchUp (");
            stringBuilder.append(x);
            stringBuilder.append(", ");
            stringBuilder.append(y);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        MotionEvent event = MotionEvent.obtain(this.mDownTime, SystemClock.uptimeMillis(), 1, (float) x, (float) y, 1);
        event.setSource(4098);
        this.mDownTime = 0;
        return injectEventSync(event);
    }

    private boolean touchMove(int x, int y) {
        if (DEBUG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("touchMove (");
            stringBuilder.append(x);
            stringBuilder.append(", ");
            stringBuilder.append(y);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        MotionEvent event = MotionEvent.obtain(this.mDownTime, SystemClock.uptimeMillis(), 2, (float) x, (float) y, 1);
        event.setSource(4098);
        return injectEventSync(event);
    }

    public boolean scrollSwipe(int downX, int downY, int upX, int upY, int steps) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("scrollSwipe (");
        stringBuilder.append(downX);
        stringBuilder.append(", ");
        stringBuilder.append(downY);
        stringBuilder.append(", ");
        stringBuilder.append(upX);
        stringBuilder.append(", ");
        stringBuilder.append(upY);
        stringBuilder.append(", ");
        stringBuilder.append(steps);
        stringBuilder.append(")");
        Log.d(str, stringBuilder.toString());
        final int i = downX;
        final int i2 = downY;
        final int i3 = upX;
        final int i4 = upY;
        final int i5 = steps;
        Runnable anonymousClass3 = new Runnable() {
            public void run() {
                InteractionController.this.swipe(i, i2, i3, i4, i5);
            }
        };
        ArrayList<AccessibilityEvent> events = new ArrayList();
        runAndWaitForEvents(anonymousClass3, new EventCollectingPredicate(4096, events), Configurator.getInstance().getScrollAcknowledgmentTimeout());
        AccessibilityEvent event = getLastMatchingEvent(events, 4096);
        boolean z = DEBUG;
        if (event == null) {
            recycleAccessibilityEvents(events);
            return DEBUG;
        }
        boolean foundEnd = DEBUG;
        boolean z2;
        String str2;
        StringBuilder stringBuilder2;
        if (event.getFromIndex() != -1 && event.getToIndex() != -1 && event.getItemCount() != -1) {
            z2 = (event.getFromIndex() == 0 || event.getItemCount() - 1 == event.getToIndex()) ? true : DEBUG;
            foundEnd = z2;
            str2 = LOG_TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("scrollSwipe reached scroll end: ");
            stringBuilder2.append(foundEnd);
            Log.d(str2, stringBuilder2.toString());
        } else if (!(event.getScrollX() == -1 || event.getScrollY() == -1)) {
            if (downX == upX) {
                z2 = (event.getScrollY() == 0 || event.getScrollY() == event.getMaxScrollY()) ? true : DEBUG;
                foundEnd = z2;
                str2 = LOG_TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Vertical scrollSwipe reached scroll end: ");
                stringBuilder2.append(foundEnd);
                Log.d(str2, stringBuilder2.toString());
            } else if (downY == upY) {
                z2 = (event.getScrollX() == 0 || event.getScrollX() == event.getMaxScrollX()) ? true : DEBUG;
                foundEnd = z2;
                str2 = LOG_TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Horizontal scrollSwipe reached scroll end: ");
                stringBuilder2.append(foundEnd);
                Log.d(str2, stringBuilder2.toString());
            }
        }
        recycleAccessibilityEvents(events);
        if (!foundEnd) {
            z = true;
        }
        return z;
    }

    private AccessibilityEvent getLastMatchingEvent(List<AccessibilityEvent> events, int type) {
        for (int x = events.size(); x > 0; x--) {
            AccessibilityEvent event = (AccessibilityEvent) events.get(x - 1);
            if (event.getEventType() == type) {
                return event;
            }
        }
        return null;
    }

    private void recycleAccessibilityEvents(List<AccessibilityEvent> events) {
        for (AccessibilityEvent event : events) {
            event.recycle();
        }
        events.clear();
    }

    public boolean swipe(int downX, int downY, int upX, int upY, int steps) {
        return swipe(downX, downY, upX, upY, steps, DEBUG);
    }

    public boolean swipe(int downX, int downY, int upX, int upY, int steps, boolean drag) {
        int i = upX;
        int i2 = upY;
        int swipeSteps = steps;
        if (swipeSteps == 0) {
            swipeSteps = 1;
        }
        double xStep = ((double) (i - downX)) / ((double) swipeSteps);
        double yStep = ((double) (i2 - downY)) / ((double) swipeSteps);
        boolean ret = touchDown(downX, downY);
        if (drag) {
            SystemClock.sleep(this.mUiAutomatorBridge.getSystemLongPressTime());
        }
        for (int i3 = 1; i3 < swipeSteps; i3++) {
            ret &= touchMove(((int) (((double) i3) * xStep)) + downX, ((int) (((double) i3) * yStep)) + downY);
            if (!ret) {
                break;
            }
            SystemClock.sleep(5);
        }
        if (drag) {
            SystemClock.sleep(REGULAR_CLICK_LENGTH);
        }
        return ret & touchUp(i, i2);
    }

    public boolean swipe(Point[] segments, int segmentSteps) {
        int swipeSteps = segmentSteps;
        if (segmentSteps == 0) {
            segmentSteps = 1;
        }
        int seg = 0;
        if (segments.length == 0) {
            return DEBUG;
        }
        boolean ret = touchDown(segments[0].x, segments[0].y);
        while (true) {
            int seg2 = seg;
            int i = 1;
            if (seg2 >= segments.length) {
                return ret & touchUp(segments[segments.length - 1].x, segments[segments.length - 1].y);
            }
            if (seg2 + 1 < segments.length) {
                double d = ((double) (segments[seg2 + 1].x - segments[seg2].x)) / ((double) segmentSteps);
                double xStep = ((double) (segments[seg2 + 1].y - segments[seg2].y)) / ((double) segmentSteps);
                while (true) {
                    int i2 = i;
                    if (i2 >= swipeSteps) {
                        break;
                    }
                    ret &= touchMove(segments[seg2].x + ((int) (((double) i2) * d)), segments[seg2].y + ((int) (((double) i2) * xStep)));
                    if (!ret) {
                        break;
                    }
                    SystemClock.sleep(5);
                    i = i2 + 1;
                }
                double yStep = xStep;
                xStep = d;
            }
            seg = seg2 + 1;
        }
    }

    public boolean sendText(String text) {
        if (DEBUG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendText (");
            stringBuilder.append(text);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        KeyEvent[] events = this.mKeyCharacterMap.getEvents(text.toCharArray());
        if (events != null) {
            long keyDelay = Configurator.getInstance().getKeyInjectionDelay();
            for (KeyEvent event2 : events) {
                if (!injectEventSync(KeyEvent.changeTimeRepeat(event2, SystemClock.uptimeMillis(), 0))) {
                    return DEBUG;
                }
                SystemClock.sleep(keyDelay);
            }
        }
        return true;
    }

    public boolean sendKey(int keyCode, int metaState) {
        int i;
        int i2;
        if (DEBUG) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendKey (");
            i = keyCode;
            stringBuilder.append(i);
            stringBuilder.append(", ");
            i2 = metaState;
            stringBuilder.append(i2);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        } else {
            i = keyCode;
            i2 = metaState;
        }
        long eventTime = SystemClock.uptimeMillis();
        KeyEvent downEvent = new KeyEvent(eventTime, eventTime, 0, i, 0, i2, -1, 0, 0, 257);
        if (injectEventSync(downEvent)) {
            if (injectEventSync(new KeyEvent(eventTime, eventTime, 1, i, 0, metaState, -1, 0, 0, 257))) {
                return true;
            }
        }
        return DEBUG;
    }

    public void setRotationRight() {
        this.mUiAutomatorBridge.setRotation(3);
    }

    public void setRotationLeft() {
        this.mUiAutomatorBridge.setRotation(1);
    }

    public void setRotationNatural() {
        this.mUiAutomatorBridge.setRotation(0);
    }

    public void freezeRotation() {
        this.mUiAutomatorBridge.setRotation(-1);
    }

    public void unfreezeRotation() {
        this.mUiAutomatorBridge.setRotation(-2);
    }

    public boolean wakeDevice() throws RemoteException {
        if (isScreenOn()) {
            return DEBUG;
        }
        sendKey(26, 0);
        return true;
    }

    public boolean sleepDevice() throws RemoteException {
        if (!isScreenOn()) {
            return DEBUG;
        }
        sendKey(26, 0);
        return true;
    }

    public boolean isScreenOn() throws RemoteException {
        return this.mUiAutomatorBridge.isScreenOn();
    }

    private boolean injectEventSync(InputEvent event) {
        return this.mUiAutomatorBridge.injectInputEvent(event, true);
    }

    private int getPointerAction(int motionEnvent, int index) {
        return (index << 8) + motionEnvent;
    }

    public boolean performMultiPointerGesture(PointerCoords[]... touches) {
        PointerCoords[][] pointerCoordsArr = touches;
        if (pointerCoordsArr.length >= 2) {
            int x;
            MotionEvent motionEvent;
            int maxSteps = 0;
            for (int x2 = 0; x2 < pointerCoordsArr.length; x2++) {
                maxSteps = maxSteps < pointerCoordsArr[x2].length ? pointerCoordsArr[x2].length : maxSteps;
            }
            PointerProperties[] properties = new PointerProperties[pointerCoordsArr.length];
            PointerCoords[] pointerCoords = new PointerCoords[pointerCoordsArr.length];
            for (x = 0; x < pointerCoordsArr.length; x++) {
                PointerProperties prop = new PointerProperties();
                prop.id = x;
                prop.toolType = 1;
                properties[x] = prop;
                pointerCoords[x] = pointerCoordsArr[x][0];
            }
            long downTime = SystemClock.uptimeMillis();
            int x3 = 1;
            PointerCoords[] pointerCoords2 = pointerCoords;
            MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), 0, 1, properties, pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0);
            boolean ret = true & injectEventSync(event);
            int x4 = x3;
            while (x4 < pointerCoordsArr.length) {
                event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), getPointerAction(MOTION_EVENT_INJECTION_DELAY_MILLIS, x4), x4 + 1, properties, pointerCoords2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0);
                ret &= injectEventSync(event);
                x4++;
                motionEvent = event;
            }
            x4 = x3;
            while (x4 < maxSteps - 1) {
                for (x = 0; x < pointerCoordsArr.length; x++) {
                    if (pointerCoordsArr[x].length > x4) {
                        pointerCoords2[x] = pointerCoordsArr[x][x4];
                    } else {
                        pointerCoords2[x] = pointerCoordsArr[x][pointerCoordsArr[x].length - 1];
                    }
                }
                event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), 2, pointerCoordsArr.length, properties, pointerCoords2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0);
                ret &= injectEventSync(event);
                SystemClock.sleep(5);
                x4++;
                motionEvent = event;
            }
            for (x4 = 0; x4 < pointerCoordsArr.length; x4++) {
                pointerCoords2[x4] = pointerCoordsArr[x4][pointerCoordsArr[x4].length - 1];
            }
            while (true) {
                x4 = x3;
                if (x4 < pointerCoordsArr.length) {
                    event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), getPointerAction(6, x4), x4 + 1, properties, pointerCoords2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0);
                    ret &= injectEventSync(event);
                    x3 = x4 + 1;
                    motionEvent = event;
                } else {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("x ");
                    stringBuilder.append(pointerCoords2[0].x);
                    Log.i(str, stringBuilder.toString());
                    return ret & injectEventSync(MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), 1, 1, properties, pointerCoords2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0));
                }
            }
        }
        throw new IllegalArgumentException("Must provide coordinates for at least 2 pointers");
    }

    public boolean toggleRecentApps() {
        return this.mUiAutomatorBridge.performGlobalAction(3);
    }

    public boolean openNotification() {
        return this.mUiAutomatorBridge.performGlobalAction(4);
    }

    public boolean openQuickSettings() {
        return this.mUiAutomatorBridge.performGlobalAction(MOTION_EVENT_INJECTION_DELAY_MILLIS);
    }
}
