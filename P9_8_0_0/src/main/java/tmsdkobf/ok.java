package tmsdkobf;

import java.util.LinkedHashSet;

public class ok<T> {
    private int AY = -1;
    private LinkedHashSet<T> Ip = new LinkedHashSet();

    public ok(int i) {
        this.AY = i;
    }

    public synchronized boolean d(T t) {
        return this.Ip.contains(t);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized T poll() {
        if (this.Ip != null) {
            Object -l_1_R = this.Ip.iterator();
            if (-l_1_R != null && -l_1_R.hasNext()) {
                Object -l_2_R = -l_1_R.next();
                this.Ip.remove(-l_2_R);
                return -l_2_R;
            }
        }
    }

    public synchronized void push(T t) {
        if (this.Ip.size() >= this.AY) {
            poll();
        }
        this.Ip.add(t);
    }
}
