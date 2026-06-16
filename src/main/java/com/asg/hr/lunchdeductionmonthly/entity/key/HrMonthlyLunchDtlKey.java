package com.asg.hr.lunchdeductionmonthly.entity.key;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrMonthlyLunchDtlKey implements Serializable {
    private Long detRowId;
    private Long transactionPoid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HrMonthlyLunchDtlKey that = (HrMonthlyLunchDtlKey) o;
        return Objects.equals(detRowId, that.detRowId) &&
               Objects.equals(transactionPoid, that.transactionPoid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detRowId, transactionPoid);
    }
}
