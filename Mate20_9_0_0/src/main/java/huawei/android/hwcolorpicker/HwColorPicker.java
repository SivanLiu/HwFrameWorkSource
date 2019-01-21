package huawei.android.hwcolorpicker;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.util.Log;
import java.lang.ref.WeakReference;

public class HwColorPicker {
    private static final float CURVE_LINEAR_COEF = 5644.5f;
    private static final float CURVE_POWER_COEF = -0.54f;
    private static final PickedColor DEFAULT_COLOR = new PickedColor();
    private static final int DEFAULT_H = 25;
    private static final int DEFAULT_W = 25;
    private static final boolean IS_EMUI_LITE = SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", false);
    public static final int MASK_CLIENT_TYPE = -16777216;
    public static final int MASK_RESULT_INDEX = 65535;
    public static final int MASK_RESULT_STATE = 16711680;
    private static final String TAG = "HwColorPicker";
    private static boolean sIsLibColorPickerExist;

    public interface AsyncProcessCallBack {
        void onColorPicked(PickedColor pickedColor);
    }

    public enum ClientType {
        Default(0),
        Extend(1),
        Debug(1),
        Music(2);
        
        private int index;

        private ClientType(int index) {
            this.index = index;
        }
    }

    private static class ColorPickingTask extends AsyncTask<WeakReference<TaskParams>, Integer, PickedColor> {
        AsyncProcessCallBack mCallBack;

        private ColorPickingTask(AsyncProcessCallBack cb) {
            this.mCallBack = cb;
        }

        @SafeVarargs
        protected final PickedColor doInBackground(WeakReference<TaskParams>... weakReferences) {
            TaskParams taskParams = (TaskParams) weakReferences[null].get();
            if (taskParams == null) {
                return HwColorPicker.DEFAULT_COLOR;
            }
            if (taskParams.rect == null) {
                return HwColorPicker.processBitmap(taskParams.bitmap, taskParams.clientType);
            }
            return HwColorPicker.processBitmap(taskParams.bitmap, taskParams.rect, taskParams.clientType);
        }

        protected void onPostExecute(PickedColor pickedColor) {
            if (this.mCallBack != null) {
                this.mCallBack.onColorPicked(pickedColor);
            }
        }
    }

    public static class PickedColor {
        private static final int WIDGET_DEFAULT_COLOR = -855638017;
        private int mClientType;
        private int[] mColorPicked;
        private int mResultState;

        private PickedColor() {
            this.mClientType = -1;
            this.mResultState = -1;
        }

        private PickedColor(int[] colorResult) {
            this.mClientType = -1;
            this.mResultState = -1;
            if (colorResult == null || colorResult.length <= 0) {
                throw new RuntimeException("Illegal result, colorResult is null or Empty!");
            }
            int flag = colorResult[0];
            this.mClientType = (-16777216 & flag) >> 24;
            this.mResultState = (HwColorPicker.MASK_RESULT_STATE & flag) >> 16;
            int requestedNum = ResultType.getRequestedColorNum(this.mClientType);
            if (colorResult.length == requestedNum + 1) {
                this.mColorPicked = new int[requestedNum];
                System.arraycopy(colorResult, 1, this.mColorPicked, 0, requestedNum);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("colorResult's length : ");
            stringBuilder.append(colorResult.length);
            stringBuilder.append(", requestedNum : ");
            stringBuilder.append(requestedNum);
            stringBuilder.append(", mClientType : ");
            stringBuilder.append(this.mClientType);
            stringBuilder.append(", mResultState : ");
            stringBuilder.append(this.mResultState);
            Log.e(HwColorPicker.TAG, stringBuilder.toString());
            throw new RuntimeException("Illegal result, colorResult's length must be (requestedNum + 1)!");
        }

        public int getState() {
            return this.mResultState;
        }

        @Deprecated
        public int getDomainColor() {
            return get(ResultType.Domain);
        }

        @Deprecated
        public int getWidgetColor() {
            if (HwColorPicker.isEnable()) {
                return get(ResultType.Widget);
            }
            return WIDGET_DEFAULT_COLOR;
        }

        @Deprecated
        public int getShadowColor() {
            return get(ResultType.Shadow);
        }

        public int get(ResultType resultType) {
            if (this.mColorPicked == null || this.mColorPicked.length <= 0) {
                return 0;
            }
            int index = resultType.index & 65535;
            if (index >= this.mColorPicked.length) {
                return 0;
            }
            return this.mColorPicked[index];
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("PickedColor{");
            sb.append(this.mClientType);
            sb.append(", ");
            sb.append(this.mResultState);
            sb.append(", [");
            if (this.mColorPicked != null) {
                int length = this.mColorPicked.length;
                for (int i = 0; i < length; i++) {
                    sb.append(String.format("0x%08x", new Object[]{Integer.valueOf(this.mColorPicked[i])}));
                    if (i != length - 1) {
                        sb.append(", ");
                    }
                }
            }
            sb.append("]}");
            return sb.toString();
        }
    }

    public enum ResultType {
        Domain(ClientType.Default, 0),
        DomainDark(ClientType.Default, 1),
        DomainDarkB(ClientType.Default, 2),
        DomainDarkC(ClientType.Default, 3),
        DomainLight(ClientType.Default, 4),
        DomainLightB(ClientType.Default, 5),
        DomainLightC(ClientType.Default, 6),
        Second(ClientType.Default, 7),
        SecondDark(ClientType.Default, 8),
        SecondLight(ClientType.Default, 9),
        Widget(ClientType.Default, 10),
        Shadow(ClientType.Default, 11),
        OriginRgb1(ClientType.Extend, 0),
        OriginRgb2(ClientType.Extend, 1),
        OriginRgb3(ClientType.Extend, 2),
        OriginNum1(ClientType.Extend, 3),
        OriginNum2(ClientType.Extend, 4),
        OriginNum3(ClientType.Extend, 5),
        MergedRgb1(ClientType.Extend, 6),
        MergedRgb2(ClientType.Extend, 7),
        MergedRgb3(ClientType.Extend, 8),
        MergedNum1(ClientType.Extend, 9),
        MergedNum2(ClientType.Extend, 10),
        MergedNum3(ClientType.Extend, 11),
        Music_Domain(ClientType.Music, 0),
        Music_Light(ClientType.Music, 1),
        Music_Widget(ClientType.Music, 2),
        Music_Title(ClientType.Music, 3);
        
        private ClientType clientType;
        private int flag;
        private int index;

        private ResultType(ClientType clientType, int index) {
            this.clientType = clientType;
            this.index = index & 65535;
            this.flag = (65535 & index) | ((clientType.index << 24) & -16777216);
        }

        public static int getRequestedColorNum(int clientType) {
            int num = 0;
            for (ResultType resultType : values()) {
                if (clientType == resultType.clientType.index) {
                    num++;
                }
            }
            return num;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(name());
            stringBuilder.append("[");
            stringBuilder.append(this.clientType);
            stringBuilder.append(", ");
            stringBuilder.append(this.index);
            stringBuilder.append(", ");
            stringBuilder.append(String.format("0x%08x", new Object[]{Integer.valueOf(this.flag)}));
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    public enum StateType {
        UnDefined(-1),
        OK(0),
        Error(1),
        None(2),
        Common(3),
        Single(4),
        Gray(5);
        
        private int index;

        private StateType(int index) {
            this.index = index;
        }
    }

    private static class TaskParams {
        Bitmap bitmap;
        ClientType clientType = ClientType.Default;
        Rect rect;

        TaskParams(Bitmap bitmap, Rect rect, ClientType clientType) {
            this.bitmap = bitmap;
            this.rect = rect;
            this.clientType = clientType;
        }
    }

    private static native int[] processPixels(int[] iArr, int i, int i2);

    static {
        sIsLibColorPickerExist = true;
        try {
            System.loadLibrary("colorpicker");
        } catch (UnsatisfiedLinkError e) {
            sIsLibColorPickerExist = false;
            Log.e(TAG, "libcolorpicker.so couldn't be found.");
        }
    }

    public static boolean isColorPickerEnable() {
        if (!sIsLibColorPickerExist) {
            Log.w(TAG, "lib colorPicker is not exist!");
        }
        return sIsLibColorPickerExist;
    }

    public static boolean isDeviceSupport() {
        return IS_EMUI_LITE ^ 1;
    }

    public static boolean isEnable() {
        return isColorPickerEnable() && isDeviceSupport();
    }

    public static PickedColor processBitmap(Bitmap bitmap) {
        if (!isEnable()) {
            return DEFAULT_COLOR;
        }
        int[] pixels = getPixelsFromBitmap(bitmap);
        if (pixels != null) {
            return processPixels(pixels);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPixelsFromBitmap(");
        stringBuilder.append(bitmap);
        stringBuilder.append("), return null!");
        Log.e(str, stringBuilder.toString());
        return DEFAULT_COLOR;
    }

    public static PickedColor processBitmap(Bitmap bitmap, Rect rect) {
        if (!isEnable()) {
            return DEFAULT_COLOR;
        }
        int[] pixels = getPixelsFromBitmap(bitmap, rect);
        if (pixels != null) {
            return processPixels(pixels);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPixelsFromBitmap(");
        stringBuilder.append(bitmap);
        stringBuilder.append(", ");
        stringBuilder.append(rect);
        stringBuilder.append("), return null!");
        Log.e(str, stringBuilder.toString());
        return DEFAULT_COLOR;
    }

    public static PickedColor processBitmap(Bitmap bitmap, ClientType clientType) {
        if (!isEnable()) {
            return DEFAULT_COLOR;
        }
        int[] pixels = getPixelsFromBitmap(bitmap);
        if (pixels != null) {
            return processPixels(pixels, clientType);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPixelsFromBitmap(");
        stringBuilder.append(bitmap);
        stringBuilder.append(", ");
        stringBuilder.append(clientType);
        stringBuilder.append("), return null!");
        Log.e(str, stringBuilder.toString());
        return DEFAULT_COLOR;
    }

    public static PickedColor processBitmap(Bitmap bitmap, Rect rect, ClientType clientType) {
        if (!isEnable()) {
            return DEFAULT_COLOR;
        }
        int[] pixels = getPixelsFromBitmap(bitmap, rect);
        if (pixels != null) {
            return processPixels(pixels, clientType);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPixelsFromBitmap(");
        stringBuilder.append(bitmap);
        stringBuilder.append(", ");
        stringBuilder.append(rect);
        stringBuilder.append(", ");
        stringBuilder.append(clientType);
        stringBuilder.append("), return null!");
        Log.e(str, stringBuilder.toString());
        return DEFAULT_COLOR;
    }

    public static void processBitmapAsync(Bitmap bitmap, AsyncProcessCallBack callback) {
        if (isEnable()) {
            processBitmapAsync(bitmap, null, ClientType.Default, callback);
            return;
        }
        if (callback != null) {
            callback.onColorPicked(DEFAULT_COLOR);
        }
    }

    public static void processBitmapAsync(Bitmap bitmap, Rect rect, AsyncProcessCallBack callback) {
        if (isEnable()) {
            processBitmapAsync(bitmap, rect, ClientType.Default, callback);
            return;
        }
        if (callback != null) {
            callback.onColorPicked(DEFAULT_COLOR);
        }
    }

    public static void processBitmapAsync(Bitmap bitmap, ClientType clientType, AsyncProcessCallBack callback) {
        if (isEnable()) {
            processBitmapAsync(bitmap, null, clientType, callback);
            return;
        }
        if (callback != null) {
            callback.onColorPicked(DEFAULT_COLOR);
        }
    }

    public static void processBitmapAsync(Bitmap bitmap, Rect rect, ClientType clientType, AsyncProcessCallBack callback) {
        if (isEnable()) {
            new ColorPickingTask(callback).execute(new WeakReference[]{new WeakReference(new TaskParams(bitmap, rect, clientType))});
            return;
        }
        if (callback != null) {
            callback.onColorPicked(DEFAULT_COLOR);
        }
    }

    private static int[] getPixelsFromBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            return getPixelsFormFixedSizeBitmap(Bitmap.createScaledBitmap(bitmap, 25, 25, null));
        }
        Log.e(TAG, "bitmap is null, can't be processed!");
        return null;
    }

    private static int[] getPixelsFromBitmap(Bitmap bitmap, Rect rect) {
        if (bitmap == null || rect == null) {
            Log.e(TAG, "bitmap is null or rect is null, can't be processed!");
            return null;
        }
        Rect realRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        if (realRect.intersect(rect)) {
            int x = realRect.left;
            int y = realRect.top;
            int w = realRect.right - realRect.left;
            int h = realRect.bottom - realRect.top;
            if (isSmallSizeRect(w, h)) {
                return createSubBitmapAndScale(bitmap, x, y, w, h);
            }
            return scaleAndCreateSubBitmap(bitmap, x, y, w, h);
        }
        Log.e(TAG, "rect and bitmap's rect has not intersection, can't be processed!");
        return null;
    }

    private static boolean isSmallSizeRect(int w, int h) {
        return Float.compare(((float) (5644.5d * Math.pow((double) w, -0.5400000214576721d))) - ((float) h), 0.0f) > 0;
    }

    private static int[] createSubBitmapAndScale(Bitmap bitmap, int x, int y, int w, int h) {
        return getPixelsFormFixedSizeBitmap(Bitmap.createScaledBitmap(Bitmap.createBitmap(bitmap, x, y, w, h), 25, 25, false));
    }

    private static int[] scaleAndCreateSubBitmap(Bitmap bitmap, int x, int y, int w, int h) {
        int scaledW = Math.round(((float) (bitmap.getWidth() * 25)) / ((float) w));
        int scaledH = Math.round(((float) (bitmap.getHeight() * 25)) / ((float) h));
        return getPixelsFormFixedSizeBitmap(Bitmap.createBitmap(Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, null), Math.round(((float) (x * 25)) / ((float) w)), Math.round(((float) (y * 25)) / ((float) h)), 25, 25));
    }

    private static int[] getPixelsFormFixedSizeBitmap(Bitmap bitmap) {
        int[] pixels = new int[625];
        bitmap.getPixels(pixels, 0, 25, 0, 0, 25, 25);
        return pixels;
    }

    private static PickedColor processPixels(int[] pixels) {
        if (isEnable()) {
            return new PickedColor(processPixels(pixels, pixels.length, ClientType.Default.index));
        }
        return DEFAULT_COLOR;
    }

    private static PickedColor processPixels(int[] pixels, ClientType clientType) {
        if (!isEnable()) {
            return DEFAULT_COLOR;
        }
        return new PickedColor(processPixels(pixels, pixels.length, clientType != null ? clientType.index : ClientType.Default.index));
    }
}
