package dev.tactos.core.model

/**
 * The single source of truth for installed toolbox modules. The app shell
 * renders its home grid from [modules]; the timeline resolves contextual
 * actions through [actionsFor].
 *
 * Construction fails fast on duplicate module ids or duplicate action ids —
 * silent shadowing between modules (or future plugins) must be impossible.
 */
class ModuleRegistry(modules: List<ToolboxModule>) {

    val modules: List<ToolboxModule> = modules.sortedWith(compareBy({ it.order }, { it.id }))

    private val byId: Map<String, ToolboxModule>

    init {
        val duplicateModules = modules.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicateModules.isEmpty()) {
            "Duplicate ToolboxModule ids: $duplicateModules"
        }
        val duplicateActions = modules.flatMap { it.clipActions() }
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
        require(duplicateActions.isEmpty()) {
            "Duplicate ClipAction ids across modules: $duplicateActions"
        }
        byId = modules.associateBy { it.id }
    }

    fun byId(id: String): ToolboxModule? = byId[id]

    /**
     * All actions applicable to [item], aggregated across modules in module
     * order, preserving each module's own declaration order.
     */
    fun actionsFor(item: ClipItem): List<ClipAction> =
        modules.flatMap { module -> module.clipActions().filter { it.appliesTo(item) } }
}
