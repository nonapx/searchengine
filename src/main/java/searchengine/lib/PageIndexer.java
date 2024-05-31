package searchengine.lib;

import lombok.RequiredArgsConstructor;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import searchengine.config.WebClientSettings;
import searchengine.exceptions.CustomBadUrlException;
import searchengine.model.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class PageIndexer {

    private final String url;
    private final WebClientSettings webClientSettings;
    private final DatabaseRoutines databaseRoutines;

    private Document jsoupDocument;

    public void indexPage(PageEntity pageEntity) throws IOException {
        TextLemmatizer textLemmatizer = new TextLemmatizer();
        HashMap<String, Integer> lemmas = textLemmatizer.getLemmas(pageEntity.getContent());
        LemmaEntity lemmaEntity;
        List<IndexEntity> indexEntities = new ArrayList<>();
        for (String lemma : lemmas.keySet()) {
            lemmaEntity = databaseRoutines.addLemma(lemma, pageEntity.getSite());
            indexEntities.add(new IndexEntity(0, pageEntity, lemmaEntity, lemmas.get(lemma)));
        }
        databaseRoutines.getIndexRepository().saveAllAndFlush(indexEntities);
        SiteEntity siteEntity = pageEntity.getSite();
        siteEntity.setStatusTime(LocalDateTime.now());
        databaseRoutines.getSiteRepository().saveAndFlush(siteEntity);
    }

    public static String getDomain(String url) {
        Pattern pattern = Pattern.compile("(https?://[^/?]+).*");
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            return normalizeUrl(matcher.group(1));
        } else {
            throw(new CustomBadUrlException("Передан некорректный url"));
        }
    }

    public static String getPath(String url) {
        String path =  url.substring(getDomain(url).length() - 1);
        return path.isEmpty() ? "/" : path;
    }

    public int loadPage(String url) {
        int httpStatusCode;
        try {
            jsoupDocument = Jsoup.connect(url)
                    .userAgent(webClientSettings.getUserAgent())
                    .referrer(webClientSettings.getReferrer())
                    .get();
            httpStatusCode = HttpStatus.OK.value();
        } catch (UnsupportedMimeTypeException | MalformedURLException e1) {
            httpStatusCode =  HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();
        } catch (HttpStatusException e2) {
            httpStatusCode = e2.getStatusCode();
        } catch (SocketTimeoutException e3) {
            httpStatusCode = HttpStatus.REQUEST_TIMEOUT.value();
        } catch (IOException e3) {
            httpStatusCode = HttpStatus.NOT_FOUND.value();
        }
        return httpStatusCode;
    }

    public static String normalizeUrl(String url) {
        int anchorPosition = url.indexOf("#");
        if (anchorPosition > 0) {
            url = url.substring(0, anchorPosition);
        }
        if (!(url.matches(".+\\.html?|.+\\?.*") || url.endsWith("/"))) {
            url = url + "/";
        }
        return url;
    }

    public List<String> findLinks() {
        List<String> list  = new ArrayList<>();
        Elements elements = jsoupDocument.select("a[href^=" + getDomain(url) + "],a[href^=/]");
        elements.forEach(e -> {
            String link = e.attr("abs:href").toLowerCase();
            if (!link.matches(".*\\.(jpe?g|png|bmp|gif|tiff|svg|mp*|ogg|aac|flac|docx?|xlsx?)")) {
                link = normalizeUrl(link);
                if (!list.contains(link)) {
                    list.add(link);
                }
            }
        });
        return list;
    }

    public void clearPageLemmas(int pageId) {
        List<IndexEntity> indexEntityList = databaseRoutines
                .getIndexRepository()
                .findByPageId(pageId);
        List<LemmaEntity> lemmaEntitiesToDelete = indexEntityList
                .stream()
                .filter(ie -> ie.getLemma().getFrequency() == 1)
                .map(ie -> ie.getLemma())
                .toList();
        List<LemmaEntity> lemmaEntitiesToIncreaseFrequency = indexEntityList
                .stream()
                .filter(ie -> ie.getLemma().getFrequency() > 1)
                .map(ie -> ie.getLemma())
                .toList();
        databaseRoutines
                .getIndexRepository()
                .deleteAllInBatch(indexEntityList);
        databaseRoutines.getIndexRepository().flush();
        if (lemmaEntitiesToDelete.size() > 0) {
            databaseRoutines.getLemmaRepository().deleteAll(lemmaEntitiesToDelete);
        }
        if (lemmaEntitiesToIncreaseFrequency.size() > 0) {
            lemmaEntitiesToIncreaseFrequency.forEach(le -> le.setFrequency(le.getFrequency() + 1));
            databaseRoutines.getLemmaRepository().saveAllAndFlush(lemmaEntitiesToIncreaseFrequency);
        }
        databaseRoutines.getLemmaRepository().flush();
    }

    public String getPageHtml() {
        return jsoupDocument.html();
    }
}
