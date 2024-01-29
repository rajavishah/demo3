package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;

@Data
public class Plan implements Serializable {
    private PlanCostShares planCostShares;
    private LinkedPlanService[] linkedPlanServices;
    private String _org;
    @Id
    @Indexed
    private String objectId;
    private String objectType;
    private String planType;
    private String creationDate;

}
