package com.android.internal.graphics.palette;

import com.android.internal.graphics.ColorUtils;
import com.android.internal.graphics.palette.Palette.Filter;
import com.android.internal.graphics.palette.Palette.Swatch;
import com.android.internal.ml.clustering.KMeans;
import com.android.internal.ml.clustering.KMeans.Mean;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VariationalKMeansQuantizer implements Quantizer {
    private static final boolean DEBUG = false;
    private static final String TAG = "KMeansQuantizer";
    private final int mInitializations;
    private final KMeans mKMeans;
    private final float mMinClusterSqDistance;
    private List<Swatch> mQuantizedColors;

    public VariationalKMeansQuantizer() {
        this(0.25f);
    }

    public VariationalKMeansQuantizer(float minClusterDistance) {
        this(minClusterDistance, 1);
    }

    public VariationalKMeansQuantizer(float minClusterDistance, int initializations) {
        this.mKMeans = new KMeans(new Random(0), 30, 0.0f);
        this.mMinClusterSqDistance = minClusterDistance * minClusterDistance;
        this.mInitializations = initializations;
    }

    public void quantize(int[] pixels, int maxColors, Filter[] filters) {
        int i;
        int i2 = 3;
        float[] hsl = new float[]{0.0f, 0.0f, 0.0f};
        float[][] hslPixels = (float[][]) Array.newInstance(Float.TYPE, new int[]{pixels.length, 3});
        for (i = 0; i < pixels.length; i++) {
            ColorUtils.colorToHSL(pixels[i], hsl);
            hslPixels[i][0] = hsl[0] / 360.0f;
            hslPixels[i][1] = hsl[1];
            hslPixels[i][2] = hsl[2];
        }
        List<Mean> optimalMeans = getOptimalKMeans(maxColors, hslPixels);
        for (i = 0; i < optimalMeans.size(); i++) {
            Mean current = (Mean) optimalMeans.get(i);
            float[] currentCentroid = current.getCentroid();
            int j = i + 1;
            while (j < optimalMeans.size()) {
                Mean compareTo = (Mean) optimalMeans.get(j);
                float[] compareToCentroid = compareTo.getCentroid();
                if (KMeans.sqDistance(currentCentroid, compareToCentroid) < this.mMinClusterSqDistance) {
                    optimalMeans.remove(compareTo);
                    current.getItems().addAll(compareTo.getItems());
                    for (int k = 0; k < currentCentroid.length; k++) {
                        currentCentroid[k] = (float) (((double) currentCentroid[k]) + (((double) (compareToCentroid[k] - currentCentroid[k])) / 2.0d));
                    }
                    j--;
                }
                j++;
            }
        }
        this.mQuantizedColors = new ArrayList();
        for (Mean mean : optimalMeans) {
            if (mean.getItems().size() != 0) {
                float[] centroid = mean.getCentroid();
                this.mQuantizedColors.add(new Swatch(new float[]{centroid[0] * 360.0f, centroid[1], centroid[2]}, mean.getItems().size()));
            }
        }
    }

    private List<Mean> getOptimalKMeans(int k, float[][] inputData) {
        List<Mean> optimal = null;
        double optimalScore = -1.7976931348623157E308d;
        for (int runs = this.mInitializations; runs > 0; runs--) {
            List<Mean> means = this.mKMeans.predict(k, inputData);
            double score = KMeans.score(means);
            if (optimal == null || score > optimalScore) {
                optimalScore = score;
                optimal = means;
            }
        }
        return optimal;
    }

    public List<Swatch> getQuantizedColors() {
        return this.mQuantizedColors;
    }
}
