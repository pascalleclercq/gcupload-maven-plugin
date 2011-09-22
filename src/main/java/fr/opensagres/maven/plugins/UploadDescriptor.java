package fr.opensagres.maven.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
 */
public class UploadDescriptor {
	// private String[] extensions,
	private String[] labels;
	private String classifier = "", summary;
	// private String prefix;
	private MavenProject project;

	private Artifact artifact;

	private static final String LABELS = "labels";
	private static final String SUMMARY = "summary";
	private static final String CLASSIFIER = "classifer";
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
			Set<Artifact> artifacts = project.getArtifacts();
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
	 * @return id of this upload (constructed of filename + extensions.
	 */
	public String getId() {
		return classifier;
	}

	/**
	 * Returns the files to upload.
	 * 
	 * @return files to upload.
	 */
	public File getFilesToUpload() {
		return artifact.getFile();
		/*
		 * String[] fileNames = getTargetFileNames(); File[] result = new
		 * File[fileNames.length]; for (int i = 0; i < result.length; ++i) {
		 * result[i] = new File(project.getBasedir() + "/target/" +
		 * fileNames[i]); } return result;
		 */}

	/* *//**
	 * Returns the list of filename extensions. This is either a
	 * user-specified list (through configuration) or a single default extension
	 * based on the project packaging property.
	 * 
	 * @return array of file name extensions.
	 */
	/*
	 * public String[] getExtensions() { if (extensions != null) return
	 * extensions; if (project.getPackaging().equals("maven-plugin") ||
	 * project.getPackaging().equals("jar")) return new String[]{"jar"}; if
	 * (project.getPackaging().equals("war")) return new String[]{"war"}; return
	 * new String[]{"jar"}; }
	 */

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

	/* *//**
	 * Return the array of target filenames as defined by the prefix,
	 * classifier and extensions.
	 * 
	 * @return the array of target filenames
	 */
	/*
	 * public String[] getTargetFileNames() { String[] extensions =
	 * getExtensions(); String[] result = new String[extensions.length]; String
	 * classifier = getClassifier();
	 * 
	 * for (int i = 0; i < extensions.length; ++i) { result[i] =
	 * createFileNameNoExtension(prefix, classifier) + "." + extensions[i]; }
	 * return result; }
	 */

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
	 * code. If no summary has been specified this is deduced from project name,
	 * version and classifier.
	 * 
	 * @return the summary of the upload.
	 */
	public String getSummary() {
		return summary == null ? project.getName() + " " + project.getVersion()
				+ " " + getClassifier() : summary;
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
				// "extensions=" + Arrays.asList(getExtensions()) +
				", files=" + Arrays.asList(getFilesToUpload()) + ", labels="
				+ Arrays.asList(getLabels()) + ", classifier='"
				+ getClassifier() + '\'' + ", summary='" + getSummary() + '\'' +
				// ", prefix='" + getPrefix() +
				'}';
	}
}
