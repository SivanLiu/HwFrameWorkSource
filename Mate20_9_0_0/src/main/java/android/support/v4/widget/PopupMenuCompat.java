package android.support.v4.widget;

import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View.OnTouchListener;
import android.widget.PopupMenu;

public final class PopupMenuCompat {
    private PopupMenuCompat() {
    }

    @Nullable
    public static OnTouchListener getDragToOpenListener(@NonNull Object popupMenu) {
        if (VERSION.SDK_INT >= 19) {
            return ((PopupMenu) popupMenu).getDragToOpenListener();
        }
        return null;
    }
}
