package io.openliberty.lemminx.liberty.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.maven.model.Plugin;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.lemminx.dom.DOMDocument;

import io.openliberty.lemminx.liberty.LibertyExtension;

public class LibertyUtils {

    private static final Logger LOGGER = Logger.getLogger(LibertyUtils.class.getName());

    protected static String version;

    private LibertyUtils() {
    }

    public static boolean isServerXMLFile(String filePath) {
        return filePath.endsWith("/" + LibertyConstants.SERVER_XML);
    }

    public static boolean isServerXMLFile(DOMDocument file) {
        return file.getDocumentURI().endsWith("/" + LibertyConstants.SERVER_XML);
    }

    public static boolean isPomXMLFile(String filePath) {
        return filePath.endsWith("/" + LibertyConstants.POM_XML);
    }

    public static boolean isPomXMLFile(DOMDocument file) {
        return file.getDocumentURI().endsWith("/" + LibertyConstants.POM_XML);
    }

    /**
     * Call the Liberty Maven Plugin to get the Liberty version from the pom.xml
     * 
     * @param pomXML path of the pom.xml file
     * @return liberty version
     */
    public static String getVersion(Path pomXML) {
        try {

            File pomXmlFile = pomXML.toFile();
            if (!pomXmlFile.exists()) {
                return version;
            }

            Plugin plugin = getPlugin("io.openliberty.tools", "liberty-maven-plugin");
            InvocationRequest mavenReq = new DefaultInvocationRequest();
            mavenReq.setPomFile(pomXmlFile);
            mavenReq.setGoals(Collections.singletonList("liberty:version"));
            Invoker invoker = new DefaultInvoker();
            // note: default invoker is trying to resolve maven home from maven.home or
            // M2_HOME environment variable
            // TODO: ensure we catch the case where the maven home cannot be resolved and
            // return the correct error
            InvocationOutputHandler outputHandler = new InvocationOutputHandler() {

                @Override
                public void consumeLine(String line) throws IOException {
                    if (line.contains("Liberty version:")) {
                        version = line;
                    }
                    if (line.contains("Could not find goal 'version'")) {
                        // indicates plugin is not supported
                        LOGGER.warning(
                                "The Liberty Maven PLugin does not contain the 'version' goal. Please update your Liberty Maven Plugin version.");
                    }
                }
            };

            invoker.setOutputHandler(outputHandler);
            InvocationResult mavenResult = invoker.execute(mavenReq);
            if (mavenResult.getExitCode() != 0) {
                LOGGER.warning("Unable to get version from Liberty Maven Plugin");
            }
        } catch (MavenInvocationException e) {
            LOGGER.warning("Unable to get version from Liberty Maven Plugin");
            e.printStackTrace();
        }
        if (version != null) {
            version = version.substring(version.indexOf(":") + 1).trim();
        }

        return version;
    }

    /**
     * Given a server XML URI, check the map of build files for a liberty version
     * Attempts to resolve path to pom.xml from the server.xml
     * @param serverXMLUri server xml uri as a string
     * @return liberty version
     */
    public static String getVersionFromMap(String serverXMLUri) {
        try {
            Map<Path, String> buildFilesMap = LibertyExtension.getBuildFilesMap();
            // TODO: problem if server.xml is not in the same relative location to the pom.xml
            // ie. if they specified a different server.xml location in their pom.xml

            // look for configFile or serverXMLFile configuration in their pom.xml?
            String pomXmlUri = serverXMLUri.substring(0, serverXMLUri.lastIndexOf("src/"))
                    + "pom.xml";
            URI pomUri = new URI(pomXmlUri);
            Path pomPath = Paths.get(pomUri);
            String version = buildFilesMap.get(pomPath);
            if (version != null ) {
                // libertyVersion = version;
                return version;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Refreshes the [pom, libertyVersion] map.
     * Only updates the map if the libertyVersion has changed.
     * 
     * @param pomFile pom.xml file
     */
    public static void refreshMap(DOMDocument pomFile) {
        try {
            Map<Path, String> buildFilesMap = LibertyExtension.getBuildFilesMap();
            URI pomUri = new URI(pomFile.getDocumentURI());
            Path pomPath = Paths.get(pomUri);
            String existingVersion = buildFilesMap.get(pomPath);
            if (existingVersion != null) {
                String newLibertyVersion = getVersion(pomPath);
                if (newLibertyVersion != existingVersion) {
                    buildFilesMap.replace(pomPath, existingVersion, newLibertyVersion);
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Given the groupId and artifactId get the corresponding plugin
     * 
     * @param groupId
     * @param artifactId
     * @return Plugin
     */
    protected Plugin getPlugin(String groupId, String artifactId) {
        Plugin plugin = project.getPlugin(groupId + ":" + artifactId);
        if (plugin == null) {
            plugin = plugin(groupId(groupId), artifactId(artifactId), version("RELEASE"));
        }
        return plugin;
    }

}
