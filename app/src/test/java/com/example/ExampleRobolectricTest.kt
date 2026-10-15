package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.actions.DeviceActionRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NOVA AI", appName)
    }

    @Test
    fun `test action registry handles battery tool`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registry = DeviceActionRegistry(context)
        val result = registry.executeTool("get_battery_info", "{}")
        assertNotNull(result)
        assertEquals("get_battery_info", result.actionName)
    }

    @Test
    fun `test action registry handles copy to clipboard tool`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registry = DeviceActionRegistry(context)
        val result = registry.executeTool("copy_to_clipboard", "{\"text\": \"Hello NOVA\"}")
        assertTrue(result.success)
        assertEquals("copy_to_clipboard", result.actionName)
    }

    @Test
    fun `test action registry handles device specs tool`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registry = DeviceActionRegistry(context)
        val result = registry.executeTool("get_device_specs", "{}")
        assertNotNull(result)
        assertEquals("get_device_specs", result.actionName)
    }

    @Test
    fun `test action registry validates invalid tool`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registry = DeviceActionRegistry(context)
        val result = registry.executeTool("non_existent_tool", "{}")
        assertEquals(false, result.success)
        assertEquals("Unknown Tool", result.summary)
    }
}
