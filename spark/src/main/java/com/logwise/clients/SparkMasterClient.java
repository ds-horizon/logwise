package com.logwise.clients;

import com.logwise.dto.response.SparkMasterJsonResponse;
import feign.RequestLine;

public interface SparkMasterClient {
  @RequestLine("GET /json")
  SparkMasterJsonResponse json();
}
