package com.asg.hr.personaldatasheet.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrPersonalDataDependentId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}