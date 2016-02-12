package org.usfirst.frc.team449;

import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An inner map for Classer
 */
public class Inner {
    /**
     * fields types indexed by field names
     */
    protected Map<String, String> components;
    /**
     * the superclass for this Inner's class
     */
    protected Inner parent;
    /**
     * the enclosing class's Classer
     */
    protected Classer enclosing;
    /**
     * the name of this inner class
     */
    protected String name;
    /**
     * JSONObject representing this inner
     */
    protected JSONObject jsonObject;

    public Inner(JavaClassSource source, Inner parent, Classer enclosing) {
        this.name = source.getName();
        this.components = new HashMap<>();
        this.parent = parent;
        this.enclosing = enclosing;

        List<FieldSource<JavaClassSource>> flds = source.getFields();
        for (FieldSource<JavaClassSource> f : flds) {
            String type = f.getType().getName();
            type = type.substring(type.lastIndexOf(".")+1);
            this.components.put(f.getName(), type);
            System.out.println(type);
        }
    }

    public JSONObject toJson() throws JSONException {
            jsonObject = new JSONObject();
            Set<String> compKeys = components.keySet();
            for (String key : compKeys) {
                String type = components.get(key);
                if (type.contains("[")) {
                    type = type.split("\\[")[0];
                }
                System.out.println(type);
                if (type.equals("double") || type.equals("boolean") || type.equals("int")) {
                    jsonObject.put(key, type);
                } else {
                    if (!jsonObject.has(type)) {
                        jsonObject.put(type, enclosing.getInners().get(type).toJson());
                        jsonObject.getJSONObject(type)
                                .put("instances", new JSONObject());
                    }
                    JSONObject inst = jsonObject.getJSONObject(type).getJSONObject("instances");
                    inst.put(key, enclosing.getInners().get(type).toJson());

                }
            }
        return jsonObject;
    }
}