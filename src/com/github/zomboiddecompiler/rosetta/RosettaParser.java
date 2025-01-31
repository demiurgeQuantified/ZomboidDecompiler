package com.github.zomboiddecompiler.rosetta;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RosettaParser {
    public final List<RosettaNamespace> namespaces = new ArrayList<>();

    public void parseJson(InputStream stream) throws FileNotFoundException {
        StringBuilder source = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(stream)) {
            while(reader.ready()) {
                source.append((char)reader.read());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String sourceString = source.toString().replace("\n", "");
        JSONObject json = new JSONObject(sourceString);
        JSONObject jsonNamespaces = json.getJSONObject("namespaces");
        for (String namespaceName : jsonNamespaces.keySet()) {
            namespaces.add(
                    parseNamespace(jsonNamespaces.getJSONObject(namespaceName), namespaceName));
        }
    }

    private String parseType(JSONObject type) {
        // FIXME: generic types are not supported
        return type.getString("basic");
    }

    private RosettaNamespace parseNamespace(JSONObject namespace, String name) {
        RosettaNamespace rosettaNamespace = new RosettaNamespace(name);

        for(String clazzName : namespace.keySet()) {
            rosettaNamespace.addClass(
                    parseClass(namespace.getJSONObject(clazzName), clazzName));
        }

        return rosettaNamespace;
    }

    private RosettaClass parseClass(JSONObject clazz, String name) {
        RosettaClass rosettaClass = new RosettaClass(name);

        JSONArray methods = clazz.optJSONArray("methods");
        if (methods != null) {
            for (int i = 0; i < methods.length(); i++) {
                rosettaClass.addMethod(
                        parseMethod(
                                methods.getJSONObject(i)));
            }
        }

        JSONObject fields = clazz.optJSONObject("fields");
        if (fields != null) {
            JSONArray fieldKeys = fields.names();
            for (Object keyObj : fieldKeys.toList()) {
                if (!(keyObj instanceof String key)) {
                    // throw a warning or something
                    continue;
                }
                JSONObject field = fields.getJSONObject(key);
                RosettaField rosettaField = new RosettaField(field.getString("name"),
                                                             parseType(field.getJSONObject("type")));
                rosettaClass.addField(rosettaField);
            }
        }
        
        // TODO: parse constructors

        return rosettaClass;
    }

    private RosettaMethod parseMethod(JSONObject method) {
        RosettaMethod rosettaMethod = new RosettaMethod(method.getString("name"));

        JSONObject returns = method.getJSONObject("returns");
        rosettaMethod.setReturnType(returns.getJSONObject("type").getString("basic"));

        JSONArray parameters = method.optJSONArray("parameters");
        if (parameters != null) {
            for (int i = 0; i < parameters.length(); i++) {
                JSONObject parameter = parameters.getJSONObject(i);
                rosettaMethod.addParameter(new RosettaMethod.Parameter(
                        parameter.getString("name"),
                        parseType(parameter.getJSONObject("type"))
                ));
            }
        }

        JSONArray modifiers = method.optJSONArray("modifiers");
        if (modifiers != null) {
            for (int i = 0; i < modifiers.length(); i++) {
                String modifier = modifiers.getString(i);
                if (Objects.equals(modifier, "static")) {
                    rosettaMethod.setStatic(true);
                }
            }
        }

        return rosettaMethod;
    }
}
