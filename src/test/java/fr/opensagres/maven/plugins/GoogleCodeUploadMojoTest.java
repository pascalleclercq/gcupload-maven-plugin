package fr.opensagres.maven.plugins;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.settings.Server;

import fr.opensagres.maven.plugins.GoogleCodeUploadMojo;

/**
 * Test google-code:upload.
 * 
 * @author <a href="mailto:pascal.leclercq@gmail.com">Pascal Leclercq</a>
 */
public class GoogleCodeUploadMojoTest extends AbstractMojoTestCase {
	protected GoogleCodeUploadMojo mojo;

	protected void setUp() throws Exception {
		super.setUp();

		File testFile = getTestFile("target/test-classes/gcupload.xml");
		System.out.println(testFile);
		System.out.println(lookupMojo("gcupload", testFile));
		mojo = (GoogleCodeUploadMojo) lookupMojo("gcupload", testFile);

	}

	public void testUpload() throws MojoFailureException,
			MojoExecutionException {
		Server server = new Server();
		server.setId("code.google.com");
		server.setUsername(System.getenv("username"));
		server.setPassword(System.getenv("password"));

		mojo.settings.addServer(server);
		assertNotNull(mojo);

		try {
			mojo.dryRun=true;
			assertNotNull(mojo.project);
			assertNotNull(mojo.project.getArtifact());
			mojo.project.getArtifact().setFile(File.createTempFile("temp", "jar"));
			mojo.execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// assertTrue( true );
	}
}
