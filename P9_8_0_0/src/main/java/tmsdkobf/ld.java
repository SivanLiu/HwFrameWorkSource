package tmsdkobf;

import android.text.TextUtils;
import com.qq.taf.jce.JceStruct;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import tmsdk.common.TMSDKContext;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.update.CheckResult;
import tmsdk.common.module.update.ICheckListener;
import tmsdk.common.module.update.IUpdateListener;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.module.update.UpdateInfo;
import tmsdk.common.module.update.UpdateManager;

public class ld {
    public static void ep() {
        try {
            ea -l_1_R = (ea) mk.a(TMSDKContext.getApplicaionContext(), UpdateConfig.APP_USAGE_PRE_NAME, UpdateConfig.intToString(40545), new ea(), "UTF-8");
            if (-l_1_R != null && -l_1_R.iC != null && -l_1_R.iC.size() > 0) {
                JSONArray -l_2_R = new JSONArray();
                Object -l_3_R = -l_1_R.iC.iterator();
                while (-l_3_R.hasNext()) {
                    dz -l_4_R = (dz) -l_3_R.next();
                    try {
                        int -l_5_I = Integer.valueOf(-l_4_R.iu).intValue();
                        String -l_6_R = -l_4_R.iv;
                        String -l_7_R = -l_4_R.iw;
                        if (!(TextUtils.isEmpty(-l_6_R) || TextUtils.isEmpty(-l_7_R))) {
                            Object -l_8_R = TMServiceFactory.getSystemInfoService().a(-l_6_R, 1);
                            if (-l_8_R != null) {
                                JSONObject -l_9_R = new JSONObject();
                                long -l_10_J = 0;
                                long -l_12_J = 0;
                                if (new File(-l_7_R).exists()) {
                                    try {
                                        Object -l_17_R = Class.forName("android.system.Os").getMethod("stat", new Class[]{String.class}).invoke(null, new Object[]{-l_7_R});
                                        Object -l_18_R = -l_17_R.getClass();
                                        -l_10_J = -l_18_R.getField("st_mtime").getLong(-l_17_R);
                                        -l_12_J = -l_18_R.getField("st_atime").getLong(-l_17_R);
                                    } catch (Throwable th) {
                                        -l_10_J = -1;
                                        -l_12_J = -1;
                                    }
                                }
                                -l_9_R.put("id", String.valueOf(-l_5_I));
                                -l_9_R.put("version_code", String.valueOf(-l_8_R.getVersionCode()));
                                -l_9_R.put("mtime", String.valueOf(-l_10_J));
                                -l_9_R.put("atime", String.valueOf(-l_12_J));
                                -l_9_R.put("is_build_in", !-l_8_R.hx() ? "0" : "1");
                                -l_2_R.put(-l_9_R);
                            }
                        }
                    } catch (Throwable th2) {
                    }
                }
                if (-l_2_R.length() > 0) {
                    la.a(-l_2_R.toString(), getPath(), 151);
                }
                kz.o(System.currentTimeMillis() / 1000);
            }
        } catch (Throwable th3) {
        }
    }

    public static void eq() {
        try {
            final Object -l_0_R = getPath();
            Object -l_1_R = la.bD(-l_0_R);
            if (-l_1_R != null && !-l_1_R.isEmpty()) {
                JceStruct -l_2_R = new ao(151, new ArrayList());
                Object -l_3_R = -l_1_R.iterator();
                while (-l_3_R.hasNext()) {
                    try {
                        Object -l_5_R = new JSONArray((String) -l_3_R.next());
                        int -l_6_I = -l_5_R.length();
                        for (int -l_7_I = 0; -l_7_I < -l_6_I; -l_7_I++) {
                            JSONObject -l_8_R = (JSONObject) -l_5_R.get(-l_7_I);
                            Object -l_9_R = new ap(new HashMap());
                            -l_9_R.bG.put(Integer.valueOf(1), -l_8_R.getString("id"));
                            -l_9_R.bG.put(Integer.valueOf(2), -l_8_R.getString("version_code"));
                            -l_9_R.bG.put(Integer.valueOf(3), -l_8_R.getString("mtime"));
                            -l_9_R.bG.put(Integer.valueOf(4), -l_8_R.getString("atime"));
                            -l_9_R.bG.put(Integer.valueOf(5), -l_8_R.getString("is_build_in"));
                            -l_2_R.bD.add(-l_9_R);
                        }
                    } catch (Throwable th) {
                    }
                }
                -l_3_R = im.bK();
                if (-l_2_R.bD.size() > 0 && -l_3_R != null) {
                    -l_3_R.a(4060, -l_2_R, null, 0, new jy() {
                        public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                            if (i3 == 0 && i4 == 0) {
                                la.bF(-l_0_R);
                                kz.n(System.currentTimeMillis() / 1000);
                            }
                        }
                    });
                }
            }
        } catch (Throwable th2) {
        }
    }

    public static void es() {
        try {
            Object -l_0_R = new File(TMSDKContext.getApplicaionContext().getFilesDir().getAbsolutePath() + File.separator + UpdateConfig.APP_USAGE_PRE_NAME);
            if (-l_0_R.exists()) {
                -l_0_R.delete();
            }
        } catch (Throwable th) {
        }
    }

    public static synchronized void et() {
        synchronized (ld.class) {
            final UpdateManager -l_0_R = (UpdateManager) ManagerCreatorC.getManager(UpdateManager.class);
            -l_0_R.check(Long.MIN_VALUE, new ICheckListener() {
                public void onCheckCanceled() {
                }

                public void onCheckEvent(int i) {
                }

                public void onCheckFinished(CheckResult -l_2_R) {
                    if (-l_2_R != null) {
                        -l_0_R.update(-l_2_R.mUpdateInfoList, new IUpdateListener(this) {
                            final /* synthetic */ AnonymousClass2 yf;

                            {
                                this.yf = r1;
                            }

                            public void onProgressChanged(UpdateInfo updateInfo, int i) {
                            }

                            public void onUpdateCanceled() {
                            }

                            public void onUpdateEvent(UpdateInfo updateInfo, int i) {
                            }

                            public void onUpdateFinished() {
                            }

                            public void onUpdateStarted() {
                            }
                        });
                    }
                }

                public void onCheckStarted() {
                }
            }, -1);
        }
    }

    public static String getPath() {
        return TMSDKContext.getApplicaionContext().getFilesDir().getAbsolutePath() + File.separator + "d_" + 151;
    }
}
