with open('gradle/libs.versions.toml', 'r') as f:
    content = f.read()

content = content.replace('fastexcel = "0.15.3"', 'fastexcel = "0.15.3"\nxml_stream = "1.0-2"\naalto_xml = "1.3.2"')
content = content.replace('fastexcel-reader = { group = "org.dhatim", name = "fastexcel-reader", version.ref = "fastexcel" }', 'fastexcel-reader = { group = "org.dhatim", name = "fastexcel-reader", version.ref = "fastexcel" }\nstax-api = { group = "javax.xml.stream", name = "stax-api", version.ref = "xml_stream" }\naalto-xml = { group = "com.fasterxml", name = "aalto-xml", version.ref = "aalto_xml" }')

with open('gradle/libs.versions.toml', 'w') as f:
    f.write(content)
