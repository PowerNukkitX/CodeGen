package cn.coolloong.codegen;

import cn.coolloong.codegen.util.DownloadUtil;
import cn.coolloong.codegen.util.Identifier;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
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
import java.util.List;
import java.util.TreeSet;

/**
 * Allay Project 2023/3/26
 *
 * @author daoge_cmd | Cool_Loong
 */
public class BlockIDGen {
    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("src/main/resources/block_palette.nbt");
        InputStream stream;
        if (file.exists()) {
            stream = new FileInputStream(file);
        } else {
            stream = DownloadUtil.downloadAsStream("https://github.com/AllayMC/BedrockData/raw/main/%s/block_palette.nbt".formatted(args[0]));
        }
        try (stream) {
            NbtMap reader = (NbtMap) NbtUtils.createGZIPReader(stream).readTag();
            BLOCK_PALETTE_NBT = reader.getList("blocks", NbtType.COMPOUND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        generate();
    }

    private static List<NbtMap> BLOCK_PALETTE_NBT;

    private static final String JAVA_DOC = """
            @author Cool_Loong
            """;

    @SneakyThrows
    public static void generate() {
        TypeSpec.Builder codeBuilder = TypeSpec.interfaceBuilder("BlockID")
                .addJavadoc(JAVA_DOC)
                .addModifiers(Modifier.PUBLIC);
        TreeSet<String> values = new TreeSet<>();
        BLOCK_PALETTE_NBT.forEach((block) -> values.add(block.getString("name")));
        for (var identifier : values) {
            codeBuilder.addField(FieldSpec
                    .builder(String.class, new Identifier(identifier).path().toUpperCase(), Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                    .initializer("$S", identifier)
                    .build());
        }
        var javaFile = JavaFile.builder("org.allaymc.dependence", codeBuilder.build()).build();
        Path path = Path.of("target/BlockID.java");
        Files.deleteIfExists(path);
        Files.writeString(path, javaFile.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
}
