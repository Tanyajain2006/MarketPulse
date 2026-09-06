package com.marketpulse.backend.research;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/research")
public class ResearchController {
    private final ResearchService service;

    public ResearchController(ResearchService service) { this.service = service; }

    @GetMapping("/search")
    public ResearchService.ResearchResponse search(@RequestParam String q) { return service.search(q); }
}
