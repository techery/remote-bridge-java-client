package io.techery.backdoor;

import java.util.Date;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.annotations.Response;

@AsyncAction(value = "getTime", incoming = true)
public class GetTimeAction {

    @Payload
    Data payload;

    @Response(timeout = 30000)
    Data response;

    public static class Data {

        private Date time;

        public Data(Date time) {
            this.time = time;
        }

        @Override
        public String toString() {
            return "ResponseData{" +
                    "time=" + time +
                    '}';
        }
    }

}
