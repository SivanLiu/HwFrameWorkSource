package com.android.internal.widget;

import android.util.Pair;
import java.util.Comparator;

final /* synthetic */ class -$Lambda$LaTFiUorkqfcqmu-zMQbCLeO77c implements Comparator {
    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return ((Integer) ((Pair) arg0).first).compareTo((Integer) ((Pair) arg1).first);
    }

    public final int compare(Object obj, Object obj2) {
        return $m$0(obj, obj2);
    }
}
