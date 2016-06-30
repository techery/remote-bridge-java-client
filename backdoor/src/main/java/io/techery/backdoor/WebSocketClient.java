package io.techery.backdoor;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketTextListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import io.techery.janet.AsyncClient;
import io.techery.janet.async.model.Message;

class WebSocketClient extends AsyncClient {

    public WebSocket websocket;
    private final MessageParser messageParser;
    private final AsyncHttpClient httpClient = new DefaultAsyncHttpClient();

    private WebSocketListener listener = new WebSocketTextListener() {

        @Override
        public void onMessage(String message) {
            callback.onMessage(messageParser.toMessage(message));
        }

        @Override
        public void onOpen(WebSocket websocket) {
            WebSocketClient.this.websocket = websocket;
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

    public WebSocketClient(MessageParser messageParser) {
        this.messageParser = messageParser;
    }

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

    @Override
    protected void send(Message message) throws Throwable {
        if (this.websocket.isOpen()) {
            this.websocket.sendMessage(messageParser.toSource(message));
        } else {
            throw new IllegalStateException("Socket is not connected");
        }
    }

    @Override
    protected void subscribe(String event) {
        //do nothing
    }

    interface MessageParser {

        Message toMessage(String source);

        String toSource(Message message);
    }
}
