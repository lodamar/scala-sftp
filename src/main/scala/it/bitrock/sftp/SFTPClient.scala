package it.bitrock.sftp

import java.io.File
import java.nio.file.Files
import java.util.Properties
import java.util.UUID._

import com.jcraft.jsch.{ ChannelSftp, JSch }

import scala.collection.JavaConverters._
import scala.util.Try

trait Client {
  def download(from: String): Either[Throwable, File]
  def upload(file: File, to: String): Either[Throwable, File]
  def copy(from: String, to: String): Either[Throwable, File]
  def move(from: String, to: String): Either[Throwable, File]
  def ls(of: String): Either[Throwable, List[String]]
}

class SFTPClient(host: String, user: String, password: String) extends Client {

  private lazy val client: ChannelSftp = {
    val jsch   = new JSch()
    val config = new Properties
    config.put("StrictHostKeyChecking", "no")

    val session = jsch.getSession(user, host)
    session.setConfig(config)
    session.setPassword(password)
    session.connect()

    val sftpChannel = session.openChannel("sftp").asInstanceOf[ChannelSftp]
    sftpChannel.connect()

    sftpChannel
  }

  override def download(from: String): Either[Throwable, File] = {
    val downloaded = for {
      dir      <- Try(Files.createTempDirectory(randomUUID().toString))
      tempFile = s"${dir.toString}/${name(from)}"
      _        <- Try(client.get(from, tempFile))
      file     <- Try(new File(tempFile))
    } yield file

    downloaded.toEither
  }

  override def upload(file: File, to: String): Either[Throwable, File] = {
    val uploaded = for {
      _ <- Try(client.put(file.getPath, to))
    } yield file

    uploaded.toEither
  }

  override def copy(from: String, to: String): Either[Throwable, File] =
    for {
      downloaded <- download(from)
      uploaded   <- upload(downloaded, to)
    } yield uploaded

  override def move(from: String, to: String): Either[Throwable, File] =
    for {
      copied <- copy(from, to)
      _      <- Try(client.rm(from))
    } yield copied

  override def ls(of: String): Either[Throwable, List[String]] = {
    val fileNames = for {
      files <- Try(client.ls(of))
      file  <- files.asInstanceOf[java.util.Vector[ChannelSftp#LsEntry]].asScala
    } yield file.getFilename

    fileNames.toEither
  }

  private def name(path: String) = path.split("/").toList.last
}

object SFTPClient {
  def apply(host: String, user: String, password: String): SFTPClient = new SFTPClient(host, user, password)

  implicit class TryToEither[T](tried: Try[T]) {
    def toEither: Either[Throwable, T] = tried.fold(Left(_), Right(_))
  }
}
