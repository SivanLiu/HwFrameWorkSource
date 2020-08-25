package com.huawei.opcollect.collector.observercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import com.huawei.opcollect.collector.receivercollection.ReceiverAction;
import com.huawei.opcollect.collector.receivercollection.SysEventUtil;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;

public class GpsStateAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "GpsStateAction";
    private static GpsStateAction instance = null;
    /* access modifiers changed from: private */
    public boolean mIsGpsOn = false;
    /* access modifiers changed from: private */
    public boolean mIsInitialized = false;
    /* access modifiers changed from: private */
    public LocationManager mLocationManager = null;

    private GpsStateAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_GPS_ON) + SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_GPS_OFF));
    }

    public static GpsStateAction getInstance(Context context) {
        GpsStateAction gpsStateAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new GpsStateAction(context, "GpsStateAction");
            }
            gpsStateAction = instance;
        }
        return gpsStateAction;
    }

    private class GpsStateBroadcastReceiver extends BroadcastReceiver {
        private GpsStateBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (GpsStateAction.this.mLocationManager == null) {
                OPCollectLog.e("GpsStateAction", "mLocationManager is null!");
                return;
            }
            boolean enabled = false;
            try {
                enabled = GpsStateAction.this.mLocationManager.isProviderEnabled("gps");
            } catch (SecurityException e) {
                OPCollectLog.e("GpsStateAction", e.getMessage());
            }
            if (enabled) {
                if (!GpsStateAction.this.mIsGpsOn || !GpsStateAction.this.mIsInitialized) {
                    boolean unused = GpsStateAction.this.mIsGpsOn = true;
                    boolean unused2 = GpsStateAction.this.mIsInitialized = true;
                    GpsStateAction.this.perform();
                }
            } else if (GpsStateAction.this.mIsGpsOn || !GpsStateAction.this.mIsInitialized) {
                boolean unused3 = GpsStateAction.this.mIsGpsOn = false;
                boolean unused4 = GpsStateAction.this.mIsInitialized = true;
                GpsStateAction.this.perform();
            }
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new GpsStateBroadcastReceiver();
            this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.location.PROVIDERS_CHANGED");
            this.mContext.registerReceiver(this.mReceiver, intentFilter, null, OdmfCollectScheduler.getInstance().getCtrlHandler());
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyGpsStateActionInstance();
        return true;
    }

    private static void destroyGpsStateActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        super.execute();
        String eventType = this.mIsGpsOn ? SysEventUtil.EVENT_GPS_ON : SysEventUtil.EVENT_GPS_OFF;
        OPCollectLog.d("GpsStateAction", eventType);
        SysEventUtil.collectSysEventData(eventType);
        return true;
    }
}
