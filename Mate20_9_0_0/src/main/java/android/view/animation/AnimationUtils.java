package android.view.animation;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.content.res.XmlResourceParser;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.rms.AppAssociate;
import android.util.AttributeSet;
import android.util.Xml;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AnimationUtils {
    private static final int SEQUENTIALLY = 1;
    private static final int TOGETHER = 0;
    private static ThreadLocal<AnimationState> sAnimationState = new ThreadLocal<AnimationState>() {
        protected AnimationState initialValue() {
            return new AnimationState();
        }
    };

    private static class AnimationState {
        boolean animationClockLocked;
        long currentVsyncTimeMillis;
        long lastReportedTimeMillis;

        private AnimationState() {
        }

        /* synthetic */ AnimationState(AnonymousClass1 x0) {
            this();
        }
    }

    public static void lockAnimationClock(long vsyncMillis) {
        AnimationState state = (AnimationState) sAnimationState.get();
        state.animationClockLocked = true;
        state.currentVsyncTimeMillis = vsyncMillis;
    }

    public static void unlockAnimationClock() {
        ((AnimationState) sAnimationState.get()).animationClockLocked = false;
    }

    public static long currentAnimationTimeMillis() {
        AnimationState state = (AnimationState) sAnimationState.get();
        if (state.animationClockLocked) {
            return Math.max(state.currentVsyncTimeMillis, state.lastReportedTimeMillis);
        }
        state.lastReportedTimeMillis = SystemClock.uptimeMillis();
        return state.lastReportedTimeMillis;
    }

    public static Animation loadAnimation(Context context, int id) throws NotFoundException {
        StringBuilder stringBuilder;
        NotFoundException rnf;
        XmlResourceParser parser = null;
        try {
            parser = context.getResources().getAnimation(id);
            Animation createAnimationFromXml = createAnimationFromXml(context, parser);
            if (parser != null) {
                parser.close();
            }
            return createAnimationFromXml;
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

    private static Animation createAnimationFromXml(Context c, XmlPullParser parser) throws XmlPullParserException, IOException {
        return createAnimationFromXml(c, parser, null, Xml.asAttributeSet(parser));
    }

    private static Animation createAnimationFromXml(Context c, XmlPullParser parser, AnimationSet parent, AttributeSet attrs) throws XmlPullParserException, IOException {
        Animation anim = null;
        int depth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 3 && parser.getDepth() <= depth) {
                break;
            }
            boolean z = true;
            if (type == 1) {
                break;
            } else if (type == 2) {
                String name = parser.getName();
                if (name.equals("set")) {
                    anim = new AnimationSet(c, attrs);
                    createAnimationFromXml(c, parser, (AnimationSet) anim, attrs);
                } else if (name.equals(AppAssociate.ASSOC_WINDOW_ALPHA)) {
                    boolean isColorInversionEnable = false;
                    if (c != null) {
                        if (Secure.getInt(c.getContentResolver(), Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0) != 1) {
                            z = false;
                        }
                        isColorInversionEnable = z;
                    }
                    if (isColorInversionEnable) {
                        anim = new AlphaAnimation(1.0f, 1.0f);
                    } else {
                        anim = new AlphaAnimation(c, attrs);
                    }
                } else if (name.equals(BatteryManager.EXTRA_SCALE)) {
                    anim = new ScaleAnimation(c, attrs);
                } else if (name.equals("rotate")) {
                    anim = new RotateAnimation(c, attrs);
                } else if (name.equals("translate")) {
                    anim = new TranslateAnimation(c, attrs);
                } else if (name.equals("cliprect")) {
                    anim = new ClipRectAnimation(c, attrs);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown animation name: ");
                    stringBuilder.append(parser.getName());
                    throw new RuntimeException(stringBuilder.toString());
                }
                if (parent != null) {
                    parent.addAnimation(anim);
                }
            }
        }
        return anim;
    }

    public static LayoutAnimationController loadLayoutAnimation(Context context, int id) throws NotFoundException {
        StringBuilder stringBuilder;
        NotFoundException rnf;
        XmlResourceParser parser = null;
        try {
            parser = context.getResources().getAnimation(id);
            LayoutAnimationController createLayoutAnimationFromXml = createLayoutAnimationFromXml(context, parser);
            if (parser != null) {
                parser.close();
            }
            return createLayoutAnimationFromXml;
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

    private static LayoutAnimationController createLayoutAnimationFromXml(Context c, XmlPullParser parser) throws XmlPullParserException, IOException {
        return createLayoutAnimationFromXml(c, parser, Xml.asAttributeSet(parser));
    }

    private static LayoutAnimationController createLayoutAnimationFromXml(Context c, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
        LayoutAnimationController controller = null;
        int depth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if ((next != 3 || parser.getDepth() > depth) && type != 1) {
                if (type == 2) {
                    LayoutAnimationController layoutAnimationController;
                    String name = parser.getName();
                    if ("layoutAnimation".equals(name)) {
                        layoutAnimationController = new LayoutAnimationController(c, attrs);
                    } else if ("gridLayoutAnimation".equals(name)) {
                        layoutAnimationController = new GridLayoutAnimationController(c, attrs);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown layout animation name: ");
                        stringBuilder.append(name);
                        throw new RuntimeException(stringBuilder.toString());
                    }
                    controller = layoutAnimationController;
                }
            }
        }
        return controller;
    }

    public static Animation makeInAnimation(Context c, boolean fromLeft) {
        Animation a;
        if (fromLeft) {
            a = loadAnimation(c, 17432578);
        } else {
            a = loadAnimation(c, 17432728);
        }
        a.setInterpolator(new DecelerateInterpolator());
        a.setStartTime(currentAnimationTimeMillis());
        return a;
    }

    public static Animation makeOutAnimation(Context c, boolean toRight) {
        Animation a;
        if (toRight) {
            a = loadAnimation(c, 17432579);
        } else {
            a = loadAnimation(c, 17432731);
        }
        a.setInterpolator(new AccelerateInterpolator());
        a.setStartTime(currentAnimationTimeMillis());
        return a;
    }

    public static Animation makeInChildBottomAnimation(Context c) {
        Animation a = loadAnimation(c, 17432725);
        a.setInterpolator(new AccelerateInterpolator());
        a.setStartTime(currentAnimationTimeMillis());
        return a;
    }

    public static Interpolator loadInterpolator(Context context, int id) throws NotFoundException {
        StringBuilder stringBuilder;
        NotFoundException rnf;
        XmlResourceParser parser = null;
        try {
            parser = context.getResources().getAnimation(id);
            Interpolator createInterpolatorFromXml = createInterpolatorFromXml(context.getResources(), context.getTheme(), parser);
            if (parser != null) {
                parser.close();
            }
            return createInterpolatorFromXml;
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

    public static Interpolator loadInterpolator(Resources res, Theme theme, int id) throws NotFoundException {
        StringBuilder stringBuilder;
        NotFoundException rnf;
        XmlResourceParser parser = null;
        try {
            parser = res.getAnimation(id);
            Interpolator createInterpolatorFromXml = createInterpolatorFromXml(res, theme, parser);
            if (parser != null) {
                parser.close();
            }
            return createInterpolatorFromXml;
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

    private static Interpolator createInterpolatorFromXml(Resources res, Theme theme, XmlPullParser parser) throws XmlPullParserException, IOException {
        BaseInterpolator interpolator = null;
        int depth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if ((next != 3 || parser.getDepth() > depth) && type != 1) {
                if (type == 2) {
                    AttributeSet attrs = Xml.asAttributeSet(parser);
                    String name = parser.getName();
                    if (name.equals("linearInterpolator")) {
                        interpolator = new LinearInterpolator();
                    } else if (name.equals("accelerateInterpolator")) {
                        interpolator = new AccelerateInterpolator(res, theme, attrs);
                    } else if (name.equals("decelerateInterpolator")) {
                        interpolator = new DecelerateInterpolator(res, theme, attrs);
                    } else if (name.equals("accelerateDecelerateInterpolator")) {
                        interpolator = new AccelerateDecelerateInterpolator();
                    } else if (name.equals("cycleInterpolator")) {
                        interpolator = new CycleInterpolator(res, theme, attrs);
                    } else if (name.equals("anticipateInterpolator")) {
                        interpolator = new AnticipateInterpolator(res, theme, attrs);
                    } else if (name.equals("overshootInterpolator")) {
                        interpolator = new OvershootInterpolator(res, theme, attrs);
                    } else if (name.equals("anticipateOvershootInterpolator")) {
                        interpolator = new AnticipateOvershootInterpolator(res, theme, attrs);
                    } else if (name.equals("bounceInterpolator")) {
                        interpolator = new BounceInterpolator();
                    } else if (name.equals("pathInterpolator")) {
                        interpolator = new PathInterpolator(res, theme, attrs);
                    } else if (name.equals("cubicBezierInterpolator") || name.equals("cubicBezierReverseInterpolator")) {
                        interpolator = (BaseInterpolator) HwFrameworkFactory.createHwInterpolator(name, res, theme, attrs);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown interpolator name: ");
                        stringBuilder.append(parser.getName());
                        throw new RuntimeException(stringBuilder.toString());
                    }
                }
            }
        }
        return interpolator;
    }
}
