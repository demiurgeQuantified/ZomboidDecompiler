module ZomboidDecompiler {
    requires org.jetbrains.annotations;
    requires org.json;
    requires vineflower;
    requires info.picocli;
    provides org.jetbrains.java.decompiler.api.plugin.Plugin with com.github.zomboiddecompiler.rosetta.RosettaPlugin;
    opens com.github.zomboiddecompiler.commands to info.picocli;
}