/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.ProxyAuthenticater;
import org.archicontribs.modelrepository.authentication.SimpleCredentialsStorage;
import org.archicontribs.modelrepository.dialogs.CloneInputDialog;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.EmptyProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Clone a model
 */
public class CloneModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;
	
	// Set this to true to store user name and password in a simple encrypted file called "credentials" in the repo's .git folder
	// It's not the most secure algorithm so you have been warned!
    public CloneModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_CLONE_16));
        setText(Messages.CloneModelAction_0);
        setToolTipText(Messages.CloneModelAction_1);
    }

    @Override
    public void run() {
        CloneInputDialog dialog = new CloneInputDialog(fWindow.getShell());
        if(dialog.open() != Window.OK) {
            return;
        }
    	
        final String repoURL = dialog.getURL();
        final String userName = dialog.getUsername();
        final String userPassword = dialog.getPassword();
        
        if(!StringUtils.isSet(repoURL) && !StringUtils.isSet(userName) && !StringUtils.isSet(userPassword)) {
            return;
        }
        
        File localGitFolder = new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(),
                GraficoUtils.getLocalGitFolderName(repoURL));
        
        // Folder is not empty
        if(localGitFolder.exists() && localGitFolder.isDirectory() && localGitFolder.list().length > 0) {
            MessageDialog.openError(fWindow.getShell(),
                    Messages.CloneModelAction_0,
                    Messages.CloneModelAction_2 + " " + localGitFolder.getAbsolutePath()); //$NON-NLS-1$

            return;
        }
        
        class Progress extends EmptyProgressMonitor implements IRunnableWithProgress {
            private IProgressMonitor monitor;

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    this.monitor = monitor;
                    
                    monitor.beginTask(Messages.CloneModelAction_4, IProgressMonitor.UNKNOWN);
                    
                    // Proxy check
                    ProxyAuthenticater.update();
                    
                    // Clone
                    GraficoUtils.cloneModel(localGitFolder, repoURL, userName, userPassword, this);
                    
                    monitor.subTask(Messages.CloneModelAction_5);
                    
                    // Load it from the Grafico files if we can
                    IArchimateModel model = GraficoUtils.loadModelFromGraficoFiles(localGitFolder, fWindow.getShell());
                    
                    // We have it
                    if(model != null) {
                        // Open it first. This will do the necessary checks and add a command stack and an archive manager so we can save it
                        IEditorModelManager.INSTANCE.openModel(model);
                        
                        // Save it as the temp file
                        IEditorModelManager.INSTANCE.saveModel(model);
                    }
                    // We couldn't load it from Grafico so create a new blank model
                    else {
                        // New one. This will open in the tree
                        model = IEditorModelManager.INSTANCE.createNewModel();
                        model.setFile(GraficoUtils.getModelFileName(localGitFolder));
                        
                        // And Save it
                        IEditorModelManager.INSTANCE.saveModel(model);
                    }
                    
                    // Store repo credentials if option is set
                    if(ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_STORE_REPO_CREDENTIALS)) {
                        SimpleCredentialsStorage sc = new SimpleCredentialsStorage(new File(localGitFolder, ".git"), IGraficoConstants.REPO_CREDENTIALS_FILE); //$NON-NLS-1$
                        sc.store(userName, userPassword);
                    }
                }
                catch(GitAPIException | IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
                    ex.printStackTrace();
                    MessageDialog.openError(fWindow.getShell(),
                            Messages.CloneModelAction_0,
                            Messages.CloneModelAction_3 + " " + //$NON-NLS-1$
                            ex.getMessage());
                }
                finally {
                    monitor.done();
                }
            }

            @Override
            public void beginTask(String title, int totalWork) {
                monitor.subTask(title);
            }

            @Override
            public boolean isCancelled() {
                return monitor.isCanceled();
            }
        }
        
        Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    ProgressMonitorDialog pmDialog = new ProgressMonitorDialog(fWindow.getShell());
                    pmDialog.run(false, true, new Progress());
                }
                catch(InvocationTargetException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
