package android.app;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Fragment.SavedState;
import android.app.FragmentManager.BackStackEntry;
import android.app.FragmentManager.FragmentLifecycleCallbacks;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Trace;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.DebugUtils;
import android.util.Log;
import android.util.LogWriter;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SuperNotCalledException;
import android.view.LayoutInflater.Factory2;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;
import com.android.internal.util.FastPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* compiled from: FragmentManager */
final class FragmentManagerImpl extends FragmentManager implements Factory2 {
    static boolean DEBUG = false;
    static final String TAG = "FragmentManager";
    static final String TARGET_REQUEST_CODE_STATE_TAG = "android:target_req_state";
    static final String TARGET_STATE_TAG = "android:target_state";
    static final String USER_VISIBLE_HINT_TAG = "android:user_visible_hint";
    static final String VIEW_STATE_TAG = "android:view_state";
    SparseArray<Fragment> mActive;
    final ArrayList<Fragment> mAdded = new ArrayList();
    boolean mAllowOldReentrantBehavior;
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
    FragmentHostCallback<?> mHost;
    final CopyOnWriteArrayList<Pair<FragmentLifecycleCallbacks, Boolean>> mLifecycleCallbacks = new CopyOnWriteArrayList();
    boolean mNeedMenuInvalidate;
    int mNextFragmentIndex = 0;
    String mNoTransactionsBecause;
    Fragment mParent;
    ArrayList<OpGenerator> mPendingActions;
    ArrayList<StartEnterTransitionListener> mPostponedTransactions;
    Fragment mPrimaryNav;
    FragmentManagerNonConfig mSavedNonConfig;
    SparseArray<Parcelable> mStateArray = null;
    Bundle mStateBundle = null;
    boolean mStateSaved;
    ArrayList<Fragment> mTmpAddedFragments;
    ArrayList<Boolean> mTmpIsPop;
    ArrayList<BackStackRecord> mTmpRecords;

    /* compiled from: FragmentManager */
    interface OpGenerator {
        boolean generateOps(ArrayList<BackStackRecord> arrayList, ArrayList<Boolean> arrayList2);
    }

    /* compiled from: FragmentManager */
    static class AnimateOnHWLayerIfNeededListener implements AnimatorListener {
        private boolean mShouldRunOnHWLayer = false;
        private View mView;

        public AnimateOnHWLayerIfNeededListener(View v) {
            if (v != null) {
                this.mView = v;
            }
        }

        public void onAnimationStart(Animator animation) {
            this.mShouldRunOnHWLayer = FragmentManagerImpl.shouldRunOnHWLayer(this.mView, animation);
            if (this.mShouldRunOnHWLayer) {
                this.mView.setLayerType(2, null);
            }
        }

        public void onAnimationEnd(Animator animation) {
            if (this.mShouldRunOnHWLayer) {
                this.mView.setLayerType(0, null);
            }
            this.mView = null;
            animation.removeListener(this);
        }

        public void onAnimationCancel(Animator animation) {
        }

        public void onAnimationRepeat(Animator animation) {
        }
    }

    /* compiled from: FragmentManager */
    private class PopBackStackState implements OpGenerator {
        final int mFlags;
        final int mId;
        final String mName;

        public PopBackStackState(String name, int id, int flags) {
            this.mName = name;
            this.mId = id;
            this.mFlags = flags;
        }

        public boolean generateOps(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop) {
            if (FragmentManagerImpl.this.mPrimaryNav != null && this.mId < 0 && this.mName == null) {
                FragmentManager childManager = FragmentManagerImpl.this.mPrimaryNav.mChildFragmentManager;
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

        public StartEnterTransitionListener(BackStackRecord record, boolean isBack) {
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

    private void throwException(RuntimeException ex) {
        Log.e(TAG, ex.getMessage());
        PrintWriter pw = new FastPrintWriter(new LogWriter(6, TAG), false, 1024);
        if (this.mHost != null) {
            Log.e(TAG, "Activity state:");
            try {
                this.mHost.onDump("  ", null, pw, new String[0]);
            } catch (Exception e) {
                pw.flush();
                Log.e(TAG, "Failed dumping state", e);
            }
        } else {
            Log.e(TAG, "Fragment manager state:");
            try {
                dump("  ", null, pw, new String[0]);
            } catch (Exception e2) {
                pw.flush();
                Log.e(TAG, "Failed dumping state", e2);
            }
        }
        pw.flush();
        throw ex;
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

    static boolean shouldRunOnHWLayer(View v, Animator anim) {
        boolean z = false;
        if (v == null || anim == null) {
            return false;
        }
        if (v.getLayerType() == 0 && v.hasOverlappingRendering() && modifiesAlpha(anim)) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:6:0x0014, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setHWLayerAnimListenerIfAlpha(View v, Animator anim) {
        if (!(v == null || anim == null || !shouldRunOnHWLayer(v, anim))) {
            anim.addListener(new AnimateOnHWLayerIfNeededListener(v));
        }
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

    public void popBackStack(String name, int flags) {
        enqueueAction(new PopBackStackState(name, -1, flags), false);
    }

    public boolean popBackStackImmediate(String name, int flags) {
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
            FragmentManager childManager = this.mPrimaryNav.mChildFragmentManager;
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
        List list;
        synchronized (this.mAdded) {
            list = (List) this.mAdded.clone();
        }
        return list;
    }

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

    Animator loadAnimator(Fragment fragment, int transit, boolean enter, int transitionStyle) {
        Animator animObj = fragment.onCreateAnimator(transit, enter, fragment.getNextAnim());
        if (animObj != null) {
            return animObj;
        }
        if (fragment.getNextAnim() != 0) {
            Animator anim = AnimatorInflater.loadAnimator(this.mHost.getContext(), fragment.getNextAnim());
            if (anim != null) {
                return anim;
            }
        }
        if (transit == 0) {
            return null;
        }
        int styleIndex = transitToStyleIndex(transit, enter);
        if (styleIndex < 0) {
            return null;
        }
        if (transitionStyle == 0 && this.mHost.onHasWindowAnimations()) {
            transitionStyle = this.mHost.onGetWindowAnimations();
        }
        if (transitionStyle == 0) {
            return null;
        }
        TypedArray attrs = this.mHost.getContext().obtainStyledAttributes(transitionStyle, R.styleable.FragmentAnimation);
        int anim2 = attrs.getResourceId(styleIndex, 0);
        attrs.recycle();
        if (anim2 == 0) {
            return null;
        }
        return AnimatorInflater.loadAnimator(this.mHost.getContext(), anim2);
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

    boolean isStateAtLeast(int state) {
        return this.mCurState >= state;
    }

    /* JADX WARNING: Removed duplicated region for block: B:77:0x0183  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x014d  */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x02a8  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x02a4  */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x02cf  */
    /* JADX WARNING: Removed duplicated region for block: B:211:0x0477  */
    /* JADX WARNING: Missing block: B:79:0x019f, code skipped:
            r1 = r0;
            ensureInflatedFragmentView(r19);
     */
    /* JADX WARNING: Missing block: B:80:0x01a3, code skipped:
            if (r1 <= 1) goto L_0x02a1;
     */
    /* JADX WARNING: Missing block: B:82:0x01a7, code skipped:
            if (DEBUG == false) goto L_0x01c0;
     */
    /* JADX WARNING: Missing block: B:83:0x01a9, code skipped:
            r0 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("moveto ACTIVITY_CREATED: ");
            r2.append(r8);
            android.util.Log.v(r0, r2.toString());
     */
    /* JADX WARNING: Missing block: B:85:0x01c2, code skipped:
            if (r8.mFromLayout != false) goto L_0x028c;
     */
    /* JADX WARNING: Missing block: B:86:0x01c4, code skipped:
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:87:0x01c7, code skipped:
            if (r8.mContainerId == 0) goto L_0x023b;
     */
    /* JADX WARNING: Missing block: B:89:0x01cc, code skipped:
            if (r8.mContainerId != -1) goto L_0x01ec;
     */
    /* JADX WARNING: Missing block: B:90:0x01ce, code skipped:
            r3 = new java.lang.StringBuilder();
            r3.append("Cannot create fragment ");
            r3.append(r8);
            r3.append(" for a container view with no id");
            throwException(new java.lang.IllegalArgumentException(r3.toString()));
     */
    /* JADX WARNING: Missing block: B:91:0x01ec, code skipped:
            r2 = (android.view.ViewGroup) r7.mContainer.onFindViewById(r8.mContainerId);
     */
    /* JADX WARNING: Missing block: B:92:0x01f6, code skipped:
            if (r2 != null) goto L_0x023a;
     */
    /* JADX WARNING: Missing block: B:94:0x01fa, code skipped:
            if (r8.mRestored != false) goto L_0x023a;
     */
    /* JADX WARNING: Missing block: B:96:?, code skipped:
            r0 = r19.getResources().getResourceName(r8.mContainerId);
     */
    /* JADX WARNING: Missing block: B:98:0x0208, code skipped:
            r0 = "unknown";
     */
    /* JADX WARNING: Missing block: B:146:0x0329, code skipped:
            if (r0 >= 4) goto L_0x034c;
     */
    /* JADX WARNING: Missing block: B:148:0x032d, code skipped:
            if (DEBUG == false) goto L_0x0346;
     */
    /* JADX WARNING: Missing block: B:149:0x032f, code skipped:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("movefrom STARTED: ");
            r2.append(r8);
            android.util.Log.v(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:150:0x0346, code skipped:
            r19.performStop();
            dispatchOnFragmentStopped(r8, false);
     */
    /* JADX WARNING: Missing block: B:151:0x034c, code skipped:
            if (r0 >= 2) goto L_0x040f;
     */
    /* JADX WARNING: Missing block: B:153:0x0350, code skipped:
            if (DEBUG == false) goto L_0x0369;
     */
    /* JADX WARNING: Missing block: B:154:0x0352, code skipped:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("movefrom ACTIVITY_CREATED: ");
            r2.append(r8);
            android.util.Log.v(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:156:0x036b, code skipped:
            if (r8.mView == null) goto L_0x037c;
     */
    /* JADX WARNING: Missing block: B:158:0x0373, code skipped:
            if (r7.mHost.onShouldSaveFragmentState(r8) == false) goto L_0x037c;
     */
    /* JADX WARNING: Missing block: B:160:0x0377, code skipped:
            if (r8.mSavedViewState != null) goto L_0x037c;
     */
    /* JADX WARNING: Missing block: B:161:0x0379, code skipped:
            saveFragmentViewState(r19);
     */
    /* JADX WARNING: Missing block: B:162:0x037c, code skipped:
            r19.performDestroyView();
            dispatchOnFragmentViewDestroyed(r8, false);
     */
    /* JADX WARNING: Missing block: B:163:0x0384, code skipped:
            if (r8.mView == null) goto L_0x0404;
     */
    /* JADX WARNING: Missing block: B:165:0x0388, code skipped:
            if (r8.mContainer == null) goto L_0x0404;
     */
    /* JADX WARNING: Missing block: B:167:0x0390, code skipped:
            if (getTargetSdk() < 26) goto L_0x039e;
     */
    /* JADX WARNING: Missing block: B:168:0x0392, code skipped:
            r8.mView.clearAnimation();
            r8.mContainer.endViewTransition(r8.mView);
     */
    /* JADX WARNING: Missing block: B:169:0x039e, code skipped:
            r1 = null;
     */
    /* JADX WARNING: Missing block: B:170:0x03a1, code skipped:
            if (r7.mCurState <= 0) goto L_0x03c3;
     */
    /* JADX WARNING: Missing block: B:172:0x03a5, code skipped:
            if (r7.mDestroyed != false) goto L_0x03c3;
     */
    /* JADX WARNING: Missing block: B:174:0x03ad, code skipped:
            if (r8.mView.getVisibility() != 0) goto L_0x03c3;
     */
    /* JADX WARNING: Missing block: B:176:0x03b8, code skipped:
            if (r8.mView.getTransitionAlpha() <= 0.0f) goto L_0x03c3;
     */
    /* JADX WARNING: Missing block: B:177:0x03ba, code skipped:
            r1 = loadAnimator(r8, r21, false, r22);
     */
    /* JADX WARNING: Missing block: B:178:0x03c3, code skipped:
            r10 = r21;
            r11 = r22;
     */
    /* JADX WARNING: Missing block: B:179:0x03c7, code skipped:
            r12 = r1;
            r8.mView.setTransitionAlpha(1.0f);
     */
    /* JADX WARNING: Missing block: B:180:0x03cf, code skipped:
            if (r12 == null) goto L_0x03fc;
     */
    /* JADX WARNING: Missing block: B:181:0x03d1, code skipped:
            r15 = r8.mContainer;
            r5 = r8.mView;
            r6 = r8;
            r15.startViewTransition(r5);
            r8.setAnimatingAway(r12);
            r8.setStateAfterAnimating(r0);
            r3 = r15;
            r9 = r1;
            r4 = r5;
            r16 = r5;
            r5 = r8;
            r1 = new android.app.FragmentManagerImpl.AnonymousClass2(r7);
            r12.addListener(r9);
            r12.setTarget(r8.mView);
            setHWLayerAnimListenerIfAlpha(r8.mView, r12);
            r12.start();
     */
    /* JADX WARNING: Missing block: B:182:0x03fc, code skipped:
            r8.mContainer.removeView(r8.mView);
     */
    /* JADX WARNING: Missing block: B:183:0x0404, code skipped:
            r10 = r21;
            r11 = r22;
     */
    /* JADX WARNING: Missing block: B:184:0x0408, code skipped:
            r8.mContainer = null;
            r8.mView = null;
            r8.mInLayout = false;
     */
    /* JADX WARNING: Missing block: B:187:0x0414, code skipped:
            if (r0 >= 1) goto L_0x0473;
     */
    /* JADX WARNING: Missing block: B:189:0x0418, code skipped:
            if (r7.mDestroyed == false) goto L_0x042a;
     */
    /* JADX WARNING: Missing block: B:191:0x041e, code skipped:
            if (r19.getAnimatingAway() == null) goto L_0x042a;
     */
    /* JADX WARNING: Missing block: B:192:0x0420, code skipped:
            r1 = r19.getAnimatingAway();
            r8.setAnimatingAway(null);
            r1.cancel();
     */
    /* JADX WARNING: Missing block: B:194:0x042e, code skipped:
            if (r19.getAnimatingAway() == null) goto L_0x0435;
     */
    /* JADX WARNING: Missing block: B:195:0x0430, code skipped:
            r8.setStateAfterAnimating(r0);
            r0 = 1;
     */
    /* JADX WARNING: Missing block: B:197:0x0437, code skipped:
            if (DEBUG == false) goto L_0x0450;
     */
    /* JADX WARNING: Missing block: B:198:0x0439, code skipped:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("movefrom CREATED: ");
            r2.append(r8);
            android.util.Log.v(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:200:0x0452, code skipped:
            if (r8.mRetaining != false) goto L_0x045b;
     */
    /* JADX WARNING: Missing block: B:201:0x0454, code skipped:
            r19.performDestroy();
            dispatchOnFragmentDestroyed(r8, false);
     */
    /* JADX WARNING: Missing block: B:202:0x045b, code skipped:
            r8.mState = 0;
     */
    /* JADX WARNING: Missing block: B:203:0x045d, code skipped:
            r19.performDetach();
            dispatchOnFragmentDetached(r8, false);
     */
    /* JADX WARNING: Missing block: B:204:0x0463, code skipped:
            if (r23 != false) goto L_0x0473;
     */
    /* JADX WARNING: Missing block: B:206:0x0467, code skipped:
            if (r8.mRetaining != false) goto L_0x046d;
     */
    /* JADX WARNING: Missing block: B:207:0x0469, code skipped:
            makeInactive(r19);
     */
    /* JADX WARNING: Missing block: B:208:0x046d, code skipped:
            r8.mHost = null;
            r8.mParentFragment = null;
            r8.mFragmentManager = null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void moveToState(Fragment f, int newState, int transit, int transitionStyle, boolean keepActive) {
        int newState2;
        int i;
        int i2;
        String str;
        StringBuilder stringBuilder;
        long j;
        int i3;
        Fragment fragment = f;
        boolean z = DEBUG;
        boolean z2 = true;
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
                    case 3:
                        break;
                    case 1:
                        i = transit;
                        i2 = transitionStyle;
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
            if (f.getAnimatingAway() != null) {
                fragment.setAnimatingAway(null);
                moveToState(fragment, f.getStateAfterAnimating(), 0, 0, true);
            }
            switch (fragment.mState) {
                case 0:
                    if (newState2 > 0) {
                        if (DEBUG) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("moveto CREATED: ");
                            stringBuilder.append(fragment);
                            Log.v(str, stringBuilder.toString());
                        }
                        if (fragment.mSavedFragmentState != null) {
                            fragment.mSavedViewState = fragment.mSavedFragmentState.getSparseParcelableArray(VIEW_STATE_TAG);
                            fragment.mTarget = getFragment(fragment.mSavedFragmentState, TARGET_STATE_TAG);
                            if (fragment.mTarget != null) {
                                fragment.mTargetRequestCode = fragment.mSavedFragmentState.getInt(TARGET_REQUEST_CODE_STATE_TAG, 0);
                            }
                            fragment.mUserVisibleHint = fragment.mSavedFragmentState.getBoolean(USER_VISIBLE_HINT_TAG, true);
                            if (!fragment.mUserVisibleHint) {
                                fragment.mDeferStart = true;
                                if (newState2 > 3) {
                                    newState2 = 3;
                                }
                            }
                        }
                        fragment.mHost = this.mHost;
                        fragment.mParentFragment = this.mParent;
                        fragment.mFragmentManager = this.mParent != null ? this.mParent.mChildFragmentManager : this.mHost.getFragmentManagerImpl();
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
                                j = 64;
                                moveToState(fragment.mTarget, 1, 0, 0, true);
                                dispatchOnFragmentPreAttached(fragment, this.mHost.getContext(), false);
                                fragment.mCalled = false;
                                Trace.traceBegin(j, "Fragment.onAttach");
                                fragment.onAttach(this.mHost.getContext());
                                Trace.traceEnd(j);
                                if (!fragment.mCalled) {
                                    if (fragment.mParentFragment == null) {
                                        this.mHost.onAttachFragment(fragment);
                                    } else {
                                        fragment.mParentFragment.onAttachFragment(fragment);
                                    }
                                    dispatchOnFragmentAttached(fragment, this.mHost.getContext(), false);
                                    if (fragment.mIsCreated) {
                                        fragment.restoreChildFragmentState(fragment.mSavedFragmentState, true);
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
                        }
                        j = 64;
                        dispatchOnFragmentPreAttached(fragment, this.mHost.getContext(), false);
                        fragment.mCalled = false;
                        Trace.traceBegin(j, "Fragment.onAttach");
                        fragment.onAttach(this.mHost.getContext());
                        Trace.traceEnd(j);
                        if (!fragment.mCalled) {
                        }
                    }
                case 1:
                    j = 64;
                    break;
                case 2:
                    if (newState2 > 2) {
                        i3 = 3;
                        break;
                    }
                    i3 = 3;
                    fragment.mState = 3;
                    break;
                case 3:
                    i3 = 3;
                    break;
                case 4:
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
                        break;
                    }
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
            stringBuilder2.append(" not updated inline; expected state ");
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
        fragment.mView = fragment.performCreateView(fragment.performGetLayoutInflater(fragment.mSavedFragmentState), container, fragment.mSavedFragmentState);
        if (fragment.mView != null) {
            fragment.mView.setSaveFromParentEnabled(false);
            if (container != null) {
                container.addView(fragment.mView);
            }
            if (fragment.mHidden) {
                fragment.mView.setVisibility(8);
            }
            Trace.traceBegin(j, "Fragment.onViewCreated");
            fragment.onViewCreated(fragment.mView, fragment.mSavedFragmentState);
            Trace.traceEnd(j);
            dispatchOnFragmentViewCreated(fragment, fragment.mView, fragment.mSavedFragmentState, false);
            if (fragment.mView.getVisibility() != 0 || fragment.mContainer == null) {
                z2 = false;
            }
            fragment.mIsNewlyAdded = z2;
        }
        fragment.performActivityCreated(fragment.mSavedFragmentState);
        dispatchOnFragmentActivityCreated(fragment, fragment.mSavedFragmentState, false);
        if (fragment.mView != null) {
            fragment.restoreViewState(fragment.mSavedFragmentState);
        }
        fragment.mSavedFragmentState = null;
        newState2 = i3;
        if (newState2 > 2) {
        }
        if (newState2 > i3) {
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
        }
        i = transit;
        i2 = transitionStyle;
        if (fragment.mState != newState2) {
        }
    }

    void moveToState(Fragment f) {
        moveToState(f, this.mCurState, 0, 0, false);
    }

    void ensureInflatedFragmentView(Fragment f) {
        if (f.mFromLayout && !f.mPerformedCreateView) {
            f.mView = f.performCreateView(f.performGetLayoutInflater(f.mSavedFragmentState), null, f.mSavedFragmentState);
            if (f.mView != null) {
                f.mView.setSaveFromParentEnabled(false);
                if (f.mHidden) {
                    f.mView.setVisibility(8);
                }
                f.onViewCreated(f.mView, f.mSavedFragmentState);
                dispatchOnFragmentViewCreated(f, f.mView, f.mSavedFragmentState, false);
            }
        }
    }

    void completeShowHideFragment(Fragment fragment) {
        if (fragment.mView != null) {
            Animator anim = loadAnimator(fragment, fragment.getNextTransition(), fragment.mHidden ^ 1, fragment.getNextTransitionStyle());
            if (anim != null) {
                anim.setTarget(fragment.mView);
                if (!fragment.mHidden) {
                    fragment.mView.setVisibility(0);
                } else if (fragment.isHideReplaced()) {
                    fragment.setHideReplaced(false);
                } else {
                    final ViewGroup container = fragment.mContainer;
                    final View animatingView = fragment.mView;
                    if (container != null) {
                        container.startViewTransition(animatingView);
                    }
                    anim.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            if (container != null) {
                                container.endViewTransition(animatingView);
                            }
                            animation.removeListener(this);
                            animatingView.setVisibility(8);
                        }
                    });
                }
                setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                anim.start();
            } else {
                int visibility;
                if (!fragment.mHidden || fragment.isHideReplaced()) {
                    visibility = 0;
                } else {
                    visibility = 8;
                }
                fragment.mView.setVisibility(visibility);
                if (fragment.isHideReplaced()) {
                    fragment.setHideReplaced(false);
                }
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
                    f.mView.setTransitionAlpha(1.0f);
                    f.mIsNewlyAdded = false;
                    Animator anim = loadAnimator(f, f.getNextTransition(), true, f.getNextTransitionStyle());
                    if (anim != null) {
                        anim.setTarget(f.mView);
                        setHWLayerAnimListenerIfAlpha(f.mView, anim);
                        anim.start();
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
        } else if (always || this.mCurState != newState) {
            this.mCurState = newState;
            if (this.mActive != null) {
                int i;
                boolean loadersRunning = false;
                for (i = 0; i < this.mAdded.size(); i++) {
                    Fragment f = (Fragment) this.mAdded.get(i);
                    moveFragmentToExpectedState(f);
                    if (f.mLoaderManager != null) {
                        loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                    }
                }
                i = this.mActive.size();
                boolean loadersRunning2 = loadersRunning;
                for (int i2 = 0; i2 < i; i2++) {
                    Fragment f2 = (Fragment) this.mActive.valueAt(i2);
                    if (f2 != null && ((f2.mRemoving || f2.mDetached) && !f2.mIsNewlyAdded)) {
                        moveFragmentToExpectedState(f2);
                        if (f2.mLoaderManager != null) {
                            loadersRunning2 |= f2.mLoaderManager.hasRunningLoaders();
                        }
                    }
                }
                if (!loadersRunning2) {
                    startPendingDeferredFragments();
                }
                if (this.mNeedMenuInvalidate && this.mHost != null && this.mCurState == 5) {
                    this.mHost.onInvalidateOptionsMenu();
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
            this.mHost.inactivateFragment(f.mWho);
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
            fragment.mHiddenChanged = 1 ^ fragment.mHiddenChanged;
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

    public Fragment findFragmentByTag(String tag) {
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
        if (this.mStateSaved) {
            throw new IllegalStateException("Can not perform this action after onSaveInstanceState");
        } else if (this.mNoTransactionsBecause != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can not perform this action inside of ");
            stringBuilder.append(this.mNoTransactionsBecause);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public boolean isStateSaved() {
        return this.mStateSaved;
    }

    public void enqueueAction(OpGenerator action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        synchronized (this) {
            if (!this.mDestroyed) {
                if (this.mHost != null) {
                    if (this.mPendingActions == null) {
                        this.mPendingActions = new ArrayList();
                    }
                    this.mPendingActions.add(action);
                    scheduleCommit();
                    return;
                }
            }
            if (allowStateLoss) {
                return;
            }
            throw new IllegalStateException("Activity has been destroyed");
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
            if (this.mAvailBackStackIndices != null) {
                if (this.mAvailBackStackIndices.size() > 0) {
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
            record = (BackStackRecord) arrayList.get(recordNum);
            if (((Boolean) arrayList2.get(recordNum)).booleanValue()) {
                record.trackAddedFragmentsInPop(this.mTmpAddedFragments);
            } else {
                oldPrimaryNav = record.expandOps(this.mTmpAddedFragments, oldPrimaryNav);
            }
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
                fragment.getView().setTransitionAlpha(0.0f);
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
                    fragment.mIsNewlyAdded = false;
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
            if (!(fragment == null || fragment.getAnimatingAway() == null)) {
                fragment.getAnimatingAway().end();
            }
            i++;
        }
    }

    /* JADX WARNING: Missing block: B:15:0x003c, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean generateOpsForPendingActions(ArrayList<BackStackRecord> records, ArrayList<Boolean> isPop) {
        boolean didSomething = false;
        synchronized (this) {
            int i = 0;
            if (this.mPendingActions != null) {
                if (this.mPendingActions.size() != 0) {
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
    }

    void doPendingDeferredStart() {
        if (this.mHavePendingDeferredStart) {
            boolean loadersRunning = false;
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = (Fragment) this.mActive.valueAt(i);
                if (!(f == null || f.mLoaderManager == null)) {
                    loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                }
            }
            if (!loadersRunning) {
                this.mHavePendingDeferredStart = false;
                startPendingDeferredFragments();
            }
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
            records.add((BackStackRecord) this.mBackStack.remove(last));
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
                records.add((BackStackRecord) this.mBackStack.remove(i));
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
        if (this.mActive != null) {
            ArrayList<FragmentManagerNonConfig> childFragments2 = null;
            ArrayList<Fragment> fragments2 = null;
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = (Fragment) this.mActive.valueAt(i);
                if (f != null) {
                    FragmentManagerNonConfig child;
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
                        for (int j = 0; j < i; j++) {
                            childFragments2.add(null);
                        }
                    }
                    if (childFragments2 != null) {
                        childFragments2.add(child);
                    }
                }
            }
            fragments = fragments2;
            childFragments = childFragments2;
        }
        if (fragments == null && childFragments == null) {
            this.mSavedNonConfig = null;
        } else {
            this.mSavedNonConfig = new FragmentManagerNonConfig(fragments, childFragments);
        }
    }

    void saveFragmentViewState(Fragment f) {
        if (f.mView != null) {
            if (this.mStateArray == null) {
                this.mStateArray = new SparseArray();
            } else {
                this.mStateArray.clear();
            }
            f.mView.saveHierarchyState(this.mStateArray);
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
                        backStack[i] = new BackStackState(this, (BackStackRecord) this.mBackStack.get(i));
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
            fms.mNextFragmentIndex = this.mNextFragmentIndex;
            if (this.mPrimaryNav != null) {
                fms.mPrimaryNavActiveIndex = this.mPrimaryNav.mIndex;
            }
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
                List<Fragment> nonConfigFragments;
                int count;
                int i;
                Fragment f;
                String str;
                StringBuilder stringBuilder;
                List<FragmentManagerNonConfig> childNonConfigs = null;
                if (nonConfig != null) {
                    nonConfigFragments = nonConfig.getFragments();
                    childNonConfigs = nonConfig.getChildNonConfigs();
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
                this.mActive = new SparseArray(fms.mActive.length);
                int i2 = 0;
                while (i2 < fms.mActive.length) {
                    FragmentState fs2 = fms.mActive[i2];
                    if (fs2 != null) {
                        FragmentManagerNonConfig childNonConfig = null;
                        if (childNonConfigs != null && i2 < childNonConfigs.size()) {
                            childNonConfig = (FragmentManagerNonConfig) childNonConfigs.get(i2);
                        }
                        f = fs2.instantiate(this.mHost, this.mContainer, this.mParent, childNonConfig);
                        if (DEBUG) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("restoreAllState: active #");
                            stringBuilder.append(i2);
                            stringBuilder.append(": ");
                            stringBuilder.append(f);
                            Log.v(str, stringBuilder.toString());
                        }
                        this.mActive.put(f.mIndex, f);
                        fs2.mInstance = null;
                    }
                    i2++;
                }
                if (nonConfig != null) {
                    nonConfigFragments = nonConfig.getFragments();
                    count = nonConfigFragments != null ? nonConfigFragments.size() : 0;
                    for (i = 0; i < count; i++) {
                        f = (Fragment) nonConfigFragments.get(i);
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
                                f.mTarget = null;
                            }
                        }
                    }
                }
                this.mAdded.clear();
                if (fms.mAdded != null) {
                    for (i2 = 0; i2 < fms.mAdded.length; i2++) {
                        StringBuilder stringBuilder3;
                        Fragment f2 = (Fragment) this.mActive.get(fms.mAdded[i2]);
                        if (f2 == null) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("No instantiated fragment for index #");
                            stringBuilder3.append(fms.mAdded[i2]);
                            throwException(new IllegalStateException(stringBuilder3.toString()));
                        }
                        f2.mAdded = true;
                        if (DEBUG) {
                            String str2 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("restoreAllState: added #");
                            stringBuilder3.append(i2);
                            stringBuilder3.append(": ");
                            stringBuilder3.append(f2);
                            Log.v(str2, stringBuilder3.toString());
                        }
                        if (this.mAdded.contains(f2)) {
                            throw new IllegalStateException("Already added!");
                        }
                        synchronized (this.mAdded) {
                            this.mAdded.add(f2);
                        }
                    }
                }
                if (fms.mBackStack != null) {
                    this.mBackStack = new ArrayList(fms.mBackStack.length);
                    for (int i3 = 0; i3 < fms.mBackStack.length; i3++) {
                        BackStackRecord bse = fms.mBackStack[i3].instantiate(this);
                        if (DEBUG) {
                            String str3 = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("restoreAllState: back stack #");
                            stringBuilder4.append(i3);
                            stringBuilder4.append(" (index ");
                            stringBuilder4.append(bse.mIndex);
                            stringBuilder4.append("): ");
                            stringBuilder4.append(bse);
                            Log.v(str3, stringBuilder4.toString());
                            PrintWriter pw = new FastPrintWriter(new LogWriter(2, TAG), false, 1024);
                            bse.dump("  ", pw, false);
                            pw.flush();
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

    public void attachController(FragmentHostCallback<?> host, FragmentContainer container, Fragment parent) {
        if (this.mHost == null) {
            this.mHost = host;
            this.mContainer = container;
            this.mParent = parent;
            this.mAllowOldReentrantBehavior = getTargetSdk() <= 25;
            return;
        }
        throw new IllegalStateException("Already attached");
    }

    int getTargetSdk() {
        if (this.mHost != null) {
            Context context = this.mHost.getContext();
            if (context != null) {
                ApplicationInfo info = context.getApplicationInfo();
                if (info != null) {
                    return info.targetSdkVersion;
                }
            }
        }
        return 0;
    }

    public void noteStateNotSaved() {
        this.mSavedNonConfig = null;
        int i = 0;
        this.mStateSaved = false;
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
        dispatchMoveToState(1);
    }

    public void dispatchActivityCreated() {
        this.mStateSaved = false;
        dispatchMoveToState(2);
    }

    public void dispatchStart() {
        this.mStateSaved = false;
        dispatchMoveToState(4);
    }

    public void dispatchResume() {
        this.mStateSaved = false;
        dispatchMoveToState(5);
    }

    public void dispatchPause() {
        dispatchMoveToState(4);
    }

    public void dispatchStop() {
        dispatchMoveToState(3);
    }

    public void dispatchDestroyView() {
        dispatchMoveToState(1);
    }

    public void dispatchDestroy() {
        this.mDestroyed = true;
        execPendingActions();
        dispatchMoveToState(0);
        this.mHost = null;
        this.mContainer = null;
        this.mParent = null;
    }

    private void dispatchMoveToState(int state) {
        if (this.mAllowOldReentrantBehavior) {
            moveToState(state, false);
        } else {
            try {
                this.mExecutingActions = true;
                moveToState(state, false);
            } finally {
                this.mExecutingActions = false;
            }
        }
        execPendingActions();
    }

    @Deprecated
    public void dispatchMultiWindowModeChanged(boolean isInMultiWindowMode) {
        for (int i = this.mAdded.size() - 1; i >= 0; i--) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performMultiWindowModeChanged(isInMultiWindowMode);
            }
        }
    }

    public void dispatchMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        for (int i = this.mAdded.size() - 1; i >= 0; i--) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performMultiWindowModeChanged(isInMultiWindowMode, newConfig);
            }
        }
    }

    @Deprecated
    public void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        for (int i = this.mAdded.size() - 1; i >= 0; i--) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performPictureInPictureModeChanged(isInPictureInPictureMode);
            }
        }
    }

    public void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        for (int i = this.mAdded.size() - 1; i >= 0; i--) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
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

    public void dispatchTrimMemory(int level) {
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performTrimMemory(level);
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

    public Fragment getPrimaryNavigationFragment() {
        return this.mPrimaryNav;
    }

    public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive) {
        this.mLifecycleCallbacks.add(new Pair(cb, Boolean.valueOf(recursive)));
    }

    public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb) {
        synchronized (this.mLifecycleCallbacks) {
            int N = this.mLifecycleCallbacks.size();
            for (int i = 0; i < N; i++) {
                if (((Pair) this.mLifecycleCallbacks.get(i)).first == cb) {
                    this.mLifecycleCallbacks.remove(i);
                    break;
                }
            }
        }
    }

    void dispatchOnFragmentPreAttached(Fragment f, Context context, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentPreAttached(f, context, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentPreAttached(this, f, context);
            }
        }
    }

    void dispatchOnFragmentAttached(Fragment f, Context context, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentAttached(f, context, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentAttached(this, f, context);
            }
        }
    }

    void dispatchOnFragmentPreCreated(Fragment f, Bundle savedInstanceState, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentPreCreated(f, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentPreCreated(this, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentCreated(Fragment f, Bundle savedInstanceState, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentCreated(f, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentCreated(this, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentActivityCreated(Fragment f, Bundle savedInstanceState, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentActivityCreated(f, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentActivityCreated(this, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentViewCreated(Fragment f, View v, Bundle savedInstanceState, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentViewCreated(f, v, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentViewCreated(this, f, v, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentStarted(Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentStarted(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentStarted(this, f);
            }
        }
    }

    void dispatchOnFragmentResumed(Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentResumed(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentResumed(this, f);
            }
        }
    }

    void dispatchOnFragmentPaused(Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentPaused(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentPaused(this, f);
            }
        }
    }

    void dispatchOnFragmentStopped(Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentStopped(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentStopped(this, f);
            }
        }
    }

    void dispatchOnFragmentSaveInstanceState(Fragment f, Bundle outState, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentSaveInstanceState(f, outState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentSaveInstanceState(this, f, outState);
            }
        }
    }

    void dispatchOnFragmentViewDestroyed(Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentViewDestroyed(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentViewDestroyed(this, f);
            }
        }
    }

    void dispatchOnFragmentDestroyed(Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentDestroyed(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentDestroyed(this, f);
            }
        }
    }

    void dispatchOnFragmentDetached(Fragment f, boolean onlyRecursive) {
        if (this.mParent != null) {
            FragmentManager parentManager = this.mParent.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentDetached(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentDetached(this, f);
            }
        }
    }

    public void invalidateOptionsMenu() {
        if (this.mHost == null || this.mCurState != 5) {
            this.mNeedMenuInvalidate = true;
        } else {
            this.mHost.onInvalidateOptionsMenu();
        }
    }

    public static int reverseTransit(int transit) {
        if (transit == 4097) {
            return 8194;
        }
        if (transit == 4099) {
            return 4099;
        }
        if (transit != 8194) {
            return 0;
        }
        return 4097;
    }

    public static int transitToStyleIndex(int transit, boolean enter) {
        int i;
        if (transit == 4097) {
            if (enter) {
                i = 0;
            } else {
                i = 1;
            }
            return i;
        } else if (transit == 4099) {
            if (enter) {
                i = 4;
            } else {
                i = 5;
            }
            return i;
        } else if (transit != 8194) {
            return -1;
        } else {
            if (enter) {
                i = 2;
            } else {
                i = 3;
            }
            return i;
        }
    }

    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        Context context2 = context;
        AttributeSet attributeSet = attrs;
        if (!"fragment".equals(name)) {
            return null;
        }
        String fname = attributeSet.getAttributeValue(null, "class");
        TypedArray a = context2.obtainStyledAttributes(attributeSet, R.styleable.Fragment);
        int i = 0;
        if (fname == null) {
            fname = a.getString(0);
        }
        String fname2 = fname;
        int id = a.getResourceId(1, -1);
        String tag = a.getString(2);
        a.recycle();
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
        return null;
    }

    Factory2 getLayoutInflaterFactory() {
        return this;
    }
}
