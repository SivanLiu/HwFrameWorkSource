package android.content.pm.split;

import android.content.pm.PackageParser;
import android.content.pm.PackageParser.PackageLite;
import android.content.pm.PackageParser.PackageParserException;
import android.content.res.ApkAssets;
import android.content.res.AssetManager;
import android.os.Build.VERSION;
import com.android.internal.util.ArrayUtils;
import java.io.IOException;
import libcore.io.IoUtils;

public class DefaultSplitAssetLoader implements SplitAssetLoader {
    private final String mBaseCodePath;
    private AssetManager mCachedAssetManager;
    private final int mFlags;
    private final String[] mSplitCodePaths;

    public DefaultSplitAssetLoader(PackageLite pkg, int flags) {
        this.mBaseCodePath = pkg.baseCodePath;
        this.mSplitCodePaths = pkg.splitCodePaths;
        this.mFlags = flags;
    }

    private static ApkAssets loadApkAssets(String path, int flags) throws PackageParserException {
        if ((flags & 1) == 0 || PackageParser.isApkPath(path)) {
            try {
                return ApkAssets.loadFromPath(path);
            } catch (IOException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to load APK at path ");
                stringBuilder.append(path);
                throw new PackageParserException(-2, stringBuilder.toString(), e);
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Invalid package file: ");
        stringBuilder2.append(path);
        throw new PackageParserException(-100, stringBuilder2.toString());
    }

    public AssetManager getBaseAssetManager() throws PackageParserException {
        if (this.mCachedAssetManager != null) {
            return this.mCachedAssetManager;
        }
        ApkAssets[] apkAssets = new ApkAssets[((this.mSplitCodePaths != null ? this.mSplitCodePaths.length : 0) + 1)];
        int splitIdx = 0 + 1;
        apkAssets[0] = loadApkAssets(this.mBaseCodePath, this.mFlags);
        if (!ArrayUtils.isEmpty(this.mSplitCodePaths)) {
            String[] strArr = this.mSplitCodePaths;
            int length = strArr.length;
            int splitIdx2 = splitIdx;
            splitIdx = 0;
            while (splitIdx < length) {
                int splitIdx3 = splitIdx2 + 1;
                apkAssets[splitIdx2] = loadApkAssets(strArr[splitIdx], this.mFlags);
                splitIdx++;
                splitIdx2 = splitIdx3;
            }
            splitIdx = splitIdx2;
        }
        AssetManager assets = new AssetManager();
        assets.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, VERSION.RESOURCES_SDK_INT);
        assets.setApkAssets(apkAssets, false);
        this.mCachedAssetManager = assets;
        return this.mCachedAssetManager;
    }

    public AssetManager getSplitAssetManager(int splitIdx) throws PackageParserException {
        return getBaseAssetManager();
    }

    public void close() throws Exception {
        IoUtils.closeQuietly(this.mCachedAssetManager);
    }
}
