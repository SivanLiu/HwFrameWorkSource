package android.support.v4.app;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

@RequiresApi(28)
public class AppComponentFactory extends android.app.AppComponentFactory {
    public final Activity instantiateActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Activity) CoreComponentFactory.checkCompatWrapper(instantiateActivityCompat(cl, className, intent));
    }

    public final Application instantiateApplication(ClassLoader cl, String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Application) CoreComponentFactory.checkCompatWrapper(instantiateApplicationCompat(cl, className));
    }

    public final BroadcastReceiver instantiateReceiver(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (BroadcastReceiver) CoreComponentFactory.checkCompatWrapper(instantiateReceiverCompat(cl, className, intent));
    }

    public final ContentProvider instantiateProvider(ClassLoader cl, String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (ContentProvider) CoreComponentFactory.checkCompatWrapper(instantiateProviderCompat(cl, className));
    }

    public final Service instantiateService(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Service) CoreComponentFactory.checkCompatWrapper(instantiateServiceCompat(cl, className, intent));
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0014 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.reflect.InvocationTargetException (r0_4 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x0014, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x001c, code:
            throw new java.lang.RuntimeException("Couldn't call constructor", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @NonNull
    public Application instantiateApplicationCompat(@NonNull ClassLoader cl, @NonNull String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return (Application) cl.loadClass(className).getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (ReflectiveOperationException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0014 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.reflect.InvocationTargetException (r0_4 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x0014, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x001c, code:
            throw new java.lang.RuntimeException("Couldn't call constructor", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @NonNull
    public Activity instantiateActivityCompat(@NonNull ClassLoader cl, @NonNull String className, @Nullable Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return (Activity) cl.loadClass(className).getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (ReflectiveOperationException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0014 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.reflect.InvocationTargetException (r0_4 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x0014, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x001c, code:
            throw new java.lang.RuntimeException("Couldn't call constructor", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @NonNull
    public BroadcastReceiver instantiateReceiverCompat(@NonNull ClassLoader cl, @NonNull String className, @Nullable Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return (BroadcastReceiver) cl.loadClass(className).getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (ReflectiveOperationException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0014 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.reflect.InvocationTargetException (r0_4 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x0014, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x001c, code:
            throw new java.lang.RuntimeException("Couldn't call constructor", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @NonNull
    public Service instantiateServiceCompat(@NonNull ClassLoader cl, @NonNull String className, @Nullable Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return (Service) cl.loadClass(className).getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (ReflectiveOperationException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0014 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.reflect.InvocationTargetException (r0_4 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x0014, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x001c, code:
            throw new java.lang.RuntimeException("Couldn't call constructor", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @NonNull
    public ContentProvider instantiateProviderCompat(@NonNull ClassLoader cl, @NonNull String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return (ContentProvider) cl.loadClass(className).getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (ReflectiveOperationException e) {
        }
    }
}
