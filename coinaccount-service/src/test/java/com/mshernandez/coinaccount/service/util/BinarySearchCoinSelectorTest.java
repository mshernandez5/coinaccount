package com.mshernandez.coinaccount.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests designed to ensure the binary search
 * coin selector selects inputs appropriately.
 */
public class BinarySearchCoinSelectorTest
{
    private CoinSelector<Long> coinSelector = new BinarySearchCoinSelector<>();
    private CoinEvaluator<Long> evaluator = new LongEvaluator();

    @Test
    public void emptyInputsShouldReturnInvalidResult()
    {
        Set<Long> inputs = new HashSet<>();
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 5L);
        assertFalse(result.isValid());
    }

    @Test
    public void singleInputMatchingTargetShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 5L);
        assertTrue(result.isValid());
        assertEquals(inputs, result.getSelection());
    }

    @Test
    public void singleInputLesserTargetShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 2L);
        assertTrue(result.isValid());
        assertEquals(inputs, result.getSelection());
    }

    @Test
    public void singleInputLessThanTargetShouldReturnInvalidResult()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 8L);
        assertFalse(result.isValid());
    }

    @Test
    public void multiInputMatchingTargetShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        inputs.add(10L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 15L);
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
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 10L);
        assertTrue(result.isValid());
        assertEquals(expected, result.getSelection());
    }

    @Test
    public void multiInputSomeInvalidShouldReturnInvalidResult()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        inputs.add(-10L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 10L);
        assert(!result.isValid());
    }

    @Test
    public void multiInputMatchingTargetShouldReturnBestResult()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(1L);
        inputs.add(5L);
        inputs.add(6L);
        inputs.add(10L);
        inputs.add(20L);
        inputs.add(40L);
        Set<Long> expected = new HashSet<>();
        expected.add(40L);
        expected.add(20L);
        expected.add(5L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 65L);
        assertTrue(result.isValid());
        assertEquals(expected, result.getSelection());
    }

    @Test
    public void multiInputApproximateTargetShouldReturnBestResult()
    {
        // Assumes BinarySearchCoinSelector Initialized With Default Parameters
        Set<Long> inputs = new HashSet<>();
        inputs.add(4L);
        inputs.add(40L);
        inputs.add(70L);
        inputs.add(100L);
        Set<Long> expected = new HashSet<>();
        expected.add(70L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, 45L);
        assertTrue(result.isValid());
        assertEquals(expected, result.getSelection());
    }

    @Test
    public void largeNumInputsShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        for (long l = 1; l < 1250; l += 2)
        {
            inputs.add(l);
        }
        long target = 281625L;
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, target);
        assertTrue(result.isValid());
        long sum = 0L;
        for (long l : result.getSelection())
        {
            sum += l;
        }
        assertEquals(sum, target);
    }

    @Test
    public void setIterationShouldFollowSelectionOrder()
    {
        long target = 45L;
        Set<Long> inputs = new HashSet<>();
        inputs.add(10L);
        inputs.add(50L);
        inputs.add(40L);
        inputs.add(1L);
        inputs.add(4L);
        Set<Long> expectedOrder = new LinkedHashSet<>();
        expectedOrder.add(40L);
        expectedOrder.add(4L);
        expectedOrder.add(1L);
        CoinSelectionResult<Long> result = coinSelector.selectInputs(evaluator, inputs, target);
        assertTrue(result.isValid());
        Iterator<Long> expectedIterator = expectedOrder.iterator();
        Iterator<Long> selectedIterator = result.getSelection().iterator();
        while (expectedIterator.hasNext())
        {
            assert(selectedIterator.hasNext());
            assertEquals(expectedIterator.next(), selectedIterator.next());
        }
        assert(!selectedIterator.hasNext());
    }
}