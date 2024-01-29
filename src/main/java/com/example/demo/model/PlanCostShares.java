package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

@Data
public class PlanCostShares implements Serializable {
    private int deductible;
    private String _org;
    private int copay;
    @Id
    private String objectId;
    private String objectType;
}
