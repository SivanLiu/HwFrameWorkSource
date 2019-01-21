package sun.net;

import java.io.FileDescriptor;
import java.net.SocketOption;
import jdk.net.NetworkPermission;
import jdk.net.SocketFlow;

public class ExtendedOptionsImpl {
    private ExtendedOptionsImpl() {
    }

    public static void checkSetOptionPermission(SocketOption<?> option) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            String check = new StringBuilder();
            check.append("setOption.");
            check.append(option.name());
            sm.checkPermission(new NetworkPermission(check.toString()));
        }
    }

    public static void checkGetOptionPermission(SocketOption<?> option) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            String check = new StringBuilder();
            check.append("getOption.");
            check.append(option.name());
            sm.checkPermission(new NetworkPermission(check.toString()));
        }
    }

    public static void checkValueType(Object value, Class<?> type) {
        if (!type.isAssignableFrom(value.getClass())) {
            String s = new StringBuilder();
            s.append("Found: ");
            s.append(value.getClass().toString());
            s.append(" Expected: ");
            s.append(type.toString());
            throw new IllegalArgumentException(s.toString());
        }
    }

    public static void setFlowOption(FileDescriptor fd, SocketFlow f) {
        throw new UnsupportedOperationException("unsupported socket option");
    }

    public static void getFlowOption(FileDescriptor fd, SocketFlow f) {
        throw new UnsupportedOperationException("unsupported socket option");
    }

    public static boolean flowSupported() {
        return false;
    }
}
