package test.rpn;

import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rpn.Command;
import rpn.Stack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StackTest {
    private Stack<Command<String>> stack;

    @BeforeEach
    public void init() {
        stack = Stack.newInstance();
    }

    @Test
    public void testCanPushStack() {
        stack.push(Arrays.asList(newCommand("test1"), newCommand("test2")));
        assertStack(stack, "test1", "test2");
    }

    @Test
    public void testCanPushEmptyList() {
        stack.push(Arrays.asList());
        assertStack(stack);

        assertThrows(NullPointerException.class, () -> {
            stack.push(null);
        });
    }

    @Test
    public void testCanPushAndPopStack() {
        stack.push(Arrays.asList(newCommand("test1"), newCommand("test2"), newCommand("test3")));
        assertEquals(Arrays.asList("test3"), toList(stack.pop(1).stream()));
        assertStack(stack, "test1", "test2");
        assertEquals(Arrays.asList(), toList(stack.pop(0).stream()));
        assertStack(stack, "test1", "test2");
        assertEquals(Arrays.asList("test1", "test2"), toList(stack.pop(2).stream()));
        assertStack(stack);
    }

    @Test
    public void testCanPopOversize() {
        stack.push(Arrays.asList(newCommand("test1"), newCommand("test2"), newCommand("test3")));
        assertEquals(Arrays.asList(), toList(stack.pop(4).stream()));
        assertEquals(Arrays.asList(), toList(stack.pop(-1).stream()));
        assertStack(stack, "test1", "test2", "test3");
    }

    private void assertStack(Stack<Command<String>> stack, String ... expected) {
        List<String> actual = toList(stack.allElements().stream());
        assertEquals(Arrays.asList(expected), actual);
        assertEquals(stack.size(), expected.length);
        assertEquals(stack.allSize(), expected.length * 2);
    }

    public static List<String> toList(Stream<Command<String>> stream) {
        return stream.map(c -> c.apply().get()).collect(Collectors.toList());
    }

    public static Command<String> newCommand(String test) {
        return new Command<String>() {
            @Override
            public Either<Exception, String> apply() {
                return Either.right(test);
            }

            @Override
            public List<Command<String>> undo() {
                return Arrays.asList();
            }

            @Override
            public int size() {
                return 2;
            }
        };
    }
}
