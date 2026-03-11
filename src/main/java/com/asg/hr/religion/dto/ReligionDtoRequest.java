package com.asg.hr.religion.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReligionDtoRequest {
    private String religionCode;
    private String description;
    private String active;
    private Long seqNo;

}
