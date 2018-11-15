package android.support.v4.widget;

import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ListPopupWindow;

public final class ListPopupWindowCompat {
    private ListPopupWindowCompat() {
    }

    @Deprecated
    public static OnTouchListener createDragToOpenListener(Object listPopupWindow, View src) {
        return createDragToOpenListener((ListPopupWindow) listPopupWindow, src);
    }

    @Nullable
    public static OnTouchListener createDragToOpenListener(@NonNull ListPopupWindow listPopupWindow, @NonNull View src) {
        if (VERSION.SDK_INT >= 19) {
            return listPopupWindow.createDragToOpenListener(src);
        }
        return null;
    }
}
