package com.android.internal.ml.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class KMeans {
    private static final boolean DEBUG = false;
    private static final String TAG = "KMeans";
    private final int mMaxIterations;
    private final Random mRandomState;
    private float mSqConvergenceEpsilon;

    public static class Mean {
        float[] mCentroid;
        final ArrayList<float[]> mClosestItems = new ArrayList();

        public Mean(int dimension) {
            this.mCentroid = new float[dimension];
        }

        public Mean(float... centroid) {
            this.mCentroid = centroid;
        }

        public float[] getCentroid() {
            return this.mCentroid;
        }

        public List<float[]> getItems() {
            return this.mClosestItems;
        }

        public String toString() {
            return "Mean(centroid: " + Arrays.toString(this.mCentroid) + ", size: " + this.mClosestItems.size() + ")";
        }
    }

    public KMeans() {
        this(new Random());
    }

    public KMeans(Random random) {
        this(random, 30, 0.005f);
    }

    public KMeans(Random random, int maxIterations, float convergenceEpsilon) {
        this.mRandomState = random;
        this.mMaxIterations = maxIterations;
        this.mSqConvergenceEpsilon = convergenceEpsilon * convergenceEpsilon;
    }

    public List<Mean> predict(int k, float[][] inputData) {
        int i;
        checkDataSetSanity(inputData);
        int dimension = inputData[0].length;
        ArrayList<Mean> means = new ArrayList();
        for (i = 0; i < k; i++) {
            Mean m = new Mean(dimension);
            for (int j = 0; j < dimension; j++) {
                m.mCentroid[j] = this.mRandomState.nextFloat();
            }
            means.add(m);
        }
        for (i = 0; i < this.mMaxIterations && !step(means, inputData); i++) {
        }
        return means;
    }

    public static double score(List<Mean> means) {
        double score = 0.0d;
        int meansSize = means.size();
        for (int i = 0; i < meansSize; i++) {
            Mean mean = (Mean) means.get(i);
            for (int j = 0; j < meansSize; j++) {
                Mean compareTo = (Mean) means.get(j);
                if (mean != compareTo) {
                    score += Math.sqrt((double) sqDistance(mean.mCentroid, compareTo.mCentroid));
                }
            }
        }
        return score;
    }

    public void checkDataSetSanity(float[][] inputData) {
        if (inputData == null) {
            throw new IllegalArgumentException("Data set is null.");
        } else if (inputData.length == 0) {
            throw new IllegalArgumentException("Data set is empty.");
        } else if (inputData[0] == null) {
            throw new IllegalArgumentException("Bad data set format.");
        } else {
            int dimension = inputData[0].length;
            int length = inputData.length;
            int i = 1;
            while (i < length) {
                if (inputData[i] == null || inputData[i].length != dimension) {
                    throw new IllegalArgumentException("Bad data set format.");
                }
                i++;
            }
        }
    }

    private boolean step(ArrayList<Mean> means, float[][] inputData) {
        int i;
        for (i = means.size() - 1; i >= 0; i--) {
            ((Mean) means.get(i)).mClosestItems.clear();
        }
        for (i = inputData.length - 1; i >= 0; i--) {
            float[] current = inputData[i];
            nearestMean(current, means).mClosestItems.add(current);
        }
        boolean converged = true;
        for (i = means.size() - 1; i >= 0; i--) {
            Mean mean = (Mean) means.get(i);
            if (mean.mClosestItems.size() != 0) {
                int j;
                float[] oldCentroid = mean.mCentroid;
                mean.mCentroid = new float[oldCentroid.length];
                for (j = 0; j < mean.mClosestItems.size(); j++) {
                    for (int p = 0; p < mean.mCentroid.length; p++) {
                        float[] fArr = mean.mCentroid;
                        fArr[p] = ((float[]) mean.mClosestItems.get(j))[p] + fArr[p];
                    }
                }
                for (j = 0; j < mean.mCentroid.length; j++) {
                    float[] fArr2 = mean.mCentroid;
                    fArr2[j] = fArr2[j] / ((float) mean.mClosestItems.size());
                }
                if (sqDistance(oldCentroid, mean.mCentroid) > this.mSqConvergenceEpsilon) {
                    converged = false;
                }
            }
        }
        return converged;
    }

    public static Mean nearestMean(float[] point, List<Mean> means) {
        Mean nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        int meanCount = means.size();
        for (int i = 0; i < meanCount; i++) {
            Mean next = (Mean) means.get(i);
            float nextDistance = sqDistance(point, next.mCentroid);
            if (nextDistance < nearestDistance) {
                nearest = next;
                nearestDistance = nextDistance;
            }
        }
        return nearest;
    }

    public static float sqDistance(float[] a, float[] b) {
        float dist = 0.0f;
        for (int i = 0; i < a.length; i++) {
            dist += (a[i] - b[i]) * (a[i] - b[i]);
        }
        return dist;
    }
}
