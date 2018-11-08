package tmsdkobf;

import android.content.Context;
import android.os.HandlerThread;
import com.qq.taf.jce.JceStruct;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import tmsdk.common.ErrorCode;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.roach.nest.ISharkCallBackNest;
import tmsdk.common.tcc.TccDiff;
import tmsdk.common.utils.q;

public class pt {
    public static final md KB = new md("r_entities_sp");

    public static void addTask(Runnable runnable, String str) {
        try {
            ps.i("addTask-task:[" + runnable + "]taskName:[" + str + "]");
            im.bJ().addTask(runnable, str);
        } catch (Object -l_2_R) {
            ps.h("e:[" + -l_2_R + "]");
        }
    }

    public static int bsPatch(String str, String str2, String str3, int i) {
        return TccDiff.bsPatch(str, str2, str3, i);
    }

    public static int download(String str, String str2, String str3) {
        Object -l_4_R;
        int -l_3_I;
        try {
            ps.i("download-url:[" + str + "]fileDir:[" + str2 + "]fileName:[" + str3 + "]");
            if (q.cK(str) || q.cK(str2) || q.cK(str3)) {
                return -57;
            }
            -l_4_R = new lx(TMSDKContext.getApplicaionContext());
            -l_4_R.bP(str2);
            -l_4_R.bQ(str3);
            -l_3_I = -l_4_R.a(null, str, false, null);
            return -l_3_I;
        } catch (Object -l_4_R2) {
            -l_3_I = ErrorCode.ERR_GET;
            ps.h("e:[" + -l_4_R2 + "]");
        }
    }

    public static String fileMd5(String str) {
        return TccDiff.fileMd5(str);
    }

    public static Context getAppContext() {
        return TMSDKContext.getApplicaionContext();
    }

    public static String getByteMd5(byte[] bArr) {
        return TccDiff.getByteMd5(bArr);
    }

    public static int getInt(String str, int i) {
        return KB.getInt(str, i);
    }

    public static long getLong(String str, long j) {
        return KB.getLong(str, j);
    }

    public static String getString(String str, String str2) {
        return KB.getString(str, str2);
    }

    public static HandlerThread newFreeHandlerThread(String str) {
        Object -l_1_R = null;
        try {
            ps.i("newFreeHandlerThread-taskName:[" + str + "]");
            -l_1_R = im.bJ().newFreeHandlerThread(str);
        } catch (Object -l_2_R) {
            ps.h("e:[" + -l_2_R + "]");
        }
        return -l_1_R;
    }

    public static Thread newFreeThread(Runnable runnable, String str) {
        Object -l_2_R = null;
        try {
            ps.i("newFreeThread-task:[" + runnable + "]taskName:[" + str + "]");
            -l_2_R = im.bJ().newFreeThread(runnable, str);
        } catch (Object -l_3_R) {
            ps.h("e:[" + -l_3_R + "]");
        }
        return -l_2_R;
    }

    public static void putInt(String str, int i) {
        KB.a(str, i, true);
    }

    public static void putLong(String str, long j) {
        KB.a(str, j, true);
    }

    public static void putString(String str, String str2) {
        KB.a(str, str2, true);
    }

    public static void remove(String str) {
        KB.remove(str);
    }

    public static int runHttpSession(int i, String str, String str2, HashMap<String, Object> hashMap, String str3, Class<?> cls, AtomicReference<Object> atomicReference) {
        int -l_7_I;
        try {
            if (q.cK(str) || q.cK(str2) || hashMap == null) {
                return -57;
            }
            pq -l_8_R = (pq) ManagerCreatorC.getManager(pq.class);
            hashMap.put("phonetype", -l_8_R.hV().ht());
            hashMap.put("userinfo", -l_8_R.hV().hu());
            Object -l_9_R = new pp(i, new pn(str, str2));
            -l_9_R.Kv = hashMap;
            -l_7_I = -l_8_R.hV().a(-l_9_R);
            if (-l_7_I != 0) {
                return -l_7_I;
            }
            if (!(!q.cJ(str3) || cls == null || atomicReference == null)) {
                Object -l_10_R = -l_8_R.hV().a(-l_9_R.Kx, str3, cls.newInstance());
                if (-l_10_R != null) {
                    atomicReference.set(-l_10_R);
                }
            }
            return -l_7_I;
        } catch (Throwable th) {
            -l_7_I = ErrorCode.ERR_WUP;
        }
    }

    public static void saveActionData(int i) {
        try {
            kt.saveActionData(i);
        } catch (Throwable th) {
        }
    }

    public static void saveMultiValueData(int i, int i2) {
        try {
            kt.saveMultiValueData(i, i2);
        } catch (Throwable th) {
        }
    }

    public static void saveStringData(int i, String str) {
        try {
            kt.e(i, str);
        } catch (Throwable th) {
        }
    }

    public static boolean sendShark(int i, JceStruct jceStruct, JceStruct jceStruct2, int i2, final ISharkCallBackNest iSharkCallBackNest, long j) {
        Object -l_7_R;
        if (jceStruct == null || iSharkCallBackNest == null) {
            return false;
        }
        try {
            -l_7_R = im.bK();
            ps.i("sendShark-sharkProxy:[" + -l_7_R + "]cmdId:[" + i + "]req:[" + jceStruct + "]resp:[" + jceStruct2 + "]flag:[" + i2 + "]callback:[" + iSharkCallBackNest + "]callBackTimeout:[" + j + "]");
            return -l_7_R.a(i, jceStruct, jceStruct2, i2, new jy() {
                public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                    ps.i("sendShark-onFinish-retCode:[" + i3 + "]dataRetCode:[" + i4 + "]");
                    iSharkCallBackNest.onFinish(i, i2, i3, i4, jceStruct);
                }
            }, j) != null;
        } catch (Object -l_7_R2) {
            ps.h("e:[" + -l_7_R2 + "]");
            return false;
        }
    }

    public static void tryReportData() {
        try {
            kr.p(true);
        } catch (Throwable th) {
        }
    }
}
