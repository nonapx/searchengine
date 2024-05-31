package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.PageUrl;
import searchengine.dto.SearchResponce;
import searchengine.dto.ShortResponce;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<ShortResponce> startIndexing() {
        indexingService.start();
        return ResponseEntity.ok(new ShortResponce(true));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<ShortResponce> stopIndexing() {
        indexingService.stop();
        return ResponseEntity.ok(new ShortResponce(true));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<ShortResponce> indexPage(@ModelAttribute PageUrl pageUrl, Model model) {
        model.addAttribute("url", pageUrl);
        indexingService.indexPage(pageUrl.getUrl());
        return ResponseEntity.ok(new ShortResponce(true));
    }

    @GetMapping("/search")
    public SearchResponce search(@RequestParam String query,
                                 @RequestParam(required = false) String site,
                                 @RequestParam int offset,
                                 @RequestParam int limit) {

        return searchService.search(query, site, offset, limit);
    }

}
