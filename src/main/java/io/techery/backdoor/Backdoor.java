package io.techery.backdoor;

import com.google.gson.Gson;
import io.techery.janet.*;
import io.techery.janet.gson.GsonConverter;
import rx.observables.BlockingObservable;

public class Backdoor {

    public static void main(String [ ] args)
    {
        Backdoor backdoor = new Backdoor();


    }

    public Backdoor() {
        String url = "ws://appium-rbridge.techery.io/join?id=123";

        GsonConverter converter = new GsonConverter(new Gson());
        WebSocketConnection client = new WebSocketConnection();
        ActionService asyncService = new AsyncActionService(url, client, converter);

        Janet janet = new Janet.Builder().addService(asyncService).build();

        ActionPipe<GetTimeAction> pipe = janet.createPipe(GetTimeAction.class);

        pipe.send(new GetTimeAction());

        BlockingObservable<ActionState<GetTimeAction>> actionStateBlockingObservable = pipe.observe().last().toBlocking();

        System.out.print(actionStateBlockingObservable.last().action.payload);
    }
}
