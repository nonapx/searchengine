package searchengine.lib;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextLemmatizer {
    private final LuceneMorphology luceneMorphRus = new RussianLuceneMorphology();
    private final LuceneMorphology luceneMorphEng = new EnglishLuceneMorphology();

    private final String RESERVED_WORDS_MASK = "submit|save|ok|search|cancel|clear|&nbsp;|&lt;|&gt|lt|gt|nbsp|up|down|left|right;";
    private final int MAX_SENTENCE_OFFSET = 100;
    private final int MINIMAL_TOKEN_LENGTH = 2;
    private final int MAX_SNIPPET_LENGTH = 300;

    public TextLemmatizer() throws IOException {
    }

    public HashMap<String, Integer> getLemmas(String htmlText) {
        HashMap<String, Integer> lemmas = new HashMap<>();
        String clearText = getPureText(htmlText)
                .toLowerCase(Locale.ROOT);
        String[] tokens = clearText.split("[^A-Za-zА-Яа-я0-9]+");
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            String lemma = getTokenNormalForm(token);
            if (!lemma.isEmpty()) {
                lemmas.put(lemma, lemmas.containsKey(lemma) ? lemmas.get(lemma) + 1 : 1);
            }
        }
        return lemmas;
    }

    private String getTokenNormalForm(String token) {
        String lemma = "";
        if (luceneMorphRus.checkString(token)) {
            List<String> morphInfo = luceneMorphRus.getMorphInfo(token);
            if (!morphInfo
                    .get(0)
                    .matches(".*(СОЮЗ|ЧАСТ|МЕЖД|ПРЕДЛ)")) {
                lemma = luceneMorphRus.getNormalForms(token).get(0);
            }
        } else if (luceneMorphEng.checkString(token)) {
            List<String> morphInfo = luceneMorphEng.getMorphInfo(token);
            if (!morphInfo
                    .get(0)
                    .matches(".*(CONJ|PREP|ARTICLE|PART)")) {
                lemma = luceneMorphEng.getNormalForms(token).get(0);
            }
        } else if (token.length() >= MINIMAL_TOKEN_LENGTH) {
            // Считаем, что попали на имя собственное
            lemma = token;
        }
        return lemma;
    }

    public String getSnippet(String htmlText, List<String> lemmasToMark) {
        String clearText = getPureText(htmlText);
        String[] tokens = clearText.split("[^A-Za-zА-Яа-я0-9]+");
        String firstTokenFound = "";
        HashSet<String> tokensForMark = new HashSet<>();
        for (String token : tokens) {
            String lemma = getTokenNormalForm(token.toLowerCase(Locale.ROOT));
            if (lemma.isEmpty()) continue;
            if (lemmasToMark.contains(lemma)) {
                firstTokenFound = token;
                tokensForMark.add(token);
                break;
            }
        }
        int sentenceStartPosition = getSentenceStartPosition(clearText, firstTokenFound);
        int firstTokenPosition = clearText.indexOf(firstTokenFound);
        int snippetStartPosition = firstTokenPosition - sentenceStartPosition <= MAX_SENTENCE_OFFSET ? sentenceStartPosition : firstTokenPosition;
        String snippet = clearText.substring(snippetStartPosition, snippetStartPosition + Math.min(clearText.length() - snippetStartPosition, MAX_SNIPPET_LENGTH));
        snippet = snippet.substring(0, snippet.lastIndexOf(" "));
        String[] snippetTokens = snippet.split("[^A-Za-zА-Яа-я0-9]+");
        for (String token : snippetTokens) {
            if (!tokensForMark.contains(token)) {
                String lemma = getTokenNormalForm(token.toLowerCase(Locale.ROOT));
                if (lemma.isEmpty()) continue;
                if (lemmasToMark.contains(lemma)) {
                    tokensForMark.add(token);
                }
            }
        }
        for (String token : tokensForMark) {
            snippet = snippet.replace(token, "<b>" + token + "</b>");
        }
        return snippet;
    }

    private int getSentenceStartPosition(String text, String token) {
        int position = 0;
        if (token.startsWith(token.substring(0, 1).toUpperCase())) {
            position = text.indexOf(token);
        } else {
            Pattern pattern = Pattern.compile(".*([А-ЯA-Z][^А-ЯA-Z]*" + token + ").*");
            Matcher matcher = pattern.matcher(text);
            if (matcher.matches()) {
                position = text.indexOf(matcher.group(1));
            }
        }
        return position;
    }

    public String getPureText(String htmlText) {
        String buffer = htmlText.replaceAll(RESERVED_WORDS_MASK, " ");
        return Jsoup.clean(buffer.replace(">", "> "), Safelist.none());
    }

}
