package com.example.demo.service;

import com.example.demo.dto.AppointmentDTOs;
import com.example.demo.dto.AppointmentDTOs.*;
import com.example.demo.dto.PetDTOs;
import com.example.demo.dto.UserDTOs;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Appointment;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private Firestore firestore;

    @Autowired
    private UserService userService;

    @Autowired
    private PetService petService;

    @Autowired
    private NotificationService notificationService;

    private static final long MINIMUM_CANCELLATION_HOURS = 24;
    private static final long MINIMUM_RESCHEDULE_HOURS = 24;

    /**
     * Obtiene las citas del día para un veterinario específico
     */
    public AppointmentSummary getVeterinarianDailyAppointments(String veterinarianId, Date date) {
        try {
            // Convertir la fecha a LocalDate para comparar solo la fecha sin hora
            LocalDate appointmentDate = date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // Obtener el inicio y fin del día
            Date startOfDay = Date.from(appointmentDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endOfDay = Date.from(appointmentDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            // Consultar las citas del día
            QuerySnapshot querySnapshot = firestore.collection("appointments")
                    .whereEqualTo("veterinarianId", veterinarianId)
                    .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                    .whereLessThan("appointmentDate", endOfDay)
                    .orderBy("appointmentDate", Query.Direction.ASCENDING)
                    .get()
                    .get();

            List<AppointmentResponse> appointments = new ArrayList<>();

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Appointment appointment = doc.toObject(Appointment.class);
                if (appointment != null) {
                    appointments.add(enrichAppointmentResponse(appointment));
                }
            }

            return new AppointmentSummary(
                    appointments,
                    appointments.size(),
                    date
            );
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error getting daily appointments: " + e.getMessage());
        }
    }

    /**
     * Enriquece la respuesta de la cita con información detallada
     */
    private AppointmentResponse enrichAppointmentResponse(Appointment appointment) throws ExecutionException, InterruptedException {
        AppointmentResponse response = new AppointmentResponse();
        response.setId(appointment.getId());
        response.setAppointmentDate(appointment.getAppointmentDate());
        response.setReason(appointment.getReason());
        response.setStatus(appointment.getStatus());
        response.setNotes(appointment.getNotes());

        // Obtener información del cliente
        response.setClient(userService.getUserById(appointment.getClientId()));

        // Obtener información del veterinario
        response.setVeterinarian(userService.getUserById(appointment.getVeterinarianId()));

        // Obtener información de la mascota
        response.setPet(petService.getPetById(appointment.getPetId()));

        // Obtener historial médico de la mascota
        response.setPetHistory(petService.getPetMedicalHistory(appointment.getPetId()));

        return response;
    }
    /**
     * Obtiene todas las citas de las mascotas del cliente actual
     */
    public List<AppointmentDTOs.AppointmentSummaryByPet> getClientPetsAppointments() {
        String clientId = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // Obtener todas las mascotas del cliente
            List<PetDTOs.PetResponse> clientPets = petService.getPetsByUserId(clientId);
            List<AppointmentDTOs.AppointmentSummaryByPet> summaries = new ArrayList<>();

            for (PetDTOs.PetResponse pet : clientPets) {
                // Obtener citas de cada mascota
                QuerySnapshot appointmentsSnapshot = firestore.collection("appointments")
                        .whereEqualTo("petId", pet.getId())
                        .whereGreaterThanOrEqualTo("appointmentDate", new Date())
                        .orderBy("appointmentDate", Query.Direction.ASCENDING)
                        .get()
                        .get();

                List<AppointmentDTOs.AppointmentResponse> appointments = appointmentsSnapshot.getDocuments().stream()
                        .map(doc -> {
                            Appointment appointment = doc.toObject(Appointment.class);
                            try {
                                return enrichAppointmentResponse(appointment);
                            } catch (ExecutionException e) {
                                throw new RuntimeException(e);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList());

                summaries.add(new AppointmentDTOs.AppointmentSummaryByPet(
                        pet,
                        appointments,
                        appointments.size()
                ));
            }

            return summaries;
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error getting client pets appointments: " + e.getMessage());
        }
    }

    /**
     * Reprograma una cita existente
     */
    public AppointmentDTOs.AppointmentResponse rescheduleAppointment(
            String appointmentId,
            AppointmentDTOs.RescheduleRequest request
    ) {
        try {
            DocumentReference appointmentRef = firestore.collection("appointments").document(appointmentId);
            DocumentSnapshot appointmentDoc = appointmentRef.get().get();

            if (!appointmentDoc.exists()) {
                throw new CustomExceptions.NotFoundException("Appointment not found");
            }

            Appointment appointment = appointmentDoc.toObject(Appointment.class);

            // Verificar tiempo mínimo para reprogramar
            if (!canReschedule(appointment.getAppointmentDate())) {
                throw new CustomExceptions.UnauthorizedException(
                        "Las citas solo pueden reprogramarse con " + MINIMUM_RESCHEDULE_HOURS + " horas de anticipación"
                );
            }

            // Guardar la fecha anterior para la notificación
            Date oldDate = appointment.getAppointmentDate();

            // Actualizar la cita
            appointment.setAppointmentDate(request.getNewDate());
            appointment.setNotes(appointment.getNotes() + "\nReprogramada: " + request.getReason());
            appointment.setUpdatedAt(new Date());

            appointmentRef.set(appointment).get();

            // Enviar notificaciones
            sendRescheduleNotifications(appointment, oldDate, request.getNewDate());

            return enrichAppointmentResponse(appointment);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error rescheduling appointment: " + e.getMessage());
        }
    }

    /**
     * Cancela una cita existente
     */
    public AppointmentDTOs.AppointmentResponse cancelAppointment(String appointmentId) {
        try {
            DocumentReference appointmentRef = firestore.collection("appointments").document(appointmentId);
            DocumentSnapshot appointmentDoc = appointmentRef.get().get();

            if (!appointmentDoc.exists()) {
                throw new CustomExceptions.NotFoundException("Appointment not found");
            }

            Appointment appointment = appointmentDoc.toObject(Appointment.class);

            // Verificar tiempo mínimo para cancelar
            if (!canCancel(appointment.getAppointmentDate())) {
                throw new CustomExceptions.UnauthorizedException(
                        "Las citas solo pueden cancelarse con " + MINIMUM_CANCELLATION_HOURS + " horas de anticipación"
                );
            }

            // Actualizar el estado de la cita
            appointment.setStatus("CANCELLED");
            appointment.setUpdatedAt(new Date());

            appointmentRef.set(appointment).get();

            // Enviar notificaciones
            sendCancellationNotifications(appointment);

            return enrichAppointmentResponse(appointment);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error canceling appointment: " + e.getMessage());
        }
    }

    /**
     * Verifica si el usuario autenticado es dueño de la cita
     */
    public boolean isOwner(String appointmentId) {
        try {
            String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
            DocumentSnapshot appointmentDoc = firestore.collection("appointments")
                    .document(appointmentId)
                    .get()
                    .get();

            if (!appointmentDoc.exists()) {
                return false;
            }

            Appointment appointment = appointmentDoc.toObject(Appointment.class);
            return appointment != null && appointment.getClientId().equals(currentUserId);
        } catch (Exception e) {
            //logger.error("Error checking appointment ownership: {}", e.getMessage());
            return false;
        }
    }

    private boolean canReschedule(Date appointmentDate) {
        return getHoursDifference(appointmentDate) >= MINIMUM_RESCHEDULE_HOURS;
    }

    private boolean canCancel(Date appointmentDate) {
        return getHoursDifference(appointmentDate) >= MINIMUM_CANCELLATION_HOURS;
    }

    private long getHoursDifference(Date appointmentDate) {
        long diffInMillies = appointmentDate.getTime() - new Date().getTime();
        return diffInMillies / (60 * 60 * 1000);
    }

    private void sendRescheduleNotifications(Appointment appointment, Date oldDate, Date newDate) {
        // Notificar al cliente
        notificationService.sendAppointmentRescheduledNotification(
                appointment.getClientId(),
                appointment.getPetId(),
                oldDate,
                newDate
        );

        // Notificar al veterinario
        notificationService.sendVeterinarianAppointmentRescheduledNotification(
                appointment.getVeterinarianId(),
                appointment.getPetId(),
                oldDate,
                newDate
        );
    }

    private void sendCancellationNotifications(Appointment appointment) {
        // Notificar al cliente
        notificationService.sendAppointmentCancelledNotification(
                appointment.getClientId(),
                appointment.getPetId(),
                appointment.getAppointmentDate()
        );

        // Notificar al veterinario
        notificationService.sendVeterinarianAppointmentCancelledNotification(
                appointment.getVeterinarianId(),
                appointment.getPetId(),
                appointment.getAppointmentDate()
        );
    }
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        String clientId = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // Validar que la mascota pertenece al cliente
            PetDTOs.PetResponse pet = petService.getPetById(request.getPetId());
            if (!pet.getOwnerId().equals(clientId)) {
                throw new CustomExceptions.UnauthorizedException("No tienes permiso para agendar citas para esta mascota");
            }

            // Validar que la fecha es futura
            if (request.getAppointmentDate().before(new Date())) {
                throw new IllegalArgumentException("La fecha de la cita debe ser futura");
            }

            // Crear la cita
            Appointment appointment = Appointment.builder()
                    .id(UUID.randomUUID().toString())
                    .petId(request.getPetId())
                    .clientId(clientId)
                    .veterinarianId(request.getVeterinarianId())
                    .appointmentDate(request.getAppointmentDate())
                    .reason(request.getReason())
                    .status("SCHEDULED")
                    .notes(request.getNotes())
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();

            // Guardar en Firestore
            firestore.collection("appointments")
                    .document(appointment.getId())
                    .set(appointment)
                    .get();

            // Enviar notificaciones
            sendNewAppointmentNotifications(appointment);

            return enrichAppointmentResponse(appointment);
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error creating appointment: " + e.getMessage());
        }
    }

    private void sendNewAppointmentNotifications(Appointment appointment) {
        try {
            // Notificar al cliente
            UserDTOs.UserResponse client = userService.getUserById(appointment.getClientId());
            PetDTOs.PetResponse pet = petService.getPetById(appointment.getPetId());
            UserDTOs.UserResponse vet = userService.getUserById(appointment.getVeterinarianId());

            String clientSubject = "Nueva cita programada para " + pet.getName();
            String clientContent = notificationService.generateNewAppointmentEmail(pet.getName(),
                    appointment.getAppointmentDate(),
                    vet.getNombre() + " " + vet.getApellido());

            notificationService.sendEmail(client.getEmail(), clientSubject, clientContent);

            // Notificar al veterinario
            String vetSubject = "Nueva cita programada";
            String vetContent = notificationService.generateNewAppointmentVetEmail(pet.getName(),
                    appointment.getAppointmentDate(),
                    client.getNombre() + " " + client.getApellido());

            notificationService.sendEmail(vet.getEmail(), vetSubject, vetContent);
        } catch (Exception e) {
            //log.error("Error sending new appointment notifications: {}", e.getMessage());
        }
    }
}