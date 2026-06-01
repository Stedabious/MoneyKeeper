package com.moneykeeper.app.data.notification

import com.moneykeeper.app.data.database.entity.RegexPatternEntity
import com.moneykeeper.app.data.repository.RegexPatternRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads user-defined patterns from the pattern library into memory and applies them during parsing.
 *
 * Patterns stored in the DB are tried after built-in parsers. If a built-in parser failed to
 * extract an amount (PARTIAL_PARSE) but a user-defined AMOUNT pattern matches, that match is used
 * to rescue the result and boost status to at least LOW_CONFIDENCE.
 */
@Singleton
class UserPatternEngine @Inject constructor(
    repository: RegexPatternRepository,
) {
    data class UserMatch(
        val patternId: Long,
        val patternString: String,
        val patternType: String,
        val matchedText: String,
        val extractedValue: String?,
    )

    data class PatternAttempt(
        val entity: RegexPatternEntity,
        val matched: Boolean,
        val matchedText: String?,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var amountPatterns: List<RegexPatternEntity> = emptyList()
    @Volatile private var merchantPatterns: List<RegexPatternEntity> = emptyList()
    @Volatile private var allPatterns: List<RegexPatternEntity> = emptyList()

    init {
        scope.launch {
            repository.getAll().collect { patterns ->
                allPatterns = patterns
                amountPatterns = patterns.filter { it.patternType == "AMOUNT" }
                merchantPatterns = patterns.filter { it.patternType == "MERCHANT" }
            }
        }
    }

    /** Try each user-defined AMOUNT pattern; return first match. */
    fun tryMatchAmount(text: String): UserMatch? {
        for (entity in amountPatterns) {
            val match = tryMatch(entity, text) ?: continue
            val extracted = if (match.groupValues.size > 1) match.groupValues[1] else match.value
            return UserMatch(entity.id, entity.patternString, entity.patternType, match.value, extracted)
        }
        return null
    }

    /** Try each user-defined MERCHANT pattern; return first match. */
    fun tryMatchMerchant(text: String): UserMatch? {
        for (entity in merchantPatterns) {
            val match = tryMatch(entity, text) ?: continue
            val extracted = if (match.groupValues.size > 1) match.groupValues[1] else match.value
            return UserMatch(entity.id, entity.patternString, entity.patternType, match.value, extracted)
        }
        return null
    }

    /** Try all user-defined patterns and return match/no-match for each — used for parseTrace. */
    fun tryAll(text: String): List<PatternAttempt> = allPatterns.map { entity ->
        val match = tryMatch(entity, text)
        PatternAttempt(entity, match != null, match?.value)
    }

    val hasPatterns: Boolean get() = allPatterns.isNotEmpty()

    private fun tryMatch(entity: RegexPatternEntity, text: String): MatchResult? = try {
        Regex(entity.patternString).find(text)
    } catch (_: Exception) { null }
}
