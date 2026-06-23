package com.smartinventory.controller;

import com.smartinventory.dto.response.ApiResponse;
import com.smartinventory.dto.response.MigrationResponse;
import com.smartinventory.service.impl.MigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
@Tag(name = "Migration", description = "Database seeding and migration endpoints")
public class MigrationController {

    private final MigrationService migrationService;

    @PostMapping("/seed")
    @Operation(summary = "Seed database with sample categories, products, users, and inventory")
    public ResponseEntity<ApiResponse<MigrationResponse>> seedDatabase(
            @RequestParam(defaultValue = "false") boolean force) {
        MigrationResponse result = migrationService.runMigration(force);
        return ResponseEntity.ok(ApiResponse.ok("Database migration completed successfully", result));
    }
}
