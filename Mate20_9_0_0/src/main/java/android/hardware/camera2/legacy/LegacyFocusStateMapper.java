package android.hardware.camera2.legacy;

import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.utils.ParamsUtils;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.Objects;

public class LegacyFocusStateMapper {
    private static final boolean DEBUG = false;
    private static String TAG = "LegacyFocusStateMapper";
    private String mAfModePrevious = null;
    private int mAfRun = 0;
    private int mAfState = 0;
    private int mAfStatePrevious = 0;
    private final Camera mCamera;
    private final Object mLock = new Object();

    public LegacyFocusStateMapper(Camera camera) {
        this.mCamera = (Camera) Preconditions.checkNotNull(camera, "camera must not be null");
    }

    /* JADX WARNING: Removed duplicated region for block: B:36:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x009a  */
    /* JADX WARNING: Removed duplicated region for block: B:108:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00cf  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00b3  */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x009a  */
    /* JADX WARNING: Removed duplicated region for block: B:108:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00cf  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00b3  */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x009a  */
    /* JADX WARNING: Removed duplicated region for block: B:108:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00cf  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00b3  */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x009a  */
    /* JADX WARNING: Removed duplicated region for block: B:108:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00cf  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00b3  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void processRequestTriggers(CaptureRequest captureRequest, Parameters parameters) {
        final int currentAfRun;
        CaptureRequest captureRequest2 = captureRequest;
        Preconditions.checkNotNull(captureRequest2, "captureRequest must not be null");
        int afStateAfterStart = 0;
        int afTrigger = ((Integer) ParamsUtils.getOrDefault(captureRequest2, CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(0))).intValue();
        final String afMode = parameters.getFocusMode();
        if (!Objects.equals(this.mAfModePrevious, afMode)) {
            synchronized (this.mLock) {
                this.mAfRun++;
                this.mAfState = 0;
            }
            this.mCamera.cancelAutoFocus();
        }
        this.mAfModePrevious = afMode;
        synchronized (this.mLock) {
            currentAfRun = this.mAfRun;
        }
        AutoFocusMoveCallback afMoveCallback = new AutoFocusMoveCallback() {
            /* JADX WARNING: Removed duplicated region for block: B:25:0x005e  */
            /* JADX WARNING: Removed duplicated region for block: B:26:0x0063  */
            /* JADX WARNING: Removed duplicated region for block: B:25:0x005e  */
            /* JADX WARNING: Removed duplicated region for block: B:26:0x0063  */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onAutoFocusMoving(boolean start, Camera camera) {
                synchronized (LegacyFocusStateMapper.this.mLock) {
                    String access$200;
                    if (currentAfRun != LegacyFocusStateMapper.this.mAfRun) {
                        access$200 = LegacyFocusStateMapper.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onAutoFocusMoving - ignoring move callbacks from old af run");
                        stringBuilder.append(currentAfRun);
                        Log.d(access$200, stringBuilder.toString());
                        return;
                    }
                    Object obj = 1;
                    int newAfState = start ? 1 : 2;
                    String str = afMode;
                    int hashCode = str.hashCode();
                    if (hashCode != -194628547) {
                        if (hashCode == 910005312) {
                            if (str.equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                obj = null;
                                switch (obj) {
                                    case null:
                                    case 1:
                                        break;
                                    default:
                                        access$200 = LegacyFocusStateMapper.TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("onAutoFocus - got unexpected onAutoFocus in mode ");
                                        stringBuilder2.append(afMode);
                                        Log.w(access$200, stringBuilder2.toString());
                                        break;
                                }
                                LegacyFocusStateMapper.this.mAfState = newAfState;
                            }
                        }
                    } else if (str.equals(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        switch (obj) {
                            case null:
                            case 1:
                                break;
                            default:
                                break;
                        }
                        LegacyFocusStateMapper.this.mAfState = newAfState;
                    }
                    obj = -1;
                    switch (obj) {
                        case null:
                        case 1:
                            break;
                        default:
                            break;
                    }
                    LegacyFocusStateMapper.this.mAfState = newAfState;
                }
            }
        };
        int hashCode = afMode.hashCode();
        int i = -1;
        if (hashCode == -194628547) {
            if (afMode.equals(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                hashCode = 3;
                switch (hashCode) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        break;
                }
                switch (afTrigger) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 3005871) {
            if (afMode.equals("auto")) {
                hashCode = 0;
                switch (hashCode) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        break;
                }
                switch (afTrigger) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 103652300) {
            if (afMode.equals(Parameters.FOCUS_MODE_MACRO)) {
                hashCode = 1;
                switch (hashCode) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        break;
                }
                switch (afTrigger) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 910005312 && afMode.equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            hashCode = 2;
            switch (hashCode) {
                case 0:
                case 1:
                case 2:
                case 3:
                    this.mCamera.setAutoFocusMoveCallback(afMoveCallback);
                    break;
            }
            switch (afTrigger) {
                case 0:
                    return;
                case 1:
                    currentAfRun = afMode.hashCode();
                    if (currentAfRun != -194628547) {
                        if (currentAfRun != 3005871) {
                            if (currentAfRun != 103652300) {
                                if (currentAfRun == 910005312 && afMode.equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                    i = 2;
                                }
                            } else if (afMode.equals(Parameters.FOCUS_MODE_MACRO)) {
                                i = 1;
                            }
                        } else if (afMode.equals("auto")) {
                            i = 0;
                        }
                    } else if (afMode.equals(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        i = 3;
                    }
                    switch (i) {
                        case 0:
                        case 1:
                            afStateAfterStart = 3;
                            break;
                        case 2:
                        case 3:
                            afStateAfterStart = 1;
                            break;
                    }
                    synchronized (this.mLock) {
                        currentAfRun = this.mAfRun + 1;
                        this.mAfRun = currentAfRun;
                        this.mAfState = afStateAfterStart;
                    }
                    if (afStateAfterStart != 0) {
                        this.mCamera.autoFocus(new AutoFocusCallback() {
                            /* JADX WARNING: Removed duplicated region for block: B:34:0x0081  */
                            /* JADX WARNING: Removed duplicated region for block: B:35:0x0086  */
                            /* JADX WARNING: Removed duplicated region for block: B:34:0x0081  */
                            /* JADX WARNING: Removed duplicated region for block: B:35:0x0086  */
                            /* JADX WARNING: Removed duplicated region for block: B:34:0x0081  */
                            /* JADX WARNING: Removed duplicated region for block: B:35:0x0086  */
                            /* JADX WARNING: Removed duplicated region for block: B:34:0x0081  */
                            /* JADX WARNING: Removed duplicated region for block: B:35:0x0086  */
                            /* Code decompiled incorrectly, please refer to instructions dump. */
                            public void onAutoFocus(boolean success, Camera camera) {
                                synchronized (LegacyFocusStateMapper.this.mLock) {
                                    int i = 1;
                                    if (LegacyFocusStateMapper.this.mAfRun != currentAfRun) {
                                        Log.d(LegacyFocusStateMapper.TAG, String.format("onAutoFocus - ignoring AF callback (old run %d, new run %d)", new Object[]{Integer.valueOf(currentAfRun), Integer.valueOf(latestAfRun)}));
                                        return;
                                    }
                                    int newAfState;
                                    if (success) {
                                        newAfState = 4;
                                    } else {
                                        newAfState = 5;
                                    }
                                    String str = afMode;
                                    int hashCode = str.hashCode();
                                    if (hashCode != -194628547) {
                                        if (hashCode != 3005871) {
                                            if (hashCode != 103652300) {
                                                if (hashCode == 910005312) {
                                                    if (str.equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                                        switch (i) {
                                                            case 0:
                                                            case 1:
                                                            case 2:
                                                            case 3:
                                                                break;
                                                            default:
                                                                String access$200 = LegacyFocusStateMapper.TAG;
                                                                StringBuilder stringBuilder = new StringBuilder();
                                                                stringBuilder.append("onAutoFocus - got unexpected onAutoFocus in mode ");
                                                                stringBuilder.append(afMode);
                                                                Log.w(access$200, stringBuilder.toString());
                                                                break;
                                                        }
                                                        LegacyFocusStateMapper.this.mAfState = newAfState;
                                                    }
                                                }
                                            } else if (str.equals(Parameters.FOCUS_MODE_MACRO)) {
                                                i = 3;
                                                switch (i) {
                                                    case 0:
                                                    case 1:
                                                    case 2:
                                                    case 3:
                                                        break;
                                                    default:
                                                        break;
                                                }
                                                LegacyFocusStateMapper.this.mAfState = newAfState;
                                            }
                                        } else if (str.equals("auto")) {
                                            i = 0;
                                            switch (i) {
                                                case 0:
                                                case 1:
                                                case 2:
                                                case 3:
                                                    break;
                                                default:
                                                    break;
                                            }
                                            LegacyFocusStateMapper.this.mAfState = newAfState;
                                        }
                                    } else if (str.equals(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                                        i = 2;
                                        switch (i) {
                                            case 0:
                                            case 1:
                                            case 2:
                                            case 3:
                                                break;
                                            default:
                                                break;
                                        }
                                        LegacyFocusStateMapper.this.mAfState = newAfState;
                                    }
                                    i = -1;
                                    switch (i) {
                                        case 0:
                                        case 1:
                                        case 2:
                                        case 3:
                                            break;
                                        default:
                                            break;
                                    }
                                    LegacyFocusStateMapper.this.mAfState = newAfState;
                                }
                            }
                        });
                        return;
                    }
                    return;
                case 2:
                    synchronized (this.mLock) {
                        synchronized (this.mLock) {
                            this.mAfRun++;
                            this.mAfState = 0;
                        }
                        this.mCamera.cancelAutoFocus();
                    }
                    return;
                default:
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("processRequestTriggers - ignoring unknown control.afTrigger = ");
                    stringBuilder.append(afTrigger);
                    Log.w(str, stringBuilder.toString());
                    return;
            }
        }
        hashCode = -1;
        switch (hashCode) {
            case 0:
            case 1:
            case 2:
            case 3:
                break;
        }
        switch (afTrigger) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            default:
                break;
        }
    }

    public void mapResultTriggers(CameraMetadataNative result) {
        int newAfState;
        Preconditions.checkNotNull(result, "result must not be null");
        synchronized (this.mLock) {
            newAfState = this.mAfState;
        }
        result.set(CaptureResult.CONTROL_AF_STATE, Integer.valueOf(newAfState));
        this.mAfStatePrevious = newAfState;
    }

    private static String afStateToString(int afState) {
        switch (afState) {
            case 0:
                return "INACTIVE";
            case 1:
                return "PASSIVE_SCAN";
            case 2:
                return "PASSIVE_FOCUSED";
            case 3:
                return "ACTIVE_SCAN";
            case 4:
                return "FOCUSED_LOCKED";
            case 5:
                return "NOT_FOCUSED_LOCKED";
            case 6:
                return "PASSIVE_UNFOCUSED";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UNKNOWN(");
                stringBuilder.append(afState);
                stringBuilder.append(")");
                return stringBuilder.toString();
        }
    }
}
