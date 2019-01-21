package android.hardware;

import java.util.GregorianCalendar;

public class GeomagneticField {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final long BASE_TIME = new GregorianCalendar(2015, 1, 1).getTimeInMillis();
    private static final float[][] DELTA_G;
    private static final float[][] DELTA_H;
    private static final float EARTH_REFERENCE_RADIUS_KM = 6371.2f;
    private static final float EARTH_SEMI_MAJOR_AXIS_KM = 6378.137f;
    private static final float EARTH_SEMI_MINOR_AXIS_KM = 6356.7524f;
    private static final float[][] G_COEFF;
    private static final float[][] H_COEFF;
    private static final float[][] SCHMIDT_QUASI_NORM_FACTORS = computeSchmidtQuasiNormFactors(G_COEFF.length);
    private float mGcLatitudeRad;
    private float mGcLongitudeRad;
    private float mGcRadiusKm;
    private float mX;
    private float mY;
    private float mZ;

    private static class LegendreTable {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        public final float[][] mP;
        public final float[][] mPDeriv;

        static {
            Class cls = GeomagneticField.class;
        }

        public LegendreTable(int maxN, float thetaRad) {
            float cos = (float) Math.cos((double) thetaRad);
            float sin = (float) Math.sin((double) thetaRad);
            this.mP = new float[(maxN + 1)][];
            this.mPDeriv = new float[(maxN + 1)][];
            this.mP[0] = new float[]{1.0f};
            this.mPDeriv[0] = new float[]{0.0f};
            int n = 1;
            while (n <= maxN) {
                this.mP[n] = new float[(n + 1)];
                this.mPDeriv[n] = new float[(n + 1)];
                int m = 0;
                while (m <= n) {
                    if (n == m) {
                        this.mP[n][m] = this.mP[n - 1][m - 1] * sin;
                        this.mPDeriv[n][m] = (this.mP[n - 1][m - 1] * cos) + (this.mPDeriv[n - 1][m - 1] * sin);
                    } else if (n == 1 || m == n - 1) {
                        this.mP[n][m] = this.mP[n - 1][m] * cos;
                        this.mPDeriv[n][m] = ((-sin) * this.mP[n - 1][m]) + (this.mPDeriv[n - 1][m] * cos);
                    } else {
                        float k = ((float) (((n - 1) * (n - 1)) - (m * m))) / ((float) (((2 * n) - 1) * ((2 * n) - 3)));
                        this.mP[n][m] = (this.mP[n - 1][m] * cos) - (this.mP[n - 2][m] * k);
                        this.mPDeriv[n][m] = (((-sin) * this.mP[n - 1][m]) + (this.mPDeriv[n - 1][m] * cos)) - (this.mPDeriv[n - 2][m] * k);
                    }
                    m++;
                }
                n++;
            }
        }
    }

    static {
        r1 = new float[13][];
        r1[0] = new float[]{0.0f};
        r1[1] = new float[]{-29438.5f, -1501.1f};
        r1[2] = new float[]{-2445.3f, 3012.5f, 1676.6f};
        r1[3] = new float[]{1351.1f, -2352.3f, 1225.6f, 581.9f};
        r1[4] = new float[]{907.2f, 813.7f, 120.3f, -335.0f, 70.3f};
        r1[5] = new float[]{-232.6f, 360.1f, 192.4f, -141.0f, -157.4f, 4.3f};
        r1[6] = new float[]{69.5f, 67.4f, 72.8f, -129.8f, -29.0f, 13.2f, -70.9f};
        r1[7] = new float[]{81.6f, -76.1f, -6.8f, 51.9f, 15.0f, 9.3f, -2.8f, 6.7f};
        r1[8] = new float[]{24.0f, 8.6f, -16.9f, -3.2f, -20.6f, 13.3f, 11.7f, -16.0f, -2.0f};
        r1[9] = new float[]{5.4f, 8.8f, 3.1f, -3.1f, 0.6f, -13.3f, -0.1f, 8.7f, -9.1f, -10.5f};
        r1[10] = new float[]{-1.9f, -6.5f, 0.2f, 0.6f, -0.6f, 1.7f, -0.7f, 2.1f, 2.3f, -1.8f, -3.6f};
        r1[11] = new float[]{3.1f, -1.5f, -2.3f, 2.1f, -0.9f, 0.6f, -0.7f, 0.2f, 1.7f, -0.2f, 0.4f, 3.5f};
        r1[12] = new float[]{-2.0f, -0.3f, 0.4f, 1.3f, -0.9f, 0.9f, 0.1f, 0.5f, -0.4f, -0.4f, 0.2f, -0.9f, 0.0f};
        G_COEFF = r1;
        r1 = new float[13][];
        r1[0] = new float[]{0.0f};
        r1[1] = new float[]{0.0f, 4796.2f};
        r1[2] = new float[]{0.0f, -2845.6f, -642.0f};
        r1[3] = new float[]{0.0f, -115.3f, 245.0f, -538.3f};
        r1[4] = new float[]{0.0f, 283.4f, -188.6f, 180.9f, -329.5f};
        r1[5] = new float[]{0.0f, 47.4f, 196.9f, -119.4f, 16.1f, 100.1f};
        r1[6] = new float[]{0.0f, -20.7f, 33.2f, 58.8f, -66.5f, 7.3f, 62.5f};
        r1[7] = new float[]{0.0f, -54.1f, -19.4f, 5.6f, 24.4f, 3.3f, -27.5f, -2.3f};
        r1[8] = new float[]{0.0f, 10.2f, -18.1f, 13.2f, -14.6f, 16.2f, 5.7f, -9.1f, 2.2f};
        r1[9] = new float[]{0.0f, -21.6f, 10.8f, 11.7f, -6.8f, -6.9f, 7.8f, 1.0f, -3.9f, 8.5f};
        r1[10] = new float[]{0.0f, 3.3f, -0.3f, 4.6f, 4.4f, -7.9f, -0.6f, -4.1f, -2.8f, -1.1f, -8.7f};
        r1[11] = new float[]{0.0f, -0.1f, 2.1f, -0.7f, -1.1f, 0.7f, -0.2f, -2.1f, -1.5f, -2.5f, -2.0f, -2.3f};
        r1[12] = new float[]{0.0f, -1.0f, 0.5f, 1.8f, -2.2f, 0.3f, 0.7f, -0.1f, 0.3f, 0.2f, -0.9f, -0.2f, 0.7f};
        H_COEFF = r1;
        r1 = new float[13][];
        r1[0] = new float[]{0.0f};
        r1[1] = new float[]{10.7f, 17.9f};
        r1[2] = new float[]{-8.6f, -3.3f, 2.4f};
        r1[3] = new float[]{3.1f, -6.2f, -0.4f, -10.4f};
        r1[4] = new float[]{-0.4f, 0.8f, -9.2f, 4.0f, -4.2f};
        r1[5] = new float[]{-0.2f, 0.1f, -1.4f, 0.0f, 1.3f, 3.8f};
        r1[6] = new float[]{-0.5f, -0.2f, -0.6f, 2.4f, -1.1f, 0.3f, 1.5f};
        r1[7] = new float[]{0.2f, -0.2f, -0.4f, 1.3f, 0.2f, -0.4f, -0.9f, 0.3f};
        r1[8] = new float[]{0.0f, 0.1f, -0.5f, 0.5f, -0.2f, 0.4f, 0.2f, -0.4f, 0.3f};
        r1[9] = new float[]{0.0f, -0.1f, -0.1f, 0.4f, -0.5f, -0.2f, 0.1f, 0.0f, -0.2f, -0.1f};
        r1[10] = new float[]{0.0f, 0.0f, -0.1f, 0.3f, -0.1f, -0.1f, -0.1f, 0.0f, -0.2f, -0.1f, -0.2f};
        r1[11] = new float[]{0.0f, 0.0f, -0.1f, 0.1f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.1f, -0.1f};
        r1[12] = new float[]{0.1f, 0.0f, 0.0f, 0.1f, -0.1f, 0.0f, 0.1f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        DELTA_G = r1;
        r1 = new float[13][];
        r1[0] = new float[]{0.0f};
        r1[1] = new float[]{0.0f, -26.8f};
        r1[2] = new float[]{0.0f, -27.1f, -13.3f};
        r1[3] = new float[]{0.0f, 8.4f, -0.4f, 2.3f};
        r1[4] = new float[]{0.0f, -0.6f, 5.3f, 3.0f, -5.3f};
        r1[5] = new float[]{0.0f, 0.4f, 1.6f, -1.1f, 3.3f, 0.1f};
        r1[6] = new float[]{0.0f, 0.0f, -2.2f, -0.7f, 0.1f, 1.0f, 1.3f};
        r1[7] = new float[]{0.0f, 0.7f, 0.5f, -0.2f, -0.1f, -0.7f, 0.1f, 0.1f};
        r1[8] = new float[]{0.0f, -0.3f, 0.3f, 0.3f, 0.6f, -0.1f, -0.2f, 0.3f, 0.0f};
        r1[9] = new float[]{0.0f, -0.2f, -0.1f, -0.2f, 0.1f, 0.1f, 0.0f, -0.2f, 0.4f, 0.3f};
        r1[10] = new float[]{0.0f, 0.1f, -0.1f, 0.0f, 0.0f, -0.2f, 0.1f, -0.1f, -0.2f, 0.1f, -0.1f};
        r1[11] = new float[]{0.0f, 0.0f, 0.1f, 0.0f, 0.1f, 0.0f, 0.0f, 0.1f, 0.0f, -0.1f, 0.0f, -0.1f};
        r1[12] = new float[]{0.0f, 0.0f, 0.0f, -0.1f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        DELTA_H = r1;
    }

    public GeomagneticField(float gdLatitudeDeg, float gdLongitudeDeg, float altitudeMeters, long timeMillis) {
        int MAX_N = G_COEFF.length;
        float gdLatitudeDeg2 = Math.min(89.99999f, Math.max(-89.99999f, gdLatitudeDeg));
        computeGeocentricCoordinates(gdLatitudeDeg2, gdLongitudeDeg, altitudeMeters);
        LegendreTable legendre = new LegendreTable(MAX_N - 1, (float) (1.5707963267948966d - ((double) this.mGcLatitudeRad)));
        float[] relativeRadiusPower = new float[(MAX_N + 2)];
        relativeRadiusPower[0] = 1.0f;
        int n = 1;
        relativeRadiusPower[1] = EARTH_REFERENCE_RADIUS_KM / this.mGcRadiusKm;
        int m = 2;
        for (int i = 2; i < relativeRadiusPower.length; i++) {
            relativeRadiusPower[i] = relativeRadiusPower[i - 1] * relativeRadiusPower[1];
        }
        float[] sinMLon = new float[MAX_N];
        float[] cosMLon = new float[MAX_N];
        sinMLon[0] = 0.0f;
        cosMLon[0] = 1.0f;
        sinMLon[1] = (float) Math.sin((double) this.mGcLongitudeRad);
        cosMLon[1] = (float) Math.cos((double) this.mGcLongitudeRad);
        while (m < MAX_N) {
            int x = m >> 1;
            sinMLon[m] = (sinMLon[m - x] * cosMLon[x]) + (cosMLon[m - x] * sinMLon[x]);
            cosMLon[m] = (cosMLon[m - x] * cosMLon[x]) - (sinMLon[m - x] * sinMLon[x]);
            m++;
        }
        float inverseCosLatitude = 1.0f / ((float) Math.cos((double) this.mGcLatitudeRad));
        float yearsSinceBase = ((float) (timeMillis - BASE_TIME)) / 3.1536001E10f;
        float gcX = 0.0f;
        float gcY = 0.0f;
        float gcZ = 0.0f;
        while (n < MAX_N) {
            float f;
            float gcZ2 = gcZ;
            gcZ = gcY;
            int m2 = 0;
            while (m2 <= n) {
                float g = G_COEFF[n][m2] + (DELTA_G[n][m2] * yearsSinceBase);
                float h = H_COEFF[n][m2] + (DELTA_H[n][m2] * yearsSinceBase);
                gcX += ((relativeRadiusPower[n + 2] * ((cosMLon[m2] * g) + (sinMLon[m2] * h))) * legendre.mPDeriv[n][m2]) * SCHMIDT_QUASI_NORM_FACTORS[n][m2];
                gcZ += ((((relativeRadiusPower[n + 2] * ((float) m2)) * ((sinMLon[m2] * g) - (cosMLon[m2] * h))) * legendre.mP[n][m2]) * SCHMIDT_QUASI_NORM_FACTORS[n][m2]) * inverseCosLatitude;
                gcZ2 -= (((((float) (n + 1)) * relativeRadiusPower[n + 2]) * ((cosMLon[m2] * g) + (sinMLon[m2] * h))) * legendre.mP[n][m2]) * SCHMIDT_QUASI_NORM_FACTORS[n][m2];
                m2++;
                MAX_N = MAX_N;
                f = gdLongitudeDeg;
            }
            n++;
            gcY = gcZ;
            gcZ = gcZ2;
            f = gdLongitudeDeg;
        }
        double latDiffRad = Math.toRadians((double) gdLatitudeDeg2) - ((double) this.mGcLatitudeRad);
        this.mX = (float) ((((double) gcX) * Math.cos(latDiffRad)) + (((double) gcZ) * Math.sin(latDiffRad)));
        this.mY = gcY;
        this.mZ = (float) ((((double) (-gcX)) * Math.sin(latDiffRad)) + (((double) gcZ) * Math.cos(latDiffRad)));
    }

    public float getX() {
        return this.mX;
    }

    public float getY() {
        return this.mY;
    }

    public float getZ() {
        return this.mZ;
    }

    public float getDeclination() {
        return (float) Math.toDegrees(Math.atan2((double) this.mY, (double) this.mX));
    }

    public float getInclination() {
        return (float) Math.toDegrees(Math.atan2((double) this.mZ, (double) getHorizontalStrength()));
    }

    public float getHorizontalStrength() {
        return (float) Math.hypot((double) this.mX, (double) this.mY);
    }

    public float getFieldStrength() {
        return (float) Math.sqrt((double) (((this.mX * this.mX) + (this.mY * this.mY)) + (this.mZ * this.mZ)));
    }

    private void computeGeocentricCoordinates(float gdLatitudeDeg, float gdLongitudeDeg, float altitudeMeters) {
        float altitudeKm = altitudeMeters / 1000.0f;
        double gdLatRad = Math.toRadians((double) gdLatitudeDeg);
        float clat = (float) Math.cos(gdLatRad);
        float slat = (float) Math.sin(gdLatRad);
        float latRad = (float) Math.sqrt((double) (((4.0680636E7f * clat) * clat) + ((4.04083E7f * slat) * slat)));
        this.mGcLatitudeRad = (float) Math.atan((double) ((((latRad * altitudeKm) + 4.04083E7f) * (slat / clat)) / ((latRad * altitudeKm) + 4.0680636E7f)));
        this.mGcLongitudeRad = (float) Math.toRadians((double) gdLongitudeDeg);
        this.mGcRadiusKm = (float) Math.sqrt((double) (((altitudeKm * altitudeKm) + ((2.0f * altitudeKm) * ((float) Math.sqrt((double) (((4.0680636E7f * clat) * clat) + ((4.04083E7f * slat) * slat)))))) + (((((4.0680636E7f * 4.0680636E7f) * clat) * clat) + (((4.04083E7f * 4.04083E7f) * slat) * slat)) / (((4.0680636E7f * clat) * clat) + ((4.04083E7f * slat) * slat)))));
    }

    private static float[][] computeSchmidtQuasiNormFactors(int maxN) {
        float[][] schmidtQuasiNorm = new float[(maxN + 1)][];
        schmidtQuasiNorm[0] = new float[]{1.0f};
        for (int n = 1; n <= maxN; n++) {
            schmidtQuasiNorm[n] = new float[(n + 1)];
            schmidtQuasiNorm[n][0] = (schmidtQuasiNorm[n - 1][0] * ((float) ((2 * n) - 1))) / ((float) n);
            int m = 1;
            while (m <= n) {
                schmidtQuasiNorm[n][m] = schmidtQuasiNorm[n][m - 1] * ((float) Math.sqrt((double) (((float) (((n - m) + 1) * (m == 1 ? 2 : 1))) / ((float) (n + m)))));
                m++;
            }
        }
        return schmidtQuasiNorm;
    }
}
