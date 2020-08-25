package huawei.com.android.server.policy;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class BallFrameLayout extends RelativeLayout {
    private BallFrameView mBallFrameView;

    public BallFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        BallFrameView ballFrameView = this.mBallFrameView;
        return ballFrameView != null && ballFrameView.dispatchTouchEvent(event);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mBallFrameView = (BallFrameView) getChildAt(1);
    }
}
