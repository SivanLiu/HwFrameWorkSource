package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import com.huawei.nb.model.collectencrypt.RawPositionState;
import com.huawei.opcollect.collector.servicecollection.LocationRecordAction;
import com.huawei.opcollect.location.HwLocation;
import com.huawei.opcollect.location.ILocationListener;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.strategy.OdmfActionManager;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Date;

public class AwareLocationAction extends Action implements ILocationListener {
    private static final String CHINA_COUNTRY_CODE = "CN";
    private static final String CHINA_COUNTRY_NUM = "156";
    private static final int LOCATION_COMPANY = 1;
    private static final int LOCATION_HOME = 0;
    private static final int LOCATION_HOMECITY = 3;
    private static final int LOCATION_OUTSIDE_CITY = 2;
    private static final int LOCATION_OVERSEA = 4;
    private static final int LOCATION_UNKNOW = -1;
    private static final int MESSAGE_ON_CHANGE = 1;
    private static final String PLACE_RECOGNITION_ACTION = "com.huawei.placerecognition.action.PLACE_RECOG";
    private static final String PLACE_RECOGNITION_KEY = "placerecognition_key";
    private static final String PLACE_RECOGNITION_PERMISSION = "com.huawei.placerecognition.permission.PLACE_RECOG";
    private static final String SP_CACHE_KEY = "CachedCity";
    private static final String SP_CACHE_XML = "CachedCity";
    private static String TAG = OPCollectConstant.LOCATION_MODE_ACTION_NAME;
    private static AwareLocationAction sInstance = null;
    private boolean isHomeOrOffice = false;
    private boolean isNeedModifyCityCode = false;
    private AwareHandler mHandler = null;
    private int mLocationType = -1;
    private AwareLocationReceiver mReceiver = null;

    private static class AwareHandler extends Handler {
        private final WeakReference<AwareLocationAction> service;

        AwareHandler(AwareLocationAction service) {
            this.service = new WeakReference(service);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg != null && msg.what == 1) {
                AwareLocationAction action = (AwareLocationAction) this.service.get();
                if (action != null) {
                    action.mLocationType = msg.arg1;
                    action.perform();
                }
            }
        }
    }

    class AwareLocationReceiver extends BroadcastReceiver {
        AwareLocationReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (AwareLocationAction.PLACE_RECOGNITION_ACTION.equalsIgnoreCase(intent.getAction())) {
                    Bundle bundle = intent.getExtras();
                    if (bundle == null) {
                        OPCollectLog.e(AwareLocationAction.TAG, "onReceive --> bundle is null");
                        return;
                    }
                    int eventID = bundle.getInt(AwareLocationAction.PLACE_RECOGNITION_KEY);
                    OPCollectLog.r(AwareLocationAction.TAG, "eventID: " + eventID);
                    if (eventID == -1) {
                        AwareLocationAction.this.isHomeOrOffice = false;
                        return;
                    }
                    boolean z;
                    AwareLocationAction awareLocationAction = AwareLocationAction.this;
                    if (eventID == 0 || eventID == 1) {
                        z = true;
                    } else {
                        z = false;
                    }
                    awareLocationAction.isNeedModifyCityCode = z;
                    AwareLocationAction.this.isHomeOrOffice = AwareLocationAction.this.isNeedModifyCityCode;
                    if (AwareLocationAction.this.mHandler == null) {
                        OPCollectLog.e(AwareLocationAction.TAG, "handler is null");
                    } else {
                        AwareLocationAction.this.mHandler.obtainMessage(1, eventID, 0).sendToTarget();
                    }
                }
            }
        }
    }

    public static synchronized AwareLocationAction getInstance(Context context) {
        AwareLocationAction awareLocationAction;
        synchronized (AwareLocationAction.class) {
            if (sInstance == null) {
                sInstance = new AwareLocationAction(context, OPCollectConstant.LOCATION_MODE_ACTION_NAME);
            }
            awareLocationAction = sInstance;
        }
        return awareLocationAction;
    }

    private AwareLocationAction(Context context, String name) {
        super(context, name);
        OPCollectLog.r(TAG, OPCollectConstant.LOCATION_MODE_ACTION_NAME);
        setDailyRecordNum(queryDailyRecordNum(RawPositionState.class));
    }

    public boolean destroy() {
        super.destroy();
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (AwareLocationAction.class) {
            sInstance = null;
        }
    }

    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new AwareLocationReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(PLACE_RECOGNITION_ACTION);
            this.mContext.registerReceiver(this.mReceiver, intentFilter, PLACE_RECOGNITION_PERMISSION, OdmfCollectScheduler.getInstance().getCtrlHandler());
            LocationRecordAction.getInstance(this.mContext).addLocationListener(OPCollectConstant.LOCATION_MODE_ACTION_NAME, this);
        }
        if (this.mHandler == null) {
            this.mHandler = new AwareHandler(this);
        }
    }

    public void disable() {
        super.disable();
        if (!(this.mReceiver == null || this.mContext == null)) {
            this.mContext.unregisterReceiver(this.mReceiver);
            OdmfActionManager.getInstance().removeLocationListener(OPCollectConstant.LOCATION_MODE_ACTION_NAME, this);
            this.mReceiver = null;
        }
        if (this.mHandler != null) {
            this.mHandler = null;
        }
    }

    protected boolean execute() {
        RawPositionState rawPositionState = new RawPositionState();
        rawPositionState.setMTimeStamp(new Date());
        rawPositionState.setMStatus(Integer.valueOf(this.mLocationType));
        rawPositionState.setMReservedText(OPCollectUtils.formatCurrentTime());
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, rawPositionState).sendToTarget();
        return true;
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

    public void onLocationSuccess(HwLocation hwLocation) {
        if (hwLocation == null) {
            OPCollectLog.e(TAG, "hwLocation is null");
        } else if (this.mContext == null) {
            OPCollectLog.e(TAG, "context is null");
        } else if (this.mHandler == null) {
            OPCollectLog.e(TAG, "handler is null");
        } else {
            OPCollectLog.r(TAG, "callback.");
            setCityCode(hwLocation.getCityCode());
            String curCountry = hwLocation.getCountry();
            String curCityCode = hwLocation.getCityCode();
            String cacheCityCode = this.mContext.getSharedPreferences("CachedCity", 32768).getString("CachedCity", "");
            if (!TextUtils.isEmpty(curCountry) && !CHINA_COUNTRY_CODE.equalsIgnoreCase(curCountry) && !CHINA_COUNTRY_NUM.equalsIgnoreCase(curCountry)) {
                this.mHandler.obtainMessage(1, 4, 0).sendToTarget();
            } else if (!TextUtils.isEmpty(curCityCode) && !TextUtils.isEmpty(cacheCityCode)) {
                if (!cacheCityCode.equals(curCityCode)) {
                    this.mHandler.obtainMessage(1, 2, 0).sendToTarget();
                } else if (!this.isHomeOrOffice) {
                    this.mHandler.obtainMessage(1, 3, 0).sendToTarget();
                }
            }
        }
    }

    private void setCityCode(String cityCode) {
        if (TextUtils.isEmpty(cityCode)) {
            OPCollectLog.e(TAG, "cityCode is null or is empty");
        } else if (this.isNeedModifyCityCode) {
            Editor editor = this.mContext.getSharedPreferences("CachedCity", 32768).edit();
            editor.putString("CachedCity", cityCode);
            editor.apply();
            this.isNeedModifyCityCode = false;
        } else {
            OPCollectLog.e(TAG, "no need modify city code");
        }
    }
}
