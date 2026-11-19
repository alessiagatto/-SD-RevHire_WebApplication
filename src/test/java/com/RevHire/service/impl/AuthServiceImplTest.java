package com.RevHire.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.RevHire.entity.User;
import com.RevHire.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthServiceImpl service;

    private User user;
    @BeforeEach void setUp() {
        user=new User(); user.setUserId(1L); user.setEmail("candidate@example.test");
        user.setPasswordHash("PlainPassword"); user.setSecurityAnswerHash("synthetic-answer");
    }

    @Test void registerSuccessDuplicateAndEncoding() {
        when(userRepository.existsByEmail("candidate@example.test")).thenReturn(false);
        when(passwordEncoder.encode("PlainPassword")).thenReturn("$2a$synthetic");
        when(userRepository.save(user)).thenReturn(user);
        assertEquals("$2a$synthetic",service.registerUser(user).getPasswordHash());
        verify(passwordEncoder).encode("PlainPassword");
        when(userRepository.existsByEmail("candidate@example.test")).thenReturn(true);
        assertEquals("Email already registered",assertThrows(RuntimeException.class,()->service.registerUser(user)).getMessage());
    }

    @Test void loginBcryptSuccessAndWrongPassword() {
        user.setPasswordHash("$2a$synthetic");
        when(userRepository.findByEmail("candidate@example.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("PlainPassword","$2a$synthetic")).thenReturn(true);
        assertEquals(user,service.login("candidate@example.test","PlainPassword"));
        when(passwordEncoder.matches("Wrong","$2a$synthetic")).thenReturn(false);
        assertEquals("Invalid credentials",assertThrows(RuntimeException.class,()->service.login("candidate@example.test","Wrong")).getMessage());
    }

    @Test void loginBcrypt2bBranch() {
        user.setPasswordHash("$2b$synthetic");
        when(userRepository.findByEmail("candidate@example.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("PlainPassword","$2b$synthetic")).thenReturn(true);
        assertEquals(user,service.login("candidate@example.test","PlainPassword"));
    }

    @Test void loginPlainTextUpgradesAndNotFound() {
        when(userRepository.findByEmail("candidate@example.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("PlainPassword")).thenReturn("$2b$new");
        assertEquals(user,service.login("candidate@example.test","PlainPassword"));
        assertEquals("$2b$new",user.getPasswordHash()); verify(userRepository).save(user);
        when(userRepository.findByEmail("missing@example.test")).thenReturn(Optional.empty());
        assertEquals("Invalid credentials",assertThrows(RuntimeException.class,()->service.login("missing@example.test","x")).getMessage());
    }

    @Test void resetPasswordSuccessWrongAnswerAndMissing() {
        when(userRepository.findByEmail("candidate@example.test")).thenReturn(Optional.of(user));
        assertEquals("Password updated successfully",service.resetPassword("candidate@example.test","synthetic-answer","NewPassword"));
        assertEquals("NewPassword",user.getPasswordHash()); verify(userRepository).save(user);
        assertThrows(RuntimeException.class,()->service.resetPassword("candidate@example.test","wrong","NewPassword"));
        when(userRepository.findByEmail("missing@example.test")).thenReturn(Optional.empty());
        assertEquals("User not found",assertThrows(RuntimeException.class,()->service.resetPassword("missing@example.test","x","y")).getMessage());
    }

    @Test void updatePasswordSuccessWrongAndMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        service.updatePassword(1L,"PlainPassword","NewPassword");
        assertEquals("NewPassword",user.getPasswordHash()); verify(userRepository).save(user);
        assertEquals("The current password you entered is incorrect",assertThrows(RuntimeException.class,()->service.updatePassword(1L,"Wrong","New")).getMessage());
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertEquals("User session expired or user not found",assertThrows(RuntimeException.class,()->service.updatePassword(2L,"x","y")).getMessage());
    }

    @Test void deleteUserSuccessAndMissing() {
        when(userRepository.existsById(1L)).thenReturn(true);
        service.deleteUser(1L); verify(userRepository).deleteById(1L);
        when(userRepository.existsById(2L)).thenReturn(false);
        assertEquals("User not found",assertThrows(RuntimeException.class,()->service.deleteUser(2L)).getMessage());
        verify(userRepository,never()).deleteById(2L);
    }
}
