package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

@Data
public class LinkedService implements Serializable {
    private String _org;
    private String name;
    @Id
    private String objectId;
    private String objectType;
}
