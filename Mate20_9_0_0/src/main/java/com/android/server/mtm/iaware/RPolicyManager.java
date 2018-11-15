package com.android.server.mtm.iaware;

import android.app.mtm.iaware.RSceneData;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.RPolicyData;
import com.android.server.mtm.iaware.policy.RPolicyCreator;

public class RPolicyManager implements RPolicyCreator {
    private static final int MSG_REPORT_SCENE = 1;
    private static final String TAG = "RPolicyManager";
    private final RPolicyCreator mDefaultRPolicyCreator = this;
    private final DispatchHandler mDispatchHandler;

    /* renamed from: com.android.server.mtm.iaware.RPolicyManager$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$rms$iaware$AwareConstant$FeatureType = new int[FeatureType.values().length];
    }

    private final class DispatchHandler extends Handler {
        public DispatchHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String str = RPolicyManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage msg.what = ");
            stringBuilder.append(msg.what);
            AwareLog.d(str, stringBuilder.toString());
            if (msg.what == 1) {
                RSceneData scene = msg.obj;
                RPolicyManager.this.getRPolicyCreator(FeatureType.getFeatureType(msg.arg1)).reportScene(scene);
            }
        }
    }

    public RPolicyManager(Context context, HandlerThread handlerThread) {
        this.mDispatchHandler = new DispatchHandler(handlerThread.getLooper());
    }

    public boolean reportScene(int featureId, RSceneData scene) {
        if (scene == null || FeatureType.getFeatureType(featureId) == FeatureType.FEATURE_INVALIDE_TYPE) {
            AwareLog.e(TAG, "reportScene error params");
            return false;
        }
        Message sceneMessage = this.mDispatchHandler.obtainMessage();
        sceneMessage.what = 1;
        sceneMessage.arg1 = featureId;
        sceneMessage.obj = scene;
        return this.mDispatchHandler.sendMessage(sceneMessage);
    }

    public RPolicyData acquirePolicyData(int featureId, RSceneData scene) {
        FeatureType featureType = FeatureType.getFeatureType(featureId);
        if (scene != null && featureType != FeatureType.FEATURE_INVALIDE_TYPE) {
            return getRPolicyCreator(featureType).createPolicyData(scene);
        }
        AwareLog.e(TAG, "acquirePolicyData error params");
        return null;
    }

    public RPolicyData createPolicyData(RSceneData scene) {
        AwareLog.d(TAG, "default createPolicyData");
        return null;
    }

    public void reportScene(RSceneData scene) {
        AwareLog.d(TAG, "default reportScene");
    }

    private RPolicyCreator getRPolicyCreator(FeatureType featureType) {
        int i = AnonymousClass1.$SwitchMap$android$rms$iaware$AwareConstant$FeatureType[featureType.ordinal()];
        return this.mDefaultRPolicyCreator;
    }
}
