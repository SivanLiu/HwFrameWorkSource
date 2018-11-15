package com.huawei.nearbysdk.closeRange;

import java.util.HashMap;

class CloseRangeBusinessCounter {
    private HashMap<CloseRangeBusinessType, Integer> map = new HashMap();

    CloseRangeBusinessCounter() {
    }

    void increase(CloseRangeBusinessType type) {
        if (this.map.containsKey(type)) {
            this.map.put(type, Integer.valueOf(((Integer) this.map.get(type)).intValue() + 1));
            return;
        }
        this.map.put(type, Integer.valueOf(1));
    }

    void decrease(CloseRangeBusinessType type) {
        if (this.map.containsKey(type)) {
            int val = ((Integer) this.map.get(type)).intValue();
            if (val == 1) {
                this.map.remove(type);
            } else {
                this.map.put(type, Integer.valueOf(val - 1));
            }
        }
    }

    boolean containsType(CloseRangeBusinessType type) {
        return this.map.containsKey(type);
    }
}
