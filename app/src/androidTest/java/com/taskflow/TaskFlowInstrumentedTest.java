package com.taskflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TaskFlowInstrumentedTest {
    @Test
    public void appContext_usesTaskFlowPackage() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.taskflow", context.getPackageName());
    }

    @Test
    public void uiIds_areReadyForEspressoFlows() {
        assertTrue(R.id.fabAdd != 0);
        assertTrue(R.id.editQuickTitle != 0);
    }
}
