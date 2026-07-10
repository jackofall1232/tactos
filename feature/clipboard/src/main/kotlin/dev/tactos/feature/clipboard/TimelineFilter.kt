package dev.tactos.feature.clipboard

import dev.tactos.core.model.ClipItem
import dev.tactos.core.model.ClipType

/** Active timeline filters. `null` means "no filter" for that dimension. */
data class TimelineFilter(
    val type: ClipType? = null,
    val category: String? = null,
)

/** Applies [selection], preserving input order. */
fun List<ClipItem>.applyFilter(selection: TimelineFilter): List<ClipItem> = filter {
    (selection.type == null || it.type == selection.type) &&
        (selection.category == null || it.category == selection.category)
}

/** Distinct non-blank categories present, sorted for stable chip order. */
fun List<ClipItem>.distinctCategories(): List<String> =
    mapNotNull { it.category?.takeIf(String::isNotBlank) }.distinct().sorted()

/** Distinct clip types present, in enum declaration order, for the filter chips. */
fun List<ClipItem>.distinctTypes(): List<ClipType> =
    ClipType.entries.filter { type -> any { it.type == type } }
