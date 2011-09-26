package fr.opensagres.maven.plugins;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

/**
 * Uploads files in the target directory to google code. This code is based on Jonathan Fuerth's ant-googlecode-task.
 *
 * @goal gcupload
 * @execute phase="deploy"
 */
public class GoogleCodeUploadMojo extends AbstractMojo {

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
	private MavenProject project;

    /**
     * Access to settings.
     *
     * @parameter expression="${settings}"
     * @readonly
     */
    private Settings settings;

    /**
     * The server id (corresponding to a server's id in settings.xml).
     *
     * @parameter default-value="code.google.com"
     */
    private String serverId;

    /**
     * The googlecode username
     */
    private String userName;

    /**
     * The googlecode password
     */
    private String password;


  


    /**
     * Google Code project name to upload to.
     *
     * @parameter 
     */
    private String projectName;


    /**
     * Upload descriptors. Each upload descriptor element may have the following subelements: <ul>
     * <li>classifier:
     * a classifier of the maven artifact (by default "")
     * <li>summary: the summary of the files to upload (by default "artifactName classifier")
     * <li>labels: the labels of the files to upload (by default based on the classifier and packaging)
     *
     * </ul>
     *
     * @parameter
     */
    private Map[] uploads;

    /**
     * Should this goal be executed without actually uploading the files.
     *
     * @parameter expression="false"
     */
    private boolean dryRun;

    /**
     * Should it be possible to upload SNAPSHOT version files. By default this is set to false. This is in accordance
     * with the googlecode file upload policy which requires files with the same name to always contain the same
     * content.
     *
     * @parameter expression="false"
     */
    private boolean allowSnapshots;

    /**
     * Checks whether the given upload descriptor is well defined and executable.
     *
     * @param descriptor the descriptor to validate.
     * @throws MojoExecutionException if the upload description is not well defined or cannot be executed in the current
     *                                state of the project (for example because a file to upload is missing).
     */
    private void validate(UploadDescriptor descriptor) throws MojoExecutionException {
    	
        File file = descriptor.getFile();
            if (!file.exists()) {
                getLog().error("File " + file + " requested by upload descriptor " + descriptor.getId()
                    + " does not exist. Make sure you execute the goals required to produce the file before.");
                throw new MojoExecutionException("Upload file " + file + " + does not exist!");
            }
        
    }

    /**
     * Executes the upload.
     *
     * @throws MojoExecutionException if connection problems appear of the uploads are not properly defined.
     */
    public void execute() throws MojoExecutionException {

        if (!allowSnapshots && isSnapshot()) {
            throw new MojoExecutionException("Cannot upload SNAPSHOT versions. If really necessary, set " +
                "allowSnapshots property to true.");
        }

        userName = settings.getServer(serverId).getUsername();
        password = settings.getServer(serverId).getPassword();


        List<UploadDescriptor> uploadDescriptors = generateUploadDescriptors();

        for (UploadDescriptor descriptor : uploadDescriptors) {
            getLog().info("Uploading " + descriptor.getId());
            if (!dryRun) {
                File file = descriptor.getFile();
                if(file==null || !file.exists())
                	throw new MojoExecutionException("artifact does not exists "+project +" and classifier="+descriptor.getClassifier()  );
                    try {
                        upload(file,  descriptor.getSummary(),
                            descriptor.getLabels());
                    } catch (IOException e) {
                    	
                        getLog().info("Problem when processing upload " + descriptor.getId(), e);
                    }
            }
        }


    }

	private boolean isSnapshot() {
		return project.getVersion().endsWith("SNAPSHOT");
	}

    /**
     * Extract the list of upload descriptors from the configuration of the plugin.
     *
     * @return the list of upload descriptors defined in the configuration of the plugin or the default descriptor if no
     *         descriptor has been defined.
     * @throws MojoExecutionException if some descriptors can't be validated
     */
    private List<UploadDescriptor> generateUploadDescriptors() throws MojoExecutionException {
        List<UploadDescriptor> uploadDescriptors = new ArrayList<UploadDescriptor>();

        if(uploads!=null){
        	
        
          if (uploads.length == 0) {
            UploadDescriptor descriptor = new UploadDescriptor(project);
            getLog().info("Loading descriptor " + descriptor.getId());
            validate(descriptor);
            uploadDescriptors.add(descriptor);
            getLog().debug(" Descriptor " + descriptor.getId() + " = " + descriptor);
        } else {
            for (Map properties : uploads) {
            	
                UploadDescriptor descriptor = new UploadDescriptor(project, properties);
                getLog().info("Loading descriptor " + descriptor.getId());
                validate(descriptor);
                uploadDescriptors.add(descriptor);
                getLog().debug(" Descriptor " + descriptor.getId() + " = " + descriptor);
            }
        }
        }
        return uploadDescriptors;
    }

    /**
     * Uploads the contents of the file  to the project's Google Code upload url. Performs the basic http authentication
     * required by Google Code.
     *
     * @param file           the file to upload
     * @param targetFileName the filename it should have on googlecode
     * @param summary        the file summary
     * @param labelArray     the labels to attach to the file
     * @throws IOException if IO goes wrong.
     */
    private void upload(File file, 
                        String summary, String[] labelArray) throws IOException {
        System.clearProperty("javax.net.ssl.trustStoreProvider"); // fixes open-jdk-issue
        System.clearProperty("javax.net.ssl.trustStoreType");

        final String BOUNDARY = "CowMooCowMooCowCowCow";
        URL url = createUploadURL();

        getLog().info("The upload URL is " + url);

        InputStream in = new BufferedInputStream(new FileInputStream(file));
        if(in.available()>0){
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
       
    
        conn.addRequestProperty("User-Agent", "Mozilla/4.76");
        conn.setRequestProperty("Cookie", "foo=bar"); 
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Basic " + createAuthToken(userName, password));
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        conn.setRequestProperty("User-Agent", "Google Code Upload Maven Plugin 1.0");
        
        getLog().info("Attempting to connect (username is " + userName + ")...");
        conn.connect();

        getLog().info("Sending request parameters...");
        OutputStream out = conn.getOutputStream();
        sendLine(out, "--" + BOUNDARY);
        sendLine(out, "content-disposition: form-data; name=\"summary\"");
        sendLine(out, "");
        sendLine(out, summary);

        if (labelArray.length > 0) {

            if (labelArray.length > 0) {
                getLog().info("Setting " + labelArray.length + " label(s)");

                for (int n = 0, i = labelArray.length; n < i; n++) {
                    sendLine(out, "--" + BOUNDARY);
                    sendLine(out, "content-disposition: form-data; name=\"label\"");
                    sendLine(out, "");
                    sendLine(out, labelArray[n].trim());
                }
            }
        }

        getLog().info("Sending file... " + file.getName());
        sendLine(out, "--" + BOUNDARY);
        sendLine(out, "content-disposition: form-data; name=\"filename\"; filename=\"" + file.getName() + "\"");
        sendLine(out, "Content-Type: application/octet-stream");
        sendLine(out, "");
        int count;
        byte[] buf = new byte[8192];
        while ((count = in.read(buf)) >= 0) {
            out.write(buf, 0, count);
        }
        in.close();
        sendLine(out, "");
        sendLine(out, "--" + BOUNDARY + "--");

        out.flush();
        out.close();

        // For whatever reason, you have to read from the input stream before
        // the url connection will start sending
        in = conn.getInputStream();

        getLog().info("Upload finished. Reading response.");

        getLog().info("HTTP Response Headers: " + conn.getHeaderFields());
        StringBuilder responseBody = new StringBuilder();
        while ((count = in.read(buf)) >= 0) {
            responseBody.append(new String(buf, 0, count, "ascii"));
        }
        getLog().info(responseBody.toString());
        in.close();

        conn.disconnect();
        }
    }

    /**
     * Just sends an ASCII version of the given string, followed by a CRLF line terminator, to the given output stream.
     *
     * @param out    the output stream to write the ASCII string to
     * @param string the string to write as ASCII code.
     * @throws IOException if IO goes wrong.
     */
    private void sendLine(OutputStream out, String string) throws IOException {
        out.write(string.getBytes("ascii"));
        out.write("\r\n".getBytes("ascii"));
    }

    /**
     * Creates a (base64-encoded) HTTP basic authentication token for the given user name and password.
     *
     * @param userName the username for the auth token.
     * @param password the password for the auth token.
     * @return a (base64-encoded) HTTP basic authentication token for the given user name and password.
     */
    private static String createAuthToken(String userName, String password) {
        Base64Converter b64 = new Base64Converter();
        return b64.encode(userName + ":" + password);
    }

    /**
     * Creates the correct URL for uploading to the named google code project.
     * The correct URL will be generated based on the {@link #projectName}.  
     * If projectName is not set, It will be guessed based on the last part of the groupId
     * @return the upload URL.
     * @throws java.net.MalformedURLException if URL is malformed.
     */
    private URL createUploadURL() throws MalformedURLException {
    	
    	 if (projectName == null) {
             //trying to guess based on the last part of the groupId
    		 String groupId=project.getGroupId();
    		 int pos=groupId.lastIndexOf(".");
    		 projectName=groupId.substring(pos+1, groupId.length());
    		 
         }
         return new URL("https", projectName + ".googlecode.com", "/files");
 
    }
}
