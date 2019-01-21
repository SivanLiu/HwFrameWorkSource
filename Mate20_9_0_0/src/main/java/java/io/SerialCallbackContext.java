package java.io;

final class SerialCallbackContext {
    private final ObjectStreamClass desc;
    private final Object obj;
    private Thread thread = Thread.currentThread();

    public SerialCallbackContext(Object obj, ObjectStreamClass desc) {
        this.obj = obj;
        this.desc = desc;
    }

    public Object getObj() throws NotActiveException {
        checkAndSetUsed();
        return this.obj;
    }

    public ObjectStreamClass getDesc() {
        return this.desc;
    }

    public void check() throws NotActiveException {
        if (this.thread != null && this.thread != Thread.currentThread()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("expected thread: ");
            stringBuilder.append(this.thread);
            stringBuilder.append(", but got: ");
            stringBuilder.append(Thread.currentThread());
            throw new NotActiveException(stringBuilder.toString());
        }
    }

    private void checkAndSetUsed() throws NotActiveException {
        if (this.thread == Thread.currentThread()) {
            this.thread = null;
            return;
        }
        throw new NotActiveException("not in readObject invocation or fields already read");
    }

    public void setUsed() {
        this.thread = null;
    }
}
