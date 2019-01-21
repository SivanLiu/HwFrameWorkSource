package android.aps;

import android.aps.IHwApsManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import java.util.List;

public class HwApsManager implements IApsManager {
    private static final String TAG = "HwApsManager";
    private static HwApsManager sInstance;
    private IHwApsManager mApsService;

    private HwApsManager() {
    }

    private boolean checkApsManagerService() {
        if (this.mApsService == null) {
            this.mApsService = Stub.asInterface(ServiceManager.getService("aps_service"));
        }
        if (this.mApsService != null) {
            return true;
        }
        Slog.e(TAG, "checkApsManagerService->service is not started yet");
        return false;
    }

    private boolean checkApsInfo(ApsAppInfo info) {
        if (info == null) {
            return false;
        }
        float resolutionRatio = info.getResolutionRatio();
        int fps = info.getFrameRatio();
        int brightnessPercent = info.getBrightnessPercent();
        int texturePercent = info.getTexturePercent();
        String str;
        StringBuilder stringBuilder;
        if (resolutionRatio <= 0.25f || resolutionRatio > 1.0f) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("check APSinfo invalid resolution ratio =");
            stringBuilder.append(resolutionRatio);
            Slog.e(str, stringBuilder.toString());
            return false;
        } else if (fps < 15 || fps > 120) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("check APSinfo invalid fps  =");
            stringBuilder.append(fps);
            Slog.e(str, stringBuilder.toString());
            return false;
        } else if (brightnessPercent < 50 || brightnessPercent > 100) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("check APS info invalid brightnessPercent=");
            stringBuilder.append(brightnessPercent);
            Slog.e(str, stringBuilder.toString());
            return false;
        } else if (texturePercent >= 50 && texturePercent <= 100) {
            return true;
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("check APS info invalid texturePercent=");
            stringBuilder.append(texturePercent);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public static synchronized HwApsManager getDefault() {
        HwApsManager hwApsManager;
        synchronized (HwApsManager.class) {
            if (sInstance == null) {
                sInstance = new HwApsManager();
            }
            hwApsManager = sInstance;
        }
        return hwApsManager;
    }

    public int setLowResolutionMode(int lowResolutionMode) {
        if (!checkApsManagerService()) {
            return -2;
        }
        try {
            return this.mApsService.setLowResolutionMode(lowResolutionMode);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setLowResolutionMode,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public int setResolution(String pkgName, float ratio, boolean switchable) {
        if (!checkApsManagerService()) {
            return -2;
        }
        if (ratio < 0.25f || 1.0f < ratio) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setResolution, invalid param ratio = ");
            stringBuilder.append(ratio);
            Slog.e(str, stringBuilder.toString());
            return -1;
        }
        try {
            return this.mApsService.setResolution(pkgName, ratio, switchable);
        } catch (RemoteException ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setResolution,ex:");
            stringBuilder2.append(ex);
            Slog.w(str2, stringBuilder2.toString());
            return -1;
        }
    }

    public int setDescentGradeResolution(String pkgName, int reduceLevel, boolean switchable) {
        try {
            return this.mApsService.setDescentGradeResolution(pkgName, reduceLevel, switchable);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setDescentGradeResolution,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public int setFps(String pkgName, int fps) {
        if (!checkApsManagerService()) {
            return -2;
        }
        if (fps < 15 || 120 < fps) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setFps, invalid param fps = ");
            stringBuilder.append(fps);
            Slog.e(str, stringBuilder.toString());
            return -1;
        }
        try {
            return this.mApsService.setFps(pkgName, fps);
        } catch (RemoteException ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setFps,ex:");
            stringBuilder2.append(ex);
            Slog.w(str2, stringBuilder2.toString());
            return -1;
        }
    }

    public int setMaxFps(String pkgName, int fps) {
        if (!checkApsManagerService()) {
            return -2;
        }
        if (fps < 15 || 120 < fps) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setMaxFps, invalid param fps = ");
            stringBuilder.append(fps);
            Slog.e(str, stringBuilder.toString());
            return -1;
        }
        try {
            return this.mApsService.setMaxFps(pkgName, fps);
        } catch (RemoteException ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setMaxFps,ex:");
            stringBuilder2.append(ex);
            Slog.w(str2, stringBuilder2.toString());
            return -1;
        }
    }

    public int setBrightness(String pkgName, int ratioPercent) {
        if (!checkApsManagerService()) {
            return -2;
        }
        try {
            return this.mApsService.setBrightness(pkgName, ratioPercent);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setBrightness,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public int setTexture(String pkgName, int ratioPercent) {
        if (!checkApsManagerService()) {
            return -2;
        }
        try {
            return this.mApsService.setTexture(pkgName, ratioPercent);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setTexture,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public int setPackageApsInfo(String pkgName, ApsAppInfo info) {
        if (!checkApsManagerService()) {
            return -2;
        }
        if (!checkApsInfo(info)) {
            return -1;
        }
        try {
            return this.mApsService.setPackageApsInfo(pkgName, info);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setPackageApsInfo,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public ApsAppInfo getPackageApsInfo(String pkgName) {
        if (!checkApsManagerService()) {
            return null;
        }
        try {
            return this.mApsService.getPackageApsInfo(pkgName);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPackageApsInfo,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return null;
        }
    }

    public float getResolution(String pkgName) {
        if (!checkApsManagerService()) {
            return -1.0f;
        }
        try {
            return this.mApsService.getResolution(pkgName);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getResolution,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1.0f;
        }
    }

    public int getFps(String pkgName) {
        if (!checkApsManagerService()) {
            return -1;
        }
        try {
            return this.mApsService.getFps(pkgName);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getFps,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public int getMaxFps(String pkgName) {
        if (!checkApsManagerService()) {
            return -1;
        }
        try {
            return this.mApsService.getMaxFps(pkgName);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getMaxFps,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public int getBrightness(String pkgName) {
        if (!checkApsManagerService()) {
            return -1;
        }
        try {
            return this.mApsService.getBrightness(pkgName);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("brightness,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public int getTexture(String pkgName) {
        if (!checkApsManagerService()) {
            return -1;
        }
        try {
            return this.mApsService.getTexture(pkgName);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getTexture,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public boolean deletePackageApsInfo(String pkgName) {
        if (!checkApsManagerService()) {
            return false;
        }
        try {
            return this.mApsService.deletePackageApsInfo(pkgName);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deletePackageApsInfo,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return false;
        }
    }

    public int isFeaturesEnabled(int bitmask) {
        if (!checkApsManagerService()) {
            return -1;
        }
        try {
            return this.mApsService.isFeaturesEnabled(bitmask);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isFeaturesEnabled,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public boolean disableFeatures(int bitmask) {
        if (!checkApsManagerService()) {
            return false;
        }
        try {
            return this.mApsService.disableFeatures(bitmask);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("disableFeatures,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean enableFeatures(int bitmask) {
        if (!checkApsManagerService()) {
            return false;
        }
        try {
            return this.mApsService.enableFeatures(bitmask);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableFeatures,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return false;
        }
    }

    public List<ApsAppInfo> getAllPackagesApsInfo() {
        if (!checkApsManagerService()) {
            return null;
        }
        try {
            return this.mApsService.getAllPackagesApsInfo();
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAllPackagesApsInfo,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return null;
        }
    }

    public List<String> getAllApsPackages() {
        if (!checkApsManagerService()) {
            return null;
        }
        try {
            return this.mApsService.getAllApsPackages();
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAllApsPackages,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return null;
        }
    }

    public boolean updateApsInfo(List<ApsAppInfo> infos) {
        if (!checkApsManagerService()) {
            return false;
        }
        try {
            return this.mApsService.updateApsInfo(infos);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateApsInfo,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean registerCallback(String pkgName, IApsManagerServiceCallback callback) {
        if (!checkApsManagerService()) {
            return false;
        }
        try {
            Slog.w(TAG, "HwApsManagerService, registerCallback, start !");
            return this.mApsService.registerCallback(pkgName, callback);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("registerCallback,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return false;
        }
    }

    public float getSeviceVersion() {
        if (!checkApsManagerService()) {
            return -1.0f;
        }
        try {
            return this.mApsService.getSeviceVersion();
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getSeviceVersion,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return 0.0f;
        }
    }

    public boolean stopPackages(List<String> pkgs) {
        if (!checkApsManagerService()) {
            return false;
        }
        try {
            return this.mApsService.stopPackages(pkgs);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopPackages,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return false;
        }
    }

    public int setDynamicResolutionRatio(String pkgName, float ratio) {
        if (!checkApsManagerService()) {
            return -2;
        }
        if (ratio < 0.25f || 1.0f < ratio) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setDynamicResolutionRatio, invalid param ratio = ");
            stringBuilder.append(ratio);
            Slog.e(str, stringBuilder.toString());
            return -1;
        }
        try {
            return this.mApsService.setDynamicResolutionRatio(pkgName, ratio);
        } catch (RemoteException ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setDynamicResolutionRatio,ex:");
            stringBuilder2.append(ex);
            Slog.w(str2, stringBuilder2.toString());
            return -1;
        }
    }

    public float getDynamicResolutionRatio(String pkgName) {
        if (!checkApsManagerService()) {
            return -1.0f;
        }
        try {
            return this.mApsService.getDynamicResolutionRatio(pkgName);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getResolution,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1.0f;
        }
    }

    public int setDynamicFps(String pkgName, int fps) {
        if (!checkApsManagerService()) {
            return -2;
        }
        if (fps == -1 || (fps >= 15 && 120 >= fps)) {
            try {
                return this.mApsService.setDynamicFps(pkgName, fps);
            } catch (RemoteException ex) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setDynamicFps,ex:");
                stringBuilder.append(ex);
                Slog.w(str, stringBuilder.toString());
                return -1;
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setDynamicFps, invalid param fps = ");
        stringBuilder2.append(fps);
        Slog.e(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("APSLog, setDynamicFps: pkg:");
        stringBuilder2.append(pkgName);
        stringBuilder2.append(", fps:");
        stringBuilder2.append(fps);
        stringBuilder2.append(",retCode:-1(invalid param)");
        Slog.i(str2, stringBuilder2.toString());
        return -1;
    }

    public int getDynamicFps(String pkgName) {
        if (!checkApsManagerService()) {
            return -1;
        }
        try {
            return this.mApsService.getDynamicFps(pkgName);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getFps,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public int setFbSkip(String pkgName, boolean onoff) {
        if (!checkApsManagerService()) {
            return -2;
        }
        try {
            return this.mApsService.setFbSkip(pkgName, onoff);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setFbSkip,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public int setHighpToLowp(String pkgName, boolean onoff) {
        if (!checkApsManagerService()) {
            return -2;
        }
        try {
            return this.mApsService.setHighpToLowp(pkgName, onoff);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setHighpToLowp,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public int setShadowMap(String pkgName, int status) {
        if (!checkApsManagerService()) {
            return -2;
        }
        if (status < 0 || 3 < status) {
            return -1;
        }
        try {
            return this.mApsService.setShadowMap(pkgName, status);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setShadowMap,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public int setMipMap(String pkgName, int status) {
        if (!checkApsManagerService()) {
            return -2;
        }
        if (status < 0 || 3 < status) {
            return -1;
        }
        try {
            return this.mApsService.setMipMap(pkgName, status);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setMipMap,ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
    }

    public boolean getFbSkip(String pkgName) {
        if (checkApsManagerService()) {
            try {
                return this.mApsService.getFbSkip(pkgName);
            } catch (RemoteException ex) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getFbSkip,ex:");
                stringBuilder.append(ex);
                Slog.w(str, stringBuilder.toString());
                return false;
            }
        }
        Slog.w(TAG, "getFbSkip, ApsManagerService not ready, return false!");
        return false;
    }

    public boolean getHighpToLowp(String pkgName) {
        if (checkApsManagerService()) {
            try {
                return this.mApsService.getHighpToLowp(pkgName);
            } catch (RemoteException ex) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getHighpToLowp,ex:");
                stringBuilder.append(ex);
                Slog.w(str, stringBuilder.toString());
                return false;
            }
        }
        Slog.w(TAG, "getHighpToLowp, ApsManagerService not ready, return false!");
        return false;
    }

    public int getShadowMap(String pkgName) {
        if (checkApsManagerService()) {
            try {
                return this.mApsService.getShadowMap(pkgName);
            } catch (RemoteException ex) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getShadowMap,ex:");
                stringBuilder.append(ex);
                Slog.w(str, stringBuilder.toString());
                return 0;
            }
        }
        Slog.w(TAG, "getShadowMap, ApsManagerService not ready, return false!");
        return -2;
    }

    public int getMipMap(String pkgName) {
        if (checkApsManagerService()) {
            try {
                return this.mApsService.getMipMap(pkgName);
            } catch (RemoteException ex) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getMipMap,ex:");
                stringBuilder.append(ex);
                Slog.w(str, stringBuilder.toString());
                return 0;
            }
        }
        Slog.w(TAG, "getMipMap, ApsManagerService not ready, return false!");
        return -2;
    }

    public boolean isSupportApsColorPlus(String pkgName) {
        if (!checkApsManagerService()) {
            return false;
        }
        boolean isColorPlusSupport = false;
        try {
            isColorPlusSupport = this.mApsService.isSupportApsColorPlus(pkgName);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isSupportApsColorPlus, ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
        }
        return isColorPlusSupport;
    }

    public int setColorPlusPkgList(List<String> pkgList) {
        if (!checkApsManagerService()) {
            return -2;
        }
        int result = -1;
        try {
            result = this.mApsService.setColorPlusPkgList(pkgList);
        } catch (RemoteException ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setColorPlusPkgList, ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
        }
        return result;
    }
}
