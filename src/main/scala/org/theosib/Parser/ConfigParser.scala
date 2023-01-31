package org.theosib.Parser

import org.theosib.Utils.{ConfigurationModelRepresenter, FileLocator}
import org.yaml.snakeyaml.{DumperOptions, Yaml}
import org.yaml.snakeyaml.representer.Representer

import java.io.FileInputStream
import java.util

object ConfigParser {
  def parseInternal(fileType: FileLocator.FileCategory, name: String, map: java.util.LinkedHashMap[String, String]): Unit = {
    val fname = FileLocator.computePath(fileType, name) + ".yaml"
    println(s"Loading ${fname}")
    val options = new DumperOptions()
    val representer = new ConfigurationModelRepresenter()
    val yaml = new Yaml(representer, options)
    val result = yaml.load(new FileInputStream(fname)).asInstanceOf[java.util.LinkedHashMap[String, Any]]

    result.forEach { case (key, value) =>
//      println(s"${key} ${value}")
      if (key.startsWith("import") || key.startsWith("include")) {
        val imports = value.asInstanceOf[util.ArrayList[String]]
        imports.forEach { x =>
//          println(s"Importing ${x}")
          parseInternal(fileType, x, map);
        }
      } else {
        map.put(key, value.asInstanceOf[String])
      }
    }
  }

  def parse(fileType: FileLocator.FileCategory, name: String): java.util.LinkedHashMap[String, String] = {
    val map = new java.util.LinkedHashMap[String, String]()
    parseInternal(fileType, name, map);
    map
  }
}
