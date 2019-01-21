package android.filterfw.core;

public class OutputPort extends FilterPort {
    protected InputPort mBasePort;
    protected InputPort mTargetPort;

    public OutputPort(Filter filter, String name) {
        super(filter, name);
    }

    public void connectTo(InputPort target) {
        if (this.mTargetPort == null) {
            this.mTargetPort = target;
            this.mTargetPort.setSourcePort(this);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this);
        stringBuilder.append(" already connected to ");
        stringBuilder.append(this.mTargetPort);
        stringBuilder.append("!");
        throw new RuntimeException(stringBuilder.toString());
    }

    public boolean isConnected() {
        return this.mTargetPort != null;
    }

    public void open() {
        super.open();
        if (this.mTargetPort != null && !this.mTargetPort.isOpen()) {
            this.mTargetPort.open();
        }
    }

    public void close() {
        super.close();
        if (this.mTargetPort != null && this.mTargetPort.isOpen()) {
            this.mTargetPort.close();
        }
    }

    public InputPort getTargetPort() {
        return this.mTargetPort;
    }

    public Filter getTargetFilter() {
        return this.mTargetPort == null ? null : this.mTargetPort.getFilter();
    }

    public void setBasePort(InputPort basePort) {
        this.mBasePort = basePort;
    }

    public InputPort getBasePort() {
        return this.mBasePort;
    }

    public boolean filterMustClose() {
        return !isOpen() && isBlocking();
    }

    public boolean isReady() {
        return (isOpen() && this.mTargetPort.acceptsFrame()) || !isBlocking();
    }

    public void clear() {
        if (this.mTargetPort != null) {
            this.mTargetPort.clear();
        }
    }

    public void pushFrame(Frame frame) {
        if (this.mTargetPort != null) {
            this.mTargetPort.pushFrame(frame);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attempting to push frame on unconnected port: ");
        stringBuilder.append(this);
        stringBuilder.append("!");
        throw new RuntimeException(stringBuilder.toString());
    }

    public void setFrame(Frame frame) {
        assertPortIsOpen();
        if (this.mTargetPort != null) {
            this.mTargetPort.setFrame(frame);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attempting to set frame on unconnected port: ");
        stringBuilder.append(this);
        stringBuilder.append("!");
        throw new RuntimeException(stringBuilder.toString());
    }

    public Frame pullFrame() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot pull frame on ");
        stringBuilder.append(this);
        stringBuilder.append("!");
        throw new RuntimeException(stringBuilder.toString());
    }

    public boolean hasFrame() {
        return this.mTargetPort == null ? false : this.mTargetPort.hasFrame();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("output ");
        stringBuilder.append(super.toString());
        return stringBuilder.toString();
    }
}
