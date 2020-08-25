package huawei.com.android.server.fingerprint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

public class BackFingerprintView extends LinearLayout {
    static final String TAG = "BackFingerprintView";

    public BackFingerprintView(Context context) {
        super(context);
    }

    public BackFingerprintView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.argb(128.0f, 0.0f, 0.0f, 0.0f));
        Log.e(TAG, "fingerprintview onDraw");
    }
}
