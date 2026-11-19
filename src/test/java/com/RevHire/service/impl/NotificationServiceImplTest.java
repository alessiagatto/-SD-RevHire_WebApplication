package com.RevHire.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.RevHire.dto.NotificationDTO;
import com.RevHire.entity.Notification;
import com.RevHire.entity.User;
import com.RevHire.repository.NotificationRepository;
import com.RevHire.repository.UserRepository;
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
class NotificationServiceImplTest {
    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @InjectMocks NotificationServiceImpl service;

    private User user;
    @BeforeEach void setUp() {
        user=new User(); user.setUserId(1L); user.setEmail("candidate@example.test");
    }

    @Test void sendNotificationSuccessCapturesState() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ArgumentCaptor<Notification> captor=ArgumentCaptor.forClass(Notification.class);
        service.sendNotification(1L,"Synthetic alert");
        verify(notificationRepository).save(captor.capture());
        Notification sent=captor.getValue();
        assertEquals(user,sent.getUser()); assertEquals("Synthetic alert",sent.getMessage()); assertFalse(sent.getIsRead());
    }
    @Test void sendNotificationUserMissing() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        RuntimeException ex=assertThrows(RuntimeException.class,()->service.sendNotification(2L,"Synthetic"));
        assertEquals("User not found with ID: 2",ex.getMessage());
        verify(notificationRepository,never()).save(any());
    }
    @Test void getNotificationsMapsAndEmpty() {
        Notification n=new Notification(); n.setNotificationId(10L); n.setUser(user); n.setMessage("Alert"); n.setIsRead(true);
        when(notificationRepository.findByUserUserId(1L)).thenReturn(List.of(n));
        List<NotificationDTO> dtos=service.getUserNotifications(1L);
        assertEquals(1,dtos.size()); assertEquals(10L,dtos.get(0).getNotificationId());
        assertEquals("Alert",dtos.get(0).getMessage()); assertTrue(dtos.get(0).getIsRead());
        when(notificationRepository.findByUserUserId(2L)).thenReturn(List.of());
        assertTrue(service.getUserNotifications(2L).isEmpty());
    }
    @Test void markAsReadSuccessAndMissing() {
        Notification n=new Notification(); n.setNotificationId(10L); n.setIsRead(false);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(n));
        service.markAsRead(10L); assertTrue(n.getIsRead()); verify(notificationRepository).save(n);
        when(notificationRepository.findById(11L)).thenReturn(Optional.empty());
        assertEquals("Notif not found",assertThrows(RuntimeException.class,()->service.markAsRead(11L)).getMessage());
        verify(notificationRepository,never()).save(argThat(x->x.getNotificationId()!=null && x.getNotificationId()==11L));
    }
}
