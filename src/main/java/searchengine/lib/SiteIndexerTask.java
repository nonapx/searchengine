package searchengine.lib;

import lombok.RequiredArgsConstructor;
import searchengine.config.WebClientSettings;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class SiteIndexerTask implements Runnable {

    private final SiteEntity siteEntity;
    private final DatabaseRoutines databaseRoutines;
    private final WebClientSettings webClientSettings;
    private final List<ForkJoinPool> poolList;

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        SiteRepository siteRepository = databaseRoutines.getSiteRepository();
        ForkJoinPool pool = new ForkJoinPool();
        poolList.add(pool);
        PageIndexerAction linksFinder = new PageIndexerAction(siteEntity.getUrl(), databaseRoutines, webClientSettings, siteEntity);
        try {
            pool.execute(linksFinder);
            var result = linksFinder.join();
        } catch (Exception e) {
            siteEntity.setStatus(IndexingStatus.FAILED);
            siteEntity.setLastError(e.getMessage());
        } finally {
            if (pool.isShutdown()) {
                siteEntity.setStatus(IndexingStatus.FAILED);
                siteEntity.setLastError("Индексация остановлена пользователем");
            } else {
                siteEntity.setStatus(IndexingStatus.INDEXED);
            }
            poolList.remove(pool);
            pool.shutdown();
        }
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(siteEntity);
        System.out.println("Индексация сайта \"" + siteEntity.getUrl() + "\", длительность " + (System.currentTimeMillis() - startTime) + " мс");
    }

}
