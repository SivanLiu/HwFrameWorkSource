package tmsdkobf;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import tmsdk.common.OfflineVideo;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.lang.MultiLangManager;
import tmsdk.common.tcc.VideoMetaRunner;
import tmsdk.common.utils.ScriptHelper;
import tmsdk.common.utils.f;

public class rp {
    HashMap<String, rn> PU = new HashMap();
    String[] PV;
    final boolean PW;

    public rp(String[] strArr, boolean z) {
        this.PW = z;
        this.PU.put("youku", new rw());
        this.PU.put("qiyi", new rs());
        this.PU.put("qqlive", new rr());
        this.PU.put("sohu", new rt());
        this.PU.put("storm", new ru(this.PW));
        this.PV = strArr;
    }

    private List<OfflineVideo> L(List<OfflineVideo> list) {
        if (list.size() == 0) {
            return list;
        }
        Object -l_2_R = new ArrayList();
        Object -l_3_R = new ArrayList();
        String -l_4_R = null;
        for (OfflineVideo -l_6_R : list) {
            if (-l_4_R == null || -l_4_R.equals(-l_6_R.mAdapter)) {
                -l_4_R = -l_6_R.mAdapter;
                -l_3_R.add(-l_6_R);
            } else {
                M(-l_3_R);
                -l_2_R.addAll(-l_3_R);
                -l_3_R.clear();
                -l_4_R = -l_6_R.mAdapter;
                -l_3_R.add(-l_6_R);
            }
        }
        M(-l_3_R);
        -l_2_R.addAll(-l_3_R);
        return -l_2_R;
    }

    private void M(List<OfflineVideo> list) {
        Collections.sort(list, new Comparator<OfflineVideo>(this) {
            final /* synthetic */ rp PX;

            {
                this.PX = r1;
            }

            public int a(OfflineVideo offlineVideo, OfflineVideo offlineVideo2) {
                int i = 0;
                int i2 = -1;
                int i3 = 1;
                int -l_3_I = offlineVideo.getStatus();
                int -l_4_I = offlineVideo2.getStatus();
                if (-l_3_I != -l_4_I) {
                    if (-l_3_I > -l_4_I) {
                        i3 = -1;
                    }
                    return i3;
                } else if (offlineVideo.mSize == offlineVideo2.mSize) {
                    return 0;
                } else {
                    if (offlineVideo.mSize <= offlineVideo2.mSize) {
                        i = 1;
                    }
                    if (i != 0) {
                        i2 = 1;
                    }
                    return i2;
                }
            }

            public /* synthetic */ int compare(Object obj, Object obj2) {
                return a((OfflineVideo) obj, (OfflineVideo) obj2);
            }
        });
    }

    private List<OfflineVideo> N(List<OfflineVideo> list) {
        if (list == null || list.size() == 0) {
            return list;
        }
        int -l_2_I = ScriptHelper.acquireRoot();
        f.h("VideoMetaRetriever", "root state " + -l_2_I);
        if (-l_2_I != 0) {
            return list;
        }
        OfflineVideo.dumpToFile(list);
        Object -l_3_R = TMSDKContext.getApplicaionContext().getApplicationInfo().sourceDir;
        Object -l_4_R = VideoMetaRunner.class.getName();
        Object -l_5_R = String.format("export CLASSPATH=%s && exec app_process /system/bin %s %s", new Object[]{-l_3_R, -l_4_R, OfflineVideo.getOfflineDatabase()});
        Object -l_6_R = "/data/local/offline_video_script";
        Object -l_7_R = String.format("echo '%s' > %s", new Object[]{-l_5_R, -l_6_R});
        Object -l_8_R = String.format("chmod 755 %s", new Object[]{-l_6_R});
        f.h("PiDeepClean", "cmd " + -l_7_R);
        f.h("VideoMetaRetriever", "cmd " + -l_7_R);
        Object -l_9_R = ScriptHelper.runScript(10000, -l_7_R, -l_8_R, -l_6_R);
        Object -l_10_R = OfflineVideo.readOfflineVideos();
        if (-l_10_R == null) {
            List<OfflineVideo> -l_10_R2 = list;
        }
        return -l_10_R;
    }

    private String[] dn(String str) {
        if (this.PV == null) {
            return null;
        }
        Object -l_2_R = str.toLowerCase();
        for (Object -l_6_R : this.PV) {
            if (-l_2_R.startsWith(-l_6_R)) {
                return do(-l_6_R);
            }
        }
        return null;
    }

    public String[] do(String str) {
        Object -l_4_R = rm.km().j(str, ((MultiLangManager) ManagerCreatorC.getManager(MultiLangManager.class)).isENG());
        if (-l_4_R == null || -l_4_R.size() == 0) {
            return null;
        }
        Object -l_7_R = new ArrayList();
        Object -l_8_R = new ArrayList();
        for (String -l_10_R : -l_4_R.keySet()) {
            try {
                -l_7_R.add(-l_10_R);
                -l_8_R.add(-l_4_R.get(-l_10_R));
            } catch (Exception e) {
            }
        }
        Object -l_9_R = new rj();
        -l_9_R.init();
        Object -l_10_R2 = -l_9_R.J(-l_7_R);
        if (-l_10_R2 != null) {
            Object -l_6_R = -l_10_R2;
            Object -l_5_R = -l_9_R.cS(-l_10_R2);
        } else {
            int -l_11_I = -l_9_R.K(-l_7_R);
            if (-l_11_I == -1) {
                -l_11_I = 0;
            }
            String -l_6_R2 = (String) -l_7_R.get(-l_11_I);
            String -l_5_R2 = (String) -l_8_R.get(-l_11_I);
        }
        if (-l_5_R == null || -l_6_R == null) {
            return null;
        }
        f.e("xx", -l_6_R + "  " + -l_5_R);
        return new String[]{-l_5_R.trim(), -l_6_R.trim()};
    }

    public List<OfflineVideo> ko() {
        Object<ro> -l_1_R = rv.kp();
        if (-l_1_R == null || -l_1_R.size() == 0) {
            return null;
        }
        List -l_2_R = rh.jZ();
        List -l_3_R = new ArrayList();
        for (ro -l_5_R : -l_1_R) {
            if (!TextUtils.isEmpty(-l_5_R.mAdapter)) {
                rn -l_6_R = (rn) this.PU.get(-l_5_R.mAdapter);
                if (-l_6_R != null) {
                    List<OfflineVideo> -l_7_R = -l_6_R.a(-l_5_R);
                    if (!(-l_7_R == null || -l_7_R.size() == 0)) {
                        Object -l_9_R = dn(rh.a(((OfflineVideo) -l_7_R.get(0)).mPath, -l_2_R));
                        for (OfflineVideo -l_11_R : -l_7_R) {
                            -l_11_R.mAdapter = -l_5_R.mAdapter;
                            -l_11_R.mPlayers = -l_5_R.mPlayers;
                            if (-l_9_R != null) {
                                -l_11_R.mAppName = -l_9_R[0];
                                -l_11_R.mPackage = -l_9_R[1];
                            }
                        }
                        -l_3_R.addAll(-l_7_R);
                    }
                }
            }
        }
        Object<OfflineVideo> -l_3_R2 = L(N(-l_3_R));
        long -l_6_J = 0;
        long -l_8_J = 0;
        long -l_10_J = 0;
        long -l_12_J = 0;
        int -l_15_I = 0;
        int -l_16_I = 0;
        int -l_17_I = 0;
        int -l_18_I = 0;
        for (OfflineVideo -l_20_R : -l_3_R2) {
            switch (-l_20_R.getStatus()) {
                case 0:
                    -l_6_J += -l_20_R.mSize;
                    -l_15_I++;
                    break;
                case 1:
                    -l_8_J += -l_20_R.mSize;
                    -l_16_I++;
                    break;
                case 2:
                    -l_10_J += -l_20_R.mSize;
                    -l_17_I++;
                    break;
                case 3:
                    -l_12_J += -l_20_R.mSize;
                    -l_18_I++;
                    break;
                default:
                    break;
            }
        }
        int -l_14_I = ((-l_15_I + -l_16_I) + -l_17_I) + -l_18_I;
        f.h("PiDeepClean", "EMID_Secure_DeepClean_OfflineVideo_ScanResult " + (((((-l_6_J + -l_8_J) + -l_10_J) + -l_12_J) >> 10) + "," + -l_14_I + "," + (-l_6_J >> 10) + "," + -l_15_I + "," + (-l_8_J >> 10) + "," + -l_16_I + "," + (-l_10_J >> 10) + "," + -l_17_I + "," + (-l_12_J >> 10) + "," + -l_18_I));
        return -l_3_R2;
    }
}
