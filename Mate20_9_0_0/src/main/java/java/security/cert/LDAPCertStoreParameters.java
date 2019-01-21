package java.security.cert;

public class LDAPCertStoreParameters implements CertStoreParameters {
    private static final int LDAP_DEFAULT_PORT = 389;
    private int port;
    private String serverName;

    public LDAPCertStoreParameters(String serverName, int port) {
        if (serverName != null) {
            this.serverName = serverName;
            this.port = port;
            return;
        }
        throw new NullPointerException();
    }

    public LDAPCertStoreParameters(String serverName) {
        this(serverName, LDAP_DEFAULT_PORT);
    }

    public LDAPCertStoreParameters() {
        this("localhost", LDAP_DEFAULT_PORT);
    }

    public String getServerName() {
        return this.serverName;
    }

    public int getPort() {
        return this.port;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("LDAPCertStoreParameters: [\n");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  serverName: ");
        stringBuilder.append(this.serverName);
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  port: ");
        stringBuilder.append(this.port);
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        sb.append("]");
        return sb.toString();
    }
}
