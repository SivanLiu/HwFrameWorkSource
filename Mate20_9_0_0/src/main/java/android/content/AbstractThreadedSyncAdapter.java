package android.content;

import android.accounts.Account;
import android.content.ISyncAdapter.Stub;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Log;
import com.android.internal.util.function.pooled.PooledLambda;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractThreadedSyncAdapter {
    private static final boolean ENABLE_LOG;
    @Deprecated
    public static final int LOG_SYNC_DETAILS = 2743;
    private static final String TAG = "SyncAdapter";
    private boolean mAllowParallelSyncs;
    private final boolean mAutoInitialize;
    private final Context mContext;
    private final ISyncAdapterImpl mISyncAdapterImpl;
    private final AtomicInteger mNumSyncStarts;
    private final Object mSyncThreadLock;
    private final HashMap<Account, SyncThread> mSyncThreads;

    private class SyncThread extends Thread {
        private final Account mAccount;
        private final String mAuthority;
        private final Bundle mExtras;
        private final SyncContext mSyncContext;
        private final Account mThreadsKey;

        private SyncThread(String name, SyncContext syncContext, String authority, Account account, Bundle extras) {
            super(name);
            this.mSyncContext = syncContext;
            this.mAuthority = authority;
            this.mAccount = account;
            this.mExtras = extras;
            this.mThreadsKey = AbstractThreadedSyncAdapter.this.toSyncKey(account);
        }

        /* JADX WARNING: Removed duplicated region for block: B:77:0x0102 A:{Catch:{ SecurityException -> 0x00fb, Error | RuntimeException -> 0x00ec, all -> 0x00ea }} */
        /* JADX WARNING: Removed duplicated region for block: B:81:0x011b  */
        /* JADX WARNING: Removed duplicated region for block: B:84:0x0124  */
        /* JADX WARNING: Removed duplicated region for block: B:87:0x0130 A:{SYNTHETIC} */
        /* JADX WARNING: Removed duplicated region for block: B:92:0x0142  */
        /* JADX WARNING: Removed duplicated region for block: B:72:0x00f3 A:{Catch:{ SecurityException -> 0x00fb, Error | RuntimeException -> 0x00ec, all -> 0x00ea }} */
        /* JADX WARNING: Removed duplicated region for block: B:100:0x0152  */
        /* JADX WARNING: Removed duplicated region for block: B:103:0x015b  */
        /* JADX WARNING: Removed duplicated region for block: B:106:0x0167 A:{SYNTHETIC} */
        /* JADX WARNING: Removed duplicated region for block: B:111:0x0179  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            SecurityException e;
            Throwable th;
            Process.setThreadPriority(10);
            if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                Log.d(AbstractThreadedSyncAdapter.TAG, "Thread started");
            }
            Trace.traceBegin(128, this.mAuthority);
            SyncResult syncResult = new SyncResult();
            ContentProviderClient provider = null;
            try {
                if (isCanceled()) {
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "Already canceled");
                    }
                    Trace.traceEnd(128);
                    if (provider != null) {
                        provider.release();
                    }
                    if (!isCanceled()) {
                        this.mSyncContext.onFinished(syncResult);
                    }
                    synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                        AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                    }
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "Thread finished");
                    }
                    return;
                }
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.d(AbstractThreadedSyncAdapter.TAG, "Calling onPerformSync...");
                }
                ContentProviderClient provider2 = AbstractThreadedSyncAdapter.this.mContext.getContentResolver().acquireContentProviderClient(this.mAuthority);
                if (provider2 != null) {
                    try {
                        AbstractThreadedSyncAdapter.this.onPerformSync(this.mAccount, this.mExtras, this.mAuthority, provider2, syncResult);
                    } catch (SecurityException e2) {
                        e = e2;
                        provider = provider2;
                        if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        }
                        AbstractThreadedSyncAdapter.this.onSecurityException(this.mAccount, this.mExtras, this.mAuthority, syncResult);
                        syncResult.databaseError = true;
                        Trace.traceEnd(128);
                        if (provider != null) {
                        }
                        if (!isCanceled()) {
                        }
                        synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                        }
                        if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        }
                    } catch (Error | RuntimeException e3) {
                        th = e3;
                        provider = provider2;
                        if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        }
                        throw th;
                    } catch (Throwable th2) {
                        th = th2;
                        provider = provider2;
                        Trace.traceEnd(128);
                        if (provider != null) {
                        }
                        if (!isCanceled()) {
                        }
                        synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                        }
                        if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        }
                        throw th;
                    }
                }
                syncResult.databaseError = true;
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.d(AbstractThreadedSyncAdapter.TAG, "onPerformSync done");
                }
                Trace.traceEnd(128);
                if (provider2 != null) {
                    provider2.release();
                }
                if (!isCanceled()) {
                    this.mSyncContext.onFinished(syncResult);
                }
                synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                    AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                }
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.d(AbstractThreadedSyncAdapter.TAG, "Thread finished");
                }
                provider = provider2;
            } catch (SecurityException e4) {
                e = e4;
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.d(AbstractThreadedSyncAdapter.TAG, "SecurityException", e);
                }
                AbstractThreadedSyncAdapter.this.onSecurityException(this.mAccount, this.mExtras, this.mAuthority, syncResult);
                syncResult.databaseError = true;
                Trace.traceEnd(128);
                if (provider != null) {
                    provider.release();
                }
                if (isCanceled()) {
                    this.mSyncContext.onFinished(syncResult);
                }
                synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                    AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                }
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.d(AbstractThreadedSyncAdapter.TAG, "Thread finished");
                }
            } catch (Error | RuntimeException e5) {
                th = e5;
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.d(AbstractThreadedSyncAdapter.TAG, "caught exception", th);
                }
                throw th;
            } catch (Throwable th3) {
                th = th3;
                Trace.traceEnd(128);
                if (provider != null) {
                    provider.release();
                }
                if (isCanceled()) {
                    this.mSyncContext.onFinished(syncResult);
                }
                synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                    AbstractThreadedSyncAdapter.this.mSyncThreads.remove(this.mThreadsKey);
                }
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.d(AbstractThreadedSyncAdapter.TAG, "Thread finished");
                }
                throw th;
            }
        }

        private boolean isCanceled() {
            return Thread.currentThread().isInterrupted();
        }
    }

    private class ISyncAdapterImpl extends Stub {
        private ISyncAdapterImpl() {
        }

        public void onUnsyncableAccount(ISyncAdapterUnsyncableAccountCallback cb) {
            Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$AbstractThreadedSyncAdapter$ISyncAdapterImpl$L6ZtOCe8gjKwJj0908ytPlrD8Rc.INSTANCE, AbstractThreadedSyncAdapter.this, cb));
        }

        /* JADX WARNING: Removed duplicated region for block: B:56:0x010c A:{Catch:{ all -> 0x00fb }} */
        /* JADX WARNING: Removed duplicated region for block: B:60:0x011b  */
        /* JADX WARNING: Missing block: B:26:0x0087, code skipped:
            if (android.content.AbstractThreadedSyncAdapter.access$100() == false) goto L_0x0091;
     */
        /* JADX WARNING: Missing block: B:27:0x0089, code skipped:
            android.util.Log.d(android.content.AbstractThreadedSyncAdapter.TAG, "startSync() finishing");
     */
        /* JADX WARNING: Missing block: B:28:0x0091, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:40:0x00e2, code skipped:
            if (r0 == false) goto L_0x00e9;
     */
        /* JADX WARNING: Missing block: B:42:?, code skipped:
            r14.onFinished(android.content.SyncResult.ALREADY_IN_PROGRESS);
     */
        /* JADX WARNING: Missing block: B:44:0x00ed, code skipped:
            if (android.content.AbstractThreadedSyncAdapter.access$100() == false) goto L_0x00f7;
     */
        /* JADX WARNING: Missing block: B:45:0x00ef, code skipped:
            android.util.Log.d(android.content.AbstractThreadedSyncAdapter.TAG, "startSync() finishing");
     */
        /* JADX WARNING: Missing block: B:46:0x00f7, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void startSync(ISyncContext syncContext, String authority, Account account, Bundle extras) {
            StringBuilder stringBuilder;
            Throwable th;
            ISyncContext iSyncContext;
            String str = authority;
            Account account2 = account;
            Bundle bundle = extras;
            if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                if (bundle != null) {
                    extras.size();
                }
                String str2 = AbstractThreadedSyncAdapter.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("startSync() start ");
                stringBuilder.append(str);
                stringBuilder.append(" ");
                stringBuilder.append(account2);
                stringBuilder.append(" ");
                stringBuilder.append(bundle);
                Log.d(str2, stringBuilder.toString());
            }
            try {
                try {
                    SyncContext syncContextClient = new SyncContext(syncContext);
                    Account threadsKey = AbstractThreadedSyncAdapter.this.toSyncKey(account2);
                    synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                        boolean alreadyInProgress = true;
                        if (AbstractThreadedSyncAdapter.this.mSyncThreads.containsKey(threadsKey)) {
                            if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                                Log.d(AbstractThreadedSyncAdapter.TAG, "  alreadyInProgress");
                            }
                        } else if (AbstractThreadedSyncAdapter.this.mAutoInitialize && bundle != null && bundle.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false)) {
                            try {
                                if (ContentResolver.getIsSyncable(account2, str) < 0) {
                                    ContentResolver.setIsSyncable(account2, str, 1);
                                }
                            } finally {
                                syncContextClient.onFinished(new SyncResult());
                            }
                        } else {
                            AbstractThreadedSyncAdapter abstractThreadedSyncAdapter = AbstractThreadedSyncAdapter.this;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("SyncAdapterThread-");
                            stringBuilder.append(AbstractThreadedSyncAdapter.this.mNumSyncStarts.incrementAndGet());
                            SyncThread syncThread = new SyncThread(stringBuilder.toString(), syncContextClient, str, account2, bundle);
                            AbstractThreadedSyncAdapter.this.mSyncThreads.put(threadsKey, syncThread);
                            syncThread.start();
                            alreadyInProgress = false;
                        }
                        boolean alreadyInProgress2 = alreadyInProgress;
                    }
                } catch (Error | RuntimeException e) {
                    th = e;
                    try {
                        if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                            Log.d(AbstractThreadedSyncAdapter.TAG, "startSync() caught exception", th);
                        }
                        throw th;
                    } catch (Throwable th2) {
                        th = th2;
                        if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                            Log.d(AbstractThreadedSyncAdapter.TAG, "startSync() finishing");
                        }
                        throw th;
                    }
                }
            } catch (Error | RuntimeException e2) {
                th = e2;
                iSyncContext = syncContext;
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                }
                throw th;
            } catch (Throwable th3) {
                th = th3;
                iSyncContext = syncContext;
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                }
                throw th;
            }
        }

        public void cancelSync(ISyncContext syncContext) {
            SyncThread info = null;
            try {
                synchronized (AbstractThreadedSyncAdapter.this.mSyncThreadLock) {
                    for (SyncThread current : AbstractThreadedSyncAdapter.this.mSyncThreads.values()) {
                        if (current.mSyncContext.getSyncContextBinder() == syncContext.asBinder()) {
                            info = current;
                            break;
                        }
                    }
                }
                if (info != null) {
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        String str = AbstractThreadedSyncAdapter.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("cancelSync() ");
                        stringBuilder.append(info.mAuthority);
                        stringBuilder.append(" ");
                        stringBuilder.append(info.mAccount);
                        Log.d(str, stringBuilder.toString());
                    }
                    if (AbstractThreadedSyncAdapter.this.mAllowParallelSyncs) {
                        AbstractThreadedSyncAdapter.this.onSyncCanceled(info);
                    } else {
                        AbstractThreadedSyncAdapter.this.onSyncCanceled();
                    }
                } else if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.w(AbstractThreadedSyncAdapter.TAG, "cancelSync() unknown context");
                }
                if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                    Log.d(AbstractThreadedSyncAdapter.TAG, "cancelSync() finishing");
                }
            } catch (Error | RuntimeException th) {
                try {
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "cancelSync() caught exception", th);
                    }
                    throw th;
                } catch (Throwable th2) {
                    if (AbstractThreadedSyncAdapter.ENABLE_LOG) {
                        Log.d(AbstractThreadedSyncAdapter.TAG, "cancelSync() finishing");
                    }
                }
            }
        }
    }

    public abstract void onPerformSync(Account account, Bundle bundle, String str, ContentProviderClient contentProviderClient, SyncResult syncResult);

    static {
        boolean z = Build.IS_DEBUGGABLE && Log.isLoggable(TAG, 3);
        ENABLE_LOG = z;
    }

    public AbstractThreadedSyncAdapter(Context context, boolean autoInitialize) {
        this(context, autoInitialize, false);
    }

    public AbstractThreadedSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        this.mSyncThreads = new HashMap();
        this.mSyncThreadLock = new Object();
        this.mContext = context;
        this.mISyncAdapterImpl = new ISyncAdapterImpl();
        this.mNumSyncStarts = new AtomicInteger(0);
        this.mAutoInitialize = autoInitialize;
        this.mAllowParallelSyncs = allowParallelSyncs;
    }

    public Context getContext() {
        return this.mContext;
    }

    private Account toSyncKey(Account account) {
        if (this.mAllowParallelSyncs) {
            return account;
        }
        return null;
    }

    public final IBinder getSyncAdapterBinder() {
        return this.mISyncAdapterImpl.asBinder();
    }

    private void handleOnUnsyncableAccount(ISyncAdapterUnsyncableAccountCallback cb) {
        boolean doSync;
        try {
            doSync = onUnsyncableAccount();
        } catch (RuntimeException e) {
            Log.e(TAG, "Exception while calling onUnsyncableAccount, assuming 'true'", e);
            doSync = true;
        }
        try {
            cb.onUnsyncableAccountDone(doSync);
        } catch (RemoteException e2) {
            Log.e(TAG, "Could not report result of onUnsyncableAccount", e2);
        }
    }

    public boolean onUnsyncableAccount() {
        return true;
    }

    public void onSecurityException(Account account, Bundle extras, String authority, SyncResult syncResult) {
    }

    public void onSyncCanceled() {
        SyncThread syncThread;
        synchronized (this.mSyncThreadLock) {
            syncThread = (SyncThread) this.mSyncThreads.get(null);
        }
        if (syncThread != null) {
            syncThread.interrupt();
        }
    }

    public void onSyncCanceled(Thread thread) {
        thread.interrupt();
    }
}
