package com.resonote.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.resonote.core.designsystem.theme.ResonoteShapes
import org.junit.Test

class TextFieldMd3ContractTest {
    @Test
    fun pinnedMaterial3OutlinedTextField_matchesFrozenDimensions() {
        assertThat(OutlinedTextFieldDefaults.MinHeight).isEqualTo(56.dp)
        assertThat(OutlinedTextFieldDefaults.UnfocusedBorderThickness).isEqualTo(1.dp)
        assertThat(OutlinedTextFieldDefaults.FocusedBorderThickness).isEqualTo(2.dp)
        assertThat(resonoteUnfocusedTextFieldBorderThickness(isError = false)).isEqualTo(1.dp)
        assertThat(resonoteUnfocusedTextFieldBorderThickness(isError = true)).isEqualTo(2.dp)
    }

    @Test
    fun resonoteOutlinedTextFieldShape_matchesFrozenFourDpRadius() {
        val shape = ResonoteShapes.extraSmall as RoundedCornerShape

        assertThat(shape.topStart.toPx(Size(100f, 100f), Density(1f))).isEqualTo(4f)
    }
}
