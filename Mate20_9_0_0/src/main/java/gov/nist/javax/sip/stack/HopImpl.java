package gov.nist.javax.sip.stack;

import gov.nist.core.Separators;
import java.io.PrintStream;
import java.io.Serializable;
import javax.sip.ListeningPoint;
import javax.sip.address.Hop;

public final class HopImpl implements Hop, Serializable {
    protected boolean defaultRoute;
    protected String host;
    protected int port;
    protected String transport;
    protected boolean uriRoute;

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.host);
        stringBuilder.append(Separators.COLON);
        stringBuilder.append(this.port);
        stringBuilder.append(Separators.SLASH);
        stringBuilder.append(this.transport);
        return stringBuilder.toString();
    }

    public HopImpl(String hostName, int portNumber, String trans) {
        this.host = hostName;
        if (this.host.indexOf(Separators.COLON) >= 0 && this.host.indexOf("[") < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            stringBuilder.append(this.host);
            stringBuilder.append("]");
            this.host = stringBuilder.toString();
        }
        this.port = portNumber;
        this.transport = trans;
    }

    HopImpl(String hop) throws IllegalArgumentException {
        if (hop != null) {
            int brack = hop.indexOf(93);
            int colon = hop.indexOf(58, brack);
            int slash = hop.indexOf(47, colon);
            if (colon > 0) {
                String portstr;
                this.host = hop.substring(0, colon);
                if (slash > 0) {
                    portstr = hop.substring(colon + 1, slash);
                    this.transport = hop.substring(slash + 1);
                } else {
                    portstr = hop.substring(colon + 1);
                    this.transport = ListeningPoint.UDP;
                }
                try {
                    this.port = Integer.parseInt(portstr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Bad port spec");
                }
            }
            int i = 5060;
            if (slash > 0) {
                this.host = hop.substring(0, slash);
                this.transport = hop.substring(slash + 1);
                if (this.transport.equalsIgnoreCase(ListeningPoint.TLS)) {
                    i = 5061;
                }
                this.port = i;
            } else {
                this.host = hop;
                this.transport = ListeningPoint.UDP;
                this.port = 5060;
            }
            if (this.host == null || this.host.length() == 0) {
                throw new IllegalArgumentException("no host!");
            }
            this.host = this.host.trim();
            this.transport = this.transport.trim();
            if (brack > 0 && this.host.charAt(0) != '[') {
                throw new IllegalArgumentException("Bad IPv6 reference spec");
            } else if (this.transport.compareToIgnoreCase(ListeningPoint.UDP) != 0 && this.transport.compareToIgnoreCase(ListeningPoint.TLS) != 0 && this.transport.compareToIgnoreCase(ListeningPoint.TCP) != 0) {
                PrintStream printStream = System.err;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Bad transport string ");
                stringBuilder.append(this.transport);
                printStream.println(stringBuilder.toString());
                throw new IllegalArgumentException(hop);
            } else {
                return;
            }
        }
        throw new IllegalArgumentException("Null arg!");
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getTransport() {
        return this.transport;
    }

    public boolean isURIRoute() {
        return this.uriRoute;
    }

    public void setURIRouteFlag() {
        this.uriRoute = true;
    }
}
