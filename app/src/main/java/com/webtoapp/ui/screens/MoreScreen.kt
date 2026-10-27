package com.webtoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSection
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSettingRow
import com.webtoapp.ui.design.WtaSpacing
import com.webtoapp.ui.design.WtaToggleRow
import com.webtoapp.ui.theme.ifDescriptionsShown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenAbout: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    WtaScreen(title = Strings.tabMore) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = WtaSpacing.ScreenHorizontal,
                    vertical = WtaSpacing.ScreenVertical
                ),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
        ) {

                WtaSection(title = Strings.moreSectionGeneral) {
                    WtaSettingCard {
                        val context = LocalContext.current
                        val categoryFilterStore = remember {
                            com.webtoapp.core.category.CategoryFilterStore(context)
                        }
                        var rememberCategory by remember {
                            mutableStateOf(categoryFilterStore.rememberEnabled)
                        }
                        WtaToggleRow(
                            title = Strings.rememberCategoryFilter,
                            subtitle = Strings.rememberCategoryFilterDesc.ifDescriptionsShown(),
                            checked = rememberCategory,
                            onCheckedChange = {
                                rememberCategory = it
                                categoryFilterStore.rememberEnabled = it
                            }
                        )
                    }
                }

                WtaSection(title = Strings.uiConfig) {
                    WtaSettingCard {
                        MoreMenuItem(
                            title = Strings.uiConfig,
                            icon = painterResource(R.drawable.ic_sidebar_settings),
                            onClick = onOpenSettings
                        )
                    }
                }

                WtaSection(title = Strings.about) {
                WtaSettingCard {
                    MoreMenuItem(
                        title = Strings.menuAbout,
                        icon = painterResource(R.drawable.ic_sidebar_about),
                        onClick = onOpenAbout
                    )
                }
                }
        }
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    icon: Painter,
    onClick: () -> Unit
) {
    WtaSettingRow(
        title = title,
        onClick = onClick,
        iconContent = {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    ) {
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
