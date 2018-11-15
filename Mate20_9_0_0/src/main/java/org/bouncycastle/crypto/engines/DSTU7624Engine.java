package org.bouncycastle.crypto.engines;

import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

public class DSTU7624Engine implements BlockCipher {
    private static final int ROUNDS_128 = 10;
    private static final int ROUNDS_256 = 14;
    private static final int ROUNDS_512 = 18;
    private static final byte[] S0 = new byte[]{(byte) -88, (byte) 67, (byte) 95, (byte) 6, (byte) 107, (byte) 117, (byte) 108, (byte) 89, (byte) 113, (byte) -33, (byte) -121, (byte) -107, (byte) 23, (byte) -16, (byte) -40, (byte) 9, (byte) 109, (byte) -13, (byte) 29, (byte) -53, (byte) -55, (byte) 77, (byte) 44, (byte) -81, (byte) 121, (byte) -32, (byte) -105, (byte) -3, (byte) 111, (byte) 75, (byte) 69, (byte) 57, (byte) 62, (byte) -35, (byte) -93, (byte) 79, (byte) -76, (byte) -74, (byte) -102, (byte) 14, (byte) 31, (byte) -65, (byte) 21, (byte) -31, (byte) 73, (byte) -46, (byte) -109, (byte) -58, (byte) -110, (byte) 114, (byte) -98, (byte) 97, (byte) -47, (byte) 99, (byte) -6, (byte) -18, (byte) -12, (byte) 25, (byte) -43, (byte) -83, (byte) 88, (byte) -92, (byte) -69, (byte) -95, (byte) -36, (byte) -14, (byte) -125, (byte) 55, (byte) 66, (byte) -28, (byte) 122, (byte) 50, (byte) -100, (byte) -52, (byte) -85, (byte) 74, (byte) -113, (byte) 110, (byte) 4, (byte) 39, (byte) 46, (byte) -25, (byte) -30, (byte) 90, (byte) -106, (byte) 22, (byte) 35, (byte) 43, (byte) -62, (byte) 101, (byte) 102, (byte) 15, PSSSigner.TRAILER_IMPLICIT, (byte) -87, (byte) 71, (byte) 65, (byte) 52, (byte) 72, (byte) -4, (byte) -73, (byte) 106, (byte) -120, (byte) -91, (byte) 83, (byte) -122, (byte) -7, (byte) 91, (byte) -37, (byte) 56, (byte) 123, (byte) -61, (byte) 30, (byte) 34, (byte) 51, (byte) 36, (byte) 40, (byte) 54, (byte) -57, (byte) -78, (byte) 59, (byte) -114, (byte) 119, (byte) -70, (byte) -11, (byte) 20, (byte) -97, (byte) 8, (byte) 85, (byte) -101, (byte) 76, (byte) -2, (byte) 96, (byte) 92, (byte) -38, (byte) 24, (byte) 70, (byte) -51, (byte) 125, (byte) 33, (byte) -80, (byte) 63, (byte) 27, (byte) -119, (byte) -1, (byte) -21, (byte) -124, (byte) 105, (byte) 58, (byte) -99, (byte) -41, (byte) -45, (byte) 112, (byte) 103, (byte) 64, (byte) -75, (byte) -34, (byte) 93, (byte) 48, (byte) -111, (byte) -79, (byte) 120, (byte) 17, (byte) 1, (byte) -27, (byte) 0, (byte) 104, (byte) -104, (byte) -96, (byte) -59, (byte) 2, (byte) -90, (byte) 116, (byte) 45, (byte) 11, (byte) -94, (byte) 118, (byte) -77, (byte) -66, (byte) -50, (byte) -67, (byte) -82, (byte) -23, (byte) -118, (byte) 49, (byte) 28, (byte) -20, (byte) -15, (byte) -103, (byte) -108, (byte) -86, (byte) -10, (byte) 38, (byte) 47, (byte) -17, (byte) -24, (byte) -116, (byte) 53, (byte) 3, (byte) -44, Byte.MAX_VALUE, (byte) -5, (byte) 5, (byte) -63, (byte) 94, (byte) -112, (byte) 32, (byte) 61, (byte) -126, (byte) -9, (byte) -22, (byte) 10, (byte) 13, (byte) 126, (byte) -8, (byte) 80, (byte) 26, (byte) -60, (byte) 7, (byte) 87, (byte) -72, (byte) 60, (byte) 98, (byte) -29, (byte) -56, (byte) -84, (byte) 82, (byte) 100, Tnaf.POW_2_WIDTH, (byte) -48, (byte) -39, (byte) 19, (byte) 12, (byte) 18, (byte) 41, (byte) 81, (byte) -71, (byte) -49, (byte) -42, (byte) 115, (byte) -115, (byte) -127, (byte) 84, (byte) -64, (byte) -19, (byte) 78, (byte) 68, (byte) -89, (byte) 42, (byte) -123, (byte) 37, (byte) -26, (byte) -54, (byte) 124, (byte) -117, (byte) 86, Byte.MIN_VALUE};
    private static final byte[] S1 = new byte[]{(byte) -50, (byte) -69, (byte) -21, (byte) -110, (byte) -22, (byte) -53, (byte) 19, (byte) -63, (byte) -23, (byte) 58, (byte) -42, (byte) -78, (byte) -46, (byte) -112, (byte) 23, (byte) -8, (byte) 66, (byte) 21, (byte) 86, (byte) -76, (byte) 101, (byte) 28, (byte) -120, (byte) 67, (byte) -59, (byte) 92, (byte) 54, (byte) -70, (byte) -11, (byte) 87, (byte) 103, (byte) -115, (byte) 49, (byte) -10, (byte) 100, (byte) 88, (byte) -98, (byte) -12, (byte) 34, (byte) -86, (byte) 117, (byte) 15, (byte) 2, (byte) -79, (byte) -33, (byte) 109, (byte) 115, (byte) 77, (byte) 124, (byte) 38, (byte) 46, (byte) -9, (byte) 8, (byte) 93, (byte) 68, (byte) 62, (byte) -97, (byte) 20, (byte) -56, (byte) -82, (byte) 84, Tnaf.POW_2_WIDTH, (byte) -40, PSSSigner.TRAILER_IMPLICIT, (byte) 26, (byte) 107, (byte) 105, (byte) -13, (byte) -67, (byte) 51, (byte) -85, (byte) -6, (byte) -47, (byte) -101, (byte) 104, (byte) 78, (byte) 22, (byte) -107, (byte) -111, (byte) -18, (byte) 76, (byte) 99, (byte) -114, (byte) 91, (byte) -52, (byte) 60, (byte) 25, (byte) -95, (byte) -127, (byte) 73, (byte) 123, (byte) -39, (byte) 111, (byte) 55, (byte) 96, (byte) -54, (byte) -25, (byte) 43, (byte) 72, (byte) -3, (byte) -106, (byte) 69, (byte) -4, (byte) 65, (byte) 18, (byte) 13, (byte) 121, (byte) -27, (byte) -119, (byte) -116, (byte) -29, (byte) 32, (byte) 48, (byte) -36, (byte) -73, (byte) 108, (byte) 74, (byte) -75, (byte) 63, (byte) -105, (byte) -44, (byte) 98, (byte) 45, (byte) 6, (byte) -92, (byte) -91, (byte) -125, (byte) 95, (byte) 42, (byte) -38, (byte) -55, (byte) 0, (byte) 126, (byte) -94, (byte) 85, (byte) -65, (byte) 17, (byte) -43, (byte) -100, (byte) -49, (byte) 14, (byte) 10, (byte) 61, (byte) 81, (byte) 125, (byte) -109, (byte) 27, (byte) -2, (byte) -60, (byte) 71, (byte) 9, (byte) -122, (byte) 11, (byte) -113, (byte) -99, (byte) 106, (byte) 7, (byte) -71, (byte) -80, (byte) -104, (byte) 24, (byte) 50, (byte) 113, (byte) 75, (byte) -17, (byte) 59, (byte) 112, (byte) -96, (byte) -28, (byte) 64, (byte) -1, (byte) -61, (byte) -87, (byte) -26, (byte) 120, (byte) -7, (byte) -117, (byte) 70, Byte.MIN_VALUE, (byte) 30, (byte) 56, (byte) -31, (byte) -72, (byte) -88, (byte) -32, (byte) 12, (byte) 35, (byte) 118, (byte) 29, (byte) 37, (byte) 36, (byte) 5, (byte) -15, (byte) 110, (byte) -108, (byte) 40, (byte) -102, (byte) -124, (byte) -24, (byte) -93, (byte) 79, (byte) 119, (byte) -45, (byte) -123, (byte) -30, (byte) 82, (byte) -14, (byte) -126, (byte) 80, (byte) 122, (byte) 47, (byte) 116, (byte) 83, (byte) -77, (byte) 97, (byte) -81, (byte) 57, (byte) 53, (byte) -34, (byte) -51, (byte) 31, (byte) -103, (byte) -84, (byte) -83, (byte) 114, (byte) 44, (byte) -35, (byte) -48, (byte) -121, (byte) -66, (byte) 94, (byte) -90, (byte) -20, (byte) 4, (byte) -58, (byte) 3, (byte) 52, (byte) -5, (byte) -37, (byte) 89, (byte) -74, (byte) -62, (byte) 1, (byte) -16, (byte) 90, (byte) -19, (byte) -89, (byte) 102, (byte) 33, Byte.MAX_VALUE, (byte) -118, (byte) 39, (byte) -57, (byte) -64, (byte) 41, (byte) -41};
    private static final byte[] S2 = new byte[]{(byte) -109, (byte) -39, (byte) -102, (byte) -75, (byte) -104, (byte) 34, (byte) 69, (byte) -4, (byte) -70, (byte) 106, (byte) -33, (byte) 2, (byte) -97, (byte) -36, (byte) 81, (byte) 89, (byte) 74, (byte) 23, (byte) 43, (byte) -62, (byte) -108, (byte) -12, (byte) -69, (byte) -93, (byte) 98, (byte) -28, (byte) 113, (byte) -44, (byte) -51, (byte) 112, (byte) 22, (byte) -31, (byte) 73, (byte) 60, (byte) -64, (byte) -40, (byte) 92, (byte) -101, (byte) -83, (byte) -123, (byte) 83, (byte) -95, (byte) 122, (byte) -56, (byte) 45, (byte) -32, (byte) -47, (byte) 114, (byte) -90, (byte) 44, (byte) -60, (byte) -29, (byte) 118, (byte) 120, (byte) -73, (byte) -76, (byte) 9, (byte) 59, (byte) 14, (byte) 65, (byte) 76, (byte) -34, (byte) -78, (byte) -112, (byte) 37, (byte) -91, (byte) -41, (byte) 3, (byte) 17, (byte) 0, (byte) -61, (byte) 46, (byte) -110, (byte) -17, (byte) 78, (byte) 18, (byte) -99, (byte) 125, (byte) -53, (byte) 53, Tnaf.POW_2_WIDTH, (byte) -43, (byte) 79, (byte) -98, (byte) 77, (byte) -87, (byte) 85, (byte) -58, (byte) -48, (byte) 123, (byte) 24, (byte) -105, (byte) -45, (byte) 54, (byte) -26, (byte) 72, (byte) 86, (byte) -127, (byte) -113, (byte) 119, (byte) -52, (byte) -100, (byte) -71, (byte) -30, (byte) -84, (byte) -72, (byte) 47, (byte) 21, (byte) -92, (byte) 124, (byte) -38, (byte) 56, (byte) 30, (byte) 11, (byte) 5, (byte) -42, (byte) 20, (byte) 110, (byte) 108, (byte) 126, (byte) 102, (byte) -3, (byte) -79, (byte) -27, (byte) 96, (byte) -81, (byte) 94, (byte) 51, (byte) -121, (byte) -55, (byte) -16, (byte) 93, (byte) 109, (byte) 63, (byte) -120, (byte) -115, (byte) -57, (byte) -9, (byte) 29, (byte) -23, (byte) -20, (byte) -19, Byte.MIN_VALUE, (byte) 41, (byte) 39, (byte) -49, (byte) -103, (byte) -88, (byte) 80, (byte) 15, (byte) 55, (byte) 36, (byte) 40, (byte) 48, (byte) -107, (byte) -46, (byte) 62, (byte) 91, (byte) 64, (byte) -125, (byte) -77, (byte) 105, (byte) 87, (byte) 31, (byte) 7, (byte) 28, (byte) -118, PSSSigner.TRAILER_IMPLICIT, (byte) 32, (byte) -21, (byte) -50, (byte) -114, (byte) -85, (byte) -18, (byte) 49, (byte) -94, (byte) 115, (byte) -7, (byte) -54, (byte) 58, (byte) 26, (byte) -5, (byte) 13, (byte) -63, (byte) -2, (byte) -6, (byte) -14, (byte) 111, (byte) -67, (byte) -106, (byte) -35, (byte) 67, (byte) 82, (byte) -74, (byte) 8, (byte) -13, (byte) -82, (byte) -66, (byte) 25, (byte) -119, (byte) 50, (byte) 38, (byte) -80, (byte) -22, (byte) 75, (byte) 100, (byte) -124, (byte) -126, (byte) 107, (byte) -11, (byte) 121, (byte) -65, (byte) 1, (byte) 95, (byte) 117, (byte) 99, (byte) 27, (byte) 35, (byte) 61, (byte) 104, (byte) 42, (byte) 101, (byte) -24, (byte) -111, (byte) -10, (byte) -1, (byte) 19, (byte) 88, (byte) -15, (byte) 71, (byte) 10, Byte.MAX_VALUE, (byte) -59, (byte) -89, (byte) -25, (byte) 97, (byte) 90, (byte) 6, (byte) 70, (byte) 68, (byte) 66, (byte) 4, (byte) -96, (byte) -37, (byte) 57, (byte) -122, (byte) 84, (byte) -86, (byte) -116, (byte) 52, (byte) 33, (byte) -117, (byte) -8, (byte) 12, (byte) 116, (byte) 103};
    private static final byte[] S3 = new byte[]{(byte) 104, (byte) -115, (byte) -54, (byte) 77, (byte) 115, (byte) 75, (byte) 78, (byte) 42, (byte) -44, (byte) 82, (byte) 38, (byte) -77, (byte) 84, (byte) 30, (byte) 25, (byte) 31, (byte) 34, (byte) 3, (byte) 70, (byte) 61, (byte) 45, (byte) 74, (byte) 83, (byte) -125, (byte) 19, (byte) -118, (byte) -73, (byte) -43, (byte) 37, (byte) 121, (byte) -11, (byte) -67, (byte) 88, (byte) 47, (byte) 13, (byte) 2, (byte) -19, (byte) 81, (byte) -98, (byte) 17, (byte) -14, (byte) 62, (byte) 85, (byte) 94, (byte) -47, (byte) 22, (byte) 60, (byte) 102, (byte) 112, (byte) 93, (byte) -13, (byte) 69, (byte) 64, (byte) -52, (byte) -24, (byte) -108, (byte) 86, (byte) 8, (byte) -50, (byte) 26, (byte) 58, (byte) -46, (byte) -31, (byte) -33, (byte) -75, (byte) 56, (byte) 110, (byte) 14, (byte) -27, (byte) -12, (byte) -7, (byte) -122, (byte) -23, (byte) 79, (byte) -42, (byte) -123, (byte) 35, (byte) -49, (byte) 50, (byte) -103, (byte) 49, (byte) 20, (byte) -82, (byte) -18, (byte) -56, (byte) 72, (byte) -45, (byte) 48, (byte) -95, (byte) -110, (byte) 65, (byte) -79, (byte) 24, (byte) -60, (byte) 44, (byte) 113, (byte) 114, (byte) 68, (byte) 21, (byte) -3, (byte) 55, (byte) -66, (byte) 95, (byte) -86, (byte) -101, (byte) -120, (byte) -40, (byte) -85, (byte) -119, (byte) -100, (byte) -6, (byte) 96, (byte) -22, PSSSigner.TRAILER_IMPLICIT, (byte) 98, (byte) 12, (byte) 36, (byte) -90, (byte) -88, (byte) -20, (byte) 103, (byte) 32, (byte) -37, (byte) 124, (byte) 40, (byte) -35, (byte) -84, (byte) 91, (byte) 52, (byte) 126, Tnaf.POW_2_WIDTH, (byte) -15, (byte) 123, (byte) -113, (byte) 99, (byte) -96, (byte) 5, (byte) -102, (byte) 67, (byte) 119, (byte) 33, (byte) -65, (byte) 39, (byte) 9, (byte) -61, (byte) -97, (byte) -74, (byte) -41, (byte) 41, (byte) -62, (byte) -21, (byte) -64, (byte) -92, (byte) -117, (byte) -116, (byte) 29, (byte) -5, (byte) -1, (byte) -63, (byte) -78, (byte) -105, (byte) 46, (byte) -8, (byte) 101, (byte) -10, (byte) 117, (byte) 7, (byte) 4, (byte) 73, (byte) 51, (byte) -28, (byte) -39, (byte) -71, (byte) -48, (byte) 66, (byte) -57, (byte) 108, (byte) -112, (byte) 0, (byte) -114, (byte) 111, (byte) 80, (byte) 1, (byte) -59, (byte) -38, (byte) 71, (byte) 63, (byte) -51, (byte) 105, (byte) -94, (byte) -30, (byte) 122, (byte) -89, (byte) -58, (byte) -109, (byte) 15, (byte) 10, (byte) 6, (byte) -26, (byte) 43, (byte) -106, (byte) -93, (byte) 28, (byte) -81, (byte) 106, (byte) 18, (byte) -124, (byte) 57, (byte) -25, (byte) -80, (byte) -126, (byte) -9, (byte) -2, (byte) -99, (byte) -121, (byte) 92, (byte) -127, (byte) 53, (byte) -34, (byte) -76, (byte) -91, (byte) -4, Byte.MIN_VALUE, (byte) -17, (byte) -53, (byte) -69, (byte) 107, (byte) 118, (byte) -70, (byte) 90, (byte) 125, (byte) 120, (byte) 11, (byte) -107, (byte) -29, (byte) -83, (byte) 116, (byte) -104, (byte) 59, (byte) 54, (byte) 100, (byte) 109, (byte) -36, (byte) -16, (byte) 89, (byte) -87, (byte) 76, (byte) 23, Byte.MAX_VALUE, (byte) -111, (byte) -72, (byte) -55, (byte) 87, (byte) 27, (byte) -32, (byte) 97};
    private static final byte[] T0 = new byte[]{(byte) -92, (byte) -94, (byte) -87, (byte) -59, (byte) 78, (byte) -55, (byte) 3, (byte) -39, (byte) 126, (byte) 15, (byte) -46, (byte) -83, (byte) -25, (byte) -45, (byte) 39, (byte) 91, (byte) -29, (byte) -95, (byte) -24, (byte) -26, (byte) 124, (byte) 42, (byte) 85, (byte) 12, (byte) -122, (byte) 57, (byte) -41, (byte) -115, (byte) -72, (byte) 18, (byte) 111, (byte) 40, (byte) -51, (byte) -118, (byte) 112, (byte) 86, (byte) 114, (byte) -7, (byte) -65, (byte) 79, (byte) 115, (byte) -23, (byte) -9, (byte) 87, (byte) 22, (byte) -84, (byte) 80, (byte) -64, (byte) -99, (byte) -73, (byte) 71, (byte) 113, (byte) 96, (byte) -60, (byte) 116, (byte) 67, (byte) 108, (byte) 31, (byte) -109, (byte) 119, (byte) -36, (byte) -50, (byte) 32, (byte) -116, (byte) -103, (byte) 95, (byte) 68, (byte) 1, (byte) -11, (byte) 30, (byte) -121, (byte) 94, (byte) 97, (byte) 44, (byte) 75, (byte) 29, (byte) -127, (byte) 21, (byte) -12, (byte) 35, (byte) -42, (byte) -22, (byte) -31, (byte) 103, (byte) -15, Byte.MAX_VALUE, (byte) -2, (byte) -38, (byte) 60, (byte) 7, (byte) 83, (byte) 106, (byte) -124, (byte) -100, (byte) -53, (byte) 2, (byte) -125, (byte) 51, (byte) -35, (byte) 53, (byte) -30, (byte) 89, (byte) 90, (byte) -104, (byte) -91, (byte) -110, (byte) 100, (byte) 4, (byte) 6, Tnaf.POW_2_WIDTH, (byte) 77, (byte) 28, (byte) -105, (byte) 8, (byte) 49, (byte) -18, (byte) -85, (byte) 5, (byte) -81, (byte) 121, (byte) -96, (byte) 24, (byte) 70, (byte) 109, (byte) -4, (byte) -119, (byte) -44, (byte) -57, (byte) -1, (byte) -16, (byte) -49, (byte) 66, (byte) -111, (byte) -8, (byte) 104, (byte) 10, (byte) 101, (byte) -114, (byte) -74, (byte) -3, (byte) -61, (byte) -17, (byte) 120, (byte) 76, (byte) -52, (byte) -98, (byte) 48, (byte) 46, PSSSigner.TRAILER_IMPLICIT, (byte) 11, (byte) 84, (byte) 26, (byte) -90, (byte) -69, (byte) 38, Byte.MIN_VALUE, (byte) 72, (byte) -108, (byte) 50, (byte) 125, (byte) -89, (byte) 63, (byte) -82, (byte) 34, (byte) 61, (byte) 102, (byte) -86, (byte) -10, (byte) 0, (byte) 93, (byte) -67, (byte) 74, (byte) -32, (byte) 59, (byte) -76, (byte) 23, (byte) -117, (byte) -97, (byte) 118, (byte) -80, (byte) 36, (byte) -102, (byte) 37, (byte) 99, (byte) -37, (byte) -21, (byte) 122, (byte) 62, (byte) 92, (byte) -77, (byte) -79, (byte) 41, (byte) -14, (byte) -54, (byte) 88, (byte) 110, (byte) -40, (byte) -88, (byte) 47, (byte) 117, (byte) -33, (byte) 20, (byte) -5, (byte) 19, (byte) 73, (byte) -120, (byte) -78, (byte) -20, (byte) -28, (byte) 52, (byte) 45, (byte) -106, (byte) -58, (byte) 58, (byte) -19, (byte) -107, (byte) 14, (byte) -27, (byte) -123, (byte) 107, (byte) 64, (byte) 33, (byte) -101, (byte) 9, (byte) 25, (byte) 43, (byte) 82, (byte) -34, (byte) 69, (byte) -93, (byte) -6, (byte) 81, (byte) -62, (byte) -75, (byte) -47, (byte) -112, (byte) -71, (byte) -13, (byte) 55, (byte) -63, (byte) 13, (byte) -70, (byte) 65, (byte) 17, (byte) 56, (byte) 123, (byte) -66, (byte) -48, (byte) -43, (byte) 105, (byte) 54, (byte) -56, (byte) 98, (byte) 27, (byte) -126, (byte) -113};
    private static final byte[] T1 = new byte[]{(byte) -125, (byte) -14, (byte) 42, (byte) -21, (byte) -23, (byte) -65, (byte) 123, (byte) -100, (byte) 52, (byte) -106, (byte) -115, (byte) -104, (byte) -71, (byte) 105, (byte) -116, (byte) 41, (byte) 61, (byte) -120, (byte) 104, (byte) 6, (byte) 57, (byte) 17, (byte) 76, (byte) 14, (byte) -96, (byte) 86, (byte) 64, (byte) -110, (byte) 21, PSSSigner.TRAILER_IMPLICIT, (byte) -77, (byte) -36, (byte) 111, (byte) -8, (byte) 38, (byte) -70, (byte) -66, (byte) -67, (byte) 49, (byte) -5, (byte) -61, (byte) -2, Byte.MIN_VALUE, (byte) 97, (byte) -31, (byte) 122, (byte) 50, (byte) -46, (byte) 112, (byte) 32, (byte) -95, (byte) 69, (byte) -20, (byte) -39, (byte) 26, (byte) 93, (byte) -76, (byte) -40, (byte) 9, (byte) -91, (byte) 85, (byte) -114, (byte) 55, (byte) 118, (byte) -87, (byte) 103, Tnaf.POW_2_WIDTH, (byte) 23, (byte) 54, (byte) 101, (byte) -79, (byte) -107, (byte) 98, (byte) 89, (byte) 116, (byte) -93, (byte) 80, (byte) 47, (byte) 75, (byte) -56, (byte) -48, (byte) -113, (byte) -51, (byte) -44, (byte) 60, (byte) -122, (byte) 18, (byte) 29, (byte) 35, (byte) -17, (byte) -12, (byte) 83, (byte) 25, (byte) 53, (byte) -26, Byte.MAX_VALUE, (byte) 94, (byte) -42, (byte) 121, (byte) 81, (byte) 34, (byte) 20, (byte) -9, (byte) 30, (byte) 74, (byte) 66, (byte) -101, (byte) 65, (byte) 115, (byte) 45, (byte) -63, (byte) 92, (byte) -90, (byte) -94, (byte) -32, (byte) 46, (byte) -45, (byte) 40, (byte) -69, (byte) -55, (byte) -82, (byte) 106, (byte) -47, (byte) 90, (byte) 48, (byte) -112, (byte) -124, (byte) -7, (byte) -78, (byte) 88, (byte) -49, (byte) 126, (byte) -59, (byte) -53, (byte) -105, (byte) -28, (byte) 22, (byte) 108, (byte) -6, (byte) -80, (byte) 109, (byte) 31, (byte) 82, (byte) -103, (byte) 13, (byte) 78, (byte) 3, (byte) -111, (byte) -62, (byte) 77, (byte) 100, (byte) 119, (byte) -97, (byte) -35, (byte) -60, (byte) 73, (byte) -118, (byte) -102, (byte) 36, (byte) 56, (byte) -89, (byte) 87, (byte) -123, (byte) -57, (byte) 124, (byte) 125, (byte) -25, (byte) -10, (byte) -73, (byte) -84, (byte) 39, (byte) 70, (byte) -34, (byte) -33, (byte) 59, (byte) -41, (byte) -98, (byte) 43, (byte) 11, (byte) -43, (byte) 19, (byte) 117, (byte) -16, (byte) 114, (byte) -74, (byte) -99, (byte) 27, (byte) 1, (byte) 63, (byte) 68, (byte) -27, (byte) -121, (byte) -3, (byte) 7, (byte) -15, (byte) -85, (byte) -108, (byte) 24, (byte) -22, (byte) -4, (byte) 58, (byte) -126, (byte) 95, (byte) 5, (byte) 84, (byte) -37, (byte) 0, (byte) -117, (byte) -29, (byte) 72, (byte) 12, (byte) -54, (byte) 120, (byte) -119, (byte) 10, (byte) -1, (byte) 62, (byte) 91, (byte) -127, (byte) -18, (byte) 113, (byte) -30, (byte) -38, (byte) 44, (byte) -72, (byte) -75, (byte) -52, (byte) 110, (byte) -88, (byte) 107, (byte) -83, (byte) 96, (byte) -58, (byte) 8, (byte) 4, (byte) 2, (byte) -24, (byte) -11, (byte) 79, (byte) -92, (byte) -13, (byte) -64, (byte) -50, (byte) 67, (byte) 37, (byte) 28, (byte) 33, (byte) 51, (byte) 15, (byte) -81, (byte) 71, (byte) -19, (byte) 102, (byte) 99, (byte) -109, (byte) -86};
    private static final byte[] T2 = new byte[]{(byte) 69, (byte) -44, (byte) 11, (byte) 67, (byte) -15, (byte) 114, (byte) -19, (byte) -92, (byte) -62, (byte) 56, (byte) -26, (byte) 113, (byte) -3, (byte) -74, (byte) 58, (byte) -107, (byte) 80, (byte) 68, (byte) 75, (byte) -30, (byte) 116, (byte) 107, (byte) 30, (byte) 17, (byte) 90, (byte) -58, (byte) -76, (byte) -40, (byte) -91, (byte) -118, (byte) 112, (byte) -93, (byte) -88, (byte) -6, (byte) 5, (byte) -39, (byte) -105, (byte) 64, (byte) -55, (byte) -112, (byte) -104, (byte) -113, (byte) -36, (byte) 18, (byte) 49, (byte) 44, (byte) 71, (byte) 106, (byte) -103, (byte) -82, (byte) -56, Byte.MAX_VALUE, (byte) -7, (byte) 79, (byte) 93, (byte) -106, (byte) 111, (byte) -12, (byte) -77, (byte) 57, (byte) 33, (byte) -38, (byte) -100, (byte) -123, (byte) -98, (byte) 59, (byte) -16, (byte) -65, (byte) -17, (byte) 6, (byte) -18, (byte) -27, (byte) 95, (byte) 32, Tnaf.POW_2_WIDTH, (byte) -52, (byte) 60, (byte) 84, (byte) 74, (byte) 82, (byte) -108, (byte) 14, (byte) -64, (byte) 40, (byte) -10, (byte) 86, (byte) 96, (byte) -94, (byte) -29, (byte) 15, (byte) -20, (byte) -99, (byte) 36, (byte) -125, (byte) 126, (byte) -43, (byte) 124, (byte) -21, (byte) 24, (byte) -41, (byte) -51, (byte) -35, (byte) 120, (byte) -1, (byte) -37, (byte) -95, (byte) 9, (byte) -48, (byte) 118, (byte) -124, (byte) 117, (byte) -69, (byte) 29, (byte) 26, (byte) 47, (byte) -80, (byte) -2, (byte) -42, (byte) 52, (byte) 99, (byte) 53, (byte) -46, (byte) 42, (byte) 89, (byte) 109, (byte) 77, (byte) 119, (byte) -25, (byte) -114, (byte) 97, (byte) -49, (byte) -97, (byte) -50, (byte) 39, (byte) -11, Byte.MIN_VALUE, (byte) -122, (byte) -57, (byte) -90, (byte) -5, (byte) -8, (byte) -121, (byte) -85, (byte) 98, (byte) 63, (byte) -33, (byte) 72, (byte) 0, (byte) 20, (byte) -102, (byte) -67, (byte) 91, (byte) 4, (byte) -110, (byte) 2, (byte) 37, (byte) 101, (byte) 76, (byte) 83, (byte) 12, (byte) -14, (byte) 41, (byte) -81, (byte) 23, (byte) 108, (byte) 65, (byte) 48, (byte) -23, (byte) -109, (byte) 85, (byte) -9, (byte) -84, (byte) 104, (byte) 38, (byte) -60, (byte) 125, (byte) -54, (byte) 122, (byte) 62, (byte) -96, (byte) 55, (byte) 3, (byte) -63, (byte) 54, (byte) 105, (byte) 102, (byte) 8, (byte) 22, (byte) -89, PSSSigner.TRAILER_IMPLICIT, (byte) -59, (byte) -45, (byte) 34, (byte) -73, (byte) 19, (byte) 70, (byte) 50, (byte) -24, (byte) 87, (byte) -120, (byte) 43, (byte) -127, (byte) -78, (byte) 78, (byte) 100, (byte) 28, (byte) -86, (byte) -111, (byte) 88, (byte) 46, (byte) -101, (byte) 92, (byte) 27, (byte) 81, (byte) 115, (byte) 66, (byte) 35, (byte) 1, (byte) 110, (byte) -13, (byte) 13, (byte) -66, (byte) 61, (byte) 10, (byte) 45, (byte) 31, (byte) 103, (byte) 51, (byte) 25, (byte) 123, (byte) 94, (byte) -22, (byte) -34, (byte) -117, (byte) -53, (byte) -87, (byte) -116, (byte) -115, (byte) -83, (byte) 73, (byte) -126, (byte) -28, (byte) -70, (byte) -61, (byte) 21, (byte) -47, (byte) -32, (byte) -119, (byte) -4, (byte) -79, (byte) -71, (byte) -75, (byte) 7, (byte) 121, (byte) -72, (byte) -31};
    private static final byte[] T3 = new byte[]{(byte) -78, (byte) -74, (byte) 35, (byte) 17, (byte) -89, (byte) -120, (byte) -59, (byte) -90, (byte) 57, (byte) -113, (byte) -60, (byte) -24, (byte) 115, (byte) 34, (byte) 67, (byte) -61, (byte) -126, (byte) 39, (byte) -51, (byte) 24, (byte) 81, (byte) 98, (byte) 45, (byte) -9, (byte) 92, (byte) 14, (byte) 59, (byte) -3, (byte) -54, (byte) -101, (byte) 13, (byte) 15, (byte) 121, (byte) -116, Tnaf.POW_2_WIDTH, (byte) 76, (byte) 116, (byte) 28, (byte) 10, (byte) -114, (byte) 124, (byte) -108, (byte) 7, (byte) -57, (byte) 94, (byte) 20, (byte) -95, (byte) 33, (byte) 87, (byte) 80, (byte) 78, (byte) -87, Byte.MIN_VALUE, (byte) -39, (byte) -17, (byte) 100, (byte) 65, (byte) -49, (byte) 60, (byte) -18, (byte) 46, (byte) 19, (byte) 41, (byte) -70, (byte) 52, (byte) 90, (byte) -82, (byte) -118, (byte) 97, (byte) 51, (byte) 18, (byte) -71, (byte) 85, (byte) -88, (byte) 21, (byte) 5, (byte) -10, (byte) 3, (byte) 6, (byte) 73, (byte) -75, (byte) 37, (byte) 9, (byte) 22, (byte) 12, (byte) 42, (byte) 56, (byte) -4, (byte) 32, (byte) -12, (byte) -27, Byte.MAX_VALUE, (byte) -41, (byte) 49, (byte) 43, (byte) 102, (byte) 111, (byte) -1, (byte) 114, (byte) -122, (byte) -16, (byte) -93, (byte) 47, (byte) 120, (byte) 0, PSSSigner.TRAILER_IMPLICIT, (byte) -52, (byte) -30, (byte) -80, (byte) -15, (byte) 66, (byte) -76, (byte) 48, (byte) 95, (byte) 96, (byte) 4, (byte) -20, (byte) -91, (byte) -29, (byte) -117, (byte) -25, (byte) 29, (byte) -65, (byte) -124, (byte) 123, (byte) -26, (byte) -127, (byte) -8, (byte) -34, (byte) -40, (byte) -46, (byte) 23, (byte) -50, (byte) 75, (byte) 71, (byte) -42, (byte) 105, (byte) 108, (byte) 25, (byte) -103, (byte) -102, (byte) 1, (byte) -77, (byte) -123, (byte) -79, (byte) -7, (byte) 89, (byte) -62, (byte) 55, (byte) -23, (byte) -56, (byte) -96, (byte) -19, (byte) 79, (byte) -119, (byte) 104, (byte) 109, (byte) -43, (byte) 38, (byte) -111, (byte) -121, (byte) 88, (byte) -67, (byte) -55, (byte) -104, (byte) -36, (byte) 117, (byte) -64, (byte) 118, (byte) -11, (byte) 103, (byte) 107, (byte) 126, (byte) -21, (byte) 82, (byte) -53, (byte) -47, (byte) 91, (byte) -97, (byte) 11, (byte) -37, (byte) 64, (byte) -110, (byte) 26, (byte) -6, (byte) -84, (byte) -28, (byte) -31, (byte) 113, (byte) 31, (byte) 101, (byte) -115, (byte) -105, (byte) -98, (byte) -107, (byte) -112, (byte) 93, (byte) -73, (byte) -63, (byte) -81, (byte) 84, (byte) -5, (byte) 2, (byte) -32, (byte) 53, (byte) -69, (byte) 58, (byte) 77, (byte) -83, (byte) 44, (byte) 61, (byte) 86, (byte) 8, (byte) 27, (byte) 74, (byte) -109, (byte) 106, (byte) -85, (byte) -72, (byte) 122, (byte) -14, (byte) 125, (byte) -38, (byte) 63, (byte) -2, (byte) 62, (byte) -66, (byte) -22, (byte) -86, (byte) 68, (byte) -58, (byte) -48, (byte) 54, (byte) 72, (byte) 112, (byte) -106, (byte) 119, (byte) 36, (byte) 83, (byte) -33, (byte) -13, (byte) -125, (byte) 40, (byte) 50, (byte) 69, (byte) 30, (byte) -92, (byte) -45, (byte) -94, (byte) 70, (byte) 110, (byte) -100, (byte) -35, (byte) 99, (byte) -44, (byte) -99};
    private boolean forEncryption;
    private long[] internalState;
    private long[][] roundKeys;
    private int roundsAmount;
    private int wordsInBlock;
    private int wordsInKey;
    private long[] workingKey;

    public DSTU7624Engine(int i) throws IllegalArgumentException {
        if (i == 128 || i == 256 || i == 512) {
            this.wordsInBlock = i >>> 6;
            this.internalState = new long[this.wordsInBlock];
            return;
        }
        throw new IllegalArgumentException("unsupported block length: only 128/256/512 are allowed");
    }

    private void addRoundKey(int i) {
        long[] jArr = this.roundKeys[i];
        for (int i2 = 0; i2 < this.wordsInBlock; i2++) {
            long[] jArr2 = this.internalState;
            jArr2[i2] = jArr2[i2] + jArr[i2];
        }
    }

    private void decryptBlock_128(byte[] bArr, int i, byte[] bArr2, int i2) {
        byte[] bArr3 = bArr2;
        int i3 = i2;
        long littleEndianToLong = Pack.littleEndianToLong(bArr, i);
        long littleEndianToLong2 = Pack.littleEndianToLong(bArr, i + 8);
        long[] jArr = this.roundKeys[this.roundsAmount];
        littleEndianToLong -= jArr[0];
        littleEndianToLong2 -= jArr[1];
        int i4 = this.roundsAmount;
        while (true) {
            littleEndianToLong = mixColumnInv(littleEndianToLong);
            littleEndianToLong2 = mixColumnInv(littleEndianToLong2);
            int i5 = (int) littleEndianToLong;
            int i6 = (int) (littleEndianToLong >>> 32);
            int i7 = (int) littleEndianToLong2;
            int i8 = (int) (littleEndianToLong2 >>> 32);
            int i9 = (((T0[i5 & 255] & 255) | ((T1[(i5 >>> 8) & 255] & 255) << 8)) | ((T2[(i5 >>> 16) & 255] & 255) << 16)) | (T3[i5 >>> 24] << 24);
            byte b = T0[i8 & 255];
            byte b2 = T1[(i8 >>> 8) & 255];
            byte b3 = T2[(i8 >>> 16) & 255];
            littleEndianToLong2 = (((long) ((T3[i8 >>> 24] << 24) | (((b & 255) | ((b2 & 255) << 8)) | ((b3 & 255) << 16)))) << 32) | (((long) i9) & BodyPartID.bodyIdMax);
            b = T0[i7 & 255];
            b2 = T1[(i7 >>> 8) & 255];
            i7 = T3[i7 >>> 24] << 24;
            i7 |= ((T2[(i7 >>> 16) & 255] & 255) << 16) | ((b & 255) | ((b2 & 255) << 8));
            byte b4 = T0[i6 & 255];
            b = T1[(i6 >>> 8) & 255];
            b2 = T2[(i6 >>> 16) & 255];
            littleEndianToLong = (((long) ((T3[i6 >>> 24] << 24) | (((b4 & 255) | ((b & 255) << 8)) | ((b2 & 255) << 16)))) << 32) | (((long) i7) & BodyPartID.bodyIdMax);
            i4--;
            if (i4 == 0) {
                long[] jArr2 = this.roundKeys[0];
                littleEndianToLong -= jArr2[1];
                Pack.longToLittleEndian(littleEndianToLong2 - jArr2[0], bArr3, i3);
                Pack.longToLittleEndian(littleEndianToLong, bArr3, i3 + 8);
                return;
            }
            long[] jArr3 = this.roundKeys[i4];
            long j = littleEndianToLong ^ jArr3[1];
            littleEndianToLong = littleEndianToLong2 ^ jArr3[0];
            littleEndianToLong2 = j;
        }
    }

    private void encryptBlock_128(byte[] bArr, int i, byte[] bArr2, int i2) {
        byte[] bArr3 = bArr2;
        int i3 = i2;
        long littleEndianToLong = Pack.littleEndianToLong(bArr, i);
        long littleEndianToLong2 = Pack.littleEndianToLong(bArr, i + 8);
        long[] jArr = this.roundKeys[0];
        littleEndianToLong += jArr[0];
        littleEndianToLong2 += jArr[1];
        int i4 = 0;
        while (true) {
            int i5 = (int) littleEndianToLong;
            int i6 = (int) (littleEndianToLong >>> 32);
            int i7 = (int) littleEndianToLong2;
            int i8 = (int) (littleEndianToLong2 >>> 32);
            int i9 = (((S0[i5 & 255] & 255) | ((S1[(i5 >>> 8) & 255] & 255) << 8)) | ((S2[(i5 >>> 16) & 255] & 255) << 16)) | (S3[i5 >>> 24] << 24);
            byte b = S0[i8 & 255];
            byte b2 = S1[(i8 >>> 8) & 255];
            byte b3 = S2[(i8 >>> 16) & 255];
            littleEndianToLong2 = (((long) ((S3[i8 >>> 24] << 24) | (((b & 255) | ((b2 & 255) << 8)) | ((b3 & 255) << 16)))) << 32) | (((long) i9) & BodyPartID.bodyIdMax);
            b = S0[i7 & 255];
            b2 = S1[(i7 >>> 8) & 255];
            i7 = S3[i7 >>> 24] << 24;
            i7 |= ((S2[(i7 >>> 16) & 255] & 255) << 16) | ((b & 255) | ((b2 & 255) << 8));
            byte b4 = S0[i6 & 255];
            b = S1[(i6 >>> 8) & 255];
            b2 = S2[(i6 >>> 16) & 255];
            littleEndianToLong = (((long) ((S3[i6 >>> 24] << 24) | (((b4 & 255) | ((b & 255) << 8)) | ((b2 & 255) << 16)))) << 32) | (((long) i7) & BodyPartID.bodyIdMax);
            littleEndianToLong2 = mixColumn(littleEndianToLong2);
            littleEndianToLong = mixColumn(littleEndianToLong);
            i4++;
            if (i4 == this.roundsAmount) {
                long[] jArr2 = this.roundKeys[this.roundsAmount];
                littleEndianToLong += jArr2[1];
                Pack.longToLittleEndian(littleEndianToLong2 + jArr2[0], bArr3, i3);
                Pack.longToLittleEndian(littleEndianToLong, bArr3, i3 + 8);
                return;
            }
            long[] jArr3 = this.roundKeys[i4];
            long j = littleEndianToLong ^ jArr3[1];
            littleEndianToLong = littleEndianToLong2 ^ jArr3[0];
            littleEndianToLong2 = j;
        }
    }

    private void invShiftRows() {
        int i = this.wordsInBlock;
        long j;
        long j2;
        long j3;
        long j4;
        long j5;
        long j6;
        long j7;
        long j8;
        if (i == 2) {
            j = this.internalState[0];
            j2 = this.internalState[1];
            j3 = (j ^ j2) & -4294967296L;
            long j9 = j ^ j3;
            j3 ^= j2;
            this.internalState[0] = j9;
            this.internalState[1] = j3;
        } else if (i == 4) {
            j4 = this.internalState[0];
            j5 = this.internalState[1];
            j6 = this.internalState[2];
            j7 = this.internalState[3];
            j8 = (j4 ^ j5) & -281470681808896L;
            j4 ^= j8;
            j5 ^= j8;
            j2 = (j6 ^ j7) & -281470681808896L;
            j6 ^= j2;
            j2 = j7 ^ j2;
            j3 = (j4 ^ j6) & -4294967296L;
            j4 ^= j3;
            j3 = j6 ^ j3;
            j = (j5 ^ j2) & 281474976645120L;
            j5 ^= j;
            j ^= j2;
            this.internalState[0] = j4;
            this.internalState[1] = j5;
            this.internalState[2] = j3;
            this.internalState[3] = j;
        } else if (i == 8) {
            j4 = this.internalState[0];
            j5 = this.internalState[1];
            j6 = this.internalState[2];
            j7 = this.internalState[3];
            j8 = this.internalState[4];
            long j10 = this.internalState[5];
            long j11 = this.internalState[6];
            long j12 = this.internalState[7];
            long j13 = (j4 ^ j5) & -71777214294589696L;
            j4 ^= j13;
            j5 ^= j13;
            j13 = (j6 ^ j7) & -71777214294589696L;
            j6 ^= j13;
            j7 ^= j13;
            j13 = (j8 ^ j10) & -71777214294589696L;
            j8 ^= j13;
            j10 ^= j13;
            j13 = (j11 ^ j12) & -71777214294589696L;
            j11 ^= j13;
            j12 ^= j13;
            j13 = (j4 ^ j6) & -281470681808896L;
            j4 ^= j13;
            j6 ^= j13;
            j13 = (j5 ^ j7) & 72056494543077120L;
            j5 ^= j13;
            j7 ^= j13;
            long j14 = (j8 ^ j11) & -281470681808896L;
            j8 ^= j14;
            j14 = j11 ^ j14;
            j11 = (j10 ^ j12) & 72056494543077120L;
            j10 ^= j11;
            j11 = j12 ^ j11;
            j3 = (j4 ^ j8) & -4294967296L;
            j4 ^= j3;
            j3 = j8 ^ j3;
            j8 = (j5 ^ j10) & 72057594021150720L;
            j5 ^= j8;
            j8 = j10 ^ j8;
            j = (j6 ^ j14) & 281474976645120L;
            j6 ^= j;
            j ^= j14;
            j14 = (j7 ^ j11) & 1099511627520L;
            j7 ^= j14;
            j14 = j11 ^ j14;
            this.internalState[0] = j4;
            this.internalState[1] = j5;
            this.internalState[2] = j6;
            this.internalState[3] = j7;
            this.internalState[4] = j3;
            this.internalState[5] = j8;
            this.internalState[6] = j;
            this.internalState[7] = j14;
        } else {
            throw new IllegalStateException("unsupported block length: only 128/256/512 are allowed");
        }
    }

    private void invSubBytes() {
        for (int i = 0; i < this.wordsInBlock; i++) {
            long j = this.internalState[i];
            int i2 = (int) j;
            int i3 = (int) (j >>> 32);
            int i4 = (((T0[i2 & 255] & 255) | ((T1[(i2 >>> 8) & 255] & 255) << 8)) | ((T2[(i2 >>> 16) & 255] & 255) << 16)) | (T3[i2 >>> 24] << 24);
            byte b = T0[i3 & 255];
            byte b2 = T1[(i3 >>> 8) & 255];
            byte b3 = T2[(i3 >>> 16) & 255];
            this.internalState[i] = (((long) ((T3[i3 >>> 24] << 24) | (((b & 255) | ((b2 & 255) << 8)) | ((b3 & 255) << 16)))) << 32) | (((long) i4) & BodyPartID.bodyIdMax);
        }
    }

    private static long mixColumn(long j) {
        long mulX = mulX(j);
        long rotate = rotate(8, j) ^ j;
        rotate = (rotate ^ rotate(16, rotate)) ^ rotate(48, j);
        return ((rotate(32, mulX2((j ^ rotate) ^ mulX)) ^ rotate) ^ rotate(40, mulX)) ^ rotate(48, mulX);
    }

    private static long mixColumnInv(long j) {
        long j2 = j;
        long rotate = rotate(8, j2) ^ j2;
        rotate = (rotate ^ rotate(32, rotate)) ^ rotate(48, j2);
        long j3 = rotate ^ j2;
        long rotate2 = rotate(48, j2);
        long rotate3 = rotate(56, j2);
        long j4 = j3 ^ rotate3;
        j4 = mulX(j4) ^ rotate(56, j3);
        j4 = rotate(40, mulX(j4) ^ j2) ^ (rotate(16, j3) ^ j2);
        j4 = mulX(j4) ^ (j3 ^ rotate2);
        return mulX(rotate(40, ((j2 ^ rotate(32, j3)) ^ rotate3) ^ mulX(((rotate2 ^ (rotate(24, j2) ^ j3)) ^ rotate3) ^ mulX(mulX(j4) ^ rotate(16, rotate))))) ^ rotate;
    }

    private void mixColumns() {
        for (int i = 0; i < this.wordsInBlock; i++) {
            this.internalState[i] = mixColumn(this.internalState[i]);
        }
    }

    private void mixColumnsInv() {
        for (int i = 0; i < this.wordsInBlock; i++) {
            this.internalState[i] = mixColumnInv(this.internalState[i]);
        }
    }

    private static long mulX(long j) {
        return (((j & -9187201950435737472L) >>> 7) * 29) ^ ((9187201950435737471L & j) << 1);
    }

    private static long mulX2(long j) {
        return (((j & 4629771061636907072L) >>> 6) * 29) ^ (((4557430888798830399L & j) << 2) ^ (((-9187201950435737472L & j) >>> 6) * 29));
    }

    private static long rotate(int i, long j) {
        return (j << (-i)) | (j >>> i);
    }

    private void rotateLeft(long[] jArr, long[] jArr2) {
        int i = this.wordsInBlock;
        long j;
        long j2;
        long j3;
        long j4;
        if (i == 2) {
            long j5 = jArr[0];
            j = jArr[1];
            jArr2[0] = (j5 >>> 56) | (j << 8);
            jArr2[1] = (j >>> 56) | (j5 << 8);
        } else if (i == 4) {
            j2 = jArr[0];
            j3 = jArr[1];
            j4 = jArr[2];
            j = jArr[3];
            jArr2[0] = (j3 >>> 24) | (j4 << 40);
            jArr2[1] = (j4 >>> 24) | (j << 40);
            jArr2[2] = (j >>> 24) | (j2 << 40);
            jArr2[3] = (j2 >>> 24) | (j3 << 40);
        } else if (i == 8) {
            j2 = jArr[0];
            j3 = jArr[1];
            j4 = jArr[2];
            long j6 = jArr[3];
            long j7 = jArr[4];
            long j8 = jArr[5];
            long j9 = jArr[6];
            long j10 = jArr[7];
            jArr2[0] = (j4 >>> 24) | (j6 << 40);
            jArr2[1] = (j6 >>> 24) | (j7 << 40);
            jArr2[2] = (j7 >>> 24) | (j8 << 40);
            jArr2[3] = (j8 >>> 24) | (j9 << 40);
            jArr2[4] = (j9 >>> 24) | (j10 << 40);
            jArr2[5] = (j10 >>> 24) | (j2 << 40);
            jArr2[6] = (j2 >>> 24) | (j3 << 40);
            jArr2[7] = (j3 >>> 24) | (j4 << 40);
        } else {
            throw new IllegalStateException("unsupported block length: only 128/256/512 are allowed");
        }
    }

    private void shiftRows() {
        int i = this.wordsInBlock;
        long j;
        long j2;
        long j3;
        long j4;
        long j5;
        long j6;
        long j7;
        if (i == 2) {
            j = this.internalState[0];
            j2 = this.internalState[1];
            j3 = (j ^ j2) & -4294967296L;
            long j8 = j ^ j3;
            j3 ^= j2;
            this.internalState[0] = j8;
            this.internalState[1] = j3;
        } else if (i == 4) {
            j4 = this.internalState[0];
            j5 = this.internalState[1];
            j6 = this.internalState[2];
            j7 = this.internalState[3];
            j3 = (j4 ^ j6) & -4294967296L;
            j4 ^= j3;
            j3 = j6 ^ j3;
            j = (j5 ^ j7) & 281474976645120L;
            j5 ^= j;
            j = j7 ^ j;
            j6 = (j4 ^ j5) & -281470681808896L;
            j5 ^= j6;
            j2 = (j3 ^ j) & -281470681808896L;
            j3 ^= j2;
            j ^= j2;
            this.internalState[0] = j4 ^ j6;
            this.internalState[1] = j5;
            this.internalState[2] = j3;
            this.internalState[3] = j;
        } else if (i == 8) {
            j4 = this.internalState[0];
            j5 = this.internalState[1];
            j6 = this.internalState[2];
            j7 = this.internalState[3];
            long j9 = this.internalState[4];
            long j10 = this.internalState[5];
            long j11 = this.internalState[6];
            long j12 = this.internalState[7];
            j3 = (j4 ^ j9) & -4294967296L;
            j4 ^= j3;
            j3 = j9 ^ j3;
            j9 = (j5 ^ j10) & 72057594021150720L;
            j5 ^= j9;
            j9 = j10 ^ j9;
            j = (j6 ^ j11) & 281474976645120L;
            j6 ^= j;
            j = j11 ^ j;
            j10 = (j7 ^ j12) & 1099511627520L;
            j7 ^= j10;
            j10 = j12 ^ j10;
            j11 = (j4 ^ j6) & -281470681808896L;
            j4 ^= j11;
            j6 ^= j11;
            j11 = (j5 ^ j7) & 72056494543077120L;
            j5 ^= j11;
            j7 ^= j11;
            long j13 = (j3 ^ j) & -281470681808896L;
            j3 ^= j13;
            j ^= j13;
            j13 = (j9 ^ j10) & 72056494543077120L;
            j9 ^= j13;
            j13 = j10 ^ j13;
            j10 = (j4 ^ j5) & -71777214294589696L;
            j4 ^= j10;
            j5 ^= j10;
            j10 = (j6 ^ j7) & -71777214294589696L;
            j6 ^= j10;
            j7 ^= j10;
            j10 = (j3 ^ j9) & -71777214294589696L;
            j3 ^= j10;
            j9 ^= j10;
            j10 = (j ^ j13) & -71777214294589696L;
            j ^= j10;
            j13 ^= j10;
            this.internalState[0] = j4;
            this.internalState[1] = j5;
            this.internalState[2] = j6;
            this.internalState[3] = j7;
            this.internalState[4] = j3;
            this.internalState[5] = j9;
            this.internalState[6] = j;
            this.internalState[7] = j13;
        } else {
            throw new IllegalStateException("unsupported block length: only 128/256/512 are allowed");
        }
    }

    private void subBytes() {
        for (int i = 0; i < this.wordsInBlock; i++) {
            long j = this.internalState[i];
            int i2 = (int) j;
            int i3 = (int) (j >>> 32);
            int i4 = (((S0[i2 & 255] & 255) | ((S1[(i2 >>> 8) & 255] & 255) << 8)) | ((S2[(i2 >>> 16) & 255] & 255) << 16)) | (S3[i2 >>> 24] << 24);
            byte b = S0[i3 & 255];
            byte b2 = S1[(i3 >>> 8) & 255];
            byte b3 = S2[(i3 >>> 16) & 255];
            this.internalState[i] = (((long) ((S3[i3 >>> 24] << 24) | (((b & 255) | ((b2 & 255) << 8)) | ((b3 & 255) << 16)))) << 32) | (((long) i4) & BodyPartID.bodyIdMax);
        }
    }

    private void subRoundKey(int i) {
        long[] jArr = this.roundKeys[i];
        for (int i2 = 0; i2 < this.wordsInBlock; i2++) {
            long[] jArr2 = this.internalState;
            jArr2[i2] = jArr2[i2] - jArr[i2];
        }
    }

    private void workingKeyExpandEven(long[] jArr, long[] jArr2) {
        Object obj = new long[this.wordsInKey];
        long[] jArr3 = new long[this.wordsInBlock];
        System.arraycopy(jArr, 0, obj, 0, this.wordsInKey);
        long j = 281479271743489L;
        int i = 0;
        while (true) {
            int i2;
            long[] jArr4;
            for (i2 = 0; i2 < this.wordsInBlock; i2++) {
                jArr3[i2] = jArr2[i2] + j;
            }
            for (i2 = 0; i2 < this.wordsInBlock; i2++) {
                this.internalState[i2] = obj[i2] + jArr3[i2];
            }
            subBytes();
            shiftRows();
            mixColumns();
            for (i2 = 0; i2 < this.wordsInBlock; i2++) {
                jArr4 = this.internalState;
                jArr4[i2] = jArr4[i2] ^ jArr3[i2];
            }
            subBytes();
            shiftRows();
            mixColumns();
            for (i2 = 0; i2 < this.wordsInBlock; i2++) {
                jArr4 = this.internalState;
                jArr4[i2] = jArr4[i2] + jArr3[i2];
            }
            System.arraycopy(this.internalState, 0, this.roundKeys[i], 0, this.wordsInBlock);
            if (this.roundsAmount != i) {
                if (this.wordsInBlock != this.wordsInKey) {
                    i += 2;
                    j <<= 1;
                    for (i2 = 0; i2 < this.wordsInBlock; i2++) {
                        jArr3[i2] = jArr2[i2] + j;
                    }
                    for (i2 = 0; i2 < this.wordsInBlock; i2++) {
                        this.internalState[i2] = obj[this.wordsInBlock + i2] + jArr3[i2];
                    }
                    subBytes();
                    shiftRows();
                    mixColumns();
                    for (i2 = 0; i2 < this.wordsInBlock; i2++) {
                        jArr4 = this.internalState;
                        jArr4[i2] = jArr4[i2] ^ jArr3[i2];
                    }
                    subBytes();
                    shiftRows();
                    mixColumns();
                    for (i2 = 0; i2 < this.wordsInBlock; i2++) {
                        jArr4 = this.internalState;
                        jArr4[i2] = jArr4[i2] + jArr3[i2];
                    }
                    System.arraycopy(this.internalState, 0, this.roundKeys[i], 0, this.wordsInBlock);
                    if (this.roundsAmount == i) {
                        return;
                    }
                }
                i += 2;
                j <<= 1;
                long j2 = obj[0];
                for (i2 = 1; i2 < obj.length; i2++) {
                    obj[i2 - 1] = obj[i2];
                }
                obj[obj.length - 1] = j2;
            } else {
                return;
            }
        }
    }

    private void workingKeyExpandKT(long[] jArr, long[] jArr2) {
        int i;
        Object obj = new long[this.wordsInBlock];
        Object obj2 = new long[this.wordsInBlock];
        this.internalState = new long[this.wordsInBlock];
        long[] jArr3 = this.internalState;
        jArr3[0] = jArr3[0] + ((long) ((this.wordsInBlock + this.wordsInKey) + 1));
        if (this.wordsInBlock == this.wordsInKey) {
            System.arraycopy(jArr, 0, obj, 0, obj.length);
            System.arraycopy(jArr, 0, obj2, 0, obj2.length);
        } else {
            System.arraycopy(jArr, 0, obj, 0, this.wordsInBlock);
            System.arraycopy(jArr, this.wordsInBlock, obj2, 0, this.wordsInBlock);
        }
        for (i = 0; i < this.internalState.length; i++) {
            jArr3 = this.internalState;
            jArr3[i] = jArr3[i] + obj[i];
        }
        subBytes();
        shiftRows();
        mixColumns();
        for (i = 0; i < this.internalState.length; i++) {
            jArr3 = this.internalState;
            jArr3[i] = jArr3[i] ^ obj2[i];
        }
        subBytes();
        shiftRows();
        mixColumns();
        for (i = 0; i < this.internalState.length; i++) {
            long[] jArr4 = this.internalState;
            jArr4[i] = jArr4[i] + obj[i];
        }
        subBytes();
        shiftRows();
        mixColumns();
        System.arraycopy(this.internalState, 0, jArr2, 0, this.wordsInBlock);
    }

    private void workingKeyExpandOdd() {
        for (int i = 1; i < this.roundsAmount; i += 2) {
            rotateLeft(this.roundKeys[i - 1], this.roundKeys[i]);
        }
    }

    private void xorRoundKey(int i) {
        long[] jArr = this.roundKeys[i];
        for (int i2 = 0; i2 < this.wordsInBlock; i2++) {
            long[] jArr2 = this.internalState;
            jArr2[i2] = jArr2[i2] ^ jArr[i2];
        }
    }

    public String getAlgorithmName() {
        return "DSTU7624";
    }

    public int getBlockSize() {
        return this.wordsInBlock << 3;
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x005b A:{LOOP_END, LOOP:0: B:21:0x0056->B:23:0x005b} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0088  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0071  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        if (cipherParameters instanceof KeyParameter) {
            this.forEncryption = z;
            byte[] key = ((KeyParameter) cipherParameters).getKey();
            int length = key.length << 3;
            int i = this.wordsInBlock << 6;
            if (length != 128 && length != 256 && length != 512) {
                throw new IllegalArgumentException("unsupported key length: only 128/256/512 are allowed");
            } else if (length == i || length == 2 * i) {
                int i2;
                if (length == 128) {
                    i = 10;
                } else if (length != 256) {
                    if (length == 512) {
                        i = 18;
                    }
                    this.wordsInKey = length >>> 6;
                    this.roundKeys = new long[(this.roundsAmount + 1)][];
                    for (i2 = 0; i2 < this.roundKeys.length; i2++) {
                        this.roundKeys[i2] = new long[this.wordsInBlock];
                    }
                    this.workingKey = new long[this.wordsInKey];
                    if (key.length != (length >>> 3)) {
                        Pack.littleEndianToLong(key, 0, this.workingKey);
                        long[] jArr = new long[this.wordsInBlock];
                        workingKeyExpandKT(this.workingKey, jArr);
                        workingKeyExpandEven(this.workingKey, jArr);
                        workingKeyExpandOdd();
                        return;
                    }
                    throw new IllegalArgumentException("Invalid key parameter passed to DSTU7624Engine init");
                } else {
                    i = 14;
                }
                this.roundsAmount = i;
                this.wordsInKey = length >>> 6;
                this.roundKeys = new long[(this.roundsAmount + 1)][];
                while (i2 < this.roundKeys.length) {
                }
                this.workingKey = new long[this.wordsInKey];
                if (key.length != (length >>> 3)) {
                }
            } else {
                throw new IllegalArgumentException("Unsupported key length");
            }
        }
        throw new IllegalArgumentException("Invalid parameter passed to DSTU7624Engine init");
    }

    public int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) throws DataLengthException, IllegalStateException {
        if (this.workingKey == null) {
            throw new IllegalStateException("DSTU7624 engine not initialised");
        } else if (getBlockSize() + i > bArr.length) {
            throw new DataLengthException("Input buffer too short");
        } else if (getBlockSize() + i2 <= bArr2.length) {
            int i3 = 0;
            if (this.forEncryption) {
                if (this.wordsInBlock != 2) {
                    Pack.littleEndianToLong(bArr, i, this.internalState);
                    addRoundKey(0);
                    while (true) {
                        subBytes();
                        shiftRows();
                        mixColumns();
                        i3++;
                        if (i3 == this.roundsAmount) {
                            break;
                        }
                        xorRoundKey(i3);
                    }
                    addRoundKey(this.roundsAmount);
                } else {
                    encryptBlock_128(bArr, i, bArr2, i2);
                    return getBlockSize();
                }
            } else if (this.wordsInBlock != 2) {
                Pack.littleEndianToLong(bArr, i, this.internalState);
                subRoundKey(this.roundsAmount);
                int i4 = this.roundsAmount;
                while (true) {
                    mixColumnsInv();
                    invShiftRows();
                    invSubBytes();
                    i4--;
                    if (i4 == 0) {
                        break;
                    }
                    xorRoundKey(i4);
                }
                subRoundKey(0);
            } else {
                decryptBlock_128(bArr, i, bArr2, i2);
                return getBlockSize();
            }
            Pack.longToLittleEndian(this.internalState, bArr2, i2);
            return getBlockSize();
        } else {
            throw new OutputLengthException("Output buffer too short");
        }
    }

    public void reset() {
        Arrays.fill(this.internalState, 0);
    }
}
