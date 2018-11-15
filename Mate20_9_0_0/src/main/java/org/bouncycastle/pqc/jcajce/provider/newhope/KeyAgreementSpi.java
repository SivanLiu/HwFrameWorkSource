package org.bouncycastle.pqc.jcajce.provider.newhope;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.ShortBufferException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.jcajce.provider.asymmetric.util.BaseAgreementSpi;
import org.bouncycastle.pqc.crypto.ExchangePair;
import org.bouncycastle.pqc.crypto.newhope.NHAgreement;
import org.bouncycastle.pqc.crypto.newhope.NHExchangePairGenerator;
import org.bouncycastle.pqc.crypto.newhope.NHPublicKeyParameters;
import org.bouncycastle.util.Arrays;

public class KeyAgreementSpi extends BaseAgreementSpi {
    private NHAgreement agreement;
    private NHExchangePairGenerator exchangePairGenerator;
    private BCNHPublicKey otherPartyKey;
    private byte[] shared;

    public KeyAgreementSpi() {
        super("NH", null);
    }

    protected byte[] calcSecret() {
        return engineGenerateSecret();
    }

    protected Key engineDoPhase(Key key, boolean z) throws InvalidKeyException, IllegalStateException {
        if (z) {
            this.otherPartyKey = (BCNHPublicKey) key;
            if (this.exchangePairGenerator != null) {
                ExchangePair generateExchange = this.exchangePairGenerator.generateExchange((AsymmetricKeyParameter) this.otherPartyKey.getKeyParams());
                this.shared = generateExchange.getSharedValue();
                return new BCNHPublicKey((NHPublicKeyParameters) generateExchange.getPublicKey());
            }
            this.shared = this.agreement.calculateAgreement(this.otherPartyKey.getKeyParams());
            return null;
        }
        throw new IllegalStateException("NewHope can only be between two parties.");
    }

    protected int engineGenerateSecret(byte[] bArr, int i) throws IllegalStateException, ShortBufferException {
        System.arraycopy(this.shared, 0, bArr, i, this.shared.length);
        Arrays.fill(this.shared, (byte) 0);
        return this.shared.length;
    }

    protected byte[] engineGenerateSecret() throws IllegalStateException {
        byte[] clone = Arrays.clone(this.shared);
        Arrays.fill(this.shared, (byte) 0);
        return clone;
    }

    protected void engineInit(Key key, SecureRandom secureRandom) throws InvalidKeyException {
        if (key != null) {
            this.agreement = new NHAgreement();
            this.agreement.init(((BCNHPrivateKey) key).getKeyParams());
            return;
        }
        this.exchangePairGenerator = new NHExchangePairGenerator(secureRandom);
    }

    protected void engineInit(Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        throw new InvalidAlgorithmParameterException("NewHope does not require parameters");
    }
}
