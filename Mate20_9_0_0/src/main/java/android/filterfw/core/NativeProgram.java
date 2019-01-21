package android.filterfw.core;

public class NativeProgram extends Program {
    private boolean mHasGetValueFunction = false;
    private boolean mHasInitFunction = false;
    private boolean mHasResetFunction = false;
    private boolean mHasSetValueFunction = false;
    private boolean mHasTeardownFunction = false;
    private boolean mTornDown = false;
    private int nativeProgramId;

    private native boolean allocate();

    private native boolean bindGetValueFunction(String str);

    private native boolean bindInitFunction(String str);

    private native boolean bindProcessFunction(String str);

    private native boolean bindResetFunction(String str);

    private native boolean bindSetValueFunction(String str);

    private native boolean bindTeardownFunction(String str);

    private native String callNativeGetValue(String str);

    private native boolean callNativeInit();

    private native boolean callNativeProcess(NativeFrame[] nativeFrameArr, NativeFrame nativeFrame);

    private native boolean callNativeReset();

    private native boolean callNativeSetValue(String str, String str2);

    private native boolean callNativeTeardown();

    private native boolean deallocate();

    private native boolean nativeInit();

    private native boolean openNativeLibrary(String str);

    public NativeProgram(String nativeLibName, String nativeFunctionPrefix) {
        allocate();
        String fullLibName = new StringBuilder();
        fullLibName.append("lib");
        fullLibName.append(nativeLibName);
        fullLibName.append(".so");
        fullLibName = fullLibName.toString();
        if (openNativeLibrary(fullLibName)) {
            String processFuncName = new StringBuilder();
            processFuncName.append(nativeFunctionPrefix);
            processFuncName.append("_process");
            processFuncName = processFuncName.toString();
            if (bindProcessFunction(processFuncName)) {
                String initFuncName = new StringBuilder();
                initFuncName.append(nativeFunctionPrefix);
                initFuncName.append("_init");
                this.mHasInitFunction = bindInitFunction(initFuncName.toString());
                String teardownFuncName = new StringBuilder();
                teardownFuncName.append(nativeFunctionPrefix);
                teardownFuncName.append("_teardown");
                this.mHasTeardownFunction = bindTeardownFunction(teardownFuncName.toString());
                String setValueFuncName = new StringBuilder();
                setValueFuncName.append(nativeFunctionPrefix);
                setValueFuncName.append("_setvalue");
                this.mHasSetValueFunction = bindSetValueFunction(setValueFuncName.toString());
                String getValueFuncName = new StringBuilder();
                getValueFuncName.append(nativeFunctionPrefix);
                getValueFuncName.append("_getvalue");
                this.mHasGetValueFunction = bindGetValueFunction(getValueFuncName.toString());
                String resetFuncName = new StringBuilder();
                resetFuncName.append(nativeFunctionPrefix);
                resetFuncName.append("_reset");
                this.mHasResetFunction = bindResetFunction(resetFuncName.toString());
                if (this.mHasInitFunction && !callNativeInit()) {
                    throw new RuntimeException("Could not initialize NativeProgram!");
                }
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not find native program function name ");
            stringBuilder.append(processFuncName);
            stringBuilder.append(" in library ");
            stringBuilder.append(fullLibName);
            stringBuilder.append("! This function is required!");
            throw new RuntimeException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Could not find native library named '");
        stringBuilder2.append(fullLibName);
        stringBuilder2.append("' required for native program!");
        throw new RuntimeException(stringBuilder2.toString());
    }

    public void tearDown() {
        if (!this.mTornDown) {
            if (!this.mHasTeardownFunction || callNativeTeardown()) {
                deallocate();
                this.mTornDown = true;
                return;
            }
            throw new RuntimeException("Could not tear down NativeProgram!");
        }
    }

    public void reset() {
        if (this.mHasResetFunction && !callNativeReset()) {
            throw new RuntimeException("Could not reset NativeProgram!");
        }
    }

    protected void finalize() throws Throwable {
        tearDown();
    }

    public void process(Frame[] inputs, Frame output) {
        if (this.mTornDown) {
            throw new RuntimeException("NativeProgram already torn down!");
        }
        NativeFrame[] nativeInputs = new NativeFrame[inputs.length];
        int i = 0;
        while (i < inputs.length) {
            if (inputs[i] == null || (inputs[i] instanceof NativeFrame)) {
                nativeInputs[i] = (NativeFrame) inputs[i];
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("NativeProgram got non-native frame as input ");
                stringBuilder.append(i);
                stringBuilder.append("!");
                throw new RuntimeException(stringBuilder.toString());
            }
        }
        if (output != null && !(output instanceof NativeFrame)) {
            throw new RuntimeException("NativeProgram got non-native output frame!");
        } else if (!callNativeProcess(nativeInputs, (NativeFrame) output)) {
            throw new RuntimeException("Calling native process() caused error!");
        }
    }

    public void setHostValue(String variableName, Object value) {
        if (this.mTornDown) {
            throw new RuntimeException("NativeProgram already torn down!");
        } else if (!this.mHasSetValueFunction) {
            throw new RuntimeException("Attempting to set native variable, but native code does not define native setvalue function!");
        } else if (!callNativeSetValue(variableName, value.toString())) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error setting native value for variable '");
            stringBuilder.append(variableName);
            stringBuilder.append("'!");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public Object getHostValue(String variableName) {
        if (this.mTornDown) {
            throw new RuntimeException("NativeProgram already torn down!");
        } else if (this.mHasGetValueFunction) {
            return callNativeGetValue(variableName);
        } else {
            throw new RuntimeException("Attempting to get native variable, but native code does not define native getvalue function!");
        }
    }

    static {
        System.loadLibrary("filterfw");
    }
}
