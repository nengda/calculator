package rpn;

import java.util.Optional;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CalculatorDemo {
    public static void main(String[] args) {
        Calculator<Number> rpn = Calculator.getInstance(Number::of, new Supplier<Stack<Command<Number>>>() {
            @Override
            public Stack<Command<Number>> get() {
                return Stack.newInstance();
            }
        });
        Scanner in = new Scanner(System.in);

        while(true) {
            System.out.println("Enter list of numbers and operators. Supported Operators are: + - * / undo clear.");
            String s = in.nextLine();
            if (s.equals("quit")) return;
            Optional<Exception> maybeException = rpn.push(s.split(" "));
            if (maybeException.isPresent())
                System.out.println(maybeException.get().getMessage());
            String result = rpn.evaluate().stream().map(r -> {
                if (r.isLeft())
                    return r.getLeft().getMessage();
                else
                    return r.get().toString();}).collect(Collectors.joining(" "));
            System.out.println("Stack: " + result);
        }

    }
}
