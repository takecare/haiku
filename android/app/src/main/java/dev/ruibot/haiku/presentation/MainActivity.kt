package dev.ruibot.haiku.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.ruibot.haiku.databinding.FragmentNavBinding
import dev.ruibot.haiku.presentation.theme.HaikuTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(viewModel = viewModel())
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {

    val scaffoldState = rememberScaffoldState()

    Screen(
        scaffoldState = scaffoldState,
        title = "Haiku: Compose", // TODO stringResource(id=...)
        onActionClicked = {
            Log.d("MAIN", "action clicked")
        }
    ) {
        AndroidViewBinding(FragmentNavBinding::inflate) {
            // this is our navhost
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
    onActionClicked: () -> Unit = {},
    content: @Composable() () -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Write", "Read", "Settings")

    HaikuTheme {
        Scaffold(
            scaffoldState = scaffoldState,
            snackbarHost = {
                // we provide no SnackbarHost as we have our own in our custom
                // snackbar composable. if we use the default one from the
                // Scaffold we either can't customise it or we end up showing
                // both ours and the default one
            },
            // FIXME we can't have the appbar be a part of this scaffold.
            // if the appbar is here then we cannot change it from any of the
            // fragments we display
            topBar = {
                AppBar(
                    title = title,
                    onActionClicked = onActionClicked,
                )
            },
            bottomBar = {
                BottomNavigation {
                    items.forEachIndexed { index, item ->
                        BottomNavigationItem(
                            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index }
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
