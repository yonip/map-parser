package org.usfirst.frc.team449;

import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Holds a class for parsing a Subsystem Map
 * Really representation of the map, holds package, imports, and fields as Fielders
 * @see Inner
 */
public class Classer {
    /**
     * fields types indexed by field names
     */
    protected Map<String, String> components;
    /**
     * a map of the extending classes' Classers, indexed by name
     */
    protected Map<String, Classer> children;
    /**
     * the Classer for this Classer's class's superclass
     */
    protected Classer parent;
    /**
     * a map of the Classers representing the inner classes of this class, indexed by their name
     */
    protected Map<String, Inner> inners;
    /**
     * the name of this Subsystem Map
     */
    protected String name;

    /**
     * Creates a class representing one of the Subsystem Maps, which holds configuration for the Robots Subsystems. <br/>
     * All classes stored in here should be subclasses of RobotMap or be RobotMap itself. <br/>
     * RobotMap should be the only Classer with a null parent.
     * @param source the JavaClassSource for parsing this Classer's class's source
     * @param parent the Classer representing the superclass of this Classer's class
     * @see Inner
     */
    public Classer(JavaClassSource source, Classer parent) {
        this.name = source.getName();
        this.children = new HashMap<>();
        this.inners = new HashMap<>();
        this.components = new HashMap<>();
        this.parent = parent;
        if (this.parent != null) { // register with the parent so toJson works
            this.parent.children.put(name, this);
        }

        // get all the fields in
        List<FieldSource<JavaClassSource>> flds = source.getFields();
        for (FieldSource<JavaClassSource> f : flds) {
            String type = f.getType().getName();
            type = type.substring(type.lastIndexOf(".")+1);
            this.components.put(f.getName(), type);
        }

        // find all of the inner classes, aka the MapObjects for components
        List<JavaSource<?>> inrs = source.getNestedTypes();
        Map<String, JavaClassSource> inrsMap = new HashMap<>(); // this will hold the inner classes that were not handles as of yet
        for (JavaSource<?> inr : inrs) {
            inrsMap.put(inr.getName(), (JavaClassSource) inr);
        }
        if (inrsMap.containsKey("MapObject")) { // we should have a reference to MapObject somehwere, but if it's not here  it could be in a superclass
            this.inners.put("MapObject", new Inner(inrsMap.remove("MapObject"), null, this));
        }

        while (!inrsMap.keySet().isEmpty()) { // keep going until we run out of inner classes
            // we need to start at some key, and for some reason Sets can't do that, so we're making an ArrayList for the keys to pick one out
            List<String> keys = new ArrayList<>();
            keys.addAll(inrsMap.keySet());
            String key = keys.get(0);
            // cool we got a key, now to see what its superclass is
            String next = inrsMap.get(key).getSuperType();
            next = next.substring(next.lastIndexOf(".")+1);
            // alright got the subclass name
            // now to find out if we already parsed its superclass
            while (!getInners().containsKey(next)) {
                // now we keep going up in the hierarchy until we find a class whose superclass is already parsed (should
                // always terminate since RobotMap is the super of all Subsystem Maps, and RobotMap will hold MapObject, superclass
                // of all inners
                key = next;
                next = inrsMap.get(key).getSuperType();
                next = next.substring(next.lastIndexOf(".")+1);
            }
            // "next" is now the key to the parent of the value for "key" in getInners()
            this.inners.put(key, new Inner(inrsMap.remove(key), getInners().get(next), this)); // parse the class associated with "key" and tell it that its super is "next" and elncloser is this object
        }
    }

    /**
     * Gets a map of the inner classes in this Subsystem Map class, and by all of its parent classes by creating an empty map,
     * filling it with this class's inner classes, and calls this method on the parent, if the parent exists. <br/>
     * This fakes inheritance of inner classes for classes
     * @return a Map of the inner classes, indexed by the inner classes' names
     * @see Inner#getComponents()
     * @see Inner
     */
    protected Map<String, Inner> getInners() {
        Map<String, Inner> map = new HashMap<>();
        map.putAll(this.inners);
        if (parent == null) {
            return map;
        }
        map.putAll(parent.getInners());
        return map;
    }

    /**
     * creates a JSONObject based on the contents of this Subsystem Map
     * <br/><code>{</code>
     * <br/><code>  "child-name": {//the child's bean},</code>
     * <br/><code>  "components": {}</code>
     * <br/><code>}</code>
     * <br/> where <code>child-name</code> is the name of a subclass of this Map and <code>components</code> holds the beans
     * of the fields for this Subsystem Map
     * <br/> this creates the most verbose tree possible
     * <br/> if the component is a primitive (specifically <code>int</code>, <code>double</code> and <code>boolean</code>)
     * the bean object is simply replaced by a string of the primitive (eg <code>"int"</code> for an <code>int</code>)
     * @return a JSONObject
     * @throws JSONException if somehow formatting broke. which should ever happen
     * @see Inner#toJson()
     * @see Inner
     */
    protected JSONObject toJson() throws JSONException { // TODO: inline documnentation
        JSONObject jo = new JSONObject();
        Set<String> childKeys = children.keySet();
        for (String  key : childKeys) {
            jo.put(key, children.get(key).toJson());
        }
        JSONObject compObj = new JSONObject();
        Set<String> compKeys = components.keySet();
        for (String key : compKeys) {
            String type = components.get(key);
            if (type.equals("double") || type.equals("boolean") || type.equals("int")) {
                compObj.put(key, type);
            } else {
                if (!compObj.has(type)) {
                    compObj.put(type, getInners().get(type).toJson());
                    compObj.getJSONObject(type).put("instances", new JSONObject());
                }
                JSONObject inst = compObj.getJSONObject(type).getJSONObject("instances");
                inst.put(key, getInners().get(type).toJson());

            }
        }

        jo.put("components", compObj);
        return jo;
    }
}
