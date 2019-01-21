package com.huawei.displayengine;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.RemoteException;
import android.os.Trace;
import com.huawei.displayengine.ImageProcessor.AlgoType;
import com.huawei.displayengine.ImageProcessor.ColorspaceParam;
import com.huawei.displayengine.ImageProcessor.ColorspaceType;
import com.huawei.displayengine.ImageProcessor.CommonInfo;
import com.huawei.displayengine.ImageProcessor.CreateTileProcessEngineParam;
import com.huawei.displayengine.ImageProcessor.ImageEngine;
import com.huawei.displayengine.ImageProcessor.ImageType;
import com.huawei.displayengine.ImageProcessor.ThumbnailParam;
import com.huawei.displayengine.ImageProcessor.TileEngineType;
import com.huawei.displayengine.ImageProcessor.TileParam;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.IOException;

class ImageProcessorAlgoImpl {
    private static final String TAG = "DE J ImageProcessorAlgoImpl";
    private static String mAlgoXmlPath = "";
    private int mHandle;
    private boolean mInited;
    private final IDisplayEngineServiceEx mService;

    private static class CreateCommonAlgoParam {
        private final int mAlgoType;
        private long mCommonHandle;
        private final int mISO;
        private final Bitmap mInBitmap;
        private final boolean mIsAlgoSkinBeauty;
        private final boolean mIsAlgoVivid;
        private final boolean mIsWideColorSpace;
        private final String mXmlPath = ImageProcessorAlgoImpl.mAlgoXmlPath;

        public CreateCommonAlgoParam(ThumbnailParam thumbnailParam) {
            this.mAlgoType = AlgoType.getType(thumbnailParam.mCommonAlgos);
            this.mInBitmap = thumbnailParam.mInBitmap;
            this.mISO = thumbnailParam.mISO;
            boolean z = false;
            this.mIsWideColorSpace = thumbnailParam.mImageType == ImageType.WIDE_COLOR_SPACE;
            this.mIsAlgoSkinBeauty = thumbnailParam.mImageType == ImageType.SKIN_BEAUTY;
            if (thumbnailParam.mImageType == ImageType.VIVID) {
                z = true;
            }
            this.mIsAlgoVivid = z;
            String str = ImageProcessorAlgoImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CreateCommonAlgoParam() thumbnail imageType=");
            stringBuilder.append(thumbnailParam.mImageType);
            stringBuilder.append(", commonAlgos=");
            stringBuilder.append(thumbnailParam.mCommonAlgos);
            DElog.i(str, stringBuilder.toString());
        }

        public long getCommonHandle() {
            return this.mCommonHandle;
        }
    }

    private static class CreateImageEngineAlgoParam {
        private long mAlgoHandle;
        private final int mAlgoType;
        private final int mBorder;
        private final int mImageHeight;
        private final int mImageWidth;
        private final float mSrMaxScale;
        private final String mXmlPath = ImageProcessorAlgoImpl.mAlgoXmlPath;

        public CreateImageEngineAlgoParam(ThumbnailParam thumbnailParam) {
            this.mAlgoType = AlgoType.getType(thumbnailParam.mAlgos);
            if (thumbnailParam.mScaleRatio != 1.0f) {
                float f = 2.0f;
                if (thumbnailParam.mScaleRatio < 2.0f) {
                    f = thumbnailParam.mScaleRatio;
                }
                this.mSrMaxScale = f;
            } else {
                this.mSrMaxScale = 1.0f;
            }
            this.mImageWidth = thumbnailParam.mInBitmap.getWidth();
            this.mImageHeight = thumbnailParam.mInBitmap.getHeight();
            this.mBorder = 0;
            String str = ImageProcessorAlgoImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CreateImageEngineAlgoParam() thumbnail type=");
            stringBuilder.append(thumbnailParam.mType);
            stringBuilder.append(", imageType=");
            stringBuilder.append(thumbnailParam.mImageType);
            stringBuilder.append(", algos=");
            stringBuilder.append(thumbnailParam.mAlgos);
            DElog.i(str, stringBuilder.toString());
        }

        public CreateImageEngineAlgoParam(CreateTileProcessEngineParam createTileProcessEngineParam) {
            this.mAlgoType = AlgoType.getType(createTileProcessEngineParam.mAlgos);
            if (createTileProcessEngineParam.mEngineType == TileEngineType.SR) {
                this.mSrMaxScale = 2.0f;
            } else {
                this.mSrMaxScale = 1.0f;
            }
            this.mImageWidth = createTileProcessEngineParam.mTileSize;
            this.mImageHeight = createTileProcessEngineParam.mTileSize;
            this.mBorder = createTileProcessEngineParam.mTileBorder;
            String str = ImageProcessorAlgoImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CreateImageEngineAlgoParam() tile type=");
            stringBuilder.append(createTileProcessEngineParam.mEngineType);
            stringBuilder.append(", algos=");
            stringBuilder.append(createTileProcessEngineParam.mAlgos);
            DElog.i(str, stringBuilder.toString());
        }

        public long getAlgoHandle() {
            return this.mAlgoHandle;
        }
    }

    private static class GetInfoFromCommonParam {
        private final long mCommonHandle;
        private int mHardwareSharpnessLevel;

        public GetInfoFromCommonParam(CommonInfo commonInfo) {
            this.mCommonHandle = commonInfo.mCommonHandle;
        }

        public int getHardwareSharpnessLevel() {
            return this.mHardwareSharpnessLevel;
        }
    }

    private static class ProcessImageEngineAlgoParam {
        private final long mAlgoHandle;
        private final long mCommonHandle;
        private final Bitmap mInBitmap;
        private final boolean mIsThumbnail;
        private final int mNonSrHeight;
        private final int mNonSrStartX;
        private final int mNonSrStartY;
        private final int mNonSrWidth;
        private final Bitmap mOutBitmap;
        private final float mSRStartX;
        private final float mSRStartY;
        private final int mSRVisibleEndX;
        private final int mSRVisibleEndY;
        private final int mSRVisibleStartX;
        private final int mSRVisibleStartY;
        private final float mScaleRatio;
        private final float mZoomInRatio;

        public ProcessImageEngineAlgoParam(ImageEngine imageEngine, CommonInfo commonInfo, ThumbnailParam thumbnailParam) {
            this.mInBitmap = thumbnailParam.mInBitmap;
            this.mOutBitmap = thumbnailParam.mOutBitmap;
            this.mAlgoHandle = imageEngine.mAlgoHandle;
            this.mCommonHandle = commonInfo.mCommonHandle;
            this.mIsThumbnail = true;
            this.mNonSrWidth = this.mInBitmap.getWidth();
            this.mNonSrHeight = this.mInBitmap.getHeight();
            this.mNonSrStartX = 0;
            this.mNonSrStartY = 0;
            this.mScaleRatio = thumbnailParam.mScaleRatio;
            this.mSRStartX = 0.0f;
            this.mSRStartY = 0.0f;
            this.mSRVisibleStartX = 0;
            this.mSRVisibleStartY = 0;
            this.mSRVisibleEndX = this.mInBitmap.getWidth() - 1;
            this.mSRVisibleEndY = this.mInBitmap.getHeight() - 1;
            this.mZoomInRatio = 1.0f;
        }

        public ProcessImageEngineAlgoParam(ImageEngine imageEngine, CommonInfo commonInfo, TileParam tileParam) {
            this.mInBitmap = tileParam.mInBitmap;
            this.mOutBitmap = tileParam.mOutBitmap;
            this.mAlgoHandle = imageEngine.mAlgoHandle;
            this.mCommonHandle = commonInfo.mCommonHandle;
            this.mIsThumbnail = false;
            if (tileParam.mEngineType == TileEngineType.SR) {
                this.mNonSrWidth = 0;
                this.mNonSrHeight = 0;
                this.mNonSrStartX = 0;
                this.mNonSrStartY = 0;
                this.mScaleRatio = tileParam.mScaleRatio;
                this.mSRStartX = tileParam.mScaledStartPoint.x;
                this.mSRStartY = tileParam.mScaledStartPoint.y;
                this.mSRVisibleStartX = tileParam.mInVisibleRange.left;
                this.mSRVisibleStartY = tileParam.mInVisibleRange.top;
                this.mSRVisibleEndX = tileParam.mInVisibleRange.right;
                this.mSRVisibleEndY = tileParam.mInVisibleRange.bottom;
                this.mZoomInRatio = tileParam.mZoomInRatio;
                return;
            }
            this.mNonSrWidth = tileParam.mDecodedSize.getWidth();
            this.mNonSrHeight = tileParam.mDecodedSize.getHeight();
            this.mNonSrStartX = tileParam.mDecodedStartPoint.x;
            this.mNonSrStartY = tileParam.mDecodedStartPoint.y;
            this.mScaleRatio = 1.0f;
            this.mSRStartX = 0.0f;
            this.mSRStartY = 0.0f;
            this.mSRVisibleStartX = 0;
            this.mSRVisibleStartY = 0;
            this.mSRVisibleEndX = this.mInBitmap.getWidth() - 1;
            this.mSRVisibleEndY = this.mInBitmap.getHeight() - 1;
            this.mZoomInRatio = 1.0f;
        }
    }

    private static class ProcessType {
        public static final int CREATE_COMMON = 1;
        public static final int CREATE_IMAGE_ENGINE = 4;
        public static final int DESTROY_COMMON = 2;
        public static final int DESTROY_IMAGE_ENGINE = 6;
        public static final int GET_INFO_FROM_COMMON = 3;
        public static final int PROCESS_IMAGE_ENGINE = 5;
        public static final int TRANSFORM_COLORSPACE = 7;

        private ProcessType() {
        }
    }

    private static class TransformColorspaceAlgoParam {
        private final Bitmap mInBitmap;
        private final int mInColorspace;
        private final Bitmap mOutBitmap;
        private final int mOutColorspace;

        public TransformColorspaceAlgoParam(ColorspaceParam colorspaceParam) {
            this.mInBitmap = colorspaceParam.mInBitmap;
            this.mOutBitmap = colorspaceParam.mOutBitmap;
            this.mInColorspace = getAlgoColorspaceId(colorspaceParam.mInColorspace);
            this.mOutColorspace = getAlgoColorspaceId(colorspaceParam.mOutColorspace);
        }

        private int getAlgoColorspaceId(ColorspaceType colorspaceType) {
            if (colorspaceType == null) {
                return 0;
            }
            switch (colorspaceType) {
                case SRGB:
                    return 0;
                case ADOBE_RGB:
                    return 1;
                case DISPLAY_P3:
                    return 2;
                case SUPER_GAMUT:
                    return 3;
                default:
                    return 0;
            }
        }
    }

    public ImageProcessorAlgoImpl(IDisplayEngineServiceEx service) {
        DElog.i(TAG, "ImageProcessorAlgoImpl enter");
        this.mService = service;
        initAlgo();
        initAlgoXmlPath();
    }

    public boolean isAlgoInitSuccess() {
        return this.mInited;
    }

    protected void finalize() throws Throwable {
        DElog.i(TAG, "finalize");
        try {
            if (this.mInited) {
                DElog.i(TAG, "deinitAlgo enter");
                Trace.traceBegin(8, "deinitAlgo");
                DisplayEngineLibraries.nativeDeinitAlgorithm(0, this.mHandle);
                Trace.traceEnd(8);
                DElog.i(TAG, "deinitAlgo exit");
                this.mInited = false;
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    private void initAlgo() {
        if (!this.mInited) {
            DElog.i(TAG, "initAlgo enter");
            Trace.traceBegin(8, "initAlgo");
            int ret = DisplayEngineLibraries.nativeInitAlgorithm(0);
            Trace.traceEnd(8);
            if (ret >= 0) {
                DElog.d(TAG, "initAlgo success");
                this.mInited = true;
                this.mHandle = ret;
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("initAlgo failed! ret = ");
                stringBuilder.append(ret);
                DElog.e(str, stringBuilder.toString());
            }
            DElog.i(TAG, "initAlgo exit");
        }
    }

    private String getLcdModelName() {
        if (this.mService == null) {
            DElog.e(TAG, "getLcdModelName() mService is null!");
            return null;
        }
        byte[] name = new byte[128];
        int ret = 0;
        try {
            int ret2 = this.mService.getEffect(14, 0, name, name.length);
            if (ret2 == 0) {
                return new String(name).trim().replaceAll("[^A-Za-z0-9_.-]", "_");
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getLcdModelName() getEffect failed! ret=");
            stringBuilder.append(ret2);
            DElog.e(str, stringBuilder.toString());
            return null;
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getLcdModelName() RemoteException ");
            stringBuilder2.append(e);
            DElog.e(str2, stringBuilder2.toString());
            return null;
        }
    }

    private String getProductName() {
        String product = Build.MODEL;
        if (product == null) {
            DElog.e(TAG, "getProductName() get android.os.Build.MODEL failed!");
            return null;
        }
        String[] productSplit = product.split("-");
        String str;
        StringBuilder stringBuilder;
        if (productSplit.length != 2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getProductName() product=");
            stringBuilder.append(product);
            stringBuilder.append(" format error!");
            DElog.e(str, stringBuilder.toString());
            return null;
        }
        String productName = productSplit[null];
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("getProductName() productName=");
        stringBuilder.append(productName);
        DElog.i(str, stringBuilder.toString());
        return productName;
    }

    private void initAlgoXmlPath() {
        String lcdModelName = getLcdModelName();
        if (lcdModelName == null) {
            DElog.w(TAG, "initAlgoXmlPath() getLcdModelName error!");
            return;
        }
        String productName;
        String xmlName = new StringBuilder();
        xmlName.append(lcdModelName);
        xmlName.append(".xml");
        xmlName = xmlName.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("display/effect/algorithm/imageprocessor/");
        stringBuilder.append(xmlName);
        File xmlFile = HwCfgFilePolicy.getCfgFile(stringBuilder.toString(), 0);
        if (xmlFile == null) {
            xmlFile = HwCfgFilePolicy.getCfgFile("display/effect/algorithm/imageprocessor/ImageProcessAlgoParam.xml", 0);
            if (xmlFile == null) {
                productName = getProductName();
                if (productName != null) {
                    String xmlNameWithProduct = new StringBuilder();
                    xmlNameWithProduct.append(productName);
                    xmlNameWithProduct.append("-");
                    xmlNameWithProduct.append(lcdModelName);
                    xmlNameWithProduct.append(".xml");
                    xmlNameWithProduct = xmlNameWithProduct.toString();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("gallery/display_engine/");
                    stringBuilder2.append(xmlNameWithProduct);
                    xmlFile = HwCfgFilePolicy.getCfgFile(stringBuilder2.toString(), 0);
                }
            }
        }
        if (xmlFile == null) {
            DElog.w(TAG, "initAlgoXmlPath() error! can't find xml");
            return;
        }
        try {
            mAlgoXmlPath = xmlFile.getCanonicalPath();
        } catch (IOException e) {
            productName = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("initAlgoXmlPath() IOException ");
            stringBuilder3.append(e);
            DElog.e(productName, stringBuilder3.toString());
        }
        DElog.i(TAG, "initAlgoXmlPath() success");
    }

    public void transformColorspace(ColorspaceParam colorspaceParam) {
        if (this.mInited) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("transformColorspace() ");
            stringBuilder.append(colorspaceParam.mInColorspace);
            stringBuilder.append(" -> ");
            stringBuilder.append(colorspaceParam.mOutColorspace);
            DElog.d(str, stringBuilder.toString());
            TransformColorspaceAlgoParam param = new TransformColorspaceAlgoParam(colorspaceParam);
            Trace.traceBegin(8, "transformColorspace");
            int ret = DisplayEngineLibraries.nativeProcessAlgorithm(0, this.mHandle, 7, param, null);
            Trace.traceEnd(8);
            if (ret != 0) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("transformColorspace() native_processAlgorithm error, ret = ");
                stringBuilder2.append(ret);
                DElog.e(TAG, stringBuilder2.toString());
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("transformColorspace failed ret=");
                stringBuilder3.append(ret);
                throw new ArithmeticException(stringBuilder3.toString());
            }
            return;
        }
        DElog.e(TAG, "transformColorspace() algo init failed");
        throw new IllegalStateException("transformColorspace algo init failed");
    }

    public long createCommonInfo(ThumbnailParam thumbnailParam) {
        if (this.mInited) {
            DElog.d(TAG, "createCommonInfo()");
            CreateCommonAlgoParam param = new CreateCommonAlgoParam(thumbnailParam);
            Trace.traceBegin(8, "createCommonInfo");
            int ret = DisplayEngineLibraries.nativeProcessAlgorithm(0, this.mHandle, 1, param, null);
            Trace.traceEnd(8);
            long handle = param.getCommonHandle();
            StringBuilder stringBuilder;
            if (ret != 0 || handle == 0) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("createCommonInfo() native_processAlgorithm error, ret = ");
                stringBuilder2.append(ret);
                DElog.e(TAG, stringBuilder2.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("createCommonInfo failed ret=");
                stringBuilder.append(ret);
                throw new ArithmeticException(stringBuilder.toString());
            }
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("createCommonInfo() commonHandle=");
            stringBuilder.append(handle);
            DElog.d(str, stringBuilder.toString());
            return handle;
        }
        DElog.e(TAG, "createCommonInfo() algo init failed");
        throw new IllegalStateException("createCommonInfo algo init failed");
    }

    public void destroyCommonInfo(CommonInfo commonInfo) {
        if (commonInfo != null && commonInfo.mCommonHandle != 0) {
            if (this.mInited) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("destroyCommonInfo(), commonHandle=");
                stringBuilder.append(commonInfo.mCommonHandle);
                DElog.d(str, stringBuilder.toString());
                Trace.traceBegin(8, "destroyCommonInfo");
                int ret = DisplayEngineLibraries.nativeProcessAlgorithm(0, this.mHandle, 2, commonInfo, null);
                Trace.traceEnd(8);
                if (ret == 0) {
                    commonInfo.mCommonHandle = 0;
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("destroyCommonInfo() native_processAlgorithm error, ret = ");
                stringBuilder.append(ret);
                DElog.e(TAG, stringBuilder.toString());
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("destroyCommonInfo failed ret=");
                stringBuilder2.append(ret);
                throw new ArithmeticException(stringBuilder2.toString());
            }
            DElog.e(TAG, "destroyCommonInfo() algo init failed");
            throw new IllegalStateException("destroyCommonInfo algo init failed");
        }
    }

    public int getHardwareSharpnessLevel(CommonInfo commonInfo) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getHardwareSharpnessLevel(), handle=");
        stringBuilder.append(commonInfo.mCommonHandle);
        DElog.d(str, stringBuilder.toString());
        return getInfoFromCommon(commonInfo).getHardwareSharpnessLevel();
    }

    private GetInfoFromCommonParam getInfoFromCommon(CommonInfo commonInfo) {
        if (this.mInited) {
            GetInfoFromCommonParam getInfoFromCommonParam = new GetInfoFromCommonParam(commonInfo);
            Trace.traceBegin(8, "getInfoFromCommon");
            int ret = DisplayEngineLibraries.nativeProcessAlgorithm(0, this.mHandle, 3, getInfoFromCommonParam, null);
            Trace.traceEnd(8);
            if (ret == 0) {
                return getInfoFromCommonParam;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getInfoFromCommon() native_processAlgorithm error, ret = ");
            stringBuilder.append(ret);
            DElog.e(TAG, stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getInfoFromCommon failed ret=");
            stringBuilder2.append(ret);
            throw new ArithmeticException(stringBuilder2.toString());
        }
        DElog.e(TAG, "getInfoFromCommon() algo init failed");
        throw new IllegalStateException("getInfoFromCommon algo init failed");
    }

    public ImageEngine createImageEngine(ThumbnailParam thumbnailParam) {
        return createImageEngine(new CreateImageEngineAlgoParam(thumbnailParam));
    }

    public ImageEngine createImageEngine(CreateTileProcessEngineParam createTileProcessEngineParam) {
        return createImageEngine(new CreateImageEngineAlgoParam(createTileProcessEngineParam));
    }

    private ImageEngine createImageEngine(CreateImageEngineAlgoParam param) {
        if (this.mInited) {
            DElog.d(TAG, "createImageEngineAlgo()");
            Trace.traceBegin(8, "createImageEngine");
            int ret = DisplayEngineLibraries.nativeProcessAlgorithm(0, this.mHandle, 4, param, null);
            Trace.traceEnd(8);
            StringBuilder stringBuilder;
            if (ret != 0 || param.getAlgoHandle() == 0) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("createImageEngineAlgo() native_processAlgorithm error, ret = ");
                stringBuilder2.append(ret);
                DElog.e(TAG, stringBuilder2.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("createImageEngineAlgo failed ret=");
                stringBuilder.append(ret);
                throw new ArithmeticException(stringBuilder.toString());
            }
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("createImageEngineAlgo() handle=");
            stringBuilder.append(param.getAlgoHandle());
            DElog.d(str, stringBuilder.toString());
            return new ImageEngine(param.getAlgoHandle());
        }
        DElog.e(TAG, "createImageEngine() algo init failed");
        throw new IllegalStateException("createImageEngine algo init failed");
    }

    private void processImageEngine(ProcessImageEngineAlgoParam param) {
        if (this.mInited) {
            DElog.d(TAG, "processImageEngine()");
            Trace.traceBegin(8, "processImageEngine");
            int ret = DisplayEngineLibraries.nativeProcessAlgorithm(0, this.mHandle, 5, param, null);
            Trace.traceEnd(8);
            if (ret != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processImageEngine() native_processAlgorithm error, ret = ");
                stringBuilder.append(ret);
                DElog.e(TAG, stringBuilder.toString());
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("processImageEngine failed ret=");
                stringBuilder2.append(ret);
                throw new ArithmeticException(stringBuilder2.toString());
            }
            return;
        }
        DElog.e(TAG, "processImageEngine() algo init failed");
        throw new IllegalStateException("processImageEngine algo init failed");
    }

    public void processThumbnail(ImageEngine imageEngine, CommonInfo commonInfo, ThumbnailParam thumbnailParam) {
        DElog.i(TAG, "processThumbnail()");
        processImageEngine(new ProcessImageEngineAlgoParam(imageEngine, commonInfo, thumbnailParam));
    }

    public void processTileAlgo(ImageEngine imageEngine, CommonInfo commonInfo, TileParam tileParam) {
        DElog.d(TAG, "processTileAlgo()");
        processImageEngine(new ProcessImageEngineAlgoParam(imageEngine, commonInfo, tileParam));
    }

    public void destroyImageEngine(ImageEngine imageEngine) {
        if (imageEngine.mAlgoHandle != 0) {
            if (this.mInited) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("destroyImageEngine(), handle=");
                stringBuilder.append(imageEngine.mAlgoHandle);
                DElog.d(str, stringBuilder.toString());
                Trace.traceBegin(8, "destroyImageEngine");
                int ret = DisplayEngineLibraries.nativeProcessAlgorithm(0, this.mHandle, 6, imageEngine, null);
                Trace.traceEnd(8);
                if (ret == 0) {
                    imageEngine.mAlgoHandle = 0;
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("destroyImageEngine() native_processAlgorithm error, ret = ");
                stringBuilder.append(ret);
                DElog.e(TAG, stringBuilder.toString());
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("destroyImageEngine failed ret=");
                stringBuilder2.append(ret);
                throw new ArithmeticException(stringBuilder2.toString());
            }
            DElog.e(TAG, "destroyImageEngine() algo init failed");
            throw new IllegalStateException("destroyImageEngine algo init failed");
        }
    }
}
