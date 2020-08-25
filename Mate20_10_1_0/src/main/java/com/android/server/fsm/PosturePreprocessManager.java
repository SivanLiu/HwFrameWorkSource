package com.android.server.fsm;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Slog;
import huawei.android.hardware.tp.HwTpManager;
import java.io.PrintWriter;

public final class PosturePreprocessManager {
    private static final String TAG = "Fsm_PosturePreprocessManager";
    private static final String TAG_FSM_TURNOFF = "turnoff";
    private static final String TAG_FSM_TURNON = "turnon";
    private static PosturePreprocessManager mInstance = null;
    /* access modifiers changed from: private */
    public Context mContext = null;
    private IntelligentPosturePreprocess mIntelligentPolicy = null;
    private final Object mLock = new Object();
    private NormalPosturePreprocess mNormalPolicy = null;
    private PosturePreprocessPolicy mPolicy = null;
    /* access modifiers changed from: private */
    public PostureStateMachine mPostureSm;
    private TestPosturePreprocess mTestPolicy = null;
    private int mWakeUpType = 0;

    private PosturePreprocessManager() {
    }

    protected static synchronized PosturePreprocessManager getInstance() {
        PosturePreprocessManager posturePreprocessManager;
        synchronized (PosturePreprocessManager.class) {
            if (mInstance == null) {
                mInstance = new PosturePreprocessManager();
            }
            posturePreprocessManager = mInstance;
        }
        return posturePreprocessManager;
    }

    /* access modifiers changed from: protected */
    public void init(Context context, int policy) {
        if (context == null) {
            Slog.e("Fsm_PosturePreprocessManager", "parameters is null, init failed.");
            return;
        }
        this.mContext = context;
        this.mIntelligentPolicy = new IntelligentPosturePreprocess();
        this.mNormalPolicy = new NormalPosturePreprocess();
        this.mTestPolicy = new TestPosturePreprocess();
        this.mPostureSm = PostureStateMachine.getInstance();
        updatePolicy(policy);
    }

    /* access modifiers changed from: protected */
    public void start(int wakeUpType) {
        this.mWakeUpType = wakeUpType;
        Slog.d("Fsm_PosturePreprocessManager", "start wakeUpType:" + wakeUpType);
        synchronized (this.mLock) {
            this.mPolicy.turnOn(wakeUpType);
        }
    }

    /* access modifiers changed from: protected */
    public void stop() {
        Slog.i("Fsm_PosturePreprocessManager", "stop");
        synchronized (this.mLock) {
            this.mPolicy.turnOff();
        }
    }

    /* access modifiers changed from: protected */
    public void updatePolicy(int type) {
        PosturePreprocessPolicy policy;
        synchronized (this.mLock) {
            if (type == 0) {
                policy = this.mNormalPolicy;
            } else if (type == 1) {
                policy = this.mIntelligentPolicy;
            } else if (type != 2) {
                try {
                    Slog.w("Fsm_PosturePreprocessManager", "invalid type" + type);
                    return;
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                policy = this.mTestPolicy;
            }
            if (this.mPolicy != null) {
                this.mPolicy.turnOff();
            }
            this.mPolicy = policy;
            this.mPolicy.turnOn(this.mWakeUpType);
        }
    }

    /* access modifiers changed from: protected */
    public void dump(String prefix, PrintWriter pw) {
        synchronized (this.mLock) {
            this.mPolicy.dump(prefix, pw);
        }
    }

    private class IntelligentPosturePreprocess implements PosturePreprocessPolicy {
        private static final int HANDHELD_MAIN = 1;
        private static final int HANDHELD_SUB = 2;
        private static final int HANDHELD_UNKNOWN = 0;
        private static final String TAG = "Fsm_IntelligentPosturePreprocess";
        private ISensorPostureCallback callback = new ISensorPostureCallback() {
            /* class com.android.server.fsm.PosturePreprocessManager.IntelligentPosturePreprocess.AnonymousClass1 */

            @Override // com.android.server.fsm.ISensorPostureCallback
            public void onPostureChange(int posture) {
                if (IntelligentPosturePreprocess.this.isFolded(posture)) {
                    IntelligentPosturePreprocess.this.updateHandheldPosture();
                } else {
                    int unused = IntelligentPosturePreprocess.this.mHandheldState = 0;
                }
                int unused2 = IntelligentPosturePreprocess.this.mSensorPosture = posture;
                IntelligentPosturePreprocess.this.processPosture();
            }
        };
        /* access modifiers changed from: private */
        public int mHandheldState = 0;
        private int mPosture = 100;
        /* access modifiers changed from: private */
        public int mSensorPosture = 100;
        private SensorPostureManager mSensorPostureManager = null;
        private HwTpManager mTpManager = null;

        IntelligentPosturePreprocess() {
            this.mSensorPostureManager = new SensorPostureManager(PosturePreprocessManager.this.mContext);
            this.mTpManager = HwTpManager.getInstance();
        }

        /* access modifiers changed from: private */
        public void processPosture() {
            Slog.i("Fsm_IntelligentPosturePreprocess", "processPosture HandheldState:" + this.mHandheldState + ", sensorposture:" + this.mSensorPosture + ", mPosture:" + this.mPosture);
            if (this.mSensorPosture != 100 && PosturePreprocessManager.this.mPostureSm != null) {
                int posture = this.mSensorPosture;
                int i = this.mHandheldState;
                if (i == 1) {
                    if (isFolded(this.mSensorPosture)) {
                        posture = 104;
                    }
                } else if (i == 2 && isFolded(this.mSensorPosture)) {
                    posture = 105;
                }
                if (this.mPosture != posture) {
                    this.mPosture = posture;
                    PosturePreprocessManager.this.mPostureSm.setPosture(this.mPosture);
                }
            }
        }

        /* access modifiers changed from: private */
        public boolean isFolded(int posture) {
            if (posture == 102 || posture == 101 || posture == 103) {
                return true;
            }
            return false;
        }

        /* access modifiers changed from: private */
        public void updateHandheldPosture() {
            if (this.mTpManager == null) {
                this.mTpManager = HwTpManager.getInstance();
            }
            this.mHandheldState = this.mTpManager.hwTsSetAftConfig("version:3+grab_gesture");
            Slog.i("Fsm_IntelligentPosturePreprocess", "updateHandheldPosture mHandheldState: " + this.mHandheldState);
        }

        @Override // com.android.server.fsm.PosturePreprocessPolicy
        public void turnOn(int wakeUpType) {
            Slog.d("Fsm_IntelligentPosturePreprocess", PosturePreprocessManager.TAG_FSM_TURNON);
            SensorPostureManager sensorPostureManager = this.mSensorPostureManager;
            if (sensorPostureManager == null) {
                Slog.i("Fsm_IntelligentPosturePreprocess", "mSensorPostureManager IS NULL");
            } else if (sensorPostureManager.turnOnPostureSensor(this.callback, wakeUpType)) {
                updateHandheldPosture();
            }
        }

        @Override // com.android.server.fsm.PosturePreprocessPolicy
        public void turnOff() {
            Slog.i("Fsm_IntelligentPosturePreprocess", PosturePreprocessManager.TAG_FSM_TURNOFF);
            SensorPostureManager sensorPostureManager = this.mSensorPostureManager;
            if (sensorPostureManager != null) {
                sensorPostureManager.turnOffPostureSensor(this.callback);
                this.mHandheldState = 0;
                this.mSensorPosture = 100;
                this.mPosture = 100;
                return;
            }
            Slog.i("Fsm_IntelligentPosturePreprocess", "mSensorPostureManager IS NULL");
        }

        @Override // com.android.server.fsm.PosturePreprocessPolicy
        public void dump(String prefix, PrintWriter pw) {
            pw.println((prefix + "  ") + "IntelligentPosturePreprocess");
        }
    }

    private class NormalPosturePreprocess implements PosturePreprocessPolicy {
        private static final String TAG = "Fsm_NormalPosturePreprocess";
        private ISensorPostureCallback callback = new ISensorPostureCallback() {
            /* class com.android.server.fsm.PosturePreprocessManager.NormalPosturePreprocess.AnonymousClass1 */

            @Override // com.android.server.fsm.ISensorPostureCallback
            public void onPostureChange(int posture) {
                Slog.i("Fsm_NormalPosturePreprocess", "onFoldStateChange posture:" + posture);
                if (PosturePreprocessManager.this.mPostureSm == null) {
                    return;
                }
                if (posture == 103) {
                    PosturePreprocessManager.this.mPostureSm.setPosture(posture, true);
                } else {
                    PosturePreprocessManager.this.mPostureSm.setPosture(posture, false);
                }
            }
        };
        private SensorFoldStateManager mSensorFoldStateManager = null;

        NormalPosturePreprocess() {
            this.mSensorFoldStateManager = new SensorFoldStateManager(PosturePreprocessManager.this.mContext);
        }

        @Override // com.android.server.fsm.PosturePreprocessPolicy
        public void turnOn(int wakeUpType) {
            Slog.d("Fsm_NormalPosturePreprocess", PosturePreprocessManager.TAG_FSM_TURNON);
            SensorFoldStateManager sensorFoldStateManager = this.mSensorFoldStateManager;
            if (sensorFoldStateManager != null) {
                sensorFoldStateManager.turnOnFoldStateSensor(this.callback, wakeUpType);
            } else {
                Slog.i("Fsm_NormalPosturePreprocess", "mSensorFoldStateManager IS NULL");
            }
        }

        @Override // com.android.server.fsm.PosturePreprocessPolicy
        public void turnOff() {
            Slog.i("Fsm_NormalPosturePreprocess", PosturePreprocessManager.TAG_FSM_TURNOFF);
            SensorFoldStateManager sensorFoldStateManager = this.mSensorFoldStateManager;
            if (sensorFoldStateManager != null) {
                sensorFoldStateManager.turnOffFoldStateSensor(this.callback);
            } else {
                Slog.i("Fsm_NormalPosturePreprocess", "mSensorFoldStateManager IS NULL");
            }
        }

        @Override // com.android.server.fsm.PosturePreprocessPolicy
        public void dump(String prefix, PrintWriter pw) {
            pw.println((prefix + "  ") + "NormalPosturePreprocess");
        }
    }

    private class TestPosturePreprocess implements PosturePreprocessPolicy {
        private static final String TAG = "Fsm_TestPosturePreprocess";

        private TestPosturePreprocess() {
        }

        private void processTestPosture() {
            int posture = 109;
            int mode = SystemProperties.getInt("persist.sys.foldDispMode", 0);
            if (mode == 1) {
                posture = 109;
            } else if (mode == 2) {
                posture = 101;
            } else if (mode != 3) {
                Slog.w("Fsm_TestPosturePreprocess", "getInitState mode = " + mode);
            } else {
                posture = 102;
            }
            Slog.i("Fsm_TestPosturePreprocess", "TestPosture posture = " + posture + ", mode = " + mode);
            PosturePreprocessManager.this.mPostureSm.setPosture(posture);
        }

        @Override // com.android.server.fsm.PosturePreprocessPolicy
        public void turnOn(int wakeUpType) {
            Slog.d("Fsm_TestPosturePreprocess", PosturePreprocessManager.TAG_FSM_TURNON);
            processTestPosture();
        }

        @Override // com.android.server.fsm.PosturePreprocessPolicy
        public void turnOff() {
            Slog.i("Fsm_TestPosturePreprocess", PosturePreprocessManager.TAG_FSM_TURNOFF);
        }

        @Override // com.android.server.fsm.PosturePreprocessPolicy
        public void dump(String prefix, PrintWriter pw) {
            pw.println((prefix + "  ") + "TestPosturePreprocess");
        }
    }
}
