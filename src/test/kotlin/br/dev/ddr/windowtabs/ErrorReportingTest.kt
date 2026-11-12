package br.dev.ddr.windowtabs

import com.intellij.diagnostic.JetBrainsMarketplaceErrorReportSubmitter
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.extensions.ExtensionPointName

class ErrorReportingTest : WindowTabsTestCase() {
    fun `test the plugin reports its errors to JetBrains Marketplace`() {
        val submitters =
            ExtensionPointName
                .create<ErrorReportSubmitter>("com.intellij.errorHandler")
                .extensionList
                .filter { submitter: ErrorReportSubmitter -> submitter.pluginDescriptor.pluginId.idString == PLUGIN_ID }

        assertEquals(1, submitters.size)
        assertInstanceOf(submitters.single(), JetBrainsMarketplaceErrorReportSubmitter::class.java)
    }

    companion object {
        private const val PLUGIN_ID = "br.dev.ddr.windowtabs"
    }
}
