package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

@Data
public class LinkedPlanService implements Serializable {
    LinkedService linkedService;
    PlanCostShares planserviceCostShares;
    private String _org;
    @Id
    private String objectId;
    private String objectType;
}
