package huawei.com.android.server.fingerprint;

import android.content.Context;
import android.graphics.Canvas;
import android.text.DynamicLayout;
import android.text.Layout.Alignment;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.server.gesture.GestureNavConst;

public class HintText extends TextView {
    private DynamicLayout mDynamicLayout;
    private TextPaint tp;

    public HintText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initView();
    }

    private void initView() {
        this.tp = new TextPaint(1);
        this.tp.setTextSize(getTextSize());
        this.tp.setColor(getCurrentTextColor());
    }

    protected void onDraw(Canvas canvas) {
        this.mDynamicLayout = new DynamicLayout(getText(), this.tp, getWidth(), Alignment.ALIGN_CENTER, 1.0f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, false);
        this.mDynamicLayout.draw(canvas);
    }
}
