package sun.nio.ch;

public abstract class AbstractPollArrayWrapper {
    static final short EVENT_OFFSET = (short) 4;
    static final short FD_OFFSET = (short) 0;
    static final short REVENT_OFFSET = (short) 6;
    static final short SIZE_POLLFD = (short) 8;
    protected AllocatedNativeObject pollArray;
    protected long pollArrayAddress;
    protected int totalChannels = 0;

    int getEventOps(int i) {
        return this.pollArray.getShort((8 * i) + 4);
    }

    int getReventOps(int i) {
        return this.pollArray.getShort((8 * i) + 6);
    }

    int getDescriptor(int i) {
        return this.pollArray.getInt((8 * i) + 0);
    }

    void putEventOps(int i, int event) {
        this.pollArray.putShort((8 * i) + 4, (short) event);
    }

    void putReventOps(int i, int revent) {
        this.pollArray.putShort((8 * i) + 6, (short) revent);
    }

    void putDescriptor(int i, int fd) {
        this.pollArray.putInt((8 * i) + 0, fd);
    }
}
