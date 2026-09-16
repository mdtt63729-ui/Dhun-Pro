package me.saket.squiggles
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
object SquigglySlider { data class SquigglesSpec(val amplitude: Dp = 2.dp, val strokeWidth: Dp = 4.dp) }
@Composable fun SquigglySlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, valueRange: ClosedFloatingPointRange<Float> = 0f..1f, onValueChangeFinished: (() -> Unit)? = null, colors: SliderColors? = null, squigglesSpec: SquigglySlider.SquigglesSpec = SquigglySlider.SquigglesSpec()) { Slider(value=value,onValueChange=onValueChange,modifier=modifier,enabled=enabled,valueRange=valueRange,onValueChangeFinished=onValueChangeFinished,colors=colors ?: androidx.compose.material3.SliderDefaults.colors()) }
