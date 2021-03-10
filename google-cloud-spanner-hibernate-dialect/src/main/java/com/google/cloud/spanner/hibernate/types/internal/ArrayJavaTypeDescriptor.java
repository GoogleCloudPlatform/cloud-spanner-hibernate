package com.google.cloud.spanner.hibernate.types.internal;

import com.google.cloud.spanner.hibernate.reflection.ReflectionUtils;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.usertype.DynamicParameterizedType;

public class ArrayJavaTypeDescriptor
    extends AbstractTypeDescriptor<List<?>>
    implements DynamicParameterizedType {

  public static final ArrayJavaTypeDescriptor INSTANCE = new ArrayJavaTypeDescriptor();

  // The type of the Spanner Array column.
  private String spannerTypeName = "";

  private Class<?> spannerType = Object.class;


  public ArrayJavaTypeDescriptor() {
    // This cast is needed to pass Object.class to the parent class
    super((Class<List<?>>)(Class<?>) List.class);
  }

  public String getSpannerTypeName() {
    return spannerTypeName;
  }

  @Override
  public List<?> fromString(String string) {
    return null;
  }

  @Override
  public <X> X unwrap(List<?> value, Class<X> type, WrapperOptions options) {
    // The generic type of value is provided by the spannerType.
    if (spannerType == Integer.class) {
      // If the value is a List<Integer>, convert it to List<Long> since Spanner only support INT64.
      value = ((List<Integer>) value).stream()
          .map(Integer::longValue)
          .collect(Collectors.toList());
    }

    return (X) value.toArray();
  }

  @Override
  public List<?> wrap(Object value, WrapperOptions options) {
    try {
      if (value instanceof Array) {
        Array sqlArray = (Array) value;
        return Arrays.asList(((Object[]) sqlArray.getArray()));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to convert SQL array type to a Java list: ", e);
    }

    throw new UnsupportedOperationException(
        "Unsupported type to convert: " + value.getClass()
            + " Java type descriptor only supports converting SQL array types.");
  }

  @Override
  public void setParameterValues(Properties parameters) {
    // Throw error if type is used on a non-List field.
    if (!parameters.get(DynamicParameterizedType.RETURNED_CLASS).equals(List.class.getName())) {
      String message = String.format(
          "Found invalid type annotation on field: %s. "
              + "The SpannerArrayListType must be applied on a java.util.List entity field.",
          parameters.get(DynamicParameterizedType.PROPERTY));

      throw new IllegalArgumentException(message);
    }

    // Get the class and the field name.
    Class<?> entityClass =
        ReflectionUtils.getClassOrFail(parameters.getProperty(DynamicParameterizedType.ENTITY));
    String fieldName = parameters.getProperty(DynamicParameterizedType.PROPERTY);

    // Get the parameterized type of the List<T>
    List<Class<?>> parameterizedTypes =
        ReflectionUtils.getParameterizedTypes(entityClass, fieldName);
    if (parameterizedTypes.isEmpty()) {
      throw new IllegalArgumentException(
          "You must specify an explicit parameterized type for your List type; i.e. List<Integer>");
    }
    Class<?> listType = parameterizedTypes.get(0);

    // Get the Spanner type string for the Java list type.
    spannerType = listType;
    spannerTypeName = getSpannerTypeCode(listType);
  }

  /**
   * Maps a Java Class type to a Spanner Column type.
   *
   * The type codes can be found in Spanner documentation:
   * https://cloud.google.com/spanner/docs/data-types#allowable_types
   */
  private static String getSpannerTypeCode(Class<?> javaType) {
    if (Integer.class.isAssignableFrom(javaType)) {
      return "INT64";
    } else if (Long.class.isAssignableFrom(javaType)) {
      return "INT64";
    } else if (Double.class.isAssignableFrom(javaType)) {
      return "FLOAT64";
    } else if (String.class.isAssignableFrom(javaType)) {
      return "STRING";
    } else if (UUID.class.isAssignableFrom(javaType)) {
      return "STRING";
    } else if (Date.class.isAssignableFrom(javaType)) {
      return "TIMESTAMP";
    } else if (Boolean.class.isAssignableFrom(javaType)) {
      return "BOOL";
    } else if (BigDecimal.class.isAssignableFrom(javaType)) {
      return "NUMERIC";
    } else if (byte[].class.isAssignableFrom(javaType)) {
      return "BYTES";
    } else {
      throw new UnsupportedOperationException(
          "The " + javaType + " is not supported as a Spanner array type.");
    }
  }
}
