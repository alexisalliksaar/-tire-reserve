package com.alexisa.tire_reserve.service.workshop;

import com.alexisa.tire_reserve.config.WorkshopsProperties;
import com.alexisa.tire_reserve.exceptions.service.workshops.WsServiceCommunicationException;
import com.alexisa.tire_reserve.exceptions.service.workshops.response.WsServiceResponseException;
import com.alexisa.tire_reserve.model.domain.AvailableTimesResponse;
import com.alexisa.tire_reserve.model.domain.BookTimeRequest;
import com.alexisa.tire_reserve.model.domain.TireChangeTime;
import com.alexisa.tire_reserve.model.domain.TireChangeTimesFilter;
import com.alexisa.tire_reserve.model.domain.enums.ServiceableVehicle;
import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.testing.FakeTicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkshopsServiceTests {

    private static final WorkshopsProperties WORKSHOPS_PROPERTIES = new WorkshopsProperties(
            Map.of(
                    "manchester",
                    new WorkshopsProperties.WorkshopProperties(
                            WorkshopId.MANCHESTER,
                            "http://localhost:1111/api/v2/test",
                            "test", "test", "test", "test",
                            List.of(ServiceableVehicle.CAR)
                    ),
                    "london",
                    new WorkshopsProperties.WorkshopProperties(
                            WorkshopId.LONDON,
                            "http://localhost:1111/api/test",
                            "test", "test", "test", "test",
                            List.of(ServiceableVehicle.CAR, ServiceableVehicle.TRUCK)
                    )
            )
    );

    private TireChangeTime manchesterTime1;
    private TireChangeTime manchesterTime2;
    private TireChangeTime londonTime1;
    private TireChangeTime londonTime2;

    private static final FakeTicker FAKE_TICKER = new FakeTicker();
    private static final Caffeine<Object, Object>  CAFFEINE_MOCK = Caffeine.newBuilder()
            .ticker(FAKE_TICKER::read)
            .executor(Runnable::run)
            .expireAfterWrite(Duration.ofMinutes(60));

    private WorkshopsService workshopsService;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private ManchesterWsApiService manchesterWsApiService;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private LondonWsApiService londonWsApiService;

    private static final BookTimeRequest BOOK_TIME_REQUEST = new BookTimeRequest("test", "1", WorkshopId.MANCHESTER);

    @BeforeEach
    public void setUp() throws WsServiceCommunicationException {
        manchesterTime1 = new TireChangeTime(WorkshopId.MANCHESTER, "1", Instant.parse("2024-08-08T06:00:00Z"));
        manchesterTime2 = new TireChangeTime(WorkshopId.MANCHESTER, "2", Instant.parse("2024-08-09T06:00:00Z"));
        londonTime1 = new TireChangeTime(WorkshopId.LONDON, "1", Instant.parse("2024-08-08T06:00:00Z"));
        londonTime2 = new TireChangeTime(WorkshopId.LONDON, "2", Instant.parse("2024-08-09T06:00:00Z"));

        List<TireChangeTime> allManchesterTimes = List.of(
                manchesterTime1,
                manchesterTime2
        );

        List<TireChangeTime> allLondonTimes = List.of(
                londonTime1,
                londonTime2
        );

        workshopsService = new WorkshopsService(WORKSHOPS_PROPERTIES, CAFFEINE_MOCK, londonWsApiService, manchesterWsApiService);

        when(manchesterWsApiService.getWorkshopProperties()).thenReturn(WORKSHOPS_PROPERTIES.map().get("manchester"));
        when(londonWsApiService.getWorkshopProperties()).thenReturn(WORKSHOPS_PROPERTIES.map().get("london"));

        when(manchesterWsApiService.getAllAvailableTireChangeTimes()).thenReturn(allManchesterTimes);
        when(londonWsApiService.getAllAvailableTireChangeTimes()).thenReturn(allLondonTimes);

        when(manchesterWsApiService.bookDesiredTime(BOOK_TIME_REQUEST)).thenReturn(manchesterTime1);
    }

    @Test
    public void testWorkshopsProperties() {
        assertThat(workshopsService.getWorkshops()).containsAll(WORKSHOPS_PROPERTIES.map().values());
    }

    private static final Instant BEFORE_ALL = Instant.parse("2023-08-09T00:00:00Z");

    @Test
    public void testAvailableTimesFilterOnWorkshopId() {

        TireChangeTimesFilter tireChangeTimesFilter = new TireChangeTimesFilter(
                List.of(WorkshopId.MANCHESTER),
                BEFORE_ALL,
                null,
                null
        );
        AvailableTimesResponse availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);

        assertThat(availableTimesResponse.availableTimes()).containsAll(List.of(manchesterTime1, manchesterTime2));
        assertThat(availableTimesResponse.availableTimes().size()).isEqualTo(2);
        assertThat(availableTimesResponse.failedWorkshops().size()).isEqualTo(0);
    }

    @Test
    public void testAvailableTimesFilterOnInstant() {

        Instant toDate;
        Instant fromDate;
        TireChangeTimesFilter tireChangeTimesFilter;
        AvailableTimesResponse availableTimesResponse;

        // Test that filtering works based on toDate being before instant
        fromDate = BEFORE_ALL;
        toDate = Instant.parse("2024-08-09T00:00:00Z");

        tireChangeTimesFilter = new TireChangeTimesFilter(
                null,
                fromDate,
                toDate,
                null
        );
        availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);

        assertThat(availableTimesResponse.availableTimes()).containsAll(List.of(manchesterTime1, londonTime1));
        assertThat(availableTimesResponse.availableTimes().size()).isEqualTo(2);
        assertThat(availableTimesResponse.failedWorkshops().size()).isEqualTo(0);

        // Test that filtering works based on fromDate being after instant

        fromDate = Instant.parse("2024-08-09T00:00:00Z");
        toDate = Instant.parse("2024-08-10T00:00:00Z");

        tireChangeTimesFilter = new TireChangeTimesFilter(
                null,
                fromDate,
                toDate,
                null
        );
        availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);

        assertThat(availableTimesResponse.availableTimes()).containsAll(List.of(manchesterTime2, londonTime2));
        assertThat(availableTimesResponse.availableTimes().size()).isEqualTo(2);
        assertThat(availableTimesResponse.failedWorkshops().size()).isEqualTo(0);

    }

    @Test
    public void testAvailableTimesFilterOnServiceableVehicles() {

        TireChangeTimesFilter tireChangeTimesFilter = new TireChangeTimesFilter(
                null,
                BEFORE_ALL,
                null,
                List.of(ServiceableVehicle.TRUCK)
        );
        AvailableTimesResponse availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);

        assertThat(availableTimesResponse.availableTimes()).containsAll(List.of(londonTime1, londonTime2));
        assertThat(availableTimesResponse.availableTimes().size()).isEqualTo(2);
        assertThat(availableTimesResponse.failedWorkshops().size()).isEqualTo(0);
    }

    @Test
    public void testAvailableTimesEmpty() {

        TireChangeTimesFilter tireChangeTimesFilter = new TireChangeTimesFilter(
                null,
                BEFORE_ALL.minus(Duration.ofDays(1)),
                BEFORE_ALL,
                null
        );
        AvailableTimesResponse availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);

        assertThat(availableTimesResponse.availableTimes().size()).isEqualTo(0);
        assertThat(availableTimesResponse.failedWorkshops().size()).isEqualTo(0);
    }

    @Test
    public void testAvailableTimesIllegalFilter() {

        TireChangeTimesFilter tireChangeTimesFilterIllegalDates = new TireChangeTimesFilter(
                null,
                BEFORE_ALL,
                BEFORE_ALL,
                null
        );

        assertThrows(IllegalArgumentException.class, () ->
                workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilterIllegalDates)
        );

        TireChangeTimesFilter tireChangeTimesFilterIllegalCombination = new TireChangeTimesFilter(
                List.of(WorkshopId.MANCHESTER),
                BEFORE_ALL,
                null,
                List.of(ServiceableVehicle.TRUCK)
        );

        assertThrows(IllegalArgumentException.class, () ->
                workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilterIllegalCombination)
        );
    }

    @Test
    public void testAvailableTimesFailedService() throws WsServiceCommunicationException {
        when(manchesterWsApiService.getAllAvailableTireChangeTimes())
                .thenThrow(new WsServiceCommunicationException(
                        "test",
                        new WsServiceResponseException(null, null),
                        WorkshopId.MANCHESTER)
                );

        TireChangeTimesFilter tireChangeTimesFilter = new TireChangeTimesFilter(
                null,
                BEFORE_ALL,
                null,
                null
        );
        AvailableTimesResponse availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);

        assertThat(availableTimesResponse.availableTimes()).containsAll(List.of(londonTime1, londonTime2));
        assertThat(availableTimesResponse.availableTimes().size()).isEqualTo(2);
        assertThat(availableTimesResponse.failedWorkshops()).contains(WorkshopId.MANCHESTER);
        assertThat(availableTimesResponse.failedWorkshops().size()).isEqualTo(1);
    }

    @Test
    public void testBookDesiredTime() throws WsServiceCommunicationException {
        // populate cache
        TireChangeTimesFilter tireChangeTimesFilter = new TireChangeTimesFilter(
                null,
                BEFORE_ALL,
                null,
                null
        );
        AvailableTimesResponse availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);
        assertThat(availableTimesResponse.availableTimes()).contains(manchesterTime1);

        TireChangeTime tireChangeTime = workshopsService.bookTime(BOOK_TIME_REQUEST);

        assertThat(tireChangeTime.getId()).isEqualTo(BOOK_TIME_REQUEST.id());
        assertThat(tireChangeTime.getWorkshopId()).isEqualTo(BOOK_TIME_REQUEST.workshopId());

        // check cache
        availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);

        assertThat(availableTimesResponse.availableTimes()).doesNotContain(manchesterTime1);
    }

    @Test
    public void testBookDesiredTimeCommunicationException() throws WsServiceCommunicationException {
        when(manchesterWsApiService.bookDesiredTime(BOOK_TIME_REQUEST))
                .thenThrow(new WsServiceCommunicationException(WorkshopId.MANCHESTER));

        assertThrows(WsServiceCommunicationException.class, () -> workshopsService.bookTime(BOOK_TIME_REQUEST));
    }

    @Test
    public void testBookDesiredTimeAlreadyBooked() throws WsServiceCommunicationException {

        when(manchesterWsApiService.bookDesiredTime(BOOK_TIME_REQUEST))
                .thenThrow(new WsServiceResponseException(null, HttpStatus.UNPROCESSABLE_ENTITY));

        // populate cache
        TireChangeTimesFilter tireChangeTimesFilter = new TireChangeTimesFilter(
                null,
                BEFORE_ALL,
                null,
                null
        );
        AvailableTimesResponse availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);
        assertThat(availableTimesResponse.availableTimes()).contains(manchesterTime1);

        assertThrows(WsServiceResponseException.class, () -> workshopsService.bookTime(BOOK_TIME_REQUEST));

        // check cache
        availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);

        assertThat(availableTimesResponse.availableTimes()).doesNotContain(manchesterTime1);

    }

    @Test
    public void testAvailableTimesCacheExpire() throws WsServiceCommunicationException {
        // populate cache
        TireChangeTimesFilter tireChangeTimesFilter = new TireChangeTimesFilter(
                null,
                BEFORE_ALL,
                null,
                null
        );
        AvailableTimesResponse availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);
        assertThat(availableTimesResponse.availableTimes()).contains(manchesterTime1);

        when(manchesterWsApiService.getAllAvailableTireChangeTimes())
                .thenReturn(List.of(manchesterTime2));

        availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);

        assertThat(availableTimesResponse.availableTimes()).contains(manchesterTime1);

        FAKE_TICKER.advance(Duration.ofMinutes(61));

        availableTimesResponse = workshopsService.getAvailableTireChangeTimes(tireChangeTimesFilter);

        assertThat(availableTimesResponse.availableTimes()).doesNotContain(manchesterTime1);

    }
}
