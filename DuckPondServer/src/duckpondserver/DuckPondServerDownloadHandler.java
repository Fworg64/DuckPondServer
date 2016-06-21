/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package duckpondserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fworg
 */
public class DuckPondServerDownloadHandler extends Thread
{
    private Socket socket = null;
    PrintWriter out;
    BufferedReader in;
    final static Path leveldir = Paths.get("./SERVERFILES");
    List<Path> gottenPaths;
    List<Path> levels;
    
    private boolean getType;
    private boolean getUser;
    private boolean getLevel;
    private boolean gotLevel;
    
    DuckPondServerDownloadHandler(Socket sock)
    {
        super("DuckPondServerUploadHandler");
        this.socket = sock;
        gottenPaths = new ArrayList<Path>();
        
        getType = false;
        getUser = false;
        getLevel = false;
        gotLevel = false;
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
        
        getType = true;
        while (true)
        {
            if (getType)
            {
                sendCatagoryOptions();
                String choice = receiveLine();
                if (choice.equals("Random"))
                {
                    System.out.println("Randoms Requested");
                    gottenPaths = getRandoms();
                    System.out.println("Randoms got");
                    getType = false;
                    getUser = true;
                }
                else if (choice.equals("\4")) //user wants to quit
                {
                    System.out.println("User Left");
                    return;
                }
                else //invalid
                {
                    System.err.println("Read invalid Request line from Client");
                    return;
                }
            }
            if (getUser)
            {
                sendPathOptions(gottenPaths); //send user folder names
                System.out.println("Gotten Paths Sent, waiting for selection");
                String choice = receiveLine(); //get choice, blocking
                if (!choice.equals("\5") && !choice.equals("\4")) //go forward
                {
                    Path chosenPath;
                    chosenPath = null; //to appease compiler
                    boolean validchoice = false;
                    for (Path p: gottenPaths) if (p.getFileName().toString().equals(choice)) {chosenPath = p; validchoice = true;}
                    if (!validchoice)
                    {
                        System.err.println("Invalid choice " + choice +" of:");
                        for (Path p: gottenPaths) System.err.println(p.getFileName().toString());
                        return;
                    }
                    //send filenames of chosen path
                    levels = getLevels(chosenPath); //chosen path should be initialized
                    getLevel = true;
                    getUser = false;
                }
                else if (choice.equals("\4")) //user wants to quit
                {
                    System.out.println("User Left");
                    return;
                }
                else //go back
                {
                   getType = true;
                   getUser = false;
                }
                
            }
            if (getLevel)
            {
                sendPathOptions(levels);
                System.out.println("Waiting for level selection");
                String choice = receiveLine(); //get choice
                if (!choice.equals("\5") && !choice.equals("\4")) //go forward
                {
                    Path chosenPath = null;//to appease compiler
                    boolean validchoice = false; 
                    for (Path p: levels) if (p.getFileName().toString().equals(choice)) {chosenPath = p; validchoice = true;}
                    if (!validchoice)
                    {
                        System.err.println("Invalid choice " + choice +" of:");
                        for (Path p: levels) System.err.println(p.getFileName().toString());
                        return;
                    }
                    else 
                    {
                        switch (sendLevel(chosenPath))
                        {
                            case -1: //error reading file
                                return;
                            case 0: //file sent
                                System.out.println("File sent: " + chosenPath.toString());
                                gotLevel = true;
                        }
                    }
                }
                else if (choice.equals("\4")) //user wants to quit
                {
                    System.out.println("User Left");
                    return;
                }
                else //user wanted to go back
                {
                    System.out.println("Recived " + choice+", read as back");
                    getUser = true;
                    getLevel = false;
                }   
            }
            if (gotLevel)
            {
                //catch the page up that gets sent
                String choice = receiveLine();
                getLevel = true;
                gotLevel=false;
            }
        }
    }
    
    public void sendCatagoryOptions()
    {
        out.println("toplevel");
        out.println("Random");
        out.println("\3"); //send 3 to signal end of list
    }
    
    public void sendPathOptions(List<Path> paths)
    {
        out.println("choices");
        for (Path p: paths)
        {
            out.println(p.getFileName());
        }
        out.println("\3"); //send 3 to signal end of list
    }
    
    public int sendLevel(Path p)
    {
        out.println("level");
        out.println(p.toString());
        //read file into array of string lines
        List<String> levellines = new ArrayList<String>();
        try
        {
            levellines = Files.readAllLines(p, Charset.forName("UTF-8"));
        }
        catch (IOException e)
        {
            System.err.println("Unable to read file: " + p.toString());
            return -1;
        }
        //send array of string lines
        for (String s: levellines)
        {
            out.println(s);
        }
        out.println("\3");
        return 0;
    }
    
    public String receiveLine()
    {
        String temp;
        try
        {
            temp = in.readLine();
        }
        catch (IOException e)
        {
            System.err.println("Could not read Name from client");
            return "";
        }
        return temp;
    }
    
    public List<Path> getLevels(Path user)
    {
        List<Path> templist = new ArrayList<Path>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(user)) {
        for (Path path : stream) {
            if (!Files.isDirectory(path) && !path.getFileName().toString().equals("ATTRIBUTES"))
            {
                templist.add(path);
            }
        }
        } catch (IOException e) {
            System.err.println("Error reading from directory");
            return templist;
        }
        return templist;
    }
    
    public ArrayList<Path> getRandoms()
    {
        ArrayList<Path> templist = new ArrayList<Path>();
        templist.clear();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(leveldir)) {
        for (Path path : stream) {
            if (Files.isDirectory(path) && (path.toFile().listFiles().length > 1))
            {
                templist.add(path);
            }
        }
        } catch (IOException e) {
            System.err.println("Error reading from directory");
            return templist;
        }
        
        ArrayList<Path> chosenones = new ArrayList<Path>();
        ArrayList<Integer> ints = new ArrayList<Integer>();
        
        if (templist.size() >= 6)
        {
            while(ints.size() != 6)
            {
                int idx = (int)(Math.random()*templist.size()); //rand of all
                if (!ints.contains(idx)) ints.add(idx);
            }

            for(int i=0;i<6;i++){ //do this 6 times
                chosenones.add(templist.get(ints.get(i)));
            }
        }
        else
        {
            chosenones = templist;
        }
        
        return chosenones;
    }
}
