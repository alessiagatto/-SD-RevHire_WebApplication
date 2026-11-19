package com.RevHire.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.RevHire.entity.*;
import com.RevHire.repository.*;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResumeServiceImplTest {
    @Mock ResumeRepository resumeRepo;
    @Mock ResumeEducationRepository educationRepo;
    @Mock ResumeExperienceRepository experienceRepo;
    @Mock ResumeSkillRepository skillRepo;
    @Mock ResumeFileRepository resumeFileRepo;
    @Mock ResumeCertificationRepository certRepo;
    @Mock ResumeProjectRepository projectRepo;
    @Mock JobSeekerProfileRepository profileRepo;
    @Mock JobSeekerProfileRepository seekerRepository;
    @InjectMocks ResumeServiceImpl service;

    private Resume resume; private JobSeekerProfile seeker;
    @BeforeEach void setUp() {
        seeker = new JobSeekerProfile(); seeker.setSeekerId(10L);
        resume = new Resume(); resume.setResumeId(100L); resume.setSeeker(seeker); resume.setObjective("Objective");
    }

    @Test void createUpdateDelete() {
        when(resumeRepo.save(resume)).thenReturn(resume);
        assertEquals(resume,service.createResume(resume));
        when(resumeRepo.findById(100L)).thenReturn(Optional.of(resume));
        Resume update = new Resume(); update.setObjective("New objective");
        assertEquals("New objective",service.updateResume(100L,update).getObjective());
        when(resumeRepo.findById(101L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.updateResume(101L,update));
        service.deleteResume(100L); verify(resumeRepo).deleteById(100L);
    }

    @Test void getResumeByUserAndById() {
        when(profileRepo.findByUserUserId(1L)).thenReturn(Optional.of(seeker));
        when(seekerRepository.findByUserUserId(1L)).thenReturn(Optional.of(seeker));
        when(resumeRepo.findBySeeker_SeekerId(10L)).thenReturn(Optional.of(resume));
        when(resumeFileRepo.findByResume_ResumeIdOrderByUploadedAtDesc(100L)).thenReturn(List.of());
        assertEquals(resume,service.getResumeByUserId(1L));
        assertNotNull(resume.getFiles());
        when(profileRepo.findByUserUserId(2L)).thenReturn(Optional.empty());
        when(seekerRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.getResumeByUserId(2L));
        when(resumeRepo.findBySeeker_SeekerId(11L)).thenReturn(Optional.empty());
        seeker.setSeekerId(11L);
        when(profileRepo.findByUserUserId(3L)).thenReturn(Optional.of(seeker));
        when(seekerRepository.findByUserUserId(3L)).thenReturn(Optional.of(seeker));
        assertThrows(RuntimeException.class,()->service.getResumeByUserId(3L));
        when(resumeRepo.findById(100L)).thenReturn(Optional.of(resume));
        assertEquals(resume,service.getResumeById(100L));
        when(resumeRepo.findById(101L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.getResumeById(101L));
    }

    @Test void educationExperienceSkillCrudAndSaveSkills() {
        ResumeEducation edu=new ResumeEducation(); edu.setDegree("Degree");
        when(educationRepo.save(edu)).thenReturn(edu); assertEquals(edu,service.addEducation(edu));
        when(educationRepo.findByResume_ResumeId(100L)).thenReturn(List.of(edu)); assertEquals(1,service.getEducationByResume(100L).size());
        service.deleteEducation(1L); verify(educationRepo).deleteById(1L);
        ResumeExperience exp=new ResumeExperience(); exp.setRole("Engineer");
        when(experienceRepo.save(exp)).thenReturn(exp); assertEquals(exp,service.addExperience(exp));
        when(experienceRepo.findByResume_ResumeId(100L)).thenReturn(List.of(exp)); assertEquals(1,service.getExperienceByResume(100L).size());
        service.deleteExperience(1L); verify(experienceRepo).deleteById(1L);
        ResumeSkill skill=new ResumeSkill(); skill.setSkillName("Java");
        when(skillRepo.save(skill)).thenReturn(skill); assertEquals(skill,service.addSkill(skill));
        when(skillRepo.findByResume_ResumeId(100L)).thenReturn(List.of(skill)); assertEquals(1,service.getSkillsByResume(100L).size());
        service.deleteSkill(1L); verify(skillRepo).deleteById(1L);
        when(resumeRepo.findById(100L)).thenReturn(Optional.of(resume));
        service.saveSkills(100L,List.of(skill));
        verify(skillRepo).deleteByResume(resume); verify(skillRepo).saveAll(List.of(skill)); assertEquals(resume,skill.getResume());
        when(resumeRepo.findById(101L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.saveSkills(101L,List.of()));
    }

    @Test void certAndProjectCrud() {
        when(resumeRepo.findById(100L)).thenReturn(Optional.of(resume));
        ResumeCertification cert=new ResumeCertification(); cert.setCertificationName("Certificate");
        when(certRepo.save(cert)).thenReturn(cert); assertEquals(cert,service.addCertification(100L,cert)); assertEquals(resume,cert.getResume());
        when(certRepo.findByResumeResumeId(100L)).thenReturn(List.of(cert)); assertEquals(1,service.getCertifications(100L).size());
        service.deleteCertification(1L); verify(certRepo).deleteById(1L);
        ResumeProject project=new ResumeProject(); project.setProjectTitle("Project");
        when(projectRepo.save(project)).thenReturn(project); assertEquals(project,service.addProject(100L,project)); assertEquals(resume,project.getResume());
        when(projectRepo.findByResumeResumeId(100L)).thenReturn(List.of(project)); assertEquals(1,service.getProjects(100L).size());
        service.deleteProject(1L); verify(projectRepo).deleteById(1L);
        when(resumeRepo.findById(101L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.addCertification(101L,new ResumeCertification()));
        assertThrows(RuntimeException.class,()->service.addProject(101L,new ResumeProject()));
    }

    @Test void saveResumeFileMockMultipart() throws Exception {
        when(resumeRepo.findBySeeker_User_UserId(1L)).thenReturn(Optional.of(resume));
        MultipartFile file=mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("candidate.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(5L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("PDF".getBytes()));
        service.saveResumeFile(1L,file);
        verify(resumeFileRepo).save(argThat(f->f.getFileName().equals("candidate.pdf") && f.getFileType().equals("PDF")));
        when(resumeRepo.findBySeeker_User_UserId(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.saveResumeFile(2L,file));
    }

    @Test void deleteResumeFileExistsAndMissing() {
        when(resumeFileRepo.existsById(1L)).thenReturn(true);
        service.deleteResumeFile(1L); verify(resumeFileRepo).deleteById(1L); verify(resumeFileRepo).flush();
        when(resumeFileRepo.existsById(2L)).thenReturn(false);
        service.deleteResumeFile(2L); verify(resumeFileRepo,never()).deleteById(2L);
    }

    @Test void getResumeFileBytesAndDownload(@TempDir Path temp) throws Exception {
        Path pdf=temp.resolve("candidate.pdf"); Files.write(pdf,"PDFDATA".getBytes());
        ResumeFile rf=new ResumeFile(); rf.setFileId(2L); rf.setFileName("candidate.pdf"); rf.setFilePath(pdf.toString()); rf.setFileType("application/pdf");
        resume.setFiles(List.of(rf));
        when(resumeRepo.findBySeeker_SeekerId(10L)).thenReturn(Optional.of(resume));
        assertArrayEquals("PDFDATA".getBytes(),service.getResumeFileBytes(10L));
        when(resumeRepo.findBySeekerSeekerId(10L)).thenReturn(Optional.of(resume));
        ResponseEntity<Resource> response=service.downloadResumeFile(10L);
        assertEquals("application/pdf",response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("candidate.pdf"));
        resume.setFiles(List.of());
        assertThrows(RuntimeException.class,()->service.getResumeFileBytes(10L));
        assertThrows(RuntimeException.class,()->service.downloadResumeFile(10L));
        when(resumeRepo.findBySeeker_SeekerId(11L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.getResumeFileBytes(11L));
        when(resumeRepo.findBySeekerSeekerId(11L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.downloadResumeFile(11L));
    }

    @Test void resumeFileMetadataAndMissing() {
        ResumeFile rf=new ResumeFile(); rf.setFileId(1L);
        when(resumeFileRepo.findById(1L)).thenReturn(Optional.of(rf)); assertEquals(rf,service.getResumeFile(1L));
        when(resumeFileRepo.findById(2L)).thenReturn(Optional.empty()); assertThrows(RuntimeException.class,()->service.getResumeFile(2L));
    }

    @Test void generateHtmlAllSectionsAndErrors() {
        seeker.setFullName("Candidate"); seeker.setLocation("Remote"); seeker.setPhone("synthetic");
        ResumeEducation edu=new ResumeEducation(); edu.setDegree("Degree"); edu.setInstitution("Institute");
        ResumeExperience exp=new ResumeExperience(); exp.setRole("Engineer"); exp.setCompanyName("Example");
        ResumeProject project=new ResumeProject(); project.setProjectTitle("Project"); project.setDescription("Description");
        ResumeCertification cert=new ResumeCertification(); cert.setCertificationName("Certificate"); cert.setCompany("Issuer");
        ResumeSkill skill=new ResumeSkill(); skill.setSkillName("Java");
        when(resumeRepo.findById(100L)).thenReturn(Optional.of(resume));
        when(seekerRepository.findById(10L)).thenReturn(Optional.of(seeker));
        when(profileRepo.findById(10L)).thenReturn(Optional.of(seeker));
        when(educationRepo.findByResume_ResumeId(100L)).thenReturn(List.of(edu));
        when(experienceRepo.findByResume_ResumeId(100L)).thenReturn(List.of(exp));
        when(skillRepo.findByResume_ResumeId(100L)).thenReturn(List.of(skill));
        when(projectRepo.findByResumeResumeId(100L)).thenReturn(List.of(project));
        when(certRepo.findByResumeResumeId(100L)).thenReturn(List.of(cert));
        String html=service.generateResumeHtml(100L);
        assertTrue(html.contains("Candidate")); assertTrue(html.contains("Project")); assertTrue(html.contains("Java"));
        when(resumeRepo.findById(101L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.generateResumeHtml(101L));
        when(resumeRepo.findById(102L)).thenReturn(Optional.of(resume));
        when(profileRepo.findById(10L)).thenReturn(Optional.empty());
        when(seekerRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.generateResumeHtml(102L));
    }
}
