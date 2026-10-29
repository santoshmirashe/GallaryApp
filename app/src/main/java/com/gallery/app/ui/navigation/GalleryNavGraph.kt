package com.gallery.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gallery.app.ui.screens.AlbumDetailScreen
import com.gallery.app.ui.screens.AlbumsScreen
import com.gallery.app.ui.screens.FavoritesScreen
import com.gallery.app.ui.screens.HomeScreen
import com.gallery.app.ui.screens.PermissionScreen
import com.gallery.app.ui.screens.ViewerScreen
import com.gallery.app.ui.viewmodel.GalleryViewModel

sealed class Screen(val route: String) {
    object Permission : Screen("permission")
    object Home : Screen("home")
    object Albums : Screen("albums")
    object AlbumDetail : Screen("album/{bucketId}") {
        fun createRoute(bucketId: Long) = "album/$bucketId"
    }
    object Favorites : Screen("favorites")
    object Viewer : Screen("viewer/{mediaIndex}/{source}") {
        fun createRoute(mediaIndex: Int, source: String) = "viewer/$mediaIndex/$source"
    }
}

@Composable
fun GalleryNavGraph() {
    val navController = rememberNavController()
    val viewModel: GalleryViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screen.Permission.route) {

        composable(Screen.Permission.route) {
            PermissionScreen(
                onPermissionGranted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Permission.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onMediaClick = { index ->
                    navController.navigate(Screen.Viewer.createRoute(index, "home"))
                },
                onAlbumsClick = { navController.navigate(Screen.Albums.route) },
                onFavoritesClick = { navController.navigate(Screen.Favorites.route) }
            )
        }

        composable(Screen.Albums.route) {
            AlbumsScreen(
                viewModel = viewModel,
                onAlbumClick = { bucketId ->
                    navController.navigate(Screen.AlbumDetail.createRoute(bucketId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("bucketId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getLong("bucketId") ?: 0L
            AlbumDetailScreen(
                viewModel = viewModel,
                bucketId = bucketId,
                onMediaClick = { index ->
                    navController.navigate(Screen.Viewer.createRoute(index, "album_$bucketId"))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                viewModel = viewModel,
                onMediaClick = { index ->
                    navController.navigate(Screen.Viewer.createRoute(index, "favorites"))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Viewer.route,
            arguments = listOf(
                navArgument("mediaIndex") { type = NavType.IntType },
                navArgument("source") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaIndex = backStackEntry.arguments?.getInt("mediaIndex") ?: 0
            val source = backStackEntry.arguments?.getString("source") ?: "home"
            ViewerScreen(
                viewModel = viewModel,
                initialIndex = mediaIndex,
                source = source,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
