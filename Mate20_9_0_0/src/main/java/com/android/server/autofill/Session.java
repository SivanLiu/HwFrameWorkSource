package com.android.server.autofill;

import android.app.ActivityManager;
import android.app.IAssistDataReceiver;
import android.app.IAssistDataReceiver.Stub;
import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.AutofillOverlay;
import android.app.assist.AssistStructure.ViewNode;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.os.RemoteCallback.OnResultListener;
import android.os.RemoteException;
import android.os.SystemClock;
import android.service.autofill.AutofillFieldClassificationService.Scores;
import android.service.autofill.Dataset;
import android.service.autofill.FieldClassification;
import android.service.autofill.FieldClassification.Match;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.InternalSanitizer;
import android.service.autofill.InternalValidator;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.service.autofill.UserData;
import android.service.autofill.ValueFinder;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.KeyEvent;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManagerClient;
import android.view.autofill.IAutofillWindowPresenter;
import android.view.autofill.IHwAutofillHelper;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.autofill.RemoteFillService.FillServiceCallbacks;
import com.android.server.autofill.ui.AutoFillUI;
import com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback;
import com.android.server.autofill.ui.PendingUi;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

final class Session implements FillServiceCallbacks, Listener, AutoFillUiCallback, ValueFinder {
    private static final String EXTRA_REQUEST_ID = "android.service.autofill.extra.REQUEST_ID";
    private static final String TAG = "AutofillSession";
    private static AtomicInteger sIdCounter = new AtomicInteger();
    public final int id;
    @GuardedBy("mLock")
    private IBinder mActivityToken;
    private final IAssistDataReceiver mAssistReceiver = new Stub() {
        public void onHandleAssistData(Bundle resultData) throws RemoteException {
            AssistStructure structure = (AssistStructure) resultData.getParcelable("structure");
            if (structure == null) {
                Slog.e(Session.TAG, "No assist structure - app might have crashed providing it");
                return;
            }
            Bundle receiverExtras = resultData.getBundle("receiverExtras");
            if (receiverExtras == null) {
                Slog.e(Session.TAG, "No receiver extras - app might have crashed providing it");
                return;
            }
            int requestId = receiverExtras.getInt(Session.EXTRA_REQUEST_ID);
            if (Helper.sVerbose) {
                String str = Session.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("New structure for requestId ");
                stringBuilder.append(requestId);
                stringBuilder.append(": ");
                stringBuilder.append(structure);
                Slog.v(str, stringBuilder.toString());
            }
            if (Session.this.mHwAutofillHelper == null || !Session.this.mHwAutofillHelper.shouldForbidFillRequest(Session.this.mClientState, Session.this.mService.getServicePackageName())) {
                FillRequest request;
                synchronized (Session.this.mLock) {
                    int i = 0;
                    try {
                        structure.ensureDataForAutofill();
                        ComponentName componentNameFromApp = structure.getActivityComponent();
                        if (componentNameFromApp == null || !Session.this.mComponentName.getPackageName().equals(componentNameFromApp.getPackageName())) {
                            Object obj;
                            String str2 = Session.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Activity ");
                            stringBuilder2.append(Session.this.mComponentName);
                            stringBuilder2.append(" forged different component on AssistStructure: ");
                            stringBuilder2.append(componentNameFromApp);
                            Slog.w(str2, stringBuilder2.toString());
                            structure.setActivityComponent(Session.this.mComponentName);
                            MetricsLogger access$700 = Session.this.mMetricsLogger;
                            LogMaker access$600 = Session.this.newLogMaker(948);
                            if (componentNameFromApp == null) {
                                obj = "null";
                            } else {
                                obj = componentNameFromApp.flattenToShortString();
                            }
                            access$700.write(access$600.addTaggedData(949, obj));
                        }
                        if (Session.this.mCompatMode) {
                            String[] urlBarIds = Session.this.mService.getUrlBarResourceIdsForCompatMode(Session.this.mComponentName.getPackageName());
                            if (Helper.sDebug) {
                                String str3 = Session.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("url_bars in compat mode: ");
                                stringBuilder3.append(Arrays.toString(urlBarIds));
                                Slog.d(str3, stringBuilder3.toString());
                            }
                            if (urlBarIds != null) {
                                Session.this.mUrlBar = Helper.sanitizeUrlBar(structure, urlBarIds);
                                if (Session.this.mUrlBar != null) {
                                    AutofillId urlBarId = Session.this.mUrlBar.getAutofillId();
                                    if (Helper.sDebug) {
                                        String str4 = Session.TAG;
                                        StringBuilder stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("Setting urlBar as id=");
                                        stringBuilder4.append(urlBarId);
                                        stringBuilder4.append(" and domain ");
                                        stringBuilder4.append(Session.this.mUrlBar.getWebDomain());
                                        Slog.d(str4, stringBuilder4.toString());
                                    }
                                    Session.this.mViewStates.put(urlBarId, new ViewState(Session.this, urlBarId, Session.this, 512));
                                }
                            }
                        }
                        structure.sanitizeForParceling(true);
                        int flags = structure.getFlags();
                        if (Session.this.mContexts == null) {
                            Session.this.mContexts = new ArrayList(1);
                        }
                        Session.this.mContexts.add(new FillContext(requestId, structure));
                        Session.this.cancelCurrentRequestLocked();
                        int numContexts = Session.this.mContexts.size();
                        while (i < numContexts) {
                            Session.this.fillContextWithAllowedValuesLocked((FillContext) Session.this.mContexts.get(i), flags);
                            i++;
                        }
                        request = new FillRequest(requestId, new ArrayList(Session.this.mContexts), Session.this.mClientState, flags);
                    } catch (RuntimeException e) {
                        Session.this.wtf(e, "Exception lazy loading assist structure for %s: %s", structure.getActivityComponent(), e);
                        return;
                    }
                }
                Session.this.mRemoteFillService.onFillRequest(request);
            }
        }

        public void onHandleAssistScreenshot(Bitmap screenshot) {
        }
    };
    @GuardedBy("mLock")
    private IAutoFillManagerClient mClient;
    @GuardedBy("mLock")
    private Bundle mClientState;
    @GuardedBy("mLock")
    private DeathRecipient mClientVulture;
    private final boolean mCompatMode;
    private final ComponentName mComponentName;
    @GuardedBy("mLock")
    private ArrayList<FillContext> mContexts;
    @GuardedBy("mLock")
    private AutofillId mCurrentViewId;
    @GuardedBy("mLock")
    private boolean mDestroyed;
    public final int mFlags;
    private final Handler mHandler;
    private boolean mHasCallback;
    private IHwAutofillHelper mHwAutofillHelper = HwFrameworkFactory.getHwAutofillHelper();
    @GuardedBy("mLock")
    private boolean mIsSaving;
    private final Object mLock;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    @GuardedBy("mLock")
    private PendingUi mPendingSaveUi;
    private final RemoteFillService mRemoteFillService;
    @GuardedBy("mLock")
    private final SparseArray<LogMaker> mRequestLogs = new SparseArray(1);
    @GuardedBy("mLock")
    private SparseArray<FillResponse> mResponses;
    @GuardedBy("mLock")
    private boolean mSaveOnAllViewsInvisible;
    @GuardedBy("mLock")
    private ArrayList<String> mSelectedDatasetIds;
    private final AutofillManagerServiceImpl mService;
    private final long mStartTime;
    private final AutoFillUI mUi;
    @GuardedBy("mLock")
    private final LocalLog mUiLatencyHistory;
    @GuardedBy("mLock")
    private long mUiShownTime;
    @GuardedBy("mLock")
    private ViewNode mUrlBar;
    @GuardedBy("mLock")
    private final ArrayMap<AutofillId, ViewState> mViewStates = new ArrayMap();
    @GuardedBy("mLock")
    private final LocalLog mWtfHistory;
    public final int uid;

    @GuardedBy("mLock")
    private AutofillId[] getIdsOfAllViewStatesLocked() {
        int numViewState = this.mViewStates.size();
        AutofillId[] ids = new AutofillId[numViewState];
        for (int i = 0; i < numViewState; i++) {
            ids[i] = ((ViewState) this.mViewStates.valueAt(i)).id;
        }
        return ids;
    }

    /* JADX WARNING: Missing block: B:18:0x0034, code:
            return r2;
     */
    /* JADX WARNING: Missing block: B:21:0x004c, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String findByAutofillId(AutofillId id) {
        synchronized (this.mLock) {
            AutofillValue value = findValueLocked(id);
            String str = null;
            if (value != null) {
                if (value.isText()) {
                    str = value.getTextValue().toString();
                    return str;
                } else if (value.isList()) {
                    CharSequence[] options = getAutofillOptionsFromContextsLocked(id);
                    if (options != null) {
                        CharSequence option = options[value.getListValue()];
                        if (option != null) {
                            str = option.toString();
                        }
                    } else {
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("findByAutofillId(): no autofill options for id ");
                        stringBuilder.append(id);
                        Slog.w(str2, stringBuilder.toString());
                    }
                }
            }
        }
    }

    public AutofillValue findRawValueByAutofillId(AutofillId id) {
        AutofillValue findValueLocked;
        synchronized (this.mLock) {
            findValueLocked = findValueLocked(id);
        }
        return findValueLocked;
    }

    @GuardedBy("mLock")
    private AutofillValue findValueLocked(AutofillId id) {
        ViewState state = (ViewState) this.mViewStates.get(id);
        if (state == null) {
            if (Helper.sDebug) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("findValueLocked(): no view state for ");
                stringBuilder.append(id);
                Slog.d(str, stringBuilder.toString());
            }
            return null;
        }
        AutofillValue value = state.getCurrentValue();
        if (value == null) {
            if (Helper.sDebug) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("findValueLocked(): no current value for ");
                stringBuilder2.append(id);
                Slog.d(str2, stringBuilder2.toString());
            }
            value = getValueFromContextsLocked(id);
        }
        return value;
    }

    @GuardedBy("mLock")
    private void fillContextWithAllowedValuesLocked(FillContext fillContext, int flags) {
        ViewNode[] nodes = fillContext.findViewNodesByAutofillIds(getIdsOfAllViewStatesLocked());
        int numViewState = this.mViewStates.size();
        for (int i = 0; i < numViewState; i++) {
            ViewState viewState = (ViewState) this.mViewStates.valueAt(i);
            ViewNode node = nodes[i];
            if (node != null) {
                AutofillValue currentValue = viewState.getCurrentValue();
                AutofillValue filledValue = viewState.getAutofilledValue();
                AutofillOverlay overlay = new AutofillOverlay();
                if (filledValue != null && filledValue.equals(currentValue)) {
                    overlay.value = currentValue;
                }
                if (this.mCurrentViewId != null) {
                    overlay.focused = this.mCurrentViewId.equals(viewState.id);
                    if (overlay.focused && (flags & 1) != 0) {
                        overlay.value = currentValue;
                    }
                }
                node.setAutofillOverlay(overlay);
            } else if (Helper.sVerbose) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fillContextWithAllowedValuesLocked(): no node for ");
                stringBuilder.append(viewState.id);
                Slog.v(str, stringBuilder.toString());
            }
        }
    }

    @GuardedBy("mLock")
    private void cancelCurrentRequestLocked() {
        int canceledRequest = this.mRemoteFillService.cancelCurrentRequest();
        if (canceledRequest != Integer.MIN_VALUE && this.mContexts != null) {
            for (int i = this.mContexts.size() - 1; i >= 0; i--) {
                if (((FillContext) this.mContexts.get(i)).getRequestId() == canceledRequest) {
                    if (Helper.sDebug) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("cancelCurrentRequest(): id = ");
                        stringBuilder.append(canceledRequest);
                        Slog.d(str, stringBuilder.toString());
                    }
                    this.mContexts.remove(i);
                    return;
                }
            }
        }
    }

    @GuardedBy("mLock")
    private void requestNewFillResponseLocked(int flags) {
        int requestId;
        do {
            requestId = sIdCounter.getAndIncrement();
        } while (requestId == Integer.MIN_VALUE);
        int ordinal = this.mRequestLogs.size() + 1;
        LogMaker log = newLogMaker(907).addTaggedData(1454, Integer.valueOf(ordinal));
        if (flags != 0) {
            log.addTaggedData(1452, Integer.valueOf(flags));
        }
        this.mRequestLogs.put(requestId, log);
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Requesting structure for request #");
            stringBuilder.append(ordinal);
            stringBuilder.append(" ,requestId=");
            stringBuilder.append(requestId);
            stringBuilder.append(", flags=");
            stringBuilder.append(flags);
            Slog.v(str, stringBuilder.toString());
        }
        cancelCurrentRequestLocked();
        long identity;
        try {
            Bundle receiverExtras = new Bundle();
            receiverExtras.putInt(EXTRA_REQUEST_ID, requestId);
            identity = Binder.clearCallingIdentity();
            if (!ActivityManager.getService().requestAutofillData(this.mAssistReceiver, receiverExtras, this.mActivityToken, flags)) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("failed to request autofill data for ");
                stringBuilder2.append(this.mActivityToken);
                Slog.w(str2, stringBuilder2.toString());
            }
            Binder.restoreCallingIdentity(identity);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    Session(AutofillManagerServiceImpl service, AutoFillUI ui, Context context, Handler handler, int userId, Object lock, int sessionId, int uid, IBinder activityToken, IBinder client, boolean hasCallback, LocalLog uiLatencyHistory, LocalLog wtfHistory, ComponentName serviceComponentName, ComponentName componentName, boolean compatMode, boolean bindInstantServiceAllowed, int flags) {
        this.id = sessionId;
        this.mFlags = flags;
        this.uid = uid;
        this.mStartTime = SystemClock.elapsedRealtime();
        this.mService = service;
        this.mLock = lock;
        this.mUi = ui;
        this.mHandler = handler;
        this.mRemoteFillService = new RemoteFillService(context, serviceComponentName, userId, this, bindInstantServiceAllowed);
        this.mActivityToken = activityToken;
        this.mHasCallback = hasCallback;
        this.mUiLatencyHistory = uiLatencyHistory;
        this.mWtfHistory = wtfHistory;
        this.mComponentName = componentName;
        this.mCompatMode = compatMode;
        setClientLocked(client);
        this.mMetricsLogger.write(newLogMaker(906).addTaggedData(1452, Integer.valueOf(flags)));
    }

    @GuardedBy("mLock")
    IBinder getActivityTokenLocked() {
        return this.mActivityToken;
    }

    void switchActivity(IBinder newActivity, IBinder newClient) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#switchActivity() rejected - session: ");
                stringBuilder.append(this.id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
                return;
            }
            this.mActivityToken = newActivity;
            setClientLocked(newClient);
            updateTrackedIdsLocked();
        }
    }

    @GuardedBy("mLock")
    private void setClientLocked(IBinder client) {
        unlinkClientVultureLocked();
        this.mClient = IAutoFillManagerClient.Stub.asInterface(client);
        this.mClientVulture = new -$$Lambda$Session$xw4trZ-LA7gCvZvpKJ93vf377ak(this);
        try {
            this.mClient.asBinder().linkToDeath(this.mClientVulture, 0);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("could not set binder death listener on autofill client: ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
        }
    }

    public static /* synthetic */ void lambda$setClientLocked$0(Session session) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handling death of ");
        stringBuilder.append(session.mActivityToken);
        stringBuilder.append(" when saving=");
        stringBuilder.append(session.mIsSaving);
        Slog.d(str, stringBuilder.toString());
        synchronized (session.mLock) {
            if (session.mIsSaving) {
                session.mUi.hideFillUi(session);
            } else {
                session.mUi.destroyAll(session.mPendingSaveUi, session, false);
            }
        }
    }

    @GuardedBy("mLock")
    private void unlinkClientVultureLocked() {
        if (this.mClient != null && this.mClientVulture != null && !this.mClient.asBinder().unlinkToDeath(this.mClientVulture, 0)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unlinking vulture from death failed for ");
            stringBuilder.append(this.mActivityToken);
            Slog.w(str, stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:27:0x0097, code:
            r8 = r0;
            r1.mService.setLastResponse(r1.id, r3);
            r14 = r23.getDisableDuration();
     */
    /* JADX WARNING: Missing block: B:28:0x00a8, code:
            if (r14 <= 0) goto L_0x0105;
     */
    /* JADX WARNING: Missing block: B:29:0x00aa, code:
            r5 = r23.getFlags();
     */
    /* JADX WARNING: Missing block: B:30:0x00b0, code:
            if (com.android.server.autofill.Helper.sDebug == false) goto L_0x00d8;
     */
    /* JADX WARNING: Missing block: B:31:0x00b2, code:
            r9 = new java.lang.StringBuilder("Service disabled autofill for ");
            r9.append(r1.mComponentName);
            r9.append(": flags=");
            r9.append(r5);
            r9 = r9.append(", duration=");
            android.util.TimeUtils.formatDuration(r14, r9);
            android.util.Slog.d(TAG, r9.toString());
     */
    /* JADX WARNING: Missing block: B:33:0x00da, code:
            if ((r5 & 2) == 0) goto L_0x00ef;
     */
    /* JADX WARNING: Missing block: B:34:0x00dc, code:
            r19 = r14;
            r1.mService.disableAutofillForActivity(r1.mComponentName, r14, r1.id, r1.mCompatMode);
     */
    /* JADX WARNING: Missing block: B:35:0x00ef, code:
            r19 = r14;
            r1.mService.disableAutofillForApp(r1.mComponentName.getPackageName(), r19, r1.id, r1.mCompatMode);
     */
    /* JADX WARNING: Missing block: B:36:0x0102, code:
            r9 = 4;
     */
    /* JADX WARNING: Missing block: B:37:0x0105, code:
            r19 = r14;
            r9 = 0;
     */
    /* JADX WARNING: Missing block: B:39:0x010c, code:
            if (r23.getDatasets() == null) goto L_0x0118;
     */
    /* JADX WARNING: Missing block: B:41:0x0116, code:
            if (r23.getDatasets().isEmpty() == false) goto L_0x011e;
     */
    /* JADX WARNING: Missing block: B:43:0x011c, code:
            if (r23.getAuthentication() == null) goto L_0x0122;
     */
    /* JADX WARNING: Missing block: B:45:0x0120, code:
            if (r19 <= 0) goto L_0x0125;
     */
    /* JADX WARNING: Missing block: B:46:0x0122, code:
            notifyUnavailableToClient(r9);
     */
    /* JADX WARNING: Missing block: B:47:0x0125, code:
            if (r8 == null) goto L_0x014b;
     */
    /* JADX WARNING: Missing block: B:49:0x012c, code:
            if (r23.getDatasets() != null) goto L_0x0130;
     */
    /* JADX WARNING: Missing block: B:50:0x012e, code:
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:51:0x0130, code:
            r0 = r23.getDatasets().size();
     */
    /* JADX WARNING: Missing block: B:52:0x0138, code:
            r8.addTaggedData(909, java.lang.Integer.valueOf(r0));
     */
    /* JADX WARNING: Missing block: B:53:0x013f, code:
            if (r7 == null) goto L_0x014b;
     */
    /* JADX WARNING: Missing block: B:54:0x0141, code:
            r8.addTaggedData(1271, java.lang.Integer.valueOf(r7.length));
     */
    /* JADX WARNING: Missing block: B:55:0x014b, code:
            r6 = r1.mLock;
     */
    /* JADX WARNING: Missing block: B:56:0x014d, code:
            monitor-enter(r6);
     */
    /* JADX WARNING: Missing block: B:59:?, code:
            processResponseLocked(r3, null, r4);
     */
    /* JADX WARNING: Missing block: B:60:0x0152, code:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:61:0x0153, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onFillRequestSuccess(int requestId, FillResponse response, String servicePackageName, int requestFlags) {
        int i = requestId;
        FillResponse fillResponse = response;
        int i2 = requestFlags;
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#onFillRequestSuccess() rejected - session: ");
                stringBuilder.append(this.id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
                return;
            }
            String str2;
            LogMaker requestLog = (LogMaker) this.mRequestLogs.get(i);
            if (requestLog != null) {
                requestLog.setType(10);
            } else {
                str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onFillRequestSuccess(): no request log for id ");
                stringBuilder2.append(i);
                Slog.w(str2, stringBuilder2.toString());
            }
            if (fillResponse == null) {
                if (requestLog != null) {
                    requestLog.addTaggedData(909, Integer.valueOf(-1));
                }
                processNullResponseLocked(i2);
                return;
            }
            AutofillId[] fieldClassificationIds = response.getFieldClassificationIds();
            if (fieldClassificationIds == null || this.mService.isFieldClassificationEnabledLocked()) {
            } else {
                str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Ignoring ");
                stringBuilder3.append(fillResponse);
                stringBuilder3.append(" because field detection is disabled");
                Slog.w(str2, stringBuilder3.toString());
                processNullResponseLocked(i2);
            }
        }
    }

    public void onFillRequestFailure(int requestId, CharSequence message, String servicePackageName) {
        onFillRequestFailureOrTimeout(requestId, false, message, servicePackageName);
    }

    public void onFillRequestTimeout(int requestId, String servicePackageName) {
        onFillRequestFailureOrTimeout(requestId, true, null, servicePackageName);
    }

    /* JADX WARNING: Missing block: B:16:0x005f, code:
            if (r8 == null) goto L_0x0068;
     */
    /* JADX WARNING: Missing block: B:17:0x0061, code:
            getUiForShowing().showError(r8, (com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback) r5);
     */
    /* JADX WARNING: Missing block: B:18:0x0068, code:
            removeSelf();
     */
    /* JADX WARNING: Missing block: B:19:0x006b, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onFillRequestFailureOrTimeout(int requestId, boolean timedOut, CharSequence message, String servicePackageName) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#onFillRequestFailureOrTimeout(req=");
                stringBuilder.append(requestId);
                stringBuilder.append(") rejected - session: ");
                stringBuilder.append(this.id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
                return;
            }
            this.mService.resetLastResponse();
            LogMaker requestLog = (LogMaker) this.mRequestLogs.get(requestId);
            if (requestLog == null) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onFillRequestFailureOrTimeout(): no log for id ");
                stringBuilder2.append(requestId);
                Slog.w(str2, stringBuilder2.toString());
            } else {
                requestLog.setType(timedOut ? 2 : 11);
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x002a, code:
            r0 = newLogMaker(918, r5);
     */
    /* JADX WARNING: Missing block: B:11:0x0030, code:
            if (r6 != null) goto L_0x0035;
     */
    /* JADX WARNING: Missing block: B:12:0x0032, code:
            r1 = 10;
     */
    /* JADX WARNING: Missing block: B:13:0x0035, code:
            r1 = 1;
     */
    /* JADX WARNING: Missing block: B:14:0x0036, code:
            r4.mMetricsLogger.write(r0.setType(r1));
     */
    /* JADX WARNING: Missing block: B:15:0x003f, code:
            if (r6 == null) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:17:0x0043, code:
            if (com.android.server.autofill.Helper.sDebug == false) goto L_0x004c;
     */
    /* JADX WARNING: Missing block: B:18:0x0045, code:
            android.util.Slog.d(TAG, "Starting intent sender on save()");
     */
    /* JADX WARNING: Missing block: B:19:0x004c, code:
            startIntentSender(r6);
     */
    /* JADX WARNING: Missing block: B:20:0x004f, code:
            removeSelf();
     */
    /* JADX WARNING: Missing block: B:21:0x0052, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onSaveRequestSuccess(String servicePackageName, IntentSender intentSender) {
        synchronized (this.mLock) {
            this.mIsSaving = false;
            if (this.mDestroyed) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#onSaveRequestSuccess() rejected - session: ");
                stringBuilder.append(this.id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
            }
        }
    }

    public void onSaveRequestFailure(CharSequence message, String servicePackageName) {
        synchronized (this.mLock) {
            this.mIsSaving = false;
            if (this.mDestroyed) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#onSaveRequestFailure() rejected - session: ");
                stringBuilder.append(this.id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
                return;
            }
            this.mMetricsLogger.write(newLogMaker(918, servicePackageName).setType(11));
            getUiForShowing().showError(message, (AutoFillUiCallback) this);
            removeSelf();
        }
    }

    @GuardedBy("mLock")
    private FillContext getFillContextByRequestIdLocked(int requestId) {
        if (this.mContexts == null) {
            return null;
        }
        int numContexts = this.mContexts.size();
        for (int i = 0; i < numContexts; i++) {
            FillContext context = (FillContext) this.mContexts.get(i);
            if (context.getRequestId() == requestId) {
                return context;
            }
        }
        return null;
    }

    public void authenticate(int requestId, int datasetIndex, IntentSender intent, Bundle extras) {
        if (Helper.sDebug) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("authenticate(): requestId=");
            stringBuilder.append(requestId);
            stringBuilder.append("; datasetIdx=");
            stringBuilder.append(datasetIndex);
            stringBuilder.append("; intentSender=");
            stringBuilder.append(intent);
            Slog.d(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Call to Session#authenticate() rejected - session: ");
                stringBuilder2.append(this.id);
                stringBuilder2.append(" destroyed");
                Slog.w(str2, stringBuilder2.toString());
                return;
            }
            Intent fillInIntent = createAuthFillInIntentLocked(requestId, extras);
            if (fillInIntent == null) {
                forceRemoveSelfLocked();
                return;
            }
            this.mService.setAuthenticationSelected(this.id, this.mClientState);
            this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$Session$LM4xf4dbxH_NTutQzBkaQNxKbV0.INSTANCE, this, Integer.valueOf(AutofillManager.makeAuthenticationId(requestId, datasetIndex)), intent, fillInIntent));
        }
    }

    public void onServiceDied(RemoteFillService service) {
    }

    public void fill(int requestId, int datasetIndex, Dataset dataset) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#fill() rejected - session: ");
                stringBuilder.append(this.id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
                return;
            }
            this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$knR7oLyPSG_CoFAxBA_nqSw3JBo.INSTANCE, this, Integer.valueOf(requestId), Integer.valueOf(datasetIndex), dataset, Boolean.valueOf(true)));
        }
    }

    public void save() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#save() rejected - session: ");
                stringBuilder.append(this.id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
                return;
            }
            this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$Z6K-VL097A8ARGd4URY-lOvvM48.INSTANCE, this.mService, this));
        }
    }

    public void cancelSave() {
        synchronized (this.mLock) {
            this.mIsSaving = false;
            if (this.mDestroyed) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#cancelSave() rejected - session: ");
                stringBuilder.append(this.id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
                return;
            }
            this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$Session$cYu1t6lYVopApYW-vct82-7slZk.INSTANCE, this));
        }
    }

    public void requestShowFillUi(AutofillId id, int width, int height, IAutofillWindowPresenter presenter) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (this.mDestroyed) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#requestShowFillUi() rejected - session: ");
                stringBuilder.append(id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
                return;
            } else if (id.equals(this.mCurrentViewId)) {
                try {
                    AutofillId autofillId = id;
                    int i = width;
                    int i2 = height;
                    this.mClient.requestShowFillUi(this.id, autofillId, i, i2, ((ViewState) this.mViewStates.get(id)).getVirtualBounds(), presenter);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error requesting to show fill UI", e);
                }
            } else if (Helper.sDebug) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Do not show full UI on ");
                stringBuilder.append(id);
                stringBuilder.append(" as it is not the current view (");
                stringBuilder.append(this.mCurrentViewId);
                stringBuilder.append(") anymore");
                Slog.d(str, stringBuilder.toString());
            }
        }
    }

    public void dispatchUnhandledKey(AutofillId id, KeyEvent keyEvent) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (this.mDestroyed) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#dispatchUnhandledKey() rejected - session: ");
                stringBuilder.append(id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
                return;
            } else if (id.equals(this.mCurrentViewId)) {
                try {
                    this.mClient.dispatchUnhandledKey(this.id, id, keyEvent);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error requesting to dispatch unhandled key", e);
                }
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Do not dispatch unhandled key on ");
                stringBuilder.append(id);
                stringBuilder.append(" as it is not the current view (");
                stringBuilder.append(this.mCurrentViewId);
                stringBuilder.append(") anymore");
                Slog.w(str, stringBuilder.toString());
            }
        }
    }

    public void requestHideFillUi(AutofillId id) {
        synchronized (this.mLock) {
            try {
                this.mClient.requestHideFillUi(this.id, id);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error requesting to hide fill UI", e);
            }
        }
    }

    public void startIntentSender(IntentSender intentSender) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#startIntentSender() rejected - session: ");
                stringBuilder.append(this.id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
                return;
            }
            removeSelfLocked();
            this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$Session$dldcS_opIdRI25w0DM6rSIaHIoc.INSTANCE, this, intentSender));
        }
    }

    private void doStartIntentSender(IntentSender intentSender) {
        try {
            synchronized (this.mLock) {
                this.mClient.startIntentSender(intentSender, null);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Error launching auth intent", e);
        }
    }

    @GuardedBy("mLock")
    void setAuthenticationResultLocked(Bundle data, int authenticationId) {
        String str;
        StringBuilder stringBuilder;
        if (this.mDestroyed) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Call to Session#setAuthenticationResultLocked() rejected - session: ");
            stringBuilder.append(this.id);
            stringBuilder.append(" destroyed");
            Slog.w(str, stringBuilder.toString());
        } else if (this.mResponses == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setAuthenticationResultLocked(");
            stringBuilder.append(authenticationId);
            stringBuilder.append("): no responses");
            Slog.w(str, stringBuilder.toString());
            removeSelf();
        } else {
            int requestId = AutofillManager.getRequestIdFromAuthenticationId(authenticationId);
            FillResponse authenticatedResponse = (FillResponse) this.mResponses.get(requestId);
            if (authenticatedResponse == null || data == null) {
                removeSelf();
                return;
            }
            int datasetIdx = AutofillManager.getDatasetIdFromAuthenticationId(authenticationId);
            if (datasetIdx == NetworkConstants.ARP_HWTYPE_RESERVED_HI || ((Dataset) authenticatedResponse.getDatasets().get(datasetIdx)) != null) {
                Parcelable result = data.getParcelable("android.view.autofill.extra.AUTHENTICATION_RESULT");
                Bundle newClientState = data.getBundle("android.view.autofill.extra.CLIENT_STATE");
                if (Helper.sDebug) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setAuthenticationResultLocked(): result=");
                    stringBuilder2.append(result);
                    stringBuilder2.append(", clientState=");
                    stringBuilder2.append(newClientState);
                    Slog.d(str2, stringBuilder2.toString());
                }
                if (result instanceof FillResponse) {
                    logAuthenticationStatusLocked(requestId, 912);
                    replaceResponseLocked(authenticatedResponse, (FillResponse) result, newClientState);
                } else if (!(result instanceof Dataset)) {
                    if (result != null) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("service returned invalid auth type: ");
                        stringBuilder3.append(result);
                        Slog.w(str3, stringBuilder3.toString());
                    }
                    logAuthenticationStatusLocked(requestId, 1128);
                    processNullResponseLocked(0);
                } else if (datasetIdx != NetworkConstants.ARP_HWTYPE_RESERVED_HI) {
                    logAuthenticationStatusLocked(requestId, 1126);
                    if (newClientState != null) {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "Updating client state from auth dataset");
                        }
                        this.mClientState = newClientState;
                    }
                    Dataset dataset = (Dataset) result;
                    authenticatedResponse.getDatasets().set(datasetIdx, dataset);
                    autoFill(requestId, datasetIdx, dataset, false);
                } else {
                    logAuthenticationStatusLocked(requestId, 1127);
                }
                return;
            }
            removeSelf();
        }
    }

    @GuardedBy("mLock")
    void setHasCallbackLocked(boolean hasIt) {
        if (this.mDestroyed) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Call to Session#setHasCallbackLocked() rejected - session: ");
            stringBuilder.append(this.id);
            stringBuilder.append(" destroyed");
            Slog.w(str, stringBuilder.toString());
            return;
        }
        this.mHasCallback = hasIt;
    }

    @GuardedBy("mLock")
    private FillResponse getLastResponseLocked(String logPrefix) {
        String str;
        StringBuilder stringBuilder;
        if (this.mContexts == null) {
            if (Helper.sDebug && logPrefix != null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(logPrefix);
                stringBuilder.append(": no contexts");
                Slog.d(str, stringBuilder.toString());
            }
            return null;
        } else if (this.mResponses == null) {
            if (Helper.sVerbose && logPrefix != null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(logPrefix);
                stringBuilder.append(": no responses on session");
                Slog.v(str, stringBuilder.toString());
            }
            return null;
        } else {
            int lastResponseIdx = getLastResponseIndexLocked();
            String str2;
            StringBuilder stringBuilder2;
            if (lastResponseIdx < 0) {
                if (logPrefix != null) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(logPrefix);
                    stringBuilder2.append(": did not get last response. mResponses=");
                    stringBuilder2.append(this.mResponses);
                    stringBuilder2.append(", mViewStates=");
                    stringBuilder2.append(this.mViewStates);
                    Slog.w(str2, stringBuilder2.toString());
                }
                return null;
            }
            FillResponse response = (FillResponse) this.mResponses.valueAt(lastResponseIdx);
            if (Helper.sVerbose && logPrefix != null) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(logPrefix);
                stringBuilder2.append(": mResponses=");
                stringBuilder2.append(this.mResponses);
                stringBuilder2.append(", mContexts=");
                stringBuilder2.append(this.mContexts);
                stringBuilder2.append(", mViewStates=");
                stringBuilder2.append(this.mViewStates);
                Slog.v(str2, stringBuilder2.toString());
            }
            return response;
        }
    }

    @GuardedBy("mLock")
    private SaveInfo getSaveInfoLocked() {
        FillResponse response = getLastResponseLocked(null);
        if (response == null) {
            return null;
        }
        return response.getSaveInfo();
    }

    public void logContextCommitted() {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$Session$0VAc60LP16186Azy3Ov7dL7BsAE.INSTANCE, this));
    }

    private void doLogContextCommitted() {
        synchronized (this.mLock) {
            logContextCommittedLocked();
        }
    }

    @GuardedBy("mLock")
    private void logContextCommittedLocked() {
        FillResponse lastResponse = getLastResponseLocked("logContextCommited()");
        if (lastResponse != null) {
            int flags = lastResponse.getFlags();
            if ((flags & 1) == 0) {
                if (Helper.sVerbose) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("logContextCommittedLocked(): ignored by flags ");
                    stringBuilder.append(flags);
                    Slog.v(str, stringBuilder.toString());
                }
                return;
            }
            ArrayList<AutofillId> changedFieldIds;
            ArrayList<String> changedDatasetIds;
            String str2;
            StringBuilder stringBuilder2;
            String str3;
            ArrayList<AutofillId> changedFieldIds2 = null;
            ArrayList<String> changedDatasetIds2 = null;
            int responseCount = this.mResponses.size();
            boolean hasAtLeastOneDataset = false;
            ArraySet<String> ignoredDatasets = null;
            int i = 0;
            while (i < responseCount) {
                List<Dataset> datasets = ((FillResponse) this.mResponses.valueAt(i)).getDatasets();
                if (datasets == null) {
                    changedFieldIds = changedFieldIds2;
                    changedDatasetIds = changedDatasetIds2;
                } else if (datasets.isEmpty()) {
                    changedFieldIds = changedFieldIds2;
                    changedDatasetIds = changedDatasetIds2;
                } else {
                    ArraySet<String> ignoredDatasets2 = ignoredDatasets;
                    int j = 0;
                    while (j < datasets.size()) {
                        Dataset dataset = (Dataset) datasets.get(j);
                        String datasetId = dataset.getId();
                        if (datasetId != null) {
                            changedFieldIds = changedFieldIds2;
                            changedDatasetIds = changedDatasetIds2;
                            if (this.mSelectedDatasetIds == null || !this.mSelectedDatasetIds.contains(datasetId)) {
                                if (Helper.sVerbose) {
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("adding ignored dataset ");
                                    stringBuilder2.append(datasetId);
                                    Slog.v(str2, stringBuilder2.toString());
                                }
                                if (ignoredDatasets2 == null) {
                                    ignoredDatasets2 = new ArraySet();
                                }
                                ignoredDatasets2.add(datasetId);
                            }
                            hasAtLeastOneDataset = true;
                        } else if (Helper.sVerbose) {
                            str3 = TAG;
                            changedFieldIds = changedFieldIds2;
                            changedFieldIds2 = new StringBuilder();
                            changedDatasetIds = changedDatasetIds2;
                            changedFieldIds2.append("logContextCommitted() skipping idless dataset ");
                            changedFieldIds2.append(dataset);
                            Slog.v(str3, changedFieldIds2.toString());
                        } else {
                            changedFieldIds = changedFieldIds2;
                            changedDatasetIds = changedDatasetIds2;
                        }
                        j++;
                        changedFieldIds2 = changedFieldIds;
                        changedDatasetIds2 = changedDatasetIds;
                    }
                    changedFieldIds = changedFieldIds2;
                    changedDatasetIds = changedDatasetIds2;
                    ignoredDatasets = ignoredDatasets2;
                    i++;
                    changedFieldIds2 = changedFieldIds;
                    changedDatasetIds2 = changedDatasetIds;
                }
                if (Helper.sVerbose) {
                    String str4 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("logContextCommitted() no datasets at ");
                    stringBuilder3.append(i);
                    Slog.v(str4, stringBuilder3.toString());
                }
                i++;
                changedFieldIds2 = changedFieldIds;
                changedDatasetIds2 = changedDatasetIds;
            }
            changedFieldIds = changedFieldIds2;
            changedDatasetIds = changedDatasetIds2;
            AutofillId[] fieldClassificationIds = lastResponse.getFieldClassificationIds();
            if (hasAtLeastOneDataset || fieldClassificationIds != null) {
                int state;
                int j2;
                UserData userData = this.mService.getUserData();
                ArrayMap<AutofillId, ArraySet<String>> manuallyFilledIds = null;
                ArraySet<String> ignoredDatasets3 = ignoredDatasets;
                ArrayList<AutofillId> changedFieldIds3 = changedFieldIds;
                ArrayList<String> changedDatasetIds3 = changedDatasetIds;
                i = 0;
                while (i < this.mViewStates.size()) {
                    ViewState viewState = (ViewState) this.mViewStates.valueAt(i);
                    state = viewState.getState();
                    if ((state & 8) != 0) {
                        String datasetId2;
                        String str5;
                        if ((state & 4) != 0) {
                            datasetId2 = viewState.getDatasetId();
                            if (datasetId2 == null) {
                                str5 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("logContextCommitted(): no dataset id on ");
                                stringBuilder2.append(viewState);
                                Slog.w(str5, stringBuilder2.toString());
                            } else {
                                AutofillValue autofilledValue = viewState.getAutofilledValue();
                                AutofillValue currentValue = viewState.getCurrentValue();
                                if (autofilledValue == null || !autofilledValue.equals(currentValue)) {
                                    ArrayList<String> changedDatasetIds4;
                                    if (Helper.sDebug != 0) {
                                        state = TAG;
                                        changedDatasetIds4 = new StringBuilder();
                                        changedDatasetIds4.append("logContextCommitted() found changed state: ");
                                        changedDatasetIds4.append(viewState);
                                        Slog.d(state, changedDatasetIds4.toString());
                                    }
                                    if (changedFieldIds3 == null) {
                                        changedFieldIds3 = new ArrayList();
                                        changedDatasetIds3 = new ArrayList();
                                    }
                                    state = changedFieldIds3;
                                    changedDatasetIds4 = changedDatasetIds3;
                                    state.add(viewState.id);
                                    changedDatasetIds4.add(datasetId2);
                                    changedFieldIds3 = state;
                                    changedDatasetIds3 = changedDatasetIds4;
                                } else if (Helper.sDebug) {
                                    String str6 = TAG;
                                    StringBuilder stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("logContextCommitted(): ignoring changed ");
                                    stringBuilder4.append(viewState);
                                    stringBuilder4.append(" because it has same value that was autofilled");
                                    Slog.d(str6, stringBuilder4.toString());
                                }
                            }
                        } else {
                            int i2 = state;
                            AutofillValue currentValue2 = viewState.getCurrentValue();
                            StringBuilder stringBuilder5;
                            if (currentValue2 == null) {
                                if (Helper.sDebug) {
                                    datasetId2 = TAG;
                                    stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("logContextCommitted(): skipping view without current value ( ");
                                    stringBuilder5.append(viewState);
                                    stringBuilder5.append(")");
                                    Slog.d(datasetId2, stringBuilder5.toString());
                                }
                            } else if (hasAtLeastOneDataset) {
                                j2 = 0;
                                while (j2 < responseCount) {
                                    AutofillValue currentValue3;
                                    FillResponse lastResponse2;
                                    FillResponse response = (FillResponse) this.mResponses.valueAt(j2);
                                    List<Dataset> datasets2 = response.getDatasets();
                                    FillResponse fillResponse;
                                    List<Dataset> list;
                                    if (datasets2 == null) {
                                        currentValue3 = currentValue2;
                                        fillResponse = response;
                                        list = datasets2;
                                        lastResponse2 = lastResponse;
                                    } else if (datasets2.isEmpty()) {
                                        currentValue3 = currentValue2;
                                        fillResponse = response;
                                        list = datasets2;
                                        lastResponse2 = lastResponse;
                                    } else {
                                        ArrayMap<AutofillId, ArraySet<String>> manuallyFilledIds2;
                                        int k = 0;
                                        while (k < datasets2.size()) {
                                            Dataset dataset2 = (Dataset) datasets2.get(k);
                                            fillResponse = response;
                                            str5 = dataset2.getId();
                                            if (str5 == null) {
                                                if (Helper.sVerbose) {
                                                    list = datasets2;
                                                    str3 = TAG;
                                                    manuallyFilledIds2 = manuallyFilledIds;
                                                    StringBuilder stringBuilder6 = new StringBuilder();
                                                    lastResponse2 = lastResponse;
                                                    stringBuilder6.append("logContextCommitted() skipping idless dataset ");
                                                    stringBuilder6.append(dataset2);
                                                    Slog.v(str3, stringBuilder6.toString());
                                                } else {
                                                    list = datasets2;
                                                    manuallyFilledIds2 = manuallyFilledIds;
                                                    lastResponse2 = lastResponse;
                                                }
                                                currentValue3 = currentValue2;
                                            } else {
                                                ArrayList<AutofillValue> values;
                                                Dataset dataset3;
                                                list = datasets2;
                                                manuallyFilledIds2 = manuallyFilledIds;
                                                lastResponse2 = lastResponse;
                                                ArrayList<AutofillValue> values2 = dataset2.getFieldValues();
                                                manuallyFilledIds = null;
                                                while (manuallyFilledIds < values2.size()) {
                                                    if (currentValue2.equals((AutofillValue) values2.get(manuallyFilledIds))) {
                                                        if (Helper.sDebug) {
                                                            currentValue3 = currentValue2;
                                                            currentValue2 = TAG;
                                                            values = values2;
                                                            stringBuilder2 = new StringBuilder();
                                                            dataset3 = dataset2;
                                                            stringBuilder2.append("field ");
                                                            stringBuilder2.append(viewState.id);
                                                            stringBuilder2.append(" was manually filled with value set by dataset ");
                                                            stringBuilder2.append(str5);
                                                            Slog.d(currentValue2, stringBuilder2.toString());
                                                        } else {
                                                            currentValue3 = currentValue2;
                                                            values = values2;
                                                            dataset3 = dataset2;
                                                        }
                                                        if (manuallyFilledIds2 == null) {
                                                            currentValue2 = new ArrayMap();
                                                            manuallyFilledIds2 = currentValue2;
                                                        } else {
                                                            currentValue2 = manuallyFilledIds2;
                                                        }
                                                        values2 = (ArraySet) currentValue2.get(viewState.id);
                                                        ArraySet<String> datasetIds;
                                                        if (values2 == null) {
                                                            datasetIds = values2;
                                                            values2 = new ArraySet(1);
                                                            currentValue2.put(viewState.id, values2);
                                                        } else {
                                                            datasetIds = values2;
                                                        }
                                                        values2.add(str5);
                                                        manuallyFilledIds2 = currentValue2;
                                                    } else {
                                                        currentValue3 = currentValue2;
                                                        values = values2;
                                                        dataset3 = dataset2;
                                                    }
                                                    manuallyFilledIds++;
                                                    currentValue2 = currentValue3;
                                                    values2 = values;
                                                    dataset2 = dataset3;
                                                }
                                                currentValue3 = currentValue2;
                                                values = values2;
                                                dataset3 = dataset2;
                                                if (this.mSelectedDatasetIds == null || this.mSelectedDatasetIds.contains(str5) == null) {
                                                    if (Helper.sVerbose != null) {
                                                        currentValue2 = TAG;
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("adding ignored dataset ");
                                                        stringBuilder2.append(str5);
                                                        Slog.v(currentValue2, stringBuilder2.toString());
                                                    }
                                                    if (ignoredDatasets3 == null) {
                                                        currentValue2 = new ArraySet();
                                                    } else {
                                                        currentValue2 = ignoredDatasets3;
                                                    }
                                                    currentValue2.add(str5);
                                                    ignoredDatasets3 = currentValue2;
                                                }
                                            }
                                            manuallyFilledIds = manuallyFilledIds2;
                                            k++;
                                            response = fillResponse;
                                            datasets2 = list;
                                            lastResponse = lastResponse2;
                                            currentValue2 = currentValue3;
                                        }
                                        currentValue3 = currentValue2;
                                        manuallyFilledIds2 = manuallyFilledIds;
                                        lastResponse2 = lastResponse;
                                        j2++;
                                        lastResponse = lastResponse2;
                                        currentValue2 = currentValue3;
                                    }
                                    if (Helper.sVerbose) {
                                        str2 = TAG;
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("logContextCommitted() no datasets at ");
                                        stringBuilder5.append(j2);
                                        Slog.v(str2, stringBuilder5.toString());
                                    }
                                    j2++;
                                    lastResponse = lastResponse2;
                                    currentValue2 = currentValue3;
                                }
                            }
                        }
                    }
                    i++;
                    lastResponse = lastResponse;
                }
                ArrayList<AutofillId> manuallyFilledFieldIds = null;
                ArrayList<ArrayList<String>> manuallyFilledDatasetIds = null;
                if (manuallyFilledIds != null) {
                    state = manuallyFilledIds.size();
                    manuallyFilledFieldIds = new ArrayList(state);
                    manuallyFilledDatasetIds = new ArrayList(state);
                    int i3 = 0;
                    while (true) {
                        j2 = i3;
                        if (j2 >= state) {
                            break;
                        }
                        ArraySet<String> datasetIds2 = (ArraySet) manuallyFilledIds.valueAt(j2);
                        manuallyFilledFieldIds.add((AutofillId) manuallyFilledIds.keyAt(j2));
                        manuallyFilledDatasetIds.add(new ArrayList(datasetIds2));
                        i3 = j2 + 1;
                    }
                }
                ArrayList<AutofillId> manuallyFilledFieldIds2 = manuallyFilledFieldIds;
                ArrayList<ArrayList<String>> manuallyFilledDatasetIds2 = manuallyFilledDatasetIds;
                FieldClassificationStrategy fcStrategy = this.mService.getFieldClassificationStrategy();
                if (userData == null || fcStrategy == null) {
                    this.mService.logContextCommittedLocked(this.id, this.mClientState, this.mSelectedDatasetIds, ignoredDatasets3, changedFieldIds3, changedDatasetIds3, manuallyFilledFieldIds2, manuallyFilledDatasetIds2, this.mComponentName, this.mCompatMode);
                } else {
                    logFieldClassificationScoreLocked(fcStrategy, ignoredDatasets3, changedFieldIds3, changedDatasetIds3, manuallyFilledFieldIds2, manuallyFilledDatasetIds2, userData, this.mViewStates.values());
                }
                return;
            }
            if (Helper.sVerbose) {
                Slog.v(TAG, "logContextCommittedLocked(): skipped (no datasets nor fields classification ids)");
            }
        }
    }

    private void logFieldClassificationScoreLocked(FieldClassificationStrategy fcStrategy, ArraySet<String> ignoredDatasets, ArrayList<AutofillId> changedFieldIds, ArrayList<String> changedDatasetIds, ArrayList<AutofillId> manuallyFilledFieldIds, ArrayList<ArrayList<String>> manuallyFilledDatasetIds, UserData userData, Collection<ViewState> viewStates) {
        String[] userValues = userData.getValues();
        String[] categoryIds = userData.getCategoryIds();
        if (userValues == null || categoryIds == null || userValues.length != categoryIds.length) {
            int idsLength = -1;
            int valuesLength = userValues == null ? -1 : userValues.length;
            if (categoryIds != null) {
                idsLength = categoryIds.length;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setScores(): user data mismatch: values.length = ");
            stringBuilder.append(valuesLength);
            stringBuilder.append(", ids.length = ");
            stringBuilder.append(idsLength);
            Slog.w(str, stringBuilder.toString());
            return;
        }
        int maxFieldsSize = UserData.getMaxFieldClassificationIdsSize();
        ArrayList<AutofillId> detectedFieldIds = new ArrayList(maxFieldsSize);
        ArrayList<FieldClassification> detectedFieldClassifications = new ArrayList(maxFieldsSize);
        String algorithm = userData.getFieldClassificationAlgorithm();
        Bundle algorithmArgs = userData.getAlgorithmArgs();
        int viewsSize = viewStates.size();
        AutofillId[] autofillIds = new AutofillId[viewsSize];
        ArrayList<AutofillValue> currentValues = new ArrayList(viewsSize);
        int k = 0;
        for (ViewState viewState : viewStates) {
            currentValues.add(viewState.getCurrentValue());
            int k2 = k + 1;
            autofillIds[k] = viewState.id;
            k = k2;
        }
        OnResultListener onResultListener = r0;
        ArrayList<AutofillValue> currentValues2 = currentValues;
        -$$Lambda$Session$mm9ZGBWriIznaZv8NlUB1a4AvJI -__lambda_session_mm9zgbwriiznazv8nlub1a4avji = new -$$Lambda$Session$mm9ZGBWriIznaZv8NlUB1a4AvJI(this, ignoredDatasets, changedFieldIds, changedDatasetIds, manuallyFilledFieldIds, manuallyFilledDatasetIds, viewsSize, autofillIds, userValues, categoryIds, detectedFieldIds, detectedFieldClassifications);
        fcStrategy.getScores(new RemoteCallback(onResultListener), algorithm, algorithmArgs, currentValues2, userValues);
    }

    public static /* synthetic */ void lambda$logFieldClassificationScoreLocked$1(Session session, ArraySet ignoredDatasets, ArrayList changedFieldIds, ArrayList changedDatasetIds, ArrayList manuallyFilledFieldIds, ArrayList manuallyFilledDatasetIds, int viewsSize, AutofillId[] autofillIds, String[] userValues, String[] categoryIds, ArrayList detectedFieldIds, ArrayList detectedFieldClassifications, Bundle result) {
        ArrayIndexOutOfBoundsException e;
        Session session2 = session;
        String[] strArr = userValues;
        Bundle bundle = result;
        if (bundle == null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "setFieldClassificationScore(): no results");
            }
            session2.mService.logContextCommittedLocked(session2.id, session2.mClientState, session2.mSelectedDatasetIds, ignoredDatasets, changedFieldIds, changedDatasetIds, manuallyFilledFieldIds, manuallyFilledDatasetIds, session2.mComponentName, session2.mCompatMode);
            return;
        }
        Scores scores = (Scores) bundle.getParcelable("scores");
        StringBuilder stringBuilder;
        if (scores == null) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("No field classification score on ");
            stringBuilder.append(bundle);
            Slog.w(str, stringBuilder.toString());
            return;
        }
        ArrayList arrayList;
        ArrayList arrayList2;
        int j = 0;
        int i = 0;
        while (i < viewsSize) {
            try {
                String str2;
                AutofillId autofillId = autofillIds[i];
                ArrayMap<String, Float> scoresByField = null;
                while (true) {
                    j = 0;
                    if (0 >= strArr.length) {
                        break;
                    }
                    String categoryId = categoryIds[0];
                    float score = scores.scores[i][0];
                    if (score > 0.0f) {
                        if (scoresByField == null) {
                            scoresByField = new ArrayMap(strArr.length);
                        }
                        Float currentScore = (Float) scoresByField.get(categoryId);
                        if (currentScore == null || currentScore.floatValue() <= score) {
                            if (Helper.sVerbose) {
                                str2 = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("adding score ");
                                stringBuilder.append(score);
                                stringBuilder.append(" at index ");
                                stringBuilder.append(0);
                                stringBuilder.append(" and id ");
                                stringBuilder.append(autofillId);
                                Slog.v(str2, stringBuilder.toString());
                            }
                            scoresByField.put(categoryId, Float.valueOf(score));
                        } else if (Helper.sVerbose) {
                            String str3 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("skipping score ");
                            stringBuilder.append(score);
                            stringBuilder.append(" because it's less than ");
                            stringBuilder.append(currentScore);
                            Slog.v(str3, stringBuilder.toString());
                        }
                    } else if (Helper.sVerbose) {
                        str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("skipping score 0 at index ");
                        stringBuilder.append(0);
                        stringBuilder.append(" and id ");
                        stringBuilder.append(autofillId);
                        Slog.v(str2, stringBuilder.toString());
                    }
                    j = 0 + 1;
                    strArr = userValues;
                }
                if (scoresByField == null) {
                    if (Helper.sVerbose) {
                        str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("no score for autofillId=");
                        stringBuilder.append(autofillId);
                        Slog.v(str2, stringBuilder.toString());
                    }
                    arrayList = detectedFieldIds;
                    arrayList2 = detectedFieldClassifications;
                } else {
                    ArrayList<Match> matches = new ArrayList(scoresByField.size());
                    j = 0;
                    while (j < scoresByField.size()) {
                        matches.add(new Match((String) scoresByField.keyAt(j), ((Float) scoresByField.valueAt(j)).floatValue()));
                        j++;
                    }
                    try {
                        detectedFieldIds.add(autofillId);
                        try {
                            detectedFieldClassifications.add(new FieldClassification(matches));
                        } catch (ArrayIndexOutOfBoundsException e2) {
                            e = e2;
                        }
                    } catch (ArrayIndexOutOfBoundsException e3) {
                        e = e3;
                    }
                }
                i++;
                strArr = userValues;
            } catch (ArrayIndexOutOfBoundsException e4) {
                e = e4;
                arrayList = detectedFieldIds;
                arrayList2 = detectedFieldClassifications;
                session2.wtf(e, "Error accessing FC score at [%d, %d] (%s): %s", Integer.valueOf(i), Integer.valueOf(j), scores, e);
                return;
            }
        }
        arrayList = detectedFieldIds;
        arrayList2 = detectedFieldClassifications;
        session2.mService.logContextCommittedLocked(session2.id, session2.mClientState, session2.mSelectedDatasetIds, ignoredDatasets, changedFieldIds, changedDatasetIds, manuallyFilledFieldIds, manuallyFilledDatasetIds, arrayList, arrayList2, session2.mComponentName, session2.mCompatMode);
    }

    /* JADX WARNING: Removed duplicated region for block: B:60:0x0183  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x0506  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x02b8  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mLock")
    public boolean showSaveLocked() {
        String str;
        String str2;
        StringBuilder stringBuilder;
        if (this.mDestroyed) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Call to Session#showSaveLocked() rejected - session: ");
            stringBuilder.append(this.id);
            stringBuilder.append(" destroyed");
            Slog.w(str2, stringBuilder.toString());
            return false;
        }
        FillResponse response = getLastResponseLocked("showSaveLocked()");
        SaveInfo saveInfo = response == null ? null : response.getSaveInfo();
        if (saveInfo == null) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "showSaveLocked(): no saveInfo from service");
            }
            return true;
        }
        boolean atLeastOneChanged;
        String str3;
        StringBuilder stringBuilder2;
        boolean allRequiredAreNotEmpty;
        AutofillValue value;
        AutofillValue initialValue;
        StringBuilder stringBuilder3;
        StringBuilder stringBuilder4;
        ArrayMap<AutofillId, InternalSanitizer> sanitizers = createSanitizers(saveInfo);
        ArrayMap<AutofillId, AutofillValue> currentValues = new ArrayMap();
        ArraySet<AutofillId> allIds = new ArraySet();
        AutofillId[] requiredIds = saveInfo.getRequiredIds();
        boolean allRequiredAreNotEmpty2 = true;
        if (requiredIds != null) {
            boolean atLeastOneChanged2;
            atLeastOneChanged = false;
            boolean atLeastOneChanged3 = false;
            while (atLeastOneChanged3 < requiredIds.length) {
                boolean allRequiredAreNotEmpty3;
                AutofillId id = requiredIds[atLeastOneChanged3];
                if (id == null) {
                    str3 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("null autofill id on ");
                    stringBuilder2.append(Arrays.toString(requiredIds));
                    Slog.w(str3, stringBuilder2.toString());
                    allRequiredAreNotEmpty3 = allRequiredAreNotEmpty2;
                    atLeastOneChanged2 = atLeastOneChanged;
                } else {
                    allIds.add(id);
                    ViewState viewState = (ViewState) this.mViewStates.get(id);
                    String str4;
                    StringBuilder stringBuilder5;
                    if (viewState == null) {
                        str4 = TAG;
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("showSaveLocked(): no ViewState for required ");
                        stringBuilder5.append(id);
                        Slog.w(str4, stringBuilder5.toString());
                        allRequiredAreNotEmpty = false;
                        break;
                    }
                    ViewState viewState2;
                    value = viewState.getCurrentValue();
                    if (value == null || value.isEmpty()) {
                        initialValue = getValueFromContextsLocked(id);
                        if (initialValue != null) {
                            if (Helper.sDebug) {
                                String str5 = TAG;
                                StringBuilder stringBuilder6 = new StringBuilder();
                                allRequiredAreNotEmpty3 = allRequiredAreNotEmpty2;
                                stringBuilder6.append("Value of required field ");
                                stringBuilder6.append(id);
                                stringBuilder6.append(" didn't change; using initial value (");
                                stringBuilder6.append(initialValue);
                                stringBuilder6.append(") instead");
                                Slog.d(str5, stringBuilder6.toString());
                            } else {
                                allRequiredAreNotEmpty3 = allRequiredAreNotEmpty2;
                            }
                            value = initialValue;
                        } else {
                            atLeastOneChanged2 = atLeastOneChanged;
                            viewState2 = viewState;
                            if (Helper.sDebug) {
                                str2 = TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("empty value for required ");
                                stringBuilder3.append(id);
                                Slog.d(str2, stringBuilder3.toString());
                            }
                            allRequiredAreNotEmpty = false;
                            atLeastOneChanged = atLeastOneChanged2;
                        }
                    } else {
                        allRequiredAreNotEmpty3 = allRequiredAreNotEmpty2;
                    }
                    allRequiredAreNotEmpty2 = getSanitizedValue(sanitizers, id, value);
                    if (allRequiredAreNotEmpty2) {
                        viewState.setSanitizedValue(allRequiredAreNotEmpty2);
                        currentValues.put(id, allRequiredAreNotEmpty2);
                        value = viewState.getAutofilledValue();
                        if (allRequiredAreNotEmpty2.equals(value)) {
                            atLeastOneChanged2 = atLeastOneChanged;
                        } else {
                            initialValue = true;
                            if (value == null) {
                                AutofillValue initialValue2 = getValueFromContextsLocked(id);
                                if (initialValue2 != null && initialValue2.equals(allRequiredAreNotEmpty2)) {
                                    if (Helper.sDebug) {
                                        String str6 = TAG;
                                        atLeastOneChanged2 = atLeastOneChanged;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("id ");
                                        stringBuilder3.append(id);
                                        stringBuilder3.append(" is part of dataset but initial value didn't change: ");
                                        stringBuilder3.append(allRequiredAreNotEmpty2);
                                        Slog.d(str6, stringBuilder3.toString());
                                    } else {
                                        atLeastOneChanged2 = atLeastOneChanged;
                                        viewState2 = viewState;
                                    }
                                    initialValue = null;
                                    if (this.mHwAutofillHelper != null) {
                                        initialValue = this.mHwAutofillHelper.updateInitialFlag(this.mClientState, this.mService.getServicePackageName());
                                    }
                                    if (initialValue != null) {
                                        if (Helper.sDebug) {
                                            String str7 = TAG;
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("found a change on required ");
                                            stringBuilder4.append(id);
                                            stringBuilder4.append(": ");
                                            stringBuilder4.append(value);
                                            stringBuilder4.append(" => ");
                                            stringBuilder4.append(allRequiredAreNotEmpty2);
                                            Slog.d(str7, stringBuilder4.toString());
                                        }
                                        atLeastOneChanged = true;
                                        atLeastOneChanged3++;
                                        allRequiredAreNotEmpty2 = allRequiredAreNotEmpty3;
                                    }
                                }
                            }
                            atLeastOneChanged2 = atLeastOneChanged;
                            viewState2 = viewState;
                            if (initialValue != null) {
                            }
                        }
                    } else {
                        if (Helper.sDebug) {
                            str4 = TAG;
                            stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("value of required field ");
                            stringBuilder5.append(id);
                            stringBuilder5.append(" failed sanitization");
                            Slog.d(str4, stringBuilder5.toString());
                        }
                        allRequiredAreNotEmpty = false;
                    }
                }
                atLeastOneChanged = atLeastOneChanged2;
                atLeastOneChanged3++;
                allRequiredAreNotEmpty2 = allRequiredAreNotEmpty3;
            }
            atLeastOneChanged2 = atLeastOneChanged;
            allRequiredAreNotEmpty = allRequiredAreNotEmpty2;
        } else {
            atLeastOneChanged = false;
            allRequiredAreNotEmpty = true;
        }
        AutofillId[] optionalIds = saveInfo.getOptionalIds();
        ArraySet<AutofillId> arraySet;
        ArrayMap<AutofillId, AutofillValue> arrayMap;
        ArrayMap<AutofillId, InternalSanitizer> arrayMap2;
        FillResponse fillResponse;
        if (allRequiredAreNotEmpty) {
            boolean atLeastOneChanged4;
            if (!(atLeastOneChanged || optionalIds == null)) {
                int i = 0;
                while (i < optionalIds.length) {
                    AutofillId id2 = optionalIds[i];
                    allIds.add(id2);
                    ViewState viewState3 = (ViewState) this.mViewStates.get(id2);
                    ViewState viewState4;
                    String str8;
                    if (viewState3 == null) {
                        str3 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("no ViewState for optional ");
                        stringBuilder2.append(id2);
                        Slog.w(str3, stringBuilder2.toString());
                        atLeastOneChanged4 = atLeastOneChanged;
                    } else if ((viewState3.getState() & 8) != 0) {
                        AutofillValue currentValue = viewState3.getCurrentValue();
                        currentValues.put(id2, currentValue);
                        value = viewState3.getAutofilledValue();
                        if (currentValue == null || currentValue.equals(value)) {
                            atLeastOneChanged4 = atLeastOneChanged;
                            viewState4 = viewState3;
                        } else {
                            String str9;
                            if (Helper.sDebug) {
                                str9 = TAG;
                                atLeastOneChanged = new StringBuilder();
                                atLeastOneChanged.append("found a change on optional ");
                                atLeastOneChanged.append(id2);
                                atLeastOneChanged.append(": ");
                                atLeastOneChanged.append(value);
                                atLeastOneChanged.append(" => ");
                                atLeastOneChanged.append(currentValue);
                                Slog.d(str9, atLeastOneChanged.toString());
                            } else {
                                viewState4 = viewState3;
                            }
                            atLeastOneChanged4 = true;
                            if (atLeastOneChanged4) {
                                arraySet = allIds;
                                arrayMap = currentValues;
                                arrayMap2 = sanitizers;
                                fillResponse = response;
                                allRequiredAreNotEmpty2 = atLeastOneChanged4;
                            } else {
                                StringBuilder stringBuilder7;
                                List<Dataset> datasets;
                                InternalValidator validator;
                                ArraySet<AutofillId> allIds2;
                                ArrayMap<AutofillId, InternalSanitizer> sanitizers2;
                                if (Helper.sDebug) {
                                    Slog.d(TAG, "at least one field changed, validate fields for save UI");
                                }
                                InternalValidator validator2 = saveInfo.getValidator();
                                if (validator2 != null) {
                                    LogMaker log = newLogMaker(1133);
                                    try {
                                        allRequiredAreNotEmpty2 = validator2.isValid(this);
                                        if (Helper.sDebug) {
                                            atLeastOneChanged = TAG;
                                            stringBuilder7 = new StringBuilder();
                                            stringBuilder7.append(validator2);
                                            stringBuilder7.append(" returned ");
                                            stringBuilder7.append(allRequiredAreNotEmpty2);
                                            Slog.d(atLeastOneChanged, stringBuilder7.toString());
                                        }
                                        if (allRequiredAreNotEmpty2) {
                                            atLeastOneChanged = true;
                                        } else {
                                            atLeastOneChanged = true;
                                        }
                                        log.setType(atLeastOneChanged);
                                        this.mMetricsLogger.write(log);
                                        if (!allRequiredAreNotEmpty2) {
                                            Slog.i(TAG, "not showing save UI because fields failed validation");
                                            return true;
                                        }
                                    } catch (Exception e) {
                                        Slog.e(TAG, "Not showing save UI because validation failed:", e);
                                        log.setType(true);
                                        this.mMetricsLogger.write(log);
                                        return true;
                                    }
                                }
                                List<Dataset> datasets2 = response.getDatasets();
                                if (datasets2 != null) {
                                    i = 0;
                                    while (i < datasets2.size()) {
                                        ArrayMap<AutofillId, AutofillValue> datasetValues;
                                        Dataset dataset = (Dataset) datasets2.get(i);
                                        atLeastOneChanged = Helper.getFields(dataset);
                                        if (Helper.sVerbose) {
                                            str8 = TAG;
                                            stringBuilder4 = new StringBuilder();
                                            datasets = datasets2;
                                            stringBuilder4.append("Checking if saved fields match contents of dataset #");
                                            stringBuilder4.append(i);
                                            stringBuilder4.append(": ");
                                            stringBuilder4.append(dataset);
                                            stringBuilder4.append("; allIds=");
                                            stringBuilder4.append(allIds);
                                            Slog.v(str8, stringBuilder4.toString());
                                        } else {
                                            datasets = datasets2;
                                        }
                                        int j = 0;
                                        while (j < allIds.size()) {
                                            AutofillId id3 = (AutofillId) allIds.valueAt(j);
                                            value = (AutofillValue) currentValues.get(id3);
                                            StringBuilder stringBuilder8;
                                            if (value != null) {
                                                validator = validator2;
                                                allIds2 = allIds;
                                                sanitizers2 = sanitizers;
                                                initialValue = (AutofillValue) atLeastOneChanged.get(id3);
                                                if (value.equals(initialValue)) {
                                                    datasetValues = atLeastOneChanged;
                                                    if (Helper.sVerbose) {
                                                        atLeastOneChanged = TAG;
                                                        stringBuilder8 = new StringBuilder();
                                                        stringBuilder8.append("no dataset changes for id ");
                                                        stringBuilder8.append(id3);
                                                        Slog.v(atLeastOneChanged, stringBuilder8.toString());
                                                    }
                                                    j++;
                                                    validator2 = validator;
                                                    allIds = allIds2;
                                                    sanitizers = sanitizers2;
                                                    atLeastOneChanged = datasetValues;
                                                } else if (Helper.sDebug) {
                                                    String str10 = TAG;
                                                    StringBuilder stringBuilder9 = new StringBuilder();
                                                    datasetValues = atLeastOneChanged;
                                                    stringBuilder9.append("found a dataset change on id ");
                                                    stringBuilder9.append(id3);
                                                    stringBuilder9.append(": from ");
                                                    stringBuilder9.append(initialValue);
                                                    stringBuilder9.append(" to ");
                                                    stringBuilder9.append(value);
                                                    Slog.d(str10, stringBuilder9.toString());
                                                }
                                            } else if (Helper.sDebug) {
                                                validator = validator2;
                                                str9 = TAG;
                                                allIds2 = allIds;
                                                stringBuilder8 = new StringBuilder();
                                                sanitizers2 = sanitizers;
                                                stringBuilder8.append("dataset has value for field that is null: ");
                                                stringBuilder8.append(id3);
                                                Slog.d(str9, stringBuilder8.toString());
                                            } else {
                                                validator = validator2;
                                                allIds2 = allIds;
                                                sanitizers2 = sanitizers;
                                            }
                                            i++;
                                            datasets2 = datasets;
                                            validator2 = validator;
                                            allIds = allIds2;
                                            sanitizers = sanitizers2;
                                        }
                                        datasetValues = atLeastOneChanged;
                                        validator = validator2;
                                        allIds2 = allIds;
                                        sanitizers2 = sanitizers;
                                        if (Helper.sDebug) {
                                            atLeastOneChanged = TAG;
                                            stringBuilder7 = new StringBuilder();
                                            stringBuilder7.append("ignoring Save UI because all fields match contents of dataset #");
                                            stringBuilder7.append(i);
                                            stringBuilder7.append(": ");
                                            stringBuilder7.append(dataset);
                                            Slog.d(atLeastOneChanged, stringBuilder7.toString());
                                        }
                                        return true;
                                    }
                                }
                                datasets = datasets2;
                                validator = validator2;
                                allIds2 = allIds;
                                sanitizers2 = sanitizers;
                                if (Helper.sDebug) {
                                    str2 = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Good news, everyone! All checks passed, show save UI for ");
                                    stringBuilder.append(this.id);
                                    stringBuilder.append("!");
                                    Slog.d(str2, stringBuilder.toString());
                                }
                                if (this.mHwAutofillHelper != null) {
                                    this.mHwAutofillHelper.cacheCurrentData(this.mClientState, this.mService.getServicePackageName(), requiredIds, currentValues);
                                }
                                this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$Session$NtvZwhlT1c4eLjg2qI6EER2oCtY.INSTANCE, this));
                                IAutoFillManagerClient client = getClient();
                                this.mPendingSaveUi = new PendingUi(this.mActivityToken, this.id, client);
                                if (this.mHwAutofillHelper != null) {
                                    this.mHwAutofillHelper.recordSavedState(this.mClientState, this.mService.getServicePackageName());
                                }
                                AutoFillUI uiForShowing = getUiForShowing();
                                atLeastOneChanged = this.mService.getServiceLabel();
                                Drawable serviceIcon = this.mService.getServiceIcon();
                                str3 = this.mService.getServicePackageName();
                                ComponentName componentName = this.mComponentName;
                                response = client;
                                uiForShowing.showSaveUi(atLeastOneChanged, serviceIcon, str3, saveInfo, this, componentName, this, this.mPendingSaveUi, this.mCompatMode);
                                if (response != null) {
                                    try {
                                        response.setSaveUiState(this.id, true);
                                    } catch (RemoteException e2) {
                                        str = TAG;
                                        atLeastOneChanged = new StringBuilder();
                                        atLeastOneChanged.append("Error notifying client to set save UI state to shown: ");
                                        atLeastOneChanged.append(e2);
                                        Slog.e(str, atLeastOneChanged.toString());
                                    }
                                }
                                this.mIsSaving = true;
                                return false;
                            }
                        }
                    } else {
                        atLeastOneChanged4 = atLeastOneChanged;
                        viewState4 = viewState3;
                        atLeastOneChanged = getValueFromContextsLocked(id2);
                        if (Helper.sDebug) {
                            str8 = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("no current value for ");
                            stringBuilder4.append(id2);
                            stringBuilder4.append("; initial value is ");
                            stringBuilder4.append(atLeastOneChanged);
                            Slog.d(str8, stringBuilder4.toString());
                        }
                        if (atLeastOneChanged) {
                            currentValues.put(id2, atLeastOneChanged);
                        }
                    }
                    i++;
                    atLeastOneChanged = atLeastOneChanged4;
                }
            }
            atLeastOneChanged4 = atLeastOneChanged;
            if (atLeastOneChanged4) {
            }
        } else {
            AutofillId[] autofillIdArr = requiredIds;
            arraySet = allIds;
            arrayMap = currentValues;
            arrayMap2 = sanitizers;
            fillResponse = response;
            allRequiredAreNotEmpty2 = atLeastOneChanged;
        }
        if (Helper.sDebug) {
            str = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("showSaveLocked(");
            stringBuilder3.append(this.id);
            stringBuilder3.append("): with no changes, comes no responsibilities.allRequiredAreNotNull=");
            stringBuilder3.append(allRequiredAreNotEmpty);
            stringBuilder3.append(", atLeastOneChanged=");
            stringBuilder3.append(allRequiredAreNotEmpty2);
            Slog.d(str, stringBuilder3.toString());
        }
        return true;
    }

    private void logSaveShown() {
        this.mService.logSaveShown(this.id, this.mClientState);
    }

    private ArrayMap<AutofillId, InternalSanitizer> createSanitizers(SaveInfo saveInfo) {
        if (saveInfo == null) {
            return null;
        }
        InternalSanitizer[] sanitizerKeys = saveInfo.getSanitizerKeys();
        if (sanitizerKeys == null) {
            return null;
        }
        int size = sanitizerKeys.length;
        ArrayMap<AutofillId, InternalSanitizer> sanitizers = new ArrayMap(size);
        if (Helper.sDebug) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Service provided ");
            stringBuilder.append(size);
            stringBuilder.append(" sanitizers");
            Slog.d(str, stringBuilder.toString());
        }
        AutofillId[][] sanitizerValues = saveInfo.getSanitizerValues();
        for (int i = 0; i < size; i++) {
            InternalSanitizer sanitizer = sanitizerKeys[i];
            AutofillId[] ids = sanitizerValues[i];
            if (Helper.sDebug) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("sanitizer #");
                stringBuilder2.append(i);
                stringBuilder2.append(" (");
                stringBuilder2.append(sanitizer);
                stringBuilder2.append(") for ids ");
                stringBuilder2.append(Arrays.toString(ids));
                Slog.d(str2, stringBuilder2.toString());
            }
            for (AutofillId id : ids) {
                sanitizers.put(id, sanitizer);
            }
        }
        return sanitizers;
    }

    private AutofillValue getSanitizedValue(ArrayMap<AutofillId, InternalSanitizer> sanitizers, AutofillId id, AutofillValue value) {
        if (sanitizers == null) {
            return value;
        }
        InternalSanitizer sanitizer = (InternalSanitizer) sanitizers.get(id);
        if (sanitizer == null) {
            return value;
        }
        AutofillValue sanitized = sanitizer.sanitize(value);
        if (Helper.sDebug) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Value for ");
            stringBuilder.append(id);
            stringBuilder.append("(");
            stringBuilder.append(value);
            stringBuilder.append(") sanitized to ");
            stringBuilder.append(sanitized);
            Slog.d(str, stringBuilder.toString());
        }
        return sanitized;
    }

    @GuardedBy("mLock")
    boolean isSavingLocked() {
        return this.mIsSaving;
    }

    @GuardedBy("mLock")
    private AutofillValue getValueFromContextsLocked(AutofillId id) {
        for (int i = this.mContexts.size() - 1; i >= 0; i--) {
            ViewNode node = Helper.findViewNodeByAutofillId(((FillContext) this.mContexts.get(i)).getStructure(), id);
            if (node != null) {
                AutofillValue value = node.getAutofillValue();
                if (Helper.sDebug) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getValueFromContexts(");
                    stringBuilder.append(id);
                    stringBuilder.append(") at ");
                    stringBuilder.append(i);
                    stringBuilder.append(": ");
                    stringBuilder.append(value);
                    Slog.d(str, stringBuilder.toString());
                }
                if (!(value == null || value.isEmpty())) {
                    return value;
                }
            }
        }
        return null;
    }

    @GuardedBy("mLock")
    private CharSequence[] getAutofillOptionsFromContextsLocked(AutofillId id) {
        for (int i = this.mContexts.size() - 1; i >= 0; i--) {
            ViewNode node = Helper.findViewNodeByAutofillId(((FillContext) this.mContexts.get(i)).getStructure(), id);
            if (node != null && node.getAutofillOptions() != null) {
                return node.getAutofillOptions();
            }
        }
        return null;
    }

    @GuardedBy("mLock")
    void callSaveLocked() {
        String str;
        StringBuilder stringBuilder;
        if (this.mDestroyed) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Call to Session#callSaveLocked() rejected - session: ");
            stringBuilder.append(this.id);
            stringBuilder.append(" destroyed");
            Slog.w(str, stringBuilder.toString());
            return;
        }
        if (Helper.sVerbose) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("callSaveLocked(): mViewStates=");
            stringBuilder.append(this.mViewStates);
            Slog.v(str, stringBuilder.toString());
        }
        if (this.mContexts == null) {
            Slog.w(TAG, "callSaveLocked(): no contexts");
            return;
        }
        ArrayMap<AutofillId, InternalSanitizer> sanitizers = createSanitizers(getSaveInfoLocked());
        int numContexts = this.mContexts.size();
        for (int contextNum = 0; contextNum < numContexts; contextNum++) {
            String str2;
            StringBuilder stringBuilder2;
            FillContext context = (FillContext) this.mContexts.get(contextNum);
            ViewNode[] nodes = context.findViewNodesByAutofillIds(getIdsOfAllViewStatesLocked());
            if (Helper.sVerbose) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("callSaveLocked(): updating ");
                stringBuilder2.append(context);
                Slog.v(str2, stringBuilder2.toString());
            }
            for (int viewStateNum = 0; viewStateNum < this.mViewStates.size(); viewStateNum++) {
                ViewState viewState = (ViewState) this.mViewStates.valueAt(viewStateNum);
                AutofillId id = viewState.id;
                AutofillValue value = viewState.getCurrentValue();
                if (value != null) {
                    ViewNode node = nodes[viewStateNum];
                    String str3;
                    StringBuilder stringBuilder3;
                    if (node == null) {
                        str3 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("callSaveLocked(): did not find node with id ");
                        stringBuilder3.append(id);
                        Slog.w(str3, stringBuilder3.toString());
                    } else {
                        if (Helper.sVerbose) {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("callSaveLocked(): updating ");
                            stringBuilder3.append(id);
                            stringBuilder3.append(" to ");
                            stringBuilder3.append(value);
                            Slog.v(str3, stringBuilder3.toString());
                        }
                        AutofillValue sanitizedValue = viewState.getSanitizedValue();
                        if (sanitizedValue == null) {
                            sanitizedValue = getSanitizedValue(sanitizers, id, value);
                        }
                        if (sanitizedValue != null) {
                            node.updateAutofillValue(sanitizedValue);
                        } else if (Helper.sDebug) {
                            String str4 = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Not updating field ");
                            stringBuilder4.append(id);
                            stringBuilder4.append(" because it failed sanitization");
                            Slog.d(str4, stringBuilder4.toString());
                        }
                    }
                } else if (Helper.sVerbose) {
                    String str5 = TAG;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("callSaveLocked(): skipping ");
                    stringBuilder5.append(id);
                    Slog.v(str5, stringBuilder5.toString());
                }
            }
            context.getStructure().sanitizeForParceling(false);
            if (Helper.sVerbose) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Dumping structure of ");
                stringBuilder2.append(context);
                stringBuilder2.append(" before calling service.save()");
                Slog.v(str2, stringBuilder2.toString());
                context.getStructure().dump(false);
            }
        }
        cancelCurrentRequestLocked();
        this.mRemoteFillService.onSaveRequest(new SaveRequest(new ArrayList(this.mContexts), this.mClientState, this.mSelectedDatasetIds));
    }

    @GuardedBy("mLock")
    private void requestNewFillResponseOnViewEnteredIfNecessaryLocked(AutofillId id, ViewState viewState, int flags) {
        String str;
        StringBuilder stringBuilder;
        if ((flags & 1) != 0) {
            if (Helper.sDebug) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Re-starting session on view ");
                stringBuilder.append(id);
                stringBuilder.append(" and flags ");
                stringBuilder.append(flags);
                Slog.d(str, stringBuilder.toString());
            }
            viewState.setState(256);
            requestNewFillResponseLocked(flags);
            return;
        }
        if (shouldStartNewPartitionLocked(id)) {
            if (Helper.sDebug) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Starting partition for view id ");
                stringBuilder.append(id);
                stringBuilder.append(": ");
                stringBuilder.append(viewState.getStateAsString());
                Slog.d(str, stringBuilder.toString());
            }
            viewState.setState(32);
            requestNewFillResponseLocked(flags);
        } else if (Helper.sVerbose) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not starting new partition for view ");
            stringBuilder.append(id);
            stringBuilder.append(": ");
            stringBuilder.append(viewState.getStateAsString());
            Slog.v(str, stringBuilder.toString());
        }
    }

    @GuardedBy("mLock")
    private boolean shouldStartNewPartitionLocked(AutofillId id) {
        if (this.mResponses == null) {
            return true;
        }
        int numResponses = this.mResponses.size();
        if (numResponses >= Helper.sPartitionMaxCount) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not starting a new partition on ");
            stringBuilder.append(id);
            stringBuilder.append(" because session ");
            stringBuilder.append(this.id);
            stringBuilder.append(" reached maximum of ");
            stringBuilder.append(Helper.sPartitionMaxCount);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
        for (int responseNum = 0; responseNum < numResponses; responseNum++) {
            FillResponse response = (FillResponse) this.mResponses.valueAt(responseNum);
            if (ArrayUtils.contains(response.getIgnoredIds(), id)) {
                return false;
            }
            SaveInfo saveInfo = response.getSaveInfo();
            if (saveInfo != null && (ArrayUtils.contains(saveInfo.getOptionalIds(), id) || ArrayUtils.contains(saveInfo.getRequiredIds(), id))) {
                return false;
            }
            List<Dataset> datasets = response.getDatasets();
            if (datasets != null) {
                int numDatasets = datasets.size();
                for (int dataSetNum = 0; dataSetNum < numDatasets; dataSetNum++) {
                    ArrayList<AutofillId> fields = ((Dataset) datasets.get(dataSetNum)).getFieldIds();
                    if (fields != null && fields.contains(id)) {
                        return false;
                    }
                }
            }
            if (ArrayUtils.contains(response.getAuthenticationIds(), id)) {
                return false;
            }
        }
        return true;
    }

    @GuardedBy("mLock")
    void updateLocked(AutofillId id, Rect virtualBounds, AutofillValue value, int action, int flags) {
        String str;
        StringBuilder stringBuilder;
        if (this.mDestroyed) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Call to Session#updateLocked() rejected - session: ");
            stringBuilder.append(id);
            stringBuilder.append(" destroyed");
            Slog.w(str, stringBuilder.toString());
            return;
        }
        String str2;
        StringBuilder stringBuilder2;
        String str3;
        if (Helper.sVerbose) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateLocked(): id=");
            stringBuilder.append(id);
            stringBuilder.append(", action=");
            stringBuilder.append(actionAsString(action));
            stringBuilder.append(", flags=");
            stringBuilder.append(flags);
            Slog.v(str, stringBuilder.toString());
        }
        ViewState viewState = (ViewState) this.mViewStates.get(id);
        if (viewState == null) {
            if (action == 1 || action == 4 || action == 2) {
                if (Helper.sVerbose) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Creating viewState for ");
                    stringBuilder2.append(id);
                    Slog.v(str2, stringBuilder2.toString());
                }
                boolean isIgnored = isIgnoredLocked(id);
                viewState = new ViewState(this, id, this, isIgnored ? 128 : 1);
                this.mViewStates.put(id, viewState);
                if (isIgnored) {
                    if (Helper.sDebug) {
                        str3 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateLocked(): ignoring view ");
                        stringBuilder2.append(viewState);
                        Slog.d(str3, stringBuilder2.toString());
                    }
                    return;
                }
            }
            if (Helper.sVerbose) {
                Slog.v(TAG, "Ignoring specific action when viewState=null");
            }
            return;
        }
        str2 = null;
        StringBuilder stringBuilder3;
        switch (action) {
            case 1:
                this.mCurrentViewId = viewState.id;
                viewState.update(value, virtualBounds, flags);
                viewState.setState(16);
                requestNewFillResponseLocked(flags);
                break;
            case 2:
                if (Helper.sVerbose && virtualBounds != null) {
                    str3 = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("entered on virtual child ");
                    stringBuilder3.append(id);
                    stringBuilder3.append(": ");
                    stringBuilder3.append(virtualBounds);
                    Slog.v(str3, stringBuilder3.toString());
                }
                if (!this.mCompatMode || (viewState.getState() & 512) == 0) {
                    requestNewFillResponseOnViewEnteredIfNecessaryLocked(id, viewState, flags);
                    if (this.mCurrentViewId != viewState.id) {
                        this.mUi.hideFillUi(this);
                        this.mCurrentViewId = viewState.id;
                    }
                    viewState.update(value, virtualBounds, flags);
                    break;
                }
                if (Helper.sDebug) {
                    str3 = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Ignoring VIEW_ENTERED on URL BAR (id=");
                    stringBuilder3.append(id);
                    stringBuilder3.append(")");
                    Slog.d(str3, stringBuilder3.toString());
                }
                return;
            case 3:
                if (this.mCurrentViewId == viewState.id) {
                    if (Helper.sVerbose) {
                        str3 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Exiting view ");
                        stringBuilder2.append(id);
                        Slog.d(str3, stringBuilder2.toString());
                    }
                    this.mUi.hideFillUi(this);
                    this.mCurrentViewId = null;
                    break;
                }
                break;
            case 4:
                String currentUrl;
                if (this.mCompatMode && (viewState.getState() & 512) != 0) {
                    currentUrl = this.mUrlBar == null ? null : this.mUrlBar.getText().toString().trim();
                    if (currentUrl == null) {
                        wtf(null, "URL bar value changed, but current value is null", new Object[0]);
                        return;
                    } else if (value == null || !value.isText()) {
                        wtf(null, "URL bar value changed to null or non-text: %s", value);
                        return;
                    } else if (value.getTextValue().toString().equals(currentUrl)) {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "Ignoring change on URL bar as it's the same");
                        }
                        return;
                    } else if (this.mSaveOnAllViewsInvisible) {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "Ignoring change on URL because session will finish when views are gone");
                        }
                        return;
                    } else {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "Finishing session because URL bar changed");
                        }
                        forceRemoveSelfLocked(5);
                        return;
                    }
                } else if (!Objects.equals(value, viewState.getCurrentValue())) {
                    if (!((value != null && !value.isEmpty()) || viewState.getCurrentValue() == null || !viewState.getCurrentValue().isText() || viewState.getCurrentValue().getTextValue() == null || getSaveInfoLocked() == null)) {
                        int length = viewState.getCurrentValue().getTextValue().length();
                        if (Helper.sDebug) {
                            currentUrl = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("updateLocked(");
                            stringBuilder4.append(id);
                            stringBuilder4.append("): resetting value that was ");
                            stringBuilder4.append(length);
                            stringBuilder4.append(" chars long");
                            Slog.d(currentUrl, stringBuilder4.toString());
                        }
                        this.mMetricsLogger.write(newLogMaker(1124).addTaggedData(1125, Integer.valueOf(length)));
                    }
                    viewState.setCurrentValue(value);
                    AutofillValue filledValue = viewState.getAutofilledValue();
                    if (filledValue == null || !filledValue.equals(value)) {
                        viewState.setState(8);
                        if (value == null || !value.isText()) {
                            str2 = null;
                        } else {
                            CharSequence text = value.getTextValue();
                            if (text != null) {
                                str2 = text.toString();
                            }
                        }
                        getUiForShowing().filterFillUi(str2, this);
                        break;
                    }
                    if (Helper.sVerbose) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ignoring autofilled change on id ");
                        stringBuilder2.append(id);
                        Slog.v(str2, stringBuilder2.toString());
                    }
                    return;
                }
                break;
            default:
                str3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("updateLocked(): unknown action: ");
                stringBuilder3.append(action);
                Slog.w(str3, stringBuilder3.toString());
                break;
        }
    }

    @GuardedBy("mLock")
    private boolean isIgnoredLocked(AutofillId id) {
        FillResponse response = getLastResponseLocked(null);
        if (response == null) {
            return false;
        }
        return ArrayUtils.contains(response.getIgnoredIds(), id);
    }

    /* JADX WARNING: Missing block: B:9:0x0028, code:
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:10:0x0029, code:
            if (r17 == null) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:12:0x002f, code:
            if (r17.isText() == false) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:13:0x0031, code:
            r0 = r17.getTextValue().toString();
     */
    /* JADX WARNING: Missing block: B:14:0x0039, code:
            getUiForShowing().showFillUi(r16, r15, r0, r12.mService.getServicePackageName(), r12.mComponentName, r12.mService.getServiceLabel(), r12.mService.getServiceIcon(), r12, r12.id, r12.mCompatMode);
            r2 = r12.mLock;
     */
    /* JADX WARNING: Missing block: B:15:0x0060, code:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:18:0x0067, code:
            if (r12.mUiShownTime != 0) goto L_0x00d7;
     */
    /* JADX WARNING: Missing block: B:19:0x0069, code:
            r12.mUiShownTime = android.os.SystemClock.elapsedRealtime();
            r0 = r12.mUiShownTime - r12.mStartTime;
     */
    /* JADX WARNING: Missing block: B:20:0x0076, code:
            if (com.android.server.autofill.Helper.sDebug == false) goto L_0x0095;
     */
    /* JADX WARNING: Missing block: B:21:0x0078, code:
            r3 = new java.lang.StringBuilder("1st UI for ");
            r3.append(r12.mActivityToken);
            r3.append(" shown in ");
            android.util.TimeUtils.formatDuration(r0, r3);
            android.util.Slog.d(TAG, r3.toString());
     */
    /* JADX WARNING: Missing block: B:22:0x0095, code:
            r3 = new java.lang.StringBuilder("id=");
            r3.append(r12.id);
            r3.append(" app=");
            r3.append(r12.mActivityToken);
            r3.append(" svc=");
            r3.append(r12.mService.getServicePackageName());
            r3.append(" latency=");
            android.util.TimeUtils.formatDuration(r0, r3);
            r12.mUiLatencyHistory.log(r3.toString());
            addTaggedDataToRequestLogLocked(r15.getRequestId(), 1145, java.lang.Long.valueOf(r0));
     */
    /* JADX WARNING: Missing block: B:23:0x00d7, code:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:24:0x00d8, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onFillReady(FillResponse response, AutofillId filledId, AutofillValue value) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#onFillReady() rejected - session: ");
                stringBuilder.append(this.id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
            }
        }
    }

    boolean isDestroyed() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDestroyed;
        }
        return z;
    }

    IAutoFillManagerClient getClient() {
        IAutoFillManagerClient iAutoFillManagerClient;
        synchronized (this.mLock) {
            iAutoFillManagerClient = this.mClient;
        }
        return iAutoFillManagerClient;
    }

    private void notifyUnavailableToClient(int sessionFinishedState) {
        synchronized (this.mLock) {
            if (this.mCurrentViewId == null) {
                return;
            }
            try {
                if (this.mHasCallback) {
                    this.mClient.notifyNoFillUi(this.id, this.mCurrentViewId, sessionFinishedState);
                } else if (sessionFinishedState != 0) {
                    this.mClient.setSessionFinished(sessionFinishedState);
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error notifying client no fill UI: id=");
                stringBuilder.append(this.mCurrentViewId);
                Slog.e(str, stringBuilder.toString(), e);
            }
        }
    }

    @GuardedBy("mLock")
    private void updateTrackedIdsLocked() {
        AutofillId saveTriggerId = null;
        FillResponse response = getLastResponseLocked(null);
        if (response != null) {
            ArraySet<AutofillId> fillableIds;
            ArraySet<AutofillId> trackedViews = null;
            this.mSaveOnAllViewsInvisible = false;
            boolean saveOnFinish = true;
            SaveInfo saveInfo = response.getSaveInfo();
            if (saveInfo != null) {
                saveTriggerId = saveInfo.getTriggerId();
                if (saveTriggerId != null) {
                    writeLog(1228);
                }
                boolean z = true;
                if ((saveInfo.getFlags() & 1) == 0) {
                    z = false;
                }
                this.mSaveOnAllViewsInvisible = z;
                if (this.mSaveOnAllViewsInvisible) {
                    if (null == null) {
                        trackedViews = new ArraySet();
                    }
                    if (saveInfo.getRequiredIds() != null) {
                        Collections.addAll(trackedViews, saveInfo.getRequiredIds());
                    }
                    if (saveInfo.getOptionalIds() != null) {
                        Collections.addAll(trackedViews, saveInfo.getOptionalIds());
                    }
                }
                if ((saveInfo.getFlags() & 2) != 0) {
                    saveOnFinish = false;
                }
            }
            List<Dataset> datasets = response.getDatasets();
            if (datasets != null) {
                ArraySet<AutofillId> fillableIds2 = null;
                for (ArraySet<AutofillId> fillableIds3 = null; fillableIds3 < datasets.size(); fillableIds3++) {
                    ArrayList<AutofillId> fieldIds = ((Dataset) datasets.get(fillableIds3)).getFieldIds();
                    if (fieldIds != null) {
                        ArraySet<AutofillId> fillableIds4 = fillableIds2;
                        for (int j = 0; j < fieldIds.size(); j++) {
                            AutofillId id = (AutofillId) fieldIds.get(j);
                            if (trackedViews == null || !trackedViews.contains(id)) {
                                fillableIds4 = ArrayUtils.add(fillableIds4, id);
                            }
                        }
                        fillableIds2 = fillableIds4;
                    }
                }
                fillableIds = fillableIds2;
            } else {
                fillableIds = null;
            }
            try {
                if (Helper.sVerbose) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateTrackedIdsLocked(): ");
                    stringBuilder.append(trackedViews);
                    stringBuilder.append(" => ");
                    stringBuilder.append(fillableIds);
                    stringBuilder.append(" triggerId: ");
                    stringBuilder.append(saveTriggerId);
                    stringBuilder.append(" saveOnFinish:");
                    stringBuilder.append(saveOnFinish);
                    Slog.v(str, stringBuilder.toString());
                }
                this.mClient.setTrackedViews(this.id, Helper.toArray(trackedViews), this.mSaveOnAllViewsInvisible, saveOnFinish, Helper.toArray(fillableIds), saveTriggerId);
            } catch (RemoteException e) {
                Slog.w(TAG, "Cannot set tracked ids", e);
            }
        }
    }

    @GuardedBy("mLock")
    void setAutofillFailureLocked(List<AutofillId> ids) {
        for (int i = 0; i < ids.size(); i++) {
            AutofillId id = (AutofillId) ids.get(i);
            ViewState viewState = (ViewState) this.mViewStates.get(id);
            if (viewState == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setAutofillFailure(): no view for id ");
                stringBuilder.append(id);
                Slog.w(str, stringBuilder.toString());
            } else {
                viewState.resetState(4);
                viewState.setState(viewState.getState() | 1024);
                if (Helper.sVerbose) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Changed state of ");
                    stringBuilder2.append(id);
                    stringBuilder2.append(" to ");
                    stringBuilder2.append(viewState.getStateAsString());
                    Slog.v(str2, stringBuilder2.toString());
                }
            }
        }
    }

    @GuardedBy("mLock")
    private void replaceResponseLocked(FillResponse oldResponse, FillResponse newResponse, Bundle newClientState) {
        setViewStatesLocked(oldResponse, 1, true);
        newResponse.setRequestId(oldResponse.getRequestId());
        this.mResponses.put(newResponse.getRequestId(), newResponse);
        processResponseLocked(newResponse, newClientState, 0);
    }

    private void processNullResponseLocked(int flags) {
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("canceling session ");
            stringBuilder.append(this.id);
            stringBuilder.append(" when server returned null");
            Slog.v(str, stringBuilder.toString());
        }
        if ((flags & 1) != 0) {
            getUiForShowing().showError(17039639, (AutoFillUiCallback) this);
        }
        this.mService.resetLastResponse();
        notifyUnavailableToClient(2);
        removeSelf();
    }

    @GuardedBy("mLock")
    private void processResponseLocked(FillResponse newResponse, Bundle newClientState, int flags) {
        this.mUi.hideAll(this);
        int requestId = newResponse.getRequestId();
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("processResponseLocked(): mCurrentViewId=");
            stringBuilder.append(this.mCurrentViewId);
            stringBuilder.append(",flags=");
            stringBuilder.append(flags);
            stringBuilder.append(", reqId=");
            stringBuilder.append(requestId);
            stringBuilder.append(", resp=");
            stringBuilder.append(newResponse);
            stringBuilder.append(",newClientState=");
            stringBuilder.append(newClientState);
            Slog.v(str, stringBuilder.toString());
        }
        if (this.mResponses == null) {
            this.mResponses = new SparseArray(2);
        }
        this.mResponses.put(requestId, newResponse);
        this.mClientState = newClientState != null ? newClientState : newResponse.getClientState();
        setViewStatesLocked(newResponse, 2, false);
        updateTrackedIdsLocked();
        if (this.mCurrentViewId != null) {
            ((ViewState) this.mViewStates.get(this.mCurrentViewId)).maybeCallOnFillReady(flags);
        }
    }

    @GuardedBy("mLock")
    private void setViewStatesLocked(FillResponse response, int state, boolean clearResponse) {
        int length;
        AutofillId[] requiredIds;
        List<Dataset> datasets = response.getDatasets();
        int i = 0;
        if (datasets != null) {
            for (int i2 = 0; i2 < datasets.size(); i2++) {
                Dataset dataset = (Dataset) datasets.get(i2);
                if (dataset == null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Ignoring null dataset on ");
                    stringBuilder.append(datasets);
                    Slog.w(str, stringBuilder.toString());
                } else {
                    setViewStatesLocked(response, dataset, state, clearResponse);
                }
            }
        } else if (response.getAuthentication() != null) {
            for (AutofillId autofillId : response.getAuthenticationIds()) {
                ViewState viewState = createOrUpdateViewStateLocked(autofillId, state, null);
                if (clearResponse) {
                    viewState.setResponse(null);
                } else {
                    viewState.setResponse(response);
                }
            }
        }
        SaveInfo saveInfo = response.getSaveInfo();
        if (saveInfo != null) {
            requiredIds = saveInfo.getRequiredIds();
            if (requiredIds != null) {
                for (AutofillId id : requiredIds) {
                    createOrUpdateViewStateLocked(id, state, null);
                }
            }
            AutofillId[] optionalIds = saveInfo.getOptionalIds();
            if (optionalIds != null) {
                for (AutofillId id2 : optionalIds) {
                    createOrUpdateViewStateLocked(id2, state, null);
                }
            }
        }
        requiredIds = response.getAuthenticationIds();
        if (requiredIds != null) {
            length = requiredIds.length;
            while (i < length) {
                createOrUpdateViewStateLocked(requiredIds[i], state, null);
                i++;
            }
        }
    }

    @GuardedBy("mLock")
    private void setViewStatesLocked(FillResponse response, Dataset dataset, int state, boolean clearResponse) {
        ArrayList<AutofillId> ids = dataset.getFieldIds();
        ArrayList<AutofillValue> values = dataset.getFieldValues();
        for (int j = 0; j < ids.size(); j++) {
            ViewState viewState = createOrUpdateViewStateLocked((AutofillId) ids.get(j), state, (AutofillValue) values.get(j));
            String datasetId = dataset.getId();
            if (datasetId != null) {
                viewState.setDatasetId(datasetId);
            }
            if (response != null) {
                viewState.setResponse(response);
            } else if (clearResponse) {
                viewState.setResponse(null);
            }
        }
    }

    @GuardedBy("mLock")
    private ViewState createOrUpdateViewStateLocked(AutofillId id, int state, AutofillValue value) {
        ViewState viewState = (ViewState) this.mViewStates.get(id);
        if (viewState != null) {
            viewState.setState(state);
        } else {
            viewState = new ViewState(this, id, this, state);
            if (Helper.sVerbose) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Adding autofillable view with id ");
                stringBuilder.append(id);
                stringBuilder.append(" and state ");
                stringBuilder.append(state);
                Slog.v(str, stringBuilder.toString());
            }
            this.mViewStates.put(id, viewState);
        }
        if ((state & 4) != 0) {
            viewState.setAutofilledValue(value);
        }
        return viewState;
    }

    void autoFill(int requestId, int datasetIndex, Dataset dataset, boolean generateEvent) {
        if (Helper.sDebug) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("autoFill(): requestId=");
            stringBuilder.append(requestId);
            stringBuilder.append("; datasetIdx=");
            stringBuilder.append(datasetIndex);
            stringBuilder.append("; dataset=");
            stringBuilder.append(dataset);
            Slog.d(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Call to Session#autoFill() rejected - session: ");
                stringBuilder2.append(this.id);
                stringBuilder2.append(" destroyed");
                Slog.w(str2, stringBuilder2.toString());
            } else if (dataset.getAuthentication() == null) {
                if (generateEvent) {
                    this.mService.logDatasetSelected(dataset.getId(), this.id, this.mClientState);
                }
                autoFillApp(dataset);
            } else {
                this.mService.logDatasetAuthenticationSelected(dataset.getId(), this.id, this.mClientState);
                setViewStatesLocked(null, dataset, 64, false);
                Intent fillInIntent = createAuthFillInIntentLocked(requestId, this.mClientState);
                if (fillInIntent == null) {
                    forceRemoveSelfLocked();
                    return;
                }
                startAuthentication(AutofillManager.makeAuthenticationId(requestId, datasetIndex), dataset.getAuthentication(), fillInIntent);
            }
        }
    }

    CharSequence getServiceName() {
        CharSequence serviceName;
        synchronized (this.mLock) {
            serviceName = this.mService.getServiceName();
        }
        return serviceName;
    }

    @GuardedBy("mLock")
    private Intent createAuthFillInIntentLocked(int requestId, Bundle extras) {
        Intent fillInIntent = new Intent();
        FillContext context = getFillContextByRequestIdLocked(requestId);
        if (context == null) {
            wtf(null, "createAuthFillInIntentLocked(): no FillContext. requestId=%d; mContexts=%s", Integer.valueOf(requestId), this.mContexts);
            return null;
        }
        fillInIntent.putExtra("android.view.autofill.extra.ASSIST_STRUCTURE", context.getStructure());
        fillInIntent.putExtra("android.view.autofill.extra.CLIENT_STATE", extras);
        return fillInIntent;
    }

    private void startAuthentication(int authenticationId, IntentSender intent, Intent fillInIntent) {
        try {
            synchronized (this.mLock) {
                this.mClient.authenticate(this.id, authenticationId, intent, fillInIntent);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Error launching auth intent", e);
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Session: [id=");
        stringBuilder.append(this.id);
        stringBuilder.append(", component=");
        stringBuilder.append(this.mComponentName);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    @GuardedBy("mLock")
    void dumpLocked(String prefix, PrintWriter pw) {
        int i;
        int requestId;
        int i2;
        String prefix2 = new StringBuilder();
        prefix2.append(prefix);
        prefix2.append("  ");
        prefix2 = prefix2.toString();
        pw.print(prefix);
        pw.print("id: ");
        pw.println(this.id);
        pw.print(prefix);
        pw.print("uid: ");
        pw.println(this.uid);
        pw.print(prefix);
        pw.print("flags: ");
        pw.println(this.mFlags);
        pw.print(prefix);
        pw.print("mComponentName: ");
        pw.println(this.mComponentName);
        pw.print(prefix);
        pw.print("mActivityToken: ");
        pw.println(this.mActivityToken);
        pw.print(prefix);
        pw.print("mStartTime: ");
        pw.println(this.mStartTime);
        pw.print(prefix);
        pw.print("Time to show UI: ");
        if (this.mUiShownTime == 0) {
            pw.println("N/A");
        } else {
            TimeUtils.formatDuration(this.mUiShownTime - this.mStartTime, pw);
            pw.println();
        }
        int requestLogsSizes = this.mRequestLogs.size();
        pw.print(prefix);
        pw.print("mSessionLogs: ");
        pw.println(requestLogsSizes);
        for (i = 0; i < requestLogsSizes; i++) {
            requestId = this.mRequestLogs.keyAt(i);
            LogMaker log = (LogMaker) this.mRequestLogs.valueAt(i);
            pw.print(prefix2);
            pw.print('#');
            pw.print(i);
            pw.print(": req=");
            pw.print(requestId);
            pw.print(", log=");
            dumpRequestLog(pw, log);
            pw.println();
        }
        pw.print(prefix);
        pw.print("mResponses: ");
        if (this.mResponses == null) {
            pw.println("null");
        } else {
            pw.println(this.mResponses.size());
            for (i = 0; i < this.mResponses.size(); i++) {
                pw.print(prefix2);
                pw.print('#');
                pw.print(i);
                pw.print(' ');
                pw.println(this.mResponses.valueAt(i));
            }
        }
        pw.print(prefix);
        pw.print("mCurrentViewId: ");
        pw.println(this.mCurrentViewId);
        pw.print(prefix);
        pw.print("mDestroyed: ");
        pw.println(this.mDestroyed);
        pw.print(prefix);
        pw.print("mIsSaving: ");
        pw.println(this.mIsSaving);
        pw.print(prefix);
        pw.print("mPendingSaveUi: ");
        pw.println(this.mPendingSaveUi);
        i = this.mViewStates.size();
        pw.print(prefix);
        pw.print("mViewStates size: ");
        pw.println(this.mViewStates.size());
        for (i2 = 0; i2 < i; i2++) {
            pw.print(prefix);
            pw.print("ViewState at #");
            pw.println(i2);
            ((ViewState) this.mViewStates.valueAt(i2)).dump(prefix2, pw);
        }
        pw.print(prefix);
        pw.print("mContexts: ");
        if (this.mContexts != null) {
            i2 = this.mContexts.size();
            for (requestId = 0; requestId < i2; requestId++) {
                FillContext context = (FillContext) this.mContexts.get(requestId);
                pw.print(prefix2);
                pw.print(context);
                if (Helper.sVerbose) {
                    pw.println("AssistStructure dumped at logcat)");
                    context.getStructure().dump(false);
                }
            }
        } else {
            pw.println("null");
        }
        pw.print(prefix);
        pw.print("mHasCallback: ");
        pw.println(this.mHasCallback);
        if (this.mClientState != null) {
            pw.print(prefix);
            pw.print("mClientState: ");
            pw.print(this.mClientState.getSize());
            pw.println(" bytes");
        }
        pw.print(prefix);
        pw.print("mCompatMode: ");
        pw.println(this.mCompatMode);
        pw.print(prefix);
        pw.print("mUrlBar: ");
        if (this.mUrlBar == null) {
            pw.println("N/A");
        } else {
            pw.print("id=");
            pw.print(this.mUrlBar.getAutofillId());
            pw.print(" domain=");
            pw.print(this.mUrlBar.getWebDomain());
            pw.print(" text=");
            Helper.printlnRedactedText(pw, this.mUrlBar.getText());
        }
        pw.print(prefix);
        pw.print("mSaveOnAllViewsInvisible: ");
        pw.println(this.mSaveOnAllViewsInvisible);
        pw.print(prefix);
        pw.print("mSelectedDatasetIds: ");
        pw.println(this.mSelectedDatasetIds);
        this.mRemoteFillService.dump(prefix, pw);
    }

    private static void dumpRequestLog(PrintWriter pw, LogMaker log) {
        pw.print("CAT=");
        pw.print(log.getCategory());
        pw.print(", TYPE=");
        int type = log.getType();
        if (type != 2) {
            switch (type) {
                case 10:
                    pw.print("SUCCESS");
                    break;
                case 11:
                    pw.print("FAILURE");
                    break;
                default:
                    pw.print("UNSUPPORTED");
                    break;
            }
        }
        pw.print("CLOSE");
        pw.print('(');
        pw.print(type);
        pw.print(')');
        pw.print(", PKG=");
        pw.print(log.getPackageName());
        pw.print(", SERVICE=");
        pw.print(log.getTaggedData(908));
        pw.print(", ORDINAL=");
        pw.print(log.getTaggedData(1454));
        dumpNumericValue(pw, log, "FLAGS", 1452);
        dumpNumericValue(pw, log, "NUM_DATASETS", 909);
        dumpNumericValue(pw, log, "UI_LATENCY", 1145);
        int authStatus = Helper.getNumericValue(log, 1453);
        if (authStatus != 0) {
            pw.print(", AUTH_STATUS=");
            if (authStatus != 912) {
                switch (authStatus) {
                    case 1126:
                        pw.print("DATASET_AUTHENTICATED");
                        break;
                    case 1127:
                        pw.print("INVALID_DATASET_AUTHENTICATION");
                        break;
                    case 1128:
                        pw.print("INVALID_AUTHENTICATION");
                        break;
                    default:
                        pw.print("UNSUPPORTED");
                        break;
                }
            }
            pw.print("AUTHENTICATED");
            pw.print('(');
            pw.print(authStatus);
            pw.print(')');
        }
        dumpNumericValue(pw, log, "FC_IDS", 1271);
        dumpNumericValue(pw, log, "COMPAT_MODE", 1414);
    }

    private static void dumpNumericValue(PrintWriter pw, LogMaker log, String field, int tag) {
        int value = Helper.getNumericValue(log, tag);
        if (value != 0) {
            pw.print(", ");
            pw.print(field);
            pw.print('=');
            pw.print(value);
        }
    }

    void autoFillApp(Dataset dataset) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Call to Session#autoFillApp() rejected - session: ");
                stringBuilder.append(this.id);
                stringBuilder.append(" destroyed");
                Slog.w(str, stringBuilder.toString());
                return;
            }
            try {
                int entryCount = dataset.getFieldIds().size();
                List<AutofillId> ids = new ArrayList(entryCount);
                List values = new ArrayList(entryCount);
                boolean waitingDatasetAuth = false;
                for (int i = 0; i < entryCount; i++) {
                    if (dataset.getFieldValues().get(i) != null) {
                        AutofillId viewId = (AutofillId) dataset.getFieldIds().get(i);
                        ids.add(viewId);
                        values.add((AutofillValue) dataset.getFieldValues().get(i));
                        ViewState viewState = (ViewState) this.mViewStates.get(viewId);
                        if (!(viewState == null || (viewState.getState() & 64) == 0)) {
                            if (Helper.sVerbose) {
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("autofillApp(): view ");
                                stringBuilder2.append(viewId);
                                stringBuilder2.append(" waiting auth");
                                Slog.v(str2, stringBuilder2.toString());
                            }
                            viewState.resetState(64);
                            waitingDatasetAuth = true;
                        }
                    }
                }
                if (!ids.isEmpty()) {
                    if (waitingDatasetAuth) {
                        this.mUi.hideFillUi(this);
                    }
                    if (Helper.sDebug) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("autoFillApp(): the buck is on the app: ");
                        stringBuilder3.append(dataset);
                        Slog.d(str3, stringBuilder3.toString());
                    }
                    this.mClient.autofill(this.id, ids, values);
                    if (this.mHwAutofillHelper != null) {
                        this.mHwAutofillHelper.updateAutoFillManagerClient(this.mClientState, this.mService.getServicePackageName(), this.mClient, this.id, ids, values);
                    }
                    if (dataset.getId() != null) {
                        if (this.mSelectedDatasetIds == null) {
                            this.mSelectedDatasetIds = new ArrayList();
                        }
                        this.mSelectedDatasetIds.add(dataset.getId());
                    }
                    setViewStatesLocked(null, dataset, 4, false);
                }
            } catch (RemoteException e) {
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Error autofilling activity: ");
                stringBuilder4.append(e);
                Slog.w(str4, stringBuilder4.toString());
            }
        }
    }

    private AutoFillUI getUiForShowing() {
        AutoFillUI autoFillUI;
        synchronized (this.mLock) {
            this.mUi.setCallback(this);
            autoFillUI = this.mUi;
        }
        return autoFillUI;
    }

    @GuardedBy("mLock")
    RemoteFillService destroyLocked() {
        if (this.mDestroyed) {
            return null;
        }
        unlinkClientVultureLocked();
        this.mUi.destroyAll(this.mPendingSaveUi, this, true);
        this.mUi.clearCallback(this);
        this.mDestroyed = true;
        int totalRequests = this.mRequestLogs.size();
        if (totalRequests > 0) {
            if (Helper.sVerbose) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("destroyLocked(): logging ");
                stringBuilder.append(totalRequests);
                stringBuilder.append(" requests");
                Slog.v(str, stringBuilder.toString());
            }
            for (int i = 0; i < totalRequests; i++) {
                this.mMetricsLogger.write((LogMaker) this.mRequestLogs.valueAt(i));
            }
        }
        this.mMetricsLogger.write(newLogMaker(919).addTaggedData(1455, Integer.valueOf(totalRequests)));
        return this.mRemoteFillService;
    }

    @GuardedBy("mLock")
    void forceRemoveSelfLocked() {
        forceRemoveSelfLocked(0);
    }

    @GuardedBy("mLock")
    void forceRemoveSelfLocked(int clientState) {
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("forceRemoveSelfLocked(): ");
            stringBuilder.append(this.mPendingSaveUi);
            Slog.v(str, stringBuilder.toString());
        }
        boolean isPendingSaveUi = isSaveUiPendingLocked();
        this.mPendingSaveUi = null;
        removeSelfLocked();
        this.mUi.destroyAll(this.mPendingSaveUi, this, false);
        if (!isPendingSaveUi) {
            try {
                this.mClient.setSessionFinished(clientState);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error notifying client to finish session", e);
            }
        }
    }

    private void removeSelf() {
        synchronized (this.mLock) {
            removeSelfLocked();
        }
    }

    @GuardedBy("mLock")
    void removeSelfLocked() {
        String str;
        StringBuilder stringBuilder;
        if (Helper.sVerbose) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("removeSelfLocked(): ");
            stringBuilder.append(this.mPendingSaveUi);
            Slog.v(str, stringBuilder.toString());
        }
        if (this.mDestroyed) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Call to Session#removeSelfLocked() rejected - session: ");
            stringBuilder.append(this.id);
            stringBuilder.append(" destroyed");
            Slog.w(str, stringBuilder.toString());
        } else if (isSaveUiPendingLocked()) {
            Slog.i(TAG, "removeSelfLocked() ignored, waiting for pending save ui");
        } else {
            RemoteFillService remoteFillService = destroyLocked();
            this.mService.removeSessionLocked(this.id);
            if (remoteFillService != null) {
                remoteFillService.destroy();
            }
        }
    }

    void onPendingSaveUi(int operation, IBinder token) {
        getUiForShowing().onPendingSaveUi(operation, token);
    }

    @GuardedBy("mLock")
    boolean isSaveUiPendingForTokenLocked(IBinder token) {
        return isSaveUiPendingLocked() && token.equals(this.mPendingSaveUi.getToken());
    }

    @GuardedBy("mLock")
    private boolean isSaveUiPendingLocked() {
        return this.mPendingSaveUi != null && this.mPendingSaveUi.getState() == 2;
    }

    @GuardedBy("mLock")
    private int getLastResponseIndexLocked() {
        int lastResponseIdx = -1;
        if (this.mResponses != null) {
            int responseCount = this.mResponses.size();
            for (int i = 0; i < responseCount; i++) {
                if (this.mResponses.keyAt(i) > -1) {
                    lastResponseIdx = i;
                }
            }
        }
        return lastResponseIdx;
    }

    private LogMaker newLogMaker(int category) {
        return newLogMaker(category, this.mService.getServicePackageName());
    }

    private LogMaker newLogMaker(int category, String servicePackageName) {
        return Helper.newLogMaker(category, this.mComponentName, servicePackageName, this.id, this.mCompatMode);
    }

    private void writeLog(int category) {
        this.mMetricsLogger.write(newLogMaker(category));
    }

    private void logAuthenticationStatusLocked(int requestId, int status) {
        addTaggedDataToRequestLogLocked(requestId, 1453, Integer.valueOf(status));
    }

    private void addTaggedDataToRequestLogLocked(int requestId, int tag, Object value) {
        LogMaker requestLog = (LogMaker) this.mRequestLogs.get(requestId);
        if (requestLog == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addTaggedDataToRequestLogLocked(tag=");
            stringBuilder.append(tag);
            stringBuilder.append("): no log for id ");
            stringBuilder.append(requestId);
            Slog.w(str, stringBuilder.toString());
            return;
        }
        requestLog.addTaggedData(tag, value);
    }

    private static String requestLogToString(LogMaker log) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        dumpRequestLog(pw, log);
        pw.flush();
        return sw.toString();
    }

    private void wtf(Exception e, String fmt, Object... args) {
        String message = String.format(fmt, args);
        this.mWtfHistory.log(message);
        if (e != null) {
            Slog.wtf(TAG, message, e);
        } else {
            Slog.wtf(TAG, message);
        }
    }

    private static String actionAsString(int action) {
        switch (action) {
            case 1:
                return "START_SESSION";
            case 2:
                return "VIEW_ENTERED";
            case 3:
                return "VIEW_EXITED";
            case 4:
                return "VALUE_CHANGED";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UNKNOWN_");
                stringBuilder.append(action);
                return stringBuilder.toString();
        }
    }
}
