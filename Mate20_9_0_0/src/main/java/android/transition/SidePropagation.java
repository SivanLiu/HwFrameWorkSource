package android.transition;

import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

public class SidePropagation extends VisibilityPropagation {
    private static final String TAG = "SlidePropagation";
    private float mPropagationSpeed = 3.0f;
    private int mSide = 80;

    public void setSide(int side) {
        this.mSide = side;
    }

    public void setPropagationSpeed(float propagationSpeed) {
        if (propagationSpeed != 0.0f) {
            this.mPropagationSpeed = propagationSpeed;
            return;
        }
        throw new IllegalArgumentException("propagationSpeed may not be 0");
    }

    public long getStartDelay(ViewGroup sceneRoot, Transition transition, TransitionValues startValues, TransitionValues endValues) {
        TransitionValues transitionValues = startValues;
        if (transitionValues == null && endValues == null) {
            return 0;
        }
        TransitionValues positionValues;
        int epicenterX;
        int epicenterY;
        int directionMultiplier = 1;
        Rect epicenter = transition.getEpicenter();
        if (endValues == null || getViewVisibility(transitionValues) == 0) {
            positionValues = transitionValues;
            directionMultiplier = -1;
        } else {
            positionValues = endValues;
        }
        int directionMultiplier2 = directionMultiplier;
        TransitionValues positionValues2 = positionValues;
        int viewCenterX = getViewX(positionValues2);
        int viewCenterY = getViewY(positionValues2);
        int[] loc = new int[2];
        View view = sceneRoot;
        view.getLocationOnScreen(loc);
        int left = loc[0] + Math.round(sceneRoot.getTranslationX());
        int top = loc[1] + Math.round(sceneRoot.getTranslationY());
        int right = left + sceneRoot.getWidth();
        int bottom = top + sceneRoot.getHeight();
        if (epicenter != null) {
            epicenterX = epicenter.centerX();
            epicenterY = epicenter.centerY();
        } else {
            epicenterX = (left + right) / 2;
            epicenterY = (top + bottom) / 2;
        }
        int directionMultiplier3 = directionMultiplier2;
        float distanceFraction = ((float) distance(view, viewCenterX, viewCenterY, epicenterX, epicenterY, left, top, right, bottom)) / ((float) getMaxDistance(sceneRoot));
        long duration = transition.getDuration();
        if (duration < 0) {
            duration = 300;
        }
        return (long) Math.round((((float) (((long) directionMultiplier3) * duration)) / this.mPropagationSpeed) * distanceFraction);
    }

    private int distance(View sceneRoot, int viewX, int viewY, int epicenterX, int epicenterY, int left, int top, int right, int bottom) {
        int side;
        boolean z = true;
        if (this.mSide == Gravity.START) {
            if (sceneRoot.getLayoutDirection() != 1) {
                z = false;
            }
            side = z ? 5 : 3;
        } else if (this.mSide == Gravity.END) {
            if (sceneRoot.getLayoutDirection() != 1) {
                z = false;
            }
            side = z ? 3 : 5;
        } else {
            side = this.mSide;
        }
        if (side == 3) {
            return (right - viewX) + Math.abs(epicenterY - viewY);
        }
        if (side == 5) {
            return (viewX - left) + Math.abs(epicenterY - viewY);
        }
        if (side == 48) {
            return (bottom - viewY) + Math.abs(epicenterX - viewX);
        }
        if (side != 80) {
            return 0;
        }
        return (viewY - top) + Math.abs(epicenterX - viewX);
    }

    private int getMaxDistance(ViewGroup sceneRoot) {
        int i = this.mSide;
        if (i == 3 || i == 5 || i == Gravity.START || i == Gravity.END) {
            return sceneRoot.getWidth();
        }
        return sceneRoot.getHeight();
    }
}
