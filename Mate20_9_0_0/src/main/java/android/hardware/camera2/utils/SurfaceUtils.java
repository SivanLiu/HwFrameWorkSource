package android.hardware.camera2.utils;

import android.hardware.camera2.legacy.LegacyCameraDevice;
import android.hardware.camera2.legacy.LegacyExceptionUtils.BufferQueueAbandonedException;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SurfaceUtils {
    public static boolean isSurfaceForPreview(Surface surface) {
        return LegacyCameraDevice.isPreviewConsumer(surface);
    }

    public static boolean isSurfaceForHwVideoEncoder(Surface surface) {
        return LegacyCameraDevice.isVideoEncoderConsumer(surface);
    }

    public static long getSurfaceId(Surface surface) {
        try {
            return LegacyCameraDevice.getSurfaceId(surface);
        } catch (BufferQueueAbandonedException e) {
            return 0;
        }
    }

    public static Size getSurfaceSize(Surface surface) {
        try {
            return LegacyCameraDevice.getSurfaceSize(surface);
        } catch (BufferQueueAbandonedException e) {
            throw new IllegalArgumentException("Surface was abandoned", e);
        }
    }

    public static int getSurfaceFormat(Surface surface) {
        try {
            return LegacyCameraDevice.detectSurfaceType(surface);
        } catch (BufferQueueAbandonedException e) {
            throw new IllegalArgumentException("Surface was abandoned", e);
        }
    }

    public static int getSurfaceDataspace(Surface surface) {
        try {
            return LegacyCameraDevice.detectSurfaceDataspace(surface);
        } catch (BufferQueueAbandonedException e) {
            throw new IllegalArgumentException("Surface was abandoned", e);
        }
    }

    public static boolean isFlexibleConsumer(Surface output) {
        return LegacyCameraDevice.isFlexibleConsumer(output);
    }

    private static void checkHighSpeedSurfaceFormat(Surface surface) {
        int surfaceFormat = getSurfaceFormat(surface);
        if (surfaceFormat != 34) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Surface format(");
            stringBuilder.append(surfaceFormat);
            stringBuilder.append(") is not for preview or hardware video encoding!");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static void checkConstrainedHighSpeedSurfaces(Collection<Surface> surfaces, Range<Integer> fpsRange, StreamConfigurationMap config) {
        if (surfaces == null || surfaces.size() == 0 || surfaces.size() > 2) {
            throw new IllegalArgumentException("Output target surface list must not be null and the size must be 1 or 2");
        }
        List<Size> highSpeedSizes;
        if (fpsRange == null) {
            highSpeedSizes = Arrays.asList(config.getHighSpeedVideoSizes());
        } else {
            Range<Integer>[] highSpeedFpsRanges = config.getHighSpeedVideoFpsRanges();
            if (Arrays.asList(highSpeedFpsRanges).contains(fpsRange)) {
                highSpeedSizes = Arrays.asList(config.getHighSpeedVideoSizesFor(fpsRange));
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Fps range ");
                stringBuilder.append(fpsRange.toString());
                stringBuilder.append(" in the request is not a supported high speed fps range ");
                stringBuilder.append(Arrays.toString(highSpeedFpsRanges));
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        for (Surface surface : surfaces) {
            checkHighSpeedSurfaceFormat(surface);
            Size surfaceSize = getSurfaceSize(surface);
            if (!highSpeedSizes.contains(surfaceSize)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Surface size ");
                stringBuilder2.append(surfaceSize.toString());
                stringBuilder2.append(" is not part of the high speed supported size list ");
                stringBuilder2.append(Arrays.toString(highSpeedSizes.toArray()));
                throw new IllegalArgumentException(stringBuilder2.toString());
            } else if (!isSurfaceForPreview(surface) && !isSurfaceForHwVideoEncoder(surface)) {
                throw new IllegalArgumentException("This output surface is neither preview nor hardware video encoding surface");
            } else if (isSurfaceForPreview(surface) && isSurfaceForHwVideoEncoder(surface)) {
                throw new IllegalArgumentException("This output surface can not be both preview and hardware video encoding surface");
            }
        }
        if (surfaces.size() == 2) {
            Iterator<Surface> iterator = surfaces.iterator();
            if (isSurfaceForPreview((Surface) iterator.next()) == isSurfaceForPreview((Surface) iterator.next())) {
                throw new IllegalArgumentException("The 2 output surfaces must have different type");
            }
        }
    }
}
