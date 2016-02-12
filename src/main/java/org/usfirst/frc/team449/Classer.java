package org.usfirst.frc.team449;

import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Holds a class for parsing
 * Really representation of the map, holds package, imports, and fields as Fielders
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
     *
     */
    protected String name;

    /**
     * constructs a Classer based on its source code.
     * @param source the JavaClassSource representing this Classer's source
     */
    public Classer(JavaClassSource source, Classer parent) {
        this.name = source.getName();
        this.children = new HashMap<>();
        this.inners = new HashMap<>();
        this.components = new HashMap<>();
        this.parent = parent;
        if (this.parent != null) {
            this.parent.children.put(name, this);
        }

        System.out.println(name);
        List<FieldSource<JavaClassSource>> flds = source.getFields();
        for (FieldSource<JavaClassSource> f : flds) {
            String type = f.getType().getName();
            type = type.substring(type.lastIndexOf(".")+1);
            this.components.put(f.getName(), type);
            System.out.println(f.getName() + " " + type);
        }
        System.out.println();

        List<JavaSource<?>> inrs = source.getNestedTypes();
        Map<String, JavaClassSource> inrsMap = new HashMap<>();
        for (JavaSource<?> inr : inrs) {
            inrsMap.put(inr.getName(), (JavaClassSource) inr);
        }
        if (inrsMap.containsKey("MapObject")) {
            this.inners.put("MapObject", new Inner(inrsMap.remove("MapObject"), null, this));
        }

        while (!inrsMap.keySet().isEmpty()) {
            List<String> keys = new ArrayList<>();
            keys.addAll(inrsMap.keySet());
            String key = keys.get(0);
            String next = inrsMap.get(key).getSuperType();
            next = next.substring(next.lastIndexOf(".")+1);
            while (!getInners().containsKey(next)) {
                key = next;
                next = inrsMap.get(key).getSuperType();
                next = next.substring(next.lastIndexOf(".")+1);
            }
            this.inners.put(key, new Inner(inrsMap.remove(key), getInners().get(next), this));
        }
    }

    protected Map<String, Inner> getInners() {
        Map<String, Inner> map = new HashMap<>();
        map.putAll(this.inners);
        if (parent == null) {
            return map;
        }
        map.putAll(parent.getInners());
        return map;
    }

    protected JSONObject toJson() throws JSONException {
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
