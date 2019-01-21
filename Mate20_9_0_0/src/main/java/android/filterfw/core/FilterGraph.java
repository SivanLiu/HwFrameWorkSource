package android.filterfw.core;

import android.filterpacks.base.FrameBranch;
import android.filterpacks.base.NullFilter;
import android.util.Log;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

public class FilterGraph {
    public static final int AUTOBRANCH_OFF = 0;
    public static final int AUTOBRANCH_SYNCED = 1;
    public static final int AUTOBRANCH_UNSYNCED = 2;
    public static final int TYPECHECK_DYNAMIC = 1;
    public static final int TYPECHECK_OFF = 0;
    public static final int TYPECHECK_STRICT = 2;
    private String TAG = "FilterGraph";
    private int mAutoBranchMode = 0;
    private boolean mDiscardUnconnectedOutputs = false;
    private HashSet<Filter> mFilters = new HashSet();
    private boolean mIsReady = false;
    private boolean mLogVerbose = Log.isLoggable(this.TAG, 2);
    private HashMap<String, Filter> mNameMap = new HashMap();
    private HashMap<OutputPort, LinkedList<InputPort>> mPreconnections = new HashMap();
    private int mTypeCheckMode = 2;

    public boolean addFilter(Filter filter) {
        if (containsFilter(filter)) {
            return false;
        }
        this.mFilters.add(filter);
        this.mNameMap.put(filter.getName(), filter);
        return true;
    }

    public boolean containsFilter(Filter filter) {
        return this.mFilters.contains(filter);
    }

    public Filter getFilter(String name) {
        return (Filter) this.mNameMap.get(name);
    }

    public void connect(Filter source, String outputName, Filter target, String inputName) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Passing null Filter in connect()!");
        } else if (containsFilter(source) && containsFilter(target)) {
            OutputPort outPort = source.getOutputPort(outputName);
            InputPort inPort = target.getInputPort(inputName);
            StringBuilder stringBuilder;
            if (outPort == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown output port '");
                stringBuilder.append(outputName);
                stringBuilder.append("' on Filter ");
                stringBuilder.append(source);
                stringBuilder.append("!");
                throw new RuntimeException(stringBuilder.toString());
            } else if (inPort != null) {
                preconnect(outPort, inPort);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown input port '");
                stringBuilder.append(inputName);
                stringBuilder.append("' on Filter ");
                stringBuilder.append(target);
                stringBuilder.append("!");
                throw new RuntimeException(stringBuilder.toString());
            }
        } else {
            throw new RuntimeException("Attempting to connect filter not in graph!");
        }
    }

    public void connect(String sourceName, String outputName, String targetName, String inputName) {
        Filter source = getFilter(sourceName);
        Filter target = getFilter(targetName);
        StringBuilder stringBuilder;
        if (source == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Attempting to connect unknown source filter '");
            stringBuilder.append(sourceName);
            stringBuilder.append("'!");
            throw new RuntimeException(stringBuilder.toString());
        } else if (target != null) {
            connect(source, outputName, target, inputName);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Attempting to connect unknown target filter '");
            stringBuilder.append(targetName);
            stringBuilder.append("'!");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public Set<Filter> getFilters() {
        return this.mFilters;
    }

    public void beginProcessing() {
        if (this.mLogVerbose) {
            Log.v(this.TAG, "Opening all filter connections...");
        }
        Iterator it = this.mFilters.iterator();
        while (it.hasNext()) {
            ((Filter) it.next()).openOutputs();
        }
        this.mIsReady = true;
    }

    public void flushFrames() {
        Iterator it = this.mFilters.iterator();
        while (it.hasNext()) {
            ((Filter) it.next()).clearOutputs();
        }
    }

    public void closeFilters(FilterContext context) {
        if (this.mLogVerbose) {
            Log.v(this.TAG, "Closing all filters...");
        }
        Iterator it = this.mFilters.iterator();
        while (it.hasNext()) {
            ((Filter) it.next()).performClose(context);
        }
        this.mIsReady = false;
    }

    public boolean isReady() {
        return this.mIsReady;
    }

    public void setAutoBranchMode(int autoBranchMode) {
        this.mAutoBranchMode = autoBranchMode;
    }

    public void setDiscardUnconnectedOutputs(boolean discard) {
        this.mDiscardUnconnectedOutputs = discard;
    }

    public void setTypeCheckMode(int typeCheckMode) {
        this.mTypeCheckMode = typeCheckMode;
    }

    public void tearDown(FilterContext context) {
        if (!this.mFilters.isEmpty()) {
            flushFrames();
            Iterator it = this.mFilters.iterator();
            while (it.hasNext()) {
                ((Filter) it.next()).performTearDown(context);
            }
            this.mFilters.clear();
            this.mNameMap.clear();
            this.mIsReady = false;
        }
    }

    private boolean readyForProcessing(Filter filter, Set<Filter> processed) {
        if (processed.contains(filter)) {
            return false;
        }
        for (InputPort port : filter.getInputPorts()) {
            Filter dependency = port.getSourceFilter();
            if (dependency != null && !processed.contains(dependency)) {
                return false;
            }
        }
        return true;
    }

    private void runTypeCheck() {
        Stack<Filter> filterStack = new Stack();
        Set<Filter> processedFilters = new HashSet();
        filterStack.addAll(getSourceFilters());
        while (!filterStack.empty()) {
            Filter filter = (Filter) filterStack.pop();
            processedFilters.add(filter);
            updateOutputs(filter);
            if (this.mLogVerbose) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Running type check on ");
                stringBuilder.append(filter);
                stringBuilder.append("...");
                Log.v(str, stringBuilder.toString());
            }
            runTypeCheckOn(filter);
            for (OutputPort port : filter.getOutputPorts()) {
                Filter target = port.getTargetFilter();
                if (target != null && readyForProcessing(target, processedFilters)) {
                    filterStack.push(target);
                }
            }
        }
        if (processedFilters.size() != getFilters().size()) {
            throw new RuntimeException("Could not schedule all filters! Is your graph malformed?");
        }
    }

    private void updateOutputs(Filter filter) {
        for (OutputPort outputPort : filter.getOutputPorts()) {
            InputPort inputPort = outputPort.getBasePort();
            if (inputPort != null) {
                FrameFormat outputFormat = filter.getOutputFormat(outputPort.getName(), inputPort.getSourceFormat());
                if (outputFormat != null) {
                    outputPort.setPortFormat(outputFormat);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Filter did not return an output format for ");
                    stringBuilder.append(outputPort);
                    stringBuilder.append("!");
                    throw new RuntimeException(stringBuilder.toString());
                }
            }
        }
    }

    private void runTypeCheckOn(Filter filter) {
        for (InputPort inputPort : filter.getInputPorts()) {
            if (this.mLogVerbose) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Type checking port ");
                stringBuilder.append(inputPort);
                Log.v(str, stringBuilder.toString());
            }
            FrameFormat sourceFormat = inputPort.getSourceFormat();
            FrameFormat targetFormat = inputPort.getPortFormat();
            if (!(sourceFormat == null || targetFormat == null)) {
                StringBuilder stringBuilder2;
                if (this.mLogVerbose) {
                    String str2 = this.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Checking ");
                    stringBuilder2.append(sourceFormat);
                    stringBuilder2.append(" against ");
                    stringBuilder2.append(targetFormat);
                    stringBuilder2.append(".");
                    Log.v(str2, stringBuilder2.toString());
                }
                boolean compatible = true;
                switch (this.mTypeCheckMode) {
                    case 0:
                        inputPort.setChecksType(false);
                        break;
                    case 1:
                        compatible = sourceFormat.mayBeCompatibleWith(targetFormat);
                        inputPort.setChecksType(true);
                        break;
                    case 2:
                        compatible = sourceFormat.isCompatibleWith(targetFormat);
                        inputPort.setChecksType(false);
                        break;
                }
                if (!compatible) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Type mismatch: Filter ");
                    stringBuilder2.append(filter);
                    stringBuilder2.append(" expects a format of type ");
                    stringBuilder2.append(targetFormat);
                    stringBuilder2.append(" but got a format of type ");
                    stringBuilder2.append(sourceFormat);
                    stringBuilder2.append("!");
                    throw new RuntimeException(stringBuilder2.toString());
                }
            }
        }
    }

    private void checkConnections() {
    }

    private void discardUnconnectedOutputs() {
        LinkedList<Filter> addedFilters = new LinkedList();
        Iterator it = this.mFilters.iterator();
        while (it.hasNext()) {
            Filter filter = (Filter) it.next();
            int id = 0;
            for (OutputPort port : filter.getOutputPorts()) {
                if (!port.isConnected()) {
                    StringBuilder stringBuilder;
                    if (this.mLogVerbose) {
                        String str = this.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Autoconnecting unconnected ");
                        stringBuilder.append(port);
                        stringBuilder.append(" to Null filter.");
                        Log.v(str, stringBuilder.toString());
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(filter.getName());
                    stringBuilder.append("ToNull");
                    stringBuilder.append(id);
                    NullFilter nullFilter = new NullFilter(stringBuilder.toString());
                    nullFilter.init();
                    addedFilters.add(nullFilter);
                    port.connectTo(nullFilter.getInputPort("frame"));
                    id++;
                }
            }
        }
        it = addedFilters.iterator();
        while (it.hasNext()) {
            addFilter((Filter) it.next());
        }
    }

    private void removeFilter(Filter filter) {
        this.mFilters.remove(filter);
        this.mNameMap.remove(filter.getName());
    }

    private void preconnect(OutputPort outPort, InputPort inPort) {
        LinkedList<InputPort> targets = (LinkedList) this.mPreconnections.get(outPort);
        if (targets == null) {
            targets = new LinkedList();
            this.mPreconnections.put(outPort, targets);
        }
        targets.add(inPort);
    }

    private void connectPorts() {
        int branchId = 1;
        for (Entry<OutputPort, LinkedList<InputPort>> connection : this.mPreconnections.entrySet()) {
            OutputPort outputPort = (OutputPort) connection.getKey();
            LinkedList<InputPort> inputPorts = (LinkedList) connection.getValue();
            if (inputPorts.size() == 1) {
                outputPort.connectTo((InputPort) inputPorts.get(0));
            } else if (this.mAutoBranchMode != 0) {
                if (this.mLogVerbose) {
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Creating branch for ");
                    stringBuilder.append(outputPort);
                    stringBuilder.append("!");
                    Log.v(str, stringBuilder.toString());
                }
                if (this.mAutoBranchMode == 1) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("branch");
                    int branchId2 = branchId + 1;
                    stringBuilder2.append(branchId);
                    FrameBranch branch = new FrameBranch(stringBuilder2.toString());
                    FrameBranch branch2 = new KeyValueMap();
                    branch.initWithAssignmentList("outputs", Integer.valueOf(inputPorts.size()));
                    addFilter(branch);
                    outputPort.connectTo(branch.getInputPort("in"));
                    Iterator<InputPort> inputPortIter = inputPorts.iterator();
                    for (OutputPort branchOutPort : branch.getOutputPorts()) {
                        branchOutPort.connectTo((InputPort) inputPortIter.next());
                    }
                    branchId = branchId2;
                } else {
                    throw new RuntimeException("TODO: Unsynced branches not implemented yet!");
                }
            } else {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Attempting to connect ");
                stringBuilder3.append(outputPort);
                stringBuilder3.append(" to multiple filter ports! Enable auto-branching to allow this.");
                throw new RuntimeException(stringBuilder3.toString());
            }
        }
        this.mPreconnections.clear();
    }

    private HashSet<Filter> getSourceFilters() {
        HashSet<Filter> sourceFilters = new HashSet();
        for (Filter filter : getFilters()) {
            if (filter.getNumberOfConnectedInputs() == 0) {
                if (this.mLogVerbose) {
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Found source filter: ");
                    stringBuilder.append(filter);
                    Log.v(str, stringBuilder.toString());
                }
                sourceFilters.add(filter);
            }
        }
        return sourceFilters;
    }

    void setupFilters() {
        if (this.mDiscardUnconnectedOutputs) {
            discardUnconnectedOutputs();
        }
        connectPorts();
        checkConnections();
        runTypeCheck();
    }
}
