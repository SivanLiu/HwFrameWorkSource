package android.icu.impl.locale;

import android.icu.util.ICUException;
import android.icu.util.ICUUncheckedIOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XCldrStub {

    public static class CollectionUtilities {
        public static <T, U extends Iterable<T>> String join(U source, String separator) {
            return XCldrStub.join((Iterable) source, separator);
        }
    }

    public static class FileUtilities {
        public static final Charset UTF8 = Charset.forName("utf-8");

        public static BufferedReader openFile(Class<?> class1, String file) {
            return openFile(class1, file, UTF8);
        }

        public static BufferedReader openFile(Class<?> class1, String file, Charset charset) {
            try {
                InputStream resourceAsStream = class1.getResourceAsStream(file);
                if (charset == null) {
                    charset = UTF8;
                }
                return new BufferedReader(new InputStreamReader(resourceAsStream, charset), 65536);
            } catch (Exception e) {
                String className = class1 == null ? null : class1.getCanonicalName();
                try {
                    String canonicalName = new File(getRelativeFileName(class1, "../util/")).getCanonicalPath();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Couldn't open file ");
                    stringBuilder.append(file);
                    stringBuilder.append("; in path ");
                    stringBuilder.append(canonicalName);
                    stringBuilder.append("; relative to class: ");
                    stringBuilder.append(className);
                    throw new ICUUncheckedIOException(stringBuilder.toString(), e);
                } catch (Exception e2) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Couldn't open file: ");
                    stringBuilder2.append(file);
                    stringBuilder2.append("; relative to class: ");
                    stringBuilder2.append(className);
                    throw new ICUUncheckedIOException(stringBuilder2.toString(), e);
                }
            }
        }

        public static String getRelativeFileName(Class<?> class1, String filename) {
            String resourceString = (class1 == null ? FileUtilities.class.getResource(filename) : class1.getResource(filename)).toString();
            if (resourceString.startsWith("file:")) {
                return resourceString.substring(5);
            }
            if (resourceString.startsWith("jar:file:")) {
                return resourceString.substring(9);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("File not found: ");
            stringBuilder.append(resourceString);
            throw new ICUUncheckedIOException(stringBuilder.toString());
        }
    }

    public static class ImmutableMap {
        public static <K, V> Map<K, V> copyOf(Map<K, V> values) {
            return Collections.unmodifiableMap(new LinkedHashMap(values));
        }
    }

    public static class ImmutableMultimap {
        public static <K, V> Multimap<K, V> copyOf(Multimap<K, V> values) {
            LinkedHashMap<K, Set<V>> temp = new LinkedHashMap();
            for (Entry<K, Set<V>> entry : values.asMap().entrySet()) {
                Object singleton;
                Set<V> value = (Set) entry.getValue();
                Object key = entry.getKey();
                if (value.size() == 1) {
                    singleton = Collections.singleton(value.iterator().next());
                } else {
                    singleton = Collections.unmodifiableSet(new LinkedHashSet(value));
                }
                temp.put(key, singleton);
            }
            return new Multimap(Collections.unmodifiableMap(temp), null);
        }
    }

    public static class ImmutableSet {
        public static <T> Set<T> copyOf(Set<T> values) {
            return Collections.unmodifiableSet(new LinkedHashSet(values));
        }
    }

    public static class Joiner {
        private final String separator;

        private Joiner(String separator) {
            this.separator = separator;
        }

        public static final Joiner on(String separator) {
            return new Joiner(separator);
        }

        public <T> String join(T[] source) {
            return XCldrStub.join((Object[]) source, this.separator);
        }

        public <T> String join(Iterable<T> source) {
            return XCldrStub.join((Iterable) source, this.separator);
        }
    }

    public static class Multimap<K, V> {
        private final Map<K, Set<V>> map;
        private final Class<Set<V>> setClass;

        private Multimap(Map<K, Set<V>> map, Class<?> setClass) {
            this.map = map;
            this.setClass = setClass != null ? setClass : HashSet.class;
        }

        public Multimap<K, V> putAll(K key, V... values) {
            if (values.length != 0) {
                createSetIfMissing(key).addAll(Arrays.asList(values));
            }
            return this;
        }

        public void putAll(K key, Collection<V> values) {
            if (!values.isEmpty()) {
                createSetIfMissing(key).addAll(values);
            }
        }

        public void putAll(Collection<K> keys, V value) {
            for (K key : keys) {
                put(key, value);
            }
        }

        public void putAll(Multimap<K, V> source) {
            for (Entry<K, Set<V>> entry : source.map.entrySet()) {
                putAll(entry.getKey(), (Collection) entry.getValue());
            }
        }

        public void put(K key, V value) {
            createSetIfMissing(key).add(value);
        }

        private Set<V> createSetIfMissing(K key) {
            Set<V> old = (Set) this.map.get(key);
            if (old != null) {
                return old;
            }
            Map map = this.map;
            Set<V> instance = getInstance();
            old = instance;
            map.put(key, instance);
            return old;
        }

        private Set<V> getInstance() {
            try {
                return (Set) this.setClass.newInstance();
            } catch (Exception e) {
                throw new ICUException(e);
            }
        }

        public Set<V> get(K key) {
            return (Set) this.map.get(key);
        }

        public Set<K> keySet() {
            return this.map.keySet();
        }

        public Map<K, Set<V>> asMap() {
            return this.map;
        }

        public Set<V> values() {
            Collection<Set<V>> values = this.map.values();
            if (values.size() == 0) {
                return Collections.emptySet();
            }
            Set<V> result = getInstance();
            for (Set<V> valueSet : values) {
                result.addAll(valueSet);
            }
            return result;
        }

        public int size() {
            return this.map.size();
        }

        public Iterable<Entry<K, V>> entries() {
            return new MultimapIterator(this.map);
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && obj.getClass() == getClass() && this.map.equals(((Multimap) obj).map));
        }

        public int hashCode() {
            return this.map.hashCode();
        }
    }

    private static class MultimapIterator<K, V> implements Iterator<Entry<K, V>>, Iterable<Entry<K, V>> {
        private final ReusableEntry<K, V> entry;
        private final Iterator<Entry<K, Set<V>>> it1;
        private Iterator<V> it2;

        private MultimapIterator(Map<K, Set<V>> map) {
            this.it2 = null;
            this.entry = new ReusableEntry();
            this.it1 = map.entrySet().iterator();
        }

        public boolean hasNext() {
            return this.it1.hasNext() || (this.it2 != null && this.it2.hasNext());
        }

        public Entry<K, V> next() {
            if (this.it2 == null || !this.it2.hasNext()) {
                Entry<K, Set<V>> e = (Entry) this.it1.next();
                this.entry.key = e.getKey();
                this.it2 = ((Set) e.getValue()).iterator();
            } else {
                this.entry.value = this.it2.next();
            }
            return this.entry;
        }

        public Iterator<Entry<K, V>> iterator() {
            return this;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class Multimaps {
        public static <K, V, R extends Multimap<K, V>> R invertFrom(Multimap<V, K> source, R target) {
            for (Entry<V, Set<K>> entry : source.asMap().entrySet()) {
                target.putAll((Collection) entry.getValue(), entry.getKey());
            }
            return target;
        }

        public static <K, V, R extends Multimap<K, V>> R invertFrom(Map<V, K> source, R target) {
            for (Entry<V, K> entry : source.entrySet()) {
                target.put(entry.getValue(), entry.getKey());
            }
            return target;
        }

        public static <K, V> Map<K, V> forMap(Map<K, V> map) {
            return map;
        }
    }

    public interface Predicate<T> {
        boolean test(T t);
    }

    public static class RegexUtilities {
        public static int findMismatch(Matcher m, CharSequence s) {
            int i = 1;
            while (i < s.length() && (m.reset(s.subSequence(0, i)).matches() || m.hitEnd())) {
                i++;
            }
            return i - 1;
        }

        public static String showMismatch(Matcher m, CharSequence s) {
            int failPoint = findMismatch(m, s);
            String show = new StringBuilder();
            show.append(s.subSequence(0, failPoint));
            show.append("☹");
            show.append(s.subSequence(failPoint, s.length()));
            return show.toString();
        }
    }

    private static class ReusableEntry<K, V> implements Entry<K, V> {
        K key;
        V value;

        private ReusableEntry() {
        }

        public K getKey() {
            return this.key;
        }

        public V getValue() {
            return this.value;
        }

        public V setValue(V v) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Splitter {
        Pattern pattern;
        boolean trimResults;

        public Splitter(char c) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\\Q");
            stringBuilder.append(c);
            stringBuilder.append("\\E");
            this(Pattern.compile(stringBuilder.toString()));
        }

        public Splitter(Pattern p) {
            this.trimResults = false;
            this.pattern = p;
        }

        public static Splitter on(char c) {
            return new Splitter(c);
        }

        public static Splitter on(Pattern p) {
            return new Splitter(p);
        }

        public List<String> splitToList(String input) {
            String[] items = this.pattern.split(input);
            if (this.trimResults) {
                for (int i = 0; i < items.length; i++) {
                    items[i] = items[i].trim();
                }
            }
            return Arrays.asList(items);
        }

        public Splitter trimResults() {
            this.trimResults = true;
            return this;
        }

        public Iterable<String> split(String input) {
            return splitToList(input);
        }
    }

    public static class HashMultimap<K, V> extends Multimap<K, V> {
        private HashMultimap() {
            super(new HashMap(), HashSet.class);
        }

        public static <K, V> HashMultimap<K, V> create() {
            return new HashMultimap();
        }
    }

    public static class LinkedHashMultimap<K, V> extends Multimap<K, V> {
        private LinkedHashMultimap() {
            super(new LinkedHashMap(), LinkedHashSet.class);
        }

        public static <K, V> LinkedHashMultimap<K, V> create() {
            return new LinkedHashMultimap();
        }
    }

    public static class TreeMultimap<K, V> extends Multimap<K, V> {
        private TreeMultimap() {
            super(new TreeMap(), TreeSet.class);
        }

        public static <K, V> TreeMultimap<K, V> create() {
            return new TreeMultimap();
        }
    }

    public static <T> String join(T[] source, String separator) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < source.length; i++) {
            if (i != 0) {
                result.append(separator);
            }
            result.append(source[i]);
        }
        return result.toString();
    }

    public static <T> String join(Iterable<T> source, String separator) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (T item : source) {
            if (first) {
                first = false;
            } else {
                result.append(separator);
            }
            result.append(item.toString());
        }
        return result.toString();
    }
}
