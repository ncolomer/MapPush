package org.mappush.resource;

import java.io.IOException;
import java.net.ServerSocket;

public class TestUtils {

    public static int findFreePort() throws IOException {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
    
    public static String buildHttpUrl(String host, int port, String path) {
    	return String.format("http://%s:%d/%s", host, port, path);
    }
    
    public static String buildWsUrl(String host, int port, String path) {
    	return String.format("ws://%s:%d/%s", host, port, path);
    }

}
