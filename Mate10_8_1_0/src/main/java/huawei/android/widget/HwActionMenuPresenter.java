package huawei.android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.ActionMenuPresenter;
import android.widget.ActionMenuPresenter.ActionButtonSubmenu;
import android.widget.ForwardingListener;
import android.widget.ListPopupWindow;
import com.android.internal.view.menu.MenuItemImpl;
import com.android.internal.view.menu.MenuPopup;
import com.android.internal.view.menu.MenuPopupHelper;
import com.android.internal.view.menu.SubMenuBuilder;
import huawei.android.widget.DecouplingUtil.ReflectUtil;

public class HwActionMenuPresenter extends ActionMenuPresenter {
    private static final boolean DEBUG = true;
    private static final String TAG = "HwActionMenuPresenter";
    private OnPreDrawListener mOverflowMenuPreDrawListener = new OnPreDrawListener() {
        public boolean onPreDraw() {
            View over = HwActionMenuPresenter.this.getOverflowButton();
            if (over != null) {
                over.getViewTreeObserver().removeOnPreDrawListener(this);
            }
            if (HwActionMenuPresenter.this.mShowOverflowMenuPending) {
                HwActionMenuPresenter.this.mShowOverflowMenuPending = false;
                HwActionMenuPresenter.this.showOverflowMenu();
            }
            return HwActionMenuPresenter.DEBUG;
        }
    };
    private boolean mShowOverflowMenuPending = false;

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
        public int overflowMenuShownInt;

        SavedState() {
        }

        SavedState(Parcel in) {
            this.openSubMenuId = in.readInt();
            this.overflowMenuShownInt = in.readInt();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.openSubMenuId);
            dest.writeInt(this.overflowMenuShownInt);
        }
    }

    public HwActionMenuPresenter(Context context) {
        super(context);
        Log.d(TAG, "new HwActionMenuPresenter");
    }

    public HwActionMenuPresenter(Context context, int menuLayout, int itemLayout) {
        super(context, menuLayout, itemLayout);
        Log.d(TAG, "new HwActionMenuPresenter with 3 param");
    }

    public boolean isPopupMenuShowing() {
        return getParentActionButtonPopup() != null ? getParentActionButtonPopup().isShowing() : false;
    }

    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState();
        state.openSubMenuId = getParrentOpenSubMenuId();
        state.overflowMenuShownInt = isOverflowMenuShowing() ? 1 : 0;
        return state;
    }

    public void onRestoreInstanceState(Parcelable state) {
        SavedState saved = (SavedState) state;
        if (saved.openSubMenuId > 0 && this.mMenu != null) {
            MenuItem item = this.mMenu.findItem(saved.openSubMenuId);
            if (item != null) {
                onSubMenuSelected((SubMenuBuilder) item.getSubMenu());
            }
        }
        if (saved.overflowMenuShownInt > 0 ? DEBUG : false) {
            showOverflowMenuPending();
        }
    }

    public void showOverflowMenuPending() {
        this.mShowOverflowMenuPending = DEBUG;
    }

    public void updateMenuView(boolean cleared) {
        super.updateMenuView(cleared);
        View over = getOverflowButton();
        if (over != null) {
            ViewTreeObserver vto = over.getViewTreeObserver();
            vto.removeOnPreDrawListener(this.mOverflowMenuPreDrawListener);
            vto.addOnPreDrawListener(this.mOverflowMenuPreDrawListener);
        }
    }

    private int getParrentOpenSubMenuId() {
        Object obj = ReflectUtil.getObject(this, "mOpenSubMenuId", ActionMenuPresenter.class);
        return obj != null ? ((Integer) obj).intValue() : 0;
    }

    private ActionButtonSubmenu getParentActionButtonPopup() {
        return (ActionButtonSubmenu) ReflectUtil.getObject(this, "mActionButtonPopup", ActionMenuPresenter.class);
    }

    private View getParrentOverflowButton() {
        return (View) ReflectUtil.getObject(this, "mOverflowButton", ActionMenuPresenter.class);
    }

    public View getItemView(final MenuItemImpl item, View convertView, ViewGroup parent) {
        View actionView = super.getItemView(item, convertView, parent);
        if (item.hasSubMenu()) {
            actionView.setOnTouchListener(new ForwardingListener(actionView) {
                public ListPopupWindow getPopup() {
                    if (HwActionMenuPresenter.this.getParentActionButtonPopup() != null) {
                        MenuPopup mp = HwActionMenuPresenter.this.getParentActionButtonPopup().getPopup();
                        if (mp != null) {
                            return mp.getMenuPopup();
                        }
                    }
                    return null;
                }

                protected boolean onForwardingStarted() {
                    return HwActionMenuPresenter.this.onSubMenuSelected((SubMenuBuilder) item.getSubMenu());
                }

                protected boolean onForwardingStopped() {
                    return HwActionMenuPresenter.this.dismissPopupMenus();
                }

                public boolean onTouch(View v, MotionEvent event) {
                    boolean z = false;
                    if (this.mForwarding && getPopup() != null) {
                        z = HwActionMenuPresenter.DEBUG;
                    }
                    this.mForwarding = z;
                    return super.onTouch(v, event);
                }
            });
        } else {
            actionView.setOnTouchListener(null);
        }
        return actionView;
    }

    public void setOverflowIcon(Drawable icon) {
        if (!(getOverflowButton() instanceof HwOverflowMenuButton)) {
            super.setOverflowIcon(icon);
        }
    }

    public Drawable getOverflowIcon() {
        if (getOverflowButton() instanceof HwOverflowMenuButton) {
            return null;
        }
        return super.getOverflowIcon();
    }

    protected void setPopupGravity(MenuPopupHelper mph) {
        mph.setGravity(0);
    }

    protected int getMaxActionButtons(int maxItems) {
        return 5;
    }

    public void setPopupLocation(int start, int end) {
        this.mPopupStartLocation = start;
        this.mPopupEndLocation = end;
    }
}
