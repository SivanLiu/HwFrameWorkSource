package com.android.server.rms.iaware.feature;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.IAwareCMSManager;
import com.android.internal.util.MemInfoReader;
import com.android.server.rms.iaware.HwStartWindowCache;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import java.util.Map;

public class StartWindowCacheFeature extends RFeature {
    private static final String APS_RESOLUTION_CHANGE_ACTION = "huawei.intent.action.APS_RESOLUTION_CHANGE_ACTION";
    private static final String APS_RESOLUTION_CHANGE_PERSISSIONS = "huawei.intent.permissions.APS_RESOLUTION_CHANGE_ACTION";
    private static final int BASE_VERSION = 5;
    private static final int DEFAULT_TOPN = 10;
    private static final long HIGH_END_DEVICE_THRESHOLD = 6144;
    private static final int INVALID_VALUE = -1;
    private static final String TAG = "StartWindowCacheFeature";
    private static final String TAG_CONFIG_NAME = "TopNConfig";
    private static final String TAG_FEATURE_NAME = "StartWindowCache";
    private static final String TAG_RAM_SIZE = "ramsize";
    private static final String TAG_TOPN = "topN";
    private boolean mIsEnabled = false;
    private BroadcastReceiver mResolutionChangeReceiver = new BroadcastReceiver() {
        /* class com.android.server.rms.iaware.feature.StartWindowCacheFeature.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            HwStartWindowCache.getInstance().notifyResolutionChange();
        }
    };

    public StartWindowCacheFeature(Context context, AwareConstant.FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean reportData(CollectData data) {
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enable() {
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean disable() {
        if (this.mIsEnabled) {
            this.mContext.unregisterReceiver(this.mResolutionChangeReceiver);
            HwStartWindowCache.getInstance().deinit();
        }
        this.mIsEnabled = false;
        AwareLog.i(TAG, "StartWindowCacheFeature disable");
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enableFeatureEx(int realVersion) {
        if (realVersion < 5 || this.mContext == null) {
            return false;
        }
        int topN = readCustConfig();
        if (topN == -1 && shouldDeviceEnableByDefault()) {
            topN = 10;
        }
        if (topN <= 0) {
            return false;
        }
        if (this.mIsEnabled) {
            this.mContext.unregisterReceiver(this.mResolutionChangeReceiver);
        }
        this.mContext.registerReceiverAsUser(this.mResolutionChangeReceiver, UserHandle.ALL, new IntentFilter(APS_RESOLUTION_CHANGE_ACTION), APS_RESOLUTION_CHANGE_PERSISSIONS, null);
        HwStartWindowCache.getInstance().init(topN);
        this.mIsEnabled = true;
        return true;
    }

    private int readCustConfig() {
        Map<String, String> configPropertries;
        AwareConfig configList = null;
        try {
            IBinder awareservice = IAwareCMSManager.getICMSManager();
            if (awareservice == null) {
                AwareLog.w(TAG, "can not find service awareservice.");
                return -1;
            }
            configList = IAwareCMSManager.getCustConfig(awareservice, TAG_FEATURE_NAME, TAG_CONFIG_NAME);
            if (configList == null) {
                AwareLog.i(TAG, "readCustConfig failure, no config use default setting");
                return -1;
            }
            MemInfoReader minfo = new MemInfoReader();
            minfo.readMemInfo();
            long totalMemMb = minfo.getTotalSize() / 1048576;
            MemoryUtils.initialRamSizeLowerBound();
            for (AwareConfig.Item item : configList.getConfigList()) {
                if (!(item == null || item.getProperties() == null || (configPropertries = item.getProperties()) == null)) {
                    String ramSize = configPropertries.get("ramsize");
                    if (MemoryUtils.checkRamSize(ramSize, Long.valueOf(totalMemMb))) {
                        AwareLog.d(TAG, "loadStartWindowCache success: ramsize: " + ramSize + " totalMemMb: " + totalMemMb);
                        return getTopNConfig(item);
                    }
                }
            }
            return -1;
        } catch (RemoteException e) {
            AwareLog.e(TAG, "getConfig RemoteException!");
        }
    }

    private int getTopNConfig(AwareConfig.Item item) {
        for (AwareConfig.SubItem subItem : item.getSubItemList()) {
            if (subItem != null) {
                String itemName = subItem.getName();
                String itemValue = subItem.getValue();
                if (!(itemName == null || itemValue == null || !TAG_TOPN.equals(itemName))) {
                    try {
                        int topN = Integer.parseInt(itemValue.trim());
                        AwareLog.i(TAG, "applyTopNConfig topN = " + topN);
                        return topN;
                    } catch (NumberFormatException e) {
                        AwareLog.e(TAG, "applyTopNConfig error!");
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    private boolean shouldDeviceEnableByDefault() {
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        return minfo.getTotalSize() / 1048576 > HIGH_END_DEVICE_THRESHOLD;
    }
}
