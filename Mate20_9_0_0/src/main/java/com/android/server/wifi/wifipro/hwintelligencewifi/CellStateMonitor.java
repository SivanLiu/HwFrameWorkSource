package com.android.server.wifi.wifipro.hwintelligencewifi;

import android.content.Context;
import android.os.Handler;
import android.telephony.CellLocation;
import android.telephony.HwTelephonyManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class CellStateMonitor {
    private static final int PHONE_TYPE_CDMA = 1;
    private static final int PHONE_TYPE_GSM = 2;
    private PhoneStateListener celllistener = new PhoneStateListener() {
        public void onCellLocationChanged(CellLocation location) {
            Log.e(MessageUtil.TAG, "onCellLocationChanged");
            super.onCellLocationChanged(location);
            CellStateMonitor.this.processCellIDChange();
        }
    };
    private Context mContext;
    private Handler mHandler;
    private TelephonyManager mTelephonyManager;

    public CellStateMonitor(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
    }

    public void startMonitor() {
        Log.e(MessageUtil.TAG, "startMonitor start listen for cell info");
        this.mTelephonyManager.listen(this.celllistener, 16);
    }

    public void stopMonitor() {
        this.mTelephonyManager.listen(this.celllistener, 0);
    }

    private void processCellIDChange() {
        if (this.mTelephonyManager != null) {
            this.mHandler.sendEmptyMessage(20);
        }
    }

    public String getCurrentCellid() {
        String cellidString = null;
        if (this.mTelephonyManager != null) {
            int phoneId = HwTelephonyManager.getDefault().getPreferredDataSubscription();
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCurrentCellid phoneId = ");
            stringBuilder.append(phoneId);
            Log.d(str, stringBuilder.toString());
            if (phoneId >= 0 && phoneId < 2) {
                CellLocation mCellLocation = HwTelephonyManager.getDefault().getCellLocation(phoneId);
                if (mCellLocation != null) {
                    int type;
                    if (mCellLocation instanceof CdmaCellLocation) {
                        type = 1;
                        Log.e(MessageUtil.TAG, "getCurrentCellid type type = PHONE_TYPE_CDMA");
                    } else if (mCellLocation instanceof GsmCellLocation) {
                        type = 2;
                        Log.e(MessageUtil.TAG, "getCurrentCellid type type = PHONE_TYPE_GSM");
                    } else {
                        type = 0;
                    }
                    int networkid;
                    StringBuilder stringBuilder2;
                    switch (type) {
                        case 1:
                            Log.e(MessageUtil.TAG, "getCurrentCellid type is PHONE_TYPE_CDMA");
                            CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) mCellLocation;
                            int systemid = cdmaCellLocation.getSystemId();
                            networkid = cdmaCellLocation.getNetworkId();
                            int cellid = cdmaCellLocation.getBaseStationId();
                            if (systemid >= 0 && networkid >= 0 && cellid >= 0) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(Integer.toString(systemid));
                                stringBuilder2.append(Integer.toString(networkid));
                                stringBuilder2.append(Integer.toString(cellid));
                                cellidString = stringBuilder2.toString();
                                str = MessageUtil.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("getCurrentCellid PHONE_TYPE_CDMA cellidString = ");
                                stringBuilder3.append(cellidString);
                                Log.e(str, stringBuilder3.toString());
                                break;
                            }
                            String plmn = this.mTelephonyManager.getNetworkOperator();
                            cellid = cdmaCellLocation.getCid();
                            if (plmn != null && cellid >= 0) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(plmn);
                                stringBuilder2.append(Integer.toString(cellid));
                                cellidString = stringBuilder2.toString();
                                str = MessageUtil.TAG;
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("getCurrentCellid VOLTE cellidString = ");
                                stringBuilder4.append(cellidString);
                                Log.e(str, stringBuilder4.toString());
                                break;
                            }
                            Log.e(MessageUtil.TAG, "getCurrentCellid cellid == null");
                            return null;
                            break;
                        case 2:
                            Log.e(MessageUtil.TAG, "getCurrentCellid type is PHONE_TYPE_GSM");
                            GsmCellLocation gsmCellLocation = (GsmCellLocation) mCellLocation;
                            String plmn2 = this.mTelephonyManager.getNetworkOperator();
                            networkid = gsmCellLocation.getCid();
                            if (plmn2 != null && networkid >= 0) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(plmn2);
                                stringBuilder2.append(Integer.toString(networkid));
                                cellidString = stringBuilder2.toString();
                                str = MessageUtil.TAG;
                                StringBuilder stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("getCurrentCellid PHONE_TYPE_GSM cellidString = ");
                                stringBuilder5.append(cellidString);
                                Log.e(str, stringBuilder5.toString());
                                break;
                            }
                            return null;
                            break;
                        default:
                            Log.e(MessageUtil.TAG, "getCurrentCellid type is error");
                            break;
                    }
                }
                return null;
            }
            return null;
        }
        Log.e(MessageUtil.TAG, "getCurrentCellid mTelephonyManager == null");
        return cellidString;
    }

    public static int getCellRssi() {
        return -1;
    }
}
