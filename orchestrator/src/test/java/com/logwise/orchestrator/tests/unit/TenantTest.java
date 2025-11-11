package com.dream11.logcentralorchestrator.tests.unit;

import com.dream11.logcentralorchestrator.enums.Tenant;
import com.dream11.logcentralorchestrator.error.ServiceError;
import com.dream11.logcentralorchestrator.rest.exception.RestException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for Tenant enum. */
public class TenantTest {

  @Test
  public void testFromValue_WithValidTenant_ReturnsTenant() {
    // Test all valid tenant values
    Assert.assertEquals(Tenant.fromValue("D11-Prod-AWS"), Tenant.D11_Prod_AWS);
    Assert.assertEquals(Tenant.fromValue("D11-Stag-AWS"), Tenant.D11_STAG_AWS);
    Assert.assertEquals(Tenant.fromValue("DP-Logs-AWS"), Tenant.DP_LOGS_AWS);
    Assert.assertEquals(Tenant.fromValue("Hulk-Prod-AWS"), Tenant.HULK_PROD_AWS);
    Assert.assertEquals(Tenant.fromValue("Delivr-AWS"), Tenant.DELIVR_AWS);
  }

  @Test
  public void testGetValue_ReturnsCorrectValue() {
    Assert.assertEquals(Tenant.D11_Prod_AWS.getValue(), "D11-Prod-AWS");
    Assert.assertEquals(Tenant.D11_STAG_AWS.getValue(), "D11-Stag-AWS");
    Assert.assertEquals(Tenant.DP_LOGS_AWS.getValue(), "DP-Logs-AWS");
    Assert.assertEquals(Tenant.HULK_PROD_AWS.getValue(), "Hulk-Prod-AWS");
    Assert.assertEquals(Tenant.DELIVR_AWS.getValue(), "Delivr-AWS");
  }

  @Test(expectedExceptions = RestException.class)
  public void testFromValue_WithInvalidTenant_ThrowsRestException() {
    Tenant.fromValue("Invalid-Tenant");
  }

  @Test(expectedExceptions = RestException.class)
  public void testFromValue_WithNullValue_ThrowsRestException() {
    Tenant.fromValue(null);
  }

  @Test(expectedExceptions = RestException.class)
  public void testFromValue_WithEmptyString_ThrowsRestException() {
    Tenant.fromValue("");
  }

  @Test
  public void testValues_ReturnsAllTenants() {
    Tenant[] tenants = Tenant.values();
    Assert.assertEquals(tenants.length, 5);
    Assert.assertEquals(tenants[0], Tenant.D11_Prod_AWS);
    Assert.assertEquals(tenants[1], Tenant.D11_STAG_AWS);
    Assert.assertEquals(tenants[2], Tenant.DP_LOGS_AWS);
    Assert.assertEquals(tenants[3], Tenant.HULK_PROD_AWS);
    Assert.assertEquals(tenants[4], Tenant.DELIVR_AWS);
  }

  @Test
  public void testFromValue_WithCaseSensitiveMismatch_ThrowsRestException() {
    try {
      Tenant.fromValue("d11-prod-aws");
      Assert.fail("Should have thrown RestException for case mismatch");
    } catch (RestException e) {
      Assert.assertNotNull(e.getError());
      Assert.assertEquals(e.getError().getCode(), ServiceError.INVALID_TENANT.getErrorCode());
    }
  }
}
