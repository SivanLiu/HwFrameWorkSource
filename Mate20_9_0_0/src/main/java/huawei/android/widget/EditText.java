package huawei.android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import huawei.android.graphics.drawable.HwAnimatedGradientDrawable;

public class EditText extends android.widget.EditText {
    private static final String TAG = "EditText";

    public EditText(Context context) {
        this(context, null);
    }

    public EditText(Context context, AttributeSet attrs) {
        this(context, attrs, 16842862);
    }

    public EditText(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        Drawable bg = getBackground();
        HwAnimatedGradientDrawable drawable = HwWidgetUtils.getHwAnimatedGradientDrawable(context, defStyleAttr);
        if (bg != null) {
            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{drawable, bg});
            layerDrawable.setPaddingMode(1);
            setBackground(layerDrawable);
            return;
        }
        setBackground(drawable);
    }
}
