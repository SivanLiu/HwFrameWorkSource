package android.graphics;

import android.content.res.AssetManager;
import android.graphics.fonts.FontVariationAxis;
import android.text.TextUtils;
import android.util.Log;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import libcore.util.NativeAllocationRegistry;

public class FontFamily {
    public static final int HW_FONT_OVERLARY = 2;
    public static final int HW_HOOK_OVERLARY = -1;
    private static String TAG = "FontFamily";
    private static final NativeAllocationRegistry sBuilderRegistry = new NativeAllocationRegistry(FontFamily.class.getClassLoader(), nGetBuilderReleaseFunc(), 64);
    private static final NativeAllocationRegistry sFamilyRegistry = new NativeAllocationRegistry(FontFamily.class.getClassLoader(), nGetFamilyReleaseFunc(), 64);
    private long mBuilderPtr;
    public boolean mIsHook;
    private Runnable mNativeBuilderCleaner;
    public long mNativePtr;

    private static native void nAddAxisValue(long j, int i, float f);

    private static native boolean nAddFont(long j, ByteBuffer byteBuffer, int i, int i2, int i3);

    private static native boolean nAddFontFromAssetManager(long j, AssetManager assetManager, String str, int i, boolean z, int i2, int i3, int i4);

    private static native boolean nAddFontWeightStyle(long j, ByteBuffer byteBuffer, int i, int i2, int i3);

    private static native long nCreateFamily(long j);

    private static native long nGetBuilderReleaseFunc();

    private static native long nGetFamilyReleaseFunc();

    private static native boolean nHwAddFont(long j, ByteBuffer byteBuffer, int i, int i2, int i3);

    private static native long nInitBuilder(String str, int i);

    private static native void nResetCoverage(long j);

    private static native void nResetFont(long j);

    private static native void nSetHwFontFamilyType(long j, int i);

    public FontFamily() {
        this.mIsHook = false;
        this.mBuilderPtr = nInitBuilder(null, 0);
        this.mNativeBuilderCleaner = sBuilderRegistry.registerNativeAllocation(this, this.mBuilderPtr);
    }

    public FontFamily(String[] langs, int variant) {
        String langsString;
        this.mIsHook = false;
        if (langs == null || langs.length == 0) {
            langsString = null;
        } else if (langs.length == 1) {
            langsString = langs[0];
        } else {
            langsString = TextUtils.join(",", langs);
        }
        this.mBuilderPtr = nInitBuilder(langsString, variant);
        this.mNativeBuilderCleaner = sBuilderRegistry.registerNativeAllocation(this, this.mBuilderPtr);
    }

    public boolean freeze() {
        if (this.mBuilderPtr != 0) {
            this.mNativePtr = nCreateFamily(this.mBuilderPtr);
            this.mNativeBuilderCleaner.run();
            this.mBuilderPtr = 0;
            if (this.mNativePtr != 0) {
                sFamilyRegistry.registerNativeAllocation(this, this.mNativePtr);
            }
            return this.mNativePtr != 0;
        } else {
            throw new IllegalStateException("This FontFamily is already frozen");
        }
    }

    public void abortCreation() {
        if (this.mBuilderPtr != 0) {
            this.mNativeBuilderCleaner.run();
            this.mBuilderPtr = 0;
            return;
        }
        throw new IllegalStateException("This FontFamily is already frozen or abandoned");
    }

    public void setHookFlag(boolean ishook) {
        this.mIsHook = ishook;
    }

    public void resetCoverage() {
        nResetCoverage(this.mNativePtr);
    }

    public void resetFont() {
        nResetFont(this.mNativePtr);
    }

    public void setHwFontFamilyType(int type) {
        nSetHwFontFamilyType(this.mNativePtr, type);
    }

    public boolean addFont(String path, int ttcIndex, FontVariationAxis[] axes, int weight, int italic) {
        String str = path;
        FontVariationAxis[] fontVariationAxisArr = axes;
        if (this.mBuilderPtr != 0) {
            FileInputStream file;
            try {
                file = new FileInputStream(str);
                FileChannel fileChannel = file.getChannel();
                ByteBuffer fontBuffer = fileChannel.map(MapMode.READ_ONLY, 0, fileChannel.size());
                if (fontVariationAxisArr != null) {
                    for (FontVariationAxis axis : fontVariationAxisArr) {
                        nAddAxisValue(this.mBuilderPtr, axis.getOpenTypeTagValue(), axis.getStyleValue());
                    }
                }
                boolean nAddFont = nAddFont(this.mBuilderPtr, fontBuffer, ttcIndex, weight, italic);
                $closeResource(null, file);
                return nAddFont;
            } catch (IOException e) {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error mapping font file ");
                stringBuilder.append(str);
                Log.e(str2, stringBuilder.toString());
                return false;
            } catch (Throwable th) {
                $closeResource(th, file);
            }
        }
        throw new IllegalStateException("Unable to call addFont after freezing.");
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    public boolean hwAddFont(String path, int ttcIndex, int weight, int italic) {
        Throwable th;
        String str;
        StringBuilder stringBuilder;
        String str2 = path;
        try {
            boolean nHwAddFont;
            FileInputStream file = new FileInputStream(str2);
            Throwable th2 = null;
            try {
                FileChannel fileChannel = file.getChannel();
                FileChannel fileChannel2 = fileChannel;
                try {
                    nHwAddFont = nHwAddFont(this.mNativePtr, fileChannel2.map(MapMode.READ_ONLY, 0, fileChannel.size()), ttcIndex, weight, italic);
                } catch (Throwable th3) {
                    th = th3;
                    th2 = th;
                    try {
                        throw th2;
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
            } catch (Throwable th5) {
                th = th5;
                $closeResource(th2, file);
                throw th;
            }
            try {
                $closeResource(null, file);
                return nHwAddFont;
            } catch (IOException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error mapping font file ");
                stringBuilder.append(str2);
                Log.e(str, stringBuilder.toString());
                return false;
            }
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error mapping font file ");
            stringBuilder.append(str2);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean addFontFromBuffer(ByteBuffer font, int ttcIndex, FontVariationAxis[] axes, int weight, int italic) {
        if (this.mBuilderPtr != 0) {
            if (axes != null) {
                for (FontVariationAxis axis : axes) {
                    nAddAxisValue(this.mBuilderPtr, axis.getOpenTypeTagValue(), axis.getStyleValue());
                }
            }
            return nAddFontWeightStyle(this.mBuilderPtr, font, ttcIndex, weight, italic);
        }
        throw new IllegalStateException("Unable to call addFontWeightStyle after freezing.");
    }

    public boolean addFontFromAssetManager(AssetManager mgr, String path, int cookie, boolean isAsset, int ttcIndex, int weight, int isItalic, FontVariationAxis[] axes) {
        FontVariationAxis[] fontVariationAxisArr = axes;
        if (this.mBuilderPtr != 0) {
            if (fontVariationAxisArr != null) {
                for (FontVariationAxis axis : fontVariationAxisArr) {
                    nAddAxisValue(this.mBuilderPtr, axis.getOpenTypeTagValue(), axis.getStyleValue());
                }
            }
            return nAddFontFromAssetManager(this.mBuilderPtr, mgr, path, cookie, isAsset, ttcIndex, weight, isItalic);
        }
        throw new IllegalStateException("Unable to call addFontFromAsset after freezing.");
    }

    private static boolean nAddFont(long builderPtr, ByteBuffer font, int ttcIndex) {
        return nAddFont(builderPtr, font, ttcIndex, -1, -1);
    }
}
