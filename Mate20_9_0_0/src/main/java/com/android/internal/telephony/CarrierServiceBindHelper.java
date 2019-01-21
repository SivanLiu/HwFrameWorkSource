package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class CarrierServiceBindHelper {
    private static final int EVENT_PERFORM_IMMEDIATE_UNBIND = 1;
    private static final int EVENT_REBIND = 0;
    private static final String LOG_TAG = "CarrierSvcBindHelper";
    private static final int UNBIND_DELAY_MILLIS = 30000;
    private AppBinding[] mBindings;
    private Context mContext;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mHandler: ");
            stringBuilder.append(msg.what);
            CarrierServiceBindHelper.log(stringBuilder.toString());
            switch (msg.what) {
                case 0:
                    AppBinding binding = (AppBinding) msg.obj;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Rebinding if necessary for phoneId: ");
                    stringBuilder2.append(binding.getPhoneId());
                    CarrierServiceBindHelper.log(stringBuilder2.toString());
                    binding.rebind();
                    return;
                case 1:
                    msg.obj.performImmediateUnbind();
                    return;
                default:
                    return;
            }
        }
    };
    private String[] mLastSimState;
    private final PackageMonitor mPackageMonitor = new CarrierServicePackageMonitor(this, null);
    private BroadcastReceiver mUserUnlockedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Received ");
            stringBuilder.append(action);
            CarrierServiceBindHelper.log(stringBuilder.toString());
            if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                for (AppBinding rebind : CarrierServiceBindHelper.this.mBindings) {
                    rebind.rebind();
                }
            }
        }
    };

    private class AppBinding {
        private int bindCount;
        private String carrierPackage;
        private String carrierServiceClass;
        private CarrierServiceConnection connection;
        private long lastBindStartMillis;
        private long lastUnbindMillis;
        private long mUnbindScheduledUptimeMillis = -1;
        private int phoneId;
        private int unbindCount;

        public AppBinding(int phoneId) {
            this.phoneId = phoneId;
        }

        public int getPhoneId() {
            return this.phoneId;
        }

        public String getPackage() {
            return this.carrierPackage;
        }

        void rebind() {
            List<String> carrierPackageNames = TelephonyManager.from(CarrierServiceBindHelper.this.mContext).getCarrierPackageNamesForIntentAndPhone(new Intent("android.service.carrier.CarrierService"), this.phoneId);
            StringBuilder stringBuilder;
            if (carrierPackageNames == null || carrierPackageNames.size() <= 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("No carrier app for: ");
                stringBuilder.append(this.phoneId);
                CarrierServiceBindHelper.log(stringBuilder.toString());
                unbind(false);
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Found carrier app: ");
            stringBuilder.append(carrierPackageNames);
            CarrierServiceBindHelper.log(stringBuilder.toString());
            String candidateCarrierPackage = (String) carrierPackageNames.get(0);
            if (!TextUtils.equals(this.carrierPackage, candidateCarrierPackage)) {
                unbind(true);
            }
            Intent carrierService = new Intent("android.service.carrier.CarrierService");
            carrierService.setPackage(candidateCarrierPackage);
            ResolveInfo carrierResolveInfo = CarrierServiceBindHelper.this.mContext.getPackageManager().resolveService(carrierService, 128);
            Bundle metadata = null;
            String candidateServiceClass = null;
            if (carrierResolveInfo != null) {
                metadata = carrierResolveInfo.serviceInfo.metaData;
                candidateServiceClass = carrierResolveInfo.getComponentInfo().getComponentName().getClassName();
            }
            Bundle metadata2 = metadata;
            String candidateServiceClass2 = candidateServiceClass;
            if (metadata2 == null || !metadata2.getBoolean("android.service.carrier.LONG_LIVED_BINDING", false)) {
                CarrierServiceBindHelper.log("Carrier app does not want a long lived binding");
                unbind(true);
                return;
            }
            if (!TextUtils.equals(this.carrierServiceClass, candidateServiceClass2)) {
                unbind(true);
            } else if (this.connection != null) {
                cancelScheduledUnbind();
                return;
            }
            this.carrierPackage = candidateCarrierPackage;
            this.carrierServiceClass = candidateServiceClass2;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Binding to ");
            stringBuilder2.append(this.carrierPackage);
            stringBuilder2.append(" for phone ");
            stringBuilder2.append(this.phoneId);
            CarrierServiceBindHelper.log(stringBuilder2.toString());
            this.bindCount++;
            this.lastBindStartMillis = System.currentTimeMillis();
            this.connection = new CarrierServiceConnection(CarrierServiceBindHelper.this, null);
            String error;
            try {
                if (!CarrierServiceBindHelper.this.mContext.bindServiceAsUser(carrierService, this.connection, 67108865, CarrierServiceBindHelper.this.mHandler, Process.myUserHandle())) {
                    error = "bindService returned false";
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Unable to bind to ");
                    stringBuilder3.append(this.carrierPackage);
                    stringBuilder3.append(" for phone ");
                    stringBuilder3.append(this.phoneId);
                    stringBuilder3.append(". Error: ");
                    stringBuilder3.append(error);
                    CarrierServiceBindHelper.log(stringBuilder3.toString());
                    unbind(true);
                }
            } catch (SecurityException error2) {
                error2 = error2.getMessage();
            }
        }

        void unbind(boolean immediate) {
            if (this.connection != null) {
                if (immediate || !this.connection.connected) {
                    cancelScheduledUnbind();
                    performImmediateUnbind();
                } else if (this.mUnbindScheduledUptimeMillis == -1) {
                    this.mUnbindScheduledUptimeMillis = 30000 + SystemClock.uptimeMillis();
                    CarrierServiceBindHelper.log("Scheduling unbind in 30000 millis");
                    CarrierServiceBindHelper.this.mHandler.sendMessageAtTime(CarrierServiceBindHelper.this.mHandler.obtainMessage(1, this), this.mUnbindScheduledUptimeMillis);
                }
            }
        }

        private void performImmediateUnbind() {
            this.unbindCount++;
            this.lastUnbindMillis = System.currentTimeMillis();
            this.carrierPackage = null;
            this.carrierServiceClass = null;
            CarrierServiceBindHelper.log("Unbinding from carrier app");
            CarrierServiceBindHelper.this.mContext.unbindService(this.connection);
            this.connection = null;
            this.mUnbindScheduledUptimeMillis = -1;
        }

        private void cancelScheduledUnbind() {
            CarrierServiceBindHelper.this.mHandler.removeMessages(1);
            this.mUnbindScheduledUptimeMillis = -1;
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Carrier app binding for phone ");
            stringBuilder.append(this.phoneId);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  connection: ");
            stringBuilder.append(this.connection);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  bindCount: ");
            stringBuilder.append(this.bindCount);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  lastBindStartMillis: ");
            stringBuilder.append(this.lastBindStartMillis);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  unbindCount: ");
            stringBuilder.append(this.unbindCount);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  lastUnbindMillis: ");
            stringBuilder.append(this.lastUnbindMillis);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mUnbindScheduledUptimeMillis: ");
            stringBuilder.append(this.mUnbindScheduledUptimeMillis);
            pw.println(stringBuilder.toString());
            pw.println();
        }
    }

    private class CarrierServiceConnection implements ServiceConnection {
        private boolean connected;

        private CarrierServiceConnection() {
        }

        /* synthetic */ CarrierServiceConnection(CarrierServiceBindHelper x0, AnonymousClass1 x1) {
            this();
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Connected to carrier app: ");
            stringBuilder.append(name.flattenToString());
            CarrierServiceBindHelper.log(stringBuilder.toString());
            this.connected = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Disconnected from carrier app: ");
            stringBuilder.append(name.flattenToString());
            CarrierServiceBindHelper.log(stringBuilder.toString());
            this.connected = false;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CarrierServiceConnection[connected=");
            stringBuilder.append(this.connected);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    private class CarrierServicePackageMonitor extends PackageMonitor {
        private CarrierServicePackageMonitor() {
        }

        /* synthetic */ CarrierServicePackageMonitor(CarrierServiceBindHelper x0, AnonymousClass1 x1) {
            this();
        }

        public void onPackageAdded(String packageName, int reason) {
            evaluateBinding(packageName, true);
        }

        public void onPackageRemoved(String packageName, int reason) {
            evaluateBinding(packageName, true);
        }

        public void onPackageUpdateFinished(String packageName, int uid) {
            evaluateBinding(packageName, true);
        }

        public void onPackageModified(String packageName) {
            evaluateBinding(packageName, false);
        }

        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            if (doit) {
                for (String packageName : packages) {
                    evaluateBinding(packageName, true);
                }
            }
            return super.onHandleForceStop(intent, packages, uid, doit);
        }

        private void evaluateBinding(String carrierPackageName, boolean forceUnbind) {
            for (AppBinding appBinding : CarrierServiceBindHelper.this.mBindings) {
                String appBindingPackage = appBinding.getPackage();
                boolean isBindingForPackage = carrierPackageName.equals(appBindingPackage);
                if (isBindingForPackage) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(carrierPackageName);
                    stringBuilder.append(" changed and corresponds to a phone. Rebinding.");
                    CarrierServiceBindHelper.log(stringBuilder.toString());
                }
                if (appBindingPackage == null || isBindingForPackage) {
                    if (forceUnbind) {
                        appBinding.unbind(true);
                    }
                    appBinding.rebind();
                }
            }
        }
    }

    public CarrierServiceBindHelper(Context context) {
        this.mContext = context;
        int numPhones = TelephonyManager.from(context).getPhoneCount();
        this.mBindings = new AppBinding[numPhones];
        this.mLastSimState = new String[numPhones];
        for (int phoneId = 0; phoneId < numPhones; phoneId++) {
            this.mBindings[phoneId] = new AppBinding(phoneId);
        }
        this.mPackageMonitor.register(context, this.mHandler.getLooper(), UserHandle.ALL, false);
        this.mContext.registerReceiverAsUser(this.mUserUnlockedReceiver, UserHandle.SYSTEM, new IntentFilter("android.intent.action.USER_UNLOCKED"), null, this.mHandler);
    }

    /* JADX WARNING: Missing block: B:12:0x004f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void updateForPhoneId(int phoneId, String simState) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update binding for phoneId: ");
        stringBuilder.append(phoneId);
        stringBuilder.append(" simState: ");
        stringBuilder.append(simState);
        log(stringBuilder.toString());
        if (SubscriptionManager.isValidPhoneId(phoneId) && !TextUtils.isEmpty(simState) && phoneId < this.mLastSimState.length && !simState.equals(this.mLastSimState[phoneId])) {
            this.mLastSimState[phoneId] = simState;
            this.mHandler.sendMessage(this.mHandler.obtainMessage(0, this.mBindings[phoneId]));
        }
    }

    private static void log(String message) {
        Log.d(LOG_TAG, message);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CarrierServiceBindHelper:");
        for (AppBinding binding : this.mBindings) {
            binding.dump(fd, pw, args);
        }
    }
}
