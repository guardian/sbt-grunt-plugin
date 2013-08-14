package com.gu

import sbt._
import Keys._
import scala.io.Source
import java.security.MessageDigest

object SbtGruntPlugin extends Plugin {

  private def runGrunt(task: String) = {
    val cmd = "grunt " + task
    cmd !
  }

// TODO: factor out cache and hash helpers

  type StateCache = Map[String, FileState]
  case class FileState(fileSize: Long, fileHash: String) {
    def matches(file: File) = {
      fileState.fileSize == file.length &&
      fileState.fileHash == hash(file)
    }
  }


  val stateCacheDir: File = {
    val t = IO.createTemporaryDirectory
    t.mkdirs()
    t.mkdir()
    t
  }

  private def readCachedState(cacheFile: File): StateCache = {
    Source.fromFile(cacheFile).foldLeft(new StateCache) { (acc, line) =>
      line.split("\t") match {
        case List(path, size, hash) => acc + (path -> FileState(size, hash))
        case _ => acc // ignore mismatching line
      }
    }
  }

  private def writeCachedState(cacheFile: File, state: StateCache) {
    if (!cacheFile.exists()) {
      cacheFile.createNewFile()
    }

    val writer = new FileWriter(cacheFile)
    state.foreach { (filePath, fileState) =>
      writer.write(filePath + "\t" + fileState.fileSize + "\t" + fileState.fileHash + "\n")
    }
    writer.close()
  }

  private def computeState(files: Seq[File]: StateCache = {
    files.foldLeft(Map[String, FileState]()) { (acc, file) =>
      acc + (file.getPath -> FileState(file.length, hash(file)))
    }
  }

  private def filesMatchState(files: Seq[File], cachedState: StateCache): Boolean = {
    if (files.size == cachedState.size) {
      files.forall { file =>
        cachedState.get(file.getPath).exists(_.matches(file))
      }
    } else false
  }

  private def hash(file: File) = {
    val source = Source.fromFile(file)
    val data = source.getLines.mkString
    source.close()
    hash(data)
  }

  private def hash(s: String) = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }

  private def filesUpToDate(finder: PathFinder): Boolean = {
    val files = finder.get
    val finderKey = hash(finder.toString)
    val cacheFile: File = stateCacheDir / finderKey // TODO and task name? or not?
    val cachedState = readCachedState(cacheFile)
    if (! cachedState || ! filesMatchState(files, cachedState)) {
      writeCachedState(cacheFile, computeState(files))
      false
    } else {
      true
    }
  }


  // Grunt command

  def gruntCommand = {
    Command.single("grunt") { (state: State, task: String) =>
      runGrunt(task)

      state
    }
  }

  // Grunt task

  def gruntTask(taskName: String, filesToCheck: SettingKey[PathFinder]) =
    (streams, filesToCheck) map { (s: TaskStreams, finder: PathFinder) =>
      // TODO: optional setting?
      if (finder.isEmpty || ! filesUpToDate(finder)) {
        val retval = runGrunt(taskName)
        if (retval != 0) {
          throw new Exception("Grunt task %s failed".format(taskName))
        }
      }
    }


  // Expose plugin

  override lazy val settings = Seq(commands += gruntCommand)

}
