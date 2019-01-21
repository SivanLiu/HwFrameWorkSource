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
    private static final long MIN_RETRY_TIME = 5000;
    private static final int MSG_CONNECT_MSDP = 0;
    private static final String TAG = "PhoneStatusRecong";
    private static PhoneStatusRecong sPhoneStatusRecong;
    private Context mContext = null;
    private final List<CurrentStatus> mCurrentStatus = new ArrayList();
    private long mCurrentStatusEnterTime = SystemClock.elapsedRealtime();
    private HwMSDPDeviceStatus mDevMSDPDeviceStatus;
    private boolean mIsConnected = false;
    private boolean mIsDebug = false;
    private boolean mIsStatusValid = true;
    private PhoneStatusHandler mPhoneStatusHandler = new PhoneStatusHandler();
    private long mRecordTime = 0;

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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CurrentStatus[ mPhoneStatus:");
            stringBuilder.append(this.mPhoneStatus);
            stringBuilder.append(", mTimestamp:");
            stringBuilder.append(this.mTimestamp);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    private class PhoneStatusHandler extends Handler {
        private PhoneStatusHandler() {
        }

        public void handleMessage(Message msg) {
            if (msg == null) {
                AwareLog.d(PhoneStatusRecong.TAG, "msg is null, error");
                return;
            }
            if (msg.what == 0) {
                PhoneStatusRecong.this.connectService(PhoneStatusRecong.this.mContext);
            }
        }
    }

    private static class DevMSDPDeviceStatusChangedCallBack implements HwMSDPDeviceStatusChangedCallBack {
        private DevMSDPDeviceStatusChangedCallBack() {
        }

        public void onDeviceStatusChanged(HwMSDPDeviceStatusChangeEvent hwMSDPDeviceStatusChangeEvent) {
            AwareLog.d(PhoneStatusRecong.TAG, "onDeviceStatusChanged");
        }
    }

    private class DevMSDPDeviceStatusServiceConnection implements HwMSDPDeviceStatusServiceConnection {
        private DevMSDPDeviceStatusServiceConnection() {
        }

        public void onServiceConnected() {
            AwareLog.d(PhoneStatusRecong.TAG, "onServiceConnected()");
            PhoneStatusRecong.this.mIsConnected = PhoneStatusRecong.this.getSupportedDeviceStatus();
            if (PhoneStatusRecong.this.mIsConnected && !PhoneStatusRecong.this.enableDeviceStatusEvent(DeviceStatusConstant.MSDP_DEVICESTATUS_TYPE_STILL_STATUS, 1, PhoneStatusRecong.CALL_BACK_INTERVAL_DEFAULT_VALUE)) {
                AwareLog.e(PhoneStatusRecong.TAG, "enable error");
                PhoneStatusRecong.this.mIsConnected = false;
            }
        }

        public void onServiceDisconnected() {
            synchronized (PhoneStatusRecong.this.mCurrentStatus) {
                PhoneStatusRecong.this.mCurrentStatus.clear();
            }
            AwareLog.d(PhoneStatusRecong.TAG, "onServiceDisconnected() and clear cache");
            PhoneStatusRecong.this.mIsConnected = false;
        }
    }

    public static synchronized PhoneStatusRecong getInstance() {
        PhoneStatusRecong phoneStatusRecong;
        synchronized (PhoneStatusRecong.class) {
            if (sPhoneStatusRecong == null) {
                sPhoneStatusRecong = new PhoneStatusRecong();
            }
            phoneStatusRecong = sPhoneStatusRecong;
        }
        return phoneStatusRecong;
    }

    public void connectService(Context context) {
        if (context != null) {
            this.mContext = context;
            this.mDevMSDPDeviceStatus = new HwMSDPDeviceStatus(context);
            AwareLog.d(TAG, "connectService");
            this.mDevMSDPDeviceStatus.connectService(new DevMSDPDeviceStatusChangedCallBack(), new DevMSDPDeviceStatusServiceConnection());
        }
    }

    public void disconnectService() {
        if (this.mDevMSDPDeviceStatus != null && this.mIsConnected) {
            disableDeviceStatusEvent(DeviceStatusConstant.MSDP_DEVICESTATUS_TYPE_STILL_STATUS, 1);
            this.mDevMSDPDeviceStatus.disconnectService();
        }
    }

    private boolean enableDeviceStatusEvent(String deviceStatus, int eventType, long reportLatencyNs) {
        if (this.mDevMSDPDeviceStatus != null) {
            return this.mDevMSDPDeviceStatus.enableDeviceStatusEvent(deviceStatus, eventType, reportLatencyNs);
        }
        return false;
    }

    private boolean disableDeviceStatusEvent(String deviceStatus, int eventType) {
        if (this.mDevMSDPDeviceStatus != null) {
            return this.mDevMSDPDeviceStatus.disableDeviceStatusEvent(deviceStatus, eventType);
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:9:0x002c, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:12:0x0030, code skipped:
            if (r9.mDevMSDPDeviceStatus == null) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:14:0x0038, code skipped:
            return r9.mDevMSDPDeviceStatus.getCurrentDeviceStatus();
     */
    /* JADX WARNING: Missing block: B:15:0x0039, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
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
            }
        }
    }

    public void getDeviceStatus() {
        HwMSDPDeviceStatusChangeEvent status = getCurrentDeviceStatus();
        if (status == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("status is null, error! mIsConnected is ");
            stringBuilder.append(this.mIsConnected);
            AwareLog.e(str, stringBuilder.toString());
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
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Current status :");
                    stringBuilder2.append(currentStatus);
                    stringBuilder2.append(", current time :");
                    stringBuilder2.append(currentStatusEnterTime);
                    AwareLog.d(str2, stringBuilder2.toString());
                }
            }
        }
    }

    private void setPhoneStatus(String currentStatus, long currentStatusEnterTime) {
        if (currentStatus != null) {
            CurrentStatus curState = new CurrentStatus(0, currentStatusEnterTime);
            int i = -1;
            switch (currentStatus.hashCode()) {
                case -1773012506:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICESTATUS_TYPE_UNKNOWN)) {
                        i = 6;
                        break;
                    }
                    break;
                case -734839525:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_HIGH_STILL)) {
                        i = 0;
                        break;
                    }
                    break;
                case -703279893:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_MOVEMENT_OF_FAST_WALKING)) {
                        i = 4;
                        break;
                    }
                    break;
                case -662314917:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_MOVEMENT_OF_OTHER)) {
                        i = 5;
                        break;
                    }
                    break;
                case 300619368:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_COARSE_STILL)) {
                        i = 1;
                        break;
                    }
                    break;
                case 605414195:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_FINE_STILL)) {
                        i = 2;
                        break;
                    }
                    break;
                case 1435500548:
                    if (currentStatus.equals(DeviceStatusConstant.MSDP_DEVICETSTATUS_TYPE_MOVEMENT_OF_WALKING)) {
                        i = 3;
                        break;
                    }
                    break;
            }
            switch (i) {
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
        synchronized (this.mCurrentStatus) {
            if (this.mIsDebug) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getCurrentStatus debug is ");
                stringBuilder.append(this.mCurrentStatus);
                AwareLog.d(str, stringBuilder.toString());
                List list = this.mCurrentStatus;
                return list;
            }
            if (!this.mIsStatusValid && this.mCurrentStatus.size() == 0) {
                this.mCurrentStatus.add(new CurrentStatus(0, SystemClock.elapsedRealtime()));
            }
            ArrayList arrayList = new ArrayList(this.mCurrentStatus);
            return arrayList;
        }
    }

    public final boolean doDumpsys(String[] args) {
        if (args == null) {
            AwareLog.e(TAG, "args is null ,error!");
            return false;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            synchronized (this.mCurrentStatus) {
                this.mCurrentStatus.clear();
                for (int i = 2; i < args.length; i += 2) {
                    this.mCurrentStatus.add(new CurrentStatus(parseInt(args[i]), parseLong(args[i + 1]).longValue()));
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("set mCurrentStatus ");
                stringBuilder.append(this.mCurrentStatus);
                AwareLog.d(str, stringBuilder.toString());
            }
            return true;
        } catch (ArrayIndexOutOfBoundsException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("ArrayIndexOutOfBoundsException, args.length : ");
            stringBuilder.append(args.length);
            AwareLog.e(str, stringBuilder.toString());
            return false;
        }
    }

    private int parseInt(String str) {
        int value = 0;
        if (str == null || str.length() == 0) {
            return 0;
        }
        try {
            value = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "parseInt NumberFormatException");
        }
        return value;
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

    public boolean getSupportedDeviceStatus() {
        if (this.mDevMSDPDeviceStatus != null) {
            String[] status = this.mDevMSDPDeviceStatus.getSupportedDeviceStatus();
            if (status != null && status.length > 0) {
                AwareLog.d(TAG, "getSupportedDeviceStatus ok");
                return true;
            }
        }
        return false;
    }
}
