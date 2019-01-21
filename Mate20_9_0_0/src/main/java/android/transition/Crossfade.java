package android.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.RectEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.rms.AppAssociate;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOverlay;
import java.util.Map;

public class Crossfade extends Transition {
    public static final int FADE_BEHAVIOR_CROSSFADE = 0;
    public static final int FADE_BEHAVIOR_OUT_IN = 2;
    public static final int FADE_BEHAVIOR_REVEAL = 1;
    private static final String LOG_TAG = "Crossfade";
    private static final String PROPNAME_BITMAP = "android:crossfade:bitmap";
    private static final String PROPNAME_BOUNDS = "android:crossfade:bounds";
    private static final String PROPNAME_DRAWABLE = "android:crossfade:drawable";
    public static final int RESIZE_BEHAVIOR_NONE = 0;
    public static final int RESIZE_BEHAVIOR_SCALE = 1;
    private static RectEvaluator sRectEvaluator = new RectEvaluator();
    private int mFadeBehavior = 1;
    private int mResizeBehavior = 1;

    public Crossfade setFadeBehavior(int fadeBehavior) {
        if (fadeBehavior >= 0 && fadeBehavior <= 2) {
            this.mFadeBehavior = fadeBehavior;
        }
        return this;
    }

    public int getFadeBehavior() {
        return this.mFadeBehavior;
    }

    public Crossfade setResizeBehavior(int resizeBehavior) {
        if (resizeBehavior >= 0 && resizeBehavior <= 1) {
            this.mResizeBehavior = resizeBehavior;
        }
        return this;
    }

    public int getResizeBehavior() {
        return this.mResizeBehavior;
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x0082, element type: int, insn element type: null */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        TransitionValues transitionValues = startValues;
        TransitionValues transitionValues2 = endValues;
        if (transitionValues == null || transitionValues2 == null) {
            return null;
        }
        boolean useParentOverlay = this.mFadeBehavior != 1;
        final View view = transitionValues2.view;
        Map<String, Object> startVals = transitionValues.values;
        Map<String, Object> endVals = transitionValues2.values;
        Rect startBounds = (Rect) startVals.get(PROPNAME_BOUNDS);
        Rect endBounds = (Rect) endVals.get(PROPNAME_BOUNDS);
        Bitmap startBitmap = (Bitmap) startVals.get(PROPNAME_BITMAP);
        Bitmap endBitmap = (Bitmap) endVals.get(PROPNAME_BITMAP);
        final BitmapDrawable startDrawable = (BitmapDrawable) startVals.get(PROPNAME_DRAWABLE);
        BitmapDrawable endDrawable = (BitmapDrawable) endVals.get(PROPNAME_DRAWABLE);
        Rect rect;
        if (startDrawable == null || endDrawable == null || startBitmap.sameAs(endBitmap)) {
            Bitmap bitmap = endBitmap;
            Bitmap bitmap2 = startBitmap;
            rect = endBounds;
            return null;
        }
        ObjectAnimator anim;
        BitmapDrawable endDrawable2;
        ViewOverlay overlay = useParentOverlay ? ((ViewGroup) view.getParent()).getOverlay() : view.getOverlay();
        if (this.mFadeBehavior == 1) {
            overlay.add(endDrawable);
        }
        overlay.add(startDrawable);
        ViewOverlay overlay2;
        if (this.mFadeBehavior == 2) {
            overlay2 = overlay;
            anim = ObjectAnimator.ofInt(startDrawable, AppAssociate.ASSOC_WINDOW_ALPHA, new int[]{255, 0, 0});
        } else {
            overlay2 = overlay;
            anim = ObjectAnimator.ofInt(startDrawable, AppAssociate.ASSOC_WINDOW_ALPHA, new int[]{0});
        }
        ObjectAnimator anim2 = anim;
        anim2.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                view.invalidate(startDrawable.getBounds());
            }
        });
        ObjectAnimator anim1 = null;
        if (this.mFadeBehavior == 2) {
            anim1 = ObjectAnimator.ofFloat(view, View.ALPHA, new float[]{0.0f, 0.0f, 1.0f});
            endDrawable2 = endDrawable;
        } else if (this.mFadeBehavior == 0) {
            endDrawable2 = endDrawable;
            anim1 = ObjectAnimator.ofFloat(view, View.ALPHA, new float[]{0.0f, 1.0f});
        } else {
            endDrawable2 = endDrawable;
        }
        BitmapDrawable endDrawable3 = endDrawable2;
        BitmapDrawable startDrawable2 = startDrawable;
        startDrawable = useParentOverlay;
        final View view2 = view;
        final BitmapDrawable bitmapDrawable = startDrawable2;
        rect = endBounds;
        final BitmapDrawable endBounds2 = endDrawable3;
        anim2.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ViewOverlay overlay = startDrawable ? ((ViewGroup) view2.getParent()).getOverlay() : view2.getOverlay();
                overlay.remove(bitmapDrawable);
                if (Crossfade.this.mFadeBehavior == 1) {
                    overlay.remove(endBounds2);
                }
            }
        });
        AnimatorSet set = new AnimatorSet();
        set.playTogether(new Animator[]{anim2});
        if (anim1 != null) {
            set.playTogether(new Animator[]{anim1});
        }
        if (this.mResizeBehavior == 1 && startBounds.equals(rect) == null) {
            startDrawable = ObjectAnimator.ofObject(startDrawable2, "bounds", sRectEvaluator, new Object[]{startBounds, rect});
            set.playTogether(new Animator[]{startDrawable});
            if (this.mResizeBehavior == 1) {
                endDrawable = ObjectAnimator.ofObject(endDrawable3, "bounds", sRectEvaluator, new Object[]{startBounds, rect});
                set.playTogether(new Animator[]{endDrawable});
            }
        } else {
            bitmapDrawable = endDrawable3;
            BitmapDrawable bitmapDrawable2 = startDrawable2;
        }
        return set;
    }

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
        Rect bounds = new Rect(0, 0, view.getWidth(), view.getHeight());
        if (this.mFadeBehavior != 1) {
            bounds.offset(view.getLeft(), view.getTop());
        }
        transitionValues.values.put(PROPNAME_BOUNDS, bounds);
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Config.ARGB_8888);
        if (view instanceof TextureView) {
            bitmap = ((TextureView) view).getBitmap();
        } else {
            view.draw(new Canvas(bitmap));
        }
        transitionValues.values.put(PROPNAME_BITMAP, bitmap);
        BitmapDrawable drawable = new BitmapDrawable(bitmap);
        drawable.setBounds(bounds);
        transitionValues.values.put(PROPNAME_DRAWABLE, drawable);
    }

    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }
}
