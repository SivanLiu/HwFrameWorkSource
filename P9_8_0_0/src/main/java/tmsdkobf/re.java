package tmsdkobf;

import android.text.TextUtils;
import java.util.HashMap;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.fg.module.cleanV2.AppGroupDesc;

public class re {
    public static HashMap<Integer, AppGroupDesc> kd() {
        Object -l_0_R = new HashMap();
        ea -l_1_R = (ea) mk.a(TMSDKContext.getApplicaionContext(), UpdateConfig.DEEP_CLEAN_APPGROUP_DESC, UpdateConfig.intToString(40350), new ea(), "UTF-8");
        if (-l_1_R == null || -l_1_R.iC == null) {
            return null;
        }
        Object -l_2_R = -l_1_R.iC.iterator();
        while (-l_2_R.hasNext()) {
            dz -l_3_R = (dz) -l_2_R.next();
            if (!(TextUtils.isEmpty(-l_3_R.iu) || TextUtils.isEmpty(-l_3_R.iv) || TextUtils.isEmpty(-l_3_R.iw) || TextUtils.isEmpty(-l_3_R.ix) || TextUtils.isEmpty(-l_3_R.iy))) {
                try {
                    Object -l_4_R = new AppGroupDesc();
                    -l_4_R.mType = Integer.valueOf(-l_3_R.iu).intValue();
                    if (-l_4_R.mType == 1) {
                        -l_4_R.mGroupId = Integer.valueOf(-l_3_R.iv).intValue();
                        -l_4_R.mTitle = -l_3_R.iw;
                        -l_4_R.mDesc = -l_3_R.ix;
                        -l_4_R.mShowPhoto = -l_3_R.iy.equals("1");
                        if (!-l_3_R.iy.equals("1")) {
                            if (-l_3_R.iy.equals("0")) {
                            }
                        }
                        -l_0_R.put(Integer.valueOf(-l_4_R.mGroupId), -l_4_R);
                    }
                } catch (Exception e) {
                }
            }
        }
        -l_2_R = new AppGroupDesc();
        -l_2_R.mGroupId = 1001;
        -l_2_R.mTitle = "微信缓存";
        -l_2_R.mDesc = "删除后可联网重新下载";
        -l_2_R.mShowPhoto = false;
        -l_0_R.put(Integer.valueOf(-l_2_R.mGroupId), -l_2_R);
        Object -l_3_R2 = new AppGroupDesc();
        -l_3_R2.mGroupId = 1002;
        -l_3_R2.mTitle = "聊天产生的文件";
        -l_3_R2.mDesc = "删除后无法恢复";
        -l_3_R2.mShowPhoto = false;
        -l_0_R.put(Integer.valueOf(-l_3_R2.mGroupId), -l_3_R2);
        -l_2_R = new AppGroupDesc();
        -l_2_R.mGroupId = 2001;
        -l_2_R.mTitle = "手Q产生的缓存";
        -l_2_R.mDesc = "删除后可联网重新下载";
        -l_2_R.mShowPhoto = false;
        -l_0_R.put(Integer.valueOf(-l_2_R.mGroupId), -l_2_R);
        -l_3_R2 = new AppGroupDesc();
        -l_3_R2.mGroupId = 2002;
        -l_3_R2.mTitle = "聊天产生文件";
        -l_3_R2.mDesc = "删除后无法恢复";
        -l_3_R2.mShowPhoto = false;
        -l_0_R.put(Integer.valueOf(-l_3_R2.mGroupId), -l_3_R2);
        -l_3_R2 = new AppGroupDesc();
        -l_3_R2.mGroupId = 10;
        -l_3_R2.mTitle = "下载的文件";
        -l_3_R2.mDesc = "删除后文件将不可恢复，请再次确认是否需要删除。";
        -l_3_R2.mShowPhoto = false;
        -l_0_R.put(Integer.valueOf(-l_3_R2.mGroupId), -l_3_R2);
        return -l_0_R;
    }
}
