package android.renderscript;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class ScriptIntrinsicBLAS extends ScriptIntrinsic {
    public static final int CONJ_TRANSPOSE = 113;
    public static final int LEFT = 141;
    public static final int LOWER = 122;
    public static final int NON_UNIT = 131;
    public static final int NO_TRANSPOSE = 111;
    public static final int RIGHT = 142;
    private static final int RsBlas_bnnm = 1000;
    private static final int RsBlas_caxpy = 29;
    private static final int RsBlas_ccopy = 28;
    private static final int RsBlas_cdotc_sub = 6;
    private static final int RsBlas_cdotu_sub = 5;
    private static final int RsBlas_cgbmv = 64;
    private static final int RsBlas_cgemm = 125;
    private static final int RsBlas_cgemv = 63;
    private static final int RsBlas_cgerc = 99;
    private static final int RsBlas_cgeru = 98;
    private static final int RsBlas_chbmv = 96;
    private static final int RsBlas_chemm = 137;
    private static final int RsBlas_chemv = 95;
    private static final int RsBlas_cher = 100;
    private static final int RsBlas_cher2 = 102;
    private static final int RsBlas_cher2k = 139;
    private static final int RsBlas_cherk = 138;
    private static final int RsBlas_chpmv = 97;
    private static final int RsBlas_chpr = 101;
    private static final int RsBlas_chpr2 = 103;
    private static final int RsBlas_cscal = 43;
    private static final int RsBlas_csscal = 45;
    private static final int RsBlas_cswap = 27;
    private static final int RsBlas_csymm = 126;
    private static final int RsBlas_csyr2k = 128;
    private static final int RsBlas_csyrk = 127;
    private static final int RsBlas_ctbmv = 66;
    private static final int RsBlas_ctbsv = 69;
    private static final int RsBlas_ctpmv = 67;
    private static final int RsBlas_ctpsv = 70;
    private static final int RsBlas_ctrmm = 129;
    private static final int RsBlas_ctrmv = 65;
    private static final int RsBlas_ctrsm = 130;
    private static final int RsBlas_ctrsv = 68;
    private static final int RsBlas_dasum = 12;
    private static final int RsBlas_daxpy = 26;
    private static final int RsBlas_dcopy = 25;
    private static final int RsBlas_ddot = 4;
    private static final int RsBlas_dgbmv = 56;
    private static final int RsBlas_dgemm = 119;
    private static final int RsBlas_dgemv = 55;
    private static final int RsBlas_dger = 90;
    private static final int RsBlas_dnrm2 = 11;
    private static final int RsBlas_drot = 39;
    private static final int RsBlas_drotg = 37;
    private static final int RsBlas_drotm = 40;
    private static final int RsBlas_drotmg = 38;
    private static final int RsBlas_dsbmv = 88;
    private static final int RsBlas_dscal = 42;
    private static final int RsBlas_dsdot = 2;
    private static final int RsBlas_dspmv = 89;
    private static final int RsBlas_dspr = 92;
    private static final int RsBlas_dspr2 = 94;
    private static final int RsBlas_dswap = 24;
    private static final int RsBlas_dsymm = 120;
    private static final int RsBlas_dsymv = 87;
    private static final int RsBlas_dsyr = 91;
    private static final int RsBlas_dsyr2 = 93;
    private static final int RsBlas_dsyr2k = 122;
    private static final int RsBlas_dsyrk = 121;
    private static final int RsBlas_dtbmv = 58;
    private static final int RsBlas_dtbsv = 61;
    private static final int RsBlas_dtpmv = 59;
    private static final int RsBlas_dtpsv = 62;
    private static final int RsBlas_dtrmm = 123;
    private static final int RsBlas_dtrmv = 57;
    private static final int RsBlas_dtrsm = 124;
    private static final int RsBlas_dtrsv = 60;
    private static final int RsBlas_dzasum = 16;
    private static final int RsBlas_dznrm2 = 15;
    private static final int RsBlas_icamax = 19;
    private static final int RsBlas_idamax = 18;
    private static final int RsBlas_isamax = 17;
    private static final int RsBlas_izamax = 20;
    private static final int RsBlas_sasum = 10;
    private static final int RsBlas_saxpy = 23;
    private static final int RsBlas_scasum = 14;
    private static final int RsBlas_scnrm2 = 13;
    private static final int RsBlas_scopy = 22;
    private static final int RsBlas_sdot = 3;
    private static final int RsBlas_sdsdot = 1;
    private static final int RsBlas_sgbmv = 48;
    private static final int RsBlas_sgemm = 113;
    private static final int RsBlas_sgemv = 47;
    private static final int RsBlas_sger = 82;
    private static final int RsBlas_snrm2 = 9;
    private static final int RsBlas_srot = 35;
    private static final int RsBlas_srotg = 33;
    private static final int RsBlas_srotm = 36;
    private static final int RsBlas_srotmg = 34;
    private static final int RsBlas_ssbmv = 80;
    private static final int RsBlas_sscal = 41;
    private static final int RsBlas_sspmv = 81;
    private static final int RsBlas_sspr = 84;
    private static final int RsBlas_sspr2 = 86;
    private static final int RsBlas_sswap = 21;
    private static final int RsBlas_ssymm = 114;
    private static final int RsBlas_ssymv = 79;
    private static final int RsBlas_ssyr = 83;
    private static final int RsBlas_ssyr2 = 85;
    private static final int RsBlas_ssyr2k = 116;
    private static final int RsBlas_ssyrk = 115;
    private static final int RsBlas_stbmv = 50;
    private static final int RsBlas_stbsv = 53;
    private static final int RsBlas_stpmv = 51;
    private static final int RsBlas_stpsv = 54;
    private static final int RsBlas_strmm = 117;
    private static final int RsBlas_strmv = 49;
    private static final int RsBlas_strsm = 118;
    private static final int RsBlas_strsv = 52;
    private static final int RsBlas_zaxpy = 32;
    private static final int RsBlas_zcopy = 31;
    private static final int RsBlas_zdotc_sub = 8;
    private static final int RsBlas_zdotu_sub = 7;
    private static final int RsBlas_zdscal = 46;
    private static final int RsBlas_zgbmv = 72;
    private static final int RsBlas_zgemm = 131;
    private static final int RsBlas_zgemv = 71;
    private static final int RsBlas_zgerc = 108;
    private static final int RsBlas_zgeru = 107;
    private static final int RsBlas_zhbmv = 105;
    private static final int RsBlas_zhemm = 140;
    private static final int RsBlas_zhemv = 104;
    private static final int RsBlas_zher = 109;
    private static final int RsBlas_zher2 = 111;
    private static final int RsBlas_zher2k = 142;
    private static final int RsBlas_zherk = 141;
    private static final int RsBlas_zhpmv = 106;
    private static final int RsBlas_zhpr = 110;
    private static final int RsBlas_zhpr2 = 112;
    private static final int RsBlas_zscal = 44;
    private static final int RsBlas_zswap = 30;
    private static final int RsBlas_zsymm = 132;
    private static final int RsBlas_zsyr2k = 134;
    private static final int RsBlas_zsyrk = 133;
    private static final int RsBlas_ztbmv = 74;
    private static final int RsBlas_ztbsv = 77;
    private static final int RsBlas_ztpmv = 75;
    private static final int RsBlas_ztpsv = 78;
    private static final int RsBlas_ztrmm = 135;
    private static final int RsBlas_ztrmv = 73;
    private static final int RsBlas_ztrsm = 136;
    private static final int RsBlas_ztrsv = 76;
    public static final int TRANSPOSE = 112;
    public static final int UNIT = 132;
    public static final int UPPER = 121;
    private Allocation mLUT;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Diag {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Side {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Transpose {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Uplo {
    }

    private ScriptIntrinsicBLAS(long id, RenderScript rs) {
        super(id, rs);
    }

    public static ScriptIntrinsicBLAS create(RenderScript rs) {
        return new ScriptIntrinsicBLAS(rs.nScriptIntrinsicCreate(13, Element.U32(rs).getID(rs)), rs);
    }

    static void validateSide(int Side) {
        if (Side != 141 && Side != 142) {
            throw new RSRuntimeException("Invalid side passed to BLAS");
        }
    }

    static void validateTranspose(int Trans) {
        if (Trans != 111 && Trans != 112 && Trans != 113) {
            throw new RSRuntimeException("Invalid transpose passed to BLAS");
        }
    }

    static void validateConjTranspose(int Trans) {
        if (Trans != 111 && Trans != 113) {
            throw new RSRuntimeException("Invalid transpose passed to BLAS");
        }
    }

    static void validateDiag(int Diag) {
        if (Diag != 131 && Diag != 132) {
            throw new RSRuntimeException("Invalid diag passed to BLAS");
        }
    }

    static void validateUplo(int Uplo) {
        if (Uplo != 121 && Uplo != 122) {
            throw new RSRuntimeException("Invalid uplo passed to BLAS");
        }
    }

    static void validateGEMV(Element e, int TransA, Allocation A, Allocation X, int incX, Allocation Y, int incY) {
        validateTranspose(TransA);
        int M = A.getType().getY();
        int N = A.getType().getX();
        if (!A.getType().getElement().isCompatible(e) || !X.getType().getElement().isCompatible(e) || !Y.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        } else if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            throw new RSRuntimeException("BLAS vectors must have Y dimension of 0 or 1");
        } else if (incX <= 0 || incY <= 0) {
            throw new RSRuntimeException("Vector increments must be greater than 0");
        } else {
            int expectedXDim;
            int expectedYDim;
            if (TransA == 111) {
                expectedXDim = ((N - 1) * incX) + 1;
                expectedYDim = 1 + ((M - 1) * incY);
            } else {
                expectedXDim = ((M - 1) * incX) + 1;
                expectedYDim = 1 + ((N - 1) * incY);
            }
            if (X.getType().getX() != expectedXDim || Y.getType().getX() != expectedYDim) {
                throw new RSRuntimeException("Incorrect vector dimensions for GEMV");
            }
        }
    }

    public void SGEMV(int TransA, float alpha, Allocation A, Allocation X, int incX, float beta, Allocation Y, int incY) {
        validateGEMV(Element.F32(this.mRS), TransA, A, X, incX, Y, incY);
        int M = A.getType().getY();
        int N = A.getType().getX();
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 47, TransA, 0, 0, 0, 0, M, N, 0, alpha, A.getID(this.mRS), X.getID(this.mRS), beta, Y.getID(this.mRS), incX, incY, 0, 0);
    }

    public void DGEMV(int TransA, double alpha, Allocation A, Allocation X, int incX, double beta, Allocation Y, int incY) {
        validateGEMV(Element.F64(this.mRS), TransA, A, X, incX, Y, incY);
        int M = A.getType().getY();
        int N = A.getType().getX();
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 55, TransA, 0, 0, 0, 0, M, N, 0, alpha, A.getID(this.mRS), X.getID(this.mRS), beta, Y.getID(this.mRS), incX, incY, 0, 0);
    }

    public void CGEMV(int TransA, Float2 alpha, Allocation A, Allocation X, int incX, Float2 beta, Allocation Y, int incY) {
        Float2 float2 = alpha;
        Float2 float22 = beta;
        Allocation allocation = Y;
        validateGEMV(Element.F32_2(this.mRS), TransA, A, X, incX, allocation, incY);
        int M = A.getType().getY();
        int N = A.getType().getX();
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        float f = float2.x;
        float f2 = float2.y;
        long id2 = A.getID(this.mRS);
        long id3 = X.getID(this.mRS);
        float f3 = float22.x;
        float f4 = float22.y;
        float f5 = f4;
        float f6 = f3;
        float f7 = f2;
        float f8 = f;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 63, TransA, 0, 0, 0, 0, M, N, 0, f8, f7, id2, id3, f6, f5, allocation.getID(this.mRS), incX, incY, 0, 0);
    }

    public void ZGEMV(int TransA, Double2 alpha, Allocation A, Allocation X, int incX, Double2 beta, Allocation Y, int incY) {
        Double2 double2 = alpha;
        Double2 double22 = beta;
        validateGEMV(Element.F64_2(this.mRS), TransA, A, X, incX, Y, incY);
        int M = A.getType().getY();
        int N = A.getType().getX();
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        double d = double2.x;
        double d2 = double2.y;
        long id2 = A.getID(this.mRS);
        long id3 = X.getID(this.mRS);
        double d3 = double22.x;
        double d4 = d2;
        d2 = double22.y;
        double d5 = d;
        long id4 = Y.getID(this.mRS);
        int i = N;
        int i2 = 0;
        renderScript.nScriptIntrinsicBLAS_Z(id, 71, TransA, 0, 0, 0, 0, M, i, i2, d5, d4, id2, id3, d3, d2, id4, incX, incY, 0, 0);
    }

    public void SGBMV(int TransA, int KL, int KU, float alpha, Allocation A, Allocation X, int incX, float beta, Allocation Y, int incY) {
        validateGEMV(Element.F32(this.mRS), TransA, A, X, incX, Y, incY);
        if (KL < 0 || KU < 0) {
            throw new RSRuntimeException("KL and KU must be greater than or equal to 0");
        }
        int M = A.getType().getY();
        int N = A.getType().getX();
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 48, TransA, 0, 0, 0, 0, M, N, 0, alpha, A.getID(this.mRS), X.getID(this.mRS), beta, Y.getID(this.mRS), incX, incY, KL, KU);
    }

    public void DGBMV(int TransA, int KL, int KU, double alpha, Allocation A, Allocation X, int incX, double beta, Allocation Y, int incY) {
        validateGEMV(Element.F64(this.mRS), TransA, A, X, incX, Y, incY);
        if (KL < 0 || KU < 0) {
            throw new RSRuntimeException("KL and KU must be greater than or equal to 0");
        }
        int M = A.getType().getY();
        int N = A.getType().getX();
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 56, TransA, 0, 0, 0, 0, M, N, 0, alpha, A.getID(this.mRS), X.getID(this.mRS), beta, Y.getID(this.mRS), incX, incY, KL, KU);
    }

    public void CGBMV(int TransA, int KL, int KU, Float2 alpha, Allocation A, Allocation X, int incX, Float2 beta, Allocation Y, int incY) {
        Float2 float2 = alpha;
        Float2 float22 = beta;
        validateGEMV(Element.F32_2(this.mRS), TransA, A, X, incX, Y, incY);
        if (KL < 0 || KU < 0) {
            throw new RSRuntimeException("KL and KU must be greater than or equal to 0");
        }
        int M = A.getType().getY();
        int N = A.getType().getX();
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        float f = float2.x;
        float f2 = float2.y;
        long id2 = A.getID(this.mRS);
        long id3 = X.getID(this.mRS);
        float f3 = float22.x;
        float f4 = float22.y;
        float f5 = f3;
        int i = TransA;
        float f6 = f4;
        float f7 = f2;
        int i2 = M;
        float f8 = f;
        int i3 = N;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 64, i, 0, 0, 0, 0, i2, i3, 0, f8, f7, id2, id3, f5, f6, Y.getID(this.mRS), incX, incY, KL, KU);
    }

    public void ZGBMV(int TransA, int KL, int KU, Double2 alpha, Allocation A, Allocation X, int incX, Double2 beta, Allocation Y, int incY) {
        Double2 double2 = alpha;
        Double2 double22 = beta;
        validateGEMV(Element.F64_2(this.mRS), TransA, A, X, incX, Y, incY);
        if (KL < 0 || KU < 0) {
            throw new RSRuntimeException("KL and KU must be greater than or equal to 0");
        }
        int M = A.getType().getY();
        int N = A.getType().getX();
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        double d = double2.x;
        double d2 = double2.y;
        double d3 = d2;
        double d4 = d;
        int i = M;
        int i2 = N;
        renderScript.nScriptIntrinsicBLAS_Z(id, 72, TransA, 0, 0, 0, 0, i, i2, 0, d4, d3, A.getID(this.mRS), X.getID(this.mRS), double22.x, double22.y, Y.getID(this.mRS), incX, incY, KL, KU);
    }

    static void validateTRMV(Element e, int Uplo, int TransA, int Diag, Allocation A, Allocation X, int incX) {
        validateTranspose(TransA);
        validateUplo(Uplo);
        validateDiag(Diag);
        int N = A.getType().getY();
        if (A.getType().getX() != N) {
            throw new RSRuntimeException("A must be a square matrix for TRMV");
        } else if (!A.getType().getElement().isCompatible(e) || !X.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        } else if (X.getType().getY() > 1) {
            throw new RSRuntimeException("BLAS vectors must have Y dimension of 0 or 1");
        } else if (incX > 0) {
            if (X.getType().getX() != 1 + ((N - 1) * incX)) {
                throw new RSRuntimeException("Incorrect vector dimensions for TRMV");
            }
        } else {
            throw new RSRuntimeException("Vector increments must be greater than 0");
        }
    }

    static int validateTPMV(Element e, int Uplo, int TransA, int Diag, Allocation Ap, Allocation X, int incX) {
        validateTranspose(TransA);
        validateUplo(Uplo);
        validateDiag(Diag);
        if (!Ap.getType().getElement().isCompatible(e) || !X.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        } else if (X.getType().getY() > 1) {
            throw new RSRuntimeException("BLAS vectors must have Y dimension of 0 or 1");
        } else if (Ap.getType().getY() <= 1) {
            int N = (int) Math.sqrt(((double) Ap.getType().getX()) * 2.0d);
            if (Ap.getType().getX() != ((N + 1) * N) / 2) {
                throw new RSRuntimeException("Invalid dimension for Ap");
            } else if (incX > 0) {
                if (X.getType().getX() == 1 + ((N - 1) * incX)) {
                    return N;
                }
                throw new RSRuntimeException("Incorrect vector dimensions for TPMV");
            } else {
                throw new RSRuntimeException("Vector increments must be greater than 0");
            }
        } else {
            throw new RSRuntimeException("Ap must have a Y dimension of 0 or 1");
        }
    }

    public void STRMV(int Uplo, int TransA, int Diag, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F32(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int N = A.getType().getY();
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 49, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0f, A.getID(this.mRS), X.getID(this.mRS), 0.0f, 0, incX, 0, 0, 0);
    }

    public void DTRMV(int Uplo, int TransA, int Diag, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F64(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int i = TransA;
        int i2 = Uplo;
        int i3 = Diag;
        int y = A.getType().getY();
        double d = 0.0d;
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 57, i, 0, 0, i2, i3, 0, y, 0, d, A.getID(this.mRS), X.getID(this.mRS), 0.0d, 0, incX, 0, 0, 0);
    }

    public void CTRMV(int Uplo, int TransA, int Diag, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F32_2(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int N = A.getType().getY();
        this.mRS.nScriptIntrinsicBLAS_Complex(getID(this.mRS), 65, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0f, 0.0f, A.getID(this.mRS), X.getID(this.mRS), 0.0f, 0.0f, 0, incX, 0, 0, 0);
    }

    public void ZTRMV(int Uplo, int TransA, int Diag, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F64_2(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int i = TransA;
        int i2 = Uplo;
        int i3 = Diag;
        int y = A.getType().getY();
        double d = 0.0d;
        double d2 = 0.0d;
        this.mRS.nScriptIntrinsicBLAS_Z(getID(this.mRS), 73, i, 0, 0, i2, i3, 0, y, 0, d, d2, A.getID(this.mRS), X.getID(this.mRS), 0.0d, 0.0d, 0, incX, 0, 0, 0);
    }

    public void STBMV(int Uplo, int TransA, int Diag, int K, Allocation A, Allocation X, int incX) {
        if (K >= 0) {
            validateTRMV(Element.F32(this.mRS), Uplo, TransA, Diag, A, X, incX);
            int N = A.getType().getY();
            this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 50, TransA, 0, 0, Uplo, Diag, 0, N, K, 0.0f, A.getID(this.mRS), X.getID(this.mRS), 0.0f, 0, incX, 0, 0, 0);
            return;
        }
        throw new RSRuntimeException("K must be greater than or equal to 0");
    }

    public void DTBMV(int Uplo, int TransA, int Diag, int K, Allocation A, Allocation X, int incX) {
        if (K >= 0) {
            validateTRMV(Element.F64(this.mRS), Uplo, TransA, Diag, A, X, incX);
            int N = A.getType().getY();
            this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 58, TransA, 0, 0, Uplo, Diag, 0, N, K, 0.0d, A.getID(this.mRS), X.getID(this.mRS), 0.0d, 0, incX, 0, 0, 0);
            return;
        }
        throw new RSRuntimeException("K must be greater than or equal to 0");
    }

    public void CTBMV(int Uplo, int TransA, int Diag, int K, Allocation A, Allocation X, int incX) {
        if (K >= 0) {
            validateTRMV(Element.F32_2(this.mRS), Uplo, TransA, Diag, A, X, incX);
            int N = A.getType().getY();
            this.mRS.nScriptIntrinsicBLAS_Complex(getID(this.mRS), 66, TransA, 0, 0, Uplo, Diag, 0, N, K, 0.0f, 0.0f, A.getID(this.mRS), X.getID(this.mRS), 0.0f, 0.0f, 0, incX, 0, 0, 0);
            return;
        }
        throw new RSRuntimeException("K must be greater than or equal to 0");
    }

    public void ZTBMV(int Uplo, int TransA, int Diag, int K, Allocation A, Allocation X, int incX) {
        if (K >= 0) {
            validateTRMV(Element.F64_2(this.mRS), Uplo, TransA, Diag, A, X, incX);
            int N = A.getType().getY();
            this.mRS.nScriptIntrinsicBLAS_Z(getID(this.mRS), 74, TransA, 0, 0, Uplo, Diag, 0, N, K, 0.0d, 0.0d, A.getID(this.mRS), X.getID(this.mRS), 0.0d, 0.0d, 0, incX, 0, 0, 0);
            return;
        }
        throw new RSRuntimeException("K must be greater than or equal to 0");
    }

    public void STPMV(int Uplo, int TransA, int Diag, Allocation Ap, Allocation X, int incX) {
        int N = validateTPMV(Element.F32(this.mRS), Uplo, TransA, Diag, Ap, X, incX);
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 51, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0f, Ap.getID(this.mRS), X.getID(this.mRS), 0.0f, 0, incX, 0, 0, 0);
    }

    public void DTPMV(int Uplo, int TransA, int Diag, Allocation Ap, Allocation X, int incX) {
        int N = validateTPMV(Element.F64(this.mRS), Uplo, TransA, Diag, Ap, X, incX);
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 59, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0d, Ap.getID(this.mRS), X.getID(this.mRS), 0.0d, 0, incX, 0, 0, 0);
    }

    public void CTPMV(int Uplo, int TransA, int Diag, Allocation Ap, Allocation X, int incX) {
        int N = validateTPMV(Element.F32_2(this.mRS), Uplo, TransA, Diag, Ap, X, incX);
        this.mRS.nScriptIntrinsicBLAS_Complex(getID(this.mRS), 67, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0f, 0.0f, Ap.getID(this.mRS), X.getID(this.mRS), 0.0f, 0.0f, 0, incX, 0, 0, 0);
    }

    public void ZTPMV(int Uplo, int TransA, int Diag, Allocation Ap, Allocation X, int incX) {
        int N = validateTPMV(Element.F64_2(this.mRS), Uplo, TransA, Diag, Ap, X, incX);
        this.mRS.nScriptIntrinsicBLAS_Z(getID(this.mRS), 75, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0d, 0.0d, Ap.getID(this.mRS), X.getID(this.mRS), 0.0d, 0.0d, 0, incX, 0, 0, 0);
    }

    public void STRSV(int Uplo, int TransA, int Diag, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F32(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int N = A.getType().getY();
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 52, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0f, A.getID(this.mRS), X.getID(this.mRS), 0.0f, 0, incX, 0, 0, 0);
    }

    public void DTRSV(int Uplo, int TransA, int Diag, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F64(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int i = TransA;
        int i2 = Uplo;
        int i3 = Diag;
        int y = A.getType().getY();
        double d = 0.0d;
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 60, i, 0, 0, i2, i3, 0, y, 0, d, A.getID(this.mRS), X.getID(this.mRS), 0.0d, 0, incX, 0, 0, 0);
    }

    public void CTRSV(int Uplo, int TransA, int Diag, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F32_2(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int N = A.getType().getY();
        this.mRS.nScriptIntrinsicBLAS_Complex(getID(this.mRS), 68, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0f, 0.0f, A.getID(this.mRS), X.getID(this.mRS), 0.0f, 0.0f, 0, incX, 0, 0, 0);
    }

    public void ZTRSV(int Uplo, int TransA, int Diag, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F64_2(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int i = TransA;
        int i2 = Uplo;
        int i3 = Diag;
        int y = A.getType().getY();
        double d = 0.0d;
        double d2 = 0.0d;
        this.mRS.nScriptIntrinsicBLAS_Z(getID(this.mRS), 76, i, 0, 0, i2, i3, 0, y, 0, d, d2, A.getID(this.mRS), X.getID(this.mRS), 0.0d, 0.0d, 0, incX, 0, 0, 0);
    }

    public void STBSV(int Uplo, int TransA, int Diag, int K, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F32(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int N = A.getType().getY();
        if (K >= 0) {
            this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 53, TransA, 0, 0, Uplo, Diag, 0, N, K, 0.0f, A.getID(this.mRS), X.getID(this.mRS), 0.0f, 0, incX, 0, 0, 0);
            return;
        }
        throw new RSRuntimeException("Number of diagonals must be positive");
    }

    public void DTBSV(int Uplo, int TransA, int Diag, int K, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F64(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int N = A.getType().getY();
        if (K >= 0) {
            this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 61, TransA, 0, 0, Uplo, Diag, 0, N, K, 0.0d, A.getID(this.mRS), X.getID(this.mRS), 0.0d, 0, incX, 0, 0, 0);
            return;
        }
        throw new RSRuntimeException("Number of diagonals must be positive");
    }

    public void CTBSV(int Uplo, int TransA, int Diag, int K, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F32_2(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int N = A.getType().getY();
        if (K >= 0) {
            this.mRS.nScriptIntrinsicBLAS_Complex(getID(this.mRS), 69, TransA, 0, 0, Uplo, Diag, 0, N, K, 0.0f, 0.0f, A.getID(this.mRS), X.getID(this.mRS), 0.0f, 0.0f, 0, incX, 0, 0, 0);
            return;
        }
        throw new RSRuntimeException("Number of diagonals must be positive");
    }

    public void ZTBSV(int Uplo, int TransA, int Diag, int K, Allocation A, Allocation X, int incX) {
        validateTRMV(Element.F64_2(this.mRS), Uplo, TransA, Diag, A, X, incX);
        int N = A.getType().getY();
        if (K >= 0) {
            this.mRS.nScriptIntrinsicBLAS_Z(getID(this.mRS), 77, TransA, 0, 0, Uplo, Diag, 0, N, K, 0.0d, 0.0d, A.getID(this.mRS), X.getID(this.mRS), 0.0d, 0.0d, 0, incX, 0, 0, 0);
            return;
        }
        throw new RSRuntimeException("Number of diagonals must be positive");
    }

    public void STPSV(int Uplo, int TransA, int Diag, Allocation Ap, Allocation X, int incX) {
        int N = validateTPMV(Element.F32(this.mRS), Uplo, TransA, Diag, Ap, X, incX);
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 54, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0f, Ap.getID(this.mRS), X.getID(this.mRS), 0.0f, 0, incX, 0, 0, 0);
    }

    public void DTPSV(int Uplo, int TransA, int Diag, Allocation Ap, Allocation X, int incX) {
        int N = validateTPMV(Element.F64(this.mRS), Uplo, TransA, Diag, Ap, X, incX);
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 62, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0d, Ap.getID(this.mRS), X.getID(this.mRS), 0.0d, 0, incX, 0, 0, 0);
    }

    public void CTPSV(int Uplo, int TransA, int Diag, Allocation Ap, Allocation X, int incX) {
        int N = validateTPMV(Element.F32_2(this.mRS), Uplo, TransA, Diag, Ap, X, incX);
        this.mRS.nScriptIntrinsicBLAS_Complex(getID(this.mRS), 70, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0f, 0.0f, Ap.getID(this.mRS), X.getID(this.mRS), 0.0f, 0.0f, 0, incX, 0, 0, 0);
    }

    public void ZTPSV(int Uplo, int TransA, int Diag, Allocation Ap, Allocation X, int incX) {
        int N = validateTPMV(Element.F64_2(this.mRS), Uplo, TransA, Diag, Ap, X, incX);
        this.mRS.nScriptIntrinsicBLAS_Z(getID(this.mRS), 78, TransA, 0, 0, Uplo, Diag, 0, N, 0, 0.0d, 0.0d, Ap.getID(this.mRS), X.getID(this.mRS), 0.0d, 0.0d, 0, incX, 0, 0, 0);
    }

    static int validateSYMV(Element e, int Uplo, Allocation A, Allocation X, Allocation Y, int incX, int incY) {
        validateUplo(Uplo);
        int N = A.getType().getY();
        if (A.getType().getX() != N) {
            throw new RSRuntimeException("A must be a square matrix for SYMV");
        } else if (!A.getType().getElement().isCompatible(e) || !X.getType().getElement().isCompatible(e) || !Y.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        } else if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            throw new RSRuntimeException("BLAS vectors must have Y dimension of 0 or 1");
        } else if (incX <= 0 || incY <= 0) {
            throw new RSRuntimeException("Vector increments must be greater than 0");
        } else {
            if (X.getType().getX() == ((N - 1) * incX) + 1) {
                if (Y.getType().getX() == 1 + ((N - 1) * incY)) {
                    return N;
                }
                throw new RSRuntimeException("Incorrect vector dimensions for SYMV");
            }
            throw new RSRuntimeException("Incorrect vector dimensions for SYMV");
        }
    }

    static int validateSPMV(Element e, int Uplo, Allocation Ap, Allocation X, int incX, Allocation Y, int incY) {
        validateUplo(Uplo);
        if (!Ap.getType().getElement().isCompatible(e) || !X.getType().getElement().isCompatible(e) || !Y.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        } else if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            throw new RSRuntimeException("BLAS vectors must have Y dimension of 0 or 1");
        } else if (Ap.getType().getY() <= 1) {
            int N = (int) Math.sqrt(((double) Ap.getType().getX()) * 2.0d);
            if (Ap.getType().getX() != ((N + 1) * N) / 2) {
                throw new RSRuntimeException("Invalid dimension for Ap");
            } else if (incX <= 0 || incY <= 0) {
                throw new RSRuntimeException("Vector increments must be greater than 0");
            } else {
                if (X.getType().getX() == ((N - 1) * incX) + 1) {
                    if (Y.getType().getX() == 1 + ((N - 1) * incY)) {
                        return N;
                    }
                    throw new RSRuntimeException("Incorrect vector dimensions for SPMV");
                }
                throw new RSRuntimeException("Incorrect vector dimensions for SPMV");
            }
        } else {
            throw new RSRuntimeException("Ap must have a Y dimension of 0 or 1");
        }
    }

    static void validateGER(Element e, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        if (!A.getType().getElement().isCompatible(e) || !X.getType().getElement().isCompatible(e) || !Y.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        } else if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            throw new RSRuntimeException("BLAS vectors must have Y dimension of 0 or 1");
        } else {
            int M = A.getType().getY();
            int N = A.getType().getX();
            if (N < 1 || M < 1) {
                throw new RSRuntimeException("M and N must be 1 or greater for GER");
            } else if (incX <= 0 || incY <= 0) {
                throw new RSRuntimeException("Vector increments must be greater than 0");
            } else {
                if (X.getType().getX() == ((M - 1) * incX) + 1) {
                    if (Y.getType().getX() != 1 + ((N - 1) * incY)) {
                        throw new RSRuntimeException("Incorrect vector dimensions for GER");
                    }
                    return;
                }
                throw new RSRuntimeException("Incorrect vector dimensions for GER");
            }
        }
    }

    static int validateSYR(Element e, int Uplo, Allocation X, int incX, Allocation A) {
        validateUplo(Uplo);
        if (A.getType().getElement().isCompatible(e) && X.getType().getElement().isCompatible(e)) {
            int N = A.getType().getX();
            if (X.getType().getY() > 1) {
                throw new RSRuntimeException("BLAS vectors must have Y dimension of 0 or 1");
            } else if (N != A.getType().getY()) {
                throw new RSRuntimeException("A must be a symmetric matrix");
            } else if (incX > 0) {
                if (X.getType().getX() == 1 + ((N - 1) * incX)) {
                    return N;
                }
                throw new RSRuntimeException("Incorrect vector dimensions for SYR");
            } else {
                throw new RSRuntimeException("Vector increments must be greater than 0");
            }
        }
        throw new RSRuntimeException("Called BLAS with wrong Element type");
    }

    static int validateSPR(Element e, int Uplo, Allocation X, int incX, Allocation Ap) {
        validateUplo(Uplo);
        if (!Ap.getType().getElement().isCompatible(e) || !X.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        } else if (X.getType().getY() > 1) {
            throw new RSRuntimeException("BLAS vectors must have Y dimension of 0 or 1");
        } else if (Ap.getType().getY() <= 1) {
            int N = (int) Math.sqrt(((double) Ap.getType().getX()) * 2.0d);
            if (Ap.getType().getX() != ((N + 1) * N) / 2) {
                throw new RSRuntimeException("Invalid dimension for Ap");
            } else if (incX > 0) {
                if (X.getType().getX() == 1 + ((N - 1) * incX)) {
                    return N;
                }
                throw new RSRuntimeException("Incorrect vector dimensions for SPR");
            } else {
                throw new RSRuntimeException("Vector increments must be greater than 0");
            }
        } else {
            throw new RSRuntimeException("Ap must have a Y dimension of 0 or 1");
        }
    }

    static int validateSYR2(Element e, int Uplo, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        validateUplo(Uplo);
        if (!A.getType().getElement().isCompatible(e) || !X.getType().getElement().isCompatible(e) || !Y.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        } else if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            throw new RSRuntimeException("BLAS vectors must have Y dimension of 0 or 1");
        } else {
            int N = A.getType().getX();
            if (N != A.getType().getY()) {
                throw new RSRuntimeException("A must be a symmetric matrix");
            } else if (incX <= 0 || incY <= 0) {
                throw new RSRuntimeException("Vector increments must be greater than 0");
            } else {
                int expectedYDim = 1 + ((N - 1) * incY);
                if (X.getType().getX() == ((N - 1) * incX) + 1 && Y.getType().getX() == expectedYDim) {
                    return N;
                }
                throw new RSRuntimeException("Incorrect vector dimensions for SYR");
            }
        }
    }

    static int validateSPR2(Element e, int Uplo, Allocation X, int incX, Allocation Y, int incY, Allocation Ap) {
        validateUplo(Uplo);
        if (!Ap.getType().getElement().isCompatible(e) || !X.getType().getElement().isCompatible(e) || !Y.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        } else if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            throw new RSRuntimeException("BLAS vectors must have Y dimension of 0 or 1");
        } else if (Ap.getType().getY() <= 1) {
            int N = (int) Math.sqrt(((double) Ap.getType().getX()) * 2.0d);
            if (Ap.getType().getX() != ((N + 1) * N) / 2) {
                throw new RSRuntimeException("Invalid dimension for Ap");
            } else if (incX <= 0 || incY <= 0) {
                throw new RSRuntimeException("Vector increments must be greater than 0");
            } else {
                int expectedYDim = 1 + ((N - 1) * incY);
                if (X.getType().getX() == ((N - 1) * incX) + 1 && Y.getType().getX() == expectedYDim) {
                    return N;
                }
                throw new RSRuntimeException("Incorrect vector dimensions for SPR2");
            }
        } else {
            throw new RSRuntimeException("Ap must have a Y dimension of 0 or 1");
        }
    }

    public void SSYMV(int Uplo, float alpha, Allocation A, Allocation X, int incX, float beta, Allocation Y, int incY) {
        int N = validateSYMV(Element.F32(this.mRS), Uplo, A, X, Y, incX, incY);
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 79, 0, 0, 0, Uplo, 0, 0, N, 0, alpha, A.getID(this.mRS), X.getID(this.mRS), beta, Y.getID(this.mRS), incX, incY, 0, 0);
    }

    public void SSBMV(int Uplo, int K, float alpha, Allocation A, Allocation X, int incX, float beta, Allocation Y, int incY) {
        if (K >= 0) {
            int N = validateSYMV(Element.F32(this.mRS), Uplo, A, X, Y, incX, incY);
            this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 80, 0, 0, 0, Uplo, 0, 0, N, K, alpha, A.getID(this.mRS), X.getID(this.mRS), beta, Y.getID(this.mRS), incX, incY, 0, 0);
            return;
        }
        throw new RSRuntimeException("K must be greater than or equal to 0");
    }

    public void SSPMV(int Uplo, float alpha, Allocation Ap, Allocation X, int incX, float beta, Allocation Y, int incY) {
        int N = validateSPMV(Element.F32(this.mRS), Uplo, Ap, X, incX, Y, incY);
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 81, 0, 0, 0, Uplo, 0, 0, N, 0, alpha, Ap.getID(this.mRS), X.getID(this.mRS), beta, Y.getID(this.mRS), incX, incY, 0, 0);
    }

    public void SGER(float alpha, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        int M = A.getType().getY();
        int N = A.getType().getX();
        validateGER(Element.F32(this.mRS), X, incX, Y, incY, A);
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 82, 0, 0, 0, 0, 0, M, N, 0, alpha, X.getID(this.mRS), Y.getID(this.mRS), 0.0f, A.getID(this.mRS), incX, incY, 0, 0);
    }

    public void SSYR(int Uplo, float alpha, Allocation X, int incX, Allocation A) {
        Allocation allocation = X;
        Allocation allocation2 = A;
        int i = Uplo;
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 83, 0, 0, 0, i, 0, 0, validateSYR(Element.F32(this.mRS), i, allocation, incX, allocation2), 0, alpha, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0f, 0, incX, 0, 0, 0);
    }

    public void SSPR(int Uplo, float alpha, Allocation X, int incX, Allocation Ap) {
        Allocation allocation = X;
        Allocation allocation2 = Ap;
        int i = Uplo;
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 84, 0, 0, 0, i, 0, 0, validateSPR(Element.F32(this.mRS), i, allocation, incX, allocation2), 0, alpha, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0f, 0, incX, 0, 0, 0);
    }

    public void SSYR2(int Uplo, float alpha, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        int N = validateSYR2(Element.F32(this.mRS), Uplo, X, incX, Y, incY, A);
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 85, 0, 0, 0, Uplo, 0, 0, N, 0, alpha, X.getID(this.mRS), Y.getID(this.mRS), 0.0f, A.getID(this.mRS), incX, incY, 0, 0);
    }

    public void SSPR2(int Uplo, float alpha, Allocation X, int incX, Allocation Y, int incY, Allocation Ap) {
        int N = validateSPR2(Element.F32(this.mRS), Uplo, X, incX, Y, incY, Ap);
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 86, 0, 0, 0, Uplo, 0, 0, N, 0, alpha, X.getID(this.mRS), Y.getID(this.mRS), 0.0f, Ap.getID(this.mRS), incX, incY, 0, 0);
    }

    public void DSYMV(int Uplo, double alpha, Allocation A, Allocation X, int incX, double beta, Allocation Y, int incY) {
        int N = validateSYMV(Element.F64(this.mRS), Uplo, A, X, Y, incX, incY);
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 87, 0, 0, 0, Uplo, 0, 0, N, 0, alpha, A.getID(this.mRS), X.getID(this.mRS), beta, Y.getID(this.mRS), incX, incY, 0, 0);
    }

    public void DSBMV(int Uplo, int K, double alpha, Allocation A, Allocation X, int incX, double beta, Allocation Y, int incY) {
        if (K >= 0) {
            int N = validateSYMV(Element.F64(this.mRS), Uplo, A, X, Y, incX, incY);
            this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 88, 0, 0, 0, Uplo, 0, 0, N, K, alpha, A.getID(this.mRS), X.getID(this.mRS), beta, Y.getID(this.mRS), incX, incY, 0, 0);
            return;
        }
        throw new RSRuntimeException("K must be greater than or equal to 0");
    }

    public void DSPMV(int Uplo, double alpha, Allocation Ap, Allocation X, int incX, double beta, Allocation Y, int incY) {
        int N = validateSPMV(Element.F64(this.mRS), Uplo, Ap, X, incX, Y, incY);
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 89, 0, 0, 0, Uplo, 0, 0, N, 0, alpha, Ap.getID(this.mRS), X.getID(this.mRS), beta, Y.getID(this.mRS), incX, incY, 0, 0);
    }

    public void DGER(double alpha, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        int M = A.getType().getY();
        int N = A.getType().getX();
        validateGER(Element.F64(this.mRS), X, incX, Y, incY, A);
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 90, 0, 0, 0, 0, 0, M, N, 0, alpha, X.getID(this.mRS), Y.getID(this.mRS), 0.0d, A.getID(this.mRS), incX, incY, 0, 0);
    }

    public void DSYR(int Uplo, double alpha, Allocation X, int incX, Allocation A) {
        Allocation allocation = X;
        Allocation allocation2 = A;
        int i = Uplo;
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 91, 0, 0, 0, i, 0, 0, validateSYR(Element.F64(this.mRS), i, allocation, incX, allocation2), 0, alpha, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0d, 0, incX, 0, 0, 0);
    }

    public void DSPR(int Uplo, double alpha, Allocation X, int incX, Allocation Ap) {
        Allocation allocation = X;
        Allocation allocation2 = Ap;
        int i = Uplo;
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 92, 0, 0, 0, i, 0, 0, validateSPR(Element.F64(this.mRS), i, allocation, incX, allocation2), 0, alpha, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0d, 0, incX, 0, 0, 0);
    }

    public void DSYR2(int Uplo, double alpha, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        int N = validateSYR2(Element.F64(this.mRS), Uplo, X, incX, Y, incY, A);
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 93, 0, 0, 0, Uplo, 0, 0, N, 0, alpha, X.getID(this.mRS), Y.getID(this.mRS), 0.0d, A.getID(this.mRS), incX, incY, 0, 0);
    }

    public void DSPR2(int Uplo, double alpha, Allocation X, int incX, Allocation Y, int incY, Allocation Ap) {
        int N = validateSPR2(Element.F64(this.mRS), Uplo, X, incX, Y, incY, Ap);
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 94, 0, 0, 0, Uplo, 0, 0, N, 0, alpha, X.getID(this.mRS), Y.getID(this.mRS), 0.0d, Ap.getID(this.mRS), incX, incY, 0, 0);
    }

    static void validateGERU(Element e, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        if (!A.getType().getElement().isCompatible(e) || !X.getType().getElement().isCompatible(e) || !Y.getType().getElement().isCompatible(e)) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        } else if (X.getType().getY() > 1 || Y.getType().getY() > 1) {
            throw new RSRuntimeException("BLAS vectors must have Y dimension of 0 or 1");
        } else {
            int M = A.getType().getY();
            int N = A.getType().getX();
            if (incX <= 0 || incY <= 0) {
                throw new RSRuntimeException("Vector increments must be greater than 0");
            }
            if (X.getType().getX() == ((M - 1) * incX) + 1) {
                if (Y.getType().getX() != 1 + ((N - 1) * incY)) {
                    throw new RSRuntimeException("Incorrect vector dimensions for GERU");
                }
                return;
            }
            throw new RSRuntimeException("Incorrect vector dimensions for GERU");
        }
    }

    public void CHEMV(int Uplo, Float2 alpha, Allocation A, Allocation X, int incX, Float2 beta, Allocation Y, int incY) {
        Float2 float2 = alpha;
        Float2 float22 = beta;
        int N = validateSYR2(Element.F32_2(this.mRS), Uplo, X, incX, Y, incY, A);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        float f = float2.x;
        float f2 = float2.y;
        long id2 = A.getID(this.mRS);
        long id3 = X.getID(this.mRS);
        float f3 = float22.x;
        float f4 = float22.y;
        float f5 = f4;
        float f6 = f3;
        float f7 = f2;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 95, 0, 0, 0, Uplo, 0, 0, N, 0, f, f7, id2, id3, f6, f5, Y.getID(this.mRS), incX, incY, 0, 0);
    }

    public void CHBMV(int Uplo, int K, Float2 alpha, Allocation A, Allocation X, int incX, Float2 beta, Allocation Y, int incY) {
        Float2 float2 = alpha;
        Float2 float22 = beta;
        int N = validateSYR2(Element.F32_2(this.mRS), Uplo, X, incX, Y, incY, A);
        if (K >= 0) {
            RenderScript renderScript = this.mRS;
            long id = getID(this.mRS);
            float f = float2.x;
            float f2 = float2.y;
            long id2 = A.getID(this.mRS);
            long id3 = X.getID(this.mRS);
            float f3 = float22.x;
            float f4 = float22.y;
            float f5 = f2;
            int i = Uplo;
            float f6 = f3;
            float f7 = f4;
            float f8 = f;
            int i2 = N;
            int i3 = K;
            renderScript.nScriptIntrinsicBLAS_Complex(id, 96, 0, 0, 0, i, 0, 0, i2, i3, f8, f5, id2, id3, f6, f7, Y.getID(this.mRS), incX, incY, 0, 0);
            return;
        }
        throw new RSRuntimeException("K must be 0 or greater for HBMV");
    }

    public void CHPMV(int Uplo, Float2 alpha, Allocation Ap, Allocation X, int incX, Float2 beta, Allocation Y, int incY) {
        Float2 float2 = alpha;
        Float2 float22 = beta;
        int N = validateSPR2(Element.F32_2(this.mRS), Uplo, X, incX, Y, incY, Ap);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        float f = float2.x;
        float f2 = float2.y;
        long id2 = Ap.getID(this.mRS);
        long id3 = X.getID(this.mRS);
        float f3 = float22.x;
        float f4 = float22.y;
        float f5 = f4;
        float f6 = f3;
        float f7 = f2;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 97, 0, 0, 0, Uplo, 0, 0, N, 0, f, f7, id2, id3, f6, f5, Y.getID(this.mRS), incX, incY, 0, 0);
    }

    public void CGERU(Float2 alpha, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        Float2 float2 = alpha;
        validateGERU(Element.F32_2(this.mRS), X, incX, Y, incY, A);
        int M = A.getType().getY();
        int N = A.getType().getX();
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        float f = float2.x;
        float f2 = float2.y;
        float f3 = f2;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 98, 0, 0, 0, 0, 0, M, N, 0, f, f3, X.getID(this.mRS), Y.getID(this.mRS), 0.0f, 0.0f, A.getID(this.mRS), incX, incY, 0, 0);
    }

    public void CGERC(Float2 alpha, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        Float2 float2 = alpha;
        validateGERU(Element.F32_2(this.mRS), X, incX, Y, incY, A);
        int M = A.getType().getY();
        int N = A.getType().getX();
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        float f = float2.x;
        float f2 = float2.y;
        float f3 = f2;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 99, 0, 0, 0, 0, 0, M, N, 0, f, f3, X.getID(this.mRS), Y.getID(this.mRS), 0.0f, 0.0f, A.getID(this.mRS), incX, incY, 0, 0);
    }

    public void CHER(int Uplo, float alpha, Allocation X, int incX, Allocation A) {
        Allocation allocation = X;
        Allocation allocation2 = A;
        int i = Uplo;
        this.mRS.nScriptIntrinsicBLAS_Complex(getID(this.mRS), 100, 0, 0, 0, i, 0, 0, validateSYR(Element.F32_2(this.mRS), i, allocation, incX, allocation2), 0, alpha, 0.0f, allocation.getID(this.mRS), 0, 0.0f, 0.0f, allocation2.getID(this.mRS), incX, 0, 0, 0);
    }

    public void CHPR(int Uplo, float alpha, Allocation X, int incX, Allocation Ap) {
        Allocation allocation = X;
        Allocation allocation2 = Ap;
        int i = Uplo;
        this.mRS.nScriptIntrinsicBLAS_Complex(getID(this.mRS), 101, 0, 0, 0, i, 0, 0, validateSPR(Element.F32_2(this.mRS), i, allocation, incX, allocation2), 0, alpha, 0.0f, allocation.getID(this.mRS), 0, 0.0f, 0.0f, allocation2.getID(this.mRS), incX, 0, 0, 0);
    }

    public void CHER2(int Uplo, Float2 alpha, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        Float2 float2 = alpha;
        int N = validateSYR2(Element.F32_2(this.mRS), Uplo, X, incX, Y, incY, A);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        float f = float2.x;
        float f2 = float2.y;
        float f3 = f2;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 102, 0, 0, 0, Uplo, 0, 0, N, 0, f, f3, X.getID(this.mRS), Y.getID(this.mRS), 0.0f, 0.0f, A.getID(this.mRS), incX, incY, 0, 0);
    }

    public void CHPR2(int Uplo, Float2 alpha, Allocation X, int incX, Allocation Y, int incY, Allocation Ap) {
        Float2 float2 = alpha;
        int N = validateSPR2(Element.F32_2(this.mRS), Uplo, X, incX, Y, incY, Ap);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        float f = float2.x;
        float f2 = float2.y;
        float f3 = f2;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 103, 0, 0, 0, Uplo, 0, 0, N, 0, f, f3, X.getID(this.mRS), Y.getID(this.mRS), 0.0f, 0.0f, Ap.getID(this.mRS), incX, incY, 0, 0);
    }

    public void ZHEMV(int Uplo, Double2 alpha, Allocation A, Allocation X, int incX, Double2 beta, Allocation Y, int incY) {
        Double2 double2 = alpha;
        Double2 double22 = beta;
        int N = validateSYR2(Element.F64_2(this.mRS), Uplo, X, incX, Y, incY, A);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        double d = double2.x;
        double d2 = double2.y;
        long id2 = A.getID(this.mRS);
        long id3 = X.getID(this.mRS);
        double d3 = double22.x;
        double d4 = d;
        double d5 = double22.y;
        long id4 = Y.getID(this.mRS);
        double d6 = d2;
        int i = 0;
        int i2 = N;
        int i3 = 0;
        renderScript.nScriptIntrinsicBLAS_Z(id, 104, 0, 0, 0, Uplo, 0, i, i2, i3, d4, d6, id2, id3, d3, d5, id4, incX, incY, 0, 0);
    }

    public void ZHBMV(int Uplo, int K, Double2 alpha, Allocation A, Allocation X, int incX, Double2 beta, Allocation Y, int incY) {
        Double2 double2 = alpha;
        Double2 double22 = beta;
        int N = validateSYR2(Element.F64_2(this.mRS), Uplo, X, incX, Y, incY, A);
        if (K >= 0) {
            int i = N;
            int i2 = K;
            this.mRS.nScriptIntrinsicBLAS_Z(getID(this.mRS), 105, 0, 0, 0, Uplo, 0, 0, i, i2, double2.x, double2.y, A.getID(this.mRS), X.getID(this.mRS), double22.x, double22.y, Y.getID(this.mRS), incX, incY, 0, 0);
            return;
        }
        throw new RSRuntimeException("K must be 0 or greater for HBMV");
    }

    public void ZHPMV(int Uplo, Double2 alpha, Allocation Ap, Allocation X, int incX, Double2 beta, Allocation Y, int incY) {
        Double2 double2 = alpha;
        Double2 double22 = beta;
        int N = validateSPR2(Element.F64_2(this.mRS), Uplo, X, incX, Y, incY, Ap);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        double d = double2.x;
        double d2 = double2.y;
        long id2 = Ap.getID(this.mRS);
        long id3 = X.getID(this.mRS);
        double d3 = double22.x;
        double d4 = d;
        double d5 = double22.y;
        long id4 = Y.getID(this.mRS);
        double d6 = d2;
        int i = 0;
        int i2 = N;
        int i3 = 0;
        renderScript.nScriptIntrinsicBLAS_Z(id, 106, 0, 0, 0, Uplo, 0, i, i2, i3, d4, d6, id2, id3, d3, d5, id4, incX, incY, 0, 0);
    }

    public void ZGERU(Double2 alpha, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        Double2 double2 = alpha;
        validateGERU(Element.F64_2(this.mRS), X, incX, Y, incY, A);
        int M = A.getType().getY();
        int N = A.getType().getX();
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        double d = double2.x;
        double d2 = double2.y;
        double d3 = d2;
        double d4 = d;
        renderScript.nScriptIntrinsicBLAS_Z(id, 107, 0, 0, 0, 0, 0, M, N, 0, d4, d3, X.getID(this.mRS), Y.getID(this.mRS), 0.0d, 0.0d, A.getID(this.mRS), incX, incY, 0, 0);
    }

    public void ZGERC(Double2 alpha, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        Double2 double2 = alpha;
        validateGERU(Element.F64_2(this.mRS), X, incX, Y, incY, A);
        int M = A.getType().getY();
        int N = A.getType().getX();
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        double d = double2.x;
        double d2 = double2.y;
        double d3 = d2;
        double d4 = d;
        renderScript.nScriptIntrinsicBLAS_Z(id, 108, 0, 0, 0, 0, 0, M, N, 0, d4, d3, X.getID(this.mRS), Y.getID(this.mRS), 0.0d, 0.0d, A.getID(this.mRS), incX, incY, 0, 0);
    }

    public void ZHER(int Uplo, double alpha, Allocation X, int incX, Allocation A) {
        Allocation allocation = X;
        Allocation allocation2 = A;
        int i = Uplo;
        this.mRS.nScriptIntrinsicBLAS_Z(getID(this.mRS), 109, 0, 0, 0, i, 0, 0, validateSYR(Element.F64_2(this.mRS), i, allocation, incX, allocation2), 0, alpha, 0.0d, allocation.getID(this.mRS), 0, 0.0d, 0.0d, allocation2.getID(this.mRS), incX, 0, 0, 0);
    }

    public void ZHPR(int Uplo, double alpha, Allocation X, int incX, Allocation Ap) {
        Allocation allocation = X;
        Allocation allocation2 = Ap;
        int i = Uplo;
        this.mRS.nScriptIntrinsicBLAS_Z(getID(this.mRS), 110, 0, 0, 0, i, 0, 0, validateSPR(Element.F64_2(this.mRS), i, allocation, incX, allocation2), 0, alpha, 0.0d, allocation.getID(this.mRS), 0, 0.0d, 0.0d, allocation2.getID(this.mRS), incX, 0, 0, 0);
    }

    public void ZHER2(int Uplo, Double2 alpha, Allocation X, int incX, Allocation Y, int incY, Allocation A) {
        Double2 double2 = alpha;
        Allocation allocation = A;
        int N = validateSYR2(Element.F64_2(this.mRS), Uplo, X, incX, Y, incY, allocation);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        double d = double2.x;
        double d2 = double2.y;
        double d3 = d2;
        double d4 = d;
        renderScript.nScriptIntrinsicBLAS_Z(id, 111, 0, 0, 0, Uplo, 0, 0, N, 0, d4, d3, X.getID(this.mRS), Y.getID(this.mRS), 0.0d, 0.0d, allocation.getID(this.mRS), incX, incY, 0, 0);
    }

    public void ZHPR2(int Uplo, Double2 alpha, Allocation X, int incX, Allocation Y, int incY, Allocation Ap) {
        Double2 double2 = alpha;
        Allocation allocation = Ap;
        int N = validateSPR2(Element.F64_2(this.mRS), Uplo, X, incX, Y, incY, allocation);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        double d = double2.x;
        double d2 = double2.y;
        double d3 = d2;
        double d4 = d;
        renderScript.nScriptIntrinsicBLAS_Z(id, 112, 0, 0, 0, Uplo, 0, 0, N, 0, d4, d3, X.getID(this.mRS), Y.getID(this.mRS), 0.0d, 0.0d, allocation.getID(this.mRS), incX, incY, 0, 0);
    }

    static void validateL3(Element e, int TransA, int TransB, int Side, Allocation A, Allocation B, Allocation C) {
        int aM = -1;
        int aN = -1;
        int bM = -1;
        int bN = -1;
        if ((A != null && !A.getType().getElement().isCompatible(e)) || ((B != null && !B.getType().getElement().isCompatible(e)) || (C != null && !C.getType().getElement().isCompatible(e)))) {
            throw new RSRuntimeException("Called BLAS with wrong Element type");
        } else if (C != null) {
            int cM = C.getType().getY();
            int cN = C.getType().getX();
            if (Side != 142) {
                if (A != null) {
                    if (TransA == 112 || TransA == 113) {
                        aN = A.getType().getY();
                        aM = A.getType().getX();
                    } else {
                        aM = A.getType().getY();
                        aN = A.getType().getX();
                    }
                }
                if (B != null) {
                    if (TransB == 112 || TransB == 113) {
                        bN = B.getType().getY();
                        bM = B.getType().getX();
                    } else {
                        bM = B.getType().getY();
                        bN = B.getType().getX();
                    }
                }
            } else if ((A != null || B == null) && (A == null || B != null)) {
                if (B != null) {
                    bM = A.getType().getY();
                    bN = A.getType().getX();
                }
                if (A != null) {
                    aM = B.getType().getY();
                    aN = B.getType().getX();
                }
            } else {
                throw new RSRuntimeException("Provided Matrix A without Matrix B, or vice versa");
            }
            if (A == null || B == null || C == null) {
                if (A == null || C == null) {
                    if (A != null && B != null && aN != bM) {
                        throw new RSRuntimeException("Called BLAS with invalid dimensions");
                    }
                } else if (cM != cN) {
                    throw new RSRuntimeException("Matrix C is not symmetric");
                } else if (aM != cM) {
                    throw new RSRuntimeException("Called BLAS with invalid dimensions");
                }
            } else if (aN != bM || aM != cM || bN != cN) {
                throw new RSRuntimeException("Called BLAS with invalid dimensions");
            }
        } else {
            throw new RSRuntimeException("Allocation C cannot be null");
        }
    }

    public void SGEMM(int TransA, int TransB, float alpha, Allocation A, Allocation B, float beta, Allocation C) {
        int M;
        int K;
        int N;
        validateTranspose(TransA);
        validateTranspose(TransB);
        validateL3(Element.F32(this.mRS), TransA, TransB, 0, A, B, C);
        int i = TransA;
        if (i != 111) {
            M = A.getType().getX();
            K = A.getType().getY();
        } else {
            M = A.getType().getY();
            K = A.getType().getX();
        }
        int i2 = TransB;
        if (i2 != 111) {
            N = B.getType().getY();
        } else {
            N = B.getType().getX();
        }
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 113, i, i2, 0, 0, 0, M, N, K, alpha, A.getID(this.mRS), B.getID(this.mRS), beta, C.getID(this.mRS), 0, 0, 0, 0);
    }

    public void DGEMM(int TransA, int TransB, double alpha, Allocation A, Allocation B, double beta, Allocation C) {
        int M;
        int K;
        int N;
        validateTranspose(TransA);
        validateTranspose(TransB);
        validateL3(Element.F64(this.mRS), TransA, TransB, 0, A, B, C);
        int i = TransA;
        if (i != 111) {
            M = A.getType().getX();
            K = A.getType().getY();
        } else {
            M = A.getType().getY();
            K = A.getType().getX();
        }
        int i2 = TransB;
        if (i2 != 111) {
            N = B.getType().getY();
        } else {
            N = B.getType().getX();
        }
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 119, i, i2, 0, 0, 0, M, N, K, alpha, A.getID(this.mRS), B.getID(this.mRS), beta, C.getID(this.mRS), 0, 0, 0, 0);
    }

    public void CGEMM(int TransA, int TransB, Float2 alpha, Allocation A, Allocation B, Float2 beta, Allocation C) {
        int M;
        int K;
        int N;
        Float2 float2 = alpha;
        Float2 float22 = beta;
        validateTranspose(TransA);
        validateTranspose(TransB);
        validateL3(Element.F32_2(this.mRS), TransA, TransB, 0, A, B, C);
        int i = TransA;
        if (i != 111) {
            M = A.getType().getX();
            K = A.getType().getY();
        } else {
            M = A.getType().getY();
            K = A.getType().getX();
        }
        int i2 = TransB;
        if (i2 != 111) {
            N = B.getType().getY();
        } else {
            N = B.getType().getX();
        }
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        float f = float2.x;
        float f2 = float2.y;
        long id2 = A.getID(this.mRS);
        long id3 = B.getID(this.mRS);
        float f3 = float22.x;
        float f4 = float22.y;
        float f5 = f2;
        int i3 = i;
        float f6 = f;
        int i4 = i2;
        float f7 = f4;
        i = 0;
        int i5 = M;
        int i6 = N;
        int i7 = K;
        float f8 = f3;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 125, i3, i4, 0, 0, i, i5, i6, i7, f6, f5, id2, id3, f8, f7, C.getID(this.mRS), 0, 0, 0, 0);
    }

    public void ZGEMM(int TransA, int TransB, Double2 alpha, Allocation A, Allocation B, Double2 beta, Allocation C) {
        int M;
        int K;
        int N;
        Double2 double2 = alpha;
        Double2 double22 = beta;
        validateTranspose(TransA);
        validateTranspose(TransB);
        validateL3(Element.F64_2(this.mRS), TransA, TransB, 0, A, B, C);
        int i = TransA;
        if (i != 111) {
            M = A.getType().getX();
            K = A.getType().getY();
        } else {
            M = A.getType().getY();
            K = A.getType().getX();
        }
        if (TransB != 111) {
            N = B.getType().getY();
        } else {
            N = B.getType().getX();
        }
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        double d = double2.x;
        double d2 = double2.y;
        long id2 = A.getID(this.mRS);
        long id3 = B.getID(this.mRS);
        double d3 = double22.x;
        double d4 = double22.y;
        long id4 = C.getID(this.mRS);
        int i2 = TransB;
        int i3 = 0;
        int i4 = 0;
        i = 0;
        int i5 = M;
        int i6 = N;
        int i7 = K;
        renderScript.nScriptIntrinsicBLAS_Z(id, 131, i, i2, i3, i4, i, i5, i6, i7, d, d2, id2, id3, d3, d4, id4, 0, 0, 0, 0);
    }

    public void SSYMM(int Side, int Uplo, float alpha, Allocation A, Allocation B, float beta, Allocation C) {
        validateSide(Side);
        validateUplo(Uplo);
        if (A.getType().getX() == A.getType().getY()) {
            validateL3(Element.F32(this.mRS), 0, 0, Side, A, B, C);
            int i = Side;
            int i2 = Uplo;
            float f = alpha;
            float f2 = beta;
            this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 114, 0, 0, i, i2, 0, C.getType().getY(), C.getType().getX(), 0, f, A.getID(this.mRS), B.getID(this.mRS), f2, C.getID(this.mRS), 0, 0, 0, 0);
            return;
        }
        Allocation allocation = A;
        Allocation allocation2 = B;
        Allocation allocation3 = C;
        throw new RSRuntimeException("Matrix A is not symmetric");
    }

    public void DSYMM(int Side, int Uplo, double alpha, Allocation A, Allocation B, double beta, Allocation C) {
        validateSide(Side);
        validateUplo(Uplo);
        if (A.getType().getX() == A.getType().getY()) {
            validateL3(Element.F64(this.mRS), 0, 0, Side, A, B, C);
            int i = Side;
            int i2 = Uplo;
            double d = alpha;
            double d2 = beta;
            this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 120, 0, 0, i, i2, 0, C.getType().getY(), C.getType().getX(), 0, d, A.getID(this.mRS), B.getID(this.mRS), d2, C.getID(this.mRS), 0, 0, 0, 0);
            return;
        }
        Allocation allocation = A;
        Allocation allocation2 = B;
        Allocation allocation3 = C;
        throw new RSRuntimeException("Matrix A is not symmetric");
    }

    public void CSYMM(int Side, int Uplo, Float2 alpha, Allocation A, Allocation B, Float2 beta, Allocation C) {
        Float2 float2 = alpha;
        Float2 float22 = beta;
        validateSide(Side);
        validateUplo(Uplo);
        if (A.getType().getX() == A.getType().getY()) {
            validateL3(Element.F32_2(this.mRS), 0, 0, Side, A, B, C);
            RenderScript renderScript = this.mRS;
            long id = getID(this.mRS);
            int y = C.getType().getY();
            int x = C.getType().getX();
            float f = float2.x;
            float f2 = float2.y;
            long id2 = A.getID(this.mRS);
            long id3 = B.getID(this.mRS);
            float f3 = float22.x;
            float f4 = float22.y;
            int i = Side;
            int i2 = Uplo;
            float f5 = f;
            float f6 = f2;
            float f7 = f3;
            renderScript.nScriptIntrinsicBLAS_Complex(id, 126, 0, 0, i, i2, 0, y, x, 0, f5, f6, id2, id3, f7, f4, C.getID(this.mRS), 0, 0, 0, 0);
            return;
        }
        Allocation allocation = A;
        throw new RSRuntimeException("Matrix A is not symmetric");
    }

    public void ZSYMM(int Side, int Uplo, Double2 alpha, Allocation A, Allocation B, Double2 beta, Allocation C) {
        Double2 double2 = alpha;
        Double2 double22 = beta;
        validateSide(Side);
        validateUplo(Uplo);
        if (A.getType().getX() == A.getType().getY()) {
            validateL3(Element.F64_2(this.mRS), 0, 0, Side, A, B, C);
            RenderScript renderScript = this.mRS;
            long id = getID(this.mRS);
            int y = C.getType().getY();
            int x = C.getType().getX();
            double d = double2.x;
            double d2 = double2.y;
            long id2 = A.getID(this.mRS);
            long id3 = B.getID(this.mRS);
            double d3 = double22.x;
            double d4 = double22.y;
            double d5 = d3;
            int i = Side;
            int i2 = Uplo;
            double d6 = d;
            double d7 = d2;
            renderScript.nScriptIntrinsicBLAS_Z(id, 132, 0, 0, i, i2, 0, y, x, 0, d6, d7, id2, id3, d5, d4, C.getID(this.mRS), 0, 0, 0, 0);
            return;
        }
        throw new RSRuntimeException("Matrix A is not symmetric");
    }

    public void SSYRK(int Uplo, int Trans, float alpha, Allocation A, float beta, Allocation C) {
        int K;
        validateTranspose(Trans);
        validateUplo(Uplo);
        validateL3(Element.F32(this.mRS), Trans, 0, 0, A, null, C);
        int i = Trans;
        if (i != 111) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 115, i, 0, 0, Uplo, 0, 0, C.getType().getX(), K, alpha, A.getID(this.mRS), 0, beta, C.getID(this.mRS), 0, 0, 0, 0);
    }

    public void DSYRK(int Uplo, int Trans, double alpha, Allocation A, double beta, Allocation C) {
        int K;
        validateTranspose(Trans);
        validateUplo(Uplo);
        validateL3(Element.F64(this.mRS), Trans, 0, 0, A, null, C);
        int i = Trans;
        if (i != 111) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 121, i, 0, 0, Uplo, 0, 0, C.getType().getX(), K, alpha, A.getID(this.mRS), 0, beta, C.getID(this.mRS), 0, 0, 0, 0);
    }

    public void CSYRK(int Uplo, int Trans, Float2 alpha, Allocation A, Float2 beta, Allocation C) {
        int K;
        Float2 float2 = alpha;
        Float2 float22 = beta;
        validateTranspose(Trans);
        validateUplo(Uplo);
        validateL3(Element.F32_2(this.mRS), Trans, 0, 0, A, null, C);
        int i = Trans;
        if (i != 111) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int x = C.getType().getX();
        float f = float2.x;
        float f2 = float2.y;
        long id2 = A.getID(this.mRS);
        float f3 = float22.x;
        float f4 = float22.y;
        float f5 = f2;
        int i2 = i;
        float f6 = f;
        int i3 = Uplo;
        float f7 = f3;
        float f8 = f4;
        i = x;
        int i4 = K;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 127, i2, 0, 0, i3, 0, 0, i, i4, f6, f5, id2, 0, f7, f8, C.getID(this.mRS), 0, 0, 0, 0);
    }

    public void ZSYRK(int Uplo, int Trans, Double2 alpha, Allocation A, Double2 beta, Allocation C) {
        int K;
        Double2 double2 = alpha;
        Double2 double22 = beta;
        validateTranspose(Trans);
        validateUplo(Uplo);
        validateL3(Element.F64_2(this.mRS), Trans, 0, 0, A, null, C);
        int i = Trans;
        if (i != 111) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int x = C.getType().getX();
        double d = double2.x;
        double d2 = double2.y;
        double d3 = d2;
        i = x;
        int i2 = K;
        renderScript.nScriptIntrinsicBLAS_Z(id, 133, i, 0, 0, Uplo, 0, 0, i, i2, d, d3, A.getID(this.mRS), 0, double22.x, double22.y, C.getID(this.mRS), 0, 0, 0, 0);
    }

    static void validateSYR2K(Element e, int Trans, Allocation A, Allocation B, Allocation C) {
        validateTranspose(Trans);
        if (A.getType().getElement().isCompatible(e) && B.getType().getElement().isCompatible(e) && C.getType().getElement().isCompatible(e)) {
            int Cdim;
            if (Trans == 112) {
                Cdim = A.getType().getX();
            } else {
                Cdim = A.getType().getY();
            }
            if (C.getType().getX() != Cdim || C.getType().getY() != Cdim) {
                throw new RSRuntimeException("Invalid symmetric matrix in SYR2K");
            } else if (A.getType().getX() != B.getType().getX() || A.getType().getY() != B.getType().getY()) {
                throw new RSRuntimeException("Invalid A and B in SYR2K");
            } else {
                return;
            }
        }
        throw new RSRuntimeException("Called BLAS with wrong Element type");
    }

    public void SSYR2K(int Uplo, int Trans, float alpha, Allocation A, Allocation B, float beta, Allocation C) {
        int K;
        int i = Trans;
        Allocation allocation = A;
        Allocation allocation2 = B;
        Allocation allocation3 = C;
        validateUplo(Uplo);
        validateSYR2K(Element.F32(this.mRS), i, allocation, allocation2, allocation3);
        if (i != 111) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        int K2 = K;
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 116, i, 0, 0, Uplo, 0, 0, C.getType().getX(), K2, alpha, allocation.getID(this.mRS), allocation2.getID(this.mRS), beta, allocation3.getID(this.mRS), 0, 0, 0, 0);
    }

    public void DSYR2K(int Uplo, int Trans, double alpha, Allocation A, Allocation B, double beta, Allocation C) {
        int K;
        int i = Trans;
        Allocation allocation = A;
        Allocation allocation2 = B;
        Allocation allocation3 = C;
        validateUplo(Uplo);
        validateSYR2K(Element.F64(this.mRS), i, allocation, allocation2, allocation3);
        if (i != 111) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        int K2 = K;
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 122, i, 0, 0, Uplo, 0, 0, C.getType().getX(), K2, alpha, allocation.getID(this.mRS), allocation2.getID(this.mRS), beta, allocation3.getID(this.mRS), 0, 0, 0, 0);
    }

    public void CSYR2K(int Uplo, int Trans, Float2 alpha, Allocation A, Allocation B, Float2 beta, Allocation C) {
        int K;
        int i = Trans;
        Float2 float2 = alpha;
        Allocation allocation = A;
        Allocation allocation2 = B;
        Float2 float22 = beta;
        Allocation allocation3 = C;
        validateUplo(Uplo);
        validateSYR2K(Element.F32_2(this.mRS), i, allocation, allocation2, allocation3);
        if (i != 111) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        int K2 = K;
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int x = C.getType().getX();
        float f = float2.x;
        float f2 = float2.y;
        long id2 = allocation.getID(this.mRS);
        long id3 = allocation2.getID(this.mRS);
        float f3 = float22.x;
        float f4 = f2;
        float f5 = f;
        float f6 = f3;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 128, i, 0, 0, Uplo, 0, 0, x, K2, f5, f4, id2, id3, f6, float22.y, allocation3.getID(this.mRS), 0, 0, 0, 0);
    }

    public void ZSYR2K(int Uplo, int Trans, Double2 alpha, Allocation A, Allocation B, Double2 beta, Allocation C) {
        int K;
        int i = Trans;
        Double2 double2 = alpha;
        Allocation allocation = A;
        Allocation allocation2 = B;
        Double2 double22 = beta;
        Allocation allocation3 = C;
        validateUplo(Uplo);
        validateSYR2K(Element.F64_2(this.mRS), i, allocation, allocation2, allocation3);
        if (i != 111) {
            K = A.getType().getY();
        } else {
            K = A.getType().getX();
        }
        int K2 = K;
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int x = C.getType().getX();
        double d = double2.x;
        double d2 = double2.y;
        double d3 = d2;
        renderScript.nScriptIntrinsicBLAS_Z(id, 134, i, 0, 0, Uplo, 0, 0, x, K2, d, d3, allocation.getID(this.mRS), allocation2.getID(this.mRS), double22.x, double22.y, allocation3.getID(this.mRS), 0, 0, 0, 0);
    }

    static void validateTRMM(Element e, int Side, int TransA, Allocation A, Allocation B) {
        validateSide(Side);
        validateTranspose(TransA);
        if (A.getType().getElement().isCompatible(e) && B.getType().getElement().isCompatible(e)) {
            int aM = A.getType().getY();
            int aN = A.getType().getX();
            if (aM == aN) {
                int bM = B.getType().getY();
                int bN = B.getType().getX();
                if (Side == 141) {
                    if (aN != bM) {
                        throw new RSRuntimeException("Called TRMM with invalid matrices");
                    }
                    return;
                } else if (bN != aM) {
                    throw new RSRuntimeException("Called TRMM with invalid matrices");
                } else {
                    return;
                }
            }
            throw new RSRuntimeException("Called TRMM with a non-symmetric matrix A");
        }
        throw new RSRuntimeException("Called BLAS with wrong Element type");
    }

    public void STRMM(int Side, int Uplo, int TransA, int Diag, float alpha, Allocation A, Allocation B) {
        Allocation allocation = A;
        Allocation allocation2 = B;
        validateUplo(Uplo);
        validateDiag(Diag);
        int i = Side;
        int i2 = TransA;
        validateTRMM(Element.F32(this.mRS), i, i2, allocation, allocation2);
        int i3 = i2;
        int i4 = i;
        int i5 = Uplo;
        int i6 = Diag;
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 117, i3, 0, i4, i5, i6, B.getType().getY(), B.getType().getX(), 0, alpha, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0f, 0, 0, 0, 0, 0);
    }

    public void DTRMM(int Side, int Uplo, int TransA, int Diag, double alpha, Allocation A, Allocation B) {
        Allocation allocation = A;
        Allocation allocation2 = B;
        validateUplo(Uplo);
        validateDiag(Diag);
        int i = Side;
        int i2 = TransA;
        validateTRMM(Element.F64(this.mRS), i, i2, allocation, allocation2);
        int i3 = i2;
        int i4 = i;
        int i5 = Uplo;
        int i6 = Diag;
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 123, i3, 0, i4, i5, i6, B.getType().getY(), B.getType().getX(), 0, alpha, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0d, 0, 0, 0, 0, 0);
    }

    public void CTRMM(int Side, int Uplo, int TransA, int Diag, Float2 alpha, Allocation A, Allocation B) {
        Float2 float2 = alpha;
        Allocation allocation = A;
        Allocation allocation2 = B;
        validateUplo(Uplo);
        validateDiag(Diag);
        int i = Side;
        int i2 = TransA;
        validateTRMM(Element.F32_2(this.mRS), i, i2, allocation, allocation2);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int y = B.getType().getY();
        int x = B.getType().getX();
        float f = float2.x;
        float f2 = float2.y;
        float f3 = f;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 129, i2, 0, i, Uplo, Diag, y, x, 0, f3, f2, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0f, 0.0f, 0, 0, 0, 0, 0);
    }

    public void ZTRMM(int Side, int Uplo, int TransA, int Diag, Double2 alpha, Allocation A, Allocation B) {
        Double2 double2 = alpha;
        Allocation allocation = A;
        Allocation allocation2 = B;
        validateUplo(Uplo);
        validateDiag(Diag);
        int i = Side;
        int i2 = TransA;
        validateTRMM(Element.F64_2(this.mRS), i, i2, allocation, allocation2);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int y = B.getType().getY();
        int x = B.getType().getX();
        double d = double2.x;
        double d2 = double2.y;
        double d3 = d;
        renderScript.nScriptIntrinsicBLAS_Z(id, 135, i2, 0, i, Uplo, Diag, y, x, 0, d3, d2, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0d, 0.0d, 0, 0, 0, 0, 0);
    }

    static void validateTRSM(Element e, int Side, int TransA, Allocation A, Allocation B) {
        validateSide(Side);
        validateTranspose(TransA);
        if (A.getType().getElement().isCompatible(e) && B.getType().getElement().isCompatible(e)) {
            int adim = A.getType().getX();
            if (adim == A.getType().getY()) {
                int bM = B.getType().getY();
                int bN = B.getType().getX();
                if (Side == 141) {
                    if (adim != bM) {
                        throw new RSRuntimeException("Called TRSM with invalid matrix dimensions");
                    }
                    return;
                } else if (adim != bN) {
                    throw new RSRuntimeException("Called TRSM with invalid matrix dimensions");
                } else {
                    return;
                }
            }
            throw new RSRuntimeException("Called TRSM with a non-symmetric matrix A");
        }
        throw new RSRuntimeException("Called BLAS with wrong Element type");
    }

    public void STRSM(int Side, int Uplo, int TransA, int Diag, float alpha, Allocation A, Allocation B) {
        Allocation allocation = A;
        Allocation allocation2 = B;
        validateUplo(Uplo);
        validateDiag(Diag);
        int i = Side;
        int i2 = TransA;
        validateTRSM(Element.F32(this.mRS), i, i2, allocation, allocation2);
        int i3 = i2;
        int i4 = i;
        int i5 = Uplo;
        int i6 = Diag;
        this.mRS.nScriptIntrinsicBLAS_Single(getID(this.mRS), 118, i3, 0, i4, i5, i6, B.getType().getY(), B.getType().getX(), 0, alpha, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0f, 0, 0, 0, 0, 0);
    }

    public void DTRSM(int Side, int Uplo, int TransA, int Diag, double alpha, Allocation A, Allocation B) {
        Allocation allocation = A;
        Allocation allocation2 = B;
        validateUplo(Uplo);
        validateDiag(Diag);
        int i = Side;
        int i2 = TransA;
        validateTRSM(Element.F64(this.mRS), i, i2, allocation, allocation2);
        int i3 = i2;
        int i4 = i;
        int i5 = Uplo;
        int i6 = Diag;
        this.mRS.nScriptIntrinsicBLAS_Double(getID(this.mRS), 124, i3, 0, i4, i5, i6, B.getType().getY(), B.getType().getX(), 0, alpha, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0d, 0, 0, 0, 0, 0);
    }

    public void CTRSM(int Side, int Uplo, int TransA, int Diag, Float2 alpha, Allocation A, Allocation B) {
        Float2 float2 = alpha;
        Allocation allocation = A;
        Allocation allocation2 = B;
        validateUplo(Uplo);
        validateDiag(Diag);
        int i = Side;
        int i2 = TransA;
        validateTRSM(Element.F32_2(this.mRS), i, i2, allocation, allocation2);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int y = B.getType().getY();
        int x = B.getType().getX();
        float f = float2.x;
        float f2 = float2.y;
        float f3 = f;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 130, i2, 0, i, Uplo, Diag, y, x, 0, f3, f2, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0f, 0.0f, 0, 0, 0, 0, 0);
    }

    public void ZTRSM(int Side, int Uplo, int TransA, int Diag, Double2 alpha, Allocation A, Allocation B) {
        Double2 double2 = alpha;
        Allocation allocation = A;
        Allocation allocation2 = B;
        validateUplo(Uplo);
        validateDiag(Diag);
        int i = Side;
        int i2 = TransA;
        validateTRSM(Element.F64_2(this.mRS), i, i2, allocation, allocation2);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int y = B.getType().getY();
        int x = B.getType().getX();
        double d = double2.x;
        double d2 = double2.y;
        double d3 = d;
        renderScript.nScriptIntrinsicBLAS_Z(id, 136, i2, 0, i, Uplo, Diag, y, x, 0, d3, d2, allocation.getID(this.mRS), allocation2.getID(this.mRS), 0.0d, 0.0d, 0, 0, 0, 0, 0);
    }

    static void validateHEMM(Element e, int Side, Allocation A, Allocation B, Allocation C) {
        validateSide(Side);
        if (A.getType().getElement().isCompatible(e) && B.getType().getElement().isCompatible(e) && C.getType().getElement().isCompatible(e)) {
            int adim = A.getType().getX();
            if (adim != A.getType().getY()) {
                throw new RSRuntimeException("Called HEMM with non-square A");
            } else if ((Side == 141 && adim != B.getType().getY()) || (Side == 142 && adim != B.getType().getX())) {
                throw new RSRuntimeException("Called HEMM with invalid B");
            } else if (B.getType().getX() != C.getType().getX() || B.getType().getY() != C.getType().getY()) {
                throw new RSRuntimeException("Called HEMM with mismatched B and C");
            } else {
                return;
            }
        }
        throw new RSRuntimeException("Called BLAS with wrong Element type");
    }

    public void CHEMM(int Side, int Uplo, Float2 alpha, Allocation A, Allocation B, Float2 beta, Allocation C) {
        Float2 float2 = alpha;
        Allocation allocation = A;
        Allocation allocation2 = B;
        Float2 float22 = beta;
        Allocation allocation3 = C;
        validateUplo(Uplo);
        int i = Side;
        validateHEMM(Element.F32_2(this.mRS), i, allocation, allocation2, allocation3);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int y = C.getType().getY();
        int x = C.getType().getX();
        float f = float2.x;
        float f2 = float2.y;
        long id2 = allocation.getID(this.mRS);
        long id3 = allocation2.getID(this.mRS);
        float f3 = float22.x;
        float f4 = float22.y;
        float f5 = f3;
        float f6 = f2;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 137, 0, 0, i, Uplo, 0, y, x, 0, f, f6, id2, id3, f5, f4, allocation3.getID(this.mRS), 0, 0, 0, 0);
    }

    public void ZHEMM(int Side, int Uplo, Double2 alpha, Allocation A, Allocation B, Double2 beta, Allocation C) {
        Double2 double2 = alpha;
        Allocation allocation = A;
        Allocation allocation2 = B;
        Double2 double22 = beta;
        Allocation allocation3 = C;
        validateUplo(Uplo);
        int i = Side;
        validateHEMM(Element.F64_2(this.mRS), i, allocation, allocation2, allocation3);
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int y = C.getType().getY();
        int x = C.getType().getX();
        double d = double2.x;
        double d2 = double2.y;
        long id2 = allocation.getID(this.mRS);
        long id3 = allocation2.getID(this.mRS);
        double d3 = d2;
        double d4 = d;
        double d5 = double22.x;
        double d6 = double22.y;
        int i2 = 0;
        double d7 = d5;
        int i3 = 0;
        int i4 = i;
        int i5 = Uplo;
        i = 0;
        int i6 = 0;
        double d8 = d4;
        renderScript.nScriptIntrinsicBLAS_Z(id, 140, i2, i3, i4, i5, i, y, x, i6, d8, d3, id2, id3, d7, d6, allocation3.getID(this.mRS), 0, 0, 0, 0);
    }

    static void validateHERK(Element e, int Trans, Allocation A, Allocation C) {
        if (A.getType().getElement().isCompatible(e) && C.getType().getElement().isCompatible(e)) {
            validateConjTranspose(Trans);
            int cdim = C.getType().getX();
            if (cdim != C.getType().getY()) {
                throw new RSRuntimeException("Called HERK with non-square C");
            } else if (Trans == 111) {
                if (cdim != A.getType().getY()) {
                    throw new RSRuntimeException("Called HERK with invalid A");
                }
                return;
            } else if (cdim != A.getType().getX()) {
                throw new RSRuntimeException("Called HERK with invalid A");
            } else {
                return;
            }
        }
        throw new RSRuntimeException("Called BLAS with wrong Element type");
    }

    public void CHERK(int Uplo, int Trans, float alpha, Allocation A, float beta, Allocation C) {
        int k;
        int i = Trans;
        Allocation allocation = A;
        Allocation allocation2 = C;
        validateUplo(Uplo);
        validateHERK(Element.F32_2(this.mRS), i, allocation, allocation2);
        if (i == 113) {
            k = A.getType().getY();
        } else {
            k = A.getType().getX();
        }
        int k2 = k;
        this.mRS.nScriptIntrinsicBLAS_Complex(getID(this.mRS), 138, i, 0, 0, Uplo, 0, 0, C.getType().getX(), k2, alpha, 0.0f, allocation.getID(this.mRS), 0, beta, 0.0f, allocation2.getID(this.mRS), 0, 0, 0, 0);
    }

    public void ZHERK(int Uplo, int Trans, double alpha, Allocation A, double beta, Allocation C) {
        int k;
        int i = Trans;
        Allocation allocation = A;
        Allocation allocation2 = C;
        validateUplo(Uplo);
        validateHERK(Element.F64_2(this.mRS), i, allocation, allocation2);
        if (i == 113) {
            k = A.getType().getY();
        } else {
            k = A.getType().getX();
        }
        int k2 = k;
        this.mRS.nScriptIntrinsicBLAS_Z(getID(this.mRS), 141, i, 0, 0, Uplo, 0, 0, C.getType().getX(), k2, alpha, 0.0d, allocation.getID(this.mRS), 0, beta, 0.0d, allocation2.getID(this.mRS), 0, 0, 0, 0);
    }

    static void validateHER2K(Element e, int Trans, Allocation A, Allocation B, Allocation C) {
        if (A.getType().getElement().isCompatible(e) && B.getType().getElement().isCompatible(e) && C.getType().getElement().isCompatible(e)) {
            validateConjTranspose(Trans);
            int cdim = C.getType().getX();
            if (cdim == C.getType().getY()) {
                if (Trans == 111) {
                    if (A.getType().getY() != cdim) {
                        throw new RSRuntimeException("Called HER2K with invalid matrices");
                    }
                } else if (A.getType().getX() != cdim) {
                    throw new RSRuntimeException("Called HER2K with invalid matrices");
                }
                if (A.getType().getX() != B.getType().getX() || A.getType().getY() != B.getType().getY()) {
                    throw new RSRuntimeException("Called HER2K with invalid A and B matrices");
                }
                return;
            }
            throw new RSRuntimeException("Called HER2K with non-square C");
        }
        throw new RSRuntimeException("Called BLAS with wrong Element type");
    }

    public void CHER2K(int Uplo, int Trans, Float2 alpha, Allocation A, Allocation B, float beta, Allocation C) {
        int k;
        int i = Trans;
        Float2 float2 = alpha;
        Allocation allocation = A;
        Allocation allocation2 = B;
        Allocation allocation3 = C;
        validateUplo(Uplo);
        validateHER2K(Element.F32_2(this.mRS), i, allocation, allocation2, allocation3);
        if (i == 111) {
            k = A.getType().getX();
        } else {
            k = A.getType().getY();
        }
        int k2 = k;
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int x = C.getType().getX();
        float f = float2.x;
        float f2 = float2.y;
        float f3 = f2;
        float f4 = f;
        renderScript.nScriptIntrinsicBLAS_Complex(id, 139, i, 0, 0, Uplo, 0, 0, x, k2, f4, f3, allocation.getID(this.mRS), allocation2.getID(this.mRS), beta, 0.0f, allocation3.getID(this.mRS), 0, 0, 0, 0);
    }

    public void ZHER2K(int Uplo, int Trans, Double2 alpha, Allocation A, Allocation B, double beta, Allocation C) {
        int k;
        int i = Trans;
        Double2 double2 = alpha;
        Allocation allocation = A;
        Allocation allocation2 = B;
        Allocation allocation3 = C;
        validateUplo(Uplo);
        validateHER2K(Element.F64_2(this.mRS), i, allocation, allocation2, allocation3);
        if (i == 111) {
            k = A.getType().getX();
        } else {
            k = A.getType().getY();
        }
        int k2 = k;
        RenderScript renderScript = this.mRS;
        long id = getID(this.mRS);
        int x = C.getType().getX();
        double d = double2.x;
        double d2 = d;
        renderScript.nScriptIntrinsicBLAS_Z(id, 142, i, 0, 0, Uplo, 0, 0, x, k2, d2, double2.y, allocation.getID(this.mRS), allocation2.getID(this.mRS), beta, 0.0d, allocation3.getID(this.mRS), 0, 0, 0, 0);
    }

    public void BNNM(Allocation A, int a_offset, Allocation B, int b_offset, Allocation C, int c_offset, int c_mult) {
        int i = a_offset;
        int i2 = b_offset;
        validateL3(Element.U8(this.mRS), 111, 112, 0, A, B, C);
        if (i < 0 || i > 255) {
            throw new RSRuntimeException("Invalid a_offset passed to BNNM");
        } else if (i2 < 0 || i2 > 255) {
            throw new RSRuntimeException("Invalid b_offset passed to BNNM");
        } else {
            int M = A.getType().getY();
            int N = B.getType().getY();
            int K = A.getType().getX();
            this.mRS.nScriptIntrinsicBLAS_BNNM(getID(this.mRS), M, N, K, A.getID(this.mRS), i, B.getID(this.mRS), i2, C.getID(this.mRS), c_offset, c_mult);
        }
    }
}
