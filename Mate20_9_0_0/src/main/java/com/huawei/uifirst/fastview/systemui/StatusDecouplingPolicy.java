package com.huawei.uifirst.fastview.systemui;

import com.huawei.uifirst.fastview.common.StatusDecoupling;

public class StatusDecouplingPolicy extends StatusDecoupling implements IStatusDecoupling {
    public final CallBack mCallback;
    private boolean mListeningSkipped = false;

    public interface CallBack {
        void associationHandle();

        void dropdownListenHandle(boolean z);

        boolean getUIProcessingState();

        void refreshUIState(Object obj);

        void setUIProcessingState(boolean z);

        int stateTransformation(Object obj);

        void taskAddition(boolean z);

        void timeoutHandle();
    }

    public StatusDecouplingPolicy(CallBack cb, int DelayTime, String modelName) {
        this.mCallback = cb;
        this.mDelayTime = DelayTime;
        this.mModelName = modelName;
    }

    public void onHandleClick(Object arg) {
        this.mUserClickEnable = true;
        int newState = this.mCallback.stateTransformation(arg);
        switch (newState) {
            case 0:
                this.mUserClickSkipped = false;
                this.mDesiredState = false;
                break;
            case 1:
                this.mUserClickSkipped = false;
                this.mDesiredState = true;
                break;
            case 2:
                this.mUserClickSkipped = true;
                this.mDesiredState ^= 1;
                break;
            default:
                this.mUserClickSkipped = false;
                this.mDesiredState = false;
                break;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onHandleClick  newState=");
        stringBuilder.append(newState);
        stringBuilder.append(" , mDesiredState =");
        stringBuilder.append(this.mDesiredState);
        stringBuilder.append(", mUserClickSkipped=");
        stringBuilder.append(this.mUserClickSkipped);
        locolLog(2, stringBuilder.toString());
        setTaskInProcessing(true);
    }

    public void onDeviceStateChanged(Object arg) {
        if (this.mUserClickEnable) {
            locolLog(2, "onDeviceStateChanged");
            checkTaskAdditionNeeded(this.mCallback.stateTransformation(arg));
            if (this.mIsTaskAdditionNeeded) {
                this.mCallback.taskAddition(this.mDesiredState);
                setTaskInProcessing(true);
            }
        }
    }

    public void onHandleUpdateState(Object arg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onHandleUpdateState arg= ");
        stringBuilder.append(arg);
        locolLog(2, stringBuilder.toString());
        CallBack callBack = this.mCallback;
        boolean z = hasListeningCallbackRemoveSkipped() && !this.mIsTaskInProcessing;
        callBack.dropdownListenHandle(z);
    }

    public void taskAddition(boolean desireState) {
        this.mCallback.taskAddition(desireState);
    }

    public void timeoutHandle() {
        this.mCallback.timeoutHandle();
    }

    public void exitPolicy() {
        locolLog(3, "exitPolicy !!!");
        this.mDesiredState = false;
        setTaskInProcessing(false);
        this.mUserClickSkipped = false;
        this.mIsTaskAdditionNeeded = false;
        this.mListeningSkipped = false;
        this.mUserClickEnable = false;
    }

    public void associationHandle() {
        this.mCallback.associationHandle();
    }

    public void refreshUIState(Object arg_state) {
        this.mCallback.refreshUIState(arg_state);
    }

    public boolean getUIProcessingState() {
        return this.mCallback.getUIProcessingState();
    }

    public void setUIProcessingState(boolean uiProcessingState) {
        this.mCallback.setUIProcessingState(uiProcessingState);
    }

    public boolean isListeningCallbackRemoveSkipNeeded() {
        boolean z = this.mUserClickSkipped && this.mIsTaskInProcessing;
        this.mListeningSkipped = z;
        return this.mListeningSkipped;
    }

    public boolean hasListeningCallbackRemoveSkipped() {
        return this.mListeningSkipped;
    }

    public void resetListeningCallbackRemoveState() {
        this.mListeningSkipped = false;
    }
}
