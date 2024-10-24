package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.AppointmentDTOs;
import com.example.demo.dto.AppointmentDTOs.*;
import com.example.demo.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/daily")
    @PreAuthorize("hasPermission(null, 'VER_CITAS_DIARIAS')")
    public ResponseEntity<ApiResponse<AppointmentSummary>> getDailyAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date,
            @RequestParam String veterinarianId
    ) {
        if (date == null) {
            date = new Date(); // Si no se proporciona fecha, usar la fecha actual
        }
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.getVeterinarianDailyAppointments(veterinarianId, date)
        ));
    }
    @GetMapping("/my-pets")
    @PreAuthorize("hasPermission(null, 'VER_CITAS_DE_MASCOTAS_MIAS')")
    public ResponseEntity<ApiResponse<List<AppointmentSummaryByPet>>> getClientPetsAppointments() {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.getClientPetsAppointments()
        ));
    }
    @PostMapping("/schedule")
    @PreAuthorize("hasPermission(null, 'PROGRAMAR_CITA')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> scheduleAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.createAppointment(request)
        ));
    }

    @PostMapping("/{appointmentId}/reschedule")
    @PreAuthorize("hasPermission(null, 'REPROGRAMAR_CITA') and @appointmentService.isOwner(#appointmentId)")
    public ResponseEntity<ApiResponse<AppointmentDTOs.AppointmentResponse>> rescheduleAppointment(
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentDTOs.RescheduleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.rescheduleAppointment(appointmentId, request)
        ));
    }

    @PostMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasPermission(null, 'PROGRAMAR_CITA') and @appointmentService.isOwner(#appointmentId)")
    public ResponseEntity<ApiResponse<AppointmentDTOs.AppointmentResponse>> cancelAppointment(
            @PathVariable String appointmentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.cancelAppointment(appointmentId)
        ));
    }
}