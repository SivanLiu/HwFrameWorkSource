package com.huawei.security.keystore;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.Log;
import com.huawei.security.HwKeystoreManager;
import com.huawei.security.keymaster.HwKeymasterArguments;
import com.huawei.security.keymaster.HwKeymasterCertificateChain;
import com.huawei.security.keymaster.HwKeymasterDefs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public abstract class HwAttestationUtils {
    private static final int ATTEST_CHALLENGE_LEN_MAX = 128;
    public static final int ID_TYPE_IMEI = 2;
    public static final int ID_TYPE_MEID = 3;
    public static final int ID_TYPE_SERIAL = 1;
    private static final String TAG = "HwAttestationUtils";

    private HwAttestationUtils() {
    }

    public static X509Certificate[] attestDeviceIds(Context context, int[] idTypes, byte[] attestationChallenge) throws HwDeviceIdAttestationException {
        if (idTypes == null) {
            throw new NullPointerException("Missing id types");
        } else if (idTypes.length > 3) {
            throw new HwDeviceIdAttestationException("idTypes length is too long");
        } else if (attestationChallenge == null) {
            throw new NullPointerException("Missing attestationChallenge");
        } else if (attestationChallenge.length > 128) {
            throw new HwDeviceIdAttestationException("Attestation Challenge is too long");
        } else if ("true".equals(SystemProperties.get("ro.config.support_hwpki"))) {
            int idType;
            String meid;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("attestationChallenge length is: ");
            stringBuilder.append(attestationChallenge.length);
            stringBuilder.append("\n");
            Log.d(str, stringBuilder.toString());
            HwKeymasterArguments attestArgs = new HwKeymasterArguments();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("idTypes are");
            stringBuilder2.append(Arrays.toString(idTypes));
            Log.i(str2, stringBuilder2.toString());
            attestArgs.addBytes(HwKeymasterDefs.KM_TAG_ATTESTATION_CHALLENGE, attestationChallenge);
            Set<Integer> idTypesSet = new ArraySet(idTypes.length);
            for (int idType2 : idTypes) {
                idTypesSet.add(Integer.valueOf(idType2));
            }
            TelephonyManager telephonyService = null;
            if (idTypesSet.contains(Integer.valueOf(2)) || idTypesSet.contains(Integer.valueOf(3))) {
                telephonyService = (TelephonyManager) context.getSystemService("phone");
                if (telephonyService == null) {
                    throw new HwDeviceIdAttestationException("Unable to access telephony service");
                }
            }
            for (Integer intValue : idTypesSet) {
                idType2 = intValue.intValue();
                switch (idType2) {
                    case 1:
                        attestArgs.addBytes(HwKeymasterDefs.KM_TAG_ATTESTATION_ID_SERIAL, Build.getSerial().getBytes(StandardCharsets.UTF_8));
                        break;
                    case 2:
                        int i = 0;
                        while (true) {
                            String imei = telephonyService.getImei(i);
                            if (imei != null) {
                                attestArgs.addBytes(HwKeymasterDefs.KM_TAG_ATTESTATION_ID_IMEI, imei.getBytes(StandardCharsets.UTF_8));
                                i++;
                            } else if (i != 0) {
                                break;
                            } else {
                                throw new HwDeviceIdAttestationException("Unable to retrieve IMEI");
                            }
                        }
                    case 3:
                        meid = telephonyService.getMeid();
                        if (meid != null) {
                            attestArgs.addBytes(HwKeymasterDefs.KM_TAG_ATTESTATION_ID_MEID, meid.getBytes(StandardCharsets.UTF_8));
                            break;
                        }
                        throw new HwDeviceIdAttestationException("Unable to retrieve MEID");
                    default:
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Unknown device ID type ");
                        stringBuilder3.append(idType2);
                        throw new IllegalArgumentException(stringBuilder3.toString());
                }
            }
            attestArgs.addBytes(HwKeymasterDefs.KM_TAG_ATTESTATION_ID_BRAND, Build.BRAND.getBytes(StandardCharsets.UTF_8));
            attestArgs.addBytes(HwKeymasterDefs.KM_TAG_ATTESTATION_ID_DEVICE, Build.DEVICE.getBytes(StandardCharsets.UTF_8));
            attestArgs.addBytes(HwKeymasterDefs.KM_TAG_ATTESTATION_ID_PRODUCT, Build.PRODUCT.getBytes(StandardCharsets.UTF_8));
            attestArgs.addBytes(HwKeymasterDefs.KM_TAG_ATTESTATION_ID_MANUFACTURER, Build.MANUFACTURER.getBytes(StandardCharsets.UTF_8));
            attestArgs.addBytes(HwKeymasterDefs.KM_TAG_ATTESTATION_ID_MODEL, Build.MODEL.getBytes(StandardCharsets.UTF_8));
            Log.i(TAG, "perform ID attestation");
            HwKeymasterCertificateChain outChain = new HwKeymasterCertificateChain();
            idType2 = HwKeystoreManager.getInstance().attestDeviceIds(attestArgs, outChain);
            meid = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("errorCode is ");
            stringBuilder4.append(Integer.toString(idType2));
            Log.i(meid, stringBuilder4.toString());
            if (idType2 == 1) {
                Log.i(TAG, "Extract certificate chain.");
                Collection<byte[]> rawChain = outChain.getCertificates();
                if (rawChain.size() >= 2) {
                    ByteArrayOutputStream concatenatedRawChain = new ByteArrayOutputStream();
                    try {
                        for (byte[] cert : rawChain) {
                            concatenatedRawChain.write(cert);
                        }
                        return (X509Certificate[]) CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(concatenatedRawChain.toByteArray())).toArray(new X509Certificate[0]);
                    } catch (IOException e) {
                        Log.i(TAG, "Unable to construct certificate chain");
                        throw new HwDeviceIdAttestationException("Unable to construct certificate chain", e);
                    } catch (Exception e2) {
                        Log.i(TAG, "Unable to construct certificate chain");
                        throw new HwDeviceIdAttestationException("Unable to construct certificate chain", e2);
                    }
                }
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("Attestation certificate chain contained");
                stringBuilder5.append(rawChain.size());
                stringBuilder5.append(" entries. At least 2 are required.");
                throw new HwDeviceIdAttestationException(stringBuilder5.toString());
            }
            throw new HwDeviceIdAttestationException("Unable to perform attestation", HwKeystoreManager.getKeyStoreException(idType2));
        } else {
            throw new HwDeviceIdAttestationException("Attestation not support for this version");
        }
    }
}
