package com.huawei.odmf.utils;

import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFRuntimeException;
import com.huawei.odmf.model.api.Attribute;

public class CursorUtils {
    public static Object extractAggregateResult(String stringValue, int aggregateOp, Attribute attribute) {
        if (aggregateOp == 2) {
            return Long.valueOf(stringValue);
        }
        int type;
        if (aggregateOp == 3) {
            if (attribute == null) {
                LOG.logE("Execute FetchRequestWithAggregateFunction failed : the column name is wrong.");
                throw new ODMFRuntimeException("Execute FetchRequestWithAggregateFunction failed : the column name is wrong.");
            }
            type = attribute.getType();
            if (type != 11 && type != 21 && type != 14 && type != 22 && type != 6 && type != 7 && type != 3 && type != 20) {
                return Double.valueOf(stringValue);
            }
            LOG.logE("Execute FetchRequestWithAggregateFunction failed : The aggregate function AVG is not support for the data type byte, char, boolean, blob and clob.");
            throw new ODMFIllegalArgumentException("Execute FetchRequestWithAggregateFunction failed : The aggregate function AVG is not support for the data type byte, char, boolean, blob and clob.");
        } else if (attribute == null) {
            LOG.logE("Execute FetchRequestWithAggregateFunction failed : the column name is wrong.");
            throw new ODMFRuntimeException("Execute FetchRequestWithAggregateFunction failed : the column name is wrong");
        } else {
            type = attribute.getType();
            if (type == 9 || type == 12 || type == 10 || type == 13) {
                return Long.valueOf(stringValue);
            }
            if (type == 4 || type == 5 || type == 20 || type == 18) {
                return Double.valueOf(stringValue);
            }
            if (type == 0 || type == 1 || type == 8 || type == 15 || type == 16 || type == 17) {
                return Long.valueOf(stringValue);
            }
            if (type == 2) {
                return stringValue;
            }
            LOG.logE("Execute FetchRequestWithAggregateFunction failed : The aggregate function MAX,MIN,SUM is not support for the data type byte, char, boolean, blob and clob.");
            throw new ODMFIllegalArgumentException("The aggregate function MAX,MIN,SUM is not support for the data type byte, char, boolean, blob and clob.");
        }
    }
}
