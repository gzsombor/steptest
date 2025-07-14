package com.github.gzsombor.steptests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;

public class ParametrizedTestScenario<P> implements Iterable<DynamicContainer>{
    private final List<P> parameters = new ArrayList<>();
    private final Function<P, Iterable<DynamicTest>> testFactory;

    public ParametrizedTestScenario(Function<P, Iterable<DynamicTest>> testFactory) {
        this.testFactory = testFactory;
    }

    public ParametrizedTestScenario(Collection<P> parameters, Function<P, Iterable<DynamicTest>> testFactory) {
        this.parameters.addAll(parameters);
        this.testFactory = testFactory;
    }

    public ParametrizedTestScenario(Function<P, Iterable<DynamicTest>> testFactory, P... parameters) {
        this.parameters.addAll(Arrays.asList(parameters));
        this.testFactory = testFactory;
    }

    Stream<DynamicContainer> build() {
        return parameters.stream().map(parameter -> {
            var testSteps = testFactory.apply(parameter);
            return DynamicContainer.dynamicContainer(
                displayName(parameter),
                testSteps
            );
        });
    }

    public ParametrizedTestScenario<P> addParameter(P parameter) {
        parameters.add(parameter);
        return this;
    }

    protected String displayName(P parameter) {
        return "Case: " + parameter.toString();
    }

    @Override
    public Iterator<DynamicContainer> iterator() {
        return build().iterator();
    }

}
