package com.android.server.content;

import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountManager;
import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ISyncStatusObserver;
import android.content.PeriodicSync;
import android.content.SyncInfo;
import android.content.SyncRequest.Builder;
import android.content.SyncStatusInfo;
import android.content.SyncStatusInfo.Stats;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.hdm.HwDeviceManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.UiThread;
import com.android.server.audio.AudioService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.voiceinteraction.DatabaseHelper.SoundModelContract;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SyncStorageEngine {
    private static final int ACCOUNTS_VERSION = 3;
    private static final double DEFAULT_FLEX_PERCENT_SYNC = 0.04d;
    private static final long DEFAULT_MIN_FLEX_ALLOWED_SECS = 5;
    private static final long DEFAULT_POLL_FREQUENCY_SECONDS = 86400;
    public static final int EVENT_START = 0;
    public static final int EVENT_STOP = 1;
    public static final int MAX_HISTORY = 100;
    public static final String MESG_CANCELED = "canceled";
    public static final String MESG_SUCCESS = "success";
    @VisibleForTesting
    static final long MILLIS_IN_4WEEKS = 2419200000L;
    private static final int MSG_WRITE_STATISTICS = 2;
    private static final int MSG_WRITE_STATUS = 1;
    public static final long NOT_IN_BACKOFF_MODE = -1;
    public static final String[] SOURCES = new String[]{"OTHER", "LOCAL", "POLL", "USER", "PERIODIC", "FEED"};
    public static final int SOURCE_FEED = 5;
    public static final int SOURCE_LOCAL = 1;
    public static final int SOURCE_OTHER = 0;
    public static final int SOURCE_PERIODIC = 4;
    public static final int SOURCE_POLL = 2;
    public static final int SOURCE_USER = 3;
    public static final int STATISTICS_FILE_END = 0;
    public static final int STATISTICS_FILE_ITEM = 101;
    public static final int STATISTICS_FILE_ITEM_OLD = 100;
    public static final int STATUS_FILE_END = 0;
    public static final int STATUS_FILE_ITEM = 100;
    private static final boolean SYNC_ENABLED_DEFAULT = false;
    private static final String TAG = "SyncManager";
    private static final String TAG_FILE = "SyncManagerFile";
    private static final long WRITE_STATISTICS_DELAY = 1800000;
    private static final long WRITE_STATUS_DELAY = 600000;
    private static final String XML_ATTR_ENABLED = "enabled";
    private static final String XML_ATTR_LISTEN_FOR_TICKLES = "listen-for-tickles";
    private static final String XML_ATTR_NEXT_AUTHORITY_ID = "nextAuthorityId";
    private static final String XML_ATTR_SYNC_RANDOM_OFFSET = "offsetInSeconds";
    private static final String XML_ATTR_USER = "user";
    private static final String XML_TAG_LISTEN_FOR_TICKLES = "listenForTickles";
    private static PeriodicSyncAddedListener mPeriodicSyncAddedListener;
    private static HashMap<String, String> sAuthorityRenames = new HashMap();
    private static volatile SyncStorageEngine sSyncStorageEngine = null;
    private final AtomicFile mAccountInfoFile;
    private final HashMap<AccountAndUser, AccountInfo> mAccounts = new HashMap();
    private final SparseArray<AuthorityInfo> mAuthorities = new SparseArray();
    private OnAuthorityRemovedListener mAuthorityRemovedListener;
    private final Calendar mCal;
    private final RemoteCallbackList<ISyncStatusObserver> mChangeListeners = new RemoteCallbackList();
    private final Context mContext;
    private final SparseArray<ArrayList<SyncInfo>> mCurrentSyncs = new SparseArray();
    private final DayStats[] mDayStats = new DayStats[28];
    private boolean mDefaultMasterSyncAutomatically;
    private boolean mGrantSyncAdaptersAccountAccess;
    private final MyHandler mHandler;
    private volatile boolean mIsClockValid;
    private final SyncLogger mLogger;
    private SparseArray<Boolean> mMasterSyncAutomatically = new SparseArray();
    private int mNextAuthorityId = 0;
    private int mNextHistoryId = 0;
    private final ArrayMap<ComponentName, SparseArray<AuthorityInfo>> mServices = new ArrayMap();
    private final AtomicFile mStatisticsFile;
    private final AtomicFile mStatusFile;
    private final ArrayList<SyncHistoryItem> mSyncHistory = new ArrayList();
    private int mSyncRandomOffset;
    private OnSyncRequestListener mSyncRequestListener;
    private final SparseArray<SyncStatusInfo> mSyncStatus = new SparseArray();
    private int mYear;
    private int mYearInDays;

    private static class AccountAuthorityValidator {
        private final AccountManager mAccountManager;
        private final SparseArray<Account[]> mAccountsCache = new SparseArray();
        private final PackageManager mPackageManager;
        private final SparseArray<ArrayMap<String, Boolean>> mProvidersPerUserCache = new SparseArray();

        AccountAuthorityValidator(Context context) {
            this.mAccountManager = (AccountManager) context.getSystemService(AccountManager.class);
            this.mPackageManager = context.getPackageManager();
        }

        boolean isAccountValid(Account account, int userId) {
            Account[] accountsForUser = (Account[]) this.mAccountsCache.get(userId);
            if (accountsForUser == null) {
                accountsForUser = this.mAccountManager.getAccountsAsUser(userId);
                this.mAccountsCache.put(userId, accountsForUser);
            }
            return ArrayUtils.contains(accountsForUser, account);
        }

        boolean isAuthorityValid(String authority, int userId) {
            ArrayMap<String, Boolean> authorityMap = (ArrayMap) this.mProvidersPerUserCache.get(userId);
            if (authorityMap == null) {
                authorityMap = new ArrayMap();
                this.mProvidersPerUserCache.put(userId, authorityMap);
            }
            if (!authorityMap.containsKey(authority)) {
                authorityMap.put(authority, Boolean.valueOf(this.mPackageManager.resolveContentProviderAsUser(authority, 786432, userId) != null));
            }
            return ((Boolean) authorityMap.get(authority)).booleanValue();
        }
    }

    static class AccountInfo {
        final AccountAndUser accountAndUser;
        final HashMap<String, AuthorityInfo> authorities = new HashMap();

        AccountInfo(AccountAndUser accountAndUser) {
            this.accountAndUser = accountAndUser;
        }
    }

    public static class AuthorityInfo {
        public static final int NOT_INITIALIZED = -1;
        public static final int NOT_SYNCABLE = 0;
        public static final int SYNCABLE = 1;
        public static final int SYNCABLE_NOT_INITIALIZED = 2;
        public static final int SYNCABLE_NO_ACCOUNT_ACCESS = 3;
        public static final int UNDEFINED = -2;
        long backoffDelay;
        long backoffTime;
        long delayUntil;
        boolean enabled;
        final int ident;
        final ArrayList<PeriodicSync> periodicSyncs;
        int syncable;
        final EndPoint target;

        AuthorityInfo(AuthorityInfo toCopy) {
            this.target = toCopy.target;
            this.ident = toCopy.ident;
            this.enabled = toCopy.enabled;
            this.syncable = toCopy.syncable;
            this.backoffTime = toCopy.backoffTime;
            this.backoffDelay = toCopy.backoffDelay;
            this.delayUntil = toCopy.delayUntil;
            this.periodicSyncs = new ArrayList();
            Iterator it = toCopy.periodicSyncs.iterator();
            while (it.hasNext()) {
                this.periodicSyncs.add(new PeriodicSync((PeriodicSync) it.next()));
            }
        }

        AuthorityInfo(EndPoint info, int id) {
            this.target = info;
            this.ident = id;
            this.enabled = false;
            this.periodicSyncs = new ArrayList();
            defaultInitialisation();
        }

        private void defaultInitialisation() {
            this.syncable = -1;
            this.backoffTime = -1;
            this.backoffDelay = -1;
            if (SyncStorageEngine.mPeriodicSyncAddedListener != null) {
                SyncStorageEngine.mPeriodicSyncAddedListener.onPeriodicSyncAdded(this.target, new Bundle(), SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS, SyncStorageEngine.calculateDefaultFlexTime(SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS));
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.target);
            stringBuilder.append(", enabled=");
            stringBuilder.append(this.enabled);
            stringBuilder.append(", syncable=");
            stringBuilder.append(this.syncable);
            stringBuilder.append(", backoff=");
            stringBuilder.append(this.backoffTime);
            stringBuilder.append(", delay=");
            stringBuilder.append(this.delayUntil);
            return stringBuilder.toString();
        }
    }

    public static class DayStats {
        public final int day;
        public int failureCount;
        public long failureTime;
        public int successCount;
        public long successTime;

        public DayStats(int day) {
            this.day = day;
        }
    }

    public static class EndPoint {
        public static final EndPoint USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL = new EndPoint(null, null, -1);
        final Account account;
        final String provider;
        final int userId;

        public EndPoint(Account account, String provider, int userId) {
            this.account = account;
            this.provider = provider;
            this.userId = userId;
        }

        public boolean matchesSpec(EndPoint spec) {
            boolean z = false;
            if (this.userId != spec.userId && this.userId != -1 && spec.userId != -1) {
                return false;
            }
            boolean accountsMatch;
            if (spec.account == null) {
                accountsMatch = true;
            } else {
                accountsMatch = this.account.equals(spec.account);
            }
            boolean providersMatch;
            if (spec.provider == null) {
                providersMatch = true;
            } else {
                providersMatch = this.provider.equals(spec.provider);
            }
            if (accountsMatch && providersMatch) {
                z = true;
            }
            return z;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.account == null ? "ALL ACCS" : "XXXXXXXXX");
            sb.append(SliceAuthority.DELIMITER);
            sb.append(this.provider == null ? "ALL PDRS" : this.provider);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(":u");
            stringBuilder.append(this.userId);
            sb.append(stringBuilder.toString());
            return sb.toString();
        }
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                synchronized (SyncStorageEngine.this.mAuthorities) {
                    SyncStorageEngine.this.writeStatusLocked();
                }
            } else if (msg.what == 2) {
                synchronized (SyncStorageEngine.this.mAuthorities) {
                    SyncStorageEngine.this.writeStatisticsLocked();
                }
            }
        }
    }

    interface OnAuthorityRemovedListener {
        void onAuthorityRemoved(EndPoint endPoint);
    }

    interface OnSyncRequestListener {
        void onSyncRequest(EndPoint endPoint, int i, Bundle bundle, int i2);
    }

    interface PeriodicSyncAddedListener {
        void onPeriodicSyncAdded(EndPoint endPoint, Bundle bundle, long j, long j2);
    }

    public static class SyncHistoryItem {
        int authorityId;
        long downstreamActivity;
        long elapsedTime;
        int event;
        long eventTime;
        Bundle extras;
        int historyId;
        boolean initialization;
        String mesg;
        int reason;
        int source;
        int syncExemptionFlag;
        long upstreamActivity;
    }

    static {
        sAuthorityRenames.put("contacts", "com.android.contacts");
        sAuthorityRenames.put("calendar", "com.android.calendar");
    }

    private SyncStorageEngine(Context context, File dataDir, Looper looper) {
        this.mHandler = new MyHandler(looper);
        this.mContext = context;
        sSyncStorageEngine = this;
        this.mLogger = SyncLogger.getInstance();
        this.mCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
        this.mDefaultMasterSyncAutomatically = this.mContext.getResources().getBoolean(17957050);
        File syncDir = new File(new File(dataDir, "system"), "sync");
        syncDir.mkdirs();
        maybeDeleteLegacyPendingInfoLocked(syncDir);
        this.mAccountInfoFile = new AtomicFile(new File(syncDir, "accounts.xml"), "sync-accounts");
        this.mStatusFile = new AtomicFile(new File(syncDir, "status.bin"), "sync-status");
        this.mStatisticsFile = new AtomicFile(new File(syncDir, "stats.bin"), "sync-stats");
        readAccountInfoLocked();
        readStatusLocked();
        readStatisticsLocked();
        readAndDeleteLegacyAccountInfoLocked();
        writeAccountInfoLocked();
        writeStatusLocked();
        writeStatisticsLocked();
        if (this.mLogger.enabled()) {
            int size = this.mAuthorities.size();
            this.mLogger.log("Loaded ", Integer.valueOf(size), " items");
            for (int i = 0; i < size; i++) {
                this.mLogger.log(this.mAuthorities.valueAt(i));
            }
        }
    }

    public static SyncStorageEngine newTestInstance(Context context) {
        return new SyncStorageEngine(context, context.getFilesDir(), Looper.getMainLooper());
    }

    public static void init(Context context, Looper looper) {
        if (sSyncStorageEngine == null) {
            sSyncStorageEngine = new SyncStorageEngine(context, Environment.getDataDirectory(), looper);
        }
    }

    public static SyncStorageEngine getSingleton() {
        if (sSyncStorageEngine != null) {
            return sSyncStorageEngine;
        }
        throw new IllegalStateException("not initialized");
    }

    protected void setOnSyncRequestListener(OnSyncRequestListener listener) {
        if (this.mSyncRequestListener == null) {
            this.mSyncRequestListener = listener;
        }
    }

    protected void setOnAuthorityRemovedListener(OnAuthorityRemovedListener listener) {
        if (this.mAuthorityRemovedListener == null) {
            this.mAuthorityRemovedListener = listener;
        }
    }

    protected void setPeriodicSyncAddedListener(PeriodicSyncAddedListener listener) {
        if (mPeriodicSyncAddedListener == null) {
            mPeriodicSyncAddedListener = listener;
        }
    }

    public int getSyncRandomOffset() {
        return this.mSyncRandomOffset;
    }

    public void addStatusChangeListener(int mask, ISyncStatusObserver callback) {
        synchronized (this.mAuthorities) {
            this.mChangeListeners.register(callback, Integer.valueOf(mask));
        }
    }

    public void removeStatusChangeListener(ISyncStatusObserver callback) {
        synchronized (this.mAuthorities) {
            this.mChangeListeners.unregister(callback);
        }
    }

    public static long calculateDefaultFlexTime(long syncTimeSeconds) {
        if (syncTimeSeconds < DEFAULT_MIN_FLEX_ALLOWED_SECS) {
            return 0;
        }
        if (syncTimeSeconds < DEFAULT_POLL_FREQUENCY_SECONDS) {
            return (long) (((double) syncTimeSeconds) * DEFAULT_FLEX_PERCENT_SYNC);
        }
        return 3456;
    }

    void reportChange(int which) {
        ArrayList<ISyncStatusObserver> reports = null;
        synchronized (this.mAuthorities) {
            int i = this.mChangeListeners.beginBroadcast();
            while (i > 0) {
                i--;
                if ((((Integer) this.mChangeListeners.getBroadcastCookie(i)).intValue() & which) != 0) {
                    if (reports == null) {
                        reports = new ArrayList(i);
                    }
                    reports.add((ISyncStatusObserver) this.mChangeListeners.getBroadcastItem(i));
                }
            }
            this.mChangeListeners.finishBroadcast();
        }
        if (Log.isLoggable("SyncManager", 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reportChange ");
            stringBuilder.append(which);
            stringBuilder.append(" to: ");
            stringBuilder.append(reports);
            Slog.v("SyncManager", stringBuilder.toString());
        }
        if (reports != null) {
            int i2 = reports.size();
            while (i2 > 0) {
                i2--;
                try {
                    ((ISyncStatusObserver) reports.get(i2)).onStatusChanged(which);
                } catch (RemoteException e) {
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x001b, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getSyncAutomatically(Account account, int userId, String providerName) {
        synchronized (this.mAuthorities) {
            boolean z = false;
            if (account != null) {
                AuthorityInfo authority = getAuthorityLocked(new EndPoint(account, providerName, userId), "getSyncAutomatically");
                if (authority != null && authority.enabled) {
                    z = true;
                }
            } else {
                int i = this.mAuthorities.size();
                while (i > 0) {
                    i--;
                    AuthorityInfo authorityInfo = (AuthorityInfo) this.mAuthorities.valueAt(i);
                    if (authorityInfo.target.matchesSpec(new EndPoint(account, providerName, userId)) && authorityInfo.enabled) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x00ac, code:
            return;
     */
    /* JADX WARNING: Missing block: B:28:0x00e7, code:
            if (r12 == false) goto L_0x00f8;
     */
    /* JADX WARNING: Missing block: B:29:0x00e9, code:
            requestSync(r9, r10, -6, r11, new android.os.Bundle(), r19);
     */
    /* JADX WARNING: Missing block: B:30:0x00f8, code:
            reportChange(1);
            queueBackup();
     */
    /* JADX WARNING: Missing block: B:31:0x00fe, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setSyncAutomatically(Account account, int userId, String providerName, boolean sync, int syncExemptionFlag, int callingUid) {
        Account account2 = account;
        int i = userId;
        String str = providerName;
        boolean z = sync;
        if (Log.isLoggable("SyncManager", 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setSyncAutomatically:  provider ");
            stringBuilder.append(str);
            stringBuilder.append(", user ");
            stringBuilder.append(i);
            stringBuilder.append(" -> ");
            stringBuilder.append(z);
            Slog.d("SyncManager", stringBuilder.toString());
        }
        this.mLogger.log("Set sync auto account=", account2, " user=", Integer.valueOf(userId), " authority=", str, " value=", Boolean.toString(sync), " callingUid=", Integer.valueOf(callingUid));
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(new EndPoint(account2, str, i), -1, false);
            if (authority.enabled == z) {
                if (Log.isLoggable("SyncManager", 2)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setSyncAutomatically: already set to ");
                    stringBuilder2.append(z);
                    stringBuilder2.append(", doing nothing");
                    Slog.d("SyncManager", stringBuilder2.toString());
                }
            } else if (z && account2 != null && HwDeviceManager.disallowOp(25, account2.type) && HwDeviceManager.disallowOp(24, str)) {
                Slog.i("SyncManager", "setSyncAutomatically() is not allowed for google account by MDM!");
                UiThread.getHandler().post(new Runnable() {
                    public void run() {
                        Toast toast = Toast.makeText(SyncStorageEngine.this.mContext, SyncStorageEngine.this.mContext.getResources().getString(33685904), 1);
                        toast.getWindowParams().type = 2006;
                        toast.show();
                    }
                });
            } else {
                if (z && authority.syncable == 2) {
                    authority.syncable = -1;
                }
                authority.enabled = z;
                writeAccountInfoLocked();
            }
        }
    }

    public int getIsSyncable(Account account, int userId, String providerName) {
        synchronized (this.mAuthorities) {
            int i;
            if (account != null) {
                AuthorityInfo authority = getAuthorityLocked(new EndPoint(account, providerName, userId), "get authority syncable");
                if (authority == null) {
                    return -1;
                }
                i = authority.syncable;
                return i;
            }
            int i2 = this.mAuthorities.size();
            while (i2 > 0) {
                i2--;
                AuthorityInfo authorityInfo = (AuthorityInfo) this.mAuthorities.valueAt(i2);
                if (authorityInfo.target != null && authorityInfo.target.provider.equals(providerName)) {
                    i = authorityInfo.syncable;
                    return i;
                }
            }
            return -1;
        }
    }

    public void setIsSyncable(Account account, int userId, String providerName, int syncable, int callingUid) {
        setSyncableStateForEndPoint(new EndPoint(account, providerName, userId), syncable, callingUid);
    }

    /* JADX WARNING: Missing block: B:16:0x0087, code:
            return;
     */
    /* JADX WARNING: Missing block: B:19:0x008e, code:
            r0 = r4;
     */
    /* JADX WARNING: Missing block: B:20:0x008f, code:
            if (r10 != 1) goto L_0x009a;
     */
    /* JADX WARNING: Missing block: B:21:0x0091, code:
            requestSync(r0, -5, new android.os.Bundle(), 0);
     */
    /* JADX WARNING: Missing block: B:22:0x009a, code:
            reportChange(1);
     */
    /* JADX WARNING: Missing block: B:23:0x009d, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setSyncableStateForEndPoint(EndPoint target, int syncable, int callingUid) {
        this.mLogger.log("Set syncable ", target, " value=", Integer.toString(syncable), " callingUid=", Integer.valueOf(callingUid));
        synchronized (this.mAuthorities) {
            AuthorityInfo aInfo = getOrCreateAuthorityLocked(target, -1, false);
            if (syncable < -1) {
                syncable = -1;
            }
            if (Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setIsSyncable: ");
                stringBuilder.append(aInfo.toString());
                stringBuilder.append(" -> ");
                stringBuilder.append(syncable);
                Slog.d("SyncManager", stringBuilder.toString());
            }
            if (aInfo.syncable != syncable) {
                aInfo.syncable = syncable;
                writeAccountInfoLocked();
            } else if (Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setIsSyncable: already set to ");
                stringBuilder2.append(syncable);
                stringBuilder2.append(", doing nothing");
                Slog.d("SyncManager", stringBuilder2.toString());
            }
        }
    }

    public Pair<Long, Long> getBackoff(EndPoint info) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getAuthorityLocked(info, "getBackoff");
            if (authority != null) {
                Pair<Long, Long> create = Pair.create(Long.valueOf(authority.backoffTime), Long.valueOf(authority.backoffDelay));
                return create;
            }
            return null;
        }
    }

    public void setBackoff(EndPoint info, long nextSyncTime, long nextDelay) {
        int i;
        boolean changed;
        EndPoint endPoint = info;
        long j = nextSyncTime;
        long j2 = nextDelay;
        if (Log.isLoggable("SyncManager", 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setBackoff: ");
            stringBuilder.append(endPoint);
            stringBuilder.append(" -> nextSyncTime ");
            stringBuilder.append(j);
            stringBuilder.append(", nextDelay ");
            stringBuilder.append(j2);
            Slog.v("SyncManager", stringBuilder.toString());
        }
        synchronized (this.mAuthorities) {
            boolean changed2 = true;
            if (endPoint.account == null || endPoint.provider == null) {
                i = 1;
                changed2 = setBackoffLocked(endPoint.account, endPoint.userId, endPoint.provider, j, j2);
            } else {
                AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(endPoint, -1, true);
                if (authorityInfo.backoffTime == j && authorityInfo.backoffDelay == j2) {
                    i = 1;
                    changed2 = false;
                } else {
                    authorityInfo.backoffTime = j;
                    authorityInfo.backoffDelay = j2;
                    i = 1;
                }
            }
            changed = changed2;
        }
        if (changed) {
            reportChange(i);
        }
    }

    private boolean setBackoffLocked(Account account, int userId, String providerName, long nextSyncTime, long nextDelay) {
        boolean changed = false;
        for (AccountInfo accountInfo : this.mAccounts.values()) {
            if (account == null || account.equals(accountInfo.accountAndUser.account) || userId == accountInfo.accountAndUser.userId) {
                for (AuthorityInfo authorityInfo : accountInfo.authorities.values()) {
                    if (providerName == null || providerName.equals(authorityInfo.target.provider)) {
                        if (!(authorityInfo.backoffTime == nextSyncTime && authorityInfo.backoffDelay == nextDelay)) {
                            authorityInfo.backoffTime = nextSyncTime;
                            authorityInfo.backoffDelay = nextDelay;
                            changed = true;
                        }
                    }
                }
            }
        }
        return changed;
    }

    public void clearAllBackoffsLocked() {
        boolean changed = false;
        synchronized (this.mAuthorities) {
            for (AccountInfo accountInfo : this.mAccounts.values()) {
                for (AuthorityInfo authorityInfo : accountInfo.authorities.values()) {
                    if (authorityInfo.backoffTime != -1 || authorityInfo.backoffDelay != -1) {
                        if (Log.isLoggable("SyncManager", 2)) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("clearAllBackoffsLocked: authority:");
                            stringBuilder.append(authorityInfo.target);
                            stringBuilder.append(" account:");
                            stringBuilder.append(accountInfo.accountAndUser.account.name);
                            stringBuilder.append(" user:");
                            stringBuilder.append(accountInfo.accountAndUser.userId);
                            stringBuilder.append(" backoffTime was: ");
                            stringBuilder.append(authorityInfo.backoffTime);
                            stringBuilder.append(" backoffDelay was: ");
                            stringBuilder.append(authorityInfo.backoffDelay);
                            Slog.v("SyncManager", stringBuilder.toString());
                        }
                        authorityInfo.backoffTime = -1;
                        authorityInfo.backoffDelay = -1;
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            reportChange(1);
        }
    }

    public long getDelayUntilTime(EndPoint info) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getAuthorityLocked(info, "getDelayUntil");
            if (authority == null) {
                return 0;
            }
            long j = authority.delayUntil;
            return j;
        }
    }

    public void setDelayUntilTime(EndPoint info, long delayUntil) {
        if (Log.isLoggable("SyncManager", 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setDelayUntil: ");
            stringBuilder.append(info);
            stringBuilder.append(" -> delayUntil ");
            stringBuilder.append(delayUntil);
            Slog.v("SyncManager", stringBuilder.toString());
        }
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, true);
            if (authority.delayUntil == delayUntil) {
                return;
            }
            authority.delayUntil = delayUntil;
            reportChange(1);
        }
    }

    boolean restoreAllPeriodicSyncs() {
        int i = 0;
        if (mPeriodicSyncAddedListener == null) {
            return false;
        }
        synchronized (this.mAuthorities) {
            while (i < this.mAuthorities.size()) {
                AuthorityInfo authority = (AuthorityInfo) this.mAuthorities.valueAt(i);
                Iterator it = authority.periodicSyncs.iterator();
                while (it.hasNext()) {
                    PeriodicSync periodicSync = (PeriodicSync) it.next();
                    mPeriodicSyncAddedListener.onPeriodicSyncAdded(authority.target, periodicSync.extras, periodicSync.period, periodicSync.flexTime);
                }
                authority.periodicSyncs.clear();
                i++;
            }
            writeAccountInfoLocked();
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:11:0x005e, code:
            if (r14 == false) goto L_0x006f;
     */
    /* JADX WARNING: Missing block: B:12:0x0060, code:
            requestSync(null, r9, -7, null, new android.os.Bundle(), r16);
     */
    /* JADX WARNING: Missing block: B:13:0x006f, code:
            reportChange(1);
            r8.mContext.sendBroadcast(android.content.ContentResolver.ACTION_SYNC_CONN_STATUS_CHANGED);
            queueBackup();
     */
    /* JADX WARNING: Missing block: B:14:0x007c, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setMasterSyncAutomatically(boolean flag, int userId, int syncExemptionFlag, int callingUid) {
        int i = userId;
        SyncLogger syncLogger = this.mLogger;
        Object[] objArr = new Object[5];
        objArr[0] = "Set master enabled=";
        objArr[1] = Boolean.valueOf(flag);
        objArr[2] = " user=";
        objArr[3] = Integer.valueOf(i);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" caller=");
        stringBuilder.append(callingUid);
        objArr[4] = stringBuilder.toString();
        syncLogger.log(objArr);
        synchronized (this.mAuthorities) {
            Boolean auto = (Boolean) this.mMasterSyncAutomatically.get(i);
            if (auto == null || !auto.equals(Boolean.valueOf(flag))) {
                this.mMasterSyncAutomatically.put(i, Boolean.valueOf(flag));
                writeAccountInfoLocked();
            }
        }
    }

    public boolean getMasterSyncAutomatically(int userId) {
        boolean booleanValue;
        synchronized (this.mAuthorities) {
            Boolean auto = (Boolean) this.mMasterSyncAutomatically.get(userId);
            booleanValue = auto == null ? this.mDefaultMasterSyncAutomatically : auto.booleanValue();
        }
        return booleanValue;
    }

    public int getAuthorityCount() {
        int size;
        synchronized (this.mAuthorities) {
            size = this.mAuthorities.size();
        }
        return size;
    }

    public AuthorityInfo getAuthority(int authorityId) {
        AuthorityInfo authorityInfo;
        synchronized (this.mAuthorities) {
            authorityInfo = (AuthorityInfo) this.mAuthorities.get(authorityId);
        }
        return authorityInfo;
    }

    public boolean isSyncActive(EndPoint info) {
        synchronized (this.mAuthorities) {
            for (SyncInfo syncInfo : getCurrentSyncs(info.userId)) {
                AuthorityInfo ainfo = getAuthority(syncInfo.authorityId);
                if (ainfo != null && ainfo.target.matchesSpec(info)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void markPending(EndPoint info, boolean pendingValue) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, true);
            if (authority == null) {
                return;
            }
            getOrCreateSyncStatusLocked(authority.ident).pending = pendingValue;
            reportChange(2);
        }
    }

    public void doDatabaseCleanup(Account[] accounts, int userId) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Updating for new accounts...");
            }
            SparseArray<AuthorityInfo> removing = new SparseArray();
            Iterator<AccountInfo> accIt = this.mAccounts.values().iterator();
            while (accIt.hasNext()) {
                AccountInfo acc = (AccountInfo) accIt.next();
                if (!ArrayUtils.contains(accounts, acc.accountAndUser.account) && acc.accountAndUser.userId == userId) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Account removed: ");
                        stringBuilder.append(acc.accountAndUser);
                        Slog.v("SyncManager", stringBuilder.toString());
                    }
                    for (AuthorityInfo auth : acc.authorities.values()) {
                        removing.put(auth.ident, auth);
                    }
                    accIt.remove();
                }
            }
            int i = removing.size();
            if (i > 0) {
                while (i > 0) {
                    i--;
                    int ident = removing.keyAt(i);
                    AuthorityInfo auth2 = (AuthorityInfo) removing.valueAt(i);
                    if (this.mAuthorityRemovedListener != null) {
                        this.mAuthorityRemovedListener.onAuthorityRemoved(auth2.target);
                    }
                    this.mAuthorities.remove(ident);
                    int j = this.mSyncStatus.size();
                    while (j > 0) {
                        j--;
                        if (this.mSyncStatus.keyAt(j) == ident) {
                            this.mSyncStatus.remove(this.mSyncStatus.keyAt(j));
                        }
                    }
                    j = this.mSyncHistory.size();
                    while (j > 0) {
                        j--;
                        if (((SyncHistoryItem) this.mSyncHistory.get(j)).authorityId == ident) {
                            this.mSyncHistory.remove(j);
                        }
                    }
                }
                writeAccountInfoLocked();
                writeStatusLocked();
                writeStatisticsLocked();
            }
        }
    }

    public SyncInfo addActiveSync(ActiveSyncContext activeSyncContext) {
        SyncInfo syncInfo;
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setActiveSync: account= auth=");
                stringBuilder.append(activeSyncContext.mSyncOperation.target);
                stringBuilder.append(" src=");
                stringBuilder.append(activeSyncContext.mSyncOperation.syncSource);
                stringBuilder.append(" extras=");
                stringBuilder.append(activeSyncContext.mSyncOperation.extras);
                Slog.v("SyncManager", stringBuilder.toString());
            }
            AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(activeSyncContext.mSyncOperation.target, -1, true);
            syncInfo = new SyncInfo(authorityInfo.ident, authorityInfo.target.account, authorityInfo.target.provider, activeSyncContext.mStartTime);
            getCurrentSyncs(authorityInfo.target.userId).add(syncInfo);
        }
        reportActiveChange();
        return syncInfo;
    }

    public void removeActiveSync(SyncInfo syncInfo, int userId) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeActiveSync: account=");
                stringBuilder.append(syncInfo.account);
                stringBuilder.append(" user=");
                stringBuilder.append(userId);
                stringBuilder.append(" auth=");
                stringBuilder.append(syncInfo.authority);
                Slog.v("SyncManager", stringBuilder.toString());
            }
            getCurrentSyncs(userId).remove(syncInfo);
        }
        reportActiveChange();
    }

    public void reportActiveChange() {
        reportChange(4);
    }

    /* JADX WARNING: Missing block: B:22:0x00a2, code:
            r0 = r4;
            reportChange(8);
     */
    /* JADX WARNING: Missing block: B:23:0x00a8, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long insertStartSyncEvent(SyncOperation op, long now) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("insertStartSyncEvent: ");
                stringBuilder.append(op);
                Slog.v("SyncManager", stringBuilder.toString());
            }
            AuthorityInfo authority = getAuthorityLocked(op.target, "insertStartSyncEvent");
            if (authority == null) {
                return -1;
            }
            SyncHistoryItem item = new SyncHistoryItem();
            item.initialization = op.isInitialization();
            item.authorityId = authority.ident;
            int i = this.mNextHistoryId;
            this.mNextHistoryId = i + 1;
            item.historyId = i;
            if (this.mNextHistoryId < 0) {
                this.mNextHistoryId = 0;
            }
            item.eventTime = now;
            item.source = op.syncSource;
            item.reason = op.reason;
            item.extras = op.extras;
            item.event = 0;
            item.syncExemptionFlag = op.syncExemptionFlag;
            this.mSyncHistory.add(0, item);
            while (this.mSyncHistory.size() > 100) {
                this.mSyncHistory.remove(this.mSyncHistory.size() - 1);
            }
            long id = (long) item.historyId;
            if (Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("returning historyId ");
                stringBuilder2.append(id);
                Slog.v("SyncManager", stringBuilder2.toString());
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:42:0x017f A:{Catch:{ all -> 0x027e }} */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0161 A:{Catch:{ all -> 0x027e }} */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x020a A:{Catch:{ all -> 0x027e }} */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x023e A:{Catch:{ all -> 0x027e }} */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x023a A:{Catch:{ all -> 0x027e }} */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x025b A:{Catch:{ all -> 0x027e }} */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0257 A:{Catch:{ all -> 0x027e }} */
    /* JADX WARNING: Missing block: B:69:0x0273, code:
            reportChange(8);
     */
    /* JADX WARNING: Missing block: B:70:0x0278, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void stopSyncEvent(long historyId, long elapsedTime, String resultMessage, long downstreamActivity, long upstreamActivity) {
        Throwable th;
        long j = historyId;
        long j2 = elapsedTime;
        String str = resultMessage;
        synchronized (this.mAuthorities) {
            try {
                if (Log.isLoggable("SyncManager", 2)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("stopSyncEvent: historyId=");
                    stringBuilder.append(j);
                    Slog.v("SyncManager", stringBuilder.toString());
                }
                SyncHistoryItem item = null;
                int i = this.mSyncHistory.size();
                while (i > 0) {
                    i--;
                    item = (SyncHistoryItem) this.mSyncHistory.get(i);
                    if (((long) item.historyId) == j) {
                        break;
                    }
                    item = null;
                }
                if (item == null) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("stopSyncEvent: no history for id ");
                    stringBuilder2.append(j);
                    Slog.w("SyncManager", stringBuilder2.toString());
                    return;
                }
                item.elapsedTime = j2;
                item.event = 1;
                item.mesg = str;
                item.downstreamActivity = downstreamActivity;
                try {
                    boolean writeStatisticsNow;
                    int i2;
                    DayStats dayStats;
                    long lastSyncTime;
                    boolean writeStatusNow;
                    StringBuilder event;
                    StringBuilder stringBuilder3;
                    item.upstreamActivity = upstreamActivity;
                    SyncStatusInfo status = getOrCreateSyncStatusLocked(item.authorityId);
                    status.maybeResetTodayStats(isClockValid(), false);
                    Stats stats = status.totalStats;
                    stats.numSyncs++;
                    stats = status.todayStats;
                    stats.numSyncs++;
                    stats = status.totalStats;
                    stats.totalElapsedTime += j2;
                    Stats stats2 = status.todayStats;
                    stats2.totalElapsedTime += j2;
                    switch (item.source) {
                        case 0:
                            stats2 = status.totalStats;
                            stats2.numSourceOther++;
                            stats2 = status.todayStats;
                            stats2.numSourceOther++;
                            break;
                        case 1:
                            stats2 = status.totalStats;
                            stats2.numSourceLocal++;
                            stats2 = status.todayStats;
                            stats2.numSourceLocal++;
                            break;
                        case 2:
                            stats2 = status.totalStats;
                            stats2.numSourcePoll++;
                            stats2 = status.todayStats;
                            stats2.numSourcePoll++;
                            break;
                        case 3:
                            stats2 = status.totalStats;
                            stats2.numSourceUser++;
                            stats2 = status.todayStats;
                            stats2.numSourceUser++;
                            break;
                        case 4:
                            stats2 = status.totalStats;
                            stats2.numSourcePeriodic++;
                            stats2 = status.todayStats;
                            stats2.numSourcePeriodic++;
                            break;
                        case 5:
                            stats2 = status.totalStats;
                            stats2.numSourceFeed++;
                            stats2 = status.todayStats;
                            stats2.numSourceFeed++;
                            break;
                    }
                    boolean writeStatisticsNow2 = false;
                    int day = getCurrentDayLocked();
                    if (this.mDayStats[0] == null) {
                        this.mDayStats[0] = new DayStats(day);
                    } else if (day != this.mDayStats[0].day) {
                        writeStatisticsNow = false;
                        System.arraycopy(this.mDayStats, 0, this.mDayStats, 1, this.mDayStats.length - 1);
                        this.mDayStats[0] = new DayStats(day);
                        writeStatisticsNow2 = true;
                    } else {
                        writeStatisticsNow = false;
                        i2 = 0;
                        dayStats = this.mDayStats[0];
                        dayStats = this.mDayStats[i2];
                        lastSyncTime = item.eventTime + j2;
                        writeStatusNow = false;
                        Stats stats3;
                        if (!MESG_SUCCESS.equals(str)) {
                            if (status.lastSuccessTime == 0 || status.lastFailureTime != 0) {
                                writeStatusNow = true;
                            }
                            status.setLastSuccess(item.source, lastSyncTime);
                            dayStats.successCount++;
                            dayStats.successTime += j2;
                        } else if (MESG_CANCELED.equals(str)) {
                            stats3 = status.totalStats;
                            stats3.numCancels++;
                            stats3 = status.todayStats;
                            stats3.numCancels++;
                            writeStatusNow = true;
                        } else {
                            if (status.lastFailureTime == 0) {
                                writeStatusNow = true;
                            }
                            stats3 = status.totalStats;
                            stats3.numFailures++;
                            stats3 = status.todayStats;
                            stats3.numFailures++;
                            status.setLastFailure(item.source, lastSyncTime, str);
                            dayStats.failureCount++;
                            dayStats.failureTime += j2;
                        }
                        event = new StringBuilder();
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        stringBuilder3.append(str);
                        stringBuilder3.append(" Source=");
                        stringBuilder3.append(SOURCES[item.source]);
                        stringBuilder3.append(" Elapsed=");
                        event.append(stringBuilder3.toString());
                        SyncManager.formatDurationHMS(event, j2);
                        event.append(" Reason=");
                        event.append(SyncOperation.reasonToString(null, item.reason));
                        if (item.syncExemptionFlag != 0) {
                            event.append(" Exemption=");
                            switch (item.syncExemptionFlag) {
                                case 1:
                                    event.append("fg");
                                    break;
                                case 2:
                                    event.append("top");
                                    break;
                                default:
                                    event.append(item.syncExemptionFlag);
                                    break;
                            }
                        }
                        event.append(" Extras=");
                        SyncOperation.extrasToStringBuilder(item.extras, event);
                        status.addEvent(event.toString());
                        if (!writeStatusNow) {
                            writeStatusLocked();
                        } else if (!this.mHandler.hasMessages(1)) {
                            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 600000);
                        }
                        if (!writeStatisticsNow) {
                            writeStatisticsLocked();
                        } else if (!this.mHandler.hasMessages(2)) {
                            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 1800000);
                        }
                    }
                    writeStatisticsNow = writeStatisticsNow2;
                    i2 = 0;
                    dayStats = this.mDayStats[i2];
                    lastSyncTime = item.eventTime + j2;
                    writeStatusNow = false;
                    if (!MESG_SUCCESS.equals(str)) {
                    }
                    event = new StringBuilder();
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    stringBuilder3.append(str);
                    stringBuilder3.append(" Source=");
                    stringBuilder3.append(SOURCES[item.source]);
                    stringBuilder3.append(" Elapsed=");
                    event.append(stringBuilder3.toString());
                    SyncManager.formatDurationHMS(event, j2);
                    event.append(" Reason=");
                    event.append(SyncOperation.reasonToString(null, item.reason));
                    if (item.syncExemptionFlag != 0) {
                    }
                    event.append(" Extras=");
                    SyncOperation.extrasToStringBuilder(item.extras, event);
                    status.addEvent(event.toString());
                    if (!writeStatusNow) {
                    }
                    if (!writeStatisticsNow) {
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                long j3 = upstreamActivity;
                throw th;
            }
        }
    }

    private List<SyncInfo> getCurrentSyncs(int userId) {
        List<SyncInfo> currentSyncsLocked;
        synchronized (this.mAuthorities) {
            currentSyncsLocked = getCurrentSyncsLocked(userId);
        }
        return currentSyncsLocked;
    }

    public List<SyncInfo> getCurrentSyncsCopy(int userId, boolean canAccessAccounts) {
        List<SyncInfo> syncsCopy;
        synchronized (this.mAuthorities) {
            List<SyncInfo> syncs = getCurrentSyncsLocked(userId);
            syncsCopy = new ArrayList();
            for (SyncInfo sync : syncs) {
                SyncInfo copy;
                if (canAccessAccounts) {
                    copy = new SyncInfo(sync);
                } else {
                    copy = SyncInfo.createAccountRedacted(sync.authorityId, sync.authority, sync.startTime);
                }
                syncsCopy.add(copy);
            }
        }
        return syncsCopy;
    }

    private List<SyncInfo> getCurrentSyncsLocked(int userId) {
        ArrayList<SyncInfo> syncs = (ArrayList) this.mCurrentSyncs.get(userId);
        if (syncs != null) {
            return syncs;
        }
        syncs = new ArrayList();
        this.mCurrentSyncs.put(userId, syncs);
        return syncs;
    }

    public Pair<AuthorityInfo, SyncStatusInfo> getCopyOfAuthorityWithSyncStatus(EndPoint info) {
        Pair<AuthorityInfo, SyncStatusInfo> createCopyPairOfAuthorityWithSyncStatusLocked;
        synchronized (this.mAuthorities) {
            createCopyPairOfAuthorityWithSyncStatusLocked = createCopyPairOfAuthorityWithSyncStatusLocked(getOrCreateAuthorityLocked(info, -1, true));
        }
        return createCopyPairOfAuthorityWithSyncStatusLocked;
    }

    public SyncStatusInfo getStatusByAuthority(EndPoint info) {
        if (info.account == null || info.provider == null) {
            return null;
        }
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            int i = 0;
            while (i < N) {
                SyncStatusInfo cur = (SyncStatusInfo) this.mSyncStatus.valueAt(i);
                AuthorityInfo ainfo = (AuthorityInfo) this.mAuthorities.get(cur.authorityId);
                if (ainfo == null || !ainfo.target.matchesSpec(info)) {
                    i++;
                } else {
                    return cur;
                }
            }
            return null;
        }
    }

    public boolean isSyncPending(EndPoint info) {
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            for (int i = 0; i < N; i++) {
                SyncStatusInfo cur = (SyncStatusInfo) this.mSyncStatus.valueAt(i);
                AuthorityInfo ainfo = (AuthorityInfo) this.mAuthorities.get(cur.authorityId);
                if (ainfo != null && ainfo.target.matchesSpec(info) && cur.pending) {
                    return true;
                }
            }
            return false;
        }
    }

    public ArrayList<SyncHistoryItem> getSyncHistory() {
        ArrayList<SyncHistoryItem> items;
        synchronized (this.mAuthorities) {
            int N = this.mSyncHistory.size();
            items = new ArrayList(N);
            for (int i = 0; i < N; i++) {
                items.add((SyncHistoryItem) this.mSyncHistory.get(i));
            }
        }
        return items;
    }

    public DayStats[] getDayStatistics() {
        DayStats[] ds;
        synchronized (this.mAuthorities) {
            ds = new DayStats[this.mDayStats.length];
            System.arraycopy(this.mDayStats, 0, ds, 0, ds.length);
        }
        return ds;
    }

    private Pair<AuthorityInfo, SyncStatusInfo> createCopyPairOfAuthorityWithSyncStatusLocked(AuthorityInfo authorityInfo) {
        return Pair.create(new AuthorityInfo(authorityInfo), new SyncStatusInfo(getOrCreateSyncStatusLocked(authorityInfo.ident)));
    }

    private int getCurrentDayLocked() {
        this.mCal.setTimeInMillis(System.currentTimeMillis());
        int dayOfYear = this.mCal.get(6);
        if (this.mYear != this.mCal.get(1)) {
            this.mYear = this.mCal.get(1);
            this.mCal.clear();
            this.mCal.set(1, this.mYear);
            this.mYearInDays = (int) (this.mCal.getTimeInMillis() / 86400000);
        }
        return this.mYearInDays + dayOfYear;
    }

    private AuthorityInfo getAuthorityLocked(EndPoint info, String tag) {
        AccountAndUser au = new AccountAndUser(info.account, info.userId);
        AccountInfo accountInfo = (AccountInfo) this.mAccounts.get(au);
        if (accountInfo == null) {
            if (tag != null && Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(tag);
                stringBuilder.append(": unknown account ");
                stringBuilder.append(au);
                Slog.v("SyncManager", stringBuilder.toString());
            }
            return null;
        }
        AuthorityInfo authority = (AuthorityInfo) accountInfo.authorities.get(info.provider);
        if (authority != null) {
            return authority;
        }
        if (tag != null && Log.isLoggable("SyncManager", 2)) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(tag);
            stringBuilder2.append(": unknown provider ");
            stringBuilder2.append(info.provider);
            Slog.v("SyncManager", stringBuilder2.toString());
        }
        return null;
    }

    private AuthorityInfo getOrCreateAuthorityLocked(EndPoint info, int ident, boolean doWrite) {
        AccountAndUser au = new AccountAndUser(info.account, info.userId);
        AccountInfo account = (AccountInfo) this.mAccounts.get(au);
        if (account == null) {
            account = new AccountInfo(au);
            this.mAccounts.put(au, account);
        }
        AuthorityInfo authority = (AuthorityInfo) account.authorities.get(info.provider);
        if (authority != null) {
            return authority;
        }
        authority = createAuthorityLocked(info, ident, doWrite);
        account.authorities.put(info.provider, authority);
        return authority;
    }

    private AuthorityInfo createAuthorityLocked(EndPoint info, int ident, boolean doWrite) {
        if (ident < 0) {
            ident = this.mNextAuthorityId;
            this.mNextAuthorityId++;
            doWrite = true;
        }
        if (Log.isLoggable("SyncManager", 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("created a new AuthorityInfo for ");
            stringBuilder.append(info);
            Slog.v("SyncManager", stringBuilder.toString());
        }
        AuthorityInfo authority = new AuthorityInfo(info, ident);
        this.mAuthorities.put(ident, authority);
        if (doWrite) {
            writeAccountInfoLocked();
        }
        return authority;
    }

    public void removeAuthority(EndPoint info) {
        synchronized (this.mAuthorities) {
            removeAuthorityLocked(info.account, info.userId, info.provider, true);
        }
    }

    private void removeAuthorityLocked(Account account, int userId, String authorityName, boolean doWrite) {
        AccountInfo accountInfo = (AccountInfo) this.mAccounts.get(new AccountAndUser(account, userId));
        if (accountInfo != null) {
            AuthorityInfo authorityInfo = (AuthorityInfo) accountInfo.authorities.remove(authorityName);
            if (authorityInfo != null) {
                if (this.mAuthorityRemovedListener != null) {
                    this.mAuthorityRemovedListener.onAuthorityRemoved(authorityInfo.target);
                }
                this.mAuthorities.remove(authorityInfo.ident);
                if (doWrite) {
                    writeAccountInfoLocked();
                }
            }
        }
    }

    private SyncStatusInfo getOrCreateSyncStatusLocked(int authorityId) {
        SyncStatusInfo status = (SyncStatusInfo) this.mSyncStatus.get(authorityId);
        if (status != null) {
            return status;
        }
        status = new SyncStatusInfo(authorityId);
        this.mSyncStatus.put(authorityId, status);
        return status;
    }

    public void writeAllState() {
        synchronized (this.mAuthorities) {
            writeStatusLocked();
            writeStatisticsLocked();
        }
    }

    public boolean shouldGrantSyncAdaptersAccountAccess() {
        return this.mGrantSyncAdaptersAccountAccess;
    }

    public void clearAndReadState() {
        synchronized (this.mAuthorities) {
            this.mAuthorities.clear();
            this.mAccounts.clear();
            this.mServices.clear();
            this.mSyncStatus.clear();
            this.mSyncHistory.clear();
            readAccountInfoLocked();
            readStatusLocked();
            readStatisticsLocked();
            readAndDeleteLegacyAccountInfoLocked();
            writeAccountInfoLocked();
            writeStatusLocked();
            writeStatisticsLocked();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:94:0x0195 A:{LOOP_END, LOOP:1: B:63:0x0107->B:94:0x0195} */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x019a A:{SYNTHETIC, EDGE_INSN: B:134:0x019a->B:95:0x019a ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x019a A:{SYNTHETIC, EDGE_INSN: B:134:0x019a->B:95:0x019a ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0195 A:{LOOP_END, LOOP:1: B:63:0x0107->B:94:0x0195} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0069 A:{SYNTHETIC, Splitter: B:18:0x0069} */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0050 A:{Catch:{ XmlPullParserException -> 0x01d9, IOException -> 0x01b4, all -> 0x01b0 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readAccountInfoLocked() {
        int highestAuthorityId = -1;
        FileInputStream fis = null;
        try {
            int eventType;
            fis = this.mAccountInfoFile.openRead();
            int i = 2;
            if (Log.isLoggable(TAG_FILE, 2)) {
                String str = TAG_FILE;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Reading ");
                stringBuilder.append(this.mAccountInfoFile.getBaseFile());
                Slog.v(str, stringBuilder.toString());
            }
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            int eventType2 = parser.getEventType();
            while (true) {
                eventType = eventType2;
                if (eventType != 2 && eventType != 1) {
                    eventType2 = parser.next();
                } else if (eventType != 1) {
                    Slog.i("SyncManager", "No initial accounts");
                    this.mNextAuthorityId = Math.max(-1 + 1, this.mNextAuthorityId);
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                        }
                    }
                    return;
                } else {
                    if ("accounts".equals(parser.getName())) {
                        String listen = parser.getAttributeValue(null, XML_ATTR_LISTEN_FOR_TICKLES);
                        String versionString = parser.getAttributeValue(null, "version");
                        int i2 = 0;
                        if (versionString == null) {
                            eventType2 = 0;
                        } else {
                            try {
                                eventType2 = Integer.parseInt(versionString);
                            } catch (NumberFormatException e2) {
                                NumberFormatException numberFormatException = e2;
                                eventType2 = 0;
                            }
                        }
                        int version = eventType2;
                        if (version < 3) {
                            this.mGrantSyncAdaptersAccountAccess = true;
                        }
                        String nextIdString = parser.getAttributeValue(null, XML_ATTR_NEXT_AUTHORITY_ID);
                        if (nextIdString == null) {
                            eventType2 = 0;
                        } else {
                            try {
                                eventType2 = Integer.parseInt(nextIdString);
                            } catch (NumberFormatException e3) {
                            }
                        }
                        this.mNextAuthorityId = Math.max(this.mNextAuthorityId, eventType2);
                        String offsetString = parser.getAttributeValue(null, XML_ATTR_SYNC_RANDOM_OFFSET);
                        if (offsetString == null) {
                            eventType2 = 0;
                        } else {
                            try {
                                eventType2 = Integer.parseInt(offsetString);
                            } catch (NumberFormatException e4) {
                                this.mSyncRandomOffset = 0;
                            }
                        }
                        this.mSyncRandomOffset = eventType2;
                        if (this.mSyncRandomOffset == 0) {
                            this.mSyncRandomOffset = new Random(System.currentTimeMillis()).nextInt(86400);
                        }
                        SparseArray sparseArray = this.mMasterSyncAutomatically;
                        boolean z = listen == null || Boolean.parseBoolean(listen);
                        sparseArray.put(0, Boolean.valueOf(z));
                        eventType2 = parser.next();
                        AuthorityInfo authority = null;
                        PeriodicSync periodicSync = null;
                        AccountAuthorityValidator validator = new AccountAuthorityValidator(this.mContext);
                        while (true) {
                            int i3;
                            if (eventType2 == i) {
                                String tagName = parser.getName();
                                if (parser.getDepth() == i) {
                                    if ("authority".equals(tagName)) {
                                        authority = parseAuthority(parser, version, validator);
                                        periodicSync = null;
                                        if (authority == null) {
                                            Object[] objArr = new Object[3];
                                            objArr[i2] = "26513719";
                                            objArr[1] = Integer.valueOf(-1);
                                            i3 = 2;
                                            objArr[2] = "Malformed authority";
                                            EventLog.writeEvent(1397638484, objArr);
                                        } else if (authority.ident > highestAuthorityId) {
                                            highestAuthorityId = authority.ident;
                                        }
                                    } else {
                                        i3 = i;
                                        if (XML_TAG_LISTEN_FOR_TICKLES.equals(tagName)) {
                                            parseListenForTickles(parser);
                                        }
                                    }
                                    eventType2 = parser.next();
                                    if (eventType2 == 1) {
                                        break;
                                    }
                                    i = i3;
                                    i2 = 0;
                                } else {
                                    i3 = i;
                                    if (parser.getDepth() == 3) {
                                        if ("periodicSync".equals(tagName) && authority != null) {
                                            periodicSync = parsePeriodicSync(parser, authority);
                                        }
                                    } else if (parser.getDepth() == 4 && periodicSync != null && "extra".equals(tagName)) {
                                        parseExtra(parser, periodicSync.extras);
                                    }
                                    eventType2 = parser.next();
                                    if (eventType2 == 1) {
                                    }
                                }
                            }
                            i3 = i;
                            eventType2 = parser.next();
                            if (eventType2 == 1) {
                            }
                        }
                    }
                    this.mNextAuthorityId = Math.max(highestAuthorityId + 1, this.mNextAuthorityId);
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e5) {
                        }
                    }
                    maybeMigrateSettingsForRenamedAuthorities();
                    return;
                }
            }
            if (eventType != 1) {
            }
        } catch (XmlPullParserException e6) {
            Slog.w("SyncManager", "Error reading accounts", e6);
            this.mNextAuthorityId = Math.max(-1 + 1, this.mNextAuthorityId);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e7) {
                }
            }
        } catch (IOException e8) {
            int highestAuthorityId2 = e8;
            if (fis == null) {
                Slog.i("SyncManager", "No initial accounts");
            } else {
                Slog.w("SyncManager", "Error reading accounts", highestAuthorityId2);
            }
            this.mNextAuthorityId = Math.max(-1 + 1, this.mNextAuthorityId);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e9) {
                }
            }
        } catch (Throwable th) {
            highestAuthorityId = th;
            this.mNextAuthorityId = Math.max(-1 + 1, this.mNextAuthorityId);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e10) {
                }
            }
        }
    }

    private void maybeDeleteLegacyPendingInfoLocked(File syncDir) {
        File file = new File(syncDir, "pending.bin");
        if (file.exists()) {
            file.delete();
        }
    }

    private boolean maybeMigrateSettingsForRenamedAuthorities() {
        AuthorityInfo authority;
        ArrayList<AuthorityInfo> authoritiesToRemove = new ArrayList();
        int N = this.mAuthorities.size();
        boolean writeNeeded = false;
        for (int i = 0; i < N; i++) {
            authority = (AuthorityInfo) this.mAuthorities.valueAt(i);
            String newAuthorityName = (String) sAuthorityRenames.get(authority.target.provider);
            if (newAuthorityName != null) {
                authoritiesToRemove.add(authority);
                if (authority.enabled) {
                    EndPoint newInfo = new EndPoint(authority.target.account, newAuthorityName, authority.target.userId);
                    if (getAuthorityLocked(newInfo, "cleanup") == null) {
                        getOrCreateAuthorityLocked(newInfo, -1, false).enabled = true;
                        writeNeeded = true;
                    }
                }
            }
        }
        Iterator it = authoritiesToRemove.iterator();
        while (it.hasNext()) {
            authority = (AuthorityInfo) it.next();
            removeAuthorityLocked(authority.target.account, authority.target.userId, authority.target.provider, false);
            writeNeeded = true;
        }
        return writeNeeded;
    }

    private void parseListenForTickles(XmlPullParser parser) {
        boolean listen = false;
        int userId = 0;
        try {
            userId = Integer.parseInt(parser.getAttributeValue(null, XML_ATTR_USER));
        } catch (NumberFormatException e) {
            Slog.e("SyncManager", "error parsing the user for listen-for-tickles", e);
        } catch (NullPointerException e2) {
            Slog.e("SyncManager", "the user in listen-for-tickles is null", e2);
        }
        String enabled = parser.getAttributeValue(null, XML_ATTR_ENABLED);
        if (enabled == null || Boolean.parseBoolean(enabled)) {
            listen = true;
        }
        this.mMasterSyncAutomatically.put(userId, Boolean.valueOf(listen));
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x0187  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x0152  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x0152  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x0187  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private AuthorityInfo parseAuthority(XmlPullParser parser, int version, AccountAuthorityValidator validator) {
        XmlPullParser xmlPullParser = parser;
        AccountAuthorityValidator accountAuthorityValidator = validator;
        AuthorityInfo authority = null;
        int id = -1;
        try {
            id = Integer.parseInt(xmlPullParser.getAttributeValue(null, "id"));
        } catch (NumberFormatException e) {
            Slog.e("SyncManager", "error parsing the id of the authority", e);
        } catch (NullPointerException e2) {
            Slog.e("SyncManager", "the id of the authority is null", e2);
        }
        if (id >= 0) {
            boolean z;
            String authorityName = xmlPullParser.getAttributeValue(null, "authority");
            String enabled = xmlPullParser.getAttributeValue(null, XML_ATTR_ENABLED);
            String syncable = xmlPullParser.getAttributeValue(null, "syncable");
            String accountName = xmlPullParser.getAttributeValue(null, "account");
            String accountType = xmlPullParser.getAttributeValue(null, SoundModelContract.KEY_TYPE);
            String user = xmlPullParser.getAttributeValue(null, XML_ATTR_USER);
            String packageName = xmlPullParser.getAttributeValue(null, "package");
            String className = xmlPullParser.getAttributeValue(null, AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS);
            int userId = user == null ? 0 : Integer.parseInt(user);
            if (accountType == null && packageName == null) {
                accountType = "com.google";
                syncable = String.valueOf(-1);
            }
            String syncable2 = syncable;
            AuthorityInfo authority2 = (AuthorityInfo) this.mAuthorities.get(id);
            if (Log.isLoggable(TAG_FILE, 2)) {
                String str = TAG_FILE;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Adding authority: account=");
                stringBuilder.append(accountName);
                stringBuilder.append(" accountType=");
                stringBuilder.append(accountType);
                stringBuilder.append(" auth=");
                stringBuilder.append(authorityName);
                stringBuilder.append(" package=");
                stringBuilder.append(packageName);
                stringBuilder.append(" class=");
                stringBuilder.append(className);
                stringBuilder.append(" user=");
                stringBuilder.append(userId);
                stringBuilder.append(" enabled=");
                stringBuilder.append(enabled);
                stringBuilder.append(" syncable=");
                stringBuilder.append(syncable2);
                Slog.v(str, stringBuilder.toString());
            }
            if (authority2 == null) {
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Slog.v(TAG_FILE, "Creating authority entry");
                }
                if (!(accountName == null || authorityName == null)) {
                    EndPoint info = new EndPoint(new Account(accountName, accountType), authorityName, userId);
                    if (accountAuthorityValidator.isAccountValid(info.account, userId) && accountAuthorityValidator.isAuthorityValid(authorityName, userId)) {
                        authority2 = getOrCreateAuthorityLocked(info, id, false);
                        if (version > 0) {
                            authority2.periodicSyncs.clear();
                        }
                        authority = authority2;
                        z = false;
                        if (authority != null) {
                        }
                    } else {
                        r4 = new Object[3];
                        z = false;
                        r4[0] = "35028827";
                        r4[1] = Integer.valueOf(-1);
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("account:");
                        stringBuilder2.append(info.account);
                        stringBuilder2.append(" provider:");
                        stringBuilder2.append(authorityName);
                        stringBuilder2.append(" user:");
                        stringBuilder2.append(userId);
                        r4[2] = stringBuilder2.toString();
                        EventLog.writeEvent(1397638484, r4);
                        authority = authority2;
                        if (authority != null) {
                            int i;
                            boolean z2 = (enabled == null || Boolean.parseBoolean(enabled)) ? true : z;
                            authority.enabled = z2;
                            if (syncable2 == null) {
                                i = -1;
                            } else {
                                try {
                                    i = Integer.parseInt(syncable2);
                                } catch (NumberFormatException e3) {
                                    if (Shell.NIGHT_MODE_STR_UNKNOWN.equals(syncable2)) {
                                        authority.syncable = -1;
                                    } else {
                                        authority.syncable = Boolean.parseBoolean(syncable2) ? 1 : z;
                                    }
                                }
                            }
                            authority.syncable = i;
                        } else {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Failure adding authority: auth=");
                            stringBuilder2.append(authorityName);
                            stringBuilder2.append(" enabled=");
                            stringBuilder2.append(enabled);
                            stringBuilder2.append(" syncable=");
                            stringBuilder2.append(syncable2);
                            Slog.w("SyncManager", stringBuilder2.toString());
                        }
                    }
                }
            }
            z = false;
            authority = authority2;
            if (authority != null) {
            }
        }
        return authority;
    }

    private PeriodicSync parsePeriodicSync(XmlPullParser parser, AuthorityInfo authorityInfo) {
        long flextime;
        StringBuilder stringBuilder;
        XmlPullParser xmlPullParser = parser;
        AuthorityInfo authorityInfo2 = authorityInfo;
        Bundle extras = new Bundle();
        String periodValue = xmlPullParser.getAttributeValue(null, "period");
        String flexValue = xmlPullParser.getAttributeValue(null, "flex");
        try {
            long period = Long.parseLong(periodValue);
            try {
                flextime = Long.parseLong(flexValue);
            } catch (NumberFormatException e) {
                NumberFormatException numberFormatException = e;
                flextime = calculateDefaultFlexTime(period);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error formatting value parsed for periodic sync flex: ");
                stringBuilder.append(flexValue);
                stringBuilder.append(", using default: ");
                stringBuilder.append(flextime);
                Slog.e("SyncManager", stringBuilder.toString());
            } catch (NullPointerException expected) {
                NullPointerException nullPointerException = expected;
                flextime = calculateDefaultFlexTime(period);
                stringBuilder = new StringBuilder();
                stringBuilder.append("No flex time specified for this sync, using a default. period: ");
                stringBuilder.append(period);
                stringBuilder.append(" flex: ");
                stringBuilder.append(flextime);
                Slog.d("SyncManager", stringBuilder.toString());
            }
            PeriodicSync periodicSync = new PeriodicSync(authorityInfo2.target.account, authorityInfo2.target.provider, extras, period, flextime);
            authorityInfo2.periodicSyncs.add(periodicSync);
            return periodicSync;
        } catch (NumberFormatException e2) {
            NumberFormatException numberFormatException2 = e2;
            Slog.e("SyncManager", "error parsing the period of a periodic sync", e2);
            return null;
        } catch (NullPointerException expected2) {
            NullPointerException nullPointerException2 = expected2;
            Slog.e("SyncManager", "the period of a periodic sync is null", expected2);
            return null;
        }
    }

    private void parseExtra(XmlPullParser parser, Bundle extras) {
        String name = parser.getAttributeValue(null, Settings.ATTR_NAME);
        String type = parser.getAttributeValue(null, SoundModelContract.KEY_TYPE);
        String value1 = parser.getAttributeValue(null, "value1");
        String value2 = parser.getAttributeValue(null, "value2");
        try {
            if ("long".equals(type)) {
                extras.putLong(name, Long.parseLong(value1));
            } else if ("integer".equals(type)) {
                extras.putInt(name, Integer.parseInt(value1));
            } else if ("double".equals(type)) {
                extras.putDouble(name, Double.parseDouble(value1));
            } else if ("float".equals(type)) {
                extras.putFloat(name, Float.parseFloat(value1));
            } else if ("boolean".equals(type)) {
                extras.putBoolean(name, Boolean.parseBoolean(value1));
            } else if ("string".equals(type)) {
                extras.putString(name, value1);
            } else if ("account".equals(type)) {
                extras.putParcelable(name, new Account(value1, value2));
            }
        } catch (NumberFormatException e) {
            Slog.e("SyncManager", "error parsing bundle value", e);
        } catch (NullPointerException e2) {
            Slog.e("SyncManager", "error parsing bundle value", e2);
        }
    }

    private void writeAccountInfoLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            String str = TAG_FILE;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Writing new ");
            stringBuilder.append(this.mAccountInfoFile.getBaseFile());
            Slog.v(str, stringBuilder.toString());
        }
        FileOutputStream fos = null;
        try {
            int m;
            fos = this.mAccountInfoFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            out.startTag(null, "accounts");
            out.attribute(null, "version", Integer.toString(3));
            out.attribute(null, XML_ATTR_NEXT_AUTHORITY_ID, Integer.toString(this.mNextAuthorityId));
            out.attribute(null, XML_ATTR_SYNC_RANDOM_OFFSET, Integer.toString(this.mSyncRandomOffset));
            int M = this.mMasterSyncAutomatically.size();
            int i = 0;
            for (m = 0; m < M; m++) {
                int userId = this.mMasterSyncAutomatically.keyAt(m);
                Boolean listen = (Boolean) this.mMasterSyncAutomatically.valueAt(m);
                out.startTag(null, XML_TAG_LISTEN_FOR_TICKLES);
                out.attribute(null, XML_ATTR_USER, Integer.toString(userId));
                out.attribute(null, XML_ATTR_ENABLED, Boolean.toString(listen.booleanValue()));
                out.endTag(null, XML_TAG_LISTEN_FOR_TICKLES);
            }
            m = this.mAuthorities.size();
            while (i < m) {
                AuthorityInfo authority = (AuthorityInfo) this.mAuthorities.valueAt(i);
                EndPoint info = authority.target;
                out.startTag(null, "authority");
                out.attribute(null, "id", Integer.toString(authority.ident));
                out.attribute(null, XML_ATTR_USER, Integer.toString(info.userId));
                out.attribute(null, XML_ATTR_ENABLED, Boolean.toString(authority.enabled));
                out.attribute(null, "account", info.account.name);
                out.attribute(null, SoundModelContract.KEY_TYPE, info.account.type);
                out.attribute(null, "authority", info.provider);
                out.attribute(null, "syncable", Integer.toString(authority.syncable));
                out.endTag(null, "authority");
                i++;
            }
            out.endTag(null, "accounts");
            out.endDocument();
            this.mAccountInfoFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w("SyncManager", "Error writing accounts", e1);
            if (fos != null) {
                this.mAccountInfoFile.failWrite(fos);
            }
        }
    }

    static int getIntColumn(Cursor c, String name) {
        return c.getInt(c.getColumnIndex(name));
    }

    static long getLongColumn(Cursor c, String name) {
        return c.getLong(c.getColumnIndex(name));
    }

    private void readAndDeleteLegacyAccountInfoLocked() {
        File file = this.mContext.getDatabasePath("syncmanager.db");
        if (file.exists()) {
            String path = file.getPath();
            CursorFactory cursorFactory = null;
            SQLiteDatabase db = null;
            try {
                db = SQLiteDatabase.openDatabase(path, null, 1);
            } catch (SQLiteException e) {
            }
            if (db != null) {
                Cursor c;
                String accountName;
                String accountType;
                boolean hasType = db.getVersion() >= 11;
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Slog.v(TAG_FILE, "Reading legacy sync accounts db");
                }
                SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
                qb.setTables("stats, status");
                HashMap<String, String> map = new HashMap();
                map.put("_id", "status._id as _id");
                map.put("account", "stats.account as account");
                if (hasType) {
                    map.put("account_type", "stats.account_type as account_type");
                }
                map.put("authority", "stats.authority as authority");
                map.put("totalElapsedTime", "totalElapsedTime");
                map.put("numSyncs", "numSyncs");
                map.put("numSourceLocal", "numSourceLocal");
                map.put("numSourcePoll", "numSourcePoll");
                map.put("numSourceServer", "numSourceServer");
                map.put("numSourceUser", "numSourceUser");
                map.put("lastSuccessSource", "lastSuccessSource");
                map.put("lastSuccessTime", "lastSuccessTime");
                map.put("lastFailureSource", "lastFailureSource");
                map.put("lastFailureTime", "lastFailureTime");
                map.put("lastFailureMesg", "lastFailureMesg");
                map.put("pending", "pending");
                qb.setProjectionMap(map);
                qb.appendWhere("stats._id = status.stats_id");
                Cursor c2 = qb.query(db, null, null, null, null, null, null);
                while (true) {
                    c = c2;
                    if (!c.moveToNext()) {
                        break;
                    }
                    accountName = c.getString(c.getColumnIndex("account"));
                    accountType = hasType ? c.getString(c.getColumnIndex("account_type")) : cursorFactory;
                    if (accountType == null) {
                        accountType = "com.google";
                    }
                    AuthorityInfo authority = getOrCreateAuthorityLocked(new EndPoint(new Account(accountName, accountType), c.getString(c.getColumnIndex("authority")), 0), -1, false);
                    if (authority != null) {
                        boolean found = false;
                        int i = this.mSyncStatus.size();
                        SyncStatusInfo st = cursorFactory;
                        while (i > 0) {
                            i--;
                            st = (SyncStatusInfo) this.mSyncStatus.valueAt(i);
                            if (st.authorityId == authority.ident) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            st = new SyncStatusInfo(authority.ident);
                            this.mSyncStatus.put(authority.ident, st);
                        }
                        st.totalStats.totalElapsedTime = getLongColumn(c, "totalElapsedTime");
                        st.totalStats.numSyncs = getIntColumn(c, "numSyncs");
                        st.totalStats.numSourceLocal = getIntColumn(c, "numSourceLocal");
                        st.totalStats.numSourcePoll = getIntColumn(c, "numSourcePoll");
                        st.totalStats.numSourceOther = getIntColumn(c, "numSourceServer");
                        st.totalStats.numSourceUser = getIntColumn(c, "numSourceUser");
                        st.totalStats.numSourcePeriodic = 0;
                        st.lastSuccessSource = getIntColumn(c, "lastSuccessSource");
                        st.lastSuccessTime = getLongColumn(c, "lastSuccessTime");
                        st.lastFailureSource = getIntColumn(c, "lastFailureSource");
                        st.lastFailureTime = getLongColumn(c, "lastFailureTime");
                        st.lastFailureMesg = c.getString(c.getColumnIndex("lastFailureMesg"));
                        st.pending = getIntColumn(c, "pending") != 0;
                    }
                    c2 = c;
                    cursorFactory = null;
                }
                c.close();
                SQLiteQueryBuilder qb2 = new SQLiteQueryBuilder();
                qb2.setTables("settings");
                int i2 = -1;
                Cursor cursor = c;
                cursor = qb2.query(db, null, null, null, null, null, null);
                while (cursor.moveToNext()) {
                    accountName = cursor.getString(cursor.getColumnIndex(Settings.ATTR_NAME));
                    accountType = cursor.getString(cursor.getColumnIndex("value"));
                    if (accountName != null) {
                        if (accountName.equals("listen_for_tickles")) {
                            boolean z = accountType == null || Boolean.parseBoolean(accountType);
                            setMasterSyncAutomatically(z, 0, 0, i2);
                        } else if (accountName.startsWith("sync_provider_")) {
                            String provider = accountName.substring("sync_provider_".length(), accountName.length());
                            int i3 = this.mAuthorities.size();
                            while (i3 > 0) {
                                i3--;
                                AuthorityInfo authority2 = (AuthorityInfo) this.mAuthorities.valueAt(i3);
                                if (authority2.target.provider.equals(provider)) {
                                    boolean z2 = accountType == null || Boolean.parseBoolean(accountType);
                                    authority2.enabled = z2;
                                    authority2.syncable = 1;
                                }
                            }
                        }
                    }
                }
                cursor.close();
                db.close();
                new File(path).delete();
            }
        }
    }

    private void readStatusLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            String str = TAG_FILE;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Reading ");
            stringBuilder.append(this.mStatusFile.getBaseFile());
            Slog.v(str, stringBuilder.toString());
        }
        try {
            byte[] data = this.mStatusFile.readFully();
            Parcel in = Parcel.obtain();
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            while (true) {
                int readInt = in.readInt();
                int token = readInt;
                if (readInt == 0) {
                    return;
                }
                if (token == 100) {
                    SyncStatusInfo status = new SyncStatusInfo(in);
                    if (this.mAuthorities.indexOfKey(status.authorityId) >= 0) {
                        status.pending = false;
                        if (Log.isLoggable(TAG_FILE, 2)) {
                            String str2 = TAG_FILE;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Adding status for id ");
                            stringBuilder2.append(status.authorityId);
                            Slog.v(str2, stringBuilder2.toString());
                        }
                        this.mSyncStatus.put(status.authorityId, status);
                    }
                } else {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Unknown status token: ");
                    stringBuilder3.append(token);
                    Slog.w("SyncManager", stringBuilder3.toString());
                    return;
                }
            }
        } catch (IOException e) {
            Slog.i("SyncManager", "No initial status");
        }
    }

    private void writeStatusLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            String str = TAG_FILE;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Writing new ");
            stringBuilder.append(this.mStatusFile.getBaseFile());
            Slog.v(str, stringBuilder.toString());
        }
        this.mHandler.removeMessages(1);
        try {
            FileOutputStream fos = this.mStatusFile.startWrite();
            Parcel out = Parcel.obtain();
            int N = this.mSyncStatus.size();
            for (int i = 0; i < N; i++) {
                SyncStatusInfo status = (SyncStatusInfo) this.mSyncStatus.valueAt(i);
                out.writeInt(100);
                status.writeToParcel(out, 0);
            }
            out.writeInt(0);
            fos.write(out.marshall());
            out.recycle();
            this.mStatusFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w("SyncManager", "Error writing status", e1);
            if (null != null) {
                this.mStatusFile.failWrite(null);
            }
        }
    }

    private void requestSync(AuthorityInfo authorityInfo, int reason, Bundle extras, int syncExemptionFlag) {
        if (Process.myUid() != 1000 || this.mSyncRequestListener == null) {
            Builder req = new Builder().syncOnce().setExtras(extras);
            req.setSyncAdapter(authorityInfo.target.account, authorityInfo.target.provider);
            ContentResolver.requestSync(req.build());
            return;
        }
        this.mSyncRequestListener.onSyncRequest(authorityInfo.target, reason, extras, syncExemptionFlag);
    }

    private void requestSync(Account account, int userId, int reason, String authority, Bundle extras, int syncExemptionFlag) {
        if (Process.myUid() != 1000 || this.mSyncRequestListener == null) {
            ContentResolver.requestSync(account, authority, extras);
        } else {
            this.mSyncRequestListener.onSyncRequest(new EndPoint(account, authority, userId), reason, extras, syncExemptionFlag);
        }
    }

    private void readStatisticsLocked() {
        try {
            byte[] data = this.mStatisticsFile.readFully();
            Parcel in = Parcel.obtain();
            int index = 0;
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            while (true) {
                int index2 = index;
                index = in.readInt();
                int token = index;
                if (index == 0) {
                    return;
                }
                if (token == 101 || token == 100) {
                    index = in.readInt();
                    if (token == 100) {
                        index = (index - 2009) + 14245;
                    }
                    DayStats ds = new DayStats(index);
                    ds.successCount = in.readInt();
                    ds.successTime = in.readLong();
                    ds.failureCount = in.readInt();
                    ds.failureTime = in.readLong();
                    if (index2 < this.mDayStats.length) {
                        this.mDayStats[index2] = ds;
                        index2++;
                    }
                    index = index2;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown stats token: ");
                    stringBuilder.append(token);
                    Slog.w("SyncManager", stringBuilder.toString());
                    return;
                }
            }
        } catch (IOException e) {
            Slog.i("SyncManager", "No initial statistics");
        }
    }

    private void writeStatisticsLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Writing new ");
            stringBuilder.append(this.mStatisticsFile.getBaseFile());
            Slog.v("SyncManager", stringBuilder.toString());
        }
        this.mHandler.removeMessages(2);
        try {
            FileOutputStream fos = this.mStatisticsFile.startWrite();
            Parcel out = Parcel.obtain();
            for (DayStats ds : this.mDayStats) {
                if (ds == null) {
                    break;
                }
                out.writeInt(101);
                out.writeInt(ds.day);
                out.writeInt(ds.successCount);
                out.writeLong(ds.successTime);
                out.writeInt(ds.failureCount);
                out.writeLong(ds.failureTime);
            }
            out.writeInt(0);
            fos.write(out.marshall());
            out.recycle();
            this.mStatisticsFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w("SyncManager", "Error writing stats", e1);
            if (null != null) {
                this.mStatisticsFile.failWrite(null);
            }
        }
    }

    public void queueBackup() {
        BackupManager.dataChanged(PackageManagerService.PLATFORM_PACKAGE_NAME);
    }

    public void setClockValid() {
        if (!this.mIsClockValid) {
            this.mIsClockValid = true;
            Slog.w("SyncManager", "Clock is valid now.");
        }
    }

    public boolean isClockValid() {
        return this.mIsClockValid;
    }

    public void resetTodayStats(boolean force) {
        if (force) {
            Log.w("SyncManager", "Force resetting today stats.");
        }
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            for (int i = 0; i < N; i++) {
                ((SyncStatusInfo) this.mSyncStatus.valueAt(i)).maybeResetTodayStats(isClockValid(), force);
            }
            writeStatusLocked();
        }
    }
}
