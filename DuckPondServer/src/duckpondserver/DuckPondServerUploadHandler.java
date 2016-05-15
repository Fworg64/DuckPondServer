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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fworg
 */
public class DuckPondServerUploadHandler extends Thread{
    
    private Socket socket = null;
    PrintWriter out;
    BufferedReader in;
    
    String user;
    String pin;
    String level;
    Path userdir;
    Path pinfile;
    Path levelfile;
    
    
    DuckPondServerUploadHandler(Socket sock)
    {
        super("DuckPondServerUploadHandler");
        this.socket = sock;
    }
    
    @Override
    public void run()
    {
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
        
        switch (readUserName())
        {
            case 1: //name did not exist
                if (getNewPin() ==0) //get a pin first, in case they disconnect
                {
                    try
                    {
                        Files.createDirectory(userdir);
                    }
                    catch(IOException e)
                    {
                        System.err.println("could not create user dir");
                        return;
                    }
                }
                else System.err.println("pin not got!!");
                break;
            case 2: //name did exist
                boolean good = false;
                while (!good)
                {
                    switch (checkPin())
                    {
                        case 1:
                            good = true;
                            System.out.println("Correct PIN entered for " + user);
                            break;
                        case -1:
                            System.err.println("CheckPin returned -1, terminating");
                            return;
                        case 0:
                            System.out.println("Incorrect PIN attempted");
                            break;
                    }
                } //repeat until pin is correct
                break;
            case -1: //could not verify existence or nonexistance
                System.err.println("Server error, could not verify files");
        }
        //user has been verified, wait for file
        out.println("hergity");
        System.out.println("Thanks for stopping by " + user);
    }
    
    private int readUserName()
    {
        try
        {
            user = in.readLine();
        }
        catch (IOException e)
        {
            System.err.println("Could not read Name from client");
        }
        
        if (user.length() >= 3) //valid
        {
            userdir = Paths.get("./SERVERFILES", user);
        }
       //test if exist
       if (Files.exists(userdir))
       {
           out.println("EXIST");
           System.out.println("Welcome Back: " + user);
           return 2;
       }
       else if (Files.notExists(userdir))
       {
           out.println("NONEXIST");
           System.out.println("New Player: " + user);
           return 1;
       }
       else return -1; 
    }
    
    private int getNewPin()
    {
        try
        {
            pin = in.readLine();
        }
        catch (IOException e)
        {
            System.err.println("Could not read PIN from client");
            return -1;
        }
        
        //write to pin file
        pinfile = Paths.get(userdir.toString(),pin);
        try {
            Files.write(pinfile, new byte[0],StandardOpenOption.WRITE);
        } catch (IOException ex) {
        System.err.println("Could not write pinfile");
        return -1;
        }
        
        out.println("RECIEVED");
        System.out.println("Pin recorded for " + user);
        return 0;
    }
    
    public int checkPin()
    {
        try
        {
            pin = in.readLine();
        }
        catch (IOException e)
        {
            System.err.println("Could not read PIN from client");
            return -1;
        }
        
        pinfile = Paths.get(userdir.toString(),pin);

        if (Files.exists(pinfile)) //pin was correct
        {
            return 1;
        }
        else return 0;
    }
    
}
