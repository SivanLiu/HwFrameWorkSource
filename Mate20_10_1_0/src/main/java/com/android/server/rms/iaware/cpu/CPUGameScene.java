package com.android.server.rms.iaware.cpu;

import android.iawareperf.UniPerf;
import android.rms.iaware.AwareLog;
import com.android.server.rms.iaware.cpu.CPUFeature;
import java.util.concurrent.atomic.AtomicBoolean;

public class CPUGameScene {
    private static final int GAME_SCENE_DELAYED = 3000;
    private static final Object SLOCK = new Object();
    private static final String TAG = "CPUGameScene";
    private static CPUGameScene sInstance;
    private CPUFeature.CPUFeatureHandler mCpuFeatureHandler;
    private AtomicBoolean mIsFeatureEnable = new AtomicBoolean(false);
    private AtomicBoolean mSetGameScene = new AtomicBoolean(false);

    private CPUGameScene() {
    }

    public static CPUGameScene getInstance() {
        CPUGameScene cPUGameScene;
        synchronized (SLOCK) {
            if (sInstance == null) {
                sInstance = new CPUGameScene();
            }
            cPUGameScene = sInstance;
        }
        return cPUGameScene;
    }

    public void enable(CPUFeature.CPUFeatureHandler handler) {
        if (this.mIsFeatureEnable.get()) {
            AwareLog.e(TAG, "CPUGameScene has already enable!");
            return;
        }
        this.mCpuFeatureHandler = handler;
        this.mIsFeatureEnable.set(true);
    }

    public void disable() {
        if (!this.mIsFeatureEnable.get()) {
            AwareLog.e(TAG, "CPUGameScene has already disable!");
        } else {
            this.mIsFeatureEnable.set(false);
        }
    }

    public void enterGameSceneMsg() {
        CPUFeature.CPUFeatureHandler cPUFeatureHandler;
        if (this.mIsFeatureEnable.get() && (cPUFeatureHandler = this.mCpuFeatureHandler) != null) {
            cPUFeatureHandler.removeMessages(CPUFeature.MSG_ENTER_GAME_SCENE);
            this.mCpuFeatureHandler.sendEmptyMessageDelayed(CPUFeature.MSG_ENTER_GAME_SCENE, 3000);
        }
    }

    public void exitGameSceneMsg() {
        CPUFeature.CPUFeatureHandler cPUFeatureHandler;
        if (this.mIsFeatureEnable.get() && (cPUFeatureHandler = this.mCpuFeatureHandler) != null) {
            cPUFeatureHandler.removeMessages(CPUFeature.MSG_EXIT_GAME_SCENE);
            if (this.mSetGameScene.get()) {
                this.mCpuFeatureHandler.sendEmptyMessageDelayed(CPUFeature.MSG_EXIT_GAME_SCENE, 0);
            } else {
                this.mCpuFeatureHandler.sendEmptyMessageDelayed(CPUFeature.MSG_EXIT_GAME_SCENE, 3000);
            }
        }
    }

    public void setGameScene() {
        UniPerf.getInstance().uniPerfEvent(4120, "", new int[]{0});
        this.mSetGameScene.set(true);
    }

    public void resetGameScene() {
        UniPerf.getInstance().uniPerfEvent(4120, "", new int[]{-1});
        this.mSetGameScene.set(false);
    }

    public void setScreenOffScene() {
        if (this.mIsFeatureEnable.get() && this.mSetGameScene.get()) {
            resetGameScene();
        }
    }
}
