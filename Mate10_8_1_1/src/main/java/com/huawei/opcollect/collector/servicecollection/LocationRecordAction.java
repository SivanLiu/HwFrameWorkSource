package com.huawei.opcollect.collector.servicecollection;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Message;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.huawei.nb.model.collectencrypt.RawLocationRecord;
import com.huawei.nb.query.Query;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.opcollect.location.HwLocation;
import com.huawei.opcollect.location.ILocationListener;
import com.huawei.opcollect.location.IntelligentLocation;
import com.huawei.opcollect.location.SystemLocation;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class LocationRecordAction extends Action {
    private static final int FIVE_MINUTES_IN_MILLISECOND = 300000;
    private static final int INVALID_SUBSCRIPTION_ID = -1;
    public static final int POSITION_CHANGE_INTELLIGENT = 1;
    public static final int POSITION_CHANGE_SYSTEM = 2;
    private static final String TAG = "LocationRecordAction";
    private static LocationRecordAction sInstance = null;
    private ConnectivityManager mConnectivityManager = null;
    private Geocoder mGeocoder = null;
    private LocationHandler mHandler = null;
    private HwLocation mLocation = null;
    private HashMap<String, ILocationListener> mLocationListeners = null;
    private final Object mLock = new Object();
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager = null;

    private static class LocationHandler extends Handler {
        private final WeakReference<LocationRecordAction> service;

        LocationHandler(LocationRecordAction service) {
            this.service = new WeakReference(service);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            OPCollectLog.r(LocationRecordAction.TAG, "handleMessage ");
            LocationRecordAction action = (LocationRecordAction) this.service.get();
            if (action != null) {
                if (msg.what == 1) {
                    action.mLocation = (HwLocation) msg.obj;
                } else if (msg.what == 2) {
                    action.mLocation = (HwLocation) msg.obj;
                }
                if (action.mLocation != null) {
                    action.perform();
                }
            }
        }
    }

    public static synchronized LocationRecordAction getInstance(Context context) {
        LocationRecordAction locationRecordAction;
        synchronized (LocationRecordAction.class) {
            if (sInstance == null) {
                sInstance = new LocationRecordAction(OPCollectConstant.LOCATION_ACTION_NAME, context);
            }
            locationRecordAction = sInstance;
        }
        return locationRecordAction;
    }

    private LocationRecordAction(String name, Context context) {
        super(context, name);
        setDailyRecordNum(queryDailyRecordNum(RawLocationRecord.class));
        if (this.mContext == null) {
            OPCollectLog.e(TAG, "context is null");
            return;
        }
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mLocationListeners = new HashMap();
        this.mGeocoder = new Geocoder(this.mContext, Locale.CHINA);
    }

    public void enable() {
        super.enable();
        SystemLocation.getInstance(this.mContext).enable();
        IntelligentLocation.getInstance(this.mContext).enable();
        synchronized (this.mLock) {
            if (this.mHandler == null) {
                this.mHandler = new LocationHandler(this);
            }
        }
    }

    public void disable() {
        super.disable();
        IntelligentLocation.getInstance(this.mContext).disable();
        synchronized (this.mLock) {
            if (this.mHandler != null) {
                this.mHandler.removeMessages(1);
                this.mHandler.removeMessages(2);
                this.mHandler = null;
            }
        }
    }

    public boolean destroy() {
        super.destroy();
        IntelligentLocation.getInstance(this.mContext).destroy();
        if (this.mLocationListeners != null) {
            this.mLocationListeners.clear();
            this.mLocationListeners = null;
        }
        this.mTelephonyManager = null;
        this.mConnectivityManager = null;
        this.mWifiManager = null;
        this.mLocation = null;
        this.mGeocoder = null;
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (LocationRecordAction.class) {
            sInstance = null;
        }
    }

    protected boolean execute() {
        OPCollectLog.r(TAG, "execute");
        if (this.mLocation == null) {
            return false;
        }
        RawLocationRecord rawLocationRecord = new RawLocationRecord();
        long timestamp = this.mLocation.getTimestamp();
        OPCollectLog.r(TAG, "execute: " + timestamp);
        if (timestamp == -1) {
            rawLocationRecord.setMTimeStamp(new Date());
        } else {
            AManagedObject aManagedObject = OdmfCollectScheduler.getInstance().getOdmfHelper().querySingleManageObject(Query.select(RawLocationRecord.class).equalTo("mTimeStamp", Long.valueOf(timestamp)));
            if (aManagedObject != null) {
                ((RawLocationRecord) aManagedObject).setMLongitude(Double.valueOf(this.mLocation.getLongitude()));
                ((RawLocationRecord) aManagedObject).setMLatitude(Double.valueOf(this.mLocation.getLatitude()));
                OdmfCollectScheduler.getInstance().getOdmfHelper().updateManageObject(aManagedObject);
                OPCollectLog.r(TAG, "update record: " + timestamp);
                return false;
            } else if (Math.abs(System.currentTimeMillis() - timestamp) > 300000) {
                OPCollectLog.r(TAG, timestamp + " data is old, ignore.");
                return false;
            } else {
                rawLocationRecord.setMTimeStamp(new Date(this.mLocation.getTimestamp()));
            }
        }
        locationToGeocode();
        getLocation(rawLocationRecord);
        getWifiInfo(rawLocationRecord);
        getCellInfo(rawLocationRecord);
        rawLocationRecord.setMReservedText(OPCollectUtils.formatCurrentTime());
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, rawLocationRecord).sendToTarget();
        if (this.mLocationListeners != null) {
            OPCollectLog.i(TAG, "listener size: " + this.mLocationListeners.size());
            for (ILocationListener listener : this.mLocationListeners.values()) {
                if (listener != null) {
                    listener.onLocationSuccess(this.mLocation);
                }
            }
        }
        return true;
    }

    private void getLocation(RawLocationRecord rawLocationRecord) {
        rawLocationRecord.setMLongitude(Double.valueOf(this.mLocation.getLongitude()));
        rawLocationRecord.setMLatitude(Double.valueOf(this.mLocation.getLatitude()));
        rawLocationRecord.setMCity(this.mLocation.getCity());
        rawLocationRecord.setMAltitude(Double.valueOf(this.mLocation.getAltitude()));
        rawLocationRecord.setMCountry(this.mLocation.getCountry());
        rawLocationRecord.setMProvince(this.mLocation.getProvince());
        rawLocationRecord.setMDetailAddress(this.mLocation.getDetailAddress());
        rawLocationRecord.setMDistrict(this.mLocation.getDistrict());
        if ("gps".equals(this.mLocation.getProvider())) {
            rawLocationRecord.setMLocationType(Character.valueOf('1'));
        } else if ("network".equals(this.mLocation.getProvider())) {
            rawLocationRecord.setMLocationType(Character.valueOf('2'));
        } else {
            rawLocationRecord.setMLocationType(Character.valueOf('0'));
        }
    }

    private void getWifiInfo(RawLocationRecord rawLocationRecord) {
        if (isWifiConnected() && this.mWifiManager != null) {
            WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                rawLocationRecord.setMWifiBSSID(wifiInfo.getBSSID());
                rawLocationRecord.setMWifiLevel(Integer.valueOf(wifiInfo.getRssi()));
                return;
            }
            rawLocationRecord.setMWifiBSSID(null);
            rawLocationRecord.setMWifiLevel(Integer.valueOf(INVALID_SUBSCRIPTION_ID));
        }
    }

    private void getCellInfo(RawLocationRecord rawLocationRecord) {
        int cell_mcc = INVALID_SUBSCRIPTION_ID;
        int cell_mnv = INVALID_SUBSCRIPTION_ID;
        int cell_id = INVALID_SUBSCRIPTION_ID;
        int cell_lac = INVALID_SUBSCRIPTION_ID;
        if (this.mTelephonyManager == null) {
            OPCollectLog.e(TAG, "TelephonyManager is null");
            return;
        }
        List<CellInfo> list = this.mTelephonyManager.getAllCellInfo();
        if (list == null || list.size() == 0) {
            OPCollectLog.e(TAG, "list is null");
            return;
        }
        List<CellInfo> registeredList = new ArrayList();
        for (CellInfo cellInfo : list) {
            if (cellInfo != null && cellInfo.isRegistered()) {
                registeredList.add(cellInfo);
            }
        }
        int size = registeredList.size();
        if (size <= 0) {
            OPCollectLog.e(TAG, "registeredList size is 0");
            return;
        }
        CellInfo info;
        int main_slot = INVALID_SUBSCRIPTION_ID;
        if (VERSION.SDK_INT > 23) {
            main_slot = SubscriptionManager.getDefaultDataSubscriptionId();
        }
        OPCollectLog.r(TAG, "slot: " + main_slot + " size: " + size + " : " + list.size());
        if (1 == size || main_slot <= INVALID_SUBSCRIPTION_ID || main_slot >= size) {
            info = (CellInfo) registeredList.get(0);
        } else {
            info = (CellInfo) registeredList.get(main_slot);
        }
        if (info != null) {
            if (info instanceof CellInfoLte) {
                CellIdentityLte cellIdentityLte = ((CellInfoLte) info).getCellIdentity();
                if (cellIdentityLte != null) {
                    cell_mcc = cellIdentityLte.getMcc();
                    cell_mnv = cellIdentityLte.getMnc();
                    cell_id = cellIdentityLte.getCi();
                    cell_lac = cellIdentityLte.getTac();
                } else {
                    return;
                }
            } else if (info instanceof CellInfoGsm) {
                CellIdentityGsm cellIdentityGsm = ((CellInfoGsm) info).getCellIdentity();
                if (cellIdentityGsm != null) {
                    cell_mcc = cellIdentityGsm.getMcc();
                    cell_mnv = cellIdentityGsm.getMnc();
                    cell_id = cellIdentityGsm.getCid();
                    cell_lac = cellIdentityGsm.getLac();
                } else {
                    return;
                }
            } else if (info instanceof CellInfoCdma) {
                CellIdentityCdma cellIdentityCdma = ((CellInfoCdma) info).getCellIdentity();
                if (cellIdentityCdma != null) {
                    String network_operator = this.mTelephonyManager.getNetworkOperator();
                    if (network_operator != null && network_operator.length() > 2) {
                        try {
                            cell_mcc = Integer.parseInt(network_operator.substring(0, 3));
                        } catch (NumberFormatException e) {
                            OPCollectLog.e(TAG, e.getMessage());
                        }
                    }
                    cell_mnv = cellIdentityCdma.getSystemId();
                    cell_id = cellIdentityCdma.getBasestationId();
                    cell_lac = cellIdentityCdma.getNetworkId();
                } else {
                    return;
                }
            } else if (info instanceof CellInfoWcdma) {
                CellIdentityWcdma cellWcdmaCellIdentity = ((CellInfoWcdma) info).getCellIdentity();
                if (cellWcdmaCellIdentity != null) {
                    cell_mcc = cellWcdmaCellIdentity.getMcc();
                    cell_mnv = cellWcdmaCellIdentity.getMnc();
                    cell_id = cellWcdmaCellIdentity.getCid();
                    cell_lac = cellWcdmaCellIdentity.getLac();
                } else {
                    return;
                }
            }
        }
        rawLocationRecord.setMCellID(Integer.valueOf(cell_id));
        rawLocationRecord.setMCellLAC(Integer.valueOf(cell_lac));
        rawLocationRecord.setMCellMCC(Integer.valueOf(cell_mcc));
        rawLocationRecord.setMCellMNC(Integer.valueOf(cell_mnv));
    }

    private boolean isWifiConnected() {
        boolean z = true;
        if (this.mConnectivityManager == null) {
            return false;
        }
        NetworkInfo info = this.mConnectivityManager.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            z = false;
        } else if (info.getType() != 1) {
            z = false;
        }
        return z;
    }

    public void addLocationListener(String key, ILocationListener listener) {
        if (this.mLocationListeners != null) {
            OPCollectLog.i(TAG, "add listener: " + listener);
            this.mLocationListeners.put(key, listener);
        }
    }

    public void removeLocationListener(String key, ILocationListener listener) {
        if (this.mLocationListeners != null) {
            OPCollectLog.i(TAG, "remove listener: " + listener);
            this.mLocationListeners.remove(key);
        }
    }

    public void dump(int indentNum, PrintWriter pw) {
        super.dump(indentNum, pw);
        SystemLocation.getInstance(this.mContext).dump(indentNum, pw);
        IntelligentLocation.getInstance(this.mContext).dump(indentNum, pw);
    }

    public Handler getLocationHandler() {
        return this.mHandler;
    }

    public Object getLock() {
        return this.mLock;
    }

    private void locationToGeocode() {
        try {
            if (this.mGeocoder != null) {
                List<Address> addresses = this.mGeocoder.getFromLocation(this.mLocation.getLatitude(), this.mLocation.getLongitude(), 1);
                if (addresses == null || addresses.size() <= 0) {
                    OPCollectLog.e(TAG, "Geocoder exception!");
                    return;
                }
                Address address = (Address) addresses.get(0);
                if (address != null) {
                    this.mLocation.setCityCode(address.getLocality());
                    this.mLocation.setCountry(address.getCountryCode());
                    this.mLocation.setCity(address.getLocality());
                    this.mLocation.setDetailAddress(address.toString());
                    this.mLocation.setDistrict(address.getSubLocality());
                    this.mLocation.setProvince(address.getAdminArea());
                }
            }
        } catch (IOException e) {
            OPCollectLog.e(TAG, "getFromLocation io failed: " + e.getMessage());
        } catch (IllegalArgumentException e2) {
            OPCollectLog.e(TAG, "getFromLocation illegal failed: " + e2.getMessage());
        }
    }
}
