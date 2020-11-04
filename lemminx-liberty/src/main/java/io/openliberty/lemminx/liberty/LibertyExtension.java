package io.openliberty.lemminx.liberty;

import org.eclipse.lemminx.services.extensions.ICompletionParticipant;
import org.eclipse.lemminx.services.extensions.IDocumentLinkParticipant;
import org.eclipse.lemminx.services.extensions.IHoverParticipant;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.diagnostics.IDiagnosticsParticipant;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lemminx.services.extensions.save.ISaveContext.SaveContextType;
import org.eclipse.lemminx.uriresolver.URIResolverExtension;
import org.eclipse.lsp4j.InitializeParams;

import java.util.logging.Logger;

import io.openliberty.lemminx.liberty.services.SettingsService;

public class LibertyExtension implements IXMLExtension {

    private static final Logger LOGGER = Logger.getLogger(LibertyExtension.class.getName());

    private URIResolverExtension xsdResolver;
    private ICompletionParticipant completionParticipant;
    private IHoverParticipant hoverParticipant;
    private IDiagnosticsParticipant diagnosticsParticipant;
    private IDocumentLinkParticipant documentLinkParticipant;

    @Override
    public void start(InitializeParams initializeParams, XMLExtensionsRegistry xmlExtensionsRegistry) {
        xsdResolver = new LibertyXSDURIResolver();
        xmlExtensionsRegistry.getResolverExtensionManager().registerResolver(xsdResolver);

        completionParticipant = new LibertyCompletionParticipant();
        xmlExtensionsRegistry.registerCompletionParticipant(completionParticipant);

        hoverParticipant = new LibertyHoverParticipant();
        xmlExtensionsRegistry.registerHoverParticipant(hoverParticipant);

        diagnosticsParticipant = new LibertyDiagnosticParticipant();
        xmlExtensionsRegistry.registerDiagnosticsParticipant(diagnosticsParticipant);

        documentLinkParticipant = new LibertyDocumentLinkParticipant();
        xmlExtensionsRegistry.registerDocumentLinkParticipant(documentLinkParticipant);
    }

    @Override
    public void stop(XMLExtensionsRegistry xmlExtensionsRegistry) {
        xmlExtensionsRegistry.getResolverExtensionManager().unregisterResolver(xsdResolver);
        xmlExtensionsRegistry.unregisterCompletionParticipant(completionParticipant);
        xmlExtensionsRegistry.unregisterHoverParticipant(hoverParticipant);
        xmlExtensionsRegistry.unregisterDiagnosticsParticipant(diagnosticsParticipant);
        xmlExtensionsRegistry.unregisterDocumentLinkParticipant(documentLinkParticipant);
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
}
