package huawei.android.security.facerecognition.task;

import huawei.android.security.facerecognition.base.HwSecurityTaskBase;
import huawei.android.security.facerecognition.base.HwSecurityTaskBase.RetCallback;
import huawei.android.security.facerecognition.base.HwSecurityTaskThread;
import huawei.android.security.facerecognition.request.FaceRecognizeRequest;
import huawei.android.security.facerecognition.utils.LogUtil;

public class OpenCameraRetryTask extends FaceRecognizeTask {
    private static final int RETRY_TIMES = 3;
    private static final String TAG = "OpenCameraRetry";
    private RetCallback mOpenCameraCallback = new RetCallback() {
        public void onTaskCallback(HwSecurityTaskBase child, int ret) {
            if (ret == 0 || ret == 2) {
                OpenCameraRetryTask.this.endWithResult(ret);
            } else {
                if ((child instanceof OpenCameraTask) && ((OpenCameraTask) child).canRetry()) {
                    OpenCameraRetryTask openCameraRetryTask = OpenCameraRetryTask.this;
                    if (openCameraRetryTask.mTimeLeft = openCameraRetryTask.mTimeLeft - 1 > 0) {
                        HwSecurityTaskThread.staticPushTask(new RetryWaitTask(OpenCameraRetryTask.this, OpenCameraRetryTask.this.mRetryWaitCallback, OpenCameraRetryTask.this.mTaskRequest), 1);
                        return;
                    }
                }
                OpenCameraRetryTask.this.endWithResult(5);
            }
        }
    };
    private RetCallback mRetryWaitCallback = new RetCallback() {
        public void onTaskCallback(HwSecurityTaskBase child, int ret) {
            if (ret == 0) {
                HwSecurityTaskThread.staticPushTask(new OpenCameraTask(OpenCameraRetryTask.this, OpenCameraRetryTask.this.mOpenCameraCallback, OpenCameraRetryTask.this.mTaskRequest), 1);
            } else {
                OpenCameraRetryTask.this.endWithResult(2);
            }
        }
    };
    private int mTimeLeft = 3;

    public OpenCameraRetryTask(HwSecurityTaskBase parent, RetCallback callback, FaceRecognizeRequest request) {
        super(parent, callback, request);
    }

    public int doAction() {
        LogUtil.i("", "start OpenCameraRetryTask");
        HwSecurityTaskThread.staticPushTask(new RetryWaitTask(this, this.mRetryWaitCallback, this.mTaskRequest), 1);
        return -1;
    }
}
