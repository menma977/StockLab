package com.owl.minerva.stocklab.ui.view

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.owl.minerva.stocklab.R
import com.owl.minerva.stocklab.database.StockLabDatabase
import com.owl.minerva.stocklab.enums.AppCurrency
import com.owl.minerva.stocklab.model.*
import com.owl.minerva.stocklab.service.AmountFormatService
import com.owl.minerva.stocklab.service.CurrencySettingsStore
import com.owl.minerva.stocklab.service.MoneyFormatService
import com.owl.minerva.stocklab.service.PricingService
import com.owl.minerva.stocklab.ui.components.AdMobBanner
import com.owl.minerva.stocklab.ui.components.MetricText
import com.owl.minerva.stocklab.ui.components.ProfitBadge
import com.owl.minerva.stocklab.ui.setupEdgeToEdge
import com.owl.minerva.stocklab.ui.theme.StockLabTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private const val HISTORY_PAGE_SIZE = 5

class ProductShowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContent {
            StockLabTheme {
                ProductShowContainer(
                    itemId = intent.getLongExtra(EXTRA_ITEM_ID, 0L),
                )
            }
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
    }
}

@Preview(showBackground = true)
@Composable
fun ProductShowPreview() {
    StockLabTheme {
        val currency = AppCurrency.USD
        ProductShowContainer(
            previewProduct = ProductShowUiState(
                name = "Sample Product",
                unit = "PCS",
                finalPrice = MoneyFormatService.format(100_000.0, currency),
                currentSellPrice = MoneyFormatService.format(0.0, currency),
                hppPerUnit = MoneyFormatService.format(96_500.0, currency),
                netIncome = MoneyFormatService.format(3_500.0, currency),
                profitCutPercent = 3,
                activeBatchCode = "TMBLR4821/B/1",
                activeBatchStock = "120 PCS",
                buyPrice = MoneyFormatService.format(70_000.0, currency),
                totalBatchHpp = MoneyFormatService.format(11_580_000.0, currency),
                batchQuantity = "120 PCS",
                batchHppPerUnit = MoneyFormatService.format(96_500.0, currency),
                templateCosts = listOf(
                    ProductCostUiState("Buy Price", MoneyFormatService.format(70_000.0, currency)),
                    ProductCostUiState("Cargo", MoneyFormatService.format(25_000.0, currency)),
                    ProductCostUiState("Fee", MoneyFormatService.format(1_500.0, currency)),
                ),
                batchSnapshotCosts = listOf(
                    ProductCostUiState("Buy Price", MoneyFormatService.format(70_000.0, currency)),
                    ProductCostUiState("Cargo", MoneyFormatService.format(25_000.0, currency)),
                    ProductCostUiState("Fee", MoneyFormatService.format(1_500.0, currency)),
                ),
                batches = ProductRecordPageUiState(
                    records = listOf(
                        ProductRecordUiState(
                            "TMBLR4821/B/1",
                            "Stock: 120 PCS",
                            "Total HPP: ${MoneyFormatService.format(11_580_000.0, currency)}",
                        ),
                    ),
                    page = 0,
                    totalItems = 1,
                ),
                ledgers = ProductRecordPageUiState(
                    records = listOf(
                        ProductRecordUiState(
                            "TMBLR4821/L/1",
                            "Batch: TMBLR4821/B/1",
                            "IN: ${MoneyFormatService.format(11_580_000.0, currency)}",
                        ),
                    ),
                    page = 0,
                    totalItems = 1,
                ),
                stockIns = ProductRecordPageUiState(
                    records = listOf(
                        ProductRecordUiState("TMBLR4821/SI/1", "Stock ID: 1", "Amount: 120"),
                    ),
                    page = 0,
                    totalItems = 1,
                ),
                stockOuts = ProductRecordPageUiState(
                    records = emptyList(),
                    page = 0,
                    totalItems = 0,
                ),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductShowContainer(
    modifier: Modifier = Modifier,
    itemId: Long = 0L,
    previewProduct: ProductShowUiState? = null,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val selectedCurrency = remember(context) {
        CurrencySettingsStore(context).getCurrency()
    }
    var product by remember { mutableStateOf(previewProduct) }
    var batchPage by remember { mutableIntStateOf(0) }
    var ledgerPage by remember { mutableIntStateOf(0) }
    var stockInPage by remember { mutableIntStateOf(0) }
    var stockOutPage by remember { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = context as? LifecycleOwner

    DisposableEffect(lifecycleOwner, previewProduct) {
        if (previewProduct != null || lifecycleOwner == null) {
            onDispose {}
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshKey += 1
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    LaunchedEffect(itemId, previewProduct, batchPage, ledgerPage, stockInPage, stockOutPage, refreshKey, selectedCurrency) {
        if (previewProduct == null && itemId > 0) {
            val database = StockLabDatabase.getInstance(context)
            val item = database.itemDao().getById(itemId)
            val hpp = database.hppDao().getLatestByItemId(itemId)
            val activeStock = database.stockDao().getFirstAvailableByItemId(itemId)
            val templateCosts = hpp?.let { hppData ->
                database.hppComponentDao().getByHppId(hppData.id)
            }.orEmpty()
            val batch = activeStock?.batchId?.let { batchId ->
                database.batchDao().getById(batchId)
            }
            val batchSnapshotCosts = activeStock?.batchId?.let { batchId ->
                database.batchCostDao().getByBatchId(batchId)
            }.orEmpty()
            val batchTotal = database.batchDao().countByItemId(itemId)
            val ledgerTotal = database.ledgerDao().countByItemId(itemId)
            val stockInTotal = database.stockInDao().countByItemId(itemId)
            val stockOutTotal = database.stockOutDao().countByItemId(itemId)
            val allBatchesForNumbering = database.batchDao().getByItemId(itemId)
            val batches = database.batchDao().getByItemIdPaged(
                itemId = itemId,
                limit = HISTORY_PAGE_SIZE,
                offset = batchPage * HISTORY_PAGE_SIZE,
            )
            val ledgers = database.ledgerDao().getByItemIdPaged(
                itemId = itemId,
                limit = HISTORY_PAGE_SIZE,
                offset = ledgerPage * HISTORY_PAGE_SIZE,
            )
            val stockIns = database.stockInDao().getByItemIdPaged(
                itemId = itemId,
                limit = HISTORY_PAGE_SIZE,
                offset = stockInPage * HISTORY_PAGE_SIZE,
            )
            val stockOuts = database.stockOutDao().getByItemIdPaged(
                itemId = itemId,
                limit = HISTORY_PAGE_SIZE,
                offset = stockOutPage * HISTORY_PAGE_SIZE,
            )

            product = item?.toProductShowUiState(
                hppPerUnit = hpp?.amount?.toDouble() ?: 0.0,
                activeStock = activeStock,
                templateCosts = templateCosts,
                batchSnapshotCosts = batchSnapshotCosts,
                totalBatchHpp = batch?.totalHpp ?: 0,
                allBatchesForNumbering = allBatchesForNumbering,
                batches = batches,
                ledgers = ledgers,
                stockIns = stockIns,
                stockOuts = stockOuts,
                batchPage = batchPage,
                batchTotal = batchTotal,
                ledgerPage = ledgerPage,
                ledgerTotal = ledgerTotal,
                stockInPage = stockInPage,
                stockInTotal = stockInTotal,
                stockOutPage = stockOutPage,
                stockOutTotal = stockOutTotal,
                currency = selectedCurrency,
                resources = resources,
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(elevation = 4.dp),
                title = {
                    Text(
                        text = stringResource(R.string.product_detail),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (previewProduct == null && itemId > 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        context.startActivity(
                            Intent(context, StockStoreActivity::class.java)
                                .putExtra(StockStoreActivity.EXTRA_ITEM_ID, itemId),
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                        )
                    },
                    text = {
                        Text(text = stringResource(R.string.add_stock))
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            product?.let { productState ->
                ProductDetailCard(
                    product = productState,
                    onBatchPageChange = { batchPage = it },
                    onLedgerPageChange = { ledgerPage = it },
                    onStockInPageChange = { stockInPage = it },
                    onStockOutPageChange = { stockOutPage = it },
                )
            } ?: ProductEmptyDetail()
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ProductDetailCard(
    product: ProductShowUiState,
    onBatchPageChange: (Int) -> Unit,
    onLedgerPageChange: (Int) -> Unit,
    onStockInPageChange: (Int) -> Unit,
    onStockOutPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DetailCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.product),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.sold_as_format, product.unit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.current_sell_price_format, product.currentSellPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AdMobBanner()

        DetailCard {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetricText(
                        label = stringResource(R.string.final_price),
                        value = product.finalPrice,
                    )
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.profit_take),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        ProfitBadge(percent = product.profitCutPercent)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricText(
                    label = stringResource(R.string.buy_price_per_unit_lower),
                    value = product.buyPrice,
                )
                MetricText(
                    label = stringResource(R.string.hpp_per_unit),
                    value = product.hppPerUnit,
                    horizontalAlignment = Alignment.End,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricText(
                    label = stringResource(R.string.net_income),
                    value = product.netIncome,
                )
                MetricText(
                    label = stringResource(R.string.unit),
                    value = product.unit,
                    horizontalAlignment = Alignment.End,
                )
            }
        }

        DetailCard {
            Text(
                text = stringResource(R.string.current_fifo_batch),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.current_fifo_batch_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = product.activeBatchCode,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = product.activeBatchStock,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        DetailSection(
            title = stringResource(R.string.reusable_hpp_template),
            description = stringResource(R.string.reusable_hpp_template_description),
            costs = product.templateCosts,
        )

        DetailSection(
            title = stringResource(R.string.current_batch_hpp_snapshot),
            description = stringResource(R.string.current_batch_hpp_snapshot_description),
            costs = product.batchSnapshotCosts,
            footerRows = listOf(
                stringResource(R.string.quantity_label) to product.batchQuantity,
                stringResource(R.string.hpp_per_unit) to product.batchHppPerUnit,
                stringResource(R.string.batch_total_hpp) to product.totalBatchHpp,
            ),
        )

        RecordSection(
            title = stringResource(R.string.batches),
            description = stringResource(R.string.batches_description),
            page = product.batches,
            onPageChange = onBatchPageChange,
        )

        RecordSection(
            title = stringResource(R.string.ledger),
            description = stringResource(R.string.ledger_description),
            page = product.ledgers,
            onPageChange = onLedgerPageChange,
        )

        RecordSection(
            title = stringResource(R.string.stock_in),
            description = stringResource(R.string.stock_in_description),
            page = product.stockIns,
            onPageChange = onStockInPageChange,
        )

        RecordSection(
            title = stringResource(R.string.stock_out),
            description = stringResource(R.string.stock_out_description),
            page = product.stockOuts,
            onPageChange = onStockOutPageChange,
        )
    }
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun ProductEmptyDetail() {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.product_detail_unavailable),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

data class ProductShowUiState(
    val name: String,
    val unit: String,
    val buyPrice: String,
    val finalPrice: String,
    val currentSellPrice: String,
    val hppPerUnit: String,
    val netIncome: String,
    val profitCutPercent: Int,
    val activeBatchCode: String,
    val activeBatchStock: String,
    val totalBatchHpp: String,
    val batchQuantity: String,
    val batchHppPerUnit: String,
    val templateCosts: List<ProductCostUiState>,
    val batchSnapshotCosts: List<ProductCostUiState>,
    val batches: ProductRecordPageUiState,
    val ledgers: ProductRecordPageUiState,
    val stockIns: ProductRecordPageUiState,
    val stockOuts: ProductRecordPageUiState,
)

data class ProductCostUiState(
    val name: String,
    val amount: String,
)

data class ProductRecordUiState(
    val title: String,
    val description: String,
    val trailing: String,
)

data class ProductRecordPageUiState(
    val records: List<ProductRecordUiState>,
    val page: Int,
    val totalItems: Int,
) {
    val totalPages: Int = ((totalItems + HISTORY_PAGE_SIZE - 1) / HISTORY_PAGE_SIZE)
        .coerceAtLeast(1)
    val canGoPrevious: Boolean = page > 0
    val canGoNext: Boolean = page < totalPages - 1
}

private fun Item.toProductShowUiState(
    hppPerUnit: Double,
    activeStock: Stock?,
    templateCosts: List<HppComponent>,
    batchSnapshotCosts: List<BatchCost>,
    totalBatchHpp: Long,
    allBatchesForNumbering: List<Batch>,
    batches: List<Batch>,
    ledgers: List<Ledger>,
    stockIns: List<StockIn>,
    stockOuts: List<StockOut>,
    batchPage: Int,
    batchTotal: Int,
    ledgerPage: Int,
    ledgerTotal: Int,
    stockInPage: Int,
    stockInTotal: Int,
    stockOutPage: Int,
    stockOutTotal: Int,
    currency: AppCurrency,
    resources: Resources,
): ProductShowUiState {
    val finalPrice = PricingService.calculateSellPrice(
        hppPerUnit = hppPerUnit,
        profitTakePercent = profitTakePercent,
    )
    val netIncome = finalPrice - hppPerUnit
    val profitCutPercent = profitTakePercent.toInt()
    return ProductShowUiState(
        name = name.orEmpty(),
        unit = unit.name,
        buyPrice = MoneyFormatService.format(buyPrice, currency),
        finalPrice = MoneyFormatService.format(finalPrice, currency),
        currentSellPrice = MoneyFormatService.format(currentSellPrice, currency),
        hppPerUnit = MoneyFormatService.format(hppPerUnit, currency),
        netIncome = MoneyFormatService.format(netIncome, currency),
        profitCutPercent = profitCutPercent,
        activeBatchCode = activeStock?.batchId
            ?.let { batchId -> allBatchesForNumbering.firstOrNull { batch -> batch.id == batchId } }
            ?.code
            ?.takeIf { code -> code.isNotBlank() }
            ?: resources.getString(R.string.no_active_batch),
        activeBatchStock = "${AmountFormatService.format(activeStock?.amount ?: 0.0)} ${unit.name}",
        totalBatchHpp = MoneyFormatService.format(totalBatchHpp, currency),
        batchQuantity = "${AmountFormatService.format(activeStock?.amount ?: 0.0)} ${unit.name}",
        batchHppPerUnit = MoneyFormatService.format(hppPerUnit, currency),
        templateCosts = templateCosts.map { cost ->
            ProductCostUiState(
                name = localizedCostName(resources, cost.name),
                amount = MoneyFormatService.format(cost.amount, currency),
            )
        },
        batchSnapshotCosts = batchSnapshotCosts.map { cost ->
            ProductCostUiState(
                name = localizedCostName(resources, cost.name),
                amount = MoneyFormatService.format(cost.amount, currency),
            )
        },
        batches = ProductRecordPageUiState(
            records = batches.map { batch ->
                ProductRecordUiState(
                    title = batch.code.ifBlank { resources.getString(R.string.batch_fallback, batch.id) },
                    description = resources.getString(
                        R.string.stock_description,
                        AmountFormatService.format(batch.amount),
                        unit.name,
                        formatTimestamp(batch.createdAt),
                    ),
                    trailing = resources.getString(
                        R.string.total_hpp_format,
                        MoneyFormatService.format(batch.totalHpp, currency),
                    ),
                )
            },
            page = batchPage,
            totalItems = batchTotal,
        ),
        ledgers = ProductRecordPageUiState(
            records = ledgers.map { ledger ->
                val batchCode = allBatchesForNumbering
                    .firstOrNull { batch -> batch.id == ledger.batchId }
                    ?.code
                    ?.takeIf { code -> code.isNotBlank() }
                    ?: resources.getString(R.string.batch_fallback, ledger.batchId)
                ProductRecordUiState(
                    title = ledger.code.ifBlank { resources.getString(R.string.ledger_fallback, ledger.id) },
                    description = resources.getString(
                        R.string.batch_label,
                        "$batchCode • ${formatTimestamp(ledger.createdAt)}",
                    ),
                    trailing = resources.getString(
                        R.string.ledger_direction_amount,
                        ledger.direction.name,
                        MoneyFormatService.format(ledger.amount, currency),
                    ),
                )
            },
            page = ledgerPage,
            totalItems = ledgerTotal,
        ),
        stockIns = ProductRecordPageUiState(
            records = stockIns.map { stockIn ->
                ProductRecordUiState(
                    title = stockIn.code.ifBlank { resources.getString(R.string.stock_in_fallback, stockIn.id) },
                    description = resources.getString(
                        R.string.stock_id_description,
                        stockIn.stockId,
                        formatTimestamp(stockIn.createdAt),
                    ),
                    trailing = resources.getString(R.string.amount_format, AmountFormatService.format(stockIn.amount)),
                )
            },
            page = stockInPage,
            totalItems = stockInTotal,
        ),
        stockOuts = ProductRecordPageUiState(
            records = stockOuts.map { stockOut ->
                ProductRecordUiState(
                    title = stockOut.code.ifBlank { resources.getString(R.string.stock_out_fallback, stockOut.id) },
                    description = resources.getString(
                        R.string.stock_id_description,
                        stockOut.stockId,
                        formatTimestamp(stockOut.createdAt),
                    ),
                    trailing = resources.getString(R.string.amount_format, AmountFormatService.format(stockOut.amount)),
                )
            },
            page = stockOutPage,
            totalItems = stockOutTotal,
        ),
    )
}

@Composable
private fun DetailSection(
    title: String,
    description: String,
    costs: List<ProductCostUiState>,
    modifier: Modifier = Modifier,
    footerRows: List<Pair<String, String>> = emptyList(),
) {
    DetailCard(
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (costs.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_cost_data_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    costs.forEach { cost ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = cost.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = cost.amount,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                if (footerRows.isNotEmpty()) {
                    HorizontalDivider()
                    footerRows.forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordSection(
    title: String,
    description: String,
    page: ProductRecordPageUiState,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailCard(
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (page.records.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_data_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    page.records.forEachIndexed { index, record ->
                        if (index > 0) {
                            HorizontalDivider()
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = record.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = record.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (record.trailing.isNotBlank()) {
                                Text(
                                    text = record.trailing,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { onPageChange(page.page - 1) },
                        enabled = page.canGoPrevious,
                    ) {
                        Text(text = stringResource(R.string.action_previous))
                    }
                    Text(
                        text = stringResource(R.string.page_of_total, page.page + 1, page.totalPages),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { onPageChange(page.page + 1) },
                        enabled = page.canGoNext,
                    ) {
                        Text(text = stringResource(R.string.action_next))
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(value: Long): String {
    return Instant.ofEpochMilli(value)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.getDefault()))
}

private fun localizedCostName(resources: Resources, name: String): String {
    return when (name.lowercase(Locale.getDefault())) {
        "buy price" -> resources.getString(R.string.buy_price_per_unit_lower)
        "tax" -> resources.getString(R.string.tax_per_unit)
        "fee" -> resources.getString(R.string.fee_per_unit)
        "packaging" -> resources.getString(R.string.packaging_per_unit)
        "handling" -> resources.getString(R.string.handling_per_unit)
        "cargo" -> resources.getString(R.string.cargo_per_unit)
        "production" -> resources.getString(R.string.production_per_unit)
        else -> name
    }
}
