package tmsdkobf;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.TreeMap;

public class op<T> {
    private TreeMap<T, LinkedList<T>> IC = null;

    public op(Comparator<T> comparator) {
        this.IC = new TreeMap(comparator);
    }

    private LinkedList<T> he() {
        return new LinkedList();
    }

    public synchronized void add(T t) {
        Object -l_2_R = (LinkedList) this.IC.get(t);
        if (-l_2_R == null) {
            -l_2_R = he();
            this.IC.put(t, -l_2_R);
        }
        -l_2_R.addLast(t);
    }

    public synchronized void clear() {
        this.IC.clear();
    }

    public synchronized boolean isEmpty() {
        return this.IC.isEmpty();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized T poll() {
        if (isEmpty()) {
            return null;
        }
        Object -l_1_R = this.IC.firstKey();
        LinkedList -l_2_R = (LinkedList) this.IC.get(-l_1_R);
        Object -l_3_R = -l_2_R.poll();
        if (-l_2_R.size() <= 0) {
            this.IC.remove(-l_1_R);
        }
    }
}
