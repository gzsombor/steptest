package com.github.gzsombor.steptests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;

public class TestScenario implements Iterable<DynamicTest> {

    record Step(String name, Runnable runnable) {
        public Step {
            Objects.requireNonNull(name, "Name cannot be null");
            Objects.requireNonNull(runnable, "Runnable cannot be null");
        }
    }

    private final List<Step> steps = new ArrayList<>();
    public TestScenario() {
    }

    private static class Iter implements Iterator<DynamicTest> {
        private final TestScenario scenario;
        private int index;
        private boolean successFull = true;
        /**
         * Creates an iterator for the given TestScenario.
         *
         * @param scenario the TestScenario to iterate over
         */
        public Iter(TestScenario scenario) {
            this.scenario = scenario;
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < scenario.steps.size();
        }

        @Override
        public DynamicTest next() {
            final var step = scenario.steps.get(index);
            index++;
            return DynamicTest.dynamicTest(String.format(Locale.ROOT, "[%1$s/%2$s] %3$s", index, scenario.steps.size(), step.name), () -> {
                Assumptions.assumeTrue(successFull, "Previous step failed, skipping this step: " + step.name);
                try {
                    step.runnable.run();
                } catch (Throwable t) {
                    successFull = false;
                    throw t;
                }
            });
        }
    }

    public TestScenario addStep(String name, Runnable runnable) {
        steps.add(new Step(name, runnable));
        return this;
    }

    @Override
    public Iterator<DynamicTest> iterator() {
        return new Iter(this);
    }

}
