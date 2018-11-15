package com.android.server;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.SpellCheckerSubtype;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.textservice.ISpellCheckerService;
import com.android.internal.textservice.ISpellCheckerServiceCallback;
import com.android.internal.textservice.ISpellCheckerSession;
import com.android.internal.textservice.ISpellCheckerSessionListener;
import com.android.internal.textservice.ITextServicesManager.Stub;
import com.android.internal.textservice.ITextServicesSessionListener;
import com.android.internal.textservice.LazyIntToIntMap;
import com.android.internal.util.DumpUtils;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParserException;

public class TextServicesManagerService extends Stub {
    private static final boolean DBG = false;
    private static final String TAG = TextServicesManagerService.class.getSimpleName();
    private final Context mContext;
    private final Object mLock = new Object();
    private final TextServicesMonitor mMonitor;
    @GuardedBy("mLock")
    private final LazyIntToIntMap mSpellCheckerOwnerUserIdMap;
    private final SparseArray<TextServicesData> mUserData = new SparseArray();
    private final UserManager mUserManager;

    private static final class ISpellCheckerServiceCallbackBinder extends ISpellCheckerServiceCallback.Stub {
        private final SpellCheckerBindGroup mBindGroup;
        private final SessionRequest mRequest;

        ISpellCheckerServiceCallbackBinder(SpellCheckerBindGroup bindGroup, SessionRequest request) {
            this.mBindGroup = bindGroup;
            this.mRequest = request;
        }

        public void onSessionCreated(ISpellCheckerSession newSession) {
            this.mBindGroup.onSessionCreated(newSession, this.mRequest);
        }
    }

    private static final class InternalDeathRecipients extends RemoteCallbackList<ISpellCheckerSessionListener> {
        private final SpellCheckerBindGroup mGroup;

        public InternalDeathRecipients(SpellCheckerBindGroup group) {
            this.mGroup = group;
        }

        public void onCallbackDied(ISpellCheckerSessionListener listener) {
            this.mGroup.removeListener(listener);
        }
    }

    private final class InternalServiceConnection implements ServiceConnection {
        private final String mSciId;
        private final HashMap<String, SpellCheckerBindGroup> mSpellCheckerBindGroups;

        public InternalServiceConnection(String id, HashMap<String, SpellCheckerBindGroup> spellCheckerBindGroups) {
            this.mSciId = id;
            this.mSpellCheckerBindGroups = spellCheckerBindGroups;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (TextServicesManagerService.this.mLock) {
                onServiceConnectedInnerLocked(name, service);
            }
        }

        private void onServiceConnectedInnerLocked(ComponentName name, IBinder service) {
            ISpellCheckerService spellChecker = ISpellCheckerService.Stub.asInterface(service);
            SpellCheckerBindGroup group = (SpellCheckerBindGroup) this.mSpellCheckerBindGroups.get(this.mSciId);
            if (group != null && this == group.mInternalConnection) {
                group.onServiceConnectedLocked(spellChecker);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (TextServicesManagerService.this.mLock) {
                onServiceDisconnectedInnerLocked(name);
            }
        }

        private void onServiceDisconnectedInnerLocked(ComponentName name) {
            SpellCheckerBindGroup group = (SpellCheckerBindGroup) this.mSpellCheckerBindGroups.get(this.mSciId);
            if (group != null && this == group.mInternalConnection) {
                group.onServiceDisconnectedLocked();
            }
        }
    }

    private static final class SessionRequest {
        public final Bundle mBundle;
        public final String mLocale;
        public final ISpellCheckerSessionListener mScListener;
        public final ITextServicesSessionListener mTsListener;
        public final int mUid;

        SessionRequest(int uid, String locale, ITextServicesSessionListener tsListener, ISpellCheckerSessionListener scListener, Bundle bundle) {
            this.mUid = uid;
            this.mLocale = locale;
            this.mTsListener = tsListener;
            this.mScListener = scListener;
            this.mBundle = bundle;
        }
    }

    private final class SpellCheckerBindGroup {
        private final String TAG = SpellCheckerBindGroup.class.getSimpleName();
        private boolean mConnected;
        private final InternalServiceConnection mInternalConnection;
        private final InternalDeathRecipients mListeners;
        private final ArrayList<SessionRequest> mOnGoingSessionRequests = new ArrayList();
        private final ArrayList<SessionRequest> mPendingSessionRequests = new ArrayList();
        private ISpellCheckerService mSpellChecker;
        HashMap<String, SpellCheckerBindGroup> mSpellCheckerBindGroups;
        private boolean mUnbindCalled;

        public SpellCheckerBindGroup(InternalServiceConnection connection) {
            this.mInternalConnection = connection;
            this.mListeners = new InternalDeathRecipients(this);
            this.mSpellCheckerBindGroups = connection.mSpellCheckerBindGroups;
        }

        public void onServiceConnectedLocked(ISpellCheckerService spellChecker) {
            if (!this.mUnbindCalled) {
                this.mSpellChecker = spellChecker;
                this.mConnected = true;
                try {
                    int size = this.mPendingSessionRequests.size();
                    for (int i = 0; i < size; i++) {
                        SessionRequest request = (SessionRequest) this.mPendingSessionRequests.get(i);
                        this.mSpellChecker.getISpellCheckerSession(request.mLocale, request.mScListener, request.mBundle, new ISpellCheckerServiceCallbackBinder(this, request));
                        this.mOnGoingSessionRequests.add(request);
                    }
                    this.mPendingSessionRequests.clear();
                } catch (RemoteException e) {
                    removeAllLocked();
                }
                cleanLocked();
            }
        }

        public void onServiceDisconnectedLocked() {
            this.mSpellChecker = null;
            this.mConnected = false;
        }

        public void removeListener(ISpellCheckerSessionListener listener) {
            synchronized (TextServicesManagerService.this.mLock) {
                this.mListeners.unregister(listener);
                Predicate<SessionRequest> removeCondition = new -$$Lambda$TextServicesManagerService$SpellCheckerBindGroup$WPb2Qavn5gWhsY_rCdz_4UGBTAw(listener.asBinder());
                this.mPendingSessionRequests.removeIf(removeCondition);
                this.mOnGoingSessionRequests.removeIf(removeCondition);
                cleanLocked();
            }
        }

        static /* synthetic */ boolean lambda$removeListener$0(IBinder scListenerBinder, SessionRequest request) {
            return request.mScListener.asBinder() == scListenerBinder;
        }

        private void cleanLocked() {
            if (!this.mUnbindCalled && this.mListeners.getRegisteredCallbackCount() <= 0 && this.mPendingSessionRequests.isEmpty() && this.mOnGoingSessionRequests.isEmpty()) {
                String sciId = this.mInternalConnection.mSciId;
                if (((SpellCheckerBindGroup) this.mSpellCheckerBindGroups.get(sciId)) == this) {
                    this.mSpellCheckerBindGroups.remove(sciId);
                }
                TextServicesManagerService.this.mContext.unbindService(this.mInternalConnection);
                this.mUnbindCalled = true;
            }
        }

        public void removeAllLocked() {
            Slog.e(this.TAG, "Remove the spell checker bind unexpectedly.");
            for (int i = this.mListeners.getRegisteredCallbackCount() - 1; i >= 0; i--) {
                this.mListeners.unregister((ISpellCheckerSessionListener) this.mListeners.getRegisteredCallbackItem(i));
            }
            this.mPendingSessionRequests.clear();
            this.mOnGoingSessionRequests.clear();
            cleanLocked();
        }

        public void getISpellCheckerSessionOrQueueLocked(SessionRequest request) {
            if (!this.mUnbindCalled) {
                this.mListeners.register(request.mScListener);
                if (this.mConnected) {
                    try {
                        this.mSpellChecker.getISpellCheckerSession(request.mLocale, request.mScListener, request.mBundle, new ISpellCheckerServiceCallbackBinder(this, request));
                        this.mOnGoingSessionRequests.add(request);
                    } catch (RemoteException e) {
                        removeAllLocked();
                    }
                    cleanLocked();
                    return;
                }
                this.mPendingSessionRequests.add(request);
            }
        }

        void onSessionCreated(ISpellCheckerSession newSession, SessionRequest request) {
            synchronized (TextServicesManagerService.this.mLock) {
                if (this.mUnbindCalled) {
                    return;
                }
                if (this.mOnGoingSessionRequests.remove(request)) {
                    try {
                        request.mTsListener.onServiceConnected(newSession);
                    } catch (RemoteException e) {
                    }
                }
                cleanLocked();
            }
        }
    }

    private static class TextServicesData {
        private final Context mContext;
        private final ContentResolver mResolver;
        private final HashMap<String, SpellCheckerBindGroup> mSpellCheckerBindGroups;
        private final ArrayList<SpellCheckerInfo> mSpellCheckerList;
        private final HashMap<String, SpellCheckerInfo> mSpellCheckerMap;
        public int mUpdateCount = 0;
        private final int mUserId;

        public TextServicesData(int userId, Context context) {
            this.mUserId = userId;
            this.mSpellCheckerMap = new HashMap();
            this.mSpellCheckerList = new ArrayList();
            this.mSpellCheckerBindGroups = new HashMap();
            this.mContext = context;
            this.mResolver = context.getContentResolver();
        }

        private void putString(String key, String str) {
            Secure.putStringForUser(this.mResolver, key, str, this.mUserId);
        }

        private String getString(String key, String defaultValue) {
            String result = Secure.getStringForUser(this.mResolver, key, this.mUserId);
            return result != null ? result : defaultValue;
        }

        private void putInt(String key, int value) {
            Secure.putIntForUser(this.mResolver, key, value, this.mUserId);
        }

        private int getInt(String key, int defaultValue) {
            return Secure.getIntForUser(this.mResolver, key, defaultValue, this.mUserId);
        }

        private boolean getBoolean(String key, boolean defaultValue) {
            return getInt(key, defaultValue) == 1;
        }

        private void putSelectedSpellChecker(String sciId) {
            putString("selected_spell_checker", sciId);
        }

        private void putSelectedSpellCheckerSubtype(int hashCode) {
            putInt("selected_spell_checker_subtype", hashCode);
        }

        private String getSelectedSpellChecker() {
            return getString("selected_spell_checker", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }

        public int getSelectedSpellCheckerSubtype(int defaultValue) {
            return getInt("selected_spell_checker_subtype", defaultValue);
        }

        public boolean isSpellCheckerEnabled() {
            int spellCheckFlag = 0;
            String supportCheckLanguage = Secure.getString(this.mContext.getContentResolver(), "check_language");
            if (!TextUtils.isEmpty(supportCheckLanguage) && supportCheckLanguage.contains(Locale.getDefault().getLanguage())) {
                spellCheckFlag = 1;
            }
            String str = "spell_checker_enabled";
            boolean z = true;
            if (spellCheckFlag != 1) {
                z = false;
            }
            return getBoolean(str, z);
        }

        public SpellCheckerInfo getCurrentSpellChecker() {
            String curSpellCheckerId = getSelectedSpellChecker();
            if (TextUtils.isEmpty(curSpellCheckerId)) {
                return null;
            }
            return (SpellCheckerInfo) this.mSpellCheckerMap.get(curSpellCheckerId);
        }

        public void setCurrentSpellChecker(SpellCheckerInfo sci) {
            if (sci != null) {
                putSelectedSpellChecker(sci.getId());
            } else {
                putSelectedSpellChecker(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
            putSelectedSpellCheckerSubtype(0);
        }

        private void initializeTextServicesData() {
            this.mSpellCheckerList.clear();
            this.mSpellCheckerMap.clear();
            this.mUpdateCount++;
            List<ResolveInfo> services = this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.service.textservice.SpellCheckerService"), 128, this.mUserId);
            int N = services.size();
            for (int i = 0; i < N; i++) {
                ResolveInfo ri = (ResolveInfo) services.get(i);
                ServiceInfo si = ri.serviceInfo;
                ComponentName compName = new ComponentName(si.packageName, si.name);
                if ("android.permission.BIND_TEXT_SERVICE".equals(si.permission)) {
                    String access$000;
                    StringBuilder stringBuilder;
                    try {
                        SpellCheckerInfo sci = new SpellCheckerInfo(this.mContext, ri);
                        if (sci.getSubtypeCount() <= 0) {
                            access$000 = TextServicesManagerService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Skipping text service ");
                            stringBuilder.append(compName);
                            stringBuilder.append(": it does not contain subtypes.");
                            Slog.w(access$000, stringBuilder.toString());
                        } else {
                            this.mSpellCheckerList.add(sci);
                            this.mSpellCheckerMap.put(sci.getId(), sci);
                        }
                    } catch (XmlPullParserException e) {
                        access$000 = TextServicesManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to load the spell checker ");
                        stringBuilder.append(compName);
                        Slog.w(access$000, stringBuilder.toString(), e);
                    } catch (IOException e2) {
                        access$000 = TextServicesManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to load the spell checker ");
                        stringBuilder.append(compName);
                        Slog.w(access$000, stringBuilder.toString(), e2);
                    }
                } else {
                    String access$0002 = TextServicesManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Skipping text service ");
                    stringBuilder2.append(compName);
                    stringBuilder2.append(": it does not require the permission ");
                    stringBuilder2.append("android.permission.BIND_TEXT_SERVICE");
                    Slog.w(access$0002, stringBuilder2.toString());
                }
            }
        }

        private void dump(PrintWriter pw) {
            int spellCheckerIndex = 0;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  User #");
            stringBuilder.append(this.mUserId);
            pw.println(stringBuilder.toString());
            pw.println("  Spell Checkers:");
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Spell Checkers: mUpdateCount=");
            stringBuilder.append(this.mUpdateCount);
            pw.println(stringBuilder.toString());
            for (SpellCheckerInfo info : this.mSpellCheckerMap.values()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  Spell Checker #");
                stringBuilder2.append(spellCheckerIndex);
                pw.println(stringBuilder2.toString());
                info.dump(pw, "    ");
                spellCheckerIndex++;
            }
            pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            pw.println("  Spell Checker Bind Groups:");
            for (Entry<String, SpellCheckerBindGroup> ent : this.mSpellCheckerBindGroups.entrySet()) {
                int j;
                StringBuilder stringBuilder3;
                SpellCheckerBindGroup grp = (SpellCheckerBindGroup) ent.getValue();
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("    ");
                stringBuilder4.append((String) ent.getKey());
                stringBuilder4.append(" ");
                stringBuilder4.append(grp);
                stringBuilder4.append(":");
                pw.println(stringBuilder4.toString());
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("      mInternalConnection=");
                stringBuilder4.append(grp.mInternalConnection);
                pw.println(stringBuilder4.toString());
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("      mSpellChecker=");
                stringBuilder4.append(grp.mSpellChecker);
                pw.println(stringBuilder4.toString());
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("      mUnbindCalled=");
                stringBuilder4.append(grp.mUnbindCalled);
                pw.println(stringBuilder4.toString());
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("      mConnected=");
                stringBuilder4.append(grp.mConnected);
                pw.println(stringBuilder4.toString());
                int numPendingSessionRequests = grp.mPendingSessionRequests.size();
                int j2 = 0;
                for (j = 0; j < numPendingSessionRequests; j++) {
                    SessionRequest req = (SessionRequest) grp.mPendingSessionRequests.get(j);
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("      Pending Request #");
                    stringBuilder5.append(j);
                    stringBuilder5.append(":");
                    pw.println(stringBuilder5.toString());
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("        mTsListener=");
                    stringBuilder5.append(req.mTsListener);
                    pw.println(stringBuilder5.toString());
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("        mScListener=");
                    stringBuilder5.append(req.mScListener);
                    pw.println(stringBuilder5.toString());
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("        mScLocale=");
                    stringBuilder5.append(req.mLocale);
                    stringBuilder5.append(" mUid=");
                    stringBuilder5.append(req.mUid);
                    pw.println(stringBuilder5.toString());
                }
                j = grp.mOnGoingSessionRequests.size();
                int j3 = 0;
                while (j3 < j) {
                    SessionRequest req2 = (SessionRequest) grp.mOnGoingSessionRequests.get(j3);
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("      On going Request #");
                    stringBuilder3.append(j3);
                    stringBuilder3.append(":");
                    pw.println(stringBuilder3.toString());
                    j3++;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("        mTsListener=");
                    stringBuilder3.append(req2.mTsListener);
                    pw.println(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("        mScListener=");
                    stringBuilder3.append(req2.mScListener);
                    pw.println(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("        mScLocale=");
                    stringBuilder3.append(req2.mLocale);
                    stringBuilder3.append(" mUid=");
                    stringBuilder3.append(req2.mUid);
                    pw.println(stringBuilder3.toString());
                    j3++;
                }
                j3 = grp.mListeners.getRegisteredCallbackCount();
                while (j2 < j3) {
                    ISpellCheckerSessionListener mScListener = (ISpellCheckerSessionListener) grp.mListeners.getRegisteredCallbackItem(j2);
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("      Listener #");
                    stringBuilder3.append(j2);
                    stringBuilder3.append(":");
                    pw.println(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("        mScListener=");
                    stringBuilder3.append(mScListener);
                    pw.println(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("        mGroup=");
                    stringBuilder3.append(grp);
                    pw.println(stringBuilder3.toString());
                    j2++;
                }
            }
        }
    }

    private final class TextServicesMonitor extends PackageMonitor {
        private TextServicesMonitor() {
        }

        /* JADX WARNING: Missing block: B:24:0x0066, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onSomePackagesChanged() {
            int userId = getChangingUserId();
            synchronized (TextServicesManagerService.this.mLock) {
                TextServicesData tsd = (TextServicesData) TextServicesManagerService.this.mUserData.get(userId);
                if (tsd == null) {
                    return;
                }
                SpellCheckerInfo sci = tsd.getCurrentSpellChecker();
                tsd.initializeTextServicesData();
                if (!tsd.isSpellCheckerEnabled()) {
                } else if (sci == null) {
                    TextServicesManagerService.this.setCurrentSpellCheckerLocked(TextServicesManagerService.this.findAvailSystemSpellCheckerLocked(null, tsd), tsd);
                } else {
                    String packageName = sci.getPackageName();
                    int change = isPackageDisappearing(packageName);
                    if (change == 3 || change == 2) {
                        SpellCheckerInfo availSci = TextServicesManagerService.this.findAvailSystemSpellCheckerLocked(packageName, tsd);
                        if (availSci == null || !(availSci == null || availSci.getId().equals(sci.getId()))) {
                            TextServicesManagerService.this.setCurrentSpellCheckerLocked(availSci, tsd);
                        }
                    }
                }
            }
        }
    }

    public static final class Lifecycle extends SystemService {
        private TextServicesManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new TextServicesManagerService(context);
        }

        public void onStart() {
            publishBinderService("textservices", this.mService);
        }

        public void onStopUser(int userHandle) {
            this.mService.onStopUser(userHandle);
        }

        public void onUnlockUser(int userHandle) {
            this.mService.onUnlockUser(userHandle);
        }
    }

    void onStopUser(int userId) {
        synchronized (this.mLock) {
            this.mSpellCheckerOwnerUserIdMap.delete(userId);
            TextServicesData tsd = (TextServicesData) this.mUserData.get(userId);
            if (tsd == null) {
                return;
            }
            unbindServiceLocked(tsd);
            this.mUserData.remove(userId);
        }
    }

    void onUnlockUser(int userId) {
        synchronized (this.mLock) {
            initializeInternalStateLocked(userId);
        }
    }

    public TextServicesManagerService(Context context) {
        this.mContext = context;
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        this.mSpellCheckerOwnerUserIdMap = new LazyIntToIntMap(new -$$Lambda$TextServicesManagerService$Gx5nx59gL-Y47MWUiJn5TqC2DLs(this));
        this.mMonitor = new TextServicesMonitor();
        this.mMonitor.register(context, null, UserHandle.ALL, true);
    }

    public static /* synthetic */ int lambda$new$0(TextServicesManagerService textServicesManagerService, int callingUserId) {
        long token = Binder.clearCallingIdentity();
        try {
            int i;
            UserInfo parent = textServicesManagerService.mUserManager.getProfileParent(callingUserId);
            if (parent != null) {
                i = parent.id;
            } else {
                i = callingUserId;
            }
            Binder.restoreCallingIdentity(token);
            return i;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void initializeInternalStateLocked(int userId) {
        if (userId == this.mSpellCheckerOwnerUserIdMap.get(userId)) {
            TextServicesData tsd = (TextServicesData) this.mUserData.get(userId);
            if (tsd == null) {
                tsd = new TextServicesData(userId, this.mContext);
                this.mUserData.put(userId, tsd);
            }
            tsd.initializeTextServicesData();
            if (tsd.getCurrentSpellChecker() == null) {
                setCurrentSpellCheckerLocked(findAvailSystemSpellCheckerLocked(null, tsd), tsd);
            }
        }
    }

    private boolean bindCurrentSpellCheckerService(Intent service, ServiceConnection conn, int flags, int userId) {
        if (service != null && conn != null) {
            return this.mContext.bindServiceAsUser(service, conn, flags, UserHandle.of(userId));
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("--- bind failed: service = ");
        stringBuilder.append(service);
        stringBuilder.append(", conn = ");
        stringBuilder.append(conn);
        stringBuilder.append(", userId =");
        stringBuilder.append(userId);
        Slog.e(str, stringBuilder.toString());
        return false;
    }

    private void unbindServiceLocked(TextServicesData tsd) {
        HashMap<String, SpellCheckerBindGroup> spellCheckerBindGroups = tsd.mSpellCheckerBindGroups;
        for (SpellCheckerBindGroup scbg : spellCheckerBindGroups.values()) {
            scbg.removeAllLocked();
        }
        spellCheckerBindGroups.clear();
    }

    private SpellCheckerInfo findAvailSystemSpellCheckerLocked(String prefPackage, TextServicesData tsd) {
        String str = prefPackage;
        ArrayList<SpellCheckerInfo> spellCheckerList = new ArrayList();
        Iterator it = tsd.mSpellCheckerList.iterator();
        while (it.hasNext()) {
            SpellCheckerInfo sci = (SpellCheckerInfo) it.next();
            if ((1 & sci.getServiceInfo().applicationInfo.flags) != 0) {
                spellCheckerList.add(sci);
            }
        }
        int spellCheckersCount = spellCheckerList.size();
        if (spellCheckersCount == 0) {
            Slog.w(TAG, "no available spell checker services found");
            return null;
        }
        if (str != null) {
            for (int i = 0; i < spellCheckersCount; i++) {
                SpellCheckerInfo sci2 = (SpellCheckerInfo) spellCheckerList.get(i);
                if (str.equals(sci2.getPackageName())) {
                    return sci2;
                }
            }
        }
        ArrayList<Locale> suitableLocales = InputMethodUtils.getSuitableLocalesForSpellChecker(this.mContext.getResources().getConfiguration().locale);
        int localeCount = suitableLocales.size();
        for (int localeIndex = 0; localeIndex < localeCount; localeIndex++) {
            Locale locale = (Locale) suitableLocales.get(localeIndex);
            for (int spellCheckersIndex = 0; spellCheckersIndex < spellCheckersCount; spellCheckersIndex++) {
                SpellCheckerInfo info = (SpellCheckerInfo) spellCheckerList.get(spellCheckersIndex);
                int subtypeCount = info.getSubtypeCount();
                for (int subtypeIndex = 0; subtypeIndex < subtypeCount; subtypeIndex++) {
                    if (locale.equals(InputMethodUtils.constructLocaleFromString(info.getSubtypeAt(subtypeIndex).getLocale()))) {
                        return info;
                    }
                }
            }
        }
        if (spellCheckersCount > 1) {
            Slog.w(TAG, "more than one spell checker service found, picking first");
        }
        return (SpellCheckerInfo) spellCheckerList.get(0);
    }

    public SpellCheckerInfo getCurrentSpellChecker(String locale) {
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData tsd = getDataFromCallingUserIdLocked(userId);
            if (tsd == null) {
                return null;
            }
            SpellCheckerInfo currentSpellChecker = tsd.getCurrentSpellChecker();
            return currentSpellChecker;
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0028, code:
            if (r6 == null) goto L_0x009b;
     */
    /* JADX WARNING: Missing block: B:11:0x002e, code:
            if (r6.getSubtypeCount() != 0) goto L_0x0032;
     */
    /* JADX WARNING: Missing block: B:12:0x0032, code:
            if (r5 != 0) goto L_0x0037;
     */
    /* JADX WARNING: Missing block: B:13:0x0034, code:
            if (r13 != false) goto L_0x0037;
     */
    /* JADX WARNING: Missing block: B:14:0x0036, code:
            return null;
     */
    /* JADX WARNING: Missing block: B:15:0x0037, code:
            r1 = null;
     */
    /* JADX WARNING: Missing block: B:16:0x0038, code:
            if (r5 != 0) goto L_0x0060;
     */
    /* JADX WARNING: Missing block: B:17:0x003a, code:
            r7 = com.android.internal.view.IInputMethodManager.Stub.asInterface(android.os.ServiceManager.getService("input_method"));
     */
    /* JADX WARNING: Missing block: B:18:0x0044, code:
            if (r7 == null) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:20:?, code:
            r8 = r7.getCurrentInputMethodSubtype();
     */
    /* JADX WARNING: Missing block: B:21:0x004b, code:
            if (r8 == null) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:22:0x004d, code:
            r9 = r8.getLocale();
     */
    /* JADX WARNING: Missing block: B:23:0x0055, code:
            if (android.text.TextUtils.isEmpty(r9) != false) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:24:0x0057, code:
            r1 = r9;
     */
    /* JADX WARNING: Missing block: B:49:0x009b, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SpellCheckerSubtype getCurrentSpellCheckerSubtype(String locale, boolean allowImplicitlySelectedSubtype) {
        int i;
        int subtypeHashCode;
        SpellCheckerInfo sci;
        Locale systemLocale;
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData tsd = getDataFromCallingUserIdLocked(userId);
            if (tsd == null) {
                return null;
            }
            i = 0;
            subtypeHashCode = tsd.getSelectedSpellCheckerSubtype(0);
            sci = tsd.getCurrentSpellChecker();
            systemLocale = this.mContext.getResources().getConfiguration().locale;
        }
        String candidateLocale;
        if (candidateLocale == null) {
            candidateLocale = systemLocale.toString();
        }
        SpellCheckerSubtype candidate = null;
        while (i < sci.getSubtypeCount()) {
            SpellCheckerSubtype scs = sci.getSubtypeAt(i);
            if (subtypeHashCode == 0) {
                String scsLocale = scs.getLocale();
                if (candidateLocale.equals(scsLocale)) {
                    return scs;
                }
                if (candidate == null && candidateLocale.length() >= 2 && scsLocale.length() >= 2 && candidateLocale.startsWith(scsLocale)) {
                    candidate = scs;
                }
            } else if (scs.hashCode() == subtypeHashCode) {
                return scs;
            }
            i++;
        }
        return candidate;
    }

    public void getSpellCheckerService(String sciId, String locale, ITextServicesSessionListener tsListener, ISpellCheckerSessionListener scListener, Bundle bundle) {
        String str = sciId;
        if (TextUtils.isEmpty(sciId) || tsListener == null || scListener == null) {
            Slog.e(TAG, "getSpellCheckerService: Invalid input.");
            return;
        }
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData tsd = getDataFromCallingUserIdLocked(callingUserId);
            if (tsd == null) {
                return;
            }
            HashMap<String, SpellCheckerInfo> spellCheckerMap = tsd.mSpellCheckerMap;
            if (spellCheckerMap.containsKey(str)) {
                SpellCheckerInfo sci = (SpellCheckerInfo) spellCheckerMap.get(str);
                HashMap<String, SpellCheckerBindGroup> spellCheckerBindGroups = tsd.mSpellCheckerBindGroups;
                SpellCheckerBindGroup bindGroup = (SpellCheckerBindGroup) spellCheckerBindGroups.get(str);
                int uid = Binder.getCallingUid();
                if (bindGroup == null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        bindGroup = startSpellCheckerServiceInnerLocked(sci, tsd);
                        Binder.restoreCallingIdentity(ident);
                        if (bindGroup == null) {
                            return;
                        }
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(ident);
                        Throwable th2 = th;
                    }
                }
                SpellCheckerBindGroup bindGroup2 = bindGroup;
                SessionRequest sessionRequest = r3;
                SessionRequest sessionRequest2 = new SessionRequest(uid, locale, tsListener, scListener, bundle);
                bindGroup2.getISpellCheckerSessionOrQueueLocked(sessionRequest);
                return;
            }
        }
    }

    public boolean isSpellCheckerEnabled() {
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData tsd = getDataFromCallingUserIdLocked(userId);
            if (tsd == null) {
                return false;
            }
            boolean isSpellCheckerEnabled = tsd.isSpellCheckerEnabled();
            return isSpellCheckerEnabled;
        }
    }

    private SpellCheckerBindGroup startSpellCheckerServiceInnerLocked(SpellCheckerInfo info, TextServicesData tsd) {
        String sciId = info.getId();
        InternalServiceConnection connection = new InternalServiceConnection(sciId, tsd.mSpellCheckerBindGroups);
        Intent serviceIntent = new Intent("android.service.textservice.SpellCheckerService");
        serviceIntent.setComponent(info.getComponent());
        if (bindCurrentSpellCheckerService(serviceIntent, connection, 8388609, tsd.mUserId)) {
            SpellCheckerBindGroup group = new SpellCheckerBindGroup(connection);
            tsd.mSpellCheckerBindGroups.put(sciId, group);
            return group;
        }
        Slog.e(TAG, "Failed to get a spell checker service.");
        return null;
    }

    public SpellCheckerInfo[] getEnabledSpellCheckers() {
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData tsd = getDataFromCallingUserIdLocked(callingUserId);
            if (tsd == null) {
                return null;
            }
            ArrayList<SpellCheckerInfo> spellCheckerList = tsd.mSpellCheckerList;
            SpellCheckerInfo[] spellCheckerInfoArr = (SpellCheckerInfo[]) spellCheckerList.toArray(new SpellCheckerInfo[spellCheckerList.size()]);
            return spellCheckerInfoArr;
        }
    }

    public void finishSpellCheckerService(ISpellCheckerSessionListener listener) {
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData tsd = getDataFromCallingUserIdLocked(userId);
            if (tsd == null) {
                return;
            }
            ArrayList<SpellCheckerBindGroup> removeList = new ArrayList();
            for (SpellCheckerBindGroup group : tsd.mSpellCheckerBindGroups.values()) {
                if (group != null) {
                    removeList.add(group);
                }
            }
            int removeSize = removeList.size();
            for (int i = 0; i < removeSize; i++) {
                ((SpellCheckerBindGroup) removeList.get(i)).removeListener(listener);
            }
        }
    }

    private void setCurrentSpellCheckerLocked(SpellCheckerInfo sci, TextServicesData tsd) {
        String id;
        if (sci != null) {
            id = sci.getId();
        } else {
            id = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            tsd.setCurrentSpellChecker(sci);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            int i = 0;
            if (args.length == 0 || (args.length == 1 && args[0].equals("-a"))) {
                synchronized (this.mLock) {
                    pw.println("Current Text Services Manager state:");
                    pw.println("  Users:");
                    int numOfUsers = this.mUserData.size();
                    while (i < numOfUsers) {
                        ((TextServicesData) this.mUserData.valueAt(i)).dump(pw);
                        i++;
                    }
                }
            } else if (args.length == 2 && args[0].equals("--user")) {
                int userId = Integer.parseInt(args[1]);
                if (this.mUserManager.getUserInfo(userId) == null) {
                    pw.println("Non-existent user.");
                    return;
                }
                TextServicesData tsd = (TextServicesData) this.mUserData.get(userId);
                if (tsd == null) {
                    pw.println("User needs to unlock first.");
                    return;
                }
                synchronized (this.mLock) {
                    pw.println("Current Text Services Manager state:");
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  User ");
                    stringBuilder.append(userId);
                    stringBuilder.append(":");
                    pw.println(stringBuilder.toString());
                    tsd.dump(pw);
                }
            } else {
                pw.println("Invalid arguments to text services.");
            }
        }
    }

    private TextServicesData getDataFromCallingUserIdLocked(int callingUserId) {
        int spellCheckerOwnerUserId = this.mSpellCheckerOwnerUserIdMap.get(callingUserId);
        TextServicesData data = (TextServicesData) this.mUserData.get(spellCheckerOwnerUserId);
        if (spellCheckerOwnerUserId != callingUserId) {
            if (data == null) {
                return null;
            }
            SpellCheckerInfo info = data.getCurrentSpellChecker();
            if (info == null || (info.getServiceInfo().applicationInfo.flags & 1) == 0) {
                return null;
            }
            return data;
        }
        return data;
    }
}
