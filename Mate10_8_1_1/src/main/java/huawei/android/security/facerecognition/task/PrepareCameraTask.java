package huawei.android.security.facerecognition.task;

import android.view.Surface;
import huawei.android.security.facerecognition.base.HwSecurityTaskBase;
import huawei.android.security.facerecognition.base.HwSecurityTaskBase.RetCallback;
import huawei.android.security.facerecognition.base.HwSecurityTaskThread;
import huawei.android.security.facerecognition.request.FaceRecognizeRequest;
import huawei.android.security.facerecognition.utils.LogUtil;
import java.util.List;

public class PrepareCameraTask extends FaceRecognizeTask {
    private RetCallback mCreateSessionCallback = new RetCallback() {
        public void onTaskCallback(HwSecurityTaskBase child, int ret) {
            if (ret == 0) {
                HwSecurityTaskThread.staticPushTask(new RepeatingRequestTask(PrepareCameraTask.this, PrepareCameraTask.this.mRepeatingRequestCallback, PrepareCameraTask.this.mTaskRequest), 1);
            } else {
                PrepareCameraTask.this.endWithResult(ret);
            }
        }
    };
    private RetCallback mOpenCameraCallback = new RetCallback() {
        public void onTaskCallback(HwSecurityTaskBase child, int ret) {
            if (ret == 0) {
                HwSecurityTaskThread.staticPushTask(new CreateSessionTask(PrepareCameraTask.this, PrepareCameraTask.this.mCreateSessionCallback, PrepareCameraTask.this.mTaskRequest, PrepareCameraTask.this.mSurfaces), 1);
            } else if (ret == 2) {
                PrepareCameraTask.this.endWithResult(2);
            } else if ((child instanceof OpenCameraTask) && ((OpenCameraTask) child).canRetry()) {
                HwSecurityTaskThread.staticPushTask(new OpenCameraRetryTask(PrepareCameraTask.this, PrepareCameraTask.this.mOpenRetryCallback, PrepareCameraTask.this.mTaskRequest), 1);
            } else {
                PrepareCameraTask.this.endWithResult(ret);
            }
        }
    };
    private RetCallback mOpenRetryCallback = new RetCallback() {
        public void onTaskCallback(HwSecurityTaskBase child, int ret) {
            if (ret == 0) {
                HwSecurityTaskThread.staticPushTask(new CreateSessionTask(PrepareCameraTask.this, PrepareCameraTask.this.mCreateSessionCallback, PrepareCameraTask.this.mTaskRequest, PrepareCameraTask.this.mSurfaces), 1);
            } else {
                PrepareCameraTask.this.endWithResult(ret);
            }
        }
    };
    private RetCallback mRepeatingRequestCallback = new RetCallback() {
        public void onTaskCallback(HwSecurityTaskBase child, int ret) {
            PrepareCameraTask.this.endWithResult(ret);
        }
    };
    private List<Surface> mSurfaces;

    public PrepareCameraTask(FaceRecognizeTask parent, RetCallback callback, FaceRecognizeRequest taskRequest, List<Surface> surfaces) {
        super(parent, callback, taskRequest);
        this.mSurfaces = surfaces;
    }

    public int doAction() {
        LogUtil.i("", "start prepare camera task");
        HwSecurityTaskThread.staticPushTask(new OpenCameraTask(this, this.mOpenCameraCallback, this.mTaskRequest), 1);
        return -1;
    }
}
