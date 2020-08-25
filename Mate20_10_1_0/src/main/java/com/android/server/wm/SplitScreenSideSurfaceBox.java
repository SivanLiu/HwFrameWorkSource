package com.android.server.wm;

import android.graphics.Rect;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.SurfaceControl;
import com.android.server.wm.utils.HwDisplaySizeUtil;

public class SplitScreenSideSurfaceBox {
    private static final Rect EMPTY_RECT = new Rect();
    private static final int FIRST_INDEX = 0;
    private static final int FORTH_INDEX = 3;
    private static final float ROUND_VALUE = 0.5f;
    private static final int SECOND_INDEX = 1;
    private static final String SIDE_PROP = SystemProperties.get("ro.config.hw_curved_side_disp", "");
    private static final int SIDE_PROP_LENGTH = 4;
    private static final int THIRD_INDEX = 2;
    private static int sBaseValue;
    private static Rect sInnerLand = new Rect();
    private static Rect sInnerPort = new Rect();
    private static Rect sOuterLand = new Rect();
    private static Rect sOuterPort = new Rect();
    private static int[] sSideParams;
    private final InsetSurface mBottom = new InsetSurface("bottom");
    private DisplayContent mDc;
    private Rect mInner = new Rect();
    private final InsetSurface mLeft = new InsetSurface("left");
    private Rect mOuter = new Rect();
    private final InsetSurface mRight = new InsetSurface("right");
    private final InsetSurface[] mSurfaces = {this.mLeft, this.mTop, this.mRight, this.mBottom};
    private final InsetSurface mTop = new InsetSurface("top");

    static {
        sBaseValue = 24;
        try {
            if (!TextUtils.isEmpty(SIDE_PROP)) {
                String[] params = SIDE_PROP.split(",");
                int length = params.length;
                if (length < 4) {
                    sSideParams = null;
                    return;
                }
                sSideParams = new int[length];
                for (int i = 0; i < length; i++) {
                    sSideParams[i] = Integer.parseInt(params[i]);
                }
                sOuterPort.set(0, 0, sSideParams[0] + sSideParams[1] + sSideParams[2], sSideParams[3]);
                sOuterLand.set(0, 0, sSideParams[3], sSideParams[0] + sSideParams[1] + sSideParams[2]);
                sInnerPort.set(sSideParams[0], 0, sSideParams[1] + sSideParams[2], sSideParams[3]);
                sInnerLand.set(0, sSideParams[0], sSideParams[3], sSideParams[1] + sSideParams[2]);
                sBaseValue = sSideParams[0];
            }
        } catch (NumberFormatException e) {
            sSideParams = null;
        } catch (Exception e2) {
            sSideParams = null;
        }
    }

    public SplitScreenSideSurfaceBox(DisplayContent dc) {
        this.mDc = dc;
        this.mTop.setDisplayContent(dc);
        this.mLeft.setDisplayContent(dc);
        this.mBottom.setDisplayContent(dc);
        this.mRight.setDisplayContent(dc);
    }

    public void layout(Rect outer, Rect inner) {
        this.mTop.layout(outer.left, outer.top, outer.right, inner.top);
        this.mLeft.layout(outer.left, outer.top, inner.left, outer.bottom);
        this.mBottom.layout(outer.left, inner.bottom, outer.right, outer.bottom);
        this.mRight.layout(inner.right, outer.top, outer.right, outer.bottom);
    }

    public void hide(SurfaceControl.Transaction t) {
        Rect rect = EMPTY_RECT;
        layout(rect, rect);
        for (InsetSurface surface : this.mSurfaces) {
            surface.hide(t);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x0069 A[LOOP:0: B:12:0x0067->B:13:0x0069, LOOP_END] */
    public void show() {
        int rotation = this.mDc.mWmService.getDefaultDisplayRotation();
        int safeSideWidth = HwDisplaySizeUtil.getInstance(this.mDc.mWmService).getSafeSideWidth();
        if (rotation != 0) {
            if (rotation != 1) {
                if (rotation != 2) {
                    if (rotation != 3) {
                        this.mOuter.set(sOuterPort);
                        this.mInner.set(sInnerPort);
                        float ratio = (((float) safeSideWidth) * 1.0f) / ((float) sBaseValue);
                        layout(scale(this.mOuter, ratio), scale(this.mInner, ratio));
                        for (InsetSurface surface : this.mSurfaces) {
                            surface.show(null);
                        }
                    }
                }
            }
            this.mOuter.set(sOuterLand);
            this.mInner.set(sInnerLand);
            float ratio2 = (((float) safeSideWidth) * 1.0f) / ((float) sBaseValue);
            layout(scale(this.mOuter, ratio2), scale(this.mInner, ratio2));
            while (r5 < r4) {
            }
        }
        this.mOuter.set(sOuterPort);
        this.mInner.set(sInnerPort);
        float ratio22 = (((float) safeSideWidth) * 1.0f) / ((float) sBaseValue);
        layout(scale(this.mOuter, ratio22), scale(this.mInner, ratio22));
        while (r5 < r4) {
        }
    }

    public void destroy() {
        this.mOuter.setEmpty();
        this.mInner.setEmpty();
        Rect rect = EMPTY_RECT;
        layout(rect, rect);
        for (InsetSurface surface : this.mSurfaces) {
            surface.remove();
        }
    }

    private Rect scale(Rect r, float scale) {
        Rect ret = new Rect(r);
        if (scale != 1.0f) {
            ret.left = (int) ((((float) r.left) * scale) + 0.5f);
            ret.top = (int) ((((float) r.top) * scale) + 0.5f);
            ret.right = (int) ((((float) r.right) * scale) + 0.5f);
            ret.bottom = (int) ((((float) r.bottom) * scale) + 0.5f);
        }
        return ret;
    }
}
