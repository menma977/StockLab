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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.owl.minerva.stocklab.database.StockLabDatabase
import com.owl.minerva.stocklab.enums.UnitType
import com.owl.minerva.stocklab.model.Item
import com.owl.minerva.stocklab.repository.*
import com.owl.minerva.stocklab.service.*
import com.owl.minerva.stocklab.ui.setupEdgeToEdge
import com.owl.minerva.stocklab.ui.components.CostAmountField
import com.owl.minerva.stocklab.ui.components.FormSectionHeader
import com.owl.minerva.stocklab.ui.components.clearFocusOnTapOutside
import com.owl.minerva.stocklab.ui.theme.StockLabTheme
import kotlinx.coroutines.launch

class ProductStoreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
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
    val focusManager = LocalFocusManager.current
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
    var nextDynamicCostId by remember { mutableIntStateOf(0) }
    val dynamicCosts = remember { mutableStateListOf<HppCostInput>() }
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
    val profitTakePercentValue = profitTakePercent.toDoubleOrNull() ?: 0.0
    val finalSellPricePreview = PricingService.calculateSellPrice(
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
                            itemService.store(
                                item = Item(
                                    name = name.trim(),
                                    buyPrice = buyPrice.toDoubleOrNull() ?: 0.0,
                                    profitTakePercent = profitTakePercentValue,
                                    unit = unit,
                                ),
                                initialStockAmount = initialStock.toDoubleOrNull() ?: 0.0,
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
                .clearFocusOnTapOutside(focusManager)
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
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
                    text = "Add New Cost",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
