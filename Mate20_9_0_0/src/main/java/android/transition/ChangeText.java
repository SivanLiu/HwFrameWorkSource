package android.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Map;

public class ChangeText extends Transition {
    public static final int CHANGE_BEHAVIOR_IN = 2;
    public static final int CHANGE_BEHAVIOR_KEEP = 0;
    public static final int CHANGE_BEHAVIOR_OUT = 1;
    public static final int CHANGE_BEHAVIOR_OUT_IN = 3;
    private static final String LOG_TAG = "TextChange";
    private static final String PROPNAME_TEXT = "android:textchange:text";
    private static final String PROPNAME_TEXT_COLOR = "android:textchange:textColor";
    private static final String PROPNAME_TEXT_SELECTION_END = "android:textchange:textSelectionEnd";
    private static final String PROPNAME_TEXT_SELECTION_START = "android:textchange:textSelectionStart";
    private static final String[] sTransitionProperties = new String[]{PROPNAME_TEXT, PROPNAME_TEXT_SELECTION_START, PROPNAME_TEXT_SELECTION_END};
    private int mChangeBehavior = 0;

    public ChangeText setChangeBehavior(int changeBehavior) {
        if (changeBehavior >= 0 && changeBehavior <= 3) {
            this.mChangeBehavior = changeBehavior;
        }
        return this;
    }

    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    public int getChangeBehavior() {
        return this.mChangeBehavior;
    }

    private void captureValues(TransitionValues transitionValues) {
        if (transitionValues.view instanceof TextView) {
            TextView textview = transitionValues.view;
            transitionValues.values.put(PROPNAME_TEXT, textview.getText());
            if (textview instanceof EditText) {
                transitionValues.values.put(PROPNAME_TEXT_SELECTION_START, Integer.valueOf(textview.getSelectionStart()));
                transitionValues.values.put(PROPNAME_TEXT_SELECTION_END, Integer.valueOf(textview.getSelectionEnd()));
            }
            if (this.mChangeBehavior > 0) {
                transitionValues.values.put(PROPNAME_TEXT_COLOR, Integer.valueOf(textview.getCurrentTextColor()));
            }
        }
    }

    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    /* JADX WARNING: Removed duplicated region for block: B:63:0x01b2  */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x01b0  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        TransitionValues transitionValues = startValues;
        TransitionValues transitionValues2 = endValues;
        if (transitionValues == null || transitionValues2 == null || !(transitionValues.view instanceof TextView) || !(transitionValues2.view instanceof TextView)) {
            return null;
        }
        int startSelectionStart;
        int endSelectionStart;
        int startSelectionEnd;
        int endSelectionEnd;
        final TextView view = (TextView) transitionValues2.view;
        Map<String, Object> startVals = transitionValues.values;
        Map<String, Object> endVals = transitionValues2.values;
        CharSequence startText = startVals.get(PROPNAME_TEXT) != null ? (CharSequence) startVals.get(PROPNAME_TEXT) : "";
        CharSequence endText = endVals.get(PROPNAME_TEXT) != null ? (CharSequence) endVals.get(PROPNAME_TEXT) : "";
        int endSelectionStart2 = -1;
        if (view instanceof EditText) {
            startSelectionStart = startVals.get(PROPNAME_TEXT_SELECTION_START) != null ? ((Integer) startVals.get(PROPNAME_TEXT_SELECTION_START)).intValue() : -1;
            int startSelectionEnd2 = startVals.get(PROPNAME_TEXT_SELECTION_END) != null ? ((Integer) startVals.get(PROPNAME_TEXT_SELECTION_END)).intValue() : startSelectionStart;
            if (endVals.get(PROPNAME_TEXT_SELECTION_START) != null) {
                endSelectionStart2 = ((Integer) endVals.get(PROPNAME_TEXT_SELECTION_START)).intValue();
            }
            endSelectionStart = endSelectionStart2;
            startSelectionEnd = startSelectionEnd2;
            endSelectionEnd = endVals.get(PROPNAME_TEXT_SELECTION_END) != null ? ((Integer) endVals.get(PROPNAME_TEXT_SELECTION_END)).intValue() : endSelectionStart2;
        } else {
            startSelectionStart = -1;
            endSelectionEnd = startSelectionStart;
            endSelectionStart = -1;
            startSelectionEnd = -1;
        }
        int startSelectionStart2 = startSelectionStart;
        int i;
        Map<String, Object> map;
        if (startText.equals(endText)) {
            i = startSelectionEnd;
            CharSequence charSequence = startText;
            map = startVals;
            return null;
        }
        int endColor;
        Animator anim;
        final int i2;
        int startSelectionStart3;
        Animator anim2;
        if (this.mChangeBehavior != 2) {
            view.setText(startText);
            if (view instanceof EditText) {
                setSelection((EditText) view, startSelectionStart2, startSelectionEnd);
            }
        }
        final CharSequence charSequence2;
        final TextView textView;
        final CharSequence charSequence3;
        int i3;
        if (this.mChangeBehavior == 0) {
            endColor = 0;
            charSequence2 = startText;
            textView = view;
            i = startSelectionEnd;
            AnonymousClass1 anonymousClass1 = r0;
            charSequence3 = endText;
            anim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
            i2 = endSelectionStart;
            startSelectionStart3 = startSelectionStart2;
            startSelectionStart2 = endSelectionEnd;
            AnonymousClass1 anonymousClass12 = new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (charSequence2.equals(textView.getText())) {
                        textView.setText(charSequence3);
                        if (textView instanceof EditText) {
                            ChangeText.this.setSelection((EditText) textView, i2, startSelectionStart2);
                        }
                    }
                }
            };
            anim.addListener(anonymousClass1);
            anim2 = anim;
            map = startVals;
            i3 = 0;
        } else {
            Animator outAnim;
            int i4;
            int i5;
            int endColor2;
            Animator inAnim;
            startSelectionStart3 = startSelectionStart2;
            i = startSelectionEnd;
            final int startColor = ((Integer) startVals.get(PROPNAME_TEXT_COLOR)).intValue();
            startSelectionEnd = ((Integer) endVals.get(PROPNAME_TEXT_COLOR)).intValue();
            if (this.mChangeBehavior == 3 || this.mChangeBehavior == 1) {
                Animator outAnim2 = ValueAnimator.ofInt(new int[]{Color.alpha(startColor), 0});
                outAnim2.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        view.setTextColor((((Integer) animation.getAnimatedValue()).intValue() << 24) | (startColor & 16777215));
                    }
                });
                AnonymousClass3 anonymousClass3 = r0;
                charSequence2 = startText;
                outAnim = outAnim2;
                textView = view;
                i4 = 1;
                charSequence3 = endText;
                startVals = 2;
                i2 = endSelectionStart;
                i5 = 3;
                startSelectionStart2 = endSelectionEnd;
                endColor2 = startSelectionEnd;
                AnonymousClass3 anonymousClass32 = new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        if (charSequence2.equals(textView.getText())) {
                            textView.setText(charSequence3);
                            if (textView instanceof EditText) {
                                ChangeText.this.setSelection((EditText) textView, i2, startSelectionStart2);
                            }
                        }
                        textView.setTextColor(startSelectionEnd);
                    }
                };
                outAnim.addListener(anonymousClass3);
            } else {
                outAnim = null;
                i4 = 1;
                endColor2 = startSelectionEnd;
                i3 = startColor;
                map = startVals;
                i5 = 3;
            }
            if (this.mChangeBehavior != i5) {
                startSelectionStart = 2;
                if (this.mChangeBehavior != 2) {
                    inAnim = null;
                    startSelectionStart = endColor2;
                    if (outAnim == null && inAnim != null) {
                        anim2 = new AnimatorSet();
                        ((AnimatorSet) anim2).playSequentially(new Animator[]{outAnim, inAnim});
                    } else if (outAnim == null) {
                        anim2 = outAnim;
                    } else {
                        endColor = startSelectionStart;
                        anim2 = inAnim;
                    }
                    endColor = startSelectionStart;
                }
            } else {
                startSelectionStart = 2;
            }
            ValueAnimator inAnim2 = new int[startSelectionStart];
            inAnim2[0] = null;
            startSelectionStart = endColor2;
            inAnim2[i4] = Color.alpha(startSelectionStart);
            anim2 = ValueAnimator.ofInt(inAnim2);
            anim2.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    view.setTextColor((((Integer) animation.getAnimatedValue()).intValue() << 24) | (startSelectionStart & 16777215));
                }
            });
            anim2.addListener(new AnimatorListenerAdapter() {
                public void onAnimationCancel(Animator animation) {
                    view.setTextColor(startSelectionStart);
                }
            });
            inAnim = anim2;
            if (outAnim == null) {
            }
            if (outAnim == null) {
            }
        }
        anim = anim2;
        final TextView textView2 = view;
        final CharSequence charSequence4 = endText;
        final int i6 = endSelectionStart;
        i2 = endSelectionEnd;
        startSelectionStart2 = endColor;
        startSelectionEnd = startText;
        CharSequence startVals2 = endText;
        final int i7 = startSelectionStart3;
        startText = i;
        addListener(new TransitionListenerAdapter() {
            int mPausedColor = 0;

            public void onTransitionPause(Transition transition) {
                if (ChangeText.this.mChangeBehavior != 2) {
                    textView2.setText(charSequence4);
                    if (textView2 instanceof EditText) {
                        ChangeText.this.setSelection((EditText) textView2, i6, i2);
                    }
                }
                if (ChangeText.this.mChangeBehavior > 0) {
                    this.mPausedColor = textView2.getCurrentTextColor();
                    textView2.setTextColor(startSelectionStart2);
                }
            }

            public void onTransitionResume(Transition transition) {
                if (ChangeText.this.mChangeBehavior != 2) {
                    textView2.setText(startSelectionEnd);
                    if (textView2 instanceof EditText) {
                        ChangeText.this.setSelection((EditText) textView2, i7, startText);
                    }
                }
                if (ChangeText.this.mChangeBehavior > 0) {
                    textView2.setTextColor(this.mPausedColor);
                }
            }

            public void onTransitionEnd(Transition transition) {
                transition.removeListener(this);
            }
        });
        return anim;
    }

    private void setSelection(EditText editText, int start, int end) {
        if (start >= 0 && end >= 0) {
            editText.setSelection(start, end);
        }
    }
}
