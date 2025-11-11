package com.dream11.logcentralorchestrator.tests.unit.common;

import com.dream11.logcentralorchestrator.common.util.MaintenanceUtils;
import com.dream11.logcentralorchestrator.setup.BaseTest;
import io.vertx.reactivex.core.Vertx;
import java.util.concurrent.atomic.AtomicBoolean;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for MaintenanceUtils. */
public class MaintenanceUtilsTest extends BaseTest {

  private Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx();
    // Clear maintenance state before each test to avoid shared state issues
    MaintenanceUtils.clearMaintenance(vertx);
  }

  @Test
  public void testSetMaintenance_SetsMaintenanceFlag() {
    // Act
    MaintenanceUtils.setMaintenance(vertx);

    // Assert
    AtomicBoolean isUnderMaintenance = MaintenanceUtils.isUnderMaintenance(vertx);
    Assert.assertTrue(isUnderMaintenance.get());
  }

  @Test
  public void testClearMaintenance_ClearsMaintenanceFlag() {
    // Arrange
    MaintenanceUtils.setMaintenance(vertx);

    // Act
    MaintenanceUtils.clearMaintenance(vertx);

    // Assert
    AtomicBoolean isUnderMaintenance = MaintenanceUtils.isUnderMaintenance(vertx);
    Assert.assertFalse(isUnderMaintenance.get());
  }

  @Test
  public void testIsUnderMaintenance_Initially_ReturnsFalse() {
    // Act
    AtomicBoolean isUnderMaintenance = MaintenanceUtils.isUnderMaintenance(vertx);

    // Assert
    Assert.assertNotNull(isUnderMaintenance);
    Assert.assertFalse(isUnderMaintenance.get());
  }

  @Test
  public void testIsUnderMaintenance_AfterSetMaintenance_ReturnsTrue() {
    // Arrange
    MaintenanceUtils.setMaintenance(vertx);

    // Act
    AtomicBoolean isUnderMaintenance = MaintenanceUtils.isUnderMaintenance(vertx);

    // Assert
    Assert.assertTrue(isUnderMaintenance.get());
  }

  @Test
  public void testSetMaintenance_ThenClearMaintenance_ReturnsFalse() {
    // Arrange
    MaintenanceUtils.setMaintenance(vertx);
    Assert.assertTrue(MaintenanceUtils.isUnderMaintenance(vertx).get());

    // Act
    MaintenanceUtils.clearMaintenance(vertx);

    // Assert
    Assert.assertFalse(MaintenanceUtils.isUnderMaintenance(vertx).get());
  }
}

