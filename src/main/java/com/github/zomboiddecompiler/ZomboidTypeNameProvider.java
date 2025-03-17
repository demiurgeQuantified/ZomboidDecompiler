package com.github.zomboiddecompiler;

import com.github.zomboiddecompiler.rosetta.vineflower.DefaultTypeNameProvider;
import com.github.zomboiddecompiler.rosetta.vineflower.ITypeNameProvider;
import com.github.zomboiddecompiler.rosetta.vineflower.VineflowerUtils;
import org.jetbrains.java.decompiler.struct.gen.VarType;

import java.util.Map;

public class ZomboidTypeNameProvider implements ITypeNameProvider {
    @Override
    public String nameVar(VarType type) {
        String typeName = VineflowerUtils.getRawTypeName(type);
        if (TYPE_NAME_OVERRIDES.containsKey(typeName)) {
            typeName = TYPE_NAME_OVERRIDES.get(typeName);
            if (type.arrayDim > 0) {
                typeName += "s";
            }
            return typeName;
        }

        typeName = DEFAULT_TYPE_NAME_PROVIDER.nameVar(type);
        if (typeName.startsWith("iso")) {
            typeName = typeName.substring(3);
        }

        return typeName.substring(0, 1).toLowerCase() + typeName.substring(1);
    }

    private static final Map<String, String> TYPE_NAME_OVERRIDES = Map.of(
            "zombie/iso/IsoGridSquare", "square",
            "zombie/iso/Vector2", "vector",
            "zombie/iso/Vector3", "vector",
            "zombie/characters/IsoGameCharacter", "character",
            "zombie/inventory/InventoryItem", "item",
            "zombie/inventory/ItemContainer", "container",
            "zombie/vehicles/BaseVehicle", "vehicle",
            "zombie/vehicles/VehiclePart", "part",
            "zombie/inventory/types/HandWeapon", "weapon",
            "se/krka/kahlua/vm/KahluaTable", "table"
    );

    private static final ITypeNameProvider DEFAULT_TYPE_NAME_PROVIDER = new DefaultTypeNameProvider();
}
