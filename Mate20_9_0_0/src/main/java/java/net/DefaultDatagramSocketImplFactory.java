package java.net;

import java.io.PrintStream;
import java.security.AccessController;
import sun.security.action.GetPropertyAction;

class DefaultDatagramSocketImplFactory {
    static Class<?> prefixImplClass;

    DefaultDatagramSocketImplFactory() {
    }

    static {
        prefixImplClass = null;
        String prefix = null;
        try {
            prefix = (String) AccessController.doPrivileged(new GetPropertyAction("impl.prefix", null));
            if (prefix != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("java.net.");
                stringBuilder.append(prefix);
                stringBuilder.append("DatagramSocketImpl");
                prefixImplClass = Class.forName(stringBuilder.toString());
            }
        } catch (Exception e) {
            PrintStream printStream = System.err;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Can't find class: java.net.");
            stringBuilder2.append(prefix);
            stringBuilder2.append("DatagramSocketImpl: check impl.prefix property");
            printStream.println(stringBuilder2.toString());
        }
    }

    static DatagramSocketImpl createDatagramSocketImpl(boolean isMulticast) throws SocketException {
        if (prefixImplClass == null) {
            return new PlainDatagramSocketImpl();
        }
        try {
            return (DatagramSocketImpl) prefixImplClass.newInstance();
        } catch (Exception e) {
            throw new SocketException("can't instantiate DatagramSocketImpl");
        }
    }
}
