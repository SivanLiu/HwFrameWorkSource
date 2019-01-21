package android.animation;

public class ArgbEvaluator implements TypeEvaluator {
    private static final ArgbEvaluator sInstance = new ArgbEvaluator();

    public static ArgbEvaluator getInstance() {
        return sInstance;
    }

    public Object evaluate(float fraction, Object startValue, Object endValue) {
        int startInt = ((Integer) startValue).intValue();
        float startA = ((float) ((startInt >> 24) & 255)) / 255.0f;
        float startR = ((float) ((startInt >> 16) & 255)) / 255.0f;
        float startG = ((float) ((startInt >> 8) & 255)) / 255.0f;
        float startB = ((float) (startInt & 255)) / 255.0f;
        int endInt = ((Integer) endValue).intValue();
        float endA = ((float) ((endInt >> 24) & 255)) / 255.0f;
        float endB = ((float) (endInt & 255)) / 255.0f;
        float startR2 = (float) Math.pow((double) startR, 2.2d);
        float startG2 = (float) Math.pow((double) startG, 2.2d);
        float startB2 = (float) Math.pow((double) startB, 2.2d);
        startG = (float) Math.pow((double) (((float) ((endInt >> 16) & 255)) / 255.0f), 2.2d);
        return Integer.valueOf((((Math.round((((endA - startA) * fraction) + startA) * 255.0f) << 24) | (Math.round(((float) Math.pow((double) (((startG - startR2) * fraction) + startR2), 0.45454545454545453d)) * 255.0f) << 16)) | (Math.round(((float) Math.pow((double) (((((float) Math.pow((double) (((float) ((endInt >> 8) & 255)) / 255.0f), 2.2d)) - startG2) * fraction) + startG2), 0.45454545454545453d)) * 255.0f) << 8)) | Math.round(((float) Math.pow((double) (((((float) Math.pow((double) endB, 2.2d)) - startB2) * fraction) + startB2), 0.45454545454545453d)) * 255.0f));
    }
}
