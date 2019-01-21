package sun.nio.ch;

class NativeThreadSet {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private long[] elts;
    private int used = 0;
    private boolean waitingToEmpty;

    NativeThreadSet(int n) {
        this.elts = new long[n];
    }

    int add() {
        long th = NativeThread.current();
        if (th == 0) {
            th = -1;
        }
        synchronized (this) {
            int on;
            int start = 0;
            if (this.used >= this.elts.length) {
                on = this.elts.length;
                Object nelts = new long[(on * 2)];
                System.arraycopy(this.elts, 0, nelts, 0, on);
                this.elts = nelts;
                start = on;
            }
            for (on = start; on < this.elts.length; on++) {
                if (this.elts[on] == 0) {
                    this.elts[on] = th;
                    this.used++;
                    return on;
                }
            }
            return -1;
        }
    }

    void remove(int i) {
        synchronized (this) {
            this.elts[i] = 0;
            this.used--;
            if (this.used == 0 && this.waitingToEmpty) {
                notifyAll();
            }
        }
    }

    synchronized void signalAndWait() {
        boolean interrupted = false;
        while (this.used > 0) {
            int u = this.used;
            int u2 = u;
            for (long th : this.elts) {
                if (th != 0) {
                    if (th != -1) {
                        NativeThread.signal(th);
                    }
                    u2--;
                    if (u2 == 0) {
                        break;
                    }
                }
            }
            this.waitingToEmpty = true;
            try {
                wait(50);
            } catch (InterruptedException e) {
                interrupted = true;
            } finally {
                this.waitingToEmpty = false;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
