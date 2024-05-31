package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import searchengine.dto.SearchResponce;
import searchengine.dto.FoundPageResponceData;
import searchengine.exceptions.CustomBadRequestException;
import searchengine.lib.DatabaseRoutines;
import searchengine.lib.SiteTotalPages;
import searchengine.lib.TextLemmatizer;
import searchengine.model.SiteEntity;
import searchengine.repositories.IFoundPageData;
import searchengine.repositories.ILemmaFrequency;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
@Service
public class SearchService {

    private final DatabaseRoutines databaseRoutines;
    private final float HIGH_FREQUENCY_LEMMAS_PERCENT = 0.5F;

    public SearchResponce search(String query, String siteUrl, int offset, int limit) {
        if (query.isEmpty()) {
            throw(new CustomBadRequestException("Задан пустой поисковый запрос"));
        }

        HashMap<String, Integer> lemmas;
        TextLemmatizer textLemmatizer = null;
        try {
            textLemmatizer = new TextLemmatizer();
            lemmas = textLemmatizer.getLemmas(query);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка парсинга поисковой строки: " + e.getMessage());
        }

        SiteTotalPages siteTotalPages  = getSiteIdAndTotalPages(siteUrl);

        List<FoundPageResponceData> foundPageList = new ArrayList<>();
        SearchResponce searchResponce = new SearchResponce(true);
        searchResponce.setData(foundPageList);

        List<String> searchByLemmas = new ArrayList<>(lemmas.keySet());
        List<String> frequentlyOccurringLemmas = getFrequentlyOccurringLemmas(lemmas.keySet(), siteTotalPages.getTotalPages());
        if ( frequentlyOccurringLemmas.size() > 0 ) {
            searchByLemmas.removeAll(frequentlyOccurringLemmas);
        }
        Pageable pageable = PageRequest.of(offset/limit, limit);
        Page<IFoundPageData> pagesFound = databaseRoutines.getPageRepository().getPagesByLemmaListAndSite(searchByLemmas, searchByLemmas.size(), siteTotalPages.getId(), pageable);
        if (pagesFound.get().count() > 0) {
            List<IFoundPageData> pages = pagesFound.get().toList();
            float maxPageRank = pages.get(0).getRelevance();
            for (IFoundPageData page : pages) {
                String title = getPageTitle(page.getSnippet());
                String snippet = textLemmatizer.getSnippet(page.getSnippet(), searchByLemmas);
                foundPageList.add(mapToPageResponceData(page, title, snippet, maxPageRank));
            }
            searchResponce.setCount((int)pagesFound.getTotalElements());
        }

        return searchResponce;
    }

    private String getPageTitle(String pageHtml) {
        Document doc = Jsoup.parse(pageHtml);
        String title = "";
        Elements elements = doc.select("title");
        if (!elements.isEmpty()) {
            title = elements.get(0).html();
        }
        return title;
    }

    private List<String> getFrequentlyOccurringLemmas(Set<String> sourceList, int totalPages) {
        List<ILemmaFrequency> lemmaFrequencies = databaseRoutines.getLemmaRepository().countLemmaFrequencyByLemmaList(sourceList);
        return lemmaFrequencies
                .stream()
                .filter(lf -> (float)lf.getFrequency()/totalPages >= HIGH_FREQUENCY_LEMMAS_PERCENT)
                .map(lf -> lf.getLemma())
                .toList();
    }

    private FoundPageResponceData mapToPageResponceData (IFoundPageData page, String title, String snippet, float maxPageRank) {
        String siteUrl = page.getSite();
        siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
        return new FoundPageResponceData(
                siteUrl
                , page.getSiteName()
                , page.getUri()
                , title
                , snippet
                ,page.getRelevance() / maxPageRank
        );
    }

    private SiteTotalPages getSiteIdAndTotalPages(String url) {
        int totalPages = 0;
        int siteId = 0;
        if (url != null) {
            Optional<SiteEntity> siteEntityOptional = databaseRoutines.getSiteRepository().findByUrl(url);
            if (!siteEntityOptional.isPresent()) {
                throw new RuntimeException("Сайт отсутствует в списке индексированных сайтов");
            }
            siteId = siteEntityOptional.get().getId();
            totalPages = databaseRoutines.getPageRepository().countBySiteId(siteId);
        } else {
            totalPages = (int)databaseRoutines.getPageRepository().count();
        }
        return new SiteTotalPages(siteId, totalPages);
    }

}
