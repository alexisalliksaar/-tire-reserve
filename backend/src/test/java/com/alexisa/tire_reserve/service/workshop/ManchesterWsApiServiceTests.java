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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;


@RestClientTest(ManchesterWsApiService.class)
@Import(ManchesterWsApiServiceTests.ManchesterWsApiServiceTestsContextConfiguration.class)
public class ManchesterWsApiServiceTests {

    private static final WorkshopsProperties WORKSHOPS_PROPERTIES = new WorkshopsProperties(
            Map.of("manchester", new WorkshopsProperties.WorkshopProperties(
                    WorkshopId.MANCHESTER,
                    "http://localhost:1111/api/v2/test",
                    "test",
                    "test",
                    "test",
                    "test",
                    List.of(ServiceableVehicle.CAR)
            ))
    );

    @TestComponent
    static class ManchesterWsApiServiceTestsContextConfiguration {

        @Bean
        public WorkshopsProperties workshopsProperties(){
            return WORKSHOPS_PROPERTIES;
        }
    }

    @Autowired
    private ManchesterWsApiService apiService;

    @Autowired
    MockRestServiceServer server;

    @Test
    public void testWorkshopProperties(){
        assertThat(apiService.getWorkshopProperties()).isEqualTo(WORKSHOPS_PROPERTIES.map().get("manchester"));
    }

    @Test
    public void testAllAvailableTimesSuccessful() throws WsServiceCommunicationException {

        String response = """
                [
                  {
                      "id": 3,
                      "time": "2024-08-07T07:00:00Z",
                      "available": true
                    },
                    {
                      "id": 4,
                      "time": "2024-08-07T08:00:00Z",
                      "available": false
                    },
                    {
                      "id": 5,
                      "time": "2024-08-07T09:00:00Z",
                      "available": true
                    }
                ]
                """;

        server.expect(requestTo("http://localhost:1111/api/v2/test/tire-change-times"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        List<TireChangeTime> availableTimes = apiService.getAllAvailableTireChangeTimes();

        server.verify();
        assertThat(availableTimes.size()).isEqualTo(2);
    }

    @Test
    public void testAllAvailableTimesInternalServerError(){
        String response = """
                {
                   "code": "10",
                   "message": "internal server error"
                 }
                """;
        server.expect(requestTo("http://localhost:1111/api/v2/test/tire-change-times"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError().body(response).contentType(MediaType.APPLICATION_JSON));

        WsServiceCommunicationException e = Assertions.assertThrows(
                WsServiceCommunicationException.class,
                () -> apiService.getAllAvailableTireChangeTimes()
        );

        server.verify();

        assertThat(e.getCausedBy()).isEqualTo(WorkshopId.MANCHESTER);
        WsServiceResponseException cause = (WsServiceResponseException) e.getCause();

        assertThat(cause.getServiceErrorResponse())
                .isEqualTo(new WsServiceErrorDescription("internal server error", "10"));
        assertThat(cause.getResponseStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testAllAvailableTimesConnectionError(){
        server.expect(requestTo("http://localhost:1111/api/v2/test/tire-change-times"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new ConnectException("Connection refused")));

        WsServiceCommunicationException e = Assertions.assertThrows(
                WsServiceCommunicationException.class,
                () -> apiService.getAllAvailableTireChangeTimes()
        );

        server.verify();

        assertThat(e.getCausedBy()).isEqualTo(WorkshopId.MANCHESTER);
        assertThat(e).hasRootCauseInstanceOf(ConnectException.class);
    }

    private static final String EXPECTED_BOOK_TIME_REQUEST_BODY = """
            {
              "contactInformation": "test"
            }
            """;

    private static final String BOOK_TIME_REQUEST_URI = String
            .format("http://localhost:1111/api/v2/test/tire-change-times/%s/booking", "1");

    private static final BookTimeRequest BOOK_TIME_REQUEST_PAR = new BookTimeRequest(
            "test",
            String.valueOf(1),
            WorkshopId.MANCHESTER
    );

    @Test
    public void testBookDesiredTimeSuccessful() throws WsServiceCommunicationException {

        String response = """
                {
                  "id": 1,
                  "time": "2024-08-12T05:00:00Z",
                  "available": false
                }
                """;

        server.expect(requestTo(BOOK_TIME_REQUEST_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(EXPECTED_BOOK_TIME_REQUEST_BODY))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));


        TireChangeTime result = apiService.bookDesiredTime(BOOK_TIME_REQUEST_PAR);

        server.verify();

        assertThat(result.getId()).isEqualTo("1");
    }

    @Test
    public void testBookDesiredTimeBadRequest(){

        String response = """
                {
                  "code": "11",
                  "message": "strconv.ParseUint: parsing \\"-1\\": invalid syntax"
                }
                """;

        String uri = String.format("http://localhost:1111/api/v2/test/tire-change-times/%s/booking", -1);

        server.expect(requestTo(uri))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(EXPECTED_BOOK_TIME_REQUEST_BODY))
                .andRespond(withBadRequest().body(response).contentType(MediaType.APPLICATION_JSON));

        BookTimeRequest bookTimeRequest = new BookTimeRequest(
                BOOK_TIME_REQUEST_PAR.contactInformation(),
                "-1",
                BOOK_TIME_REQUEST_PAR.workshopId()
        );

        WsServiceResponseException e = Assertions.assertThrows(
                WsServiceResponseException.class,
                () -> apiService.bookDesiredTime(bookTimeRequest)
        );

        server.verify();

        assertThat(e.getResponseStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(e.getServiceErrorResponse()).isEqualTo(
                new WsServiceErrorDescription("strconv.ParseUint: parsing \"-1\": invalid syntax", "11")
        );
    }

    @Test
    public void testBookDesiredTimeAlreadyBooked(){

        String response = """
                {
                  "code": "22",
                  "message": "tire change time 1 is unavailable"
                }
                """;

        server.expect(requestTo(BOOK_TIME_REQUEST_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(EXPECTED_BOOK_TIME_REQUEST_BODY))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body(response).contentType(MediaType.APPLICATION_JSON));

        WsServiceResponseException e = Assertions.assertThrows(
                WsServiceResponseException.class,
                () -> apiService.bookDesiredTime(BOOK_TIME_REQUEST_PAR)
        );

        server.verify();

        assertThat(e.getResponseStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(e.getServiceErrorResponse()).isEqualTo(
                new WsServiceErrorDescription("tire change time 1 is unavailable", "22")
        );
    }

    @Test
    public void testBookDesiredTimeInternalServerError(){

        String response = """
                {
                   "code": "10",
                   "message": "internal server error"
                }
                """;

        server.expect(requestTo(BOOK_TIME_REQUEST_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(EXPECTED_BOOK_TIME_REQUEST_BODY))
                .andRespond(withServerError().body(response).contentType(MediaType.APPLICATION_JSON));

        WsServiceResponseException e = Assertions.assertThrows(
                WsServiceResponseException.class,
                () -> apiService.bookDesiredTime(BOOK_TIME_REQUEST_PAR)
        );

        server.verify();

        assertThat(e.getResponseStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(e.getServiceErrorResponse()).isEqualTo(
                new WsServiceErrorDescription("internal server error", "10")
        );
    }

    @Test
    public void testBookDesiredTimeConnectionError(){

        server.expect(requestTo(BOOK_TIME_REQUEST_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(EXPECTED_BOOK_TIME_REQUEST_BODY))
                .andRespond(withException(new ConnectException("Connection refused")));

        WsServiceCommunicationException e = Assertions.assertThrows(
                WsServiceCommunicationException.class,
                () -> apiService.bookDesiredTime(BOOK_TIME_REQUEST_PAR)
        );

        server.verify();

        assertThat(e.getCausedBy()).isEqualTo(WorkshopId.MANCHESTER);
        assertThat(e).hasRootCauseInstanceOf(ConnectException.class);
    }
}
