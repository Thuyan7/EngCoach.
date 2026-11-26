package com.engcoach.vocab;

import com.engcoach.ai.AnthropicExplanationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Looks a word up so the learner does not have to type the meaning.
 * Meaning: Claude when an API key is configured (it sees the sentence, so it picks the right sense), otherwise the
 * free MyMemory translation service. Part of speech and pronunciation come from the free dictionaryapi.dev.
 * Every call is best effort with short timeouts; a failure just leaves that field empty.
 */
@Component
public class HttpWordLookupSource implements WordLookupSource {

    private static final Logger log = LoggerFactory.getLogger(HttpWordLookupSource.class);

    private final AnthropicExplanationService anthropic;
    private final RestClient translator = client("https://api.mymemory.translated.net");
    private final RestClient dictionary = client("https://api.dictionaryapi.dev");

    public HttpWordLookupSource(AnthropicExplanationService anthropic) {
        this.anthropic = anthropic;
    }

    private static RestClient client(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public WordLookup find(String word, String context) {
        String meaning = meaningFromClaude(word, context);
        if (meaning == null) {
            meaning = meaningFromTranslator(word);
        }
        String partOfSpeech = null;
        String phonetic = null;
        if (!word.contains(" ")) {
            JsonNode entry = dictionaryEntry(word);
            if (entry != null) {
                phonetic = text(entry.get("phonetic"));
                JsonNode meanings = entry.get("meanings");
                if (meanings != null && meanings.isArray() && !meanings.isEmpty()) {
                    partOfSpeech = text(meanings.get(0).get("partOfSpeech"));
                }
            }
        }
        return new WordLookup(meaning, partOfSpeech, phonetic);
    }

    private String meaningFromClaude(String word, String context) {
        String sentence = context == null || context.isBlank() ? "" : "\nSentence it appears in: \"" + context + "\"";
        String prompt = "Translate the English word or phrase \"" + word + "\" into Vietnamese, with the sense it has in "
                + "a TOEIC test." + sentence + "\nReply with only the Vietnamese meaning, at most 8 words, "
                + "no quotes and no explanation.";
        Optional<String> answer = anthropic.complete(prompt, 40);
        return answer.map(HttpWordLookupSource::oneLine).filter(s -> !s.isEmpty()).orElse(null);
    }

    private String meaningFromTranslator(String word) {
        try {
            JsonNode body = translator.get()
                    .uri("/get?q={q}&langpair=en|vi", word)
                    .retrieve().body(JsonNode.class);
            String translated = body == null ? null : text(body.path("responseData").get("translatedText"));
            if (translated == null || translated.equalsIgnoreCase(word) || translated.contains("MYMEMORY")) {
                return null;
            }
            return oneLine(translated);
        } catch (Exception ex) {
            log.warn("Translation lookup failed: {}", ex.getMessage());
            return null;
        }
    }

    private JsonNode dictionaryEntry(String word) {
        try {
            JsonNode body = dictionary.get().uri("/api/v2/entries/en/{word}", word).retrieve().body(JsonNode.class);
            return body != null && body.isArray() && !body.isEmpty() ? body.get(0) : null;
        } catch (Exception ex) {
            // Unknown words answer 404; that is normal, not worth a warning.
            log.debug("Dictionary lookup failed: {}", ex.getMessage());
            return null;
        }
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() || node.asText().isBlank() ? null : node.asText().trim();
    }

    private static String oneLine(String value) {
        String cleaned = value.replace("\"", "").replaceAll("\\s+", " ").trim();
        return cleaned.length() > 200 ? cleaned.substring(0, 200) : cleaned;
    }
}
