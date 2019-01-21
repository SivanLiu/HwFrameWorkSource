package java.lang;

import android.system.OsConstants;
import dalvik.system.VMDebug;
import dalvik.system.VMRuntime;
import dalvik.system.VMStack;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import libcore.io.IoUtils;
import libcore.io.Libcore;
import libcore.util.EmptyArray;
import sun.reflect.CallerSensitive;

public class Runtime {
    private static Runtime currentRuntime = new Runtime();
    private static boolean finalizeOnExit;
    private volatile String[] mLibPaths = null;
    private List<Thread> shutdownHooks = new ArrayList();
    private boolean shuttingDown;
    private boolean tracingMethods;

    private static native void nativeExit(int i);

    private static native String nativeLoad(String str, ClassLoader classLoader);

    private static native void runFinalization0();

    public native long freeMemory();

    public native void gc();

    public native long maxMemory();

    public native long totalMemory();

    public static Runtime getRuntime() {
        return currentRuntime;
    }

    private Runtime() {
    }

    public void exit(int status) {
        synchronized (this) {
            if (!this.shuttingDown) {
                Thread[] hooks;
                this.shuttingDown = true;
                synchronized (this.shutdownHooks) {
                    hooks = new Thread[this.shutdownHooks.size()];
                    this.shutdownHooks.toArray(hooks);
                }
                int i = 0;
                for (Thread hook : hooks) {
                    hook.start();
                }
                int length = hooks.length;
                while (i < length) {
                    try {
                        hooks[i].join();
                    } catch (InterruptedException e) {
                    }
                    i++;
                }
                if (finalizeOnExit) {
                    runFinalization();
                }
                nativeExit(status);
            }
        }
    }

    public void addShutdownHook(Thread hook) {
        if (hook == null) {
            throw new NullPointerException("hook == null");
        } else if (this.shuttingDown) {
            throw new IllegalStateException("VM already shutting down");
        } else if (hook.started) {
            throw new IllegalArgumentException("Hook has already been started");
        } else {
            synchronized (this.shutdownHooks) {
                if (this.shutdownHooks.contains(hook)) {
                    throw new IllegalArgumentException("Hook already registered.");
                }
                this.shutdownHooks.add(hook);
            }
        }
    }

    public boolean removeShutdownHook(Thread hook) {
        if (hook == null) {
            throw new NullPointerException("hook == null");
        } else if (this.shuttingDown) {
            throw new IllegalStateException("VM already shutting down");
        } else {
            boolean remove;
            synchronized (this.shutdownHooks) {
                remove = this.shutdownHooks.remove((Object) hook);
            }
            return remove;
        }
    }

    public void halt(int status) {
        nativeExit(status);
    }

    @Deprecated
    public static void runFinalizersOnExit(boolean value) {
        finalizeOnExit = value;
    }

    public Process exec(String command) throws IOException {
        return exec(command, null, null);
    }

    public Process exec(String command, String[] envp) throws IOException {
        return exec(command, envp, null);
    }

    public Process exec(String command, String[] envp, File dir) throws IOException {
        if (command.length() != 0) {
            StringTokenizer st = new StringTokenizer(command);
            String[] cmdarray = new String[st.countTokens()];
            int i = 0;
            while (st.hasMoreTokens()) {
                cmdarray[i] = st.nextToken();
                i++;
            }
            return exec(cmdarray, envp, dir);
        }
        throw new IllegalArgumentException("Empty command");
    }

    public Process exec(String[] cmdarray) throws IOException {
        return exec(cmdarray, null, null);
    }

    public Process exec(String[] cmdarray, String[] envp) throws IOException {
        return exec(cmdarray, envp, null);
    }

    public Process exec(String[] cmdarray, String[] envp, File dir) throws IOException {
        return new ProcessBuilder(cmdarray).environment(envp).directory(dir).start();
    }

    public int availableProcessors() {
        return (int) Libcore.os.sysconf(OsConstants._SC_NPROCESSORS_CONF);
    }

    public void runFinalization() {
        VMRuntime.runFinalization(0);
    }

    public void traceInstructions(boolean on) {
    }

    public void traceMethodCalls(boolean on) {
        if (on != this.tracingMethods) {
            if (on) {
                VMDebug.startMethodTracing();
            } else {
                VMDebug.stopMethodTracing();
            }
            this.tracingMethods = on;
        }
    }

    @CallerSensitive
    public void load(String filename) {
        load0(VMStack.getStackClass1(), filename);
    }

    private void checkTargetSdkVersionForLoad(String methodName) {
        int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
        if (targetSdkVersion > 24) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(methodName);
            stringBuilder.append(" is not supported on SDK ");
            stringBuilder.append(targetSdkVersion);
            throw new UnsupportedOperationException(stringBuilder.toString());
        }
    }

    void load(String absolutePath, ClassLoader loader) {
        checkTargetSdkVersionForLoad("java.lang.Runtime#load(String, ClassLoader)");
        System.logE("java.lang.Runtime#load(String, ClassLoader) is private and will be removed in a future Android release");
        if (absolutePath != null) {
            String error = nativeLoad(absolutePath, loader);
            if (error != null) {
                throw new UnsatisfiedLinkError(error);
            }
            return;
        }
        throw new NullPointerException("absolutePath == null");
    }

    synchronized void load0(Class<?> fromClass, String filename) {
        if (!new File(filename).isAbsolute()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expecting an absolute path of the library: ");
            stringBuilder.append(filename);
            throw new UnsatisfiedLinkError(stringBuilder.toString());
        } else if (filename != null) {
            String error = nativeLoad(filename, fromClass.getClassLoader());
            if (error != null) {
                throw new UnsatisfiedLinkError(error);
            }
        } else {
            throw new NullPointerException("filename == null");
        }
    }

    @CallerSensitive
    public void loadLibrary(String libname) {
        loadLibrary0(VMStack.getCallingClassLoader(), libname);
    }

    public void loadLibrary(String libname, ClassLoader classLoader) {
        checkTargetSdkVersionForLoad("java.lang.Runtime#loadLibrary(String, ClassLoader)");
        System.logE("java.lang.Runtime#loadLibrary(String, ClassLoader) is private and will be removed in a future Android release");
        loadLibrary0(classLoader, libname);
    }

    synchronized void loadLibrary0(ClassLoader loader, String libname) {
        if (libname.indexOf(File.separatorChar) == -1) {
            String libraryName = libname;
            String filename;
            if (loader != null) {
                filename = loader.findLibrary(libraryName);
                if (filename != null) {
                    String error = nativeLoad(filename, loader);
                    if (error != null) {
                        throw new UnsatisfiedLinkError(error);
                    }
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append((Object) loader);
                stringBuilder.append(" couldn't find \"");
                stringBuilder.append(System.mapLibraryName(libraryName));
                stringBuilder.append("\"");
                throw new UnsatisfiedLinkError(stringBuilder.toString());
            }
            filename = System.mapLibraryName(libraryName);
            Object candidates = new ArrayList();
            String lastError = null;
            for (String directory : getLibPaths()) {
                String candidate = new StringBuilder();
                candidate.append(directory);
                candidate.append(filename);
                candidate = candidate.toString();
                candidates.add(candidate);
                if (IoUtils.canOpenReadOnly(candidate)) {
                    String error2 = nativeLoad(candidate, loader);
                    if (error2 != null) {
                        lastError = error2;
                    } else {
                        return;
                    }
                }
            }
            if (lastError != null) {
                throw new UnsatisfiedLinkError(lastError);
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Library ");
            stringBuilder2.append(libraryName);
            stringBuilder2.append(" not found; tried ");
            stringBuilder2.append(candidates);
            throw new UnsatisfiedLinkError(stringBuilder2.toString());
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Directory separator should not appear in library name: ");
        stringBuilder3.append(libname);
        throw new UnsatisfiedLinkError(stringBuilder3.toString());
    }

    private String[] getLibPaths() {
        if (this.mLibPaths == null) {
            synchronized (this) {
                if (this.mLibPaths == null) {
                    this.mLibPaths = initLibPaths();
                }
            }
        }
        return this.mLibPaths;
    }

    private static String[] initLibPaths() {
        String javaLibraryPath = System.getProperty("java.library.path");
        if (javaLibraryPath == null) {
            return EmptyArray.STRING;
        }
        String[] paths = javaLibraryPath.split(":");
        for (int i = 0; i < paths.length; i++) {
            if (!paths[i].endsWith("/")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(paths[i]);
                stringBuilder.append("/");
                paths[i] = stringBuilder.toString();
            }
        }
        return paths;
    }

    @Deprecated
    public InputStream getLocalizedInputStream(InputStream in) {
        return in;
    }

    @Deprecated
    public OutputStream getLocalizedOutputStream(OutputStream out) {
        return out;
    }
}
