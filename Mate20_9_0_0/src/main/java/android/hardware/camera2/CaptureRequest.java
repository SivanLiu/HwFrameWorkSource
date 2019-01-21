package android.hardware.camera2;

import android.content.RestrictionsManager;
import android.graphics.Rect;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.impl.PublicKey;
import android.hardware.camera2.impl.SyntheticKey;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.TonemapCurve;
import android.hardware.camera2.utils.HashCodeHelpers;
import android.hardware.camera2.utils.SurfaceUtils;
import android.hardware.camera2.utils.TypeReference;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.ArraySet;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public final class CaptureRequest extends CameraMetadata<Key<?>> implements Parcelable {
    @PublicKey
    public static final Key<Boolean> BLACK_LEVEL_LOCK = new Key("android.blackLevel.lock", Boolean.TYPE);
    @PublicKey
    public static final Key<Integer> COLOR_CORRECTION_ABERRATION_MODE = new Key("android.colorCorrection.aberrationMode", Integer.TYPE);
    @PublicKey
    public static final Key<RggbChannelVector> COLOR_CORRECTION_GAINS = new Key("android.colorCorrection.gains", RggbChannelVector.class);
    @PublicKey
    public static final Key<Integer> COLOR_CORRECTION_MODE = new Key("android.colorCorrection.mode", Integer.TYPE);
    @PublicKey
    public static final Key<ColorSpaceTransform> COLOR_CORRECTION_TRANSFORM = new Key("android.colorCorrection.transform", ColorSpaceTransform.class);
    @PublicKey
    public static final Key<Integer> CONTROL_AE_ANTIBANDING_MODE = new Key("android.control.aeAntibandingMode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> CONTROL_AE_EXPOSURE_COMPENSATION = new Key("android.control.aeExposureCompensation", Integer.TYPE);
    @PublicKey
    public static final Key<Boolean> CONTROL_AE_LOCK = new Key("android.control.aeLock", Boolean.TYPE);
    @PublicKey
    public static final Key<Integer> CONTROL_AE_MODE = new Key("android.control.aeMode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> CONTROL_AE_PRECAPTURE_TRIGGER = new Key("android.control.aePrecaptureTrigger", Integer.TYPE);
    @PublicKey
    public static final Key<MeteringRectangle[]> CONTROL_AE_REGIONS = new Key("android.control.aeRegions", MeteringRectangle[].class);
    @PublicKey
    public static final Key<Range<Integer>> CONTROL_AE_TARGET_FPS_RANGE = new Key("android.control.aeTargetFpsRange", new TypeReference<Range<Integer>>() {
    });
    @PublicKey
    public static final Key<Integer> CONTROL_AF_MODE = new Key("android.control.afMode", Integer.TYPE);
    @PublicKey
    public static final Key<MeteringRectangle[]> CONTROL_AF_REGIONS = new Key("android.control.afRegions", MeteringRectangle[].class);
    @PublicKey
    public static final Key<Integer> CONTROL_AF_TRIGGER = new Key("android.control.afTrigger", Integer.TYPE);
    @PublicKey
    public static final Key<Boolean> CONTROL_AWB_LOCK = new Key("android.control.awbLock", Boolean.TYPE);
    @PublicKey
    public static final Key<Integer> CONTROL_AWB_MODE = new Key("android.control.awbMode", Integer.TYPE);
    @PublicKey
    public static final Key<MeteringRectangle[]> CONTROL_AWB_REGIONS = new Key("android.control.awbRegions", MeteringRectangle[].class);
    @PublicKey
    public static final Key<Integer> CONTROL_CAPTURE_INTENT = new Key("android.control.captureIntent", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> CONTROL_EFFECT_MODE = new Key("android.control.effectMode", Integer.TYPE);
    @PublicKey
    public static final Key<Boolean> CONTROL_ENABLE_ZSL = new Key("android.control.enableZsl", Boolean.TYPE);
    @PublicKey
    public static final Key<Integer> CONTROL_MODE = new Key("android.control.mode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> CONTROL_POST_RAW_SENSITIVITY_BOOST = new Key("android.control.postRawSensitivityBoost", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> CONTROL_SCENE_MODE = new Key("android.control.sceneMode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> CONTROL_VIDEO_STABILIZATION_MODE = new Key("android.control.videoStabilizationMode", Integer.TYPE);
    public static final Creator<CaptureRequest> CREATOR = new Creator<CaptureRequest>() {
        public CaptureRequest createFromParcel(Parcel in) {
            CaptureRequest request = new CaptureRequest();
            request.readFromParcel(in);
            return request;
        }

        public CaptureRequest[] newArray(int size) {
            return new CaptureRequest[size];
        }
    };
    @PublicKey
    public static final Key<Integer> DISTORTION_CORRECTION_MODE = new Key("android.distortionCorrection.mode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> EDGE_MODE = new Key("android.edge.mode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> FLASH_MODE = new Key("android.flash.mode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> HOT_PIXEL_MODE = new Key("android.hotPixel.mode", Integer.TYPE);
    public static final Key<double[]> JPEG_GPS_COORDINATES = new Key("android.jpeg.gpsCoordinates", double[].class);
    @PublicKey
    @SyntheticKey
    public static final Key<Location> JPEG_GPS_LOCATION = new Key("android.jpeg.gpsLocation", Location.class);
    public static final Key<String> JPEG_GPS_PROCESSING_METHOD = new Key("android.jpeg.gpsProcessingMethod", String.class);
    public static final Key<Long> JPEG_GPS_TIMESTAMP = new Key("android.jpeg.gpsTimestamp", Long.TYPE);
    @PublicKey
    public static final Key<Integer> JPEG_ORIENTATION = new Key("android.jpeg.orientation", Integer.TYPE);
    @PublicKey
    public static final Key<Byte> JPEG_QUALITY = new Key("android.jpeg.quality", Byte.TYPE);
    @PublicKey
    public static final Key<Byte> JPEG_THUMBNAIL_QUALITY = new Key("android.jpeg.thumbnailQuality", Byte.TYPE);
    @PublicKey
    public static final Key<Size> JPEG_THUMBNAIL_SIZE = new Key("android.jpeg.thumbnailSize", Size.class);
    public static final Key<Boolean> LED_TRANSMIT = new Key("android.led.transmit", Boolean.TYPE);
    @PublicKey
    public static final Key<Float> LENS_APERTURE = new Key("android.lens.aperture", Float.TYPE);
    @PublicKey
    public static final Key<Float> LENS_FILTER_DENSITY = new Key("android.lens.filterDensity", Float.TYPE);
    @PublicKey
    public static final Key<Float> LENS_FOCAL_LENGTH = new Key("android.lens.focalLength", Float.TYPE);
    @PublicKey
    public static final Key<Float> LENS_FOCUS_DISTANCE = new Key("android.lens.focusDistance", Float.TYPE);
    @PublicKey
    public static final Key<Integer> LENS_OPTICAL_STABILIZATION_MODE = new Key("android.lens.opticalStabilizationMode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> NOISE_REDUCTION_MODE = new Key("android.noiseReduction.mode", Integer.TYPE);
    @PublicKey
    public static final Key<Float> REPROCESS_EFFECTIVE_EXPOSURE_FACTOR = new Key("android.reprocess.effectiveExposureFactor", Float.TYPE);
    public static final Key<Integer> REQUEST_ID = new Key(RestrictionsManager.REQUEST_KEY_ID, Integer.TYPE);
    @PublicKey
    public static final Key<Rect> SCALER_CROP_REGION = new Key("android.scaler.cropRegion", Rect.class);
    @PublicKey
    public static final Key<Long> SENSOR_EXPOSURE_TIME = new Key("android.sensor.exposureTime", Long.TYPE);
    @PublicKey
    public static final Key<Long> SENSOR_FRAME_DURATION = new Key("android.sensor.frameDuration", Long.TYPE);
    @PublicKey
    public static final Key<Integer> SENSOR_SENSITIVITY = new Key("android.sensor.sensitivity", Integer.TYPE);
    @PublicKey
    public static final Key<int[]> SENSOR_TEST_PATTERN_DATA = new Key("android.sensor.testPatternData", int[].class);
    @PublicKey
    public static final Key<Integer> SENSOR_TEST_PATTERN_MODE = new Key("android.sensor.testPatternMode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> SHADING_MODE = new Key("android.shading.mode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> STATISTICS_FACE_DETECT_MODE = new Key("android.statistics.faceDetectMode", Integer.TYPE);
    @PublicKey
    public static final Key<Boolean> STATISTICS_HOT_PIXEL_MAP_MODE = new Key("android.statistics.hotPixelMapMode", Boolean.TYPE);
    @PublicKey
    public static final Key<Integer> STATISTICS_LENS_SHADING_MAP_MODE = new Key("android.statistics.lensShadingMapMode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> STATISTICS_OIS_DATA_MODE = new Key("android.statistics.oisDataMode", Integer.TYPE);
    @PublicKey
    @SyntheticKey
    public static final Key<TonemapCurve> TONEMAP_CURVE = new Key("android.tonemap.curve", TonemapCurve.class);
    public static final Key<float[]> TONEMAP_CURVE_BLUE = new Key("android.tonemap.curveBlue", float[].class);
    public static final Key<float[]> TONEMAP_CURVE_GREEN = new Key("android.tonemap.curveGreen", float[].class);
    public static final Key<float[]> TONEMAP_CURVE_RED = new Key("android.tonemap.curveRed", float[].class);
    @PublicKey
    public static final Key<Float> TONEMAP_GAMMA = new Key("android.tonemap.gamma", Float.TYPE);
    @PublicKey
    public static final Key<Integer> TONEMAP_MODE = new Key("android.tonemap.mode", Integer.TYPE);
    @PublicKey
    public static final Key<Integer> TONEMAP_PRESET_CURVE = new Key("android.tonemap.presetCurve", Integer.TYPE);
    private static final ArraySet<Surface> mEmptySurfaceSet = new ArraySet();
    private final String TAG;
    private boolean mIsPartOfCHSRequestList;
    private boolean mIsReprocess;
    private String mLogicalCameraId;
    private CameraMetadataNative mLogicalCameraSettings;
    private final HashMap<String, CameraMetadataNative> mPhysicalCameraSettings;
    private int mReprocessableSessionId;
    private int[] mStreamIdxArray;
    private boolean mSurfaceConverted;
    private int[] mSurfaceIdxArray;
    private final ArraySet<Surface> mSurfaceSet;
    private final Object mSurfacesLock;
    private Object mUserTag;

    public static final class Builder {
        private final CaptureRequest mRequest;

        public Builder(CameraMetadataNative template, boolean reprocess, int reprocessableSessionId, String logicalCameraId, Set<String> physicalCameraIdSet) {
            this.mRequest = new CaptureRequest(template, reprocess, reprocessableSessionId, logicalCameraId, physicalCameraIdSet, null);
        }

        public void addTarget(Surface outputTarget) {
            this.mRequest.mSurfaceSet.add(outputTarget);
        }

        public void removeTarget(Surface outputTarget) {
            this.mRequest.mSurfaceSet.remove(outputTarget);
        }

        public <T> void set(Key<T> key, T value) {
            this.mRequest.mLogicalCameraSettings.set((Key) key, (Object) value);
        }

        public <T> T get(Key<T> key) {
            return this.mRequest.mLogicalCameraSettings.get((Key) key);
        }

        public <T> Builder setPhysicalCameraKey(Key<T> key, T value, String physicalCameraId) {
            if (this.mRequest.mPhysicalCameraSettings.containsKey(physicalCameraId)) {
                ((CameraMetadataNative) this.mRequest.mPhysicalCameraSettings.get(physicalCameraId)).set((Key) key, (Object) value);
                return this;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Physical camera id: ");
            stringBuilder.append(physicalCameraId);
            stringBuilder.append(" is not valid!");
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public <T> T getPhysicalCameraKey(Key<T> key, String physicalCameraId) {
            if (this.mRequest.mPhysicalCameraSettings.containsKey(physicalCameraId)) {
                return ((CameraMetadataNative) this.mRequest.mPhysicalCameraSettings.get(physicalCameraId)).get((Key) key);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Physical camera id: ");
            stringBuilder.append(physicalCameraId);
            stringBuilder.append(" is not valid!");
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public void setTag(Object tag) {
            this.mRequest.mUserTag = tag;
        }

        public void setPartOfCHSRequestList(boolean partOfCHSList) {
            this.mRequest.mIsPartOfCHSRequestList = partOfCHSList;
        }

        public CaptureRequest build() {
            return new CaptureRequest(this.mRequest, null);
        }

        public boolean isEmpty() {
            return this.mRequest.mLogicalCameraSettings.isEmpty();
        }
    }

    public static final class Key<T> {
        private final android.hardware.camera2.impl.CameraMetadataNative.Key<T> mKey;

        public Key(String name, Class<T> type, long vendorId) {
            this.mKey = new android.hardware.camera2.impl.CameraMetadataNative.Key(name, (Class) type, vendorId);
        }

        public Key(String name, Class<T> type) {
            this.mKey = new android.hardware.camera2.impl.CameraMetadataNative.Key(name, (Class) type);
        }

        public Key(String name, TypeReference<T> typeReference) {
            this.mKey = new android.hardware.camera2.impl.CameraMetadataNative.Key(name, (TypeReference) typeReference);
        }

        public String getName() {
            return this.mKey.getName();
        }

        public long getVendorId() {
            return this.mKey.getVendorId();
        }

        public final int hashCode() {
            return this.mKey.hashCode();
        }

        public final boolean equals(Object o) {
            return (o instanceof Key) && ((Key) o).mKey.equals(this.mKey);
        }

        public String toString() {
            return String.format("CaptureRequest.Key(%s)", new Object[]{this.mKey.getName()});
        }

        public android.hardware.camera2.impl.CameraMetadataNative.Key<T> getNativeKey() {
            return this.mKey;
        }

        Key(android.hardware.camera2.impl.CameraMetadataNative.Key<?> nativeKey) {
            this.mKey = nativeKey;
        }
    }

    /* synthetic */ CaptureRequest(CaptureRequest x0, AnonymousClass1 x1) {
        this(x0);
    }

    /* synthetic */ CaptureRequest(CameraMetadataNative x0, boolean x1, int x2, String x3, Set x4, AnonymousClass1 x5) {
        this(x0, x1, x2, x3, x4);
    }

    private CaptureRequest() {
        this.TAG = "CaptureRequest-JV";
        this.mSurfaceSet = new ArraySet();
        this.mSurfacesLock = new Object();
        this.mSurfaceConverted = false;
        this.mPhysicalCameraSettings = new HashMap();
        this.mIsPartOfCHSRequestList = false;
        this.mIsReprocess = false;
        this.mReprocessableSessionId = -1;
    }

    private CaptureRequest(CaptureRequest source) {
        this.TAG = "CaptureRequest-JV";
        this.mSurfaceSet = new ArraySet();
        this.mSurfacesLock = new Object();
        this.mSurfaceConverted = false;
        this.mPhysicalCameraSettings = new HashMap();
        this.mIsPartOfCHSRequestList = false;
        this.mLogicalCameraId = new String(source.mLogicalCameraId);
        for (Entry<String, CameraMetadataNative> entry : source.mPhysicalCameraSettings.entrySet()) {
            this.mPhysicalCameraSettings.put(new String((String) entry.getKey()), new CameraMetadataNative((CameraMetadataNative) entry.getValue()));
        }
        this.mLogicalCameraSettings = (CameraMetadataNative) this.mPhysicalCameraSettings.get(this.mLogicalCameraId);
        setNativeInstance(this.mLogicalCameraSettings);
        this.mSurfaceSet.addAll(source.mSurfaceSet);
        this.mIsReprocess = source.mIsReprocess;
        this.mIsPartOfCHSRequestList = source.mIsPartOfCHSRequestList;
        this.mReprocessableSessionId = source.mReprocessableSessionId;
        this.mUserTag = source.mUserTag;
    }

    private CaptureRequest(CameraMetadataNative settings, boolean isReprocess, int reprocessableSessionId, String logicalCameraId, Set<String> physicalCameraIdSet) {
        this.TAG = "CaptureRequest-JV";
        this.mSurfaceSet = new ArraySet();
        this.mSurfacesLock = new Object();
        this.mSurfaceConverted = false;
        this.mPhysicalCameraSettings = new HashMap();
        this.mIsPartOfCHSRequestList = false;
        if (physicalCameraIdSet == null || !isReprocess) {
            this.mLogicalCameraId = logicalCameraId;
            this.mLogicalCameraSettings = CameraMetadataNative.move(settings);
            this.mPhysicalCameraSettings.put(this.mLogicalCameraId, this.mLogicalCameraSettings);
            if (physicalCameraIdSet != null) {
                for (String physicalId : physicalCameraIdSet) {
                    this.mPhysicalCameraSettings.put(physicalId, new CameraMetadataNative(this.mLogicalCameraSettings));
                }
            }
            setNativeInstance(this.mLogicalCameraSettings);
            this.mIsReprocess = isReprocess;
            if (!isReprocess) {
                this.mReprocessableSessionId = -1;
                return;
            } else if (reprocessableSessionId != -1) {
                this.mReprocessableSessionId = reprocessableSessionId;
                return;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Create a reprocess capture request with an invalid session ID: ");
                stringBuilder.append(reprocessableSessionId);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        throw new IllegalArgumentException("Create a reprocess capture request with with more than one physical camera is not supported!");
    }

    public <T> T get(Key<T> key) {
        return this.mLogicalCameraSettings.get((Key) key);
    }

    protected <T> T getProtected(Key<?> key) {
        return this.mLogicalCameraSettings.get((Key) key);
    }

    protected Class<Key<?>> getKeyClass() {
        return Key.class;
    }

    public List<Key<?>> getKeys() {
        return super.getKeys();
    }

    public Object getTag() {
        return this.mUserTag;
    }

    public boolean isReprocess() {
        return this.mIsReprocess;
    }

    public boolean isPartOfCRequestList() {
        return this.mIsPartOfCHSRequestList;
    }

    public CameraMetadataNative getNativeCopy() {
        return new CameraMetadataNative(this.mLogicalCameraSettings);
    }

    public int getReprocessableSessionId() {
        if (this.mIsReprocess && this.mReprocessableSessionId != -1) {
            return this.mReprocessableSessionId;
        }
        throw new IllegalStateException("Getting the reprocessable session ID for a non-reprocess capture request is illegal.");
    }

    public boolean equals(Object other) {
        return (other instanceof CaptureRequest) && equals((CaptureRequest) other);
    }

    private boolean equals(CaptureRequest other) {
        return other != null && Objects.equals(this.mUserTag, other.mUserTag) && this.mSurfaceSet.equals(other.mSurfaceSet) && this.mPhysicalCameraSettings.equals(other.mPhysicalCameraSettings) && this.mLogicalCameraId.equals(other.mLogicalCameraId) && this.mLogicalCameraSettings.equals(other.mLogicalCameraSettings) && this.mIsReprocess == other.mIsReprocess && this.mReprocessableSessionId == other.mReprocessableSessionId;
    }

    public int hashCode() {
        return HashCodeHelpers.hashCodeGeneric(this.mPhysicalCameraSettings, this.mSurfaceSet, this.mUserTag);
    }

    private void readFromParcel(Parcel in) {
        int physicalCameraCount = in.readInt();
        if (physicalCameraCount > 0) {
            this.mLogicalCameraId = in.readString();
            this.mLogicalCameraSettings = new CameraMetadataNative();
            this.mLogicalCameraSettings.readFromParcel(in);
            setNativeInstance(this.mLogicalCameraSettings);
            this.mPhysicalCameraSettings.put(this.mLogicalCameraId, this.mLogicalCameraSettings);
            boolean z = true;
            for (int i = 1; i < physicalCameraCount; i++) {
                String physicalId = in.readString();
                CameraMetadataNative physicalCameraSettings = new CameraMetadataNative();
                physicalCameraSettings.readFromParcel(in);
                this.mPhysicalCameraSettings.put(physicalId, physicalCameraSettings);
            }
            int i2 = 0;
            if (in.readInt() == 0) {
                z = false;
            }
            this.mIsReprocess = z;
            this.mReprocessableSessionId = -1;
            synchronized (this.mSurfacesLock) {
                this.mSurfaceSet.clear();
                Parcelable[] parcelableArray = in.readParcelableArray(Surface.class.getClassLoader());
                if (parcelableArray != null) {
                    int length = parcelableArray.length;
                    while (i2 < length) {
                        this.mSurfaceSet.add((Surface) parcelableArray[i2]);
                        i2++;
                    }
                }
                if (in.readInt() == 0) {
                } else {
                    throw new RuntimeException("Reading cached CaptureRequest is not supported");
                }
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Physical camera count");
        stringBuilder.append(physicalCameraCount);
        stringBuilder.append(" should always be positive");
        throw new RuntimeException(stringBuilder.toString());
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mPhysicalCameraSettings.size());
        dest.writeString(this.mLogicalCameraId);
        this.mLogicalCameraSettings.writeToParcel(dest, flags);
        for (Entry<String, CameraMetadataNative> entry : this.mPhysicalCameraSettings.entrySet()) {
            if (!((String) entry.getKey()).equals(this.mLogicalCameraId)) {
                dest.writeString((String) entry.getKey());
                ((CameraMetadataNative) entry.getValue()).writeToParcel(dest, flags);
            }
        }
        dest.writeInt(this.mIsReprocess);
        synchronized (this.mSurfacesLock) {
            ArraySet<Surface> surfaces = this.mSurfaceConverted ? mEmptySurfaceSet : this.mSurfaceSet;
            dest.writeParcelableArray((Surface[]) surfaces.toArray(new Surface[surfaces.size()]), flags);
            int i = 0;
            if (this.mSurfaceConverted) {
                dest.writeInt(this.mStreamIdxArray.length);
                while (true) {
                    int i2 = i;
                    if (i2 >= this.mStreamIdxArray.length) {
                        break;
                    }
                    dest.writeInt(this.mStreamIdxArray[i2]);
                    dest.writeInt(this.mSurfaceIdxArray[i2]);
                    i = i2 + 1;
                }
            } else {
                dest.writeInt(0);
            }
        }
    }

    public boolean containsTarget(Surface surface) {
        return this.mSurfaceSet.contains(surface);
    }

    public Collection<Surface> getTargets() {
        return Collections.unmodifiableCollection(this.mSurfaceSet);
    }

    public String getLogicalCameraId() {
        return this.mLogicalCameraId;
    }

    public void convertSurfaceToStreamId(SparseArray<OutputConfiguration> configuredOutputs) {
        SparseArray<OutputConfiguration> sparseArray = configuredOutputs;
        synchronized (this.mSurfacesLock) {
            if (this.mSurfaceConverted) {
                Log.v("CaptureRequest-JV", "Cannot convert already converted surfaces!");
                return;
            }
            this.mStreamIdxArray = new int[this.mSurfaceSet.size()];
            this.mSurfaceIdxArray = new int[this.mSurfaceSet.size()];
            int i = 0;
            Iterator it = this.mSurfaceSet.iterator();
            while (it.hasNext()) {
                Surface s = (Surface) it.next();
                int j = 0;
                boolean streamFound = false;
                int i2 = i;
                for (i = 0; i < configuredOutputs.size(); i++) {
                    int streamId = sparseArray.keyAt(i);
                    int surfaceId = 0;
                    for (Surface outSurface : ((OutputConfiguration) sparseArray.valueAt(i)).getSurfaces()) {
                        if (s == outSurface) {
                            streamFound = true;
                            this.mStreamIdxArray[i2] = streamId;
                            this.mSurfaceIdxArray[i2] = surfaceId;
                            i2++;
                            break;
                        }
                        surfaceId++;
                    }
                    if (streamFound) {
                        break;
                    }
                }
                if (!streamFound) {
                    long reqSurfaceId = SurfaceUtils.getSurfaceId(s);
                    while (true) {
                        i = j;
                        if (i >= configuredOutputs.size()) {
                            break;
                        }
                        j = sparseArray.keyAt(i);
                        int surfaceId2 = 0;
                        for (Surface outSurface2 : ((OutputConfiguration) sparseArray.valueAt(i)).getSurfaces()) {
                            if (reqSurfaceId == SurfaceUtils.getSurfaceId(outSurface2)) {
                                streamFound = true;
                                this.mStreamIdxArray[i2] = j;
                                this.mSurfaceIdxArray[i2] = surfaceId2;
                                i2++;
                                break;
                            }
                            surfaceId2++;
                        }
                        if (streamFound) {
                            break;
                        }
                        j = i + 1;
                    }
                }
                i = i2;
                if (!streamFound) {
                    this.mStreamIdxArray = null;
                    this.mSurfaceIdxArray = null;
                    throw new IllegalArgumentException("CaptureRequest contains unconfigured Input/Output Surface!");
                }
            }
            this.mSurfaceConverted = true;
        }
    }

    public void recoverStreamIdToSurface() {
        synchronized (this.mSurfacesLock) {
            if (this.mSurfaceConverted) {
                this.mStreamIdxArray = null;
                this.mSurfaceIdxArray = null;
                this.mSurfaceConverted = false;
                return;
            }
            Log.v("CaptureRequest-JV", "Cannot convert already converted surfaces!");
        }
    }
}
