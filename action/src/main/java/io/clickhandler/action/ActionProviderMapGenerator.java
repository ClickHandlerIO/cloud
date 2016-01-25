package io.clickhandler.action;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 *
 */
public class ActionProviderMapGenerator {
    public static void main(String[] args) throws IOException {
        new ActionProviderMapGenerator().run();
    }

    public void run() throws IOException {
        final Reflections reflections = new Reflections("engine", "action");

        final List<List<Class<? extends Action>>> groups = Lists.newArrayList();
        groups.add(Lists.newArrayList());

        reflections.getSubTypesOf(Action.class).stream().filter(
                cls -> !Modifier.isAbstract(cls.getModifiers()) && !Modifier.isInterface(cls.getModifiers())
        ).forEach(cls -> {
            List<Class<? extends Action>> current = groups.get(groups.size() - 1);
            if (current.size() == 254) {
                current = Lists.newArrayList();
                groups.add(current);
            }
            current.add(cls);
        });

        if (groups.get(0).isEmpty()) {
            throw new RuntimeException("No Actions were found");
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("package action;\n\n");
        sb.append("import io.clickhandler.action.Action;\n\n");
        sb.append("import javax.inject.Inject;\n");
        sb.append("import javax.inject.Provider;\n");
        sb.append("import javax.inject.Singleton;\n");
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.Map;\n\n");

        sb.append("@Singleton\n");
        sb.append("public final class ActionProviderMapImpl implements io.clickhandler.action.ActionProviderMap {\n");

        sb.append("\tprivate final HashMap<Class, Provider<? extends Action>> map = new HashMap<>();\n\n");

        sb.append("\t@Inject\n");
        sb.append("\tpublic ActionProviderMapImpl(");
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("_").append(i).append(" __").append(i);
        }
        sb.append(") {\n");

        for (int i = 0; i < groups.size(); i++) {
            sb.append("\t\tmap.putAll(__").append(i).append(".get());\n");
        }

        sb.append("\t}\n\n");

        sb.append("\tpublic Map<Class, Provider<? extends Action>> get() { return this.map; }\n\n");

        for (int i = 0; i < groups.size(); i++) {
            final List<Class<? extends Action>> group = groups.get(i);
            sb.append("\tpublic static class _").append(i).append(" implements Provider<Map<Class, Provider<? extends Action>>>").append(" {\n");
            sb.append("\t\tprivate final Map<Class, Provider<? extends Action>> map = new HashMap<>();\n\n");

            sb.append("\t\t@Inject\n");
            sb.append("\t\tpublic ").append("_").append(i).append("(");
            for (int g = 0; g < group.size(); g++) {
                final Class<? extends Action> actionClass = group.get(g);
                if (g > 0) {
                    sb.append(", ");
                }
                sb.append("Provider<").append(actionClass.getCanonicalName()).append("> _").append(g);
            }
            sb.append(") {\n");

            for (int g = 0; g < group.size(); g++) {
                final Class<? extends Action> actionClass = group.get(g);
                sb.append("\t\t\tmap.put(").append(actionClass.getCanonicalName()).append(".class, _").append(g).append(");\n");
            }

            sb.append("\t\t}\n\n");

            sb.append("\t\tpublic Map<Class, Provider<? extends Action>> get() { return this.map; }\n");

            sb.append("\t}\n\n");
        }

        sb.setLength(sb.length() - 1);
        sb.append("}\n");

        Files.write(sb.toString().getBytes(Charsets.UTF_8), new File("server/src/main/java/action/ActionProviderMapImpl.java"));
    }
}
