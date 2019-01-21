package android.transition;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;
import android.view.ViewGroup;
import com.android.internal.R;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class TransitionInflater {
    private static final Class<?>[] sConstructorSignature = new Class[]{Context.class, AttributeSet.class};
    private static final ArrayMap<String, Constructor> sConstructors = new ArrayMap();
    private Context mContext;

    private TransitionInflater(Context context) {
        this.mContext = context;
    }

    public static TransitionInflater from(Context context) {
        return new TransitionInflater(context);
    }

    public Transition inflateTransition(int resource) {
        InflateException ex;
        XmlResourceParser parser = this.mContext.getResources().getXml(resource);
        try {
            Transition createTransitionFromXml = createTransitionFromXml(parser, Xml.asAttributeSet(parser), null);
            parser.close();
            return createTransitionFromXml;
        } catch (XmlPullParserException e) {
            ex = new InflateException(e.getMessage());
            ex.initCause(e);
            throw ex;
        } catch (IOException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(parser.getPositionDescription());
            stringBuilder.append(": ");
            stringBuilder.append(e2.getMessage());
            ex = new InflateException(stringBuilder.toString());
            ex.initCause(e2);
            throw ex;
        } catch (Throwable th) {
            parser.close();
        }
    }

    public TransitionManager inflateTransitionManager(int resource, ViewGroup sceneRoot) {
        InflateException ex;
        XmlResourceParser parser = this.mContext.getResources().getXml(resource);
        try {
            TransitionManager createTransitionManagerFromXml = createTransitionManagerFromXml(parser, Xml.asAttributeSet(parser), sceneRoot);
            parser.close();
            return createTransitionManagerFromXml;
        } catch (XmlPullParserException e) {
            ex = new InflateException(e.getMessage());
            ex.initCause(e);
            throw ex;
        } catch (IOException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(parser.getPositionDescription());
            stringBuilder.append(": ");
            stringBuilder.append(e2.getMessage());
            ex = new InflateException(stringBuilder.toString());
            ex.initCause(e2);
            throw ex;
        } catch (Throwable th) {
            parser.close();
        }
    }

    private Transition createTransitionFromXml(XmlPullParser parser, AttributeSet attrs, Transition parent) throws XmlPullParserException, IOException {
        Transition transition = null;
        int depth = parser.getDepth();
        TransitionSet transitionSet = parent instanceof TransitionSet ? (TransitionSet) parent : null;
        while (true) {
            int next = parser.next();
            int type = next;
            if ((next != 3 || parser.getDepth() > depth) && type != 1) {
                if (type == 2) {
                    String name = parser.getName();
                    if ("fade".equals(name)) {
                        transition = new Fade(this.mContext, attrs);
                    } else if ("changeBounds".equals(name)) {
                        transition = new ChangeBounds(this.mContext, attrs);
                    } else if ("slide".equals(name)) {
                        transition = new Slide(this.mContext, attrs);
                    } else if ("explode".equals(name)) {
                        transition = new Explode(this.mContext, attrs);
                    } else if ("changeImageTransform".equals(name)) {
                        transition = new ChangeImageTransform(this.mContext, attrs);
                    } else if ("changeTransform".equals(name)) {
                        transition = new ChangeTransform(this.mContext, attrs);
                    } else if ("changeClipBounds".equals(name)) {
                        transition = new ChangeClipBounds(this.mContext, attrs);
                    } else if ("autoTransition".equals(name)) {
                        transition = new AutoTransition(this.mContext, attrs);
                    } else if ("recolor".equals(name)) {
                        transition = new Recolor(this.mContext, attrs);
                    } else if ("changeScroll".equals(name)) {
                        transition = new ChangeScroll(this.mContext, attrs);
                    } else if ("transitionSet".equals(name)) {
                        transition = new TransitionSet(this.mContext, attrs);
                    } else if ("transition".equals(name)) {
                        transition = (Transition) createCustom(attrs, Transition.class, "transition");
                    } else if ("targets".equals(name)) {
                        getTargetIds(parser, attrs, parent);
                    } else if ("arcMotion".equals(name)) {
                        parent.setPathMotion(new ArcMotion(this.mContext, attrs));
                    } else if ("pathMotion".equals(name)) {
                        parent.setPathMotion((PathMotion) createCustom(attrs, PathMotion.class, "pathMotion"));
                    } else if ("patternPathMotion".equals(name)) {
                        parent.setPathMotion(new PatternPathMotion(this.mContext, attrs));
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown scene name: ");
                        stringBuilder.append(parser.getName());
                        throw new RuntimeException(stringBuilder.toString());
                    }
                    if (transition != null) {
                        if (!parser.isEmptyElementTag()) {
                            createTransitionFromXml(parser, attrs, transition);
                        }
                        if (transitionSet != null) {
                            transitionSet.addTransition(transition);
                            transition = null;
                        } else if (parent != null) {
                            throw new InflateException("Could not add transition to another transition.");
                        }
                    }
                }
            }
        }
        return transition;
    }

    private Object createCustom(AttributeSet attrs, Class expectedType, String tag) {
        StringBuilder stringBuilder;
        String className = attrs.getAttributeValue(null, "class");
        if (className != null) {
            try {
                Object newInstance;
                synchronized (sConstructors) {
                    Constructor constructor = (Constructor) sConstructors.get(className);
                    if (constructor == null) {
                        Class c = this.mContext.getClassLoader().loadClass(className).asSubclass(expectedType);
                        if (c != null) {
                            constructor = c.getConstructor(sConstructorSignature);
                            constructor.setAccessible(true);
                            sConstructors.put(className, constructor);
                        }
                    }
                    newInstance = constructor.newInstance(new Object[]{this.mContext, attrs});
                }
                return newInstance;
            } catch (InstantiationException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not instantiate ");
                stringBuilder.append(expectedType);
                stringBuilder.append(" class ");
                stringBuilder.append(className);
                throw new InflateException(stringBuilder.toString(), e);
            } catch (ClassNotFoundException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not instantiate ");
                stringBuilder.append(expectedType);
                stringBuilder.append(" class ");
                stringBuilder.append(className);
                throw new InflateException(stringBuilder.toString(), e2);
            } catch (InvocationTargetException e3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not instantiate ");
                stringBuilder.append(expectedType);
                stringBuilder.append(" class ");
                stringBuilder.append(className);
                throw new InflateException(stringBuilder.toString(), e3);
            } catch (NoSuchMethodException e4) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not instantiate ");
                stringBuilder.append(expectedType);
                stringBuilder.append(" class ");
                stringBuilder.append(className);
                throw new InflateException(stringBuilder.toString(), e4);
            } catch (IllegalAccessException e5) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not instantiate ");
                stringBuilder.append(expectedType);
                stringBuilder.append(" class ");
                stringBuilder.append(className);
                throw new InflateException(stringBuilder.toString(), e5);
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(tag);
        stringBuilder2.append(" tag must have a 'class' attribute");
        throw new InflateException(stringBuilder2.toString());
    }

    private void getTargetIds(XmlPullParser parser, AttributeSet attrs, Transition transition) throws XmlPullParserException, IOException {
        int depth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if ((next == 3 && parser.getDepth() <= depth) || type == 1) {
                return;
            }
            if (type == 2) {
                if (parser.getName().equals("target")) {
                    TypedArray a = this.mContext.obtainStyledAttributes(attrs, R.styleable.TransitionTarget);
                    int id = a.getResourceId(1, 0);
                    if (id != 0) {
                        transition.addTarget(id);
                    } else {
                        int resourceId = a.getResourceId(2, 0);
                        id = resourceId;
                        if (resourceId != 0) {
                            transition.excludeTarget(id, true);
                        } else {
                            String string = a.getString(4);
                            String transitionName = string;
                            if (string != null) {
                                transition.addTarget(transitionName);
                            } else {
                                string = a.getString(5);
                                transitionName = string;
                                if (string != null) {
                                    transition.excludeTarget(transitionName, true);
                                } else {
                                    String className = a.getString(3);
                                    if (className != null) {
                                        try {
                                            transition.excludeTarget(Class.forName(className), true);
                                        } catch (ClassNotFoundException e) {
                                            a.recycle();
                                            StringBuilder stringBuilder = new StringBuilder();
                                            stringBuilder.append("Could not create ");
                                            stringBuilder.append(className);
                                            throw new RuntimeException(stringBuilder.toString(), e);
                                        }
                                    }
                                    String string2 = a.getString(0);
                                    className = string2;
                                    if (string2 != null) {
                                        transition.addTarget(Class.forName(className));
                                    }
                                }
                            }
                        }
                    }
                    a.recycle();
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unknown scene name: ");
                    stringBuilder2.append(parser.getName());
                    throw new RuntimeException(stringBuilder2.toString());
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:20:0x0058, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private TransitionManager createTransitionManagerFromXml(XmlPullParser parser, AttributeSet attrs, ViewGroup sceneRoot) throws XmlPullParserException, IOException {
        StringBuilder stringBuilder;
        int depth = parser.getDepth();
        TransitionManager transitionManager = null;
        while (true) {
            int next = parser.next();
            int type = next;
            if ((next != 3 || parser.getDepth() > depth) && type != 1) {
                if (type == 2) {
                    String name = parser.getName();
                    if (name.equals("transitionManager")) {
                        transitionManager = new TransitionManager();
                    } else if (!name.equals("transition") || transitionManager == null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown scene name: ");
                        stringBuilder.append(parser.getName());
                    } else {
                        loadTransition(attrs, sceneRoot, transitionManager);
                    }
                }
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown scene name: ");
        stringBuilder.append(parser.getName());
        throw new RuntimeException(stringBuilder.toString());
    }

    private void loadTransition(AttributeSet attrs, ViewGroup sceneRoot, TransitionManager transitionManager) throws NotFoundException {
        TypedArray a = this.mContext.obtainStyledAttributes(attrs, R.styleable.TransitionManager);
        int transitionId = a.getResourceId(2, -1);
        int fromId = a.getResourceId(0, -1);
        Scene toScene = null;
        Scene fromScene = fromId < 0 ? null : Scene.getSceneForLayout(sceneRoot, fromId, this.mContext);
        int toId = a.getResourceId(1, -1);
        if (toId >= 0) {
            toScene = Scene.getSceneForLayout(sceneRoot, toId, this.mContext);
        }
        if (transitionId >= 0) {
            Transition transition = inflateTransition(transitionId);
            if (transition != null) {
                if (toScene == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No toScene for transition ID ");
                    stringBuilder.append(transitionId);
                    throw new RuntimeException(stringBuilder.toString());
                } else if (fromScene == null) {
                    transitionManager.setTransition(toScene, transition);
                } else {
                    transitionManager.setTransition(fromScene, toScene, transition);
                }
            }
        }
        a.recycle();
    }
}
