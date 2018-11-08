package tmsdk.common.module.aresengine;

import java.util.HashMap;
import java.util.Map.Entry;

public final class FilterConfig {
    public static final int STATE_ACCEPTABLE = 0;
    public static final int STATE_DISABLE = 3;
    public static final int STATE_EMTPY = 4;
    public static final int STATE_ENABLE = 2;
    public static final int STATE_REJECTABLE = 1;
    private HashMap<Integer, Integer> Ab;

    public FilterConfig() {
        this(null);
    }

    public FilterConfig(String str) {
        this.Ab = new HashMap();
        if (str != null) {
            Object -l_2_R = str.trim().split(",");
            for (int -l_3_I = 0; -l_3_I < -l_2_R.length; -l_3_I += 2) {
                set(Integer.parseInt(-l_2_R[-l_3_I]), Integer.parseInt(-l_2_R[-l_3_I + 1]));
            }
        }
    }

    public String dump() {
        Object -l_1_R = new StringBuffer();
        for (Entry -l_3_R : this.Ab.entrySet()) {
            -l_1_R.append((-l_3_R.getKey() + "," + -l_3_R.getValue()) + ",");
        }
        if (-l_1_R.length() > 0) {
            -l_1_R.deleteCharAt(-l_1_R.length() - 1);
        }
        return -l_1_R.toString();
    }

    public int get(int i) {
        Integer -l_2_R = (Integer) this.Ab.get(Integer.valueOf(i));
        return -l_2_R == null ? 4 : -l_2_R.intValue();
    }

    public void reset() {
        this.Ab.clear();
    }

    public void set(int i, int i2) {
        if (i2 == 0 || i2 == 1 || i2 == 2 || i2 == 3 || i2 == 4) {
            this.Ab.put(Integer.valueOf(i), Integer.valueOf(i2));
            return;
        }
        throw new IllegalStateException("the state " + i2 + " is not define.");
    }
}
