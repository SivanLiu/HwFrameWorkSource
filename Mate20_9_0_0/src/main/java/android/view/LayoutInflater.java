package android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.Trace;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import com.android.internal.R;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class LayoutInflater {
    private static final int[] ATTRS_THEME = new int[]{16842752};
    private static final String ATTR_LAYOUT = "layout";
    private static final ClassLoader BOOT_CLASS_LOADER = LayoutInflater.class.getClassLoader();
    private static final boolean DEBUG = false;
    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];
    private static final String TAG = LayoutInflater.class.getSimpleName();
    private static final String TAG_1995 = "blink";
    private static final String TAG_INCLUDE = "include";
    private static final String TAG_MERGE = "merge";
    private static final String TAG_REQUEST_FOCUS = "requestFocus";
    private static final String TAG_TAG = "tag";
    static final Class<?>[] mConstructorSignature = new Class[]{Context.class, AttributeSet.class};
    private static final HashMap<String, Constructor<? extends View>> sConstructorMap = new HashMap();
    final Object[] mConstructorArgs;
    protected final Context mContext;
    private Factory mFactory;
    private Factory2 mFactory2;
    private boolean mFactorySet;
    private Filter mFilter;
    private HashMap<String, Boolean> mFilterMap;
    private Factory2 mPrivateFactory;
    private TypedValue mTempValue;

    public interface Factory {
        View onCreateView(String str, Context context, AttributeSet attributeSet);
    }

    public interface Filter {
        boolean onLoadClass(Class cls);
    }

    public interface Factory2 extends Factory {
        View onCreateView(View view, String str, Context context, AttributeSet attributeSet);
    }

    private static class FactoryMerger implements Factory2 {
        private final Factory mF1;
        private final Factory2 mF12;
        private final Factory mF2;
        private final Factory2 mF22;

        FactoryMerger(Factory f1, Factory2 f12, Factory f2, Factory2 f22) {
            this.mF1 = f1;
            this.mF2 = f2;
            this.mF12 = f12;
            this.mF22 = f22;
        }

        public View onCreateView(String name, Context context, AttributeSet attrs) {
            View v = this.mF1.onCreateView(name, context, attrs);
            if (v != null) {
                return v;
            }
            return this.mF2.onCreateView(name, context, attrs);
        }

        public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
            View v;
            if (this.mF12 != null) {
                v = this.mF12.onCreateView(parent, name, context, attrs);
            } else {
                v = this.mF1.onCreateView(name, context, attrs);
            }
            if (v != null) {
                return v;
            }
            View onCreateView;
            if (this.mF22 != null) {
                onCreateView = this.mF22.onCreateView(parent, name, context, attrs);
            } else {
                onCreateView = this.mF2.onCreateView(name, context, attrs);
            }
            return onCreateView;
        }
    }

    private static class BlinkLayout extends FrameLayout {
        private static final int BLINK_DELAY = 500;
        private static final int MESSAGE_BLINK = 66;
        private boolean mBlink;
        private boolean mBlinkState;
        private final Handler mHandler = new Handler(new Callback() {
            public boolean handleMessage(Message msg) {
                if (msg.what != 66) {
                    return false;
                }
                if (BlinkLayout.this.mBlink) {
                    BlinkLayout.this.mBlinkState = BlinkLayout.this.mBlinkState ^ 1;
                    BlinkLayout.this.makeBlink();
                }
                BlinkLayout.this.invalidate();
                return true;
            }
        });

        public BlinkLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        private void makeBlink() {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(66), 500);
        }

        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            this.mBlink = true;
            this.mBlinkState = true;
            makeBlink();
        }

        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            this.mBlink = false;
            this.mBlinkState = true;
            this.mHandler.removeMessages(66);
        }

        protected void dispatchDraw(Canvas canvas) {
            if (this.mBlinkState) {
                super.dispatchDraw(canvas);
            }
        }
    }

    public abstract LayoutInflater cloneInContext(Context context);

    protected LayoutInflater(Context context) {
        this.mConstructorArgs = new Object[2];
        this.mContext = context;
    }

    protected LayoutInflater(LayoutInflater original, Context newContext) {
        this.mConstructorArgs = new Object[2];
        this.mContext = newContext;
        this.mFactory = original.mFactory;
        this.mFactory2 = original.mFactory2;
        this.mPrivateFactory = original.mPrivateFactory;
        setFilter(original.mFilter);
    }

    public static LayoutInflater from(Context context) {
        LayoutInflater LayoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        if (LayoutInflater != null) {
            return LayoutInflater;
        }
        throw new AssertionError("LayoutInflater not found.");
    }

    public Context getContext() {
        return this.mContext;
    }

    public final Factory getFactory() {
        return this.mFactory;
    }

    public final Factory2 getFactory2() {
        return this.mFactory2;
    }

    public void setFactory(Factory factory) {
        if (this.mFactorySet) {
            throw new IllegalStateException("A factory has already been set on this LayoutInflater");
        } else if (factory != null) {
            this.mFactorySet = true;
            if (this.mFactory == null) {
                this.mFactory = factory;
            } else {
                this.mFactory = new FactoryMerger(factory, null, this.mFactory, this.mFactory2);
            }
        } else {
            throw new NullPointerException("Given factory can not be null");
        }
    }

    public void setFactory2(Factory2 factory) {
        if (this.mFactorySet) {
            throw new IllegalStateException("A factory has already been set on this LayoutInflater");
        } else if (factory != null) {
            this.mFactorySet = true;
            if (this.mFactory == null) {
                this.mFactory2 = factory;
                this.mFactory = factory;
                return;
            }
            FactoryMerger factoryMerger = new FactoryMerger(factory, factory, this.mFactory, this.mFactory2);
            this.mFactory2 = factoryMerger;
            this.mFactory = factoryMerger;
        } else {
            throw new NullPointerException("Given factory can not be null");
        }
    }

    public void setPrivateFactory(Factory2 factory) {
        if (this.mPrivateFactory == null) {
            this.mPrivateFactory = factory;
        } else {
            this.mPrivateFactory = new FactoryMerger(factory, factory, this.mPrivateFactory, this.mPrivateFactory);
        }
    }

    public Filter getFilter() {
        return this.mFilter;
    }

    public void setFilter(Filter filter) {
        this.mFilter = filter;
        if (filter != null) {
            this.mFilterMap = new HashMap();
        }
    }

    public View inflate(int resource, ViewGroup root) {
        return inflate(resource, root, root != null);
    }

    public View inflate(XmlPullParser parser, ViewGroup root) {
        return inflate(parser, root, root != null);
    }

    public View inflate(int resource, ViewGroup root, boolean attachToRoot) {
        XmlPullParser parser = getContext().getResources().getLayout(resource);
        try {
            View inflate = inflate(parser, root, attachToRoot);
            return inflate;
        } finally {
            parser.close();
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:63:0x0111=Splitter:B:63:0x0111, B:35:0x008c=Splitter:B:35:0x008c} */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00a9  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0037 A:{Catch:{ XmlPullParserException -> 0x00fd, Exception -> 0x00d2, all -> 0x00cd, all -> 0x0110 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public View inflate(XmlPullParser parser, ViewGroup root, boolean attachToRoot) {
        XmlPullParserException e;
        InflateException ie;
        Exception e2;
        StringBuilder stringBuilder;
        Throwable th;
        View view = root;
        synchronized (this.mConstructorArgs) {
            XmlPullParser xmlPullParser;
            try {
                int type;
                int i = 8;
                Trace.traceBegin(8, "inflate");
                Context inflaterContext = this.mContext;
                AttributeSet attrs = Xml.asAttributeSet(parser);
                Context lastContext = (Context) this.mConstructorArgs[0];
                this.mConstructorArgs[0] = inflaterContext;
                View result = view;
                while (true) {
                    View result2 = result;
                    try {
                        int next = parser.next();
                        type = next;
                        if (next != 2 && type != 1) {
                            result = result2;
                        } else if (type != 2) {
                            String name = parser.getName();
                            if (!TAG_MERGE.equals(name)) {
                                i = 1;
                                View temp = createViewFromTag(view, name, inflaterContext, attrs);
                                LayoutParams params = null;
                                if (view != null) {
                                    params = view.generateLayoutParams(attrs);
                                    if (!attachToRoot) {
                                        temp.setLayoutParams(params);
                                    }
                                }
                                try {
                                    rInflateChildren(parser, temp, attrs, i);
                                    if (view != null && attachToRoot) {
                                        view.addView(temp, params);
                                    }
                                    if (view == null || !attachToRoot) {
                                        result2 = temp;
                                    }
                                } catch (XmlPullParserException e3) {
                                    e = e3;
                                    ie = new InflateException(e.getMessage(), e);
                                    ie.setStackTrace(EMPTY_STACK_TRACE);
                                    throw ie;
                                } catch (Exception e4) {
                                    e2 = e4;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(parser.getPositionDescription());
                                    stringBuilder.append(": ");
                                    stringBuilder.append(e2.getMessage());
                                    ie = new InflateException(stringBuilder.toString(), e2);
                                    ie.setStackTrace(EMPTY_STACK_TRACE);
                                    throw ie;
                                }
                            } else if (view == null || !attachToRoot) {
                                i = 1;
                                throw new InflateException("<merge /> can be used only with a valid ViewGroup root and attachToRoot=true");
                            } else {
                                i = 1;
                                try {
                                    rInflate(parser, view, inflaterContext, attrs, false);
                                    xmlPullParser = parser;
                                } catch (XmlPullParserException e5) {
                                    e = e5;
                                    xmlPullParser = parser;
                                    ie = new InflateException(e.getMessage(), e);
                                    ie.setStackTrace(EMPTY_STACK_TRACE);
                                    throw ie;
                                } catch (Exception e6) {
                                    e2 = e6;
                                    xmlPullParser = parser;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(parser.getPositionDescription());
                                    stringBuilder.append(": ");
                                    stringBuilder.append(e2.getMessage());
                                    ie = new InflateException(stringBuilder.toString(), e2);
                                    ie.setStackTrace(EMPTY_STACK_TRACE);
                                    throw ie;
                                } catch (Throwable th2) {
                                    th = th2;
                                    xmlPullParser = parser;
                                    this.mConstructorArgs[0] = lastContext;
                                    this.mConstructorArgs[i] = null;
                                    Trace.traceEnd(8);
                                    throw th;
                                }
                            }
                            this.mConstructorArgs[0] = lastContext;
                            this.mConstructorArgs[i] = null;
                            Trace.traceEnd(8);
                            return result2;
                        } else {
                            xmlPullParser = parser;
                            int i2 = type;
                            i = 1;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(parser.getPositionDescription());
                            stringBuilder2.append(": No start tag found!");
                            throw new InflateException(stringBuilder2.toString());
                        }
                    } catch (XmlPullParserException e7) {
                        e = e7;
                        xmlPullParser = parser;
                        i = 1;
                        ie = new InflateException(e.getMessage(), e);
                        ie.setStackTrace(EMPTY_STACK_TRACE);
                        throw ie;
                    } catch (Exception e8) {
                        e2 = e8;
                        xmlPullParser = parser;
                        i = 1;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(parser.getPositionDescription());
                        stringBuilder.append(": ");
                        stringBuilder.append(e2.getMessage());
                        ie = new InflateException(stringBuilder.toString(), e2);
                        ie.setStackTrace(EMPTY_STACK_TRACE);
                        throw ie;
                    } catch (Throwable th3) {
                        th = th3;
                        this.mConstructorArgs[0] = lastContext;
                        this.mConstructorArgs[i] = null;
                        Trace.traceEnd(8);
                        throw th;
                    }
                }
                if (type != 2) {
                }
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    private final boolean verifyClassLoader(Constructor<? extends View> constructor) {
        ClassLoader constructorLoader = constructor.getDeclaringClass().getClassLoader();
        if (constructorLoader == BOOT_CLASS_LOADER) {
            return true;
        }
        ClassLoader cl = this.mContext.getClassLoader();
        while (constructorLoader != cl) {
            cl = cl.getParent();
            if (cl == null) {
                return false;
            }
        }
        return true;
    }

    public final View createView(String name, String prefix, AttributeSet attrs) throws ClassNotFoundException, InflateException {
        Constructor<? extends View> constructor;
        StringBuilder stringBuilder;
        InflateException ie;
        synchronized (sConstructorMap) {
            constructor = (Constructor) sConstructorMap.get(name);
        }
        if (!(constructor == null || verifyClassLoader(constructor))) {
            synchronized (sConstructorMap) {
                sConstructorMap.remove(name);
            }
            constructor = null;
        }
        Class<? extends View> clazz = null;
        StringBuilder stringBuilder2;
        String stringBuilder3;
        try {
            Trace.traceBegin(8, name);
            if (constructor == null) {
                ClassLoader classLoader = this.mContext.getClassLoader();
                if (prefix != null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(prefix);
                    stringBuilder2.append(name);
                    stringBuilder3 = stringBuilder2.toString();
                } else {
                    stringBuilder3 = name;
                }
                clazz = classLoader.loadClass(stringBuilder3).asSubclass(View.class);
                if (!(this.mFilter == null || clazz == null || this.mFilter.onLoadClass(clazz))) {
                    failNotAllowed(name, prefix, attrs);
                }
                constructor = clazz.getConstructor(mConstructorSignature);
                constructor.setAccessible(true);
                synchronized (sConstructorMap) {
                    sConstructorMap.put(name, constructor);
                }
            } else if (this.mFilter != null) {
                Boolean allowedState = (Boolean) this.mFilterMap.get(name);
                if (allowedState == null) {
                    String stringBuilder4;
                    ClassLoader classLoader2 = this.mContext.getClassLoader();
                    if (prefix != null) {
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append(prefix);
                        stringBuilder5.append(name);
                        stringBuilder4 = stringBuilder5.toString();
                    } else {
                        stringBuilder4 = name;
                    }
                    clazz = classLoader2.loadClass(stringBuilder4).asSubclass(View.class);
                    boolean allowed = clazz != null && this.mFilter.onLoadClass(clazz);
                    this.mFilterMap.put(name, Boolean.valueOf(allowed));
                    if (!allowed) {
                        failNotAllowed(name, prefix, attrs);
                    }
                } else if (allowedState.equals(Boolean.FALSE)) {
                    failNotAllowed(name, prefix, attrs);
                }
            }
            Object lastContext = this.mConstructorArgs[0];
            if (this.mConstructorArgs[0] == null) {
                this.mConstructorArgs[0] = this.mContext;
            }
            Object[] args = this.mConstructorArgs;
            args[1] = attrs;
            View view = (View) constructor.newInstance(args);
            if (view instanceof ViewStub) {
                ((ViewStub) view).setLayoutInflater(cloneInContext((Context) args[0]));
            }
            this.mConstructorArgs[0] = lastContext;
            Trace.traceEnd(8);
            return view;
        } catch (NoSuchMethodException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(attrs.getPositionDescription());
            stringBuilder.append(": Error inflating class ");
            if (prefix != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append(name);
                stringBuilder3 = stringBuilder2.toString();
            } else {
                stringBuilder3 = name;
            }
            stringBuilder.append(stringBuilder3);
            ie = new InflateException(stringBuilder.toString(), e);
            ie.setStackTrace(EMPTY_STACK_TRACE);
            throw ie;
        } catch (ClassCastException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(attrs.getPositionDescription());
            stringBuilder.append(": Class is not a View ");
            if (prefix != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append(name);
                stringBuilder3 = stringBuilder2.toString();
            } else {
                stringBuilder3 = name;
            }
            stringBuilder.append(stringBuilder3);
            ie = new InflateException(stringBuilder.toString(), e2);
            ie.setStackTrace(EMPTY_STACK_TRACE);
            throw ie;
        } catch (ClassNotFoundException e3) {
            throw e3;
        } catch (Exception e4) {
            try {
                stringBuilder = new StringBuilder();
                stringBuilder.append(attrs.getPositionDescription());
                stringBuilder.append(": Error inflating class ");
                stringBuilder.append(clazz == null ? MediaStore.UNKNOWN_STRING : clazz.getName());
                ie = new InflateException(stringBuilder.toString(), e4);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
            } catch (Throwable th) {
                Trace.traceEnd(8);
            }
        }
    }

    private void failNotAllowed(String name, String prefix, AttributeSet attrs) {
        String stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(attrs.getPositionDescription());
        stringBuilder2.append(": Class not allowed to be inflated ");
        if (prefix != null) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(prefix);
            stringBuilder3.append(name);
            stringBuilder = stringBuilder3.toString();
        } else {
            stringBuilder = name;
        }
        stringBuilder2.append(stringBuilder);
        throw new InflateException(stringBuilder2.toString());
    }

    protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
        return createView(name, "android.view.", attrs);
    }

    protected View onCreateView(View parent, String name, AttributeSet attrs) throws ClassNotFoundException {
        return onCreateView(name, attrs);
    }

    private View createViewFromTag(View parent, String name, Context context, AttributeSet attrs) {
        return createViewFromTag(parent, name, context, attrs, false);
    }

    /* JADX WARNING: Removed duplicated region for block: B:27:0x005c A:{Catch:{ all -> 0x007d, InflateException -> 0x00cc, ClassNotFoundException -> 0x00a8, Exception -> 0x0084 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    View createViewFromTag(View parent, String name, Context context, AttributeSet attrs, boolean ignoreThemeAttr) {
        StringBuilder stringBuilder;
        InflateException ie;
        if (name.equals("view")) {
            name = attrs.getAttributeValue(null, "class");
        }
        if (!ignoreThemeAttr) {
            TypedArray ta = context.obtainStyledAttributes(attrs, ATTRS_THEME);
            int themeResId = ta.getResourceId(0, 0);
            if (themeResId != 0) {
                context = new ContextThemeWrapper(context, themeResId);
            }
            ta.recycle();
        }
        if (name.equals(TAG_1995)) {
            return new BlinkLayout(context, attrs);
        }
        Object lastContext;
        try {
            View view;
            if (this.mFactory2 != null) {
                view = this.mFactory2.onCreateView(parent, name, context, attrs);
            } else if (this.mFactory != null) {
                view = this.mFactory.onCreateView(name, context, attrs);
            } else {
                view = null;
                if (view == null && this.mPrivateFactory != null) {
                    view = this.mPrivateFactory.onCreateView(parent, name, context, attrs);
                }
                if (view == null) {
                    View view2;
                    lastContext = this.mConstructorArgs[0];
                    this.mConstructorArgs[0] = context;
                    if (-1 == name.indexOf(46)) {
                        view2 = onCreateView(parent, name, attrs);
                    } else {
                        view2 = createView(name, null, attrs);
                    }
                    view = view2;
                    this.mConstructorArgs[0] = lastContext;
                }
                return view;
            }
            view = this.mPrivateFactory.onCreateView(parent, name, context, attrs);
            if (view == null) {
            }
            return view;
        } catch (InflateException e) {
            throw e;
        } catch (ClassNotFoundException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(attrs.getPositionDescription());
            stringBuilder.append(": Error inflating class ");
            stringBuilder.append(name);
            ie = new InflateException(stringBuilder.toString(), e2);
            ie.setStackTrace(EMPTY_STACK_TRACE);
            throw ie;
        } catch (Exception e3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(attrs.getPositionDescription());
            stringBuilder.append(": Error inflating class ");
            stringBuilder.append(name);
            ie = new InflateException(stringBuilder.toString(), e3);
            ie.setStackTrace(EMPTY_STACK_TRACE);
            throw ie;
        } catch (Throwable th) {
            this.mConstructorArgs[0] = lastContext;
        }
    }

    final void rInflateChildren(XmlPullParser parser, View parent, AttributeSet attrs, boolean finishInflate) throws XmlPullParserException, IOException {
        rInflate(parser, parent, parent.getContext(), attrs, finishInflate);
    }

    void rInflate(XmlPullParser parser, View parent, Context context, AttributeSet attrs, boolean finishInflate) throws XmlPullParserException, IOException {
        int depth = parser.getDepth();
        boolean pendingRequestFocus = false;
        while (true) {
            int next = parser.next();
            int type = next;
            if ((next != 3 || parser.getDepth() > depth) && type != 1) {
                if (type == 2) {
                    String name = parser.getName();
                    if (TAG_REQUEST_FOCUS.equals(name)) {
                        pendingRequestFocus = true;
                        consumeChildElements(parser);
                    } else if ("tag".equals(name)) {
                        parseViewTag(parser, parent, attrs);
                    } else if (TAG_INCLUDE.equals(name)) {
                        if (parser.getDepth() != 0) {
                            parseInclude(parser, context, parent, attrs);
                        } else {
                            throw new InflateException("<include /> cannot be the root element");
                        }
                    } else if (TAG_MERGE.equals(name)) {
                        throw new InflateException("<merge /> must be the root element");
                    } else {
                        View view = createViewFromTag(parent, name, context, attrs);
                        ViewGroup viewGroup = (ViewGroup) parent;
                        LayoutParams params = viewGroup.generateLayoutParams(attrs);
                        rInflateChildren(parser, view, attrs, true);
                        viewGroup.addView(view, params);
                    }
                }
            }
        }
        if (pendingRequestFocus) {
            parent.restoreDefaultFocus();
        }
        if (finishInflate) {
            parent.onFinishInflate();
        }
    }

    private void parseViewTag(XmlPullParser parser, View view, AttributeSet attrs) throws XmlPullParserException, IOException {
        TypedArray ta = view.getContext().obtainStyledAttributes(attrs, R.styleable.ViewTag);
        view.setTag(ta.getResourceId(1, 0), ta.getText(0));
        ta.recycle();
        consumeChildElements(parser);
    }

    /* JADX WARNING: Removed duplicated region for block: B:76:0x0141  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x009a A:{Catch:{ all -> 0x0163 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseInclude(XmlPullParser parser, Context context, View parent, AttributeSet attrs) throws XmlPullParserException, IOException {
        Throwable th;
        Context context2 = context;
        View view = parent;
        AttributeSet attributeSet = attrs;
        if (view instanceof ViewGroup) {
            String value;
            TypedArray ta = context2.obtainStyledAttributes(attributeSet, ATTRS_THEME);
            int themeResId = ta.getResourceId(0, 0);
            boolean hasThemeOverride = themeResId != 0;
            if (hasThemeOverride) {
                context2 = new ContextThemeWrapper(context2, themeResId);
            }
            Context context3 = context2;
            ta.recycle();
            int layout = attributeSet.getAttributeResourceValue(null, ATTR_LAYOUT, 0);
            if (layout == 0) {
                value = attributeSet.getAttributeValue(null, ATTR_LAYOUT);
                if (value == null || value.length() <= 0) {
                    throw new InflateException("You must specify a layout in the include tag: <include layout=\"@layout/layoutID\" />");
                }
                layout = context3.getResources().getIdentifier(value.substring(1), "attr", context3.getPackageName());
            }
            if (this.mTempValue == null) {
                this.mTempValue = new TypedValue();
            }
            if (layout != 0 && context3.getTheme().resolveAttribute(layout, this.mTempValue, true)) {
                layout = this.mTempValue.resourceId;
            }
            int layout2 = layout;
            String str;
            if (layout2 != 0) {
                XmlResourceParser childParser = context3.getResources().getLayout(layout2);
                int i;
                try {
                    int type;
                    AttributeSet childAttrs = Xml.asAttributeSet(childParser);
                    while (true) {
                        AttributeSet childAttrs2 = childAttrs;
                        layout = childParser.next();
                        type = layout;
                        int type2;
                        AttributeSet childAttrs3;
                        if (layout != 2 && type != 1) {
                            childAttrs = childAttrs2;
                        } else if (type != 2) {
                            value = childParser.getName();
                            if (TAG_MERGE.equals(value)) {
                                type2 = type;
                                XmlResourceParser childParser2 = childParser;
                                try {
                                    rInflate(childParser, view, context3, childAttrs2, false);
                                    childParser = childParser2;
                                } catch (Throwable th2) {
                                    th = th2;
                                    childParser = childParser2;
                                    childParser.close();
                                    throw th;
                                }
                            }
                            View view2;
                            ViewGroup type3;
                            int visibility;
                            type2 = type;
                            i = layout2;
                            AttributeSet childAttrs4 = childAttrs2;
                            XmlResourceParser childParser3 = childParser;
                            str = null;
                            try {
                                LayoutParams params;
                                view2 = createViewFromTag(view, value, context3, childAttrs4, hasThemeOverride);
                                type3 = (ViewGroup) view;
                                childAttrs2 = context3.obtainStyledAttributes(attributeSet, R.styleable.Include);
                                layout2 = childAttrs2.getResourceId(0, -1);
                                visibility = childAttrs2.getInt(1, -1);
                                childAttrs2.recycle();
                                LayoutParams params2 = str;
                                try {
                                    params2 = type3.generateLayoutParams(attributeSet);
                                } catch (RuntimeException e) {
                                }
                                if (params2 == null) {
                                    childAttrs3 = childAttrs4;
                                    try {
                                        params = type3.generateLayoutParams(childAttrs3);
                                    } catch (Throwable th3) {
                                        th = th3;
                                        childParser = childParser3;
                                        childParser.close();
                                        throw th;
                                    }
                                }
                                childAttrs3 = childAttrs4;
                                params = params2;
                                view2.setLayoutParams(params);
                                childParser = childParser3;
                            } catch (Throwable th4) {
                                th = th4;
                                childParser = childParser3;
                                childParser.close();
                                throw th;
                            }
                            try {
                                rInflateChildren(childParser, view2, childAttrs3, true);
                                if (layout2 != -1) {
                                    view2.setId(layout2);
                                }
                                switch (visibility) {
                                    case 0:
                                        view2.setVisibility(0);
                                        break;
                                    case 1:
                                        view2.setVisibility(4);
                                        break;
                                    case 2:
                                        view2.setVisibility(8);
                                        break;
                                    default:
                                        break;
                                }
                                type3.addView(view2);
                            } catch (Throwable th5) {
                                th = th5;
                                childParser.close();
                                throw th;
                            }
                            childParser.close();
                            consumeChildElements(parser);
                            return;
                        } else {
                            type2 = type;
                            childAttrs3 = childAttrs2;
                            i = layout2;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(childParser.getPositionDescription());
                            stringBuilder.append(": No start tag found!");
                            throw new InflateException(stringBuilder.toString());
                        }
                    }
                    if (type != 2) {
                    }
                } catch (Throwable th6) {
                    th = th6;
                    i = layout2;
                    childParser.close();
                    throw th;
                }
            }
            str = attributeSet.getAttributeValue(null, ATTR_LAYOUT);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("You must specify a valid layout reference. The layout ID ");
            stringBuilder2.append(str);
            stringBuilder2.append(" is not valid.");
            throw new InflateException(stringBuilder2.toString());
        }
        throw new InflateException("<include /> can only be used inside of a ViewGroup");
    }

    static final void consumeChildElements(XmlPullParser parser) throws XmlPullParserException, IOException {
        int currentDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if ((next == 3 && parser.getDepth() <= currentDepth) || type == 1) {
                return;
            }
        }
    }
}
