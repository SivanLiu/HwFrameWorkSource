package com.android.internal.widget;

import android.util.Pair;
import java.util.Comparator;

final /* synthetic */ class -$Lambda$LaTFiUorkqfcqmu-zMQbCLeO77c implements Comparator {
    public static final /* synthetic */ -$Lambda$LaTFiUorkqfcqmu-zMQbCLeO77c $INST$0 = new -$Lambda$LaTFiUorkqfcqmu-zMQbCLeO77c();

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return ((Integer) ((Pair) arg0).first).compareTo((Integer) ((Pair) arg1).first);
    }

    private /* synthetic */ -$Lambda$LaTFiUorkqfcqmu-zMQbCLeO77c() {
    }

    public final int compare(Object obj, Object obj2) {
        return $m$0(obj, obj2);
    }
}
