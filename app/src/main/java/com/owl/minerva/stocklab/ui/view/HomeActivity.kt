package com.owl.minerva.stocklab.ui.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.owl.minerva.stocklab.MainActivity
import com.owl.minerva.stocklab.R
import com.owl.minerva.stocklab.database.StockLabDatabase
import com.owl.minerva.stocklab.enums.AppCurrency
import com.owl.minerva.stocklab.model.Item
import com.owl.minerva.stocklab.model.Ledger
import com.owl.minerva.stocklab.repository.ItemRepositoryImpl
import com.owl.minerva.stocklab.repository.LedgerRepositoryImpl
import com.owl.minerva.stocklab.repository.StockOutRepositoryImpl
import com.owl.minerva.stocklab.repository.StockRepositoryImpl
import com.owl.minerva.stocklab.service.AppMessageException
import com.owl.minerva.stocklab.service.CurrencySettingsStore
import com.owl.minerva.stocklab.service.MoneyFormatService
import com.owl.minerva.stocklab.service.StockOutService
import com.owl.minerva.stocklab.ui.components.AdMobBanner
import com.owl.minerva.stocklab.ui.components.ButtonIcon
import com.owl.minerva.stocklab.ui.components.ProfitMiniChart
import com.owl.minerva.stocklab.ui.components.clearFocusOnTapOutside
import com.owl.minerva.stocklab.ui.setupEdgeToEdge
import com.owl.minerva.stocklab.ui.theme.StockLabTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContent {
            StockLabTheme {
                HomeContainer()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    StockLabTheme {
        HomeContainer(
            monthlyCashFlowPreview = 0L,
            cashFlowChartValuesPreview = listOf(12f, 18f, 15f, 14f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContainer(
    modifier: Modifier = Modifier,
    monthlyCashFlowPreview: Long? = null,
    cashFlowChartValuesPreview: List<Float>? = null,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val resources = LocalResources.current
    val database = remember(context) {
        StockLabDatabase.getInstance(context)
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currencySettingsStore = remember(context) {
        CurrencySettingsStore(context)
    }
    var selectedCurrency by remember {
        mutableStateOf(currencySettingsStore.getCurrency())
    }
    var settingsDialogOpen by remember { mutableStateOf(false) }
    var clearDataConfirmOpen by remember { mutableStateOf(false) }
    var sellDialogOpen by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var productExpanded by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("") }
    var currentSellPrice by remember { mutableStateOf("") }
    var isClearingData by remember { mutableStateOf(false) }
    var isSelling by remember { mutableStateOf(false) }
    val items by database.itemDao().getAll().collectAsState(initial = emptyList())
    val stockOutService = remember(database) {
        StockOutService(
            stockOutRepository = StockOutRepositoryImpl(database.stockOutDao()),
            itemRepository = ItemRepositoryImpl(database.itemDao()),
            stockRepository = StockRepositoryImpl(database.stockDao()),
            ledgerRepository = LedgerRepositoryImpl(database.ledgerDao()),
        )
    }
    val currentMonthRange = remember {
        val start = LocalDate.now().withDayOfMonth(1)
        val end = start.plusMonths(1)
        val zone = ZoneId.systemDefault()

        start.atStartOfDay(zone).toInstant().toEpochMilli() to
                end.atStartOfDay(zone).toInstant().toEpochMilli()
    }
    val recentLedgers by database.ledgerDao().getLatest(10).collectAsState(initial = emptyList())
    val monthlyCashFlow = if (monthlyCashFlowPreview != null) {
        monthlyCashFlowPreview
    } else {
        val ledgerDao = remember(context) {
            database.ledgerDao()
        }
        val cashFlow by ledgerDao.getTotalBetween(
            startAt = currentMonthRange.first,
            endAt = currentMonthRange.second,
        ).collectAsState(initial = 0L)
        cashFlow
    }
    val monthlyCashFlowText = remember(monthlyCashFlow, selectedCurrency) {
        MoneyFormatService.formatCompact(monthlyCashFlow, selectedCurrency)
    }
    val cashFlowChartValues = if (cashFlowChartValuesPreview != null) {
        cashFlowChartValuesPreview
    } else {
        val ledgerDao = remember(database) {
            database.ledgerDao()
        }
        val monthRanges = remember {
            lastFourMonthRanges()
        }
        monthRanges.map { range ->
            val cashFlow by ledgerDao.getTotalBetween(
                startAt = range.first,
                endAt = range.second,
            ).collectAsState(initial = 0L)

            cashFlow.toFloat()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val todayText = remember {
                    LocalDate.now().format(
                        DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.getDefault())
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.welcome_back),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = todayText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledIconButton(
                    onClick = {
                        settingsDialogOpen = true
                    },
                    modifier = Modifier.size(35.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.profile_settings),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.monthly_cash_flow),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Text(
                            text = monthlyCashFlowText,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = when {
                                    monthlyCashFlowText.length > 18 -> 20.sp
                                    monthlyCashFlowText.length > 14 -> 24.sp
                                    else -> MaterialTheme.typography.headlineLarge.fontSize
                                },
                            ),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    ProfitMiniChart(
                        values = cashFlowChartValues,
                        modifier = Modifier
                            .width(120.dp)
                            .height(95.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ButtonIcon(
                    title = stringResource(R.string.product),
                    icon = Icons.Default.Inventory2,
                    onClick = {
                        context.startActivity(Intent(context, ProductActivity::class.java))
                    },
                    modifier = Modifier.weight(1f),
                )
                ButtonIcon(
                    title = stringResource(R.string.sell),
                    icon = Icons.Default.PointOfSale,
                    onClick = {
                        sellDialogOpen = true
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            AdMobBanner(
                modifier = Modifier.fillMaxWidth(),
            )

            RecentLedgerSection(
                ledgers = recentLedgers,
                items = items,
                currency = selectedCurrency,
                modifier = Modifier.weight(1f),
            )
        }
    }

    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    val sellSavedMessage = stringResource(R.string.sell_saved)
    val invalidSellMessage = stringResource(R.string.error_invalid_sell)
    val unexpectedActionMessage = stringResource(R.string.error_unexpected_action)

    if (settingsDialogOpen) {
        SettingsDialog(
            selectedCurrency = selectedCurrency,
            onCurrencyChange = { currency ->
                selectedCurrency = currency
                currencySettingsStore.setCurrency(currency)
            },
            onClearAllData = {
                clearDataConfirmOpen = true
            },
            onPrivacyPolicy = {
                val browserIntent = Intent(Intent.ACTION_VIEW, privacyPolicyUrl.toUri())
                runCatching {
                    context.startActivity(browserIntent)
                }.onFailure {
                    scope.launch {
                        snackbarHostState.showSnackbar(unexpectedActionMessage)
                    }
                }
            },
            onDismiss = {
                settingsDialogOpen = false
            },
        )
    }

    if (clearDataConfirmOpen) {
        AlertDialog(
            onDismissRequest = { clearDataConfirmOpen = false },
            title = {
                Text(text = stringResource(R.string.clear_all_data_confirm_title))
            },
            text = {
                Text(text = stringResource(R.string.clear_all_data_confirm_message))
            },
            confirmButton = {
                TextButton(
                    enabled = !isClearingData,
                    onClick = {
                        if (isClearingData) {
                            return@TextButton
                        }
                        clearDataConfirmOpen = false
                        scope.launch {
                            isClearingData = true
                            try {
                                withContext(Dispatchers.IO) {
                                    database.clearAllTables()
                                    context.getSharedPreferences("stock_lab_settings", Context.MODE_PRIVATE)
                                        .edit {
                                            clear()
                                        }
                                }
                                val restartIntent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(restartIntent)
                            } catch (error: Exception) {
                                snackbarHostState.showSnackbar(unexpectedActionMessage)
                            } finally {
                                isClearingData = false
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(text = stringResource(R.string.clear_all_data))
                }
            },
            dismissButton = {
                TextButton(onClick = { clearDataConfirmOpen = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (sellDialogOpen) {
        AlertDialog(
            onDismissRequest = {
                sellDialogOpen = false
            },
            title = {
                Text(text = stringResource(R.string.sell))
            },
            text = {
                Column(
                    modifier = Modifier.clearFocusOnTapOutside(focusManager),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExposedDropdownMenuBox(
                        expanded = productExpanded,
                        onExpandedChange = { productExpanded = !productExpanded },
                    ) {
                        OutlinedTextField(
                            value = selectedItem?.name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            label = {
                                Text(text = stringResource(R.string.product))
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded)
                            },
                            singleLine = true,
                        )

                        ExposedDropdownMenu(
                            expanded = productExpanded,
                            onDismissRequest = { productExpanded = false },
                        ) {
                            items.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = item.name.orEmpty())
                                    },
                                    onClick = {
                                        selectedItem = item
                                        scope.launch {
                                            val latestHpp = database.hppDao().getLatestByItemId(item.id)
                                            currentSellPrice = calculateDefaultSellPrice(
                                                item = item,
                                                hppPerUnit = latestHpp?.amount?.toDouble(),
                                            ).toInputText()
                                        }
                                        productExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(text = stringResource(R.string.quantity))
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                        ),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = currentSellPrice,
                        onValueChange = { currentSellPrice = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(text = stringResource(R.string.current_sell_price))
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                        ),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSelling,
                    onClick = {
                        if (isSelling) {
                            return@TextButton
                        }
                        scope.launch {
                            isSelling = true
                            try {
                                withContext(Dispatchers.IO) {
                                    database.withTransaction {
                                        stockOutService.sell(
                                            itemId = selectedItem?.id ?: 0L,
                                            quantity = quantity.toDoubleOrNull() ?: 0.0,
                                            currentSellPrice = currentSellPrice.toDoubleOrNull() ?: 0.0,
                                        )
                                    }
                                }
                                sellDialogOpen = false
                                selectedItem = null
                                quantity = ""
                                currentSellPrice = ""
                                snackbarHostState.showSnackbar(sellSavedMessage)
                            } catch (error: AppMessageException) {
                                snackbarHostState.showSnackbar(resources.getString(error.messageResId))
                            } catch (error: IllegalArgumentException) {
                                snackbarHostState.showSnackbar(error.message ?: invalidSellMessage)
                            } catch (error: Exception) {
                                snackbarHostState.showSnackbar(unexpectedActionMessage)
                            } finally {
                                isSelling = false
                            }
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sellDialogOpen = false
                    },
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun RecentLedgerSection(
    ledgers: List<Ledger>,
    items: List<Item>,
    currency: AppCurrency,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.recent_ledger),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (ledgers.isEmpty()) {
            Text(
                text = stringResource(R.string.no_ledger_entries_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = ledgers,
                    key = { ledger -> ledger.id },
                ) { ledger ->
                    val productName = items
                        .firstOrNull { item -> item.id == ledger.itemId }
                        ?.name
                        .orEmpty()
                        .ifBlank { stringResource(R.string.unknown_product) }

                    RecentLedgerRow(
                        ledger = ledger,
                        productName = productName,
                        currency = currency,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentLedgerRow(
    ledger: Ledger,
    productName: String,
    currency: AppCurrency,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = ledger.code.ifBlank { stringResource(R.string.ledger_fallback, ledger.id) },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = productName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = stringResource(
                    R.string.ledger_direction_amount,
                    ledger.direction.name,
                    MoneyFormatService.formatCompact(ledger.amount, currency),
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    selectedCurrency: AppCurrency,
    onCurrencyChange: (AppCurrency) -> Unit,
    onClearAllData: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.currency),
                    style = MaterialTheme.typography.titleSmall,
                )
                AppCurrency.entries.forEach { currency ->
                    ListItem(
                        headlineContent = {
                            Text(text = currency.name)
                        },
                        supportingContent = {
                            Text(text = stringResource(currency.displayNameRes))
                        },
                        leadingContent = {
                            RadioButton(
                                selected = currency == selectedCurrency,
                                onClick = {
                                    onCurrencyChange(currency)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                TextButton(
                    onClick = onClearAllData,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.clear_all_data))
                }

                TextButton(
                    onClick = onPrivacyPolicy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.privacy_policy))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_done))
            }
        },
    )
}

private fun Double.toInputText(): String {
    return if (this % 1.0 == 0.0) {
        toLong().toString()
    } else {
        toString()
    }
}

private fun calculateDefaultSellPrice(
    item: Item,
    hppPerUnit: Double?,
): Double {
    return when {
        item.currentSellPrice > 0.0 -> item.currentSellPrice
        hppPerUnit != null && hppPerUnit > 0.0 -> {
            hppPerUnit * (1.0 + item.profitTakePercent / 100.0)
        }

        else -> 0.0
    }
}

private fun lastFourMonthRanges(): List<Pair<Long, Long>> {
    val zone = ZoneId.systemDefault()
    val currentMonthStart = LocalDate.now().withDayOfMonth(1)

    return (3 downTo 0).map { monthOffset ->
        val start = currentMonthStart.minusMonths(monthOffset.toLong())
        val end = start.plusMonths(1)

        start.atStartOfDay(zone).toInstant().toEpochMilli() to
                end.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
