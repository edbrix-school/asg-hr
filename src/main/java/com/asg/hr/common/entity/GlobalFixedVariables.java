package com.asg.hr.common.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GLOBAL_FIXED_VARIABLES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalFixedVariables {

    @Id
    @Column(name = "FXPOID")
    private Long fxpoid;

    @Column(name = "MODULE_CODE", length = 10)
    private String moduleCode;

    @Column(name = "VIEW_USING", length = 100)
    private String viewUsing;

    @Column(name = "SEQNO")
    private Long seqno;

    @Column(name = "VARIABLE_NAME", length = 100)
    private String variableName;

    @Column(name = "VAR_CATEGORY", length = 20)
    private String varCategory;

    @Column(name = "VARIBALE_ID", length = 20) // keeping DB typo as-is
    private String varibaleId;
}