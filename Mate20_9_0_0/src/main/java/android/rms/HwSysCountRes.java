package android.rms;

import android.os.Bundle;
import android.os.SystemClock;
import android.rms.config.ResourceConfig;
import android.rms.control.ResourceCountControl;
import android.rms.utils.Utils;
import android.util.Log;

public class HwSysCountRes extends HwSysResImpl {
    private static final String TAG = "RMS.HwSysCountRes";
    protected long mPreReportTime;
    protected ResourceCountControl mResourceCountControl;
    private String mTag;

    protected HwSysCountRes(int resourceType, String tag, int[] whiteListTypes) {
        super(resourceType, tag, whiteListTypes);
        this.mTag = TAG;
        this.mResourceCountControl = new ResourceCountControl();
        this.mPreReportTime = 0;
        this.mTag = tag;
    }

    protected HwSysCountRes(int resourceType, String tag) {
        this(resourceType, tag, new int[]{0});
    }

    protected boolean isResourceCountOverload(int callingUid, String pkg, int typeID, int count) {
        long id = super.getResourceId(callingUid, pkg, typeID);
        ResourceConfig config = this.mResourceConfig[typeID];
        int softThreshold = config.getResouceWarningThreshold();
        int hardThreshold = config.getResouceUrgentThreshold();
        int normalThreshold = config.getResouceNormalThreshold();
        int timeInterval = config.getLoopInterval();
        int totalTimeInterval = config.getTotalLoopInterval();
        int i;
        int hardThreshold2;
        long id2;
        ResourceConfig config2;
        if (this.mResourceCountControl.checkCountOverload(id, softThreshold, hardThreshold, normalThreshold, count, this.mResourceType)) {
            if (Utils.DEBUG || Utils.HWFLOW) {
                String str = this.mTag;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("HwSysCountRes is threshold Overload  id=");
                stringBuilder.append(id);
                stringBuilder.append(" CurrentCount =");
                stringBuilder.append(count);
                stringBuilder.append(" softThreshold=");
                stringBuilder.append(softThreshold);
                stringBuilder.append(" hardThreshold=");
                stringBuilder.append(hardThreshold);
                Log.i(str, stringBuilder.toString());
            } else {
                i = count;
            }
            hardThreshold2 = hardThreshold;
            id2 = id;
            config2 = config;
            if (this.mResourceCountControl.isReportTime(id, timeInterval, this.mPreReportTime, totalTimeInterval)) {
                int overloadNum = this.mResourceCountControl.getOverloadNumber(id2);
                int totalCount = this.mResourceCountControl.getTotalCount(id2);
                Bundle mBundle = createBundleForResource(id2, typeID, config2, this.mResourceCountControl, pkg);
                this.mResourceManger.recordResourceOverloadStatus(callingUid, pkg, this.mResourceType, overloadNum, 0, totalCount, mBundle);
                this.mPreReportTime = SystemClock.uptimeMillis();
            }
            if (this.mResourceCountControl.getTotalCount(id2) > hardThreshold2) {
                return true;
            }
        }
        i = count;
        hardThreshold2 = hardThreshold;
        int i2 = softThreshold;
        id2 = id;
        config2 = config;
        return false;
    }

    protected boolean isResourceCountOverload(int callingUid, String pkg, int typeID) {
        return isResourceCountOverload(callingUid, pkg, typeID, -1);
    }

    protected int getCount(int callingUid, String pkg, int typeID) {
        return this.mResourceCountControl.getCount(super.getResourceId(callingUid, pkg, typeID));
    }

    protected Bundle createBundleForResource(long id, int typeID, ResourceConfig config, ResourceCountControl mResourceCountControl, String pkg) {
        return null;
    }
}
