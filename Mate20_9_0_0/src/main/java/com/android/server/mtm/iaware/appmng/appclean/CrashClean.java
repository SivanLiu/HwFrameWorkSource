package com.android.server.mtm.iaware.appmng.appclean;

import android.app.mtm.iaware.appmng.AppMngConstant.AppCleanSource;
import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.content.Context;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import com.android.server.mtm.iaware.appmng.AwareAppCleanerForCrash;
import com.android.server.mtm.iaware.appmng.AwareAppMngSortPolicy;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrashClean extends CleanSource {
    private static final String TAG = "CrashClean";
    private int mCleanCount;
    private Context mContext;
    private int mLevel;
    private String mPkgName;
    private int mUserId;

    public CrashClean(int userid, int level, String pkgname, Context context) {
        this.mUserId = userid;
        this.mLevel = level;
        this.mPkgName = pkgname;
        this.mContext = context;
    }

    public void clean() {
        if (this.mPkgName != null) {
            ArrayList<AwareProcessInfo> proclist = AwareProcessInfo.getAwareProcInfosFromPackage(this.mPkgName, this.mUserId);
            if (proclist.isEmpty()) {
                proclist.add(CleanSource.getDeadAwareProcInfo(this.mPkgName, this.mUserId));
            }
            List<AwareProcessBlockInfo> info = CleanSource.mergeBlock(DecisionMaker.getInstance().decideAll(proclist, this.mLevel, AppMngFeature.APP_CLEAN, AppCleanSource.CRASH));
            if (info == null || info.isEmpty()) {
                AwareLog.e(TAG, "info is empty");
                return;
            }
            Map<Integer, List<AwareProcessBlockInfo>> srcProcList = new ArrayMap();
            srcProcList.put(Integer.valueOf(2), info);
            this.mCleanCount = AwareAppCleanerForCrash.getInstance(this.mContext).execute(new AwareAppMngSortPolicy(null, srcProcList), null);
            for (AwareProcessBlockInfo block : info) {
                if (block != null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("pkg = ");
                    stringBuilder.append(block.mPackageName);
                    stringBuilder.append(", uid = ");
                    stringBuilder.append(block.mUid);
                    stringBuilder.append(", policy = ");
                    stringBuilder.append(block.mCleanType);
                    stringBuilder.append(", reason = ");
                    stringBuilder.append(block.mReason);
                    AwareLog.i(str, stringBuilder.toString());
                    updateHistory(AppCleanSource.CRASH, block);
                    uploadToBigData(AppCleanSource.CRASH, block);
                }
            }
        }
    }

    public int getCleanCount() {
        return this.mCleanCount;
    }
}
