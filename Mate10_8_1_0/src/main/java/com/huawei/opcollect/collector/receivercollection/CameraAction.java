package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.location.Location;
import com.huawei.opcollect.location.SystemLocation;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.EventIdConstant;
import com.huawei.opcollect.utils.LocationChange;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONException;
import org.json.JSONObject;

public class CameraAction extends Action implements LocationChange {
    private static String TAG = OPCollectConstant.CAMERA_TAKE_ACTION_NAME;
    private static CameraAction sInstance = null;
    private AtomicInteger accummulateNumber = new AtomicInteger();
    private CameraReceiver mReceiver = null;

    class CameraReceiver extends BroadcastReceiver {
        CameraReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if ("android.hardware.action.NEW_PICTURE".equalsIgnoreCase(action)) {
                    OPCollectLog.r(CameraAction.TAG, action);
                    CameraAction.this.perform();
                }
            }
        }
    }

    private CameraAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_TAKE_PICTURE));
    }

    public static synchronized CameraAction getInstance(Context context) {
        CameraAction cameraAction;
        synchronized (CameraAction.class) {
            if (sInstance == null) {
                sInstance = new CameraAction(context, OPCollectConstant.CAMERA_TAKE_ACTION_NAME);
            }
            cameraAction = sInstance;
        }
        return cameraAction;
    }

    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new CameraReceiver();
            IntentFilter intentFilter = new IntentFilter();
            try {
                intentFilter.addAction("android.hardware.action.NEW_PICTURE");
                intentFilter.addDataType("image/*");
            } catch (MalformedMimeTypeException e) {
                OPCollectLog.e(TAG, "MalformedMimeTypeException:" + e.toString());
            }
            this.mContext.registerReceiver(this.mReceiver, intentFilter, null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            SystemLocation.getInstance(this.mContext).enable();
        }
    }

    public void onGetLocation(Location location) {
        saveLocation(location);
    }

    protected boolean execute() {
        if (SystemLocation.getInstance(this.mContext).isLocating()) {
            this.accummulateNumber.getAndIncrement();
        } else {
            SystemLocation.getInstance(this.mContext).getCurrentLocation(this);
        }
        return true;
    }

    public boolean perform() {
        return super.perform();
    }

    public void disable() {
        super.disable();
        if (this.mReceiver != null && this.mContext != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
    }

    public boolean destroy() {
        super.destroy();
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (CameraAction.class) {
            sInstance = null;
        }
    }

    private void saveLocation(Location location) {
        int sendNum = this.accummulateNumber.getAndSet(0);
        JSONObject object = new JSONObject();
        if (location != null) {
            try {
                object.put("latitude", location.getLatitude());
                object.put("longitude", location.getLongitude());
            } catch (JSONException e) {
                OPCollectLog.e(TAG, "JSONException:" + e.getMessage());
            }
        } else {
            try {
                object.put("latitude", EventIdConstant.PURPOSE_STR_BLANK);
                object.put("longitude", EventIdConstant.PURPOSE_STR_BLANK);
            } catch (JSONException e2) {
                OPCollectLog.e(TAG, "JSONException:" + e2.getMessage());
            }
        }
        while (sendNum >= 0) {
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_TAKE_PICTURE, object.toString());
            sendNum--;
        }
    }

    public void dump(int indentNum, PrintWriter pw) {
        super.dump(indentNum, pw);
        if (pw != null) {
            String indent = String.format("%" + indentNum + "s\\-", new Object[]{" "});
            if (this.mReceiver == null) {
                pw.println(indent + "receiver is null");
            } else {
                pw.println(indent + "receiver not null");
            }
        }
    }
}
