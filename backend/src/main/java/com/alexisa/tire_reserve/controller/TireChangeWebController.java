package com.alexisa.tire_reserve.controller;

import com.alexisa.tire_reserve.exceptions.service.workshops.WsServiceCommunicationException;
import com.alexisa.tire_reserve.exceptions.service.workshops.response.WsServiceResponseException;
import com.alexisa.tire_reserve.model.domain.TireChangeTime;
import com.alexisa.tire_reserve.model.domain.AvailableTimesResponse;
import com.alexisa.tire_reserve.model.domain.BookTimeRequest;
import com.alexisa.tire_reserve.model.domain.TireChangeTimesFilter;
import com.alexisa.tire_reserve.model.domain.error.WsServiceErrorDescription;
import com.alexisa.tire_reserve.model.dto.AvailableTireTimeDTO;
import com.alexisa.tire_reserve.model.dto.AvailableTireTimesDTO;
import com.alexisa.tire_reserve.model.dto.TireChangeTimesFilterDTO;
import com.alexisa.tire_reserve.model.dto.WsServiceErrorDTO;
import com.alexisa.tire_reserve.model.dto.workshop.WorkshopDTO;
import com.alexisa.tire_reserve.model.dto.workshop.WorkshopsRequestDTO;
import com.alexisa.tire_reserve.service.workshop.WorkshopsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@CrossOrigin
public class TireChangeWebController {

    private final WorkshopsService workshopsService;

    @GetMapping("/workshops")
    public WorkshopsRequestDTO getWorkshops(){
        List<WorkshopDTO> workshopDTOS = workshopsService.getWorkshops().stream()
                .map(WorkshopDTO::fromWorkshopProperties)
                .toList();
        return new WorkshopsRequestDTO(workshopDTOS);
    }

    @PostMapping("/tire-change-times/available")
    @ResponseStatus(HttpStatus.OK)
    public AvailableTireTimesDTO getAvailableTireChangeTimes(@RequestBody TireChangeTimesFilterDTO filterDTO) {

        TireChangeTimesFilter filter = TireChangeTimesFilter.fromDTO(filterDTO);

        AvailableTimesResponse availableTimesResponse = workshopsService.getAvailableTireChangeTimes(filter);

        List<AvailableTireTimeDTO> availableTimeDTOs = availableTimesResponse.availableTimes().stream()
                .sorted(Comparator.comparing(TireChangeTime::getInstant))
                .map(AvailableTireTimeDTO::dtoFromModel)
                .toList();
        return new AvailableTireTimesDTO(availableTimeDTOs, availableTimesResponse.failedWorkshops());
    }

    @PostMapping("/tire-change-times/available/book")
    @ResponseStatus(HttpStatus.OK)
    public AvailableTireTimeDTO bookTireChangeTime(@RequestBody BookTimeRequest bookTimeRequest) throws WsServiceCommunicationException {
        TireChangeTime bookedTime = workshopsService.bookTime(bookTimeRequest);
        return AvailableTireTimeDTO.dtoFromModel(bookedTime);
    }

    @ExceptionHandler(WsServiceResponseException.class)
    public ResponseEntity<WsServiceErrorDTO> handleWsServiceResponseException(WsServiceResponseException e) {

        WsServiceErrorDescription errorDescription = e.getServiceErrorResponse();
        WsServiceErrorDTO errorDTO = new WsServiceErrorDTO(errorDescription.errorMessage());

        HttpStatus status = HttpStatus.valueOf(e.getResponseStatusCode().value());

        if (status.isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY)){
            log.info(e.getMessage(), e);
        } else {
            log.warn(e.getMessage(), e);
        }

        return ResponseEntity.status(status).body(errorDTO);
    }

    @ExceptionHandler(WsServiceCommunicationException.class)
    public ResponseEntity<WsServiceErrorDTO> handleWsServiceCommunicationException(WsServiceCommunicationException e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        WsServiceErrorDTO errorDTO = new WsServiceErrorDTO("Internal server error");
        log.error(e.getMessage(), e);

        return ResponseEntity.status(status).body(errorDTO);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<WsServiceErrorDTO> handleWsServiceCommunicationException(IllegalArgumentException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        WsServiceErrorDTO errorDTO = new WsServiceErrorDTO(e.getMessage());
        return ResponseEntity.status(status).body(errorDTO);
    }

}
