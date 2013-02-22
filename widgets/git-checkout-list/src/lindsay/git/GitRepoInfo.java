package lindsay.git;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.aviarc.core.resource.ResourceFile;

public class GitRepoInfo {
    public class RepoCheckoutInfo {
        private String _remoteURI;
        private String _appPath;

        public RepoCheckoutInfo(String remoteURI, String appPath) {
            _remoteURI = remoteURI;
            _appPath = appPath;
        }

        public String getRemoteURI() {
            return _remoteURI;
        }

        public String getAppPath() {
            return _appPath;
        }
    }

    private ArrayList<RepoCheckoutInfo> _repos;

    public GitRepoInfo() {
        _repos = new ArrayList<RepoCheckoutInfo>();
    }

    public void addRepoCheckout(String remoteURI, String appPath) {
        _repos.add(new RepoCheckoutInfo(remoteURI, appPath));
    }
    
    public List<RepoCheckoutInfo> getRepoCheckoutInfo() {
        return Collections.unmodifiableList(_repos);
    }

    public static GitRepoInfo createFrom(ResourceFile definitionFile) {
        DocumentBuilder dBuilder;
        try {
            dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        InputStream is = definitionFile.getInputStream();
        Document d;
        
        GitRepoInfo result = new GitRepoInfo();
        
        try {
            try {
                d = dBuilder.parse(is);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }            
        } finally {
            try {
                is.close();
            } catch (IOException ignore) {
            }
        }
        
        NodeList nl = d.getElementsByTagName("clone-git-repo");
        Element n;
        for (int i = 0; i < nl.getLength(); i++) {
            n = (Element)nl.item(i);
            result.addRepoCheckout(n.getAttribute("repo"), n.getAttribute("path"));
        }
        
        return result;
    }
}
