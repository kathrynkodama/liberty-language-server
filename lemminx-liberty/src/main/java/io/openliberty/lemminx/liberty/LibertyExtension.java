package io.openliberty.lemminx.liberty;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lemminx.services.extensions.ICompletionParticipant;
import org.eclipse.lemminx.services.extensions.IHoverParticipant;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.diagnostics.IDiagnosticsParticipant;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lemminx.services.extensions.save.ISaveContext.SaveContextType;
import org.eclipse.lemminx.uriresolver.URIResolverExtension;
import org.eclipse.lsp4j.InitializeParams;

import io.openliberty.lemminx.liberty.services.SettingsService;
import io.openliberty.lemminx.liberty.util.LibertyUtils;

public class LibertyExtension implements IXMLExtension {

    private static final Logger LOGGER = Logger.getLogger(LibertyExtension.class.getName());
    private static Map<Path, String> buildFilesMap;

    private URIResolverExtension xsdResolver;
    private ICompletionParticipant completionParticipant;
    private IHoverParticipant hoverParticipant;
    private IDiagnosticsParticipant diagnosticsParticipant;

    @Override
    public void start(InitializeParams initializeParams, XMLExtensionsRegistry xmlExtensionsRegistry) {
        xsdResolver = new LibertyXSDURIResolver();
        xmlExtensionsRegistry.getResolverExtensionManager().registerResolver(xsdResolver);

        // populate map
        buildFilesMap = new HashMap<Path, String>();
        populateVersionsMap(initializeParams.getRootUri());

        completionParticipant = new LibertyCompletionParticipant();
        xmlExtensionsRegistry.registerCompletionParticipant(completionParticipant);

        hoverParticipant = new LibertyHoverParticipant();
        xmlExtensionsRegistry.registerHoverParticipant(hoverParticipant);

        diagnosticsParticipant = new LibertyDiagnosticParticipant();
        xmlExtensionsRegistry.registerDiagnosticsParticipant(diagnosticsParticipant);
    }

    @Override
    public void stop(XMLExtensionsRegistry xmlExtensionsRegistry) {
        xmlExtensionsRegistry.getResolverExtensionManager().unregisterResolver(xsdResolver);
        xmlExtensionsRegistry.unregisterCompletionParticipant(completionParticipant);
        xmlExtensionsRegistry.unregisterHoverParticipant(hoverParticipant);
        xmlExtensionsRegistry.unregisterDiagnosticsParticipant(diagnosticsParticipant);
    }

    // Do save is called on startup with a Settings update
    // and any time the settings are updated.
    @Override
    public void doSave(ISaveContext saveContext) {
        // Only need to update settings if the save event was for settings
        // Not if an xml file was updated.
        if (saveContext.getType() == SaveContextType.SETTINGS) {
            Object xmlSettings = saveContext.getSettings();
            SettingsService.getInstance().updateLibertySettings(xmlSettings);
            LOGGER.fine("Liberty XML settings updated");
        }
    }

    public static Map<Path, String> getBuildFilesMap() {
        return buildFilesMap;
    }

    private void populateVersionsMap(String rootUriString) {
        try {
            URI rootUri = new URI(rootUriString);
            Path rootPath = Paths.get(rootUri);
            // searches for pom.xml
            // TODO: exclude directories like: target, build, etc.
            List<Path> buildFiles = Files.walk(rootPath)
                    .filter(p -> (Files.isRegularFile(p) && p.getFileName().endsWith("pom.xml")))
                    .collect(Collectors.toList());
            for (Path p : buildFiles) {
                String version = LibertyUtils.getVersion(p);
                buildFilesMap.put(p, version);
            }
        } catch (IOException | URISyntaxException e) {
            LOGGER.warning("Unable to get build files: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
