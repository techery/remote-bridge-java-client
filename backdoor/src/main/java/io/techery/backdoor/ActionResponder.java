package io.techery.backdoor;

public interface ActionResponder<T> {

    T handle(T action);
}
