package io.techery.backdoor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.UUID;

import io.techery.janet.ActionHolder;
import io.techery.janet.ActionServiceWrapper;
import io.techery.janet.AsyncActionService;
import io.techery.janet.AsyncClient;
import io.techery.janet.JanetException;
import io.techery.janet.async.model.IncomingMessage;
import io.techery.janet.async.model.Message;
import io.techery.janet.async.model.WaitingAction;
import io.techery.janet.async.protocol.AsyncProtocol;
import io.techery.janet.async.protocol.MessageRule;
import io.techery.janet.async.protocol.ResponseMatcher;
import io.techery.janet.converter.Converter;
import io.techery.janet.gson.GsonConverter;

public class RemoteBridgeService extends ActionServiceWrapper {

    private static final String KEY_ID = "id";
    private static final String KEY_ACTION = "action";
    private static final String KEY_DATA = "data";


    private RemoteBridgeService(AsyncActionService actionService) {
        super(actionService);
    }

    public static RemoteBridgeService create(String host, int port, Consumer consumer, String sessionId) {
        String url = new StringBuilder()
                .append("ws://")
                .append(host)
                .append(":")
                .append(port)
                .append("/")
                .append(consumer.method)
                .append("?id=")
                .append(sessionId)
                .toString();
        Gson gson = new Gson();
        AsyncClient client = new WebSocketClient(new BridgeMessageParser(gson));
        AsyncProtocol protocol = new AsyncProtocol.Builder()
                .setTextMessageRule(new BridgeMessageRule(gson))
                .setResponseMatcher(new BridgeResponseMatcher(gson))
                .build();
        Converter converter = new GsonConverter(gson);
        AsyncActionService asyncService = new AsyncActionService(url, client, protocol, converter);
        return new RemoteBridgeService(asyncService);
    }

    private static class BridgeMessageRule implements MessageRule<String> {

        private final Gson gson;
        private final JsonParser jsonParser;

        private BridgeMessageRule(Gson gson) {
            this.gson = gson;
            this.jsonParser = new JsonParser();
        }

        @Override
        public String handleMessage(Message message) throws Throwable {
            return gson.fromJson(message.getDataAsText(), JsonObject.class)
                    .get(KEY_DATA)
                    .toString();
        }

        @Override
        public Message createMessage(String event, String payload) throws Throwable {
            JsonObject json = new JsonObject();
            json.addProperty(KEY_ID, UUID.randomUUID().toString());
            json.add(KEY_DATA, jsonParser.parse(payload));
            return Message.createTextMessage(event, json.toString());
        }
    }

    private static class BridgeResponseMatcher implements ResponseMatcher {

        private final Gson gson;

        private BridgeResponseMatcher(Gson gson) {
            this.gson = gson;
        }

        @Override
        public boolean match(WaitingAction waitingAction, IncomingMessage incomingMessage) {
            String id1 = gson.fromJson(waitingAction.getMessage().getDataAsText(), JsonObject.class)
                    .get(KEY_ID).getAsString();
            String id2 = gson.fromJson(incomingMessage.getMessage().getDataAsText(), JsonObject.class)
                    .get(KEY_ID).getAsString();
            return id1.equals(id2);
        }
    }

    private static class BridgeMessageParser implements WebSocketClient.MessageParser {

        private final Gson gson;

        private BridgeMessageParser(Gson gson) {
            this.gson = gson;
        }

        @Override
        public Message toMessage(String source) {
            String event = gson.fromJson(source, JsonObject.class)
                    .get(KEY_ACTION)
                    .getAsString();
            return Message.createTextMessage(event, source);
        }

        @Override
        public String toSource(Message message) {
            JsonObject json = gson.fromJson(message.getDataAsText(), JsonObject.class);
            json.addProperty(KEY_ACTION, message.getEvent());
            return json.toString();
        }

    }

    public enum Consumer {
        DEVICE("create"), CLIENT("connect");

        private final String method;

        Consumer(String method) {
            this.method = method;
        }
    }

    @Override
    protected <A> boolean onInterceptSend(ActionHolder<A> holder) throws JanetException {
        return false;
    }

    @Override
    protected <A> void onInterceptCancel(ActionHolder<A> holder) {
    }

    @Override
    protected <A> void onInterceptStart(ActionHolder<A> holder) {
    }

    @Override
    protected <A> void onInterceptProgress(ActionHolder<A> holder, int progress) {
    }

    @Override
    protected <A> void onInterceptSuccess(ActionHolder<A> holder) {
    }

    @Override
    protected <A> boolean onInterceptFail(ActionHolder<A> holder, JanetException e) {
        return false;
    }
}
