package io.techery.backdoor;


import io.techery.janet.async.PendingResponseMatcher;
import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.annotations.PendingResponse;

public class RPCAction<T, RT> {

    @AsyncAction(value = "response", incoming = true)
    public static class RPCResponseAction<RT> {

        @Payload
        RPCPayload<RT> payload;
    }

    @PendingResponse(value = ResponseMatcher.class, timeout = 3000)
    RPCResponseAction<RT> responseAction;

    public static class ResponseMatcher implements PendingResponseMatcher<RPCAction, RPCResponseAction> {

        @Override
        public boolean match(RPCAction requestAction, RPCResponseAction response) {
            // Condition to link request with response
            return requestAction.payload.id.equalsIgnoreCase(response.payload.id);
        }
    }

    public static class RPCPayload<P> {
        P value;
        String id;

        public RPCPayload(P value) {
            this.value = value;
        }
    }

    @Payload
    RPCPayload<T> payload;

    public RPCAction(T payload) {
        this.payload = new RPCPayload<T>(payload);
    }

    public T getPayload() {
        return payload.value;
    }

    public RT getResponsePayload() {
        return responseAction.payload.value;
    }
}
