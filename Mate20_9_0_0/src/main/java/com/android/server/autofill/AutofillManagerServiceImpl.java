package com.android.server.autofill;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.service.autofill.AutofillServiceInfo;
import android.service.autofill.FieldClassification;
import android.service.autofill.FieldClassification.Match;
import android.service.autofill.FillEventHistory;
import android.service.autofill.FillEventHistory.Event;
import android.service.autofill.FillResponse;
import android.service.autofill.UserData;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManagerClient;
import android.view.autofill.IAutoFillManagerClient.Stub;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.server.LocalServices;
import com.android.server.autofill.ui.AutoFillUI;
import com.android.server.job.controllers.JobStatus;
import com.android.server.os.HwBootFail;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class AutofillManagerServiceImpl {
    private static final int MAX_ABANDONED_SESSION_MILLIS = 30000;
    private static final int MAX_SESSION_ID_CREATE_TRIES = 2048;
    private static final String TAG = "AutofillManagerServiceImpl";
    private static final Random sRandom = new Random();
    private final AutofillCompatState mAutofillCompatState;
    @GuardedBy("mLock")
    private RemoteCallbackList<IAutoFillManagerClient> mClients;
    private final Context mContext;
    @GuardedBy("mLock")
    private boolean mDisabled;
    @GuardedBy("mLock")
    private ArrayMap<ComponentName, Long> mDisabledActivities;
    @GuardedBy("mLock")
    private ArrayMap<String, Long> mDisabledApps;
    @GuardedBy("mLock")
    private FillEventHistory mEventHistory;
    private final FieldClassificationStrategy mFieldClassificationStrategy;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), null, true);
    @GuardedBy("mLock")
    private AutofillServiceInfo mInfo;
    private long mLastPrune = 0;
    private final Object mLock;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final LocalLog mRequestsHistory;
    @GuardedBy("mLock")
    private final SparseArray<Session> mSessions = new SparseArray();
    @GuardedBy("mLock")
    private boolean mSetupComplete;
    private final AutoFillUI mUi;
    private final LocalLog mUiLatencyHistory;
    @GuardedBy("mLock")
    private UserData mUserData;
    private final int mUserId;
    private final LocalLog mWtfHistory;

    private class PruneTask extends AsyncTask<Void, Void, Void> {
        private PruneTask() {
        }

        protected Void doInBackground(Void... ignored) {
            int numSessionsToRemove;
            SparseArray<IBinder> sessionsToRemove;
            int i;
            int i2;
            synchronized (AutofillManagerServiceImpl.this.mLock) {
                numSessionsToRemove = AutofillManagerServiceImpl.this.mSessions.size();
                sessionsToRemove = new SparseArray(numSessionsToRemove);
                i = 0;
                for (i2 = 0; i2 < numSessionsToRemove; i2++) {
                    Session session = (Session) AutofillManagerServiceImpl.this.mSessions.valueAt(i2);
                    sessionsToRemove.put(session.id, session.getActivityTokenLocked());
                }
            }
            int numSessionsToRemove2 = numSessionsToRemove;
            SparseArray<IBinder> sessionsToRemove2 = sessionsToRemove;
            IActivityManager am = ActivityManager.getService();
            i2 = numSessionsToRemove2;
            numSessionsToRemove2 = 0;
            while (numSessionsToRemove2 < i2) {
                try {
                    if (am.getActivityClassForToken((IBinder) sessionsToRemove2.valueAt(numSessionsToRemove2)) != null) {
                        sessionsToRemove2.removeAt(numSessionsToRemove2);
                        numSessionsToRemove2--;
                        i2--;
                    }
                } catch (RemoteException e) {
                    Slog.w(AutofillManagerServiceImpl.TAG, "Cannot figure out if activity is finished", e);
                }
                numSessionsToRemove2++;
            }
            synchronized (AutofillManagerServiceImpl.this.mLock) {
                while (true) {
                    numSessionsToRemove2 = i;
                    if (numSessionsToRemove2 < i2) {
                        Session sessionToRemove = (Session) AutofillManagerServiceImpl.this.mSessions.get(sessionsToRemove2.keyAt(numSessionsToRemove2));
                        if (sessionToRemove != null && sessionsToRemove2.valueAt(numSessionsToRemove2) == sessionToRemove.getActivityTokenLocked()) {
                            String str;
                            StringBuilder stringBuilder;
                            if (!sessionToRemove.isSavingLocked()) {
                                if (Helper.sDebug) {
                                    str = AutofillManagerServiceImpl.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Prune session ");
                                    stringBuilder.append(sessionToRemove.id);
                                    stringBuilder.append(" (");
                                    stringBuilder.append(sessionToRemove.getActivityTokenLocked());
                                    stringBuilder.append(")");
                                    Slog.i(str, stringBuilder.toString());
                                }
                                sessionToRemove.removeSelfLocked();
                            } else if (Helper.sVerbose) {
                                str = AutofillManagerServiceImpl.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Session ");
                                stringBuilder.append(sessionToRemove.id);
                                stringBuilder.append(" is saving");
                                Slog.v(str, stringBuilder.toString());
                            }
                        }
                        i = numSessionsToRemove2 + 1;
                    }
                }
            }
            return null;
        }
    }

    AutofillManagerServiceImpl(Context context, Object lock, LocalLog requestsHistory, LocalLog uiLatencyHistory, LocalLog wtfHistory, int userId, AutoFillUI ui, AutofillCompatState autofillCompatState, boolean disabled) {
        this.mContext = context;
        this.mLock = lock;
        this.mRequestsHistory = requestsHistory;
        this.mUiLatencyHistory = uiLatencyHistory;
        this.mWtfHistory = wtfHistory;
        this.mUserId = userId;
        this.mUi = ui;
        this.mFieldClassificationStrategy = new FieldClassificationStrategy(context, userId);
        this.mAutofillCompatState = autofillCompatState;
        updateLocked(disabled);
    }

    CharSequence getServiceName() {
        String packageName = getServicePackageName();
        if (packageName == null) {
            return null;
        }
        try {
            PackageManager pm = this.mContext.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, null));
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not get label for ");
            stringBuilder.append(packageName);
            stringBuilder.append(": ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return packageName;
        }
    }

    @GuardedBy("mLock")
    private int getServiceUidLocked() {
        if (this.mInfo != null) {
            return this.mInfo.getServiceInfo().applicationInfo.uid;
        }
        Slog.w(TAG, "getServiceUidLocked(): no mInfo");
        return -1;
    }

    String[] getUrlBarResourceIdsForCompatMode(String packageName) {
        return this.mAutofillCompatState.getUrlBarResourceIds(packageName, this.mUserId);
    }

    String getServicePackageName() {
        ComponentName serviceComponent = getServiceComponentName();
        if (serviceComponent != null) {
            return serviceComponent.getPackageName();
        }
        return null;
    }

    ComponentName getServiceComponentName() {
        synchronized (this.mLock) {
            if (this.mInfo == null) {
                return null;
            }
            ComponentName componentName = this.mInfo.getServiceInfo().getComponentName();
            return componentName;
        }
    }

    private boolean isSetupCompletedLocked() {
        return "1".equals(Secure.getStringForUser(this.mContext.getContentResolver(), "user_setup_complete", this.mUserId));
    }

    private String getComponentNameFromSettings() {
        return Secure.getStringForUser(this.mContext.getContentResolver(), "autofill_service", this.mUserId);
    }

    /* JADX WARNING: Removed duplicated region for block: B:9:0x0083 A:{Splitter: B:5:0x005a, ExcHandler: java.lang.RuntimeException (r4_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:9:0x0083, code:
            r4 = move-exception;
     */
    /* JADX WARNING: Missing block: B:10:0x0084, code:
            r6 = TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("Error getting service info for '");
            r7.append(r3);
            r7.append("': ");
            r7.append(r4);
            android.util.Slog.e(r6, r7.toString());
            r2 = null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mLock")
    void updateLocked(boolean disabled) {
        Exception e;
        boolean wasEnabled = isEnabledLocked();
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateLocked(u=");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("): wasEnabled=");
            stringBuilder.append(wasEnabled);
            stringBuilder.append(", mSetupComplete= ");
            stringBuilder.append(this.mSetupComplete);
            stringBuilder.append(", disabled=");
            stringBuilder.append(disabled);
            stringBuilder.append(", mDisabled=");
            stringBuilder.append(this.mDisabled);
            Slog.v(str, stringBuilder.toString());
        }
        this.mSetupComplete = isSetupCompletedLocked();
        this.mDisabled = disabled;
        ComponentName serviceComponent = null;
        ServiceInfo serviceInfo = null;
        String componentName = getComponentNameFromSettings();
        if (!TextUtils.isEmpty(componentName)) {
            try {
                serviceComponent = ComponentName.unflattenFromString(componentName);
                serviceInfo = AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 0, this.mUserId);
                if (serviceInfo == null) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Bad AutofillService name: ");
                    stringBuilder2.append(componentName);
                    Slog.e(str2, stringBuilder2.toString());
                }
            } catch (Exception e2) {
            }
        }
        StringBuilder stringBuilder3;
        if (serviceInfo != null) {
            try {
                this.mInfo = new AutofillServiceInfo(this.mContext, serviceComponent, this.mUserId);
                if (Helper.sDebug) {
                    String str3 = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Set component for user ");
                    stringBuilder3.append(this.mUserId);
                    stringBuilder3.append(" as ");
                    stringBuilder3.append(this.mInfo);
                    Slog.d(str3, stringBuilder3.toString());
                }
            } catch (Exception e3) {
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Bad AutofillServiceInfo for '");
                stringBuilder4.append(componentName);
                stringBuilder4.append("': ");
                stringBuilder4.append(e3);
                Slog.e(str4, stringBuilder4.toString());
                this.mInfo = null;
            }
        } else {
            this.mInfo = null;
            if (Helper.sDebug) {
                e3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Reset component for user ");
                stringBuilder3.append(this.mUserId);
                stringBuilder3.append(" (");
                stringBuilder3.append(componentName);
                stringBuilder3.append(")");
                Slog.d(e3, stringBuilder3.toString());
            }
        }
        boolean isEnabled = isEnabledLocked();
        if (wasEnabled != isEnabled) {
            if (!isEnabled) {
                for (int i = this.mSessions.size() - 1; i >= 0; i--) {
                    ((Session) this.mSessions.valueAt(i)).removeSelfLocked();
                }
            }
            sendStateToClients(false);
        }
    }

    @GuardedBy("mLock")
    boolean addClientLocked(IAutoFillManagerClient client) {
        if (this.mClients == null) {
            this.mClients = new RemoteCallbackList();
        }
        this.mClients.register(client);
        return isEnabledLocked();
    }

    @GuardedBy("mLock")
    void removeClientLocked(IAutoFillManagerClient client) {
        if (this.mClients != null) {
            this.mClients.unregister(client);
        }
    }

    @GuardedBy("mLock")
    void setAuthenticationResultLocked(Bundle data, int sessionId, int authenticationId, int uid) {
        if (isEnabledLocked()) {
            Session session = (Session) this.mSessions.get(sessionId);
            if (session != null && uid == session.uid) {
                session.setAuthenticationResultLocked(data, authenticationId);
            }
        }
    }

    void setHasCallback(int sessionId, int uid, boolean hasIt) {
        if (isEnabledLocked()) {
            Session session = (Session) this.mSessions.get(sessionId);
            if (session != null && uid == session.uid) {
                synchronized (this.mLock) {
                    session.setHasCallbackLocked(hasIt);
                }
            }
        }
    }

    @GuardedBy("mLock")
    int startSessionLocked(IBinder activityToken, int uid, IBinder appCallbackToken, AutofillId autofillId, Rect virtualBounds, AutofillValue value, boolean hasCallback, ComponentName componentName, boolean compatMode, boolean bindInstantServiceAllowed, int flags) {
        int i = flags;
        if (!isEnabledLocked()) {
            return 0;
        }
        String shortComponentName = componentName.toShortString();
        ComponentName componentName2 = componentName;
        String str;
        StringBuilder stringBuilder;
        if (isAutofillDisabledLocked(componentName2)) {
            if (Helper.sDebug) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("startSession(");
                stringBuilder.append(shortComponentName);
                stringBuilder.append("): ignored because disabled by service");
                Slog.d(str, stringBuilder.toString());
            }
            try {
                Stub.asInterface(appCallbackToken).setSessionFinished(4);
            } catch (RemoteException e) {
                RemoteException remoteException = e;
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Could not notify ");
                stringBuilder2.append(shortComponentName);
                stringBuilder2.append(" that it's disabled: ");
                stringBuilder2.append(e);
                Slog.w(str2, stringBuilder2.toString());
            }
            return Integer.MIN_VALUE;
        }
        IBinder iBinder;
        if (Helper.sVerbose) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startSession(): token=");
            iBinder = activityToken;
            stringBuilder.append(iBinder);
            stringBuilder.append(", flags=");
            stringBuilder.append(i);
            Slog.v(str, stringBuilder.toString());
        } else {
            iBinder = activityToken;
        }
        pruneAbandonedSessionsLocked();
        Session newSession = createSessionByTokenLocked(iBinder, uid, appCallbackToken, hasCallback, componentName2, compatMode, bindInstantServiceAllowed, i);
        if (newSession == null) {
            return Integer.MIN_VALUE;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("id=");
        stringBuilder.append(newSession.id);
        stringBuilder.append(" uid=");
        stringBuilder.append(uid);
        stringBuilder.append(" a=");
        stringBuilder.append(shortComponentName);
        stringBuilder.append(" s=");
        stringBuilder.append(this.mInfo.getServiceInfo().packageName);
        stringBuilder.append(" u=");
        stringBuilder.append(this.mUserId);
        stringBuilder.append(" i=");
        AutofillId autofillId2 = autofillId;
        stringBuilder.append(autofillId2);
        stringBuilder.append(" b=");
        Rect rect = virtualBounds;
        stringBuilder.append(rect);
        stringBuilder.append(" hc=");
        stringBuilder.append(hasCallback);
        stringBuilder.append(" f=");
        stringBuilder.append(i);
        String historyItem = stringBuilder.toString();
        this.mRequestsHistory.log(historyItem);
        newSession.updateLocked(autofillId2, rect, value, 1, i);
        return newSession.id;
    }

    @GuardedBy("mLock")
    private void pruneAbandonedSessionsLocked() {
        long now = System.currentTimeMillis();
        if (this.mLastPrune < now - 30000) {
            this.mLastPrune = now;
            if (this.mSessions.size() > 0) {
                new PruneTask().execute(new Void[0]);
            }
        }
    }

    @GuardedBy("mLock")
    void setAutofillFailureLocked(int sessionId, int uid, List<AutofillId> ids) {
        if (isEnabledLocked()) {
            Session session = (Session) this.mSessions.get(sessionId);
            if (session == null || uid != session.uid) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setAutofillFailure(): no session for ");
                stringBuilder.append(sessionId);
                stringBuilder.append("(");
                stringBuilder.append(uid);
                stringBuilder.append(")");
                Slog.v(str, stringBuilder.toString());
                return;
            }
            session.setAutofillFailureLocked(ids);
        }
    }

    @GuardedBy("mLock")
    void finishSessionLocked(int sessionId, int uid) {
        if (isEnabledLocked()) {
            Session session = (Session) this.mSessions.get(sessionId);
            if (session == null || uid != session.uid) {
                if (Helper.sVerbose) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("finishSessionLocked(): no session for ");
                    stringBuilder.append(sessionId);
                    stringBuilder.append("(");
                    stringBuilder.append(uid);
                    stringBuilder.append(")");
                    Slog.v(str, stringBuilder.toString());
                }
                return;
            }
            session.logContextCommitted();
            boolean finished = session.showSaveLocked();
            if (Helper.sVerbose) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("finishSessionLocked(): session finished on save? ");
                stringBuilder2.append(finished);
                Slog.v(str2, stringBuilder2.toString());
            }
            if (finished) {
                session.removeSelfLocked();
            }
        }
    }

    @GuardedBy("mLock")
    void cancelSessionLocked(int sessionId, int uid) {
        if (isEnabledLocked()) {
            Session session = (Session) this.mSessions.get(sessionId);
            if (session == null || uid != session.uid) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cancelSessionLocked(): no session for ");
                stringBuilder.append(sessionId);
                stringBuilder.append("(");
                stringBuilder.append(uid);
                stringBuilder.append(")");
                Slog.w(str, stringBuilder.toString());
                return;
            }
            session.removeSelfLocked();
        }
    }

    @GuardedBy("mLock")
    void disableOwnedAutofillServicesLocked(int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disableOwnedServices(");
        stringBuilder.append(uid);
        stringBuilder.append("): ");
        stringBuilder.append(this.mInfo);
        Slog.i(str, stringBuilder.toString());
        if (this.mInfo != null) {
            ServiceInfo serviceInfo = this.mInfo.getServiceInfo();
            if (serviceInfo.applicationInfo.uid != uid) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("disableOwnedServices(): ignored when called by UID ");
                stringBuilder2.append(uid);
                stringBuilder2.append(" instead of ");
                stringBuilder2.append(serviceInfo.applicationInfo.uid);
                stringBuilder2.append(" for service ");
                stringBuilder2.append(this.mInfo);
                Slog.w(str2, stringBuilder2.toString());
                return;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                String autoFillService = getComponentNameFromSettings();
                ComponentName componentName = serviceInfo.getComponentName();
                if (componentName.equals(ComponentName.unflattenFromString(autoFillService))) {
                    this.mMetricsLogger.action(1135, componentName.getPackageName());
                    Secure.putStringForUser(this.mContext.getContentResolver(), "autofill_service", null, this.mUserId);
                    destroySessionsLocked();
                } else {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("disableOwnedServices(): ignored because current service (");
                    stringBuilder3.append(serviceInfo);
                    stringBuilder3.append(") does not match Settings (");
                    stringBuilder3.append(autoFillService);
                    stringBuilder3.append(")");
                    Slog.w(str3, stringBuilder3.toString());
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    @GuardedBy("mLock")
    private Session createSessionByTokenLocked(IBinder activityToken, int uid, IBinder appCallbackToken, boolean hasCallback, ComponentName componentName, boolean compatMode, boolean bindInstantServiceAllowed, int flags) {
        AutofillManagerServiceImpl autofillManagerServiceImpl = this;
        int tries = 0;
        while (true) {
            int tries2 = tries + 1;
            if (tries2 > 2048) {
                Slog.w(TAG, "Cannot create session in 2048 tries");
                return null;
            }
            int sessionId = sRandom.nextInt();
            if (sessionId == Integer.MIN_VALUE || autofillManagerServiceImpl.mSessions.indexOfKey(sessionId) >= 0) {
                autofillManagerServiceImpl = autofillManagerServiceImpl;
                tries = tries2;
            } else {
                autofillManagerServiceImpl.assertCallerLocked(componentName, compatMode);
                AutoFillUI autoFillUI = autofillManagerServiceImpl.mUi;
                Context context = autofillManagerServiceImpl.mContext;
                Handler handler = autofillManagerServiceImpl.mHandler;
                int i = autofillManagerServiceImpl.mUserId;
                Object obj = autofillManagerServiceImpl.mLock;
                LocalLog localLog = autofillManagerServiceImpl.mUiLatencyHistory;
                LocalLog localLog2 = localLog;
                Session newSession = new Session(autofillManagerServiceImpl, autoFillUI, context, handler, i, obj, sessionId, uid, activityToken, appCallbackToken, hasCallback, localLog2, autofillManagerServiceImpl.mWtfHistory, autofillManagerServiceImpl.mInfo.getServiceInfo().getComponentName(), componentName, compatMode, bindInstantServiceAllowed, flags);
                this.mSessions.put(newSession.id, newSession);
                return newSession;
            }
        }
    }

    private void assertCallerLocked(ComponentName componentName, boolean compatMode) {
        String packageName = componentName.getPackageName();
        PackageManager pm = this.mContext.getPackageManager();
        int callingUid = Binder.getCallingUid();
        try {
            int packageUid = pm.getPackageUidAsUser(packageName, UserHandle.getCallingUserId());
            if (callingUid != packageUid && !((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).hasRunningActivity(callingUid, packageName)) {
                String callingPackage;
                String[] packages = pm.getPackagesForUid(callingUid);
                if (packages != null) {
                    callingPackage = packages[0];
                } else {
                    callingPackage = new StringBuilder();
                    callingPackage.append("uid-");
                    callingPackage.append(callingUid);
                    callingPackage = callingPackage.toString();
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("App (package=");
                stringBuilder.append(callingPackage);
                stringBuilder.append(", UID=");
                stringBuilder.append(callingUid);
                stringBuilder.append(") passed component (");
                stringBuilder.append(componentName);
                stringBuilder.append(") owned by UID ");
                stringBuilder.append(packageUid);
                Slog.w(str, stringBuilder.toString());
                LogMaker log = new LogMaker(948).setPackageName(callingPackage).addTaggedData(908, getServicePackageName()).addTaggedData(949, componentName == null ? "null" : componentName.flattenToShortString());
                if (compatMode) {
                    log.addTaggedData(1414, Integer.valueOf(1));
                }
                this.mMetricsLogger.write(log);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid component: ");
                stringBuilder2.append(componentName);
                throw new SecurityException(stringBuilder2.toString());
            }
        } catch (NameNotFoundException e) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Could not verify UID for ");
            stringBuilder3.append(componentName);
            throw new SecurityException(stringBuilder3.toString());
        }
    }

    boolean restoreSession(int sessionId, int uid, IBinder activityToken, IBinder appCallback) {
        Session session = (Session) this.mSessions.get(sessionId);
        if (session == null || uid != session.uid) {
            return false;
        }
        session.switchActivity(activityToken, appCallback);
        return true;
    }

    @GuardedBy("mLock")
    boolean updateSessionLocked(int sessionId, int uid, AutofillId autofillId, Rect virtualBounds, AutofillValue value, int action, int flags) {
        Session session = (Session) this.mSessions.get(sessionId);
        String str;
        StringBuilder stringBuilder;
        if (session != null && session.uid == uid) {
            session.updateLocked(autofillId, virtualBounds, value, action, flags);
            return false;
        } else if ((flags & 1) != 0) {
            if (Helper.sDebug) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("restarting session ");
                stringBuilder.append(sessionId);
                stringBuilder.append(" due to manual request on ");
                stringBuilder.append(autofillId);
                Slog.d(str, stringBuilder.toString());
            }
            return true;
        } else {
            if (Helper.sVerbose) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateSessionLocked(): session gone for ");
                stringBuilder.append(sessionId);
                stringBuilder.append("(");
                stringBuilder.append(uid);
                stringBuilder.append(")");
                Slog.v(str, stringBuilder.toString());
            }
            return false;
        }
    }

    @GuardedBy("mLock")
    void removeSessionLocked(int sessionId) {
        this.mSessions.remove(sessionId);
    }

    void handleSessionSave(Session session) {
        synchronized (this.mLock) {
            if (this.mSessions.get(session.id) == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleSessionSave(): already gone: ");
                stringBuilder.append(session.id);
                Slog.w(str, stringBuilder.toString());
                return;
            }
            session.callSaveLocked();
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0049, code:
            if (com.android.server.autofill.Helper.sDebug == false) goto L_0x0071;
     */
    /* JADX WARNING: Missing block: B:17:0x004b, code:
            r0 = TAG;
            r1 = new java.lang.StringBuilder();
            r1.append("No pending Save UI for token ");
            r1.append(r7);
            r1.append(" and operation ");
            r1.append(android.util.DebugUtils.flagsToString(android.view.autofill.AutofillManager.class, "PENDING_UI_OPERATION_", r6));
            android.util.Slog.d(r0, r1.toString());
     */
    /* JADX WARNING: Missing block: B:18:0x0071, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void onPendingSaveUi(int operation, IBinder token) {
        if (Helper.sVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPendingSaveUi(");
            stringBuilder.append(operation);
            stringBuilder.append("): ");
            stringBuilder.append(token);
            Slog.v(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            for (int i = this.mSessions.size() - 1; i >= 0; i--) {
                Session session = (Session) this.mSessions.valueAt(i);
                if (session.isSaveUiPendingForTokenLocked(token)) {
                    session.onPendingSaveUi(operation, token);
                    return;
                }
            }
        }
    }

    @GuardedBy("mLock")
    void handlePackageUpdateLocked(String packageName) {
        ServiceInfo serviceInfo = this.mFieldClassificationStrategy.getServiceInfo();
        if (serviceInfo != null && serviceInfo.packageName.equals(packageName)) {
            resetExtServiceLocked();
        }
    }

    @GuardedBy("mLock")
    void resetExtServiceLocked() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "reset autofill service.");
        }
        this.mFieldClassificationStrategy.reset();
    }

    @GuardedBy("mLock")
    void destroyLocked() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "destroyLocked()");
        }
        resetExtServiceLocked();
        int numSessions = this.mSessions.size();
        ArraySet<RemoteFillService> remoteFillServices = new ArraySet(numSessions);
        int i = 0;
        for (int i2 = 0; i2 < numSessions; i2++) {
            RemoteFillService remoteFillService = ((Session) this.mSessions.valueAt(i2)).destroyLocked();
            if (remoteFillService != null) {
                remoteFillServices.add(remoteFillService);
            }
        }
        this.mSessions.clear();
        while (i < remoteFillServices.size()) {
            ((RemoteFillService) remoteFillServices.valueAt(i)).destroy();
            i++;
        }
        sendStateToClients(true);
        if (this.mClients != null) {
            this.mClients.kill();
            this.mClients = null;
        }
    }

    CharSequence getServiceLabel() {
        return this.mInfo.getServiceInfo().loadSafeLabel(this.mContext.getPackageManager(), 0.0f, 5);
    }

    Drawable getServiceIcon() {
        return this.mInfo.getServiceInfo().loadIcon(this.mContext.getPackageManager());
    }

    void setLastResponse(int sessionId, FillResponse response) {
        synchronized (this.mLock) {
            this.mEventHistory = new FillEventHistory(sessionId, response.getClientState());
        }
    }

    void resetLastResponse() {
        synchronized (this.mLock) {
            this.mEventHistory = null;
        }
    }

    @GuardedBy("mLock")
    private boolean isValidEventLocked(String method, int sessionId) {
        String str;
        StringBuilder stringBuilder;
        if (this.mEventHistory == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(method);
            stringBuilder.append(": not logging event because history is null");
            Slog.w(str, stringBuilder.toString());
            return false;
        } else if (sessionId == this.mEventHistory.getSessionId()) {
            return true;
        } else {
            if (Helper.sDebug) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(method);
                stringBuilder.append(": not logging event for session ");
                stringBuilder.append(sessionId);
                stringBuilder.append(" because tracked session is ");
                stringBuilder.append(this.mEventHistory.getSessionId());
                Slog.d(str, stringBuilder.toString());
            }
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:10:0x002c, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void setAuthenticationSelected(int sessionId, Bundle clientState) {
        Throwable th;
        synchronized (this.mLock) {
            try {
                if (isValidEventLocked("setAuthenticationSelected()", sessionId)) {
                    FillEventHistory fillEventHistory = this.mEventHistory;
                    Event event = r4;
                    Event event2 = new Event(2, null, clientState, null, null, null, null, null, null, null, null);
                    fillEventHistory.addEvent(event);
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x002d, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void logDatasetAuthenticationSelected(String selectedDataset, int sessionId, Bundle clientState) {
        Throwable th;
        synchronized (this.mLock) {
            try {
                if (isValidEventLocked("logDatasetAuthenticationSelected()", sessionId)) {
                    FillEventHistory fillEventHistory = this.mEventHistory;
                    Event event = r4;
                    Event event2 = new Event(1, selectedDataset, clientState, null, null, null, null, null, null, null, null);
                    fillEventHistory.addEvent(event);
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x002c, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void logSaveShown(int sessionId, Bundle clientState) {
        Throwable th;
        synchronized (this.mLock) {
            try {
                if (isValidEventLocked("logSaveShown()", sessionId)) {
                    FillEventHistory fillEventHistory = this.mEventHistory;
                    Event event = r4;
                    Event event2 = new Event(3, null, clientState, null, null, null, null, null, null, null, null);
                    fillEventHistory.addEvent(event);
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x002d, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void logDatasetSelected(String selectedDataset, int sessionId, Bundle clientState) {
        Throwable th;
        synchronized (this.mLock) {
            try {
                if (isValidEventLocked("logDatasetSelected()", sessionId)) {
                    FillEventHistory fillEventHistory = this.mEventHistory;
                    Event event = r4;
                    Event event2 = new Event(0, selectedDataset, clientState, null, null, null, null, null, null, null, null);
                    fillEventHistory.addEvent(event);
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    @GuardedBy("mLock")
    void logContextCommittedLocked(int sessionId, Bundle clientState, ArrayList<String> selectedDatasets, ArraySet<String> ignoredDatasets, ArrayList<AutofillId> changedFieldIds, ArrayList<String> changedDatasetIds, ArrayList<AutofillId> manuallyFilledFieldIds, ArrayList<ArrayList<String>> manuallyFilledDatasetIds, ComponentName appComponentName, boolean compatMode) {
        logContextCommittedLocked(sessionId, clientState, selectedDatasets, ignoredDatasets, changedFieldIds, changedDatasetIds, manuallyFilledFieldIds, manuallyFilledDatasetIds, null, null, appComponentName, compatMode);
    }

    @GuardedBy("mLock")
    void logContextCommittedLocked(int sessionId, Bundle clientState, ArrayList<String> selectedDatasets, ArraySet<String> ignoredDatasets, ArrayList<AutofillId> changedFieldIds, ArrayList<String> changedDatasetIds, ArrayList<AutofillId> manuallyFilledFieldIds, ArrayList<ArrayList<String>> manuallyFilledDatasetIds, ArrayList<AutofillId> detectedFieldIdsList, ArrayList<FieldClassification> detectedFieldClassificationsList, ComponentName appComponentName, boolean compatMode) {
        int i = sessionId;
        ArrayList<AutofillId> arrayList = detectedFieldIdsList;
        ArrayList<FieldClassification> arrayList2 = detectedFieldClassificationsList;
        boolean z = compatMode;
        if (isValidEventLocked("logDatasetNotSelected()", i)) {
            List list;
            ArraySet arraySet;
            if (Helper.sVerbose) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("logContextCommitted() with FieldClassification: id=");
                stringBuilder.append(i);
                stringBuilder.append(", selectedDatasets=");
                list = selectedDatasets;
                stringBuilder.append(list);
                stringBuilder.append(", ignoredDatasetIds=");
                arraySet = ignoredDatasets;
                stringBuilder.append(arraySet);
                stringBuilder.append(", changedAutofillIds=");
                stringBuilder.append(changedFieldIds);
                stringBuilder.append(", changedDatasetIds=");
                stringBuilder.append(changedDatasetIds);
                stringBuilder.append(", manuallyFilledFieldIds=");
                stringBuilder.append(manuallyFilledFieldIds);
                stringBuilder.append(", detectedFieldIds=");
                stringBuilder.append(arrayList);
                stringBuilder.append(", detectedFieldClassifications=");
                stringBuilder.append(arrayList2);
                stringBuilder.append(", appComponentName=");
                stringBuilder.append(appComponentName.toShortString());
                stringBuilder.append(", compatMode=");
                stringBuilder.append(z);
                Slog.v(str, stringBuilder.toString());
            } else {
                list = selectedDatasets;
                arraySet = ignoredDatasets;
                ArrayList<AutofillId> arrayList3 = changedFieldIds;
                ArrayList<String> arrayList4 = changedDatasetIds;
                ArrayList<AutofillId> arrayList5 = manuallyFilledFieldIds;
            }
            AutofillId[] detectedFieldsIds = null;
            FieldClassification[] detectedFieldClassifications = null;
            ComponentName componentName;
            if (arrayList != null) {
                AutofillId[] detectedFieldsIds2;
                detectedFieldsIds = new AutofillId[detectedFieldIdsList.size()];
                arrayList.toArray(detectedFieldsIds);
                detectedFieldClassifications = new FieldClassification[detectedFieldClassificationsList.size()];
                arrayList2.toArray(detectedFieldClassifications);
                int numberFields = detectedFieldsIds.length;
                float totalScore = 0.0f;
                int totalSize = 0;
                int i2 = 0;
                while (i2 < numberFields) {
                    List<Match> matches = detectedFieldClassifications[i2].getMatches();
                    int size = matches.size();
                    totalSize += size;
                    float totalScore2 = totalScore;
                    int j = 0;
                    while (true) {
                        detectedFieldsIds2 = detectedFieldsIds;
                        detectedFieldsIds = j;
                        if (detectedFieldsIds >= size) {
                            break;
                        }
                        totalScore2 += ((Match) matches.get(detectedFieldsIds)).getScore();
                        j = detectedFieldsIds + 1;
                        detectedFieldsIds = detectedFieldsIds2;
                        matches = matches;
                    }
                    i2++;
                    totalScore = totalScore2;
                    detectedFieldsIds = detectedFieldsIds2;
                    arrayList = detectedFieldIdsList;
                    arrayList2 = detectedFieldClassificationsList;
                }
                detectedFieldsIds2 = detectedFieldsIds;
                componentName = appComponentName;
                this.mMetricsLogger.write(Helper.newLogMaker(1273, componentName, getServicePackageName(), i, z).setCounterValue(numberFields).addTaggedData(1274, Integer.valueOf((int) ((100.0f * totalScore) / ((float) totalSize)))));
                detectedFieldsIds = detectedFieldsIds2;
            } else {
                componentName = appComponentName;
            }
            this.mEventHistory.addEvent(new Event(4, null, clientState, list, arraySet, changedFieldIds, changedDatasetIds, manuallyFilledFieldIds, manuallyFilledDatasetIds, detectedFieldsIds, detectedFieldClassifications));
            return;
        }
        ArrayList<String> arrayList6 = selectedDatasets;
    }

    /* JADX WARNING: Missing block: B:12:0x0015, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    FillEventHistory getFillEventHistory(int callingUid) {
        synchronized (this.mLock) {
            if (this.mEventHistory == null || !isCalledByServiceLocked("getFillEventHistory", callingUid)) {
            } else {
                FillEventHistory fillEventHistory = this.mEventHistory;
                return fillEventHistory;
            }
        }
    }

    UserData getUserData() {
        UserData userData;
        synchronized (this.mLock) {
            userData = this.mUserData;
        }
        return userData;
    }

    UserData getUserData(int callingUid) {
        synchronized (this.mLock) {
            if (isCalledByServiceLocked("getUserData", callingUid)) {
                UserData userData = this.mUserData;
                return userData;
            }
            return null;
        }
    }

    void setUserData(int callingUid, UserData userData) {
        synchronized (this.mLock) {
            if (isCalledByServiceLocked("setUserData", callingUid)) {
                this.mUserData = userData;
                this.mMetricsLogger.write(new LogMaker(1272).setPackageName(getServicePackageName()).addTaggedData(914, Integer.valueOf(this.mUserData == null ? 0 : this.mUserData.getCategoryIds().length)));
                return;
            }
        }
    }

    @GuardedBy("mLock")
    private boolean isCalledByServiceLocked(String methodName, int callingUid) {
        if (getServiceUidLocked() == callingUid) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(methodName);
        stringBuilder.append("() called by UID ");
        stringBuilder.append(callingUid);
        stringBuilder.append(", but service UID is ");
        stringBuilder.append(getServiceUidLocked());
        Slog.w(str, stringBuilder.toString());
        return false;
    }

    @GuardedBy("mLock")
    void dumpLocked(String prefix, PrintWriter pw) {
        int size;
        int i;
        String str = prefix;
        PrintWriter printWriter = pw;
        String prefix2 = new StringBuilder();
        prefix2.append(str);
        prefix2.append("  ");
        prefix2 = prefix2.toString();
        printWriter.print(str);
        printWriter.print("User: ");
        printWriter.println(this.mUserId);
        printWriter.print(str);
        printWriter.print("UID: ");
        printWriter.println(getServiceUidLocked());
        printWriter.print(str);
        printWriter.print("Autofill Service Info: ");
        if (this.mInfo == null) {
            printWriter.println("N/A");
        } else {
            pw.println();
            this.mInfo.dump(prefix2, printWriter);
            printWriter.print(str);
            printWriter.print("Service Label: ");
            printWriter.println(getServiceLabel());
        }
        printWriter.print(str);
        printWriter.print("Component from settings: ");
        printWriter.println(getComponentNameFromSettings());
        printWriter.print(str);
        printWriter.print("Default component: ");
        printWriter.println(this.mContext.getString(17039783));
        printWriter.print(str);
        printWriter.print("Disabled: ");
        printWriter.println(this.mDisabled);
        printWriter.print(str);
        printWriter.print("Field classification enabled: ");
        printWriter.println(isFieldClassificationEnabledLocked());
        printWriter.print(str);
        printWriter.print("Compat pkgs: ");
        ArrayMap<String, Long> compatPkgs = getCompatibilityPackagesLocked();
        if (compatPkgs == null) {
            printWriter.println("N/A");
        } else {
            printWriter.println(compatPkgs);
        }
        printWriter.print(str);
        printWriter.print("Setup complete: ");
        printWriter.println(this.mSetupComplete);
        printWriter.print(str);
        printWriter.print("Last prune: ");
        printWriter.println(this.mLastPrune);
        printWriter.print(str);
        printWriter.print("Disabled apps: ");
        if (this.mDisabledApps == null) {
            printWriter.println("N/A");
        } else {
            size = this.mDisabledApps.size();
            printWriter.println(size);
            StringBuilder builder = new StringBuilder();
            long now = SystemClock.elapsedRealtime();
            for (int i2 = 0; i2 < size; i2++) {
                String packageName = (String) this.mDisabledApps.keyAt(i2);
                long expiration = ((Long) this.mDisabledApps.valueAt(i2)).longValue();
                builder.append(str);
                builder.append(str);
                builder.append(i2);
                builder.append(". ");
                builder.append(packageName);
                builder.append(": ");
                TimeUtils.formatDuration(expiration - now, builder);
                builder.append(10);
            }
            printWriter.println(builder);
        }
        printWriter.print(str);
        printWriter.print("Disabled activities: ");
        if (this.mDisabledActivities == null) {
            printWriter.println("N/A");
        } else {
            size = this.mDisabledActivities.size();
            printWriter.println(size);
            StringBuilder builder2 = new StringBuilder();
            long now2 = SystemClock.elapsedRealtime();
            for (int i3 = 0; i3 < size; i3++) {
                ComponentName component = (ComponentName) this.mDisabledActivities.keyAt(i3);
                long expiration2 = ((Long) this.mDisabledActivities.valueAt(i3)).longValue();
                builder2.append(str);
                builder2.append(str);
                builder2.append(i3);
                builder2.append(". ");
                builder2.append(component);
                builder2.append(": ");
                TimeUtils.formatDuration(expiration2 - now2, builder2);
                builder2.append(10);
            }
            printWriter.println(builder2);
        }
        size = this.mSessions.size();
        if (size == 0) {
            printWriter.print(str);
            printWriter.println("No sessions");
        } else {
            printWriter.print(str);
            printWriter.print(size);
            printWriter.println(" sessions:");
            for (i = 0; i < size; i++) {
                printWriter.print(str);
                printWriter.print("#");
                printWriter.println(i + 1);
                ((Session) this.mSessions.valueAt(i)).dumpLocked(prefix2, printWriter);
            }
        }
        printWriter.print(str);
        printWriter.print("Clients: ");
        if (this.mClients == null) {
            printWriter.println("N/A");
        } else {
            pw.println();
            this.mClients.dump(printWriter, prefix2);
        }
        if (this.mEventHistory != null && this.mEventHistory.getEvents() != null && this.mEventHistory.getEvents().size() != 0) {
            printWriter.print(str);
            printWriter.println("Events of last fill response:");
            printWriter.print(str);
            i = this.mEventHistory.getEvents().size();
            int i4 = 0;
            while (true) {
                int i5 = i4;
                if (i5 >= i) {
                    break;
                }
                Event event = (Event) this.mEventHistory.getEvents().get(i5);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(i5);
                stringBuilder.append(": eventType=");
                stringBuilder.append(event.getType());
                stringBuilder.append(" datasetId=");
                stringBuilder.append(event.getDatasetId());
                printWriter.println(stringBuilder.toString());
                i4 = i5 + 1;
            }
        } else {
            printWriter.print(str);
            printWriter.println("No event on last fill response");
        }
        printWriter.print(str);
        printWriter.print("User data: ");
        if (this.mUserData == null) {
            printWriter.println("N/A");
        } else {
            pw.println();
            this.mUserData.dump(prefix2, printWriter);
        }
        printWriter.print(str);
        printWriter.println("Field Classification strategy: ");
        this.mFieldClassificationStrategy.dump(prefix2, printWriter);
    }

    @GuardedBy("mLock")
    void destroySessionsLocked() {
        if (this.mSessions.size() == 0) {
            this.mUi.destroyAll(null, null, false);
            return;
        }
        while (this.mSessions.size() > 0) {
            ((Session) this.mSessions.valueAt(0)).forceRemoveSelfLocked();
        }
    }

    @GuardedBy("mLock")
    void destroyFinishedSessionsLocked() {
        for (int i = this.mSessions.size() - 1; i >= 0; i--) {
            Session session = (Session) this.mSessions.valueAt(i);
            if (session.isSavingLocked()) {
                if (Helper.sDebug) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("destroyFinishedSessionsLocked(): ");
                    stringBuilder.append(session.id);
                    Slog.d(str, stringBuilder.toString());
                }
                session.forceRemoveSelfLocked();
            }
        }
    }

    @GuardedBy("mLock")
    void listSessionsLocked(ArrayList<String> output) {
        int numSessions = this.mSessions.size();
        for (int i = 0; i < numSessions; i++) {
            Object componentName;
            StringBuilder stringBuilder = new StringBuilder();
            if (this.mInfo != null) {
                componentName = this.mInfo.getServiceInfo().getComponentName();
            } else {
                componentName = null;
            }
            stringBuilder.append(componentName);
            stringBuilder.append(":");
            stringBuilder.append(this.mSessions.keyAt(i));
            output.add(stringBuilder.toString());
        }
    }

    @GuardedBy("mLock")
    ArrayMap<String, Long> getCompatibilityPackagesLocked() {
        if (this.mInfo != null) {
            return this.mInfo.getCompatibilityPackages();
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:9:0x0010, code:
            r3 = 0;
     */
    /* JADX WARNING: Missing block: B:10:0x0012, code:
            if (r3 >= r2) goto L_0x0058;
     */
    /* JADX WARNING: Missing block: B:12:?, code:
            r4 = (android.view.autofill.IAutoFillManagerClient) r1.getBroadcastItem(r3);
     */
    /* JADX WARNING: Missing block: B:14:?, code:
            r5 = r9.mLock;
     */
    /* JADX WARNING: Missing block: B:15:0x001c, code:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:16:0x001d, code:
            if (r10 != false) goto L_0x002a;
     */
    /* JADX WARNING: Missing block: B:19:0x0023, code:
            if (isClientSessionDestroyedLocked(r4) == false) goto L_0x0026;
     */
    /* JADX WARNING: Missing block: B:20:0x0026, code:
            r6 = false;
     */
    /* JADX WARNING: Missing block: B:22:0x002a, code:
            r6 = true;
     */
    /* JADX WARNING: Missing block: B:23:0x002b, code:
            r7 = isEnabledLocked();
     */
    /* JADX WARNING: Missing block: B:24:0x002f, code:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:25:0x0030, code:
            r5 = 0;
     */
    /* JADX WARNING: Missing block: B:26:0x0031, code:
            if (r7 == false) goto L_0x0035;
     */
    /* JADX WARNING: Missing block: B:27:0x0033, code:
            r5 = 0 | 1;
     */
    /* JADX WARNING: Missing block: B:28:0x0035, code:
            if (r6 == false) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:29:0x0037, code:
            r5 = r5 | 2;
     */
    /* JADX WARNING: Missing block: B:30:0x0039, code:
            if (r10 == false) goto L_0x003d;
     */
    /* JADX WARNING: Missing block: B:31:0x003b, code:
            r5 = r5 | 4;
     */
    /* JADX WARNING: Missing block: B:34:0x003f, code:
            if (com.android.server.autofill.Helper.sDebug == false) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:35:0x0041, code:
            r5 = r5 | 8;
     */
    /* JADX WARNING: Missing block: B:37:0x0045, code:
            if (com.android.server.autofill.Helper.sVerbose == false) goto L_0x0049;
     */
    /* JADX WARNING: Missing block: B:38:0x0047, code:
            r5 = r5 | 16;
     */
    /* JADX WARNING: Missing block: B:39:0x0049, code:
            r4.setState(r5);
     */
    /* JADX WARNING: Missing block: B:47:0x0054, code:
            r1.finishBroadcast();
     */
    /* JADX WARNING: Missing block: B:49:0x0058, code:
            r1.finishBroadcast();
     */
    /* JADX WARNING: Missing block: B:50:0x005c, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void sendStateToClients(boolean resetClient) {
        synchronized (this.mLock) {
            if (this.mClients == null) {
                return;
            } else {
                RemoteCallbackList<IAutoFillManagerClient> clients = this.mClients;
                int userClientCount = clients.beginBroadcast();
            }
        }
        int i++;
    }

    @GuardedBy("mLock")
    private boolean isClientSessionDestroyedLocked(IAutoFillManagerClient client) {
        int sessionCount = this.mSessions.size();
        for (int i = 0; i < sessionCount; i++) {
            Session session = (Session) this.mSessions.valueAt(i);
            if (session.getClient().equals(client)) {
                return session.isDestroyed();
            }
        }
        return true;
    }

    @GuardedBy("mLock")
    boolean isEnabledLocked() {
        return (!this.mSetupComplete || this.mInfo == null || this.mDisabled) ? false : true;
    }

    void disableAutofillForApp(String packageName, long duration, int sessionId, boolean compatMode) {
        synchronized (this.mLock) {
            if (this.mDisabledApps == null) {
                this.mDisabledApps = new ArrayMap(1);
            }
            long expiration = SystemClock.elapsedRealtime() + duration;
            if (expiration < 0) {
                expiration = JobStatus.NO_LATEST_RUNTIME;
            }
            this.mDisabledApps.put(packageName, Long.valueOf(expiration));
            this.mMetricsLogger.write(Helper.newLogMaker(1231, packageName, getServicePackageName(), sessionId, compatMode).addTaggedData(1145, Integer.valueOf(duration > 2147483647L ? HwBootFail.STAGE_BOOT_SUCCESS : (int) duration)));
        }
    }

    void disableAutofillForActivity(ComponentName componentName, long duration, int sessionId, boolean compatMode) {
        synchronized (this.mLock) {
            int intDuration;
            if (this.mDisabledActivities == null) {
                this.mDisabledActivities = new ArrayMap(1);
            }
            long expiration = SystemClock.elapsedRealtime() + duration;
            if (expiration < 0) {
                expiration = JobStatus.NO_LATEST_RUNTIME;
            }
            this.mDisabledActivities.put(componentName, Long.valueOf(expiration));
            if (duration > 2147483647L) {
                intDuration = HwBootFail.STAGE_BOOT_SUCCESS;
            } else {
                intDuration = (int) duration;
            }
            LogMaker log = new LogMaker(1232).setComponentName(componentName).addTaggedData(908, getServicePackageName()).addTaggedData(1145, Integer.valueOf(intDuration)).addTaggedData(1456, Integer.valueOf(sessionId));
            if (compatMode) {
                log.addTaggedData(1414, Integer.valueOf(1));
            }
            this.mMetricsLogger.write(log);
        }
    }

    @GuardedBy("mLock")
    private boolean isAutofillDisabledLocked(ComponentName componentName) {
        long elapsedTime = 0;
        if (this.mDisabledActivities != null) {
            elapsedTime = SystemClock.elapsedRealtime();
            Long expiration = (Long) this.mDisabledActivities.get(componentName);
            if (expiration != null) {
                if (expiration.longValue() >= elapsedTime) {
                    return true;
                }
                if (Helper.sVerbose) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Removing ");
                    stringBuilder.append(componentName.toShortString());
                    stringBuilder.append(" from disabled list");
                    Slog.v(str, stringBuilder.toString());
                }
                this.mDisabledActivities.remove(componentName);
            }
        }
        String packageName = componentName.getPackageName();
        if (this.mDisabledApps == null) {
            return false;
        }
        Long expiration2 = (Long) this.mDisabledApps.get(packageName);
        if (expiration2 == null) {
            return false;
        }
        if (elapsedTime == 0) {
            elapsedTime = SystemClock.elapsedRealtime();
        }
        if (expiration2.longValue() >= elapsedTime) {
            return true;
        }
        if (Helper.sVerbose) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Removing ");
            stringBuilder2.append(packageName);
            stringBuilder2.append(" from disabled list");
            Slog.v(str2, stringBuilder2.toString());
        }
        this.mDisabledApps.remove(packageName);
        return false;
    }

    boolean isFieldClassificationEnabled(int callingUid) {
        synchronized (this.mLock) {
            if (isCalledByServiceLocked("isFieldClassificationEnabled", callingUid)) {
                boolean isFieldClassificationEnabledLocked = isFieldClassificationEnabledLocked();
                return isFieldClassificationEnabledLocked;
            }
            return false;
        }
    }

    boolean isFieldClassificationEnabledLocked() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), "autofill_field_classification", 1, this.mUserId) == 1;
    }

    FieldClassificationStrategy getFieldClassificationStrategy() {
        return this.mFieldClassificationStrategy;
    }

    String[] getAvailableFieldClassificationAlgorithms(int callingUid) {
        synchronized (this.mLock) {
            if (isCalledByServiceLocked("getFCAlgorithms()", callingUid)) {
                return this.mFieldClassificationStrategy.getAvailableAlgorithms();
            }
            return null;
        }
    }

    String getDefaultFieldClassificationAlgorithm(int callingUid) {
        synchronized (this.mLock) {
            if (isCalledByServiceLocked("getDefaultFCAlgorithm()", callingUid)) {
                return this.mFieldClassificationStrategy.getDefaultAlgorithm();
            }
            return null;
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AutofillManagerServiceImpl: [userId=");
        stringBuilder.append(this.mUserId);
        stringBuilder.append(", component=");
        stringBuilder.append(this.mInfo != null ? this.mInfo.getServiceInfo().getComponentName() : null);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
