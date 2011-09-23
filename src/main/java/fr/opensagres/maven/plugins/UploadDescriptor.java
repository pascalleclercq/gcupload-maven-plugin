package fr.opensagres.maven.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * An UploadDescriptor describes an upload to be performed by the gcupload goal.
 * It implements maven's convention over configuration paradigm by automatically
 * deducing default values for properties based on the project properties.
 * 
 * @author Sebastian Riedel
 * @author Pascal Leclercq
 */
public class UploadDescriptor {

	private String[] labels;
	private String classifier = "", summary;

	private MavenProject project;

	private Artifact artifact;

	private static final String LABELS = "labels";
	private static final String SUMMARY = "summary";
	private static final String CLASSIFIER = "classifier";
	private static final Set<String> allowedProperties = new HashSet<String>(
			Arrays.asList(CLASSIFIER, LABELS, SUMMARY));

	/**
	 * Creates a new default upload descriptor for the given maven project.
	 * 
	 * @param project
	 *            the maven project to extract default values from.
	 */
	public UploadDescriptor(MavenProject project) {
		this.project = project;
	}

	/**
	 * Creates a new upload descriptor using the given property map and maven
	 * project for extracting properties of the upload.
	 * 
	 * @param project
	 *            the maven project.
	 * @param properties
	 *            a map with properties for the upload.
	 */
	public UploadDescriptor(MavenProject project, Map properties) {
		this(project);

		@SuppressWarnings({ "unchecked" })
		HashSet<String> propertyKeys = new HashSet<String>(properties.keySet());

		propertyKeys.removeAll(allowedProperties);
		if (!propertyKeys.isEmpty())
			throw new IllegalArgumentException(
					"The following property keys are not allowed in "
							+ "constructing an upload descriptor: "
							+ propertyKeys);

		if (properties.containsKey(CLASSIFIER)) {
			setClassifier((String) properties.get(CLASSIFIER));
			List<Artifact> artifacts = project.getAttachedArtifacts();
			for (Artifact artifact : artifacts) {
				if (artifact.getClassifier().equals(getClassifier())) {
					this.artifact = artifact;
					break;
				}
			}
		} else {
			this.artifact = project.getArtifact();
		}

		if (properties.containsKey(LABELS)) {
			String labelsString = (String) properties.get(LABELS);
			setLabels(labelsString.split("[;, ]+"));
		}
		if (properties.containsKey(SUMMARY)) {
			setSummary((String) properties.get(SUMMARY));
		}

	}

	/**
	 * Returns the id of this upload to be used in info messages.
	 * 
	 * @return id of this upload (classifier or blank).
	 */
	public String getId() {
		return classifier;
	}

	/**
	 * Returns the files to upload.
	 * 
	 * @return files to upload.
	 */
	public File getFile() {
		if (artifact != null)
			return artifact.getFile();

		// else
		return null;
	}

	/**
	 * Returns the classifier that the target files have appended to their
	 * prefix. For example, if the prefix is "gcupload-maven-plugin-0.9" and the
	 * classifier is "jar-with-dependencies" then the filename without extension
	 * will be "gcupload-maven-plugin-0.9-jar-with-dependencies". If the
	 * classifier is the empty string the hyphen will not be added.
	 * 
	 * @return the classifier of this upload.
	 */
	public String getClassifier() {
		return classifier == null ? "" : classifier;
	}



	/**
	 * creates a filename without extension based on prefix and classifier.
	 * 
	 * @param prefix
	 *            the filename prefix
	 * @param classifier
	 *            the filename classifier
	 * @return a combination of both.
	 */
	private String createFileNameNoExtension(String prefix, String classifier) {
		return prefix
				+ (classifier != null && classifier.length() > 0 ? "-"
						+ classifier : "");
	}

	/**
	 * Returns the summary of this upload to be added to the file in google
	 * code. If no summary has been specified this is deduced "description"
	 *  tag of the project.
	 * 
	 * @return the summary of the upload.
	 */
	public String getSummary() {
		return summary == null ? project.getDescription() : summary;
	}

	/**
	 * Returns the set of labels for this upload. By default labels are
	 * generated based on the packacking of the project and classifier of this
	 * upload.
	 * 
	 * @return labels of this upload.
	 */
	String[] getLabels() {
		if (labels != null)
			return labels;
		ArrayList<String> result = new ArrayList<String>();
		if (project.getPackaging().equals("jar")) {
			result.add("OpSys-All");
		} else if (project.getPackaging().equals("maven-plugin")) {
			result.add("OpSys-All");
		}
		if (getClassifier().contains("src")
				|| getClassifier().contains("sources")) {
			result.add("Type-Source");
		}
		
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Set upload labels.
	 * 
	 * @param labels
	 *            labels to attach to uploads.
	 */
	public void setLabels(String[] labels) {
		this.labels = labels;
	}

	/**
	 * Sets the upload classifier.
	 * 
	 * @param classifier
	 *            upload classifier.
	 */
	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	/**
	 * Sets the upload summary.
	 * 
	 * @param summary
	 *            the summary for the upload.
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}

	/**
	 * Returns a string representation of this upload descriptor.
	 * 
	 * @return a string representation of this upload descriptor.
	 */
	public String toString() {
		return "UploadDescriptor{"
				+
				", files=" + getFile() + ", labels="
				+ Arrays.asList(getLabels()) + ", classifier='"
				+ getClassifier() + '\'' + ", summary='" + getSummary() +
				'}';
	}
}
