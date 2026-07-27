package com.finsmart.controller;

import com.finsmart.service.PdfReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final PdfReportService pdfReportService;

    /**
     * GET /api/report/monthly?year=2024&month=5
     * Returns an HTML string. Frontend opens it in a new window and triggers window.print().
     */
    @GetMapping(value = "/monthly", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> monthlyReport(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {

        LocalDate now = LocalDate.now();
        int y = year  > 0 ? year  : now.getYear();
        int m = month > 0 ? month : now.getMonthValue();

        String html = pdfReportService.generateMonthlyReportHtml(ud.getUsername(), y, m);
        return ResponseEntity.ok(html);
    }
}
