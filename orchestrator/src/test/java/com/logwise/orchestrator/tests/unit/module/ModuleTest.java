package com.dream11.logcentralorchestrator.tests.unit.module;

import com.dream11.logcentralorchestrator.module.ClientModule;
import com.dream11.logcentralorchestrator.module.MainModule;
import com.dream11.logcentralorchestrator.setup.BaseTest;
import io.vertx.reactivex.core.Vertx;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ClientModule and MainModule. */
public class ModuleTest extends BaseTest {

  private Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx();
  }

  // ========== ClientModule Tests ==========

  @Test
  public void testClientModule_Constructor_CreatesInstance() {
    // Act
    ClientModule module = new ClientModule(vertx);

    // Assert
    Assert.assertNotNull(module);
  }

  @Test
  public void testClientModule_Instance_Created() {
    // Arrange & Act
    ClientModule module = new ClientModule(vertx);

    // Assert - bindConfiguration is protected, so we can't test it directly
    // But we can verify the module was created successfully
    Assert.assertNotNull(module);
  }

  // ========== MainModule Tests ==========

  @Test
  public void testMainModule_Constructor_CreatesInstance() {
    // Act
    MainModule module = new MainModule(vertx);

    // Assert
    Assert.assertNotNull(module);
  }
}
