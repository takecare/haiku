package dev.ruibot.haiku.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.Navigation
import dagger.hilt.android.AndroidEntryPoint
import dev.ruibot.haiku.databinding.FragmentNavBinding
import dev.ruibot.haiku.presentation.theme.HaikuTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.events
            .flowWithLifecycle(lifecycle)
            .onEach { event ->
                when (event) {
                    is MainEvent.NavigateToWrite -> {
                        // FIXME how do we do this? no nav controller here!!
                    }
                    is MainEvent.NavigateToRead -> {}
                    is MainEvent.NavigateToSettings -> {}
                }
            }
            .launchIn(lifecycleScope)

        setContent {
            // MainScreen(viewModel())
            MainScreen(viewModel)
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    // navController: NavController
) {

    val scaffoldState = rememberScaffoldState()

    val state: MainUiState by viewModel.uiState.collectAsStateWithLifecycle(
        initialValue = MainUiState.Loading()
    )

    val navItems: List<NavItem> = when (state) {
        is MainUiState.Loading -> {
            (state as MainUiState.Loading).navItems
        }
        is MainUiState.Content -> {
            (state as MainUiState.Content).navItems
        }
        is MainUiState.Error -> {
            (state as MainUiState.Error).navItems
        }
    }

    Screen(
        scaffoldState = scaffoldState,
        title = "Haiku: Compose", // TODO stringResource(id=...)
        onNavItemClicked = { navItemIndex ->
            viewModel.navItemClicked() // TODO @RUI
        },
        onActionClicked = {
            Log.d("MAIN", "action clicked")
        },
        navItems = navItems
    ) {
        AndroidViewBinding(FragmentNavBinding::inflate) {
            // FIXME we can get the nav controller here but we actually need it one level above
            // (i.e. outside "MainScreen"). here we can't actually do anything with it!
            // the only way is to drop Events and merge them with UiState
            val navController = Navigation.findNavController(navHostFragment)
        }
    }
}

@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    title: String = "Haiku",
    onActionClicked: () -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(title) },
        actions = {
            IconButton(onClick = onActionClicked) {
                Icon(Icons.Filled.Delete, contentDescription = "Localized action description")
            }
        }
    )
}

@Composable
fun Screen(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    title: String,
    navItems: List<NavItem>, // TODO maybe don't use this model here
    onActionClicked: () -> Unit = {},
    onNavItemClicked: (item: Int) -> Unit = {},
    content: @Composable() () -> Unit
) {

    var selectedItem by remember { mutableStateOf(0) }

    HaikuTheme {
        Scaffold(
            scaffoldState = scaffoldState,
            snackbarHost = {
                // we provide no SnackbarHost as we have our own in our custom
                // snackbar composable. if we use the default one from the
                // Scaffold we either can't customise it or we end up showing
                // both ours and the default one
            },
            // FIXME we can't have the appbar be a part of this scaffold!
            // if the appbar is here then we cannot change it from any of the
            // fragments we display - unless we share a viewmodel, ugh
            // unfortunately this means that each screen will have to have to
            // deal with its own snackbar, ugh!!
            topBar = {
                AppBar(
                    title = title,
                    onActionClicked = onActionClicked,
                )
            },
            bottomBar = {
                BottomNavigation {
                    navItems.forEachIndexed { index, item ->
                        BottomNavigationItem(
                            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                            label = { Text(stringResource(item.title)) },
                            selected = selectedItem == index,
                            onClick = {
                                selectedItem = index
                                // TODO @RUI
                            }
                        )
                    }
                }
            }
        ) { contentPadding ->
            Column(modifier = modifier.padding(contentPadding)) {
                content()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            HaikuSnackbar(
                snackbarHostState = scaffoldState.snackbarHostState,
                onAction = { scaffoldState.snackbarHostState.currentSnackbarData?.performAction() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun HaikuSnackbar(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onAction: () -> Unit = {},
) {
    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data: SnackbarData ->
            Snackbar(
                content = {
                    Text(
                        text = data.message,
                        style = MaterialTheme.typography.body2
                    )
                },
                action = {
                    data.actionLabel?.let { actionLabel ->
                        TextButton(onClick = onAction) {
                            Text(
                                text = actionLabel,
                                color = MaterialTheme.colors.primary,
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }
                },
                backgroundColor = MaterialTheme.colors.background,
                contentColor = contentColorFor(MaterialTheme.colors.background)
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Bottom)
    )
}
