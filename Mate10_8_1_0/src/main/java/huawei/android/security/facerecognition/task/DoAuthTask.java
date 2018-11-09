package huawei.android.security.facerecognition.task;

import huawei.android.security.facerecognition.FaceRecognizeEvent;
import huawei.android.security.facerecognition.FaceRecognizeManagerImpl.CallbackHolder;
import huawei.android.security.facerecognition.FaceRecognizeManagerImpl.ServiceHolder;
import huawei.android.security.facerecognition.base.HwSecurityMsgCenter;
import huawei.android.security.facerecognition.base.HwSecurityTaskBase;
import huawei.android.security.facerecognition.base.HwSecurityTaskBase.EventListener;
import huawei.android.security.facerecognition.base.HwSecurityTaskBase.RetCallback;
import huawei.android.security.facerecognition.base.HwSecurityTaskBase.TimerOutProc;
import huawei.android.security.facerecognition.base.HwSecurityTimerTask;
import huawei.android.security.facerecognition.request.FaceRecognizeRequest;
import huawei.android.security.facerecognition.utils.LogUtil;

public class DoAuthTask extends FaceRecognizeTask implements EventListener {
    private static final long TIMEOUT = 10000;
    private int mFlags;
    private int mRetErrorCode;
    private int mRetUserId;
    TimerOutProc mTimeoutProc = new TimerOutProc() {
        public void onTimerOut() {
            DoAuthTask.this.endWithResult(5);
        }
    };
    private HwSecurityTimerTask mTimer;

    public boolean onEvent(FaceRecognizeEvent ev) {
        switch (ev.getType()) {
            case 1:
                LogUtil.d("do auth", "cancel auth");
                if (ev.getArgs()[0] == this.mTaskRequest.getReqId()) {
                    this.mTaskRequest.setCancel();
                    endWithResult(2);
                    break;
                }
                return false;
            case 6:
                LogUtil.d("do auth", "interrupt auth");
                this.mTaskRequest.setCameraCancel();
                endWithResult(2);
                break;
            case 10:
                this.mRetErrorCode = ev.getArgs()[0];
                LogUtil.d("do auth", "auth result : " + this.mRetErrorCode);
                if (this.mRetErrorCode != 0) {
                    endWithResult(6);
                    break;
                }
                this.mRetUserId = ev.getArgs()[1];
                endWithResult(0);
                break;
            case 11:
                LogUtil.d("do auth", "auth acquired");
                CallbackHolder.getInstance().onCallbackEvent(this.mTaskRequest.getReqId(), 2, 3, ev.getArgs()[0]);
                break;
            default:
                return false;
        }
        return true;
    }

    public DoAuthTask(HwSecurityTaskBase parent, RetCallback callback, FaceRecognizeRequest request, int flags) {
        super(parent, callback, request);
        this.mFlags = flags;
        this.mTimer = new HwSecurityTimerTask();
        HwSecurityMsgCenter.staticRegisterEvent(6, this, this);
        HwSecurityMsgCenter.staticRegisterEvent(1, this, this);
        HwSecurityMsgCenter.staticRegisterEvent(10, this, this);
        HwSecurityMsgCenter.staticRegisterEvent(11, this, this);
    }

    public int doAction() {
        LogUtil.i("", "do auth task");
        if (ServiceHolder.getInstance().authenticate(this.mFlags) != 0) {
            return 5;
        }
        this.mTimer.setTimeout(TIMEOUT, this.mTimeoutProc);
        return -1;
    }

    public void onStop() {
        this.mTimer.cancel();
        HwSecurityMsgCenter.staticUnregisterEvent(6, this);
        HwSecurityMsgCenter.staticUnregisterEvent(1, this);
        HwSecurityMsgCenter.staticUnregisterEvent(10, this);
        HwSecurityMsgCenter.staticUnregisterEvent(11, this);
    }

    public int getErrorCode() {
        return this.mRetErrorCode;
    }

    public int getUserId() {
        return this.mRetUserId;
    }
}
