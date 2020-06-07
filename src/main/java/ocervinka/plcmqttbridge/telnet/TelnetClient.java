package ocervinka.plcmqttbridge.telnet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;


public class TelnetClient {

    private static final Logger LOGGER = LogManager.getLogger();
    
    private Socket socket;
    private PrintWriter writer;
    private volatile boolean stopRequest = false;
    
    private final Timer timer;


    public TelnetClient(final String host, final int port, final TelnetClientListener listener) {
        TimerTask timerTask = new TimerTask() {

            @Override
            public void run() {
                BufferedReader reader = null;
                
                while (!stopRequest) {
                    
                    if (socket == null) {
                        try {
                            socket = new Socket(host, port);
                            socket.setKeepAlive(true);
                            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "ASCII"), true);
                        } catch (IOException ex) {
                            listener.onError(TelnetClient.this, "can not open and initialize socket", ex);
                            socket = null;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored) {}
                            continue;
                        }
                        listener.onConnect(TelnetClient.this, socket.toString());
                    }
                    
                    String line;
                    try {
                        line = reader.readLine();
                        if (line == null) {
                            throw new IOException("end of stream reached");
                        }
                    } catch (IOException e) {
                        if (stopRequest) {
                            break;
                        }
                        listener.onError(TelnetClient.this, "error when reading line", e);
                        try {
                            socket.close();
                        } catch (IOException e2) {
                            LOGGER.error("error when closing socket: ", e2);
                        }
                        socket = null;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {}
                        continue;
                    }
                    
                    try {
                        listener.onLineRead(TelnetClient.this, line);
                    } catch(Exception ex) {
                        listener.onError(TelnetClient.this, "error when calling listener", ex);
                    }
                } // while
                
                listener.onDisconnect(TelnetClient.this);
                
            } // run method
            
        }; // TimerTask class
        
        timer = new Timer("TelnetClient");
        timer.schedule(timerTask, 0);   //run now and only once
    }
    
    
    public void close() {

        LOGGER.info("closing sockets...");
        stopRequest = true;

        if (socket != null) {
            try {
                socket.close();
            } catch(IOException e) {
                LOGGER.error("error when closing socket ", e);
            }
        }
        
        // this might not be needed because the task should end naturally
        LOGGER.info("canceling socket timer...");
        timer.cancel();
    }
    

    public void write(String line) {
        if (socket != null) {
            synchronized (this) {
                writer.println(line);
            }
        }
    }
    
}
