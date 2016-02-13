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
 * A representation of an inner class for a Subsystem Map. Generally this will hold the configuration for a component
 * @see Classer
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

    /**
     * Creates a class representing one of the inner Maps, which holds configuration for the Robots components. <br/>
     * All classes stored in here should be subclasses of MapObject or be MapObject itself. <br/>
     * MapObject should be the only Inner with a null parent. <br/>
     * The enclosing Classer is to be able to find any Inner that may be used as a type for this Inner's fields.
     * @param source the JavaClassSource for pasrsing this Inner's source code
     * @param parent the Inner object representing the superclass of the class this Inner represents
     * @param enclosing the enclosing class for this Inner, which references a Subsystem Map, so that this Inner can find
     *                  the context that it is in
     * @see Classer
     */
    public Inner(JavaClassSource source, Inner parent, Classer enclosing) {
        this.name = source.getName();
        this.components = new HashMap<>();
        this.parent = parent;
        this.enclosing = enclosing;

        // find all the fields
        List<FieldSource<JavaClassSource>> flds = source.getFields();
        for (FieldSource<JavaClassSource> f : flds) {
            String type = f.getType().getName();
            type = type.substring(type.lastIndexOf(".")+1);
            this.components.put(f.getName(), type);
        }
    }

    /**
     * Gets a map of the fields designated by this inner class, and by all of its parent classes by creating an empty map,
     * filling it with this class's fields, and calls this method on the parent, if the parent exists. <br/>
     * This fakes inheritance of fields for classes
     * @return a Map of the field types, indexed by the fields' names
     * @see Classer#getInners()
     * @see Classer
     */
    protected Map<String, String> getComponents() {
        Map<String, String> map = new HashMap<>();
        map.putAll(this.components);
        if (parent == null) {
            return map;
        }
        map.putAll(parent.getComponents());
        return map;
    }

    /**
     * creates a JSONObject based on the contents of this inner-class representation
     * <br/><code>{</code>
     * <br/><code>  "component-name": {//the component's bean},</code>
     * <br/><code>  "instances": {}</code>
     * <br/><code>}</code>
     * <br/> where <code>component-name</code> is the name of a field for this inner class
     * <br/> this creates the most verbose tree possible
     * <br/> if the component is a primitive (specifically <code>int</code>, <code>double</code> and <code>boolean</code>)
     * the bean object is simply replaced by a string of the primitive (eg <code>"int"</code> for an <code>int</code>)
     * <p/>
     * here is, for example, the JSONObject representation for a Motor (which only has primitive fields):
     * <br/><code>{</code>
     * <br/><code>  "PORT" : "int",
     * <br/><code>  "INVERTED" : "boolean"
     * <br/><code>}</code>
     * @return a JSONObject
     * @throws JSONException if somehow formatting broke. which should ever happen
     * @see Classer#toJson()
     * @see Classer
     */
    public JSONObject toJson() throws JSONException {
            jsonObject = new JSONObject();
            Set<String> compKeys = getComponents().keySet();
            for (String key : compKeys) {
                String type = getComponents().get(key);
                if (type.contains("[")) {
                    type = type.split("\\[")[0];
                }
                if (type.equals("double") || type.equals("boolean") || type.equals("int")) {
                    jsonObject.put(key, type);
                } else {
                    if (!jsonObject.has(type)) {
                        jsonObject.put(type, enclosing.getInners().get(type).toJson());
                        jsonObject.getJSONObject(type).put("instances", new JSONObject());
                    }
                    JSONObject inst = jsonObject.getJSONObject(type).getJSONObject("instances");
                    inst.put(key, enclosing.getInners().get(type).toJson());

                }
            }
        return jsonObject;
    }
}
