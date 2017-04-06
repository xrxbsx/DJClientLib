package me.redpanda.djclientlib;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFrame;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;

/**
 * <p>
 * MIT License
 * <p>
 * Copyright (c) 2017 Blake R.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class DiscordWSClient extends WebSocketAdapter {
    private DiscordClientImpl client;
    private long seq;
    private long lastHeartbeatTime;
    private JSONEventHandler eventHandler = new JSONEventHandler();

    DiscordWSClient(DiscordClientImpl client) {
        this.client = client;
    }

    private void identify() {
        System.out.println("Sending identify...");

        JSONObject payload = new JSONObject();

        JSONObject data = new JSONObject();
        data.put("token", client.getToken());
        data.put("compress", true);
        data.put("large_threshold", 250);

        JSONObject prop = new JSONObject();
        prop.put("$os", System.getProperty("os.name"));
        prop.put("$browser", "X");
        prop.put("$device", "X");
        prop.put("$referring_domain", "");
        prop.put("$referrer", "");

        data.put("properties", prop);
        data.put("v", 6);

        payload.put("d", data);
        payload.put("op", 2);

        client.getWebSocket().sendText(payload.toString(4));
        System.out.println("\nSent identify!");
        System.out.println(data.toString(4) + "\n");
    }

    @Override
    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        System.out.println("*********** GATEWAY CLOSED ***********");
        System.out.println("Reason: " + frame.getCloseReason());
        System.out.println("Code: " + frame.getCloseCode());
        System.out.println("**************************************");
        System.exit(0);
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        System.out.println("WebSocket connected!");
//        identify();
    }

    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
        onTextMessage(websocket, decompress(binary));
    }

    private String decompress(byte[] data) {
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(data);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            byte[] output = outputStream.toByteArray();
            return new String(output, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onTextMessage(WebSocket websocket, String text) {
        JSONObject content = new JSONObject(text);
        int op = content.getInt("op");

        switch (op) {
            case 0:
                //Received event
                try {
                    System.out.println("Received event \'" + content.getString("t") + "\'");
                    eventHandler.handle(content);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                // Received a heartbeat request
                sendHeartbeat();
                break;
            case 7:
                // Received a reconnect request
                break;
            case 9:
                // Received an invalidate request
                break;
            case 10:
                // Received HELLO packet
                onHelloReceived(content);
                break;
            case 11:
                // Received heartbeat ACK
                client.setWebSocketPing(System.currentTimeMillis() - lastHeartbeatTime);
                System.out.println("Received heartbeat ACK");
                break;
            default:
                System.out.println("Received an unknown opcode(" + op + ")!\n" + content.toString(4));
        }
    }

    private void onHelloReceived(JSONObject object) {
        System.out.println("HELLO received!");

        identify();

        long heartbeatInterval = object.getJSONObject("d").getLong("heartbeat_interval");
        startHeartbeatLoop(heartbeatInterval);
    }

    private void startHeartbeatLoop(long interval) {
        System.out.println("Starting heartbeat loop with interval of " + interval + "ms...");
        client.getExecutor().scheduleAtFixedRate(this::sendHeartbeat, 1000, interval, TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeat() {
        System.out.println("Sent heartbeat...");
        client.getWebSocket().sendText("{\"op\":1, \"d\":" + seq + "}");
        lastHeartbeatTime = System.currentTimeMillis();
    }
}