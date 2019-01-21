package com.android.server.print;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.print.IPrintSpooler;
import android.print.IPrintSpoolerCallbacks;
import android.print.IPrintSpoolerCallbacks.Stub;
import android.print.IPrintSpoolerClient;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.util.Slog;
import android.util.TimedRemoteCaller;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.job.controllers.JobStatus;
import com.android.server.utils.PriorityDump;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeoutException;
import libcore.io.IoUtils;

final class RemotePrintSpooler {
    private static final long BIND_SPOOLER_SERVICE_TIMEOUT = (Build.IS_ENG ? JobStatus.DEFAULT_TRIGGER_MAX_DELAY : JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    private static final boolean DEBUG = true;
    private static final String LOG_TAG = "RemotePrintSpooler";
    private final PrintSpoolerCallbacks mCallbacks;
    private boolean mCanUnbind;
    private final ClearCustomPrinterIconCacheCaller mClearCustomPrinterIconCache = new ClearCustomPrinterIconCacheCaller();
    private final PrintSpoolerClient mClient;
    private final Context mContext;
    private final OnCustomPrinterIconLoadedCaller mCustomPrinterIconLoadedCaller = new OnCustomPrinterIconLoadedCaller();
    private boolean mDestroyed;
    private final GetCustomPrinterIconCaller mGetCustomPrinterIconCaller = new GetCustomPrinterIconCaller();
    private final GetPrintJobInfoCaller mGetPrintJobInfoCaller = new GetPrintJobInfoCaller();
    private final GetPrintJobInfosCaller mGetPrintJobInfosCaller = new GetPrintJobInfosCaller();
    private final Intent mIntent;
    @GuardedBy("mLock")
    private boolean mIsBinding;
    private boolean mIsLowPriority;
    private final Object mLock = new Object();
    private IPrintSpooler mRemoteInstance;
    private final ServiceConnection mServiceConnection = new MyServiceConnection();
    private final SetPrintJobStateCaller mSetPrintJobStatusCaller = new SetPrintJobStateCaller();
    private final SetPrintJobTagCaller mSetPrintJobTagCaller = new SetPrintJobTagCaller();
    private final UserHandle mUserHandle;

    private static abstract class BasePrintSpoolerServiceCallbacks extends Stub {
        private BasePrintSpoolerServiceCallbacks() {
        }

        public void onGetPrintJobInfosResult(List<PrintJobInfo> list, int sequence) {
        }

        public void onGetPrintJobInfoResult(PrintJobInfo printJob, int sequence) {
        }

        public void onCancelPrintJobResult(boolean canceled, int sequence) {
        }

        public void onSetPrintJobStateResult(boolean success, int sequece) {
        }

        public void onSetPrintJobTagResult(boolean success, int sequence) {
        }

        public void onCustomPrinterIconCached(int sequence) {
        }

        public void onGetCustomPrinterIconResult(Icon icon, int sequence) {
        }

        public void customPrinterIconCacheCleared(int sequence) {
        }
    }

    private static final class ClearCustomPrinterIconCacheCaller extends TimedRemoteCaller<Void> {
        private final IPrintSpoolerCallbacks mCallback = new BasePrintSpoolerServiceCallbacks() {
            public void customPrinterIconCacheCleared(int sequence) {
                ClearCustomPrinterIconCacheCaller.this.onRemoteMethodResult(null, sequence);
            }
        };

        public ClearCustomPrinterIconCacheCaller() {
            super(5000);
        }

        public Void clearCustomPrinterIconCache(IPrintSpooler target) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.clearCustomPrinterIconCache(this.mCallback, sequence);
            return (Void) getResultTimed(sequence);
        }
    }

    private static final class GetCustomPrinterIconCaller extends TimedRemoteCaller<Icon> {
        private final IPrintSpoolerCallbacks mCallback = new BasePrintSpoolerServiceCallbacks() {
            public void onGetCustomPrinterIconResult(Icon icon, int sequence) {
                GetCustomPrinterIconCaller.this.onRemoteMethodResult(icon, sequence);
            }
        };

        public GetCustomPrinterIconCaller() {
            super(5000);
        }

        public Icon getCustomPrinterIcon(IPrintSpooler target, PrinterId printerId) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.getCustomPrinterIcon(printerId, this.mCallback, sequence);
            return (Icon) getResultTimed(sequence);
        }
    }

    private static final class GetPrintJobInfoCaller extends TimedRemoteCaller<PrintJobInfo> {
        private final IPrintSpoolerCallbacks mCallback = new BasePrintSpoolerServiceCallbacks() {
            public void onGetPrintJobInfoResult(PrintJobInfo printJob, int sequence) {
                GetPrintJobInfoCaller.this.onRemoteMethodResult(printJob, sequence);
            }
        };

        public GetPrintJobInfoCaller() {
            super(5000);
        }

        public PrintJobInfo getPrintJobInfo(IPrintSpooler target, PrintJobId printJobId, int appId) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.getPrintJobInfo(printJobId, this.mCallback, appId, sequence);
            return (PrintJobInfo) getResultTimed(sequence);
        }
    }

    private static final class GetPrintJobInfosCaller extends TimedRemoteCaller<List<PrintJobInfo>> {
        private final IPrintSpoolerCallbacks mCallback = new BasePrintSpoolerServiceCallbacks() {
            public void onGetPrintJobInfosResult(List<PrintJobInfo> printJobs, int sequence) {
                GetPrintJobInfosCaller.this.onRemoteMethodResult(printJobs, sequence);
            }
        };

        public GetPrintJobInfosCaller() {
            super(5000);
        }

        public List<PrintJobInfo> getPrintJobInfos(IPrintSpooler target, ComponentName componentName, int state, int appId) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.getPrintJobInfos(this.mCallback, componentName, state, appId, sequence);
            return (List) getResultTimed(sequence);
        }
    }

    private final class MyServiceConnection implements ServiceConnection {
        private MyServiceConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (RemotePrintSpooler.this.mLock) {
                RemotePrintSpooler.this.mRemoteInstance = IPrintSpooler.Stub.asInterface(service);
                RemotePrintSpooler.this.setClientLocked();
                RemotePrintSpooler.this.mLock.notifyAll();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (RemotePrintSpooler.this.mLock) {
                RemotePrintSpooler.this.clearClientLocked();
                RemotePrintSpooler.this.mRemoteInstance = null;
            }
        }
    }

    private static final class OnCustomPrinterIconLoadedCaller extends TimedRemoteCaller<Void> {
        private final IPrintSpoolerCallbacks mCallback = new BasePrintSpoolerServiceCallbacks() {
            public void onCustomPrinterIconCached(int sequence) {
                OnCustomPrinterIconLoadedCaller.this.onRemoteMethodResult(null, sequence);
            }
        };

        public OnCustomPrinterIconLoadedCaller() {
            super(5000);
        }

        public Void onCustomPrinterIconLoaded(IPrintSpooler target, PrinterId printerId, Icon icon) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.onCustomPrinterIconLoaded(printerId, icon, this.mCallback, sequence);
            return (Void) getResultTimed(sequence);
        }
    }

    public interface PrintSpoolerCallbacks {
        void onAllPrintJobsForServiceHandled(ComponentName componentName);

        void onPrintJobQueued(PrintJobInfo printJobInfo);

        void onPrintJobStateChanged(PrintJobInfo printJobInfo);
    }

    private static final class PrintSpoolerClient extends IPrintSpoolerClient.Stub {
        private final WeakReference<RemotePrintSpooler> mWeakSpooler;

        public PrintSpoolerClient(RemotePrintSpooler spooler) {
            this.mWeakSpooler = new WeakReference(spooler);
        }

        public void onPrintJobQueued(PrintJobInfo printJob) {
            RemotePrintSpooler spooler = (RemotePrintSpooler) this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.mCallbacks.onPrintJobQueued(printJob);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onAllPrintJobsForServiceHandled(ComponentName printService) {
            RemotePrintSpooler spooler = (RemotePrintSpooler) this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.mCallbacks.onAllPrintJobsForServiceHandled(printService);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onAllPrintJobsHandled() {
            RemotePrintSpooler spooler = (RemotePrintSpooler) this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.onAllPrintJobsHandled();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onPrintJobStateChanged(PrintJobInfo printJob) {
            RemotePrintSpooler spooler = (RemotePrintSpooler) this.mWeakSpooler.get();
            if (spooler != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    spooler.onPrintJobStateChanged(printJob);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    private static final class SetPrintJobStateCaller extends TimedRemoteCaller<Boolean> {
        private final IPrintSpoolerCallbacks mCallback = new BasePrintSpoolerServiceCallbacks() {
            public void onSetPrintJobStateResult(boolean success, int sequence) {
                SetPrintJobStateCaller.this.onRemoteMethodResult(Boolean.valueOf(success), sequence);
            }
        };

        public SetPrintJobStateCaller() {
            super(5000);
        }

        public boolean setPrintJobState(IPrintSpooler target, PrintJobId printJobId, int status, String error) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.setPrintJobState(printJobId, status, error, this.mCallback, sequence);
            return ((Boolean) getResultTimed(sequence)).booleanValue();
        }
    }

    private static final class SetPrintJobTagCaller extends TimedRemoteCaller<Boolean> {
        private final IPrintSpoolerCallbacks mCallback = new BasePrintSpoolerServiceCallbacks() {
            public void onSetPrintJobTagResult(boolean success, int sequence) {
                SetPrintJobTagCaller.this.onRemoteMethodResult(Boolean.valueOf(success), sequence);
            }
        };

        public SetPrintJobTagCaller() {
            super(5000);
        }

        public boolean setPrintJobTag(IPrintSpooler target, PrintJobId printJobId, String tag) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.setPrintJobTag(printJobId, tag, this.mCallback, sequence);
            return ((Boolean) getResultTimed(sequence)).booleanValue();
        }
    }

    public RemotePrintSpooler(Context context, int userId, boolean lowPriority, PrintSpoolerCallbacks callbacks) {
        this.mContext = context;
        this.mUserHandle = new UserHandle(userId);
        this.mCallbacks = callbacks;
        this.mIsLowPriority = lowPriority;
        this.mClient = new PrintSpoolerClient(this);
        this.mIntent = new Intent();
        this.mIntent.setComponent(new ComponentName("com.android.printspooler", "com.android.printspooler.model.PrintSpoolerService"));
    }

    public void increasePriority() {
        if (this.mIsLowPriority) {
            this.mIsLowPriority = false;
            synchronized (this.mLock) {
                throwIfDestroyedLocked();
                while (!this.mCanUnbind) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                        Slog.e(LOG_TAG, "Interrupted while waiting for operation to complete");
                    }
                }
                Slog.i(LOG_TAG, "Unbinding as previous binding was low priority");
                unbindLocked();
            }
        }
    }

    public final List<PrintJobInfo> getPrintJobInfos(ComponentName componentName, int state, int appId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            List printJobInfos = this.mGetPrintJobInfosCaller.getPrintJobInfos(getRemoteInstanceLazy(), componentName, state, appId);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] getPrintJobInfos()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
            return printJobInfos;
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            StringBuilder stringBuilder2;
            try {
                Slog.e(LOG_TAG, "Error getting print jobs.", e);
                String str2 = LOG_TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[user: ");
                stringBuilder2.append(this.mUserHandle.getIdentifier());
                stringBuilder2.append("] getPrintJobInfos()");
                Slog.i(str2, stringBuilder2.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return null;
                }
            } catch (Throwable th) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[user: ");
                stringBuilder2.append(this.mUserHandle.getIdentifier());
                stringBuilder2.append("] getPrintJobInfos()");
                Slog.i(LOG_TAG, stringBuilder2.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final void createPrintJob(PrintJobInfo printJob) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            getRemoteInstanceLazy().createPrintJob(printJob);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] createPrintJob()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            try {
                Slog.e(LOG_TAG, "Error creating print job.", e);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] createPrintJob()");
                Slog.i(str, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (Throwable th) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] createPrintJob()");
                Slog.i(LOG_TAG, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final void writePrintJobData(ParcelFileDescriptor fd, PrintJobId printJobId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            getRemoteInstanceLazy().writePrintJobData(fd, printJobId);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] writePrintJobData()");
            Slog.i(str, stringBuilder.toString());
            IoUtils.closeQuietly(fd);
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            try {
                Slog.e(LOG_TAG, "Error writing print job data.", e);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] writePrintJobData()");
                Slog.i(str, stringBuilder.toString());
                IoUtils.closeQuietly(fd);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (Throwable th) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] writePrintJobData()");
                Slog.i(LOG_TAG, stringBuilder.toString());
                IoUtils.closeQuietly(fd);
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int appId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            PrintJobInfo printJobInfo = this.mGetPrintJobInfoCaller.getPrintJobInfo(getRemoteInstanceLazy(), printJobId, appId);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] getPrintJobInfo()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
            return printJobInfo;
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            StringBuilder stringBuilder2;
            try {
                Slog.e(LOG_TAG, "Error getting print job info.", e);
                String str2 = LOG_TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[user: ");
                stringBuilder2.append(this.mUserHandle.getIdentifier());
                stringBuilder2.append("] getPrintJobInfo()");
                Slog.i(str2, stringBuilder2.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return null;
                }
            } catch (Throwable th) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[user: ");
                stringBuilder2.append(this.mUserHandle.getIdentifier());
                stringBuilder2.append("] getPrintJobInfo()");
                Slog.i(LOG_TAG, stringBuilder2.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final boolean setPrintJobState(PrintJobId printJobId, int state, String error) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        StringBuilder stringBuilder;
        try {
            boolean printJobState = this.mSetPrintJobStatusCaller.setPrintJobState(getRemoteInstanceLazy(), printJobId, state, error);
            String str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] setPrintJobState()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
            return printJobState;
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            try {
                Slog.e(LOG_TAG, "Error setting print job state.", e);
                String str2 = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] setPrintJobState()");
                Slog.i(str2, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return false;
                }
            } catch (Throwable th) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[user: ");
                stringBuilder2.append(this.mUserHandle.getIdentifier());
                stringBuilder2.append("] setPrintJobState()");
                Slog.i(LOG_TAG, stringBuilder2.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final void setProgress(PrintJobId printJobId, float progress) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            getRemoteInstanceLazy().setProgress(printJobId, progress);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] setProgress()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException | InterruptedException | TimeoutException re) {
            try {
                Slog.e(LOG_TAG, "Error setting progress.", re);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] setProgress()");
                Slog.i(str, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (Throwable th) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] setProgress()");
                Slog.i(LOG_TAG, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final void setStatus(PrintJobId printJobId, CharSequence status) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            getRemoteInstanceLazy().setStatus(printJobId, status);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] setStatus()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            try {
                Slog.e(LOG_TAG, "Error setting status.", e);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] setStatus()");
                Slog.i(str, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (Throwable th) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] setStatus()");
                Slog.i(LOG_TAG, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final void setStatus(PrintJobId printJobId, int status, CharSequence appPackageName) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            getRemoteInstanceLazy().setStatusRes(printJobId, status, appPackageName);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] setStatus()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            try {
                Slog.e(LOG_TAG, "Error setting status.", e);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] setStatus()");
                Slog.i(str, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (Throwable th) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] setStatus()");
                Slog.i(LOG_TAG, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            this.mCustomPrinterIconLoadedCaller.onCustomPrinterIconLoaded(getRemoteInstanceLazy(), printerId, icon);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] onCustomPrinterIconLoaded()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException | InterruptedException | TimeoutException re) {
            try {
                Slog.e(LOG_TAG, "Error loading new custom printer icon.", re);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] onCustomPrinterIconLoaded()");
                Slog.i(str, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (Throwable th) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] onCustomPrinterIconLoaded()");
                Slog.i(LOG_TAG, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final Icon getCustomPrinterIcon(PrinterId printerId) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        try {
            Icon customPrinterIcon = this.mGetCustomPrinterIconCaller.getCustomPrinterIcon(getRemoteInstanceLazy(), printerId);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] getCustomPrinterIcon()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
            return customPrinterIcon;
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            try {
                Slog.e(LOG_TAG, "Error getting custom printer icon.", e);
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[user: ");
                stringBuilder2.append(this.mUserHandle.getIdentifier());
                stringBuilder2.append("] getCustomPrinterIcon()");
                Slog.i(str2, stringBuilder2.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return null;
                }
            } catch (Throwable th) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("[user: ");
                stringBuilder3.append(this.mUserHandle.getIdentifier());
                stringBuilder3.append("] getCustomPrinterIcon()");
                Slog.i(LOG_TAG, stringBuilder3.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public void clearCustomPrinterIconCache() {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            this.mClearCustomPrinterIconCache.clearCustomPrinterIconCache(getRemoteInstanceLazy());
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] clearCustomPrinterIconCache()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            try {
                Slog.e(LOG_TAG, "Error clearing custom printer icon cache.", e);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] clearCustomPrinterIconCache()");
                Slog.i(str, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (Throwable th) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] clearCustomPrinterIconCache()");
                Slog.i(LOG_TAG, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final boolean setPrintJobTag(PrintJobId printJobId, String tag) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        StringBuilder stringBuilder;
        try {
            boolean printJobTag = this.mSetPrintJobTagCaller.setPrintJobTag(getRemoteInstanceLazy(), printJobId, tag);
            String str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] setPrintJobTag()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
            return printJobTag;
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            try {
                Slog.e(LOG_TAG, "Error setting print job tag.", e);
                String str2 = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] setPrintJobTag()");
                Slog.i(str2, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                    return false;
                }
            } catch (Throwable th) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[user: ");
                stringBuilder2.append(this.mUserHandle.getIdentifier());
                stringBuilder2.append("] setPrintJobTag()");
                Slog.i(LOG_TAG, stringBuilder2.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final void setPrintJobCancelling(PrintJobId printJobId, boolean cancelling) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            getRemoteInstanceLazy().setPrintJobCancelling(printJobId, cancelling);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] setPrintJobCancelling()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            try {
                Slog.e(LOG_TAG, "Error setting print job cancelling.", e);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] setPrintJobCancelling()");
                Slog.i(str, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (Throwable th) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] setPrintJobCancelling()");
                Slog.i(LOG_TAG, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final void getRemoteInstanceLazyFirstly() {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            getRemoteInstanceLazy();
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] pruneApprovedPrintServices()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (TimeoutException re) {
            Slog.e(LOG_TAG, "Can not get remote service.", re);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] pruneApprovedPrintServices()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (InterruptedException e) {
            try {
                Slog.e(LOG_TAG, "Can not get remote service.", e);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] pruneApprovedPrintServices()");
                Slog.i(str, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (Throwable th) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] pruneApprovedPrintServices()");
                Slog.i(LOG_TAG, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final void pruneApprovedPrintServices(List<ComponentName> servicesToKeep) {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            getRemoteInstanceLazy().pruneApprovedPrintServices(servicesToKeep);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] pruneApprovedPrintServices()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException | InterruptedException | TimeoutException e) {
            try {
                Slog.e(LOG_TAG, "Error pruning approved print services.", e);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] pruneApprovedPrintServices()");
                Slog.i(str, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (Throwable th) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] pruneApprovedPrintServices()");
                Slog.i(LOG_TAG, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final void removeObsoletePrintJobs() {
        throwIfCalledOnMainThread();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            this.mCanUnbind = false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            getRemoteInstanceLazy().removeObsoletePrintJobs();
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] removeObsoletePrintJobs()");
            Slog.i(str, stringBuilder.toString());
            synchronized (this.mLock) {
                this.mCanUnbind = true;
                this.mLock.notifyAll();
            }
        } catch (RemoteException | InterruptedException | TimeoutException te) {
            try {
                Slog.e(LOG_TAG, "Error removing obsolete print jobs .", te);
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] removeObsoletePrintJobs()");
                Slog.i(str, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            } catch (Throwable th) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserHandle.getIdentifier());
                stringBuilder.append("] removeObsoletePrintJobs()");
                Slog.i(LOG_TAG, stringBuilder.toString());
                synchronized (this.mLock) {
                    this.mCanUnbind = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public final void destroy() {
        throwIfCalledOnMainThread();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[user: ");
        stringBuilder.append(this.mUserHandle.getIdentifier());
        stringBuilder.append("] destroy()");
        Slog.i(str, stringBuilder.toString());
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            unbindLocked();
            this.mDestroyed = true;
            this.mCanUnbind = false;
        }
    }

    public void dump(DualDumpOutputStream dumpStream) {
        synchronized (this.mLock) {
            dumpStream.write("is_destroyed", 1133871366145L, this.mDestroyed);
            dumpStream.write("is_bound", 1133871366146L, this.mRemoteInstance != null);
        }
        try {
            if (dumpStream.isProto()) {
                dumpStream.write(null, 1146756268035L, TransferPipe.dumpAsync(getRemoteInstanceLazy().asBinder(), new String[]{PriorityDump.PROTO_ARG}));
            } else {
                dumpStream.writeNested("internal_state", TransferPipe.dumpAsync(getRemoteInstanceLazy().asBinder(), new String[0]));
            }
        } catch (RemoteException | IOException | InterruptedException | TimeoutException e) {
            Slog.e(LOG_TAG, "Failed to dump remote instance", e);
        }
    }

    private void onAllPrintJobsHandled() {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            unbindLocked();
        }
    }

    private void onPrintJobStateChanged(PrintJobInfo printJob) {
        this.mCallbacks.onPrintJobStateChanged(printJob);
    }

    private IPrintSpooler getRemoteInstanceLazy() throws TimeoutException, InterruptedException {
        synchronized (this.mLock) {
            IPrintSpooler iPrintSpooler;
            if (this.mRemoteInstance != null) {
                iPrintSpooler = this.mRemoteInstance;
                return iPrintSpooler;
            }
            bindLocked();
            iPrintSpooler = this.mRemoteInstance;
            return iPrintSpooler;
        }
    }

    @GuardedBy("mLock")
    private void bindLocked() throws TimeoutException, InterruptedException {
        while (this.mIsBinding) {
            this.mLock.wait();
        }
        if (this.mRemoteInstance == null) {
            this.mIsBinding = true;
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] bindLocked() ");
            stringBuilder.append(this.mIsLowPriority ? "low priority" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            Slog.i(str, stringBuilder.toString());
            try {
                int flags;
                if (this.mIsLowPriority) {
                    flags = 1;
                } else {
                    flags = 67108865;
                }
                this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, flags, this.mUserHandle);
                long startMillis = SystemClock.uptimeMillis();
                while (this.mRemoteInstance == null) {
                    long remainingMillis = BIND_SPOOLER_SERVICE_TIMEOUT - (SystemClock.uptimeMillis() - startMillis);
                    if (remainingMillis > 0) {
                        this.mLock.wait(remainingMillis);
                    } else {
                        throw new TimeoutException("Cannot get spooler!");
                    }
                }
                this.mCanUnbind = true;
            } finally {
                this.mIsBinding = false;
                this.mLock.notifyAll();
            }
        }
    }

    private void unbindLocked() {
        if (this.mRemoteInstance != null) {
            while (!this.mCanUnbind) {
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                }
            }
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserHandle.getIdentifier());
            stringBuilder.append("] unbindLocked()");
            Slog.i(str, stringBuilder.toString());
            clearClientLocked();
            this.mRemoteInstance = null;
            this.mContext.unbindService(this.mServiceConnection);
        }
    }

    private void setClientLocked() {
        if (this.mRemoteInstance == null) {
            Slog.e(LOG_TAG, "set Client fail,because mRemoteInstance is null.");
            return;
        }
        try {
            this.mRemoteInstance.setClient(this.mClient);
        } catch (RemoteException re) {
            Slog.d(LOG_TAG, "Error setting print spooler client", re);
        }
    }

    private void clearClientLocked() {
        if (this.mRemoteInstance == null) {
            Slog.e(LOG_TAG, "clear Client fail,because mRemoteInstance is null.");
            return;
        }
        try {
            this.mRemoteInstance.setClient(null);
        } catch (RemoteException re) {
            Slog.d(LOG_TAG, "Error clearing print spooler client", re);
        }
    }

    private void throwIfDestroyedLocked() {
        if (this.mDestroyed) {
            throw new IllegalStateException("Cannot interact with a destroyed instance.");
        }
    }

    private void throwIfCalledOnMainThread() {
        if (Thread.currentThread() == this.mContext.getMainLooper().getThread()) {
            throw new RuntimeException("Cannot invoke on the main thread");
        }
    }
}
