package android.transition;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TypeEvaluator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class TransitionUtils {
    private static int MAX_IMAGE_SIZE = 1048576;

    public static class MatrixEvaluator implements TypeEvaluator<Matrix> {
        float[] mTempEndValues = new float[9];
        Matrix mTempMatrix = new Matrix();
        float[] mTempStartValues = new float[9];

        public Matrix evaluate(float fraction, Matrix startValue, Matrix endValue) {
            startValue.getValues(this.mTempStartValues);
            endValue.getValues(this.mTempEndValues);
            for (int i = 0; i < 9; i++) {
                this.mTempEndValues[i] = this.mTempStartValues[i] + (fraction * (this.mTempEndValues[i] - this.mTempStartValues[i]));
            }
            this.mTempMatrix.setValues(this.mTempEndValues);
            return this.mTempMatrix;
        }
    }

    static Animator mergeAnimators(Animator animator1, Animator animator2) {
        if (animator1 == null) {
            return animator2;
        }
        if (animator2 == null) {
            return animator1;
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(new Animator[]{animator1, animator2});
        return animatorSet;
    }

    public static Transition mergeTransitions(Transition... transitions) {
        int i = 0;
        int nonNullIndex = -1;
        int count = 0;
        for (int i2 = 0; i2 < transitions.length; i2++) {
            if (transitions[i2] != null) {
                count++;
                nonNullIndex = i2;
            }
        }
        if (count == 0) {
            return null;
        }
        if (count == 1) {
            return transitions[nonNullIndex];
        }
        TransitionSet transitionSet = new TransitionSet();
        while (i < transitions.length) {
            if (transitions[i] != null) {
                transitionSet.addTransition(transitions[i]);
            }
            i++;
        }
        return transitionSet;
    }

    public static View copyViewImage(ViewGroup sceneRoot, View view, View parent) {
        Matrix matrix = new Matrix();
        matrix.setTranslate((float) (-parent.getScrollX()), (float) (-parent.getScrollY()));
        view.transformMatrixToGlobal(matrix);
        sceneRoot.transformMatrixToLocal(matrix);
        RectF bounds = new RectF(0.0f, 0.0f, (float) view.getWidth(), (float) view.getHeight());
        matrix.mapRect(bounds);
        int left = Math.round(bounds.left);
        int top = Math.round(bounds.top);
        int right = Math.round(bounds.right);
        int bottom = Math.round(bounds.bottom);
        ImageView copy = new ImageView(view.getContext());
        copy.setScaleType(ScaleType.CENTER_CROP);
        Bitmap bitmap = createViewBitmap(view, matrix, bounds, sceneRoot);
        if (bitmap != null) {
            copy.setImageBitmap(bitmap);
        }
        copy.measure(MeasureSpec.makeMeasureSpec(right - left, 1073741824), MeasureSpec.makeMeasureSpec(bottom - top, 1073741824));
        copy.layout(left, top, right, bottom);
        return copy;
    }

    public static Bitmap createDrawableBitmap(Drawable drawable, View hostView) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        float scale = Math.min(1.0f, ((float) MAX_IMAGE_SIZE) / ((float) (width * height)));
        if ((drawable instanceof BitmapDrawable) && scale == 1.0f) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        int bitmapWidth = (int) (((float) width) * scale);
        int bitmapHeight = (int) (((float) height) * scale);
        Picture picture = new Picture();
        Canvas canvas = picture.beginRecording(width, height);
        Rect existingBounds = drawable.getBounds();
        int left = existingBounds.left;
        int top = existingBounds.top;
        int right = existingBounds.right;
        int bottom = existingBounds.bottom;
        drawable.setBounds(0, 0, bitmapWidth, bitmapHeight);
        drawable.draw(canvas);
        drawable.setBounds(left, top, right, bottom);
        picture.endRecording();
        return Bitmap.createBitmap(picture);
    }

    public static Bitmap createViewBitmap(View view, Matrix matrix, RectF bounds, ViewGroup sceneRoot) {
        boolean addToOverlay = view.isAttachedToWindow() ^ 1;
        ViewGroup parent = null;
        int indexInParent = 0;
        if (addToOverlay) {
            if (sceneRoot == null || !sceneRoot.isAttachedToWindow()) {
                return null;
            }
            parent = (ViewGroup) view.getParent();
            indexInParent = parent.indexOfChild(view);
            sceneRoot.getOverlay().add(view);
        }
        Bitmap bitmap = null;
        int bitmapWidth = Math.round(bounds.width());
        int bitmapHeight = Math.round(bounds.height());
        if (bitmapWidth > 0 && bitmapHeight > 0) {
            float scale = Math.min(1.0f, ((float) MAX_IMAGE_SIZE) / ((float) (bitmapWidth * bitmapHeight)));
            bitmapWidth = (int) (((float) bitmapWidth) * scale);
            bitmapHeight = (int) (((float) bitmapHeight) * scale);
            matrix.postTranslate(-bounds.left, -bounds.top);
            matrix.postScale(scale, scale);
            Picture picture = new Picture();
            Canvas canvas = picture.beginRecording(bitmapWidth, bitmapHeight);
            canvas.concat(matrix);
            view.draw(canvas);
            picture.endRecording();
            bitmap = Bitmap.createBitmap(picture);
        }
        if (addToOverlay) {
            sceneRoot.getOverlay().remove(view);
            parent.addView(view, indexInParent);
        }
        return bitmap;
    }
}
