package com.android.internal.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import android.widget.TextView;

public class TooltipPopup {
    private static final String TAG = "TooltipPopup";
    private final View mContentView;
    private final Context mContext;
    private final LayoutParams mLayoutParams = new LayoutParams();
    private final TextView mMessageView;
    private final int[] mTmpAnchorPos = new int[2];
    private final int[] mTmpAppPos = new int[2];
    private final Rect mTmpDisplayFrame = new Rect();

    public TooltipPopup(Context context) {
        this.mContext = context;
        this.mContentView = LayoutInflater.from(this.mContext).inflate(17367321, null);
        this.mMessageView = (TextView) this.mContentView.findViewById(16908299);
        this.mLayoutParams.setTitle(this.mContext.getString(17041262));
        this.mLayoutParams.packageName = this.mContext.getOpPackageName();
        this.mLayoutParams.type = 1005;
        this.mLayoutParams.width = -2;
        this.mLayoutParams.height = -2;
        this.mLayoutParams.format = -3;
        this.mLayoutParams.windowAnimations = 16974594;
        this.mLayoutParams.flags = 24;
    }

    public void show(View anchorView, int anchorX, int anchorY, boolean fromTouch, CharSequence tooltipText) {
        if (isShowing()) {
            hide();
        }
        this.mMessageView.setText(tooltipText);
        computePosition(anchorView, anchorX, anchorY, fromTouch, this.mLayoutParams);
        ((WindowManager) this.mContext.getSystemService("window")).addView(this.mContentView, this.mLayoutParams);
    }

    public void hide() {
        if (isShowing()) {
            ((WindowManager) this.mContext.getSystemService("window")).removeView(this.mContentView);
        }
    }

    public View getContentView() {
        return this.mContentView;
    }

    public boolean isShowing() {
        return this.mContentView.getParent() != null;
    }

    private void computePosition(View anchorView, int anchorX, int anchorY, boolean fromTouch, LayoutParams outParams) {
        int offsetX;
        int offsetAbove;
        int offsetBelow;
        int i;
        LayoutParams layoutParams = outParams;
        layoutParams.token = anchorView.getApplicationWindowToken();
        int tooltipPreciseAnchorThreshold = this.mContext.getResources().getDimensionPixelOffset(17105380);
        if (anchorView.getWidth() >= tooltipPreciseAnchorThreshold) {
            offsetX = anchorX;
        } else {
            offsetX = anchorView.getWidth() / 2;
        }
        if (anchorView.getHeight() >= tooltipPreciseAnchorThreshold) {
            offsetAbove = this.mContext.getResources().getDimensionPixelOffset(17105379);
            offsetBelow = anchorY + offsetAbove;
            offsetAbove = anchorY - offsetAbove;
        } else {
            offsetBelow = anchorView.getHeight();
            offsetAbove = 0;
        }
        layoutParams.gravity = 49;
        int tooltipOffset = this.mContext.getResources();
        if (fromTouch) {
            i = 17105383;
        } else {
            i = 17105382;
        }
        tooltipOffset = tooltipOffset.getDimensionPixelOffset(i);
        View appView = WindowManagerGlobal.getInstance().getWindowView(anchorView.getApplicationWindowToken());
        if (appView == null) {
            Slog.e(TAG, "Cannot find app view");
            return;
        }
        appView.getWindowVisibleDisplayFrame(this.mTmpDisplayFrame);
        appView.getLocationOnScreen(this.mTmpAppPos);
        anchorView.getLocationOnScreen(this.mTmpAnchorPos);
        int[] iArr = this.mTmpAnchorPos;
        iArr[0] = iArr[0] - this.mTmpAppPos[0];
        iArr = this.mTmpAnchorPos;
        iArr[1] = iArr[1] - this.mTmpAppPos[1];
        layoutParams.x = (this.mTmpAnchorPos[0] + offsetX) - (appView.getWidth() / 2);
        int spec = MeasureSpec.makeMeasureSpec(0, 0);
        this.mContentView.measure(spec, spec);
        int tooltipHeight = this.mContentView.getMeasuredHeight();
        int yAbove = ((this.mTmpAnchorPos[1] + offsetAbove) - tooltipOffset) - tooltipHeight;
        int yBelow = (this.mTmpAnchorPos[1] + offsetBelow) + tooltipOffset;
        if (fromTouch) {
            if (yAbove >= 0) {
                layoutParams.y = yAbove;
            } else {
                layoutParams.y = yBelow;
            }
        } else if (yBelow + tooltipHeight <= this.mTmpDisplayFrame.height()) {
            layoutParams.y = yBelow;
        } else {
            layoutParams.y = yAbove;
        }
    }
}
