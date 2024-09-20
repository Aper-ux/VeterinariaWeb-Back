package com.example.demo.service;

import com.example.demo.dto.AuthDTOs.*;
import com.example.demo.dto.UserDTOs.UserResponse;
import com.example.demo.exception.CustomExceptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import com.google.firebase.auth.UserRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserService userService;

    @Value("${firebase.api.key}")
    private String firebaseApiKey;

    private final WebClient webClient;

    public AuthService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://identitytoolkit.googleapis.com/v1").build();
    }

    public AuthResponse registerUser(RegisterRequest request) {
        try {
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getEmail())
                    .setPassword(request.getPassword())
                    .setDisplayName(request.getNombre() + " " + request.getApellido());

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);

            UserResponse user = userService.createUser(userRecord.getUid(), request);

            List<String> rolesWithPrefix = user.getRoles().stream()
                    .map(role -> "ROLE_" + role.name())
                    .collect(Collectors.toList());

            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", rolesWithPrefix);
            FirebaseAuth.getInstance().setCustomUserClaims(userRecord.getUid(), claims);

            String customToken = FirebaseAuth.getInstance().createCustomToken(userRecord.getUid());
            String idToken = exchangeCustomTokenForIdToken(customToken);

            return new AuthResponse(idToken, user);
        } catch (FirebaseAuthException e) {
            throw new CustomExceptions.AuthenticationException("Error during user registration: " + e.getMessage());
        }
    }

    public AuthResponse loginUser(LoginRequest request) {
        try {
            // Obtener el usuario por correo electrónico
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(request.getEmail());
            UserResponse user = userService.getUserById(userRecord.getUid());

            // Validar la contraseña
            WebClient webClient = WebClient.builder().baseUrl("https://identitytoolkit.googleapis.com/v1").build();
            Map<String, Object> loginData = Map.of(
                    "email", request.getEmail(),
                    "password", request.getPassword(),
                    "returnSecureToken", true
            );

            Map<String, Object> response = webClient.post()
                    .uri("/accounts:signInWithPassword?key=" + firebaseApiKey)
                    .bodyValue(loginData)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.containsKey("error")) {
                throw new CustomExceptions.InvalidCredentialsException("Invalid email or password.");
            }

            // Si la contraseña es válida, generar el token personalizado
            String customToken = FirebaseAuth.getInstance().createCustomToken(userRecord.getUid());
            String idToken = exchangeCustomTokenForIdToken(customToken);

            return new AuthResponse(idToken, user);
        } catch (FirebaseAuthException e) {
            throw new CustomExceptions.AuthenticationException("Error during user login: " + e.getMessage());
        } catch (CustomExceptions.InvalidCredentialsException e) {
            throw new CustomExceptions.InvalidCredentialsException(e.getMessage());
        } catch (Exception e) {
            // Captura cualquier excepción inesperada para evitar el error 500 sin contexto
            //aqui esta el error cuando no se cumple correo y contraseña correctos
            throw new CustomExceptions.AuthenticationException("Invalid email or password." );
        }
    }



    private String exchangeCustomTokenForIdToken(String customToken) {
        return webClient.post()
                .uri("/accounts:signInWithCustomToken?key=" + firebaseApiKey)
                .bodyValue(Map.of("token", customToken, "returnSecureToken", true))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("idToken"))
                .block();
    }



    public void logout(String uid) {
        try {
            // Revocar todos los tokens del usuario
            FirebaseAuth.getInstance().revokeRefreshTokens(uid);
        } catch (FirebaseAuthException e) {
            throw new CustomExceptions.AuthenticationException("Error during user logout: " + e.getMessage());
        }
    }

    public AuthResponse refreshToken(String uid) {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
            UserResponse user = userService.getUserById(uid);

            // Generar nuevo token personalizado
            String customToken = FirebaseAuth.getInstance().createCustomToken(uid);

            return new AuthResponse(customToken, user);
        } catch (FirebaseAuthException e) {
            throw new CustomExceptions.AuthenticationException("Error refreshing token: " + e.getMessage());
        }
    }

    public void resetPassword(String email) {
        try {
            FirebaseAuth.getInstance().generatePasswordResetLink(email);
        } catch (FirebaseAuthException e) {
            throw new CustomExceptions.AuthenticationException("Error generating password reset link: " + e.getMessage());
        }
    }
}