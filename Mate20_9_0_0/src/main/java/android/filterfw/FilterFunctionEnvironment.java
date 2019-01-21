package android.filterfw;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterFactory;
import android.filterfw.core.FilterFunction;
import android.filterfw.core.FrameManager;

public class FilterFunctionEnvironment extends MffEnvironment {
    public FilterFunctionEnvironment() {
        super(null);
    }

    public FilterFunctionEnvironment(FrameManager frameManager) {
        super(frameManager);
    }

    public FilterFunction createFunction(Class filterClass, Object... parameters) {
        String filterName = new StringBuilder();
        filterName.append("FilterFunction(");
        filterName.append(filterClass.getSimpleName());
        filterName.append(")");
        Filter filter = FilterFactory.sharedFactory().createFilterByClass(filterClass, filterName.toString());
        filter.initWithAssignmentList(parameters);
        return new FilterFunction(getContext(), filter);
    }
}
