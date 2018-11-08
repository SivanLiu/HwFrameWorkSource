package tmsdkobf;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.utils.q;

public class se {
    public List<String> QO = new ArrayList();
    public List<a> QP = new ArrayList();
    public List<a> QQ = new ArrayList();
    public List<a> QR = new ArrayList();

    public static class a extends qt {
        public String mAdapter;
        public String[] mPlayers;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean V(Context context) {
        ea -l_2_R = (ea) mk.b(TMSDKContext.getApplicaionContext(), UpdateConfig.PROCESSMANAGER_WHITE_LIST_NAME, UpdateConfig.intToString(40006), new ea(), "UTF-8");
        if (-l_2_R == null || -l_2_R.iC == null) {
            return false;
        }
        Object -l_3_R = -l_2_R.iC.iterator();
        while (-l_3_R.hasNext()) {
            dz -l_4_R = (dz) -l_3_R.next();
            if (-l_4_R.iu != null) {
                try {
                    switch (Integer.valueOf(-l_4_R.iu).intValue()) {
                        case 3:
                            if (-l_4_R.iv == null) {
                                break;
                            }
                            this.QO.add(-l_4_R.iv);
                            break;
                        case 4:
                            break;
                        case 5:
                            Object -l_7_R = b(-l_4_R);
                            if (-l_7_R != null) {
                                if (!q.cK(-l_7_R.mAdapter)) {
                                    a(this.QR, -l_7_R);
                                    break;
                                }
                                a(this.QQ, -l_7_R);
                                break;
                            }
                            break;
                        case 6:
                            Object -l_6_R = b(-l_4_R);
                            if (-l_6_R == null) {
                                break;
                            }
                            a(this.QP, -l_6_R);
                            break;
                        default:
                            break;
                    }
                } catch (Object -l_5_R) {
                    -l_5_R.printStackTrace();
                }
            }
        }
        return true;
    }

    private static void a(List<a> list, a aVar) {
        if (q.cK(aVar.Ok)) {
            list.add(aVar);
            return;
        }
        int -l_2_I = 0;
        while (-l_2_I < list.size() && !q.cK(((a) list.get(-l_2_I)).Ok)) {
            -l_2_I++;
        }
        list.add(-l_2_I, aVar);
    }

    private static a b(dz dzVar) {
        Object -l_1_R = new a();
        -l_1_R.Ok = dzVar.iv;
        if (q.cJ(dzVar.iw)) {
            Object -l_2_R = dzVar.iw.split("&");
            if (-l_2_R != null) {
                Object -l_3_R = -l_2_R;
                for (Object -l_6_R : -l_2_R) {
                    if (-l_6_R.length() > 2) {
                        int -l_7_I = -l_6_R.charAt(0);
                        Object -l_8_R = -l_6_R.substring(2);
                        switch (-l_7_I) {
                            case 49:
                                -l_1_R.mFileName = -l_8_R;
                                break;
                            case 50:
                                -l_1_R.Ol = -l_8_R;
                                break;
                            case 51:
                                -l_1_R.Om = -l_8_R;
                                break;
                            case 52:
                                -l_1_R.On = -l_8_R;
                                break;
                            case 53:
                                -l_1_R.Oo = -l_8_R;
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        if (q.cJ(dzVar.ix)) {
            -l_1_R.mPlayers = dzVar.ix.split("&");
        }
        if (q.cJ(dzVar.iy)) {
            -l_1_R.mAdapter = dzVar.iy;
        }
        return -l_1_R;
    }

    public boolean U(Context context) {
        return V(context);
    }
}
