sbt-grunt-plugin
================

An [sbt](http://www.scala-sbt.org/) plugin to wrap [Grunt](http://gruntjs.com/) tasks and expose them as sbt tasks.

This plugin provides a `gruntTask` function will return an sbt task wrapping the Grunt task with the given name. You can use it to add steps to your compile or test setup in your `build.sbt` file, e.g.:

     (compile in Compile) <<= (compile in Compile) dependsOn (gruntTask("requirejs"))


This plugin also exposes a `grunt` command which can be called from the sbt console:

     > grunt jshint  # run the "jshint" grunt task
     
     Running "jshint" (jshint) task
     >> 150 files lint free.
     
     Done, without errors.
