package com.mshernandez.vertconomy.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    private Evaluator<Long> evaluator = new LongEvaluator();

    @Test
    public void emptyInputsShouldReturnNull()
    {
        Set<Long> inputs = new HashSet<>();
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 5L);
        assertNull(selected);
    }

    @Test
    public void singleInputMatchingTargetShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 5L);
        assertNotNull(selected);
        assertEquals(inputs, selected);
    }

    @Test
    public void singleInputLesserTargetShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 2L);
        assertNotNull(selected);
        assertEquals(inputs, selected);
    }

    @Test
    public void singleInputLargerTargetShouldReturnNull()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 8L);
        assertNull(selected);
    }

    @Test
    public void multiInputMatchingTargetShouldReturnValidSet()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        inputs.add(10L);
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 15L);
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
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 10L);
        assertNotNull(selected);
        assertEquals(expected, selected);
    }

    @Test
    public void multiInputSomeInvalidShouldReturnNull()
    {
        Set<Long> inputs = new HashSet<>();
        inputs.add(5L);
        inputs.add(-10L);
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 10L);
        assertNull(selected);
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
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 65L);
        assertNotNull(selected);
        assertEquals(expected, selected);
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
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, 45L);
        assertNotNull(selected);
        assertEquals(expected, selected);
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
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, target);
        assertNotNull(selected);
        long sum = 0L;
        for (long l : selected)
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
        Set<Long> selected = coinSelector.selectInputs(evaluator, inputs, 0L, target);
        Iterator<Long> expectedIterator = expectedOrder.iterator();
        Iterator<Long> selectedIterator = selected.iterator();
        while (expectedIterator.hasNext())
        {
            assert(selectedIterator.hasNext());
            assertEquals(expectedIterator.next(), selectedIterator.next());
        }
        assert(!selectedIterator.hasNext());
    }
}