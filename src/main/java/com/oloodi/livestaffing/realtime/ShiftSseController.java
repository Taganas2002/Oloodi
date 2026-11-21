package com.oloodi.livestaffing.realtime;

import com.oloodi.livestaffing.common.error.ResourceNotFoundException;
import com.oloodi.livestaffing.shift.repository.ShiftRepository;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/shifts")
public class ShiftSseController {

    private final ShiftRepository shiftRepository;
    private final ShiftSseService shiftSseService;

    public ShiftSseController(ShiftRepository shiftRepository, ShiftSseService shiftSseService) {
        this.shiftRepository = shiftRepository;
        this.shiftSseService = shiftSseService;
    }

    @GetMapping(path = "/{shiftId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable UUID shiftId) {
        if (!shiftRepository.existsById(shiftId)) {
            throw new ResourceNotFoundException("Shift not found: " + shiftId);
        }
        return shiftSseService.subscribe(shiftId);
    }
}
