package org.boon.json.serializers.impl;

import org.boon.cache.Cache;
import org.boon.core.Type;
import org.boon.core.reflection.FastStringUtils;
import org.boon.core.reflection.fields.FieldAccess;
import org.boon.json.serializers.*;
import org.boon.primitive.CharBuf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Created by rick on 1/1/14.
 */
public class FieldSerializerUseAnnotationsImpl implements FieldSerializer {

    private final boolean includeNulls;
    private final boolean includeDefault;
    private final boolean useAnnotations;
    private final boolean includeEmpty;
    private final boolean handleSimpleBackReference;
    private final boolean handleComplexBackReference;
    private IdentityHashMap idMap;
    private Map<Class, CustomObjectSerializer> overrideMap;
    private Map<String, CustomFieldSerializer> customFieldSerializerMap;
    private List<CustomFieldSerializer> customFieldSerializers;

    private final List<FieldFilter> filterProperties;




    public FieldSerializerUseAnnotationsImpl ( boolean includeNulls,
                                               boolean includeDefault, boolean useAnnotations,
                                               boolean includeEmpty, boolean handleSimpleBackReference,
                                               boolean handleComplexBackReference,
                                               Map<Class, CustomObjectSerializer> overrideMap,
                                               List<FieldFilter> filterProperties,
                                               Map<String, CustomFieldSerializer> customFieldSerializerMap,
                                               List<CustomFieldSerializer> customFieldSerializers) {
        this.includeNulls = includeNulls;
        this.includeDefault = includeDefault;
        this.useAnnotations = useAnnotations;
        this.includeEmpty = includeEmpty;
        this.handleSimpleBackReference = handleSimpleBackReference;
        this.handleComplexBackReference = handleComplexBackReference;

        if (handleComplexBackReference) {
            idMap = new IdentityHashMap (  );
        }

        this.overrideMap = overrideMap;
        this.filterProperties = filterProperties;
        this.customFieldSerializerMap = customFieldSerializerMap;
        this.customFieldSerializers = customFieldSerializers;

    }

    private void serializeFieldName ( String name, CharBuf builder ) {
        builder.addJsonFieldName ( FastStringUtils.toCharArray ( name ) );
    }

    @Override
    public final boolean serializeField ( JsonSerializerInternal serializer, Object parent, FieldAccess fieldAccess, CharBuf builder ) {

        final String fieldName = fieldAccess.getName();
        final Type typeEnum = fieldAccess.typeEnum();
        if ( useAnnotations && fieldAccess.ignore() )  {
            return false;
        }
        final boolean include = (useAnnotations && fieldAccess.include());

        if (filterProperties != null ) {
            for (FieldFilter filter: filterProperties)  {
                if (!filter.include ( parent, fieldAccess )) {
                    return false;
                }
            }
        }


        if ( customFieldSerializerMap != null ) {

            final CustomFieldSerializer customFieldSerializer = customFieldSerializerMap.get ( fieldAccess.getName () );
            if ( customFieldSerializer.serializeField (serializer, parent, fieldAccess, builder) ) {
                return true;
            }
        }

        if ( customFieldSerializers != null ) {

            for (CustomFieldSerializer cfs : customFieldSerializers) {
                if (cfs.serializeField ( serializer, parent, fieldAccess, builder ) == true) {
                    return true;
                }
            }

        }



        switch ( typeEnum ) {
            case INT:
                int value = fieldAccess.getInt ( parent );
                if (includeDefault || include || value !=0) {
                    serializeFieldName ( fieldName, builder );
                    builder.addInt ( value  );
                    return true;
                }
                return false;
            case BOOLEAN:
                boolean bvalue = fieldAccess.getBoolean ( parent );
                if (includeDefault || include ||  bvalue ) {
                    serializeFieldName ( fieldName, builder );
                    builder.addBoolean ( bvalue  );
                    return true;
                }
                return false;

            case BYTE:
                byte byvalue = fieldAccess.getByte ( parent );
                if (includeDefault || include ||  byvalue != 0 ) {
                    serializeFieldName ( fieldName, builder );
                    builder.addByte ( byvalue  );
                    return true;
                }
                return false;
            case LONG:
                long lvalue = fieldAccess.getLong ( parent );
                if ( includeDefault || include || lvalue != 0 ) {
                    serializeFieldName ( fieldName, builder );
                    builder.addLong ( lvalue  );
                    return true;
                }
                return false;
            case DOUBLE:
                double dvalue = fieldAccess.getDouble ( parent );
                if (includeDefault || include || dvalue != 0 ) {
                    serializeFieldName ( fieldName, builder );
                    builder.addDouble ( dvalue  );
                    return true;
                }
                return false;
            case FLOAT:
                float fvalue = fieldAccess.getFloat ( parent );
                if (includeDefault || include || fvalue != 0.0f ) {
                    serializeFieldName ( fieldName, builder );
                    builder.addFloat ( fvalue  );
                    return true;
                }
                return false;
            case SHORT:
                short svalue = fieldAccess.getShort( parent );
                if (includeDefault || include || svalue != 0 ) {
                    serializeFieldName ( fieldName, builder );
                    builder.addShort ( svalue  );
                    return true;
                }
                return false;
            case CHAR:
                char cvalue = fieldAccess.getChar( parent );
                if (includeDefault || include || cvalue != 0 ) {
                    serializeFieldName ( fieldName, builder );
                    builder.addChar( cvalue  );
                    return true;
                }
                return false;

        }

        Object value = fieldAccess.getObject ( parent );

        if (!includeNulls && !include && value == null ) {
            return false;
        }


        if ((includeNulls || fieldAccess.include()) &&  value == null ) {
            serializeFieldName ( fieldName, builder );
            builder.addNull ();
            return true;
        }


        if (handleSimpleBackReference && value == parent ) {
            return false;
        } else if (handleComplexBackReference) {
            if ( idMap.containsKey ( value ) ) {
                return false;
            } else {
                idMap.put ( value, value );
            }
        }


        if ( overrideMap!=null ) {
            final CustomObjectSerializer customObjectSerializer = overrideMap.get ( fieldAccess.getType () );
            if (customObjectSerializer!=null) {
                serializeFieldName ( fieldName, builder );
                customObjectSerializer.serializeObject ( serializer, value, builder );
                return true;
            }
        }



        switch ( typeEnum )  {
            case BIG_DECIMAL:
                serializeFieldName ( fieldName, builder );
                builder.addBigDecimal ( (BigDecimal ) value );
                return true;
            case BIG_INT:
                serializeFieldName ( fieldName, builder );
                builder.addBigInteger ( ( BigInteger ) value );
                return true;
            case DATE:
                serializeFieldName ( fieldName, builder );
                serializer.serializeDate ( ( Date ) value, builder );
                return true;
            case STRING:
                String string = (String) value;
                if (includeEmpty ||  include || string.length() > 0) {
                    serializeFieldName ( fieldName, builder );
                    serializer.serializeString( string, builder );
                    return true;
                }
                return false;
            case CHAR_SEQUENCE:
                String s2 =  value.toString();
                if (includeEmpty ||  include || s2.length() > 0) {
                    serializeFieldName ( fieldName, builder );
                    serializer.serializeString( s2, builder );
                    return true;
                }
                return false;
            case INTEGER_WRAPPER:
                serializeFieldName ( fieldName, builder );
                builder.addInt ( ( Integer ) value );
                return true;
            case LONG_WRAPPER:
                serializeFieldName ( fieldName, builder );
                builder.addLong ( ( Long ) value );
                return true;
            case FLOAT_WRAPPER:
                serializeFieldName ( fieldName, builder );
                builder.addFloat ( ( Float ) value );
                return true;
            case DOUBLE_WRAPPER:
                serializeFieldName ( fieldName, builder );
                builder.addDouble ( ( Double ) value );
                return true;
            case SHORT_WRAPPER:
                serializeFieldName ( fieldName, builder );
                builder.addShort ( ( Short ) value );
                return true;
            case BYTE_WRAPPER:
                serializeFieldName ( fieldName, builder );
                builder.addByte ( ( Byte ) value );
                return true;
            case CHAR_WRAPPER:
                serializeFieldName ( fieldName, builder );
                builder.addChar ( ( Character ) value );
                return true;
            case ENUM:
                serializeFieldName ( fieldName, builder );
                builder.addQuoted ( value.toString () );
                return true;
            case COLLECTION:
            case LIST:
            case SET:
                Collection collection = (Collection) value;
                if (includeEmpty ||  include || collection.size () > 0) {
                    serializeFieldName ( fieldName, builder );
                    serializer.serializeCollection ( collection, builder );
                    return true;
                }
                return false;
            case MAP:
                Map map = (Map) value;
                if (includeEmpty ||  include || map.size () > 0) {
                    serializeFieldName ( fieldName, builder );
                    serializer.serializeMap ( map, builder );
                    return true;
                }
                return false;
            case ARRAY:
                Object []  array  = (Object []) value;
                if (includeEmpty ||  include || array.length > 0) {
                    serializeFieldName ( fieldName, builder );
                    serializer.serializeArray ( value, builder );
                    return true;
                }
                return false;
            case INSTANCE:
                serializeFieldName ( fieldName, builder );
                serializer.serializeInstance ( value, builder );
                return true;
            default:
                serializeFieldName ( fieldName, builder );
                serializer.serializeUnknown ( value, builder );
                return true;

        }

    }
}
