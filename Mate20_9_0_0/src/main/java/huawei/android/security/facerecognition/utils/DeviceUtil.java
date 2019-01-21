package huawei.android.security.facerecognition.utils;

import android.os.SystemProperties;
import java.io.FileInputStream;
import java.io.IOException;

public class DeviceUtil {
    private static final int CAP_MIN = -100;
    private static final double DISABLE_CAP_THRESHOLD = (((double) SystemProperties.getInt("ro.config.face_disable_cap", CAP_MIN)) * 1.0d);
    private static final double DISABLE_TEMP_THRESHOLD = ((((double) SystemProperties.getInt("ro.config.face_disable_temp", TEMP_MIN)) * 1.0d) / 10.0d);
    private static final int MAX_BYTES = 10;
    private static final String TAG = "DeviceUtil";
    private static final int TEMP_MAX = 1000;
    private static final int TEMP_MIN = -1000;
    private static final double TEMP_THRESHOLD = ((((double) SystemProperties.getInt("ro.config.low_temp_tsh", TEMP_MIN)) * 1.0d) / 10.0d);
    private static final String sCapacityFilePath = "/sys/class/power_supply/Battery/capacity";
    private static final String sTempFilePath = "/sys/class/power_supply/Battery/temp";

    public static boolean reachDisabledTempCap(double temperature, double capacity) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disable temperature threshold is : ");
        stringBuilder.append(DISABLE_TEMP_THRESHOLD);
        stringBuilder.append(", cap is : ");
        stringBuilder.append(DISABLE_CAP_THRESHOLD);
        LogUtil.d(str, stringBuilder.toString());
        return temperature <= DISABLE_TEMP_THRESHOLD && capacity <= DISABLE_CAP_THRESHOLD;
    }

    public static boolean isLowTemperature(double temperature) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("temperature threshold is : ");
        stringBuilder.append(TEMP_THRESHOLD);
        LogUtil.d(str, stringBuilder.toString());
        return temperature <= TEMP_THRESHOLD;
    }

    public static double getBatteryCapacity() {
        return readDoubleFromFile(sCapacityFilePath);
    }

    public static double getBatteryTemperature() {
        return readDoubleFromFile(sTempFilePath) / 10.0d;
    }

    private static double readDoubleFromFile(String filePath) {
        String str;
        StringBuilder stringBuilder;
        double ret = 1000.0d;
        String retStr = null;
        FileInputStream in;
        try {
            in = new FileInputStream(filePath);
            byte[] max = new byte[10];
            int bytesRead = in.read(max, 0, 10);
            if (bytesRead <= 0) {
                in.close();
                return 1000.0d;
            }
            byte[] toReturn = new byte[bytesRead];
            System.arraycopy(max, 0, toReturn, 0, bytesRead);
            retStr = new String(toReturn, CharacterSets.DEFAULT_CHARSET_NAME);
            ret = Double.valueOf(retStr).doubleValue();
            in.close();
            return ret;
        } catch (IOException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("read status occurs IOException");
            stringBuilder.append(e.getMessage());
            LogUtil.w(str, stringBuilder.toString());
        } catch (NumberFormatException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("wrong fromat : ");
            stringBuilder.append(retStr);
            LogUtil.w(str, stringBuilder.toString());
        } catch (Throwable th) {
            r2.addSuppressed(th);
        }
    }
}
