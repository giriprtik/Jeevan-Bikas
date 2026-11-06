package com.hospital.opd.service;

import com.hospital.opd.dto.LabResultItem;
import com.hospital.opd.dto.VisitLabResults;
import com.hospital.opd.model.Patient;
import com.hospital.opd.model.Visit;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LabReportPdfService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kathmandu");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private static final Color ABNORMAL = new Color(185, 28, 28);
    private static final Color HEADER_BG = new Color(241, 245, 249);
    private static final Color BORDER = new Color(203, 213, 225);
    private static final Color MUTED = new Color(100, 116, 139);

    private final Font fTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private final Font fSubtitle = FontFactory.getFont(FontFactory.HELVETICA, 11, MUTED);
    private final Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private final Font fLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, MUTED);
    private final Font fCell = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private final Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private final Font fAbnormal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, ABNORMAL);
    private final Font fSmall = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

    private final String hospitalName;

    public LabReportPdfService(@Value("${app.hospital.name:Jeevan Bikas Hospital}") String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public byte[] render(Visit visit, Patient patient, VisitLabResults lab) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph title = new Paragraph(hospitalName, fTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph subtitle = new Paragraph("Laboratory Investigation Report", fSubtitle);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(14);
            doc.add(subtitle);

            // Patient / order details
            PdfPTable info = new PdfPTable(new float[]{1.3f, 2.2f, 1.3f, 2.2f});
            info.setWidthPercentage(100);
            info.setSpacingAfter(14);
            addInfo(info, "Patient", patient.getFullName());
            addInfo(info, "MRN", patient.getMrn());
            addInfo(info, "Age / Sex", age(patient) + " / " + sex(patient));
            addInfo(info, "Blood group", orDash(patient.getBloodGroup()));
            addInfo(info, "Visit / Token", "#" + visit.getId() + " / " + visit.getTokenNumber());
            addInfo(info, "Lab order", "#" + lab.orderId());
            addInfo(info, "Ordered on", format(lab.orderedAt()));
            addInfo(info, "Referring doctor", orDash(lab.orderedByDoctor()));
            doc.add(info);

            // One table per test, in the order the results came back
            Map<String, List<LabResultItem>> byTest = lab.results().stream()
                    .collect(Collectors.groupingBy(LabResultItem::testCode, LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<String, List<LabResultItem>> entry : byTest.entrySet()) {
                List<LabResultItem> rows = entry.getValue();
                String testName = rows.get(0).testName() != null ? rows.get(0).testName() : entry.getKey();

                Paragraph section = new Paragraph(testName + " (" + entry.getKey() + ")", fSection);
                section.setSpacingBefore(6);
                section.setSpacingAfter(4);
                doc.add(section);

                PdfPTable table = new PdfPTable(new float[]{4f, 2f, 3.4f, 1.6f});
                table.setWidthPercentage(100);
                table.setHeaderRows(1);
                for (String h : List.of("Parameter", "Result", "Reference range", "Flag")) {
                    PdfPCell c = cell(h, fHead);
                    c.setBackgroundColor(HEADER_BG);
                    table.addCell(c);
                }
                for (LabResultItem r : rows) {
                    table.addCell(cell(r.parameterName(), fCell));
                    table.addCell(cell(r.observedValue(), r.abnormal() ? fAbnormal : fCell));
                    table.addCell(cell(orDash(r.referenceRange()), fCell));
                    table.addCell(cell(r.abnormal() ? "ABNORMAL" : "", fAbnormal));
                }
                doc.add(table);

                String notes = rows.stream()
                        .map(LabResultItem::technicianNotes)
                        .filter(n -> n != null && !n.isBlank())
                        .distinct()
                        .collect(Collectors.joining(" "));
                if (!notes.isEmpty()) {
                    Paragraph p = new Paragraph("Notes: " + notes, fSmall);
                    p.setSpacingBefore(3);
                    doc.add(p);
                }
            }

            // Footer
            String enteredBy = lab.results().stream()
                    .map(LabResultItem::resultEnteredBy).filter(Objects::nonNull).distinct()
                    .collect(Collectors.joining(", "));
            OffsetDateTime enteredAt = lab.results().stream()
                    .map(LabResultItem::resultEnteredAt).filter(Objects::nonNull)
                    .max(Comparator.naturalOrder()).orElse(null);

            Paragraph footer = new Paragraph(
                    "Results entered by " + orDash(enteredBy) + " on " + format(enteredAt) + ".", fSmall);
            footer.setSpacingBefore(18);
            doc.add(footer);
            doc.add(new Paragraph(
                    "Values marked ABNORMAL fall outside the reference range. Reference ranges shown are for adults "
                    + "and may vary by age and sex.", fSmall));
            doc.add(new Paragraph("Generated " + format(OffsetDateTime.now()) + ". Electronically generated report.", fSmall));
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate lab report PDF", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
        return out.toByteArray();
    }

    private void addInfo(PdfPTable table, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, fLabel));
        l.setBorder(PdfPCell.NO_BORDER);
        l.setPaddingBottom(4);
        PdfPCell v = new PdfPCell(new Phrase(orDash(value), fCell));
        v.setBorder(PdfPCell.NO_BORDER);
        v.setPaddingBottom(4);
        table.addCell(l);
        table.addCell(v);
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, font));
        c.setBorderColor(BORDER);
        c.setPadding(5);
        return c;
    }

    private static String age(Patient p) {
        LocalDate dob = p.getDateOfBirth();
        if (dob == null) return "-";
        int years = Period.between(dob, LocalDate.now(ZONE)).getYears();
        return years + "y" + (p.isDobEstimated() ? " (est.)" : "");
    }

    private static String sex(Patient p) {
        if (p.getGender() == null) return "-";
        String g = p.getGender().name();
        return g.charAt(0) + g.substring(1).toLowerCase();
    }

    private static String format(OffsetDateTime t) {
        return t == null ? "-" : t.atZoneSameInstant(ZONE).format(TS);
    }

    private static String orDash(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}