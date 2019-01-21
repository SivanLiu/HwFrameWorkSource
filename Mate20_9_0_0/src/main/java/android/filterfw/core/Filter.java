package android.filterfw.core;

import android.filterfw.format.ObjectFormat;
import android.filterfw.io.GraphIOException;
import android.filterfw.io.TextGraphReader;
import android.util.Log;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public abstract class Filter {
    static final int STATUS_ERROR = 6;
    static final int STATUS_FINISHED = 5;
    static final int STATUS_PREINIT = 0;
    static final int STATUS_PREPARED = 2;
    static final int STATUS_PROCESSING = 3;
    static final int STATUS_RELEASED = 7;
    static final int STATUS_SLEEPING = 4;
    static final int STATUS_UNPREPARED = 1;
    private static final String TAG = "Filter";
    private long mCurrentTimestamp;
    private HashSet<Frame> mFramesToRelease;
    private HashMap<String, Frame> mFramesToSet;
    private int mInputCount = -1;
    private HashMap<String, InputPort> mInputPorts;
    private boolean mIsOpen = false;
    private boolean mLogVerbose;
    private String mName;
    private int mOutputCount = -1;
    private HashMap<String, OutputPort> mOutputPorts;
    private int mSleepDelay;
    private int mStatus = 0;

    public abstract void process(FilterContext filterContext);

    public abstract void setupPorts();

    public Filter(String name) {
        this.mName = name;
        this.mFramesToRelease = new HashSet();
        this.mFramesToSet = new HashMap();
        this.mStatus = 0;
        this.mLogVerbose = Log.isLoggable(TAG, 2);
    }

    public static final boolean isAvailable(String filterName) {
        try {
            try {
                Thread.currentThread().getContextClassLoader().loadClass(filterName).asSubclass(Filter.class);
                return true;
            } catch (ClassCastException e) {
                return false;
            }
        } catch (ClassNotFoundException e2) {
            return false;
        }
    }

    public final void initWithValueMap(KeyValueMap valueMap) {
        initFinalPorts(valueMap);
        initRemainingPorts(valueMap);
        this.mStatus = 1;
    }

    public final void initWithAssignmentString(String assignments) {
        try {
            initWithValueMap(new TextGraphReader().readKeyValueAssignments(assignments));
        } catch (GraphIOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public final void initWithAssignmentList(Object... keyValues) {
        KeyValueMap valueMap = new KeyValueMap();
        valueMap.setKeyValues(keyValues);
        initWithValueMap(valueMap);
    }

    public final void init() throws ProtocolException {
        initWithValueMap(new KeyValueMap());
    }

    public String getFilterClassName() {
        return getClass().getSimpleName();
    }

    public final String getName() {
        return this.mName;
    }

    public boolean isOpen() {
        return this.mIsOpen;
    }

    public void setInputFrame(String inputName, Frame frame) {
        FilterPort port = getInputPort(inputName);
        if (!port.isOpen()) {
            port.open();
        }
        port.setFrame(frame);
    }

    public final void setInputValue(String inputName, Object value) {
        setInputFrame(inputName, wrapInputValue(inputName, value));
    }

    protected void prepare(FilterContext context) {
    }

    protected void parametersUpdated(Set<String> set) {
    }

    protected void delayNextProcess(int millisecs) {
        this.mSleepDelay = millisecs;
        this.mStatus = 4;
    }

    public FrameFormat getOutputFormat(String portName, FrameFormat inputFormat) {
        return null;
    }

    public final FrameFormat getInputFormat(String portName) {
        return getInputPort(portName).getSourceFormat();
    }

    public void open(FilterContext context) {
    }

    public final int getSleepDelay() {
        return 250;
    }

    public void close(FilterContext context) {
    }

    public void tearDown(FilterContext context) {
    }

    public final int getNumberOfConnectedInputs() {
        int c = 0;
        for (InputPort inputPort : this.mInputPorts.values()) {
            if (inputPort.isConnected()) {
                c++;
            }
        }
        return c;
    }

    public final int getNumberOfConnectedOutputs() {
        int c = 0;
        for (OutputPort outputPort : this.mOutputPorts.values()) {
            if (outputPort.isConnected()) {
                c++;
            }
        }
        return c;
    }

    public final int getNumberOfInputs() {
        return this.mOutputPorts == null ? 0 : this.mInputPorts.size();
    }

    public final int getNumberOfOutputs() {
        return this.mInputPorts == null ? 0 : this.mOutputPorts.size();
    }

    public final InputPort getInputPort(String portName) {
        if (this.mInputPorts != null) {
            InputPort result = (InputPort) this.mInputPorts.get(portName);
            if (result != null) {
                return result;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown input port '");
            stringBuilder.append(portName);
            stringBuilder.append("' on filter ");
            stringBuilder.append(this);
            stringBuilder.append("!");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Attempting to access input port '");
        stringBuilder2.append(portName);
        stringBuilder2.append("' of ");
        stringBuilder2.append(this);
        stringBuilder2.append(" before Filter has been initialized!");
        throw new NullPointerException(stringBuilder2.toString());
    }

    public final OutputPort getOutputPort(String portName) {
        if (this.mInputPorts != null) {
            OutputPort result = (OutputPort) this.mOutputPorts.get(portName);
            if (result != null) {
                return result;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown output port '");
            stringBuilder.append(portName);
            stringBuilder.append("' on filter ");
            stringBuilder.append(this);
            stringBuilder.append("!");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Attempting to access output port '");
        stringBuilder2.append(portName);
        stringBuilder2.append("' of ");
        stringBuilder2.append(this);
        stringBuilder2.append(" before Filter has been initialized!");
        throw new NullPointerException(stringBuilder2.toString());
    }

    protected final void pushOutput(String name, Frame frame) {
        if (frame.getTimestamp() == -2) {
            if (this.mLogVerbose) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Default-setting output Frame timestamp on port ");
                stringBuilder.append(name);
                stringBuilder.append(" to ");
                stringBuilder.append(this.mCurrentTimestamp);
                Log.v(str, stringBuilder.toString());
            }
            frame.setTimestamp(this.mCurrentTimestamp);
        }
        getOutputPort(name).pushFrame(frame);
    }

    protected final Frame pullInput(String name) {
        Frame result = getInputPort(name).pullFrame();
        if (this.mCurrentTimestamp == -1) {
            this.mCurrentTimestamp = result.getTimestamp();
            if (this.mLogVerbose) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Default-setting current timestamp from input port ");
                stringBuilder.append(name);
                stringBuilder.append(" to ");
                stringBuilder.append(this.mCurrentTimestamp);
                Log.v(str, stringBuilder.toString());
            }
        }
        this.mFramesToRelease.add(result);
        return result;
    }

    public void fieldPortValueUpdated(String name, FilterContext context) {
    }

    protected void transferInputPortFrame(String name, FilterContext context) {
        getInputPort(name).transfer(context);
    }

    protected void initProgramInputs(Program program, FilterContext context) {
        if (program != null) {
            for (InputPort inputPort : this.mInputPorts.values()) {
                if (inputPort.getTarget() == program) {
                    inputPort.transfer(context);
                }
            }
        }
    }

    protected void addInputPort(String name) {
        addMaskedInputPort(name, null);
    }

    protected void addMaskedInputPort(String name, FrameFormat formatMask) {
        InputPort port = new StreamPort(this, name);
        if (this.mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Filter ");
            stringBuilder.append(this);
            stringBuilder.append(" adding ");
            stringBuilder.append(port);
            Log.v(str, stringBuilder.toString());
        }
        this.mInputPorts.put(name, port);
        port.setPortFormat(formatMask);
    }

    protected void addOutputPort(String name, FrameFormat format) {
        OutputPort port = new OutputPort(this, name);
        if (this.mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Filter ");
            stringBuilder.append(this);
            stringBuilder.append(" adding ");
            stringBuilder.append(port);
            Log.v(str, stringBuilder.toString());
        }
        port.setPortFormat(format);
        this.mOutputPorts.put(name, port);
    }

    protected void addOutputBasedOnInput(String outputName, String inputName) {
        OutputPort port = new OutputPort(this, outputName);
        if (this.mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Filter ");
            stringBuilder.append(this);
            stringBuilder.append(" adding ");
            stringBuilder.append(port);
            Log.v(str, stringBuilder.toString());
        }
        port.setBasePort(getInputPort(inputName));
        this.mOutputPorts.put(outputName, port);
    }

    protected void addFieldPort(String name, Field field, boolean hasDefault, boolean isFinal) {
        InputPort fieldPort;
        field.setAccessible(true);
        if (isFinal) {
            fieldPort = new FinalPort(this, name, field, hasDefault);
        } else {
            fieldPort = new FieldPort(this, name, field, hasDefault);
        }
        if (this.mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Filter ");
            stringBuilder.append(this);
            stringBuilder.append(" adding ");
            stringBuilder.append(fieldPort);
            Log.v(str, stringBuilder.toString());
        }
        fieldPort.setPortFormat(ObjectFormat.fromClass(field.getType(), 1));
        this.mInputPorts.put(name, fieldPort);
    }

    protected void addProgramPort(String name, String varName, Field field, Class varType, boolean hasDefault) {
        field.setAccessible(true);
        InputPort programPort = new ProgramPort(this, name, varName, field, hasDefault);
        if (this.mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Filter ");
            stringBuilder.append(this);
            stringBuilder.append(" adding ");
            stringBuilder.append(programPort);
            Log.v(str, stringBuilder.toString());
        }
        programPort.setPortFormat(ObjectFormat.fromClass(varType, 1));
        this.mInputPorts.put(name, programPort);
    }

    protected void closeOutputPort(String name) {
        getOutputPort(name).close();
    }

    protected void setWaitsOnInputPort(String portName, boolean waits) {
        getInputPort(portName).setBlocking(waits);
    }

    protected void setWaitsOnOutputPort(String portName, boolean waits) {
        getOutputPort(portName).setBlocking(waits);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("'");
        stringBuilder.append(getName());
        stringBuilder.append("' (");
        stringBuilder.append(getFilterClassName());
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    final Collection<InputPort> getInputPorts() {
        return this.mInputPorts.values();
    }

    final Collection<OutputPort> getOutputPorts() {
        return this.mOutputPorts.values();
    }

    final synchronized int getStatus() {
        return this.mStatus;
    }

    final synchronized void unsetStatus(int flag) {
        this.mStatus &= ~flag;
    }

    final synchronized void performOpen(FilterContext context) {
        if (!this.mIsOpen) {
            String str;
            StringBuilder stringBuilder;
            if (this.mStatus == 1) {
                if (this.mLogVerbose) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Preparing ");
                    stringBuilder2.append(this);
                    Log.v(str, stringBuilder2.toString());
                }
                prepare(context);
                this.mStatus = 2;
            }
            if (this.mStatus == 2) {
                if (this.mLogVerbose) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Opening ");
                    stringBuilder.append(this);
                    Log.v(str, stringBuilder.toString());
                }
                open(context);
                this.mStatus = 3;
            }
            if (this.mStatus == 3) {
                this.mIsOpen = true;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Filter ");
                stringBuilder.append(this);
                stringBuilder.append(" was brought into invalid state during opening (state: ");
                stringBuilder.append(this.mStatus);
                stringBuilder.append(")!");
                throw new RuntimeException(stringBuilder.toString());
            }
        }
    }

    final synchronized void performProcess(FilterContext context) {
        StringBuilder stringBuilder;
        if (this.mStatus != 7) {
            transferInputFrames(context);
            if (this.mStatus < 3) {
                performOpen(context);
            }
            if (this.mLogVerbose) {
                String str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Processing ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            this.mCurrentTimestamp = -1;
            process(context);
            releasePulledFrames(context);
            if (filterMustClose()) {
                performClose(context);
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Filter ");
            stringBuilder.append(this);
            stringBuilder.append(" is already torn down!");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    final synchronized void performClose(FilterContext context) {
        if (this.mIsOpen) {
            if (this.mLogVerbose) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Closing ");
                stringBuilder.append(this);
                Log.v(str, stringBuilder.toString());
            }
            this.mIsOpen = false;
            this.mStatus = 2;
            close(context);
            closePorts();
        }
    }

    final synchronized void performTearDown(FilterContext context) {
        performClose(context);
        if (this.mStatus != 7) {
            tearDown(context);
            this.mStatus = 7;
        }
    }

    /* JADX WARNING: Missing block: B:13:0x003f, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final synchronized boolean canProcess() {
        if (this.mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Checking if can process: ");
            stringBuilder.append(this);
            stringBuilder.append(" (");
            stringBuilder.append(this.mStatus);
            stringBuilder.append(").");
            Log.v(str, stringBuilder.toString());
        }
        boolean z = false;
        if (this.mStatus > 3) {
            return false;
        }
        if (inputConditionsMet() && outputConditionsMet()) {
            z = true;
        }
    }

    final void openOutputs() {
        if (this.mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Opening all output ports on ");
            stringBuilder.append(this);
            stringBuilder.append("!");
            Log.v(str, stringBuilder.toString());
        }
        for (OutputPort outputPort : this.mOutputPorts.values()) {
            if (!outputPort.isOpen()) {
                outputPort.open();
            }
        }
    }

    final void clearInputs() {
        for (InputPort inputPort : this.mInputPorts.values()) {
            inputPort.clear();
        }
    }

    final void clearOutputs() {
        for (OutputPort outputPort : this.mOutputPorts.values()) {
            outputPort.clear();
        }
    }

    final void notifyFieldPortValueUpdated(String name, FilterContext context) {
        if (this.mStatus == 3 || this.mStatus == 2) {
            fieldPortValueUpdated(name, context);
        }
    }

    final synchronized void pushInputFrame(String inputName, Frame frame) {
        FilterPort port = getInputPort(inputName);
        if (!port.isOpen()) {
            port.open();
        }
        port.pushFrame(frame);
    }

    final synchronized void pushInputValue(String inputName, Object value) {
        pushInputFrame(inputName, wrapInputValue(inputName, value));
    }

    private final void initFinalPorts(KeyValueMap values) {
        this.mInputPorts = new HashMap();
        this.mOutputPorts = new HashMap();
        addAndSetFinalPorts(values);
    }

    private final void initRemainingPorts(KeyValueMap values) {
        addAnnotatedPorts();
        setupPorts();
        setInitialInputValues(values);
    }

    private final void addAndSetFinalPorts(KeyValueMap values) {
        for (Field field : getClass().getDeclaredFields()) {
            Annotation annotation = field.getAnnotation(GenerateFinalPort.class);
            Annotation annotation2 = annotation;
            if (annotation != null) {
                GenerateFinalPort generator = (GenerateFinalPort) annotation2;
                String name = generator.name().isEmpty() ? field.getName() : generator.name();
                addFieldPort(name, field, generator.hasDefault(), true);
                if (values.containsKey(name)) {
                    setImmediateInputValue(name, values.get(name));
                    values.remove(name);
                } else if (!generator.hasDefault()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No value specified for final input port '");
                    stringBuilder.append(name);
                    stringBuilder.append("' of filter ");
                    stringBuilder.append(this);
                    stringBuilder.append("!");
                    throw new RuntimeException(stringBuilder.toString());
                }
            }
        }
    }

    private final void addAnnotatedPorts() {
        for (Field field : getClass().getDeclaredFields()) {
            Annotation annotation = field.getAnnotation(GenerateFieldPort.class);
            Annotation annotation2 = annotation;
            if (annotation != null) {
                addFieldGenerator((GenerateFieldPort) annotation2, field);
            } else {
                annotation = field.getAnnotation(GenerateProgramPort.class);
                annotation2 = annotation;
                if (annotation != null) {
                    addProgramGenerator((GenerateProgramPort) annotation2, field);
                } else {
                    annotation = field.getAnnotation(GenerateProgramPorts.class);
                    annotation2 = annotation;
                    if (annotation != null) {
                        for (GenerateProgramPort generator : ((GenerateProgramPorts) annotation2).value()) {
                            addProgramGenerator(generator, field);
                        }
                    }
                }
            }
        }
    }

    private final void addFieldGenerator(GenerateFieldPort generator, Field field) {
        addFieldPort(generator.name().isEmpty() ? field.getName() : generator.name(), field, generator.hasDefault(), false);
    }

    private final void addProgramGenerator(GenerateProgramPort generator, Field field) {
        String name = generator.name();
        addProgramPort(name, generator.variableName().isEmpty() ? name : generator.variableName(), field, generator.type(), generator.hasDefault());
    }

    private final void setInitialInputValues(KeyValueMap values) {
        for (Entry<String, Object> entry : values.entrySet()) {
            setInputValue((String) entry.getKey(), entry.getValue());
        }
    }

    private final void setImmediateInputValue(String name, Object value) {
        if (this.mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Setting immediate value ");
            stringBuilder.append(value);
            stringBuilder.append(" for port ");
            stringBuilder.append(name);
            stringBuilder.append("!");
            Log.v(str, stringBuilder.toString());
        }
        FilterPort port = getInputPort(name);
        port.open();
        port.setFrame(SimpleFrame.wrapObject(value, null));
    }

    private final void transferInputFrames(FilterContext context) {
        for (InputPort inputPort : this.mInputPorts.values()) {
            inputPort.transfer(context);
        }
    }

    private final Frame wrapInputValue(String inputName, Object value) {
        Frame serializedFrame;
        boolean shouldSerialize = true;
        MutableFrameFormat inputFormat = ObjectFormat.fromObject(value, 1);
        if (value == null) {
            FrameFormat portFormat = getInputPort(inputName).getPortFormat();
            inputFormat.setObjectClass(portFormat == null ? null : portFormat.getObjectClass());
        }
        if ((value instanceof Number) || (value instanceof Boolean) || (value instanceof String) || !(value instanceof Serializable)) {
            shouldSerialize = false;
        }
        if (shouldSerialize) {
            serializedFrame = new SerializedFrame(inputFormat, null);
        } else {
            serializedFrame = new SimpleFrame(inputFormat, null);
        }
        Frame frame = serializedFrame;
        frame.setObjectValue(value);
        return frame;
    }

    private final void releasePulledFrames(FilterContext context) {
        Iterator it = this.mFramesToRelease.iterator();
        while (it.hasNext()) {
            context.getFrameManager().releaseFrame((Frame) it.next());
        }
        this.mFramesToRelease.clear();
    }

    private final boolean inputConditionsMet() {
        for (FilterPort port : this.mInputPorts.values()) {
            if (!port.isReady()) {
                if (this.mLogVerbose) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Input condition not met: ");
                    stringBuilder.append(port);
                    stringBuilder.append("!");
                    Log.v(str, stringBuilder.toString());
                }
                return false;
            }
        }
        return true;
    }

    private final boolean outputConditionsMet() {
        for (FilterPort port : this.mOutputPorts.values()) {
            if (!port.isReady()) {
                if (this.mLogVerbose) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Output condition not met: ");
                    stringBuilder.append(port);
                    stringBuilder.append("!");
                    Log.v(str, stringBuilder.toString());
                }
                return false;
            }
        }
        return true;
    }

    private final void closePorts() {
        if (this.mLogVerbose) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Closing all ports on ");
            stringBuilder.append(this);
            stringBuilder.append("!");
            Log.v(str, stringBuilder.toString());
        }
        for (InputPort inputPort : this.mInputPorts.values()) {
            inputPort.close();
        }
        for (OutputPort outputPort : this.mOutputPorts.values()) {
            outputPort.close();
        }
    }

    private final boolean filterMustClose() {
        String str;
        StringBuilder stringBuilder;
        for (InputPort inputPort : this.mInputPorts.values()) {
            if (inputPort.filterMustClose()) {
                if (this.mLogVerbose) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Filter ");
                    stringBuilder.append(this);
                    stringBuilder.append(" must close due to port ");
                    stringBuilder.append(inputPort);
                    Log.v(str, stringBuilder.toString());
                }
                return true;
            }
        }
        for (OutputPort outputPort : this.mOutputPorts.values()) {
            if (outputPort.filterMustClose()) {
                if (this.mLogVerbose) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Filter ");
                    stringBuilder.append(this);
                    stringBuilder.append(" must close due to port ");
                    stringBuilder.append(outputPort);
                    Log.v(str, stringBuilder.toString());
                }
                return true;
            }
        }
        return false;
    }
}
