package cn.coolloong.codegen;

import cn.coolloong.codegen.util.DownloadUtil;
import cn.coolloong.codegen.util.StringUtil;
import com.squareup.javapoet.*;
import lombok.SneakyThrows;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public class ItemIDGen {
    private static final String VERSION = "1.20.50.03";
    private static final String JAVA_DOC = """
            @author Cool_Loong
            """;
    private static final Map<String, NbtMap> ITEM_DATA = new TreeMap<>();

    static {
        try (InputStream stream = DownloadUtil.downloadAsStream("https://github.com/AllayMC/BedrockData/raw/main/%s/item_data.nbt".formatted(VERSION))) {
            NbtMap reader = (NbtMap) NbtUtils.createGZIPReader(stream).readTag();
            reader.getList("item", NbtType.COMPOUND).stream().filter(nbt-> !nbt.containsKey("blockId")).forEach(item -> ITEM_DATA.put(item.getString("name"), item));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        generate();
    }

    @SneakyThrows
    public static void generate() {
        TypeSpec.Builder codeBuilder = TypeSpec.interfaceBuilder("ItemID")
                .addJavadoc(JAVA_DOC)
                .addModifiers(Modifier.PUBLIC);
        addEnums(codeBuilder);
        var javaFile = JavaFile.builder("", codeBuilder.build()).build();
        javaFile.writeToPath(Path.of("target"));
    }

    private static void addEnums(TypeSpec.Builder codeBuilder) {
        for (var entry : ITEM_DATA.entrySet()) {
            var split = StringUtil.fastTwoPartSplit(
                    StringUtil.fastTwoPartSplit(entry.getKey(), ":", "")[1],
                    ".", "");
            var valueName = split[0].isBlank() ? split[1].toUpperCase() : split[0].toUpperCase() + "_" + split[1].toUpperCase();
            codeBuilder.addField(
                    FieldSpec.builder(String.class, valueName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$S", entry.getKey()).build()
            );
        }
    }
}
