package tmsdkobf;

import java.util.LinkedHashMap;

public class pe<K, V> {
    private int AY = -1;
    private LinkedHashMap<K, V> Jr = new LinkedHashMap();

    public pe(int i) {
        this.AY = i;
    }

    public void f(K k) {
        this.Jr.remove(k);
    }

    public V get(K k) {
        return this.Jr.get(k);
    }

    public LinkedHashMap<K, V> hH() {
        return this.Jr;
    }

    public V put(K k, V v) {
        if (this.Jr.size() >= this.AY) {
            Object -l_3_R = this.Jr.keySet();
            if (-l_3_R != null) {
                this.Jr.remove(-l_3_R.iterator().next());
            }
        }
        return this.Jr.put(k, v);
    }

    public int size() {
        return this.Jr.size();
    }
}
