package com.dream11.logcentralorchestrator.tests.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dream11.logcentralorchestrator.config.ApplicationConfig;
import com.dream11.logcentralorchestrator.config.ApplicationConfigProvider;
import com.dream11.logcentralorchestrator.enums.Tenant;
import com.dream11.logcentralorchestrator.util.ApplicationConfigUtil;
import java.util.ArrayList;
import java.util.List;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ApplicationConfigUtil. */
public class ApplicationConfigUtilTest {

  private ApplicationConfig mockConfig;

  @BeforeMethod
  public void setUp() {
    mockConfig = mock(ApplicationConfig.class);
  }

  @Test
  public void testGetTenantConfig_WithValidTenant_ReturnsTenantConfig() {
    // Arrange
    ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
    when(tenantConfig.getName()).thenReturn("D11-Prod-AWS");
    
    List<ApplicationConfig.TenantConfig> tenants = new ArrayList<>();
    tenants.add(tenantConfig);
    when(mockConfig.getTenants()).thenReturn(tenants);
    
    try (MockedStatic<ApplicationConfigProvider> mockedProvider = 
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig)
          .thenReturn(mockConfig);
      
      // Act
      ApplicationConfig.TenantConfig result = ApplicationConfigUtil.getTenantConfig(Tenant.D11_Prod_AWS);
      
      // Assert
      Assert.assertNotNull(result);
      Assert.assertEquals(result.getName(), "D11-Prod-AWS");
    }
  }

  @Test
  public void testGetTenantConfig_WithInvalidTenant_ReturnsNull() {
    // Arrange
    ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
    when(tenantConfig.getName()).thenReturn("D11-Prod-AWS");
    
    List<ApplicationConfig.TenantConfig> tenants = new ArrayList<>();
    tenants.add(tenantConfig);
    when(mockConfig.getTenants()).thenReturn(tenants);
    
    try (MockedStatic<ApplicationConfigProvider> mockedProvider = 
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig)
          .thenReturn(mockConfig);
      
      // Act
      ApplicationConfig.TenantConfig result = ApplicationConfigUtil.getTenantConfig(Tenant.D11_STAG_AWS);
      
      // Assert
      Assert.assertNull(result);
    }
  }

  @Test
  public void testGetTenantConfig_WithEmptyTenants_ReturnsNull() {
    // Arrange
    when(mockConfig.getTenants()).thenReturn(new ArrayList<>());
    
    try (MockedStatic<ApplicationConfigProvider> mockedProvider = 
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig)
          .thenReturn(mockConfig);
      
      // Act
      ApplicationConfig.TenantConfig result = ApplicationConfigUtil.getTenantConfig(Tenant.D11_Prod_AWS);
      
      // Assert
      Assert.assertNull(result);
    }
  }

  @Test
  public void testIsAwsObjectStore_WithAwsObjectStore_ReturnsTrue() {
    // Arrange
    ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
    ApplicationConfig.ObjectStoreConfig objectStoreConfig = mock(ApplicationConfig.ObjectStoreConfig.class);
    ApplicationConfig.S3Config s3Config = mock(ApplicationConfig.S3Config.class);
    
    when(tenantConfig.getObjectStore()).thenReturn(objectStoreConfig);
    when(objectStoreConfig.getAws()).thenReturn(s3Config);
    
    // Act
    boolean result = ApplicationConfigUtil.isAwsObjectStore(tenantConfig);
    
    // Assert
    Assert.assertTrue(result);
  }

  @Test
  public void testIsAwsObjectStore_WithNullAws_ReturnsFalse() {
    // Arrange
    ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
    ApplicationConfig.ObjectStoreConfig objectStoreConfig = mock(ApplicationConfig.ObjectStoreConfig.class);
    
    when(tenantConfig.getObjectStore()).thenReturn(objectStoreConfig);
    when(objectStoreConfig.getAws()).thenReturn(null);
    
    // Act
    boolean result = ApplicationConfigUtil.isAwsObjectStore(tenantConfig);
    
    // Assert
    Assert.assertFalse(result);
  }

  @Test
  public void testIsAwsObjectStore_WithNullObjectStore_ReturnsFalse() {
    // Arrange
    ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
    when(tenantConfig.getObjectStore()).thenReturn(null);
    
    // Act
    try {
      ApplicationConfigUtil.isAwsObjectStore(tenantConfig);
      Assert.fail("Should have thrown NullPointerException");
    } catch (NullPointerException e) {
      // Expected
    }
  }
}

