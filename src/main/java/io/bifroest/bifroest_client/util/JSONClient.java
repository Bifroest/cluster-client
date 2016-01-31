package com.goodgame.profiling.bifroest.bifroest_client.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import org.json.JSONObject;

public class JSONClient {

    private JSONClient() {
        // Utility class
    }

    public static JSONObject request( String host, int port, JSONObject content ) {
        try ( Socket clientSocket = new Socket( host, port ) ) {
            DataOutputStream out = new DataOutputStream( clientSocket.getOutputStream() );
            BufferedReader in = new BufferedReader( new InputStreamReader( clientSocket.getInputStream() ) );
            out.writeBytes( content.toString() );
            String line;
            StringBuilder result = new StringBuilder();
            while ( ( line = in.readLine() ) != null ) {
                result.append( line );
            }
            return new JSONObject( result.toString() );
        } catch ( IOException e ) {
            throw new RuntimeException( "Could not connect to " + host, e );
        }
    }

}
