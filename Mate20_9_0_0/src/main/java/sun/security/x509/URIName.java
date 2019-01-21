package sun.security.x509;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class URIName implements GeneralNameInterface {
    private String host;
    private DNSName hostDNS;
    private IPAddressName hostIP;
    private URI uri;

    public URIName(DerValue derValue) throws IOException {
        this(derValue.getIA5String());
    }

    public URIName(String name) throws IOException {
        StringBuilder stringBuilder;
        try {
            this.uri = new URI(name);
            if (this.uri.getScheme() != null) {
                this.host = this.uri.getHost();
                if (this.host == null) {
                    return;
                }
                if (this.host.charAt(0) == '[') {
                    try {
                        this.hostIP = new IPAddressName(this.host.substring(1, this.host.length() - 1));
                        return;
                    } catch (IOException e) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("invalid URI name (host portion is not a valid IPv6 address):");
                        stringBuilder.append(name);
                        throw new IOException(stringBuilder.toString());
                    }
                }
                try {
                    this.hostDNS = new DNSName(this.host);
                    return;
                } catch (IOException e2) {
                    try {
                        this.hostIP = new IPAddressName(this.host);
                        return;
                    } catch (Exception e3) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("invalid URI name (host portion is not a valid DNS name, IPv4 address, or IPv6 address):");
                        stringBuilder.append(name);
                        throw new IOException(stringBuilder.toString());
                    }
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("URI name must include scheme:");
            stringBuilder2.append(name);
            throw new IOException(stringBuilder2.toString());
        } catch (URISyntaxException use) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("invalid URI name:");
            stringBuilder3.append(name);
            throw new IOException(stringBuilder3.toString(), use);
        }
    }

    public static URIName nameConstraint(DerValue value) throws IOException {
        String name = value.getIA5String();
        StringBuilder stringBuilder;
        try {
            URI uri = new URI(name);
            if (uri.getScheme() == null) {
                String host = uri.getSchemeSpecificPart();
                try {
                    DNSName hostDNS;
                    if (host.startsWith(".")) {
                        hostDNS = new DNSName(host.substring(1));
                    } else {
                        hostDNS = new DNSName(host);
                    }
                    return new URIName(uri, host, hostDNS);
                } catch (IOException ioe) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("invalid URI name constraint:");
                    stringBuilder2.append(name);
                    throw new IOException(stringBuilder2.toString(), ioe);
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("invalid URI name constraint (should not include scheme):");
            stringBuilder.append(name);
            throw new IOException(stringBuilder.toString());
        } catch (URISyntaxException use) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("invalid URI name constraint:");
            stringBuilder.append(name);
            throw new IOException(stringBuilder.toString(), use);
        }
    }

    URIName(URI uri, String host, DNSName hostDNS) {
        this.uri = uri;
        this.host = host;
        this.hostDNS = hostDNS;
    }

    public int getType() {
        return 6;
    }

    public void encode(DerOutputStream out) throws IOException {
        out.putIA5String(this.uri.toASCIIString());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("URIName: ");
        stringBuilder.append(this.uri.toString());
        return stringBuilder.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof URIName)) {
            return false;
        }
        return this.uri.equals(((URIName) obj).getURI());
    }

    public URI getURI() {
        return this.uri;
    }

    public String getName() {
        return this.uri.toString();
    }

    public String getScheme() {
        return this.uri.getScheme();
    }

    public String getHost() {
        return this.host;
    }

    public Object getHostObject() {
        if (this.hostIP != null) {
            return this.hostIP;
        }
        return this.hostDNS;
    }

    public int hashCode() {
        return this.uri.hashCode();
    }

    public int constrains(GeneralNameInterface inputName) throws UnsupportedOperationException {
        if (inputName == null) {
            return -1;
        }
        if (inputName.getType() != 6) {
            return -1;
        }
        String otherHost = ((URIName) inputName).getHost();
        if (otherHost.equalsIgnoreCase(this.host)) {
            return 0;
        }
        DNSName otherHostObject = ((URIName) inputName).getHostObject();
        if (this.hostDNS == null || !(otherHostObject instanceof DNSName)) {
            return 3;
        }
        int i;
        boolean otherDomain = false;
        boolean thisDomain = this.host.charAt(0) == '.';
        if (otherHost.charAt(0) == '.') {
            otherDomain = true;
        }
        int constraintType = this.hostDNS.constrains(otherHostObject);
        if (thisDomain || otherDomain || !(constraintType == 2 || constraintType == 1)) {
            i = constraintType;
        } else {
            i = 3;
        }
        if (thisDomain != otherDomain && r5 == 0) {
            if (!thisDomain) {
                return 1;
            }
            i = 2;
        }
        return i;
    }

    public int subtreeDepth() throws UnsupportedOperationException {
        try {
            return new DNSName(this.host).subtreeDepth();
        } catch (IOException ioe) {
            throw new UnsupportedOperationException(ioe.getMessage());
        }
    }
}
