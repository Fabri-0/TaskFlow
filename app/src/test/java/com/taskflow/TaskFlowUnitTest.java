package com.taskflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.taskflow.utils.DateUtils;
import com.taskflow.utils.ProgressUtils;
import com.taskflow.utils.Validators;

import org.junit.Test;

public class TaskFlowUnitTest {
    @Test
    public void invalidEmail_isRejected() {
        assertFalse(Validators.isValidEmail("correo-invalido"));
    }

    @Test
    public void shortPassword_isRejected() {
        assertFalse(Validators.isValidPassword("12345"));
    }

    @Test
    public void blankTaskTitle_isRejected() {
        assertFalse(Validators.isValidTaskTitle("   "));
    }

    @Test
    public void progressTwoOfFive_isFortyPercent() {
        assertEquals(40, ProgressUtils.calculatePercentage(2, 5));
    }

    @Test
    public void overdueDate_isDetected() {
        long yesterday = DateUtils.startOfToday() - 60_000L;
        assertTrue(DateUtils.isOverdue(yesterday, false));
        assertFalse(DateUtils.isOverdue(yesterday, true));
    }

    @Test
    public void timestampFormatsToReadableText() {
        assertFalse(DateUtils.formatDate(1_700_000_000_000L).isEmpty());
    }
}
