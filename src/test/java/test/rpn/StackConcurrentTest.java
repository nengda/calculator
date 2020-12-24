package test.rpn;

import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import com.google.code.tempusfugit.concurrency.RepeatingRule;
import com.google.code.tempusfugit.concurrency.annotations.Concurrent;
import com.google.code.tempusfugit.concurrency.annotations.Repeating;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import rpn.Command;
import rpn.Stack;
import static test.rpn.StackTest.newCommand;
import static test.rpn.StackTest.toList;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class StackConcurrentTest {
    private static List<String> TEST_DATA = Arrays.asList("1", "2", "3", "4", "5");
    private static Stack<Command<String>> pushStack;
    private static Stack<Command<String>> popStack;
    private static Stack<Command<String>> popPushSelfStack;
    private static Stack<Command<String>> popPushStack;

    @BeforeClass
    public static void init() {
        pushStack = Stack.newInstance();
        popStack = Stack.newInstance();
        popPushSelfStack = Stack.newInstance();
        while (popStack.size() < 100 * 99 * TEST_DATA.size()) {
            prepareStackWithTestData(popStack);
            prepareStackWithTestData(popPushSelfStack);
        }
        popPushStack = Stack.newInstance();
    }

    @Rule
    public ConcurrentRule concurrently = new ConcurrentRule();

    @Rule
    public RepeatingRule repeats = new RepeatingRule();

    @Test
    @Concurrent(count = 100)
    @Repeating(repetition = 99)
    public void runPushConcurrently() {
        prepareStackWithTestData(pushStack);
    }

    @Test
    @Concurrent(count = 100)
    @Repeating(repetition = 99)
    public void runPopPushConcurrently() {
        popPushStack.push(popStack.pop(TEST_DATA.size()));
        popPushSelfStack.push(popPushSelfStack.pop(TEST_DATA.size()));
    }

    @AfterClass
    public static void canReduceUnderStress() {
        assertStackSize(pushStack);
        assertStackSize(popPushStack);
        assertStackSize(popPushSelfStack);
        assertStackAgainstTestData(pushStack);
        assertStackAgainstTestData(popPushStack);
        assertStackAgainstTestData(popPushSelfStack);
    }

    private static void assertStackSize(Stack<Command<String>> stack) {
        assertEquals(100 * 99 * TEST_DATA.size(), stack.size());
        assertEquals(100 * 99 * TEST_DATA.size() * 2, stack.allSize());

    }
    private static void assertStackAgainstTestData(Stack<Command<String>> stack) {
        while (stack.size() > 0) {
            Assertions.assertEquals(TEST_DATA, toList(stack.pop(TEST_DATA.size()).stream()));
        }
    }

    private static void prepareStackWithTestData(Stack<Command<String>> stack) {
        stack.push(TEST_DATA.stream().map(v -> newCommand(v)).collect(Collectors.toList()));
    }

}
