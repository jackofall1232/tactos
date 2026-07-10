package dev.tactos.core.model

/**
 * The detected content type of a clipboard item. Detection lives in
 * `core/detect`; this enum is the shared vocabulary between the timeline,
 * detectors, and toolbox modules.
 */
enum class ClipType {
    URL,
    IP_ADDRESS,
    COLOR,
    JSON,
    EMAIL,
    PHONE,
    TEXT,
}
