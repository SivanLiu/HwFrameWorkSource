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
        return inverse(new Matrix(this.n, this.m), new Matrix(this.n, this.m * 2));
    }

    public Matrix inverse(Matrix result, Matrix scratch) {
        if (this.n == this.m && this.n == result.n && this.m == result.m && this.n == scratch.n && this.m * 2 == scratch.m) {
            int j;
            int ii;
            double d;
            int i = 0;
            while (i < this.n) {
                j = 0;
                while (j < this.m) {
                    scratch.put(i, j, get(i, j));
                    scratch.put(i, this.m + j, i == j ? 1.0d : 0.0d);
                    j++;
                }
                i++;
            }
            for (i = 0; i < this.n; i++) {
                int ibest = i;
                double vbest = Math.abs(scratch.get(i, i));
                for (ii = i + 1; ii < this.n; ii++) {
                    double v = Math.abs(scratch.get(ii, i));
                    if (v > vbest) {
                        ibest = ii;
                        vbest = v;
                    }
                }
                if (ibest != i) {
                    for (j = 0; j < scratch.m; j++) {
                        double t = scratch.get(i, j);
                        scratch.put(i, j, scratch.get(ibest, j));
                        scratch.put(ibest, j, t);
                    }
                }
                d = scratch.get(i, i);
                if (d == 0.0d) {
                    throw new ArithmeticException("Singular matrix");
                }
                for (j = 0; j < scratch.m; j++) {
                    scratch.put(i, j, scratch.get(i, j) / d);
                }
                for (ii = i + 1; ii < this.n; ii++) {
                    d = scratch.get(ii, i);
                    for (j = 0; j < scratch.m; j++) {
                        scratch.put(ii, j, scratch.get(ii, j) - (scratch.get(i, j) * d));
                    }
                }
            }
            for (i = this.n - 1; i >= 0; i--) {
                for (ii = 0; ii < i; ii++) {
                    d = scratch.get(ii, i);
                    for (j = 0; j < scratch.m; j++) {
                        scratch.put(ii, j, scratch.get(ii, j) - (scratch.get(i, j) * d));
                    }
                }
            }
            for (i = 0; i < result.n; i++) {
                for (j = 0; j < result.m; j++) {
                    result.put(i, j, scratch.get(i, this.m + j));
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
