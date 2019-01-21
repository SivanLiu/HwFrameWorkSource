package android.security.net.config;

import android.content.Context;
import java.security.Provider;
import java.security.Security;
import libcore.net.NetworkSecurityPolicy;

public final class NetworkSecurityConfigProvider extends Provider {
    private static final String PREFIX;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(NetworkSecurityConfigProvider.class.getPackage().getName());
        stringBuilder.append(".");
        PREFIX = stringBuilder.toString();
    }

    public NetworkSecurityConfigProvider() {
        super("AndroidNSSP", 1.0d, "Android Network Security Policy Provider");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(PREFIX);
        stringBuilder.append("RootTrustManagerFactorySpi");
        put("TrustManagerFactory.PKIX", stringBuilder.toString());
        put("Alg.Alias.TrustManagerFactory.X509", "PKIX");
    }

    public static void install(Context context) {
        ApplicationConfig config = new ApplicationConfig(new ManifestConfigSource(context));
        ApplicationConfig.setDefaultInstance(config);
        int pos = Security.insertProviderAt(new NetworkSecurityConfigProvider(), 1);
        if (pos == 1) {
            NetworkSecurityPolicy.setInstance(new ConfigNetworkSecurityPolicy(config));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to install provider as highest priority provider. Provider was installed at position ");
        stringBuilder.append(pos);
        throw new RuntimeException(stringBuilder.toString());
    }
}
