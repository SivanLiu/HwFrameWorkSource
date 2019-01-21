package android.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import junit.framework.Assert;
import junit.framework.TestCase;

@Deprecated
public class InstrumentationTestCase extends TestCase {
    private Instrumentation mInstrumentation;

    public void injectInstrumentation(Instrumentation instrumentation) {
        this.mInstrumentation = instrumentation;
    }

    @Deprecated
    public void injectInsrumentation(Instrumentation instrumentation) {
        injectInstrumentation(instrumentation);
    }

    public Instrumentation getInstrumentation() {
        return this.mInstrumentation;
    }

    public final <T extends Activity> T launchActivity(String pkg, Class<T> activityCls, Bundle extras) {
        Intent intent = new Intent("android.intent.action.MAIN");
        if (extras != null) {
            intent.putExtras(extras);
        }
        return launchActivityWithIntent(pkg, activityCls, intent);
    }

    public final <T extends Activity> T launchActivityWithIntent(String pkg, Class<T> activityCls, Intent intent) {
        intent.setClassName(pkg, activityCls.getName());
        intent.addFlags(268435456);
        T activity = getInstrumentation().startActivitySync(intent);
        getInstrumentation().waitForIdleSync();
        return activity;
    }

    public void runTestOnUiThread(final Runnable r) throws Throwable {
        final Throwable[] exceptions = new Throwable[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                try {
                    r.run();
                } catch (Throwable throwable) {
                    exceptions[0] = throwable;
                }
            }
        });
        if (exceptions[0] != null) {
            throw exceptions[0];
        }
    }

    protected void runTest() throws Throwable {
        String fName = getName();
        Assert.assertNotNull(fName);
        Method method = null;
        try {
            method = getClass().getMethod(fName, (Class[]) null);
        } catch (NoSuchMethodException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Method \"");
            stringBuilder.append(fName);
            stringBuilder.append("\" not found");
            Assert.fail(stringBuilder.toString());
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Method \"");
            stringBuilder2.append(fName);
            stringBuilder2.append("\" should be public");
            Assert.fail(stringBuilder2.toString());
        }
        int runCount = 1;
        boolean isRepetitive = false;
        if (method.isAnnotationPresent(FlakyTest.class)) {
            runCount = ((FlakyTest) method.getAnnotation(FlakyTest.class)).tolerance();
        } else if (method.isAnnotationPresent(RepetitiveTest.class)) {
            runCount = ((RepetitiveTest) method.getAnnotation(RepetitiveTest.class)).numIterations();
            isRepetitive = true;
        }
        if (method.isAnnotationPresent(UiThreadTest.class)) {
            final int tolerance = runCount;
            final boolean repetitive = isRepetitive;
            final Method testMethod = method;
            Throwable[] exceptions = new Throwable[1];
            final Throwable[] thArr = exceptions;
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    try {
                        InstrumentationTestCase.this.runMethod(testMethod, tolerance, repetitive);
                    } catch (Throwable throwable) {
                        thArr[0] = throwable;
                    }
                }
            });
            if (exceptions[0] != null) {
                throw exceptions[0];
            }
            return;
        }
        runMethod(method, runCount, isRepetitive);
    }

    private void runMethod(Method runMethod, int tolerance) throws Throwable {
        runMethod(runMethod, tolerance, false);
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x004d  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x004c A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void runMethod(Method runMethod, int tolerance, boolean isRepetitive) throws Throwable {
        Throwable exception;
        int runCount = 0;
        while (true) {
            Bundle iterations;
            try {
                runMethod.invoke(this, (Object[]) null);
                exception = null;
                runCount++;
                if (isRepetitive) {
                    iterations = new Bundle();
                    iterations.putInt("currentiterations", runCount);
                    getInstrumentation().sendStatus(2, iterations);
                }
            } catch (InvocationTargetException e) {
                e.fillInStackTrace();
                exception = e.getTargetException();
                runCount++;
                if (isRepetitive) {
                    iterations = new Bundle();
                }
            } catch (IllegalAccessException e2) {
                e2.fillInStackTrace();
                exception = e2;
                runCount++;
                if (isRepetitive) {
                    iterations = new Bundle();
                }
            } catch (Throwable th) {
                runCount++;
                if (isRepetitive) {
                    Bundle iterations2 = new Bundle();
                    iterations2.putInt("currentiterations", runCount);
                    getInstrumentation().sendStatus(2, iterations2);
                }
            }
            if (runCount >= tolerance || (!isRepetitive && exception == null)) {
                if (exception == null) {
                    throw exception;
                }
                return;
            }
        }
        if (exception == null) {
        }
    }

    public void sendKeys(String keysSequence) {
        StringBuilder stringBuilder;
        Instrumentation instrumentation = getInstrumentation();
        for (String key : keysSequence.split(" ")) {
            String key2;
            int keyCount;
            int repeater = key2.indexOf(42);
            if (repeater == -1) {
                keyCount = 1;
            } else {
                try {
                    keyCount = Integer.parseInt(key2.substring(0, repeater));
                } catch (NumberFormatException e) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Invalid repeat count: ");
                    stringBuilder2.append(key2);
                    Log.w("ActivityTestCase", stringBuilder2.toString());
                }
            }
            if (repeater != -1) {
                key2 = key2.substring(repeater + 1);
            }
            int j = 0;
            while (j < keyCount) {
                try {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("KEYCODE_");
                    stringBuilder3.append(key2);
                    try {
                        instrumentation.sendKeyDownUpSync(KeyEvent.class.getField(stringBuilder3.toString()).getInt(0));
                    } catch (SecurityException e2) {
                    }
                    j++;
                } catch (NoSuchFieldException e3) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown keycode: KEYCODE_");
                    stringBuilder.append(key2);
                    Log.w("ActivityTestCase", stringBuilder.toString());
                } catch (IllegalAccessException e4) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown keycode: KEYCODE_");
                    stringBuilder.append(key2);
                    Log.w("ActivityTestCase", stringBuilder.toString());
                }
            }
        }
        instrumentation.waitForIdleSync();
    }

    public void sendKeys(int... keys) {
        Instrumentation instrumentation = getInstrumentation();
        for (int sendKeyDownUpSync : keys) {
            try {
                instrumentation.sendKeyDownUpSync(sendKeyDownUpSync);
            } catch (SecurityException e) {
            }
        }
        instrumentation.waitForIdleSync();
    }

    public void sendRepeatedKeys(int... keys) {
        int count = keys.length;
        if ((count & 1) != 1) {
            Instrumentation instrumentation = getInstrumentation();
            for (int i = 0; i < count; i += 2) {
                int keyCount = keys[i];
                int keyCode = keys[i + 1];
                for (int j = 0; j < keyCount; j++) {
                    try {
                        instrumentation.sendKeyDownUpSync(keyCode);
                    } catch (SecurityException e) {
                    }
                }
            }
            instrumentation.waitForIdleSync();
            return;
        }
        throw new IllegalArgumentException("The size of the keys array must be a multiple of 2");
    }

    protected void tearDown() throws Exception {
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        super.tearDown();
    }
}
