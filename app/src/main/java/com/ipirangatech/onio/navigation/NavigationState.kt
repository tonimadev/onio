package com.ipirangatech.onio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator

/** One back stack per top-level destination; the first top-level key is the "home" stack. */
class NavigationState(
    val topLevelKeys: List<NavKey>,
    private val selectedIndexState: MutableIntState,
    val backStacks: List<NavBackStack<NavKey>>,
) {
    var selectedIndex: Int
        get() = selectedIndexState.intValue
        set(value) {
            selectedIndexState.intValue = value
        }

    val selectedKey: NavKey get() = topLevelKeys[selectedIndex]
    val currentBackStack: NavBackStack<NavKey> get() = backStacks[selectedIndex]
    val currentKey: NavKey? get() = currentBackStack.lastOrNull()

    /** Home stack always stays alive underneath the selected one so back returns to it. */
    val stacksInUse: List<Int> get() = if (selectedIndex == 0) listOf(0) else listOf(0, selectedIndex)
}

@Composable
fun rememberNavigationState(topLevelKeys: List<NavKey>): NavigationState {
    val selected = rememberSaveable { mutableIntStateOf(0) }
    val stacks = topLevelKeys.map { rememberNavBackStack(it) }
    return remember(topLevelKeys, stacks) { NavigationState(topLevelKeys, selected, stacks) }
}

class Navigator(private val state: NavigationState) {
    fun navigate(key: NavKey) {
        state.currentBackStack.add(key)
    }

    fun selectTopLevel(key: NavKey) {
        val index = state.topLevelKeys.indexOf(key)
        if (index < 0) return
        if (index == state.selectedIndex) {
            // Re-selecting the active tab pops back to its root.
            val stack = state.backStacks[index]
            while (stack.size > 1) stack.removeLastOrNull()
        } else {
            state.selectedIndex = index
        }
    }

    /** Opens [key] on top of another tab's stack and switches to it. */
    fun launchIn(topLevel: NavKey, key: NavKey) {
        val index = state.topLevelKeys.indexOf(topLevel)
        if (index < 0) return
        state.backStacks[index].add(key)
        state.selectedIndex = index
    }

    /** Pops the current screen; never empties a stack. Falls back to the home tab. */
    fun goBack(): Boolean {
        val stack = state.currentBackStack
        return when {
            stack.size > 1 -> {
                stack.removeLastOrNull()
                true
            }
            state.selectedIndex != 0 -> {
                state.selectedIndex = 0
                true
            }
            else -> false
        }
    }

    fun canGoBack(): Boolean = state.currentBackStack.size > 1 || state.selectedIndex != 0
}

/** Keys of every stack in use, home stack first, in the order NavDisplay should render them. */
val NavigationState.displayKeys: List<NavKey>
    get() = stacksInUse.flatMap { backStacks[it] }

@Composable
fun rememberNavEntryDecorators(): List<NavEntryDecorator<NavKey>> {
    val saveable = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val viewModelStore = rememberViewModelStoreNavEntryDecorator<NavKey>()
    return listOf(saveable, viewModelStore)
}
