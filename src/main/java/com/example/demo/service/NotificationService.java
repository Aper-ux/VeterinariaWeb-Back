package com.example.demo.service;


import com.example.demo.model.AlertStatus;
import com.example.demo.model.LowStockAlert;
import com.example.demo.model.Role;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    private Firestore firestore;

    public void sendLowStockAlert(LowStockAlert alert) {
        try {
            // Obtener emails de recepcionistas
            List<String> recipientEmails = getUserEmailsByRole(Role.RECEPCIONISTA);

            String subject = "Alerta de Stock Bajo: " + alert.getProductName();
            String content = generateLowStockEmailContent(alert);

            // Enviar email a cada recepcionista
            for (String email : recipientEmails) {
                sendEmail(email, subject, content);
            }
        } catch (Exception e) {
            log.error("Error sending low stock alert: {}", e.getMessage());
        }
    }

    void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            emailSender.send(message);
            log.info("Alert email sent to: {}", to);
        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage());
        }
    }

    private String generateLowStockEmailContent(LowStockAlert alert) {
        return """
            <html>
            <body>
                <h2>Alerta de Stock Bajo</h2>
                <div style="background-color: %s; padding: 20px; border-radius: 5px;">
                    <h3>Producto: %s</h3>
                    <p>Stock Actual: %d</p>
                    <p>Stock Mínimo: %d</p>
                    <p>Estado: %s</p>
                </div>
                <p>Se requiere realizar un nuevo pedido.</p>
                <a href="http://tu-aplicacion.com/inventory/restock/%s">Crear Orden de Reabastecimiento</a>
            </body>
            </html>
            """.formatted(
                alert.getStatus() == AlertStatus.CRITICAL ? "#ffebee" : "#fff3e0",
                alert.getProductName(),
                alert.getCurrentStock(),
                alert.getMinThreshold(),
                alert.getStatus(),
                alert.getProductId()
        );
    }
    /**
     * Obtiene los emails de los usuarios que tienen un rol específico
     */
    public List<String> getUserEmailsByRole(Role role) {
        try {
            // Consultar todos los usuarios con el rol especificado
            QuerySnapshot querySnapshot = firestore.collection("users")
                    .whereArrayContains("roles", role)
                    .get()
                    .get();

            // Extraer y retornar los emails
            return querySnapshot.getDocuments().stream()
                    .map(doc -> doc.getString("email"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error obteniendo emails para el rol {}: {}", role, e.getMessage());
            return new ArrayList<>(); // Retornar lista vacía en caso de error
        }
    }
}