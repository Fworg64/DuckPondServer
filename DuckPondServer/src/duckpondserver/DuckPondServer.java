/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package duckpondserver;

import java.io.IOException;
import java.net.ServerSocket;

/**
 *
 * @author fworg
 */

public class DuckPondServer {
    public static final int PORTNUM = 42069;

  public static void main(String[] args) throws IOException 
  {

    int portNumber;
      
    if (args.length != 1) {
        portNumber = PORTNUM;
    }
    else portNumber = Integer.parseInt(args[0]);
    
    boolean listening = true;
        
    try (ServerSocket serverSocket = new ServerSocket(portNumber)) 
    { 
        while (listening) 
        {
        new DuckPondServerUploadHandler(serverSocket.accept()).start();
	}
    }
    catch (IOException e) 
    {
        System.err.println("Could not listen on port " + portNumber);
        System.exit(-1);
        
    }
  }
    
}
