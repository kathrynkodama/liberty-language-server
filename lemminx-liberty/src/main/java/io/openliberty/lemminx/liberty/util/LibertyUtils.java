package io.openliberty.lemminx.liberty.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.openliberty.lemminx.liberty.services.LibertyProjectsManager;

public class LibertyUtils {

    private static final Logger LOGGER = Logger.getLogger(LibertyUtils.class.getName());

    private LibertyUtils() {
    }

    public static boolean isServerXMLFile(String filePath) {
        return filePath.endsWith("/" + LibertyConstants.SERVER_XML);
    }

    public static boolean isServerXMLFile(DOMDocument file) {
        return file.getDocumentURI().endsWith("/" + LibertyConstants.SERVER_XML);
    }

    public static boolean isLibertyDevMetadataFile(String filePath) {
        return filePath.endsWith("/" + LibertyConstants.DEV_METADATA_XML);
    }

    public static boolean isLibertyDevMetadataFile(DOMDocument file) {
        return file.getDocumentURI().endsWith("/" + LibertyConstants.DEV_METADATA_XML);
    }

    /**
     * Given a server xml uri find the associated workspace folder and search that
     * folder for the most recently edited file that matches the given name
     * 
     * @param serverXmlURI
     * @param filename
     * @return path to given file or null if could not be found
     */
    public static Path findFileInWorkspace(String serverXmlURI, String filename) {

        String workspaceFolderURI = LibertyProjectsManager.getWorkspaceFolder(serverXmlURI);
        if (workspaceFolderURI == null) {
            return null;
        }
        try {
            URI rootURI = new URI(workspaceFolderURI);
            Path rootPath = Paths.get(rootURI);
            List<Path> jarFiles = Files.walk(rootPath)
                    .filter(p -> (Files.isRegularFile(p) && p.getFileName().endsWith(filename)))
                    .collect(Collectors.toList());
            if (jarFiles.isEmpty()) {
                return null;
            }
            if (jarFiles.size() == 1) {
                return jarFiles.get(0);
            }
            Path lastModified = jarFiles.get(0);
            for (Path p : jarFiles) {
                if (lastModified.toFile().lastModified() < p.toFile().lastModified()) {
                    lastModified = p;
                }
            }
            return lastModified;
        } catch (IOException | URISyntaxException e) {
            LOGGER.warning("Could not find: " + filename + ": " + e.getMessage());
            return null;
        }
    }

    public static String getVersion(DOMDocument serverXML) {
        // LOGGER.info("----- get version for: " + serverXML.getDocumentURI());
        
        // find workspace folder this serverXML belongs to
        String workspaceFolderURI = LibertyProjectsManager.getWorkspaceFolder(serverXML.getDocumentURI());
        if (workspaceFolderURI == null) {
            return null;
        }
        // LOGGER.info("----- workspaceFolderURI: " + workspaceFolderURI);

        // search workspace folder for metadata file
        
        // File workspaceFolder = new File(workspaceFolderURI);
        // LOGGER.info("----- workspaceFolder File exists: " + workspaceFolder.exists());
        

        // get version from metadata file

        return null;

    }

    public static File[] findMetadataFile(File dir) {
        File[] matchingFiles = dir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.equals("liberty-dev-metadata.xml");
            }

        });
        LOGGER.info("matchingFiles: " + Arrays.toString(matchingFiles));
        return matchingFiles;

        // return null;
    }

    public static String getVersionFromMetadataFile(Path devMetadataXML) {
        File devMetadataXMLFile = devMetadataXML.toFile();
        if (!devMetadataXMLFile.exists()) {
            return null;
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(devMetadataXMLFile);

            doc.getDocumentElement().normalize();
            LOGGER.info("Root element: " + doc.getDocumentElement().getNodeName());

            NodeList nList = doc.getElementsByTagName("libertyVersion");
            for (int i = 0; i < nList.getLength(); i++) {
                LOGGER.info("value: " + nList.item(i));
            }


        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.warning("Unable to parse " + devMetadataXML + ": " + e.getMessage());
        }
       

        return null;
    }
}
