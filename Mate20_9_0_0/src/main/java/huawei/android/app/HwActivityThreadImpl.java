package huawei.android.app;

import android.app.ActivityThread;
import android.app.Application;
import android.common.HwActivityThread;
import android.common.HwFrameworkFactory;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.IAwareBitmapCacher;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.rms.iaware.AwareAppLiteSysLoadManager;
import android.rms.iaware.AwareAppScheduleManager;
import android.util.Log;
import com.huawei.android.content.pm.HwPackageManager;
import java.lang.reflect.Field;

public class HwActivityThreadImpl implements HwActivityThread {
    private static final String TAG = "HwActivityThreadImpl";
    private static final boolean mDecodeBitmapOptEnable = SystemProperties.getBoolean("persist.kirin.decodebitmap_opt", false);
    private static HwActivityThreadImpl sInstance;
    private int mPerfOptEnable = -1;

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
        if (pkgName != null && !strHwModel.equals("")) {
            if (pkgName.equals("com.sina.weibo") || pkgName.equals("com.tencent.mobileqq")) {
                try {
                    Field field = Build.class.getField("MODEL");
                    field.setAccessible(true);
                    field.set(null, strHwModel);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    Log.e(TAG, "modify Build.MODEL fail!");
                }
            }
        }
    }

    public int isPerfOptEnable(int optTypeId) {
        if (this.mPerfOptEnable != -1) {
            return this.mPerfOptEnable;
        }
        if (ActivityThread.currentActivityThread() == null) {
            return 0;
        }
        String packageName = ActivityThread.currentPackageName();
        if (packageName != null) {
            try {
                if (HwPackageManager.getService().isPerfOptEnable(packageName, optTypeId)) {
                    this.mPerfOptEnable = 1;
                } else {
                    this.mPerfOptEnable = 0;
                }
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        }
        this.mPerfOptEnable = 1;
        return this.mPerfOptEnable;
    }

    public boolean decodeBitmapOptEnable() {
        return mDecodeBitmapOptEnable;
    }

    public void reportBindApplicationToAware(Application app, String processName) {
        AwareAppScheduleManager.getInstance().init(processName, app);
        AwareAppLiteSysLoadManager.getInstance().init(processName, app);
        IAwareBitmapCacher obj = HwFrameworkFactory.getHwIAwareBitmapCacher();
        if (obj != null) {
            obj.init(processName, app);
        }
    }

    public Drawable getCacheDrawableFromAware(int resId, Resources wrapper, int cookie, AssetManager asset) {
        return AwareAppScheduleManager.getInstance().getCacheDrawableFromAware(resId, wrapper, cookie, asset);
    }

    public void postCacheDrawableToAware(int resId, Resources wrapper, long time, int cookie, AssetManager asset) {
        AwareAppScheduleManager.getInstance().postCacheDrawableToAware(resId, wrapper, time, cookie, asset);
    }

    public void hitDrawableCache(int resId) {
        AwareAppScheduleManager.getInstance().hitDrawableCache(resId);
    }

    public boolean getWechatScanOpt() {
        return AwareAppScheduleManager.getInstance().getWechatScanOpt();
    }

    public String getWechatScanActivity() {
        return AwareAppScheduleManager.getInstance().getWechatScanActivity();
    }

    public boolean isScene(int scene) {
        return AwareAppLiteSysLoadManager.getInstance().isInSysLoadScene(scene);
    }

    public boolean isLiteSysLoadEnable() {
        return AwareAppLiteSysLoadManager.getInstance().isLiteSysLoadEnable();
    }
}
