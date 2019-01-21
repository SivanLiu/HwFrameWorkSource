package android.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.RectEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.BrowserContract.Bookmarks;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;
import java.util.Map;

public class ChangeBounds extends Transition {
    private static final Property<View, PointF> BOTTOM_RIGHT_ONLY_PROPERTY = new Property<View, PointF>(PointF.class, "bottomRight") {
        public void set(View view, PointF bottomRight) {
            view.setLeftTopRightBottom(view.getLeft(), view.getTop(), Math.round(bottomRight.x), Math.round(bottomRight.y));
        }

        public PointF get(View view) {
            return null;
        }
    };
    private static final Property<ViewBounds, PointF> BOTTOM_RIGHT_PROPERTY = new Property<ViewBounds, PointF>(PointF.class, "bottomRight") {
        public void set(ViewBounds viewBounds, PointF bottomRight) {
            viewBounds.setBottomRight(bottomRight);
        }

        public PointF get(ViewBounds viewBounds) {
            return null;
        }
    };
    private static final Property<Drawable, PointF> DRAWABLE_ORIGIN_PROPERTY = new Property<Drawable, PointF>(PointF.class, "boundsOrigin") {
        private Rect mBounds = new Rect();

        public void set(Drawable object, PointF value) {
            object.copyBounds(this.mBounds);
            this.mBounds.offsetTo(Math.round(value.x), Math.round(value.y));
            object.setBounds(this.mBounds);
        }

        public PointF get(Drawable object) {
            object.copyBounds(this.mBounds);
            return new PointF((float) this.mBounds.left, (float) this.mBounds.top);
        }
    };
    private static final String LOG_TAG = "ChangeBounds";
    private static final Property<View, PointF> POSITION_PROPERTY = new Property<View, PointF>(PointF.class, Bookmarks.POSITION) {
        public void set(View view, PointF topLeft) {
            int left = Math.round(topLeft.x);
            int top = Math.round(topLeft.y);
            view.setLeftTopRightBottom(left, top, view.getWidth() + left, view.getHeight() + top);
        }

        public PointF get(View view) {
            return null;
        }
    };
    private static final String PROPNAME_BOUNDS = "android:changeBounds:bounds";
    private static final String PROPNAME_CLIP = "android:changeBounds:clip";
    private static final String PROPNAME_PARENT = "android:changeBounds:parent";
    private static final String PROPNAME_WINDOW_X = "android:changeBounds:windowX";
    private static final String PROPNAME_WINDOW_Y = "android:changeBounds:windowY";
    private static final Property<View, PointF> TOP_LEFT_ONLY_PROPERTY = new Property<View, PointF>(PointF.class, "topLeft") {
        public void set(View view, PointF topLeft) {
            view.setLeftTopRightBottom(Math.round(topLeft.x), Math.round(topLeft.y), view.getRight(), view.getBottom());
        }

        public PointF get(View view) {
            return null;
        }
    };
    private static final Property<ViewBounds, PointF> TOP_LEFT_PROPERTY = new Property<ViewBounds, PointF>(PointF.class, "topLeft") {
        public void set(ViewBounds viewBounds, PointF topLeft) {
            viewBounds.setTopLeft(topLeft);
        }

        public PointF get(ViewBounds viewBounds) {
            return null;
        }
    };
    private static RectEvaluator sRectEvaluator = new RectEvaluator();
    private static final String[] sTransitionProperties = new String[]{PROPNAME_BOUNDS, PROPNAME_CLIP, PROPNAME_PARENT, PROPNAME_WINDOW_X, PROPNAME_WINDOW_Y};
    boolean mReparent;
    boolean mResizeClip;
    int[] tempLocation;

    private static class ViewBounds {
        private int mBottom;
        private int mBottomRightCalls;
        private int mLeft;
        private int mRight;
        private int mTop;
        private int mTopLeftCalls;
        private View mView;

        public ViewBounds(View view) {
            this.mView = view;
        }

        public void setTopLeft(PointF topLeft) {
            this.mLeft = Math.round(topLeft.x);
            this.mTop = Math.round(topLeft.y);
            this.mTopLeftCalls++;
            if (this.mTopLeftCalls == this.mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        public void setBottomRight(PointF bottomRight) {
            this.mRight = Math.round(bottomRight.x);
            this.mBottom = Math.round(bottomRight.y);
            this.mBottomRightCalls++;
            if (this.mTopLeftCalls == this.mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        private void setLeftTopRightBottom() {
            this.mView.setLeftTopRightBottom(this.mLeft, this.mTop, this.mRight, this.mBottom);
            this.mTopLeftCalls = 0;
            this.mBottomRightCalls = 0;
        }
    }

    public ChangeBounds() {
        this.tempLocation = new int[2];
        this.mResizeClip = false;
        this.mReparent = false;
    }

    public ChangeBounds(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.tempLocation = new int[2];
        this.mResizeClip = false;
        this.mReparent = false;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChangeBounds);
        boolean resizeClip = a.getBoolean(0, false);
        a.recycle();
        setResizeClip(resizeClip);
    }

    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    public void setResizeClip(boolean resizeClip) {
        this.mResizeClip = resizeClip;
    }

    public boolean getResizeClip() {
        return this.mResizeClip;
    }

    @Deprecated
    public void setReparent(boolean reparent) {
        this.mReparent = reparent;
    }

    private void captureValues(TransitionValues values) {
        View view = values.view;
        if (view.isLaidOut() || view.getWidth() != 0 || view.getHeight() != 0) {
            values.values.put(PROPNAME_BOUNDS, new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
            values.values.put(PROPNAME_PARENT, values.view.getParent());
            if (this.mReparent) {
                values.view.getLocationInWindow(this.tempLocation);
                values.values.put(PROPNAME_WINDOW_X, Integer.valueOf(this.tempLocation[0]));
                values.values.put(PROPNAME_WINDOW_Y, Integer.valueOf(this.tempLocation[1]));
            }
            if (this.mResizeClip) {
                values.values.put(PROPNAME_CLIP, view.getClipBounds());
            }
        }
    }

    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private boolean parentMatches(View startParent, View endParent) {
        if (!this.mReparent) {
            return true;
        }
        boolean z = true;
        TransitionValues endValues = getMatchedTransitionValues(startParent, true);
        if (endValues == null) {
            if (startParent != endParent) {
                z = false;
            }
            return z;
        }
        if (endParent != endValues.view) {
            z = false;
        }
        return z;
    }

    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        TransitionValues transitionValues = startValues;
        TransitionValues transitionValues2 = endValues;
        TransitionValues transitionValues3;
        if (transitionValues == null || transitionValues2 == null) {
            transitionValues3 = transitionValues2;
            return null;
        }
        Map<String, Object> startParentVals = transitionValues.values;
        Map<String, Object> endParentVals = transitionValues2.values;
        ViewGroup startParent = (ViewGroup) startParentVals.get(PROPNAME_PARENT);
        ViewGroup endParent = (ViewGroup) endParentVals.get(PROPNAME_PARENT);
        Map<String, Object> map;
        ViewGroup viewGroup;
        ViewGroup viewGroup2;
        if (startParent == null) {
            map = endParentVals;
            viewGroup = startParent;
            viewGroup2 = endParent;
            transitionValues3 = transitionValues2;
        } else if (endParent == null) {
            Map<String, Object> map2 = startParentVals;
            map = endParentVals;
            viewGroup = startParent;
            viewGroup2 = endParent;
            transitionValues3 = transitionValues2;
        } else {
            View view = transitionValues2.view;
            int startLeft;
            int numChanges;
            ChangeBounds endBottom;
            View view2;
            int startWidth;
            ViewGroup viewGroup3;
            TransitionValues transitionValues4;
            if (parentMatches(startParent, endParent)) {
                Rect startBounds = (Rect) transitionValues.values.get(PROPNAME_BOUNDS);
                Rect endBounds = (Rect) transitionValues2.values.get(PROPNAME_BOUNDS);
                startLeft = startBounds.left;
                int endLeft = endBounds.left;
                int startTop = startBounds.top;
                int endTop = endBounds.top;
                int startRight = startBounds.right;
                int endRight = endBounds.right;
                startParentVals = startBounds.bottom;
                int endBottom2 = endBounds.bottom;
                int startWidth2 = startRight - startLeft;
                endParent = startParentVals - startTop;
                int endWidth = endRight - endLeft;
                int endHeight = endBottom2 - endTop;
                Rect startClip = (Rect) transitionValues.values.get(PROPNAME_CLIP);
                Rect endClip = (Rect) transitionValues2.values.get(PROPNAME_CLIP);
                numChanges = 0;
                if (!((startWidth2 == 0 || endParent == null) && (endWidth == 0 || endHeight == 0))) {
                    if (!(startLeft == endLeft && startTop == endTop)) {
                        numChanges = 0 + 1;
                    }
                    if (!(startRight == endRight && startParentVals == endBottom2)) {
                        numChanges++;
                    }
                }
                if (!(startClip == null || startClip.equals(endClip)) || (startClip == null && endClip != null)) {
                    numChanges++;
                }
                if (numChanges > 0) {
                    Rect endClip2;
                    int endBottom3;
                    Animator anim;
                    Rect startClip2 = startClip;
                    if (view.getParent() instanceof ViewGroup) {
                        final ViewGroup parent = (ViewGroup) view.getParent();
                        endClip2 = endClip;
                        parent.suppressLayout(true);
                        endBottom3 = endBottom2;
                        endBottom = this;
                        endBottom.addListener(new TransitionListenerAdapter() {
                            boolean mCanceled = false;

                            public void onTransitionCancel(Transition transition) {
                                parent.suppressLayout(false);
                                this.mCanceled = true;
                            }

                            public void onTransitionEnd(Transition transition) {
                                if (!this.mCanceled) {
                                    parent.suppressLayout(false);
                                }
                                transition.removeListener(this);
                            }

                            public void onTransitionPause(Transition transition) {
                                parent.suppressLayout(false);
                            }

                            public void onTransitionResume(Transition transition) {
                                parent.suppressLayout(true);
                            }
                        });
                    } else {
                        endClip2 = endClip;
                        endBottom3 = endBottom2;
                        endBottom = this;
                    }
                    int i;
                    int i2;
                    int startWidth3;
                    int startHeight;
                    int i3;
                    int i4;
                    int i5;
                    Map<String, Object> map3;
                    int i6;
                    int i7;
                    int i8;
                    if (endBottom.mResizeClip) {
                        int endLeft2;
                        int startTop2;
                        int startLeft2;
                        Rect startClip3;
                        Rect endClip3;
                        ObjectAnimator clipAnimator;
                        i = endWidth;
                        i2 = numChanges;
                        startWidth3 = startWidth2;
                        startHeight = endParent;
                        view2 = view;
                        startParent = endBottom3;
                        startWidth = startWidth3;
                        numChanges = Math.max(startWidth, endWidth);
                        int startRight2 = startRight;
                        int endRight2 = endRight;
                        view2.setLeftTopRightBottom(startLeft, startTop, startLeft + numChanges, startTop + Math.max(endParent, endHeight));
                        ObjectAnimator positionAnimator = null;
                        if (startLeft == endLeft && startTop == endTop) {
                            endLeft2 = endLeft;
                            startTop2 = startTop;
                            startLeft2 = startLeft;
                        } else {
                            ObjectAnimator positionAnimator2 = null;
                            startLeft2 = startLeft;
                            startTop2 = startTop;
                            endLeft2 = endLeft;
                            positionAnimator = ObjectAnimator.ofObject(view2, POSITION_PROPERTY, null, getPathMotion().getPath((float) startLeft, (float) startTop, (float) endLeft, (float) endTop));
                        }
                        ObjectAnimator positionAnimator3 = positionAnimator;
                        i3 = startTop2;
                        final Rect finalClip = endClip2;
                        if (startClip2 == null) {
                            endLeft = 0;
                            startClip3 = new Rect(0, 0, startWidth, endParent);
                        } else {
                            endLeft = 0;
                            startClip3 = startClip2;
                        }
                        if (endClip2 == null) {
                            endClip3 = new Rect(endLeft, endLeft, endWidth, endHeight);
                        } else {
                            endClip3 = endClip2;
                        }
                        int i9;
                        if (startClip3.equals(endClip3)) {
                            endClip2 = endClip3;
                            i4 = endTop;
                            startClip2 = startClip3;
                            i9 = endHeight;
                            startHeight = endWidth;
                            i5 = startWidth;
                            int i10 = numChanges;
                            map3 = startParentVals;
                            i6 = startRight2;
                            i7 = endRight2;
                            endBottom3 = startLeft2;
                            i8 = endLeft2;
                            startParentVals = positionAnimator3;
                            clipAnimator = null;
                        } else {
                            view2.setClipBounds(startClip3);
                            ObjectAnimator clipAnimator2 = null;
                            int endTop2 = endTop;
                            ObjectAnimator positionAnimator4 = positionAnimator3;
                            positionAnimator3 = ObjectAnimator.ofObject(view2, "clipBounds", sRectEvaluator, new Object[]{startClip3, endClip3});
                            i6 = startRight2;
                            endClip2 = endClip3;
                            i5 = startWidth;
                            AnonymousClass9 anonymousClass9 = r0;
                            final View view3 = view2;
                            startClip2 = startClip3;
                            endRight = endLeft2;
                            map3 = startParentVals;
                            startParentVals = positionAnimator4;
                            clipAnimator = positionAnimator3;
                            positionAnimator3 = endTop2;
                            i9 = endHeight;
                            endHeight = endRight2;
                            endWidth = startParent;
                            AnonymousClass9 anonymousClass92 = new AnimatorListenerAdapter() {
                                private boolean mIsCanceled;

                                public void onAnimationCancel(Animator animation) {
                                    this.mIsCanceled = true;
                                }

                                public void onAnimationEnd(Animator animation) {
                                    if (!this.mIsCanceled) {
                                        view3.setClipBounds(finalClip);
                                        view3.setLeftTopRightBottom(endRight, positionAnimator3, endHeight, endWidth);
                                    }
                                }
                            };
                            clipAnimator.addListener(anonymousClass9);
                        }
                        anim = TransitionUtils.mergeAnimators(startParentVals, clipAnimator);
                    } else {
                        view.setLeftTopRightBottom(startLeft, startTop, startRight, startParentVals);
                        View view4;
                        int startBottom;
                        if (numChanges != 2) {
                            i = endWidth;
                            i2 = numChanges;
                            startWidth3 = startWidth2;
                            startHeight = endParent;
                            view4 = view;
                            startWidth2 = endBottom3;
                            if (startLeft != endLeft) {
                                view2 = view4;
                            } else if (startTop != endTop) {
                                view2 = view4;
                            } else {
                                view2 = view4;
                                anim = ObjectAnimator.ofObject(view2, BOTTOM_RIGHT_ONLY_PROPERTY, null, getPathMotion().getPath((float) startRight, (float) startParentVals, (float) endRight, (float) startWidth2));
                                endBottom3 = startLeft;
                                startBottom = startParentVals;
                                endParent = startHeight;
                                startHeight = i;
                                i5 = startWidth3;
                            }
                            anim = ObjectAnimator.ofObject(view2, TOP_LEFT_ONLY_PROPERTY, null, getPathMotion().getPath((float) startLeft, (float) startTop, (float) endLeft, (float) endTop));
                            endBottom3 = startLeft;
                            startBottom = startParentVals;
                            endParent = startHeight;
                            startHeight = i;
                            i5 = startWidth3;
                        } else if (startWidth2 == endWidth && endParent == endHeight) {
                            startHeight = endParent;
                            anim = ObjectAnimator.ofObject(view, POSITION_PROPERTY, null, getPathMotion().getPath((float) startLeft, (float) startTop, (float) endLeft, (float) endTop));
                            i6 = startRight;
                            i8 = endLeft;
                            i4 = endTop;
                            i3 = startTop;
                            i7 = endRight;
                            map3 = startParentVals;
                            i5 = startWidth2;
                            view2 = view;
                            startWidth2 = endBottom3;
                            endParent = startHeight;
                            startHeight = endWidth;
                        } else {
                            i2 = numChanges;
                            startHeight = endParent;
                            endHeight = new ViewBounds(view);
                            i = endWidth;
                            Path topLeftPath = getPathMotion().getPath((float) startLeft, (float) startTop, (float) endLeft, (float) endTop);
                            ObjectAnimator topLeftAnimator = ObjectAnimator.ofObject(endHeight, TOP_LEFT_PROPERTY, null, topLeftPath);
                            startWidth3 = startWidth2;
                            view4 = view;
                            ObjectAnimator bottomRightAnimator = ObjectAnimator.ofObject(endHeight, BOTTOM_RIGHT_PROPERTY, null, getPathMotion().getPath((float) startRight, (float) startParentVals, (float) endRight, (float) endBottom3));
                            numChanges = new AnimatorSet();
                            numChanges.playTogether(new Animator[]{topLeftAnimator, bottomRightAnimator});
                            endParent = numChanges;
                            numChanges.addListener(new AnimatorListenerAdapter() {
                                private ViewBounds mViewBounds = endHeight;
                            });
                            i6 = startRight;
                            startBottom = startParentVals;
                            anim = endParent;
                            endParent = startHeight;
                            startHeight = i;
                            view2 = view4;
                        }
                    }
                    return anim;
                }
                viewGroup3 = sceneRoot;
                transitionValues4 = startValues;
                transitionValues3 = endValues;
            } else {
                map = endParentVals;
                viewGroup = startParent;
                viewGroup2 = endParent;
                view2 = view;
                viewGroup3 = sceneRoot;
                viewGroup3.getLocationInWindow(this.tempLocation);
                transitionValues4 = startValues;
                startWidth = ((Integer) transitionValues4.values.get(PROPNAME_WINDOW_X)).intValue() - this.tempLocation[0];
                numChanges = ((Integer) transitionValues4.values.get(PROPNAME_WINDOW_Y)).intValue() - this.tempLocation[1];
                transitionValues3 = endValues;
                startParent = ((Integer) transitionValues3.values.get(PROPNAME_WINDOW_X)).intValue() - this.tempLocation[0];
                endParent = ((Integer) transitionValues3.values.get(PROPNAME_WINDOW_Y)).intValue() - this.tempLocation[1];
                if (!(startWidth == startParent && numChanges == endParent)) {
                    int width = view2.getWidth();
                    startLeft = view2.getHeight();
                    Bitmap bitmap = Bitmap.createBitmap(width, startLeft, Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    view2.draw(canvas);
                    BitmapDrawable drawable = new BitmapDrawable(bitmap);
                    drawable.setBounds(startWidth, numChanges, startWidth + width, numChanges + startLeft);
                    float transitionAlpha = view2.getTransitionAlpha();
                    view2.setTransitionAlpha(0.0f);
                    sceneRoot.getOverlay().add(drawable);
                    Canvas canvas2 = canvas;
                    Bitmap bitmap2 = bitmap;
                    int height = startLeft;
                    Path topLeftPath2 = getPathMotion().getPath((float) startWidth, (float) numChanges, (float) startParent, (float) endParent);
                    PropertyValuesHolder origin = PropertyValuesHolder.ofObject(DRAWABLE_ORIGIN_PROPERTY, null, topLeftPath2);
                    ObjectAnimator anim2 = ObjectAnimator.ofPropertyValuesHolder(drawable, new PropertyValuesHolder[]{origin});
                    BitmapDrawable drawable2 = drawable;
                    final ViewGroup viewGroup4 = viewGroup3;
                    AnonymousClass10 anonymousClass10 = r0;
                    final BitmapDrawable bitmapDrawable = drawable2;
                    ObjectAnimator anim3 = anim2;
                    final View view5 = view2;
                    topLeftPath2 = transitionAlpha;
                    AnonymousClass10 anonymousClass102 = new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            viewGroup4.getOverlay().remove(bitmapDrawable);
                            view5.setTransitionAlpha(topLeftPath2);
                        }
                    };
                    anim3.addListener(anonymousClass10);
                    return anim3;
                }
            }
            return null;
        }
        return null;
    }
}
