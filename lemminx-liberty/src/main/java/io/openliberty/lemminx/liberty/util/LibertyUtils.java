package io.openliberty.lemminx.liberty.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

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
        File pomXmlFile = pomXML.toFile();
        if (!pomXmlFile.exists()) {
            return version;
        }

        try {
            Runtime rt = Runtime.getRuntime();
            // TODO: should we check for maven wrapper and use that if set?
            Process process = rt.exec("mvn io.openliberty.tools:liberty-maven-plugin:version -f \"" + pomXML + "\"");
            int exitValue = process.waitFor();
            LOGGER.info("value: " + exitValue);
            if (exitValue == 0) {
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while ((line = in.readLine()) != null) {
                    if (line.contains("Liberty version:")) {
                        version = line;
                    }
                    if (line.contains("Could not find goal 'version'")) {
                        // indicates plugin is not supported
                        LOGGER.warning(
                                "The Liberty Maven Plugin does not contain the 'version' goal. Please update your Liberty Maven Plugin version to 3.3-M5-SNAPSHOT or higher.");
                    }
                }
            } else {
                LOGGER.warning("Unable to execute maven command. Using Liberty default version of Liberty: "
                        + LibertyConstants.DEFAULT_LIBERTY_VERSION);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.warning("Unable to execute maven command. Using Liberty default version of Liberty: "
                    + LibertyConstants.DEFAULT_LIBERTY_VERSION + "; " + e);
        }
        if (version != null) {
            version = version.substring(version.indexOf(":") + 1).trim();
        }
        return version;
    }

    /**
     * Given a server XML URI, check the map of build files for a liberty version
     * Attempts to resolve path to pom.xml from the server.xml
     * 
     * @param serverXMLUri server xml uri as a string
     * @return liberty version
     */
    public static String getVersionFromMap(String serverXMLUri) {
        try {
            Map<Path, String> buildFilesMap = LibertyExtension.getBuildFilesMap();
            // TODO: problem if server.xml is not in the same relative location to the
            // pom.xml
            // ie. if they specified a different server.xml location in their pom.xml

            // look for configFile or serverXMLFile configuration in their pom.xml?
            String pomXmlUri = serverXMLUri.substring(0, serverXMLUri.lastIndexOf("src/")) + "pom.xml";
            URI pomUri = new URI(pomXmlUri);
            Path pomPath = Paths.get(pomUri);
            String version = buildFilesMap.get(pomPath);
            if (version != null) {
                return version;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Refreshes the [pom, libertyVersion] map. Only updates the map if the
     * libertyVersion has changed.
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
}
