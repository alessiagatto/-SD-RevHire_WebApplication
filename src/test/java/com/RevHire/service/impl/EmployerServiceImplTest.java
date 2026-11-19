package com.RevHire.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.RevHire.dto.EmployerDashboardDTO;
import com.RevHire.dto.EmployerProfileDTO;
import com.RevHire.entity.EmployerProfile;
import com.RevHire.entity.User;
import com.RevHire.repository.ApplicationRepository;
import com.RevHire.repository.EmployerProfileRepository;
import com.RevHire.repository.JobRepository;
import com.RevHire.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EmployerServiceImplTest {
    @Mock EmployerProfileRepository employerProfileRepository;
    @Mock UserRepository userRepository;
    @Mock JobRepository jobRepository;
    @Mock ApplicationRepository applicationRepository;
    @InjectMocks EmployerServiceImpl service;

    private User user; private EmployerProfile profile; private EmployerProfileDTO dto;
    @BeforeEach void setUp() {
        user=new User(); user.setUserId(1L); user.setEmail("employer@example.test");
        profile=new EmployerProfile(); profile.setEmployerId(10L); profile.setUser(user);
        profile.setCompanyName("Example"); profile.setIndustry("Technology"); profile.setCompanySize(50);
        profile.setDescription("Synthetic"); profile.setWebsite("https://example.test"); profile.setLocation("Remote");
        dto=new EmployerProfileDTO(); dto.setCompanyName("Example"); dto.setIndustry("Technology"); dto.setCompanySize(50);
        dto.setDescription("Synthetic"); dto.setWebsite("https://example.test"); dto.setLocation("Remote"); dto.setContactEmail("new@example.test");
    }

    @Test void createOrUpdateExistingProfileAndNewProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employerProfileRepository.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        when(userRepository.save(user)).thenReturn(user); when(employerProfileRepository.save(profile)).thenReturn(profile);
        assertEquals(dto,service.createOrUpdateProfile(1L,dto)); assertEquals("new@example.test",user.getEmail());
        verify(employerProfileRepository).save(profile);
        when(employerProfileRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(new User()));
        EmployerProfileDTO noEmail=new EmployerProfileDTO(); noEmail.setCompanyName("New");
        assertEquals(noEmail,service.createOrUpdateProfile(2L,noEmail));
    }
    @Test void createOrUpdateNullEmptyEmailAndErrors() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employerProfileRepository.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        EmployerProfileDTO noEmail=new EmployerProfileDTO(); noEmail.setCompanyName("Example");
        assertEquals(noEmail,service.createOrUpdateProfile(1L,noEmail));
        verify(userRepository,never()).save(any());
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        ResponseStatusException ex=assertThrows(ResponseStatusException.class,()->service.createOrUpdateProfile(2L,dto));
        assertEquals(404,ex.getStatusCode().value());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException()).when(userRepository).save(user);
        ResponseStatusException conflict=assertThrows(ResponseStatusException.class,()->service.createOrUpdateProfile(1L,dto));
        assertEquals(409,conflict.getStatusCode().value());
    }
    @Test void getProfileSuccessMissingAndNullUser() {
        when(employerProfileRepository.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        EmployerProfileDTO result=service.getProfile(1L);
        assertEquals("Example",result.getCompanyName()); assertEquals("employer@example.test",result.getContactEmail());
        when(employerProfileRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.getProfile(2L));
        profile.setUser(null);
        when(employerProfileRepository.findByUserUserId(3L)).thenReturn(Optional.of(profile));
        assertNull(service.getProfile(3L).getContactEmail());
    }
    @Test void dashboardExistingProfileCountsCompletion() {
        when(employerProfileRepository.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        when(jobRepository.countByEmployerEmployerId(10L)).thenReturn(5L);
        when(jobRepository.countByEmployerEmployerIdAndStatus(10L,"OPEN")).thenReturn(3L);
        when(applicationRepository.countByJob_Employer_EmployerId(10L)).thenReturn(20L);
        when(applicationRepository.countByJob_Employer_EmployerIdAndStatus(10L,"PENDING")).thenReturn(4L);
        EmployerDashboardDTO dash=service.getDashboard(1L);
        assertEquals(5L,dash.getTotalJobs()); assertEquals(3L,dash.getActiveJobs()); assertEquals(20L,dash.getTotalApplications()); assertEquals(4L,dash.getPendingReviews());
        assertEquals(100.0,dash.getProfileCompletionPercentage());
    }
    @Test void dashboardCreatesDefaultAndUserMissing() {
        when(employerProfileRepository.findByUserUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        EmployerProfile created=new EmployerProfile(); created.setEmployerId(10L); created.setUser(user);
        when(employerProfileRepository.save(any(EmployerProfile.class))).thenReturn(created);
        when(jobRepository.countByEmployerEmployerId(10L)).thenReturn(0L);
        when(jobRepository.countByEmployerEmployerIdAndStatus(10L,"OPEN")).thenReturn(0L);
        when(applicationRepository.countByJob_Employer_EmployerId(10L)).thenReturn(0L);
        when(applicationRepository.countByJob_Employer_EmployerIdAndStatus(10L,"PENDING")).thenReturn(0L);
        EmployerDashboardDTO dash=service.getDashboard(1L); assertNotNull(dash); verify(employerProfileRepository).save(any());
        when(employerProfileRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.getDashboard(2L));
    }
    @Test void completionPartialAndEmpty() {
        when(employerProfileRepository.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        profile.setIndustry(null); profile.setWebsite(""); profile.setDescription(null);
        when(jobRepository.countByEmployerEmployerId(anyLong())).thenReturn(0L);
        when(jobRepository.countByEmployerEmployerIdAndStatus(anyLong(),eq("OPEN"))).thenReturn(0L);
        when(applicationRepository.countByJob_Employer_EmployerId(anyLong())).thenReturn(0L);
        when(applicationRepository.countByJob_Employer_EmployerIdAndStatus(anyLong(),eq("PENDING"))).thenReturn(0L);
        assertEquals(25.0,service.getDashboard(1L).getProfileCompletionPercentage());
        EmployerProfile empty=new EmployerProfile();
        when(employerProfileRepository.findByUserUserId(2L)).thenReturn(Optional.of(empty));
        // dashboard calls calculateCompletion with the user id and repository lookup by that id
        assertEquals(0.0,service.getDashboard(2L).getProfileCompletionPercentage());
    }
}
