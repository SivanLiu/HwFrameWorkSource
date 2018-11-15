package android.app;

import android.os.FileUtils;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.BaseDexClassLoader.Reporter;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import libcore.io.Libcore;

class DexLoadReporter implements Reporter {
    private static final boolean DEBUG = false;
    private static final DexLoadReporter INSTANCE = new DexLoadReporter();
    private static final String TAG = "DexLoadReporter";
    @GuardedBy("mDataDirs")
    private final Set<String> mDataDirs = new HashSet();

    private DexLoadReporter() {
    }

    static DexLoadReporter getInstance() {
        return INSTANCE;
    }

    void registerAppDataDir(String packageName, String dataDir) {
        if (dataDir != null) {
            synchronized (this.mDataDirs) {
                this.mDataDirs.add(dataDir);
            }
        }
    }

    public void report(List<BaseDexClassLoader> classLoadersChain, List<String> classPaths) {
        if (classLoadersChain.size() != classPaths.size()) {
            Slog.wtf(TAG, "Bad call to DexLoadReporter: argument size mismatch");
        } else if (classPaths.isEmpty()) {
            Slog.wtf(TAG, "Bad call to DexLoadReporter: empty dex paths");
        } else {
            String[] dexPathsForRegistration = ((String) classPaths.get(0)).split(File.pathSeparator);
            if (dexPathsForRegistration.length != 0) {
                notifyPackageManager(classLoadersChain, classPaths);
                registerSecondaryDexForProfiling(dexPathsForRegistration);
            }
        }
    }

    private void notifyPackageManager(List<BaseDexClassLoader> classLoadersChain, List<String> classPaths) {
        List<String> classLoadersNames = new ArrayList(classPaths.size());
        for (BaseDexClassLoader bdc : classLoadersChain) {
            classLoadersNames.add(bdc.getClass().getName());
        }
        String packageName = ActivityThread.currentPackageName();
        try {
            ActivityThread.getPackageManager().notifyDexLoad(packageName, classLoadersNames, classPaths, VMRuntime.getRuntime().vmInstructionSet());
        } catch (RemoteException re) {
            Slog.e(TAG, "Failed to notify PM about dex load for package " + packageName, re);
        }
    }

    private void registerSecondaryDexForProfiling(String[] dexPaths) {
        int i = 0;
        if (SystemProperties.getBoolean("dalvik.vm.dexopt.secondary", false)) {
            String[] dataDirs;
            synchronized (this.mDataDirs) {
                dataDirs = (String[]) this.mDataDirs.toArray(new String[0]);
            }
            int length = dexPaths.length;
            while (i < length) {
                registerSecondaryDexForProfiling(dexPaths[i], dataDirs);
                i++;
            }
        }
    }

    private void registerSecondaryDexForProfiling(String dexPath, String[] dataDirs) {
        if (isSecondaryDexFile(dexPath, dataDirs)) {
            try {
                File realDexPath = new File(Libcore.os.realpath(dexPath));
                File secondaryProfileDir = new File(realDexPath.getParent(), "oat");
                File secondaryProfile = new File(secondaryProfileDir, realDexPath.getName() + ".cur.prof");
                if (secondaryProfileDir.exists() || secondaryProfileDir.mkdir()) {
                    try {
                        boolean created = secondaryProfile.createNewFile();
                        VMRuntime.registerAppInfo(secondaryProfile.getPath(), new String[]{dexPath});
                        return;
                    } catch (IOException ex) {
                        Slog.e(TAG, "Failed to create profile for secondary dex " + dexPath + ":" + ex.getMessage());
                        return;
                    }
                }
                Slog.e(TAG, "Could not create the profile directory: " + secondaryProfile);
            } catch (ErrnoException ex2) {
                Slog.e(TAG, "Failed to get the real path of secondary dex " + dexPath + ":" + ex2.getMessage());
            }
        }
    }

    private boolean isSecondaryDexFile(String dexPath, String[] dataDirs) {
        for (String dataDir : dataDirs) {
            if (FileUtils.contains(dataDir, dexPath)) {
                return true;
            }
        }
        return false;
    }
}
