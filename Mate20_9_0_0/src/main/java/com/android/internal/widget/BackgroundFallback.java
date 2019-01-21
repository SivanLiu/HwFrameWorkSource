package com.android.internal.widget;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

public class BackgroundFallback {
    private Drawable mBackgroundFallback;

    public void setDrawable(Drawable d) {
        this.mBackgroundFallback = d;
    }

    public boolean hasFallback() {
        return this.mBackgroundFallback != null;
    }

    public void draw(ViewGroup boundsView, ViewGroup root, Canvas c, View content, View coveringView1, View coveringView2) {
        Canvas canvas = c;
        View view = coveringView1;
        View view2 = coveringView2;
        if (hasFallback()) {
            View child;
            int width = boundsView.getWidth();
            int height = boundsView.getHeight();
            int rootOffsetX = root.getLeft();
            int rootOffsetY = root.getTop();
            int left = width;
            int top = height;
            int right = 0;
            int bottom = 0;
            int childCount = root.getChildCount();
            int top2 = top;
            top = left;
            left = 0;
            while (left < childCount) {
                child = root.getChildAt(left);
                int childCount2 = childCount;
                childCount = child.getBackground();
                if (child != content) {
                    if (child.getVisibility() == 0) {
                        if (!isOpaque(childCount)) {
                        }
                    }
                    left++;
                    childCount = childCount2;
                } else if (childCount == 0 && (child instanceof ViewGroup) && ((ViewGroup) child).getChildCount() == 0) {
                    left++;
                    childCount = childCount2;
                }
                top = Math.min(top, child.getLeft() + rootOffsetX);
                top2 = Math.min(top2, child.getTop() + rootOffsetY);
                right = Math.max(right, child.getRight() + rootOffsetX);
                bottom = Math.max(bottom, child.getBottom() + rootOffsetY);
                left++;
                childCount = childCount2;
            }
            childCount = bottom;
            bottom = top;
            boolean eachBarCoversTopInY = true;
            left = 0;
            while (left < 2) {
                child = left == 0 ? view : view2;
                if (child != null && child.getVisibility() == 0 && child.getAlpha() == 1.0f && isOpaque(child.getBackground())) {
                    if (child.getTop() <= 0 && child.getBottom() >= height && child.getLeft() <= 0 && child.getRight() >= bottom) {
                        bottom = 0;
                    }
                    if (child.getTop() <= 0 && child.getBottom() >= height && child.getLeft() <= right && child.getRight() >= width) {
                        right = width;
                    }
                    if (child.getTop() <= 0 && child.getBottom() >= top && child.getLeft() <= 0 && child.getRight() >= width) {
                        top2 = 0;
                    }
                    if (child.getTop() <= childCount && child.getBottom() >= height && child.getLeft() <= 0 && child.getRight() >= width) {
                        childCount = height;
                    }
                    int i = (child.getTop() > 0 || child.getBottom() < top2) ? 0 : 1;
                    eachBarCoversTopInY &= i;
                } else {
                    eachBarCoversTopInY = false;
                }
                left++;
            }
            if (eachBarCoversTopInY && (viewsCoverEntireWidth(view, view2, width) || viewsCoverEntireWidth(view2, view, width))) {
                top2 = 0;
            }
            if (bottom < right && top2 < childCount) {
                int i2;
                if (top2 > 0) {
                    i2 = 0;
                    this.mBackgroundFallback.setBounds(0, 0, width, top2);
                    this.mBackgroundFallback.draw(canvas);
                } else {
                    i2 = 0;
                }
                if (bottom > 0) {
                    this.mBackgroundFallback.setBounds(i2, top2, bottom, height);
                    this.mBackgroundFallback.draw(canvas);
                }
                if (right < width) {
                    this.mBackgroundFallback.setBounds(right, top2, width, height);
                    this.mBackgroundFallback.draw(canvas);
                }
                if (childCount < height) {
                    this.mBackgroundFallback.setBounds(bottom, childCount, right, height);
                    this.mBackgroundFallback.draw(canvas);
                }
            }
        }
    }

    private boolean isOpaque(Drawable childBg) {
        return childBg != null && childBg.getOpacity() == -1;
    }

    private boolean viewsCoverEntireWidth(View view1, View view2, int width) {
        return view1.getLeft() <= 0 && view1.getRight() >= view2.getLeft() && view2.getRight() >= width;
    }
}
