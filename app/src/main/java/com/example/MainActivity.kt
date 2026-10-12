package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ActionCenterScreen
import com.example.ui.screens.ActiveVisitScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StoresScreen
import com.example.ui.screens.VisitsListScreen
import com.example.ui.theme.StoreFlowTheme
import com.example.ui.viewmodels.StoreFlowViewModel

enum class AppScreen(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    STORES("Stores", Icons.Default.Storefront),
    VISITS("Visits", Icons.Default.Checklist),
    ACTIONS("Actions", Icons.Default.Assignment),
    SCHEDULE("Schedule", Icons.Default.CalendarToday),
    REPORTS("Reports", Icons.Default.Description),
    SETTINGS("Settings", Icons.Default.Settings),
    ACTIVE_VISIT("Active Visit", Icons.Default.Checklist)
}

class MainActivity : ComponentActivity() {

    private val viewModel: StoreFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StoreFlowTheme {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: StoreFlowViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var previousScreen by remember { mutableStateOf(AppScreen.HOME) }

    val activeVisit by viewModel.activeVisit.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()

    // Handle system back button gracefully
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        if (currentScreen == AppScreen.ACTIVE_VISIT) {
            currentScreen = previousScreen
        } else {
            currentScreen = AppScreen.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentScreen != AppScreen.ACTIVE_VISIT) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "StoreFlow",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2E7D32))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Offline Mode",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                previousScreen = currentScreen
                                currentScreen = AppScreen.REPORTS
                            },
                            modifier = Modifier.testTag("top_bar_reports_btn")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "Reports")
                        }
                        IconButton(
                            onClick = {
                                previousScreen = currentScreen
                                currentScreen = AppScreen.SETTINGS
                            },
                            modifier = Modifier.testTag("top_bar_settings_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (currentScreen != AppScreen.ACTIVE_VISIT) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    val navItems = listOf(
                        AppScreen.HOME,
                        AppScreen.STORES,
                        AppScreen.VISITS,
                        AppScreen.ACTIONS,
                        AppScreen.SCHEDULE
                    )

                    navItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = {
                                previousScreen = currentScreen
                                currentScreen = screen
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToStores = { currentScreen = AppScreen.STORES },
                    onNavigateToActiveVisit = {
                        previousScreen = AppScreen.HOME
                        currentScreen = AppScreen.ACTIVE_VISIT
                    },
                    onNavigateToActions = { currentScreen = AppScreen.ACTIONS },
                    onNavigateToSchedule = { currentScreen = AppScreen.SCHEDULE },
                    onNavigateToReports = { currentScreen = AppScreen.REPORTS },
                    onStartVisitForStore = { storeId ->
                        viewModel.startVisit(storeId) {
                            previousScreen = currentScreen
                            currentScreen = AppScreen.ACTIVE_VISIT
                        }
                    }
                )

                AppScreen.STORES -> StoresScreen(
                    viewModel = viewModel,
                    onStartVisit = { storeId ->
                        viewModel.startVisit(storeId) {
                            previousScreen = AppScreen.STORES
                            currentScreen = AppScreen.ACTIVE_VISIT
                        }
                    }
                )

                AppScreen.VISITS -> VisitsListScreen(
                    viewModel = viewModel,
                    onResumeActiveVisit = {
                        previousScreen = AppScreen.VISITS
                        currentScreen = AppScreen.ACTIVE_VISIT
                    },
                    onStartNewVisit = {
                        currentScreen = AppScreen.STORES
                    }
                )

                AppScreen.ACTIONS -> ActionCenterScreen(
                    viewModel = viewModel
                )

                AppScreen.SCHEDULE -> ScheduleScreen(
                    viewModel = viewModel,
                    onStartVisitForStore = { storeId ->
                        viewModel.startVisit(storeId) {
                            previousScreen = AppScreen.SCHEDULE
                            currentScreen = AppScreen.ACTIVE_VISIT
                        }
                    }
                )

                AppScreen.REPORTS -> ReportsScreen(
                    viewModel = viewModel
                )

                AppScreen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )

                AppScreen.ACTIVE_VISIT -> ActiveVisitScreen(
                    viewModel = viewModel,
                    onVisitFinished = {
                        currentScreen = AppScreen.VISITS
                    },
                    onBack = {
                        currentScreen = previousScreen
                    }
                )
            }
        }
    }
}
