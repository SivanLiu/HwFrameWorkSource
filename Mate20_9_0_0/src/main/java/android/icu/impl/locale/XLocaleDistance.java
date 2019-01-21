package android.icu.impl.locale;

import android.icu.impl.ICUResourceBundle;
import android.icu.impl.Row;
import android.icu.impl.Row.R4;
import android.icu.impl.Utility;
import android.icu.impl.locale.XCldrStub.CollectionUtilities;
import android.icu.impl.locale.XCldrStub.ImmutableMap;
import android.icu.impl.locale.XCldrStub.ImmutableMultimap;
import android.icu.impl.locale.XCldrStub.ImmutableSet;
import android.icu.impl.locale.XCldrStub.LinkedHashMultimap;
import android.icu.impl.locale.XCldrStub.Multimap;
import android.icu.impl.locale.XCldrStub.Multimaps;
import android.icu.impl.locale.XCldrStub.Predicate;
import android.icu.impl.locale.XCldrStub.Splitter;
import android.icu.impl.locale.XCldrStub.TreeMultimap;
import android.icu.impl.locale.XLikelySubtags.LSR;
import android.icu.text.LocaleDisplayNames;
import android.icu.text.PluralRules;
import android.icu.util.LocaleMatcher;
import android.icu.util.Output;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundleIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class XLocaleDistance {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final int ABOVE_THRESHOLD = 100;
    private static final Set<String> ALL_FINAL_REGIONS = ImmutableSet.copyOf(CONTAINER_TO_CONTAINED_FINAL.get("001"));
    @Deprecated
    public static final String ANY = "�";
    static final Multimap<String, String> CONTAINER_TO_CONTAINED = xGetContainment();
    static final Multimap<String, String> CONTAINER_TO_CONTAINED_FINAL;
    private static final XLocaleDistance DEFAULT;
    static final boolean PRINT_OVERRIDES = false;
    static final LocaleDisplayNames english = LocaleDisplayNames.getInstance(ULocale.ENGLISH);
    private final int defaultLanguageDistance;
    private final int defaultRegionDistance;
    private final int defaultScriptDistance;
    private final DistanceTable languageDesired2Supported;
    private final RegionMapper regionMapper;

    @Deprecated
    public static class DistanceNode {
        final int distance;

        public DistanceNode(int distance) {
            this.distance = distance;
        }

        public DistanceTable getDistanceTable() {
            return null;
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && obj.getClass() == getClass() && this.distance == ((DistanceNode) obj).distance);
        }

        public int hashCode() {
            return this.distance;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\ndistance: ");
            stringBuilder.append(this.distance);
            return stringBuilder.toString();
        }
    }

    public enum DistanceOption {
        NORMAL,
        SCRIPT_FIRST
    }

    @Deprecated
    public static abstract class DistanceTable {
        abstract Set<String> getCloser(int i);

        abstract int getDistance(String str, String str2, Output<DistanceTable> output, boolean z);

        abstract String toString(boolean z);

        public DistanceTable compact() {
            return this;
        }

        public DistanceNode getInternalNode(String any, String any2) {
            return null;
        }

        public Map<String, Set<String>> getInternalMatches() {
            return null;
        }

        public boolean isEmpty() {
            return true;
        }
    }

    private interface IdMapper<K, V> {
        V toId(K k);
    }

    private static class RegionSet {
        private Operation operation;
        private final Set<String> tempRegions;

        private enum Operation {
            add,
            remove
        }

        private RegionSet() {
            this.tempRegions = new TreeSet();
            this.operation = null;
        }

        private Set<String> parseSet(String barString) {
            this.operation = Operation.add;
            int last = 0;
            this.tempRegions.clear();
            int i = 0;
            while (i < barString.length()) {
                char c = barString.charAt(i);
                if (c == '+') {
                    add(barString, last, i);
                    last = i + 1;
                    this.operation = Operation.add;
                } else if (c == '-') {
                    add(barString, last, i);
                    last = i + 1;
                    this.operation = Operation.remove;
                }
                i++;
            }
            add(barString, last, i);
            return this.tempRegions;
        }

        private Set<String> inverse() {
            TreeSet<String> result = new TreeSet(XLocaleDistance.ALL_FINAL_REGIONS);
            result.removeAll(this.tempRegions);
            return result;
        }

        private void add(String barString, int last, int i) {
            if (i > last) {
                changeSet(this.operation, barString.substring(last, i));
            }
        }

        private void changeSet(Operation operation, String region) {
            Collection<String> contained = XLocaleDistance.CONTAINER_TO_CONTAINED_FINAL.get(region);
            if (contained == null || contained.isEmpty()) {
                if (Operation.add == operation) {
                    this.tempRegions.add(region);
                } else {
                    this.tempRegions.remove(region);
                }
            } else if (Operation.add == operation) {
                this.tempRegions.addAll(contained);
            } else {
                this.tempRegions.removeAll(contained);
            }
        }
    }

    static class AddSub implements Predicate<DistanceNode> {
        private final String desiredSub;
        private final CopyIfEmpty r;
        private final String supportedSub;

        AddSub(String desiredSub, String supportedSub, StringDistanceTable distanceTableToCopy) {
            this.r = new CopyIfEmpty(distanceTableToCopy);
            this.desiredSub = desiredSub;
            this.supportedSub = supportedSub;
        }

        public boolean test(DistanceNode node) {
            if (node != null) {
                ((StringDistanceNode) node).addSubtables(this.desiredSub, this.supportedSub, this.r);
                return true;
            }
            throw new IllegalArgumentException("bad structure");
        }
    }

    static class CopyIfEmpty implements Predicate<DistanceNode> {
        private final StringDistanceTable toCopy;

        CopyIfEmpty(StringDistanceTable resetIfNotNull) {
            this.toCopy = resetIfNotNull;
        }

        public boolean test(DistanceNode node) {
            StringDistanceTable subtables = (StringDistanceTable) node.getDistanceTable();
            if (subtables.subtables.isEmpty()) {
                subtables.copy(this.toCopy);
            }
            return true;
        }
    }

    static class IdMakerFull<T> implements IdMapper<T, Integer> {
        private final List<T> intToObject;
        final String name;
        private final Map<T, Integer> objectToInt;

        IdMakerFull(String name) {
            this.objectToInt = new HashMap();
            this.intToObject = new ArrayList();
            this.name = name;
        }

        IdMakerFull() {
            this("unnamed");
        }

        IdMakerFull(String name, T zeroValue) {
            this(name);
            add(zeroValue);
        }

        public Integer add(T source) {
            Integer result = (Integer) this.objectToInt.get(source);
            if (result != null) {
                return result;
            }
            Integer newResult = Integer.valueOf(this.intToObject.size());
            this.objectToInt.put(source, newResult);
            this.intToObject.add(source);
            return newResult;
        }

        public Integer toId(T source) {
            return (Integer) this.objectToInt.get(source);
        }

        public T fromId(int id) {
            return this.intToObject.get(id);
        }

        public T intern(T source) {
            return fromId(add(source).intValue());
        }

        public int size() {
            return this.intToObject.size();
        }

        public Integer getOldAndAdd(T source) {
            Integer result = (Integer) this.objectToInt.get(source);
            if (result == null) {
                this.objectToInt.put(source, Integer.valueOf(this.intToObject.size()));
                this.intToObject.add(source);
            }
            return result;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(size());
            stringBuilder.append(PluralRules.KEYWORD_RULE_SEPARATOR);
            stringBuilder.append(this.intToObject);
            return stringBuilder.toString();
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && obj.getClass() == getClass() && this.intToObject.equals(((IdMakerFull) obj).intToObject));
        }

        public int hashCode() {
            return this.intToObject.hashCode();
        }
    }

    static class RegionMapper implements IdMapper<String, String> {
        final Multimap<String, String> macroToPartitions;
        final Set<ULocale> paradigms;
        final Map<String, String> regionToPartition;
        final Multimap<String, String> variableToPartition;

        static class Builder {
            private final Set<ULocale> paradigms = new LinkedHashSet();
            private final RegionSet regionSet = new RegionSet();
            private final Multimap<String, String> regionToRawPartition = TreeMultimap.create();

            Builder() {
            }

            void add(String variable, String barString) {
                for (String region : this.regionSet.parseSet(barString)) {
                    this.regionToRawPartition.put(region, variable);
                }
                Set<String> inverse = this.regionSet.inverse();
                String region2 = new StringBuilder();
                region2.append("$!");
                region2.append(variable.substring(1));
                region2 = region2.toString();
                for (String region3 : inverse) {
                    this.regionToRawPartition.put(region3, region2);
                }
            }

            public Builder addParadigms(String... paradigmRegions) {
                for (String paradigm : paradigmRegions) {
                    this.paradigms.add(new ULocale(paradigm));
                }
                return this;
            }

            RegionMapper build() {
                String region;
                IdMakerFull<Collection<String>> id = new IdMakerFull("partition");
                Multimap<String, String> variableToPartitions = TreeMultimap.create();
                Map regionToPartition = new TreeMap();
                Multimap<String, String> partitionToRegions = TreeMultimap.create();
                for (Entry<String, Set<String>> e : this.regionToRawPartition.asMap().entrySet()) {
                    region = (String) e.getKey();
                    Collection<String> rawPartition = (Collection) e.getValue();
                    String partition = String.valueOf((char) (945 + id.add(rawPartition).intValue()));
                    regionToPartition.put(region, partition);
                    partitionToRegions.put(partition, region);
                    for (String variable : rawPartition) {
                        variableToPartitions.put(variable, partition);
                    }
                }
                Multimap<String, String> macroToPartitions = TreeMultimap.create();
                for (Entry<String, Set<String>> e2 : XLocaleDistance.CONTAINER_TO_CONTAINED.asMap().entrySet()) {
                    region = (String) e2.getKey();
                    for (Entry<String, Set<String>> e22 : partitionToRegions.asMap().entrySet()) {
                        String partition2 = (String) e22.getKey();
                        if (!Collections.disjoint((Collection) e2.getValue(), (Collection) e22.getValue())) {
                            macroToPartitions.put(region, partition2);
                        }
                    }
                }
                return new RegionMapper(variableToPartitions, regionToPartition, macroToPartitions, this.paradigms);
            }
        }

        private RegionMapper(Multimap<String, String> variableToPartitionIn, Map<String, String> regionToPartitionIn, Multimap<String, String> macroToPartitionsIn, Set<ULocale> paradigmsIn) {
            this.variableToPartition = ImmutableMultimap.copyOf(variableToPartitionIn);
            this.regionToPartition = ImmutableMap.copyOf(regionToPartitionIn);
            this.macroToPartitions = ImmutableMultimap.copyOf(macroToPartitionsIn);
            this.paradigms = ImmutableSet.copyOf(paradigmsIn);
        }

        public String toId(String region) {
            String result = (String) this.regionToPartition.get(region);
            return result == null ? "" : result;
        }

        public Collection<String> getIdsFromVariable(String variable) {
            if (variable.equals("*")) {
                return Collections.singleton("*");
            }
            Collection<String> result = this.variableToPartition.get(variable);
            if (result != null && !result.isEmpty()) {
                return result;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Variable not defined: ");
            stringBuilder.append(variable);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public Set<String> regions() {
            return this.regionToPartition.keySet();
        }

        public Set<String> variables() {
            return this.variableToPartition.keySet();
        }

        public String toString() {
            TreeMultimap<String, String> partitionToVariables = (TreeMultimap) Multimaps.invertFrom(this.variableToPartition, TreeMultimap.create());
            TreeMultimap<String, String> partitionToRegions = TreeMultimap.create();
            for (Entry<String, String> e : this.regionToPartition.entrySet()) {
                partitionToRegions.put((String) e.getValue(), (String) e.getKey());
            }
            StringBuilder buffer = new StringBuilder();
            buffer.append("Partition ➠ Variables ➠ Regions (final)");
            for (Entry<String, Set<String>> e2 : partitionToVariables.asMap().entrySet()) {
                buffer.append(10);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append((String) e2.getKey());
                stringBuilder.append("\t");
                stringBuilder.append(e2.getValue());
                stringBuilder.append("\t");
                stringBuilder.append(partitionToRegions.get((String) e2.getKey()));
                buffer.append(stringBuilder.toString());
            }
            buffer.append("\nMacro ➠ Partitions");
            for (Entry<String, Set<String>> e22 : this.macroToPartitions.asMap().entrySet()) {
                buffer.append(10);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append((String) e22.getKey());
                stringBuilder2.append("\t");
                stringBuilder2.append(e22.getValue());
                buffer.append(stringBuilder2.toString());
            }
            return buffer.toString();
        }
    }

    static class StringDistanceNode extends DistanceNode {
        final DistanceTable distanceTable;

        public StringDistanceNode(int distance, DistanceTable distanceTable) {
            super(distance);
            this.distanceTable = distanceTable;
        }

        /* JADX WARNING: Missing block: B:9:0x0026, code skipped:
            if (super.equals(r2) != false) goto L_0x002b;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean equals(Object obj) {
            if (this != obj) {
                if (obj != null && obj.getClass() == getClass()) {
                    StringDistanceNode stringDistanceNode = (StringDistanceNode) obj;
                    StringDistanceNode other = stringDistanceNode;
                    if (this.distance == stringDistanceNode.distance) {
                        if (Utility.equals(this.distanceTable, other.distanceTable)) {
                        }
                    }
                }
                return false;
            }
            return true;
        }

        public int hashCode() {
            return this.distance ^ Utility.hashCode(this.distanceTable);
        }

        StringDistanceNode(int distance) {
            this(distance, new StringDistanceTable());
        }

        public void addSubtables(String desiredSub, String supportedSub, CopyIfEmpty r) {
            ((StringDistanceTable) this.distanceTable).addSubtables(desiredSub, supportedSub, r);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("distance: ");
            stringBuilder.append(this.distance);
            stringBuilder.append("\n");
            stringBuilder.append(this.distanceTable);
            return stringBuilder.toString();
        }

        public void copyTables(StringDistanceTable value) {
            if (value != null) {
                ((StringDistanceTable) this.distanceTable).copy(value);
            }
        }

        public DistanceTable getDistanceTable() {
            return this.distanceTable;
        }
    }

    @Deprecated
    public static class StringDistanceTable extends DistanceTable {
        final Map<String, Map<String, DistanceNode>> subtables;

        StringDistanceTable(Map<String, Map<String, DistanceNode>> tables) {
            this.subtables = tables;
        }

        StringDistanceTable() {
            this(XLocaleDistance.newMap());
        }

        public boolean isEmpty() {
            return this.subtables.isEmpty();
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && obj.getClass() == getClass() && this.subtables.equals(((StringDistanceTable) obj).subtables));
        }

        public int hashCode() {
            return this.subtables.hashCode();
        }

        public int getDistance(String desired, String supported, Output<DistanceTable> distanceTable, boolean starEquals) {
            boolean star = false;
            Map<String, DistanceNode> sub2 = (Map) this.subtables.get(desired);
            if (sub2 == null) {
                sub2 = (Map) this.subtables.get(XLocaleDistance.ANY);
                star = true;
            }
            DistanceNode value = (DistanceNode) sub2.get(supported);
            if (value == null) {
                value = (DistanceNode) sub2.get(XLocaleDistance.ANY);
                if (value == null && !star) {
                    sub2 = (Map) this.subtables.get(XLocaleDistance.ANY);
                    value = (DistanceNode) sub2.get(supported);
                    if (value == null) {
                        value = (DistanceNode) sub2.get(XLocaleDistance.ANY);
                    }
                }
                star = true;
            }
            if (distanceTable != null) {
                distanceTable.value = ((StringDistanceNode) value).distanceTable;
            }
            return (starEquals && star && desired.equals(supported)) ? 0 : value.distance;
        }

        public void copy(StringDistanceTable other) {
            for (Entry<String, Map<String, DistanceNode>> e1 : other.subtables.entrySet()) {
                for (Entry<String, DistanceNode> e2 : ((Map) e1.getValue()).entrySet()) {
                    addSubtable((String) e1.getKey(), (String) e2.getKey(), ((DistanceNode) e2.getValue()).distance);
                }
            }
        }

        DistanceNode addSubtable(String desired, String supported, int distance) {
            Map<String, DistanceNode> sub2 = (Map) this.subtables.get(desired);
            if (sub2 == null) {
                Map map = this.subtables;
                Map<String, DistanceNode> access$000 = XLocaleDistance.newMap();
                sub2 = access$000;
                map.put(desired, access$000);
            }
            DistanceNode oldNode = (DistanceNode) sub2.get(supported);
            if (oldNode != null) {
                return oldNode;
            }
            StringDistanceNode newNode = new StringDistanceNode(distance);
            sub2.put(supported, newNode);
            return newNode;
        }

        private DistanceNode getNode(String desired, String supported) {
            Map<String, DistanceNode> sub2 = (Map) this.subtables.get(desired);
            if (sub2 == null) {
                return null;
            }
            return (DistanceNode) sub2.get(supported);
        }

        public void addSubtables(String desired, String supported, Predicate<DistanceNode> action) {
            DistanceNode node = getNode(desired, supported);
            if (node == null) {
                Output<DistanceTable> node2 = new Output();
                node = addSubtable(desired, supported, getDistance(desired, supported, node2, 1));
                if (node2.value != null) {
                    ((StringDistanceNode) node).copyTables((StringDistanceTable) node2.value);
                }
            }
            action.test(node);
        }

        public void addSubtables(String desiredLang, String supportedLang, String desiredScript, String supportedScript, int percentage) {
            String str = desiredLang;
            String str2 = supportedLang;
            String str3 = desiredScript;
            String str4 = supportedScript;
            int i = percentage;
            boolean haveKeys = false;
            for (Entry<String, Map<String, DistanceNode>> e1 : this.subtables.entrySet()) {
                boolean desiredIsKey = str.equals((String) e1.getKey());
                if (desiredIsKey || str.equals(XLocaleDistance.ANY)) {
                    for (Entry<String, DistanceNode> e2 : ((Map) e1.getValue()).entrySet()) {
                        boolean haveKeys2;
                        boolean supportedIsKey = str2.equals((String) e2.getKey());
                        int i2 = (desiredIsKey && supportedIsKey) ? 1 : 0;
                        haveKeys |= i2;
                        if (supportedIsKey || str2.equals(XLocaleDistance.ANY)) {
                            haveKeys2 = haveKeys;
                            ((StringDistanceTable) ((DistanceNode) e2.getValue()).getDistanceTable()).addSubtable(str3, str4, i);
                        } else {
                            haveKeys2 = haveKeys;
                        }
                        haveKeys = haveKeys2;
                    }
                }
            }
            StringDistanceTable dt = new StringDistanceTable();
            dt.addSubtable(str3, str4, i);
            addSubtables(str, str2, new CopyIfEmpty(dt));
        }

        public void addSubtables(String desiredLang, String supportedLang, String desiredScript, String supportedScript, String desiredRegion, String supportedRegion, int percentage) {
            String str = desiredLang;
            String str2 = supportedLang;
            boolean haveKeys = false;
            for (Entry<String, Map<String, DistanceNode>> e1 : this.subtables.entrySet()) {
                boolean desiredIsKey = str.equals((String) e1.getKey());
                if (desiredIsKey || str.equals(XLocaleDistance.ANY)) {
                    for (Entry<String, DistanceNode> e2 : ((Map) e1.getValue()).entrySet()) {
                        boolean supportedIsKey = str2.equals((String) e2.getKey());
                        int i = (desiredIsKey && supportedIsKey) ? 1 : 0;
                        haveKeys |= i;
                        if (supportedIsKey || str2.equals(XLocaleDistance.ANY)) {
                            ((StringDistanceTable) ((StringDistanceNode) e2.getValue()).distanceTable).addSubtables(desiredScript, supportedScript, desiredRegion, supportedRegion, percentage);
                        }
                    }
                }
            }
            StringDistanceTable dt = new StringDistanceTable();
            dt.addSubtable(desiredRegion, supportedRegion, percentage);
            addSubtables(str, str2, new AddSub(desiredScript, supportedScript, dt));
        }

        public String toString() {
            return toString(false);
        }

        public String toString(boolean abbreviate) {
            return toString(abbreviate, "", new IdMakerFull("interner"), new StringBuilder()).toString();
        }

        public StringBuilder toString(boolean abbreviate, String indent, IdMakerFull<Object> intern, StringBuilder buffer) {
            boolean z = abbreviate;
            String str = indent;
            IdMakerFull<Object> idMakerFull = intern;
            StringBuilder stringBuilder = buffer;
            String indent2 = indent.isEmpty() ? "" : "\t";
            Integer id = z ? idMakerFull.getOldAndAdd(this.subtables) : null;
            char c = '#';
            char c2 = 10;
            if (id != null) {
                stringBuilder.append(indent2);
                stringBuilder.append('#');
                stringBuilder.append(id);
                stringBuilder.append(10);
            } else {
                String indent22;
                for (Entry<String, Map<String, DistanceNode>> e1 : this.subtables.entrySet()) {
                    Map<String, DistanceNode> subsubtable = (Map) e1.getValue();
                    stringBuilder.append(indent2);
                    stringBuilder.append((String) e1.getKey());
                    String indent3 = "\t";
                    id = z ? idMakerFull.getOldAndAdd(subsubtable) : null;
                    if (id != null) {
                        stringBuilder.append(indent3);
                        stringBuilder.append(c);
                        stringBuilder.append(id);
                        stringBuilder.append(c2);
                    } else {
                        for (Entry<String, DistanceNode> e2 : subsubtable.entrySet()) {
                            char c3;
                            DistanceNode value = (DistanceNode) e2.getValue();
                            stringBuilder.append(indent3);
                            stringBuilder.append((String) e2.getKey());
                            id = z ? idMakerFull.getOldAndAdd(value) : null;
                            if (id != null) {
                                stringBuilder.append(9);
                                stringBuilder.append(c);
                                stringBuilder.append(id);
                                stringBuilder.append(10);
                                indent22 = indent2;
                                c3 = 10;
                            } else {
                                stringBuilder.append(9);
                                stringBuilder.append(value.distance);
                                DistanceTable distanceTable = value.getDistanceTable();
                                if (distanceTable != null) {
                                    id = z ? idMakerFull.getOldAndAdd(distanceTable) : null;
                                    if (id != null) {
                                        stringBuilder.append(9);
                                        stringBuilder.append('#');
                                        stringBuilder.append(id);
                                        stringBuilder.append(10);
                                        indent22 = indent2;
                                    } else {
                                        StringDistanceTable stringDistanceTable = (StringDistanceTable) distanceTable;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append(str);
                                        indent22 = indent2;
                                        stringBuilder2.append("\t\t\t");
                                        stringDistanceTable.toString(z, stringBuilder2.toString(), idMakerFull, stringBuilder);
                                    }
                                    c3 = 10;
                                } else {
                                    indent22 = indent2;
                                    c3 = 10;
                                    stringBuilder.append(10);
                                }
                            }
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(str);
                            stringBuilder3.append(9);
                            indent3 = stringBuilder3.toString();
                            c2 = c3;
                            indent2 = indent22;
                            c = '#';
                        }
                    }
                    indent2 = str;
                    c2 = c2;
                    c = '#';
                }
                indent22 = indent2;
            }
            return stringBuilder;
        }

        public StringDistanceTable compact() {
            return new CompactAndImmutablizer().compact(this);
        }

        public Set<String> getCloser(int threshold) {
            Set<String> result = new HashSet();
            for (Entry<String, Map<String, DistanceNode>> e1 : this.subtables.entrySet()) {
                String desired = (String) e1.getKey();
                for (Entry<String, DistanceNode> e2 : ((Map) e1.getValue()).entrySet()) {
                    if (((DistanceNode) e2.getValue()).distance < threshold) {
                        result.add(desired);
                        break;
                    }
                }
            }
            return result;
        }

        public Integer getInternalDistance(String a, String b) {
            Map<String, DistanceNode> subsub = (Map) this.subtables.get(a);
            Integer num = null;
            if (subsub == null) {
                return null;
            }
            DistanceNode dnode = (DistanceNode) subsub.get(b);
            if (dnode != null) {
                num = Integer.valueOf(dnode.distance);
            }
            return num;
        }

        public DistanceNode getInternalNode(String a, String b) {
            Map<String, DistanceNode> subsub = (Map) this.subtables.get(a);
            if (subsub == null) {
                return null;
            }
            return (DistanceNode) subsub.get(b);
        }

        public Map<String, Set<String>> getInternalMatches() {
            Map<String, Set<String>> result = new LinkedHashMap();
            for (Entry<String, Map<String, DistanceNode>> entry : this.subtables.entrySet()) {
                result.put((String) entry.getKey(), new LinkedHashSet(((Map) entry.getValue()).keySet()));
            }
            return result;
        }
    }

    static class CompactAndImmutablizer extends IdMakerFull<Object> {
        CompactAndImmutablizer() {
        }

        StringDistanceTable compact(StringDistanceTable item) {
            if (toId((Object) item) != null) {
                return (StringDistanceTable) intern(item);
            }
            return new StringDistanceTable(compact(item.subtables, 0));
        }

        <K, T> Map<K, T> compact(Map<K, T> item, int level) {
            if (toId((Object) item) != null) {
                return (Map) intern(item);
            }
            Map<K, T> copy = new LinkedHashMap();
            for (Entry<K, T> entry : item.entrySet()) {
                T value = entry.getValue();
                if (value instanceof Map) {
                    copy.put(entry.getKey(), compact((Map) value, level + 1));
                } else {
                    copy.put(entry.getKey(), compact((DistanceNode) value));
                }
            }
            return ImmutableMap.copyOf(copy);
        }

        DistanceNode compact(DistanceNode item) {
            if (toId((Object) item) != null) {
                return (DistanceNode) intern(item);
            }
            DistanceTable distanceTable = item.getDistanceTable();
            if (distanceTable == null || distanceTable.isEmpty()) {
                return new DistanceNode(item.distance);
            }
            return new StringDistanceNode(item.distance, compact((StringDistanceTable) ((StringDistanceNode) item).distanceTable));
        }
    }

    static {
        String desiredRaw;
        List<String> desired;
        List<String> supported;
        String[] paradigmRegions;
        int i;
        Integer distance;
        Multimap<String, String> containerToFinalContainedBuilder = TreeMultimap.create();
        for (Entry<String, Set<String>> entry : CONTAINER_TO_CONTAINED.asMap().entrySet()) {
            String container = (String) entry.getKey();
            for (String contained : (Set) entry.getValue()) {
                if (CONTAINER_TO_CONTAINED.get(contained) == null) {
                    containerToFinalContainedBuilder.put(container, contained);
                }
            }
        }
        CONTAINER_TO_CONTAINED_FINAL = ImmutableMultimap.copyOf(containerToFinalContainedBuilder);
        String[][] variableOverrides = new String[][]{new String[]{"$enUS", "AS+GU+MH+MP+PR+UM+US+VI"}, new String[]{"$cnsar", "HK+MO"}, new String[]{"$americas", "019"}, new String[]{"$maghreb", "MA+DZ+TN+LY+MR+EH"}};
        String[] paradigmRegions2 = new String[]{"en", "en-GB", "es", "es-419", "pt-BR", "pt-PT"};
        String[][] regionRuleOverrides = new String[][]{new String[]{"ar_*_$maghreb", "ar_*_$maghreb", "96"}, new String[]{"ar_*_$!maghreb", "ar_*_$!maghreb", "96"}, new String[]{"ar_*_*", "ar_*_*", "95"}, new String[]{"en_*_$enUS", "en_*_$enUS", "96"}, new String[]{"en_*_$!enUS", "en_*_$!enUS", "96"}, new String[]{"en_*_*", "en_*_*", "95"}, new String[]{"es_*_$americas", "es_*_$americas", "96"}, new String[]{"es_*_$!americas", "es_*_$!americas", "96"}, new String[]{"es_*_*", "es_*_*", "95"}, new String[]{"pt_*_$americas", "pt_*_$americas", "96"}, new String[]{"pt_*_$!americas", "pt_*_$!americas", "96"}, new String[]{"pt_*_*", "pt_*_*", "95"}, new String[]{"zh_Hant_$cnsar", "zh_Hant_$cnsar", "96"}, new String[]{"zh_Hant_$!cnsar", "zh_Hant_$!cnsar", "96"}, new String[]{"zh_Hant_*", "zh_Hant_*", "95"}, new String[]{"*_*_*", "*_*_*", "96"}};
        Builder rmb = new Builder().addParadigms(paradigmRegions2);
        for (String[] variableRule : variableOverrides) {
            rmb.add(variableRule[0], variableRule[1]);
        }
        StringDistanceTable defaultDistanceTable = new StringDistanceTable();
        RegionMapper defaultRegionMapper = rmb.build();
        Splitter bar = Splitter.on('_');
        List<R4<List<String>, List<String>, Integer, Boolean>>[] sorted = new ArrayList[]{new ArrayList(), new ArrayList(), new ArrayList()};
        for (R4<String, String, Integer, Boolean> info : xGetLanguageMatcherData()) {
            desiredRaw = (String) info.get0();
            String supportedRaw = (String) info.get1();
            desired = bar.splitToList(desiredRaw);
            supported = bar.splitToList(supportedRaw);
            Boolean oneway = (Boolean) info.get3();
            int distance2 = desiredRaw.equals("*_*") ? 50 : ((Integer) info.get2()).intValue();
            String[][] variableOverrides2 = variableOverrides;
            variableOverrides = desired.size();
            paradigmRegions = paradigmRegions2;
            if (variableOverrides == 3) {
                distance2 = 3;
                variableOverrides = variableOverrides2;
                paradigmRegions2 = paradigmRegions;
            } else {
                int size = variableOverrides;
                sorted[variableOverrides - 1].add(Row.of(desired, supported, Integer.valueOf(distance2), oneway));
                variableOverrides = variableOverrides2;
                paradigmRegions2 = paradigmRegions;
            }
        }
        paradigmRegions = paradigmRegions2;
        for (List<R4<List<String>, List<String>, Integer, Boolean>> item1 : sorted) {
            for (R4<List<String>, List<String>, Integer, Boolean> item2 : item1) {
                List<String> desired2 = (List) item2.get0();
                List<String> supported2 = (List) item2.get1();
                distance = (Integer) item2.get2();
                Boolean oneway2 = (Boolean) item2.get3();
                add(defaultDistanceTable, desired2, supported2, distance.intValue());
                if (!(oneway2 == Boolean.TRUE || desired2.equals(supported2))) {
                    add(defaultDistanceTable, supported2, desired2, distance.intValue());
                }
                printMatchXml(desired2, supported2, distance, oneway2);
            }
        }
        int length = regionRuleOverrides.length;
        for (i = 0; i < length; i++) {
            String[] rule = regionRuleOverrides[i];
            supported = new ArrayList(bar.splitToList(rule[0]));
            desired = new ArrayList(bar.splitToList(rule[1]));
            distance = Integer.valueOf(100 - Integer.parseInt(rule[2]));
            printMatchXml(supported, desired, distance, Boolean.valueOf(false));
            Collection<String> desiredRegions = defaultRegionMapper.getIdsFromVariable((String) supported.get(2));
            StringBuilder stringBuilder;
            if (desiredRegions.isEmpty()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Bad region variable: ");
                stringBuilder.append((String) supported.get(2));
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            Collection<String> supportedRegions = defaultRegionMapper.getIdsFromVariable((String) desired.get(2));
            if (supportedRegions.isEmpty()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Bad region variable: ");
                stringBuilder.append((String) desired.get(2));
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            int i2;
            for (String desiredRaw2 : desiredRegions) {
                String[][] regionRuleOverrides2 = regionRuleOverrides;
                i2 = length;
                supported.set(2, desiredRaw2.toString());
                Iterator it = supportedRegions.iterator();
                while (it.hasNext()) {
                    String supportedRegion2 = (String) it.next();
                    Iterator it2 = it;
                    desired.set(2, supportedRegion2.toString());
                    add(defaultDistanceTable, supported, desired, distance.intValue());
                    add(defaultDistanceTable, desired, supported, distance.intValue());
                    it = it2;
                }
                regionRuleOverrides = regionRuleOverrides2;
                length = i2;
            }
            i2 = length;
        }
        DEFAULT = new XLocaleDistance(defaultDistanceTable.compact(), defaultRegionMapper);
    }

    private static String fixAny(String string) {
        return "*".equals(string) ? ANY : string;
    }

    private static List<R4<String, String, Integer, Boolean>> xGetLanguageMatcherData() {
        List<R4<String, String, Integer, Boolean>> distanceList = new ArrayList();
        UResourceBundleIterator iter = ((ICUResourceBundle) LocaleMatcher.getICUSupplementalData().findTopLevel("languageMatchingNew").get("written")).getIterator();
        while (iter.hasNext()) {
            ICUResourceBundle item = (ICUResourceBundle) iter.next();
            boolean oneway = item.getSize() > 3 && "1".equals(item.getString(3));
            distanceList.add((R4) Row.of(item.getString(0), item.getString(1), Integer.valueOf(Integer.parseInt(item.getString(2))), Boolean.valueOf(oneway)).freeze());
        }
        return Collections.unmodifiableList(distanceList);
    }

    private static Set<String> xGetParadigmLocales() {
        return Collections.unmodifiableSet(new HashSet(Arrays.asList(((ICUResourceBundle) LocaleMatcher.getICUSupplementalData().findTopLevel("languageMatchingInfo").get("written").get("paradigmLocales")).getStringArray())));
    }

    private static Map<String, String> xGetMatchVariables() {
        ICUResourceBundle writtenMatchVariables = (ICUResourceBundle) LocaleMatcher.getICUSupplementalData().findTopLevel("languageMatchingInfo").get("written").get("matchVariable");
        HashMap<String, String> matchVariables = new HashMap();
        Enumeration<String> enumer = writtenMatchVariables.getKeys();
        while (enumer.hasMoreElements()) {
            String key = (String) enumer.nextElement();
            matchVariables.put(key, writtenMatchVariables.getString(key));
        }
        return Collections.unmodifiableMap(matchVariables);
    }

    private static Multimap<String, String> xGetContainment() {
        TreeMultimap<String, String> containment = TreeMultimap.create();
        containment.putAll((Object) "001", (Object[]) new String[]{"019", "002", "150", "142", "009"}).putAll((Object) "011", "BF", "BJ", "CI", "CV", "GH", "GM", "GN", "GW", "LR", "ML", "MR", "NE", "NG", "SH", "SL", "SN", "TG").putAll((Object) "013", "BZ", "CR", "GT", "HN", "MX", "NI", "PA", "SV").putAll((Object) "014", "BI", "DJ", "ER", "ET", "KE", "KM", "MG", "MU", "MW", "MZ", "RE", "RW", "SC", "SO", "SS", "TZ", "UG", "YT", "ZM", "ZW").putAll((Object) "142", "145", "143", "030", "034", "035").putAll((Object) "143", "TM", "TJ", "KG", "KZ", "UZ").putAll((Object) "145", "AE", "AM", "AZ", "BH", "CY", "GE", "IL", "IQ", "JO", "KW", "LB", "OM", "PS", "QA", "SA", "SY", "TR", "YE", "NT", "YD").putAll((Object) "015", "DZ", "EG", "EH", "LY", "MA", "SD", "TN", "EA", "IC").putAll((Object) "150", "154", "155", "151", "039").putAll((Object) "151", "BG", "BY", "CZ", "HU", "MD", "PL", "RO", "RU", "SK", "UA", "SU").putAll((Object) "154", "GG", "IM", "JE", "AX", "DK", "EE", "FI", "FO", "GB", "IE", "IS", "LT", "LV", "NO", "SE", "SJ").putAll((Object) "155", "AT", "BE", "CH", "DE", "FR", "LI", "LU", "MC", "NL", "DD", "FX").putAll((Object) "017", "AO", "CD", "CF", "CG", "CM", "GA", "GQ", "ST", "TD", "ZR").putAll((Object) "018", "BW", "LS", "NA", "SZ", "ZA").putAll((Object) "019", "021", "013", "029", "005", "003", "419").putAll((Object) "002", "015", "011", "017", "014", "018").putAll((Object) "021", "BM", "CA", "GL", "PM", "US").putAll((Object) "029", "AG", "AI", "AW", "BB", "BL", "BQ", "BS", "CU", "CW", "DM", "DO", "GD", "GP", "HT", "JM", "KN", "KY", "LC", "MF", "MQ", "MS", "PR", "SX", "TC", "TT", "VC", "VG", "VI", "AN").putAll((Object) "003", "021", "013", "029").putAll((Object) "030", "CN", "HK", "JP", "KP", "KR", "MN", "MO", "TW").putAll((Object) "035", "BN", "ID", "KH", "LA", "MM", "MY", "PH", "SG", "TH", "TL", "VN", "BU", "TP").putAll((Object) "039", "AD", "AL", "BA", "ES", "GI", "GR", "HR", "IT", "ME", "MK", "MT", "RS", "PT", "SI", "SM", "VA", "XK", "CS", "YU").putAll((Object) "419", "013", "029", "005").putAll((Object) "005", "AR", "BO", "BR", "CL", "CO", "EC", "FK", "GF", "GY", "PE", "PY", "SR", "UY", "VE").putAll((Object) "053", "AU", "NF", "NZ").putAll((Object) "054", "FJ", "NC", "PG", "SB", "VU").putAll((Object) "057", "FM", "GU", "KI", "MH", "MP", "NR", "PW").putAll((Object) "061", "AS", "CK", "NU", "PF", "PN", "TK", "TO", "TV", "WF", "WS").putAll((Object) "034", "AF", "BD", "BT", "IN", "IR", "LK", "MV", "NP", "PK").putAll((Object) "009", "053", "054", "057", "061", "QO").putAll((Object) "QO", "AQ", "BV", "CC", "CX", "GS", "HM", "IO", "TF", "UM", "AC", "CP", "DG", "TA");
        TreeMultimap<String, String> containmentResolved = TreeMultimap.create();
        fill("001", containment, containmentResolved);
        return ImmutableMultimap.copyOf(containmentResolved);
    }

    private static Set<String> fill(String region, TreeMultimap<String, String> containment, Multimap<String, String> toAddTo) {
        Collection<String> contained = containment.get(region);
        if (contained == null) {
            return Collections.emptySet();
        }
        toAddTo.putAll((Object) region, (Collection) contained);
        for (String subregion : contained) {
            toAddTo.putAll((Object) region, fill(subregion, containment, toAddTo));
        }
        return toAddTo.get(region);
    }

    public XLocaleDistance(DistanceTable datadistancetable2, RegionMapper regionMapper) {
        this.languageDesired2Supported = datadistancetable2;
        this.regionMapper = regionMapper;
        StringDistanceNode languageNode = (StringDistanceNode) ((Map) ((StringDistanceTable) this.languageDesired2Supported).subtables.get(ANY)).get(ANY);
        this.defaultLanguageDistance = languageNode.distance;
        StringDistanceNode scriptNode = (StringDistanceNode) ((Map) ((StringDistanceTable) languageNode.distanceTable).subtables.get(ANY)).get(ANY);
        this.defaultScriptDistance = scriptNode.distance;
        this.defaultRegionDistance = ((DistanceNode) ((Map) ((StringDistanceTable) scriptNode.distanceTable).subtables.get(ANY)).get(ANY)).distance;
    }

    private static Map newMap() {
        return new TreeMap();
    }

    public int distance(ULocale desired, ULocale supported, int threshold, DistanceOption distanceOption) {
        return distanceRaw(LSR.fromMaximalized(desired), LSR.fromMaximalized(supported), threshold, distanceOption);
    }

    public int distanceRaw(LSR desired, LSR supported, int threshold, DistanceOption distanceOption) {
        return distanceRaw(desired.language, supported.language, desired.script, supported.script, desired.region, supported.region, threshold, distanceOption);
    }

    /* JADX WARNING: Removed duplicated region for block: B:53:0x0101  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00fe  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int distanceRaw(String desiredLang, String supportedlang, String desiredScript, String supportedScript, String desiredRegion, String supportedRegion, int threshold, DistanceOption distanceOption) {
        String str = desiredRegion;
        String str2 = supportedRegion;
        int i = threshold;
        Output<DistanceTable> subtable = new Output();
        int distance = this.languageDesired2Supported.getDistance(desiredLang, supportedlang, subtable, true);
        boolean scriptFirst = distanceOption == DistanceOption.SCRIPT_FIRST;
        if (scriptFirst) {
            distance >>= 2;
        }
        if (distance < 0) {
            distance = 0;
        } else if (distance >= i) {
            return 100;
        }
        int scriptDistance = ((DistanceTable) subtable.value).getDistance(desiredScript, supportedScript, subtable, true);
        if (scriptFirst) {
            scriptDistance >>= 1;
        }
        distance += scriptDistance;
        if (distance >= i) {
            return 100;
        }
        if (desiredRegion.equals(supportedRegion)) {
            return distance;
        }
        int subdistance;
        Collection<String> collection;
        Output<DistanceTable> output;
        int distance2;
        String desiredPartition = this.regionMapper.toId(str);
        String supportedPartition = this.regionMapper.toId(str2);
        Collection<String> desiredPartitions = desiredPartition.isEmpty() ? this.regionMapper.macroToPartitions.get(str) : null;
        Collection<String> supportedPartitions = supportedPartition.isEmpty() ? this.regionMapper.macroToPartitions.get(str2) : null;
        if (desiredPartitions != null) {
        } else if (supportedPartitions != null) {
            int i2 = scriptDistance;
        } else {
            subdistance = ((DistanceTable) subtable.value).getDistance(desiredPartition, supportedPartition, null, 0);
            collection = supportedPartitions;
            output = subtable;
            distance2 = distance + subdistance;
            return distance2 < i ? 100 : distance2;
        }
        int subdistance2 = 0;
        if (desiredPartitions == null) {
            desiredPartitions = Collections.singleton(desiredPartition);
        }
        if (supportedPartitions == null) {
            supportedPartitions = Collections.singleton(supportedPartition);
        }
        Iterator it = desiredPartitions.iterator();
        while (it.hasNext()) {
            Iterator it2;
            String str3;
            String desiredPartition2 = (String) it.next();
            subdistance = subdistance2;
            Iterator subdistance3 = supportedPartitions.iterator();
            collection = supportedPartitions;
            distance2 = subdistance;
            while (subdistance3.hasNext()) {
                Iterator it3 = subdistance3;
                it2 = it;
                output = subtable;
                int tempSubdistance = ((DistanceTable) subtable.value).getDistance(desiredPartition2, (String) subdistance3.next(), null, false);
                if (distance2 < tempSubdistance) {
                    distance2 = tempSubdistance;
                }
                subdistance3 = it3;
                it = it2;
                subtable = output;
                str3 = desiredLang;
            }
            it2 = it;
            subdistance2 = distance2;
            supportedPartitions = collection;
            subtable = subtable;
            str3 = desiredLang;
        }
        subdistance = subdistance2;
        collection = supportedPartitions;
        output = subtable;
        distance2 = distance + subdistance;
        if (distance2 < i) {
        }
        return distance2 < i ? 100 : distance2;
    }

    public static XLocaleDistance getDefault() {
        return DEFAULT;
    }

    private static void printMatchXml(List<String> list, List<String> list2, Integer distance, Boolean oneway) {
    }

    private static String fixedName(List<String> match) {
        String region;
        List<String> alt = new ArrayList(match);
        int size = alt.size();
        StringBuilder result = new StringBuilder();
        if (size >= 3) {
            region = (String) alt.get(2);
            if (region.equals("*") || region.startsWith("$")) {
                result.append(region);
            } else {
                result.append(english.regionDisplayName(region));
            }
        }
        if (size >= 2) {
            String script = (String) alt.get(1);
            if (script.equals("*")) {
                result.insert(0, script);
            } else {
                result.insert(0, english.scriptDisplayName(script));
            }
        }
        if (size >= 1) {
            region = (String) alt.get(0);
            if (region.equals("*")) {
                result.insert(0, region);
            } else {
                result.insert(0, english.languageDisplayName(region));
            }
        }
        return CollectionUtilities.join(alt, "; ");
    }

    public static void add(StringDistanceTable languageDesired2Supported, List<String> desired, List<String> supported, int percentage) {
        List<String> list = desired;
        List<String> list2 = supported;
        int size = desired.size();
        StringDistanceTable stringDistanceTable;
        int i;
        if (size != supported.size() || size < 1 || size > 3) {
            stringDistanceTable = languageDesired2Supported;
            i = percentage;
            throw new IllegalArgumentException();
        }
        String desiredLang = fixAny((String) list.get(0));
        String supportedLang = fixAny((String) list2.get(0));
        if (size == 1) {
            languageDesired2Supported.addSubtable(desiredLang, supportedLang, percentage);
            return;
        }
        stringDistanceTable = languageDesired2Supported;
        i = percentage;
        String desiredScript = fixAny((String) list.get(1));
        String supportedScript = fixAny((String) list2.get(1));
        if (size == 2) {
            stringDistanceTable.addSubtables(desiredLang, supportedLang, desiredScript, supportedScript, i);
            return;
        }
        stringDistanceTable.addSubtables(desiredLang, supportedLang, desiredScript, supportedScript, fixAny((String) list.get(2)), fixAny((String) list2.get(2)), i);
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean abbreviate) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.regionMapper);
        stringBuilder.append("\n");
        stringBuilder.append(this.languageDesired2Supported.toString(abbreviate));
        return stringBuilder.toString();
    }

    static Set<String> getContainingMacrosFor(Collection<String> input, Set<String> output) {
        output.clear();
        for (Entry<String, Set<String>> entry : CONTAINER_TO_CONTAINED.asMap().entrySet()) {
            if (input.containsAll((Collection) entry.getValue())) {
                output.add((String) entry.getKey());
            }
        }
        return output;
    }

    public static <K, V> Multimap<K, V> invertMap(Map<V, K> map) {
        return Multimaps.invertFrom(Multimaps.forMap(map), LinkedHashMultimap.create());
    }

    public Set<ULocale> getParadigms() {
        return this.regionMapper.paradigms;
    }

    public int getDefaultLanguageDistance() {
        return this.defaultLanguageDistance;
    }

    public int getDefaultScriptDistance() {
        return this.defaultScriptDistance;
    }

    public int getDefaultRegionDistance() {
        return this.defaultRegionDistance;
    }

    @Deprecated
    public StringDistanceTable internalGetDistanceTable() {
        return (StringDistanceTable) this.languageDesired2Supported;
    }

    public static void main(String[] args) {
        DistanceTable table = getDefault().languageDesired2Supported;
        if (!table.equals(table.compact())) {
            throw new IllegalArgumentException("Compaction isn't equal");
        }
    }
}
