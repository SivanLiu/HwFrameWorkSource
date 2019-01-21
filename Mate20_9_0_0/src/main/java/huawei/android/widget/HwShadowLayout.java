package huawei.android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import huawei.android.widget.effect.engine.HwShadowEngine;
import huawei.android.widget.effect.engine.HwShadowEngine.ShadowType;

public class HwShadowLayout extends FrameLayout {
    private HwShadowEngine mShadowEngine = new HwShadowEngine(this, ShadowType.Medium);

    public HwShadowLayout(Context context) {
        super(context);
    }

    public HwShadowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HwShadowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (this.mShadowEngine.isEnable() && getParent() != null) {
            ((ViewGroup) getParent()).setClipChildren(false);
        }
    }

    protected void onDraw(Canvas canvas) {
        this.mShadowEngine.renderShadow(canvas);
        super.onDraw(canvas);
    }

    public void setShadowEnable(boolean isEnable) {
        this.mShadowEngine.setEnable(isEnable);
    }

    public boolean isShadowEnable() {
        return this.mShadowEngine.isEnable();
    }
}
