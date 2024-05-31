package searchengine.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    Optional<PageEntity> findBySiteIdAndPath(int siteId, String path);

    int countBySiteId(int siteId);

    @Query(value = "select s.url as site, s.name as siteName, p.path as uri, '' as title, p.content as snippet, q.relevance as relevance \n" +
            "from page p " +
            " join ( " +
            "select i.page_id, count(distinct l.lemma), sum(i.`rank`) relevance " +
            "from lemma l " +
            "join `index` i on i.lemma_id = l.id " +
            "where l.lemma in :lemmasList " +
            "and (l.site_id = :siteId or :siteId = 0) " +
            "group by i.page_id having count(distinct l.lemma) = :lemmasCount" +
            ") q on p.id = q.page_id " +
            "join site s on s.id = p.site_id " +
            "order by q.relevance desc"
            , nativeQuery = true
            , countQuery = "select count(1) from (" +
            "select i.page_id, count(distinct l.lemma) " +
            "from lemma l " +
            "join `index` i on i.lemma_id = l.id " +
            "where l.lemma in :lemmasList " +
            "and (l.site_id = :siteId or :siteId = 0) " +
            "group by i.page_id " +
            "having count(distinct l.lemma) = :lemmasCount" +
            ") q")
    Page<IFoundPageData> getPagesByLemmaListAndSite(Collection<String> lemmasList, int lemmasCount, int siteId, Pageable pageable);


}
