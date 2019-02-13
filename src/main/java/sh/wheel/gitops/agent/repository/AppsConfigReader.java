package sh.wheel.gitops.agent.repository;

import sh.wheel.gitops.agent.config.AppConfig;
import sh.wheel.gitops.agent.config.NamespaceConfig;
import sh.wheel.gitops.agent.model.App;
import sh.wheel.gitops.agent.model.BuildConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AppsConfigReader {

    public static final String APP_CONFIG = "config.yaml";
    public static final String BUILD_CONFIG_DIR = "build/";
    public static final String NAMESPACE_CONFIG_DIR = "namespace/";
    public static final String TEMPLATE_DIR = "template/";
    private GenericYamlDeserializer deserializer = new GenericYamlDeserializer();

    public List<App> readAllApps(Path appsDir) throws IOException {
        List<Path> appDirs = Files.list(appsDir).collect(Collectors.toList());
        ArrayList<App> apps = new ArrayList<>();
        for (Path appDir : appDirs) {
            try {
                App app = readApp(appDir);
                apps.add(app);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return apps;
    }

    public App readApp(Path appDir) throws IOException {
        AppConfig appConfig = deserializer.deserialize(appDir.resolve(APP_CONFIG), AppConfig.class);
        List<BuildConfig> buildConfigs = deserializer.readDirectory(appDir.resolve(BUILD_CONFIG_DIR), BuildConfig.class);
        List<NamespaceConfig> namespaceConfigs = deserializer.readDirectory(appDir.resolve(NAMESPACE_CONFIG_DIR), NamespaceConfig.class);
        return createApp(appConfig, buildConfigs, namespaceConfigs, appDir);
    }

    private App createApp(AppConfig appConfig, List<BuildConfig> buildConfigs, List<NamespaceConfig> namespaceConfigs, Path appDir) throws IOException {
        for (NamespaceConfig namespaceConfig : namespaceConfigs) {
            String templateFile = namespaceConfig.getTemplateFile();
            Path templateFilePath = appDir.resolve(TEMPLATE_DIR).resolve(templateFile);
            String template = new String(Files.readAllBytes(templateFilePath));
            namespaceConfig.setTemplate(template);
        }
        return new App(appConfig, buildConfigs, namespaceConfigs, appDir);
    }

}
