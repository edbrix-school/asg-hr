package com.asg.hr.client;

import com.asg.common.lib.client.GenericRestClient;
import com.asg.common.lib.dto.response.CostCenterResponseDto;
import com.asg.common.lib.dto.response.ApiResponseWrapper;
import com.asg.common.lib.utility.RestClientUtil;
import com.asg.hr.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
@RequiredArgsConstructor
public class CostCenterServiceClient {

    private final GenericRestClient restClient;

    @Value("${finance.service.url:http://localhost:8086/finance/api/}")
    private String financeServiceUrl;

    public CostCenterResponseDto findById(Long costCenterPoid) {
        String url = financeServiceUrl + "v1/cost-center/" + costCenterPoid;
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Action-Requested", "VIEW");
        try {
            ApiResponseWrapper<CostCenterResponseDto> response = restClient.get(url, new ParameterizedTypeReference<ApiResponseWrapper<CostCenterResponseDto>>() {
            }, headers);
            return RestClientUtil.extractData(response);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("CostCenter", "costCenterPoid", costCenterPoid);
        }
    }
}
