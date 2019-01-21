package android.security.net.config;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.os.storage.StorageManager;
import android.security.net.config.NetworkSecurityConfig.Builder;
import android.telephony.SubscriptionPlan;
import android.util.ArraySet;
import android.util.Base64;
import android.util.Pair;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XmlConfigSource implements ConfigSource {
    private static final int CONFIG_BASE = 0;
    private static final int CONFIG_DEBUG = 2;
    private static final int CONFIG_DOMAIN = 1;
    private final ApplicationInfo mApplicationInfo;
    private Context mContext;
    private final boolean mDebugBuild;
    private NetworkSecurityConfig mDefaultConfig;
    private Set<Pair<Domain, NetworkSecurityConfig>> mDomainMap;
    private boolean mInitialized;
    private final Object mLock = new Object();
    private final int mResourceId;

    public static class ParserException extends Exception {
        public ParserException(XmlPullParser parser, String message, Throwable cause) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(message);
            stringBuilder.append(" at: ");
            stringBuilder.append(parser.getPositionDescription());
            super(stringBuilder.toString(), cause);
        }

        public ParserException(XmlPullParser parser, String message) {
            this(parser, message, null);
        }
    }

    public XmlConfigSource(Context context, int resourceId, ApplicationInfo info) {
        this.mContext = context;
        this.mResourceId = resourceId;
        this.mApplicationInfo = new ApplicationInfo(info);
        this.mDebugBuild = (this.mApplicationInfo.flags & 2) != 0;
    }

    public Set<Pair<Domain, NetworkSecurityConfig>> getPerDomainConfigs() {
        ensureInitialized();
        return this.mDomainMap;
    }

    public NetworkSecurityConfig getDefaultConfig() {
        ensureInitialized();
        return this.mDefaultConfig;
    }

    private static final String getConfigString(int configType) {
        switch (configType) {
            case 0:
                return "base-config";
            case 1:
                return "domain-config";
            case 2:
                return "debug-overrides";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown config type: ");
                stringBuilder.append(configType);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private void ensureInitialized() {
        synchronized (this.mLock) {
            if (this.mInitialized) {
                return;
            }
            XmlResourceParser parser;
            try {
                parser = this.mContext.getResources().getXml(this.mResourceId);
                parseNetworkSecurityConfig(parser);
                this.mContext = null;
                this.mInitialized = true;
                if (parser != null) {
                    $closeResource(null, parser);
                }
            } catch (NotFoundException | ParserException | IOException | XmlPullParserException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to parse XML configuration from ");
                stringBuilder.append(this.mContext.getResources().getResourceEntryName(this.mResourceId));
                throw new RuntimeException(stringBuilder.toString(), e);
            } catch (Throwable th) {
                if (parser != null) {
                    $closeResource(r2, parser);
                }
            }
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private Pin parsePin(XmlResourceParser parser) throws IOException, XmlPullParserException, ParserException {
        String digestAlgorithm = parser.getAttributeValue(null, "digest");
        if (!Pin.isSupportedDigestAlgorithm(digestAlgorithm)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported pin digest algorithm: ");
            stringBuilder.append(digestAlgorithm);
            throw new ParserException(parser, stringBuilder.toString());
        } else if (parser.next() == 4) {
            try {
                byte[] decodedDigest = Base64.decode(parser.getText().trim(), 0);
                int expectedLength = Pin.getDigestLength(digestAlgorithm);
                if (decodedDigest.length != expectedLength) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("digest length ");
                    stringBuilder2.append(decodedDigest.length);
                    stringBuilder2.append(" does not match expected length for ");
                    stringBuilder2.append(digestAlgorithm);
                    stringBuilder2.append(" of ");
                    stringBuilder2.append(expectedLength);
                    throw new ParserException(parser, stringBuilder2.toString());
                } else if (parser.next() == 3) {
                    return new Pin(digestAlgorithm, decodedDigest);
                } else {
                    throw new ParserException(parser, "pin contains additional elements");
                }
            } catch (IllegalArgumentException e) {
                throw new ParserException(parser, "Invalid pin digest", e);
            }
        } else {
            throw new ParserException(parser, "Missing pin digest");
        }
    }

    private PinSet parsePinSet(XmlResourceParser parser) throws IOException, XmlPullParserException, ParserException {
        String expirationDate = parser.getAttributeValue(null, "expiration");
        long expirationTimestampMilis = SubscriptionPlan.BYTES_UNLIMITED;
        if (expirationDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                Date date = sdf.parse(expirationDate);
                if (date != null) {
                    expirationTimestampMilis = date.getTime();
                } else {
                    throw new ParserException(parser, "Invalid expiration date in pin-set");
                }
            } catch (ParseException e) {
                throw new ParserException(parser, "Invalid expiration date in pin-set", e);
            }
        }
        int outerDepth = parser.getDepth();
        Set<Pin> pins = new ArraySet();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals("pin")) {
                pins.add(parsePin(parser));
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
        return new PinSet(pins, expirationTimestampMilis);
    }

    private Domain parseDomain(XmlResourceParser parser, Set<String> seenDomains) throws IOException, XmlPullParserException, ParserException {
        boolean includeSubdomains = parser.getAttributeBooleanValue(null, "includeSubdomains", false);
        if (parser.next() == 4) {
            String domain = parser.getText().trim().toLowerCase(Locale.US);
            if (parser.next() != 3) {
                throw new ParserException(parser, "domain contains additional elements");
            } else if (seenDomains.add(domain)) {
                return new Domain(domain, includeSubdomains);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(domain);
                stringBuilder.append(" has already been specified");
                throw new ParserException(parser, stringBuilder.toString());
            }
        }
        throw new ParserException(parser, "Domain name missing");
    }

    private CertificatesEntryRef parseCertificatesEntry(XmlResourceParser parser, boolean defaultOverridePins) throws IOException, XmlPullParserException, ParserException {
        boolean overridePins = parser.getAttributeBooleanValue(null, "overridePins", defaultOverridePins);
        int sourceId = parser.getAttributeResourceValue(null, "src", -1);
        String sourceString = parser.getAttributeValue(null, "src");
        if (sourceString != null) {
            CertificateSource source;
            if (sourceId != -1) {
                source = new ResourceCertificateSource(sourceId, this.mContext);
            } else if (StorageManager.UUID_SYSTEM.equals(sourceString)) {
                source = SystemCertificateSource.getInstance();
            } else if ("user".equals(sourceString)) {
                source = UserCertificateSource.getInstance();
            } else {
                throw new ParserException(parser, "Unknown certificates src. Should be one of system|user|@resourceVal");
            }
            XmlUtils.skipCurrentTag(parser);
            return new CertificatesEntryRef(source, overridePins);
        }
        throw new ParserException(parser, "certificates element missing src attribute");
    }

    private Collection<CertificatesEntryRef> parseTrustAnchors(XmlResourceParser parser, boolean defaultOverridePins) throws IOException, XmlPullParserException, ParserException {
        int outerDepth = parser.getDepth();
        List<CertificatesEntryRef> anchors = new ArrayList();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals("certificates")) {
                anchors.add(parseCertificatesEntry(parser, defaultOverridePins));
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
        return anchors;
    }

    private List<Pair<Builder, Set<Domain>>> parseConfigEntry(XmlResourceParser parser, Set<String> seenDomains, Builder parentBuilder, int configType) throws IOException, XmlPullParserException, ParserException {
        Set<String> set;
        XmlConfigSource xmlConfigSource = this;
        XmlResourceParser xmlResourceParser = parser;
        int i = configType;
        List<Pair<Builder, Set<Domain>>> builders = new ArrayList();
        Builder builder = new Builder();
        builder.setParent(parentBuilder);
        Set<Domain> domains = new ArraySet();
        boolean seenPinSet = false;
        boolean seenTrustAnchors = false;
        boolean z = false;
        boolean defaultOverridePins = i == 2;
        String configName = parser.getName();
        int outerDepth = parser.getDepth();
        builders.add(new Pair(builder, domains));
        int i2 = 0;
        while (i2 < parser.getAttributeCount()) {
            String name = xmlResourceParser.getAttributeName(i2);
            if ("hstsEnforced".equals(name)) {
                builder.setHstsEnforced(xmlResourceParser.getAttributeBooleanValue(i2, z));
            } else if ("cleartextTrafficPermitted".equals(name)) {
                builder.setCleartextTrafficPermitted(xmlResourceParser.getAttributeBooleanValue(i2, true));
            }
            i2++;
            z = false;
        }
        while (XmlUtils.nextElementWithin(xmlResourceParser, outerDepth)) {
            String tagName = parser.getName();
            StringBuilder stringBuilder;
            if ("domain".equals(tagName)) {
                if (i == 1) {
                    domains.add(parseDomain(parser, seenDomains));
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("domain element not allowed in ");
                    stringBuilder.append(getConfigString(configType));
                    throw new ParserException(xmlResourceParser, stringBuilder.toString());
                }
            } else if ("trust-anchors".equals(tagName)) {
                if (seenTrustAnchors) {
                    throw new ParserException(xmlResourceParser, "Multiple trust-anchor elements not allowed");
                }
                builder.addCertificatesEntryRefs(xmlConfigSource.parseTrustAnchors(xmlResourceParser, defaultOverridePins));
                seenTrustAnchors = true;
            } else if (!"pin-set".equals(tagName)) {
                if (!"domain-config".equals(tagName)) {
                    set = seenDomains;
                    XmlUtils.skipCurrentTag(parser);
                } else if (i == 1) {
                    builders.addAll(xmlConfigSource.parseConfigEntry(xmlResourceParser, seenDomains, builder, i));
                } else {
                    set = seenDomains;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Nested domain-config not allowed in ");
                    stringBuilder2.append(getConfigString(configType));
                    throw new ParserException(xmlResourceParser, stringBuilder2.toString());
                }
                xmlConfigSource = this;
            } else if (i != 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("pin-set element not allowed in ");
                stringBuilder.append(getConfigString(configType));
                throw new ParserException(xmlResourceParser, stringBuilder.toString());
            } else if (seenPinSet) {
                throw new ParserException(xmlResourceParser, "Multiple pin-set elements not allowed");
            } else {
                builder.setPinSet(parsePinSet(parser));
                seenPinSet = true;
            }
            set = seenDomains;
            xmlConfigSource = this;
        }
        set = seenDomains;
        if (i != 1 || !domains.isEmpty()) {
            return builders;
        }
        throw new ParserException(xmlResourceParser, "No domain elements in domain-config");
    }

    /* JADX WARNING: Missing block: B:8:0x0018, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void addDebugAnchorsIfNeeded(Builder debugConfigBuilder, Builder builder) {
        if (debugConfigBuilder != null && debugConfigBuilder.hasCertificatesEntryRefs() && builder.hasCertificatesEntryRefs()) {
            builder.addCertificatesEntryRefs(debugConfigBuilder.getCertificatesEntryRefs());
        }
    }

    private void parseNetworkSecurityConfig(XmlResourceParser parser) throws IOException, XmlPullParserException, ParserException {
        XmlResourceParser xmlResourceParser = parser;
        Set<String> seenDomains = new ArraySet();
        List<Pair<Builder, Set<Domain>>> builders = new ArrayList();
        Builder baseConfigBuilder = null;
        Builder debugConfigBuilder = null;
        boolean seenDebugOverrides = false;
        boolean seenBaseConfig = false;
        XmlUtils.beginDocument(xmlResourceParser, "network-security-config");
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(xmlResourceParser, outerDepth)) {
            if ("base-config".equals(parser.getName())) {
                if (seenBaseConfig) {
                    throw new ParserException(xmlResourceParser, "Only one base-config allowed");
                }
                seenBaseConfig = true;
                baseConfigBuilder = ((Pair) parseConfigEntry(xmlResourceParser, seenDomains, null, 0).get(0)).first;
            } else if ("domain-config".equals(parser.getName())) {
                builders.addAll(parseConfigEntry(xmlResourceParser, seenDomains, baseConfigBuilder, 1));
            } else if (!"debug-overrides".equals(parser.getName())) {
                XmlUtils.skipCurrentTag(parser);
            } else if (seenDebugOverrides) {
                throw new ParserException(xmlResourceParser, "Only one debug-overrides allowed");
            } else {
                if (this.mDebugBuild) {
                    debugConfigBuilder = ((Pair) parseConfigEntry(xmlResourceParser, null, null, 2).get(0)).first;
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
                seenDebugOverrides = true;
            }
        }
        if (this.mDebugBuild && debugConfigBuilder == null) {
            debugConfigBuilder = parseDebugOverridesResource();
        }
        Builder platformDefaultBuilder = NetworkSecurityConfig.getDefaultBuilder(this.mApplicationInfo);
        addDebugAnchorsIfNeeded(debugConfigBuilder, platformDefaultBuilder);
        if (baseConfigBuilder != null) {
            baseConfigBuilder.setParent(platformDefaultBuilder);
            addDebugAnchorsIfNeeded(debugConfigBuilder, baseConfigBuilder);
        } else {
            baseConfigBuilder = platformDefaultBuilder;
        }
        Set<Pair<Domain, NetworkSecurityConfig>> configs = new ArraySet();
        for (Pair<Builder, Set<Domain>> entry : builders) {
            Builder builder = entry.first;
            Set<Domain> domains = entry.second;
            if (builder.getParent() == null) {
                builder.setParent(baseConfigBuilder);
            }
            addDebugAnchorsIfNeeded(debugConfigBuilder, builder);
            NetworkSecurityConfig config = builder.build();
            Iterator it = domains.iterator();
            while (it.hasNext()) {
                Iterator it2 = it;
                Set<String> seenDomains2 = seenDomains;
                configs.add(new Pair((Domain) it.next(), config));
                it = it2;
                seenDomains = seenDomains2;
            }
            xmlResourceParser = parser;
        }
        this.mDefaultConfig = baseConfigBuilder.build();
        this.mDomainMap = configs;
    }

    private Builder parseDebugOverridesResource() throws IOException, XmlPullParserException, ParserException {
        Throwable debugConfigBuilder;
        Resources resources = this.mContext.getResources();
        String packageName = resources.getResourcePackageName(this.mResourceId);
        String entryName = resources.getResourceEntryName(this.mResourceId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(entryName);
        stringBuilder.append("_debug");
        int resId = resources.getIdentifier(stringBuilder.toString(), "xml", packageName);
        Throwable th = null;
        if (resId == 0) {
            return null;
        }
        Builder debugConfigBuilder2 = null;
        XmlResourceParser parser = resources.getXml(resId);
        try {
            XmlUtils.beginDocument(parser, "network-security-config");
            int outerDepth = parser.getDepth();
            Builder debugConfigBuilder3 = null;
            boolean seenDebugOverrides = false;
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                try {
                    if (!"debug-overrides".equals(parser.getName())) {
                        XmlUtils.skipCurrentTag(parser);
                    } else if (seenDebugOverrides) {
                        throw new ParserException(parser, "Only one debug-overrides allowed");
                    } else {
                        if (this.mDebugBuild) {
                            debugConfigBuilder3 = (Builder) ((Pair) parseConfigEntry(parser, null, null, 2).get(0)).first;
                        } else {
                            XmlUtils.skipCurrentTag(parser);
                        }
                        seenDebugOverrides = true;
                    }
                } catch (Throwable th2) {
                    debugConfigBuilder = th2;
                    if (parser != null) {
                        $closeResource(th, parser);
                    }
                    throw debugConfigBuilder;
                }
            }
            if (parser != null) {
                $closeResource(null, parser);
            }
            return debugConfigBuilder3;
        } catch (Throwable th3) {
            th = th3;
            throw th;
        }
    }
}
