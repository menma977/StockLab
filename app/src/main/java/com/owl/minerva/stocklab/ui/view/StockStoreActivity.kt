package com.owl.minerva.stocklab.ui.view

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.owl.minerva.stocklab.R
import com.owl.minerva.stocklab.database.StockLabDatabase
import com.owl.minerva.stocklab.repository.*
import com.owl.minerva.stocklab.service.*
import com.owl.minerva.stocklab.ui.components.CostAmountField
import com.owl.minerva.stocklab.ui.components.FormSectionHeader
import com.owl.minerva.stocklab.ui.components.clearFocusOnTapOutside
import com.owl.minerva.stocklab.ui.setupEdgeToEdge
import com.owl.minerva.stocklab.ui.theme.StockLabTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StockStoreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContent {
            StockLabTheme {
                StockStoreContainer(
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
fun StockStorePreview() {
    StockLabTheme {
        StockStoreContainer()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockStoreContainer(
    modifier: Modifier = Modifier,
    itemId: Long = 0L,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val stockSavedMessage = stringResource(R.string.stock_saved)
    val invalidStockMessage = stringResource(R.string.error_invalid_stock)
    val unexpectedActionMessage = stringResource(R.string.error_unexpected_action)
    val selectedCurrency = remember(context) {
        CurrencySettingsStore(context).getCurrency()
    }
    val database = remember(context) {
        StockLabDatabase.getInstance(context)
    }
    val stockBatchService = remember(context) {
        StockBatchService(
            itemRepository = ItemRepositoryImpl(database.itemDao()),
            stockRepository = StockRepositoryImpl(database.stockDao()),
            batchRepository = BatchRepositoryImpl(database.batchDao()),
            stockInRepository = StockInRepositoryImpl(database.stockInDao()),
            hppRepository = HppRepositoryImpl(database.hppDao()),
            hppComponentRepository = HppComponentRepositoryImpl(database.hppComponentDao()),
            ledgerRepository = LedgerRepositoryImpl(database.ledgerDao()),
            batchCostRepository = BatchCostRepositoryImpl(database.batchCostDao()),
        )
    }

    var productName by remember { mutableStateOf("") }
    var existingProfitTakePercent by remember { mutableDoubleStateOf(0.0) }
    var stockAmount by remember { mutableStateOf("") }
    var profitTakePercent by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var tax by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("") }
    var handling by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var production by remember { mutableStateOf("") }
    var nextDynamicCostId by remember { mutableIntStateOf(0) }
    var isSavingStock by remember { mutableStateOf(false) }
    val dynamicCosts = remember { mutableStateListOf<HppCostInput>() }

    LaunchedEffect(itemId) {
        if (itemId > 0L) {
            val item = database.itemDao().getById(itemId)
            productName = item?.name.orEmpty()
            existingProfitTakePercent = item?.profitTakePercent ?: 0.0
            val hpp = database.hppDao().getLatestByItemId(itemId)
            val components = hpp?.let { hppData ->
                database.hppComponentDao().getByHppId(hppData.id)
            }.orEmpty()
            val fixedCosts = components.associateBy { component -> component.name.lowercase() }
            buyPrice = fixedCosts["buy price"]?.amount?.toString()
                ?: item?.buyPrice?.let { AmountFormatService.format(it) }.orEmpty()
            tax = fixedCosts["tax"]?.amount?.toString().orEmpty()
            fee = fixedCosts["fee"]?.amount?.toString().orEmpty()
            packaging = fixedCosts["packaging"]?.amount?.toString().orEmpty()
            handling = fixedCosts["handling"]?.amount?.toString().orEmpty()
            cargo = fixedCosts["cargo"]?.amount?.toString().orEmpty()
            production = fixedCosts["production"]?.amount?.toString().orEmpty()

            dynamicCosts.clear()
            components
                .filterNot { component -> component.name.lowercase() in HppCostService.fixedCostNames }
                .forEach { component ->
                    dynamicCosts.add(
                        HppCostInput(
                            id = nextDynamicCostId,
                            name = component.name,
                            amount = component.amount.toString(),
                        ),
                    )
                    nextDynamicCostId += 1
                }
        }
    }

    val effectiveProfitTakePercent = profitTakePercent.toDoubleOrNull() ?: existingProfitTakePercent
    val currentHppPerUnit = HppCostService.calculateHppPerUnit(
        buyPrice = buyPrice,
        tax = tax,
        fee = fee,
        packaging = packaging,
        handling = handling,
        cargo = cargo,
        production = production,
        dynamicCosts = dynamicCosts,
    )
    val finalSellPrice = PricingService.calculateSellPrice(
        hppPerUnit = currentHppPerUnit.toDouble(),
        profitTakePercent = effectiveProfitTakePercent,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(elevation = 4.dp),
                title = {
                    Text(text = stringResource(R.string.add_stock))
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
            ExtendedFloatingActionButton(
                onClick = {
                    if (!isSavingStock) scope.launch {
                        isSavingStock = true
                        try {
                            val profitTakePercentInput = profitTakePercent.trim().let { value ->
                                if (value.isBlank()) {
                                    null
                                } else {
                                    value.toDoubleOrNull()
                                        ?: throw AppMessageException(R.string.error_profit_take_valid_number)
                                }
                            }
                            withContext(Dispatchers.IO) {
                                database.withTransaction {
                                    stockBatchService.store(
                                        itemId = itemId,
                                        amount = stockAmount.toDoubleOrNull() ?: 0.0,
                                        hppComponents = HppCostService.buildComponents(
                                            buyPrice = buyPrice,
                                            tax = tax,
                                            fee = fee,
                                            packaging = packaging,
                                            handling = handling,
                                            cargo = cargo,
                                            production = production,
                                            dynamicCosts = dynamicCosts,
                                        ),
                                        profitTakePercent = profitTakePercentInput,
                                    )
                                }
                            }
                            snackbarHostState.showSnackbar(stockSavedMessage)
                            (context as? Activity)?.finish()
                        } catch (error: AppMessageException) {
                            snackbarHostState.showSnackbar(resources.getString(error.messageResId))
                        } catch (error: IllegalArgumentException) {
                            snackbarHostState.showSnackbar(error.message ?: invalidStockMessage)
                        } catch (error: Exception) {
                            snackbarHostState.showSnackbar(unexpectedActionMessage)
                        } finally {
                            isSavingStock = false
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(text = stringResource(R.string.action_save))
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .clearFocusOnTapOutside(focusManager)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            FormSectionHeader(title = stringResource(R.string.stock))

            OutlinedTextField(
                value = productName,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = stringResource(R.string.product))
                },
                readOnly = true,
                singleLine = true,
            )

            OutlinedTextField(
                value = stockAmount,
                onValueChange = { stockAmount = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                label = {
                    Text(text = stringResource(R.string.stock_amount))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            OutlinedTextField(
                value = profitTakePercent,
                onValueChange = { profitTakePercent = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                label = {
                    Text(text = stringResource(R.string.profit_take_percent))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            AssistChip(
                onClick = {},
                modifier = Modifier.padding(top = 12.dp),
                label = {
                    Text(
                        text = stringResource(
                            R.string.hpp_per_unit_format,
                            MoneyFormatService.format(currentHppPerUnit.toDouble(), selectedCurrency),
                        ),
                    )
                },
            )

            AssistChip(
                onClick = {},
                modifier = Modifier.padding(top = 8.dp),
                label = {
                    Text(
                        text = stringResource(
                            R.string.final_sell_price_format,
                            MoneyFormatService.format(finalSellPrice, selectedCurrency),
                        ),
                    )
                },
            )

            FormSectionHeader(title = stringResource(R.string.hpp_costs))

            CostAmountField(
                value = buyPrice,
                onValueChange = { buyPrice = it },
                label = stringResource(R.string.buy_price_per_unit),
            )

            CostAmountField(
                value = tax,
                onValueChange = { tax = it },
                label = stringResource(R.string.tax_per_unit),
            )

            CostAmountField(
                value = fee,
                onValueChange = { fee = it },
                label = stringResource(R.string.fee_per_unit),
            )

            CostAmountField(
                value = packaging,
                onValueChange = { packaging = it },
                label = stringResource(R.string.packaging_per_unit),
            )

            CostAmountField(
                value = handling,
                onValueChange = { handling = it },
                label = stringResource(R.string.handling_per_unit),
            )

            CostAmountField(
                value = cargo,
                onValueChange = { cargo = it },
                label = stringResource(R.string.cargo_per_unit),
            )

            CostAmountField(
                value = production,
                onValueChange = { production = it },
                label = stringResource(R.string.production_per_unit),
            )

            FormSectionHeader(title = stringResource(R.string.extra_costs))

            dynamicCosts.forEachIndexed { index, cost ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    OutlinedTextField(
                        value = cost.name,
                        onValueChange = { value ->
                            dynamicCosts.updateById(cost.id) { current ->
                                current.copy(name = value)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(text = stringResource(R.string.cost_name))
                        },
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = cost.amount,
                        onValueChange = { value ->
                            dynamicCosts.updateById(cost.id) { current ->
                                current.copy(amount = value)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        label = {
                            Text(text = stringResource(R.string.amount_per_unit))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )

                    IconButton(
                        onClick = {
                            dynamicCosts.removeAll { current -> current.id == cost.id }
                        },
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .width(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.remove_cost),
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    dynamicCosts.add(HppCostInput(id = nextDynamicCostId))
                    nextDynamicCostId += 1
                },
                modifier = Modifier.padding(top = 12.dp),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(R.string.add_new_cost),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun SnapshotStateList<HppCostInput>.updateById(
    id: Int,
    transform: (HppCostInput) -> HppCostInput,
) {
    val currentIndex = indexOfFirst { cost -> cost.id == id }
    if (currentIndex >= 0) {
        this[currentIndex] = transform(this[currentIndex])
    }
}
