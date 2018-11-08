package com.android.server.pc;

import android.util.HwPCUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class DisplayDriverCommunicator {
    private static final String FILE_PATH = "/sys/class/dp/source/source_mode";
    private static String KEYBOARD_STATE_INFO = "/sys/devices/platform/hwsw_kb/stateinfo";
    private static final int KEYBOARD_STATE_INFO_MAX_LENGTH = 100;
    public static final String START_DESKTOP_MODE_VALUE = "0";
    public static final String START_PHONE_MODE_VALUE = "1";
    private static final String TAG = "DisplayDriverCommunicator";
    private static volatile DisplayDriverCommunicator mInstance = null;
    private boolean isFeatureSupport;

    private DisplayDriverCommunicator() {
        this.isFeatureSupport = false;
        this.isFeatureSupport = isFeatureSupport();
    }

    private boolean isFeatureSupport() {
        if (new File(FILE_PATH).exists()) {
            return true;
        }
        return false;
    }

    public synchronized void resetProjectionMode() {
        Log.v(TAG, "resetProjectionMode isFeatureSupport = " + this.isFeatureSupport);
        if (this.isFeatureSupport) {
            setProjectionModeValue("1");
        }
    }

    public synchronized void enableProjectionMode() {
        Log.v(TAG, "enableProjectionMode isFeatureSupport = " + this.isFeatureSupport);
        if (this.isFeatureSupport) {
            setProjectionModeValue("0");
        }
    }

    private void setProjectionModeValue(String val) {
        Throwable th;
        Log.v(TAG, "setProjectionModeValue " + val);
        FileOutputStream fileOutputStream = null;
        try {
            FileOutputStream fileOutWriteMode = new FileOutputStream(new File(FILE_PATH));
            try {
                fileOutWriteMode.write(val.getBytes("utf-8"));
                if (fileOutWriteMode != null) {
                    try {
                        fileOutWriteMode.close();
                    } catch (IOException e) {
                        Log.e(TAG, "fail to setProjectionModeValue");
                    }
                }
                fileOutputStream = fileOutWriteMode;
            } catch (FileNotFoundException e2) {
                fileOutputStream = fileOutWriteMode;
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "fail to setProjectionModeValue");
                    }
                }
                Log.v(TAG, "setProjectionModeValue end");
            } catch (IOException e4) {
                fileOutputStream = fileOutWriteMode;
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e5) {
                        Log.e(TAG, "fail to setProjectionModeValue");
                    }
                }
                Log.v(TAG, "setProjectionModeValue end");
            } catch (Throwable th2) {
                th = th2;
                fileOutputStream = fileOutWriteMode;
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e6) {
                        Log.e(TAG, "fail to setProjectionModeValue");
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e7) {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            Log.v(TAG, "setProjectionModeValue end");
        } catch (IOException e8) {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            Log.v(TAG, "setProjectionModeValue end");
        } catch (Throwable th3) {
            th = th3;
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            throw th;
        }
        Log.v(TAG, "setProjectionModeValue end");
    }

    public static synchronized DisplayDriverCommunicator getInstance() {
        DisplayDriverCommunicator displayDriverCommunicator;
        synchronized (DisplayDriverCommunicator.class) {
            if (mInstance == null) {
                mInstance = new DisplayDriverCommunicator();
            }
            displayDriverCommunicator = mInstance;
        }
        return displayDriverCommunicator;
    }

    public static boolean isExclusiveKeyboardOnline() {
        Throwable th;
        BufferedReader bufferedReader = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(KEYBOARD_STATE_INFO), StandardCharsets.UTF_8));
            try {
                StringBuilder sb = new StringBuilder();
                while (true) {
                    int intC = reader.read();
                    if (intC == -1) {
                        break;
                    }
                    char c = (char) intC;
                    if (c != '\n') {
                        if (sb.length() >= 100) {
                            break;
                        }
                        sb.append(c);
                    } else {
                        break;
                    }
                }
                String keyboardStatus = sb.toString();
                HwPCUtils.log(TAG, "Exclisive status:" + keyboardStatus);
                if (keyboardStatus == null || !keyboardStatus.trim().equals("keyboard is online".trim())) {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            HwPCUtils.log(TAG, "isExclisiveKeyboardOnline() close():IOException");
                        }
                    }
                    return false;
                }
                HwPCUtils.log(TAG, "isExclisiveKeyboardOnline():true");
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e2) {
                        HwPCUtils.log(TAG, "isExclisiveKeyboardOnline() close():IOException");
                    }
                }
                return true;
            } catch (IOException e3) {
                bufferedReader = reader;
            } catch (Throwable th2) {
                th = th2;
                bufferedReader = reader;
            }
        } catch (IOException e4) {
            try {
                HwPCUtils.log(TAG, "isExclisiveKeyboardOnline():IOException");
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e5) {
                        HwPCUtils.log(TAG, "isExclisiveKeyboardOnline() close():IOException");
                    }
                }
                return false;
            } catch (Throwable th3) {
                th = th3;
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e6) {
                        HwPCUtils.log(TAG, "isExclisiveKeyboardOnline() close():IOException");
                    }
                }
                throw th;
            }
        }
    }
}
