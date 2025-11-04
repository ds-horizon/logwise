package com.logwise.dto.request;

import com.logwise.dto.entity.SparkStageHistory;
import lombok.Data;

@Data
public class ScaleSparkClusterRequest {
    private Boolean enableUpScale;
    private Boolean enableDownScale;
    private SparkStageHistory sparkStageHistory;
}