package com.owl.minerva.stocklab.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.owl.minerva.stocklab.database.StockLabDatabase
import com.owl.minerva.stocklab.model.Item
import com.owl.minerva.stocklab.model.Ledger
import com.owl.minerva.stocklab.repository.ItemRepositoryImpl
import com.owl.minerva.stocklab.repository.LedgerRepositoryImpl
import com.owl.minerva.stocklab.repository.StockOutRepositoryImpl
import com.owl.minerva.stocklab.repository.StockRepositoryImpl
import com.owl.minerva.stocklab.service.StockOutService
import com.owl.minerva.stocklab.ui.components.ButtonIcon
import com.owl.minerva.stocklab.ui.components.ProfitMiniChart
import com.owl.minerva.stocklab.ui.theme.StockLabTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    val database = remember(context) {
        StockLabDatabase.getInstance(context)
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var sellDialogOpen by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var productExpanded by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("") }
    var currentSellPrice by remember { mutableStateOf("") }
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
    val monthlyCashFlowText = remember(monthlyCashFlow) {
        formatCompactMoney(monthlyCashFlow)
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
                        text = "Welcome Back",
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
                    onClick = {},
                    modifier = Modifier.size(35.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Profile settings",
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
                                text = "Monthly Cash Flow",
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
                    title = "Product",
                    icon = Icons.Default.Inventory2,
                    onClick = {
                        context.startActivity(Intent(context, ProductActivity::class.java))
                    },
                    modifier = Modifier.weight(1f),
                )
                ButtonIcon(
                    title = "Sell",
                    icon = Icons.Default.PointOfSale,
                    onClick = {
                        sellDialogOpen = true
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            RecentLedgerSection(
                ledgers = recentLedgers,
                items = items,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (sellDialogOpen) {
        AlertDialog(
            onDismissRequest = {
                sellDialogOpen = false
            },
            title = {
                Text(text = "Sell")
            },
            text = {
                Column(
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
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            label = {
                                Text(text = "Product")
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
                            Text(text = "Quantity")
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
                            Text(text = "Current Sell Price")
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
                    onClick = {
                        scope.launch {
                            try {
                                stockOutService.sell(
                                    itemId = selectedItem?.id ?: 0L,
                                    quantity = quantity.toDoubleOrNull() ?: 0.0,
                                    currentSellPrice = currentSellPrice.toDoubleOrNull() ?: 0.0,
                                )
                                sellDialogOpen = false
                                selectedItem = null
                                quantity = ""
                                currentSellPrice = ""
                                snackbarHostState.showSnackbar("Sell saved")
                            } catch (error: IllegalArgumentException) {
                                snackbarHostState.showSnackbar(error.message ?: "Invalid sell")
                            }
                        }
                    },
                ) {
                    Text(text = "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sellDialogOpen = false
                    },
                ) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}

@Composable
private fun RecentLedgerSection(
    ledgers: List<Ledger>,
    items: List<Item>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Recent Ledger",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (ledgers.isEmpty()) {
            Text(
                text = "No ledger entries yet",
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
                        .ifBlank { "Unknown Product" }

                    RecentLedgerRow(
                        ledger = ledger,
                        productName = productName,
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
                    text = ledger.code.ifBlank { "Ledger ${ledger.id}" },
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
                text = "${ledger.direction.name}: ${formatCompactMoney(ledger.amount)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

private fun formatCompactMoney(value: Long): String {
    val absValue = kotlin.math.abs(value)
    val sign = if (value < 0) "-" else ""
    val divisor = when {
        absValue >= 1_000_000_000L -> 1_000_000_000L to "B"
        absValue >= 1_000_000L -> 1_000_000L to "M"
        absValue >= 1_000L -> 1_000L to "K"
        else -> return value.toString()
    }
    val scaled = BigDecimal(absValue)
        .divide(BigDecimal(divisor.first), 2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

    return "$sign$scaled${divisor.second}"
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
