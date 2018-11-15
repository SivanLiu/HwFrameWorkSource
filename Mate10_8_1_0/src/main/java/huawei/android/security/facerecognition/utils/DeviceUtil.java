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
        LogUtil.d(TAG, "disable temperature threshold is : " + DISABLE_TEMP_THRESHOLD + ", cap is : " + DISABLE_CAP_THRESHOLD);
        if (temperature > DISABLE_TEMP_THRESHOLD || capacity > DISABLE_CAP_THRESHOLD) {
            return false;
        }
        return true;
    }

    public static boolean isLowTemperature(double temperature) {
        LogUtil.d(TAG, "temperature threshold is : " + TEMP_THRESHOLD);
        return temperature <= TEMP_THRESHOLD;
    }

    public static double getBatteryCapacity() {
        return readDoubleFromFile(sCapacityFilePath);
    }

    public static double getBatteryTemperature() {
        return readDoubleFromFile(sTempFilePath) / 10.0d;
    }

    private static double readDoubleFromFile(String filePath) {
        Throwable th;
        double ret = 1000.0d;
        String str = null;
        Throwable th2 = null;
        FileInputStream fileInputStream = null;
        try {
            IOException e;
            FileInputStream in = new FileInputStream(filePath);
            try {
                byte[] max = new byte[10];
                int bytesRead = in.read(max, 0, 10);
                if (bytesRead <= 0) {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 == null) {
                        return 1000.0d;
                    }
                    try {
                        throw th2;
                    } catch (IOException e2) {
                        e = e2;
                        fileInputStream = in;
                    } catch (NumberFormatException e3) {
                        LogUtil.w(TAG, "wrong fromat : " + str);
                        return ret;
                    }
                }
                byte[] toReturn = new byte[bytesRead];
                System.arraycopy(max, 0, toReturn, 0, bytesRead);
                String retStr = new String(toReturn, CharacterSets.DEFAULT_CHARSET_NAME);
                try {
                    ret = Double.valueOf(retStr).doubleValue();
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable th4) {
                            th2 = th4;
                        }
                    }
                    if (th2 != null) {
                        try {
                            throw th2;
                        } catch (IOException e4) {
                            e = e4;
                        } catch (NumberFormatException e5) {
                            str = retStr;
                            LogUtil.w(TAG, "wrong fromat : " + str);
                            return ret;
                        }
                    }
                    return ret;
                } catch (Throwable th5) {
                    th = th5;
                    fileInputStream = in;
                    str = retStr;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Throwable th6) {
                            if (th2 == null) {
                                th2 = th6;
                            } else if (th2 != th6) {
                                th2.addSuppressed(th6);
                            }
                        }
                    }
                    if (th2 != null) {
                        try {
                            throw th2;
                        } catch (IOException e6) {
                            e = e6;
                        } catch (NumberFormatException e7) {
                            LogUtil.w(TAG, "wrong fromat : " + str);
                            return ret;
                        }
                    }
                    throw th;
                }
            } catch (Throwable th7) {
                th = th7;
                fileInputStream = in;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (th2 != null) {
                    throw th;
                } else {
                    throw th2;
                }
            }
            LogUtil.w(TAG, "read status occurs IOException" + e.getMessage());
            return ret;
        } catch (Throwable th8) {
            th = th8;
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (th2 != null) {
                throw th2;
            }
            throw th;
        }
    }
}
