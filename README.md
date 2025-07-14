# StepTest

 In the JUnit framework, it is difficult to write modular tests from scratch, i.e. where you can easily build longer processes from self-defined, independent steps, and thus validate more complex business processes. For example, when you have a process with N steps and you need to check that if step 2 is skipped, step 3....5 will still run exactly the same, but step 6 will have a different outcome.

  Of course, it is possible to define tests where the functions corresponding to step N are called only one after the other, but then if an error occurs, it is much more difficult to see where the error came from - especially if the same step is repeatedly used in the same scenario with slightly different parameterization. It's a much better developer experience if you can immediately see that the program is stuck at step N of scenario X.

 This is what this minimal library - consisting of 2 public classes - is trying to facilitate.

A JUnit 5 utility class for creating sequential test scenarios where each step depends on the success of the previous steps. If any step fails, all subsequent steps are automatically skipped using JUnit's `Assumptions` mechanism.

## Features

- **Sequential Execution**: Steps are executed in the order they are added
- **Automatic Skipping**: If a step fails, all subsequent steps are skipped
- **Fluent API**: Method chaining support for easy test scenario building
- **Clear Naming**: Each step shows its position in the sequence (e.g., "[2/5] Step Name")

## Basic Usage

### Simple Sequential Test

```java
@TestFactory
Iterator<DynamicTest> internalFunctionalityCanBeAccessedAfterEmailConfirmation() {
    return new TestScenario()
        .addStep("User creates an account", () -> {
            accountService.create(...);
        })
        .addStep("Email address confirmed ", () -> {
            accountService.registrationConfirmed(...);
        })
        .addStep("User can access internal functionality", () -> {
            internalService.doSomeBusinessStuff(...);
        })
        .addStep("Everything as expected", this::checkEverything);
}

@TestFactory
Iterator<DynamicTest> internalFunctionalityDeniedBeforeEmailConfirmation() {
    return new TestScenario()
        .addStep("User creates an account", () -> {
            accountService.create(...);
        })
        .addStep("User can't access internal functionality", () -> {
            assertThrows(NotConfirmedException.class, () -> internalService.doSomeBusinessStuff(...));
        })
        .addStep("Everything as expected", this::checkNothingChanged);
}

```

## Advanced Usage

### Dynamic Step Addition

```java
@TestFactory
Iterator<DynamicTest> dynamicStepScenario() {
    var scenario = new TestScenario();
    var values = Arrays.asList(2, 3, 5);

    // Setup code
    scenario.addStep("Setup test environment", this::initializeTestEnvironment);

    // Dynamically add steps based on test cases
    values.forEach(value -> {
        scenario.addStep("Execute step " + value, () -> {
            executeStepWithValue(value);
        });
    });

    scenario.addStep("Cleanup", this::cleanupTestEnvironment);
    return scenario;
}
```

### Parametric Test Executions

Unfortunately JUnit's @ParametrizedTest annotation does not support @TestFactory annotation or handling methods that returns any values, so we can't just return a well configured TestScenario based on function parameters. That's why we have a ParametrizedTestScenario class to have a simple
solution to implement simplified but similar logic.


```java
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


```



## Best Practices

1. **Keep Steps Focused**: Each step should test one specific aspect of the workflow
2. **Use Descriptive Names**: Step names should clearly describe what is being tested.
3. **Make Steps Independent**: Each step should not depend on mutable state from previous steps beyond what's being tested
4. **Use Meaningful Assertions**: Include clear assertion messages for better failure diagnosis

## Requirements

- JUnit 5 (Jupiter)
- Java 17 or higher

