package com.github.zomboiddecompiler.rosetta;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class RosettaParser {
    public final List<RosettaNamespace> namespaces = new ArrayList<>();

    public void parseDirectory(Path directory) {
        Path jsonDirectory = directory.resolve("json");
        if (Files.exists(jsonDirectory)) {
            directory = jsonDirectory;
        }

        try (Stream<Path> files = Files.walk(directory)) {
            for (Path file : files.toList()) {
                if (!file.getFileName().toString().toLowerCase().endsWith(".json")) continue;
                try {
                    parseJson(Files.newInputStream(file));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void parseJson(InputStream stream) {
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
                rosettaField.setNotes(field.optString("notes"));

                rosettaClass.addField(rosettaField);
            }
        }
        
        JSONArray constructors = clazz.optJSONArray("constructors");
        if (constructors != null) {
            for (int i = 0; i < constructors.length(); i++) {
                rosettaClass.addConstructor(
                        parseConstructor(constructors.getJSONObject(i)));
            }
        }

        rosettaClass.setNotes(clazz.optString("notes"));

        return rosettaClass;
    }

    private RosettaReturn parseReturn(JSONObject returns) {
        String type = parseType(returns.getJSONObject("type"));
        if (type.equalsIgnoreCase("void")) {
            return RosettaReturn.VOID;
        }
        
        RosettaReturn rosettaReturn = new RosettaReturn(
                returns.optString("name"),
                parseType(returns.getJSONObject("type"))
        );

        rosettaReturn.setNotes(returns.optString("notes"));

        return rosettaReturn;
    }

    private void parseParameters(RosettaExecutable executable, JSONArray parameters) {
        for (int i = 0; i < parameters.length(); i++) {
            JSONObject parameter = parameters.getJSONObject(i);
            RosettaParameter rosettaParameter = new RosettaParameter(
                    parameter.getString("name"),
                    parseType(parameter.getJSONObject("type"))
            );

            rosettaParameter.setNotes(parameter.optString("notes"));

            executable.addParameter(rosettaParameter);
        }
    }

    private RosettaMethod parseMethod(JSONObject method) {
        RosettaMethod rosettaMethod = new RosettaMethod(
                method.getString("name"), parseReturn(method.getJSONObject("returns")));

        JSONArray parameters = method.optJSONArray("parameters");
        if (parameters != null) {
            parseParameters(rosettaMethod, parameters);
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

        rosettaMethod.setNotes(method.optString("notes"));

        return rosettaMethod;
    }

    private RosettaConstructor parseConstructor(JSONObject constructor) {
        RosettaConstructor rosettaConstructor = new RosettaConstructor();

        JSONArray parameters = constructor.optJSONArray("parameters");
        if (parameters != null) {
            parseParameters(rosettaConstructor, parameters);
        }
        rosettaConstructor.setNotes(constructor.optString("notes"));

        return rosettaConstructor;
    }
}
