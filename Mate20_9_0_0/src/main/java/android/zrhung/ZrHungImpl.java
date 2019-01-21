package android.zrhung;

import android.util.Log;
import android.util.ZRHung;
import android.util.ZRHung.HungConfig;
import android.zrhung.appeye.AppBootFail;
import android.zrhung.appeye.AppEyeANR;
import android.zrhung.appeye.AppEyeBK;
import android.zrhung.appeye.AppEyeBinderBlock;
import android.zrhung.appeye.AppEyeCL;
import android.zrhung.appeye.AppEyeCLA;
import android.zrhung.appeye.AppEyeFocusWindow;
import android.zrhung.appeye.AppEyeFwkBlock;
import android.zrhung.appeye.AppEyeHK;
import android.zrhung.appeye.AppEyeObs;
import android.zrhung.appeye.AppEyeRcv;
import android.zrhung.appeye.AppEyeTransparentWindow;
import android.zrhung.appeye.AppEyeUiProbe;
import android.zrhung.appeye.AppEyeXcollie;
import android.zrhung.watchpoint.SysHungScreenOn;

public class ZrHungImpl implements IZrHung {
    private static final String TAG = "ZrHungImpl";
    protected static final String ZRHUNG_PID_PARAM = "pid";
    protected static final String ZRHUNG_PKGNAME_PARAM = "packageName";
    protected static final String ZRHUNG_PROCNAME_PARAM = "processName";
    protected static final String ZRHUNG_RECOVERRESULT_PARAM = "recoverresult";
    protected static final String ZRHUNG_UID_PARAM = "uid";
    protected short mWpId;

    protected ZrHungImpl(String wpName) {
        this.mWpId = getWatchponitId(wpName);
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static IZrHung getZrHung(String wpName) {
        Object obj;
        switch (wpName.hashCode()) {
            case -1823227498:
                if (wpName.equals("appeye_anr")) {
                    obj = 7;
                    break;
                }
            case -843673519:
                if (wpName.equals("appeye_homekey")) {
                    obj = 6;
                    break;
                }
            case -707278626:
                if (wpName.equals("appeye_receiver")) {
                    obj = null;
                    break;
                }
            case -358036264:
                if (wpName.equals("appeye_nofocuswindow")) {
                    obj = 12;
                    break;
                }
            case -112346872:
                if (wpName.equals("appeye_ssbinderfull")) {
                    obj = 14;
                    break;
                }
            case 129990321:
                if (wpName.equals("appeye_xcollie")) {
                    obj = 15;
                    break;
                }
            case 226809278:
                if (wpName.equals("appeye_clear")) {
                    obj = 3;
                    break;
                }
            case 292286899:
                if (wpName.equals("appeye_transparentwindow")) {
                    obj = 11;
                    break;
                }
            case 450048453:
                if (wpName.equals("appeye_observer")) {
                    obj = 1;
                    break;
                }
            case 488462959:
                if (wpName.equals("zrhung_wp_screenon_framework")) {
                    obj = 10;
                    break;
                }
            case 891740963:
                if (wpName.equals("appeye_clearall")) {
                    obj = 4;
                    break;
                }
            case 1118179582:
                if (wpName.equals("appeye_frameworkblock")) {
                    obj = 9;
                    break;
                }
            case 1256170960:
                if (wpName.equals("zrhung_wp_vm_watchdog")) {
                    obj = 8;
                    break;
                }
            case 1935326413:
                if (wpName.equals("appeye_uiprobe")) {
                    obj = 2;
                    break;
                }
            case 2011374409:
                if (wpName.equals("appeye_backkey")) {
                    obj = 5;
                    break;
                }
            case 2114922495:
                if (wpName.equals("appeye_bootfail")) {
                    obj = 13;
                    break;
                }
            default:
                obj = -1;
                break;
        }
        switch (obj) {
            case null:
                return AppEyeRcv.getInstance(wpName);
            case 1:
                return AppEyeObs.getInstance(wpName);
            case 2:
                return AppEyeUiProbe.get();
            case 3:
                return AppEyeCL.getInstance(wpName);
            case 4:
                return AppEyeCLA.getInstance(wpName);
            case 5:
                return new AppEyeBK(wpName);
            case 6:
                return new AppEyeHK(wpName);
            case 7:
                return new AppEyeANR(wpName);
            case 8:
                return new SysHungVmWTG(wpName);
            case 9:
                return AppEyeFwkBlock.getInstance();
            case 10:
                return SysHungScreenOn.getInstance(wpName);
            case 11:
                return AppEyeTransparentWindow.getInstance(wpName);
            case 12:
                return AppEyeFocusWindow.getInstance(wpName);
            case 13:
                return AppBootFail.getInstance(wpName);
            case 14:
                return AppEyeBinderBlock.getInstance(wpName);
            case 15:
                return AppEyeXcollie.getInstance(wpName);
            default:
                return null;
        }
    }

    public int init(ZrHungData args) {
        return 0;
    }

    public boolean start(ZrHungData args) {
        return false;
    }

    public boolean check(ZrHungData args) {
        return false;
    }

    public boolean cancelCheck(ZrHungData args) {
        return false;
    }

    public boolean stop(ZrHungData args) {
        return false;
    }

    public boolean sendEvent(ZrHungData args) {
        return false;
    }

    public ZrHungData query() {
        return null;
    }

    public boolean addInfo(ZrHungData args) {
        return false;
    }

    protected HungConfig getConfig() {
        HungConfig cfg = ZRHung.getHungConfig(this.mWpId);
        if (cfg == null || cfg.status != 0) {
            Log.e(TAG, "ZRHung.getConfig failed!");
            return null;
        }
        Log.d(TAG, "ZRHung.getConfig success!");
        return cfg;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private short getWatchponitId(String name) {
        short s;
        switch (name.hashCode()) {
            case -1823227498:
                if (name.equals("appeye_anr")) {
                    s = (short) 7;
                    break;
                }
            case -843673519:
                if (name.equals("appeye_homekey")) {
                    s = (short) 6;
                    break;
                }
            case -707278626:
                if (name.equals("appeye_receiver")) {
                    s = (short) 0;
                    break;
                }
            case -358036264:
                if (name.equals("appeye_nofocuswindow")) {
                    s = (short) 12;
                    break;
                }
            case -112346872:
                if (name.equals("appeye_ssbinderfull")) {
                    s = (short) 14;
                    break;
                }
            case 129990321:
                if (name.equals("appeye_xcollie")) {
                    s = (short) 15;
                    break;
                }
            case 226809278:
                if (name.equals("appeye_clear")) {
                    s = (short) 3;
                    break;
                }
            case 292286899:
                if (name.equals("appeye_transparentwindow")) {
                    s = (short) 11;
                    break;
                }
            case 450048453:
                if (name.equals("appeye_observer")) {
                    s = (short) 1;
                    break;
                }
            case 488462959:
                if (name.equals("zrhung_wp_screenon_framework")) {
                    s = (short) 10;
                    break;
                }
            case 891740963:
                if (name.equals("appeye_clearall")) {
                    s = (short) 4;
                    break;
                }
            case 1118179582:
                if (name.equals("appeye_frameworkblock")) {
                    s = (short) 9;
                    break;
                }
            case 1256170960:
                if (name.equals("zrhung_wp_vm_watchdog")) {
                    s = (short) 8;
                    break;
                }
            case 1935326413:
                if (name.equals("appeye_uiprobe")) {
                    s = (short) 2;
                    break;
                }
            case 2011374409:
                if (name.equals("appeye_backkey")) {
                    s = (short) 5;
                    break;
                }
            case 2114922495:
                if (name.equals("appeye_bootfail")) {
                    s = (short) 13;
                    break;
                }
            default:
                s = (short) -1;
                break;
        }
        switch (s) {
            case (short) 0:
                return (short) 276;
            case (short) 1:
                return (short) 277;
            case (short) 2:
                return (short) 258;
            case (short) 3:
                return (short) 265;
            case (short) 4:
                return (short) 266;
            case (short) 5:
                return (short) 516;
            case (short) 6:
                return (short) 515;
            case (short) 7:
                return (short) 269;
            case (short) 8:
                return (short) 22;
            case (short) 9:
                return (short) 271;
            case (short) 10:
                return (short) 11;
            case (short) 11:
                return (short) 273;
            case (short) 12:
                return (short) 272;
            case (short) 13:
                return (short) 264;
            case (short) 14:
                return (short) 288;
            case (short) 15:
                return ZRHung.XCOLLIE_FWK_SERVICE;
            default:
                return (short) 0;
        }
    }

    protected boolean sendAppEyeEvent(short wpId, ZrHungData args, String cmdBuf, String buffer) {
        StringBuilder sb = new StringBuilder();
        if (args != null) {
            try {
                int uid = args.getInt("uid");
                if (uid > 0) {
                    sb.append("uid = ");
                    sb.append(Integer.toString(uid));
                    sb.append(10);
                }
                int pid = args.getInt("pid");
                if (pid > 0) {
                    sb.append("pid = ");
                    sb.append(Integer.toString(pid));
                    sb.append(10);
                }
                String pkgName = args.getString("packageName");
                if (pkgName != null) {
                    sb.append("packageName = ");
                    sb.append(pkgName);
                    sb.append(10);
                }
                String procName = args.getString("processName");
                if (procName != null) {
                    sb.append("processName = ");
                    sb.append(procName);
                    sb.append(10);
                }
                String recoverresult = args.getString(ZRHUNG_RECOVERRESULT_PARAM);
                if (recoverresult != null) {
                    sb.append("result = ");
                    sb.append(recoverresult);
                    sb.append(10);
                }
            } catch (Exception ex) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception info ex:");
                stringBuilder.append(ex);
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
        if (buffer != null) {
            sb.append(buffer);
        }
        if (ZRHung.sendHungEvent(wpId, cmdBuf, sb.toString()) == null) {
            Log.e(TAG, " sendAppFreezeEvent failed!");
        }
        return true;
    }
}
