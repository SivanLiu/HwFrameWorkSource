package android.net;

import android.os.SystemClock;
import android.util.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class SntpClient {
    private static final boolean DBG = true;
    private static final int NTP_LEAP_NOSYNC = 3;
    private static final int NTP_MODE_BROADCAST = 5;
    private static final int NTP_MODE_CLIENT = 3;
    private static final int NTP_MODE_SERVER = 4;
    private static final int NTP_PACKET_SIZE = 48;
    private static final int NTP_PORT = 123;
    private static final int NTP_STRATUM_DEATH = 0;
    private static final int NTP_STRATUM_MAX = 15;
    private static final int NTP_VERSION = 3;
    private static final long OFFSET_1900_TO_1970 = 2208988800L;
    private static final int ORIGINATE_TIME_OFFSET = 24;
    private static final int RECEIVE_TIME_OFFSET = 32;
    private static final int REFERENCE_TIME_OFFSET = 16;
    private static final String TAG = "SntpClient";
    private static final int TRANSMIT_TIME_OFFSET = 40;
    private String mNtpIpAddress;
    private long mNtpTime;
    private long mNtpTimeReference;
    private long mRoundTripTime;

    private static class InvalidServerReplyException extends Exception {
        public InvalidServerReplyException(String message) {
            super(message);
        }
    }

    public boolean requestTime(String host, int timeout, Network network) {
        network.setPrivateDnsBypass(true);
        try {
            return requestTime(network.getByName(host), 123, timeout, network);
        } catch (Exception e) {
            EventLogTags.writeNtpFailure(host, e.toString());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("request time failed: ");
            stringBuilder.append(e);
            Log.d(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean requestTime(InetAddress address, int port, int timeout, Network network) {
        InetAddress inetAddress = address;
        int i = port;
        int i2 = timeout;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ntp request time : ");
        stringBuilder.append(inetAddress);
        stringBuilder.append("port : ");
        stringBuilder.append(i);
        stringBuilder.append(" timeout : ");
        stringBuilder.append(i2);
        Log.d(str, stringBuilder.toString());
        if (inetAddress != null) {
            this.mNtpIpAddress = address.toString();
        }
        DatagramSocket socket = null;
        int oldTag = TrafficStats.getAndSetThreadStatsTag(TrafficStats.TAG_SYSTEM_NTP);
        try {
            socket = new DatagramSocket();
            network.bindSocket(socket);
            socket.setSoTimeout(i2);
            byte[] buffer = new byte[48];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, inetAddress, i);
            buffer[0] = (byte) 27;
            long requestTime = System.currentTimeMillis();
            long requestTicks = SystemClock.elapsedRealtime();
            writeTimeStamp(buffer, 40, requestTime);
            socket.send(request);
            socket.receive(new DatagramPacket(buffer, buffer.length));
            long responseTicks = SystemClock.elapsedRealtime();
            long responseTime = requestTime + (responseTicks - requestTicks);
            byte leap = (byte) ((buffer[0] >> 6) & 3);
            byte mode = (byte) (buffer[0] & 7);
            i2 = buffer[1] & 255;
            long originateTime = readTimeStamp(buffer, 24);
            long receiveTime = readTimeStamp(buffer, 32);
            long transmitTime = readTimeStamp(buffer, 40);
            checkValidServerReply(leap, mode, i2, transmitTime);
            long roundTripTime = (responseTicks - requestTicks) - (transmitTime - receiveTime);
            long clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime)) / 2;
            if (inetAddress != null) {
                transmitTime = clockOffset;
                EventLogTags.writeNtpSuccess(address.toString(), roundTripTime, transmitTime);
            } else {
                transmitTime = clockOffset;
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("round trip: ");
            stringBuilder2.append(roundTripTime);
            stringBuilder2.append("ms, clock offset: ");
            stringBuilder2.append(transmitTime);
            stringBuilder2.append("ms");
            Log.d(str2, stringBuilder2.toString());
            this.mNtpTime = responseTime + transmitTime;
            this.mNtpTimeReference = responseTicks;
            this.mRoundTripTime = roundTripTime;
            socket.close();
            TrafficStats.setThreadStatsTag(oldTag);
            return true;
        } catch (Exception e) {
            if (inetAddress != null) {
                EventLogTags.writeNtpFailure(address.toString(), e.toString());
            }
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("request time failed: ");
            stringBuilder3.append(e);
            Log.d(str3, stringBuilder3.toString());
            if (socket != null) {
                socket.close();
            }
            TrafficStats.setThreadStatsTag(oldTag);
            return false;
        } catch (Throwable th) {
            if (socket != null) {
                socket.close();
            }
            TrafficStats.setThreadStatsTag(oldTag);
        }
    }

    @Deprecated
    public boolean requestTime(String host, int timeout) {
        Log.w(TAG, "Shame on you for calling the hidden API requestTime()!");
        return false;
    }

    public String getNtpIpAddress() {
        return this.mNtpIpAddress;
    }

    public long getNtpTime() {
        return this.mNtpTime;
    }

    public long getNtpTimeReference() {
        return this.mNtpTimeReference;
    }

    public long getRoundTripTime() {
        return this.mRoundTripTime;
    }

    private static void checkValidServerReply(byte leap, byte mode, int stratum, long transmitTime) throws InvalidServerReplyException {
        StringBuilder stringBuilder;
        if (leap == (byte) 3) {
            throw new InvalidServerReplyException("unsynchronized server");
        } else if (mode != (byte) 4 && mode != (byte) 5) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("untrusted mode: ");
            stringBuilder.append(mode);
            throw new InvalidServerReplyException(stringBuilder.toString());
        } else if (stratum == 0 || stratum > 15) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("untrusted stratum: ");
            stringBuilder.append(stratum);
            throw new InvalidServerReplyException(stringBuilder.toString());
        } else if (transmitTime == 0) {
            throw new InvalidServerReplyException("zero transmitTime");
        }
    }

    private long read32(byte[] buffer, int offset) {
        byte b0 = buffer[offset];
        byte b1 = buffer[offset + 1];
        byte b2 = buffer[offset + 2];
        byte b3 = buffer[offset + 3];
        return (((((long) ((b0 & 128) == 128 ? (b0 & 127) + 128 : b0)) << 24) + (((long) ((b1 & 128) == 128 ? (b1 & 127) + 128 : b1)) << 16)) + (((long) ((b2 & 128) == 128 ? (b2 & 127) + 128 : b2)) << 8)) + ((long) ((b3 & 128) == 128 ? 128 + (b3 & 127) : b3));
    }

    private long readTimeStamp(byte[] buffer, int offset) {
        long seconds = read32(buffer, offset);
        long fraction = read32(buffer, offset + 4);
        if (seconds == 0 && fraction == 0) {
            return 0;
        }
        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((1000 * fraction) / 4294967296L);
    }

    private void writeTimeStamp(byte[] buffer, int offset, long time) {
        byte[] bArr = buffer;
        int i = offset;
        if (time == 0) {
            Arrays.fill(bArr, i, i + 8, (byte) 0);
            return;
        }
        long seconds = time / 1000;
        long milliseconds = time - (seconds * 1000);
        seconds += OFFSET_1900_TO_1970;
        int offset2 = i + 1;
        bArr[i] = (byte) ((int) (seconds >> 24));
        i = offset2 + 1;
        bArr[offset2] = (byte) ((int) (seconds >> 16));
        offset2 = i + 1;
        bArr[i] = (byte) ((int) (seconds >> 8));
        i = offset2 + 1;
        bArr[offset2] = (byte) ((int) (seconds >> 0));
        long fraction = (4294967296L * milliseconds) / 1000;
        int offset3 = i + 1;
        bArr[i] = (byte) ((int) (fraction >> 24));
        i = offset3 + 1;
        bArr[offset3] = (byte) ((int) (fraction >> 16));
        offset3 = i + 1;
        bArr[i] = (byte) ((int) (fraction >> 8));
        i = offset3 + 1;
        bArr[offset3] = (byte) ((int) (Math.random() * 255.0d));
    }
}
