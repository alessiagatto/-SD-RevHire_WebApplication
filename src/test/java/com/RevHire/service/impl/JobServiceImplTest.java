package com.RevHire.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.RevHire.dto.JobDTO;
import com.RevHire.entity.EmployerProfile;
import com.RevHire.entity.Job;
import com.RevHire.repository.EmployerProfileRepository;
import com.RevHire.repository.JobRepository;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {
    @Mock private JobRepository jobRepository;
    @Mock private EmployerProfileRepository employerRepository;
    @InjectMocks private JobServiceImpl jobService;

    private EmployerProfile employer;
    private Job job;

    @BeforeEach
    void setUp() {
        employer = new EmployerProfile();
        employer.setEmployerId(10L);
        employer.setCompanyName("Example Corp");
        job = new Job();
        job.setJobId(100L);
        job.setTitle("Developer");
        job.setLocation("Remote");
        job.setJobType("FULL_TIME");
        job.setStatus("OPEN");
        job.setActive(true);
        job.setEmployer(employer);
    }

    @Test void createJobSetsDefaults() {
        when(employerRepository.findByUserUserId(1L)).thenReturn(Optional.of(employer));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        Job result = jobService.createJob(job, 1L);
        verify(jobRepository).save(captor.capture());
        assertEquals(employer,captor.getValue().getEmployer());
        assertEquals("OPEN",captor.getValue().getStatus());
        assertTrue(captor.getValue().getActive());
        assertEquals(employer, result.getEmployer());
        assertEquals("OPEN", result.getStatus());
        assertTrue(result.getActive());
    }

    @Test void createJobEmployerMissing() {
        when(employerRepository.findByUserUserId(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> jobService.createJob(job, 1L));
    }

    @Test void getAllOpenJobsMapsDtosAndEmpty() {
        when(jobRepository.findByStatus("OPEN")).thenReturn(List.of(job));
        List<JobDTO> result = jobService.getAllOpenJobs();
        assertEquals(1, result.size());
        assertEquals("Developer", result.get(0).getTitle());
        when(jobRepository.findByStatus("OPEN")).thenReturn(List.of());
        assertTrue(jobService.getAllOpenJobs().isEmpty());
    }

    @Test void searchJobsPassesFiltersAndMapsDtos() {
        when(jobRepository.advancedSearch("Dev", "Remote", 2, "Degree", 40.0, 80.0, "FULL_TIME"))
                .thenReturn(List.of(job));
        List<JobDTO> result = jobService.searchJobs("Dev", "Remote", 2, "Degree", 40.0, 80.0, "FULL_TIME");
        assertEquals(1, result.size());
    }

    @Test void closeJobSuccessAndMissing() {
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        jobService.closeJob(100L);
        assertEquals("CLOSED", job.getStatus());
        verify(jobRepository).save(job);
        when(jobRepository.findById(101L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> jobService.closeJob(101L));
    }

    @Test void deleteJobSuccessAndMissing() {
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        jobService.deleteJob(100L);
        verify(jobRepository).delete(job);
        when(jobRepository.findById(101L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> jobService.deleteJob(101L));
    }

    @Test void getJobsByUserAndEmployerMissing() {
        when(employerRepository.findByUserUserId(1L)).thenReturn(Optional.of(employer));
        when(jobRepository.findByEmployerEmployerId(10L)).thenReturn(List.of(job));
        assertEquals(1, jobService.getJobsByUserId(1L).size());
        when(employerRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> jobService.getJobsByUserId(2L));
    }

    @Test void sortedBranchesNameRecentDefault() {
        when(employerRepository.findByUserUserId(1L)).thenReturn(Optional.of(employer));
        Job beta = new Job(); beta.setJobId(102L); beta.setTitle("Beta"); beta.setEmployer(employer);
        Job alpha = new Job(); alpha.setJobId(101L); alpha.setTitle("Alpha"); alpha.setEmployer(employer);
        when(jobRepository.findByEmployerEmployerIdOrderByTitleAsc(10L)).thenReturn(List.of(alpha,beta));
        when(jobRepository.findByEmployerEmployerIdOrderByJobIdDesc(10L)).thenReturn(List.of(beta,alpha));
        when(jobRepository.findByEmployerEmployerId(10L)).thenReturn(List.of(beta,alpha));
        List<JobDTO> byName = jobService.getEmployerJobsSorted(1L, "NAME");
        assertEquals(List.of("Alpha","Beta"), List.of(byName.get(0).getTitle(),byName.get(1).getTitle()));
        List<JobDTO> recent = jobService.getEmployerJobsSorted(1L, "recent");
        assertEquals(List.of("Beta","Alpha"), List.of(recent.get(0).getTitle(),recent.get(1).getTitle()));
        List<JobDTO> defaultOrder = jobService.getEmployerJobsSorted(1L, "other");
        assertEquals(List.of("Beta","Alpha"), List.of(defaultOrder.get(0).getTitle(),defaultOrder.get(1).getTitle()));
        when(employerRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> jobService.getEmployerJobsSorted(2L, "name"));
    }

    @Test void toggleStatusBothBranchesAndMissing() {
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        JobDTO closed = jobService.toggleJobStatus(100L);
        assertEquals("CLOSED", closed.getStatus());
        assertFalse(job.getActive());
        JobDTO opened = jobService.toggleJobStatus(100L);
        assertEquals("OPEN", opened.getStatus());
        assertTrue(job.getActive());
        when(jobRepository.findById(101L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> jobService.toggleJobStatus(101L));
    }

    @Test void getByIdSuccessAndMissing() {
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        assertEquals("Developer", jobService.getJobById(100L).getTitle());
        when(jobRepository.findById(101L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jobService.getJobById(101L));
        assertTrue(ex.getMessage().contains("101"));
    }

    @Test void updateJobCopiesFieldsAndMissing() {
        Job updated = new Job();
        updated.setTitle("Senior Developer"); updated.setLocation("Hybrid");
        updated.setJobType("CONTRACT"); updated.setDescription("Synthetic");
        updated.setExperienceRequired(5); updated.setEducationRequired("Degree");
        updated.setSalaryMin(java.math.BigDecimal.valueOf(50000));
        updated.setSalaryMax(java.math.BigDecimal.valueOf(90000));
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        JobDTO result = jobService.updateJob(100L, updated);
        assertEquals("Senior Developer", result.getTitle());
        assertEquals("Hybrid", job.getLocation());
        assertEquals("CONTRACT", job.getJobType());
        assertEquals(java.math.BigDecimal.valueOf(50000),job.getSalaryMin());
        assertEquals(java.math.BigDecimal.valueOf(90000),job.getSalaryMax());
        assertEquals("Synthetic",job.getDescription());
        assertEquals(5,job.getExperienceRequired());
        assertEquals("Degree",job.getEducationRequired());
        when(jobRepository.findById(101L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> jobService.updateJob(101L, updated));
    }

    @Test void recommendedMapsDtosAndEmpty() {
        when(jobRepository.findRecommendedJobs("Java")).thenReturn(List.of(job));
        assertEquals(1, jobService.getRecommendedJobs("Java").size());
        when(jobRepository.findRecommendedJobs("Unknown")).thenReturn(List.of());
        assertTrue(jobService.getRecommendedJobs("Unknown").isEmpty());
    }
}
