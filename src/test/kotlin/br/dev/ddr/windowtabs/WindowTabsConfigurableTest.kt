package br.dev.ddr.windowtabs

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent

class WindowTabsConfigurableTest : WindowTabsTestCase() {
    private fun checkBoxes(component: JComponent): List<JBCheckBox> {
        val type = JBCheckBox::class.java

        return UIUtil.findComponentsOfType(component, type)
    }

    fun `test shows the saved settings`() {
        WindowTabsSettings.getInstance().state.showOnMac = true
        val configurable = WindowTabsConfigurable()

        val component = configurable.createComponent()
        configurable.reset()

        assertEquals(
            listOf(false, true, true),
            checkBoxes(component).map { checkBox: JBCheckBox -> checkBox.isSelected },
        )
        assertFalse(configurable.isModified)
        configurable.disposeUIResources()
    }

    fun `test applying saves the settings and refreshes the strips`() {
        val configurable = WindowTabsConfigurable()
        val component = configurable.createComponent()
        configurable.reset()
        val calls = recordTabsChanged()

        checkBoxes(component)[0].isSelected = true
        assertTrue(configurable.isModified)
        configurable.apply()
        dispatchEvents()

        assertTrue(WindowTabsSettings.getInstance().state.showWithSingleProject)
        assertEquals(1, calls())
        configurable.disposeUIResources()
    }
}
