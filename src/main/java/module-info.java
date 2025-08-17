import com.github.zomboiddecompiler.rosetta.vineflower.RosettaPlugin;

module com.github.zomboiddecompiler {
    requires org.jetbrains.annotations;
    requires org.json;
    requires vineflower;
    requires info.picocli;
    requires java.compiler;
    requires org.objectweb.asm;
    requires jdk.jdi;
    requires org.snakeyaml.engine.v2;

    provides org.jetbrains.java.decompiler.api.plugin.Plugin with
            RosettaPlugin;

    opens com.github.zomboiddecompiler.commands to
            info.picocli;
}