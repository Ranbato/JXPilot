package org.lambertland.kxpilot.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ---------------------------------------------------------------------------
// Navigator — back-stack based router
// ---------------------------------------------------------------------------

/**
 * Owns a [Screen] back-stack and exposes the current screen as a [StateFlow].
 *
 * State holders receive a [Navigator] instance via constructor injection so they
 * can trigger navigation without holding a reference to any composable.
 *
 * Thread-safety: all mutations must happen on the Main dispatcher.
 *
 * @param initial         The first screen pushed onto the stack.
 * @param onExitRequested Called when [pop] is invoked on a single-entry stack
 *                        (i.e. nothing to pop back to).  Desktop callers pass
 *                        `::exitApplication` here.
 */
class Navigator(
    initial: Screen = Screen.MainMenu,
    private val onExitRequested: () -> Unit = {},
) {
    private val stack = ArrayDeque<Screen>().also { it.addLast(initial) }

    private val _current = MutableStateFlow(initial)

    /** The currently visible screen. Collect in the root composable. */
    val current: StateFlow<Screen> = _current.asStateFlow()

    /** Push a new screen onto the stack and navigate to it. */
    fun push(screen: Screen) {
        stack.addLast(screen)
        _current.value = screen
    }

    /**
     * Pop the top screen off the stack and return to the previous one.
     * Returns false if the stack has only one entry; in that case
     * [onExitRequested] is invoked so the application can exit gracefully.
     */
    fun pop(): Boolean {
        if (stack.size <= 1) {
            onExitRequested()
            return false
        }
        stack.removeLast()
        _current.value = stack.last()
        return true
    }

    /** Replace the current screen without adding to the back stack. */
    fun replace(screen: Screen) {
        if (stack.isNotEmpty()) {
            stack[stack.size - 1] = screen
        } else {
            stack.addLast(screen)
        }
        _current.value = screen
    }
}
