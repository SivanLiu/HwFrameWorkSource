package android.animation;

import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LayoutTransition {
    private static TimeInterpolator ACCEL_DECEL_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    public static final int APPEARING = 2;
    public static final int CHANGE_APPEARING = 0;
    public static final int CHANGE_DISAPPEARING = 1;
    public static final int CHANGING = 4;
    private static TimeInterpolator DECEL_INTERPOLATOR = new DecelerateInterpolator();
    private static long DEFAULT_DURATION = 300;
    public static final int DISAPPEARING = 3;
    private static final int FLAG_APPEARING = 1;
    private static final int FLAG_CHANGE_APPEARING = 4;
    private static final int FLAG_CHANGE_DISAPPEARING = 8;
    private static final int FLAG_CHANGING = 16;
    private static final int FLAG_DISAPPEARING = 2;
    private static ObjectAnimator defaultChange;
    private static ObjectAnimator defaultChangeIn;
    private static ObjectAnimator defaultChangeOut;
    private static ObjectAnimator defaultFadeIn;
    private static ObjectAnimator defaultFadeOut;
    private static TimeInterpolator sAppearingInterpolator = ACCEL_DECEL_INTERPOLATOR;
    private static TimeInterpolator sChangingAppearingInterpolator = DECEL_INTERPOLATOR;
    private static TimeInterpolator sChangingDisappearingInterpolator = DECEL_INTERPOLATOR;
    private static TimeInterpolator sChangingInterpolator = DECEL_INTERPOLATOR;
    private static TimeInterpolator sDisappearingInterpolator = ACCEL_DECEL_INTERPOLATOR;
    private final LinkedHashMap<View, Animator> currentAppearingAnimations = new LinkedHashMap();
    private final LinkedHashMap<View, Animator> currentChangingAnimations = new LinkedHashMap();
    private final LinkedHashMap<View, Animator> currentDisappearingAnimations = new LinkedHashMap();
    private final HashMap<View, OnLayoutChangeListener> layoutChangeListenerMap = new HashMap();
    private boolean mAnimateParentHierarchy = true;
    private Animator mAppearingAnim = null;
    private long mAppearingDelay = DEFAULT_DURATION;
    private long mAppearingDuration = DEFAULT_DURATION;
    private TimeInterpolator mAppearingInterpolator = sAppearingInterpolator;
    private Animator mChangingAnim = null;
    private Animator mChangingAppearingAnim = null;
    private long mChangingAppearingDelay = 0;
    private long mChangingAppearingDuration = DEFAULT_DURATION;
    private TimeInterpolator mChangingAppearingInterpolator = sChangingAppearingInterpolator;
    private long mChangingAppearingStagger = 0;
    private long mChangingDelay = 0;
    private Animator mChangingDisappearingAnim = null;
    private long mChangingDisappearingDelay = DEFAULT_DURATION;
    private long mChangingDisappearingDuration = DEFAULT_DURATION;
    private TimeInterpolator mChangingDisappearingInterpolator = sChangingDisappearingInterpolator;
    private long mChangingDisappearingStagger = 0;
    private long mChangingDuration = DEFAULT_DURATION;
    private TimeInterpolator mChangingInterpolator = sChangingInterpolator;
    private long mChangingStagger = 0;
    private Animator mDisappearingAnim = null;
    private long mDisappearingDelay = 0;
    private long mDisappearingDuration = DEFAULT_DURATION;
    private TimeInterpolator mDisappearingInterpolator = sDisappearingInterpolator;
    private ArrayList<TransitionListener> mListeners;
    private int mTransitionTypes = 15;
    private final HashMap<View, Animator> pendingAnimations = new HashMap();
    private long staggerDelay;

    public interface TransitionListener {
        void endTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i);

        void startTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i);
    }

    private static final class CleanupCallback implements OnPreDrawListener, OnAttachStateChangeListener {
        final Map<View, OnLayoutChangeListener> layoutChangeListenerMap;
        final ViewGroup parent;

        CleanupCallback(Map<View, OnLayoutChangeListener> listenerMap, ViewGroup parent) {
            this.layoutChangeListenerMap = listenerMap;
            this.parent = parent;
        }

        private void cleanup() {
            this.parent.getViewTreeObserver().removeOnPreDrawListener(this);
            this.parent.removeOnAttachStateChangeListener(this);
            if (this.layoutChangeListenerMap.size() > 0) {
                for (View view : this.layoutChangeListenerMap.keySet()) {
                    view.removeOnLayoutChangeListener((OnLayoutChangeListener) this.layoutChangeListenerMap.get(view));
                }
                this.layoutChangeListenerMap.clear();
            }
        }

        public void onViewAttachedToWindow(View v) {
        }

        public void onViewDetachedFromWindow(View v) {
            cleanup();
        }

        public boolean onPreDraw() {
            cleanup();
            return true;
        }
    }

    static /* synthetic */ long access$214(LayoutTransition x0, long x1) {
        long j = x0.staggerDelay + x1;
        x0.staggerDelay = j;
        return j;
    }

    public LayoutTransition() {
        if (defaultChangeIn == null) {
            PropertyValuesHolder pvhLeft = PropertyValuesHolder.ofInt("left", 0, 1);
            PropertyValuesHolder pvhTop = PropertyValuesHolder.ofInt("top", 0, 1);
            PropertyValuesHolder pvhRight = PropertyValuesHolder.ofInt("right", 0, 1);
            PropertyValuesHolder pvhBottom = PropertyValuesHolder.ofInt("bottom", 0, 1);
            PropertyValuesHolder pvhScrollX = PropertyValuesHolder.ofInt("scrollX", 0, 1);
            PropertyValuesHolder pvhScrollY = PropertyValuesHolder.ofInt("scrollY", 0, 1);
            defaultChangeIn = ObjectAnimator.ofPropertyValuesHolder(null, pvhLeft, pvhTop, pvhRight, pvhBottom, pvhScrollX, pvhScrollY);
            defaultChangeIn.setDuration(DEFAULT_DURATION);
            defaultChangeIn.setStartDelay(this.mChangingAppearingDelay);
            defaultChangeIn.setInterpolator(this.mChangingAppearingInterpolator);
            defaultChangeOut = defaultChangeIn.clone();
            defaultChangeOut.setStartDelay(this.mChangingDisappearingDelay);
            defaultChangeOut.setInterpolator(this.mChangingDisappearingInterpolator);
            defaultChange = defaultChangeIn.clone();
            defaultChange.setStartDelay(this.mChangingDelay);
            defaultChange.setInterpolator(this.mChangingInterpolator);
            defaultFadeIn = ObjectAnimator.ofFloat(null, "alpha", 0.0f, 1.0f);
            defaultFadeIn.setDuration(DEFAULT_DURATION);
            defaultFadeIn.setStartDelay(this.mAppearingDelay);
            defaultFadeIn.setInterpolator(this.mAppearingInterpolator);
            defaultFadeOut = ObjectAnimator.ofFloat(null, "alpha", 1.0f, 0.0f);
            defaultFadeOut.setDuration(DEFAULT_DURATION);
            defaultFadeOut.setStartDelay(this.mDisappearingDelay);
            defaultFadeOut.setInterpolator(this.mDisappearingInterpolator);
        }
        this.mChangingAppearingAnim = defaultChangeIn;
        this.mChangingDisappearingAnim = defaultChangeOut;
        this.mChangingAnim = defaultChange;
        this.mAppearingAnim = defaultFadeIn;
        this.mDisappearingAnim = defaultFadeOut;
    }

    public void setDuration(long duration) {
        this.mChangingAppearingDuration = duration;
        this.mChangingDisappearingDuration = duration;
        this.mChangingDuration = duration;
        this.mAppearingDuration = duration;
        this.mDisappearingDuration = duration;
    }

    public void enableTransitionType(int transitionType) {
        switch (transitionType) {
            case 0:
                this.mTransitionTypes |= 4;
                return;
            case 1:
                this.mTransitionTypes |= 8;
                return;
            case 2:
                this.mTransitionTypes |= 1;
                return;
            case 3:
                this.mTransitionTypes |= 2;
                return;
            case 4:
                this.mTransitionTypes |= 16;
                return;
            default:
                return;
        }
    }

    public void disableTransitionType(int transitionType) {
        switch (transitionType) {
            case 0:
                this.mTransitionTypes &= -5;
                return;
            case 1:
                this.mTransitionTypes &= -9;
                return;
            case 2:
                this.mTransitionTypes &= -2;
                return;
            case 3:
                this.mTransitionTypes &= -3;
                return;
            case 4:
                this.mTransitionTypes &= -17;
                return;
            default:
                return;
        }
    }

    public boolean isTransitionTypeEnabled(int transitionType) {
        boolean z = false;
        switch (transitionType) {
            case 0:
                if ((this.mTransitionTypes & 4) == 4) {
                    z = true;
                }
                return z;
            case 1:
                if ((this.mTransitionTypes & 8) == 8) {
                    z = true;
                }
                return z;
            case 2:
                if ((this.mTransitionTypes & 1) == 1) {
                    z = true;
                }
                return z;
            case 3:
                if ((this.mTransitionTypes & 2) == 2) {
                    z = true;
                }
                return z;
            case 4:
                if ((this.mTransitionTypes & 16) == 16) {
                    z = true;
                }
                return z;
            default:
                return false;
        }
    }

    public void setStartDelay(int transitionType, long delay) {
        switch (transitionType) {
            case 0:
                this.mChangingAppearingDelay = delay;
                return;
            case 1:
                this.mChangingDisappearingDelay = delay;
                return;
            case 2:
                this.mAppearingDelay = delay;
                return;
            case 3:
                this.mDisappearingDelay = delay;
                return;
            case 4:
                this.mChangingDelay = delay;
                return;
            default:
                return;
        }
    }

    public long getStartDelay(int transitionType) {
        switch (transitionType) {
            case 0:
                return this.mChangingAppearingDelay;
            case 1:
                return this.mChangingDisappearingDelay;
            case 2:
                return this.mAppearingDelay;
            case 3:
                return this.mDisappearingDelay;
            case 4:
                return this.mChangingDelay;
            default:
                return 0;
        }
    }

    public void setDuration(int transitionType, long duration) {
        switch (transitionType) {
            case 0:
                this.mChangingAppearingDuration = duration;
                return;
            case 1:
                this.mChangingDisappearingDuration = duration;
                return;
            case 2:
                this.mAppearingDuration = duration;
                return;
            case 3:
                this.mDisappearingDuration = duration;
                return;
            case 4:
                this.mChangingDuration = duration;
                return;
            default:
                return;
        }
    }

    public long getDuration(int transitionType) {
        switch (transitionType) {
            case 0:
                return this.mChangingAppearingDuration;
            case 1:
                return this.mChangingDisappearingDuration;
            case 2:
                return this.mAppearingDuration;
            case 3:
                return this.mDisappearingDuration;
            case 4:
                return this.mChangingDuration;
            default:
                return 0;
        }
    }

    public void setStagger(int transitionType, long duration) {
        if (transitionType != 4) {
            switch (transitionType) {
                case 0:
                    this.mChangingAppearingStagger = duration;
                    return;
                case 1:
                    this.mChangingDisappearingStagger = duration;
                    return;
                default:
                    return;
            }
        }
        this.mChangingStagger = duration;
    }

    public long getStagger(int transitionType) {
        if (transitionType == 4) {
            return this.mChangingStagger;
        }
        switch (transitionType) {
            case 0:
                return this.mChangingAppearingStagger;
            case 1:
                return this.mChangingDisappearingStagger;
            default:
                return 0;
        }
    }

    public void setInterpolator(int transitionType, TimeInterpolator interpolator) {
        switch (transitionType) {
            case 0:
                this.mChangingAppearingInterpolator = interpolator;
                return;
            case 1:
                this.mChangingDisappearingInterpolator = interpolator;
                return;
            case 2:
                this.mAppearingInterpolator = interpolator;
                return;
            case 3:
                this.mDisappearingInterpolator = interpolator;
                return;
            case 4:
                this.mChangingInterpolator = interpolator;
                return;
            default:
                return;
        }
    }

    public TimeInterpolator getInterpolator(int transitionType) {
        switch (transitionType) {
            case 0:
                return this.mChangingAppearingInterpolator;
            case 1:
                return this.mChangingDisappearingInterpolator;
            case 2:
                return this.mAppearingInterpolator;
            case 3:
                return this.mDisappearingInterpolator;
            case 4:
                return this.mChangingInterpolator;
            default:
                return null;
        }
    }

    public void setAnimator(int transitionType, Animator animator) {
        switch (transitionType) {
            case 0:
                this.mChangingAppearingAnim = animator;
                return;
            case 1:
                this.mChangingDisappearingAnim = animator;
                return;
            case 2:
                this.mAppearingAnim = animator;
                return;
            case 3:
                this.mDisappearingAnim = animator;
                return;
            case 4:
                this.mChangingAnim = animator;
                return;
            default:
                return;
        }
    }

    public Animator getAnimator(int transitionType) {
        switch (transitionType) {
            case 0:
                return this.mChangingAppearingAnim;
            case 1:
                return this.mChangingDisappearingAnim;
            case 2:
                return this.mAppearingAnim;
            case 3:
                return this.mDisappearingAnim;
            case 4:
                return this.mChangingAnim;
            default:
                return null;
        }
    }

    private void runChangeTransition(ViewGroup parent, View newView, int changeReason) {
        Animator baseAnimator;
        long duration;
        Animator parentAnimator;
        Animator baseAnimator2;
        Animator parentAnimator2;
        ViewGroup viewGroup = parent;
        switch (changeReason) {
            case 2:
                baseAnimator = this.mChangingAppearingAnim;
                duration = this.mChangingAppearingDuration;
                parentAnimator = defaultChangeIn;
                break;
            case 3:
                baseAnimator = this.mChangingDisappearingAnim;
                duration = this.mChangingDisappearingDuration;
                parentAnimator = defaultChangeOut;
                break;
            case 4:
                baseAnimator = this.mChangingAnim;
                duration = this.mChangingDuration;
                parentAnimator = defaultChange;
                break;
            default:
                baseAnimator2 = null;
                parentAnimator2 = null;
                duration = 0;
                break;
        }
        baseAnimator2 = baseAnimator;
        parentAnimator2 = parentAnimator;
        if (baseAnimator2 != null) {
            this.staggerDelay = 0;
            ViewTreeObserver observer = parent.getViewTreeObserver();
            if (observer.isAlive()) {
                int numChildren = parent.getChildCount();
                int i = 0;
                while (true) {
                    int i2 = i;
                    if (i2 < numChildren) {
                        View child = viewGroup.getChildAt(i2);
                        if (child != newView) {
                            setupChangeAnimation(viewGroup, changeReason, baseAnimator2, duration, child);
                        }
                        i = i2 + 1;
                    } else {
                        ViewTreeObserver observer2;
                        View view = newView;
                        if (this.mAnimateParentHierarchy) {
                            ViewGroup tempParent = viewGroup;
                            while (tempParent != null) {
                                int numChildren2;
                                ViewParent parentParent = tempParent.getParent();
                                if (parentParent instanceof ViewGroup) {
                                    numChildren2 = numChildren;
                                    observer2 = observer;
                                    setupChangeAnimation((ViewGroup) parentParent, changeReason, parentAnimator2, duration, tempParent);
                                    tempParent = (ViewGroup) parentParent;
                                } else {
                                    numChildren2 = numChildren;
                                    observer2 = observer;
                                    tempParent = null;
                                }
                                view = newView;
                                numChildren = numChildren2;
                                observer = observer2;
                            }
                        }
                        observer2 = observer;
                        CleanupCallback callback = new CleanupCallback(this.layoutChangeListenerMap, viewGroup);
                        observer2.addOnPreDrawListener(callback);
                        viewGroup.addOnAttachStateChangeListener(callback);
                        return;
                    }
                }
            }
        }
    }

    public void setAnimateParentHierarchy(boolean animateParentHierarchy) {
        this.mAnimateParentHierarchy = animateParentHierarchy;
    }

    private void setupChangeAnimation(ViewGroup parent, int changeReason, Animator baseAnimator, long duration, View child) {
        final View view = child;
        if (this.layoutChangeListenerMap.get(view) == null) {
            if (child.getWidth() != 0 || child.getHeight() != 0) {
                Animator anim = baseAnimator.clone();
                anim.setTarget(view);
                anim.setupStartValues();
                Animator currentAnimation = (Animator) this.pendingAnimations.get(view);
                if (currentAnimation != null) {
                    currentAnimation.cancel();
                    this.pendingAnimations.remove(view);
                }
                this.pendingAnimations.put(view, anim);
                ValueAnimator pendingAnimRemover = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(duration + 100);
                pendingAnimRemover.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        LayoutTransition.this.pendingAnimations.remove(view);
                    }
                });
                pendingAnimRemover.start();
                final Animator animator = anim;
                final int i = changeReason;
                final long j = duration;
                final View view2 = view;
                final ViewGroup viewGroup = parent;
                OnLayoutChangeListener listener = new OnLayoutChangeListener() {
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        animator.setupEndValues();
                        if (animator instanceof ValueAnimator) {
                            boolean valuesDiffer = false;
                            PropertyValuesHolder[] oldValues = animator.getValues();
                            for (PropertyValuesHolder pvh : oldValues) {
                                if (pvh.mKeyframes instanceof KeyframeSet) {
                                    KeyframeSet keyframeSet = pvh.mKeyframes;
                                    if (keyframeSet.mFirstKeyframe == null || keyframeSet.mLastKeyframe == null || !keyframeSet.mFirstKeyframe.getValue().equals(keyframeSet.mLastKeyframe.getValue())) {
                                        valuesDiffer = true;
                                    }
                                } else if (!pvh.mKeyframes.getValue(0.0f).equals(pvh.mKeyframes.getValue(1.0f))) {
                                    valuesDiffer = true;
                                }
                            }
                            if (!valuesDiffer) {
                                return;
                            }
                        }
                        long startDelay = 0;
                        switch (i) {
                            case 2:
                                startDelay = LayoutTransition.this.mChangingAppearingDelay + LayoutTransition.this.staggerDelay;
                                LayoutTransition.access$214(LayoutTransition.this, LayoutTransition.this.mChangingAppearingStagger);
                                if (LayoutTransition.this.mChangingAppearingInterpolator != LayoutTransition.sChangingAppearingInterpolator) {
                                    animator.setInterpolator(LayoutTransition.this.mChangingAppearingInterpolator);
                                    break;
                                }
                                break;
                            case 3:
                                startDelay = LayoutTransition.this.mChangingDisappearingDelay + LayoutTransition.this.staggerDelay;
                                LayoutTransition.access$214(LayoutTransition.this, LayoutTransition.this.mChangingDisappearingStagger);
                                if (LayoutTransition.this.mChangingDisappearingInterpolator != LayoutTransition.sChangingDisappearingInterpolator) {
                                    animator.setInterpolator(LayoutTransition.this.mChangingDisappearingInterpolator);
                                    break;
                                }
                                break;
                            case 4:
                                startDelay = LayoutTransition.this.mChangingDelay + LayoutTransition.this.staggerDelay;
                                LayoutTransition.access$214(LayoutTransition.this, LayoutTransition.this.mChangingStagger);
                                if (LayoutTransition.this.mChangingInterpolator != LayoutTransition.sChangingInterpolator) {
                                    animator.setInterpolator(LayoutTransition.this.mChangingInterpolator);
                                    break;
                                }
                                break;
                        }
                        animator.setStartDelay(startDelay);
                        animator.setDuration(j);
                        Animator prevAnimation = (Animator) LayoutTransition.this.currentChangingAnimations.get(view2);
                        if (prevAnimation != null) {
                            prevAnimation.cancel();
                        }
                        if (((Animator) LayoutTransition.this.pendingAnimations.get(view2)) != null) {
                            LayoutTransition.this.pendingAnimations.remove(view2);
                        }
                        LayoutTransition.this.currentChangingAnimations.put(view2, animator);
                        viewGroup.requestTransitionStart(LayoutTransition.this);
                        view2.removeOnLayoutChangeListener(this);
                        LayoutTransition.this.layoutChangeListenerMap.remove(view2);
                    }
                };
                final ViewGroup viewGroup2 = parent;
                final View view3 = view;
                final int i2 = changeReason;
                final OnLayoutChangeListener onLayoutChangeListener = listener;
                anim.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationStart(Animator animator) {
                        if (LayoutTransition.this.hasListeners()) {
                            Iterator it = ((ArrayList) LayoutTransition.this.mListeners.clone()).iterator();
                            while (it.hasNext()) {
                                TransitionListener listener = (TransitionListener) it.next();
                                LayoutTransition layoutTransition = LayoutTransition.this;
                                ViewGroup viewGroup = viewGroup2;
                                View view = view3;
                                int i = i2 == 2 ? 0 : i2 == 3 ? 1 : 4;
                                listener.startTransition(layoutTransition, viewGroup, view, i);
                            }
                        }
                    }

                    public void onAnimationCancel(Animator animator) {
                        view3.removeOnLayoutChangeListener(onLayoutChangeListener);
                        LayoutTransition.this.layoutChangeListenerMap.remove(view3);
                    }

                    public void onAnimationEnd(Animator animator) {
                        LayoutTransition.this.currentChangingAnimations.remove(view3);
                        if (LayoutTransition.this.hasListeners()) {
                            Iterator it = ((ArrayList) LayoutTransition.this.mListeners.clone()).iterator();
                            while (it.hasNext()) {
                                TransitionListener listener = (TransitionListener) it.next();
                                LayoutTransition layoutTransition = LayoutTransition.this;
                                ViewGroup viewGroup = viewGroup2;
                                View view = view3;
                                int i = i2 == 2 ? 0 : i2 == 3 ? 1 : 4;
                                listener.endTransition(layoutTransition, viewGroup, view, i);
                            }
                        }
                    }
                });
                view.addOnLayoutChangeListener(listener);
                this.layoutChangeListenerMap.put(view, listener);
            }
        }
    }

    public void startChangingAnimations() {
        for (Animator anim : ((LinkedHashMap) this.currentChangingAnimations.clone()).values()) {
            if (anim instanceof ObjectAnimator) {
                ((ObjectAnimator) anim).setCurrentPlayTime(0);
            }
            anim.start();
        }
    }

    public void endChangingAnimations() {
        for (Animator anim : ((LinkedHashMap) this.currentChangingAnimations.clone()).values()) {
            anim.start();
            anim.end();
        }
        this.currentChangingAnimations.clear();
    }

    public boolean isChangingLayout() {
        return this.currentChangingAnimations.size() > 0;
    }

    public boolean isRunning() {
        return this.currentChangingAnimations.size() > 0 || this.currentAppearingAnimations.size() > 0 || this.currentDisappearingAnimations.size() > 0;
    }

    public void cancel() {
        if (this.currentChangingAnimations.size() > 0) {
            for (Animator anim : ((LinkedHashMap) this.currentChangingAnimations.clone()).values()) {
                anim.cancel();
            }
            this.currentChangingAnimations.clear();
        }
        if (this.currentAppearingAnimations.size() > 0) {
            for (Animator anim2 : ((LinkedHashMap) this.currentAppearingAnimations.clone()).values()) {
                anim2.end();
            }
            this.currentAppearingAnimations.clear();
        }
        if (this.currentDisappearingAnimations.size() > 0) {
            for (Animator anim22 : ((LinkedHashMap) this.currentDisappearingAnimations.clone()).values()) {
                anim22.end();
            }
            this.currentDisappearingAnimations.clear();
        }
    }

    public void cancel(int transitionType) {
        switch (transitionType) {
            case 0:
            case 1:
            case 4:
                if (this.currentChangingAnimations.size() > 0) {
                    for (Animator anim : ((LinkedHashMap) this.currentChangingAnimations.clone()).values()) {
                        anim.cancel();
                    }
                    this.currentChangingAnimations.clear();
                    return;
                }
                return;
            case 2:
                if (this.currentAppearingAnimations.size() > 0) {
                    for (Animator anim2 : ((LinkedHashMap) this.currentAppearingAnimations.clone()).values()) {
                        anim2.end();
                    }
                    this.currentAppearingAnimations.clear();
                    return;
                }
                return;
            case 3:
                if (this.currentDisappearingAnimations.size() > 0) {
                    for (Animator anim22 : ((LinkedHashMap) this.currentDisappearingAnimations.clone()).values()) {
                        anim22.end();
                    }
                    this.currentDisappearingAnimations.clear();
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void runAppearingTransition(final ViewGroup parent, final View child) {
        Animator currentAnimation = (Animator) this.currentDisappearingAnimations.get(child);
        if (currentAnimation != null) {
            currentAnimation.cancel();
        }
        if (this.mAppearingAnim == null) {
            if (hasListeners()) {
                Iterator it = ((ArrayList) this.mListeners.clone()).iterator();
                while (it.hasNext()) {
                    ((TransitionListener) it.next()).endTransition(this, parent, child, 2);
                }
            }
            return;
        }
        Animator anim = this.mAppearingAnim.clone();
        anim.setTarget(child);
        anim.setStartDelay(this.mAppearingDelay);
        anim.setDuration(this.mAppearingDuration);
        if (this.mAppearingInterpolator != sAppearingInterpolator) {
            anim.setInterpolator(this.mAppearingInterpolator);
        }
        if (anim instanceof ObjectAnimator) {
            ((ObjectAnimator) anim).setCurrentPlayTime(0);
        }
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                LayoutTransition.this.currentAppearingAnimations.remove(child);
                if (LayoutTransition.this.hasListeners()) {
                    Iterator it = ((ArrayList) LayoutTransition.this.mListeners.clone()).iterator();
                    while (it.hasNext()) {
                        ((TransitionListener) it.next()).endTransition(LayoutTransition.this, parent, child, 2);
                    }
                }
            }
        });
        this.currentAppearingAnimations.put(child, anim);
        anim.start();
    }

    private void runDisappearingTransition(final ViewGroup parent, final View child) {
        Animator currentAnimation = (Animator) this.currentAppearingAnimations.get(child);
        if (currentAnimation != null) {
            currentAnimation.cancel();
        }
        if (this.mDisappearingAnim == null) {
            if (hasListeners()) {
                Iterator it = ((ArrayList) this.mListeners.clone()).iterator();
                while (it.hasNext()) {
                    ((TransitionListener) it.next()).endTransition(this, parent, child, 3);
                }
            }
            return;
        }
        Animator anim = this.mDisappearingAnim.clone();
        anim.setStartDelay(this.mDisappearingDelay);
        anim.setDuration(this.mDisappearingDuration);
        if (this.mDisappearingInterpolator != sDisappearingInterpolator) {
            anim.setInterpolator(this.mDisappearingInterpolator);
        }
        anim.setTarget(child);
        final float preAnimAlpha = child.getAlpha();
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                LayoutTransition.this.currentDisappearingAnimations.remove(child);
                child.setAlpha(preAnimAlpha);
                if (LayoutTransition.this.hasListeners()) {
                    Iterator it = ((ArrayList) LayoutTransition.this.mListeners.clone()).iterator();
                    while (it.hasNext()) {
                        ((TransitionListener) it.next()).endTransition(LayoutTransition.this, parent, child, 3);
                    }
                }
            }
        });
        if (anim instanceof ObjectAnimator) {
            ((ObjectAnimator) anim).setCurrentPlayTime(0);
        }
        this.currentDisappearingAnimations.put(child, anim);
        anim.start();
    }

    private void addChild(ViewGroup parent, View child, boolean changesLayout) {
        if (parent.getWindowVisibility() == 0) {
            if ((this.mTransitionTypes & 1) == 1) {
                cancel(3);
            }
            if (changesLayout && (this.mTransitionTypes & 4) == 4) {
                cancel(0);
                cancel(4);
            }
            if (hasListeners() && (this.mTransitionTypes & 1) == 1) {
                Iterator it = ((ArrayList) this.mListeners.clone()).iterator();
                while (it.hasNext()) {
                    ((TransitionListener) it.next()).startTransition(this, parent, child, 2);
                }
            }
            if (changesLayout && (this.mTransitionTypes & 4) == 4) {
                runChangeTransition(parent, child, 2);
            }
            if ((this.mTransitionTypes & 1) == 1) {
                runAppearingTransition(parent, child);
            }
        }
    }

    private boolean hasListeners() {
        return this.mListeners != null && this.mListeners.size() > 0;
    }

    public void layoutChange(ViewGroup parent) {
        if (parent.getWindowVisibility() == 0 && (this.mTransitionTypes & 16) == 16 && !isRunning()) {
            runChangeTransition(parent, null, 4);
        }
    }

    public void addChild(ViewGroup parent, View child) {
        addChild(parent, child, true);
    }

    @Deprecated
    public void showChild(ViewGroup parent, View child) {
        addChild(parent, child, true);
    }

    public void showChild(ViewGroup parent, View child, int oldVisibility) {
        addChild(parent, child, oldVisibility == 8);
    }

    private void removeChild(ViewGroup parent, View child, boolean changesLayout) {
        if (parent.getWindowVisibility() == 0) {
            if ((this.mTransitionTypes & 2) == 2) {
                cancel(2);
            }
            if (changesLayout && (this.mTransitionTypes & 8) == 8) {
                cancel(1);
                cancel(4);
            }
            if (hasListeners() && (this.mTransitionTypes & 2) == 2) {
                Iterator it = ((ArrayList) this.mListeners.clone()).iterator();
                while (it.hasNext()) {
                    ((TransitionListener) it.next()).startTransition(this, parent, child, 3);
                }
            }
            if (changesLayout && (this.mTransitionTypes & 8) == 8) {
                runChangeTransition(parent, child, 3);
            }
            if ((this.mTransitionTypes & 2) == 2) {
                runDisappearingTransition(parent, child);
            }
        }
    }

    public void removeChild(ViewGroup parent, View child) {
        removeChild(parent, child, true);
    }

    @Deprecated
    public void hideChild(ViewGroup parent, View child) {
        removeChild(parent, child, true);
    }

    public void hideChild(ViewGroup parent, View child, int newVisibility) {
        removeChild(parent, child, newVisibility == 8);
    }

    public void addTransitionListener(TransitionListener listener) {
        if (this.mListeners == null) {
            this.mListeners = new ArrayList();
        }
        this.mListeners.add(listener);
    }

    public void removeTransitionListener(TransitionListener listener) {
        if (this.mListeners != null) {
            this.mListeners.remove(listener);
        }
    }

    public List<TransitionListener> getTransitionListeners() {
        return this.mListeners;
    }
}
