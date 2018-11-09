package android.graphics;

import android.content.res.AssetManager;
import android.graphics.fonts.FontVariationAxis;
import android.util.Log;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class FontFamily {
    public static final int HW_FONT_OVERLARY = 2;
    public static final int HW_HOOK_OVERLARY = -1;
    private static String TAG = "FontFamily";
    private long mBuilderPtr;
    public long mNativePtr;

    private static native void nAbort(long j);

    private static native void nAddAxisValue(long j, int i, float f);

    private static native boolean nAddFont(long j, ByteBuffer byteBuffer, int i, int i2, int i3);

    private static native boolean nAddFontFromAssetManager(long j, AssetManager assetManager, String str, int i, boolean z, int i2, int i3, int i4);

    private static native boolean nAddFontWeightStyle(long j, ByteBuffer byteBuffer, int i, int i2, int i3);

    private static native void nAllowUnsupportedFont(long j);

    private static native long nCreateFamily(long j);

    private static native boolean nHwAddFont(long j, ByteBuffer byteBuffer, int i, int i2, int i3);

    private static native long nInitBuilder(String str, int i);

    private static native void nResetCoverage(long j);

    private static native void nResetFont(long j);

    private static native void nSetHwFontFamilyType(long j, int i);

    private static native void nUnrefFamily(long j);

    public FontFamily() {
        this.mBuilderPtr = nInitBuilder(null, 0);
    }

    public FontFamily(String lang, int variant) {
        this.mBuilderPtr = nInitBuilder(lang, variant);
    }

    public boolean freeze() {
        if (this.mBuilderPtr == 0) {
            throw new IllegalStateException("This FontFamily is already frozen");
        }
        this.mNativePtr = nCreateFamily(this.mBuilderPtr);
        this.mBuilderPtr = 0;
        return this.mNativePtr != 0;
    }

    public void abortCreation() {
        if (this.mBuilderPtr == 0) {
            throw new IllegalStateException("This FontFamily is already frozen or abandoned");
        }
        nAbort(this.mBuilderPtr);
        this.mBuilderPtr = 0;
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mNativePtr != 0) {
                nUnrefFamily(this.mNativePtr);
            }
            if (this.mBuilderPtr != 0) {
                nAbort(this.mBuilderPtr);
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
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
        Throwable th;
        Throwable th2;
        if (this.mBuilderPtr == 0) {
            throw new IllegalStateException("Unable to call addFont after freezing.");
        }
        Throwable th3 = null;
        FileInputStream fileInputStream = null;
        try {
            FileInputStream fileInputStream2 = new FileInputStream(path);
            try {
                FileChannel fileChannel = fileInputStream2.getChannel();
                ByteBuffer fontBuffer = fileChannel.map(MapMode.READ_ONLY, 0, fileChannel.size());
                if (axes != null) {
                    for (FontVariationAxis axis : axes) {
                        nAddAxisValue(this.mBuilderPtr, axis.getOpenTypeTagValue(), axis.getStyleValue());
                    }
                }
                boolean nAddFont = nAddFont(this.mBuilderPtr, fontBuffer, ttcIndex, weight, italic);
                if (fileInputStream2 != null) {
                    try {
                        fileInputStream2.close();
                    } catch (Throwable th4) {
                        th3 = th4;
                    }
                }
                if (th3 == null) {
                    return nAddFont;
                }
                try {
                    throw th3;
                } catch (IOException e) {
                    fileInputStream = fileInputStream2;
                }
            } catch (Throwable th5) {
                th = th5;
                fileInputStream = fileInputStream2;
                th2 = null;
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
                if (th2 == null) {
                    throw th;
                }
                try {
                    throw th2;
                } catch (IOException e2) {
                    Log.e(TAG, "Error mapping font file " + path);
                    return false;
                }
            }
        } catch (Throwable th7) {
            th = th7;
            th2 = null;
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (th2 == null) {
                throw th2;
            }
            throw th;
        }
    }

    public boolean hwAddFont(String path, int ttcIndex, int weight, int italic) {
        Throwable th;
        Throwable th2;
        Throwable th3 = null;
        FileInputStream fileInputStream = null;
        try {
            FileInputStream fileInputStream2 = new FileInputStream(path);
            try {
                FileChannel fileChannel = fileInputStream2.getChannel();
                boolean nHwAddFont = nHwAddFont(this.mNativePtr, fileChannel.map(MapMode.READ_ONLY, 0, fileChannel.size()), ttcIndex, weight, italic);
                if (fileInputStream2 != null) {
                    try {
                        fileInputStream2.close();
                    } catch (Throwable th4) {
                        th3 = th4;
                    }
                }
                if (th3 == null) {
                    return nHwAddFont;
                }
                try {
                    throw th3;
                } catch (IOException e) {
                    fileInputStream = fileInputStream2;
                }
            } catch (Throwable th5) {
                th = th5;
                fileInputStream = fileInputStream2;
                th2 = null;
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
                if (th2 == null) {
                    throw th;
                }
                try {
                    throw th2;
                } catch (IOException e2) {
                    Log.e(TAG, "Error mapping font file " + path);
                    return false;
                }
            }
        } catch (Throwable th7) {
            th = th7;
            th2 = null;
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (th2 == null) {
                throw th2;
            }
            throw th;
        }
    }

    public boolean addFontFromBuffer(ByteBuffer font, int ttcIndex, FontVariationAxis[] axes, int weight, int italic) {
        if (this.mBuilderPtr == 0) {
            throw new IllegalStateException("Unable to call addFontWeightStyle after freezing.");
        }
        if (axes != null) {
            for (FontVariationAxis axis : axes) {
                nAddAxisValue(this.mBuilderPtr, axis.getOpenTypeTagValue(), axis.getStyleValue());
            }
        }
        return nAddFontWeightStyle(this.mBuilderPtr, font, ttcIndex, weight, italic);
    }

    public boolean addFontFromAssetManager(AssetManager mgr, String path, int cookie, boolean isAsset, int ttcIndex, int weight, int isItalic, FontVariationAxis[] axes) {
        if (this.mBuilderPtr == 0) {
            throw new IllegalStateException("Unable to call addFontFromAsset after freezing.");
        }
        if (axes != null) {
            for (FontVariationAxis axis : axes) {
                nAddAxisValue(this.mBuilderPtr, axis.getOpenTypeTagValue(), axis.getStyleValue());
            }
        }
        return nAddFontFromAssetManager(this.mBuilderPtr, mgr, path, cookie, isAsset, ttcIndex, weight, isItalic);
    }

    public void allowUnsupportedFont() {
        if (this.mBuilderPtr == 0) {
            throw new IllegalStateException("Unable to allow unsupported font.");
        }
        nAllowUnsupportedFont(this.mBuilderPtr);
    }

    private static boolean nAddFont(long builderPtr, ByteBuffer font, int ttcIndex) {
        return nAddFont(builderPtr, font, ttcIndex, -1, -1);
    }
}
