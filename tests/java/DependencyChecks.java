import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Audits the router's actual dependency references in each official pack combination. */
public final class DependencyChecks {
    private record Member(String owner, String name, String desc, boolean field, boolean isStatic) {}
    private static final Set<Member> members = new HashSet<>();
    private static final Set<String> types = new HashSet<>();
    private static boolean dependency(String n) {
        return n != null && (n.startsWith("org/cyclops/") || n.startsWith("appeng/") ||
            n.startsWith("de/mari_023/") || n.startsWith("top/theillusivec4/") || n.startsWith("net/neoforged/"));
    }
    public static void main(String[] args) throws Exception {
        try (var classes = Files.walk(Path.of(args[0]))) {
            for (var p : classes.filter(p -> p.toString().endsWith(".class")).toList()) {
                var n = new ClassNode(); new ClassReader(Files.readAllBytes(p)).accept(n, 0);
                type(n.superName); n.interfaces.forEach(DependencyChecks::type);
                for (var f : n.fields) descriptor(f.desc);
                for (var m : n.methods) {
                    descriptor(m.desc);
                    for (var i : m.instructions) {
                        if (i instanceof MethodInsnNode v) member(v.owner, v.name, v.desc, false, v.getOpcode() == Opcodes.INVOKESTATIC);
                        if (i instanceof FieldInsnNode v) member(v.owner, v.name, v.desc, true, v.getOpcode() == Opcodes.GETSTATIC || v.getOpcode() == Opcodes.PUTSTATIC);
                        if (i instanceof TypeInsnNode v) type(v.desc);
                        if (i instanceof LdcInsnNode v && v.cst instanceof Type t) descriptor(t.getDescriptor());
                        if (i instanceof InvokeDynamicInsnNode v) {
                            descriptor(v.desc);
                            for (var a : v.bsmArgs) {
                                if (a instanceof Handle h) member(h.getOwner(), h.getName(), h.getDesc(), h.getTag() <= 4,
                                    h.getTag() == Opcodes.H_INVOKESTATIC || h.getTag() == Opcodes.H_GETSTATIC || h.getTag() == Opcodes.H_PUTSTATIC);
                                if (a instanceof Type t) descriptor(t.getDescriptor());
                            }
                        }
                    }
                }
            }
        }
        int failures = 0, total = 0;
        for (var list : Files.list(Path.of(args[1])).filter(p -> p.toString().endsWith(".txt")).sorted().toList()) {
            var nodes = new HashMap<String, ClassNode>();
            for (var path : Files.readAllLines(list)) if (!path.isBlank()) {
                try (var input = Files.newInputStream(Path.of(path))) { loadJar(input, nodes); }
            }
            var errors = new ArrayList<String>();
            for (var name : types) if (node(nodes, name) == null) errors.add("missing type " + name);
            for (var m : members) if (!resolves(nodes, m, new HashSet<>())) errors.add("missing member " + m);
            total++;
            if (errors.isEmpty()) System.out.println("PASS: " + list.getFileName() + " types=" + types.size() + " members=" + members.size());
            else { failures++; System.out.println("FAIL: " + list.getFileName() + " " + String.join("; ", errors)); }
        }
        System.out.println("RESULT: combinations=" + total + " failures=" + failures);
        if (total == 0 || failures > 0) throw new AssertionError("Dependency audit failed");
    }
    private static void loadJar(InputStream input, Map<String, ClassNode> nodes) throws IOException {
        try (var jar = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = jar.getNextEntry()) != null) {
                if (entry.getName().endsWith(".jar")) loadJar(new ByteArrayInputStream(jar.readAllBytes()), nodes);
                else if (entry.getName().endsWith(".class")) {
                    var n = new ClassNode(); new ClassReader(jar.readAllBytes()).accept(n, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                    nodes.put(n.name, n);
                }
            }
        }
    }
    private static ClassNode node(Map<String, ClassNode> nodes, String name) {
        if (nodes.containsKey(name)) return nodes.get(name);
        if (name.startsWith("java/") || name.startsWith("net/minecraft/")) {
            try { var n = new ClassNode(); new ClassReader(name.replace('/', '.')).accept(n, ClassReader.SKIP_CODE); nodes.put(name, n); return n; }
            catch (IOException ignored) { }
        }
        return null;
    }
    private static boolean resolves(Map<String, ClassNode> nodes, Member m, Set<String> seen) {
        if (!seen.add(m.owner)) return false;
        var n = node(nodes, m.owner); if (n == null) return false;
        int access = m.field ? n.fields.stream().filter(f -> f.name.equals(m.name) && f.desc.equals(m.desc)).mapToInt(f -> f.access).findFirst().orElse(-1)
            : n.methods.stream().filter(f -> f.name.equals(m.name) && f.desc.equals(m.desc)).mapToInt(f -> f.access).findFirst().orElse(-1);
        if (access >= 0) return (access & Opcodes.ACC_PUBLIC) != 0 && ((access & Opcodes.ACC_STATIC) != 0) == m.isStatic;
        if (m.name.equals("<init>")) return false;
        var parents = new ArrayList<>(n.interfaces); if (n.superName != null) parents.add(n.superName);
        for (var parent : parents) if (resolves(nodes, new Member(parent, m.name, m.desc, m.field, m.isStatic), seen)) return true;
        return false;
    }
    private static void member(String owner, String name, String desc, boolean field, boolean isStatic) {
        type(owner); descriptor(desc); if (dependency(owner)) members.add(new Member(owner, name, desc, field, isStatic));
    }
    private static void type(String name) { if (dependency(name)) types.add(name); }
    private static void descriptor(String desc) {
        var t = Type.getType(desc);
        if (t.getSort() == Type.METHOD) { descriptor(t.getReturnType().getDescriptor()); for (var a : t.getArgumentTypes()) descriptor(a.getDescriptor()); }
        else if (t.getSort() == Type.ARRAY) descriptor(t.getElementType().getDescriptor());
        else if (t.getSort() == Type.OBJECT) type(t.getInternalName());
    }
}
