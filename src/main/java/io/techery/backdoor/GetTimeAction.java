package io.techery.backdoor;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;

@AsyncAction("get_time")
public class GetTimeAction {

    @Payload
    String payload;


}
