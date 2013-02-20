import sbt._
import Keys._

object SbtGruntPlugin extends Plugin {

  private def runGrunt(task: String) = {
    val cmd = "grunt " + task
    cmd !
  }

  // Grunt command

  def gruntCommand = {
    Command.single("grunt") { (state: State, task: String) =>
      runGrunt(task)

      state
    }
  }

  // Grunt task

  def gruntTask(taskName: String) = streams map { (s: TaskStreams) =>
      val retval = runGrunt(taskName)
      if (retval != 0) {
        throw new Exception("Grunt task %s failed".format(taskName))
      }
  }


  // Expose plugin

  override lazy val settings = Seq(commands += gruntCommand)

}
