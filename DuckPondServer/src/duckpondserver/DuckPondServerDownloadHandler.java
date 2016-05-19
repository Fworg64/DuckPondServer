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
public class DuckPondServerDownloadHandler extends Thread
{
    private Socket socket = null;
    PrintWriter out;
    BufferedReader in;
    
    DuckPondServerDownloadHandler(Socket sock)
    {
        super("DuckPondServerUploadHandler");
        this.socket = sock;
    }
    
    @Override
    public void run()
    {
        System.out.println("Download session started");
        try 
        {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream()));
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        
        switch (getFolderRequest())
        {
            case -1: //could not read request
                System.err.println("Failed to read Request line from Client");
                return;
            case -2: //invalid request
                System.err.println("Read invalid Request line from Client");
                return;
            case 0: //user would like to cancel
                System.out.println("User leaving");
                return;
            case 1: //return random user folder
                out.println("RANDOM");
                break;
            case 2: //return popular user folders
                out.println("POPULARS");
                break;
            case 3: //reccomended requested
                out.println("RECOMMENEDED");
                break;
            case 4: //search requested
                out.println("SEARCH");
                break;   
        }
        System.out.println("Session terminated");
    }
    
    public int getFolderRequest()
    {
        String fromclient;
        try
        {
            fromclient = in.readLine();
        }
        catch (IOException e)
        {
            System.err.println("Could not read Request from client");
            return -1;
        }
        if (fromclient.equals("R")) {//random user
            System.out.println("random requested");
            return 1;
        } 
        else if (fromclient.equals("P")) {//popular
            System.out.println("popular requested");
            return 2;
        } 
        else if (fromclient.equals("C")) {//ReComended
            System.out.println("Choice requested");
            return 3;
        } 
        else if (fromclient.equals("S")) {//search for user
            System.out.println("Search requested");
            return 4;
        } 
        else if (fromclient.equals("Q")) { //gracefull quit requested
            return 0;
        }
        else {//invalid request
            System.out.println("Invalid request");
            return -2;
        }
    }
}
