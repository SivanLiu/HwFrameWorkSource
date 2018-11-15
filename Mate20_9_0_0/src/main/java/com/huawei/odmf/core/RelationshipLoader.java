package com.huawei.odmf.core;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import com.huawei.odmf.database.DataBase;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFIllegalStateException;
import com.huawei.odmf.exception.ODMFRelatedObjectNotFoundException;
import com.huawei.odmf.exception.ODMFRuntimeException;
import com.huawei.odmf.model.AEntityHelper;
import com.huawei.odmf.model.ARelationship;
import com.huawei.odmf.model.api.Entity;
import com.huawei.odmf.model.api.ObjectModel;
import com.huawei.odmf.model.api.Relationship;
import com.huawei.odmf.store.DatabaseTableHelper;
import com.huawei.odmf.utils.LOG;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class RelationshipLoader {
    private DataBase db;
    private Map<String, AEntityHelper> helperMap;
    private ObjectModel model;
    private List<LazyList<ManagedObject>> needClearLazyListModify = new ArrayList();
    private List<ODMFList<ManagedObject>> needClearODMFListModify = new ArrayList();

    RelationshipLoader(DataBase db, ObjectModel model, Map<String, AEntityHelper> helperMap) {
        this.db = db;
        this.model = model;
        this.helperMap = helperMap;
    }

    void handleRelationship(Collection<ManagedObject> manageObjects) {
        for (ManagedObject object : manageObjects) {
            AEntityHelper entityHelper = ((AManagedObject) object).getHelper();
            Entity entity = entityHelper.getEntity();
            if (entity.getRelationships() != null) {
                List<? extends Relationship> relationships = entity.getRelationships();
                int size = relationships.size();
                for (int i = 0; i < size; i++) {
                    if (object.isRelationshipUpdate(i)) {
                        Relationship relationship = (Relationship) relationships.get(i);
                        Object value = entityHelper.getRelationshipObject(relationship.getFieldName(), object);
                        Long rowId = object.getRowId();
                        if (relationship.getRelationShipType() == 0) {
                            handleManyToManyRelationship(relationship, object, value, rowId.longValue());
                        } else if (relationship.getRelationShipType() == 4) {
                            handleOneToManyRelationship(relationship, object, value, rowId.longValue());
                        } else if (relationship.getRelationShipType() == 2) {
                            handleManyToOneRelationship(relationship, value, rowId);
                        } else if (relationship.getRelationShipType() == 6) {
                            handleOneToOneRelationship(relationship, object, value, rowId);
                        }
                    }
                }
            }
            object.reSetRelationshipUpdateSigns();
        }
        if (this.needClearLazyListModify.size() > 0 || this.needClearODMFListModify.size() > 0) {
            clearAllModify();
        }
    }

    private void handleOneToOneRelationship(Relationship relationship, ManagedObject object, Object value, Long rowId) {
        if (value instanceof ManagedObject) {
            ManagedObject toOneObject = (ManagedObject) value;
            if (toOneObject.getRowId().longValue() < 0) {
                throw new ODMFRelatedObjectNotFoundException("The object in the relationship of the insert object are not yet persist in the database");
            }
            updateOneToOneValue(relationship, rowId.longValue(), toOneObject.getRowId().longValue());
        } else if (value instanceof Long) {
            Long toOneObject2 = (Long) value;
            if (toOneObject2.longValue() < 0) {
                throw new ODMFRelatedObjectNotFoundException("The object in the relationship of the insert object are not yet persist in the database");
            }
            updateOneToOneValue(relationship, rowId.longValue(), toOneObject2.longValue());
        }
        updateOneToOneRelationshipRemoveOldValue(relationship, object, value);
    }

    private void handleManyToOneRelationship(Relationship relationship, Object value, Long rowId) {
        if (value instanceof ManagedObject) {
            ManagedObject toOneObject = (ManagedObject) value;
            if (toOneObject.getRowId().longValue() < 0) {
                throw new ODMFRelatedObjectNotFoundException("The object in the relationship of the insert object are not yet persist in the database");
            }
            updateRelationValue(relationship.getBaseEntity().getTableName(), relationship.getFieldName(), rowId.longValue(), toOneObject.getRowId());
        } else if (value instanceof Long) {
            Long toOneObject2 = (Long) value;
            if (toOneObject2.longValue() < 0) {
                throw new ODMFRelatedObjectNotFoundException("The object in the relationship of the insert object are not yet persist in the database");
            }
            updateRelationValue(relationship.getBaseEntity().getTableName(), relationship.getFieldName(), rowId.longValue(), toOneObject2);
        } else {
            String tableName = relationship.getBaseEntity().getTableName();
            updateRelationValue(tableName, relationship.getFieldName(), rowId.longValue(), null);
        }
    }

    private void handleOneToManyRelationship(Relationship relationship, ManagedObject object, Object value, long rowId) {
        if (value != null) {
            handleOneToManyValue(relationship, object, value);
        } else {
            removeOneToManyValue(relationship, rowId);
        }
    }

    private void handleManyToManyRelationship(Relationship relationship, ManagedObject object, Object value, long rowId) {
        if (value != null) {
            handleManyToManyValue(relationship, object, value);
        } else {
            removeManyToManyValue(relationship, rowId);
        }
    }

    private void clearAllModify() {
        int i;
        int lazySize = this.needClearLazyListModify.size();
        for (i = 0; i < lazySize; i++) {
            ((LazyList) this.needClearLazyListModify.get(i)).clearModify();
        }
        int modifySize = this.needClearODMFListModify.size();
        for (i = 0; i < modifySize; i++) {
            ((ODMFList) this.needClearODMFListModify.get(i)).clearModify();
        }
        this.needClearLazyListModify.clear();
        this.needClearODMFListModify.clear();
    }

    private void updatingLazyListOneToManyValue(Relationship relationship, ManagedObject object, Object value) {
        LazyList<ManagedObject> lazyList = (LazyList) value;
        if (lazyList.getBaseObj() == object) {
            List<ManagedObject> remove = lazyList.getRemoveList();
            List<ManagedObject> insert = lazyList.getInsertList();
            if (remove == null || insert == null || (insert.size() == 0 && remove.size() == 0)) {
                handleOneToManyRelationshipUseJavaList(relationship, value, object.getRowId().longValue());
                return;
            }
            removeOneToManyValue(relationship, object.getRowId().longValue(), remove);
            addOneToManyValue(relationship, insert, object.getRowId().longValue());
            this.needClearLazyListModify.add(lazyList);
            return;
        }
        handleOneToManyRelationshipUseJavaList(relationship, value, object.getRowId().longValue());
    }

    private void updatingODMFListOneToManyValue(Relationship relationship, ManagedObject object, Object value) {
        ODMFList<ManagedObject> odmfList = (ODMFList) value;
        if (odmfList.getBaseObj() == object) {
            List<ManagedObject> remove = odmfList.getRemoveList();
            List<ManagedObject> insert = odmfList.getInsertList();
            if (remove == null || insert == null || (insert.size() == 0 && remove.size() == 0)) {
                handleOneToManyRelationshipUseJavaList(relationship, value, object.getRowId().longValue());
                return;
            }
            removeOneToManyValue(relationship, object.getRowId().longValue(), remove);
            addOneToManyValue(relationship, insert, object.getRowId().longValue());
            this.needClearODMFListModify.add(odmfList);
            return;
        }
        handleOneToManyRelationshipUseJavaList(relationship, value, object.getRowId().longValue());
    }

    private void handleOneToManyValue(Relationship relationship, ManagedObject object, Object value) {
        if (value instanceof LazyList) {
            updatingLazyListOneToManyValue(relationship, object, value);
        } else if (value instanceof ODMFList) {
            updatingODMFListOneToManyValue(relationship, object, value);
        } else {
            handleOneToManyRelationshipUseJavaList(relationship, value, object.getRowId().longValue());
        }
    }

    private void updatingLazyListManyToManyValue(Relationship relationship, ManagedObject object, Object value) {
        LazyList<ManagedObject> lazyList = (LazyList) value;
        if (lazyList.getBaseObj() == object) {
            List<ManagedObject> remove = lazyList.getRemoveList();
            List<ManagedObject> insert = lazyList.getInsertList();
            if (remove == null || insert == null || (insert.size() == 0 && remove.size() == 0)) {
                handleManyToManyRelationshipUseJavaList(relationship, value, object.getRowId().longValue());
                return;
            }
            removeManyToManyValue(relationship, object.getRowId().longValue(), remove);
            addManyToManyValue(relationship, insert, object.getRowId().longValue());
            this.needClearLazyListModify.add(lazyList);
            return;
        }
        handleManyToManyRelationshipUseJavaList(relationship, value, object.getRowId().longValue());
    }

    private void updatingODMFListManyToManyValue(Relationship relationship, ManagedObject object, Object value) {
        ODMFList<ManagedObject> odmfList = (ODMFList) value;
        if (odmfList.getBaseObj() == object) {
            List<ManagedObject> remove = odmfList.getRemoveList();
            List<ManagedObject> insert = odmfList.getInsertList();
            if (remove == null || insert == null || (insert.size() == 0 && remove.size() == 0)) {
                handleManyToManyRelationshipUseJavaList(relationship, value, object.getRowId().longValue());
                return;
            }
            removeManyToManyValue(relationship, object.getRowId().longValue(), remove);
            addManyToManyValue(relationship, insert, object.getRowId().longValue());
            this.needClearODMFListModify.add(odmfList);
            return;
        }
        handleManyToManyRelationshipUseJavaList(relationship, value, object.getRowId().longValue());
    }

    private void handleManyToManyValue(Relationship relationship, ManagedObject object, Object value) {
        if (value instanceof LazyList) {
            updatingLazyListManyToManyValue(relationship, object, value);
        } else if (value instanceof ODMFList) {
            updatingODMFListManyToManyValue(relationship, object, value);
        } else {
            handleManyToManyRelationshipUseJavaList(relationship, value, object.getRowId().longValue());
        }
    }

    void handleCascadeDelete(ManagedObject manageObject, List<ManagedObject> objects) {
        Entity entity = this.model.getEntity(manageObject.getEntityName());
        AEntityHelper helper = ((AManagedObject) manageObject).getHelper();
        if (entity.getRelationships() != null) {
            List<? extends Relationship> relationships = entity.getRelationships();
            int size = relationships.size();
            for (int i = 0; i < size; i++) {
                Relationship relationship = (Relationship) relationships.get(i);
                if (relationship.getCascade().equals(ARelationship.DELETE_CASCADE)) {
                    if (relationship.getRelationShipType() == 6) {
                        handleOneToOneCascade(helper, relationship, manageObject, objects);
                    } else if (relationship.getRelationShipType() == 2) {
                        handleManyToOneCascade(helper, relationship, manageObject, objects);
                    } else if (relationship.getRelationShipType() == 4) {
                        handleOneToManyCascade(helper, relationship, manageObject, objects);
                    } else {
                        handleManyToManyCascade(helper, relationship, manageObject, objects);
                    }
                }
                if (relationship.getRelationShipType() == 0) {
                    this.db.delete(DatabaseTableHelper.getManyToManyMidTableName(relationship), DatabaseTableHelper.getRelationshipColumnName(relationship.getBaseEntity()) + " = ?", new String[]{String.valueOf(manageObject.getRowId())});
                }
            }
        }
    }

    private void handleManyToManyCascade(AEntityHelper helper, Relationship relationship, ManagedObject manageObject, List<ManagedObject> objects) {
        try {
            List relatedObjects = (List) helper.getRelationshipObject(relationship.getFieldName(), manageObject);
            if (relatedObjects != null) {
                int size = relatedObjects.size();
                for (int j = 0; j < size; j++) {
                    Object obj = relatedObjects.get(j);
                    ManagedObject object = null;
                    if (obj instanceof ManagedObject) {
                        object = (ManagedObject) obj;
                    } else if (obj instanceof Long) {
                        object = getTheRelatedObj(relationship, (Long) obj);
                    }
                    if (!(object == null || !checkManyToManyCascadeDelete(relationship, manageObject, object) || isContainedObject(objects, object))) {
                        objects.add(object);
                        handleCascadeDelete(object, objects);
                    }
                }
            }
        } catch (ODMFRelatedObjectNotFoundException e) {
        }
    }

    private void handleOneToManyCascade(AEntityHelper helper, Relationship relationship, ManagedObject manageObject, List<ManagedObject> objects) {
        try {
            List relatedObjects = (List) helper.getRelationshipObject(relationship.getFieldName(), manageObject);
            if (relatedObjects != null) {
                int size = relatedObjects.size();
                for (int j = 0; j < size; j++) {
                    Object object = relatedObjects.get(j);
                    ManagedObject obj;
                    if (object instanceof ManagedObject) {
                        obj = (ManagedObject) object;
                        if (!isContainedObject(objects, obj)) {
                            objects.add(obj);
                            handleCascadeDelete(obj, objects);
                        }
                    } else if (object instanceof Long) {
                        obj = getTheRelatedObj(relationship, (Long) object);
                        if (!(obj == null || isContainedObject(objects, obj))) {
                            objects.add(obj);
                            handleCascadeDelete(obj, objects);
                        }
                    }
                }
            }
        } catch (ODMFRelatedObjectNotFoundException e) {
        }
    }

    private void handleManyToOneCascade(AEntityHelper helper, Relationship relationship, ManagedObject manageObject, List<ManagedObject> objects) {
        try {
            Object related = helper.getRelationshipObject(relationship.getFieldName(), manageObject);
            if (related instanceof ManagedObject) {
                ManagedObject relatedObj = (ManagedObject) related;
                if (checkManyToOneCascadeDelete(relationship, manageObject, relatedObj) && !isContainedObject(objects, relatedObj)) {
                    objects.add(relatedObj);
                    handleCascadeDelete(relatedObj, objects);
                }
            } else if (related instanceof Long) {
                Long relatedObj2 = (Long) related;
                if (checkManyToOneCascadeDelete(relationship, manageObject, relatedObj2)) {
                    ManagedObject relatedObject = getTheRelatedObj(relationship, relatedObj2);
                    if (relatedObject != null && !isContainedObject(objects, relatedObject)) {
                        objects.add(relatedObject);
                        handleCascadeDelete(relatedObject, objects);
                    }
                }
            }
        } catch (ODMFRelatedObjectNotFoundException e) {
        }
    }

    private void handleOneToOneCascade(AEntityHelper helper, Relationship relationship, ManagedObject manageObject, List<ManagedObject> objects) {
        try {
            Object relatedObj = helper.getRelationshipObject(relationship.getFieldName(), manageObject);
            ManagedObject relatedObject;
            if (relatedObj instanceof ManagedObject) {
                relatedObject = (ManagedObject) relatedObj;
                if (!isContainedObject(objects, relatedObject)) {
                    objects.add(relatedObject);
                    handleCascadeDelete(relatedObject, objects);
                }
            } else if (relatedObj instanceof Long) {
                Long relatedId = (Long) relatedObj;
                relatedObject = getTheRelatedObj(relationship, relatedId);
                if (relatedObject == null) {
                    LOG.logW("Cascade delete failed : the object not found which rowId is " + relatedId);
                } else if (!isContainedObject(objects, relatedObject)) {
                    objects.add(relatedObject);
                    handleCascadeDelete(relatedObject, objects);
                }
            }
        } catch (ODMFRelatedObjectNotFoundException e) {
        }
    }

    private ManagedObject getTheRelatedObj(Relationship relationship, Long relatedId) {
        Entity relatedEntity = relationship.getRelatedEntity();
        AEntityHelper entityHelper = getHelper(relatedEntity.getEntityName());
        ManagedObject managedObject = null;
        Cursor cursor = null;
        try {
            cursor = DatabaseQueryService.query(this.db, relatedEntity.getTableName(), new String[]{DatabaseQueryService.getRowidColumnName() + " AS " + DatabaseQueryService.getRowidColumnName(), "*"}, DatabaseQueryService.getRowidColumnName() + "=?", new String[]{String.valueOf(relatedId)});
            if (cursor.moveToNext()) {
                managedObject = (ManagedObject) entityHelper.readObject(cursor, 0);
                managedObject.setObjectId(new AObjectId(relatedEntity.getEntityName(), relatedId));
                managedObject.setRowId(relatedId);
            }
            if (cursor != null) {
                cursor.close();
            }
            return managedObject;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private AEntityHelper getHelper(String entityName) {
        return (AEntityHelper) this.helperMap.get(entityName);
    }

    private void updateOneToOneRelationshipRemoveOldValue(Relationship relationship, ManagedObject object, Object toOne) {
        String tableName;
        if (relationship.isMajor()) {
            tableName = relationship.getBaseEntity().getTableName();
        } else {
            tableName = relationship.getRelatedEntity().getTableName();
        }
        String fieldName = relationship.getForeignKeyName();
        String selection;
        String[] selectionArgs;
        ContentValues values;
        if (toOne instanceof ManagedObject) {
            ManagedObject toOneObject = (ManagedObject) toOne;
            selection = fieldName + " = ? And " + DatabaseQueryService.getRowidColumnName() + " <> ?";
            selectionArgs = relationship.isMajor() ? new String[]{String.valueOf(toOneObject.getRowId()), String.valueOf(object.getRowId())} : new String[]{String.valueOf(object.getRowId()), String.valueOf(toOneObject.getRowId())};
            values = new ContentValues();
            values.putNull(fieldName);
            this.db.update(tableName, values, selection, selectionArgs);
        } else if (toOne instanceof Long) {
            Long toOneObject2 = (Long) toOne;
            selection = fieldName + " = ? And " + DatabaseQueryService.getRowidColumnName() + " <> ?";
            selectionArgs = relationship.isMajor() ? new String[]{String.valueOf(toOneObject2), String.valueOf(object.getRowId())} : new String[]{String.valueOf(object.getRowId()), String.valueOf(toOneObject2)};
            values = new ContentValues();
            values.putNull(fieldName);
            this.db.update(tableName, values, selection, selectionArgs);
        } else {
            selection = relationship.isMajor() ? DatabaseQueryService.getRowidColumnName() + " = ?" : fieldName + " = ?";
            selectionArgs = new String[]{String.valueOf(object.getRowId())};
            values = new ContentValues();
            values.putNull(fieldName);
            this.db.update(tableName, values, selection, selectionArgs);
        }
    }

    private void updateOneToOneValue(Relationship relationship, long rowId, long relatedObjectRowId) {
        String tableName;
        String fieldName;
        if (relationship.isMajor()) {
            Long l;
            tableName = relationship.getBaseEntity().getTableName();
            fieldName = relationship.getForeignKeyName();
            if (relatedObjectRowId < 0) {
                l = null;
            } else {
                l = Long.valueOf(relatedObjectRowId);
            }
            updateRelationValue(tableName, fieldName, rowId, l);
        } else if (relatedObjectRowId < 0) {
            tableName = relationship.getInverseRelationship().getBaseEntity().getTableName();
            fieldName = relationship.getForeignKeyName();
            ContentValues contentValues = new ContentValues();
            contentValues.putNull(fieldName);
            this.db.update(tableName, contentValues, fieldName + " = ? ", new String[]{String.valueOf(rowId)});
        } else {
            updateRelationValue(relationship.getInverseRelationship().getBaseEntity().getTableName(), relationship.getForeignKeyName(), relatedObjectRowId, Long.valueOf(rowId));
        }
    }

    private void addOneToManyValue(Relationship relationship, List manageObjects, long id) {
        int size = manageObjects.size();
        for (int i = 0; i < size; i++) {
            Long rowId;
            ManagedObject obj = manageObjects.get(i);
            if (obj instanceof ManagedObject) {
                rowId = obj.getRowId();
            } else if (obj instanceof Long) {
                rowId = (Long) obj;
            } else {
                throw new ODMFIllegalArgumentException("the related id is null!");
            }
            updateRelationValue(relationship.getRelatedEntity().getTableName(), relationship.getInverseRelationship().getFieldName(), rowId.longValue(), Long.valueOf(id));
        }
    }

    private void addManyToManyValue(Relationship relationship, List manageObjects, long id) {
        String tableName = DatabaseTableHelper.getManyToManyMidTableName(relationship);
        Set<Long> ids = new HashSet();
        Cursor cursor = null;
        try {
            cursor = this.db.query(false, tableName, new String[]{DatabaseTableHelper.getRelationshipColumnName(relationship.getRelatedEntity())}, DatabaseTableHelper.getRelationshipColumnName(relationship.getBaseEntity()) + " = ?", new String[]{String.valueOf(id)}, null, null, null, null);
            while (cursor.moveToNext()) {
                ids.add(Long.valueOf(cursor.getLong(DatabaseQueryService.getOdmfRowidIndex())));
            }
            if (cursor != null) {
                cursor.close();
            }
            int size = manageObjects.size();
            for (int i = 0; i < size; i++) {
                Long obj = manageObjects.get(i);
                Long rawId = null;
                if (obj instanceof ManagedObject) {
                    rawId = ((ManagedObject) manageObjects.get(i)).getRowId();
                } else if (obj instanceof Long) {
                    rawId = obj;
                }
                if (rawId == null) {
                    throw new ODMFIllegalStateException("The object in the relationship has not be inserted yet.");
                }
                if (!ids.contains(rawId)) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DatabaseTableHelper.getRelationshipColumnName(relationship.getBaseEntity()), Long.valueOf(id));
                    contentValues.put(DatabaseTableHelper.getRelationshipColumnName(relationship.getRelatedEntity()), rawId);
                    this.db.insertOrThrow(tableName, null, contentValues);
                    ids.add(rawId);
                }
            }
        } catch (SQLException e) {
            LOG.logE("execute addManyToManyValue error : A SQLException happens when query related ids.");
            throw new ODMFRuntimeException("execute addManyToManyValue error : " + e.getMessage());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void removeManyToManyValue(Relationship relationship, long id) {
        this.db.delete(DatabaseTableHelper.getManyToManyMidTableName(relationship), DatabaseTableHelper.getRelationshipColumnName(relationship.getBaseEntity()) + " = ?", new String[]{String.valueOf(id)});
    }

    private void removeManyToManyValue(Relationship relationship, long id, List<ManagedObject> managedObjects) {
        this.db.delete(DatabaseTableHelper.getManyToManyMidTableName(relationship), DatabaseTableHelper.getRelationshipColumnName(relationship.getBaseEntity()) + " = ? AND " + DatabaseTableHelper.getRelationshipColumnName(relationship.getRelatedEntity()) + " IN (" + getIdString(managedObjects) + ")", new String[]{String.valueOf(id)});
    }

    private void removeOneToManyValue(Relationship relationship, long id) {
        String tableName = relationship.getRelatedEntity().getTableName();
        String whereClause = relationship.getInverseRelationship().getFieldName() + " = ?";
        String[] whereArgs = new String[]{String.valueOf(id)};
        ContentValues values = new ContentValues();
        values.putNull(relationship.getInverseRelationship().getFieldName());
        this.db.update(tableName, values, whereClause, whereArgs);
    }

    private void removeOneToManyValue(Relationship relationship, long id, List<ManagedObject> managedObjects) {
        String tableName = relationship.getRelatedEntity().getTableName();
        String whereClause = relationship.getInverseRelationship().getFieldName() + " = ? AND " + DatabaseQueryService.getRowidColumnName() + " IN (" + getIdString(managedObjects) + ")";
        String[] whereArgs = new String[]{String.valueOf(id)};
        ContentValues values = new ContentValues();
        values.putNull(relationship.getInverseRelationship().getFieldName());
        this.db.update(tableName, values, whereClause, whereArgs);
    }

    private String getIdString(List<ManagedObject> managedObjects) {
        StringBuilder stringBuilder = new StringBuilder();
        int size = managedObjects.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                stringBuilder.append(((ManagedObject) managedObjects.get(i)).getRowId());
                stringBuilder.append(',');
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }

    private void updateRelationValue(String entityName, String columnName, long rowId, Long value) {
        ContentValues contentValues = new ContentValues();
        if (value != null) {
            contentValues.put(columnName, value);
        } else {
            contentValues.putNull(columnName);
        }
        this.db.update(entityName, contentValues, DatabaseQueryService.getRowidColumnName() + " = ?", new String[]{String.valueOf(rowId)});
    }

    private boolean checkManyToOneCascadeDelete(Relationship relationship, ManagedObject object, ManagedObject relatedObj) {
        return checkManyToOneCascadeDelete(relationship, object, relatedObj.getRowId());
    }

    private boolean checkManyToOneCascadeDelete(Relationship relationship, ManagedObject object, Long relatedObj) {
        Cursor cursor = null;
        try {
            cursor = DatabaseQueryService.query(this.db, relationship.getBaseEntity().getTableName(), new String[]{relationship.getForeignKeyName()}, relationship.getForeignKeyName() + " = ? And " + DatabaseQueryService.getRowidColumnName() + " <> ?", new String[]{String.valueOf(relatedObj), String.valueOf(object.getRowId())});
            int count = cursor.getCount();
            if (cursor != null) {
                cursor.close();
            }
            if (count <= 0) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            LOG.logE("execute checkManyToOneCascadeDelete error : A SQLException happens when query related count.");
            throw new ODMFRuntimeException("execute checkManyToOneCascadeDelete error : " + e.getMessage());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean checkManyToManyCascadeDelete(Relationship relationship, ManagedObject object, ManagedObject relatedObj) {
        Cursor cursor = null;
        try {
            cursor = DatabaseQueryService.query(this.db, DatabaseTableHelper.getManyToManyMidTableName(relationship), new String[]{DatabaseTableHelper.getRelationshipColumnName(relationship.getRelatedEntity())}, DatabaseTableHelper.getRelationshipColumnName(relationship.getRelatedEntity()) + " = ? And " + DatabaseTableHelper.getRelationshipColumnName(relationship.getBaseEntity()) + " <> ?", new String[]{String.valueOf(relatedObj.getRowId()), String.valueOf(object.getRowId())});
            int count = cursor.getCount();
            if (cursor != null) {
                cursor.close();
            }
            if (count <= 0) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            LOG.logE("execute checkManyToManyCascadeDelete error : A SQLException happens when query related count.");
            throw new ODMFRuntimeException("execute checkManyToManyCascadeDelete error : " + e.getMessage());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean isContainedObject(List<ManagedObject> manageObjects, ManagedObject manageObject) {
        int size = manageObjects.size();
        for (int i = 0; i < size; i++) {
            ManagedObject object = (ManagedObject) manageObjects.get(i);
            if (object.getRowId().equals(manageObject.getRowId()) && object.getEntityName().equals(manageObject.getEntityName())) {
                return true;
            }
        }
        return false;
    }

    private void handleManyToManyRelationshipUseJavaList(Relationship relationship, Object values, long rowId) {
        removeManyToManyValue(relationship, rowId);
        addManyToManyValue(relationship, (List) values, rowId);
    }

    private void handleOneToManyRelationshipUseJavaList(Relationship relationship, Object values, long rowId) {
        removeOneToManyValue(relationship, rowId);
        addOneToManyValue(relationship, (List) values, rowId);
    }

    Cursor getManyToOneRelationshipCursor(ObjectId objectID, Relationship relationship) {
        Entity baseEntity = relationship.getBaseEntity();
        String fieldName = relationship.getFieldName();
        return DatabaseQueryService.query(this.db, baseEntity.getTableName(), new String[]{fieldName}, DatabaseQueryService.getRowidColumnName() + " = " + objectID.getId(), null);
    }

    Cursor getOneToOneRelationshipCursor(ObjectId objectID, Relationship relationship) {
        String fieldName;
        Entity relatedEntity = relationship.getRelatedEntity();
        Entity baseEntity = relationship.getBaseEntity();
        if (relationship.isMajor()) {
            fieldName = relationship.getFieldName();
        } else {
            fieldName = relationship.getInverseRelationship().getFieldName();
        }
        return DatabaseQueryService.query(this.db, relationship.isMajor() ? baseEntity.getTableName() : relatedEntity.getTableName(), relationship.isMajor() ? new String[]{fieldName + " AS " + fieldName} : new String[]{DatabaseQueryService.getRowidColumnName() + " AS " + fieldName}, relationship.isMajor() ? DatabaseQueryService.getRowidColumnName() + " = ?" : fieldName + " = ? ", new String[]{"" + objectID.getId()});
    }

    Cursor getOneToManyRelationshipCursor(ObjectId objectID, Relationship relationship) {
        Cursor cursor = DatabaseQueryService.query(this.db, relationship.getRelatedEntity().getTableName(), new String[]{DatabaseQueryService.getRowidColumnName() + " AS " + DatabaseQueryService.getRowidColumnName()}, relationship.getInverseRelationship().getFieldName() + " = ?", new String[]{String.valueOf(objectID.getId())});
        if (!relationship.getNotFound().equals(ARelationship.EXCEPTION) || cursor == null || cursor.getCount() > 0) {
            return cursor;
        }
        cursor.close();
        throw new ODMFRelatedObjectNotFoundException("The relevant object not found");
    }

    Cursor getManyToManyRelationshipCursor(ObjectId objectID, Relationship relationship) {
        Entity relatedEntity = relationship.getRelatedEntity();
        Entity baseEntity = relationship.getBaseEntity();
        Cursor cursor = DatabaseQueryService.query(this.db, baseEntity.getTableName() + " AS t1," + DatabaseTableHelper.getManyToManyMidTableName(relationship) + " AS t2", new String[]{"t2." + DatabaseTableHelper.getRelationshipColumnName(relatedEntity)}, "t1." + DatabaseQueryService.getRowidColumnName() + " = " + objectID.getId() + " AND " + "t1" + "." + DatabaseQueryService.getRowidColumnName() + " = t2." + DatabaseTableHelper.getRelationshipColumnName(baseEntity), null);
        if (!relationship.getNotFound().equals(ARelationship.EXCEPTION) || cursor == null || cursor.getCount() > 0) {
            return cursor;
        }
        cursor.close();
        throw new ODMFRelatedObjectNotFoundException("The relevant object not found");
    }
}
