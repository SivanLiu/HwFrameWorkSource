package com.android.server.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationRequest;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;
import com.android.internal.location.ProviderRequest;
import com.android.server.LocationManagerServiceUtil;
import com.huawei.ncdft.HwNcDftConnManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HwGpsLogServices implements IHwGpsLogServices {
    private static final int BUF_MIXSIZE = 10;
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final int DOMAIN_GNSS = 0;
    private static final long NETWORK_POS_TIMEOUT_SECOND = 11000;
    private static final String TAG = "HwGnssLogServices";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    static HwGpsLogServices mHwGpsLogServices;
    HwNcDftConnManager hwNcDftConnManager;
    private Context mContext;
    private LocationManagerServiceUtil mLocationManagerServiceUtil;
    private boolean mNetWorkFixPending = false;
    private Timer mNlpTimer = null;
    private TimerTask mNlpTimerTask = null;
    private HandlerThread mThread;

    private HwGpsLogServices(Context context) {
        this.mContext = context;
        Log.d(TAG, "enter HwGpsLogServices");
        this.mThread = new HandlerThread("HwGpsLogServices");
        this.mThread.start();
        this.hwNcDftConnManager = new HwNcDftConnManager(this.mContext);
    }

    public static synchronized HwGpsLogServices getInstance(Context context) {
        HwGpsLogServices hwGpsLogServices;
        synchronized (HwGpsLogServices.class) {
            if (mHwGpsLogServices == null) {
                mHwGpsLogServices = new HwGpsLogServices(context);
            }
            hwGpsLogServices = mHwGpsLogServices;
        }
        return hwGpsLogServices;
    }

    public static synchronized HwGpsLogServices getGpsLogService() {
        HwGpsLogServices hwGpsLogServices;
        synchronized (HwGpsLogServices.class) {
            hwGpsLogServices = mHwGpsLogServices;
        }
        return hwGpsLogServices;
    }

    private int sendToDft(int event, List<String> list) {
        if (this.hwNcDftConnManager != null) {
            this.hwNcDftConnManager.reportToDft(0, event, list);
            return 0;
        }
        Log.d(TAG, "hwNcDftConnManager is null, ignore!");
        return 1;
    }

    public void netWorkLocation(String provider, ProviderRequest providerRequest) {
        if (provider.equalsIgnoreCase("network")) {
            boolean isNeedUpdate = true;
            if (providerRequest.locationRequests.size() == 0) {
                isNeedUpdate = false;
            } else {
                for (LocationRequest request : providerRequest.locationRequests) {
                    if (request.getNumUpdates() <= 0) {
                        isNeedUpdate = false;
                    }
                }
            }
            if (this.hwNcDftConnManager != null && isNeedUpdate) {
                String requestInterval = String.valueOf(providerRequest.interval);
                List<String> list = new ArrayList();
                list.add(requestInterval);
                list.add(provider);
                sendToDft(0, list);
            }
            if (this.mNetWorkFixPending) {
                Log.e(TAG, "Network pos is already runing.");
                return;
            }
            this.mNetWorkFixPending = true;
            startNlpTimer();
        }
    }

    private void startNlpTimer() {
        if (this.mNlpTimer == null) {
            this.mNlpTimer = new Timer();
        }
        if (this.mNlpTimerTask != null) {
            this.mNlpTimerTask.cancel();
            this.mNlpTimerTask = null;
        }
        this.mNlpTimerTask = new TimerTask() {
            public void run() {
                HwGpsLogServices.this.mLocationManagerServiceUtil = LocationManagerServiceUtil.getDefault();
                if (HwGpsLogServices.this.mLocationManagerServiceUtil == null) {
                    Log.d(HwGpsLogServices.TAG, " mLocationManagerServiceUtil == null ");
                    HwGpsLogServices.this.mNetWorkFixPending = false;
                    return;
                }
                LocationProviderInterface p = (LocationProviderInterface) HwGpsLogServices.this.mLocationManagerServiceUtil.getRealProviders().get("network");
                if (p == null) {
                    Log.d(HwGpsLogServices.TAG, " LocationProviderInterface p is null ");
                    HwGpsLogServices.this.mNetWorkFixPending = false;
                    return;
                }
                Bundle extras = new Bundle();
                int status = p.getStatus(extras);
                String str = HwGpsLogServices.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  network position over 11s,  NLP status:  ");
                stringBuilder.append(status);
                Log.d(str, stringBuilder.toString());
                if (status > 2) {
                    HwGpsLogServices.this.updateNLPStatusRecord(status);
                }
                ArrayList<Integer> statusList = extras.getIntegerArrayList("status");
                if (statusList != null) {
                    int i = 10;
                    if (statusList.size() < 10) {
                        i = statusList.size();
                    }
                    int list_size = i;
                    String str2 = HwGpsLogServices.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" list network position over 11s,  list_size:  ");
                    stringBuilder2.append(list_size);
                    Log.d(str2, stringBuilder2.toString());
                    for (i = 0; i < list_size; i++) {
                        HwGpsLogServices.this.updateNLPStatusRecord(((Integer) statusList.get(i)).intValue());
                    }
                }
                HwGpsLogServices.this.mNetWorkFixPending = false;
            }
        };
        try {
            this.mNlpTimer.schedule(this.mNlpTimerTask, NETWORK_POS_TIMEOUT_SECOND);
        } catch (IllegalStateException e) {
            Log.e(TAG, " TimerTask is scheduled failed.");
        }
    }

    private void stopNlpTimer() {
        this.mNetWorkFixPending = false;
        Log.d(TAG, "stopNlpTimer ");
        if (this.mNlpTimer != null) {
            this.mNlpTimer.cancel();
            this.mNlpTimer.purge();
            this.mNlpTimer = null;
        }
        if (this.mNlpTimerTask != null) {
            this.mNlpTimerTask.cancel();
            this.mNlpTimerTask = null;
        }
    }

    public void openGpsSwitchFail(int open) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(Integer.toString(open));
        sendToDft(14, list);
    }

    public void initGps(boolean isEnable, byte EngineCapabilities) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(String.valueOf(isEnable));
        list.add(Integer.toString(EngineCapabilities));
        sendToDft(1, list);
    }

    public void updateXtraDloadStatus(boolean status) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(String.valueOf(status));
        sendToDft(10, list);
    }

    public void updateNtpDloadStatus(boolean status) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(String.valueOf(status));
        sendToDft(11, list);
    }

    public void updateSetPosMode(boolean status, int interval) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(String.valueOf(status));
        list.add(Integer.toString(interval));
        sendToDft(12, list);
    }

    public void updateSetPosMode(boolean status) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateSetPosMode = ");
        stringBuilder.append(status);
        Log.i(str, stringBuilder.toString());
    }

    public void updateApkName(String provider, String hashCode, String name, String apkRunMode, String apkTimeStamp) {
        if (this.hwNcDftConnManager == null) {
            return;
        }
        if (provider.equalsIgnoreCase("gps") || provider.equalsIgnoreCase("network") || provider.equalsIgnoreCase("APKSTOPPROVIDER")) {
            List<String> list = new ArrayList();
            list.add(String.valueOf(provider));
            list.add(String.valueOf(hashCode));
            list.add(String.valueOf(name));
            list.add(String.valueOf(apkRunMode));
            list.add(String.valueOf(apkTimeStamp));
            sendToDft(31, list);
        }
    }

    public void updateNLPStatus(int status) {
        stopNlpTimer();
        updateNLPStatusRecord(status);
    }

    public void updateNLPStatusRecord(int status) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(String.valueOf(status));
        sendToDft(29, list);
    }

    public void startGps(boolean isEnable, int PositionMode) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(String.valueOf(isEnable));
        list.add(Integer.toString(PositionMode));
        sendToDft(2, list);
    }

    public void updateNetworkState(NetworkInfo info) {
        if (this.hwNcDftConnManager != null) {
            this.hwNcDftConnManager.reportNetworkInfo(0, 3, info);
        }
    }

    public void updateAgpsState(int type, int state) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(Integer.toString(type));
        list.add(Integer.toString(state));
        sendToDft(4, list);
    }

    public void stopGps(boolean status) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(String.valueOf(status));
        sendToDft(5, list);
    }

    public void permissionErr(String packageName) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(packageName);
        sendToDft(13, list);
    }

    public void addGeofenceStatus() {
        List<String> list = new ArrayList();
        list.clear();
        list.add("0");
        sendToDft(15, list);
    }

    public void addBatchingStatus() {
        List<String> list = new ArrayList();
        list.clear();
        list.add("0");
        sendToDft(16, list);
    }

    public void updateGpsRunState(int status) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(Integer.toString(status));
        sendToDft(6, list);
    }

    public void updateLocation(Location location, long time, String provider) {
        if (this.hwNcDftConnManager != null) {
            this.hwNcDftConnManager.reportGnssLocation(0, 8, location, time, provider);
        }
    }

    public void updateSvStatus(int svCount, int[] svs, float[] snrs, float[] svElevations, float[] svAzimuths) {
        if (this.hwNcDftConnManager != null) {
            this.hwNcDftConnManager.reportGnssSvStatus(0, 9, svCount, svs, snrs, svElevations, svAzimuths);
        }
    }

    public void reportErrorNtpTime(long currentNtpTime, long realTime) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(String.valueOf(currentNtpTime));
        list.add(String.valueOf(realTime));
        sendToDft(19, list);
    }

    public void reportBinderError() {
        List<String> list = new ArrayList();
        list.clear();
        list.add("0");
        sendToDft(20, list);
    }

    public void processGnssHalDriverEvent(String strJsonExceptionBody) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("driver err is ");
        stringBuilder.append(strJsonExceptionBody);
        Log.d(str, stringBuilder.toString());
    }

    public void updateModemData(byte aPosMode, byte aAidingDataStatus, byte aAidingDataReqFlg, byte[] aCurNetStatus, byte aAGPSResult, byte aSUPLStatus, byte aTimeFlg, byte aAddrFlg, byte[] aServerAdder, long aSUPLStatusCode, long aAgpsStartTime, long aAtlOpenTime, long aConnSvrTime, long aAgpsEndTime, short aServerIpPort) {
    }

    public void updateNtpServerInfo(String address) {
        if (address != null) {
            List<String> list = new ArrayList();
            list.clear();
            list.add(address);
            sendToDft(23, list);
        }
    }

    public void injectExtraParam(String extraParam) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(extraParam);
        sendToDft(24, list);
    }

    public void injectTimeParam(int timeSource, long ntpTime, int uncertainty) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(Integer.toString(timeSource));
        list.add(String.valueOf(ntpTime));
        list.add(Integer.toString(uncertainty));
        sendToDft(27, list);
    }

    public int logEvent(int type, int event, String parameter) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(Integer.toString(type));
        list.add(Integer.toString(event));
        list.add(parameter);
        int result = sendToDft(25, list);
        if (result != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("send Higeo CHR msg error:");
            stringBuilder.append(type);
            stringBuilder.append(",");
            stringBuilder.append(event);
            stringBuilder.append(",");
            stringBuilder.append(parameter);
            Log.d(str, stringBuilder.toString());
        }
        return result;
    }

    public void LocationSettingsOffErr(String provider) {
        List<String> list = new ArrayList();
        list.add(provider);
        sendToDft(28, list);
    }

    public void setQuickGpsParam(int id, String param) {
        List<String> list = new ArrayList();
        list.clear();
        list.add(Integer.toString(id));
        list.add(param);
        sendToDft(30, list);
    }
}
