package android.hardware.display;

import android.graphics.Rect;
import android.text.TextUtils;

public final class DisplayViewport {
    public int deviceHeight;
    public int deviceWidth;
    public int displayId;
    public final Rect logicalFrame = new Rect();
    public int orientation;
    public final Rect physicalFrame = new Rect();
    public String uniqueId;
    public boolean valid;

    public void copyFrom(DisplayViewport viewport) {
        this.valid = viewport.valid;
        this.displayId = viewport.displayId;
        this.orientation = viewport.orientation;
        this.logicalFrame.set(viewport.logicalFrame);
        this.physicalFrame.set(viewport.physicalFrame);
        this.deviceWidth = viewport.deviceWidth;
        this.deviceHeight = viewport.deviceHeight;
        this.uniqueId = viewport.uniqueId;
    }

    public DisplayViewport makeCopy() {
        DisplayViewport dv = new DisplayViewport();
        dv.copyFrom(this);
        return dv;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (o == this) {
            return true;
        }
        if (!(o instanceof DisplayViewport)) {
            return false;
        }
        DisplayViewport other = (DisplayViewport) o;
        if (!(this.valid == other.valid && this.displayId == other.displayId && this.orientation == other.orientation && this.logicalFrame.equals(other.logicalFrame) && this.physicalFrame.equals(other.physicalFrame) && this.deviceWidth == other.deviceWidth && this.deviceHeight == other.deviceHeight && TextUtils.equals(this.uniqueId, other.uniqueId))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        int result = 1 + ((31 * 1) + this.valid);
        result += (31 * result) + this.displayId;
        result += (31 * result) + this.orientation;
        result += (31 * result) + this.logicalFrame.hashCode();
        result += (31 * result) + this.physicalFrame.hashCode();
        result += (31 * result) + this.deviceWidth;
        result += (31 * result) + this.deviceHeight;
        return result + ((31 * result) + this.uniqueId.hashCode());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DisplayViewport{valid=");
        stringBuilder.append(this.valid);
        stringBuilder.append(", displayId=");
        stringBuilder.append(this.displayId);
        stringBuilder.append(", uniqueId='");
        stringBuilder.append(this.uniqueId);
        stringBuilder.append("', orientation=");
        stringBuilder.append(this.orientation);
        stringBuilder.append(", logicalFrame=");
        stringBuilder.append(this.logicalFrame);
        stringBuilder.append(", physicalFrame=");
        stringBuilder.append(this.physicalFrame);
        stringBuilder.append(", deviceWidth=");
        stringBuilder.append(this.deviceWidth);
        stringBuilder.append(", deviceHeight=");
        stringBuilder.append(this.deviceHeight);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
