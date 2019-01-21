package huawei.android.view;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.AnimatorSet.Builder;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.Log;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.widget.ActionMenuPresenter;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuItemImpl;
import huawei.android.widget.DecouplingUtil.ReflectUtil;
import huawei.android.widget.HwActionMenuPresenter;
import huawei.android.widget.HwToolbar;
import huawei.android.widget.loader.ResLoader;

public class HwSearchAnimationUtils {
    private static final boolean DBG = true;
    private static final int ENTER_SEARCH = 1;
    private static final int EXIT_SEARCH = 0;
    private static final String TAG = "HwSearchAnimationUtils";

    private static class ViewWrapper {
        private LayoutParams mLayoutParams;
        private int mState;
        private View mTarget;

        ViewWrapper(View target, int state) {
            this.mTarget = target;
            this.mState = state;
            this.mLayoutParams = target.getLayoutParams();
        }

        public int getWidth() {
            return this.mLayoutParams.width;
        }

        public void setWidth(int width) {
            this.mLayoutParams.width = width;
            this.mTarget.requestLayout();
            String str = HwSearchAnimationUtils.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ViewWrapper.setWidth: state=");
            stringBuilder.append(this.mState);
            stringBuilder.append(", view=");
            stringBuilder.append(this.mTarget.toString());
            stringBuilder.append(", input width=");
            stringBuilder.append(width);
            stringBuilder.append(", effect width=");
            stringBuilder.append(this.mTarget.getWidth());
            Log.d(str, stringBuilder.toString());
        }

        public int getHeight() {
            return this.mLayoutParams.height;
        }

        public void setHeight(int height) {
            this.mLayoutParams.height = height;
            this.mTarget.requestLayout();
            String str = HwSearchAnimationUtils.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ViewWrapper.setWidth: state=");
            stringBuilder.append(this.mState);
            stringBuilder.append(", view=");
            stringBuilder.append(this.mTarget.toString());
            stringBuilder.append(", input height=");
            stringBuilder.append(height);
            stringBuilder.append(", effect height=");
            stringBuilder.append(this.mTarget.getHeight());
            Log.d(str, stringBuilder.toString());
        }
    }

    public static Animator getSearchFixedOpenAnimator(Context context, View maskView) {
        int resId = ResLoader.getInstance().getIdentifier(context, "animator", "search_mask");
        ObjectAnimator objectAnimator = null;
        if (resId != 0) {
            objectAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(context, resId);
            if (objectAnimator != null) {
                objectAnimator.setTarget(maskView);
            }
        } else {
            Log.w(TAG, "search_mask animator file not found");
        }
        return objectAnimator;
    }

    public static Animator getSearchFixedCloseAnimator(Context context, View maskView) {
        int resId = ResLoader.getInstance().getIdentifier(context, "animator", "search_unmask");
        ObjectAnimator objectAnimator = null;
        if (resId != 0) {
            objectAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(context, resId);
            if (objectAnimator != null) {
                objectAnimator.setTarget(maskView);
            }
        } else {
            Log.w(TAG, "search_unmask animator file not found");
        }
        return objectAnimator;
    }

    public static Animator getSearchBoxAlwaysOnAppbarOpenAnimator(Context context, View enterView, View exitView) {
        ResLoader resLoader = ResLoader.getInstance();
        int openEnterResId = resLoader.getIdentifier(context, "animator", "search_open_enter");
        int openExitResId = resLoader.getIdentifier(context, "animator", "search_open_exit");
        if (openEnterResId == 0 || openExitResId == 0) {
            Log.w(TAG, "search_open animator file not found");
            return null;
        }
        ((ObjectAnimator) AnimatorInflater.loadAnimator(context, openEnterResId)).setTarget(enterView);
        ((ObjectAnimator) AnimatorInflater.loadAnimator(context, openExitResId)).setTarget(exitView);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(new Animator[]{openEnterAnimator, openExitAnimator});
        return animatorSet;
    }

    public static Animator getSearchBoxAlwaysOnAppbarCloseAnimator(Context context, View enterView, View exitView) {
        ResLoader resLoader = ResLoader.getInstance();
        int closeEnterResId = resLoader.getIdentifier(context, "animator", "search_close_enter");
        int closeExitResId = resLoader.getIdentifier(context, "animator", "search_close_exit");
        if (closeEnterResId == 0 || closeExitResId == 0) {
            Log.w(TAG, "search_close animator file not found");
            return null;
        }
        ((ObjectAnimator) AnimatorInflater.loadAnimator(context, closeEnterResId)).setTarget(enterView);
        ((ObjectAnimator) AnimatorInflater.loadAnimator(context, closeExitResId)).setTarget(exitView);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(new Animator[]{closeEnterAnimator, closeExitAnimator});
        return animatorSet;
    }

    public static OnActionExpandListener getAlphaAnimatedOnActionExpandListener() {
        return new OnActionExpandListener() {
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (item == null) {
                    Log.w(HwSearchAnimationUtils.TAG, "MenuItem is null");
                    return false;
                }
                View actionView = item.getActionView();
                AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
                alphaAnimation.setDuration(50);
                if (actionView != null) {
                    actionView.startAnimation(alphaAnimation);
                }
                return HwSearchAnimationUtils.DBG;
            }

            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (item == null) {
                    Log.w(HwSearchAnimationUtils.TAG, "MenuItem is null");
                    return false;
                }
                View actionView = item.getActionView();
                AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
                alphaAnimation.setDuration(50);
                if (actionView != null) {
                    actionView.startAnimation(alphaAnimation);
                }
                return HwSearchAnimationUtils.DBG;
            }
        };
    }

    public static Animator getSearchBelowActionBarSearchAnimator(Context context, MenuItem searchMenuItem, HwToolbar toolbar, View search, View mask, View holder, View tmpCollapseBtn) {
        Exception e;
        int i;
        int i2;
        ViewWrapper viewWrapper;
        int aniTimeLong;
        Context context2 = context;
        View view = toolbar;
        View view2 = search;
        View view3 = tmpCollapseBtn;
        AnimatorSet animatorSet = new AnimatorSet();
        ViewWrapper searchWrapper = new ViewWrapper(view2, 1);
        ViewWrapper holderWrapper = new ViewWrapper(holder, 1);
        ViewWrapper toolbarWrapper = new ViewWrapper(view, 1);
        int aniTimeShort = getIntegerFromRes(context2, "search_below_actionbar_toolbar_duration", 100);
        int aniTimeLong2 = getIntegerFromRes(context2, "search_below_actionbar_search_box_duration", 150);
        boolean isRtl;
        String str;
        StringBuilder stringBuilder;
        ViewWrapper viewWrapper2;
        try {
            if (search.getWidth() != toolbar.getWidth()) {
                return null;
            }
            int xTranslation;
            float yTranslation;
            StringBuilder stringBuilder2;
            AnimatorSet animatorSetTmp;
            isRtl = toolbar.getLayoutDirection() == 1;
            try {
                float x;
                xTranslation = tmpCollapseBtn.getWidth();
                yTranslation = (float) (-search.getHeight());
                String str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SearchAnimator: xTranslation ");
                stringBuilder2.append(xTranslation);
                Log.d(str2, stringBuilder2.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SearchAnimator: yTranslation ");
                stringBuilder.append(yTranslation);
                Log.d(str, stringBuilder.toString());
                view3.setAlpha(0.0f);
                view3.setVisibility(0);
                if (isRtl) {
                    try {
                        x = (toolbar.getX() + ((float) toolbar.getWidth())) - ((float) tmpCollapseBtn.getWidth());
                    } catch (Exception e2) {
                        e = e2;
                        i = aniTimeLong2;
                        i2 = aniTimeShort;
                        viewWrapper2 = toolbarWrapper;
                        viewWrapper = holderWrapper;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("SearchAnimator failed. Info: ");
                        stringBuilder.append(e.toString());
                        Log.d(str, stringBuilder.toString());
                        return null;
                    }
                }
                x = toolbar.getX();
                view3.setX(x);
                view3.setY(toolbar.getY());
                str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("SearchAnimator: tmpCollapseBtn.x ");
                stringBuilder3.append(tmpCollapseBtn.getX());
                Log.d(str2, stringBuilder3.toString());
                animatorSetTmp = new AnimatorSet();
                animatorSetTmp.setDuration((long) aniTimeLong2);
                aniTimeLong = aniTimeLong2;
            } catch (Exception e3) {
                e = e3;
                i = aniTimeLong2;
                i2 = aniTimeShort;
                viewWrapper2 = toolbarWrapper;
                viewWrapper = holderWrapper;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SearchAnimator failed. Info: ");
                stringBuilder.append(e.toString());
                Log.d(str, stringBuilder.toString());
                return null;
            }
            try {
                Builder builder = animatorSetTmp.play(ObjectAnimator.ofFloat(view2, "translationY", new float[]{0.0f, yTranslation}));
                float yTranslation2;
                if (isRtl) {
                    yTranslation2 = yTranslation;
                    builder.with(ObjectAnimator.ofFloat(view2, "translationX", new float[]{0.0f, (float) (-xTranslation)}));
                } else {
                    try {
                        yTranslation2 = yTranslation;
                        builder.with(ObjectAnimator.ofFloat(view2, "translationX", new float[]{0.0f, (float) xTranslation}));
                    } catch (Exception e4) {
                        e = e4;
                        i2 = aniTimeShort;
                        viewWrapper2 = toolbarWrapper;
                        viewWrapper = holderWrapper;
                        i = aniTimeLong;
                    }
                }
                builder.with(ObjectAnimator.ofInt(searchWrapper, "width", new int[]{search.getWidth(), search.getWidth() - xTranslation}));
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SearchAnimator: search.width ");
                stringBuilder.append(search.getWidth());
                Log.d(str, stringBuilder.toString());
                builder.with(ObjectAnimator.ofFloat(mask, "Alpha", new float[]{0.0f, 0.2f}));
                builder.with(ObjectAnimator.ofFloat(view3, "Alpha", new float[]{0.0f, 1.0f}));
                builder.with(ObjectAnimator.ofInt(holderWrapper, "height", new int[]{holder.getHeight(), holder.getHeight() - search.getHeight()}));
                String str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SearchAnimator: holder.height ");
                stringBuilder2.append(holder.getHeight());
                stringBuilder2.append(" search.height ");
                stringBuilder2.append(search.getHeight());
                Log.d(str3, stringBuilder2.toString());
                if (toolbar.getHeight() != search.getHeight()) {
                    builder.with(ObjectAnimator.ofInt(toolbarWrapper, "height", new int[]{toolbar.getHeight(), search.getHeight()}));
                }
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SearchAnimator: toolbar.height ");
                stringBuilder2.append(toolbar.getHeight());
                Log.d(str3, stringBuilder2.toString());
                ObjectAnimator animatorToolbarAlpha = ObjectAnimator.ofFloat(view, "alpha", new float[]{1.0f, 0.0f});
                animatorToolbarAlpha.setDuration((long) aniTimeShort);
                animatorSet.playTogether(new Animator[]{animatorToolbarAlpha, animatorSetTmp});
                animatorSet.setInterpolator(AnimationUtils.loadInterpolator(context2, 17563661));
                AnonymousClass2 anonymousClass2 = anonymousClass2;
                final MenuItem menuItem = searchMenuItem;
                final View view4 = view;
                AnonymousClass2 anonymousClass22 = anonymousClass2;
                final View view5 = view2;
                aniTimeLong = builder;
                aniTimeLong2 = view3;
                final ViewWrapper viewWrapper3 = searchWrapper;
                viewWrapper2 = toolbarWrapper;
                toolbarWrapper = holderWrapper;
                holderWrapper = viewWrapper2;
                try {
                    anonymousClass2 = new AnimatorListener() {
                        public void onAnimationStart(Animator animation) {
                        }

                        public void onAnimationEnd(Animator animation) {
                            try {
                                MenuBuilder mMenu = (MenuBuilder) ReflectUtil.getObject(menuItem, "mMenu", MenuItemImpl.class);
                                if (mMenu != null) {
                                    ReflectUtil.callMethod(mMenu, "expandItemActionView", new Class[]{MenuItemImpl.class}, new Object[]{menuItem}, MenuBuilder.class);
                                }
                                HwActionMenuPresenter mActionMenuPresenter = (HwActionMenuPresenter) ReflectUtil.getObject(view4, "mActionMenuPresenter", HwToolbar.class);
                                if (mActionMenuPresenter == null) {
                                    Log.d(HwSearchAnimationUtils.TAG, "search animation onAnimationEnd: mActionMenuPresenter is null");
                                    return;
                                }
                                View mOverflowButton = (View) ReflectUtil.getObject(mActionMenuPresenter, "mHwOverflowButton", ActionMenuPresenter.class);
                                if (mOverflowButton != null) {
                                    Log.d(HwSearchAnimationUtils.TAG, "search animation onAnimationEnd: hide overflow button");
                                    mOverflowButton.setVisibility(8);
                                }
                                menuItem.getActionView().getLayoutParams().width = -1;
                                view4.getLayoutParams().height = -2;
                                view4.setAlpha(1.0f);
                                view5.setVisibility(4);
                                aniTimeLong2.setVisibility(4);
                                int heightSearch = viewWrapper3.getHeight();
                                int heightHolder = toolbarWrapper.getHeight();
                                int heightToolbar = holderWrapper.getHeight();
                                String str = HwSearchAnimationUtils.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("search animation onAnimationEnd: exec, search width is ");
                                stringBuilder.append(heightSearch);
                                stringBuilder.append(", holder height is ");
                                stringBuilder.append(heightHolder);
                                stringBuilder.append(", toolbar height is ");
                                stringBuilder.append(heightToolbar);
                                Log.d(str, stringBuilder.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        public void onAnimationCancel(Animator animation) {
                        }

                        public void onAnimationRepeat(Animator animation) {
                        }
                    };
                    animatorSet.addListener(anonymousClass22);
                    return animatorSet;
                } catch (Exception e5) {
                    e = e5;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("SearchAnimator failed. Info: ");
                    stringBuilder.append(e.toString());
                    Log.d(str, stringBuilder.toString());
                    return null;
                }
            } catch (Exception e6) {
                e = e6;
                i2 = aniTimeShort;
                viewWrapper2 = toolbarWrapper;
                viewWrapper = holderWrapper;
                i = aniTimeLong;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SearchAnimator failed. Info: ");
                stringBuilder.append(e.toString());
                Log.d(str, stringBuilder.toString());
                return null;
            }
        } catch (Exception e7) {
            e = e7;
            i = aniTimeLong2;
            i2 = aniTimeShort;
            viewWrapper2 = toolbarWrapper;
            viewWrapper = holderWrapper;
            isRtl = false;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SearchAnimator failed. Info: ");
            stringBuilder.append(e.toString());
            Log.d(str, stringBuilder.toString());
            return null;
        }
    }

    public static Animator getSearchBelowActionBarCancelAnimator(Context context, HwToolbar toolbar, View search, View mask, View holder, View tmpCollapseBtn) {
        Exception e;
        int i;
        String str;
        StringBuilder stringBuilder;
        Context context2 = context;
        View view = search;
        View view2 = tmpCollapseBtn;
        AnimatorSet animatorSet = new AnimatorSet();
        ViewWrapper searchWrapper = new ViewWrapper(view, 0);
        ViewWrapper holderWrapper = new ViewWrapper(holder, 0);
        int aniTimeShort = getIntegerFromRes(context2, "search_below_actionbar_toolbar_duration", 100);
        int aniTimeLong = getIntegerFromRes(context2, "search_below_actionbar_search_box_duration", 150);
        int aniTimeDelay = getIntegerFromRes(context2, "search_below_actionbar_toolbar_delay", 50);
        boolean isRtl;
        try {
            isRtl = toolbar.getLayoutDirection() == 1;
            try {
                float x;
                int yTranslation = -toolbar.getHeight();
                int xTranslation = tmpCollapseBtn.getWidth();
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("CancelAnimator: xTranslation ");
                stringBuilder2.append(xTranslation);
                Log.d(str2, stringBuilder2.toString());
                str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("CancelAnimator: yTranslation ");
                stringBuilder3.append(yTranslation);
                Log.d(str2, stringBuilder3.toString());
                if (isRtl) {
                    try {
                        x = (toolbar.getX() + ((float) toolbar.getWidth())) - ((float) tmpCollapseBtn.getWidth());
                    } catch (Exception e2) {
                        e = e2;
                        i = aniTimeDelay;
                    }
                } else {
                    x = toolbar.getX();
                }
                view2.setX(x);
                str2 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("CancelAnimator: tmpCollapseBtn.x ");
                stringBuilder3.append(tmpCollapseBtn.getX());
                Log.d(str2, stringBuilder3.toString());
                AnimatorSet animatorSetTmp = new AnimatorSet();
                animatorSetTmp.setDuration((long) aniTimeLong);
                Builder builder = animatorSetTmp.play(ObjectAnimator.ofFloat(view, "translationY", new float[]{(float) yTranslation, 0.0f}));
                if (isRtl) {
                    builder.with(ObjectAnimator.ofFloat(view, "translationX", new float[]{(float) (-xTranslation), 0.0f}));
                } else {
                    builder.with(ObjectAnimator.ofFloat(view, "translationX", new float[]{(float) xTranslation, 0.0f}));
                }
                builder.with(ObjectAnimator.ofInt(searchWrapper, "width", new int[]{search.getWidth(), search.getWidth() + xTranslation}));
                String str3 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("CancelAnimator: search.width ");
                stringBuilder4.append(search.getWidth());
                Log.d(str3, stringBuilder4.toString());
                builder.with(ObjectAnimator.ofFloat(mask, "Alpha", new float[]{0.2f, 0.0f}));
                builder.with(ObjectAnimator.ofFloat(view2, "Alpha", new float[]{1.0f, 0.0f}));
                builder.with(ObjectAnimator.ofInt(holderWrapper, "height", new int[]{holder.getHeight(), search.getHeight()}));
                str3 = TAG;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("CancelAnimator: holder.height ");
                stringBuilder4.append(holder.getHeight());
                stringBuilder4.append(" search.height ");
                stringBuilder4.append(search.getHeight());
                Log.d(str3, stringBuilder4.toString());
                ObjectAnimator animatorToolbarAlpha = ObjectAnimator.ofFloat(toolbar, "alpha", new float[]{0.0f, 1.0f});
                Builder builder2 = builder;
                animatorToolbarAlpha.setDuration((long) aniTimeShort);
                animatorToolbarAlpha.setStartDelay((long) aniTimeDelay);
                animatorSet.playTogether(new Animator[]{animatorToolbarAlpha, animatorSetTmp});
                animatorSet.setInterpolator(AnimationUtils.loadInterpolator(context2, 17563661));
                AnonymousClass3 anonymousClass3 = anonymousClass3;
                final boolean isRtl2 = toolbar;
                AnonymousClass3 anonymousClass32 = anonymousClass3;
                final View view3 = view;
                final View view4 = view2;
                final ViewWrapper viewWrapper = searchWrapper;
                aniTimeDelay = holderWrapper;
                try {
                    anonymousClass3 = new AnimatorListener() {
                        public void onAnimationStart(Animator animation) {
                            Log.d(HwSearchAnimationUtils.TAG, "back animation onAnimationStart: exec.");
                            try {
                                HwActionMenuPresenter mActionMenuPresenter = (HwActionMenuPresenter) ReflectUtil.getObject(isRtl2, "mActionMenuPresenter", HwToolbar.class);
                                if (mActionMenuPresenter == null) {
                                    Log.d(HwSearchAnimationUtils.TAG, "back animation onAnimationStart: mActionMenuPresenter is null");
                                    return;
                                }
                                View mOverflowButton = (View) ReflectUtil.getObject(mActionMenuPresenter, "mHwOverflowButton", ActionMenuPresenter.class);
                                if (mOverflowButton != null) {
                                    Log.d(HwSearchAnimationUtils.TAG, "back animation onAnimationStart: show overflow button");
                                    mOverflowButton.setVisibility(0);
                                }
                                isRtl2.setAlpha(0.0f);
                                view3.setVisibility(0);
                                view4.setVisibility(0);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        public void onAnimationEnd(Animator animation) {
                            view4.setVisibility(4);
                            view3.clearFocus();
                            int heightSearch = viewWrapper.getHeight();
                            int heightHolder = aniTimeDelay.getHeight();
                            String str = HwSearchAnimationUtils.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("back animation onAnimationEnd: exec, search width is ");
                            stringBuilder.append(heightSearch);
                            stringBuilder.append(", holder height is ");
                            stringBuilder.append(heightHolder);
                            stringBuilder.append(" .");
                            Log.d(str, stringBuilder.toString());
                        }

                        public void onAnimationCancel(Animator animation) {
                        }

                        public void onAnimationRepeat(Animator animation) {
                        }
                    };
                    animatorSet.addListener(anonymousClass32);
                    return animatorSet;
                } catch (Exception e3) {
                    e = e3;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("CancelAnimator failed. Info: ");
                    stringBuilder.append(e.toString());
                    Log.d(str, stringBuilder.toString());
                    return null;
                }
            } catch (Exception e4) {
                e = e4;
                i = aniTimeDelay;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("CancelAnimator failed. Info: ");
                stringBuilder.append(e.toString());
                Log.d(str, stringBuilder.toString());
                return null;
            }
        } catch (Exception e5) {
            e = e5;
            i = aniTimeDelay;
            isRtl = false;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("CancelAnimator failed. Info: ");
            stringBuilder.append(e.toString());
            Log.d(str, stringBuilder.toString());
            return null;
        }
    }

    private static int getIntegerFromRes(Context context, String resName, int defVal) {
        int val = defVal;
        int resId = ResLoader.getInstance().getIdentifier(context, "values", resName);
        if (resId == 0) {
            return val;
        }
        try {
            return context.getResources().getInteger(resId);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getIntegerFromRes: ");
            stringBuilder.append(resName);
            stringBuilder.append("not found, use default value.");
            Log.d(str, stringBuilder.toString());
            return val;
        }
    }
}
