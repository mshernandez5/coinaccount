package com.mshernandez.coinaccount.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class MaxAmountCoinSelectorTest
{
    private CoinSelector<Long> coinSelector = new MaxAmountCoinSelector<>();
    private CoinEvaluator<Long> evaluator = new LongEvaluator();

    @Test
    public void emptyInputsShouldReturnInvalidResult()
    {
        Set<Long> inputs = new HashSet<>();
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 0L);
        assertFalse(result.isValid());
    }

    @Test
    public void singleInputShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 0L);
        assertTrue(result.isValid());
        assertEquals(inputs, result.getSelection());
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
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 0L);
        assertTrue(result.isValid());
        assertEquals(inputs, result.getSelection());
    }

    @Test
    public void multiInputSomeInvalidShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(-5L);
        inputs.add(10L);
        Set<Long> expected = new HashSet<>();
        expected.add(10L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 0L);
        assertTrue(result.isValid());
        assertEquals(expected, result.getSelection());
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
        CoinEvaluator<Long> evaluatorSimulatingFees = new LongEvaluator(fee);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluatorSimulatingFees, inputs, 0L);
        assertTrue(result.isValid());
        assertEquals(expected, result.getSelection());
    }

    @Test
    public void minimumTargetMetShouldReturnAllInputs()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(20L);
        inputs.add(10L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 30L);
        assertTrue(result.isValid());
        assertEquals(inputs, result.getSelection());
    }

    @Test
    public void minimumTargetNotMetShouldReturnInvalidResult()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(20L);
        inputs.add(10L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 31L);
        assertFalse(result.isValid());
    }
}
