/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.fit.proxy.v4;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.olingo.client.api.v4.EdmEnabledODataClient;
import org.apache.olingo.ext.proxy.Service;
import static org.apache.olingo.fit.proxy.v4.AbstractTestITCase.container;
//CHECKSTYLE:OFF (Maven checkstyle)
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.InMemoryEntities;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.Customer;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.Order;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.PersonCollection;
//CHECKSTYLE:ON (Maven checkstyle)
import org.junit.Test;

/**
 * This is the unit test class to check entity create operations.
 */
public class APIBasicDesignTestITCase extends AbstractTestITCase {

  protected Service<EdmEnabledODataClient> getContainerFactory() {
    return containerFactory;
  }

  protected InMemoryEntities getContainer() {
    return container;
  }

  @Test
  public void readAndCheckForPrimitive() {
    final Customer customer = container.getCustomers().getByKey(1);
    assertNotNull(customer);
    assertNull(customer.getPersonID());

    assertEquals(1, customer.load().getPersonID(), 0);
  }

  @Test
  public void readWholeEntitySet() {
    PersonCollection person = container.getPeople().execute();
    assertEquals(5, person.size(), 0);

    int pageCount = 1;
    while (person.hasNextPage()) {
      pageCount++;
      assertFalse(person.nextPage().execute().isEmpty());
    }

    assertEquals(2, pageCount);
  }

  @Test
  public void loadWithSelect() {
    org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.Order order =
            container.getOrders().getByKey(8);
    assertNull(order.getOrderID());
    assertNull(order.getOrderDate());

    order.select("OrderID");
    order.load();

    assertNull(order.getOrderDate());
    assertNotNull(order.getOrderID());

    order.clearQueryOptions();
    order.load();
    assertNotNull(order.getOrderDate());
    assertNotNull(order.getOrderID());
  }

  @Test
  public void loadWithSelectAndExpand() {
    final Customer customer = container.getCustomers().getByKey(1);

    customer.expand("Orders");
    customer.select("Orders", "PersonID");

    customer.load();
    assertEquals(1, customer.getOrders().size());
  }

  @Test
  public void createDelete() {
    // Create order ....
    final Order order = container.getOrders().newOrder();
    order.setOrderID(1105);

    final Calendar orderDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    orderDate.clear();
    orderDate.set(2011, 3, 4, 16, 3, 57);
    order.setOrderDate(new Timestamp(orderDate.getTimeInMillis()));

    order.setShelfLife(BigDecimal.ZERO);
    order.setOrderShelfLifes(Arrays.asList(new BigDecimal[] {BigDecimal.TEN.negate(), BigDecimal.TEN}));

    container.flush();

    Order actual = container.getOrders().getByKey(1105);
    assertNull(actual.getOrderID());

    actual.load();
    assertEquals(1105, actual.getOrderID(), 0);
    assertEquals(orderDate.getTimeInMillis(), actual.getOrderDate().getTime());
    assertEquals(BigDecimal.ZERO, actual.getShelfLife());
    assertEquals(2, actual.getOrderShelfLifes().size());

    containerFactory.getContext().detachAll();

    // Delete order ...
    container.getOrders().delete(container.getOrders().getByKey(1105));
    actual = container.getOrders().getByKey(1105);
    assertNull(actual);

    container.flush();

    containerFactory.getContext().detachAll();
    try {
      container.getOrders().getByKey(105).load();
      fail();
    } catch (IllegalArgumentException e) {
    }
  }
}