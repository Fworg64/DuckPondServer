/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package duckpondserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author fworg
 */
public class DuckPondServerUploadHandler extends Thread{
    private Socket socket = null;
    DuckPondServerUploadHandler(Socket sock)
    {
        super("DuckPondServerUploadHandler");
        this.socket = sock;
    }
    
    public void run()
    {
        try (
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream()));
        ) {
            String inputLine, outputLine;
            //create object to deal with stuff
            outputLine = "herp.";
            out.println(outputLine);

            while ((inputLine = in.readLine()) != null) {
                //do stuff here too
                outputLine = inputLine;
                out.println(outputLine);
                if (outputLine.equals("Bye"))
                    break;
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
