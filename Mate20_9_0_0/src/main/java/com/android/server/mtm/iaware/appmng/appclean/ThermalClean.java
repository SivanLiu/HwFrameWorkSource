package com.android.server.mtm.iaware.appmng.appclean;

import android.app.mtm.iaware.appmng.AppCleanParam;
import android.app.mtm.iaware.appmng.AppCleanParam.Builder;
import android.app.mtm.iaware.appmng.AppMngConstant.AppCleanSource;
import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.app.mtm.iaware.appmng.IAppCleanCallback;
import android.content.Context;
import android.os.RemoteException;
import android.rms.iaware.AwareLog;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.taskstatus.ProcessCleaner;
import java.util.ArrayList;
import java.util.List;

public class ThermalClean extends CleanSource {
    private static final String TAG = "ThermalClean";
    private IAppCleanCallback mCallback;
    private Context mContext;
    private AppCleanParam mParam;

    public ThermalClean(AppCleanParam param, IAppCleanCallback callback, Context context) {
        this.mParam = param;
        this.mContext = context;
        this.mCallback = callback;
    }

    public void clean() {
        if (this.mParam != null) {
            List<String> pkgList = this.mParam.getStringList();
            List<Integer> userIdList = this.mParam.getIntList();
            if (!pkgList.isEmpty()) {
                if (pkgList.size() != userIdList.size()) {
                    AwareLog.e(TAG, "size of pkglist should same to userIdlist");
                    return;
                }
                ArrayList<AwareProcessInfo> proclist = new ArrayList();
                int size = pkgList.size();
                for (int i = 0; i < size; i++) {
                    String packageName = (String) pkgList.get(i);
                    int userId = ((Integer) userIdList.get(i)).intValue();
                    ArrayList<AwareProcessInfo> procInfo = AwareProcessInfo.getAwareProcInfosFromPackage(packageName, userId);
                    if (procInfo.isEmpty()) {
                        proclist.add(CleanSource.getDeadAwareProcInfo(packageName, userId));
                    } else {
                        proclist.addAll(procInfo);
                    }
                }
                List<AwareProcessBlockInfo> policy = CleanSource.mergeBlock(DecisionMaker.getInstance().decideAll(proclist, this.mParam.getLevel(), AppMngFeature.APP_CLEAN, AppCleanSource.THERMAL));
                if (policy == null || policy.isEmpty()) {
                    AwareLog.e(TAG, "policy is empty");
                    return;
                }
                String str;
                StringBuilder stringBuilder;
                size = 0;
                for (AwareProcessBlockInfo block : policy) {
                    if (block != null) {
                        size += ProcessCleaner.getInstance(this.mContext).uniformClean(block, null, "Thermal");
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("pkg = ");
                        stringBuilder.append(block.mPackageName);
                        stringBuilder.append(", uid = ");
                        stringBuilder.append(block.mUid);
                        stringBuilder.append(", policy = ");
                        stringBuilder.append(block.mCleanType);
                        stringBuilder.append(", reason = ");
                        stringBuilder.append(block.mReason);
                        AwareLog.i(str, stringBuilder.toString());
                        updateHistory(AppCleanSource.THERMAL, block);
                        uploadToBigData(AppCleanSource.THERMAL, block);
                    }
                }
                AppCleanParam result = new Builder(this.mParam.getSource()).timeStamp(this.mParam.getTimeStamp()).killedCount(size).build();
                if (this.mCallback != null) {
                    try {
                        this.mCallback.onCleanFinish(result);
                    } catch (RemoteException e) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("RemoteExcption e = ");
                        stringBuilder.append(e);
                        AwareLog.e(str, stringBuilder.toString());
                    }
                }
            }
        }
    }
}
