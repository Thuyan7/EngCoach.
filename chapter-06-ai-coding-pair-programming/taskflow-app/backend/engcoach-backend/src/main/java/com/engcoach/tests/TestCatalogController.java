package com.engcoach.tests;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/tests")
public class TestCatalogController {
    private final TestCatalogService testCatalogService;

    public TestCatalogController(TestCatalogService testCatalogService) {
        this.testCatalogService = testCatalogService;
    }

    @GetMapping
    public ResponseEntity<List<ToeicTestView>> listTests() {
        return ResponseEntity.ok(testCatalogService.listTests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ToeicTestView> getTest(@PathVariable Long id) {
        return ResponseEntity.ok(testCatalogService.getTest(id));
    }
}
