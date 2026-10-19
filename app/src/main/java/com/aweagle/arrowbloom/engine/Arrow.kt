package com.aweagle.arrowbloom.engine

enum class ArrowState {
    IDLE,
    LAUNCHING,
    ESCAPED,
    BLOCKED
}

/**
 * An Arrow on the puzzle board.
 *
 * @param id Unique identifier for this arrow within the board.
 * @param head The coordinate containing the arrowhead.
 * @param direction Cardinal direction the arrowhead points toward.
 * @param body All grid coordinates occupied by this arrow. Head is included.
 * @param colorIndex Visual palette index for rendering.
 * @param state Current lifecycle state of the arrow.
 */
data class Arrow(
    val id: Int,
    val head: GridPos,
    val direction: Direction,
    val body: List<GridPos> = listOf(head),
    val colorIndex: Int = 0,
    val state: ArrowState = ArrowState.IDLE
) {
    init {
        require(body.contains(head)) { "Arrow body must contain its head position" }
    }

    /**
     * Checks if this arrow occupies the specified cell.
     */
    fun occupies(pos: GridPos): Boolean = body.contains(pos)

    /**
     * Returns a copy with the updated state.
     */
    fun withState(newState: ArrowState): Arrow = copy(state = newState)
}
