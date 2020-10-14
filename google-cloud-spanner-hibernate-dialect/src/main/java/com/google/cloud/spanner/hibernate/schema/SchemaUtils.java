/*
 * Copyright 2019-2020 Google LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package com.google.cloud.spanner.hibernate.schema;

import com.google.cloud.spanner.hibernate.Interleaved;
import com.google.cloud.spanner.hibernate.reflection.SpannerEntityFieldKey;
import com.google.cloud.spanner.hibernate.reflection.SpannerKeyFieldIterator;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/**
 * Schema utilities for reading table {@link Metadata} in Hibernate.
 */
class SchemaUtils {

  private SchemaUtils() {
  }

  /**
   * Returns the {@link Interleaved} annotation on a table if it exists.
   */
  public static Interleaved getInterleaveAnnotation(Table table, Metadata metadata) {
    Class<?> entityClass = getEntityClass(table, metadata);
    return null != entityClass ? entityClass.getAnnotation(Interleaved.class) : null;
  }

  /**
   * Returns the bound entity class on a table if it exists.
   */
  public static Class<?> getEntityClass(Table table, Metadata metadata) {
    for (PersistentClass pc : metadata.getEntityBindings()) {
      if (pc.getTable().equals(table) && pc.getMappedClass() != null) {
        return pc.getMappedClass();
      }
    }

    return null;
  }

  /**
   * Gets the Spanner {@link Table} by entity class.
   */
  public static Table getTable(Class<?> entityClass, Metadata metadata) {
    PersistentClass pc = metadata.getEntityBinding(entityClass.getCanonicalName());
    if (pc != null) {
      return pc.getTable();
    }

    throw new IllegalArgumentException(
        String.format("Could not find table for entity class %s.", entityClass.getName()));
  }

  /**
   * Verifies that the composite key for an interleaved class is a super set of
   * the parent class primary key. Assumes all tables are being verified, so does
   * not recurse.
   */
  public static boolean validateInterleaved(Class<?> potentialChild) {
    Interleaved interleaved = potentialChild.getAnnotation(Interleaved.class);

    if (null == interleaved) {
      // not interleaved, we're good
      return true;
    }

    try {
      Set<SpannerEntityFieldKey> childIds = resolveIdFields(potentialChild);
      Set<SpannerEntityFieldKey> parentIds = resolveIdFields(interleaved.parentEntity());

      // Child ids should be super set of parent ids
      return childIds.size() > parentIds.size() && childIds.containsAll(parentIds);

    } catch (SecurityException se) {
      // Could not prove the interleaved table to be invalid, so assume valid
      return true;
    }
  }

  /**
   * Resolve the fields that make up the composite key.
   */
  public static Set<SpannerEntityFieldKey> resolveIdFields(Class<?> entity)
      throws SecurityException {
    Field embeddedId = getEmbeddedId(entity);

    Class<?> compositeKeyClazz = embeddedId != null ? embeddedId.getType() : entity;
    boolean keepAllFields = embeddedId != null;

    Set<SpannerEntityFieldKey> ids = new HashSet<>();
    for (Field field: SpannerKeyFieldIterator.iterable(compositeKeyClazz)) {
      if (keepAllFields || null != field.getAnnotation(Id.class)) {
        ids.add(new SpannerEntityFieldKey(field.getType(), field.getName()));
      }
    }

    return ids;
  }

  /**
   * Returns the field marked with the {@link EmbeddedId} annotation on a class if it exists.
   */
  public static Field getEmbeddedId(Class<?> entity) throws SecurityException {
    for (Field field : SpannerKeyFieldIterator.iterable(entity)) {
      if (null != field.getAnnotation(EmbeddedId.class)) {
        return field;
      }
    }

    return null;
  }
}
