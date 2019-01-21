package android.animation;

import android.content.Context;
import android.content.res.ConfigurationBoundResourceCache;
import android.content.res.ConstantState;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.hwtheme.HwThemeManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.PathParser;
import android.util.PathParser.PathData;
import android.util.StateSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.InflateException;
import android.view.animation.AnimationUtils;
import android.view.animation.BaseInterpolator;
import android.view.animation.Interpolator;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AnimatorInflater {
    private static final boolean DBG_ANIMATOR_INFLATER = false;
    private static final int SEQUENTIALLY = 1;
    private static final String TAG = "AnimatorInflater";
    private static final int TOGETHER = 0;
    private static final int VALUE_TYPE_COLOR = 3;
    private static final int VALUE_TYPE_FLOAT = 0;
    private static final int VALUE_TYPE_INT = 1;
    private static final int VALUE_TYPE_PATH = 2;
    private static final int VALUE_TYPE_UNDEFINED = 4;
    private static final TypedValue sTmpTypedValue = new TypedValue();

    private static class PathDataEvaluator implements TypeEvaluator<PathData> {
        private final PathData mPathData;

        private PathDataEvaluator() {
            this.mPathData = new PathData();
        }

        public PathData evaluate(float fraction, PathData startPathData, PathData endPathData) {
            if (PathParser.interpolatePathData(this.mPathData, startPathData, endPathData, fraction)) {
                return this.mPathData;
            }
            throw new IllegalArgumentException("Can't interpolate between two incompatible pathData");
        }
    }

    public static Animator loadAnimator(Context context, int id) throws NotFoundException {
        return loadAnimator(context.getResources(), context.getTheme(), id);
    }

    public static Animator loadAnimator(Resources resources, Theme theme, int id) throws NotFoundException {
        return loadAnimator(resources, theme, id, 1.0f);
    }

    public static Animator loadAnimator(Resources resources, Theme theme, int id, float pathErrorScale) throws NotFoundException {
        StringBuilder stringBuilder;
        NotFoundException rnf;
        ConfigurationBoundResourceCache<Animator> animatorCache = resources.getAnimatorCache();
        Animator animator = (Animator) animatorCache.getInstance((long) id, resources, theme);
        if (animator != null) {
            return animator;
        }
        XmlResourceParser parser = null;
        try {
            parser = resources.getAnimation(id);
            animator = createAnimatorFromXml(resources, theme, parser, pathErrorScale);
            if (animator != null) {
                animator.appendChangingConfigurations(getChangingConfigs(resources, id));
                ConstantState<Animator> constantState = animator.createConstantState();
                if (constantState != null) {
                    animatorCache.put((long) id, theme, constantState);
                    animator = (Animator) constantState.newInstance(resources, theme);
                }
            }
            if (parser != null) {
                parser.close();
            }
            return animator;
        } catch (XmlPullParserException ex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't load animation resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            rnf = new NotFoundException(stringBuilder.toString());
            rnf.initCause(ex);
            throw rnf;
        } catch (IOException ex2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't load animation resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            rnf = new NotFoundException(stringBuilder.toString());
            rnf.initCause(ex2);
            throw rnf;
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
        }
    }

    public static StateListAnimator loadStateListAnimator(Context context, int id) throws NotFoundException {
        StringBuilder stringBuilder;
        NotFoundException rnf;
        Resources resources = context.getResources();
        ConfigurationBoundResourceCache<StateListAnimator> cache = resources.getStateListAnimatorCache();
        Theme theme = context.getTheme();
        StateListAnimator animator = (StateListAnimator) cache.getInstance((long) id, resources, theme);
        if (animator != null) {
            return animator;
        }
        XmlResourceParser parser = null;
        try {
            parser = resources.getAnimation(id);
            animator = createStateListAnimatorFromXml(context, parser, Xml.asAttributeSet(parser));
            if (animator != null) {
                animator.appendChangingConfigurations(getChangingConfigs(resources, id));
                ConstantState<StateListAnimator> constantState = animator.createConstantState();
                if (constantState != null) {
                    cache.put((long) id, theme, constantState);
                    animator = (StateListAnimator) constantState.newInstance(resources, theme);
                }
            }
            if (parser != null) {
                parser.close();
            }
            return animator;
        } catch (XmlPullParserException ex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't load state list animator resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            rnf = new NotFoundException(stringBuilder.toString());
            rnf.initCause(ex);
            throw rnf;
        } catch (IOException ex2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't load state list animator resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            rnf = new NotFoundException(stringBuilder.toString());
            rnf.initCause(ex2);
            throw rnf;
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private static StateListAnimator createStateListAnimatorFromXml(Context context, XmlPullParser parser, AttributeSet attributeSet) throws IOException, XmlPullParserException {
        StateListAnimator stateListAnimator = new StateListAnimator();
        while (true) {
            switch (parser.next()) {
                case 1:
                case 3:
                    return stateListAnimator;
                case 2:
                    if (HwThemeManager.TAG_ITEM.equals(parser.getName())) {
                        int attributeCount = parser.getAttributeCount();
                        int[] states = new int[attributeCount];
                        int stateIndex = 0;
                        Animator animator = null;
                        for (int i = 0; i < attributeCount; i++) {
                            int attrName = attributeSet.getAttributeNameResource(i);
                            if (attrName == 16843213) {
                                animator = loadAnimator(context, attributeSet.getAttributeResourceValue(i, 0));
                            } else {
                                int i2;
                                int stateIndex2 = stateIndex + 1;
                                if (attributeSet.getAttributeBooleanValue(i, false)) {
                                    i2 = attrName;
                                } else {
                                    i2 = -attrName;
                                }
                                states[stateIndex] = i2;
                                stateIndex = stateIndex2;
                            }
                        }
                        if (animator == null) {
                            animator = createAnimatorFromXml(context.getResources(), context.getTheme(), parser, 1.0f);
                        }
                        if (animator != null) {
                            stateListAnimator.addState(StateSet.trimStateSet(states, stateIndex), animator);
                            break;
                        }
                        throw new NotFoundException("animation state item must have a valid animation");
                    }
                    continue;
                default:
                    break;
            }
        }
    }

    private static PropertyValuesHolder getPVH(TypedArray styledAttributes, int valueType, int valueFromId, int valueToId, String propertyName) {
        int valueType2;
        PropertyValuesHolder returnValue;
        TypedArray typedArray = styledAttributes;
        int i = valueFromId;
        int i2 = valueToId;
        String str = propertyName;
        TypedValue tvFrom = typedArray.peekValue(i);
        boolean hasFrom = tvFrom != null;
        int fromType = hasFrom ? tvFrom.type : 0;
        TypedValue tvTo = typedArray.peekValue(i2);
        boolean hasTo = tvTo != null;
        int toType = hasTo ? tvTo.type : 0;
        int i3 = valueType;
        if (i3 != 4) {
            valueType2 = i3;
        } else if ((hasFrom && isColorType(fromType)) || (hasTo && isColorType(toType))) {
            valueType2 = 3;
        } else {
            valueType2 = 0;
        }
        boolean getFloats = valueType2 == 0;
        TypedValue typedValue;
        int toType2;
        PropertyValuesHolder propertyValuesHolder;
        if (valueType2 == 2) {
            String fromString = typedArray.getString(i);
            String toString = typedArray.getString(i2);
            PathData nodesFrom = fromString == null ? null : new PathData(fromString);
            if (toString == null) {
                TypedValue typedValue2 = tvFrom;
                tvFrom = null;
            } else {
                tvFrom = new PathData(toString);
            }
            if (nodesFrom == null && tvFrom == null) {
                typedValue = tvTo;
                toType2 = toType;
                propertyValuesHolder = null;
            } else {
                if (nodesFrom != null) {
                    propertyValuesHolder = null;
                    TypeEvaluator tvTo2 = new PathDataEvaluator();
                    if (tvFrom == null) {
                        toType2 = toType;
                        returnValue = PropertyValuesHolder.ofObject(str, tvTo2, nodesFrom);
                    } else if (PathParser.canMorph(nodesFrom, tvFrom)) {
                        returnValue = PropertyValuesHolder.ofObject(str, tvTo2, nodesFrom, tvFrom);
                        toType2 = toType;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(" Can't morph from ");
                        stringBuilder.append(fromString);
                        stringBuilder.append(" to ");
                        stringBuilder.append(toString);
                        throw new InflateException(stringBuilder.toString());
                    }
                }
                toType2 = toType;
                propertyValuesHolder = null;
                if (tvFrom != null) {
                    returnValue = PropertyValuesHolder.ofObject(str, new PathDataEvaluator(), tvFrom);
                }
                tvTo = toType2;
                toType = valueToId;
            }
            returnValue = propertyValuesHolder;
            tvTo = toType2;
            toType = valueToId;
        } else {
            typedValue = tvTo;
            toType2 = toType;
            propertyValuesHolder = null;
            TypeEvaluator evaluator = null;
            if (valueType2 == 3) {
                evaluator = ArgbEvaluator.getInstance();
            }
            if (getFloats) {
                float valueTo;
                if (hasFrom) {
                    float valueFrom;
                    if (fromType == 5) {
                        valueFrom = typedArray.getDimension(i, 0.0f);
                    } else {
                        valueFrom = typedArray.getFloat(i, 0.0f);
                    }
                    if (hasTo) {
                        if (toType2 == 5) {
                            valueTo = typedArray.getDimension(valueToId, 0.0f);
                        } else {
                            valueTo = typedArray.getFloat(valueToId, 0.0f);
                        }
                        returnValue = PropertyValuesHolder.ofFloat(str, valueFrom, valueTo);
                    } else {
                        toType = valueToId;
                        valueTo = PropertyValuesHolder.ofFloat(str, valueFrom);
                    }
                } else {
                    toType = valueToId;
                    if (toType2 == 5) {
                        valueTo = typedArray.getDimension(toType, 0.0f);
                    } else {
                        valueTo = typedArray.getFloat(toType, 0.0f);
                    }
                    valueTo = PropertyValuesHolder.ofFloat(str, valueTo);
                }
                returnValue = valueTo;
            } else {
                int toType3 = toType2;
                toType = valueToId;
                int valueTo2;
                int i4;
                if (hasFrom) {
                    int valueFrom2;
                    if (fromType == 5) {
                        valueFrom2 = (int) typedArray.getDimension(i, 0.0f);
                    } else if (isColorType(fromType)) {
                        valueFrom2 = typedArray.getColor(i, 0);
                    } else {
                        valueFrom2 = typedArray.getInt(i, 0);
                    }
                    if (hasTo) {
                        if (toType3 == 5) {
                            valueTo2 = (int) typedArray.getDimension(toType, 0.0f);
                            i4 = 0;
                        } else if (isColorType(toType3)) {
                            i4 = 0;
                            valueTo2 = typedArray.getColor(toType, 0);
                        } else {
                            i4 = 0;
                            valueTo2 = typedArray.getInt(toType, 0);
                        }
                        returnValue = PropertyValuesHolder.ofInt(str, valueFrom, valueTo2);
                    } else {
                        returnValue = PropertyValuesHolder.ofInt(str, valueFrom);
                    }
                } else if (hasTo) {
                    if (toType3 == 5) {
                        valueTo2 = (int) typedArray.getDimension(toType, 0.0f);
                        i4 = 0;
                    } else if (isColorType(toType3)) {
                        i4 = 0;
                        valueTo2 = typedArray.getColor(toType, 0);
                    } else {
                        i4 = 0;
                        valueTo2 = typedArray.getInt(toType, 0);
                    }
                    returnValue = PropertyValuesHolder.ofInt(str, valueTo2);
                } else {
                    returnValue = propertyValuesHolder;
                }
            }
            if (!(returnValue == null || evaluator == null)) {
                returnValue.setEvaluator(evaluator);
            }
        }
        return returnValue;
    }

    private static void parseAnimatorFromTypeArray(ValueAnimator anim, TypedArray arrayAnimator, TypedArray arrayObjectAnimator, float pixelSize) {
        long duration = (long) arrayAnimator.getInt(1, 300);
        long startDelay = (long) arrayAnimator.getInt(2, 0);
        int valueType = arrayAnimator.getInt(7, 4);
        if (valueType == 4) {
            valueType = inferValueTypeFromValues(arrayAnimator, 5, 6);
        }
        if (getPVH(arrayAnimator, valueType, 5, 6, "") != null) {
            anim.setValues(getPVH(arrayAnimator, valueType, 5, 6, ""));
        }
        anim.setDuration(duration);
        anim.setStartDelay(startDelay);
        if (arrayAnimator.hasValue(3)) {
            anim.setRepeatCount(arrayAnimator.getInt(3, 0));
        }
        if (arrayAnimator.hasValue(4)) {
            anim.setRepeatMode(arrayAnimator.getInt(4, 1));
        }
        if (arrayObjectAnimator != null) {
            setupObjectAnimator(anim, arrayObjectAnimator, valueType, pixelSize);
        }
    }

    private static TypeEvaluator setupAnimatorForPath(ValueAnimator anim, TypedArray arrayAnimator) {
        String fromString = arrayAnimator.getString(5);
        String toString = arrayAnimator.getString(6);
        PathData pathDataFrom = fromString == null ? null : new PathData(fromString);
        PathData pathDataTo = toString == null ? null : new PathData(toString);
        if (pathDataFrom != null) {
            if (pathDataTo != null) {
                anim.setObjectValues(pathDataFrom, pathDataTo);
                if (!PathParser.canMorph(pathDataFrom, pathDataTo)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(arrayAnimator.getPositionDescription());
                    stringBuilder.append(" Can't morph from ");
                    stringBuilder.append(fromString);
                    stringBuilder.append(" to ");
                    stringBuilder.append(toString);
                    throw new InflateException(stringBuilder.toString());
                }
            }
            anim.setObjectValues(pathDataFrom);
            return new PathDataEvaluator();
        } else if (pathDataTo == null) {
            return null;
        } else {
            anim.setObjectValues(pathDataTo);
            return new PathDataEvaluator();
        }
    }

    private static void setupObjectAnimator(ValueAnimator anim, TypedArray arrayObjectAnimator, int valueType, float pixelSize) {
        TypedArray typedArray = arrayObjectAnimator;
        int valueType2 = valueType;
        ObjectAnimator oa = (ObjectAnimator) anim;
        String pathData = typedArray.getString(1);
        if (pathData != null) {
            String propertyXName = typedArray.getString(2);
            String propertyYName = typedArray.getString(3);
            if (valueType2 == 2 || valueType2 == 4) {
                valueType2 = 0;
            }
            if (propertyXName == null && propertyYName == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(arrayObjectAnimator.getPositionDescription());
                stringBuilder.append(" propertyXName or propertyYName is needed for PathData");
                throw new InflateException(stringBuilder.toString());
            }
            Keyframes xKeyframes;
            Keyframes yKeyframes;
            PathKeyframes keyframeSet = KeyframeSet.ofPath(PathParser.createPathFromPathData(pathData), 0.5f * pixelSize);
            if (valueType2 == 0) {
                xKeyframes = keyframeSet.createXFloatKeyframes();
                yKeyframes = keyframeSet.createYFloatKeyframes();
            } else {
                xKeyframes = keyframeSet.createXIntKeyframes();
                yKeyframes = keyframeSet.createYIntKeyframes();
            }
            PropertyValuesHolder x = null;
            PropertyValuesHolder y = null;
            if (propertyXName != null) {
                x = PropertyValuesHolder.ofKeyframes(propertyXName, xKeyframes);
            }
            if (propertyYName != null) {
                y = PropertyValuesHolder.ofKeyframes(propertyYName, yKeyframes);
            }
            if (x == null) {
                oa.setValues(y);
                return;
            } else if (y == null) {
                oa.setValues(x);
                return;
            } else {
                oa.setValues(x, y);
                return;
            }
        }
        oa.setPropertyName(typedArray.getString(0));
    }

    private static void setupValues(ValueAnimator anim, TypedArray arrayAnimator, boolean getFloats, boolean hasFrom, int fromType, boolean hasTo, int toType) {
        if (getFloats) {
            if (hasFrom) {
                float valueFrom;
                if (fromType == 5) {
                    valueFrom = arrayAnimator.getDimension(5, 0.0f);
                } else {
                    valueFrom = arrayAnimator.getFloat(5, 0.0f);
                }
                if (hasTo) {
                    float valueTo;
                    if (toType == 5) {
                        valueTo = arrayAnimator.getDimension(6, 0.0f);
                    } else {
                        valueTo = arrayAnimator.getFloat(6, 0.0f);
                    }
                    anim.setFloatValues(valueFrom, valueTo);
                    return;
                }
                anim.setFloatValues(valueFrom);
                return;
            }
            float valueTo2;
            if (toType == 5) {
                valueTo2 = arrayAnimator.getDimension(6, 0.0f);
            } else {
                valueTo2 = arrayAnimator.getFloat(6, 0.0f);
            }
            anim.setFloatValues(valueTo2);
        } else if (hasFrom) {
            int valueFrom2;
            if (fromType == 5) {
                valueFrom2 = (int) arrayAnimator.getDimension(5, 0.0f);
            } else if (isColorType(fromType)) {
                valueFrom2 = arrayAnimator.getColor(5, 0);
            } else {
                valueFrom2 = arrayAnimator.getInt(5, 0);
            }
            if (hasTo) {
                int valueTo3;
                if (toType == 5) {
                    valueTo3 = (int) arrayAnimator.getDimension(6, 0.0f);
                } else if (isColorType(toType)) {
                    valueTo3 = arrayAnimator.getColor(6, 0);
                } else {
                    valueTo3 = arrayAnimator.getInt(6, 0);
                }
                anim.setIntValues(valueFrom2, valueTo3);
                return;
            }
            anim.setIntValues(valueFrom2);
        } else if (hasTo) {
            int valueTo4;
            if (toType == 5) {
                valueTo4 = (int) arrayAnimator.getDimension(6, 0.0f);
            } else if (isColorType(toType)) {
                valueTo4 = arrayAnimator.getColor(6, 0);
            } else {
                valueTo4 = arrayAnimator.getInt(6, 0);
            }
            anim.setIntValues(valueTo4);
        }
    }

    private static Animator createAnimatorFromXml(Resources res, Theme theme, XmlPullParser parser, float pixelSize) throws XmlPullParserException, IOException {
        return createAnimatorFromXml(res, theme, parser, Xml.asAttributeSet(parser), null, 0, pixelSize);
    }

    /* JADX WARNING: Removed duplicated region for block: B:37:0x00c1  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Animator createAnimatorFromXml(Resources res, Theme theme, XmlPullParser parser, AttributeSet attrs, AnimatorSet parent, int sequenceOrdering, float pixelSize) throws XmlPullParserException, IOException {
        XmlPullParser xmlPullParser;
        Resources resources = res;
        Theme theme2 = theme;
        AttributeSet attributeSet = attrs;
        AnimatorSet animatorSet = parent;
        float f = pixelSize;
        Animator anim = null;
        int depth = parser.getDepth();
        ArrayList<Animator> childAnims = null;
        while (true) {
            int depth2 = depth;
            int next = parser.next();
            int type = next;
            if ((next != 3 || parser.getDepth() > depth2) && type != 1) {
                if (type != 2) {
                    depth = depth2;
                } else {
                    String name = parser.getName();
                    boolean gotValues = false;
                    if (name.equals("objectAnimator")) {
                        anim = loadObjectAnimator(resources, theme2, attributeSet, f);
                    } else if (name.equals("animator")) {
                        anim = loadAnimator(resources, theme2, attributeSet, null, f);
                    } else {
                        if (name.equals("set")) {
                            TypedArray a;
                            Animator anim2 = new AnimatorSet();
                            if (theme2 != null) {
                                a = theme2.obtainStyledAttributes(attributeSet, R.styleable.AnimatorSet, 0, 0);
                            } else {
                                a = resources.obtainAttributes(attributeSet, R.styleable.AnimatorSet);
                            }
                            TypedArray a2 = a;
                            anim2.appendChangingConfigurations(a2.getChangingConfigurations());
                            AttributeSet attributeSet2 = attributeSet;
                            TypedArray a3 = a2;
                            Animator anim3 = anim2;
                            createAnimatorFromXml(resources, theme2, parser, attributeSet2, (AnimatorSet) anim2, a2.getInt(0, 0), f);
                            a3.recycle();
                            anim = anim3;
                        } else if (name.equals("propertyValuesHolder")) {
                            PropertyValuesHolder[] values = loadValues(resources, theme2, parser, Xml.asAttributeSet(parser));
                            if (!(values == null || anim == null || !(anim instanceof ValueAnimator))) {
                                ((ValueAnimator) anim).setValues(values);
                            }
                            gotValues = true;
                        } else {
                            xmlPullParser = parser;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown animator name: ");
                            stringBuilder.append(parser.getName());
                            throw new RuntimeException(stringBuilder.toString());
                        }
                        if (!(animatorSet == null || gotValues)) {
                            if (childAnims == null) {
                                childAnims = new ArrayList();
                            }
                            childAnims.add(anim);
                        }
                        depth = depth2;
                        attributeSet = attrs;
                    }
                    xmlPullParser = parser;
                    if (childAnims == null) {
                    }
                    childAnims.add(anim);
                    depth = depth2;
                    attributeSet = attrs;
                }
            }
        }
        xmlPullParser = parser;
        if (!(animatorSet == null || childAnims == null)) {
            Animator[] animsArray = new Animator[childAnims.size()];
            int index = 0;
            Iterator it = childAnims.iterator();
            while (it.hasNext()) {
                int index2 = index + 1;
                animsArray[index] = (Animator) it.next();
                index = index2;
            }
            if (sequenceOrdering == 0) {
                animatorSet.playTogether(animsArray);
            } else {
                animatorSet.playSequentially(animsArray);
            }
        }
        return anim;
    }

    private static PropertyValuesHolder[] loadValues(Resources res, Theme theme, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
        int i;
        PropertyValuesHolder[] valuesArray;
        ArrayList<PropertyValuesHolder> values = null;
        while (true) {
            int eventType = parser.getEventType();
            int type = eventType;
            i = 0;
            if (eventType == 3 || type == 1) {
                valuesArray = null;
            } else if (type != 2) {
                parser.next();
            } else {
                if (parser.getName().equals("propertyValuesHolder")) {
                    TypedArray a;
                    if (theme != null) {
                        a = theme.obtainStyledAttributes(attrs, R.styleable.PropertyValuesHolder, 0, 0);
                    } else {
                        a = res.obtainAttributes(attrs, R.styleable.PropertyValuesHolder);
                    }
                    String propertyName = a.getString(3);
                    int valueType = a.getInt(2, 4);
                    PropertyValuesHolder pvh = loadPvh(res, theme, parser, propertyName, valueType);
                    if (pvh == null) {
                        pvh = getPVH(a, valueType, 0, 1, propertyName);
                    }
                    if (pvh != null) {
                        if (values == null) {
                            values = new ArrayList();
                        }
                        values.add(pvh);
                    }
                    a.recycle();
                }
                parser.next();
            }
        }
        valuesArray = null;
        if (values != null) {
            int count = values.size();
            valuesArray = new PropertyValuesHolder[count];
            while (i < count) {
                valuesArray[i] = (PropertyValuesHolder) values.get(i);
                i++;
            }
        }
        return valuesArray;
    }

    private static int inferValueTypeOfKeyframe(Resources res, Theme theme, AttributeSet attrs) {
        TypedArray a;
        int valueType = 0;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs, R.styleable.Keyframe, 0, 0);
        } else {
            a = res.obtainAttributes(attrs, R.styleable.Keyframe);
        }
        TypedValue keyframeValue = a.peekValue(0);
        if ((keyframeValue != null) && isColorType(keyframeValue.type)) {
            valueType = 3;
        }
        a.recycle();
        return valueType;
    }

    private static int inferValueTypeFromValues(TypedArray styledAttributes, int valueFromId, int valueToId) {
        TypedValue tvFrom = styledAttributes.peekValue(valueFromId);
        boolean hasTo = true;
        boolean hasFrom = tvFrom != null;
        int fromType = hasFrom ? tvFrom.type : 0;
        TypedValue tvTo = styledAttributes.peekValue(valueToId);
        if (tvTo == null) {
            hasTo = false;
        }
        int toType = hasTo ? tvTo.type : 0;
        if ((hasFrom && isColorType(fromType)) || (hasTo && isColorType(toType))) {
            return 3;
        }
        return 0;
    }

    private static void dumpKeyframes(Object[] keyframes, String header) {
        if (keyframes != null && keyframes.length != 0) {
            Log.d(TAG, header);
            int count = keyframes.length;
            for (int i = 0; i < count; i++) {
                Keyframe keyframe = keyframes[i];
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Keyframe ");
                stringBuilder.append(i);
                stringBuilder.append(": fraction ");
                stringBuilder.append(keyframe.getFraction() < 0.0f ? "null" : Float.valueOf(keyframe.getFraction()));
                stringBuilder.append(", , value : ");
                stringBuilder.append(keyframe.hasValue() ? keyframe.getValue() : "null");
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x0046  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static PropertyValuesHolder loadPvh(Resources res, Theme theme, XmlPullParser parser, String propertyName, int valueType) throws XmlPullParserException, IOException {
        Resources resources = res;
        Theme theme2 = theme;
        PropertyValuesHolder value = null;
        ArrayList<Keyframe> keyframes = null;
        int valueType2 = valueType;
        while (true) {
            int next = parser.next();
            int type = next;
            Keyframe keyframe;
            if (next == 3 || type == 1) {
                if (keyframes != null) {
                    next = keyframes.size();
                    int count = next;
                    if (next > 0) {
                        next = 0;
                        Keyframe firstKeyframe = (Keyframe) keyframes.get(0);
                        Keyframe lastKeyframe = (Keyframe) keyframes.get(count - 1);
                        float endFraction = lastKeyframe.getFraction();
                        float f = 1.0f;
                        float f2 = 0.0f;
                        if (endFraction < 1.0f) {
                            if (endFraction < 0.0f) {
                                lastKeyframe.setFraction(1.0f);
                            } else {
                                keyframes.add(keyframes.size(), createNewKeyframe(lastKeyframe, 1.0f));
                                count++;
                            }
                        }
                        float startFraction = firstKeyframe.getFraction();
                        if (startFraction != 0.0f) {
                            if (startFraction < 0.0f) {
                                firstKeyframe.setFraction(0.0f);
                            } else {
                                keyframes.add(0, createNewKeyframe(firstKeyframe, 0.0f));
                                count++;
                            }
                        }
                        Keyframe[] keyframeArray = new Keyframe[count];
                        keyframes.toArray(keyframeArray);
                        while (next < count) {
                            float f3;
                            keyframe = keyframeArray[next];
                            if (keyframe.getFraction() < f2) {
                                if (next == 0) {
                                    keyframe.setFraction(f2);
                                } else {
                                    if (next == count - 1) {
                                        keyframe.setFraction(f);
                                        f3 = 0.0f;
                                    } else {
                                        int startIndex = next;
                                        int j = startIndex + 1;
                                        int endIndex = next;
                                        while (true) {
                                            int j2 = j;
                                            if (j2 >= count - 1) {
                                                f3 = 0.0f;
                                                break;
                                            }
                                            f3 = 0.0f;
                                            if (keyframeArray[j2].getFraction() >= 0.0f) {
                                                break;
                                            }
                                            endIndex = j2;
                                            j = j2 + 1;
                                            resources = res;
                                            theme2 = theme;
                                        }
                                        distributeKeyframes(keyframeArray, keyframeArray[endIndex + 1].getFraction() - keyframeArray[startIndex - 1].getFraction(), startIndex, endIndex);
                                    }
                                    next++;
                                    f2 = f3;
                                    resources = res;
                                    theme2 = theme;
                                    f = 1.0f;
                                }
                            }
                            f3 = f2;
                            next++;
                            f2 = f3;
                            resources = res;
                            theme2 = theme;
                            f = 1.0f;
                        }
                        value = PropertyValuesHolder.ofKeyframe(propertyName, keyframeArray);
                        if (valueType2 == 3) {
                            value.setEvaluator(ArgbEvaluator.getInstance());
                        }
                        return value;
                    }
                }
            } else if (parser.getName().equals("keyframe")) {
                if (valueType2 == 4) {
                    valueType2 = inferValueTypeOfKeyframe(resources, theme2, Xml.asAttributeSet(parser));
                }
                keyframe = loadKeyframe(resources, theme2, Xml.asAttributeSet(parser), valueType2);
                if (keyframe != null) {
                    if (keyframes == null) {
                        keyframes = new ArrayList();
                    }
                    keyframes.add(keyframe);
                }
                parser.next();
            }
        }
        if (keyframes != null) {
        }
        String str = propertyName;
        return value;
    }

    private static Keyframe createNewKeyframe(Keyframe sampleKeyframe, float fraction) {
        if (sampleKeyframe.getType() == Float.TYPE) {
            return Keyframe.ofFloat(fraction);
        }
        if (sampleKeyframe.getType() == Integer.TYPE) {
            return Keyframe.ofInt(fraction);
        }
        return Keyframe.ofObject(fraction);
    }

    private static void distributeKeyframes(Keyframe[] keyframes, float gap, int startIndex, int endIndex) {
        float increment = gap / ((float) ((endIndex - startIndex) + 2));
        for (int i = startIndex; i <= endIndex; i++) {
            keyframes[i].setFraction(keyframes[i - 1].getFraction() + increment);
        }
    }

    private static Keyframe loadKeyframe(Resources res, Theme theme, AttributeSet attrs, int valueType) throws XmlPullParserException, IOException {
        TypedArray a;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs, R.styleable.Keyframe, 0, 0);
        } else {
            a = res.obtainAttributes(attrs, R.styleable.Keyframe);
        }
        Keyframe keyframe = null;
        float fraction = a.getFloat(3, -1.0f);
        TypedValue keyframeValue = a.peekValue(0);
        boolean hasValue = keyframeValue != null;
        if (valueType == 4) {
            if (hasValue && isColorType(keyframeValue.type)) {
                valueType = 3;
            } else {
                valueType = 0;
            }
        }
        if (hasValue) {
            if (valueType != 3) {
                switch (valueType) {
                    case 0:
                        keyframe = Keyframe.ofFloat(fraction, a.getFloat(0, 0.0f));
                        break;
                    case 1:
                        break;
                }
            }
            keyframe = Keyframe.ofInt(fraction, a.getInt(0, 0));
        } else {
            Keyframe ofFloat;
            if (valueType == 0) {
                ofFloat = Keyframe.ofFloat(fraction);
            } else {
                ofFloat = Keyframe.ofInt(fraction);
            }
            keyframe = ofFloat;
        }
        int resID = a.getResourceId(1, 0);
        if (resID > 0) {
            keyframe.setInterpolator(AnimationUtils.loadInterpolator(res, theme, resID));
        }
        a.recycle();
        return keyframe;
    }

    private static ObjectAnimator loadObjectAnimator(Resources res, Theme theme, AttributeSet attrs, float pathErrorScale) throws NotFoundException {
        ObjectAnimator anim = new ObjectAnimator();
        loadAnimator(res, theme, attrs, anim, pathErrorScale);
        return anim;
    }

    private static ValueAnimator loadAnimator(Resources res, Theme theme, AttributeSet attrs, ValueAnimator anim, float pathErrorScale) throws NotFoundException {
        TypedArray arrayAnimator;
        TypedArray arrayObjectAnimator = null;
        if (theme != null) {
            arrayAnimator = theme.obtainStyledAttributes(attrs, R.styleable.Animator, 0, 0);
        } else {
            arrayAnimator = res.obtainAttributes(attrs, R.styleable.Animator);
        }
        if (anim != null) {
            if (theme != null) {
                arrayObjectAnimator = theme.obtainStyledAttributes(attrs, R.styleable.PropertyAnimator, 0, 0);
            } else {
                arrayObjectAnimator = res.obtainAttributes(attrs, R.styleable.PropertyAnimator);
            }
            anim.appendChangingConfigurations(arrayObjectAnimator.getChangingConfigurations());
        }
        if (anim == null) {
            anim = new ValueAnimator();
        }
        anim.appendChangingConfigurations(arrayAnimator.getChangingConfigurations());
        parseAnimatorFromTypeArray(anim, arrayAnimator, arrayObjectAnimator, pathErrorScale);
        int resID = arrayAnimator.getResourceId(0, 0);
        if (resID > 0) {
            Interpolator interpolator = AnimationUtils.loadInterpolator(res, theme, resID);
            if (interpolator instanceof BaseInterpolator) {
                anim.appendChangingConfigurations(((BaseInterpolator) interpolator).getChangingConfiguration());
            }
            anim.setInterpolator(interpolator);
        }
        arrayAnimator.recycle();
        if (arrayObjectAnimator != null) {
            arrayObjectAnimator.recycle();
        }
        return anim;
    }

    private static int getChangingConfigs(Resources resources, int id) {
        int i;
        synchronized (sTmpTypedValue) {
            resources.getValue(id, sTmpTypedValue, true);
            i = sTmpTypedValue.changingConfigurations;
        }
        return i;
    }

    private static boolean isColorType(int type) {
        return type >= 28 && type <= 31;
    }
}
