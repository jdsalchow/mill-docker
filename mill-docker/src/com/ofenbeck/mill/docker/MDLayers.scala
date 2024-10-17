package com.ofenbeck.mill.docker

import com.google.cloud.tools.jib.api.buildplan.FilePermissions
import java.time.Instant

/** Adds an entry to the layer with the given permissions and file modification time. Only adds the single source file
  * to the exact path in the container file system. See {@link Builder#addEntry(Path, AbsoluteUnixPath)} for more
  * information.
  *
  * @param sourceFile
  *   the source file to add to the layer
  * @param pathInContainer
  *   the path in the container file system corresponding to the {@code sourceFile}
  * @param permissions
  *   the file permissions on the container
  * @param modificationTime
  *   the file modification time
  * @param ownership
  *   file ownership. For example, "1234", "user", ":5678", ":group", "1234:5678", and "user:group". Note that "" (empty
  *   string), ":" (single colon), "0:", ":0" are allowed and representative of "0:0" or "root:root", but prefer an
  *   empty string for "0:0".
  * @return
  *   this
  * @see
  *   Builder#addEntry(Path, AbsoluteUnixPath)
  * @see
  *   FilePermissions#DEFAULT_FILE_PERMISSIONS
  * @see
  *   FilePermissions#DEFAULT_FOLDER_PERMISSIONS
  */

case class MDFileEntry(
    sourceFile: mill.PathRef,
    pathInContainer: os.Path,
    permissions: FilePermissions,
    modificationTime: Instant,
    ownership: String,
) {}
object MDFileEntry {
  implicit val filepermissions: upickle.default.ReadWriter[FilePermissions] = upickle.default
    .readwriter[String]
    .bimap[FilePermissions](
      _.toOctalString(),
      str => FilePermissions.fromOctalString(str),
    )
  implicit val instantRW: upickle.default.ReadWriter[Instant] = upickle.default
    .readwriter[String]
    .bimap[Instant](
      _.toString,
      str => Instant.parse(str),
    )
  implicit val ospath: upickle.default.ReadWriter[os.Path] = upickle.default.readwriter[String].bimap[os.Path](
    _.toString(),
    str => os.Path(str),
  )
  implicit val rw: upickle.default.ReadWriter[MDFileEntry] = upickle.default.macroRW
}

case class MDLayer(
    val entries: Vector[MDFileEntry] = Vector.empty[MDFileEntry],
)

object MDLayers {
  def createDefaultLayers(buildSettings: BuildSettings): Vector[MDLayer] = {

    val (upstreamClassSnapShot, upstreamClass) =
      buildSettings.upstreamAssemblyClasspath.partition(MDShared.isSnapshotDependency(_))
    Vector(
      MDLayer(toMDFileEntries(upstreamClass, os.Path("/app/lib/"))),
      MDLayer(toMDFileEntries(upstreamClassSnapShot, os.Path("/app/lib/"))),
      MDLayer(toMDFileEntries(buildSettings.resourcesPaths, os.Path("/app/resources/"))),
      MDLayer(toMDFileEntries(Seq(buildSettings.compiledClasses), os.Path("/app/classes/"))),
    )
  }

  def toMDFileEntries(files: Seq[mill.PathRef], pathInContainer: os.Path): Vector[MDFileEntry] =
    files.toVector.map(file =>
      MDFileEntry(file, pathInContainer, FilePermissions.DEFAULT_FILE_PERMISSIONS, Instant.now(), ""),
    )

}
