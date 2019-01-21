package android.rms;

import android.os.Bundle;
import android.os.SystemClock;
import android.rms.config.ResourceConfig;
import android.rms.control.ResourceFlowControl;
import android.rms.utils.Utils;
import android.util.Log;

public class HwSysSpeedRes extends HwSysResImpl {
    private static final String TAG = "RMS.HwSysSpeedRes";
    private static int[] mWhiteListTypes = new int[]{0};
    protected int mOverloadNumber;
    protected int mOverloadPeriod;
    protected long mPreReportTime;
    protected ResourceFlowControl mResourceFlowControl;
    private String mTag;

    protected HwSysSpeedRes(int resourceType, String tag) {
        this(resourceType, tag, mWhiteListTypes);
    }

    protected HwSysSpeedRes(int resourceType, String tag, int[] mWhiteListTypes) {
        super(resourceType, tag, mWhiteListTypes);
        this.mTag = TAG;
        this.mPreReportTime = 0;
        this.mResourceFlowControl = new ResourceFlowControl();
        this.mTag = tag;
    }

    protected int getSpeedOverloadStrategy(int typeID) {
        int strategy = this.mResourceConfig[typeID].getResourceStrategy();
        int maxPeriod = this.mResourceConfig[typeID].getResourceMaxPeroid();
        if (Utils.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getOverloadStrategy  resource_strategy /");
            stringBuilder.append(strategy);
            stringBuilder.append(" mOverloadPeriod/");
            stringBuilder.append(this.mOverloadPeriod);
            stringBuilder.append(" MaxPeriod/");
            stringBuilder.append(maxPeriod);
            Log.d(str, stringBuilder.toString());
        }
        if (this.mOverloadPeriod >= maxPeriod) {
            return strategy;
        }
        return 1;
    }

    protected boolean isResourceSpeedOverload(int callingUid, String pkg, int typeID) {
        int threshold;
        ResourceConfig resourceConfig;
        long id = super.getResourceId(callingUid, pkg, typeID);
        ResourceConfig config = this.mResourceConfig[typeID];
        int threshold2 = config.getResourceThreshold();
        int loopInterval = config.getLoopInterval();
        if (this.mResourceFlowControl.checkSpeedOverload(id, threshold2, loopInterval)) {
            int maxPeriod = config.getResourceMaxPeroid();
            int totalTimeInterval = config.getTotalLoopInterval();
            this.mOverloadPeriod = this.mResourceFlowControl.getOverloadPeroid(id);
            if (this.mOverloadPeriod >= maxPeriod) {
                int maxPeriod2;
                if (this.mResourceFlowControl.isReportTime(id, loopInterval, this.mPreReportTime, totalTimeInterval) || this.mResourceType == 18) {
                    maxPeriod2 = maxPeriod;
                    threshold = threshold2;
                    Bundle bundle = createBundleForResource(id, typeID, config, this.mResourceFlowControl);
                    this.mOverloadNumber = this.mResourceFlowControl.getOverloadNumber(id);
                    maxPeriod = this.mResourceFlowControl.getCountInPeroid(id);
                    this.mResourceManger.recordResourceOverloadStatus(callingUid, pkg, this.mResourceType, this.mOverloadNumber, this.mOverloadPeriod, maxPeriod, bundle);
                    this.mPreReportTime = SystemClock.uptimeMillis();
                    String str = this.mTag;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("HwSysSpeedRes is threshold Overload  id=");
                    stringBuilder.append(id);
                    stringBuilder.append(" pkg=");
                    stringBuilder.append(pkg);
                    stringBuilder.append(" threshold=");
                    stringBuilder.append(threshold);
                    stringBuilder.append(" OverloadPeriod=");
                    stringBuilder.append(this.mOverloadPeriod);
                    stringBuilder.append(" maxPeriod=");
                    stringBuilder.append(maxPeriod2);
                    Log.i(str, stringBuilder.toString());
                } else {
                    maxPeriod2 = maxPeriod;
                    int i = loopInterval;
                    threshold = threshold2;
                    resourceConfig = config;
                    threshold2 = pkg;
                }
                return true;
            }
        }
        threshold = threshold2;
        resourceConfig = config;
        threshold2 = pkg;
        return false;
    }

    protected Bundle createBundleForResource(long id, int typeID, ResourceConfig config, ResourceFlowControl resourceFlowControl) {
        return null;
    }
}
