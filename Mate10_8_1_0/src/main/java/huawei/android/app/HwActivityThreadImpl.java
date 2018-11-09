package huawei.android.app;

import android.common.HwActivityThread;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;
import java.lang.reflect.Field;

public class HwActivityThreadImpl implements HwActivityThread {
    private static final String TAG = "HwActivityThreadImpl";
    private static HwActivityThreadImpl sInstance;

    public static synchronized HwActivityThreadImpl getDefault() {
        HwActivityThreadImpl hwActivityThreadImpl;
        synchronized (HwActivityThreadImpl.class) {
            if (sInstance == null) {
                sInstance = new HwActivityThreadImpl();
            }
            hwActivityThreadImpl = sInstance;
        }
        return hwActivityThreadImpl;
    }

    public void changeToSpecialModel(String pkgName) {
        String strHwModel = SystemProperties.get("ro.product.hw_model", "");
        if (pkgName != null && (strHwModel.equals("") ^ 1) != 0) {
            if (pkgName.equals("com.sina.weibo") || pkgName.equals("com.tencent.mobileqq")) {
                try {
                    Field field = Build.class.getField("MODEL");
                    field.setAccessible(true);
                    field.set(null, strHwModel);
                } catch (NoSuchFieldException e) {
                    Log.e(TAG, "modify Build.MODEL fail!");
                }
            }
        }
    }
}
