package test.rpn;

import io.vavr.control.Either;
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
                return Stack.newInstance();
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
        Optional<Exception> maybeException = rpn.push("1", "2", "3", "*", "5", "+", "*", "*", "6", "5");
        assertTrue(maybeException.isPresent());
        assertEquals(IllegalArgumentException.class, maybeException.get().getClass());
        assertEquals("Operator '*' (position 8), insufficient parameter", maybeException.get().getMessage());
        assertEquals(Arrays.asList("11"), rpn.evaluate().stream().map(n -> n.get().toString()).collect(Collectors.toList()));
    }

    @Test
    public void testCanUndoNothing() {
        Optional<Exception> maybeException = rpn.push("undo", "3", "4");
        assertTrue(maybeException.isPresent());
        assertEquals(IllegalArgumentException.class, maybeException.get().getClass());
        assertEquals("Operator 'undo' (position 1), insufficient parameter", maybeException.get().getMessage());
        assertEquals(Arrays.asList(), rpn.evaluate().stream().map(n -> n.get().toString()).collect(Collectors.toList()));
    }

    @Test
    public void testCanClearNothing() {
        assertEvaluation(Arrays.asList(), "clear");
    }

    @Test
    public void testCanEvaluateInvalidOperator() {
        Optional<Exception> maybeException = rpn.push("");
        assertTrue(maybeException.isPresent());
        assertEquals(IllegalArgumentException.class, maybeException.get().getClass());
        assertEvaluation(Arrays.asList("100"), "20", "5", "*");
        assertEvaluation(Arrays.asList("20", "5"), "undo");
        maybeException = rpn.push("null");
        assertTrue(maybeException.isPresent());
        assertEquals(IllegalArgumentException.class, maybeException.get().getClass());
    }

    @Test
    public void testCanEvaluateIllegalArithmeticStateAndRecover() {
        Optional<Exception> maybeException = rpn.push("1", "2", "3", "+", "-", "sqrt", "2", "*", "6", "5", "*");
        assertFalse(maybeException.isPresent());
        List<Either<Exception, Number>> result = rpn.evaluate();
        assertEquals(2, result.size());
        assertEquals(NumberFormatException.class, result.get(0).getLeft().getClass());
        assertEquals("30", result.get(1).get().toString());
        assertEvaluation(Arrays.asList("4", "30"), "undo", "undo", "undo", "undo", "undo", "undo", "-1", "*", "sqrt", "2", "*", "6", "5", "*");
    }

    private void assertEvaluation(List<String> expected, String ... input) {
        Optional<Exception> maybeException = rpn.push(input);
        assertFalse(maybeException.isPresent());
        assertEquals(expected, rpn.evaluate().stream().map(n -> n.get().toString()).collect(Collectors.toList()));
    }

}
