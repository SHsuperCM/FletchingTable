# Fletching Table
Additions and automations for fabric-loom

## This project is still experimental, use at your own risk or wait a little so I can polish it :)

## Features
### Entrypoints
Automatically registers entrypoints into the project's mod json.

Simply annotate a static class, method or field with `@Entrypoint` and specify the desired entrypoint to register to in its value!
Fletching Table will automatically process it and add it to existing entrypoints in `fabric.mod.json`.

### Included Jars
Exposes jars that were included in dependencies with Loom's `include` configuration.

To use, first add an `includedJars` closure in your dependencies. 
Add parent dependencies to extract from using the `from` configuration, at the end call `extractAll()`.
Then, use extracted jars in other configurations by setting the group to `includedJars` and the name to the name of the extracted jar.

As an example, here's how to add Pride Lib inside Lambdynamic Lights from the modrinth maven to the modCompileOnly configuration:
```groovy
includedJars {
    from "maven.modrinth:lambdynamiclights:2.1.0+1.17"
    extractAll()
}
modCompileOnly "includedJars:pridelib-1.1.0+1.17"
```

### Settings
Fletching Table's default settings can be changed in an extension named `fletchingTable` as follows:
```groovy
fletchingTable {
    // Enables the included jars extraction process
    enableIncludedJars = true //default
    // Enables injecting processed entrypoint annotations to the mod json
    enableEntrypoints = true //default
    // Enables the entire annotation processor
    enableAnnotationProcessor = true //default
}
```


## Setup
Add the plugin by applying it **after loom**.
```
plugins {
  id 'fabric-loom' ...
  id "io.shcm.shsupercm.fabric.fletchingtable" version "1.1"
}
```

## Planned
 - Automatic Mixin registry

## About Fletching Table
I originally suggested automatic entrypoints to loom but it was denied as it was out of scope. I was told it would make a good library and while I still believe Fabric should have automatic entrypoints by default, I agree that it needs to be a thing regardless.

Someone then told me on the Fabric discord that [Fudge](https://github.com/natanfudge) already made [a library for entrypoint annotations](https://github.com/natanfudge/AutoFabric) but it turned out to be buggy and hard to fix(why kotlin..).
Still, Fudge did a good amount of work that in the end helped me set up Fletching Table so thanks for that.

I also had some more things I wanted out of loom so I figured I might as well make my own plugin.

Why "Fletching Table"?
I- Idk.. just wanted something Loom-adjacent and didnt want to call it Smithing Table :p
