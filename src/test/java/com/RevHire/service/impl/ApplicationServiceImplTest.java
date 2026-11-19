package com.RevHire.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.RevHire.dto.*;
import com.RevHire.entity.*;
import com.RevHire.repository.*;
import com.RevHire.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {
    @Mock ApplicationRepository applicationRepository;
    @Mock ApplicationNoteRepository applicationNoteRepository;
    @Mock JobRepository jobRepository;
    @Mock JobSeekerProfileRepository seekerRepository;
    @Mock ResumeRepository resumeRepository;
    @Mock NotificationService notificationService;
    @Mock EmployerProfileRepository employerRepository;
    @Mock ResumeFileRepository resumeFileRepository;
    @InjectMocks ApplicationServiceImpl service;

    private Job job; private JobSeekerProfile seeker; private Resume resume; private Application application;
    private EmployerProfile employer; private User seekerUser; private User employerUser;

    @BeforeEach void setUp() {
        employerUser = new User(); employerUser.setUserId(20L);
        employer = new EmployerProfile(); employer.setEmployerId(10L); employer.setCompanyName("Example"); employer.setUser(employerUser);
        job = new Job(); job.setJobId(100L); job.setTitle("Developer"); job.setEmployer(employer);
        seekerUser = new User(); seekerUser.setUserId(30L); seekerUser.setEmail("candidate@example.test");
        seeker = new JobSeekerProfile(); seeker.setSeekerId(40L); seeker.setFullName("Candidate"); seeker.setUser(seekerUser);
        resume = new Resume(); resume.setResumeId(50L); resume.setSeeker(seeker);
        application = new Application(); application.setApplicationId(60L); application.setJob(job); application.setSeeker(seeker); application.setResume(resume);
        application.setStatus("APPLIED"); application.setAppliedDate(LocalDateTime.of(2025,1,1,10,0));
    }

    private void stubValidApply() {
        when(applicationRepository.findByJobJobIdAndSeekerSeekerId(100L,30L)).thenReturn(Optional.empty());
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(seekerRepository.findByUserUserId(30L)).thenReturn(Optional.of(seeker));
        when(resumeRepository.findBySeeker_SeekerId(40L)).thenReturn(Optional.of(resume));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test void applySuccessWithAndWithoutFile() {
        stubValidApply();
        ResumeFile file = new ResumeFile(); file.setFileId(70L); file.setResume(resume);
        when(resumeFileRepository.findById(70L)).thenReturn(Optional.of(file));
        Application withFile = service.applyJob(100L,30L,50L,70L,"Cover");
        assertEquals("APPLIED", withFile.getStatus());
        verify(notificationService).sendNotification(20L,"Candidate applied for Developer");
        Application withoutFile = service.applyJob(100L,30L,50L,null,"Cover");
        assertNotNull(withoutFile);
        verify(applicationRepository, times(2)).save(any(Application.class));
    }

    @Test void applyDuplicateAndLookupErrors() {
        when(applicationRepository.findByJobJobIdAndSeekerSeekerId(100L,30L)).thenReturn(Optional.of(application));
        assertEquals("Already applied for this job", assertThrows(RuntimeException.class, () -> service.applyJob(100L,30L,50L,null,"")).getMessage());
        reset(applicationRepository, jobRepository);
        when(jobRepository.findById(100L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.applyJob(100L,30L,50L,null,""));
        reset(jobRepository, seekerRepository); when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(seekerRepository.findByUserUserId(30L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.applyJob(100L,30L,50L,null,""));
    }

    @Test void applyResumeAndFileErrors() {
        when(applicationRepository.findByJobJobIdAndSeekerSeekerId(100L,30L)).thenReturn(Optional.empty());
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(seekerRepository.findByUserUserId(30L)).thenReturn(Optional.of(seeker));
        when(resumeRepository.findBySeeker_SeekerId(40L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.applyJob(100L,30L,50L,null,""));
        reset(resumeRepository); when(resumeRepository.findBySeeker_SeekerId(40L)).thenReturn(Optional.of(resume));
        when(resumeFileRepository.findById(70L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.applyJob(100L,30L,50L,70L,""));
        ResumeFile wrong = new ResumeFile(); Resume wrongResume = new Resume(); wrongResume.setResumeId(999L); wrong.setResume(wrongResume);
        when(resumeFileRepository.findById(70L)).thenReturn(Optional.of(wrong));
        assertThrows(RuntimeException.class, () -> service.applyJob(100L,30L,50L,70L,""));
        resume.setSeeker(new JobSeekerProfile());
        resume.getSeeker().setSeekerId(999L);
        assertThrows(RuntimeException.class, () -> service.applyJob(100L,30L,50L,null,""));
    }

    @Test void seekerAndJobMappingsAndEmpty() {
        when(applicationRepository.findBySeekerSeekerId(40L)).thenReturn(List.of(application));
        List<ApplicationResponseDTO> seeker = service.getApplicationsBySeeker(40L);
        assertEquals("Candidate", seeker.get(0).getApplicantName());
        when(applicationRepository.findBySeekerSeekerId(40L)).thenReturn(List.of());
        assertTrue(service.getApplicationsBySeeker(40L).isEmpty());
        when(applicationRepository.findByJobJobId(100L)).thenReturn(List.of(application));
        assertEquals(60L, service.getApplicationsByJob(100L).get(0).getId());
    }

    @Test void withdrawSuccessAndMissing() {
        when(applicationRepository.findById(60L)).thenReturn(Optional.of(application));
        service.withdrawApplication(60L,"No longer available");
        assertEquals("WITHDRAWN",application.getStatus()); assertEquals("No longer available",application.getWithdrawReason());
        verify(applicationRepository).save(application);
        when(applicationRepository.findById(61L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.withdrawApplication(61L,""));
    }

    @Test void updateStatusBranches() {
        when(applicationRepository.findById(60L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);
        for (String status : List.of("SHORTLISTED","REJECTED","UNDER_REVIEW","APPLIED","WITHDRAWN")) {
            Application result = service.updateStatus(60L,status);
            assertEquals(status,result.getStatus());
        }
        verify(applicationRepository,atLeastOnce()).save(application);
        verify(notificationService,atLeastOnce()).sendNotification(eq(30L),anyString());
    }

    @Test void updateStatusErrorsAndInvalid() {
        when(applicationRepository.findById(60L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateStatus(60L,"APPLIED"));
        when(applicationRepository.findById(60L)).thenReturn(Optional.of(application));
        assertEquals("Invalid status value", assertThrows(RuntimeException.class, () -> service.updateStatus(60L,"INVALID")).getMessage());
    }

    @Test void employerMappingAndErrors() {
        when(employerRepository.findByUserUserId(20L)).thenReturn(Optional.of(employer));
        when(applicationRepository.findByJobEmployerEmployerId(10L)).thenReturn(List.of(application));
        EmployerApplicationDTO dto = service.getApplicationsByEmployer(20L).get(0);
        assertEquals(60L,dto.getApplicationId()); assertEquals(50L,dto.getResumeId());
        application.setResume(null);
        assertNull(service.getApplicationsByEmployer(20L).get(0).getResumeId());
        when(employerRepository.findByUserUserId(21L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getApplicationsByEmployer(21L));
    }

    @Test void employerNotesSuccessAndMissingEntities() {
        when(applicationRepository.findById(60L)).thenReturn(Optional.of(application));
        when(employerRepository.findById(10L)).thenReturn(Optional.of(employer));
        ArgumentCaptor<ApplicationNote> captor = ArgumentCaptor.forClass(ApplicationNote.class);
        assertEquals("Note added successfully",service.addEmployerNotes(60L,10L,"Synthetic note"));
        verify(applicationNoteRepository).save(captor.capture());
        assertEquals("Synthetic note",captor.getValue().getNoteText());
        when(applicationRepository.findById(61L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.addEmployerNotes(61L,10L,""));
        when(applicationRepository.findById(60L)).thenReturn(Optional.of(application));
        when(employerRepository.findById(11L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.addEmployerNotes(60L,11L,""));
    }
}
