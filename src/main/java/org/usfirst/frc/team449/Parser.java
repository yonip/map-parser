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
    public static String path = "/Users/yonipedersen/Documents/Robotics/robotics-2016/src";
    public static PrintStream writer;

    public static void main(String[] args) {
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

    public static Stack<File> getFiles(File file) {
        Stack<File> dir = new Stack<>();
        Stack<File> files = new Stack<>();
        File[] subfiles;
        dir.push(file);
        if (!file.isDirectory()) {
            return dir;
        }

        while (!dir.empty()) {
            subfiles = dir.pop().listFiles();
            for (File subfile : subfiles) {
                if (subfile.isDirectory()) {
                    dir.push(subfile);
                } else {
                    files.push(subfile);
                }
            }
        }

        return files;
    }

    public static Map<String, File> mapFiles(File file) {
        Map<String, File> map = new HashMap<>();
        Stack<File> files = getFiles(file);
        for (File f : files) {
            File rep = map.put(f.getName(), f);
            if (rep != null) {
                System.out.println("Replaced file " + rep.getAbsolutePath() + " with " + f.getAbsolutePath());
            }
        }
        return map;
    }

    public static Map<String, File> filterFile(File file) {
        Map<String, File> map = mapFiles(file);
        Map<String, File> ret = new HashMap<>();
        for (String name : map.keySet()) {
            if (name.equals("OIMap.java")) {
                continue;
            }
            Pattern mapPattern = Pattern.compile("(\\w+Map)(?=\\.java)"); // look for something composed of word characters with Map.java after them
            Matcher mapMatcher = mapPattern.matcher(name);
            int start;
            int end;
            if (mapMatcher.find()) {
                start = mapMatcher.start();
                end = mapMatcher.end();
            } else {
                continue;
            }
            ret.put(name.substring(start, end), map.get(name));
            //System.out.println(name.substring(start, end));
        }
        return ret;
    }

    public static JSONObject mapClasses(File file) {
        Map<String, File> files = filterFile(file);
        Map<String, JavaClassSource> srcs = new HashMap<>();
        Map<String, Classer> classers = new HashMap<>();
        try {
            Set<String> names = files.keySet();
            for (String name : names) {
                //System.out.println(name);
                JavaClassSource source = Roaster.parse(JavaClassSource.class, files.get(name));
                srcs.put(name, source);
            }
            System.out.println();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Classer robotMap = new Classer(srcs.remove("RobotMap"), null);
        classers.put("RobotMap", robotMap);

        while (!srcs.keySet().isEmpty()) {
            List<String> keys = new ArrayList<>();
            keys.addAll(srcs.keySet());
            String key = keys.get(0);
            String next = srcs.get(key).getSuperType();
            next = next.substring(next.lastIndexOf(".")+1);
            while (!classers.containsKey(next)) {
                key = next;
                next = srcs.get(key).getSuperType();
                next = next.substring(next.lastIndexOf(".") + 1);
                //System.out.println(key + " " + next);
                //System.out.println(classers.containsKey(next));
            }
            // "next" is now the key to the parent of the value for "key" in Classers
            classers.put(key, new Classer(srcs.remove(key), classers.get(next)));
        }
        try {
            return robotMap.toJson();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
