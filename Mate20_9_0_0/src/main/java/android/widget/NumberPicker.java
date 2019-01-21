package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import libcore.icu.LocaleData;

public class NumberPicker extends LinearLayout {
    private static final int DEFAULT_LAYOUT_RESOURCE_ID = 17367208;
    private static final long DEFAULT_LONG_PRESS_UPDATE_INTERVAL = 300;
    private static final char[] DIGIT_CHARACTERS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 1632, 1633, 1634, 1635, 1636, 1637, 1638, 1639, 1640, 1641, 1776, 1777, 1778, 1779, 1780, 1781, 1782, 1783, 1784, 1785, 2406, 2407, 2408, 2409, 2410, 2411, 2412, 2413, 2414, 2415, 2534, 2535, 2536, 2537, 2538, 2539, 2540, 2541, 2542, 2543, 3302, 3303, 3304, 3305, 3306, 3307, 3308, 3309, 3310, 3311};
    private static final int SELECTOR_ADJUSTMENT_DURATION_MILLIS = 800;
    private static final int SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 8;
    private static final int SELECTOR_WHEEL_ITEM_COUNT = 3;
    private static final int SIZE_UNSPECIFIED = -1;
    private static final int SNAP_SCROLL_DURATION = 300;
    private static final float TOP_AND_BOTTOM_FADING_EDGE_STRENGTH = 0.9f;
    private static final int UNSCALED_DEFAULT_SELECTION_DIVIDERS_DISTANCE = 48;
    private static final int UNSCALED_DEFAULT_SELECTION_DIVIDER_HEIGHT = 2;
    private static final TwoDigitFormatter sTwoDigitFormatter = new TwoDigitFormatter();
    private int SELECTOR_MIDDLE_ITEM_INDEX;
    private AccessibilityNodeProviderImpl mAccessibilityNodeProvider;
    private final Scroller mAdjustScroller;
    private BeginSoftInputOnLongPressCommand mBeginSoftInputOnLongPressCommand;
    private int mBottomSelectionDividerBottom;
    private ChangeCurrentByOneFromLongPressCommand mChangeCurrentByOneFromLongPressCommand;
    private final boolean mComputeMaxWidth;
    private int mCurrentScrollOffset;
    private final ImageButton mDecrementButton;
    private boolean mDecrementVirtualButtonPressed;
    private String[] mDisplayedValues;
    private final Scroller mFlingScroller;
    private Formatter mFormatter;
    private final boolean mHasSelectorWheel;
    private boolean mHideWheelUntilFocused;
    private boolean mIgnoreMoveEvents;
    private final ImageButton mIncrementButton;
    private boolean mIncrementVirtualButtonPressed;
    private int mInitialScrollOffset;
    private final EditText mInputText;
    private long mLastDownEventTime;
    private float mLastDownEventY;
    private float mLastDownOrMoveEventY;
    private int mLastHandledDownDpadKeyCode;
    private int mLastHoveredChildVirtualViewId;
    private long mLongPressUpdateInterval;
    private final int mMaxHeight;
    private int mMaxValue;
    private int mMaxWidth;
    private int mMaximumFlingVelocity;
    private final int mMinHeight;
    private int mMinValue;
    private final int mMinWidth;
    private int mMinimumFlingVelocity;
    private OnScrollListener mOnScrollListener;
    private OnValueChangeListener mOnValueChangeListener;
    private boolean mPerformClickOnTap;
    private final PressedStateHelper mPressedStateHelper;
    private int mPreviousScrollerY;
    private int mScrollState;
    private final Drawable mSelectionDivider;
    private final int mSelectionDividerHeight;
    private final int mSelectionDividersDistance;
    private int mSelectorElementHeight;
    private final SparseArray<String> mSelectorIndexToStringCache;
    private int[] mSelectorIndices;
    private int mSelectorTextGapHeight;
    protected int mSelectorWheelItemCount;
    private final Paint mSelectorWheelPaint;
    private SetSelectionCommand mSetSelectionCommand;
    private final int mSolidColor;
    private final int mTextSize;
    private int mTopSelectionDividerTop;
    private int mTouchSlop;
    private int mValue;
    private VelocityTracker mVelocityTracker;
    private final Drawable mVirtualButtonPressedDrawable;
    private boolean mWrapSelectorWheel;
    private boolean mWrapSelectorWheelPreferred;

    class BeginSoftInputOnLongPressCommand implements Runnable {
        BeginSoftInputOnLongPressCommand() {
        }

        public void run() {
            NumberPicker.this.performLongClick();
        }
    }

    class ChangeCurrentByOneFromLongPressCommand implements Runnable {
        private boolean mIncrement;

        ChangeCurrentByOneFromLongPressCommand() {
        }

        private void setStep(boolean increment) {
            this.mIncrement = increment;
        }

        public void run() {
            NumberPicker.this.changeValueByOne(this.mIncrement);
            NumberPicker.this.postDelayed(this, NumberPicker.this.mLongPressUpdateInterval);
        }
    }

    public interface Formatter {
        String format(int i);
    }

    public interface OnScrollListener {
        public static final int SCROLL_STATE_FLING = 2;
        public static final int SCROLL_STATE_IDLE = 0;
        public static final int SCROLL_STATE_TOUCH_SCROLL = 1;

        @Retention(RetentionPolicy.SOURCE)
        public @interface ScrollState {
        }

        void onScrollStateChange(NumberPicker numberPicker, int i);
    }

    public interface OnValueChangeListener {
        void onValueChange(NumberPicker numberPicker, int i, int i2);
    }

    class PressedStateHelper implements Runnable {
        public static final int BUTTON_DECREMENT = 2;
        public static final int BUTTON_INCREMENT = 1;
        private final int MODE_PRESS = 1;
        private final int MODE_TAPPED = 2;
        private int mManagedButton;
        private int mMode;

        PressedStateHelper() {
        }

        public void cancel() {
            this.mMode = 0;
            this.mManagedButton = 0;
            NumberPicker.this.removeCallbacks(this);
            if (NumberPicker.this.mIncrementVirtualButtonPressed) {
                NumberPicker.this.mIncrementVirtualButtonPressed = false;
                NumberPicker.this.invalidate(0, NumberPicker.this.mBottomSelectionDividerBottom, NumberPicker.this.mRight, NumberPicker.this.mBottom);
            }
            NumberPicker.this.mDecrementVirtualButtonPressed = false;
            if (NumberPicker.this.mDecrementVirtualButtonPressed) {
                NumberPicker.this.invalidate(0, 0, NumberPicker.this.mRight, NumberPicker.this.mTopSelectionDividerTop);
            }
        }

        public void buttonPressDelayed(int button) {
            cancel();
            this.mMode = 1;
            this.mManagedButton = button;
            NumberPicker.this.postDelayed(this, (long) ViewConfiguration.getTapTimeout());
        }

        public void buttonTapped(int button) {
            cancel();
            this.mMode = 2;
            this.mManagedButton = button;
            NumberPicker.this.post(this);
        }

        public void run() {
            switch (this.mMode) {
                case 1:
                    switch (this.mManagedButton) {
                        case 1:
                            NumberPicker.this.mIncrementVirtualButtonPressed = true;
                            NumberPicker.this.invalidate(0, NumberPicker.this.mBottomSelectionDividerBottom, NumberPicker.this.mRight, NumberPicker.this.mBottom);
                            return;
                        case 2:
                            NumberPicker.this.mDecrementVirtualButtonPressed = true;
                            NumberPicker.this.invalidate(0, 0, NumberPicker.this.mRight, NumberPicker.this.mTopSelectionDividerTop);
                            return;
                        default:
                            return;
                    }
                case 2:
                    switch (this.mManagedButton) {
                        case 1:
                            if (!NumberPicker.this.mIncrementVirtualButtonPressed) {
                                NumberPicker.this.postDelayed(this, (long) ViewConfiguration.getPressedStateDuration());
                            }
                            NumberPicker.access$1280(NumberPicker.this, 1);
                            NumberPicker.this.invalidate(0, NumberPicker.this.mBottomSelectionDividerBottom, NumberPicker.this.mRight, NumberPicker.this.mBottom);
                            return;
                        case 2:
                            if (!NumberPicker.this.mDecrementVirtualButtonPressed) {
                                NumberPicker.this.postDelayed(this, (long) ViewConfiguration.getPressedStateDuration());
                            }
                            NumberPicker.access$1680(NumberPicker.this, 1);
                            NumberPicker.this.invalidate(0, 0, NumberPicker.this.mRight, NumberPicker.this.mTopSelectionDividerTop);
                            return;
                        default:
                            return;
                    }
                default:
                    return;
            }
        }
    }

    private static class SetSelectionCommand implements Runnable {
        private final EditText mInputText;
        private boolean mPosted;
        private int mSelectionEnd;
        private int mSelectionStart;

        public SetSelectionCommand(EditText inputText) {
            this.mInputText = inputText;
        }

        public void post(int selectionStart, int selectionEnd) {
            this.mSelectionStart = selectionStart;
            this.mSelectionEnd = selectionEnd;
            if (!this.mPosted) {
                this.mInputText.post(this);
                this.mPosted = true;
            }
        }

        public void cancel() {
            if (this.mPosted) {
                this.mInputText.removeCallbacks(this);
                this.mPosted = false;
            }
        }

        public void run() {
            this.mPosted = false;
            try {
                this.mInputText.setSelection(this.mSelectionStart, this.mSelectionEnd);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    class AccessibilityNodeProviderImpl extends AccessibilityNodeProvider {
        private static final int UNDEFINED = Integer.MIN_VALUE;
        private static final int VIRTUAL_VIEW_ID_DECREMENT = 3;
        private static final int VIRTUAL_VIEW_ID_INCREMENT = 1;
        private static final int VIRTUAL_VIEW_ID_INPUT = 2;
        private int mAccessibilityFocusedView = Integer.MIN_VALUE;
        private final int[] mTempArray = new int[2];
        private final Rect mTempRect = new Rect();

        AccessibilityNodeProviderImpl() {
        }

        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            if (virtualViewId == -1) {
                return createAccessibilityNodeInfoForNumberPicker(NumberPicker.this.mScrollX, NumberPicker.this.mScrollY, NumberPicker.this.mScrollX + (NumberPicker.this.mRight - NumberPicker.this.mLeft), NumberPicker.this.mScrollY + (NumberPicker.this.mBottom - NumberPicker.this.mTop));
            }
            switch (virtualViewId) {
                case 1:
                    return createAccessibilityNodeInfoForVirtualButton(1, getVirtualIncrementButtonText(), NumberPicker.this.mScrollX, NumberPicker.this.mBottomSelectionDividerBottom - NumberPicker.this.mSelectionDividerHeight, NumberPicker.this.mScrollX + (NumberPicker.this.mRight - NumberPicker.this.mLeft), NumberPicker.this.mScrollY + (NumberPicker.this.mBottom - NumberPicker.this.mTop));
                case 2:
                    return createAccessibiltyNodeInfoForInputText(NumberPicker.this.mScrollX, NumberPicker.this.mTopSelectionDividerTop + NumberPicker.this.mSelectionDividerHeight, NumberPicker.this.mScrollX + (NumberPicker.this.mRight - NumberPicker.this.mLeft), NumberPicker.this.mBottomSelectionDividerBottom - NumberPicker.this.mSelectionDividerHeight);
                case 3:
                    return createAccessibilityNodeInfoForVirtualButton(3, getVirtualDecrementButtonText(), NumberPicker.this.mScrollX, NumberPicker.this.mScrollY, NumberPicker.this.mScrollX + (NumberPicker.this.mRight - NumberPicker.this.mLeft), NumberPicker.this.mTopSelectionDividerTop + NumberPicker.this.mSelectionDividerHeight);
                default:
                    return super.createAccessibilityNodeInfo(virtualViewId);
            }
        }

        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String searched, int virtualViewId) {
            if (TextUtils.isEmpty(searched)) {
                return Collections.emptyList();
            }
            String searchedLowerCase = searched.toLowerCase();
            List<AccessibilityNodeInfo> result = new ArrayList();
            if (virtualViewId != -1) {
                switch (virtualViewId) {
                    case 1:
                    case 2:
                    case 3:
                        findAccessibilityNodeInfosByTextInChild(searchedLowerCase, virtualViewId, result);
                        return result;
                    default:
                        return super.findAccessibilityNodeInfosByText(searched, virtualViewId);
                }
            }
            findAccessibilityNodeInfosByTextInChild(searchedLowerCase, 3, result);
            findAccessibilityNodeInfosByTextInChild(searchedLowerCase, 2, result);
            findAccessibilityNodeInfosByTextInChild(searchedLowerCase, 1, result);
            return result;
        }

        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            boolean z = false;
            if (virtualViewId != -1) {
                switch (virtualViewId) {
                    case 1:
                        if (action != 16) {
                            if (action != 64) {
                                if (action != 128 || this.mAccessibilityFocusedView != virtualViewId) {
                                    return false;
                                }
                                this.mAccessibilityFocusedView = Integer.MIN_VALUE;
                                sendAccessibilityEventForVirtualView(virtualViewId, 65536);
                                NumberPicker.this.invalidate(0, NumberPicker.this.mBottomSelectionDividerBottom, NumberPicker.this.mRight, NumberPicker.this.mBottom);
                                return true;
                            } else if (this.mAccessibilityFocusedView == virtualViewId) {
                                return false;
                            } else {
                                this.mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, 32768);
                                NumberPicker.this.invalidate(0, NumberPicker.this.mBottomSelectionDividerBottom, NumberPicker.this.mRight, NumberPicker.this.mBottom);
                                return true;
                            }
                        } else if (!NumberPicker.this.isEnabled()) {
                            return false;
                        } else {
                            NumberPicker.this.changeValueByOne(true);
                            sendAccessibilityEventForVirtualView(virtualViewId, 1);
                            return true;
                        }
                    case 2:
                        if (action != 16) {
                            if (action != 32) {
                                if (action != 64) {
                                    if (action != 128) {
                                        switch (action) {
                                            case 1:
                                                if (!NumberPicker.this.isEnabled() || NumberPicker.this.mInputText.isFocused()) {
                                                    return false;
                                                }
                                                return NumberPicker.this.mInputText.requestFocus();
                                            case 2:
                                                if (!NumberPicker.this.isEnabled() || !NumberPicker.this.mInputText.isFocused()) {
                                                    return false;
                                                }
                                                NumberPicker.this.mInputText.clearFocus();
                                                return true;
                                            default:
                                                return NumberPicker.this.mInputText.performAccessibilityAction(action, arguments);
                                        }
                                    } else if (this.mAccessibilityFocusedView != virtualViewId) {
                                        return false;
                                    } else {
                                        this.mAccessibilityFocusedView = Integer.MIN_VALUE;
                                        sendAccessibilityEventForVirtualView(virtualViewId, 65536);
                                        NumberPicker.this.mInputText.invalidate();
                                        return true;
                                    }
                                } else if (this.mAccessibilityFocusedView == virtualViewId) {
                                    return false;
                                } else {
                                    this.mAccessibilityFocusedView = virtualViewId;
                                    sendAccessibilityEventForVirtualView(virtualViewId, 32768);
                                    NumberPicker.this.mInputText.invalidate();
                                    return true;
                                }
                            } else if (!NumberPicker.this.isEnabled()) {
                                return false;
                            } else {
                                NumberPicker.this.performLongClick();
                                return true;
                            }
                        } else if (!NumberPicker.this.isEnabled()) {
                            return false;
                        } else {
                            NumberPicker.this.performClick();
                            return true;
                        }
                    case 3:
                        if (action != 16) {
                            if (action != 64) {
                                if (action != 128 || this.mAccessibilityFocusedView != virtualViewId) {
                                    return false;
                                }
                                this.mAccessibilityFocusedView = Integer.MIN_VALUE;
                                sendAccessibilityEventForVirtualView(virtualViewId, 65536);
                                NumberPicker.this.invalidate(0, 0, NumberPicker.this.mRight, NumberPicker.this.mTopSelectionDividerTop);
                                return true;
                            } else if (this.mAccessibilityFocusedView == virtualViewId) {
                                return false;
                            } else {
                                this.mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, 32768);
                                NumberPicker.this.invalidate(0, 0, NumberPicker.this.mRight, NumberPicker.this.mTopSelectionDividerTop);
                                return true;
                            }
                        } else if (!NumberPicker.this.isEnabled()) {
                            return false;
                        } else {
                            if (virtualViewId == 1) {
                                z = true;
                            }
                            NumberPicker.this.changeValueByOne(z);
                            sendAccessibilityEventForVirtualView(virtualViewId, 1);
                            return true;
                        }
                }
            } else if (action != 64) {
                if (action != 128) {
                    if (action != 4096) {
                        if (action == 8192) {
                            if (!NumberPicker.this.isEnabled() || (!NumberPicker.this.getWrapSelectorWheel() && NumberPicker.this.getValue() <= NumberPicker.this.getMinValue())) {
                                return false;
                            }
                            NumberPicker.this.changeValueByOne(false);
                            return true;
                        }
                    } else if (!NumberPicker.this.isEnabled() || (!NumberPicker.this.getWrapSelectorWheel() && NumberPicker.this.getValue() >= NumberPicker.this.getMaxValue())) {
                        return false;
                    } else {
                        NumberPicker.this.changeValueByOne(true);
                        return true;
                    }
                } else if (this.mAccessibilityFocusedView != virtualViewId) {
                    return false;
                } else {
                    this.mAccessibilityFocusedView = Integer.MIN_VALUE;
                    NumberPicker.this.clearAccessibilityFocus();
                    return true;
                }
            } else if (this.mAccessibilityFocusedView == virtualViewId) {
                return false;
            } else {
                this.mAccessibilityFocusedView = virtualViewId;
                NumberPicker.this.requestAccessibilityFocus();
                return true;
            }
            return super.performAction(virtualViewId, action, arguments);
        }

        public void sendAccessibilityEventForVirtualView(int virtualViewId, int eventType) {
            switch (virtualViewId) {
                case 1:
                    if (hasVirtualIncrementButton()) {
                        sendAccessibilityEventForVirtualButton(virtualViewId, eventType, getVirtualIncrementButtonText());
                        return;
                    }
                    return;
                case 2:
                    sendAccessibilityEventForVirtualText(eventType);
                    return;
                case 3:
                    if (hasVirtualDecrementButton()) {
                        sendAccessibilityEventForVirtualButton(virtualViewId, eventType, getVirtualDecrementButtonText());
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        private void sendAccessibilityEventForVirtualText(int eventType) {
            if (AccessibilityManager.getInstance(NumberPicker.this.mContext).isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
                NumberPicker.this.mInputText.onInitializeAccessibilityEvent(event);
                NumberPicker.this.mInputText.onPopulateAccessibilityEvent(event);
                event.setSource(NumberPicker.this, 2);
                NumberPicker.this.requestSendAccessibilityEvent(NumberPicker.this, event);
            }
        }

        private void sendAccessibilityEventForVirtualButton(int virtualViewId, int eventType, String text) {
            if (AccessibilityManager.getInstance(NumberPicker.this.mContext).isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
                event.setClassName(Button.class.getName());
                event.setPackageName(NumberPicker.this.mContext.getPackageName());
                event.getText().add(text);
                event.setEnabled(NumberPicker.this.isEnabled());
                event.setSource(NumberPicker.this, virtualViewId);
                NumberPicker.this.requestSendAccessibilityEvent(NumberPicker.this, event);
            }
        }

        private void findAccessibilityNodeInfosByTextInChild(String searchedLowerCase, int virtualViewId, List<AccessibilityNodeInfo> outResult) {
            String text;
            switch (virtualViewId) {
                case 1:
                    text = getVirtualIncrementButtonText();
                    if (!TextUtils.isEmpty(text) && text.toString().toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(1));
                    }
                    return;
                case 2:
                    CharSequence text2 = NumberPicker.this.mInputText.getText();
                    if (TextUtils.isEmpty(text2) || !text2.toString().toLowerCase().contains(searchedLowerCase)) {
                        CharSequence contentDesc = NumberPicker.this.mInputText.getText();
                        if (!TextUtils.isEmpty(contentDesc) && contentDesc.toString().toLowerCase().contains(searchedLowerCase)) {
                            outResult.add(createAccessibilityNodeInfo(2));
                            return;
                        }
                    }
                    outResult.add(createAccessibilityNodeInfo(2));
                    return;
                    break;
                case 3:
                    text = getVirtualDecrementButtonText();
                    if (!TextUtils.isEmpty(text) && text.toString().toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(3));
                    }
                    return;
            }
        }

        private AccessibilityNodeInfo createAccessibiltyNodeInfoForInputText(int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = NumberPicker.this.mInputText.createAccessibilityNodeInfo();
            info.setSource(NumberPicker.this, 2);
            if (this.mAccessibilityFocusedView != 2) {
                info.addAction(64);
            }
            if (this.mAccessibilityFocusedView == 2) {
                info.addAction(128);
            }
            Rect boundsInParent = this.mTempRect;
            boundsInParent.set(left, top, right, bottom);
            info.setVisibleToUser(NumberPicker.this.isVisibleToUser(boundsInParent));
            info.setBoundsInParent(boundsInParent);
            Rect boundsInScreen = boundsInParent;
            int[] locationOnScreen = this.mTempArray;
            NumberPicker.this.getLocationOnScreen(locationOnScreen);
            boundsInScreen.offset(locationOnScreen[0], locationOnScreen[1]);
            info.setBoundsInScreen(boundsInScreen);
            return info;
        }

        private AccessibilityNodeInfo createAccessibilityNodeInfoForVirtualButton(int virtualViewId, String text, int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setClassName(Button.class.getName());
            info.setPackageName(NumberPicker.this.mContext.getPackageName());
            info.setSource(NumberPicker.this, virtualViewId);
            info.setParent(NumberPicker.this);
            info.setText(text);
            info.setClickable(true);
            info.setLongClickable(true);
            info.setEnabled(NumberPicker.this.isEnabled());
            Rect boundsInParent = this.mTempRect;
            boundsInParent.set(left, top, right, bottom);
            info.setVisibleToUser(NumberPicker.this.isVisibleToUser(boundsInParent));
            info.setBoundsInParent(boundsInParent);
            Rect boundsInScreen = boundsInParent;
            int[] locationOnScreen = this.mTempArray;
            NumberPicker.this.getLocationOnScreen(locationOnScreen);
            boundsInScreen.offset(locationOnScreen[0], locationOnScreen[1]);
            info.setBoundsInScreen(boundsInScreen);
            if (this.mAccessibilityFocusedView != virtualViewId) {
                info.addAction(64);
            }
            if (this.mAccessibilityFocusedView == virtualViewId) {
                info.addAction(128);
            }
            if (NumberPicker.this.isEnabled()) {
                info.addAction(16);
            }
            return info;
        }

        private AccessibilityNodeInfo createAccessibilityNodeInfoForNumberPicker(int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setClassName(NumberPicker.class.getName());
            info.setPackageName(NumberPicker.this.mContext.getPackageName());
            info.setSource(NumberPicker.this);
            if (hasVirtualDecrementButton()) {
                info.addChild(NumberPicker.this, 3);
            }
            info.addChild(NumberPicker.this, 2);
            if (hasVirtualIncrementButton()) {
                info.addChild(NumberPicker.this, 1);
            }
            info.setParent((View) NumberPicker.this.getParentForAccessibility());
            info.setEnabled(NumberPicker.this.isEnabled());
            info.setScrollable(true);
            float applicationScale = NumberPicker.this.getContext().getResources().getCompatibilityInfo().applicationScale;
            Rect boundsInParent = this.mTempRect;
            boundsInParent.set(left, top, right, bottom);
            boundsInParent.scale(applicationScale);
            info.setBoundsInParent(boundsInParent);
            info.setVisibleToUser(NumberPicker.this.isVisibleToUser());
            Rect boundsInScreen = boundsInParent;
            int[] locationOnScreen = this.mTempArray;
            NumberPicker.this.getLocationOnScreen(locationOnScreen);
            boundsInScreen.offset(locationOnScreen[0], locationOnScreen[1]);
            boundsInScreen.scale(applicationScale);
            info.setBoundsInScreen(boundsInScreen);
            if (this.mAccessibilityFocusedView != -1) {
                info.addAction(64);
            }
            if (this.mAccessibilityFocusedView == -1) {
                info.addAction(128);
            }
            if (NumberPicker.this.isEnabled()) {
                if (NumberPicker.this.getWrapSelectorWheel() || NumberPicker.this.getValue() < NumberPicker.this.getMaxValue()) {
                    info.addAction(4096);
                }
                if (NumberPicker.this.getWrapSelectorWheel() || NumberPicker.this.getValue() > NumberPicker.this.getMinValue()) {
                    info.addAction(8192);
                }
            }
            return info;
        }

        private boolean hasVirtualDecrementButton() {
            return NumberPicker.this.getWrapSelectorWheel() || NumberPicker.this.getValue() > NumberPicker.this.getMinValue();
        }

        private boolean hasVirtualIncrementButton() {
            return NumberPicker.this.getWrapSelectorWheel() || NumberPicker.this.getValue() < NumberPicker.this.getMaxValue();
        }

        private String getVirtualDecrementButtonText() {
            int value = NumberPicker.this.mValue - 1;
            if (NumberPicker.this.mWrapSelectorWheel) {
                value = NumberPicker.this.getWrappedSelectorIndex(value);
            }
            if (value < NumberPicker.this.mMinValue) {
                return null;
            }
            String access$6800;
            if (NumberPicker.this.mDisplayedValues == null) {
                access$6800 = NumberPicker.this.formatNumber(value);
            } else {
                access$6800 = NumberPicker.this.mDisplayedValues[value - NumberPicker.this.mMinValue];
            }
            return access$6800;
        }

        private String getVirtualIncrementButtonText() {
            int value = NumberPicker.this.mValue + 1;
            if (NumberPicker.this.mWrapSelectorWheel) {
                value = NumberPicker.this.getWrappedSelectorIndex(value);
            }
            if (value > NumberPicker.this.mMaxValue) {
                return null;
            }
            String access$6800;
            if (NumberPicker.this.mDisplayedValues == null) {
                access$6800 = NumberPicker.this.formatNumber(value);
            } else {
                access$6800 = NumberPicker.this.mDisplayedValues[value - NumberPicker.this.mMinValue];
            }
            return access$6800;
        }
    }

    private static class TwoDigitFormatter implements Formatter {
        final Object[] mArgs = new Object[1];
        final StringBuilder mBuilder = new StringBuilder();
        java.util.Formatter mFmt;
        char mZeroDigit;

        TwoDigitFormatter() {
            init(Locale.getDefault());
        }

        private void init(Locale locale) {
            this.mFmt = createFormatter(locale);
            this.mZeroDigit = getZeroDigit(locale);
        }

        public String format(int value) {
            Locale currentLocale = Locale.getDefault();
            if (this.mZeroDigit != getZeroDigit(currentLocale)) {
                init(currentLocale);
            }
            this.mArgs[0] = Integer.valueOf(value);
            this.mBuilder.delete(0, this.mBuilder.length());
            this.mFmt.format("%02d", this.mArgs);
            return this.mFmt.toString();
        }

        private static char getZeroDigit(Locale locale) {
            return LocaleData.get(locale).zeroDigit;
        }

        private java.util.Formatter createFormatter(Locale locale) {
            return new java.util.Formatter(this.mBuilder, locale);
        }
    }

    class InputTextFilter extends NumberKeyListener {
        InputTextFilter() {
        }

        public int getInputType() {
            return 1;
        }

        protected char[] getAcceptedChars() {
            return NumberPicker.DIGIT_CHARACTERS;
        }

        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (NumberPicker.this.mSetSelectionCommand != null) {
                NumberPicker.this.mSetSelectionCommand.cancel();
            }
            int i = 0;
            CharSequence filtered;
            if (NumberPicker.this.mDisplayedValues == null) {
                filtered = super.filter(source, start, end, dest, dstart, dend);
                if (filtered == null) {
                    filtered = source.subSequence(start, end);
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.valueOf(dest.subSequence(0, dstart)));
                stringBuilder.append(filtered);
                stringBuilder.append(dest.subSequence(dend, dest.length()));
                String result = stringBuilder.toString();
                if ("".equals(result)) {
                    return result;
                }
                if (NumberPicker.this.getSelectedPos(result) > NumberPicker.this.mMaxValue || result.length() > String.valueOf(NumberPicker.this.mMaxValue).length()) {
                    return "";
                }
                return filtered;
            }
            filtered = String.valueOf(source.subSequence(start, end));
            if (TextUtils.isEmpty(filtered)) {
                return "";
            }
            String result2 = new StringBuilder();
            result2.append(String.valueOf(dest.subSequence(0, dstart)));
            result2.append(filtered);
            result2.append(dest.subSequence(dend, dest.length()));
            result2 = result2.toString();
            String str = String.valueOf(result2).toLowerCase();
            String[] access$800 = NumberPicker.this.mDisplayedValues;
            int length = access$800.length;
            while (i < length) {
                String val = access$800[i];
                if (val.toLowerCase().startsWith(str)) {
                    NumberPicker.this.postSetSelectionCommand(result2.length(), val.length());
                    return val.subSequence(dstart, val.length());
                }
                i++;
            }
            return "";
        }
    }

    public static class CustomEditText extends EditText {
        public CustomEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void onEditorAction(int actionCode) {
            super.onEditorAction(actionCode);
            if (actionCode == 6) {
                clearFocus();
            }
        }
    }

    static /* synthetic */ boolean access$1280(NumberPicker x0, int x1) {
        byte b = (byte) (x0.mIncrementVirtualButtonPressed ^ x1);
        x0.mIncrementVirtualButtonPressed = b;
        return b;
    }

    static /* synthetic */ boolean access$1680(NumberPicker x0, int x1) {
        byte b = (byte) (x0.mDecrementVirtualButtonPressed ^ x1);
        x0.mDecrementVirtualButtonPressed = b;
        return b;
    }

    public static final Formatter getTwoDigitFormatter() {
        return sTwoDigitFormatter;
    }

    public NumberPicker(Context context) {
        this(context, null);
    }

    public NumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 16844068);
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mSelectorWheelItemCount = 3;
        this.SELECTOR_MIDDLE_ITEM_INDEX = this.mSelectorWheelItemCount / 2;
        this.mWrapSelectorWheelPreferred = true;
        this.mLongPressUpdateInterval = 300;
        this.mSelectorIndexToStringCache = new SparseArray();
        this.mSelectorIndices = new int[this.mSelectorWheelItemCount];
        this.mInitialScrollOffset = Integer.MIN_VALUE;
        this.mScrollState = 0;
        this.mLastHandledDownDpadKeyCode = -1;
        TypedArray attributesArray = context.obtainStyledAttributes(attrs, R.styleable.NumberPicker, defStyleAttr, defStyleRes);
        int layoutResId = attributesArray.getResourceId(2, DEFAULT_LAYOUT_RESOURCE_ID);
        this.mHasSelectorWheel = layoutResId != DEFAULT_LAYOUT_RESOURCE_ID;
        this.mHideWheelUntilFocused = attributesArray.getBoolean(1, false);
        this.mSolidColor = attributesArray.getColor(0, 0);
        Drawable selectionDivider = attributesArray.getDrawable(7);
        if (selectionDivider != null) {
            selectionDivider.setCallback(this);
            selectionDivider.setLayoutDirection(getLayoutDirection());
            if (selectionDivider.isStateful()) {
                selectionDivider.setState(getDrawableState());
            }
        }
        this.mSelectionDivider = selectionDivider;
        this.mSelectionDividerHeight = attributesArray.getDimensionPixelSize(8, (int) TypedValue.applyDimension(1, 2.0f, getResources().getDisplayMetrics()));
        this.mSelectionDividersDistance = attributesArray.getDimensionPixelSize(9, (int) TypedValue.applyDimension(1, 48.0f, getResources().getDisplayMetrics()));
        this.mMinHeight = attributesArray.getDimensionPixelSize(5, -1);
        this.mMaxHeight = attributesArray.getDimensionPixelSize(3, -1);
        if (this.mMinHeight == -1 || this.mMaxHeight == -1 || this.mMinHeight <= this.mMaxHeight) {
            this.mMinWidth = attributesArray.getDimensionPixelSize(6, -1);
            this.mMaxWidth = attributesArray.getDimensionPixelSize(4, -1);
            if (this.mMinWidth == -1 || this.mMaxWidth == -1 || this.mMinWidth <= this.mMaxWidth) {
                this.mComputeMaxWidth = this.mMaxWidth == -1;
                this.mVirtualButtonPressedDrawable = attributesArray.getDrawable(10);
                attributesArray.recycle();
                this.mPressedStateHelper = new PressedStateHelper();
                setWillNotDraw(this.mHasSelectorWheel ^ 1);
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
                inflater.inflate(layoutResId, this, true);
                OnClickListener onClickListener = new OnClickListener() {
                    public void onClick(View v) {
                        NumberPicker.this.hideSoftInput();
                        NumberPicker.this.mInputText.clearFocus();
                        if (v.getId() == 16908990) {
                            NumberPicker.this.changeValueByOne(true);
                        } else {
                            NumberPicker.this.changeValueByOne(false);
                        }
                    }
                };
                OnLongClickListener onLongClickListener = new OnLongClickListener() {
                    public boolean onLongClick(View v) {
                        NumberPicker.this.hideSoftInput();
                        NumberPicker.this.mInputText.clearFocus();
                        if (v.getId() == 16908990) {
                            NumberPicker.this.postChangeCurrentByOneFromLongPress(true, 0);
                        } else {
                            NumberPicker.this.postChangeCurrentByOneFromLongPress(false, 0);
                        }
                        return true;
                    }
                };
                if (this.mHasSelectorWheel) {
                    this.mIncrementButton = null;
                } else {
                    this.mIncrementButton = (ImageButton) findViewById(16908990);
                    this.mIncrementButton.setOnClickListener(onClickListener);
                    this.mIncrementButton.setOnLongClickListener(onLongClickListener);
                }
                if (this.mHasSelectorWheel) {
                    this.mDecrementButton = null;
                } else {
                    this.mDecrementButton = (ImageButton) findViewById(16908850);
                    this.mDecrementButton.setOnClickListener(onClickListener);
                    this.mDecrementButton.setOnLongClickListener(onLongClickListener);
                }
                this.mInputText = (EditText) findViewById(16909148);
                this.mInputText.setOnFocusChangeListener(new OnFocusChangeListener() {
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            NumberPicker.this.mInputText.selectAll();
                            return;
                        }
                        NumberPicker.this.mInputText.setSelection(0, 0);
                        NumberPicker.this.validateInputTextView(v);
                    }
                });
                this.mInputText.setFilters(new InputFilter[]{new InputTextFilter()});
                this.mInputText.setAccessibilityLiveRegion(1);
                this.mInputText.setRawInputType(2);
                this.mInputText.setImeOptions(6);
                ViewConfiguration configuration = ViewConfiguration.get(context);
                this.mTouchSlop = configuration.getScaledTouchSlop();
                this.mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
                this.mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity() / 8;
                this.mTextSize = (int) this.mInputText.getTextSize();
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setTextAlign(Align.CENTER);
                paint.setTextSize((float) this.mTextSize);
                paint.setTypeface(this.mInputText.getTypeface());
                ColorStateList colors = this.mInputText.getTextColors();
                int color = colors.getColorForState(ENABLED_STATE_SET, -1);
                paint.setColor(color);
                this.mSelectorWheelPaint = paint;
                this.mFlingScroller = new Scroller(getContext(), null, true);
                this.mAdjustScroller = new Scroller(getContext(), new DecelerateInterpolator(2.5f));
                updateInputTextView();
                if (getImportantForAccessibility() == 0) {
                    color = 1;
                    setImportantForAccessibility(1);
                } else {
                    color = 1;
                }
                if (getFocusable() == 16) {
                    setFocusable(color);
                    setFocusableInTouchMode(color);
                    return;
                }
                return;
            }
            throw new IllegalArgumentException("minWidth > maxWidth");
        }
        throw new IllegalArgumentException("minHeight > maxHeight");
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mHasSelectorWheel) {
            int msrdWdth = getMeasuredWidth();
            int msrdHght = getMeasuredHeight();
            int inptTxtMsrdWdth = this.mInputText.getMeasuredWidth();
            int inptTxtMsrdHght = this.mInputText.getMeasuredHeight();
            int inptTxtLeft = (msrdWdth - inptTxtMsrdWdth) / 2;
            int inptTxtTop = (msrdHght - inptTxtMsrdHght) / 2;
            this.mInputText.layout(inptTxtLeft, inptTxtTop, inptTxtLeft + inptTxtMsrdWdth, inptTxtTop + inptTxtMsrdHght);
            if (changed) {
                initializeSelectorWheel();
                initializeFadingEdgesEx();
                this.mTopSelectionDividerTop = ((getHeight() - this.mSelectionDividersDistance) / 2) - this.mSelectionDividerHeight;
                this.mBottomSelectionDividerBottom = (this.mTopSelectionDividerTop + (2 * this.mSelectionDividerHeight)) + this.mSelectionDividersDistance;
            }
            return;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mHasSelectorWheel) {
            super.onMeasure(makeMeasureSpec(widthMeasureSpec, this.mMaxWidth), makeMeasureSpec(heightMeasureSpec, this.mMaxHeight));
            setMeasuredDimension(resolveSizeAndStateRespectingMinSize(this.mMinWidth, getMeasuredWidth(), widthMeasureSpec), resolveSizeAndStateRespectingMinSize(this.mMinHeight, getMeasuredHeight(), heightMeasureSpec));
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private boolean moveToFinalScrollerPosition(Scroller scroller) {
        scroller.forceFinished(true);
        int amountToScroll = scroller.getFinalY() - scroller.getCurrY();
        int overshootAdjustment = this.mInitialScrollOffset - ((this.mCurrentScrollOffset + amountToScroll) % this.mSelectorElementHeight);
        if (overshootAdjustment == 0) {
            return false;
        }
        if (Math.abs(overshootAdjustment) > this.mSelectorElementHeight / 2) {
            if (overshootAdjustment > 0) {
                overshootAdjustment -= this.mSelectorElementHeight;
            } else {
                overshootAdjustment += this.mSelectorElementHeight;
            }
        }
        scrollBy(0, amountToScroll + overshootAdjustment);
        return true;
    }

    /* JADX WARNING: Missing block: B:31:0x00af, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!this.mHasSelectorWheel || !isEnabled() || event.getActionMasked() != 0) {
            return false;
        }
        removeAllCallbacks();
        hideSoftInput();
        float y = event.getY();
        this.mLastDownEventY = y;
        this.mLastDownOrMoveEventY = y;
        this.mLastDownEventTime = event.getEventTime();
        this.mIgnoreMoveEvents = false;
        this.mPerformClickOnTap = false;
        if (this.mLastDownEventY < ((float) this.mTopSelectionDividerTop)) {
            if (this.mScrollState == 0) {
                this.mPressedStateHelper.buttonPressDelayed(2);
            }
        } else if (this.mLastDownEventY > ((float) this.mBottomSelectionDividerBottom) && this.mScrollState == 0) {
            this.mPressedStateHelper.buttonPressDelayed(1);
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        if (!this.mFlingScroller.isFinished()) {
            this.mFlingScroller.forceFinished(true);
            this.mAdjustScroller.forceFinished(true);
            onScrollStateChange(0);
        } else if (!this.mAdjustScroller.isFinished()) {
            this.mFlingScroller.forceFinished(true);
            this.mAdjustScroller.forceFinished(true);
        } else if (this.mLastDownEventY < ((float) this.mTopSelectionDividerTop)) {
            postChangeCurrentByOneFromLongPress(false, (long) ViewConfiguration.getLongPressTimeout());
        } else if (this.mLastDownEventY > ((float) this.mBottomSelectionDividerBottom)) {
            postChangeCurrentByOneFromLongPress(true, (long) ViewConfiguration.getLongPressTimeout());
        } else {
            this.mPerformClickOnTap = true;
            postBeginSoftInputOnLongPressCommand();
        }
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || !this.mHasSelectorWheel) {
            return false;
        }
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(event);
        switch (event.getActionMasked()) {
            case 1:
                removeBeginSoftInputCommand();
                removeChangeCurrentByOneFromLongPress();
                this.mPressedStateHelper.cancel();
                VelocityTracker velocityTracker = this.mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumFlingVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity();
                if (Math.abs(initialVelocity) > this.mMinimumFlingVelocity) {
                    fling(initialVelocity);
                    onScrollStateChange(2);
                } else {
                    int eventY = (int) event.getY();
                    long deltaTime = event.getEventTime() - this.mLastDownEventTime;
                    if (((int) Math.abs(((float) eventY) - this.mLastDownEventY)) > this.mTouchSlop || deltaTime >= ((long) ViewConfiguration.getTapTimeout())) {
                        ensureScrollWheelAdjusted();
                    } else if (this.mPerformClickOnTap) {
                        this.mPerformClickOnTap = false;
                        performClick();
                    } else {
                        int selectorIndexOffset = (eventY / this.mSelectorElementHeight) - this.SELECTOR_MIDDLE_ITEM_INDEX;
                        if (selectorIndexOffset > 0) {
                            changeValueByOne(true);
                            this.mPressedStateHelper.buttonTapped(1);
                        } else if (selectorIndexOffset < 0) {
                            changeValueByOne(false);
                            this.mPressedStateHelper.buttonTapped(2);
                        }
                    }
                    onScrollStateChange(0);
                }
                this.mVelocityTracker.recycle();
                this.mVelocityTracker = null;
                break;
            case 2:
                if (!this.mIgnoreMoveEvents) {
                    float currentMoveY = event.getY();
                    if (this.mScrollState == 1) {
                        scrollBy(0, (int) (currentMoveY - this.mLastDownOrMoveEventY));
                        invalidate();
                    } else if (((int) Math.abs(currentMoveY - this.mLastDownEventY)) > this.mTouchSlop) {
                        removeAllCallbacks();
                        onScrollStateChange(1);
                    }
                    this.mLastDownOrMoveEventY = currentMoveY;
                    break;
                }
                break;
        }
        return true;
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == 1 || action == 3) {
            removeAllCallbacks();
        }
        return super.dispatchTouchEvent(event);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode != 23 && keyCode != 66) {
            switch (keyCode) {
                case 19:
                case 20:
                    if (this.mHasSelectorWheel) {
                        switch (event.getAction()) {
                            case 0:
                                if (this.mWrapSelectorWheel || (keyCode != 20 ? getValue() <= getMinValue() : getValue() >= getMaxValue())) {
                                    requestFocus();
                                    this.mLastHandledDownDpadKeyCode = keyCode;
                                    removeAllCallbacks();
                                    if (this.mFlingScroller.isFinished()) {
                                        changeValueByOne(keyCode == 20);
                                    }
                                    return true;
                                }
                            case 1:
                                if (this.mLastHandledDownDpadKeyCode == keyCode) {
                                    this.mLastHandledDownDpadKeyCode = -1;
                                    return true;
                                }
                                break;
                        }
                    }
                    break;
            }
        }
        removeAllCallbacks();
        return super.dispatchKeyEvent(event);
    }

    public boolean dispatchTrackballEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == 1 || action == 3) {
            removeAllCallbacks();
        }
        return super.dispatchTrackballEvent(event);
    }

    protected boolean dispatchHoverEvent(MotionEvent event) {
        if (!this.mHasSelectorWheel) {
            return super.dispatchHoverEvent(event);
        }
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            int hoveredVirtualViewId;
            int eventY = (int) event.getY();
            if (eventY < this.mTopSelectionDividerTop) {
                hoveredVirtualViewId = 3;
            } else if (eventY > this.mBottomSelectionDividerBottom) {
                hoveredVirtualViewId = 1;
            } else {
                hoveredVirtualViewId = 2;
            }
            int action = event.getActionMasked();
            AccessibilityNodeProviderImpl provider = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
            if (action != 7) {
                switch (action) {
                    case 9:
                        provider.sendAccessibilityEventForVirtualView(hoveredVirtualViewId, 128);
                        this.mLastHoveredChildVirtualViewId = hoveredVirtualViewId;
                        provider.performAction(hoveredVirtualViewId, 64, null);
                        break;
                    case 10:
                        provider.sendAccessibilityEventForVirtualView(hoveredVirtualViewId, 256);
                        this.mLastHoveredChildVirtualViewId = -1;
                        break;
                }
            } else if (!(this.mLastHoveredChildVirtualViewId == hoveredVirtualViewId || this.mLastHoveredChildVirtualViewId == -1)) {
                provider.sendAccessibilityEventForVirtualView(this.mLastHoveredChildVirtualViewId, 256);
                provider.sendAccessibilityEventForVirtualView(hoveredVirtualViewId, 128);
                this.mLastHoveredChildVirtualViewId = hoveredVirtualViewId;
                provider.performAction(hoveredVirtualViewId, 64, null);
            }
        }
        return false;
    }

    public void computeScroll() {
        Scroller scroller = this.mFlingScroller;
        if (scroller.isFinished()) {
            scroller = this.mAdjustScroller;
            if (scroller.isFinished()) {
                return;
            }
        }
        scroller.computeScrollOffset();
        int currentScrollerY = scroller.getCurrY();
        if (this.mPreviousScrollerY == 0) {
            this.mPreviousScrollerY = scroller.getStartY();
        }
        scrollBy(0, currentScrollerY - this.mPreviousScrollerY);
        this.mPreviousScrollerY = currentScrollerY;
        if (scroller.isFinished()) {
            onScrollerFinished(scroller);
        } else {
            invalidate();
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!this.mHasSelectorWheel) {
            this.mIncrementButton.setEnabled(enabled);
        }
        if (!this.mHasSelectorWheel) {
            this.mDecrementButton.setEnabled(enabled);
        }
        this.mInputText.setEnabled(enabled);
    }

    public void scrollBy(int x, int y) {
        int[] selectorIndices = this.mSelectorIndices;
        int startScrollOffset = this.mCurrentScrollOffset;
        if (!this.mWrapSelectorWheel && y > 0 && selectorIndices[this.SELECTOR_MIDDLE_ITEM_INDEX] <= this.mMinValue) {
            this.mCurrentScrollOffset = this.mInitialScrollOffset;
        } else if (this.mWrapSelectorWheel || y >= 0 || selectorIndices[this.SELECTOR_MIDDLE_ITEM_INDEX] < this.mMaxValue) {
            int maxCount = this.mDisplayedValues != null ? this.mDisplayedValues.length + 1 : (this.mMaxValue - this.mMinValue) + 2;
            int maxScrollY = this.mSelectorElementHeight * maxCount;
            if (maxScrollY > 0 && (y > maxScrollY || y < (-maxScrollY))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("the y is abnormal, y:");
                stringBuilder.append(y);
                stringBuilder.append(", maxCount:");
                stringBuilder.append(maxCount);
                stringBuilder.append(", maxScrollY:");
                stringBuilder.append(maxScrollY);
                Log.w("NumberPicker", stringBuilder.toString());
                y = y < 0 ? -this.mSelectorElementHeight : this.mSelectorElementHeight;
            }
            this.mCurrentScrollOffset += y;
            while (this.mCurrentScrollOffset - this.mInitialScrollOffset > this.mSelectorTextGapHeight) {
                this.mCurrentScrollOffset -= this.mTextSize + this.mSelectorTextGapHeight;
                decrementSelectorIndices(selectorIndices);
                setValueInternal(selectorIndices[this.SELECTOR_MIDDLE_ITEM_INDEX], true);
                if (!this.mWrapSelectorWheel && selectorIndices[this.SELECTOR_MIDDLE_ITEM_INDEX] <= this.mMinValue) {
                    this.mCurrentScrollOffset = this.mInitialScrollOffset;
                }
                if (needToPlayIvtEffectWhenScrolling(y)) {
                    playIvtEffect();
                }
                playSound();
            }
            while (this.mCurrentScrollOffset - this.mInitialScrollOffset < (-this.mSelectorTextGapHeight)) {
                this.mCurrentScrollOffset += this.mTextSize + this.mSelectorTextGapHeight;
                incrementSelectorIndices(selectorIndices);
                setValueInternal(selectorIndices[this.SELECTOR_MIDDLE_ITEM_INDEX], true);
                if (!this.mWrapSelectorWheel && selectorIndices[this.SELECTOR_MIDDLE_ITEM_INDEX] >= this.mMaxValue) {
                    this.mCurrentScrollOffset = this.mInitialScrollOffset;
                }
                if (needToPlayIvtEffectWhenScrolling(y)) {
                    playIvtEffect();
                }
                playSound();
            }
            if (startScrollOffset != this.mCurrentScrollOffset) {
                onScrollChanged(0, this.mCurrentScrollOffset, 0, startScrollOffset);
            }
        } else {
            this.mCurrentScrollOffset = this.mInitialScrollOffset;
        }
    }

    protected int computeVerticalScrollOffset() {
        return this.mCurrentScrollOffset;
    }

    protected int computeVerticalScrollRange() {
        return ((this.mMaxValue - this.mMinValue) + 1) * this.mSelectorElementHeight;
    }

    protected int computeVerticalScrollExtent() {
        return getHeight();
    }

    public int getSolidColor() {
        return this.mSolidColor;
    }

    public void setOnValueChangedListener(OnValueChangeListener onValueChangedListener) {
        this.mOnValueChangeListener = onValueChangedListener;
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
    }

    public void setFormatter(Formatter formatter) {
        if (formatter != this.mFormatter) {
            this.mFormatter = formatter;
            initializeSelectorWheelIndices();
            updateInputTextView();
        }
    }

    public void setValue(int value) {
        setValueInternal(value, false);
    }

    public boolean performClick() {
        if (!this.mHasSelectorWheel) {
            return super.performClick();
        }
        if (!super.performClick()) {
            showSoftInput();
        }
        return true;
    }

    public boolean performLongClick() {
        if (!this.mHasSelectorWheel) {
            return super.performLongClick();
        }
        if (!super.performLongClick()) {
            showSoftInput();
            this.mIgnoreMoveEvents = true;
        }
        return true;
    }

    private void showSoftInput() {
        InputMethodManager inputMethodManager = InputMethodManager.peekInstance();
        if (inputMethodManager != null) {
            if (this.mHasSelectorWheel) {
                this.mInputText.setVisibility(0);
            }
            this.mInputText.requestFocus();
            inputMethodManager.showSoftInput(this.mInputText, 0);
        }
    }

    private void hideSoftInput() {
        InputMethodManager inputMethodManager = InputMethodManager.peekInstance();
        if (inputMethodManager != null && inputMethodManager.isActive(this.mInputText)) {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        }
        if (this.mHasSelectorWheel) {
            this.mInputText.setVisibility(4);
        }
    }

    private void tryComputeMaxWidth() {
        if (this.mComputeMaxWidth) {
            int maxTextWidth = 0;
            int i = 0;
            float digitWidth;
            if (this.mDisplayedValues == null) {
                float maxDigitWidth = 0.0f;
                while (i <= 9) {
                    digitWidth = this.mSelectorWheelPaint.measureText(formatNumberWithLocale(i));
                    if (digitWidth > maxDigitWidth) {
                        maxDigitWidth = digitWidth;
                    }
                    i++;
                }
                i = 0;
                for (int current = this.mMaxValue; current > 0; current /= 10) {
                    i++;
                }
                maxTextWidth = (int) (((float) i) * maxDigitWidth);
            } else {
                int valueCount = this.mDisplayedValues.length;
                while (i < valueCount) {
                    digitWidth = this.mSelectorWheelPaint.measureText(this.mDisplayedValues[i]);
                    if (digitWidth > ((float) maxTextWidth)) {
                        maxTextWidth = (int) digitWidth;
                    }
                    i++;
                }
            }
            maxTextWidth += this.mInputText.getPaddingLeft() + this.mInputText.getPaddingRight();
            if (this.mMaxWidth != maxTextWidth) {
                if (maxTextWidth > this.mMinWidth) {
                    this.mMaxWidth = maxTextWidth;
                } else {
                    this.mMaxWidth = this.mMinWidth;
                }
                invalidate();
            }
        }
    }

    public boolean getWrapSelectorWheel() {
        return this.mWrapSelectorWheel;
    }

    public void setWrapSelectorWheel(boolean wrapSelectorWheel) {
        this.mWrapSelectorWheelPreferred = wrapSelectorWheel;
        updateWrapSelectorWheel();
    }

    private void updateWrapSelectorWheel() {
        boolean z = false;
        if ((this.mMaxValue - this.mMinValue >= this.mSelectorIndices.length) && this.mWrapSelectorWheelPreferred) {
            z = true;
        }
        this.mWrapSelectorWheel = z;
    }

    public void setOnLongPressUpdateInterval(long intervalMillis) {
        this.mLongPressUpdateInterval = intervalMillis;
    }

    public int getValue() {
        return this.mValue;
    }

    public int getMinValue() {
        return this.mMinValue;
    }

    public void setMinValue(int minValue) {
        if (this.mMinValue != minValue) {
            if (minValue >= 0) {
                this.mMinValue = minValue;
                if (this.mMinValue > this.mValue) {
                    this.mValue = this.mMinValue;
                }
                updateWrapSelectorWheel();
                initializeSelectorWheelIndices();
                updateInputTextView();
                tryComputeMaxWidth();
                invalidate();
                return;
            }
            throw new IllegalArgumentException("minValue must be >= 0");
        }
    }

    public int getMaxValue() {
        return this.mMaxValue;
    }

    public void setMaxValue(int maxValue) {
        if (this.mMaxValue != maxValue) {
            if (maxValue >= 0) {
                this.mMaxValue = maxValue;
                if (this.mMaxValue < this.mValue) {
                    this.mValue = this.mMaxValue;
                }
                updateWrapSelectorWheel();
                initializeSelectorWheelIndices();
                updateInputTextView();
                tryComputeMaxWidth();
                invalidate();
                return;
            }
            throw new IllegalArgumentException("maxValue must be >= 0");
        }
    }

    public String[] getDisplayedValues() {
        return this.mDisplayedValues;
    }

    public void setDisplayedValues(String[] displayedValues) {
        if (this.mDisplayedValues != displayedValues) {
            this.mDisplayedValues = displayedValues;
            if (this.mDisplayedValues != null) {
                this.mInputText.setRawInputType(524289);
            } else {
                this.mInputText.setRawInputType(2);
            }
            updateInputTextView();
            initializeSelectorWheelIndices();
            tryComputeMaxWidth();
        }
    }

    public CharSequence getDisplayedValueForCurrentSelection() {
        return (CharSequence) this.mSelectorIndexToStringCache.get(getValue());
    }

    protected float getTopFadingEdgeStrength() {
        return 0.9f;
    }

    protected float getBottomFadingEdgeStrength() {
        return 0.9f;
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeAllCallbacks();
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable selectionDivider = this.mSelectionDivider;
        if (selectionDivider != null && selectionDivider.isStateful() && selectionDivider.setState(getDrawableState())) {
            invalidateDrawable(selectionDivider);
        }
    }

    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mSelectionDivider != null) {
            this.mSelectionDivider.jumpToCurrentState();
        }
    }

    public void onResolveDrawables(int layoutDirection) {
        super.onResolveDrawables(layoutDirection);
        if (this.mSelectionDivider != null) {
            this.mSelectionDivider.setLayoutDirection(layoutDirection);
        }
    }

    protected void onDraw(Canvas canvas) {
        Canvas canvas2 = canvas;
        if (this.mHasSelectorWheel) {
            boolean showSelectorWheel = this.mHideWheelUntilFocused ? hasFocus() : true;
            float x = (float) ((this.mRight - this.mLeft) / 2);
            float y = (float) this.mCurrentScrollOffset;
            if (showSelectorWheel && this.mVirtualButtonPressedDrawable != null && this.mScrollState == 0) {
                if (this.mDecrementVirtualButtonPressed) {
                    this.mVirtualButtonPressedDrawable.setState(PRESSED_STATE_SET);
                    this.mVirtualButtonPressedDrawable.setBounds(0, 0, this.mRight, this.mTopSelectionDividerTop);
                    this.mVirtualButtonPressedDrawable.draw(canvas2);
                }
                if (this.mIncrementVirtualButtonPressed) {
                    this.mVirtualButtonPressedDrawable.setState(PRESSED_STATE_SET);
                    this.mVirtualButtonPressedDrawable.setBounds(0, this.mBottomSelectionDividerBottom, this.mRight, this.mBottom);
                    this.mVirtualButtonPressedDrawable.draw(canvas2);
                }
            }
            int[] selectorIndices = this.mSelectorIndices;
            float y2 = y;
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= selectorIndices.length) {
                    break;
                }
                String scrollSelectorValue = (String) this.mSelectorIndexToStringCache.get(selectorIndices[i2]);
                if ((!showSelectorWheel || i2 == this.SELECTOR_MIDDLE_ITEM_INDEX) && (i2 != this.SELECTOR_MIDDLE_ITEM_INDEX || this.mInputText.getVisibility() == 0)) {
                } else {
                    String scrollSelectorValue2 = scrollSelectorValue;
                    setSelectorColor(i2, this.mCurrentScrollOffset, this.mInitialScrollOffset, this.SELECTOR_MIDDLE_ITEM_INDEX, this.mSelectorElementHeight, this.mSelectorWheelPaint);
                    canvas2.drawText(scrollSelectorValue2, x, adjustYCoordinate(i2, y2), this.mSelectorWheelPaint);
                }
                y2 = adjustYPosition(i2, y2) + ((float) this.mSelectorElementHeight);
                i = i2 + 1;
            }
            if (showSelectorWheel && this.mSelectionDivider != null) {
                i = this.mTopSelectionDividerTop;
                this.mSelectionDivider.setBounds(0, i, this.mRight, this.mSelectionDividerHeight + i);
                this.mSelectionDivider.draw(canvas2);
                int bottomOfBottomDivider = this.mBottomSelectionDividerBottom;
                this.mSelectionDivider.setBounds(0, bottomOfBottomDivider - this.mSelectionDividerHeight, this.mRight, bottomOfBottomDivider);
                this.mSelectionDivider.draw(canvas2);
            }
            return;
        }
        super.onDraw(canvas);
    }

    public void onInitializeAccessibilityEventInternal(AccessibilityEvent event) {
        super.onInitializeAccessibilityEventInternal(event);
        event.setClassName(NumberPicker.class.getName());
        event.setScrollable(true);
        event.setScrollY((this.mMinValue + this.mValue) * this.mSelectorElementHeight);
        event.setMaxScrollY((this.mMaxValue - this.mMinValue) * this.mSelectorElementHeight);
    }

    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (!this.mHasSelectorWheel) {
            return super.getAccessibilityNodeProvider();
        }
        if (this.mAccessibilityNodeProvider == null) {
            this.mAccessibilityNodeProvider = new AccessibilityNodeProviderImpl();
        }
        return this.mAccessibilityNodeProvider;
    }

    private int makeMeasureSpec(int measureSpec, int maxSize) {
        if (maxSize == -1) {
            return measureSpec;
        }
        int size = MeasureSpec.getSize(measureSpec);
        int mode = MeasureSpec.getMode(measureSpec);
        if (mode == Integer.MIN_VALUE) {
            return MeasureSpec.makeMeasureSpec(Math.min(size, maxSize), 1073741824);
        }
        if (mode == 0) {
            return MeasureSpec.makeMeasureSpec(maxSize, 1073741824);
        }
        if (mode == 1073741824) {
            return measureSpec;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown measure mode: ");
        stringBuilder.append(mode);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private int resolveSizeAndStateRespectingMinSize(int minSize, int measuredSize, int measureSpec) {
        if (minSize != -1) {
            return View.resolveSizeAndState(Math.max(minSize, measuredSize), measureSpec, 0);
        }
        return measuredSize;
    }

    private void initializeSelectorWheelIndices() {
        this.mSelectorIndexToStringCache.clear();
        int[] selectorIndices = this.mSelectorIndices;
        int current = getValue();
        for (int i = 0; i < this.mSelectorIndices.length; i++) {
            int selectorIndex = (i - this.SELECTOR_MIDDLE_ITEM_INDEX) + current;
            if (this.mWrapSelectorWheel) {
                selectorIndex = getWrappedSelectorIndex(selectorIndex);
            }
            selectorIndices[i] = selectorIndex;
            ensureCachedScrollSelectorValue(selectorIndices[i]);
        }
    }

    private void setValueInternal(int current, boolean notifyChange) {
        if (this.mValue != current) {
            if (this.mWrapSelectorWheel) {
                current = getWrappedSelectorIndex(current);
            } else {
                current = Math.min(Math.max(current, this.mMinValue), this.mMaxValue);
            }
            int previous = this.mValue;
            this.mValue = current;
            if (this.mScrollState != 2) {
                updateInputTextView();
            }
            if (notifyChange) {
                notifyChange(previous, current);
                playIvtEffectWhenFling(previous, current);
            }
            initializeSelectorWheelIndices();
            invalidate();
        }
    }

    private void changeValueByOne(boolean increment) {
        if (this.mHasSelectorWheel) {
            hideSoftInput();
            if (!moveToFinalScrollerPosition(this.mFlingScroller)) {
                moveToFinalScrollerPosition(this.mAdjustScroller);
            }
            this.mPreviousScrollerY = 0;
            if (increment) {
                this.mFlingScroller.startScroll(0, 0, 0, -this.mSelectorElementHeight, 300);
            } else {
                this.mFlingScroller.startScroll(0, 0, 0, this.mSelectorElementHeight, 300);
            }
            invalidate();
        } else if (increment) {
            setValueInternal(this.mValue + 1, true);
        } else {
            setValueInternal(this.mValue - 1, true);
        }
    }

    private void initializeSelectorWheel() {
        initializeSelectorWheelIndices();
        int[] selectorIndices = this.mSelectorIndices;
        this.mSelectorTextGapHeight = (int) ((((float) ((this.mBottom - this.mTop) - (selectorIndices.length * this.mTextSize))) / ((float) selectorIndices.length)) + 0.5f);
        this.mSelectorElementHeight = initializeSelectorElementHeight(this.mTextSize, this.mSelectorTextGapHeight);
        this.mInitialScrollOffset = (this.mInputText.getBaseline() + this.mInputText.getTop()) - (this.mSelectorElementHeight * this.SELECTOR_MIDDLE_ITEM_INDEX);
        this.mCurrentScrollOffset = this.mInitialScrollOffset;
        updateInputTextView();
    }

    protected void initializeFadingEdgesEx() {
        initializeFadingEdges();
    }

    private void initializeFadingEdges() {
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(((this.mBottom - this.mTop) - this.mTextSize) / 2);
    }

    private void onScrollerFinished(Scroller scroller) {
        if (scroller == this.mFlingScroller) {
            ensureScrollWheelAdjusted();
            updateInputTextView();
            onScrollStateChange(0);
        } else if (this.mScrollState != 1) {
            updateInputTextView();
        }
    }

    private void onScrollStateChange(int scrollState) {
        if (this.mScrollState != scrollState) {
            this.mScrollState = scrollState;
            if (this.mOnScrollListener != null) {
                this.mOnScrollListener.onScrollStateChange(this, scrollState);
            }
        }
    }

    private void fling(int velocityY) {
        this.mPreviousScrollerY = 0;
        if (velocityY > 0) {
            this.mFlingScroller.fling(0, 0, 0, velocityY, 0, 0, 0, Integer.MAX_VALUE);
        } else {
            this.mFlingScroller.fling(0, Integer.MAX_VALUE, 0, velocityY, 0, 0, 0, Integer.MAX_VALUE);
        }
        setFlingDirection(velocityY);
        invalidate();
    }

    private int getWrappedSelectorIndex(int selectorIndex) {
        if (selectorIndex > this.mMaxValue) {
            return (this.mMinValue + ((selectorIndex - this.mMaxValue) % (this.mMaxValue - this.mMinValue))) - 1;
        }
        if (selectorIndex < this.mMinValue) {
            return (this.mMaxValue - ((this.mMinValue - selectorIndex) % (this.mMaxValue - this.mMinValue))) + 1;
        }
        return selectorIndex;
    }

    private void incrementSelectorIndices(int[] selectorIndices) {
        int i;
        for (i = 0; i < selectorIndices.length - 1; i++) {
            selectorIndices[i] = selectorIndices[i + 1];
        }
        i = selectorIndices[selectorIndices.length - 2] + 1;
        if (this.mWrapSelectorWheel && i > this.mMaxValue) {
            i = this.mMinValue;
        }
        selectorIndices[selectorIndices.length - 1] = i;
        ensureCachedScrollSelectorValue(i);
    }

    private void decrementSelectorIndices(int[] selectorIndices) {
        int i;
        for (i = selectorIndices.length - 1; i > 0; i--) {
            selectorIndices[i] = selectorIndices[i - 1];
        }
        i = selectorIndices[1] - 1;
        if (this.mWrapSelectorWheel && i < this.mMinValue) {
            i = this.mMaxValue;
        }
        selectorIndices[0] = i;
        ensureCachedScrollSelectorValue(i);
    }

    private void ensureCachedScrollSelectorValue(int selectorIndex) {
        SparseArray<String> cache = this.mSelectorIndexToStringCache;
        if (((String) cache.get(selectorIndex)) == null) {
            String scrollSelectorValue;
            if (selectorIndex < this.mMinValue || selectorIndex > this.mMaxValue) {
                scrollSelectorValue = "";
            } else if (this.mDisplayedValues != null) {
                scrollSelectorValue = this.mDisplayedValues[selectorIndex - this.mMinValue];
            } else {
                scrollSelectorValue = formatNumber(selectorIndex);
            }
            cache.put(selectorIndex, scrollSelectorValue);
        }
    }

    private String formatNumber(int value) {
        return this.mFormatter != null ? this.mFormatter.format(value) : formatNumberWithLocale(value);
    }

    private void validateInputTextView(View v) {
        String str = String.valueOf(((TextView) v).getText());
        if (TextUtils.isEmpty(str)) {
            updateInputTextView();
        } else {
            setValueInternal(getSelectedPos(str.toString()), true);
        }
    }

    private boolean updateInputTextView() {
        String text;
        if (this.mDisplayedValues == null) {
            text = formatNumber(this.mValue);
        } else {
            text = this.mDisplayedValues[this.mValue - this.mMinValue];
        }
        if (!TextUtils.isEmpty(text)) {
            CharSequence beforeText = this.mInputText.getText();
            if (!text.equals(beforeText.toString())) {
                this.mInputText.setText((CharSequence) text);
                if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
                    AccessibilityEvent event = AccessibilityEvent.obtain(16);
                    this.mInputText.onInitializeAccessibilityEvent(event);
                    this.mInputText.onPopulateAccessibilityEvent(event);
                    event.setFromIndex(0);
                    event.setRemovedCount(beforeText.length());
                    event.setAddedCount(text.length());
                    event.setBeforeText(beforeText);
                    event.setSource(this, 2);
                    requestSendAccessibilityEvent(this, event);
                }
                return true;
            }
        }
        return false;
    }

    private void notifyChange(int previous, int current) {
        if (this.mOnValueChangeListener != null) {
            this.mOnValueChangeListener.onValueChange(this, previous, this.mValue);
        }
    }

    private void postChangeCurrentByOneFromLongPress(boolean increment, long delayMillis) {
        if (this.mChangeCurrentByOneFromLongPressCommand == null) {
            this.mChangeCurrentByOneFromLongPressCommand = new ChangeCurrentByOneFromLongPressCommand();
        } else {
            removeCallbacks(this.mChangeCurrentByOneFromLongPressCommand);
        }
        this.mChangeCurrentByOneFromLongPressCommand.setStep(increment);
        postDelayed(this.mChangeCurrentByOneFromLongPressCommand, delayMillis);
    }

    private void removeChangeCurrentByOneFromLongPress() {
        if (this.mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(this.mChangeCurrentByOneFromLongPressCommand);
        }
    }

    private void postBeginSoftInputOnLongPressCommand() {
        if (this.mBeginSoftInputOnLongPressCommand == null) {
            this.mBeginSoftInputOnLongPressCommand = new BeginSoftInputOnLongPressCommand();
        } else {
            removeCallbacks(this.mBeginSoftInputOnLongPressCommand);
        }
        postDelayed(this.mBeginSoftInputOnLongPressCommand, (long) ViewConfiguration.getLongPressTimeout());
    }

    private void removeBeginSoftInputCommand() {
        if (this.mBeginSoftInputOnLongPressCommand != null) {
            removeCallbacks(this.mBeginSoftInputOnLongPressCommand);
        }
    }

    private void removeAllCallbacks() {
        if (this.mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(this.mChangeCurrentByOneFromLongPressCommand);
        }
        if (this.mSetSelectionCommand != null) {
            this.mSetSelectionCommand.cancel();
        }
        if (this.mBeginSoftInputOnLongPressCommand != null) {
            removeCallbacks(this.mBeginSoftInputOnLongPressCommand);
        }
        this.mPressedStateHelper.cancel();
    }

    private int getSelectedPos(String value) {
        if (this.mDisplayedValues == null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return this.mMinValue;
            }
        }
        for (int i = 0; i < this.mDisplayedValues.length; i++) {
            value = value.toLowerCase();
            if (this.mDisplayedValues[i].toLowerCase().startsWith(value)) {
                return this.mMinValue + i;
            }
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e2) {
            return this.mMinValue;
        }
    }

    private void postSetSelectionCommand(int selectionStart, int selectionEnd) {
        if (this.mSetSelectionCommand == null) {
            this.mSetSelectionCommand = new SetSelectionCommand(this.mInputText);
        }
        this.mSetSelectionCommand.post(selectionStart, selectionEnd);
    }

    private boolean ensureScrollWheelAdjusted() {
        int deltaY = this.mInitialScrollOffset - this.mCurrentScrollOffset;
        if (deltaY == 0) {
            return false;
        }
        this.mPreviousScrollerY = 0;
        if (Math.abs(deltaY) > this.mSelectorElementHeight / 2) {
            deltaY += deltaY > 0 ? -this.mSelectorElementHeight : this.mSelectorElementHeight;
        }
        this.mAdjustScroller.startScroll(0, 0, 0, deltaY, 800);
        invalidate();
        return true;
    }

    private static String formatNumberWithLocale(int value) {
        return String.format(Locale.getDefault(), "%d", new Object[]{Integer.valueOf(value)});
    }

    public void addFireList(NumberPicker np) {
    }

    protected int getNormalTextColor(int color) {
        return color;
    }

    protected void setSelectorColor(int i, int currentOffset, int initOffset, int index, int height, Paint paint) {
    }

    protected float adjustYPosition(int i, float y) {
        return y;
    }

    protected int initializeSelectorElementHeight(int textSize, int selectorTextGapHeight) {
        return textSize + selectorTextGapHeight;
    }

    public EditText getInputText() {
        return this.mInputText;
    }

    protected Paint getSelectorWheelPaint() {
        return this.mSelectorWheelPaint;
    }

    protected boolean getHasSelectorWheel() {
        return this.mHasSelectorWheel;
    }

    protected void playIvtEffect() {
    }

    protected boolean needToPlayIvtEffectWhenScrolling(int scrollByY) {
        return false;
    }

    protected void playIvtEffectWhenFling(int previous, int current) {
    }

    protected void setFlingDirection(int velocityY) {
    }

    protected float adjustYCoordinate(int i, float y) {
        return y;
    }

    protected void playSound() {
    }

    public int getSelectorMiddleItemIdex() {
        return this.SELECTOR_MIDDLE_ITEM_INDEX;
    }

    public void setSelectMiddleItemIdex(int selectMiddleItemIdex) {
        this.SELECTOR_MIDDLE_ITEM_INDEX = selectMiddleItemIdex;
    }

    public int[] getSelectorIndices() {
        return this.mSelectorIndices;
    }

    public void setSelectorIndices(int[] selectorIndices) {
        this.mSelectorIndices = selectorIndices;
    }
}
