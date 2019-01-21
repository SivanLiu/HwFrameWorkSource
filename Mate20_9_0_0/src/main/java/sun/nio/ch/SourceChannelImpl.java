package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe.SourceChannel;
import java.nio.channels.spi.SelectorProvider;

class SourceChannelImpl extends SourceChannel implements SelChImpl {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int ST_INUSE = 0;
    private static final int ST_KILLED = 1;
    private static final int ST_UNINITIALIZED = -1;
    private static final NativeDispatcher nd = new FileDispatcherImpl();
    FileDescriptor fd;
    int fdVal;
    private final Object lock = new Object();
    private volatile int state = -1;
    private final Object stateLock = new Object();
    private volatile long thread = 0;

    public FileDescriptor getFD() {
        return this.fd;
    }

    public int getFDVal() {
        return this.fdVal;
    }

    SourceChannelImpl(SelectorProvider sp, FileDescriptor fd) {
        super(sp);
        this.fd = fd;
        this.fdVal = IOUtil.fdVal(fd);
        this.state = 0;
    }

    protected void implCloseSelectableChannel() throws IOException {
        synchronized (this.stateLock) {
            if (this.state != 1) {
                nd.preClose(this.fd);
            }
            long th = this.thread;
            if (th != 0) {
                NativeThread.signal(th);
            }
            if (!isRegistered()) {
                kill();
            }
        }
    }

    public void kill() throws IOException {
        synchronized (this.stateLock) {
            if (this.state == 1) {
            } else if (this.state == -1) {
                this.state = 1;
            } else {
                nd.close(this.fd);
                this.state = 1;
            }
        }
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        IOUtil.configureBlocking(this.fd, block);
    }

    public boolean translateReadyOps(int ops, int initialOps, SelectionKeyImpl sk) {
        int intOps = sk.nioInterestOps();
        int oldOps = sk.nioReadyOps();
        int newOps = initialOps;
        if ((Net.POLLNVAL & ops) == 0) {
            int i = (Net.POLLERR | Net.POLLHUP) & ops;
            boolean z = $assertionsDisabled;
            if (i != 0) {
                newOps = intOps;
                sk.nioReadyOps(newOps);
                if (((~oldOps) & newOps) != 0) {
                    z = true;
                }
                return z;
            }
            if (!((Net.POLLIN & ops) == 0 || (intOps & 1) == 0)) {
                newOps |= 1;
            }
            sk.nioReadyOps(newOps);
            if (((~oldOps) & newOps) != 0) {
                z = true;
            }
            return z;
        }
        throw new Error("POLLNVAL detected");
    }

    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl sk) {
        return translateReadyOps(ops, sk.nioReadyOps(), sk);
    }

    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl sk) {
        return translateReadyOps(ops, 0, sk);
    }

    public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        if (ops == 1) {
            ops = Net.POLLIN;
        }
        sk.selector.putEventOps(sk, ops);
    }

    private void ensureOpen() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    public int read(ByteBuffer dst) throws IOException {
        if (dst != null) {
            ensureOpen();
            synchronized (this.lock) {
                boolean z = $assertionsDisabled;
                int n = 0;
                boolean z2 = true;
                try {
                    begin();
                    if (isOpen()) {
                        this.thread = NativeThread.current();
                        do {
                            n = IOUtil.read(this.fd, dst, -1, nd);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        int normalize = IOStatus.normalize(n);
                        this.thread = 0;
                        if (n <= 0) {
                            if (n != -2) {
                                end(z);
                                return normalize;
                            }
                        }
                        z = true;
                        end(z);
                        return normalize;
                    }
                    this.thread = 0;
                    if (n <= 0) {
                        if (n != -2) {
                            z2 = $assertionsDisabled;
                        }
                    }
                    end(z2);
                    return 0;
                } catch (Throwable th) {
                    this.thread = 0;
                    if (n <= 0) {
                        if (n != -2) {
                            end(z);
                        }
                    }
                    z = true;
                    end(z);
                }
            }
        } else {
            throw new NullPointerException();
        }
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        if (offset >= 0 && length >= 0 && offset <= dsts.length - length) {
            return read(Util.subsequence(dsts, offset, length));
        }
        throw new IndexOutOfBoundsException();
    }

    public long read(ByteBuffer[] dsts) throws IOException {
        if (dsts != null) {
            ensureOpen();
            synchronized (this.lock) {
                long n = 0;
                boolean z = true;
                try {
                    begin();
                    if (isOpen()) {
                        this.thread = NativeThread.current();
                        do {
                            n = IOUtil.read(this.fd, dsts, nd);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        long normalize = IOStatus.normalize(n);
                        this.thread = 0;
                        if (n <= 0) {
                            if (n != -2) {
                                z = $assertionsDisabled;
                            }
                        }
                        end(z);
                        return normalize;
                    }
                    this.thread = 0;
                    if (n <= 0) {
                        if (n != -2) {
                            z = $assertionsDisabled;
                        }
                    }
                    end(z);
                    return 0;
                } catch (Throwable th) {
                    this.thread = 0;
                    if (n <= 0) {
                        if (n != -2) {
                            z = $assertionsDisabled;
                        }
                    }
                    end(z);
                }
            }
        } else {
            throw new NullPointerException();
        }
    }
}
