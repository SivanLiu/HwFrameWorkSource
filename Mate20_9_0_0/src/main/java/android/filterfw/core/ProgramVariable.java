package android.filterfw.core;

public class ProgramVariable {
    private Program mProgram;
    private String mVarName;

    public ProgramVariable(Program program, String varName) {
        this.mProgram = program;
        this.mVarName = varName;
    }

    public Program getProgram() {
        return this.mProgram;
    }

    public String getVariableName() {
        return this.mVarName;
    }

    public void setValue(Object value) {
        if (this.mProgram != null) {
            this.mProgram.setHostValue(this.mVarName, value);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attempting to set program variable '");
        stringBuilder.append(this.mVarName);
        stringBuilder.append("' but the program is null!");
        throw new RuntimeException(stringBuilder.toString());
    }

    public Object getValue() {
        if (this.mProgram != null) {
            return this.mProgram.getHostValue(this.mVarName);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attempting to get program variable '");
        stringBuilder.append(this.mVarName);
        stringBuilder.append("' but the program is null!");
        throw new RuntimeException(stringBuilder.toString());
    }
}
