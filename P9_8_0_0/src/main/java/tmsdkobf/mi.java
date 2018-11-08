package tmsdkobf;

import android.os.IBinder;
import android.os.IInterface;
import java.lang.reflect.Method;
import java.util.HashMap;

public final class mi {
    private static IBinder mRemote;
    private static HashMap<String, IBinder> sCache;
    private static Class<?> zL;
    private static Method zM;
    private static Method zN;
    private static Method zO;
    private static Method zP;

    static {
        Object -l_0_R;
        try {
            zL = Class.forName("android.os.ServiceManager");
            zM = zL.getDeclaredMethod("getService", new Class[]{String.class});
            zN = zL.getDeclaredMethod("addService", new Class[]{String.class, IBinder.class});
            zO = zL.getDeclaredMethod("checkService", new Class[]{String.class});
            zP = zL.getDeclaredMethod("listServices", new Class[0]);
            -l_0_R = zL.getDeclaredField("sCache");
            -l_0_R.setAccessible(true);
            sCache = (HashMap) -l_0_R.get(null);
            Object -l_1_R = zL.getDeclaredField("sServiceManager");
            -l_1_R.setAccessible(true);
            mRemote = ((IInterface) -l_1_R.get(null)).asBinder();
        } catch (Object -l_0_R2) {
            -l_0_R2.printStackTrace();
        } catch (Object -l_0_R22) {
            -l_0_R22.printStackTrace();
        } catch (Object -l_0_R222) {
            -l_0_R222.printStackTrace();
        } catch (Object -l_0_R2222) {
            -l_0_R2222.printStackTrace();
        } catch (Object -l_0_R22222) {
            -l_0_R22222.printStackTrace();
        } catch (Object -l_0_R222222) {
            -l_0_R222222.printStackTrace();
        }
    }

    private static Object a(Method method, Object... objArr) {
        Object -l_2_R = null;
        try {
            -l_2_R = method.invoke(null, objArr);
        } catch (Object -l_3_R) {
            -l_3_R.printStackTrace();
        } catch (Object -l_3_R2) {
            -l_3_R2.printStackTrace();
        } catch (Object -l_3_R22) {
            -l_3_R22.printStackTrace();
        }
        return -l_2_R;
    }

    public static IBinder checkService(String str) {
        return (IBinder) a(zO, str);
    }

    public static IBinder getService(String str) {
        return (IBinder) a(zM, str);
    }
}
