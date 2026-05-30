package com.owl.minerva.stocklab.ui.view

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.owl.minerva.stocklab.database.StockLabDatabase
import com.owl.minerva.stocklab.enums.UnitType
import com.owl.minerva.stocklab.model.Item
import com.owl.minerva.stocklab.repository.*
import com.owl.minerva.stocklab.service.CurrencySettingsStore
import com.owl.minerva.stocklab.service.ItemHppComponentInput
import com.owl.minerva.stocklab.service.ItemService
import com.owl.minerva.stocklab.service.MoneyFormatService
import com.owl.minerva.stocklab.ui.theme.StockLabTheme
import kotlinx.coroutines.launch

class ProductStoreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockLabTheme {
                ProductStoreContainer()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProductStorePreview() {
    StockLabTheme {
        ProductStoreContainer()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductStoreContainer(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedCurrency = remember(context) {
        CurrencySettingsStore(context).getCurrency()
    }
    val itemService = remember(context) {
        val database = StockLabDatabase.getInstance(context)
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

    var name by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var profitTakePercent by remember { mutableStateOf("") }
    var initialStock by remember { mutableStateOf("") }
    var tax by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("") }
    var handling by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var production by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(UnitType.PCS) }
    var unitExpanded by remember { mutableStateOf(false) }
    var nextDynamicCostId by remember { mutableStateOf(0) }
    val dynamicCosts = remember { mutableStateListOf<DynamicCostInput>() }
    val currentHppPerUnit = calculateHppPerUnit(
        buyPrice = buyPrice,
        tax = tax,
        fee = fee,
        packaging = packaging,
        handling = handling,
        cargo = cargo,
        production = production,
        dynamicCosts = dynamicCosts,
    )
    val profitTakePercentValue = profitTakePercent.toDoubleOrNull() ?: 0.0
    val finalSellPricePreview = calculateSellPrice(
        hppPerUnit = currentHppPerUnit.toDouble(),
        profitTakePercent = profitTakePercentValue,
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
                    Text(
                        text = "Add Product",
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
                    scope.launch {
                        try {
                            val fixedCosts = listOf(
                                ItemHppComponentInput("Buy Price", parseCostAmount(buyPrice)),
                                ItemHppComponentInput("Tax", parseCostAmount(tax)),
                                ItemHppComponentInput("Fee", parseCostAmount(fee)),
                                ItemHppComponentInput("Packaging", parseCostAmount(packaging)),
                                ItemHppComponentInput("Handling", parseCostAmount(handling)),
                                ItemHppComponentInput("Cargo", parseCostAmount(cargo)),
                                ItemHppComponentInput("Production", parseCostAmount(production)),
                            )
                            val extraCosts = dynamicCosts.mapNotNull { cost ->
                                val costName = cost.name.trim()
                                val costAmount = parseCostAmount(cost.amount)

                                if (costName.isBlank() && costAmount == 0L) {
                                    null
                                } else {
                                    require(costName.isNotBlank()) { "Extra cost name cannot be blank." }
                                    ItemHppComponentInput(costName, costAmount)
                                }
                            }

                            itemService.store(
                                item = Item(
                                    name = name.trim(),
                                    buyPrice = buyPrice.toDoubleOrNull() ?: 0.0,
                                    profitTakePercent = profitTakePercentValue,
                                    unit = unit,
                                ),
                                initialStockAmount = initialStock.toDoubleOrNull() ?: 0.0,
                                hppComponents = fixedCosts + extraCosts,
                            )
                            snackbarHostState.showSnackbar("Product saved")
                            (context as? Activity)?.finish()
                        } catch (error: IllegalArgumentException) {
                            snackbarHostState.showSnackbar(error.message ?: "Invalid product")
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
                    Text(text = "Save")
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            FormSectionHeader(title = "General")

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "Name")
                },
                singleLine = true,
            )

            OutlinedTextField(
                value = initialStock,
                onValueChange = { initialStock = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                label = {
                    Text(text = "Initial Stock")
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
                    Text(text = "Profit Take (%)")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            AssistChip(
                onClick = {},
                modifier = Modifier.padding(top = 12.dp),
                label = {
                    Text(text = "Final Sell Price: ${MoneyFormatService.format(finalSellPricePreview, selectedCurrency)}")
                },
            )

            FormSectionHeader(title = "Cost Per Unit")

            OutlinedTextField(
                value = buyPrice,
                onValueChange = { buyPrice = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                label = {
                    Text(text = "Buy Price Per Unit")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            CostAmountField(
                value = tax,
                onValueChange = { tax = it },
                label = "Tax Per Unit",
            )

            CostAmountField(
                value = fee,
                onValueChange = { fee = it },
                label = "Fee Per Unit",
            )

            CostAmountField(
                value = packaging,
                onValueChange = { packaging = it },
                label = "Packaging Per Unit",
            )

            CostAmountField(
                value = handling,
                onValueChange = { handling = it },
                label = "Handling Per Unit",
            )

            CostAmountField(
                value = cargo,
                onValueChange = { cargo = it },
                label = "Cargo Per Unit",
            )

            CostAmountField(
                value = production,
                onValueChange = { production = it },
                label = "Production Per Unit",
            )

            FormSectionHeader(title = "Unit")

            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = !unitExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                OutlinedTextField(
                    value = unit.name,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    label = {
                        Text(text = "Unit")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                    },
                    singleLine = true,
                )

                ExposedDropdownMenu(
                    expanded = unitExpanded,
                    onDismissRequest = { unitExpanded = false },
                ) {
                    UnitType.entries.forEach { unitType ->
                        DropdownMenuItem(
                            text = {
                                Text(text = unitType.name)
                            },
                            onClick = {
                                unit = unitType
                                unitExpanded = false
                            },
                        )
                    }
                }
            }

            FormSectionHeader(title = "Extra Costs")

            dynamicCosts.forEachIndexed { index, cost ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    OutlinedTextField(
                        value = cost.name,
                        onValueChange = { value ->
                            dynamicCosts[index] = cost.copy(name = value)
                        },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(text = "Cost Name")
                        },
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = cost.amount,
                        onValueChange = { value ->
                            dynamicCosts[index] = cost.copy(amount = value)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        label = {
                            Text(text = "Amount Per Unit")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )

                    IconButton(
                        onClick = { dynamicCosts.removeAt(index) },
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .width(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove cost",
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    dynamicCosts.add(DynamicCostInput(id = nextDynamicCostId))
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
                    text = "Add New Cost",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

private data class DynamicCostInput(
    val id: Int,
    val name: String = "",
    val amount: String = "",
)

@Composable
private fun FormSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun CostAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        label = {
            Text(text = label)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

private fun parseCostAmount(value: String): Long {
    return value.toDoubleOrNull()?.toLong() ?: 0L
}

private fun calculateHppPerUnit(
    buyPrice: String,
    tax: String,
    fee: String,
    packaging: String,
    handling: String,
    cargo: String,
    production: String,
    dynamicCosts: List<DynamicCostInput>,
): Long {
    return parseCostAmount(buyPrice) +
        parseCostAmount(tax) +
        parseCostAmount(fee) +
        parseCostAmount(packaging) +
        parseCostAmount(handling) +
        parseCostAmount(cargo) +
        parseCostAmount(production) +
        dynamicCosts.sumOf { cost -> parseCostAmount(cost.amount) }
}

private fun calculateSellPrice(
    hppPerUnit: Double,
    profitTakePercent: Double,
): Double {
    return hppPerUnit * (1.0 + profitTakePercent / 100.0)
}
