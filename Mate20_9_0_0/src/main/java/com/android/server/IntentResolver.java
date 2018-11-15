package com.android.server;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.FastImmutableArraySet;
import android.util.Log;
import android.util.LogPrinter;
import android.util.MutableInt;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.FastPrintWriter;
import com.android.server.am.HwBroadcastRadarUtil;
import com.android.server.voiceinteraction.DatabaseHelper.SoundModelContract;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class IntentResolver<F extends IntentFilter, R> {
    private static final boolean DEBUG = false;
    private static final boolean HWFLOW;
    private static final String TAG = "IntentResolver";
    private static final boolean localLOGV = false;
    private static final boolean localVerificationLOGV = false;
    private static final Comparator mResolvePrioritySorter = new Comparator() {
        public int compare(Object o1, Object o2) {
            int q1 = ((IntentFilter) o1).getPriority();
            int q2 = ((IntentFilter) o2).getPriority();
            if (q1 > q2) {
                return -1;
            }
            return q1 < q2 ? 1 : 0;
        }
    };
    private final ArrayMap<String, F[]> mActionToFilter = new ArrayMap();
    private final ArrayMap<String, F[]> mBaseTypeToFilter = new ArrayMap();
    private final ArraySet<F> mFilters = new ArraySet();
    private final ArrayMap<String, F[]> mSchemeToFilter = new ArrayMap();
    private final ArrayMap<String, F[]> mTypeToFilter = new ArrayMap();
    private final ArrayMap<String, F[]> mTypedActionToFilter = new ArrayMap();
    private final ArrayMap<String, F[]> mWildTypeToFilter = new ArrayMap();

    private class IteratorWrapper implements Iterator<F> {
        private F mCur;
        private final Iterator<F> mI;

        IteratorWrapper(Iterator<F> it) {
            this.mI = it;
        }

        public boolean hasNext() {
            return this.mI.hasNext();
        }

        public F next() {
            IntentFilter intentFilter = (IntentFilter) this.mI.next();
            this.mCur = intentFilter;
            return intentFilter;
        }

        public void remove() {
            if (this.mCur != null) {
                IntentResolver.this.removeFilterInternal(this.mCur);
            }
            this.mI.remove();
        }
    }

    protected abstract boolean isPackageForFilter(String str, F f);

    protected abstract F[] newArray(int i);

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    public void addFilter(F f) {
        this.mFilters.add(f);
        int numS = register_intent_filter(f, f.schemesIterator(), this.mSchemeToFilter, "      Scheme: ");
        int numT = register_mime_types(f, "      Type: ");
        if (numS == 0 && numT == 0) {
            register_intent_filter(f, f.actionsIterator(), this.mActionToFilter, "      Action: ");
        }
        if (numT != 0) {
            register_intent_filter(f, f.actionsIterator(), this.mTypedActionToFilter, "      TypedAction: ");
        }
    }

    public static boolean filterEquals(IntentFilter f1, IntentFilter f2) {
        int s1 = f1.countActions();
        if (s1 != f2.countActions()) {
            return false;
        }
        int i;
        for (i = 0; i < s1; i++) {
            if (!f2.hasAction(f1.getAction(i))) {
                return false;
            }
        }
        s1 = f1.countCategories();
        if (s1 != f2.countCategories()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasCategory(f1.getCategory(i))) {
                return false;
            }
        }
        s1 = f1.countDataTypes();
        if (s1 != f2.countDataTypes()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasExactDataType(f1.getDataType(i))) {
                return false;
            }
        }
        s1 = f1.countDataSchemes();
        if (s1 != f2.countDataSchemes()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasDataScheme(f1.getDataScheme(i))) {
                return false;
            }
        }
        s1 = f1.countDataAuthorities();
        if (s1 != f2.countDataAuthorities()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasDataAuthority(f1.getDataAuthority(i))) {
                return false;
            }
        }
        s1 = f1.countDataPaths();
        if (s1 != f2.countDataPaths()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasDataPath(f1.getDataPath(i))) {
                return false;
            }
        }
        s1 = f1.countDataSchemeSpecificParts();
        if (s1 != f2.countDataSchemeSpecificParts()) {
            return false;
        }
        for (i = 0; i < s1; i++) {
            if (!f2.hasDataSchemeSpecificPart(f1.getDataSchemeSpecificPart(i))) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<F> collectFilters(F[] array, IntentFilter matching) {
        ArrayList<F> res = null;
        if (array != null) {
            for (F cur : array) {
                if (cur == null) {
                    break;
                }
                if (filterEquals(cur, matching)) {
                    if (res == null) {
                        res = new ArrayList();
                    }
                    res.add(cur);
                }
            }
        }
        return res;
    }

    public ArrayList<F> findFilters(IntentFilter matching) {
        if (matching.countDataSchemes() == 1) {
            return collectFilters((IntentFilter[]) this.mSchemeToFilter.get(matching.getDataScheme(0)), matching);
        }
        if (matching.countDataTypes() != 0 && matching.countActions() == 1) {
            return collectFilters((IntentFilter[]) this.mTypedActionToFilter.get(matching.getAction(0)), matching);
        }
        if (matching.countDataTypes() == 0 && matching.countDataSchemes() == 0 && matching.countActions() == 1) {
            return collectFilters((IntentFilter[]) this.mActionToFilter.get(matching.getAction(0)), matching);
        }
        ArrayList<F> res = null;
        Iterator it = this.mFilters.iterator();
        while (it.hasNext()) {
            IntentFilter cur = (IntentFilter) it.next();
            if (filterEquals(cur, matching)) {
                if (res == null) {
                    res = new ArrayList();
                }
                res.add(cur);
            }
        }
        return res;
    }

    public void removeFilter(F f) {
        removeFilterInternal(f);
        this.mFilters.remove(f);
    }

    void removeFilterInternal(F f) {
        int numS = unregister_intent_filter(f, f.schemesIterator(), this.mSchemeToFilter, "      Scheme: ");
        int numT = unregister_mime_types(f, "      Type: ");
        if (numS == 0 && numT == 0) {
            try {
                unregister_intent_filter(f, f.actionsIterator(), this.mActionToFilter, "      Action: ");
            } catch (ConcurrentModificationException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to Removing filter: ");
                stringBuilder.append(f);
                stringBuilder.append(" while unregistering action error!");
                Slog.e(str, stringBuilder.toString(), e);
            }
        }
        if (numT != 0) {
            unregister_intent_filter(f, f.actionsIterator(), this.mTypedActionToFilter, "      TypedAction: ");
        }
    }

    boolean dumpMap(PrintWriter out, String titlePrefix, String title, String prefix, ArrayMap<String, F[]> map, String packageName, boolean printFilter, boolean collapseDuplicates) {
        Printer printer;
        String title2;
        IntentResolver intentResolver = this;
        PrintWriter printWriter = out;
        String str = prefix;
        ArrayMap<String, F[]> arrayMap = map;
        String str2 = packageName;
        String eprefix = new StringBuilder();
        eprefix.append(str);
        eprefix.append("  ");
        eprefix = eprefix.toString();
        String fprefix = new StringBuilder();
        fprefix.append(str);
        fprefix.append("    ");
        fprefix = fprefix.toString();
        ArrayMap<Object, MutableInt> found = new ArrayMap();
        String title3 = title;
        Printer printer2 = null;
        boolean printedSomething = false;
        int mapi = 0;
        while (mapi < map.size()) {
            boolean printedSomething2;
            String title4;
            IntentFilter[] a = (IntentFilter[]) arrayMap.valueAt(mapi);
            int N = a.length;
            boolean printedHeader = false;
            int i;
            if (!collapseDuplicates || printFilter) {
                printedSomething2 = printedSomething;
                printer = printer2;
                title4 = title3;
                i = 0;
                while (i < N) {
                    F f = a[i];
                    F filter = f;
                    if (f == null) {
                        break;
                    }
                    if (str2 == null || intentResolver.isPackageForFilter(str2, filter)) {
                        if (title4 != null) {
                            out.print(titlePrefix);
                            printWriter.println(title4);
                            title4 = null;
                        }
                        if (!printedHeader) {
                            printWriter.print(eprefix);
                            printWriter.print((String) arrayMap.keyAt(mapi));
                            printWriter.println(":");
                            printedHeader = true;
                        }
                        intentResolver.dumpFilter(printWriter, fprefix, filter);
                        if (printFilter) {
                            if (printer == null) {
                                printer2 = new PrintWriterPrinter(printWriter);
                            } else {
                                printer2 = printer;
                            }
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(fprefix);
                            stringBuilder.append("  ");
                            filter.dump(printer2, stringBuilder.toString());
                            printedSomething2 = true;
                            printer = printer2;
                        } else {
                            printedSomething2 = true;
                        }
                    }
                    i++;
                    intentResolver = this;
                    printWriter = out;
                }
            } else {
                found.clear();
                int i2 = 0;
                while (true) {
                    int i3 = i2;
                    if (i3 >= N) {
                        break;
                    }
                    F f2 = a[i3];
                    F filter2 = f2;
                    if (f2 == null) {
                        break;
                    }
                    F filter3;
                    if (str2 != null) {
                        filter3 = filter2;
                        if (!intentResolver.isPackageForFilter(str2, filter3)) {
                            F f3 = filter3;
                            printedSomething2 = printedSomething;
                            title2 = title3;
                            printer = printer2;
                            i2 = i3 + 1;
                            title3 = title2;
                            printer2 = printer;
                            printedSomething = printedSomething2;
                            str = prefix;
                        }
                    } else {
                        filter3 = filter2;
                    }
                    title2 = title3;
                    Object label = intentResolver.filterToLabel(filter3);
                    i = found.indexOfKey(label);
                    printer = printer2;
                    if (i < 0) {
                        printedSomething2 = printedSomething;
                        found.put(label, new MutableInt(1));
                    } else {
                        printedSomething2 = printedSomething;
                        MutableInt printedSomething3 = (MutableInt) found.valueAt(i);
                        printedSomething3.value++;
                    }
                    i2 = i3 + 1;
                    title3 = title2;
                    printer2 = printer;
                    printedSomething = printedSomething2;
                    str = prefix;
                }
                printedSomething2 = printedSomething;
                printer = printer2;
                title4 = title3;
                for (i = 0; i < found.size(); i++) {
                    if (title4 != null) {
                        out.print(titlePrefix);
                        printWriter.println(title4);
                        title4 = null;
                    }
                    if (!printedHeader) {
                        printWriter.print(eprefix);
                        printWriter.print((String) arrayMap.keyAt(mapi));
                        printWriter.println(":");
                        printedHeader = true;
                    }
                    printedSomething2 = true;
                    intentResolver.dumpFilterLabel(printWriter, fprefix, found.keyAt(i), ((MutableInt) found.valueAt(i)).value);
                }
            }
            title3 = title4;
            printer2 = printer;
            printedSomething = printedSomething2;
            mapi++;
            intentResolver = this;
            printWriter = out;
            str = prefix;
        }
        title2 = title3;
        printer = printer2;
        return printedSomething;
    }

    void writeProtoMap(ProtoOutputStream proto, long fieldId, ArrayMap<String, F[]> map) {
        ProtoOutputStream protoOutputStream = proto;
        ArrayMap<String, F[]> arrayMap = map;
        int N = map.size();
        for (int mapi = 0; mapi < N; mapi++) {
            long token = proto.start(fieldId);
            protoOutputStream.write(1138166333441L, (String) arrayMap.keyAt(mapi));
            for (F f : (IntentFilter[]) arrayMap.valueAt(mapi)) {
                if (f != null) {
                    protoOutputStream.write(2237677961218L, f.toString());
                }
            }
            protoOutputStream.end(token);
        }
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        writeProtoMap(proto, 2246267895809L, this.mTypeToFilter);
        writeProtoMap(proto, 2246267895810L, this.mBaseTypeToFilter);
        writeProtoMap(proto, 2246267895811L, this.mWildTypeToFilter);
        writeProtoMap(proto, 2246267895812L, this.mSchemeToFilter);
        writeProtoMap(proto, 2246267895813L, this.mActionToFilter);
        writeProtoMap(proto, 2246267895814L, this.mTypedActionToFilter);
        proto.end(token);
    }

    public boolean dump(PrintWriter out, String title, String prefix, String packageName, boolean printFilter, boolean collapseDuplicates) {
        String str = prefix;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("  ");
        String innerPrefix = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        stringBuilder.append(str);
        String sepPrefix = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(title);
        stringBuilder.append("\n");
        stringBuilder.append(str);
        String stringBuilder2 = stringBuilder.toString();
        if (dumpMap(out, stringBuilder2, "Full MIME Types:", innerPrefix, this.mTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            stringBuilder2 = sepPrefix;
        }
        if (dumpMap(out, stringBuilder2, "Base MIME Types:", innerPrefix, this.mBaseTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            stringBuilder2 = sepPrefix;
        }
        if (dumpMap(out, stringBuilder2, "Wild MIME Types:", innerPrefix, this.mWildTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            stringBuilder2 = sepPrefix;
        }
        if (dumpMap(out, stringBuilder2, "Schemes:", innerPrefix, this.mSchemeToFilter, packageName, printFilter, collapseDuplicates)) {
            stringBuilder2 = sepPrefix;
        }
        if (dumpMap(out, stringBuilder2, "Non-Data Actions:", innerPrefix, this.mActionToFilter, packageName, printFilter, collapseDuplicates)) {
            stringBuilder2 = sepPrefix;
        }
        if (dumpMap(out, stringBuilder2, "MIME Typed Actions:", innerPrefix, this.mTypedActionToFilter, packageName, printFilter, collapseDuplicates)) {
            stringBuilder2 = sepPrefix;
        }
        return stringBuilder2 == sepPrefix;
    }

    public Iterator<F> filterIterator() {
        return new IteratorWrapper(this.mFilters.iterator());
    }

    public Set<F> filterSet() {
        return Collections.unmodifiableSet(this.mFilters);
    }

    public List<R> queryIntentFromList(Intent intent, String resolvedType, boolean defaultOnly, ArrayList<F[]> listCut, int userId) {
        ArrayList<R> resultList = new ArrayList();
        int i = 0;
        boolean debug = (intent.getFlags() & 8) != 0;
        FastImmutableArraySet<String> categories = getFastIntentCategories(intent);
        String scheme = intent.getScheme();
        int N = listCut.size();
        while (true) {
            int i2 = i;
            if (i2 < N) {
                buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme, (IntentFilter[]) listCut.get(i2), resultList, userId);
                i = i2 + 1;
            } else {
                filterResults(resultList);
                sortResults(resultList);
                return resultList;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:40:0x0162  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x0145  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x01c8  */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x01a1  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x01e3  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0205  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x0217  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x022b  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x023e  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0258  */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x01a1  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x01c8  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x01e3  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0205  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x0217  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x022b  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x023e  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0258  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x01c8  */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x01a1  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x01e3  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x0205  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x0217  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x022b  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x023e  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0258  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<R> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
        boolean z;
        Intent intent2;
        F[] secondTypeCut;
        F[] thirdTypeCut;
        F[] firstTypeCut;
        FastImmutableArraySet<String> categories;
        F[] schemeCut;
        String str = resolvedType;
        String scheme = intent.getScheme();
        ArrayList<R> finalList = new ArrayList();
        boolean debug = (intent.getFlags() & 8) != 0;
        if (debug) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Resolving type=");
            stringBuilder.append(str);
            stringBuilder.append(" scheme=");
            stringBuilder.append(scheme);
            stringBuilder.append(" defaultOnly=");
            z = defaultOnly;
            stringBuilder.append(z);
            stringBuilder.append(" userId=");
            stringBuilder.append(userId);
            stringBuilder.append(" of ");
            intent2 = intent;
            stringBuilder.append(intent2);
            Slog.v(str2, stringBuilder.toString());
        } else {
            intent2 = intent;
            z = defaultOnly;
            int i = userId;
        }
        F[] firstTypeCut2 = null;
        if (str != null) {
            int slashpos = str.indexOf(47);
            if (slashpos > 0) {
                String baseType = str.substring(0, slashpos);
                String str3;
                StringBuilder stringBuilder2;
                if (baseType.equals("*")) {
                    secondTypeCut = null;
                    if (intent.getAction() != null) {
                        firstTypeCut2 = (IntentFilter[]) this.mTypedActionToFilter.get(intent.getAction());
                        if (debug) {
                            str3 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Typed Action list: ");
                            stringBuilder2.append(Arrays.toString(firstTypeCut2));
                            Slog.v(str3, stringBuilder2.toString());
                        }
                    }
                    thirdTypeCut = null;
                    if (scheme == null) {
                    }
                    firstTypeCut2 = (IntentFilter[]) this.mActionToFilter.get(intent.getAction());
                    if (debug) {
                    }
                    firstTypeCut = firstTypeCut2;
                    categories = getFastIntentCategories(intent);
                    if (firstTypeCut != null) {
                    }
                    if (secondTypeCut != null) {
                    }
                    if (thirdTypeCut != null) {
                    }
                    if (schemeCut != null) {
                    }
                    filterResults(finalList);
                    sortResults(finalList);
                    if (debug) {
                    }
                    return finalList;
                }
                String str4;
                StringBuilder stringBuilder3;
                F[] secondTypeCut2;
                F[] thirdTypeCut2;
                F[] firstTypeCut3;
                if (resolvedType.length() != slashpos + 2) {
                    secondTypeCut = null;
                } else if (str.charAt(slashpos + 1) != '*') {
                    secondTypeCut = null;
                } else {
                    StringBuilder stringBuilder4;
                    firstTypeCut2 = (IntentFilter[]) this.mBaseTypeToFilter.get(baseType);
                    if (debug) {
                        str4 = TAG;
                        stringBuilder3 = new StringBuilder();
                        secondTypeCut = null;
                        stringBuilder3.append("First type cut: ");
                        stringBuilder3.append(Arrays.toString(firstTypeCut2));
                        Slog.v(str4, stringBuilder3.toString());
                    } else {
                        secondTypeCut = null;
                    }
                    secondTypeCut2 = (IntentFilter[]) this.mWildTypeToFilter.get(baseType);
                    if (debug) {
                        str4 = TAG;
                        stringBuilder3 = new StringBuilder();
                        schemeCut = firstTypeCut2;
                        stringBuilder3.append("Second type cut: ");
                        stringBuilder3.append(Arrays.toString(secondTypeCut2));
                        Slog.v(str4, stringBuilder3.toString());
                    } else {
                        schemeCut = firstTypeCut2;
                    }
                    firstTypeCut2 = schemeCut;
                    thirdTypeCut2 = (IntentFilter[]) this.mWildTypeToFilter.get("*");
                    if (debug) {
                        firstTypeCut3 = firstTypeCut2;
                    } else {
                        str4 = TAG;
                        stringBuilder3 = new StringBuilder();
                        firstTypeCut3 = firstTypeCut2;
                        stringBuilder3.append("Third type cut: ");
                        stringBuilder3.append(Arrays.toString(thirdTypeCut2));
                        Slog.v(str4, stringBuilder3.toString());
                    }
                    secondTypeCut = secondTypeCut2;
                    thirdTypeCut = thirdTypeCut2;
                    firstTypeCut2 = firstTypeCut3;
                    if (scheme == null) {
                        secondTypeCut2 = (IntentFilter[]) this.mSchemeToFilter.get(scheme);
                        if (debug) {
                            String str5 = TAG;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("Scheme list: ");
                            stringBuilder5.append(Arrays.toString(secondTypeCut2));
                            Slog.v(str5, stringBuilder5.toString());
                        }
                        schemeCut = secondTypeCut2;
                    } else {
                        schemeCut = null;
                    }
                    if (str == null && scheme == null && intent.getAction() != null) {
                        firstTypeCut2 = (IntentFilter[]) this.mActionToFilter.get(intent.getAction());
                        if (debug) {
                            str3 = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Action list: ");
                            stringBuilder4.append(Arrays.toString(firstTypeCut2));
                            Slog.v(str3, stringBuilder4.toString());
                        }
                    }
                    firstTypeCut = firstTypeCut2;
                    categories = getFastIntentCategories(intent);
                    if (firstTypeCut != null) {
                        buildResolveList(intent2, categories, debug, z, str, scheme, firstTypeCut, finalList, userId);
                    }
                    if (secondTypeCut != null) {
                        buildResolveList(intent, categories, debug, defaultOnly, str, scheme, secondTypeCut, finalList, userId);
                    }
                    if (thirdTypeCut != null) {
                        buildResolveList(intent, categories, debug, defaultOnly, str, scheme, thirdTypeCut, finalList, userId);
                    }
                    if (schemeCut != null) {
                        buildResolveList(intent, categories, debug, defaultOnly, str, scheme, schemeCut, finalList, userId);
                    }
                    filterResults(finalList);
                    sortResults(finalList);
                    if (debug) {
                        Slog.v(TAG, "Final result list:");
                        int i2 = 0;
                        while (true) {
                            int i3 = i2;
                            if (i3 >= finalList.size()) {
                                break;
                            }
                            str3 = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("  ");
                            stringBuilder4.append(finalList.get(i3));
                            Slog.v(str3, stringBuilder4.toString());
                            i2 = i3 + 1;
                        }
                    }
                    return finalList;
                }
                firstTypeCut2 = (IntentFilter[]) this.mTypeToFilter.get(str);
                if (debug) {
                    str3 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("First type cut: ");
                    stringBuilder2.append(Arrays.toString(firstTypeCut2));
                    Slog.v(str3, stringBuilder2.toString());
                }
                secondTypeCut2 = (IntentFilter[]) this.mWildTypeToFilter.get(baseType);
                if (debug) {
                    str4 = TAG;
                    stringBuilder3 = new StringBuilder();
                    firstTypeCut = firstTypeCut2;
                    stringBuilder3.append("Second type cut: ");
                    stringBuilder3.append(Arrays.toString(secondTypeCut2));
                    Slog.v(str4, stringBuilder3.toString());
                } else {
                    firstTypeCut = firstTypeCut2;
                }
                firstTypeCut2 = firstTypeCut;
                thirdTypeCut2 = (IntentFilter[]) this.mWildTypeToFilter.get("*");
                if (debug) {
                }
                secondTypeCut = secondTypeCut2;
                thirdTypeCut = thirdTypeCut2;
                firstTypeCut2 = firstTypeCut3;
                if (scheme == null) {
                }
                firstTypeCut2 = (IntentFilter[]) this.mActionToFilter.get(intent.getAction());
                if (debug) {
                }
                firstTypeCut = firstTypeCut2;
                categories = getFastIntentCategories(intent);
                if (firstTypeCut != null) {
                }
                if (secondTypeCut != null) {
                }
                if (thirdTypeCut != null) {
                }
                if (schemeCut != null) {
                }
                filterResults(finalList);
                sortResults(finalList);
                if (debug) {
                }
                return finalList;
            }
        }
        secondTypeCut = null;
        thirdTypeCut = null;
        if (scheme == null) {
        }
        firstTypeCut2 = (IntentFilter[]) this.mActionToFilter.get(intent.getAction());
        if (debug) {
        }
        firstTypeCut = firstTypeCut2;
        categories = getFastIntentCategories(intent);
        if (firstTypeCut != null) {
        }
        if (secondTypeCut != null) {
        }
        if (thirdTypeCut != null) {
        }
        if (schemeCut != null) {
        }
        filterResults(finalList);
        sortResults(finalList);
        if (debug) {
        }
        return finalList;
    }

    protected boolean allowFilterResult(F f, List<R> list) {
        return true;
    }

    protected boolean isFilterStopped(F f, int userId) {
        return false;
    }

    protected boolean isFilterVerified(F filter) {
        return filter.isVerified();
    }

    protected R newResult(F filter, int match, int userId) {
        return filter;
    }

    protected void sortResults(List<R> results) {
        Collections.sort(results, mResolvePrioritySorter);
    }

    protected void filterResults(List<R> list) {
    }

    protected void dumpFilter(PrintWriter out, String prefix, F filter) {
        out.print(prefix);
        out.println(filter);
    }

    protected Object filterToLabel(F f) {
        return "IntentFilter";
    }

    protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
        out.print(prefix);
        out.print(label);
        out.print(": ");
        out.println(count);
    }

    private final void addFilter(ArrayMap<String, F[]> map, String name, F filter) {
        IntentFilter[] array = (IntentFilter[]) map.get(name);
        if (array == null) {
            F[] array2 = newArray(2);
            map.put(name, array2);
            array2[0] = filter;
            return;
        }
        int N = array.length;
        int i = N;
        while (i > 0 && array[i - 1] == null) {
            i--;
        }
        if (i < N) {
            array[i] = filter;
            return;
        }
        F[] newa = newArray((N * 3) / 2);
        System.arraycopy(array, 0, newa, 0, N);
        newa[N] = filter;
        map.put(name, newa);
    }

    private final int register_mime_types(F filter, String prefix) {
        Iterator<String> i = filter.typesIterator();
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = (String) i.next();
            num++;
            String baseName = name;
            int slashpos = name.indexOf(47);
            if (slashpos > 0) {
                baseName = name.substring(0, slashpos).intern();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(name);
                stringBuilder.append("/*");
                name = stringBuilder.toString();
            }
            addFilter(this.mTypeToFilter, name, filter);
            if (slashpos > 0) {
                addFilter(this.mBaseTypeToFilter, baseName, filter);
            } else {
                addFilter(this.mWildTypeToFilter, baseName, filter);
            }
        }
        return num;
    }

    private final int unregister_mime_types(F filter, String prefix) {
        Iterator<String> i = filter.typesIterator();
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = (String) i.next();
            num++;
            String baseName = name;
            int slashpos = name.indexOf(47);
            if (slashpos > 0) {
                baseName = name.substring(0, slashpos).intern();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(name);
                stringBuilder.append("/*");
                name = stringBuilder.toString();
            }
            remove_all_objects(this.mTypeToFilter, name, filter);
            if (slashpos > 0) {
                remove_all_objects(this.mBaseTypeToFilter, baseName, filter);
            } else {
                remove_all_objects(this.mWildTypeToFilter, baseName, filter);
            }
        }
        return num;
    }

    private final int register_intent_filter(F filter, Iterator<String> i, ArrayMap<String, F[]> dest, String prefix) {
        int num = 0;
        if (i == null) {
            return 0;
        }
        while (i.hasNext()) {
            num++;
            addFilter(dest, (String) i.next(), filter);
        }
        return num;
    }

    private final int unregister_intent_filter(F filter, Iterator<String> i, ArrayMap<String, F[]> dest, String prefix) {
        int num = 0;
        if (i == null) {
            return 0;
        }
        while (i.hasNext()) {
            num++;
            remove_all_objects(dest, (String) i.next(), filter);
        }
        return num;
    }

    private final void remove_all_objects(ArrayMap<String, F[]> map, String name, Object object) {
        IntentFilter[] array = (IntentFilter[]) map.get(name);
        if (array != null) {
            int idx = array.length - 1;
            while (idx >= 0 && array[idx] == null) {
                idx--;
            }
            int LAST = idx;
            while (idx >= 0) {
                if (array[idx] == object) {
                    int remain = LAST - idx;
                    if (remain > 0) {
                        System.arraycopy(array, idx + 1, array, idx, remain);
                    }
                    array[LAST] = null;
                    LAST--;
                }
                idx--;
            }
            if (LAST < 0) {
                map.remove(name);
            } else if (LAST < array.length / 2) {
                F[] newa = newArray(LAST + 2);
                System.arraycopy(array, 0, newa, 0, LAST + 1);
                map.put(name, newa);
            }
        }
    }

    private static FastImmutableArraySet<String> getFastIntentCategories(Intent intent) {
        Set<String> categories = intent.getCategories();
        if (categories == null) {
            return null;
        }
        return new FastImmutableArraySet((String[]) categories.toArray(new String[categories.size()]));
    }

    private void buildResolveList(Intent intent, FastImmutableArraySet<String> categories, boolean debug, boolean defaultOnly, String resolvedType, String scheme, F[] src, List<R> dest, int userId) {
        Printer logPrinter;
        PrintWriter logPrintWriter;
        int i;
        int i2;
        int i3;
        String action;
        Uri data;
        String packageName;
        int i4 = src;
        List<R> list = dest;
        int i5 = userId;
        String action2 = intent.getAction();
        Uri data2 = intent.getData();
        String packageName2 = intent.getPackage();
        boolean excludingStopped = intent.isExcludingStopped();
        if (debug) {
            logPrinter = new LogPrinter(2, TAG, 3);
            logPrintWriter = new FastPrintWriter(logPrinter);
        } else {
            logPrinter = null;
            logPrintWriter = null;
        }
        Printer logPrinter2 = logPrinter;
        PrintWriter logPrintWriter2 = logPrintWriter;
        int N = i4 != 0 ? i4.length : 0;
        boolean hasNonDefaults = false;
        int i6 = 0;
        while (true) {
            i = i6;
            if (i < N) {
                F f = i4[i];
                F filter = f;
                if (f != null) {
                    String str;
                    StringBuilder stringBuilder;
                    PrintWriter logPrintWriter3;
                    Printer logPrinter3;
                    if (debug && HWFLOW) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Matching against filter ");
                        stringBuilder.append(filter);
                        Slog.v(str, stringBuilder.toString());
                    }
                    String str2;
                    StringBuilder stringBuilder2;
                    if (excludingStopped && isFilterStopped(filter, i5)) {
                        if (debug) {
                            Slog.v(TAG, "  Filter's target is stopped; skipping");
                        }
                    } else if (packageName2 == null || isPackageForFilter(packageName2, filter)) {
                        String str3;
                        if (filter.getAutoVerify() && debug) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("  Filter verified: ");
                            stringBuilder2.append(isFilterVerified(filter));
                            Slog.v(str2, stringBuilder2.toString());
                            i4 = filter.countDataAuthorities();
                            i6 = 0;
                            while (i6 < i4) {
                                str3 = TAG;
                                i2 = i4;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                i3 = i;
                                stringBuilder3.append("   ");
                                stringBuilder3.append(filter.getDataAuthority(i6).getHost());
                                Slog.v(str3, stringBuilder3.toString());
                                i6++;
                                i4 = i2;
                                i = i3;
                            }
                        }
                        i3 = i;
                        F filter2;
                        if (allowFilterResult(filter, list)) {
                            action = action2;
                            i2 = i3;
                            filter2 = filter;
                            i3 = N;
                            Uri uri = data2;
                            data = data2;
                            logPrintWriter3 = logPrintWriter2;
                            packageName = packageName2;
                            logPrinter3 = logPrinter2;
                            i4 = filter.match(action2, resolvedType, scheme, uri, categories, TAG);
                            if (i4 >= 0) {
                                if (debug) {
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("  Filter matched!  match=0x");
                                    stringBuilder.append(Integer.toHexString(i4));
                                    stringBuilder.append(" hasDefault=");
                                    stringBuilder.append(filter2.hasCategory("android.intent.category.DEFAULT"));
                                    Slog.v(str, stringBuilder.toString());
                                }
                                if (!defaultOnly || filter2.hasCategory("android.intent.category.DEFAULT")) {
                                    R oneResult = newResult(filter2, i4, i5);
                                    if (oneResult != null) {
                                        list.add(oneResult);
                                        if (debug) {
                                            dumpFilter(logPrintWriter3, "    ", filter2);
                                            logPrintWriter3.flush();
                                            filter2.dump(logPrinter3, "    ");
                                        }
                                    }
                                } else {
                                    hasNonDefaults = true;
                                }
                            } else if (debug && HWFLOW) {
                                switch (i4) {
                                    case -4:
                                        str3 = "category";
                                        break;
                                    case -3:
                                        str3 = HwBroadcastRadarUtil.KEY_ACTION;
                                        break;
                                    case -2:
                                        str3 = "data";
                                        break;
                                    case -1:
                                        str3 = SoundModelContract.KEY_TYPE;
                                        break;
                                    default:
                                        str3 = "unknown reason";
                                        break;
                                }
                                String str4 = TAG;
                                i = new StringBuilder();
                                i.append("  Filter did not match: ");
                                i.append(str3);
                                Slog.v(str4, i.toString());
                            }
                            i6 = i2 + 1;
                            logPrintWriter2 = logPrintWriter3;
                            logPrinter2 = logPrinter3;
                            N = i3;
                            action2 = action;
                            data2 = data;
                            packageName2 = packageName;
                            i4 = src;
                        } else {
                            if (debug) {
                                Slog.v(TAG, "  Filter's target already added");
                            }
                            action = action2;
                            data = data2;
                            packageName = packageName2;
                            i2 = i3;
                            filter2 = filter;
                            i3 = N;
                            logPrintWriter3 = logPrintWriter2;
                            logPrinter3 = logPrinter2;
                            i6 = i2 + 1;
                            logPrintWriter2 = logPrintWriter3;
                            logPrinter2 = logPrinter3;
                            N = i3;
                            action2 = action;
                            data2 = data;
                            packageName2 = packageName;
                            i4 = src;
                        }
                    } else if (debug) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("  Filter is not from package ");
                        stringBuilder2.append(packageName2);
                        stringBuilder2.append("; skipping");
                        Slog.v(str2, stringBuilder2.toString());
                    }
                    i2 = i;
                    i3 = N;
                    action = action2;
                    data = data2;
                    packageName = packageName2;
                    logPrintWriter3 = logPrintWriter2;
                    logPrinter3 = logPrinter2;
                    i6 = i2 + 1;
                    logPrintWriter2 = logPrintWriter3;
                    logPrinter2 = logPrinter3;
                    N = i3;
                    action2 = action;
                    data2 = data;
                    packageName2 = packageName;
                    i4 = src;
                }
            }
        }
        i2 = i;
        i3 = N;
        action = action2;
        data = data2;
        packageName = packageName2;
        if (!debug || !hasNonDefaults) {
            return;
        }
        if (dest.size() == 0) {
            Slog.v(TAG, "resolveIntent failed: found match, but none with CATEGORY_DEFAULT");
        } else if (dest.size() > 1) {
            Slog.v(TAG, "resolveIntent: multiple matches, only some with CATEGORY_DEFAULT");
        }
    }
}
