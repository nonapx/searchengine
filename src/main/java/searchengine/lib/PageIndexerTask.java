package searchengine.lib;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import searchengine.config.WebClientSettings;
import searchengine.model.IndexingStatus;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.time.LocalDateTime;

import static searchengine.model.IndexingStatus.INDEXING;

@AllArgsConstructor
public class PageIndexerTask implements  Runnable {
    private WebClientSettings webClientSettings;
    private DatabaseRoutines databaseRoutines;
    private PageEntity pageEntity;

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        SiteEntity siteEntity = pageEntity.getSite();
        siteEntity.setStatus(INDEXING);
        siteEntity.setLastError("");
        siteEntity.setStatusTime(LocalDateTime.now());
        databaseRoutines.getSiteRepository().saveAndFlush(siteEntity);

        String url = siteEntity.getUrl() + pageEntity.getPath();

        PageIndexer pageIndexer = new PageIndexer(url, webClientSettings, databaseRoutines);
        try {
            pageIndexer.clearPageLemmas(pageEntity.getId());
            int httpStatusCode = pageIndexer.loadPage(url);
            pageEntity.setCode(httpStatusCode);
            if (httpStatusCode != HttpStatus.OK.value()) {
                siteEntity.setStatus(IndexingStatus.FAILED);
                siteEntity.setLastError("Ошибка загрузки страницы");
            } else {
                pageEntity.setContent(pageIndexer.getPageHtml());
                pageIndexer.indexPage(pageEntity);
                siteEntity.setStatus(IndexingStatus.INDEXED);
            }
        } catch (Exception e) {
            siteEntity.setStatus(IndexingStatus.FAILED);
            siteEntity.setLastError(e.getMessage());
        }
        siteEntity.setStatusTime(LocalDateTime.now());
        databaseRoutines.getSiteRepository().saveAndFlush(siteEntity);
        databaseRoutines.getPageRepository().saveAndFlush(pageEntity);
        System.out.println("Индексация страницы произведена за " + (System.currentTimeMillis() - startTime) + " мс");
    }
}
