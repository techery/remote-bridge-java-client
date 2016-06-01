package io.techery.backdoor;

import io.techery.janet.AsyncClient;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class WebSocketConnection extends AsyncClient {


    public WebSocket websocket;

    AsyncHttpClient httpClient = new DefaultAsyncHttpClient();
    private WebSocketListener listener = new WebSocketListener() {


        @Override
        public void onOpen(WebSocket websocket) {
            WebSocketConnection.this.websocket = websocket;
        }

        @Override
        public void onClose(WebSocket websocket) {

        }

        @Override
        public void onError(Throwable t) {

        }
    };

    @Override
    protected boolean isConnected() {
        return this.websocket != null && this.websocket.isOpen();
    }

    @Override
    protected void connect(String url, boolean reconnectIfConnected) throws Throwable {
        WebSocketUpgradeHandler.Builder builder = new WebSocketUpgradeHandler.Builder()
                .addWebSocketListener(listener);
        httpClient.prepareGet(url).execute(builder.build()).get();
    }

    @Override
    protected void disconnect() throws Throwable {
        this.websocket.close();
    }

    @Override
    protected void send(String event, String payload) throws Throwable {
        this.websocket.sendMessage("");
    }

    @Override
    protected void send(String event, byte[] payload) throws Throwable {
        throw new NotImplementedException();
    }

    @Override
    protected void subscribe(String event) {
        throw new NotImplementedException();
    }
}
