package com.android.commands.media;

import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.IAudioService.Stub;
import android.os.ServiceManager;
import android.util.AndroidException;
import com.android.internal.os.BaseCommand;
import java.io.PrintStream;

public class VolumeCtrl {
    private static final String ADJUST_LOWER = "lower";
    private static final String ADJUST_RAISE = "raise";
    private static final String ADJUST_SAME = "same";
    static final String LOG_OK = "[ok]";
    static final String LOG_V = "[v]";
    static final String LOG_W = "[w]";
    private static final String TAG = "VolumeCtrl";
    public static final String USAGE = new String("the options are as follows: \n\t\t--stream STREAM selects the stream to control, see AudioManager.STREAM_*\n\t\t                controls AudioManager.STREAM_MUSIC if no stream is specified\n\t\t--set INDEX     sets the volume index value\n\t\t--adj DIRECTION adjusts the volume, use raise|same|lower for the direction\n\t\t--get           outputs the current volume\n\t\t--show          shows the UI during the volume change\n\texamples:\n\t\tadb shell media volume --show --stream 3 --set 11\n\t\tadb shell media volume --stream 0 --adj lower\n\t\tadb shell media volume --stream 3 --get\n");
    private static final int VOLUME_CONTROL_MODE_ADJUST = 2;
    private static final int VOLUME_CONTROL_MODE_SET = 1;

    /* JADX WARNING: Removed duplicated region for block: B:49:0x0121  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0158  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x0156  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x0154  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x0121  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0158  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x0156  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x0154  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x0121  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0158  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x0156  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x0154  */
    /* JADX WARNING: Missing block: B:9:0x002d, code skipped:
            if (r9.equals("--show") != false) goto L_0x004f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void run(BaseCommand cmd) throws Exception {
        BaseCommand baseCommand = cmd;
        int stream = 3;
        int volIndex = 5;
        int mode = 0;
        int adjDir = VOLUME_CONTROL_MODE_SET;
        boolean showUi = false;
        boolean doGet = false;
        String adjustment = null;
        while (true) {
            String nextOption = cmd.nextOption();
            String option = nextOption;
            int i = 0;
            if (nextOption != null) {
                switch (option.hashCode()) {
                    case 42995463:
                        if (option.equals("--adj")) {
                            i = 4;
                            break;
                        }
                    case 43001270:
                        if (option.equals("--get")) {
                            i = VOLUME_CONTROL_MODE_SET;
                            break;
                        }
                    case 43012802:
                        if (option.equals("--set")) {
                            i = 3;
                            break;
                        }
                    case 1333399709:
                        break;
                    case 1508023584:
                        if (option.equals("--stream")) {
                            i = VOLUME_CONTROL_MODE_ADJUST;
                            break;
                        }
                    default:
                        i = -1;
                        break;
                }
                StringBuilder stringBuilder;
                switch (i) {
                    case 0:
                        showUi = true;
                        break;
                    case VOLUME_CONTROL_MODE_SET /*1*/:
                        doGet = true;
                        log(LOG_V, "will get volume");
                        break;
                    case VOLUME_CONTROL_MODE_ADJUST /*2*/:
                        stream = Integer.decode(cmd.nextArgRequired()).intValue();
                        nextOption = LOG_V;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("will control stream=");
                        stringBuilder.append(stream);
                        stringBuilder.append(" (");
                        stringBuilder.append(streamName(stream));
                        stringBuilder.append(")");
                        log(nextOption, stringBuilder.toString());
                        break;
                    case 3:
                        volIndex = Integer.decode(cmd.nextArgRequired()).intValue();
                        mode = VOLUME_CONTROL_MODE_SET;
                        nextOption = LOG_V;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("will set volume to index=");
                        stringBuilder.append(volIndex);
                        log(nextOption, stringBuilder.toString());
                        break;
                    case 4:
                        mode = VOLUME_CONTROL_MODE_ADJUST;
                        adjustment = cmd.nextArgRequired();
                        log(LOG_V, "will adjust volume");
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown argument ");
                        stringBuilder.append(option);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            if (mode == VOLUME_CONTROL_MODE_ADJUST) {
                if (adjustment != null) {
                    int i2;
                    int hashCode = adjustment.hashCode();
                    if (hashCode == 3522662) {
                        if (adjustment.equals(ADJUST_SAME)) {
                            i2 = VOLUME_CONTROL_MODE_SET;
                            switch (i2) {
                                case 0:
                                    break;
                                case VOLUME_CONTROL_MODE_SET /*1*/:
                                    break;
                                case VOLUME_CONTROL_MODE_ADJUST /*2*/:
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else if (hashCode == 103164673) {
                        if (adjustment.equals(ADJUST_LOWER)) {
                            i2 = VOLUME_CONTROL_MODE_ADJUST;
                            switch (i2) {
                                case 0:
                                    break;
                                case VOLUME_CONTROL_MODE_SET /*1*/:
                                    break;
                                case VOLUME_CONTROL_MODE_ADJUST /*2*/:
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else if (hashCode == 108275692 && adjustment.equals(ADJUST_RAISE)) {
                        i2 = 0;
                        switch (i2) {
                            case 0:
                                adjDir = VOLUME_CONTROL_MODE_SET;
                                break;
                            case VOLUME_CONTROL_MODE_SET /*1*/:
                                adjDir = 0;
                                break;
                            case VOLUME_CONTROL_MODE_ADJUST /*2*/:
                                adjDir = -1;
                                break;
                            default:
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Error: no valid volume adjustment, was ");
                                stringBuilder2.append(adjustment);
                                stringBuilder2.append(", expected ");
                                stringBuilder2.append(ADJUST_LOWER);
                                stringBuilder2.append("|");
                                stringBuilder2.append(ADJUST_SAME);
                                stringBuilder2.append("|");
                                stringBuilder2.append(ADJUST_RAISE);
                                baseCommand.showError(stringBuilder2.toString());
                                return;
                        }
                    }
                    i2 = -1;
                    switch (i2) {
                        case 0:
                            break;
                        case VOLUME_CONTROL_MODE_SET /*1*/:
                            break;
                        case VOLUME_CONTROL_MODE_ADJUST /*2*/:
                            break;
                        default:
                            break;
                    }
                }
                baseCommand.showError("Error: no valid volume adjustment (null)");
                return;
            }
            log(LOG_V, "Connecting to AudioService");
            IAudioService audioService = Stub.asInterface(ServiceManager.checkService("audio"));
            if (audioService == null) {
                System.err.println("Error type 2");
                throw new AndroidException("Can't connect to audio service; is the system running?");
            } else if (mode != VOLUME_CONTROL_MODE_SET || (volIndex <= audioService.getStreamMaxVolume(stream) && volIndex >= audioService.getStreamMinVolume(stream))) {
                if (showUi) {
                    i = VOLUME_CONTROL_MODE_SET;
                }
                int flag = i;
                String pack = cmd.getClass().getPackage().getName();
                if (mode == VOLUME_CONTROL_MODE_SET) {
                    audioService.setStreamVolume(stream, volIndex, flag, pack);
                } else if (mode == VOLUME_CONTROL_MODE_ADJUST) {
                    audioService.adjustStreamVolume(stream, adjDir, flag, pack);
                }
                if (doGet) {
                    String str = LOG_V;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("volume is ");
                    stringBuilder3.append(audioService.getStreamVolume(stream));
                    stringBuilder3.append(" in range [");
                    stringBuilder3.append(audioService.getStreamMinVolume(stream));
                    stringBuilder3.append("..");
                    stringBuilder3.append(audioService.getStreamMaxVolume(stream));
                    stringBuilder3.append("]");
                    log(str, stringBuilder3.toString());
                }
                return;
            } else {
                baseCommand.showError(String.format("Error: invalid volume index %d for stream %d (should be in [%d..%d])", new Object[]{Integer.valueOf(volIndex), Integer.valueOf(stream), Integer.valueOf(audioService.getStreamMinVolume(stream)), Integer.valueOf(audioService.getStreamMaxVolume(stream))}));
                return;
            }
        }
    }

    static void log(String code, String msg) {
        PrintStream printStream = System.out;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(code);
        stringBuilder.append(" ");
        stringBuilder.append(msg);
        printStream.println(stringBuilder.toString());
    }

    static String streamName(int stream) {
        try {
            return AudioSystem.STREAM_NAMES[stream];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "invalid stream";
        }
    }
}
