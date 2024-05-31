package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.*;

import searchengine.exceptions.CustomBadRequestException;
import searchengine.exceptions.CustomBadUrlException;
import searchengine.lib.DatabaseRoutines;
import searchengine.lib.SiteIndexerTask;
import searchengine.lib.PageIndexer;
import searchengine.lib.PageIndexerTask;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import static searchengine.model.IndexingStatus.INDEXED;
import static searchengine.model.IndexingStatus.INDEXING;

@RequiredArgsConstructor
@Service
public class IndexingService {

    private final SitesList siteList;
    private final WebClientSettings webClientSettings;
    private final DatabaseRoutines databaseRoutines;

    private List<ForkJoinPool> poolList = new ArrayList<>();
    private Thread pageIndexingThread = null;

    public void start() {
        SiteRepository siteRepository = databaseRoutines.getSiteRepository();
        List<SiteEntity> indexedSites = siteRepository.findAllByStatus(INDEXING);
        if (!indexedSites.isEmpty()) {
            throw(new CustomBadRequestException("Индексация уже запущена"));
        }
        siteRepository.deleteAll();
        siteRepository.flush();
        for (Site site : siteList.getSites()) {
            SiteEntity siteEntity = siteRepository.saveAndFlush(new SiteEntity(site.getUrl(), site.getName()));
            new Thread(new SiteIndexerTask(siteEntity, databaseRoutines, webClientSettings, poolList)).start();
        }
    }

    public void stop() {
        SiteRepository siteRepository = databaseRoutines.getSiteRepository();
        List<SiteEntity> indexedSites = siteRepository.findAllByStatus(INDEXING);
        if (indexedSites.isEmpty()) {
            throw (new CustomBadRequestException("Индексация не запущена"));
        }
        for (ForkJoinPool pool : poolList) {
            pool.shutdownNow();
        }
        poolList.clear();
        if (pageIndexingThread != null && pageIndexingThread.isAlive()) {
            throw (new CustomBadRequestException("Нельзя отстановить отдельно запущенную индексацию одной страницы"));
        }
    }

    public void indexPage(String url) {
        String normalizedUrl = PageIndexer.normalizeUrl(url);
        String domain = PageIndexer.getDomain(normalizedUrl);
        if (!siteList.getSites().stream().anyMatch(s -> s.getUrl().contains(domain))) {
            throw (new CustomBadUrlException("Данная страница находится за пределами сайтов, "
                    + "указанных в конфигурационном файле"));
        }
        PageEntity pageEntity = findOrCreatePageEntityByUrl(normalizedUrl);
        SiteEntity siteEntity = pageEntity.getSite();
        if (siteEntity.getStatus() == INDEXING) {
            throw new CustomBadRequestException("Сайт уже уже индексируется");
        }
        pageIndexingThread = new Thread(new PageIndexerTask(webClientSettings, databaseRoutines, pageEntity));
        pageIndexingThread.start();
    }

    private PageEntity findOrCreatePageEntityByUrl(String url) {
        String domain = PageIndexer.getDomain(url);
        String path = PageIndexer.getPath(url);
        SiteEntity siteEntity = null;
        PageEntity pageEntity = null;
        Optional<SiteEntity> siteEntityOptional = databaseRoutines
                .getSiteRepository()
                .findByUrl(domain);
        if (siteEntityOptional.isPresent()) {
            siteEntity = siteEntityOptional.get();
            Optional<PageEntity> pageEntityOptional = databaseRoutines
                    .getPageRepository()
                    .findBySiteIdAndPath(siteEntity.getId(), path);
            if (pageEntityOptional.isPresent()) {
                pageEntity = pageEntityOptional.get();
            }
        } else {
            siteEntity = new SiteEntity(0, INDEXED, LocalDateTime.now(), "", domain, domain);
            databaseRoutines.getSiteRepository().saveAndFlush(siteEntity);
        }
        if (pageEntity == null) {
            pageEntity = new PageEntity(0, siteEntity, path, 0, "");
            databaseRoutines.getPageRepository().saveAndFlush(pageEntity);
        }
        return pageEntity;
    }

}
