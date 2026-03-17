package com.zebratic.sensekeyboard

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple word suggestion engine.
 * - Learns words the user types
 * - Suggests based on prefix matching
 * - Persists learned words in SharedPreferences
 */
class WordSuggestions(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("word_freq", Context.MODE_PRIVATE)
    private val wordFrequency = mutableMapOf<String, Int>()

    // Common English words as a baseline
    private val baseWords = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see",
        "other", "than", "then", "now", "look", "only", "come", "its", "over",
        "think", "also", "back", "after", "use", "two", "how", "our", "work",
        "first", "well", "way", "even", "new", "want", "because", "any", "these",
        "give", "day", "most", "us", "great", "thanks", "hello", "hey", "yes",
        "please", "sorry", "okay", "sure", "right", "yeah", "nice", "cool",
        "awesome", "love", "really", "very", "much", "more", "here", "where",
        "why", "help", "need", "let", "still", "should", "before", "too", "does",
        "didn't", "don't", "won't", "can't", "isn't", "wasn't", "aren't",
        "search", "youtube", "netflix", "settings", "home", "play", "pause", "stop",
        "open", "close", "next", "back", "menu", "ok", "cancel", "done", "password",
        "email", "name", "address", "phone", "message", "send", "delete", "edit"
    )

    // Common Danish words
    private val baseDanishWords = setOf(
        "og", "i", "at", "det", "er", "en", "til", "på", "med", "for",
        "den", "har", "de", "ikke", "af", "et", "var", "jeg", "han", "som",
        "vi", "kan", "der", "fra", "sig", "hun", "men", "blev", "vil", "så",
        "skal", "her", "alle", "være", "have", "også", "når", "efter", "om",
        "godt", "havde", "min", "kun", "mange", "meget", "hvor", "eller",
        "hvad", "dag", "hej", "tak", "ja", "nej", "godmorgen", "godaften",
        "velkommen", "undskyld", "farvel", "venligst"
    )

    init {
        // Load learned words
        prefs.all.forEach { (word, freq) ->
            if (freq is Int) wordFrequency[word] = freq
        }
    }

    // Track word pairs for next-word prediction
    private val nextWordFreq = mutableMapOf<String, MutableMap<String, Int>>()
    private var lastWord = ""

    fun learnWord(word: String) {
        val w = word.lowercase().trim()
        if (w.length < 2) return
        val freq = (wordFrequency[w] ?: 0) + 1
        wordFrequency[w] = freq
        prefs.edit().putInt(w, freq).apply()

        // Learn word pair
        if (lastWord.isNotEmpty()) {
            val pairs = nextWordFreq.getOrPut(lastWord) { mutableMapOf() }
            pairs[w] = (pairs[w] ?: 0) + 1
            // Persist pair
            prefs.edit().putInt("pair_${lastWord}_$w", pairs[w]!!).apply()
        }
        lastWord = w
    }

    /**
     * Get next-word predictions after a completed word.
     */
    fun getNextWordSuggestions(afterWord: String, limit: Int = 5): List<String> {
        val w = afterWord.lowercase().trim()
        val pairs = nextWordFreq[w]

        // Also check persisted pairs
        val candidates = mutableMapOf<String, Int>()
        if (pairs != null) candidates.putAll(pairs)

        // Add common follow-up words from base
        val commonFollowers = mapOf(
            "i" to listOf("am", "have", "want", "need", "think", "know", "will", "can", "don't"),
            "the" to listOf("best", "most", "first", "last", "new", "old", "same", "other"),
            "is" to listOf("a", "the", "not", "very", "really", "so", "too", "also"),
            "to" to listOf("the", "be", "do", "go", "get", "make", "see", "have"),
            "it" to listOf("is", "was", "will", "can", "would", "should"),
            "you" to listOf("are", "can", "have", "want", "need", "know", "should"),
            "that" to listOf("is", "was", "the", "it", "you", "we", "I"),
            "what" to listOf("is", "are", "do", "did", "about", "the"),
            "how" to listOf("are", "do", "is", "about", "much", "many", "long"),
            "thank" to listOf("you", "god"),
            "thanks" to listOf("for", "a", "so"),
            "good" to listOf("morning", "night", "evening", "afternoon", "luck", "job"),
            "can" to listOf("you", "I", "we", "not", "be"),
            "do" to listOf("you", "not", "it", "we", "this"),
        )

        commonFollowers[w]?.forEach { word ->
            if (word !in candidates) candidates[word] = 1
        }

        return candidates.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    fun resetHistory() {
        wordFrequency.clear()
        nextWordFreq.clear()
        lastWord = ""
        prefs.edit().clear().apply()
    }

    fun getSuggestions(prefix: String, limit: Int = 5): List<String> {
        val p = prefix.lowercase().trim()
        if (p.isEmpty()) return emptyList()

        // Combine all sources
        val candidates = mutableMapOf<String, Int>()

        // Learned words (highest priority via frequency)
        wordFrequency.forEach { (word, freq) ->
            if (word.startsWith(p) && word != p) {
                candidates[word] = freq * 10 // boost learned words
            }
        }

        // Base words
        baseWords.forEach { word ->
            if (word.startsWith(p) && word != p && word !in candidates) {
                candidates[word] = 1
            }
        }
        baseDanishWords.forEach { word ->
            if (word.startsWith(p) && word != p && word !in candidates) {
                candidates[word] = 1
            }
        }

        return candidates.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }
}
