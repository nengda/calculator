package test.rpn;

import rpn.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import rpn.Number;

public class CalculatorTest {
    Calculator<Number> rpn;

    @BeforeEach
    public void init() {
        rpn = Calculator.getInstance(Number::of, new Supplier<Stack<Command<Number>>>() {
            @Override
            public Stack<Command<Number>> get() {
                return Stacks.newInstance();
            }
        });
    }

    @Test
    public void testCanEvaluateExample1() {
        assertEvaluation(Arrays.asList("5", "2"), "5", "2");
    }

    @Test
    public void testCanEvaluateExample2() {
        assertEvaluation(Arrays.asList("1.4142135623"), "2", "sqrt");
        assertEvaluation(Arrays.asList("3"), "clear", "9", "sqrt");
    }

    @Test
    public void testCanEvaluateExample3() {
        assertEvaluation(Arrays.asList("3"), "5", "2", "-");
        assertEvaluation(Arrays.asList("0"), "3", "-");
        assertEvaluation(Arrays.asList(), "clear");
    }

    @Test
    public void testCanEvaluateExample4() {
        assertEvaluation(Arrays.asList("5", "4", "3", "2"), "5", "4", "3", "2");
        assertEvaluation(Arrays.asList("20"), "undo", "undo", "*");
        assertEvaluation(Arrays.asList("100"), "5", "*");
        assertEvaluation(Arrays.asList("20", "5"), "undo");
    }

    @Test
    public void testCanEvaluateExample5() {
        assertEvaluation(Arrays.asList("7", "6"), "7", "12", "2", "/");
        assertEvaluation(Arrays.asList("42"), "*");
        assertEvaluation(Arrays.asList("10.5"), "4", "/");
    }

    @Test
    public void testCanEvaluateExample6() {
        assertEvaluation(Arrays.asList("1", "2", "3", "4", "5"), "1", "2", "3", "4", "5");
        assertEvaluation(Arrays.asList("1", "2", "3", "20"), "*");
        assertEvaluation(Arrays.asList("-1"), "clear", "3", "4", "-");
    }

    @Test
    public void testCanEvaluateExample7() {
        assertEvaluation(Arrays.asList("1", "2", "3", "4", "5"), "1", "2", "3", "4", "5");
        assertEvaluation(Arrays.asList("120"), "*", "*", "*", "*");
    }

    @Test
    public void testCanEvaluateExample8() {
        Optional<Exception> maybeException = rpn.offer("1", "2", "3", "*", "5", "+", "*", "*", "6", "5");
        assertTrue(maybeException.isPresent());
        assertEquals(IllegalArgumentException.class, maybeException.get().getClass());
        assertEquals("Operator '*' (position 8), insufficient parameter", maybeException.get().getMessage());
        assertEquals(Arrays.asList("11"), rpn.evaluate().stream().map(n -> n.get().toString()).collect(Collectors.toList()));
    }

    @Test
    public void testCanEvaluateInvalidOperator() {
        Optional<Exception> maybeException = rpn.offer("Invalid");
        assertTrue(maybeException.isPresent());
        assertEquals(IllegalArgumentException.class, maybeException.get().getClass());
        assertEvaluation(Arrays.asList("100"), "20", "5", "*");
        assertEvaluation(Arrays.asList("20", "5"), "undo");
        maybeException = rpn.offer("null");
        assertTrue(maybeException.isPresent());
        assertEquals(IllegalArgumentException.class, maybeException.get().getClass());
    }


    private void assertEvaluation(List<String> expected, String ... input) {
        Optional<Exception> maybeException = rpn.offer(input);
        assertFalse(maybeException.isPresent());
        assertEquals(expected, rpn.evaluate().stream().map(n -> n.get().toString()).collect(Collectors.toList()));
    }

}
