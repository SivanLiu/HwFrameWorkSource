package android.security.keystore;

import android.annotation.SystemApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyStore;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keymaster.KeymasterDefs;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;

@SystemApi
public abstract class AttestationUtils {
    public static final int ID_TYPE_IMEI = 2;
    public static final int ID_TYPE_MEID = 3;
    public static final int ID_TYPE_SERIAL = 1;

    private AttestationUtils() {
    }

    public static X509Certificate[] parseCertificateChain(KeymasterCertificateChain kmChain) throws KeyAttestationException {
        Collection<byte[]> rawChain = kmChain.getCertificates();
        if (rawChain.size() >= 2) {
            ByteArrayOutputStream concatenatedRawChain = new ByteArrayOutputStream();
            try {
                for (byte[] cert : rawChain) {
                    concatenatedRawChain.write(cert);
                }
                return (X509Certificate[]) CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(concatenatedRawChain.toByteArray())).toArray(new X509Certificate[0]);
            } catch (Exception e) {
                throw new KeyAttestationException("Unable to construct certificate chain", e);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attestation certificate chain contained ");
        stringBuilder.append(rawChain.size());
        stringBuilder.append(" entries. At least two are required.");
        throw new KeyAttestationException(stringBuilder.toString());
    }

    private static KeymasterArguments prepareAttestationArgumentsForDeviceId(Context context, int[] idTypes, byte[] attestationChallenge) throws DeviceIdAttestationException {
        if (idTypes != null) {
            return prepareAttestationArguments(context, idTypes, attestationChallenge);
        }
        throw new NullPointerException("Missing id types");
    }

    public static KeymasterArguments prepareAttestationArguments(Context context, int[] idTypes, byte[] attestationChallenge) throws DeviceIdAttestationException {
        if (attestationChallenge != null) {
            KeymasterArguments attestArgs = new KeymasterArguments();
            attestArgs.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_CHALLENGE, attestationChallenge);
            if (idTypes == null) {
                return attestArgs;
            }
            Set<Integer> idTypesSet = new ArraySet(idTypes.length);
            for (int idType : idTypes) {
                idTypesSet.add(Integer.valueOf(idType));
            }
            TelephonyManager telephonyService = null;
            if (idTypesSet.contains(Integer.valueOf(2)) || idTypesSet.contains(Integer.valueOf(3))) {
                telephonyService = (TelephonyManager) context.getSystemService("phone");
                if (telephonyService == null) {
                    throw new DeviceIdAttestationException("Unable to access telephony service");
                }
            }
            for (Integer idType2 : idTypesSet) {
                String imei;
                switch (idType2.intValue()) {
                    case 1:
                        attestArgs.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_SERIAL, Build.getSerial().getBytes(StandardCharsets.UTF_8));
                        break;
                    case 2:
                        imei = telephonyService.getImei(0);
                        if (imei != null) {
                            attestArgs.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_IMEI, imei.getBytes(StandardCharsets.UTF_8));
                            break;
                        }
                        throw new DeviceIdAttestationException("Unable to retrieve IMEI");
                    case 3:
                        imei = telephonyService.getMeid(0);
                        if (imei != null) {
                            attestArgs.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_MEID, imei.getBytes(StandardCharsets.UTF_8));
                            break;
                        }
                        throw new DeviceIdAttestationException("Unable to retrieve MEID");
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown device ID type ");
                        stringBuilder.append(idType2);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            attestArgs.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_BRAND, Build.BRAND.getBytes(StandardCharsets.UTF_8));
            attestArgs.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_DEVICE, Build.DEVICE.getBytes(StandardCharsets.UTF_8));
            attestArgs.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_PRODUCT, Build.PRODUCT.getBytes(StandardCharsets.UTF_8));
            attestArgs.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_MANUFACTURER, Build.MANUFACTURER.getBytes(StandardCharsets.UTF_8));
            attestArgs.addBytes(KeymasterDefs.KM_TAG_ATTESTATION_ID_MODEL, Build.MODEL.getBytes(StandardCharsets.UTF_8));
            return attestArgs;
        }
        throw new NullPointerException("Missing attestation challenge");
    }

    public static X509Certificate[] attestDeviceIds(Context context, int[] idTypes, byte[] attestationChallenge) throws DeviceIdAttestationException {
        KeymasterArguments attestArgs = prepareAttestationArgumentsForDeviceId(context, idTypes, attestationChallenge);
        KeymasterCertificateChain outChain = new KeymasterCertificateChain();
        int errorCode = KeyStore.getInstance().attestDeviceIds(attestArgs, outChain);
        if (errorCode == 1) {
            try {
                return parseCertificateChain(outChain);
            } catch (KeyAttestationException e) {
                throw new DeviceIdAttestationException(e.getMessage(), e);
            }
        }
        throw new DeviceIdAttestationException("Unable to perform attestation", KeyStore.getKeyStoreException(errorCode));
    }

    public static boolean isChainValid(KeymasterCertificateChain chain) {
        return chain != null && chain.getCertificates().size() >= 2;
    }
}
