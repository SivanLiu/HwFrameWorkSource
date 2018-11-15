package com.android.server.autofill;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.service.autofill.IAutofillFieldClassificationService;
import android.util.Log;
import android.util.Slog;
import android.view.autofill.AutofillValue;
import com.android.internal.annotations.GuardedBy;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class FieldClassificationStrategy {
    private static final String TAG = "FieldClassificationStrategy";
    private final Context mContext;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private ArrayList<Command> mQueuedCommands;
    @GuardedBy("mLock")
    private IAutofillFieldClassificationService mRemoteService;
    @GuardedBy("mLock")
    private ServiceConnection mServiceConnection;
    private final int mUserId;

    private interface Command {
        void run(IAutofillFieldClassificationService iAutofillFieldClassificationService) throws RemoteException;
    }

    private interface MetadataParser<T> {
        T get(Resources resources, int i);
    }

    public FieldClassificationStrategy(Context context, int userId) {
        this.mContext = context;
        this.mUserId = userId;
    }

    ServiceInfo getServiceInfo() {
        String packageName = this.mContext.getPackageManager().getServicesSystemSharedLibraryPackageName();
        if (packageName == null) {
            Slog.w(TAG, "no external services package!");
            return null;
        }
        Intent intent = new Intent("android.service.autofill.AutofillFieldClassificationService");
        intent.setPackage(packageName);
        ResolveInfo resolveInfo = this.mContext.getPackageManager().resolveService(intent, 132);
        if (resolveInfo != null && resolveInfo.serviceInfo != null) {
            return resolveInfo.serviceInfo;
        }
        Slog.w(TAG, "No valid components found.");
        return null;
    }

    private ComponentName getServiceComponentName() {
        ServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo == null) {
            return null;
        }
        ComponentName name = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        if ("android.permission.BIND_AUTOFILL_FIELD_CLASSIFICATION_SERVICE".equals(serviceInfo.permission)) {
            if (Helper.sVerbose) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getServiceComponentName(): ");
                stringBuilder.append(name);
                Slog.v(str, stringBuilder.toString());
            }
            return name;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(name.flattenToShortString());
        stringBuilder2.append(" does not require permission ");
        stringBuilder2.append("android.permission.BIND_AUTOFILL_FIELD_CLASSIFICATION_SERVICE");
        Slog.w(str2, stringBuilder2.toString());
        return null;
    }

    void reset() {
        synchronized (this.mLock) {
            if (this.mServiceConnection != null) {
                if (Helper.sDebug) {
                    Slog.d(TAG, "reset(): unbinding service.");
                }
                this.mContext.unbindService(this.mServiceConnection);
                this.mServiceConnection = null;
            } else if (Helper.sDebug) {
                Slog.d(TAG, "reset(): service is not bound. Do nothing.");
            }
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Exception block dominator not found, method:com.android.server.autofill.FieldClassificationStrategy.connectAndRun(com.android.server.autofill.FieldClassificationStrategy$Command):void, dom blocks: [B:5:0x0007, B:33:0x0093]
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.searchTryCatchDominators(ProcessTryCatchRegions.java:89)
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.process(ProcessTryCatchRegions.java:45)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.postProcessRegions(RegionMakerVisitor.java:63)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:58)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private void connectAndRun(com.android.server.autofill.FieldClassificationStrategy.Command r10) {
        /*
        r9 = this;
        r0 = r9.mLock;
        monitor-enter(r0);
        r1 = r9.mRemoteService;	 Catch:{ all -> 0x00b6 }
        if (r1 == 0) goto L_0x0032;
    L_0x0007:
        r1 = com.android.server.autofill.Helper.sVerbose;	 Catch:{ RemoteException -> 0x0019 }
        if (r1 == 0) goto L_0x0013;	 Catch:{ RemoteException -> 0x0019 }
    L_0x000b:
        r1 = "FieldClassificationStrategy";	 Catch:{ RemoteException -> 0x0019 }
        r2 = "running command right away";	 Catch:{ RemoteException -> 0x0019 }
        android.util.Slog.v(r1, r2);	 Catch:{ RemoteException -> 0x0019 }
    L_0x0013:
        r1 = r9.mRemoteService;	 Catch:{ RemoteException -> 0x0019 }
        r10.run(r1);	 Catch:{ RemoteException -> 0x0019 }
        goto L_0x0030;
    L_0x0019:
        r1 = move-exception;
        r2 = "FieldClassificationStrategy";	 Catch:{ all -> 0x00b6 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00b6 }
        r3.<init>();	 Catch:{ all -> 0x00b6 }
        r4 = "exception calling service: ";	 Catch:{ all -> 0x00b6 }
        r3.append(r4);	 Catch:{ all -> 0x00b6 }
        r3.append(r1);	 Catch:{ all -> 0x00b6 }
        r3 = r3.toString();	 Catch:{ all -> 0x00b6 }
        android.util.Slog.w(r2, r3);	 Catch:{ all -> 0x00b6 }
    L_0x0030:
        monitor-exit(r0);	 Catch:{ all -> 0x00b6 }
        return;	 Catch:{ all -> 0x00b6 }
    L_0x0032:
        r1 = com.android.server.autofill.Helper.sDebug;	 Catch:{ all -> 0x00b6 }
        if (r1 == 0) goto L_0x003e;	 Catch:{ all -> 0x00b6 }
    L_0x0036:
        r1 = "FieldClassificationStrategy";	 Catch:{ all -> 0x00b6 }
        r2 = "service is null; queuing command";	 Catch:{ all -> 0x00b6 }
        android.util.Slog.d(r1, r2);	 Catch:{ all -> 0x00b6 }
    L_0x003e:
        r1 = r9.mQueuedCommands;	 Catch:{ all -> 0x00b6 }
        r2 = 1;	 Catch:{ all -> 0x00b6 }
        if (r1 != 0) goto L_0x004a;	 Catch:{ all -> 0x00b6 }
    L_0x0043:
        r1 = new java.util.ArrayList;	 Catch:{ all -> 0x00b6 }
        r1.<init>(r2);	 Catch:{ all -> 0x00b6 }
        r9.mQueuedCommands = r1;	 Catch:{ all -> 0x00b6 }
    L_0x004a:
        r1 = r9.mQueuedCommands;	 Catch:{ all -> 0x00b6 }
        r1.add(r10);	 Catch:{ all -> 0x00b6 }
        r1 = r9.mServiceConnection;	 Catch:{ all -> 0x00b6 }
        if (r1 == 0) goto L_0x0055;	 Catch:{ all -> 0x00b6 }
    L_0x0053:
        monitor-exit(r0);	 Catch:{ all -> 0x00b6 }
        return;	 Catch:{ all -> 0x00b6 }
    L_0x0055:
        r1 = com.android.server.autofill.Helper.sVerbose;	 Catch:{ all -> 0x00b6 }
        if (r1 == 0) goto L_0x0060;	 Catch:{ all -> 0x00b6 }
    L_0x0059:
        r1 = "FieldClassificationStrategy";	 Catch:{ all -> 0x00b6 }
        r3 = "creating connection";	 Catch:{ all -> 0x00b6 }
        android.util.Slog.v(r1, r3);	 Catch:{ all -> 0x00b6 }
    L_0x0060:
        r1 = new com.android.server.autofill.FieldClassificationStrategy$1;	 Catch:{ all -> 0x00b6 }
        r1.<init>();	 Catch:{ all -> 0x00b6 }
        r9.mServiceConnection = r1;	 Catch:{ all -> 0x00b6 }
        r1 = r9.getServiceComponentName();	 Catch:{ all -> 0x00b6 }
        r3 = com.android.server.autofill.Helper.sVerbose;	 Catch:{ all -> 0x00b6 }
        if (r3 == 0) goto L_0x0085;	 Catch:{ all -> 0x00b6 }
    L_0x006f:
        r3 = "FieldClassificationStrategy";	 Catch:{ all -> 0x00b6 }
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00b6 }
        r4.<init>();	 Catch:{ all -> 0x00b6 }
        r5 = "binding to: ";	 Catch:{ all -> 0x00b6 }
        r4.append(r5);	 Catch:{ all -> 0x00b6 }
        r4.append(r1);	 Catch:{ all -> 0x00b6 }
        r4 = r4.toString();	 Catch:{ all -> 0x00b6 }
        android.util.Slog.v(r3, r4);	 Catch:{ all -> 0x00b6 }
    L_0x0085:
        if (r1 == 0) goto L_0x00b4;	 Catch:{ all -> 0x00b6 }
    L_0x0087:
        r3 = new android.content.Intent;	 Catch:{ all -> 0x00b6 }
        r3.<init>();	 Catch:{ all -> 0x00b6 }
        r3.setComponent(r1);	 Catch:{ all -> 0x00b6 }
        r4 = android.os.Binder.clearCallingIdentity();	 Catch:{ all -> 0x00b6 }
        r6 = r9.mContext;	 Catch:{ all -> 0x00af }
        r7 = r9.mServiceConnection;	 Catch:{ all -> 0x00af }
        r8 = r9.mUserId;	 Catch:{ all -> 0x00af }
        r8 = android.os.UserHandle.of(r8);	 Catch:{ all -> 0x00af }
        r6.bindServiceAsUser(r3, r7, r2, r8);	 Catch:{ all -> 0x00af }
        r2 = com.android.server.autofill.Helper.sVerbose;	 Catch:{ all -> 0x00af }
        if (r2 == 0) goto L_0x00ab;	 Catch:{ all -> 0x00af }
    L_0x00a4:
        r2 = "FieldClassificationStrategy";	 Catch:{ all -> 0x00af }
        r6 = "bound";	 Catch:{ all -> 0x00af }
        android.util.Slog.v(r2, r6);	 Catch:{ all -> 0x00af }
    L_0x00ab:
        android.os.Binder.restoreCallingIdentity(r4);	 Catch:{ all -> 0x00b6 }
        goto L_0x00b4;	 Catch:{ all -> 0x00b6 }
    L_0x00af:
        r2 = move-exception;	 Catch:{ all -> 0x00b6 }
        android.os.Binder.restoreCallingIdentity(r4);	 Catch:{ all -> 0x00b6 }
        throw r2;	 Catch:{ all -> 0x00b6 }
    L_0x00b4:
        monitor-exit(r0);	 Catch:{ all -> 0x00b6 }
        return;	 Catch:{ all -> 0x00b6 }
    L_0x00b6:
        r1 = move-exception;	 Catch:{ all -> 0x00b6 }
        monitor-exit(r0);	 Catch:{ all -> 0x00b6 }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.autofill.FieldClassificationStrategy.connectAndRun(com.android.server.autofill.FieldClassificationStrategy$Command):void");
    }

    String[] getAvailableAlgorithms() {
        return (String[]) getMetadataValue("android.autofill.field_classification.available_algorithms", -$$Lambda$FieldClassificationStrategy$NQQgQ63vxhPkiwOWrnwRyuYSHTM.INSTANCE);
    }

    String getDefaultAlgorithm() {
        return (String) getMetadataValue("android.autofill.field_classification.default_algorithm", -$$Lambda$FieldClassificationStrategy$vGIL1YGX_9ksoSV74T7gO4fkEBE.INSTANCE);
    }

    private <T> T getMetadataValue(String field, MetadataParser<T> parser) {
        ServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo == null) {
            return null;
        }
        try {
            return parser.get(this.mContext.getPackageManager().getResourcesForApplication(serviceInfo.applicationInfo), serviceInfo.metaData.getInt(field));
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error getting application resources for ");
            stringBuilder.append(serviceInfo);
            Log.e(str, stringBuilder.toString(), e);
            return null;
        }
    }

    void getScores(RemoteCallback callback, String algorithmName, Bundle algorithmArgs, List<AutofillValue> actualValues, String[] userDataValues) {
        connectAndRun(new -$$Lambda$FieldClassificationStrategy$gMvCLMmbA3vJ4CLoCwKkhYrCMsQ(callback, algorithmName, algorithmArgs, actualValues, userDataValues));
    }

    void dump(String prefix, PrintWriter pw) {
        ComponentName impl = getServiceComponentName();
        pw.print(prefix);
        pw.print("User ID: ");
        pw.println(this.mUserId);
        pw.print(prefix);
        pw.print("Queued commands: ");
        if (this.mQueuedCommands == null) {
            pw.println("N/A");
        } else {
            pw.println(this.mQueuedCommands.size());
        }
        pw.print(prefix);
        pw.print("Implementation: ");
        if (impl == null) {
            pw.println("N/A");
            return;
        }
        pw.println(impl.flattenToShortString());
        pw.print(prefix);
        pw.print("Available algorithms: ");
        pw.println(Arrays.toString(getAvailableAlgorithms()));
        pw.print(prefix);
        pw.print("Default algorithm: ");
        pw.println(getDefaultAlgorithm());
    }
}
