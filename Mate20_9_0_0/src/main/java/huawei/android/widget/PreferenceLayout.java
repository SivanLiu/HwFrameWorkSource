package huawei.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import huawei.android.widget.loader.ResLoaderUtil;

public class PreferenceLayout extends LinearLayout {
    private static final String TAG = "PreferenceLayout";
    private View mDetailView;
    private View mIconView;
    private View mTitleView;
    private View mWidgetView;

    public PreferenceLayout(Context context) {
        this(context, null);
    }

    public PreferenceLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreferenceLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void addView(View child, int index, LayoutParams params) {
        super.addView(child, index, params);
        int id = child.getId();
        if (id == ResLoaderUtil.getViewId(getContext(), "icon_frame")) {
            this.mIconView = findViewById(ResLoaderUtil.getViewId(getContext(), "icon_frame"));
        } else if (id == ResLoaderUtil.getViewId(getContext(), "title_frame")) {
            this.mTitleView = findViewById(ResLoaderUtil.getViewId(getContext(), "title_frame"));
        } else if (id == ResLoaderUtil.getViewId(getContext(), "detail")) {
            this.mDetailView = findViewById(ResLoaderUtil.getViewId(getContext(), "detail"));
        } else if (id == 16908312) {
            this.mWidgetView = findViewById(16908312);
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i = widthMeasureSpec;
        int i2 = heightMeasureSpec;
        if (this.mTitleView == null || this.mDetailView == null || this.mIconView == null || this.mWidgetView == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mTitleView = ");
            stringBuilder.append(this.mTitleView);
            stringBuilder.append(" , mDetailView = ");
            stringBuilder.append(this.mDetailView);
            stringBuilder.append(" , mIconView = ");
            stringBuilder.append(this.mIconView);
            stringBuilder.append(" , mWidgetView = ");
            stringBuilder.append(this.mWidgetView);
            Log.w(str, stringBuilder.toString());
            return;
        }
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        this.mTitleView.measure(0, i2);
        this.mDetailView.measure(0, i2);
        int titleWidthOrigin = this.mTitleView.getMeasuredWidth();
        int detailWidthOrigin = this.mDetailView.getMeasuredWidth();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        LinearLayout.LayoutParams titleParams = (LinearLayout.LayoutParams) this.mTitleView.getLayoutParams();
        int titleHeightMeasureSpec = getChildMeasureSpec(i2, ((getPaddingTop() + getPaddingBottom()) + titleParams.topMargin) + titleParams.bottomMargin, titleParams.height);
        LinearLayout.LayoutParams detailParams = (LinearLayout.LayoutParams) this.mDetailView.getLayoutParams();
        int detailHeightMeasureSpec = getChildMeasureSpec(i2, ((getPaddingTop() + getPaddingBottom()) + detailParams.topMargin) + detailParams.bottomMargin, detailParams.height);
        int iconWidth = this.mIconView.getMeasuredWidth();
        int availableSpace = (((widthSize - getPaddingStart()) - getPaddingEnd()) - iconWidth) - this.mWidgetView.getMeasuredWidth();
        int maxTitleWidthWithDetail = (availableSpace * 2) / 3;
        if (this.mTitleView.getVisibility() == 0 && this.mDetailView.getVisibility() == 0) {
            if (titleWidthOrigin > maxTitleWidthWithDetail) {
                this.mTitleView.measure(MeasureSpec.makeMeasureSpec(maxTitleWidthWithDetail, 1073741824), titleHeightMeasureSpec);
                this.mDetailView.measure(MeasureSpec.makeMeasureSpec(availableSpace - maxTitleWidthWithDetail, 1073741824), detailHeightMeasureSpec);
            }
            if (titleWidthOrigin + detailWidthOrigin > availableSpace) {
                if (titleWidthOrigin > availableSpace - maxTitleWidthWithDetail && titleWidthOrigin <= maxTitleWidthWithDetail) {
                    adjustHeight(i, titleWidthOrigin, availableSpace - titleWidthOrigin);
                } else if (titleWidthOrigin <= availableSpace - maxTitleWidthWithDetail) {
                    adjustHeight(i, availableSpace - maxTitleWidthWithDetail, maxTitleWidthWithDetail);
                }
            } else if (detailWidthOrigin > availableSpace - maxTitleWidthWithDetail) {
                this.mTitleView.measure(MeasureSpec.makeMeasureSpec(availableSpace - detailWidthOrigin, 1073741824), titleHeightMeasureSpec);
                this.mDetailView.measure(MeasureSpec.makeMeasureSpec(detailWidthOrigin, 1073741824), detailHeightMeasureSpec);
            }
        }
    }

    private void adjustHeight(int widthMeasureSpec, int titleWidth, int detailWidth) {
        this.mTitleView.measure(MeasureSpec.makeMeasureSpec(titleWidth, 1073741824), 0);
        this.mDetailView.measure(MeasureSpec.makeMeasureSpec(detailWidth, 1073741824), 0);
        int heightSpec = MeasureSpec.makeMeasureSpec((this.mTitleView.getMeasuredHeight() > this.mDetailView.getMeasuredHeight() ? this.mTitleView : this.mDetailView).getMeasuredHeight(), 1073741824);
        this.mIconView.measure(0, heightSpec);
        this.mWidgetView.measure(0, heightSpec);
        setMeasuredDimension(widthMeasureSpec, heightSpec);
    }
}
