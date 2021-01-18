package io.openliberty.lemminx.liberty.services;

import java.util.List;

import org.eclipse.lsp4j.WorkspaceFolder;

public class LibertyProjectsManager {

    private static final LibertyProjectsManager INSTANCE = new LibertyProjectsManager();

    private List<WorkspaceFolder> workspaceFolders;

    public static LibertyProjectsManager getInstance() {
        return INSTANCE;
    }

    private LibertyProjectsManager() {

    }

    public void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
        this.workspaceFolders = workspaceFolders;
    }

    public List<WorkspaceFolder> getWorkspaceFolders() {
        return this.workspaceFolders;
    }

    /**
     * Given a serverXML URI return the corresponding workspace folder URI
     * 
     * @param serverXMLUri
     * @return
     */
    public static String getWorkspaceFolder(String serverXMLUri) {
        for (WorkspaceFolder folder : LibertyProjectsManager.getInstance().getWorkspaceFolders()) {
            if (serverXMLUri.contains(folder.getUri())) {
                return folder.getUri();
            }
        }
        return null;
    }

}
