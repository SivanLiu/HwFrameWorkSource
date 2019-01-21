package android.app;

import android.app.servertransaction.ClientTransaction;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug.MemoryInfo;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.android.internal.app.IVoiceInteractor;
import java.util.List;
import java.util.Map;

public interface IApplicationThread extends IInterface {

    public static abstract class Stub extends Binder implements IApplicationThread {
        private static final String DESCRIPTOR = "android.app.IApplicationThread";
        static final int TRANSACTION_attachAgent = 49;
        static final int TRANSACTION_bindApplication = 4;
        static final int TRANSACTION_clearDnsCache = 26;
        static final int TRANSACTION_dispatchPackageBroadcast = 22;
        static final int TRANSACTION_dumpActivity = 25;
        static final int TRANSACTION_dumpDbInfo = 36;
        static final int TRANSACTION_dumpGfxInfo = 34;
        static final int TRANSACTION_dumpHeap = 24;
        static final int TRANSACTION_dumpMemInfo = 32;
        static final int TRANSACTION_dumpMemInfoProto = 33;
        static final int TRANSACTION_dumpProvider = 35;
        static final int TRANSACTION_dumpService = 12;
        static final int TRANSACTION_handleTrustStorageUpdate = 48;
        static final int TRANSACTION_iawareTrimMemory = 31;
        static final int TRANSACTION_notifyCleartextNetwork = 44;
        static final int TRANSACTION_processInBackground = 9;
        static final int TRANSACTION_profilerControl = 16;
        static final int TRANSACTION_requestAssistContextExtras = 38;
        static final int TRANSACTION_requestContentNode = 55;
        static final int TRANSACTION_requestContentOther = 56;
        static final int TRANSACTION_runIsolatedEntryPoint = 5;
        static final int TRANSACTION_scheduleApplicationInfoChanged = 50;
        static final int TRANSACTION_scheduleApplicationThemeInfoChanged = 51;
        static final int TRANSACTION_scheduleBindService = 10;
        static final int TRANSACTION_scheduleCrash = 23;
        static final int TRANSACTION_scheduleCreateBackupAgent = 18;
        static final int TRANSACTION_scheduleCreateService = 2;
        static final int TRANSACTION_scheduleDestroyBackupAgent = 19;
        static final int TRANSACTION_scheduleEnterAnimationComplete = 43;
        static final int TRANSACTION_scheduleExit = 6;
        static final int TRANSACTION_scheduleInstallProvider = 41;
        static final int TRANSACTION_scheduleLocalVoiceInteractionStarted = 47;
        static final int TRANSACTION_scheduleLowMemory = 14;
        static final int TRANSACTION_scheduleOnNewActivityOptions = 20;
        static final int TRANSACTION_schedulePCWindowStateChanged = 54;
        static final int TRANSACTION_scheduleReceiver = 1;
        static final int TRANSACTION_scheduleRegisteredReceiver = 13;
        static final int TRANSACTION_scheduleServiceArgs = 7;
        static final int TRANSACTION_scheduleSleeping = 15;
        static final int TRANSACTION_scheduleStopService = 3;
        static final int TRANSACTION_scheduleSuicide = 21;
        static final int TRANSACTION_scheduleTransaction = 53;
        static final int TRANSACTION_scheduleTranslucentConversionComplete = 39;
        static final int TRANSACTION_scheduleTrimMemory = 30;
        static final int TRANSACTION_scheduleUnbindService = 11;
        static final int TRANSACTION_setCoreSettings = 28;
        static final int TRANSACTION_setHttpProxy = 27;
        static final int TRANSACTION_setNetworkBlockSeq = 52;
        static final int TRANSACTION_setProcessState = 40;
        static final int TRANSACTION_setSchedulingGroup = 17;
        static final int TRANSACTION_startBinderTracking = 45;
        static final int TRANSACTION_stopBinderTrackingAndDump = 46;
        static final int TRANSACTION_unstableProviderDied = 37;
        static final int TRANSACTION_updatePackageCompatibilityInfo = 29;
        static final int TRANSACTION_updateTimePrefs = 42;
        static final int TRANSACTION_updateTimeZone = 8;

        private static class Proxy implements IApplicationThread {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public void scheduleReceiver(Intent intent, ActivityInfo info, CompatibilityInfo compatInfo, int resultCode, String data, Bundle extras, boolean sync, int sendingUser, int processState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (compatInfo != null) {
                        _data.writeInt(1);
                        compatInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(resultCode);
                    _data.writeString(data);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(sync);
                    _data.writeInt(sendingUser);
                    _data.writeInt(processState);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleCreateService(IBinder token, ServiceInfo info, CompatibilityInfo compatInfo, int processState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (compatInfo != null) {
                        _data.writeInt(1);
                        compatInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(processState);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleStopService(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void bindApplication(String packageName, ApplicationInfo info, List<ProviderInfo> providers, ComponentName testName, ProfilerInfo profilerInfo, Bundle testArguments, IInstrumentationWatcher testWatcher, IUiAutomationConnection uiAutomationConnection, int debugMode, boolean enableBinderTracking, boolean trackAllocation, boolean restrictedBackupMode, boolean persistent, Configuration config, CompatibilityInfo compatInfo, Map services, Bundle coreSettings, String buildSerial, boolean isAutofillCompatEnabled) throws RemoteException {
                Throwable th;
                int i;
                boolean z;
                boolean z2;
                List<ProviderInfo> list;
                ApplicationInfo applicationInfo = info;
                ComponentName componentName = testName;
                ProfilerInfo profilerInfo2 = profilerInfo;
                Bundle bundle = testArguments;
                Configuration configuration = config;
                CompatibilityInfo compatibilityInfo = compatInfo;
                Bundle bundle2 = coreSettings;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeString(packageName);
                        if (applicationInfo != null) {
                            _data.writeInt(1);
                            applicationInfo.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(0);
                        }
                        try {
                            _data.writeTypedList(providers);
                            if (componentName != null) {
                                _data.writeInt(1);
                                componentName.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            if (profilerInfo2 != null) {
                                _data.writeInt(1);
                                profilerInfo2.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            if (bundle != null) {
                                _data.writeInt(1);
                                bundle.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            _data.writeStrongBinder(testWatcher != null ? testWatcher.asBinder() : null);
                            _data.writeStrongBinder(uiAutomationConnection != null ? uiAutomationConnection.asBinder() : null);
                        } catch (Throwable th2) {
                            th = th2;
                            i = debugMode;
                            z = enableBinderTracking;
                            z2 = trackAllocation;
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeInt(debugMode);
                        } catch (Throwable th3) {
                            th = th3;
                            z = enableBinderTracking;
                            z2 = trackAllocation;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        list = providers;
                        i = debugMode;
                        z = enableBinderTracking;
                        z2 = trackAllocation;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(enableBinderTracking);
                        try {
                            int i2;
                            _data.writeInt(trackAllocation);
                            _data.writeInt(restrictedBackupMode);
                            _data.writeInt(persistent);
                            if (configuration != null) {
                                _data.writeInt(1);
                                i2 = 0;
                                configuration.writeToParcel(_data, 0);
                            } else {
                                i2 = 0;
                                _data.writeInt(0);
                            }
                            if (compatibilityInfo != null) {
                                _data.writeInt(1);
                                compatibilityInfo.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(i2);
                            }
                            _data.writeMap(services);
                            if (bundle2 != null) {
                                _data.writeInt(1);
                                bundle2.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            _data.writeString(buildSerial);
                            _data.writeInt(isAutofillCompatEnabled);
                            this.mRemote.transact(4, _data, null, 1);
                            _data.recycle();
                        } catch (Throwable th5) {
                            th = th5;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        z2 = trackAllocation;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    String str = packageName;
                    list = providers;
                    i = debugMode;
                    z = enableBinderTracking;
                    z2 = trackAllocation;
                    _data.recycle();
                    throw th;
                }
            }

            public void runIsolatedEntryPoint(String entryPoint, String[] entryPointArgs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(entryPoint);
                    _data.writeStringArray(entryPointArgs);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleExit() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleServiceArgs(IBinder token, ParceledListSlice args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void updateTimeZone() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void processInBackground() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleBindService(IBinder token, Intent intent, boolean rebind, int processState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(rebind);
                    _data.writeInt(processState);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleUnbindService(IBinder token, Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dumpService(ParcelFileDescriptor fd, IBinder servicetoken, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(servicetoken);
                    _data.writeStringArray(args);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleRegisteredReceiver(IIntentReceiver receiver, Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser, int processState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(receiver != null ? receiver.asBinder() : null);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(resultCode);
                    _data.writeString(data);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(ordered);
                    _data.writeInt(sticky);
                    _data.writeInt(sendingUser);
                    _data.writeInt(processState);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleLowMemory() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleSleeping(IBinder token, boolean sleeping) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(sleeping);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void profilerControl(boolean start, ProfilerInfo profilerInfo, int profileType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(start);
                    if (profilerInfo != null) {
                        _data.writeInt(1);
                        profilerInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(profileType);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setSchedulingGroup(int group) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(group);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleCreateBackupAgent(ApplicationInfo app, CompatibilityInfo compatInfo, int backupMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (app != null) {
                        _data.writeInt(1);
                        app.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (compatInfo != null) {
                        _data.writeInt(1);
                        compatInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(backupMode);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleDestroyBackupAgent(ApplicationInfo app, CompatibilityInfo compatInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (app != null) {
                        _data.writeInt(1);
                        app.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (compatInfo != null) {
                        _data.writeInt(1);
                        compatInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleOnNewActivityOptions(IBinder token, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (options != null) {
                        _data.writeInt(1);
                        options.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleSuicide() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dispatchPackageBroadcast(int cmd, String[] packages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cmd);
                    _data.writeStringArray(packages);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleCrash(String msg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msg);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dumpHeap(boolean managed, boolean mallocInfo, boolean runGc, String path, ParcelFileDescriptor fd) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(managed);
                    _data.writeInt(mallocInfo);
                    _data.writeInt(runGc);
                    _data.writeString(path);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dumpActivity(ParcelFileDescriptor fd, IBinder servicetoken, String prefix, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(servicetoken);
                    _data.writeString(prefix);
                    _data.writeStringArray(args);
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void clearDnsCache() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setHttpProxy(String proxy, String port, String exclList, Uri pacFileUrl) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(proxy);
                    _data.writeString(port);
                    _data.writeString(exclList);
                    if (pacFileUrl != null) {
                        _data.writeInt(1);
                        pacFileUrl.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setCoreSettings(Bundle coreSettings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (coreSettings != null) {
                        _data.writeInt(1);
                        coreSettings.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void updatePackageCompatibilityInfo(String pkg, CompatibilityInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleTrimMemory(int level) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(level);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void iawareTrimMemory(int level, boolean fromIAware) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(level);
                    _data.writeInt(fromIAware);
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dumpMemInfo(ParcelFileDescriptor fd, MemoryInfo mem, boolean checkin, boolean dumpInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (mem != null) {
                        _data.writeInt(1);
                        mem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(checkin);
                    _data.writeInt(dumpInfo);
                    _data.writeInt(dumpDalvik);
                    _data.writeInt(dumpSummaryOnly);
                    _data.writeInt(dumpUnreachable);
                    _data.writeStringArray(args);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dumpMemInfoProto(ParcelFileDescriptor fd, MemoryInfo mem, boolean dumpInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (mem != null) {
                        _data.writeInt(1);
                        mem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(dumpInfo);
                    _data.writeInt(dumpDalvik);
                    _data.writeInt(dumpSummaryOnly);
                    _data.writeInt(dumpUnreachable);
                    _data.writeStringArray(args);
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dumpGfxInfo(ParcelFileDescriptor fd, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringArray(args);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dumpProvider(ParcelFileDescriptor fd, IBinder servicetoken, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(servicetoken);
                    _data.writeStringArray(args);
                    this.mRemote.transact(35, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dumpDbInfo(ParcelFileDescriptor fd, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringArray(args);
                    this.mRemote.transact(36, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void unstableProviderDied(IBinder provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(provider);
                    this.mRemote.transact(37, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void requestAssistContextExtras(IBinder activityToken, IBinder requestToken, int requestType, int sessionId, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(activityToken);
                    _data.writeStrongBinder(requestToken);
                    _data.writeInt(requestType);
                    _data.writeInt(sessionId);
                    _data.writeInt(flags);
                    this.mRemote.transact(38, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleTranslucentConversionComplete(IBinder token, boolean timeout) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(timeout);
                    this.mRemote.transact(39, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setProcessState(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(40, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleInstallProvider(ProviderInfo provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (provider != null) {
                        _data.writeInt(1);
                        provider.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(41, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void updateTimePrefs(int timeFormatPreference) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(timeFormatPreference);
                    this.mRemote.transact(42, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleEnterAnimationComplete(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(43, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void notifyCleartextNetwork(byte[] firstPacket) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(firstPacket);
                    this.mRemote.transact(44, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void startBinderTracking() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(45, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void stopBinderTrackingAndDump(ParcelFileDescriptor fd) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(46, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleLocalVoiceInteractionStarted(IBinder token, IVoiceInteractor voiceInteractor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeStrongBinder(voiceInteractor != null ? voiceInteractor.asBinder() : null);
                    this.mRemote.transact(47, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void handleTrustStorageUpdate() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(48, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void attachAgent(String path) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(path);
                    this.mRemote.transact(49, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleApplicationInfoChanged(ApplicationInfo ai) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ai != null) {
                        _data.writeInt(1);
                        ai.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(50, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleApplicationThemeInfoChanged(ApplicationInfo ai, boolean fromThemeChange) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ai != null) {
                        _data.writeInt(1);
                        ai.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(fromThemeChange);
                    this.mRemote.transact(51, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setNetworkBlockSeq(long procStateSeq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(procStateSeq);
                    this.mRemote.transact(52, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleTransaction(ClientTransaction transaction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (transaction != null) {
                        _data.writeInt(1);
                        transaction.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(53, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void schedulePCWindowStateChanged(IBinder token, int windowState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(windowState);
                    this.mRemote.transact(54, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void requestContentNode(IBinder appToken, Bundle data, int token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(appToken);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(token);
                    this.mRemote.transact(55, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void requestContentOther(IBinder appToken, Bundle data, int token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(appToken);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(token);
                    this.mRemote.transact(56, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IApplicationThread asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IApplicationThread)) {
                return new Proxy(obj);
            }
            return (IApplicationThread) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            String descriptor = DESCRIPTOR;
            String descriptor2;
            Parcel parcel2;
            if (i != 1598968902) {
                boolean _arg1 = false;
                CompatibilityInfo _arg12 = null;
                Bundle _arg5;
                IBinder _arg0;
                ParceledListSlice _arg13;
                IBinder _arg02;
                Intent _arg14;
                Intent _arg15;
                ParcelFileDescriptor _arg03;
                ApplicationInfo _arg04;
                Bundle _arg16;
                String _arg05;
                ParcelFileDescriptor _arg06;
                MemoryInfo _arg17;
                ApplicationInfo _arg07;
                switch (i) {
                    case 1:
                        Intent _arg08;
                        ActivityInfo _arg18;
                        CompatibilityInfo _arg2;
                        descriptor2 = descriptor;
                        parcel2 = parcel;
                        parcel2.enforceInterface(descriptor2);
                        if (data.readInt() != 0) {
                            _arg08 = (Intent) Intent.CREATOR.createFromParcel(parcel2);
                        } else {
                            _arg08 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg18 = (ActivityInfo) ActivityInfo.CREATOR.createFromParcel(parcel2);
                        } else {
                            _arg18 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg2 = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(parcel2);
                        } else {
                            _arg2 = null;
                        }
                        int _arg3 = data.readInt();
                        String _arg4 = data.readString();
                        if (data.readInt() != 0) {
                            _arg5 = (Bundle) Bundle.CREATOR.createFromParcel(parcel2);
                        } else {
                            _arg5 = null;
                        }
                        scheduleReceiver(_arg08, _arg18, _arg2, _arg3, _arg4, _arg5, data.readInt() != 0, data.readInt(), data.readInt());
                        return true;
                    case 2:
                        ServiceInfo _arg19;
                        descriptor2 = descriptor;
                        parcel2 = parcel;
                        parcel2.enforceInterface(descriptor2);
                        _arg0 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg19 = (ServiceInfo) ServiceInfo.CREATOR.createFromParcel(parcel2);
                        } else {
                            _arg19 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg12 = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(parcel2);
                        }
                        scheduleCreateService(_arg0, _arg19, _arg12, data.readInt());
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        scheduleStopService(data.readStrongBinder());
                        return true;
                    case 4:
                        ApplicationInfo _arg110;
                        ComponentName _arg32;
                        ProfilerInfo _arg42;
                        Configuration _arg132;
                        CompatibilityInfo _arg142;
                        parcel.enforceInterface(descriptor);
                        String _arg09 = data.readString();
                        if (data.readInt() != 0) {
                            _arg110 = (ApplicationInfo) ApplicationInfo.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg110 = null;
                        }
                        List<ProviderInfo> _arg22 = parcel.createTypedArrayList(ProviderInfo.CREATOR);
                        if (data.readInt() != 0) {
                            _arg32 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg32 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg42 = (ProfilerInfo) ProfilerInfo.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg42 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg5 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg5 = null;
                        }
                        IInstrumentationWatcher _arg6 = android.app.IInstrumentationWatcher.Stub.asInterface(data.readStrongBinder());
                        IUiAutomationConnection _arg7 = android.app.IUiAutomationConnection.Stub.asInterface(data.readStrongBinder());
                        int _arg8 = data.readInt();
                        boolean _arg9 = data.readInt() != 0;
                        boolean _arg10 = data.readInt() != 0;
                        String descriptor3 = descriptor;
                        descriptor = data.readInt() != 0;
                        Parcel parcel3 = parcel;
                        boolean _arg122 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg132 = (Configuration) Configuration.CREATOR.createFromParcel(parcel3);
                        } else {
                            _arg132 = null;
                        }
                        Configuration _arg133 = _arg132;
                        if (data.readInt() != 0) {
                            _arg142 = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(parcel3);
                        } else {
                            _arg142 = null;
                        }
                        CompatibilityInfo _arg143 = _arg142;
                        ClassLoader cl = getClass().getClassLoader();
                        Map _arg152 = parcel3.readHashMap(cl);
                        if (data.readInt() != 0) {
                            _arg13 = (Bundle) Bundle.CREATOR.createFromParcel(parcel3);
                        }
                        ParceledListSlice _arg162 = _arg13;
                        bindApplication(_arg09, _arg110, _arg22, _arg32, _arg42, _arg5, _arg6, _arg7, _arg8, _arg9, _arg10, descriptor, _arg122, _arg133, _arg143, _arg152, _arg162, data.readString(), data.readInt() != 0);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        runIsolatedEntryPoint(data.readString(), data.createStringArray());
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        scheduleExit();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg13 = (ParceledListSlice) ParceledListSlice.CREATOR.createFromParcel(parcel);
                        }
                        scheduleServiceArgs(_arg0, _arg13);
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        updateTimeZone();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        processInBackground();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg14 = (Intent) Intent.CREATOR.createFromParcel(parcel);
                        }
                        _arg15 = _arg14;
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        scheduleBindService(_arg02, _arg15, _arg1, data.readInt());
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg14 = (Intent) Intent.CREATOR.createFromParcel(parcel);
                        }
                        scheduleUnbindService(_arg0, _arg14);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        dumpService(_arg03, data.readStrongBinder(), data.createStringArray());
                        return true;
                    case 13:
                        Bundle _arg43;
                        parcel.enforceInterface(descriptor);
                        IIntentReceiver _arg010 = android.content.IIntentReceiver.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg15 = (Intent) Intent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg15 = null;
                        }
                        int _arg23 = data.readInt();
                        String _arg33 = data.readString();
                        if (data.readInt() != 0) {
                            _arg43 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg43 = null;
                        }
                        scheduleRegisteredReceiver(_arg010, _arg15, _arg23, _arg33, _arg43, data.readInt() != 0, data.readInt() != 0, data.readInt(), data.readInt());
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        scheduleLowMemory();
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        scheduleSleeping(_arg02, _arg1);
                        return true;
                    case 16:
                        ProfilerInfo _arg111;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        if (data.readInt() != 0) {
                            _arg111 = (ProfilerInfo) ProfilerInfo.CREATOR.createFromParcel(parcel);
                        }
                        profilerControl(_arg1, _arg111, data.readInt());
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        setSchedulingGroup(data.readInt());
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (ApplicationInfo) ApplicationInfo.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg04 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg12 = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(parcel);
                        }
                        scheduleCreateBackupAgent(_arg04, _arg12, data.readInt());
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (ApplicationInfo) ApplicationInfo.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg04 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg12 = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(parcel);
                        }
                        scheduleDestroyBackupAgent(_arg04, _arg12);
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg16 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        scheduleOnNewActivityOptions(_arg0, _arg16);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        scheduleSuicide();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        dispatchPackageBroadcast(data.readInt(), data.createStringArray());
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        scheduleCrash(data.readString());
                        return true;
                    case 24:
                        ParcelFileDescriptor _arg44;
                        parcel.enforceInterface(descriptor);
                        boolean _arg011 = data.readInt() != 0;
                        boolean _arg112 = data.readInt() != 0;
                        boolean _arg24 = data.readInt() != 0;
                        String _arg34 = data.readString();
                        if (data.readInt() != 0) {
                            _arg44 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg44 = null;
                        }
                        dumpHeap(_arg011, _arg112, _arg24, _arg34, _arg44);
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        dumpActivity(_arg03, data.readStrongBinder(), data.readString(), data.createStringArray());
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        clearDnsCache();
                        return true;
                    case 27:
                        Uri _arg35;
                        parcel.enforceInterface(descriptor);
                        _arg05 = data.readString();
                        String _arg113 = data.readString();
                        String _arg25 = data.readString();
                        if (data.readInt() != 0) {
                            _arg35 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        }
                        setHttpProxy(_arg05, _arg113, _arg25, _arg35);
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg16 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        setCoreSettings(_arg16);
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        _arg05 = data.readString();
                        if (data.readInt() != 0) {
                            _arg12 = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(parcel);
                        }
                        updatePackageCompatibilityInfo(_arg05, _arg12);
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        scheduleTrimMemory(data.readInt());
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        int _arg012 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        iawareTrimMemory(_arg012, _arg1);
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg06 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg06 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg17 = (MemoryInfo) MemoryInfo.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg17 = null;
                        }
                        dumpMemInfo(_arg06, _arg17, data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.createStringArray());
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg06 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg06 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg17 = (MemoryInfo) MemoryInfo.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg17 = null;
                        }
                        dumpMemInfoProto(_arg06, _arg17, data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.createStringArray());
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        dumpGfxInfo(_arg03, data.createStringArray());
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        dumpProvider(_arg03, data.readStrongBinder(), data.createStringArray());
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        dumpDbInfo(_arg03, data.createStringArray());
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        unstableProviderDied(data.readStrongBinder());
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        requestAssistContextExtras(data.readStrongBinder(), data.readStrongBinder(), data.readInt(), data.readInt(), data.readInt());
                        return true;
                    case 39:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        scheduleTranslucentConversionComplete(_arg02, _arg1);
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        setProcessState(data.readInt());
                        return true;
                    case 41:
                        ProviderInfo _arg013;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg013 = (ProviderInfo) ProviderInfo.CREATOR.createFromParcel(parcel);
                        }
                        scheduleInstallProvider(_arg013);
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        updateTimePrefs(data.readInt());
                        return true;
                    case 43:
                        parcel.enforceInterface(descriptor);
                        scheduleEnterAnimationComplete(data.readStrongBinder());
                        return true;
                    case 44:
                        parcel.enforceInterface(descriptor);
                        notifyCleartextNetwork(data.createByteArray());
                        return true;
                    case 45:
                        parcel.enforceInterface(descriptor);
                        startBinderTracking();
                        return true;
                    case 46:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        }
                        stopBinderTrackingAndDump(_arg03);
                        return true;
                    case 47:
                        parcel.enforceInterface(descriptor);
                        scheduleLocalVoiceInteractionStarted(data.readStrongBinder(), com.android.internal.app.IVoiceInteractor.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 48:
                        parcel.enforceInterface(descriptor);
                        handleTrustStorageUpdate();
                        return true;
                    case 49:
                        parcel.enforceInterface(descriptor);
                        attachAgent(data.readString());
                        return true;
                    case 50:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg07 = (ApplicationInfo) ApplicationInfo.CREATOR.createFromParcel(parcel);
                        }
                        scheduleApplicationInfoChanged(_arg07);
                        return true;
                    case 51:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg07 = (ApplicationInfo) ApplicationInfo.CREATOR.createFromParcel(parcel);
                        }
                        ApplicationInfo _arg014 = _arg07;
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        scheduleApplicationThemeInfoChanged(_arg014, _arg1);
                        return true;
                    case 52:
                        parcel.enforceInterface(descriptor);
                        setNetworkBlockSeq(data.readLong());
                        return true;
                    case 53:
                        ClientTransaction _arg015;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg015 = (ClientTransaction) ClientTransaction.CREATOR.createFromParcel(parcel);
                        }
                        scheduleTransaction(_arg015);
                        return true;
                    case 54:
                        parcel.enforceInterface(descriptor);
                        schedulePCWindowStateChanged(data.readStrongBinder(), data.readInt());
                        return true;
                    case 55:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg16 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        requestContentNode(_arg0, _arg16, data.readInt());
                        return true;
                    case 56:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg16 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        requestContentOther(_arg0, _arg16, data.readInt());
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            descriptor2 = descriptor;
            parcel2 = parcel;
            reply.writeString(descriptor2);
            return true;
        }
    }

    void attachAgent(String str) throws RemoteException;

    void bindApplication(String str, ApplicationInfo applicationInfo, List<ProviderInfo> list, ComponentName componentName, ProfilerInfo profilerInfo, Bundle bundle, IInstrumentationWatcher iInstrumentationWatcher, IUiAutomationConnection iUiAutomationConnection, int i, boolean z, boolean z2, boolean z3, boolean z4, Configuration configuration, CompatibilityInfo compatibilityInfo, Map map, Bundle bundle2, String str2, boolean z5) throws RemoteException;

    void clearDnsCache() throws RemoteException;

    void dispatchPackageBroadcast(int i, String[] strArr) throws RemoteException;

    void dumpActivity(ParcelFileDescriptor parcelFileDescriptor, IBinder iBinder, String str, String[] strArr) throws RemoteException;

    void dumpDbInfo(ParcelFileDescriptor parcelFileDescriptor, String[] strArr) throws RemoteException;

    void dumpGfxInfo(ParcelFileDescriptor parcelFileDescriptor, String[] strArr) throws RemoteException;

    void dumpHeap(boolean z, boolean z2, boolean z3, String str, ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void dumpMemInfo(ParcelFileDescriptor parcelFileDescriptor, MemoryInfo memoryInfo, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, String[] strArr) throws RemoteException;

    void dumpMemInfoProto(ParcelFileDescriptor parcelFileDescriptor, MemoryInfo memoryInfo, boolean z, boolean z2, boolean z3, boolean z4, String[] strArr) throws RemoteException;

    void dumpProvider(ParcelFileDescriptor parcelFileDescriptor, IBinder iBinder, String[] strArr) throws RemoteException;

    void dumpService(ParcelFileDescriptor parcelFileDescriptor, IBinder iBinder, String[] strArr) throws RemoteException;

    void handleTrustStorageUpdate() throws RemoteException;

    void iawareTrimMemory(int i, boolean z) throws RemoteException;

    void notifyCleartextNetwork(byte[] bArr) throws RemoteException;

    void processInBackground() throws RemoteException;

    void profilerControl(boolean z, ProfilerInfo profilerInfo, int i) throws RemoteException;

    void requestAssistContextExtras(IBinder iBinder, IBinder iBinder2, int i, int i2, int i3) throws RemoteException;

    void requestContentNode(IBinder iBinder, Bundle bundle, int i) throws RemoteException;

    void requestContentOther(IBinder iBinder, Bundle bundle, int i) throws RemoteException;

    void runIsolatedEntryPoint(String str, String[] strArr) throws RemoteException;

    void scheduleApplicationInfoChanged(ApplicationInfo applicationInfo) throws RemoteException;

    void scheduleApplicationThemeInfoChanged(ApplicationInfo applicationInfo, boolean z) throws RemoteException;

    void scheduleBindService(IBinder iBinder, Intent intent, boolean z, int i) throws RemoteException;

    void scheduleCrash(String str) throws RemoteException;

    void scheduleCreateBackupAgent(ApplicationInfo applicationInfo, CompatibilityInfo compatibilityInfo, int i) throws RemoteException;

    void scheduleCreateService(IBinder iBinder, ServiceInfo serviceInfo, CompatibilityInfo compatibilityInfo, int i) throws RemoteException;

    void scheduleDestroyBackupAgent(ApplicationInfo applicationInfo, CompatibilityInfo compatibilityInfo) throws RemoteException;

    void scheduleEnterAnimationComplete(IBinder iBinder) throws RemoteException;

    void scheduleExit() throws RemoteException;

    void scheduleInstallProvider(ProviderInfo providerInfo) throws RemoteException;

    void scheduleLocalVoiceInteractionStarted(IBinder iBinder, IVoiceInteractor iVoiceInteractor) throws RemoteException;

    void scheduleLowMemory() throws RemoteException;

    void scheduleOnNewActivityOptions(IBinder iBinder, Bundle bundle) throws RemoteException;

    void schedulePCWindowStateChanged(IBinder iBinder, int i) throws RemoteException;

    void scheduleReceiver(Intent intent, ActivityInfo activityInfo, CompatibilityInfo compatibilityInfo, int i, String str, Bundle bundle, boolean z, int i2, int i3) throws RemoteException;

    void scheduleRegisteredReceiver(IIntentReceiver iIntentReceiver, Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2, int i3) throws RemoteException;

    void scheduleServiceArgs(IBinder iBinder, ParceledListSlice parceledListSlice) throws RemoteException;

    void scheduleSleeping(IBinder iBinder, boolean z) throws RemoteException;

    void scheduleStopService(IBinder iBinder) throws RemoteException;

    void scheduleSuicide() throws RemoteException;

    void scheduleTransaction(ClientTransaction clientTransaction) throws RemoteException;

    void scheduleTranslucentConversionComplete(IBinder iBinder, boolean z) throws RemoteException;

    void scheduleTrimMemory(int i) throws RemoteException;

    void scheduleUnbindService(IBinder iBinder, Intent intent) throws RemoteException;

    void setCoreSettings(Bundle bundle) throws RemoteException;

    void setHttpProxy(String str, String str2, String str3, Uri uri) throws RemoteException;

    void setNetworkBlockSeq(long j) throws RemoteException;

    void setProcessState(int i) throws RemoteException;

    void setSchedulingGroup(int i) throws RemoteException;

    void startBinderTracking() throws RemoteException;

    void stopBinderTrackingAndDump(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void unstableProviderDied(IBinder iBinder) throws RemoteException;

    void updatePackageCompatibilityInfo(String str, CompatibilityInfo compatibilityInfo) throws RemoteException;

    void updateTimePrefs(int i) throws RemoteException;

    void updateTimeZone() throws RemoteException;
}
