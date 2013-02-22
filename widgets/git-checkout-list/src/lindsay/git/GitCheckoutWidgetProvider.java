package lindsay.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.channels.FileChannel;

import lindsay.git.GitRepoInfo.RepoCheckoutInfo;

import org.eclipse.jgit.api.Git;

import com.aviarc.core.Aviarc;
import com.aviarc.core.application.Application;
import com.aviarc.core.components.AviarcURN;
import com.aviarc.core.diagnostics.ResourceCompilationInfo;
import com.aviarc.core.diagnostics.SimpleResourceDiagnostic;
import com.aviarc.core.resource.ResourceDirectory;
import com.aviarc.core.resource.ResourceFile;
import com.aviarc.core.widget.WidgetProviderCreationRecord;
import com.aviarc.core.widget.WidgetProviderFactory;
import com.aviarc.framework.toronto.widget.BrokenWidgetProviderCreationRecord;

public class GitCheckoutWidgetProvider implements WidgetProviderFactory {

    //private Aviarc _aviarc;
    private Application _app;

    public void initialize(Aviarc aviarc, Application app) {
        // TODO Auto-generated method stub
        //_aviarc = aviarc;
        _app = app;
    }

    public WidgetProviderCreationRecord createWidgetProvider(AviarcURN urn, ResourceDirectory dir, ClassLoader classLoader) {
        // TODO Auto-generated method stub
        ResourceCompilationInfo compileInfo = new ResourceCompilationInfo();
        
        ResourceFile definitionFile = dir.getFile("definition.xml");
        GitRepoInfo gitInfo = GitRepoInfo.createFrom(definitionFile);
        
        
        for (RepoCheckoutInfo repoInfo : gitInfo.getRepoCheckoutInfo()) {
            cloneGitRepo(urn, dir, compileInfo, repoInfo.getRemoteURI(), repoInfo.getAppPath());
        }
        
        
        
        
        return new BrokenWidgetProviderCreationRecord(urn,dir, compileInfo);
                
    }

    private void cloneGitRepo(AviarcURN urn,
                              ResourceDirectory dir,
                              ResourceCompilationInfo compileInfo,
                              String remoteURI,
                              String appPath) {
        File tempLocation = _app.getTemporaryResourceManager().getNewTemporaryDirectory();
        
        ResourceDirectory metadataPath = _app.getMetadataDirectory().getDirectory(appPath);
        if (metadataPath.exists())  {                      
            if (alreadyClonedHere(metadataPath, remoteURI)) {
                return;
            }
        } 
        
        try {
            Git.cloneRepository() 
            .setURI(remoteURI)
            .setDirectory(tempLocation)
            .call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 
        
        
        metadataPath.create();
      
        
        
        
        String pathBase = tempLocation.getPath();
        System.out.println("pathBase: " + pathBase);
        if (!pathBase.endsWith("/")) {
            pathBase = pathBase + "/";
        }
        copyFilesToMetadata(pathBase, tempLocation, metadataPath, metadataPath);
        
        // We add this error in so that the user knows to recompile
        compileInfo.add(SimpleResourceDiagnostic.newWidgetError("Git checkout", "Git repository " + remoteURI + " cloned to " + appPath + ". Recompile required."));
        
        recordCloning(metadataPath, remoteURI);
    }

    private boolean alreadyClonedHere(ResourceDirectory metadataPath, String remoteURI) {
        // Look for '.clonedRepos' file
        ResourceFile cloneRecord = metadataPath.getFile(".clonedRepos");
        if (!cloneRecord.exists()) {
            return false;
        }
        
        InputStream is = cloneRecord.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        
        try {
            String line = reader.readLine();
            while (line != null) {
                if (remoteURI.equals(line)) {
                    return true;
                }
                line = reader.readLine();
            }                     
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                is.close();
                reader.close();
            } catch (Exception ignore) {}
        }
        return false;
    }
    
    private void recordCloning(ResourceDirectory metadataPath, String remoteURI) {
     // Look for '.clonedRepos' file
        ResourceFile cloneRecord = metadataPath.getFile(".clonedRepos");
        if (!cloneRecord.exists()) {
            cloneRecord.create();
        }
        String contents = cloneRecord.readUTF8();
        contents = contents.concat(remoteURI + "\n");
        cloneRecord.writeUTF8(contents);              
    }

    private void copyFilesToMetadata(String pathBase, File tempLocation, ResourceDirectory dir, ResourceDirectory root) {
        File[] files = tempLocation.listFiles();
        String path, relativePath;
        for (File f : files) {
            
            path = f.getPath();
            relativePath = path.substring(pathBase.length());
            System.out.println("Path:" + path + " relative path: " + relativePath );
            
            if (f.isFile()) {
                ResourceFile file = root.createFile(relativePath);
                // copy t
                System.out.println("Copying " + f.getPath() + " to " + file.getPath());                 
                try {
                    copyCompletely(new FileInputStream(f), file.getOutputStream());
                } catch (Exception e) {
                    System.out.println("Failed to copy: " + e.getMessage());
                }           
            } else {       
                // ignore ".git"
                if (".git".equals(relativePath)) {
                    System.out.println("Ignoring: " + relativePath);
                    continue;
                }
                ResourceDirectory newDir;
                newDir = root.getDirectory(relativePath);
                if (!newDir.exists()) {
                    System.out.println("Creating folder: " + relativePath);
                    newDir.create();
                }
                copyFilesToMetadata(pathBase, f, newDir, root); 
                
            }
        }
    }
    
    public static void copyCompletely(InputStream input, OutputStream output) throws IOException {
        // if both are file streams, use channel IO
        if ((output instanceof FileOutputStream) && (input instanceof FileInputStream)) {
          try {
            FileChannel target = ((FileOutputStream) output).getChannel();
            FileChannel source = ((FileInputStream) input).getChannel();
            
            source.transferTo(0, Integer.MAX_VALUE, target);

            source.close();
            target.close();

            return;
          } catch (Exception e) { /* failover to byte stream version */
          }
        }

        byte[] buf = new byte[8192];
        while (true) {
          int length = input.read(buf);
          if (length < 0)
            break;
          output.write(buf, 0, length);
        }

        try {
          input.close();
        } catch (IOException ignore) {
        }
        try {
          output.close();
        } catch (IOException ignore) {
        }
      }

}





































