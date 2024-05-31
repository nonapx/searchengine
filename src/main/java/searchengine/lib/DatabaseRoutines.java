package searchengine.lib;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Getter
@Setter
public class DatabaseRoutines {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deletePageLemma(IndexEntity indexEntity) {
        Optional<LemmaEntity> lemmaEntityOptional = lemmaRepository.findLemmaById(indexEntity.getLemma().getId());
        if (!lemmaEntityOptional.isPresent()) {
            return;
        }
        LemmaEntity lemmaEntity = lemmaEntityOptional.get();
        if (lemmaEntity.getFrequency() > 1) {
            lemmaEntity.setFrequency(lemmaEntity.getFrequency() - 1);
            lemmaRepository.save(lemmaEntity);
        } else {
            lemmaRepository.delete(lemmaEntity);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LemmaEntity addLemma(String lemma, SiteEntity site) {
        LemmaEntity lemmaEntity;
        Optional<LemmaEntity> lemmaEntityOptional = lemmaRepository.findLemmaByLemmaAndSiteId(lemma, site.getId());
        if (lemmaEntityOptional.isPresent()) {
            lemmaEntity = lemmaEntityOptional.get();
            lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
        } else {
            lemmaEntity = new LemmaEntity(0, site, lemma, 1);
        }
        return lemmaRepository.saveAndFlush(lemmaEntity);
    }

}
