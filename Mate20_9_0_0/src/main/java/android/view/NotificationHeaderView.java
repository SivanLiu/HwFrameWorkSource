package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.RemoteViews.RemoteView;
import com.android.internal.widget.CachingIconView;
import java.util.ArrayList;

@RemoteView
public class NotificationHeaderView extends ViewGroup {
    public static final int NO_COLOR = 1;
    private boolean mAcceptAllTouches;
    private View mAppName;
    private View mAppOps;
    private OnClickListener mAppOpsListener;
    private Drawable mBackground;
    private View mCameraIcon;
    private final int mChildMinWidth;
    private final int mContentEndMargin;
    private boolean mEntireHeaderClickable;
    private ImageView mExpandButton;
    private OnClickListener mExpandClickListener;
    private boolean mExpandOnlyOnButton;
    private boolean mExpanded;
    private final int mGravity;
    private View mHeaderText;
    private CachingIconView mIcon;
    private int mIconColor;
    private View mMicIcon;
    private int mOriginalNotificationColor;
    private View mOverlayIcon;
    private View mProfileBadge;
    private int mProgressBarNotificationColor;
    ViewOutlineProvider mProvider;
    private View mSecondaryHeaderText;
    private boolean mShowExpandButtonAtEnd;
    private boolean mShowWorkBadgeAtEnd;
    private int mTotalWidth;
    private HeaderTouchListener mTouchListener;

    public class HeaderTouchListener implements OnTouchListener {
        private Rect mAppOpsRect;
        private float mDownX;
        private float mDownY;
        private Rect mExpandButtonRect;
        private final ArrayList<Rect> mTouchRects = new ArrayList();
        private int mTouchSlop;
        private boolean mTrackGesture;

        public void bindTouchRects() {
            this.mTouchRects.clear();
            addRectAroundView(NotificationHeaderView.this.mIcon);
            this.mExpandButtonRect = addRectAroundView(NotificationHeaderView.this.mExpandButton);
            this.mAppOpsRect = addRectAroundView(NotificationHeaderView.this.mAppOps);
            addWidthRect();
            this.mTouchSlop = ViewConfiguration.get(NotificationHeaderView.this.getContext()).getScaledTouchSlop();
        }

        private void addWidthRect() {
            Rect r = new Rect();
            r.top = 0;
            r.bottom = (int) (32.0f * NotificationHeaderView.this.getResources().getDisplayMetrics().density);
            r.left = 0;
            r.right = NotificationHeaderView.this.getWidth();
            this.mTouchRects.add(r);
        }

        private Rect addRectAroundView(View view) {
            Rect r = getRectAroundView(view);
            this.mTouchRects.add(r);
            return r;
        }

        private Rect getRectAroundView(View view) {
            float size = 48.0f * NotificationHeaderView.this.getResources().getDisplayMetrics().density;
            float width = Math.max(size, (float) view.getWidth());
            float height = Math.max(size, (float) view.getHeight());
            Rect r = new Rect();
            if (view.getVisibility() == 8) {
                view = NotificationHeaderView.this.getFirstChildNotGone();
                r.left = (int) (((float) view.getLeft()) - (width / 2.0f));
            } else {
                r.left = (int) ((((float) (view.getLeft() + view.getRight())) / 2.0f) - (width / 2.0f));
            }
            r.top = (int) ((((float) (view.getTop() + view.getBottom())) / 2.0f) - (height / 2.0f));
            r.bottom = (int) (((float) r.top) + height);
            r.right = (int) (((float) r.left) + width);
            return r;
        }

        public boolean onTouch(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getActionMasked() & 255) {
                case 0:
                    this.mTrackGesture = false;
                    if (isInside(x, y)) {
                        this.mDownX = x;
                        this.mDownY = y;
                        this.mTrackGesture = true;
                        return true;
                    }
                    break;
                case 1:
                    if (this.mTrackGesture) {
                        if (!NotificationHeaderView.this.mAppOps.isVisibleToUser() || (!this.mAppOpsRect.contains((int) x, (int) y) && !this.mAppOpsRect.contains((int) this.mDownX, (int) this.mDownY))) {
                            NotificationHeaderView.this.mExpandButton.performClick();
                            break;
                        }
                        NotificationHeaderView.this.mAppOps.performClick();
                        return true;
                    }
                    break;
                case 2:
                    if (this.mTrackGesture && (Math.abs(this.mDownX - x) > ((float) this.mTouchSlop) || Math.abs(this.mDownY - y) > ((float) this.mTouchSlop))) {
                        this.mTrackGesture = false;
                        break;
                    }
            }
            return this.mTrackGesture;
        }

        private boolean isInside(float x, float y) {
            if (NotificationHeaderView.this.mAcceptAllTouches) {
                return true;
            }
            if (NotificationHeaderView.this.mExpandOnlyOnButton) {
                return this.mExpandButtonRect.contains((int) x, (int) y);
            }
            for (int i = 0; i < this.mTouchRects.size(); i++) {
                if (((Rect) this.mTouchRects.get(i)).contains((int) x, (int) y)) {
                    return true;
                }
            }
            return false;
        }
    }

    public NotificationHeaderView(Context context) {
        this(context, null);
    }

    public NotificationHeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationHeaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTouchListener = new HeaderTouchListener();
        this.mProvider = new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                if (NotificationHeaderView.this.mBackground != null) {
                    outline.setRect(0, 0, NotificationHeaderView.this.getWidth(), NotificationHeaderView.this.getHeight());
                    outline.setAlpha(1.0f);
                }
            }
        };
        Resources res = getResources();
        this.mChildMinWidth = res.getDimensionPixelSize(17105228);
        this.mContentEndMargin = res.getDimensionPixelSize(17105206);
        this.mEntireHeaderClickable = res.getBoolean(17956998);
        TypedArray ta = context.obtainStyledAttributes(attrs, new int[]{16842927}, defStyleAttr, defStyleRes);
        this.mGravity = ta.getInt(0, 0);
        ta.recycle();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAppName = findViewById(16908734);
        this.mHeaderText = findViewById(16908953);
        this.mSecondaryHeaderText = findViewById(16908955);
        this.mExpandButton = (ImageView) findViewById(16908876);
        this.mIcon = (CachingIconView) findViewById(16908294);
        this.mProfileBadge = findViewById(16909220);
        this.mCameraIcon = findViewById(16908792);
        this.mMicIcon = findViewById(16909083);
        this.mOverlayIcon = findViewById(16909171);
        this.mAppOps = findViewById(16908735);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        int givenWidth = MeasureSpec.getSize(widthMeasureSpec);
        int givenHeight = MeasureSpec.getSize(heightMeasureSpec);
        int wrapContentWidthSpec = MeasureSpec.makeMeasureSpec(givenWidth, Integer.MIN_VALUE);
        int wrapContentHeightSpec = MeasureSpec.makeMeasureSpec(givenHeight, Integer.MIN_VALUE);
        int totalWidth = getPaddingStart() + getPaddingEnd();
        for (i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                child.measure(ViewGroup.getChildMeasureSpec(wrapContentWidthSpec, lp.leftMargin + lp.rightMargin, lp.width), ViewGroup.getChildMeasureSpec(wrapContentHeightSpec, lp.topMargin + lp.bottomMargin, lp.height));
                totalWidth += (lp.leftMargin + lp.rightMargin) + child.getMeasuredWidth();
            }
        }
        if (totalWidth > givenWidth) {
            i = shrinkViewForOverflow(wrapContentHeightSpec, shrinkViewForOverflow(wrapContentHeightSpec, totalWidth - givenWidth, this.mAppName, this.mChildMinWidth), this.mHeaderText, 0);
            if (this.mSecondaryHeaderText != null) {
                shrinkViewForOverflow(wrapContentHeightSpec, i, this.mSecondaryHeaderText, 0);
            }
        }
        this.mTotalWidth = Math.min(totalWidth, givenWidth);
        setMeasuredDimension(givenWidth, givenHeight);
    }

    private int shrinkViewForOverflow(int heightSpec, int overFlow, View targetView, int minimumWidth) {
        int oldWidth = targetView.getMeasuredWidth();
        if (overFlow <= 0 || targetView.getVisibility() == 8 || oldWidth <= minimumWidth) {
            return overFlow;
        }
        int newSize = Math.max(minimumWidth, oldWidth - overFlow);
        targetView.measure(MeasureSpec.makeMeasureSpec(newSize, Integer.MIN_VALUE), heightSpec);
        return overFlow - (oldWidth - newSize);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingStart();
        int end = getMeasuredWidth();
        int i = 0;
        if ((this.mGravity & 1) != 0) {
            left += (getMeasuredWidth() / 2) - (this.mTotalWidth / 2);
        }
        int childCount = getChildCount();
        int ownHeight = (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom();
        while (i < childCount) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                int measuredWidth;
                int childHeight = child.getMeasuredHeight();
                MarginLayoutParams params = (MarginLayoutParams) child.getLayoutParams();
                left += params.getMarginStart();
                int left2 = child.getMeasuredWidth() + left;
                int top = (int) (((float) getPaddingTop()) + (((float) (ownHeight - childHeight)) / 2.0f));
                int bottom = top + childHeight;
                int layoutLeft = left;
                int layoutRight = left2;
                if (child == this.mExpandButton && this.mShowExpandButtonAtEnd) {
                    layoutRight = end - this.mContentEndMargin;
                    measuredWidth = layoutRight - child.getMeasuredWidth();
                    layoutLeft = measuredWidth;
                    end = measuredWidth;
                }
                if (child == this.mProfileBadge) {
                    measuredWidth = getPaddingEnd();
                    if (this.mShowWorkBadgeAtEnd != 0) {
                        measuredWidth = this.mContentEndMargin;
                    }
                    layoutRight = end - measuredWidth;
                    left = layoutRight - child.getMeasuredWidth();
                    layoutLeft = left;
                    end = left;
                }
                if (child == this.mAppOps) {
                    layoutRight = end - this.mContentEndMargin;
                    measuredWidth = layoutRight - child.getMeasuredWidth();
                    layoutLeft = measuredWidth;
                    end = measuredWidth;
                }
                if (getLayoutDirection() == 1) {
                    left = layoutLeft;
                    layoutLeft = getWidth() - layoutRight;
                    layoutRight = getWidth() - left;
                }
                child.layout(layoutLeft, top, layoutRight, bottom);
                left = left2 + params.getMarginEnd();
            }
            i++;
        }
        updateTouchListener();
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    public void setHeaderBackgroundDrawable(Drawable drawable) {
        if (drawable != null) {
            setWillNotDraw(false);
            this.mBackground = drawable;
            this.mBackground.setCallback(this);
            setOutlineProvider(this.mProvider);
        } else {
            setWillNotDraw(true);
            this.mBackground = null;
            setOutlineProvider(null);
        }
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        if (this.mBackground != null) {
            this.mBackground.setBounds(0, 0, getWidth(), getHeight());
            this.mBackground.draw(canvas);
        }
    }

    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mBackground;
    }

    protected void drawableStateChanged() {
        if (this.mBackground != null && this.mBackground.isStateful()) {
            this.mBackground.setState(getDrawableState());
        }
    }

    private void updateTouchListener() {
        if (this.mExpandClickListener == null && this.mAppOpsListener == null) {
            setOnTouchListener(null);
            return;
        }
        setOnTouchListener(this.mTouchListener);
        this.mTouchListener.bindTouchRects();
    }

    public void setAppOpsOnClickListener(OnClickListener l) {
        this.mAppOpsListener = l;
        this.mAppOps.setOnClickListener(this.mAppOpsListener);
        this.mCameraIcon.setOnClickListener(this.mAppOpsListener);
        this.mMicIcon.setOnClickListener(this.mAppOpsListener);
        this.mOverlayIcon.setOnClickListener(this.mAppOpsListener);
        updateTouchListener();
    }

    public void setOnClickListener(OnClickListener l) {
        this.mExpandClickListener = l;
        this.mExpandButton.setOnClickListener(this.mExpandClickListener);
        updateTouchListener();
    }

    @RemotableViewMethod
    public void setOriginalIconColor(int color) {
        this.mIconColor = color;
    }

    public int getOriginalIconColor() {
        return this.mIconColor;
    }

    @RemotableViewMethod
    public void setOriginalNotificationColor(int color) {
        this.mOriginalNotificationColor = color;
    }

    public int getOriginalNotificationColor() {
        return this.mOriginalNotificationColor;
    }

    @RemotableViewMethod
    public void setExpanded(boolean expanded) {
        this.mExpanded = expanded;
        updateExpandButton();
    }

    public void showAppOpsIcons(ArraySet<Integer> appOps) {
        if (this.mOverlayIcon != null && this.mCameraIcon != null && this.mMicIcon != null && appOps != null) {
            int i = 8;
            this.mOverlayIcon.setVisibility(appOps.contains(Integer.valueOf(24)) ? 0 : 8);
            this.mCameraIcon.setVisibility(appOps.contains(Integer.valueOf(26)) ? 0 : 8);
            View view = this.mMicIcon;
            if (appOps.contains(Integer.valueOf(27))) {
                i = 0;
            }
            view.setVisibility(i);
        }
    }

    private void updateExpandButton() {
        int drawableId;
        int contentDescriptionId;
        if (this.mExpanded) {
            drawableId = 17302324;
            contentDescriptionId = 17040007;
        } else {
            drawableId = 17302381;
            contentDescriptionId = 17040006;
        }
        this.mExpandButton.setImageDrawable(getContext().getDrawable(drawableId));
        this.mExpandButton.setColorFilter(this.mOriginalNotificationColor);
        this.mExpandButton.setContentDescription(this.mContext.getText(contentDescriptionId));
    }

    public void setShowWorkBadgeAtEnd(boolean showWorkBadgeAtEnd) {
        if (showWorkBadgeAtEnd != this.mShowWorkBadgeAtEnd) {
            setClipToPadding(showWorkBadgeAtEnd ^ 1);
            this.mShowWorkBadgeAtEnd = showWorkBadgeAtEnd;
        }
    }

    public void setShowExpandButtonAtEnd(boolean showExpandButtonAtEnd) {
        if (showExpandButtonAtEnd != this.mShowExpandButtonAtEnd) {
            setClipToPadding(showExpandButtonAtEnd ^ 1);
            this.mShowExpandButtonAtEnd = showExpandButtonAtEnd;
        }
    }

    public View getWorkProfileIcon() {
        return this.mProfileBadge;
    }

    public CachingIconView getIcon() {
        return this.mIcon;
    }

    private View getFirstChildNotGone() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                return child;
            }
        }
        return this;
    }

    public ImageView getExpandButton() {
        return this.mExpandButton;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public boolean isInTouchRect(float x, float y) {
        if (this.mExpandClickListener == null) {
            return false;
        }
        return this.mTouchListener.isInside(x, y);
    }

    @RemotableViewMethod
    public void setAcceptAllTouches(boolean acceptAllTouches) {
        boolean z = this.mEntireHeaderClickable || acceptAllTouches;
        this.mAcceptAllTouches = z;
    }

    @RemotableViewMethod
    public void setExpandOnlyOnButton(boolean expandOnlyOnButton) {
        this.mExpandOnlyOnButton = expandOnlyOnButton;
    }
}
