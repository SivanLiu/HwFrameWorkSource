package android.view;

import android.common.HwFrameworkFactory;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.proto.ProtoOutputStream;
import android.view.Display.HdrCapabilities;
import android.view.Display.Mode;
import android.view.DisplayCutout.ParcelableWrapper;
import com.huawei.forcerotation.IForceRotationManager;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public final class DisplayInfo implements Parcelable {
    public static final Creator<DisplayInfo> CREATOR = new Creator<DisplayInfo>() {
        public DisplayInfo createFromParcel(Parcel source) {
            return new DisplayInfo(source, null);
        }

        public DisplayInfo[] newArray(int size) {
            return new DisplayInfo[size];
        }
    };
    public String address;
    public int appHeight;
    public long appVsyncOffsetNanos;
    public int appWidth;
    public int colorMode;
    public int defaultModeId;
    public DisplayCutout displayCutout;
    public int flags;
    public HdrCapabilities hdrCapabilities;
    public int largestNominalAppHeight;
    public int largestNominalAppWidth;
    public int layerStack;
    public int logicalDensityDpi;
    public int logicalHeight;
    public int logicalWidth;
    public int modeId;
    public String name;
    public int overscanBottom;
    public int overscanLeft;
    public int overscanRight;
    public int overscanTop;
    public String ownerPackageName;
    public int ownerUid;
    public float physicalXDpi;
    public float physicalYDpi;
    public long presentationDeadlineNanos;
    public int removeMode;
    public int rotation;
    public int smallestNominalAppHeight;
    public int smallestNominalAppWidth;
    public int state;
    public int[] supportedColorModes;
    public Mode[] supportedModes;
    public int type;
    public String uniqueId;

    /* synthetic */ DisplayInfo(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public DisplayInfo() {
        this.supportedModes = Mode.EMPTY_ARRAY;
        this.supportedColorModes = new int[]{0};
        this.removeMode = 0;
    }

    public DisplayInfo(DisplayInfo other) {
        this.supportedModes = Mode.EMPTY_ARRAY;
        this.supportedColorModes = new int[]{0};
        this.removeMode = 0;
        copyFrom(other);
    }

    private DisplayInfo(Parcel source) {
        this.supportedModes = Mode.EMPTY_ARRAY;
        this.supportedColorModes = new int[]{0};
        this.removeMode = 0;
        readFromParcel(source);
    }

    public boolean equals(Object o) {
        return (o instanceof DisplayInfo) && equals((DisplayInfo) o);
    }

    public boolean equals(DisplayInfo other) {
        return other != null && this.layerStack == other.layerStack && this.flags == other.flags && this.type == other.type && Objects.equals(this.address, other.address) && Objects.equals(this.uniqueId, other.uniqueId) && this.appWidth == other.appWidth && this.appHeight == other.appHeight && this.smallestNominalAppWidth == other.smallestNominalAppWidth && this.smallestNominalAppHeight == other.smallestNominalAppHeight && this.largestNominalAppWidth == other.largestNominalAppWidth && this.largestNominalAppHeight == other.largestNominalAppHeight && this.logicalWidth == other.logicalWidth && this.logicalHeight == other.logicalHeight && this.overscanLeft == other.overscanLeft && this.overscanTop == other.overscanTop && this.overscanRight == other.overscanRight && this.overscanBottom == other.overscanBottom && Objects.equals(this.displayCutout, other.displayCutout) && this.rotation == other.rotation && this.modeId == other.modeId && this.defaultModeId == other.defaultModeId && this.colorMode == other.colorMode && Arrays.equals(this.supportedColorModes, other.supportedColorModes) && Objects.equals(this.hdrCapabilities, other.hdrCapabilities) && this.logicalDensityDpi == other.logicalDensityDpi && this.physicalXDpi == other.physicalXDpi && this.physicalYDpi == other.physicalYDpi && this.appVsyncOffsetNanos == other.appVsyncOffsetNanos && this.presentationDeadlineNanos == other.presentationDeadlineNanos && this.state == other.state && this.ownerUid == other.ownerUid && Objects.equals(this.ownerPackageName, other.ownerPackageName) && this.removeMode == other.removeMode;
    }

    public int hashCode() {
        return 0;
    }

    public void copyFrom(DisplayInfo other) {
        this.layerStack = other.layerStack;
        this.flags = other.flags;
        this.type = other.type;
        this.address = other.address;
        this.name = other.name;
        this.uniqueId = other.uniqueId;
        this.appWidth = other.appWidth;
        this.appHeight = other.appHeight;
        this.smallestNominalAppWidth = other.smallestNominalAppWidth;
        this.smallestNominalAppHeight = other.smallestNominalAppHeight;
        this.largestNominalAppWidth = other.largestNominalAppWidth;
        this.largestNominalAppHeight = other.largestNominalAppHeight;
        this.logicalWidth = other.logicalWidth;
        this.logicalHeight = other.logicalHeight;
        this.overscanLeft = other.overscanLeft;
        this.overscanTop = other.overscanTop;
        this.overscanRight = other.overscanRight;
        this.overscanBottom = other.overscanBottom;
        this.displayCutout = other.displayCutout;
        this.rotation = other.rotation;
        this.modeId = other.modeId;
        this.defaultModeId = other.defaultModeId;
        this.supportedModes = (Mode[]) Arrays.copyOf(other.supportedModes, other.supportedModes.length);
        this.colorMode = other.colorMode;
        this.supportedColorModes = Arrays.copyOf(other.supportedColorModes, other.supportedColorModes.length);
        this.hdrCapabilities = other.hdrCapabilities;
        this.logicalDensityDpi = other.logicalDensityDpi;
        this.physicalXDpi = other.physicalXDpi;
        this.physicalYDpi = other.physicalYDpi;
        this.appVsyncOffsetNanos = other.appVsyncOffsetNanos;
        this.presentationDeadlineNanos = other.presentationDeadlineNanos;
        this.state = other.state;
        this.ownerUid = other.ownerUid;
        this.ownerPackageName = other.ownerPackageName;
        this.removeMode = other.removeMode;
    }

    public void readFromParcel(Parcel source) {
        int i;
        this.layerStack = source.readInt();
        this.flags = source.readInt();
        this.type = source.readInt();
        this.address = source.readString();
        this.name = source.readString();
        this.appWidth = source.readInt();
        this.appHeight = source.readInt();
        this.smallestNominalAppWidth = source.readInt();
        this.smallestNominalAppHeight = source.readInt();
        this.largestNominalAppWidth = source.readInt();
        this.largestNominalAppHeight = source.readInt();
        this.logicalWidth = source.readInt();
        this.logicalHeight = source.readInt();
        this.overscanLeft = source.readInt();
        this.overscanTop = source.readInt();
        this.overscanRight = source.readInt();
        this.overscanBottom = source.readInt();
        this.displayCutout = ParcelableWrapper.readCutoutFromParcel(source);
        this.rotation = source.readInt();
        this.modeId = source.readInt();
        this.defaultModeId = source.readInt();
        int nModes = source.readInt();
        this.supportedModes = new Mode[nModes];
        int i2 = 0;
        for (i = 0; i < nModes; i++) {
            this.supportedModes[i] = (Mode) Mode.CREATOR.createFromParcel(source);
        }
        this.colorMode = source.readInt();
        i = source.readInt();
        this.supportedColorModes = new int[i];
        while (i2 < i) {
            this.supportedColorModes[i2] = source.readInt();
            i2++;
        }
        this.hdrCapabilities = (HdrCapabilities) source.readParcelable(null);
        this.logicalDensityDpi = source.readInt();
        this.physicalXDpi = source.readFloat();
        this.physicalYDpi = source.readFloat();
        this.appVsyncOffsetNanos = source.readLong();
        this.presentationDeadlineNanos = source.readLong();
        this.state = source.readInt();
        this.ownerUid = source.readInt();
        this.ownerPackageName = source.readString();
        this.uniqueId = source.readString();
        this.removeMode = source.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.layerStack);
        dest.writeInt(this.flags);
        dest.writeInt(this.type);
        dest.writeString(this.address);
        dest.writeString(this.name);
        dest.writeInt(this.appWidth);
        dest.writeInt(this.appHeight);
        dest.writeInt(this.smallestNominalAppWidth);
        dest.writeInt(this.smallestNominalAppHeight);
        dest.writeInt(this.largestNominalAppWidth);
        dest.writeInt(this.largestNominalAppHeight);
        dest.writeInt(this.logicalWidth);
        dest.writeInt(this.logicalHeight);
        dest.writeInt(this.overscanLeft);
        dest.writeInt(this.overscanTop);
        dest.writeInt(this.overscanRight);
        dest.writeInt(this.overscanBottom);
        ParcelableWrapper.writeCutoutToParcel(this.displayCutout, dest, flags);
        dest.writeInt(this.rotation);
        dest.writeInt(this.modeId);
        dest.writeInt(this.defaultModeId);
        dest.writeInt(this.supportedModes.length);
        int i = 0;
        for (Mode writeToParcel : this.supportedModes) {
            writeToParcel.writeToParcel(dest, flags);
        }
        dest.writeInt(this.colorMode);
        dest.writeInt(this.supportedColorModes.length);
        while (i < this.supportedColorModes.length) {
            dest.writeInt(this.supportedColorModes[i]);
            i++;
        }
        dest.writeParcelable(this.hdrCapabilities, flags);
        dest.writeInt(this.logicalDensityDpi);
        dest.writeFloat(this.physicalXDpi);
        dest.writeFloat(this.physicalYDpi);
        dest.writeLong(this.appVsyncOffsetNanos);
        dest.writeLong(this.presentationDeadlineNanos);
        dest.writeInt(this.state);
        dest.writeInt(this.ownerUid);
        dest.writeString(this.ownerPackageName);
        dest.writeString(this.uniqueId);
        dest.writeInt(this.removeMode);
    }

    public int describeContents() {
        return 0;
    }

    public Mode getMode() {
        return findMode(this.modeId);
    }

    public Mode getDefaultMode() {
        return findMode(this.defaultModeId);
    }

    private Mode findMode(int id) {
        for (int i = 0; i < this.supportedModes.length; i++) {
            if (this.supportedModes[i].getModeId() == id) {
                return this.supportedModes[i];
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to locate mode ");
        stringBuilder.append(id);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public int findDefaultModeByRefreshRate(float refreshRate) {
        Mode[] modes = this.supportedModes;
        Mode defaultMode = getDefaultMode();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].matches(defaultMode.getPhysicalWidth(), defaultMode.getPhysicalHeight(), refreshRate)) {
                return modes[i].getModeId();
            }
        }
        return 0;
    }

    public float[] getDefaultRefreshRates() {
        Mode[] modes = this.supportedModes;
        ArraySet<Float> rates = new ArraySet();
        Mode defaultMode = getDefaultMode();
        for (Mode mode : modes) {
            if (mode.getPhysicalWidth() == defaultMode.getPhysicalWidth() && mode.getPhysicalHeight() == defaultMode.getPhysicalHeight()) {
                rates.add(Float.valueOf(mode.getRefreshRate()));
            }
        }
        float[] result = new float[rates.size()];
        int i = 0;
        Iterator it = rates.iterator();
        while (it.hasNext()) {
            int i2 = i + 1;
            result[i] = ((Float) it.next()).floatValue();
            i = i2;
        }
        return result;
    }

    public void getAppMetrics(DisplayMetrics outMetrics) {
        getAppMetrics(outMetrics, CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO, null);
    }

    public void getAppMetrics(DisplayMetrics outMetrics, DisplayAdjustments displayAdjustments) {
        getMetricsWithSize(outMetrics, displayAdjustments.getCompatibilityInfo(), displayAdjustments.getConfiguration(), this.appWidth, this.appHeight);
    }

    public void getAppMetrics(DisplayMetrics outMetrics, CompatibilityInfo ci, Configuration configuration) {
        getMetricsWithSize(outMetrics, ci, configuration, this.appWidth, this.appHeight);
    }

    public void getLogicalMetrics(DisplayMetrics outMetrics, CompatibilityInfo compatInfo, Configuration configuration) {
        getMetricsWithSize(outMetrics, compatInfo, configuration, this.logicalWidth, this.logicalHeight);
    }

    public int getNaturalWidth() {
        return (this.rotation == 0 || this.rotation == 2) ? this.logicalWidth : this.logicalHeight;
    }

    public int getNaturalHeight() {
        return (this.rotation == 0 || this.rotation == 2) ? this.logicalHeight : this.logicalWidth;
    }

    public boolean isHdr() {
        int[] types = this.hdrCapabilities != null ? this.hdrCapabilities.getSupportedHdrTypes() : null;
        return types != null && types.length > 0;
    }

    public boolean isWideColorGamut() {
        for (int colorMode : this.supportedColorModes) {
            if (colorMode == 6 || colorMode > 7) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAccess(int uid) {
        return Display.hasAccess(uid, this.flags, this.ownerUid);
    }

    private void getMetricsWithSize(DisplayMetrics outMetrics, CompatibilityInfo compatInfo, Configuration configuration, int width, int height) {
        int i = this.logicalDensityDpi;
        outMetrics.noncompatDensityDpi = i;
        outMetrics.densityDpi = i;
        float f = ((float) this.logicalDensityDpi) * 0.00625f;
        outMetrics.noncompatDensity = f;
        outMetrics.density = f;
        f = outMetrics.density;
        outMetrics.noncompatScaledDensity = f;
        outMetrics.scaledDensity = f;
        f = this.physicalXDpi;
        outMetrics.noncompatXdpi = f;
        outMetrics.xdpi = f;
        f = this.physicalYDpi;
        outMetrics.noncompatYdpi = f;
        outMetrics.ydpi = f;
        Rect appBounds = configuration != null ? configuration.windowConfiguration.getAppBounds() : null;
        width = appBounds != null ? appBounds.width() : width;
        height = appBounds != null ? appBounds.height() : height;
        IForceRotationManager forceRotationManager = HwFrameworkFactory.getForceRotationManager();
        if (forceRotationManager.isForceRotationSupported()) {
            width = forceRotationManager.recalculateWidthForForceRotation(width, height, this.logicalHeight);
        }
        outMetrics.widthPixels = width;
        outMetrics.noncompatWidthPixels = width;
        outMetrics.heightPixels = height;
        outMetrics.noncompatHeightPixels = height;
        if (!compatInfo.equals(CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO)) {
            compatInfo.applyToDisplayMetrics(outMetrics);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DisplayInfo{\"");
        sb.append(this.name);
        sb.append("\", uniqueId \"");
        sb.append(this.uniqueId);
        sb.append("\", app ");
        sb.append(this.appWidth);
        sb.append(" x ");
        sb.append(this.appHeight);
        sb.append(", real ");
        sb.append(this.logicalWidth);
        sb.append(" x ");
        sb.append(this.logicalHeight);
        if (!(this.overscanLeft == 0 && this.overscanTop == 0 && this.overscanRight == 0 && this.overscanBottom == 0)) {
            sb.append(", overscan (");
            sb.append(this.overscanLeft);
            sb.append(",");
            sb.append(this.overscanTop);
            sb.append(",");
            sb.append(this.overscanRight);
            sb.append(",");
            sb.append(this.overscanBottom);
            sb.append(")");
        }
        sb.append(", largest app ");
        sb.append(this.largestNominalAppWidth);
        sb.append(" x ");
        sb.append(this.largestNominalAppHeight);
        sb.append(", smallest app ");
        sb.append(this.smallestNominalAppWidth);
        sb.append(" x ");
        sb.append(this.smallestNominalAppHeight);
        sb.append(", mode ");
        sb.append(this.modeId);
        sb.append(", defaultMode ");
        sb.append(this.defaultModeId);
        sb.append(", modes ");
        sb.append(Arrays.toString(this.supportedModes));
        sb.append(", colorMode ");
        sb.append(this.colorMode);
        sb.append(", supportedColorModes ");
        sb.append(Arrays.toString(this.supportedColorModes));
        sb.append(", hdrCapabilities ");
        sb.append(this.hdrCapabilities);
        sb.append(", rotation ");
        sb.append(this.rotation);
        sb.append(", density ");
        sb.append(this.logicalDensityDpi);
        sb.append(" (");
        sb.append(this.physicalXDpi);
        sb.append(" x ");
        sb.append(this.physicalYDpi);
        sb.append(") dpi, layerStack ");
        sb.append(this.layerStack);
        sb.append(", appVsyncOff ");
        sb.append(this.appVsyncOffsetNanos);
        sb.append(", presDeadline ");
        sb.append(this.presentationDeadlineNanos);
        sb.append(", type ");
        sb.append(Display.typeToString(this.type));
        if (this.address != null) {
            sb.append(", address ");
            sb.append(this.address);
        }
        sb.append(", state ");
        sb.append(Display.stateToString(this.state));
        if (!(this.ownerUid == 0 && this.ownerPackageName == null)) {
            sb.append(", owner ");
            sb.append(this.ownerPackageName);
            sb.append(" (uid ");
            sb.append(this.ownerUid);
            sb.append(")");
        }
        sb.append(flagsToString(this.flags));
        sb.append(", removeMode ");
        sb.append(this.removeMode);
        sb.append("}");
        return sb.toString();
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long fieldId) {
        long token = protoOutputStream.start(fieldId);
        protoOutputStream.write(1120986464257L, this.logicalWidth);
        protoOutputStream.write(1120986464258L, this.logicalHeight);
        protoOutputStream.write(1120986464259L, this.appWidth);
        protoOutputStream.write(1120986464260L, this.appHeight);
        protoOutputStream.write(1138166333445L, this.name);
        protoOutputStream.end(token);
    }

    private static String flagsToString(int flags) {
        StringBuilder result = new StringBuilder();
        if ((flags & 2) != 0) {
            result.append(", FLAG_SECURE");
        }
        if ((flags & 1) != 0) {
            result.append(", FLAG_SUPPORTS_PROTECTED_BUFFERS");
        }
        if ((flags & 4) != 0) {
            result.append(", FLAG_PRIVATE");
        }
        if ((flags & 8) != 0) {
            result.append(", FLAG_PRESENTATION");
        }
        if ((1073741824 & flags) != 0) {
            result.append(", FLAG_SCALING_DISABLED");
        }
        if ((flags & 16) != 0) {
            result.append(", FLAG_ROUND");
        }
        return result.toString();
    }
}
