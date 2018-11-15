package com.android.server.wifi.HwQoE;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.hidata.HwQoEUdpNetWorkInfo;
import com.android.server.wifi.HwQoE.HiDataTracfficInfo.WeChatMobileTrafficInfo;
import java.util.List;

public class HidataWechatTraffic {
    public static final long AUDIO_AVG_TRAFFIC_WEALTHY = 15360;
    public static final long AUDIO_TOTAL_TRAFFIC_WEALTHY = 51200;
    public static final int MIN_VALID_COUNTER = 3;
    public static final long MIN_VALID_TIME = 60000;
    private static final String TAG = "HiDATA_WechatTraffic";
    public static final long VIDEO_AVG_TRAFFIC_WEALTHY = 102400;
    public static final long VIDEO_TOTAL_TRAFFIC_WEALTHY = 204800;
    private Context mContext;
    private int mCurrMoniorUid;
    private String mCurrentDefaultDataImsi;
    private HwQoEJNIAdapter mHwQoEJNIAdapter = HwQoEJNIAdapter.getInstance();
    private HwQoEQualityManager mHwQoEQualityManager = HwQoEQualityManager.getInstance(this.mContext);
    private HwQoEUdpNetWorkInfo mMobileEndUdpInfo;
    private HwQoEUdpNetWorkInfo mMobileStartUdpInfo;
    private TelephonyManager mTelephonyManager = ((TelephonyManager) this.mContext.getSystemService("phone"));
    private long mWechatCallDuration;
    private long mWechatCallStartTime;
    private long mWechatTraffic;
    private int mWechatType;

    public HidataWechatTraffic(Context context) {
        this.mContext = context;
    }

    public void updateMobileWechatStateChanged(int state, int type, int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateMobileWechatStateChanged,state: ");
        stringBuilder.append(state);
        stringBuilder.append(", type: ");
        stringBuilder.append(type);
        stringBuilder.append(" ,uid: ");
        stringBuilder.append(uid);
        Log.d(str, stringBuilder.toString());
        if (1 == state) {
            if (this.mCurrMoniorUid == uid) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(uid);
                stringBuilder.append(" Already in the statistics, ignore the duplicate request");
                Log.d(str, stringBuilder.toString());
                return;
            }
            this.mWechatCallStartTime = System.currentTimeMillis();
            this.mCurrMoniorUid = uid;
            getCurrentDefaultDataImsi();
            this.mWechatType = type;
            this.mMobileStartUdpInfo = this.mHwQoEJNIAdapter.getUdpNetworkStatsDetail(uid, 0);
        } else if (state == 0) {
            if (this.mCurrMoniorUid != uid) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(uid);
                stringBuilder.append(" Not in statistics, ignore the wrong request,moniorUid = ");
                stringBuilder.append(this.mCurrMoniorUid);
                Log.d(str, stringBuilder.toString());
                return;
            }
            this.mMobileEndUdpInfo = this.mHwQoEJNIAdapter.getUdpNetworkStatsDetail(uid, 0);
            this.mWechatTraffic = calculateWechatTraffic(this.mMobileEndUdpInfo, this.mMobileStartUdpInfo);
            this.mCurrMoniorUid = 0;
            this.mWechatCallDuration = (System.currentTimeMillis() - this.mWechatCallStartTime) / 1000;
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("wechat type: ");
            stringBuilder2.append(this.mWechatType);
            stringBuilder2.append(", traffic:");
            stringBuilder2.append(this.mWechatTraffic / 1000);
            stringBuilder2.append(" KB, duration:");
            stringBuilder2.append(this.mWechatCallDuration);
            stringBuilder2.append(" s");
            Log.d(str2, stringBuilder2.toString());
            updateTrafficToDB(this.mCurrentDefaultDataImsi, this.mWechatType, this.mWechatTraffic, this.mWechatCallDuration);
            this.mWechatType = 0;
            this.mWechatCallDuration = 0;
        }
    }

    public boolean wechatTrafficWealthy(int type) {
        if (TextUtils.isEmpty(getCurrentDefaultDataImsi())) {
            return false;
        }
        WeChatMobileTrafficInfo trafficInfo = queryTrafficToDB(this.mCurrentDefaultDataImsi, type);
        if (trafficInfo == null) {
            Log.w(TAG, "wechat trafficInfo is null ");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("wechat trafficInfo: ");
        stringBuilder.append(trafficInfo.toString());
        Log.d(str, stringBuilder.toString());
        if (trafficInfo.counter <= 3 || trafficInfo.totalTime <= 180000) {
            return false;
        }
        if (type == 1) {
            if (trafficInfo.totalTraffic > VIDEO_TOTAL_TRAFFIC_WEALTHY || trafficInfo.avgTraffic > VIDEO_AVG_TRAFFIC_WEALTHY) {
                return true;
            }
        } else if ((type == 2 || trafficInfo.avgTraffic > AUDIO_AVG_TRAFFIC_WEALTHY) && trafficInfo.totalTraffic > AUDIO_TOTAL_TRAFFIC_WEALTHY) {
            return true;
        }
        return false;
    }

    private void updateTrafficToDB(String dataImsi, int type, long traffic, long duration) {
        this.mHwQoEQualityManager.addAPPTracfficData(new HiDataTracfficInfo(dataImsi, type, traffic, duration));
    }

    public WeChatMobileTrafficInfo queryTrafficToDB(String dataImsi, int type) {
        List<HiDataTracfficInfo> hiDataTracfficInfoList = this.mHwQoEQualityManager.queryTracfficData(dataImsi, type, HiDataTrafficManager.getTimeFromNow(HiDataTrafficManager.MONTH_TIME), System.currentTimeMillis());
        if (hiDataTracfficInfoList == null || hiDataTracfficInfoList.size() == 0) {
            Log.d(TAG, "hiDataTracfficInfoList is null:");
            return null;
        }
        WeChatMobileTrafficInfo mWeChatMobileTrafficInfo = new WeChatMobileTrafficInfo();
        int size = hiDataTracfficInfoList.size();
        mWeChatMobileTrafficInfo.counter = size;
        for (int i = 0; i < size; i++) {
            HiDataTracfficInfo hiDataTracfficInfo = (HiDataTracfficInfo) hiDataTracfficInfoList.get(i);
            if (hiDataTracfficInfo != null) {
                mWeChatMobileTrafficInfo.totalTraffic += hiDataTracfficInfo.mThoughtput;
                mWeChatMobileTrafficInfo.totalTime += hiDataTracfficInfo.mDuration;
            }
        }
        return mWeChatMobileTrafficInfo;
    }

    private String getCurrentDefaultDataImsi() {
        this.mCurrentDefaultDataImsi = this.mTelephonyManager.getSubscriberId(SubscriptionManager.getDefaultDataSubscriptionId());
        return this.mCurrentDefaultDataImsi;
    }

    private long calculateWechatTraffic(HwQoEUdpNetWorkInfo currUdpInfo, HwQoEUdpNetWorkInfo lastUdpInfo) {
        if (currUdpInfo == null || lastUdpInfo == null) {
            return 0;
        }
        if (currUdpInfo.getUid() == lastUdpInfo.getUid()) {
            return (currUdpInfo.getRxUdpBytes() - lastUdpInfo.getRxUdpBytes()) + (currUdpInfo.getTxUdpBytes() - lastUdpInfo.getTxUdpBytes());
        }
        Log.d(TAG, "uid is error,calculateWechatTraffic");
        return 0;
    }
}
