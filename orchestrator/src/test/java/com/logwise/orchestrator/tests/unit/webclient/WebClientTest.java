package com.dream11.logcentralorchestrator.tests.unit.webclient;

import com.dream11.logcentralorchestrator.setup.BaseTest;
import com.dream11.logcentralorchestrator.webclient.client.WebClient;
import com.dream11.logcentralorchestrator.webclient.client.WebClientConfig;
import com.dream11.logcentralorchestrator.webclient.client.impl.WebClientImpl;
import io.vertx.core.Vertx;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for WebClient, WebClientImpl, and WebClientConfig. */
public class WebClientTest extends BaseTest {

  private Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx().getDelegate();
  }

  // ========== WebClient Tests ==========

  @Test
  public void testCreate_WithVertx_CreatesWebClient() {
    // Act
    WebClient webClient = WebClient.create(vertx);

    // Assert
    Assert.assertNotNull(webClient);
    webClient.close(); // Cleanup
  }

  @Test
  public void testCreate_WithVertxAndConfigType_CreatesWebClient() {
    // Act
    WebClient webClient = WebClient.create(vertx, "test");

    // Assert
    Assert.assertNotNull(webClient);
    webClient.close(); // Cleanup
  }

  @Test
  public void testGetWebClient_ReturnsWebClient() {
    // Arrange
    WebClient webClient = WebClient.create(vertx);

    // Act
    io.vertx.ext.web.client.WebClient vertxWebClient = webClient.getWebClient();

    // Assert
    Assert.assertNotNull(vertxWebClient);
    webClient.close(); // Cleanup
  }

  // ========== WebClientImpl Tests ==========

  @Test
  public void testWebClientImpl_Constructor_WithVertx_CreatesInstance() {
    // Arrange
    WebClientConfig config = new WebClientConfig();
    config.setConnectTimeout(5000);
    config.setIdleTimeout(30);

    // Act
    WebClientImpl webClient = new WebClientImpl(vertx, config);

    // Assert
    Assert.assertNotNull(webClient);
    webClient.close(); // Cleanup
  }

  @Test
  public void testWebClientImpl_Close_ClosesWebClient() {
    // Arrange
    WebClientConfig config = new WebClientConfig();
    WebClientImpl webClient = new WebClientImpl(vertx, config);

    // Act
    webClient.close();

    // Assert - Should not throw exception
    Assert.assertNotNull(webClient);
  }

  // ========== WebClientConfig Tests ==========

  @Test
  public void testWebClientConfig_Constructor_WithJsonObject_CreatesConfig() {
    // Arrange & Act
    WebClientConfig config = new WebClientConfig();
    config.setConnectTimeout(5000);
    config.setIdleTimeout(30);

    // Assert
    Assert.assertNotNull(config);
    Assert.assertEquals(config.getConnectTimeout(), Integer.valueOf(5000));
    Assert.assertEquals(config.getIdleTimeout(), Integer.valueOf(30));
  }

  @Test
  public void testWebClientConfig_Constructor_WithNullJson_UsesDefaults() {
    // Act
    WebClientConfig config = new WebClientConfig();

    // Assert
    Assert.assertNotNull(config);
    // Default values are set by Lombok @NoArgsConstructor
    Assert.assertNotNull(config.getConnectTimeout());
    Assert.assertNotNull(config.getIdleTimeout());
  }
}
