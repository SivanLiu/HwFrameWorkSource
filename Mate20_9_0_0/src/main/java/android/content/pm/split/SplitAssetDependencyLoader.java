package android.content.pm.split;

import android.content.pm.PackageParser;
import android.content.pm.PackageParser.PackageLite;
import android.content.pm.PackageParser.PackageParserException;
import android.content.res.ApkAssets;
import android.content.res.AssetManager;
import android.os.Build.VERSION;
import android.util.SparseArray;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import libcore.io.IoUtils;

public class SplitAssetDependencyLoader extends SplitDependencyLoader<PackageParserException> implements SplitAssetLoader {
    private final AssetManager[] mCachedAssetManagers = new AssetManager[this.mSplitPaths.length];
    private final ApkAssets[][] mCachedSplitApks = new ApkAssets[this.mSplitPaths.length][];
    private final int mFlags;
    private final String[] mSplitPaths;

    public SplitAssetDependencyLoader(PackageLite pkg, SparseArray<int[]> dependencies, int flags) {
        super(dependencies);
        this.mSplitPaths = new String[(pkg.splitCodePaths.length + 1)];
        this.mSplitPaths[0] = pkg.baseCodePath;
        System.arraycopy(pkg.splitCodePaths, 0, this.mSplitPaths, 1, pkg.splitCodePaths.length);
        this.mFlags = flags;
    }

    protected boolean isSplitCached(int splitIdx) {
        return this.mCachedAssetManagers[splitIdx] != null;
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

    private static AssetManager createAssetManagerWithAssets(ApkAssets[] apkAssets) {
        AssetManager assets = new AssetManager();
        assets.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, VERSION.RESOURCES_SDK_INT);
        assets.setApkAssets(apkAssets, false);
        return assets;
    }

    protected void constructSplit(int splitIdx, int[] configSplitIndices, int parentSplitIdx) throws PackageParserException {
        ArrayList<ApkAssets> assets = new ArrayList();
        if (parentSplitIdx >= 0) {
            Collections.addAll(assets, this.mCachedSplitApks[parentSplitIdx]);
        }
        assets.add(loadApkAssets(this.mSplitPaths[splitIdx], this.mFlags));
        for (int configSplitIdx : configSplitIndices) {
            assets.add(loadApkAssets(this.mSplitPaths[configSplitIdx], this.mFlags));
        }
        this.mCachedSplitApks[splitIdx] = (ApkAssets[]) assets.toArray(new ApkAssets[assets.size()]);
        this.mCachedAssetManagers[splitIdx] = createAssetManagerWithAssets(this.mCachedSplitApks[splitIdx]);
    }

    public AssetManager getBaseAssetManager() throws PackageParserException {
        loadDependenciesForSplit(0);
        return this.mCachedAssetManagers[0];
    }

    public AssetManager getSplitAssetManager(int idx) throws PackageParserException {
        loadDependenciesForSplit(idx + 1);
        return this.mCachedAssetManagers[idx + 1];
    }

    public void close() throws Exception {
        for (AssetManager assets : this.mCachedAssetManagers) {
            IoUtils.closeQuietly(assets);
        }
    }
}
