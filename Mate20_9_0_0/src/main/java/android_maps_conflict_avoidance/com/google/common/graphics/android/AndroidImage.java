package android_maps_conflict_avoidance.com.google.common.graphics.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android_maps_conflict_avoidance.com.google.common.graphics.GoogleGraphics;
import android_maps_conflict_avoidance.com.google.common.graphics.GoogleImage;
import java.util.Map;

public class AndroidImage implements GoogleImage {
    private static volatile int bitmapCount = 0;
    private volatile Bitmap bitmap;
    private final boolean isOriginal;
    private boolean pinned;

    public enum AutoScale {
        AUTO_SCALE_ENABLED,
        AUTO_SCALE_DISABLED
    }

    public AndroidImage(byte[] imageData, int imageOffset, int imageLength) {
        this.pinned = false;
        this.bitmap = BitmapFactory.decodeByteArray(imageData, imageOffset, imageLength);
        if (this.bitmap != null) {
            synchronized (AndroidImage.class) {
                bitmapCount++;
                this.isOriginal = true;
            }
            return;
        }
        throw new IllegalStateException("Null Bitmap!");
    }

    public AndroidImage(int width, int height) {
        this(width, height, true);
    }

    public AndroidImage(int width, int height, boolean processAlpha) {
        this.pinned = false;
        this.bitmap = Bitmap.createBitmap(width, height, processAlpha ? Config.ARGB_8888 : Config.RGB_565);
        if (this.bitmap != null) {
            synchronized (AndroidImage.class) {
                bitmapCount++;
                this.isOriginal = true;
            }
            return;
        }
        throw new IllegalStateException("Null Bitmap!");
    }

    public AndroidImage(Bitmap bitmap) {
        this.pinned = false;
        this.bitmap = bitmap;
        this.isOriginal = false;
    }

    public AndroidImage(Context context, Map<String, Integer> stringIdMap, String name, AutoScale autoScale) {
        this.pinned = false;
        String cleanName = cleanName(name);
        if (stringIdMap != null) {
            Integer resourceId = (Integer) stringIdMap.get(cleanName);
            if (resourceId != null) {
                Options options = null;
                if (autoScale == AutoScale.AUTO_SCALE_DISABLED) {
                    options = new Options();
                    options.inScaled = false;
                }
                this.bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId.intValue(), options);
                if (autoScale == AutoScale.AUTO_SCALE_DISABLED && this.bitmap != null) {
                    this.bitmap.setDensity(android_maps_conflict_avoidance.com.google.common.Config.getInstance().getPixelsPerInch());
                }
            }
        }
        if (this.bitmap == null) {
            this.bitmap = BitmapFactory.decodeFile(name);
        }
        if (this.bitmap != null) {
            synchronized (AndroidImage.class) {
                bitmapCount++;
                this.isOriginal = true;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Null Bitmap! \"");
        stringBuilder.append(name);
        stringBuilder.append("\"; if seen during a test, ");
        stringBuilder.append("this usually means that the image file needs to be added to the test.config file");
        throw new IllegalStateException(stringBuilder.toString());
    }

    private static String cleanName(String name) {
        if (name.indexOf("/") == 0) {
            name = name.substring(1);
        }
        int dotIndex = name.indexOf(".");
        if (dotIndex > 0) {
            return name.substring(0, dotIndex);
        }
        return name;
    }

    public void pin() {
        this.pinned = true;
    }

    public void recycle() {
        if (!this.pinned && this.bitmap != null) {
            this.bitmap.recycle();
            this.bitmap = null;
        }
    }

    public Bitmap getBitmap() {
        return this.bitmap;
    }

    public int getWidth() {
        return this.bitmap.getWidth();
    }

    public int getHeight() {
        return this.bitmap.getHeight();
    }

    public GoogleGraphics getGraphics() {
        return new AndroidGraphics(new Canvas(this.bitmap));
    }

    public GoogleImage createScaledImage(int srcX, int srcY, int srcWidth, int srcHeight, int newWidth, int newHeight) {
        ScaledAndroidImage image = new ScaledAndroidImage(this, newWidth, newHeight, srcX, srcY, srcWidth, srcHeight);
        if (newWidth * newHeight < 4096) {
            image.getGraphics();
        }
        return image;
    }

    public void drawImage(GoogleGraphics g, int x, int y) {
        ((AndroidGraphics) g).getCanvas().drawBitmap(this.bitmap, (float) x, (float) y, null);
    }

    protected void finalize() throws Throwable {
        compact();
        super.finalize();
    }

    /* JADX WARNING: Missing block: B:10:0x000e, code:
            if (r0 == null) goto L_0x0028;
     */
    /* JADX WARNING: Missing block: B:11:0x0010, code:
            r1 = android_maps_conflict_avoidance.com.google.common.graphics.android.AndroidImage.class;
     */
    /* JADX WARNING: Missing block: B:12:0x0012, code:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:14:?, code:
            bitmapCount--;
     */
    /* JADX WARNING: Missing block: B:15:0x001b, code:
            if (bitmapCount < 0) goto L_0x001f;
     */
    /* JADX WARNING: Missing block: B:16:0x001d, code:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:18:0x0024, code:
            throw new java.lang.IllegalStateException();
     */
    /* JADX WARNING: Missing block: B:22:0x0028, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void compact() {
        Throwable th;
        if (this.isOriginal) {
            synchronized (this) {
                Bitmap b;
                try {
                    b = this.bitmap;
                    try {
                        this.bitmap = null;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    b = null;
                    throw th;
                }
            }
        }
        this.bitmap = null;
    }
}
