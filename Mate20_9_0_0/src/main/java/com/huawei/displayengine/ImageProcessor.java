package com.huawei.displayengine;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import android.graphics.ColorSpace.Named;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.util.Size;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class ImageProcessor {
    private static final String CMD_CREATE_TILE_PROCESS_ENGINE = "createTileProcessEngine";
    private static final String CMD_DESTROY_TILE_PROCESS_ENGINE = "destroyTileProcessEngine";
    private static final String CMD_GET_SUPPORT_CMD = "getSupportCmd";
    private static final String CMD_GET_WIDE_COLOR_GAMUT_SUPPORTED = "getWideColorGamutSupported";
    private static final String CMD_PROCESS_THUMBNAIL = "processThumbnail";
    private static final String CMD_PROCESS_TILE = "processTile";
    private static final String CMD_TRANSFORM_COLORSPACE = "transformColorspace";
    private static final int COMMON_INFO_CACHE_MAX_SIZE = 14;
    private static final int DEFAULT_HARDWARE_SHARPNESS_LEVEL = 0;
    private static final String TAG = "DE J ImageProcessor";
    private static final int UNINIT_HARDWARE_SHARPNESS_LEVEL = -1;
    private static ImageProcessorAlgoStrategy mAlgoStrategy;
    private static final Set<String> mImageDescriptionBeauty = new HashSet<String>() {
        {
            add("fbt");
            add("fptbty");
            add("fairlight");
            add("btf");
            add("btfmdn");
            add("fbtmdn");
            add("btfhdr");
            add("fbthdr");
            add("rbt");
            add("rpt");
            add("ptr");
            add("btr");
            add("rbtozCO");
            add("btrozCO");
            add("rbtmdn");
            add("btrmdn");
            add("rbthdr");
            add("btrhdr");
        }
    };
    private static final Set<String> mImageDescriptionVivid = new HashSet<String>() {
        {
            add("vivid");
            add("HDRBFace");
            add("HDRB");
            add("BFace");
            add("Bright");
        }
    };
    private final ImageProcessorAlgoImpl mAlgo;
    private Map<String, CommonInfo> mCommonInfoCache;
    private String mCurrentFilePath;
    private final boolean mEnable;
    private int mHardwareSharpnessLevel = -1;
    private final IDisplayEngineServiceEx mService;
    private Map<Long, ImageEngine> mTileProcessEngineCache;

    public enum AlgoType {
        ACE(1),
        SR(2),
        SHARPNESS(4),
        GMP(8),
        ACM(16),
        LUT3D(32);
        
        private int mId;

        private AlgoType(int id) {
            this.mId = id;
        }

        public static int getType(Set<AlgoType> types) {
            if (types == null) {
                return 0;
            }
            int id = 0;
            for (AlgoType type : types) {
                id |= type.mId;
            }
            return id;
        }
    }

    private static class BitmapConfigTransformer {
        private final ColorspaceParam mColorspaceParam;
        private final Bitmap mInOriginalBitmap = this.mColorspaceParam.mInBitmap;
        private Bitmap mInTransBitmap;
        private final Bitmap mOutOriginalBitmap = this.mColorspaceParam.mOutBitmap;
        private Bitmap mOutTransBitmap;

        private BitmapConfigTransformer(ColorspaceParam param) {
            this.mColorspaceParam = param;
            String str;
            StringBuilder stringBuilder;
            if (this.mInOriginalBitmap == this.mOutOriginalBitmap) {
                str = ImageProcessor.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("BitmapConfigTransformer() in==out=");
                stringBuilder.append(this.mInOriginalBitmap.getConfig());
                DElog.i(str, stringBuilder.toString());
                return;
            }
            str = ImageProcessor.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("BitmapConfigTransformer() in=");
            stringBuilder.append(this.mInOriginalBitmap.getConfig());
            stringBuilder.append(", out=");
            stringBuilder.append(this.mOutOriginalBitmap.getConfig());
            DElog.i(str, stringBuilder.toString());
        }

        public static BitmapConfigTransformer create(ColorspaceParam param) {
            if (param.mInBitmap.getConfig() == Config.ARGB_8888 && param.mOutBitmap.getConfig() == Config.ARGB_8888) {
                return null;
            }
            return new BitmapConfigTransformer(param);
        }

        public void doPreTransform() {
            DElog.i(ImageProcessor.TAG, "BitmapConfigTransformer doPreTransform()");
            if (this.mInOriginalBitmap.getConfig() != Config.ARGB_8888) {
                this.mInTransBitmap = this.mInOriginalBitmap.copy(Config.ARGB_8888, true);
                if (this.mInTransBitmap != null) {
                    this.mColorspaceParam.mInBitmap = this.mInTransBitmap;
                    if (this.mInOriginalBitmap == this.mOutOriginalBitmap) {
                        this.mOutTransBitmap = this.mInTransBitmap;
                        this.mColorspaceParam.mOutBitmap = this.mOutTransBitmap;
                        DElog.i(ImageProcessor.TAG, "BitmapConfigTransformer doPreTransform() done");
                        return;
                    }
                }
                DElog.e(ImageProcessor.TAG, "BitmapConfigTransformer doPreTransform() error! can't copy in bitmap");
                throw new IllegalArgumentException("doPreTransform can't copy in bitmap");
            }
            if (this.mOutOriginalBitmap.getConfig() != Config.ARGB_8888) {
                ColorSpace colorSpace;
                ColorSpace colorSpace2 = this.mOutOriginalBitmap.getColorSpace();
                int width = this.mOutOriginalBitmap.getWidth();
                int height = this.mOutOriginalBitmap.getHeight();
                Config config = Config.ARGB_8888;
                if (colorSpace2 != null) {
                    colorSpace = colorSpace2;
                } else {
                    colorSpace = ColorSpace.get(Named.SRGB);
                }
                this.mOutTransBitmap = Bitmap.createBitmap(width, height, config, true, colorSpace);
                if (this.mOutTransBitmap != null) {
                    this.mColorspaceParam.mOutBitmap = this.mOutTransBitmap;
                } else {
                    DElog.e(ImageProcessor.TAG, "BitmapConfigTransformer doPreTransform() error! can't create out bitmap");
                    throw new IllegalArgumentException("doPreTransform can't create out bitmap");
                }
            }
            DElog.i(ImageProcessor.TAG, "BitmapConfigTransformer doPreTransform() done");
        }

        public void doPostTransform() {
            if (this.mOutTransBitmap != null) {
                DElog.i(ImageProcessor.TAG, "BitmapConfigTransformer doPostTransform()");
                new Canvas(this.mOutOriginalBitmap).drawBitmap(this.mOutTransBitmap, 0.0f, 0.0f, null);
                DElog.i(ImageProcessor.TAG, "BitmapConfigTransformer doPostTransform() done");
            }
        }
    }

    public static class ColorspaceParam {
        protected static final String PARAM_IN_BITMAP = "inBitmap";
        protected static final String PARAM_OUT_BITMAP = "outBitmap";
        public Bitmap mInBitmap;
        public ColorspaceType mInColorspace;
        public Bitmap mOutBitmap;
        public ColorspaceType mOutColorspace;

        public ColorspaceParam(Bitmap inBitmap, Bitmap outBitmap, ColorspaceType inColorspace, ColorspaceType outColorspace) {
            this.mInBitmap = inBitmap;
            this.mOutBitmap = outBitmap;
            this.mInColorspace = inColorspace;
            this.mOutColorspace = outColorspace;
        }

        public ColorspaceParam(Map<String, Object> param) {
            this.mInBitmap = (Bitmap) param.get(PARAM_IN_BITMAP);
            this.mOutBitmap = (Bitmap) param.get(PARAM_OUT_BITMAP);
            if (isParamInvalid()) {
                throw new IllegalArgumentException("ColorspaceParam input param invalid");
            }
            this.mInColorspace = ColorspaceType.getEnum(this.mInBitmap.getColorSpace());
            if (this.mInColorspace == null) {
                this.mInColorspace = ColorspaceType.SRGB;
                String str = ImageProcessor.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ColorspaceParam() error! unsupport inColorspace = ");
                stringBuilder.append(this.mInBitmap.getColorSpace());
                stringBuilder.append(", treat as SRGB");
                DElog.w(str, stringBuilder.toString());
            }
            this.mOutColorspace = ImageProcessor.mAlgoStrategy.needNormallizeColorSpace() ? ImageProcessor.mAlgoStrategy.getNormallizeColorGamut() : this.mInColorspace;
        }

        public ColorspaceParam(Map<String, Object> param, ColorspaceType inColorspace, ColorspaceType outColorspace) {
            this.mInBitmap = (Bitmap) param.get(PARAM_IN_BITMAP);
            this.mOutBitmap = (Bitmap) param.get(PARAM_OUT_BITMAP);
            this.mInColorspace = inColorspace;
            this.mOutColorspace = outColorspace;
            if (isParamInvalid()) {
                throw new IllegalArgumentException("ColorspaceParam input param invalid");
            }
        }

        private boolean isParamInvalid() {
            if (this.mInBitmap == null) {
                DElog.e(ImageProcessor.TAG, "isParamInvalid() error! mInBitmap is null");
                return true;
            }
            Config bitmapConfig = this.mInBitmap.getConfig();
            String str;
            StringBuilder stringBuilder;
            if (bitmapConfig != Config.ARGB_8888 && bitmapConfig != Config.RGB_565) {
                str = ImageProcessor.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("isParamInvalid() error! unsupported mInBitmap format ");
                stringBuilder.append(bitmapConfig);
                DElog.e(str, stringBuilder.toString());
                return true;
            } else if (this.mOutBitmap == null) {
                DElog.e(ImageProcessor.TAG, "isParamInvalid() error! mOutBitmap is null");
                return true;
            } else {
                bitmapConfig = this.mOutBitmap.getConfig();
                if (bitmapConfig == Config.ARGB_8888 || bitmapConfig == Config.RGB_565) {
                    return false;
                }
                str = ImageProcessor.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("isParamInvalid() error! unsupported mOutBitmap format ");
                stringBuilder.append(bitmapConfig);
                DElog.e(str, stringBuilder.toString());
                return true;
            }
        }
    }

    public enum ColorspaceType {
        SRGB(ColorSpace.get(Named.SRGB)),
        ADOBE_RGB(ColorSpace.get(Named.ADOBE_RGB)),
        DISPLAY_P3(ColorSpace.get(Named.DISPLAY_P3)),
        SUPER_GAMUT(null);
        
        private static final Map<Integer, ColorspaceType> mColorSpaceIdToEnum = null;
        private ColorSpace mColorSpace;

        static {
            mColorSpaceIdToEnum = new HashMap();
            ColorspaceType[] values = values();
            int length = values.length;
            int i;
            while (i < length) {
                ColorspaceType type = values[i];
                if (type.mColorSpace != null) {
                    mColorSpaceIdToEnum.put(Integer.valueOf(type.mColorSpace.getId()), type);
                }
                i++;
            }
        }

        private ColorspaceType(ColorSpace colorSpace) {
            this.mColorSpace = colorSpace;
        }

        public static ColorspaceType getEnum(ColorSpace colorSpace) {
            if (colorSpace == null) {
                return null;
            }
            return (ColorspaceType) mColorSpaceIdToEnum.get(Integer.valueOf(colorSpace.getId()));
        }
    }

    public static class CommonInfo {
        private ImageProcessorAlgoImpl mAlgo;
        public long mCommonHandle;

        public CommonInfo(long handle, ImageProcessorAlgoImpl algo) {
            this.mCommonHandle = handle;
            this.mAlgo = algo;
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mCommonHandle != 0) {
                    String str = ImageProcessor.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("CommonInfo finalize() mCommonHandle=");
                    stringBuilder.append(this.mCommonHandle);
                    DElog.i(str, stringBuilder.toString());
                    if (this.mAlgo != null) {
                        this.mAlgo.destroyCommonInfo(this);
                    }
                }
                super.finalize();
            } catch (Throwable th) {
                super.finalize();
            }
        }
    }

    public static class CreateTileProcessEngineParam {
        private static final String PARAM_ENGINE_TYPE = "engineType";
        private static final String PARAM_TILE_BORDER = "tileBorder";
        private static final String PARAM_TILE_SIZE = "tileSize";
        public Set<AlgoType> mAlgos = ImageProcessor.mAlgoStrategy.getTileEngineAlgos(this.mEngineType);
        public TileEngineType mEngineType;
        public int mTileBorder;
        public int mTileSize;

        public CreateTileProcessEngineParam(Map<String, Object> param) {
            this.mEngineType = TileEngineType.valueOf((String) param.get(PARAM_ENGINE_TYPE));
            this.mTileSize = ((Integer) param.get(PARAM_TILE_SIZE)).intValue();
            this.mTileBorder = ((Integer) param.get(PARAM_TILE_BORDER)).intValue();
        }
    }

    private static class DestroyTileProcessEngineParam {
        private static final String PARAM_ENGINE = "engine";
        public long mAlgoHandle;

        public DestroyTileProcessEngineParam(Map<String, Object> param) {
            this.mAlgoHandle = ((Long) param.get(PARAM_ENGINE)).longValue();
        }
    }

    public static class ImageEngine {
        public long mAlgoHandle;

        public ImageEngine(long handle) {
            this.mAlgoHandle = handle;
        }

        protected void finalize() throws Throwable {
            if (this.mAlgoHandle != 0) {
                String str = ImageProcessor.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ImageEngine finalize() error! haven't destroyed yet, mAlgoHandle=");
                stringBuilder.append(this.mAlgoHandle);
                DElog.e(str, stringBuilder.toString());
            }
            super.finalize();
        }
    }

    public enum ImageType {
        NORMAL,
        WIDE_COLOR_SPACE,
        SKIN_BEAUTY,
        VIVID
    }

    private static class InstanceHolder {
        public static ImageProcessorAlgoStrategy mInstance = new ImageProcessorAlgoStrategy();

        private InstanceHolder() {
        }
    }

    public enum ThumbnailType {
        DEFAULT,
        MICRO,
        FAST,
        ANIMATION,
        FULLSCREEN,
        HALFSCREEN,
        GIF
    }

    public enum TileEngineType {
        SR,
        NON_SR
    }

    public static class ThumbnailParam extends ColorspaceParam {
        private static final String PARAM_FILE_PATH = "filePath";
        private static final String PARAM_IMAGE_DESCRIPTION = "imageDescription";
        private static final String PARAM_ISO = "iso";
        private static final String PARAM_SCALE_RATIO = "scaleRatio";
        private static final String PARAM_SKIN_BEAUTY = "skinBeauty";
        private static final String PARAM_THUMBNAIL_TYPE = "thumbnailType";
        public Set<AlgoType> mAlgos;
        public Set<AlgoType> mCommonAlgos;
        public String mFilePath;
        public int mISO;
        public String mImageDescription;
        public ImageType mImageType = ImageType.NORMAL;
        public float mScaleRatio = 1.0f;
        public boolean mSkinBeauty;
        public ThumbnailType mType = ThumbnailType.DEFAULT;

        public ThumbnailParam(Map<String, Object> param) {
            super(param);
            this.mFilePath = (String) param.get(PARAM_FILE_PATH);
            if (param.containsKey(PARAM_THUMBNAIL_TYPE)) {
                this.mType = ThumbnailType.valueOf((String) param.get(PARAM_THUMBNAIL_TYPE));
            }
            if (param.containsKey(PARAM_SCALE_RATIO)) {
                this.mScaleRatio = ((Float) param.get(PARAM_SCALE_RATIO)).floatValue();
            }
            if (param.containsKey(PARAM_ISO)) {
                this.mISO = ((Integer) param.get(PARAM_ISO)).intValue();
            }
            if (param.containsKey(PARAM_SKIN_BEAUTY)) {
                this.mSkinBeauty = ((Boolean) param.get(PARAM_SKIN_BEAUTY)).booleanValue();
            }
            boolean isVivid = false;
            if (param.containsKey(PARAM_IMAGE_DESCRIPTION)) {
                this.mImageDescription = (String) param.get(PARAM_IMAGE_DESCRIPTION);
                this.mSkinBeauty = ImageProcessor.isImageDescriptionBeauty(this.mImageDescription);
                isVivid = ImageProcessor.isImageDescriptionVivid(this.mImageDescription);
            }
            if (this.mSkinBeauty && ImageProcessor.mAlgoStrategy.isAlgoSkinBeautyEnable()) {
                this.mImageType = ImageType.SKIN_BEAUTY;
            } else if (isVivid && ImageProcessor.mAlgoStrategy.isAlgoVividEnable()) {
                this.mImageType = ImageType.VIVID;
            } else if (this.mInColorspace != ColorspaceType.SRGB && ImageProcessor.mAlgoStrategy.isAlgoWideColorSpaceEnable()) {
                this.mImageType = ImageType.WIDE_COLOR_SPACE;
            }
            this.mAlgos = ImageProcessor.mAlgoStrategy.getThumbnailAlgos(this.mType, this.mImageType);
            this.mCommonAlgos = ImageProcessor.mAlgoStrategy.getCommonInfoAlgos(this.mType, this.mImageType);
            if (isParamInvalid()) {
                throw new IllegalArgumentException("processThumbnail input param invalid");
            }
        }

        private boolean isParamInvalid() {
            String str;
            StringBuilder stringBuilder;
            if (this.mFilePath.isEmpty()) {
                DElog.e(ImageProcessor.TAG, "isParamInvalid() error! mFilePath is empty");
                return true;
            } else if (this.mType == null) {
                DElog.e(ImageProcessor.TAG, "isParamInvalid() error! mType is empty");
                return true;
            } else if (this.mScaleRatio <= 0.0f || this.mScaleRatio > 2.0f) {
                str = ImageProcessor.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("isParamInvalid() error! mScaleRatio=");
                stringBuilder.append(this.mScaleRatio);
                stringBuilder.append(" out of range");
                DElog.e(str, stringBuilder.toString());
                return true;
            } else if (this.mISO >= 0 && this.mISO <= 102400) {
                return false;
            } else {
                str = ImageProcessor.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("isParamInvalid() error! mISO=");
                stringBuilder.append(this.mISO);
                stringBuilder.append(" out of range");
                DElog.e(str, stringBuilder.toString());
                return true;
            }
        }
    }

    public static class TileParam extends ColorspaceParam {
        private static final String PARAM_DECODED_SIZE = "decodedSize";
        private static final String PARAM_DECODED_START_POINT = "decodedStartPoint";
        private static final String PARAM_ENGINE = "engine";
        private static final String PARAM_ENGINE_TYPE = "engineType";
        private static final String PARAM_FILE_PATH = "filePath";
        private static final String PARAM_IMAGE_DESCRIPTION = "imageDescription";
        private static final String PARAM_IN_VISIBLE_RANGE = "inVisibleRange";
        private static final String PARAM_SCALED_START_POINT = "scaledStartPoint";
        private static final String PARAM_SCALE_RATIO = "scaleRatio";
        private static final String PARAM_SKIN_BEAUTY = "skinBeauty";
        private static final String PARAM_ZOOM_IN_RATIO = "zoomInRatio";
        public long mAlgoHandle;
        public Size mDecodedSize;
        public Point mDecodedStartPoint;
        public TileEngineType mEngineType;
        public String mFilePath;
        public String mImageDescription;
        public ImageType mImageType = ImageType.NORMAL;
        public Rect mInVisibleRange;
        public float mScaleRatio = 1.0f;
        public PointF mScaledStartPoint;
        public float mZoomInRatio = 1.0f;

        public TileParam(Map<String, Object> param) {
            super(param);
            this.mEngineType = TileEngineType.valueOf((String) param.get(PARAM_ENGINE_TYPE));
            this.mAlgoHandle = ((Long) param.get(PARAM_ENGINE)).longValue();
            this.mFilePath = (String) param.get(PARAM_FILE_PATH);
            if (this.mEngineType == TileEngineType.SR) {
                this.mScaleRatio = ((Float) param.get(PARAM_SCALE_RATIO)).floatValue();
                this.mZoomInRatio = ((Float) param.get(PARAM_ZOOM_IN_RATIO)).floatValue();
                this.mScaledStartPoint = (PointF) param.get(PARAM_SCALED_START_POINT);
                if (param.containsKey(PARAM_IN_VISIBLE_RANGE)) {
                    this.mInVisibleRange = (Rect) param.get(PARAM_IN_VISIBLE_RANGE);
                } else {
                    this.mInVisibleRange = new Rect(0, 0, this.mInBitmap.getWidth() - 1, this.mInBitmap.getHeight() - 1);
                }
            } else {
                this.mDecodedSize = (Size) param.get(PARAM_DECODED_SIZE);
                this.mDecodedStartPoint = (Point) param.get(PARAM_DECODED_START_POINT);
            }
            boolean isSkinBeauty = false;
            if (param.containsKey(PARAM_SKIN_BEAUTY)) {
                isSkinBeauty = ((Boolean) param.get(PARAM_SKIN_BEAUTY)).booleanValue();
            }
            boolean isVivid = false;
            if (param.containsKey(PARAM_IMAGE_DESCRIPTION)) {
                this.mImageDescription = (String) param.get(PARAM_IMAGE_DESCRIPTION);
                isSkinBeauty = ImageProcessor.isImageDescriptionBeauty(this.mImageDescription);
                isVivid = ImageProcessor.isImageDescriptionVivid(this.mImageDescription);
            }
            if (isSkinBeauty && ImageProcessor.mAlgoStrategy.isAlgoSkinBeautyEnable()) {
                this.mImageType = ImageType.SKIN_BEAUTY;
            } else if (isVivid && ImageProcessor.mAlgoStrategy.isAlgoVividEnable()) {
                this.mImageType = ImageType.VIVID;
            } else if (this.mInColorspace != ColorspaceType.SRGB && ImageProcessor.mAlgoStrategy.isAlgoWideColorSpaceEnable()) {
                this.mImageType = ImageType.WIDE_COLOR_SPACE;
            }
            if (isParamInvalid()) {
                throw new IllegalArgumentException("processTile input param invalid");
            }
        }

        private boolean isParamInvalid() {
            if (this.mEngineType == null) {
                DElog.e(ImageProcessor.TAG, "isParamInvalid() error! mEngineType is null");
                return true;
            } else if (this.mFilePath.isEmpty()) {
                DElog.e(ImageProcessor.TAG, "isParamInvalid() error! mFilePath is empty");
                return true;
            } else {
                String str;
                StringBuilder stringBuilder;
                if (this.mEngineType == TileEngineType.SR) {
                    if (this.mScaleRatio <= 0.0f || this.mScaleRatio > 2.0f) {
                        str = ImageProcessor.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("isParamInvalid() error! mScaleRatio=");
                        stringBuilder.append(this.mScaleRatio);
                        stringBuilder.append(" out of range");
                        DElog.e(str, stringBuilder.toString());
                        return true;
                    } else if (this.mZoomInRatio <= 0.0f) {
                        str = ImageProcessor.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("isParamInvalid() error! mZoomInRatio=");
                        stringBuilder.append(this.mZoomInRatio);
                        stringBuilder.append(" out of range");
                        DElog.e(str, stringBuilder.toString());
                        return true;
                    } else if (this.mScaledStartPoint == null) {
                        DElog.e(ImageProcessor.TAG, "isParamInvalid() error! mScaledStartPoint is null");
                        return true;
                    } else if (isVisibleRangeInvalid(this.mInVisibleRange)) {
                        return true;
                    }
                } else if (this.mDecodedSize == null) {
                    DElog.e(ImageProcessor.TAG, "isParamInvalid() error! mDecodedSize is null");
                    return true;
                } else if (this.mDecodedSize.getWidth() <= 0 || this.mDecodedSize.getHeight() <= 0) {
                    str = ImageProcessor.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("isParamInvalid() error! mDecodedSize=");
                    stringBuilder.append(this.mDecodedSize);
                    DElog.e(str, stringBuilder.toString());
                    return true;
                } else if (this.mDecodedStartPoint == null) {
                    DElog.e(ImageProcessor.TAG, "isParamInvalid() error! mDecodedStartPoint is null");
                    return true;
                }
                return false;
            }
        }

        private boolean isVisibleRangeInvalid(Rect rect) {
            if (rect == null) {
                DElog.e(ImageProcessor.TAG, "isVisibleRangeInvalid() error! rect is null");
                return true;
            } else if (rect.left >= 0 && rect.top >= 0 && rect.left < rect.right && rect.top < rect.bottom && rect.right < this.mInBitmap.getWidth() && rect.bottom < this.mInBitmap.getHeight()) {
                return false;
            } else {
                String str = ImageProcessor.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isVisibleRangeInvalid() error! rect=");
                stringBuilder.append(rect);
                DElog.e(str, stringBuilder.toString());
                return true;
            }
        }
    }

    public static boolean isCommandOwner(String command) {
        if (command == null) {
            return false;
        }
        boolean z = true;
        switch (command.hashCode()) {
            case -1791436785:
                if (command.equals(CMD_TRANSFORM_COLORSPACE)) {
                    z = true;
                    break;
                }
                break;
            case -911672607:
                if (command.equals(CMD_GET_SUPPORT_CMD)) {
                    z = false;
                    break;
                }
                break;
            case 202511805:
                if (command.equals(CMD_PROCESS_TILE)) {
                    z = true;
                    break;
                }
                break;
            case 802499670:
                if (command.equals(CMD_GET_WIDE_COLOR_GAMUT_SUPPORTED)) {
                    z = true;
                    break;
                }
                break;
            case 1324162877:
                if (command.equals(CMD_PROCESS_THUMBNAIL)) {
                    z = true;
                    break;
                }
                break;
            case 2079953895:
                if (command.equals(CMD_CREATE_TILE_PROCESS_ENGINE)) {
                    z = true;
                    break;
                }
                break;
            case 2101719209:
                if (command.equals(CMD_DESTROY_TILE_PROCESS_ENGINE)) {
                    z = true;
                    break;
                }
                break;
        }
        switch (z) {
            case false:
            case true:
            case true:
            case true:
            case true:
            case true:
            case true:
                return true;
            default:
                return false;
        }
    }

    public static boolean isSceneSensitive(int scene, int action) {
        if (scene == 3 && (action == 9 || action == 10 || action == 13)) {
            return true;
        }
        return false;
    }

    public ImageProcessor(IDisplayEngineServiceEx service) {
        DElog.i(TAG, "ImageProcessor enter");
        mAlgoStrategy = InstanceHolder.mInstance;
        this.mService = service;
        this.mAlgo = new ImageProcessorAlgoImpl(service);
        boolean z = mAlgoStrategy.isImageProcessorEnable() && this.mAlgo.isAlgoInitSuccess();
        this.mEnable = z;
        this.mCommonInfoCache = Collections.synchronizedMap(new LinkedHashMap<String, CommonInfo>(16, 0.75f, true) {
            protected boolean removeEldestEntry(Entry<String, CommonInfo> entry) {
                if (size() > 14) {
                    return true;
                }
                return false;
            }
        });
        this.mTileProcessEngineCache = Collections.synchronizedMap(new HashMap());
        DElog.i(TAG, "ImageProcessor exit");
    }

    protected void finalize() throws Throwable {
        DElog.i(TAG, "finalize");
        try {
            clearCommonInfo();
            clearImageEngine();
        } finally {
            super.finalize();
        }
    }

    public Object imageProcess(String command, Map<String, Object> param) {
        if (command == null) {
            return null;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("imageProcess() command=");
        stringBuilder.append(command);
        DElog.d(str, stringBuilder.toString());
        if (command.equals(CMD_GET_SUPPORT_CMD)) {
            return getSupportCmd();
        }
        if (this.mEnable) {
            Object obj = -1;
            switch (command.hashCode()) {
                case -1791436785:
                    if (command.equals(CMD_TRANSFORM_COLORSPACE)) {
                        obj = 5;
                        break;
                    }
                    break;
                case 202511805:
                    if (command.equals(CMD_PROCESS_TILE)) {
                        obj = 3;
                        break;
                    }
                    break;
                case 802499670:
                    if (command.equals(CMD_GET_WIDE_COLOR_GAMUT_SUPPORTED)) {
                        obj = null;
                        break;
                    }
                    break;
                case 1324162877:
                    if (command.equals(CMD_PROCESS_THUMBNAIL)) {
                        obj = 1;
                        break;
                    }
                    break;
                case 2079953895:
                    if (command.equals(CMD_CREATE_TILE_PROCESS_ENGINE)) {
                        obj = 2;
                        break;
                    }
                    break;
                case 2101719209:
                    if (command.equals(CMD_DESTROY_TILE_PROCESS_ENGINE)) {
                        obj = 4;
                        break;
                    }
                    break;
            }
            switch (obj) {
                case null:
                    return getWideColorGamutSupported();
                case 1:
                    processThumbnail((Map) param);
                    break;
                case 2:
                    return createTileProcessEngine(param);
                case 3:
                    processTile((Map) param);
                    break;
                case 4:
                    destroyTileProcessEngine(param);
                    break;
                case 5:
                    transformColorspaceToSRGB(param);
                    break;
                default:
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("imageProcess() error! undefine command=");
                    stringBuilder.append(command);
                    DElog.e(str, stringBuilder.toString());
                    break;
            }
            return null;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("imageProcess() is disable, command=");
        stringBuilder.append(command);
        DElog.e(str, stringBuilder.toString());
        return null;
    }

    private List<String> getSupportCmd() {
        List<String> supportedCmd = new ArrayList();
        if (this.mEnable) {
            supportedCmd.add(CMD_GET_SUPPORT_CMD);
            supportedCmd.add(CMD_GET_WIDE_COLOR_GAMUT_SUPPORTED);
            supportedCmd.add(CMD_PROCESS_THUMBNAIL);
            supportedCmd.add(CMD_CREATE_TILE_PROCESS_ENGINE);
            supportedCmd.add(CMD_PROCESS_TILE);
            supportedCmd.add(CMD_DESTROY_TILE_PROCESS_ENGINE);
            if (mAlgoStrategy.needNormallizeColorSpace()) {
                supportedCmd.add(CMD_TRANSFORM_COLORSPACE);
            }
        } else {
            supportedCmd.add(CMD_GET_SUPPORT_CMD);
        }
        return supportedCmd;
    }

    private Boolean getWideColorGamutSupported() {
        boolean z = this.mEnable && (mAlgoStrategy.needNormallizeColorSpace() || mAlgoStrategy.isAlgoWideColorSpaceEnable());
        return Boolean.valueOf(z);
    }

    private void transformColorspaceToSRGB(Map<String, Object> param) {
        DElog.i(TAG, "transformColorspaceToSRGB() begin");
        if (!mAlgoStrategy.isImageProcessorEnable()) {
            throw new UnsupportedOperationException("image process is disabled");
        } else if (mAlgoStrategy.needNormallizeColorSpace()) {
            ColorspaceParam colorspaceParam = new ColorspaceParam(param, ColorspaceType.SUPER_GAMUT, ColorspaceType.SRGB);
            if (isGMPInImageMode()) {
                BitmapConfigTransformer transformer = BitmapConfigTransformer.create(colorspaceParam);
                if (transformer != null) {
                    transformer.doPreTransform();
                }
                this.mAlgo.transformColorspace(colorspaceParam);
                if (transformer != null) {
                    transformer.doPostTransform();
                }
                DElog.i(TAG, "transformColorspaceToSRGB() trans end");
                return;
            }
            if (colorspaceParam.mInBitmap != colorspaceParam.mOutBitmap) {
                copyPixels(colorspaceParam.mInBitmap, colorspaceParam.mOutBitmap);
                DElog.i(TAG, "transformColorspaceToSRGB() copy end");
            } else {
                DElog.i(TAG, "transformColorspaceToSRGB() bypass end");
            }
        } else {
            throw new UnsupportedOperationException("transform colorspace is disabled");
        }
    }

    private boolean isGMPInImageMode() {
        if (this.mService == null) {
            DElog.e(TAG, "isGMPInImageMode() mService is null!");
            return false;
        }
        boolean z = true;
        byte[] isImage = new byte[1];
        int ret = 0;
        try {
            ret = this.mService.getEffect(3, 4, isImage, isImage.length);
            if (ret != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isGMPInImageMode() getEffect failed, return ");
                stringBuilder.append(ret);
                DElog.e(str, stringBuilder.toString());
                return false;
            }
            if (isImage[0] == (byte) 0) {
                z = false;
            }
            return z;
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isGMPInImageMode() RemoteException ");
            stringBuilder2.append(e);
            DElog.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    private void transformColorspaceOnBitmap(ColorspaceParam colorspaceParam, Bitmap inBitmap, Bitmap outBitmap) {
        if (mAlgoStrategy.needNormallizeColorSpace() && colorspaceParam.mInColorspace != colorspaceParam.mOutColorspace) {
            this.mAlgo.transformColorspace(new ColorspaceParam(inBitmap, outBitmap, colorspaceParam.mInColorspace, colorspaceParam.mOutColorspace));
        }
    }

    private static boolean isImageDescriptionBeauty(String imageDescription) {
        if (imageDescription == null || imageDescription.isEmpty()) {
            return false;
        }
        return mImageDescriptionBeauty.contains(imageDescription.split("_", 2)[0]);
    }

    private static boolean isImageDescriptionVivid(String imageDescription) {
        if (imageDescription == null || imageDescription.isEmpty()) {
            return false;
        }
        String[] split = imageDescription.split("_", 3);
        if (split.length < 2) {
            return false;
        }
        return mImageDescriptionVivid.contains(split[1]);
    }

    private void processThumbnail(Map<String, Object> param) {
        DElog.d(TAG, "processThumbnail()");
        if (mAlgoStrategy.isImageProcessorEnable()) {
            ThumbnailParam thumbnailParam = new ThumbnailParam(param);
            BitmapConfigTransformer transformer = BitmapConfigTransformer.create(thumbnailParam);
            if (transformer != null) {
                transformer.doPreTransform();
            }
            processThumbnail(thumbnailParam);
            if (transformer != null) {
                transformer.doPostTransform();
                return;
            }
            return;
        }
        throw new UnsupportedOperationException("image process is disabled");
    }

    private static void copyPixels(Bitmap in, Bitmap out) {
        if (in != null && out != null && in != out) {
            ByteBuffer buffer = ByteBuffer.allocate(in.getByteCount());
            in.copyPixelsToBuffer(buffer);
            buffer.rewind();
            out.copyPixelsFromBuffer(buffer);
            DElog.i(TAG, "copyPixels() done");
        }
    }

    private void copyPixelsToOutBitmapIfNeeded(ColorspaceParam thumbnailParam) {
        if (thumbnailParam.mInBitmap != thumbnailParam.mOutBitmap) {
            if (!mAlgoStrategy.needNormallizeColorSpace() || thumbnailParam.mInColorspace == thumbnailParam.mOutColorspace) {
                copyPixels(thumbnailParam.mInBitmap, thumbnailParam.mOutBitmap);
            }
        }
    }

    private void processThumbnail(ThumbnailParam thumbnailParam) {
        boolean processDone = false;
        CommonInfo commonInfo = null;
        if (mAlgoStrategy.needRunSoftwareAlgo(thumbnailParam.mType, thumbnailParam.mImageType)) {
            commonInfo = getCommonInfo(thumbnailParam.mFilePath);
            if (commonInfo == null) {
                try {
                    commonInfo = new CommonInfo(this.mAlgo.createCommonInfo(thumbnailParam), this.mAlgo);
                    if (mAlgoStrategy.needSaveCommonInfo(thumbnailParam.mType)) {
                        saveCommonInfo(thumbnailParam.mFilePath, commonInfo);
                    }
                } catch (RuntimeException e) {
                    transformColorspaceOnBitmap(thumbnailParam, thumbnailParam.mInBitmap, thumbnailParam.mInBitmap);
                    collectInfoForImageRecognization(thumbnailParam, commonInfo);
                    throw e;
                }
            }
            if (thumbnailParam.mAlgos != null) {
                ImageEngine imageEngine = null;
                try {
                    imageEngine = this.mAlgo.createImageEngine(thumbnailParam);
                    this.mAlgo.processThumbnail(imageEngine, commonInfo, thumbnailParam);
                    processDone = true;
                    this.mAlgo.destroyImageEngine(imageEngine);
                    if (null != null) {
                        this.mAlgo.destroyImageEngine(null);
                    }
                } catch (RuntimeException e2) {
                    transformColorspaceOnBitmap(thumbnailParam, thumbnailParam.mInBitmap, thumbnailParam.mInBitmap);
                    collectInfoForImageRecognization(thumbnailParam, commonInfo);
                    throw e2;
                } catch (Throwable th) {
                    if (imageEngine != null) {
                        this.mAlgo.destroyImageEngine(imageEngine);
                    }
                }
            }
        }
        transformColorspaceOnBitmap(thumbnailParam, processDone ? thumbnailParam.mOutBitmap : thumbnailParam.mInBitmap, thumbnailParam.mOutBitmap);
        if (!processDone) {
            copyPixelsToOutBitmapIfNeeded(thumbnailParam);
        }
        collectInfoForImageRecognization(thumbnailParam, commonInfo);
    }

    private void collectInfoForImageRecognization(String filePath, String imageDescription, CommonInfo commonInfo) {
        int hardwareSharpnessLevel = 0;
        if (this.mCurrentFilePath == null || !this.mCurrentFilePath.equals(filePath)) {
            this.mCurrentFilePath = filePath;
            this.mHardwareSharpnessLevel = -1;
            if (commonInfo != null) {
                hardwareSharpnessLevel = this.mAlgo.getHardwareSharpnessLevel(commonInfo);
                this.mHardwareSharpnessLevel = hardwareSharpnessLevel;
            }
            sendInfoToImageRecognization(this.mCurrentFilePath, imageDescription, hardwareSharpnessLevel);
        } else if (this.mHardwareSharpnessLevel == -1 && commonInfo != null) {
            hardwareSharpnessLevel = this.mAlgo.getHardwareSharpnessLevel(commonInfo);
            this.mHardwareSharpnessLevel = hardwareSharpnessLevel;
            if (hardwareSharpnessLevel != 0) {
                sendInfoToImageRecognization(this.mCurrentFilePath, imageDescription, hardwareSharpnessLevel);
            }
        }
    }

    private void collectInfoForImageRecognization(ThumbnailParam thumbnailParam, CommonInfo commonInfo) {
        if (thumbnailParam.mType == ThumbnailType.FAST || thumbnailParam.mType == ThumbnailType.ANIMATION || thumbnailParam.mType == ThumbnailType.FULLSCREEN) {
            collectInfoForImageRecognization(thumbnailParam.mFilePath, thumbnailParam.mImageDescription, commonInfo);
        }
    }

    private void collectInfoForImageRecognization(TileParam tileParam, CommonInfo commonInfo) {
        collectInfoForImageRecognization(tileParam.mFilePath, tileParam.mImageDescription, commonInfo);
    }

    private void sendInfoToImageRecognization(String filePath, String imageDescription, int hardwareSharpnessLevel) {
        if (this.mService == null) {
            DElog.e(TAG, "sendInfoToImageRecognization() mService is null!");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendInfoToImageRecognization filePath=");
        stringBuilder.append(filePath);
        stringBuilder.append(", imageDescription=");
        stringBuilder.append(imageDescription);
        stringBuilder.append(", hardwareSharpnessLevel=");
        stringBuilder.append(hardwareSharpnessLevel);
        DElog.i(str, stringBuilder.toString());
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString("filePath", filePath);
        if (imageDescription != null) {
            bundle.putString("imageDescription", imageDescription);
        }
        bundle.putInt("hardwareSharpnessLevel", hardwareSharpnessLevel);
        try {
            this.mService.setData(4, bundle);
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sendInfoToImageRecognization setData error! filePath=");
            stringBuilder2.append(filePath);
            stringBuilder2.append(", hardwareSharpnessLevel=");
            stringBuilder2.append(hardwareSharpnessLevel);
            stringBuilder2.append(", ");
            stringBuilder2.append(e.getMessage());
            DElog.e(str2, stringBuilder2.toString());
        }
    }

    private void saveCommonInfo(String hashID, CommonInfo commonInfo) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CommonInfoCache save ");
        stringBuilder.append(hashID);
        stringBuilder.append(", commonHandle=");
        stringBuilder.append(commonInfo.mCommonHandle);
        DElog.d(str, stringBuilder.toString());
        this.mCommonInfoCache.put(hashID, commonInfo);
    }

    private void clearCommonInfo() {
        if (!this.mCommonInfoCache.isEmpty()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CommonInfoCache clear size=");
            stringBuilder.append(this.mCommonInfoCache.size());
            DElog.i(str, stringBuilder.toString());
            this.mCommonInfoCache.clear();
        }
    }

    private CommonInfo getCommonInfo(String hashID) {
        CommonInfo commonInfo = (CommonInfo) this.mCommonInfoCache.get(hashID);
        if (commonInfo == null || commonInfo.mCommonHandle != 0) {
            return commonInfo;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCommonInfo() error! hashID ");
        stringBuilder.append(hashID);
        stringBuilder.append(" commonHandle is 0");
        DElog.e(TAG, stringBuilder.toString());
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getCommonInfo() error! hashID ");
        stringBuilder2.append(hashID);
        stringBuilder2.append(" commonHandle is 0");
        throw new IllegalStateException(stringBuilder2.toString());
    }

    private Long createTileProcessEngine(Map<String, Object> param) {
        DElog.i(TAG, "createTileProcessEngine()");
        if (mAlgoStrategy.isImageProcessorEnable()) {
            CreateTileProcessEngineParam createTileProcessEngineParam = new CreateTileProcessEngineParam(param);
            if (createTileProcessEngineParam.mAlgos == null) {
                return Long.valueOf(0);
            }
            ImageEngine imageEngine = this.mAlgo.createImageEngine(createTileProcessEngineParam);
            saveImageEngine(imageEngine);
            return Long.valueOf(imageEngine.mAlgoHandle);
        }
        throw new UnsupportedOperationException("image process is disabled");
    }

    private void processTile(Map<String, Object> param) {
        DElog.d(TAG, "processTile()");
        if (mAlgoStrategy.isImageProcessorEnable()) {
            TileParam tileParam = new TileParam(param);
            BitmapConfigTransformer transformer = BitmapConfigTransformer.create(tileParam);
            if (transformer != null) {
                transformer.doPreTransform();
            }
            processTile(tileParam);
            if (transformer != null) {
                transformer.doPostTransform();
                return;
            }
            return;
        }
        throw new UnsupportedOperationException("image process is disabled");
    }

    private void processTile(TileParam tileParam) {
        boolean processDone = false;
        CommonInfo commonInfo = null;
        if (mAlgoStrategy.needRunSoftwareAlgo(tileParam.mEngineType, tileParam.mImageType)) {
            commonInfo = getCommonInfo(tileParam.mFilePath);
            if (commonInfo == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processTile() error! can't find commonInfo for ");
                stringBuilder.append(tileParam.mFilePath);
                stringBuilder.append(", mCommonInfoCache size=");
                stringBuilder.append(this.mCommonInfoCache.size());
                DElog.e(str, stringBuilder.toString());
                if (tileParam.mEngineType == TileEngineType.NON_SR) {
                    transformColorspaceOnBitmap(tileParam, tileParam.mInBitmap, tileParam.mOutBitmap);
                }
                collectInfoForImageRecognization(tileParam, commonInfo);
                stringBuilder = new StringBuilder();
                stringBuilder.append("processTile() can't find commonInfo, mCommonInfoCache size=");
                stringBuilder.append(this.mCommonInfoCache.size());
                throw new IllegalStateException(stringBuilder.toString());
            }
            ImageEngine imageEngine = getImageEngine(tileParam.mAlgoHandle);
            if (imageEngine == null) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("processTile() error! can't find imageEngine for ");
                stringBuilder2.append(tileParam.mAlgoHandle);
                stringBuilder2.append(", mTileProcessEngineCache size=");
                stringBuilder2.append(this.mTileProcessEngineCache.size());
                DElog.e(str2, stringBuilder2.toString());
                if (tileParam.mEngineType == TileEngineType.NON_SR) {
                    transformColorspaceOnBitmap(tileParam, tileParam.mInBitmap, tileParam.mOutBitmap);
                }
                collectInfoForImageRecognization(tileParam, commonInfo);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("processTile() can't find imageEngine, mTileProcessEngineCache size=");
                stringBuilder2.append(this.mTileProcessEngineCache.size());
                throw new IllegalStateException(stringBuilder2.toString());
            }
            try {
                this.mAlgo.processTileAlgo(imageEngine, commonInfo, tileParam);
                processDone = true;
            } catch (RuntimeException e) {
                if (tileParam.mEngineType == TileEngineType.NON_SR) {
                    transformColorspaceOnBitmap(tileParam, tileParam.mInBitmap, tileParam.mOutBitmap);
                }
                collectInfoForImageRecognization(tileParam, commonInfo);
                throw e;
            }
        }
        if (tileParam.mEngineType == TileEngineType.NON_SR) {
            transformColorspaceOnBitmap(tileParam, processDone ? tileParam.mOutBitmap : tileParam.mInBitmap, tileParam.mOutBitmap);
            if (!processDone) {
                copyPixelsToOutBitmapIfNeeded(tileParam);
            }
        } else if (!processDone) {
            DElog.e(TAG, "processTile() error! engineType is SR but no algo run");
            throw new IllegalStateException("processTile() error! engineType is SR but no algo run");
        }
        collectInfoForImageRecognization(tileParam, commonInfo);
    }

    private void destroyTileProcessEngine(Map<String, Object> param) {
        DElog.d(TAG, "destroyTileProcessEngine()");
        if (mAlgoStrategy.isImageProcessorEnable()) {
            DestroyTileProcessEngineParam destroyTileProcessEngineParam = new DestroyTileProcessEngineParam(param);
            if (destroyTileProcessEngineParam.mAlgoHandle != 0) {
                removeImageEngine(destroyTileProcessEngineParam.mAlgoHandle);
                return;
            }
            return;
        }
        throw new UnsupportedOperationException("image process is disabled");
    }

    private void saveImageEngine(ImageEngine imageEngine) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("TileProcessEngineCache save ");
        stringBuilder.append(imageEngine.mAlgoHandle);
        DElog.d(str, stringBuilder.toString());
        this.mTileProcessEngineCache.put(Long.valueOf(imageEngine.mAlgoHandle), imageEngine);
    }

    private ImageEngine getImageEngine(long algoHandle) {
        return (ImageEngine) this.mTileProcessEngineCache.get(Long.valueOf(algoHandle));
    }

    private void removeImageEngine(long algoHandle) {
        ImageEngine imageEngine = (ImageEngine) this.mTileProcessEngineCache.get(Long.valueOf(algoHandle));
        if (imageEngine == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeImageEngine() error! can't find ");
            stringBuilder.append(algoHandle);
            DElog.e(str, stringBuilder.toString());
            return;
        }
        this.mAlgo.destroyImageEngine(imageEngine);
        this.mTileProcessEngineCache.remove(Long.valueOf(algoHandle));
    }

    private void clearImageEngine() {
        if (!this.mTileProcessEngineCache.isEmpty()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TileProcessEngineCache clear size=");
            stringBuilder.append(this.mTileProcessEngineCache.size());
            DElog.e(str, stringBuilder.toString());
            synchronized (this.mTileProcessEngineCache) {
                for (ImageEngine imageEngine : this.mTileProcessEngineCache.values()) {
                    this.mAlgo.destroyImageEngine(imageEngine);
                }
            }
            this.mTileProcessEngineCache.clear();
        }
    }

    public void setScene(int scene, int action) {
        if (action != 13) {
            switch (action) {
                case 9:
                    DElog.i(TAG, "setScene THUMBNAIL");
                    this.mCurrentFilePath = null;
                    this.mHardwareSharpnessLevel = -1;
                    return;
                case 10:
                    DElog.i(TAG, "setScene FULLSCREEN");
                    return;
                default:
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setScene unknown action = ");
                    stringBuilder.append(action);
                    DElog.e(str, stringBuilder.toString());
                    return;
            }
        }
        DElog.i(TAG, "setScene EXIT");
        clearCommonInfo();
    }
}
