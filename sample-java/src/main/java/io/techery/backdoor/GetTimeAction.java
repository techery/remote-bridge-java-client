package io.techery.backdoor;

import java.util.Date;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Response;

@AsyncAction(value = "getTime")
public class GetTimeAction {

    @Response(timeout = 3000)
    ResponseData response;

    public ResponseData getResponse() {
        return response;
    }

    public static class ResponseData {

        private Date time;

        @Override
        public String toString() {
            return "ResponseData{" +
                    "time=" + time +
                    '}';
        }
    }

}
