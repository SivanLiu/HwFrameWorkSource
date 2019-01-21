package huawei.android.widget.effect.engine;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.util.Log;
import android.view.View;
import huawei.android.hwcolorpicker.HwColorPicker;
import huawei.android.hwcolorpicker.HwColorPicker.ClientType;
import huawei.android.hwcolorpicker.HwColorPicker.PickedColor;
import huawei.android.hwcolorpicker.HwColorPicker.ResultType;

public class HwColorPickerEngine {
    private static final int DEFAULT_COLOR = 0;
    private static final String TAG = "HwColorPickerEngine";
    private static boolean mEnable = true;

    public static int getColor(View view, ClientType clientType, ResultType resultType) {
        if (view == null || clientType == null || resultType == null) {
            Log.w(TAG, "bitmap and clientType and resultType cannot be null");
            return 0;
        } else if (!isEnable()) {
            return 0;
        } else {
            try {
                Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Config.ARGB_8888);
                PickedColor pickedColor = HwColorPicker.processBitmap(bitmap, clientType);
                if (bitmap != null) {
                    bitmap.recycle();
                }
                if (pickedColor == null) {
                    return 0;
                }
                return pickedColor.get(resultType);
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("An exception has occurred : ");
                stringBuilder.append(e.getMessage());
                Log.w(str, stringBuilder.toString());
                return 0;
            }
        }
    }

    public static int getColor(Bitmap bitmap, ClientType clientType, ResultType resultType) {
        if (bitmap == null || clientType == null || resultType == null) {
            Log.w(TAG, "bitmap and clientType and resultType cannot be null");
            return 0;
        } else if (!isEnable()) {
            return 0;
        } else {
            PickedColor pickedColor = HwColorPicker.processBitmap(bitmap, clientType);
            if (pickedColor == null) {
                return 0;
            }
            return pickedColor.get(resultType);
        }
    }

    public static boolean isEnable() {
        return HwColorPicker.isEnable() && mEnable;
    }

    public static void setEnable(boolean enable) {
        mEnable = enable;
    }
}
