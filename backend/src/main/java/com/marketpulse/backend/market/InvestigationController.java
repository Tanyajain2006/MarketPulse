package com.marketpulse.backend.market;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/investigations")
public class InvestigationController {
    private final InvestigationService investigations;

    public InvestigationController(InvestigationService investigations) { this.investigations = investigations; }

    @GetMapping("/{ticker}")
    public InvestigationDtos.InvestigationResponse investigate(Authentication authentication,
            @PathVariable String ticker, @RequestParam Long watchlistId) {
        return investigations.investigate(authentication.getName(), ticker, watchlistId);
    }
}