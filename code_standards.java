//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter:4.1.0
//SOURCES utils.java

import org.springframework.util.function.ThrowingConsumer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

void main() throws Exception {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor();) {
        var start = System.currentTimeMillis();
        var callables = new ArrayList<Callable<Void>>();
        var root = Paths.get(".");
        var preflight = false;
        try (var paths = Files.walk(root)) {
            var poms = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .toList();
            for (var pom : poms)
                callables.add(() -> {
                    process(pom, preflight);
                    return null;
                });
            executor.invokeAll(callables);
            var stop = System.currentTimeMillis();
            IO.println("total time in ms.:" + (stop - start));
        }
    }
}

void process(Path pom, boolean preflight) throws Exception {
    var springBootVersion = "4.1.0";
    var mavenJavaFormatMavenPlugin = "0.0.47";
    var springAiVersion = "2.0.0";
    var javaVersion = "25";
    var springCloudVersion = "2025.1.2";
    var springModulithVersion = "2.1.0";
    var processors = List.of(
            new SpringBootParentVersionMavenProjectTransformer(springBootVersion), //
            new JavaformatPluginAddingMavenProjectTransformer(mavenJavaFormatMavenPlugin), //
            new JavaformatPluginApplyingMavenProjectTransformer(),
            new PropertiesOverridingMavenProjectTransformer("java.version", javaVersion),
            new PropertiesOverridingMavenProjectTransformer("spring-ai.version", springAiVersion),
            new PropertiesOverridingMavenProjectTransformer("spring-cloud.version", springCloudVersion),
            new PropertiesOverridingMavenProjectTransformer("spring-modulith.version", springModulithVersion)
    );

    var dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(false);
    dbf.setIgnoringElementContentWhitespace(true);

    var doc = dbf.newDocumentBuilder().parse(pom.toFile());
    doc.getDocumentElement().normalize();

    var mavenProject = new MavenProject(pom, doc);

    for (var p : processors) {
        p.accept(mavenProject);
        write(doc, preflight, pom);
    }
}

private static void write(Document doc, boolean preflight, Path pom) throws Exception {
    stripWhitespaceNodes(doc);

    try (var sw = preflight ? new StringWriter() : new FileWriter(pom.toFile())) {
        write(doc, sw);
        if (preflight)
            IO.println("transformed pom.xml: " + sw);
    }
}

interface MavenProjectTransformer extends ThrowingConsumer<MavenProject> {
}

record MavenProject(Path pomFile, Document pom) {
}

static class PropertiesOverridingMavenProjectTransformer implements MavenProjectTransformer {

    private final String propertyValue;

    private final String propertyName;

    PropertiesOverridingMavenProjectTransformer(String propertyName, String propertyValue) {
        this.propertyValue = propertyValue;
        this.propertyName = propertyName;
    }

    @Override
    public void acceptWithException(MavenProject mp) throws Exception {
        var doc = mp.pom();
        var project = doc.getDocumentElement();
        var properties = firstChildElement(project, "properties");
        if (properties == null) {
            properties = doc.createElement("properties");
            project.appendChild(properties);
        }
        var propertyElement = firstChildElement(properties, this.propertyName);
        if (propertyElement == null) {
            propertyElement = doc.createElement(this.propertyName);
            properties.appendChild(propertyElement);
        }
        if (!propertyValue.equals(propertyElement.getTextContent().trim())) {
            propertyElement.setTextContent(this.propertyValue);
        }
    }
}

static class JavaformatPluginApplyingMavenProjectTransformer implements MavenProjectTransformer {

    @Override
    public void acceptWithException(MavenProject document) throws Exception {
        var pomFile = new File(document
                .pomFile() //
                .normalize() //
                .toFile()
                .getAbsolutePath());
        var parent = pomFile.getParentFile();
        var mvnw = new File(parent, "mvnw");
        Runner.run(parent.toPath(), mvnw.getAbsolutePath(), "spring-javaformat:apply");
    }
}

static class JavaformatPluginAddingMavenProjectTransformer implements MavenProjectTransformer {

    private final String groupId = "io.spring.javaformat";
    private final String artifactId = "spring-javaformat-maven-plugin";
    private final String version;

    JavaformatPluginAddingMavenProjectTransformer(String version) {
        this.version = version;
    }

    @Override
    public void acceptWithException(MavenProject doc) throws Exception {
        if (hasPlugin(doc.pom(), groupId, artifactId)) {
            return;
        }
        var plugins = ensurePluginsElement(doc.pom());
        plugins.appendChild(buildPluginElement(doc.pom(), groupId, artifactId, version));
    }

}

static class SpringBootParentVersionMavenProjectTransformer implements MavenProjectTransformer {

    private final String groupId = "org.springframework.boot";
    private final String artifactId = "spring-boot-starter-parent";
    private final String version;

    SpringBootParentVersionMavenProjectTransformer(String version) {
        this.version = version;
    }

    @Override
    public void acceptWithException(MavenProject doc) throws Exception {

        var parent = firstChildElement(doc.pom().getDocumentElement(), "parent");
        if (parent == null) {
            return; // no parent block; nothing to pin
        }
        var gid = childText(parent, "groupId");
        var aid = childText(parent, "artifactId");
        if (!groupId.equals(gid) || !artifactId.equals(aid)) {
            return; // some other parent; leave it alone
        }

        var versionEl = firstChildElement(parent, "version");
        if (versionEl == null) {
            versionEl = doc.pom().createElement("version");
            versionEl.setTextContent(version);
            parent.appendChild(versionEl);
        }//
        else if (!version.equals(versionEl.getTextContent().trim())) {
            versionEl.setTextContent(version);
        }
    }

}

static void stripWhitespaceNodes(Document doc) throws XPathExpressionException {
    var xp = XPathFactory.newInstance().newXPath();
    var empty = (NodeList) xp.evaluate(
            "//text()[normalize-space(.)='']", doc, XPathConstants.NODESET);
    for (var i = 0; i < empty.getLength(); i++) {
        var node = empty.item(i);
        node.getParentNode().removeChild(node);
    }
}

/**
 * True if a <plugin> with our groupId + artifactId already exists anywhere.
 */
static boolean hasPlugin(Document doc, String groupId, String artifactId) {
    var plugins = doc.getElementsByTagName("plugin");
    for (var i = 0; i < plugins.getLength(); i++) {
        var plugin = (Element) plugins.item(i);
        var gid = childText(plugin, "groupId");
        var aid = childText(plugin, "artifactId");
        if (groupId.equals(gid) && artifactId.equals(aid)) {
            return true;
        }
    }
    return false;
}

/**
 * Find or create <project><build><plugins>, returning the plugins element.
 */
static Element ensurePluginsElement(Document doc) {
    var project = doc.getDocumentElement();
    var build = firstChildElement(project, "build");
    if (build == null) {
        build = doc.createElement("build");
        project.appendChild(build);
    }
    var plugins = firstChildElement(build, "plugins");
    if (plugins == null) {
        plugins = doc.createElement("plugins");
        build.appendChild(plugins);
    }
    return plugins;
}

static Element buildPluginElement(Document doc, String groupId, String artifactId, String version) {
    var plugin = doc.createElement("plugin");
    plugin.appendChild(textElement(doc, "groupId", groupId));
    plugin.appendChild(textElement(doc, "artifactId", artifactId));
    plugin.appendChild(textElement(doc, "version", version));
    return plugin;
}

// --- small DOM helpers ---
static Element firstChildElement(Element parent, String name) {
    var children = parent.getChildNodes();
    for (var i = 0; i < children.getLength(); i++) {
        var n = children.item(i);
        if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(name)) {
            return (Element) n;
        }
    }
    return null;
}

static String childText(Element parent, String name) {
    var e = firstChildElement(parent, name);
    return e == null ? null : e.getTextContent().trim();
}

static Element textElement(Document doc, String name, String value) {
    var e = doc.createElement(name);
    e.setTextContent(value);
    return e;
}


static void write(Document doc, Writer pom) throws Exception {
    var t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty(OutputKeys.INDENT, "yes");
    t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    t.transform(new DOMSource(doc), new StreamResult(pom));
}
