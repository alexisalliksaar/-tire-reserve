package com.alexisa.tire_reserve.service.workshop;

import com.alexisa.tire_reserve.config.WorkshopsProperties;
import com.alexisa.tire_reserve.exceptions.service.workshops.WsServiceCommunicationException;
import com.alexisa.tire_reserve.exceptions.service.workshops.response.WsServiceResponseException;
import com.alexisa.tire_reserve.model.domain.TireChangeTime;
import com.alexisa.tire_reserve.model.domain.BookTimeRequest;
import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import com.alexisa.tire_reserve.model.domain.error.WsServiceErrorDescription;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@ConditionalOnProperty(
        value = "workshops.map.london.workshop-id",
        havingValue = "LONDON"
)
public class LondonWsApiService implements WsApiServiceI {

    private final WorkshopsProperties.WorkshopProperties wsProps;
    private final RestClient restClient;
    private final WorkshopId workshopId;

    public LondonWsApiService(
            WorkshopsProperties workshopsProperties,
            @Autowired
            RestClient.Builder autoConfRestClientBuilder
    ) {
        this.wsProps = workshopsProperties.map().get("london");
        this.workshopId = wsProps.workshopId();
        restClient = createRestClient(autoConfRestClientBuilder.clone(), wsProps.apiPath());
    }

    @Override
    public WorkshopsProperties.WorkshopProperties getWorkshopProperties() {
        return wsProps;
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<TireChangeTime> getAllAvailableTireChangeTimes() throws WsServiceCommunicationException {

        LocalDate currentDateInUTC = LocalDate.now(ZoneId.of("UTC"));
        LocalDate nextYear = currentDateInUTC.plusYears(1);

        String fromDate = currentDateInUTC.format(DATE_TIME_FORMATTER);
        String toDate = nextYear.format(DATE_TIME_FORMATTER);

        LondonAvailableTimesResponse apiResponse;
        try {
            apiResponse = restClient.get()
                    .uri("/tire-change-times/available?from={from}&until={until}", fromDate, toDate)
                    .exchange((request, response) ->{
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return Objects.requireNonNull(response.bodyTo(LondonAvailableTimesResponse.class));
                        }

                        LondonErrorResponse errorResponse = response.bodyTo(LondonErrorResponse.class);
                        String exMessageTemplate = "London api responded to available tire change times " +
                                "request with code '%s' and message '%s'";
                        handleUnSuccessfulResponse(response.getStatusCode(), errorResponse, exMessageTemplate);
                        throw new AssertionError("Unreachable");
                    });
        } catch (WsServiceResponseException e) {
            throw new WsServiceCommunicationException(
                    "London api responded with error status code when requesting available times",
                    e,
                    workshopId
            );
        } catch (RuntimeException e){
            throw new WsServiceCommunicationException(
                    "General exception thrown when requesting available times from London api",
                    e,
                    workshopId
            );
        }

        return apiResponse.availableTimes().stream()
                .map(availableTime -> availableTime.modelFromDTO(workshopId))
                .toList();
    }

    private void handleUnSuccessfulResponse(
            HttpStatusCode status,
            LondonErrorResponse errorResponse,
            String exMessageTemplate
    ) throws WsServiceResponseException {
        WsServiceErrorDescription errorDescription = null;
        String exMessage = null;

        if (errorResponse != null) {
            errorDescription = new WsServiceErrorDescription(errorResponse.error(), String.valueOf(errorResponse.statusCode()));
            exMessage = String.format(exMessageTemplate,
                    errorDescription.code(),
                    errorDescription.errorMessage()
            );
        }

        throw new WsServiceResponseException(exMessage, errorDescription, status);
    }

    @Override
    public TireChangeTime bookDesiredTime(BookTimeRequest bookTimeRequest) throws WsServiceCommunicationException, WsServiceResponseException {
        LondonAvailableTime apiResponse;

        try {
            apiResponse = restClient.put()
                    .uri("/tire-change-times/{uuid}/booking", bookTimeRequest.id())
                    .contentType(MediaType.APPLICATION_XML)
                    .body(new LondonTireChangeBookingRequest(bookTimeRequest.contactInformation()))
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()){
                            return Objects.requireNonNull(response.bodyTo(LondonAvailableTime.class));
                        }
                        log.info("London api could not book tire change time request: {}", bookTimeRequest);
                        LondonErrorResponse errorResponse = response.bodyTo(LondonErrorResponse.class);
                        String exMessageTemplate = "London api responded to booking tire change time " +
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

    @JacksonXmlRootElement(localName = "london.tireChangeTimesResponse")
    private record LondonAvailableTimesResponse(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "availableTime")
            List<LondonAvailableTime> availableTimes
    ){}
    @JacksonXmlRootElement(localName = "london.tireChangeBookingResponse")
    private record LondonAvailableTime(
            @JacksonXmlProperty(localName = "uuid")
            String uuid,
            @JacksonXmlProperty(localName = "time")
            String time
    ){

        private TireChangeTime modelFromDTO(WorkshopId workshopId){
            return new TireChangeTime(workshopId, uuid, TireChangeTime.resolveInstant(time));
        }
    }

    @JacksonXmlRootElement(localName = "london.errorResponse")
    private record LondonErrorResponse(
            @JacksonXmlProperty(localName = "error")
            String error,
            @JacksonXmlProperty(localName = "statusCode")
            Integer statusCode
    ){}

    @JacksonXmlRootElement(localName = "london.tireChangeBookingRequest")
    private record LondonTireChangeBookingRequest(
            @JacksonXmlProperty(localName = "contactInformation")
            String contactInformation
    ){}
}
