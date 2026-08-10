package com.trustplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = { "rate-limit.upload.capacity=100" })
@AutoConfigureMockMvc
public class ImageUploadSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    private byte[] createMockImageBytes(String format) {
        try {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                    10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB
            );
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, format, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"MANAGE_MEDIA"})
    public void whenUploadingValidJpeg_thenReturnsSuccess() throws Exception {
        byte[] content = createMockImageBytes("jpeg");
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("mediaType", "IMAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"MANAGE_MEDIA"})
    public void whenUploadingValidPng_thenReturnsSuccess() throws Exception {
        byte[] content = createMockImageBytes("png");
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", content);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("mediaType", "IMAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"MANAGE_MEDIA"})
    public void whenUploadingValidPdf_thenReturnsSuccess() throws Exception {
        // PDF magic bytes: 25 50 44 46 (%PDF)
        byte[] content = new byte[]{(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", content);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("mediaType", "IMAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"MANAGE_MEDIA"})
    public void whenUploadingInvalidFileWithJpegContentType_thenReturnsBadRequest() throws Exception {
        byte[] content = "malicious script content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "malicious.jpg", "image/jpeg", content);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("mediaType", "IMAGE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"MANAGE_MEDIA"})
    public void whenUploadingInvalidFileWithPngContentType_thenReturnsBadRequest() throws Exception {
        byte[] content = "malicious script content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "malicious.png", "image/png", content);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("mediaType", "IMAGE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"MANAGE_MEDIA"})
    public void whenUploadingUnsupportedFormat_thenReturnsBadRequest() throws Exception {
        byte[] content = "some plain text files".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("mediaType", "IMAGE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"MANAGE_MEDIA"})
    public void whenUploadingSvg_thenReturnsBadRequest() throws Exception {
        byte[] content = "<svg><script>alert(1)</script></svg>".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.svg", "image/svg+xml", content);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("mediaType", "IMAGE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = {"READ_CONTENT"})
    public void whenUploadingAsUserWithoutManageMedia_thenReturnsForbidden() throws Exception {
        byte[] content = createMockImageBytes("jpeg");
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("mediaType", "IMAGE"))
                .andExpect(status().isForbidden());
    }
}
