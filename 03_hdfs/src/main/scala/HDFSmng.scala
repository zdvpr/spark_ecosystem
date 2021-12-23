
object HDFSmng extends App {

  import org.apache.hadoop.conf._
  import org.apache.hadoop.fs._
  import java.net.URI

  val conf = new Configuration()
  val fsLocation = "hdfs://localhost:9000"
  val locationStage = fsLocation + "/stage"
  val locationOds = fsLocation +  "/ods"
  val fileSystem = FileSystem.get(new URI(fsLocation), conf)
  val path = new Path(locationStage + "/date=2020-12-01/part-0000.csv")
  fileSystem.open(path)

  def createFolder(folderPath: String): Unit = {
    val path = new Path(folderPath)
    if (!fileSystem.exists(path)) {
      fileSystem.mkdirs(path)
    }
  }

  val pathStage = new Path(locationStage)

  val dirs = fileSystem.listStatus(pathStage).filter(_.isDirectory)

  createFolder(locationOds)

  for (i <- dirs) {
    val pos = i.getPath.toString.indexOf("/date=")
    if (pos>0) {
      val curDir = i.getPath.toString.substring(pos)

      createFolder(locationOds+curDir)

      val target = fileSystem.listStatus(new Path(locationOds+curDir))
      target.foreach(e => fileSystem.delete(e.getPath, true))

      val files = fileSystem.listStatus(new Path(locationStage+curDir)).filter(!_.isDirectory).filter(_.getPath.toString.endsWith(".csv"))
      val arrStageFiles:Array[Path] = new Array[Path](files.length)

      for (k <- 0 to (files.length-1))
        arrStageFiles(k) = files(k).getPath

      val len = arrStageFiles.length
      val firstFile = if (len>0) arrStageFiles(0) else null

      if (len>1)
        fileSystem.concat(firstFile, arrStageFiles.slice(1,len))

      if (len>0)
        fileSystem.rename(firstFile,new Path(locationOds+curDir+"/"+firstFile.getName) )

    }
  }

}
