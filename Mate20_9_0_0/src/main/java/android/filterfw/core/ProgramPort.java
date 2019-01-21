package android.filterfw.core;

import java.lang.reflect.Field;

public class ProgramPort extends FieldPort {
    protected String mVarName;

    public ProgramPort(Filter filter, String name, String varName, Field field, boolean hasDefault) {
        super(filter, name, field, hasDefault);
        this.mVarName = varName;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Program ");
        stringBuilder.append(super.toString());
        return stringBuilder.toString();
    }

    public synchronized void transfer(FilterContext context) {
        StringBuilder stringBuilder;
        if (this.mValueWaiting) {
            try {
                Program fieldValue = this.mField.get(this.mFilter);
                if (fieldValue != null) {
                    fieldValue.setHostValue(this.mVarName, this.mValue);
                    this.mValueWaiting = false;
                }
            } catch (IllegalAccessException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Access to program field '");
                stringBuilder.append(this.mField.getName());
                stringBuilder.append("' was denied!");
                throw new RuntimeException(stringBuilder.toString());
            } catch (ClassCastException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Non Program field '");
                stringBuilder.append(this.mField.getName());
                stringBuilder.append("' annotated with ProgramParameter!");
                throw new RuntimeException(stringBuilder.toString());
            }
        }
    }
}
