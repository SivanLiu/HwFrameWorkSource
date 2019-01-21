package sun.net.www;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

public class MessageHeader {
    private String[] keys;
    private int nkeys;
    private String[] values;

    class HeaderIterator implements Iterator<String> {
        boolean haveNext = false;
        int index = 0;
        String key;
        Object lock;
        int next = -1;

        public HeaderIterator(String k, Object lock) {
            this.key = k;
            this.lock = lock;
        }

        public boolean hasNext() {
            synchronized (this.lock) {
                if (this.haveNext) {
                    return true;
                }
                while (this.index < MessageHeader.this.nkeys) {
                    if (this.key.equalsIgnoreCase(MessageHeader.this.keys[this.index])) {
                        this.haveNext = true;
                        int i = this.index;
                        this.index = i + 1;
                        this.next = i;
                        return true;
                    }
                    this.index++;
                }
                return false;
            }
        }

        public String next() {
            synchronized (this.lock) {
                String str;
                if (this.haveNext) {
                    this.haveNext = false;
                    str = MessageHeader.this.values[this.next];
                    return str;
                } else if (hasNext()) {
                    str = next();
                    return str;
                } else {
                    throw new NoSuchElementException("No more elements");
                }
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not allowed");
        }
    }

    public MessageHeader() {
        grow();
    }

    public MessageHeader(InputStream is) throws IOException {
        parseHeader(is);
    }

    public synchronized String getHeaderNamesInList() {
        StringJoiner joiner;
        joiner = new StringJoiner(",");
        for (int i = 0; i < this.nkeys; i++) {
            joiner.add(this.keys[i]);
        }
        return joiner.toString();
    }

    public synchronized void reset() {
        this.keys = null;
        this.values = null;
        this.nkeys = 0;
        grow();
    }

    public synchronized String findValue(String k) {
        int i;
        if (k == null) {
            i = this.nkeys;
            do {
                i--;
                if (i >= 0) {
                }
            } while (this.keys[i] != null);
            return this.values[i];
        }
        i = this.nkeys;
        do {
            i--;
            if (i >= 0) {
            }
        } while (!k.equalsIgnoreCase(this.keys[i]));
        return this.values[i];
        return null;
    }

    /* JADX WARNING: Missing block: B:11:0x001a, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized int getKey(String k) {
        int i = this.nkeys;
        while (true) {
            i--;
            if (i < 0) {
                return -1;
            }
            if (this.keys[i] == k || (k != null && k.equalsIgnoreCase(this.keys[i]))) {
            }
        }
    }

    public synchronized String getKey(int n) {
        if (n >= 0) {
            if (n < this.nkeys) {
                return this.keys[n];
            }
        }
        return null;
    }

    public synchronized String getValue(int n) {
        if (n >= 0) {
            if (n < this.nkeys) {
                return this.values[n];
            }
        }
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:37:0x0042 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:7:0x000a A:{Catch:{ all -> 0x0020 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized String findNextValue(String k, String v) {
        int i;
        boolean foundV = false;
        if (k == null) {
            try {
                i = this.nkeys;
            } finally {
            }
        } else {
            i = this.nkeys;
            while (true) {
                i--;
                if (i < 0) {
                    break;
                } else if (k.equalsIgnoreCase(this.keys[i])) {
                    if (foundV) {
                        return this.values[i];
                    } else if (this.values[i] == v) {
                        foundV = true;
                    }
                }
            }
            return null;
        }
        i--;
        if (i >= 0) {
            if (this.keys[i] == null) {
                if (!foundV) {
                    if (this.values[i] == v) {
                        foundV = true;
                    }
                }
                return this.values[i];
            }
            i--;
            if (i >= 0) {
            }
        }
        return null;
    }

    public boolean filterNTLMResponses(String k) {
        boolean found = false;
        int i = 0;
        while (i < this.nkeys) {
            if (k.equalsIgnoreCase(this.keys[i]) && this.values[i] != null && this.values[i].length() > 5 && this.values[i].substring(0, 5).equalsIgnoreCase("NTLM ")) {
                found = true;
                break;
            }
            i++;
        }
        if (found) {
            int j = 0;
            i = 0;
            while (i < this.nkeys) {
                if (!(k.equalsIgnoreCase(this.keys[i]) && ("Negotiate".equalsIgnoreCase(this.values[i]) || "Kerberos".equalsIgnoreCase(this.values[i])))) {
                    if (i != j) {
                        this.keys[j] = this.keys[i];
                        this.values[j] = this.values[i];
                    }
                    j++;
                }
                i++;
            }
            if (j != this.nkeys) {
                this.nkeys = j;
                return true;
            }
        }
        return false;
    }

    public Iterator<String> multiValueIterator(String k) {
        return new HeaderIterator(k, this);
    }

    public synchronized Map<String, List<String>> getHeaders() {
        return getHeaders(null);
    }

    public synchronized Map<String, List<String>> getHeaders(String[] excludeList) {
        return filterAndAddHeaders(excludeList, null);
    }

    public synchronized Map<String, List<String>> filterAndAddHeaders(String[] excludeList, Map<String, List<String>> include) {
        Map<String, List<String>> m;
        boolean skipIt = false;
        m = new HashMap();
        int i = this.nkeys;
        while (true) {
            i--;
            if (i < 0) {
                break;
            }
            if (excludeList != null) {
                int j = 0;
                while (j < excludeList.length) {
                    if (excludeList[j] != null && excludeList[j].equalsIgnoreCase(this.keys[i])) {
                        skipIt = true;
                        break;
                    }
                    j++;
                }
            }
            if (skipIt) {
                skipIt = false;
            } else {
                List<String> l = (List) m.get(this.keys[i]);
                if (l == null) {
                    l = new ArrayList();
                    m.put(this.keys[i], l);
                }
                l.add(this.values[i]);
            }
        }
        if (include != null) {
            for (Entry<String, List<String>> entry : include.entrySet()) {
                List<String> l2 = (List) m.get(entry.getKey());
                if (l2 == null) {
                    l2 = new ArrayList();
                    m.put((String) entry.getKey(), l2);
                }
                l2.addAll((Collection) entry.getValue());
            }
        }
        for (String key : m.keySet()) {
            m.put(key, Collections.unmodifiableList((List) m.get(key)));
        }
        return Collections.unmodifiableMap(m);
    }

    public synchronized void print(PrintStream p) {
        for (int i = 0; i < this.nkeys; i++) {
            if (this.keys[i] != null) {
                String stringBuilder;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.keys[i]);
                if (this.values[i] != null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(": ");
                    stringBuilder3.append(this.values[i]);
                    stringBuilder = stringBuilder3.toString();
                } else {
                    stringBuilder = "";
                }
                stringBuilder2.append(stringBuilder);
                stringBuilder2.append("\r\n");
                p.print(stringBuilder2.toString());
            }
        }
        p.print("\r\n");
        p.flush();
    }

    public synchronized void add(String k, String v) {
        grow();
        this.keys[this.nkeys] = k;
        this.values[this.nkeys] = v;
        this.nkeys++;
    }

    public synchronized void prepend(String k, String v) {
        grow();
        for (int i = this.nkeys; i > 0; i--) {
            this.keys[i] = this.keys[i - 1];
            this.values[i] = this.values[i - 1];
        }
        this.keys[0] = k;
        this.values[0] = v;
        this.nkeys++;
    }

    /* JADX WARNING: Missing block: B:12:0x0019, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void set(int i, String k, String v) {
        grow();
        if (i >= 0) {
            if (i >= this.nkeys) {
                add(k, v);
            } else {
                this.keys[i] = k;
                this.values[i] = v;
            }
        }
    }

    private void grow() {
        if (this.keys == null || this.nkeys >= this.keys.length) {
            Object nk = new String[(this.nkeys + 4)];
            Object nv = new String[(this.nkeys + 4)];
            if (this.keys != null) {
                System.arraycopy(this.keys, 0, nk, 0, this.nkeys);
            }
            if (this.values != null) {
                System.arraycopy(this.values, 0, nv, 0, this.nkeys);
            }
            this.keys = nk;
            this.values = nv;
        }
    }

    public synchronized void remove(String k) {
        int i = 0;
        int j;
        if (k == null) {
            while (i < this.nkeys) {
                try {
                    while (this.keys[i] == null && i < this.nkeys) {
                        for (j = i; j < this.nkeys - 1; j++) {
                            this.keys[j] = this.keys[j + 1];
                            this.values[j] = this.values[j + 1];
                        }
                        this.nkeys--;
                    }
                    i++;
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        while (i < this.nkeys) {
            while (k.equalsIgnoreCase(this.keys[i]) && i < this.nkeys) {
                for (j = i; j < this.nkeys - 1; j++) {
                    this.keys[j] = this.keys[j + 1];
                    this.values[j] = this.values[j + 1];
                }
                this.nkeys--;
            }
            i++;
        }
    }

    public synchronized void set(String k, String v) {
        int i = this.nkeys;
        do {
            i--;
            if (i < 0) {
                add(k, v);
                return;
            }
        } while (!k.equalsIgnoreCase(this.keys[i]));
        this.values[i] = v;
    }

    public synchronized void setIfNotSet(String k, String v) {
        if (findValue(k) == null) {
            add(k, v);
        }
    }

    public static String canonicalID(String id) {
        if (id == null) {
            return "";
        }
        char charAt;
        char c;
        int st = 0;
        int len = id.length();
        boolean substr = false;
        while (st < len) {
            charAt = id.charAt(st);
            c = charAt;
            if (charAt != '<' && c > ' ') {
                break;
            }
            st++;
            substr = true;
        }
        while (st < len) {
            charAt = id.charAt(len - 1);
            c = charAt;
            if (charAt != '>' && c > ' ') {
                break;
            }
            len--;
            substr = true;
        }
        return substr ? id.substring(st, len) : id;
    }

    public void parseHeader(InputStream is) throws IOException {
        synchronized (this) {
            this.nkeys = 0;
        }
        mergeHeader(is);
    }

    /* JADX WARNING: Removed duplicated region for block: B:71:0x006b A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x0062  */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x0062  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x006b A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void mergeHeader(InputStream is) throws IOException {
        if (is != null) {
            char[] s = new char[10];
            int firstc = is.read();
            while (firstc != 10 && firstc != 13 && firstc >= 0) {
                int read;
                String k;
                String v;
                int keyend = -1;
                boolean inKey = firstc > 32;
                int len = 0 + 1;
                s[0] = (char) firstc;
                while (true) {
                    read = is.read();
                    int c = read;
                    if (read >= 0) {
                        if (c != 13) {
                            boolean inKey2;
                            if (c != 32) {
                                if (c != 58) {
                                    switch (c) {
                                        case 9:
                                            c = 32;
                                            break;
                                        case 10:
                                            break;
                                    }
                                }
                                if (inKey && len > 0) {
                                    keyend = len;
                                }
                                inKey2 = false;
                                inKey = inKey2;
                                if (len >= s.length) {
                                    Object ns = new char[(s.length * 2)];
                                    System.arraycopy((Object) s, 0, ns, 0, len);
                                    s = ns;
                                }
                                read = len + 1;
                                s[len] = (char) c;
                                len = read;
                            }
                            inKey2 = false;
                            inKey = inKey2;
                            if (len >= s.length) {
                            }
                            read = len + 1;
                            s[len] = (char) c;
                            len = read;
                        }
                        firstc = is.read();
                        if (c == 13 && firstc == 10) {
                            firstc = is.read();
                            if (firstc == 13) {
                                firstc = is.read();
                            }
                        }
                        if (!(firstc == 10 || firstc == 13 || firstc > 32)) {
                            c = 32;
                            if (len >= s.length) {
                            }
                            read = len + 1;
                            s[len] = (char) c;
                            len = read;
                        }
                    } else {
                        firstc = -1;
                    }
                }
                while (len > 0 && s[len - 1] <= ' ') {
                    len--;
                }
                if (keyend <= 0) {
                    k = null;
                    read = 0;
                } else {
                    k = String.copyValueOf(s, 0, keyend);
                    if (keyend < len && s[keyend] == ':') {
                        keyend++;
                    }
                    read = keyend;
                    while (read < len && s[read] <= ' ') {
                        read++;
                    }
                }
                if (read >= len) {
                    v = new String();
                } else {
                    v = String.copyValueOf(s, read, len - read);
                }
                add(k, v);
            }
        }
    }

    public synchronized String toString() {
        String result;
        result = new StringBuilder();
        result.append(super.toString());
        result.append(this.nkeys);
        result.append(" pairs: ");
        result = result.toString();
        int i = 0;
        while (i < this.keys.length && i < this.nkeys) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(result);
            stringBuilder.append("{");
            stringBuilder.append(this.keys[i]);
            stringBuilder.append(": ");
            stringBuilder.append(this.values[i]);
            stringBuilder.append("}");
            result = stringBuilder.toString();
            i++;
        }
        return result;
    }
}
