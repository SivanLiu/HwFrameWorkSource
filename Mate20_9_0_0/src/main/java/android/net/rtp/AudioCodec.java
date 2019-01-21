package android.net.rtp;

import java.util.Arrays;

public class AudioCodec {
    public static final AudioCodec AMR = new AudioCodec(97, "AMR/8000", null);
    public static final AudioCodec GSM = new AudioCodec(3, "GSM/8000", null);
    public static final AudioCodec GSM_EFR = new AudioCodec(96, "GSM-EFR/8000", null);
    public static final AudioCodec PCMA = new AudioCodec(8, "PCMA/8000", null);
    public static final AudioCodec PCMU = new AudioCodec(0, "PCMU/8000", null);
    private static final AudioCodec[] sCodecs = new AudioCodec[]{GSM_EFR, AMR, GSM, PCMU, PCMA};
    public final String fmtp;
    public final String rtpmap;
    public final int type;

    private AudioCodec(int type, String rtpmap, String fmtp) {
        this.type = type;
        this.rtpmap = rtpmap;
        this.fmtp = fmtp;
    }

    public static AudioCodec[] getCodecs() {
        return (AudioCodec[]) Arrays.copyOf(sCodecs, sCodecs.length);
    }

    public static AudioCodec getCodec(int type, String rtpmap, String fmtp) {
        if (type < 0 || type > 127) {
            return null;
        }
        String channels;
        AudioCodec hint = null;
        int channels2 = 0;
        if (rtpmap != null) {
            String clue = rtpmap.trim().toUpperCase();
            AudioCodec[] audioCodecArr = sCodecs;
            int length = audioCodecArr.length;
            while (channels < length) {
                AudioCodec codec = audioCodecArr[channels];
                if (clue.startsWith(codec.rtpmap)) {
                    channels = clue.substring(codec.rtpmap.length());
                    if (channels.length() == 0 || channels.equals("/1")) {
                        hint = codec;
                    }
                } else {
                    channels++;
                }
            }
        } else if (type < 96) {
            AudioCodec[] audioCodecArr2 = sCodecs;
            int length2 = audioCodecArr2.length;
            while (channels2 < length2) {
                AudioCodec codec2 = audioCodecArr2[channels2];
                if (type == codec2.type) {
                    hint = codec2;
                    rtpmap = codec2.rtpmap;
                    break;
                }
                channels2++;
            }
        }
        if (hint == null) {
            return null;
        }
        if (hint == AMR && fmtp != null) {
            channels = fmtp.toLowerCase();
            if (channels.contains("crc=1") || channels.contains("robust-sorting=1") || channels.contains("interleaving=")) {
                return null;
            }
        }
        return new AudioCodec(type, rtpmap, fmtp);
    }
}
