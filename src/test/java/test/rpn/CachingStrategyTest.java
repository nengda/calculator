package rpn;

import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CachingStrategyTest {
    private CacheStrategy<String> strategy;

    @BeforeEach
    public void init() {
        strategy = CacheStrategy.get();
    }

    @Test
    public void testCanCacheExecution() {
        String value = "test";
        Command<String> command = newCommand(value);
        assertEquals(value, command.apply().get());
        assertEquals(1, command.size());
        command = strategy.apply(command);
        assertEquals(value, command.apply().get());
        assertEquals(value, command.apply().get());
        assertEquals(2, command.size());
    }

    private Command<String> newCommand(String test) {
        return new Command<String>() {
            int count = 0;
            @Override
            public Either<Exception, String> apply() {
                count++;
                return Either.right(test);
            }

            @Override
            public List<Command<String>> undo() {
                return Arrays.asList();
            }

            @Override
            public int size() {
                return count;
            }
        };
    }
}
