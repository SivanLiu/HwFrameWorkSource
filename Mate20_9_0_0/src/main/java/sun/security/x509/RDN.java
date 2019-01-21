package sun.security.x509;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.security.auth.x500.X500Principal;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class RDN {
    final AVA[] assertion;
    private volatile List<AVA> avaList;
    private volatile String canonicalString;

    public RDN(String name) throws IOException {
        this(name, Collections.emptyMap());
    }

    public RDN(String name, Map<String, String> keywordMap) throws IOException {
        StringBuilder stringBuilder;
        int quoteCount = 0;
        int searchOffset = 0;
        int avaOffset = 0;
        List<AVA> avaVec = new ArrayList(3);
        int nextPlus = name.indexOf(43);
        while (nextPlus >= 0) {
            quoteCount += X500Name.countQuotes(name, searchOffset, nextPlus);
            if (!(nextPlus <= 0 || name.charAt(nextPlus - 1) == '\\' || quoteCount == 1)) {
                String avaString = name.substring(avaOffset, nextPlus);
                if (avaString.length() != 0) {
                    avaVec.add(new AVA(new StringReader(avaString), (Map) keywordMap));
                    avaOffset = nextPlus + 1;
                    quoteCount = 0;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("empty AVA in RDN \"");
                    stringBuilder.append(name);
                    stringBuilder.append("\"");
                    throw new IOException(stringBuilder.toString());
                }
            }
            searchOffset = nextPlus + 1;
            nextPlus = name.indexOf(43, searchOffset);
        }
        String avaString2 = name.substring(avaOffset);
        if (avaString2.length() != 0) {
            avaVec.add(new AVA(new StringReader(avaString2), (Map) keywordMap));
            this.assertion = (AVA[]) avaVec.toArray(new AVA[avaVec.size()]);
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("empty AVA in RDN \"");
        stringBuilder.append(name);
        stringBuilder.append("\"");
        throw new IOException(stringBuilder.toString());
    }

    RDN(String name, String format) throws IOException {
        this(name, format, Collections.emptyMap());
    }

    RDN(String name, String format, Map<String, String> keywordMap) throws IOException {
        if (format.equalsIgnoreCase(X500Principal.RFC2253)) {
            int avaOffset = 0;
            List<AVA> avaVec = new ArrayList(3);
            int nextPlus = name.indexOf(43);
            while (nextPlus >= 0) {
                if (nextPlus > 0 && name.charAt(nextPlus - 1) != '\\') {
                    String avaString = name.substring(avaOffset, nextPlus);
                    if (avaString.length() != 0) {
                        avaVec.add(new AVA(new StringReader(avaString), 3, keywordMap));
                        avaOffset = nextPlus + 1;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("empty AVA in RDN \"");
                        stringBuilder.append(name);
                        stringBuilder.append("\"");
                        throw new IOException(stringBuilder.toString());
                    }
                }
                nextPlus = name.indexOf(43, nextPlus + 1);
            }
            String avaString2 = name.substring(avaOffset);
            if (avaString2.length() != 0) {
                avaVec.add(new AVA(new StringReader(avaString2), 3, keywordMap));
                this.assertion = (AVA[]) avaVec.toArray(new AVA[avaVec.size()]);
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("empty AVA in RDN \"");
            stringBuilder2.append(name);
            stringBuilder2.append("\"");
            throw new IOException(stringBuilder2.toString());
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Unsupported format ");
        stringBuilder3.append(format);
        throw new IOException(stringBuilder3.toString());
    }

    RDN(DerValue rdn) throws IOException {
        if (rdn.tag == (byte) 49) {
            DerValue[] avaset = new DerInputStream(rdn.toByteArray()).getSet(5);
            this.assertion = new AVA[avaset.length];
            for (int i = 0; i < avaset.length; i++) {
                this.assertion[i] = new AVA(avaset[i]);
            }
            return;
        }
        throw new IOException("X500 RDN");
    }

    RDN(int i) {
        this.assertion = new AVA[i];
    }

    public RDN(AVA ava) {
        if (ava != null) {
            this.assertion = new AVA[]{ava};
            return;
        }
        throw new NullPointerException();
    }

    public RDN(AVA[] avas) {
        this.assertion = (AVA[]) avas.clone();
        int i = 0;
        while (i < this.assertion.length) {
            if (this.assertion[i] != null) {
                i++;
            } else {
                throw new NullPointerException();
            }
        }
    }

    public List<AVA> avas() {
        List<AVA> list = this.avaList;
        if (list != null) {
            return list;
        }
        list = Collections.unmodifiableList(Arrays.asList(this.assertion));
        this.avaList = list;
        return list;
    }

    public int size() {
        return this.assertion.length;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RDN)) {
            return false;
        }
        RDN other = (RDN) obj;
        if (this.assertion.length != other.assertion.length) {
            return false;
        }
        return toRFC2253String(true).equals(other.toRFC2253String(true));
    }

    public int hashCode() {
        return toRFC2253String(true).hashCode();
    }

    DerValue findAttribute(ObjectIdentifier oid) {
        for (int i = 0; i < this.assertion.length; i++) {
            if (this.assertion[i].oid.equals((Object) oid)) {
                return this.assertion[i].value;
            }
        }
        return null;
    }

    void encode(DerOutputStream out) throws IOException {
        out.putOrderedSetOf((byte) 49, this.assertion);
    }

    public String toString() {
        int i = 0;
        if (this.assertion.length == 1) {
            return this.assertion[0].toString();
        }
        StringBuilder sb = new StringBuilder();
        while (i < this.assertion.length) {
            if (i != 0) {
                sb.append(" + ");
            }
            sb.append(this.assertion[i].toString());
            i++;
        }
        return sb.toString();
    }

    public String toRFC1779String() {
        return toRFC1779String(Collections.emptyMap());
    }

    public String toRFC1779String(Map<String, String> oidMap) {
        int i = 0;
        if (this.assertion.length == 1) {
            return this.assertion[0].toRFC1779String(oidMap);
        }
        StringBuilder sb = new StringBuilder();
        while (i < this.assertion.length) {
            if (i != 0) {
                sb.append(" + ");
            }
            sb.append(this.assertion[i].toRFC1779String(oidMap));
            i++;
        }
        return sb.toString();
    }

    public String toRFC2253String() {
        return toRFC2253StringInternal(false, Collections.emptyMap());
    }

    public String toRFC2253String(Map<String, String> oidMap) {
        return toRFC2253StringInternal(false, oidMap);
    }

    public String toRFC2253String(boolean canonical) {
        if (!canonical) {
            return toRFC2253StringInternal(false, Collections.emptyMap());
        }
        String c = this.canonicalString;
        if (c == null) {
            c = toRFC2253StringInternal(true, Collections.emptyMap());
            this.canonicalString = c;
        }
        return c;
    }

    private String toRFC2253StringInternal(boolean canonical, Map<String, String> oidMap) {
        int i = 0;
        if (this.assertion.length == 1) {
            String toRFC2253CanonicalString;
            if (canonical) {
                toRFC2253CanonicalString = this.assertion[0].toRFC2253CanonicalString();
            } else {
                toRFC2253CanonicalString = this.assertion[0].toRFC2253String(oidMap);
            }
            return toRFC2253CanonicalString;
        }
        AVA[] toOutput = this.assertion;
        if (canonical) {
            toOutput = (AVA[]) this.assertion.clone();
            Arrays.sort(toOutput, AVAComparator.getInstance());
        }
        StringJoiner sj = new StringJoiner("+");
        int length = toOutput.length;
        while (i < length) {
            CharSequence toRFC2253CanonicalString2;
            AVA ava = toOutput[i];
            if (canonical) {
                toRFC2253CanonicalString2 = ava.toRFC2253CanonicalString();
            } else {
                toRFC2253CanonicalString2 = ava.toRFC2253String(oidMap);
            }
            sj.add(toRFC2253CanonicalString2);
            i++;
        }
        return sj.toString();
    }
}
