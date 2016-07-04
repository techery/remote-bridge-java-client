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
import io.techery.janet.async.model.Message;
import io.techery.janet.async.model.ProtocolAction;
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
                .setMessageRule(new BridgeMessageRule(gson))
                .setResponseMatcher(new BridgeResponseMatcher())
                .build();
        Converter converter = new GsonConverter(gson);
        AsyncActionService service = new AsyncActionService(url, client, protocol, converter);
        return new RemoteBridgeService(service);
    }

    private static class BridgeMessageRule implements MessageRule {

        private final Gson gson;
        private final JsonParser jsonParser;

        private BridgeMessageRule(Gson gson) {
            this.gson = gson;
            this.jsonParser = new JsonParser();
        }

        @Override
        public ProtocolAction handleMessage(Message message) throws Throwable {
            JsonObject json = gson.fromJson(message.getDataAsText(), JsonObject.class);
            return ProtocolAction.of(message)
                    .payload(json.get(KEY_DATA).toString())
                    .metadata(KEY_ID, json.get(KEY_ID).toString());
        }

        @Override
        public Message createMessage(ProtocolAction protocolAction) throws Throwable {
            String messageId = UUID.randomUUID().toString();
            JsonObject json = new JsonObject();
            json.addProperty(KEY_ID, messageId);
            json.add(KEY_DATA, jsonParser.parse(protocolAction.getPayloadAsString()));
            //save message id for response matching
            protocolAction.metadata(KEY_ID, messageId);
            return Message.createTextMessage(protocolAction.getEvent(), json.toString());
        }
    }

    private static class BridgeResponseMatcher implements ResponseMatcher {

        @Override
        public boolean match(ProtocolAction waitingAction, ProtocolAction incomingAction) {
            return waitingAction.getEvent().equals(incomingAction.getEvent())
                    && waitingAction.getMetadata(KEY_ID).equals(incomingAction.getMetadata(KEY_ID));
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
