package com.dream11.logcentralorchestrator.constants;

import com.dream11.logcentralorchestrator.enums.ComponentType;
import com.dream11.logcentralorchestrator.enums.Tenant;
import lombok.experimental.UtilityClass;

/** Test constants for unit tests. */
@UtilityClass
public class TestConstants {
  public static final String VALID_TENANT_NAME = "D11-Prod-AWS";
  public static final Tenant VALID_TENANT = Tenant.D11_Prod_AWS;
  public static final String APPLICATION_COMPONENT_TYPE = ComponentType.APPLICATION.getValue();
  public static final Integer DEFAULT_DRIVER_CORES = 2;
  public static final Integer DEFAULT_DRIVER_MEMORY_GB = 12;
  public static final Integer DELAY_MINUTES = 5;
}
