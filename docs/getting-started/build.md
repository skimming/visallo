# Build Instructions

In the majority of cases, most Visallo components can be built by simply opening a terminal to the component root directory and running `mvn package`. More specific instructions for both can be found below.

<a name="root-module"/>
## Root Module Installation

You'll need to install the Visallo `root` Maven module after you've cloned the [source code](source-code.md) and any time you pull the latest Visallo source code. Please note that you will need to [configure Maven to use a proxy](https://maven.apache.org/guides/mini/guide-proxies.html) if your network requires a web proxy to access content on the Internet.

    mvn install -f root/pom.xml

## Smoke Test

You should make sure everything compiles and tests pass before going any further. Otherwise, it's hard to reason about what might be wrong when things later fail.

Compile all modules:

    mvn compile

Run all unit tests, continuing on failures and without failing the build:

    mvn test -fn

It is a known issue that some unit tests fail on Windows. The following are expected to fail:
* `org.visallo.core.formula.FormulaEvaluatorTest`

## Web Application

Building the Visallo web application is very similar to running it. From the root directory of the Visallo project, run

    mvn package -pl web/war -am -Pdefault-webapp -DskipTests -Dsource.skip=true

The previous command will create a default WAR file in the `web/war/target` directory. This WAR file is meant to mimic
the webapp that starts when you run the maven command described in the section on [running](running.md). You will need
to set the `VISALLO_DIR` environment variable to point to the parent directory of your Visallo configuration files.

<a name="web-plugin"/>
## Web Application Plugins

The Visallo web application can be extended with dynamically loaded plugins. You can find some example plugins in `web/plugins`.

To build a web plugin, run:

    mvn package -pl ./web/plugins/<plugin-name>/ -am -DskipTests

Once the plugin JAR file is created, copy it to `$VISALLO_DIR/lib`, which should be accessible on all of your servers.

This is the easiest way to expose the plugin to all web servers. The Visallo web application will automatically add the JAR file to the classpath.

To learn more about extending Visallo with plugins, please [read this](../extension-points/index.md).


## Graph Property Workers

Each graph property worker can be built independently using the following Maven command:

    mvn package -pl <path_to_plugin_dir> -am

Once the plugin JAR file is created, copy it to `$VISALLO_DIR/lib`, which should be accessible on all of your servers.

As an example, to build and deploy the `tika-mime-type-plugin` one would run the following commands from the root of
the Visallo project:

    mvn package -pl graph-property-worker/plugins/tika-mime-type -am
    cp graph-property-worker/plugins/tika-mime-type/target/visallo-tika-mime-*-jar-with-dependencies.jar $VISALLO_DIR/lib

Visallo's will automatically detect graph property workers found within the classpath.
