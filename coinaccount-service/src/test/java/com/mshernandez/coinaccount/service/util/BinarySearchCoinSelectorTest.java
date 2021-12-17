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
        CoinSelectionState<Long> result = new CoinSelectionBuilder<Long>()
            .step(coinSelector, inputs)
            .evaluator(evaluator)
            .target(5L)
            .select();
        assertFalse(result.isComplete());
    }

    @Test
    public void singleInputMatchingTargetShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        CoinSelectionState<Long> result = new CoinSelectionBuilder<Long>()
            .step(coinSelector, inputs)
            .evaluator(evaluator)
            .target(5L)
            .select();
        assertTrue(result.isComplete());
        assertEquals(inputs, result.getSelection());
    }

    @Test
    public void singleInputLesserTargetShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        CoinSelectionState<Long> result = new CoinSelectionBuilder<Long>()
            .step(coinSelector, inputs)
            .evaluator(evaluator)
            .target(2L)
            .select();
        assertTrue(result.isComplete());
        assertEquals(inputs, result.getSelection());
    }

    @Test
    public void singleInputLessThanTargetShouldReturnInvalidResult()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        CoinSelectionState<Long> result = new CoinSelectionBuilder<Long>()
            .step(coinSelector, inputs)
            .evaluator(evaluator)
            .target(8L)
            .select();
        assertFalse(result.isComplete());
    }

    @Test
    public void multiInputMatchingTargetShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        inputs.add(10L);
        CoinSelectionState<Long> result = new CoinSelectionBuilder<Long>()
            .step(coinSelector, inputs)
            .evaluator(evaluator)
            .target(15L)
            .select();
        assertTrue(result.isComplete());
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
        CoinSelectionState<Long> result = new CoinSelectionBuilder<Long>()
            .step(coinSelector, inputs)
            .evaluator(evaluator)
            .target(10L)
            .select();
        assertTrue(result.isComplete());
        assertEquals(expected, result.getSelection());
    }

    @Test
    public void multiInputSomeInvalidShouldReturnInvalidResult()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        inputs.add(-10L);
        CoinSelectionState<Long> result = new CoinSelectionBuilder<Long>()
            .step(coinSelector, inputs)
            .evaluator(evaluator)
            .target(10L)
            .select();
        assertFalse(result.isComplete());
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
        CoinSelectionState<Long> result = new CoinSelectionBuilder<Long>()
            .step(coinSelector, inputs)
            .evaluator(evaluator)
            .target(65L)
            .select();
        assertTrue(result.isComplete());
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
        CoinSelectionState<Long> result = new CoinSelectionBuilder<Long>()
            .step(coinSelector, inputs)
            .evaluator(evaluator)
            .target(45L)
            .select();
        assertTrue(result.isComplete());
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
        CoinSelectionState<Long> result = new CoinSelectionBuilder<Long>()
            .step(coinSelector, inputs)
            .evaluator(evaluator)
            .target(target)
            .select();
        assertTrue(result.isComplete());
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
        CoinSelectionState<Long> result = new CoinSelectionBuilder<Long>()
            .step(coinSelector, inputs)
            .evaluator(evaluator)
            .target(target)
            .select();
        assertTrue(result.isComplete());
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