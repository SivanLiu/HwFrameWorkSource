package android.app;

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
import android.os.PersistableBundle;
import android.os.RemoteException;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.content.ReferrerIntent;
import java.util.List;
import java.util.Map;

public interface IApplicationThread extends IInterface {

    public static abstract class Stub extends Binder implements IApplicationThread {
        private static final String DESCRIPTOR = "android.app.IApplicationThread";
        static final int TRANSACTION_attachAgent = 61;
        static final int TRANSACTION_bindApplication = 12;
        static final int TRANSACTION_clearDnsCache = 37;
        static final int TRANSACTION_dispatchPackageBroadcast = 33;
        static final int TRANSACTION_dumpActivity = 36;
        static final int TRANSACTION_dumpDbInfo = 46;
        static final int TRANSACTION_dumpGfxInfo = 44;
        static final int TRANSACTION_dumpHeap = 35;
        static final int TRANSACTION_dumpMemInfo = 43;
        static final int TRANSACTION_dumpProvider = 45;
        static final int TRANSACTION_dumpService = 20;
        static final int TRANSACTION_handleTrustStorageUpdate = 60;
        static final int TRANSACTION_iawareTrimMemory = 42;
        static final int TRANSACTION_notifyCleartextNetwork = 54;
        static final int TRANSACTION_processInBackground = 17;
        static final int TRANSACTION_profilerControl = 27;
        static final int TRANSACTION_requestAssistContextExtras = 48;
        static final int TRANSACTION_requestContentNode = 65;
        static final int TRANSACTION_requestContentOther = 66;
        static final int TRANSACTION_scheduleActivityConfigurationChanged = 23;
        static final int TRANSACTION_scheduleActivityMovedToDisplay = 24;
        static final int TRANSACTION_scheduleApplicationInfoChanged = 62;
        static final int TRANSACTION_scheduleBindService = 18;
        static final int TRANSACTION_scheduleConfigurationChanged = 14;
        static final int TRANSACTION_scheduleCrash = 34;
        static final int TRANSACTION_scheduleCreateBackupAgent = 29;
        static final int TRANSACTION_scheduleCreateService = 10;
        static final int TRANSACTION_scheduleDestroyActivity = 8;
        static final int TRANSACTION_scheduleDestroyBackupAgent = 30;
        static final int TRANSACTION_scheduleEnterAnimationComplete = 53;
        static final int TRANSACTION_scheduleExit = 13;
        static final int TRANSACTION_scheduleInstallProvider = 51;
        static final int TRANSACTION_scheduleLaunchActivity = 6;
        static final int TRANSACTION_scheduleLocalVoiceInteractionStarted = 59;
        static final int TRANSACTION_scheduleLowMemory = 22;
        static final int TRANSACTION_scheduleMultiWindowModeChanged = 57;
        static final int TRANSACTION_scheduleNewIntent = 7;
        static final int TRANSACTION_scheduleOnNewActivityOptions = 31;
        static final int TRANSACTION_schedulePCWindowStateChanged = 64;
        static final int TRANSACTION_schedulePauseActivity = 1;
        static final int TRANSACTION_schedulePictureInPictureModeChanged = 58;
        static final int TRANSACTION_scheduleReceiver = 9;
        static final int TRANSACTION_scheduleRegisteredReceiver = 21;
        static final int TRANSACTION_scheduleRelaunchActivity = 25;
        static final int TRANSACTION_scheduleResumeActivity = 4;
        static final int TRANSACTION_scheduleSendResult = 5;
        static final int TRANSACTION_scheduleServiceArgs = 15;
        static final int TRANSACTION_scheduleSleeping = 26;
        static final int TRANSACTION_scheduleStopActivity = 2;
        static final int TRANSACTION_scheduleStopService = 11;
        static final int TRANSACTION_scheduleSuicide = 32;
        static final int TRANSACTION_scheduleTranslucentConversionComplete = 49;
        static final int TRANSACTION_scheduleTrimMemory = 41;
        static final int TRANSACTION_scheduleUnbindService = 19;
        static final int TRANSACTION_scheduleWindowVisibility = 3;
        static final int TRANSACTION_setCoreSettings = 39;
        static final int TRANSACTION_setHttpProxy = 38;
        static final int TRANSACTION_setNetworkBlockSeq = 63;
        static final int TRANSACTION_setProcessState = 50;
        static final int TRANSACTION_setSchedulingGroup = 28;
        static final int TRANSACTION_startBinderTracking = 55;
        static final int TRANSACTION_stopBinderTrackingAndDump = 56;
        static final int TRANSACTION_unstableProviderDied = 47;
        static final int TRANSACTION_updatePackageCompatibilityInfo = 40;
        static final int TRANSACTION_updateTimePrefs = 52;
        static final int TRANSACTION_updateTimeZone = 16;

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

            public void schedulePauseActivity(IBinder token, boolean finished, boolean userLeaving, int configChanges, boolean dontReport) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    int i2;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(finished ? 1 : 0);
                    if (userLeaving) {
                        i2 = 1;
                    } else {
                        i2 = 0;
                    }
                    _data.writeInt(i2);
                    _data.writeInt(configChanges);
                    if (!dontReport) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleStopActivity(IBinder token, boolean showWindow, int configChanges) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (!showWindow) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    _data.writeInt(configChanges);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleWindowVisibility(IBinder token, boolean showWindow) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (!showWindow) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleResumeActivity(IBinder token, int procState, boolean isForward, Bundle resumeArgs) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(procState);
                    if (!isForward) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    if (resumeArgs != null) {
                        _data.writeInt(1);
                        resumeArgs.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleSendResult(IBinder token, List<ResultInfo> results) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedList(results);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleLaunchActivity(Intent intent, IBinder token, int ident, ActivityInfo info, Configuration curConfig, Configuration overrideConfig, CompatibilityInfo compatInfo, String referrer, IVoiceInteractor voiceInteractor, int procState, Bundle state, PersistableBundle persistentState, List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents, boolean notResumed, boolean isForward, ProfilerInfo profilerInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(token);
                    _data.writeInt(ident);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (curConfig != null) {
                        _data.writeInt(1);
                        curConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (overrideConfig != null) {
                        _data.writeInt(1);
                        overrideConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (compatInfo != null) {
                        _data.writeInt(1);
                        compatInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(referrer);
                    _data.writeStrongBinder(voiceInteractor != null ? voiceInteractor.asBinder() : null);
                    _data.writeInt(procState);
                    if (state != null) {
                        _data.writeInt(1);
                        state.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (persistentState != null) {
                        _data.writeInt(1);
                        persistentState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeTypedList(pendingResults);
                    _data.writeTypedList(pendingNewIntents);
                    _data.writeInt(notResumed ? 1 : 0);
                    _data.writeInt(isForward ? 1 : 0);
                    if (profilerInfo != null) {
                        _data.writeInt(1);
                        profilerInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleNewIntent(List<ReferrerIntent> intent, IBinder token, boolean andPause) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(intent);
                    _data.writeStrongBinder(token);
                    if (!andPause) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleDestroyActivity(IBinder token, boolean finished, int configChanges) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (!finished) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    _data.writeInt(configChanges);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleReceiver(Intent intent, ActivityInfo info, CompatibilityInfo compatInfo, int resultCode, String data, Bundle extras, boolean sync, int sendingUser, int processState) throws RemoteException {
                int i = 1;
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
                    if (!sync) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    _data.writeInt(sendingUser);
                    _data.writeInt(processState);
                    this.mRemote.transact(9, _data, null, 1);
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
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleStopService(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void bindApplication(String packageName, ApplicationInfo info, List<ProviderInfo> providers, ComponentName testName, ProfilerInfo profilerInfo, Bundle testArguments, IInstrumentationWatcher testWatcher, IUiAutomationConnection uiAutomationConnection, int debugMode, boolean enableBinderTracking, boolean trackAllocation, boolean restrictedBackupMode, boolean persistent, Configuration config, CompatibilityInfo compatInfo, Map services, Bundle coreSettings, String buildSerial) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeTypedList(providers);
                    if (testName != null) {
                        _data.writeInt(1);
                        testName.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (profilerInfo != null) {
                        _data.writeInt(1);
                        profilerInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (testArguments != null) {
                        _data.writeInt(1);
                        testArguments.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(testWatcher != null ? testWatcher.asBinder() : null);
                    _data.writeStrongBinder(uiAutomationConnection != null ? uiAutomationConnection.asBinder() : null);
                    _data.writeInt(debugMode);
                    _data.writeInt(enableBinderTracking ? 1 : 0);
                    _data.writeInt(trackAllocation ? 1 : 0);
                    _data.writeInt(restrictedBackupMode ? 1 : 0);
                    _data.writeInt(persistent ? 1 : 0);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (compatInfo != null) {
                        _data.writeInt(1);
                        compatInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeMap(services);
                    if (coreSettings != null) {
                        _data.writeInt(1);
                        coreSettings.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(buildSerial);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleExit() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleConfigurationChanged(Configuration config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, null, 1);
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
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void updateTimeZone() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void processInBackground() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleBindService(IBinder token, Intent intent, boolean rebind, int processState) throws RemoteException {
                int i = 1;
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
                    if (!rebind) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    _data.writeInt(processState);
                    this.mRemote.transact(18, _data, null, 1);
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
                    this.mRemote.transact(19, _data, null, 1);
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
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleRegisteredReceiver(IIntentReceiver receiver, Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser, int processState) throws RemoteException {
                IBinder iBinder = null;
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    int i2;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (receiver != null) {
                        iBinder = receiver.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
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
                    if (ordered) {
                        i2 = 1;
                    } else {
                        i2 = 0;
                    }
                    _data.writeInt(i2);
                    if (!sticky) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    _data.writeInt(sendingUser);
                    _data.writeInt(processState);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleLowMemory() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleActivityConfigurationChanged(IBinder token, Configuration overrideConfig) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (overrideConfig != null) {
                        _data.writeInt(1);
                        overrideConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleActivityMovedToDisplay(IBinder token, int displayId, Configuration overrideConfig) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(displayId);
                    if (overrideConfig != null) {
                        _data.writeInt(1);
                        overrideConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleRelaunchActivity(IBinder token, List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents, int configChanges, boolean notResumed, Configuration config, Configuration overrideConfig, boolean preserveWindow) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedList(pendingResults);
                    _data.writeTypedList(pendingNewIntents);
                    _data.writeInt(configChanges);
                    _data.writeInt(notResumed ? 1 : 0);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (overrideConfig != null) {
                        _data.writeInt(1);
                        overrideConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (!preserveWindow) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleSleeping(IBinder token, boolean sleeping) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (!sleeping) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void profilerControl(boolean start, ProfilerInfo profilerInfo, int profileType) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!start) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    if (profilerInfo != null) {
                        _data.writeInt(1);
                        profilerInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(profileType);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setSchedulingGroup(int group) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(group);
                    this.mRemote.transact(28, _data, null, 1);
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
                    this.mRemote.transact(29, _data, null, 1);
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
                    this.mRemote.transact(30, _data, null, 1);
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
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleSuicide() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, _data, null, 1);
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
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleCrash(String msg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msg);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dumpHeap(boolean managed, boolean mallocInfo, boolean runGc, String path, ParcelFileDescriptor fd) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    int i2;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(managed ? 1 : 0);
                    if (mallocInfo) {
                        i2 = 1;
                    } else {
                        i2 = 0;
                    }
                    _data.writeInt(i2);
                    if (!runGc) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    _data.writeString(path);
                    if (fd != null) {
                        _data.writeInt(1);
                        fd.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(35, _data, null, 1);
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
                    this.mRemote.transact(36, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void clearDnsCache() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, _data, null, 1);
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
                    this.mRemote.transact(38, _data, null, 1);
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
                    this.mRemote.transact(39, _data, null, 1);
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
                    this.mRemote.transact(40, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleTrimMemory(int level) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(level);
                    this.mRemote.transact(41, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void iawareTrimMemory(int level, boolean fromIAware) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(level);
                    if (!fromIAware) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(42, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dumpMemInfo(ParcelFileDescriptor fd, MemoryInfo mem, boolean checkin, boolean dumpInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable, String[] args) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    int i2;
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
                    if (checkin) {
                        i2 = 1;
                    } else {
                        i2 = 0;
                    }
                    _data.writeInt(i2);
                    if (dumpInfo) {
                        i2 = 1;
                    } else {
                        i2 = 0;
                    }
                    _data.writeInt(i2);
                    if (dumpDalvik) {
                        i2 = 1;
                    } else {
                        i2 = 0;
                    }
                    _data.writeInt(i2);
                    if (dumpSummaryOnly) {
                        i2 = 1;
                    } else {
                        i2 = 0;
                    }
                    _data.writeInt(i2);
                    if (!dumpUnreachable) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    _data.writeStringArray(args);
                    this.mRemote.transact(43, _data, null, 1);
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
                    this.mRemote.transact(44, _data, null, 1);
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
                    this.mRemote.transact(45, _data, null, 1);
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
                    this.mRemote.transact(46, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void unstableProviderDied(IBinder provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(provider);
                    this.mRemote.transact(47, _data, null, 1);
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
                    this.mRemote.transact(48, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleTranslucentConversionComplete(IBinder token, boolean timeout) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (!timeout) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(49, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setProcessState(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(50, _data, null, 1);
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
                    this.mRemote.transact(51, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void updateTimePrefs(int timeFormatPreference) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(timeFormatPreference);
                    this.mRemote.transact(52, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleEnterAnimationComplete(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(53, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void notifyCleartextNetwork(byte[] firstPacket) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(firstPacket);
                    this.mRemote.transact(54, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void startBinderTracking() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(55, _data, null, 1);
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
                    this.mRemote.transact(56, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleMultiWindowModeChanged(IBinder token, boolean isInMultiWindowMode, Configuration newConfig) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (!isInMultiWindowMode) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    if (newConfig != null) {
                        _data.writeInt(1);
                        newConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(57, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void schedulePictureInPictureModeChanged(IBinder token, boolean isInPictureInPictureMode, Configuration newConfig) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (!isInPictureInPictureMode) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    if (newConfig != null) {
                        _data.writeInt(1);
                        newConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(58, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void scheduleLocalVoiceInteractionStarted(IBinder token, IVoiceInteractor voiceInteractor) throws RemoteException {
                IBinder iBinder = null;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (voiceInteractor != null) {
                        iBinder = voiceInteractor.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    this.mRemote.transact(59, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void handleTrustStorageUpdate() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(60, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void attachAgent(String path) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(path);
                    this.mRemote.transact(61, _data, null, 1);
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
                    this.mRemote.transact(62, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setNetworkBlockSeq(long procStateSeq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(procStateSeq);
                    this.mRemote.transact(63, _data, null, 1);
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
                    this.mRemote.transact(64, _data, null, 1);
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
                    this.mRemote.transact(65, _data, null, 1);
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
                    this.mRemote.transact(66, _data, null, 1);
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
            IBinder _arg0;
            int _arg1;
            boolean _arg2;
            Intent intent;
            int _arg22;
            Configuration configuration;
            CompatibilityInfo compatibilityInfo;
            int _arg3;
            Bundle bundle;
            String _arg02;
            Intent intent2;
            ParcelFileDescriptor parcelFileDescriptor;
            String _arg32;
            Configuration configuration2;
            boolean _arg03;
            ApplicationInfo applicationInfo;
            CompatibilityInfo compatibilityInfo2;
            Bundle bundle2;
            boolean _arg12;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    schedulePauseActivity(data.readStrongBinder(), data.readInt() != 0, data.readInt() != 0, data.readInt(), data.readInt() != 0);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleStopActivity(data.readStrongBinder(), data.readInt() != 0, data.readInt());
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleWindowVisibility(data.readStrongBinder(), data.readInt() != 0);
                    return true;
                case 4:
                    Bundle bundle3;
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    _arg1 = data.readInt();
                    _arg2 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        bundle3 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundle3 = null;
                    }
                    scheduleResumeActivity(_arg0, _arg1, _arg2, bundle3);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleSendResult(data.readStrongBinder(), data.createTypedArrayList(ResultInfo.CREATOR));
                    return true;
                case 6:
                    ActivityInfo activityInfo;
                    Configuration configuration3;
                    CompatibilityInfo compatibilityInfo3;
                    Bundle bundle4;
                    PersistableBundle persistableBundle;
                    ProfilerInfo profilerInfo;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intent = (Intent) Intent.CREATOR.createFromParcel(data);
                    } else {
                        intent = null;
                    }
                    IBinder _arg13 = data.readStrongBinder();
                    _arg22 = data.readInt();
                    if (data.readInt() != 0) {
                        activityInfo = (ActivityInfo) ActivityInfo.CREATOR.createFromParcel(data);
                    } else {
                        activityInfo = null;
                    }
                    if (data.readInt() != 0) {
                        configuration3 = (Configuration) Configuration.CREATOR.createFromParcel(data);
                    } else {
                        configuration3 = null;
                    }
                    if (data.readInt() != 0) {
                        configuration = (Configuration) Configuration.CREATOR.createFromParcel(data);
                    } else {
                        configuration = null;
                    }
                    if (data.readInt() != 0) {
                        compatibilityInfo3 = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(data);
                    } else {
                        compatibilityInfo3 = null;
                    }
                    String _arg7 = data.readString();
                    IVoiceInteractor _arg8 = com.android.internal.app.IVoiceInteractor.Stub.asInterface(data.readStrongBinder());
                    int _arg9 = data.readInt();
                    if (data.readInt() != 0) {
                        bundle4 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundle4 = null;
                    }
                    if (data.readInt() != 0) {
                        persistableBundle = (PersistableBundle) PersistableBundle.CREATOR.createFromParcel(data);
                    } else {
                        persistableBundle = null;
                    }
                    List<ResultInfo> _arg122 = data.createTypedArrayList(ResultInfo.CREATOR);
                    List<ReferrerIntent> _arg132 = data.createTypedArrayList(ReferrerIntent.CREATOR);
                    boolean _arg14 = data.readInt() != 0;
                    boolean _arg15 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        profilerInfo = (ProfilerInfo) ProfilerInfo.CREATOR.createFromParcel(data);
                    } else {
                        profilerInfo = null;
                    }
                    scheduleLaunchActivity(intent, _arg13, _arg22, activityInfo, configuration3, configuration, compatibilityInfo3, _arg7, _arg8, _arg9, bundle4, persistableBundle, _arg122, _arg132, _arg14, _arg15, profilerInfo);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleNewIntent(data.createTypedArrayList(ReferrerIntent.CREATOR), data.readStrongBinder(), data.readInt() != 0);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleDestroyActivity(data.readStrongBinder(), data.readInt() != 0, data.readInt());
                    return true;
                case 9:
                    ActivityInfo activityInfo2;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        intent = (Intent) Intent.CREATOR.createFromParcel(data);
                    } else {
                        intent = null;
                    }
                    if (data.readInt() != 0) {
                        activityInfo2 = (ActivityInfo) ActivityInfo.CREATOR.createFromParcel(data);
                    } else {
                        activityInfo2 = null;
                    }
                    if (data.readInt() != 0) {
                        compatibilityInfo = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(data);
                    } else {
                        compatibilityInfo = null;
                    }
                    _arg3 = data.readInt();
                    String _arg4 = data.readString();
                    if (data.readInt() != 0) {
                        bundle = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundle = null;
                    }
                    scheduleReceiver(intent, activityInfo2, compatibilityInfo, _arg3, _arg4, bundle, data.readInt() != 0, data.readInt(), data.readInt());
                    return true;
                case 10:
                    ServiceInfo serviceInfo;
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        serviceInfo = (ServiceInfo) ServiceInfo.CREATOR.createFromParcel(data);
                    } else {
                        serviceInfo = null;
                    }
                    if (data.readInt() != 0) {
                        compatibilityInfo = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(data);
                    } else {
                        compatibilityInfo = null;
                    }
                    scheduleCreateService(_arg0, serviceInfo, compatibilityInfo, data.readInt());
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleStopService(data.readStrongBinder());
                    return true;
                case 12:
                    ApplicationInfo applicationInfo2;
                    ComponentName componentName;
                    ProfilerInfo profilerInfo2;
                    Configuration configuration4;
                    CompatibilityInfo compatibilityInfo4;
                    Bundle bundle5;
                    data.enforceInterface(DESCRIPTOR);
                    _arg02 = data.readString();
                    if (data.readInt() != 0) {
                        applicationInfo2 = (ApplicationInfo) ApplicationInfo.CREATOR.createFromParcel(data);
                    } else {
                        applicationInfo2 = null;
                    }
                    List<ProviderInfo> _arg23 = data.createTypedArrayList(ProviderInfo.CREATOR);
                    if (data.readInt() != 0) {
                        componentName = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        componentName = null;
                    }
                    if (data.readInt() != 0) {
                        profilerInfo2 = (ProfilerInfo) ProfilerInfo.CREATOR.createFromParcel(data);
                    } else {
                        profilerInfo2 = null;
                    }
                    if (data.readInt() != 0) {
                        bundle = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundle = null;
                    }
                    IInstrumentationWatcher _arg6 = android.app.IInstrumentationWatcher.Stub.asInterface(data.readStrongBinder());
                    IUiAutomationConnection _arg72 = android.app.IUiAutomationConnection.Stub.asInterface(data.readStrongBinder());
                    int _arg82 = data.readInt();
                    boolean _arg92 = data.readInt() != 0;
                    boolean _arg10 = data.readInt() != 0;
                    boolean _arg11 = data.readInt() != 0;
                    boolean _arg123 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        configuration4 = (Configuration) Configuration.CREATOR.createFromParcel(data);
                    } else {
                        configuration4 = null;
                    }
                    if (data.readInt() != 0) {
                        compatibilityInfo4 = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(data);
                    } else {
                        compatibilityInfo4 = null;
                    }
                    Map _arg152 = data.readHashMap(getClass().getClassLoader());
                    if (data.readInt() != 0) {
                        bundle5 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundle5 = null;
                    }
                    bindApplication(_arg02, applicationInfo2, _arg23, componentName, profilerInfo2, bundle, _arg6, _arg72, _arg82, _arg92, _arg10, _arg11, _arg123, configuration4, compatibilityInfo4, _arg152, bundle5, data.readString());
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleExit();
                    return true;
                case 14:
                    Configuration configuration5;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        configuration5 = (Configuration) Configuration.CREATOR.createFromParcel(data);
                    } else {
                        configuration5 = null;
                    }
                    scheduleConfigurationChanged(configuration5);
                    return true;
                case 15:
                    ParceledListSlice parceledListSlice;
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        parceledListSlice = (ParceledListSlice) ParceledListSlice.CREATOR.createFromParcel(data);
                    } else {
                        parceledListSlice = null;
                    }
                    scheduleServiceArgs(_arg0, parceledListSlice);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    updateTimeZone();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    processInBackground();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        intent2 = (Intent) Intent.CREATOR.createFromParcel(data);
                    } else {
                        intent2 = null;
                    }
                    scheduleBindService(_arg0, intent2, data.readInt() != 0, data.readInt());
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        intent2 = (Intent) Intent.CREATOR.createFromParcel(data);
                    } else {
                        intent2 = null;
                    }
                    scheduleUnbindService(_arg0, intent2);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        parcelFileDescriptor = null;
                    }
                    dumpService(parcelFileDescriptor, data.readStrongBinder(), data.createStringArray());
                    return true;
                case 21:
                    Bundle bundle6;
                    data.enforceInterface(DESCRIPTOR);
                    IIntentReceiver _arg04 = android.content.IIntentReceiver.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        intent2 = (Intent) Intent.CREATOR.createFromParcel(data);
                    } else {
                        intent2 = null;
                    }
                    _arg22 = data.readInt();
                    _arg32 = data.readString();
                    if (data.readInt() != 0) {
                        bundle6 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundle6 = null;
                    }
                    scheduleRegisteredReceiver(_arg04, intent2, _arg22, _arg32, bundle6, data.readInt() != 0, data.readInt() != 0, data.readInt(), data.readInt());
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleLowMemory();
                    return true;
                case 23:
                    Configuration configuration6;
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        configuration6 = (Configuration) Configuration.CREATOR.createFromParcel(data);
                    } else {
                        configuration6 = null;
                    }
                    scheduleActivityConfigurationChanged(_arg0, configuration6);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    _arg1 = data.readInt();
                    if (data.readInt() != 0) {
                        configuration2 = (Configuration) Configuration.CREATOR.createFromParcel(data);
                    } else {
                        configuration2 = null;
                    }
                    scheduleActivityMovedToDisplay(_arg0, _arg1, configuration2);
                    return true;
                case 25:
                    Configuration configuration7;
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    List<ResultInfo> _arg16 = data.createTypedArrayList(ResultInfo.CREATOR);
                    List<ReferrerIntent> _arg24 = data.createTypedArrayList(ReferrerIntent.CREATOR);
                    _arg3 = data.readInt();
                    boolean _arg42 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        configuration = (Configuration) Configuration.CREATOR.createFromParcel(data);
                    } else {
                        configuration = null;
                    }
                    if (data.readInt() != 0) {
                        configuration7 = (Configuration) Configuration.CREATOR.createFromParcel(data);
                    } else {
                        configuration7 = null;
                    }
                    scheduleRelaunchActivity(_arg0, _arg16, _arg24, _arg3, _arg42, configuration, configuration7, data.readInt() != 0);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleSleeping(data.readStrongBinder(), data.readInt() != 0);
                    return true;
                case 27:
                    ProfilerInfo profilerInfo3;
                    data.enforceInterface(DESCRIPTOR);
                    _arg03 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        profilerInfo3 = (ProfilerInfo) ProfilerInfo.CREATOR.createFromParcel(data);
                    } else {
                        profilerInfo3 = null;
                    }
                    profilerControl(_arg03, profilerInfo3, data.readInt());
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    setSchedulingGroup(data.readInt());
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        applicationInfo = (ApplicationInfo) ApplicationInfo.CREATOR.createFromParcel(data);
                    } else {
                        applicationInfo = null;
                    }
                    if (data.readInt() != 0) {
                        compatibilityInfo2 = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(data);
                    } else {
                        compatibilityInfo2 = null;
                    }
                    scheduleCreateBackupAgent(applicationInfo, compatibilityInfo2, data.readInt());
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        applicationInfo = (ApplicationInfo) ApplicationInfo.CREATOR.createFromParcel(data);
                    } else {
                        applicationInfo = null;
                    }
                    if (data.readInt() != 0) {
                        compatibilityInfo2 = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(data);
                    } else {
                        compatibilityInfo2 = null;
                    }
                    scheduleDestroyBackupAgent(applicationInfo, compatibilityInfo2);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        bundle2 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundle2 = null;
                    }
                    scheduleOnNewActivityOptions(_arg0, bundle2);
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleSuicide();
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    dispatchPackageBroadcast(data.readInt(), data.createStringArray());
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleCrash(data.readString());
                    return true;
                case 35:
                    ParcelFileDescriptor parcelFileDescriptor2;
                    data.enforceInterface(DESCRIPTOR);
                    _arg03 = data.readInt() != 0;
                    _arg12 = data.readInt() != 0;
                    _arg2 = data.readInt() != 0;
                    _arg32 = data.readString();
                    if (data.readInt() != 0) {
                        parcelFileDescriptor2 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        parcelFileDescriptor2 = null;
                    }
                    dumpHeap(_arg03, _arg12, _arg2, _arg32, parcelFileDescriptor2);
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        parcelFileDescriptor = null;
                    }
                    dumpActivity(parcelFileDescriptor, data.readStrongBinder(), data.readString(), data.createStringArray());
                    return true;
                case 37:
                    data.enforceInterface(DESCRIPTOR);
                    clearDnsCache();
                    return true;
                case 38:
                    Uri uri;
                    data.enforceInterface(DESCRIPTOR);
                    _arg02 = data.readString();
                    String _arg17 = data.readString();
                    String _arg25 = data.readString();
                    if (data.readInt() != 0) {
                        uri = (Uri) Uri.CREATOR.createFromParcel(data);
                    } else {
                        uri = null;
                    }
                    setHttpProxy(_arg02, _arg17, _arg25, uri);
                    return true;
                case 39:
                    Bundle bundle7;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bundle7 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundle7 = null;
                    }
                    setCoreSettings(bundle7);
                    return true;
                case 40:
                    data.enforceInterface(DESCRIPTOR);
                    _arg02 = data.readString();
                    if (data.readInt() != 0) {
                        compatibilityInfo2 = (CompatibilityInfo) CompatibilityInfo.CREATOR.createFromParcel(data);
                    } else {
                        compatibilityInfo2 = null;
                    }
                    updatePackageCompatibilityInfo(_arg02, compatibilityInfo2);
                    return true;
                case 41:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleTrimMemory(data.readInt());
                    return true;
                case 42:
                    data.enforceInterface(DESCRIPTOR);
                    iawareTrimMemory(data.readInt(), data.readInt() != 0);
                    return true;
                case 43:
                    MemoryInfo memoryInfo;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        parcelFileDescriptor = null;
                    }
                    if (data.readInt() != 0) {
                        memoryInfo = (MemoryInfo) MemoryInfo.CREATOR.createFromParcel(data);
                    } else {
                        memoryInfo = null;
                    }
                    dumpMemInfo(parcelFileDescriptor, memoryInfo, data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.createStringArray());
                    return true;
                case 44:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        parcelFileDescriptor = null;
                    }
                    dumpGfxInfo(parcelFileDescriptor, data.createStringArray());
                    return true;
                case 45:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        parcelFileDescriptor = null;
                    }
                    dumpProvider(parcelFileDescriptor, data.readStrongBinder(), data.createStringArray());
                    return true;
                case 46:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        parcelFileDescriptor = null;
                    }
                    dumpDbInfo(parcelFileDescriptor, data.createStringArray());
                    return true;
                case 47:
                    data.enforceInterface(DESCRIPTOR);
                    unstableProviderDied(data.readStrongBinder());
                    return true;
                case 48:
                    data.enforceInterface(DESCRIPTOR);
                    requestAssistContextExtras(data.readStrongBinder(), data.readStrongBinder(), data.readInt(), data.readInt(), data.readInt());
                    return true;
                case 49:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleTranslucentConversionComplete(data.readStrongBinder(), data.readInt() != 0);
                    return true;
                case 50:
                    data.enforceInterface(DESCRIPTOR);
                    setProcessState(data.readInt());
                    return true;
                case 51:
                    ProviderInfo providerInfo;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        providerInfo = (ProviderInfo) ProviderInfo.CREATOR.createFromParcel(data);
                    } else {
                        providerInfo = null;
                    }
                    scheduleInstallProvider(providerInfo);
                    return true;
                case 52:
                    data.enforceInterface(DESCRIPTOR);
                    updateTimePrefs(data.readInt());
                    return true;
                case 53:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleEnterAnimationComplete(data.readStrongBinder());
                    return true;
                case 54:
                    data.enforceInterface(DESCRIPTOR);
                    notifyCleartextNetwork(data.createByteArray());
                    return true;
                case 55:
                    data.enforceInterface(DESCRIPTOR);
                    startBinderTracking();
                    return true;
                case 56:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(data);
                    } else {
                        parcelFileDescriptor = null;
                    }
                    stopBinderTrackingAndDump(parcelFileDescriptor);
                    return true;
                case 57:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    _arg12 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        configuration2 = (Configuration) Configuration.CREATOR.createFromParcel(data);
                    } else {
                        configuration2 = null;
                    }
                    scheduleMultiWindowModeChanged(_arg0, _arg12, configuration2);
                    return true;
                case 58:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    _arg12 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        configuration2 = (Configuration) Configuration.CREATOR.createFromParcel(data);
                    } else {
                        configuration2 = null;
                    }
                    schedulePictureInPictureModeChanged(_arg0, _arg12, configuration2);
                    return true;
                case 59:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleLocalVoiceInteractionStarted(data.readStrongBinder(), com.android.internal.app.IVoiceInteractor.Stub.asInterface(data.readStrongBinder()));
                    return true;
                case 60:
                    data.enforceInterface(DESCRIPTOR);
                    handleTrustStorageUpdate();
                    return true;
                case 61:
                    data.enforceInterface(DESCRIPTOR);
                    attachAgent(data.readString());
                    return true;
                case 62:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        applicationInfo = (ApplicationInfo) ApplicationInfo.CREATOR.createFromParcel(data);
                    } else {
                        applicationInfo = null;
                    }
                    scheduleApplicationInfoChanged(applicationInfo);
                    return true;
                case 63:
                    data.enforceInterface(DESCRIPTOR);
                    setNetworkBlockSeq(data.readLong());
                    return true;
                case 64:
                    data.enforceInterface(DESCRIPTOR);
                    schedulePCWindowStateChanged(data.readStrongBinder(), data.readInt());
                    return true;
                case 65:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        bundle2 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundle2 = null;
                    }
                    requestContentNode(_arg0, bundle2, data.readInt());
                    return true;
                case 66:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        bundle2 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        bundle2 = null;
                    }
                    requestContentOther(_arg0, bundle2, data.readInt());
                    return true;
                case IBinder.INTERFACE_TRANSACTION /*1598968902*/:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    void attachAgent(String str) throws RemoteException;

    void bindApplication(String str, ApplicationInfo applicationInfo, List<ProviderInfo> list, ComponentName componentName, ProfilerInfo profilerInfo, Bundle bundle, IInstrumentationWatcher iInstrumentationWatcher, IUiAutomationConnection iUiAutomationConnection, int i, boolean z, boolean z2, boolean z3, boolean z4, Configuration configuration, CompatibilityInfo compatibilityInfo, Map map, Bundle bundle2, String str2) throws RemoteException;

    void clearDnsCache() throws RemoteException;

    void dispatchPackageBroadcast(int i, String[] strArr) throws RemoteException;

    void dumpActivity(ParcelFileDescriptor parcelFileDescriptor, IBinder iBinder, String str, String[] strArr) throws RemoteException;

    void dumpDbInfo(ParcelFileDescriptor parcelFileDescriptor, String[] strArr) throws RemoteException;

    void dumpGfxInfo(ParcelFileDescriptor parcelFileDescriptor, String[] strArr) throws RemoteException;

    void dumpHeap(boolean z, boolean z2, boolean z3, String str, ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void dumpMemInfo(ParcelFileDescriptor parcelFileDescriptor, MemoryInfo memoryInfo, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, String[] strArr) throws RemoteException;

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

    void scheduleActivityConfigurationChanged(IBinder iBinder, Configuration configuration) throws RemoteException;

    void scheduleActivityMovedToDisplay(IBinder iBinder, int i, Configuration configuration) throws RemoteException;

    void scheduleApplicationInfoChanged(ApplicationInfo applicationInfo) throws RemoteException;

    void scheduleBindService(IBinder iBinder, Intent intent, boolean z, int i) throws RemoteException;

    void scheduleConfigurationChanged(Configuration configuration) throws RemoteException;

    void scheduleCrash(String str) throws RemoteException;

    void scheduleCreateBackupAgent(ApplicationInfo applicationInfo, CompatibilityInfo compatibilityInfo, int i) throws RemoteException;

    void scheduleCreateService(IBinder iBinder, ServiceInfo serviceInfo, CompatibilityInfo compatibilityInfo, int i) throws RemoteException;

    void scheduleDestroyActivity(IBinder iBinder, boolean z, int i) throws RemoteException;

    void scheduleDestroyBackupAgent(ApplicationInfo applicationInfo, CompatibilityInfo compatibilityInfo) throws RemoteException;

    void scheduleEnterAnimationComplete(IBinder iBinder) throws RemoteException;

    void scheduleExit() throws RemoteException;

    void scheduleInstallProvider(ProviderInfo providerInfo) throws RemoteException;

    void scheduleLaunchActivity(Intent intent, IBinder iBinder, int i, ActivityInfo activityInfo, Configuration configuration, Configuration configuration2, CompatibilityInfo compatibilityInfo, String str, IVoiceInteractor iVoiceInteractor, int i2, Bundle bundle, PersistableBundle persistableBundle, List<ResultInfo> list, List<ReferrerIntent> list2, boolean z, boolean z2, ProfilerInfo profilerInfo) throws RemoteException;

    void scheduleLocalVoiceInteractionStarted(IBinder iBinder, IVoiceInteractor iVoiceInteractor) throws RemoteException;

    void scheduleLowMemory() throws RemoteException;

    void scheduleMultiWindowModeChanged(IBinder iBinder, boolean z, Configuration configuration) throws RemoteException;

    void scheduleNewIntent(List<ReferrerIntent> list, IBinder iBinder, boolean z) throws RemoteException;

    void scheduleOnNewActivityOptions(IBinder iBinder, Bundle bundle) throws RemoteException;

    void schedulePCWindowStateChanged(IBinder iBinder, int i) throws RemoteException;

    void schedulePauseActivity(IBinder iBinder, boolean z, boolean z2, int i, boolean z3) throws RemoteException;

    void schedulePictureInPictureModeChanged(IBinder iBinder, boolean z, Configuration configuration) throws RemoteException;

    void scheduleReceiver(Intent intent, ActivityInfo activityInfo, CompatibilityInfo compatibilityInfo, int i, String str, Bundle bundle, boolean z, int i2, int i3) throws RemoteException;

    void scheduleRegisteredReceiver(IIntentReceiver iIntentReceiver, Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2, int i3) throws RemoteException;

    void scheduleRelaunchActivity(IBinder iBinder, List<ResultInfo> list, List<ReferrerIntent> list2, int i, boolean z, Configuration configuration, Configuration configuration2, boolean z2) throws RemoteException;

    void scheduleResumeActivity(IBinder iBinder, int i, boolean z, Bundle bundle) throws RemoteException;

    void scheduleSendResult(IBinder iBinder, List<ResultInfo> list) throws RemoteException;

    void scheduleServiceArgs(IBinder iBinder, ParceledListSlice parceledListSlice) throws RemoteException;

    void scheduleSleeping(IBinder iBinder, boolean z) throws RemoteException;

    void scheduleStopActivity(IBinder iBinder, boolean z, int i) throws RemoteException;

    void scheduleStopService(IBinder iBinder) throws RemoteException;

    void scheduleSuicide() throws RemoteException;

    void scheduleTranslucentConversionComplete(IBinder iBinder, boolean z) throws RemoteException;

    void scheduleTrimMemory(int i) throws RemoteException;

    void scheduleUnbindService(IBinder iBinder, Intent intent) throws RemoteException;

    void scheduleWindowVisibility(IBinder iBinder, boolean z) throws RemoteException;

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
