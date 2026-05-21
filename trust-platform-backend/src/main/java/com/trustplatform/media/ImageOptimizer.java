package com.trustplatform.media;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

@Slf4j
@Service
public class ImageOptimizer {

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;

    public String optimizeAndSave(MultipartFile file, String uploadDir) throws IOException {
        // Ensure upload directory exists
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Read source image
        BufferedImage srcImg = ImageIO.read(file.getInputStream());
        if (srcImg == null) {
            throw new IllegalArgumentException("Invalid image file or format.");
        }

        // Rescale image if it exceeds max dimensions
        BufferedImage processedImg = scaleImageIfNeeded(srcImg);

        // Generate unique filename
        String uniqueId = UUID.randomUUID().toString();
        String webpFilename = uniqueId + ".webp";
        File targetFile = new File(dir, webpFilename);

        // Try writing in WebP format
        boolean success = writeImage(processedImg, "webp", targetFile, 0.75f);
        
        if (!success) {
            // Fallback to JPEG if WebP is not supported by JRE ImageIO
            log.warn("WebP format is not supported by the environment's ImageIO. Falling back to high-compression JPEG.");
            String jpegFilename = uniqueId + ".jpg";
            targetFile = new File(dir, jpegFilename);
            success = writeImage(processedImg, "jpeg", targetFile, 0.75f);
            if (!success) {
                // Final fallback: standard raw write
                ImageIO.write(processedImg, "jpg", targetFile);
            }
            return "/uploads/" + jpegFilename;
        }

        return "/uploads/" + webpFilename;
    }

    private BufferedImage scaleImageIfNeeded(BufferedImage srcImg) {
        int width = srcImg.getWidth();
        int height = srcImg.getHeight();

        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) {
            return srcImg;
        }

        double ratio = Math.min((double) MAX_WIDTH / width, (double) MAX_HEIGHT / height);
        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        
        // High quality rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(srcImg, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return scaled;
    }

    private boolean writeImage(BufferedImage image, String format, File destination, float quality) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            return false;
        }

        ImageWriter writer = writers.next();
        try (FileImageOutputStream output = new FileImageOutputStream(destination)) {
            writer.setOutput(output);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }

            writer.write(null, new IIOImage(image, null, null), param);
            return true;
        } catch (IOException e) {
            log.error("Failed to write image in format: " + format, e);
            return false;
        } finally {
            writer.dispose();
        }
    }
}
