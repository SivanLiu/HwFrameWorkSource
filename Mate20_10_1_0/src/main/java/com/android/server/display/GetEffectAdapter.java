package com.android.server.display;

import android.os.Bundle;
import android.os.RemoteException;
import com.android.server.hidata.hinetwork.HwHiNetworkParmStatistics;
import com.android.server.multiwin.HwMultiWinConstants;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.displayengine.DeLog;
import com.huawei.displayengine.IDisplayEngineService;
import com.huawei.hiai.awareness.AwarenessInnerConstants;

class GetEffectAdapter {
    public static final int ERR_INVALID_INPUT = -2;
    public static final int ERR_OTHER = -1;
    private static final String TAG = "DE J GetEffectAdapter";

    GetEffectAdapter() {
    }

    public static int getEffect(IDisplayEngineService service, int feature, int type, Bundle data) throws RemoteException {
        if (feature == 33) {
            return FoldingCompensationProcessor.getEffect(service, type, data);
        }
        DeLog.e(TAG, "Unknown feature:" + feature);
        return -2;
    }

    private static class FoldingCompensationProcessor {
        private FoldingCompensationProcessor() {
        }

        public static int getEffect(IDisplayEngineService service, int type, Bundle data) throws RemoteException {
            if (data == null) {
                DeLog.e(GetEffectAdapter.TAG, "Invalid input: data is null!");
                return -2;
            } else if (type == 2) {
                return getPanelInfo(service, data);
            } else {
                if (type == 6) {
                    return getCalibrationGain(service, data);
                }
                if (type == 8) {
                    return getCalibrationTestLevels(service, data);
                }
                if (type == 9) {
                    return getCalibrationApplyTime(service, data);
                }
                DeLog.e(GetEffectAdapter.TAG, "Invalid input: unsupport type=" + type);
                return -2;
            }
        }

        private static int getPanelInfo(IDisplayEngineService service, Bundle data) throws RemoteException {
            int[] result = new int[3];
            int ret = service.getEffectEx(33, 2, result, result.length);
            if (ret != 0) {
                return ret;
            }
            data.putInt("RegionLine1", result[0]);
            data.putInt("RegionLine2", result[1]);
            int i = result[2];
            if (i == 0) {
                data.putString("RegionDirection", "top");
            } else if (i == 1) {
                data.putString("RegionDirection", HwMultiWinConstants.LEFT_HAND_LAZY_MODE_STR);
            } else if (i == 2) {
                data.putString("RegionDirection", "bottom");
            } else if (i != 3) {
                DeLog.e(GetEffectAdapter.TAG, "Invalid panel region direction=" + result[2]);
                return -1;
            } else {
                data.putString("RegionDirection", HwMultiWinConstants.RIGHT_HAND_LAZY_MODE_STR);
            }
            return 0;
        }

        private static int getCalibrationGain(IDisplayEngineService service, Bundle data) throws RemoteException {
            int[] result = new int[4];
            result[0] = data.getInt(MemoryConstant.MEM_FILECACHE_ITEM_LEVEL);
            String color = data.getString("RGBDimension");
            if (color == null) {
                DeLog.e(GetEffectAdapter.TAG, "Invalid input: color is null!");
                return -2;
            }
            if (AwarenessInnerConstants.HAS_RELATION_FENCE_SEPARATE_KEY.equals(color)) {
                result[1] = 0;
            } else if ("G".equals(color)) {
                result[1] = 1;
            } else if ("B".equals(color)) {
                result[1] = 2;
            } else {
                DeLog.e(GetEffectAdapter.TAG, "Invalid input: color=" + color);
                return -2;
            }
            String region = data.getString("Region");
            if (region == null) {
                DeLog.e(GetEffectAdapter.TAG, "Invalid input: region is null!");
                return -2;
            }
            if (HwMultiWinConstants.LEFT_HAND_LAZY_MODE_STR.equals(region)) {
                result[2] = 1;
            } else if (HwMultiWinConstants.RIGHT_HAND_LAZY_MODE_STR.equals(region)) {
                result[2] = 3;
            } else {
                DeLog.e(GetEffectAdapter.TAG, "Invalid input: region=" + region);
                return -2;
            }
            int ret = service.getEffectEx(33, 6, result, result.length);
            if (ret != 0) {
                return ret;
            }
            data.putInt(HwHiNetworkParmStatistics.GAIN_SUB, result[3]);
            return 0;
        }

        private static int getCalibrationTestLevels(IDisplayEngineService service, Bundle data) throws RemoteException {
            int[] result = new int[9];
            int ret = service.getEffectEx(33, 8, result, result.length);
            if (ret != 0) {
                return ret;
            }
            data.putIntArray("levels", result);
            return 0;
        }

        private static int getCalibrationApplyTime(IDisplayEngineService service, Bundle data) throws RemoteException {
            int[] result = new int[1];
            int ret = service.getEffectEx(33, 9, result, result.length);
            if (ret != 0) {
                return ret;
            }
            data.putInt("IntervalInMs", result[0]);
            return 0;
        }
    }
}
