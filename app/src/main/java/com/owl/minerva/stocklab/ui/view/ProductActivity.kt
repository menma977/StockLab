package com.owl.minerva.stocklab.ui.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.owl.minerva.stocklab.database.StockLabDatabase
import com.owl.minerva.stocklab.enums.AppCurrency
import com.owl.minerva.stocklab.enums.UnitType
import com.owl.minerva.stocklab.model.Batch
import com.owl.minerva.stocklab.model.Hpp
import com.owl.minerva.stocklab.model.Item
import com.owl.minerva.stocklab.model.Stock
import com.owl.minerva.stocklab.repository.*
import com.owl.minerva.stocklab.service.CurrencySettingsStore
import com.owl.minerva.stocklab.service.ItemService
import com.owl.minerva.stocklab.service.MoneyFormatService
import com.owl.minerva.stocklab.ui.components.ProfitBadge
import com.owl.minerva.stocklab.ui.theme.StockLabTheme
import kotlinx.coroutines.launch

class ProductActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockLabTheme {
                ProductContainer()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProductPreview() {
    StockLabTheme {
        ProductContainer(
            previewProducts = listOf(
                ProductCardUiState(
                    name = "Sample Product",
                    finalPrice = "\$25,000.00",
                    currentSellPrice = "\$0.00",
                    profitCutPercent = 25,
                    activeBatchCode = "TMBLR4821/B/1",
                    totalStock = "120 PCS",
                    hppPerUnit = "\$18,000.00",
                    netIncome = "\$7,000.00",
                ),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductContainer(
    modifier: Modifier = Modifier,
    previewProducts: List<ProductCardUiState>? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedCurrency = remember(context) {
        CurrencySettingsStore(context).getCurrency()
    }
    var deleteTarget by remember { mutableStateOf<ProductCardUiState?>(null) }
    val database = if (previewProducts == null) {
        remember(context) {
            StockLabDatabase.getInstance(context)
        }
    } else {
        null
    }
    val itemService = if (database != null) {
        remember(context) {
            ItemService(
                itemRepository = ItemRepositoryImpl(database.itemDao()),
                hppRepository = HppRepositoryImpl(database.hppDao()),
                hppComponentRepository = HppComponentRepositoryImpl(database.hppComponentDao()),
                batchRepository = BatchRepositoryImpl(database.batchDao()),
                stockRepository = StockRepositoryImpl(database.stockDao()),
                ledgerRepository = LedgerRepositoryImpl(database.ledgerDao()),
                batchCostRepository = BatchCostRepositoryImpl(database.batchCostDao()),
                stockInRepository = StockInRepositoryImpl(database.stockInDao()),
                stockOutRepository = StockOutRepositoryImpl(database.stockOutDao()),
            )
        }
    } else {
        null
    }
    val products = if (previewProducts != null) {
        previewProducts
    } else {
        requireNotNull(database)
        requireNotNull(itemService)

        val items by itemService.index().collectAsState(initial = emptyList())
        val stocks by database.stockDao().getAll().collectAsState(initial = emptyList())
        val hpps by database.hppDao().getAll().collectAsState(initial = emptyList())
        val batches by database.batchDao().getAll().collectAsState(initial = emptyList())

        items.map { item ->
            item.toProductCardUiState(
                stocks = stocks,
                hpps = hpps,
                batches = batches,
                currency = selectedCurrency,
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(elevation = 4.dp),
                title = {
                    Text(
                        text = "Product",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    context.startActivity(Intent(context, ProductStoreActivity::class.java))
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(text = "Add Product")
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(products) { product ->
                ProductListCard(
                    product = product,
                    onShowClick = { itemId ->
                        context.startActivity(
                            Intent(context, ProductShowActivity::class.java)
                                .putExtra(ProductShowActivity.EXTRA_ITEM_ID, itemId),
                        )
                    },
                    onDeleteClick = {
                        deleteTarget = product
                    },
                )
            }
        }
    }

    deleteTarget?.let { product ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text(text = "Delete Product")
            },
            text = {
                Text(text = "Delete ${product.name}? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val itemId = product.itemId
                        deleteTarget = null
                        if (itemId == null || itemService == null) {
                            return@TextButton
                        }
                        scope.launch {
                            val item = itemService.show(itemId)
                            if (item == null) {
                                snackbarHostState.showSnackbar("Product was not found")
                            } else {
                                itemService.delete(item)
                                snackbarHostState.showSnackbar("Product deleted")
                            }
                        }
                    },
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}

@Composable
private fun ProductListCard(
    product: ProductCardUiState,
    onShowClick: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RowLine(
                left = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ProductMetric(
                            label = "Product Name",
                            value = product.name,
                        )
                        Text(
                            text = "Current Sell Price: ${product.currentSellPrice}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                right = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onDeleteClick,
                            enabled = product.itemId != null,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete product",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                product.itemId?.let { itemId ->
                                    onShowClick(itemId)
                                }
                            },
                        ) {
                            Text(text = "Show")
                        }
                    }
                },
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                RowLine(
                    modifier = Modifier.padding(14.dp),
                    left = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Final Price",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = product.finalPrice,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    },
                    right = {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Profit Take",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            ProfitBadge(percent = product.profitCutPercent)
                        }
                    },
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Current Active Batch",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RowLine(
                        left = {
                            Text(
                                text = product.activeBatchCode,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        right = {
                            Text(
                                text = product.totalStock,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                    )
                }
            }

            HorizontalDivider()

            RowLine(
                left = {
                    ProductMetric(
                        label = "HPP per Unit",
                        value = product.hppPerUnit,
                    )
                },
                right = {
                    ProductMetric(
                        label = "Net Income",
                        value = product.netIncome,
                        horizontalAlignment = Alignment.End,
                    )
                },
            )
        }
    }
}

@Composable
private fun RowLine(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        left()
        right()
    }
}

@Composable
private fun ProductMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

data class ProductCardUiState(
    val itemId: Long? = null,
    val name: String,
    val finalPrice: String,
    val currentSellPrice: String,
    val profitCutPercent: Int,
    val activeBatchCode: String,
    val totalStock: String,
    val hppPerUnit: String,
    val netIncome: String,
)

private fun Item.toProductCardUiState(
    stocks: List<Stock>,
    hpps: List<Hpp>,
    batches: List<Batch>,
    currency: AppCurrency,
): ProductCardUiState {
    val hppPerUnit = hpps
        .filter { hpp -> hpp.itemId == id }
        .maxByOrNull { hpp -> hpp.id }
        ?.amount
        ?.toDouble()
        ?: 0.0
    val finalPrice = calculateSellPrice(
        hppPerUnit = hppPerUnit,
        profitTakePercent = profitTakePercent,
    )
    val net = finalPrice - hppPerUnit
    val profitCutPercent = profitTakePercent.toInt()
    val itemStocks = stocks.filter { stock -> stock.itemId == id }
    val activeStock = itemStocks
        .filter { stock -> stock.amount > 0.0 }
        .minByOrNull { stock -> stock.id }
    val activeBatchStock = activeStock?.amount ?: 0.0
    val activeBatchCode = activeStock
        ?.let { stock -> batches.firstOrNull { batch -> batch.id == stock.batchId } }
        ?.code
        ?.takeIf { code -> code.isNotBlank() }
        ?: "No active batch"

    return ProductCardUiState(
        itemId = id,
        name = name.orEmpty(),
        finalPrice = MoneyFormatService.format(finalPrice, currency),
        currentSellPrice = MoneyFormatService.format(currentSellPrice, currency),
        profitCutPercent = profitCutPercent,
        activeBatchCode = activeBatchCode,
        totalStock = "${formatAmount(activeBatchStock)} ${unit.label()}",
        hppPerUnit = MoneyFormatService.format(hppPerUnit, currency),
        netIncome = MoneyFormatService.format(net, currency),
    )
}

private fun UnitType.label(): String = name

private fun formatAmount(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

private fun calculateSellPrice(
    hppPerUnit: Double,
    profitTakePercent: Double,
): Double {
    return hppPerUnit * (1.0 + profitTakePercent / 100.0)
}
