package com.android.server.am;

import android.content.res.HwPCMultiWindowCompatibility;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;

public class HwPCMultiWindowSettingsDefault {
    private static final String DEFAULT_FILE_PATH = "/system/etc/multiwindow_compat_app.conf";
    public HashMap<String, Integer> mSettingsDefaults;

    public HwPCMultiWindowSettingsDefault(HwPCMultiWindowManager settings) {
        loadSettingsDefaults();
    }

    public int getAppDefaultMode(String pkgName, int screenOrientation) {
        Integer mode = (Integer) this.mSettingsDefaults.get(pkgName);
        if (mode != null) {
            return mode.intValue();
        }
        if (screenOrientation == 0) {
            return HwPCMultiWindowCompatibility.getLandscapeWithAllAction();
        }
        return HwPCMultiWindowCompatibility.getPortraitWithAllAction();
    }

    public int getAppDefaultMode(String pkgName) {
        Integer in = (Integer) this.mSettingsDefaults.get(pkgName);
        if (in != null) {
            return in.intValue();
        }
        return HwPCMultiWindowCompatibility.getPortraitWithAllAction();
    }

    public void setAppDefaultMode(String pkgName, int mode) {
        if (!TextUtils.isEmpty(pkgName)) {
            this.mSettingsDefaults.put(pkgName, Integer.valueOf(mode));
        }
    }

    private String getDefaultFilePath() {
        return DEFAULT_FILE_PATH;
    }

    private void loadSettingsDefaults() {
        Throwable th;
        this.mSettingsDefaults = new HashMap();
        File file = new File(getDefaultFilePath());
        if (file.exists()) {
            FileInputStream fileInputStream = null;
            BufferedReader bufferedReader = null;
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fis, Charset.defaultCharset()));
                    try {
                        String line = reader.readLine();
                        if (line != null) {
                            line = line.trim();
                        }
                        while (line != null) {
                            int index = line.indexOf(32);
                            if (index != -1) {
                                String packageName = line.substring(0, index);
                                try {
                                    this.mSettingsDefaults.put(packageName, Integer.valueOf(Integer.parseInt(line.substring(index + 1).trim())));
                                } catch (Exception e) {
                                    Log.e("MultiWindowManager", "WindowMode Parse Error: " + line);
                                }
                            }
                            line = reader.readLine();
                        }
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e2) {
                                Log.e("MultiWindowManager", "loadSettingsDefaults close IOException");
                                return;
                            }
                        }
                        if (fis != null) {
                            fis.close();
                            return;
                        }
                        return;
                    } catch (FileNotFoundException e3) {
                        bufferedReader = reader;
                        fileInputStream = fis;
                    } catch (IOException e4) {
                        bufferedReader = reader;
                        fileInputStream = fis;
                    } catch (Throwable th2) {
                        th = th2;
                        bufferedReader = reader;
                        fileInputStream = fis;
                    }
                } catch (FileNotFoundException e5) {
                    fileInputStream = fis;
                    try {
                        Log.e("MultiWindowManager", "loadSettingsDefaults FileNotFoundException");
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e6) {
                                Log.e("MultiWindowManager", "loadSettingsDefaults close IOException");
                                return;
                            }
                        }
                        if (fileInputStream != null) {
                            fileInputStream.close();
                            return;
                        }
                        return;
                    } catch (Throwable th3) {
                        th = th3;
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e7) {
                                Log.e("MultiWindowManager", "loadSettingsDefaults close IOException");
                                throw th;
                            }
                        }
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        throw th;
                    }
                } catch (IOException e8) {
                    fileInputStream = fis;
                    Log.e("MultiWindowManager", "loadSettingsDefaults IOException");
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e9) {
                            Log.e("MultiWindowManager", "loadSettingsDefaults close IOException");
                            return;
                        }
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close();
                        return;
                    }
                    return;
                } catch (Throwable th4) {
                    th = th4;
                    fileInputStream = fis;
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    throw th;
                }
            } catch (FileNotFoundException e10) {
                Log.e("MultiWindowManager", "loadSettingsDefaults FileNotFoundException");
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                    return;
                }
                return;
            } catch (IOException e11) {
                Log.e("MultiWindowManager", "loadSettingsDefaults IOException");
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                    return;
                }
                return;
            }
        }
        this.mSettingsDefaults.put("com.huawei.desktop.explorer", Integer.valueOf(HwPCMultiWindowCompatibility.getLandscapeWithAllAction()));
        this.mSettingsDefaults.put("com.chaozhuo.browser", Integer.valueOf(HwPCMultiWindowCompatibility.getLandscapeWithAllAction()));
        this.mSettingsDefaults.put("com.android.calendar", Integer.valueOf(HwPCMultiWindowCompatibility.getLandscapeWithAllAction()));
        this.mSettingsDefaults.put("com.android.email", Integer.valueOf(HwPCMultiWindowCompatibility.getLandscapeWithAllAction()));
        this.mSettingsDefaults.put("com.example.android.notepad", Integer.valueOf(HwPCMultiWindowCompatibility.getLandscapeWithAllAction()));
        this.mSettingsDefaults.put("com.huawei.android.internal.app", Integer.valueOf(4));
    }
}
