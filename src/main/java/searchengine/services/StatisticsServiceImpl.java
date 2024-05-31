package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.lib.DatabaseRoutines;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final DatabaseRoutines databaseRoutines;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        boolean isIndexing = false;
        for(int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            Optional<SiteEntity> siteEntityOptional = databaseRoutines.getSiteRepository().findByUrl(site.getUrl());
            if (siteEntityOptional.isPresent()) {
                SiteEntity siteEntity = siteEntityOptional.get();
                if (siteEntity.getStatus() == IndexingStatus.INDEXING) isIndexing = true;
                int pages = databaseRoutines.getPageRepository().countBySiteId(siteEntity.getId());
                int lemmas = databaseRoutines.getLemmaRepository().countLemmaBySiteId(siteEntity.getId());
                item.setPages(pages);
                item.setLemmas(lemmas);
                item.setStatus(siteEntity.getStatus());
                item.setError(siteEntity.getLastError());
                item.setStatusTime(ZonedDateTime.of(siteEntity.getStatusTime(), ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli());
                total.setPages(total.getPages() + pages);
                total.setLemmas(total.getLemmas() + lemmas);
                detailed.add(item);
            }
        }
        total.setIndexing(isIndexing);

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
