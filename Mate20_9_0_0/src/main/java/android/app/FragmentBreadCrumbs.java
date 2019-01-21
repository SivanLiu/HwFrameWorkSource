package android.app;

import android.animation.LayoutTransition;
import android.app.FragmentManager.BackStackEntry;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.R;

@Deprecated
public class FragmentBreadCrumbs extends ViewGroup implements OnBackStackChangedListener {
    private static final int DEFAULT_GRAVITY = 8388627;
    private boolean bArabic;
    Activity mActivity;
    LinearLayout mContainer;
    private int mGravity;
    LayoutInflater mInflater;
    private int mLayoutResId;
    int mMaxVisible;
    private OnBreadCrumbClickListener mOnBreadCrumbClickListener;
    private OnClickListener mOnClickListener;
    private OnClickListener mParentClickListener;
    BackStackRecord mParentEntry;
    private int mTextColor;
    BackStackRecord mTopEntry;

    @Deprecated
    public interface OnBreadCrumbClickListener {
        boolean onBreadCrumbClick(BackStackEntry backStackEntry, int i);
    }

    public FragmentBreadCrumbs(Context context) {
        this(context, null);
    }

    public FragmentBreadCrumbs(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.fragmentBreadCrumbsStyle);
    }

    public FragmentBreadCrumbs(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FragmentBreadCrumbs(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mMaxVisible = -1;
        this.bArabic = isRtlLocale();
        this.mOnClickListener = new OnClickListener() {
            public void onClick(View v) {
                if (v.getTag() instanceof BackStackEntry) {
                    BackStackEntry bse = (BackStackEntry) v.getTag();
                    if (bse != FragmentBreadCrumbs.this.mParentEntry) {
                        if (FragmentBreadCrumbs.this.mOnBreadCrumbClickListener != null) {
                            if (FragmentBreadCrumbs.this.mOnBreadCrumbClickListener.onBreadCrumbClick(bse == FragmentBreadCrumbs.this.mTopEntry ? null : bse, 0)) {
                                return;
                            }
                        }
                        if (bse == FragmentBreadCrumbs.this.mTopEntry) {
                            FragmentBreadCrumbs.this.mActivity.getFragmentManager().popBackStack();
                        } else {
                            FragmentBreadCrumbs.this.mActivity.getFragmentManager().popBackStack(bse.getId(), 0);
                        }
                    } else if (FragmentBreadCrumbs.this.mParentClickListener != null) {
                        FragmentBreadCrumbs.this.mParentClickListener.onClick(v);
                    }
                }
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FragmentBreadCrumbs, defStyleAttr, defStyleRes);
        this.mGravity = a.getInt(0, DEFAULT_GRAVITY);
        this.mLayoutResId = a.getResourceId(2, R.layout.fragment_bread_crumb_item);
        this.mTextColor = a.getColor(1, 0);
        a.recycle();
    }

    public void setActivity(Activity a) {
        this.mActivity = a;
        this.mInflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mContainer = (LinearLayout) this.mInflater.inflate(R.layout.fragment_bread_crumbs, this, false);
        addView(this.mContainer);
        a.getFragmentManager().addOnBackStackChangedListener(this);
        updateCrumbs();
        setLayoutTransition(new LayoutTransition());
    }

    public void setMaxVisible(int visibleCrumbs) {
        if (visibleCrumbs >= 1) {
            this.mMaxVisible = visibleCrumbs;
            return;
        }
        throw new IllegalArgumentException("visibleCrumbs must be greater than zero");
    }

    public void setParentTitle(CharSequence title, CharSequence shortTitle, OnClickListener listener) {
        this.mParentEntry = createBackStackEntry(title, shortTitle);
        this.mParentClickListener = listener;
        updateCrumbs();
    }

    public void setOnBreadCrumbClickListener(OnBreadCrumbClickListener listener) {
        this.mOnBreadCrumbClickListener = listener;
    }

    private BackStackRecord createBackStackEntry(CharSequence title, CharSequence shortTitle) {
        if (title == null) {
            return null;
        }
        BackStackRecord entry = new BackStackRecord((FragmentManagerImpl) this.mActivity.getFragmentManager());
        entry.setBreadCrumbTitle(title);
        entry.setBreadCrumbShortTitle(shortTitle);
        return entry;
    }

    public void setTitle(CharSequence title, CharSequence shortTitle) {
        this.mTopEntry = createBackStackEntry(title, shortTitle);
        updateCrumbs();
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() != 0) {
            int childRight;
            View child = getChildAt(null);
            int childTop = this.mPaddingTop;
            int childBottom = (this.mPaddingTop + child.getMeasuredHeight()) - this.mPaddingBottom;
            int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity & 8388615, getLayoutDirection());
            if (absoluteGravity == 1) {
                absoluteGravity = this.mPaddingLeft + (((this.mRight - this.mLeft) - child.getMeasuredWidth()) / 2);
                childRight = child.getMeasuredWidth() + absoluteGravity;
            } else if (absoluteGravity != 5) {
                absoluteGravity = this.mPaddingLeft;
                childRight = child.getMeasuredWidth() + absoluteGravity;
            } else {
                childRight = (this.mRight - this.mLeft) - this.mPaddingRight;
                absoluteGravity = childRight - child.getMeasuredWidth();
            }
            if (absoluteGravity < this.mPaddingLeft) {
                absoluteGravity = this.mPaddingLeft;
            }
            if (childRight > (this.mRight - this.mLeft) - this.mPaddingRight) {
                childRight = (this.mRight - this.mLeft) - this.mPaddingRight;
            }
            child.layout(absoluteGravity, childTop, childRight, childBottom);
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        int maxHeight = 0;
        int maxWidth = 0;
        int measuredChildState = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
                measuredChildState = combineMeasuredStates(measuredChildState, child.getMeasuredState());
            }
        }
        setMeasuredDimension(resolveSizeAndState(Math.max(maxWidth + (this.mPaddingLeft + this.mPaddingRight), getSuggestedMinimumWidth()), widthMeasureSpec, measuredChildState), resolveSizeAndState(Math.max(maxHeight + (this.mPaddingTop + this.mPaddingBottom), getSuggestedMinimumHeight()), heightMeasureSpec, measuredChildState << 16));
    }

    public void onBackStackChanged() {
        updateCrumbs();
    }

    private int getPreEntryCount() {
        int i = 0;
        int i2 = this.mTopEntry != null ? 1 : 0;
        if (this.mParentEntry != null) {
            i = 1;
        }
        return i2 + i;
    }

    private BackStackEntry getPreEntry(int index) {
        if (this.mParentEntry == null) {
            return this.mTopEntry;
        }
        return index == 0 ? this.mParentEntry : this.mTopEntry;
    }

    void updateCrumbs() {
        int i;
        View v;
        int j;
        FragmentManager fm = this.mActivity.getFragmentManager();
        int numEntries = fm.getBackStackEntryCount();
        int numPreEntries = getPreEntryCount();
        int numViews = this.mContainer.getChildCount();
        for (i = 0; i < numEntries + numPreEntries; i++) {
            BackStackEntry bse;
            if (i < numPreEntries) {
                bse = getPreEntry(i);
            } else {
                bse = fm.getBackStackEntryAt(i - numPreEntries);
            }
            if (i < numViews) {
                if (this.bArabic) {
                    v = this.mContainer.getChildAt(0);
                } else {
                    v = this.mContainer.getChildAt(i);
                }
                if (v.getTag() != bse) {
                    for (j = i; j < numViews; j++) {
                        if (this.bArabic) {
                            this.mContainer.removeViewAt(0);
                        } else {
                            this.mContainer.removeViewAt(i);
                        }
                    }
                    numViews = i;
                }
            }
            if (i >= numViews) {
                v = this.mInflater.inflate(this.mLayoutResId, this, false);
                TextView text = (TextView) v.findViewById(16908310);
                text.setText(bse.getBreadCrumbTitle());
                text.setTag(bse);
                text.setTextColor(this.mTextColor);
                if (i == 0) {
                    v.findViewById(R.id.left_icon).setVisibility(8);
                }
                if (this.bArabic) {
                    this.mContainer.addView(v, 0);
                } else {
                    this.mContainer.addView(v);
                }
                text.setOnClickListener(this.mOnClickListener);
            }
        }
        i = numEntries + numPreEntries;
        numViews = this.mContainer.getChildCount();
        while (numViews > i) {
            this.mContainer.removeViewAt(numViews - 1);
            numViews--;
        }
        int i2 = 0;
        while (i2 < numViews) {
            View findViewById;
            v = this.mContainer.getChildAt(i2);
            boolean z = true;
            if (this.bArabic) {
                findViewById = v.findViewById(16908310);
                if (i2 == 0) {
                    z = false;
                }
                findViewById.setEnabled(z);
            } else {
                findViewById = v.findViewById(16908310);
                if (i2 >= numViews - 1) {
                    z = false;
                }
                findViewById.setEnabled(z);
            }
            if (this.mMaxVisible > 0) {
                v.setVisibility(i2 < numViews - this.mMaxVisible ? 8 : 0);
                findViewById = v.findViewById(R.id.left_icon);
                j = (i2 <= numViews - this.mMaxVisible || i2 == 0) ? 8 : 0;
                findViewById.setVisibility(j);
            }
            i2++;
        }
    }
}
