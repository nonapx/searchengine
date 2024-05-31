package searchengine.repositories;

public interface IFoundPageData {
    String getSite();
    String getSiteName();
    String getUri();
    String getTitle();
    String getSnippet();
    float getRelevance();
}
