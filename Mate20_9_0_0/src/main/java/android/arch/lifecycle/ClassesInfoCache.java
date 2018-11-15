package android.arch.lifecycle;

import android.arch.lifecycle.Lifecycle.Event;
import android.support.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class ClassesInfoCache {
    private static final int CALL_TYPE_NO_ARG = 0;
    private static final int CALL_TYPE_PROVIDER = 1;
    private static final int CALL_TYPE_PROVIDER_WITH_EVENT = 2;
    static ClassesInfoCache sInstance = new ClassesInfoCache();
    private final Map<Class, CallbackInfo> mCallbackMap = new HashMap();
    private final Map<Class, Boolean> mHasLifecycleMethods = new HashMap();

    static class CallbackInfo {
        final Map<Event, List<MethodReference>> mEventToHandlers = new HashMap();
        final Map<MethodReference, Event> mHandlerToEvent;

        CallbackInfo(Map<MethodReference, Event> handlerToEvent) {
            this.mHandlerToEvent = handlerToEvent;
            for (Entry<MethodReference, Event> entry : handlerToEvent.entrySet()) {
                Event event = (Event) entry.getValue();
                List<MethodReference> methodReferences = (List) this.mEventToHandlers.get(event);
                if (methodReferences == null) {
                    methodReferences = new ArrayList();
                    this.mEventToHandlers.put(event, methodReferences);
                }
                methodReferences.add(entry.getKey());
            }
        }

        void invokeCallbacks(LifecycleOwner source, Event event, Object target) {
            invokeMethodsForEvent((List) this.mEventToHandlers.get(event), source, event, target);
            invokeMethodsForEvent((List) this.mEventToHandlers.get(Event.ON_ANY), source, event, target);
        }

        private static void invokeMethodsForEvent(List<MethodReference> handlers, LifecycleOwner source, Event event, Object mWrapped) {
            if (handlers != null) {
                for (int i = handlers.size() - 1; i >= 0; i--) {
                    ((MethodReference) handlers.get(i)).invokeCallback(source, event, mWrapped);
                }
            }
        }
    }

    static class MethodReference {
        final int mCallType;
        final Method mMethod;

        MethodReference(int callType, Method method) {
            this.mCallType = callType;
            this.mMethod = method;
            this.mMethod.setAccessible(true);
        }

        void invokeCallback(LifecycleOwner source, Event event, Object target) {
            try {
                switch (this.mCallType) {
                    case 0:
                        this.mMethod.invoke(target, new Object[0]);
                        return;
                    case 1:
                        this.mMethod.invoke(target, new Object[]{source});
                        return;
                    case 2:
                        this.mMethod.invoke(target, new Object[]{source, event});
                        return;
                    default:
                        return;
                }
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Failed to call observer method", e.getCause());
            } catch (IllegalAccessException e2) {
                throw new RuntimeException(e2);
            }
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MethodReference that = (MethodReference) o;
            if (!(this.mCallType == that.mCallType && this.mMethod.getName().equals(that.mMethod.getName()))) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return (31 * this.mCallType) + this.mMethod.getName().hashCode();
        }
    }

    ClassesInfoCache() {
    }

    boolean hasLifecycleMethods(Class klass) {
        if (this.mHasLifecycleMethods.containsKey(klass)) {
            return ((Boolean) this.mHasLifecycleMethods.get(klass)).booleanValue();
        }
        Method[] methods = getDeclaredMethods(klass);
        for (Method method : methods) {
            if (((OnLifecycleEvent) method.getAnnotation(OnLifecycleEvent.class)) != null) {
                createInfo(klass, methods);
                return true;
            }
        }
        this.mHasLifecycleMethods.put(klass, Boolean.valueOf(false));
        return false;
    }

    private Method[] getDeclaredMethods(Class klass) {
        try {
            return klass.getDeclaredMethods();
        } catch (NoClassDefFoundError e) {
            throw new IllegalArgumentException("The observer class has some methods that use newer APIs which are not available in the current OS version. Lifecycles cannot access even other methods so you should make sure that your observer classes only access framework classes that are available in your min API level OR use lifecycle:compiler annotation processor.", e);
        }
    }

    CallbackInfo getInfo(Class klass) {
        CallbackInfo existing = (CallbackInfo) this.mCallbackMap.get(klass);
        if (existing != null) {
            return existing;
        }
        return createInfo(klass, null);
    }

    private void verifyAndPutHandler(Map<MethodReference, Event> handlers, MethodReference newHandler, Event newEvent, Class klass) {
        Event event = (Event) handlers.get(newHandler);
        if (event != null && newEvent != event) {
            Method method = newHandler.mMethod;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Method ");
            stringBuilder.append(method.getName());
            stringBuilder.append(" in ");
            stringBuilder.append(klass.getName());
            stringBuilder.append(" already declared with different @OnLifecycleEvent value: previous value ");
            stringBuilder.append(event);
            stringBuilder.append(", new value ");
            stringBuilder.append(newEvent);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (event == null) {
            handlers.put(newHandler, newEvent);
        }
    }

    private CallbackInfo createInfo(Class klass, @Nullable Method[] declaredMethods) {
        Class cls = klass;
        Class superclass = klass.getSuperclass();
        Map<MethodReference, Event> handlerToEvent = new HashMap();
        if (superclass != null) {
            CallbackInfo superInfo = getInfo(superclass);
            if (superInfo != null) {
                handlerToEvent.putAll(superInfo.mHandlerToEvent);
            }
        }
        int i = 0;
        for (Class intrfc : klass.getInterfaces()) {
            for (Entry<MethodReference, Event> entry : getInfo(intrfc).mHandlerToEvent.entrySet()) {
                verifyAndPutHandler(handlerToEvent, (MethodReference) entry.getKey(), (Event) entry.getValue(), cls);
            }
        }
        Method[] methods = declaredMethods != null ? declaredMethods : getDeclaredMethods(klass);
        int length = methods.length;
        boolean hasLifecycleMethods = false;
        int hasLifecycleMethods2 = 0;
        while (hasLifecycleMethods2 < length) {
            Class superclass2;
            Method method = methods[hasLifecycleMethods2];
            OnLifecycleEvent annotation = (OnLifecycleEvent) method.getAnnotation(OnLifecycleEvent.class);
            if (annotation == null) {
                superclass2 = superclass;
            } else {
                hasLifecycleMethods = true;
                Class<?>[] params = method.getParameterTypes();
                int callType = 0;
                if (params.length > 0) {
                    callType = 1;
                    if (!params[i].isAssignableFrom(LifecycleOwner.class)) {
                        throw new IllegalArgumentException("invalid parameter type. Must be one and instanceof LifecycleOwner");
                    }
                }
                Event event = annotation.value();
                superclass2 = superclass;
                if (params.length > 1) {
                    callType = 2;
                    if (params[1].isAssignableFrom(Event.class) == null) {
                        throw new IllegalArgumentException("invalid parameter type. second arg must be an event");
                    } else if (event != Event.ON_ANY) {
                        throw new IllegalArgumentException("Second arg is supported only for ON_ANY value");
                    }
                }
                if (params.length <= 2) {
                    verifyAndPutHandler(handlerToEvent, new MethodReference(callType, method), event, cls);
                } else {
                    throw new IllegalArgumentException("cannot have more than 2 params");
                }
            }
            hasLifecycleMethods2++;
            superclass = superclass2;
            i = 0;
        }
        CallbackInfo info = new CallbackInfo(handlerToEvent);
        this.mCallbackMap.put(cls, info);
        this.mHasLifecycleMethods.put(cls, Boolean.valueOf(hasLifecycleMethods));
        return info;
    }
}
