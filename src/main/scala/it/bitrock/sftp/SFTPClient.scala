package it.bitrock.sftp

import java.io.File
import java.nio.file.Files
import java.util.UUID._

import scala.util.Try

class SFTPClient(sftpRawClient: SFTPRawClient) {

  def download(from: String): Either[Throwable, File] = {
    val downloaded = for {
      dir      <- Try(Files.createTempDirectory(randomUUID().toString))
      tempFile = s"${dir.toString}/${name(from)}"
      _        <- Try(sftpRawClient.get(from, tempFile))
      file     <- Try(new File(tempFile))
    } yield file

    downloaded.toEither
  }

  def upload(file: File, to: String): Either[Throwable, File] = {
    val uploaded = for {
      _ <- Try(sftpRawClient.put(file.getPath, to))
    } yield file

    uploaded.toEither
  }

  def copy(from: String, to: String): Either[Throwable, File] =
    for {
      downloaded <- download(from)
      uploaded   <- upload(downloaded, to)
    } yield uploaded

  def move(from: String, to: String): Either[Throwable, File] =
    for {
      copied <- copy(from, to)
      _      <- Try(sftpRawClient.rm(from)).toEither
    } yield copied

  def listFiles(of: String): Either[Throwable, List[String]] = {
    val fileNames = for {
      files <- Try(sftpRawClient.ls(of))
    } yield files

    fileNames.toEither
  }

  private def name(path: String) = path.split("/").toList.last
}

object SFTPClient {
  def apply(host: String, user: String, password: String): SFTPClient = new SFTPClient(new SFTPRawClientJsch(host, user, password))

  implicit class TryToEither[T](tried: Try[T]) {
    def toEither: Either[Throwable, T] = tried.fold(Left(_), Right(_))
  }
}
