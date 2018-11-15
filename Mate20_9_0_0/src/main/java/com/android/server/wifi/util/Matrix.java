package com.android.server.wifi.util;

public class Matrix {
    public final int m;
    public final double[] mem;
    public final int n;

    public Matrix(int rows, int cols) {
        this.n = rows;
        this.m = cols;
        this.mem = new double[(rows * cols)];
    }

    public Matrix(int stride, double[] values) {
        this.n = ((values.length + stride) - 1) / stride;
        this.m = stride;
        this.mem = values;
        if (this.mem.length != this.n * this.m) {
            throw new IllegalArgumentException();
        }
    }

    public Matrix(Matrix that) {
        this.n = that.n;
        this.m = that.m;
        this.mem = new double[that.mem.length];
        for (int i = 0; i < this.mem.length; i++) {
            this.mem[i] = that.mem[i];
        }
    }

    public double get(int i, int j) {
        if (i >= 0 && i < this.n && j >= 0 && j < this.m) {
            return this.mem[(this.m * i) + j];
        }
        throw new IndexOutOfBoundsException();
    }

    public void put(int i, int j, double v) {
        if (i < 0 || i >= this.n || j < 0 || j >= this.m) {
            throw new IndexOutOfBoundsException();
        }
        this.mem[(this.m * i) + j] = v;
    }

    public Matrix plus(Matrix that) {
        return plus(that, new Matrix(this.n, this.m));
    }

    public Matrix plus(Matrix that, Matrix result) {
        if (this.n == that.n && this.m == that.m && this.n == result.n && this.m == result.m) {
            for (int i = 0; i < this.mem.length; i++) {
                result.mem[i] = this.mem[i] + that.mem[i];
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    public Matrix minus(Matrix that) {
        return minus(that, new Matrix(this.n, this.m));
    }

    public Matrix minus(Matrix that, Matrix result) {
        if (this.n == that.n && this.m == that.m && this.n == result.n && this.m == result.m) {
            for (int i = 0; i < this.mem.length; i++) {
                result.mem[i] = this.mem[i] - that.mem[i];
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    public Matrix times(double scalar) {
        return times(scalar, new Matrix(this.n, this.m));
    }

    public Matrix times(double scalar, Matrix result) {
        if (this.n == result.n && this.m == result.m) {
            for (int i = 0; i < this.mem.length; i++) {
                result.mem[i] = this.mem[i] * scalar;
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    public Matrix dot(Matrix that) {
        return dot(that, new Matrix(this.n, that.m));
    }

    public Matrix dot(Matrix that, Matrix result) {
        if (this.n == result.n && this.m == that.n && that.m == result.m) {
            for (int i = 0; i < this.n; i++) {
                for (int j = 0; j < that.m; j++) {
                    double s = 0.0d;
                    for (int k = 0; k < this.m; k++) {
                        s += get(i, k) * that.get(k, j);
                    }
                    result.put(i, j, s);
                }
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    public Matrix transpose() {
        return transpose(new Matrix(this.m, this.n));
    }

    public Matrix transpose(Matrix result) {
        if (this.n == result.m && this.m == result.n) {
            for (int i = 0; i < this.n; i++) {
                for (int j = 0; j < this.m; j++) {
                    result.put(j, i, get(i, j));
                }
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    public Matrix inverse() {
        return inverse(new Matrix(this.n, this.m), new Matrix(this.n, 2 * this.m));
    }

    public Matrix inverse(Matrix result, Matrix scratch) {
        Matrix matrix = result;
        Matrix matrix2 = scratch;
        if (this.n == this.m && this.n == matrix.n && this.m == matrix.m && this.n == matrix2.n && 2 * this.m == matrix2.m) {
            int j;
            int i = 0;
            while (i < this.n) {
                j = 0;
                while (j < this.m) {
                    matrix2.put(i, j, get(i, j));
                    matrix2.put(i, this.m + j, i == j ? 1.0d : 0.0d);
                    j++;
                }
                i++;
            }
            i = 0;
            while (i < this.n) {
                int ii;
                double v;
                j = i;
                double vbest = Math.abs(matrix2.get(j, j));
                for (ii = i + 1; ii < this.n; ii++) {
                    v = Math.abs(matrix2.get(ii, i));
                    if (v > vbest) {
                        j = ii;
                        vbest = v;
                    }
                }
                if (j != i) {
                    for (ii = 0; ii < matrix2.m; ii++) {
                        v = matrix2.get(i, ii);
                        matrix2.put(i, ii, matrix2.get(j, ii));
                        matrix2.put(j, ii, v);
                    }
                }
                double d = matrix2.get(i, i);
                if (d != 0.0d) {
                    int j2;
                    for (j2 = 0; j2 < matrix2.m; j2++) {
                        matrix2.put(i, j2, matrix2.get(i, j2) / d);
                    }
                    for (j2 = i + 1; j2 < this.n; j2++) {
                        d = matrix2.get(j2, i);
                        for (int j3 = 0; j3 < matrix2.m; j3++) {
                            matrix2.put(j2, j3, matrix2.get(j2, j3) - (matrix2.get(i, j3) * d));
                        }
                    }
                    i++;
                } else {
                    throw new ArithmeticException("Singular matrix");
                }
            }
            for (i = this.n - 1; i >= 0; i--) {
                for (j = 0; j < i; j++) {
                    double d2 = matrix2.get(j, i);
                    for (int j4 = 0; j4 < matrix2.m; j4++) {
                        matrix2.put(j, j4, matrix2.get(j, j4) - (matrix2.get(i, j4) * d2));
                    }
                }
            }
            for (i = 0; i < matrix.n; i++) {
                for (j = 0; j < matrix.m; j++) {
                    matrix.put(i, j, matrix2.get(i, this.m + j));
                }
            }
            return matrix;
        }
        throw new IllegalArgumentException();
    }

    public Matrix dotTranspose(Matrix that) {
        return dotTranspose(that, new Matrix(this.n, that.n));
    }

    public Matrix dotTranspose(Matrix that, Matrix result) {
        if (this.n == result.n && this.m == that.m && that.n == result.m) {
            for (int i = 0; i < this.n; i++) {
                for (int j = 0; j < that.n; j++) {
                    double s = 0.0d;
                    for (int k = 0; k < this.m; k++) {
                        s += get(i, k) * that.get(j, k);
                    }
                    result.put(i, j, s);
                }
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Matrix)) {
            return false;
        }
        Matrix other = (Matrix) that;
        if (this.n != other.n || this.m != other.m) {
            return false;
        }
        for (int i = 0; i < this.mem.length; i++) {
            if (this.mem[i] != other.mem[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = (this.n * 101) + this.m;
        for (double hashCode : this.mem) {
            h = (h * 37) + Double.hashCode(hashCode);
        }
        return h;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder((this.n * this.m) * 8);
        sb.append("[");
        for (int i = 0; i < this.mem.length; i++) {
            if (i > 0) {
                sb.append(i % this.m == 0 ? "; " : ", ");
            }
            sb.append(this.mem[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
