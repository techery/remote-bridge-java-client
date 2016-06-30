package io.techery.backdoor;

import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.techery.janet.ActionPipe;
import io.techery.janet.Janet;
import io.techery.janet.async.actions.ConnectAsyncAction;
import io.techery.janet.async.actions.DisconnectAsyncAction;
import io.techery.janet.helper.ActionStateToActionTransformer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class Backdoor {

    private final Janet janet;
    private final Executor executor;
    private final CompositeSubscription subscriptions;
    private final CompositeAction1<Throwable> failCallbacks;

    public Backdoor(String host, int port, String sessionId) {
        this(host, port, sessionId, Executors.newCachedThreadPool());
    }

    public Backdoor(String host, int port, String sessionId, Executor executor) {
        this.janet = new Janet.Builder()
                .addService(RemoteBridgeService.create(host, port, RemoteBridgeService.Consumer.DEVICE, sessionId))
                .build();
        this.executor = executor;
        this.subscriptions = new CompositeSubscription();
        this.failCallbacks = new CompositeAction1<Throwable>();
    }

    public void connect() {
        connect(new Subscriber<ConnectAsyncAction>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public void onNext(ConnectAsyncAction connectAsyncAction) {
            }
        });
    }

    public void connect(Subscriber<ConnectAsyncAction> subscriber) {
        janet.createPipe(ConnectAsyncAction.class)
                .createObservableResult(new ConnectAsyncAction(true))
                .subscribe(subscriber);
    }

    public void close() {
        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
            subscriptions.clear();
        }
        failCallbacks.clear();
        janet.createPipe(DisconnectAsyncAction.class)
                .send(new DisconnectAsyncAction());
    }

    public <A> void register(Class<A> actionClass, ActionResponder<A> responder) {
        ActionPipe<A> pipe = janet.createPipe(actionClass, Schedulers.from(executor));
        Subscription subscription = pipe
                .observe()
                .compose(new ActionStateToActionTransformer<A>())
                .subscribe(new ActionHandler<A>(pipe, responder), failCallbacks);
        subscriptions.add(subscription);
    }

    public void addOnFailCallback(Action1<Throwable> callback) {
        failCallbacks.add(callback);
    }

    private static class ActionHandler<A> implements Action1<A> {

        private final ActionPipe<A> pipe;
        private final ActionResponder<A> responder;

        private ActionHandler(ActionPipe<A> pipe, ActionResponder<A> responder) {
            this.pipe = pipe;
            this.responder = responder;
        }

        @Override
        public void call(A action) {
            action = responder.handle(action);
            pipe.send(action);
        }
    }

    private static class CompositeAction1<A> implements Action1<A> {

        private final HashSet<Action1<A>> callbacks;

        public CompositeAction1() {
            this.callbacks = new HashSet<Action1<A>>();
        }

        @Override
        public void call(A a) {
            for (Action1<A> callback : callbacks) {
                callback.call(a);
            }
        }

        public void add(Action1<A> callback) {
            callbacks.add(callback);
        }

        public void clear() {
            callbacks.clear();
        }
    }
}
