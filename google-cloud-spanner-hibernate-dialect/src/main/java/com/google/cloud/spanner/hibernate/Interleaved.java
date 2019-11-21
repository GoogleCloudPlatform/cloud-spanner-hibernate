/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.spanner.hibernate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.persistence.Entity;

/**
 * This annotation can be used to annotate an {@link Entity} class that should be interleaved in
 * a parent table. This annotation is Cloud Spanner specific and is only used when automatic schema
 * generation is used. If you create your schema manually, you may leave this annotation out.
 *
 * <p>To generate the following schema:
 *
 * <pre>
 * CREATE TABLE ParentTable (ParentId INT64, Name STRING(MAX)) PRIMARY KEY (ParentId);
 * CREATE TABLE ChildTable (ParentId INT64, ChildId INT64, ChildName STRING(MAX))
 *              PRIMARY KEY (ParentId, ChildId),
 *              INTERLEAVE IN PARENT ParentTable
 * </pre>
 *
 * <p>The following Java definition should be used:
 *
 * <pre>{@code
 * @Entity
 * @Table(name = "ParentTable")
 * public class Parent {
 *   @Id
 *   private Long parentId;
 *
 *   @Column
 *   private String name;
 *   ...
 * }
 *
 * @Entity
 * @Table(name = "ChildTable")
 * @Interleaved(parent = Parent.class)
 * public class Child {
 *   public static class ChildId implements Serializable {
 *     private Long parentId;
 *     private Long childId;
 *     ...
 *   }
 *
 *   @EmbeddedId
 *   private ChildId id;
 *
 *   @Column
 *   private String ChildName
 *   ...
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Interleaved {

  /**
   * The parent table that this table will be interleaved in.
   *
   * @return the entity class of the parent table
   */
  Class<?> parentEntity() default void.class;

  /**
   * Indicates whether when a row from the parent table is deleted that the child rows in this table
   * will automatically be deleted as well.
   *
   * @return <code>true</code> if ON DELETE CASCADE should be added to the CREATE TABLE string
   */
  boolean cascadeDelete() default false;
}
