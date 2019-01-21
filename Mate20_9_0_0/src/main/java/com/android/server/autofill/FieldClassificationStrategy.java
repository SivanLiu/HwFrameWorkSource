package com.android.server.autofill;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.autofill.IAutofillFieldClassificationService;
import android.service.autofill.IAutofillFieldClassificationService.Stub;
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

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:5:0x0007, B:33:0x0093] */
    /* JADX WARNING: Missing block: B:10:0x0019, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:12:?, code skipped:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("exception calling service: ");
            r3.append(r1);
            android.util.Slog.w(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:40:0x00b0, code skipped:
            android.os.Binder.restoreCallingIdentity(r4);
     */
    /* JADX WARNING: Missing block: B:43:0x00b5, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void connectAndRun(Command command) {
        synchronized (this.mLock) {
            if (this.mRemoteService != null) {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "running command right away");
                }
                command.run(this.mRemoteService);
            } else {
                if (Helper.sDebug) {
                    Slog.d(TAG, "service is null; queuing command");
                }
                if (this.mQueuedCommands == null) {
                    this.mQueuedCommands = new ArrayList(1);
                }
                this.mQueuedCommands.add(command);
                if (this.mServiceConnection != null) {
                    return;
                }
                if (Helper.sVerbose) {
                    Slog.v(TAG, "creating connection");
                }
                this.mServiceConnection = new ServiceConnection() {
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        if (Helper.sVerbose) {
                            String str = FieldClassificationStrategy.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("onServiceConnected(): ");
                            stringBuilder.append(name);
                            Slog.v(str, stringBuilder.toString());
                        }
                        synchronized (FieldClassificationStrategy.this.mLock) {
                            FieldClassificationStrategy.this.mRemoteService = Stub.asInterface(service);
                            if (FieldClassificationStrategy.this.mQueuedCommands != null) {
                                int size = FieldClassificationStrategy.this.mQueuedCommands.size();
                                if (Helper.sDebug) {
                                    String str2 = FieldClassificationStrategy.TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("running ");
                                    stringBuilder2.append(size);
                                    stringBuilder2.append(" queued commands");
                                    Slog.d(str2, stringBuilder2.toString());
                                }
                                for (int i = 0; i < size; i++) {
                                    Command queuedCommand = (Command) FieldClassificationStrategy.this.mQueuedCommands.get(i);
                                    try {
                                        if (Helper.sVerbose) {
                                            String str3 = FieldClassificationStrategy.TAG;
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("running queued command #");
                                            stringBuilder3.append(i);
                                            Slog.v(str3, stringBuilder3.toString());
                                        }
                                        queuedCommand.run(FieldClassificationStrategy.this.mRemoteService);
                                    } catch (RemoteException e) {
                                        String str4 = FieldClassificationStrategy.TAG;
                                        StringBuilder stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("exception calling ");
                                        stringBuilder4.append(name);
                                        stringBuilder4.append(": ");
                                        stringBuilder4.append(e);
                                        Slog.w(str4, stringBuilder4.toString());
                                    }
                                }
                                FieldClassificationStrategy.this.mQueuedCommands = null;
                            } else if (Helper.sDebug) {
                                Slog.d(FieldClassificationStrategy.TAG, "no queued commands");
                            }
                        }
                    }

                    public void onServiceDisconnected(ComponentName name) {
                        if (Helper.sVerbose) {
                            String str = FieldClassificationStrategy.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("onServiceDisconnected(): ");
                            stringBuilder.append(name);
                            Slog.v(str, stringBuilder.toString());
                        }
                        synchronized (FieldClassificationStrategy.this.mLock) {
                            FieldClassificationStrategy.this.mRemoteService = null;
                        }
                    }

                    public void onBindingDied(ComponentName name) {
                        if (Helper.sVerbose) {
                            String str = FieldClassificationStrategy.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("onBindingDied(): ");
                            stringBuilder.append(name);
                            Slog.v(str, stringBuilder.toString());
                        }
                        synchronized (FieldClassificationStrategy.this.mLock) {
                            FieldClassificationStrategy.this.mRemoteService = null;
                        }
                    }

                    public void onNullBinding(ComponentName name) {
                        if (Helper.sVerbose) {
                            String str = FieldClassificationStrategy.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("onNullBinding(): ");
                            stringBuilder.append(name);
                            Slog.v(str, stringBuilder.toString());
                        }
                        synchronized (FieldClassificationStrategy.this.mLock) {
                            FieldClassificationStrategy.this.mRemoteService = null;
                        }
                    }
                };
                ComponentName component = getServiceComponentName();
                if (Helper.sVerbose) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("binding to: ");
                    stringBuilder.append(component);
                    Slog.v(str, stringBuilder.toString());
                }
                if (component != null) {
                    Intent intent = new Intent();
                    intent.setComponent(component);
                    long token = Binder.clearCallingIdentity();
                    this.mContext.bindServiceAsUser(intent, this.mServiceConnection, 1, UserHandle.of(this.mUserId));
                    if (Helper.sVerbose) {
                        Slog.v(TAG, "bound");
                    }
                    Binder.restoreCallingIdentity(token);
                }
            }
        }
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
