package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import com.huawei.opcollect.location.SystemLocation;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.LocationChange;
import com.huawei.opcollect.utils.OPCollectLog;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONException;
import org.json.JSONObject;

public class CameraAction extends ReceiverAction implements LocationChange {
    private static final Object LOCK = new Object();
    private static final String TAG = "CameraAction";
    private static CameraAction instance = null;
    private AtomicInteger accumulateNumber = new AtomicInteger();

    private CameraAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_TAKE_PICTURE));
    }

    public static CameraAction getInstance(Context context) {
        CameraAction cameraAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new CameraAction(context, "CameraAction");
            }
            cameraAction = instance;
        }
        return cameraAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new CameraReceiver();
            IntentFilter intentFilter = new IntentFilter();
            try {
                intentFilter.addAction("android.hardware.action.NEW_PICTURE");
                intentFilter.addDataType("image/*");
            } catch (IntentFilter.MalformedMimeTypeException e) {
                OPCollectLog.e("CameraAction", "MalformedMimeTypeException:" + e.toString());
            }
            this.mContext.registerReceiver(this.mReceiver, intentFilter, null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            SystemLocation.getInstance(this.mContext).enable();
        }
    }

    @Override // com.huawei.opcollect.utils.LocationChange
    public void onGetLocation(Location location) {
        saveLocation(location);
    }

    class CameraReceiver extends BroadcastReceiver {
        CameraReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if ("android.hardware.action.NEW_PICTURE".equalsIgnoreCase(action)) {
                    OPCollectLog.r("CameraAction", action);
                    CameraAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        if (!SystemLocation.getInstance(this.mContext).isLocating()) {
            SystemLocation.getInstance(this.mContext).getCurrentLocation(this);
            return true;
        }
        this.accumulateNumber.getAndIncrement();
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyCameraActionInstance();
        return true;
    }

    private static void destroyCameraActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    private void saveLocation(Location location) {
        JSONObject object = new JSONObject();
        if (location != null) {
            try {
                object.put("latitude", location.getLatitude());
                object.put("longitude", location.getLongitude());
            } catch (JSONException e) {
                OPCollectLog.e("CameraAction", "JSONException:" + e.getMessage());
            }
        } else {
            try {
                object.put("latitude", "");
                object.put("longitude", "");
            } catch (JSONException e2) {
                OPCollectLog.e("CameraAction", "JSONException:" + e2.getMessage());
            }
        }
        for (int sendNum = this.accumulateNumber.getAndSet(0); sendNum >= 0; sendNum--) {
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_TAKE_PICTURE, object.toString());
        }
    }
}
