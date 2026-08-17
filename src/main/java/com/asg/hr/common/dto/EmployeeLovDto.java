package com.asg.hr.common.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * The employee list a screen may show, narrowed to what the user is allowed to pick from.
 * <p>
 * A user who may select any employee gets the LOV list as the LOV endpoints return it; a restricted
 * user gets only their own employee, which is one lookup rather than the whole list.
 */
@Data
@Builder
public class EmployeeLovDto {

    /** LOV the list was read from, as the caller named it. */
    private String lovName;

    /** True when the list holds only the user's own employee because they may not pick another. */
    private boolean restrictedToOwnEmployee;

    /** Rows matching the request before paging; equals the size of {@link #data} when restricted. */
    private int totalRecords;

    /** The rows themselves, empty when a restricted user has no linked employee. */
    private List<LovGetListDto> data;
}
