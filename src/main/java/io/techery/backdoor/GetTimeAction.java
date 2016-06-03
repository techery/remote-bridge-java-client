package io.techery.backdoor;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.annotations.PendingResponse;

@AsyncAction("getTime")
public class GetTimeAction extends RPCAction<GetTimeAction.Params, String> {
    
    public GetTimeAction(Params payload) {
        super(payload);
    }

    static class Params {
        final String name;

        Params(String name) {
            this.name = name;
        }
    }


}
