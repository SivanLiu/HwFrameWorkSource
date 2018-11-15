package com.android.server.hidata.wavemapping.statehandler;

import android.common.HwFrameworkFactory;
import android.net.booster.IHwCommBoosterServiceManager;
import android.os.Bundle;
import com.android.server.hidata.wavemapping.util.LogUtil;

public class HwWmpFastBackLteManager {
    private static final String TAG;
    public static final int WAVEMAPPING_DATA_BACK_TO_LTE = 304;
    private static HwWmpFastBackLteManager mHwWmpFastBackLteManager = null;
    private IHwCommBoosterServiceManager mBooster = null;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(HwWmpFastBackLteManager.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public HwWmpFastBackLteManager() {
        LogUtil.i("HwWmpFastBackLteManager");
        try {
            this.mBooster = HwFrameworkFactory.getHwCommBoosterServiceManager();
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(TAG);
            stringBuilder.append(" HwWmpFastBackLteManager ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            e.printStackTrace();
        }
    }

    public static synchronized HwWmpFastBackLteManager getInstance() {
        HwWmpFastBackLteManager hwWmpFastBackLteManager;
        synchronized (HwWmpFastBackLteManager.class) {
            if (mHwWmpFastBackLteManager == null) {
                mHwWmpFastBackLteManager = new HwWmpFastBackLteManager();
            }
            hwWmpFastBackLteManager = mHwWmpFastBackLteManager;
        }
        return hwWmpFastBackLteManager;
    }

    public void SendDataToBooster(HwWmpFastBackLte mBack) {
        LogUtil.i("SendDataToBooster");
        Bundle data = new Bundle();
        data.putInt("SubId", mBack.mSubId);
        data.putInt("PlmnId", mBack.mPlmnId);
        data.putInt("Rat", mBack.mRat);
        data.putInt("Earfcn", mBack.mEarfcn);
        data.putInt("Lai", mBack.mLai);
        data.putInt("CellId", mBack.mCellId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Back2Lte: SubID=");
        stringBuilder.append(mBack.mSubId);
        stringBuilder.append(" PlmnId=");
        stringBuilder.append(mBack.mPlmnId);
        stringBuilder.append(" Rat=");
        stringBuilder.append(mBack.mRat);
        stringBuilder.append(" Earfcn=");
        stringBuilder.append(mBack.mEarfcn);
        stringBuilder.append(" Lai=");
        stringBuilder.append(mBack.mEarfcn);
        stringBuilder.append(" CellId=");
        stringBuilder.append(mBack.mCellId);
        LogUtil.d(stringBuilder.toString());
        int ret = this.mBooster.reportBoosterPara("com.android.server.hidata.appqoe", 304, data);
        if (ret != 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("reportBoosterPara failed, ret=");
            stringBuilder2.append(ret);
            LogUtil.e(stringBuilder2.toString());
        }
    }
}
