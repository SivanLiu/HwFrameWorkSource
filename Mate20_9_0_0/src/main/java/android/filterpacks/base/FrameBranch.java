package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFinalPort;

public class FrameBranch extends Filter {
    @GenerateFinalPort(hasDefault = true, name = "outputs")
    private int mNumberOfOutputs = 2;

    public FrameBranch(String name) {
        super(name);
    }

    public void setupPorts() {
        addInputPort("in");
        for (int i = 0; i < this.mNumberOfOutputs; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("out");
            stringBuilder.append(i);
            addOutputBasedOnInput(stringBuilder.toString(), "in");
        }
    }

    public FrameFormat getOutputFormat(String portName, FrameFormat inputFormat) {
        return inputFormat;
    }

    public void process(FilterContext context) {
        Frame input = pullInput("in");
        for (int i = 0; i < this.mNumberOfOutputs; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("out");
            stringBuilder.append(i);
            pushOutput(stringBuilder.toString(), input);
        }
    }
}
