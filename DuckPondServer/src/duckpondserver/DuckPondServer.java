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
    public static final int UPLOADPORTNUM = 42069;
    public static final int DOWNLDPORTNUM = 42068;

  public static void main(String[] args) throws IOException 
  {

    final int uploadPortNumber;
    final int downldPortNumber;
      
    if (args.length != 1) {
        uploadPortNumber = UPLOADPORTNUM;
    }
    else uploadPortNumber = Integer.parseInt(args[0]);
    
    if (args.length != 2) {
        downldPortNumber = DOWNLDPORTNUM;
    }
    else downldPortNumber = Integer.parseInt(args[1]);
    
    boolean listening = true;
    
    new Thread( new Runnable() { //create thread to listen for download connections
        @Override
        public void run() {
            try(ServerSocket downldSocket = new ServerSocket(downldPortNumber))
            {
                while (true)
                {
                    new DuckPondServerDownloadHandler(downldSocket.accept()).start();
                }
            }
            catch (IOException e) 
            {
                System.err.println("Could not listen on port " + uploadPortNumber);
                System.exit(-1);
            }
        }
        }).start();
        
    try (ServerSocket uploadSocket = new ServerSocket(uploadPortNumber))   
    { 
        while (listening) 
        {
        new DuckPondServerUploadHandler(uploadSocket.accept()).start();
	}
    }
    catch (IOException e) 
    {
        System.err.println("Could not listen on port " + uploadPortNumber);
        System.exit(-1);
    }
  }
    
}
