package com.fuelmap.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fuelmap.app.data.local.MarkWithItems
import com.fuelmap.app.util.TimeFormat

@Composable
fun MarkHistoryRow(
    mark: MarkWithItems,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                TimeFormat.dateTime(mark.mark.createdAt) + if (mark.mark.isCurrent) " · актуальная" else "",
                style = MaterialTheme.typography.labelMedium
            )
            mark.items.forEach { item ->
                Text(
                    "${item.type.title}: " + if (item.available) "есть · %.2f ₽".format(item.price) else "нет",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            trailing?.invoke()
        }
    }
}
