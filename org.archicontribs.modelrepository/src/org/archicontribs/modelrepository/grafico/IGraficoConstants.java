/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;


/**
 * Grafico Constants
 * 
 * @author Phillip Beauvoir
 */
public interface IGraficoConstants {
    /**
     * Filename to use for serialization of folder elements
     */
    String FOLDER_XML = "folder.xml"; //$NON-NLS-1$
    
    /**
     * Name of folder for images
     */
    String IMAGES_FOLDER = "images"; //$NON-NLS-1$
    
    /**
     * Name of folder for model
     */
    String MODEL_FOLDER = "model"; //$NON-NLS-1$

    /**
     * File name of user name/password for each git repo
     */
    String REPO_CREDENTIALS_FILE = "credentials"; //$NON-NLS-1$
    
    /**
     * File name of user name/password for Proxy Server
     */
    String PROXY_CREDENTIALS_FILE = "proxy_credentials"; //$NON-NLS-1$
}
