package android.security.net.config;

import com.android.internal.annotations.VisibleForTesting;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;

public class RootTrustManagerFactorySpi extends TrustManagerFactorySpi {
    private ApplicationConfig mApplicationConfig;
    private NetworkSecurityConfig mConfig;

    @VisibleForTesting
    public static final class ApplicationConfigParameters implements ManagerFactoryParameters {
        public final ApplicationConfig config;

        public ApplicationConfigParameters(ApplicationConfig config) {
            this.config = config;
        }
    }

    public void engineInit(ManagerFactoryParameters spec) throws InvalidAlgorithmParameterException {
        if (spec instanceof ApplicationConfigParameters) {
            this.mApplicationConfig = ((ApplicationConfigParameters) spec).config;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported spec: ");
        stringBuilder.append(spec);
        stringBuilder.append(". Only ");
        stringBuilder.append(ApplicationConfigParameters.class.getName());
        stringBuilder.append(" supported");
        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
    }

    public void engineInit(KeyStore ks) throws KeyStoreException {
        if (ks != null) {
            this.mApplicationConfig = new ApplicationConfig(new KeyStoreConfigSource(ks));
        } else {
            this.mApplicationConfig = ApplicationConfig.getDefaultInstance();
        }
    }

    public TrustManager[] engineGetTrustManagers() {
        if (this.mApplicationConfig != null) {
            return new TrustManager[]{this.mApplicationConfig.getTrustManager()};
        }
        throw new IllegalStateException("TrustManagerFactory not initialized");
    }
}
