# github-changes-maven-plugin
Maven changes plugin for GitHub hosted repositories.

[![Build Status](https://travis-ci.org/heuermh/github-changes-maven-plugin.svg?branch=master)](https://travis-ci.org/heuermh/github-changes-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.heuermh.maven.plugin.changes/github-changes-maven-plugin.svg?maxAge=600)](http://search.maven.org/#search%7Cga%7C1%7Ccom.github.heuermh.maven.plugin.changes)


### Hacking github-changes-maven-plugin

Install

 * JDK 1.8 or later, http://openjdk.java.net
 * Apache Maven 3.3.9 or later, http://maven.apache.org

To build

    $ mvn install


### Running github-changes-maven-plugin

Run the `github-changes-maven-plugin` specifying the GitHub milestone identifier.

```bash
$ mvn \
    com.github.heuermh.maven.plugin.changes:github-changes-maven-plugin:1.1:github-changes \
    -DmilestoneId=1
```

The GitHub repository is parsed from the project issue management URL. If this is not specified
in `pom.xml`, it can be provided via `-DissueManagementUrl=https://github.com/user/repo/issues`
parameter.

Changes from closed issues and merged or closed pull requests associated with the specified
GitHub milestone identifier are written to the changes file `CHANGES.md` (which can be specified
via `-DchangesFile=my-changes.md` parameter) in Markdown format.

An example `CHANGES.md` file maintained by this plugin may be seen in the
[bigdatagenomics/adam](https://github.com/bigdatagenomics/adam) repository at:

https://github.com/bigdatagenomics/adam/blob/master/CHANGES.md
