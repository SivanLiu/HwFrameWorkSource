package android.view.autofill;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.graphics.Rect;
import android.metrics.LogMaker;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.service.autofill.FillEventHistory;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.autofill.-$Lambda$6ub2tg3C-4hyczXTkY_CEW2ET8I.AnonymousClass1;
import android.view.autofill.-$Lambda$6ub2tg3C-4hyczXTkY_CEW2ET8I.AnonymousClass10;
import android.view.autofill.-$Lambda$6ub2tg3C-4hyczXTkY_CEW2ET8I.AnonymousClass2;
import android.view.autofill.-$Lambda$6ub2tg3C-4hyczXTkY_CEW2ET8I.AnonymousClass3;
import android.view.autofill.-$Lambda$6ub2tg3C-4hyczXTkY_CEW2ET8I.AnonymousClass4;
import android.view.autofill.-$Lambda$6ub2tg3C-4hyczXTkY_CEW2ET8I.AnonymousClass5;
import android.view.autofill.-$Lambda$6ub2tg3C-4hyczXTkY_CEW2ET8I.AnonymousClass6;
import android.view.autofill.-$Lambda$6ub2tg3C-4hyczXTkY_CEW2ET8I.AnonymousClass7;
import android.view.autofill.-$Lambda$6ub2tg3C-4hyczXTkY_CEW2ET8I.AnonymousClass8;
import android.view.autofill.-$Lambda$6ub2tg3C-4hyczXTkY_CEW2ET8I.AnonymousClass9;
import android.view.autofill.IAutoFillManagerClient.Stub;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.Preconditions;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import sun.misc.Cleaner;

public final class AutofillManager {
    public static final int ACTION_START_SESSION = 1;
    public static final int ACTION_VALUE_CHANGED = 4;
    public static final int ACTION_VIEW_ENTERED = 2;
    public static final int ACTION_VIEW_EXITED = 3;
    private static final int AUTHENTICATION_ID_DATASET_ID_MASK = 65535;
    private static final int AUTHENTICATION_ID_DATASET_ID_SHIFT = 16;
    public static final int AUTHENTICATION_ID_DATASET_ID_UNDEFINED = 65535;
    public static final String EXTRA_ASSIST_STRUCTURE = "android.view.autofill.extra.ASSIST_STRUCTURE";
    public static final String EXTRA_AUTHENTICATION_RESULT = "android.view.autofill.extra.AUTHENTICATION_RESULT";
    public static final String EXTRA_CLIENT_STATE = "android.view.autofill.extra.CLIENT_STATE";
    public static final String EXTRA_RESTORE_SESSION_TOKEN = "android.view.autofill.extra.RESTORE_SESSION_TOKEN";
    public static final int FLAG_ADD_CLIENT_DEBUG = 2;
    public static final int FLAG_ADD_CLIENT_ENABLED = 1;
    public static final int FLAG_ADD_CLIENT_VERBOSE = 4;
    private static final String LAST_AUTOFILLED_DATA_TAG = "android:lastAutoFilledData";
    public static final int NO_SESSION = Integer.MIN_VALUE;
    public static final int PENDING_UI_OPERATION_CANCEL = 1;
    public static final int PENDING_UI_OPERATION_RESTORE = 2;
    private static final String SESSION_ID_TAG = "android:sessionId";
    public static final int STATE_ACTIVE = 1;
    public static final int STATE_FINISHED = 2;
    public static final int STATE_SHOWING_SAVE_UI = 3;
    private static final String STATE_TAG = "android:state";
    public static final int STATE_UNKNOWN = 0;
    private static final String TAG = "AutofillManager";
    @GuardedBy("mLock")
    private AutofillCallback mCallback;
    private final Context mContext;
    @GuardedBy("mLock")
    private boolean mEnabled;
    @GuardedBy("mLock")
    private ArraySet<AutofillId> mFillableIds;
    @GuardedBy("mLock")
    private ParcelableMap mLastAutofilledData;
    private final Object mLock = new Object();
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final IAutoFillManager mService;
    @GuardedBy("mLock")
    private IAutoFillManagerClient mServiceClient;
    @GuardedBy("mLock")
    private Cleaner mServiceClientCleaner;
    @GuardedBy("mLock")
    private int mSessionId = Integer.MIN_VALUE;
    @GuardedBy("mLock")
    private int mState = 0;
    @GuardedBy("mLock")
    private TrackedViews mTrackedViews;

    public static abstract class AutofillCallback {
        public static final int EVENT_INPUT_HIDDEN = 2;
        public static final int EVENT_INPUT_SHOWN = 1;
        public static final int EVENT_INPUT_UNAVAILABLE = 3;

        public void onAutofillEvent(View view, int event) {
        }

        public void onAutofillEvent(View view, int virtualId, int event) {
        }
    }

    public interface AutofillClient {
        void autofillCallbackAuthenticate(int i, IntentSender intentSender, Intent intent);

        boolean autofillCallbackRequestHideFillUi();

        boolean autofillCallbackRequestShowFillUi(View view, int i, int i2, Rect rect, IAutofillWindowPresenter iAutofillWindowPresenter);

        void autofillCallbackResetableStateAvailable();

        View findViewByAutofillIdTraversal(int i);

        View[] findViewsByAutofillIdTraversal(int[] iArr);

        ComponentName getComponentNameForAutofill();

        boolean[] getViewVisibility(int[] iArr);

        boolean isVisibleForAutofill();

        void runOnUiThread(Runnable runnable);
    }

    private static final class AutofillManagerClient extends Stub {
        private final WeakReference<AutofillManager> mAfm;

        AutofillManagerClient(AutofillManager autofillManager) {
            this.mAfm = new WeakReference(autofillManager);
        }

        public void setState(boolean enabled, boolean resetSession, boolean resetClient) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new AnonymousClass10(enabled, resetSession, resetClient, afm));
            }
        }

        public void autofill(int sessionId, List<AutofillId> ids, List<AutofillValue> values) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new AnonymousClass4(sessionId, afm, ids, values));
            }
        }

        public void authenticate(int sessionId, int authenticationId, IntentSender intent, Intent fillInIntent) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new AnonymousClass5(sessionId, authenticationId, afm, intent, fillInIntent));
            }
        }

        public void requestShowFillUi(int sessionId, AutofillId id, int width, int height, Rect anchorBounds, IAutofillWindowPresenter presenter) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new AnonymousClass6(sessionId, width, height, afm, id, anchorBounds, presenter));
            }
        }

        public void requestHideFillUi(int sessionId, AutofillId id) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$Lambda$6ub2tg3C-4hyczXTkY_CEW2ET8I(afm, id));
            }
        }

        public void notifyNoFillUi(int sessionId, AutofillId id, boolean sessionFinished) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new AnonymousClass8(sessionFinished, sessionId, afm, id));
            }
        }

        public void startIntentSender(IntentSender intentSender, Intent intent) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new AnonymousClass1(afm, intentSender, intent));
            }
        }

        static /* synthetic */ void lambda$-android_view_autofill_AutofillManager$AutofillManagerClient_71444(AutofillManager afm, IntentSender intentSender, Intent intent) {
            try {
                afm.mContext.startIntentSender(intentSender, intent, 0, 0, 0);
            } catch (SendIntentException e) {
                Log.e(AutofillManager.TAG, "startIntentSender() failed for intent:" + intentSender, e);
            }
        }

        public void setTrackedViews(int sessionId, AutofillId[] ids, boolean saveOnAllViewsInvisible, AutofillId[] fillableIds) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new AnonymousClass9(saveOnAllViewsInvisible, sessionId, afm, ids, fillableIds));
            }
        }

        public void setSaveUiState(int sessionId, boolean shown) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new AnonymousClass7(shown, sessionId, afm));
            }
        }

        public void setSessionFinished(int newState) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new AnonymousClass2(newState, afm));
            }
        }
    }

    private class TrackedViews {
        private ArraySet<AutofillId> mInvisibleTrackedIds;
        private ArraySet<AutofillId> mVisibleTrackedIds;

        private <T> boolean isInSet(ArraySet<T> set, T value) {
            return set != null ? set.contains(value) : false;
        }

        private <T> ArraySet<T> addToSet(ArraySet<T> set, T valueToAdd) {
            if (set == null) {
                set = new ArraySet(1);
            }
            set.add(valueToAdd);
            return set;
        }

        private <T> ArraySet<T> removeFromSet(ArraySet<T> set, T valueToRemove) {
            if (set == null) {
                return null;
            }
            set.remove(valueToRemove);
            if (set.isEmpty()) {
                return null;
            }
            return set;
        }

        TrackedViews(AutofillId[] trackedIds) {
            AutofillClient client = AutofillManager.this.getClientLocked();
            if (!(trackedIds == null || client == null)) {
                boolean[] isVisible;
                if (client.isVisibleForAutofill()) {
                    isVisible = client.getViewVisibility(AutofillManager.this.getViewIds(trackedIds));
                } else {
                    isVisible = new boolean[trackedIds.length];
                }
                int numIds = trackedIds.length;
                for (int i = 0; i < numIds; i++) {
                    AutofillId id = trackedIds[i];
                    if (isVisible[i]) {
                        this.mVisibleTrackedIds = addToSet(this.mVisibleTrackedIds, id);
                    } else {
                        this.mInvisibleTrackedIds = addToSet(this.mInvisibleTrackedIds, id);
                    }
                }
            }
            if (Helper.sVerbose) {
                Log.v(AutofillManager.TAG, "TrackedViews(trackedIds=" + trackedIds + "): " + " mVisibleTrackedIds=" + this.mVisibleTrackedIds + " mInvisibleTrackedIds=" + this.mInvisibleTrackedIds);
            }
            if (this.mVisibleTrackedIds == null) {
                AutofillManager.this.finishSessionLocked();
            }
        }

        void notifyViewVisibilityChanged(AutofillId id, boolean isVisible) {
            AutofillClient client = AutofillManager.this.getClientLocked();
            if (Helper.sDebug) {
                Log.d(AutofillManager.TAG, "notifyViewVisibilityChanged(): id=" + id + " isVisible=" + isVisible);
            }
            if (client != null && client.isVisibleForAutofill()) {
                if (isVisible) {
                    if (isInSet(this.mInvisibleTrackedIds, id)) {
                        this.mInvisibleTrackedIds = removeFromSet(this.mInvisibleTrackedIds, id);
                        this.mVisibleTrackedIds = addToSet(this.mVisibleTrackedIds, id);
                    }
                } else if (isInSet(this.mVisibleTrackedIds, id)) {
                    this.mVisibleTrackedIds = removeFromSet(this.mVisibleTrackedIds, id);
                    this.mInvisibleTrackedIds = addToSet(this.mInvisibleTrackedIds, id);
                }
            }
            if (this.mVisibleTrackedIds == null) {
                if (Helper.sVerbose) {
                    Log.v(AutofillManager.TAG, "No more visible ids. Invisibile = " + this.mInvisibleTrackedIds);
                }
                AutofillManager.this.finishSessionLocked();
            }
        }

        void onVisibleForAutofillLocked() {
            AutofillClient client = AutofillManager.this.getClientLocked();
            ArraySet arraySet = null;
            ArraySet<AutofillId> updatedInvisibleTrackedIds = null;
            if (client != null) {
                boolean[] isVisible;
                int i;
                AutofillId id;
                if (this.mInvisibleTrackedIds != null) {
                    ArrayList<AutofillId> orderedInvisibleIds = new ArrayList(this.mInvisibleTrackedIds);
                    isVisible = client.getViewVisibility(AutofillManager.this.getViewIds((List) orderedInvisibleIds));
                    int numInvisibleTrackedIds = orderedInvisibleIds.size();
                    for (i = 0; i < numInvisibleTrackedIds; i++) {
                        id = (AutofillId) orderedInvisibleIds.get(i);
                        if (isVisible[i]) {
                            arraySet = addToSet(arraySet, id);
                            if (Helper.sDebug) {
                                Log.d(AutofillManager.TAG, "onVisibleForAutofill() " + id + " became visible");
                            }
                        } else {
                            updatedInvisibleTrackedIds = addToSet(updatedInvisibleTrackedIds, id);
                        }
                    }
                }
                if (this.mVisibleTrackedIds != null) {
                    ArrayList<AutofillId> orderedVisibleIds = new ArrayList(this.mVisibleTrackedIds);
                    isVisible = client.getViewVisibility(AutofillManager.this.getViewIds((List) orderedVisibleIds));
                    int numVisibleTrackedIds = orderedVisibleIds.size();
                    for (i = 0; i < numVisibleTrackedIds; i++) {
                        id = (AutofillId) orderedVisibleIds.get(i);
                        if (isVisible[i]) {
                            arraySet = addToSet(arraySet, id);
                        } else {
                            updatedInvisibleTrackedIds = addToSet(updatedInvisibleTrackedIds, id);
                            if (Helper.sDebug) {
                                Log.d(AutofillManager.TAG, "onVisibleForAutofill() " + id + " became invisible");
                            }
                        }
                    }
                }
                this.mInvisibleTrackedIds = updatedInvisibleTrackedIds;
                this.mVisibleTrackedIds = arraySet;
            }
            if (this.mVisibleTrackedIds == null) {
                AutofillManager.this.finishSessionLocked();
            }
        }
    }

    public static int makeAuthenticationId(int requestId, int datasetId) {
        return (requestId << 16) | (65535 & datasetId);
    }

    public static int getRequestIdFromAuthenticationId(int authRequestId) {
        return authRequestId >> 16;
    }

    public static int getDatasetIdFromAuthenticationId(int authRequestId) {
        return 65535 & authRequestId;
    }

    public AutofillManager(Context context, IAutoFillManager service) {
        this.mContext = (Context) Preconditions.checkNotNull(context, "context cannot be null");
        this.mService = service;
    }

    public void onCreate(Bundle savedInstanceState) {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                this.mLastAutofilledData = (ParcelableMap) savedInstanceState.getParcelable(LAST_AUTOFILLED_DATA_TAG);
                if (isActiveLocked()) {
                    Log.w(TAG, "New session was started before onCreate()");
                    return;
                }
                this.mSessionId = savedInstanceState.getInt(SESSION_ID_TAG, Integer.MIN_VALUE);
                this.mState = savedInstanceState.getInt(STATE_TAG, 0);
                if (this.mSessionId != Integer.MIN_VALUE) {
                    ensureServiceClientAddedIfNeededLocked();
                    AutofillClient client = getClientLocked();
                    if (client != null) {
                        try {
                            if (this.mService.restoreSession(this.mSessionId, this.mContext.getActivityToken(), this.mServiceClient.asBinder())) {
                                if (Helper.sDebug) {
                                    Log.d(TAG, "session " + this.mSessionId + " was restored");
                                }
                                client.autofillCallbackResetableStateAvailable();
                            } else {
                                Log.w(TAG, "Session " + this.mSessionId + " could not be restored");
                                this.mSessionId = Integer.MIN_VALUE;
                                this.mState = 0;
                            }
                        } catch (RemoteException e) {
                            Log.e(TAG, "Could not figure out if there was an autofill session", e);
                        }
                    }
                }
            }
        } else {
            return;
        }
    }

    public void onVisibleForAutofill() {
        synchronized (this.mLock) {
            if (this.mEnabled && isActiveLocked() && this.mTrackedViews != null) {
                this.mTrackedViews.onVisibleForAutofillLocked();
            }
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                if (this.mSessionId != Integer.MIN_VALUE) {
                    outState.putInt(SESSION_ID_TAG, this.mSessionId);
                }
                if (this.mState != 0) {
                    outState.putInt(STATE_TAG, this.mState);
                }
                if (this.mLastAutofilledData != null) {
                    outState.putParcelable(LAST_AUTOFILLED_DATA_TAG, this.mLastAutofilledData);
                }
            }
        }
    }

    public boolean isEnabled() {
        if (!hasAutofillFeature()) {
            return false;
        }
        boolean z;
        synchronized (this.mLock) {
            ensureServiceClientAddedIfNeededLocked();
            z = this.mEnabled;
        }
        return z;
    }

    public FillEventHistory getFillEventHistory() {
        try {
            return this.mService.getFillEventHistory();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    public void requestAutofill(View view) {
        notifyViewEntered(view, 1);
    }

    public void requestAutofill(View view, int virtualId, Rect absBounds) {
        notifyViewEntered(view, virtualId, absBounds, 1);
    }

    public void notifyViewEntered(View view) {
        notifyViewEntered(view, 0);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyViewEntered(View view, int flags) {
        if (hasAutofillFeature()) {
            AutofillCallback callback = null;
            synchronized (this.mLock) {
                if (!isFinishedLocked() || (flags & 1) != 0) {
                    ensureServiceClientAddedIfNeededLocked();
                    if (this.mEnabled) {
                        AutofillId id = getAutofillId(view);
                        AutofillValue value = view.getAutofillValue();
                        if (isActiveLocked()) {
                            updateSessionLocked(id, null, value, 2, flags);
                        } else {
                            startSessionLocked(id, null, value, flags);
                        }
                    } else if (this.mCallback != null) {
                        callback = this.mCallback;
                    }
                } else if (Helper.sVerbose) {
                    Log.v(TAG, "notifyViewEntered(flags=" + flags + ", view=" + view + "): ignored on state " + getStateAsStringLocked());
                }
            }
        }
    }

    public void notifyViewExited(View view) {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                ensureServiceClientAddedIfNeededLocked();
                if (this.mEnabled && isActiveLocked()) {
                    updateSessionLocked(getAutofillId(view), null, null, 3, 0);
                }
            }
        }
    }

    public void notifyViewVisibilityChanged(View view, boolean isVisible) {
        notifyViewVisibilityChangedInternal(view, 0, isVisible, false);
    }

    public void notifyViewVisibilityChanged(View view, int virtualId, boolean isVisible) {
        notifyViewVisibilityChangedInternal(view, virtualId, isVisible, true);
    }

    private void notifyViewVisibilityChangedInternal(View view, int virtualId, boolean isVisible, boolean virtual) {
        synchronized (this.mLock) {
            if (this.mEnabled && isActiveLocked()) {
                AutofillId id;
                if (virtual) {
                    id = getAutofillId(view, virtualId);
                } else {
                    id = view.getAutofillId();
                }
                if (!(isVisible || this.mFillableIds == null || !this.mFillableIds.contains(id))) {
                    if (Helper.sDebug) {
                        Log.d(TAG, "Hidding UI when view " + id + " became invisible");
                    }
                    requestHideFillUi(id, view);
                }
                if (this.mTrackedViews != null) {
                    this.mTrackedViews.notifyViewVisibilityChanged(id, isVisible);
                }
            }
        }
    }

    public void notifyViewEntered(View view, int virtualId, Rect absBounds) {
        notifyViewEntered(view, virtualId, absBounds, 0);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyViewEntered(View view, int virtualId, Rect bounds, int flags) {
        if (hasAutofillFeature()) {
            AutofillCallback callback = null;
            synchronized (this.mLock) {
                if (!isFinishedLocked() || (flags & 1) != 0) {
                    ensureServiceClientAddedIfNeededLocked();
                    if (this.mEnabled) {
                        AutofillId id = getAutofillId(view, virtualId);
                        if (isActiveLocked()) {
                            updateSessionLocked(id, bounds, null, 2, flags);
                        } else {
                            startSessionLocked(id, bounds, null, flags);
                        }
                    } else if (this.mCallback != null) {
                        callback = this.mCallback;
                    }
                } else if (Helper.sVerbose) {
                    Log.v(TAG, "notifyViewEntered(flags=" + flags + ", view=" + view + ", virtualId=" + virtualId + "): ignored on state " + getStateAsStringLocked());
                }
            }
        }
    }

    public void notifyViewExited(View view, int virtualId) {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                ensureServiceClientAddedIfNeededLocked();
                if (this.mEnabled && isActiveLocked()) {
                    updateSessionLocked(getAutofillId(view, virtualId), null, null, 3, 0);
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyValueChanged(View view) {
        if (hasAutofillFeature()) {
            AutofillId id = null;
            boolean valueWasRead = false;
            AutofillValue value = null;
            synchronized (this.mLock) {
                if (this.mLastAutofilledData == null) {
                    view.setAutofilled(false);
                } else {
                    id = getAutofillId(view);
                    if (this.mLastAutofilledData.containsKey(id)) {
                        value = view.getAutofillValue();
                        valueWasRead = true;
                        if (Objects.equals(this.mLastAutofilledData.get(id), value)) {
                            view.setAutofilled(true);
                        } else {
                            view.setAutofilled(false);
                            this.mLastAutofilledData.remove(id);
                        }
                    } else {
                        view.setAutofilled(false);
                    }
                }
                if (this.mEnabled && (isActiveLocked() ^ 1) == 0) {
                    if (id == null) {
                        id = getAutofillId(view);
                    }
                    if (!valueWasRead) {
                        value = view.getAutofillValue();
                    }
                    updateSessionLocked(id, null, value, 4, 0);
                } else if (Helper.sVerbose && this.mEnabled) {
                    Log.v(TAG, "notifyValueChanged(" + view + "): ignoring on state " + getStateAsStringLocked());
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyValueChanged(View view, int virtualId, AutofillValue value) {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                if (this.mEnabled && (isActiveLocked() ^ 1) == 0) {
                    updateSessionLocked(getAutofillId(view, virtualId), null, value, 4, 0);
                    return;
                }
            }
        }
    }

    public void commit() {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                if (this.mEnabled || (isActiveLocked() ^ 1) == 0) {
                    finishSessionLocked();
                    return;
                }
            }
        }
    }

    public void cancel() {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                if (this.mEnabled || (isActiveLocked() ^ 1) == 0) {
                    cancelSessionLocked();
                    return;
                }
            }
        }
    }

    public void disableOwnedAutofillServices() {
        disableAutofillServices();
    }

    public void disableAutofillServices() {
        if (hasAutofillFeature()) {
            try {
                this.mService.disableOwnedAutofillServices(this.mContext.getUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean hasEnabledAutofillServices() {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.isServiceEnabled(this.mContext.getUserId(), this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isAutofillSupported() {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.isServiceSupported(this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private AutofillClient getClientLocked() {
        return this.mContext.getAutofillClient();
    }

    private ComponentName getComponentNameFromContext(AutofillClient client) {
        return client == null ? null : client.getComponentNameForAutofill();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onAuthenticationResult(int authenticationId, Intent data) {
        if (hasAutofillFeature()) {
            if (Helper.sDebug) {
                Log.d(TAG, "onAuthenticationResult(): d=" + data);
            }
            synchronized (this.mLock) {
                if (!isActiveLocked() || data == null) {
                } else {
                    try {
                        Thread.sleep(60);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "onAuthenticationResult() sleep Interrupted");
                    }
                    Parcelable result = data.getParcelableExtra(EXTRA_AUTHENTICATION_RESULT);
                    Bundle responseData = new Bundle();
                    responseData.putParcelable(EXTRA_AUTHENTICATION_RESULT, result);
                    try {
                        this.mService.setAuthenticationResult(responseData, this.mSessionId, authenticationId, this.mContext.getUserId());
                    } catch (RemoteException e2) {
                        Log.e(TAG, "Error delivering authentication result", e2);
                    }
                }
            }
        } else {
            return;
        }
    }

    private static AutofillId getAutofillId(View view) {
        return new AutofillId(view.getAutofillViewId());
    }

    private static AutofillId getAutofillId(View parent, int virtualId) {
        return new AutofillId(parent.getAutofillViewId(), virtualId);
    }

    private void startSessionLocked(AutofillId id, Rect bounds, AutofillValue value, int flags) {
        if (Helper.sVerbose) {
            Log.v(TAG, "startSessionLocked(): id=" + id + ", bounds=" + bounds + ", value=" + value + ", flags=" + flags + ", state=" + getStateAsStringLocked());
        }
        if (this.mState == 0 || (flags & 1) != 0) {
            try {
                AutofillClient client = getClientLocked();
                ComponentName componentName = getComponentNameFromContext(client);
                if (componentName == null) {
                    Log.w(TAG, "startSessionLocked(): context is not activity: " + this.mContext);
                    return;
                }
                this.mSessionId = this.mService.startSession(this.mContext.getActivityToken(), this.mServiceClient.asBinder(), id, bounds, value, this.mContext.getUserId(), this.mCallback != null, flags, componentName);
                if (this.mSessionId != Integer.MIN_VALUE) {
                    this.mState = 1;
                }
                if (client != null) {
                    client.autofillCallbackResetableStateAvailable();
                }
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        if (Helper.sVerbose) {
            Log.v(TAG, "not automatically starting session for " + id + " on state " + getStateAsStringLocked());
        }
    }

    private void finishSessionLocked() {
        if (Helper.sVerbose) {
            Log.v(TAG, "finishSessionLocked(): " + getStateAsStringLocked());
        }
        if (isActiveLocked()) {
            try {
                this.mService.finishSession(this.mSessionId, this.mContext.getUserId());
                resetSessionLocked();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    private void cancelSessionLocked() {
        if (Helper.sVerbose) {
            Log.v(TAG, "cancelSessionLocked(): " + getStateAsStringLocked());
        }
        if (isActiveLocked()) {
            try {
                this.mService.cancelSession(this.mSessionId, this.mContext.getUserId());
                resetSessionLocked();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    private void resetSessionLocked() {
        this.mSessionId = Integer.MIN_VALUE;
        this.mState = 0;
        this.mTrackedViews = null;
        this.mFillableIds = null;
    }

    private void updateSessionLocked(AutofillId id, Rect bounds, AutofillValue value, int action, int flags) {
        if (Helper.sVerbose && action != 3) {
            Log.v(TAG, "updateSessionLocked(): id=" + id + ", bounds=" + bounds + ", value=" + value + ", action=" + action + ", flags=" + flags);
        }
        if ((flags & 1) != 0) {
            try {
                AutofillClient client = getClientLocked();
                ComponentName componentName = getComponentNameFromContext(client);
                if (componentName == null) {
                    Log.w(TAG, "startSessionLocked(): context is not activity: " + this.mContext);
                    return;
                }
                int newId = this.mService.updateOrRestartSession(this.mContext.getActivityToken(), this.mServiceClient.asBinder(), id, bounds, value, this.mContext.getUserId(), this.mCallback != null, flags, componentName, this.mSessionId, action);
                if (newId != this.mSessionId) {
                    if (Helper.sDebug) {
                        Log.d(TAG, "Session restarted: " + this.mSessionId + "=>" + newId);
                    }
                    this.mSessionId = newId;
                    this.mState = this.mSessionId == Integer.MIN_VALUE ? 0 : 1;
                    if (client != null) {
                        client.autofillCallbackResetableStateAvailable();
                    }
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        this.mService.updateSession(this.mSessionId, id, bounds, value, action, flags, this.mContext.getUserId());
    }

    private void ensureServiceClientAddedIfNeededLocked() {
        boolean z = true;
        if (getClientLocked() != null && this.mServiceClient == null) {
            this.mServiceClient = new AutofillManagerClient(this);
            try {
                boolean z2;
                int userId = this.mContext.getUserId();
                int flags = this.mService.addClient(this.mServiceClient, userId);
                if ((flags & 1) != 0) {
                    z2 = true;
                } else {
                    z2 = false;
                }
                this.mEnabled = z2;
                if ((flags & 2) != 0) {
                    z2 = true;
                } else {
                    z2 = false;
                }
                Helper.sDebug = z2;
                if ((flags & 4) == 0) {
                    z = false;
                }
                Helper.sVerbose = z;
                this.mServiceClientCleaner = Cleaner.create(this, new AnonymousClass3(userId, this.mService, this.mServiceClient));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    static /* synthetic */ void lambda$-android_view_autofill_AutofillManager_41569(IAutoFillManager service, IAutoFillManagerClient serviceClient, int userId) {
        try {
            service.removeClient(serviceClient, userId);
        } catch (RemoteException e) {
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void registerCallback(AutofillCallback callback) {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                if (callback == null) {
                    return;
                }
                boolean hadCallback = this.mCallback != null;
                this.mCallback = callback;
                if (!hadCallback) {
                    try {
                        this.mService.setHasCallback(this.mSessionId, this.mContext.getUserId(), true);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void unregisterCallback(AutofillCallback callback) {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                if (callback != null) {
                    if (this.mCallback != null) {
                        if (callback == this.mCallback) {
                            this.mCallback = null;
                            try {
                                this.mService.setHasCallback(this.mSessionId, this.mContext.getUserId(), false);
                            } catch (RemoteException e) {
                                throw e.rethrowFromSystemServer();
                            }
                        }
                    }
                }
            }
        }
    }

    private void requestShowFillUi(int sessionId, AutofillId id, int width, int height, Rect anchorBounds, IAutofillWindowPresenter presenter) {
        View anchor = findView(id);
        if (anchor != null) {
            AutofillCallback callback = null;
            synchronized (this.mLock) {
                if (this.mSessionId == sessionId) {
                    AutofillClient client = getClientLocked();
                    if (!(client == null || !client.autofillCallbackRequestShowFillUi(anchor, width, height, anchorBounds, presenter) || this.mCallback == null)) {
                        callback = this.mCallback;
                    }
                }
            }
            if (callback != null) {
                if (id.isVirtual()) {
                    callback.onAutofillEvent(anchor, id.getVirtualChildId(), 1);
                } else {
                    callback.onAutofillEvent(anchor, 1);
                }
            }
        }
    }

    private void authenticate(int sessionId, int authenticationId, IntentSender intent, Intent fillInIntent) {
        synchronized (this.mLock) {
            if (sessionId == this.mSessionId) {
                AutofillClient client = getClientLocked();
                if (client != null) {
                    client.autofillCallbackAuthenticate(authenticationId, intent, fillInIntent);
                }
            }
        }
    }

    private void setState(boolean enabled, boolean resetSession, boolean resetClient) {
        synchronized (this.mLock) {
            this.mEnabled = enabled;
            if (!this.mEnabled || resetSession) {
                resetSessionLocked();
            }
            if (resetClient) {
                this.mServiceClient = null;
                if (this.mServiceClientCleaner != null) {
                    this.mServiceClientCleaner.clean();
                    this.mServiceClientCleaner = null;
                }
            }
        }
    }

    private void setAutofilledIfValuesIs(View view, AutofillValue targetValue) {
        if (Objects.equals(view.getAutofillValue(), targetValue)) {
            synchronized (this.mLock) {
                if (this.mLastAutofilledData == null) {
                    this.mLastAutofilledData = new ParcelableMap(1);
                }
                this.mLastAutofilledData.put(getAutofillId(view), targetValue);
            }
            view.setAutofilled(true);
        }
    }

    private void autofill(int sessionId, List<AutofillId> ids, List<AutofillValue> values) {
        synchronized (this.mLock) {
            if (sessionId != this.mSessionId) {
                return;
            }
            AutofillClient client = getClientLocked();
            if (client == null) {
                return;
            }
            int i;
            int itemCount = ids.size();
            int numApplied = 0;
            ArrayMap virtualValues = null;
            View[] views = client.findViewsByAutofillIdTraversal(getViewIds((List) ids));
            for (i = 0; i < itemCount; i++) {
                AutofillId id = (AutofillId) ids.get(i);
                AutofillValue value = (AutofillValue) values.get(i);
                int viewId = id.getViewId();
                View view = views[i];
                if (view == null) {
                    Log.w(TAG, "autofill(): no View with id " + viewId);
                } else if (id.isVirtual()) {
                    if (virtualValues == null) {
                        virtualValues = new ArrayMap(1);
                    }
                    SparseArray<AutofillValue> valuesByParent = (SparseArray) virtualValues.get(view);
                    if (valuesByParent == null) {
                        valuesByParent = new SparseArray(5);
                        virtualValues.put(view, valuesByParent);
                    }
                    valuesByParent.put(id.getVirtualChildId(), value);
                } else {
                    if (this.mLastAutofilledData == null) {
                        this.mLastAutofilledData = new ParcelableMap(itemCount - i);
                    }
                    this.mLastAutofilledData.put(id, value);
                    view.autofill(value);
                    setAutofilledIfValuesIs(view, value);
                    numApplied++;
                }
            }
            if (virtualValues != null) {
                for (i = 0; i < virtualValues.size(); i++) {
                    SparseArray childrenValues = (SparseArray) virtualValues.valueAt(i);
                    ((View) virtualValues.keyAt(i)).autofill(childrenValues);
                    numApplied += childrenValues.size();
                }
            }
            this.mMetricsLogger.write(new LogMaker(MetricsEvent.AUTOFILL_DATASET_APPLIED).setPackageName(this.mContext.getPackageName()).addTaggedData(MetricsEvent.FIELD_AUTOFILL_NUM_VALUES, Integer.valueOf(itemCount)).addTaggedData(MetricsEvent.FIELD_AUTOFILL_NUM_VIEWS_FILLED, Integer.valueOf(numApplied)));
        }
    }

    private void setTrackedViews(int sessionId, AutofillId[] trackedIds, boolean saveOnAllViewsInvisible, AutofillId[] fillableIds) {
        synchronized (this.mLock) {
            if (this.mEnabled && this.mSessionId == sessionId) {
                if (saveOnAllViewsInvisible) {
                    this.mTrackedViews = new TrackedViews(trackedIds);
                } else {
                    this.mTrackedViews = null;
                }
                if (fillableIds != null) {
                    if (this.mFillableIds == null) {
                        this.mFillableIds = new ArraySet(fillableIds.length);
                    }
                    for (AutofillId id : fillableIds) {
                        this.mFillableIds.add(id);
                    }
                    if (Helper.sVerbose) {
                        Log.v(TAG, "setTrackedViews(): fillableIds=" + fillableIds + ", mFillableIds" + this.mFillableIds);
                    }
                }
            }
        }
    }

    private void setSaveUiState(int sessionId, boolean shown) {
        if (Helper.sDebug) {
            Log.d(TAG, "setSaveUiState(" + sessionId + "): " + shown);
        }
        synchronized (this.mLock) {
            if (this.mSessionId != Integer.MIN_VALUE) {
                Log.w(TAG, "setSaveUiState(" + sessionId + ", " + shown + ") called on existing session " + this.mSessionId + "; cancelling it");
                cancelSessionLocked();
            }
            if (shown) {
                this.mSessionId = sessionId;
                this.mState = 3;
            } else {
                this.mSessionId = Integer.MIN_VALUE;
                this.mState = 0;
            }
        }
    }

    private void setSessionFinished(int newState) {
        synchronized (this.mLock) {
            if (Helper.sVerbose) {
                Log.v(TAG, "setSessionFinished(): from " + this.mState + " to " + newState);
            }
            resetSessionLocked();
            this.mState = newState;
        }
    }

    private void requestHideFillUi(AutofillId id) {
        View anchor = findView(id);
        if (Helper.sVerbose) {
            Log.v(TAG, "requestHideFillUi(" + id + "): anchor = " + anchor);
        }
        if (anchor != null) {
            requestHideFillUi(id, anchor);
        }
    }

    private void requestHideFillUi(AutofillId id, View anchor) {
        AutofillCallback callback = null;
        synchronized (this.mLock) {
            AutofillClient client = getClientLocked();
            if (!(client == null || !client.autofillCallbackRequestHideFillUi() || this.mCallback == null)) {
                callback = this.mCallback;
            }
        }
        if (callback == null) {
            return;
        }
        if (id.isVirtual()) {
            callback.onAutofillEvent(anchor, id.getVirtualChildId(), 2);
        } else {
            callback.onAutofillEvent(anchor, 2);
        }
    }

    private void notifyNoFillUi(int sessionId, AutofillId id, boolean sessionFinished) {
        if (Helper.sVerbose) {
            Log.v(TAG, "notifyNoFillUi(): sessionId=" + sessionId + ", autofillId=" + id + ", finished=" + sessionFinished);
        }
        View anchor = findView(id);
        if (anchor != null) {
            AutofillCallback callback = null;
            synchronized (this.mLock) {
                if (this.mSessionId == sessionId && getClientLocked() != null) {
                    callback = this.mCallback;
                }
            }
            if (callback != null) {
                if (id.isVirtual()) {
                    callback.onAutofillEvent(anchor, id.getVirtualChildId(), 3);
                } else {
                    callback.onAutofillEvent(anchor, 3);
                }
            }
            if (sessionFinished) {
                setSessionFinished(2);
            }
        }
    }

    private int[] getViewIds(AutofillId[] autofillIds) {
        int numIds = autofillIds.length;
        int[] viewIds = new int[numIds];
        for (int i = 0; i < numIds; i++) {
            viewIds[i] = autofillIds[i].getViewId();
        }
        return viewIds;
    }

    private int[] getViewIds(List<AutofillId> autofillIds) {
        int numIds = autofillIds.size();
        int[] viewIds = new int[numIds];
        for (int i = 0; i < numIds; i++) {
            viewIds[i] = ((AutofillId) autofillIds.get(i)).getViewId();
        }
        return viewIds;
    }

    private View findView(AutofillId autofillId) {
        AutofillClient client = getClientLocked();
        if (client == null) {
            return null;
        }
        return client.findViewByAutofillIdTraversal(autofillId.getViewId());
    }

    public boolean hasAutofillFeature() {
        return this.mService != null;
    }

    public void onPendingSaveUi(int operation, IBinder token) {
        if (Helper.sVerbose) {
            Log.v(TAG, "onPendingSaveUi(" + operation + "): " + token);
        }
        synchronized (this.mLock) {
            try {
                this.mService.onPendingSaveUi(operation, token);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    public void dump(String outerPrefix, PrintWriter pw) {
        boolean z = true;
        pw.print(outerPrefix);
        pw.println("AutofillManager:");
        String pfx = outerPrefix + "  ";
        pw.print(pfx);
        pw.print("sessionId: ");
        pw.println(this.mSessionId);
        pw.print(pfx);
        pw.print("state: ");
        pw.println(getStateAsStringLocked());
        pw.print(pfx);
        pw.print("enabled: ");
        pw.println(this.mEnabled);
        pw.print(pfx);
        pw.print("hasService: ");
        pw.println(this.mService != null);
        pw.print(pfx);
        pw.print("hasCallback: ");
        if (this.mCallback == null) {
            z = false;
        }
        pw.println(z);
        pw.print(pfx);
        pw.print("last autofilled data: ");
        pw.println(this.mLastAutofilledData);
        pw.print(pfx);
        pw.print("tracked views: ");
        if (this.mTrackedViews == null) {
            pw.println("null");
        } else {
            String pfx2 = pfx + "  ";
            pw.println();
            pw.print(pfx2);
            pw.print("visible:");
            pw.println(this.mTrackedViews.mVisibleTrackedIds);
            pw.print(pfx2);
            pw.print("invisible:");
            pw.println(this.mTrackedViews.mInvisibleTrackedIds);
        }
        pw.print(pfx);
        pw.print("fillable ids: ");
        pw.println(this.mFillableIds);
    }

    private String getStateAsStringLocked() {
        switch (this.mState) {
            case 0:
                return "STATE_UNKNOWN";
            case 1:
                return "STATE_ACTIVE";
            case 2:
                return "STATE_FINISHED";
            case 3:
                return "STATE_SHOWING_SAVE_UI";
            default:
                return "INVALID:" + this.mState;
        }
    }

    private boolean isActiveLocked() {
        return this.mState == 1;
    }

    private boolean isFinishedLocked() {
        return this.mState == 2;
    }

    private void post(Runnable runnable) {
        AutofillClient client = getClientLocked();
        if (client == null) {
            if (Helper.sVerbose) {
                Log.v(TAG, "ignoring post() because client is null");
            }
            return;
        }
        client.runOnUiThread(runnable);
    }
}
