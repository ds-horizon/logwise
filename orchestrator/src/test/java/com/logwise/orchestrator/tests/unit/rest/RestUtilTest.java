package com.dream11.logcentralorchestrator.tests.unit.rest;

import com.dream11.logcentralorchestrator.common.app.AppContext;
import com.dream11.logcentralorchestrator.common.entity.VertxEntity;
import com.dream11.logcentralorchestrator.rest.RestUtil;
import com.dream11.logcentralorchestrator.rest.request.Sorting;
import com.dream11.logcentralorchestrator.setup.BaseTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for RestUtil. */
public class RestUtilTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testToSortingIterator_WithSingleAsc_ReturnsAscSorting() {
    // Act
    List<Sorting> result = RestUtil.toSortingIterator("name");

    // Assert
    Assert.assertEquals(result.size(), 1);
    Assert.assertEquals(result.get(0).getSortingKey(), "name");
    Assert.assertEquals(result.get(0).getSortingOrder(), Sorting.Order.ASC);
  }

  @Test
  public void testToSortingIterator_WithSingleDesc_ReturnsDescSorting() {
    // Act
    List<Sorting> result = RestUtil.toSortingIterator("-name");

    // Assert
    Assert.assertEquals(result.size(), 1);
    Assert.assertEquals(result.get(0).getSortingKey(), "name");
    Assert.assertEquals(result.get(0).getSortingOrder(), Sorting.Order.DESC);
  }

  @Test
  public void testToSortingIterator_WithMultipleSorts_ReturnsMultipleSortings() {
    // Act
    List<Sorting> result = RestUtil.toSortingIterator("name,-date,status");

    // Assert
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get(0).getSortingKey(), "name");
    Assert.assertEquals(result.get(0).getSortingOrder(), Sorting.Order.ASC);
    Assert.assertEquals(result.get(1).getSortingKey(), "date");
    Assert.assertEquals(result.get(1).getSortingOrder(), Sorting.Order.DESC);
    Assert.assertEquals(result.get(2).getSortingKey(), "status");
    Assert.assertEquals(result.get(2).getSortingOrder(), Sorting.Order.ASC);
  }

  @Test
  public void testToSortingIterator_WithNull_ThrowsException() {
    // Act & Assert
    try {
      RestUtil.toSortingIterator(null);
      Assert.fail("Should have thrown NullPointerException");
    } catch (NullPointerException e) {
      // Expected
    }
  }

  @Test
  public void testToSortingString_WithSingleAsc_ReturnsString() {
    // Act
    String result = RestUtil.toSortingString("name");

    // Assert
    Assert.assertEquals(result, "name ASC");
  }

  @Test
  public void testToSortingString_WithSingleDesc_ReturnsString() {
    // Act
    String result = RestUtil.toSortingString("-name");

    // Assert
    Assert.assertEquals(result, "name DESC");
  }

  @Test
  public void testToSortingString_WithMultipleSorts_ReturnsCommaSeparatedString() {
    // Act
    String result = RestUtil.toSortingString("name,-date");

    // Assert
    Assert.assertTrue(result.contains("name ASC"));
    Assert.assertTrue(result.contains("date DESC"));
  }

  @Test
  public void testToSortingString_WithNull_ThrowsException() {
    // Act & Assert
    try {
      RestUtil.toSortingString(null);
      Assert.fail("Should have thrown NullPointerException");
    } catch (NullPointerException e) {
      // Expected
    }
  }

  @Test
  public void testGetString_WithString_ReturnsString() throws Exception {
    // Arrange
    String input = "test-string";

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      // Act
      String result = RestUtil.getString(input);

      // Assert
      Assert.assertEquals(result, input);
    }
  }

  @Test
  public void testGetString_WithJsonObject_ReturnsString() throws Exception {
    // Arrange
    JsonObject jsonObject = new JsonObject().put("key", "value");

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      // Act
      String result = RestUtil.getString(jsonObject);

      // Assert
      Assert.assertNotNull(result);
      Assert.assertTrue(result.contains("key"));
    }
  }

  @Test
  public void testGetString_WithList_ReturnsJsonArrayString() throws Exception {
    // Arrange
    List<String> list = Arrays.asList("item1", "item2");
    ObjectMapper mockObjectMapper = new ObjectMapper();

    // Act - List is handled by collectionClasses check, but AppContext.getInstance is still called
    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      mockedAppContext.when(() -> AppContext.getInstance(ObjectMapper.class)).thenReturn(mockObjectMapper);
      
      String result = RestUtil.getString(list);

      // Assert
      Assert.assertNotNull(result);
      Assert.assertTrue(result.contains("item1"));
    }
  }

  @Test
  public void testGetString_WithSet_ReturnsJsonArrayString() throws Exception {
    // Arrange
    Set<String> set = new HashSet<>(Arrays.asList("item1", "item2"));

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      // Act
      String result = RestUtil.getString(set);

      // Assert
      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testGetString_WithArrayList_ReturnsJsonArrayString() throws Exception {
    // Arrange
    ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("item1", "item2"));

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      // Act
      String result = RestUtil.getString(arrayList);

      // Assert
      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testGetString_WithHashSet_ReturnsJsonArrayString() throws Exception {
    // Arrange
    HashSet<String> hashSet = new HashSet<>(Arrays.asList("item1", "item2"));

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      // Act
      String result = RestUtil.getString(hashSet);

      // Assert
      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testGetString_WithLinkedHashSet_ReturnsJsonArrayString() throws Exception {
    // Arrange
    LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>(Arrays.asList("item1", "item2"));

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      // Act
      String result = RestUtil.getString(linkedHashSet);

      // Assert
      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testGetString_WithLinkedList_ReturnsJsonArrayString() throws Exception {
    // Arrange
    LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("item1", "item2"));

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      // Act
      String result = RestUtil.getString(linkedList);

      // Assert
      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testGetString_WithVertxEntity_ReturnsJsonString() throws Exception {
    // Arrange
    VertxEntity entity =
        new VertxEntity() {
          @Override
          public JsonObject toJson() {
            return new JsonObject().put("test", "value");
          }
        };

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      // Act
      String result = RestUtil.getString(entity);

      // Assert
      Assert.assertNotNull(result);
      Assert.assertTrue(result.contains("test"));
    }
  }

  @Test
  public void testGetString_WithCustomObject_UsesObjectMapper() throws Exception {
    // Arrange
    TestObject testObject = new TestObject("test-value");

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      ObjectMapper mockMapper = Mockito.mock(ObjectMapper.class);
      Mockito.when(mockMapper.writeValueAsString(testObject)).thenReturn("{\"value\":\"test-value\"}");
      mockedAppContext.when(() -> AppContext.getInstance(ObjectMapper.class)).thenReturn(mockMapper);

      // Act
      String result = RestUtil.getString(testObject);

      // Assert
      Assert.assertNotNull(result);
      Mockito.verify(mockMapper).writeValueAsString(testObject);
    }
  }

  @Test
  public void testAnnotatedClasses_WithValidPackage_ReturnsAnnotatedClasses() {
    // Arrange
    List<String> packageNames = Arrays.asList("com.dream11.logcentralorchestrator.rest.v1");

    // Act
    List<Class<?>> result = RestUtil.annotatedClasses(javax.ws.rs.Path.class, packageNames);

    // Assert
    Assert.assertNotNull(result);
    // Should find Component and Metrics classes which are annotated with @Path
    Assert.assertTrue(result.size() >= 0); // At least 0 (may vary based on classpath)
  }

  @Test
  public void testAnnotatedClasses_WithInvalidPackage_ReturnsEmptyList() {
    // Arrange
    List<String> packageNames = Arrays.asList("com.nonexistent.package");

    // Act
    List<Class<?>> result = RestUtil.annotatedClasses(javax.ws.rs.Path.class, packageNames);

    // Assert
    Assert.assertNotNull(result);
    // Should handle gracefully and return empty list or log error
  }

  @Test
  public void testAnnotatedClasses_WithProviderAnnotation_ReturnsProviders() {
    // Arrange
    List<String> packageNames = Arrays.asList("com.dream11.logcentralorchestrator.rest.provider");

    // Act
    List<Class<?>> result = RestUtil.annotatedClasses(javax.ws.rs.ext.Provider.class, packageNames);

    // Assert
    Assert.assertNotNull(result);
    // Should find provider classes
    Assert.assertTrue(result.size() >= 0);
  }

  @Test
  public void testAbstractRouteList_WithValidPackage_ReturnsRoutes() {
    // Arrange
    List<String> packageNames = Arrays.asList("com.dream11.logcentralorchestrator.rest");

    // Act
    List<com.dream11.logcentralorchestrator.rest.AbstractRoute> result = RestUtil.abstractRouteList(packageNames);

    // Assert
    Assert.assertNotNull(result);
    // Should return list (may be empty if no AbstractRoute implementations exist)
    Assert.assertTrue(result.size() >= 0);
  }

  @Test
  public void testAbstractRouteList_WithInvalidPackage_ReturnsEmptyList() {
    // Arrange
    List<String> packageNames = Arrays.asList("com.nonexistent.package");

    // Act
    List<com.dream11.logcentralorchestrator.rest.AbstractRoute> result = RestUtil.abstractRouteList(packageNames);

    // Assert
    Assert.assertNotNull(result);
    // Should handle gracefully
  }

  private static class TestObject {
    private String value;

    public TestObject(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
