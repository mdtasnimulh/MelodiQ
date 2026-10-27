package com.tasnimulhasan.melodiq.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasnimulhasan.designsystem.component.Icon
import com.tasnimulhasan.melodiq.navigation.CustomNavigationItem

/**
 * One left-aligned drawer row. The page you are on is bright and glows; the rest are dimmed.
 */
@Composable
fun CustomNavigationItemView(
    navigationItem: CustomNavigationItem,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (selected) Color.White else Color.White.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = navigationItem.icon),
            contentDescription = "Navigation Item Icon",
            tint = tint
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = navigationItem.title,
            color = tint,
            fontSize = 18.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                shadow = if (selected) Shadow(Color.White.copy(alpha = 0.6f), Offset.Zero, 18f) else null
            ),
        )
    }
}
