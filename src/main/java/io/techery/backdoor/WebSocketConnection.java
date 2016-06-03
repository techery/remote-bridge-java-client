package io.techery.backdoor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.techery.janet.AsyncClient;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketTextListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import java.util.UUID;

public class WebSocketConnection extends AsyncClient {

    public WebSocket websocket;
    private final Gson gson = new Gson();

    AsyncHttpClient httpClient = new DefaultAsyncHttpClient();

    private WebSocketListener listener = new WebSocketTextListener() {

        @Override
        public void onMessage(String message) {
            JsonObject jsonObject = gson.fromJson(message, JsonObject.class);

            String action = jsonObject.get("action").getAsString();
            String actionId = jsonObject.get("actionId").getAsString();
            String payload = gson.toJson(jsonObject.get("response"));

            callback.onMessage(action, payload);
        }

        @Override
        public void onOpen(WebSocket websocket) {
            WebSocketConnection.this.websocket = websocket;
            callback.onConnect();
        }

        @Override
        public void onClose(WebSocket websocket) {
            callback.onDisconnect("Unknown");
        }

        @Override
        public void onError(Throwable t) {
            callback.onConnectionError(t);
        }
    };

    @Override
    protected boolean isConnected() {
        return this.websocket != null && this.websocket.isOpen();
    }

    @Override
    protected void connect(String url, boolean reconnectIfConnected) throws Throwable {
        if (isConnected()) {
            if (reconnectIfConnected) {
                this.websocket.close();
                this.websocket = null;
            } else {
                callback.onConnect();
            }
        } else {
            WebSocketUpgradeHandler.Builder builder = new WebSocketUpgradeHandler.Builder()
                    .addWebSocketListener(listener);

            WebSocketUpgradeHandler handler = builder.build();

            httpClient
                    .prepareGet(url)
                    .addHeader("Sec-WebSocket-Protocol", "remote-bridge-protocol")
                    .execute(handler)
                    .get();
        }
    }

    @Override
    protected void disconnect() throws Throwable {
        this.websocket.close();
        this.websocket = null;

        callback.onDisconnect("Disconnected");
    }

    private static class Message {
        String action;
        String actionId;
        JsonObject params;
    }

    @Override
    protected void send(String event, String payload) throws Throwable {

        final Message message = new Message();
        message.action = event;
        message.actionId = UUID.randomUUID().toString();
        message.params = gson.fromJson(payload, JsonObject.class);

        final String jsonMessage = gson.toJson(message);

        if (this.websocket.isOpen()) {
            this.websocket.sendMessage(jsonMessage);
        } else {
            throw new IllegalStateException("Socket is not connected");
        }
    }

    @Override
    protected void send(String event, byte[] payload) throws Throwable {

    }

    @Override
    protected void subscribe(String event) {

    }
}
