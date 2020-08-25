package com.android.server.hidata.appqoe.ai;

import com.android.server.hidata.appqoe.HwAPPQoEUtils;

public class HwApkQoeAiCalc {
    private static final int FLOAT_TO_INT_ENLARGE_10E3 = 1000;
    private static final String TAG = "HiData_HwApkQoeAiCalc";
    private int mCurrentAiScore = 0;

    public int getCurrentAiScore() {
        return this.mCurrentAiScore;
    }

    /* JADX INFO: Multiple debug info for r0v2 double: [D('aiParamsNum' int), D('tmpInt' double)] */
    /* JADX INFO: Multiple debug info for r9v4 long: [D('curThres' long), D('psSlowProba' double)] */
    public boolean judgeQualitByAi(float[] info, int threshold) {
        long curJudgeValue;
        int aiParamsNum = 300;
        long[][] aiParams = HwApkQoeAiParams.APK_AI_PARAMS;
        double finalClfValue = 0.0d;
        double fakeBtsAggValue = 0.0d;
        double trueBtsAggValue = 0.0d;
        double psSlowProba = 0.0d;
        double tmpInt = 0.0d;
        int i = 0;
        while (i < aiParamsNum) {
            long curThres = aiParams[i][0];
            long weight = aiParams[i][1];
            long leftChildIsFake = aiParams[i][3];
            if (info[(int) aiParams[i][2]] * 1000.0f <= ((float) curThres)) {
                curJudgeValue = leftChildIsFake;
            } else {
                curJudgeValue = leftChildIsFake * -1;
            }
            finalClfValue += (double) (curJudgeValue * weight);
            if (curJudgeValue == 1) {
                fakeBtsAggValue += (double) (curJudgeValue * weight);
            } else {
                trueBtsAggValue += (double) (curJudgeValue * weight);
            }
            i++;
            aiParams = aiParams;
            aiParamsNum = aiParamsNum;
            psSlowProba = psSlowProba;
            tmpInt = tmpInt;
        }
        double tmpInt2 = fakeBtsAggValue - trueBtsAggValue;
        if (tmpInt2 == 0.0d) {
            tmpInt2 = 1.0d;
        }
        double psSlowProba2 = (1000.0d * fakeBtsAggValue) / tmpInt2;
        this.mCurrentAiScore = (int) psSlowProba2;
        HwAPPQoEUtils.logD(TAG, false, "ps_slow_proba = %{public}f", Double.valueOf(psSlowProba2));
        if (finalClfValue <= 0.0d || psSlowProba2 <= ((double) threshold)) {
            return false;
        }
        return true;
    }
}
