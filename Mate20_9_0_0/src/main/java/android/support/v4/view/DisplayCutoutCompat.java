package android.support.v4.view;

import android.graphics.Rect;
import android.os.Build.VERSION;
import android.view.DisplayCutout;
import java.util.List;

public final class DisplayCutoutCompat {
    private final Object mDisplayCutout;

    public DisplayCutoutCompat(Rect safeInsets, List<Rect> boundingRects) {
        this(VERSION.SDK_INT >= 28 ? new DisplayCutout(safeInsets, boundingRects) : null);
    }

    private DisplayCutoutCompat(Object displayCutout) {
        this.mDisplayCutout = displayCutout;
    }

    public int getSafeInsetTop() {
        if (VERSION.SDK_INT >= 28) {
            return ((DisplayCutout) this.mDisplayCutout).getSafeInsetTop();
        }
        return 0;
    }

    public int getSafeInsetBottom() {
        if (VERSION.SDK_INT >= 28) {
            return ((DisplayCutout) this.mDisplayCutout).getSafeInsetBottom();
        }
        return 0;
    }

    public int getSafeInsetLeft() {
        if (VERSION.SDK_INT >= 28) {
            return ((DisplayCutout) this.mDisplayCutout).getSafeInsetLeft();
        }
        return 0;
    }

    public int getSafeInsetRight() {
        if (VERSION.SDK_INT >= 28) {
            return ((DisplayCutout) this.mDisplayCutout).getSafeInsetRight();
        }
        return 0;
    }

    public List<Rect> getBoundingRects() {
        if (VERSION.SDK_INT >= 28) {
            return ((DisplayCutout) this.mDisplayCutout).getBoundingRects();
        }
        return null;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DisplayCutoutCompat other = (DisplayCutoutCompat) o;
        if (this.mDisplayCutout != null) {
            z = this.mDisplayCutout.equals(other.mDisplayCutout);
        } else if (other.mDisplayCutout != null) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return this.mDisplayCutout == null ? 0 : this.mDisplayCutout.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DisplayCutoutCompat{");
        stringBuilder.append(this.mDisplayCutout);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    static DisplayCutoutCompat wrap(Object displayCutout) {
        return displayCutout == null ? null : new DisplayCutoutCompat(displayCutout);
    }
}
