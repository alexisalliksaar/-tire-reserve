package com.alexisa.tire_reserve.service.workshop;

import com.alexisa.tire_reserve.config.WorkshopsProperties;
import com.alexisa.tire_reserve.exceptions.service.workshops.WsServiceCommunicationException;
import com.alexisa.tire_reserve.exceptions.service.workshops.response.WsServiceResponseException;
import com.alexisa.tire_reserve.model.domain.TireChangeTime;
import com.alexisa.tire_reserve.model.domain.BookTimeRequest;
import com.alexisa.tire_reserve.model.domain.error.WsServiceErrorDescription;
import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

@Service
@ConditionalOnProperty(
        value = "workshops.map.manchester.workshop-id",
        havingValue = "MANCHESTER"
)
@Slf4j
public class ManchesterWsApiService implements WsApiServiceI {

    private final WorkshopsProperties.WorkshopProperties wsProps;
    private final RestClient restClient;
    private final WorkshopId workshopId;

    public ManchesterWsApiService(
            WorkshopsProperties workshopsProperties,
            @Autowired
            RestClient.Builder autoConfRestClientBuilder
    ) {
        this.wsProps = workshopsProperties.map().get("manchester");
        workshopId = this.wsProps.workshopId();
        restClient = createRestClient(autoConfRestClientBuilder.clone(), wsProps.apiPath());
    }


    @Override
    public WorkshopsProperties.WorkshopProperties getWorkshopProperties() {
        return wsProps;
    }

    @Override
    public List<TireChangeTime> getAllAvailableTireChangeTimes() throws WsServiceCommunicationException {

        List<ManchesterTireChangeTime> apiResponse;
        try {
            apiResponse = restClient.get()
                    .uri("/tire-change-times")
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()){
                            ParameterizedTypeReference<List<ManchesterTireChangeTime>> typeRef = new ParameterizedTypeReference<>() {};
                            return Objects.requireNonNullElse(response.bodyTo(typeRef), List.of());
                        }

                        ManchesterErrorResponse errorResponse = response.bodyTo(ManchesterErrorResponse.class);
                        String exMessageTemplate = "Manchester api responded to available tire change times " +
                                "request with code '%s' and message '%s'";
                        handleUnSuccessfulResponse(response.getStatusCode(), errorResponse, exMessageTemplate);
                        throw new AssertionError("Unreachable");
                    });
        } catch (WsServiceResponseException e) {
            throw new WsServiceCommunicationException(
                    "Manchester api responded with error status code when requesting available times",
                    e,
                    workshopId
            );
        } catch (RuntimeException e) {
            throw new WsServiceCommunicationException(
                    "General exception thrown when requesting available times from Manchester api",
                    e,
                    workshopId
            );
        }

        return apiResponse.stream()
                .filter(ManchesterTireChangeTime::available)
                .map(manchesterAvailableTime -> manchesterAvailableTime.modelFromDTO(workshopId))
                .toList();
    }

    private void handleUnSuccessfulResponse(
            HttpStatusCode status,
            ManchesterErrorResponse errorResponse,
            String exMessageTemplate
    ) throws WsServiceResponseException {

        WsServiceErrorDescription errorDescription = null;
        String exMessage = null;

        if (errorResponse != null) {
            errorDescription = new WsServiceErrorDescription(errorResponse.message(), errorResponse.code());
            exMessage = String.format(exMessageTemplate,
                    errorDescription.code(),
                    errorDescription.errorMessage()
            );
        }

        throw new WsServiceResponseException(exMessage, errorDescription, status);
    }


    @Override
    public TireChangeTime bookDesiredTime(BookTimeRequest bookTimeRequest)
            throws WsServiceCommunicationException,
            WsServiceResponseException
    {
        ManchesterTireChangeTime apiResponse;

        try {
            apiResponse = restClient.post()
                    .uri("/tire-change-times/{id}/booking", bookTimeRequest.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ManchesterBookRequest(bookTimeRequest.contactInformation()))
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()){
                            return Objects.requireNonNull(response.bodyTo(ManchesterTireChangeTime.class));
                        }
                        log.info("Manchester api could not book tire change time request: {}", bookTimeRequest);
                        ManchesterErrorResponse errorResponse = response.bodyTo(ManchesterErrorResponse.class);

                        String exMessageTemplate = "Manchester api responded to booking tire change time " +
                                "request with code '%s' and message '%s'";
                        handleUnSuccessfulResponse(response.getStatusCode(), errorResponse, exMessageTemplate);

                        throw new AssertionError("Unreachable");
                        });
        } catch (WsServiceResponseException e){
            throw e;
        } catch (RuntimeException e) {
            throw new WsServiceCommunicationException(
                    "General exception thrown when trying to book time for Manchester api",
                    e,
                    workshopId
            );
        }

        return apiResponse.modelFromDTO(workshopId);
    }

    private RestClient createRestClient(RestClient.Builder restClientBuilder, @NotBlank String apiPath) {
        restClientBuilder.baseUrl(apiPath);
        return restClientBuilder.build();
    }

    private record ManchesterTireChangeTime(
            int id,
            String time,
            boolean available
    ){
        private TireChangeTime modelFromDTO(WorkshopId workshopId) {
            return new TireChangeTime(workshopId, String.valueOf(id), TireChangeTime.resolveInstant(time));
        }
    }

    private record ManchesterBookRequest(String contactInformation){}

    private record ManchesterErrorResponse(
            String code,
            String message
    ){}
}
