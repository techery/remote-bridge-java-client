package io.techery.backdoor;

import java.util.Date;

import io.techery.janet.Janet;
import rx.schedulers.Schedulers;

public class Sample {

    private static final String HOST = "0.0.0.0";
    private static final int PORT = 8080;
    private static final String SESSION_ID = "123";

    public static void main(String[] args) {
        DeviceSample deviceSample = new DeviceSample();
        deviceSample.connect();
        new ClientSample().run();
        deviceSample.disconnect();
    }

    private static class DeviceSample {
        private final Backdoor backdoor;

        public DeviceSample() {
            backdoor = new Backdoor(HOST, PORT, SESSION_ID);
            backdoor.register(GetTimeAction.class, new ActionResponder<GetTimeAction>() {
                @Override
                public GetTimeAction handle(GetTimeAction action) {
                    action.payload = new GetTimeAction.Data(new Date());
                    return action;
                }
            });
        }

        public void connect() {
            backdoor.connect();
        }

        public void disconnect() {
            backdoor.close();
        }
    }

    private static class ClientSample implements Runnable {

        private final Janet janet;

        public ClientSample() {
            RemoteBridgeService service = RemoteBridgeService.create("0.0.0.0", 8080, RemoteBridgeService.Consumer.CLIENT, "123");
            janet = new Janet.Builder().addService(service).build();
        }

        @Override
        public void run() {
            GetTimeAction action = janet.createPipe(GetTimeAction.class, Schedulers.io())
                    .createObservableResult(new GetTimeAction())
                    .toBlocking()
                    .last();
            System.out.println(action.response);
        }
    }
}
