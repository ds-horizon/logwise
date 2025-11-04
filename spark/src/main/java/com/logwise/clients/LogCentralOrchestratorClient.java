package com.logwise.clients;

import com.logwise.dto.request.ScaleSparkClusterRequest;
import com.logwise.dto.response.GetSparkStageHistoryResponse;
import feign.HeaderMap;
import feign.QueryMap;
import feign.RequestLine;
import java.util.Map;

public interface LogCentralOrchestratorClient {
  @RequestLine("POST /scale-spark-cluster")
  Map<String, Object> postScaleSparkCluster(
      @HeaderMap Map<String, String> headers, ScaleSparkClusterRequest request);

  @RequestLine("GET /spark-stage-history")
  GetSparkStageHistoryResponse getSparkStageHistory(
      @HeaderMap Map<String, String> headers, @QueryMap Map<String, String> queryMap);
}
