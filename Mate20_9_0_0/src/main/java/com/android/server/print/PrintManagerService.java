package com.android.server.print;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.os.UserManager;
import android.print.IPrintDocumentAdapter;
import android.print.IPrintJobStateChangeListener;
import android.print.IPrintManager.Stub;
import android.print.IPrintServicesChangeListener;
import android.print.IPrinterDiscoveryObserver;
import android.print.PrintAttributes;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.printservice.PrintServiceInfo;
import android.printservice.recommendation.IRecommendationsChangeListener;
import android.printservice.recommendation.RecommendationInfo;
import android.provider.Settings.Secure;
import android.util.Log;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.widget.Toast;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.power.IHwShutdownThread;
import com.android.server.utils.PriorityDump;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public final class PrintManagerService extends SystemService {
    private static final String LOG_TAG = "PrintManagerService";
    private final PrintManagerImpl mPrintManagerImpl;

    class PrintManagerImpl extends Stub {
        private static final int BACKGROUND_USER_ID = -10;
        private final Context mContext;
        private final Object mLock = new Object();
        private final UserManager mUserManager;
        private final SparseArray<UserState> mUserStates = new SparseArray();

        PrintManagerImpl(Context context) {
            this.mContext = context;
            this.mUserManager = (UserManager) context.getSystemService("user");
            registerContentObservers();
            registerBroadcastReceivers();
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new PrintShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        /* JADX WARNING: Missing block: B:28:0x0085, code:
            r7 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:30:?, code:
            r2 = r1.print(r10, r11, r12, r5, r6);
     */
        /* JADX WARNING: Missing block: B:33:0x0096, code:
            android.os.Binder.restoreCallingIdentity(r7);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public Bundle print(String printJobName, IPrintDocumentAdapter adapter, PrintAttributes attributes, String packageName, int appId, int userId) {
            adapter = (IPrintDocumentAdapter) Preconditions.checkNotNull(adapter);
            if (isPrintingEnabled()) {
                printJobName = (String) Preconditions.checkStringNotEmpty(printJobName);
                packageName = (String) Preconditions.checkStringNotEmpty(packageName);
                int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
                synchronized (this.mLock) {
                    if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                        return null;
                    }
                    int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                    String resolvedPackageName = resolveCallingPackageNameEnforcingSecurity(packageName);
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
                }
            } else {
                CharSequence disabledMessage = null;
                DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
                int callingUserId = UserHandle.getCallingUserId();
                long identity = Binder.clearCallingIdentity();
                try {
                    disabledMessage = dpmi.getPrintingDisabledReasonForUser(callingUserId);
                    if (disabledMessage != null) {
                        Toast.makeText(this.mContext, Looper.getMainLooper(), disabledMessage, 1).show();
                    }
                    try {
                        adapter.start();
                    } catch (RemoteException e) {
                        Log.e(PrintManagerService.LOG_TAG, "Error calling IPrintDocumentAdapter.start()");
                    }
                    try {
                        adapter.finish();
                    } catch (RemoteException e2) {
                        Log.e(PrintManagerService.LOG_TAG, "Error calling IPrintDocumentAdapter.finish()");
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
            return r2;
        }

        /* JADX WARNING: Missing block: B:10:0x001e, code:
            r4 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:12:?, code:
            r1 = r3.getPrintJobInfos(r2);
     */
        /* JADX WARNING: Missing block: B:15:0x002b, code:
            android.os.Binder.restoreCallingIdentity(r4);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public List<PrintJobInfo> getPrintJobInfos(int appId, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return null;
                }
                int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
            return r1;
        }

        /* JADX WARNING: Missing block: B:12:0x0021, code:
            r4 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:14:?, code:
            r2 = r3.getPrintJobInfo(r7, r0);
     */
        /* JADX WARNING: Missing block: B:17:0x002e, code:
            android.os.Binder.restoreCallingIdentity(r4);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int appId, int userId) {
            if (printJobId == null) {
                return null;
            }
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return null;
                }
                int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
            return r2;
        }

        /* JADX WARNING: Missing block: B:10:0x0021, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:12:?, code:
            r1 = r2.getCustomPrinterIcon(r6);
     */
        /* JADX WARNING: Missing block: B:15:0x002e, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public Icon getCustomPrinterIcon(PrinterId printerId, int userId) {
            printerId = (PrinterId) Preconditions.checkNotNull(printerId);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return null;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
            return r1;
        }

        /* JADX WARNING: Missing block: B:11:0x0020, code:
            r4 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:13:?, code:
            r3.cancelPrintJob(r7, r2);
     */
        /* JADX WARNING: Missing block: B:16:0x002d, code:
            android.os.Binder.restoreCallingIdentity(r4);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void cancelPrintJob(PrintJobId printJobId, int appId, int userId) {
            if (printJobId != null) {
                int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
                synchronized (this.mLock) {
                    if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    } else {
                        int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                        UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:12:0x0026, code:
            r4 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:14:?, code:
            r3.restartPrintJob(r7, r2);
     */
        /* JADX WARNING: Missing block: B:17:0x0033, code:
            android.os.Binder.restoreCallingIdentity(r4);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void restartPrintJob(PrintJobId printJobId, int appId, int userId) {
            if (printJobId != null && isPrintingEnabled()) {
                int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
                synchronized (this.mLock) {
                    if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    } else {
                        int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                        UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0025, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r1 = r2.getPrintServices(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x0032, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public List<PrintServiceInfo> getPrintServices(int selectionFlags, int userId) {
            Preconditions.checkFlagsArgument(selectionFlags, 3);
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICES", null);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return null;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
            return r1;
        }

        /* JADX WARNING: Missing block: B:19:0x0051, code:
            r4 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:21:?, code:
            r3.setPrintServiceEnabled(r7, r8);
     */
        /* JADX WARNING: Missing block: B:24:0x005e, code:
            android.os.Binder.restoreCallingIdentity(r4);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void setPrintServiceEnabled(ComponentName service, boolean isEnabled, int userId) {
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            int appId = UserHandle.getAppId(Binder.getCallingUid());
            if (appId != 1000) {
                try {
                    if (appId != UserHandle.getAppId(this.mContext.getPackageManager().getPackageUidAsUser("com.android.printspooler", resolvedUserId))) {
                        throw new SecurityException("Only system and print spooler can call this");
                    }
                } catch (NameNotFoundException e) {
                    Log.e(PrintManagerService.LOG_TAG, "Could not verify caller", e);
                    return;
                }
            }
            service = (ComponentName) Preconditions.checkNotNull(service);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0021, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r1 = r2.getPrintServiceRecommendations();
     */
        /* JADX WARNING: Missing block: B:14:0x002e, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public List<RecommendationInfo> getPrintServiceRecommendations(int userId) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICE_RECOMMENDATIONS", null);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return null;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
            return r1;
        }

        /* JADX WARNING: Missing block: B:9:0x0020, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r2.createPrinterDiscoverySession(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x002d, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void createPrinterDiscoverySession(IPrinterDiscoveryObserver observer, int userId) {
            observer = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(observer);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0020, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r2.destroyPrinterDiscoverySession(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x002d, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void destroyPrinterDiscoverySession(IPrinterDiscoveryObserver observer, int userId) {
            observer = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(observer);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:12:0x002b, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:14:?, code:
            r2.startPrinterDiscovery(r6, r7);
     */
        /* JADX WARNING: Missing block: B:17:0x0038, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void startPrinterDiscovery(IPrinterDiscoveryObserver observer, List<PrinterId> priorityList, int userId) {
            observer = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(observer);
            if (priorityList != null) {
                priorityList = (List) Preconditions.checkCollectionElementsNotNull(priorityList, "PrinterId");
            }
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0020, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r2.stopPrinterDiscovery(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x002d, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void stopPrinterDiscovery(IPrinterDiscoveryObserver observer, int userId) {
            observer = (IPrinterDiscoveryObserver) Preconditions.checkNotNull(observer);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0022, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r2.validatePrinters(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x002f, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void validatePrinters(List<PrinterId> printerIds, int userId) {
            printerIds = (List) Preconditions.checkCollectionElementsNotNull(printerIds, "PrinterId");
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0020, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r2.startPrinterStateTracking(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x002d, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void startPrinterStateTracking(PrinterId printerId, int userId) {
            printerId = (PrinterId) Preconditions.checkNotNull(printerId);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0020, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r2.stopPrinterStateTracking(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x002d, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void stopPrinterStateTracking(PrinterId printerId, int userId) {
            printerId = (PrinterId) Preconditions.checkNotNull(printerId);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0024, code:
            r4 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r3.addPrintJobStateChangeListener(r7, r2);
     */
        /* JADX WARNING: Missing block: B:14:0x0031, code:
            android.os.Binder.restoreCallingIdentity(r4);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void addPrintJobStateChangeListener(IPrintJobStateChangeListener listener, int appId, int userId) throws RemoteException {
            listener = (IPrintJobStateChangeListener) Preconditions.checkNotNull(listener);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                } else {
                    int resolvedAppId = resolveCallingAppEnforcingPermissions(appId);
                    UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
                }
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0020, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r2.removePrintJobStateChangeListener(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x002d, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void removePrintJobStateChangeListener(IPrintJobStateChangeListener listener, int userId) {
            listener = (IPrintJobStateChangeListener) Preconditions.checkNotNull(listener);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0028, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r2.addPrintServicesChangeListener(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x0035, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void addPrintServicesChangeListener(IPrintServicesChangeListener listener, int userId) throws RemoteException {
            listener = (IPrintServicesChangeListener) Preconditions.checkNotNull(listener);
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICES", null);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0028, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r2.removePrintServicesChangeListener(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x0035, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void removePrintServicesChangeListener(IPrintServicesChangeListener listener, int userId) {
            listener = (IPrintServicesChangeListener) Preconditions.checkNotNull(listener);
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICES", null);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0028, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r2.addPrintServiceRecommendationsChangeListener(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x0035, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void addPrintServiceRecommendationsChangeListener(IRecommendationsChangeListener listener, int userId) throws RemoteException {
            listener = (IRecommendationsChangeListener) Preconditions.checkNotNull(listener);
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICE_RECOMMENDATIONS", null);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0028, code:
            r3 = android.os.Binder.clearCallingIdentity();
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            r2.removePrintServiceRecommendationsChangeListener(r6);
     */
        /* JADX WARNING: Missing block: B:14:0x0035, code:
            android.os.Binder.restoreCallingIdentity(r3);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void removePrintServiceRecommendationsChangeListener(IRecommendationsChangeListener listener, int userId) {
            listener = (IRecommendationsChangeListener) Preconditions.checkNotNull(listener);
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRINT_SERVICE_RECOMMENDATIONS", null);
            int resolvedUserId = resolveCallingUserEnforcingPermissions(userId);
            synchronized (this.mLock) {
                if (resolveCallingProfileParentLocked(resolvedUserId) != getCurrentUserId()) {
                    return;
                }
                UserState userState = getOrCreateUserStateLocked(resolvedUserId, null);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            fd = (FileDescriptor) Preconditions.checkNotNull(fd);
            if (DumpUtils.checkDumpPermission(this.mContext, PrintManagerService.LOG_TAG, pw)) {
                int i = 0;
                int opti = 0;
                boolean dumpAsProto = false;
                while (opti < args.length) {
                    String opt = args[opti];
                    if (opt == null || opt.length() <= 0 || opt.charAt(0) != '-') {
                        break;
                    }
                    opti++;
                    if (PriorityDump.PROTO_ARG.equals(opt)) {
                        dumpAsProto = true;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown argument: ");
                        stringBuilder.append(opt);
                        stringBuilder.append("; use -h for help");
                        pw.println(stringBuilder.toString());
                    }
                }
                ArrayList<UserState> userStatesToDump = new ArrayList();
                synchronized (this.mLock) {
                    int numUserStates = this.mUserStates.size();
                    while (i < numUserStates) {
                        userStatesToDump.add((UserState) this.mUserStates.valueAt(i));
                        i++;
                    }
                }
                long identity = Binder.clearCallingIdentity();
                if (dumpAsProto) {
                    try {
                        dump(new DualDumpOutputStream(new ProtoOutputStream(fd)), userStatesToDump);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(identity);
                    }
                } else {
                    pw.println("PRINT MANAGER STATE (dumpsys print)");
                    dump(new DualDumpOutputStream(new IndentingPrintWriter(pw, "  ")), userStatesToDump);
                }
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean getBindInstantServiceAllowed(int userId) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || callingUid == 0) {
                UserState userState;
                synchronized (this.mLock) {
                    userState = getOrCreateUserStateLocked(userId, null);
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    boolean bindInstantServiceAllowed = userState.getBindInstantServiceAllowed();
                    return bindInstantServiceAllowed;
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            } else {
                throw new SecurityException("Can only be called by uid 2000 or 0");
            }
        }

        public void setBindInstantServiceAllowed(int userId, boolean allowed) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || callingUid == 0) {
                UserState userState;
                synchronized (this.mLock) {
                    userState = getOrCreateUserStateLocked(userId, null);
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    userState.setBindInstantServiceAllowed(allowed);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            } else {
                throw new SecurityException("Can only be called by uid 2000 or 0");
            }
        }

        private boolean isPrintingEnabled() {
            return this.mUserManager.hasUserRestriction("no_printing", Binder.getCallingUserHandle()) ^ 1;
        }

        private void dump(DualDumpOutputStream dumpStream, ArrayList<UserState> userStatesToDump) {
            int userStateCount = userStatesToDump.size();
            for (int i = 0; i < userStateCount; i++) {
                long token = dumpStream.start("user_states", 2246267895809L);
                ((UserState) userStatesToDump.get(i)).dump(dumpStream);
                dumpStream.end(token);
            }
            dumpStream.flush();
        }

        private void registerContentObservers() {
            final Uri enabledPrintServicesUri = Secure.getUriFor("disabled_print_services");
            this.mContext.getContentResolver().registerContentObserver(enabledPrintServicesUri, false, new ContentObserver(BackgroundThread.getHandler()) {
                public void onChange(boolean selfChange, Uri uri, int userId) {
                    if (enabledPrintServicesUri.equals(uri)) {
                        synchronized (PrintManagerImpl.this.mLock) {
                            int userCount = PrintManagerImpl.this.mUserStates.size();
                            int i = 0;
                            while (i < userCount) {
                                if (userId == -1 || userId == PrintManagerImpl.this.mUserStates.keyAt(i)) {
                                    ((UserState) PrintManagerImpl.this.mUserStates.valueAt(i)).updateIfNeededLocked();
                                }
                                i++;
                            }
                        }
                    }
                }
            }, -1);
        }

        private void registerBroadcastReceivers() {
            new PackageMonitor() {
                private boolean hasPrintService(String packageName) {
                    Intent intent = new Intent("android.printservice.PrintService");
                    intent.setPackage(packageName);
                    List<ResolveInfo> installedServices = PrintManagerImpl.this.mContext.getPackageManager().queryIntentServicesAsUser(intent, 276824068, getChangingUserId());
                    return (installedServices == null || installedServices.isEmpty()) ? false : true;
                }

                private boolean hadPrintService(UserState userState, String packageName) {
                    List<PrintServiceInfo> installedServices = userState.getPrintServices(3);
                    if (installedServices == null) {
                        return false;
                    }
                    int numInstalledServices = installedServices.size();
                    for (int i = 0; i < numInstalledServices; i++) {
                        if (((PrintServiceInfo) installedServices.get(i)).getResolveInfo().serviceInfo.packageName.equals(packageName)) {
                            return true;
                        }
                    }
                    return false;
                }

                public void onPackageModified(String packageName) {
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        UserState userState = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false, false);
                        boolean prunePrintServices = false;
                        synchronized (PrintManagerImpl.this.mLock) {
                            if (hadPrintService(userState, packageName) || hasPrintService(packageName)) {
                                userState.updateIfNeededLocked();
                                prunePrintServices = true;
                            }
                        }
                        if (prunePrintServices) {
                            userState.prunePrintServices();
                        }
                    }
                }

                public void onPackageRemoved(String packageName, int uid) {
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        UserState userState = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false, false);
                        boolean prunePrintServices = false;
                        synchronized (PrintManagerImpl.this.mLock) {
                            if (hadPrintService(userState, packageName)) {
                                userState.updateIfNeededLocked();
                                prunePrintServices = true;
                            }
                        }
                        if (prunePrintServices) {
                            userState.prunePrintServices();
                        }
                    }
                }

                /* JADX WARNING: Missing block: B:26:0x0065, code:
            return false;
     */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public boolean onHandleForceStop(Intent intent, String[] stoppedPackages, int uid, boolean doit) {
                    String[] strArr = stoppedPackages;
                    if (!PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        return false;
                    }
                    synchronized (PrintManagerImpl.this.mLock) {
                        UserState userState = PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false, false);
                        boolean stoppedSomePackages = false;
                        List<PrintServiceInfo> enabledServices = userState.getPrintServices(1);
                        if (enabledServices == null) {
                            return false;
                        }
                        for (PrintServiceInfo componentName : enabledServices) {
                            String componentPackage = componentName.getComponentName().getPackageName();
                            int length = strArr.length;
                            int i = 0;
                            while (i < length) {
                                if (!componentPackage.equals(strArr[i])) {
                                    i++;
                                } else if (doit) {
                                    stoppedSomePackages = true;
                                } else {
                                    return true;
                                }
                            }
                        }
                        if (stoppedSomePackages) {
                            userState.updateIfNeededLocked();
                        }
                    }
                }

                public void onPackageAdded(String packageName, int uid) {
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(getChangingUserId())) {
                        synchronized (PrintManagerImpl.this.mLock) {
                            if (hasPrintService(packageName)) {
                                PrintManagerImpl.this.getOrCreateUserStateLocked(getChangingUserId(), false, false).updateIfNeededLocked();
                            }
                        }
                    }
                }
            }.register(this.mContext, BackgroundThread.getHandler().getLooper(), UserHandle.ALL, true);
        }

        private UserState getOrCreateUserStateLocked(int userId, boolean lowPriority) {
            return getOrCreateUserStateLocked(userId, lowPriority, true);
        }

        private UserState getOrCreateUserStateLocked(int userId, boolean lowPriority, boolean enforceUserUnlockingOrUnlocked) {
            if (!enforceUserUnlockingOrUnlocked || this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
                UserState userState = (UserState) this.mUserStates.get(userId);
                if (userState == null) {
                    userState = new UserState(this.mContext, userId, this.mLock, lowPriority);
                    this.mUserStates.put(userId, userState);
                }
                if (!lowPriority) {
                    userState.increasePriority();
                }
                return userState;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User ");
            stringBuilder.append(userId);
            stringBuilder.append(" must be unlocked for printing to be available");
            throw new IllegalStateException(stringBuilder.toString());
        }

        private void handleUserUnlocked(final int userId) {
            BackgroundThread.getHandler().post(new Runnable() {
                public void run() {
                    if (PrintManagerImpl.this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
                        UserState userState;
                        synchronized (PrintManagerImpl.this.mLock) {
                            userState = PrintManagerImpl.this.getOrCreateUserStateLocked(userId, true, false);
                            userState.updateIfNeededLocked();
                        }
                        userState.removeObsoletePrintJobs();
                    }
                }
            });
        }

        private void handleUserStopped(final int userId) {
            BackgroundThread.getHandler().post(new Runnable() {
                public void run() {
                    synchronized (PrintManagerImpl.this.mLock) {
                        UserState userState = (UserState) PrintManagerImpl.this.mUserStates.get(userId);
                        if (userState != null) {
                            userState.destroyLocked();
                            PrintManagerImpl.this.mUserStates.remove(userId);
                        }
                    }
                }
            });
        }

        private int resolveCallingProfileParentLocked(int userId) {
            if (userId == getCurrentUserId()) {
                return userId;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                UserInfo parent = this.mUserManager.getProfileParent(userId);
                if (parent != null) {
                    int identifier = parent.getUserHandle().getIdentifier();
                    return identifier;
                }
                Binder.restoreCallingIdentity(identity);
                return -10;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Missing block: B:14:0x0045, code:
            return r6;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private int resolveCallingAppEnforcingPermissions(int appId) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 0) {
                return appId;
            }
            int callingAppId = UserHandle.getAppId(callingUid);
            if (appId == callingAppId || callingAppId == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || callingAppId == 1000 || this.mContext.checkCallingPermission("com.android.printspooler.permission.ACCESS_ALL_PRINT_JOBS") == 0) {
                return appId;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Call from app ");
            stringBuilder.append(callingAppId);
            stringBuilder.append(" as app ");
            stringBuilder.append(appId);
            stringBuilder.append(" without com.android.printspooler.permission.ACCESS_ALL_PRINT_JOBS");
            throw new SecurityException(stringBuilder.toString());
        }

        private int resolveCallingUserEnforcingPermissions(int userId) {
            try {
                return ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, null);
            } catch (RemoteException e) {
                return userId;
            }
        }

        private String resolveCallingPackageNameEnforcingSecurity(String packageName) {
            for (Object equals : this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid())) {
                if (packageName.equals(equals)) {
                    return packageName;
                }
            }
            throw new IllegalArgumentException("packageName has to belong to the caller");
        }

        private int getCurrentUserId() {
            long identity = Binder.clearCallingIdentity();
            try {
                int currentUser = ActivityManager.getCurrentUser();
                return currentUser;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public PrintManagerService(Context context) {
        super(context);
        this.mPrintManagerImpl = new PrintManagerImpl(context);
    }

    public void onStart() {
        publishBinderService("print", this.mPrintManagerImpl);
    }

    public void onUnlockUser(int userHandle) {
        this.mPrintManagerImpl.handleUserUnlocked(userHandle);
    }

    public void onStopUser(int userHandle) {
        this.mPrintManagerImpl.handleUserStopped(userHandle);
    }
}
