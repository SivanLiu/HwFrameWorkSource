package java.net;

public class Proxy {
    public static final Proxy NO_PROXY = new Proxy();
    private SocketAddress sa;
    private Type type;

    public enum Type {
        DIRECT,
        HTTP,
        SOCKS
    }

    private Proxy() {
        this.type = Type.DIRECT;
        this.sa = null;
    }

    public Proxy(Type type, SocketAddress sa) {
        if (type == Type.DIRECT || !(sa instanceof InetSocketAddress)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("type ");
            stringBuilder.append((Object) type);
            stringBuilder.append(" is not compatible with address ");
            stringBuilder.append((Object) sa);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.type = type;
        this.sa = sa;
    }

    public Type type() {
        return this.type;
    }

    public SocketAddress address() {
        return this.sa;
    }

    public String toString() {
        if (type() == Type.DIRECT) {
            return "DIRECT";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type());
        stringBuilder.append(" @ ");
        stringBuilder.append(address());
        return stringBuilder.toString();
    }

    public final boolean equals(Object obj) {
        boolean z = false;
        if (obj == null || !(obj instanceof Proxy)) {
            return false;
        }
        Proxy p = (Proxy) obj;
        if (p.type() != type()) {
            return false;
        }
        if (address() != null) {
            return address().equals(p.address());
        }
        if (p.address() == null) {
            z = true;
        }
        return z;
    }

    public final int hashCode() {
        if (address() == null) {
            return type().hashCode();
        }
        return type().hashCode() + address().hashCode();
    }
}
