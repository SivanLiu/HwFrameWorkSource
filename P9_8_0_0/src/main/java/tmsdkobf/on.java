package tmsdkobf;

import java.io.IOException;
import java.io.InputStream;

public abstract class on {

    public interface a {
        void a(boolean z, int i, int i2);
    }

    public static class b {
        private int Iw;
        private String Ix;
        private int mPort;

        public b(String str, int i) {
            this.Ix = str;
            this.mPort = i;
        }

        public b(String str, int i, int i2) {
            this.Iw = i2;
            this.Ix = str;
            this.mPort = i;
        }

        protected Object clone() throws CloneNotSupportedException {
            return new b(this.Ix, this.mPort, this.Iw);
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            b -l_2_R = (b) obj;
            return -l_2_R.Ix.equals(this.Ix) && -l_2_R.mPort == this.mPort;
        }

        public int getPort() {
            return this.mPort;
        }

        public int hashCode() {
            return super.hashCode();
        }

        public String hd() {
            return this.Ix;
        }

        public String toString() {
            return this.mPort < 0 ? this.Ix : this.Ix + ":" + this.mPort;
        }
    }

    public static byte[] a(InputStream inputStream, int -l_4_I, int -l_7_I, a aVar) throws IOException {
        Object -l_5_R = new byte[-l_7_I];
        int -l_6_I = 0;
        int -l_7_I2 = -l_7_I;
        while (-l_6_I < -l_7_I && -l_7_I2 > 0) {
            int -l_8_I = inputStream.read(-l_5_R, -l_4_I, -l_7_I2);
            if (-l_8_I > 0) {
                -l_6_I += -l_8_I;
                -l_4_I += -l_8_I;
                -l_7_I2 -= -l_8_I;
                if (aVar != null) {
                    aVar.a(false, -l_6_I, -l_7_I);
                }
            } else if (aVar != null) {
                aVar.a(true, -l_6_I, -l_7_I);
            }
        }
        return -l_6_I == -l_7_I ? -l_5_R : null;
    }
}
