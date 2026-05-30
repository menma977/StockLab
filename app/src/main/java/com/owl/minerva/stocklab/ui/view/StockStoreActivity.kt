package com.owl.minerva.stocklab.ui.view

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.owl.minerva.stocklab.database.StockLabDatabase
import com.owl.minerva.stocklab.repository.BatchCostRepositoryImpl
import com.owl.minerva.stocklab.repository.BatchRepositoryImpl
import com.owl.minerva.stocklab.repository.HppComponentRepositoryImpl
import com.owl.minerva.stocklab.repository.HppRepositoryImpl
import com.owl.minerva.stocklab.repository.ItemRepositoryImpl
import com.owl.minerva.stocklab.repository.LedgerRepositoryImpl
import com.owl.minerva.stocklab.repository.StockInRepositoryImpl
import com.owl.minerva.stocklab.repository.StockRepositoryImpl
import com.owl.minerva.stocklab.service.CurrencySettingsStore
import com.owl.minerva.stocklab.service.AmountFormatService
import com.owl.minerva.stocklab.service.HppCostInput
import com.owl.minerva.stocklab.service.HppCostService
import com.owl.minerva.stocklab.service.MoneyFormatService
import com.owl.minerva.stocklab.service.PricingService
import com.owl.minerva.stocklab.service.StockBatchService
import com.owl.minerva.stocklab.ui.components.CostAmountField
import com.owl.minerva.stocklab.ui.components.FormSectionHeader
import com.owl.minerva.stocklab.ui.theme.StockLabTheme
import kotlinx.coroutines.launch

class StockStoreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
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
                    Text(text = "Add Stock")
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
                            val profitTakePercentInput = profitTakePercent.trim().let { value ->
                                if (value.isBlank()) {
                                    null
                                } else {
                                    value.toDoubleOrNull()
                                        ?: throw IllegalArgumentException("Profit take must be a valid number.")
                                }
                            }
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
                            snackbarHostState.showSnackbar("Stock saved")
                            (context as? Activity)?.finish()
                        } catch (error: IllegalArgumentException) {
                            snackbarHostState.showSnackbar(error.message ?: "Invalid stock")
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
            FormSectionHeader(title = "Stock")

            OutlinedTextField(
                value = productName,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "Product")
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
                    Text(text = "Stock Amount")
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
                    Text(text = "HPP Per Unit: ${MoneyFormatService.format(currentHppPerUnit.toDouble(), selectedCurrency)}")
                },
            )

            AssistChip(
                onClick = {},
                modifier = Modifier.padding(top = 8.dp),
                label = {
                    Text(text = "Final Sell Price: ${MoneyFormatService.format(finalSellPrice, selectedCurrency)}")
                },
            )

            FormSectionHeader(title = "HPP Costs")

            CostAmountField(
                value = buyPrice,
                onValueChange = { buyPrice = it },
                label = "Buy Price Per Unit",
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
        }
    }
}
