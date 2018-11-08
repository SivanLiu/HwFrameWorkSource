package tmsdkobf;

import android.text.TextUtils;
import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.utils.m;
import tmsdk.fg.module.cleanV2.RubbishEntity;

public class qy {
    private static ox Cq = ((ox) ManagerCreatorC.getManager(ox.class));

    private int a(ov ovVar, Map<String, ov> map) {
        ov -l_3_R = (ov) map.get(ovVar.getPackageName());
        if (-l_3_R == null) {
            return -1;
        }
        if (-l_3_R.getVersionCode() <= ovVar.getVersionCode()) {
            return -l_3_R.getVersionCode() != ovVar.getVersionCode() ? 1 : 0;
        } else {
            return 2;
        }
    }

    private String a(ra raVar, int i) {
        switch (i) {
            case -1:
                return m.cF("apk_not_installed");
            case 0:
                return m.cF("apk_installed");
            case 1:
                return m.cF("apk_new_version");
            case 2:
                return m.cF("apk_old_version");
            default:
                return null;
        }
    }

    private boolean a(ra raVar, String str, List<qt> list) {
        Object -l_5_R = null;
        if (raVar.ME == null) {
            return false;
        }
        for (String -l_7_R : raVar.ME) {
            if (str.startsWith(-l_7_R)) {
                -l_5_R = -l_7_R;
            }
        }
        if (list == null || r6 == null) {
            return false;
        }
        long -l_8_J = (Calendar.getInstance().getTimeInMillis() - new File(str).lastModified()) / 86400000;
        for (qt -l_11_R : list) {
            if (!TextUtils.isEmpty(-l_11_R.Om)) {
                Object -l_12_R = db(-l_11_R.Om);
                if ((-l_8_J < ((long) -l_12_R[0]) ? 1 : null) != null) {
                    continue;
                } else {
                    if ((-l_8_J > ((long) -l_12_R[1]) ? 1 : null) == null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean b(ov ovVar, Map<String, List<Integer>> map) {
        if (ovVar == null || map == null) {
            return false;
        }
        List -l_3_R = (List) map.get(ovVar.getPackageName());
        return (-l_3_R == null || -l_3_R.indexOf(Integer.valueOf(ovVar.getVersionCode())) == -1) ? false : true;
    }

    private int[] db(String str) {
        Object -l_2_R = str.split(",");
        Object -l_3_R = new int[2];
        -l_3_R[0] = !-l_2_R[0].equals("-") ? Integer.parseInt(-l_2_R[0]) : Integer.MIN_VALUE;
        -l_3_R[1] = !-l_2_R[1].equals("-") ? Integer.parseInt(-l_2_R[1]) : Integer.MAX_VALUE;
        return -l_3_R;
    }

    private String dc(String str) {
        int -l_2_I = str.lastIndexOf(File.separator);
        return -l_2_I < 0 ? str : str.substring(-l_2_I + 1);
    }

    private void f(ov ovVar) {
        if (ovVar.getAppName() == null) {
            Object -l_4_R;
            try {
                -l_4_R = TMSDKContext.getApplicaionContext().getPackageManager();
                ovVar.setAppName(-l_4_R.getApplicationLabel(-l_4_R.getApplicationInfo(ovVar.getPackageName(), 0)).toString());
            } catch (Object -l_4_R2) {
                -l_4_R2.printStackTrace();
            }
        }
    }

    protected RubbishEntity a(ra raVar, boolean z, String -l_13_R, long j, Map<String, List<Integer>> map, Map<String, ov> map2, List<qt> list) {
        RubbishEntity rubbishEntity = null;
        ov -l_11_R = null;
        try {
            -l_11_R = Cq.g(-l_13_R, 73);
        } catch (Throwable th) {
        }
        if (-l_11_R == null || -l_11_R.getPackageName() == null) {
            return new RubbishEntity(1, -l_13_R, true, j, dc(-l_13_R), null, m.cF("broken_apk"));
        }
        f(-l_11_R);
        int -l_12_I = a(-l_11_R, (Map) map2);
        String -l_10_R = a(raVar, -l_12_I);
        if (b(-l_11_R, map)) {
            return new RubbishEntity(2, -l_13_R, true, j, -l_11_R.getAppName(), -l_11_R.getPackageName(), m.cF("apk_repeated"));
        }
        if (-l_10_R == null) {
            -l_10_R = Integer.toString(-l_11_R.getVersionCode());
        }
        boolean -l_13_I = a(raVar, -l_13_R, list);
        if (2 == -l_12_I) {
            -l_13_I = true;
        }
        RubbishEntity rubbishEntity2;
        if (!z) {
            rubbishEntity2 = new RubbishEntity(2, -l_13_R, -l_13_I, j, -l_11_R.getAppName(), -l_11_R.getPackageName(), -l_10_R);
        } else if (-l_13_I) {
            rubbishEntity2 = new RubbishEntity(2, -l_13_R, -l_13_I, j, -l_11_R.getAppName(), -l_11_R.getPackageName(), -l_10_R);
        }
        return rubbishEntity;
    }
}
