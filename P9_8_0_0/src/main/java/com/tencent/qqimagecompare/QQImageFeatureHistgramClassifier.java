package com.tencent.qqimagecompare;

import java.util.ArrayList;

public class QQImageFeatureHistgramClassifier {
    private int mThreshold = 80;
    private ArrayList<QQImageFeatureHSV> nE = new ArrayList();
    private IClassifyCallback nF;
    private Object nG;
    private int nH;

    public interface IClassifyCallback {
        void OnStep(int i, Object obj);
    }

    public void addFeature(QQImageFeatureHSV qQImageFeatureHSV) {
        this.nE.add(qQImageFeatureHSV);
        this.nH = this.nE.size();
    }

    public ArrayList<ArrayList<QQImageFeatureHSV>> classify() {
        Object -l_1_R = new ArrayList();
        if (this.nE.size() > 0) {
            while (true) {
                int -l_2_I = this.nE.size();
                Object -l_3_R;
                if (-l_2_I != 1) {
                    if (-l_2_I <= 1) {
                        break;
                    }
                    -l_3_R = new ArrayList();
                    QQImageFeatureHSV -l_4_R = (QQImageFeatureHSV) this.nE.remove(0);
                    -l_3_R.add(-l_4_R);
                    Object -l_5_R = this.nE.iterator();
                    while (-l_5_R.hasNext()) {
                        QQImageFeatureHSV -l_6_R = (QQImageFeatureHSV) -l_5_R.next();
                        if (-l_4_R.compare(-l_6_R) >= this.mThreshold) {
                            -l_3_R.add(-l_6_R);
                            -l_5_R.remove();
                        }
                    }
                    -l_1_R.add(-l_3_R);
                    if (this.nF != null) {
                        this.nF.OnStep(((this.nH - this.nE.size()) * 100) / this.nH, this.nG);
                    }
                } else {
                    -l_3_R = new ArrayList();
                    -l_3_R.add(this.nE.remove(0));
                    -l_1_R.add(-l_3_R);
                    if (this.nF != null) {
                        this.nF.OnStep(((this.nH - this.nE.size()) * 100) / this.nH, this.nG);
                    }
                }
            }
        }
        return -l_1_R;
    }

    public void setClassifyCallback(IClassifyCallback iClassifyCallback, Object obj) {
        this.nF = iClassifyCallback;
        this.nG = obj;
    }

    public void setThreshold(int i) {
        this.mThreshold = i;
    }
}
