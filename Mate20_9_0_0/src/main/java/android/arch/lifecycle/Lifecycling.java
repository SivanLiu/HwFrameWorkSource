package android.arch.lifecycle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestrictTo({Scope.LIBRARY_GROUP})
public class Lifecycling {
    private static final int GENERATED_CALLBACK = 2;
    private static final int REFLECTIVE_CALLBACK = 1;
    private static Map<Class, Integer> sCallbackCache = new HashMap();
    private static Map<Class, List<Constructor<? extends GeneratedAdapter>>> sClassToAdapters = new HashMap();

    @NonNull
    static GenericLifecycleObserver getCallback(Object object) {
        if (object instanceof FullLifecycleObserver) {
            return new FullLifecycleObserverAdapter((FullLifecycleObserver) object);
        }
        if (object instanceof GenericLifecycleObserver) {
            return (GenericLifecycleObserver) object;
        }
        Class<?> klass = object.getClass();
        if (getObserverConstructorType(klass) != 2) {
            return new ReflectiveGenericLifecycleObserver(object);
        }
        List<Constructor<? extends GeneratedAdapter>> constructors = (List) sClassToAdapters.get(klass);
        int i = 0;
        if (constructors.size() == 1) {
            return new SingleGeneratedAdapterObserver(createGeneratedAdapter((Constructor) constructors.get(0), object));
        }
        GeneratedAdapter[] adapters = new GeneratedAdapter[constructors.size()];
        while (i < constructors.size()) {
            adapters[i] = createGeneratedAdapter((Constructor) constructors.get(i), object);
            i++;
        }
        return new CompositeGeneratedAdaptersObserver(adapters);
    }

    private static GeneratedAdapter createGeneratedAdapter(Constructor<? extends GeneratedAdapter> constructor, Object object) {
        try {
            return (GeneratedAdapter) constructor.newInstance(new Object[]{object});
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e2) {
            throw new RuntimeException(e2);
        } catch (InvocationTargetException e3) {
            throw new RuntimeException(e3);
        }
    }

    @Nullable
    private static Constructor<? extends GeneratedAdapter> generatedConstructor(Class<?> klass) {
        try {
            String adapterName;
            Class<? extends GeneratedAdapter> aClass;
            Package aPackage = klass.getPackage();
            String name = klass.getCanonicalName();
            String fullPackage = aPackage != null ? aPackage.getName() : "";
            if (fullPackage.isEmpty()) {
                adapterName = name;
            } else {
                adapterName = name.substring(fullPackage.length() + 1);
            }
            adapterName = getAdapterName(adapterName);
            if (fullPackage.isEmpty()) {
                aClass = adapterName;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(fullPackage);
                stringBuilder.append(".");
                stringBuilder.append(adapterName);
                aClass = stringBuilder.toString();
            }
            Constructor<? extends GeneratedAdapter> constructor = Class.forName(aClass).getDeclaredConstructor(new Class[]{klass});
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return constructor;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException e2) {
            throw new RuntimeException(e2);
        }
    }

    private static int getObserverConstructorType(Class<?> klass) {
        if (sCallbackCache.containsKey(klass)) {
            return ((Integer) sCallbackCache.get(klass)).intValue();
        }
        int type = resolveObserverCallbackType(klass);
        sCallbackCache.put(klass, Integer.valueOf(type));
        return type;
    }

    private static int resolveObserverCallbackType(Class<?> klass) {
        if (klass.getCanonicalName() == null) {
            return 1;
        }
        Constructor<? extends GeneratedAdapter> constructor = generatedConstructor(klass);
        if (constructor != null) {
            sClassToAdapters.put(klass, Collections.singletonList(constructor));
            return 2;
        } else if (ClassesInfoCache.sInstance.hasLifecycleMethods(klass)) {
            return 1;
        } else {
            Class<?> superclass = klass.getSuperclass();
            List<Constructor<? extends GeneratedAdapter>> adapterConstructors = null;
            if (isLifecycleParent(superclass)) {
                if (getObserverConstructorType(superclass) == 1) {
                    return 1;
                }
                adapterConstructors = new ArrayList((Collection) sClassToAdapters.get(superclass));
            }
            for (Class<?> intrface : klass.getInterfaces()) {
                if (isLifecycleParent(intrface)) {
                    if (getObserverConstructorType(intrface) == 1) {
                        return 1;
                    }
                    if (adapterConstructors == null) {
                        adapterConstructors = new ArrayList();
                    }
                    adapterConstructors.addAll((Collection) sClassToAdapters.get(intrface));
                }
            }
            if (adapterConstructors == null) {
                return 1;
            }
            sClassToAdapters.put(klass, adapterConstructors);
            return 2;
        }
    }

    private static boolean isLifecycleParent(Class<?> klass) {
        return klass != null && LifecycleObserver.class.isAssignableFrom(klass);
    }

    public static String getAdapterName(String className) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(className.replace(".", "_"));
        stringBuilder.append("_LifecycleAdapter");
        return stringBuilder.toString();
    }

    private Lifecycling() {
    }
}
