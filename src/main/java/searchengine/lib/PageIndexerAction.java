package searchengine.lib;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import searchengine.config.WebClientSettings;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

public class PageIndexerAction extends RecursiveAction  {

    private DatabaseRoutines databaseRoutines;
    private WebClientSettings webClientSettings;

    private final String url;

    List<PageIndexerAction> tasks = new ArrayList<PageIndexerAction>();
    private String path;
    private PageRepository pageRepository;
    private SiteEntity siteEntity;

    public PageIndexerAction(String url, DatabaseRoutines databaseRoutines, WebClientSettings webClientSettings, SiteEntity siteEntity) {
        this.url = url;
        this.databaseRoutines = databaseRoutines;
        this.webClientSettings = webClientSettings;
        this.siteEntity = siteEntity;
        pageRepository = databaseRoutines.getPageRepository();
        path = PageIndexer.getPath(url);
    }

    @Override
    protected void compute() {
        Optional<PageEntity> pageEntityOptional = pageRepository.findBySiteIdAndPath(siteEntity.getId(), path);
        if (pageEntityOptional.isPresent()) return;
        PageIndexer pageIndexer = new PageIndexer(url, webClientSettings, databaseRoutines);
        int httpStatusCode = pageIndexer.loadPage(url);
        if (httpStatusCode == HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()) {
            return;
        }
        PageEntity pageEntity = null;
        try {
            pageEntity = pageRepository.saveAndFlush(new PageEntity(0, siteEntity, path, httpStatusCode, pageIndexer.getPageHtml()));
        } catch (ConstraintViolationException | DataIntegrityViolationException e) {
            return;
        }
        if (httpStatusCode != HttpStatus.OK.value()) return;
        for (String link : pageIndexer.findLinks()) {
            waitBetweenPageLoads();
            PageIndexerAction task = new PageIndexerAction(link, databaseRoutines, webClientSettings, siteEntity);
            tasks.add(task);
            task.fork();
        }
        try {
            pageIndexer.indexPage(pageEntity);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка индексации страницы \"" + url + "\" " + e.getMessage());
        }
        tasks.forEach(t -> t.join());
    }

    private void waitBetweenPageLoads() {
        try {
            TimeUnit.MILLISECONDS.sleep(webClientSettings.getTimeout());
        } catch (InterruptedException e) {
            throw new RuntimeException("Ожидание прервано");
        }
    }

}
