package com.dream11.logcentralorchestrator.tests.unit.rest;

import com.dream11.logcentralorchestrator.rest.request.Sorting;
import io.vertx.core.json.JsonObject;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for Sorting. */
public class SortingTest {

  @Test
  public void testOf_WithValidInputs_CreatesSorting() {
    // Act
    Sorting sorting = Sorting.of("name", "ASC");

    // Assert
    Assert.assertNotNull(sorting);
    Assert.assertEquals(sorting.getSortingKey(), "name");
    Assert.assertEquals(sorting.getSortingOrder(), Sorting.Order.ASC);
  }

  @Test
  public void testOf_WithDescOrder_CreatesSorting() {
    // Act
    Sorting sorting = Sorting.of("date", "DESC");

    // Assert
    Assert.assertEquals(sorting.getSortingKey(), "date");
    Assert.assertEquals(sorting.getSortingOrder(), Sorting.Order.DESC);
  }

  @Test
  public void testConstructor_Default_CreatesEmptySorting() {
    // Act
    Sorting sorting = new Sorting();

    // Assert
    Assert.assertNotNull(sorting);
    Assert.assertNull(sorting.getSortingKey());
    Assert.assertNull(sorting.getSortingOrder());
  }

  @Test
  public void testConstructor_WithJsonObject_CreatesSorting() {
    // Arrange
    JsonObject json = new JsonObject();
    json.put("sortingKey", "testKey");
    json.put("sortingOrder", "ASC");

    // Act
    Sorting sorting = new Sorting(json);

    // Assert
    Assert.assertEquals(sorting.getSortingKey(), "testKey");
    Assert.assertEquals(sorting.getSortingOrder(), Sorting.Order.ASC);
  }

  @Test
  public void testToJson_WithValidSorting_ReturnsJsonObject() {
    // Arrange
    Sorting sorting = Sorting.of("name", "ASC");

    // Act
    JsonObject json = sorting.toJson();

    // Assert
    Assert.assertNotNull(json);
    Assert.assertEquals(json.getString("sortingKey"), "name");
    Assert.assertEquals(json.getString("sortingOrder"), "ASC");
  }

  @Test
  public void testToString_WithValidSorting_ReturnsString() {
    // Arrange
    Sorting sorting = Sorting.of("name", "ASC");

    // Act
    String result = sorting.toString();

    // Assert
    Assert.assertEquals(result, "name ASC");
  }

  @Test
  public void testOrderEnum_Values() {
    // Assert
    Assert.assertNotNull(Sorting.Order.ASC);
    Assert.assertNotNull(Sorting.Order.DESC);
  }
}

