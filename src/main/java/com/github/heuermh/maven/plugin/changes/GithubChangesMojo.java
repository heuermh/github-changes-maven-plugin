/*
 * The authors of this file license it to you under the
 * Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You
 * may obtain a copy of the License at
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
package com.github.heuermh.maven.plugin.changes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.Files;

import org.apache.maven.execution.MavenSession;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRepository;

/**
 * Maven changes plugin for GitHub hosted repositories.
 *
 * @author  Michael Heuer
 */
@Mojo(name = "github-changes")
public final class GithubChangesMojo extends AbstractMojo {
    /** Github milestone identifier. */
    @Parameter(property = "milestoneId", required = true)
    private int milestoneId;

    /** Changes file. */
    @Parameter(property = "changesFile", defaultValue = "${project.basedir}/CHANGES.md", required = true)
    private File changesFile;

    /** Issue management URL. */
    @Parameter(defaultValue = "${project.issueManagement.url}", readonly = true, required = true)
    private String issueManagementUrl;

    /** Current working directory. */
    @Parameter(property = "basedir", required = true)
    private String basedir;

    /** Maven session. */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession mavenSession;

    /** GitHub issue management URL pattern. */
    private static final Pattern ISSUE_URL = Pattern.compile("^.+\\/([a-zA-Z0-9_-]+\\/[a-zA-Z0-9_-]+)\\/issues\\/*$");

    @Override
    public void execute() throws MojoExecutionException {
        if (!isExecutionRoot()) {
            return;
        }
        try {
            // write milestone changes to new temporary file
            getLog().info("Writing milestone " + milestoneId + " changes to temporary file");
            File milestoneChanges = File.createTempFile("CHANGES." + milestoneId + ".", ".md");
            write(milestoneChanges);

            // merge milestone changes into existing changes file
            getLog().info("Merging milestone " + milestoneId + " changes into existing changes file");
            File merged = merge(changesFile, milestoneChanges);

            // move merged changes file to CHANGES.md
            getLog().info("Moving merged changes file to " + changesFile);
            Files.move(merged, changesFile);
        }
        catch (IOException e) {
            throw new MojoExecutionException("could not write changes for milestone id " + milestoneId + ", caught " + e.getMessage(), e);
        }
    }

    void write(final File milestoneChanges) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(milestoneChanges), true);

            Matcher m = ISSUE_URL.matcher(issueManagementUrl);
            if (!m.matches()) {
                throw new IOException("could not parse owner and repo from issue management URL " + issueManagementUrl);
            }

            getLog().info("Retrieving issues for milestone " + milestoneId + " from repository " + m.group(1));

            GitHub github = GitHub.connect();
            GHRepository repository = github.getRepository(m.group(1));
            GHMilestone milestone = repository.getMilestone(milestoneId);
            List<GHIssue> pulls = new ArrayList<GHIssue>();
            writer.println("\n### Version " + milestone.getTitle() + " ###\n");
            writer.println("**Closed issues:**\n");
            for (GHIssue issue : repository.getIssues(GHIssueState.CLOSED, milestone)) {
                if (issue.isPullRequest()) {
                    pulls.add(issue);
                }
                else {
                    writer.println(" - " + issue.getTitle() + " [\\#" + issue.getNumber() + "](" + issue.getHtmlUrl() + ")");
                }
            }
            writer.println("\n**Merged and closed pull requests:**\n");
            for (GHIssue pull : pulls) {
                writer.println(" - " + pull.getTitle() + " [\\#" + pull.getNumber() + "](" + pull.getHtmlUrl() + ") ([" + pull.getUser().getLogin() + "](" + pull.getUser().getHtmlUrl() + "))");
            }
        }
        finally {
            try {
                writer.close();
            }
            catch (Exception e) {
                // empty
            }
        }
    }

    static File merge(final File changesFile, final File milestoneChanges) throws IOException {
        PrintWriter writer = null;
        File mergedFile = File.createTempFile("CHANGES.merged", ".md");
        try {
            writer = new PrintWriter(new FileWriter(mergedFile), true);
            List<String> changesLines = Files.readLines(changesFile, Charset.forName("UTF-8"));
            List<String> milestoneChangesLines = Files.readLines(milestoneChanges, Charset.forName("UTF-8"));

            if (changesLines.isEmpty()) {
                // add new header
                writer.println("# Changelog #");
            }
            else {
                // write header from existing CHANGES.md
                writer.println(changesLines.get(0));
            }

            // write all lines from milestone changes
            for (String line : milestoneChangesLines) {
                writer.println(line);
            }

            // add a blank line
            writer.println("");

            if (changesLines.size() > 1) {
                // write remaining lines from existing CHANGES.md
                for (int i = 1; i < changesLines.size(); i++) {
                    writer.println(changesLines.get(i));
                }
            }
        }
        finally {
            try {
                writer.close();
            }
            catch (Exception e) {
                // empty
            }            
        }
        return mergedFile;
    }

    boolean isExecutionRoot() {
        return mavenSession.getExecutionRootDirectory().equalsIgnoreCase(basedir);
    }
}
