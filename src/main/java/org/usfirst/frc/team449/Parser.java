package org.usfirst.frc.team449;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class. Includes all functionality for parsing the file
 */
public class Parser {
    // TODO make this depend on where the built .jar is
    // TODO check that this is looking at a RobotProject
    public static String path = "/Users/yonipedersen/Documents/Robotics/robotics-2016/src";

    public static void main(String[] args) {
        PrintStream writer = null;
        try {
            writer = new PrintStream("src/main/resources/cfg.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            String out = mapClasses(new File(path)).toString(2);
            System.out.println(out);
            writer.println(out);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds all the files that may be nested in the given File. <br/>
     * If the given File is not a directory, a stack containing only the given file is returned. <br/>
     * If the given File is a directory, its contents are searched, as well as any subfolders, until all subfolders of this
     * directory are searched for non-directory files. All non-directory files that were found will be in the returned stack.
     * @param file the File to start searching for non-directory files
     * @return a Stack of Files
     */
    public static Stack<File> getFiles(File file) {
        Stack<File> dir = new Stack<>(); // a stack to hold all the directory Files
        Stack<File> files = new Stack<>(); // all non-directory files, will be returned if the given files is a directory
        File[] subfiles; // to hold the files within each directory that will be searched
        dir.push(file);
        // you just gave me a file, not a directory, there's nothing to search through so ill give it back to you
        if (!file.isDirectory()) {
            return dir;
        }

        while (!dir.empty()) {
            subfiles = dir.pop().listFiles(); // look through the contents of the next directory
            for (File subfile : subfiles) {
                if (subfile.isDirectory()) {
                    dir.push(subfile); // it's a directory, ill have to look through it later
                } else {
                    files.push(subfile); // it's not a directory, so i'll add it to the stack i will return
                }
            }
        }

        return files;
    }

    /**
     * Creates a map of all the subfiles of the given file, using {@link File#getName()} for the index. <br/>
     * If the given file is not a directory, a map with only that file will be returned. <br/>
     * If any files are removed from this map due to overriding, a message is printed to the console acknowledging the fact.
     * @param file the File to look through, or to return
     * @return a Map of Files indexed by their names
     * @see File#getName()
     * @see #getFiles(File)
     */
    public static Map<String, File> mapFiles(File file) {
        Map<String, File> map = new HashMap<>();
        Stack<File> files = getFiles(file); // use getFiles to get the neste files
        for (File f : files) {
            File rep = map.put(f.getName(), f); // put those files in a map
            if (rep != null) { // this will only happen if two files have the same name and extension, shouldn't affect the RobotMaps though
                System.out.println("Replaced file " + rep.getAbsolutePath() + " with " + f.getAbsolutePath());
            }
        }
        return map;
    }

    /**
     * Finds all non-directory Files nested in the given file, and returns a Map of only the files that have names that
     * would, by naming convention, make them Robot or Subsystem Maps. <br/>
     * The current implementation also excludes OIMap, since it doesn't extend RobotMap by convention right now. <br/>
     * The index is the class name of the file. <br/>
     * In this implementation, only files with names that match <code>(\w+Map)(?=\.java)</code> are added to the map, and
     * are indexed by the string that that regex returns (e.g. the file <code>TankDriveMap.java</code> would be indexed under
     * <code>TankDriveMap</code> while <code>TankDriveSubsystem.java</code> or <code>cfg.json</code> simply won't be indexed)
     * @param file the File to look through for Robot and Subsystem Maps
     * @return a Map of Robot and Subsystem Maps indexed by class name
     * @see #mapFiles(File)
     */
    public static Map<String, File> filterFile(File file) {
        Map<String, File> map = mapFiles(file); // get a map of all the files nested in the given file
        Map<String, File> ret = new HashMap<>();
        for (String name : map.keySet()) {
            Pattern mapPattern = Pattern.compile("(\\w+Map)(?=\\.java)"); // look for something composed of word characters with Map.java after them
            Matcher mapMatcher = mapPattern.matcher(name);
            int start;
            int end;
            if (mapMatcher.find()) { // if it matches, find the start and end of the matching region, for indexing
                start = mapMatcher.start();
                end = mapMatcher.end();
            } else {
                continue; // nothing was matched in this file's name, so we don't want it
            }
            ret.put(name.substring(start, end), map.get(name)); // store this Map under its class name
        }
        return ret;
    }

    /**
     * Creates the JSON that is the most verbose form for a skeleton of a map (a configuration map, not a {@link Map}) generated
     * from the Robot and Subsystem Maps nested inside the given file. Subsytem Maps are direct children of their superclass,
     * while their fields are stored under the object <code>components</code>. The fields are grouped under their declared class,
     * so that the conventions for sharing values across instances of a class is easier. Any instances of a class are under the
     * <code>instances</code> object, and so any field of the class defined as a direct child of the class will be the default
     * value for all of its instances. <br/>
     * @param file the file to search through for Robot and Subsystem Maps
     * @return a JSONObject representing the whole configuration tree for the Robot using the found maps
     * @see #filterFile(File)
     * @see Classer
     * @see Inner
     * @see JSONObject
     */
    public static JSONObject mapClasses(File file) {
        Map<String, File> files = filterFile(file); // find the relevant files
        Map<String, JavaClassSource> srcs = new HashMap<>(); // make a map for the source parsers
        // make a map for the Classers that hold the relevant information and are configured for this purpose. also keeps track of what maps were parsed so far
        Map<String, Classer> classers = new HashMap<>();
        try {
            Set<String> names = files.keySet(); // now lets get all the names so we can iterate through the map of files
            for (String name : names) {
                JavaClassSource source = Roaster.parse(JavaClassSource.class, files.get(name)); // make a parser for the file given by name
                srcs.put(name, source); // add that parser to the map, and index it by its name
            }
        } catch (FileNotFoundException e) { // shouldn't happen since we created the map based on the files that _were_ there
            e.printStackTrace();
        }

        Classer robotMap = new Classer(srcs.remove("RobotMap"), null); // there should always be a RobotMap, and it is always the parent of all the other maps
        classers.put("RobotMap", robotMap); // alright, we parsed the RobotMap 10/10

        while (!srcs.keySet().isEmpty()) { // keep going until we run out of files
            // we need to start at some key, and for some reason Sets can't do that, so we're making an ArrayList for the keys to pick one out
            List<String> keys = new ArrayList<>();
            keys.addAll(srcs.keySet());
            String key = keys.get(0);
            // cool we got a key, now to see what its superclass is
            String next = srcs.get(key).getSuperType();
            next = next.substring(next.lastIndexOf(".")+1);
            // alright got the subclass name
            // now to find out if we already parsed its superclass
            while (!classers.containsKey(next)) { // now we keep going up in the hierarchy until we find a class whose superclass is already parsed (should always terminate since RobotMap is the super of all Subsystem Maps
                key = next;
                next = srcs.get(key).getSuperType();
                next = next.substring(next.lastIndexOf(".") + 1);
            }
            // "next" is now the key to the parent of the value for "key" in Classers
            classers.put(key, new Classer(srcs.remove(key), classers.get(next))); // parse the class associated with "key" and tell it that its super is "next"
        }
        try {
            return robotMap.toJson();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
