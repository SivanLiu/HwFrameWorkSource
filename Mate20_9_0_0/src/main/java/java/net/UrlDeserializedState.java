package java.net;

/* compiled from: URL */
final class UrlDeserializedState {
    private final String authority;
    private final String file;
    private final int hashCode;
    private final String host;
    private final int port;
    private final String protocol;
    private final String ref;

    public UrlDeserializedState(String protocol, String host, int port, String authority, String file, String ref, int hashCode) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.authority = authority;
        this.file = file;
        this.ref = ref;
        this.hashCode = hashCode;
    }

    String getProtocol() {
        return this.protocol;
    }

    String getHost() {
        return this.host;
    }

    String getAuthority() {
        return this.authority;
    }

    int getPort() {
        return this.port;
    }

    String getFile() {
        return this.file;
    }

    String getRef() {
        return this.ref;
    }

    int getHashCode() {
        return this.hashCode;
    }

    String reconstituteUrlString() {
        int len = this.protocol.length() + 1;
        if (this.authority != null && this.authority.length() > 0) {
            len += 2 + this.authority.length();
        }
        if (this.file != null) {
            len += this.file.length();
        }
        if (this.ref != null) {
            len += 1 + this.ref.length();
        }
        StringBuilder result = new StringBuilder(len);
        result.append(this.protocol);
        result.append(":");
        if (this.authority != null && this.authority.length() > 0) {
            result.append("//");
            result.append(this.authority);
        }
        if (this.file != null) {
            result.append(this.file);
        }
        if (this.ref != null) {
            result.append("#");
            result.append(this.ref);
        }
        return result.toString();
    }
}
