package com.example.demo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.database.url}")
    private String databaseUrl;

    @Value("${firebase.config.path}")
    private String configPath;

    // Bean para inicializar FirebaseApp
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Cargar el archivo JSON de configuración de Firebase
        InputStream serviceAccount = new ClassPathResource(configPath).getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl(databaseUrl)
                .build();

        // Verificar si FirebaseApp ya fue inicializado, y si no, inicializarlo
        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        } else {
            return FirebaseApp.getInstance();
        }
    }

    // Bean para Firestore
    @Bean
    public Firestore firestore() throws IOException {
        return FirestoreClient.getFirestore(firebaseApp());
    }
    // Bean para FirebaseAuth
    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        return FirebaseAuth.getInstance(firebaseApp()); // Asegura que FirebaseApp esté inicializado antes
    }
}
