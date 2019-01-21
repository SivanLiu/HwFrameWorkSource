package java.util.logging;

import dalvik.system.VMStack;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LogRecord implements Serializable {
    private static final int MIN_SEQUENTIAL_THREAD_ID = 1073741823;
    private static final AtomicLong globalSequenceNumber = new AtomicLong(0);
    private static final AtomicInteger nextThreadId = new AtomicInteger(MIN_SEQUENTIAL_THREAD_ID);
    private static final long serialVersionUID = 5372048053134512534L;
    private static final ThreadLocal<Integer> threadIds = new ThreadLocal();
    private Level level;
    private String loggerName;
    private String message;
    private long millis = System.currentTimeMillis();
    private transient boolean needToInferCaller = true;
    private transient Object[] parameters;
    private transient ResourceBundle resourceBundle;
    private String resourceBundleName;
    private long sequenceNumber = globalSequenceNumber.getAndIncrement();
    private String sourceClassName;
    private String sourceMethodName;
    private int threadID = defaultThreadID();
    private Throwable thrown;

    private int defaultThreadID() {
        long tid = Thread.currentThread().getId();
        if (tid < 1073741823) {
            return (int) tid;
        }
        Integer id = (Integer) threadIds.get();
        if (id == null) {
            id = Integer.valueOf(nextThreadId.getAndIncrement());
            threadIds.set(id);
        }
        return id.intValue();
    }

    public LogRecord(Level level, String msg) {
        level.getClass();
        this.level = level;
        this.message = msg;
    }

    public String getLoggerName() {
        return this.loggerName;
    }

    public void setLoggerName(String name) {
        this.loggerName = name;
    }

    public ResourceBundle getResourceBundle() {
        return this.resourceBundle;
    }

    public void setResourceBundle(ResourceBundle bundle) {
        this.resourceBundle = bundle;
    }

    public String getResourceBundleName() {
        return this.resourceBundleName;
    }

    public void setResourceBundleName(String name) {
        this.resourceBundleName = name;
    }

    public Level getLevel() {
        return this.level;
    }

    public void setLevel(Level level) {
        if (level != null) {
            this.level = level;
            return;
        }
        throw new NullPointerException();
    }

    public long getSequenceNumber() {
        return this.sequenceNumber;
    }

    public void setSequenceNumber(long seq) {
        this.sequenceNumber = seq;
    }

    public String getSourceClassName() {
        if (this.needToInferCaller) {
            inferCaller();
        }
        return this.sourceClassName;
    }

    public void setSourceClassName(String sourceClassName) {
        this.sourceClassName = sourceClassName;
        this.needToInferCaller = false;
    }

    public String getSourceMethodName() {
        if (this.needToInferCaller) {
            inferCaller();
        }
        return this.sourceMethodName;
    }

    public void setSourceMethodName(String sourceMethodName) {
        this.sourceMethodName = sourceMethodName;
        this.needToInferCaller = false;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object[] getParameters() {
        return this.parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public int getThreadID() {
        return this.threadID;
    }

    public void setThreadID(int threadID) {
        this.threadID = threadID;
    }

    public long getMillis() {
        return this.millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }

    public Throwable getThrown() {
        return this.thrown;
    }

    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeByte(1);
        int i = 0;
        out.writeByte(0);
        if (this.parameters == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(this.parameters.length);
        while (i < this.parameters.length) {
            if (this.parameters[i] == null) {
                out.writeObject(null);
            } else {
                out.writeObject(this.parameters[i].toString());
            }
            i++;
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int major = in.readByte();
        int minor = in.readByte();
        if (major == (byte) 1) {
            int len = in.readInt();
            if (len >= -1) {
                if (len == -1) {
                    this.parameters = null;
                } else if (len < 255) {
                    this.parameters = new Object[len];
                    for (int i = 0; i < this.parameters.length; i++) {
                        this.parameters[i] = in.readObject();
                    }
                } else {
                    List<Object> params = new ArrayList(Math.min(len, 1024));
                    for (int i2 = 0; i2 < len; i2++) {
                        params.add(in.readObject());
                    }
                    this.parameters = params.toArray(new Object[params.size()]);
                }
                if (this.resourceBundleName != null) {
                    try {
                        this.resourceBundle = ResourceBundle.getBundle(this.resourceBundleName, Locale.getDefault(), ClassLoader.getSystemClassLoader());
                    } catch (MissingResourceException e) {
                        try {
                            this.resourceBundle = ResourceBundle.getBundle(this.resourceBundleName, Locale.getDefault(), Thread.currentThread().getContextClassLoader());
                        } catch (MissingResourceException e2) {
                            this.resourceBundle = null;
                        }
                    }
                }
                this.needToInferCaller = false;
                return;
            }
            throw new NegativeArraySizeException();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LogRecord: bad version: ");
        stringBuilder.append(major);
        stringBuilder.append(".");
        stringBuilder.append(minor);
        throw new IOException(stringBuilder.toString());
    }

    private void inferCaller() {
        int ix = 0;
        this.needToInferCaller = false;
        StackTraceElement[] stack = VMStack.getThreadStackTrace(Thread.currentThread());
        int depth = stack.length;
        boolean lookingForLogger = true;
        while (ix < depth) {
            StackTraceElement frame = stack[ix];
            String cname = frame.getClassName();
            boolean isLoggerImpl = isLoggerImplFrame(cname);
            if (lookingForLogger) {
                if (isLoggerImpl) {
                    lookingForLogger = false;
                }
            } else if (!(isLoggerImpl || cname.startsWith("java.lang.reflect.") || cname.startsWith("sun.reflect."))) {
                setSourceClassName(cname);
                setSourceMethodName(frame.getMethodName());
                return;
            }
            ix++;
        }
    }

    private boolean isLoggerImplFrame(String cname) {
        return cname.equals("java.util.logging.Logger") || cname.startsWith("java.util.logging.LoggingProxyImpl") || cname.startsWith("sun.util.logging.");
    }
}
