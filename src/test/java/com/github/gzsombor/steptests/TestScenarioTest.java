package com.github.gzsombor.steptests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.TestAbortedException;

class TestScenarioTest {

    final static Runnable emptyFunction = () -> {
        // Empty function for testing
    };

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create empty scenario with default constructor")
        void shouldCreateEmptyScenarioWithDefaultConstructor() {
            TestScenario scenario = new TestScenario();

            assertFalse(scenario.iterator().hasNext());
        }

        @Test
        @DisplayName("Should create scenario ready for step addition")
        void shouldCreateScenarioReadyForStepAddition() {
            TestScenario scenario = new TestScenario();

            scenario.addStep("Test step", emptyFunction);
            assertTrue(scenario.iterator().hasNext());
        }
    }

    @Nested
    @DisplayName("Step Record Tests")
    class StepRecordTests {

        @Test
        @DisplayName("Should create valid step with name and runnable")
        void shouldCreateValidStepWithNameAndRunnable() {
            Runnable runnable = emptyFunction;
            TestScenario.Step step = new TestScenario.Step("Test", runnable);

            assertEquals("Test", step.name());
            assertEquals(runnable, step.runnable());
        }

        @Test
        @DisplayName("Should throw exception when name is null")
        void shouldThrowExceptionWhenNameIsNull() {
            assertThrows(NullPointerException.class, 
                () -> new TestScenario.Step(null, emptyFunction));
        }

        @Test
        @DisplayName("Should throw exception when runnable is null")
        void shouldThrowExceptionWhenRunnableIsNull() {
            assertThrows(NullPointerException.class, 
                () -> new TestScenario.Step("Test", null));
        }

        @Test
        @DisplayName("Should throw exception when both name and runnable are null")
        void shouldThrowExceptionWhenBothNameAndRunnableAreNull() {
            assertThrows(NullPointerException.class, 
                () -> new TestScenario.Step(null, null));
        }
    }

    @Nested
    @DisplayName("Iterator Behavior Tests")
    class IteratorBehaviorTests {

        @Test
        @DisplayName("Should return true for hasNext when steps exist")
        void shouldReturnTrueForHasNextWhenStepsExist() {
            TestScenario scenario = new TestScenario();
            scenario.addStep("Step 1", emptyFunction);

            assertTrue(scenario.iterator().hasNext());
        }

        @Test
        @DisplayName("Should return false for hasNext after all steps consumed")
        void shouldReturnFalseForHasNextAfterAllStepsConsumed() {
            TestScenario scenario = new TestScenario();
            scenario.addStep("Step 1", emptyFunction);
            var iterator = scenario.iterator();

            assertTrue(iterator.hasNext());
            iterator.next(); // Consume the step
            assertFalse(iterator.hasNext());
        }

        @Test
        @DisplayName("Should return DynamicTest from next method")
        void shouldReturnDynamicTestFromNextMethod() {
            TestScenario scenario = new TestScenario();
            scenario.addStep("Test Step", emptyFunction);

            DynamicTest test = scenario.iterator().next();

            assertNotNull(test);
            assertTrue(test.getDisplayName().contains("Test Step"));
        }

        @Test
        @DisplayName("Should format test display name correctly")
        void shouldFormatTestDisplayNameCorrectly() {
            TestScenario scenario = new TestScenario();
            scenario.addStep("First Step", emptyFunction);
            scenario.addStep("Second Step", emptyFunction);
            var iterator = scenario.iterator();

            DynamicTest test1 = iterator.next();
            DynamicTest test2 = iterator.next();

            assertEquals("[1/2] First Step", test1.getDisplayName());
            assertEquals("[2/2] Second Step", test2.getDisplayName());
        }

        @Test
        @DisplayName("Should handle single step correctly")
        void shouldHandleSingleStepCorrectly() {
            TestScenario scenario = new TestScenario();
            scenario.addStep("Only Step", emptyFunction);

            DynamicTest test = scenario.iterator().next();

            assertEquals("[1/1] Only Step", test.getDisplayName());
        }
    }

    @Nested
    @DisplayName("Step Execution Tests")
    class StepExecutionTests {

        @Test
        @DisplayName("Should execute step runnable successfully")
        void shouldExecuteStepRunnableSuccessfully() {
            AtomicInteger counter = new AtomicInteger(0);
            TestScenario scenario = new TestScenario();
            scenario.addStep("Increment", counter::incrementAndGet);

            DynamicTest test = scenario.iterator().next();

            assertDoesNotThrow(test.getExecutable());
            assertEquals(1, counter.get());
        }

        @Test
        @DisplayName("Should execute multiple steps in sequence")
        void shouldExecuteMultipleStepsInSequence() {
            List<String> executionOrder = new ArrayList<>();
            TestScenario scenario = new TestScenario();
            scenario.addStep("Step 1", () -> executionOrder.add("Step 1"));
            scenario.addStep("Step 2", () -> executionOrder.add("Step 2"));
            var iterator = scenario.iterator();

            DynamicTest test1 = iterator.next();
            DynamicTest test2 = iterator.next();

            assertDoesNotThrow(test1.getExecutable());
            assertDoesNotThrow(test2.getExecutable());
            assertEquals(List.of("Step 1", "Step 2"), executionOrder);
        }

        @Test
        @DisplayName("Should execute empty function without errors")
        void shouldExecuteEmptyFunctionWithoutErrors() {
            TestScenario scenario = new TestScenario();
            scenario.addStep("Empty Function", emptyFunction);

            DynamicTest test = scenario.iterator().next();

            assertDoesNotThrow(test.getExecutable());
        }

        @Test
        @DisplayName("Should propagate exception from step runnable")
        void shouldPropagateExceptionFromStepRunnable() {
            RuntimeException expectedException = new RuntimeException("Test exception");
            TestScenario scenario = new TestScenario();
            scenario.addStep("Failing Step", () -> {
                throw expectedException;
            });

            DynamicTest test = scenario.iterator().next();

            RuntimeException actualException = assertThrows(RuntimeException.class, test.getExecutable());
            assertEquals(expectedException, actualException);
        }
    }

    @Nested
    @DisplayName("Failure Handling Tests")
    class FailureHandlingTests {

        @Test
        @DisplayName("Should skip subsequent steps after failure")
        void shouldSkipSubsequentStepsAfterFailure() {
            AtomicInteger counter = new AtomicInteger(0);
            TestScenario scenario = new TestScenario();
            scenario.addStep("Failing Step", () -> {
                counter.incrementAndGet();
                throw new RuntimeException("Failure");
            });
            scenario.addStep("Should be skipped", counter::incrementAndGet);
            var iterator = scenario.iterator();

            DynamicTest test1 = iterator.next();
            DynamicTest test2 = iterator.next();

            assertThrows(RuntimeException.class, test1.getExecutable());
            assertThrows(TestAbortedException.class, test2.getExecutable());
            assertEquals(1, counter.get()); // Only first step executed
        }

        @Test
        @DisplayName("Should include step name in skip message")
        void shouldIncludeStepNameInSkipMessage() {
            TestScenario scenario = new TestScenario();
            scenario.addStep("Failing Step", () -> {
                throw new RuntimeException("Failure");
            });
            scenario.addStep("Skipped Step", emptyFunction);
            var iterator = scenario.iterator();

            DynamicTest test1 = iterator.next();
            DynamicTest test2 = iterator.next();

            assertThrows(RuntimeException.class, test1.getExecutable());
            TestAbortedException exception = assertThrows(TestAbortedException.class, test2.getExecutable());
            assertTrue(exception.getMessage().contains("Skipped Step"));
        }

        @Test
        @DisplayName("Should handle multiple failures in sequence")
        void shouldHandleMultipleFailuresInSequence() {
            TestScenario scenario = new TestScenario();
            scenario.addStep("Failing Step", () -> {
                throw new RuntimeException("First failure");
            });
            scenario.addStep("Skipped Step 1", emptyFunction);
            scenario.addStep("Skipped Step 2", emptyFunction);
            var iterator = scenario.iterator();

            DynamicTest test1 = iterator.next();
            DynamicTest test2 = iterator.next();
            DynamicTest test3 = iterator.next();

            assertThrows(RuntimeException.class, test1.getExecutable());
            assertThrows(TestAbortedException.class, test2.getExecutable());
            assertThrows(TestAbortedException.class, test3.getExecutable());
        }

        @Test
        @DisplayName("Should continue normal execution after successful steps")
        void shouldContinueNormalExecutionAfterSuccessfulSteps() {
            AtomicInteger counter = new AtomicInteger(0);
            TestScenario scenario = new TestScenario();
            scenario.addStep("Success Step 1", counter::incrementAndGet);
            scenario.addStep("Success Step 2", counter::incrementAndGet);
            scenario.addStep("Success Step 3", counter::incrementAndGet);
            var iterator = scenario.iterator();

            DynamicTest test1 = iterator.next();
            DynamicTest test2 = iterator.next();
            DynamicTest test3 = iterator.next();

            assertDoesNotThrow(test1.getExecutable());
            assertDoesNotThrow(test2.getExecutable());
            assertDoesNotThrow(test3.getExecutable());
            assertEquals(3, counter.get());
        }
    }

    @Nested
    @DisplayName("AddStep Method Tests")
    class AddStepMethodTests {

        @Test
        @DisplayName("Should add multiple steps to scenario")
        void shouldAddMultipleStepsToScenario() {
            AtomicInteger counter = new AtomicInteger(0);
            TestScenario scenario = new TestScenario();

            scenario.addStep("Step 1", counter::incrementAndGet);
            scenario.addStep("Step 2", counter::incrementAndGet);
            var iterator = scenario.iterator();

            DynamicTest test1 = iterator.next();
            DynamicTest test2 = iterator.next();

            assertDoesNotThrow(test1.getExecutable());
            assertDoesNotThrow(test2.getExecutable());
            assertEquals(2, counter.get());
        }

        @Test
        @DisplayName("Should return same scenario instance for method chaining")
        void shouldReturnSameScenarioInstanceForMethodChaining() {
            TestScenario scenario = new TestScenario();

            TestScenario result = scenario.addStep("Step 1", emptyFunction);

            assertSame(scenario, result);
        }

        @Test
        @DisplayName("Should allow chaining multiple addStep calls")
        void shouldAllowChainingMultipleAddStepCalls() {
            AtomicInteger counter = new AtomicInteger(0);
            TestScenario scenario = new TestScenario()
                .addStep("Step 1", counter::incrementAndGet)
                .addStep("Step 2", counter::incrementAndGet)
                .addStep("Step 3", counter::incrementAndGet);

            // Execute all steps
            for (var test : scenario) {
                assertDoesNotThrow(test.getExecutable());
            }

            assertEquals(3, counter.get());
        }

        @Test
        @DisplayName("Should add step after iterator has started")
        void shouldAddStepAfterIteratorHasStarted() {
            AtomicInteger counter = new AtomicInteger(0);
            TestScenario scenario = new TestScenario();
            scenario.addStep("Initial Step", counter::incrementAndGet);
            var iterator = scenario.iterator();

            DynamicTest test1 = iterator.next();
            scenario.addStep("Added Step", counter::incrementAndGet);
            DynamicTest test2 = iterator.next();

            assertDoesNotThrow(test1.getExecutable());
            assertDoesNotThrow(test2.getExecutable());
            assertEquals(2, counter.get());
        }

        @Test
        @DisplayName("Should handle null name in addStep")
        void shouldHandleNullNameInAddStep() {
            TestScenario scenario = new TestScenario();

            assertThrows(NullPointerException.class, 
                () -> scenario.addStep(null, emptyFunction));
        }

        @Test
        @DisplayName("Should handle null runnable in addStep")
        void shouldHandleNullRunnableInAddStep() {
            TestScenario scenario = new TestScenario();

            assertThrows(NullPointerException.class, 
                () -> scenario.addStep("Test", null));
        }

        @Test
        @DisplayName("Should maintain correct order of added steps")
        void shouldMaintainCorrectOrderOfAddedSteps() {
            List<String> executionOrder = new ArrayList<>();
            TestScenario scenario = new TestScenario()
                .addStep("First", () -> executionOrder.add("First"))
                .addStep("Second", () -> executionOrder.add("Second"))
                .addStep("Third", () -> executionOrder.add("Third"));
            
            for (var test : scenario) {
                assertDoesNotThrow(test.getExecutable());
            }

            assertEquals(List.of("First", "Second", "Third"), executionOrder);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Integration Tests")
    class EdgeCasesAndIntegrationTests {

        @Test
        @DisplayName("Should handle empty step name")
        void shouldHandleEmptyStepName() {
            TestScenario scenario = new TestScenario();
            scenario.addStep("", emptyFunction);

            DynamicTest test = scenario.iterator().next();

            assertEquals("[1/1] ", test.getDisplayName());
        }

        @Test
        @DisplayName("Should handle step with special characters in name")
        void shouldHandleStepWithSpecialCharactersInName() {
            String specialName = "Test with émojis 🚀 and symbols @#$%";
            TestScenario scenario = new TestScenario();
            scenario.addStep(specialName, emptyFunction);

            DynamicTest test = scenario.iterator().next();

            assertTrue(test.getDisplayName().contains(specialName));
        }

        @Test
        @DisplayName("Should handle large number of steps")
        void shouldHandleLargeNumberOfSteps() {
            TestScenario scenario = new TestScenario();
            AtomicInteger counter = new AtomicInteger(0);

            // Add 100 steps
            for (int i = 0; i < 100; i++) {
                scenario.addStep("Step " + i, counter::incrementAndGet);
            }

            // Execute all steps
            int stepCount = 0;
            for (var test : scenario) {
                assertDoesNotThrow(test.getExecutable());
                stepCount++;
            }

            assertEquals(100, stepCount);
            assertEquals(100, counter.get());
        }

        @Test
        @DisplayName("Should maintain correct step numbering with dynamic additions")
        void shouldMaintainCorrectStepNumberingWithDynamicAdditions() {
            TestScenario scenario = new TestScenario();
            scenario.addStep("Step 1", emptyFunction);
            scenario.addStep("Step 2", emptyFunction);
            var iterator = scenario.iterator();

            assertEquals("[1/2] Step 1", iterator.next().getDisplayName());
            assertEquals("[2/2] Step 2", iterator.next().getDisplayName());
        }

        @Test
        @DisplayName("Should handle mixed successful and failing steps")
        void shouldHandleMixedSuccessfulAndFailingSteps() {
            AtomicInteger counter = new AtomicInteger(0);
            TestScenario scenario = new TestScenario()
                .addStep("Success 1", counter::incrementAndGet)
                .addStep("Success 2", counter::incrementAndGet)
                .addStep("Failure", () -> {
                    counter.incrementAndGet();
                    throw new RuntimeException("Test failure");
                })
                .addStep("Skipped", counter::incrementAndGet);
            var iterator = scenario.iterator();
            
            DynamicTest test1 = iterator.next();
            DynamicTest test2 = iterator.next();
            DynamicTest test3 = iterator.next();
            DynamicTest test4 = iterator.next();

            assertDoesNotThrow(test1.getExecutable());
            assertDoesNotThrow(test2.getExecutable());
            assertThrows(RuntimeException.class, test3.getExecutable());
            assertThrows(TestAbortedException.class, test4.getExecutable());
            assertEquals(3, counter.get()); // Only first 3 steps executed
        }

        @Test
        @DisplayName("Should handle very long step names")
        void shouldHandleVeryLongStepNames() {
            String longName = "This is a very long step name that contains many characters and should still work correctly ".repeat(5);
            TestScenario scenario = new TestScenario();
            scenario.addStep(longName, emptyFunction);

            DynamicTest test = scenario.iterator().next();

            assertTrue(test.getDisplayName().contains(longName));
            assertEquals("[1/1] " + longName, test.getDisplayName());
        }
    }

    @TestFactory
    @DisplayName("Dynamic Test Factory Example")
    Iterable<DynamicTest> dynamicTestFactoryExample() {
        AtomicInteger counter = new AtomicInteger(0);
        return new TestScenario()
            .addStep("Initialize", () -> counter.set(10))
            .addStep("Increment", counter::incrementAndGet)
            .addStep("Verify", () -> assertEquals(11, counter.get()));
    }

    @TestFactory
    @DisplayName("Parametrized Test Scenario Example")
    Iterable<DynamicContainer> dynamicNodeFactoryExample() {
        return new ParametrizedTestScenario<>(List.of(1, 2, 5), value -> {
            AtomicInteger counter = new AtomicInteger(0);
            return new TestScenario()
                .addStep("Initialize", () -> counter.set(10))
                .addStep("Increment", () -> counter.addAndGet(value))
                .addStep("Verify", () -> assertEquals(10 + value, counter.get()));
        });
    }

}