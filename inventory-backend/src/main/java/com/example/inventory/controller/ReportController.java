package com.example.inventory.controller;

import com.example.inventory.advice.ApiResponse;
import com.example.inventory.dto.ReportEmailRequest;
import com.example.inventory.service.EmailService;
import com.example.inventory.service.InventoryReportService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final InventoryReportService reportService;
    private final EmailService emailService;

    @GetMapping(value = "/inventory.csv", produces = "text/csv")
    public ResponseEntity<byte[]> downloadInventoryReport() {
        String fileName = reportService.createFileName();
        byte[] csv = reportService.createCsv();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName)
                                .build()
                                .toString()
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<Void>> emailInventoryReport(
            @Valid @RequestBody ReportEmailRequest request) {

        String fileName = reportService.createFileName();

        emailService.sendReport(
                request.getRecipientEmail(),
                reportService.createCsv(),
                fileName
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        new com.example.inventory.advice.ApiError(
                                org.springframework.http.HttpStatus.OK,
                                "Inventory report emailed successfully"
                        )
                )
        );
    }
}
