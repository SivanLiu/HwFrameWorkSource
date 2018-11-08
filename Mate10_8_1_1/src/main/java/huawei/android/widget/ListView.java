package huawei.android.widget;

import android.content.Context;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ListView extends android.widget.ListView {
    private static final boolean IS_EMUI_LITE = SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", false);

    public ListView(Context context) {
        this(context, null);
    }

    public ListView(Context context, AttributeSet attrs) {
        this(context, attrs, 16842868);
    }

    public ListView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setItemDeleteAnimation(boolean enable) {
        if ("com.android.mms".equals(getContext().getPackageName()) && enable) {
            this.mIsSupportAnim = true;
        }
        if (IS_EMUI_LITE) {
            this.mIsSupportAnim = false;
        }
        wrapObserver();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        wrapObserver();
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!this.mIsSupportAnim || this.mListDeleteAnimator == null) {
            return super.dispatchTouchEvent(ev);
        }
        this.mListDeleteAnimator.cancel();
        return true;
    }
}
