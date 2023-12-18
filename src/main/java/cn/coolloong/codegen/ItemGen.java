package cn.coolloong.codegen;

import cn.coolloong.codegen.util.DownloadUtil;
import cn.coolloong.codegen.util.StringUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.javapoet.*;
import lombok.SneakyThrows;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;

import javax.lang.model.element.Modifier;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * CodeGen about ItemID and runtime_item_states.json
 */
public class ItemGen {
    private static final String JAVA_DOC = """
            @author Cool_Loong
            """;
    private static final Map<String, NbtMap> ITEM_ID = new TreeMap<>();
    private static List<NbtMap> ITEM_DATA;

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("src/main/resources/item_data.nbt");
        InputStream stream;
        if (file.exists()) {
            stream = new FileInputStream(file);
        } else {
            stream = DownloadUtil.downloadAsStream("https://github.com/AllayMC/BedrockData/raw/main/%s/item_data.nbt".formatted(args[0]));
        }
        try (stream) {
            NbtMap reader = (NbtMap) NbtUtils.createGZIPReader(stream).readTag();
            ITEM_DATA = reader.getList("item", NbtType.COMPOUND);
            ITEM_DATA.stream().filter(nbt -> !nbt.containsKey("blockId")).forEach(item -> ITEM_ID.put(item.getString("name"), item));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        generateItemID();
        generateRuntimeIdJson();
    }

    @SneakyThrows
    public static void generateItemID() {
        TypeSpec.Builder codeBuilder = TypeSpec.interfaceBuilder("ItemID")
                .addJavadoc(JAVA_DOC)
                .addModifiers(Modifier.PUBLIC);
        for (var entry : ITEM_ID.entrySet()) {
            var split = StringUtil.fastTwoPartSplit(
                    StringUtil.fastTwoPartSplit(entry.getKey(), ":", "")[1],
                    ".", "");
            var valueName = split[0].isBlank() ? split[1].toUpperCase() : split[0].toUpperCase() + "_" + split[1].toUpperCase();
            codeBuilder.addField(
                    FieldSpec.builder(String.class, valueName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$S", entry.getKey()).build()
            );
        }
        var javaFile = JavaFile.builder("", codeBuilder.build()).build();
        javaFile.writeToPath(Path.of("target"));
    }

    @SneakyThrows
    public static void generateRuntimeIdJson() {
        List<RuntimeIDEntry> result = new ArrayList<>();
        for (var entry : ITEM_DATA) {
            result.add(new RuntimeIDEntry(entry.getString("name"), entry.getShort("id"), entry.getBoolean("isComponentBased")));
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(result);
        Path path = Path.of("target/runtime_item_states.json");
        Files.deleteIfExists(path);
        Files.writeString(path, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    record RuntimeIDEntry(String name, int id, boolean isComponentBased) {
    }
}
