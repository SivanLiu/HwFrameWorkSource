package android.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Rect;
import android.hwtheme.HwThemeManager;
import android.os.FreezeScreenScene;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import huawei.android.hwpicaveragenoises.HwPicAverageNoises;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class HwWallpaperManager extends WallpaperManager {
    private static boolean DEBUG = false;
    private static String TAG = "HwWallpaperManager";
    private static final Object sSync = new Object[0];
    public static final int x_land = 2;
    public static final int x_port = 0;
    public static final int y_land = 3;
    public static final int y_port = 1;
    private HwCustHwWallpaperManager mCustHwWallpaperManager = ((HwCustHwWallpaperManager) HwCustUtils.createObj(HwCustHwWallpaperManager.class, new Object[0]));

    public HwWallpaperManager(IWallpaperManager service, Context context, Handler handler) {
        super(service, context, handler);
    }

    public Bitmap getBlurBitmap(Rect rect) {
        return getBlurBitmap(rect, false);
    }

    public Bitmap getBitmap() {
        return super.getBitmap();
    }

    private Bitmap getBlurBitmap(Rect rect, boolean exclusiveIntersect) {
        int i;
        Rect rect2 = rect;
        Rect thumbnailRect = new Rect(rect2.left / 4, rect2.top / 4, rect2.right / 4, rect2.bottom / 4);
        if (rect.width() > 0 && rect.width() < 4) {
            thumbnailRect.right = thumbnailRect.left + 1;
        }
        if (rect.height() > 0 && rect.height() < 4) {
            thumbnailRect.bottom = thumbnailRect.top + 1;
        }
        Rect thumbnailRect2 = cutForLiveWallpaper(thumbnailRect);
        Bitmap bp = peekBlurWallpaperBitmap(thumbnailRect2);
        if (bp == null) {
            Log.w(TAG, "getBlurBitmap return default cause peek null blur wallpaper.");
            return null;
        }
        Rect wall = new Rect(0, 0, bp.getWidth(), bp.getHeight());
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getBlurBitmap wall=");
            stringBuilder.append(wall);
            stringBuilder.append(",rect=");
            stringBuilder.append(rect2);
            stringBuilder.append(",thumbnailRect=");
            stringBuilder.append(thumbnailRect2);
            Log.d(str, stringBuilder.toString());
        }
        if (wall.intersect(thumbnailRect2)) {
            int height = wall.height();
            int width = wall.width();
            int[] inPixels = new int[(width * height)];
            int i2;
            try {
                Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
                if (bitmap == null) {
                    try {
                        Log.e(TAG, "getBlurBitmap() bitmap = null");
                        return null;
                    } catch (OutOfMemoryError e) {
                        i2 = width;
                        i = height;
                        Log.e(TAG, "HwWallpaperManager can't create blur wallpaper for out of memory.");
                        return null;
                    }
                }
                i2 = width;
                try {
                    bp.getPixels(inPixels, 0, width, wall.left, wall.top, width, height);
                    try {
                        bitmap.setPixels(inPixels, 0, i2, 0, 0, i2, height);
                        if (HwPicAverageNoises.isAverageNoiseSupported()) {
                            return new HwPicAverageNoises().addNoiseWithBlackBoard(bitmap, 1275068416);
                        }
                        return bitmap;
                    } catch (OutOfMemoryError e2) {
                        Log.e(TAG, "HwWallpaperManager can't create blur wallpaper for out of memory.");
                        return null;
                    }
                } catch (OutOfMemoryError e3) {
                    i = height;
                    Log.e(TAG, "HwWallpaperManager can't create blur wallpaper for out of memory.");
                    return null;
                }
            } catch (OutOfMemoryError e4) {
                i2 = width;
                i = height;
                Log.e(TAG, "HwWallpaperManager can't create blur wallpaper for out of memory.");
                return null;
            }
        }
        Log.w(TAG, "getBlurBitmap return default cause rect intersect fail.");
        return null;
    }

    public void setCallback(Object callback) {
        int count = mCallbacks.size();
        int i = 0;
        while (i < count) {
            if (((WeakReference) mCallbacks.get(i)).get() != callback) {
                i++;
            } else {
                return;
            }
        }
        mCallbacks.add(new WeakReference(callback));
    }

    public void setStream(InputStream data) throws IOException {
        setStream(data, 3);
    }

    public void setStream(InputStream data, int flag) throws IOException {
        setWallpaperStartingPoints(new int[]{-1, -1, -1, -1});
        super.setStream(data, null, true, flag);
    }

    protected Bitmap createDefaultWallpaperBitmap(Bitmap bm) {
        if (bm == null) {
            return null;
        }
        String str;
        StringBuilder stringBuilder;
        Display display = ((WindowManager) getContext().getSystemService(FreezeScreenScene.WINDOW_PARAM)).getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        int max = size.x > size.y ? size.x : size.y;
        int min = size.x < size.y ? size.x : size.y;
        int width = bm.getWidth();
        int height = bm.getHeight();
        if (this.mCustHwWallpaperManager != null) {
            if (this.mCustHwWallpaperManager.isScrollWallpaper(max, min, width, height, getContext())) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Default wallpaper is scrollable: width=");
                stringBuilder.append(width);
                stringBuilder.append(", height=");
                stringBuilder.append(height);
                Log.w(str, stringBuilder.toString());
                return bm;
            }
        }
        if ((height == max || width == max) && (width == 2 * min || width == max)) {
            int[] offsets = new int[]{-1, -1, -1, -1};
            if (TextUtils.isEmpty(SystemProperties.get("ro.config.wallpaper")) && !new File("/data/cust/wallpaper/", "wallpaper1.jpg").exists()) {
                String filePath = "/data/skin/description.xml";
                offsets = parseWallpaperOffsets("/data/skin/description.xml");
            }
            if (offsets[0] == -1) {
                offsets[0] = (width - min) / 2;
            } else if (offsets[0] < -1) {
                offsets[0] = 0;
            } else if (offsets[0] > width - min) {
                offsets[0] = width - min;
            }
            if (offsets[2] == -1) {
                offsets[2] = (width - max) / 2;
            } else if (offsets[2] < -1) {
                offsets[2] = 0;
            } else if (offsets[2] > width - max) {
                offsets[2] = width - max;
            }
            if (offsets[2] > offsets[0]) {
                offsets[2] = offsets[0];
            } else if (offsets[2] < offsets[0] - (max - min)) {
                offsets[2] = offsets[0] - (max - min);
            }
            try {
                Bitmap newbm = Bitmap.createBitmap(bm, offsets[2], 0, max, max);
                offsets[0] = offsets[0] - offsets[2];
                offsets[1] = 0;
                offsets[2] = 0;
                if (offsets[3] == -1) {
                    offsets[3] = (max - min) / 2;
                } else if (offsets[3] < -1) {
                    offsets[3] = 0;
                } else if (offsets[3] > max - min) {
                    offsets[3] = max - min;
                }
                IWallpaperManager mService = getIWallpaperManager();
                if (mService != null) {
                    try {
                        mService.setCurrOffsets(offsets);
                    } catch (RemoteException e) {
                    } catch (SecurityException e2) {
                        Log.e(TAG, "Caller has no permission to set current wallpaper offsets!");
                    }
                }
                if (!(bm == null || bm == newbm || bm.isRecycled())) {
                    bm.recycle();
                }
                return newbm;
            } catch (IllegalArgumentException e3) {
                Log.w(TAG, "Create default square bitmap error, run generateBitmap! ");
                return HwThemeManager.generateBitmap(getContext(), bm, min, max);
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Irregular default bitmap: width=");
        stringBuilder.append(width);
        stringBuilder.append(", height=");
        stringBuilder.append(height);
        Log.w(str, stringBuilder.toString());
        return HwThemeManager.generateBitmap(getContext(), bm, min, max);
    }

    private void setWallpaperStartingPoints(int[] offsets) {
        IWallpaperManager mService = getIWallpaperManager();
        if (mService != null) {
            try {
                mService.setNextOffsets(offsets);
            } catch (RemoteException e) {
            }
        }
    }

    public int[] getWallpaperStartingPoints() {
        int[] defaultOffsets = new int[]{-1, -1, -1, -1};
        int[] offsets = null;
        IWallpaperManager mService = getIWallpaperManager();
        if (mService != null) {
            try {
                offsets = mService.getCurrOffsets();
            } catch (RemoteException e) {
            }
        }
        if (offsets == null) {
            return defaultOffsets;
        }
        return offsets;
    }

    public void setBitmap(Bitmap bitmap) throws IOException {
        setWallpaperStartingPoints(new int[]{-1, -1, -1, -1});
        super.setBitmap(bitmap);
    }

    public void setBitmapWithOffsets(Bitmap bitmap, int[] offsets) throws IOException {
        setWallpaperStartingPoints(offsets);
        super.setBitmap(bitmap);
    }

    public void setStreamWithOffsets(InputStream data, int[] offsets) throws IOException {
        setStreamWithOffsets(data, offsets, 3);
    }

    public void setStreamWithOffsets(InputStream data, int[] offsets, int flag) throws IOException {
        setWallpaperStartingPoints(offsets);
        super.setStream(data, null, true, flag);
    }

    public int[] parseWallpaperOffsets(String srcFile) {
        int[] offsets = new int[]{-1, -1, -1, -1};
        if (srcFile == null) {
            return offsets;
        }
        File file = new File(srcFile);
        if (!file.exists()) {
            return offsets;
        }
        String tagName;
        String is = null;
        try {
            FileInputStream is2 = new FileInputStream(file);
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(is2, CharacterSets.DEFAULT_CHARSET_NAME);
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                if (eventType == 2) {
                    tagName = parser.getName();
                    if (tagName.equals("WP_Xoffset_port")) {
                        offsets[0] = Integer.parseInt(parser.nextText());
                    } else if (tagName.equals("WP_Yoffset_port")) {
                        offsets[1] = Integer.parseInt(parser.nextText());
                    } else if (tagName.equals("WP_Xoffset_land")) {
                        offsets[2] = Integer.parseInt(parser.nextText());
                    } else if (tagName.equals("WP_Yoffset_land")) {
                        offsets[3] = Integer.parseInt(parser.nextText());
                    }
                } else if (eventType == 3) {
                }
            }
            try {
                is2.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            if (is != null) {
                is.close();
            }
        } catch (XmlPullParserException e2) {
            e2.printStackTrace();
            if (is != null) {
                is.close();
            }
        } catch (IOException ioe2) {
            ioe2.printStackTrace();
            if (is != null) {
                is.close();
            }
        } catch (Exception e3) {
            e3.printStackTrace();
            if (is != null) {
                is.close();
            }
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe3) {
                    ioe3.printStackTrace();
                }
            }
        }
        tagName = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Parse offsets:");
        stringBuilder.append(offsets[0]);
        stringBuilder.append(",");
        stringBuilder.append(offsets[1]);
        stringBuilder.append(",");
        stringBuilder.append(offsets[2]);
        stringBuilder.append(",");
        stringBuilder.append(offsets[3]);
        Log.d(tagName, stringBuilder.toString());
        return offsets;
    }

    private Rect cutForLiveWallpaper(Rect rect) {
        WallpaperInfo wallpaperInfo = getWallpaperInfo();
        int mCurrentUserId = 0;
        ApplicationInfo ai = null;
        if (wallpaperInfo != null) {
            String packageName = wallpaperInfo.getPackageName();
            IWallpaperManager mService = getIWallpaperManager();
            int flags = 0;
            if (mService != null) {
                try {
                    mCurrentUserId = mService.getWallpaperUserId();
                } catch (RemoteException e) {
                    Log.e(TAG, "IWallpaperManager or IPackageManager RemoteException error");
                }
            }
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0, mCurrentUserId);
            if (ai != null) {
                flags = ai.flags;
            }
            if ((flags & 1) == 0 && rect.left == 0) {
                DisplayMetrics dm = new DisplayMetrics();
                ((WindowManager) getContext().getSystemService(FreezeScreenScene.WINDOW_PARAM)).getDefaultDisplay().getMetrics(dm);
                int disWidth = (dm.widthPixels > dm.heightPixels ? dm.heightPixels : dm.widthPixels) / 4;
                rect.right = rect.right > disWidth ? disWidth : rect.right;
                rect.right = rect.left + 2 > rect.right ? rect.left + 2 : rect.right;
                if (rect.width() > 2 * 30) {
                    rect.left += 30;
                    rect.right -= 30;
                } else {
                    int dw = disWidth / 2;
                    rect.left = dw - 2;
                    rect.right = dw + 2;
                }
            }
        }
        return rect;
    }
}
