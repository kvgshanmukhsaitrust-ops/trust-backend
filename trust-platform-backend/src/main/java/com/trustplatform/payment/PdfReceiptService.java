package com.trustplatform.payment;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.trustplatform.donation.Donation;
import com.trustplatform.donation.DonationRepository;
import com.trustplatform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReceiptService {

    private final DonationRepository donationRepository;

    @Value("${app.receipts.storage-path:./receipts}")
    private String storageBasePath;

    // ===================================
    // GENERATE PDF RECEIPT (or return cached)
    // ===================================
    public String generateOrGetReceipt(Long donationId) throws Exception {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        // If already generated, verify file still exists
        if (donation.getReceiptPdfPath() != null) {
            File existing = new File(donation.getReceiptPdfPath());
            if (existing.exists()) {
                log.info("[PdfReceiptService] Returning cached PDF for donationId={}", donationId);
                return donation.getReceiptPdfPath();
            }
            log.warn("[PdfReceiptService] Cached path missing on disk - regenerating for donationId={}", donationId);
        }

        // Deterministic filename from UUID
        String uuid = donation.getReceiptUuid() != null ? donation.getReceiptUuid() : donationId.toString();
        return generatePdf(donation, uuid);
    }

    private String generatePdf(Donation donation, String uuid) throws Exception {
        java.time.LocalDateTime created = donation.getCreatedAt() != null ? donation.getCreatedAt() : java.time.LocalDateTime.now();
        String year = String.valueOf(created.getYear());
        String month = String.format("%02d", created.getMonthValue());
        
        Path dir = Paths.get(storageBasePath, "archived", year, month);
        Files.createDirectories(dir);

        String filename = "receipt-" + uuid + ".pdf";
        String fullPath = dir.resolve(filename).toAbsolutePath().toString();

        Document document = new Document(PageSize.A4, 50, 50, 60, 60);
        try (FileOutputStream fos = new FileOutputStream(fullPath)) {
            PdfWriter.getInstance(document, fos);
            document.open();

            // Color Palette
            Color trustGold = new Color(176, 122, 63);
            Color navyDark = new Color(15, 23, 42);
            Color slate = new Color(100, 116, 139);
            Color success = new Color(16, 185, 129);

            // Fonts
            Font orgFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, trustGold);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9, slate);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, slate);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, navyDark);
            Font bigAmount = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, trustGold);
            Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, success);

            // Header
            Paragraph orgName = new Paragraph("K.V.G SHANMUKA SAI CHARITABLE TRUST", orgFont);
            orgName.setAlignment(Element.ALIGN_LEFT);
            document.add(orgName);

            document.add(new Paragraph("Nagpur, Maharashtra, India  \u2022  Govt Exemption Code: NGO-TRUST-4122", subFont));
            document.add(new Paragraph("CIN: U85300MH2020NPL12345  \u2022  80G Registration Active", subFont));
            document.add(new Chunk(new LineSeparator(1f, 100f, trustGold, Element.ALIGN_CENTER, -2)));
            document.add(Chunk.NEWLINE);

            // Title
            Paragraph title = new Paragraph("OFFICIAL DONATION RECEIPT - 80G TAX EXEMPTION CERTIFICATE",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, navyDark));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Receipt metadata
            addLabelValue(document, labelFont, valueFont, "Receipt Number",
                    donation.getReceiptNumber() != null ? donation.getReceiptNumber() : "PENDING");
            addLabelValue(document, labelFont, valueFont, "Receipt UUID", uuid);
            if (donation.getCreatedAt() != null) {
                addLabelValue(document, labelFont, valueFont, "Date of Donation",
                        donation.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
            }
            addLabelValue(document, labelFont, valueFont, "Payment Method",
                    donation.getPaymentMethod() != null ? donation.getPaymentMethod() : "Online Gateway");
            addLabelValue(document, labelFont, valueFont, "Transaction ID",
                    donation.getTransactionId() != null ? donation.getTransactionId() : "N/A");

            document.add(Chunk.NEWLINE);
            document.add(new Chunk(new LineSeparator(0.5f, 100f, new Color(226, 232, 240), Element.ALIGN_CENTER, -2)));
            document.add(Chunk.NEWLINE);

            // Donor details
            Paragraph donorHeader = new Paragraph("DONOR DETAILS", labelFont);
            document.add(donorHeader);
            addLabelValue(document, labelFont, valueFont, "Full Name", donation.getDonorName());
            addLabelValue(document, labelFont, valueFont, "Email", donation.getDonorEmail());
            if (donation.getDonorPan() != null) {
                String maskedPan = "XXXXX" + donation.getDonorPan().substring(5);
                addLabelValue(document, labelFont, valueFont, "PAN Card (80G)", maskedPan);
            }
            if (donation.getDonorAddress() != null) {
                addLabelValue(document, labelFont, valueFont, "Address", donation.getDonorAddress());
            }

            document.add(Chunk.NEWLINE);
            document.add(new Chunk(new LineSeparator(0.5f, 100f, new Color(226, 232, 240), Element.ALIGN_CENTER, -2)));
            document.add(Chunk.NEWLINE);

            // Amount
            Paragraph amtLabel = new Paragraph("TOTAL DONATION AMOUNT", labelFont);
            amtLabel.setAlignment(Element.ALIGN_RIGHT);
            document.add(amtLabel);

            Paragraph amt = new Paragraph("Rs. " + String.format("%,.2f", donation.getAmount()), bigAmount);
            amt.setAlignment(Element.ALIGN_RIGHT);
            document.add(amt);

            Paragraph statusP = new Paragraph("STATUS: " + donation.getStatus().name(), statusFont);
            statusP.setAlignment(Element.ALIGN_RIGHT);
            document.add(statusP);

            document.add(Chunk.NEWLINE);
            document.add(new Chunk(new LineSeparator(1f, 100f, trustGold, Element.ALIGN_CENTER, -2)));
            document.add(Chunk.NEWLINE);

            // 80G Notice
            Font noticeFont = FontFactory.getFont(FontFactory.HELVETICA, 8, slate);
            Font noticeBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, navyDark);
            document.add(new Paragraph("TAX EXEMPTION NOTICE UNDER SECTION 80G OF THE INCOME TAX ACT, 1961", noticeBold));
            document.add(new Paragraph(
                "This receipt serves as official legal proof of donation to K.V.G Shanmuka Sai Charitable Trust, " +
                "a registered non-profit organization under the Income Tax Act. 50% of this contribution is eligible " +
                "for a tax deduction under Section 80G. Please retain this certificate for your income tax filings.", noticeFont));

            document.add(Chunk.NEWLINE);

            // Campaign
            if (donation.getEvent() != null) {
                addLabelValue(document, labelFont, valueFont, "Campaign / Initiative", donation.getEvent().getTitle());
            }
            if (donation.getMessage() != null && !donation.getMessage().isBlank()) {
                addLabelValue(document, labelFont, valueFont, "Donor Message", donation.getMessage());
            }

            document.add(Chunk.NEWLINE);

            // Footer
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(148, 163, 184));
            Paragraph footer = new Paragraph(
                "This is a computer-generated receipt. No physical signature is required.  |  Generated: " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) +
                "  |  trust-platform v2.0", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        }

        // Persist path back to donation record
        donation.setReceiptPdfPath(fullPath);
        donationRepository.save(donation);
        log.info("[PdfReceiptService] PDF generated at: {}", fullPath);
        return fullPath;
    }

    private void addLabelValue(Document doc, Font labelFont, Font valueFont, String label, String value)
            throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ":  ", labelFont));
        p.add(new Chunk(value != null ? value : "-", valueFont));
        p.setSpacingAfter(4);
        doc.add(p);
    }
}
