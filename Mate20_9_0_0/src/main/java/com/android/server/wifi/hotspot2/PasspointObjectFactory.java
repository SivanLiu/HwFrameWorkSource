package com.android.server.wifi.hotspot2;

import android.content.Context;
import android.net.wifi.hotspot2.PasspointConfiguration;
import com.android.org.conscrypt.TrustManagerImpl;
import com.android.server.wifi.Clock;
import com.android.server.wifi.SIMAccessor;
import com.android.server.wifi.WifiKeyStore;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.PasspointConfigStoreData.DataSource;
import com.android.server.wifi.hotspot2.PasspointEventHandler.Callbacks;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;

public class PasspointObjectFactory {
    public PasspointEventHandler makePasspointEventHandler(WifiNative wifiNative, Callbacks callbacks) {
        return new PasspointEventHandler(wifiNative, callbacks);
    }

    public PasspointProvider makePasspointProvider(PasspointConfiguration config, WifiKeyStore keyStore, SIMAccessor simAccessor, long providerId, int creatorUid) {
        return new PasspointProvider(config, keyStore, simAccessor, providerId, creatorUid);
    }

    public PasspointConfigStoreData makePasspointConfigStoreData(WifiKeyStore keyStore, SIMAccessor simAccessor, DataSource dataSource) {
        return new CustPasspointConfigStoreData(keyStore, simAccessor, dataSource);
    }

    public AnqpCache makeAnqpCache(Clock clock) {
        return new AnqpCache(clock);
    }

    public ANQPRequestManager makeANQPRequestManager(PasspointEventHandler handler, Clock clock) {
        return new ANQPRequestManager(handler, clock);
    }

    public CertificateVerifier makeCertificateVerifier() {
        return new CertificateVerifier();
    }

    public PasspointProvisioner makePasspointProvisioner(Context context) {
        return new PasspointProvisioner(context, this);
    }

    public OsuNetworkConnection makeOsuNetworkConnection(Context context) {
        return new OsuNetworkConnection(context);
    }

    public OsuServerConnection makeOsuServerConnection() {
        return new OsuServerConnection();
    }

    public WfaKeyStore makeWfaKeyStore() {
        return new WfaKeyStore();
    }

    public SSLContext getSSLContext(String tlsVersion) {
        try {
            return SSLContext.getInstance(tlsVersion);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public TrustManagerImpl getTrustManagerImpl(KeyStore ks) {
        return new TrustManagerImpl(ks);
    }
}
