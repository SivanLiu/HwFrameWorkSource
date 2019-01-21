package android.gesture;

class Instance {
    private static final float[] ORIENTATIONS = new float[]{0.0f, 0.7853982f, 1.5707964f, 2.3561945f, 3.1415927f, 0.0f, -0.7853982f, -1.5707964f, -2.3561945f, -3.1415927f};
    private static final int PATCH_SAMPLE_SIZE = 16;
    private static final int SEQUENCE_SAMPLE_SIZE = 16;
    final long id;
    final String label;
    final float[] vector;

    private Instance(long id, float[] sample, String sampleName) {
        this.id = id;
        this.vector = sample;
        this.label = sampleName;
    }

    private void normalize() {
        float[] sample = this.vector;
        int size = sample.length;
        int i = 0;
        float sum = 0.0f;
        for (int i2 = 0; i2 < size; i2++) {
            sum += sample[i2] * sample[i2];
        }
        float magnitude = (float) Math.sqrt((double) sum);
        while (i < size) {
            sample[i] = sample[i] / magnitude;
            i++;
        }
    }

    static Instance createInstance(int sequenceType, int orientationType, Gesture gesture, String label) {
        if (sequenceType == 2) {
            Instance instance = new Instance(gesture.getID(), temporalSampler(orientationType, gesture), label);
            instance.normalize();
            return instance;
        }
        return new Instance(gesture.getID(), spatialSampler(gesture), label);
    }

    private static float[] spatialSampler(Gesture gesture) {
        return GestureUtils.spatialSampling(gesture, 16, false);
    }

    private static float[] temporalSampler(int orientationType, Gesture gesture) {
        float[] pts = GestureUtils.temporalSampling((GestureStroke) gesture.getStrokes().get(0), 16);
        float[] center = GestureUtils.computeCentroid(pts);
        float orientation = (float) Math.atan2((double) (pts[1] - center[1]), (double) (pts[0] - center[0]));
        float adjustment = -orientation;
        if (orientationType != 1) {
            float adjustment2 = adjustment;
            for (float delta : ORIENTATIONS) {
                float delta2 = delta2 - orientation;
                if (Math.abs(delta2) < Math.abs(adjustment2)) {
                    adjustment2 = delta2;
                }
            }
            adjustment = adjustment2;
        }
        GestureUtils.translate(pts, -center[0], -center[1]);
        GestureUtils.rotate(pts, adjustment);
        return pts;
    }
}
