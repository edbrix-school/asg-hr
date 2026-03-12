package com.asg.hr.religion.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.exceptions.ResourceAlreadyExistsException;
import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.religion.dto.ReligionDtoRequest;
import com.asg.hr.religion.dto.ReligionDtoResponse;
import com.asg.hr.religion.entity.HrReligionMaster;
import com.asg.hr.religion.repository.ReligionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReligionImplementation implements ReligionService {

	private final ReligionRepository repository;
	private final LoggingService loggingService;
	private final DocumentDeleteService documentDeleteService;
	private final DocumentSearchService documentSearchService;

	private static final String RELIGION_POID = "RELIGION_POID";

	@Override
	public ReligionDtoResponse getReligionById(Long religionPoid) {
		HrReligionMaster entity = repository.findById(religionPoid)
				.orElseThrow(() -> new RuntimeException("Religion not found"));
		ReligionDtoResponse response = new ReligionDtoResponse();
		response.setReligionPoid(entity.getReligionPoid());
		response.setReligionCode(entity.getReligionCode());
		response.setDescription(entity.getReligionDescription());
		response.setActive(entity.getActive());
		response.setCreatedBy(entity.getCreatedBy());
		response.setCreatedDate(entity.getCreatedDate());
		response.setLastModifiedBy(entity.getLastModifiedBy());
		response.setLastModifiedDate(entity.getLastModifiedDate());

		return response;
	}

	@Override
	public Long createReligion(ReligionDtoRequest religionDto) {

		validateReligionDto(religionDto, null);

		Long groupPoid = UserContext.getGroupPoid();

		HrReligionMaster entity = new HrReligionMaster();

		entity.setGroupPoid(groupPoid);
		entity.setReligionCode(religionDto.getReligionCode());
		entity.setReligionDescription(religionDto.getDescription());
		entity.setSeqNo(religionDto.getSeqNo());
		entity.setActive(religionDto.getActive());
		entity.setDeleted("N");

		repository.save(entity);

		log.info("Created Religion with ID: {}", entity.getReligionPoid());

		// Log the creation
		String key = entity.getReligionPoid().toString();
		loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), key);

		return entity.getReligionPoid();

	}

	@Override
	public Long updateReligion(ReligionDtoRequest religionDto, Long religionPoid) {

		HrReligionMaster religion = repository.findByReligionPoidDeleted(religionPoid)
				.orElseThrow(() -> new ResourceNotFoundException("Religion", "religionPoid", religionPoid));

		validateReligionDto(religionDto, religionPoid);

		// Create a copy of the existing entity for logging
		HrReligionMaster oldEntity = new HrReligionMaster();
		BeanUtils.copyProperties(religion, oldEntity);

		religion.setReligionCode(religionDto.getReligionCode());
		religion.setReligionDescription(religionDto.getDescription());
		religion.setSeqNo(religionDto.getSeqNo());
		religion.setActive(religionDto.getActive());

		HrReligionMaster updated = repository.save(religion);
		log.info("Updated Religion with ID: {}", updated.getReligionPoid());

		// Log the update with changes
		String key = updated.getReligionPoid().toString();
		loggingService.logChanges(oldEntity, updated, HrReligionMaster.class, UserContext.getDocumentId(), key,
				LogDetailsEnum.MODIFIED, RELIGION_POID);

		return updated.getReligionPoid();

	}

	@Override
	public void deleteReligion(Long religionPoid, DeleteReasonDto deleteReasonDto) {
		repository.findByReligionPoidDeleted(religionPoid)
				.orElseThrow(() -> new ResourceNotFoundException("Religion", "religionPoid", religionPoid));

		// Use DocumentDeleteService for deletion (handles logging internally)
		documentDeleteService.deleteDocument(religionPoid, "HR_RELIGION_MASTER", RELIGION_POID, deleteReasonDto, null);

		log.info("Soft deleted Religion with ID: {}", religionPoid);
	}

	@Override
	public Map<String, Object> listReligion(FilterRequestDto filterRequest, Pageable pageable) {
		String operator = documentSearchService.resolveOperator(filterRequest);
		String isDeleted = documentSearchService.resolveIsDeleted(filterRequest);
		List<FilterDto> filterList = documentSearchService.resolveFilters(filterRequest);

		RawSearchResult raw = documentSearchService.search(UserContext.getDocumentId(), filterList, operator, pageable,
				isDeleted, "RELIGION_DESCRIPTION", RELIGION_POID);

		Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
		return PaginationUtil.wrapPage(page, raw.displayFields());
	}

	private void checkDuplicate(Optional<HrReligionMaster> entity, Long religionPoid, String message) {
		entity.filter(r -> !Objects.equals(r.getReligionPoid(), religionPoid)).ifPresent(r -> {
			throw new ResourceAlreadyExistsException(message);
		});
	}

	private void validateRequired(String value, String message) {
		if (value == null || value.trim().isEmpty()) {
			throw new ValidationException(message);
		}
	}

	private void validateReligionDto(ReligionDtoRequest religionDto, Long religionPoid) {

		validateRequired(religionDto.getReligionCode(), "Religion Code is required");
		validateRequired(religionDto.getDescription(), "Religion Description is required");

		checkDuplicate(repository.findByReligionCode(religionDto.getReligionCode()), religionPoid,
				"Religion Code already exists");

		checkDuplicate(repository.findByReligionDescription(religionDto.getDescription()), religionPoid,
				"Religion Description already exists");
	}

}
