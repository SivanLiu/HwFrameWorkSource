package huawei.android.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverCallNotAllowedException;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ListAdapter;
import androidhwext.R;
import huawei.android.widget.RollbackRuleDetector.RollBackScrollListener;
import java.util.HashMap;
import java.util.Map;

public class ExpandableListView extends android.widget.ExpandableListView {
    private static final String CLICL_STATUS_BAR_ACTION = "com.huawei.intent.action.CLICK_STATUSBAR";
    private static final int SCREEN_PAGE_NUMBER = 15;
    private static final int SCROLL_TO_TOP_DURATION = 600;
    private static final String SYSTEMUI_PERMITION = "huawei.permission.CLICK_STATUSBAR_BROADCAST";
    private static final String TAG = "ExpandableListView";
    private Map<Integer, String> allViewOriginalPadding;
    private Context mContext;
    private IntentFilter mFilter;
    private boolean mHasRegistReciver;
    private boolean mHasUsedRollback;
    private HwCutoutUtil mHwCutoutUtil;
    private Context mReceiverContext;
    private RollbackRuleDetector mRollbackRuleDetector;
    private BroadcastReceiver mScrollToTopReceiver;
    private boolean mScrollTopEnable;

    public ExpandableListView(Context context) {
        this(context, null);
    }

    public ExpandableListView(Context context, AttributeSet attrs) {
        this(context, attrs, 16842863);
    }

    public ExpandableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ExpandableListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.allViewOriginalPadding = new HashMap();
        this.mHwCutoutUtil = null;
        this.mScrollTopEnable = false;
        this.mHasRegistReciver = false;
        this.mScrollToTopReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (ExpandableListView.CLICL_STATUS_BAR_ACTION.equals(intent.getAction())) {
                    ExpandableListView.this.post(new Runnable() {
                        public void run() {
                            int num = ExpandableListView.this.getChildCount() * 15;
                            if (ExpandableListView.this.getFirstVisiblePosition() > num) {
                                ExpandableListView.this.setSelection(num);
                            }
                            ExpandableListView.this.smoothScrollToPositionFromTop(0, 0, ExpandableListView.SCROLL_TO_TOP_DURATION);
                            if (!ExpandableListView.this.mHasUsedRollback) {
                                ExpandableListView.this.mRollbackRuleDetector.postScrollUsedEvent();
                                ExpandableListView.this.mHasUsedRollback = true;
                            }
                        }
                    });
                }
            }
        };
        this.mContext = context;
        this.mHwCutoutUtil = new HwCutoutUtil();
        setIgnoreScrollMultiSelectStub();
        if (context.getApplicationContext() != null) {
            this.mReceiverContext = context.getApplicationContext();
        } else {
            this.mReceiverContext = context;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HwExpandableListView, defStyleAttr, defStyleRes);
        this.mScrollTopEnable = a.getBoolean(0, true);
        a.recycle();
        this.mRollbackRuleDetector = new RollbackRuleDetector(new RollBackScrollListener() {
            public int getScrollYDistance() {
                View view = ExpandableListView.this.getChildAt(0);
                if (view != null) {
                    return (ExpandableListView.this.getFirstVisiblePosition() * view.getHeight()) - view.getTop();
                }
                return 0;
            }
        });
    }

    protected void adjustIndicatorLocation(Rect indicatorRect, boolean isLayoutRtl, int indicatorStart, Drawable groupIndicator) {
        if (indicatorRect != null && isLayoutRtl && indicatorStart != 0 && groupIndicator != null) {
            indicatorRect.left = indicatorStart;
            indicatorRect.right = groupIndicator.getIntrinsicWidth() + indicatorStart;
        }
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (this.mHwCutoutUtil != null) {
            this.mHwCutoutUtil.checkCutoutStatus(insets, this, this.mContext);
        }
        return super.onApplyWindowInsets(insets);
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
        ListAdapter adapter = getAdapter();
        int childCount = getChildCount();
        if (this.mHwCutoutUtil != null) {
            this.mHwCutoutUtil.checkViewInCutoutArea(this);
        }
        int mRight = 0;
        int mLeft = 0;
        for (int i = 0; i < childCount; i++) {
            View item = getChildAt(i);
            if (!(item == null || adapter == null)) {
                int viewType = adapter.getItemViewType(getFirstVisiblePosition() + i);
                String originalPadding = (String) this.allViewOriginalPadding.get(Integer.valueOf(viewType));
                if (originalPadding == null) {
                    mLeft = item.getPaddingLeft();
                    mRight = item.getPaddingRight();
                    Map map = this.allViewOriginalPadding;
                    Integer valueOf = Integer.valueOf(viewType);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("");
                    stringBuilder.append(mLeft);
                    stringBuilder.append(":");
                    stringBuilder.append(mRight);
                    map.put(valueOf, stringBuilder.toString());
                } else {
                    String[] singlePadding = originalPadding.split(":");
                    mLeft = Integer.parseInt(singlePadding[0]);
                    mRight = Integer.parseInt(singlePadding[1]);
                }
                if (this.mHwCutoutUtil != null) {
                    this.mHwCutoutUtil.doCutoutPadding(item, mLeft, mRight);
                }
            }
        }
    }

    protected void dispatchDraw(Canvas canvas) {
        if (this.mHwCutoutUtil == null || !this.mHwCutoutUtil.getNeedFitCutout()) {
            super.dispatchDraw(canvas);
            return;
        }
        int rotate = this.mHwCutoutUtil.getDisplayRotate();
        int mCutoutPadding = this.mHwCutoutUtil.getCutoutPadding();
        HwCutoutUtil hwCutoutUtil = this.mHwCutoutUtil;
        boolean noNavigationBar = HwCutoutUtil.isNavigationBarExist(this.mContext) ^ true;
        if (1 == rotate) {
            this.mPaddingLeft += mCutoutPadding;
            super.dispatchDraw(canvas);
            this.mPaddingLeft -= mCutoutPadding;
        } else if (3 == rotate && noNavigationBar) {
            this.mPaddingRight += mCutoutPadding;
            super.dispatchDraw(canvas);
            this.mPaddingRight -= mCutoutPadding;
        } else {
            super.dispatchDraw(canvas);
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerReceiver();
        if (this.mHasRegistReciver) {
            this.mRollbackRuleDetector.start(this);
        }
    }

    public void setScrollTopEnable(boolean scrollTopEnable) {
        if (scrollTopEnable != this.mScrollTopEnable) {
            this.mScrollTopEnable = scrollTopEnable;
            if (scrollTopEnable) {
                registerReceiver();
                if (this.mHasRegistReciver) {
                    this.mRollbackRuleDetector.start(this);
                    return;
                }
                return;
            }
            unregisterReceiver();
            this.mRollbackRuleDetector.stop();
        }
    }

    private void registerReceiver() {
        if (this.mScrollTopEnable && !this.mHasRegistReciver && this.mReceiverContext != null) {
            if (this.mFilter == null) {
                this.mFilter = new IntentFilter(CLICL_STATUS_BAR_ACTION);
            }
            try {
                this.mReceiverContext.registerReceiver(this.mScrollToTopReceiver, this.mFilter, SYSTEMUI_PERMITION, null);
                this.mHasRegistReciver = true;
            } catch (ReceiverCallNotAllowedException e) {
                Log.w(TAG, "There is a problem with the APP application scenario:BroadcastReceiver components are not allowed to register to receive intents");
                this.mHasRegistReciver = false;
            }
        }
    }

    private void unregisterReceiver() {
        if (this.mHasRegistReciver && this.mReceiverContext != null) {
            try {
                this.mReceiverContext.unregisterReceiver(this.mScrollToTopReceiver);
                this.mHasRegistReciver = false;
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver not registered");
            }
        }
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregisterReceiver();
        this.mRollbackRuleDetector.stop();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        this.mRollbackRuleDetector.onTouchEvent(ev);
        return super.onTouchEvent(ev);
    }
}
