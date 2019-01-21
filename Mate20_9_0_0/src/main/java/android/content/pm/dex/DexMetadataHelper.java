package android.content.pm.dex;

import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.PackageLite;
import android.content.pm.PackageParser.PackageParserException;
import android.util.ArrayMap;
import android.util.jar.StrictJarFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DexMetadataHelper {
    private static final String DEX_METADATA_FILE_EXTENSION = ".dm";

    private DexMetadataHelper() {
    }

    public static boolean isDexMetadataFile(File file) {
        return isDexMetadataPath(file.getName());
    }

    private static boolean isDexMetadataPath(String path) {
        return path.endsWith(DEX_METADATA_FILE_EXTENSION);
    }

    public static long getPackageDexMetadataSize(PackageLite pkg) {
        long sizeBytes = 0;
        for (String dexMetadata : getPackageDexMetadata(pkg).values()) {
            sizeBytes += new File(dexMetadata).length();
        }
        return sizeBytes;
    }

    public static File findDexMetadataForFile(File targetFile) {
        File dexMetadataFile = new File(buildDexMetadataPathForFile(targetFile));
        return dexMetadataFile.exists() ? dexMetadataFile : null;
    }

    public static Map<String, String> getPackageDexMetadata(Package pkg) {
        return buildPackageApkToDexMetadataMap(pkg.getAllCodePaths());
    }

    private static Map<String, String> getPackageDexMetadata(PackageLite pkg) {
        return buildPackageApkToDexMetadataMap(pkg.getAllCodePaths());
    }

    private static Map<String, String> buildPackageApkToDexMetadataMap(List<String> codePaths) {
        ArrayMap<String, String> result = new ArrayMap();
        for (int i = codePaths.size() - 1; i >= 0; i--) {
            String codePath = (String) codePaths.get(i);
            String dexMetadataPath = buildDexMetadataPathForFile(new File(codePath));
            if (Files.exists(Paths.get(dexMetadataPath, new String[0]), new LinkOption[0])) {
                result.put(codePath, dexMetadataPath);
            }
        }
        return result;
    }

    public static String buildDexMetadataPathForApk(String codePath) {
        if (PackageParser.isApkPath(codePath)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(codePath.substring(0, codePath.length() - PackageParser.APK_FILE_EXTENSION.length()));
            stringBuilder.append(DEX_METADATA_FILE_EXTENSION);
            return stringBuilder.toString();
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Corrupted package. Code path is not an apk ");
        stringBuilder2.append(codePath);
        throw new IllegalStateException(stringBuilder2.toString());
    }

    private static String buildDexMetadataPathForFile(File targetFile) {
        if (PackageParser.isApkFile(targetFile)) {
            return buildDexMetadataPathForApk(targetFile.getPath());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(targetFile.getPath());
        stringBuilder.append(DEX_METADATA_FILE_EXTENSION);
        return stringBuilder.toString();
    }

    public static void validatePackageDexMetadata(Package pkg) throws PackageParserException {
        for (String dexMetadata : getPackageDexMetadata(pkg).values()) {
            validateDexMetadataFile(dexMetadata);
        }
    }

    private static void validateDexMetadataFile(String dmaPath) throws PackageParserException {
        StrictJarFile jarFile = null;
        try {
            try {
                new StrictJarFile(dmaPath, false, false).close();
            } catch (IOException e) {
            }
        } catch (IOException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error opening ");
            stringBuilder.append(dmaPath);
            throw new PackageParserException(PackageManager.INSTALL_FAILED_BAD_DEX_METADATA, stringBuilder.toString(), e2);
        } catch (Throwable th) {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e3) {
                }
            }
        }
    }

    public static void validateDexPaths(String[] paths) {
        ArrayList<String> apks = new ArrayList();
        int i = 0;
        for (int i2 = 0; i2 < paths.length; i2++) {
            if (PackageParser.isApkPath(paths[i2])) {
                apks.add(paths[i2]);
            }
        }
        ArrayList<String> unmatchedDmFiles = new ArrayList();
        while (i < paths.length) {
            String dmPath = paths[i];
            if (isDexMetadataPath(dmPath)) {
                boolean valid = false;
                for (int j = apks.size() - 1; j >= 0; j--) {
                    if (dmPath.equals(buildDexMetadataPathForFile(new File((String) apks.get(j))))) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    unmatchedDmFiles.add(dmPath);
                }
            }
            i++;
        }
        if (!unmatchedDmFiles.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unmatched .dm files: ");
            stringBuilder.append(unmatchedDmFiles);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }
}
