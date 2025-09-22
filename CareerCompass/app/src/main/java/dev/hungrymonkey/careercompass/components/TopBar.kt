package dev.hungrymonkey.careercompass.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import dev.hungrymonkey.careercompass.ui.theme.CareerCompassTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String = "CareerCompass", 
    navController: NavHostController? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = navigationIcon ?: {},
        actions = actions
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewTopBar() {
    CareerCompassTheme {
        TopBar()
    }
}