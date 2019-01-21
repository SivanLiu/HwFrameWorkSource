package android.graphics.drawable;

import android.app.slice.Slice;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.util.AttributeSet;
import android.view.InflateException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class DrawableInflater {
    private static final HashMap<String, Constructor<? extends Drawable>> CONSTRUCTOR_MAP = new HashMap();
    private final ClassLoader mClassLoader;
    private final Resources mRes;

    public static Drawable loadDrawable(Context context, int id) {
        return loadDrawable(context.getResources(), context.getTheme(), id);
    }

    public static Drawable loadDrawable(Resources resources, Theme theme, int id) {
        return resources.getDrawable(id, theme);
    }

    public DrawableInflater(Resources res, ClassLoader classLoader) {
        this.mRes = res;
        this.mClassLoader = classLoader;
    }

    public Drawable inflateFromXml(String name, XmlPullParser parser, AttributeSet attrs, Theme theme) throws XmlPullParserException, IOException {
        return inflateFromXmlForDensity(name, parser, attrs, 0, theme);
    }

    Drawable inflateFromXmlForDensity(String name, XmlPullParser parser, AttributeSet attrs, int density, Theme theme) throws XmlPullParserException, IOException {
        if (name.equals("drawable")) {
            name = attrs.getAttributeValue(null, "class");
            if (name == null) {
                throw new InflateException("<drawable> tag must specify class attribute");
            }
        }
        Drawable drawable = inflateFromTag(name);
        if (drawable == null) {
            drawable = inflateFromClass(name);
        }
        drawable.setSrcDensityOverride(density);
        drawable.inflate(this.mRes, parser, attrs, theme);
        return drawable;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Drawable inflateFromTag(String name) {
        Object obj;
        switch (name.hashCode()) {
            case -2024464016:
                if (name.equals("adaptive-icon")) {
                    obj = 7;
                    break;
                }
            case -1724158635:
                if (name.equals("transition")) {
                    obj = 5;
                    break;
                }
            case -1671889043:
                if (name.equals("nine-patch")) {
                    obj = 19;
                    break;
                }
            case -1493546681:
                if (name.equals("animation-list")) {
                    obj = 16;
                    break;
                }
            case -1388777169:
                if (name.equals("bitmap")) {
                    obj = 18;
                    break;
                }
            case -930826704:
                if (name.equals("ripple")) {
                    obj = 6;
                    break;
                }
            case -925180581:
                if (name.equals("rotate")) {
                    obj = 14;
                    break;
                }
            case -820387517:
                if (name.equals("vector")) {
                    obj = 10;
                    break;
                }
            case -510364471:
                if (name.equals("animated-selector")) {
                    obj = 1;
                    break;
                }
            case -94197862:
                if (name.equals("layer-list")) {
                    obj = 4;
                    break;
                }
            case 3056464:
                if (name.equals("clip")) {
                    obj = 13;
                    break;
                }
            case 69897545:
                if (name.equals("hwcolorful")) {
                    obj = 3;
                    break;
                }
            case 94842723:
                if (name.equals(Slice.SUBTYPE_COLOR)) {
                    obj = 8;
                    break;
                }
            case 100360477:
                if (name.equals("inset")) {
                    obj = 17;
                    break;
                }
            case 109250890:
                if (name.equals("scale")) {
                    obj = 12;
                    break;
                }
            case 109399969:
                if (name.equals("shape")) {
                    obj = 9;
                    break;
                }
            case 160680263:
                if (name.equals("level-list")) {
                    obj = 2;
                    break;
                }
            case 1191572447:
                if (name.equals("selector")) {
                    obj = null;
                    break;
                }
            case 1442046129:
                if (name.equals("animated-image")) {
                    obj = 20;
                    break;
                }
            case 2013827269:
                if (name.equals("animated-rotate")) {
                    obj = 15;
                    break;
                }
            case 2118620333:
                if (name.equals("animated-vector")) {
                    obj = 11;
                    break;
                }
            default:
                obj = -1;
                break;
        }
        switch (obj) {
            case null:
                return new StateListDrawable();
            case 1:
                return new AnimatedStateListDrawable();
            case 2:
                return new LevelListDrawable();
            case 3:
                return new HwColorfulDrawable();
            case 4:
                return new LayerDrawable();
            case 5:
                return new TransitionDrawable();
            case 6:
                return new RippleDrawable();
            case 7:
                return new AdaptiveIconDrawable();
            case 8:
                return new ColorDrawable();
            case 9:
                return new GradientDrawable();
            case 10:
                return new VectorDrawable();
            case 11:
                return new AnimatedVectorDrawable();
            case 12:
                return new ScaleDrawable();
            case 13:
                return new ClipDrawable();
            case 14:
                return new RotateDrawable();
            case 15:
                return new AnimatedRotateDrawable();
            case 16:
                return new AnimationDrawable();
            case 17:
                return new InsetDrawable();
            case 18:
                return new BitmapDrawable();
            case 19:
                return new NinePatchDrawable();
            case 20:
                return new AnimatedImageDrawable();
            default:
                return null;
        }
    }

    private Drawable inflateFromClass(String className) {
        StringBuilder stringBuilder;
        InflateException ie;
        try {
            Constructor<? extends Drawable> constructor;
            synchronized (CONSTRUCTOR_MAP) {
                constructor = (Constructor) CONSTRUCTOR_MAP.get(className);
                if (constructor == null) {
                    constructor = this.mClassLoader.loadClass(className).asSubclass(Drawable.class).getConstructor(new Class[0]);
                    CONSTRUCTOR_MAP.put(className, constructor);
                }
            }
            return (Drawable) constructor.newInstance(new Object[0]);
        } catch (NoSuchMethodException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error inflating class ");
            stringBuilder.append(className);
            ie = new InflateException(stringBuilder.toString());
            ie.initCause(e);
            throw ie;
        } catch (ClassCastException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Class is not a Drawable ");
            stringBuilder.append(className);
            ie = new InflateException(stringBuilder.toString());
            ie.initCause(e2);
            throw ie;
        } catch (ClassNotFoundException e3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Class not found ");
            stringBuilder.append(className);
            ie = new InflateException(stringBuilder.toString());
            ie.initCause(e3);
            throw ie;
        } catch (Exception e4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error inflating class ");
            stringBuilder.append(className);
            ie = new InflateException(stringBuilder.toString());
            ie.initCause(e4);
            throw ie;
        }
    }
}
