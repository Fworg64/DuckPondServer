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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
    List<String> levellines;
    Path userdir;
    Path pinfile;
    Path levelfile;
    
    boolean userverified;
    
    Calendar cal;
    DateFormat dateFormat;
    
    DuckPondServerUploadHandler(Socket sock)
    {
        super("DuckPondServerUploadHandler");
        this.socket = sock;
        userverified = false;
        levellines = new ArrayList<String>();
        
        cal = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
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
                    if (writeUserAndPin() ==0)
                    {
                        System.out.println("Pin recorded for " + user);
                        userverified = true;
                    }
                    else System.out.println("Error writing pinfile");
                }
                else System.err.println("pin not got!!");
                break;
            case 2: //name did exist
                userverified = false;
                while (!userverified)
                {
                    switch (checkPin())
                    {
                        case 1:
                            userverified = true;
                            System.out.println("Correct PIN entered for " + user);
                            out.println("ALLSGOOD");
                            break;
                        case -1:
                            System.err.println("CheckPin returned -1, terminating");
                            return;
                        case 0:
                            System.out.println("Incorrect PIN attempted");
                            out.println("TRYAGAIN");
                            break;
                    }
                } //repeat until pin is correct
                break;
            case -1: //could not verify existence or nonexistance
                System.err.println("Server error, could not verify files");
                break;
        }
        //user has been verified, wait for file
        if (userverified)
        {
            boolean stayconnected = true;
            while (stayconnected)
            {
                System.out.println("Waiting for file");
                switch (readFile()) //reads level from client, stores in level if success 
                {
                    case -3: //user quit
                        stayconnected = false;
                        break;
                    case -2: //invalid filename
                                System.err.println("Invalid filename, disconnecnting");
                                stayconnected = false;
                                break;
                    case -1: //error reading file
                        System.err.println("Error reading file, disconencting");
                        stayconnected = false;
                        break;
                    case 0: //file read correctly
                        switch (writeFile())
                        {
                            case -1: //error writing file
                                System.err.println("Error writing file, diconecting");
                                return;
                            case 0: //file write success!
                                System.out.println("File Wrote!");
                                break;
                        }
                }
            }
            System.out.println("Thanks for stopping by " + user);
        }
        else
        {
            out.println("GEERRRR");
            System.out.println("Unable to verify " + user);
        }
        System.out.println(dateFormat.format(cal.getTime()) +": Session Terminated for: " + user);
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
           System.out.println(dateFormat.format(cal.getTime()) + ": Welcome Back: " + user);
           return 2;
       }
       else if (Files.notExists(userdir))
       {
           out.println("NONEXIST");
           System.out.println(dateFormat.format(cal.getTime())+": New Player: " + user);
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
        if (pin.length() != 4)
        {
            out.println("INVALIDPIN");
            return -2;
        }
        out.println("RECIEVED");
        return 0;
    }
    
    private int writeUserAndPin()
    {
         //write to pin file
        pinfile = Paths.get(userdir.toString(),"ATTRIBUTES", "pin");
        try {
            Files.createDirectory(userdir);
            System.out.println("Dir created for: " + userdir.toString());
            Files.createDirectory(Paths.get(userdir.toString(), "ATTRIBUTES"));
            System.out.println("ATTRIBUTES dir created");
            System.out.println("attemting to create: " + pinfile.toString());
            Files.createFile(pinfile);
            System.out.println("attemting to write pin to " + pinfile.toString());
            Files.write(pinfile, pin.getBytes());
        } 
        catch (IOException ex) {
        System.err.println("Could not write pinfile");
        return -1;
        }
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
            return -2;
        }
        
        pinfile = Paths.get(userdir.toString(),"ATTRIBUTES", "pin");

        try //pin was correct
        {
            if (pin.equals(new String(Files.readAllBytes(pinfile)))) return 1;//pin was correct
            else return 0; //incorrect pin
        }
        catch (IOException e)
        {
            System.err.println("could not read pinfile @ " + pinfile.toString());
            return -1;
        }
    }
    
    private int readFile()
    {
        String temp;
        levellines.clear();
        try
        {
            temp = in.readLine(); //first filename
            if (temp.equals("\3")) return -3; //user wants to quit
            levelfile = Paths.get(userdir.toString(), temp);
            System.out.println("Read filename as " + temp);
            if (temp.equalsIgnoreCase("ATTRIBUTES")) {//invalid filename
                System.out.append("Invalid filename: ATTRIBUTES");
                return -2;
            } 
            temp = in.readLine();
            while (!temp.equals("\4")) //last char will be EOT
            {
                levellines.add(temp);
                System.out.println("read a line: " + temp);
                temp = in.readLine();
            }
        }
        catch (IOException e)
        {
            System.err.println("Error reading level from client");
            return -1;
        }
        System.out.println("file read " + levelfile.toString() + " for " + 
                            Integer.toString(levellines.size()) + " lines.");
        return 0;
    }
    
    public int writeFile()
    {
        try {
            Files.write(levelfile, levellines, Charset.forName("UTF-8"));
        } 
        catch (IOException ex) {
        System.err.println("Could not write levelfile");
        return -1;
        }
        System.out.println("Wrote file " + levelfile.toString() + 
                " with lines: " + Integer.toString(levellines.size()));
        return 0;
    }
    
}
