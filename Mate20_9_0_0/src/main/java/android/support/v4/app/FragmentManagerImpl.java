package android.support.v4.app;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.arch.lifecycle.ViewModelStore;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment.SavedState;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentManager.FragmentLifecycleCallbacks;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.util.ArraySet;
import android.support.v4.util.DebugUtils;
import android.support.v4.util.LogWriter;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater.Factory2;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* compiled from: FragmentManager */
final class FragmentManagerImpl extends FragmentManager implements Factory2 {
    static final Interpolator ACCELERATE_CUBIC = new AccelerateInterpolator(1.5f);
    static final Interpolator ACCELERATE_QUINT = new AccelerateInterpolator(2.5f);
    static final int ANIM_DUR = 220;
    public static final int ANIM_STYLE_CLOSE_ENTER = 3;
    public static final int ANIM_STYLE_CLOSE_EXIT = 4;
    public static final int ANIM_STYLE_FADE_ENTER = 5;
    public static final int ANIM_STYLE_FADE_EXIT = 6;
    public static final int ANIM_STYLE_OPEN_ENTER = 1;
    public static final int ANIM_STYLE_OPEN_EXIT = 2;
    static boolean DEBUG = false;
    static final Interpolator DECELERATE_CUBIC = new DecelerateInterpolator(1.5f);
    static final Interpolator DECELERATE_QUINT = new DecelerateInterpolator(2.5f);
    static final String TAG = "FragmentManager";
    static final String TARGET_REQUEST_CODE_STATE_TAG = "android:target_req_state";
    static final String TARGET_STATE_TAG = "android:target_state";
    static final String USER_VISIBLE_HINT_TAG = "android:user_visible_hint";
    static final String VIEW_STATE_TAG = "android:view_state";
    static Field sAnimationListenerField = null;
    SparseArray<Fragment> mActive;
    final ArrayList<Fragment> mAdded = new ArrayList();
    ArrayList<Integer> mAvailBackStackIndices;
    ArrayList<BackStackRecord> mBackStack;
    ArrayList<OnBackStackChangedListener> mBackStackChangeListeners;
    ArrayList<BackStackRecord> mBackStackIndices;
    FragmentContainer mContainer;
    ArrayList<Fragment> mCreatedMenus;
    int mCurState = 0;
    boolean mDestroyed;
    Runnable mExecCommit = new Runnable() {
        public void run() {
            FragmentManagerImpl.this.execPendingActions();
        }
    };
    boolean mExecutingActions;
    boolean mHavePendingDeferredStart;
    FragmentHostCallback mHost;
    private final CopyOnWriteArrayList<FragmentLifecycleCallbacksHolder> mLifecycleCallbacks = new CopyOnWriteArrayList();
    boolean mNeedMenuInvalidate;
    int mNextFragmentIndex = 0;
    String mNoTransactionsBecause;
    Fragment mParent;
    ArrayList<OpGenerator> mPendingActions;
    ArrayList<StartEnterTransitionListener> mPostponedTransactions;
    @Nullable
    Fragment mPrimaryNav;
    FragmentManagerNonConfig mSavedNonConfig;
    SparseArray<Parcelable> mStateArray = null;
    Bundle mStateBundle = null;
    boolean mStateSaved;
    boolean mStopped;
    ArrayList<Fragment> mTmpAddedFragments;
    ArrayList<Boolean> mTmpIsPop;
    ArrayList<BackStackRecord> mTmpRecords;

    /* compiled from: FragmentManager */
    private static class AnimationListenerWrapper implements AnimationListener {
        private final AnimationListener mWrapped;

        /* synthetic */ AnimationListenerWrapper(AnimationListener x0, AnonymousClass1 x1) {
            this(x0);
        }

        private AnimationListenerWrapper(AnimationListener wrapped) {
            this.mWrapped = wrapped;
        }

        @CallSuper
        public void onAnimationStart(Animation animation) {
            if (this.mWrapped != null) {
                this.mWrapped.onAnimationStart(animation);
            }
        }

        @CallSuper
        public void onAnimationEnd(Animation animation) {
            if (this.mWrapped != null) {
                this.mWrapped.onAnimationEnd(animation);
            }
        }

        @CallSuper
        public void onAnimationRepeat(Animation animation) {
            if (this.mWrapped != null) {
                this.mWrapped.onAnimationRepeat(animation);
            }
        }
    }

    /* compiled from: FragmentManager */
    private static class AnimationOrAnimator {
        public final Animation animation;
        public final Animator animator;

        private AnimationOrAnimator(Animation animation) {
            this.animation = animation;
            this.animator = null;
            if (animation == null) {
                throw new IllegalStateException("Animation cannot be null");
            }
        }

        private AnimationOrAnimator(Animator animator) {
            this.animation = null;
            this.animator = animator;
            if (animator == null) {
                throw new IllegalStateException("Animator cannot be null");
            }
        }
    }

    /* compiled from: FragmentManager */
    private static class AnimatorOnHWLayerIfNeededListener extends AnimatorListenerAdapter {
        View mView;

        AnimatorOnHWLayerIfNeededListener(View v) {
            this.mView = v;
        }

        public void onAnimationStart(Animator animation) {
            this.mView.setLayerType(2, null);
        }

        public void onAnimationEnd(Animator animation) {
            this.mView.setLayerType(0, null);
            animation.removeListener(this);
        }
    }

    /* compiled from: FragmentManager */
    private static class EndViewTransitionAnimator extends AnimationSet implements Runnable {
        private final View mChild;
        private boolean mEnded;
        private final ViewGroup mParent;
        private boolean mTransitionEnded;

        EndViewTransitionAnimator(@NonNull Animation animation, @NonNull ViewGroup parent, @NonNull View child) {
            super(false);
            this.mParent = parent;
            this.mChild = child;
            addAnimation(animation);
        }

        public boolean getTransformation(long currentTime, Transformation t) {
            if (this.mEnded) {
                return this.mTransitionEnded ^ true;
            }
            if (!super.getTransformation(currentTime, t)) {
                this.mEnded = true;
                OneShotPreDrawListener.add(this.mParent, this);
            }
            return true;
        }

        public boolean getTransformation(long currentTime, Transformation outTransformation, float scale) {
            if (this.mEnded) {
                return this.mTransitionEnded ^ true;
            }
            if (!super.getTransformation(currentTime, outTransformation, scale)) {
                this.mEnded = true;
                OneShotPreDrawListener.add(this.mParent, this);
            }
            return true;
        }

        public void run() {
            this.mParent.endViewTransition(this.mChild);
            this.mTransitionEnded = true;
        }
    }

    /* compiled from: FragmentManager */
    private static final class FragmentLifecycleCallbacksHolder {
        final FragmentLifecycleCallbacks mCallback;
        final boolean mRecursive;

        FragmentLifecycleCallbacksHolder(FragmentLifecycleCallbacks callback, boolean recursive) {
            this.mCallback = callback;
            this.mRecursive = recursive;
        }
    }

    /* compiled from: FragmentManager */
    static class FragmentTag {
        public static final int[] Fragment = new int[]{16842755, 16842960, 16842961};
        public static final int Fragment_id = 1;
        public static final int Fragment_name = 0;
        public static final int Fragment_tag = 2;

        private FragmentTag() {
        }
    }

    /* compiled from: FragmentManager */
    interface OpGenerator {
        boolean generateOps(ArrayList<BackStackRecord> arrayList, ArrayList<Boolean> arrayList2);
    }

    /* compiled from: FragmentManager */
    private static class AnimateOnHWLayerIfNeededListener extends AnimationListenerWrapper {
        View mView;

        AnimateOnHWLayerIfNeededListener(View v, AnimationListener listener) {
            super(listener, null);
            this.mView = v;
        }

        @CallSuper
        public void onAnimationEnd(Animation animation) {
            if (ViewCompat.isAttachedToWindow(this.mView) || VERSION.SDK_INT >= 24) {
                this.mView.post(new Runnable() {
                    public void run() {
                        AnimateOnHWLayerIfNeededListener.this.mView.setLayerType(0, null);
                    }
                });
            } else {
                this.mView.setLayerType(0, null);
            }
            super.onAnimationEnd(animation);
        }
    }

    /* compiled from: FragmentManager */
    private class PopBackStackState implements OpGenerator {
        final int mFlags;
        final int mId;
        final String mName;

        PopBackStackState(String name, int id, int flags) {
            this.mName = name;
            this.mId = id;
            this.mFlags = flags;
        }

        public boolean generateOps(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop) {
            if (FragmentManagerImpl.this.mPrimaryNav != null && this.mId < 0 && this.mName == null) {
                FragmentManager childManager = FragmentManagerImpl.this.mPrimaryNav.peekChildFragmentManager();
                if (childManager != null && childManager.popBackStackImmediate()) {
                    return false;
                }
            }
            return FragmentManagerImpl.this.popBackStackState(records, isRecordPop, this.mName, this.mId, this.mFlags);
        }
    }

    /* compiled from: FragmentManager */
    static class StartEnterTransitionListener implements OnStartEnterTransitionListener {
        private final boolean mIsBack;
        private int mNumPostponed;
        private final BackStackRecord mRecord;

        StartEnterTransitionListener(BackStackRecord record, boolean isBack) {
            this.mIsBack = isBack;
            this.mRecord = record;
        }

        public void onStartEnterTransition() {
            this.mNumPostponed--;
            if (this.mNumPostponed == 0) {
                this.mRecord.mManager.scheduleCommit();
            }
        }

        public void startListening() {
            this.mNumPostponed++;
        }

        public boolean isReady() {
            return this.mNumPostponed == 0;
        }

        public void completeTransaction() {
            boolean z = false;
            boolean canceled = this.mNumPostponed > 0;
            FragmentManagerImpl manager = this.mRecord.mManager;
            int numAdded = manager.mAdded.size();
            for (int i = 0; i < numAdded; i++) {
                Fragment fragment = (Fragment) manager.mAdded.get(i);
                fragment.setOnStartEnterTransitionListener(null);
                if (canceled && fragment.isPostponed()) {
                    fragment.startPostponedEnterTransition();
                }
            }
            FragmentManagerImpl fragmentManagerImpl = this.mRecord.mManager;
            BackStackRecord backStackRecord = this.mRecord;
            boolean z2 = this.mIsBack;
            if (!canceled) {
                z = true;
            }
            fragmentManagerImpl.completeExecute(backStackRecord, z2, z, true);
        }

        public void cancelTransaction() {
            this.mRecord.mManager.completeExecute(this.mRecord, this.mIsBack, false, false);
        }
    }

    FragmentManagerImpl() {
    }

    static boolean modifiesAlpha(AnimationOrAnimator anim) {
        if (anim.animation instanceof AlphaAnimation) {
            return true;
        }
        if (!(anim.animation instanceof AnimationSet)) {
            return modifiesAlpha(anim.animator);
        }
        List<Animation> anims = ((AnimationSet) anim.animation).getAnimations();
        for (int i = 0; i < anims.size(); i++) {
            if (anims.get(i) instanceof AlphaAnimation) {
                return true;
            }
        }
        return false;
    }

    static boolean modifiesAlpha(Animator anim) {
        if (anim == null) {
            return false;
        }
        if (anim instanceof ValueAnimator) {
            PropertyValuesHolder[] values = ((ValueAnimator) anim).getValues();
            for (PropertyValuesHolder propertyName : values) {
                if ("alpha".equals(propertyName.getPropertyName())) {
                    return true;
                }
            }
        } else if (anim instanceof AnimatorSet) {
            List<Animator> animList = ((AnimatorSet) anim).getChildAnimations();
            for (int i = 0; i < animList.size(); i++) {
                if (modifiesAlpha((Animator) animList.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean shouldRunOnHWLayer(View v, AnimationOrAnimator anim) {
        boolean z = false;
        if (v == null || anim == null) {
            return false;
        }
        if (VERSION.SDK_INT >= 19 && v.getLayerType() == 0 && ViewCompat.hasOverlappingRendering(v) && modifiesAlpha(anim)) {
            z = true;
        }
        return z;
    }

    private void throwException(RuntimeException ex) {
        Log.e(TAG, ex.getMessage());
        Log.e(TAG, "Activity state:");
        PrintWriter pw = new PrintWriter(new LogWriter(TAG));
        if (this.mHost != null) {
            try {
                this.mHost.onDump("  ", null, pw, new String[0]);
            } catch (Exception e) {
                Log.e(TAG, "Failed dumping state", e);
            }
        } else {
            try {
                dump("  ", null, pw, new String[0]);
            } catch (Exception e2) {
                Log.e(TAG, "Failed dumping state", e2);
            }
        }
        throw ex;
    }

    public FragmentTransaction beginTransaction() {
        return new BackStackRecord(this);
    }

    public boolean executePendingTransactions() {
        boolean updates = execPendingActions();
        forcePostponedTransactions();
        return updates;
    }

    public void popBackStack() {
        enqueueAction(new PopBackStackState(null, -1, 0), false);
    }

    public boolean popBackStackImmediate() {
        checkStateLoss();
        return popBackStackImmediate(null, -1, 0);
    }

    public void popBackStack(@Nullable String name, int flags) {
        enqueueAction(new PopBackStackState(name, -1, flags), false);
    }

    public boolean popBackStackImmediate(@Nullable String name, int flags) {
        checkStateLoss();
        return popBackStackImmediate(name, -1, flags);
    }

    public void popBackStack(int id, int flags) {
        if (id >= 0) {
            enqueueAction(new PopBackStackState(null, id, flags), false);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad id: ");
        stringBuilder.append(id);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public boolean popBackStackImmediate(int id, int flags) {
        checkStateLoss();
        execPendingActions();
        if (id >= 0) {
            return popBackStackImmediate(null, id, flags);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad id: ");
        stringBuilder.append(id);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private boolean popBackStackImmediate(String name, int id, int flags) {
        execPendingActions();
        ensureExecReady(true);
        if (this.mPrimaryNav != null && id < 0 && name == null) {
            FragmentManager childManager = this.mPrimaryNav.peekChildFragmentManager();
            if (childManager != null && childManager.popBackStackImmediate()) {
                return true;
            }
        }
        boolean executePop = popBackStackState(this.mTmpRecords, this.mTmpIsPop, name, id, flags);
        if (executePop) {
            this.mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(this.mTmpRecords, this.mTmpIsPop);
            } finally {
                cleanupExec();
            }
        }
        doPendingDeferredStart();
        burpActive();
        return executePop;
    }

    public int getBackStackEntryCount() {
        return this.mBackStack != null ? this.mBackStack.size() : 0;
    }

    public BackStackEntry getBackStackEntryAt(int index) {
        return (BackStackEntry) this.mBackStack.get(index);
    }

    public void addOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (this.mBackStackChangeListeners == null) {
            this.mBackStackChangeListeners = new ArrayList();
        }
        this.mBackStackChangeListeners.add(listener);
    }

    public void removeOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (this.mBackStackChangeListeners != null) {
            this.mBackStackChangeListeners.remove(listener);
        }
    }

    public void putFragment(Bundle bundle, String key, Fragment fragment) {
        if (fragment.mIndex < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Fragment ");
            stringBuilder.append(fragment);
            stringBuilder.append(" is not currently in the FragmentManager");
            throwException(new IllegalStateException(stringBuilder.toString()));
        }
        bundle.putInt(key, fragment.mIndex);
    }

    @Nullable
    public Fragment getFragment(Bundle bundle, String key) {
        int index = bundle.getInt(key, -1);
        if (index == -1) {
            return null;
        }
        Fragment f = (Fragment) this.mActive.get(index);
        if (f == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Fragment no longer exists for key ");
            stringBuilder.append(key);
            stringBuilder.append(": index ");
            stringBuilder.append(index);
            throwException(new IllegalStateException(stringBuilder.toString()));
        }
        return f;
    }

    public List<Fragment> getFragments() {
        if (this.mAdded.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        List<Fragment> list;
        synchronized (this.mAdded) {
            list = (List) this.mAdded.clone();
        }
        return list;
    }

    List<Fragment> getActiveFragments() {
        if (this.mActive == null) {
            return null;
        }
        int count = this.mActive.size();
        ArrayList<Fragment> fragments = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            fragments.add(this.mActive.valueAt(i));
        }
        return fragments;
    }

    int getActiveFragmentCount() {
        if (this.mActive == null) {
            return 0;
        }
        return this.mActive.size();
    }

    @Nullable
    public SavedState saveFragmentInstanceState(Fragment fragment) {
        if (fragment.mIndex < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Fragment ");
            stringBuilder.append(fragment);
            stringBuilder.append(" is not currently in the FragmentManager");
            throwException(new IllegalStateException(stringBuilder.toString()));
        }
        SavedState savedState = null;
        if (fragment.mState <= 0) {
            return null;
        }
        Bundle result = saveFragmentBasicState(fragment);
        if (result != null) {
            savedState = new SavedState(result);
        }
        return savedState;
    }

    public boolean isDestroyed() {
        return this.mDestroyed;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FragmentManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        if (this.mParent != null) {
            DebugUtils.buildShortClassTag(this.mParent, sb);
        } else {
            DebugUtils.buildShortClassTag(this.mHost, sb);
        }
        sb.append("}}");
        return sb.toString();
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        int N;
        int i;
        Fragment f;
        BackStackRecord bs;
        String innerPrefix = new StringBuilder();
        innerPrefix.append(prefix);
        innerPrefix.append("    ");
        innerPrefix = innerPrefix.toString();
        int i2 = 0;
        if (this.mActive != null) {
            N = this.mActive.size();
            if (N > 0) {
                writer.print(prefix);
                writer.print("Active Fragments in ");
                writer.print(Integer.toHexString(System.identityHashCode(this)));
                writer.println(":");
                for (i = 0; i < N; i++) {
                    f = (Fragment) this.mActive.valueAt(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(f);
                    if (f != null) {
                        f.dump(innerPrefix, fd, writer, args);
                    }
                }
            }
        }
        N = this.mAdded.size();
        if (N > 0) {
            writer.print(prefix);
            writer.println("Added Fragments:");
            for (i = 0; i < N; i++) {
                f = (Fragment) this.mAdded.get(i);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i);
                writer.print(": ");
                writer.println(f.toString());
            }
        }
        if (this.mCreatedMenus != null) {
            N = this.mCreatedMenus.size();
            if (N > 0) {
                writer.print(prefix);
                writer.println("Fragments Created Menus:");
                for (i = 0; i < N; i++) {
                    f = (Fragment) this.mCreatedMenus.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(f.toString());
                }
            }
        }
        if (this.mBackStack != null) {
            N = this.mBackStack.size();
            if (N > 0) {
                writer.print(prefix);
                writer.println("Back Stack:");
                for (i = 0; i < N; i++) {
                    bs = (BackStackRecord) this.mBackStack.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(bs.toString());
                    bs.dump(innerPrefix, fd, writer, args);
                }
            }
        }
        synchronized (this) {
            if (this.mBackStackIndices != null) {
                N = this.mBackStackIndices.size();
                if (N > 0) {
                    writer.print(prefix);
                    writer.println("Back Stack Indices:");
                    for (i = 0; i < N; i++) {
                        bs = (BackStackRecord) this.mBackStackIndices.get(i);
                        writer.print(prefix);
                        writer.print("  #");
                        writer.print(i);
                        writer.print(": ");
                        writer.println(bs);
                    }
                }
            }
            if (this.mAvailBackStackIndices != null && this.mAvailBackStackIndices.size() > 0) {
                writer.print(prefix);
                writer.print("mAvailBackStackIndices: ");
                writer.println(Arrays.toString(this.mAvailBackStackIndices.toArray()));
            }
        }
        if (this.mPendingActions != null) {
            N = this.mPendingActions.size();
            if (N > 0) {
                writer.print(prefix);
                writer.println("Pending Actions:");
                while (i2 < N) {
                    OpGenerator r = (OpGenerator) this.mPendingActions.get(i2);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i2);
                    writer.print(": ");
                    writer.println(r);
                    i2++;
                }
            }
        }
        writer.print(prefix);
        writer.println("FragmentManager misc state:");
        writer.print(prefix);
        writer.print("  mHost=");
        writer.println(this.mHost);
        writer.print(prefix);
        writer.print("  mContainer=");
        writer.println(this.mContainer);
        if (this.mParent != null) {
            writer.print(prefix);
            writer.print("  mParent=");
            writer.println(this.mParent);
        }
        writer.print(prefix);
        writer.print("  mCurState=");
        writer.print(this.mCurState);
        writer.print(" mStateSaved=");
        writer.print(this.mStateSaved);
        writer.print(" mStopped=");
        writer.print(this.mStopped);
        writer.print(" mDestroyed=");
        writer.println(this.mDestroyed);
        if (this.mNeedMenuInvalidate) {
            writer.print(prefix);
            writer.print("  mNeedMenuInvalidate=");
            writer.println(this.mNeedMenuInvalidate);
        }
        if (this.mNoTransactionsBecause != null) {
            writer.print(prefix);
            writer.print("  mNoTransactionsBecause=");
            writer.println(this.mNoTransactionsBecause);
        }
    }

    static AnimationOrAnimator makeOpenCloseAnimation(Context context, float startScale, float endScale, float startAlpha, float endAlpha) {
        Animation set = new AnimationSet(false);
        ScaleAnimation scale = new ScaleAnimation(startScale, endScale, startScale, endScale, 1, 0.5f, 1, 0.5f);
        scale.setInterpolator(DECELERATE_QUINT);
        scale.setDuration(220);
        set.addAnimation(scale);
        AlphaAnimation alpha = new AlphaAnimation(startAlpha, endAlpha);
        alpha.setInterpolator(DECELERATE_CUBIC);
        alpha.setDuration(220);
        set.addAnimation(alpha);
        return new AnimationOrAnimator(set, null);
    }

    static AnimationOrAnimator makeFadeAnimation(Context context, float start, float end) {
        Animation anim = new AlphaAnimation(start, end);
        anim.setInterpolator(DECELERATE_CUBIC);
        anim.setDuration(220);
        return new AnimationOrAnimator(anim, null);
    }

    AnimationOrAnimator loadAnimation(Fragment fragment, int transit, boolean enter, int transitionStyle) {
        int nextAnim = fragment.getNextAnim();
        Animation animation = fragment.onCreateAnimation(transit, enter, nextAnim);
        if (animation != null) {
            return new AnimationOrAnimator(animation, null);
        }
        Animator animator = fragment.onCreateAnimator(transit, enter, nextAnim);
        if (animator != null) {
            return new AnimationOrAnimator(animator, null);
        }
        if (nextAnim != 0) {
            boolean isAnim = "anim".equals(this.mHost.getContext().getResources().getResourceTypeName(nextAnim));
            boolean successfulLoad = false;
            if (isAnim) {
                try {
                    animation = AnimationUtils.loadAnimation(this.mHost.getContext(), nextAnim);
                    if (animation != null) {
                        return new AnimationOrAnimator(animation, null);
                    }
                    successfulLoad = true;
                } catch (NotFoundException e) {
                    throw e;
                } catch (RuntimeException e2) {
                }
            }
            if (!successfulLoad) {
                try {
                    animator = AnimatorInflater.loadAnimator(this.mHost.getContext(), nextAnim);
                    if (animator != null) {
                        return new AnimationOrAnimator(animator, null);
                    }
                } catch (RuntimeException e3) {
                    if (isAnim) {
                        throw e3;
                    }
                    animation = AnimationUtils.loadAnimation(this.mHost.getContext(), nextAnim);
                    if (animation != null) {
                        return new AnimationOrAnimator(animation, null);
                    }
                }
            }
        }
        if (transit == 0) {
            return null;
        }
        int styleIndex = transitToStyleIndex(transit, enter);
        if (styleIndex < 0) {
            return null;
        }
        switch (styleIndex) {
            case 1:
                return makeOpenCloseAnimation(this.mHost.getContext(), 1.125f, 1.0f, 0.0f, 1.0f);
            case 2:
                return makeOpenCloseAnimation(this.mHost.getContext(), 1.0f, 0.975f, 1.0f, 0.0f);
            case 3:
                return makeOpenCloseAnimation(this.mHost.getContext(), 0.975f, 1.0f, 0.0f, 1.0f);
            case 4:
                return makeOpenCloseAnimation(this.mHost.getContext(), 1.0f, 1.075f, 1.0f, 0.0f);
            case 5:
                return makeFadeAnimation(this.mHost.getContext(), 0.0f, 1.0f);
            case 6:
                return makeFadeAnimation(this.mHost.getContext(), 1.0f, 0.0f);
            default:
                if (transitionStyle == 0 && this.mHost.onHasWindowAnimations()) {
                    transitionStyle = this.mHost.onGetWindowAnimations();
                }
                return transitionStyle == 0 ? null : null;
        }
    }

    public void performPendingDeferredStart(Fragment f) {
        if (f.mDeferStart) {
            if (this.mExecutingActions) {
                this.mHavePendingDeferredStart = true;
                return;
            }
            f.mDeferStart = false;
            moveToState(f, this.mCurState, 0, 0, false);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0030, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void setHWLayerAnimListenerIfAlpha(View v, AnimationOrAnimator anim) {
        if (!(v == null || anim == null || !shouldRunOnHWLayer(v, anim))) {
            if (anim.animator != null) {
                anim.animator.addListener(new AnimatorOnHWLayerIfNeededListener(v));
            } else {
                AnimationListener originalListener = getAnimationListener(anim.animation);
                v.setLayerType(2, null);
                anim.animation.setAnimationListener(new AnimateOnHWLayerIfNeededListener(v, originalListener));
            }
        }
    }

    private static AnimationListener getAnimationListener(Animation animation) {
        try {
            if (sAnimationListenerField == null) {
                sAnimationListenerField = Animation.class.getDeclaredField("mListener");
                sAnimationListenerField.setAccessible(true);
            }
            return (AnimationListener) sAnimationListenerField.get(animation);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "No field with the name mListener is found in Animation class", e);
            return null;
        } catch (IllegalAccessException e2) {
            Log.e(TAG, "Cannot access Animation's mListener field", e2);
            return null;
        }
    }

    boolean isStateAtLeast(int state) {
        return this.mCurState >= state;
    }

    /* JADX WARNING: Removed duplicated region for block: B:218:0x0478  */
    /* JADX WARNING: Missing block: B:82:0x01ac, code:
            r1 = r0;
            ensureInflatedFragmentView(r16);
     */
    /* JADX WARNING: Missing block: B:83:0x01b0, code:
            if (r1 <= 1) goto L_0x02a8;
     */
    /* JADX WARNING: Missing block: B:85:0x01b4, code:
            if (DEBUG == false) goto L_0x01cc;
     */
    /* JADX WARNING: Missing block: B:86:0x01b6, code:
            r0 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("moveto ACTIVITY_CREATED: ");
            r2.append(r8);
            android.util.Log.v(r0, r2.toString());
     */
    /* JADX WARNING: Missing block: B:88:0x01ce, code:
            if (r8.mFromLayout != false) goto L_0x0293;
     */
    /* JADX WARNING: Missing block: B:89:0x01d0, code:
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:90:0x01d3, code:
            if (r8.mContainerId == 0) goto L_0x0246;
     */
    /* JADX WARNING: Missing block: B:92:0x01d8, code:
            if (r8.mContainerId != -1) goto L_0x01f8;
     */
    /* JADX WARNING: Missing block: B:93:0x01da, code:
            r3 = new java.lang.StringBuilder();
            r3.append("Cannot create fragment ");
            r3.append(r8);
            r3.append(" for a container view with no id");
            throwException(new java.lang.IllegalArgumentException(r3.toString()));
     */
    /* JADX WARNING: Missing block: B:94:0x01f8, code:
            r2 = (android.view.ViewGroup) r7.mContainer.onFindViewById(r8.mContainerId);
     */
    /* JADX WARNING: Missing block: B:95:0x0202, code:
            if (r2 != null) goto L_0x0245;
     */
    /* JADX WARNING: Missing block: B:97:0x0206, code:
            if (r8.mRestored != false) goto L_0x0245;
     */
    /* JADX WARNING: Missing block: B:99:?, code:
            r0 = r16.getResources().getResourceName(r8.mContainerId);
     */
    /* JADX WARNING: Missing block: B:101:0x0214, code:
            r0 = android.support.v4.os.EnvironmentCompat.MEDIA_UNKNOWN;
     */
    /* JADX WARNING: Missing block: B:147:0x0328, code:
            if (r0 >= 4) goto L_0x034a;
     */
    /* JADX WARNING: Missing block: B:149:0x032c, code:
            if (DEBUG == false) goto L_0x0344;
     */
    /* JADX WARNING: Missing block: B:150:0x032e, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("movefrom STARTED: ");
            r2.append(r8);
            android.util.Log.v(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:151:0x0344, code:
            r16.performStop();
            dispatchOnFragmentStopped(r8, false);
     */
    /* JADX WARNING: Missing block: B:152:0x034a, code:
            if (r0 >= 3) goto L_0x0369;
     */
    /* JADX WARNING: Missing block: B:154:0x034e, code:
            if (DEBUG == false) goto L_0x0366;
     */
    /* JADX WARNING: Missing block: B:155:0x0350, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("movefrom STOPPED: ");
            r2.append(r8);
            android.util.Log.v(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:156:0x0366, code:
            r16.performReallyStop();
     */
    /* JADX WARNING: Missing block: B:157:0x0369, code:
            if (r0 >= 2) goto L_0x03fa;
     */
    /* JADX WARNING: Missing block: B:159:0x036d, code:
            if (DEBUG == false) goto L_0x0385;
     */
    /* JADX WARNING: Missing block: B:160:0x036f, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("movefrom ACTIVITY_CREATED: ");
            r2.append(r8);
            android.util.Log.v(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:162:0x0387, code:
            if (r8.mView == null) goto L_0x0398;
     */
    /* JADX WARNING: Missing block: B:164:0x038f, code:
            if (r7.mHost.onShouldSaveFragmentState(r8) == false) goto L_0x0398;
     */
    /* JADX WARNING: Missing block: B:166:0x0393, code:
            if (r8.mSavedViewState != null) goto L_0x0398;
     */
    /* JADX WARNING: Missing block: B:167:0x0395, code:
            saveFragmentViewState(r16);
     */
    /* JADX WARNING: Missing block: B:168:0x0398, code:
            r16.performDestroyView();
            dispatchOnFragmentViewDestroyed(r8, false);
     */
    /* JADX WARNING: Missing block: B:169:0x03a0, code:
            if (r8.mView == null) goto L_0x03e6;
     */
    /* JADX WARNING: Missing block: B:171:0x03a4, code:
            if (r8.mContainer == null) goto L_0x03e6;
     */
    /* JADX WARNING: Missing block: B:172:0x03a6, code:
            r8.mContainer.endViewTransition(r8.mView);
            r8.mView.clearAnimation();
            r1 = null;
     */
    /* JADX WARNING: Missing block: B:173:0x03b6, code:
            if (r7.mCurState <= 0) goto L_0x03d3;
     */
    /* JADX WARNING: Missing block: B:175:0x03ba, code:
            if (r7.mDestroyed != false) goto L_0x03d3;
     */
    /* JADX WARNING: Missing block: B:177:0x03c2, code:
            if (r8.mView.getVisibility() != 0) goto L_0x03d3;
     */
    /* JADX WARNING: Missing block: B:179:0x03c8, code:
            if (r8.mPostponedAlpha < 0.0f) goto L_0x03d3;
     */
    /* JADX WARNING: Missing block: B:180:0x03ca, code:
            r1 = loadAnimation(r8, r18, false, r19);
     */
    /* JADX WARNING: Missing block: B:181:0x03d3, code:
            r2 = r18;
            r4 = r19;
     */
    /* JADX WARNING: Missing block: B:182:0x03d7, code:
            r8.mPostponedAlpha = 0.0f;
     */
    /* JADX WARNING: Missing block: B:183:0x03d9, code:
            if (r1 == null) goto L_0x03de;
     */
    /* JADX WARNING: Missing block: B:184:0x03db, code:
            animateRemoveFragment(r8, r1, r0);
     */
    /* JADX WARNING: Missing block: B:185:0x03de, code:
            r8.mContainer.removeView(r8.mView);
     */
    /* JADX WARNING: Missing block: B:186:0x03e6, code:
            r2 = r18;
            r4 = r19;
     */
    /* JADX WARNING: Missing block: B:187:0x03ea, code:
            r8.mContainer = null;
            r8.mView = null;
            r8.mViewLifecycleOwner = null;
            r8.mViewLifecycleOwnerLiveData.setValue(null);
            r8.mInnerView = null;
            r8.mInLayout = false;
     */
    /* JADX WARNING: Missing block: B:189:0x03fe, code:
            if (r0 >= 1) goto L_0x0474;
     */
    /* JADX WARNING: Missing block: B:191:0x0402, code:
            if (r7.mDestroyed == false) goto L_0x0425;
     */
    /* JADX WARNING: Missing block: B:193:0x0408, code:
            if (r16.getAnimatingAway() == null) goto L_0x0415;
     */
    /* JADX WARNING: Missing block: B:194:0x040a, code:
            r1 = r16.getAnimatingAway();
            r8.setAnimatingAway(null);
            r1.clearAnimation();
     */
    /* JADX WARNING: Missing block: B:196:0x0419, code:
            if (r16.getAnimator() == null) goto L_0x0425;
     */
    /* JADX WARNING: Missing block: B:197:0x041b, code:
            r1 = r16.getAnimator();
            r8.setAnimator(null);
            r1.cancel();
     */
    /* JADX WARNING: Missing block: B:199:0x0429, code:
            if (r16.getAnimatingAway() != null) goto L_0x0470;
     */
    /* JADX WARNING: Missing block: B:201:0x042f, code:
            if (r16.getAnimator() == null) goto L_0x0432;
     */
    /* JADX WARNING: Missing block: B:203:0x0434, code:
            if (DEBUG == false) goto L_0x044c;
     */
    /* JADX WARNING: Missing block: B:204:0x0436, code:
            r1 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("movefrom CREATED: ");
            r3.append(r8);
            android.util.Log.v(r1, r3.toString());
     */
    /* JADX WARNING: Missing block: B:206:0x044e, code:
            if (r8.mRetaining != false) goto L_0x0457;
     */
    /* JADX WARNING: Missing block: B:207:0x0450, code:
            r16.performDestroy();
            dispatchOnFragmentDestroyed(r8, false);
     */
    /* JADX WARNING: Missing block: B:208:0x0457, code:
            r8.mState = 0;
     */
    /* JADX WARNING: Missing block: B:209:0x0459, code:
            r16.performDetach();
            dispatchOnFragmentDetached(r8, false);
     */
    /* JADX WARNING: Missing block: B:210:0x045f, code:
            if (r20 != false) goto L_0x0474;
     */
    /* JADX WARNING: Missing block: B:212:0x0463, code:
            if (r8.mRetaining != false) goto L_0x0469;
     */
    /* JADX WARNING: Missing block: B:213:0x0465, code:
            makeInactive(r16);
     */
    /* JADX WARNING: Missing block: B:214:0x0469, code:
            r8.mHost = null;
            r8.mParentFragment = null;
            r8.mFragmentManager = null;
     */
    /* JADX WARNING: Missing block: B:215:0x0470, code:
            r8.setStateAfterAnimating(r0);
            r0 = 1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void moveToState(Fragment f, int newState, int transit, int transitionStyle, boolean keepActive) {
        int newState2;
        int i;
        int i2;
        String str;
        StringBuilder stringBuilder;
        Fragment fragment = f;
        boolean z = true;
        if (!fragment.mAdded || fragment.mDetached) {
            newState2 = newState;
            if (newState2 > 1) {
                newState2 = 1;
            }
        } else {
            newState2 = newState;
        }
        if (fragment.mRemoving && newState > fragment.mState) {
            if (fragment.mState == 0 && f.isInBackStack()) {
                newState2 = 1;
            } else {
                newState2 = fragment.mState;
            }
        }
        if (fragment.mDeferStart && fragment.mState < 4 && newState > 3) {
            newState2 = 3;
        }
        if (fragment.mState > newState2) {
            if (fragment.mState > newState2) {
                switch (fragment.mState) {
                    case 2:
                        break;
                    case 1:
                        i = transit;
                        i2 = transitionStyle;
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        if (newState2 < 5) {
                            if (DEBUG) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("movefrom RESUMED: ");
                                stringBuilder.append(fragment);
                                Log.v(str, stringBuilder.toString());
                            }
                            f.performPause();
                            dispatchOnFragmentPaused(fragment, false);
                            break;
                        }
                        break;
                }
            }
        } else if (!fragment.mFromLayout || fragment.mInLayout) {
            if (!(f.getAnimatingAway() == null && f.getAnimator() == null)) {
                fragment.setAnimatingAway(null);
                fragment.setAnimator(null);
                moveToState(fragment, f.getStateAfterAnimating(), 0, 0, true);
            }
            switch (fragment.mState) {
                case 0:
                    if (newState2 > 0) {
                        FragmentManagerImpl fragmentManagerImpl;
                        if (DEBUG) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("moveto CREATED: ");
                            stringBuilder.append(fragment);
                            Log.v(str, stringBuilder.toString());
                        }
                        if (fragment.mSavedFragmentState != null) {
                            fragment.mSavedFragmentState.setClassLoader(this.mHost.getContext().getClassLoader());
                            fragment.mSavedViewState = fragment.mSavedFragmentState.getSparseParcelableArray(VIEW_STATE_TAG);
                            fragment.mTarget = getFragment(fragment.mSavedFragmentState, TARGET_STATE_TAG);
                            if (fragment.mTarget != null) {
                                fragment.mTargetRequestCode = fragment.mSavedFragmentState.getInt(TARGET_REQUEST_CODE_STATE_TAG, 0);
                            }
                            if (fragment.mSavedUserVisibleHint != null) {
                                fragment.mUserVisibleHint = fragment.mSavedUserVisibleHint.booleanValue();
                                fragment.mSavedUserVisibleHint = null;
                            } else {
                                fragment.mUserVisibleHint = fragment.mSavedFragmentState.getBoolean(USER_VISIBLE_HINT_TAG, true);
                            }
                            if (!fragment.mUserVisibleHint) {
                                fragment.mDeferStart = true;
                                if (newState2 > 3) {
                                    newState2 = 3;
                                }
                            }
                        }
                        fragment.mHost = this.mHost;
                        fragment.mParentFragment = this.mParent;
                        if (this.mParent != null) {
                            fragmentManagerImpl = this.mParent.mChildFragmentManager;
                        } else {
                            fragmentManagerImpl = this.mHost.getFragmentManagerImpl();
                        }
                        fragment.mFragmentManager = fragmentManagerImpl;
                        if (fragment.mTarget != null) {
                            if (this.mActive.get(fragment.mTarget.mIndex) != fragment.mTarget) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Fragment ");
                                stringBuilder.append(fragment);
                                stringBuilder.append(" declared target fragment ");
                                stringBuilder.append(fragment.mTarget);
                                stringBuilder.append(" that does not belong to this FragmentManager!");
                                throw new IllegalStateException(stringBuilder.toString());
                            } else if (fragment.mTarget.mState < 1) {
                                moveToState(fragment.mTarget, 1, 0, 0, true);
                            }
                        }
                        dispatchOnFragmentPreAttached(fragment, this.mHost.getContext(), false);
                        fragment.mCalled = false;
                        fragment.onAttach(this.mHost.getContext());
                        if (fragment.mCalled) {
                            if (fragment.mParentFragment == null) {
                                this.mHost.onAttachFragment(fragment);
                            } else {
                                fragment.mParentFragment.onAttachFragment(fragment);
                            }
                            dispatchOnFragmentAttached(fragment, this.mHost.getContext(), false);
                            if (fragment.mIsCreated) {
                                fragment.restoreChildFragmentState(fragment.mSavedFragmentState);
                                fragment.mState = 1;
                            } else {
                                dispatchOnFragmentPreCreated(fragment, fragment.mSavedFragmentState, false);
                                fragment.performCreate(fragment.mSavedFragmentState);
                                dispatchOnFragmentCreated(fragment, fragment.mSavedFragmentState, false);
                            }
                            fragment.mRetaining = false;
                            break;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Fragment ");
                        stringBuilder.append(fragment);
                        stringBuilder.append(" did not call through to super.onAttach()");
                        throw new SuperNotCalledException(stringBuilder.toString());
                    }
                    break;
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
            }
        } else {
            return;
        }
        i = transit;
        i2 = transitionStyle;
        if (fragment.mState != newState2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("moveToState: Fragment state for ");
            stringBuilder2.append(fragment);
            stringBuilder2.append(" not updated inline; ");
            stringBuilder2.append("expected state ");
            stringBuilder2.append(newState2);
            stringBuilder2.append(" found ");
            stringBuilder2.append(fragment.mState);
            Log.w(str2, stringBuilder2.toString());
            fragment.mState = newState2;
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("No view found for id 0x");
        stringBuilder3.append(Integer.toHexString(fragment.mContainerId));
        stringBuilder3.append(" (");
        stringBuilder3.append(resName);
        stringBuilder3.append(") for fragment ");
        stringBuilder3.append(fragment);
        throwException(new IllegalArgumentException(stringBuilder3.toString()));
        ViewGroup container = container;
        fragment.mContainer = container;
        fragment.performCreateView(fragment.performGetLayoutInflater(fragment.mSavedFragmentState), container, fragment.mSavedFragmentState);
        if (fragment.mView != null) {
            fragment.mInnerView = fragment.mView;
            fragment.mView.setSaveFromParentEnabled(false);
            if (container != null) {
                container.addView(fragment.mView);
            }
            if (fragment.mHidden) {
                fragment.mView.setVisibility(8);
            }
            fragment.onViewCreated(fragment.mView, fragment.mSavedFragmentState);
            dispatchOnFragmentViewCreated(fragment, fragment.mView, fragment.mSavedFragmentState, false);
            if (fragment.mView.getVisibility() != 0 || fragment.mContainer == null) {
                z = false;
            }
            fragment.mIsNewlyAdded = z;
        } else {
            fragment.mInnerView = null;
        }
        fragment.performActivityCreated(fragment.mSavedFragmentState);
        dispatchOnFragmentActivityCreated(fragment, fragment.mSavedFragmentState, false);
        if (fragment.mView != null) {
            fragment.restoreViewState(fragment.mSavedFragmentState);
        }
        fragment.mSavedFragmentState = null;
        newState2 = newState;
        if (newState2 > 2) {
            fragment.mState = 3;
        }
        if (newState2 > 3) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("moveto STARTED: ");
                stringBuilder.append(fragment);
                Log.v(str, stringBuilder.toString());
            }
            f.performStart();
            dispatchOnFragmentStarted(fragment, false);
        }
        if (newState2 > 4) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("moveto RESUMED: ");
                stringBuilder.append(fragment);
                Log.v(str, stringBuilder.toString());
            }
            f.performResume();
            dispatchOnFragmentResumed(fragment, false);
            fragment.mSavedFragmentState = null;
            fragment.mSavedViewState = null;
        }
        i = transit;
        i2 = transitionStyle;
        if (fragment.mState != newState2) {
        }
    }

    private void animateRemoveFragment(@NonNull final Fragment fragment, @NonNull AnimationOrAnimator anim, int newState) {
        final View viewToAnimate = fragment.mView;
        final ViewGroup container = fragment.mContainer;
        container.startViewTransition(viewToAnimate);
        fragment.setStateAfterAnimating(newState);
        if (anim.animation != null) {
            Animation animation = new EndViewTransitionAnimator(anim.animation, container, viewToAnimate);
            fragment.setAnimatingAway(fragment.mView);
            animation.setAnimationListener(new AnimationListenerWrapper(getAnimationListener(animation)) {
                public void onAnimationEnd(Animation animation) {
                    super.onAnimationEnd(animation);
                    container.post(new Runnable() {
                        public void run() {
                            if (fragment.getAnimatingAway() != null) {
                                fragment.setAnimatingAway(null);
                                FragmentManagerImpl.this.moveToState(fragment, fragment.getStateAfterAnimating(), 0, 0, false);
                            }
                        }
                    });
                }
            });
            setHWLayerAnimListenerIfAlpha(viewToAnimate, anim);
            fragment.mView.startAnimation(animation);
            return;
        }
        Animator animator = anim.animator;
        fragment.setAnimator(anim.animator);
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                container.endViewTransition(viewToAnimate);
                Animator animator = fragment.getAnimator();
                fragment.setAnimator(null);
                if (animator != null && container.indexOfChild(viewToAnimate) < 0) {
                    FragmentManagerImpl.this.moveToState(fragment, fragment.getStateAfterAnimating(), 0, 0, false);
                }
            }
        });
        animator.setTarget(fragment.mView);
        setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
        animator.start();
    }

    void moveToState(Fragment f) {
        moveToState(f, this.mCurState, 0, 0, false);
    }

    void ensureInflatedFragmentView(Fragment f) {
        if (f.mFromLayout && !f.mPerformedCreateView) {
            f.performCreateView(f.performGetLayoutInflater(f.mSavedFragmentState), null, f.mSavedFragmentState);
            if (f.mView != null) {
                f.mInnerView = f.mView;
                f.mView.setSaveFromParentEnabled(false);
                if (f.mHidden) {
                    f.mView.setVisibility(8);
                }
                f.onViewCreated(f.mView, f.mSavedFragmentState);
                dispatchOnFragmentViewCreated(f, f.mView, f.mSavedFragmentState, false);
                return;
            }
            f.mInnerView = null;
        }
    }

    void completeShowHideFragment(final Fragment fragment) {
        if (fragment.mView != null) {
            AnimationOrAnimator anim = loadAnimation(fragment, fragment.getNextTransition(), fragment.mHidden ^ true, fragment.getNextTransitionStyle());
            if (anim == null || anim.animator == null) {
                if (anim != null) {
                    setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                    fragment.mView.startAnimation(anim.animation);
                    anim.animation.start();
                }
                int visibility = (!fragment.mHidden || fragment.isHideReplaced()) ? 0 : 8;
                fragment.mView.setVisibility(visibility);
                if (fragment.isHideReplaced()) {
                    fragment.setHideReplaced(false);
                }
            } else {
                anim.animator.setTarget(fragment.mView);
                if (!fragment.mHidden) {
                    fragment.mView.setVisibility(0);
                } else if (fragment.isHideReplaced()) {
                    fragment.setHideReplaced(false);
                } else {
                    final ViewGroup container = fragment.mContainer;
                    final View animatingView = fragment.mView;
                    container.startViewTransition(animatingView);
                    anim.animator.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            container.endViewTransition(animatingView);
                            animation.removeListener(this);
                            if (fragment.mView != null) {
                                fragment.mView.setVisibility(8);
                            }
                        }
                    });
                }
                setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                anim.animator.start();
            }
        }
        if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) {
            this.mNeedMenuInvalidate = true;
        }
        fragment.mHiddenChanged = false;
        fragment.onHiddenChanged(fragment.mHidden);
    }

    void moveFragmentToExpectedState(Fragment f) {
        if (f != null) {
            int nextState = this.mCurState;
            if (f.mRemoving) {
                if (f.isInBackStack()) {
                    nextState = Math.min(nextState, 1);
                } else {
                    nextState = Math.min(nextState, 0);
                }
            }
            moveToState(f, nextState, f.getNextTransition(), f.getNextTransitionStyle(), false);
            if (f.mView != null) {
                Fragment underFragment = findFragmentUnder(f);
                if (underFragment != null) {
                    View underView = underFragment.mView;
                    ViewGroup container = f.mContainer;
                    int underIndex = container.indexOfChild(underView);
                    int viewIndex = container.indexOfChild(f.mView);
                    if (viewIndex < underIndex) {
                        container.removeViewAt(viewIndex);
                        container.addView(f.mView, underIndex);
                    }
                }
                if (f.mIsNewlyAdded && f.mContainer != null) {
                    if (f.mPostponedAlpha > 0.0f) {
                        f.mView.setAlpha(f.mPostponedAlpha);
                    }
                    f.mPostponedAlpha = 0.0f;
                    f.mIsNewlyAdded = false;
                    AnimationOrAnimator anim = loadAnimation(f, f.getNextTransition(), true, f.getNextTransitionStyle());
                    if (anim != null) {
                        setHWLayerAnimListenerIfAlpha(f.mView, anim);
                        if (anim.animation != null) {
                            f.mView.startAnimation(anim.animation);
                        } else {
                            anim.animator.setTarget(f.mView);
                            anim.animator.start();
                        }
                    }
                }
            }
            if (f.mHiddenChanged) {
                completeShowHideFragment(f);
            }
        }
    }

    void moveToState(int newState, boolean always) {
        if (this.mHost == null && newState != 0) {
            throw new IllegalStateException("No activity");
        } else if (always || newState != this.mCurState) {
            this.mCurState = newState;
            if (this.mActive != null) {
                int i;
                int numAdded = this.mAdded.size();
                for (i = 0; i < numAdded; i++) {
                    moveFragmentToExpectedState((Fragment) this.mAdded.get(i));
                }
                i = this.mActive.size();
                for (int i2 = 0; i2 < i; i2++) {
                    Fragment f = (Fragment) this.mActive.valueAt(i2);
                    if (f != null && ((f.mRemoving || f.mDetached) && !f.mIsNewlyAdded)) {
                        moveFragmentToExpectedState(f);
                    }
                }
                startPendingDeferredFragments();
                if (this.mNeedMenuInvalidate && this.mHost != null && this.mCurState == 5) {
                    this.mHost.onSupportInvalidateOptionsMenu();
                    this.mNeedMenuInvalidate = false;
                }
            }
        }
    }

    void startPendingDeferredFragments() {
        if (this.mActive != null) {
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = (Fragment) this.mActive.valueAt(i);
                if (f != null) {
                    performPendingDeferredStart(f);
                }
            }
        }
    }

    void makeActive(Fragment f) {
        if (f.mIndex < 0) {
            int i = this.mNextFragmentIndex;
            this.mNextFragmentIndex = i + 1;
            f.setIndex(i, this.mParent);
            if (this.mActive == null) {
                this.mActive = new SparseArray();
            }
            this.mActive.put(f.mIndex, f);
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Allocated fragment index ");
                stringBuilder.append(f);
                Log.v(str, stringBuilder.toString());
            }
        }
    }

    void makeInactive(Fragment f) {
        if (f.mIndex >= 0) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Freeing fragment index ");
                stringBuilder.append(f);
                Log.v(str, stringBuilder.toString());
            }
            this.mActive.put(f.mIndex, null);
            f.initState();
        }
    }

    public void addFragment(Fragment fragment, boolean moveToStateNow) {
        StringBuilder stringBuilder;
        if (DEBUG) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("add: ");
            stringBuilder.append(fragment);
            Log.v(str, stringBuilder.toString());
        }
        makeActive(fragment);
        if (!fragment.mDetached) {
            if (this.mAdded.contains(fragment)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Fragment already added: ");
                stringBuilder.append(fragment);
                throw new IllegalStateException(stringBuilder.toString());
            }
            synchronized (this.mAdded) {
                this.mAdded.add(fragment);
            }
            fragment.mAdded = true;
            fragment.mRemoving = false;
            if (fragment.mView == null) {
                fragment.mHiddenChanged = false;
            }
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                this.mNeedMenuInvalidate = true;
            }
            if (moveToStateNow) {
                moveToState(fragment);
            }
        }
    }

    public void removeFragment(Fragment fragment) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove: ");
            stringBuilder.append(fragment);
            stringBuilder.append(" nesting=");
            stringBuilder.append(fragment.mBackStackNesting);
            Log.v(str, stringBuilder.toString());
        }
        boolean inactive = fragment.isInBackStack() ^ true;
        if (!fragment.mDetached || inactive) {
            synchronized (this.mAdded) {
                this.mAdded.remove(fragment);
            }
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                this.mNeedMenuInvalidate = true;
            }
            fragment.mAdded = false;
            fragment.mRemoving = true;
        }
    }

    public void hideFragment(Fragment fragment) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hide: ");
            stringBuilder.append(fragment);
            Log.v(str, stringBuilder.toString());
        }
        if (!fragment.mHidden) {
            fragment.mHidden = true;
            fragment.mHiddenChanged = true ^ fragment.mHiddenChanged;
        }
    }

    public void showFragment(Fragment fragment) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("show: ");
            stringBuilder.append(fragment);
            Log.v(str, stringBuilder.toString());
        }
        if (fragment.mHidden) {
            fragment.mHidden = false;
            fragment.mHiddenChanged ^= 1;
        }
    }

    public void detachFragment(Fragment fragment) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("detach: ");
            stringBuilder.append(fragment);
            Log.v(str, stringBuilder.toString());
        }
        if (!fragment.mDetached) {
            fragment.mDetached = true;
            if (fragment.mAdded) {
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("remove from detach: ");
                    stringBuilder2.append(fragment);
                    Log.v(str2, stringBuilder2.toString());
                }
                synchronized (this.mAdded) {
                    this.mAdded.remove(fragment);
                }
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    this.mNeedMenuInvalidate = true;
                }
                fragment.mAdded = false;
            }
        }
    }

    public void attachFragment(Fragment fragment) {
        String str;
        StringBuilder stringBuilder;
        if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("attach: ");
            stringBuilder.append(fragment);
            Log.v(str, stringBuilder.toString());
        }
        if (fragment.mDetached) {
            fragment.mDetached = false;
            if (!fragment.mAdded) {
                if (this.mAdded.contains(fragment)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Fragment already added: ");
                    stringBuilder.append(fragment);
                    throw new IllegalStateException(stringBuilder.toString());
                }
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("add from attach: ");
                    stringBuilder.append(fragment);
                    Log.v(str, stringBuilder.toString());
                }
                synchronized (this.mAdded) {
                    this.mAdded.add(fragment);
                }
                fragment.mAdded = true;
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    this.mNeedMenuInvalidate = true;
                }
            }
        }
    }

    @Nullable
    public Fragment findFragmentById(int id) {
        int i;
        Fragment f;
        for (i = this.mAdded.size() - 1; i >= 0; i--) {
            f = (Fragment) this.mAdded.get(i);
            if (f != null && f.mFragmentId == id) {
                return f;
            }
        }
        if (this.mActive != null) {
            for (i = this.mActive.size() - 1; i >= 0; i--) {
                f = (Fragment) this.mActive.valueAt(i);
                if (f != null && f.mFragmentId == id) {
                    return f;
                }
            }
        }
        return null;
    }

    @Nullable
    public Fragment findFragmentByTag(@Nullable String tag) {
        int i;
        Fragment f;
        if (tag != null) {
            for (i = this.mAdded.size() - 1; i >= 0; i--) {
                f = (Fragment) this.mAdded.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        if (!(this.mActive == null || tag == null)) {
            for (i = this.mActive.size() - 1; i >= 0; i--) {
                f = (Fragment) this.mActive.valueAt(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        return null;
    }

    public Fragment findFragmentByWho(String who) {
        if (!(this.mActive == null || who == null)) {
            for (int i = this.mActive.size() - 1; i >= 0; i--) {
                Fragment f = (Fragment) this.mActive.valueAt(i);
                if (f != null) {
                    Fragment findFragmentByWho = f.findFragmentByWho(who);
                    f = findFragmentByWho;
                    if (findFragmentByWho != null) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    private void checkStateLoss() {
        if (isStateSaved()) {
            throw new IllegalStateException("Can not perform this action after onSaveInstanceState");
        } else if (this.mNoTransactionsBecause != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can not perform this action inside of ");
            stringBuilder.append(this.mNoTransactionsBecause);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public boolean isStateSaved() {
        return this.mStateSaved || this.mStopped;
    }

    public void enqueueAction(OpGenerator action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        synchronized (this) {
            if (!this.mDestroyed && this.mHost != null) {
                if (this.mPendingActions == null) {
                    this.mPendingActions = new ArrayList();
                }
                this.mPendingActions.add(action);
                scheduleCommit();
            } else if (allowStateLoss) {
            } else {
                throw new IllegalStateException("Activity has been destroyed");
            }
        }
    }

    private void scheduleCommit() {
        synchronized (this) {
            boolean pendingReady = false;
            boolean postponeReady = (this.mPostponedTransactions == null || this.mPostponedTransactions.isEmpty()) ? false : true;
            if (this.mPendingActions != null && this.mPendingActions.size() == 1) {
                pendingReady = true;
            }
            if (postponeReady || pendingReady) {
                this.mHost.getHandler().removeCallbacks(this.mExecCommit);
                this.mHost.getHandler().post(this.mExecCommit);
            }
        }
    }

    public int allocBackStackIndex(BackStackRecord bse) {
        synchronized (this) {
            int index;
            String str;
            StringBuilder stringBuilder;
            if (this.mAvailBackStackIndices == null || this.mAvailBackStackIndices.size() <= 0) {
                if (this.mBackStackIndices == null) {
                    this.mBackStackIndices = new ArrayList();
                }
                index = this.mBackStackIndices.size();
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Setting back stack index ");
                    stringBuilder.append(index);
                    stringBuilder.append(" to ");
                    stringBuilder.append(bse);
                    Log.v(str, stringBuilder.toString());
                }
                this.mBackStackIndices.add(bse);
                return index;
            }
            index = ((Integer) this.mAvailBackStackIndices.remove(this.mAvailBackStackIndices.size() - 1)).intValue();
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Adding back stack index ");
                stringBuilder.append(index);
                stringBuilder.append(" with ");
                stringBuilder.append(bse);
                Log.v(str, stringBuilder.toString());
            }
            this.mBackStackIndices.set(index, bse);
            return index;
        }
    }

    public void setBackStackIndex(int index, BackStackRecord bse) {
        synchronized (this) {
            if (this.mBackStackIndices == null) {
                this.mBackStackIndices = new ArrayList();
            }
            int N = this.mBackStackIndices.size();
            String str;
            StringBuilder stringBuilder;
            if (index < N) {
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Setting back stack index ");
                    stringBuilder.append(index);
                    stringBuilder.append(" to ");
                    stringBuilder.append(bse);
                    Log.v(str, stringBuilder.toString());
                }
                this.mBackStackIndices.set(index, bse);
            } else {
                while (N < index) {
                    this.mBackStackIndices.add(null);
                    if (this.mAvailBackStackIndices == null) {
                        this.mAvailBackStackIndices = new ArrayList();
                    }
                    if (DEBUG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Adding available back stack index ");
                        stringBuilder.append(N);
                        Log.v(str, stringBuilder.toString());
                    }
                    this.mAvailBackStackIndices.add(Integer.valueOf(N));
                    N++;
                }
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Adding back stack index ");
                    stringBuilder.append(index);
                    stringBuilder.append(" with ");
                    stringBuilder.append(bse);
                    Log.v(str, stringBuilder.toString());
                }
                this.mBackStackIndices.add(bse);
            }
        }
    }

    public void freeBackStackIndex(int index) {
        synchronized (this) {
            this.mBackStackIndices.set(index, null);
            if (this.mAvailBackStackIndices == null) {
                this.mAvailBackStackIndices = new ArrayList();
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Freeing back stack index ");
                stringBuilder.append(index);
                Log.v(str, stringBuilder.toString());
            }
            this.mAvailBackStackIndices.add(Integer.valueOf(index));
        }
    }

    private void ensureExecReady(boolean allowStateLoss) {
        if (this.mExecutingActions) {
            throw new IllegalStateException("FragmentManager is already executing transactions");
        } else if (this.mHost == null) {
            throw new IllegalStateException("Fragment host has been destroyed");
        } else if (Looper.myLooper() == this.mHost.getHandler().getLooper()) {
            if (!allowStateLoss) {
                checkStateLoss();
            }
            if (this.mTmpRecords == null) {
                this.mTmpRecords = new ArrayList();
                this.mTmpIsPop = new ArrayList();
            }
            this.mExecutingActions = true;
            try {
                executePostponedTransaction(null, null);
            } finally {
                this.mExecutingActions = false;
            }
        } else {
            throw new IllegalStateException("Must be called from main thread of fragment host");
        }
    }

    public void execSingleAction(OpGenerator action, boolean allowStateLoss) {
        if (!allowStateLoss || (this.mHost != null && !this.mDestroyed)) {
            ensureExecReady(allowStateLoss);
            if (action.generateOps(this.mTmpRecords, this.mTmpIsPop)) {
                this.mExecutingActions = true;
                try {
                    removeRedundantOperationsAndExecute(this.mTmpRecords, this.mTmpIsPop);
                } finally {
                    cleanupExec();
                }
            }
            doPendingDeferredStart();
            burpActive();
        }
    }

    private void cleanupExec() {
        this.mExecutingActions = false;
        this.mTmpIsPop.clear();
        this.mTmpRecords.clear();
    }

    public boolean execPendingActions() {
        ensureExecReady(true);
        boolean didSomething = false;
        while (generateOpsForPendingActions(this.mTmpRecords, this.mTmpIsPop)) {
            this.mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(this.mTmpRecords, this.mTmpIsPop);
                cleanupExec();
                didSomething = true;
            } catch (Throwable th) {
                cleanupExec();
                throw th;
            }
        }
        doPendingDeferredStart();
        burpActive();
        return didSomething;
    }

    private void executePostponedTransaction(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop) {
        int numPostponed = this.mPostponedTransactions == null ? 0 : this.mPostponedTransactions.size();
        int i = 0;
        while (i < numPostponed) {
            int index;
            StartEnterTransitionListener listener = (StartEnterTransitionListener) this.mPostponedTransactions.get(i);
            if (!(records == null || listener.mIsBack)) {
                index = records.indexOf(listener.mRecord);
                if (index != -1 && ((Boolean) isRecordPop.get(index)).booleanValue()) {
                    listener.cancelTransaction();
                    i++;
                }
            }
            if (listener.isReady() || (records != null && listener.mRecord.interactsWith(records, 0, records.size()))) {
                this.mPostponedTransactions.remove(i);
                i--;
                numPostponed--;
                if (!(records == null || listener.mIsBack)) {
                    index = records.indexOf(listener.mRecord);
                    int index2 = index;
                    if (index != -1 && ((Boolean) isRecordPop.get(index2)).booleanValue()) {
                        listener.cancelTransaction();
                    }
                }
                listener.completeTransaction();
            }
            i++;
        }
    }

    private void removeRedundantOperationsAndExecute(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop) {
        if (records != null && !records.isEmpty()) {
            if (isRecordPop == null || records.size() != isRecordPop.size()) {
                throw new IllegalStateException("Internal error with the back stack records");
            }
            executePostponedTransaction(records, isRecordPop);
            int numRecords = records.size();
            int startIndex = 0;
            int recordNum = 0;
            while (recordNum < numRecords) {
                if (!((BackStackRecord) records.get(recordNum)).mReorderingAllowed) {
                    if (startIndex != recordNum) {
                        executeOpsTogether(records, isRecordPop, startIndex, recordNum);
                    }
                    int reorderingEnd = recordNum + 1;
                    if (((Boolean) isRecordPop.get(recordNum)).booleanValue()) {
                        while (reorderingEnd < numRecords && ((Boolean) isRecordPop.get(reorderingEnd)).booleanValue() && !((BackStackRecord) records.get(reorderingEnd)).mReorderingAllowed) {
                            reorderingEnd++;
                        }
                    }
                    executeOpsTogether(records, isRecordPop, recordNum, reorderingEnd);
                    startIndex = reorderingEnd;
                    recordNum = reorderingEnd - 1;
                }
                recordNum++;
            }
            if (startIndex != numRecords) {
                executeOpsTogether(records, isRecordPop, startIndex, numRecords);
            }
        }
    }

    private void executeOpsTogether(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        BackStackRecord record;
        ArrayList<BackStackRecord> arrayList = records;
        ArrayList<Boolean> arrayList2 = isRecordPop;
        int i = startIndex;
        int i2 = endIndex;
        boolean allowReordering = ((BackStackRecord) arrayList.get(i)).mReorderingAllowed;
        if (this.mTmpAddedFragments == null) {
            this.mTmpAddedFragments = new ArrayList();
        } else {
            this.mTmpAddedFragments.clear();
        }
        this.mTmpAddedFragments.addAll(this.mAdded);
        boolean addToBackStack = false;
        Fragment oldPrimaryNav = getPrimaryNavigationFragment();
        int recordNum = i;
        while (true) {
            boolean z = true;
            if (recordNum >= i2) {
                break;
            }
            Fragment oldPrimaryNav2;
            record = (BackStackRecord) arrayList.get(recordNum);
            if (((Boolean) arrayList2.get(recordNum)).booleanValue()) {
                oldPrimaryNav2 = record.trackAddedFragmentsInPop(this.mTmpAddedFragments, oldPrimaryNav);
            } else {
                oldPrimaryNav2 = record.expandOps(this.mTmpAddedFragments, oldPrimaryNav);
            }
            oldPrimaryNav = oldPrimaryNav2;
            if (!(addToBackStack || record.mAddToBackStack)) {
                z = false;
            }
            addToBackStack = z;
            recordNum++;
        }
        this.mTmpAddedFragments.clear();
        if (!allowReordering) {
            FragmentTransition.startTransitions(this, arrayList, arrayList2, i, i2, false);
        }
        executeOps(records, isRecordPop, startIndex, endIndex);
        int postponeIndex = i2;
        if (allowReordering) {
            ArraySet<Fragment> addedFragments = new ArraySet();
            addAddedFragments(addedFragments);
            ArraySet<Fragment> addedFragments2 = addedFragments;
            recordNum = postponePostponableTransactions(arrayList, arrayList2, i, i2, addedFragments);
            makeRemovedFragmentsInvisible(addedFragments2);
            postponeIndex = recordNum;
        }
        if (postponeIndex != i && allowReordering) {
            FragmentTransition.startTransitions(this, arrayList, arrayList2, i, postponeIndex, true);
            moveToState(this.mCurState, true);
        }
        for (recordNum = i; recordNum < i2; recordNum++) {
            record = (BackStackRecord) arrayList.get(recordNum);
            if (((Boolean) arrayList2.get(recordNum)).booleanValue() && record.mIndex >= 0) {
                freeBackStackIndex(record.mIndex);
                record.mIndex = -1;
            }
            record.runOnCommitRunnables();
        }
        if (addToBackStack) {
            reportBackStackChanged();
        }
    }

    private void makeRemovedFragmentsInvisible(ArraySet<Fragment> fragments) {
        int numAdded = fragments.size();
        for (int i = 0; i < numAdded; i++) {
            Fragment fragment = (Fragment) fragments.valueAt(i);
            if (!fragment.mAdded) {
                View view = fragment.getView();
                fragment.mPostponedAlpha = view.getAlpha();
                view.setAlpha(0.0f);
            }
        }
    }

    private int postponePostponableTransactions(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex, ArraySet<Fragment> added) {
        int postponeIndex = endIndex;
        for (int i = endIndex - 1; i >= startIndex; i--) {
            BackStackRecord record = (BackStackRecord) records.get(i);
            boolean isPop = ((Boolean) isRecordPop.get(i)).booleanValue();
            boolean isPostponed = record.isPostponed() && !record.interactsWith(records, i + 1, endIndex);
            if (isPostponed) {
                if (this.mPostponedTransactions == null) {
                    this.mPostponedTransactions = new ArrayList();
                }
                StartEnterTransitionListener listener = new StartEnterTransitionListener(record, isPop);
                this.mPostponedTransactions.add(listener);
                record.setOnStartPostponedListener(listener);
                if (isPop) {
                    record.executeOps();
                } else {
                    record.executePopOps(false);
                }
                postponeIndex--;
                if (i != postponeIndex) {
                    records.remove(i);
                    records.add(postponeIndex, record);
                }
                addAddedFragments(added);
            }
        }
        return postponeIndex;
    }

    private void completeExecute(BackStackRecord record, boolean isPop, boolean runTransitions, boolean moveToState) {
        if (isPop) {
            record.executePopOps(moveToState);
        } else {
            record.executeOps();
        }
        ArrayList<BackStackRecord> records = new ArrayList(1);
        ArrayList<Boolean> isRecordPop = new ArrayList(1);
        records.add(record);
        isRecordPop.add(Boolean.valueOf(isPop));
        if (runTransitions) {
            FragmentTransition.startTransitions(this, records, isRecordPop, 0, 1, true);
        }
        if (moveToState) {
            moveToState(this.mCurState, true);
        }
        if (this.mActive != null) {
            int numActive = this.mActive.size();
            for (int i = 0; i < numActive; i++) {
                Fragment fragment = (Fragment) this.mActive.valueAt(i);
                if (fragment != null && fragment.mView != null && fragment.mIsNewlyAdded && record.interactsWith(fragment.mContainerId)) {
                    if (fragment.mPostponedAlpha > 0.0f) {
                        fragment.mView.setAlpha(fragment.mPostponedAlpha);
                    }
                    if (moveToState) {
                        fragment.mPostponedAlpha = 0.0f;
                    } else {
                        fragment.mPostponedAlpha = -1.0f;
                        fragment.mIsNewlyAdded = false;
                    }
                }
            }
        }
    }

    private Fragment findFragmentUnder(Fragment f) {
        ViewGroup container = f.mContainer;
        View view = f.mView;
        if (container == null || view == null) {
            return null;
        }
        for (int i = this.mAdded.indexOf(f) - 1; i >= 0; i--) {
            Fragment underFragment = (Fragment) this.mAdded.get(i);
            if (underFragment.mContainer == container && underFragment.mView != null) {
                return underFragment;
            }
        }
        return null;
    }

    private static void executeOps(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            BackStackRecord record = (BackStackRecord) records.get(i);
            boolean moveToState = true;
            if (((Boolean) isRecordPop.get(i)).booleanValue()) {
                record.bumpBackStackNesting(-1);
                if (i != endIndex - 1) {
                    moveToState = false;
                }
                record.executePopOps(moveToState);
            } else {
                record.bumpBackStackNesting(1);
                record.executeOps();
            }
        }
    }

    private void addAddedFragments(ArraySet<Fragment> added) {
        if (this.mCurState >= 1) {
            int state = Math.min(this.mCurState, 4);
            int numAdded = this.mAdded.size();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 < numAdded) {
                    Fragment fragment = (Fragment) this.mAdded.get(i2);
                    if (fragment.mState < state) {
                        moveToState(fragment, state, fragment.getNextAnim(), fragment.getNextTransition(), false);
                        if (!(fragment.mView == null || fragment.mHidden || !fragment.mIsNewlyAdded)) {
                            added.add(fragment);
                        }
                    }
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }
    }

    private void forcePostponedTransactions() {
        if (this.mPostponedTransactions != null) {
            while (!this.mPostponedTransactions.isEmpty()) {
                ((StartEnterTransitionListener) this.mPostponedTransactions.remove(0)).completeTransaction();
            }
        }
    }

    private void endAnimatingAwayFragments() {
        int i = 0;
        int numFragments = this.mActive == null ? 0 : this.mActive.size();
        while (i < numFragments) {
            Fragment fragment = (Fragment) this.mActive.valueAt(i);
            if (fragment != null) {
                if (fragment.getAnimatingAway() != null) {
                    int stateAfterAnimating = fragment.getStateAfterAnimating();
                    View animatingAway = fragment.getAnimatingAway();
                    Animation animation = animatingAway.getAnimation();
                    if (animation != null) {
                        animation.cancel();
                        animatingAway.clearAnimation();
                    }
                    fragment.setAnimatingAway(null);
                    moveToState(fragment, stateAfterAnimating, 0, 0, false);
                } else if (fragment.getAnimator() != null) {
                    fragment.getAnimator().end();
                }
            }
            i++;
        }
    }

    /* JADX WARNING: Missing block: B:14:0x003c, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean generateOpsForPendingActions(ArrayList<BackStackRecord> records, ArrayList<Boolean> isPop) {
        boolean didSomething = false;
        synchronized (this) {
            int i = 0;
            if (this.mPendingActions == null || this.mPendingActions.size() == 0) {
            } else {
                while (i < this.mPendingActions.size()) {
                    didSomething |= ((OpGenerator) this.mPendingActions.get(i)).generateOps(records, isPop);
                    i++;
                }
                this.mPendingActions.clear();
                this.mHost.getHandler().removeCallbacks(this.mExecCommit);
                return didSomething;
            }
        }
    }

    void doPendingDeferredStart() {
        if (this.mHavePendingDeferredStart) {
            this.mHavePendingDeferredStart = false;
            startPendingDeferredFragments();
        }
    }

    void reportBackStackChanged() {
        if (this.mBackStackChangeListeners != null) {
            for (int i = 0; i < this.mBackStackChangeListeners.size(); i++) {
                ((OnBackStackChangedListener) this.mBackStackChangeListeners.get(i)).onBackStackChanged();
            }
        }
    }

    void addBackStackState(BackStackRecord state) {
        if (this.mBackStack == null) {
            this.mBackStack = new ArrayList();
        }
        this.mBackStack.add(state);
    }

    boolean popBackStackState(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, String name, int id, int flags) {
        if (this.mBackStack == null) {
            return false;
        }
        int last;
        if (name == null && id < 0 && (flags & 1) == 0) {
            last = this.mBackStack.size() - 1;
            if (last < 0) {
                return false;
            }
            records.add(this.mBackStack.remove(last));
            isRecordPop.add(Boolean.valueOf(true));
        } else {
            last = -1;
            if (name != null || id >= 0) {
                BackStackRecord bss;
                last = this.mBackStack.size() - 1;
                while (last >= 0) {
                    bss = (BackStackRecord) this.mBackStack.get(last);
                    if ((name != null && name.equals(bss.getName())) || (id >= 0 && id == bss.mIndex)) {
                        break;
                    }
                    last--;
                }
                if (last < 0) {
                    return false;
                }
                if ((flags & 1) != 0) {
                    last--;
                    while (last >= 0) {
                        bss = (BackStackRecord) this.mBackStack.get(last);
                        if ((name == null || !name.equals(bss.getName())) && (id < 0 || id != bss.mIndex)) {
                            break;
                        }
                        last--;
                    }
                }
            }
            if (last == this.mBackStack.size() - 1) {
                return false;
            }
            for (int i = this.mBackStack.size() - 1; i > last; i--) {
                records.add(this.mBackStack.remove(i));
                isRecordPop.add(Boolean.valueOf(true));
            }
        }
        return true;
    }

    FragmentManagerNonConfig retainNonConfig() {
        setRetaining(this.mSavedNonConfig);
        return this.mSavedNonConfig;
    }

    private static void setRetaining(FragmentManagerNonConfig nonConfig) {
        if (nonConfig != null) {
            List<Fragment> fragments = nonConfig.getFragments();
            if (fragments != null) {
                for (Fragment fragment : fragments) {
                    fragment.mRetaining = true;
                }
            }
            List<FragmentManagerNonConfig> children = nonConfig.getChildNonConfigs();
            if (children != null) {
                for (FragmentManagerNonConfig child : children) {
                    setRetaining(child);
                }
            }
        }
    }

    void saveNonConfig() {
        ArrayList<Fragment> fragments = null;
        ArrayList<FragmentManagerNonConfig> childFragments = null;
        ArrayList<ViewModelStore> viewModelStores = null;
        if (this.mActive != null) {
            ArrayList<ViewModelStore> viewModelStores2 = null;
            ArrayList<FragmentManagerNonConfig> childFragments2 = null;
            ArrayList<Fragment> fragments2 = null;
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = (Fragment) this.mActive.valueAt(i);
                if (f != null) {
                    FragmentManagerNonConfig child;
                    int j;
                    if (f.mRetainInstance) {
                        if (fragments2 == null) {
                            fragments2 = new ArrayList();
                        }
                        fragments2.add(f);
                        f.mTargetIndex = f.mTarget != null ? f.mTarget.mIndex : -1;
                        if (DEBUG) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("retainNonConfig: keeping retained ");
                            stringBuilder.append(f);
                            Log.v(str, stringBuilder.toString());
                        }
                    }
                    if (f.mChildFragmentManager != null) {
                        f.mChildFragmentManager.saveNonConfig();
                        child = f.mChildFragmentManager.mSavedNonConfig;
                    } else {
                        child = f.mChildNonConfig;
                    }
                    if (childFragments2 == null && child != null) {
                        childFragments2 = new ArrayList(this.mActive.size());
                        for (j = 0; j < i; j++) {
                            childFragments2.add(null);
                        }
                    }
                    if (childFragments2 != null) {
                        childFragments2.add(child);
                    }
                    if (viewModelStores2 == null && f.mViewModelStore != null) {
                        viewModelStores2 = new ArrayList(this.mActive.size());
                        for (j = 0; j < i; j++) {
                            viewModelStores2.add(null);
                        }
                    }
                    if (viewModelStores2 != null) {
                        viewModelStores2.add(f.mViewModelStore);
                    }
                }
            }
            fragments = fragments2;
            childFragments = childFragments2;
            viewModelStores = viewModelStores2;
        }
        if (fragments == null && childFragments == null && viewModelStores == null) {
            this.mSavedNonConfig = null;
        } else {
            this.mSavedNonConfig = new FragmentManagerNonConfig(fragments, childFragments, viewModelStores);
        }
    }

    void saveFragmentViewState(Fragment f) {
        if (f.mInnerView != null) {
            if (this.mStateArray == null) {
                this.mStateArray = new SparseArray();
            } else {
                this.mStateArray.clear();
            }
            f.mInnerView.saveHierarchyState(this.mStateArray);
            if (this.mStateArray.size() > 0) {
                f.mSavedViewState = this.mStateArray;
                this.mStateArray = null;
            }
        }
    }

    Bundle saveFragmentBasicState(Fragment f) {
        Bundle result = null;
        if (this.mStateBundle == null) {
            this.mStateBundle = new Bundle();
        }
        f.performSaveInstanceState(this.mStateBundle);
        dispatchOnFragmentSaveInstanceState(f, this.mStateBundle, false);
        if (!this.mStateBundle.isEmpty()) {
            result = this.mStateBundle;
            this.mStateBundle = null;
        }
        if (f.mView != null) {
            saveFragmentViewState(f);
        }
        if (f.mSavedViewState != null) {
            if (result == null) {
                result = new Bundle();
            }
            result.putSparseParcelableArray(VIEW_STATE_TAG, f.mSavedViewState);
        }
        if (!f.mUserVisibleHint) {
            if (result == null) {
                result = new Bundle();
            }
            result.putBoolean(USER_VISIBLE_HINT_TAG, f.mUserVisibleHint);
        }
        return result;
    }

    Parcelable saveAllState() {
        forcePostponedTransactions();
        endAnimatingAwayFragments();
        execPendingActions();
        this.mStateSaved = true;
        this.mSavedNonConfig = null;
        if (this.mActive == null || this.mActive.size() <= 0) {
            return null;
        }
        StringBuilder stringBuilder;
        int N = this.mActive.size();
        FragmentState[] active = new FragmentState[N];
        int i = 0;
        boolean haveFragments = false;
        for (int i2 = 0; i2 < N; i2++) {
            Fragment f = (Fragment) this.mActive.valueAt(i2);
            if (f != null) {
                StringBuilder stringBuilder2;
                if (f.mIndex < 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failure saving state: active ");
                    stringBuilder.append(f);
                    stringBuilder.append(" has cleared index: ");
                    stringBuilder.append(f.mIndex);
                    throwException(new IllegalStateException(stringBuilder.toString()));
                }
                haveFragments = true;
                FragmentState fs = new FragmentState(f);
                active[i2] = fs;
                if (f.mState <= 0 || fs.mSavedFragmentState != null) {
                    fs.mSavedFragmentState = f.mSavedFragmentState;
                } else {
                    fs.mSavedFragmentState = saveFragmentBasicState(f);
                    if (f.mTarget != null) {
                        if (f.mTarget.mIndex < 0) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Failure saving state: ");
                            stringBuilder2.append(f);
                            stringBuilder2.append(" has target not in fragment manager: ");
                            stringBuilder2.append(f.mTarget);
                            throwException(new IllegalStateException(stringBuilder2.toString()));
                        }
                        if (fs.mSavedFragmentState == null) {
                            fs.mSavedFragmentState = new Bundle();
                        }
                        putFragment(fs.mSavedFragmentState, TARGET_STATE_TAG, f.mTarget);
                        if (f.mTargetRequestCode != 0) {
                            fs.mSavedFragmentState.putInt(TARGET_REQUEST_CODE_STATE_TAG, f.mTargetRequestCode);
                        }
                    }
                }
                if (DEBUG) {
                    String str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Saved state of ");
                    stringBuilder2.append(f);
                    stringBuilder2.append(": ");
                    stringBuilder2.append(fs.mSavedFragmentState);
                    Log.v(str, stringBuilder2.toString());
                }
            }
        }
        if (haveFragments) {
            int[] added = null;
            BackStackState[] backStack = null;
            N = this.mAdded.size();
            if (N > 0) {
                added = new int[N];
                for (int i3 = 0; i3 < N; i3++) {
                    added[i3] = ((Fragment) this.mAdded.get(i3)).mIndex;
                    if (added[i3] < 0) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failure saving state: active ");
                        stringBuilder.append(this.mAdded.get(i3));
                        stringBuilder.append(" has cleared index: ");
                        stringBuilder.append(added[i3]);
                        throwException(new IllegalStateException(stringBuilder.toString()));
                    }
                    if (DEBUG) {
                        String str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("saveAllState: adding fragment #");
                        stringBuilder.append(i3);
                        stringBuilder.append(": ");
                        stringBuilder.append(this.mAdded.get(i3));
                        Log.v(str2, stringBuilder.toString());
                    }
                }
            }
            if (this.mBackStack != null) {
                N = this.mBackStack.size();
                if (N > 0) {
                    backStack = new BackStackState[N];
                    while (i < N) {
                        backStack[i] = new BackStackState((BackStackRecord) this.mBackStack.get(i));
                        if (DEBUG) {
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("saveAllState: adding back stack #");
                            stringBuilder3.append(i);
                            stringBuilder3.append(": ");
                            stringBuilder3.append(this.mBackStack.get(i));
                            Log.v(str3, stringBuilder3.toString());
                        }
                        i++;
                    }
                }
            }
            FragmentManagerState fms = new FragmentManagerState();
            fms.mActive = active;
            fms.mAdded = added;
            fms.mBackStack = backStack;
            if (this.mPrimaryNav != null) {
                fms.mPrimaryNavActiveIndex = this.mPrimaryNav.mIndex;
            }
            fms.mNextFragmentIndex = this.mNextFragmentIndex;
            saveNonConfig();
            return fms;
        }
        if (DEBUG) {
            Log.v(TAG, "saveAllState: no fragments!");
        }
        return null;
    }

    void restoreAllState(Parcelable state, FragmentManagerNonConfig nonConfig) {
        if (state != null) {
            FragmentManagerState fms = (FragmentManagerState) state;
            if (fms.mActive != null) {
                int count;
                int i;
                Fragment f;
                String str;
                StringBuilder stringBuilder;
                Fragment f2;
                List<FragmentManagerNonConfig> childNonConfigs = null;
                List<ViewModelStore> viewModelStores = null;
                if (nonConfig != null) {
                    List<Fragment> nonConfigFragments = nonConfig.getFragments();
                    childNonConfigs = nonConfig.getChildNonConfigs();
                    viewModelStores = nonConfig.getViewModelStores();
                    count = nonConfigFragments != null ? nonConfigFragments.size() : 0;
                    for (i = 0; i < count; i++) {
                        f = (Fragment) nonConfigFragments.get(i);
                        if (DEBUG) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("restoreAllState: re-attaching retained ");
                            stringBuilder.append(f);
                            Log.v(str, stringBuilder.toString());
                        }
                        int index = 0;
                        while (index < fms.mActive.length && fms.mActive[index].mIndex != f.mIndex) {
                            index++;
                        }
                        if (index == fms.mActive.length) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Could not find active fragment with index ");
                            stringBuilder2.append(f.mIndex);
                            throwException(new IllegalStateException(stringBuilder2.toString()));
                        }
                        FragmentState fs = fms.mActive[index];
                        fs.mInstance = f;
                        f.mSavedViewState = null;
                        f.mBackStackNesting = 0;
                        f.mInLayout = false;
                        f.mAdded = false;
                        f.mTarget = null;
                        if (fs.mSavedFragmentState != null) {
                            fs.mSavedFragmentState.setClassLoader(this.mHost.getContext().getClassLoader());
                            f.mSavedViewState = fs.mSavedFragmentState.getSparseParcelableArray(VIEW_STATE_TAG);
                            f.mSavedFragmentState = fs.mSavedFragmentState;
                        }
                    }
                }
                List<ViewModelStore> viewModelStores2 = viewModelStores;
                List<FragmentManagerNonConfig> childNonConfigs2 = childNonConfigs;
                this.mActive = new SparseArray(fms.mActive.length);
                int i2 = 0;
                while (i2 < fms.mActive.length) {
                    FragmentState fs2 = fms.mActive[i2];
                    if (fs2 != null) {
                        FragmentManagerNonConfig childNonConfig = null;
                        if (childNonConfigs2 != null && i2 < childNonConfigs2.size()) {
                            childNonConfig = (FragmentManagerNonConfig) childNonConfigs2.get(i2);
                        }
                        FragmentManagerNonConfig childNonConfig2 = childNonConfig;
                        ViewModelStore viewModelStore = null;
                        if (viewModelStores2 != null && i2 < viewModelStores2.size()) {
                            viewModelStore = (ViewModelStore) viewModelStores2.get(i2);
                        }
                        f2 = fs2.instantiate(this.mHost, this.mContainer, this.mParent, childNonConfig2, viewModelStore);
                        if (DEBUG) {
                            String str2 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("restoreAllState: active #");
                            stringBuilder3.append(i2);
                            stringBuilder3.append(": ");
                            stringBuilder3.append(f2);
                            Log.v(str2, stringBuilder3.toString());
                        }
                        this.mActive.put(f2.mIndex, f2);
                        fs2.mInstance = null;
                    }
                    i2++;
                }
                if (nonConfig != null) {
                    List<Fragment> nonConfigFragments2 = nonConfig.getFragments();
                    count = nonConfigFragments2 != null ? nonConfigFragments2.size() : 0;
                    for (i = 0; i < count; i++) {
                        f = (Fragment) nonConfigFragments2.get(i);
                        if (f.mTargetIndex >= 0) {
                            f.mTarget = (Fragment) this.mActive.get(f.mTargetIndex);
                            if (f.mTarget == null) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Re-attaching retained fragment ");
                                stringBuilder.append(f);
                                stringBuilder.append(" target no longer exists: ");
                                stringBuilder.append(f.mTargetIndex);
                                Log.w(str, stringBuilder.toString());
                            }
                        }
                    }
                }
                this.mAdded.clear();
                if (fms.mAdded != null) {
                    i2 = 0;
                    while (true) {
                        count = i2;
                        if (count >= fms.mAdded.length) {
                            break;
                        }
                        StringBuilder stringBuilder4;
                        f2 = (Fragment) this.mActive.get(fms.mAdded[count]);
                        if (f2 == null) {
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("No instantiated fragment for index #");
                            stringBuilder4.append(fms.mAdded[count]);
                            throwException(new IllegalStateException(stringBuilder4.toString()));
                        }
                        f2.mAdded = true;
                        if (DEBUG) {
                            String str3 = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("restoreAllState: added #");
                            stringBuilder4.append(count);
                            stringBuilder4.append(": ");
                            stringBuilder4.append(f2);
                            Log.v(str3, stringBuilder4.toString());
                        }
                        if (this.mAdded.contains(f2)) {
                            throw new IllegalStateException("Already added!");
                        }
                        synchronized (this.mAdded) {
                            this.mAdded.add(f2);
                        }
                        i2 = count + 1;
                    }
                }
                if (fms.mBackStack != null) {
                    this.mBackStack = new ArrayList(fms.mBackStack.length);
                    for (i2 = 0; i2 < fms.mBackStack.length; i2++) {
                        BackStackRecord bse = fms.mBackStack[i2].instantiate(this);
                        if (DEBUG) {
                            String str4 = TAG;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("restoreAllState: back stack #");
                            stringBuilder5.append(i2);
                            stringBuilder5.append(" (index ");
                            stringBuilder5.append(bse.mIndex);
                            stringBuilder5.append("): ");
                            stringBuilder5.append(bse);
                            Log.v(str4, stringBuilder5.toString());
                            PrintWriter pw = new PrintWriter(new LogWriter(TAG));
                            bse.dump("  ", pw, false);
                            pw.close();
                        }
                        this.mBackStack.add(bse);
                        if (bse.mIndex >= 0) {
                            setBackStackIndex(bse.mIndex, bse);
                        }
                    }
                } else {
                    this.mBackStack = null;
                }
                if (fms.mPrimaryNavActiveIndex >= 0) {
                    this.mPrimaryNav = (Fragment) this.mActive.get(fms.mPrimaryNavActiveIndex);
                }
                this.mNextFragmentIndex = fms.mNextFragmentIndex;
            }
        }
    }

    private void burpActive() {
        if (this.mActive != null) {
            for (int i = this.mActive.size() - 1; i >= 0; i--) {
                if (this.mActive.valueAt(i) == null) {
                    this.mActive.delete(this.mActive.keyAt(i));
                }
            }
        }
    }

    public void attachController(FragmentHostCallback host, FragmentContainer container, Fragment parent) {
        if (this.mHost == null) {
            this.mHost = host;
            this.mContainer = container;
            this.mParent = parent;
            return;
        }
        throw new IllegalStateException("Already attached");
    }

    public void noteStateNotSaved() {
        this.mSavedNonConfig = null;
        int i = 0;
        this.mStateSaved = false;
        this.mStopped = false;
        int addedCount = this.mAdded.size();
        while (i < addedCount) {
            Fragment fragment = (Fragment) this.mAdded.get(i);
            if (fragment != null) {
                fragment.noteStateNotSaved();
            }
            i++;
        }
    }

    public void dispatchCreate() {
        this.mStateSaved = false;
        this.mStopped = false;
        dispatchStateChange(1);
    }

    public void dispatchActivityCreated() {
        this.mStateSaved = false;
        this.mStopped = false;
        dispatchStateChange(2);
    }

    public void dispatchStart() {
        this.mStateSaved = false;
        this.mStopped = false;
        dispatchStateChange(4);
    }

    public void dispatchResume() {
        this.mStateSaved = false;
        this.mStopped = false;
        dispatchStateChange(5);
    }

    public void dispatchPause() {
        dispatchStateChange(4);
    }

    public void dispatchStop() {
        this.mStopped = true;
        dispatchStateChange(3);
    }

    public void dispatchReallyStop() {
        dispatchStateChange(2);
    }

    public void dispatchDestroyView() {
        dispatchStateChange(1);
    }

    public void dispatchDestroy() {
        this.mDestroyed = true;
        execPendingActions();
        dispatchStateChange(0);
        this.mHost = null;
        this.mContainer = null;
        this.mParent = null;
    }

    private void dispatchStateChange(int nextState) {
        try {
            this.mExecutingActions = true;
            moveToState(nextState, false);
            execPendingActions();
        } finally {
            this.mExecutingActions = false;
        }
    }

    public void dispatchMultiWindowModeChanged(boolean isInMultiWindowMode) {
        for (int i = this.mAdded.size() - 1; i >= 0; i--) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performMultiWindowModeChanged(isInMultiWindowMode);
            }
        }
    }

    public void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        for (int i = this.mAdded.size() - 1; i >= 0; i--) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performPictureInPictureModeChanged(isInPictureInPictureMode);
            }
        }
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performConfigurationChanged(newConfig);
            }
        }
    }

    public void dispatchLowMemory() {
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performLowMemory();
            }
        }
    }

    public boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        int i = 0;
        if (this.mCurState < 1) {
            return false;
        }
        int i2;
        ArrayList<Fragment> newMenus = null;
        boolean show = false;
        for (i2 = 0; i2 < this.mAdded.size(); i2++) {
            Fragment f = (Fragment) this.mAdded.get(i2);
            if (f != null && f.performCreateOptionsMenu(menu, inflater)) {
                show = true;
                if (newMenus == null) {
                    newMenus = new ArrayList();
                }
                newMenus.add(f);
            }
        }
        if (this.mCreatedMenus != null) {
            while (true) {
                i2 = i;
                if (i2 >= this.mCreatedMenus.size()) {
                    break;
                }
                Fragment f2 = (Fragment) this.mCreatedMenus.get(i2);
                if (newMenus == null || !newMenus.contains(f2)) {
                    f2.onDestroyOptionsMenu();
                }
                i = i2 + 1;
            }
        }
        this.mCreatedMenus = newMenus;
        return show;
    }

    public boolean dispatchPrepareOptionsMenu(Menu menu) {
        int i = 0;
        if (this.mCurState < 1) {
            return false;
        }
        boolean show = false;
        while (i < this.mAdded.size()) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null && f.performPrepareOptionsMenu(menu)) {
                show = true;
            }
            i++;
        }
        return show;
    }

    public boolean dispatchOptionsItemSelected(MenuItem item) {
        if (this.mCurState < 1) {
            return false;
        }
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null && f.performOptionsItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean dispatchContextItemSelected(MenuItem item) {
        if (this.mCurState < 1) {
            return false;
        }
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null && f.performContextItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    public void dispatchOptionsMenuClosed(Menu menu) {
        if (this.mCurState >= 1) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = (Fragment) this.mAdded.get(i);
                if (f != null) {
                    f.performOptionsMenuClosed(menu);
                }
            }
        }
    }

    public void setPrimaryNavigationFragment(Fragment f) {
        if (f == null || (this.mActive.get(f.mIndex) == f && (f.mHost == null || f.getFragmentManager() == this))) {
            this.mPrimaryNav = f;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Fragment ");
        stringBuilder.append(f);
        stringBuilder.append(" is not an active fragment of FragmentManager ");
        stringBuilder.append(this);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @Nullable
    public Fragment getPrimaryNavigationFragment() {
        return this.mPrimaryNav;
    }

    public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive) {
        this.mLifecycleCallbacks.add(new FragmentLifecycleCallbacksHolder(cb, recursive));
    }

    public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb) {
        synchronized (this.mLifecycleCallbacks) {
            int N = this.mLifecycleCallbacks.size();
            for (int i = 0; i < N; i++) {
                if (((FragmentLifecycleCallbacksHolder) this.mLifecycleCallbacks.get(i)).mCallback == cb) {
                    this.mLifecycleCallbacks.remove(i);
                    break;
                }
            }
        }
    }

    void dispatchOnFragmentPreAttached(@NonNull Fragment f, @NonNull Context context, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentPreAttached(f, context, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentPreAttached(this, f, context);
            }
        }
    }

    void dispatchOnFragmentAttached(@NonNull Fragment f, @NonNull Context context, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentAttached(f, context, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentAttached(this, f, context);
            }
        }
    }

    void dispatchOnFragmentPreCreated(@NonNull Fragment f, @Nullable Bundle savedInstanceState, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentPreCreated(f, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentPreCreated(this, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentCreated(@NonNull Fragment f, @Nullable Bundle savedInstanceState, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentCreated(f, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentCreated(this, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentActivityCreated(@NonNull Fragment f, @Nullable Bundle savedInstanceState, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentActivityCreated(f, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentActivityCreated(this, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentViewCreated(@NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentViewCreated(f, v, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentViewCreated(this, f, v, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentStarted(@NonNull Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentStarted(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentStarted(this, f);
            }
        }
    }

    void dispatchOnFragmentResumed(@NonNull Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentResumed(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentResumed(this, f);
            }
        }
    }

    void dispatchOnFragmentPaused(@NonNull Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentPaused(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentPaused(this, f);
            }
        }
    }

    void dispatchOnFragmentStopped(@NonNull Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentStopped(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentStopped(this, f);
            }
        }
    }

    void dispatchOnFragmentSaveInstanceState(@NonNull Fragment f, @NonNull Bundle outState, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentSaveInstanceState(f, outState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentSaveInstanceState(this, f, outState);
            }
        }
    }

    void dispatchOnFragmentViewDestroyed(@NonNull Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentViewDestroyed(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentViewDestroyed(this, f);
            }
        }
    }

    void dispatchOnFragmentDestroyed(@NonNull Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentDestroyed(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentDestroyed(this, f);
            }
        }
    }

    void dispatchOnFragmentDetached(@NonNull Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentDetached(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = (FragmentLifecycleCallbacksHolder) it.next();
            if (!onlyRecursive || holder.mRecursive) {
                holder.mCallback.onFragmentDetached(this, f);
            }
        }
    }

    public static int reverseTransit(int transit) {
        if (transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {
            return 8194;
        }
        if (transit == FragmentTransaction.TRANSIT_FRAGMENT_FADE) {
            return FragmentTransaction.TRANSIT_FRAGMENT_FADE;
        }
        if (transit != 8194) {
            return 0;
        }
        return FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
    }

    public static int transitToStyleIndex(int transit, boolean enter) {
        if (transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {
            return enter ? 1 : 2;
        } else if (transit == FragmentTransaction.TRANSIT_FRAGMENT_FADE) {
            return enter ? 5 : 6;
        } else if (transit != 8194) {
            return -1;
        } else {
            return enter ? 3 : 4;
        }
    }

    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        Context context2 = context;
        AttributeSet attributeSet = attrs;
        if (!"fragment".equals(name)) {
            return null;
        }
        String fname = attributeSet.getAttributeValue(null, "class");
        TypedArray a = context2.obtainStyledAttributes(attributeSet, FragmentTag.Fragment);
        int i = 0;
        if (fname == null) {
            fname = a.getString(0);
        }
        String fname2 = fname;
        int id = a.getResourceId(1, -1);
        String tag = a.getString(2);
        a.recycle();
        if (!Fragment.isSupportFragmentClass(this.mHost.getContext(), fname2)) {
            return null;
        }
        if (parent != null) {
            i = parent.getId();
        }
        int containerId = i;
        StringBuilder stringBuilder;
        if (containerId == -1 && id == -1 && tag == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(attrs.getPositionDescription());
            stringBuilder.append(": Must specify unique android:id, android:tag, or have a parent with an id for ");
            stringBuilder.append(fname2);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        Fragment fragment;
        Fragment fragment2;
        Fragment fragment3 = id != -1 ? findFragmentById(id) : null;
        if (fragment3 == null && tag != null) {
            fragment3 = findFragmentByTag(tag);
        }
        if (fragment3 == null && containerId != -1) {
            fragment3 = findFragmentById(containerId);
        }
        if (DEBUG) {
            fname = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onCreateView: id=0x");
            stringBuilder2.append(Integer.toHexString(id));
            stringBuilder2.append(" fname=");
            stringBuilder2.append(fname2);
            stringBuilder2.append(" existing=");
            stringBuilder2.append(fragment3);
            Log.v(fname, stringBuilder2.toString());
        }
        if (fragment3 == null) {
            Fragment fragment4 = this.mContainer.instantiate(context2, fname2, null);
            fragment4.mFromLayout = true;
            fragment4.mFragmentId = id != 0 ? id : containerId;
            fragment4.mContainerId = containerId;
            fragment4.mTag = tag;
            fragment4.mInLayout = true;
            fragment4.mFragmentManager = this;
            fragment4.mHost = this.mHost;
            fragment4.onInflate(this.mHost.getContext(), attributeSet, fragment4.mSavedFragmentState);
            addFragment(fragment4, true);
            fragment = fragment4;
        } else if (fragment3.mInLayout) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(attrs.getPositionDescription());
            stringBuilder.append(": Duplicate id 0x");
            stringBuilder.append(Integer.toHexString(id));
            stringBuilder.append(", tag ");
            stringBuilder.append(tag);
            stringBuilder.append(", or parent id 0x");
            stringBuilder.append(Integer.toHexString(containerId));
            stringBuilder.append(" with another fragment for ");
            stringBuilder.append(fname2);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            fragment3.mInLayout = true;
            fragment3.mHost = this.mHost;
            if (!fragment3.mRetaining) {
                fragment3.onInflate(this.mHost.getContext(), attributeSet, fragment3.mSavedFragmentState);
            }
            fragment = fragment3;
        }
        if (this.mCurState >= 1 || !fragment.mFromLayout) {
            fragment2 = fragment;
            moveToState(fragment2);
        } else {
            fragment2 = fragment;
            moveToState(fragment, 1, 0, 0, null);
        }
        if (fragment2.mView != null) {
            if (id != 0) {
                fragment2.mView.setId(id);
            }
            if (fragment2.mView.getTag() == null) {
                fragment2.mView.setTag(tag);
            }
            return fragment2.mView;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Fragment ");
        stringBuilder.append(fname2);
        stringBuilder.append(" did not create a view.");
        throw new IllegalStateException(stringBuilder.toString());
    }

    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return onCreateView(null, name, context, attrs);
    }

    Factory2 getLayoutInflaterFactory() {
        return this;
    }
}
