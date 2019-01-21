package android.os;

import java.io.PrintStream;

public class Broadcaster {
    private Registration mReg;

    private class Registration {
        Registration next;
        Registration prev;
        int senderWhat;
        int[] targetWhats;
        Handler[] targets;

        private Registration() {
        }
    }

    /* JADX WARNING: Missing block: B:32:0x009b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void request(int senderWhat, Handler target, int targetWhat) {
        synchronized (this) {
            Registration r;
            if (this.mReg == null) {
                r = new Registration();
                r.senderWhat = senderWhat;
                r.targets = new Handler[1];
                r.targetWhats = new int[1];
                r.targets[0] = target;
                r.targetWhats[0] = targetWhat;
                this.mReg = r;
                r.next = r;
                r.prev = r;
            } else {
                int n;
                Registration start = this.mReg;
                r = start;
                while (r.senderWhat < senderWhat) {
                    r = r.next;
                    if (r == start) {
                        break;
                    }
                }
                if (r.senderWhat != senderWhat) {
                    Registration reg = new Registration();
                    reg.senderWhat = senderWhat;
                    reg.targets = new Handler[1];
                    reg.targetWhats = new int[1];
                    reg.next = r;
                    reg.prev = r.prev;
                    r.prev.next = reg;
                    r.prev = reg;
                    if (r == this.mReg && r.senderWhat > reg.senderWhat) {
                        this.mReg = reg;
                    }
                    r = reg;
                    n = 0;
                } else {
                    n = r.targets.length;
                    Handler[] oldTargets = r.targets;
                    int[] oldWhats = r.targetWhats;
                    int i = 0;
                    while (i < n) {
                        if (oldTargets[i] == target && oldWhats[i] == targetWhat) {
                            return;
                        }
                        i++;
                    }
                    r.targets = new Handler[(n + 1)];
                    System.arraycopy(oldTargets, 0, r.targets, 0, n);
                    r.targetWhats = new int[(n + 1)];
                    System.arraycopy(oldWhats, 0, r.targetWhats, 0, n);
                }
                r.targets[n] = target;
                r.targetWhats[n] = targetWhat;
            }
        }
    }

    /* JADX WARNING: Missing block: B:27:0x0058, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void cancelRequest(int senderWhat, Handler target, int targetWhat) {
        synchronized (this) {
            Registration start = this.mReg;
            Registration r = start;
            if (r == null) {
                return;
            }
            while (r.senderWhat < senderWhat) {
                r = r.next;
                if (r == start) {
                    break;
                }
            }
            if (r.senderWhat == senderWhat) {
                Handler[] targets = r.targets;
                int[] whats = r.targetWhats;
                int oldLen = targets.length;
                int i = 0;
                while (i < oldLen) {
                    if (targets[i] == target && whats[i] == targetWhat) {
                        r.targets = new Handler[(oldLen - 1)];
                        r.targetWhats = new int[(oldLen - 1)];
                        if (i > 0) {
                            System.arraycopy(targets, 0, r.targets, 0, i);
                            System.arraycopy(whats, 0, r.targetWhats, 0, i);
                        }
                        int remainingLen = (oldLen - i) - 1;
                        if (remainingLen != 0) {
                            System.arraycopy(targets, i + 1, r.targets, i, remainingLen);
                            System.arraycopy(whats, i + 1, r.targetWhats, i, remainingLen);
                        }
                    } else {
                        i++;
                    }
                }
            }
        }
    }

    public void dumpRegistrations() {
        synchronized (this) {
            Registration start = this.mReg;
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Broadcaster ");
            stringBuilder.append(this);
            stringBuilder.append(" {");
            printStream.println(stringBuilder.toString());
            if (start != null) {
                Registration r = start;
                do {
                    PrintStream printStream2 = System.out;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("    senderWhat=");
                    stringBuilder2.append(r.senderWhat);
                    printStream2.println(stringBuilder2.toString());
                    int n = r.targets.length;
                    for (int i = 0; i < n; i++) {
                        PrintStream printStream3 = System.out;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("        [");
                        stringBuilder3.append(r.targetWhats[i]);
                        stringBuilder3.append("] ");
                        stringBuilder3.append(r.targets[i]);
                        printStream3.println(stringBuilder3.toString());
                    }
                    r = r.next;
                } while (r != start);
            }
            System.out.println("}");
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0036, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void broadcast(Message msg) {
        synchronized (this) {
            if (this.mReg == null) {
                return;
            }
            int senderWhat = msg.what;
            Registration start = this.mReg;
            Registration r = start;
            while (r.senderWhat < senderWhat) {
                r = r.next;
                if (r == start) {
                    break;
                }
            }
            if (r.senderWhat == senderWhat) {
                Handler[] targets = r.targets;
                int[] whats = r.targetWhats;
                int n = targets.length;
                for (int i = 0; i < n; i++) {
                    Handler target = targets[i];
                    Message m = Message.obtain();
                    m.copyFrom(msg);
                    m.what = whats[i];
                    target.sendMessage(m);
                }
            }
        }
    }
}
