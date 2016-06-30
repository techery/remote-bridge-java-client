package io.techery.backdoor;

import java.util.HashMap;

import io.techery.janet.ActionPipe;
import io.techery.janet.ActionState;
import io.techery.janet.Janet;
import rx.observables.BlockingObservable;

public class Backdoor {

    public static void main(String[] args) {
        new Backdoor();
    }

    public Backdoor() {
        RemoteBridgeService service = RemoteBridgeService.create("0.0.0.0", 8080, RemoteBridgeService.Consumer.CLIENT, "123");

        Janet janet = new Janet.Builder().addService(service).build();

        ActionPipe<GetTimeAction> pipe = janet.createPipe(GetTimeAction.class);

        BlockingObservable<ActionState<GetTimeAction>> actionStateBlockingObservable =
                pipe.createObservable(new GetTimeAction())
                        .last().toBlocking();

        System.out.println(actionStateBlockingObservable.last().action.getResponse());
    }
}
