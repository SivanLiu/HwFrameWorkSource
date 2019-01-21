package sun.security.util;

import java.security.AlgorithmParameters;
import java.security.CryptoPrimitive;
import java.security.Key;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisabledAlgorithmConstraints extends AbstractAlgorithmConstraints {
    public static final String PROPERTY_CERTPATH_DISABLED_ALGS = "jdk.certpath.disabledAlgorithms";
    public static final String PROPERTY_JAR_DISABLED_ALGS = "jdk.jar.disabledAlgorithms";
    public static final String PROPERTY_TLS_DISABLED_ALGS = "jdk.tls.disabledAlgorithms";
    private static final Debug debug = Debug.getInstance("certpath");
    private final Constraints algorithmConstraints;
    private final String[] disabledAlgorithms;

    private static abstract class Constraint {
        String algorithm;
        Constraint nextConstraint;

        enum Operator {
            EQ,
            NE,
            LT,
            LE,
            GT,
            GE;

            /* JADX WARNING: Removed duplicated region for block: B:32:0x005d  */
            /* JADX WARNING: Removed duplicated region for block: B:44:0x0088  */
            /* JADX WARNING: Removed duplicated region for block: B:42:0x0085  */
            /* JADX WARNING: Removed duplicated region for block: B:40:0x0082  */
            /* JADX WARNING: Removed duplicated region for block: B:38:0x007f  */
            /* JADX WARNING: Removed duplicated region for block: B:36:0x007c  */
            /* JADX WARNING: Removed duplicated region for block: B:34:0x0079  */
            /* JADX WARNING: Removed duplicated region for block: B:32:0x005d  */
            /* JADX WARNING: Removed duplicated region for block: B:44:0x0088  */
            /* JADX WARNING: Removed duplicated region for block: B:42:0x0085  */
            /* JADX WARNING: Removed duplicated region for block: B:40:0x0082  */
            /* JADX WARNING: Removed duplicated region for block: B:38:0x007f  */
            /* JADX WARNING: Removed duplicated region for block: B:36:0x007c  */
            /* JADX WARNING: Removed duplicated region for block: B:34:0x0079  */
            /* JADX WARNING: Removed duplicated region for block: B:32:0x005d  */
            /* JADX WARNING: Removed duplicated region for block: B:44:0x0088  */
            /* JADX WARNING: Removed duplicated region for block: B:42:0x0085  */
            /* JADX WARNING: Removed duplicated region for block: B:40:0x0082  */
            /* JADX WARNING: Removed duplicated region for block: B:38:0x007f  */
            /* JADX WARNING: Removed duplicated region for block: B:36:0x007c  */
            /* JADX WARNING: Removed duplicated region for block: B:34:0x0079  */
            /* JADX WARNING: Removed duplicated region for block: B:32:0x005d  */
            /* JADX WARNING: Removed duplicated region for block: B:44:0x0088  */
            /* JADX WARNING: Removed duplicated region for block: B:42:0x0085  */
            /* JADX WARNING: Removed duplicated region for block: B:40:0x0082  */
            /* JADX WARNING: Removed duplicated region for block: B:38:0x007f  */
            /* JADX WARNING: Removed duplicated region for block: B:36:0x007c  */
            /* JADX WARNING: Removed duplicated region for block: B:34:0x0079  */
            /* JADX WARNING: Removed duplicated region for block: B:32:0x005d  */
            /* JADX WARNING: Removed duplicated region for block: B:44:0x0088  */
            /* JADX WARNING: Removed duplicated region for block: B:42:0x0085  */
            /* JADX WARNING: Removed duplicated region for block: B:40:0x0082  */
            /* JADX WARNING: Removed duplicated region for block: B:38:0x007f  */
            /* JADX WARNING: Removed duplicated region for block: B:36:0x007c  */
            /* JADX WARNING: Removed duplicated region for block: B:34:0x0079  */
            /* JADX WARNING: Removed duplicated region for block: B:32:0x005d  */
            /* JADX WARNING: Removed duplicated region for block: B:44:0x0088  */
            /* JADX WARNING: Removed duplicated region for block: B:42:0x0085  */
            /* JADX WARNING: Removed duplicated region for block: B:40:0x0082  */
            /* JADX WARNING: Removed duplicated region for block: B:38:0x007f  */
            /* JADX WARNING: Removed duplicated region for block: B:36:0x007c  */
            /* JADX WARNING: Removed duplicated region for block: B:34:0x0079  */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            static Operator of(String s) {
                Object obj;
                int hashCode = s.hashCode();
                if (hashCode == 60) {
                    if (s.equals("<")) {
                        obj = 2;
                        switch (obj) {
                            case null:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            case 4:
                                break;
                            case 5:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 62) {
                    if (s.equals(">")) {
                        obj = 4;
                        switch (obj) {
                            case null:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            case 4:
                                break;
                            case 5:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 1084) {
                    if (s.equals("!=")) {
                        obj = 1;
                        switch (obj) {
                            case null:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            case 4:
                                break;
                            case 5:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 1921) {
                    if (s.equals("<=")) {
                        obj = 3;
                        switch (obj) {
                            case null:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            case 4:
                                break;
                            case 5:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 1952) {
                    if (s.equals("==")) {
                        obj = null;
                        switch (obj) {
                            case null:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            case 4:
                                break;
                            case 5:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 1983 && s.equals(">=")) {
                    obj = 5;
                    switch (obj) {
                        case null:
                            return EQ;
                        case 1:
                            return NE;
                        case 2:
                            return LT;
                        case 3:
                            return LE;
                        case 4:
                            return GT;
                        case 5:
                            return GE;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error in security property. ");
                            stringBuilder.append(s);
                            stringBuilder.append(" is not a legal Operator");
                            throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                obj = -1;
                switch (obj) {
                    case null:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                    default:
                        break;
                }
            }
        }

        public abstract void permits(CertConstraintParameters certConstraintParameters) throws CertPathValidatorException;

        private Constraint() {
            this.nextConstraint = null;
        }

        public boolean permits(Key key) {
            return true;
        }
    }

    private static class Constraints {
        private static final Pattern keySizePattern = Pattern.compile("keySize\\s*(<=|<|==|!=|>|>=)\\s*(\\d+)");
        private Map<String, Set<Constraint>> constraintsMap = new HashMap();

        public Constraints(String[] constraintArray) {
            String[] strArr = constraintArray;
            int length = strArr.length;
            int i = 0;
            int i2 = 0;
            while (i2 < length) {
                int i3;
                String constraintEntry = strArr[i2];
                if (!(constraintEntry == null || constraintEntry.isEmpty())) {
                    constraintEntry = constraintEntry.trim();
                    if (DisabledAlgorithmConstraints.debug != null) {
                        Debug access$000 = DisabledAlgorithmConstraints.debug;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Constraints: ");
                        stringBuilder.append(constraintEntry);
                        access$000.println(stringBuilder.toString());
                    }
                    int space = constraintEntry.indexOf(32);
                    int space2;
                    if (space > 0) {
                        String algorithm = AlgorithmDecomposer.hashName(constraintEntry.substring(i, space).toUpperCase(Locale.ENGLISH));
                        String[] split = constraintEntry.substring(space + 1).split("&");
                        int length2 = split.length;
                        boolean jdkCALimit = false;
                        Constraint lastConstraint = null;
                        Constraint c = null;
                        int c2 = i;
                        while (c2 < length2) {
                            String entry = split[c2].trim();
                            Matcher matcher = keySizePattern.matcher(entry);
                            StringBuilder stringBuilder2;
                            if (matcher.matches()) {
                                if (DisabledAlgorithmConstraints.debug != null) {
                                    Debug access$0002 = DisabledAlgorithmConstraints.debug;
                                    i3 = length;
                                    stringBuilder2 = new StringBuilder();
                                    space2 = space;
                                    stringBuilder2.append("Constraints set to keySize: ");
                                    stringBuilder2.append(entry);
                                    access$0002.println(stringBuilder2.toString());
                                } else {
                                    i3 = length;
                                    space2 = space;
                                }
                                c = new KeySizeConstraint(algorithm, Operator.of(matcher.group(1)), Integer.parseInt(matcher.group(2)));
                            } else {
                                i3 = length;
                                space2 = space;
                                if (entry.equalsIgnoreCase("jdkCA")) {
                                    if (DisabledAlgorithmConstraints.debug != null) {
                                        DisabledAlgorithmConstraints.debug.println("Constraints set to jdkCA.");
                                    }
                                    if (jdkCALimit) {
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Only one jdkCA entry allowed in property. Constraint: ");
                                        stringBuilder2.append(constraintEntry);
                                        throw new IllegalArgumentException(stringBuilder2.toString());
                                    }
                                    c = new jdkCAConstraint(algorithm);
                                    jdkCALimit = true;
                                }
                            }
                            if (lastConstraint == null) {
                                if (!this.constraintsMap.containsKey(algorithm)) {
                                    this.constraintsMap.putIfAbsent(algorithm, new HashSet());
                                }
                                if (c != null) {
                                    ((Set) this.constraintsMap.get(algorithm)).add(c);
                                }
                            } else {
                                lastConstraint.nextConstraint = c;
                            }
                            lastConstraint = c;
                            c2++;
                            length = i3;
                            space = space2;
                            strArr = constraintArray;
                        }
                    } else {
                        i3 = length;
                        space2 = space;
                        this.constraintsMap.putIfAbsent(constraintEntry.toUpperCase(Locale.ENGLISH), new HashSet());
                        i2++;
                        length = i3;
                        strArr = constraintArray;
                        i = 0;
                    }
                }
                i3 = length;
                i2++;
                length = i3;
                strArr = constraintArray;
                i = 0;
            }
        }

        private Set<Constraint> getConstraints(String algorithm) {
            return (Set) this.constraintsMap.get(algorithm);
        }

        public boolean permits(Key key) {
            Set<Constraint> set = getConstraints(key.getAlgorithm());
            if (set == null) {
                return true;
            }
            for (Constraint constraint : set) {
                if (!constraint.permits(key)) {
                    if (DisabledAlgorithmConstraints.debug != null) {
                        Debug access$000 = DisabledAlgorithmConstraints.debug;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("keySizeConstraint: failed key constraint check ");
                        stringBuilder.append(KeyUtil.getKeySize(key));
                        access$000.println(stringBuilder.toString());
                    }
                    return false;
                }
            }
            return true;
        }

        public void permits(CertConstraintParameters cp) throws CertPathValidatorException {
            X509Certificate cert = cp.getCertificate();
            if (DisabledAlgorithmConstraints.debug != null) {
                Debug access$000 = DisabledAlgorithmConstraints.debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Constraints.permits(): ");
                stringBuilder.append(cert.getSigAlgName());
                access$000.println(stringBuilder.toString());
            }
            Set<String> algorithms = AlgorithmDecomposer.decomposeOneHash(cert.getSigAlgName());
            if (algorithms != null && !algorithms.isEmpty()) {
                algorithms.add(cert.getPublicKey().getAlgorithm());
                for (String algorithm : algorithms) {
                    Set<Constraint> set = getConstraints(algorithm);
                    if (set != null) {
                        for (Constraint constraint : set) {
                            constraint.permits(cp);
                        }
                    }
                }
            }
        }
    }

    private static class KeySizeConstraint extends Constraint {
        private int maxSize;
        private int minSize;
        private int prohibitedSize = -1;

        public KeySizeConstraint(String algo, Operator operator, int length) {
            super();
            this.algorithm = algo;
            int i = 0;
            switch (operator) {
                case EQ:
                    this.minSize = 0;
                    this.maxSize = Integer.MAX_VALUE;
                    this.prohibitedSize = length;
                    return;
                case NE:
                    this.minSize = length;
                    this.maxSize = length;
                    return;
                case LT:
                    this.minSize = length;
                    this.maxSize = Integer.MAX_VALUE;
                    return;
                case LE:
                    this.minSize = length + 1;
                    this.maxSize = Integer.MAX_VALUE;
                    return;
                case GT:
                    this.minSize = 0;
                    this.maxSize = length;
                    return;
                case GE:
                    this.minSize = 0;
                    if (length > 1) {
                        i = length - 1;
                    }
                    this.maxSize = i;
                    return;
                default:
                    this.minSize = Integer.MAX_VALUE;
                    this.maxSize = -1;
                    return;
            }
        }

        public void permits(CertConstraintParameters cp) throws CertPathValidatorException {
            if (!permitsImpl(cp.getCertificate().getPublicKey())) {
                if (this.nextConstraint != null) {
                    this.nextConstraint.permits(cp);
                } else {
                    throw new CertPathValidatorException("Algorithm constraints check failed on keysize limits", null, null, -1, BasicReason.ALGORITHM_CONSTRAINED);
                }
            }
        }

        public boolean permits(Key key) {
            if (this.nextConstraint != null && this.nextConstraint.permits(key)) {
                return true;
            }
            if (DisabledAlgorithmConstraints.debug != null) {
                Debug access$000 = DisabledAlgorithmConstraints.debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("KeySizeConstraints.permits(): ");
                stringBuilder.append(this.algorithm);
                access$000.println(stringBuilder.toString());
            }
            return permitsImpl(key);
        }

        private boolean permitsImpl(Key key) {
            boolean z = true;
            if (this.algorithm.compareToIgnoreCase(key.getAlgorithm()) != 0) {
                return true;
            }
            int size = KeyUtil.getKeySize(key);
            if (size == 0) {
                return false;
            }
            if (size <= 0) {
                return true;
            }
            if (size < this.minSize || size > this.maxSize || this.prohibitedSize == size) {
                z = false;
            }
            return z;
        }
    }

    private static class jdkCAConstraint extends Constraint {
        jdkCAConstraint(String algo) {
            super();
            this.algorithm = algo;
        }

        public void permits(CertConstraintParameters cp) throws CertPathValidatorException {
            if (DisabledAlgorithmConstraints.debug != null) {
                Debug access$000 = DisabledAlgorithmConstraints.debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("jdkCAConstraints.permits(): ");
                stringBuilder.append(this.algorithm);
                access$000.println(stringBuilder.toString());
            }
            if (!cp.isTrustedMatch()) {
                return;
            }
            if (this.nextConstraint != null) {
                this.nextConstraint.permits(cp);
            } else {
                throw new CertPathValidatorException("Algorithm constraints check failed on certificate anchor limits", null, null, -1, BasicReason.ALGORITHM_CONSTRAINED);
            }
        }
    }

    public DisabledAlgorithmConstraints(String propertyName) {
        this(propertyName, new AlgorithmDecomposer());
    }

    public DisabledAlgorithmConstraints(String propertyName, AlgorithmDecomposer decomposer) {
        super(decomposer);
        this.disabledAlgorithms = AbstractAlgorithmConstraints.getAlgorithms(propertyName);
        this.algorithmConstraints = new Constraints(this.disabledAlgorithms);
    }

    public final boolean permits(Set<CryptoPrimitive> primitives, String algorithm, AlgorithmParameters parameters) {
        if (primitives != null && !primitives.isEmpty()) {
            return AbstractAlgorithmConstraints.checkAlgorithm(this.disabledAlgorithms, algorithm, this.decomposer);
        }
        throw new IllegalArgumentException("No cryptographic primitive specified");
    }

    public final boolean permits(Set<CryptoPrimitive> primitives, Key key) {
        return checkConstraints(primitives, "", key, null);
    }

    public final boolean permits(Set<CryptoPrimitive> primitives, String algorithm, Key key, AlgorithmParameters parameters) {
        if (algorithm != null && algorithm.length() != 0) {
            return checkConstraints(primitives, algorithm, key, parameters);
        }
        throw new IllegalArgumentException("No algorithm name specified");
    }

    public final void permits(Set<CryptoPrimitive> primitives, CertConstraintParameters cp) throws CertPathValidatorException {
        checkConstraints(primitives, cp);
    }

    public final void permits(Set<CryptoPrimitive> primitives, X509Certificate cert) throws CertPathValidatorException {
        checkConstraints(primitives, new CertConstraintParameters(cert));
    }

    public boolean checkProperty(String param) {
        param = param.toLowerCase(Locale.ENGLISH);
        for (String block : this.disabledAlgorithms) {
            if (block.toLowerCase(Locale.ENGLISH).indexOf(param) >= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean checkConstraints(Set<CryptoPrimitive> primitives, String algorithm, Key key, AlgorithmParameters parameters) {
        if (key == null) {
            throw new IllegalArgumentException("The key cannot be null");
        } else if ((algorithm == null || algorithm.length() == 0 || permits(primitives, algorithm, parameters)) && permits(primitives, key.getAlgorithm(), null)) {
            return this.algorithmConstraints.permits(key);
        } else {
            return false;
        }
    }

    private void checkConstraints(Set<CryptoPrimitive> primitives, CertConstraintParameters cp) throws CertPathValidatorException {
        Set<CryptoPrimitive> set = primitives;
        X509Certificate cert = cp.getCertificate();
        String algorithm = cert.getSigAlgName();
        CertConstraintParameters certConstraintParameters;
        StringBuilder stringBuilder;
        if (!permits(set, algorithm, null)) {
            certConstraintParameters = cp;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Algorithm constraints check failed on disabled signature algorithm: ");
            stringBuilder.append(algorithm);
            throw new CertPathValidatorException(stringBuilder.toString(), null, null, -1, BasicReason.ALGORITHM_CONSTRAINED);
        } else if (permits(set, cert.getPublicKey().getAlgorithm(), null)) {
            this.algorithmConstraints.permits(cp);
        } else {
            certConstraintParameters = cp;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Algorithm constraints check failed on disabled public key algorithm: ");
            stringBuilder.append(algorithm);
            throw new CertPathValidatorException(stringBuilder.toString(), null, null, -1, BasicReason.ALGORITHM_CONSTRAINED);
        }
    }
}
