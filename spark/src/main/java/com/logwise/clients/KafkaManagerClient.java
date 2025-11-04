package com.logwise.clients;

import com.logwise.dto.response.TopicIdentitiesResponse;
import feign.Param;
import feign.RequestLine;

public interface KafkaManagerClient {

    @RequestLine("GET /api/status/{clusterName}/topicIdentities")
    TopicIdentitiesResponse topicIdentities(@Param("clusterName") String clusterName);
}