package me.redpanda.djclientlib;

import com.mashape.unirest.http.Unirest;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

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
public class ClientBuilder {
    private String token;
    private boolean autoReconnect = true;

    public DiscordClient build() {
        try {
            String gatewayUrl = Unirest.get("https://discordapp.com/api/gateway").asJson().getBody().getObject().getString("url") + "/?v=6&encoding=json";
            System.out.println(gatewayUrl);
            WebSocket ws = new WebSocketFactory().createSocket(gatewayUrl, 5000);

            DiscordClientImpl clientImpl = new DiscordClientImpl();
            clientImpl.setToken(token);
            clientImpl.setWebSocket(ws);

            System.out.println("Connecting to WebSocket...");
            ws.addListener(new DiscordWSClient(clientImpl));
            ws.connect();
            return clientImpl;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public ClientBuilder setToken(String token) {
        this.token = token;
        return this;
    }
}