package com.huawei.odmf.user;

import android.text.TextUtils;
import com.huawei.odmf.core.AObjectContext;
import com.huawei.odmf.core.ManagedObject;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFIllegalPredicateException;
import com.huawei.odmf.exception.ODMFRuntimeException;
import com.huawei.odmf.predicate.FetchRequest;
import com.huawei.odmf.user.api.ObjectContext;
import com.huawei.odmf.user.api.Query;
import java.lang.reflect.Field;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class AQueryImpl<T extends ManagedObject> implements Query<T> {
    public static final int AVG = 3;
    public static final int COUNT = 2;
    private static final String DOT = ".";
    public static final int MAX = 0;
    public static final int MIN = 1;
    public static final int SUM = 4;
    private List<Integer> aggregateOps = new ArrayList();
    private List<String> columns = new ArrayList();
    private List<String> columnsWithAggregateFunction = new ArrayList();
    private String entityName = null;
    private FetchRequest<T> fetchRequest = null;
    private boolean isSorted = false;
    private boolean needOpBeforeThis = false;
    private AObjectContext objectContext = null;
    private List<String> sqlArgs = new ArrayList();
    private String tableName = null;

    public AQueryImpl(String entityName2, FetchRequest<T> fetchRequest2, ObjectContext objectContext2) {
        this.entityName = entityName2;
        this.fetchRequest = fetchRequest2;
        this.objectContext = (AObjectContext) objectContext2;
        this.needOpBeforeThis = false;
        this.tableName = entityName2.substring(entityName2.lastIndexOf(DOT) + 1);
    }

    public Iterator iterator() {
        return findAllLazyList().iterator();
    }

    @Override // com.huawei.odmf.user.api.Query
    public List<Object> queryWithAggregateFunction() {
        this.fetchRequest.setSelectionArgs((String[]) this.sqlArgs.toArray(new String[0]));
        this.fetchRequest.setColumnsWithAggregateFunction((String[]) this.columnsWithAggregateFunction.toArray(new String[0]));
        this.fetchRequest.setColumns((String[]) this.columns.toArray(new String[0]));
        Integer[] integers = (Integer[]) this.aggregateOps.toArray(new Integer[0]);
        int[] ints = new int[integers.length];
        for (int i = 0; i < integers.length; i++) {
            ints[i] = integers[i].intValue();
        }
        this.fetchRequest.setAggregateOp(ints);
        return this.objectContext.executeFetchRequestWithAggregateFunction(this.fetchRequest);
    }

    @Override // com.huawei.odmf.user.api.Query
    public List<T> findAll() {
        this.fetchRequest.setSelectionArgs((String[]) this.sqlArgs.toArray(new String[0]));
        return this.objectContext.executeFetchRequest(this.fetchRequest);
    }

    @Override // com.huawei.odmf.user.api.Query
    public List<T> findAllLazyList() {
        this.fetchRequest.setSelectionArgs((String[]) this.sqlArgs.toArray(new String[0]));
        return this.objectContext.executeFetchRequestLazyList(this.fetchRequest);
    }

    @Override // com.huawei.odmf.user.api.Query
    public ListIterator<T> listIterator() {
        this.fetchRequest.setSelectionArgs((String[]) this.sqlArgs.toArray(new String[0]));
        return this.objectContext.executeFetchRequestLazyList(this.fetchRequest).listIterator();
    }

    private void checkParameter(String methodName, String field, String... args) {
        if (field == null) {
            throw new ODMFIllegalArgumentException("QueryImpl." + methodName + "(): field is null.");
        } else if (field.equals("")) {
            throw new ODMFIllegalArgumentException("QueryImpl." + methodName + "(): string 'field' is empty.");
        } else if (args != null) {
            int length = args.length;
            for (int i = 0; i < length; i++) {
                if (args[i] == null) {
                    throw new ODMFIllegalArgumentException("QueryImpl." + methodName + "(): value" + i + " is null.");
                }
            }
        }
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, Byte value) {
        return equalTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, Short value) {
        return equalTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, Integer value) {
        return equalTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, Long value) {
        return equalTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, Double value) {
        return equalTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, Float value) {
        return equalTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, Boolean value) {
        return equalTo(field, String.valueOf(value.booleanValue() ? 1 : 0));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, Date value) {
        return equalTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, Time value) {
        return equalTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, Timestamp value) {
        return equalTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, Calendar value) {
        return equalTo(field, String.valueOf(value.getTimeInMillis()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> equalTo(String field, String value) {
        checkParameter("equalTo", field, value);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append("AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" = ? ");
        this.sqlArgs.add(value);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, Byte value) {
        return notEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, Short value) {
        return notEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, Integer value) {
        return notEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, Long value) {
        return notEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, Double value) {
        return notEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, Float value) {
        return notEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, Boolean value) {
        return notEqualTo(field, String.valueOf(value.booleanValue() ? 1 : 0));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, Date value) {
        return notEqualTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, Time value) {
        return notEqualTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, Timestamp value) {
        return notEqualTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, Calendar value) {
        return notEqualTo(field, String.valueOf(value.getTimeInMillis()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> notEqualTo(String field, String value) {
        checkParameter("notEqualTo", field, value);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append("AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" <> ? ");
        this.sqlArgs.add(value);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> beginGroup() {
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append("AND ");
            this.needOpBeforeThis = false;
        }
        this.fetchRequest.getSqlRequest().append(" ( ");
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> endGroup() {
        if (!this.needOpBeforeThis) {
            throw new ODMFIllegalPredicateException("QueryImpl.endGroup(): you cannot use function or() before end parenthesis, start a query with endGroup(), or use endGroup() right after beginGroup().");
        }
        this.fetchRequest.getSqlRequest().append(" ) ");
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> or() {
        if (!this.needOpBeforeThis) {
            throw new ODMFIllegalPredicateException("QueryImpl.or(): you are starting a sql request with predicate \"or\" or using function or() immediately after another or(). that is ridiculous.");
        }
        this.fetchRequest.getSqlRequest().append(" OR ");
        this.needOpBeforeThis = false;
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> and() {
        if (this.needOpBeforeThis) {
            return this;
        }
        throw new ODMFIllegalPredicateException("QueryImpl.and(): you should not start a request with \"and\" or use or() before this function.");
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> contains(String field, String value) {
        checkParameter("contains", field, value);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append(" AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" LIKE ? ");
        this.sqlArgs.add("%" + value + "%");
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> beginsWith(String field, String value) {
        checkParameter("beginsWith", field, value);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append(" AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" LIKE ? ");
        this.sqlArgs.add(value + "%");
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> endsWith(String field, String value) {
        checkParameter("endsWith", field, value);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append(" AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" LIKE ? ");
        this.sqlArgs.add("%" + value);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> isNull(String field) {
        checkParameter("isNull", field, new String[0]);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append(" AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" is null ");
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> isNotNull(String field) {
        checkParameter("isNotNull", field, new String[0]);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append(" AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" is not null ");
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> like(String field, String value) {
        checkParameter("like", field, value);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append(" AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" LIKE ? ");
        this.sqlArgs.add(value);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> between(String field, Integer low, Integer high) {
        return between(field, String.valueOf(low), String.valueOf(high));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> between(String field, Long low, Long high) {
        return between(field, String.valueOf(low), String.valueOf(high));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> between(String field, Double low, Double high) {
        return between(field, String.valueOf(low), String.valueOf(high));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> between(String field, Float low, Float high) {
        return between(field, String.valueOf(low), String.valueOf(high));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> between(String field, Date low, Date high) {
        return between(field, String.valueOf(low.getTime()), String.valueOf(high.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> between(String field, Time low, Time high) {
        return between(field, String.valueOf(low.getTime()), String.valueOf(high.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> between(String field, Timestamp low, Timestamp high) {
        return between(field, String.valueOf(low.getTime()), String.valueOf(high.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> between(String field, Calendar low, Calendar high) {
        return between(field, String.valueOf(low.getTimeInMillis()), String.valueOf(high.getTimeInMillis()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> between(String field, String low, String high) {
        checkParameter("between", field, low, high);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append(" AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" BETWEEN ? AND ? ");
        this.sqlArgs.add(low);
        this.sqlArgs.add(high);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThan(String field, Integer value) {
        return greaterThan(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThan(String field, Long value) {
        return greaterThan(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThan(String field, Double value) {
        return greaterThan(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThan(String field, Float value) {
        return greaterThan(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThan(String field, Date value) {
        return greaterThan(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThan(String field, Time value) {
        return greaterThan(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThan(String field, Timestamp value) {
        return greaterThan(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThan(String field, Calendar value) {
        return greaterThan(field, String.valueOf(value.getTimeInMillis()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThan(String field, String value) {
        checkParameter("greaterThan", field, value);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append("AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" > ? ");
        this.sqlArgs.add(value);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThan(String field, Integer value) {
        return lessThan(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThan(String field, Long value) {
        return lessThan(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThan(String field, Double value) {
        return lessThan(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThan(String field, Float value) {
        return lessThan(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThan(String field, Date value) {
        return lessThan(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThan(String field, Time value) {
        return lessThan(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThan(String field, Timestamp value) {
        return lessThan(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThan(String field, Calendar value) {
        return lessThan(field, String.valueOf(value.getTimeInMillis()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThan(String field, String value) {
        checkParameter("lessThan", field, value);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append("AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" < ? ");
        this.sqlArgs.add(value);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThanOrEqualTo(String field, Integer value) {
        return greaterThanOrEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThanOrEqualTo(String field, Long value) {
        return greaterThanOrEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThanOrEqualTo(String field, Double value) {
        return greaterThanOrEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThanOrEqualTo(String field, Float value) {
        return greaterThanOrEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThanOrEqualTo(String field, Date value) {
        return greaterThanOrEqualTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThanOrEqualTo(String field, Time value) {
        return greaterThanOrEqualTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThanOrEqualTo(String field, Timestamp value) {
        return greaterThanOrEqualTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThanOrEqualTo(String field, Calendar value) {
        return greaterThanOrEqualTo(field, String.valueOf(value.getTimeInMillis()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> greaterThanOrEqualTo(String field, String value) {
        checkParameter("greaterThanOrEqualTo", field, value);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append("AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" >= ? ");
        this.sqlArgs.add(value);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThanOrEqualTo(String field, Integer value) {
        return lessThanOrEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThanOrEqualTo(String field, Long value) {
        return lessThanOrEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThanOrEqualTo(String field, Double value) {
        return lessThanOrEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThanOrEqualTo(String field, Float value) {
        return lessThanOrEqualTo(field, String.valueOf(value));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThanOrEqualTo(String field, Date value) {
        return lessThanOrEqualTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThanOrEqualTo(String field, Time value) {
        return lessThanOrEqualTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThanOrEqualTo(String field, Timestamp value) {
        return lessThanOrEqualTo(field, String.valueOf(value.getTime()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThanOrEqualTo(String field, Calendar value) {
        return lessThanOrEqualTo(field, String.valueOf(value.getTimeInMillis()));
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> lessThanOrEqualTo(String field, String value) {
        checkParameter("lessThanOrEqualTo", field, value);
        if (this.needOpBeforeThis) {
            this.fetchRequest.getSqlRequest().append("AND ");
        } else {
            this.needOpBeforeThis = true;
        }
        this.fetchRequest.getSqlRequest().append(processFieldAndJoinClause(removeQuotes(field))).append(" <= ? ");
        this.sqlArgs.add(value);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> orderByAsc(String field) {
        checkParameter("orderByAsc", field, new String[0]);
        if (this.isSorted) {
            this.fetchRequest.getOrder().append(',');
        }
        this.fetchRequest.getOrder().append(processFieldAndJoinClause(removeQuotes(field))).append(" ASC ");
        this.isSorted = true;
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> orderByDesc(String field) {
        checkParameter("orderByDesc", field, new String[0]);
        if (this.isSorted) {
            this.fetchRequest.getOrder().append(',');
        }
        this.fetchRequest.getOrder().append(processFieldAndJoinClause(removeQuotes(field))).append(" DESC ");
        this.isSorted = true;
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> limit(int value) {
        if (!this.fetchRequest.getLimit().equals("")) {
            throw new ODMFIllegalPredicateException("QueryImpl.limit(): limit cannot be set twice.");
        } else if (value < 1) {
            throw new ODMFIllegalArgumentException("QueryImpl.limit(): limit cannot be less than zero.");
        } else {
            this.fetchRequest.setLimit(Integer.toString(value));
            return this;
        }
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> max(String field) {
        String fieldWithBrackets;
        checkParameter("max", field, new String[0]);
        String normalizedField = removeQuotes(field);
        if (normalizedField.equals("*")) {
            fieldWithBrackets = normalizedField;
        } else {
            fieldWithBrackets = appendTableName(surroundWithQuote(normalizedField));
        }
        this.columnsWithAggregateFunction.add("MAX(" + fieldWithBrackets + ")");
        this.columns.add(normalizedField);
        this.aggregateOps.add(0);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> min(String field) {
        String fieldWithBrackets;
        checkParameter("min", field, new String[0]);
        String normalizedField = removeQuotes(field);
        if (normalizedField.equals("*")) {
            fieldWithBrackets = normalizedField;
        } else {
            fieldWithBrackets = appendTableName(surroundWithQuote(normalizedField));
        }
        this.columnsWithAggregateFunction.add("MIN(" + fieldWithBrackets + ")");
        this.columns.add(normalizedField);
        this.aggregateOps.add(1);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> count(String field) {
        String fieldWithBrackets;
        checkParameter("count", field, new String[0]);
        String normalizedField = removeQuotes(field);
        if (normalizedField.equals("*")) {
            fieldWithBrackets = normalizedField;
        } else {
            fieldWithBrackets = appendTableName(surroundWithQuote(normalizedField));
        }
        this.columnsWithAggregateFunction.add("COUNT(" + fieldWithBrackets + ")");
        this.columns.add(normalizedField);
        this.aggregateOps.add(2);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> avg(String field) {
        String fieldWithBrackets;
        checkParameter("avg", field, new String[0]);
        String normalizedField = removeQuotes(field);
        if (normalizedField.equals("*")) {
            fieldWithBrackets = normalizedField;
        } else {
            fieldWithBrackets = appendTableName(surroundWithQuote(normalizedField));
        }
        this.columnsWithAggregateFunction.add("AVG(" + fieldWithBrackets + ")");
        this.columns.add(normalizedField);
        this.aggregateOps.add(3);
        return this;
    }

    @Override // com.huawei.odmf.user.api.Query
    public Query<T> sum(String field) {
        String fieldWithBrackets;
        checkParameter("sum", field, new String[0]);
        String normalizedField = removeQuotes(field);
        if (normalizedField.equals("*")) {
            fieldWithBrackets = normalizedField;
        } else {
            fieldWithBrackets = appendTableName(surroundWithQuote(normalizedField));
        }
        this.columnsWithAggregateFunction.add("SUM(" + fieldWithBrackets + ")");
        this.columns.add(normalizedField);
        this.aggregateOps.add(4);
        return this;
    }

    public FetchRequest<T> getFetchRequest() {
        this.fetchRequest.setSelectionArgs((String[]) this.sqlArgs.toArray(new String[0]));
        if (this.aggregateOps.size() > 0) {
            this.fetchRequest.setColumnsWithAggregateFunction((String[]) this.columnsWithAggregateFunction.toArray(new String[0]));
            this.fetchRequest.setColumns((String[]) this.columns.toArray(new String[0]));
            Integer[] integers = (Integer[]) this.aggregateOps.toArray(new Integer[0]);
            int[] ints = new int[integers.length];
            for (int i = 0; i < integers.length; i++) {
                ints[i] = integers[i].intValue();
            }
            this.fetchRequest.setAggregateOp(ints);
        }
        return this.fetchRequest;
    }

    private String processFieldAndJoinClause(String fieldName) {
        if (!fieldName.contains(DOT)) {
            return this.tableName + DOT + surroundWithQuote(fieldName);
        }
        String[] split = fieldName.split("\\.");
        if (split.length != 2) {
            throw new ODMFIllegalArgumentException("Wrong field name.");
        }
        String beforeDot = split[0];
        String afterDot = surroundWithQuote(split[1]);
        try {
            Field classField = this.fetchRequest.getTheClass().getDeclaredField(beforeDot);
            if (classField.getType().toString().equals("interface java.util.List")) {
                String genericType = classField.getGenericType().toString();
                String joinedEntityName = genericType.substring(genericType.indexOf("<") + 1, genericType.indexOf(">"));
                String joinedTableName = joinedEntityName.substring(joinedEntityName.lastIndexOf(DOT) + 1);
                if (this.fetchRequest.getJoinedEntities().contains(joinedEntityName)) {
                    return joinedTableName + DOT + afterDot;
                }
                this.fetchRequest.getJoinClause().append(" INNER JOIN ").append(joinedTableName).append(" ON ").append(this.entityName).append("=").append(joinedEntityName);
                String mFieldName = joinedTableName + DOT + afterDot;
                this.fetchRequest.addToJoinedEntities(joinedEntityName);
                return mFieldName;
            }
            String joinedEntityName2 = classField.getType().getName();
            String joinedTableName2 = joinedEntityName2.substring(joinedEntityName2.lastIndexOf(DOT) + 1);
            if (this.fetchRequest.getJoinedEntities().contains(joinedEntityName2)) {
                return joinedTableName2 + DOT + afterDot;
            }
            this.fetchRequest.getJoinClause().append(" INNER JOIN ").append(joinedTableName2).append(" ON ").append(this.entityName).append("=").append(joinedEntityName2);
            String mFieldName2 = joinedTableName2 + DOT + afterDot;
            this.fetchRequest.addToJoinedEntities(joinedEntityName2);
            return mFieldName2;
        } catch (NoSuchFieldException e) {
            throw new ODMFRuntimeException("NoSuchFieldException");
        }
    }

    private static String removeQuotes(String s) {
        if (TextUtils.isEmpty(s)) {
            return "";
        }
        return s.replace("'", "").replace("\"", "");
    }

    private static String surroundWithQuote(String s) {
        if (TextUtils.isEmpty(s)) {
            return "";
        }
        return '\"' + s + '\"';
    }

    private String appendTableName(String s) {
        if (TextUtils.isEmpty(s)) {
            return "";
        }
        return surroundWithQuote(this.tableName) + DOT + s;
    }
}
