package tmsdkobf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class mr<E> {
    private final int AY;
    private final ConcurrentLinkedQueue<E> tk = new ConcurrentLinkedQueue();

    public mr(int i) {
        this.AY = i;
    }

    public boolean addAll(Collection<? extends E> collection) {
        synchronized (this.tk) {
            if (collection != null) {
                boolean addAll = this.tk.addAll(collection);
                return addAll;
            }
            return false;
        }
    }

    public void clear() {
        this.tk.clear();
    }

    public Queue<E> fc() {
        return this.tk;
    }

    public ArrayList<E> fd() {
        Object -l_2_R;
        synchronized (this.tk) {
            -l_2_R = new ArrayList();
            Object -l_3_R = this.tk.iterator();
            while (-l_3_R.hasNext()) {
                -l_2_R.add(-l_3_R.next());
            }
        }
        return -l_2_R;
    }

    public boolean offer(E e) {
        synchronized (this.tk) {
            if (e != null) {
                if (this.tk.size() >= this.AY) {
                    this.tk.poll();
                }
                boolean offer = this.tk.offer(e);
                return offer;
            }
            return false;
        }
    }

    public boolean removeAll(Collection<?> collection) {
        boolean removeAll;
        synchronized (this.tk) {
            removeAll = this.tk.removeAll(collection);
        }
        return removeAll;
    }

    public int size() {
        return this.tk.size();
    }
}
