package android.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

abstract class GenericInflater<T, P extends Parent> {
    private static final Class[] mConstructorSignature = new Class[]{Context.class, AttributeSet.class};
    private static final HashMap sConstructorMap = new HashMap();
    private final boolean DEBUG;
    private final Object[] mConstructorArgs;
    protected final Context mContext;
    private String mDefaultPackage;
    private Factory<T> mFactory;
    private boolean mFactorySet;

    public interface Factory<T> {
        T onCreateItem(String str, Context context, AttributeSet attributeSet);
    }

    public interface Parent<T> {
        void addItemFromInflater(T t);
    }

    private static class FactoryMerger<T> implements Factory<T> {
        private final Factory<T> mF1;
        private final Factory<T> mF2;

        FactoryMerger(Factory<T> f1, Factory<T> f2) {
            this.mF1 = f1;
            this.mF2 = f2;
        }

        public T onCreateItem(String name, Context context, AttributeSet attrs) {
            T v = this.mF1.onCreateItem(name, context, attrs);
            if (v != null) {
                return v;
            }
            return this.mF2.onCreateItem(name, context, attrs);
        }
    }

    public abstract GenericInflater cloneInContext(Context context);

    protected GenericInflater(Context context) {
        this.DEBUG = false;
        this.mConstructorArgs = new Object[2];
        this.mContext = context;
    }

    protected GenericInflater(GenericInflater<T, P> original, Context newContext) {
        this.DEBUG = false;
        this.mConstructorArgs = new Object[2];
        this.mContext = newContext;
        this.mFactory = original.mFactory;
    }

    public void setDefaultPackage(String defaultPackage) {
        this.mDefaultPackage = defaultPackage;
    }

    public String getDefaultPackage() {
        return this.mDefaultPackage;
    }

    public Context getContext() {
        return this.mContext;
    }

    public final Factory<T> getFactory() {
        return this.mFactory;
    }

    public void setFactory(Factory<T> factory) {
        if (this.mFactorySet) {
            throw new IllegalStateException("A factory has already been set on this inflater");
        } else if (factory != null) {
            this.mFactorySet = true;
            if (this.mFactory == null) {
                this.mFactory = factory;
            } else {
                this.mFactory = new FactoryMerger(factory, this.mFactory);
            }
        } else {
            throw new NullPointerException("Given factory can not be null");
        }
    }

    public T inflate(int resource, P root) {
        return inflate(resource, (Parent) root, root != null);
    }

    public T inflate(XmlPullParser parser, P root) {
        return inflate(parser, (Parent) root, root != null);
    }

    public T inflate(int resource, P root, boolean attachToRoot) {
        XmlPullParser parser = getContext().getResources().getXml(resource);
        try {
            T inflate = inflate(parser, (Parent) root, attachToRoot);
            return inflate;
        } finally {
            parser.close();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x0033 A:{SYNTHETIC, Splitter:B:16:0x0033} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x001d A:{Catch:{ InflateException -> 0x0082, XmlPullParserException -> 0x0074, IOException -> 0x004e }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public T inflate(XmlPullParser parser, P root, boolean attachToRoot) {
        T result;
        InflateException ex;
        synchronized (this.mConstructorArgs) {
            int type;
            AttributeSet attrs = Xml.asAttributeSet(parser);
            this.mConstructorArgs[0] = this.mContext;
            result = root;
            while (true) {
                StringBuilder stringBuilder;
                try {
                    int next = parser.next();
                    type = next;
                    if (next == 2 || type == 1) {
                        if (type != 2) {
                            result = onMergeRoots(root, attachToRoot, (Parent) createItemFromTag(parser, parser.getName(), attrs));
                            rInflate(parser, result, attrs);
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(parser.getPositionDescription());
                            stringBuilder.append(": No start tag found!");
                            throw new InflateException(stringBuilder.toString());
                        }
                    }
                } catch (InflateException e) {
                    throw e;
                } catch (XmlPullParserException e2) {
                    ex = new InflateException(e2.getMessage());
                    ex.initCause(e2);
                    throw ex;
                } catch (IOException e3) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(parser.getPositionDescription());
                    stringBuilder.append(": ");
                    stringBuilder.append(e3.getMessage());
                    ex = new InflateException(stringBuilder.toString());
                    ex.initCause(e3);
                    throw ex;
                }
            }
            if (type != 2) {
            }
        }
        return result;
    }

    public final T createItem(String name, String prefix, AttributeSet attrs) throws ClassNotFoundException, InflateException {
        InflateException ie;
        Constructor constructor = (Constructor) sConstructorMap.get(name);
        if (constructor == null) {
            StringBuilder stringBuilder;
            try {
                String stringBuilder2;
                Class clazz = this.mContext.getClassLoader();
                if (prefix != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(prefix);
                    stringBuilder.append(name);
                    stringBuilder2 = stringBuilder.toString();
                } else {
                    stringBuilder2 = name;
                }
                constructor = clazz.loadClass(stringBuilder2).getConstructor(mConstructorSignature);
                constructor.setAccessible(true);
                sConstructorMap.put(name, constructor);
            } catch (NoSuchMethodException e) {
                String stringBuilder3;
                stringBuilder = new StringBuilder();
                stringBuilder.append(attrs.getPositionDescription());
                stringBuilder.append(": Error inflating class ");
                if (prefix != null) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append(prefix);
                    stringBuilder4.append(name);
                    stringBuilder3 = stringBuilder4.toString();
                } else {
                    stringBuilder3 = name;
                }
                stringBuilder.append(stringBuilder3);
                ie = new InflateException(stringBuilder.toString());
                ie.initCause(e);
                throw ie;
            } catch (ClassNotFoundException e2) {
                throw e2;
            } catch (Exception e22) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(attrs.getPositionDescription());
                stringBuilder.append(": Error inflating class ");
                stringBuilder.append(constructor.getClass().getName());
                ie = new InflateException(stringBuilder.toString());
                ie.initCause(e22);
                throw ie;
            }
        }
        Object[] args = this.mConstructorArgs;
        args[1] = attrs;
        return constructor.newInstance(args);
    }

    protected T onCreateItem(String name, AttributeSet attrs) throws ClassNotFoundException {
        return createItem(name, this.mDefaultPackage, attrs);
    }

    private final T createItemFromTag(XmlPullParser parser, String name, AttributeSet attrs) {
        StringBuilder stringBuilder;
        InflateException ie;
        try {
            T item = this.mFactory == null ? null : this.mFactory.onCreateItem(name, this.mContext, attrs);
            if (item != null) {
                return item;
            }
            if (-1 == name.indexOf(46)) {
                return onCreateItem(name, attrs);
            }
            return createItem(name, null, attrs);
        } catch (InflateException e) {
            throw e;
        } catch (ClassNotFoundException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(attrs.getPositionDescription());
            stringBuilder.append(": Error inflating class ");
            stringBuilder.append(name);
            ie = new InflateException(stringBuilder.toString());
            ie.initCause(e2);
            throw ie;
        } catch (Exception e3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(attrs.getPositionDescription());
            stringBuilder.append(": Error inflating class ");
            stringBuilder.append(name);
            ie = new InflateException(stringBuilder.toString());
            ie.initCause(e3);
            throw ie;
        }
    }

    private void rInflate(XmlPullParser parser, T parent, AttributeSet attrs) throws XmlPullParserException, IOException {
        int depth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if ((next == 3 && parser.getDepth() <= depth) || type == 1) {
                return;
            }
            if (type == 2) {
                if (!onCreateCustomFromTag(parser, parent, attrs)) {
                    T item = createItemFromTag(parser, parser.getName(), attrs);
                    ((Parent) parent).addItemFromInflater(item);
                    rInflate(parser, item, attrs);
                }
            }
        }
    }

    protected boolean onCreateCustomFromTag(XmlPullParser parser, T t, AttributeSet attrs) throws XmlPullParserException {
        return false;
    }

    protected P onMergeRoots(P p, boolean attachToGivenRoot, P xmlRoot) {
        return xmlRoot;
    }
}
