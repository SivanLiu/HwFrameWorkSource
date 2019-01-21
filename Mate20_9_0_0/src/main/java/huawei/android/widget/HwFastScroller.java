package huawei.android.widget;

import android.graphics.Rect;
import android.rms.iaware.AppTypeInfo;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.AbsListView;
import android.widget.FastScrollerEx;
import android.widget.ImageView;
import android.widget.TextView;

public class HwFastScroller extends FastScrollerEx {
    public /* bridge */ /* synthetic */ int getWidth() {
        return super.getWidth();
    }

    public /* bridge */ /* synthetic */ boolean isAlwaysShowEnabled() {
        return super.isAlwaysShowEnabled();
    }

    public /* bridge */ /* synthetic */ boolean isEnabled() {
        return super.isEnabled();
    }

    public /* bridge */ /* synthetic */ boolean onInterceptHoverEvent(MotionEvent x0) {
        return super.onInterceptHoverEvent(x0);
    }

    public /* bridge */ /* synthetic */ boolean onInterceptTouchEvent(MotionEvent x0) {
        return super.onInterceptTouchEvent(x0);
    }

    public /* bridge */ /* synthetic */ void onItemCountChanged(int x0, int x1) {
        super.onItemCountChanged(x0, x1);
    }

    public /* bridge */ /* synthetic */ PointerIcon onResolvePointerIcon(MotionEvent x0, int x1) {
        return super.onResolvePointerIcon(x0, x1);
    }

    public /* bridge */ /* synthetic */ void onScroll(int x0, int x1, int x2) {
        super.onScroll(x0, x1, x2);
    }

    public /* bridge */ /* synthetic */ void onSectionsChanged() {
        super.onSectionsChanged();
    }

    public /* bridge */ /* synthetic */ void onSizeChanged(int x0, int x1, int x2, int x3) {
        super.onSizeChanged(x0, x1, x2, x3);
    }

    public /* bridge */ /* synthetic */ boolean onTouchEvent(MotionEvent x0) {
        return super.onTouchEvent(x0);
    }

    public /* bridge */ /* synthetic */ void remove() {
        super.remove();
    }

    public /* bridge */ /* synthetic */ void setAlwaysShow(boolean x0) {
        super.setAlwaysShow(x0);
    }

    public /* bridge */ /* synthetic */ void setEnabled(boolean x0) {
        super.setEnabled(x0);
    }

    public /* bridge */ /* synthetic */ void setScrollBarStyle(int x0) {
        super.setScrollBarStyle(x0);
    }

    public /* bridge */ /* synthetic */ void setScrollbarPosition(int x0) {
        super.setScrollbarPosition(x0);
    }

    public /* bridge */ /* synthetic */ void setStyle(int x0) {
        super.setStyle(x0);
    }

    public /* bridge */ /* synthetic */ void stop() {
        super.stop();
    }

    public /* bridge */ /* synthetic */ void updateLayout() {
        super.updateLayout();
    }

    public HwFastScroller(AbsListView listView, int styleResId) {
        super(listView, styleResId);
    }

    protected void measureViewToSide(View view, View adjacent, Rect margins, Rect out) {
        int marginLeft;
        int marginTop;
        int marginRight;
        int maxWidth;
        int tmpRight;
        int left;
        int tmpLeft;
        View view2 = view;
        Rect rect = margins;
        boolean isPreviewImage = false;
        if (view2 instanceof TextView) {
            isPreviewImage = true;
        }
        if (rect == null) {
            marginLeft = 0;
            marginTop = 0;
            marginRight = 0;
        } else {
            marginLeft = rect.left;
            marginTop = rect.top;
            marginRight = rect.right;
        }
        Rect container = getContainerRect();
        int containerWidth = container.width();
        int containerHeight = container.height();
        if (adjacent == null) {
            maxWidth = containerWidth;
        } else if (getLayoutFromRight()) {
            maxWidth = adjacent.getLeft();
        } else {
            maxWidth = containerWidth - adjacent.getRight();
        }
        view2.measure(MeasureSpec.makeMeasureSpec((maxWidth - marginLeft) - marginRight, AppTypeInfo.APP_ATTRIBUTE_OVERSEA), MeasureSpec.makeMeasureSpec(0, 0));
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();
        if (getLayoutFromRight()) {
            if (isPreviewImage) {
                tmpRight = (containerWidth / 2) + (width / 2);
                left = tmpRight - width;
            } else {
                tmpRight = (adjacent == null ? container.right : adjacent.getLeft()) - marginRight;
                left = tmpRight - width;
            }
            tmpLeft = left;
            tmpLeft = tmpRight;
        } else if (isPreviewImage) {
            tmpLeft = (containerWidth / 2) + (width / 2);
            left = tmpLeft - width;
        } else {
            left = (adjacent == null ? container.left : adjacent.getRight()) + marginLeft;
            tmpLeft = left + width;
        }
        int left2 = left;
        if (isPreviewImage) {
            tmpRight = (containerHeight / 2) - (height / 2);
            left = tmpRight + height;
        } else {
            tmpRight = marginTop;
            left = tmpRight + view.getMeasuredHeight();
        }
        out.set(left2, tmpRight, tmpLeft, left);
    }

    protected void setThumbPos(float position) {
        ImageView trackImage = getTrackImage();
        ImageView thumbImage = getThumbImage();
        float min = (float) trackImage.getTop();
        thumbImage.setTranslationY(((position * (((float) trackImage.getBottom()) - min)) + min) - (((float) thumbImage.getHeight()) / 2.0f));
    }
}
