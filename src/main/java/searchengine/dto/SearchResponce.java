package searchengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponce {
    private boolean result;
    private int count;
    private List<FoundPageResponceData> data;

    public SearchResponce(boolean result) {
        this.result = result;
    }
}
