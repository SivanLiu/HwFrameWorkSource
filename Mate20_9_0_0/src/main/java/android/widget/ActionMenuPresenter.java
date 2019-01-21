package android.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hwcontrol.HwWidgetFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.ActionProvider;
import android.view.ActionProvider.SubUiVisibilityListener;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.PathInterpolator;
import android.widget.ActionMenuView.ActionMenuChildView;
import com.android.internal.view.ActionBarPolicy;
import com.android.internal.view.menu.ActionMenuItemView;
import com.android.internal.view.menu.ActionMenuItemView.PopupCallback;
import com.android.internal.view.menu.BaseMenuPresenter;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuItemImpl;
import com.android.internal.view.menu.MenuPopupHelper;
import com.android.internal.view.menu.MenuPresenter.Callback;
import com.android.internal.view.menu.MenuView;
import com.android.internal.view.menu.MenuView.ItemView;
import com.android.internal.view.menu.ShowableListMenu;
import com.android.internal.view.menu.SubMenuBuilder;
import java.util.ArrayList;
import java.util.List;

public class ActionMenuPresenter extends BaseMenuPresenter implements SubUiVisibilityListener {
    private static final int ITEM_ANIMATION_DURATION = 150;
    private static final int ITEM_ANIMATION_FADEIN_DURATION = 350;
    private static final int ITEM_ANIMATION_FADEOUT_DURATION = 350;
    private static final int ITEM_ANIMATION_MOVE_DURATION = 350;
    private final SparseBooleanArray mActionButtonGroups = new SparseBooleanArray();
    private ActionButtonSubmenu mActionButtonPopup;
    private int mActionItemWidthLimit;
    private boolean mAnimationEnabled = false;
    private OnAttachStateChangeListener mAttachStateChangeListener = new OnAttachStateChangeListener() {
        public void onViewAttachedToWindow(View v) {
        }

        public void onViewDetachedFromWindow(View v) {
            ((View) ActionMenuPresenter.this.mMenuView).getViewTreeObserver().removeOnPreDrawListener(ActionMenuPresenter.this.mItemAnimationPreDrawListener);
            ActionMenuPresenter.this.mPreLayoutItems.clear();
            ActionMenuPresenter.this.mPostLayoutItems.clear();
        }
    };
    private boolean mExpandedActionViewsExclusive;
    private View mHwOverflowButton;
    private OnPreDrawListener mItemAnimationPreDrawListener = new OnPreDrawListener() {
        public boolean onPreDraw() {
            ActionMenuPresenter.this.computeMenuItemAnimationInfo(false);
            ((View) ActionMenuPresenter.this.mMenuView).getViewTreeObserver().removeOnPreDrawListener(this);
            if (ActionMenuPresenter.this.getIsToolBarMode()) {
                ActionMenuPresenter.this.runItemAnimations();
            }
            return true;
        }
    };
    private int mMaxItems;
    private boolean mMaxItemsSet;
    private int mMinCellSize;
    int mOpenSubMenuId;
    private OverflowPopup mOverflowPopup;
    private Drawable mPendingOverflowIcon;
    private boolean mPendingOverflowIconSet;
    private ActionMenuPopupCallback mPopupCallback;
    protected int mPopupEndLocation;
    final PopupPresenterCallback mPopupPresenterCallback = new PopupPresenterCallback(this, null);
    protected int mPopupStartLocation;
    private SparseArray<MenuItemLayoutInfo> mPostLayoutItems = new SparseArray();
    private OpenOverflowRunnable mPostedOpenRunnable;
    private SparseArray<MenuItemLayoutInfo> mPreLayoutItems = new SparseArray();
    private boolean mReserveOverflow;
    private boolean mReserveOverflowSet;
    private List<ItemAnimationInfo> mRunningItemAnimations = new ArrayList();
    private boolean mStrictWidthLimit;
    private boolean mToolbarAttachOverlay = false;
    private int mWidthLimit;
    private boolean mWidthLimitSet;

    private static class ItemAnimationInfo {
        static final int FADE_IN = 1;
        static final int FADE_OUT = 2;
        static final int MOVE = 0;
        int animType;
        Animator animator;
        int id;
        MenuItemLayoutInfo menuItemLayoutInfo;

        ItemAnimationInfo(int id, MenuItemLayoutInfo info, Animator anim, int animType) {
            this.id = id;
            this.menuItemLayoutInfo = info;
            this.animator = anim;
            this.animType = animType;
        }
    }

    private static class MenuItemLayoutInfo {
        int left;
        int location_x;
        int location_y;
        int top;
        View view;

        MenuItemLayoutInfo(View view, boolean preLayout) {
            this.left = view.getLeft();
            this.top = view.getTop();
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            this.location_x = location[0];
            this.location_y = location[1];
            if (preLayout) {
                this.left = (int) (((float) this.left) + view.getTranslationX());
                this.top = (int) (((float) this.top) + view.getTranslationY());
            }
            this.view = view;
        }
    }

    private class OpenOverflowRunnable implements Runnable {
        private OverflowPopup mPopup;

        public OpenOverflowRunnable(OverflowPopup popup) {
            this.mPopup = popup;
        }

        public void run() {
            if (ActionMenuPresenter.this.mMenu != null) {
                ActionMenuPresenter.this.mMenu.changeMenuMode();
            }
            View menuView = (View) ActionMenuPresenter.this.mMenuView;
            if (!(menuView == null || menuView.getWindowToken() == null || !this.mPopup.tryShow())) {
                ActionMenuPresenter.this.mOverflowPopup = this.mPopup;
            }
            ActionMenuPresenter.this.mPostedOpenRunnable = null;
        }
    }

    private class ActionMenuPopupCallback extends PopupCallback {
        private ActionMenuPopupCallback() {
        }

        /* synthetic */ ActionMenuPopupCallback(ActionMenuPresenter x0, AnonymousClass1 x1) {
            this();
        }

        public ShowableListMenu getPopup() {
            return ActionMenuPresenter.this.mActionButtonPopup != null ? ActionMenuPresenter.this.mActionButtonPopup.getPopup() : null;
        }
    }

    private class PopupPresenterCallback implements Callback {
        private PopupPresenterCallback() {
        }

        /* synthetic */ PopupPresenterCallback(ActionMenuPresenter x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            boolean z = false;
            if (subMenu == null) {
                return false;
            }
            ActionMenuPresenter.this.mOpenSubMenuId = ((SubMenuBuilder) subMenu).getItem().getItemId();
            Callback cb = ActionMenuPresenter.this.getCallback();
            if (cb != null) {
                z = cb.onOpenSubMenu(subMenu);
            }
            return z;
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            if (menu instanceof SubMenuBuilder) {
                menu.getRootMenu().close(false);
            }
            Callback cb = ActionMenuPresenter.this.getCallback();
            if (cb != null) {
                cb.onCloseMenu(menu, allMenusAreClosing);
            }
        }
    }

    private static class SavedState implements Parcelable {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        public int openSubMenuId;

        SavedState() {
        }

        SavedState(Parcel in) {
            this.openSubMenuId = in.readInt();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.openSubMenuId);
        }
    }

    public class ActionButtonSubmenu extends MenuPopupHelper {
        public ActionButtonSubmenu(Context context, SubMenuBuilder subMenu, View anchorView) {
            super(context, subMenu, anchorView, false, 16843844);
            if (!((MenuItemImpl) subMenu.getItem()).isActionButton()) {
                setAnchorView(ActionMenuPresenter.this.mHwOverflowButton == null ? (View) ActionMenuPresenter.this.mMenuView : ActionMenuPresenter.this.mHwOverflowButton);
            }
            setPresenterCallback(ActionMenuPresenter.this.mPopupPresenterCallback);
        }

        protected void onDismiss() {
            ActionMenuPresenter.this.mActionButtonPopup = null;
            ActionMenuPresenter.this.mOpenSubMenuId = 0;
            super.onDismiss();
        }
    }

    public class OverflowPopup extends MenuPopupHelper {
        public OverflowPopup(Context context, MenuBuilder menu, View anchorView, boolean overflowOnly) {
            super(context, menu, anchorView, overflowOnly, 16843844);
            ActionMenuPresenter.this.setPopupGravity(this);
            setPresenterCallback(ActionMenuPresenter.this.mPopupPresenterCallback);
        }

        protected void onDismiss() {
            if (ActionMenuPresenter.this.mMenu != null) {
                ActionMenuPresenter.this.mMenu.close();
            }
            ActionMenuPresenter.this.mOverflowPopup = null;
            super.onDismiss();
        }
    }

    public class OverflowMenuButton extends ImageButton implements ActionMenuChildView {
        private final float[] mTempPts = new float[2];

        public OverflowMenuButton(Context context) {
            super(context, null, 16843510);
            setClickable(true);
            setFocusable(true);
            setVisibility(0);
            setEnabled(true);
            setOnTouchListener(new ForwardingListener(this, ActionMenuPresenter.this) {
                public ShowableListMenu getPopup() {
                    if (ActionMenuPresenter.this.mOverflowPopup == null) {
                        return null;
                    }
                    return ActionMenuPresenter.this.mOverflowPopup.getPopup();
                }

                public boolean onForwardingStarted() {
                    ActionMenuPresenter.this.showOverflowMenu();
                    return true;
                }

                public boolean onForwardingStopped() {
                    if (ActionMenuPresenter.this.mPostedOpenRunnable != null) {
                        return false;
                    }
                    ActionMenuPresenter.this.hideOverflowMenu();
                    return true;
                }
            });
        }

        public boolean performClick() {
            if (super.performClick()) {
                return true;
            }
            playSoundEffect(0);
            ActionMenuPresenter.this.showOverflowMenu();
            return true;
        }

        public boolean needsDividerBefore() {
            return false;
        }

        public boolean needsDividerAfter() {
            return false;
        }

        public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfoInternal(info);
            info.setCanOpenPopup(true);
        }

        protected boolean setFrame(int l, int t, int r, int b) {
            boolean changed = super.setFrame(l, t, r, b);
            Drawable d = getDrawable();
            Drawable bg = getBackground();
            if (!(d == null || bg == null)) {
                int width = getWidth();
                int height = getHeight();
                int halfEdge = Math.max(width, height) / 2;
                int centerX = (width + (getPaddingLeft() - getPaddingRight())) / 2;
                int centerY = (height + (getPaddingTop() - getPaddingBottom())) / 2;
                bg.setHotspotBounds(centerX - halfEdge, centerY - halfEdge, centerX + halfEdge, centerY + halfEdge);
            }
            return changed;
        }
    }

    public ActionMenuPresenter(Context context) {
        super(context, 17367071, 17367070);
    }

    public ActionMenuPresenter(Context context, int menuLayout, int itemLayout) {
        super(context, menuLayout, itemLayout);
    }

    public void initForMenu(Context context, MenuBuilder menu) {
        super.initForMenu(context, menu);
        Resources res = context.getResources();
        ActionBarPolicy abp = ActionBarPolicy.get(context);
        if (!this.mReserveOverflowSet) {
            this.mReserveOverflow = abp.showsOverflowMenuButton();
        }
        if (!this.mWidthLimitSet) {
            this.mWidthLimit = abp.getEmbeddedMenuWidthLimit();
        }
        if (!this.mMaxItemsSet) {
            this.mMaxItems = abp.getMaxActionButtons();
            this.mMaxItems = getMaxActionButtons(this.mMaxItems);
        }
        int width = this.mWidthLimit;
        if (this.mReserveOverflow) {
            if (this.mHwOverflowButton == null) {
                this.mHwOverflowButton = HwWidgetFactory.getHwOverflowMenuButton(this.mSystemContext, this);
                if (this.mHwOverflowButton == null) {
                    this.mHwOverflowButton = new OverflowMenuButton(this.mSystemContext);
                }
                if (this.mPendingOverflowIconSet) {
                    if (this.mHwOverflowButton instanceof OverflowMenuButton) {
                        ((OverflowMenuButton) this.mHwOverflowButton).setImageDrawable(this.mPendingOverflowIcon);
                    }
                    this.mPendingOverflowIcon = null;
                    this.mPendingOverflowIconSet = false;
                }
                int spec = MeasureSpec.makeMeasureSpec(0, 0);
                this.mHwOverflowButton.measure(spec, spec);
            }
            width -= this.mHwOverflowButton.getMeasuredWidth();
        } else {
            this.mHwOverflowButton = null;
        }
        this.mActionItemWidthLimit = width;
        this.mMinCellSize = (int) (56.0f * res.getDisplayMetrics().density);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (!this.mMaxItemsSet) {
            this.mMaxItems = ActionBarPolicy.get(this.mContext).getMaxActionButtons();
            this.mMaxItems = getMaxActionButtons(this.mMaxItems);
        }
        if (HwWidgetFactory.isHwTheme(this.mSystemContext)) {
            this.mHwOverflowButton = null;
        }
        if (this.mMenu != null) {
            this.mMenu.onItemsChanged(true);
        }
    }

    public void setWidthLimit(int width, boolean strict) {
        this.mWidthLimit = width;
        this.mStrictWidthLimit = strict;
        this.mWidthLimitSet = true;
    }

    public void setReserveOverflow(boolean reserveOverflow) {
        this.mReserveOverflow = reserveOverflow;
        this.mReserveOverflowSet = true;
    }

    public void setItemLimit(int itemCount) {
        this.mMaxItems = itemCount;
        this.mMaxItemsSet = true;
    }

    public void setExpandedActionViewsExclusive(boolean isExclusive) {
        this.mExpandedActionViewsExclusive = isExclusive;
    }

    public void setOverflowIcon(Drawable icon) {
        if (this.mHwOverflowButton == null) {
            this.mPendingOverflowIconSet = true;
            this.mPendingOverflowIcon = icon;
        } else if (this.mHwOverflowButton instanceof OverflowMenuButton) {
            ((OverflowMenuButton) this.mHwOverflowButton).setImageDrawable(icon);
        }
    }

    public Drawable getOverflowIcon() {
        if (this.mHwOverflowButton != null) {
            if (this.mHwOverflowButton instanceof OverflowMenuButton) {
                return ((OverflowMenuButton) this.mHwOverflowButton).getDrawable();
            }
        } else if (this.mPendingOverflowIconSet) {
            return this.mPendingOverflowIcon;
        }
        return null;
    }

    public MenuView getMenuView(ViewGroup root) {
        MenuView oldMenuView = this.mMenuView;
        MenuView result = super.getMenuView(root);
        if (oldMenuView != result) {
            ((ActionMenuView) result).setPresenter(this);
            if (oldMenuView != null) {
                ((View) oldMenuView).removeOnAttachStateChangeListener(this.mAttachStateChangeListener);
            }
            ((View) result).addOnAttachStateChangeListener(this.mAttachStateChangeListener);
        }
        return result;
    }

    public View getItemView(MenuItemImpl item, View convertView, ViewGroup parent) {
        View actionView = item.getActionView();
        if (actionView == null || item.hasCollapsibleActionView()) {
            actionView = super.getItemView(item, convertView, parent);
        }
        actionView.setVisibility(item.isActionViewExpanded() ? 8 : 0);
        ActionMenuView menuParent = (ActionMenuView) parent;
        LayoutParams lp = actionView.getLayoutParams();
        if (!menuParent.checkLayoutParams(lp)) {
            actionView.setLayoutParams(menuParent.generateLayoutParams(lp));
        }
        return actionView;
    }

    public void bindItemView(MenuItemImpl item, ItemView itemView) {
        itemView.initialize(item, 0);
        ActionMenuItemView actionItemView = (ActionMenuItemView) itemView;
        actionItemView.setItemInvoker(this.mMenuView);
        if (this.mPopupCallback == null) {
            this.mPopupCallback = new ActionMenuPopupCallback(this, null);
        }
        actionItemView.setPopupCallback(this.mPopupCallback);
    }

    public boolean shouldIncludeItem(int childIndex, MenuItemImpl item) {
        return item.isActionButton();
    }

    public boolean getToolBarAttachOverlay() {
        return getIsToolBarMode() && this.mToolbarAttachOverlay;
    }

    private boolean getIsToolBarMode() {
        if (this.mMenuView != null) {
            View parent = (View) ((View) this.mMenuView).getParent();
            if (parent != null && parent.getId() == 16909357) {
                return true;
            }
        }
        return false;
    }

    private View getAnimateOverlayRootView() {
        if (this.mMenuView != null) {
            View parent = (View) ((View) this.mMenuView).getParent();
            if (parent != null && parent.getId() == 16909357) {
                return (View) parent.getParent();
            }
        }
        return null;
    }

    private void computeMenuItemAnimationInfo(boolean preLayout) {
        ViewGroup menuView = this.mMenuView;
        int count = menuView.getChildCount();
        SparseArray items = preLayout ? this.mPreLayoutItems : this.mPostLayoutItems;
        if (preLayout) {
            this.mPreLayoutItems.clear();
        } else {
            this.mPostLayoutItems.clear();
        }
        for (int i = 0; i < count; i++) {
            View child = menuView.getChildAt(i);
            int id = child.getId();
            if (!((id <= 0 && id != -1) || child.getWidth() == 0 || child.getHeight() == 0)) {
                items.put(id, new MenuItemLayoutInfo(child, preLayout));
            }
        }
    }

    private void runItemAnimations() {
        int i;
        final int id;
        int j;
        int i2 = 0;
        for (i = 0; i < this.mPreLayoutItems.size(); i++) {
            id = this.mPreLayoutItems.keyAt(i);
            final MenuItemLayoutInfo menuItemLayoutInfoPre = (MenuItemLayoutInfo) this.mPreLayoutItems.get(id);
            int postLayoutIndex = this.mPostLayoutItems.indexOfKey(id);
            ObjectAnimator anim;
            if (postLayoutIndex >= 0) {
                MenuItemLayoutInfo menuItemLayoutInfoPost = (MenuItemLayoutInfo) this.mPostLayoutItems.valueAt(postLayoutIndex);
                PropertyValuesHolder pvhX = null;
                PropertyValuesHolder pvhY = null;
                if (menuItemLayoutInfoPre.left != menuItemLayoutInfoPost.left) {
                    pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, new float[]{(float) (menuItemLayoutInfoPre.left - menuItemLayoutInfoPost.left), 0.0f});
                }
                if (menuItemLayoutInfoPre.top != menuItemLayoutInfoPost.top) {
                    pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, new float[]{(float) (menuItemLayoutInfoPre.top - menuItemLayoutInfoPost.top), 0.0f});
                }
                if (!(pvhX == null && pvhY == null)) {
                    for (j = 0; j < this.mRunningItemAnimations.size(); j++) {
                        ItemAnimationInfo oldInfo = (ItemAnimationInfo) this.mRunningItemAnimations.get(j);
                        if (oldInfo.id == id && oldInfo.animType == 0) {
                            oldInfo.animator.cancel();
                        }
                    }
                    if (pvhX == null) {
                        anim = ObjectAnimator.ofPropertyValuesHolder(menuItemLayoutInfoPost.view, new PropertyValuesHolder[]{pvhY});
                    } else if (pvhY != null) {
                        anim = ObjectAnimator.ofPropertyValuesHolder(menuItemLayoutInfoPost.view, new PropertyValuesHolder[]{pvhX, pvhY});
                    } else {
                        anim = ObjectAnimator.ofPropertyValuesHolder(menuItemLayoutInfoPost.view, new PropertyValuesHolder[]{pvhX});
                    }
                    anim.setDuration(350);
                    anim.setInterpolator(new PathInterpolator(0.3f, 0.15f, 0.1f, 0.85f));
                    anim.start();
                    this.mRunningItemAnimations.add(new ItemAnimationInfo(id, menuItemLayoutInfoPost, anim, 0));
                    anim.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            for (int j = 0; j < ActionMenuPresenter.this.mRunningItemAnimations.size(); j++) {
                                if (((ItemAnimationInfo) ActionMenuPresenter.this.mRunningItemAnimations.get(j)).animator == animation) {
                                    ActionMenuPresenter.this.mRunningItemAnimations.remove(j);
                                    return;
                                }
                            }
                        }
                    });
                }
                this.mPostLayoutItems.remove(id);
            } else {
                float oldAlpha = 1.0f;
                for (j = 0; j < this.mRunningItemAnimations.size(); j++) {
                    ItemAnimationInfo oldInfo2 = (ItemAnimationInfo) this.mRunningItemAnimations.get(j);
                    if (oldInfo2.id == id && oldInfo2.animType == 1) {
                        oldAlpha = oldInfo2.menuItemLayoutInfo.view.getAlpha();
                        oldInfo2.animator.cancel();
                    }
                }
                anim = ObjectAnimator.ofFloat(menuItemLayoutInfoPre.view, View.ALPHA, new float[]{oldAlpha, 0.0f});
                if (id == -1) {
                    this.mToolbarAttachOverlay = true;
                } else {
                    ((ActionMenuItemView) menuItemLayoutInfoPre.view).setToolBarAttachOverlay(true);
                }
                final ViewGroup container = (ViewGroup) getAnimateOverlayRootView();
                if (container != null) {
                    container.getOverlay().add(menuItemLayoutInfoPre.view);
                    this.mRunningItemAnimations.add(new ItemAnimationInfo(id, menuItemLayoutInfoPre, anim, 2));
                    anim.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationStart(Animator animation) {
                            int[] location = new int[2];
                            container.getLocationOnScreen(location);
                            menuItemLayoutInfoPre.view.setX((float) (menuItemLayoutInfoPre.location_x - location[0]));
                            menuItemLayoutInfoPre.view.setY((float) (menuItemLayoutInfoPre.location_y - location[1]));
                        }

                        public void onAnimationEnd(Animator animation) {
                            for (int j = 0; j < ActionMenuPresenter.this.mRunningItemAnimations.size(); j++) {
                                if (((ItemAnimationInfo) ActionMenuPresenter.this.mRunningItemAnimations.get(j)).animator == animation) {
                                    ActionMenuPresenter.this.mRunningItemAnimations.remove(j);
                                    break;
                                }
                            }
                            menuItemLayoutInfoPre.view.setTranslationY(0.0f);
                            if (id == -1) {
                                ActionMenuPresenter.this.mToolbarAttachOverlay = false;
                            } else {
                                ((ActionMenuItemView) menuItemLayoutInfoPre.view).setToolBarAttachOverlay(false);
                            }
                            if (container != null) {
                                container.getOverlay().remove(menuItemLayoutInfoPre.view);
                            }
                        }
                    });
                    anim.setDuration(350);
                    anim.setInterpolator(new PathInterpolator(0.3f, 0.15f, 0.1f, 0.85f));
                    anim.start();
                }
            }
        }
        i = 0;
        while (i < this.mPostLayoutItems.size()) {
            id = this.mPostLayoutItems.keyAt(i);
            j = this.mPostLayoutItems.indexOfKey(id);
            if (j >= 0) {
                MenuItemLayoutInfo menuItemLayoutInfo = (MenuItemLayoutInfo) this.mPostLayoutItems.valueAt(j);
                float oldAlpha2 = 0.0f;
                for (int j2 = i2; j2 < this.mRunningItemAnimations.size(); j2++) {
                    ItemAnimationInfo oldInfo3 = (ItemAnimationInfo) this.mRunningItemAnimations.get(j2);
                    if (oldInfo3.id == id && oldInfo3.animType == 2) {
                        oldAlpha2 = oldInfo3.menuItemLayoutInfo.view.getAlpha();
                        oldInfo3.animator.cancel();
                    }
                }
                ObjectAnimator anim2 = ObjectAnimator.ofFloat(menuItemLayoutInfo.view, View.ALPHA, new float[]{oldAlpha2, 1.0f});
                anim2.start();
                anim2.setDuration(350);
                anim2.setInterpolator(new PathInterpolator(0.3f, 0.15f, 0.1f, 0.85f));
                this.mRunningItemAnimations.add(new ItemAnimationInfo(id, menuItemLayoutInfo, anim2, 1));
                anim2.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        for (int j = 0; j < ActionMenuPresenter.this.mRunningItemAnimations.size(); j++) {
                            if (((ItemAnimationInfo) ActionMenuPresenter.this.mRunningItemAnimations.get(j)).animator == animation) {
                                ActionMenuPresenter.this.mRunningItemAnimations.remove(j);
                                return;
                            }
                        }
                    }
                });
            }
            i++;
            i2 = 0;
        }
    }

    private void setupItemAnimations() {
        computeMenuItemAnimationInfo(true);
        ((View) this.mMenuView).getViewTreeObserver().addOnPreDrawListener(this.mItemAnimationPreDrawListener);
    }

    public void updateMenuView(boolean cleared) {
        ArrayList<MenuItemImpl> actionItems;
        int i;
        if (((ViewGroup) ((View) this.mMenuView).getParent()) != null && this.mAnimationEnabled) {
            setupItemAnimations();
        }
        boolean z = false;
        if (this.mActionButtonPopup == null || !this.mActionButtonPopup.isShowing()) {
            this.mIsReuse = false;
        } else {
            this.mIsReuse = true;
        }
        super.updateMenuView(cleared);
        ((View) this.mMenuView).requestLayout();
        if (this.mMenu != null) {
            actionItems = this.mMenu.getActionItems();
            int count = actionItems.size();
            for (i = 0; i < count; i++) {
                ActionProvider provider = ((MenuItemImpl) actionItems.get(i)).getActionProvider();
                if (provider != null) {
                    provider.setSubUiVisibilityListener(this);
                }
            }
        }
        actionItems = this.mMenu != null ? this.mMenu.getNonActionItems() : null;
        boolean hasOverflow = false;
        if (this.mReserveOverflow && actionItems != null) {
            i = actionItems.size();
            if (i == 1) {
                hasOverflow = ((MenuItemImpl) actionItems.get(0)).isActionViewExpanded() ^ 1;
            } else {
                if (i > 0) {
                    z = true;
                }
                hasOverflow = z;
            }
        }
        if (hasOverflow) {
            if (this.mHwOverflowButton == null) {
                this.mHwOverflowButton = HwWidgetFactory.getHwOverflowMenuButton(this.mSystemContext, this);
                if (this.mHwOverflowButton == null) {
                    this.mHwOverflowButton = new OverflowMenuButton(this.mSystemContext);
                }
            }
            ViewGroup parent = (ViewGroup) this.mHwOverflowButton.getParent();
            if (parent != this.mMenuView) {
                if (parent != null) {
                    parent.removeView(this.mHwOverflowButton);
                }
                ActionMenuView menuView = this.mMenuView;
                menuView.addView(this.mHwOverflowButton, (LayoutParams) menuView.generateOverflowButtonLayoutParams());
            }
        } else if (this.mHwOverflowButton != null && this.mHwOverflowButton.getParent() == this.mMenuView) {
            ((ViewGroup) this.mMenuView).removeView(this.mHwOverflowButton);
        }
        ((ActionMenuView) this.mMenuView).setOverflowReserved(this.mReserveOverflow);
    }

    public boolean filterLeftoverView(ViewGroup parent, int childIndex) {
        if (parent.getChildAt(childIndex) == this.mHwOverflowButton) {
            return false;
        }
        return super.filterLeftoverView(parent, childIndex);
    }

    public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
        int i = 0;
        if (!subMenu.hasVisibleItems()) {
            return false;
        }
        SubMenuBuilder topSubMenu = subMenu;
        while (topSubMenu.getParentMenu() != this.mMenu) {
            topSubMenu = (SubMenuBuilder) topSubMenu.getParentMenu();
        }
        View anchor = findViewForItem(topSubMenu.getItem());
        if (anchor == null) {
            return false;
        }
        this.mOpenSubMenuId = subMenu.getItem().getItemId();
        boolean preserveIconSpacing = false;
        int count = subMenu.size();
        while (i < count) {
            MenuItem childItem = subMenu.getItem(i);
            if (childItem.isVisible() && childItem.getIcon() != null) {
                preserveIconSpacing = true;
                break;
            }
            i++;
        }
        this.mActionButtonPopup = new ActionButtonSubmenu(this.mContext, subMenu, anchor);
        this.mActionButtonPopup.setForceShowIcon(preserveIconSpacing);
        this.mActionButtonPopup.show();
        super.onSubMenuSelected(subMenu);
        return true;
    }

    private View findViewForItem(MenuItem item) {
        ViewGroup parent = this.mMenuView;
        if (parent == null) {
            return null;
        }
        int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = parent.getChildAt(i);
            if ((child instanceof ItemView) && ((ItemView) child).getItemData() == item) {
                return child;
            }
        }
        return null;
    }

    public boolean showOverflowMenu() {
        if (!this.mReserveOverflow || isOverflowMenuShowing() || this.mMenu == null || this.mMenuView == null || this.mPostedOpenRunnable != null || this.mMenu.getNonActionItems().isEmpty() || this.mHwOverflowButton == null) {
            return false;
        }
        OverflowPopup overflowPopup = new OverflowPopup(this.mContext, this.mMenu, this.mHwOverflowButton, true);
        MenuPopupWindow menuPopupWindow = overflowPopup.getPopup().getMenuPopup();
        if (menuPopupWindow != null) {
            menuPopupWindow.mPopup.setPopupLocation(this.mPopupStartLocation, this.mPopupEndLocation);
        }
        this.mPostedOpenRunnable = new OpenOverflowRunnable(overflowPopup);
        ((View) this.mMenuView).post(this.mPostedOpenRunnable);
        super.onSubMenuSelected(null);
        return true;
    }

    public boolean hideOverflowMenu() {
        if (this.mPostedOpenRunnable == null || this.mMenuView == null) {
            MenuPopupHelper popup = this.mOverflowPopup;
            if (popup == null) {
                return false;
            }
            popup.dismiss();
            return true;
        }
        ((View) this.mMenuView).removeCallbacks(this.mPostedOpenRunnable);
        this.mPostedOpenRunnable = null;
        return true;
    }

    public boolean dismissPopupMenus() {
        return hideOverflowMenu() | hideSubMenus();
    }

    public boolean hideSubMenus() {
        if (this.mActionButtonPopup == null) {
            return false;
        }
        this.mActionButtonPopup.dismiss();
        return true;
    }

    public boolean isOverflowMenuShowing() {
        return this.mOverflowPopup != null && this.mOverflowPopup.isShowing();
    }

    public boolean isOverflowMenuShowPending() {
        return this.mPostedOpenRunnable != null || isOverflowMenuShowing();
    }

    public boolean isOverflowReserved() {
        return this.mReserveOverflow;
    }

    public boolean flagActionItems() {
        ArrayList<MenuItemImpl> visibleItems;
        int itemsSize;
        int itemsSize2;
        int requiredItems;
        ViewGroup parent;
        ActionMenuPresenter actionMenuPresenter = this;
        if (actionMenuPresenter.mMenu != null) {
            visibleItems = actionMenuPresenter.mMenu.getVisibleItems();
            itemsSize = visibleItems.size();
        } else {
            visibleItems = null;
            itemsSize = 0;
        }
        int maxActions = actionMenuPresenter.mMaxItems;
        int widthLimit = actionMenuPresenter.mActionItemWidthLimit;
        int querySpec = MeasureSpec.makeMeasureSpec(0, 0);
        ViewGroup parent2 = actionMenuPresenter.mMenuView;
        int requiredItems2 = 0;
        int requestedItems = 0;
        int firstActionWidth = 0;
        boolean hasOverflow = false;
        int maxActions2 = maxActions;
        for (maxActions = 0; maxActions < itemsSize; maxActions++) {
            MenuItemImpl item = (MenuItemImpl) visibleItems.get(maxActions);
            if (item.requiresActionButton()) {
                requiredItems2++;
            } else if (item.requestsActionButton()) {
                requestedItems++;
            } else {
                hasOverflow = true;
            }
            if (actionMenuPresenter.mExpandedActionViewsExclusive && item.isActionViewExpanded()) {
                maxActions2 = 0;
            }
        }
        if (actionMenuPresenter.mReserveOverflow && (hasOverflow || requiredItems2 + requestedItems > maxActions2)) {
            maxActions2--;
        }
        maxActions2 -= requiredItems2;
        SparseBooleanArray seenGroups = actionMenuPresenter.mActionButtonGroups;
        seenGroups.clear();
        int cellSize = 0;
        int cellsRemaining = 0;
        if (actionMenuPresenter.mStrictWidthLimit) {
            cellsRemaining = widthLimit / actionMenuPresenter.mMinCellSize;
            cellSize = actionMenuPresenter.mMinCellSize + ((widthLimit % actionMenuPresenter.mMinCellSize) / cellsRemaining);
        }
        int i = 0;
        while (i < itemsSize) {
            ArrayList<MenuItemImpl> visibleItems2;
            MenuItemImpl item2 = (MenuItemImpl) visibleItems.get(i);
            itemsSize2 = itemsSize;
            View v;
            if (item2.requiresActionButton()) {
                int widthLimit2;
                v = actionMenuPresenter.getItemView(item2, null, parent2);
                requiredItems = requiredItems2;
                if (actionMenuPresenter.mStrictWidthLimit != 0) {
                    cellsRemaining -= ActionMenuView.measureChildForCells(v, cellSize, cellsRemaining, querySpec, 0);
                } else {
                    v.measure(querySpec, querySpec);
                }
                requiredItems2 = v.getMeasuredWidth();
                widthLimit -= requiredItems2;
                if (firstActionWidth == 0) {
                    firstActionWidth = requiredItems2;
                }
                itemsSize = item2.getGroupId();
                if (itemsSize != 0) {
                    widthLimit2 = widthLimit;
                    widthLimit = 1;
                    seenGroups.put(itemsSize, true);
                } else {
                    widthLimit2 = widthLimit;
                    widthLimit = 1;
                }
                item2.setIsActionButton(widthLimit);
                visibleItems2 = visibleItems;
                parent = parent2;
                widthLimit = widthLimit2;
            } else {
                requiredItems = requiredItems2;
                if (item2.requestsActionButton()) {
                    requiredItems2 = item2.getGroupId();
                    boolean inGroup = seenGroups.get(requiredItems2);
                    boolean isAction = (maxActions2 > 0 || inGroup) && widthLimit > 0 && (!actionMenuPresenter.mStrictWidthLimit || cellsRemaining > 0);
                    boolean isAction2;
                    if (isAction) {
                        int cells;
                        isAction2 = isAction;
                        v = actionMenuPresenter.getItemView(item2, null, parent2);
                        parent = parent2;
                        if (actionMenuPresenter.mStrictWidthLimit != null) {
                            cells = ActionMenuView.measureChildForCells(v, cellSize, cellsRemaining, querySpec, 0);
                            cellsRemaining -= cells;
                            if (cells == 0) {
                                parent2 = null;
                            } else {
                                parent2 = isAction2;
                            }
                        } else {
                            v.measure(querySpec, querySpec);
                            parent2 = isAction2;
                        }
                        cells = v.getMeasuredWidth();
                        widthLimit -= cells;
                        if (firstActionWidth == 0) {
                            firstActionWidth = cells;
                        }
                        if (actionMenuPresenter.mStrictWidthLimit != null) {
                            isAction = (widthLimit >= 0) & parent2;
                        } else {
                            isAction = (widthLimit + firstActionWidth > 0 ? 1 : 0) & parent2;
                        }
                    } else {
                        isAction2 = isAction;
                        parent = parent2;
                    }
                    if (!isAction || requiredItems2 == 0) {
                        if (inGroup) {
                            seenGroups.put(requiredItems2, false);
                            parent2 = null;
                            while (parent2 < i) {
                                MenuItemImpl areYouMyGroupie = (MenuItemImpl) visibleItems.get(parent2);
                                visibleItems2 = visibleItems;
                                if (areYouMyGroupie.getGroupId() == requiredItems2) {
                                    if (areYouMyGroupie.isActionButton()) {
                                        maxActions2++;
                                    }
                                    areYouMyGroupie.setIsActionButton(false);
                                }
                                parent2++;
                                visibleItems = visibleItems2;
                            }
                        }
                        visibleItems2 = visibleItems;
                    } else {
                        seenGroups.put(requiredItems2, true);
                        visibleItems2 = visibleItems;
                    }
                    if (isAction) {
                        maxActions2--;
                    }
                    item2.setIsActionButton(isAction);
                } else {
                    visibleItems2 = visibleItems;
                    parent = parent2;
                    item2.setIsActionButton(false);
                    i++;
                    itemsSize = itemsSize2;
                    requiredItems2 = requiredItems;
                    parent2 = parent;
                    visibleItems = visibleItems2;
                    actionMenuPresenter = this;
                }
            }
            i++;
            itemsSize = itemsSize2;
            requiredItems2 = requiredItems;
            parent2 = parent;
            visibleItems = visibleItems2;
            actionMenuPresenter = this;
        }
        itemsSize2 = itemsSize;
        parent = parent2;
        requiredItems = requiredItems2;
        return true;
    }

    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        dismissPopupMenus();
        super.onCloseMenu(menu, allMenusAreClosing);
    }

    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState();
        state.openSubMenuId = this.mOpenSubMenuId;
        return state;
    }

    public void onRestoreInstanceState(Parcelable state) {
        SavedState saved = (SavedState) state;
        if (saved.openSubMenuId > 0) {
            MenuItem item = this.mMenu.findItem(saved.openSubMenuId);
            if (item != null) {
                onSubMenuSelected((SubMenuBuilder) item.getSubMenu());
            }
        }
    }

    public void onSubUiVisibilityChanged(boolean isVisible) {
        if (isVisible) {
            super.onSubMenuSelected(null);
        } else if (this.mMenu != null) {
            this.mMenu.close(false);
        }
    }

    public void setMenuView(ActionMenuView menuView) {
        if (menuView != this.mMenuView) {
            if (this.mMenuView != null) {
                ((View) this.mMenuView).removeOnAttachStateChangeListener(this.mAttachStateChangeListener);
            }
            this.mMenuView = menuView;
            menuView.initialize(this.mMenu);
            menuView.addOnAttachStateChangeListener(this.mAttachStateChangeListener);
        }
    }

    public OverflowPopup getOverflowPopup() {
        return this.mOverflowPopup;
    }

    public ActionButtonSubmenu getActionButtonPopup() {
        return this.mActionButtonPopup;
    }

    public OpenOverflowRunnable getPostedOpenRunnable() {
        return this.mPostedOpenRunnable;
    }

    public int getOpenSubMenuId() {
        return this.mOpenSubMenuId;
    }

    protected View getOverflowButton() {
        return this.mHwOverflowButton;
    }

    protected void setPopupGravity(MenuPopupHelper mph) {
        mph.setGravity(Gravity.END);
    }

    protected int getMaxActionButtons(int maxItems) {
        return maxItems;
    }

    public void showOverflowMenuPending() {
    }

    public void setPopupLocation(int start, int end) {
    }
}
