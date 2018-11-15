package org.bouncycastle.asn1.x509;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Integers;
import org.bouncycastle.util.Strings;

public class PKIXNameConstraintValidator implements NameConstraintValidator {
    private Set excludedSubtreesDN = new HashSet();
    private Set excludedSubtreesDNS = new HashSet();
    private Set excludedSubtreesEmail = new HashSet();
    private Set excludedSubtreesIP = new HashSet();
    private Set excludedSubtreesURI = new HashSet();
    private Set permittedSubtreesDN;
    private Set permittedSubtreesDNS;
    private Set permittedSubtreesEmail;
    private Set permittedSubtreesIP;
    private Set permittedSubtreesURI;

    private void checkExcludedDN(Set set, ASN1Sequence aSN1Sequence) throws NameConstraintValidatorException {
        if (!set.isEmpty()) {
            for (ASN1Sequence withinDNSubtree : set) {
                if (withinDNSubtree(aSN1Sequence, withinDNSubtree)) {
                    throw new NameConstraintValidatorException("Subject distinguished name is from an excluded subtree");
                }
            }
        }
    }

    private void checkExcludedDN(X500Name x500Name) throws NameConstraintValidatorException {
        checkExcludedDN(this.excludedSubtreesDN, ASN1Sequence.getInstance(x500Name));
    }

    private void checkExcludedDNS(Set set, String str) throws NameConstraintValidatorException {
        if (!set.isEmpty()) {
            for (String str2 : set) {
                if (withinDomain(str, str2) || str.equalsIgnoreCase(str2)) {
                    throw new NameConstraintValidatorException("DNS is from an excluded subtree.");
                }
            }
        }
    }

    private void checkExcludedEmail(Set set, String str) throws NameConstraintValidatorException {
        if (!set.isEmpty()) {
            for (String emailIsConstrained : set) {
                if (emailIsConstrained(str, emailIsConstrained)) {
                    throw new NameConstraintValidatorException("Email address is from an excluded subtree.");
                }
            }
        }
    }

    private void checkExcludedIP(Set set, byte[] bArr) throws NameConstraintValidatorException {
        if (!set.isEmpty()) {
            for (byte[] isIPConstrained : set) {
                if (isIPConstrained(bArr, isIPConstrained)) {
                    throw new NameConstraintValidatorException("IP is from an excluded subtree.");
                }
            }
        }
    }

    private void checkExcludedURI(Set set, String str) throws NameConstraintValidatorException {
        if (!set.isEmpty()) {
            for (String isUriConstrained : set) {
                if (isUriConstrained(str, isUriConstrained)) {
                    throw new NameConstraintValidatorException("URI is from an excluded subtree.");
                }
            }
        }
    }

    private void checkPermittedDN(Set set, ASN1Sequence aSN1Sequence) throws NameConstraintValidatorException {
        if (set != null) {
            if (!set.isEmpty() || aSN1Sequence.size() != 0) {
                for (ASN1Sequence withinDNSubtree : set) {
                    if (withinDNSubtree(aSN1Sequence, withinDNSubtree)) {
                        return;
                    }
                }
                throw new NameConstraintValidatorException("Subject distinguished name is not from a permitted subtree");
            }
        }
    }

    private void checkPermittedDN(X500Name x500Name) throws NameConstraintValidatorException {
        checkPermittedDN(this.permittedSubtreesDN, ASN1Sequence.getInstance(x500Name.toASN1Primitive()));
    }

    private void checkPermittedDNS(Set set, String str) throws NameConstraintValidatorException {
        if (set != null) {
            for (String str2 : set) {
                if (!withinDomain(str, str2)) {
                    if (str.equalsIgnoreCase(str2)) {
                    }
                }
                return;
            }
            if (str.length() != 0 || set.size() != 0) {
                throw new NameConstraintValidatorException("DNS is not from a permitted subtree.");
            }
        }
    }

    private void checkPermittedEmail(Set set, String str) throws NameConstraintValidatorException {
        if (set != null) {
            for (String emailIsConstrained : set) {
                if (emailIsConstrained(str, emailIsConstrained)) {
                    return;
                }
            }
            if (str.length() != 0 || set.size() != 0) {
                throw new NameConstraintValidatorException("Subject email address is not from a permitted subtree.");
            }
        }
    }

    private void checkPermittedIP(Set set, byte[] bArr) throws NameConstraintValidatorException {
        if (set != null) {
            for (byte[] isIPConstrained : set) {
                if (isIPConstrained(bArr, isIPConstrained)) {
                    return;
                }
            }
            if (bArr.length != 0 || set.size() != 0) {
                throw new NameConstraintValidatorException("IP is not from a permitted subtree.");
            }
        }
    }

    private void checkPermittedURI(Set set, String str) throws NameConstraintValidatorException {
        if (set != null) {
            for (String isUriConstrained : set) {
                if (isUriConstrained(str, isUriConstrained)) {
                    return;
                }
            }
            if (str.length() != 0 || set.size() != 0) {
                throw new NameConstraintValidatorException("URI is not from a permitted subtree.");
            }
        }
    }

    private boolean collectionsAreEqual(Collection collection, Collection collection2) {
        if (collection == collection2) {
            return true;
        }
        if (collection == null || collection2 == null || collection.size() != collection2.size()) {
            return false;
        }
        for (Object next : collection) {
            boolean z;
            for (Object equals : collection2) {
                if (equals(next, equals)) {
                    z = true;
                    continue;
                    break;
                }
            }
            z = false;
            continue;
            if (!z) {
                return false;
            }
        }
        return true;
    }

    private static int compareTo(byte[] bArr, byte[] bArr2) {
        return Arrays.areEqual(bArr, bArr2) ? 0 : Arrays.areEqual(max(bArr, bArr2), bArr) ? 1 : -1;
    }

    private boolean emailIsConstrained(String str, String str2) {
        String substring = str.substring(str.indexOf(64) + 1);
        if (str2.indexOf(64) != -1) {
            if (str.equalsIgnoreCase(str2)) {
                return true;
            }
        } else if (str2.charAt(0) != '.') {
            if (substring.equalsIgnoreCase(str2)) {
                return true;
            }
        } else if (withinDomain(substring, str2)) {
            return true;
        }
        return false;
    }

    private boolean equals(Object obj, Object obj2) {
        return obj == obj2 ? true : (obj == null || obj2 == null) ? false : ((obj instanceof byte[]) && (obj2 instanceof byte[])) ? Arrays.areEqual((byte[]) obj, (byte[]) obj2) : obj.equals(obj2);
    }

    private static String extractHostFromURL(String str) {
        str = str.substring(str.indexOf(58) + 1);
        if (str.indexOf("//") != -1) {
            str = str.substring(str.indexOf("//") + 2);
        }
        if (str.lastIndexOf(58) != -1) {
            str = str.substring(0, str.lastIndexOf(58));
        }
        str = str.substring(str.indexOf(58) + 1);
        str = str.substring(str.indexOf(64) + 1);
        return str.indexOf(47) != -1 ? str.substring(0, str.indexOf(47)) : str;
    }

    private byte[][] extractIPsAndSubnetMasks(byte[] bArr, byte[] bArr2) {
        int length = bArr.length / 2;
        Object obj = new byte[length];
        System.arraycopy(bArr, 0, new byte[length], 0, length);
        System.arraycopy(bArr, length, obj, 0, length);
        Object obj2 = new byte[length];
        System.arraycopy(bArr2, 0, new byte[length], 0, length);
        System.arraycopy(bArr2, length, obj2, 0, length);
        return new byte[][]{r2, obj, r7, obj2};
    }

    private String extractNameAsString(GeneralName generalName) {
        return DERIA5String.getInstance(generalName.getName()).getString();
    }

    private int hashCollection(Collection collection) {
        int i = 0;
        if (collection == null) {
            return 0;
        }
        for (Object next : collection) {
            i += next instanceof byte[] ? Arrays.hashCode((byte[]) next) : next.hashCode();
        }
        return i;
    }

    private Set intersectDN(Set set, Set set2) {
        Set hashSet = new HashSet();
        for (GeneralSubtree base : set2) {
            ASN1Sequence instance = ASN1Sequence.getInstance(base.getBase().getName().toASN1Primitive());
            if (set != null) {
                for (ASN1Sequence aSN1Sequence : set) {
                    if (withinDNSubtree(instance, aSN1Sequence)) {
                        hashSet.add(instance);
                    } else if (withinDNSubtree(aSN1Sequence, instance)) {
                        hashSet.add(aSN1Sequence);
                    }
                }
            } else if (instance != null) {
                hashSet.add(instance);
            }
        }
        return hashSet;
    }

    private Set intersectDNS(Set set, Set set2) {
        Set hashSet = new HashSet();
        for (GeneralSubtree base : set2) {
            String extractNameAsString = extractNameAsString(base.getBase());
            if (set != null) {
                for (String str : set) {
                    if (withinDomain(str, extractNameAsString)) {
                        hashSet.add(str);
                    } else if (withinDomain(extractNameAsString, str)) {
                        hashSet.add(extractNameAsString);
                    }
                }
            } else if (extractNameAsString != null) {
                hashSet.add(extractNameAsString);
            }
        }
        return hashSet;
    }

    private Set intersectEmail(Set set, Set set2) {
        Set hashSet = new HashSet();
        for (GeneralSubtree base : set2) {
            String extractNameAsString = extractNameAsString(base.getBase());
            if (set != null) {
                for (String intersectEmail : set) {
                    intersectEmail(extractNameAsString, intersectEmail, hashSet);
                }
            } else if (extractNameAsString != null) {
                hashSet.add(extractNameAsString);
            }
        }
        return hashSet;
    }

    /* JADX WARNING: Missing block: B:17:0x0052, code:
            if (withinDomain(r5.substring(r4.indexOf(64) + 1), r4) != false) goto L_0x007b;
     */
    /* JADX WARNING: Missing block: B:25:0x006e, code:
            if (withinDomain(r5, r4) != false) goto L_0x007b;
     */
    /* JADX WARNING: Missing block: B:29:0x0079, code:
            if (withinDomain(r5, r4) != false) goto L_0x007b;
     */
    /* JADX WARNING: Missing block: B:35:0x0093, code:
            if (r5.substring(r5.indexOf(64) + 1).equalsIgnoreCase(r4) != false) goto L_0x007b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void intersectEmail(String str, String str2, Set set) {
        if (str.indexOf(64) != -1) {
            String substring = str.substring(str.indexOf(64) + 1);
            if (str2.indexOf(64) != -1) {
                return;
            }
            return;
        }
        if (!str.startsWith(".")) {
            if (str2.indexOf(64) == -1) {
                if (str2.startsWith(".")) {
                }
            }
            return;
        } else if (str2.indexOf(64) == -1) {
            if (str2.startsWith(".")) {
                if (!(withinDomain(str, str2) || str.equalsIgnoreCase(str2))) {
                }
            }
        }
        set.add(str2);
        return;
        set.add(str);
    }

    private Set intersectIP(Set set, Set set2) {
        Set hashSet = new HashSet();
        for (GeneralSubtree base : set2) {
            Object octets = ASN1OctetString.getInstance(base.getBase().getName()).getOctets();
            if (set != null) {
                for (byte[] intersectIPRange : set) {
                    hashSet.addAll(intersectIPRange(intersectIPRange, octets));
                }
            } else if (octets != null) {
                hashSet.add(octets);
            }
        }
        return hashSet;
    }

    private Set intersectIPRange(byte[] bArr, byte[] bArr2) {
        if (bArr.length != bArr2.length) {
            return Collections.EMPTY_SET;
        }
        byte[][] extractIPsAndSubnetMasks = extractIPsAndSubnetMasks(bArr, bArr2);
        byte[] bArr3 = extractIPsAndSubnetMasks[0];
        byte[] bArr4 = extractIPsAndSubnetMasks[1];
        byte[] bArr5 = extractIPsAndSubnetMasks[2];
        bArr = extractIPsAndSubnetMasks[3];
        byte[][] minMaxIPs = minMaxIPs(bArr3, bArr4, bArr5, bArr);
        return compareTo(max(minMaxIPs[0], minMaxIPs[2]), min(minMaxIPs[1], minMaxIPs[3])) == 1 ? Collections.EMPTY_SET : Collections.singleton(ipWithSubnetMask(or(minMaxIPs[0], minMaxIPs[2]), or(bArr4, bArr)));
    }

    private Set intersectURI(Set set, Set set2) {
        Set hashSet = new HashSet();
        for (GeneralSubtree base : set2) {
            String extractNameAsString = extractNameAsString(base.getBase());
            if (set != null) {
                for (String intersectURI : set) {
                    intersectURI(intersectURI, extractNameAsString, hashSet);
                }
            } else if (extractNameAsString != null) {
                hashSet.add(extractNameAsString);
            }
        }
        return hashSet;
    }

    /* JADX WARNING: Missing block: B:17:0x0052, code:
            if (withinDomain(r5.substring(r4.indexOf(64) + 1), r4) != false) goto L_0x007b;
     */
    /* JADX WARNING: Missing block: B:25:0x006e, code:
            if (withinDomain(r5, r4) != false) goto L_0x007b;
     */
    /* JADX WARNING: Missing block: B:29:0x0079, code:
            if (withinDomain(r5, r4) != false) goto L_0x007b;
     */
    /* JADX WARNING: Missing block: B:35:0x0093, code:
            if (r5.substring(r5.indexOf(64) + 1).equalsIgnoreCase(r4) != false) goto L_0x007b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void intersectURI(String str, String str2, Set set) {
        if (str.indexOf(64) != -1) {
            String substring = str.substring(str.indexOf(64) + 1);
            if (str2.indexOf(64) != -1) {
                return;
            }
            return;
        }
        if (!str.startsWith(".")) {
            if (str2.indexOf(64) == -1) {
                if (str2.startsWith(".")) {
                }
            }
            return;
        } else if (str2.indexOf(64) == -1) {
            if (str2.startsWith(".")) {
                if (!(withinDomain(str, str2) || str.equalsIgnoreCase(str2))) {
                }
            }
        }
        set.add(str2);
        return;
        set.add(str);
    }

    private byte[] ipWithSubnetMask(byte[] bArr, byte[] bArr2) {
        int length = bArr.length;
        Object obj = new byte[(length * 2)];
        System.arraycopy(bArr, 0, obj, 0, length);
        System.arraycopy(bArr2, 0, obj, length, length);
        return obj;
    }

    private boolean isIPConstrained(byte[] bArr, byte[] bArr2) {
        int length = bArr.length;
        int i = 0;
        if (length != bArr2.length / 2) {
            return false;
        }
        Object obj = new byte[length];
        System.arraycopy(bArr2, length, obj, 0, length);
        byte[] bArr3 = new byte[length];
        byte[] bArr4 = new byte[length];
        while (i < length) {
            bArr3[i] = (byte) (bArr2[i] & obj[i]);
            bArr4[i] = (byte) (bArr[i] & obj[i]);
            i++;
        }
        return Arrays.areEqual(bArr3, bArr4);
    }

    private boolean isUriConstrained(String str, String str2) {
        str = extractHostFromURL(str);
        if (str2.startsWith(".")) {
            if (withinDomain(str, str2)) {
                return true;
            }
        } else if (str.equalsIgnoreCase(str2)) {
            return true;
        }
        return false;
    }

    private static byte[] max(byte[] bArr, byte[] bArr2) {
        for (int i = 0; i < bArr.length; i++) {
            if ((bArr[i] & 65535) > (65535 & bArr2[i])) {
                return bArr;
            }
        }
        return bArr2;
    }

    private static byte[] min(byte[] bArr, byte[] bArr2) {
        for (int i = 0; i < bArr.length; i++) {
            if ((bArr[i] & 65535) < (65535 & bArr2[i])) {
                return bArr;
            }
        }
        return bArr2;
    }

    private byte[][] minMaxIPs(byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4) {
        int length = bArr.length;
        byte[] bArr5 = new byte[length];
        byte[] bArr6 = new byte[length];
        byte[] bArr7 = new byte[length];
        byte[] bArr8 = new byte[length];
        for (int i = 0; i < length; i++) {
            bArr5[i] = (byte) (bArr[i] & bArr2[i]);
            bArr6[i] = (byte) ((bArr[i] & bArr2[i]) | (~bArr2[i]));
            bArr7[i] = (byte) (bArr3[i] & bArr4[i]);
            bArr8[i] = (byte) ((bArr3[i] & bArr4[i]) | (~bArr4[i]));
        }
        return new byte[][]{bArr5, bArr6, bArr7, bArr8};
    }

    private static byte[] or(byte[] bArr, byte[] bArr2) {
        byte[] bArr3 = new byte[bArr.length];
        for (int i = 0; i < bArr.length; i++) {
            bArr3[i] = (byte) (bArr[i] | bArr2[i]);
        }
        return bArr3;
    }

    private String stringifyIP(byte[] bArr) {
        StringBuilder stringBuilder;
        String str = "";
        for (int i = 0; i < bArr.length / 2; i++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(Integer.toString(bArr[i] & 255));
            stringBuilder.append(".");
            str = stringBuilder.toString();
        }
        String substring = str.substring(0, str.length() - 1);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(substring);
        stringBuilder2.append("/");
        substring = stringBuilder2.toString();
        for (int length = bArr.length / 2; length < bArr.length; length++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(substring);
            stringBuilder.append(Integer.toString(bArr[length] & 255));
            stringBuilder.append(".");
            substring = stringBuilder.toString();
        }
        return substring.substring(0, substring.length() - 1);
    }

    private String stringifyIPCollection(Set set) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append("[");
        String stringBuilder2 = stringBuilder.toString();
        for (byte[] stringifyIP : set) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append(stringifyIP(stringifyIP));
            stringBuilder.append(",");
            stringBuilder2 = stringBuilder.toString();
        }
        if (stringBuilder2.length() > 1) {
            stringBuilder2 = stringBuilder2.substring(0, stringBuilder2.length() - 1);
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(stringBuilder2);
        stringBuilder3.append("]");
        return stringBuilder3.toString();
    }

    private Set unionDN(Set set, ASN1Sequence aSN1Sequence) {
        if (!set.isEmpty()) {
            Set hashSet = new HashSet();
            for (ASN1Sequence aSN1Sequence2 : set) {
                if (withinDNSubtree(aSN1Sequence, aSN1Sequence2)) {
                    hashSet.add(aSN1Sequence2);
                } else {
                    if (!withinDNSubtree(aSN1Sequence2, aSN1Sequence)) {
                        hashSet.add(aSN1Sequence2);
                    }
                    hashSet.add(aSN1Sequence);
                }
            }
            return hashSet;
        } else if (aSN1Sequence == null) {
            return set;
        } else {
            set.add(aSN1Sequence);
            return set;
        }
    }

    private Set unionDNS(Set set, String str) {
        if (!set.isEmpty()) {
            Set hashSet = new HashSet();
            for (String str2 : set) {
                if (!withinDomain(str2, str)) {
                    if (withinDomain(str, str2)) {
                        hashSet.add(str2);
                    } else {
                        hashSet.add(str2);
                    }
                }
                hashSet.add(str);
            }
            return hashSet;
        } else if (str == null) {
            return set;
        } else {
            set.add(str);
            return set;
        }
    }

    private Set unionEmail(Set set, String str) {
        if (!set.isEmpty()) {
            Set hashSet = new HashSet();
            for (String unionEmail : set) {
                unionEmail(unionEmail, str, hashSet);
            }
            return hashSet;
        } else if (str == null) {
            return set;
        } else {
            set.add(str);
            return set;
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:52:0x00b0 in {6, 11, 14, 21, 28, 31, 33, 35, 37, 38, 43, 48, 51} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private void unionEmail(java.lang.String r4, java.lang.String r5, java.util.Set r6) {
        /*
        r3 = this;
        r0 = 64;
        r1 = r4.indexOf(r0);
        r2 = -1;
        if (r1 == r2) goto L_0x0036;
    L_0x0009:
        r1 = r4.indexOf(r0);
        r1 = r1 + 1;
        r1 = r4.substring(r1);
        r0 = r5.indexOf(r0);
        if (r0 == r2) goto L_0x0020;
    L_0x0019:
        r0 = r4.equalsIgnoreCase(r5);
        if (r0 == 0) goto L_0x007f;
    L_0x001f:
        goto L_0x007b;
    L_0x0020:
        r0 = ".";
        r0 = r5.startsWith(r0);
        if (r0 == 0) goto L_0x002f;
    L_0x0028:
        r0 = r3.withinDomain(r1, r5);
        if (r0 == 0) goto L_0x007f;
    L_0x002e:
        goto L_0x0071;
    L_0x002f:
        r0 = r1.equalsIgnoreCase(r5);
        if (r0 == 0) goto L_0x007f;
    L_0x0035:
        goto L_0x0071;
    L_0x0036:
        r1 = ".";
        r1 = r4.startsWith(r1);
        if (r1 == 0) goto L_0x0083;
    L_0x003e:
        r1 = r5.indexOf(r0);
        if (r1 == r2) goto L_0x0055;
    L_0x0044:
        r0 = r4.indexOf(r0);
        r0 = r0 + 1;
        r0 = r5.substring(r0);
        r0 = r3.withinDomain(r0, r4);
        if (r0 == 0) goto L_0x007f;
    L_0x0054:
        goto L_0x007b;
    L_0x0055:
        r0 = ".";
        r0 = r5.startsWith(r0);
        if (r0 == 0) goto L_0x0075;
    L_0x005d:
        r0 = r3.withinDomain(r4, r5);
        if (r0 != 0) goto L_0x0071;
    L_0x0063:
        r0 = r4.equalsIgnoreCase(r5);
        if (r0 == 0) goto L_0x006a;
    L_0x0069:
        goto L_0x0071;
    L_0x006a:
        r0 = r3.withinDomain(r5, r4);
        if (r0 == 0) goto L_0x007f;
    L_0x0070:
        goto L_0x007b;
    L_0x0071:
        r6.add(r5);
        return;
    L_0x0075:
        r0 = r3.withinDomain(r5, r4);
        if (r0 == 0) goto L_0x007f;
    L_0x007b:
        r6.add(r4);
        return;
    L_0x007f:
        r6.add(r4);
        goto L_0x0071;
    L_0x0083:
        r1 = r5.indexOf(r0);
        if (r1 == r2) goto L_0x009a;
    L_0x0089:
        r0 = r4.indexOf(r0);
        r0 = r0 + 1;
        r0 = r5.substring(r0);
        r0 = r0.equalsIgnoreCase(r4);
        if (r0 == 0) goto L_0x007f;
    L_0x0099:
        goto L_0x007b;
    L_0x009a:
        r0 = ".";
        r0 = r5.startsWith(r0);
        if (r0 == 0) goto L_0x00a9;
    L_0x00a2:
        r0 = r3.withinDomain(r4, r5);
        if (r0 == 0) goto L_0x007f;
    L_0x00a8:
        goto L_0x0071;
    L_0x00a9:
        r0 = r4.equalsIgnoreCase(r5);
        if (r0 == 0) goto L_0x007f;
    L_0x00af:
        goto L_0x007b;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.x509.PKIXNameConstraintValidator.unionEmail(java.lang.String, java.lang.String, java.util.Set):void");
    }

    private Set unionIP(Set set, byte[] bArr) {
        if (!set.isEmpty()) {
            Set hashSet = new HashSet();
            for (byte[] unionIPRange : set) {
                hashSet.addAll(unionIPRange(unionIPRange, bArr));
            }
            return hashSet;
        } else if (bArr == null) {
            return set;
        } else {
            set.add(bArr);
            return set;
        }
    }

    private Set unionIPRange(byte[] bArr, byte[] bArr2) {
        Set hashSet = new HashSet();
        if (Arrays.areEqual(bArr, bArr2)) {
            hashSet.add(bArr);
            return hashSet;
        }
        hashSet.add(bArr);
        hashSet.add(bArr2);
        return hashSet;
    }

    private Set unionURI(Set set, String str) {
        if (!set.isEmpty()) {
            Set hashSet = new HashSet();
            for (String unionURI : set) {
                unionURI(unionURI, str, hashSet);
            }
            return hashSet;
        } else if (str == null) {
            return set;
        } else {
            set.add(str);
            return set;
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:52:0x00b0 in {6, 11, 14, 21, 28, 31, 33, 35, 37, 38, 43, 48, 51} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private void unionURI(java.lang.String r4, java.lang.String r5, java.util.Set r6) {
        /*
        r3 = this;
        r0 = 64;
        r1 = r4.indexOf(r0);
        r2 = -1;
        if (r1 == r2) goto L_0x0036;
    L_0x0009:
        r1 = r4.indexOf(r0);
        r1 = r1 + 1;
        r1 = r4.substring(r1);
        r0 = r5.indexOf(r0);
        if (r0 == r2) goto L_0x0020;
    L_0x0019:
        r0 = r4.equalsIgnoreCase(r5);
        if (r0 == 0) goto L_0x007f;
    L_0x001f:
        goto L_0x007b;
    L_0x0020:
        r0 = ".";
        r0 = r5.startsWith(r0);
        if (r0 == 0) goto L_0x002f;
    L_0x0028:
        r0 = r3.withinDomain(r1, r5);
        if (r0 == 0) goto L_0x007f;
    L_0x002e:
        goto L_0x0071;
    L_0x002f:
        r0 = r1.equalsIgnoreCase(r5);
        if (r0 == 0) goto L_0x007f;
    L_0x0035:
        goto L_0x0071;
    L_0x0036:
        r1 = ".";
        r1 = r4.startsWith(r1);
        if (r1 == 0) goto L_0x0083;
    L_0x003e:
        r1 = r5.indexOf(r0);
        if (r1 == r2) goto L_0x0055;
    L_0x0044:
        r0 = r4.indexOf(r0);
        r0 = r0 + 1;
        r0 = r5.substring(r0);
        r0 = r3.withinDomain(r0, r4);
        if (r0 == 0) goto L_0x007f;
    L_0x0054:
        goto L_0x007b;
    L_0x0055:
        r0 = ".";
        r0 = r5.startsWith(r0);
        if (r0 == 0) goto L_0x0075;
    L_0x005d:
        r0 = r3.withinDomain(r4, r5);
        if (r0 != 0) goto L_0x0071;
    L_0x0063:
        r0 = r4.equalsIgnoreCase(r5);
        if (r0 == 0) goto L_0x006a;
    L_0x0069:
        goto L_0x0071;
    L_0x006a:
        r0 = r3.withinDomain(r5, r4);
        if (r0 == 0) goto L_0x007f;
    L_0x0070:
        goto L_0x007b;
    L_0x0071:
        r6.add(r5);
        return;
    L_0x0075:
        r0 = r3.withinDomain(r5, r4);
        if (r0 == 0) goto L_0x007f;
    L_0x007b:
        r6.add(r4);
        return;
    L_0x007f:
        r6.add(r4);
        goto L_0x0071;
    L_0x0083:
        r1 = r5.indexOf(r0);
        if (r1 == r2) goto L_0x009a;
    L_0x0089:
        r0 = r4.indexOf(r0);
        r0 = r0 + 1;
        r0 = r5.substring(r0);
        r0 = r0.equalsIgnoreCase(r4);
        if (r0 == 0) goto L_0x007f;
    L_0x0099:
        goto L_0x007b;
    L_0x009a:
        r0 = ".";
        r0 = r5.startsWith(r0);
        if (r0 == 0) goto L_0x00a9;
    L_0x00a2:
        r0 = r3.withinDomain(r4, r5);
        if (r0 == 0) goto L_0x007f;
    L_0x00a8:
        goto L_0x0071;
    L_0x00a9:
        r0 = r4.equalsIgnoreCase(r5);
        if (r0 == 0) goto L_0x007f;
    L_0x00af:
        goto L_0x007b;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.x509.PKIXNameConstraintValidator.unionURI(java.lang.String, java.lang.String, java.util.Set):void");
    }

    private static boolean withinDNSubtree(ASN1Sequence aSN1Sequence, ASN1Sequence aSN1Sequence2) {
        if (aSN1Sequence2.size() < 1 || aSN1Sequence2.size() > aSN1Sequence.size()) {
            return false;
        }
        for (int size = aSN1Sequence2.size() - 1; size >= 0; size--) {
            if (!aSN1Sequence2.getObjectAt(size).equals(aSN1Sequence.getObjectAt(size))) {
                return false;
            }
        }
        return true;
    }

    private boolean withinDomain(String str, String str2) {
        if (str2.startsWith(".")) {
            str2 = str2.substring(1);
        }
        String[] split = Strings.split(str2, '.');
        String[] split2 = Strings.split(str, '.');
        if (split2.length <= split.length) {
            return false;
        }
        int length = split2.length - split.length;
        for (int i = -1; i < split.length; i++) {
            if (i == -1) {
                if (split2[i + length].equals("")) {
                    return false;
                }
            } else if (!split[i].equalsIgnoreCase(split2[i + length])) {
                return false;
            }
        }
        return true;
    }

    public void addExcludedSubtree(GeneralSubtree generalSubtree) {
        GeneralName base = generalSubtree.getBase();
        switch (base.getTagNo()) {
            case 1:
                this.excludedSubtreesEmail = unionEmail(this.excludedSubtreesEmail, extractNameAsString(base));
                return;
            case 2:
                this.excludedSubtreesDNS = unionDNS(this.excludedSubtreesDNS, extractNameAsString(base));
                return;
            case 4:
                this.excludedSubtreesDN = unionDN(this.excludedSubtreesDN, (ASN1Sequence) base.getName().toASN1Primitive());
                return;
            case 6:
                this.excludedSubtreesURI = unionURI(this.excludedSubtreesURI, extractNameAsString(base));
                return;
            case 7:
                this.excludedSubtreesIP = unionIP(this.excludedSubtreesIP, ASN1OctetString.getInstance(base.getName()).getOctets());
                return;
            default:
                return;
        }
    }

    public void checkExcluded(GeneralName generalName) throws NameConstraintValidatorException {
        switch (generalName.getTagNo()) {
            case 1:
                checkExcludedEmail(this.excludedSubtreesEmail, extractNameAsString(generalName));
                return;
            case 2:
                checkExcludedDNS(this.excludedSubtreesDNS, DERIA5String.getInstance(generalName.getName()).getString());
                return;
            case 4:
                checkExcludedDN(X500Name.getInstance(generalName.getName()));
                return;
            case 6:
                checkExcludedURI(this.excludedSubtreesURI, DERIA5String.getInstance(generalName.getName()).getString());
                return;
            case 7:
                checkExcludedIP(this.excludedSubtreesIP, ASN1OctetString.getInstance(generalName.getName()).getOctets());
                return;
            default:
                return;
        }
    }

    public void checkPermitted(GeneralName generalName) throws NameConstraintValidatorException {
        switch (generalName.getTagNo()) {
            case 1:
                checkPermittedEmail(this.permittedSubtreesEmail, extractNameAsString(generalName));
                return;
            case 2:
                checkPermittedDNS(this.permittedSubtreesDNS, DERIA5String.getInstance(generalName.getName()).getString());
                return;
            case 4:
                checkPermittedDN(X500Name.getInstance(generalName.getName()));
                return;
            case 6:
                checkPermittedURI(this.permittedSubtreesURI, DERIA5String.getInstance(generalName.getName()).getString());
                return;
            case 7:
                checkPermittedIP(this.permittedSubtreesIP, ASN1OctetString.getInstance(generalName.getName()).getOctets());
                return;
            default:
                return;
        }
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof PKIXNameConstraintValidator)) {
            return false;
        }
        PKIXNameConstraintValidator pKIXNameConstraintValidator = (PKIXNameConstraintValidator) obj;
        if (collectionsAreEqual(pKIXNameConstraintValidator.excludedSubtreesDN, this.excludedSubtreesDN) && collectionsAreEqual(pKIXNameConstraintValidator.excludedSubtreesDNS, this.excludedSubtreesDNS) && collectionsAreEqual(pKIXNameConstraintValidator.excludedSubtreesEmail, this.excludedSubtreesEmail) && collectionsAreEqual(pKIXNameConstraintValidator.excludedSubtreesIP, this.excludedSubtreesIP) && collectionsAreEqual(pKIXNameConstraintValidator.excludedSubtreesURI, this.excludedSubtreesURI) && collectionsAreEqual(pKIXNameConstraintValidator.permittedSubtreesDN, this.permittedSubtreesDN) && collectionsAreEqual(pKIXNameConstraintValidator.permittedSubtreesDNS, this.permittedSubtreesDNS) && collectionsAreEqual(pKIXNameConstraintValidator.permittedSubtreesEmail, this.permittedSubtreesEmail) && collectionsAreEqual(pKIXNameConstraintValidator.permittedSubtreesIP, this.permittedSubtreesIP) && collectionsAreEqual(pKIXNameConstraintValidator.permittedSubtreesURI, this.permittedSubtreesURI)) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return ((((((((hashCollection(this.excludedSubtreesDN) + hashCollection(this.excludedSubtreesDNS)) + hashCollection(this.excludedSubtreesEmail)) + hashCollection(this.excludedSubtreesIP)) + hashCollection(this.excludedSubtreesURI)) + hashCollection(this.permittedSubtreesDN)) + hashCollection(this.permittedSubtreesDNS)) + hashCollection(this.permittedSubtreesEmail)) + hashCollection(this.permittedSubtreesIP)) + hashCollection(this.permittedSubtreesURI);
    }

    public void intersectEmptyPermittedSubtree(int i) {
        switch (i) {
            case 1:
                this.permittedSubtreesEmail = new HashSet();
                return;
            case 2:
                this.permittedSubtreesDNS = new HashSet();
                return;
            case 4:
                this.permittedSubtreesDN = new HashSet();
                return;
            case 6:
                this.permittedSubtreesURI = new HashSet();
                return;
            case 7:
                this.permittedSubtreesIP = new HashSet();
                return;
            default:
                return;
        }
    }

    public void intersectPermittedSubtree(GeneralSubtree generalSubtree) {
        intersectPermittedSubtree(new GeneralSubtree[]{generalSubtree});
    }

    public void intersectPermittedSubtree(GeneralSubtree[] generalSubtreeArr) {
        Map hashMap = new HashMap();
        for (int i = 0; i != generalSubtreeArr.length; i++) {
            GeneralSubtree generalSubtree = generalSubtreeArr[i];
            Integer valueOf = Integers.valueOf(generalSubtree.getBase().getTagNo());
            if (hashMap.get(valueOf) == null) {
                hashMap.put(valueOf, new HashSet());
            }
            ((Set) hashMap.get(valueOf)).add(generalSubtree);
        }
        for (Entry entry : hashMap.entrySet()) {
            switch (((Integer) entry.getKey()).intValue()) {
                case 1:
                    this.permittedSubtreesEmail = intersectEmail(this.permittedSubtreesEmail, (Set) entry.getValue());
                    break;
                case 2:
                    this.permittedSubtreesDNS = intersectDNS(this.permittedSubtreesDNS, (Set) entry.getValue());
                    break;
                case 4:
                    this.permittedSubtreesDN = intersectDN(this.permittedSubtreesDN, (Set) entry.getValue());
                    break;
                case 6:
                    this.permittedSubtreesURI = intersectURI(this.permittedSubtreesURI, (Set) entry.getValue());
                    break;
                case 7:
                    this.permittedSubtreesIP = intersectIP(this.permittedSubtreesIP, (Set) entry.getValue());
                    break;
                default:
                    break;
            }
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append("permitted:\n");
        String stringBuilder2 = stringBuilder.toString();
        if (this.permittedSubtreesDN != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append("DN:\n");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append(this.permittedSubtreesDN.toString());
            stringBuilder.append("\n");
            stringBuilder2 = stringBuilder.toString();
        }
        if (this.permittedSubtreesDNS != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append("DNS:\n");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append(this.permittedSubtreesDNS.toString());
            stringBuilder.append("\n");
            stringBuilder2 = stringBuilder.toString();
        }
        if (this.permittedSubtreesEmail != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append("Email:\n");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append(this.permittedSubtreesEmail.toString());
            stringBuilder.append("\n");
            stringBuilder2 = stringBuilder.toString();
        }
        if (this.permittedSubtreesURI != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append("URI:\n");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append(this.permittedSubtreesURI.toString());
            stringBuilder.append("\n");
            stringBuilder2 = stringBuilder.toString();
        }
        if (this.permittedSubtreesIP != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append("IP:\n");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append(stringifyIPCollection(this.permittedSubtreesIP));
            stringBuilder.append("\n");
            stringBuilder2 = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(stringBuilder2);
        stringBuilder.append("excluded:\n");
        stringBuilder2 = stringBuilder.toString();
        if (!this.excludedSubtreesDN.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append("DN:\n");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append(this.excludedSubtreesDN.toString());
            stringBuilder.append("\n");
            stringBuilder2 = stringBuilder.toString();
        }
        if (!this.excludedSubtreesDNS.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append("DNS:\n");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append(this.excludedSubtreesDNS.toString());
            stringBuilder.append("\n");
            stringBuilder2 = stringBuilder.toString();
        }
        if (!this.excludedSubtreesEmail.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append("Email:\n");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append(this.excludedSubtreesEmail.toString());
            stringBuilder.append("\n");
            stringBuilder2 = stringBuilder.toString();
        }
        if (!this.excludedSubtreesURI.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append("URI:\n");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuilder2);
            stringBuilder.append(this.excludedSubtreesURI.toString());
            stringBuilder.append("\n");
            stringBuilder2 = stringBuilder.toString();
        }
        if (this.excludedSubtreesIP.isEmpty()) {
            return stringBuilder2;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(stringBuilder2);
        stringBuilder.append("IP:\n");
        stringBuilder2 = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(stringBuilder2);
        stringBuilder.append(stringifyIPCollection(this.excludedSubtreesIP));
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
