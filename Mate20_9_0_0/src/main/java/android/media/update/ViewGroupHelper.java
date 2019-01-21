package android.media.update;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public abstract class ViewGroupHelper<T extends ViewGroupProvider> extends ViewGroup {
    public final T mProvider;

    @FunctionalInterface
    public interface ProviderCreator<T extends ViewGroupProvider> {
        T createProvider(ViewGroupHelper<T> viewGroupHelper, ViewGroupProvider viewGroupProvider, ViewGroupProvider viewGroupProvider2);
    }

    public class PrivateProvider implements ViewGroupProvider {
        public CharSequence getAccessibilityClassName_impl() {
            return ViewGroupHelper.this.getAccessibilityClassName();
        }

        public boolean onTouchEvent_impl(MotionEvent ev) {
            return ViewGroupHelper.this.onTouchEvent(ev);
        }

        public boolean onTrackballEvent_impl(MotionEvent ev) {
            return ViewGroupHelper.this.onTrackballEvent(ev);
        }

        public void onFinishInflate_impl() {
            ViewGroupHelper.this.onFinishInflate();
        }

        public void setEnabled_impl(boolean enabled) {
            ViewGroupHelper.this.setEnabled(enabled);
        }

        public void onAttachedToWindow_impl() {
            ViewGroupHelper.this.onAttachedToWindow();
        }

        public void onDetachedFromWindow_impl() {
            ViewGroupHelper.this.onDetachedFromWindow();
        }

        public void onVisibilityAggregated_impl(boolean isVisible) {
            ViewGroupHelper.this.onVisibilityAggregated(isVisible);
        }

        public void onLayout_impl(boolean changed, int left, int top, int right, int bottom) {
            ViewGroupHelper.this.onLayout(changed, left, top, right, bottom);
        }

        public void onMeasure_impl(int widthMeasureSpec, int heightMeasureSpec) {
            ViewGroupHelper.this.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        public int getSuggestedMinimumWidth_impl() {
            return ViewGroupHelper.this.getSuggestedMinimumWidth();
        }

        public int getSuggestedMinimumHeight_impl() {
            return ViewGroupHelper.this.getSuggestedMinimumHeight();
        }

        public void setMeasuredDimension_impl(int measuredWidth, int measuredHeight) {
            ViewGroupHelper.this.setMeasuredDimension(measuredWidth, measuredHeight);
        }

        public boolean dispatchTouchEvent_impl(MotionEvent ev) {
            return ViewGroupHelper.this.dispatchTouchEvent(ev);
        }

        public boolean checkLayoutParams_impl(LayoutParams p) {
            return ViewGroupHelper.this.checkLayoutParams(p);
        }

        public LayoutParams generateDefaultLayoutParams_impl() {
            return ViewGroupHelper.this.generateDefaultLayoutParams();
        }

        public LayoutParams generateLayoutParams_impl(AttributeSet attrs) {
            return ViewGroupHelper.this.generateLayoutParams(attrs);
        }

        public LayoutParams generateLayoutParams_impl(LayoutParams lp) {
            return ViewGroupHelper.this.generateLayoutParams(lp);
        }

        public boolean shouldDelayChildPressedState_impl() {
            return ViewGroupHelper.this.shouldDelayChildPressedState();
        }

        public void measureChildWithMargins_impl(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
            ViewGroupHelper.this.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
        }
    }

    public class SuperProvider implements ViewGroupProvider {
        public CharSequence getAccessibilityClassName_impl() {
            return super.getAccessibilityClassName();
        }

        public boolean onTouchEvent_impl(MotionEvent ev) {
            return super.onTouchEvent(ev);
        }

        public boolean onTrackballEvent_impl(MotionEvent ev) {
            return super.onTrackballEvent(ev);
        }

        public void onFinishInflate_impl() {
            super.onFinishInflate();
        }

        public void setEnabled_impl(boolean enabled) {
            super.setEnabled(enabled);
        }

        public void onAttachedToWindow_impl() {
            super.onAttachedToWindow();
        }

        public void onDetachedFromWindow_impl() {
            super.onDetachedFromWindow();
        }

        public void onVisibilityAggregated_impl(boolean isVisible) {
            super.onVisibilityAggregated(isVisible);
        }

        public void onLayout_impl(boolean changed, int left, int top, int right, int bottom) {
        }

        public void onMeasure_impl(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        public int getSuggestedMinimumWidth_impl() {
            return super.getSuggestedMinimumWidth();
        }

        public int getSuggestedMinimumHeight_impl() {
            return super.getSuggestedMinimumHeight();
        }

        public void setMeasuredDimension_impl(int measuredWidth, int measuredHeight) {
            super.setMeasuredDimension(measuredWidth, measuredHeight);
        }

        public boolean dispatchTouchEvent_impl(MotionEvent ev) {
            return super.dispatchTouchEvent(ev);
        }

        public boolean checkLayoutParams_impl(LayoutParams p) {
            return super.checkLayoutParams(p);
        }

        public LayoutParams generateDefaultLayoutParams_impl() {
            return super.generateDefaultLayoutParams();
        }

        public LayoutParams generateLayoutParams_impl(AttributeSet attrs) {
            return super.generateLayoutParams(attrs);
        }

        public LayoutParams generateLayoutParams_impl(LayoutParams lp) {
            return super.generateLayoutParams(lp);
        }

        public boolean shouldDelayChildPressedState_impl() {
            return super.shouldDelayChildPressedState();
        }

        public void measureChildWithMargins_impl(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
            super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
        }
    }

    public ViewGroupHelper(ProviderCreator<T> creator, Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mProvider = creator.createProvider(this, new SuperProvider(), new PrivateProvider());
    }

    public T getProvider() {
        return this.mProvider;
    }

    protected void onAttachedToWindow() {
        this.mProvider.onAttachedToWindow_impl();
    }

    protected void onDetachedFromWindow() {
        this.mProvider.onDetachedFromWindow_impl();
    }

    public CharSequence getAccessibilityClassName() {
        return this.mProvider.getAccessibilityClassName_impl();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mProvider.onTouchEvent_impl(ev);
    }

    public boolean onTrackballEvent(MotionEvent ev) {
        return this.mProvider.onTrackballEvent_impl(ev);
    }

    public void onFinishInflate() {
        this.mProvider.onFinishInflate_impl();
    }

    public void setEnabled(boolean enabled) {
        this.mProvider.setEnabled_impl(enabled);
    }

    public void onVisibilityAggregated(boolean isVisible) {
        this.mProvider.onVisibilityAggregated_impl(isVisible);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mProvider.onLayout_impl(changed, left, top, right, bottom);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mProvider.onMeasure_impl(widthMeasureSpec, heightMeasureSpec);
    }

    protected int getSuggestedMinimumWidth() {
        return this.mProvider.getSuggestedMinimumWidth_impl();
    }

    protected int getSuggestedMinimumHeight() {
        return this.mProvider.getSuggestedMinimumHeight_impl();
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        return this.mProvider.dispatchTouchEvent_impl(ev);
    }

    protected boolean checkLayoutParams(LayoutParams p) {
        return this.mProvider.checkLayoutParams_impl(p);
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return this.mProvider.generateDefaultLayoutParams_impl();
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return this.mProvider.generateLayoutParams_impl(attrs);
    }

    protected LayoutParams generateLayoutParams(LayoutParams lp) {
        return this.mProvider.generateLayoutParams_impl(lp);
    }

    public boolean shouldDelayChildPressedState() {
        return this.mProvider.shouldDelayChildPressedState_impl();
    }

    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        this.mProvider.measureChildWithMargins_impl(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
    }
}
