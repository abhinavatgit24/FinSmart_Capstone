package com.finsmart.controller;

import com.finsmart.dto.response.ApiResponse;
import com.finsmart.dto.response.CsvImportResult;
import com.finsmart.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final CsvImportService csvImportService;

    /**
     * POST /api/import/csv
     * Accepts multipart/form-data with field "file" containing the CSV.
     */
    @PostMapping("/csv")
    public ResponseEntity<ApiResponse<CsvImportResult>> importCsv(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam("file") MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Uploaded file is empty"));
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.contains("csv") && !contentType.contains("text")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only CSV files are accepted"));
        }

        CsvImportResult result = csvImportService.importCsv(ud.getUsername(), file);
        String message = result.getImported() + " transactions imported" +
                (result.getSkipped() > 0 ? ", " + result.getSkipped() + " skipped" : "");

        return ResponseEntity.ok(ApiResponse.ok(message, result));
    }
}
