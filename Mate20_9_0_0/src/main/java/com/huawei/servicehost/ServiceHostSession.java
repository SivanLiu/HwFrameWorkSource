package com.huawei.servicehost;

import android.hardware.camera2.TotalCaptureResult;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import com.huawei.servicehost.IIPListener.Stub;
import com.huawei.servicehost.d3d.IIPEvent4D3DKeyFrame;
import com.huawei.servicehost.d3d.IIPEvent4D3DStatus;
import com.huawei.servicehost.d3d.IIPRequest4CreatePipeline;
import com.huawei.servicehost.d3d.IIPRequest4SetSex;
import com.huawei.servicehost.normal.IIPEvent4Metadata;
import com.huawei.servicehost.normal.IIPRequest4Metadata;
import com.huawei.servicehost.pp.IIPEvent4PPStatus;
import com.huawei.servicehost.pp.IIPEvent4Thumbnail;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ServiceHostSession {
    private static final boolean DEBUG = false;
    private static final int QUICK_THUMB_HEAD_SIZE = 16;
    private static final int QUICK_THUMB_MAX_SIZE = 2097152;
    private static final int QUICK_THUMB_PIXEL_SIZE = 4;
    private static final String REQUEST_TYPE_CREATE_PIPELINE = "createpipeline";
    private static final String REQUEST_TYPE_D3D_CANCEL_CAPTURE = "cancelcapture";
    private static final String REQUEST_TYPE_D3D_SET_SEX = "setsex";
    private static final String REQUEST_TYPE_D3D_START_CAPTURE = "startcapture";
    private static final String REQUEST_TYPE_D3D_START_FACE3D = "startface3d";
    private static final String REQUEST_TYPE_DESTROY_PIPELINE = "destorypipeline";
    private static final String REQUEST_TYPE_SEND_METADATA = "metadata";
    public static final String SESSION_D3D = "d3d";
    private static final String SESSION_EVENT_D3DKEYFRAME = "keyframe";
    private static final String SESSION_EVENT_METADATA = "metadata";
    private static final String SESSION_EVENT_RESULT = "result";
    private static final String SESSION_EVENT_STATUS = "status";
    private static final String SESSION_EVENT_THUMBNAIL = "thumbnail";
    public static final String SESSION_NORMAL = "normal";
    public static final String SESSION_PP = "pp";
    private static final String TAG = "ServiceHostSession";
    private CaptureStatusListener mCaptureStatusListener = null;
    private IIPEvent4D3DKeyFrame mD3dKeyFrame = null;
    private IIPRequest4CreatePipeline mIpD3dRequest4CreatePipeline = null;
    private IIPListener mIpListener = new Stub() {
        public void onIPCompleted(IIPRequest iipRequest) throws RemoteException {
            Log.v(ServiceHostSession.TAG, "onIPCompleted");
        }

        public void onIPEvent(IIPEvent iipEvent) throws RemoteException {
            if (iipEvent != null) {
                IBinder obj = iipEvent.getObject();
                String type = iipEvent.getType();
                int i = -1;
                int i2 = 0;
                switch (type.hashCode()) {
                    case -934426595:
                        if (type.equals(ServiceHostSession.SESSION_EVENT_RESULT)) {
                            i = 3;
                            break;
                        }
                        break;
                    case -892481550:
                        if (type.equals(ServiceHostSession.SESSION_EVENT_STATUS)) {
                            i = 1;
                            break;
                        }
                        break;
                    case -450004177:
                        if (type.equals("metadata")) {
                            i = 0;
                            break;
                        }
                        break;
                    case 507522670:
                        if (type.equals(ServiceHostSession.SESSION_EVENT_D3DKEYFRAME)) {
                            i = 4;
                            break;
                        }
                        break;
                    case 1330532588:
                        if (type.equals(ServiceHostSession.SESSION_EVENT_THUMBNAIL)) {
                            i = 2;
                            break;
                        }
                        break;
                }
                String name;
                String str;
                StringBuilder stringBuilder;
                String path;
                switch (i) {
                    case 0:
                        if (ServiceHostSession.this.mMetadataListener != null) {
                            ServiceHostSession.this.mMetadataListener.onMetadataArrived(ServiceHostSession.this.serviceHostUtil.getTotalCaptureResult(IIPEvent4Metadata.Stub.asInterface(obj)));
                            break;
                        }
                        return;
                    case 1:
                        name = iipEvent.getName();
                        if (ServiceHostSession.SESSION_PP.equals(name)) {
                            if (ServiceHostSession.this.mCaptureStatusListener != null) {
                                IIPEvent4PPStatus iipEvent4PPStatus = IIPEvent4PPStatus.Stub.asInterface(obj);
                                str = ServiceHostSession.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("status name: ");
                                stringBuilder.append(name);
                                stringBuilder.append(", status: ");
                                stringBuilder.append(iipEvent4PPStatus.getStatus());
                                Log.i(str, stringBuilder.toString());
                                ServiceHostSession.this.mCaptureStatusListener.onCaptureStatusArrived(iipEvent4PPStatus.getStatus());
                            } else {
                                return;
                            }
                        }
                        if (ServiceHostSession.SESSION_D3D.equals(name) && ServiceHostSession.this.mStatusListener != null) {
                            ServiceHostSession.this.mStatusListener.onStatusArrived(IIPEvent4D3DStatus.Stub.asInterface(obj).getStatus());
                            break;
                        }
                        return;
                    case 2:
                        if (ServiceHostSession.this.mThumbnailListener != null) {
                            IIPEvent4Thumbnail iipEvent4Thumbnail = IIPEvent4Thumbnail.Stub.asInterface(obj);
                            path = iipEvent4Thumbnail.getFilePath();
                            str = ServiceHostSession.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("thumbnail arrived, file path: ");
                            stringBuilder.append(path);
                            Log.d(str, stringBuilder.toString());
                            byte[] data = ServiceHostSession.this.extractQuickThumbnail(iipEvent4Thumbnail);
                            if (data.length != 0) {
                                ServiceHostSession.this.setIpListener(null);
                                ServiceHostSession.this.mThumbnailListener.onThumbnailArrived(data, path);
                                break;
                            }
                            Log.e(ServiceHostSession.TAG, "quick thumbnail is null!");
                            return;
                        }
                        return;
                    case 3:
                        break;
                    case 4:
                        if (ServiceHostSession.this.mKeyFrameListener != null) {
                            ServiceHostSession.this.mD3dKeyFrame = IIPEvent4D3DKeyFrame.Stub.asInterface(obj);
                            if (ServiceHostSession.this.mD3dKeyFrame != null) {
                                i = ServiceHostSession.this.mD3dKeyFrame.getKeyFrameCount();
                                if (i > 0) {
                                    try {
                                        path = ServiceHostSession.TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("key frame count: ");
                                        stringBuilder.append(i);
                                        Log.i(path, stringBuilder.toString());
                                        ArrayList<ImageWrap> keyFrameList = new ArrayList();
                                        while (i2 < i) {
                                            ImageWrap keyFrameBuffer = ServiceHostSession.this.mD3dKeyFrame.getKeyFrame(i2);
                                            if (keyFrameBuffer == null) {
                                                Log.i(ServiceHostSession.TAG, "key frame buffer is invalid!");
                                            } else {
                                                keyFrameList.add(keyFrameBuffer);
                                            }
                                            i2++;
                                        }
                                        ServiceHostSession.this.mKeyFrameListener.onKeyFrameArrived(keyFrameList);
                                        break;
                                    } catch (RemoteException e) {
                                        str = ServiceHostSession.TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("receive key frame failed.");
                                        stringBuilder.append(e.getMessage());
                                        Log.e(str, stringBuilder.toString());
                                        break;
                                    }
                                }
                                path = ServiceHostSession.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("invalid frame count: ");
                                stringBuilder2.append(i);
                                Log.i(path, stringBuilder2.toString());
                                return;
                            }
                            Log.i(ServiceHostSession.TAG, "key frame is invalid.");
                            return;
                        }
                        return;
                    default:
                        name = ServiceHostSession.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("IP event type: ");
                        stringBuilder3.append(type);
                        Log.d(name, stringBuilder3.toString());
                        break;
                }
            }
        }
    };
    private com.huawei.servicehost.normal.IIPRequest4CreatePipeline mIpPreviewRequest4CreatePipeline = null;
    private com.huawei.servicehost.normal.IIPRequest4CreatePipeline mIpVideoRequest4CreatePipeline = null;
    private boolean mIsVideo = DEBUG;
    private KeyFrameListener mKeyFrameListener = null;
    private MetadataListener mMetadataListener = null;
    private com.huawei.servicehost.pp.IIPRequest4CreatePipeline mPpRequest4CreatePipeline = null;
    private IImageProcessSession mSession = null;
    private Object mSessionLock = new Object();
    private String mSessionType = SESSION_NORMAL;
    private StatusListener mStatusListener = null;
    private ThumbnailListener mThumbnailListener = null;
    private ServiceHostUtil serviceHostUtil = new ServiceHostUtil();

    public interface CaptureStatusListener {
        void onCaptureStatusArrived(int i);
    }

    public interface KeyFrameListener {
        void onKeyFrameArrived(ArrayList<ImageWrap> arrayList);
    }

    public interface MetadataListener {
        void onMetadataArrived(TotalCaptureResult totalCaptureResult);
    }

    public interface StatusListener {
        void onStatusArrived(int i);
    }

    public interface ThumbnailListener {
        void onThumbnailArrived(byte[] bArr, String str);
    }

    public ServiceHostSession(String type) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("create session for: ");
        stringBuilder.append(type);
        Log.i(str, stringBuilder.toString());
        this.mSession = ImageProcessManager.get().createIPSession(type);
        if (this.mSession == null) {
            Log.e(TAG, "create session failed!");
        }
        setIpListener(this.mIpListener);
        this.mSessionType = type;
    }

    public void destroy() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("destroy session for ");
        stringBuilder.append(this.mSessionType);
        Log.i(str, stringBuilder.toString());
        setIpListener(null);
        synchronized (this.mSessionLock) {
            if (this.mSession == null) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("session for ");
                stringBuilder2.append(this.mSessionType);
                stringBuilder2.append(" has destroyed before!");
                Log.i(str, stringBuilder2.toString());
                return;
            }
            try {
                this.mSession.process(this.mSession.createIPRequest(REQUEST_TYPE_DESTROY_PIPELINE), true);
                this.mSession = null;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("destroy successfully for ");
                stringBuilder.append(this.mSessionType);
                Log.i(str, stringBuilder.toString());
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("destroy session for ");
                stringBuilder3.append(this.mSessionType);
                stringBuilder3.append(" failed: ");
                stringBuilder3.append(e.getMessage());
                Log.e(str2, stringBuilder3.toString());
            }
        }
    }

    public List<SHSurface> exchangeSurface(List<SHSurface> shSurfaces, String json, List<Size> captureSizes) {
        if (shSurfaces == null) {
            Log.e(TAG, "invalid input surface list.");
            return null;
        } else if (shSurfaces.size() == 0) {
            Log.e(TAG, "invalid input surface.");
            return null;
        } else if (SESSION_D3D.equals(this.mSessionType)) {
            return exchangeSurface4D3d(shSurfaces, json);
        } else {
            if (captureSizes == null) {
                Log.e(TAG, "invalid input capture list.");
                return null;
            } else if (captureSizes.size() == 0) {
                Log.e(TAG, "invalid input capture size.");
                return null;
            } else {
                SHSurface shSurface;
                String str;
                StringBuilder stringBuilder;
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("exchange surfaces, size: ");
                stringBuilder2.append(shSurfaces.size());
                Log.i(str2, stringBuilder2.toString());
                SHSurface shPreview1Surface = null;
                SHSurface shPreview2Surface = null;
                SHSurface shVideo1Surface = null;
                for (SHSurface shSurface2 : shSurfaces) {
                    if (!(shSurface2 == null || shSurface2.size == null)) {
                        if (shSurface2.type != null) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("input surface: ");
                            stringBuilder.append(shSurface2.surface);
                            stringBuilder.append(", type: ");
                            stringBuilder.append(shSurface2.type);
                            stringBuilder.append(", size: ");
                            stringBuilder.append(shSurface2.size);
                            stringBuilder.append(", camera id: ");
                            stringBuilder.append(shSurface2.cameraId);
                            Log.i(str, stringBuilder.toString());
                            switch (shSurface2.type) {
                                case SURFACE_FOR_PREVIEW:
                                    if (shSurface2.cameraId != 0) {
                                        shPreview2Surface = shSurface2;
                                        break;
                                    }
                                    shPreview1Surface = shSurface2;
                                    break;
                                case SURFACE_FOR_VIDEO:
                                    shVideo1Surface = shSurface2;
                                    break;
                                default:
                                    Log.e(TAG, "invalid surface type!");
                                    break;
                            }
                        }
                    }
                }
                if (shPreview1Surface == null) {
                    Log.e(TAG, "invalid preview surface!");
                    return null;
                }
                List<SHSurface> shPreviewSurfaces;
                if (shVideo1Surface != null) {
                    processIpSessionVideoRequest(shPreview1Surface, shVideo1Surface, (Size) captureSizes.get(0), json);
                } else {
                    shPreviewSurfaces = new ArrayList();
                    shPreviewSurfaces.add(shPreview1Surface);
                    shPreviewSurfaces.add(shPreview2Surface);
                    processIpSessionPreviewRequest(shPreviewSurfaces, captureSizes, json);
                }
                shPreviewSurfaces = new ArrayList();
                shPreview1Surface.surface = getPreview1Surface();
                shPreviewSurfaces.add(shPreview1Surface);
                if (shVideo1Surface != null) {
                    shVideo1Surface.surface = getVideo1Surface();
                    if (shVideo1Surface.surface != null) {
                        shPreviewSurfaces.add(shVideo1Surface);
                    }
                }
                SHSurface shMetadataSurface = new SHSurface();
                shMetadataSurface.surface = getMetadataSurface();
                if (shMetadataSurface.surface != null) {
                    shMetadataSurface.type = SurfaceType.SURFACE_FOR_METADATA;
                    shMetadataSurface.cameraId = shPreview1Surface.cameraId;
                    shPreviewSurfaces.add(shMetadataSurface);
                }
                shSurface2 = new SHSurface();
                shSurface2.surface = getCapture1Surface();
                if (shSurface2.surface != null) {
                    shSurface2.type = SurfaceType.SURFACE_FOR_CAPTURE;
                    shSurface2.cameraId = shPreview1Surface.cameraId;
                    shPreviewSurfaces.add(shSurface2);
                }
                if (shPreview2Surface != null) {
                    shPreview2Surface.surface = getPreview2Surface();
                    if (shPreview2Surface.surface != null) {
                        shPreviewSurfaces.add(shPreview2Surface);
                    }
                    SHSurface shCapture2Surface = new SHSurface();
                    shCapture2Surface.surface = getCapture2Surface();
                    if (shCapture2Surface.surface != null) {
                        shCapture2Surface.type = SurfaceType.SURFACE_FOR_CAPTURE;
                        shCapture2Surface.cameraId = shPreview2Surface.cameraId;
                        shPreviewSurfaces.add(shCapture2Surface);
                    }
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("exchange surface successfully, surfaces size: ");
                stringBuilder.append(shPreviewSurfaces.size());
                Log.i(str, stringBuilder.toString());
                return shPreviewSurfaces;
            }
        }
    }

    public void capture(ServiceHostSession previewSession, String filePath, String json) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("capture with file path: ");
        stringBuilder.append(filePath);
        Log.i(str, stringBuilder.toString());
        processPpSessionCaptureRequest(previewSession, filePath, json);
    }

    public void setMetadataListener(MetadataListener listener) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set metadata listener: ");
        stringBuilder.append(listener);
        Log.i(str, stringBuilder.toString());
        this.mMetadataListener = listener;
    }

    public void setThumbnailListener(ThumbnailListener listener) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set thumbnail listener: ");
        stringBuilder.append(listener);
        Log.i(str, stringBuilder.toString());
        this.mThumbnailListener = listener;
    }

    public void setStatusListener(StatusListener listener) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set status listener: ");
        stringBuilder.append(listener);
        Log.i(str, stringBuilder.toString());
        this.mStatusListener = listener;
    }

    public void setCaptureStatusListener(CaptureStatusListener listener) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set capture status listener: ");
        stringBuilder.append(listener);
        Log.i(str, stringBuilder.toString());
        this.mCaptureStatusListener = listener;
    }

    public void setKeyFrameListener(KeyFrameListener listener) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set key frame listener: ");
        stringBuilder.append(listener);
        Log.i(str, stringBuilder.toString());
        this.mKeyFrameListener = listener;
    }

    public void sendRequest(List<ServiceHostMetadata> requests) {
        Log.i(TAG, "send request");
        if (!SESSION_D3D.equals(this.mSessionType)) {
            if (requests.size() <= 0) {
                Log.e(TAG, "invalid requests to servicehost.");
                return;
            }
            IIPRequest iipRequest4Metadata = createIPRequest("metadata");
            if (iipRequest4Metadata == null) {
                Log.e(TAG, "create IPRequest for metadata failed!");
                return;
            }
            try {
                IIPRequest4Metadata.Stub.asInterface(getIBinderFromRequest(iipRequest4Metadata)).setMetadata(((ServiceHostMetadata) requests.get(0)).getNativeMetadata());
                Log.d(TAG, "process metadata request.");
                process(iipRequest4Metadata, true);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("set metadata failed: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    public void setParameter(int key, Bundle param) {
    }

    public void getParameter(int key, Bundle param) {
    }

    private void processIpSessionPreviewRequest(List<SHSurface> shPreviewSurfaces, List<Size> captureSizes, String json) {
        RemoteException e;
        List<SHSurface> list = shPreviewSurfaces;
        List<Size> list2 = captureSizes;
        Log.i(TAG, "process IP session request.");
        IIPRequest ipRequest = createIPRequest(REQUEST_TYPE_CREATE_PIPELINE);
        IBinder binder = getIBinderFromRequest(ipRequest);
        if (binder == null) {
            Log.e(TAG, "IP request get binder failed!");
            return;
        }
        this.mIsVideo = DEBUG;
        SHSurface shPreview1Surface = (SHSurface) list.get(0);
        Size capture1Size = (Size) list2.get(0);
        SHSurface shPreview2Surface = null;
        if (shPreviewSurfaces.size() > 1) {
            shPreview2Surface = (SHSurface) list.get(1);
        }
        SHSurface shPreview2Surface2 = shPreview2Surface;
        Size capture2Size = null;
        if (captureSizes.size() > 1) {
            capture2Size = (Size) list2.get(1);
        }
        Size capture2Size2 = capture2Size;
        this.mIpPreviewRequest4CreatePipeline = com.huawei.servicehost.normal.IIPRequest4CreatePipeline.Stub.asInterface(binder);
        String str;
        try {
            String str2;
            StringBuilder stringBuilder;
            ImageDescriptor preview2ImageDescriptor;
            int usage = BufferShareManager.get().getDefaultUsage();
            String str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("get default usage: ");
            stringBuilder2.append(Integer.toHexString(usage));
            Log.i(str3, stringBuilder2.toString());
            ImageDescriptor preview1ImageDescriptor = new ImageDescriptor();
            preview1ImageDescriptor.setUsage(usage | ImageDescriptor.GRALLOC_USAGE_HW_COMPOSER);
            if (shPreview1Surface.size != null) {
                preview1ImageDescriptor.setWidth(shPreview1Surface.size.getWidth());
                preview1ImageDescriptor.setHeight(shPreview1Surface.size.getHeight());
                String str4 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("set preview1 size: ");
                stringBuilder3.append(shPreview1Surface.size.getWidth());
                stringBuilder3.append("x");
                stringBuilder3.append(shPreview1Surface.size.getHeight());
                Log.d(str4, stringBuilder3.toString());
            }
            this.mIpPreviewRequest4CreatePipeline.setPreviewFormat(preview1ImageDescriptor);
            this.mIpPreviewRequest4CreatePipeline.setPreview1Surface(shPreview1Surface.surface);
            ImageDescriptor capture1ImageDescriptor = new ImageDescriptor();
            capture1ImageDescriptor.setUsage(usage);
            capture1ImageDescriptor.setFormat(35);
            if (capture1Size != null) {
                capture1ImageDescriptor.setWidth(capture1Size.getWidth());
                capture1ImageDescriptor.setHeight(capture1Size.getHeight());
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("set capture1 size: ");
                stringBuilder.append(capture1Size.getWidth());
                stringBuilder.append("x");
                stringBuilder.append(capture1Size.getHeight());
                Log.d(str2, stringBuilder.toString());
            }
            this.mIpPreviewRequest4CreatePipeline.setCaptureFormat(capture1ImageDescriptor);
            if (shPreview2Surface2 != null) {
                preview2ImageDescriptor = new ImageDescriptor();
                preview2ImageDescriptor.setUsage(usage | ImageDescriptor.GRALLOC_USAGE_HW_COMPOSER);
                if (shPreview2Surface2.size != null) {
                    preview2ImageDescriptor.setWidth(shPreview2Surface2.size.getWidth());
                    preview2ImageDescriptor.setHeight(shPreview2Surface2.size.getHeight());
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("set preview2 size: ");
                    stringBuilder.append(shPreview2Surface2.size.getWidth());
                    stringBuilder.append("x");
                    stringBuilder.append(shPreview2Surface2.size.getHeight());
                    Log.d(str2, stringBuilder.toString());
                }
                this.mIpPreviewRequest4CreatePipeline.setPreviewFormatLine2(preview2ImageDescriptor);
                if (shPreview2Surface2.surface != null) {
                    this.mIpPreviewRequest4CreatePipeline.setPreview1Surface(shPreview2Surface2.surface);
                }
            }
            if (capture2Size2 != null) {
                preview2ImageDescriptor = new ImageDescriptor();
                preview2ImageDescriptor.setUsage(usage);
                preview2ImageDescriptor.setFormat(35);
                if (capture1Size != null) {
                    preview2ImageDescriptor.setWidth(capture1Size.getWidth());
                    preview2ImageDescriptor.setHeight(capture1Size.getHeight());
                    str = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("set capture2 size: ");
                    stringBuilder4.append(capture1Size.getWidth());
                    stringBuilder4.append("x");
                    stringBuilder4.append(capture1Size.getHeight());
                    Log.d(str, stringBuilder4.toString());
                }
                this.mIpPreviewRequest4CreatePipeline.setCaptureFormatLine2(preview2ImageDescriptor);
            }
            try {
                this.mIpPreviewRequest4CreatePipeline.setLayout(json);
                process(ipRequest, true);
            } catch (RemoteException e2) {
                e = e2;
            }
        } catch (RemoteException e3) {
            e = e3;
            str = json;
            String str5 = TAG;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("process IP session resuest failed: ");
            stringBuilder5.append(e.getMessage());
            Log.e(str5, stringBuilder5.toString());
        }
    }

    private void processPpSessionCaptureRequest(ServiceHostSession previewSession, String filePath, String json) {
        Log.i(TAG, "process PP session request");
        if (previewSession == null) {
            Log.e(TAG, "Invalid preview session!");
            return;
        }
        IIPRequest ipRequest = createIPRequest(REQUEST_TYPE_CREATE_PIPELINE);
        IBinder binder = getIBinderFromRequest(ipRequest);
        if (binder == null) {
            Log.e(TAG, "PP request get binder failed!");
            return;
        }
        if (this.mPpRequest4CreatePipeline == null) {
            this.mPpRequest4CreatePipeline = com.huawei.servicehost.pp.IIPRequest4CreatePipeline.Stub.asInterface(binder);
        }
        try {
            this.mPpRequest4CreatePipeline.setLayout(json);
            this.mPpRequest4CreatePipeline.setFilePath(filePath);
            this.mPpRequest4CreatePipeline.setForegroundSession(previewSession.asBinder());
            process(ipRequest, true);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("process PP session request failed: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    private void processIpSessionVideoRequest(SHSurface shPreviewSurface, SHSurface shVideoSurface, Size captureSize, String json) {
        Log.i(TAG, "process IP video request()");
        IIPRequest ipRequest = createIPRequest(REQUEST_TYPE_CREATE_PIPELINE);
        IBinder binder = getIBinderFromRequest(ipRequest);
        if (binder == null) {
            Log.e(TAG, "IP request get binder failed!");
            return;
        }
        this.mIsVideo = true;
        if (this.mIpVideoRequest4CreatePipeline == null) {
            this.mIpVideoRequest4CreatePipeline = com.huawei.servicehost.normal.IIPRequest4CreatePipeline.Stub.asInterface(binder);
        }
        try {
            int usage = BufferShareManager.get().getDefaultUsage();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get default usage: ");
            stringBuilder.append(Integer.toHexString(usage));
            Log.i(str, stringBuilder.toString());
            ImageDescriptor previewImageDescriptor = new ImageDescriptor();
            if (shPreviewSurface.size != null) {
                previewImageDescriptor.setWidth(shPreviewSurface.size.getWidth());
                previewImageDescriptor.setHeight(shPreviewSurface.size.getHeight());
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("set video preview size: ");
                stringBuilder2.append(shPreviewSurface.size.getWidth());
                stringBuilder2.append("x");
                stringBuilder2.append(shPreviewSurface.size.getHeight());
                Log.d(str2, stringBuilder2.toString());
            }
            previewImageDescriptor.setUsage(2304);
            this.mIpVideoRequest4CreatePipeline.setPreviewFormat(previewImageDescriptor);
            ImageDescriptor captureImageDescriptor = new ImageDescriptor();
            if (captureSize != null) {
                captureImageDescriptor.setWidth(captureSize.getWidth());
                captureImageDescriptor.setHeight(captureSize.getHeight());
            }
            captureImageDescriptor.setUsage(usage);
            captureImageDescriptor.setFormat(35);
            this.mIpVideoRequest4CreatePipeline.setCaptureFormat(captureImageDescriptor);
            if (captureSize != null) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("set video capture size: ");
                stringBuilder3.append(captureSize.getWidth());
                stringBuilder3.append("x");
                stringBuilder3.append(captureSize.getHeight());
                Log.d(str3, stringBuilder3.toString());
            }
            ImageDescriptor videoImageDescriptor = new ImageDescriptor();
            if (shVideoSurface.size != null) {
                videoImageDescriptor.setWidth(shVideoSurface.size.getWidth());
                videoImageDescriptor.setHeight(shVideoSurface.size.getHeight());
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("set video size: ");
                stringBuilder4.append(shVideoSurface.size.getWidth());
                stringBuilder4.append("x");
                stringBuilder4.append(shVideoSurface.size.getHeight());
                Log.d(str4, stringBuilder4.toString());
            }
            videoImageDescriptor.setUsage(73728);
            this.mIpVideoRequest4CreatePipeline.setVideoFormat(videoImageDescriptor);
            this.mIpVideoRequest4CreatePipeline.setLayout(json);
            this.mIpVideoRequest4CreatePipeline.setPreview1Surface(shPreviewSurface.surface);
            this.mIpVideoRequest4CreatePipeline.setCamera1VideoSurface(shVideoSurface.surface);
            process(ipRequest, true);
        } catch (RemoteException e) {
            String str5 = TAG;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("process ip video resuest failed:");
            stringBuilder5.append(e.getMessage());
            Log.e(str5, stringBuilder5.toString());
        }
    }

    private IIPRequest createIPRequest(String type) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("create IP request, type: ");
        stringBuilder.append(type);
        Log.i(str, stringBuilder.toString());
        synchronized (this.mSessionLock) {
            if (this.mSession == null) {
                Log.e(TAG, "session is null!");
                return null;
            }
            try {
                IIPRequest request = this.mSession.createIPRequest(type);
                Log.i(TAG, "create IP request end.");
                return request;
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("create ");
                stringBuilder2.append(type);
                stringBuilder2.append(" IP request failed: ");
                stringBuilder2.append(e.getMessage());
                Log.e(str2, stringBuilder2.toString());
                return null;
            } catch (NullPointerException e2) {
                Log.e(TAG, "session is null !");
                return null;
            }
        }
    }

    private IBinder getIBinderFromRequest(IIPRequest request) {
        Log.i(TAG, "get binder from request.");
        if (request == null) {
            Log.e(TAG, "request is null!");
            return null;
        }
        try {
            return request.getObject();
        } catch (RemoteException e) {
            Log.e(TAG, "Get IBinder from IP Request fail.");
            return null;
        }
    }

    private void process(IIPRequest request, boolean isSync) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("process request, sync: ");
        stringBuilder.append(isSync);
        Log.i(str, stringBuilder.toString());
        synchronized (this.mSessionLock) {
            if (this.mSession == null) {
                Log.e(TAG, "session is null!");
                return;
            }
            try {
                this.mSession.process(request, isSync);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("process request fail: ");
                stringBuilder2.append(e.getMessage());
                Log.e(str2, stringBuilder2.toString());
            } catch (NullPointerException e2) {
                Log.e(TAG, "session is null !");
            }
        }
        Log.i(TAG, "process request end.");
    }

    private IBinder asBinder() {
        synchronized (this.mSessionLock) {
            if (this.mSession == null) {
                Log.e(TAG, "session is null!");
                return null;
            }
            IBinder binder = this.mSession.asBinder();
            return binder;
        }
    }

    private Surface getPreview1Surface() {
        Surface surface = null;
        com.huawei.servicehost.normal.IIPRequest4CreatePipeline pipeline = this.mIpPreviewRequest4CreatePipeline;
        if (this.mIsVideo) {
            pipeline = this.mIpVideoRequest4CreatePipeline;
        }
        try {
            surface = pipeline.getCamera1Surface();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get preview1 surface fail: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        if (!checkSurfaceValid(surface)) {
            surface = null;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("get preview1 surface: ");
        stringBuilder2.append(surface);
        Log.i(str2, stringBuilder2.toString());
        return surface;
    }

    private Surface getPreview2Surface() {
        Surface surface = null;
        com.huawei.servicehost.normal.IIPRequest4CreatePipeline pipeline = this.mIpPreviewRequest4CreatePipeline;
        if (this.mIsVideo) {
            pipeline = this.mIpVideoRequest4CreatePipeline;
        }
        try {
            surface = pipeline.getCamera2Surface();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get preview2 surface fail: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        if (!checkSurfaceValid(surface)) {
            surface = null;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("get preview2 surface: ");
        stringBuilder2.append(surface);
        Log.i(str2, stringBuilder2.toString());
        return surface;
    }

    private Surface getMetadataSurface() {
        Surface surface = null;
        com.huawei.servicehost.normal.IIPRequest4CreatePipeline pipeline = this.mIpPreviewRequest4CreatePipeline;
        if (this.mIsVideo) {
            pipeline = this.mIpVideoRequest4CreatePipeline;
        }
        try {
            surface = pipeline.getMetadataSurface();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get metadata surface fail: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        if (!checkSurfaceValid(surface)) {
            surface = null;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("get metadata surface: ");
        stringBuilder2.append(surface);
        Log.i(str2, stringBuilder2.toString());
        return surface;
    }

    private Surface getCapture1Surface() {
        Surface surface = null;
        com.huawei.servicehost.normal.IIPRequest4CreatePipeline pipeline = this.mIpPreviewRequest4CreatePipeline;
        if (this.mIsVideo) {
            pipeline = this.mIpVideoRequest4CreatePipeline;
        }
        try {
            surface = pipeline.getCamera1CapSurface();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get capture1 surface fail: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        if (!checkSurfaceValid(surface)) {
            surface = null;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("get capture1 surface: ");
        stringBuilder2.append(surface);
        Log.i(str2, stringBuilder2.toString());
        return surface;
    }

    private Surface getCapture2Surface() {
        Surface surface = null;
        com.huawei.servicehost.normal.IIPRequest4CreatePipeline pipeline = this.mIpPreviewRequest4CreatePipeline;
        if (this.mIsVideo) {
            pipeline = this.mIpVideoRequest4CreatePipeline;
        }
        try {
            surface = pipeline.getCamera2CapSurface();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get capture2 surface fail: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        if (!checkSurfaceValid(surface)) {
            surface = null;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("get capture2 surface: ");
        stringBuilder2.append(surface);
        Log.i(str2, stringBuilder2.toString());
        return surface;
    }

    private Surface getVideo1Surface() {
        Surface surface = null;
        try {
            surface = this.mIpVideoRequest4CreatePipeline.getCamera1VideoSurface();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get video1 surface fail: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        if (!checkSurfaceValid(surface)) {
            surface = null;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("get video1 surface: ");
        stringBuilder2.append(surface);
        Log.i(str2, stringBuilder2.toString());
        return surface;
    }

    private void setIpListener(IIPListener listener) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set IP listener to servicehost: ");
        stringBuilder.append(listener);
        Log.i(str, stringBuilder.toString());
        synchronized (this.mSessionLock) {
            if (this.mSession == null) {
                Log.e(TAG, "session is null!");
                return;
            }
            try {
                this.mSession.setIPListener(listener);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("create IP listner failed: ");
                stringBuilder2.append(e.getMessage());
                Log.e(str2, stringBuilder2.toString());
            }
        }
    }

    private byte[] extractQuickThumbnail(IIPEvent4Thumbnail iipEvent4Thumbnail) {
        Log.i(TAG, "extract quick thumbnail.");
        try {
            ByteBuffer dataBuffer = iipEvent4Thumbnail.getImage().getData();
            int width = (dataBuffer.get(8) & 255) + ((dataBuffer.get(9) & 255) << 8);
            int height = (dataBuffer.get(12) & 255) + ((dataBuffer.get(13) & 255) << 8);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("width: ");
            stringBuilder.append(width);
            stringBuilder.append(", heigth: ");
            stringBuilder.append(height);
            Log.i(str, stringBuilder.toString());
            int quickThumbSize = ((width * height) * 4) + 16;
            if (quickThumbSize <= 0 || quickThumbSize > QUICK_THUMB_MAX_SIZE) {
                quickThumbSize = QUICK_THUMB_MAX_SIZE;
            }
            byte[] data = new byte[quickThumbSize];
            dataBuffer.rewind();
            dataBuffer.get(data);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("extract quick thumbnail, size: ");
            stringBuilder2.append(quickThumbSize);
            Log.i(str2, stringBuilder2.toString());
            return data;
        } catch (RemoteException e) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("parse event fail:");
            stringBuilder3.append(e.getMessage());
            Log.e(str3, stringBuilder3.toString());
            return new byte[0];
        }
    }

    private boolean checkSurfaceValid(Surface surface) {
        if (surface == null) {
            return DEBUG;
        }
        if (surface.isValid()) {
            return true;
        }
        Log.e(TAG, "get invalid surface!");
        return DEBUG;
    }

    private List<SHSurface> exchangeSurface4D3d(List<SHSurface> shSurfaces, String json) {
        Log.i(TAG, "exchange surface for d3d.");
        SHSurface shPreviewSurface = (SHSurface) shSurfaces.get(0);
        if (shPreviewSurface == null) {
            Log.e(TAG, "invalid preview surface!");
            return null;
        }
        int slaveCameraId = 2;
        if (shSurfaces.size() > 1) {
            slaveCameraId = ((SHSurface) shSurfaces.get(1)).cameraId;
        }
        processIpSessionD3dRequest(shPreviewSurface, json);
        List<SHSurface> surfaces = new ArrayList();
        try {
            SHSurface shPreview1Surface = new SHSurface();
            shPreview1Surface.surface = this.mIpD3dRequest4CreatePipeline.getCamera1Surface();
            shPreview1Surface.type = SurfaceType.SURFACE_FOR_D3DPREVIEW;
            shPreview1Surface.cameraId = shPreviewSurface.cameraId;
            surfaces.add(shPreview1Surface);
            SHSurface shPreview2Surface = new SHSurface();
            shPreview2Surface.surface = this.mIpD3dRequest4CreatePipeline.getCamera2Surface();
            shPreview2Surface.type = SurfaceType.SURFACE_FOR_D3DPREVIEW;
            shPreview2Surface.cameraId = slaveCameraId;
            surfaces.add(shPreview2Surface);
            SHSurface shMetadataSurface = new SHSurface();
            shMetadataSurface.surface = this.mIpD3dRequest4CreatePipeline.getMetadataSurface();
            shMetadataSurface.type = SurfaceType.SURFACE_FOR_METADATA;
            shMetadataSurface.cameraId = shPreviewSurface.cameraId;
            surfaces.add(shMetadataSurface);
            SHSurface shDmapSurface = new SHSurface();
            shDmapSurface.surface = this.mIpD3dRequest4CreatePipeline.getDmapSurface();
            shDmapSurface.type = SurfaceType.SURFACE_FOR_DMAP;
            shDmapSurface.cameraId = shPreviewSurface.cameraId;
            surfaces.add(shDmapSurface);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("process set sex session resuest failed: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("d3d exchange surface successfully, surfaces size: ");
        stringBuilder2.append(surfaces.size());
        Log.i(str2, stringBuilder2.toString());
        return surfaces;
    }

    private void processIpSessionD3dRequest(SHSurface shPreviewSurface, String json) {
        Log.i(TAG, "process IP session request for d3d.");
        IIPRequest ipRequest = createIPRequest(REQUEST_TYPE_CREATE_PIPELINE);
        IBinder binder = getIBinderFromRequest(ipRequest);
        if (binder == null) {
            Log.e(TAG, "IP request for d3d get binder failed!");
            return;
        }
        this.mIpD3dRequest4CreatePipeline = IIPRequest4CreatePipeline.Stub.asInterface(binder);
        try {
            this.mIpD3dRequest4CreatePipeline.setPreview1Surface(shPreviewSurface.surface);
            this.mIpD3dRequest4CreatePipeline.setLayout(json);
            process(ipRequest, true);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("process d3d IP session resuest failed: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    public void startPhotoing() {
        Log.i(TAG, "start capture for d3d.");
        process(createIPRequest(REQUEST_TYPE_D3D_START_CAPTURE), true);
    }

    public void canclePhotoing() {
        Log.i(TAG, "cancel capture for d3d.");
        process(createIPRequest(REQUEST_TYPE_D3D_CANCEL_CAPTURE), true);
    }

    public void startFace3D() {
        Log.i(TAG, "start face 3d for d3d.");
        process(createIPRequest(REQUEST_TYPE_D3D_START_FACE3D), true);
    }

    public void setSex(int var, String filePath, int fileSource) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set sex, var: ");
        stringBuilder.append(var);
        stringBuilder.append(", file path: ");
        stringBuilder.append(filePath);
        stringBuilder.append(", file source: ");
        stringBuilder.append(fileSource);
        Log.i(str, stringBuilder.toString());
        IIPRequest request = createIPRequest(REQUEST_TYPE_D3D_SET_SEX);
        IBinder binder = getIBinderFromRequest(request);
        if (binder == null) {
            Log.e(TAG, "IP request for d3d get binder failed!");
            return;
        }
        IIPRequest4SetSex request4StartCapture = IIPRequest4SetSex.Stub.asInterface(binder);
        try {
            request4StartCapture.setSex(var);
            request4StartCapture.setFileSource(fileSource);
            request4StartCapture.setFilePath(filePath);
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("process set sex session resuest failed: ");
            stringBuilder2.append(e.getMessage());
            Log.e(str2, stringBuilder2.toString());
        }
        process(request, true);
    }

    public void releaseBuffer() {
        Log.i(TAG, "release key frame.");
        if (this.mD3dKeyFrame != null) {
            try {
                this.mD3dKeyFrame.release();
                Log.d(TAG, "release key frame success.");
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("release keyframe failed! ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
            }
        }
    }
}
