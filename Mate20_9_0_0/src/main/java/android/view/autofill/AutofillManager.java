package android.view.autofill;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.common.HwFrameworkFactory;
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
import android.provider.SettingsStringUtil;
import android.service.autofill.FillEventHistory;
import android.service.autofill.UserData;
import android.service.notification.ZenModeConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.AccessibilityPolicy;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.autofill.IAutoFillManagerClient.Stub;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParserException;
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
    public static final int FC_SERVICE_TIMEOUT = 5000;
    public static final int FLAG_ADD_CLIENT_DEBUG = 2;
    public static final int FLAG_ADD_CLIENT_ENABLED = 1;
    public static final int FLAG_ADD_CLIENT_VERBOSE = 4;
    private static final String LAST_AUTOFILLED_DATA_TAG = "android:lastAutoFilledData";
    public static final int NO_SESSION = Integer.MIN_VALUE;
    public static final int PENDING_UI_OPERATION_CANCEL = 1;
    public static final int PENDING_UI_OPERATION_RESTORE = 2;
    private static final String SESSION_ID_TAG = "android:sessionId";
    public static final int SET_STATE_FLAG_DEBUG = 8;
    public static final int SET_STATE_FLAG_ENABLED = 1;
    public static final int SET_STATE_FLAG_RESET_CLIENT = 4;
    public static final int SET_STATE_FLAG_RESET_SESSION = 2;
    public static final int SET_STATE_FLAG_VERBOSE = 16;
    public static final int STATE_ACTIVE = 1;
    public static final int STATE_DISABLED_BY_SERVICE = 4;
    public static final int STATE_FINISHED = 2;
    public static final int STATE_SHOWING_SAVE_UI = 3;
    private static final String STATE_TAG = "android:state";
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_UNKNOWN_COMPAT_MODE = 5;
    private static final String TAG = "AutofillManager";
    @GuardedBy("mLock")
    private AutofillCallback mCallback;
    @GuardedBy("mLock")
    private CompatibilityBridge mCompatibilityBridge;
    private final Context mContext;
    @GuardedBy("mLock")
    private boolean mEnabled;
    @GuardedBy("mLock")
    private ArraySet<AutofillId> mEnteredIds;
    @GuardedBy("mLock")
    private ArraySet<AutofillId> mFillableIds;
    private IHwAutofillHelper mHwAutofillHelper = HwFrameworkFactory.getHwAutofillHelper();
    private AutofillId mIdShownFillUi;
    @GuardedBy("mLock")
    private ParcelableMap mLastAutofilledData;
    private final Object mLock = new Object();
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    @GuardedBy("mLock")
    private boolean mOnInvisibleCalled;
    @GuardedBy("mLock")
    private boolean mSaveOnFinish;
    @GuardedBy("mLock")
    private AutofillId mSaveTriggerId;
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

        @Retention(RetentionPolicy.SOURCE)
        public @interface AutofillEventType {
        }

        public void onAutofillEvent(View view, int event) {
        }

        public void onAutofillEvent(View view, int virtualId, int event) {
        }
    }

    public interface AutofillClient {
        void autofillClientAuthenticate(int i, IntentSender intentSender, Intent intent);

        void autofillClientDispatchUnhandledKey(View view, KeyEvent keyEvent);

        View autofillClientFindViewByAccessibilityIdTraversal(int i, int i2);

        View autofillClientFindViewByAutofillIdTraversal(AutofillId autofillId);

        View[] autofillClientFindViewsByAutofillIdTraversal(AutofillId[] autofillIdArr);

        IBinder autofillClientGetActivityToken();

        ComponentName autofillClientGetComponentName();

        AutofillId autofillClientGetNextAutofillId();

        boolean[] autofillClientGetViewVisibility(AutofillId[] autofillIdArr);

        boolean autofillClientIsCompatibilityModeEnabled();

        boolean autofillClientIsFillUiShowing();

        boolean autofillClientIsVisibleForAutofill();

        boolean autofillClientRequestHideFillUi();

        boolean autofillClientRequestShowFillUi(View view, int i, int i2, Rect rect, IAutofillWindowPresenter iAutofillWindowPresenter);

        void autofillClientResetableStateAvailable();

        void autofillClientRunOnUiThread(Runnable runnable);

        boolean isDisablingEnterExitEventForAutofill();
    }

    private class TrackedViews {
        private ArraySet<AutofillId> mInvisibleTrackedIds;
        private ArraySet<AutofillId> mVisibleTrackedIds;

        private <T> boolean isInSet(ArraySet<T> set, T value) {
            return set != null && set.contains(value);
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
            AutofillClient client = AutofillManager.this.getClient();
            if (!(ArrayUtils.isEmpty(trackedIds) || client == null)) {
                boolean[] isVisible;
                if (client.autofillClientIsVisibleForAutofill()) {
                    if (Helper.sVerbose) {
                        Log.v(AutofillManager.TAG, "client is visible, check tracked ids");
                    }
                    isVisible = client.autofillClientGetViewVisibility(trackedIds);
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
                String str = AutofillManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("TrackedViews(trackedIds=");
                stringBuilder.append(Arrays.toString(trackedIds));
                stringBuilder.append("):  mVisibleTrackedIds=");
                stringBuilder.append(this.mVisibleTrackedIds);
                stringBuilder.append(" mInvisibleTrackedIds=");
                stringBuilder.append(this.mInvisibleTrackedIds);
                Log.v(str, stringBuilder.toString());
            }
            if (this.mVisibleTrackedIds == null) {
                AutofillManager.this.finishSessionLocked();
            }
        }

        @GuardedBy("mLock")
        void notifyViewVisibilityChangedLocked(AutofillId id, boolean isVisible) {
            String str;
            StringBuilder stringBuilder;
            if (Helper.sDebug) {
                str = AutofillManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("notifyViewVisibilityChangedLocked(): id=");
                stringBuilder.append(id);
                stringBuilder.append(" isVisible=");
                stringBuilder.append(isVisible);
                Log.d(str, stringBuilder.toString());
            }
            if (AutofillManager.this.isClientVisibleForAutofillLocked()) {
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
                    str = AutofillManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("No more visible ids. Invisibile = ");
                    stringBuilder.append(this.mInvisibleTrackedIds);
                    Log.v(str, stringBuilder.toString());
                }
                AutofillManager.this.finishSessionLocked();
            }
        }

        @GuardedBy("mLock")
        void onVisibleForAutofillChangedLocked() {
            AutofillClient client = AutofillManager.this.getClient();
            ArraySet<AutofillId> updatedVisibleTrackedIds = null;
            ArraySet<AutofillId> updatedInvisibleTrackedIds = null;
            if (client != null) {
                ArrayList<AutofillId> orderedInvisibleIds;
                boolean[] isVisible;
                int numInvisibleTrackedIds;
                if (Helper.sVerbose) {
                    String str = AutofillManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onVisibleForAutofillChangedLocked(): inv= ");
                    stringBuilder.append(this.mInvisibleTrackedIds);
                    stringBuilder.append(" vis=");
                    stringBuilder.append(this.mVisibleTrackedIds);
                    Log.v(str, stringBuilder.toString());
                }
                int i = 0;
                if (this.mInvisibleTrackedIds != null) {
                    orderedInvisibleIds = new ArrayList(this.mInvisibleTrackedIds);
                    isVisible = client.autofillClientGetViewVisibility(Helper.toArray(orderedInvisibleIds));
                    numInvisibleTrackedIds = orderedInvisibleIds.size();
                    ArraySet<AutofillId> updatedInvisibleTrackedIds2 = null;
                    updatedInvisibleTrackedIds = null;
                    for (int i2 = 0; i2 < numInvisibleTrackedIds; i2++) {
                        AutofillId id = (AutofillId) orderedInvisibleIds.get(i2);
                        if (isVisible[i2]) {
                            updatedInvisibleTrackedIds = addToSet(updatedInvisibleTrackedIds, id);
                            if (Helper.sDebug) {
                                String str2 = AutofillManager.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("onVisibleForAutofill() ");
                                stringBuilder2.append(id);
                                stringBuilder2.append(" became visible");
                                Log.d(str2, stringBuilder2.toString());
                            }
                        } else {
                            updatedInvisibleTrackedIds2 = addToSet(updatedInvisibleTrackedIds2, id);
                        }
                    }
                    updatedVisibleTrackedIds = updatedInvisibleTrackedIds;
                    updatedInvisibleTrackedIds = updatedInvisibleTrackedIds2;
                }
                if (this.mVisibleTrackedIds != null) {
                    orderedInvisibleIds = new ArrayList(this.mVisibleTrackedIds);
                    isVisible = client.autofillClientGetViewVisibility(Helper.toArray(orderedInvisibleIds));
                    numInvisibleTrackedIds = orderedInvisibleIds.size();
                    while (i < numInvisibleTrackedIds) {
                        AutofillId id2 = (AutofillId) orderedInvisibleIds.get(i);
                        if (isVisible[i]) {
                            updatedVisibleTrackedIds = addToSet(updatedVisibleTrackedIds, id2);
                        } else {
                            updatedInvisibleTrackedIds = addToSet(updatedInvisibleTrackedIds, id2);
                            if (Helper.sDebug) {
                                String str3 = AutofillManager.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("onVisibleForAutofill() ");
                                stringBuilder3.append(id2);
                                stringBuilder3.append(" became invisible");
                                Log.d(str3, stringBuilder3.toString());
                            }
                        }
                        i++;
                    }
                }
                this.mInvisibleTrackedIds = updatedInvisibleTrackedIds;
                this.mVisibleTrackedIds = updatedVisibleTrackedIds;
            }
            if (this.mVisibleTrackedIds == null) {
                if (Helper.sVerbose) {
                    Log.v(AutofillManager.TAG, "onVisibleForAutofillChangedLocked(): no more visible ids");
                }
                AutofillManager.this.finishSessionLocked();
            }
        }
    }

    private final class CompatibilityBridge implements AccessibilityPolicy {
        @GuardedBy("mLock")
        AccessibilityServiceInfo mCompatServiceInfo;
        @GuardedBy("mLock")
        private final Rect mFocusedBounds = new Rect();
        @GuardedBy("mLock")
        private long mFocusedNodeId = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
        @GuardedBy("mLock")
        private int mFocusedWindowId = -1;
        @GuardedBy("mLock")
        private final Rect mTempBounds = new Rect();

        CompatibilityBridge() {
            AccessibilityManager.getInstance(AutofillManager.this.mContext).setAccessibilityPolicy(this);
        }

        private AccessibilityServiceInfo getCompatServiceInfo() {
            synchronized (AutofillManager.this.mLock) {
                if (this.mCompatServiceInfo != null) {
                    AccessibilityServiceInfo accessibilityServiceInfo = this.mCompatServiceInfo;
                    return accessibilityServiceInfo;
                }
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(ZenModeConfig.SYSTEM_AUTHORITY, "com.android.server.autofill.AutofillCompatAccessibilityService"));
                try {
                    this.mCompatServiceInfo = new AccessibilityServiceInfo(AutofillManager.this.mContext.getPackageManager().resolveService(intent, 1048704), AutofillManager.this.mContext);
                    AccessibilityServiceInfo accessibilityServiceInfo2 = this.mCompatServiceInfo;
                    return accessibilityServiceInfo2;
                } catch (IOException | XmlPullParserException e) {
                    String str = AutofillManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot find compat autofill service:");
                    stringBuilder.append(intent);
                    Log.e(str, stringBuilder.toString());
                    throw new IllegalStateException("Cannot find compat autofill service");
                }
            }
        }

        public boolean isEnabled(boolean accessibilityEnabled) {
            return true;
        }

        public int getRelevantEventTypes(int relevantEventTypes) {
            return (((relevantEventTypes | 8) | 16) | 1) | 2048;
        }

        public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(List<AccessibilityServiceInfo> installedServices) {
            if (installedServices == null) {
                installedServices = new ArrayList();
            }
            installedServices.add(getCompatServiceInfo());
            return installedServices;
        }

        public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int feedbackTypeFlags, List<AccessibilityServiceInfo> enabledService) {
            if (enabledService == null) {
                enabledService = new ArrayList();
            }
            enabledService.add(getCompatServiceInfo());
            return enabledService;
        }

        public AccessibilityEvent onAccessibilityEvent(AccessibilityEvent event, boolean accessibilityEnabled, int relevantEventTypes) {
            int eventType = event.getEventType();
            if (eventType == 1) {
                synchronized (AutofillManager.this.mLock) {
                    notifyViewClicked(event.getWindowId(), event.getSourceNodeId());
                }
            } else if (eventType == 8) {
                synchronized (AutofillManager.this.mLock) {
                    if (this.mFocusedWindowId == event.getWindowId() && this.mFocusedNodeId == event.getSourceNodeId()) {
                        return event;
                    }
                    if (!(this.mFocusedWindowId == -1 || this.mFocusedNodeId == AccessibilityNodeInfo.UNDEFINED_NODE_ID)) {
                        notifyViewExited(this.mFocusedWindowId, this.mFocusedNodeId);
                        this.mFocusedWindowId = -1;
                        this.mFocusedNodeId = AccessibilityNodeInfo.UNDEFINED_NODE_ID;
                        this.mFocusedBounds.set(0, 0, 0, 0);
                    }
                    int windowId = event.getWindowId();
                    long nodeId = event.getSourceNodeId();
                    if (notifyViewEntered(windowId, nodeId, this.mFocusedBounds)) {
                        this.mFocusedWindowId = windowId;
                        this.mFocusedNodeId = nodeId;
                    }
                }
            } else if (eventType == 16) {
                synchronized (AutofillManager.this.mLock) {
                    if (this.mFocusedWindowId == event.getWindowId() && this.mFocusedNodeId == event.getSourceNodeId()) {
                        notifyValueChanged(event.getWindowId(), event.getSourceNodeId());
                    }
                }
            } else if (eventType == 2048) {
                AutofillClient client = AutofillManager.this.getClient();
                if (client != null) {
                    synchronized (AutofillManager.this.mLock) {
                        if (client.autofillClientIsFillUiShowing()) {
                            notifyViewEntered(this.mFocusedWindowId, this.mFocusedNodeId, this.mFocusedBounds);
                        }
                        updateTrackedViewsLocked();
                    }
                }
            }
            return accessibilityEnabled ? event : null;
        }

        private boolean notifyViewEntered(int windowId, long nodeId, Rect focusedBounds) {
            int virtualId = AccessibilityNodeInfo.getVirtualDescendantId(nodeId);
            if (!isVirtualNode(virtualId)) {
                return false;
            }
            View view = findViewByAccessibilityId(windowId, nodeId);
            if (view == null) {
                return false;
            }
            AccessibilityNodeInfo node = findVirtualNodeByAccessibilityId(view, virtualId);
            if (node == null || !node.isEditable()) {
                return false;
            }
            Rect newBounds = this.mTempBounds;
            node.getBoundsInScreen(newBounds);
            if (newBounds.equals(focusedBounds)) {
                return false;
            }
            focusedBounds.set(newBounds);
            AutofillManager.this.notifyViewEntered(view, virtualId, newBounds);
            return true;
        }

        private void notifyViewExited(int windowId, long nodeId) {
            int virtualId = AccessibilityNodeInfo.getVirtualDescendantId(nodeId);
            if (isVirtualNode(virtualId)) {
                View view = findViewByAccessibilityId(windowId, nodeId);
                if (view != null) {
                    AutofillManager.this.notifyViewExited(view, virtualId);
                }
            }
        }

        private void notifyValueChanged(int windowId, long nodeId) {
            int virtualId = AccessibilityNodeInfo.getVirtualDescendantId(nodeId);
            if (isVirtualNode(virtualId)) {
                View view = findViewByAccessibilityId(windowId, nodeId);
                if (view != null) {
                    AccessibilityNodeInfo node = findVirtualNodeByAccessibilityId(view, virtualId);
                    if (node != null) {
                        AutofillManager.this.notifyValueChanged(view, virtualId, AutofillValue.forText(node.getText()));
                    }
                }
            }
        }

        private void notifyViewClicked(int windowId, long nodeId) {
            int virtualId = AccessibilityNodeInfo.getVirtualDescendantId(nodeId);
            if (isVirtualNode(virtualId)) {
                View view = findViewByAccessibilityId(windowId, nodeId);
                if (view != null && findVirtualNodeByAccessibilityId(view, virtualId) != null) {
                    AutofillManager.this.notifyViewClicked(view, virtualId);
                }
            }
        }

        @GuardedBy("mLock")
        private void updateTrackedViewsLocked() {
            if (AutofillManager.this.mTrackedViews != null) {
                AutofillManager.this.mTrackedViews.onVisibleForAutofillChangedLocked();
            }
        }

        private View findViewByAccessibilityId(int windowId, long nodeId) {
            AutofillClient client = AutofillManager.this.getClient();
            if (client == null) {
                return null;
            }
            return client.autofillClientFindViewByAccessibilityIdTraversal(AccessibilityNodeInfo.getAccessibilityViewId(nodeId), windowId);
        }

        private AccessibilityNodeInfo findVirtualNodeByAccessibilityId(View view, int virtualId) {
            AccessibilityNodeProvider provider = view.getAccessibilityNodeProvider();
            if (provider == null) {
                return null;
            }
            return provider.createAccessibilityNodeInfo(virtualId);
        }

        private boolean isVirtualNode(int nodeId) {
            return (nodeId == -1 || nodeId == Integer.MAX_VALUE) ? false : true;
        }
    }

    private static final class AutofillManagerClient extends Stub {
        private final WeakReference<AutofillManager> mAfm;

        AutofillManagerClient(AutofillManager autofillManager) {
            this.mAfm = new WeakReference(autofillManager);
        }

        public void setState(int flags) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$$Lambda$AutofillManager$AutofillManagerClient$qH36EJk2Hkdja9ZZmTxqYPyr0YA(afm, flags));
            }
        }

        public void autofill(int sessionId, List<AutofillId> ids, List<AutofillValue> values) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$$Lambda$AutofillManager$AutofillManagerClient$1jAzMluMSJksx55SMUQn4BKB2Ng(afm, sessionId, ids, values));
            }
        }

        public void authenticate(int sessionId, int authenticationId, IntentSender intent, Intent fillInIntent) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$$Lambda$AutofillManager$AutofillManagerClient$qyxZ4PACUgHFGSvMBHzgwjJ3yns(afm, sessionId, authenticationId, intent, fillInIntent));
            }
        }

        public void requestShowFillUi(int sessionId, AutofillId id, int width, int height, Rect anchorBounds, IAutofillWindowPresenter presenter) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$$Lambda$AutofillManager$AutofillManagerClient$kRL9XILLc2XNr90gxVDACLzcyqc(afm, sessionId, id, width, height, anchorBounds, presenter));
            }
        }

        public void requestHideFillUi(int sessionId, AutofillId id) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$$Lambda$AutofillManager$AutofillManagerClient$dCTetwfU0gT1ZrSzZGZiGStXlOY(afm, id));
            }
        }

        public void notifyNoFillUi(int sessionId, AutofillId id, int sessionFinishedState) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$$Lambda$AutofillManager$AutofillManagerClient$K79QnIPRaZuikYDQdsLcIUBhqiI(afm, sessionId, id, sessionFinishedState));
            }
        }

        public void dispatchUnhandledKey(int sessionId, AutofillId id, KeyEvent fullScreen) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$$Lambda$AutofillManager$AutofillManagerClient$xqXjXW0fvc8JdYR5fgGKw9lJc3I(afm, sessionId, id, fullScreen));
            }
        }

        public void startIntentSender(IntentSender intentSender, Intent intent) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$$Lambda$AutofillManager$AutofillManagerClient$pM5e3ez5KTBdZt4d8qLEERBUSiU(afm, intentSender, intent));
            }
        }

        static /* synthetic */ void lambda$startIntentSender$7(AutofillManager afm, IntentSender intentSender, Intent intent) {
            try {
                afm.mContext.startIntentSender(intentSender, intent, 0, 0, 0);
            } catch (SendIntentException e) {
                String str = AutofillManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startIntentSender() failed for intent:");
                stringBuilder.append(intentSender);
                Log.e(str, stringBuilder.toString(), e);
            }
        }

        public void setTrackedViews(int sessionId, AutofillId[] ids, boolean saveOnAllViewsInvisible, boolean saveOnFinish, AutofillId[] fillableIds, AutofillId saveTriggerId) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$$Lambda$AutofillManager$AutofillManagerClient$BPlC2x7GLNHFS92rPUSzbcpFhUc(afm, sessionId, ids, saveOnAllViewsInvisible, saveOnFinish, fillableIds, saveTriggerId));
            }
        }

        public void setSaveUiState(int sessionId, boolean shown) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$$Lambda$AutofillManager$AutofillManagerClient$QIW-100CKwHzdHffwaus9KOEHCA(afm, sessionId, shown));
            }
        }

        public void setSessionFinished(int newState) {
            AutofillManager afm = (AutofillManager) this.mAfm.get();
            if (afm != null) {
                afm.post(new -$$Lambda$AutofillManager$AutofillManagerClient$V-s28jF4_S72cRk4llkGpwbJnmk(afm, newState));
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

    public void enableCompatibilityMode() {
        synchronized (this.mLock) {
            this.mCompatibilityBridge = new CompatibilityBridge();
        }
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
                    AutofillClient client = getClient();
                    if (client != null) {
                        try {
                            if (this.mService.restoreSession(this.mSessionId, client.autofillClientGetActivityToken(), this.mServiceClient.asBinder())) {
                                if (Helper.sDebug) {
                                    String str = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("session ");
                                    stringBuilder.append(this.mSessionId);
                                    stringBuilder.append(" was restored");
                                    Log.d(str, stringBuilder.toString());
                                }
                                client.autofillClientResetableStateAvailable();
                            } else {
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Session ");
                                stringBuilder2.append(this.mSessionId);
                                stringBuilder2.append(" could not be restored");
                                Log.w(str2, stringBuilder2.toString());
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
        Choreographer.getInstance().postCallback(3, new -$$Lambda$AutofillManager$YfpJNFodEuj5lbXfPlc77fsEvC8(this), null);
    }

    public static /* synthetic */ void lambda$onVisibleForAutofill$0(AutofillManager autofillManager) {
        synchronized (autofillManager.mLock) {
            if (autofillManager.mEnabled && autofillManager.isActiveLocked() && autofillManager.mTrackedViews != null) {
                autofillManager.mTrackedViews.onVisibleForAutofillChangedLocked();
            }
        }
    }

    public void onInvisibleForAutofill() {
        synchronized (this.mLock) {
            this.mOnInvisibleCalled = true;
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

    @GuardedBy("mLock")
    public boolean isCompatibilityModeEnabledLocked() {
        return this.mCompatibilityBridge != null;
    }

    public boolean isEnabled() {
        if (!hasAutofillFeature()) {
            return false;
        }
        synchronized (this.mLock) {
            if (isDisabledByServiceLocked()) {
                return false;
            }
            ensureServiceClientAddedIfNeededLocked();
            boolean z = this.mEnabled;
            return z;
        }
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

    @GuardedBy("mLock")
    private boolean shouldIgnoreViewEnteredLocked(AutofillId id, int flags) {
        String str;
        StringBuilder stringBuilder;
        if (isDisabledByServiceLocked()) {
            if (Helper.sVerbose) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ignoring notifyViewEntered(flags=");
                stringBuilder.append(flags);
                stringBuilder.append(", view=");
                stringBuilder.append(id);
                stringBuilder.append(") on state ");
                stringBuilder.append(getStateAsStringLocked());
                stringBuilder.append(" because disabled by svc");
                Log.v(str, stringBuilder.toString());
            }
            return true;
        } else if (isFinishedLocked() && (flags & 1) == 0 && this.mEnteredIds != null && this.mEnteredIds.contains(id)) {
            if (Helper.sVerbose) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ignoring notifyViewEntered(flags=");
                stringBuilder.append(flags);
                stringBuilder.append(", view=");
                stringBuilder.append(id);
                stringBuilder.append(") on state ");
                stringBuilder.append(getStateAsStringLocked());
                stringBuilder.append(" because view was already entered: ");
                stringBuilder.append(this.mEnteredIds);
                Log.v(str, stringBuilder.toString());
            }
            return true;
        } else {
            if (Helper.sVerbose) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("not ignoring notifyViewEntered(flags=");
                stringBuilder2.append(flags);
                stringBuilder2.append(", view=");
                stringBuilder2.append(id);
                stringBuilder2.append(", state ");
                stringBuilder2.append(getStateAsStringLocked());
                stringBuilder2.append(", enteredIds=");
                stringBuilder2.append(this.mEnteredIds);
                Log.v(str, stringBuilder2.toString());
            }
            return false;
        }
    }

    private boolean isClientVisibleForAutofillLocked() {
        AutofillClient client = getClient();
        return client != null && client.autofillClientIsVisibleForAutofill();
    }

    private boolean isClientDisablingEnterExitEvent() {
        AutofillClient client = getClient();
        return client != null && client.isDisablingEnterExitEventForAutofill();
    }

    private void notifyViewEntered(View view, int flags) {
        if (this.mHwAutofillHelper != null) {
            this.mHwAutofillHelper.recordCurrentInfo(this.mContext, view);
        }
        if (hasAutofillFeature()) {
            AutofillCallback callback;
            synchronized (this.mLock) {
                callback = notifyViewEnteredLocked(view, flags);
            }
            if (callback != null) {
                this.mCallback.onAutofillEvent(view, 3);
            }
        }
    }

    @GuardedBy("mLock")
    private AutofillCallback notifyViewEnteredLocked(View view, int flags) {
        AutofillId id = view.getAutofillId();
        if (shouldIgnoreViewEnteredLocked(id, flags)) {
            return null;
        }
        AutofillCallback callback = null;
        ensureServiceClientAddedIfNeededLocked();
        if (this.mEnabled) {
            if (!isClientDisablingEnterExitEvent()) {
                AutofillValue value = view.getAutofillValue();
                if (isActiveLocked()) {
                    updateSessionLocked(id, null, value, 2, flags);
                } else {
                    startSessionLocked(id, null, value, flags);
                }
                addEnteredIdLocked(id);
            }
        } else if (this.mCallback != null) {
            callback = this.mCallback;
        }
        return callback;
    }

    public void notifyViewExited(View view) {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                notifyViewExitedLocked(view);
            }
        }
    }

    @GuardedBy("mLock")
    void notifyViewExitedLocked(View view) {
        ensureServiceClientAddedIfNeededLocked();
        if (this.mEnabled && isActiveLocked() && !isClientDisablingEnterExitEvent()) {
            updateSessionLocked(view.getAutofillId(), null, null, 3, 0);
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
                String str;
                StringBuilder stringBuilder;
                if (virtual) {
                    id = getAutofillId(view, virtualId);
                } else {
                    id = view.getAutofillId();
                }
                if (Helper.sVerbose) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("visibility changed for ");
                    stringBuilder.append(id);
                    stringBuilder.append(": ");
                    stringBuilder.append(isVisible);
                    Log.v(str, stringBuilder.toString());
                }
                if (!(isVisible || this.mFillableIds == null || !this.mFillableIds.contains(id))) {
                    if (Helper.sDebug) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Hidding UI when view ");
                        stringBuilder.append(id);
                        stringBuilder.append(" became invisible");
                        Log.d(str, stringBuilder.toString());
                    }
                    requestHideFillUi(id, view);
                }
                if (this.mTrackedViews != null) {
                    this.mTrackedViews.notifyViewVisibilityChangedLocked(id, isVisible);
                } else if (Helper.sVerbose) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Ignoring visibility change on ");
                    stringBuilder.append(id);
                    stringBuilder.append(": no tracked views");
                    Log.v(str, stringBuilder.toString());
                }
            }
        }
    }

    public void notifyViewEntered(View view, int virtualId, Rect absBounds) {
        notifyViewEntered(view, virtualId, absBounds, 0);
    }

    private void notifyViewEntered(View view, int virtualId, Rect bounds, int flags) {
        if (hasAutofillFeature()) {
            AutofillCallback callback;
            synchronized (this.mLock) {
                callback = notifyViewEnteredLocked(view, virtualId, bounds, flags);
            }
            if (callback != null) {
                callback.onAutofillEvent(view, virtualId, 3);
            }
        }
    }

    @GuardedBy("mLock")
    private AutofillCallback notifyViewEnteredLocked(View view, int virtualId, Rect bounds, int flags) {
        AutofillId id = getAutofillId(view, virtualId);
        AutofillCallback callback = null;
        if (shouldIgnoreViewEnteredLocked(id, flags)) {
            return null;
        }
        ensureServiceClientAddedIfNeededLocked();
        if (this.mEnabled) {
            if (!isClientDisablingEnterExitEvent()) {
                if (isActiveLocked()) {
                    updateSessionLocked(id, bounds, null, 2, flags);
                } else {
                    startSessionLocked(id, bounds, null, flags);
                }
                addEnteredIdLocked(id);
            }
        } else if (this.mCallback != null) {
            callback = this.mCallback;
        }
        return callback;
    }

    @GuardedBy("mLock")
    private void addEnteredIdLocked(AutofillId id) {
        if (this.mEnteredIds == null) {
            this.mEnteredIds = new ArraySet(1);
        }
        this.mEnteredIds.add(id);
    }

    public void notifyViewExited(View view, int virtualId) {
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyViewExited(");
            stringBuilder.append(view.getAutofillId());
            stringBuilder.append(", ");
            stringBuilder.append(virtualId);
            Log.v(str, stringBuilder.toString());
        }
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                notifyViewExitedLocked(view, virtualId);
            }
        }
    }

    @GuardedBy("mLock")
    private void notifyViewExitedLocked(View view, int virtualId) {
        ensureServiceClientAddedIfNeededLocked();
        if (this.mEnabled && isActiveLocked() && !isClientDisablingEnterExitEvent()) {
            updateSessionLocked(getAutofillId(view, virtualId), null, null, 3, 0);
        }
    }

    /* JADX WARNING: Missing block: B:32:0x0096, code skipped:
            return;
     */
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
                    id = view.getAutofillId();
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
                if (this.mEnabled) {
                    if (isActiveLocked()) {
                        if (id == null) {
                            id = view.getAutofillId();
                        }
                        if (!valueWasRead) {
                            value = view.getAutofillValue();
                        }
                        updateSessionLocked(id, null, value, 4, 0);
                        return;
                    }
                }
                if (Helper.sVerbose) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("notifyValueChanged(");
                    stringBuilder.append(view.getAutofillId());
                    stringBuilder.append("): ignoring on state ");
                    stringBuilder.append(getStateAsStringLocked());
                    Log.v(str, stringBuilder.toString());
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0057, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyValueChanged(View view, int virtualId, AutofillValue value) {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                if (this.mEnabled) {
                    if (isActiveLocked()) {
                        updateSessionLocked(getAutofillId(view, virtualId), null, value, 4, 0);
                        return;
                    }
                }
                if (Helper.sVerbose) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("notifyValueChanged(");
                    stringBuilder.append(view.getAutofillId());
                    stringBuilder.append(SettingsStringUtil.DELIMITER);
                    stringBuilder.append(virtualId);
                    stringBuilder.append("): ignoring on state ");
                    stringBuilder.append(getStateAsStringLocked());
                    Log.v(str, stringBuilder.toString());
                }
            }
        }
    }

    public void notifyViewClicked(View view) {
        notifyViewClicked(view.getAutofillId());
    }

    public void notifyViewClicked(View view, int virtualId) {
        notifyViewClicked(getAutofillId(view, virtualId));
    }

    /* JADX WARNING: Missing block: B:23:0x0070, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:25:0x0072, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyViewClicked(AutofillId id) {
        if (hasAutofillFeature()) {
            if (Helper.sVerbose) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("notifyViewClicked(): id=");
                stringBuilder.append(id);
                stringBuilder.append(", trigger=");
                stringBuilder.append(this.mSaveTriggerId);
                Log.v(str, stringBuilder.toString());
            }
            synchronized (this.mLock) {
                if (this.mEnabled) {
                    if (isActiveLocked()) {
                        if (this.mSaveTriggerId != null && this.mSaveTriggerId.equals(id)) {
                            if (Helper.sDebug) {
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("triggering commit by click of ");
                                stringBuilder2.append(id);
                                Log.d(str2, stringBuilder2.toString());
                            }
                            commitLocked();
                            this.mMetricsLogger.write(newLog(1229));
                        }
                    }
                }
            }
        }
    }

    public void onActivityFinishing() {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                if (this.mSaveOnFinish) {
                    if (Helper.sDebug) {
                        Log.d(TAG, "onActivityFinishing(): calling commitLocked()");
                    }
                    commitLocked();
                } else {
                    if (Helper.sDebug) {
                        Log.d(TAG, "onActivityFinishing(): calling cancelLocked()");
                    }
                    cancelLocked();
                }
            }
        }
    }

    public void commit() {
        if (hasAutofillFeature()) {
            if (Helper.sVerbose) {
                Log.v(TAG, "commit() called by app");
            }
            synchronized (this.mLock) {
                commitLocked();
            }
        }
    }

    @GuardedBy("mLock")
    private void commitLocked() {
        if (this.mEnabled || isActiveLocked()) {
            finishSessionLocked();
        }
    }

    public void cancel() {
        if (Helper.sVerbose) {
            Log.v(TAG, "cancel() called by app");
        }
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                cancelLocked();
            }
        }
    }

    @GuardedBy("mLock")
    private void cancelLocked() {
        if (this.mEnabled || isActiveLocked()) {
            cancelSessionLocked();
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

    public ComponentName getAutofillServiceComponentName() {
        if (this.mService == null) {
            return null;
        }
        try {
            return this.mService.getAutofillServiceComponentName();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getUserDataId() {
        try {
            return this.mService.getUserDataId();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    public UserData getUserData() {
        try {
            return this.mService.getUserData();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    public void setUserData(UserData userData) {
        try {
            this.mService.setUserData(userData);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public boolean isFieldClassificationEnabled() {
        try {
            return this.mService.isFieldClassificationEnabled();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public String getDefaultFieldClassificationAlgorithm() {
        try {
            return this.mService.getDefaultFieldClassificationAlgorithm();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    public List<String> getAvailableFieldClassificationAlgorithms() {
        try {
            String[] algorithms = this.mService.getAvailableFieldClassificationAlgorithms();
            return algorithms != null ? Arrays.asList(algorithms) : Collections.emptyList();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
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

    private AutofillClient getClient() {
        AutofillClient client = this.mContext.getAutofillClient();
        if (client == null && Helper.sDebug) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No AutofillClient for ");
            stringBuilder.append(this.mContext.getPackageName());
            stringBuilder.append(" on context ");
            stringBuilder.append(this.mContext);
            Log.d(str, stringBuilder.toString());
        }
        return client;
    }

    public boolean isAutofillUiShowing() {
        AutofillClient client = this.mContext.getAutofillClient();
        return client != null && client.autofillClientIsFillUiShowing();
    }

    public void onAuthenticationResult(int authenticationId, Intent data, View focusView) {
        if (hasAutofillFeature()) {
            if (Helper.sDebug) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onAuthenticationResult(): d=");
                stringBuilder.append(data);
                Log.d(str, stringBuilder.toString());
            }
            synchronized (this.mLock) {
                if (isActiveLocked()) {
                    if (!(this.mOnInvisibleCalled || focusView == null || !focusView.canNotifyAutofillEnterExitEvent())) {
                        notifyViewExitedLocked(focusView);
                        notifyViewEnteredLocked(focusView, 0);
                    }
                    if (data == null) {
                        return;
                    }
                    try {
                        Thread.sleep(60);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "onAuthenticationResult() sleep Interrupted");
                    }
                    Parcelable result = data.getParcelableExtra(EXTRA_AUTHENTICATION_RESULT);
                    Bundle responseData = new Bundle();
                    responseData.putParcelable(EXTRA_AUTHENTICATION_RESULT, result);
                    Bundle newClientState = data.getBundleExtra(EXTRA_CLIENT_STATE);
                    if (newClientState != null) {
                        responseData.putBundle(EXTRA_CLIENT_STATE, newClientState);
                    }
                    try {
                        this.mService.setAuthenticationResult(responseData, this.mSessionId, authenticationId, this.mContext.getUserId());
                    } catch (RemoteException e2) {
                        Log.e(TAG, "Error delivering authentication result", e2);
                    }
                } else {
                    return;
                }
            }
        }
        return;
    }

    public AutofillId getNextAutofillId() {
        AutofillClient client = getClient();
        if (client == null) {
            return null;
        }
        AutofillId id = client.autofillClientGetNextAutofillId();
        if (id == null && Helper.sDebug) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getNextAutofillId(): client ");
            stringBuilder.append(client);
            stringBuilder.append(" returned null");
            Log.d(str, stringBuilder.toString());
        }
        return id;
    }

    private static AutofillId getAutofillId(View parent, int virtualId) {
        return new AutofillId(parent.getAutofillViewId(), virtualId);
    }

    @GuardedBy("mLock")
    private void startSessionLocked(AutofillId id, Rect bounds, AutofillValue value, int flags) {
        String str;
        StringBuilder stringBuilder;
        Rect rect;
        AutofillValue autofillValue;
        AutofillId autofillId = id;
        int i = flags;
        if (Helper.sVerbose) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startSessionLocked(): id=");
            stringBuilder.append(autofillId);
            stringBuilder.append(", bounds=");
            rect = bounds;
            stringBuilder.append(rect);
            stringBuilder.append(", value=");
            autofillValue = value;
            stringBuilder.append(autofillValue);
            stringBuilder.append(", flags=");
            stringBuilder.append(i);
            stringBuilder.append(", state=");
            stringBuilder.append(getStateAsStringLocked());
            stringBuilder.append(", compatMode=");
            stringBuilder.append(isCompatibilityModeEnabledLocked());
            stringBuilder.append(", enteredIds=");
            stringBuilder.append(this.mEnteredIds);
            Log.v(str, stringBuilder.toString());
        } else {
            rect = bounds;
            autofillValue = value;
        }
        if (this.mState == 0 || isFinishedLocked() || (i & 1) != 0) {
            try {
                AutofillClient client = getClient();
                if (client != null) {
                    int i2 = 1;
                    this.mSessionId = this.mService.startSession(client.autofillClientGetActivityToken(), this.mServiceClient.asBinder(), autofillId, rect, autofillValue, this.mContext.getUserId(), this.mCallback != null, i, client.autofillClientGetComponentName(), isCompatibilityModeEnabledLocked());
                    if (this.mSessionId != Integer.MIN_VALUE) {
                        this.mState = i2;
                    }
                    client.autofillClientResetableStateAvailable();
                    return;
                }
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        if (Helper.sVerbose) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("not automatically starting session for ");
            stringBuilder.append(autofillId);
            stringBuilder.append(" on state ");
            stringBuilder.append(getStateAsStringLocked());
            stringBuilder.append(" and flags ");
            stringBuilder.append(i);
            Log.v(str, stringBuilder.toString());
        }
    }

    @GuardedBy("mLock")
    private void finishSessionLocked() {
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("finishSessionLocked(): ");
            stringBuilder.append(getStateAsStringLocked());
            Log.v(str, stringBuilder.toString());
        }
        if (isActiveLocked()) {
            try {
                this.mService.finishSession(this.mSessionId, this.mContext.getUserId());
                resetSessionLocked(true);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @GuardedBy("mLock")
    private void cancelSessionLocked() {
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cancelSessionLocked(): ");
            stringBuilder.append(getStateAsStringLocked());
            Log.v(str, stringBuilder.toString());
        }
        if (isActiveLocked()) {
            try {
                this.mService.cancelSession(this.mSessionId, this.mContext.getUserId());
                resetSessionLocked(true);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @GuardedBy("mLock")
    private void resetSessionLocked(boolean resetEnteredIds) {
        this.mSessionId = Integer.MIN_VALUE;
        this.mState = 0;
        this.mTrackedViews = null;
        this.mFillableIds = null;
        this.mSaveTriggerId = null;
        this.mIdShownFillUi = null;
        if (resetEnteredIds) {
            this.mEnteredIds = null;
        }
    }

    @GuardedBy("mLock")
    private void updateSessionLocked(AutofillId id, Rect bounds, AutofillValue value, int action, int flags) {
        AutofillId autofillId;
        Rect rect;
        AutofillValue autofillValue;
        int i = flags;
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateSessionLocked(): id=");
            autofillId = id;
            stringBuilder.append(autofillId);
            stringBuilder.append(", bounds=");
            rect = bounds;
            stringBuilder.append(rect);
            stringBuilder.append(", value=");
            autofillValue = value;
            stringBuilder.append(autofillValue);
            stringBuilder.append(", action=");
            stringBuilder.append(action);
            stringBuilder.append(", flags=");
            stringBuilder.append(i);
            Log.v(str, stringBuilder.toString());
        } else {
            autofillId = id;
            rect = bounds;
            autofillValue = value;
            int i2 = action;
        }
        if ((i & 1) != 0) {
            try {
                AutofillClient client = getClient();
                if (client != null) {
                    int newId = this.mService.updateOrRestartSession(client.autofillClientGetActivityToken(), this.mServiceClient.asBinder(), autofillId, rect, autofillValue, this.mContext.getUserId(), this.mCallback != null, i, client.autofillClientGetComponentName(), this.mSessionId, action, isCompatibilityModeEnabledLocked());
                    if (newId != this.mSessionId) {
                        if (Helper.sDebug) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Session restarted: ");
                            stringBuilder2.append(this.mSessionId);
                            stringBuilder2.append("=>");
                            stringBuilder2.append(newId);
                            Log.d(str2, stringBuilder2.toString());
                        }
                        this.mSessionId = newId;
                        this.mState = this.mSessionId == Integer.MIN_VALUE ? 0 : 1;
                        client.autofillClientResetableStateAvailable();
                    }
                } else {
                    return;
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        this.mService.updateSession(this.mSessionId, id, bounds, value, action, i, this.mContext.getUserId());
    }

    @GuardedBy("mLock")
    private void ensureServiceClientAddedIfNeededLocked() {
        if (getClient() != null && this.mServiceClient == null) {
            this.mServiceClient = new AutofillManagerClient(this);
            try {
                int userId = this.mContext.getUserId();
                int flags = this.mService.addClient(this.mServiceClient, userId);
                boolean z = false;
                this.mEnabled = (flags & 1) != 0;
                Helper.sDebug = (flags & 2) != 0;
                if ((flags & 4) != 0) {
                    z = true;
                }
                Helper.sVerbose = z;
                this.mServiceClientCleaner = Cleaner.create(this, new -$$Lambda$AutofillManager$V76JiQu509LCUz3-ckpb-nB3JhA(this.mService, this.mServiceClient, userId));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    static /* synthetic */ void lambda$ensureServiceClientAddedIfNeededLocked$1(IAutoFillManager service, IAutoFillManagerClient serviceClient, int userId) {
        try {
            service.removeClient(serviceClient, userId);
        } catch (RemoteException e) {
        }
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:6:0x000c, B:17:0x001c] */
    /* JADX WARNING: Missing block: B:19:0x002a, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:22:0x002f, code skipped:
            throw r2.rethrowFromSystemServer();
     */
    /* JADX WARNING: Missing block: B:24:0x0031, code skipped:
            return;
     */
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
                    this.mService.setHasCallback(this.mSessionId, this.mContext.getUserId(), true);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:24:0x0032, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void unregisterCallback(AutofillCallback callback) {
        if (hasAutofillFeature()) {
            synchronized (this.mLock) {
                if (callback != null) {
                    try {
                        if (this.mCallback != null) {
                            if (callback == this.mCallback) {
                                this.mCallback = null;
                                this.mService.setHasCallback(this.mSessionId, this.mContext.getUserId(), false);
                            }
                        }
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    } catch (Throwable th) {
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x002e, code skipped:
            if (r10 == null) goto L_0x0042;
     */
    /* JADX WARNING: Missing block: B:18:0x0035, code skipped:
            if (r2.isVirtual() == false) goto L_0x003f;
     */
    /* JADX WARNING: Missing block: B:19:0x0037, code skipped:
            r10.onAutofillEvent(r9, r2.getVirtualChildId(), 1);
     */
    /* JADX WARNING: Missing block: B:20:0x003f, code skipped:
            r10.onAutofillEvent(r9, 1);
     */
    /* JADX WARNING: Missing block: B:21:0x0042, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void requestShowFillUi(int sessionId, AutofillId id, int width, int height, Rect anchorBounds, IAutofillWindowPresenter presenter) {
        Throwable th;
        AutofillId autofillId = id;
        View anchor = findView(autofillId);
        if (anchor != null) {
            AutofillCallback callback = null;
            synchronized (this.mLock) {
                try {
                    if (this.mSessionId == sessionId) {
                        try {
                            AutofillClient client = getClient();
                            if (client != null && client.autofillClientRequestShowFillUi(anchor, width, height, anchorBounds, presenter)) {
                                callback = this.mCallback;
                                this.mIdShownFillUi = autofillId;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    int i = sessionId;
                    throw th;
                }
            }
        }
    }

    private void authenticate(int sessionId, int authenticationId, IntentSender intent, Intent fillInIntent) {
        synchronized (this.mLock) {
            if (sessionId == this.mSessionId) {
                AutofillClient client = getClient();
                if (client != null) {
                    this.mOnInvisibleCalled = false;
                    client.autofillClientAuthenticate(authenticationId, intent, fillInIntent);
                }
            }
        }
    }

    private void dispatchUnhandledKey(int sessionId, AutofillId id, KeyEvent keyEvent) {
        View anchor = findView(id);
        if (anchor != null) {
            synchronized (this.mLock) {
                if (this.mSessionId == sessionId) {
                    AutofillClient client = getClient();
                    if (client != null) {
                        client.autofillClientDispatchUnhandledKey(anchor, keyEvent);
                    }
                }
            }
        }
    }

    private void setState(int flags) {
        boolean z;
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setState(");
            stringBuilder.append(flags);
            stringBuilder.append(")");
            Log.v(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            z = false;
            this.mEnabled = (flags & 1) != 0;
            if (!(this.mEnabled && (flags & 2) == 0)) {
                resetSessionLocked(true);
            }
            if ((flags & 4) != 0) {
                this.mServiceClient = null;
                if (this.mServiceClientCleaner != null) {
                    this.mServiceClientCleaner.clean();
                    this.mServiceClientCleaner = null;
                }
            }
        }
        Helper.sDebug = (flags & 8) != 0;
        if ((flags & 16) != 0) {
            z = true;
        }
        Helper.sVerbose = z;
    }

    private void setAutofilledIfValuesIs(View view, AutofillValue targetValue) {
        if (Objects.equals(view.getAutofillValue(), targetValue)) {
            synchronized (this.mLock) {
                if (this.mLastAutofilledData == null) {
                    this.mLastAutofilledData = new ParcelableMap(1);
                }
                this.mLastAutofilledData.put(view.getAutofillId(), targetValue);
            }
            view.setAutofilled(true);
        }
    }

    private void autofill(int sessionId, List<AutofillId> ids, List<AutofillValue> values) {
        Throwable th;
        List<AutofillValue> list;
        synchronized (this.mLock) {
            List<AutofillId> list2;
            try {
                if (sessionId != this.mSessionId) {
                    return;
                }
                AutofillClient client = getClient();
                if (client == null) {
                    return;
                }
                AutofillClient client2;
                View[] views;
                int itemCount = ids.size();
                ArrayMap<View, SparseArray<AutofillValue>> virtualValues = null;
                View[] views2 = client.autofillClientFindViewsByAutofillIdTraversal(Helper.toArray(ids));
                ArrayList<AutofillId> failedIds = null;
                int numApplied = 0;
                int i = 0;
                while (i < itemCount) {
                    try {
                        AutofillId id = (AutofillId) ids.get(i);
                        AutofillValue value = (AutofillValue) values.get(i);
                        int viewId = id.getViewId();
                        View view = views2[i];
                        if (view == null) {
                            String str = TAG;
                            client2 = client;
                            StringBuilder stringBuilder = new StringBuilder();
                            views = views2;
                            stringBuilder.append("autofill(): no View with id ");
                            stringBuilder.append(id);
                            Log.d(str, stringBuilder.toString());
                            if (failedIds == null) {
                                failedIds = new ArrayList();
                            }
                            failedIds.add(id);
                        } else {
                            client2 = client;
                            views = views2;
                            if (id.isVirtual()) {
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
                        i++;
                        client = client2;
                        views2 = views;
                        int i2 = sessionId;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
                list2 = ids;
                list = values;
                client2 = client;
                views = views2;
                if (failedIds != null) {
                    if (Helper.sVerbose) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("autofill(): total failed views: ");
                        stringBuilder2.append(failedIds);
                        Log.v(str2, stringBuilder2.toString());
                    }
                    try {
                        this.mService.setAutofillFailure(this.mSessionId, failedIds, this.mContext.getUserId());
                    } catch (RemoteException e) {
                        e.rethrowFromSystemServer();
                    }
                }
                if (virtualValues != null) {
                    int i3 = 0;
                    while (true) {
                        i = i3;
                        if (i >= virtualValues.size()) {
                            break;
                        }
                        SparseArray childrenValues = (SparseArray) virtualValues.valueAt(i);
                        ((View) virtualValues.keyAt(i)).autofill(childrenValues);
                        numApplied += childrenValues.size();
                        i3 = i + 1;
                    }
                }
                this.mMetricsLogger.write(newLog(913).addTaggedData(914, Integer.valueOf(itemCount)).addTaggedData(915, Integer.valueOf(numApplied)));
            } catch (Throwable th3) {
                th = th3;
                list2 = ids;
                list = values;
                throw th;
            }
        }
    }

    private LogMaker newLog(int category) {
        LogMaker log = new LogMaker(category).addTaggedData(1456, Integer.valueOf(this.mSessionId));
        if (isCompatibilityModeEnabledLocked()) {
            log.addTaggedData(1414, Integer.valueOf(1));
        }
        AutofillClient client = getClient();
        if (client == null) {
            log.setPackageName(this.mContext.getPackageName());
        } else {
            log.setComponentName(client.autofillClientGetComponentName());
        }
        return log;
    }

    private void setTrackedViews(int sessionId, AutofillId[] trackedIds, boolean saveOnAllViewsInvisible, boolean saveOnFinish, AutofillId[] fillableIds, AutofillId saveTriggerId) {
        synchronized (this.mLock) {
            if (this.mEnabled && this.mSessionId == sessionId) {
                if (saveOnAllViewsInvisible) {
                    this.mTrackedViews = new TrackedViews(trackedIds);
                } else {
                    this.mTrackedViews = null;
                }
                this.mSaveOnFinish = saveOnFinish;
                if (fillableIds != null) {
                    if (this.mFillableIds == null) {
                        this.mFillableIds = new ArraySet(fillableIds.length);
                    }
                    for (AutofillId id : fillableIds) {
                        this.mFillableIds.add(id);
                    }
                    if (Helper.sVerbose) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("setTrackedViews(): fillableIds=");
                        stringBuilder.append(fillableIds);
                        stringBuilder.append(", mFillableIds");
                        stringBuilder.append(this.mFillableIds);
                        Log.v(str, stringBuilder.toString());
                    }
                }
                if (!(this.mSaveTriggerId == null || this.mSaveTriggerId.equals(saveTriggerId))) {
                    setNotifyOnClickLocked(this.mSaveTriggerId, false);
                }
                if (!(saveTriggerId == null || saveTriggerId.equals(this.mSaveTriggerId))) {
                    this.mSaveTriggerId = saveTriggerId;
                    setNotifyOnClickLocked(this.mSaveTriggerId, true);
                }
            }
        }
    }

    private void setNotifyOnClickLocked(AutofillId id, boolean notify) {
        View view = findView(id);
        if (view == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNotifyOnClick(): invalid id: ");
            stringBuilder.append(id);
            Log.w(str, stringBuilder.toString());
            return;
        }
        view.setNotifyAutofillManagerOnClick(notify);
    }

    private void setSaveUiState(int sessionId, boolean shown) {
        if (Helper.sDebug) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setSaveUiState(");
            stringBuilder.append(sessionId);
            stringBuilder.append("): ");
            stringBuilder.append(shown);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            if (this.mSessionId != Integer.MIN_VALUE) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setSaveUiState(");
                stringBuilder2.append(sessionId);
                stringBuilder2.append(", ");
                stringBuilder2.append(shown);
                stringBuilder2.append(") called on existing session ");
                stringBuilder2.append(this.mSessionId);
                stringBuilder2.append("; cancelling it");
                Log.w(str2, stringBuilder2.toString());
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setSessionFinished(): from ");
                stringBuilder.append(getStateAsStringLocked());
                stringBuilder.append(" to ");
                stringBuilder.append(getStateAsString(newState));
                Log.v(str, stringBuilder.toString());
            }
            if (newState == 5) {
                resetSessionLocked(true);
                this.mState = 0;
            } else {
                resetSessionLocked(false);
                this.mState = newState;
            }
        }
    }

    public void requestHideFillUi() {
        requestHideFillUi(this.mIdShownFillUi, true);
    }

    private void requestHideFillUi(AutofillId id, boolean force) {
        View anchor = id == null ? null : findView(id);
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("requestHideFillUi(");
            stringBuilder.append(id);
            stringBuilder.append("): anchor = ");
            stringBuilder.append(anchor);
            Log.v(str, stringBuilder.toString());
        }
        if (anchor == null) {
            if (force) {
                AutofillClient client = getClient();
                if (client != null) {
                    client.autofillClientRequestHideFillUi();
                }
            }
            return;
        }
        requestHideFillUi(id, anchor);
    }

    private void requestHideFillUi(AutofillId id, View anchor) {
        AutofillCallback callback = null;
        synchronized (this.mLock) {
            AutofillClient client = getClient();
            if (client != null && client.autofillClientRequestHideFillUi()) {
                this.mIdShownFillUi = null;
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

    private void notifyNoFillUi(int sessionId, AutofillId id, int sessionFinishedState) {
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyNoFillUi(): sessionId=");
            stringBuilder.append(sessionId);
            stringBuilder.append(", autofillId=");
            stringBuilder.append(id);
            stringBuilder.append(", sessionFinishedState=");
            stringBuilder.append(sessionFinishedState);
            Log.v(str, stringBuilder.toString());
        }
        View anchor = findView(id);
        if (anchor != null) {
            AutofillCallback callback = null;
            synchronized (this.mLock) {
                if (this.mSessionId == sessionId && getClient() != null) {
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
            if (sessionFinishedState != 0) {
                setSessionFinished(sessionFinishedState);
            }
        }
    }

    private View findView(AutofillId autofillId) {
        AutofillClient client = getClient();
        if (client != null) {
            return client.autofillClientFindViewByAutofillIdTraversal(autofillId);
        }
        return null;
    }

    public boolean hasAutofillFeature() {
        return this.mService != null;
    }

    public void onPendingSaveUi(int operation, IBinder token) {
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPendingSaveUi(");
            stringBuilder.append(operation);
            stringBuilder.append("): ");
            stringBuilder.append(token);
            Log.v(str, stringBuilder.toString());
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
        pw.print(outerPrefix);
        pw.println("AutofillManager:");
        String pfx = new StringBuilder();
        pfx.append(outerPrefix);
        pfx.append("  ");
        pfx = pfx.toString();
        pw.print(pfx);
        pw.print("sessionId: ");
        pw.println(this.mSessionId);
        pw.print(pfx);
        pw.print("state: ");
        pw.println(getStateAsStringLocked());
        pw.print(pfx);
        pw.print("context: ");
        pw.println(this.mContext);
        pw.print(pfx);
        pw.print("client: ");
        pw.println(getClient());
        pw.print(pfx);
        pw.print("enabled: ");
        pw.println(this.mEnabled);
        pw.print(pfx);
        pw.print("hasService: ");
        boolean z = false;
        pw.println(this.mService != null);
        pw.print(pfx);
        pw.print("hasCallback: ");
        if (this.mCallback != null) {
            z = true;
        }
        pw.println(z);
        pw.print(pfx);
        pw.print("onInvisibleCalled ");
        pw.println(this.mOnInvisibleCalled);
        pw.print(pfx);
        pw.print("last autofilled data: ");
        pw.println(this.mLastAutofilledData);
        pw.print(pfx);
        pw.print("tracked views: ");
        if (this.mTrackedViews == null) {
            pw.println("null");
        } else {
            String pfx2 = new StringBuilder();
            pfx2.append(pfx);
            pfx2.append("  ");
            pfx2 = pfx2.toString();
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
        pw.print(pfx);
        pw.print("entered ids: ");
        pw.println(this.mEnteredIds);
        pw.print(pfx);
        pw.print("save trigger id: ");
        pw.println(this.mSaveTriggerId);
        pw.print(pfx);
        pw.print("save on finish(): ");
        pw.println(this.mSaveOnFinish);
        pw.print(pfx);
        pw.print("compat mode enabled: ");
        pw.println(isCompatibilityModeEnabledLocked());
        pw.print(pfx);
        pw.print("debug: ");
        pw.print(Helper.sDebug);
        pw.print(" verbose: ");
        pw.println(Helper.sVerbose);
    }

    @GuardedBy("mLock")
    private String getStateAsStringLocked() {
        return getStateAsString(this.mState);
    }

    private static String getStateAsString(int state) {
        switch (state) {
            case 0:
                return "UNKNOWN";
            case 1:
                return "ACTIVE";
            case 2:
                return "FINISHED";
            case 3:
                return "SHOWING_SAVE_UI";
            case 4:
                return "DISABLED_BY_SERVICE";
            case 5:
                return "UNKNOWN_COMPAT_MODE";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("INVALID:");
                stringBuilder.append(state);
                return stringBuilder.toString();
        }
    }

    @GuardedBy("mLock")
    private boolean isActiveLocked() {
        return this.mState == 1;
    }

    @GuardedBy("mLock")
    private boolean isDisabledByServiceLocked() {
        return this.mState == 4;
    }

    @GuardedBy("mLock")
    private boolean isFinishedLocked() {
        return this.mState == 2;
    }

    private void post(Runnable runnable) {
        AutofillClient client = getClient();
        if (client == null) {
            if (Helper.sVerbose) {
                Log.v(TAG, "ignoring post() because client is null");
            }
            return;
        }
        client.autofillClientRunOnUiThread(runnable);
    }
}
