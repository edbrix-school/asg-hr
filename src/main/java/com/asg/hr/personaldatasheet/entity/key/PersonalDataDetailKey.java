package com.asg.hr.personaldatasheet.entity.key;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalDataDetailKey implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}