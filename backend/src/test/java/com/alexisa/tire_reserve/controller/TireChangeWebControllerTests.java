package com.alexisa.tire_reserve.controller;

import com.alexisa.tire_reserve.config.WorkshopsProperties;
import com.alexisa.tire_reserve.exceptions.service.workshops.WsServiceCommunicationException;
import com.alexisa.tire_reserve.exceptions.service.workshops.response.WsServiceResponseException;
import com.alexisa.tire_reserve.model.domain.AvailableTimesResponse;
import com.alexisa.tire_reserve.model.domain.BookTimeRequest;
import com.alexisa.tire_reserve.model.domain.TireChangeTime;
import com.alexisa.tire_reserve.model.domain.TireChangeTimesFilter;
import com.alexisa.tire_reserve.model.domain.enums.ServiceableVehicle;
import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import com.alexisa.tire_reserve.model.domain.error.WsServiceErrorDescription;
import com.alexisa.tire_reserve.model.dto.AvailableTireTimeDTO;
import com.alexisa.tire_reserve.model.dto.AvailableTireTimesDTO;
import com.alexisa.tire_reserve.model.dto.workshop.WorkshopsRequestDTO;
import com.alexisa.tire_reserve.service.workshop.WorkshopsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.Instant;
import java.util.List;

import static com.alexisa.tire_reserve.controller.TireChangeWebControllerTests.ResponseBodyMatchers.responseBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TireChangeWebController.class)
public class TireChangeWebControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private WorkshopsService workshopsService;

    @Test
    public void testGetWorkshopsInformation() throws Exception {
        WorkshopsProperties.WorkshopProperties workshopProperties = new WorkshopsProperties.WorkshopProperties(
                WorkshopId.MANCHESTER,
                "testApiPath",
                "testCity",
                "testAddress",
                "testPhoneNr",
                "testEmail",
                List.of(ServiceableVehicle.CAR)
        );

        when(workshopsService.getWorkshops())
                .thenReturn(List.of(workshopProperties));

        String expectedJson = """
                {
                    "workshops": [
                        {
                            "workshopId": "MANCHESTER",
                            "city": "testCity",
                            "address": "testAddress",
                            "email": "testEmail",
                            "phoneNumber": "testPhoneNr",
                            "serviceableVehicles": ["CAR"]
                        }
                    ]
                }
                """;

        mvc.perform(
                    get("/api/workshops")
                ).andExpect(status().isOk())
                .andExpect(responseBody().containsObjectAsJson(expectedJson, WorkshopsRequestDTO.class));
    }

    @Test
    public void testPostAvailableTireChangeTimes() throws Exception {
        String requestBody = """
                {
                    "fromDate": "2023-08-09"
                }
                """;

        TireChangeTimesFilter filter = new TireChangeTimesFilter(null, Instant.parse("2023-08-09T00:00:00Z"), null, null);

        TireChangeTime tireChangeTime = new TireChangeTime(WorkshopId.MANCHESTER, "1", Instant.parse("2024-08-08T06:00:00Z"));
        AvailableTimesResponse availableTimesResponse = new AvailableTimesResponse(List.of(tireChangeTime), List.of(WorkshopId.LONDON));

        when(workshopsService.getAvailableTireChangeTimes(filter))
                .thenReturn(availableTimesResponse);

        String expectedJson = """
                {
                     "tireChangeTimes": [
                         {
                             "workshopId": "MANCHESTER",
                             "id": "1",
                             "time": "2024-08-08T06:00:00Z"
                         }
                     ],
                     "failedWorkshopIds": ["LONDON"]
                }
                """;

        mvc.perform(
                    post("/api/tire-change-times/available")
                            .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(responseBody().containsObjectAsJson(expectedJson, AvailableTireTimesDTO.class));
    }

    @Test
    public void testPostAvailableTireChangeTimesIllegalFilter() throws Exception {
        String requestBody = """
                {
                     "fromDate": "2022-07-04",
                     "toDate": "2021-08-08"
                }
                """;

        TireChangeTimesFilter filter = new TireChangeTimesFilter(null, Instant.parse("2022-07-04T00:00:00Z"), Instant.parse("2021-08-08T00:00:00Z"), null);

        when(workshopsService.getAvailableTireChangeTimes(filter))
                .thenThrow(new IllegalArgumentException("testMessage"));

        mvc.perform(
                        post("/api/tire-change-times/available")
                                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("testMessage"));
    }

    @Test
    public void testPostBookDesiredTime() throws Exception {
        String requestBody = """
                {
                     "contactInformation": "testContactInformation",
                     "id": "1",
                     "workshopId": "MANCHESTER"
                }
                """;

        BookTimeRequest bookTimeRequest = new BookTimeRequest("testContactInformation", "1", WorkshopId.MANCHESTER);
        TireChangeTime tireChangeTime = new TireChangeTime(WorkshopId.MANCHESTER, "1", Instant.parse("2024-08-08T06:00:00Z"));

        when(workshopsService.bookTime(bookTimeRequest))
                .thenReturn(tireChangeTime);

        String expectedJson = """
                {
                    "id": "1",
                     "workshopId": "MANCHESTER",
                     "time": "2024-08-08T06:00:00Z"
                }
                """;
        mvc.perform(
                        post("/api/tire-change-times/available/book")
                                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(responseBody().containsObjectAsJson(expectedJson, AvailableTireTimeDTO.class));
    }

    @Test
    public void testPostBookDesiredTimeWsServiceResponseException() throws Exception {
        String requestBody = """
                {
                     "contactInformation": "testContactInformation",
                     "id": "1",
                     "workshopId": "MANCHESTER"
                }
                """;

        BookTimeRequest bookTimeRequest = new BookTimeRequest("testContactInformation", "1", WorkshopId.MANCHESTER);
        WsServiceErrorDescription errorDescription = new WsServiceErrorDescription("testMessage", "testCode");

        when(workshopsService.bookTime(bookTimeRequest))
                .thenThrow(new WsServiceResponseException(errorDescription, HttpStatus.UNPROCESSABLE_ENTITY));

        mvc.perform(
                        post("/api/tire-change-times/available/book")
                                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().is(HttpStatus.UNPROCESSABLE_ENTITY.value()))
                .andExpect(jsonPath("$.message").value("testMessage"));
    }

    @Test
    public void testPostBookDesiredTimeWsServiceCommunicationException() throws Exception {
        String requestBody = """
                {
                     "contactInformation": "testContactInformation",
                     "id": "1",
                     "workshopId": "MANCHESTER"
                }
                """;

        BookTimeRequest bookTimeRequest = new BookTimeRequest("testContactInformation", "1", WorkshopId.MANCHESTER);

        when(workshopsService.bookTime(bookTimeRequest))
                .thenThrow(new WsServiceCommunicationException(WorkshopId.MANCHESTER));

        mvc.perform(
                        post("/api/tire-change-times/available/book")
                                .content(requestBody).contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    public static class ResponseBodyMatchers {
        private static final ObjectMapper objectMapper = new ObjectMapper();

        public <T> ResultMatcher containsObjectAsJson(
                String expectedJson,
                Class<T> targetClass) {
            return mvcResult -> {
                String json = mvcResult.getResponse().getContentAsString();
                T actualObject = objectMapper.readValue(json, targetClass);
                T expectedObject = objectMapper.readValue(expectedJson, targetClass);
                assertThat(actualObject).usingRecursiveComparison().isEqualTo(expectedObject);
            };
        }

        static ResponseBodyMatchers responseBody(){
            return new ResponseBodyMatchers();
        }

    }
}
