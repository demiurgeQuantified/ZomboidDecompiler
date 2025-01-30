package com.github.zomboiddecompiler.rosetta;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RosettaParser {
    public final List<RosettaNamespace> namespaces = new ArrayList<>();

    public void parseJson(File file) throws FileNotFoundException {
        StringBuilder source = new StringBuilder();
        try (FileReader fileReader = new FileReader(file)) {
            while(fileReader.ready()) {
                source.append((char)fileReader.read());
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
                        parameter.getJSONObject("type").getString("basic")
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
