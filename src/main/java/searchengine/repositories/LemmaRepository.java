package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import searchengine.model.LemmaEntity;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_READ)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    Optional<LemmaEntity> findLemmaById(int Id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    Optional<LemmaEntity> findLemmaByLemmaAndSiteId(String lemma, int siteId);

    @Query(value = "select l.lemma as lemma, count(distinct i.page_id) as frequency from lemma l " +
            "join `index` i on i.lemma_id = l.id where l.lemma in :lemmaList group by l.lemma", nativeQuery = true)
    List<ILemmaFrequency> countLemmaFrequencyByLemmaList(Collection<String> lemmaList);

    int countLemmaBySiteId(int siteId);

}
