package com.alexisa.tire_reserve.service.workshop;

import com.alexisa.tire_reserve.config.WorkshopsProperties;
import com.alexisa.tire_reserve.exceptions.service.workshops.WsServiceCommunicationException;
import com.alexisa.tire_reserve.exceptions.service.workshops.response.WsServiceResponseException;
import com.alexisa.tire_reserve.model.domain.TireChangeTime;
import com.alexisa.tire_reserve.model.domain.AvailableTimesResponse;
import com.alexisa.tire_reserve.model.domain.BookTimeRequest;
import com.alexisa.tire_reserve.model.domain.TireChangeTimesFilter;
import com.alexisa.tire_reserve.model.domain.enums.ServiceableVehicle;
import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
public class WorkshopsService {

    private final Map<WorkshopId, WsApiServiceI> workshopServices;
    private final Map<WorkshopId, List<ServiceableVehicle>> workshopServiceableVehiclesMap;
    private final LoadingCache<String, List<CachedWsAvailableTimes>> cache;
    private static final String cacheKey = "availableTimes";

    public WorkshopsService(
            WorkshopsProperties workshopsProperties,
            @Autowired Caffeine<Object, Object> caffeine,
            @Autowired(required = false) LondonWsApiService londonWsApiService,
            @Autowired(required = false) ManchesterWsApiService manchesterWsApiService
    ) {
        workshopServices = new HashMap<>();
        workshopServiceableVehiclesMap = new HashMap<>();

        for (WorkshopsProperties.WorkshopProperties wsProps : workshopsProperties.map().values()) {
            if (workshopServices.containsKey(wsProps.workshopId())){
                throw new IllegalStateException("Multiple workshops configured for workshop: " + wsProps.workshopId().getName());
            }

            WsApiServiceI correspondingApi = switch (wsProps.workshopId()){
                case MANCHESTER -> manchesterWsApiService;
                case LONDON -> londonWsApiService;
            };
            if (correspondingApi != null) {
                workshopServices.put(wsProps.workshopId(), correspondingApi);
                workshopServiceableVehiclesMap.put(wsProps.workshopId(), wsProps.serviceableVehicles());
            }
        }
        log.info("Configured workshop services: {}", workshopServices.keySet());

        cache = caffeine
                .build(key -> updateAvailableTimesCache());
    }

    public List<WorkshopsProperties.WorkshopProperties> getWorkshops(){
        return workshopServices.values().stream()
                .map(WsApiServiceI::getWorkshopProperties)
                .toList();
    }

    public AvailableTimesResponse getAvailableTireChangeTimes(TireChangeTimesFilter filter) {
        if (filter.toDate() != null && ! filter.toDate().isAfter(filter.fromDate())){
            throw new IllegalArgumentException(String.format(
                    "Expected argument 'toDate' value '%s' to be after 'fromDate' value '%s'",
                    filter.toDate(),
                    filter.fromDate()
            ));
        }

        Set<WorkshopId> allowedWorkshops = allowedWorkshopIds(filter.selectedWorkshops(), filter.serviceableVehicles());
        if (allowedWorkshops.isEmpty()){
            throw new IllegalArgumentException(String.format(
                    "The combination of selected workshops '%s' and serviceable vehicles '%s' doesn't allow for any times to be queried",
                    filter.selectedWorkshops(),
                    filter.serviceableVehicles()
            ));
        }

        List<CachedWsAvailableTimes> allTimes = getAllAvailableTimesFromCache();
        List<WorkshopId> failedServiceIds = getAllFailedServiceIds(allTimes);
        failedServiceIds =failedServiceIds.stream().filter(allowedWorkshops::contains).toList();

        Stream<TireChangeTime> resultStream = allTimes.stream()
                .filter(wsTimes ->
                        allowedWorkshops.contains(wsTimes.workshopId)
                ).map(CachedWsAvailableTimes::availableTimes)
                .filter(Objects::nonNull)
                .flatMap(List::stream);

        resultStream = filterTimesOnInstant(
                resultStream,
                filter.fromDate(),
                filter.toDate()
        );
        resultStream = filterTimesOnReserved(resultStream);

        return new AvailableTimesResponse(resultStream.toList(), failedServiceIds);
    }

    private Set<WorkshopId> allowedWorkshopIds(
            List<WorkshopId> selectedWorkshops,
            List<ServiceableVehicle> serviceableVehicles
    ) {
        Set<WorkshopId> allowedWorkshops = EnumSet.allOf(WorkshopId.class);
        if (CollectionUtils.isNotEmpty(selectedWorkshops)) {
            allowedWorkshops.removeIf(allowedWorkshop -> !selectedWorkshops.contains(allowedWorkshop));
        }
        if (CollectionUtils.isNotEmpty(serviceableVehicles)) {
            allowedWorkshops.removeIf(allowedWorkshop ->
                    !CollectionUtils.containsAny(
                            workshopServiceableVehiclesMap.get(allowedWorkshop),
                            serviceableVehicles
                    )
            );
        }
        return allowedWorkshops;
    }

    private Stream<TireChangeTime> filterTimesOnInstant(
            Stream<TireChangeTime> timesStream,
            Instant from,
            @Nullable
            Instant to
    ){
        Stream<TireChangeTime> resultStream = timesStream;

        resultStream = resultStream.filter(el -> !el.getInstant().isBefore(from));
        if (to != null) {
            resultStream = resultStream.filter(el -> !el.getInstant().isAfter(to));
        }

        return resultStream;
    }
    private Stream<TireChangeTime> filterTimesOnReserved(Stream<TireChangeTime> timesStream){
        return timesStream.filter(time -> !time.isReservedSet());
    }

    private List<CachedWsAvailableTimes> getAllAvailableTimesFromCache() {
        List<CachedWsAvailableTimes> availableTimes = cache.get(cacheKey);

        if (availableTimes.stream().anyMatch(wsTimes -> wsTimes.cachePopulationEx != null)){
            cache.invalidate(cacheKey);
            return cache.get(cacheKey);
        }

        return availableTimes;
    }

    private List<CachedWsAvailableTimes> updateAvailableTimesCache(){
        log.info("Repopulating available tire change times cache");

        return workshopServices.values()
                .parallelStream()
                .map(WorkshopsService::getAvailableTimesFromService)
                .toList();
    }

    private static CachedWsAvailableTimes getAvailableTimesFromService(WsApiServiceI service) {
        WorkshopId workshopId = service.getWorkshopProperties().workshopId();
        List<TireChangeTime> times;
        WsServiceCommunicationException ex = null;
        try {
            times = Collections.unmodifiableList(service.getAllAvailableTireChangeTimes());
            log.info("Received {} available times from workshop with id '{}'", times.size(), workshopId);
        } catch (WsServiceCommunicationException e) {
            times = null;
            ex = e;
            log.warn("Failed refreshing cache of workshop '{}", workshopId, ex);
        }
        return new CachedWsAvailableTimes(workshopId, times, ex);
    }

    private static List<WorkshopId> getAllFailedServiceIds(List<CachedWsAvailableTimes> availableTimes){
        return availableTimes.stream()
                .map(CachedWsAvailableTimes::cachePopulationEx)
                .filter(Objects::nonNull)
                .map(WsServiceCommunicationException::getCausedBy)
                .toList();
    }

    public TireChangeTime bookTime(BookTimeRequest bookTimeRequest) throws WsServiceCommunicationException {

        WsApiServiceI targetService = workshopServices.get(bookTimeRequest.workshopId());

        TireChangeTime bookedTime;
        try {
            bookedTime = targetService.bookDesiredTime(bookTimeRequest);
        } catch (WsServiceResponseException e) {
            // If time already booked, try to update the cache as well
            if (e.getResponseStatusCode().isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY)) {
                updateBookedTimeInCache(new TireChangeTime(
                        bookTimeRequest.workshopId(),
                        bookTimeRequest.id(),
                        null
                ));
            }
            throw e;
        }
        updateBookedTimeInCache(bookedTime);
        return bookedTime;
    }

    private void updateBookedTimeInCache(TireChangeTime bookedTime) {
        List<CachedWsAvailableTimes> cachedTimes = getAllAvailableTimesFromCache();
        Optional<CachedWsAvailableTimes> wsTimesOpt = cachedTimes.stream()
                .filter(wsTimes -> wsTimes.workshopId == bookedTime.getWorkshopId())
                .findFirst();
        if (wsTimesOpt.isPresent()) {
            CachedWsAvailableTimes wsTimes = wsTimesOpt.get();
            List<TireChangeTime> times = wsTimes.availableTimes;
            if (CollectionUtils.isNotEmpty(times)){
                times.stream()
                        .filter(bookedTime::equals)
                        .findFirst()
                        .ifPresent(TireChangeTime::setReservedTrue);
            }
        }
    }

    public record CachedWsAvailableTimes(
            WorkshopId workshopId,
            @Nullable List<TireChangeTime> availableTimes,
            @Nullable WsServiceCommunicationException cachePopulationEx
    ) { }
}
