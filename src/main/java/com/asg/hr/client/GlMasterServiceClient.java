package com.asg.hr.client;

import com.asg.common.lib.client.GenericRestClient;
import com.asg.common.lib.dto.response.ApiResponseWrapper;
import com.asg.common.lib.dto.response.GLMasterResponseDto;
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
public class GlMasterServiceClient {

    private final GenericRestClient restClient;

    @Value("${finance.service.url:http://localhost:8086/finance/api/}")
    private String financeServiceUrl;

    public GLMasterResponseDto findById(Long costCenterPoid) {
        String url = financeServiceUrl + "v1/gl-master/" + costCenterPoid;
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Action-Requested", "VIEW");
        headers.add("X-Log-Enabled", "false");
        try {
            ApiResponseWrapper<GLMasterResponseDto> response = restClient.get(url, new ParameterizedTypeReference<ApiResponseWrapper<GLMasterResponseDto>>() {
            }, headers);
            return RestClientUtil.extractData(response);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Gl Master", "Gl poid", costCenterPoid);
        }
    }
}
