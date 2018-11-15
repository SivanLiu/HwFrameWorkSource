package com.android.server;

import android.app.ActivityManagerInternal;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.health.V1_0.HealthInfo;
import android.hardware.health.V2_0.IHealth;
import android.hardware.health.V2_0.IHealthInfoCallback;
import android.hardware.health.V2_0.Result;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.metrics.LogMaker;
import android.os.BatteryManager;
import android.os.BatteryManagerInternal;
import android.os.BatteryProperty;
import android.os.Binder;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBatteryPropertiesListener;
import android.os.IBatteryPropertiesRegistrar.Stub;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.Trace;
import android.os.UEventObserver;
import android.os.UEventObserver.UEvent;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.util.EventLog;
import android.util.Flog;
import android.util.MutableInt;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.DumpUtils;
import com.android.server.am.AbsHwMtmBroadcastResourceManager;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.BatteryStatsService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.pm.DumpState;
import com.android.server.power.PowerManagerService;
import com.android.server.storage.DeviceStorageMonitorService;
import com.android.server.utils.PriorityDump;
import com.huawei.android.os.HwVibrator;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class BatteryService extends AbsBatteryService {
    private static final long BATTERY_LEVEL_CHANGE_THROTTLE_MS = 60000;
    private static final int BATTERY_PLUGGED_NONE = 0;
    private static final int BATTERY_SCALE = 100;
    private static final String CHARGER_TYPE = "sys/class/hw_power/charger/charge_data/chargerType";
    private static final String CHARGE_STATUS = "charge_status";
    private static final String CHARGE_WIRED_STATUS = "1";
    private static final String CHARGE_WIRELESS_STATUS = "2";
    private static final boolean DEBUG = false;
    private static final String[] DUMPSYS_ARGS = new String[]{"--checkin", "--unplugged"};
    private static final String DUMPSYS_DATA_PATH = "/data/system/";
    private static final long HEALTH_HAL_WAIT_MS = 1000;
    private static final boolean IS_EMU = boardname.contains("emulator");
    private static final boolean IS_FPGA = boardname.contains("fpga");
    private static final boolean IS_UDP = boardname.contains("udp");
    private static final int MAX_BATTERY_LEVELS_QUEUE_SIZE = 100;
    static final int OPTION_FORCE_UPDATE = 1;
    private static final int SHUTDOWN_DELAY_TIMEOUT = 20000;
    private static final String TAG = BatteryService.class.getSimpleName();
    private static final int VIBRATE_LAST_TIME = 300;
    private static String boardname = SystemProperties.get("ro.board.boardname", "0");
    private final String changingVibrateType = "haptic.battery.charging";
    private ActivityManagerInternal mActivityManagerInternal;
    private ActivityManagerService mAms = null;
    private boolean mBatteryLevelCritical;
    private boolean mBatteryLevelLow;
    private ArrayDeque<Bundle> mBatteryLevelsEventQueue;
    private BatteryPropertiesRegistrar mBatteryPropertiesRegistrar;
    private final IBatteryStats mBatteryStats;
    BinderService mBinderService;
    private int mChargeStartLevel;
    private long mChargeStartTime;
    private final Context mContext;
    private int mCriticalBatteryLevel;
    private int mDischargeStartLevel;
    private long mDischargeStartTime;
    private final Handler mHandler;
    private HealthHalCallback mHealthHalCallback;
    private HealthInfo mHealthInfo;
    private HealthServiceWrapper mHealthServiceWrapper;
    private int mInvalidCharger;
    private int mLastBatteryHealth;
    private int mLastBatteryLevel;
    private long mLastBatteryLevelChangedSentMs;
    private boolean mLastBatteryLevelCritical;
    private boolean mLastBatteryPresent;
    private int mLastBatteryStatus;
    private int mLastBatteryTemperature;
    private int mLastBatteryVoltage;
    private int mLastChargeCounter;
    private final HealthInfo mLastHealthInfo = new HealthInfo();
    private int mLastInvalidCharger;
    private int mLastMaxChargingCurrent;
    private int mLastMaxChargingVoltage;
    private int mLastPlugType = -1;
    private Led mLed;
    private final Object mLock = new Object();
    private int mLowBatteryCloseWarningLevel;
    private int mLowBatteryWarningLevel;
    private MetricsLogger mMetricsLogger;
    private CheckForShutdown mPendingCheckForShutdown;
    protected int mPlugType;
    private boolean mSentLowBatteryBroadcast = false;
    private int mSequence = 1;
    private int mShutdownBatteryTemperature;
    private boolean mUpdatesStopped;

    private final class BatteryPropertiesRegistrar extends Stub {
        private BatteryPropertiesRegistrar() {
        }

        /* synthetic */ BatteryPropertiesRegistrar(BatteryService x0, AnonymousClass1 x1) {
            this();
        }

        public void registerListener(IBatteryPropertiesListener listener) {
            Slog.e(BatteryService.TAG, "health: must not call registerListener on battery properties");
        }

        public void unregisterListener(IBatteryPropertiesListener listener) {
            Slog.e(BatteryService.TAG, "health: must not call unregisterListener on battery properties");
        }

        public int getProperty(int id, BatteryProperty prop) throws RemoteException {
            BatteryService.traceBegin("HealthGetProperty");
            try {
                IHealth service = BatteryService.this.mHealthServiceWrapper.getLastService();
                if (service != null) {
                    MutableInt outResult = new MutableInt(1);
                    switch (id) {
                        case 1:
                            service.getChargeCounter(new -$$Lambda$BatteryService$BatteryPropertiesRegistrar$7Y-B9O7NDYgUY9hQvFzC2FQ2V5w(outResult, prop));
                            break;
                        case 2:
                            service.getCurrentNow(new -$$Lambda$BatteryService$BatteryPropertiesRegistrar$JTQ79fl14NyImudsJhx-Mp1dJI8(outResult, prop));
                            break;
                        case 3:
                            service.getCurrentAverage(new -$$Lambda$BatteryService$BatteryPropertiesRegistrar$KZAu97wwr_7_MI0awCjQTzdIuAI(outResult, prop));
                            break;
                        case 4:
                            service.getCapacity(new -$$Lambda$BatteryService$BatteryPropertiesRegistrar$DM4ow6LC--JYWBfhHp2f1JW8nww(outResult, prop));
                            break;
                        case 5:
                            service.getEnergyCounter(new -$$Lambda$BatteryService$BatteryPropertiesRegistrar$9z3zqgxtPzBN8Qoni5nHVb0m8EY(outResult, prop));
                            break;
                        case 6:
                            service.getChargeStatus(new -$$Lambda$BatteryService$BatteryPropertiesRegistrar$hInbvsihGvN2hXqvdcoFYzdeqHw(outResult, prop));
                            break;
                    }
                    int i = outResult.value;
                    return i;
                }
                throw new RemoteException("no health service");
            } finally {
                BatteryService.traceEnd();
            }
        }

        static /* synthetic */ void lambda$getProperty$0(MutableInt outResult, BatteryProperty prop, int result, int value) {
            outResult.value = result;
            if (result == 0) {
                prop.setLong((long) value);
            }
        }

        static /* synthetic */ void lambda$getProperty$1(MutableInt outResult, BatteryProperty prop, int result, int value) {
            outResult.value = result;
            if (result == 0) {
                prop.setLong((long) value);
            }
        }

        static /* synthetic */ void lambda$getProperty$2(MutableInt outResult, BatteryProperty prop, int result, int value) {
            outResult.value = result;
            if (result == 0) {
                prop.setLong((long) value);
            }
        }

        static /* synthetic */ void lambda$getProperty$3(MutableInt outResult, BatteryProperty prop, int result, int value) {
            outResult.value = result;
            if (result == 0) {
                prop.setLong((long) value);
            }
        }

        static /* synthetic */ void lambda$getProperty$4(MutableInt outResult, BatteryProperty prop, int result, int value) {
            outResult.value = result;
            if (result == 0) {
                prop.setLong((long) value);
            }
        }

        static /* synthetic */ void lambda$getProperty$5(MutableInt outResult, BatteryProperty prop, int result, long value) {
            outResult.value = result;
            if (result == 0) {
                prop.setLong(value);
            }
        }

        public void scheduleUpdate() throws RemoteException {
            BatteryService.traceBegin("HealthScheduleUpdate");
            try {
                IHealth service = BatteryService.this.mHealthServiceWrapper.getLastService();
                if (service != null) {
                    service.update();
                    return;
                }
                throw new RemoteException("no health service");
            } finally {
                BatteryService.traceEnd();
            }
        }

        public int alterWirelessTxSwitch(int status) {
            return BatteryService.this.alterWirelessTxSwitch(status);
        }

        public int getWirelessTxSwitch() {
            return BatteryService.this.getWirelessTxSwitch();
        }

        public boolean supportWirelessTxCharge() {
            return BatteryService.this.supportWirelessTxCharge();
        }
    }

    private final class BinderService extends Binder {
        private BinderService() {
        }

        /* synthetic */ BinderService(BatteryService x0, AnonymousClass1 x1) {
            this();
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(BatteryService.this.mContext, BatteryService.TAG, pw)) {
                if (args.length <= 0 || !PriorityDump.PROTO_ARG.equals(args[0])) {
                    BatteryService.this.dumpInternal(fd, pw, args);
                } else {
                    BatteryService.this.dumpProto(fd);
                }
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new Shell().exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    private final class CheckForShutdown implements Runnable {
        private CheckForShutdown() {
        }

        /* synthetic */ CheckForShutdown(BatteryService x0, AnonymousClass1 x1) {
            this();
        }

        public void run() {
            PowerManagerService.lowLevelShutdown("battery");
        }
    }

    @VisibleForTesting
    static final class HealthServiceWrapper {
        public static final String INSTANCE_HEALTHD = "backup";
        public static final String INSTANCE_VENDOR = "default";
        private static final String TAG = "HealthServiceWrapper";
        private static final List<String> sAllInstances = Arrays.asList(new String[]{INSTANCE_VENDOR, INSTANCE_HEALTHD});
        private Callback mCallback;
        private final HandlerThread mHandlerThread = new HandlerThread("HealthServiceRefresh");
        private IHealthSupplier mHealthSupplier;
        private String mInstanceName;
        private final AtomicReference<IHealth> mLastService = new AtomicReference();
        private final IServiceNotification mNotification = new Notification(this, null);

        interface Callback {
            void onRegistration(IHealth iHealth, IHealth iHealth2, String str);
        }

        interface IHealthSupplier {
            IHealth get(String name) throws NoSuchElementException, RemoteException {
                return IHealth.getService(name, true);
            }
        }

        interface IServiceManagerSupplier {
            IServiceManager get() throws NoSuchElementException, RemoteException {
                return IServiceManager.getService();
            }
        }

        private class Notification extends IServiceNotification.Stub {
            private Notification() {
            }

            /* synthetic */ Notification(HealthServiceWrapper x0, AnonymousClass1 x1) {
                this();
            }

            public final void onRegistration(String interfaceName, String instanceName, boolean preexisting) {
                if (IHealth.kInterfaceName.equals(interfaceName) && HealthServiceWrapper.this.mInstanceName.equals(instanceName)) {
                    HealthServiceWrapper.this.mHandlerThread.getThreadHandler().post(new Runnable() {
                        /* JADX WARNING: Removed duplicated region for block: B:5:0x005b A:{Splitter: B:0:0x0000, ExcHandler: java.util.NoSuchElementException (r0_4 'ex' java.lang.Exception)} */
                        /* JADX WARNING: Missing block: B:5:0x005b, code:
            r0 = move-exception;
     */
                        /* JADX WARNING: Missing block: B:6:0x005c, code:
            r1 = com.android.server.BatteryService.HealthServiceWrapper.TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("health: Cannot get instance '");
            r2.append(com.android.server.BatteryService.HealthServiceWrapper.access$2500(r5.this$1.this$0));
            r2.append("': ");
            r2.append(r0.getMessage());
            r2.append(". Perhaps no permission?");
            android.util.Slog.e(r1, r2.toString());
     */
                        /* Code decompiled incorrectly, please refer to instructions dump. */
                        public void run() {
                            try {
                                IHealth newService = HealthServiceWrapper.this.mHealthSupplier.get(HealthServiceWrapper.this.mInstanceName);
                                IHealth oldService = (IHealth) HealthServiceWrapper.this.mLastService.getAndSet(newService);
                                if (!Objects.equals(newService, oldService)) {
                                    String str = HealthServiceWrapper.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("health: new instance registered ");
                                    stringBuilder.append(HealthServiceWrapper.this.mInstanceName);
                                    Slog.i(str, stringBuilder.toString());
                                    HealthServiceWrapper.this.mCallback.onRegistration(oldService, newService, HealthServiceWrapper.this.mInstanceName);
                                }
                            } catch (Exception ex) {
                            }
                        }
                    });
                }
            }
        }

        HealthServiceWrapper() {
        }

        IHealth getLastService() {
            return (IHealth) this.mLastService.get();
        }

        void init(Callback callback, IServiceManagerSupplier managerSupplier, IHealthSupplier healthSupplier) throws RemoteException, NoSuchElementException, NullPointerException {
            if (callback == null || managerSupplier == null || healthSupplier == null) {
                throw new NullPointerException();
            }
            this.mCallback = callback;
            this.mHealthSupplier = healthSupplier;
            IHealth newService = null;
            for (String name : sAllInstances) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("HealthInitGetService_");
                stringBuilder.append(name);
                BatteryService.traceBegin(stringBuilder.toString());
                try {
                    newService = healthSupplier.get(name);
                } catch (NoSuchElementException e) {
                } catch (Throwable th) {
                    BatteryService.traceEnd();
                }
                BatteryService.traceEnd();
                if (newService != null) {
                    this.mInstanceName = name;
                    this.mLastService.set(newService);
                    break;
                }
            }
            if (this.mInstanceName == null || newService == null) {
                throw new NoSuchElementException(String.format("No IHealth service instance among %s is available. Perhaps no permission?", new Object[]{sAllInstances.toString()}));
            }
            this.mCallback.onRegistration(null, newService, this.mInstanceName);
            BatteryService.traceBegin("HealthInitRegisterNotification");
            this.mHandlerThread.start();
            try {
                managerSupplier.get().registerForNotifications(IHealth.kInterfaceName, this.mInstanceName, this.mNotification);
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("health: HealthServiceWrapper listening to instance ");
                stringBuilder2.append(this.mInstanceName);
                Slog.i(str, stringBuilder2.toString());
            } finally {
                BatteryService.traceEnd();
            }
        }

        @VisibleForTesting
        HandlerThread getHandlerThread() {
            return this.mHandlerThread;
        }
    }

    private final class Led {
        private final int mBatteryFullARGB;
        private final int mBatteryLedOff;
        private final int mBatteryLedOn;
        private final Light mBatteryLight;
        private final int mBatteryLowARGB;
        private final int mBatteryMediumARGB;

        public Led(Context context, LightsManager lights) {
            this.mBatteryLight = lights.getLight(3);
            this.mBatteryLowARGB = context.getResources().getInteger(17694840);
            this.mBatteryMediumARGB = context.getResources().getInteger(17694841);
            this.mBatteryFullARGB = context.getResources().getInteger(17694837);
            this.mBatteryLedOn = context.getResources().getInteger(17694839);
            this.mBatteryLedOff = context.getResources().getInteger(17694838);
        }

        public void updateLightsLocked() {
            int level = BatteryService.this.mHealthInfo.batteryLevel;
            int status = BatteryService.this.mHealthInfo.batteryStatus;
            if (level < BatteryService.this.mLowBatteryWarningLevel) {
                if (status == 2) {
                    this.mBatteryLight.setColor(this.mBatteryLowARGB);
                } else {
                    this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, this.mBatteryLedOn, this.mBatteryLedOff);
                }
            } else if (status != 2 && status != 5) {
                this.mBatteryLight.turnOff();
            } else if (status == 5 || level >= 90) {
                this.mBatteryLight.setColor(this.mBatteryFullARGB);
            } else {
                this.mBatteryLight.setColor(this.mBatteryMediumARGB);
            }
        }
    }

    private final class LocalService extends BatteryManagerInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(BatteryService x0, AnonymousClass1 x1) {
            this();
        }

        public boolean isPowered(int plugTypeSet) {
            boolean access$2200;
            synchronized (BatteryService.this.mLock) {
                access$2200 = BatteryService.this.isPoweredLocked(plugTypeSet);
            }
            return access$2200;
        }

        public int getPlugType() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mPlugType;
            }
            return i;
        }

        public int getBatteryLevel() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mHealthInfo.batteryLevel;
            }
            return i;
        }

        public int getBatteryChargeCounter() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mHealthInfo.batteryChargeCounter;
            }
            return i;
        }

        public int getBatteryFullCharge() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mHealthInfo.batteryFullCharge;
            }
            return i;
        }

        public boolean getBatteryLevelLow() {
            boolean access$2300;
            synchronized (BatteryService.this.mLock) {
                access$2300 = BatteryService.this.mBatteryLevelLow;
            }
            return access$2300;
        }

        public int getInvalidCharger() {
            int access$100;
            synchronized (BatteryService.this.mLock) {
                access$100 = BatteryService.this.mInvalidCharger;
            }
            return access$100;
        }

        public void updateBatteryLight(boolean enable, int ledOnMS, int ledOffMS) {
            synchronized (BatteryService.this.mLock) {
                BatteryService.this.updateLight(enable, ledOnMS, ledOffMS);
            }
        }

        public void notifyFrontCameraStates(boolean opened) {
            BatteryService.this.cameraUpdateLight(opened);
        }
    }

    class Shell extends ShellCommand {
        Shell() {
        }

        public int onCommand(String cmd) {
            return BatteryService.this.onShellCommand(this, cmd);
        }

        public void onHelp() {
            BatteryService.dumpHelp(getOutPrintWriter());
        }
    }

    private final class HealthHalCallback extends IHealthInfoCallback.Stub implements Callback {
        private HealthHalCallback() {
        }

        /* synthetic */ HealthHalCallback(BatteryService x0, AnonymousClass1 x1) {
            this();
        }

        public void healthInfoChanged(android.hardware.health.V2_0.HealthInfo props) {
            BatteryService.this.update(props);
        }

        public void onRegistration(IHealth oldService, IHealth newService, String instance) {
            if (newService != null) {
                int r;
                String access$800;
                StringBuilder stringBuilder;
                BatteryService.traceBegin("HealthUnregisterCallback");
                if (oldService != null) {
                    try {
                        r = oldService.unregisterCallback(this);
                        if (r != 0) {
                            access$800 = BatteryService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("health: cannot unregister previous callback: ");
                            stringBuilder.append(Result.toString(r));
                            Slog.w(access$800, stringBuilder.toString());
                        }
                    } catch (RemoteException ex) {
                        access$800 = BatteryService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("health: cannot unregister previous callback (transaction error): ");
                        stringBuilder.append(ex.getMessage());
                        Slog.w(access$800, stringBuilder.toString());
                    } catch (Throwable th) {
                        BatteryService.traceEnd();
                    }
                }
                BatteryService.traceEnd();
                BatteryService.traceBegin("HealthRegisterCallback");
                try {
                    r = newService.registerCallback(this);
                    if (r != 0) {
                        access$800 = BatteryService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("health: cannot register callback: ");
                        stringBuilder.append(Result.toString(r));
                        Slog.w(access$800, stringBuilder.toString());
                        BatteryService.traceEnd();
                        return;
                    }
                    newService.update();
                    BatteryService.traceEnd();
                } catch (RemoteException ex2) {
                    access$800 = BatteryService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("health: cannot register callback (transaction error): ");
                    stringBuilder.append(ex2.getMessage());
                    Slog.e(access$800, stringBuilder.toString());
                } catch (Throwable th2) {
                    BatteryService.traceEnd();
                }
            }
        }
    }

    public BatteryService(Context context) {
        super(context);
        this.mContext = context;
        this.mHandler = new Handler(true);
        this.mLed = new Led(context, (LightsManager) getLocalService(LightsManager.class));
        this.mBatteryStats = BatteryStatsService.getService();
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mAms = (ActivityManagerService) ServiceManager.getService("activity");
        this.mCriticalBatteryLevel = this.mContext.getResources().getInteger(17694757);
        this.mLowBatteryWarningLevel = this.mContext.getResources().getInteger(17694805);
        this.mLowBatteryCloseWarningLevel = this.mLowBatteryWarningLevel + this.mContext.getResources().getInteger(17694804);
        this.mShutdownBatteryTemperature = this.mContext.getResources().getInteger(17694866);
        this.mBatteryLevelsEventQueue = new ArrayDeque();
        this.mMetricsLogger = new MetricsLogger();
        if (new File("/sys/devices/virtual/switch/invalid_charger/state").exists()) {
            new UEventObserver() {
                public void onUEvent(UEvent event) {
                    int invalidCharger = BatteryService.CHARGE_WIRED_STATUS.equals(event.get("SWITCH_STATE"));
                    synchronized (BatteryService.this.mLock) {
                        if (BatteryService.this.mInvalidCharger != invalidCharger) {
                            BatteryService.this.mInvalidCharger = invalidCharger;
                        }
                    }
                }
            }.startObserving("DEVPATH=/devices/virtual/switch/invalid_charger");
        }
    }

    public void onStart() {
        registerHealthCallback();
        this.mBinderService = new BinderService(this, null);
        publishBinderService("battery", this.mBinderService);
        this.mBatteryPropertiesRegistrar = new BatteryPropertiesRegistrar(this, null);
        publishBinderService("batteryproperties", this.mBatteryPropertiesRegistrar);
        publishLocalService(BatteryManagerInternal.class, new LocalService(this, null));
    }

    public void onBootPhase(int phase) {
        if (phase == 550) {
            synchronized (this.mLock) {
                this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("low_power_trigger_level"), false, new ContentObserver(this.mHandler) {
                    public void onChange(boolean selfChange) {
                        synchronized (BatteryService.this.mLock) {
                            BatteryService.this.updateBatteryWarningLevelLocked();
                        }
                    }
                }, -1);
                updateBatteryWarningLevelLocked();
            }
        }
    }

    protected void registerHealthCallback() {
        traceBegin("HealthInitWrapper");
        this.mHealthServiceWrapper = new HealthServiceWrapper();
        this.mHealthHalCallback = new HealthHalCallback(this, null);
        try {
            this.mHealthServiceWrapper.init(this.mHealthHalCallback, new IServiceManagerSupplier() {
            }, new IHealthSupplier() {
            });
            traceEnd();
            traceBegin("HealthInitWaitUpdate");
            long beforeWait = SystemClock.uptimeMillis();
            synchronized (this.mLock) {
                while (this.mHealthInfo == null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("health: Waited ");
                    stringBuilder.append(SystemClock.uptimeMillis() - beforeWait);
                    stringBuilder.append("ms for callbacks. Waiting another ");
                    stringBuilder.append(1000);
                    stringBuilder.append(" ms...");
                    Slog.i(str, stringBuilder.toString());
                    try {
                        this.mLock.wait(1000);
                    } catch (InterruptedException e) {
                        Slog.i(TAG, "health: InterruptedException when waiting for update.  Continuing...");
                    }
                }
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("health: Waited ");
            stringBuilder2.append(SystemClock.uptimeMillis() - beforeWait);
            stringBuilder2.append("ms and received the update.");
            Slog.i(str2, stringBuilder2.toString());
            traceEnd();
        } catch (RemoteException ex) {
            Slog.e(TAG, "health: cannot register callback. (RemoteException)");
            throw ex.rethrowFromSystemServer();
        } catch (NoSuchElementException ex2) {
            Slog.e(TAG, "health: cannot register callback. (no supported health HAL service)");
            throw ex2;
        } catch (Throwable th) {
            traceEnd();
        }
    }

    private void updateBatteryWarningLevelLocked() {
        ContentResolver resolver = this.mContext.getContentResolver();
        int defWarnLevel = this.mContext.getResources().getInteger(17694805);
        this.mLowBatteryWarningLevel = Global.getInt(resolver, "low_power_trigger_level", defWarnLevel);
        if (this.mLowBatteryWarningLevel == 0) {
            this.mLowBatteryWarningLevel = defWarnLevel;
        }
        if (this.mLowBatteryWarningLevel < this.mCriticalBatteryLevel) {
            this.mLowBatteryWarningLevel = this.mCriticalBatteryLevel;
        }
        this.mLowBatteryCloseWarningLevel = this.mLowBatteryWarningLevel + this.mContext.getResources().getInteger(17694804);
        processValuesLocked(true);
    }

    private boolean isPoweredLocked(int plugTypeSet) {
        if (this.mHealthInfo.batteryStatus == 1) {
            return true;
        }
        if ((plugTypeSet & 1) != 0 && this.mHealthInfo.chargerAcOnline) {
            return true;
        }
        if ((plugTypeSet & 2) != 0 && this.mHealthInfo.chargerUsbOnline) {
            return true;
        }
        if ((plugTypeSet & 4) == 0 || !this.mHealthInfo.chargerWirelessOnline) {
            return false;
        }
        return true;
    }

    private boolean shouldSendBatteryLowLocked() {
        boolean plugged = this.mPlugType != 0;
        boolean oldPlugged = this.mLastPlugType != 0;
        if (plugged || this.mHealthInfo.batteryStatus == 1 || this.mHealthInfo.batteryLevel > this.mLowBatteryWarningLevel) {
            return false;
        }
        if (oldPlugged || this.mLastBatteryLevel > this.mLowBatteryWarningLevel) {
            return true;
        }
        return false;
    }

    private void shutdownIfNoPowerLocked() {
        if (this.mHealthInfo.batteryLevel == 0 && !isPoweredLocked(7)) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (BatteryService.this.mActivityManagerInternal.isSystemReady()) {
                        String access$800 = BatteryService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Shutdown because of: batteryLevel =");
                        stringBuilder.append(BatteryService.this.mHealthInfo.batteryLevel);
                        Slog.e(access$800, stringBuilder.toString());
                        Intent intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
                        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                        intent.putExtra("android.intent.extra.REASON", "battery");
                        intent.setFlags(268435456);
                        BatteryService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    }
                }
            });
            if (this.mPendingCheckForShutdown == null) {
                this.mPendingCheckForShutdown = new CheckForShutdown(this, null);
                this.mHandler.postDelayed(this.mPendingCheckForShutdown, 20000);
            }
        } else if (this.mPendingCheckForShutdown != null) {
            this.mHandler.removeCallbacks(this.mPendingCheckForShutdown);
            this.mPendingCheckForShutdown = null;
        }
    }

    private void shutdownIfOverTempLocked() {
        if (this.mHealthInfo.batteryTemperature > this.mShutdownBatteryTemperature) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (BatteryService.this.mActivityManagerInternal.isSystemReady()) {
                        String access$800 = BatteryService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Shutdown because of: batteryTemperature =");
                        stringBuilder.append(BatteryService.this.mHealthInfo.batteryTemperature);
                        Slog.e(access$800, stringBuilder.toString());
                        Intent intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
                        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                        intent.putExtra("android.intent.extra.REASON", "thermal,battery");
                        intent.setFlags(268435456);
                        BatteryService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    }
                }
            });
        }
    }

    private void update(android.hardware.health.V2_0.HealthInfo info) {
        traceBegin("HealthInfoUpdate");
        printBatteryLog(this.mHealthInfo, info, this.mPlugType, this.mUpdatesStopped);
        synchronized (this.mLock) {
            if (this.mUpdatesStopped) {
                copy(this.mLastHealthInfo, info.legacy);
            } else {
                this.mHealthInfo = info.legacy;
                processValuesLocked(false);
                this.mLock.notifyAll();
            }
        }
        traceEnd();
    }

    private static void copy(HealthInfo dst, HealthInfo src) {
        dst.chargerAcOnline = src.chargerAcOnline;
        dst.chargerUsbOnline = src.chargerUsbOnline;
        dst.chargerWirelessOnline = src.chargerWirelessOnline;
        dst.maxChargingCurrent = src.maxChargingCurrent;
        dst.maxChargingVoltage = src.maxChargingVoltage;
        dst.batteryStatus = src.batteryStatus;
        dst.batteryHealth = src.batteryHealth;
        dst.batteryPresent = src.batteryPresent;
        dst.batteryLevel = src.batteryLevel;
        dst.batteryVoltage = src.batteryVoltage;
        dst.batteryTemperature = src.batteryTemperature;
        dst.batteryCurrent = src.batteryCurrent;
        dst.batteryCycleCount = src.batteryCycleCount;
        dst.batteryFullCharge = src.batteryFullCharge;
        dst.batteryChargeCounter = src.batteryChargeCounter;
        dst.batteryTechnology = src.batteryTechnology;
    }

    private void processValuesLocked(boolean force) {
        boolean logOutlier = false;
        long dischargeDuration = 0;
        boolean z = this.mHealthInfo.batteryStatus != 1 && this.mHealthInfo.batteryLevel <= this.mCriticalBatteryLevel;
        this.mBatteryLevelCritical = z;
        if (this.mHealthInfo.chargerAcOnline) {
            this.mPlugType = 1;
        } else if (this.mHealthInfo.chargerUsbOnline) {
            this.mPlugType = 2;
        } else if (this.mHealthInfo.chargerWirelessOnline) {
            this.mPlugType = 4;
        } else {
            this.mPlugType = 0;
        }
        try {
            this.mBatteryStats.setBatteryState(this.mHealthInfo.batteryStatus, this.mHealthInfo.batteryHealth, this.mPlugType, this.mHealthInfo.batteryLevel, this.mHealthInfo.batteryTemperature, this.mHealthInfo.batteryVoltage, this.mHealthInfo.batteryChargeCounter, this.mHealthInfo.batteryFullCharge);
        } catch (RemoteException e) {
        }
        shutdownIfNoPowerLocked();
        shutdownIfOverTempLocked();
        if (!(!force && this.mHealthInfo.batteryStatus == this.mLastBatteryStatus && this.mHealthInfo.batteryHealth == this.mLastBatteryHealth && this.mHealthInfo.batteryPresent == this.mLastBatteryPresent && this.mHealthInfo.batteryLevel == this.mLastBatteryLevel && this.mPlugType == this.mLastPlugType && this.mHealthInfo.batteryVoltage == this.mLastBatteryVoltage && this.mHealthInfo.batteryTemperature == this.mLastBatteryTemperature && this.mHealthInfo.maxChargingCurrent == this.mLastMaxChargingCurrent && this.mHealthInfo.maxChargingVoltage == this.mLastMaxChargingVoltage && this.mHealthInfo.batteryChargeCounter == this.mLastChargeCounter && this.mInvalidCharger == this.mLastInvalidCharger)) {
            final Intent statusIntent;
            if (this.mPlugType != this.mLastPlugType) {
                LogMaker builder;
                long dischargeDuration2;
                if (this.mLastPlugType == 0) {
                    this.mChargeStartLevel = this.mHealthInfo.batteryLevel;
                    this.mChargeStartTime = SystemClock.elapsedRealtime();
                    builder = new LogMaker(1417);
                    builder.setType(4);
                    builder.addTaggedData(1421, Integer.valueOf(this.mPlugType));
                    builder.addTaggedData(1418, Integer.valueOf(this.mHealthInfo.batteryLevel));
                    this.mMetricsLogger.write(builder);
                    if (!(this.mDischargeStartTime == 0 || this.mDischargeStartLevel == this.mHealthInfo.batteryLevel)) {
                        logOutlier = true;
                        EventLog.writeEvent(EventLogTags.BATTERY_DISCHARGE, new Object[]{Long.valueOf(SystemClock.elapsedRealtime() - this.mDischargeStartTime), Integer.valueOf(this.mDischargeStartLevel), Integer.valueOf(this.mHealthInfo.batteryLevel)});
                        this.mDischargeStartTime = 0;
                        dischargeDuration = dischargeDuration2;
                    }
                } else if (this.mPlugType == 0) {
                    this.mDischargeStartTime = SystemClock.elapsedRealtime();
                    this.mDischargeStartLevel = this.mHealthInfo.batteryLevel;
                    dischargeDuration2 = SystemClock.elapsedRealtime() - this.mChargeStartTime;
                    if (!(this.mChargeStartTime == 0 || dischargeDuration2 == 0)) {
                        builder = new LogMaker(1417);
                        builder.setType(5);
                        builder.addTaggedData(1421, Integer.valueOf(this.mLastPlugType));
                        builder.addTaggedData(1420, Long.valueOf(dischargeDuration2));
                        builder.addTaggedData(1418, Integer.valueOf(this.mChargeStartLevel));
                        builder.addTaggedData(1419, Integer.valueOf(this.mHealthInfo.batteryLevel));
                        this.mMetricsLogger.write(builder);
                    }
                    this.mChargeStartTime = 0;
                }
            }
            if (!(this.mHealthInfo.batteryStatus == this.mLastBatteryStatus && this.mHealthInfo.batteryHealth == this.mLastBatteryHealth && this.mHealthInfo.batteryPresent == this.mLastBatteryPresent && this.mPlugType == this.mLastPlugType)) {
                EventLog.writeEvent(EventLogTags.BATTERY_STATUS, new Object[]{Integer.valueOf(this.mHealthInfo.batteryStatus), Integer.valueOf(this.mHealthInfo.batteryHealth), Integer.valueOf(this.mHealthInfo.batteryPresent), Integer.valueOf(this.mPlugType), this.mHealthInfo.batteryTechnology});
            }
            if (this.mHealthInfo.batteryLevel != this.mLastBatteryLevel) {
                if (BatteryManager.HW_BATTERY_LEV_JOB_ALLOWED > 0) {
                    if (this.mHealthInfo.batteryLevel >= BatteryManager.HW_BATTERY_LEV_JOB_ALLOWED && this.mLastBatteryLevel < BatteryManager.HW_BATTERY_LEV_JOB_ALLOWED) {
                        statusIntent = new Intent("com.huawei.intent.action.BATTERY_LEV_JOB_ALLOWED");
                        this.mHandler.post(new Runnable() {
                            public void run() {
                                BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                            }
                        });
                    } else if (this.mHealthInfo.batteryLevel < BatteryManager.HW_BATTERY_LEV_JOB_ALLOWED && this.mLastBatteryLevel >= BatteryManager.HW_BATTERY_LEV_JOB_ALLOWED) {
                        statusIntent = new Intent("com.huawei.intent.action.BATTERY_LEV_JOB_NOT_ALLOWED");
                        this.mHandler.post(new Runnable() {
                            public void run() {
                                BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                            }
                        });
                    }
                }
                EventLog.writeEvent(EventLogTags.BATTERY_LEVEL, new Object[]{Integer.valueOf(this.mHealthInfo.batteryLevel), Integer.valueOf(this.mHealthInfo.batteryVoltage), Integer.valueOf(this.mHealthInfo.batteryTemperature)});
            }
            if (this.mBatteryLevelCritical && !this.mLastBatteryLevelCritical && this.mPlugType == 0) {
                logOutlier = true;
                dischargeDuration = SystemClock.elapsedRealtime() - this.mDischargeStartTime;
            }
            if (this.mBatteryLevelLow) {
                if (this.mPlugType != 0) {
                    this.mBatteryLevelLow = false;
                } else if (this.mHealthInfo.batteryLevel >= this.mLowBatteryCloseWarningLevel) {
                    this.mBatteryLevelLow = false;
                } else if (force && this.mHealthInfo.batteryLevel >= this.mLowBatteryWarningLevel) {
                    this.mBatteryLevelLow = false;
                }
            } else if (this.mPlugType == 0 && this.mHealthInfo.batteryStatus != 1 && this.mHealthInfo.batteryLevel <= this.mLowBatteryWarningLevel) {
                this.mBatteryLevelLow = true;
            }
            this.mSequence++;
            if (this.mPlugType != 0 && this.mLastPlugType == 0) {
                statusIntent = new Intent("android.intent.action.ACTION_POWER_CONNECTED");
                statusIntent.setFlags(67108864);
                statusIntent.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() {
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                    }
                });
                playRing();
                boolean isWirelessCharge = isWirelessCharge();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("power connected, isWirelessCharge:");
                stringBuilder.append(isWirelessCharge);
                Slog.d(str, stringBuilder.toString());
                if (isWirelessCharge) {
                    new SystemVibrator(this.mContext).vibrate(300);
                } else if (HwVibrator.isSupportHwVibrator("haptic.battery.charging")) {
                    HwVibrator.setHwVibrator(Process.myUid(), this.mContext.getPackageName(), "haptic.battery.charging");
                }
            } else if (this.mPlugType == 0 && this.mLastPlugType != 0) {
                statusIntent = new Intent("android.intent.action.ACTION_POWER_DISCONNECTED");
                statusIntent.setFlags(67108864);
                statusIntent.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() {
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                    }
                });
                stopRing();
            }
            if (shouldSendBatteryLowLocked()) {
                this.mSentLowBatteryBroadcast = true;
                statusIntent = new Intent("android.intent.action.BATTERY_LOW");
                statusIntent.setFlags(67108864);
                statusIntent.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() {
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                    }
                });
            } else if (this.mSentLowBatteryBroadcast && this.mHealthInfo.batteryLevel >= this.mLowBatteryCloseWarningLevel) {
                this.mSentLowBatteryBroadcast = false;
                statusIntent = new Intent("android.intent.action.BATTERY_OKAY");
                statusIntent.setFlags(67108864);
                statusIntent.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() {
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                    }
                });
            }
            if (!(force || this.mAms == null)) {
                AbsHwMtmBroadcastResourceManager mHwMtmBRManager = this.mAms.getBgBroadcastQueue().getMtmBRManager();
                if (mHwMtmBRManager != null) {
                    if (mHwMtmBRManager.iawareNeedSkipBroadcastSend("android.intent.action.BATTERY_CHANGED", new Object[]{this.mHealthInfo, Integer.valueOf(this.mLastBatteryStatus), Integer.valueOf(this.mLastBatteryHealth), Boolean.valueOf(this.mLastBatteryPresent), Integer.valueOf(this.mLastBatteryLevel), Integer.valueOf(this.mPlugType), Integer.valueOf(this.mLastPlugType), Integer.valueOf(this.mLastBatteryVoltage), Integer.valueOf(this.mLastBatteryTemperature), Integer.valueOf(this.mLastMaxChargingCurrent), Integer.valueOf(this.mLastMaxChargingVoltage), Integer.valueOf(this.mLastChargeCounter), Integer.valueOf(this.mInvalidCharger), Integer.valueOf(this.mLastInvalidCharger)})) {
                        return;
                    }
                }
            }
            sendBatteryChangedIntentLocked();
            if (this.mLastBatteryLevel != this.mHealthInfo.batteryLevel) {
                sendBatteryLevelChangedIntentLocked();
            }
            updateLight();
            if (logOutlier && dischargeDuration != 0) {
                logOutlierLocked(dischargeDuration);
            }
            this.mLastBatteryStatus = this.mHealthInfo.batteryStatus;
            this.mLastBatteryHealth = this.mHealthInfo.batteryHealth;
            this.mLastBatteryPresent = this.mHealthInfo.batteryPresent;
            this.mLastBatteryLevel = this.mHealthInfo.batteryLevel;
            this.mLastPlugType = this.mPlugType;
            this.mLastBatteryVoltage = this.mHealthInfo.batteryVoltage;
            this.mLastBatteryTemperature = this.mHealthInfo.batteryTemperature;
            this.mLastMaxChargingCurrent = this.mHealthInfo.maxChargingCurrent;
            this.mLastMaxChargingVoltage = this.mHealthInfo.maxChargingVoltage;
            this.mLastChargeCounter = this.mHealthInfo.batteryChargeCounter;
            this.mLastBatteryLevelCritical = this.mBatteryLevelCritical;
            this.mLastInvalidCharger = this.mInvalidCharger;
        }
    }

    private void sendBatteryChangedIntentLocked() {
        Intent intent = new Intent("android.intent.action.BATTERY_CHANGED");
        intent.addFlags(1610612736);
        int icon = getIconLocked(this.mHealthInfo.batteryLevel);
        String status = isWirelessCharge() ? CHARGE_WIRELESS_STATUS : CHARGE_WIRED_STATUS;
        intent.putExtra(CHARGE_STATUS, status);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("normal charge status : ");
        stringBuilder.append(status);
        Slog.i(str, stringBuilder.toString());
        intent.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
        intent.putExtra("status", this.mHealthInfo.batteryStatus);
        intent.putExtra("health", this.mHealthInfo.batteryHealth);
        intent.putExtra("present", this.mHealthInfo.batteryPresent);
        intent.putExtra("level", this.mHealthInfo.batteryLevel);
        intent.putExtra("battery_low", this.mSentLowBatteryBroadcast);
        intent.putExtra("scale", 100);
        intent.putExtra("icon-small", icon);
        intent.putExtra("plugged", this.mPlugType);
        intent.putExtra("voltage", this.mHealthInfo.batteryVoltage);
        intent.putExtra("temperature", this.mHealthInfo.batteryTemperature);
        intent.putExtra("technology", this.mHealthInfo.batteryTechnology);
        intent.putExtra("invalid_charger", this.mInvalidCharger);
        intent.putExtra("max_charging_current", this.mHealthInfo.maxChargingCurrent);
        intent.putExtra("max_charging_voltage", this.mHealthInfo.maxChargingVoltage);
        intent.putExtra("charge_counter", this.mHealthInfo.batteryChargeCounter);
        this.mHandler.post(new -$$Lambda$BatteryService$2x73lvpB0jctMSVP4qb9sHAqRPw(intent));
    }

    private void sendBatteryLevelChangedIntentLocked() {
        Bundle event = new Bundle();
        long now = SystemClock.elapsedRealtime();
        event.putInt(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
        event.putInt("status", this.mHealthInfo.batteryStatus);
        event.putInt("health", this.mHealthInfo.batteryHealth);
        event.putBoolean("present", this.mHealthInfo.batteryPresent);
        event.putInt("level", this.mHealthInfo.batteryLevel);
        event.putBoolean("battery_low", this.mSentLowBatteryBroadcast);
        event.putInt("scale", 100);
        event.putInt("plugged", this.mPlugType);
        event.putInt("voltage", this.mHealthInfo.batteryVoltage);
        event.putLong("android.os.extra.EVENT_TIMESTAMP", now);
        boolean queueWasEmpty = this.mBatteryLevelsEventQueue.isEmpty();
        this.mBatteryLevelsEventQueue.add(event);
        if (this.mBatteryLevelsEventQueue.size() > 100) {
            this.mBatteryLevelsEventQueue.removeFirst();
        }
        if (queueWasEmpty) {
            this.mHandler.postDelayed(new -$$Lambda$BatteryService$D1kwd7L7yyqN5niz3KWkTepVmUk(this), now - this.mLastBatteryLevelChangedSentMs > 60000 ? 0 : (this.mLastBatteryLevelChangedSentMs + 60000) - now);
        }
    }

    private void sendEnqueuedBatteryLevelChangedEvents() {
        ArrayList<Bundle> events;
        synchronized (this.mLock) {
            events = new ArrayList(this.mBatteryLevelsEventQueue);
            this.mBatteryLevelsEventQueue.clear();
        }
        Intent intent = new Intent("android.intent.action.BATTERY_LEVEL_CHANGED");
        intent.addFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        intent.putParcelableArrayListExtra("android.os.extra.EVENTS", events);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.BATTERY_STATS");
        this.mLastBatteryLevelChangedSentMs = SystemClock.elapsedRealtime();
    }

    protected boolean isWirelessCharge() {
        try {
            return "11".equals(FileUtils.readTextFile(new File(CHARGER_TYPE), 0, null).trim());
        } catch (IOException e) {
            Slog.e(TAG, "Error occurs when read sys/class/hw_power/charger/charge_data/chargerType", e);
            return false;
        }
    }

    private int alterWirelessTxSwitch(int status) {
        return alterWirelessTxSwitchInternal(status);
    }

    private int getWirelessTxSwitch() {
        return getWirelessTxSwitchInternal();
    }

    private boolean supportWirelessTxCharge() {
        return supportWirelessTxChargeInternal();
    }

    private void logBatteryStatsLocked() {
        IBinder batteryInfoService = ServiceManager.getService("batterystats");
        if (batteryInfoService != null) {
            DropBoxManager db = (DropBoxManager) this.mContext.getSystemService("dropbox");
            if (db != null && db.isTagEnabled("BATTERY_DISCHARGE_INFO")) {
                File dumpFile = null;
                FileOutputStream dumpStream = null;
                String str;
                StringBuilder stringBuilder;
                try {
                    dumpFile = new File("/data/system/batterystats.dump");
                    dumpStream = new FileOutputStream(dumpFile);
                    batteryInfoService.dump(dumpStream.getFD(), DUMPSYS_ARGS);
                    FileUtils.sync(dumpStream);
                    db.addFile("BATTERY_DISCHARGE_INFO", dumpFile, 2);
                    try {
                        dumpStream.close();
                    } catch (IOException e) {
                        Slog.e(TAG, "failed to close dumpsys output stream");
                    }
                    if (!dumpFile.delete()) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("failed to delete temporary dumpsys file: ");
                        stringBuilder.append(dumpFile.getAbsolutePath());
                        Slog.e(str, stringBuilder.toString());
                    }
                } catch (RemoteException e2) {
                    Slog.e(TAG, "failed to dump battery service", e2);
                    if (dumpStream != null) {
                        try {
                            dumpStream.close();
                        } catch (IOException e3) {
                            Slog.e(TAG, "failed to close dumpsys output stream");
                        }
                    }
                    if (!(dumpFile == null || dumpFile.delete())) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                } catch (IOException e4) {
                    Slog.e(TAG, "failed to write dumpsys file", e4);
                    if (dumpStream != null) {
                        try {
                            dumpStream.close();
                        } catch (IOException e5) {
                            Slog.e(TAG, "failed to close dumpsys output stream");
                        }
                    }
                    if (!(dumpFile == null || dumpFile.delete())) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                } catch (Throwable th) {
                    if (dumpStream != null) {
                        try {
                            dumpStream.close();
                        } catch (IOException e6) {
                            Slog.e(TAG, "failed to close dumpsys output stream");
                        }
                    }
                    if (!(dumpFile == null || dumpFile.delete())) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("failed to delete temporary dumpsys file: ");
                        stringBuilder2.append(dumpFile.getAbsolutePath());
                        Slog.e(str2, stringBuilder2.toString());
                    }
                }
            }
        }
    }

    private void logOutlierLocked(long duration) {
        ContentResolver cr = this.mContext.getContentResolver();
        String dischargeThresholdString = Global.getString(cr, "battery_discharge_threshold");
        String durationThresholdString = Global.getString(cr, "battery_discharge_duration_threshold");
        if (dischargeThresholdString != null && durationThresholdString != null) {
            try {
                long durationThreshold = Long.parseLong(durationThresholdString);
                int dischargeThreshold = Integer.parseInt(dischargeThresholdString);
                if (duration <= durationThreshold && this.mDischargeStartLevel - this.mHealthInfo.batteryLevel >= dischargeThreshold) {
                    logBatteryStatsLocked();
                }
            } catch (NumberFormatException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid DischargeThresholds GService string: ");
                stringBuilder.append(durationThresholdString);
                stringBuilder.append(" or ");
                stringBuilder.append(dischargeThresholdString);
                Slog.e(str, stringBuilder.toString());
            }
        }
    }

    private int getIconLocked(int level) {
        if (this.mHealthInfo.batteryStatus == 2) {
            return 17303498;
        }
        if (this.mHealthInfo.batteryStatus == 3) {
            return 17303484;
        }
        if (this.mHealthInfo.batteryStatus != 4 && this.mHealthInfo.batteryStatus != 5) {
            return 17303512;
        }
        if (!isPoweredLocked(7) || this.mHealthInfo.batteryLevel < 100) {
            return 17303484;
        }
        return 17303498;
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Battery service (battery) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  set [-f] [ac|usb|wireless|status|level|temp|present|invalid] <value>");
        pw.println("    Force a battery property value, freezing battery state.");
        pw.println("    -f: force a battery change broadcast be sent, prints new sequence.");
        pw.println("  unplug [-f]");
        pw.println("    Force battery unplugged, freezing battery state.");
        pw.println("    -f: force a battery change broadcast be sent, prints new sequence.");
        pw.println("  reset [-f]");
        pw.println("    Unfreeze battery state, returning to current hardware values.");
        pw.println("    -f: force a battery change broadcast be sent, prints new sequence.");
    }

    int parseOptions(Shell shell) {
        int opts = 0;
        while (true) {
            String nextOption = shell.getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                return opts;
            }
            if ("-f".equals(opt)) {
                opts |= 1;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x0049  */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x01c0  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x007a  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x004e  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0049  */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x01c0  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x007a  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x004e  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0049  */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x01c0  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x007a  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x004e  */
    /* JADX WARNING: Missing block: B:58:0x00e5, code:
            if (r2.equals("usb") != false) goto L_0x0114;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int onShellCommand(Shell shell, String cmd) {
        long ident;
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        boolean z;
        PrintWriter pw = shell.getOutPrintWriter();
        int hashCode = cmd.hashCode();
        boolean z2 = true;
        if (hashCode != -840325209) {
            if (hashCode != 113762) {
                if (hashCode == 108404047 && cmd.equals("reset")) {
                    z = true;
                    switch (z) {
                        case false:
                            hashCode = parseOptions(shell);
                            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                            if (!this.mUpdatesStopped) {
                                copy(this.mLastHealthInfo, this.mHealthInfo);
                            }
                            this.mHealthInfo.chargerAcOnline = false;
                            this.mHealthInfo.chargerUsbOnline = false;
                            this.mHealthInfo.chargerWirelessOnline = false;
                            ident = Binder.clearCallingIdentity();
                            try {
                                this.mUpdatesStopped = true;
                                processValuesFromShellLocked(pw, hashCode);
                                break;
                            } finally {
                                Binder.restoreCallingIdentity(ident);
                            }
                        case true:
                            hashCode = parseOptions(shell);
                            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                            String key = shell.getNextArg();
                            if (key == null) {
                                pw.println("No property specified");
                                return -1;
                            }
                            String value = shell.getNextArg();
                            if (value == null) {
                                pw.println("No value specified");
                                return -1;
                            }
                            long ident2;
                            try {
                                if (!this.mUpdatesStopped) {
                                    copy(this.mLastHealthInfo, this.mHealthInfo);
                                }
                                boolean update = true;
                                switch (key.hashCode()) {
                                    case -1000044642:
                                        if (key.equals("wireless")) {
                                            z2 = true;
                                            break;
                                        }
                                    case -892481550:
                                        if (key.equals("status")) {
                                            z2 = true;
                                            break;
                                        }
                                    case -318277445:
                                        if (key.equals("present")) {
                                            z2 = false;
                                            break;
                                        }
                                    case 3106:
                                        if (key.equals("ac")) {
                                            z2 = true;
                                            break;
                                        }
                                    case 116100:
                                        break;
                                    case 3556308:
                                        if (key.equals("temp")) {
                                            z2 = true;
                                            break;
                                        }
                                    case 102865796:
                                        if (key.equals("level")) {
                                            z2 = true;
                                            break;
                                        }
                                    case 957830652:
                                        if (key.equals("counter")) {
                                            z2 = true;
                                            break;
                                        }
                                    case 1959784951:
                                        if (key.equals("invalid")) {
                                            z2 = true;
                                            break;
                                        }
                                    default:
                                        z2 = true;
                                        break;
                                }
                                switch (z2) {
                                    case false:
                                        this.mHealthInfo.batteryPresent = Integer.parseInt(value) != 0;
                                        break;
                                    case true:
                                        this.mHealthInfo.chargerAcOnline = Integer.parseInt(value) != 0;
                                        break;
                                    case true:
                                        this.mHealthInfo.chargerUsbOnline = Integer.parseInt(value) != 0;
                                        break;
                                    case true:
                                        this.mHealthInfo.chargerWirelessOnline = Integer.parseInt(value) != 0;
                                        break;
                                    case true:
                                        this.mHealthInfo.batteryStatus = Integer.parseInt(value);
                                        break;
                                    case true:
                                        this.mHealthInfo.batteryLevel = Integer.parseInt(value);
                                        break;
                                    case true:
                                        this.mHealthInfo.batteryChargeCounter = Integer.parseInt(value);
                                        break;
                                    case true:
                                        this.mHealthInfo.batteryTemperature = Integer.parseInt(value);
                                        break;
                                    case true:
                                        this.mInvalidCharger = Integer.parseInt(value);
                                        break;
                                    default:
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Unknown set option: ");
                                        stringBuilder.append(key);
                                        pw.println(stringBuilder.toString());
                                        update = false;
                                        break;
                                }
                                if (update) {
                                    ident2 = Binder.clearCallingIdentity();
                                    this.mUpdatesStopped = true;
                                    processValuesFromShellLocked(pw, hashCode);
                                    Binder.restoreCallingIdentity(ident2);
                                    break;
                                }
                            } catch (NumberFormatException e) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Bad value: ");
                                stringBuilder2.append(value);
                                pw.println(stringBuilder2.toString());
                                return -1;
                            } catch (Throwable th) {
                                Binder.restoreCallingIdentity(ident2);
                            }
                            break;
                        case true:
                            hashCode = parseOptions(shell);
                            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                            ident = Binder.clearCallingIdentity();
                            try {
                                if (this.mUpdatesStopped) {
                                    this.mUpdatesStopped = false;
                                    copy(this.mHealthInfo, this.mLastHealthInfo);
                                    processValuesFromShellLocked(pw, hashCode);
                                }
                                Binder.restoreCallingIdentity(ident);
                                break;
                            } catch (Throwable th2) {
                                Binder.restoreCallingIdentity(ident);
                            }
                        default:
                            return shell.handleDefaultCommands(cmd);
                    }
                    return 0;
                }
            } else if (cmd.equals("set")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return 0;
            }
        } else if (cmd.equals("unplug")) {
            z = false;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return 0;
        }
        z = true;
        switch (z) {
            case false:
                break;
            case true:
                break;
            case true:
                break;
            default:
                break;
        }
        return 0;
    }

    private void processValuesFromShellLocked(PrintWriter pw, int opts) {
        processValuesLocked((opts & 1) != 0);
        if ((opts & 1) != 0) {
            pw.println(this.mSequence);
        }
    }

    private void dumpInternal(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mLock) {
            if (args != null) {
                if (args.length != 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("dumpInternal args[0]: ");
                    stringBuilder.append(args[0]);
                    stringBuilder.append(" mUpdatesStopped: ");
                    stringBuilder.append(this.mUpdatesStopped);
                    Flog.i(NativeResponseCode.SERVICE_REGISTRATION_FAILED, stringBuilder.toString());
                }
            }
            if (args == null || args.length == 0 || "-a".equals(args[0])) {
                pw.println("Current Battery Service state:");
                if (this.mUpdatesStopped) {
                    pw.println("  (UPDATES STOPPED -- use 'reset' to restart)");
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  AC powered: ");
                stringBuilder2.append(this.mHealthInfo.chargerAcOnline);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  USB powered: ");
                stringBuilder2.append(this.mHealthInfo.chargerUsbOnline);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  Wireless powered: ");
                stringBuilder2.append(this.mHealthInfo.chargerWirelessOnline);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  Max charging current: ");
                stringBuilder2.append(this.mHealthInfo.maxChargingCurrent);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  Max charging voltage: ");
                stringBuilder2.append(this.mHealthInfo.maxChargingVoltage);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  Charge counter: ");
                stringBuilder2.append(this.mHealthInfo.batteryChargeCounter);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  status: ");
                stringBuilder2.append(this.mHealthInfo.batteryStatus);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  health: ");
                stringBuilder2.append(this.mHealthInfo.batteryHealth);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  present: ");
                stringBuilder2.append(this.mHealthInfo.batteryPresent);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  level: ");
                stringBuilder2.append(this.mHealthInfo.batteryLevel);
                pw.println(stringBuilder2.toString());
                pw.println("  scale: 100");
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  voltage: ");
                stringBuilder2.append(this.mHealthInfo.batteryVoltage);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  temperature: ");
                stringBuilder2.append(this.mHealthInfo.batteryTemperature);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  technology: ");
                stringBuilder2.append(this.mHealthInfo.batteryTechnology);
                pw.println(stringBuilder2.toString());
            } else {
                new Shell().exec(this.mBinderService, null, fd, null, args, null, new ResultReceiver(null));
            }
        }
    }

    private void dumpProto(FileDescriptor fd) {
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (this.mLock) {
            proto.write(1133871366145L, this.mUpdatesStopped);
            int batteryPluggedValue = 0;
            if (this.mHealthInfo.chargerAcOnline) {
                batteryPluggedValue = 1;
            } else if (this.mHealthInfo.chargerUsbOnline) {
                batteryPluggedValue = 2;
            } else if (this.mHealthInfo.chargerWirelessOnline) {
                batteryPluggedValue = 4;
            }
            proto.write(1159641169922L, batteryPluggedValue);
            proto.write(1120986464259L, this.mHealthInfo.maxChargingCurrent);
            proto.write(1120986464260L, this.mHealthInfo.maxChargingVoltage);
            proto.write(1120986464261L, this.mHealthInfo.batteryChargeCounter);
            proto.write(1159641169926L, this.mHealthInfo.batteryStatus);
            proto.write(1159641169927L, this.mHealthInfo.batteryHealth);
            proto.write(1133871366152L, this.mHealthInfo.batteryPresent);
            proto.write(1120986464265L, this.mHealthInfo.batteryLevel);
            proto.write(1120986464266L, 100);
            proto.write(1120986464267L, this.mHealthInfo.batteryVoltage);
            proto.write(1120986464268L, this.mHealthInfo.batteryTemperature);
            proto.write(1138166333453L, this.mHealthInfo.batteryTechnology);
        }
        proto.flush();
    }

    private static void traceBegin(String name) {
        Trace.traceBegin(524288, name);
    }

    private static void traceEnd() {
        Trace.traceEnd(524288);
    }

    protected HealthInfo getHealthInfo() {
        return this.mHealthInfo;
    }

    protected int getLowBatteryWarningLevel() {
        return this.mLowBatteryWarningLevel;
    }

    protected void updateLight() {
        this.mLed.updateLightsLocked();
    }

    protected void cameraUpdateLight(boolean enable) {
    }
}
