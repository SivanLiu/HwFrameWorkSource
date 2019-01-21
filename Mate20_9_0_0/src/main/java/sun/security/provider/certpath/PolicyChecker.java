package sun.security.provider.certpath;

import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.PolicyNode;
import java.security.cert.PolicyQualifierInfo;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import sun.security.util.Debug;
import sun.security.x509.CertificatePoliciesExtension;
import sun.security.x509.CertificatePolicyMap;
import sun.security.x509.InhibitAnyPolicyExtension;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.PolicyConstraintsExtension;
import sun.security.x509.PolicyInformation;
import sun.security.x509.PolicyMappingsExtension;
import sun.security.x509.X509CertImpl;

class PolicyChecker extends PKIXCertPathChecker {
    static final String ANY_POLICY = "2.5.29.32.0";
    private static final Debug debug = Debug.getInstance("certpath");
    private final boolean anyPolicyInhibited;
    private int certIndex;
    private final int certPathLen;
    private final boolean expPolicyRequired;
    private int explicitPolicy;
    private int inhibitAnyPolicy;
    private final Set<String> initPolicies;
    private final boolean polMappingInhibited;
    private int policyMapping;
    private final boolean rejectPolicyQualifiers;
    private PolicyNodeImpl rootNode;
    private Set<String> supportedExts;

    PolicyChecker(Set<String> initialPolicies, int certPathLen, boolean expPolicyRequired, boolean polMappingInhibited, boolean anyPolicyInhibited, boolean rejectPolicyQualifiers, PolicyNodeImpl rootNode) {
        if (initialPolicies.isEmpty()) {
            this.initPolicies = new HashSet(1);
            this.initPolicies.add(ANY_POLICY);
        } else {
            this.initPolicies = new HashSet((Collection) initialPolicies);
        }
        this.certPathLen = certPathLen;
        this.expPolicyRequired = expPolicyRequired;
        this.polMappingInhibited = polMappingInhibited;
        this.anyPolicyInhibited = anyPolicyInhibited;
        this.rejectPolicyQualifiers = rejectPolicyQualifiers;
        this.rootNode = rootNode;
    }

    public void init(boolean forward) throws CertPathValidatorException {
        if (forward) {
            throw new CertPathValidatorException("forward checking not supported");
        }
        this.certIndex = 1;
        int i = 0;
        this.explicitPolicy = this.expPolicyRequired ? 0 : this.certPathLen + 1;
        this.policyMapping = this.polMappingInhibited ? 0 : this.certPathLen + 1;
        if (!this.anyPolicyInhibited) {
            i = this.certPathLen + 1;
        }
        this.inhibitAnyPolicy = i;
    }

    public boolean isForwardCheckingSupported() {
        return false;
    }

    public Set<String> getSupportedExtensions() {
        if (this.supportedExts == null) {
            this.supportedExts = new HashSet(4);
            this.supportedExts.add(PKIXExtensions.CertificatePolicies_Id.toString());
            this.supportedExts.add(PKIXExtensions.PolicyMappings_Id.toString());
            this.supportedExts.add(PKIXExtensions.PolicyConstraints_Id.toString());
            this.supportedExts.add(PKIXExtensions.InhibitAnyPolicy_Id.toString());
            this.supportedExts = Collections.unmodifiableSet(this.supportedExts);
        }
        return this.supportedExts;
    }

    public void check(Certificate cert, Collection<String> unresCritExts) throws CertPathValidatorException {
        checkPolicy((X509Certificate) cert);
        if (unresCritExts != null && !unresCritExts.isEmpty()) {
            unresCritExts.remove(PKIXExtensions.CertificatePolicies_Id.toString());
            unresCritExts.remove(PKIXExtensions.PolicyMappings_Id.toString());
            unresCritExts.remove(PKIXExtensions.PolicyConstraints_Id.toString());
            unresCritExts.remove(PKIXExtensions.InhibitAnyPolicy_Id.toString());
        }
    }

    private void checkPolicy(X509Certificate currCert) throws CertPathValidatorException {
        String msg = "certificate policies";
        if (debug != null) {
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PolicyChecker.checkPolicy() ---checking ");
            stringBuilder.append(msg);
            stringBuilder.append("...");
            debug.println(stringBuilder.toString());
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PolicyChecker.checkPolicy() certIndex = ");
            stringBuilder.append(this.certIndex);
            debug.println(stringBuilder.toString());
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PolicyChecker.checkPolicy() BEFORE PROCESSING: explicitPolicy = ");
            stringBuilder.append(this.explicitPolicy);
            debug.println(stringBuilder.toString());
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PolicyChecker.checkPolicy() BEFORE PROCESSING: policyMapping = ");
            stringBuilder.append(this.policyMapping);
            debug.println(stringBuilder.toString());
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PolicyChecker.checkPolicy() BEFORE PROCESSING: inhibitAnyPolicy = ");
            stringBuilder.append(this.inhibitAnyPolicy);
            debug.println(stringBuilder.toString());
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PolicyChecker.checkPolicy() BEFORE PROCESSING: policyTree = ");
            stringBuilder.append(this.rootNode);
            debug.println(stringBuilder.toString());
        }
        try {
            X509CertImpl currCertImpl = X509CertImpl.toImpl(currCert);
            boolean finalCert = this.certIndex == this.certPathLen;
            this.rootNode = processPolicies(this.certIndex, this.initPolicies, this.explicitPolicy, this.policyMapping, this.inhibitAnyPolicy, this.rejectPolicyQualifiers, this.rootNode, currCertImpl, finalCert);
            if (!finalCert) {
                this.explicitPolicy = mergeExplicitPolicy(this.explicitPolicy, currCertImpl, finalCert);
                this.policyMapping = mergePolicyMapping(this.policyMapping, currCertImpl);
                this.inhibitAnyPolicy = mergeInhibitAnyPolicy(this.inhibitAnyPolicy, currCertImpl);
            }
            this.certIndex++;
            if (debug != null) {
                Debug debug2 = debug;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("PolicyChecker.checkPolicy() AFTER PROCESSING: explicitPolicy = ");
                stringBuilder2.append(this.explicitPolicy);
                debug2.println(stringBuilder2.toString());
                debug2 = debug;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("PolicyChecker.checkPolicy() AFTER PROCESSING: policyMapping = ");
                stringBuilder2.append(this.policyMapping);
                debug2.println(stringBuilder2.toString());
                debug2 = debug;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("PolicyChecker.checkPolicy() AFTER PROCESSING: inhibitAnyPolicy = ");
                stringBuilder2.append(this.inhibitAnyPolicy);
                debug2.println(stringBuilder2.toString());
                debug2 = debug;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("PolicyChecker.checkPolicy() AFTER PROCESSING: policyTree = ");
                stringBuilder2.append(this.rootNode);
                debug2.println(stringBuilder2.toString());
                debug2 = debug;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("PolicyChecker.checkPolicy() ");
                stringBuilder2.append(msg);
                stringBuilder2.append(" verified");
                debug2.println(stringBuilder2.toString());
            }
        } catch (CertificateException ce) {
            throw new CertPathValidatorException(ce);
        }
    }

    static int mergeExplicitPolicy(int explicitPolicy, X509CertImpl currCert, boolean finalCert) throws CertPathValidatorException {
        if (explicitPolicy > 0 && !X509CertImpl.isSelfIssued(currCert)) {
            explicitPolicy--;
        }
        try {
            PolicyConstraintsExtension polConstExt = currCert.getPolicyConstraintsExtension();
            if (polConstExt == null) {
                return explicitPolicy;
            }
            int require = polConstExt.get(PolicyConstraintsExtension.REQUIRE).intValue();
            if (debug != null) {
                Debug debug = debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PolicyChecker.mergeExplicitPolicy() require Index from cert = ");
                stringBuilder.append(require);
                debug.println(stringBuilder.toString());
            }
            if (finalCert) {
                if (require == 0) {
                    explicitPolicy = require;
                }
            } else if (require != -1 && (explicitPolicy == -1 || require < explicitPolicy)) {
                explicitPolicy = require;
            }
            return explicitPolicy;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("PolicyChecker.mergeExplicitPolicy unexpected exception");
                e.printStackTrace();
            }
            throw new CertPathValidatorException(e);
        }
    }

    static int mergePolicyMapping(int policyMapping, X509CertImpl currCert) throws CertPathValidatorException {
        if (policyMapping > 0 && !X509CertImpl.isSelfIssued(currCert)) {
            policyMapping--;
        }
        try {
            PolicyConstraintsExtension polConstExt = currCert.getPolicyConstraintsExtension();
            if (polConstExt == null) {
                return policyMapping;
            }
            int inhibit = polConstExt.get(PolicyConstraintsExtension.INHIBIT).intValue();
            if (debug != null) {
                Debug debug = debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PolicyChecker.mergePolicyMapping() inhibit Index from cert = ");
                stringBuilder.append(inhibit);
                debug.println(stringBuilder.toString());
            }
            if (inhibit != -1 && (policyMapping == -1 || inhibit < policyMapping)) {
                policyMapping = inhibit;
            }
            return policyMapping;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("PolicyChecker.mergePolicyMapping unexpected exception");
                e.printStackTrace();
            }
            throw new CertPathValidatorException(e);
        }
    }

    static int mergeInhibitAnyPolicy(int inhibitAnyPolicy, X509CertImpl currCert) throws CertPathValidatorException {
        if (inhibitAnyPolicy > 0 && !X509CertImpl.isSelfIssued(currCert)) {
            inhibitAnyPolicy--;
        }
        try {
            InhibitAnyPolicyExtension inhAnyPolExt = (InhibitAnyPolicyExtension) currCert.getExtension(PKIXExtensions.InhibitAnyPolicy_Id);
            if (inhAnyPolExt == null) {
                return inhibitAnyPolicy;
            }
            int skipCerts = inhAnyPolExt.get(InhibitAnyPolicyExtension.SKIP_CERTS).intValue();
            if (debug != null) {
                Debug debug = debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PolicyChecker.mergeInhibitAnyPolicy() skipCerts Index from cert = ");
                stringBuilder.append(skipCerts);
                debug.println(stringBuilder.toString());
            }
            if (skipCerts != -1 && skipCerts < inhibitAnyPolicy) {
                inhibitAnyPolicy = skipCerts;
            }
            return inhibitAnyPolicy;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("PolicyChecker.mergeInhibitAnyPolicy unexpected exception");
                e.printStackTrace();
            }
            throw new CertPathValidatorException(e);
        }
    }

    static PolicyNodeImpl processPolicies(int certIndex, Set<String> initPolicies, int explicitPolicy, int policyMapping, int inhibitAnyPolicy, boolean rejectPolicyQualifiers, PolicyNodeImpl origRootNode, X509CertImpl currCert, boolean finalCert) throws CertPathValidatorException {
        PolicyNodeImpl rootNode;
        Set<PolicyQualifierInfo> anyQuals;
        int explicitPolicy2;
        int i = certIndex;
        Set<String> set = initPolicies;
        boolean z = rejectPolicyQualifiers;
        boolean z2 = finalCert;
        boolean policiesCritical = false;
        Set<PolicyQualifierInfo> anyQuals2 = new HashSet();
        if (origRootNode == null) {
            rootNode = null;
        } else {
            rootNode = origRootNode.copyTree();
        }
        PolicyNodeImpl rootNode2 = rootNode;
        CertificatePoliciesExtension currCertPolicies = currCert.getCertificatePoliciesExtension();
        if (currCertPolicies == null || rootNode2 == null) {
            if (currCertPolicies == null) {
                if (debug != null) {
                    debug.println("PolicyChecker.processPolicies() no policies present in cert");
                }
                rootNode2 = null;
            }
            anyQuals = anyQuals2;
        } else {
            boolean policiesCritical2 = currCertPolicies.isCritical();
            if (debug != null) {
                Debug debug = debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PolicyChecker.processPolicies() policiesCritical = ");
                stringBuilder.append(policiesCritical2);
                debug.println(stringBuilder.toString());
            }
            try {
                Debug debug2;
                List<PolicyInformation> policyInfo = currCertPolicies.get(CertificatePoliciesExtension.POLICIES);
                if (debug != null) {
                    debug2 = debug;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("PolicyChecker.processPolicies() rejectPolicyQualifiers = ");
                    stringBuilder2.append(z);
                    debug2.println(stringBuilder2.toString());
                }
                Iterator it = policyInfo.iterator();
                boolean foundAnyPolicy = false;
                anyQuals = anyQuals2;
                while (it.hasNext()) {
                    Iterator it2;
                    PolicyInformation curPolInfo = (PolicyInformation) it.next();
                    String objectIdentifier = curPolInfo.getPolicyIdentifier().getIdentifier().toString();
                    if (objectIdentifier.equals(ANY_POLICY)) {
                        anyQuals = curPolInfo.getPolicyQualifiers();
                        foundAnyPolicy = true;
                        it2 = it;
                    } else {
                        if (debug != null) {
                            debug2 = debug;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("PolicyChecker.processPolicies() processing policy: ");
                            stringBuilder3.append(objectIdentifier);
                            debug2.println(stringBuilder3.toString());
                        }
                        Set<PolicyQualifierInfo> pQuals = curPolInfo.getPolicyQualifiers();
                        if (!pQuals.isEmpty() && z && policiesCritical2) {
                            throw new CertPathValidatorException("critical policy qualifiers present in certificate", null, null, -1, PKIXReason.INVALID_POLICY);
                        }
                        Set<PolicyQualifierInfo> pQuals2 = pQuals;
                        String curPolicy = objectIdentifier;
                        it2 = it;
                        if (!processParents(i, policiesCritical2, z, rootNode2, objectIdentifier, pQuals2, false)) {
                            processParents(i, policiesCritical2, z, rootNode2, curPolicy, pQuals2, true);
                        }
                    }
                    it = it2;
                }
                if (foundAnyPolicy && (inhibitAnyPolicy > 0 || (!z2 && X509CertImpl.isSelfIssued(currCert)))) {
                    if (debug != null) {
                        debug.println("PolicyChecker.processPolicies() processing policy: 2.5.29.32.0");
                    }
                    processParents(i, policiesCritical2, z, rootNode2, ANY_POLICY, anyQuals, true);
                }
                rootNode2.prune(i);
                if (!rootNode2.getChildren().hasNext()) {
                    rootNode2 = null;
                }
                policiesCritical = policiesCritical2;
            } catch (IOException policiesCritical3) {
                throw new CertPathValidatorException("Exception while retrieving policyOIDs", policiesCritical3);
            }
        }
        if (!(rootNode2 == null || z2)) {
            rootNode2 = processPolicyMappings(currCert, i, policyMapping, rootNode2, policiesCritical3, anyQuals);
        }
        if (!(rootNode2 == null || set.contains(ANY_POLICY) || currCertPolicies == null)) {
            rootNode2 = removeInvalidNodes(rootNode2, i, set, currCertPolicies);
            if (rootNode2 != null && z2) {
                rootNode2 = rewriteLeafNodes(i, set, rootNode2);
            }
        }
        if (z2) {
            explicitPolicy2 = mergeExplicitPolicy(explicitPolicy, currCert, z2);
        } else {
            explicitPolicy2 = explicitPolicy;
            X509CertImpl x509CertImpl = currCert;
        }
        if (explicitPolicy2 != 0 || rootNode2 != null) {
            return rootNode2;
        }
        throw new CertPathValidatorException("non-null policy tree required and policy tree is null", null, null, -1, PKIXReason.INVALID_POLICY);
    }

    private static PolicyNodeImpl rewriteLeafNodes(int certIndex, Set<String> initPolicies, PolicyNodeImpl rootNode) {
        int i = certIndex;
        PolicyNodeImpl rootNode2 = rootNode;
        Set<PolicyNodeImpl> anyNodes = rootNode2.getPolicyNodesValid(i, ANY_POLICY);
        if (anyNodes.isEmpty()) {
            return rootNode2;
        }
        PolicyNodeImpl anyNode = (PolicyNodeImpl) anyNodes.iterator().next();
        PolicyNodeImpl parentNode = (PolicyNodeImpl) anyNode.getParent();
        parentNode.deleteChild(anyNode);
        HashSet initial = new HashSet((Collection) initPolicies);
        for (PolicyNodeImpl node : rootNode2.getPolicyNodes(i)) {
            initial.remove(node.getValidPolicy());
        }
        if (initial.isEmpty()) {
            rootNode2.prune(i);
            if (!rootNode.getChildren().hasNext()) {
                rootNode2 = null;
            }
        } else {
            boolean anyCritical = anyNode.isCritical();
            Set<PolicyQualifierInfo> anyQualifiers = anyNode.getPolicyQualifiers();
            Iterator it = initial.iterator();
            while (it.hasNext()) {
                String policy = (String) it.next();
                Iterator it2 = it;
                PolicyNodeImpl policyNodeImpl = new PolicyNodeImpl(parentNode, policy, anyQualifiers, anyCritical, Collections.singleton(policy), false);
                it = it2;
            }
        }
        return rootNode2;
    }

    private static boolean processParents(int certIndex, boolean policiesCritical, boolean rejectPolicyQualifiers, PolicyNodeImpl rootNode, String curPolicy, Set<PolicyQualifierInfo> pQuals, boolean matchAny) throws CertPathValidatorException {
        Debug debug;
        StringBuilder stringBuilder;
        String str = curPolicy;
        boolean z = matchAny;
        boolean foundMatch = false;
        if (debug != null) {
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PolicyChecker.processParents(): matchAny = ");
            stringBuilder.append(z);
            debug.println(stringBuilder.toString());
        }
        for (PolicyNodeImpl curParent : rootNode.getPolicyNodesExpected(certIndex - 1, str, z)) {
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("PolicyChecker.processParents() found parent:\n");
                stringBuilder.append(curParent.asString());
                debug.println(stringBuilder.toString());
            }
            String curParPolicy = curParent.getValidPolicy();
            foundMatch = false;
            Set<String> curExpPols;
            if (str.equals(ANY_POLICY)) {
                Set<String> parExpPols = curParent.getExpectedPolicies();
                PolicyNodeImpl curNode = null;
                for (String curParExpPol : parExpPols) {
                    Set<String> parExpPols2;
                    Iterator<PolicyNodeImpl> childIter = curParent.getChildren();
                    while (childIter.hasNext()) {
                        String childPolicy = ((PolicyNodeImpl) childIter.next()).getValidPolicy();
                        if (curParExpPol.equals(childPolicy)) {
                            if (debug != null) {
                                Debug debug2 = debug;
                                curExpPols = foundMatch;
                                foundMatch = new StringBuilder();
                                foundMatch.append(childPolicy);
                                parExpPols2 = parExpPols;
                                foundMatch.append(" in parent's expected policy set already appears in child node");
                                debug2.println(foundMatch.toString());
                            } else {
                                curExpPols = foundMatch;
                                parExpPols2 = parExpPols;
                            }
                            foundMatch = curExpPols;
                            parExpPols = parExpPols2;
                        } else {
                            curExpPols = foundMatch;
                            parExpPols2 = parExpPols;
                        }
                    }
                    curExpPols = foundMatch;
                    parExpPols2 = parExpPols;
                    foundMatch = new HashSet();
                    foundMatch.add(curParExpPol);
                    curNode = new PolicyNodeImpl(curParent, curParExpPol, pQuals, policiesCritical, foundMatch, false);
                    foundMatch = curExpPols;
                    parExpPols = parExpPols2;
                }
                Object obj = foundMatch;
            } else {
                curExpPols = null;
                Set<String> curExpPols2 = new HashSet();
                curExpPols2.add(str);
                foundMatch = new PolicyNodeImpl(curParent, str, pQuals, policiesCritical, curExpPols2, null);
            }
            foundMatch = true;
        }
        return foundMatch;
    }

    private static PolicyNodeImpl processPolicyMappings(X509CertImpl currCert, int certIndex, int policyMapping, PolicyNodeImpl rootNode, boolean policiesCritical, Set<PolicyQualifierInfo> anyQuals) throws CertPathValidatorException {
        int i = certIndex;
        int i2 = policyMapping;
        PolicyNodeImpl policyNodeImpl = rootNode;
        PolicyMappingsExtension polMappingsExt = currCert.getPolicyMappingsExtension();
        if (polMappingsExt == null) {
            return policyNodeImpl;
        }
        if (debug != null) {
            debug.println("PolicyChecker.processPolicyMappings() inside policyMapping check");
        }
        List<CertificatePolicyMap> maps = null;
        try {
            PolicyNodeImpl rootNode2;
            List<CertificatePolicyMap> maps2 = polMappingsExt.get(PolicyMappingsExtension.MAP);
            maps = null;
            for (CertificatePolicyMap polMap : maps2) {
                StringBuilder stringBuilder;
                String issuerDomain = polMap.getIssuerIdentifier().getIdentifier().toString();
                String subjectDomain = polMap.getSubjectIdentifier().getIdentifier().toString();
                if (debug != null) {
                    Debug debug = debug;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("PolicyChecker.processPolicyMappings() issuerDomain = ");
                    stringBuilder.append(issuerDomain);
                    debug.println(stringBuilder.toString());
                    debug = debug;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("PolicyChecker.processPolicyMappings() subjectDomain = ");
                    stringBuilder.append(subjectDomain);
                    debug.println(stringBuilder.toString());
                }
                String str;
                if (issuerDomain.equals(ANY_POLICY)) {
                    str = subjectDomain;
                    throw new CertPathValidatorException("encountered an issuerDomainPolicy of ANY_POLICY", null, null, -1, PKIXReason.INVALID_POLICY);
                } else if (subjectDomain.equals(ANY_POLICY)) {
                    str = subjectDomain;
                    throw new CertPathValidatorException("encountered a subjectDomainPolicy of ANY_POLICY", null, null, -1, PKIXReason.INVALID_POLICY);
                } else {
                    List<CertificatePolicyMap> maps3;
                    Set<PolicyNodeImpl> validNodes = policyNodeImpl.getPolicyNodesValid(i, issuerDomain);
                    int i3 = -1;
                    PolicyNodeImpl curAnyNode;
                    if (validNodes.isEmpty()) {
                        maps3 = maps2;
                        if (i2 > 0 || i2 == -1) {
                            Iterator it = policyNodeImpl.getPolicyNodesValid(i, ANY_POLICY).iterator();
                            while (it.hasNext()) {
                                curAnyNode = (PolicyNodeImpl) it.next();
                                PolicyNodeImpl curAnyNodeParent = (PolicyNodeImpl) curAnyNode.getParent();
                                Set<String> expPols = new HashSet();
                                expPols.add(subjectDomain);
                                Iterator it2 = it;
                                Set<PolicyNodeImpl> validNodes2 = validNodes;
                                str = subjectDomain;
                                PolicyNodeImpl policyNodeImpl2 = new PolicyNodeImpl(curAnyNodeParent, issuerDomain, anyQuals, policiesCritical, expPols, 1);
                                it = it2;
                                validNodes = validNodes2;
                                subjectDomain = str;
                            }
                        }
                    } else {
                        for (PolicyNodeImpl curNode : validNodes) {
                            if (i2 > 0) {
                                maps3 = maps2;
                            } else if (i2 == i3) {
                                maps3 = maps2;
                            } else {
                                if (i2 == 0) {
                                    curAnyNode = (PolicyNodeImpl) curNode.getParent();
                                    if (debug != null) {
                                        Debug debug2 = debug;
                                        stringBuilder = new StringBuilder();
                                        maps3 = maps2;
                                        stringBuilder.append("PolicyChecker.processPolicyMappings() before deleting: policy tree = ");
                                        stringBuilder.append((Object) policyNodeImpl);
                                        debug2.println(stringBuilder.toString());
                                    } else {
                                        maps3 = maps2;
                                    }
                                    curAnyNode.deleteChild(curNode);
                                    if (debug != null) {
                                        maps = debug;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("PolicyChecker.processPolicyMappings() after deleting: policy tree = ");
                                        stringBuilder.append((Object) policyNodeImpl);
                                        maps.println(stringBuilder.toString());
                                    }
                                    maps = 1;
                                } else {
                                    maps3 = maps2;
                                }
                                maps2 = maps3;
                                i3 = -1;
                            }
                            curNode.addExpectedPolicy(subjectDomain);
                            maps2 = maps3;
                            i3 = -1;
                        }
                        maps3 = maps2;
                    }
                    maps2 = maps3;
                }
            }
            if (maps != null) {
                policyNodeImpl.prune(i);
                if (!rootNode.getChildren().hasNext()) {
                    if (debug != null) {
                        debug.println("setting rootNode to null");
                    }
                    rootNode2 = null;
                    return rootNode2;
                }
            }
            rootNode2 = policyNodeImpl;
            return rootNode2;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("PolicyChecker.processPolicyMappings() mapping exception");
                e.printStackTrace();
            }
            throw new CertPathValidatorException("Exception while checking mapping", e);
        }
    }

    private static PolicyNodeImpl removeInvalidNodes(PolicyNodeImpl rootNode, int certIndex, Set<String> initPolicies, CertificatePoliciesExtension currCertPolicies) throws CertPathValidatorException {
        try {
            boolean childDeleted = false;
            for (PolicyInformation curPolInfo : currCertPolicies.get(CertificatePoliciesExtension.POLICIES)) {
                String curPolicy = curPolInfo.getPolicyIdentifier().getIdentifier().toString();
                if (debug != null) {
                    Debug debug = debug;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PolicyChecker.processPolicies() processing policy second time: ");
                    stringBuilder.append(curPolicy);
                    debug.println(stringBuilder.toString());
                }
                for (PolicyNodeImpl curNode : rootNode.getPolicyNodesValid(certIndex, curPolicy)) {
                    PolicyNodeImpl parentNode = (PolicyNodeImpl) curNode.getParent();
                    if (!(!parentNode.getValidPolicy().equals(ANY_POLICY) || initPolicies.contains(curPolicy) || curPolicy.equals(ANY_POLICY))) {
                        Debug debug2;
                        StringBuilder stringBuilder2;
                        if (debug != null) {
                            debug2 = debug;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("PolicyChecker.processPolicies() before deleting: policy tree = ");
                            stringBuilder2.append((Object) rootNode);
                            debug2.println(stringBuilder2.toString());
                        }
                        parentNode.deleteChild(curNode);
                        childDeleted = true;
                        if (debug != null) {
                            debug2 = debug;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("PolicyChecker.processPolicies() after deleting: policy tree = ");
                            stringBuilder2.append((Object) rootNode);
                            debug2.println(stringBuilder2.toString());
                        }
                    }
                }
            }
            if (!childDeleted) {
                return rootNode;
            }
            rootNode.prune(certIndex);
            if (rootNode.getChildren().hasNext()) {
                return rootNode;
            }
            return null;
        } catch (IOException ioe) {
            throw new CertPathValidatorException("Exception while retrieving policyOIDs", ioe);
        }
    }

    PolicyNode getPolicyTree() {
        if (this.rootNode == null) {
            return null;
        }
        PolicyNodeImpl policyTree = this.rootNode.copyTree();
        policyTree.setImmutable();
        return policyTree;
    }
}
