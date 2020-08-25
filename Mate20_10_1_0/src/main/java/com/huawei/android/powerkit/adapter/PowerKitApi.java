package com.huawei.android.powerkit.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import com.huawei.android.powerkit.PowerKitConnection;
import com.huawei.android.powerkit.Sink;
import com.huawei.android.powerkit.adapter.IPowerKitApi;
import com.huawei.android.powerkit.adapter.IStateSink;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class PowerKitApi implements IBinder.DeathRecipient {
    private static final String TAG = "PowerKitApi";
    private static final int TIME_THRESHOLD = 300000;
    ServiceConnection mConnection;
    private Context mContext;
    private final ArrayList<Integer> mEnabledStates;
    /* access modifiers changed from: private */
    public PowerKitConnection mKitConnection;
    /* access modifiers changed from: private */
    public final Object mLock;
    /* access modifiers changed from: private */
    public PowerKitApi mMe;
    /* access modifiers changed from: private */
    public IPowerKitApi mService;
    /* access modifiers changed from: private */
    public final HashSet<Sink> mSinkSet;
    /* access modifiers changed from: private */
    public final HashMap<Sink, ArrayList<Integer>> mSinkSetStates;
    private final SinkTransport mSinkTransport;

    public PowerKitApi(Context context, PowerKitConnection pkConnection) {
        this.mMe = null;
        this.mKitConnection = null;
        this.mService = null;
        this.mConnection = new ServiceConnection() {
            /* class com.huawei.android.powerkit.adapter.PowerKitApi.AnonymousClass1 */

            public void onServiceDisconnected(ComponentName name) {
                Log.v(PowerKitApi.TAG, "Powerkit service disconnected");
                PowerKitApi.this.mKitConnection.onServiceDisconnected();
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                IPowerKitApi unused = PowerKitApi.this.mService = IPowerKitApi.Stub.asInterface(service);
                Log.v(PowerKitApi.TAG, "Powerkit service connected");
                PowerKitApi.this.mKitConnection.onServiceConnected();
                try {
                    PowerKitApi.this.mService.asBinder().linkToDeath(PowerKitApi.this.mMe, 0);
                } catch (Exception e) {
                    Log.w(PowerKitApi.TAG, "powerkit linkToDeath failed ! calling pid: " + Binder.getCallingPid());
                }
            }
        };
        this.mSinkSet = new HashSet<>();
        this.mEnabledStates = new ArrayList<>();
        this.mSinkSetStates = new HashMap<>();
        this.mLock = new Object();
        this.mSinkTransport = new SinkTransport();
        this.mMe = this;
        this.mKitConnection = pkConnection;
        this.mContext = context;
        bindPowerKitService();
    }

    private boolean bindPowerKitService() {
        boolean ret = this.mContext.getApplicationContext().bindService(createExplicitIntent(this.mContext, new Intent("com.huawei.android.powerkit.PowerKitService")), this.mConnection, 1);
        Log.v(TAG, "bind powerkit service, ret = " + ret);
        return ret;
    }

    private Intent createExplicitIntent(Context context, Intent implicitIntent) {
        List<ResolveInfo> resolveInfo = context.getPackageManager().queryIntentServices(implicitIntent, 0);
        String packageName = "com.huawei.powergenie";
        String className = "com.huawei.android.powerkit.PowerKitService";
        if (resolveInfo == null || resolveInfo.size() != 1) {
            Log.w(TAG, "not only one match for intent: " + implicitIntent);
        } else {
            ResolveInfo serviceInfo = resolveInfo.get(0);
            packageName = serviceInfo.serviceInfo.packageName;
            className = serviceInfo.serviceInfo.name;
        }
        ComponentName component = new ComponentName(packageName, className);
        Log.i(TAG, "match for intent,  packageName:" + packageName + " className:" + className);
        Intent explicitIntent = new Intent(implicitIntent);
        explicitIntent.setComponent(component);
        return explicitIntent;
    }

    public String getPowerKitVersion(Context context) throws RemoteException {
        if (this.mService != null) {
            return this.mService.getPowerKitVersion(context.getPackageName());
        }
        throw new RemoteException("PowerKit server is not found");
    }

    public float getCurrentResolutionRatio(Context context) throws RemoteException {
        if (this.mService != null) {
            return this.mService.getCurrentResolutionRatio(context.getPackageName());
        }
        throw new RemoteException("PowerKit server is not found");
    }

    public int getCurrentFps(Context context) throws RemoteException {
        if (this.mService != null) {
            return this.mService.getCurrentFps(context.getPackageName());
        }
        throw new RemoteException("PowerKit server is not found");
    }

    public int setFps(Context context, int fps) throws RemoteException {
        if (this.mService != null) {
            return this.mService.setFps(context.getPackageName(), fps);
        }
        throw new RemoteException("PowerKit server is not found");
    }

    public boolean applyForResourceUse(Context context, boolean apply, String module, int resourceType, long timeoutInMS, String reason) throws RemoteException {
        if (this.mService != null) {
            return this.mService.applyForResourceUse(context.getPackageName(), apply, module, resourceType, timeoutInMS, reason);
        }
        throw new RemoteException("PowerKit server is not found");
    }

    public boolean notifyCallingModules(String callerPkg, String self, List<String> callingModules) throws RemoteException {
        if (this.mService != null) {
            return this.mService.notifyCallingModules(callerPkg, self, callingModules);
        }
        throw new RemoteException("PowerKit server is not found");
    }

    public boolean enableStateEvent(Sink sink, int stateType) throws RemoteException {
        if (this.mService == null) {
            throw new RemoteException("PowerKit server is not found");
        }
        synchronized (this.mLock) {
            if (!registerSink(sink)) {
                return false;
            }
            Log.i(TAG, "registerSink return true");
            ArrayList<Integer> states = this.mSinkSetStates.get(sink);
            if (states == null) {
                ArrayList<Integer> states2 = new ArrayList<>();
                states2.add(Integer.valueOf(stateType));
                this.mSinkSetStates.put(sink, states2);
            } else {
                states.add(Integer.valueOf(stateType));
            }
            this.mEnabledStates.add(Integer.valueOf(stateType));
            return this.mService.enableStateEvent(stateType);
        }
    }

    public boolean disableStateEvent(Sink sink, int stateType) throws RemoteException {
        if (this.mService == null) {
            throw new RemoteException("PowerKit server is not found");
        }
        synchronized (this.mLock) {
            ArrayList<Integer> states = this.mSinkSetStates.get(sink);
            if (states != null) {
                states.remove(Integer.valueOf(stateType));
                if (states.size() == 0) {
                    this.mSinkSetStates.remove(sink);
                    unregisterSink(sink);
                }
            }
            this.mEnabledStates.remove(Integer.valueOf(stateType));
        }
        Log.i(TAG, "disableStateEvent ! stateType: " + stateType);
        return this.mService.disableStateEvent(stateType);
    }

    public boolean isUserSleeping(Context context) throws RemoteException {
        if (this.mService == null) {
            throw new RemoteException("PowerKit server is not found");
        }
        Log.i(TAG, "isUserSleeping ! pkg: " + context.getPackageName());
        return this.mService.isUserSleeping(context.getPackageName());
    }

    public int getPowerMode(Context context) throws RemoteException {
        if (this.mService == null) {
            throw new RemoteException("PowerKit server is not found");
        }
        Log.i(TAG, "getPowerMode ! pkg: " + context.getPackageName());
        return this.mService.getPowerMode(context.getPackageName());
    }

    public boolean registerMaintenanceTime(Context context, boolean isRegister, String module, long inactiveTime, long activeTime) throws RemoteException {
        if (this.mService == null) {
            throw new RemoteException("PowerKit server is not found");
        } else if (!isRegister || (inactiveTime >= 300000 && activeTime <= 300000)) {
            Log.i(TAG, "pkg: " + context.getPackageName() + (isRegister ? " register" : " unRegister") + " maintenance time for pkg " + module);
            return this.mService.registerMaintenanceTime(context.getPackageName(), isRegister, module, inactiveTime, activeTime);
        } else {
            Log.i(TAG, "pkg: " + context.getPackageName() + " inactiveTime: " + inactiveTime + " activeTime: " + activeTime);
            return false;
        }
    }

    public boolean setPowerOptimizeType(Context context, boolean isSet, int appType, int optimizeType) throws RemoteException {
        if (this.mService == null) {
            throw new RemoteException("PowerKit server is not found");
        }
        Log.i(TAG, "pkg: " + context.getPackageName() + " isSet: " + isSet + " appType: " + appType + " optimizeType: " + optimizeType);
        return this.mService.setPowerOptimizeType(context.getPackageName(), isSet, appType, optimizeType);
    }

    public int getPowerOptimizeType(Context context) throws RemoteException {
        if (this.mService == null) {
            throw new RemoteException("PowerKit server is not found");
        }
        Log.i(TAG, "pkg: " + context.getPackageName() + " get optimize type.");
        return this.mService.getPowerOptimizeType(context.getPackageName());
    }

    public boolean setActiveState(Context context, int stateType, int eventType) throws RemoteException {
        if (this.mService == null) {
            throw new RemoteException("PowerKit server is not found");
        }
        Log.i(TAG, "pkg: " + context.getPackageName() + " set stateType:" + stateType + " eventType:" + eventType);
        return this.mService.setActiveState(context.getPackageName(), stateType, eventType);
    }

    private void startStateRecognitionProvider() {
        try {
            this.mService.registerSink(this.mSinkTransport);
        } catch (RemoteException e) {
            Log.e(TAG, "register sink transport fail.");
        }
    }

    private void stopStateRecognitionProvider() {
        try {
            this.mService.unregisterSink(this.mSinkTransport);
        } catch (RemoteException e) {
            Log.e(TAG, "unregister sink transport fail.");
        }
    }

    private boolean registerSink(Sink sink) {
        if (sink == null) {
            Log.e(TAG, "registerSink a null sink fail.");
            return false;
        } else if (this.mSinkSet.contains(sink)) {
            return true;
        } else {
            this.mSinkSet.add(sink);
            if (this.mSinkSet.size() != 1) {
                return true;
            }
            startStateRecognitionProvider();
            return true;
        }
    }

    private void unregisterSink(Sink sink) {
        this.mSinkSet.remove(sink);
        if (this.mSinkSet.size() == 0) {
            stopStateRecognitionProvider();
        }
    }

    public void binderDied() {
        Log.e(TAG, "powerkit process binder was died and connecting ...");
        this.mService = null;
        int maxCount = 5;
        while (maxCount > 0) {
            maxCount--;
            SystemClock.sleep((long) (new Random().nextInt(2001) + 1000));
            if (bindPowerKitService()) {
                return;
            }
        }
    }

    private final class SinkTransport extends IStateSink.Stub {
        private SinkTransport() {
        }

        @Override // com.huawei.android.powerkit.adapter.IStateSink
        public void onPowerOverUsing(String module, int resourceType, long stats_duration, long hold_time, String extend) {
            Log.i(PowerKitApi.TAG, "onPowerOverUsing moudle:" + module + " resource:" + resourceType + " duration:" + stats_duration + " time:" + hold_time + " extend:" + extend);
            synchronized (PowerKitApi.this.mLock) {
                if (!PowerKitApi.this.mSinkSet.isEmpty()) {
                    Iterator it = PowerKitApi.this.mSinkSet.iterator();
                    while (it.hasNext()) {
                        Sink sink = (Sink) it.next();
                        ArrayList<Integer> states = (ArrayList) PowerKitApi.this.mSinkSetStates.get(sink);
                        if (states != null && states.contains(50)) {
                            sink.onPowerOverUsing(module, resourceType, stats_duration, hold_time, extend);
                        }
                    }
                }
            }
        }
    }
}
