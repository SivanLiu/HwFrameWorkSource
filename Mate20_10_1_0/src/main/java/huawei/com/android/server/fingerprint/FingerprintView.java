package huawei.com.android.server.fingerprint;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class FingerprintView extends RelativeLayout {
    static final String TAG = "FingerprintView";
    private ICallBack mHandleViewCallback;

    public interface ICallBack {
        void onConfigurationChanged(Configuration configuration);

        void onDrawFinish();

        void userActivity();
    }

    public FingerprintView(Context context) {
        super(context);
    }

    public FingerprintView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.argb(128.0f, 0.0f, 0.0f, 0.0f));
        Log.e(TAG, "fingerprintview onDraw");
    }

    public boolean onTouchEvent(MotionEvent ev) {
        ICallBack iCallBack = this.mHandleViewCallback;
        if (iCallBack == null) {
            return false;
        }
        iCallBack.userActivity();
        return false;
    }

    public void setCallback(ICallBack handleViewCallback) {
        this.mHandleViewCallback = handleViewCallback;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
