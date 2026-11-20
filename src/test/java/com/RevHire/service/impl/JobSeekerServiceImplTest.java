package com.RevHire.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.RevHire.dto.*;
import com.RevHire.entity.*;
import com.RevHire.repository.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobSeekerServiceImplTest {
    @Mock JobSeekerProfileRepository profileRepo;
    @Mock ResumeRepository resumeRepo;
    @Mock ResumeFileRepository resumeFileRepo;
    @Mock FavoriteJobRepository favoriteJobRepo;
    @Mock NotificationRepository notificationRepo;
    @Mock UserRepository userRepo;
    @Mock JobRepository jobRepo;
    @Mock ResumeSkillRepository resumeSkillRepo;
    @Mock JobSeekerProfileRepository seekerRepository;
    JobSeekerServiceImpl service;

    private User user; private JobSeekerProfile profile; private Job job;
    @BeforeEach void setUp() {
        service = new JobSeekerServiceImpl(profileRepo, resumeRepo, resumeFileRepo, favoriteJobRepo, notificationRepo, userRepo, jobRepo, resumeSkillRepo, seekerRepository);
        user=new User(); user.setUserId(1L); user.setEmail("candidate@example.test");
        profile=new JobSeekerProfile(); profile.setSeekerId(10L); profile.setFullName("Candidate"); profile.setUser(user);
        job=new Job(); job.setJobId(20L); job.setTitle("Java Developer"); job.setLocation("Remote");
        job.setSalaryMin(BigDecimal.valueOf(40000)); job.setSalaryMax(BigDecimal.valueOf(70000)); job.setJobType("FULL_TIME"); job.setStatus("OPEN");
    }

    @Test void createProfileSuccessAndUserMissing() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(profileRepo.save(profile)).thenReturn(profile);
        when(seekerRepository.save(profile)).thenReturn(profile);
        assertEquals(profile,service.createProfile(profile,1L)); assertEquals(user,profile.getUser());
        when(userRepo.findById(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.createProfile(profile,2L));
    }
    @Test void getProfilePresentAndEmpty() {
        when(profileRepo.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        when(seekerRepository.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        assertEquals(profile,service.getProfile(1L).orElseThrow());
        when(seekerRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        when(profileRepo.findByUserUserId(2L)).thenReturn(Optional.empty());
        assertTrue(service.getProfile(2L).isEmpty());
    }
    @Test void updateProfileCopiesFieldsAndMissing() {
        when(profileRepo.findById(10L)).thenReturn(Optional.of(profile));
        when(seekerRepository.findById(10L)).thenReturn(Optional.of(profile));
        JobSeekerProfile update=new JobSeekerProfile(); update.setFullName("Updated"); update.setPhone("x"); update.setLocation("Here");
        update.setCurrentEmploymentStatus("Available"); update.setTotalExperience(5); update.setProfileCompletion(90);
        when(profileRepo.save(profile)).thenReturn(profile);
        when(seekerRepository.save(profile)).thenReturn(profile);
        assertEquals("Updated",service.updateProfile(10L,update).getFullName());
        assertEquals(5,profile.getTotalExperience());
        when(seekerRepository.findById(11L)).thenReturn(Optional.empty());
        when(profileRepo.findById(11L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.updateProfile(11L,update));
    }
    @Test void getOrCreateResumeExistingNewAndProfileMissing() {
        Resume resume=new Resume(); resume.setSeeker(profile);
        when(profileRepo.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        when(seekerRepository.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        when(resumeRepo.findBySeekerSeekerId(10L)).thenReturn(Optional.of(resume));
        assertEquals(resume,service.getOrCreateResume(1L));
        when(resumeRepo.findBySeekerSeekerId(10L)).thenReturn(Optional.empty());
        when(resumeRepo.save(any(Resume.class))).thenAnswer(i->i.getArgument(0));
        Resume created=service.getOrCreateResume(1L); assertEquals(profile,created.getSeeker());
        when(seekerRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        when(profileRepo.findByUserUserId(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.getOrCreateResume(2L));
    }
    @Test void uploadResumeFileSuccessAndIoError() throws Exception {
        Resume resume=new Resume(); resume.setResumeId(30L);
        when(profileRepo.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        when(seekerRepository.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        when(resumeRepo.findBySeekerSeekerId(10L)).thenReturn(Optional.of(resume));
        MultipartFile file=mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("candidate.pdf"); when(file.getSize()).thenReturn(100L);
        ResumeFile saved=new ResumeFile(); saved.setResume(resume);
        when(resumeFileRepo.save(any(ResumeFile.class))).thenAnswer(i->i.getArgument(0));
        ResumeFile result=service.uploadResumeFile(1L,file);
        assertEquals(resume,result.getResume()); assertEquals("PDF",result.getFileType());
        verify(file).transferTo(any(java.io.File.class));
        doThrow(new IOException("disk")).when(file).transferTo(any(java.io.File.class));
        assertThrows(RuntimeException.class,()->service.uploadResumeFile(1L,file));
        when(profileRepo.findByUserUserId(2L)).thenReturn(Optional.empty());
        when(seekerRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.uploadResumeFile(2L,file));
    }
    @Test void favoriteSuccessDuplicateMissingAndMapping() {
        FavoriteJob fav=new FavoriteJob(); fav.setFavId(50L); fav.setSeeker(profile); fav.setJob(job);
        when(seekerRepository.findByUserUserId(1L)).thenReturn(Optional.of(profile));
        when(jobRepo.findById(20L)).thenReturn(Optional.of(job));
        when(favoriteJobRepo.existsBySeekerSeekerIdAndJobJobId(1L,20L)).thenReturn(false);
        when(favoriteJobRepo.save(any(FavoriteJob.class))).thenAnswer(i->i.getArgument(0));
        assertEquals(job,service.addFavoriteJob(1L,20L).getJob());
        when(favoriteJobRepo.existsBySeekerSeekerIdAndJobJobId(1L,20L)).thenReturn(true);
        assertThrows(RuntimeException.class,()->service.addFavoriteJob(1L,20L));
        when(seekerRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        when(profileRepo.findByUserUserId(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.addFavoriteJob(2L,20L));
        when(jobRepo.findById(21L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.addFavoriteJob(1L,21L));
        when(favoriteJobRepo.findBySeekerSeekerId(10L)).thenReturn(List.of(fav));
        List<FavoriteJobDTO> dtos=service.getFavorites(10L);
        assertEquals(1,dtos.size()); assertEquals("Java Developer",dtos.get(0).getTitle());
        when(favoriteJobRepo.findBySeekerSeekerId(11L)).thenReturn(List.of());
        assertTrue(service.getFavorites(11L).isEmpty());
        service.removeFavoriteJob(50L); verify(favoriteJobRepo).deleteById(50L);
    }
    @Test void notificationReadSuccessMissingAndEmptyFavoritesCompanyNull() {
        Notification n=new Notification(); n.setNotificationId(60L); n.setIsRead(false);
        when(notificationRepo.findById(60L)).thenReturn(Optional.of(n));
        service.markNotificationAsRead(60L); assertTrue(n.getIsRead()); verify(notificationRepo).save(n);
        when(notificationRepo.findById(61L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.markNotificationAsRead(61L));
        FavoriteJob fav=new FavoriteJob(); fav.setFavId(50L); fav.setJob(job); job.setEmployer(null);
        when(favoriteJobRepo.findBySeekerSeekerId(10L)).thenReturn(List.of(fav));
        assertEquals("Unknown Company",service.getFavorites(10L).get(0).getCompanyName());
    }
    @Test void searchJobsNullAndValuesAndRecommended() {
        when(jobRepo.findAdvanced(eq("%Java%"),eq("%Remote%"),eq(3),eq("%Degree%"),
                eq(BigDecimal.valueOf(40000.0)),eq(BigDecimal.valueOf(70000.0)),eq("%FULL_TIME%"),eq("OPEN")))
                .thenReturn(List.of(job));
        assertEquals(1,service.searchJobs("Java","Remote",3,"Degree",40000.0,70000.0,"FULL_TIME").size());
        when(jobRepo.findAdvanced(eq("%"),eq("%"),eq(0),eq("%"),eq(BigDecimal.ZERO),
                eq(BigDecimal.valueOf(9999999)),eq("%"),eq("OPEN"))).thenReturn(List.of());
        assertTrue(service.searchJobs(null,null,null,null,null,null,null).isEmpty());
        when(jobRepo.findAll()).thenReturn(List.of(job));
        assertEquals(1,service.getRecommendedJobs(List.of("java")).size());
        assertTrue(service.getRecommendedJobs(List.of("python")).isEmpty());
        assertTrue(service.getRecommendedJobs(List.of()).isEmpty());
        assertTrue(service.getRecommendedJobs(null).isEmpty());
    }
}
