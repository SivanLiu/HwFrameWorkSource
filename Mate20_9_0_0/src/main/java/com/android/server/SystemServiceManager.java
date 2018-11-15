package com.android.server;

import android.content.Context;
import android.os.Binder;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Slog;
import com.android.server.os.HwBootFail;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class SystemServiceManager {
    private static final int SERVICE_CALL_WARN_TIME_MS = 50;
    private static final String TAG = "SystemServiceManager";
    private final Context mContext;
    private int mCurrentPhase = -1;
    private boolean mRuntimeRestarted;
    private long mRuntimeStartElapsedTime;
    private long mRuntimeStartUptime;
    private boolean mSafeMode;
    private final ArrayList<SystemService> mServices = new ArrayList();

    SystemServiceManager(Context context) {
        this.mContext = context;
    }

    public SystemService startService(String className) {
        try {
            return startService(Class.forName(className));
        } catch (ClassNotFoundException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Starting ");
            stringBuilder.append(className);
            Slog.i(TAG, stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to create service ");
            stringBuilder2.append(className);
            stringBuilder2.append(": service class not found, usually indicates that the caller should have called PackageManager.hasSystemFeature() to check whether the feature is available on this device before trying to start the services that implement it");
            throw new RuntimeException(stringBuilder2.toString(), ex);
        }
    }

    public <T extends SystemService> T startService(Class<T> serviceClass) {
        StringBuilder stringBuilder;
        String name;
        try {
            name = serviceClass.getName();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("StartService ");
            stringBuilder2.append(name);
            Trace.traceBegin(524288, stringBuilder2.toString());
            if (SystemService.class.isAssignableFrom(serviceClass)) {
                SystemService service = (SystemService) serviceClass.getConstructor(new Class[]{Context.class}).newInstance(new Object[]{this.mContext});
                startService(service);
                Trace.traceEnd(524288);
                return service;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Failed to create ");
            stringBuilder3.append(name);
            stringBuilder3.append(": service must extend ");
            stringBuilder3.append(SystemService.class.getName());
            throw new RuntimeException(stringBuilder3.toString());
        } catch (InstantiationException ex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to create service ");
            stringBuilder.append(name);
            stringBuilder.append(": service could not be instantiated");
            throw new RuntimeException(stringBuilder.toString(), ex);
        } catch (IllegalAccessException ex2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to create service ");
            stringBuilder.append(name);
            stringBuilder.append(": service must have a public constructor with a Context argument");
            throw new RuntimeException(stringBuilder.toString(), ex2);
        } catch (NoSuchMethodException ex3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to create service ");
            stringBuilder.append(name);
            stringBuilder.append(": service must have a public constructor with a Context argument");
            throw new RuntimeException(stringBuilder.toString(), ex3);
        } catch (InvocationTargetException ex4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to create service ");
            stringBuilder.append(name);
            stringBuilder.append(": service constructor threw an exception");
            throw new RuntimeException(stringBuilder.toString(), ex4);
        } catch (Throwable th) {
            Trace.traceEnd(524288);
        }
    }

    public void startService(SystemService service) {
        synchronized (this.mServices) {
            this.mServices.add(service);
        }
        long time = SystemClock.elapsedRealtime();
        try {
            service.onStart();
            warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onStart");
        } catch (RuntimeException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to start service ");
            stringBuilder.append(service.getClass().getName());
            stringBuilder.append(": onStart threw an exception");
            throw new RuntimeException(stringBuilder.toString(), ex);
        }
    }

    public void startBootPhase(int phase) {
        if (phase > this.mCurrentPhase) {
            this.mCurrentPhase = phase;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Starting phase ");
            stringBuilder.append(this.mCurrentPhase);
            Slog.i(str, stringBuilder.toString());
            int i;
            SystemService service;
            try {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("OnBootPhase ");
                stringBuilder2.append(phase);
                Trace.traceBegin(524288, stringBuilder2.toString());
                HwBootFail.setBootStage(HwBootFail.changeBootStage(phase));
                int serviceLen = this.mServices.size();
                i = 0;
                while (i < serviceLen) {
                    service = (SystemService) this.mServices.get(i);
                    long time = SystemClock.elapsedRealtime();
                    Trace.traceBegin(524288, service.getClass().getName());
                    service.onBootPhase(this.mCurrentPhase);
                    warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onBootPhase");
                    Trace.traceEnd(524288);
                    i++;
                }
                Trace.traceEnd(524288);
            } catch (Exception ex) {
                String stringBuilder3;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Failed to boot service ");
                if (service == null) {
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("null at index ");
                    stringBuilder5.append(i);
                    stringBuilder3 = stringBuilder5.toString();
                } else {
                    stringBuilder3 = service.getClass().getName();
                }
                stringBuilder4.append(stringBuilder3);
                stringBuilder4.append(": onBootPhase threw an exception during phase ");
                stringBuilder4.append(this.mCurrentPhase);
                throw new RuntimeException(stringBuilder4.toString(), ex);
            } catch (Throwable th) {
                Trace.traceEnd(524288);
            }
        } else {
            throw new IllegalArgumentException("Next phase must be larger than previous");
        }
    }

    public boolean isBootCompleted() {
        return this.mCurrentPhase >= 1000;
    }

    public void startUser(int userHandle) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Calling onStartUser u");
        stringBuilder.append(userHandle);
        Slog.i(str, stringBuilder.toString());
        int serviceLen = this.mServices.size();
        for (int i = 0; i < serviceLen; i++) {
            SystemService service = (SystemService) this.mServices.get(i);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onStartUser ");
            stringBuilder2.append(service.getClass().getName());
            Trace.traceBegin(524288, stringBuilder2.toString());
            long time = SystemClock.elapsedRealtime();
            try {
                service.onStartUser(userHandle);
            } catch (Exception ex) {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Failure reporting start of user ");
                stringBuilder3.append(userHandle);
                stringBuilder3.append(" to service ");
                stringBuilder3.append(service.getClass().getName());
                Slog.wtf(str2, stringBuilder3.toString(), ex);
            }
            warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onStartUser ");
            Trace.traceEnd(524288);
        }
    }

    public void unlockUser(int userHandle) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Calling onUnlockUser u");
        stringBuilder.append(userHandle);
        Slog.i(str, stringBuilder.toString());
        int serviceLen = this.mServices.size();
        for (int i = 0; i < serviceLen; i++) {
            SystemService service = (SystemService) this.mServices.get(i);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onUnlockUser ");
            stringBuilder2.append(service.getClass().getName());
            Trace.traceBegin(524288, stringBuilder2.toString());
            long time = SystemClock.elapsedRealtime();
            try {
                service.onUnlockUser(userHandle);
            } catch (Exception ex) {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Failure reporting unlock of user ");
                stringBuilder3.append(userHandle);
                stringBuilder3.append(" to service ");
                stringBuilder3.append(service.getClass().getName());
                Slog.wtf(str2, stringBuilder3.toString(), ex);
            }
            warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onUnlockUser ");
            Trace.traceEnd(524288);
        }
    }

    public void switchUser(int userHandle) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Calling switchUser u");
        stringBuilder.append(userHandle);
        Slog.i(str, stringBuilder.toString());
        int serviceLen = this.mServices.size();
        for (int i = 0; i < serviceLen; i++) {
            SystemService service = (SystemService) this.mServices.get(i);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onSwitchUser ");
            stringBuilder2.append(service.getClass().getName());
            Trace.traceBegin(524288, stringBuilder2.toString());
            long time = SystemClock.elapsedRealtime();
            try {
                service.onSwitchUser(userHandle);
            } catch (Exception ex) {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Failure reporting switch of user ");
                stringBuilder3.append(userHandle);
                stringBuilder3.append(" to service ");
                stringBuilder3.append(service.getClass().getName());
                Slog.wtf(str2, stringBuilder3.toString(), ex);
            }
            warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onSwitchUser");
            Trace.traceEnd(524288);
        }
    }

    public void stopUser(int userHandle) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Calling onStopUser u");
        stringBuilder.append(userHandle);
        Slog.i(str, stringBuilder.toString());
        int serviceLen = this.mServices.size();
        for (int i = 0; i < serviceLen; i++) {
            SystemService service = (SystemService) this.mServices.get(i);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onStopUser ");
            stringBuilder2.append(service.getClass().getName());
            Trace.traceBegin(524288, stringBuilder2.toString());
            long time = SystemClock.elapsedRealtime();
            try {
                service.onStopUser(userHandle);
            } catch (Exception ex) {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Failure reporting stop of user ");
                stringBuilder3.append(userHandle);
                stringBuilder3.append(" to service ");
                stringBuilder3.append(service.getClass().getName());
                Slog.wtf(str2, stringBuilder3.toString(), ex);
            }
            warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onStopUser");
            Trace.traceEnd(524288);
        }
    }

    public void cleanupUser(int userHandle) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Calling onCleanupUser u");
        stringBuilder.append(userHandle);
        Slog.i(str, stringBuilder.toString());
        int serviceLen = this.mServices.size();
        for (int i = 0; i < serviceLen; i++) {
            SystemService service = (SystemService) this.mServices.get(i);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onCleanupUser ");
            stringBuilder2.append(service.getClass().getName());
            Trace.traceBegin(524288, stringBuilder2.toString());
            long time = SystemClock.elapsedRealtime();
            try {
                service.onCleanupUser(userHandle);
            } catch (Exception ex) {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Failure reporting cleanup of user ");
                stringBuilder3.append(userHandle);
                stringBuilder3.append(" to service ");
                stringBuilder3.append(service.getClass().getName());
                Slog.wtf(str2, stringBuilder3.toString(), ex);
            }
            warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onCleanupUser");
            Trace.traceEnd(524288);
        }
    }

    void setSafeMode(boolean safeMode) {
        this.mSafeMode = safeMode;
    }

    public boolean isSafeMode() {
        return this.mSafeMode;
    }

    public boolean isRuntimeRestarted() {
        return this.mRuntimeRestarted;
    }

    public long getRuntimeStartElapsedTime() {
        return this.mRuntimeStartElapsedTime;
    }

    public long getRuntimeStartUptime() {
        return this.mRuntimeStartUptime;
    }

    void setStartInfo(boolean runtimeRestarted, long runtimeStartElapsedTime, long runtimeStartUptime) {
        this.mRuntimeRestarted = runtimeRestarted;
        this.mRuntimeStartElapsedTime = runtimeStartElapsedTime;
        this.mRuntimeStartUptime = runtimeStartUptime;
    }

    private void warnIfTooLong(long duration, SystemService service, String operation) {
        if (duration > 50) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Service ");
            stringBuilder.append(service.getClass().getName());
            stringBuilder.append(" took ");
            stringBuilder.append(duration);
            stringBuilder.append(" ms in ");
            stringBuilder.append(operation);
            Slog.w(str, stringBuilder.toString());
        }
    }

    public void dump() {
        StringBuilder builder = new StringBuilder();
        builder.append("Current phase: ");
        builder.append(this.mCurrentPhase);
        builder.append("\n");
        builder.append("Services:\n");
        int startedLen = this.mServices.size();
        for (int i = 0; i < startedLen; i++) {
            SystemService service = (SystemService) this.mServices.get(i);
            builder.append("\t");
            builder.append(service.getClass().getSimpleName());
            builder.append("\n");
        }
        Slog.e(TAG, builder.toString());
    }

    public String dumpInfo() {
        if (1000 != Binder.getCallingUid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Slog.e(str, stringBuilder.toString());
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Current phase: ");
        builder.append(this.mCurrentPhase);
        builder.append("\n");
        builder.append("Services:\n");
        int startedLen = this.mServices.size();
        for (int i = 0; i < startedLen; i++) {
            SystemService service = (SystemService) this.mServices.get(i);
            builder.append("\t");
            builder.append(service.getClass().getSimpleName());
            builder.append("\n");
        }
        return builder.toString();
    }
}
