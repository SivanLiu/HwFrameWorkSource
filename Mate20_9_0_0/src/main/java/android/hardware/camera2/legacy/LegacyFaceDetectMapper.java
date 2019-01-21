package android.hardware.camera2.legacy;

import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.legacy.ParameterUtils.ZoomData;
import android.hardware.camera2.utils.ParamsUtils;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.List;

public class LegacyFaceDetectMapper {
    private static final boolean DEBUG = false;
    private static String TAG = "LegacyFaceDetectMapper";
    private final Camera mCamera;
    private boolean mFaceDetectEnabled = false;
    private boolean mFaceDetectReporting = false;
    private boolean mFaceDetectScenePriority = false;
    private final boolean mFaceDetectSupported;
    private Face[] mFaces;
    private Face[] mFacesPrev;
    private final Object mLock = new Object();

    public LegacyFaceDetectMapper(Camera camera, CameraCharacteristics characteristics) {
        this.mCamera = (Camera) Preconditions.checkNotNull(camera, "camera must not be null");
        Preconditions.checkNotNull(characteristics, "characteristics must not be null");
        this.mFaceDetectSupported = ArrayUtils.contains((int[]) characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES), 1);
        if (this.mFaceDetectSupported) {
            this.mCamera.setFaceDetectionListener(new FaceDetectionListener() {
                public void onFaceDetection(Face[] faces, Camera camera) {
                    int lengthFaces = faces == null ? 0 : faces.length;
                    synchronized (LegacyFaceDetectMapper.this.mLock) {
                        if (LegacyFaceDetectMapper.this.mFaceDetectEnabled) {
                            LegacyFaceDetectMapper.this.mFaces = faces;
                        } else if (lengthFaces > 0) {
                            Log.d(LegacyFaceDetectMapper.TAG, "onFaceDetection - Ignored some incoming faces sinceface detection was disabled");
                        }
                    }
                }
            });
        }
    }

    public void processFaceDetectMode(CaptureRequest captureRequest, Parameters parameters) {
        Preconditions.checkNotNull(captureRequest, "captureRequest must not be null");
        boolean z = false;
        int fdMode = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.STATISTICS_FACE_DETECT_MODE, Integer.valueOf(0))).intValue();
        if (fdMode == 0 || this.mFaceDetectSupported) {
            int sceneMode = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.CONTROL_SCENE_MODE, Integer.valueOf(0))).intValue();
            if (sceneMode != 1 || this.mFaceDetectSupported) {
                switch (fdMode) {
                    case 0:
                    case 1:
                        break;
                    case 2:
                        Log.w(TAG, "processFaceDetectMode - statistics.faceDetectMode == FULL unsupported, downgrading to SIMPLE");
                        break;
                    default:
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("processFaceDetectMode - ignoring unknown statistics.faceDetectMode = ");
                        stringBuilder.append(fdMode);
                        Log.w(str, stringBuilder.toString());
                        return;
                }
                boolean enableFaceDetect = fdMode != 0 || sceneMode == 1;
                synchronized (this.mLock) {
                    if (enableFaceDetect != this.mFaceDetectEnabled) {
                        if (enableFaceDetect) {
                            this.mCamera.startFaceDetection();
                        } else {
                            this.mCamera.stopFaceDetection();
                            this.mFaces = null;
                        }
                        this.mFaceDetectEnabled = enableFaceDetect;
                        this.mFaceDetectScenePriority = sceneMode == 1;
                        if (fdMode != 0) {
                            z = true;
                        }
                        this.mFaceDetectReporting = z;
                    }
                }
                return;
            }
            Log.w(TAG, "processFaceDetectMode - ignoring control.sceneMode == FACE_PRIORITY; face detection is not available");
            return;
        }
        Log.w(TAG, "processFaceDetectMode - Ignoring statistics.faceDetectMode; face detection is not available");
    }

    public void mapResultFaces(CameraMetadataNative result, LegacyRequest legacyRequest) {
        int fdMode;
        Face[] faces;
        boolean fdScenePriority;
        CameraMetadataNative cameraMetadataNative = result;
        LegacyRequest legacyRequest2 = legacyRequest;
        Preconditions.checkNotNull(cameraMetadataNative, "result must not be null");
        Preconditions.checkNotNull(legacyRequest2, "legacyRequest must not be null");
        synchronized (this.mLock) {
            fdMode = this.mFaceDetectReporting ? 1 : 0;
            if (this.mFaceDetectReporting) {
                faces = this.mFaces;
            } else {
                faces = null;
            }
            fdScenePriority = this.mFaceDetectScenePriority;
            Face[] previousFaces = this.mFacesPrev;
            this.mFacesPrev = faces;
        }
        CameraCharacteristics characteristics = legacyRequest2.characteristics;
        Rect activeArray = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        ZoomData zoomData = ParameterUtils.convertScalerCropRegion(activeArray, (Rect) legacyRequest2.captureRequest.get(CaptureRequest.SCALER_CROP_REGION), legacyRequest2.previewSize, legacyRequest2.parameters);
        List<android.hardware.camera2.params.Face> convertedFaces = new ArrayList();
        if (faces != null) {
            int length = faces.length;
            int i = 0;
            while (i < length) {
                Face face = faces[i];
                if (face != null) {
                    convertedFaces.add(ParameterUtils.convertFaceFromLegacy(face, activeArray, zoomData));
                } else {
                    Log.w(TAG, "mapResultFaces - read NULL face from camera1 device");
                }
                i++;
                legacyRequest2 = legacyRequest;
            }
        }
        cameraMetadataNative.set(CaptureResult.STATISTICS_FACES, (android.hardware.camera2.params.Face[]) convertedFaces.toArray(new android.hardware.camera2.params.Face[0]));
        cameraMetadataNative.set(CaptureResult.STATISTICS_FACE_DETECT_MODE, Integer.valueOf(fdMode));
        if (fdScenePriority) {
            cameraMetadataNative.set(CaptureResult.CONTROL_SCENE_MODE, Integer.valueOf(1));
        }
    }
}
