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
    
    DuckPondServerDownloadHandler(Socket sock)
    {
        super("DuckPondServerUploadHandler");
        this.socket = sock;
        gottenPaths = new ArrayList<Path>();
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
            case 1: //return random user folders
                //out.println("RANDOM");
                gottenPaths = getRandoms();
                break;
            case 2: //return popular user folders
                out.println("POPULARS");
                //gottenPaths = getPopulars();
                break;
            case 3: //reccomended requested
                out.println("RECOMMENEDED");
                //gottenPaths = getRecommended();
                break;
            case 4: //search requested
                out.println("SEARCH");
                //get searchstring
                //gottenPaths = getSearch(searchstring)
                break;   
        }
        sendPathOptions(gottenPaths);
        String choice = receiveLine(); //get choice
        Path chosenPath;
        chosenPath = Paths.get("BOGUS", "TO", "APPEASE", "COMPILER");
        boolean validchoice = false;
        for (Path p: gottenPaths) if (p.getFileName().toString().equals(choice)) {chosenPath = p; validchoice = true;}
        if (!validchoice)
        {
            System.err.println("Invalid choice " + choice +" of:");
            for (Path p: gottenPaths) System.err.println(p.getFileName().toString());
            return;
        }
        //send filenames of chosen path
        List<Path> levels;
        levels = getLevels(chosenPath); //chosen path should be initialized
        if (levels.isEmpty()) //User has no Levels! or they couldnt be loaded
        {
            sendPathOptions(levels); //send it anyway, let client figure it out
        }
        else //user had something they'd like to share...
        {
            sendPathOptions(levels);
            choice = receiveLine(); //get choice
            validchoice = false;
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
                        System.out.println("File sent!");
                }
            }
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
    
    public void sendPathOptions(List<Path> paths)
    {
        for (Path p: paths)
        {
            out.println(p.getFileName());
        }
        out.println("\3"); //send 3 to signal end of list
    }
    
    public int sendLevel(Path p)
    {
        //read file into array of string lines
        List<String> levellines = new ArrayList<String>();
        try
        {
            levellines = Files.readAllLines(p);
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
            if (Files.isDirectory(path))
            {
                templist.add(path);
            }
        }
        } catch (IOException e) {
            System.err.println("Error reading from directory");
            return templist;
        }
        
        ArrayList<Path> chosenones = new ArrayList<Path>();
        for(int i=0;i<6;i++){ //do this 6 times
            int idx = (int)(Math.random()*templist.size()); //rand of all
            chosenones.add(templist.get(idx));
        }
        return chosenones;
    }
}
