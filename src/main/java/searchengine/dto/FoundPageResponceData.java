package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import searchengine.repositories.IFoundPageData;

@AllArgsConstructor
@Getter
@Setter
public class FoundPageResponceData implements IFoundPageData {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;
}
