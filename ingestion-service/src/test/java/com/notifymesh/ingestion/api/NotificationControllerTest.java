package com.notifymesh.ingestion.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifymesh.ingestion.idempotency.IdempotencyService;
import com.notifymesh.ingestion.kafka.NotificationEventPublisher;
import com.notifymesh.vendor.NotificationChannel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.notifymesh.vendor.NotificationRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdempotencyService idempotencyService;

    @MockBean
    private NotificationEventPublisher publisher;

    @Test
    void acceptsValidRequestAndPublishesEvent() throws Exception {
        when(idempotencyService.tryClaim(anyString())).thenReturn(true);

        String requestBody = objectMapper.writeValueAsString(
                new NotificationRequestDto("req-1", NotificationChannel.SMS, "+10000000000", null, "hello"));

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value("req-1"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(publisher, times(1)).publish(captor.capture());
        assertEquals("req-1", captor.getValue().requestId());
        assertEquals(NotificationChannel.SMS, captor.getValue().channel());
    }

    @Test
    void generatesRequestIdWhenNotSupplied() throws Exception {
        when(idempotencyService.tryClaim(anyString())).thenReturn(true);

        String requestBody = objectMapper.writeValueAsString(
                new NotificationRequestDto(null, NotificationChannel.EMAIL, "a@example.com", "subj", "hello"));

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void returnsDuplicateWithoutPublishingWhenIdempotencyClaimFails() throws Exception {
        when(idempotencyService.tryClaim("req-dup")).thenReturn(false);

        String requestBody = objectMapper.writeValueAsString(
                new NotificationRequestDto("req-dup", NotificationChannel.SMS, "+10000000000", null, "hello"));

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-dup"))
                .andExpect(jsonPath("$.status").value("DUPLICATE"));

        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsRequestMissingRequiredFields() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new NotificationRequestDto("req-2", null, "", null, ""));

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.channel").exists())
                .andExpect(jsonPath("$.fields.recipient").exists())
                .andExpect(jsonPath("$.fields.body").exists());

        verifyNoInteractions(idempotencyService);
        verifyNoInteractions(publisher);
    }
}
