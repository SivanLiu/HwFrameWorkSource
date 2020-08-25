package com.android.server.rms.iaware.dev;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.rms.iaware.AwareLog;
import com.huawei.msdp.devicestatus.DeviceStatusConstant;
import com.huawei.msdp.devicestatus.HwMSDPDeviceStatus;
import com.huawei.msdp.devicestatus.HwMSDPDeviceStatusChangeEvent;
import com.huawei.msdp.devicestatus.HwMSDPDeviceStatusChangedCallBack;
import com.huawei.msdp.devicestatus.HwMSDPDeviceStatusEvent;
import com.huawei.msdp.devicestatus.HwMSDPDeviceStatusServiceConnection;
import java.util.ArrayList;
import java.util.List;

public class PhoneStatusRecong {
    private static final long CALL_BACK_INTERVAL_DEFAULT_VALUE = 86400000000000L;
    private static final int DEBUG_TRUE = 1;
    private static final long MIN_RETRY_TIME = 1800000;
    private static final int MSG_CONNECT_MSDP = 0;
    private static final String TAG = "PhoneStatusRecong";
    private static final Object sLock = new Object();
    private static PhoneStatusRecong sPhoneStatusRecong;
    private Context mContext = null;
    /* access modifiers changed from: private */
    public final List<CurrentStatus> mCurrentStatus = new ArrayList();
    private long mCurrentStatusEnterTime = SystemClock.elapsedRealtime();
    private HwMSDPDeviceStatus mDevMsdpDeviceStatus;
    private DevMsdpDeviceStatusChangedCallBack mDevMsdpDeviceStatusChangedCallBack;
    private DevMsdpDeviceStatusServiceConnection mDevMsdpDeviceStatusServiceConnection;
    /* access modifiers changed from: private */
    public boolean mIsConnected = false;
    private boolean mIsStatusValid = true;
    private PhoneStatusHandler mPhoneStatusHandler = new PhoneStatusHandler();
    private long mRecordTime = 0;

    public static PhoneStatusRecong getInstance() {
        PhoneStatusRecong phoneStatusRecong;
        synchronized (sLock) {
            if (sPhoneStatusRecong == null) {
                sPhoneStatusRecong = new PhoneStatusRecong();
            }
            phoneStatusRecong = sPhoneStatusRecong;
        }
        return phoneStatusRecong;
    }

    public void init(Context context) {
        if (context != null) {
            this.mContext = context;
            this.mDevMsdpDeviceStatus = new HwMSDPDeviceStatus(context);
            this.mDevMsdpDeviceStatusChangedCallBack = new DevMsdpDeviceStatusChangedCallBack();
            this.mDevMsdpDeviceStatusServiceConnection = new DevMsdpDeviceStatusServiceConnection();
        }
    }

    private class PhoneStatusHandler extends Handler {
        private PhoneStatusHandler() {
        }

        public void handleMessage(Message msg) {
            if (msg == null) {
                AwareLog.d(PhoneStatusRecong.TAG, "msg is null, error");
            } else if (msg.what == 0) {
                PhoneStatusRecong.this.connectService();
            }
        }
    }

    public void connectService() {
        if (this.mContext != null && this.mDevMsdpDeviceStatus != null && this.mDevMsdpDeviceStatusChangedCallBack != null && this.mDevMsdpDeviceStatusServiceConnection != null) {
            AwareLog.d(TAG, "connectService");
            this.mDevMsdpDeviceStatus.connectService(this.mDevMsdpDeviceStatusChangedCallBack, this.mDevMsdpDeviceStatusServiceConnection);
        }
    }

    public void disconnectService() {
        if (this.mDevMsdpDeviceStatus != null && this.mIsConnected) {
            disableDeviceStatusEvent(DeviceStatusConstant.MSDP_DEVICESTATUS_TYPE_STILL_STATUS, 1);
            this.mDevMsdpDeviceStatus.disconnectService();
        }
    }

    /* access modifiers changed from: private */
    public boolean enableDeviceStatusEvent(String deviceStatus, int eventType, long reportLatencyNs) {
        HwMSDPDeviceStatus hwMSDPDeviceStatus = this.mDevMsdpDeviceStatus;
        if (hwMSDPDeviceStatus != null) {
            return hwMSDPDeviceStatus.enableDeviceStatusEvent(deviceStatus, eventType, reportLatencyNs);
        }
        return false;
    }

    private boolean disableDeviceStatusEvent(String deviceStatus, int eventType) {
        HwMSDPDeviceStatus hwMSDPDeviceStatus = this.mDevMsdpDeviceStatus;
        if (hwMSDPDeviceStatus != null) {
            return hwMSDPDeviceStatus.disableDeviceStatusEvent(deviceStatus, eventType);
        }
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x002f, code lost:
        r0 = r9.mDevMsdpDeviceStatus;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0031, code lost:
        if (r0 == null) goto L_0x0038;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0037, code lost:
        return r0.getCurrentDeviceStatus();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0038, code lost:
        return null;
     */
    private HwMSDPDeviceStatusChangeEvent getCurrentDeviceStatus() {
        synchronized (this.mCurrentStatus) {
            if (!this.mIsConnected) {
                this.mCurrentStatus.clear();
                long curTime = SystemClock.elapsedRealtime();
                if (curTime - this.mRecordTime >= MIN_RETRY_TIME) {
                    this.mRecordTime = curTime;
                    Message msg = this.mPhoneStatusHandler.obtainMessage();
                    msg.what = 0;
                    this.mPhoneStatusHandler.sendMessage(msg);
                }
                return null;
            }
        }
    }

    public void getDeviceStatus() {
        HwMSDPDeviceStatusChangeEvent status = getCurrentDeviceStatus();
        if (status == null) {
            AwareLog.e(TAG, "status is null, error! mIsConnected is " + this.mIsConnected);
            this.mIsStatusValid = false;
            return;
        }
        Iterable<HwMSDPDeviceStatusEvent> statusList = status.getDeviceStatusRecognitionEvents();
        if (statusList == null) {
            this.mIsStatusValid = false;
            return;
        }
        this.mIsStatusValid = true;
        synchronized (this.mCurrentStatus) {
            this.mCurrentStatus.clear();
            for (HwMSDPDeviceStatusEvent event : statusList) {
                if (event != null) {
                    long currentStatusEnterTime = event.getmTimestampNs();
                    String currentStatus = event.getmDeviceStatus();
                    setPhoneStatus(currentStatus, currentStatusEnterTime);
                    AwareLog.d(TAG, "Current status :" + currentStatus + ", current time :" + currentStatusEnterTime);
                }
            }
        }
    }

    private void setPhoneStatus(String currentStatus, long currentStatusEnterTime) {
        if (currentStatus != null) {
            CurrentStatus curState = new CurrentStatus(0, currentStatusEnterTime);
            char c = 65535;
            switch (currentStatus.hashCode()) {
                case -1773012506:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICESTATUS_TYPE_UNKNOWN)) {
                        c = 6;
                        break;
                    }
                    break;
                case -734839525:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_HIGH_STILL)) {
                        c = 0;
                        break;
                    }
                    break;
                case -703279893:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_MOVEMENT_OF_FAST_WALKING)) {
                        c = 4;
                        break;
                    }
                    break;
                case -662314917:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_MOVEMENT_OF_OTHER)) {
                        c = 5;
                        break;
                    }
                    break;
                case 300619368:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_COARSE_STILL)) {
                        c = 1;
                        break;
                    }
                    break;
                case 605414195:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_FINE_STILL)) {
                        c = 2;
                        break;
                    }
                    break;
                case 1435500548:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_MOVEMENT_OF_WALKING)) {
                        c = 3;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    curState.setPhoneStatus(1);
                    break;
                case 1:
                    curState.setPhoneStatus(2);
                    break;
                case 2:
                    curState.setPhoneStatus(3);
                    break;
                case 3:
                    curState.setPhoneStatus(4);
                    break;
                case 4:
                    curState.setPhoneStatus(5);
                    break;
                case 5:
                    curState.setPhoneStatus(6);
                    break;
                case 6:
                    curState.setPhoneStatus(0);
                    break;
                default:
                    curState.setPhoneStatus(0);
                    break;
            }
            this.mCurrentStatus.add(curState);
        }
    }

    public List<CurrentStatus> getCurrentStatus() {
        ArrayList arrayList;
        synchronized (this.mCurrentStatus) {
            if (!this.mIsStatusValid && this.mCurrentStatus.size() == 0) {
                this.mCurrentStatus.add(new CurrentStatus(0, SystemClock.elapsedRealtime()));
            }
            arrayList = new ArrayList(this.mCurrentStatus);
        }
        return arrayList;
    }

    private int parseInt(String str) {
        if (str == null || str.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "parseInt NumberFormatException");
            return 0;
        }
    }

    private Long parseLong(String str) {
        long value = this.mCurrentStatusEnterTime;
        if (str == null || str.length() == 0) {
            return Long.valueOf(value);
        }
        try {
            value = Long.parseLong(str);
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "parseLong NumberFormatException");
        }
        return Long.valueOf(value);
    }

    private class DevMsdpDeviceStatusServiceConnection implements HwMSDPDeviceStatusServiceConnection {
        private DevMsdpDeviceStatusServiceConnection() {
        }

        @Override // com.huawei.msdp.devicestatus.HwMSDPDeviceStatusServiceConnection
        public void onServiceConnected() {
            AwareLog.d(PhoneStatusRecong.TAG, "onServiceConnected()");
            boolean unused = PhoneStatusRecong.this.mIsConnected = true;
            if (!PhoneStatusRecong.this.enableDeviceStatusEvent(DeviceStatusConstant.MSDP_DEVICESTATUS_TYPE_STILL_STATUS, 1, PhoneStatusRecong.CALL_BACK_INTERVAL_DEFAULT_VALUE)) {
                AwareLog.e(PhoneStatusRecong.TAG, "enable error");
            }
        }

        @Override // com.huawei.msdp.devicestatus.HwMSDPDeviceStatusServiceConnection
        public void onServiceDisconnected() {
            synchronized (PhoneStatusRecong.this.mCurrentStatus) {
                PhoneStatusRecong.this.mCurrentStatus.clear();
            }
            AwareLog.d(PhoneStatusRecong.TAG, "onServiceDisconnected() and clear cache");
            boolean unused = PhoneStatusRecong.this.mIsConnected = false;
        }
    }

    private static class DevMsdpDeviceStatusChangedCallBack implements HwMSDPDeviceStatusChangedCallBack {
        private DevMsdpDeviceStatusChangedCallBack() {
        }

        @Override // com.huawei.msdp.devicestatus.HwMSDPDeviceStatusChangedCallBack
        public void onDeviceStatusChanged(HwMSDPDeviceStatusChangeEvent hwMSDPDeviceStatusChangeEvent) {
            AwareLog.d(PhoneStatusRecong.TAG, "onDeviceStatusChanged");
        }
    }

    public static class CurrentStatus {
        private int mPhoneStatus;
        private long mTimestamp;

        public CurrentStatus(int phoneStatus, long timestamp) {
            this.mPhoneStatus = phoneStatus;
            this.mTimestamp = timestamp;
        }

        public int getPhoneStatus() {
            return this.mPhoneStatus;
        }

        public void setPhoneStatus(int phoneStatus) {
            this.mPhoneStatus = phoneStatus;
        }

        public long getTimestamp() {
            return this.mTimestamp;
        }

        public void setTimestamp(long timestamp) {
            this.mTimestamp = timestamp;
        }

        public String toString() {
            return "CurrentStatus[ mPhoneStatus:" + this.mPhoneStatus + ", mTimestamp:" + this.mTimestamp + "]";
        }
    }
}
