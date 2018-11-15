package com.android.server.autofill;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.content.ComponentName;
import android.metrics.LogMaker;
import android.service.autofill.Dataset;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.view.WindowManager.LayoutParams;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;

public final class Helper {
    private static final String TAG = "AutofillHelper";
    public static boolean sDebug = true;
    public static Boolean sFullScreenMode = null;
    static int sPartitionMaxCount = 10;
    public static boolean sVerbose = true;
    public static int sVisibleDatasetsMaxCount = 3;

    private interface ViewNodeFilter {
        boolean matches(ViewNode viewNode);
    }

    private Helper() {
        throw new UnsupportedOperationException("contains static members only");
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

    public static String paramsToString(LayoutParams params) {
        StringBuilder builder = new StringBuilder(25);
        params.dumpDimensions(builder);
        return builder.toString();
    }

    static ArrayMap<AutofillId, AutofillValue> getFields(Dataset dataset) {
        ArrayList<AutofillId> ids = dataset.getFieldIds();
        ArrayList<AutofillValue> values = dataset.getFieldValues();
        int i = 0;
        int size = ids == null ? 0 : ids.size();
        ArrayMap<AutofillId, AutofillValue> fields = new ArrayMap(size);
        while (i < size) {
            fields.put((AutofillId) ids.get(i), (AutofillValue) values.get(i));
            i++;
        }
        return fields;
    }

    private static LogMaker newLogMaker(int category, String servicePackageName, int sessionId, boolean compatMode) {
        LogMaker log = new LogMaker(category).addTaggedData(908, servicePackageName).addTaggedData(1456, Integer.toString(sessionId));
        if (compatMode) {
            log.addTaggedData(1414, Integer.valueOf(1));
        }
        return log;
    }

    public static LogMaker newLogMaker(int category, String packageName, String servicePackageName, int sessionId, boolean compatMode) {
        return newLogMaker(category, servicePackageName, sessionId, compatMode).setPackageName(packageName);
    }

    public static LogMaker newLogMaker(int category, ComponentName componentName, String servicePackageName, int sessionId, boolean compatMode) {
        return newLogMaker(category, servicePackageName, sessionId, compatMode).setComponentName(componentName);
    }

    public static void printlnRedactedText(PrintWriter pw, CharSequence text) {
        if (text == null) {
            pw.println("null");
            return;
        }
        pw.print(text.length());
        pw.println("_chars");
    }

    public static ViewNode findViewNodeByAutofillId(AssistStructure structure, AutofillId autofillId) {
        return findViewNode(structure, new -$$Lambda$Helper$nK3g_oXXf8NGajcUf0W5JsQzf3w(autofillId));
    }

    private static ViewNode findViewNode(AssistStructure structure, ViewNodeFilter filter) {
        LinkedList<ViewNode> nodesToProcess = new LinkedList();
        int numWindowNodes = structure.getWindowNodeCount();
        for (int i = 0; i < numWindowNodes; i++) {
            nodesToProcess.add(structure.getWindowNodeAt(i).getRootViewNode());
        }
        while (!nodesToProcess.isEmpty()) {
            ViewNode node = (ViewNode) nodesToProcess.removeFirst();
            if (filter.matches(node)) {
                return node;
            }
            for (int i2 = 0; i2 < node.getChildCount(); i2++) {
                nodesToProcess.addLast(node.getChildAt(i2));
            }
        }
        return null;
    }

    public static ViewNode sanitizeUrlBar(AssistStructure structure, String[] urlBarIds) {
        ViewNode urlBarNode = findViewNode(structure, new -$$Lambda$Helper$laLKWmsGqkFIaRXW5rR6_s66Vsw(urlBarIds));
        if (urlBarNode != null) {
            String domain = urlBarNode.getText().toString();
            String str;
            StringBuilder stringBuilder;
            if (domain.isEmpty()) {
                if (sDebug) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("sanitizeUrlBar(): empty on ");
                    stringBuilder.append(urlBarNode.getIdEntry());
                    Slog.d(str, stringBuilder.toString());
                }
                return null;
            }
            urlBarNode.setWebDomain(domain);
            if (sDebug) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("sanitizeUrlBar(): id=");
                stringBuilder.append(urlBarNode.getIdEntry());
                stringBuilder.append(", domain=");
                stringBuilder.append(urlBarNode.getWebDomain());
                Slog.d(str, stringBuilder.toString());
            }
        }
        return urlBarNode;
    }

    static int getNumericValue(LogMaker log, int tag) {
        Object value = log.getTaggedData(tag);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}
