package com.android.server.hidata.wavemapping.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

public class CellStateMonitor {
    private static final int PHONE_TYPE_CDMA = 1;
    private static final int PHONE_TYPE_GSM = 2;
    private PhoneStateListener celllistener = new PhoneStateListener() {
        public void onCellLocationChanged(CellLocation location) {
            LogUtil.i("onCellLocationChanged");
            super.onCellLocationChanged(location);
            CellStateMonitor.this.processCellIDChange();
        }
    };
    private Context mContext;
    private Handler mHandler;
    private TelephonyManager mTelephonyManager;
    private int preService = 3;
    private BroadcastReceiver serviceListener = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                LogUtil.w(" action is null");
                return;
            }
            Object obj = -1;
            if (action.hashCode() == -2104353374 && action.equals("android.intent.action.SERVICE_STATE")) {
                obj = null;
            }
            if (obj == null) {
                ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("service state changes: from ");
                stringBuilder.append(CellStateMonitor.this.preService);
                stringBuilder.append(" to ");
                stringBuilder.append(serviceState.getState());
                LogUtil.i(stringBuilder.toString());
                if (serviceState.getState() == 0 && CellStateMonitor.this.preService != 0) {
                    CellStateMonitor.this.mHandler.sendEmptyMessage(95);
                }
                if (serviceState.getState() != 0 && CellStateMonitor.this.preService == 0) {
                    CellStateMonitor.this.mHandler.sendEmptyMessage(96);
                }
                CellStateMonitor.this.preService = serviceState.getState();
            }
        }
    };

    public CellStateMonitor(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
    }

    public void startMonitor() {
        LogUtil.i("startMonitor start listen for cell info");
        this.mTelephonyManager.listen(this.celllistener, 16);
        if (this.mContext != null) {
            this.mContext.registerReceiver(this.serviceListener, new IntentFilter("android.intent.action.SERVICE_STATE"));
        }
    }

    public void stopMonitor() {
        this.mTelephonyManager.listen(this.celllistener, 0);
        this.mContext.unregisterReceiver(this.serviceListener);
    }

    private void processCellIDChange() {
        if (this.mTelephonyManager != null) {
            this.mHandler.sendEmptyMessage(93);
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String getCurrentCellid() {
        String cellidString = null;
        if (this.mTelephonyManager != null) {
            CellLocation mCellLocation = this.mTelephonyManager.getCellLocation();
            if (mCellLocation != null) {
                int type;
                if (mCellLocation instanceof CdmaCellLocation) {
                    type = 1;
                    LogUtil.i("getCurrentCellid type type = PHONE_TYPE_CDMA");
                } else if (mCellLocation instanceof GsmCellLocation) {
                    type = 2;
                    LogUtil.i("getCurrentCellid type type = PHONE_TYPE_GSM");
                } else {
                    type = 0;
                }
                int networkid;
                StringBuilder stringBuilder;
                switch (type) {
                    case 1:
                        LogUtil.i("getCurrentCellid type is PHONE_TYPE_CDMA");
                        CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) mCellLocation;
                        if (cdmaCellLocation != null) {
                            int systemid = cdmaCellLocation.getSystemId();
                            networkid = cdmaCellLocation.getNetworkId();
                            int cellid = cdmaCellLocation.getBaseStationId();
                            if (systemid >= 0 && networkid >= 0 && cellid >= 0) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(Integer.toString(systemid));
                                stringBuilder.append(Integer.toString(networkid));
                                stringBuilder.append(Integer.toString(cellid));
                                cellidString = stringBuilder.toString();
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("getCurrentCellid PHONE_TYPE_CDMA cellidString = ");
                                stringBuilder.append(cellidString);
                                LogUtil.i(stringBuilder.toString());
                                break;
                            }
                            return null;
                        }
                        break;
                    case 2:
                        LogUtil.i("getCurrentCellid type is PHONE_TYPE_GSM");
                        GsmCellLocation gsmCellLocation = (GsmCellLocation) mCellLocation;
                        if (gsmCellLocation != null) {
                            String plmn = this.mTelephonyManager.getNetworkOperator();
                            networkid = gsmCellLocation.getCid();
                            if (plmn != null && networkid >= 0) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(plmn);
                                stringBuilder.append(Integer.toString(networkid));
                                cellidString = stringBuilder.toString();
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("getCurrentCellid PHONE_TYPE_GSM cellidString = ");
                                stringBuilder.append(cellidString);
                                LogUtil.i(stringBuilder.toString());
                                break;
                            }
                            return null;
                        }
                        break;
                    default:
                        LogUtil.e("getCurrentCellid type is error");
                        break;
                }
            }
            return null;
        }
        LogUtil.e("getCurrentCellid mTelephonyManager == null");
        return cellidString;
    }

    public static int getCellRssi() {
        return -1;
    }
}
