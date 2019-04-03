/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;
import java.util.Date;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class DateWithTemporalTimestampTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DateEvent.class
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			DateEvent dateEvent = new DateEvent( new Date() );
			entityManager.persist( dateEvent );
		} );
	}

	@Entity(name = "DateEvent_date_with_temporal_timestamp")
	private static class DateEvent {

		@Id
		@GeneratedValue
		private Long id;

		//tag::basic-datetime-temporal-timestamp-example[]
		@Column(name = "`timestamp`")
		@Temporal(TemporalType.TIMESTAMP)
		private Date timestamp;
		//end::basic-datetime-temporal-timestamp-example[]

		public DateEvent() {
		}

		public DateEvent(Date timestamp) {
			this.timestamp = timestamp;
		}

		public Long getId() {
			return id;
		}

		public Date getTimestamp() {
			return timestamp;
		}
	}
}
