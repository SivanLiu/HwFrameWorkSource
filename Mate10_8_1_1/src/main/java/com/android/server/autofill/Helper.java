package com.android.server.autofill;

import android.metrics.LogMaker;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public final class Helper {
    public static boolean sDebug = false;
    static int sPartitionMaxCount = 10;
    public static boolean sVerbose = false;

    private Helper() {
        throw new UnsupportedOperationException("contains static members only");
    }

    static void append(StringBuilder builder, Bundle bundle) {
        if (bundle == null || (sVerbose ^ 1) != 0) {
            builder.append("null");
            return;
        }
        Set<String> keySet = bundle.keySet();
        builder.append("[Bundle with ").append(keySet.size()).append(" extras:");
        for (String key : keySet) {
            Object obj = bundle.get(key);
            builder.append(' ').append(key).append('=');
            if (obj instanceof Object[]) {
                obj = Arrays.toString((Objects[]) obj);
            }
            builder.append(obj);
        }
        builder.append(']');
    }

    static String bundleToString(Bundle bundle) {
        StringBuilder builder = new StringBuilder();
        append(builder, bundle);
        return builder.toString();
    }

    static AutofillId[] toArray(ArraySet<AutofillId> set) {
        if (set == null) {
            return null;
        }
        AutofillId[] array = new AutofillId[set.size()];
        for (int i = 0; i < set.size(); i++) {
            array[i] = (AutofillId) set.valueAt(i);
        }
        return array;
    }

    static ArrayMap<AutofillId, AutofillValue> getFields(Dataset dataset) {
        ArrayList<AutofillId> ids = dataset.getFieldIds();
        ArrayList<AutofillValue> values = dataset.getFieldValues();
        int size = ids == null ? 0 : ids.size();
        ArrayMap<AutofillId, AutofillValue> fields = new ArrayMap(size);
        for (int i = 0; i < size; i++) {
            fields.put((AutofillId) ids.get(i), (AutofillValue) values.get(i));
        }
        return fields;
    }

    public static LogMaker newLogMaker(int category, String packageName, String servicePackageName) {
        LogMaker log = new LogMaker(category).setPackageName(packageName);
        if (servicePackageName != null) {
            log.addTaggedData(908, servicePackageName);
        }
        return log;
    }
}
