package java.util.logging;

public class MemoryHandler extends Handler {
    private static final int DEFAULT_SIZE = 1000;
    private LogRecord[] buffer;
    int count;
    private volatile Level pushLevel;
    private int size;
    int start;
    private Handler target;

    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".push");
        this.pushLevel = manager.getLevelProperty(stringBuilder.toString(), Level.SEVERE);
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".size");
        this.size = manager.getIntProperty(stringBuilder.toString(), 1000);
        if (this.size <= 0) {
            this.size = 1000;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".level");
        setLevel(manager.getLevelProperty(stringBuilder.toString(), Level.ALL));
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".filter");
        setFilter(manager.getFilterProperty(stringBuilder.toString(), null));
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".formatter");
        setFormatter(manager.getFormatterProperty(stringBuilder.toString(), new SimpleFormatter()));
    }

    public MemoryHandler() {
        this.sealed = false;
        configure();
        this.sealed = true;
        LogManager manager = LogManager.getLogManager();
        String handlerName = getClass().getName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(handlerName);
        stringBuilder.append(".target");
        String targetName = manager.getProperty(stringBuilder.toString());
        if (targetName != null) {
            try {
                this.target = (Handler) ClassLoader.getSystemClassLoader().loadClass(targetName).newInstance();
            } catch (Exception e) {
                try {
                    Class<?> clz = Thread.currentThread().getContextClassLoader().loadClass(targetName);
                    this.target = (Handler) clz.newInstance();
                    Class<?> ex = clz;
                } catch (Exception innerE) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("MemoryHandler can't load handler target \"");
                    stringBuilder2.append(targetName);
                    stringBuilder2.append("\"");
                    throw new RuntimeException(stringBuilder2.toString(), innerE);
                }
            }
            init();
            return;
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("The handler ");
        stringBuilder3.append(handlerName);
        stringBuilder3.append(" does not specify a target");
        throw new RuntimeException(stringBuilder3.toString());
    }

    private void init() {
        this.buffer = new LogRecord[this.size];
        this.start = 0;
        this.count = 0;
    }

    public MemoryHandler(Handler target, int size, Level pushLevel) {
        if (target == null || pushLevel == null) {
            throw new NullPointerException();
        } else if (size > 0) {
            this.sealed = false;
            configure();
            this.sealed = true;
            this.target = target;
            this.pushLevel = pushLevel;
            this.size = size;
            init();
        } else {
            throw new IllegalArgumentException();
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0046, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void publish(LogRecord record) {
        if (isLoggable(record)) {
            this.buffer[(this.start + this.count) % this.buffer.length] = record;
            if (this.count < this.buffer.length) {
                this.count++;
            } else {
                this.start++;
                this.start %= this.buffer.length;
            }
            if (record.getLevel().intValue() >= this.pushLevel.intValue()) {
                push();
            }
        }
    }

    public synchronized void push() {
        for (int i = 0; i < this.count; i++) {
            this.target.publish(this.buffer[(this.start + i) % this.buffer.length]);
        }
        this.start = 0;
        this.count = 0;
    }

    public void flush() {
        this.target.flush();
    }

    public void close() throws SecurityException {
        this.target.close();
        setLevel(Level.OFF);
    }

    public synchronized void setPushLevel(Level newLevel) throws SecurityException {
        if (newLevel != null) {
            checkPermission();
            this.pushLevel = newLevel;
        } else {
            throw new NullPointerException();
        }
    }

    public Level getPushLevel() {
        return this.pushLevel;
    }

    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record);
    }
}
