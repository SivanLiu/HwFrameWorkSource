package org.bouncycastle.crypto.engines;

import java.lang.reflect.Array;
import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.crypto.tls.CipherSuite;

public class RijndaelEngine implements BlockCipher {
    private static final int MAXKC = 64;
    private static final int MAXROUNDS = 14;
    private static final byte[] S = new byte[]{(byte) 99, (byte) 124, (byte) 119, (byte) 123, (byte) -14, (byte) 107, (byte) 111, (byte) -59, (byte) 48, (byte) 1, (byte) 103, (byte) 43, (byte) -2, (byte) -41, (byte) -85, (byte) 118, (byte) -54, (byte) -126, (byte) -55, (byte) 125, (byte) -6, (byte) 89, (byte) 71, (byte) -16, (byte) -83, (byte) -44, (byte) -94, (byte) -81, (byte) -100, (byte) -92, (byte) 114, (byte) -64, (byte) -73, (byte) -3, (byte) -109, (byte) 38, (byte) 54, (byte) 63, (byte) -9, (byte) -52, (byte) 52, (byte) -91, (byte) -27, (byte) -15, (byte) 113, (byte) -40, (byte) 49, (byte) 21, (byte) 4, (byte) -57, (byte) 35, (byte) -61, (byte) 24, (byte) -106, (byte) 5, (byte) -102, (byte) 7, (byte) 18, Byte.MIN_VALUE, (byte) -30, (byte) -21, (byte) 39, (byte) -78, (byte) 117, (byte) 9, (byte) -125, (byte) 44, (byte) 26, (byte) 27, (byte) 110, (byte) 90, (byte) -96, (byte) 82, (byte) 59, (byte) -42, (byte) -77, (byte) 41, (byte) -29, (byte) 47, (byte) -124, (byte) 83, (byte) -47, (byte) 0, (byte) -19, (byte) 32, (byte) -4, (byte) -79, (byte) 91, (byte) 106, (byte) -53, (byte) -66, (byte) 57, (byte) 74, (byte) 76, (byte) 88, (byte) -49, (byte) -48, (byte) -17, (byte) -86, (byte) -5, (byte) 67, (byte) 77, (byte) 51, (byte) -123, (byte) 69, (byte) -7, (byte) 2, Byte.MAX_VALUE, (byte) 80, (byte) 60, (byte) -97, (byte) -88, (byte) 81, (byte) -93, (byte) 64, (byte) -113, (byte) -110, (byte) -99, (byte) 56, (byte) -11, PSSSigner.TRAILER_IMPLICIT, (byte) -74, (byte) -38, (byte) 33, Tnaf.POW_2_WIDTH, (byte) -1, (byte) -13, (byte) -46, (byte) -51, (byte) 12, (byte) 19, (byte) -20, (byte) 95, (byte) -105, (byte) 68, (byte) 23, (byte) -60, (byte) -89, (byte) 126, (byte) 61, (byte) 100, (byte) 93, (byte) 25, (byte) 115, (byte) 96, (byte) -127, (byte) 79, (byte) -36, (byte) 34, (byte) 42, (byte) -112, (byte) -120, (byte) 70, (byte) -18, (byte) -72, (byte) 20, (byte) -34, (byte) 94, (byte) 11, (byte) -37, (byte) -32, (byte) 50, (byte) 58, (byte) 10, (byte) 73, (byte) 6, (byte) 36, (byte) 92, (byte) -62, (byte) -45, (byte) -84, (byte) 98, (byte) -111, (byte) -107, (byte) -28, (byte) 121, (byte) -25, (byte) -56, (byte) 55, (byte) 109, (byte) -115, (byte) -43, (byte) 78, (byte) -87, (byte) 108, (byte) 86, (byte) -12, (byte) -22, (byte) 101, (byte) 122, (byte) -82, (byte) 8, (byte) -70, (byte) 120, (byte) 37, (byte) 46, (byte) 28, (byte) -90, (byte) -76, (byte) -58, (byte) -24, (byte) -35, (byte) 116, (byte) 31, (byte) 75, (byte) -67, (byte) -117, (byte) -118, (byte) 112, (byte) 62, (byte) -75, (byte) 102, (byte) 72, (byte) 3, (byte) -10, (byte) 14, (byte) 97, (byte) 53, (byte) 87, (byte) -71, (byte) -122, (byte) -63, (byte) 29, (byte) -98, (byte) -31, (byte) -8, (byte) -104, (byte) 17, (byte) 105, (byte) -39, (byte) -114, (byte) -108, (byte) -101, (byte) 30, (byte) -121, (byte) -23, (byte) -50, (byte) 85, (byte) 40, (byte) -33, (byte) -116, (byte) -95, (byte) -119, (byte) 13, (byte) -65, (byte) -26, (byte) 66, (byte) 104, (byte) 65, (byte) -103, (byte) 45, (byte) 15, (byte) -80, (byte) 84, (byte) -69, (byte) 22};
    private static final byte[] Si = new byte[]{(byte) 82, (byte) 9, (byte) 106, (byte) -43, (byte) 48, (byte) 54, (byte) -91, (byte) 56, (byte) -65, (byte) 64, (byte) -93, (byte) -98, (byte) -127, (byte) -13, (byte) -41, (byte) -5, (byte) 124, (byte) -29, (byte) 57, (byte) -126, (byte) -101, (byte) 47, (byte) -1, (byte) -121, (byte) 52, (byte) -114, (byte) 67, (byte) 68, (byte) -60, (byte) -34, (byte) -23, (byte) -53, (byte) 84, (byte) 123, (byte) -108, (byte) 50, (byte) -90, (byte) -62, (byte) 35, (byte) 61, (byte) -18, (byte) 76, (byte) -107, (byte) 11, (byte) 66, (byte) -6, (byte) -61, (byte) 78, (byte) 8, (byte) 46, (byte) -95, (byte) 102, (byte) 40, (byte) -39, (byte) 36, (byte) -78, (byte) 118, (byte) 91, (byte) -94, (byte) 73, (byte) 109, (byte) -117, (byte) -47, (byte) 37, (byte) 114, (byte) -8, (byte) -10, (byte) 100, (byte) -122, (byte) 104, (byte) -104, (byte) 22, (byte) -44, (byte) -92, (byte) 92, (byte) -52, (byte) 93, (byte) 101, (byte) -74, (byte) -110, (byte) 108, (byte) 112, (byte) 72, (byte) 80, (byte) -3, (byte) -19, (byte) -71, (byte) -38, (byte) 94, (byte) 21, (byte) 70, (byte) 87, (byte) -89, (byte) -115, (byte) -99, (byte) -124, (byte) -112, (byte) -40, (byte) -85, (byte) 0, (byte) -116, PSSSigner.TRAILER_IMPLICIT, (byte) -45, (byte) 10, (byte) -9, (byte) -28, (byte) 88, (byte) 5, (byte) -72, (byte) -77, (byte) 69, (byte) 6, (byte) -48, (byte) 44, (byte) 30, (byte) -113, (byte) -54, (byte) 63, (byte) 15, (byte) 2, (byte) -63, (byte) -81, (byte) -67, (byte) 3, (byte) 1, (byte) 19, (byte) -118, (byte) 107, (byte) 58, (byte) -111, (byte) 17, (byte) 65, (byte) 79, (byte) 103, (byte) -36, (byte) -22, (byte) -105, (byte) -14, (byte) -49, (byte) -50, (byte) -16, (byte) -76, (byte) -26, (byte) 115, (byte) -106, (byte) -84, (byte) 116, (byte) 34, (byte) -25, (byte) -83, (byte) 53, (byte) -123, (byte) -30, (byte) -7, (byte) 55, (byte) -24, (byte) 28, (byte) 117, (byte) -33, (byte) 110, (byte) 71, (byte) -15, (byte) 26, (byte) 113, (byte) 29, (byte) 41, (byte) -59, (byte) -119, (byte) 111, (byte) -73, (byte) 98, (byte) 14, (byte) -86, (byte) 24, (byte) -66, (byte) 27, (byte) -4, (byte) 86, (byte) 62, (byte) 75, (byte) -58, (byte) -46, (byte) 121, (byte) 32, (byte) -102, (byte) -37, (byte) -64, (byte) -2, (byte) 120, (byte) -51, (byte) 90, (byte) -12, (byte) 31, (byte) -35, (byte) -88, (byte) 51, (byte) -120, (byte) 7, (byte) -57, (byte) 49, (byte) -79, (byte) 18, Tnaf.POW_2_WIDTH, (byte) 89, (byte) 39, Byte.MIN_VALUE, (byte) -20, (byte) 95, (byte) 96, (byte) 81, Byte.MAX_VALUE, (byte) -87, (byte) 25, (byte) -75, (byte) 74, (byte) 13, (byte) 45, (byte) -27, (byte) 122, (byte) -97, (byte) -109, (byte) -55, (byte) -100, (byte) -17, (byte) -96, (byte) -32, (byte) 59, (byte) 77, (byte) -82, (byte) 42, (byte) -11, (byte) -80, (byte) -56, (byte) -21, (byte) -69, (byte) 60, (byte) -125, (byte) 83, (byte) -103, (byte) 97, (byte) 23, (byte) 43, (byte) 4, (byte) 126, (byte) -70, (byte) 119, (byte) -42, (byte) 38, (byte) -31, (byte) 105, (byte) 20, (byte) 99, (byte) 85, (byte) 33, (byte) 12, (byte) 125};
    private static final byte[] aLogtable = new byte[]{(byte) 0, (byte) 3, (byte) 5, (byte) 15, (byte) 17, (byte) 51, (byte) 85, (byte) -1, (byte) 26, (byte) 46, (byte) 114, (byte) -106, (byte) -95, (byte) -8, (byte) 19, (byte) 53, (byte) 95, (byte) -31, (byte) 56, (byte) 72, (byte) -40, (byte) 115, (byte) -107, (byte) -92, (byte) -9, (byte) 2, (byte) 6, (byte) 10, (byte) 30, (byte) 34, (byte) 102, (byte) -86, (byte) -27, (byte) 52, (byte) 92, (byte) -28, (byte) 55, (byte) 89, (byte) -21, (byte) 38, (byte) 106, (byte) -66, (byte) -39, (byte) 112, (byte) -112, (byte) -85, (byte) -26, (byte) 49, (byte) 83, (byte) -11, (byte) 4, (byte) 12, (byte) 20, (byte) 60, (byte) 68, (byte) -52, (byte) 79, (byte) -47, (byte) 104, (byte) -72, (byte) -45, (byte) 110, (byte) -78, (byte) -51, (byte) 76, (byte) -44, (byte) 103, (byte) -87, (byte) -32, (byte) 59, (byte) 77, (byte) -41, (byte) 98, (byte) -90, (byte) -15, (byte) 8, (byte) 24, (byte) 40, (byte) 120, (byte) -120, (byte) -125, (byte) -98, (byte) -71, (byte) -48, (byte) 107, (byte) -67, (byte) -36, Byte.MAX_VALUE, (byte) -127, (byte) -104, (byte) -77, (byte) -50, (byte) 73, (byte) -37, (byte) 118, (byte) -102, (byte) -75, (byte) -60, (byte) 87, (byte) -7, Tnaf.POW_2_WIDTH, (byte) 48, (byte) 80, (byte) -16, (byte) 11, (byte) 29, (byte) 39, (byte) 105, (byte) -69, (byte) -42, (byte) 97, (byte) -93, (byte) -2, (byte) 25, (byte) 43, (byte) 125, (byte) -121, (byte) -110, (byte) -83, (byte) -20, (byte) 47, (byte) 113, (byte) -109, (byte) -82, (byte) -23, (byte) 32, (byte) 96, (byte) -96, (byte) -5, (byte) 22, (byte) 58, (byte) 78, (byte) -46, (byte) 109, (byte) -73, (byte) -62, (byte) 93, (byte) -25, (byte) 50, (byte) 86, (byte) -6, (byte) 21, (byte) 63, (byte) 65, (byte) -61, (byte) 94, (byte) -30, (byte) 61, (byte) 71, (byte) -55, (byte) 64, (byte) -64, (byte) 91, (byte) -19, (byte) 44, (byte) 116, (byte) -100, (byte) -65, (byte) -38, (byte) 117, (byte) -97, (byte) -70, (byte) -43, (byte) 100, (byte) -84, (byte) -17, (byte) 42, (byte) 126, (byte) -126, (byte) -99, PSSSigner.TRAILER_IMPLICIT, (byte) -33, (byte) 122, (byte) -114, (byte) -119, Byte.MIN_VALUE, (byte) -101, (byte) -74, (byte) -63, (byte) 88, (byte) -24, (byte) 35, (byte) 101, (byte) -81, (byte) -22, (byte) 37, (byte) 111, (byte) -79, (byte) -56, (byte) 67, (byte) -59, (byte) 84, (byte) -4, (byte) 31, (byte) 33, (byte) 99, (byte) -91, (byte) -12, (byte) 7, (byte) 9, (byte) 27, (byte) 45, (byte) 119, (byte) -103, (byte) -80, (byte) -53, (byte) 70, (byte) -54, (byte) 69, (byte) -49, (byte) 74, (byte) -34, (byte) 121, (byte) -117, (byte) -122, (byte) -111, (byte) -88, (byte) -29, (byte) 62, (byte) 66, (byte) -58, (byte) 81, (byte) -13, (byte) 14, (byte) 18, (byte) 54, (byte) 90, (byte) -18, (byte) 41, (byte) 123, (byte) -115, (byte) -116, (byte) -113, (byte) -118, (byte) -123, (byte) -108, (byte) -89, (byte) -14, (byte) 13, (byte) 23, (byte) 57, (byte) 75, (byte) -35, (byte) 124, (byte) -124, (byte) -105, (byte) -94, (byte) -3, (byte) 28, (byte) 36, (byte) 108, (byte) -76, (byte) -57, (byte) 82, (byte) -10, (byte) 1, (byte) 3, (byte) 5, (byte) 15, (byte) 17, (byte) 51, (byte) 85, (byte) -1, (byte) 26, (byte) 46, (byte) 114, (byte) -106, (byte) -95, (byte) -8, (byte) 19, (byte) 53, (byte) 95, (byte) -31, (byte) 56, (byte) 72, (byte) -40, (byte) 115, (byte) -107, (byte) -92, (byte) -9, (byte) 2, (byte) 6, (byte) 10, (byte) 30, (byte) 34, (byte) 102, (byte) -86, (byte) -27, (byte) 52, (byte) 92, (byte) -28, (byte) 55, (byte) 89, (byte) -21, (byte) 38, (byte) 106, (byte) -66, (byte) -39, (byte) 112, (byte) -112, (byte) -85, (byte) -26, (byte) 49, (byte) 83, (byte) -11, (byte) 4, (byte) 12, (byte) 20, (byte) 60, (byte) 68, (byte) -52, (byte) 79, (byte) -47, (byte) 104, (byte) -72, (byte) -45, (byte) 110, (byte) -78, (byte) -51, (byte) 76, (byte) -44, (byte) 103, (byte) -87, (byte) -32, (byte) 59, (byte) 77, (byte) -41, (byte) 98, (byte) -90, (byte) -15, (byte) 8, (byte) 24, (byte) 40, (byte) 120, (byte) -120, (byte) -125, (byte) -98, (byte) -71, (byte) -48, (byte) 107, (byte) -67, (byte) -36, Byte.MAX_VALUE, (byte) -127, (byte) -104, (byte) -77, (byte) -50, (byte) 73, (byte) -37, (byte) 118, (byte) -102, (byte) -75, (byte) -60, (byte) 87, (byte) -7, Tnaf.POW_2_WIDTH, (byte) 48, (byte) 80, (byte) -16, (byte) 11, (byte) 29, (byte) 39, (byte) 105, (byte) -69, (byte) -42, (byte) 97, (byte) -93, (byte) -2, (byte) 25, (byte) 43, (byte) 125, (byte) -121, (byte) -110, (byte) -83, (byte) -20, (byte) 47, (byte) 113, (byte) -109, (byte) -82, (byte) -23, (byte) 32, (byte) 96, (byte) -96, (byte) -5, (byte) 22, (byte) 58, (byte) 78, (byte) -46, (byte) 109, (byte) -73, (byte) -62, (byte) 93, (byte) -25, (byte) 50, (byte) 86, (byte) -6, (byte) 21, (byte) 63, (byte) 65, (byte) -61, (byte) 94, (byte) -30, (byte) 61, (byte) 71, (byte) -55, (byte) 64, (byte) -64, (byte) 91, (byte) -19, (byte) 44, (byte) 116, (byte) -100, (byte) -65, (byte) -38, (byte) 117, (byte) -97, (byte) -70, (byte) -43, (byte) 100, (byte) -84, (byte) -17, (byte) 42, (byte) 126, (byte) -126, (byte) -99, PSSSigner.TRAILER_IMPLICIT, (byte) -33, (byte) 122, (byte) -114, (byte) -119, Byte.MIN_VALUE, (byte) -101, (byte) -74, (byte) -63, (byte) 88, (byte) -24, (byte) 35, (byte) 101, (byte) -81, (byte) -22, (byte) 37, (byte) 111, (byte) -79, (byte) -56, (byte) 67, (byte) -59, (byte) 84, (byte) -4, (byte) 31, (byte) 33, (byte) 99, (byte) -91, (byte) -12, (byte) 7, (byte) 9, (byte) 27, (byte) 45, (byte) 119, (byte) -103, (byte) -80, (byte) -53, (byte) 70, (byte) -54, (byte) 69, (byte) -49, (byte) 74, (byte) -34, (byte) 121, (byte) -117, (byte) -122, (byte) -111, (byte) -88, (byte) -29, (byte) 62, (byte) 66, (byte) -58, (byte) 81, (byte) -13, (byte) 14, (byte) 18, (byte) 54, (byte) 90, (byte) -18, (byte) 41, (byte) 123, (byte) -115, (byte) -116, (byte) -113, (byte) -118, (byte) -123, (byte) -108, (byte) -89, (byte) -14, (byte) 13, (byte) 23, (byte) 57, (byte) 75, (byte) -35, (byte) 124, (byte) -124, (byte) -105, (byte) -94, (byte) -3, (byte) 28, (byte) 36, (byte) 108, (byte) -76, (byte) -57, (byte) 82, (byte) -10, (byte) 1};
    private static final byte[] logtable = new byte[]{(byte) 0, (byte) 0, (byte) 25, (byte) 1, (byte) 50, (byte) 2, (byte) 26, (byte) -58, (byte) 75, (byte) -57, (byte) 27, (byte) 104, (byte) 51, (byte) -18, (byte) -33, (byte) 3, (byte) 100, (byte) 4, (byte) -32, (byte) 14, (byte) 52, (byte) -115, (byte) -127, (byte) -17, (byte) 76, (byte) 113, (byte) 8, (byte) -56, (byte) -8, (byte) 105, (byte) 28, (byte) -63, (byte) 125, (byte) -62, (byte) 29, (byte) -75, (byte) -7, (byte) -71, (byte) 39, (byte) 106, (byte) 77, (byte) -28, (byte) -90, (byte) 114, (byte) -102, (byte) -55, (byte) 9, (byte) 120, (byte) 101, (byte) 47, (byte) -118, (byte) 5, (byte) 33, (byte) 15, (byte) -31, (byte) 36, (byte) 18, (byte) -16, (byte) -126, (byte) 69, (byte) 53, (byte) -109, (byte) -38, (byte) -114, (byte) -106, (byte) -113, (byte) -37, (byte) -67, (byte) 54, (byte) -48, (byte) -50, (byte) -108, (byte) 19, (byte) 92, (byte) -46, (byte) -15, (byte) 64, (byte) 70, (byte) -125, (byte) 56, (byte) 102, (byte) -35, (byte) -3, (byte) 48, (byte) -65, (byte) 6, (byte) -117, (byte) 98, (byte) -77, (byte) 37, (byte) -30, (byte) -104, (byte) 34, (byte) -120, (byte) -111, Tnaf.POW_2_WIDTH, (byte) 126, (byte) 110, (byte) 72, (byte) -61, (byte) -93, (byte) -74, (byte) 30, (byte) 66, (byte) 58, (byte) 107, (byte) 40, (byte) 84, (byte) -6, (byte) -123, (byte) 61, (byte) -70, (byte) 43, (byte) 121, (byte) 10, (byte) 21, (byte) -101, (byte) -97, (byte) 94, (byte) -54, (byte) 78, (byte) -44, (byte) -84, (byte) -27, (byte) -13, (byte) 115, (byte) -89, (byte) 87, (byte) -81, (byte) 88, (byte) -88, (byte) 80, (byte) -12, (byte) -22, (byte) -42, (byte) 116, (byte) 79, (byte) -82, (byte) -23, (byte) -43, (byte) -25, (byte) -26, (byte) -83, (byte) -24, (byte) 44, (byte) -41, (byte) 117, (byte) 122, (byte) -21, (byte) 22, (byte) 11, (byte) -11, (byte) 89, (byte) -53, (byte) 95, (byte) -80, (byte) -100, (byte) -87, (byte) 81, (byte) -96, Byte.MAX_VALUE, (byte) 12, (byte) -10, (byte) 111, (byte) 23, (byte) -60, (byte) 73, (byte) -20, (byte) -40, (byte) 67, (byte) 31, (byte) 45, (byte) -92, (byte) 118, (byte) 123, (byte) -73, (byte) -52, (byte) -69, (byte) 62, (byte) 90, (byte) -5, (byte) 96, (byte) -79, (byte) -122, (byte) 59, (byte) 82, (byte) -95, (byte) 108, (byte) -86, (byte) 85, (byte) 41, (byte) -99, (byte) -105, (byte) -78, (byte) -121, (byte) -112, (byte) 97, (byte) -66, (byte) -36, (byte) -4, PSSSigner.TRAILER_IMPLICIT, (byte) -107, (byte) -49, (byte) -51, (byte) 55, (byte) 63, (byte) 91, (byte) -47, (byte) 83, (byte) 57, (byte) -124, (byte) 60, (byte) 65, (byte) -94, (byte) 109, (byte) 71, (byte) 20, (byte) 42, (byte) -98, (byte) 93, (byte) 86, (byte) -14, (byte) -45, (byte) -85, (byte) 68, (byte) 17, (byte) -110, (byte) -39, (byte) 35, (byte) 32, (byte) 46, (byte) -119, (byte) -76, (byte) 124, (byte) -72, (byte) 38, (byte) 119, (byte) -103, (byte) -29, (byte) -91, (byte) 103, (byte) 74, (byte) -19, (byte) -34, (byte) -59, (byte) 49, (byte) -2, (byte) 24, (byte) 13, (byte) 99, (byte) -116, Byte.MIN_VALUE, (byte) -64, (byte) -9, (byte) 112, (byte) 7};
    private static final int[] rcon = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 27, 54, CipherSuite.TLS_DH_anon_WITH_AES_128_CBC_SHA256, 216, CipherSuite.TLS_DHE_PSK_WITH_AES_256_GCM_SHA384, 77, CipherSuite.TLS_DHE_RSA_WITH_SEED_CBC_SHA, 47, 94, 188, 99, 198, CipherSuite.TLS_DH_DSS_WITH_SEED_CBC_SHA, 53, CipherSuite.TLS_DHE_DSS_WITH_AES_256_CBC_SHA256, 212, CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA384, 125, 250, 239, CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256, CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA};
    static byte[][] shifts0 = new byte[][]{new byte[]{(byte) 0, (byte) 8, Tnaf.POW_2_WIDTH, (byte) 24}, new byte[]{(byte) 0, (byte) 8, Tnaf.POW_2_WIDTH, (byte) 24}, new byte[]{(byte) 0, (byte) 8, Tnaf.POW_2_WIDTH, (byte) 24}, new byte[]{(byte) 0, (byte) 8, Tnaf.POW_2_WIDTH, (byte) 32}, new byte[]{(byte) 0, (byte) 8, (byte) 24, (byte) 32}};
    static byte[][] shifts1 = new byte[][]{new byte[]{(byte) 0, (byte) 24, Tnaf.POW_2_WIDTH, (byte) 8}, new byte[]{(byte) 0, (byte) 32, (byte) 24, Tnaf.POW_2_WIDTH}, new byte[]{(byte) 0, (byte) 40, (byte) 32, (byte) 24}, new byte[]{(byte) 0, (byte) 48, (byte) 40, (byte) 24}, new byte[]{(byte) 0, (byte) 56, (byte) 40, (byte) 32}};
    private long A0;
    private long A1;
    private long A2;
    private long A3;
    private int BC;
    private long BC_MASK;
    private int ROUNDS;
    private int blockBits;
    private boolean forEncryption;
    private byte[] shifts0SC;
    private byte[] shifts1SC;
    private long[][] workingKey;

    public RijndaelEngine() {
        this(128);
    }

    public RijndaelEngine(int i) {
        byte[] bArr;
        if (i == 128) {
            this.BC = 32;
            this.BC_MASK = BodyPartID.bodyIdMax;
            this.shifts0SC = shifts0[0];
            bArr = shifts1[0];
        } else if (i == CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256) {
            this.BC = 40;
            this.BC_MASK = 1099511627775L;
            this.shifts0SC = shifts0[1];
            bArr = shifts1[1];
        } else if (i == 192) {
            this.BC = 48;
            this.BC_MASK = 281474976710655L;
            this.shifts0SC = shifts0[2];
            bArr = shifts1[2];
        } else if (i == 224) {
            this.BC = 56;
            this.BC_MASK = 72057594037927935L;
            this.shifts0SC = shifts0[3];
            bArr = shifts1[3];
        } else if (i == 256) {
            this.BC = 64;
            this.BC_MASK = -1;
            this.shifts0SC = shifts0[4];
            bArr = shifts1[4];
        } else {
            throw new IllegalArgumentException("unknown blocksize to Rijndael");
        }
        this.shifts1SC = bArr;
        this.blockBits = i;
    }

    private void InvMixColumn() {
        long j;
        long j2 = 0;
        long j3 = 0;
        long j4 = j3;
        long j5 = j4;
        for (int i = 0; i < this.BC; i += 8) {
            int i2 = (int) ((this.A0 >> i) & 255);
            int i3 = (int) ((this.A1 >> i) & 255);
            int i4 = (int) ((this.A2 >> i) & 255);
            j = j5;
            int i5 = (int) ((this.A3 >> i) & 255);
            int i6 = -1;
            i2 = i2 != 0 ? logtable[i2 & 255] & 255 : -1;
            i3 = i3 != 0 ? logtable[i3 & 255] & 255 : -1;
            int i7 = i4 != 0 ? logtable[i4 & 255] & 255 : -1;
            if (i5 != 0) {
                i6 = logtable[i5 & 255] & 255;
            }
            j2 |= ((long) ((((mul0xe(i2) ^ mul0xb(i3)) ^ mul0xd(i7)) ^ mul0x9(i6)) & 255)) << i;
            j3 |= ((long) ((((mul0xe(i3) ^ mul0xb(i7)) ^ mul0xd(i6)) ^ mul0x9(i2)) & 255)) << i;
            j4 |= ((long) ((((mul0xe(i7) ^ mul0xb(i6)) ^ mul0xd(i2)) ^ mul0x9(i3)) & 255)) << i;
            j5 = j | (((long) ((((mul0xe(i6) ^ mul0xb(i2)) ^ mul0xd(i3)) ^ mul0x9(i7)) & 255)) << i);
        }
        j = j5;
        this.A0 = j2;
        this.A1 = j3;
        this.A2 = j4;
        this.A3 = j;
    }

    private void KeyAddition(long[] jArr) {
        this.A0 ^= jArr[0];
        this.A1 ^= jArr[1];
        this.A2 ^= jArr[2];
        this.A3 ^= jArr[3];
    }

    private void MixColumn() {
        long j;
        long j2 = 0;
        long j3 = 0;
        long j4 = j3;
        long j5 = j4;
        for (int i = 0; i < this.BC; i += 8) {
            int i2 = (int) ((this.A0 >> i) & 255);
            int i3 = (int) ((this.A1 >> i) & 255);
            int i4 = (int) ((this.A2 >> i) & 255);
            j = j5;
            int i5 = (int) ((this.A3 >> i) & 255);
            j2 |= ((long) ((((mul0x2(i2) ^ mul0x3(i3)) ^ i4) ^ i5) & 255)) << i;
            j3 |= ((long) ((((mul0x2(i3) ^ mul0x3(i4)) ^ i5) ^ i2) & 255)) << i;
            j4 |= ((long) ((((mul0x2(i4) ^ mul0x3(i5)) ^ i2) ^ i3) & 255)) << i;
            j5 = j | (((long) ((((mul0x2(i5) ^ mul0x3(i2)) ^ i3) ^ i4) & 255)) << i);
        }
        j = j5;
        this.A0 = j2;
        this.A1 = j3;
        this.A2 = j4;
        this.A3 = j;
    }

    private void ShiftRow(byte[] bArr) {
        this.A1 = shift(this.A1, bArr[1]);
        this.A2 = shift(this.A2, bArr[2]);
        this.A3 = shift(this.A3, bArr[3]);
    }

    private void Substitution(byte[] bArr) {
        this.A0 = applyS(this.A0, bArr);
        this.A1 = applyS(this.A1, bArr);
        this.A2 = applyS(this.A2, bArr);
        this.A3 = applyS(this.A3, bArr);
    }

    private long applyS(long j, byte[] bArr) {
        long j2 = 0;
        for (int i = 0; i < this.BC; i += 8) {
            j2 |= ((long) (bArr[(int) ((j >> i) & 255)] & 255)) << i;
        }
        return j2;
    }

    private void decryptBlock(long[][] jArr) {
        KeyAddition(jArr[this.ROUNDS]);
        Substitution(Si);
        ShiftRow(this.shifts1SC);
        for (int i = this.ROUNDS - 1; i > 0; i--) {
            KeyAddition(jArr[i]);
            InvMixColumn();
            Substitution(Si);
            ShiftRow(this.shifts1SC);
        }
        KeyAddition(jArr[0]);
    }

    private void encryptBlock(long[][] jArr) {
        KeyAddition(jArr[0]);
        for (int i = 1; i < this.ROUNDS; i++) {
            Substitution(S);
            ShiftRow(this.shifts0SC);
            MixColumn();
            KeyAddition(jArr[i]);
        }
        Substitution(S);
        ShiftRow(this.shifts0SC);
        KeyAddition(jArr[this.ROUNDS]);
    }

    private long[][] generateWorkingKey(byte[] bArr) {
        int i;
        byte[] bArr2 = bArr;
        int i2 = 8;
        int length = bArr2.length * 8;
        int i3 = 4;
        byte[][] bArr3 = (byte[][]) Array.newInstance(byte.class, new int[]{4, 64});
        long[][] jArr = (long[][]) Array.newInstance(long.class, new int[]{15, 4});
        if (length == 128) {
            i = 4;
        } else if (length == CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256) {
            i = 5;
        } else if (length == 192) {
            i = 6;
        } else if (length == 224) {
            i = 7;
        } else if (length == 256) {
            i = 8;
        } else {
            throw new IllegalArgumentException("Key length not 128/160/192/224/256 bits.");
        }
        this.ROUNDS = length >= this.blockBits ? i + 6 : (this.BC / 8) + 6;
        int i4 = 0;
        int i5 = 0;
        while (i4 < bArr2.length) {
            int i6 = i5 + 1;
            bArr3[i4 % 4][i4 / 4] = bArr2[i5];
            i4++;
            i5 = i6;
        }
        int i7 = 0;
        i4 = 0;
        while (true) {
            i5 = 1;
            if (i7 >= i || i4 >= (this.ROUNDS + 1) * (this.BC / 8)) {
                i7 = 0;
            } else {
                for (i5 = 0; i5 < 4; i5++) {
                    long[] jArr2 = jArr[i4 / (this.BC / 8)];
                    jArr2[i5] = (((long) (bArr3[i5][i7] & 255)) << ((i4 * 8) % this.BC)) | jArr2[i5];
                }
                i7++;
                i4++;
            }
        }
        i7 = 0;
        while (i4 < (this.ROUNDS + i5) * (this.BC / i2)) {
            byte[] bArr4;
            int i8;
            length = 0;
            while (length < i3) {
                bArr4 = bArr3[length];
                length++;
                bArr4[0] = (byte) (S[bArr3[length % 4][i - 1] & 255] ^ bArr4[0]);
            }
            byte[] bArr5 = bArr3[0];
            int i9 = i7 + 1;
            bArr5[0] = (byte) (rcon[i7] ^ bArr5[0]);
            byte[] bArr6;
            if (i <= 6) {
                for (length = i5; length < i; length++) {
                    for (i8 = 0; i8 < i3; i8++) {
                        bArr6 = bArr3[i8];
                        bArr6[length] = (byte) (bArr6[length] ^ bArr3[i8][length - 1]);
                    }
                }
            } else {
                for (length = i5; length < i3; length++) {
                    for (i8 = 0; i8 < i3; i8++) {
                        bArr6 = bArr3[i8];
                        bArr6[length] = (byte) (bArr6[length] ^ bArr3[i8][length - 1]);
                    }
                }
                for (length = 0; length < i3; length++) {
                    bArr4 = bArr3[length];
                    bArr4[i3] = (byte) (bArr4[i3] ^ S[bArr3[length][3] & 255]);
                }
                for (length = 5; length < i; length++) {
                    for (i8 = 0; i8 < i3; i8++) {
                        bArr6 = bArr3[i8];
                        bArr6[length] = (byte) (bArr6[length] ^ bArr3[i8][length - 1]);
                    }
                }
            }
            length = 0;
            while (length < i && i4 < (this.ROUNDS + r11) * (this.BC / r3)) {
                i8 = 0;
                for (i3 = 
/*
Method generation error in method: org.bouncycastle.crypto.engines.RijndaelEngine.generateWorkingKey(byte[]):long[][], dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r4_2 'i3' int) = (r4_1 'i3' int), (r4_5 'i3' int) binds: {(r4_1 'i3' int)=B:64:0x0144, (r4_5 'i3' int)=B:71:0x0174} in method: org.bouncycastle.crypto.engines.RijndaelEngine.generateWorkingKey(byte[]):long[][], dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:185)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:220)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:220)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: PHI can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 25 more

*/

    private byte mul0x2(int i) {
        return i != 0 ? aLogtable[25 + (logtable[i] & 255)] : (byte) 0;
    }

    private byte mul0x3(int i) {
        return i != 0 ? aLogtable[1 + (logtable[i] & 255)] : (byte) 0;
    }

    private byte mul0x9(int i) {
        return i >= 0 ? aLogtable[199 + i] : (byte) 0;
    }

    private byte mul0xb(int i) {
        return i >= 0 ? aLogtable[104 + i] : (byte) 0;
    }

    private byte mul0xd(int i) {
        return i >= 0 ? aLogtable[238 + i] : (byte) 0;
    }

    private byte mul0xe(int i) {
        return i >= 0 ? aLogtable[223 + i] : (byte) 0;
    }

    private void packBlock(byte[] bArr, int i) {
        for (int i2 = 0; i2 != this.BC; i2 += 8) {
            int i3 = i + 1;
            bArr[i] = (byte) ((int) (this.A0 >> i2));
            i = i3 + 1;
            bArr[i3] = (byte) ((int) (this.A1 >> i2));
            i3 = i + 1;
            bArr[i] = (byte) ((int) (this.A2 >> i2));
            i = i3 + 1;
            bArr[i3] = (byte) ((int) (this.A3 >> i2));
        }
    }

    private long shift(long j, int i) {
        return ((j << (this.BC - i)) | (j >>> i)) & this.BC_MASK;
    }

    private void unpackBlock(byte[] bArr, int i) {
        int i2 = i + 1;
        this.A0 = (long) (bArr[i] & 255);
        i = i2 + 1;
        this.A1 = (long) (bArr[i2] & 255);
        i2 = i + 1;
        this.A2 = (long) (bArr[i] & 255);
        i = i2 + 1;
        this.A3 = (long) (bArr[i2] & 255);
        for (i2 = 8; i2 != this.BC; i2 += 8) {
            int i3 = i + 1;
            this.A0 |= ((long) (bArr[i] & 255)) << i2;
            i = i3 + 1;
            this.A1 |= ((long) (bArr[i3] & 255)) << i2;
            i3 = i + 1;
            this.A2 |= ((long) (bArr[i] & 255)) << i2;
            i = i3 + 1;
            this.A3 |= ((long) (bArr[i3] & 255)) << i2;
        }
    }

    public String getAlgorithmName() {
        return "Rijndael";
    }

    public int getBlockSize() {
        return this.BC / 2;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            this.workingKey = generateWorkingKey(((KeyParameter) cipherParameters).getKey());
            this.forEncryption = z;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to Rijndael init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (this.workingKey == null) {
            throw new IllegalStateException("Rijndael engine not initialised");
        } else if ((this.BC / 2) + i > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if ((this.BC / 2) + i2 <= bArr2.length) {
            if (this.forEncryption) {
                unpackBlock(bArr, i);
                encryptBlock(this.workingKey);
            } else {
                unpackBlock(bArr, i);
                decryptBlock(this.workingKey);
            }
            packBlock(bArr2, i2);
            return this.BC / 2;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }
}
