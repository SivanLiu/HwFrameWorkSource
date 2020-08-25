package com.huawei.odmf.model;

import android.content.Context;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Xml;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFXmlParserException;
import com.huawei.odmf.model.api.Index;
import com.huawei.odmf.model.api.ObjectModel;
import com.huawei.odmf.utils.JudgeUtils;
import com.huawei.odmf.utils.LOG;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XmlParserHelper {
    private static final String DEFAULT_DATABASE_NAME = "NaturalBase";
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final int DEFAULT_VERSION_CODE = 1;
    private static final String XML_BLOB = "Blob";
    private static final String XML_BOOLEAN = "Boolean";
    private static final String XML_BYTE = "Byte";
    private static final String XML_CALENDAR = "Calendar";
    private static final String XML_CASCADE = "cascade";
    private static final String XML_CHARACTER = "Character";
    private static final String XML_CLASS = "class";
    private static final String XML_CLOB = "Clob";
    private static final String XML_COMPOSITE_ID = "composite-id";
    private static final String XML_COMPOSITE_INDEX = "composite-index";
    private static final String XML_DATABASE_NAME = "databaseName";
    private static final String XML_DATE = "Date";
    private static final String XML_DEFAULT = "default";
    private static final String XML_DOUBLE = "Double";
    private static final String XML_FALSE = "false";
    private static final String XML_FLOAT = "Float";
    private static final String XML_GENERATOR = "generator";
    private static final String XML_ID = "id";
    private static final String XML_INDEX = "index";
    private static final String XML_INDEX_PROPERTY = "index-property";
    private static final String XML_INT = "Integer";
    private static final String XML_KEY_PROPERTY = "key-property";
    private static final String XML_LAZY = "lazy";
    private static final String XML_LONG = "Long";
    private static final String XML_MAPPED_BY = "mapped-by";
    private static final String XML_NAME = "name";
    private static final String XML_NATURAL_BASE_MAPPING = "NaturalBase-mapping";
    private static final String XML_NOT_FOUND = "not-found";
    private static final String XML_NOT_NULL = "not_null";
    private static final String XML_PACKAGE = "package";
    private static final String XML_PRIM_BOOLEAN = "boolean";
    private static final String XML_PRIM_BYTE = "byte";
    private static final String XML_PRIM_CHAR = "char";
    private static final String XML_PRIM_DOUBLE = "double";
    private static final String XML_PRIM_FLOAT = "float";
    private static final String XML_PRIM_INT = "int";
    private static final String XML_PRIM_LONG = "long";
    private static final String XML_PRIM_SHORT = "short";
    private static final String XML_PROPERTY = "property";
    private static final String XML_PROPERTY_REF = "property-ref";
    private static final String XML_SHORT = "Short";
    private static final String XML_STRING = "String";
    private static final String XML_TIME = "Time";
    private static final String XML_TIMESTAMP = "Timestamp";
    private static final String XML_TO_MANY = "to-many";
    private static final String XML_TO_ONE = "to-one";
    private static final String XML_TRUE = "true";
    private static final String XML_TYPE = "type";
    private static final String XML_UNIQUE = "unique";
    private static final String XML_VERSION = "version";
    private static final String XML_VERSION_CODE = "versionCode";
    private AEntity currentEntity = null;
    private List<AEntityId> currentIds = null;
    private AIndex currentIndex = null;
    private String databaseName = DEFAULT_DATABASE_NAME;
    private Map<String, AEntity> entities;
    private String fileName;
    private boolean hasId = false;
    private InputStream inputStream;
    private AObjectModel model;
    private List<RelationDescription> relationDescriptions;
    private String version = DEFAULT_VERSION;
    private int versionCode = 1;
    private XmlPullParser xmlPullParser;

    XmlParserHelper(Context context, String fileName2) {
        this.fileName = fileName2;
        this.model = new AObjectModel();
        this.entities = new ArrayMap();
        this.relationDescriptions = new ArrayList();
        this.xmlPullParser = Xml.newPullParser();
        try {
            this.inputStream = context.getAssets().open(fileName2);
            initXmlParser();
        } catch (FileNotFoundException e) {
            throw new ODMFIllegalArgumentException("The xml file not found.");
        } catch (IOException e2) {
            throw new ODMFXmlParserException("An IOException occurred when parser xml.");
        }
    }

    XmlParserHelper(InputStream inputStream2, String fileName2) {
        this.fileName = fileName2;
        this.inputStream = inputStream2;
        this.model = new AObjectModel();
        this.entities = new ArrayMap();
        this.relationDescriptions = new ArrayList();
        this.xmlPullParser = Xml.newPullParser();
        initXmlParser();
    }

    private void initXmlParser() {
        try {
            this.xmlPullParser.setInput(this.inputStream, "utf-8");
        } catch (XmlPullParserException e) {
            throw new ODMFXmlParserException("An XmlPullParserException occurred when set input stream of the XmlPullParser.");
        }
    }

    public ObjectModel getModel() {
        AObjectModel aObjectModel;
        try {
            int eventType = this.xmlPullParser.getEventType();
            while (eventType != 1) {
                switch (eventType) {
                    case 2:
                        parserStartTag();
                        break;
                    case 3:
                        parserEndTag();
                        break;
                }
                eventType = this.xmlPullParser.next();
            }
            generateModel();
            aObjectModel = this.model;
            try {
                if (this.inputStream != null) {
                    this.inputStream.close();
                }
            } catch (IOException e) {
                LOG.logE("xml parser stream closed error.");
            }
        } catch (IOException | XmlPullParserException e2) {
            aObjectModel = null;
            try {
                if (this.inputStream != null) {
                    this.inputStream.close();
                }
            } catch (IOException e3) {
                LOG.logE("xml parser stream closed error.");
            }
        } catch (Throwable th) {
            try {
                if (this.inputStream != null) {
                    this.inputStream.close();
                }
            } catch (IOException e4) {
                LOG.logE("xml parser stream closed error.");
            }
            throw th;
        }
        return aObjectModel;
    }

    private void parserStartTag() {
        if (XML_NATURAL_BASE_MAPPING.equals(this.xmlPullParser.getName())) {
            parserNaturalBaseMapping();
        } else if (XML_CLASS.equals(this.xmlPullParser.getName())) {
            parserClass();
        } else if (XML_ID.equals(this.xmlPullParser.getName()) || XML_COMPOSITE_ID.equals(this.xmlPullParser.getName())) {
            parserPrimaryKey();
        } else if (XML_PROPERTY.equals(this.xmlPullParser.getName())) {
            parserProperty();
        } else if (XML_GENERATOR.equals(this.xmlPullParser.getName())) {
            parserPrimaryKeyGenerator();
        } else if (XML_KEY_PROPERTY.equals(this.xmlPullParser.getName())) {
            parserPrimaryKeyProperty();
        } else if (isRelationship(this.xmlPullParser.getName())) {
            parserRelationship();
        } else if (XML_COMPOSITE_INDEX.equals(this.xmlPullParser.getName())) {
            parserCompositeIndex();
        } else if (XML_INDEX_PROPERTY.equals(this.xmlPullParser.getName())) {
            parserIndexProperty();
        } else {
            LOG.logE("The xml form is wrong, the name of start tag is mismatch.");
        }
    }

    private void parserEndTag() {
        if (XML_CLASS.equals(this.xmlPullParser.getName())) {
            if (this.currentEntity == null) {
                throw new ODMFXmlParserException("The xml form is wrong, no start tag of a class match this end tag.");
            }
            this.currentEntity.setEntityIds(this.currentIds);
            if (this.entities.containsValue(this.currentEntity)) {
                this.entities.put(this.currentEntity.getEntityName(), this.currentEntity);
            }
            if (!this.hasId) {
                throw new ODMFXmlParserException("The xml form is wrong, this class do not have a primary key.");
            }
            this.currentEntity = null;
            this.currentIds = null;
        }
        if (!XML_COMPOSITE_INDEX.equals(this.xmlPullParser.getName())) {
            return;
        }
        if (this.currentEntity == null) {
            throw new ODMFXmlParserException("The xml form is wrong, the composite-index tag may not belong to any class.");
        }
        this.currentEntity.getIndexes().add(this.currentIndex);
        this.currentIndex = null;
    }

    private void generateModel() {
        this.relationDescriptions = sortRelationship();
        int size = this.relationDescriptions.size();
        for (int i = 0; i < size; i++) {
            RelationDescription description = this.relationDescriptions.get(i);
            if (this.entities.get(description.baseClass) != null) {
                this.entities.get(description.baseClass).getRelationships().add(relationshipParser(description));
            }
        }
        this.model.setModelName(this.fileName);
        this.model.setDatabaseVersion(this.version);
        this.model.setDatabaseVersionCode(this.versionCode);
        this.model.setDatabaseName(this.databaseName);
        this.model.setEntities(this.entities);
    }

    private void parserIndexProperty() {
        String columnName = this.xmlPullParser.getAttributeValue(null, XML_NAME);
        if (this.currentEntity == null || this.currentIndex == null) {
            throw new ODMFXmlParserException("The xml form is wrong, the index-property tag may not belong to any class or composite-index.");
        }
        this.currentIndex.addAttribute(this.currentEntity.getAttribute(columnName));
    }

    private void parserCompositeIndex() {
        this.currentIndex = new AIndex(this.xmlPullParser.getAttributeValue(null, XML_NAME));
    }

    private void parserRelationship() {
        if (this.currentEntity == null) {
            throw new ODMFXmlParserException("The xml form is wrong, the relationship tag may not belong to any class.");
        }
        this.relationDescriptions.add(relationDescriptionParser());
    }

    private void parserPrimaryKeyProperty() {
        String name = this.xmlPullParser.getAttributeValue(null, XML_NAME);
        if (this.currentEntity == null || this.currentIds == null) {
            throw new ODMFXmlParserException("The xml form is wrong, the key-property tag may not belong to any class or id.");
        }
        AAttribute attribute = this.currentEntity.getAttribute(name);
        if (attribute == null) {
            throw new ODMFXmlParserException("The xml form is wrong, the class do not have the property the key-property tag specified.");
        }
        AEntityId entityId = new AEntityId(attribute.getFieldName(), attribute.getType(), attribute.hasIndex(), attribute.isUnique(), attribute.isNotNull(), attribute.isLazy(), attribute.getDefaultValue(), AEntityId.NATURAL_ID);
        this.currentIds.add(entityId);
        int index = this.currentEntity.getAttributes().indexOf(attribute);
        this.currentEntity.getAttributes().remove(attribute);
        this.currentEntity.getAttributes().add(index, entityId);
    }

    private void parserPrimaryKeyGenerator() {
        String generatorType = this.xmlPullParser.getAttributeValue(null, XML_CLASS);
        if (this.currentEntity == null || this.currentIds == null) {
            throw new ODMFXmlParserException("The xml form is wrong, the generator tag may not belong to any class or id.");
        }
        this.currentIds.get(0).setGeneratorType(generatorType);
        this.currentEntity.getAttributes().add(this.currentIds.get(0));
        if (generatorType != null && generatorType.equals(AEntityId.INCREMENT)) {
            this.currentEntity.setKeyAutoIncrement(true);
        }
    }

    private void parserProperty() {
        AAttribute attribute = attributeParser();
        if (this.currentEntity == null) {
            throw new ODMFXmlParserException("The xml form is wrong, the property tag may not belong to any class.");
        }
        this.currentEntity.getAttributes().add(attribute);
    }

    private void parserPrimaryKey() {
        if (this.hasId) {
            throw new ODMFXmlParserException("The xml form is wrong, this class has too many primary key.");
        }
        this.hasId = true;
        this.currentIds = entityIdParser();
    }

    private void parserClass() {
        this.currentEntity = entityParser();
        this.currentEntity.setModel(this.model);
        this.currentIds = new ArrayList();
        this.hasId = false;
        this.entities.put(this.currentEntity.getEntityName(), this.currentEntity);
        List<AAttribute> attributes = new ArrayList<>();
        List<ARelationship> relationships = new ArrayList<>();
        List<Index> indexes = new ArrayList<>();
        this.currentEntity.setAttributes(attributes);
        this.currentEntity.setRelationships(relationships);
        this.currentEntity.setIndexes(indexes);
    }

    private void parserNaturalBaseMapping() {
        String currentVersion = this.xmlPullParser.getAttributeValue(null, XML_VERSION);
        if (!TextUtils.isEmpty(currentVersion)) {
            if (!JudgeUtils.checkVersion(currentVersion)) {
                LOG.logE("The databaseVersion form is wrong.");
                throw new ODMFXmlParserException("The databaseVersion form is wrong.");
            }
            this.version = currentVersion;
        }
        String parseName = this.xmlPullParser.getAttributeValue(null, XML_DATABASE_NAME);
        String currentVersionCode = this.xmlPullParser.getAttributeValue(null, XML_VERSION_CODE);
        if (!TextUtils.isEmpty(currentVersionCode)) {
            try {
                this.versionCode = Integer.parseInt(currentVersionCode);
            } catch (NumberFormatException e) {
                LOG.logE("The database version code form is wrong.");
                throw new ODMFXmlParserException("The database version code form is wrong : " + e.getMessage());
            }
        }
        if (!TextUtils.isEmpty(parseName)) {
            this.databaseName = parseName;
        }
    }

    private boolean isRelationship(String tagName) {
        return XML_TO_ONE.equals(tagName) || XML_TO_MANY.equals(tagName);
    }

    private int getType(String typeName) throws ODMFXmlParserException {
        if (XML_INT.equals(typeName)) {
            return 0;
        }
        if (XML_LONG.equals(typeName)) {
            return 1;
        }
        if (XML_SHORT.equals(typeName)) {
            return 8;
        }
        if (XML_STRING.equals(typeName)) {
            return 2;
        }
        if (XML_FLOAT.equals(typeName)) {
            return 4;
        }
        if (XML_DOUBLE.equals(typeName)) {
            return 5;
        }
        if (XML_BLOB.equals(typeName)) {
            return 6;
        }
        if (XML_CLOB.equals(typeName)) {
            return 7;
        }
        if (XML_TIME.equals(typeName)) {
            return 10;
        }
        if (XML_DATE.equals(typeName)) {
            return 9;
        }
        if (XML_BOOLEAN.equals(typeName)) {
            return 3;
        }
        if (XML_BYTE.equals(typeName)) {
            return 11;
        }
        if (XML_CALENDAR.equals(typeName)) {
            return 12;
        }
        if (XML_TIMESTAMP.equals(typeName)) {
            return 13;
        }
        if (XML_CHARACTER.equals(typeName)) {
            return 14;
        }
        if (XML_PRIM_INT.equals(typeName)) {
            return 15;
        }
        if ("long".equals(typeName)) {
            return 16;
        }
        if (XML_PRIM_SHORT.equals(typeName)) {
            return 17;
        }
        if ("float".equals(typeName)) {
            return 18;
        }
        if ("double".equals(typeName)) {
            return 19;
        }
        if (XML_PRIM_BOOLEAN.equals(typeName)) {
            return 20;
        }
        if (XML_PRIM_BYTE.equals(typeName)) {
            return 21;
        }
        if (XML_PRIM_CHAR.equals(typeName)) {
            return 22;
        }
        throw new ODMFXmlParserException("illegal type defined.");
    }

    private List<RelationDescription> sortRelationship() {
        List<RelationDescription> descriptions = new ArrayList<>();
        int size = this.relationDescriptions.size();
        for (int i = 0; i < size; i++) {
            RelationDescription description = this.relationDescriptions.get(i);
            if (description.inverseRelation == null) {
                descriptions.add(description);
            }
        }
        for (int i2 = 0; i2 < size; i2++) {
            RelationDescription description2 = this.relationDescriptions.get(i2);
            if (description2.inverseRelation != null) {
                descriptions.add(description2);
            }
        }
        return descriptions;
    }

    private AEntity entityParser() {
        String className = this.xmlPullParser.getAttributeValue(null, XML_NAME);
        String classPackage = this.xmlPullParser.getAttributeValue(null, XML_PACKAGE);
        String entityVersion = this.xmlPullParser.getAttributeValue(null, XML_VERSION);
        String currentVersionCode = this.xmlPullParser.getAttributeValue(null, XML_VERSION_CODE);
        int entityVersionCode = 1;
        if (className == null || classPackage == null) {
            LOG.logE("Parser entity failed : The className and classPackage must be set.");
            throw new ODMFXmlParserException("Parser relationship failed : The name, class, property-ref must be set.");
        } else if (entityVersion == null || JudgeUtils.checkVersion(entityVersion)) {
            if (!TextUtils.isEmpty(currentVersionCode)) {
                try {
                    entityVersionCode = Integer.parseInt(currentVersionCode);
                } catch (NumberFormatException e) {
                    LOG.logE("The entity version code form is wrong.");
                    throw new ODMFXmlParserException("The entity version code form is wrong : " + e.getMessage());
                }
            }
            AEntity currentEntity2 = new AEntity();
            currentEntity2.setEntityName(classPackage + "." + className);
            currentEntity2.setTableName(className);
            if (entityVersion == null) {
                entityVersion = DEFAULT_VERSION;
            }
            currentEntity2.setEntityVersion(entityVersion);
            currentEntity2.setEntityVersionCode(entityVersionCode);
            return currentEntity2;
        } else {
            throw new ODMFXmlParserException("The entityVersion form is wrong.");
        }
    }

    private List<AEntityId> entityIdParser() throws ODMFXmlParserException {
        List<AEntityId> currentIds2 = new ArrayList<>();
        if (XML_ID.equals(this.xmlPullParser.getName())) {
            AEntityId currentId = new AEntityId();
            String idName = this.xmlPullParser.getAttributeValue(null, XML_NAME);
            String idType = this.xmlPullParser.getAttributeValue(null, XML_TYPE);
            currentId.setFieldName(idName);
            currentId.setColumnName(idName);
            currentId.setType(getType(idType));
            currentId.setUnique(true);
            currentId.setNotNull(true);
            currentIds2.add(currentId);
        }
        return currentIds2;
    }

    private boolean checkDefaultValue(String type, String defaultValue) {
        boolean z = true;
        if (type.equals(XML_CHARACTER) || type.equals(XML_PRIM_CHAR)) {
            if (defaultValue.length() != 1) {
                z = false;
            }
            return z;
        } else if (type.equals(XML_INT) || type.equals(XML_SHORT) || type.equals(XML_LONG) || type.equals(XML_BYTE) || type.equals(XML_PRIM_INT) || type.equals(XML_PRIM_SHORT) || type.equals("long") || type.equals(XML_PRIM_BYTE)) {
            return defaultValue.matches("^[+-]?[0-9]+$");
        } else {
            if (type.equals(XML_DOUBLE) || type.equals(XML_FLOAT) || type.equals("double") || type.equals("float")) {
                return defaultValue.matches("^[+-]?[0-9]+(.[0-9]+)?$");
            }
            if (type.equals(XML_STRING) || type.equals(XML_BLOB) || type.equals(XML_CLOB)) {
                return true;
            }
            if (type.equals(XML_BOOLEAN) || type.equals(XML_PRIM_BOOLEAN)) {
                return defaultValue.equals(XML_TRUE) || defaultValue.equals(XML_FALSE);
            }
            if (type.equals(XML_TIME)) {
                return defaultValue.matches("[0-9]{2}:[0-9]{2}:[0-9]{2}");
            }
            if (type.equals(XML_TIMESTAMP) || type.equals(XML_CALENDAR) || type.equals(XML_DATE)) {
                return defaultValue.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]+");
            }
            return false;
        }
    }

    private AAttribute attributeParser() throws ODMFXmlParserException {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4 = true;
        String propertyName = this.xmlPullParser.getAttributeValue(null, XML_NAME);
        String propertyType = this.xmlPullParser.getAttributeValue(null, XML_TYPE);
        String propertyUnique = this.xmlPullParser.getAttributeValue(null, XML_UNIQUE);
        String propertyNotNull = this.xmlPullParser.getAttributeValue(null, XML_NOT_NULL);
        String propertyLazy = this.xmlPullParser.getAttributeValue(null, XML_LAZY);
        String propertyDefault = this.xmlPullParser.getAttributeValue(null, XML_DEFAULT);
        String propertyIndex = this.xmlPullParser.getAttributeValue(null, XML_INDEX);
        if (propertyName == null || propertyType == null) {
            LOG.logE("Parser attribute failed : The name and type must be set.");
            throw new ODMFXmlParserException("Parser relationship failed : The name, class, property-ref must be set.");
        } else if (propertyDefault == null || checkDefaultValue(propertyType, propertyDefault)) {
            int typeId = getType(propertyType);
            if (TextUtils.isEmpty(propertyIndex) || !propertyIndex.equals(XML_TRUE)) {
                z = false;
            } else {
                z = true;
            }
            if (TextUtils.isEmpty(propertyUnique) || !propertyUnique.equals(XML_TRUE)) {
                z2 = false;
            } else {
                z2 = true;
            }
            if (TextUtils.isEmpty(propertyNotNull) || !propertyNotNull.equals(XML_TRUE)) {
                z3 = false;
            } else {
                z3 = true;
            }
            if (TextUtils.isEmpty(propertyLazy) || !propertyLazy.equals(XML_TRUE)) {
                z4 = false;
            }
            return new AAttribute(propertyName, typeId, z, z2, z3, z4, propertyDefault);
        } else {
            throw new ODMFXmlParserException("default_value not match");
        }
    }

    private RelationDescription relationDescriptionParser() throws ODMFXmlParserException {
        String relationName = this.xmlPullParser.getAttributeValue(null, XML_NAME);
        String relationClass = this.xmlPullParser.getAttributeValue(null, XML_CLASS);
        String relationPropertyRef = this.xmlPullParser.getAttributeValue(null, XML_PROPERTY_REF);
        String relationCascade = this.xmlPullParser.getAttributeValue(null, XML_CASCADE);
        String relationLazy = this.xmlPullParser.getAttributeValue(null, XML_LAZY);
        String relationNotFound = this.xmlPullParser.getAttributeValue(null, XML_NOT_FOUND);
        String relationInverse = this.xmlPullParser.getAttributeValue(null, XML_MAPPED_BY);
        String relationType = this.xmlPullParser.getName();
        if (relationName != null && relationClass != null && relationPropertyRef != null) {
            return new RelationDescription(relationClass, this.currentEntity.getEntityName(), relationPropertyRef, relationName, relationCascade, relationLazy, relationNotFound, relationType, relationInverse);
        }
        LOG.logE("Parser relationship failed : The name, class, property-ref must be set.");
        throw new ODMFXmlParserException("Parser relationship failed : The name, class, property-ref must be set.");
    }

    private int getRelationType(RelationDescription description) {
        if (description.type.equals(XML_TO_ONE)) {
            return 2;
        }
        if (description.type.equals(XML_TO_MANY)) {
            return 0;
        }
        throw new ODMFXmlParserException("Illegal relationship defined:wrong type.");
    }

    private ARelationship relationshipParser(RelationDescription description) throws ODMFXmlParserException {
        String foreignKeyName = description.foreignKey;
        String relatedEntityIdName = description.refProperty;
        int relationShipType = getRelationType(description);
        String cascade = description.cascade;
        if (cascade == null) {
            cascade = ARelationship.NONE_CASCADE;
        }
        boolean isLazy = !TextUtils.isEmpty(description.lazy) && description.lazy.equals(XML_TRUE);
        String notFound = description.notFound;
        if (notFound == null) {
            notFound = ARelationship.IGNORE;
        }
        String inverse = description.inverseRelation;
        AEntity baseEntity = this.entities.get(description.baseClass);
        AEntity relatedEntity = this.entities.get(description.refClass);
        if (relatedEntity == null) {
            throw new ODMFXmlParserException("Illegal relationship defined:class not found.");
        }
        ARelationship relationship = new ARelationship(foreignKeyName, relatedEntityIdName, relationShipType, baseEntity, relatedEntity, cascade, isLazy, notFound, null, true);
        if (!(inverse == null || relatedEntity.getRelationship(inverse) == null)) {
            relationship.setInverseRelationship(relatedEntity.getRelationship(inverse));
            relationship.setRelationShipType(relationShipType + (relatedEntity.getRelationship(inverse).getRelationShipType() * 2));
            relatedEntity.getRelationship(inverse).setInverseRelationship(relationship);
            relatedEntity.getRelationship(inverse).setRelationShipType((relationShipType * 2) + relatedEntity.getRelationship(inverse).getRelationShipType());
            if (relationShipType == 2 && relatedEntity.getRelationship(inverse).getRelationShipType() == 4) {
                relatedEntity.getRelationship(inverse).setMajor(false);
            } else {
                relationship.setMajor(false);
            }
        }
        return relationship;
    }

    private static class RelationDescription {
        String baseClass;
        String cascade;
        String foreignKey;
        String inverseRelation;
        String lazy;
        String notFound;
        String refClass;
        String refProperty;
        String type;

        RelationDescription(String refClass2, String baseClass2, String refProperty2, String foreignKey2, String cascade2, String lazy2, String notFound2, String type2, String inverseRelation2) {
            this.refClass = refClass2;
            this.baseClass = baseClass2;
            this.refProperty = refProperty2;
            this.foreignKey = foreignKey2;
            this.cascade = cascade2;
            this.lazy = lazy2;
            this.notFound = notFound2;
            this.type = type2;
            this.inverseRelation = inverseRelation2;
        }
    }
}
