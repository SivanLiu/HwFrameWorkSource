package huawei.com.android.internal.os;

import android.util.Log;
import dalvik.system.PathClassLoader;
import huawei.cust.HwCfgFilePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HwFLClassLoader extends PathClassLoader {
    public static final String TAG = "HwFLClassLoader";
    private static final String USE_FEATURE_LIST = "/feature/used-list";
    private static boolean mInitUsedList = false;
    private static List<String> mUsedFeatureList = new ArrayList();

    static {
        initUsedList();
    }

    public HwFLClassLoader(String dexPath, ClassLoader parent) {
        super(dexPath, parent);
    }

    public HwFLClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) {
        super(dexPath, librarySearchPath, parent);
    }

    private static void initUsedList() {
        File pathFile = HwCfgFilePolicy.getCfgFile(USE_FEATURE_LIST, 0);
        if (pathFile == null) {
            Log.d(TAG, "get used feature list :/feature/used-list failed!");
            return;
        }
        BufferedReader br = null;
        try {
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(pathFile), "UTF-8"), 256);
                String line = "";
                while (true) {
                    String readLine = br.readLine();
                    line = readLine;
                    if (readLine != null) {
                        line = line.trim();
                        if (!line.startsWith("#")) {
                            if (!line.equals("")) {
                                readLine = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("add package: ");
                                stringBuilder.append(line);
                                stringBuilder.append(" in FEATURE_USED_LIST");
                                Log.v(readLine, stringBuilder.toString());
                                mUsedFeatureList.add(line);
                            }
                        }
                    } else {
                        try {
                            break;
                        } catch (IOException e) {
                            Log.e(TAG, "Error in close BufferedReader /feature/used-list.", e);
                        }
                    }
                }
                br.close();
            } catch (IOException e2) {
                Log.e(TAG, "Error reading /feature/used-list.", e2);
                if (br != null) {
                    br.close();
                }
            } catch (Throwable th) {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "Error in close BufferedReader /feature/used-list.", e3);
                    }
                }
            }
            mInitUsedList = true;
        } catch (FileNotFoundException e4) {
            Log.e(TAG, "Couldn't find /feature/used-list.");
        }
    }

    private static boolean isUseFeature(String dexPath) {
        if (!mInitUsedList) {
            Log.d(TAG, "USE_FEATURE_LIST had not init! ");
            return false;
        } else if (dexPath == null || dexPath.isEmpty()) {
            return false;
        } else {
            for (String feature : mUsedFeatureList) {
                if (dexPath.contains(feature)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static ClassLoader getHwFLClassLoaderParent(String dexPath) {
        if (isUseFeature(dexPath)) {
            return ClassLoader.getSystemClassLoader();
        }
        return null;
    }
}
