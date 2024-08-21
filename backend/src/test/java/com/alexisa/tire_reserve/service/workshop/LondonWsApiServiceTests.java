package com.alexisa.tire_reserve.service.workshop;

import com.alexisa.tire_reserve.config.WorkshopsProperties;
import com.alexisa.tire_reserve.exceptions.service.workshops.WsServiceCommunicationException;
import com.alexisa.tire_reserve.exceptions.service.workshops.response.WsServiceResponseException;
import com.alexisa.tire_reserve.model.domain.BookTimeRequest;
import com.alexisa.tire_reserve.model.domain.TireChangeTime;
import com.alexisa.tire_reserve.model.domain.enums.ServiceableVehicle;
import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import com.alexisa.tire_reserve.model.domain.error.WsServiceErrorDescription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.ConnectException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(LondonWsApiService.class)
@Import(LondonWsApiServiceTests.LondonWsApiServiceTestsContextConfiguration.class)
public class LondonWsApiServiceTests {

    private static final WorkshopsProperties WORKSHOPS_PROPERTIES = new WorkshopsProperties(
            Map.of("london", new WorkshopsProperties.WorkshopProperties(
                    WorkshopId.LONDON,
                    "http://localhost:1111/api/test",
                    "test",
                    "test",
                    "test",
                    "test",
                    List.of(ServiceableVehicle.CAR)
            ))
    );

    @TestComponent
    static class LondonWsApiServiceTestsContextConfiguration {

        @Bean
        public WorkshopsProperties workshopsProperties(){
            return WORKSHOPS_PROPERTIES;
        }
    }

    @Autowired
    private LondonWsApiService apiService;

    @Autowired
    MockRestServiceServer server;

    @Test
    public void testWorkshopPropertiesTest(){
        assertThat(apiService.getWorkshopProperties()).isEqualTo(WORKSHOPS_PROPERTIES.map().get("london"));
    }

    private void assertSameUriWithoutQueryParams(URI actual) {
        Assertions.assertTrue(actual.toString().startsWith("http://localhost:1111/api/test/tire-change-times/available"));
    }

    @Test
    public void testAllAvailableTimesSuccessful() throws WsServiceCommunicationException {

        String response = """
                <tireChangeTimesResponse>
                  <availableTime>
                    <uuid>adb6d5c2-9c0f-41e6-ae3c-edceea03a583</uuid>
                    <time>2024-10-23T06:00:00Z</time>
                  </availableTime>
                  <availableTime>
                    <uuid>813b6adc-11e2-46db-940b-81c747bea9d6</uuid>
                    <time>2024-10-23T07:00:00Z</time>
                  </availableTime>
                </tireChangeTimesResponse>
                """;

        server.expect(request ->
                        assertSameUriWithoutQueryParams(
                                request.getURI()
                        )
                ).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(response, MediaType.APPLICATION_XML));

        List<TireChangeTime> availableTimes = apiService.getAllAvailableTireChangeTimes();

        server.verify();
        assertThat(availableTimes.size()).isEqualTo(2);
    }

    @Test
    public void testAllAvailableTimesInternalServerError(){
        String response = """
                <london.errorResponse>
                  	<error>internal server error</error>
                  	<statusCode>500</statusCode>
                </london.errorResponse>
                """;
        server.expect(request ->
                assertSameUriWithoutQueryParams(
                        request.getURI()
                )
                ).andExpect(method(HttpMethod.GET))
                .andRespond(withServerError().body(response).contentType(MediaType.APPLICATION_XML));

        WsServiceCommunicationException e = Assertions.assertThrows(
                WsServiceCommunicationException.class,
                () -> apiService.getAllAvailableTireChangeTimes()
        );

        server.verify();

        assertThat(e.getCausedBy()).isEqualTo(WorkshopId.LONDON);
        WsServiceResponseException cause = (WsServiceResponseException) e.getCause();

        assertThat(cause.getServiceErrorResponse())
                .isEqualTo(new WsServiceErrorDescription("internal server error", "500"));
        assertThat(cause.getResponseStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testAllAvailableTimesConnectionError(){
        server.expect(request ->
                        assertSameUriWithoutQueryParams(
                                request.getURI()
                        )
                ).andExpect(method(HttpMethod.GET))
                .andRespond(withException(new ConnectException("Connection refused")));

        WsServiceCommunicationException e = Assertions.assertThrows(
                WsServiceCommunicationException.class,
                () -> apiService.getAllAvailableTireChangeTimes()
        );

        server.verify();

        assertThat(e.getCausedBy()).isEqualTo(WorkshopId.LONDON);
        assertThat(e).hasRootCauseInstanceOf(ConnectException.class);
    }

    private static final BookTimeRequest BOOK_TIME_REQUEST_PAR = new BookTimeRequest(
            "test",
            "813b6adc-11e2-46db-940b-81c747bea9d6",
            WorkshopId.LONDON
    );

    private static final String BOOK_TIME_REQUEST_URI = String
            .format("http://localhost:1111/api/test/tire-change-times/%s/booking", BOOK_TIME_REQUEST_PAR.id());

    private static final String EXPECTED_BOOK_TIME_REQUEST_BODY = """
            <london.tireChangeBookingRequest>
            	<contactInformation>test</contactInformation>
            </london.tireChangeBookingRequest>
            """;


    @Test
    public void testBookDesiredTimeSuccessful() throws WsServiceCommunicationException {
        String response = """
                <tireChangeBookingResponse>
                  <uuid>813b6adc-11e2-46db-940b-81c747bea9d6</uuid>
                  <time>2024-10-23T06:00:00Z</time>
                </tireChangeBookingResponse>
                """;

        server.expect(requestTo(BOOK_TIME_REQUEST_URI))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(content().xml(EXPECTED_BOOK_TIME_REQUEST_BODY))
                .andRespond(withSuccess(response, MediaType.APPLICATION_XML));

        TireChangeTime result = apiService.bookDesiredTime(BOOK_TIME_REQUEST_PAR);

        server.verify();

        assertThat(result.getId()).isEqualTo("813b6adc-11e2-46db-940b-81c747bea9d6");
    }

    @Test
    public void testBookDesiredTimeBadRequest(){

        String response = """
                <errorResponse>
                   <statusCode>400</statusCode>
                   <error>Key: 'tireChangeBookingURI.UUID' Error:Field validation for 'UUID' failed on the 'min' tag</error>
                 </errorResponse>
                """;

        String invalidId = "a";
        String uri = String.format("http://localhost:1111/api/test/tire-change-times/%s/booking", invalidId);

        server.expect(requestTo(uri))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(content().xml(EXPECTED_BOOK_TIME_REQUEST_BODY))
                .andRespond(withBadRequest().body(response).contentType(MediaType.APPLICATION_XML));

        BookTimeRequest bookTimeRequest = new BookTimeRequest(
                BOOK_TIME_REQUEST_PAR.contactInformation(),
                invalidId,
                BOOK_TIME_REQUEST_PAR.workshopId()
        );

        WsServiceResponseException e = Assertions.assertThrows(
                WsServiceResponseException.class,
                () -> apiService.bookDesiredTime(bookTimeRequest)
        );

        server.verify();

        assertThat(e.getResponseStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(e.getServiceErrorResponse()).isEqualTo(
                new WsServiceErrorDescription(
                        "Key: 'tireChangeBookingURI.UUID' Error:Field validation for 'UUID' failed on the 'min' tag",
                        "400"
                )
        );
    }

    @Test
    public void testBookDesiredTimeAlreadyBooked(){

        String response = """
                <errorResponse>
                   <statusCode>422</statusCode>
                   <error>tire change time  is unavailable</error>
                 </errorResponse>
                """;

        server.expect(requestTo(BOOK_TIME_REQUEST_URI))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(content().xml(EXPECTED_BOOK_TIME_REQUEST_BODY))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body(response).contentType(MediaType.APPLICATION_XML));

        WsServiceResponseException e = Assertions.assertThrows(
                WsServiceResponseException.class,
                () -> apiService.bookDesiredTime(BOOK_TIME_REQUEST_PAR)
        );

        server.verify();

        assertThat(e.getResponseStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(e.getServiceErrorResponse()).isEqualTo(
                new WsServiceErrorDescription(
                        "tire change time  is unavailable",
                        "422"
                )
        );
    }

    @Test
    public void testBookDesiredTimeInternalServerError(){

        String response = """
                <errorResponse>
                   <statusCode>500</statusCode>
                   <error>Internal server error</error>
                 </errorResponse>
                """;

        server.expect(requestTo(BOOK_TIME_REQUEST_URI))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(content().xml(EXPECTED_BOOK_TIME_REQUEST_BODY))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body(response).contentType(MediaType.APPLICATION_XML));

        WsServiceResponseException e = Assertions.assertThrows(
                WsServiceResponseException.class,
                () -> apiService.bookDesiredTime(BOOK_TIME_REQUEST_PAR)
        );

        server.verify();

        assertThat(e.getResponseStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(e.getServiceErrorResponse()).isEqualTo(
                new WsServiceErrorDescription(
                        "Internal server error",
                        "500"
                )
        );
    }

    @Test
    public void testBookDesiredTimeConnectionError(){
        server.expect(requestTo(BOOK_TIME_REQUEST_URI))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(content().xml(EXPECTED_BOOK_TIME_REQUEST_BODY))
                .andRespond(withException(new ConnectException("Connection refused")));

        WsServiceCommunicationException e = Assertions.assertThrows(
                WsServiceCommunicationException.class,
                () -> apiService.bookDesiredTime(BOOK_TIME_REQUEST_PAR)
        );

        server.verify();

        assertThat(e.getCausedBy()).isEqualTo(WorkshopId.LONDON);
        assertThat(e).hasRootCauseInstanceOf(ConnectException.class);
    }

}
