package huawei.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockPatternView.DisplayMode;
import com.android.internal.widget.LockPatternView.OnPatternListener;
import java.util.List;

public class HwLockPatternViewEx extends FrameLayout {
    public static final int ANIMATE = 1;
    protected static final long CLEAR_WRONG_ATTEMPT_TIMEOUT_MS = 1000;
    public static final int CORRECT = 0;
    private static final int PATTERN_COLOR = -2143860913;
    private static final String TAG = "HwLockPatternView";
    public static final int WRONG = 2;
    private Runnable mClearPatternRunnable;
    protected OnPatternListener mLockPatternListener;
    private LockPatternUtils mLockPatternUtils;
    private LockPatternView mLockPatternView;
    private OnPatternDetectListener mRegisteredListener;

    public interface OnPatternDetectListener {
        void onPatternComplete(String str);
    }

    public HwLockPatternViewEx(Context context) {
        this(context, null);
    }

    public HwLockPatternViewEx(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mClearPatternRunnable = new Runnable() {
            public void run() {
                HwLockPatternViewEx.this.mLockPatternView.clearPattern();
            }
        };
        this.mLockPatternListener = new OnPatternListener() {
            public void onPatternStart() {
                HwLockPatternViewEx.this.mLockPatternView.removeCallbacks(HwLockPatternViewEx.this.mClearPatternRunnable);
            }

            public void onPatternCleared() {
                HwLockPatternViewEx.this.mLockPatternView.removeCallbacks(HwLockPatternViewEx.this.mClearPatternRunnable);
            }

            public void onPatternDetected(List<Cell> pattern) {
                if (pattern == null || pattern.size() < 4) {
                    HwLockPatternViewEx.this.prcocessWrong();
                } else {
                    HwLockPatternViewEx.this.notifyPatternComplete(pattern);
                }
            }

            public void onPatternCellAdded(List<Cell> list) {
            }
        };
        this.mLockPatternUtils = new LockPatternUtils(context);
        boolean isTactileFeedback = this.mLockPatternUtils.isTactileFeedbackEnabled();
        boolean isStealthMode = this.mLockPatternUtils.isVisiblePatternEnabled(0) ^ true;
        this.mLockPatternView = new LockPatternView(context, attrs);
        this.mLockPatternView.setTactileFeedbackEnabled(isTactileFeedback);
        this.mLockPatternView.setInStealthMode(isStealthMode);
        this.mLockPatternView.setRegularColor(true, PATTERN_COLOR);
        this.mLockPatternView.setEnabled(true);
        this.mLockPatternView.enableInput();
        this.mLockPatternView.clearPattern();
        addView(this.mLockPatternView);
    }

    public void setOnPatterDetectListener(OnPatternDetectListener listener) {
        if (listener != null) {
            this.mRegisteredListener = listener;
            this.mLockPatternView.setOnPatternListener(this.mLockPatternListener);
            return;
        }
        Log.e(TAG, "setOnPatterDetectListener The listener is NULL!");
    }

    public void setColor(boolean isInKeyguard, int color) {
        this.mLockPatternView.setRegularColor(isInKeyguard, color);
    }

    public void setDisplayModeEx(int mode) {
        switch (mode) {
            case 0:
                this.mLockPatternView.setDisplayMode(DisplayMode.Correct);
                return;
            case 1:
                this.mLockPatternView.setDisplayMode(DisplayMode.Animate);
                return;
            case 2:
                this.mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                postClearPatternRunnable();
                return;
            default:
                return;
        }
    }

    private void prcocessWrong() {
        this.mLockPatternView.setDisplayMode(DisplayMode.Wrong);
        this.mLockPatternView.setEnabled(true);
        this.mLockPatternView.enableInput();
        postClearPatternRunnable();
    }

    private void postClearPatternRunnable() {
        this.mLockPatternView.removeCallbacks(this.mClearPatternRunnable);
        this.mLockPatternView.postDelayed(this.mClearPatternRunnable, CLEAR_WRONG_ATTEMPT_TIMEOUT_MS);
    }

    private void notifyPatternComplete(List<Cell> pattern) {
        if (pattern != null && pattern.size() >= 4) {
            String patternString = LockPatternUtils.patternToString(pattern);
            if (this.mRegisteredListener != null) {
                this.mRegisteredListener.onPatternComplete(patternString);
            } else {
                Log.e(TAG, "notifyPatternComplete mRegisteredListener is NULL!");
            }
        }
    }
}
