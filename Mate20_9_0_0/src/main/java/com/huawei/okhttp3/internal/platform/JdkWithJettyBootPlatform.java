package com.huawei.okhttp3.internal.platform;

import com.huawei.okhttp3.Protocol;
import com.huawei.okhttp3.internal.Util;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import javax.net.ssl.SSLSocket;

class JdkWithJettyBootPlatform extends Platform {
    private final Class<?> clientProviderClass;
    private final Method getMethod;
    private final Method putMethod;
    private final Method removeMethod;
    private final Class<?> serverProviderClass;

    private static class JettyNegoProvider implements InvocationHandler {
        private final List<String> protocols;
        String selected;
        boolean unsupported;

        public JettyNegoProvider(List<String> protocols) {
            this.protocols = protocols;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();
            if (args == null) {
                args = Util.EMPTY_STRING_ARRAY;
            }
            if (methodName.equals("supports") && Boolean.TYPE == returnType) {
                return Boolean.valueOf(true);
            }
            if (methodName.equals("unsupported") && Void.TYPE == returnType) {
                this.unsupported = true;
                return null;
            } else if (methodName.equals("protocols") && args.length == 0) {
                return this.protocols;
            } else {
                if ((methodName.equals("selectProtocol") || methodName.equals("select")) && String.class == returnType && args.length == 1 && (args[0] instanceof List)) {
                    List<String> peerProtocols = args[0];
                    int size = peerProtocols.size();
                    for (int i = 0; i < size; i++) {
                        if (this.protocols.contains(peerProtocols.get(i))) {
                            String str = (String) peerProtocols.get(i);
                            this.selected = str;
                            return str;
                        }
                    }
                    String str2 = (String) this.protocols.get(0);
                    this.selected = str2;
                    return str2;
                } else if ((!methodName.equals("protocolSelected") && !methodName.equals("selected")) || args.length != 1) {
                    return method.invoke(this, args);
                } else {
                    this.selected = (String) args[0];
                    return null;
                }
            }
        }
    }

    public JdkWithJettyBootPlatform(Method putMethod, Method getMethod, Method removeMethod, Class<?> clientProviderClass, Class<?> serverProviderClass) {
        this.putMethod = putMethod;
        this.getMethod = getMethod;
        this.removeMethod = removeMethod;
        this.clientProviderClass = clientProviderClass;
        this.serverProviderClass = serverProviderClass;
    }

    public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols) {
        List<String> names = Platform.alpnProtocolNames(protocols);
        try {
            Object provider = Proxy.newProxyInstance(Platform.class.getClassLoader(), new Class[]{this.clientProviderClass, this.serverProviderClass}, new JettyNegoProvider(names));
            this.putMethod.invoke(null, new Object[]{sslSocket, provider});
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    public void afterHandshake(SSLSocket sslSocket) {
        try {
            this.removeMethod.invoke(null, new Object[]{sslSocket});
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError();
        }
    }

    public String getSelectedProtocol(SSLSocket socket) {
        try {
            String str = null;
            JettyNegoProvider provider = (JettyNegoProvider) Proxy.getInvocationHandler(this.getMethod.invoke(null, new Object[]{socket}));
            if (provider.unsupported || provider.selected != null) {
                if (!provider.unsupported) {
                    str = provider.selected;
                }
                return str;
            }
            Platform.get().log(4, "ALPN callback dropped: HTTP/2 is disabled. Is alpn-boot on the boot class path?", null);
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError();
        }
    }

    public static Platform buildIfSupported() {
        try {
            String negoClassName = "org.eclipse.jetty.alpn.ALPN";
            Class<?> negoClass = Class.forName(negoClassName);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(negoClassName);
            stringBuilder.append("$Provider");
            Class<?> providerClass = Class.forName(stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(negoClassName);
            stringBuilder2.append("$ClientProvider");
            Class<?> clientProviderClass = Class.forName(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(negoClassName);
            stringBuilder2.append("$ServerProvider");
            Class<?> serverProviderClass = Class.forName(stringBuilder2.toString());
            return new JdkWithJettyBootPlatform(negoClass.getMethod("put", new Class[]{SSLSocket.class, providerClass}), negoClass.getMethod("get", new Class[]{SSLSocket.class}), negoClass.getMethod("remove", new Class[]{SSLSocket.class}), clientProviderClass, serverProviderClass);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return null;
        }
    }
}
