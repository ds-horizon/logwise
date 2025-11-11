package com.dream11.logcentralorchestrator.tests.unit;

import com.dream11.logcentralorchestrator.enums.ComponentType;
import com.dream11.logcentralorchestrator.error.ServiceError;
import com.dream11.logcentralorchestrator.rest.exception.RestException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for ComponentType enum. */
public class ComponentTypeTest {

  @Test
  public void testFromValue_WithValidComponentType_ReturnsComponentType() {
    Assert.assertEquals(ComponentType.fromValue("application"), ComponentType.APPLICATION);
  }

  @Test
  public void testGetValue_ReturnsCorrectValue() {
    Assert.assertEquals(ComponentType.APPLICATION.getValue(), "application");
  }

  @Test(expectedExceptions = RestException.class)
  public void testFromValue_WithInvalidComponentType_ThrowsRestException() {
    ComponentType.fromValue("invalid-type");
  }

  @Test(expectedExceptions = RestException.class)
  public void testFromValue_WithNullValue_ThrowsRestException() {
    ComponentType.fromValue(null);
  }

  @Test(expectedExceptions = RestException.class)
  public void testFromValue_WithEmptyString_ThrowsRestException() {
    ComponentType.fromValue("");
  }

  @Test
  public void testValues_ReturnsAllComponentTypes() {
    ComponentType[] componentTypes = ComponentType.values();
    Assert.assertEquals(componentTypes.length, 1);
    Assert.assertEquals(componentTypes[0], ComponentType.APPLICATION);
  }

  @Test
  public void testFromValue_WithCaseSensitiveMismatch_ThrowsRestException() {
    try {
      ComponentType.fromValue("Application");
      Assert.fail("Should have thrown RestException for case mismatch");
    } catch (RestException e) {
      Assert.assertNotNull(e.getError());
      Assert.assertEquals(e.getError().getCode(), ServiceError.INVALID_COMPONENT_TYPE.getErrorCode());
    }
  }
}

