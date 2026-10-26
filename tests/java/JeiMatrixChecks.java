import compat.jeirouter.mixin.RouterMixinPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Checks real JEI artifacts, including every JEI member referenced by the compiled router. */
public final class JeiMatrixChecks {
    private record Member(String owner, String name, String desc, boolean field, boolean isStatic) {}
    private static final Set<Member> members = new HashSet<>();
    private static final Set<String> types = new HashSet<>();
    public static void main(String[] args) throws Exception {
        try (var classes = Files.walk(Path.of(args[0]))) {
            for (var file : classes.filter(p -> p.toString().endsWith(".class")).toList()) {
                var node = new ClassNode(); new ClassReader(Files.readAllBytes(file)).accept(node, 0);
                type(node.superName); node.interfaces.forEach(JeiMatrixChecks::type);
                for (var field : node.fields) descriptor(field.desc);
                for (var method : node.methods) {
                    descriptor(method.desc);
                    for (var instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode m) member(m.owner, m.name, m.desc, false, m.getOpcode() == Opcodes.INVOKESTATIC);
                        if (instruction instanceof FieldInsnNode f) member(f.owner, f.name, f.desc, true,
                            f.getOpcode() == Opcodes.GETSTATIC || f.getOpcode() == Opcodes.PUTSTATIC);
                        if (instruction instanceof TypeInsnNode t) type(t.desc);
                        if (instruction instanceof LdcInsnNode l && l.cst instanceof Type t) descriptor(t.getDescriptor());
                        if (instruction instanceof InvokeDynamicInsnNode i) {
                            descriptor(i.desc);
                            for (var arg : i.bsmArgs) {
                                if (arg instanceof Handle h) member(h.getOwner(), h.getName(), h.getDesc(), h.getTag() <= 4,
                                    h.getTag() == Opcodes.H_INVOKESTATIC || h.getTag() == Opcodes.H_GETSTATIC || h.getTag() == Opcodes.H_PUTSTATIC);
                                if (arg instanceof Type t) descriptor(t.getDescriptor());
                            }
                        }
                    }
                }
            }
        }
        int count = 0;
        var failures = new ArrayList<String>();
        try (var jars = Files.list(Path.of(args[1]))) {
            for (var path : jars.filter(p -> p.getFileName().toString().matches("jei-1\\.21\\.1-neoforge-.*\\.jar")).sorted().toList()) {
                String version = path.getFileName().toString().replace("jei-1.21.1-neoforge-", "").replace(".jar", "");
                if (args.length >= 4 && (compareVersions(version, args[2]) < 0 || compareVersions(version, args[3]) > 0)) continue;
                try (var jar = new ZipFile(path.toFile())) {
                    var nodes = new HashMap<String, ClassNode>();
                    var object = new ClassNode(); new ClassReader("java.lang.Object").accept(object, ClassReader.SKIP_CODE);
                    nodes.put(object.name, object);
                    for (var entry : Collections.list(jar.entries())) if (entry.getName().endsWith(".class") && entry.getName().startsWith("mezz/jei/")) {
                        var node = new ClassNode();
                        new ClassReader(jar.getInputStream(entry)).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                        nodes.put(node.name, node);
                    }
                    var controller = nodes.get("mezz/jei/gui/recipes/RecipeTransferButtonController");
                    var layout = nodes.get("mezz/jei/gui/recipes/RecipeLayoutWithButtons");
                    boolean legacyController = RouterMixinPlugin.signatureMatches(controller, true);
                    boolean legacyLayout = RouterMixinPlugin.signatureMatches(layout, true);
                    require(RouterMixinPlugin.signatureMatches(controller, legacyController) &&
                        !RouterMixinPlugin.signatureMatches(controller, !legacyController), version + " controller adapter mismatch");
                    require(RouterMixinPlugin.signatureMatches(layout, legacyLayout) &&
                        !RouterMixinPlugin.signatureMatches(layout, !legacyLayout), version + " layout adapter mismatch");
                    for (var type : types) require(nodes.containsKey(type), version + " missing class " + type);
                    for (var member : members) require(resolves(nodes, member, new HashSet<>()), version + " missing/incompatible member " + member);
                    // These members are invoked reflectively, so they are not in the compiled call sites.
                    boolean service = nodes.containsKey("mezz/jei/common/transfer/RecipeTransferService");
                    String owner = "mezz/jei/common/transfer/" + (service ? "RecipeTransferService" : "RecipeTransferUtil");
                    String leading = service ? "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;" :
                        "Lmezz/jei/api/recipe/transfer/IRecipeTransferManager;Lnet/minecraft/world/inventory/AbstractContainerMenu;";
                    String recipe = leading + "Lmezz/jei/api/gui/IRecipeLayoutDrawable;Lnet/minecraft/world/entity/player/Player;";
                    require(resolves(nodes, new Member(owner, "getTransferRecipeError", "(" + recipe + ")Ljava/util/Optional;", false, !service), new HashSet<>()), version + " error API mismatch");
                    require(resolves(nodes, new Member(owner, "transferRecipe", "(" + recipe + "Z)Z", false, !service), new HashSet<>()), version + " transfer API mismatch");
                    if (service) require(resolves(nodes, new Member(owner, "<init>", "(Lmezz/jei/api/recipe/transfer/IRecipeTransferManager;)V", false, false), new HashSet<>()), version + " service constructor mismatch");
                    String packet = "mezz/jei/common/network/packets/PacketRecipeTransfer" + (service ? "WithResult" : "");
                    require(resolves(nodes, new Member(packet, "TYPE", "Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$Type;", true, true), new HashSet<>()), version + " packet channel mismatch");
                    System.out.println("PASS: " + version + " controller=" + (legacyController ? "legacy" : "modern") +
                        " layout=" + (legacyLayout ? "legacy" : "modern") + " transfer=" + (service ? "service" : "util") +
                        " JEI types=" + types.size() + " members=" + members.size());
                    count++;
                } catch (AssertionError | Exception e) {
                    failures.add(version + ": " + e);
                    System.out.println("FAIL: " + version + ": " + e.getMessage());
                }
            }
        }
        require(count > 0, "No compatible JEI artifacts checked");
        System.out.println("RESULT: compatible=" + count + " incompatible=" + failures.size());
        if (args.length >= 5) require(count + failures.size() == Integer.parseInt(args[4]), "JEI artifact set incomplete: " + (count + failures.size()));
        require(failures.isEmpty(), "Incompatible JEI artifacts: " + failures.size());
        System.out.println("PASS: ALL " + count + " JEI ARTIFACTS; adapter selection uses interfaces, not versions");
    }
    private static void member(String owner, String name, String desc, boolean field, boolean isStatic) {
        type(owner); descriptor(desc);
        if (owner.startsWith("mezz/jei/")) members.add(new Member(owner, name, desc, field, isStatic));
    }
    private static void type(String name) { if (name != null && name.startsWith("mezz/jei/")) types.add(name); }
    private static void descriptor(String desc) {
        var t = Type.getType(desc);
        if (t.getSort() == Type.METHOD) { descriptor(t.getReturnType().getDescriptor()); for (var arg : t.getArgumentTypes()) descriptor(arg.getDescriptor()); }
        else if (t.getSort() == Type.ARRAY) descriptor(t.getElementType().getDescriptor());
        else if (t.getSort() == Type.OBJECT) type(t.getInternalName());
    }
    private static boolean resolves(Map<String, ClassNode> nodes, Member member, Set<String> seen) {
        if (!seen.add(member.owner)) return false;
        var node = nodes.get(member.owner);
        if (node == null) return false;
        int access = member.field ? node.fields.stream().filter(f -> f.name.equals(member.name) && f.desc.equals(member.desc))
            .mapToInt(f -> f.access).findFirst().orElse(-1) : node.methods.stream().filter(m -> m.name.equals(member.name) && m.desc.equals(member.desc))
            .mapToInt(m -> m.access).findFirst().orElse(-1);
        if (access >= 0) return (access & Opcodes.ACC_PUBLIC) != 0 && ((access & Opcodes.ACC_STATIC) != 0) == member.isStatic;
        var parents = new ArrayList<>(node.interfaces); if (node.superName != null) parents.add(node.superName);
        if (member.name.equals("<init>")) return false;
        for (var parent : parents) if (resolves(nodes, new Member(parent, member.name, member.desc, member.field, member.isStatic), seen)) return true;
        return false;
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static int compareVersions(String a, String b) {
        var left = a.split("\\."); var right = b.split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int result = Integer.compare(i < left.length ? Integer.parseInt(left[i]) : 0, i < right.length ? Integer.parseInt(right[i]) : 0);
            if (result != 0) return result;
        }
        return 0;
    }
}
