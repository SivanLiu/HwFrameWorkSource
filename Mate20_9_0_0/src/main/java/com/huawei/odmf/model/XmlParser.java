package com.huawei.odmf.model;

import android.content.Context;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Xml;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFXmlParserException;
import com.huawei.odmf.model.api.Index;
import com.huawei.odmf.utils.JudgeUtils;
import com.huawei.odmf.utils.LOG;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XmlParser {
    private static final String DEFAULT_DATABASE_NAME = "NaturalBase";
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final int DEFAULT_VERSION_CODE = 1;
    private static final String TAG = "XmlParser";
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

    private static class RelationDescription {
        protected String baseClass;
        protected String cascade;
        protected String foreignKey;
        protected String inverseRelation;
        protected String lazy;
        protected String notFound;
        protected String refClass;
        protected String refProperty;
        protected String type;

        public RelationDescription(String refClass, String baseClass, String ref_property, String foreignKey, String cascade, String lazy, String notFound, String type, String inverseRelation) {
            this.refClass = refClass;
            this.baseClass = baseClass;
            this.refProperty = ref_property;
            this.foreignKey = foreignKey;
            this.cascade = cascade;
            this.lazy = lazy;
            this.notFound = notFound;
            this.type = type;
            this.inverseRelation = inverseRelation;
        }
    }

    public static AObjectModel parseToModel(String fileDir, String fileName) {
        if (TextUtils.isEmpty(fileDir) || TextUtils.isEmpty(fileName)) {
            throw new ODMFIllegalArgumentException("parameter fileDir or fileName is null");
        }
        try {
            AObjectModel model = parseFromStream(new FileInputStream(new File(fileDir, fileName)), fileName);
            if (model != null) {
                return model;
            }
            throw new ODMFXmlParserException("xml parser exception");
        } catch (FileNotFoundException e) {
            throw new ODMFIllegalArgumentException("The xml file not found");
        }
    }

    public static AObjectModel parseToModel(File file) {
        if (file == null) {
            throw new ODMFIllegalArgumentException("parameter file error");
        }
        try {
            AObjectModel model = parseFromStream(new FileInputStream(file), file.getName());
            if (model != null) {
                return model;
            }
            throw new ODMFXmlParserException("xml parser exception");
        } catch (FileNotFoundException e) {
            throw new ODMFIllegalArgumentException("The xml file not found");
        }
    }

    public static AObjectModel parseToModel(Context context, String assetsFileName) {
        if (TextUtils.isEmpty(assetsFileName) || context == null) {
            throw new ODMFIllegalArgumentException("parameter assetsFileName error");
        }
        try {
            AObjectModel model = parseFromStream(context.getAssets().open(assetsFileName), assetsFileName);
            if (model != null) {
                return model;
            }
            throw new ODMFXmlParserException("xml parser exception");
        } catch (FileNotFoundException e) {
            throw new ODMFIllegalArgumentException("The xml file not found");
        } catch (IOException e2) {
            throw new ODMFXmlParserException("An IOException occurred when parser xml");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x007c A:{SYNTHETIC, Splitter: B:22:0x007c} */
    /* JADX WARNING: Missing block: B:6:0x0038, code:
            r17 = r18;
            r15 = r16;
     */
    /* JADX WARNING: Missing block: B:7:0x003c, code:
            r25 = r42.next();
     */
    /* JADX WARNING: Missing block: B:85:0x01c9, code:
            throw new com.huawei.odmf.exception.ODMFXmlParserException("The xml form is wrong, the generator tag may not belong to any class or id.");
     */
    /* JADX WARNING: Missing block: B:114:0x02bd, code:
            throw new com.huawei.odmf.exception.ODMFXmlParserException("The xml form is wrong, the index-property tag may not belong to any class or composite-index.");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static AObjectModel parseFromStream(InputStream is, String fileName) {
        Throwable th;
        AObjectModel model = new AObjectModel();
        String version = DEFAULT_VERSION;
        int versionCode = 1;
        String databaseName = DEFAULT_DATABASE_NAME;
        boolean idCount = false;
        AEntity currentEntity = null;
        List<AEntityId> currentId = null;
        AIndex aIndex = null;
        Map<String, AEntity> entities = new ArrayMap();
        List<RelationDescription> relationDescriptions = new ArrayList();
        XmlPullParser xmlPullParser = Xml.newPullParser();
        try {
            xmlPullParser.setInput(is, "utf-8");
            int eventType = xmlPullParser.getEventType();
            while (true) {
                AIndex currentIndex = aIndex;
                List<AEntityId> currentId2 = currentId;
                if (eventType != 1) {
                    switch (eventType) {
                        case 2:
                            try {
                                if (!XML_NATURAL_BASE_MAPPING.equals(xmlPullParser.getName())) {
                                    if (!xmlPullParser.getName().equals(XML_CLASS)) {
                                        if (!xmlPullParser.getName().equals(XML_ID) && !xmlPullParser.getName().equals(XML_COMPOSITE_ID)) {
                                            AAttribute aAttribute;
                                            if (!xmlPullParser.getName().equals(XML_PROPERTY)) {
                                                if (!xmlPullParser.getName().equals(XML_GENERATOR)) {
                                                    if (!xmlPullParser.getName().equals(XML_KEY_PROPERTY)) {
                                                        if (!isRelationship(xmlPullParser.getName())) {
                                                            if (!xmlPullParser.getName().equals(XML_COMPOSITE_INDEX)) {
                                                                if (xmlPullParser.getName().equals(XML_INDEX_PROPERTY)) {
                                                                    String columnName = xmlPullParser.getAttributeValue(null, XML_NAME);
                                                                    if (currentEntity != null && currentIndex != null) {
                                                                        currentIndex.addAttribute(currentEntity.getAttribute(columnName));
                                                                        aIndex = currentIndex;
                                                                        currentId = currentId2;
                                                                        break;
                                                                    }
                                                                    break;
                                                                }
                                                            }
                                                            AIndex aIndex2 = new AIndex(xmlPullParser.getAttributeValue(null, XML_NAME));
                                                            currentId = currentId2;
                                                            break;
                                                        } else if (currentEntity != null) {
                                                            relationDescriptions.add(relationDescriptionParser(xmlPullParser, currentEntity.getEntityName()));
                                                            aIndex = currentIndex;
                                                            currentId = currentId2;
                                                            break;
                                                        } else {
                                                            throw new ODMFXmlParserException("The xml form is wrong, the relationship tag may not belong to any class.");
                                                        }
                                                    }
                                                    String name = xmlPullParser.getAttributeValue(null, XML_NAME);
                                                    if (currentEntity != null && currentId2 != null) {
                                                        aAttribute = currentEntity.getAttribute(name);
                                                        if (aAttribute != null) {
                                                            AEntityId entityId = new AEntityId(aAttribute.getFieldName(), aAttribute.getType(), aAttribute.hasIndex(), aAttribute.isUnique(), aAttribute.isNotNull(), aAttribute.isLazy(), aAttribute.getDefault_value(), AEntityId.NATURAL_ID);
                                                            currentId2.add(entityId);
                                                            int index = currentEntity.getAttributes().indexOf(aAttribute);
                                                            currentEntity.getAttributes().remove(aAttribute);
                                                            currentEntity.getAttributes().add(index, entityId);
                                                            aIndex = currentIndex;
                                                            currentId = currentId2;
                                                            break;
                                                        }
                                                        throw new ODMFXmlParserException("The xml form is wrong, the class do not have the property the key-property tag specified.");
                                                    }
                                                    break;
                                                }
                                                String generator_class = xmlPullParser.getAttributeValue(null, XML_CLASS);
                                                if (currentEntity != null && currentId2 != null) {
                                                    ((AEntityId) currentId2.get(0)).setGeneratorType(generator_class);
                                                    currentEntity.getAttributes().add(currentId2.get(0));
                                                    if (generator_class != null) {
                                                        if (generator_class.equals(AEntityId.INCREMENT)) {
                                                            currentEntity.setKeyAutoIncrement(true);
                                                        }
                                                    }
                                                    aIndex = currentIndex;
                                                    currentId = currentId2;
                                                    break;
                                                }
                                                break;
                                            }
                                            aAttribute = attributeParser(xmlPullParser);
                                            if (currentEntity != null) {
                                                currentEntity.getAttributes().add(aAttribute);
                                                aIndex = currentIndex;
                                                currentId = currentId2;
                                                break;
                                            }
                                            throw new ODMFXmlParserException("The xml form is wrong, the property tag may not belong to any class.");
                                        } else if (!idCount) {
                                            idCount = true;
                                            currentId = entityIdParser(xmlPullParser);
                                            aIndex = currentIndex;
                                            break;
                                        } else {
                                            throw new ODMFXmlParserException("The xml form is wrong, this class has too many primary key.");
                                        }
                                    }
                                    currentEntity = entityParser(xmlPullParser);
                                    currentEntity.setModel(model);
                                    currentId = new ArrayList();
                                    idCount = false;
                                    try {
                                        entities.put(currentEntity.getEntityName(), currentEntity);
                                        List<AAttribute> attributes = new ArrayList();
                                        List<ARelationship> relationships = new ArrayList();
                                        List<Index> indexes = new ArrayList();
                                        currentEntity.setAttributes(attributes);
                                        currentEntity.setRelationships(relationships);
                                        currentEntity.setIndexes(indexes);
                                        aIndex = currentIndex;
                                        break;
                                    } catch (IOException e) {
                                        aIndex = currentIndex;
                                        break;
                                    } catch (XmlPullParserException e2) {
                                        aIndex = currentIndex;
                                        break;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        aIndex = currentIndex;
                                        break;
                                    }
                                }
                                String currentVersion = xmlPullParser.getAttributeValue(null, XML_VERSION);
                                if (!TextUtils.isEmpty(currentVersion)) {
                                    if (JudgeUtils.checkVersion(currentVersion)) {
                                        version = currentVersion;
                                    } else {
                                        LOG.logE("The databaseVersion form is wrong.");
                                        throw new ODMFXmlParserException("The databaseVersion form is wrong.");
                                    }
                                }
                                String parseName = xmlPullParser.getAttributeValue(null, XML_DATABASE_NAME);
                                String currentVersionCode = xmlPullParser.getAttributeValue(null, XML_VERSION_CODE);
                                if (!TextUtils.isEmpty(currentVersionCode)) {
                                    versionCode = Integer.parseInt(currentVersionCode);
                                }
                                if (!TextUtils.isEmpty(parseName)) {
                                    databaseName = parseName;
                                }
                                aIndex = currentIndex;
                                currentId = currentId2;
                                break;
                            } catch (NumberFormatException e3) {
                                LOG.logE("The database version code form is wrong.");
                                throw new ODMFXmlParserException("The database version code form is wrong : " + e3.getMessage());
                            } catch (IOException e4) {
                                aIndex = currentIndex;
                                currentId = currentId2;
                                break;
                            } catch (XmlPullParserException e5) {
                                aIndex = currentIndex;
                                currentId = currentId2;
                                break;
                            } catch (Throwable th3) {
                                th = th3;
                                aIndex = currentIndex;
                                currentId = currentId2;
                                break;
                            }
                            break;
                        case 3:
                            if (!xmlPullParser.getName().equals(XML_CLASS)) {
                                if (xmlPullParser.getName().equals(XML_COMPOSITE_INDEX)) {
                                    if (currentEntity != null) {
                                        currentEntity.getIndexes().add(currentIndex);
                                        aIndex = null;
                                        currentId = currentId2;
                                        break;
                                    }
                                    throw new ODMFXmlParserException("The xml form is wrong, the composite-index tag may not belong to any class.");
                                }
                            } else if (currentEntity != null) {
                                currentEntity.setEntityId(currentId2);
                                if (entities.containsValue(currentEntity)) {
                                    entities.put(currentEntity.getEntityName(), currentEntity);
                                }
                                if (idCount) {
                                    currentEntity = null;
                                    currentId = null;
                                    aIndex = currentIndex;
                                    break;
                                }
                                throw new ODMFXmlParserException("The xml form is wrong, this class do not have a primary key.");
                            } else {
                                throw new ODMFXmlParserException("The xml form is wrong.");
                            }
                    }
                }
                relationDescriptions = sortRelationship(relationDescriptions);
                int size = relationDescriptions.size();
                for (int i = 0; i < size; i++) {
                    RelationDescription description = (RelationDescription) relationDescriptions.get(i);
                    if (entities.get(description.baseClass) != null) {
                        ((AEntity) entities.get(description.baseClass)).getRelationships().add(relationshipParser(description, entities));
                    }
                }
                model.setModelName(fileName);
                model.setDatabaseVersion(version);
                model.setDatabaseVersionCode(versionCode);
                model.setDatabaseName(databaseName);
                model.setEntities(entities);
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e6) {
                        LOG.logE("xml parser stream closed error.");
                    }
                }
                aIndex = currentIndex;
                currentId = currentId2;
            }
            throw new ODMFXmlParserException("The xml form is wrong, the key-property tag may not belong to any class or id.");
        } catch (IOException e7) {
            model = null;
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e8) {
                    LOG.logE("xml parser stream closed error.");
                }
            }
            return model;
        } catch (XmlPullParserException e9) {
            model = null;
            if (is != null) {
            }
            return model;
        } catch (Throwable th4) {
            th = th4;
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e10) {
                    LOG.logE("xml parser stream closed error.");
                }
            }
            throw th;
        }
    }

    public static boolean isRelationship(String tag_name) {
        return tag_name.equals(XML_TO_ONE) || tag_name.equals(XML_TO_MANY);
    }

    public static int getType(String typeName) throws ODMFXmlParserException {
        if (typeName.equals(XML_INT)) {
            return 0;
        }
        if (typeName.equals(XML_LONG)) {
            return 1;
        }
        if (typeName.equals(XML_SHORT)) {
            return 8;
        }
        if (typeName.equals(XML_STRING)) {
            return 2;
        }
        if (typeName.equals(XML_FLOAT)) {
            return 4;
        }
        if (typeName.equals(XML_DOUBLE)) {
            return 5;
        }
        if (typeName.equals(XML_BLOB)) {
            return 6;
        }
        if (typeName.equals(XML_CLOB)) {
            return 7;
        }
        if (typeName.equals(XML_TIME)) {
            return 10;
        }
        if (typeName.equals(XML_DATE)) {
            return 9;
        }
        if (typeName.equals(XML_BOOLEAN)) {
            return 3;
        }
        if (typeName.equals(XML_BYTE)) {
            return 11;
        }
        if (typeName.equals(XML_CALENDAR)) {
            return 12;
        }
        if (typeName.equals(XML_TIMESTAMP)) {
            return 13;
        }
        if (typeName.equals(XML_CHARACTER)) {
            return 14;
        }
        if (typeName.equals(XML_PRIM_INT)) {
            return 15;
        }
        if (typeName.equals(XML_PRIM_LONG)) {
            return 16;
        }
        if (typeName.equals(XML_PRIM_SHORT)) {
            return 17;
        }
        if (typeName.equals(XML_PRIM_FLOAT)) {
            return 18;
        }
        if (typeName.equals(XML_PRIM_DOUBLE)) {
            return 19;
        }
        if (typeName.equals(XML_PRIM_BOOLEAN)) {
            return 20;
        }
        if (typeName.equals(XML_PRIM_BYTE)) {
            return 21;
        }
        if (typeName.equals(XML_PRIM_CHAR)) {
            return 22;
        }
        throw new ODMFXmlParserException("illegal type defined.");
    }

    public static List<RelationDescription> sortRelationship(List<RelationDescription> relationDescriptions) {
        int i;
        RelationDescription description;
        List<RelationDescription> descriptions = new ArrayList();
        int size = relationDescriptions.size();
        for (i = 0; i < size; i++) {
            description = (RelationDescription) relationDescriptions.get(i);
            if (description.inverseRelation == null) {
                descriptions.add(description);
            }
        }
        for (i = 0; i < size; i++) {
            description = (RelationDescription) relationDescriptions.get(i);
            if (description.inverseRelation != null) {
                descriptions.add(description);
            }
        }
        return descriptions;
    }

    private static AEntity entityParser(XmlPullParser xmlPullParser) {
        AEntity currentEntity = new AEntity();
        String className = xmlPullParser.getAttributeValue(null, XML_NAME);
        String classPackage = xmlPullParser.getAttributeValue(null, XML_PACKAGE);
        String entityVersion = xmlPullParser.getAttributeValue(null, XML_VERSION);
        String currentVersionCode = xmlPullParser.getAttributeValue(null, XML_VERSION_CODE);
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
            currentEntity.setEntityName(classPackage + "." + className);
            currentEntity.setTableName(className);
            if (entityVersion == null) {
                entityVersion = DEFAULT_VERSION;
            }
            currentEntity.setEntityVersion(entityVersion);
            currentEntity.setEntityVersionCode(entityVersionCode);
            return currentEntity;
        } else {
            throw new ODMFXmlParserException("The entityVersion form is wrong.");
        }
    }

    private static List<AEntityId> entityIdParser(XmlPullParser xmlPullParser) throws ODMFXmlParserException {
        List<AEntityId> currentIds = new ArrayList();
        if (XML_ID.equals(xmlPullParser.getName())) {
            AEntityId currentId = new AEntityId();
            String id_name = xmlPullParser.getAttributeValue(null, XML_NAME);
            String id_type = xmlPullParser.getAttributeValue(null, XML_TYPE);
            currentId.setFieldName(id_name);
            currentId.setColumnName(id_name);
            currentId.setType(getType(id_type));
            currentId.setUnique(true);
            currentId.setNotNull(true);
            currentIds.add(currentId);
        }
        return currentIds;
    }

    private static boolean checkDefaultValue(String type, String defaultValue) {
        boolean z = true;
        if (type.equals(XML_CHARACTER) || type.equals(XML_PRIM_CHAR)) {
            if (defaultValue.length() != 1) {
                z = false;
            }
            return z;
        } else if (type.equals(XML_INT) || type.equals(XML_SHORT) || type.equals(XML_LONG) || type.equals(XML_PRIM_INT) || type.equals(XML_PRIM_SHORT) || type.equals(XML_PRIM_LONG) || type.equals(XML_BYTE) || type.equals(XML_PRIM_BYTE)) {
            return defaultValue.matches("^[+-]?[0-9]+$");
        } else {
            if (type.equals(XML_DOUBLE) || type.equals(XML_FLOAT) || type.equals(XML_PRIM_DOUBLE) || type.equals(XML_PRIM_FLOAT)) {
                return defaultValue.matches("^[+-]?[0-9]+(.[0-9]+)?$");
            }
            if (type.equals(XML_STRING) || type.equals(XML_BLOB) || type.equals(XML_CLOB)) {
                return true;
            }
            if (type.equals(XML_BOOLEAN) || type.equals(XML_PRIM_BOOLEAN)) {
                if (defaultValue.equals(XML_TRUE) || defaultValue.equals(XML_FALSE)) {
                    return true;
                }
                return false;
            } else if (type.equals(XML_TIME)) {
                return defaultValue.matches("[0-9]{2}:[0-9]{2}:[0-9]{2}");
            } else {
                if (type.equals(XML_TIMESTAMP) || type.equals(XML_CALENDAR) || type.equals(XML_DATE)) {
                    return defaultValue.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]+");
                }
                return false;
            }
        }
    }

    private static AAttribute attributeParser(XmlPullParser xmlPullParser) throws ODMFXmlParserException {
        boolean z = true;
        String property_name = xmlPullParser.getAttributeValue(null, XML_NAME);
        String property_type = xmlPullParser.getAttributeValue(null, XML_TYPE);
        String property_unique = xmlPullParser.getAttributeValue(null, XML_UNIQUE);
        String property_not_null = xmlPullParser.getAttributeValue(null, XML_NOT_NULL);
        String property_lazy = xmlPullParser.getAttributeValue(null, XML_LAZY);
        String property_default = xmlPullParser.getAttributeValue(null, XML_DEFAULT);
        String property_index = xmlPullParser.getAttributeValue(null, XML_INDEX);
        if (property_name == null || property_type == null) {
            LOG.logE("Parser attribute failed : The name and type must be set.");
            throw new ODMFXmlParserException("Parser relationship failed : The name, class, property-ref must be set.");
        } else if (property_default == null || checkDefaultValue(property_type, property_default)) {
            boolean z2;
            boolean z3;
            boolean z4;
            int typeId = getType(property_type);
            if (TextUtils.isEmpty(property_index) || !property_index.equals(XML_TRUE)) {
                z2 = false;
            } else {
                z2 = true;
            }
            if (TextUtils.isEmpty(property_unique) || !property_unique.equals(XML_TRUE)) {
                z3 = false;
            } else {
                z3 = true;
            }
            if (TextUtils.isEmpty(property_not_null) || !property_not_null.equals(XML_TRUE)) {
                z4 = false;
            } else {
                z4 = true;
            }
            if (TextUtils.isEmpty(property_lazy) || !property_lazy.equals(XML_TRUE)) {
                z = false;
            }
            return new AAttribute(property_name, typeId, z2, z3, z4, z, property_default);
        } else {
            throw new ODMFXmlParserException("default_value not match");
        }
    }

    private static RelationDescription relationDescriptionParser(XmlPullParser xmlPullParser, String entityName) throws ODMFXmlParserException {
        String relation_name = xmlPullParser.getAttributeValue(null, XML_NAME);
        String relation_class = xmlPullParser.getAttributeValue(null, XML_CLASS);
        String relation_property_ref = xmlPullParser.getAttributeValue(null, XML_PROPERTY_REF);
        String relation_cascade = xmlPullParser.getAttributeValue(null, XML_CASCADE);
        String relation_lazy = xmlPullParser.getAttributeValue(null, XML_LAZY);
        String relation_not_found = xmlPullParser.getAttributeValue(null, XML_NOT_FOUND);
        String relation_inverse = xmlPullParser.getAttributeValue(null, XML_MAPPED_BY);
        String relation_type = xmlPullParser.getName();
        if (relation_name != null && relation_class != null && relation_property_ref != null) {
            return new RelationDescription(relation_class, entityName, relation_property_ref, relation_name, relation_cascade, relation_lazy, relation_not_found, relation_type, relation_inverse);
        }
        LOG.logE("Parser relationship failed : The name, class, property-ref must be set.");
        throw new ODMFXmlParserException("Parser relationship failed : The name, class, property-ref must be set.");
    }

    private static int getRelationType(RelationDescription description) {
        if (description.type.equals(XML_TO_ONE)) {
            return 2;
        }
        if (description.type.equals(XML_TO_MANY)) {
            return 0;
        }
        throw new ODMFXmlParserException("illegal relationship defined:wrong type.");
    }

    private static ARelationship relationshipParser(RelationDescription description, Map<String, AEntity> entities) throws ODMFXmlParserException {
        String foreignKeyName = description.foreignKey;
        String relatedEntityIdName = description.refProperty;
        int relationShipType = getRelationType(description);
        AEntity baseEntity = (AEntity) entities.get(description.baseClass);
        AEntity relatedEntity = (AEntity) entities.get(description.refClass);
        if (relatedEntity == null) {
            throw new ODMFXmlParserException("illegal relationship defined:class not found.");
        }
        String cascade = description.cascade;
        if (cascade == null) {
            cascade = ARelationship.NONE_CASCADE;
        }
        boolean lazy = !TextUtils.isEmpty(description.lazy) && description.lazy.equals(XML_TRUE);
        String not_found = description.notFound;
        if (not_found == null) {
            not_found = ARelationship.IGNORE;
        }
        String inverse = description.inverseRelation;
        ARelationship relationship = new ARelationship(foreignKeyName, relatedEntityIdName, relationShipType, baseEntity, relatedEntity, cascade, lazy, not_found, null, true);
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
}
