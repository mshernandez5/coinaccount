package com.mshernandez.vertconomy.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class MaxAmountCoinSelectorTest
{
    private CoinSelector<Long> coinSelector = new MaxAmountCoinSelector<>();
    private Evaluator<Long> evaluator = new LongEvaluator();

    @Test
    public void emptyInputsShouldReturnNull()
    {
        Set<Long> inputs = new HashSet<>();
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 0L);
        assertNull(selected);
    }

    @Test
    public void singleInputShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 0L);
        assertNotNull(selected);
        assertEquals(inputs, selected);
    }

    @Test
    public void multiInputShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(1L);
        inputs.add(5L);
        inputs.add(6L);
        inputs.add(10L);
        inputs.add(20L);
        inputs.add(40L);
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 0L);
        assertNotNull(selected);
        assertEquals(inputs, selected);
    }

    @Test
    public void multiInputSomeInvalidShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(-5L);
        inputs.add(10L);
        Set<Long> expected = new HashSet<>();
        expected.add(10L);
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 0L);
        assertNotNull(selected);
        assertEquals(expected, selected);
    }

    @Test
    public void multiInputWithFeesShouldReturnOnlyLessThanFees()
    {
        long fee = 10L;
        Set<Long> inputs = new HashSet<>();
        for (long l = -10L; l < 1250L; l += 1L)
        {
            inputs.add(l);
        }
        Set<Long> expected = new HashSet<>();
        for (long l = fee + 1L; l < 1250L; l += 1L)
        {
            expected.add(l);
        }
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, fee, 0L);
        assertNotNull(selected);
        assertEquals(expected, selected);
    }
}
